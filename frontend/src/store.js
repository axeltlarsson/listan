import {createStore, applyMiddleware, compose} from 'redux'
import reducer from './reducers'
import thunk from 'redux-thunk'
import api from './middleware/api'

const middlewares = [thunk, api]
const composeEnhancers = window.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__ || compose
const store = createStore(
  reducer,
  composeEnhancers(applyMiddleware(...middlewares))
)

export default store

