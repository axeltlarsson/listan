import React from 'react'
import {connect} from 'react-redux'
import {addList} from '../actions'
import { withStyles } from '@material-ui/core/styles'
import TextField from '@material-ui/core/TextField'
import Button from '@material-ui/core/Button'
import AddIcon from '@material-ui/icons/Add'
import Heading from '../components/Heading'
import withRouter from 'react-router-dom/withRouter'

let AddList = ({dispatch, classes, history}) => {
  let name = {value: ''}
  let descr = {value: ''}

  return(
    <div id="add-list-wrapper">
      <Heading id="add-lists-header">Lägg till lista</Heading>
    <form
      onSubmit={(e) => {
        e.preventDefault()
        if (!name || !name.value.trim())
          return
        dispatch(addList(name.value, descr.value))
        name.value = ''
        descr.value = ''
        setTimeout(() => history.push('/'), 250)
      }}
      className={classes.form}
      id="add-list-form">
      <TextField
        className={classes.textField}
    id="add-list-name"
        required
        onChange={(e) => {name = e.target}}
        label="Namn"
        margin="dense"
      />
      <TextField
        id="add-list-desription"
        className={classes.textField}
        onChange={(e) => {descr = e.target}}
        label="Beskrivning"
        margin="dense"
      />
      <Button
        id="add-list-btn"
        variant="fab"
        color="secondary"
        type="submit" >
        <AddIcon />
      </Button>
    </form>
    </div>
  )
}

const styleSheet = (theme) => (
  {
    textField: {
      marginLeft: theme.spacing.unit,
      marginRight: theme.spacing.unit
    }
  }
)

AddList = connect()(AddList)
export default withStyles(styleSheet)(withRouter(AddList))
