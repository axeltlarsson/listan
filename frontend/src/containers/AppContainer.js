import {connect} from 'react-redux'
import App from '../components/App'

const mapStateToProps = ({auth, ws}) => {
  return {
    auth,
    ws
  }
}

const AppContainer = connect(
  mapStateToProps,
  null
)(App)

export default AppContainer

