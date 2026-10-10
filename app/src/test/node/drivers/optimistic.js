'use strict';
// D1: optimistic play/pause + skip state machine, incl. rollback-first design.
// Placeholders replaced by the runner: __SPLUX__, __PAINT__
__SPLUX__
const assert = require('assert');
const Ux = window.__splUx;

// --- play: optimistic paint is a lease; splUpdate reconciles ---
let opt = Ux.optBeginPlay(true, 1000);
assert.strictEqual(opt.kind, 'play');
assert.strictEqual(opt.desired, true);
assert.strictEqual(opt.deadline, 1000 + Ux.OPT_PLAY_MS);

let r = Ux.optReconcile(opt, { playing: false }, 1500);
assert.strictEqual(r.action, 'keep', 'not yet confirmed, not expired -> hold optimistic paint');
assert.strictEqual(r.paint, true);

r = Ux.optReconcile(opt, { playing: true }, 1500);
assert.strictEqual(r.action, 'confirm', 'real state flipped -> lease cleared');

r = Ux.optReconcile(opt, { playing: false }, 1000 + Ux.OPT_PLAY_MS);
assert.strictEqual(r.action, 'revert', 'deadline passed without confirmation -> revert');
assert.strictEqual(r.paint, false, 'revert paints the REAL (not playing) icon');

// --- skip: progress reset optimistically; title change confirms ---
opt = Ux.optBeginSkip('Song A', 1000);
r = Ux.optReconcile(opt, { title: 'Song A' }, 1500);
assert.strictEqual(r.action, 'keep');
r = Ux.optReconcile(opt, { title: 'Song B' }, 2000);
assert.strictEqual(r.action, 'confirm', 'new title arrived -> skip confirmed');
r = Ux.optReconcile(opt, { title: 'Song A' }, 1000 + Ux.OPT_SKIP_MS);
assert.strictEqual(r.action, 'revert', 'skip never landed -> revert + toast');

// --- DOM: the icon paint happens synchronously in the tap handler ---
__PAINT__
function mkEl() { return { innerHTML: '', __splPh: undefined }; }
document._els['spl-play'] = mkEl();
document._els['spl-play-mini'] = mkEl();
window.splPaintPlayIcon(true);
assert.ok(document._els['spl-play'].innerHTML.includes('M2.7 1a.7.7'), 'pause glyph painted');
assert.strictEqual(document._els['spl-play'].__splPh, document._els['spl-play'].innerHTML,
  '__splPh synced so splUpdate will not clobber the optimistic paint');
assert.strictEqual(document._els['spl-play-mini'].__splPh, document._els['spl-play'].innerHTML, 'mini button painted too');
window.splPaintPlayIcon(false);
assert.ok(document._els['spl-play'].innerHTML.includes('M3 1.713'), 'play glyph painted');
console.log('PASS optimistic');
