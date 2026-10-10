package com.project.lol.webview.injections
/*
 * CREDIT: Spotilol - Custom Search Overlay.
 *
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⣀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣀⡀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣾⠙⠻⢶⣄⡀⠀⠀⠀⢀⣤⠶⠛⠛⡇⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢹⣇⠀⠀⣙⣿⣦⣤⣴⣿⣁⠀⠀⣸⠇⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠙⣡⣾⣿⣿⣿⣿⣿⣿⣿⣷⣌⠋⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣴⣿⣷⣄⡈⢻⣿⡟⢁⣠⣾⣿⣦⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢹⣿⣿⣿⣿⠘⣿⠃⣿⣿⣿⣿⡏⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣀⠀⠈⠛⣰⠿⣆⠛⠁⠀⡀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⣼⣿⣦⠀⠘⠛⠋⠀⣴⣿⠁⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⣀⣤⣶⣾⣿⣿⣿⣿⡇⠀⠀⠀⢸⣿⣏⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⣠⣶⣿⣿⣿⣿⣿⣿⣿⣿⠿⠿⠀⠀⠀⠾⢿⣿⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⣠⣿⣿⣿⣿⣿⣿⡿⠟⠋⣁⣠⣤⣤⡶⠶⠶⣤⣄⠈⠀⠀⠀⠀⠀⠀
⠀⠀⠀⢰⣿⣿⣮⣉⣉⣉⣤⣴⣶⣿⣿⣋⡥⠄⠀⠀⠀⠀⠉⢻⣄⠀⠀⠀⠀⠀
⠀⠀⠀⠸⣿⣿⣿⣿⣿⣿⣿⣿⣿⣟⣋⣁⣤⣀⣀⣤⣤⣤⣤⣄⣿⡄⠀⠀⠀⠀
⠀⠀⠀⠀⠙⠿⣿⣿⣿⣿⣿⣿⣿⡿⠿⠛⠋⠉⠁⠀⠀⠀⠀⠈⠛⠃⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠉⠉⠉⠉⠉⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
 */


