'use strict';
// Round 2: playlist-detail DOM snapshot restore (app.js glue) + bindDetailTap.
// __SPLUX__ provides __splUx; __APPDETAIL__ provides tryRestoreDetailSnap + bindDetailTap.
__SPLUX__

__APPDETAIL__

// Stub the tap binder: record handlers per element.
var tapLog = [];
window.bindTap = function(el, fn){
  el._tapFns = el._tapFns || [];
  el._tapFns.push(fn);
};
window.playFromUri = function(uri, ctx){ tapLog.push({uri:uri, ctx:ctx}); };

function assert(c, msg){ if(!c) throw new Error('ASSERT: '+msg); }

(function(){
  var Ux = window.__splUx;
  var uri = 'spotify:playlist:snap1';
  var tracks = [
    {uri:'spotify:track:a', name:'A'},
    {uri:'spotify:track:b', name:'B'}
  ];
  // Freshness source: the prefetch LRU must hold the uri's data.
  Ux.prefetch.cache.set(uri, {tracks:tracks, next:null, ts:Date.now()});
  assert(Ux.detailSnapSave(uri, '<div class="song-row">A</div><div class="song-row">B</div>', 2)===true, 'snapshot saved');

  // ---- restore path ----
  var list = makeEl('div');
  var r0 = makeEl('div'), r1 = makeEl('div');
  list.children = [r0, r1]; // stub innerHTML does not parse; rows pre-exist
  var res = tryRestoreDetailSnap(list, uri);
  assert(res && res.next===null, 'restore returns {next:null}');
  assert(list.innerHTML==='<div class="song-row">A</div><div class="song-row">B</div>', 'innerHTML restored from snapshot');
  assert(r0._tapFns && r0._tapFns.length===1, 'row 0 rebound');
  assert(r1._tapFns && r1._tapFns.length===1, 'row 1 rebound');
  assert(window.__splDetailStats.restores===1, 'restores stat incremented');
  // Tap row 0 -> plays track a with the playlist as context.
  r0._tapFns[0]();
  assert(tapLog.length===1 && tapLog[0].uri==='spotify:track:a' && tapLog[0].ctx===uri,
    'rebound tap plays the right track with context');

  // ---- miss paths ----
  assert(tryRestoreDetailSnap(list, 'spotify:playlist:other')===null, 'uri mismatch -> null');
  Ux.prefetch.cache.del(uri);
  assert(tryRestoreDetailSnap(list, uri)===null, 'prefetch data gone -> null');
  Ux.prefetch.cache.set(uri, {tracks:tracks, next:null, ts:Date.now()});
  Ux.detailSnapClear();
  assert(tryRestoreDetailSnap(list, uri)===null, 'snapshot cleared -> null');
  assert(tryRestoreDetailSnap(null, uri)===null, 'null list -> null');

  // ---- bindDetailTap alone ----
  var row = makeEl('div');
  bindDetailTap(row, {uri:'spotify:track:z'}, 'spotify:playlist:ctx');
  assert(row._tapFns.length===1, 'bindDetailTap attaches one handler');
  tapLog = [];
  row._tapFns[0]();
  assert(tapLog.length===1 && tapLog[0].uri==='spotify:track:z' && tapLog[0].ctx==='spotify:playlist:ctx',
    'bindDetailTap plays track with context');

  console.log('detailsnap: snapshot restore + rebind + miss paths + bindDetailTap — ALL PASS');
})();
