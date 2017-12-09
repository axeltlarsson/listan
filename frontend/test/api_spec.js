import { expect } from 'chai'
import {mergeItemState, deriveItemActions, mergeListState, deriveListActions} from '../src/middleware/api'
import {
  addItem, editItem, completeItem,
  unCompleteItem, deleteItem
} from '../src/actions'
import reducer from '../src/reducers'

describe('api middleware mergeListState and deriveListActions', () => {
  const list1 = {
    uuid: 'list-1',
    name: 'list one',
    description: 'a list',
    updated: {time: 0},
    created: {time: 0},
    fulfilled: false
  }

  const list2 = {
    uuid: 'list-2',
    name: 'list two',
    description: 'a snd list',
    updated: {time: 100},
    created: {time: 100},
    fulfilled: false
  }

  it('handles empty clientState with non-empty serverState', () => {
    const serverState = [list1, list2]
    const localState = []

    const newState = mergeListState(localState, serverState, {})
    expect(newState).to.eql(serverState.map(l => {return {...l,  fulfilled: true}}))

    const derivedActions = deriveListActions(serverState, newState)
    expect(derivedActions).to.eql([])
  })

  it('handles empty serverState with non-empty clientState', () => {
    const serverState = []
    const localState = {
      lists: {
        byId: {
          [list1.uuid]: list1,
          [list2.uuid]: list2
        },
        allIds: [list1.uuid, list2.uuid]
      }
    }
    const localStateTransformed = localState.lists.allIds.map(id => localState.lists.byId[id])
    expect(localStateTransformed).to.eql([list1, list2])

    const newState = mergeListState(localStateTransformed, serverState, {})
    expect(newState).to.eql(localStateTransformed)
    const actions = deriveListActions(serverState, newState)
    expect(actions).to.have.lengthOf(2)
    expectAddList(actions[0], list1)
    expectAddList(actions[1], list2)

    const state = actions.reduce(reducer, {})
    expect(scrubListsTimestamps(state.lists)).to.eql(scrubListsTimestamps(localState.lists))
  })

  const scrubListsTimestamps = (state) => {
    const result = {byId: {}, allIds: []}
    state.allIds.forEach(id => {
      const list = state.byId[id]
      const scrubbedList = {...list, updated: {}, created: {}}
      result.allIds.push(id)
      result.byId[id] = scrubbedList
    })
    return result
  }

  const expectAddList = (action, list) => {
    expect(action.type).to.eql("ADD_LIST")
    expect(action.name).to.eql(list.name)
    expect(action.description).to.eql(list.description)
  }


  it('handles server-side deletes', () => {
    const serverState = [list1]
    const localState = [list1, {...list2, fulfilled: true}] // if not fulfilled it would be added to server
    const newState = mergeListState(localState, serverState, {})
    expect(newState).to.eql([{...list1, fulfilled: true}])

    const actions = deriveListActions(serverState, newState)
    expect(actions).to.eql([])
  })

  it('handles client-side deletes', () => {
    const serverState = [list1, list2]
    const localState = [list1]
    const deletions = {[list2.uuid]: true}
    const newState = mergeListState(localState, serverState, deletions)
    expect(newState).to.eql([{...list1, fulfilled: true}])

    const actions = deriveListActions(serverState, newState)
    expect(actions).to.have.lengthOf(1)
    expect(actions[0].type).to.eql("DELETE_LIST")
    expect(actions[0].uuid).to.eql(list2.uuid)
  })

  it('resolves conflicts by updated timestamp', () => {
    // server has updated list2
    const serverState = [list1, {...list2, description: 'updated server', updated: {time: 200}}]
    // client has updated list1, and fulfilled list2 (it has to be fulfilled otherwise it would not be on server
    const localState = [{...list1, name: "new name given by client", updated: {time: 200}}, {...list2, fulfilled: true}]
    const newState = mergeListState(localState, serverState, {})
    // expect state to contain client's list1 and server's list2 (but fulfilled)
    expect(newState).to.eql([localState[0], {...serverState[1], fulfilled: true}])

    const actions = deriveListActions(serverState, newState)
    expect(actions).to.have.lengthOf(1)
    expect(actions[0].type).to.eql("UPDATE_LIST_NAME")
    expect(actions[0].uuid).to.eql(list1.uuid)
    expect(actions[0].newName).to.eql("new name given by client")
  })
})


