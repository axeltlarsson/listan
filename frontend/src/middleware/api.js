import {
  ADD_ITEM, EDIT_ITEM, REQUEST_WS, requestWs, wsSuccess, wsFailure,
  fulfilItem, DELETE_ITEM, completeItem, unCompleteItem, deleteItem,
  serverAddItem, serverEditItem, serverCompleteItem, serverUnCompleteItem,
  serverDeleteItem, COMPLETE_ITEM, UNCOMPLETE_ITEM, setItems, CLEAR_TOGGLED,
  addItem, editItem, LOGOUT, logout, ADD_LIST, fulfilList,
  UPDATE_LIST_NAME, UPDATE_LIST_DESCRIPTION, DELETE_LIST, setLists, addList,
  updateListName, updateListDescription, deleteList, serverAddList, serverUpdateListName,
  serverUpdateListDescription, serverDeleteList
} from '../actions'
import * as uuidGen from 'uuid'
import {WS_API_URL} from '../constants'

const onOpen = (ws, store) => evt => {
  console.log("onOpen")
  connectionAttempts = 0
}

let connectionAttempts = 0
const attemptReconnection = (store) => {
  connectionAttempts++
  setTimeout(() => {
    store.dispatch(requestWs())
  }, Math.min(10 * Math.random() * Math.pow(2, connectionAttempts), 5000))
}

const onClose = (ws, store) => closeEvent => {
  console.log("onClose", closeEvent)
  store.dispatch(wsFailure(closeEvent.reason))
  if (store.getState().auth.isAuthenticated && ws.readyState !== WebSocket.CONNECTING)
    attemptReconnection(store)
}

const onError = (store) => error => {
  console.log("onError: ", error)
  store.dispatch(wsFailure())
  ws.close()
}

const sync = (ws, store) => {
  const getStateMsg = {
    type: "GetState",
    ack: uuidGen.v4()
  }
  send(ws, getStateMsg).then(state => {
    // Resolve lists
    const lists = state.lists
    const localLists = store.getState().lists.allIds.map(i => store.getState().lists.byId[i])
    const mergedListState = mergeListState(localLists, lists, deletedLists)

    // Set local state to new list state
    let normalisedListState = {
      byId: {}, // build this in next step
      allIds: mergedListState.map(l => l.uuid)
    }
    mergedListState.forEach(l => {
      normalisedListState.byId[l.uuid] = l
    })

    store.dispatch(setLists(normalisedListState))

    // Dispatch derived actions that will be sent to server
    const derivedListActions = deriveListActions(lists, mergedListState)
    derivedListActions.forEach(a => api(store)(_ => _)(a))

    // Resolve items
    const items = state.items
    const mergedItemState = mergeItemState(store.getState().items, items, deletedItems)
    const derivedItemActions = deriveItemActions(items, mergedItemState)
    // Set local store to new state, dispatch derived actions
    store.dispatch(setItems(mergedItemState))
    derivedItemActions.forEach(a => api(store)(_ => _)(a))
    promiseMap.clear()
  }).catch(err => {
    console.error("GetState promise broken!", err)
  })
}

