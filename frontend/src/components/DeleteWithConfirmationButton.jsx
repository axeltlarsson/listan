import React, { Component } from 'react';
import Button from 'material-ui/Button';
import Dialog, {
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
} from 'material-ui/Dialog';
import IconButton from 'material-ui/IconButton'
import DeleteIcon from 'material-ui-icons/Delete'

export default class DeleteWithConfirmationButton extends Component {
  state = {
    open: false,
  };

  handleRequestClose = () => {
    this.setState({ open: false });
  };

  proceed = () => {
    this.props.onDelete()
    this.setState({open: false });
  };

  render() {
    return (
      <div>
        <IconButton
          aria-label={this.props.title}
          onClick={() => this.setState({ open: true })}>
          <DeleteIcon />
        </IconButton>
        <Dialog open={this.state.open} onRequestClose={this.handleRequestClose}>
          <DialogTitle>
            {this.props.title}
          </DialogTitle>
          <DialogContent>
            <DialogContentText>
              {this.props.msg}
            </DialogContentText>
          </DialogContent>
          <DialogActions>
            <Button onClick={this.handleRequestClose} color="primary">
              No
            </Button>
            <Button onClick={this.proceed} color="primary">
              Yes
            </Button>
          </DialogActions>
        </Dialog>
      </div>
    );
  }
}
