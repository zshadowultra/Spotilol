package com.project.lol.webview.injections

// Icons: Solar icon set by 480 Design, CC BY 4.0: https://github.com/480-Design/Solar-Icon-Set
object DownloadButton {
    const val CONTENT = """
            /* DownloadButton icons: Solar set by 480 Design, CC BY 4.0 - https://github.com/480-Design/Solar-Icon-Set */
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
                btn.innerHTML = '<svg width=\"16\" height=\"16\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M17 9.00195C19.175 9.01406 20.3529 9.11051 21.1213 9.8789C22 10.7576 22 12.1718 22 15.0002V16.0002C22 18.8286 22 20.2429 21.1213 21.1215C20.2426 22.0002 18.8284 22.0002 16 22.0002H8C5.17157 22.0002 3.75736 22.0002 2.87868 21.1215C2 20.2429 2 18.8286 2 16.0002L2 15.0002C2 12.1718 2 10.7576 2.87868 9.87889C3.64706 9.11051 4.82497 9.01406 7 9.00195\"/><path stroke-linejoin=\"round\" d=\"M12 2L12 15M15 11.5L12 15L9 11.5\"/></g></svg>';
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