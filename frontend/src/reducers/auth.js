import {
  LOGIN_REQUEST, LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT, WS_FAILURE
} from '../actions'

const authenticated = () => {
  const jwt = localStorage.getItem('jwt')
  return jwt !== null && JSON.parse(atob(jwt.split(".")[1])).exp > Math.floor(Date.now() / 1000)
}

const auth = (
  state = {
    isFetching: false,
    isAuthenticated: authenticated(),
    jwt: localStorage.getItem('jwt')
  },
  action
) => {
  switch (action.type) {
    case LOGIN_REQUEST:
      return {...state,
        isFetching: true,
        isAuthenticated: false,
        errorMessage: ''
      }
    case LOGIN_SUCCESS:
      return {...state,
        isFetching: false,
        isAuthenticated: true,
        jwt: action.jwt,
        errorMessage: ''
      }
    case LOGIN_FAILURE:
      return {...state,
        isFetching: false,
        isAuthenticated: false,
        errorMessage: action.message
      }
    case LOGOUT:
      return {...state, isAuthenticated: false, jwt: undefined}
    default:
      return state
  }
}

export default auth

