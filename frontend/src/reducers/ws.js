import {REQUEST_WS, WS_SUCCESS, WS_FAILURE, LOGOUT} from '../actions'

const ws = (
  state = {
    isPending: false,
    isReady: false,
    reason: ''
  },
  action
) => {
  switch (action.type) {
    case REQUEST_WS:
      return {
        ...state,
        isPending: true,
        isReady: false
      }
    case WS_SUCCESS:
      return {
        ...state,
        isReady: true,
        isPending: false
      }
    case WS_FAILURE:
      return {
        ...state,
        isReady: false,
        isPending: false,
        reason: action.reason,
      }
    case LOGOUT:
      return {
        ...state,
        isReady: false,
        isPending: false
      }
    default:
      return state
  }
}

export default ws

