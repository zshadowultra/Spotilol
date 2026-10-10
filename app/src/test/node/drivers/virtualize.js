'use strict';
// C7: windowing math + virtualized list behavior (recycled nodes, scroll preserved).
__SPLUX__
const assert = require('assert');
const Ux = window.__splUx;

// --- pure windowing math ---
let r = Ux.virtRange(0, 700, 56, 10, 2000);
assert.deepStrictEqual(r, { start: 0, end: 22 }, 'top of a 2000-track list');
assert.strictEqual(r.end - r.start + 1, 23, 'live rows at top');

r = Ux.virtRange(56000, 700, 56, 10, 2000);
assert.deepStrictEqual(r, { start: 990, end: 1022 }, 'middle of list');

r = Ux.virtRange(2000 * 56 - 700, 700, 56, 10, 2000);
assert.deepStrictEqual(r, { start: 1977, end: 1999 }, 'bottom clamps to list end');

r = Ux.virtRange(0, 700, 56, 10, 0);
assert.deepStrictEqual(r, { start: 0, end: -1 }, 'empty list renders nothing');

r = Ux.virtRange(-50, 700, 56, 10, 2000);
assert.strictEqual(r.start, 0, 'negative scrollTop clamps');

// --- DOM virtualization: 2000 items -> ~2 dozen live rows ---
const items = [];
for (let i = 0; i < 2000; i++) items.push({ id: 't' + i, name: 'Track ' + i });

const container = makeEl('div');
const scroller = makeEl('div');
scroller.clientHeight = 700;
scroller.scrollTop = 0;

function renderRow(el, t) {
  el.innerHTML = '';
  const nm = makeEl('div');
  nm.className = 'song-name';
  nm.innerText = t.name;
  el.appendChild(nm);
  el.__boundTo = t.id;
}

const virt = Ux.virtualize(container, scroller, items, renderRow, { rowH: 56 });
assert.strictEqual(virt.count(), 2000);
assert.ok(virt.liveCount() <= 40, 'live rows bounded, got ' + virt.liveCount());
assert.strictEqual(virt.liveCount(), 23, 'exactly viewport+overscan rows at top');
assert.deepStrictEqual(virt.range(), { start: 0, end: 22 });

// scroll position and content preserved on scroll
scroller.scrollTop = 56000;
scroller.dispatch('scroll');
assert.deepStrictEqual(virt.range(), { start: 990, end: 1022 }, 'window follows scroll');
// every visible pooled node is bound to the item at its index
const spacer = container.children[0];
let boundOk = 0;
for (const node of spacer.children) {
  if (node.style.display === 'none') continue;
  const idx = Math.round(parseFloat(node.style.top) / 56);
  assert.strictEqual(node.__boundTo, 't' + idx, 'recycled node rebound to row ' + idx);
  boundOk++;
}
assert.strictEqual(boundOk, 33, 'all 33 window rows bound');
// scroll height preserved -> scroll position never jumps
assert.strictEqual(spacer.style.height, (2000 * 56) + 'px', 'spacer keeps total height');
assert.strictEqual(scroller.scrollTop, 56000, 'scrollTop untouched by recycle');

// appendItems (pagination) keeps the window valid
virt.appendItems([{ id: 't2000', name: 'Track 2000' }]);
assert.strictEqual(virt.count(), 2001);
assert.strictEqual(spacer.style.height, (2001 * 56) + 'px');

virt.destroy();
assert.strictEqual(container.children.length, 0, 'destroy removes the spacer');
console.log('PASS virtualize');
