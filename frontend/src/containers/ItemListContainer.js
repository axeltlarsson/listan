import {connect} from 'react-redux'
import {completeItem, unCompleteItem, editItem, toggleItemEditMode, deleteItem} from '../actions'
import ItemList from '../components/ItemList'

const mapStateToProps = (state, {list_uuid}) => {
  return {
    items: state.items.filter(item => item.list_uuid === list_uuid)
  }
}

const mapDispatchToProps = (dispatch) => {
  return {
    onItemClick: (uuid, completed) => {
      if (completed)
        dispatch(unCompleteItem(uuid))
      else
        dispatch(completeItem(uuid))
    },
    onEditClick: (uuid) => {
      dispatch(toggleItemEditMode(uuid))
    },
    onDeleteClick: (uuid) => {
      dispatch(deleteItem(uuid))
    },
    onItemChange: (uuid, newContents) => {
      dispatch(editItem(uuid, newContents))
    }
  }
}
const ItemListContainer = connect(
  mapStateToProps,
  mapDispatchToProps
)(ItemList)

export default ItemListContainer
