'use strict';
// Minimal DOM stub so the injected UX helpers run under plain node.
// Only what __splUx / builders touch is implemented.
global.window = global;
try { global.navigator = {}; } catch (e) { /* node>=22: navigator is getter-only; tests do not need it */ }
global.location = { href: 'https://open.spotify.com/' };

function makeClassList() {
  const s = new Set();
  return {
    add(c){ s.add(c); }, remove(c){ s.delete(c); }, contains(c){ return s.has(c); },
    toggle(c, f){ if (f === undefined) f = !s.has(c); if (f) s.add(c); else s.delete(c); return f; },
    _has(c){ return s.has(c); },
  };
}
function makeEl(tag) {
  const el = {
    tagName: String(tag).toUpperCase(),
    children: [], style: {}, dataset: {},
    className: '', innerHTML: '', innerText: '', textContent: '',
    attributes: {}, parentNode: null, isConnected: true,
    _listeners: {},
    setAttribute(k, v){ this.attributes[k] = String(v); },
    getAttribute(k){ return Object.prototype.hasOwnProperty.call(this.attributes, k) ? this.attributes[k] : null; },
    removeAttribute(k){ delete this.attributes[k]; },
    appendChild(c){ this.children.push(c); c.parentNode = this; return c; },
    removeChild(c){ const i = this.children.indexOf(c); if (i >= 0) this.children.splice(i, 1); c.parentNode = null; return c; },
    insertBefore(c, ref){ const i = this.children.indexOf(ref); if (i >= 0) this.children.splice(i, 0, c); else this.children.push(c); c.parentNode = this; return c; },
    addEventListener(t, f){ (this._listeners[t] = this._listeners[t] || []).push(f); },
    removeEventListener(t, f){ const a = this._listeners[t]; if (a) { const i = a.indexOf(f); if (i >= 0) a.splice(i, 1); } },
    dispatch(t){ const a = this._listeners[t] || []; const e = { target: this }; a.forEach(f => f(e)); },
    querySelector(){ return null; },
    querySelectorAll(){ return []; },
    classList: makeClassList(),
    get offsetHeight(){ return this._offsetHeight || 0; },
    set offsetHeight(v){ this._offsetHeight = v; },
  };
  return el;
}
global.makeEl = makeEl;

global.document = {
  documentElement: makeEl('html'),
  head: makeEl('head'),
  body: makeEl('body'),
  _els: {},
  getElementById(id){ return this._els[id] || null; },
  createElement(tag){ return makeEl(tag); },
  addEventListener(){}, removeEventListener(){},
  querySelector(){ return null; }, querySelectorAll(){ return []; },
};

// No network in unit tests: every background fetch attempt must fail fast
// and be swallowed by the production catch handlers.
global.fetch = () => Promise.reject(new Error('no-network-in-test'));
