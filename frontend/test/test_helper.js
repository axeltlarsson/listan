import { JSDOM } from 'jsdom';

const dom = new JSDOM('<!doctype html><html><body></body></html>', {
  url: 'http://localhost'
});
const win = dom.window;
const doc = win.document;

// Mock localStorage
const localStorageMock = {
  store: {},
  getItem: function(key) { return this.store[key] || null },
  setItem: function(key, value) { this.store[key] = value },
  removeItem: function(key) { delete this.store[key] },
  clear: function() { this.store = {} }
}

global.document = doc;
global.window = win;

Object.defineProperty(global.window, 'localStorage', {
  value: localStorageMock,
  writable: true
});

Object.defineProperty(global, 'localStorage', {
  value: localStorageMock,
  writable: true
});

Object.keys(win).forEach((key) => {
  if (!(key in global)) {
    global[key] = win[key];
  }
});
