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
                            try {
                                var av = document.querySelector('audio, video');
                                if (av && av.duration && isFinite(av.duration)) durationMs = Math.floor(av.duration * 1000);
                            } catch (e0) {}
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
                            try { if (typeof window.actSeek === 'function') window.actSeek(Math.round(ms)); } catch (e) {}
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
                        shuffle: function(want) {
                            try {
                                if (typeof window.actToggleShuffle !== 'function' || typeof window.splShuffleState !== 'function') return;
                                var cur = window.splShuffleState();
                                var isOn = cur === 'shuffle' || cur === 'smart';
                                if (!!want !== isOn) window.actToggleShuffle();
                            } catch (e) {}
                        },
                        repeat: function() {
                            try { if (typeof window.actRepeat === 'function') window.actRepeat(); } catch (e) {}
                        },
                        on: function() { return function() {}; }
                    };
                }

                function extractBody(html) {
                    var m = html.match(/<body[^>]*>([\s\S]*)<\/body>/i);
                    return m ? m[1] : html;
                }

                // DEBUG: visible error banner
                window.__splErrors = [];
                window.addEventListener('error', function(e) {
                    window.__splErrors.push(e.message);
                    var b = document.getElementById('spl-err-banner');
                    if (!b) {
                        b = document.createElement('div');
                        b.id = 'spl-err-banner';
                        b.style.cssText = 'position:fixed;top:0;left:0;right:0;background:#c00;color:#fff;font-size:11px;padding:8px;z-index:999999999;white-space:pre-wrap;max-height:40%;overflow:auto;';
                        document.body.appendChild(b);
                    }
                    b.textContent = 'ERR: ' + window.__splErrors.join('\n');
                });
                function loadAndInject() {
                    try {
                        // Idempotent: onPageFinished can fire multiple times (SPA
                        // navigations, reloads) and each injection would append
                        // another #spl-custom-ui-root. The visible copy would be
                        // the latest one, but $("#nav") binds to the first —
                        // leaving the user tapping a dead UI.
                        if (document.getElementById('spl-custom-ui-root')) return;
                        var html = AndBridge.loadCustomUiAsset('index.html');
                        var css = AndBridge.loadCustomUiAsset('styles.css');
                        var tokens = AndBridge.loadCustomUiAsset('tokens.css');
                        var js = AndBridge.loadCustomUiAsset('app.js');

                        if (!html) {
                            console.error('[CustomUI] Failed to load index.html');
                            return;
                        }

                        hideSpotifyUi();
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
                                                var card = document.createElement('div');
                                                card.className = 'recent-card';
                                                var uri = pid;
                                                var name = pname || 'Playlist';
                                                var img = pl.image || '';
                                                var __bind = (typeof window.bindTap === 'function') ? window.bindTap : function(el, fn) { el.addEventListener('click', fn); };
                                                __bind(card, (function(u, n, im) {
                                                    return function() {
                                                        try {
                                                            if (typeof window.__splLogTap === 'function') {
                                                                window.__splLogTap('playlist-card:' + n);
                                                            }
                                                        } catch (x) {}
                                                        if (typeof window.openPlaylistDetail === 'function') {
                                                            window.openPlaylistDetail(u, n, im);
                                                        } else if (typeof window.playPlaylist === 'function') {
                                                            // Fallback for older injected JS
                                                            window.playPlaylist(u, n, im);
                                                        }
                                                    };
                                                })(uri, name, img));
                                                if (img) {
                                                    var thumb = document.createElement('img');
                                                    thumb.className = 'recent-thumb';
                                                    thumb.src = img;
                                                    thumb.alt = name;
                                                    card.appendChild(thumb);
                                                } else {
                                                    var ph = document.createElement('div');
                                                    ph.className = 'recent-thumb';
                                                    ph.style.background = 'var(--sp-accent-congaso)';
                                                    card.appendChild(ph);
                                                }
                                                var span = document.createElement('span');
                                                span.className = 'recent-title';
                                                span.innerText = name;
                                                card.appendChild(span);
                                                grid.appendChild(card);
                                            }
                                            console.log('[CustomUI] home grid populated with ' + seen + ' real playlists');
                                        }

                                        // ---- Real sections (replaces fakes) ----
                                        replaceFakeSections(lib);
                                    } catch (e) { console.error('[CustomUI] home populate failed', e); }
                                }).catch(function(e) {});
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
                                        var card = document.createElement('div');
                                        card.className = 'release-card';
                                        var __bind2 = (typeof window.bindTap === 'function') ? window.bindTap : function(el, fn) { el.addEventListener('click', fn); };
                                        __bind2(card, (function(u, n, im) {
                                            return function() {
                                                try {
                                                    if (typeof window.__splLogTap === 'function') {
                                                        window.__splLogTap('release-card:' + n);
                                                    }
                                                } catch (x) {}
                                                if (typeof window.playPlaylist === 'function') {
                                                    window.playPlaylist(u, n, im);
                                                }
                                                try {
                                                    if (typeof window.showToast === 'function') {
                                                        window.showToast(n);
                                                    }
                                                } catch (y) {}
                                            };
                                        })(uri, name, img));
                                        if (img) {
                                            var cover = document.createElement('img');
                                            cover.className = 'release-cover';
                                            cover.src = img;
                                            cover.alt = name;
                                            card.appendChild(cover);
                                        } else {
                                            var phc = document.createElement('div');
                                            phc.className = 'release-cover';
                                            phc.style.background = 'var(--sp-accent-congaso)';
                                            card.appendChild(phc);
                                        }
                                        var nm = document.createElement('div');
                                        nm.className = 'release-name';
                                        nm.innerText = name;
                                        card.appendChild(nm);
                                        var ar = document.createElement('div');
                                        ar.className = 'release-artist';
                                        ar.innerText = sub;
                                        card.appendChild(ar);
                                        scroll.appendChild(card);
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
                        setTimeout(loadRealHomeContent, 3000);
                        setTimeout(loadRealHomeContent, 8000);

                        // Live sync is handled by the Cadence app.js payload (see custom-ui/app.js).

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
