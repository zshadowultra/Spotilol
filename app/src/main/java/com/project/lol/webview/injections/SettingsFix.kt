package com.project.lol.webview.injections

/*
 * CREDIT: Spotilol - Settings Page Fix
 */

object SettingsFix {
    const val CONTENT = """
        (function(){
            if(window.__splSettingsFix) return;
            window.__splSettingsFix = true;

            try {
                var origDefine = customElements.define.bind(customElements);
                customElements.define = function(name, constructor, options) {
                    if (name === 'ms-store-badge') {
                        try { AndBridge.dbg('s', 'Blocked ms-store-badge CE'); } catch(e) {}
                        return;
                    }
                    return origDefine(name, constructor, options);
                };
            } catch(e) {}

            function isSpotifyUrl(url) {
                return url.indexOf('https://open.spotify.com/') === 0 ||
                       url.indexOf('https://accounts.spotify.com/') === 0;
            }

            function isOAuthUrl(url) {
                var host = '';
                try {
                    host = new URL(url).hostname.toLowerCase();
                } catch(e) { return false; }
                return host === 'google.com' ||
                       host.indexOf('.google.com') !== -1 ||
                       host.indexOf('.google.') !== -1 ||
                       host === 'facebook.com' ||
                       host.indexOf('.facebook.com') !== -1 ||
                       host === 'appleid.apple.com' ||
                       host.indexOf('.apple.com') !== -1;
            }

            function isAllowed(url) {
                return isSpotifyUrl(url) || isOAuthUrl(url);
            }

            function hardBlock(el) {
                if (el.__splHardBlocked) return;
                el.__splHardBlocked = true;
                el.addEventListener('click', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    return false;
                }, true);
                el.addEventListener('auxclick', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    return false;
                }, true);
                el.addEventListener('pointerdown', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    return false;
                }, true);
            }

            function interceptLink(a) {
                if (a.__splIntercepted) return;
                a.__splIntercepted = true;

                a.addEventListener('click', function(e) {
                    var href = this.href || '';
                    if (!href || href.charAt(0) === '#') return;
                    if (isAllowed(href)) return;

                    e.preventDefault();
                    e.stopPropagation();
                    try { AndBridge.dbg('s', 'Blocked external nav: ' + href); } catch(err) {}
                    return false;
                }, true);

                a.addEventListener('auxclick', function(e) {
                    var href = this.href || '';
                    if (!href || isAllowed(href)) return;
                    e.preventDefault();
                    e.stopPropagation();
                    return false;
                }, true);
            }

            function interceptSubtree(root) {
                if (!root || root.nodeType !== 1) return;
                if (root.matches) {
                    if (root.matches('a[target="_blank"]')) interceptLink(root);
                    if (root.matches('ms-store-badge')) { hardBlock(root); root.style.pointerEvents = 'none'; }
                    if (root.matches('img[src*="get.microsoft.com"]')) { hardBlock(root); root.style.pointerEvents = 'none'; root.style.display = 'none'; }
                }
                var links = root.querySelectorAll('a[target="_blank"]');
                for (var i = 0; i < links.length; i++) interceptLink(links[i]);

                var badges = root.querySelectorAll('ms-store-badge');
                for (var j = 0; j < badges.length; j++) {
                    hardBlock(badges[j]);
                    badges[j].style.pointerEvents = 'none';
                }

                var imgs = root.querySelectorAll('img[src*="get.microsoft.com"]');
                for (var k = 0; k < imgs.length; k++) {
                    hardBlock(imgs[k]);
                    imgs[k].style.pointerEvents = 'none';
                    imgs[k].style.display = 'none';
                }
            }

            interceptSubtree(document.documentElement);

            var obs = new MutationObserver(function(muts){
                for (var i = 0; i < muts.length; i++) {
                    var an = muts[i].addedNodes;
                    for (var j = 0; j < an.length; j++) interceptSubtree(an[j]);
                }
            });
            obs.observe(document.documentElement, {
                childList: true, subtree: true
            });
        })();
    """
}