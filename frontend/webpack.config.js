module.exports = function(e) {
  let env = e || 'dev'
  return require(`./webpack.${env}.js`)

}