const onMessage = (ws, store) => evt => {
  const msg = JSON.parse(evt.data)
  switch (msg.type) {
    case "AuthRequest":
      const response = {
        type: "Auth",
        token: store.getState().auth.jwt,
        ack: uuidGen.v4()
      }
      send(ws, response).then(auth => {
        console.info("Authenticated WS")
        store.dispatch(wsSuccess())
        sync(ws, store)
        ping(ws, store)
      }).catch(err => {
        console.error(err)
        store.dispatch(wsFailure("Unauthorized"))
        store.dispatch(logout())
      })
      break
    case "AuthResponse":
      if (msg.status === "Authentication success")
        resolveAction(msg.ack, msg.status)
      else
        rejectAction(msg.ack, msg.status)
      break
    case "Pong":
      resolveAction(msg.ack, null)
      break
    case "UUIDResponse":
      resolveAction(msg.ack, msg.uuid)
      break
    case "FailureResponse":
      rejectAction(msg.ack, msg.error)
      break
    case "GetStateResponse":
      resolveAction(msg.ack, {lists: msg.lists, items: msg.items})
      break
    case "AddItem":
      ws.send(JSON.stringify({type: "Ack", ack: msg.ack}))
      store.dispatch(serverAddItem(msg.contents, msg.list_uuid, msg.uuid))
      break
    case "EditItem":
      ws.send(JSON.stringify({type: "Ack", ack: msg.ack}))
      store.dispatch(serverEditItem(msg.uuid, msg.contents))
      break
    case "CompleteItem":
      ws.send(JSON.stringify({type: "Ack", ack: msg.ack}))
      store.dispatch(serverCompleteItem(msg.uuid))
      break
    case "UnCompleteItem":
      ws.send(JSON.stringify({type: "Ack", ack: msg.ack}))
      store.dispatch(serverUnCompleteItem(msg.uuid))
      break
    case "DeleteItem":
      ws.send(JSON.stringify({type: "Ack", ack: msg.ack}))
      store.dispatch(serverDeleteItem(msg.uuid))
      break
    case "AddList":
      ws.send(JSON.stringify({type: "Ack", ack: msg.ack}))
      store.dispatch(serverAddList(msg.name, msg.description, msg.uuid))
      break
    case "UpdateListName":
      ws.send(JSON.stringify({type: "Ack", ack: msg.ack}))
      store.dispatch(serverUpdateListName(msg.uuid, msg.name))
      break
    case "UpdateListDescription":
      ws.send(JSON.stringify({type: "Ack", ack: msg.ack}))
      store.dispatch(serverUpdateListDescription(msg.uuid, msg.description))
      break
    case "DeleteList":
      ws.send(JSON.stringify({type: "Ack", ack: msg.ack}))
      store.dispatch(serverDeleteList(msg.uuid))
      break
    default:
      break
  }
}

/*
 *  Resolve action corresponding to 'ack'
 *  with 'value' if possible, log error otherwise
 */
const resolveAction = (ack, value) => {
  const promise = promiseMap.get(ack)
  if (promise === undefined) {
    console.error("Cannot find promise for: ", ack, value)
  } else {
    promise.resolve(value)
    promiseMap.delete(ack)
  }
}

/*
 * Reject action corresponding to 'ack' with
 * 'value' if possible, log error otherwise
 */
const rejectAction = (ack, value) => {
  const promise = promiseMap.get(ack)
  if (promise === undefined) {
    console.error("Cannot find promise for: ", ack, value)
  } else {
    promise.reject(value)
    promiseMap.delete(ack)
  }
}

/*
 * Returns a Promise that is fulfilled when msg has been accepted server side,
 * or that is rejected if the server responds with an error
 */
const send = (ws, msg) => {
  const p = new Promise((resolve, reject) => {
    promiseMap.set(msg.ack, {resolve, reject, type: msg.type})
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(msg))
    } else {
      console.warn("Cannot send msg, socket is not ready:", msg)
      reject({network_err: "cannot send msg, socket is not ready!"})
    }
  })
  return timeout(p, 1500)
}

const delay = (time) => {
  return new Promise(fulfill => {
    setTimeout(fulfill, time)
  })
}

const timeout = (promise, time) => {
  return Promise.race([
      promise,
      delay(time).then(() => {
        throw { network_err: `No ack provided within specified ${time} ms`}
      })
    ])
}

const ping = (ws, store) => {
  const msg = {
    type: "Ping",
    ack: uuidGen.v4()
  }
  send(ws, msg).then(() => {
    setTimeout(() => ping(ws, store), 2000)
  }).catch(err => {
    console.log("err", err)
    if (err.network_err && ws.readyState === WebSocket.OPEN) {
      ws.close()
      store.dispatch(wsFailure())
      store.dispatch(requestWs())
    }
  })
}

/*
 * Returns the new desired state for items
 */
export const mergeItemState = (localState, serverState, deleted) => {
  return localState
    .filter(i => !i.fulfilled || serverState.find(_ => _.uuid === i.uuid))
    .concat(serverState.filter(i => !localState.find(_ => _.uuid === i.uuid)))
    .filter(i => !deleted[i.uuid])
    .map(i => {
      const itemInServer = serverState.find(_ => _.uuid === i.uuid)
      if (itemInServer) {
        // Allow a gap of diff of 50 millis
        if (i.updated.time + 50 > itemInServer.updated.time && !eqlItems(itemInServer, i))
          return i
        else
          return {...itemInServer, fulfilled: true}
      } else {
        return i
      }
    })
}

const eqlItems = (item1, item2) => {
  return (
    item1.uuid == item2.uuid &&
    item1.contents == item2.contents &&
    item1.completed == item2.completed &&
    item1.list_uuid == item2.list_uuid
  )
}

/*
 * Returns the new desired state for lists
 */
