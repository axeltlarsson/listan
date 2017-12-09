import * as uuidGen from 'uuid'
import 'whatwg-fetch'
import {LOGIN_URL} from '../constants'

// TODO: split into more files

/*
 * list actions
 * actions prefixed `SERVER` means that they *COME* from the server
 */
export const ADD_LIST = 'ADD_LIST'
export const addList = (name, description, uuid = uuidGen.v4()) => {
  return {
    type: ADD_LIST,
    uuid,
    name,
    description,
    created: { time: Date.now() / 1000 },
    updated: { time: Date.now() / 1000 }
  }
}
export const SERVER_ADD_LIST = 'SERVER_ADD_LIST'
export const serverAddList = (name, description, uuid) => {
  return {...addList(name, description, uuid), type: SERVER_ADD_LIST}
}


export const UPDATE_LIST_NAME = 'UPDATE_LIST_NAME'
export const updateListName = (uuid, newName) => {
  return {
    type: UPDATE_LIST_NAME,
    uuid,
    newName
  }
}
export const SERVER_UPDATE_LIST_NAME = 'SERVER_UPDATE_LIST_NAME'
export const serverUpdateListName = (uuid, newName) => {
  return {...updateListName(uuid, newName), type: SERVER_UPDATE_LIST_NAME}
}
export const UPDATE_LIST_DESCRIPTION = 'UPDATE_LIST_DESCRIPTION'
export const updateListDescription = (uuid, newDescription) => {
  return {
    type: UPDATE_LIST_DESCRIPTION,
    uuid,
    newDescription
  }
}

export const SERVER_UPDATE_LIST_DESCRIPTION = 'SERVER_UPDATE_LIST_DESCRIPTION'
export const serverUpdateListDescription = (uuid, newDescription) => {
  return {...updateListDescription(uuid, newDescription), type: SERVER_UPDATE_LIST_DESCRIPTION}
}

export const DELETE_LIST = 'DELETE_LIST'
export const deleteList = (uuid) => {
  return {
    type: DELETE_LIST,
    uuid
  }
}
export const SERVER_DELETE_LIST = 'SERVER_DELETE_LIST'
export const serverDeleteList = (uuid) => {
  return {...deleteList(uuid), type: SERVER_DELETE_LIST}
}

export const FULFIL_LIST = 'FULFIL_LIST'
export const fulfilList = (uuid) => {
  return {
    type: FULFIL_LIST,
    uuid
  }
}

export const TOGGLE_LIST_NAME_EDIT_MODE = 'TOGGLE_LIST_NAME_EDIT_MODE'
export const toggleListNameEditMode = (uuid) => {
  return {
    type: TOGGLE_LIST_NAME_EDIT_MODE,
    uuid
  }
}
export const TOGGLE_LIST_DESCR_EDIT_MODE = 'TOGGLE_LIST_DESCR_EDIT_MODE'
export const toggleListDescrEditMode = (uuid) => {
  return {
    type: TOGGLE_LIST_DESCR_EDIT_MODE,
    uuid
  }
}

export const SET_LISTS = 'SET_LISTS'
export const setLists = (lists) => {
  return {
    type: SET_LISTS,
    lists
  }
}

/*
 * item actions
 * Actions prefixed `SERVER` means that they *COME* from the server
*/
export const ADD_ITEM = 'ADD_ITEM'
export const addItem = (contents, list_uuid, uuid = uuidGen.v4()) => {
  return {
    type: ADD_ITEM,
    uuid,
    contents,
    list_uuid,
    created: { time: Date.now() },
    updated: { time: Date.now() }
  }
}

export const SERVER_ADD_ITEM = 'SERVER_ADD_ITEM'
export const serverAddItem = (contents, list_uuid, uuid) => {
  return {...addItem(contents, list_uuid, uuid), type: SERVER_ADD_ITEM}
}

export const COMPLETE_ITEM = 'COMPLETE_ITEM'

export const completeItem = (uuid) => {
  return {
    type: COMPLETE_ITEM,
    uuid,
    updated: { time: Date.now() }
  }
}

export const SERVER_COMPLETE_ITEM = 'SERVER_COMPLETE_ITEM'
export const serverCompleteItem = (uuid) => {
  return {...completeItem(uuid), type: SERVER_COMPLETE_ITEM}
}

export const UNCOMPLETE_ITEM = 'UNCOMPLETE_ITEM'

export const unCompleteItem = (uuid) => {
  return {
    type: UNCOMPLETE_ITEM,
    uuid,
    updated: { time: Date.now() }
  }
}

