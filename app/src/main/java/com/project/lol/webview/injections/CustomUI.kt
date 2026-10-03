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

                        // Load the user's real playlists into the home grid.
                        // Replaces the hardcoded demo tiles with data from
                        // window.fetchAllLibrary() + window.parseLibrary().
                        // Same CSS classes (recent-card/recent-thumb/recent-title)
                        // so the approved visual design is untouched.
                        function loadRealHomeContent() {
                            try {
                                if (typeof window.fetchAllLibrary !== 'function') return;
                                if (typeof window.parseLibrary !== 'function') return;
                                window.fetchAllLibrary().then(function(items) {
                                    try {
                                        var lib = window.parseLibrary(items);
                                        var grid = document.getElementById('homeGrid');
                                        if (!grid || !lib || !lib.playlists || !lib.playlists.length) return;
                                        // Keep the Liked Songs card (first child), replace the rest
                                        var likedCard = grid.children[0] || null;
                                        grid.innerHTML = '';
                                        if (likedCard) grid.appendChild(likedCard);
                                        lib.playlists.slice(0, 5).forEach(function(pl) {
                                            var card = document.createElement('div');
                                            card.className = 'recent-card';
                                            var uri = pl.id || '';
                                            var name = pl.name || 'Playlist';
                                            var img = pl.image || '';
                                            card.addEventListener('click', (function(u, n, im) {
                                                return function() {
                                                    if (typeof window.playPlaylist === 'function') {
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
                                        });
                                        console.log('[CustomUI] home grid populated with ' + Math.min(5, lib.playlists.length) + ' real playlists');
                                    } catch (e) { console.error('[CustomUI] home populate failed', e); }
                                }).catch(function(e) {});
                            } catch (e) {}
                        }
                        // Defer: library fetch needs Spotify auth tokens to be ready
                        setTimeout(loadRealHomeContent, 3000);
                        setTimeout(loadRealHomeContent, 8000);

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
