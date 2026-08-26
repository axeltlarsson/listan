import React from 'react'
import ReactDOM from 'react-dom'
import { Simulate } from 'react-dom/test-utils'
import { expect } from 'chai'
import Login from '../src/components/Login'

const render = (props = {}) => {
  const container = document.createElement('div')
  document.body.appendChild(container)
  ReactDOM.render(
    <Login
      onLogin={() => {}}
      auth={{ isAuthenticated: false, isFetching: false, errorMessage: '' }}
      ws={{}}
      location={{ state: undefined }}
      {...props}
    />,
    container
  )
  return container
}

describe('Login form', () => {
  let container

  afterEach(() => {
    if (container) {
      ReactDOM.unmountComponentAtNode(container)
      document.body.removeChild(container)
      container = null
    }
  })

  it('marks the fields up for password managers', () => {
    container = render()
    const user = container.querySelector('#user-name')
    const pass = container.querySelector('#password')

    expect(user.getAttribute('autocomplete')).to.equal('username')
    expect(user.getAttribute('name')).to.equal('username')
    expect(pass.getAttribute('autocomplete')).to.equal('current-password')
    expect(pass.getAttribute('name')).to.equal('password')
  })

  it('hides the password until the toggle is pressed', () => {
    container = render()
    const toggle = container.querySelector('#password-toggle')

    expect(container.querySelector('#password').getAttribute('type')).to.equal('password')

    Simulate.click(toggle)
    expect(container.querySelector('#password').getAttribute('type')).to.equal('text')

    Simulate.click(toggle)
    expect(container.querySelector('#password').getAttribute('type')).to.equal('password')
  })

  it('keeps what was typed when toggling visibility', () => {
    container = render()
    container.querySelector('#password').value = 'hunter2'

    Simulate.click(container.querySelector('#password-toggle'))

    const after = container.querySelector('#password')
    expect(after.value).to.equal('hunter2')
    expect(after.getAttribute('type')).to.equal('text')
  })

  it('does not submit the form when toggling', () => {
    // An IconButton inside a <form> defaults to type=submit, which would
    // attempt a login on every toggle.
    let submitted = false
    container = render({ onLogin: () => { submitted = true } })
    const toggle = container.querySelector('#password-toggle')

    expect(toggle.getAttribute('type')).to.equal('button')

    Simulate.click(toggle)
    expect(submitted).to.be.false
  })

  it('submits what was typed, trimming only the user name', () => {
    const seen = []
    container = render({ onLogin: (u, p) => seen.push([u, p]) })

    container.querySelector('#user-name').value = '  alice  '
    container.querySelector('#password').value = ' pw '

    Simulate.submit(container.querySelector('#login-form'))

    expect(seen).to.deep.equal([['alice', ' pw ']])
  })

  it('keeps the id on the input, which the inputRef fallback relies on', () => {
    // react-mdl renders the id onto the input itself. The onSubmit fallback
    // reads .value off that element directly; if this ever changed to a
    // wrapper, the fallback would silently yield ''.
    container = render()
    expect(document.getElementById('password').tagName).to.equal('INPUT')
    expect(document.getElementById('user-name').tagName).to.equal('INPUT')
  })
})