export const SERVER_UNCOMPLETE_ITEM = 'SERVER_UNCOMPLETE_ITEM'
export const serverUnCompleteItem = (uuid) => {
  return {...unCompleteItem(uuid), type: SERVER_UNCOMPLETE_ITEM}
}

export const EDIT_ITEM = 'EDIT_ITEM'

export const editItem = (uuid, newContents) => {
  return {
    type: EDIT_ITEM,
    contents: newContents,
    uuid,
    updated: { time: Date.now() }
  }
}

export const SERVER_EDIT_ITEM = 'SERVER_EDIT_ITEM'

export const serverEditItem = (uuid, newContents) => {
  return {...editItem(uuid, newContents), type: SERVER_EDIT_ITEM}
}

export const TOGGLE_ITEM_EDIT_MODE = 'TOGGLE_ITEM_EDIT_MODE'

export const toggleItemEditMode = (uuid) => {
  return {
    type: TOGGLE_ITEM_EDIT_MODE,
    uuid
  }
}

export const DELETE_ITEM = 'DELETE_ITEM'

export const deleteItem = (uuid) => {
  return {
    type: DELETE_ITEM,
    uuid
  }
}

export const SERVER_DELETE_ITEM = 'SERVER_DELETE_ITEM'
export const serverDeleteItem = (uuid) => {
  return {...deleteItem(uuid), type: SERVER_DELETE_ITEM}
}

export const CLEAR_TOGGLED = 'CLEAR_TOGGLED'

export const clearToggled = () => {
  return {
    type: CLEAR_TOGGLED
  }
}

export const FULFIL_ITEM = 'FULFIL_ITEM'

export const fulfilItem = (uuid) => {
  return {
    type: FULFIL_ITEM,
    uuid
  }
}

export const SET_ITEMS = 'SET_ITEMS'

export const setItems = (items) => {
  return {
    type: SET_ITEMS,
    items
  }
}

/*
 * auth
 */
export const LOGIN_REQUEST = 'LOGIN_REQUEST'

export const requestLogin = (userName, password) => {
  return {
    type: LOGIN_REQUEST,
    isFetching: true,
    isAuthenticated: false,
    userName,
    password
  }
}

export const LOGIN_SUCCESS = 'LOGIN_SUCCESS'

export const loginSuccess = (jwt) => {
  return {
    type: LOGIN_SUCCESS,
    isFetching: false,
    isAuthenticated: true,
    jwt
  }
}

export const LOGIN_FAILURE = 'LOGIN_FAILURE'

export const loginError = (message) => {
  return {
    type: LOGIN_FAILURE,
    isFetching: false,
    isAuthenticated: false,
    message
  }
}

export const loginUser = (userName, password) => dispatch => {
    dispatch(requestLogin(userName, password))

    return fetch(LOGIN_URL, {
      mode: 'cors',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: userName,
        password: password
      })
    }).then(response => {
      if (!response.ok) {
        if (response.statusText == "Unauthorized")
          dispatch(loginError("Felaktigt användarnamn eller lösenord"))
        else
          dispatch(loginError(response.statusText))
      } else {
        response.json().then(jwt => {
          localStorage.setItem('jwt', jwt.token)
          dispatch(loginSuccess(jwt.token))
          dispatch(openWebSocket())
        }).catch(err => console.warn("JSON Error: ", err))
      }
    }).catch(err => {
      dispatch(loginError("Kunde ej nå servern"))
      console.warn("Error: ", err)
    })
}

export const LOGOUT = 'LOGOUT'

export const logout = () => {
  localStorage.removeItem('jwt')
  return {
    type: LOGOUT,
    isAuthenticated: false
  }
}

/*
 * ws
 */
export const openWebSocket = () => (dispatch, getState) => {
  if (getState().auth.isAuthenticated)
    dispatch(requestWs())
}

export const REQUEST_WS = 'REQUEST_WS'

export const requestWs = () => {
  return {
    type: REQUEST_WS,
    ws: {
      readyState: WebSocket.CONNECTING
    }
  }
}

export const WS_SUCCESS = 'WS_SUCCESS'

export const wsSuccess = () => {
  return {
    type: WS_SUCCESS,
    ws: {
      readyState: WebSocket.OPEN
    }
  }
}

export const WS_FAILURE = 'WS_FAILURE'

export const wsFailure = (reason = '') => {
  return {
    type: WS_FAILURE,
    ws: {
      readyState: WebSocket.CLOSED,
      reason
    }
  }
}

