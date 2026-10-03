package com.project.lol.webview.injections

/*
 * CustomUI - Full-takeover custom Spotify UI.
 *
 * Loads the judge-loop-approved UI (index.html, styles.css, tokens.css, app.js)
 * from assets/custom-ui/ and injects it into the WebView, hiding Spotify's
 * React SPA. Only the audio element is kept alive.
 *
 * The UI talks to window.__bridge which is wired to the real player seams:
 *   __bridge.toggle()    -> actPlayPause()
 *   __bridge.next()      -> actSkipForward()
 *   __bridge.prev()      -> actSkipBack()
 *   __bridge.seek(ms)    -> actSeek()
 *   __bridge.track       -> { title, artist, art, durationMs } (live from TrackObserver)
 *   __bridge.playing     -> boolean (live from splIsPlaying)
 *   __bridge.on(event, cb) -> subscribe to trackchange/playstate
 */

object CustomUI {
    const val CONTENT = """
            (function(){
                if (window.__splCustomUiLoaded) return;
                window.__splCustomUiLoaded = true;

                // Hide Spotify's SPA, keep only media elements alive
                function hideSpotifyUi() {
                    var st = document.createElement('style');
                    st.id = 'spl-custom-ui-hide';
                    st.textContent = [
                        '#main, .Root, [data-testid="main"] { display: none !important; }',
                        'aside[data-testid="now-playing-bar"] { display: none !important; }',
                        '#spotilolPlayerControls { display: none !important; }',
                        'audio, video { position: fixed !important; width: 1px !important; height: 1px !important;',
                        '  opacity: 0 !important; pointer-events: none !important; }',
                        '#spl-custom-ui-root { position: fixed !important; inset: 0 !important;',
                        '  z-index: 2147483647 !important; background: #000; overflow: hidden; }',
                        '#spl-custom-ui-root iframe { width: 100% !important; height: 100% !important; border: 0; }'
                    ].join('\n');
                    (document.head || document.documentElement).appendChild(st);
                }

                // Build the real bridge backed by player seams
                function buildBridge() {
                    var listeners = { trackchange: [], playstate: [] };

                    function getTrack() {
                        try {
                            var title = '', artist = '', art = '', durationMs = 0;
                            var widget = document.querySelector('[data-testid="now-playing-widget"]');
                            if (widget) {
                                var titleEl = widget.querySelector('a[href*="/track/"]');
                                if (titleEl) title = (titleEl.textContent || '').trim();
                                var artistEl = widget.querySelector('[data-testid="now-playing-widget"] a[href*="/artist/"]');
                                if (artistEl) artist = (artistEl.textContent || '').trim();
                                var img = widget.querySelector('img');
                                if (img) art = img.src || '';
                            }
                            var durEl = document.querySelector('div[data-testid="playback-duration"]');
                            if (durEl) {
                                var parts = (durEl.textContent || '0:00').split(':');
                                durationMs = (parseInt(parts[0]) * 60 + parseInt(parts[1] || '0')) * 1000;
                            }
                            return { title: title, artist: artist, art: art, durationMs: durationMs };
                        } catch (e) { return { title: '', artist: '', art: '', durationMs: 0 }; }
                    }

                    function isPlaying() {
                        try {
                            if (typeof window.splIsPlaying === 'function') {
                                var s = window.splIsPlaying();
                                if (s !== null) return s;
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
                            if (typeof window.actPlayPause === 'function') {
                                var s = isPlaying();
                                window.actPlayPause(s ? true : false);
                            }
                        },
                        next: function() {
                            if (typeof window.actSkipForward === 'function') window.actSkipForward();
                        },
                        prev: function() {
                            if (typeof window.actSkipBack === 'function') window.actSkipBack();
                        },
                        seek: function(ms) {
                            if (typeof window.actSeek === 'function') window.actSeek(ms / 1000);
                        },
                        on: function(event, cb) {
                            if (listeners[event]) {
                                listeners[event].push(cb);
                                return function() {
                                    var i = listeners[event].indexOf(cb);
                                    if (i >= 0) listeners[event].splice(i, 1);
                                };
                            }
                            return function() {};
                        },
                        _emit: function(event, data) {
                            (listeners[event] || []).forEach(function(cb) {
                                try { cb(data); } catch (e) {}
                            });
                        }
                    };

                    // Wire track changes to bridge listeners
                    if (typeof window.splOnTrackChange === 'function') {
                        window.splOnTrackChange(function(uri, id) {
                            window.__bridge._emit('trackchange', { uri: uri, id: id, track: getTrack() });
                        });
                    }

                    // Poll play state and emit changes
                    var lastPlaying = null;
                    setInterval(function() {
                        var p = isPlaying();
                        if (p !== lastPlaying) {
                            lastPlaying = p;
                            window.__bridge._emit('playstate', p);
                        }
                    }, 500);
                }

                // Load UI files from assets and inject
                function loadAndInject() {
                    try {
                        var html = AndBridge.loadCustomUiAsset('index.html');
                        var css = AndBridge.loadCustomUiAsset('styles.css');
                        var tokens = AndBridge.loadCustomUiAsset('tokens.css');
                        var js = AndBridge.loadCustomUiAsset('app.js');

                        if (!html) {
                            console.error('[CustomUI] Failed to load index.html from assets');
                            return;
                        }

                        hideSpotifyUi();
                        buildBridge();

                        // Create root container
                        var root = document.createElement('div');
                        root.id = 'spl-custom-ui-root';
                        document.body.appendChild(root);

                        // Inject CSS
                        function injectCss(content, id) {
                            if (!content) return;
                            var st = document.createElement('style');
                            st.id = id;
                            st.textContent = content;
                            document.head.appendChild(st);
                        }
                        injectCss(tokens, 'spl-custom-ui-tokens');
                        injectCss(css, 'spl-custom-ui-styles');

                        // Inject HTML (strip outer html/body tags, keep content)
                        var tmp = document.createElement('div');
                        tmp.innerHTML = html;
                        // Move all child nodes into root
                        while (tmp.firstChild) {
                            root.appendChild(tmp.firstChild);
                        }

                        // Fix relative art/ paths - they need to load from assets
                        // We'll use a custom scheme handler or base64; for now rewrite via bridge
                        // Actually: inject JS last so it can find the DOM
                        if (js) {
                            var script = document.createElement('script');
                            script.textContent = js;
                            document.body.appendChild(script);
                        }

                        // Rewrite art/ image paths to use asset loading
                        root.querySelectorAll('img[src^="art/"]').forEach(function(img) {
                            var path = img.getAttribute('src');
                            // Load as base64 via bridge and set data URL
                            // For now, keep relative - WebView may resolve via file:///android_asset/
                            img.src = 'file:///android_asset/custom-ui/' + path;
                        });

                        console.log('[CustomUI] Injected successfully');
                    } catch (e) {
                        console.error('[CustomUI] Injection failed:', e);
                    }
                }

                // Wait for AndBridge and player seams to be ready
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
