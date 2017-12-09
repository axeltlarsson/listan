import {combineReducers} from 'redux'
import items from './items'
import auth from './auth'
import ws from './ws'
import lists from './lists'

const reducer = combineReducers({
  lists,
  items,
  auth,
  ws
})

export default reducer
