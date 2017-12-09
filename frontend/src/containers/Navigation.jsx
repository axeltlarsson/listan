import React from 'react'
import { connect } from 'react-redux'
import { withStyles } from 'material-ui/styles'
import BottomNavigation, { BottomNavigationButton } from 'material-ui/BottomNavigation'
import Icon from 'material-ui/Icon'
import { withRouter } from 'react-router-dom'
import Paper from 'material-ui/Paper'

const styles = theme => ({
  root: {
    paddingTop: 16,
    paddingBottom: 16,
    marginTop: theme.spacing.unit * 3,
    width: '100%'
  },
  navigation: {
  }
})

class Navigation extends React.Component {
  state = {
    value: this.props.location.pathname
  }

  handleChange = (event, value) => {
    this.setState({ value })
    this.props.history.push(value);
  };

  componentWillReceiveProps(nextProps) {
    this.setState({ value: nextProps.location.pathname })
  }

  render() {
    const classes = this.props.classes
    const { value } = this.state

    return (
      <Paper className={classes.root, "navigation-paper"} elevation={4} >
      <BottomNavigation
        value={value}
        onChange={this.handleChange}
        showLabels
        className={classes.navigation}
      >
        <BottomNavigationButton value="/" label="Listor" icon={<Icon>view_list</Icon>} />
        <BottomNavigationButton value="/new" label="Ny lista" icon={<Icon>add_box</Icon>} />
        <BottomNavigationButton value="/logout" label="Logga ut" icon={<Icon>exit_to_app</Icon>} />
      </BottomNavigation>
      </Paper>
    )
  }
}

export default withStyles(styles)(withRouter(Navigation))
