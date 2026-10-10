'use strict';
// D3: prefetch LRU — cap 20, access refresh, eviction order.
__SPLUX__
const assert = require('assert');
const Ux = window.__splUx;

const l = Ux.lru(20);
for (let i = 0; i < 20; i++) l.set('uri' + i, { tracks: [i] });
assert.strictEqual(l.size(), 20);

// access refresh: uri0 becomes most-recently-used
assert.deepStrictEqual(l.get('uri0'), { tracks: [0] });

// inserting #21 evicts the least-recently-used (uri1), not uri0
l.set('uri20', { tracks: [20] });
assert.strictEqual(l.size(), 20, 'cap held at 20');
assert.strictEqual(l.has('uri1'), false, 'LRU victim evicted');
assert.strictEqual(l.has('uri0'), true, 'recently-read entry survived');
assert.strictEqual(l.has('uri20'), true, 'new entry present');

// updating an existing key does not grow the cache
l.set('uri0', { tracks: [99] });
assert.strictEqual(l.size(), 20);
assert.deepStrictEqual(l.get('uri0'), { tracks: [99] });

l.clear();
assert.strictEqual(l.size(), 0);
console.log('PASS lru');