// Icons: Solar icon set by 480 Design, CC BY 4.0: https://github.com/480-Design/Solar-Icon-Set
object SearchOverlay {
    const val CONTENT = """
            /* SearchOverlay icons: Solar set by 480 Design, CC BY 4.0 - https://github.com/480-Design/Solar-Icon-Set */
            (function(){
                if(window.splSearchInit) return;
                window.splSearchInit = true;

                var panel = null, pInput = null, debTimer = null, seq = 0;
                var lastQ = '', anchoredBtn = null;
                var showGuardUntil = 0;
                var resizeTimer = null;

                var HASH = '23f33ca50a0f4153dafc5cd1b4d1370db01b72130c2994bd0ffd07d5a7fee8f0';
                var RECENT_HASH = '3ec071f88e403779d4da9bc5744feb9d64cd07d10daf1f966b912baadaa3d598';
                var LIB_CHECK_HASH = '134337999233cc6fdd6b1e6dbf94841409f04a946c5c7b744b09ba0dfe5a85ed';
                var LIB_TOGGLE_HASH = '1ad0d40b3c09660d818b9e770eb1e84745dfbe941df159a64f8772b6fa2bfc3a';

                /* Solar icons (Linear) by 480 Design, CC BY 4.0 */ var ICONS = {
                    search: '<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><circle cx=\"11.5\" cy=\"11.5\" r=\"9.5\"/><path d=\"M18.2173 18.2178L21.9999 22.0004\"/></g></svg>',
                    artist: '<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><circle cx=\"12\" cy=\"6\" r=\"4\"/><path d=\"M20 17.5C20 19.9853 20 22 12 22C4 22 4 19.9853 4 17.5C4 15.0147 7.58172 13 12 13C16.4183 13 20 15.0147 20 17.5Z\"/></g></svg>',
                    track: '<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M13 18V10V2\"/><circle cx=\"9\" cy=\"18\" r=\"4\"/><path d=\"M19 8C15.6863 8 13 5.31371 13 2\"/></g></svg>',
                    album: '<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><circle cx=\"12\" cy=\"12\" r=\"3\"/><path d=\"M4.92893 19.0711C8.83418 22.9763 15.1658 22.9763 19.0711 19.0711C22.9763 15.1658 22.9763 8.83418 19.0711 4.92893C15.1658 1.02369 8.83418 1.02369 4.92893 4.92893C1.02369 8.83418 1.02369 15.1658 4.92893 19.0711Z\"/><path d=\"M7.40381 16.5967C4.8654 14.0583 4.8654 9.94271 7.40381 7.4043M16.5962 7.4043C19.1346 9.94271 19.1346 14.0583 16.5962 16.5967\"/></g></svg>',
                    playlist: '<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M21 6L3 6\"/><path d=\"M21 10L3 10\"/><path d=\"M11 14L3 14\"/><path d=\"M11 18H3\"/><path d=\"M18.875 14.1183C20.5288 15.0732 21.3558 15.5506 21.4772 16.2394C21.5076 16.4118 21.5076 16.5881 21.4772 16.7604C21.3558 17.4492 20.5288 17.9266 18.875 18.8815C17.2212 19.8363 16.3942 20.3137 15.737 20.0745C15.5725 20.0147 15.4199 19.9265 15.2858 19.814C14.75 19.3644 14.75 18.4096 14.75 16.4999C14.75 14.5902 14.75 13.6354 15.2858 13.1858C15.4199 13.0733 15.5725 12.9852 15.737 12.9253C16.3942 12.6861 17.2212 13.1635 18.875 14.1183Z\"/></g></svg>',
                    podcast: '<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M9 10C9 8.34315 10.3431 7 12 7C13.6569 7 15 8.34315 15 10V13C15 14.6569 13.6569 16 12 16C10.3431 16 9 14.6569 9 13V10Z\"/><path d=\"M13 10L15 10\"/><path d=\"M13 13L15 13\"/><path d=\"M9 10L10 10M9 13L10 13\"/><path d=\"M4.15381 16C5.17341 16 5.99996 15.1734 5.99996 14.1538V9.99998C5.99996 6.68628 8.68624 4 11.9999 4C15.3136 4 17.9999 6.68628 17.9999 9.99998V14.1538C17.9999 15.1734 18.8265 16 19.8461 16\"/><path d=\"M2 12C2 10.8954 2.89543 10 4 10C5.10457 10 6 10.8954 6 12V14C6 15.1046 5.10457 16 4 16C2.89543 16 2 15.1046 2 14V12Z\"/><path d=\"M18 12C18 10.8954 18.8954 10 20 10C21.1046 10 22 10.8954 22 12V14C22 15.1046 21.1046 16 20 16C18.8954 16 18 15.1046 18 14V12Z\"/><path d=\"M12 16V19\"/></g></svg>',
                    clear: '<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><circle cx=\"12\" cy=\"12\" r=\"10\"/><path d=\"M14.5 9.50002L9.5 14.5M9.49998 9.5L14.5 14.5\"/></g></svg>',
                    browse: '<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M3.74181 20.5545C3.74181 20.5545 3.74181 20.5545 3.74181 20.5545C4.94143 22 7.17414 22 11.6395 22H12.3607C16.8261 22 19.0589 22 20.2585 20.5545M3.74181 20.5545C2.54219 19.1091 2.95365 16.9146 3.77657 12.5257C4.36179 9.40452 4.65441 7.84393 5.7653 6.92196M20.2585 20.5545C20.2585 20.5545 20.2585 20.5545 20.2585 20.5545C21.4581 19.1091 21.0466 16.9146 20.2237 12.5257C19.6385 9.40452 19.3459 7.84393 18.235 6.92196M18.235 6.92196C18.235 6.92196 18.235 6.92196 18.235 6.92196C17.1241 6 15.5363 6 12.3607 6H11.6395C8.46398 6 6.8762 6 5.7653 6.92196C5.7653 6.92196 5.7653 6.92196 5.7653 6.92196\"/><path d=\"M9 6V5C9 3.34315 10.3431 2 12 2C13.6569 2 15 3.34315 15 5V6\"/></g></svg>',
                    add: '<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><circle cx=\"12\" cy=\"12\" r=\"10\"/><path d=\"M15 12L12 12M12 12L9 12M12 12L12 9M12 12L12 15\"/></g></svg>',
                    added: '<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><circle cx=\"12\" cy=\"12\" r=\"10\"/><path stroke-linejoin=\"round\" d=\"M8.5 12.5L10.5 14.5L15.5 9.5\"/></g></svg>',
                    play: '<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\" d=\"M20.4086 9.35258C22.5305 10.5065 22.5305 13.4935 20.4086 14.6474L7.59662 21.6145C5.53435 22.736 3 21.2763 3 18.9671L3 5.0329C3 2.72368 5.53435 1.26402 7.59661 2.38548L20.4086 9.35258Z\"/></svg>'
                };

                function iconFor(kind){ return ICONS[kind] || ICONS.search; }

                function splArtInto(im, url){
                    if(window.__splUx){ window.__splUx.artCache.loadInto(im, url); }
                    else { im.setAttribute('loading','lazy'); im.setAttribute('decoding','async'); im.src=url; }
                }
                function splDwell(el, uri){
                    if(!el || !uri || !window.__splUx) return;
                    el.addEventListener('mouseenter', function(){ window.__splUx.prefetch.arm(uri); });
                    el.addEventListener('mouseleave', function(){ window.__splUx.prefetch.disarm(uri); });
                    el.addEventListener('focus', function(){ window.__splUx.prefetch.arm(uri); });
                    el.addEventListener('blur', function(){ window.__splUx.prefetch.disarm(uri); });
                }

                function pickImg(sources){
                    if(!sources || !sources.length) return '';
                    var best = null;
                    for(var i=0;i<sources.length;i++){
                        var s = sources[i];
                        if(!s || !s.url) continue;
                        var w = s.width || 0;
                        if(!best) { best = s; continue; }
                        if(w >= 40 && w < (best.width || 9999)) best = s;
                        if(!best.width && w) best = s;
                    }
                    return best ? best.url : '';
                }

                function vw(){
                    try {
                        if(window.visualViewport && window.visualViewport.width) return window.visualViewport.width;
                    } catch(e){}
                    return window.innerWidth || 411;
                }

                function css(){
                    var st = document.createElement('style');
                    st.textContent = [
                        '#global-nav-bar input[data-testid="search-input"]{display:none!important}',
                        '#global-nav-bar form[role="search"]{min-width:48px!important;width:48px!important;max-width:48px!important;height:48px!important;min-height:48px!important;display:flex!important;align-items:center!important}',
                        '#global-nav-bar form[role="search"] [class*="form-input-icon__icon"]{position:static!important;top:auto!important;transform:none!important}',
                        '#global-nav-bar form[role="search"] > :not([class*="form-input-icon__icon--leading"]){display:none!important}',
                        'html.spl-search-active [data-testid="search-dropdown"],html.spl-search-active [data-testid="search-page-searchbar-searchbar-dropdown"]{display:none!important}',
                        '#splSearchPanel{position:fixed;z-index:2147483647!important;background:rgba(24,24,24,.98);border:1px solid rgba(255,255,255,.08);border-radius:14px;box-shadow:0 8px 32px rgba(0,0,0,.6);display:none;flex-direction:column;overflow:hidden;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;color:#fff}',
                        '#splSearchPanel .spl-sph{display:flex;align-items:center;gap:10px;padding:8px 12px;border-bottom:1px solid rgba(255,255,255,.06)}',
                        '#splSearchPanel .spl-spi{flex:1;min-width:0;background:transparent;border:none;outline:none;color:#fff;font-size:14px;height:32px}',
                        '#splSearchPanel .spl-spi::placeholder{color:rgba(255,255,255,.4)}',
                        '#splSearchPanel .spl-spx,#splSearchPanel .spl-sbr{box-sizing:border-box;width:30px;height:30px;flex-shrink:0;background:none;border:none;border-radius:50%;color:rgba(255,255,255,.7);cursor:pointer;display:flex;align-items:center;justify-content:center;padding:0;transition:background .15s}',
                        '#splSearchPanel .spl-spx:hover,#splSearchPanel .spl-sbr:hover{background:rgba(255,255,255,.12);color:#fff}',
                        '#splSearchPanel .spl-spx svg,#splSearchPanel .spl-sbr svg{width:16px;height:16px}',
                        '#splSearchPanel .spl-sfav{box-sizing:border-box;width:28px;height:28px;flex-shrink:0;background:none;border:none;border-radius:50%;color:rgba(255,255,255,.6);cursor:pointer;display:flex;align-items:center;justify-content:center;padding:0;transition:background .15s,color .15s}',
                        '#splSearchPanel .spl-sfav:hover{background:rgba(255,255,255,.12);color:#fff}',
                        '#splSearchPanel .spl-sfav.saved{color:var(--spl-accent,#1DB954)}',
                        '#splSearchPanel .spl-sfav svg{width:16px;height:16px}',
                        '#splSearchPanel .spl-slist{max-height:55vh;overflow-y:auto;padding:6px;overscroll-behavior:contain}',
                        '#splSearchPanel .spl-srow{display:flex;align-items:center;gap:10px;padding:8px;border-radius:8px;cursor:pointer;min-height:44px;box-sizing:border-box}',
                        '#splSearchPanel .spl-srow:hover{background:rgba(255,255,255,.08)}',
                        '#splSearchPanel .spl-sicon{position:relative;width:36px;height:36px;flex-shrink:0;border-radius:6px;overflow:hidden;background:#282828;display:flex;align-items:center;justify-content:center;color:#b3b3b3}',
                        '#splSearchPanel .spl-sicon img{width:100%;height:100%;object-fit:cover}',
                        '#splSearchPanel .spl-sicon svg{width:18px;height:18px}',
                        '#splSearchPanel .spl-splay{position:absolute;inset:0;background:rgba(0,0,0,.5);display:flex;align-items:center;justify-content:center;color:#fff;opacity:0;transition:opacity .15s;cursor:pointer}',
                        '#splSearchPanel .spl-srow:hover .spl-splay{opacity:1}',
                        '#splSearchPanel .spl-splay svg{width:18px;height:18px}',
                        '#splSearchPanel .spl-stx{flex:1;min-width:0;overflow:hidden}',
                        '#splSearchPanel .spl-sname{font-size:13px;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;line-height:1.3}',
                        '#splSearchPanel .spl-sname a,#splSearchPanel .spl-ssub a{color:inherit;text-decoration:none}',
                        '#splSearchPanel .spl-sname a:hover,#splSearchPanel .spl-ssub a:hover{color:var(--spl-accent,#1DB954)}',
                        '#splSearchPanel .spl-sbadge{display:inline-flex;align-items:center;justify-content:center;background:#b3b3b3;color:#121212;font-size:10.5px;font-weight:600;line-height:14px;border-radius:2px;padding:1px 5px;margin-right:5px;vertical-align:middle;font-family:inherit}',
                        '#splSearchPanel .spl-ssub{font-size:11px;color:rgba(255,255,255,.55);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;line-height:1.3;margin-top:2px}',
                        '#splSearchPanel .spl-stitle{font-size:12px;font-weight:700;color:rgba(255,255,255,.85);padding:8px 10px 4px}',
                        '#splSearchPanel .spl-sem{display:flex;align-items:center;justify-content:center;gap:8px;padding:14px;font-size:12px;color:rgba(255,255,255,.5)}',
                        '#splSearchPanel .spl-sem svg{width:16px;height:16px}',
                        '#splSearchPanel .spl-slist::-webkit-scrollbar{width:4px}',
                        '#splSearchPanel .spl-slist::-webkit-scrollbar-thumb{background:rgba(255,255,255,.2);border-radius:2px}'
                    ].join(' ');
                    var t = document.head || document.documentElement;
                    if(t) t.appendChild(st);
                }

                function anchorSearchIcon(){
                    var icon = document.querySelector('#global-nav-bar button[data-testid="search-icon"]');
                    if(icon && icon.isConnected){
                        anchoredBtn = icon;
                        return true;
                    }
                    return false;
                }

                function anchorPanel(){
                    if(!panel || !document.body) return;
                    var body = document.body;
                    var main = document.getElementById('main');
                    if(main && main.parentNode === body){
                        if(panel.previousElementSibling !== main) body.insertBefore(panel, main.nextElementSibling);
                    } else if(body.lastElementChild !== panel){
                        body.appendChild(panel);
                    }
                }

                function showPanel(){
                    if(!panel) return;
                    if(!anchorSearchIcon()) return;
                    anchorPanel();
                    showGuardUntil = Date.now() + 400;
                    var r = anchoredBtn.getBoundingClientRect();
                    var w = vw();
                    var pw = Math.min(360, w - 24);
                    panel.style.width = Math.round(pw) + 'px';
                    var left = Math.max(8, Math.min(Math.round(r.left), Math.round(w - pw - 8)));
                    panel.style.top = Math.round(r.bottom + 8) + 'px';
                    panel.style.left = left + 'px';
                    panel.style.display = 'flex';
                    document.documentElement.classList.add('spl-search-active');
                    if(pInput){
                        pInput.focus();
                        if(!pInput.value.trim()){ renderLoading(); doRecent(); }
                    }
                }

                function onViewportResize(){
                    if(!panel || panel.style.display === 'none') return;
                    if(Date.now() < showGuardUntil) return;
                    if(!anchoredBtn || !anchoredBtn.isConnected) return;
                    clearTimeout(resizeTimer);
                    resizeTimer = setTimeout(function(){
                        if(panel && panel.style.display !== 'none' && anchoredBtn && anchoredBtn.isConnected){
                            var r = anchoredBtn.getBoundingClientRect();
                            if(r.width > 0 && r.height > 0){
                                var w = vw();
                                var pw = Math.min(360, w - 24);
                                panel.style.width = Math.round(pw) + 'px';
                                var left = Math.max(8, Math.min(Math.round(r.left), Math.round(w - pw - 8)));
                                panel.style.top = Math.round(r.bottom + 8) + 'px';
                                panel.style.left = left + 'px';
                            }
                        }
                    }, 120);
                }

                function hidePanel(){
                    if(panel) panel.style.display = 'none';
                    document.documentElement.classList.remove('spl-search-active');
                    lastQ = '';
                }

                function navTo(path){
                    if(!path) return;
                    try {
                        history.pushState({}, '', path);
                        window.dispatchEvent(new PopStateEvent('popstate', { state: null }));
                    } catch(e){
                        window.location.href = path;
                    }
                }

                function linkTo(path, text, extraCls){
                    var a = document.createElement('a');
                    a.href = path || '#';
                    a.textContent = text || '';
                    if(extraCls) a.className = 'spl-slink ' + extraCls;
                    else a.className = 'spl-slink';
                    a.addEventListener('mousedown', function(e){ e.preventDefault(); e.stopPropagation(); });
                    a.addEventListener('click', function(e){
                        e.preventDefault();
                        e.stopPropagation();
                        hidePanel();
                        navTo(path);
                    });
                    return a;
                }

                function artistLinks(artists, sep){
                    var frag = document.createDocumentFragment();
                    var items = artists || [];
                    for(var i=0;i<items.length;i++){
                        var it = items[i];
                        var nm = it && it.profile && it.profile.name;
                        var u = it && it.uri || '';
                        if(!nm) continue;
                        if(i > 0 && sep) frag.appendChild(document.createTextNode(sep));
                        frag.appendChild(linkTo('/artist/' + u.replace('spotify:artist:',''), nm));
                    }
                    return frag;
                }

                function showLinkFor(d, showData){
                    var nm = null, u = null;
                    if(showData && showData.data){
                        nm = showData.data.name;
                        u = showData.data.uri || '';
                    } else if(showData && showData.name){
                        nm = showData.name;
                        u = showData.uri || '';
                    }
                    if(!nm) return null;
                    return linkTo('/show/' + u.replace('spotify:show:',''), nm);
                }

                function favBtnFor(uri){
                    var b = document.createElement('button');
                    b.className = 'spl-sfav';
                    b.setAttribute('aria-label','Save to Your Library');
                    var saved = false;
                    function paint(){
                        b.innerHTML = saved ? iconFor('added') : iconFor('add');
                        b.classList.toggle('saved', saved);
                        b.setAttribute('aria-label', saved ? 'Remove from Your Library' : 'Save to Your Library');
                    }
                    paint();
                    if(uri && window.spotAuthToken){
                        window.__splOwnCall=true;
                        fetch('https://api-partner.spotify.com/pathfinder/v2/query', {
                            method:'POST',
                            headers:{'Authorization': window.spotAuthToken,'Client-Token': window.spotCliToken || '','Content-Type':'application/json'},
                            body: JSON.stringify({
                                variables:{uris:[uri]},
                                operationName:'areEntitiesInLibrary',
                                extensions:{persistedQuery:{version:1,sha256Hash:window.opHash('areEntitiesInLibrary',LIB_CHECK_HASH)}}
                            })
                        }).then(function(r){ return r.json(); }).then(function(d){
                            var l = d && d.data && d.data.lookup;
                            if(l && l[0] && l[0].data) saved = !!l[0].data.saved;
                            paint();
                        }).catch(function(){});
                        window.__splOwnCall=false;
                    }
                    b.addEventListener('mousedown', function(e){ e.preventDefault(); e.stopPropagation(); });
                    b.addEventListener('click', function(e){
                        e.preventDefault();
                        e.stopPropagation();
                        if(!window.spotAuthToken || !uri) return;
                        saved = !saved;
                        paint();
                        var op = saved ? 'addToLibrary' : 'removeFromLibrary';
                        window.__splOwnCall=true;
                        fetch('https://api-partner.spotify.com/pathfinder/v2/query', {
                            method:'POST',
                            headers:{'Authorization': window.spotAuthToken,'Client-Token': window.spotCliToken || '','Content-Type':'application/json'},
                            body: JSON.stringify({
                                variables:{libraryItemUris:[uri]},
                                operationName:op,
                                extensions:{persistedQuery:{version:1,sha256Hash:window.opHash(op,LIB_TOGGLE_HASH)}}
                            })
                        }).catch(function(){});
                        window.__splOwnCall=false;
                    });
                    return b;
                }

                function rowFor(item){
                    var t = item.__typename || '';
                    var d = item.data || {};
                    var el = document.createElement('div');
                    el.className = 'spl-srow';
                    var icon = document.createElement('div');
                    icon.className = 'spl-sicon';
                    var tx = document.createElement('div');
                    tx.className = 'spl-stx';
                    var name = document.createElement('div');
                    name.className = 'spl-sname';
                    var sub = document.createElement('div');
                    sub.className = 'spl-ssub';

                    if(t === 'SearchAutoCompleteEntity'){
                        var text = d.text || '';
                        el.setAttribute('data-kind','search');
                        el.setAttribute('data-text',text);
                        el.setAttribute('data-uri',d.uri || '');
                        icon.innerHTML = iconFor('search');
                        var ac = document.createElement('a');
                        ac.href = '/search/' + encodeURIComponent(text);
                        ac.className = 'spl-slink';
                        ac.textContent = text;
                        ac.addEventListener('mousedown', function(e){ e.preventDefault(); e.stopPropagation(); });
                        ac.addEventListener('click', function(e){
                            e.preventDefault();
                            e.stopPropagation();
                            if(text && pInput){
                                pInput.value = text;
                                doSearch(text);
                                showPanel();
                            }
                        });
                        name.appendChild(ac);
                        sub.textContent = 'Search';
                    } else if(t === 'ArtistResponseWrapper'){
                        el.setAttribute('data-kind','artist');
                        el.setAttribute('data-uri',d.uri || '');
                        var av = pickImg(d.visuals && d.visuals.avatarImage && d.visuals.avatarImage.sources);
                        if(av){ var im=document.createElement('img'); splArtInto(im,av); icon.appendChild(im); } else { icon.innerHTML = iconFor('artist'); }
                        name.appendChild(linkTo('/artist/' + (d.uri||'').replace('spotify:artist:',''), (d.profile && d.profile.name) || ''));
                        sub.textContent = 'Artist';
                    } else if(t === 'TrackResponseWrapper'){
                        el.setAttribute('data-kind','track');
                        el.setAttribute('data-uri',d.uri || '');
                        var cv = pickImg(d.albumOfTrack && d.albumOfTrack.coverArt && d.albumOfTrack.coverArt.sources);
                        if(cv){ var im2=document.createElement('img'); splArtInto(im2,cv); icon.appendChild(im2); } else { icon.innerHTML = iconFor('track'); }
                        name.appendChild(linkTo('/track/' + (d.uri||'').replace('spotify:track:',''), d.name || ''));
                        var isExplicit = !!(d.contentRating && d.contentRating.label === 'EXPLICIT');
                        if(isExplicit){
                            var eb = document.createElement('span');
                            eb.className = 'spl-sbadge';
                            eb.setAttribute('aria-label','Explicit');
                            eb.textContent = 'E';
                            sub.appendChild(eb);
                        }
                        sub.appendChild(document.createTextNode('Song'));
                        var arts = d.artists && d.artists.items || [];
                        if(arts.length){
                            sub.appendChild(document.createTextNode(' • '));
                            sub.appendChild(artistLinks(arts, ', '));
                        }
                    } else if(t === 'AlbumResponseWrapper'){
                        el.setAttribute('data-kind','album');
                        el.setAttribute('data-uri',d.uri || '');
                        var cv2 = pickImg(d.coverArt && d.coverArt.sources);
                        if(cv2){ var im3=document.createElement('img'); splArtInto(im3,cv2); icon.appendChild(im3); } else { icon.innerHTML = iconFor('album'); }
                        name.appendChild(linkTo('/album/' + (d.uri||'').replace('spotify:album:',''), d.name || ''));
                        var arts2 = d.artists && d.artists.items || [];
                        if(arts2.length){ sub.appendChild(artistLinks(arts2, ', ')); sub.appendChild(document.createTextNode(' · Album')); }
                        else { sub.textContent = 'Album'; }
                    } else if(t === 'PlaylistResponseWrapper'){
                        el.setAttribute('data-kind','playlist');
                        el.setAttribute('data-uri',d.uri || '');
                        var cv3 = pickImg(d.images && d.images.items && d.images.items[0] && d.images.items[0].sources) || pickImg(d.visualIdentity && d.visualIdentity.squareCoverImage && d.visualIdentity.squareCoverImage.sources) || pickImg(d.images && d.images.sources) || pickImg(d.visuals && d.visuals.image && d.visuals.image.sources);
                        if(cv3){ var im4=document.createElement('img'); splArtInto(im4,cv3); icon.appendChild(im4); } else { icon.innerHTML = iconFor('playlist'); }
                        name.appendChild(linkTo('/playlist/' + (d.uri||'').replace('spotify:playlist:',''), d.name || ''));
                        var owner = d.ownerV2 && d.ownerV2.data;
                        if(owner && owner.name){
                            sub.appendChild(document.createTextNode('Playlist · '));
                            sub.appendChild(linkTo('/user/' + (owner.username || owner.uri.replace('spotify:user:','')), owner.name));
                        } else {
                            sub.textContent = 'Playlist';
                        }
                    } else if(t === 'PodcastEpisodeResponseWrapper' || t === 'EpisodeResponseWrapper'){
                        el.setAttribute('data-kind','episode');
                        el.setAttribute('data-uri',d.uri || '');
                        var cv4 = pickImg(d.coverArt && d.coverArt.sources) || pickImg(d.images && d.images.sources);
                        if(cv4){ var im5=document.createElement('img'); splArtInto(im5,cv4); icon.appendChild(im5); } else { icon.innerHTML = iconFor('podcast'); }
                        name.appendChild(linkTo('/episode/' + (d.uri||'').replace(/^spotify:episode:/,''), d.name || ''));
                        var showL = showLinkFor(d, d.podcastV2 || d.show);
                        if(showL){ sub.appendChild(document.createTextNode('Episode · ')); sub.appendChild(showL); }
                        else { sub.textContent = 'Episode'; }
                    } else if(t === 'ShowResponseWrapper'){
                        el.setAttribute('data-kind','show');
                        el.setAttribute('data-uri',d.uri || '');
                        var cv5 = pickImg(d.coverArt && d.coverArt.sources) || pickImg(d.images && d.images.sources);
                        if(cv5){ var im6=document.createElement('img'); splArtInto(im6,cv5); icon.appendChild(im6); } else { icon.innerHTML = iconFor('podcast'); }
                        name.appendChild(linkTo('/show/' + (d.uri||'').replace('spotify:show:',''), d.name || ''));
                        sub.textContent = 'Podcast';
                    } else {
                        el.setAttribute('data-kind','search');
                        el.setAttribute('data-text',(d.text||d.name||''));
                        el.setAttribute('data-uri',d.uri || '');
                        icon.innerHTML = iconFor('search');
                        name.textContent = d.text || d.name || '';
                        sub.textContent = '';
                    }

                    tx.appendChild(name);
                    tx.appendChild(sub);
                    el.appendChild(icon);
                    el.appendChild(tx);
                    var k = el.getAttribute('data-kind');
                    var uri = el.getAttribute('data-uri') || '';
                    if(k && k !== 'artist' && k !== 'search' && uri){
                        var ply = document.createElement('div');
                        ply.className = 'spl-splay';
                        ply.innerHTML = iconFor('play');
                        icon.appendChild(ply);
                        icon.addEventListener('click', function(e){
                            e.preventDefault();
                            e.stopPropagation();
                            if(window.playFromUri){
                                window.playFromUri(uri);
                                hidePanel();
                            }
                        });
                        el.appendChild(favBtnFor(uri));
                    }
                    el.addEventListener('mousedown', function(e){ e.preventDefault(); });
                    var dk = el.getAttribute('data-kind');
                    if(dk==='artist'||dk==='album'||dk==='playlist'||dk==='show'){ splDwell(el, el.getAttribute('data-uri')); }
                    return el;
                }

                function renderLoading(){
                    var list = panel ? panel.querySelector('.spl-slist') : null;
                    if(!list) return;
                    list.innerHTML = '';
                    var em = document.createElement('div');
                    em.className = 'spl-sem';
                    em.innerHTML = iconFor('search') + '<span>Searching...</span>';
                    list.appendChild(em);
                }

                function renderResults(items){
                    var list = panel ? panel.querySelector('.spl-slist') : null;
                    if(!list) return;
                    list.innerHTML = '';
                    if(!items || !items.length){
                        var em = document.createElement('div');
                        em.className = 'spl-sem';
                        em.textContent = 'No results';
                        list.appendChild(em);
                        return;
                    }
                    for(var i=0;i<items.length;i++){
                        list.appendChild(rowFor(items[i]));
                    }
                }

                function renderRecent(items){
                    var list = panel ? panel.querySelector('.spl-slist') : null;
                    if(!list) return;
                    list.innerHTML = '';
                    if(!items || !items.length){
                        var em = document.createElement('div');
                        em.className = 'spl-sem';
                        em.textContent = 'No recent searches';
                        list.appendChild(em);
                        return;
                    }
                    var t = document.createElement('div');
                    t.className = 'spl-stitle';
                    t.textContent = 'Recent searches';
                    list.appendChild(t);
                    for(var i=0;i<items.length;i++){
                        list.appendChild(rowFor(items[i]));
                    }
                }

                function doRecent(){
                    var my = ++seq;
                    if(!window.spotAuthToken){ return; }
                    window.__splOwnCall=true;
                    fetch('https://api-partner.spotify.com/pathfinder/v2/query', {
                        method:'POST',
                        headers:{
                            'Authorization': window.spotAuthToken,
                            'Client-Token': window.spotCliToken || '',
                            'Content-Type':'application/json'
                        },
                        body: JSON.stringify({
                            variables:{limit:50,includeAuthors:true,includeEpisodeContentRatingsV2:true},
                            operationName:'recentSearches',
                            extensions:{persistedQuery:{version:1,sha256Hash:window.opHash('recentSearches',RECENT_HASH)}}
                        })
                    }).then(function(r){ return r.json(); }).then(function(data){
                        if(my !== seq) return;
                        var items = data && data.data && data.data.recentSearches && data.data.recentSearches.recentSearchesItems && data.data.recentSearches.recentSearchesItems.items || [];
                        var flat = [];
                        for(var i=0;i<items.length;i++){
                            if(items[i] && items[i].data) flat.push(items[i]);
                        }
                        renderRecent(flat);
                    }).catch(function(){});
                    window.__splOwnCall=false;
                }

                function doSearch(q){
                    var my = ++seq;
                    if(!window.spotAuthToken){ return; }
                    window.__splOwnCall=true;
                    fetch('https://api-partner.spotify.com/pathfinder/v2/query', {
                        method:'POST',
                        headers:{
                            'Authorization': window.spotAuthToken,
                            'Client-Token': window.spotCliToken || '',
                            'Content-Type':'application/json'
                        },
                        body: JSON.stringify({
                            variables:{query:q,limit:16,numberOfTopResults:16,offset:0,includeAuthors:true,includeAlbumPreReleases:true,includeEpisodeContentRatingsV2:true},
                            operationName:'searchSuggestions',
                            extensions:{persistedQuery:{version:1,sha256Hash:window.opHash('searchSuggestions',HASH)}}
                        })
                    }).then(function(r){ return r.json(); }).then(function(data){
                        if(my !== seq) return;
                        var items = data && data.data && data.data.searchV2 && data.data.searchV2.topResultsV2 && data.data.searchV2.topResultsV2.itemsV2 || [];
                        var flat = [];
                        for(var i=0;i<items.length;i++){
                            if(items[i] && items[i].item) flat.push(items[i].item);
                        }
                        renderResults(flat);
                    }).catch(function(){});
                    window.__splOwnCall=false;
                }

                function onInput(){
                    var v = pInput.value.trim();
                    if(!v){
                        lastQ = '';
                        clearTimeout(debTimer);
                        renderLoading();
                        doRecent();
                        return;
                    }
                    renderLoading();
                    if(v === lastQ) return;
                    lastQ = v;
                    clearTimeout(debTimer);
                    // C8: debounce search-as-you-type 250ms after the last keystroke
                    // before hitting the Spotify API (was 220ms).
                    debTimer = setTimeout(function(){ doSearch(v); }, 250);
                }

                function buildPanel(){
                    if(panel) return;
                    panel = document.createElement('div');
                    panel.id = 'splSearchPanel';

                    var head = document.createElement('div');
                    head.className = 'spl-sph';

                    pInput = document.createElement('input');
                    pInput.className = 'spl-spi';
                    pInput.type = 'text';
                    pInput.placeholder = 'What do you want to play?';
                    pInput.setAttribute('spellcheck','false');
                    pInput.autocomplete = 'off';

                    var br = document.createElement('button');
                    br.className = 'spl-sbr';
                    br.setAttribute('aria-label','Browse');
                    br.innerHTML = iconFor('browse');
                    br.addEventListener('mousedown', function(e){ e.preventDefault(); });
                    br.addEventListener('click', function(e){
                        e.preventDefault();
                        e.stopPropagation();
                        var q = (pInput.value||'').trim();
                        hidePanel();
                        if(q) navTo('/search/' + encodeURIComponent(q));
                        else navTo('/search');
                    });

                    var x = document.createElement('button');
                    x.className = 'spl-spx';
                    x.setAttribute('aria-label','Clear search');
                    x.innerHTML = iconFor('clear');
                    x.addEventListener('mousedown', function(e){ e.preventDefault(); });
                    x.addEventListener('click', function(){
                        pInput.value = '';
                        lastQ = '';
                        renderLoading();
                        doRecent();
                    });

                    head.appendChild(pInput);
                    head.appendChild(x);
                    head.appendChild(br);

                    var list = document.createElement('div');
                    list.className = 'spl-slist';

                    panel.appendChild(head);
                    panel.appendChild(list);
                    document.body.appendChild(panel);

                    pInput.addEventListener('input', onInput);
                    pInput.addEventListener('keydown', function(e){
                        if(e.key === 'Escape'){
                            if(pInput.value){ pInput.value=''; lastQ=''; renderLoading(); doRecent(); }
                            else hidePanel();
                        } else if(e.key === 'Enter'){
                            e.preventDefault();
                            var q = (pInput.value||'').trim();
                            if(q){
                                hidePanel();
                                navTo('/search/' + encodeURIComponent(q));
                            }
                        }
                    });

                    document.addEventListener('mousedown', function(e){
                        if(panel.style.display !== 'none' && Date.now() >= showGuardUntil && !panel.contains(e.target) && !(anchoredBtn && anchoredBtn.contains(e.target))){
                            hidePanel();
                        }
                    }, true);
                    document.addEventListener('scroll', function(e){
                        if(panel.style.display !== 'none' && Date.now() >= showGuardUntil){
                            var t = e.target;
                            if(!(t === panel || (panel.contains && panel.contains(t)))) hidePanel();
                        }
                    }, {capture:true, passive:true});
                }

                function bindSearchIcon(){
                    var icon = document.querySelector('#global-nav-bar button[data-testid="search-icon"]');
                    if(!icon || icon._splSearch) return;
                    icon._splSearch = true;
                    anchoredBtn = icon;
                    icon.addEventListener('click', function(e){
                        e.preventDefault();
                        e.stopPropagation();
                        if(panel && panel.style.display !== 'none'){ hidePanel(); }
                        else { buildPanel(); showPanel(); }
                    }, true);
                }

                css();
                if(window.visualViewport){
                    window.visualViewport.addEventListener('resize', onViewportResize);
                }
                window.addEventListener('resize', onViewportResize);
                bindSearchIcon();
                var navBar = document.getElementById('global-nav-bar');
                var mo = null;
                var moTarget = null;
                function startMo(){
                    navBar = document.getElementById('global-nav-bar');
                    if(!navBar) return;
                    if(mo && moTarget === navBar) return;
                    if(mo) mo.disconnect();
                    mo = new MutationObserver(function(){
                        if(window.__splBg) return;
                        var icon = document.querySelector('#global-nav-bar button[data-testid="search-icon"]');
                        if(icon && !icon._splSearch) bindSearchIcon();
                    });
                    mo.observe(navBar, { childList: true, subtree: true });
                    moTarget = navBar;
                }
                startMo();
                var navMo = new MutationObserver(function(){ startMo(); });
                function watchBody(){
                    if(document.body) navMo.observe(document.body, { childList: true, subtree: false });
                }
                watchBody();
                if(!navBar) document.addEventListener('DOMContentLoaded', watchBody, { once: true });
                // C1+C5: the 2s bindSearchIcon poll is replaced by the nav-bar
                // MutationObserver above (event-driven rebind when the nav bar
                // remounts) plus a 5s window.__splWarden fallback for anything the
                // observer misses (e.g. the icon swapped without a childList
                // mutation on navBar, or navBar detached before watchBody ran).
                // The warden skips when window.__splBg is true.
                function splSearchWarden(){
                    if(navBar && !navBar.isConnected) startMo();
                    bindSearchIcon();
                }
                if(window.__splWardenAdd) window.__splWardenAdd('splSearch', splSearchWarden);
                else setInterval(function(){ if(window.__splBg) return; splSearchWarden(); }, 2000);
            })();
        
    """
}
