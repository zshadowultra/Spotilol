package com.project.lol.webview.injections

object PlaybackControls {
    const val CONTENT = """
            window.playFromUri = function(uri, contextUri) {
                var playContext = contextUri || uri;
                var isLikedSongs = (playContext === 'your_library' || playContext.indexOf('collection') !== -1 || playContext === 'playlists' || playContext === 'spotify:collection:tracks');

                var playOptions = {
                    license: 'tft',
                    skip_to: {},
                    player_options_override: {}
                };

                var commandContext = {
                    uri: playContext,
                    url: 'context://' + playContext,
                    metadata: {}
                };

                var featIdent = playContext.match(/^spotify:([^:]+)/);
                featIdent = featIdent ? featIdent[1] : null;
                if (featIdent == 'user' || isLikedSongs) featIdent = 'your_library';

                if (isLikedSongs) {
                    var tracks = window.likedSongsCache || [];
                    var targetUri = (uri && uri.indexOf(':track:') !== -1) ? uri : ((tracks[0] && tracks[0].id) || uri);
                    var trackList = (tracks || []).map(function(t) { return t.id || t.uri || t; }).filter(Boolean);

                    if (targetUri && targetUri.indexOf(':track:') !== -1 && trackList.indexOf(targetUri) === -1) {
                        trackList.unshift(targetUri);
                    }

                    var collectionUri = window.spotUserId ? 'spotify:user:' + window.spotUserId + ':collection' : 'spotify:collection:tracks';

                    commandContext = {
                        uri: collectionUri,
                        url: 'context://' + collectionUri,
                        metadata: { context_description: window.splLikedName() },
                        pages: [{ page_url: 'context://' + collectionUri, tracks: trackList.map(function(u) { return { uri: u }; }) }]
                    };

                    featIdent = 'collection-tracks';

                    playOptions.skip_to = { track_uri: targetUri };
                } else if (contextUri && contextUri !== uri) {
                    playOptions.skip_to = { track_uri: uri };
                }

                (window.mngFetch || oriFetch)('https://gew4-spclient.spotify.com/connect-state/v1/player/command/from/' + window.spotDevId + '/to/' + window.spotDevId, {
                    method: 'POST',
                    headers: { 'Authorization': window.spotAuthToken, 'Client-Token': window.spotCliToken, 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        command: {
                            context: commandContext,
                            play_origin: {
                                feature_identifier: featIdent || 'your_library',
                                feature_version: featVer,
                                referrer_identifier: 'your_library'
                            },
                            options: playOptions,
                            endpoint: 'play'
                        }
                    })
                });
            };
            window.splLikedName = function() {
                try {
                    var h1 = document.querySelector('main h1');
                    if (h1 && h1.textContent && h1.textContent.trim()) return h1.textContent.trim();
                } catch(e) {}
                try {
                    var lib = window.mediaLib;
                    if (lib && lib.playlists) {
                        for (var i = 0; i < lib.playlists.length; i++) {
                            var p = lib.playlists[i];
                            if ((p.id || '').indexOf('collection') !== -1 && p.name) return p.name;
                        }
                    }
                } catch(e) {}
                return 'Liked Songs';
            };
            window.splIsLikedName = function(n) {
                n = String(n || '').toLowerCase();
                if (!n) return false;
                return /liked songs|titres lik|titres aim|j'aime|canciones que te gustan|me gusta|lieblingstitel|brani che ti piacciono|m[uú]sicas curtidas|gelikete nummers|ulubione utwory|polubione utwory|любимые треки|мне нравится|улюблені треки|お気に入りの曲|いいねした曲|좋아요 표시한 곡|喜欢的歌曲|喜愛的歌曲|喜歡的歌曲|beğenilen şarkılar|gillade l[åa]tar|tykätyt kappaleet|obl[íi]bené skladby|obľúbené skladby|kedvelt dalok|melodii apreciate|αγαπημένα τραγούδια|שירים שאהבת|पसंद किए गए गाने|เพลงที่ถูกใจ|lagu yang disukai|bài hát đã thích|харесани песни/i.test(n);
            };
            window.splEnsurePb=function(){
                var pb=window.pBtn;
                if(pb && document.documentElement.contains(pb)) return pb;
                var all=document.querySelectorAll('aside button[data-testid=control-button-playpause], button[data-testid=control-button-playpause]');
                var first=null;
                for(var i=0;i<all.length;i++){
                    if(first===null) first=all[i];
                    if(all[i].getClientRects().length>0){ first=all[i]; break; }
                }
                if(first){ window.pBtn=first; return first; }
                try{ AndBridge.dbg('e','bt auto-pause: no play button found'); }catch(e){}
                return null;
            };
            window.splPauseOutcome=function(cb){
                var s1=window.splIsPlaying();
                if(s1===false){ cb('paused'); return; }
                setTimeout(function(){
                    var s2=window.splIsPlaying();
                    if(s2===false) cb('paused');
                    else if(s1===true && s2===true) cb('playing');
                    else cb('unknown');
                },400);
            };
            window.splPauseRetry=function(tries){
                tries=tries||0;
                var cur=(typeof window.splIsPlaying==='function')?window.splIsPlaying():null;
                if(cur===false) return;
                if(tries>0 && cur!==true) return;
                var pb=window.splEnsurePb();
                if(!pb) return;
                try{ window.reqPause=true; window.ulFlag=false; }catch(e){}
                pb.click();
                setTimeout(function(){
                    window.splPauseOutcome(function(outcome){
                        if(outcome==='paused'){
                            try{ AndBridge.dbg('s','bt auto-pause: confirmed paused'); }catch(e){}
                            return;
                        }
                        if(outcome==='unknown'){
                            try{ AndBridge.dbg('w','bt auto-pause: play state unknown, not retrying'); }catch(e){}
                            return;
                        }
                        if(tries>=3){
                            try{ AndBridge.dbg('e','bt auto-pause: playback continued after '+(tries+1)+' attempts'); }catch(e){}
                            return;
                        }
                        try{ AndBridge.dbg('w','bt auto-pause: still playing, retry '+(tries+1)); }catch(e){}
                        window.splPauseRetry(tries+1);
                    });
                },900);
            };
            window.actPlayPause = function(play) {
                var pb = window.splEnsurePb();
                if (!pb) return false;
                var cur = (typeof window.splIsPlaying === 'function') ? window.splIsPlaying() : null;
                if (play === null || typeof play === 'undefined' || cur === null) {
                    pb.click();
                } else if (play === true) {
                    if (!cur) pb.click();
                } else if (play === false) {
                    window.splPauseRetry(0);
                }
                return true;
            };
            window.actSkipBack = function() {
                var bb = document.querySelector('button[data-testid=control-button-skip-back]');
                if(bb) { AndBridge.wakeUp(); bb.click(); return true; }
                return false;
            };
            window.actSkipForward = function() {
                var fb = document.querySelector('button[data-testid=control-button-skip-forward]');
                if(fb) { AndBridge.wakeUp(); fb.click(); return true; }
                return false;
            };
            window.splShuffleBtn = function() {
                // C3: cache the resolved button; re-resolve only if it left the
                // DOM. Steady-state cost per call drops from a full-document
                // button scan to one isConnected check (this runs from the 10Hz
                // splUpdate loop and readTrackState).
                var c = window.__splShuffleBtn;
                if(c && c.isConnected) return c;
                var b = document.querySelector('button[data-testid="control-button-shuffle"]');
                if(!b){
                    var sk = document.querySelector('button[data-testid="control-button-skip-back"]');
                    if(sk) {
                        var p = sk.previousElementSibling;
                        if(p && p.tagName === 'BUTTON') b = p;
                        else {
                            var f = sk.parentElement ? sk.parentElement.querySelector('button') : null;
                            if(f && f !== sk) b = f;
                        }
                    }
                }
                if(!b){
                    var bs = document.querySelectorAll('button');
                    for(var i=0;i<bs.length;i++){
                        var ic = bs[i].querySelector('svg path');
                        if(ic && (ic.getAttribute('d')||'').indexOf('M13.151.922') === 0 && !/spl-btn/.test(bs[i].className||'')) { b = bs[i]; break; }
                    }
                }
                if(b) window.__splShuffleBtn = b;
                return b || null;
            };
            window.splShuffleState = function() {
                var b = window.splShuffleBtn();
                if(!b) return 'off';
                if(b.getAttribute('aria-disabled') === 'true') return 'disabled';
                if((b.className||'').indexOf('text-bright-accent') === -1) return 'off';
                return /smart|intelligent|inteligente|intelligente|inteligentny|slim|умный|スマート|스마트|智能|akıllı|älykäs/i.test(b.getAttribute('aria-label')||'') ? 'smart' : 'shuffle';
            };
            window.actToggleShuffle = function() {
                var sb = window.splShuffleBtn();
                if(sb && sb.getAttribute('aria-disabled') !== 'true') {
                    AndBridge.wakeUp();
                    sb.click();
                }
            };
            window.splRepeatBtn = function() {
                var b = document.querySelector('button[data-testid="control-button-repeat"]');
                if(b) return b;
                var fw = document.querySelector('button[data-testid="control-button-skip-forward"]');
                if(fw && fw.nextElementSibling && fw.nextElementSibling.tagName === 'BUTTON') return fw.nextElementSibling;
                var bs = document.querySelectorAll('button');
                for(var i=0;i<bs.length;i++){
                    var ic = bs[i].querySelector('svg path');
                    if(ic && (ic.getAttribute('d')||'').indexOf('M0 4.75') === 0 && !/spl-btn/.test(bs[i].className||'')) return bs[i];
                }
                return null;
            };
            window.actRepeat = function() {
                var rb = window.splRepeatBtn();
                if(rb) {
                    if(repmode=='false') repmode='true';
                    else if(repmode=='true') repmode='mixed';
                    else repmode='false';
                    updMedia();
                    rb.click();
                }
            };
            window.actAddToFav = function() {
                 var fb = document.querySelector('div[data-testid="now-playing-widget"] button[aria-label="Save to Your Library"], div[data-testid="now-playing-widget"] button[aria-label="Remove from Your Library"]');
                if(fb) {
                    if(fb.getAttribute('aria-checked')==='false') {
                        fb.click();
                        isfav=true;
                        updMedia();
                    } else {
                        AndBridge.wakeUp();
                        fb.click();
                        // B5: one-shot context-menu watch, not a standing interval —
                        // self-clears on first hit with a 5s hard backstop (rftimeout).
                        // No event signal exists for "React rendered the playlist
                        // picker menu"; frozen by the native onPause()/pauseTimers()
                        // path (B1) if the app backgrounds mid-flow.
                        var rfint = setInterval(function(){
                            var fr = document.querySelector('#context-menu button[role=menuitemcheckbox][aria-checked=true]');
                            if(fr) {
                                clearInterval(rfint);
                                fr.click();
                                setTimeout(function(){
                                    var sb = document.querySelector('#context-menu button[type=submit]');
                                    if(sb) { sb.click(); isfav=false; updMedia(); }
                                    AndBridge.wakeOff();
                                },500);
                            }
                        },1000);
                        var rftimeout = setTimeout(function(){
                            clearInterval(rfint);
                            AndBridge.wakeOff();
                        },5000);
                    }
                }
            };
            window.actSeek = function(pos) {
                var rg = document.querySelector('div[data-testid=playback-progressbar] input[type=range]');
                if(rg) { rg.value=pos+1; rg.dispatchEvent(new Event('change',{bubbles:true})); }
            };
            window.splToast = function(msg) {
                try {
                    if (typeof window.showToast === 'function') { window.showToast(msg); return; }
                } catch(e) {}
                try {
                    var t = document.createElement('div');
                    t.textContent = String(msg);
                    t.style.cssText = 'position:fixed;left:50%;bottom:96px;transform:translateX(-50%);background:rgba(20,20,20,.95);color:#fff;padding:10px 16px;border-radius:10px;font-size:13px;z-index:2147483647;pointer-events:none;font-family:sans-serif;';
                    document.body.appendChild(t);
                    setTimeout(function(){ try{ t.parentNode.removeChild(t); }catch(e){} }, 2400);
                } catch(e) {}
            };
            /*__SPLUX_START__*/
            (function(){
                if(window.__splUx) return;
                var Ux = {};
                Ux.VERSION = '1.0';

                /* ---------- LRU cache (pure) ---------- */
                Ux.lru = function(cap){
                    var m = new Map(); cap = Math.max(1, cap|0);
                    function evict(){ var k = m.keys().next().value; m.delete(k); return k; }
                    return {
                        get: function(k){ if(!m.has(k)) return undefined; var v=m.get(k); m.delete(k); m.set(k,v); return v; },
                        set: function(k,v){ if(m.has(k)) m.delete(k); while(m.size>=cap) evict(); m.set(k,v); },
                        has: function(k){ return m.has(k); },
                        del: function(k){ m.delete(k); },
                        clear: function(){ m.clear(); },
                        size: function(){ return m.size; },
                        keys: function(){ var a=[]; m.forEach(function(v,k){ a.push(k); }); return a; }
                    };
                };

                /* ---------- debounce ---------- */
                Ux.debounce = function(fn, ms){
                    var t = null;
                    var d = function(){ var self=this, args=arguments; if(t) clearTimeout(t); t=setTimeout(function(){ t=null; fn.apply(self,args); }, ms); };
                    d.cancel = function(){ if(t){ clearTimeout(t); t=null; } };
                    d.pending = function(){ return t!==null; };
                    return d;
                };

                /* ---------- idle scheduling: requestIdleCallback with setTimeout fallback ---------- */
                Ux.idle = function(fn){
                    function run(){ try{ fn(); }catch(e){} }
                    try{
                        if(window.requestIdleCallback){ window.requestIdleCallback(run, {timeout:3000}); return; }
                    }catch(e){}
                    setTimeout(run, 2000);
                };

                /* ---------- network hints (round 2): preconnect + dns-prefetch ----------
                   One-time <link> hints for the fixed artwork/API hosts. i.scdn.co
                   serves artwork two ways (<img src> = non-CORS, artCache's
                   fetch(url,{mode:'cors'}) = CORS) and the two use separate
                   connections, so both preconnect variants are emitted; every
                   preconnect is paired with a dns-prefetch fallback. */
                Ux.netHints = function(){
                    try{
                        if(!document || !document.head) return false;
                        var ch = document.head.children || [];
                        for(var i=0;i<ch.length;i++){
                            if(ch[i] && ch[i].getAttribute && ch[i].getAttribute('id')==='spl-net-hints') return true;
                        }
                        var marker = document.createElement('meta');
                        marker.setAttribute('id','spl-net-hints');
                        document.head.appendChild(marker);
                        function link(rel, href, crossorigin){
                            var l = document.createElement('link');
                            l.setAttribute('rel', rel);
                            l.setAttribute('href', href);
                            if(crossorigin) l.setAttribute('crossorigin','anonymous');
                            document.head.appendChild(l);
                        }
                        link('preconnect','https://i.scdn.co',false);
                        link('preconnect','https://i.scdn.co',true);
                        link('dns-prefetch','https://i.scdn.co',false);
                        link('preconnect','https://api.spotify.com',false);
                        link('dns-prefetch','https://api.spotify.com',false);
                        return true;
                    }catch(e){ return false; }
                };

                /* ---------- virtualized windowing math (pure) ----------
                   Returns inclusive [start,end] row indices intersecting the viewport
                   plus overscan on both sides. */
                Ux.virtRange = function(scrollTop, viewH, rowH, overscan, count){
                    if(!(count>0) || !(rowH>0) || !(viewH>0)) return {start:0, end:-1};
                    if(scrollTop<0) scrollTop=0;
                    var first = Math.floor(scrollTop/rowH);
                    if(first>=count) first=count-1;
                    var last = Math.ceil((scrollTop+viewH)/rowH)-1;
                    if(last>=count) last=count-1;
                    if(last<0) last=0;
                    var start = first-overscan; if(start<0) start=0;
                    var end = last+overscan; if(end>=count) end=count-1;
                    return {start:start, end:end};
                };

                /* ---------- virtualized list (DOM) ----------
                   Recycles a pool of absolutely-positioned row nodes inside a spacer that
                   preserves total scroll height, so scroll position never jumps. */
                Ux._virtOverscan = 10;
                Ux.setOverscan = function(n){ Ux._virtOverscan = Math.max(0, n|0); };
                Ux.getOverscan = function(){ return Ux._virtOverscan; };

                Ux.virtualize = function(container, scroller, items, renderRow, opts){
                    opts = opts||{};
                    var doc = container.ownerDocument || document;
                    var scr = scroller || container;
                    var st = {
                        items: items||[], renderRow: renderRow,
                        rowH: opts.rowH||0, pool: [], spacer: null,
                        start: -1, end: -1, raf: 0, dead: false
                    };
                    container.style.position = 'relative';
                    var spacer = doc.createElement('div');
                    spacer.className = 'spl-virt-spacer';
                    container.appendChild(spacer);
                    st.spacer = spacer;

                    function measureRowH(){
                        if(st.rowH>0) return st.rowH;
                        try{
                            var probe = doc.createElement('div');
                            probe.style.cssText = 'position:absolute;visibility:hidden;left:0;right:0;top:0;';
                            renderRow(probe, st.items[0], 0);
                            spacer.appendChild(probe);
                            var h = probe.offsetHeight||0;
                            spacer.removeChild(probe);
                            if(h>0){ st.rowH=h; return h; }
                        }catch(e){}
                        st.rowH = 64;
                        return 64;
                    }
                    function layout(){
                        spacer.style.height = (st.items.length*st.rowH)+'px';
                    }
                    function render(){
                        st.raf = 0;
                        if(st.dead) return;
                        var rowH = measureRowH();
                        layout();
                        var viewH = scr.clientHeight || scr.offsetHeight || 600;
                        var stop = scr.scrollTop || 0;
                        var r = Ux.virtRange(stop, viewH, rowH, Ux._virtOverscan, st.items.length);
                        if(r.start===st.start && r.end===st.end) return;
                        st.start=r.start; st.end=r.end;
                        var need = r.end>=r.start ? (r.end-r.start+1) : 0;
                        while(st.pool.length<need){
                            var n = doc.createElement('div');
                            n.className = 'spl-virt-row';
                            n.style.position='absolute'; n.style.left='0'; n.style.right='0';
                            spacer.appendChild(n);
                            st.pool.push(n);
                        }
                        for(var i=0;i<st.pool.length;i++){
                            var node = st.pool[i];
                            if(i<need){
                                var idx = r.start+i;
                                node.style.display='';
                                node.style.top=(idx*rowH)+'px';
                                node.style.height=rowH+'px';
                                try{ st.renderRow(node, st.items[idx], idx); }catch(e){}
                            } else {
                                node.style.display='none';
                            }
                        }
                    }
                    function onScroll(){
                        if(st.raf) return;
                        st.raf = 1;
                        try{
                            (window.requestAnimationFrame||function(f){ f(); })(function(){ render(); });
                        }catch(e){ render(); }
                    }
                    function attach(){
                        try{ scr.addEventListener('scroll', onScroll, {passive:true}); }
                        catch(e){ scr.addEventListener('scroll', onScroll); }
                    }
                    st.update = function(){ st.start=-1; render(); };
                    st.appendItems = function(more){ if(more&&more.length){ st.items=st.items.concat(more); render(); } };
                    st.setItems = function(items2){ st.items=items2||[]; st.start=-1; st.end=-1; render(); };
                    st.liveCount = function(){ return st.pool.length; };
                    st.range = function(){ return {start:st.start, end:st.end}; };
                    st.count = function(){ return st.items.length; };
                    st.destroy = function(){
                        st.dead=true;
                        try{ scr.removeEventListener('scroll', onScroll); }catch(e){}
                        try{ container.removeChild(spacer); }catch(e){}
                        try{ container.style.position=''; }catch(e){}
                        st.pool=[];
                    };
                    measureRowH(); layout(); attach(); render();
                    return st;
                };

                /* ---------- optimistic UI core (pure state machine; D1) ----------
                   Rollback design: every optimistic paint is a lease. The splUpdate loop
                   (source of truth) reconciles each tick:
                     confirmed -> lease cleared, real state already matches;
                     expired without confirmation -> 'revert' (caller repaints real state
                        and shows the error toast);
                     otherwise -> 'keep' (caller holds the optimistic paint one more tick). */
                Ux.OPT_PLAY_MS = 2000;
                Ux.OPT_SKIP_MS = 4000;
                Ux.optBeginPlay = function(desired, now){ return {kind:'play', desired:!!desired, deadline:(now==null?Date.now():now)+Ux.OPT_PLAY_MS}; };
                Ux.optBeginSkip = function(prevTitle, now){ return {kind:'skip', prevTitle:String(prevTitle==null?'':prevTitle), deadline:(now==null?Date.now():now)+Ux.OPT_SKIP_MS}; };
                Ux.optReconcile = function(opt, truth, now){
                    if(!opt) return {action:'none'};
                    now = (now==null?Date.now():now);
                    if(opt.kind==='play'){
                        if(truth && truth.playing===opt.desired) return {action:'confirm'};
                        if(now>=opt.deadline) return {action:'revert', paint:!opt.desired};
                        return {action:'keep', paint:opt.desired};
                    }
                    if(opt.kind==='skip'){
                        var t = truth?String(truth.title==null?'':truth.title):'';
                        if(t && t!==opt.prevTitle) return {action:'confirm'};
                        if(now>=opt.deadline) return {action:'revert'};
                        return {action:'keep'};
                    }
                    return {action:'none'};
                };

                /* ---------- prefetch manager (D3): dwell-triggered, LRU cap 20 ---------- */
                Ux.PREFETCH_CAP = 20;
                Ux.DWELL_MS = 400;
                Ux.prefetch = (function(){
                    var cache = Ux.lru(Ux.PREFETCH_CAP);
                    var timers = {};   // uri -> timeout id (dwell pending)
                    var inflight = {}; // uri -> true
                    var hits = 0, misses = 0;
                    function log(m){ try{ if(window.AndBridge) AndBridge.dbg('v','[prefetch] '+m); }catch(e){} }

                    function trackListUrl(uri){
                        if(!uri) return null;
                        if(uri.indexOf('collection')!==-1) return 'https://api.spotify.com/v1/me/tracks?limit=50';
                        var m = /spotify:(playlist|album):([^:\/?#]+)/.exec(uri||'');
                        if(!m) return null;
                        return m[1]==='album'
                            ? 'https://api.spotify.com/v1/albums/'+m[2]+'/tracks?limit=50'
                            : 'https://api.spotify.com/v1/playlists/'+m[2]+'/tracks?limit=50&fields=items(track(uri,name,artists(name),album(images))),next';
                    }
                    function parseTracks(data){
                        var out=[];
                        var items=(data&&data.items)||[];
                        for(var i=0;i<items.length;i++){ var t=items[i].track||items[i]; if(t&&t.uri) out.push(t); }
                        return out;
                    }
                    function fetchImpl(uri, fetchFn){
                        var cached = cache.get(uri);
                        if(cached){ hits++; log('HIT '+uri+' ('+cached.tracks.length+' tracks)'); return Promise.resolve(cached); }
                        var url = trackListUrl(uri);
                        if(!url) return Promise.reject(new Error('no url for '+uri));
                        var token = window.spotAuthToken;
                        if(!token) return Promise.reject(new Error('no auth'));
                        if(inflight[uri]) return Promise.reject(new Error('inflight '+uri));
                        inflight[uri]=true;
                        misses++;
                        log('MISS '+uri);
                        var f = fetchFn || fetch;
                        var p = f(url,{headers:{'Authorization':token}}).then(function(r){
                            if(!r.ok) throw new Error('http '+r.status);
                            return r.json();
                        }).then(function(data){
                            var rec={tracks:parseTracks(data), next:(data&&data.next)||null, ts:Date.now()};
                            cache.set(uri, rec);
                            primeArtwork(rec.tracks);
                            return rec;
                        });
                        var done=function(){ delete inflight[uri]; };
                        p.then(done,done);
                        return p;
                    }
                    /* Round 2: pre-warm the artwork cache when prefetch data lands.
                       The first 8 tracks' (smallest-variant) artwork URLs are
                       fetched into artCache on idle, so opening the detail view
                       finds them already cached. Dwell-gated like the prefetch
                       itself — no wasted requests for un-dwelled items. */
                    function primeArtwork(tracks){
                        try{
                            if(!Ux.artCache || !Ux.artCache.prime || !tracks || !tracks.length) return;
                            var urls=[], seen={};
                            for(var i=0;i<tracks.length && urls.length<8;i++){
                                var t=tracks[i]; if(!t) continue;
                                var imgs=(t.album&&t.album.images)||[];
                                var u=imgs.length?imgs[imgs.length-1].url:'';
                                if(u && !seen[u]){ seen[u]=1; urls.push(u); }
                            }
                            if(!urls.length) return;
                            Ux.idle(function(){
                                for(var j=0;j<urls.length;j++){ try{ Ux.artCache.prime(urls[j]); }catch(e){} }
                            });
                        }catch(e){}
                    }
                    return {
                        cache: cache,
                        stats: function(){ return {hits:hits, misses:misses, size:cache.size(), inflight:Object.keys(inflight).length}; },
                        trackListUrl: trackListUrl,
                        get: function(uri, fetchFn){ return fetchImpl(uri, fetchFn); },
                        prime: function(uri, fetchFn){
                            if(!uri || cache.has(uri) || inflight[uri]) return;
                            fetchImpl(uri, fetchFn).catch(function(){});
                        },
                        arm: function(uri){      // hover/focus: fires after DWELL_MS of dwell
                            if(!uri || cache.has(uri) || timers[uri] || inflight[uri]) return;
                            timers[uri]=setTimeout(function(){ delete timers[uri]; fetchImpl(uri).catch(function(){}); }, Ux.DWELL_MS);
                        },
                        disarm: function(uri){ if(uri && timers[uri]){ clearTimeout(timers[uri]); delete timers[uri]; } },
                        cancelAll: function(){ // navigation away / memory pressure
                            for(var k in timers){ try{clearTimeout(timers[k]);}catch(e){} }
                            timers={};
                            log('cancelAll: dwell timers cleared');
                        },
                        _reset: function(){ cache.clear(); timers={}; inflight={}; hits=0; misses=0; }
                    };
                })();

                /* ---------- artwork cache (D4 fallback): memory LRU + IndexedDB ----------
                   Service-Worker caching is unavailable in this WebView (see swProbe
                   below + MEASUREMENTS-D.md), so artwork caching lives in the injected
                   layer: a memory LRU in front of an IndexedDB LRU. */
                Ux.artCache = (function(){
                    var MEM_CAP=60, IDB_CAP=250;
                    var mem = Ux.lru(MEM_CAP);
                    var dbPromise=null;
                    function log(m){ try{ if(window.AndBridge) AndBridge.dbg('v','[artCache] '+m); }catch(e){} }
                    function idb(){
                        if(dbPromise) return dbPromise;
                        dbPromise = new Promise(function(res){
                            try{
                                if(!window.indexedDB){ res(null); return; }
                                var rq=window.indexedDB.open('__splArt',1);
                                rq.onupgradeneeded=function(){ try{ rq.result.createObjectStore('art'); }catch(e){} };
                                rq.onsuccess=function(){ res(rq.result); };
                                rq.onerror=function(){ res(null); };
                            }catch(e){ res(null); }
                        });
                        return dbPromise;
                    }
                    /* Timestamp-trim eviction to `cap` entries (oldest ts first).
                       Parameterized so the quota-retry path can trim harder. */
                    function idbTrimTo(db, cap, done){
                        try{
                            var tx=db.transaction('art','readwrite');
                            var st=tx.objectStore('art');
                            var rows=[];
                            var rq=st.openCursor();
                            rq.onsuccess=function(){
                                var c=rq.result;
                                if(c){ rows.push({k:c.key, ts:(c.value&&c.value.ts)||0}); try{c.continue();}catch(e){} }
                                else {
                                    rows.sort(function(a,b){ return a.ts-b.ts; });
                                    for(var i=0;i+cap<rows.length;i++){ try{ st.delete(rows[i].k); }catch(e){} }
                                }
                            };
                            tx.oncomplete=function(){ if(done) try{done();}catch(e){} };
                            tx.onerror=function(){ if(done) try{done();}catch(e){} };
                        }catch(e){ if(done) try{done();}catch(x){} }
                    }
                    function idbTrim(db){ idbTrimTo(db, IDB_CAP, null); }
                    function idbPut(url, blob, retried){
                        idb().then(function(db){
                            if(!db) return;
                            try{
                                var tx=db.transaction('art','readwrite');
                                var st=tx.objectStore('art');
                                var rq=st.put({blob:blob, ts:Date.now()}, url);
                                rq.onerror=function(){
                                    var err=rq.error;
                                    var quota=err&&(err.name==='QuotaExceededError'||err.code===22);
                                    if(!retried && quota){
                                        log('quota exceeded, trimming to half cap and retrying');
                                        idbTrimTo(db, Math.floor(IDB_CAP/2), function(){ idbPut(url, blob, true); });
                                    }
                                };
                                tx.oncomplete=function(){ idbTrim(db); };
                            }catch(e){}
                        });
                    }
                    function idbGet(url){
                        return idb().then(function(db){
                            if(!db) return null;
                            return new Promise(function(res){
                                try{
                                    var rq=db.transaction('art','readonly').objectStore('art').get(url);
                                    rq.onsuccess=function(){
                                        var v=rq.result;
                                        var blob=v&&v.blob?v.blob:null;
                                        if(blob){
                                            /* True LRU across sessions: refresh the
                                               access timestamp on a hit. */
                                            try{
                                                var tx2=db.transaction('art','readwrite');
                                                tx2.objectStore('art').put({blob:blob, ts:Date.now()}, url);
                                            }catch(e){}
                                        }
                                        res(blob);
                                    };
                                    rq.onerror=function(){ res(null); };
                                }catch(e){ res(null); }
                            });
                        });
                    }
                    function objUrl(b){ try{ return URL.createObjectURL(b); }catch(e){ return null; } }
                    /* Fetch a URL into mem+IDB without assigning it to an <img>.
                       Used to pre-warm the cache for not-yet-rendered artwork. */
                    function fetchAndStore(url){
                        if(!url || url.indexOf('blob:')===0 || url.indexOf('data:')===0) return;
                        if(mem.has(url)) return;
                        try{
                            fetch(url,{mode:'cors'}).then(function(r){
                                if(!r.ok) throw new Error('http '+r.status);
                                return r.blob();
                            }).then(function(b){
                                var ou=objUrl(b);
                                if(ou){ mem.set(url,ou); idbPut(url,b); log('network cached'); }
                            }).catch(function(){});
                        }catch(e){}
                    }
                    return {
                        size: function(){ return mem.size(); },
                        clear: function(){ mem.clear(); log('cleared'); },
                        has: function(url){ return mem.has(url); },
                        prime: function(url){ fetchAndStore(url); },
                        /* cache-first artwork assignment; direct src on any failure */
                        loadInto: function(img, url){
                            if(!img || !url) return;
                            try{ img.setAttribute('loading','lazy'); }catch(e){}
                            try{ img.setAttribute('decoding','async'); }catch(e){}
                            var hit=mem.get(url);
                            if(hit){ img.src=hit; log('mem HIT'); return; }
                            img.src=url;
                            idbGet(url).then(function(blob){
                                if(blob){
                                    var ou=objUrl(blob);
                                    if(ou){ mem.set(url,ou); try{ img.src=ou; }catch(e){} log('idb HIT'); }
                                    return;
                                }
                                fetchAndStore(url);
                            }).catch(function(){});
                        },
                        _memSet: function(url, v){ mem.set(url,v); },
                        _reset: function(){ mem.clear(); }
                    };
                })();

                /* ---------- detail-view DOM snapshot (round 2): instant revisit ----------
                   The playlist/album detail is the only view rebuilt per visit. After
                   a successful small render we keep its HTML; on revisit with the
                   prefetch data still cached, the caller restores innerHTML + rebinds
                   taps instead of re-rendering. Single snapshot (last-visited),
                   uri-keyed. Event listeners do not survive innerHTML restore, so
                   the caller rebinds row taps by index from the cached track data. */
                Ux.DETAIL_SNAP_MAX_ROWS = 40;
                Ux._detailSnap = null;
                Ux.detailSnapSave = function(uri, html, n){
                    if(!uri || !html || !(n>0) || n>Ux.DETAIL_SNAP_MAX_ROWS) return false;
                    Ux._detailSnap = {uri:String(uri), html:String(html), n:n|0, ts:Date.now()};
                    return true;
                };
                Ux.detailSnapGet = function(uri){
                    var s=Ux._detailSnap;
                    if(!s || s.uri!==String(uri)) return null;
                    return s.html;
                };
                Ux.detailSnapClear = function(){ Ux._detailSnap=null; };
                Ux.detailSnapStats = function(){
                    var s=Ux._detailSnap;
                    return {has:!!s, uri:s?s.uri:null, n:s?s.n:0};
                };

                /* ---------- memory pressure (B3-JS): called from native onTrimMemory ---------- */
                function splLog(l,m){ try{ if(window.AndBridge) AndBridge.dbg(l,'[memPressure] '+m); }catch(e){} }
                function applyModerate(){
                    Ux.setOverscan(2);
                    try{ Ux.artCache.clear(); }catch(e){}
                    try{ Ux.prefetch.cancelAll(); }catch(e){}
                    try{ Ux.detailSnapClear(); }catch(e){}
                    try{
                        if(!document.getElementById('spl-mem-css')){
                            var s=document.createElement('style'); s.id='spl-mem-css';
                            s.textContent='.spl-mem-moderate .spl-virt-row{content-visibility:auto}';
                            (document.head||document.documentElement).appendChild(s);
                        }
                        document.documentElement.classList.add('spl-mem-moderate');
                    }catch(e){}
                }
                function applyCritical(){
                    applyModerate();
                    Ux.setOverscan(0);
                    try{
                        if(!document.getElementById('spl-memcrit-css')){
                            var s=document.createElement('style'); s.id='spl-memcrit-css';
                            s.textContent='.spl-mem-critical .song-art,.spl-mem-critical .recent-thumb,.spl-mem-critical .release-cover,.spl-mem-critical #spl-cover-img,.spl-mem-critical .spl-sicon img{display:none!important}';
                            (document.head||document.documentElement).appendChild(s);
                        }
                        document.documentElement.classList.add('spl-mem-critical');
                    }catch(e){}
                    try{ window.__splMinimalUi=true; }catch(e){}
                }
                window.__spotilol = window.__spotilol || {};
                window.__spotilol.onMemoryPressure = function(level){
                    var lv = String(level==null?'':level).toUpperCase();
                    var num = parseInt(lv,10);
                    var critical = /CRITICAL/.test(lv) || /COMPLETE/.test(lv) || num===15 || num===80;
                    var moderate = /MODERATE/.test(lv) || /LOW/.test(lv) || /UI_HIDDEN/.test(lv) || /BACKGROUND/.test(lv)
                        || num===5 || num===10 || num===20 || num===40 || num===60;
                    if(critical){ applyCritical(); splLog('w','CRITICAL ('+level+'): overscan=0, art cache dropped, prefetch cancelled, minimal UI'); }
                    else if(moderate){ applyModerate(); splLog('w','MODERATE ('+level+'): overscan=2, art cache dropped, prefetch cancelled'); }
                    else { splLog('v','ignored level '+level); }
                    return true;
                };
                window.__spotilol.clearMemoryPressure = function(){
                    try{ document.documentElement.classList.remove('spl-mem-critical','spl-mem-moderate'); }catch(e){}
                    Ux.setOverscan(10);
                    try{ window.__splMinimalUi=false; }catch(e){}
                    return true;
                };

                /* ---------- D4 spike probe: can a Service Worker register here? ----------
                   Static answer (verified in code): NO while WorkerNeutralize's
                   BlockServiceWorker pref is on (default) — navigator.serviceWorker.register
                   is overridden to return a rejected promise. This probe reports the exact
                   runtime reason for the log. */
                Ux.swProbe = function(){
                    function done(ok, reason){ return {ok:!!ok, reason:String(reason||'')}; }
                    try{
                        if(!navigator.serviceWorker) return Promise.resolve(done(false,'navigator.serviceWorker undefined'));
                        var p;
                        try{ p = navigator.serviceWorker.register('/__spl-sw-probe.js'); }
                        catch(e){ return Promise.resolve(done(false,'register threw: '+((e&&e.message)||e))); }
                        return p.then(function(){ return done(true,'registered'); },
                                       function(e){ return done(false,'register rejected: '+((e&&e.message)||e)); });
                    }catch(e){ return Promise.resolve(done(false,'probe threw: '+((e&&e.message)||e))); }
                };

                window.__splUx = Ux;
            })();
            /*__SPLUX_END__*/

        
    """
}
