import { expect } from 'chai'
import reducer from '../src/reducers'
import {
  ADD_ITEM, completeItem, editItem, toggleItemEditMode, deleteItem, unCompleteItem,
  fulfilItem, TOGGLE_LIST_NAME_EDIT_MODE, TOGGLE_LIST_DESCR_EDIT_MODE
} from '../src/actions'

describe('lists reducer', () => {
  it('handles ADD_LIST', () => {
    const initialState = {}
    const action = { type: 'ADD_LIST', uuid: 'l1', name: 'a list', description: 'descr' }
    const nextState = reducer(initialState, action)
    expect(nextState.lists).to.eql({
        byId: {
          'l1': {
            uuid: 'l1',
            name: 'a list',
            description: 'descr',
            created: undefined,
            updated: undefined,
            fulfilled: false
          }
        },
        allIds: ['l1'],
        pending: true
      })

    const action2 = { type: 'ADD_LIST', uuid: 'l2', name: 'snd list', description: 'descr2' }
    const nextState2 = reducer(nextState, action2)
    expect(nextState2.lists).to.eql({
      byId: {
        'l1': {
          uuid: 'l1',
          name: 'a list',
          description: 'descr',
          created: undefined,
          updated: undefined,
          fulfilled: false
        },
        'l2': {
          uuid: 'l2',
          name: 'snd list',
          description: 'descr2',
          created: undefined,
          updated: undefined,
          fulfilled: false
        }
      },
      allIds: ['l1', 'l2'],
      pending: true
    })

  })

  it('handles TOGGLE_LIST_NAME_EDIT_MODE', () => {
    const initialState = {
      lists: {
        byId: {
          list1: { uuid: 'list1', name: 'a name' }
        },
        allIds: ['list1']
      }
    }
    const action = {type: TOGGLE_LIST_NAME_EDIT_MODE, uuid: 'list1', updated: { time: 100 }}
    const nextState = reducer(initialState, action)
    expect(nextState.lists).to.eql({
      byId: {
        list1: {
          uuid: 'list1',
          name: 'a name',
          name_edit_mode: true,
          updated: { time: 100 }
        }
      },
      allIds: ['list1']
    })
  })


  it('handles TOGGLE_LIST_DESCR_EDIT_MODE', () => {
    const initialState = {
      lists: {
        byId: {
          list1: { uuid: 'list1', name: 'some name' }
        },
        allIds: ['list1']
      }
    }
    const action = {type: TOGGLE_LIST_DESCR_EDIT_MODE, uuid: 'list1', updated: { time: 100 }}
    const nextState = reducer(initialState, action)
    expect(nextState.lists).to.eql({
      byId: {
        list1: {
          uuid: 'list1',
          name: 'some name',
          descr_edit_mode: true,
          updated: {time: 100}
        }
      },
      allIds: ['list1']
    })
  })

  it('handles UPDATE_LIST_NAME', () => {
    const initialState = {
      lists: {
        byId: {
          list1: {
            uuid: 'list1',
            name: 'old name'
          }
        },
        allIds: ['list1']
      }
    }
    const action = { type: 'UPDATE_LIST_NAME', uuid: 'list1',  newName: 'new name', updated: { time: 100 } }
    const newState = reducer(initialState, action)
    expect(newState.lists).to.eql({
      byId: {
        list1: {
          uuid: 'list1',
          name: 'new name',
          updated: { time: 100 }
        }
      },
      allIds: ['list1']
    })

  })

  it('handles UPDATE_LIST_DESCRIPTION', () => {
    const initialState = {
      lists: {
        byId: {
          list1: {
            uuid: 'list1',
            name: 'name',
            description: 'old desr'
          }
        },
        allIds: ['list1']
      }
    }

    const action = { type: 'UPDATE_LIST_DESCRIPTION', uuid: 'list1', description: 'new description', updated: {time: 200} }
    const newState = reducer(initialState, action)
    expect(newState.lists).to.eql({
      byId: {
        list1: {
          uuid: 'list1',
          name: 'name',
          description: 'new description',
          updated: { time: 200 }
        }
      },
      allIds: ['list1']
    })
  })


  it('handles DELETE_LIST', () => {
    const initialState = {
      lists: {
        byId: {
          list1: {
            uuid: 'list1',
            name: 'name',
            description: 'old desr'
          },
          list2: {
            uuid: 'list2',
            name: 'name 2',
            description: 'description'
          }
        },
        allIds: ['list1', 'list2']
      }
    }

    const action = { type: 'DELETE_LIST', uuid: 'list1' }
    const newState = reducer(initialState, action)
    expect(newState.lists).to.eql({
      byId: {
        list2: {
          uuid: 'list2',
          name: 'name 2',
          description: 'description'
        }
      },
      allIds: ['list2']
    })
  })

  it('handles setLists', () => {
    const initialState = {
      lists: {
        byId: {
          list0: { uuid: 'list0', name: 'name', description: ''}
        },
        allIds: ['list0']
      }
    }
    const payload = [
      { uuid: 'list1', name: 'list1', description: '' },
      { uuid: 'list2', name: 'list2', description: '' }
    ]
    let transformedPayload = {
      byId: {
      },
      allIds: payload.map(l => l.uuid)
    }
    payload.forEach(l => {
      transformedPayload.byId[l.uuid] = l
    })

    expect(transformedPayload).to.eql({
      byId: {
        'list1': { uuid: 'list1', name: 'list1', description: ''},
        'list2': { uuid: 'list2', name: 'list2', description: ''}
      },
      allIds: ['list1', 'list2']
    })

    const action = { type: 'SET_LISTS', lists: transformedPayload }

    const newState = reducer(initialState, action)
    expect(newState.lists).to.eql(transformedPayload)
  })
})

