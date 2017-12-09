import React from 'react'
import {IconButton} from 'react-mdl'
import PropTypes from 'prop-types'

const ClearToggledButton = ({onClearToggled}) => (
  <IconButton name="clear" onClick={() => onClearToggled()}/>
)

ClearToggledButton.propTypes = {
  onClearToggled: PropTypes.func.isRequired
}

export default ClearToggledButton

