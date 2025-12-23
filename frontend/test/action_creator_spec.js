import { expect } from 'chai'
import {
  SERVER_ADD_ITEM, serverAddItem, SERVER_EDIT_ITEM, serverEditItem,
  serverCompleteItem, SERVER_COMPLETE_ITEM, serverUnCompleteItem, SERVER_UNCOMPLETE_ITEM,
  serverDeleteItem, SERVER_DELETE_ITEM,
  loginUser, LOGIN_REQUEST, LOGIN_SUCCESS, LOGIN_FAILURE
} from '../src/actions'

describe('loginUser action creator', () => {
  let originalFetch
  let originalSetItem

  beforeEach(() => {
    originalFetch = global.fetch
    originalSetItem = global.window.localStorage.setItem
  })

  afterEach(() => {
    global.fetch = originalFetch
    global.window.localStorage.setItem = originalSetItem
  })

  it('dispatches LOGIN_SUCCESS even when localStorage.setItem throws', (done) => {
    // Mock fetch to return successful login
    global.fetch = () => Promise.resolve({
      ok: true,
      json: () => Promise.resolve({ token: 'test-jwt-token' })
    })

    // Mock localStorage.setItem to throw (simulates iOS private browsing)
    global.window.localStorage.setItem = () => {
      throw new Error('QuotaExceededError')
    }

    const dispatched = []
    const dispatch = (action) => dispatched.push(action)

    loginUser('testuser', 'testpass')(dispatch).then(() => {
      const types = dispatched.map(a => a.type)
      expect(types).to.include(LOGIN_REQUEST)
      expect(types).to.include(LOGIN_SUCCESS)
      expect(types).to.not.include(LOGIN_FAILURE)
      done()
    }).catch(done)
  })

  it('dispatches LOGIN_FAILURE when JSON parsing fails', (done) => {
    // Mock fetch to return successful response but invalid JSON
    global.fetch = () => Promise.resolve({
      ok: true,
      json: () => Promise.reject(new Error('Invalid JSON'))
    })

    const dispatched = []
    const dispatch = (action) => dispatched.push(action)

    loginUser('testuser', 'testpass')(dispatch).then(() => {
      const types = dispatched.map(a => a.type)
      expect(types).to.include(LOGIN_REQUEST)
      expect(types).to.include(LOGIN_FAILURE)
      done()
    }).catch(done)
  })
})

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

