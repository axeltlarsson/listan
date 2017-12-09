import {
  ADD_ITEM, SERVER_ADD_ITEM, EDIT_ITEM, SERVER_EDIT_ITEM, COMPLETE_ITEM,
  SERVER_COMPLETE_ITEM, UNCOMPLETE_ITEM, SERVER_UNCOMPLETE_ITEM, DELETE_ITEM,
  SERVER_DELETE_ITEM, FULFIL_ITEM, CLEAR_TOGGLED, TOGGLE_ITEM_EDIT_MODE,
  SET_ITEMS
} from '../actions'

/*
 * Reducer for the list of items
 */
const items = (state = [], action) => {
  switch (action.type) {
    case ADD_ITEM: // fall-through
    case SERVER_ADD_ITEM:
      return [
        ...state,
        item(undefined, action)
      ]
    /* Actions that apply to one specific item */
    case COMPLETE_ITEM:         // fall-through
    case SERVER_COMPLETE_ITEM:  // fall-through
    case UNCOMPLETE_ITEM:       // fall-through
    case SERVER_UNCOMPLETE_ITEM:// fall-through
    case TOGGLE_ITEM_EDIT_MODE: // fall-through
    case EDIT_ITEM:             // fall-through
    case SERVER_EDIT_ITEM:      // fall-through
    case FULFIL_ITEM:
      return state.map(i => item(i, action))
    case SERVER_DELETE_ITEM:
    case DELETE_ITEM:
      return state.filter(i => i.uuid !== action.uuid)
    case CLEAR_TOGGLED:
      return state.filter(i => !i.completed)
    case SET_ITEMS:
      return action.items
    default:
      return state
  }
}

export default items

/*
 * Helper reducer for individual item
 */
const item = (state, action) => {
  switch (action.type) {
    case ADD_ITEM:
      return {
        uuid: action.uuid,
        contents: action.contents,
        completed: false,
        fulfilled: false,
        list_uuid: action.list_uuid,
        updated: action.updated,
        created: action.created
      }

    case SERVER_ADD_ITEM:
      return {
        uuid: action.uuid,
        contents: action.contents,
        completed: false,
        fulfilled: true,
        list_uuid: action.list_uuid,
        updated: action.updated,
        created: action.created
      }

    case COMPLETE_ITEM:
    case SERVER_COMPLETE_ITEM:
      if (state.uuid !== action.uuid)
        return state
      return {
        ...state,
        completed: true,
        updated: action.updated
      }

    case SERVER_UNCOMPLETE_ITEM:
    case UNCOMPLETE_ITEM:
      if (state.uuid !== action.uuid)
        return state
      return {
        ...state,
        completed: false,
        updated: action.updated
      }

    case SERVER_EDIT_ITEM:
    case EDIT_ITEM:
      if (state.uuid !== action.uuid)
        return state
      return {...state, contents: action.contents, updated: action.updated}

    case FULFIL_ITEM:
      if (state.uuid !== action.uuid)
        return state
      return {
        ...state,
        fulfilled: true
      }

    case TOGGLE_ITEM_EDIT_MODE:
      if (state.uuid !== action.uuid)
        return state
      return {...state, editMode: !state.editMode}

    default:
      return state
  }
}

