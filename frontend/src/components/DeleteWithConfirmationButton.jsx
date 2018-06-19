import React, { Component } from 'react';
import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';
import IconButton from '@material-ui/core/IconButton'
import Icon from '@material-ui/core/Icon';
import DeleteIcon from '@material-ui/icons/Delete'

export default class DeleteWithConfirmationButton extends Component {
  state = {
    open: false,
  };

  handleRequestClose = () => {
    this.setState({ open: false });
  };

  proceed = () => {
    this.props.onDelete()
    this.setState({ open: false });
  };

  render() {
    return (
      <div>
        <IconButton
          aria-label={this.props.title}
          onClick={() => this.setState({ open: true })}>
          <DeleteIcon />
        </IconButton>
        <Dialog
          fullscreen="true"
          open={this.state.open}
          onClose={this.handleRequestClose}
          aria-labelledby="responsive-dialog-title"
        >
          <DialogTitle id="responsive-dialog-title">
            {this.props.title}
          </DialogTitle>
          <DialogContent>
            <DialogContentText>
              {this.props.msg}
            </DialogContentText>
          </DialogContent>
          <DialogActions>
            <Button onClick={this.handleRequestClose} color="primary" autoFocus>
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
