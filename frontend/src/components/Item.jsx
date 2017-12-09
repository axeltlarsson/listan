import React from 'react'
import Editable from '../components/Editable'
import {IconButton, Menu, MenuItem, Icon} from 'react-mdl'

const Item = ({
  completed,
  editMode,
  contents,
  fulfilled,
  uuid,
  onClick,
  onEditClick,
  onDeleteClick,
  onItemChange
}) => (
  <div className="item">
    {editMode ?
      <Editable
        className="item-editable"
        uuid={uuid}
        contents={contents}
        onEdit={onItemChange}
        onToggle={onEditClick}
      /> :
      <span
        className={
          completed ?
            "item-contents completed" :
            "item-contents"
        }
        onClick={onClick}>
        {contents}
      </span>
    }
  {/*Show a menu in normal mode, do not show menu in edit mode */}
  {editMode ?
    <IconButton className="item-editable-close" name="arrow_back" onClick={onEditClick} /> :
    <div className="item-menu" style={{position: 'relative'}}>
      <IconButton name="more_vert" id={"item-menu_" + uuid} />
      <Menu ripple target={"item-menu_" + uuid} align="right">
        <MenuItem onClick={onEditClick}><Icon name="mode_edit" />Ändra</MenuItem>
        <MenuItem onClick={onDeleteClick}><Icon name="delete" />Ta bort</MenuItem>
      </Menu>
    </div>
  }
  </div>
)

export default Item
