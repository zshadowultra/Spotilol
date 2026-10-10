'use strict';
// D5: skeleton screens match final row geometry exactly — same CSS classes on
// every box (geometry comes from the stylesheet), shimmer is overlay-only.
__SPLUX__
__CUSTOMUI_FNS__
__APPSKELETON__
const assert = require('assert');

function struct(el) {
  const cls = (el.className || '').split(/\s+/).filter(c => c && c !== 'spl-skel' && c !== 'spl-skel-row');
  let tag = (el.tagName || 'div').toLowerCase();
  if (tag === 'img') tag = 'div'; // artwork geometry comes from the CSS class, not the tag
  return [tag + '.' + cls.sort().join('.'), (el.children || []).map(struct)];
}
function flat(t, out) { out = out || []; out.push(t[0]); t[1].forEach(c => flat(c, out)); return out; }
function show(a) { return JSON.stringify(a); }

// 1. home grid card: skeleton vs final
const skCard = flat(struct(skelRecentCard()));
const reCard = flat(struct(realRecentCard('spotify:playlist:x', 'My Playlist', 'http://img/x.jpg')));
assert.deepStrictEqual(skCard, reCard,
  'recent-card geometry mismatch:\nskel ' + show(skCard) + '\nreal ' + show(reCard));

// 2. home section card: skeleton vs final
const skRel = flat(struct(skelReleaseCard()));
const reRel = flat(struct(realReleaseCard('spotify:album:y', 'Album', 'http://img/y.jpg', 'Artist')));
assert.deepStrictEqual(skRel, reRel,
  'release-card geometry mismatch:\nskel ' + show(skRel) + '\nreal ' + show(reRel));

// 3. library row: skeleton vs final
const skRow = flat(struct(skelSongRow()));
const reRow = flat(struct(libSongRow('spotify:playlist:z', 'Name', 'Playlist', 'http://img/z.jpg')));
assert.deepStrictEqual(skRow, reRow,
  'song-row geometry mismatch:\nskel ' + show(skRow) + '\nreal ' + show(reRow));

// 4. playlist-detail skeleton (app.js) uses the same .song-row family
const appSk = flat(struct(appSkelSongRow()));
const cusSkNoMore = skRow.filter(c => c !== 'button.song-more');
assert.deepStrictEqual(appSk, cusSkNoMore, 'app.js skeleton must match the library skeleton row');

// 5. the shimmer CSS must not alter the box model of skeleton rows
const css = __SKELCSS__;
const firstRule = css.slice(0, css.indexOf('}'));
['margin', 'padding', 'border', 'width', 'height', 'top:', 'left:'].forEach(prop => {
  assert.ok(!new RegExp('(^|;)\\s*' + prop).test(firstRule),
    'skeleton CSS must not change box geometry, found "' + prop + '" in: ' + firstRule);
});
assert.ok(css.includes('@keyframes'), 'shimmer keyframes present');
console.log('PASS skeleton');