describe('items reducer', () => {

  it('handles ADD_ITEM', () => {
    const initialState = {}
    const action = { type: ADD_ITEM, uuid: '1', contents: 'the first ever item',
      created: { time: 1 }, updated: { time: 1 }, list_uuid: 'list1' }
    const nextState = reducer(initialState, action)
    expect(nextState.items[0]).to.deep.equal({
      uuid: '1',
      contents: 'the first ever item',
      created: { time: 1 },
      completed: false,
      fulfilled: false,
      updated: { time: 1 },
      list_uuid: 'list1'})
  })

  it('handles FULFILL_ITEM and does not delete items for no apparent reason', () => {
    const initialState = {}
    const addAction = { type: ADD_ITEM, uuid: '1', contents: 'hejsan', created: { time: 1}, updated: { time: 1} }
    const add2Action = { type: ADD_ITEM, uuid: '2', contents: 'då', created: { time: 1}, updated: { time: 1} }
    const addedState = reducer(reducer(initialState, addAction), add2Action)
    expect(addedState.items[0]).to.eql({
      uuid: '1',
      contents: 'hejsan',
      completed: false, created: { time: 1}, updated: { time: 1},
      fulfilled: false,
      list_uuid: undefined
    })
    const fAction = fulfilItem('1')
    const fulfilledState = reducer(addedState, fAction)
    expect(fulfilledState.items).to.have.length(2)
    expect(fulfilledState.items[0]).to.have.property('fulfilled', true)
    expect(fulfilledState.items[1]).to.have.property('fulfilled', false)
  })


  it('handles COMPLETE_ITEM and UNCOMPLETE_ITEM and does not delete items for no apparent reason', () => {
    const initialState = {
      items: [{
        uuid: '1',
        contents: 'the first ever item',
        completed: false,
        updated: { time: 2 }
      },{
        uuid: '2',
        contents: 'the second ever item',
        completed: false,
        updated: { time: 2 }
      }]
    }
    const action = {
      type: 'COMPLETE_ITEM',
      uuid: '1',
      updated: { time: 2 }
    }
    const nextState = reducer(initialState, action)
    expect(nextState.items[0]).to.deep.equal({
      uuid: '1',
      contents: 'the first ever item',
      completed: true,
      updated: { time: 2 }
    })
    expect(nextState.items).to.have.length(2)
    const action2 = {
      type: 'UNCOMPLETE_ITEM',
      uuid: '1',
      updated: { time: 2 }
    }
    const nextState2 = reducer(nextState, action2)
    expect(nextState2.items).to.eql(initialState.items)
  })

  it('handles TOGGLE_ITEM_EDIT_MODE via (toggleItemEditMode action)', () => {
    const initialState = {
      items: [{
        uuid: '1',
        contents: 'some stupid text',
        completed: false,
        editMode: false
      }]
    }
    const action = toggleItemEditMode('1')
    const nextState = reducer(initialState, action)

    expect(nextState.items).to.eql([{
        uuid: '1',
        contents: 'some stupid text',
        completed: false,
        editMode: true
      }]
    )
  })

  it('handles EDIT_ITEM (via editItem action)', () => {
    const initialState = {
      items: [{
        uuid: '1',
        contents: 'some stupid contents',
        completed: false,
        updated: { time: 1 }
      }]
    }
    const action = {
      type: 'EDIT_ITEM',
      contents: 'updated contents',
      uuid: '1',
      updated: { time: 1 }
    }
    const nextState = reducer(initialState, action)

    expect(nextState.items).to.eql(
      [{
        uuid: '1',
        contents: 'updated contents',
        completed: false,
        updated: { time: 1 }
      }]
    )
  })

  it('handles DELETE_ITEM (via deleteItem action)', () => {
    const initialState = {
      items: [{
        uuid: '1',
        contents: 'some stupid item that needs to be deleted',
        completed: false
      }, {
        uuid: '2',
        contents: 'do not delete me',
        completed: true
      }]
    }
    const action = deleteItem('1')
    const nextState = reducer(initialState, action)

    expect(nextState.items).to.eql(
      [{
        uuid: '2',
        contents: 'do not delete me',
        completed: true
      }]
    )
  })
})

