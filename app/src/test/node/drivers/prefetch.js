'use strict';
// D3: dwell-prefetch — fires only after DWELL_MS, cancelled on leave/navigate,
// results cached in the LRU, hits logged via stats.
__SPLUX__
const assert = require('assert');
const Ux = window.__splUx;

Ux.DWELL_MS = 60; // speed up the test (production: 400)
window.spotAuthToken = 'test-token';

let fetchCalls = [];
global.fetch = (url) => {
  fetchCalls.push(url);
  return Promise.resolve({
    ok: true,
    json: () => Promise.resolve({ items: [{ track: { uri: 'spotify:track:x', name: 'X' } }], next: null }),
  });
};

const P = Ux.prefetch;
P._reset();

// 1. arm then disarm before dwell -> no fetch
P.arm('spotify:playlist:aaa');
P.disarm('spotify:playlist:aaa');

// 2. arm and let dwell elapse -> fetch fires once
P.arm('spotify:playlist:bbb');

setTimeout(() => {
  try {
    assert.strictEqual(fetchCalls.length, 1, 'one fetch after dwell, got ' + fetchCalls.length);
    assert.ok(fetchCalls[0].includes('playlists/bbb/tracks'), 'correct track-list URL: ' + fetchCalls[0]);
  } catch (e) { console.error('FAIL prefetch(1): ' + e.message); process.exit(1); }

  setTimeout(() => {
    try {
      // 3. second get() for the same uri is a cache hit (no new network)
      const before = fetchCalls.length;
      P.get('spotify:playlist:bbb').then((rec) => {
        try {
          assert.strictEqual(fetchCalls.length, before, 'cache hit -> no new fetch');
          assert.strictEqual(rec.tracks.length, 1);
          const st = P.stats();
          assert.strictEqual(st.hits, 1, 'hit logged');
          assert.strictEqual(st.misses, 1, 'miss logged');

          // 4. cancelAll clears pending dwell timers (navigation away)
          P.arm('spotify:playlist:ccc');
          P.cancelAll();
          setTimeout(() => {
            try {
              assert.strictEqual(fetchCalls.length, before, 'cancelled dwell never fetched');
              // 5. LRU cap: 21 distinct -> 20 kept
              P._reset();
              const jobs = [];
              for (let i = 0; i < 21; i++) jobs.push(P.get('spotify:playlist:p' + i).catch(() => {}));
              Promise.all(jobs).then(() => {
                try {
                  assert.strictEqual(P.cache.size(), 20, 'prefetch LRU capped at 20');
                  console.log('PASS prefetch');
                } catch (e) { console.error('FAIL prefetch(3): ' + e.message); process.exit(1); }
              });
            } catch (e) { console.error('FAIL prefetch(2): ' + e.message); process.exit(1); }
          }, 120);
        } catch (e) { console.error('FAIL prefetch(hit): ' + e.message); process.exit(1); }
      });
    } catch (e) { console.error('FAIL prefetch: ' + e.message); process.exit(1); }
  }, 60);
}, 140);
