import React from 'react'
import {Component} from 'react'
import { withStyles } from 'material-ui/styles'
import Card, {CardContent, CardActions} from 'material-ui/Card'
import Collapse from 'material-ui/transitions/Collapse'
import IconButton from 'material-ui/IconButton'
import ExpandMoreIcon from 'material-ui-icons/ExpandMore'
import ModeEditIcon from 'material-ui-icons/ModeEdit'
import ArrowBackIcon from 'material-ui-icons/ArrowBack'
import Typography from 'material-ui/Typography'
import classnames from 'classnames'
import ItemListContainer from '../containers/ItemListContainer'
import AddItem from '../containers/AddItem'
import DeleteWithConfirmationButton from './DeleteWithConfirmationButton'
import Editable from '../components/Editable'
import PropTypes from 'prop-types'
import {indigo} from 'material-ui/colors'

const styleSheet = (theme) =>
  ({
    card: { width: '100%' },
    cardContent: {
      paddingLeft: 0,
      paddingRight: 0,
    },
    expand: {
      transform: 'rotate(0deg)',
      transition: theme.transitions.create('transform', {
        duration: theme.transitions.duration.shortest,
      }),
    },
    heading: {
      width: '100%',
      textAlign: 'center',
      fontWeight: 100,
      fontSize: '4rem',
      color: indigo[900],
      padding: theme.spacing.unit
    },
    actions: {
      display: 'flex',
      justifyContent: 'flex-end'
    },
    expandOpen: {
      transform: 'rotate(180deg)',
    },
    flexGrow: { flex: '1 1 auto' },
  })

class ListCard extends Component {
  state = { expanded: true };

  handleExpandClick = () => {
    this.setState({ expanded: !this.state.expanded })
  };

  render() {
    const {uuid, name, classes, onDelete, name_edit_mode,
           onListNameChange, onListNameEditToggle} = this.props

    return (<div className="list-card">
      <Card className={classes.card}>
        <CardContent>
          <div className={classes.header}>
            {name_edit_mode ?
              <Editable
                className={classes.headerEditable}
                uuid={uuid}
                contents={name}
                onEdit={onListNameChange}
                onToggle={onListNameEditToggle}
              />
              :
              <Typography type="headline" className={classes.heading} component="h1">{name}</Typography>
            }
          </div>
          <div className={classes.flexGrow} />
          <CardActions disableActionSpacing className={classes.actions}>
            {name_edit_mode ?
                <IconButton onClick={() => onListNameEditToggle(uuid)}><ArrowBackIcon/></IconButton>
                :
                <IconButton onClick={() => onListNameEditToggle(uuid)}><ModeEditIcon/></IconButton>
            }
            <DeleteWithConfirmationButton
              onDelete={() => onDelete(uuid)}
              msg={"Är du säker på att du vill radera listan `" + name + "`?"}
              title={"Radera listan?"}
            />
            <IconButton
              className={classnames(classes.expand, {
                [classes.expandOpen]: this.state.expanded,
              })}
              onClick={this.handleExpandClick}
              aria-expanded={this.state.expanded}
              aria-label="Visa mer" >
              <ExpandMoreIcon />
            </IconButton>
          </CardActions>
          <Collapse in={this.state.expanded} transitionDuration="auto" unmountOnExit>
            <CardContent className={classes.cardContent}>
              <ItemListContainer list_uuid={uuid} />
              <AddItem list_uuid={uuid} />
            </CardContent>
          </Collapse>
        </CardContent>
      </Card>
      </div>
    )
  }
}

ListCard.propTypes = {
  uuid: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  name_edit_mode: PropTypes.bool,
  onListNameChange: PropTypes.func.isRequired,
  onListNameEditToggle: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired,
  classes: PropTypes.object.isRequired
}

export default withStyles(styleSheet)(ListCard)
