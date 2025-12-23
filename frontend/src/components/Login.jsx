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
          // Fallback to DOM query if react-mdl inputRef is unavailable (iOS Safari)
          const userEl = document.getElementById('user-name')
          const passEl = document.getElementById('password')
          const userValue = (userName && userName.inputRef && userName.inputRef.value)
            || (userEl && userEl.querySelector('input') && userEl.querySelector('input').value)
            || ''
          const passValue = (password && password.inputRef && password.inputRef.value)
            || (passEl && passEl.querySelector('input') && passEl.querySelector('input').value)
            || ''
          onLogin(userValue.trim(), passValue)
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

