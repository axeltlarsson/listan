import React from 'react'
import PropTypes from 'prop-types'
import {indigo} from '@material-ui/core/colors'
import { withStyles } from '@material-ui/core/styles'

const Heading = ({children, id, classes}) => (
  <h1 id={id} className={classes.heading}>{children}</h1>
)

const styleSheet = () =>
  ({
    heading: {
      fontSize: "4rem",
      textAlign: 'center',
      marginTop: 0,
      marginBottom: 0,
      fontWeight: 100,
      color: indigo[900]
    }
  })

Heading.propTypes = {
  id: PropTypes.string,
  classes: PropTypes.object.isRequired,
}

export default withStyles(styleSheet)(Heading)
