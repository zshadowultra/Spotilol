'use strict';
// Round 2 timing instrumentation: the PerfMarks.kt helper (__PERF__) must
// define window.__splPerf with ordered marks, non-negative numeric deltas,
// "[spl-perf] <name> <n>ms" bridge lines, and a Date.now fallback when
// performance.now is missing.
__PERF__

const assert = require('assert');

// ---- fakes: deterministic clock, queued rAF, recording bridge ----
let clock = 1000.0;
global.performance = { now: function() { return clock; } };
const rafQueue = [];
global.requestAnimationFrame = function(cb) { rafQueue.push(cb); return rafQueue.length; };
function runRaf() { const q = rafQueue.splice(0); q.forEach(function(cb) { cb(clock); }); }
const lines = [];
global.AndBridge = { perfMark: function(m) { lines.push(String(m)); } };

try {
  const p = window.__splPerf;
  assert.ok(p, '__splPerf is defined');

  // 1. marks fire in order
  p.mark('docstart-begin');
  clock += 2.5;
  p.mark('core-begin');
  clock += 1.25;
  const names = p.marks.map(function(m) { return m.n; });
  assert.deepStrictEqual(names, ['docstart-begin', 'core-begin'],
    'marks fire in order, got ' + JSON.stringify(names));

  // 2. emitSince: delta is a number >= 0, one decimal, bridged in order
  const d = p.emitSince('docstart-begin', 'docstart-eval');
  assert.strictEqual(typeof d, 'number', 'delta is a number');
  assert.ok(d >= 0, 'delta >= 0, got ' + d);
  assert.ok(Math.abs(d - 3.75) < 1e-9, 'delta is 3.75, got ' + d);
  const last = lines[lines.length - 1];
  assert.strictEqual(last, '[spl-perf] docstart-eval 3.8ms',
    'line format, got ' + JSON.stringify(last));

  // 3. unknown mark -> -1 and no bridge line
  const nBefore = lines.length;
  const d2 = p.emitSince('no-such-mark', 'nope');
  assert.strictEqual(d2, -1, 'unknown mark returns -1');
  assert.strictEqual(lines.length, nBefore, 'unknown mark emits nothing');

  // 4. tap-play-icon path: t0 in the tap handler, emit from rAF
  const t0 = p.now();
  clock += 8.4;
  const prf = p;
  requestAnimationFrame(function() { prf.emit('tap-play-icon', prf.now() - t0); });
  runRaf();
  const tapLine = lines[lines.length - 1];
  assert.ok(tapLine.indexOf('[spl-perf] tap-play-icon ') === 0,
    'tap line prefix, got ' + JSON.stringify(tapLine));
  const tapMs = parseFloat(tapLine.split(' ').pop());
  assert.ok(tapMs >= 8.3 && tapMs < 8.5, 'tap delta ~8.4ms, got ' + tapMs);

  // 5. emit without a delta (ms === undefined) — label-only line
  p.emit('custom-event');
  assert.strictEqual(lines[lines.length - 1], '[spl-perf] custom-event',
    'label-only line, got ' + JSON.stringify(lines[lines.length - 1]));

  // 6. since() on a missing mark is -1 (no crash)
  assert.strictEqual(p.since('missing'), -1, 'since() of missing mark is -1');

  // 7. performance.now fallback: without it, now() uses Date.now()
  delete global.performance;
  const tFallback = p.now();
  assert.strictEqual(typeof tFallback, 'number', 'fallback now() is a number');
  assert.ok(tFallback > 0, 'fallback now() > 0');
  global.performance = { now: function() { return clock; } };

  // 8. bridge lines are the only side effect: one line per emit
  const emitted = p.marks.filter(function(m) { return m.n.indexOf('emit:') === 0; }).length;
  assert.strictEqual(emitted, lines.length,
    'every emit is bridged exactly once: ' + emitted + ' vs ' + lines.length);

  console.log('PASS perf');
} catch (e) { console.error('FAIL perf: ' + (e && e.message)); process.exit(1); }
