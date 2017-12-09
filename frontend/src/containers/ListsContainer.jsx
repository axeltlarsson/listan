import {connect} from 'react-redux'
import {clearToggled, deleteList, updateListName, toggleListNameEditMode, toggleListDescrEditMode} from '../actions'
import Lists from '../components/Lists'

const mapStateToProps = (state) => {
  return {
    lists: state.lists.allIds.map(id => state.lists.byId[id]),
    pending: state.lists.pending
  }
}

const mapDispatchToProps = (dispatch) => {
  return {
    onDeleteClick: (uuid) => {
      dispatch(deleteList(uuid))
    },
    onListNameChange: (uuid, newName) => {
      dispatch(updateListName(uuid, newName))
    },
    onListNameEditToggle: (uuid) => {
      dispatch(toggleListNameEditMode(uuid))
    },
    onListDescrEditToggle: (uuid) => {
      dispatch(toggleListDescrEditMode(uuid))
    },
    onClearToggled: () => dispatch(clearToggled())
  }
}

const ListsContainer = connect(
  mapStateToProps,
  mapDispatchToProps
)(Lists)

export default ListsContainer

