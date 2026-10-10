'use strict';
// C8: debounce — 5 rapid keystrokes -> exactly 1 search, ~250ms after the last.
__SPLUX__
const assert = require('assert');
const Ux = window.__splUx;

const firedAt = [];
const t0 = Date.now();
const d = Ux.debounce(function(q) { firedAt.push({ q, at: Date.now() - t0 }); }, 250);

d('a'); d('ab'); d('abc'); d('abcd'); d('abcde');
assert.strictEqual(d.pending(), true, 'timer pending while typing');

setTimeout(() => {
  try {
    assert.strictEqual(firedAt.length, 1, 'exactly one search fired, got ' + firedAt.length);
    assert.strictEqual(firedAt[0].q, 'abcde', 'only the last query is searched');
    assert.ok(firedAt[0].at >= 240 && firedAt[0].at < 600,
      'fired ~250ms after last keystroke, got ' + firedAt[0].at + 'ms');
    assert.strictEqual(d.pending(), false, 'no timer left pending');
    console.log('PASS debounce');
  } catch (e) { console.error('FAIL debounce: ' + e.message); process.exit(1); }
}, 700);
