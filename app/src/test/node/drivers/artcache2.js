'use strict';
// Round 2: artCache upgrades + netHints + detailSnap helpers + idle.
// Fake IndexedDB (in-memory) BEFORE __SPLUX__ so idb() binds to it.
(function(){
  var stores = {};
  var quotaNext = false;
  function Req(){ this.onsuccess=null; this.onerror=null; this.onupgradeneeded=null; this.result=null; this.error=null; }
  function dbObj(){
    return {
      createObjectStore:function(name){ if(!stores[name]) stores[name]=[]; },
      transaction:function(name, mode){
        var tx = { oncomplete:null, onerror:null };
        var rows = stores[name] || (stores[name]=[]);
        tx.objectStore=function(){
          return {
            put:function(v,k){
              var rq=new Req();
              setTimeout(function(){
                try{
                  if(quotaNext){ quotaNext=false; rq.error={name:'QuotaExceededError',code:22}; }
                  else {
                    var i=rows.findIndex(function(r){return r.k===k;});
                    if(i>=0) rows[i]={k:k,v:v}; else rows.push({k:k,v:v});
                    rq.result=k;
                  }
                  if(rq.error){ if(rq.onerror) rq.onerror({target:rq}); }
                  else if(rq.onsuccess) rq.onsuccess({target:rq});
                }catch(e){}
              },0);
              setTimeout(function(){ try{ if(tx.oncomplete) tx.oncomplete(); }catch(e){} },1);
              return rq;
            },
            get:function(k){
              var rq=new Req();
              setTimeout(function(){
                try{
                  var r=rows.find(function(x){return x.k===k;});
                  rq.result=r?r.v:undefined;
                  if(rq.onsuccess) rq.onsuccess({target:rq});
                }catch(e){}
              },0);
              return rq;
            },
            openCursor:function(){
              var rq=new Req(); var idx=0;
              (function next(){
                setTimeout(function(){
                  try{
                    if(idx<rows.length){
                      var r=rows[idx];
                      rq.result={ key:r.k, value:r.v, continue:function(){ idx++; next(); } };
                    } else {
                      rq.result=null;
                      setTimeout(function(){ try{ if(tx.oncomplete) tx.oncomplete(); }catch(e){} },1);
                    }
                    if(rq.onsuccess) rq.onsuccess({target:rq});
                  }catch(e){}
                },0);
              })();
              return rq;
            },
            delete:function(k){
              var rq=new Req();
              setTimeout(function(){
                try{
                  var i=rows.findIndex(function(r){return r.k===k;});
                  if(i>=0) rows.splice(i,1);
                  if(rq.onsuccess) rq.onsuccess({target:rq});
                }catch(e){}
              },0);
              return rq;
            }
          };
        };
        return tx;
      }
    };
  }
  global.indexedDB = {
    open:function(){
      var rq=new Req();
      setTimeout(function(){
        try{
          rq.result=dbObj();
          if(rq.onupgradeneeded) rq.onupgradeneeded({target:rq});
          setTimeout(function(){ try{ if(rq.onsuccess) rq.onsuccess({target:rq}); }catch(e){} },0);
        }catch(e){}
      },0);
      return rq;
    }
  };
  global.__idb = {
    setQuotaNext:function(){ quotaNext=true; },
    size:function(name){ return (stores[name]||[]).length; },
    has:function(name,k){ return (stores[name]||[]).some(function(r){return r.k===k;}); },
    ts:function(name,k){ var r=(stores[name]||[]).find(function(x){return x.k===k;}); return r?r.v.ts:null; },
    seed:function(name,k,v){ var s=stores[name]||(stores[name]=[]); s.push({k:k,v:v}); },
    clear:function(){ for(var k in stores) stores[k]=[]; }
  };
})();

// Deterministic blob URLs + controllable fetch.
var __blobN = 0;
global.URL.createObjectURL = function(){ __blobN++; return 'blob:test-'+__blobN; };
var fetchCalls = [];
global.fetch = function(url){
  fetchCalls.push(String(url));
  return Promise.resolve({ ok:true, blob:function(){ return Promise.resolve(new Blob(['img:'+url])); } });
};
global.spotAuthToken = 'test-token';

