package com.project.lol.webview.injections

object CookieBypass {
    const val CONTENT = """
            (function(){
                function dismissBanner(){
                    var banner = document.querySelector('#onetrust-banner-sdk');
                    if(!banner) return false;
                    // Prefer reject-all, fall back to accept
                    var btn = document.querySelector('#onetrust-reject-all-handler')
                        || document.querySelector('#onetrust-accept-btn-handler');
                    if(btn){ btn.click(); return true; }
                    // Cross-origin iframe fallback: hide it
                    banner.style.display = 'none';
                    return true;
                }
                if(dismissBanner()) return;
                var obs = new MutationObserver(function(){
                    if(dismissBanner()) obs.disconnect();
                });
                obs.observe(document.documentElement, {childList: true, subtree: true});
                // Safety: stop observing after 15s
                setTimeout(function(){ obs.disconnect(); }, 15000);
            })();
        
    """
}
