import React from 'react'
import WebSocketConnectionIndicator from './WebSocketConnectionIndicator'
import AddList from '../containers/AddList'
import ListsContainer from '../containers/ListsContainer'
import LoginContainer from '../containers/LoginContainer'
import Navigation from '../containers/Navigation'
import { Route, Redirect, Switch } from 'react-router-dom'
import PropTypes from 'prop-types'

import {
  CSSTransition,
  TransitionGroup,
} from 'react-transition-group';

const App = ({
  auth,
  ws,
  location
}) => (
  <div id="app">
    {auth.isAuthenticated ? (
      <div id="main">
        <div id="main-top">
          <WebSocketConnectionIndicator isReady={ws.isReady} isPending={ws.isPending} />
          <TransitionGroup className="transition-group">
            <CSSTransition key={location.key} classNames="fade" timeout={300}>
              <section className="route-section">
                <Switch location={location}>
                  <Route path="/new" component={AddList} />
                  <Route path="/" component={ListsContainer} />
                </Switch>
              </section>
            </CSSTransition>
          </TransitionGroup>
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
  ws: PropTypes.object,
  location: PropTypes.object
}

export default App
