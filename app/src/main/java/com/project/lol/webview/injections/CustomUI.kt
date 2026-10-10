package com.project.lol.webview.injections

/*
 * CustomUI - Full-takeover custom Spotify UI.
 *
 * Loads the judge-loop-approved UI from assets/custom-ui/ and injects it into
 * the WebView, hiding Spotify's React SPA. Only the audio element is kept alive.
 *
 * Fixes vs v1:
 * - Images: art/ paths replaced with base64 data URLs (file:// blocked by
 *   mixed-content policy on https://open.spotify.com)
 * - HTML: body content extracted via regex (full document via innerHTML mangles)
 * - Live sync: polls real player state and updates UI DOM directly
 * - Controls: overrides prototype stub functions (skipTrack, toggleLike) to
 *   hit real Spotify seams via window.__bridge
 */

object CustomUI {
    const val CONTENT = """
            (function(){
                if (window.__splCustomUiLoaded) return;
                window.__splCustomUiLoaded = true;

                function hideSpotifyUi() {
                    var st = document.createElement('style');
                    st.id = 'spl-custom-ui-hide';
                    st.textContent = [
                        '#main, .Root { display: none !important; }',
                        'aside[data-testid="now-playing-bar"] { display: none !important; }',
                        '#spotilolPlayerControls { display: none !important; }',
                        'audio, video { position: fixed !important; width: 1px !important; height: 1px !important;',
                        '  opacity: 0 !important; pointer-events: none !important; }',
                        '#spl-custom-ui-root { position: fixed !important; inset: 0 !important;',
                        '  z-index: 2147483647 !important; background: #000; overflow: hidden; }'
                    ].join('\n');
                    (document.head || document.documentElement).appendChild(st);
                }

                function buildBridge() {
                    function getTrack() {
                        try {
                            var title = '', artist = '', art = '', durationMs = 0;
                            var widget = document.querySelector('[data-testid="now-playing-widget"]');
                            if (widget) {
                                var link = widget.querySelector('a[href*="/track/"]');
                                if (link) title = (link.textContent || '').trim();
                                // Artist is often in a separate element
                                var artistEl = widget.querySelector('span[class*="artist"], div[class*="artist"] a');
                                if (!artistEl) {
                                    // Fallback: look for text after title
                                    var allLinks = widget.querySelectorAll('a');
                                    for (var i = 0; i < allLinks.length; i++) {
                                        var href = allLinks[i].getAttribute('href') || '';
                                        if (href.indexOf('/artist/') === 0) {
                                            artist = (allLinks[i].textContent || '').trim();
                                            break;
                                        }
                                    }
                                } else {
                                    artist = (artistEl.textContent || '').trim();
                                }
                                var img = widget.querySelector('img');
                                if (img) art = img.src || '';
                            }
                            return { title: title, artist: artist, art: art, durationMs: durationMs };
                        } catch (e) { return { title: '', artist: '', art: '', durationMs: 0 }; }
                    }

                    function isPlaying() {
                        try {
                            if (typeof window.splIsPlaying === 'function') {
                                var s = window.splIsPlaying();
                                if (s !== null && s !== undefined) return !!s;
                            }
                            var el = document.querySelector('audio, video');
                            if (el) return !el.paused;
                        } catch (e) {}
                        return false;
                    }

                    window.__bridge = {
                        get track() { return getTrack(); },
                        get playing() { return isPlaying(); },
                        get positionMs() {
                            try {
                                var el = document.querySelector('audio, video');
                                return el ? Math.floor(el.currentTime * 1000) : 0;
                            } catch (e) { return 0; }
                        },
                        toggle: function() {
                            try {
                                if (typeof window.actPlayPause === 'function') {
                                    window.actPlayPause(isPlaying());
                                }
                            } catch (e) {}
                        },
                        next: function() {
                            try { if (typeof window.actSkipForward === 'function') window.actSkipForward(); } catch (e) {}
                        },
                        prev: function() {
                            try { if (typeof window.actSkipBack === 'function') window.actSkipBack(); } catch (e) {}
                        },
                        seek: function(ms) {
                            try { if (typeof window.actSeek === 'function') window.actSeek(ms / 1000); } catch (e) {}
                        },
                        like: function() {
                            try {
                                // Click Spotify's like button in the now-playing widget
                                var btn = document.querySelector('[data-testid="now-playing-widget"] button[aria-label*="like" i], [data-testid="now-playing-widget"] button[aria-label*="Unlike" i]');
                                if (!btn) {
                                    // Fallback: use actAddToFav if available
                                    if (typeof window.actAddToFav === 'function') { window.actAddToFav(); return; }
                                }
                                if (btn) btn.click();
                            } catch (e) {}
                        },
                        on: function() { return function() {}; }
                    };
                }

                function extractBody(html) {
                    var m = html.match(/<body[^>]*>([\s\S]*)<\/body>/i);
                    return m ? m[1] : html;
                }

                function loadAndInject() {
                    try {
                        var html = AndBridge.loadCustomUiAsset('index.html');
                        var css = AndBridge.loadCustomUiAsset('styles.css');
                        var tokens = AndBridge.loadCustomUiAsset('tokens.css');
                        var js = AndBridge.loadCustomUiAsset('app.js');

                        if (!html) {
                            console.error('[CustomUI] Failed to load index.html');
                            return;
                        }

                        hideSpotifyUi();
                        // Round 2: one-time preconnect/dns-prefetch hints for the
                        // fixed artwork + API hosts (i.scdn.co, api.spotify.com).
                        try{ if(window.__splUx && window.__splUx.netHints) window.__splUx.netHints(); }catch(e){}
                        buildBridge();

                        var root = document.createElement('div');
                        root.id = 'spl-custom-ui-root';
                        document.body.appendChild(root);

                        function injectCss(content, id) {
                            if (!content) return;
                            var st = document.createElement('style');
                            st.id = id;
                            st.textContent = content;
                            document.head.appendChild(st);
                        }
                        injectCss(tokens, 'spl-custom-ui-tokens');
                        injectCss(css, 'spl-custom-ui-styles');

                        // D5: skeleton shimmer CSS. The shimmer is a ::after overlay
                        // only — it never changes the box model, so skeleton rows
                        // keep the exact geometry of the final rows.
                        (function(){
                            if(document.getElementById('spl-skel-css')) return;
                            var sk=document.createElement('style');
                            sk.id='spl-skel-css';
                            sk.textContent='.spl-skel{position:relative;overflow:hidden;background:rgba(255,255,255,.06)!important;color:transparent!important;}'
                                +'.spl-skel::after{content:"";position:absolute;inset:0;background:linear-gradient(100deg,transparent 20%,rgba(255,255,255,.09) 50%,transparent 80%);animation:splSkelShimmer 1.25s infinite;}'
                                +'@keyframes splSkelShimmer{from{transform:translateX(-100%)}to{transform:translateX(100%)}}'
                                +'.spl-skel-row{pointer-events:none;}';
                            (document.head||document.documentElement).appendChild(sk);
                        })();

                        function splArtInto(im, url){
                            if(window.__splUx){ window.__splUx.artCache.loadInto(im, url); }
                            else { try{ im.setAttribute('loading','lazy'); }catch(e){} try{ im.setAttribute('decoding','async'); }catch(e){} im.src=url; }
                        }
                        function splDwell(el, uri){
                            if(!el || !uri || !window.__splUx) return;
                            el.addEventListener('mouseenter', function(){ window.__splUx.prefetch.arm(uri); });
                            el.addEventListener('mouseleave', function(){ window.__splUx.prefetch.disarm(uri); });
                            el.addEventListener('focus', function(){ window.__splUx.prefetch.arm(uri); });
                            el.addEventListener('blur', function(){ window.__splUx.prefetch.disarm(uri); });
                        }
                        function skelRecentCard(){
                            var card=document.createElement('div');
                            card.className='recent-card'; card.setAttribute('data-spl-skel','1');
                            var th=document.createElement('div'); th.className='recent-thumb spl-skel'; card.appendChild(th);
                            var sp=document.createElement('span'); sp.className='recent-title spl-skel'; sp.innerText='\u00a0\u00a0\u00a0\u00a0\u00a0\u00a0'; card.appendChild(sp);
                            return card;
                        }
                        function skelReleaseCard(){
                            var card=document.createElement('div');
                            card.className='release-card'; card.setAttribute('data-spl-skel','1');
                            var cv=document.createElement('div'); cv.className='release-cover spl-skel'; card.appendChild(cv);
                            var nm=document.createElement('div'); nm.className='release-name spl-skel'; nm.innerText='\u00a0\u00a0\u00a0\u00a0'; card.appendChild(nm);
                            var ar=document.createElement('div'); ar.className='release-artist spl-skel'; ar.innerText='\u00a0\u00a0'; card.appendChild(ar);
                            return card;
                        }
                        function skelSongRow(){
                            var row=document.createElement('div');
                            row.className='song-row spl-skel-row'; row.setAttribute('data-spl-skel','1');
                            var a=document.createElement('div'); a.className='song-art spl-skel'; row.appendChild(a);
                            var info=document.createElement('div'); info.className='song-info';
                            var nm=document.createElement('div'); nm.className='song-name spl-skel'; nm.innerText='\u00a0\u00a0\u00a0\u00a0\u00a0\u00a0'; info.appendChild(nm);
                            var sb=document.createElement('div'); sb.className='song-subtitle spl-skel'; sb.innerText='\u00a0\u00a0\u00a0'; info.appendChild(sb);
                            row.appendChild(info);
                            var more=document.createElement('button'); more.className='song-more spl-skel'; more.setAttribute('disabled',''); row.appendChild(more);
                            return row;
                        }
                        function realRecentCard(uri, name, img){
                            var card=document.createElement('div');
                            card.className='recent-card';
                            var __bind=(typeof window.bindTap==='function')?window.bindTap:function(el,fn){ el.addEventListener('click',fn); };
                            __bind(card,(function(u,n,im){
                                return function(){
                                    try{ if(typeof window.__splLogTap==='function'){ window.__splLogTap('playlist-card:'+n); } }catch(x){}
                                    if(typeof window.openPlaylistDetail==='function'){ window.openPlaylistDetail(u,n,im); }
                                    else if(typeof window.playPlaylist==='function'){ window.playPlaylist(u,n,im); }
                                };
                            })(uri,name,img));
                            if(img){
                                var thumb=document.createElement('img');
                                thumb.className='recent-thumb'; thumb.alt=name;
                                splArtInto(thumb,img);
                                card.appendChild(thumb);
                            } else {
                                var ph=document.createElement('div');
                                ph.className='recent-thumb';
                                ph.style.background='var(--sp-accent-congaso)';
                                card.appendChild(ph);
                            }
                            var span=document.createElement('span');
                            span.className='recent-title'; span.innerText=name;
                            card.appendChild(span);
                            splDwell(card,uri);
                            return card;
                        }
                        function realReleaseCard(uri, name, img, sub){
                            var card=document.createElement('div');
                            card.className='release-card';
                            var __bind2=(typeof window.bindTap==='function')?window.bindTap:function(el,fn){ el.addEventListener('click',fn); };
                            __bind2(card,(function(u,n,im){
                                return function(){
                                    try{ if(typeof window.__splLogTap==='function'){ window.__splLogTap('release-card:'+n); } }catch(x){}
                                    if(typeof window.playPlaylist==='function'){ window.playPlaylist(u,n,im); }
                                    try{ if(typeof window.showToast==='function'){ window.showToast(n); } }catch(y){}
                                };
                            })(uri,name,img));
                            if(img){
                                var cover=document.createElement('img');
                                cover.className='release-cover'; cover.alt=name;
                                splArtInto(cover,img);
                                card.appendChild(cover);
                            } else {
                                var phc=document.createElement('div');
                                phc.className='release-cover';
                                phc.style.background='var(--sp-accent-congaso)';
                                card.appendChild(phc);
                            }
                            var nm=document.createElement('div');
                            nm.className='release-name'; nm.innerText=name;
                            card.appendChild(nm);
                            var ar=document.createElement('div');
                            ar.className='release-artist'; ar.innerText=sub||'';
                            card.appendChild(ar);
                            splDwell(card,uri);
                            return card;
                        }
                        function libSongRow(uri, name, sub, img){
                            var row=document.createElement('div');
                            row.className='song-row';
                            if(img){
                                var a=document.createElement('img');
                                a.className='song-art'; a.alt=name;
                                splArtInto(a,img);
                                row.appendChild(a);
                            } else {
                                var ph=document.createElement('div');
                                ph.className='song-art';
                                ph.style.background='var(--sp-accent-congaso)';
                                row.appendChild(ph);
                            }
                            var info=document.createElement('div'); info.className='song-info';
                            var nm=document.createElement('div'); nm.className='song-name'; nm.innerText=name; info.appendChild(nm);
                            var sb=document.createElement('div'); sb.className='song-subtitle'; sb.innerText=sub||''; info.appendChild(sb);
                            row.appendChild(info);
                            var more=document.createElement('button'); more.className='song-more'; more.innerText='\u22ee';
                            var __b=(typeof window.bindTap==='function')?window.bindTap:function(el,fn){ el.addEventListener('click',fn); };
                            __b(more,function(e){ if(e&&e.stopPropagation) e.stopPropagation(); if(typeof window.showToast==='function') window.showToast(name); });
                            row.appendChild(more);
                            __b(row,function(){ if(typeof window.openPlaylistDetail==='function') window.openPlaylistDetail(uri,name,img); });
                            splDwell(row,uri);
                            return row;
                        }
                        var __splHomeSkel=false;
                        function clearSkel(root){
                            try{
                                var sks=root.querySelectorAll('[data-spl-skel]');
                                for(var i=0;i<sks.length;i++){ if(sks[i].parentNode) sks[i].parentNode.removeChild(sks[i]); }
                            }catch(e){}
                        }
                        function showHomeSkeletons(){
                            if(__splHomeSkel) return; __splHomeSkel=true;
                            try{
                                var grid=document.getElementById('homeGrid');
                                if(grid){
                                    var liked=grid.children[0]||null;
                                    grid.innerHTML='';
                                    if(liked) grid.appendChild(liked);
                                    for(var i=0;i<4;i++) grid.appendChild(skelRecentCard());
                                }
                                var homeView=document.querySelector('#tabHome .home-view');
                                if(homeView){
                                    var titles=homeView.querySelectorAll('.section-title');
                                    for(var t=0;t<titles.length;t++){
                                        if(/pre-save/i.test(titles[t].textContent||'')){
                                            var sib=titles[t].nextElementSibling;
                                            if(sib&&sib.classList&&sib.classList.contains('release-scroll')){
                                                sib.innerHTML='';
                                                for(var k=0;k<6;k++) sib.appendChild(skelReleaseCard());
                                            }
                                            break;
                                        }
                                    }
                                }
                            }catch(e){}
                        }
                        function loadRealLibraryContent(){
                            try{
                                if(typeof window.fetchAllLibrary!=='function'||typeof window.parseLibrary!=='function') return;
                                var list=document.querySelector('#tabLibrary .song-list');
                                if(!list||list.getAttribute('data-spl-real')==='1') return;
                                list.setAttribute('data-spl-real','1');
                                list.innerHTML='';
                                for(var i=0;i<8;i++) list.appendChild(skelSongRow());
                                window.fetchAllLibrary().then(function(items){
                                    try{
                                        var lib=window.parseLibrary(items);
                                        if(!lib){ clearSkel(list); return; }
                                        list.innerHTML='';
                                        var n=0;
                                        function push(uri,name,sub,img){
                                            if(n>=60) return;
                                            n++;
                                            list.appendChild(libSongRow(uri,name,sub,img));
                                        }
                                        var pls=lib.playlists||[];
                                        for(var p=0;p<pls.length;p++){
                                            var it=pls[p]; var pid=it.id||'';
                                            if(pid.indexOf('collection')!==-1) push(pid,it.name||'Liked Songs','Playlist','https://misc.scdn.co/liked-songs/liked-songs-640.png');
                                            else push(pid,it.name||'Playlist','Playlist',it.image||'');
                                        }
                                        var als=lib.albums||[];
                                        for(var a=0;a<als.length&&n<60;a++){ var al=als[a]; push(al.id||'',al.name||'Album','Album'+(((al.artists||[]).length)?' \u00b7 '+al.artists.join(', '):''),al.image||''); }
                                        var ars=lib.artists||[];
                                        for(var r=0;r<ars.length&&n<60;r++){ var ar2=ars[r]; push(ar2.id||'',ar2.name||'Artist','Artist',ar2.image||''); }
                                    }catch(e){ clearSkel(list); }
                                }).catch(function(){ clearSkel(list); });
                            }catch(e){}
                        }

                        // Extract body content and fix art/ paths with base64
                        var bodyHtml = extractBody(html);

                        // Replace art/ references with base64 data URLs
                        var artFiles = ['1465847899084-d164df4dedc6.jpg','1470225620780-dba8ba36b745.jpg',
                            '1478737270239-2f02b77fc618.jpg','1493225457124-a3eb161ffa5f.jpg',
                            '1507525428034-b723cf961d3e.jpg','1509198397868-475647b2a1e5.jpg',
                            '1511671782779-c97d3d27a1d4.jpg','1514525253161-7a46d19cd819.jpg',
                            '1518709268805-4e9042af9f23.jpg'];
                        artFiles.forEach(function(f) {
                            try {
                                var b64 = AndBridge.loadCustomUiAssetBase64('art/' + f);
                                if (b64) {
                                    var dataUrl = 'data:image/jpeg;base64,' + b64;
                                    bodyHtml = bodyHtml.split('art/' + f).join(dataUrl);
                                    if (js) js = js.split('art/' + f).join(dataUrl);
                                }
                            } catch (e) {}
                        });

                        root.innerHTML = bodyHtml;

                        // Inject JS - functions become global
                        if (js) {
                            var script = document.createElement('script');
                            script.textContent = js;
                            document.body.appendChild(script);
                        }

                        // Override prototype stubs with real bridge calls
                        // Must run after app.js defines the functions
                        setTimeout(function() {
                            try {
                                // skipTrack was a toast-only stub - wire to real
                                window.skipTrack = function(dir) {
                                    if (dir > 0) window.__bridge.next();
                                    else window.__bridge.prev();
                                };
                                // toggleLike was local-only - wire to real
                                var origToggleLike = window.toggleLike;
                                window.toggleLike = function(e) {
                                    if (e) e.stopPropagation();
                                    window.__bridge.like();
                                    // Still update UI optimistically
                                    if (origToggleLike) {
                                        try { origToggleLike.call(window, null); } catch (x) {}
                                        // Revert the local flip since real state will sync
                                        // Actually keep it - the sync loop will correct if wrong
                                    }
                                };
                            } catch (e) { console.error('[CustomUI] override failed', e); }
                        }, 100);

                        // Load the user's real library content into the home view.
                        // - Grid: keeps the Liked Songs card, adds up to 6 real
                        //   playlists (deduped: the API also returns the Liked
                        //   Songs collection). Taps open the playlist detail.
                        // - Sections: replaces the fake "Pre-save upcoming
                        //   releases" block with real "Your Albums" and
                        //   "Your Artists" sections from the parsed library.
                        // Same CSS classes everywhere, so the approved visual
                        // design is untouched.
                        function loadRealHomeContent() {
                            try {
                                if (typeof window.fetchAllLibrary !== 'function') return;
                                if (typeof window.parseLibrary !== 'function') return;
                                showHomeSkeletons();
                                window.fetchAllLibrary().then(function(items) {
                                    try {
                                        var lib = window.parseLibrary(items);
                                        if (!lib) return;

                                        // ---- Home grid ----
                                        var grid = document.getElementById('homeGrid');
                                        if (grid && lib.playlists && lib.playlists.length) {
                                            // Keep the Liked Songs card (first child), replace the rest
                                            var likedCard = grid.children[0] || null;
                                            grid.innerHTML = '';
                                            if (likedCard) grid.appendChild(likedCard);
                                            var seen = 0;
                                            for (var i = 0; i < lib.playlists.length && seen < 6; i++) {
                                                var pl = lib.playlists[i];
                                                var pid = pl.id || '';
                                                var pname = pl.name || '';
                                                // Dedupe: skip the Liked Songs collection the API also returns
                                                if (pid.indexOf('collection') !== -1) continue;
                                                if (/liked songs/i.test(pname)) continue;
                                                seen++;
                                                grid.appendChild(realRecentCard(pid, pname || 'Playlist', pl.image || ''));
                                            }
                                            console.log('[CustomUI] home grid populated with ' + seen + ' real playlists');
                                        }

                                        // ---- Real sections (replaces fakes) ----
                                        replaceFakeSections(lib);

                                        // Round 2: pre-warm the artwork cache on idle for the
                                        // library screen — the same playlist/album/artist art
                                        // recurs across Home/Library, so the first Library
                                        // visit finds it already cached. One-shot per boot.
                                        try{
                                            if(window.__splUx && window.__splUx.artCache && window.__splUx.artCache.prime && !window.__splHomeWarmed){
                                                window.__splHomeWarmed = true;
                                                var warmUrls=[], warmSeen={};
                                                function warmPush(u){ if(u && !warmSeen[u] && warmUrls.length<12){ warmSeen[u]=1; warmUrls.push(u); } }
                                                (lib.playlists||[]).forEach(function(p){ warmPush(p.image); });
                                                (lib.albums||[]).forEach(function(a){ warmPush(a.image); });
                                                (lib.artists||[]).forEach(function(a){ warmPush(a.image); });
                                                if(warmUrls.length){
                                                    window.__splUx.idle(function(){
                                                        for(var wi=0;wi<warmUrls.length;wi++){
                                                            try{ window.__splUx.artCache.prime(warmUrls[wi]); }catch(e){}
                                                        }
                                                    });
                                                }
                                            }
                                        }catch(e){}
                                    } catch (e) { console.error('[CustomUI] home populate failed', e); }
                                }).catch(function(e){ try{ var g=document.getElementById('homeGrid'); if(g) clearSkel(g); }catch(x){} });
                            } catch (e) {}
                        }
                        // Removes the fake "Pre-save upcoming releases" block
                        // and builds "Your Albums" / "Your Artists" from the
                        // real library. Idempotent: previously built sections
                        // are tagged and removed before rebuilding (the loader
                        // fires twice as a fallback).
                        function replaceFakeSections(lib) {
                            try {
                                var homeView = document.querySelector('#tabHome .home-view');
                                if (!homeView) return;

                                // Remove sections built by a previous run
                                var old = homeView.querySelectorAll('[data-spl-real-section]');
                                for (var r = 0; r < old.length; r++) {
                                    if (old[r].parentNode) old[r].parentNode.removeChild(old[r]);
                                }

                                // Find and remove the fake block
                                var anchor = null;
                                var titles = homeView.querySelectorAll('.section-title');
                                for (var t = 0; t < titles.length; t++) {
                                    if (/pre-save/i.test(titles[t].textContent || '')) {
                                        anchor = titles[t];
                                        var sib = anchor.nextElementSibling;
                                        if (sib && sib.classList && sib.classList.contains('release-scroll')) {
                                            if (sib.parentNode) sib.parentNode.removeChild(sib);
                                        }
                                        if (anchor.parentNode) anchor.parentNode.removeChild(anchor);
                                        anchor = null;
                                        break;
                                    }
                                }

                                function buildSection(heading, items, subOf) {
                                    if (!items || !items.length) return; // never show fakes
                                    var h = document.createElement('h2');
                                    h.className = 'section-title';
                                    h.setAttribute('data-spl-real-section', '1');
                                    h.innerText = heading;
                                    var scroll = document.createElement('div');
                                    scroll.className = 'release-scroll';
                                    scroll.setAttribute('data-spl-real-section', '1');
                                    items.slice(0, 10).forEach(function(it) {
                                        var uri = it.id || '';
                                        var name = it.name || heading;
                                        var img = it.image || '';
                                        var sub = subOf ? (it[subOf] || []).join(', ') : '';
                                        scroll.appendChild(realReleaseCard(uri, name, img, sub));
                                    });
                                    // Insert where the fake block was, else append
                                    if (anchor && anchor.parentNode) {
                                        anchor.parentNode.insertBefore(h, anchor);
                                        anchor.parentNode.insertBefore(scroll, anchor);
                                    } else {
                                        homeView.appendChild(h);
                                        homeView.appendChild(scroll);
                                    }
                                }

                                buildSection('Your Albums', lib.albums, 'artists');
                                buildSection('Your Artists', lib.artists, null);
                                console.log('[CustomUI] real sections built');
                            } catch (e) { console.error('[CustomUI] sections failed', e); }
                        }
                        // Defer: library fetch needs Spotify auth tokens to be ready
                        showHomeSkeletons();
                        setTimeout(loadRealHomeContent, 3000);
                        setTimeout(loadRealHomeContent, 8000);
                        setTimeout(loadRealLibraryContent, 3500);
                        setTimeout(loadRealLibraryContent, 9000);

                        // Live sync: poll real state and update UI
                        var lastTitle = '', lastPlaying = null;
                        var NOTHING_PLAYING = 'Nothing playing';
                        setInterval(function() {
                            try {
                                var track = window.__bridge.track;
                                var playing = window.__bridge.playing;

                                // Update track info if changed (or if nothing is playing)
                                var curTitle = (track.title || '').trim();
                                if (curTitle && curTitle !== lastTitle) {
                                    lastTitle = curTitle;
                                    var titleEls = ['miniTitle', 'fpTitle'];
                                    var artistEls = ['miniArtist', 'fpArtist'];
                                    titleEls.forEach(function(id) {
                                        var el = document.getElementById(id);
                                        if (el) el.innerText = track.title;
                                    });
                                    artistEls.forEach(function(id) {
                                        var el = document.getElementById(id);
                                        if (el) el.innerText = track.artist || '';
                                    });
                                    // Update artwork if we have a real URL
                                    if (track.art && track.art.indexOf('data:') !== 0) {
                                        ['miniThumb', 'fpArtwork'].forEach(function(id) {
                                            var el = document.getElementById(id);
                                            if (el) el.src = track.art;
                                        });
                                    }
                                } else if (!curTitle && lastTitle !== NOTHING_PLAYING) {
                                    // Nothing playing - show placeholder instead of stale mock
                                    lastTitle = NOTHING_PLAYING;
                                    ['miniTitle', 'fpTitle'].forEach(function(id) {
                                        var el = document.getElementById(id);
                                        if (el) el.innerText = NOTHING_PLAYING;
                                    });
                                    ['miniArtist', 'fpArtist'].forEach(function(id) {
                                        var el = document.getElementById(id);
                                        if (el) el.innerText = 'Pick something to listen to';
                                    });
                                }

                                // Update play/pause icons if changed
                                if (playing !== lastPlaying) {
                                    lastPlaying = playing;
                                    var pauseSvg = '<path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>';
                                    var playSvg = '<path d="M8 5v14l11-7z"/>';
                                    var icon = playing ? pauseSvg : playSvg;
                                    ['miniPlayIcon', 'fpPlayIcon', 'headerPlayIcon'].forEach(function(id) {
                                        var el = document.getElementById(id);
                                        if (el) el.innerHTML = icon;
                                    });
                                }
                            } catch (e) {}
                        }, 1000);

                        console.log('[CustomUI] Injected successfully');
                    } catch (e) {
                        console.error('[CustomUI] Injection failed:', e);
                    }
                }

                var tries = 0;
                var iv = setInterval(function() {
                    tries++;
                    if (typeof AndBridge !== 'undefined' && AndBridge.loadCustomUiAsset) {
                        clearInterval(iv);
                        loadAndInject();
                    } else if (tries > 50) {
                        clearInterval(iv);
                        console.error('[CustomUI] AndBridge not available');
                    }
                }, 200);
            })();
    """
}
