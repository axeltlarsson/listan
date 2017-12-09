import { expect } from 'chai'
import {
  SERVER_ADD_ITEM, serverAddItem, SERVER_EDIT_ITEM, serverEditItem,
  serverCompleteItem, SERVER_COMPLETE_ITEM, serverUnCompleteItem, SERVER_UNCOMPLETE_ITEM,
  serverDeleteItem, SERVER_DELETE_ITEM
} from '../src/actions'

describe('server action creators', () => {
  it('creates correct SERVER_ADD_ITEM', () => {
    const action = serverAddItem("some contents", "list1", "some uuid")
    // using .include because action includes updated and created times
    expect(action).to.include({
      type: SERVER_ADD_ITEM,
      uuid: "some uuid",
      contents: "some contents"
    })
  })

  it('creates correct SERVER_EDIT_ITEM', () => {
    const action = serverEditItem("uuid", "contents")
    expect(action).to.include({
      type: SERVER_EDIT_ITEM,
      uuid: "uuid",
      contents: "contents"
    })
  })

  it('creates correct SERVER_COMPLETE_ITEM', () => {
    const action = serverCompleteItem("uuid")
    expect(action).to.include({
      type: SERVER_COMPLETE_ITEM,
      uuid: "uuid"
    })
  })

  it('creates correct SERVER_UNCOMPLETE_ITEM', () => {
    const action = serverUnCompleteItem("uuid")
    expect(action).to.include({
      type: SERVER_UNCOMPLETE_ITEM,
      uuid: "uuid"
    })
  })

  it('creates correct SERVER_DELETE_ITEM', () => {
    const action = serverDeleteItem("uuid")
    expect(action).to.include({
      type: SERVER_DELETE_ITEM,
      uuid: "uuid"
    })
  })
})

