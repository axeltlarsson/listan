import React from 'react'
import ListCard from './ListCard'
import ReactCSSTransitionGroup from 'react-addons-css-transition-group'
import withStyles from 'material-ui/styles/withStyles'
import ClearToggledButton from './ClearToggledButton'
import PropTypes from 'prop-types'

const styleSheet = {
  noLists: {
    textAlign: 'center'
  },
  clearToggled: {
    display: 'flex',
    justifyContent: 'center',
    marginBottom: '1em'
  }
}
const Lists = ({
  lists,
  pending,
  onDeleteClick,
  onListNameChange,
  onListNameEditToggle,
  classes,
  onClearToggled
}) => (
  <div id="list-list">
  {lists.length == 0 && !pending ? (
      <div className={classes.noLists}>
      <h2>Det finns inget här än</h2>
      <p>Lägg till en ny lista genom att klicka på "Ny lista" i navigationsmenyn</p>
      </div>
    ) : (
      <ReactCSSTransitionGroup
      transitionName="ex"
      transitionEnterTimeout={200}
      transitionLeaveTimeout={200}>
      {lists.map((list) =>
        <ListCard
        key={list.uuid}
        onDelete={onDeleteClick}
        onListNameChange={onListNameChange}
        onListNameEditToggle={onListNameEditToggle}
        {...list}
        />
      )}
      <div className={classes.clearToggled}>
        <ClearToggledButton onClearToggled={onClearToggled}/>
      </div>
      </ReactCSSTransitionGroup>
    )}
  </div>
)

Lists.propTypes = {
  lists: PropTypes.arrayOf(PropTypes.object).isRequired,
  pending: PropTypes.bool,
  onDeleteClick: PropTypes.func.isRequired,
  onListNameChange: PropTypes.func.isRequired,
  onListNameEditToggle: PropTypes.func.isRequired,
  onClearToggled: PropTypes.func.isRequired,
  classes: PropTypes.object.isRequired
}
export default withStyles(styleSheet)(Lists)

