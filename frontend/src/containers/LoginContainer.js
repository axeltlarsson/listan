import {connect} from 'react-redux'
import Login from '../components/Login'
import {loginUser} from '../actions'

const mapStateToProps = ({auth, ws}) => {
  return {
    auth,
    ws
  }
}

const mapDispatchToProps = (dispatch) => {
  return {
    onLogin: (userName, password) => {
      dispatch(loginUser(userName, password))
    }
  }
}

const LoginContainer = connect(
  mapStateToProps,
  mapDispatchToProps
)(Login)

export default LoginContainer

