const CommonConfig = require('./webpack.common.js');
const merge = require('webpack-merge');
const webpack = require('webpack');
const path = require('path');
// https://medium.com/webpack/predictable-long-term-caching-with-webpack-d3eee1d3fa31
const NameAllModulesPlugin = require('name-all-modules-plugin');

module.exports = merge(CommonConfig, {
  entry: {
    vendor: ['material-ui', 'material-ui-icons', 'classnames']
  },
  plugins: [
    new webpack.DefinePlugin({
      'process.env': {
        'NODE_ENV': JSON.stringify('production'),
        'WS_API_URL': JSON.stringify(process.env.WS_API_URL || 'wss://listan.axellarsson.nu/api/ws'),
        'LOGIN_URL': JSON.stringify(process.env.LOGIN_URL || 'https://listan.axellarsson.nu/api/login')
      }
    }),
    new webpack.NamedModulesPlugin(),
    new webpack.NamedChunksPlugin((chunk) => {
      if (chunk.name) {
        return chunk.name;
      }
      return chunk.modules.map(m => path.relative(m.context, m.request)).join("_");
    }),
    new webpack.optimize.CommonsChunkPlugin({
      name: 'vendor',
      minChunks: Infinity
    }),
    new webpack.optimize.CommonsChunkPlugin({
      name: 'runtime'
    }),
    new NameAllModulesPlugin(),
    new webpack.optimize.UglifyJsPlugin({})
  ],
  devtool: 'cheap-module-source-map',
  output: {
    filename: '[name].[chunkhash].js'
  }
});

