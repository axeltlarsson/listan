import React from 'react'
import { connect } from 'react-redux'
import { logout } from '../actions'

class Logout extends React.Component {
  componentDidMount() {
    this.props.dispatch(logout())
    this.props.history.replace('/login')
  }

  render() {
    return (<h1>logout component</h1>) 
  }
}

Logout = connect()(Logout)
export default Logout
