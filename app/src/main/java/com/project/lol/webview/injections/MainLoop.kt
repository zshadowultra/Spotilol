package com.project.lol.webview.injections

// Icons: Solar icon set by 480 Design, CC BY 4.0: https://github.com/480-Design/Solar-Icon-Set
object MainLoop {
    const val CONTENT = """
            /* MainLoop icons: Solar set by 480 Design, CC BY 4.0 - https://github.com/480-Design/Solar-Icon-Set */
            window.firstFuck = function(){
                // C1: pfint consolidated into window.__splWarden (PlayerCore.kt).
                // The warden ticks every 5s and skips when window.__splBg is true,
                // so this check no longer needs its own interval (and gains the bg
                // guard pfint never had). Registration overwrites by name, so
                // re-running firstFuck() is idempotent — no interval leak.
                function splPfCheck(){
                    if(playing && document.visibilityState=='hidden' && !!document.querySelector('.VideoPlayer__container video')) {
                        AndBridge.wakeUp();
                    } else if(!AndBridge.isWoke() && document.visibilityState=='visible' && !document.querySelector('.VideoPlayer__container video')) {
                        AndBridge.wakeOff();
                    }

                    if(typeof npBtn=='undefined') {
                        var lyBtn = document.querySelector('button[data-testid=lyrics-button]:not(.fuckd)');
                        var queueBtn = document.querySelector('button[data-testid=control-button-queue]:not(.fuckd)');
                        var anchorBtn = lyBtn || queueBtn;
                        if(anchorBtn) {
                            if(anchorBtn === lyBtn) lyBtn.classList.add('fuckd');
                            npBtn = document.createElement('button');
                            npBtn.className = 'npbtn';
                            npBtn.onclick = clickNP;
                            npBtn.innerHTML = '<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path stroke-linejoin=\"round\" d=\"M10.5 9L13.5 12L10.5 15\"/><path d=\"M2 12C2 7.28595 2 4.92893 3.46447 3.46447C4.92893 2 7.28595 2 12 2C16.714 2 19.0711 2 20.5355 3.46447C22 4.92893 22 7.28595 22 12C22 16.714 22 19.0711 20.5355 20.5355C19.0711 22 16.714 22 12 22C7.28595 22 4.92893 22 3.46447 20.5355C2 19.0711 2 16.714 2 12Z\"/></g></svg>';
                            window.timerBtn = document.createElement('button');
                            timerBtn.className = 'npbtn';
                            timerBtn.onclick = function(){ AndBridge.openTimerDialog(); };
                            timerBtn.innerHTML = '<svg width=\"16\" height=\"16\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M21 13C21 17.9706 16.9706 22 12 22C7.02944 22 3 17.9706 3 13C3 8.02944 7.02944 4 12 4C16.9706 4 21 8.02944 21 13Z\"/><path stroke-linejoin=\"round\" d=\"M12 13V9\"/><path d=\"M10 2H14\"/></g></svg>';
                            anchorBtn.before(npBtn);
                            npBtn.before(timerBtn);
                            closeNowPlay();
                        }
                    }

                    var pb = document.querySelector('aside button[data-testid=control-button-playpause]:not(.fuckd)');
                    if(pb) wirePlayBtn(pb);
                }
                if(window.__splWardenAdd) window.__splWardenAdd('splPf', splPfCheck);
                else { if(pfint) clearInterval(pfint); pfint = setInterval(splPfCheck, 5000); }

                // B5: bootIv is a one-shot boot loop, not a poller — it wires the
                // play button fast at startup and self-terminates after 100 tries
                // (30s). No DOM/event signal can replace "page just loaded, wait
                // for the player to mount". Frozen by the native onPause()/
                // pauseTimers() path (B1) if the app backgrounds mid-boot.
                var tries = 0;
                var bootIv = setInterval(function(){
                    tries++;
                    var pb = document.querySelector('aside button[data-testid=control-button-playpause]:not(.fuckd)');
                    if(pb){ clearInterval(bootIv); wirePlayBtn(pb); }
                    else if(tries > 100){ clearInterval(bootIv); }
                },300);
            };

            window.wirePlayBtn = function(pb){
                window.pBtn = pb;
                pb.classList.add('fuckd');

                pBtn.addEventListener('click', function(e){
                    if(window.splIsPlaying()!==false) {
                        reqPause=true;
                        ulFlag=false;
                        AndBridge.wakeOff();
                        return;
                    }
                    reqPause=false;
                    if(e.isTrusted && !ulFlag){
                        AndBridge.wakeUp();
                        ulFlag=true;
                        setTimeout(function(){
                            if(ulFlag && window.splIsPlaying()===false) {
                                AndBridge.deferMessage('unlock');
                                actSkipForward();
                                // B5: uIv is a one-shot unlock confirmation poll (<=6 tries,
                                // self-clearing), not a standing interval. No event signal
                                // exists for "unlock round-trip completed"; frozen by the
                                // native onPause()/pauseTimers() path (B1) if backgrounded.
                                var uTries=0;
                                var uIv=setInterval(function(){
                                    uTries++;
                                    if(window.splIsPlaying()!==false){
                                        window.__splUnlocked=true;
                                        ulFlag=false;
                                        clearInterval(uIv);
                                    } else if(uTries>5){
                                        clearInterval(uIv);
                                        ulFlag=false;
                                    }
                                },1000);
                            } else if(ulFlag) {
                                ulFlag=false;
                                window.__splUnlocked=true;
                            }
                        },10000);
                    }
                });

                if(!ffDone){
                    ffDone=true;
                    AndBridge.manageTShut(true);
                    AndBridge.manageTSleep(false);
                    addAndAuto();
                    addAutoFeatures();
                    addCSSJSHack();
                }
                AndBridge.playLoaded();
            };
            firstFuck();
    """
}