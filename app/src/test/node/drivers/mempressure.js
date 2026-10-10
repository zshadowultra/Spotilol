'use strict';
// B3-JS: window.__spotilol.onMemoryPressure(level) behavior per level.
__SPLUX__
const assert = require('assert');
const Ux = window.__splUx;

// seed state: full overscan, warm caches, a pending prefetch
Ux.setOverscan(10);
Ux.artCache._memSet('http://x/a.jpg', 'blob:a');
Ux.prefetch._reset();
Ux.DWELL_MS = 50;
window.spotAuthToken = 't';
Ux.prefetch.arm('spotify:playlist:zzz'); // pending dwell timer
const added = [];
const origAdd = document.documentElement.classList.add.bind(document.documentElement.classList);
document.documentElement.classList.add = (c) => { added.push(c); origAdd(c); };

// --- MODERATE ---
const r1 = window.__spotilol.onMemoryPressure('MODERATE');
assert.strictEqual(r1, true);
assert.strictEqual(Ux.getOverscan(), 2, 'overscan shrunk 10 -> 2');
assert.strictEqual(Ux.artCache.size(), 0, 'artwork memory cache dropped');
assert.deepStrictEqual(
  added.filter(c => c === 'spl-mem-moderate').length, 1, 'moderate CSS hook applied');
assert.strictEqual(window.__splMinimalUi, undefined, 'not minimal yet');

// --- CRITICAL ---
const r2 = window.__spotilol.onMemoryPressure('CRITICAL');
assert.strictEqual(r2, true);
assert.strictEqual(Ux.getOverscan(), 0, 'overscan 0 at critical');
assert.ok(added.includes('spl-mem-critical'), 'critical CSS hook applied');
assert.strictEqual(window.__splMinimalUi, true, 'minimal player UI flag set');

// --- numeric Android trim codes map too ---
window.__spotilol.clearMemoryPressure();
assert.strictEqual(Ux.getOverscan(), 10, 'overscan restored');
assert.strictEqual(window.__splMinimalUi, false);
window.__spotilol.onMemoryPressure(80); // TRIM_MEMORY_COMPLETE
assert.strictEqual(Ux.getOverscan(), 0, 'code 80 -> critical');
window.__spotilol.clearMemoryPressure();
window.__spotilol.onMemoryPressure(60); // TRIM_MEMORY_MODERATE
assert.strictEqual(Ux.getOverscan(), 2, 'code 60 -> moderate');

// --- unknown level is ignored, never crashes ---
window.__spotilol.onMemoryPressure('WHATEVER');
assert.strictEqual(Ux.getOverscan(), 2, 'unknown level leaves state alone');
console.log('PASS mempressure');
