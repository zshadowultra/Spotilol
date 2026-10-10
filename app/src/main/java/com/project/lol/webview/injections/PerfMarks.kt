package com.project.lol.webview.injections

/**
 * Timing instrumentation for the user-requested perf audit (round 2).
 *
 * Defines `window.__splPerf` — a tiny, allocation-cheap mark/measure helper
 * with an in-memory audit trail. Measured events are bridged to logcat through
 * `AndBridge.perfMark()` → `Logger.perf()` (logcat tag `spotilol.perf`;
 * capture with `adb logcat -s spotilol.perf:I`).
 *
 * Wired in from several places:
 *  - document-start bootstrap: [PerfMarks.CONTENT] + [DOCSTART_BEGIN] /
 *    [DOCSTART_END] parts (SpotifyWebViewClient.buildEarlyJs)
 *  - post-login core chunk begin/end (injectSplitPayload, same file)
 *  - deferred-chunk eval (buildDeferredSchedulerJs, same file)
 *  - custom-ui app.js: script eval, first paint (rAF), screen opens,
 *    renderPlaylistTracks (assets/custom-ui/app.js)
 *  - CustomUI.kt: home/library content loads
 *  - SpotilolPlayer.kt: tap -> icon-paint delta (splOptPlay)
 *
 * Every use site is guarded (`window.__splPerf&&...` / try/catch), so the
 * feature files also run without the helper (e.g. in the node drivers).
 */
object PerfMarks {
    const val CONTENT = """
            window.__splPerf=window.__splPerf||(function(){
                var marks=[];
                function now(){
                    try{ if(typeof performance!=='undefined'&&performance.now) return performance.now(); }catch(e){}
                    return Date.now();
                }
                function lastT(name){
                    for(var i=marks.length-1;i>=0;i--){ if(marks[i].n===name) return marks[i].t; }
                    return -1;
                }
                function send(line){
                    try{
                        if(window.AndBridge&&typeof window.AndBridge.perfMark==='function')
                            window.AndBridge.perfMark(line);
                    }catch(e){}
                }
                function emit(name,ms){
                    var line='[spl-perf] '+name+(ms===undefined?'':' '+(Math.round(ms*10)/10)+'ms');
                    marks.push({n:'emit:'+name,t:now()});
                    send(line);
                }
                return {
                    marks:marks,
                    now:now,
                    mark:function(name){ var t=now(); marks.push({n:name,t:t}); return t; },
                    since:function(name){ var t0=lastT(name); return t0<0?-1:now()-t0; },
                    emit:emit,
                    emitSince:function(name,label){
                        var t0=lastT(name);
                        if(t0<0) return -1;
                        var d=now()-t0;
                        emit(label||name,d);
                        return d;
                    }
                };
            })();
    """

    /** JS one-liner marking the start of the document-start payload eval. */
    const val DOCSTART_BEGIN = "window.__splPerf&&window.__splPerf.mark('docstart-begin');"

    /** JS one-liner emitting the document-start payload eval duration. */
    const val DOCSTART_END = "window.__splPerf&&window.__splPerf.emitSince('docstart-begin','docstart-eval');"

    /** JS one-liner marking the start of the post-login core chunk eval. */
    const val CORE_BEGIN = "window.__splPerf&&window.__splPerf.mark('core-begin');"

    /** JS one-liner emitting the post-login core chunk eval duration. */
    const val CORE_END = "window.__splPerf&&window.__splPerf.emitSince('core-begin','core-eval');"
}
