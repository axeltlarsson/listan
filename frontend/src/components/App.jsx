import React from 'react'
import WebSocketConnectionIndicator from './WebSocketConnectionIndicator'
import AddList from '../containers/AddList'
import ListsContainer from '../containers/ListsContainer'
import Navigation from '../containers/Navigation'
import { Route, Redirect, Switch } from 'react-router-dom'
import PropTypes from 'prop-types'

const App = ({
  auth,
  ws
}) => (
  <div id="app">
    {auth.isAuthenticated ? (
      <div id="main">
        <div id="main-top">
          <WebSocketConnectionIndicator isReady={ws.isReady} isPending={ws.isPending} />
          <Switch>
              <Route path="/new" component={AddList} />
              <Route path="/" component={ListsContainer} />
          </Switch>
        </div>
        <div id="main-bottom">
          <Navigation />
        </div>
      </div>
      ) : (
        <Redirect to={{ pathname: '/login' }}/>
      )
    }
  </div>
)

App.propTypes = {
  auth: PropTypes.object,
  ws: PropTypes.object
}

export default App
