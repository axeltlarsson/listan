module.exports = function(e, argv) {
  let env = argv.mode || 'development'
  return require(`./webpack.${env}.js`)

}