export const mergeListState = (localState, serverState, deleted) => {
  return localState
    .filter(l => !l.fulfilled || serverState.find(_ => _.uuid === l.uuid))
    .concat(serverState.filter(l => !localState.find(_ => _.uuid === l.uuid)))
    .filter(l => !deleted[l.uuid])
    .map(l => {
      const listInServer = serverState.find(_ => _.uuid === l.uuid)
      if (listInServer) {
        // Allow time gap of 50 ms
        if (l.updated.time + 50 > listInServer.updated.time && !eqlLists(listInServer, l))
          return l
        else
          return {...listInServer, fulfilled: true}
      } else {
        return l
      }
    })
}

const eqlLists = (list1, list2) => {
  return (
    list1.uuid == list2.uuid &&
    list1.name == list2.name &&
    list1.description == list2.description
  )
}

/*
 * Returns the required actions to transform serverState into state for items
 */
export const deriveItemActions = (serverState, state) => {
  const notInState = serverState.filter(i => !state.find(_ => _.uuid === i.uuid))
  const deletions = notInState.map(i => deleteItem(i.uuid))
  const notInServer = state.filter(i => !serverState.find(_ => _.uuid === i.uuid))
  const adds = notInServer.map(i => addItem(i.contents, i.list_uuid, i.uuid))
  const inBoth = state.filter(i => serverState.find(_ => _.uuid === i.uuid))
  const edits = inBoth.filter(i => {
    const s = serverState.find(_ => _.uuid === i.uuid)
    return (s.contents !== i.contents)
  }).map(i => editItem(i.uuid, i.contents))
  const toggles = inBoth.filter(i => {
    const s = serverState.find(_ => _.uuid === i.uuid)
    return (s.completed !== i.completed)
  }).map(i => i.completed ? completeItem(i.uuid) : unCompleteItem(i.uuid))

  return adds.concat(deletions).concat(edits).concat(toggles)
}

/*
 * Returns the requried actions to transform serverState into state for lists
 */
export const deriveListActions = (serverState, state) => {
  const notInState = serverState.filter(l => !state.find(_ => _.uuid === l.uuid))
  const deletions = notInState.map(l => deleteList(l.uuid))
  const notInServer = state.filter(l => !serverState.find(_ => _.uuid === l.uuid))
  const adds = notInServer.map(l => addList(l.name, l.description, l.uuid))
  const inBoth = state.filter(l => serverState.find(_ => _.uuid === l.uuid))
  const nameEdits = inBoth.filter(l => {
    const s = serverState.find(_ => _.uuid === l.uuid)
    return (s.name !== l.name)
  }).map(l => updateListName(l.uuid, l.name))
  const descrEdits = inBoth.filter(l => {
    const s = serverState.find(_ => _.uuid === l.uuid)
    return (s.description !== l.description)
  }).map(l => updateListDescription(l.uuid, l.description))

  return adds.concat(deletions).concat(nameEdits).concat(descrEdits)
}

const promiseMap = new Map()
let deletedItems = {}
let deletedLists = {}
let ws = null

window.addEventListener("beforeunload", (e) => {
  if (promiseMap.size > 0) {
    for (let msg of promiseMap.values()) {
      console.log(msg.type)
      console.log(msg)
      promiseMap.clear()
      if (msg.type !== "Ping") {
        e.returnValue = "Osparade ändringar"
        // TODO: show info to user (e.g. toast)
        return "Osparade ändringar"
      }
    }
  }
  return null
})

const handleBrokenWsPromise = (err, promiseName, store) => {
  console.error(`Promise ${promiseName} broken!`, err)
  if (err.network_err && ws.readyState !== WebSocket.CLOSING && ws.readyState !== WebSocket.CONNECTING) {
    ws.close()
    store.dispatch(wsFailure(err.network_err))
    store.dispatch(requestWs())
  }
}

