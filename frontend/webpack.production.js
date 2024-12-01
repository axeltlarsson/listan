const CommonConfig = require('./webpack.common.js');
const merge = require('webpack-merge');
const webpack = require('webpack');
const path = require('path');
// https://medium.com/webpack/predictable-long-term-caching-with-webpack-d3eee1d3fa31
const NameAllModulesPlugin = require('name-all-modules-plugin');

module.exports = merge(CommonConfig, {
  entry: {
    vendor: ['@material-ui/core', '@material-ui/icons', 'classnames', 'react', 'react-dom']
  },
  plugins: [
    new webpack.DefinePlugin({
      'process.env': {
        'NODE_ENV': JSON.stringify('production'),
        'WS_API_URL': JSON.stringify(process.env.WS_API_URL),
        'LOGIN_URL': JSON.stringify(process.env.LOGIN_URL)
      }
    }),
    new NameAllModulesPlugin(),
  ],
  devtool: 'cheap-module-source-map',
  output: {
    filename: '[name].[chunkhash].js'
  }
});

