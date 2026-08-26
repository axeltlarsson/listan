import React, { useState } from 'react'
import { Textfield, Button, Spinner, IconButton } from 'react-mdl'
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
  const [showPassword, setShowPassword] = useState(false)

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
          // Fallback to DOM query if react-mdl inputRef is unavailable (iOS
          // Safari). react-mdl puts the id on the input itself, so these are
          // the inputs — not wrappers to search within.
          const userEl = document.getElementById('user-name')
          const passEl = document.getElementById('password')
          const userValue = (userName && userName.inputRef && userName.inputRef.value)
            || (userEl && userEl.value)
            || ''
          const passValue = (password && password.inputRef && password.inputRef.value)
            || (passEl && passEl.value)
            || ''
          onLogin(userValue.trim(), passValue)
        }}>
        <Textfield
          id="user-name"
          ref={(input) => { userName = input}}
          required
          name="username"
          autoComplete="username"
          label="Användarnamn"
          floatingLabel
        />
        <div id="password-field">
          <Textfield
            id="password"
            required
            ref={(input) => { password = input }}
            name="password"
            autoComplete="current-password"
            type={showPassword ? "text" : "password"}
            label="Lösenord"
            floatingLabel
            error={auth.errorMessage}
          />
          <IconButton
            id="password-toggle"
            // Default button type in a form is submit — that would log in
            // on every toggle.
            type="button"
            name={showPassword ? "visibility_off" : "visibility"}
            title={showPassword ? "Dölj lösenord" : "Visa lösenord"}
            aria-label={showPassword ? "Dölj lösenord" : "Visa lösenord"}
            aria-pressed={showPassword}
            onClick={() => setShowPassword(!showPassword)}
          />
        </div>
        {auth.isFetching ?
          <Spinner /> :
          <Button id="login-button" raised colored type="submit">Logga in</Button>
        }
      </form>
      </div>
    )
    }
    </div>
  )
}
export default Login