const api = store => next => action => {
  if (action === undefined)
    console.log("UNDEFINED ACTION SUBMITTED!")
  switch (action.type) {
    case REQUEST_WS: {
      if (ws && ws.readyState === WebSocket.OPEN) {
        let res = next(action)
        store.dispatch(wsSuccess())
        return res
      }
      if (!ws || ws.readyState === WebSocket.CLOSED || ws.readyState === WebSocket.CLOSING) {
        ws = new WebSocket(WS_API_URL)
        ws.onopen = onOpen(ws, store)
        ws.onmessage = onMessage(ws, store)
        ws.onerror = onError(store)
        ws.onclose = onClose(ws, store)
      }
      break
    }
    case ADD_ITEM: {
      const msg = {
        type: "AddItem",
        ack: uuidGen.v4(),
        contents: action.contents,
        uuid: action.uuid,
        list_uuid: action.list_uuid
      }
      send(ws, msg).then(uuid => {
        console.log("AddItem accepted server side", uuid)
        store.dispatch(fulfilItem(action.uuid, uuid))
      }).catch(err => {
        handleBrokenWsPromise(err, ADD_ITEM, store)
      })
      break
    }
    case EDIT_ITEM: {
      const msg = {
        type: "EditItem",
        uuid: action.uuid,
        ack: uuidGen.v4(),
        contents: action.contents
      }
      send(ws, msg).then(uuid => {
        console.log("edit accepted", uuid)
      }).catch(err => {
        handleBrokenWsPromise(err, EDIT_ITEM, store)
      })
      break
    }
    case COMPLETE_ITEM: {
      const msg = {
        type: "CompleteItem",
        uuid: action.uuid,
        ack: uuidGen.v4()
      }
      send(ws, msg).then(uuid => {
        console.log("completeItem accepted", uuid)
      }).catch(err => {
        handleBrokenWsPromise(err, COMPLETE_ITEM, store)
      })
      break
    }
    case UNCOMPLETE_ITEM: {
      const msg = {
        type: "UnCompleteItem",
        uuid: action.uuid,
        ack: uuidGen.v4()
      }
      send(ws, msg).then(uuid => {
        console.log("UnCompleteItem accepted", uuid)
      }).catch(err => {
        handleBrokenWsPromise(err, UNCOMPLETE_ITEM, store)
      })
      break
    }
    case DELETE_ITEM: {
      const msg = {
        type: "DeleteItem",
        ack: uuidGen.v4(),
        uuid: action.uuid
      }
      deletedItems[action.uuid] = true
      send(ws, msg).then(uuid => {
        console.log("delete accepted", uuid)
        delete deletedItems[uuid]
      }).catch(err => {
        handleBrokenWsPromise(err, DELETE_ITEM, store)
      })
      break
    }
    case CLEAR_TOGGLED: {
      const msgs = store.getState().items.filter(i => i.completed).map(i => {
        return {
          type: "DeleteItem",
          ack: uuidGen.v4(),
          uuid: i.uuid
        }
      })
      msgs.forEach(msg => {
        deletedItems[msg.uuid] = true
        send(ws, msg).then(uuid => {
          console.log("deleted toggled", uuid)
          delete deletedItems[msg.uuid]
        }).catch(err => {
          handleBrokenWsPromise(err, CLEAR_TOGGLED, store)
        })
      })
      break
    }
    case ADD_LIST: {
      const msg = {
        type: "AddList",
        ack: uuidGen.v4(),
        name: action.name,
        description: action.description,
        uuid: action.uuid
      }
      send(ws, msg).then(uuid => {
        console.log("AddList accepted server side", uuid)
        store.dispatch(fulfilList(action.uuid, uuid))
      }).catch(err => {
        handleBrokenWsPromise(err, ADD_LIST, store)
      })
      break
    }
    case UPDATE_LIST_NAME: {
      const msg = {
        type: "UpdateListName",
        ack: uuidGen.v4(),
        name: action.newName,
        uuid: action.uuid
      }
      send(ws, msg).then(uuid => {
        console.log("UpdateListName accepted server side", uuid)
      }).catch(err => {
        handleBrokenWsPromise(err, UPDATE_LIST_NAME, store)
      })
      break
    }
    case UPDATE_LIST_DESCRIPTION: {
      const msg = {
        type: "UpdateListDescription",
        ack: uuidGen.v4(),
        description: action.newDescription,
        uuid: action.uuid
      }
      send(ws, msg).then(uuid => {
        console.log("UpdateListDescription accepted server side", uuid)
      }).catch(err => {
        handleBrokenWsPromise(err, UPDATE_LIST_DESCRIPTION, store)
      })
      break
    }
    case DELETE_LIST: {
      const msg = {
        type: "DeleteList",
        ack: uuidGen.v4(),
        uuid: action.uuid
      }
      deletedLists[action.uuid] = true
      send(ws, msg).then(uuid => {
        console.log("DeleteList accepted server side", uuid)
        delete deletedLists[uuid]
      }).catch(err => {
        handleBrokenWsPromise(err, DELETE_LIST, store)
      })
      break
    }
    case LOGOUT: {
      if (ws) {
        ws.close()
      }
      break
    }
    default: {
      return next(action)
    }
  }
  return next(action)
}

export default api

