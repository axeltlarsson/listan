import {
  ADD_LIST, SERVER_ADD_LIST, UPDATE_LIST_NAME, SERVER_UPDATE_LIST_NAME, UPDATE_LIST_DESCRIPTION,
  SERVER_UPDATE_LIST_DESCRIPTION, DELETE_LIST, SERVER_DELETE_LIST, TOGGLE_LIST_NAME_EDIT_MODE,
  TOGGLE_LIST_DESCR_EDIT_MODE, FULFIL_LIST, SET_LISTS
} from '../actions'

const lists = (state = {byId: {}, allIds: [], pending: true}, action) => {
  switch (action.type) {
    case ADD_LIST: // fall-through
    case SERVER_ADD_LIST:
      return {
        ...state,
        byId: byId(state.byId, action),
        allIds: [...state.allIds, action.uuid]
      }
    case UPDATE_LIST_NAME:
    case SERVER_UPDATE_LIST_NAME:
    case UPDATE_LIST_DESCRIPTION:
    case SERVER_UPDATE_LIST_DESCRIPTION:
    case TOGGLE_LIST_NAME_EDIT_MODE:
    case TOGGLE_LIST_DESCR_EDIT_MODE:
    case FULFIL_LIST:
      return {
        ...state,
        byId: byId(state.byId, action)
      }
    case DELETE_LIST:
    case SERVER_DELETE_LIST:
      return {
        ...state,
        byId: byId(state.byId, action),
        allIds: state.allIds.filter(id => id !== action.uuid)
      }
    case SET_LISTS:
      return action.lists
    default:
      return state
  }
}
export default lists


// Handles `byId` substate, state is 'byId' object
const byId = (state, action) => {
  let substate = state[action.uuid]

  switch (action.type) {
    case ADD_LIST: // fall-through
    case SERVER_ADD_LIST:
      return {
        ...state,
        ...list(substate, action)
      }
    case UPDATE_LIST_NAME:
    case SERVER_UPDATE_LIST_NAME:
    case UPDATE_LIST_DESCRIPTION:
    case SERVER_UPDATE_LIST_DESCRIPTION:
    case TOGGLE_LIST_NAME_EDIT_MODE:
    case TOGGLE_LIST_DESCR_EDIT_MODE:
    case FULFIL_LIST:
      return {
        ...state,
        [action.uuid]: list(substate, action)
      }
    case DELETE_LIST:
    case SERVER_DELETE_LIST:
      let {[action.uuid]: toDelete, ...lists} = state
      return lists
    default:
      return state
  }
}

// Handles individual list objects
const list = (state, action) => {
  switch (action.type) {
    case ADD_LIST:
      return {
        [action.uuid]: {
          uuid: action.uuid,
          name: action.name,
          description: action.description,
          created: action.created,
          updated: action.updated,
          fulfilled: false
        }
      }
    case SERVER_ADD_LIST:
      return {
        [action.uuid]: {
          uuid: action.uuid,
          name: action.name,
          description: action.description,
          created: action.created,
          updated: action.updated,
          fulfilled: true
        }
      }
    case UPDATE_LIST_NAME:
    case SERVER_UPDATE_LIST_NAME:
      return {
        ...state,
        name: action.newName,
        updated: action.updated
      }
    case UPDATE_LIST_DESCRIPTION:
    case SERVER_UPDATE_LIST_DESCRIPTION:
      return {
        ...state,
        description: action.description,
        updated: action.updated
      }
    case TOGGLE_LIST_NAME_EDIT_MODE:
      return {
        ...state,
        updated: action.updated,
        name_edit_mode: !state.name_edit_mode
      }
    case TOGGLE_LIST_DESCR_EDIT_MODE:
      return {
        ...state,
        updated: action.updated,
        descr_edit_mode: !state.descr_edit_mode
      }
    case FULFIL_LIST:
      return {
        ...state,
        fulfilled: true
      }
    default:
      return state
  }
}


