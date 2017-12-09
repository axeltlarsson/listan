import jsdom from 'jsdom';

const doc = jsdom.jsdom('<!doctype html><html><body></body></html>');
const win = doc.defaultView;
const localStorage = {
  getItem: (key) => null,
  setItem: (key, value) => null
}
const atob = (str) => str

global.document = doc;
global.window = win;
global.window.localStorage = localStorage;

Object.keys(window).forEach((key) => {
  if (!(key in global)) {
    global[key] = window[key];
  }
});