describe('api middleware mergeItemState', () => {
  const item1 = {
    uuid: "s-1",
    contents: "item 1",
    list_uuid: 'list1',
    completed: false,
    updated: {
      time: 100
    }
  }
  const item2 = {
    uuid: "s-2",
    contents: "item 2",
    list_uuid: 'list1',
    completed: true,
    updated: {
      time: 110
    }
  }
  const item3 = {
    uuid: "c-3",
    contents: "item 3",
    list_uuid: 'list1',
    completed: true,
    updated: {
      time: 105
    }
  }
  const item1Fulfilled = {...item1, fulfilled: true}
  const item2Fulfilled = {...item2, fulfilled: true}
  const item3Fulfilled = {...item3, fulfilled: true}
  const item1LocalUpdated = Object
    .assign(
      {},
      item1Fulfilled,
      {contents: "item1 client update", completed: true, updated: {time: 200}}
    )
  const item2ServerUpdated = { ...item2, contents: "has changed on server", updated: {time:203} }

  it('handles server deletions', () => {
    const clientState = [item1Fulfilled, item2Fulfilled, item3Fulfilled]
    const serverState = [item1, item2]
    // Item3 has been deleted on the server
    const newState = mergeItemState(clientState, serverState, {})
    expect(newState).to.eql([item1Fulfilled, item2Fulfilled])

    const actions = deriveItemActions(serverState, newState)
    expect(actions).to.eql([])
  })

  it('handles client side deletes', () => {
    const clientState = [item1Fulfilled, item2Fulfilled]
    let deletions = {}
    deletions[item3.uuid] = true
    const serverState = [item1, item2, item3]
    const newState = mergeItemState(clientState, serverState, deletions)
    expect(newState).to.eql([item1Fulfilled, item2Fulfilled])

    const actions = deriveItemActions(serverState, newState)
    expect(actions).to.eql([deleteItem(item3.uuid)])
  })

  it('retains the most updated item', () => {
    const clientState = [item1LocalUpdated, item2Fulfilled, item3]
    const serverState = [item1, item2ServerUpdated]
    const newState = mergeItemState(clientState, serverState, {})
    expect(newState).to.eql([ item1LocalUpdated, {...item2ServerUpdated, fulfilled: true}, item3 ])

    const actions = deriveItemActions(serverState, newState)
    expect(actions).to.have.lengthOf(3)
    expect(actions[0]).to.include(scrubTimestamps(addItem(item3.contents, "list1", item3.uuid)))
    expect(actions[1]).to.include(scrubTimestamps(editItem(item1LocalUpdated.uuid, item1LocalUpdated.contents)))
    expect(actions[2]).to.include(scrubTimestamps(completeItem(item1LocalUpdated.uuid)))

  })

  const scrubTimestamps = (action) => {
    delete action['created']
    delete action['updated']
    return action
  }

  it('handles client side updates on unfullfilled items', () => {
    // Item2 has been "uncompleted" since it is completed on the server
    const item2Updated = { ...item2, updated: { time: 400 }, completed: false }
    // item3 has had its contents changed client side before being fulfilled
    const item3Updated = { ...item3, updated: {time: 401}, contents: "changed client side" }

    const clientState = [
      item1Fulfilled,
      item2Updated,
      item3Updated
    ]
    const serverState = [item1, {...item2, completed: true}]
    const newState = mergeItemState(clientState, serverState, {})
    expect(newState).to.eql([item1Fulfilled, item2Updated, item3Updated])
    const actions = deriveItemActions(serverState, newState)
    expect(actions).to.have.lengthOf(2)
    expect(actions[0]).to.include(scrubTimestamps(addItem(item3Updated.contents, "list1", item3Updated.uuid)))
    expect(actions[1]).to.include(scrubTimestamps(unCompleteItem(item2Updated.uuid)))
  })

  it('handles empty client state properly', () => {
    const clientState = []
    const serverState = [item1, item2, item3]
    const newState = mergeItemState(clientState, serverState, {})
    expect(newState).to.eql([item1Fulfilled, item2Fulfilled, item3Fulfilled])

    const actions = deriveItemActions(serverState, newState)
    expect(actions).to.eql([])
  })

  it('handles empty server state properly', () => {
    const newState = mergeItemState([item1, item2, item3Fulfilled], [], {})
    expect(newState).to.eql([item1, item2])

    const actions = deriveItemActions([], newState)
    expect(actions).to.eql([
      addItem(item1.contents, "list1", item1.uuid),
      addItem(item2.contents, "list1", item2.uuid)
    ])
  })

  it('handles empty server and client state properly', () => {
    const newState = mergeItemState([], [], {})
    expect(newState).to.eql([])

    const actions = deriveItemActions([], [])
    expect(actions).to.eql([])
  })

  it('handles no-change states', () => {
    const clientState = [item1Fulfilled, item2Fulfilled, item3Fulfilled]
    const serverState = [item1, item2, item3]
    const newState = mergeItemState(clientState, serverState, {})
    // Nothing has happened when client was offline
    expect(newState).to.eql(clientState)

    const actions = deriveItemActions(serverState, newState)
    expect(actions).to.eql([])
  })
})