__SPLUX__

function assert(c, msg){ if(!c) throw new Error('ASSERT: '+msg); }
function sleep(ms){ return new Promise(function(r){ setTimeout(r, ms); }); }
async function pollFor(fn, timeoutMs){
  var t0=Date.now();
  while(Date.now()-t0 < timeoutMs){ if(fn()) return true; await sleep(100); }
  return fn();
}

(async function(){
  var Ux = window.__splUx;
  assert(Ux, '__splUx exists');

  // ---- netHints: one-time, idempotent, exact link set ----
  var head0 = document.head.children.length;
  assert(Ux.netHints()===true, 'netHints returns true');
  var links = document.head.children.slice(head0).filter(function(e){ return e.tagName==='LINK'; });
  assert(links.length===5, 'exactly 5 link hints, got '+links.length);
  var rels = links.map(function(l){ return l.getAttribute('rel')+'|'+l.getAttribute('href')+'|'+(l.getAttribute('crossorigin')||''); });
  assert(rels.indexOf('preconnect|https://i.scdn.co|')!==-1, 'plain preconnect i.scdn.co');
  assert(rels.indexOf('preconnect|https://i.scdn.co|anonymous')!==-1, 'crossorigin preconnect i.scdn.co');
  assert(rels.indexOf('dns-prefetch|https://i.scdn.co|')!==-1, 'dns-prefetch i.scdn.co');
  assert(rels.indexOf('preconnect|https://api.spotify.com|')!==-1, 'preconnect api.spotify.com');
  assert(rels.indexOf('dns-prefetch|https://api.spotify.com|')!==-1, 'dns-prefetch api.spotify.com');
  var head1 = document.head.children.length;
  assert(Ux.netHints()===true, 'second call returns true');
  assert(document.head.children.length===head1, 'second call adds no links (idempotent)');

  // ---- Ux.idle: runs the fn via fallback path ----
  var idleRan = false;
  Ux.idle(function(){ idleRan=true; });
  await sleep(2300);
  assert(idleRan, 'idle callback ran');

  // ---- artCache.prime: fetch-once, mem dedup, skips blob:/data: ----
  var ac = Ux.artCache;
  var f0 = fetchCalls.length;
  ac.prime('https://i.scdn.co/image/prime1');
  await sleep(300);
  assert(fetchCalls.length===f0+1, 'prime fetches once');
  assert(ac.has('https://i.scdn.co/image/prime1'), 'primed url in mem');
  ac.prime('https://i.scdn.co/image/prime1');
  await sleep(200);
  assert(fetchCalls.length===f0+1, 'second prime is a mem hit (no fetch)');
  ac.prime('blob:test-1'); ac.prime('data:image/png;base64,xx');
  await sleep(200);
  assert(fetchCalls.length===f0+1, 'blob:/data: urls never fetched');

  // ---- IDB cap 250 with timestamp-trim eviction intact ----
  __idb.clear(); ac._reset();
  for(var i=0;i<260;i++) ac.prime('https://i.scdn.co/image/cap'+i);
  var ok = await pollFor(function(){ return __idb.size('art')===250; }, 4000);
  assert(ok, 'IDB trims to 250, size='+__idb.size('art'));

  // ---- quota error: trim to half cap and retry once ----
  __idb.setQuotaNext();
  ac.prime('https://i.scdn.co/image/quota1');
  ok = await pollFor(function(){ return __idb.has('art','https://i.scdn.co/image/quota1'); }, 4000);
  assert(ok, 'quota-failed put retried and stored');
  var sz = __idb.size('art');
  assert(sz<=130, 'quota path trimmed to ~half cap, size='+sz);

  // ---- idbGet hit refreshes ts (true LRU across sessions) ----
  __idb.clear(); ac._reset();
  __idb.seed('art','https://i.scdn.co/image/ts1',{blob:new Blob(['x']), ts:1});
  var img = makeEl('img');
  ac.loadInto(img, 'https://i.scdn.co/image/ts1');
  ok = await pollFor(function(){ return __idb.ts('art','https://i.scdn.co/image/ts1')>1; }, 3000);
  assert(ok, 'ts refreshed on IDB hit');
  assert(ac.has('https://i.scdn.co/image/ts1'), 'hit promoted to mem');
  assert(String(img.src).indexOf('blob:test-')===0, 'img got object URL, src='+img.src);

  // ---- loadInto mem-hit path: no fetch, direct src ----
  var f1 = fetchCalls.length;
  ac._memSet('https://i.scdn.co/image/mem1','blob:cached-1');
  var img2 = makeEl('img');
  ac.loadInto(img2, 'https://i.scdn.co/image/mem1');
  assert(img2.src==='blob:cached-1', 'mem hit sets src directly');
  assert(fetchCalls.length===f1, 'mem hit triggers no fetch');

  // ---- detailSnap helpers ----
  assert(Ux.detailSnapSave('spotify:playlist:1','<div>x</div>',10)===true, 'save valid');
  assert(Ux.detailSnapGet('spotify:playlist:1')==='<div>x</div>', 'get hit');
  assert(Ux.detailSnapGet('spotify:playlist:2')===null, 'get miss on uri mismatch');
  assert(Ux.detailSnapSave('spotify:playlist:1','<div>y</div>',41)===false, 'reject >40 rows');
  assert(Ux.detailSnapSave('spotify:playlist:1','<div>y</div>',0)===false, 'reject 0 rows');
  assert(Ux.detailSnapSave('spotify:playlist:1','',5)===false, 'reject empty html');
  assert(Ux.detailSnapSave(null,'<div>y</div>',5)===false, 'reject null uri');
  assert(Ux.detailSnapGet('spotify:playlist:1')==='<div>x</div>', 'failed saves do not clobber');
  assert(Ux.detailSnapSave('spotify:playlist:1','<div>z</div>',3)===true, 'overwrite ok');
  assert(Ux.detailSnapGet('spotify:playlist:1')==='<div>z</div>', 'overwrite visible');
  var st = Ux.detailSnapStats();
  assert(st.has===true && st.uri==='spotify:playlist:1' && st.n===3, 'stats shape');
  Ux.detailSnapClear();
  assert(Ux.detailSnapGet('spotify:playlist:1')===null, 'clear works');
  assert(Ux.detailSnapStats().has===false, 'stats after clear');

  // ---- prefetch data arrival pre-warms artwork (first 8, smallest variant) ----
  __idb.clear(); ac._reset();
  var f2 = fetchCalls.length;
  function mkTrack(i){
    return { uri:'spotify:track:t'+i,
      album:{ images:[{url:'https://i.scdn.co/image/big'+i},{url:'https://i.scdn.co/image/small'+i}] } };
  }
  var tracks=[]; for(var ti=0; ti<10; ti++) tracks.push(mkTrack(ti));
  var fakeFetch = function(){
    return Promise.resolve({ ok:true, json:function(){ return Promise.resolve({items:tracks, next:null}); } });
  };
  await Ux.prefetch.get('spotify:playlist:warmtest', fakeFetch);
  await sleep(2600); // idle fallback delay
  var primed = fetchCalls.slice(f2);
  assert(primed.length===8, 'exactly 8 artwork urls primed, got '+primed.length);
  for(var pi=0; pi<8; pi++)
    assert(primed.indexOf('https://i.scdn.co/image/small'+pi)!==-1, 'smallest variant primed for track '+pi);
  assert(primed.indexOf('https://i.scdn.co/image/small8')===-1, 'tracks beyond 8 not primed');
  assert(primed.indexOf('https://i.scdn.co/image/big0')===-1, 'large variant not primed');

  console.log('artcache2: netHints, idle, prime, idb-cap-250, quota-retry, ts-refresh, detailSnap, prefetch-artwork-prewarm — ALL PASS');
})().catch(function(e){ console.error('FAIL: '+(e&&e.stack||e)); process.exit(1); });
