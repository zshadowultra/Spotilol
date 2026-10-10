package com.project.lol.webview.injections

/*
 * CREDIT: Spotilol - Error Dialog Restyle
 */

// Icons: Solar icon set by 480 Design, CC BY 4.0: https://github.com/480-Design/Solar-Icon-Set
object ErrorDialogRestyle {
    const val CONTENT = """
            /* ErrorDialogRestyle icons: Solar set by 480 Design, CC BY 4.0 - https://github.com/480-Design/Solar-Icon-Set */
        (function(){
            if (window.__splErrDlgInit) return;
            window.__splErrDlgInit = true;

            var KNOWN_TITLES = [
                'something went wrong',
                'unexpected error',
                'oops, something went wrong',
                'ein fehler ist aufgetreten',
                'se ha producido un error',
                'une erreur est survenue',
                'si è verificato un errore',
                'ocorreu um erro',
                'произошла ошибка',
                '問題が発生しました'
            ];

            var st = document.createElement('style');
            st.id = 'spl-errdlg-style';
            st.textContent = [
                '.spl-errdlg{position:fixed!important;inset:0!important;width:auto!important;max-width:none!important;margin:0!important;padding:24px!important;box-sizing:border-box!important;display:flex!important;align-items:center!important;justify-content:center!important;background:rgba(0,0,0,.62)!important;-webkit-backdrop-filter:blur(6px);backdrop-filter:blur(6px);z-index:2147483647!important;overflow:auto!important}',
                '.spl-errdlg-card{width:min(400px,100%)!important;position:relative;background:rgba(18,18,18,.96)!important;-webkit-backdrop-filter:blur(24px);backdrop-filter:blur(24px);border:1px solid rgba(255,255,255,.08)!important;border-radius:24px!important;padding:36px 26px 26px!important;display:flex!important;flex-direction:column!important;align-items:center!important;text-align:center!important;box-shadow:0 24px 64px rgba(0,0,0,.65)!important;animation:splErrIn .45s cubic-bezier(.2,.8,.2,1) both;overflow:hidden;margin:0!important}',
                '.spl-errdlg-card::before{content:"";position:absolute;top:0;left:0;right:0;height:3px;background:linear-gradient(90deg,transparent,#1DB954,transparent);opacity:.7}',
                '.spl-errdlg-icon{width:84px;height:84px;border-radius:50%;background:rgba(29,185,84,.10);display:flex;align-items:center;justify-content:center;margin-bottom:20px;animation:splErrPulse 1.8s ease-in-out infinite;flex-shrink:0}',
                '.spl-errdlg-icon svg{width:40px;height:40px;color:#1DB954}',
                '.spl-errdlg h1{font-size:22px!important;font-weight:700!important;color:#fff!important;letter-spacing:-.3px;margin:0 0 8px!important;padding:0!important;text-transform:none!important;max-width:100%!important}',
                '.spl-errdlg p{font-size:14px!important;color:rgba(255,255,255,.55)!important;margin:0 0 26px!important;line-height:1.5!important;padding:0!important;max-width:100%!important}',
                '.spl-errdlg-card > div:last-child{width:100%!important;display:flex!important;justify-content:center!important;margin:0!important;padding:0!important}',
                '.spl-errdlg button{-webkit-tap-highlight-color:transparent;width:100%!important;min-height:50px!important;background:#1DB954!important;color:#fff!important;border:none!important;border-radius:25px!important;font-size:15px!important;font-weight:700!important;letter-spacing:.3px!important;cursor:pointer;padding:0 24px!important;font-family:inherit!important;transition:transform .15s,background .2s,box-shadow .2s;box-shadow:0 8px 24px rgba(29,185,84,.35)}',
                '.spl-errdlg button:hover{background:#1ed760!important;transform:translateY(-1px);box-shadow:0 10px 28px rgba(29,185,84,.45)}',
                '.spl-errdlg button:active{transform:scale(.97)!important}',
                '@keyframes splErrIn{0%{opacity:0;transform:translateY(16px) scale(.94)}100%{opacity:1;transform:translateY(0) scale(1)}}',
                '@keyframes splErrPulse{0%,100%{box-shadow:0 0 0 0 rgba(29,185,84,.28)}50%{box-shadow:0 0 0 16px rgba(29,185,84,0)}}',
                '@media(max-width:420px){.spl-errdlg{padding:16px!important}.spl-errdlg-card{padding:30px 22px 22px!important;border-radius:20px!important}.spl-errdlg-icon{width:72px;height:72px;margin-bottom:16px}.spl-errdlg-icon svg{width:34px;height:34px}.spl-errdlg h1{font-size:19px!important}.spl-errdlg p{font-size:13px!important;margin-bottom:22px!important}}'
            ].join('\n');

            function appendStyle(){
                var t = document.head || document.documentElement;
                if (t && !document.getElementById('spl-errdlg-style')) t.appendChild(st);
            }
            appendStyle();

            var ICON = '<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M8.66667 11.2426C8.20528 10.9374 7.68059 10.7184 7.11616 10.6089C6.8475 10.5567 6.56983 10.5294 6.28571 10.5294C3.91878 10.5294 2 12.4256 2 14.7647C2 17.1038 3.91878 19 6.28571 19M14.381 8.02721C14.9767 7.81911 15.6178 7.70588 16.2857 7.70588C16.9404 7.70588 17.5693 7.81468 18.1551 8.01498M7.11616 10.6089C6.88706 9.9978 6.7619 9.33687 6.7619 8.64706C6.7619 5.52827 9.32028 3 12.4762 3C15.4159 3 17.8371 5.19371 18.1551 8.01498M18.1551 8.01498C20.393 8.78024 22 10.8811 22 13.3529C22 16.0599 20.0726 18.3221 17.5 18.8722\"/><path d=\"M13.5 17.5L12 19M12 19L10.5 20.5M12 19L10.5 17.5M12 19L13.5 20.5\"/></g></svg>';

            function looksLikeErrorDialog(dlg){
                var h = dlg.querySelector('h1');
                if (!h) return false;
                var t = (h.textContent || '').trim().toLowerCase();
                for (var i = 0; i < KNOWN_TITLES.length; i++){
                    if (t === KNOWN_TITLES[i]) return true;
                }
                var btn = dlg.querySelector('button');
                if (btn && /reload|refresh|recarrega|ricarica|neuladen/i.test(btn.textContent || '')) return true;
                return false;
            }

            function dress(dlg){
                if (dlg.__splErrDressed) return;
                dlg.__splErrDressed = true;
                dlg.classList.add('spl-errdlg');
                var inner = dlg.firstElementChild;
                if (inner) inner.classList.add('spl-errdlg-card');
                var h1 = dlg.querySelector('h1');
                if (h1 && !dlg.querySelector('.spl-errdlg-icon')){
                    var ic = document.createElement('div');
                    ic.className = 'spl-errdlg-icon';
                    ic.innerHTML = ICON;
                    h1.parentNode.insertBefore(ic, h1);
                }
            }

            function scan(node){
                if (!node || node.nodeType !== 1) return;
                var hits = [];
                if (node.matches && node.matches('div[role=dialog][aria-modal=true]')) hits.push(node);
                if (node.querySelectorAll){
                    var q = node.querySelectorAll('div[role=dialog][aria-modal=true]');
                    for (var i = 0; i < q.length; i++) hits.push(q[i]);
                }
               for (var j = 0; j < hits.length; j++){
                    var hit = hits[j];
                    if (hit.__splErrDressed) continue;
                    if (looksLikeErrorDialog(hit)) dress(hit);
                }
            }

            var obs = new MutationObserver(function(muts){
                for (var i = 0; i < muts.length; i++){
                    var a = muts[i].addedNodes;
                    for (var j = 0; j < a.length; j++) {
                        if (a[j].nodeType === 1) scan(a[j]);
                    }
                }
            });
            obs.observe(document.body, { childList: true, subtree: true });
            scan(document.body);
        })();
    """
}