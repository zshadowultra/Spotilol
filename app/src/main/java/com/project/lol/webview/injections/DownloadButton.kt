package com.project.lol.webview.injections

object DownloadButton {
    const val CONTENT = """
            window.splDoDownload = function(){
                var id = window.splTrackId || window.__curTrackId;
                if(!id){
                    AndBridge.deferMessage('Track not ready');
                    return;
                }
                var m = (window.__splTrackMeta && window.__splTrackMeta[id]) || {};
                var payload = JSON.stringify({
                    trackId: id,
                    title: m.name || window.track || window.__curTrackName || '',
                    artist: m.artist || window.artist || window.__curTrackArtist || '',
                    album: m.album || window.__curTrackAlbum || '',
                    cover: m.cover || window.__curTrackCover || window.cover || ''
                });
                AndBridge.downloadTrack(payload);
            };
            window.splAddDownloadBtn = function(){
                if(typeof window.dlBtn !== 'undefined') return;
                var lyBtn = document.querySelector('button[data-testid=lyrics-button]:not(.splf)');
                var queueBtn = document.querySelector('button[data-testid=control-button-queue]:not(.splf)');
                var anchorBtn = lyBtn || queueBtn;
                if(!anchorBtn) return;
                if(anchorBtn === lyBtn) lyBtn.classList.add('splf');
                var btn = document.createElement('button');
                btn.className = 'npbtn';
                btn.title = 'Download track';
                btn.innerHTML = '<svg viewBox="0 0 16 16" width="16" height="16"><path fill="currentColor" d="M8 1a1 1 0 0 1 1 1v6.586l2.293-2.293a1 1 0 1 1 1.414 1.414l-4 4a1 1 0 0 1-1.414 0l-4-4a1 1 0 1 1 1.414-1.414L7 8.586V2a1 1 0 0 1 1-1zM2 13a1 1 0 0 1 1-1h10a1 1 0 1 1 0 2H3a1 1 0 0 1-1-1z"/></svg>';
                btn.onclick = function(){ splDoDownload(); };
                anchorBtn.before(btn);
                window.dlBtn = btn;
            };
            // C1: the 5s scan moves into window.__splWarden (PlayerCore.kt) as a named
            // check; the warden skips when window.__splBg is true (the old interval
            // had no bg guard). splAddDownloadBtn() is idempotent (no-ops once
            // window.dlBtn is set), so warden re-runs are free.
            if(window.__splWardenAdd) window.__splWardenAdd('splDlBtn', window.splAddDownloadBtn);
            else setInterval(window.splAddDownloadBtn, 5000);
            // C5: event-driven — the anchor buttons (lyrics/queue) live in the
            // now-playing bar (not div[data-testid="action-bar-row"], which is the
            // playlist-header bar). Re-run the idempotent scan when such buttons
            // are added, instead of waiting for the next poll. The warden check
            // above remains as fallback for anything the observer misses.
            if(!window.__splDlBtnObs && window.MutationObserver){
                window.__splDlBtnObs = new MutationObserver(function(muts){
                    if(window.__splBg) return;
                    if(typeof window.dlBtn !== 'undefined') return;
                    for(var i=0;i<muts.length;i++){
                        var nodes=muts[i].addedNodes;
                        for(var j=0;j<nodes.length;j++){
                            var n=nodes[j];
                            if(!n||n.nodeType!==1) continue;
                            if((n.matches && n.matches('button[data-testid=lyrics-button],button[data-testid=control-button-queue]')) ||
                               (n.querySelector && n.querySelector('button[data-testid=lyrics-button],button[data-testid=control-button-queue]'))){
                                try{ window.splAddDownloadBtn(); }catch(e){}
                                return;
                            }
                        }
                    }
                });
                try{
                    var dlTarget = document.querySelector('aside[data-testid="now-playing-bar"]') || document.body || document.documentElement;
                    window.__splDlBtnObs.observe(dlTarget, { childList: true, subtree: true });
                }catch(e){}
            }
        
    """
}