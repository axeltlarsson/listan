import React from 'react'
import { Textfield, Button, Spinner } from 'react-mdl'
import Heading from './Heading'
import { Redirect } from 'react-router-dom'

const Login = ({
  onLogin,
  auth,
  ws,
  location: {
    state: from
  }
}) => {
  let userName = null
  let password = null

  return (
    <div>
    {auth.isAuthenticated ? (
      <Redirect to={from || '/'} />
    ) : (
      <div id="login">
      <Heading id="login-header">Logga in</Heading>
      <form
        id="login-form"
        onSubmit={(e) => {
          e.preventDefault()
          onLogin(userName.inputRef.value.trim(), password.inputRef.value)
        }}>
        <Textfield
          id="user-name"
          ref={(input) => { userName = input}}
          required
          label="Användarnamn"
          floatingLabel
        />
        <Textfield
          id="password"
          required
          ref={(input) => { password = input }}
          type="password"
          label="Lösenord"
          floatingLabel
          error={auth.errorMessage}
        />
        {auth.isFetching ?
          <Spinner /> :
          <Button id="login-button" raised colored ripple type="submit">Logga in</Button>
        }
      </form>
      </div>
    )
    }
    </div>
  )
}
export default Login

