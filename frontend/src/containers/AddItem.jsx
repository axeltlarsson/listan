import React from 'react'
import {connect} from 'react-redux'
import {addItem} from '../actions'
import {Textfield, FABButton, Icon} from 'react-mdl'

let AddItem = ({list_uuid, dispatch}) => {
  let input
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        if (!input || !input.value.trim())
          return
        dispatch(addItem(input.value, list_uuid))
        input.value = ''
      }}
      id="add-item">
      <Textfield
        onChange={(e) => {input = e.target}}
        label="Lägg till post"
        autoComplete="off" /* it just does not look good in MD */
        onKeyDown={(e) => {
          if (e.key == 'Enter') {
            e.preventDefault()
            if (!input || !input.value.trim())
              return
            dispatch(addItem(e.target.value, list_uuid))
            e.target.value = ''
          }
        }}
      />
      <FABButton
        mini
        colored
        ripple
        type="submit" >
        <Icon name="add" />
      </FABButton>
    </form>
  )
}

AddItem = connect()(AddItem)
export default AddItem
