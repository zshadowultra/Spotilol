package com.project.lol.webview.injections

object AutoFeatures {
    const val CONTENT = """
            window.addAutoFeatures = function(){
                if('pBtn' in window && firstPlay && window.autoPlayMode!=='disabled' && window.splIsPlaying()===false) {
                    pBtn.click();
                    firstPlay=false;
                }
                // C1: afint consolidated into window.__splWarden (PlayerCore.kt).
                // The warden ticks every 5s and skips when window.__splBg is true,
                // so this check no longer needs its own interval (and gains the bg
                // guard afint never had). Registration overwrites by name, so
                // re-running addAutoFeatures() is idempotent — no interval leak.
                function splAfCheck(){
                    if(window.closeNpPref) closeNowPlay();
                    var ft = document.querySelector('aside div.encore-bright-accent-set button');
                    if(ft && window.__splTakeControl) {
                        ft.click();
                        setTimeout(function(){
                            var cb = document.querySelector('aside ul[role=list] li[role=listitem] div[role=button]');
                            if(cb) cb.click();
                        },500);
                    }
                    if(window.autoPlayMode==='permanent' && 'pBtn' in window && !reqPause && !ulFlag && window.splIsPlaying()===false) {
                        pBtn.click();
                    }
                    if(window.autoPlayMode==='onetime' && !window.__splApDone && !window.__splApActive && 'pBtn' in window && !reqPause && window.splIsPlaying()===false) {
                        if(typeof splAutoPlay === 'function') splAutoPlay();
                    }
                }
                if(window.__splWardenAdd) window.__splWardenAdd('splAf', splAfCheck);
                else { if(afint) clearInterval(afint); afint = setInterval(splAfCheck, 5000); }
            };
        
    """
}
