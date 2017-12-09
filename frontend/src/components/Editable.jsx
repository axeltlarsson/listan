import React from 'react'
import {editItem, toggleItemEditMode} from '../actions'
import {Textfield} from 'react-mdl'

export const Editable = ({
  uuid,
  contents,
  dispatch,
  onEdit,     // function to invoke when text is edited
  onToggle,   // function to toggle edit mode
  className
}) => {
  return (
    <div className="editable">
      <Textfield
        onChange={(e) => onEdit(uuid, e.target.value)}
        onKeyDown={(e) =>{
          if (e.key == 'Enter')
            onToggle(uuid)
        }}
        autoFocus={true}
        autoComplete="off"
        defaultValue={contents}
        label=""
        onBlur={(e) => onToggle(uuid)}
        className="editable"
      />
    </div>
  )
}
export default Editable
