package com.project.lol.webview.injections
/*
 * CREDIT: Spotilol - Custom Player.
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
object SpotilolPlayer {
    const val CONTENT = """
            /* Icons: Solar icon set by 480 Design, CC BY 4.0: https://github.com/480-Design/Solar-Icon-Set */
            window.__splOpt=null;
            window.splPaintPlayIcon=function(playing){
                var ph=playing
                    ?'<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M2 6C2 4.11438 2 3.17157 2.58579 2.58579C3.17157 2 4.11438 2 6 2C7.88562 2 8.82843 2 9.41421 2.58579C10 3.17157 10 4.11438 10 6V18C10 19.8856 10 20.8284 9.41421 21.4142C8.82843 22 7.88562 22 6 22C4.11438 22 3.17157 22 2.58579 21.4142C2 20.8284 2 19.8856 2 18V6Z\"/><path d=\"M14 6C14 4.11438 14 3.17157 14.5858 2.58579C15.1716 2 16.1144 2 18 2C19.8856 2 20.8284 2 21.4142 2.58579C22 3.17157 22 4.11438 22 6V18C22 19.8856 22 20.8284 21.4142 21.4142C20.8284 22 19.8856 22 18 22C16.1144 22 15.1716 22 14.5858 21.4142C14 20.8284 14 19.8856 14 18V6Z\"/></g></svg>'
                    :'<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\" d=\"M20.4086 9.35258C22.5305 10.5065 22.5305 13.4935 20.4086 14.6474L7.59662 21.6145C5.53435 22.736 3 21.2763 3 18.9671L3 5.0329C3 2.72368 5.53435 1.26402 7.59661 2.38548L20.4086 9.35258Z\"/></svg>';
                var pp=document.getElementById('spl-play'),ppm=document.getElementById('spl-play-mini');
                if(pp){ pp.innerHTML=ph; pp.__splPh=ph; }
                if(ppm){ ppm.innerHTML=ph; ppm.__splPh=ph; }
            };
            window.initSpotilolPlayer=function(){
                if(document.getElementById('spotilolPlayerControls')) return;
                var npb=document.querySelector('aside[data-testid="now-playing-bar"]');
                if(!npb) return;
                npb.style.display='none';

                function splFindShuffle(){
                    return (typeof window.splShuffleBtn === 'function') ? window.splShuffleBtn() : document.querySelector('button[data-testid="control-button-shuffle"]');
                }
                function splFindRepeat(){
                    return (typeof window.splRepeatBtn === 'function') ? window.splRepeatBtn() : document.querySelector('button[data-testid="control-button-repeat"]');
                }
                function splShuffleState(){
                    return (typeof window.splShuffleState === 'function') ? window.splShuffleState() : 'off';
                }

                var pl=document.createElement('div');
                pl.id='spotilolPlayerControls';
                pl.innerHTML=''
                    +'<div class="spl-top">'
                    +'<div class="spl-cover"><img id="spl-cover-img" src="" alt=""></div>'
                    +'<div class="spl-info"><div class="spl-track" id="spl-track">No track</div>'
                    +'<div class="spl-artist" id="spl-artist">\u2014</div></div>'
                    +'<button class="spl-btn spl-btn-sm spl-liked-btn" id="spl-liked" aria-label="Like"><svg viewBox=\"0 0 24 24\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M2 9.1371C2 14 6.01943 16.5914 8.96173 18.9109C10 19.7294 11 20.5 12 20.5C13 20.5 14 19.7294 15.0383 18.9109C17.9806 16.5914 22 14 22 9.1371C22 4.27416 16.4998 0.825464 12 5.50063C7.50016 0.825464 2 4.27416 2 9.1371Z\"/></svg></button>'
                    +'<div class="spl-mini-transport">'
                    +'<button class="spl-btn" id="spl-prev-mini" aria-label="Previous"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M7.34016 9.35258C5.55328 10.5065 5.55328 13.4935 7.34015 14.6474L18.1292 21.6145C19.8658 22.736 22 21.2763 22 18.9671L22 5.0329C22 2.72368 19.8658 1.26402 18.1292 2.38548L7.34016 9.35258Z\"/><path d=\"M2 5V19\"/></g></svg></button>'
                    +'<button class="spl-btn spl-play" id="spl-play-mini" aria-label="Play"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\" d=\"M20.4086 9.35258C22.5305 10.5065 22.5305 13.4935 20.4086 14.6474L7.59662 21.6145C5.53435 22.736 3 21.2763 3 18.9671L3 5.0329C3 2.72368 5.53435 1.26402 7.59661 2.38548L20.4086 9.35258Z\"/></svg></button>'
                    +'<button class="spl-btn" id="spl-next-mini" aria-label="Next"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M16.6598 9.35258C18.4467 10.5065 18.4467 13.4935 16.6598 14.6474L5.87083 21.6145C4.13419 22.736 2 21.2763 2 18.9671L2 5.0329C2 2.72368 4.13419 1.26402 5.87083 2.38548L16.6598 9.35258Z\"/><path d=\"M22 5V19\"/></g></svg></button>'
                    +'</div>'
                    +'</div>'
                    +'<div class="spl-row2">'
                    +'<div class="spl-actions-left">'
                    +'<button class="spl-btn spl-btn-sm" id="spl-timer" aria-label="Timer"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M21 13C21 17.9706 16.9706 22 12 22C7.02944 22 3 17.9706 3 13C3 8.02944 7.02944 4 12 4C16.9706 4 21 8.02944 21 13Z\"/><path stroke-linejoin=\"round\" d=\"M12 13V9\"/><path d=\"M10 2H14\"/></g></svg></button>'
                    +'<button class="spl-btn spl-btn-sm" id="spl-pip" aria-label="Picture in Picture"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M11 21H10C6.22876 21 4.34315 21 3.17157 19.8284C2 18.6569 2 16.7712 2 13V11C2 7.22876 2 5.34315 3.17157 4.17157C4.34315 3 6.22876 3 10 3H14C17.7712 3 19.6569 3 20.8284 4.17157C22 5.34315 22 7.22876 22 11\"/><path d=\"M13 17C13 15.1144 13 14.1716 13.5858 13.5858C14.1716 13 15.1144 13 17 13H18C19.8856 13 20.8284 13 21.4142 13.5858C22 14.1716 22 15.1144 22 17C22 18.8856 22 19.8284 21.4142 20.4142C20.8284 21 19.8856 21 18 21H17C15.1144 21 14.1716 21 13.5858 20.4142C13 19.8284 13 18.8856 13 17Z\"/></g></svg></button>'
                    +'<button class="spl-btn spl-btn-sm" id="spl-nptoggle" aria-label="Now Playing"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path stroke-linejoin=\"round\" d=\"M10.5 9L13.5 12L10.5 15\"/><path d=\"M2 12C2 7.28595 2 4.92893 3.46447 3.46447C4.92893 2 7.28595 2 12 2C16.714 2 19.0711 2 20.5355 3.46447C22 4.92893 22 7.28595 22 12C22 16.714 22 19.0711 20.5355 20.5355C19.0711 22 16.714 22 12 22C7.28595 22 4.92893 22 3.46447 20.5355C2 19.0711 2 16.714 2 12Z\"/></g></svg></button>'
                    +'<button class="spl-btn spl-btn-sm" id="spl-lyrics" aria-label="Lyrics"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M7 8C7 5.23858 9.23858 3 12 3C14.7614 3 17 5.23858 17 8V11C17 13.7614 14.7614 16 12 16C9.23858 16 7 13.7614 7 11V8Z\"/><path d=\"M13 8L17 8\"/><path d=\"M13 11L17 11\"/><path d=\"M20 10V11C20 15.4183 16.4183 19 12 19C7.58172 19 4 15.4183 4 11V10\"/><path d=\"M12 19V22\"/></g></svg></button>'
                    +'<button class="spl-btn spl-btn-sm" id="spl-queue" aria-label="Queue"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M20 7L4 7\"/><path d=\"M15 12L4 12\"/><path d=\"M9 17H4\"/></g></svg></button>'
                    +'<button class="spl-btn spl-btn-sm" id="spl-download" aria-label="Download"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M17 9.00195C19.175 9.01406 20.3529 9.11051 21.1213 9.8789C22 10.7576 22 12.1718 22 15.0002V16.0002C22 18.8286 22 20.2429 21.1213 21.1215C20.2426 22.0002 18.8284 22.0002 16 22.0002H8C5.17157 22.0002 3.75736 22.0002 2.87868 21.1215C2 20.2429 2 18.8286 2 16.0002L2 15.0002C2 12.1718 2 10.7576 2.87868 9.87889C3.64706 9.11051 4.82497 9.01406 7 9.00195\"/><path stroke-linejoin=\"round\" d=\"M12 2L12 15M15 11.5L12 15L9 11.5\"/></g></svg></button>'
                    +'<button class="spl-btn spl-btn-sm" id="spl-dl-cancel" aria-label="Cancel download" style="display:none;"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><circle cx=\"12\" cy=\"12\" r=\"10\"/><path d=\"M14.5 9.50002L9.5 14.5M9.49998 9.5L14.5 14.5\"/></g></svg></button>'
                    +'<div class="spl-vol-wrap" id="spl-vol">'
                    +'<button class="spl-btn spl-btn-sm spl-vol-btn" id="spl-vol-btn" aria-label="Volume"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M1.53479 10.9714C1.60847 9.76255 1.64531 9.15814 1.95854 8.57679C2.24473 8.04563 2.7923 7.53042 3.33988 7.27707C3.93921 6.99979 4.62617 6.99979 6.00008 6.99979C6.51215 6.99979 6.76819 6.99979 7.0162 6.95791C7.26138 6.9165 7.50046 6.84478 7.72795 6.74438C7.95806 6.64283 8.17181 6.50189 8.59932 6.22002L8.81825 6.07566C11.3612 4.39898 12.6327 3.56063 13.7001 3.92487C13.9047 3.9947 14.1028 4.09551 14.2797 4.21984C15.2024 4.86829 15.2725 6.37699 15.4127 9.3944C15.4646 10.5117 15.5 11.4679 15.5 11.9998C15.5 12.5317 15.4646 13.4879 15.4127 14.6052C15.2725 17.6226 15.2024 19.1313 14.2797 19.7797C14.1028 19.9041 13.9047 20.0049 13.7001 20.0747C12.6327 20.4389 11.3612 19.6006 8.81825 17.9239L8.59932 17.7796C8.17181 17.4977 7.95806 17.3567 7.72795 17.2552C7.50046 17.1548 7.26138 17.0831 7.0162 17.0417C6.76819 16.9998 6.51215 16.9998 6.00008 16.9998C4.62617 16.9998 3.93921 16.9998 3.33988 16.7225C2.7923 16.4692 2.24473 15.9539 1.95854 15.4228C1.64531 14.8414 1.60847 14.237 1.53479 13.0282C1.51299 12.6706 1.5 12.3222 1.5 11.9998C1.5 11.6774 1.51299 11.329 1.53479 10.9714Z\"/><path d=\"M18 9C18 9 18.5 9.9 18.5 12C18.5 14.1 18 15 18 15\"/></g></svg></button>'
                    +'<div class="spl-vol-bar" id="spl-vol-bar"><div class="spl-vol-track"></div><div class="spl-vol-fill" id="spl-vol-fill"></div><div class="spl-vol-handle" id="spl-vol-handle"></div></div>'
                    +'</div>'
                    +'</div>'
                    +'</div>'
                    +'<div class="spl-bottom">'
                    +'<span class="spl-time" id="spl-pos">0:00</span>'
                    +'<div class="spl-bar-wrap"><div class="spl-bar" id="spl-bar"><div class="spl-fill" id="spl-fill"></div><div class="spl-handle" id="spl-handle"></div></div></div>'
                    +'<span class="spl-time" id="spl-dur">0:00</span>'
                    +'</div>'
                    +'<div class="spl-edgebar" id="spl-edgebar"><div class="spl-fill" id="spl-fill-edge"></div></div>'
                    +'<div class="spl-transport">'
                    +'<button class="spl-btn spl-btn-sm" id="spl-shuffle" aria-label="Shuffle"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"1.5\"><path d=\"M2 17H5.60286C7.26213 17 8.09177 17 8.77953 16.6106C9.46728 16.2212 9.89413 15.5098 10.7478 14.087L13.2522 9.91303C14.1059 8.49021 14.5327 7.7788 15.2205 7.3894C15.9082 7 16.7379 7 18.3971 7H22M20 5L22 7L20 9\"/><path d=\"M2 7H6.66762C7.28299 7 7.59068 7 7.8746 7.05526C8.51417 7.17975 9.09582 7.50908 9.53163 7.99346C9.72509 8.20848 9.88339 8.47232 10.2 9M22 17H17.3324C16.717 17 16.4093 17 16.1254 16.9447C15.4858 16.8202 14.9042 16.4909 14.4684 16.0065C14.2749 15.7915 14.1166 15.5277 13.8 15M20 19L22 17L20 15\"/></g></svg></button>'
                    +'<button class="spl-btn" id="spl-prev" aria-label="Previous"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M7.34016 9.35258C5.55328 10.5065 5.55328 13.4935 7.34015 14.6474L18.1292 21.6145C19.8658 22.736 22 21.2763 22 18.9671L22 5.0329C22 2.72368 19.8658 1.26402 18.1292 2.38548L7.34016 9.35258Z\"/><path d=\"M2 5V19\"/></g></svg></button>'
                    +'<button class="spl-btn spl-play" id="spl-play" aria-label="Play"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\" d=\"M20.4086 9.35258C22.5305 10.5065 22.5305 13.4935 20.4086 14.6474L7.59662 21.6145C5.53435 22.736 3 21.2763 3 18.9671L3 5.0329C3 2.72368 5.53435 1.26402 7.59661 2.38548L20.4086 9.35258Z\"/></svg></button>'
                    +'<button class="spl-btn" id="spl-next" aria-label="Next"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M16.6598 9.35258C18.4467 10.5065 18.4467 13.4935 16.6598 14.6474L5.87083 21.6145C4.13419 22.736 2 21.2763 2 18.9671L2 5.0329C2 2.72368 4.13419 1.26402 5.87083 2.38548L16.6598 9.35258Z\"/><path d=\"M22 5V19\"/></g></svg></button>'
                    +'<button class="spl-btn spl-btn-sm" id="spl-repeat" aria-label="Repeat"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"1.5\"><path d=\"M9.5 19H9.00028C5.13428 19 2 15.866 2 12C2 8.13401 5.13401 5 9 5H11L9 3\"/><path d=\"M15 21L13 19H15C18.866 19 22 15.866 22 12C22 8.13401 18.866 5 15 5H14.5\"/></g></svg></button>'
                    +'</div>';

                document.body.appendChild(pl);
                if(window.__splHideEmpty) pl.classList.add('spl-empty');
                    document.body.appendChild(pl);
                    
                    window.splApplyEmpty=function(){
                        var t=document.getElementById('spl-track');
                        var empty=!t||!t.textContent||t.textContent==='No track';
                        if(window.__splHideEmpty&&empty) pl.classList.add('spl-empty');
                        else pl.classList.remove('spl-empty');
                    };

                if(!document.getElementById('spl-vol-css')){
                    var sst=document.createElement('style');sst.id='spl-vol-css';
                    sst.textContent='#spotilolPlayerControls .spl-vol-wrap{display:flex;align-items:center;gap:2px;margin-right:2px}#spotilolPlayerControls .spl-vol-btn{flex-shrink:0}#spotilolPlayerControls .spl-vol-bar{position:relative;width:70px;height:38px;display:flex;align-items:center;cursor:pointer;flex-shrink:0;margin:0 2px}#spotilolPlayerControls .spl-vol-track{position:absolute;left:0;right:0;top:50%;transform:translateY(-50%);height:4px;border-radius:2px;background:rgba(255,255,255,.14)}#spotilolPlayerControls .spl-vol-fill{position:absolute;left:0;top:50%;transform:translateY(-50%);height:4px;border-radius:2px;background:var(--spl-accent,#1db954);width:0%}#spotilolPlayerControls .spl-vol-handle{position:absolute;top:50%;left:0%;width:12px;height:12px;transform:translate(-50%,-50%);border-radius:50%;background:#fff;opacity:0;transition:opacity .15s;box-shadow:0 1px 4px rgba(0,0,0,.5);pointer-events:none}#spotilolPlayerControls .spl-vol-bar:hover .spl-vol-handle,#spotilolPlayerControls .spl-vol-bar:active .spl-vol-handle{opacity:1}#spotilolPlayerControls .spl-top .spl-liked-btn{margin-left:-8px}#spotilolPlayerControls.spl-mini .spl-top .spl-liked-btn{padding:4px;min-width:30px;min-height:30px}#spotilolPlayerControls.spl-mini .spl-top .spl-liked-btn svg{width:14px;height:14px}#spotilolPlayerControls.spl-empty{opacity:0!important;pointer-events:none!important;transform:translateY(24px)!important}';
                    var t=document.head||document.documentElement;if(t)t.appendChild(sst);
                }

                if(!document.getElementById('spl-vol-css')){
                    var sst=document.createElement('style');sst.id='spl-vol-css';
                    sst.textContent='#spotilolPlayerControls .spl-vol-wrap{display:flex;align-items:center;gap:2px;margin-right:2px}#spotilolPlayerControls .spl-vol-btn{flex-shrink:0}#spotilolPlayerControls .spl-vol-bar{position:relative;width:70px;height:38px;display:flex;align-items:center;cursor:pointer;flex-shrink:0;margin:0 2px}#spotilolPlayerControls .spl-vol-track{position:absolute;left:0;right:0;top:50%;transform:translateY(-50%);height:4px;border-radius:2px;background:rgba(255,255,255,.14)}#spotilolPlayerControls .spl-vol-fill{position:absolute;left:0;top:50%;transform:translateY(-50%);height:4px;border-radius:2px;background:#1db954;width:0%}#spotilolPlayerControls .spl-vol-handle{position:absolute;top:50%;left:0%;width:12px;height:12px;transform:translate(-50%,-50%);border-radius:50%;background:#fff;opacity:0;transition:opacity .15s;box-shadow:0 1px 4px rgba(0,0,0,.5);pointer-events:none}#spotilolPlayerControls .spl-vol-bar:hover .spl-vol-handle,#spotilolPlayerControls .spl-vol-bar:active .spl-vol-handle{opacity:1}';
                    var t=document.head||document.documentElement;if(t)t.appendChild(sst);
                }

                function splOptPlay(){
                    // Timing audit (round 2): tap -> icon-paint delta. t0 is the
                    // tap-handler entry; the rAF callback runs right before the
                    // next paint — the closest cheap proxy for "icon is on
                    // screen". Emitted to logcat (tag spotilol.perf).
                    var __p0=(window.__splPerf?window.__splPerf.now():-1);
                    var st=window.splIsPlaying();
                    var target=(st===null)?null:!st;
                    var ok=window.actPlayPause(target);
                    if(target!==null&&ok!==false&&window.__splUx){
                        window.__splOpt=window.__splUx.optBeginPlay(target,Date.now());
                        window.splPaintPlayIcon(target);
                        if(__p0>=0&&window.__splPerf&&window.requestAnimationFrame){
                            try{
                                var __prf=window.__splPerf;
                                window.requestAnimationFrame(function(){
                                    try{ __prf.emit('tap-play-icon',__prf.now()-__p0); }catch(e){}
                                });
                            }catch(e){}
                        }
                    }
                }
                function splOptSkip(ok){
                    if(ok===false||!window.__splUx) return;
                    var tk=document.getElementById('spl-track');
                    window.__splOpt=window.__splUx.optBeginSkip(tk?tk.textContent:'',Date.now());
                    var fl=document.getElementById('spl-fill'),fe=document.getElementById('spl-fill-edge'),ps=document.getElementById('spl-pos');
                    if(fl) fl.style.transform='scaleX(0)';
                    if(fe) fe.style.transform='scaleX(0)';
                    if(ps) ps.textContent='0:00';
                }
                document.getElementById('spl-prev').onclick=function(){splOptSkip(window.actSkipBack())};
                document.getElementById('spl-next').onclick=function(){splOptSkip(window.actSkipForward())};
                document.getElementById('spl-play').onclick=function(){splOptPlay()};
                document.getElementById('spl-prev-mini').onclick=function(){splOptSkip(window.actSkipBack())};
                document.getElementById('spl-next-mini').onclick=function(){splOptSkip(window.actSkipForward())};
                document.getElementById('spl-play-mini').onclick=function(){splOptPlay()};
                document.getElementById('spl-shuffle').onclick=function(){var sb=splFindShuffle();if(sb&&sb.getAttribute('aria-disabled')!=='true')sb.click()};
                document.getElementById('spl-repeat').onclick=function(){actRepeat()};
                document.getElementById('spl-lyrics').onclick=function(){if(this.classList.contains('spl-disabled'))return;if(typeof closeNowPlay==='function') closeNowPlay();var lb=document.querySelector('button[data-testid=lyrics-button]');if(lb&&!lb.disabled)lb.click()};
                document.getElementById('spl-queue').onclick=function(){var qb=document.querySelector('button[data-testid=control-button-queue]');if(qb)qb.click()};
                document.getElementById('spl-vol-btn').onclick=function(){var vb=document.querySelector('button[data-testid=volume-bar-toggle-mute-button]');if(vb)vb.click()};
                (function(){
                    var vBar=document.getElementById('spl-vol-bar');
                    function splVolRange(){var r=document.querySelector('div[data-testid="volume-bar"] input[type="range"]');if(r)return r;return document.querySelector('input[type="range"][data-testid="volume-bar"]');}
                    function splSetVolPct(pct){
                        var rng=splVolRange();
                        if(!rng)return;
                        var max=parseFloat(rng.getAttribute('max'))||1;
                        var val=pct*max;
                        if(pct<=0) val=parseFloat(rng.getAttribute('min'))||0;
                        var setter=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;
                        setter.call(rng,String(val));
                        rng.dispatchEvent(new Event('input',{bubbles:true}));
                        rng.dispatchEvent(new Event('change',{bubbles:true}));
                    }
                    function volTo(e){if(!vBar)return;var r=vBar.getBoundingClientRect();var pct=Math.max(0,Math.min(1,(e.clientX-r.left)/r.width));splSetVolPct(pct);}
                    var vDrag=false;
                    vBar.addEventListener('mousedown',function(e){vDrag=true;volTo(e);});
                    vBar.addEventListener('touchstart',function(e){vDrag=true;volTo(e.touches[0]);},{passive:true});
                    document.addEventListener('mousemove',function(e){if(vDrag)volTo(e);});
                    document.addEventListener('touchmove',function(e){if(vDrag)volTo(e.touches[0]);},{passive:true});
                    document.addEventListener('mouseup',function(){vDrag=false;});
                    document.addEventListener('touchend',function(){vDrag=false;});
                    document.addEventListener('touchcancel',function(){vDrag=false;});
                })();
                document.getElementById('spl-nptoggle').onclick=function(){clickNP()};
                document.getElementById('spl-timer').onclick=function(){AndBridge.openTimerDialog()};
                document.getElementById('spl-pip').onclick=function(){
                    var pv=document.querySelector('.VideoPlayer__container video');
                    if(pv){
                        var w=pv.videoWidth||0,h=pv.videoHeight||0;
                        AndBridge.enterPipVideo(w,h);
                    } else {
                        AndBridge.enterPip();
                    }
                };
                window.__splPipFillMark=function(n){
                    if(!n) return;
                    try{
                        if(n.__splSavedStyle===undefined){
                            n.__splSavedStyle=n.getAttribute('style');
                            window.__splPipFillList=window.__splPipFillList||[];
                            window.__splPipFillList.push(n);
                        }
                    }catch(e){}
                };
                window.__splPipFillUnblock=function(n){
                    var props=[['transform','none'],['translate','none'],['rotate','none'],['scale','none'],['will-change','auto'],['contain','none'],['container-type','normal'],['filter','none'],['perspective','none'],['backdrop-filter','none'],['content-visibility','visible']];
                    var el=n;
                    while(el&&el!==document.documentElement){
                        try{
                            var cs=window.getComputedStyle(el);
                            for(var i=0;i<props.length;i++){
                                var cur=cs.getPropertyValue(props[i][0]);
                                if(cur&&cur!=='none'&&cur!=='normal'&&cur!=='auto'&&cur!=='visible'){
                                    window.__splPipFillMark(el);
                                    el.style.setProperty(props[i][0],props[i][1],'important');
                                }
                            }
                        }catch(e){}
                        el=el.parentElement;
                    }
                };
                window.__splPipFillApply=function(){
                    try{
                        var c=document.querySelector('.VideoPlayer__container');
                        var v=c?c.querySelector('video'):null;
                        if(!c||!v) return false;
                        var ctl=document.getElementById('spotilolPlayerControls');
                        window.__splPipFillMark(c);window.__splPipFillMark(v);window.__splPipFillMark(ctl);
                        window.__splPipFillUnblock(c);
                        var vv=window.visualViewport;
                        var vw=vv&&vv.width?Math.round(vv.width):(window.innerWidth||0);
                        var vh=vv&&vv.height?Math.round(vv.height):(window.innerHeight||0);
                        if(!(vw>0)||!(vh>0)) return false;
                        var vox=vv&&vv.offsetLeft?Math.round(vv.offsetLeft):0;
                        var voy=vv&&vv.offsetTop?Math.round(vv.offsetTop):0;
                        c.style.setProperty('position','fixed','important');
                        c.style.setProperty('transform','none','important');
                        c.style.setProperty('margin','0','important');
                        c.style.setProperty('padding','0','important');
                        c.style.setProperty('border','0','important');
                        c.style.setProperty('overflow','hidden','important');
                        c.style.setProperty('inset','auto','important');
                        c.style.setProperty('top',voy+'px','important');
                        c.style.setProperty('left',vox+'px','important');
                        c.style.setProperty('width',vw+'px','important');
                        c.style.setProperty('height',vh+'px','important');
                        c.style.setProperty('min-width','0','important');
                        c.style.setProperty('min-height','0','important');
                        c.style.setProperty('max-width','none','important');
                        c.style.setProperty('max-height','none','important');
                        c.style.setProperty('z-index','2147483646','important');
                        c.style.setProperty('background','#000','important');
                        v.style.setProperty('width','100%','important');
                        v.style.setProperty('height','100%','important');
                        v.style.setProperty('object-fit','contain','important');
                        v.style.setProperty('max-width','none','important');
                        v.style.setProperty('max-height','none','important');
                        if(ctl) ctl.style.setProperty('display','none','important');
                        window.__splPipFillTopLayer(c);
                        return true;
                    }catch(e){}
                    return false;
                };
                window.__splPipFillTopLayer=function(c){
                    try{
                        if(window.__splPopoverOk===undefined){
                            window.__splPopoverOk=!!(window.HTMLElement&&HTMLElement.prototype&&('showPopover' in HTMLElement.prototype));
                        }
                        if(!window.__splPopoverOk) return;
                        if(c.matches(':popover-open')) return;
                        if(!c.hasAttribute('popover')) c.setAttribute('popover','manual');
                        try{ c.showPopover(); }catch(e){}
                        if(!c.matches(':popover-open')){
                            try{ c.removeAttribute('popover'); }catch(e){}
                        }
                    }catch(e){}
                };
                window.__splPipFillRestore=function(){
                    var l=window.__splPipFillList||[];
                    for(var i=0;i<l.length;i++){
                        try{
                            var n=l[i];
                            if(n&&n.isConnected){
                                try{ if(n.matches(':popover-open')) n.hidePopover(); }catch(e){}
                                try{ if(n.hasAttribute('popover')) n.removeAttribute('popover'); }catch(e){}
                                if(n.__splSavedStyle==null) n.removeAttribute('style'); else n.setAttribute('style',n.__splSavedStyle);
                            }
                            if(n) delete n.__splSavedStyle;
                        }catch(e){}
                    }
                    window.__splPipFillList=[];
                };
                window.__splPipFillVideo=function(on){
                    try{
                        if(on){
                            if(document.fullscreenElement){ try{ document.exitFullscreen(); }catch(e){} }
                            if(!window.__splFillOn){
                                window.__splFillOn=true;
                                if(!window.__splFillHooked){
                                    window.__splFillHooked=true;
                                    var re=function(){ if(window.__splFillOn) window.__splPipFillApply(); };
                                    window.addEventListener('orientationchange',re);
                                    window.addEventListener('resize',re);
                                    try{
                                        if(window.visualViewport){
                                            window.visualViewport.addEventListener('resize',re);
                                            window.visualViewport.addEventListener('scroll',re);
                                        }
                                    }catch(e){}
                                }
                            }
                            if(!window.__splFillTimer) window.__splFillTimer=setInterval(function(){ if(window.__splFillOn) window.__splPipFillApply(); },700);
                            return window.__splPipFillApply();
                        }
                        if(!window.__splFillOn) return true;
                        window.__splFillOn=false;
                        if(window.__splFillTimer){ clearInterval(window.__splFillTimer); window.__splFillTimer=null; }
                        window.__splPipFillRestore();
                        return true;
                    }catch(e){}
                    return false;
                };
                document.getElementById('spl-liked').onclick=function(){actAddToFav()};
                document.getElementById('spl-download').onclick=function(){splDoDownload()};
                document.getElementById('spl-dl-cancel').onclick=function(){ try{ AndBridge.cancelDownload(); }catch(e){} };

                var splTrack=document.getElementById('spl-track');
                var splArtist=document.getElementById('spl-artist');
                splTrack.style.cursor='pointer';
                splArtist.style.cursor='pointer';
                splTrack.onclick=function(){
                    if(pl.classList.contains('spl-mini'))return;
                    if(typeof closeNowPlay==='function') closeNowPlay();
                    var rl=document.querySelector('a[data-testid=context-item-link]');
                    if(rl){rl.click();}
                };
                splArtist.onclick=function(){
                    if(pl.classList.contains('spl-mini'))return;
                    if(typeof closeNowPlay==='function') closeNowPlay();
                    var al=document.querySelector('a[data-testid=context-item-info-artist]');
                    if(!al) al=document.querySelector('a[data-testid=context-item-info-show]');
                    if(al){al.click();}
                };

                var barEl=document.getElementById('spl-bar');
                var edgeBarEl=document.getElementById('spl-edgebar');
                var dragging=false,dragEl=barEl;
                function seekTo(el,e){var r=el.getBoundingClientRect();var pct=Math.max(0,Math.min(1,(e.clientX-r.left)/r.width));var rg=document.querySelector('[data-testid="playback-progressbar"] input[type=range]');var mx=parseInt(rg?rg.getAttribute('max'):0)||1;actSeek(Math.round(pct*mx))}
                function bindSeek(el){el.addEventListener('mousedown',function(e){dragEl=el;dragging=true;seekTo(el,e)});el.addEventListener('touchstart',function(e){dragEl=el;dragging=true;seekTo(el,e.touches[0])},{passive:true});}
                bindSeek(barEl);bindSeek(edgeBarEl);
                document.addEventListener('mousemove',function(e){if(dragging)seekTo(dragEl,e)});
                document.addEventListener('touchmove',function(e){if(dragging)seekTo(dragEl,e.touches[0])},{passive:true});
                document.addEventListener('mouseup',function(){dragging=false});
                document.addEventListener('touchend',function(){dragging=false});

                var splMini=false;
                var splDrag=null,splSuppressClick=false,splLastDragEnd=0;
                function splSetMini(m){
                    splMini=!!m;
                    window.splMiniPref=splMini;
                    pl.classList.toggle('spl-mini',splMini);
                }
                function splDragStart(x,y){
                    splDrag={sx:x,sy:y,moving:false,dy:0,mini:splMini};
                    pl.style.transition='none';
                }
                function splDragMove(x,y){
                    if(!splDrag)return;
                    var dy=y-splDrag.sy,dx=x-splDrag.sx;
                    if(!splDrag.moving){
                        if(Math.abs(dy)<10||Math.abs(dy)<Math.abs(dx))return;
                        splDrag.moving=true;
                    }
                    splDrag.dy=splDrag.mini?Math.min(0,dy):Math.max(0,dy);
                    pl.style.transform='translateY('+splDrag.dy+'px)';
                    pl.style.opacity=String(Math.max(.7,1-Math.abs(splDrag.dy)/500));
                }
                function splDragEnd(){
                    if(!splDrag)return;
                    var d=splDrag;
                    splDrag=null;
                    pl.style.transition='';
                    pl.style.transform='';
                    pl.style.opacity='';
                    if(d.moving){
                        splSuppressClick=true;
                        splLastDragEnd=Date.now();
                        setTimeout(function(){splSuppressClick=false;},100);
                        if(d.mini){if(d.dy<-70)splSetMini(false);}
                        else{if(d.dy>70)splSetMini(true);}
                    }
                }
                pl.addEventListener('touchstart',function(e){if(e.target.closest('#spl-bar')||e.target.closest('#spl-edgebar')||e.target.closest('.spl-vol-bar'))return;var t=e.touches[0];splDragStart(t.clientX,t.clientY);},{passive:true});
                pl.addEventListener('touchmove',function(e){if(splDrag&&splDrag.moving)e.preventDefault();if(!splDrag)return;var t=e.touches[0];splDragMove(t.clientX,t.clientY);},{passive:false});
                pl.addEventListener('touchend',function(e){if(splDrag&&splDrag.moving)e.preventDefault();splDragEnd();});
                pl.addEventListener('touchcancel',function(){splDragEnd();});
                pl.addEventListener('mousedown',function(e){if(e.button!==0)return;if(e.target.closest('#spl-bar')||e.target.closest('#spl-edgebar')||e.target.closest('.spl-vol-bar')||e.target.closest('button'))return;splDragStart(e.clientX,e.clientY);});
                document.addEventListener('mousemove',function(e){splDragMove(e.clientX,e.clientY);});
                document.addEventListener('mouseup',function(){splDragEnd();});
                pl.addEventListener('click',function(e){if(splSuppressClick)return;if(Date.now()-splLastDragEnd<400)return;if(splMini&&!e.target.closest('button')&&!e.target.closest('#spl-bar')&&!e.target.closest('#spl-edgebar'))splSetMini(false);});                    // Cached getElementById with isConnected validation (re-resolves if DOM rebuilt)
                    var __splElCache={};
                    function splEl(id){
                        var el=__splElCache[id];
                        if(!el||!el.isConnected){ el=document.getElementById(id); __splElCache[id]=el; }
                        return el;
                    }
                    window.splUpdate=function(){
                        var ci=splEl('spl-cover-img');
                        var tk=splEl('spl-track');
                        var ar=splEl('spl-artist');
                        var fl=splEl('spl-fill');
                        var fe=splEl('spl-fill-edge');
                        var hd=splEl('spl-handle');
                        var ps=splEl('spl-pos');
                        var ds=splEl('spl-dur');
                        var pp=splEl('spl-play');
                        var ppm=splEl('spl-play-mini');
                        var sh=splEl('spl-shuffle');
                        var rp=splEl('spl-repeat');
                        var vl=splEl('spl-vol');
                        var lk=splEl('spl-liked');
                        var ly=splEl('spl-lyrics');
                        var tm=splEl('spl-timer');

                        var npb=document.querySelector('[data-testid="now-playing-widget"]');
                        var imgEl=npb?npb.querySelector('img[data-testid="cover-art-image"]'):null;
                        if(ci&&imgEl&&imgEl.src&&ci.src!==imgEl.src) ci.src=imgEl.src;

                        var trackEl=document.querySelector('a[data-testid=context-item-link]');
                        if(tk&&trackEl&&trackEl.textContent&&tk.textContent!==trackEl.textContent) tk.textContent=trackEl.textContent;

                        var artistEl=document.querySelector('a[data-testid=context-item-info-artist]');
                        if(!artistEl) artistEl=document.querySelector('a[data-testid=context-item-info-show]');
                        if(ar&&artistEl&&tk.textContent!=='No track') ar.textContent=artistEl.textContent||'';

                        var rg=document.querySelector('[data-testid="playback-progressbar"] input[type=range]');
                        var __optHold=null;
                        if(window.__splUx&&window.__splOpt){
                            var __opt=window.__splOpt;
                            var __rec=window.__splUx.optReconcile(__opt,{playing:window.splIsPlayingSticky(),title:(tk&&tk.textContent)||''},Date.now());
                            if(__rec.action==='confirm'){ window.__splOpt=null; }
                            else if(__rec.action==='revert'){
                                var __k=__opt.kind; window.__splOpt=null;
                                if(typeof window.splToast==='function') window.splToast(__k==='play'?'Could not change playback':'Skip failed');
                            }
                            else if(__rec.action==='keep'){ __optHold=__opt.kind; }
                        }
                        if((pp||ppm)&&__optHold!=='play'){
                            var isPlaying=window.splIsPlayingSticky();
                            var ph=isPlaying
                                ?'<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M2 6C2 4.11438 2 3.17157 2.58579 2.58579C3.17157 2 4.11438 2 6 2C7.88562 2 8.82843 2 9.41421 2.58579C10 3.17157 10 4.11438 10 6V18C10 19.8856 10 20.8284 9.41421 21.4142C8.82843 22 7.88562 22 6 22C4.11438 22 3.17157 22 2.58579 21.4142C2 20.8284 2 19.8856 2 18V6Z\"/><path d=\"M14 6C14 4.11438 14 3.17157 14.5858 2.58579C15.1716 2 16.1144 2 18 2C19.8856 2 20.8284 2 21.4142 2.58579C22 3.17157 22 4.11438 22 6V18C22 19.8856 22 20.8284 21.4142 21.4142C20.8284 22 19.8856 22 18 22C16.1144 22 15.1716 22 14.5858 21.4142C14 20.8284 14 19.8856 14 18V6Z\"/></g></svg>'
                                :'<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\" d=\"M20.4086 9.35258C22.5305 10.5065 22.5305 13.4935 20.4086 14.6474L7.59662 21.6145C5.53435 22.736 3 21.2763 3 18.9671L3 5.0329C3 2.72368 5.53435 1.26402 7.59661 2.38548L20.4086 9.35258Z\"/></svg>';
                            if(pp && pp.__splPh !== ph){ pp.innerHTML = ph; pp.__splPh = ph; }
                            if(ppm && ppm.__splPh !== ph){ ppm.innerHTML = ph; ppm.__splPh = ph; }
                        }
                        if(sh){
                            var sst=splShuffleState();
                            sh.classList.toggle('spl-active',sst==='shuffle'||sst==='smart');
                            sh.classList.toggle('spl-disabled',sst==='disabled');
                            var isSmart=sst==='smart';
                            var hasSparkle=!!sh.querySelector('.spl-sparkle');
                            if(isSmart&&!hasSparkle){
                                sh.innerHTML='<svg class=\"spl-sparkle\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\"><path stroke=\"currentColor\" stroke-width=\"1.5\" d=\"M12.619 3.778c2.173-1.388 3.26-2.081 4.095-1.651s.866 1.698.928 4.236l.016.657c.017.72.026 1.081.175 1.393c.148.311.42.538.963.992l.495.413c1.911 1.598 2.867 2.397 2.688 3.342c-.18.946-1.374 1.412-3.765 2.344l-.618.241c-.68.265-1.02.398-1.274.647c-.255.249-.395.586-.677 1.261l-.257.614c-.992 2.375-1.488 3.563-2.434 3.717c-.946.155-1.715-.826-3.254-2.788l-.398-.507c-.438-.558-.656-.836-.962-.994c-.306-.157-.665-.176-1.382-.213l-.654-.033c-2.524-.13-3.786-.195-4.192-1.045c-.405-.85.314-1.922 1.753-4.067l.373-.555c.408-.61.613-.914.679-1.26c.065-.347-.016-.696-.177-1.393l-.147-.635C4.024 6.039 3.74 4.81 4.436 4.13c.695-.68 1.91-.362 4.338.275l.628.164c.69.181 1.035.271 1.382.215c.346-.057.655-.254 1.272-.648z\"/><path fill=\"currentColor\" d=\"M19.53 18.47a.75.75 0 1 0-1.06 1.06zm.94 3.06a.75.75 0 1 0 1.06-1.06zm-2-2l2 2l1.06-1.06l-2-2z\"/></g></svg>';
                            } else if(!isSmart&&hasSparkle){
                                sh.innerHTML='<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"1.5\"><path d=\"M2 17H5.60286C7.26213 17 8.09177 17 8.77953 16.6106C9.46728 16.2212 9.89413 15.5098 10.7478 14.087L13.2522 9.91303C14.1059 8.49021 14.5327 7.7788 15.2205 7.3894C15.9082 7 16.7379 7 18.3971 7H22M20 5L22 7L20 9\"/><path d=\"M2 7H6.66762C7.28299 7 7.59068 7 7.8746 7.05526C8.51417 7.17975 9.09582 7.50908 9.53163 7.99346C9.72509 8.20848 9.88339 8.47232 10.2 9M22 17H17.3324C16.717 17 16.4093 17 16.1254 16.9447C15.4858 16.8202 14.9042 16.4909 14.4684 16.0065C14.2749 15.7915 14.1166 15.5277 13.8 15M20 19L22 17L20 15\"/></g></svg>';
                            }
                        }
                        if(rp){
                            var rr=splFindRepeat();
                            var rc=rr?rr.getAttribute('aria-checked'):null;
                            var rDisabled=!!(rr&&(rr.disabled||rr.getAttribute('aria-disabled')==='true'));
                            rp.classList.toggle('spl-active',rc==='true'||rc==='mixed');
                            rp.classList.toggle('spl-disabled',rDisabled);
                            rp.classList.toggle('spl-repeat-track',rc==='mixed');
                            if(rc==='mixed'&&!rp.getAttribute('data-rt')){
                                rp.setAttribute('data-rt','1');
                                rp.innerHTML='<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"1.5\"><path d=\"M9.5 19H9.00028C5.13428 19 2 15.866 2 12C2 8.13401 5.13401 5 9 5H11L9 3\"/><path d=\"M15 21L13 19H15C18.866 19 22 15.866 22 12C22 8.13401 18.866 5 15 5H14.5\"/><path d=\"M10.5 11.5L12 10V14\"/></g></svg>';
                            } else if(rc!=='mixed'&&rp.getAttribute('data-rt')){
                                rp.removeAttribute('data-rt');
                                rp.innerHTML='<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"1.5\"><path d=\"M9.5 19H9.00028C5.13428 19 2 15.866 2 12C2 8.13401 5.13401 5 9 5H11L9 3\"/><path d=\"M15 21L13 19H15C18.866 19 22 15.866 22 12C22 8.13401 18.866 5 15 5H14.5\"/></g></svg>';
                            }
                        }
                        if(lk){
                            var fb=document.querySelector('div[data-testid="now-playing-widget"] button[aria-label="Save to Your Library"], div[data-testid="now-playing-widget"] button[aria-label="Remove from Your Library"]');
                            var liked=fb&&fb.getAttribute('aria-checked')==='true';
                            lk.classList.toggle('spl-active',liked===true);
                        }
                        if(vl){
                            var vbb=splEl('spl-vol-btn');
                            var vf=splEl('spl-vol-fill');
                            var vh=splEl('spl-vol-handle');
                            var vrb=document.querySelector('button[data-testid=volume-bar-toggle-mute-button]');
                            var vrg=document.querySelector('div[data-testid="volume-bar"] input[type="range"]')||document.querySelector('input[type="range"][data-testid="volume-bar"]');
                            var vpct=0;
                            if(vrg){vpct=parseFloat(vrg.value||'0')/(parseFloat(vrg.getAttribute('max'))||1);}
                            var vmic=vrb?vrb.querySelector('svg path'):null;
                            var muted=(vmic&&(vmic.getAttribute('d')||'').indexOf('M13.86 5.47')===0)||vpct<=0;
                            vl.classList.toggle('spl-active',muted===true);
                            var hasX=vbb&&!!vbb.querySelector('.spl-mute-x');
                            if(muted&&!hasX){
                                vbb.innerHTML='<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M15.5 11.9998C15.5 12.5317 15.4646 13.4879 15.4127 14.6052C15.2725 17.6226 15.2024 19.1313 14.2797 19.7797C14.1028 19.9041 13.9047 20.0049 13.7001 20.0747C12.7327 20.4048 11.5975 19.747 9.5 18.3725M7.0162 17.0417C6.76819 16.9998 6.51215 16.9998 6.00008 16.9998C4.62617 16.9998 3.93921 16.9998 3.33988 16.7225C2.7923 16.4692 2.24473 15.9539 1.95854 15.4228C1.64531 14.8414 1.60847 14.237 1.53479 13.0282C1.51299 12.6706 1.5 12.3222 1.5 11.9998C1.5 11.6774 1.51299 11.329 1.53479 10.9714C1.60847 9.76255 1.64531 9.15814 1.95854 8.57679C2.24473 8.04563 2.7923 7.53042 3.33988 7.27707C3.93921 6.99979 4.62617 6.99979 6.00008 6.99979C6.51215 6.99979 6.76819 6.99979 7.0162 6.95791C7.26138 6.9165 7.50046 6.84478 7.72795 6.74438C7.95806 6.64283 8.17181 6.50189 8.59932 6.22002L8.81825 6.07566C11.3612 4.39898 12.6327 3.56063 13.7001 3.92487C13.9047 3.9947 14.1028 4.09551 14.2797 4.21984C15.115 4.80685 15.2516 6.09882 15.374 8.57679\"/><path d=\"M20 18C20 18 21.5 16.2 21.5 12C21.5 9.56658 20.9965 7.93882 20.5729 7\"/><path d=\"M18 15C18 15 18.5 14.1 18.5 12C18.5 11.1381 18.4158 10.4784 18.3165 10\"/><path d=\"M22 2L2 22\"/></g></svg>';
                            } else if(!muted&&hasX){
                                vbb.innerHTML='<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><g fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-width=\"1.5\"><path d=\"M1.53479 10.9714C1.60847 9.76255 1.64531 9.15814 1.95854 8.57679C2.24473 8.04563 2.7923 7.53042 3.33988 7.27707C3.93921 6.99979 4.62617 6.99979 6.00008 6.99979C6.51215 6.99979 6.76819 6.99979 7.0162 6.95791C7.26138 6.9165 7.50046 6.84478 7.72795 6.74438C7.95806 6.64283 8.17181 6.50189 8.59932 6.22002L8.81825 6.07566C11.3612 4.39898 12.6327 3.56063 13.7001 3.92487C13.9047 3.9947 14.1028 4.09551 14.2797 4.21984C15.2024 4.86829 15.2725 6.37699 15.4127 9.3944C15.4646 10.5117 15.5 11.4679 15.5 11.9998C15.5 12.5317 15.4646 13.4879 15.4127 14.6052C15.2725 17.6226 15.2024 19.1313 14.2797 19.7797C14.1028 19.9041 13.9047 20.0049 13.7001 20.0747C12.6327 20.4389 11.3612 19.6006 8.81825 17.9239L8.59932 17.7796C8.17181 17.4977 7.95806 17.3567 7.72795 17.2552C7.50046 17.1548 7.26138 17.0831 7.0162 17.0417C6.76819 16.9998 6.51215 16.9998 6.00008 16.9998C4.62617 16.9998 3.93921 16.9998 3.33988 16.7225C2.7923 16.4692 2.24473 15.9539 1.95854 15.4228C1.64531 14.8414 1.60847 14.237 1.53479 13.0282C1.51299 12.6706 1.5 12.3222 1.5 11.9998C1.5 11.6774 1.51299 11.329 1.53479 10.9714Z\"/><path d=\"M18 9C18 9 18.5 9.9 18.5 12C18.5 14.1 18 15 18 15\"/></g></svg>';
                            }
                            if(vf) vf.style.width=(Math.max(0,Math.min(1,vpct))*100)+'%';
                            if(vh) vh.style.left=(Math.max(0,Math.min(1,vpct))*100)+'%';
                        }
                        var lb=document.querySelector('button[data-testid=lyrics-button]');
                        if(lb){
                            ly.style.display='';
                            ly.classList.toggle('spl-disabled',lb.disabled||lb.getAttribute('aria-disabled')==='true');
                        } else {
                            ly.style.display='none';
                        }
                        if(tm) tm.classList.toggle('spl-active',typeof sleepTimerActive!=='undefined'&&sleepTimerActive&&sleepTimerActive.value);
                        var dcb=splEl('spl-dl-cancel');
                        if(dcb) dcb.style.display = window.__splDlActive ? '' : 'none';

                        var pbEl=document.querySelector('[data-testid="playback-progressbar"] [data-testid="progress-bar"]');
                        if(pbEl&&__optHold!=='skip'){
                            var cs=getComputedStyle(pbEl);
                            var tr=cs.getPropertyValue('--progress-bar-transform');
                            if(tr){
                                var pct=parseFloat(tr)||0;
                                if(fl) fl.style.transform='scaleX('+(pct/100)+')';
                                if(fe) fe.style.transform='scaleX('+(pct/100)+')';
                                if(hd) hd.style.left=pct+'%';
                            }
                        }
                        var posEl=document.querySelector('[data-testid="playback-position"]');
                        var durEl=document.querySelector('[data-testid="playback-duration"]');
                        if(ps&&posEl) ps.textContent=posEl.textContent;
                        if(ds&&durEl) ds.textContent=durEl.textContent;
                        splApplyEmpty();
                    };
                    function formatTime(ms){
                        var t=Math.floor(ms/1000);
                        return Math.floor(t/60)+':'+(t%60<10?'0':'')+t%60;
                    }

                    var rafLastTime=0;
                    function rafUpdate(timestamp){
                        if(timestamp-rafLastTime>500){
                            if(document.visibilityState!=='hidden'&&!window.__splBg) splUpdate();
                            rafLastTime=timestamp;
                        }
                        requestAnimationFrame(rafUpdate);
                    }
                    if(window.splMiniPref) splSetMini(true);
                    requestAnimationFrame(rafUpdate);
            };
            if(document.readyState==='complete') initSpotilolPlayer();
            else window.addEventListener('load',initSpotilolPlayer);
            setInterval(function(){
                if(window.__splBg) return;
                var npb=document.querySelector('aside[data-testid="now-playing-bar"]');
                if(npb&&npb.style.display!=='none') initSpotilolPlayer();
            },3000);
        
    """
}
