import React from 'react'
import { render } from 'react-dom'
import { Provider } from 'react-redux'
import store from './store'
import AppContainer from './containers/AppContainer'
import { openWebSocket } from './actions'
import { MuiThemeProvider, createMuiTheme } from '@material-ui/core/styles'
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom'
import { AnimatedSwitch } from 'react-router-transition'
import LoginContainer from './containers/LoginContainer'
import Heading from './components/Heading'
import Logout from './containers/Logout'
import 'react-mdl/extra/material.css'
import 'react-mdl/extra/material.js'

import './style.css'

const theme = createMuiTheme()

render(
  <Provider store={store}>
    <MuiThemeProvider theme={theme}>
      <Router>
        <Switch>
          <Route exact path="/login" component={LoginContainer} />
          <Route path="/logout" component={Logout} />
          <Route path="/" component={AppContainer} />
        </Switch>
      </Router>
    </MuiThemeProvider>
  </Provider>,
  document.getElementById('root')
)
store.dispatch(openWebSocket())

