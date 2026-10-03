// Seeded from window.__bridge (mock in the VM, real seams in the app).
    // Falls back to the authored demo values when the bridge is absent.
    const __b = (typeof window !== 'undefined' && window.__bridge) || {};
    const __bt = __b.track || {};
    let isPlaying = typeof __b.playing === 'boolean' ? __b.playing : true;
    let isLiked = typeof __b.liked === 'boolean' ? __b.liked : true;
    let activeTrack = {
      title: __bt.title || 'Nothing playing',
      artist: __bt.artist || 'Pick something to listen to',
      thumb: __bt.art || 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7',
      colorToken: 'sp-accent-congaso'
    };

    function resolveAccent(tokenName) {
      const val = getComputedStyle(document.documentElement).getPropertyValue('--' + tokenName).trim();
      return val || getComputedStyle(document.documentElement).getPropertyValue('--sp-accent-congaso').trim() || 'var(--sp-accent-congaso)';
    }

    function showToast(text) {
      const toast = document.getElementById('spToast');
      toast.innerText = text;
      toast.classList.add('show');
      closeAllSheets();
      setTimeout(() => {
        toast.classList.remove('show');
      }, 2200);
    }

    // ---- tap observability (diagnostic, no UX noise) ----
    // Ring buffer of recent taps; long-press the header avatar to inspect.
    window.__splTapLog = window.__splTapLog || [];
    window.__splLastError = window.__splLastError || null;
    function __splLogTap(what) {
      try {
        window.__splTapLog.push({ t: Date.now(), what: String(what) });
        while (window.__splTapLog.length > 20) window.__splTapLog.shift();
      } catch (e) {}
    }
    window.__splLogTap = __splLogTap;

    function openSheet(id) {
      document.getElementById('modalBackdrop').classList.add('open');
      document.getElementById(id).classList.add('open');
    }

    function closeAllSheets() {
      document.getElementById('modalBackdrop').classList.remove('open');
      document.querySelectorAll('.bottom-sheet').forEach(sheet => sheet.classList.remove('open'));
    }

    function openProfileMenu() { openSheet('profileSheet'); }
    function openDeviceMenu(e) { if (e) e.stopPropagation(); openSheet('deviceSheet'); }
    function openShareSheet() { closeAllSheets(); openSheet('shareSheet'); }
    function openCreateMenu() { openSheet('createSheet'); }
    function openSortMenu() { openSheet('sortSheet'); }

    function selectSort(label) {
      document.getElementById('currentSortLabel').innerText = label;
      showToast(`Sorted by ${label}`);
    }

    function openSongMenu(e, title, artist, thumb) {
      if (e) e.stopPropagation();
      document.getElementById('menuTitle').innerText = title;
      document.getElementById('menuArtist').innerText = artist;
      document.getElementById('menuThumb').src = thumb;
      openSheet('songMenuSheet');
    }

    function openCurrentTrackMenu() {
      openSongMenu(null, activeTrack.title, activeTrack.artist, activeTrack.thumb);
    }

    function actionLikeCurrent() {
      toggleLike();
      showToast(isLiked ? 'Added to Liked Songs' : 'Removed from Liked Songs');
    }

    // Filter chips (solid white when active). Single active chip across
    // all rows: clears every .chip so Search/Library rows never double up.
    function filterChip(btn, type) {
      document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
      btn.classList.add('active');
      showToast(`Showing ${type}`);
    }

    function switchTab(tabName, element) {
      try {
        __splLogTap('switchTab:' + tabName);
        document.querySelectorAll('.tab-view').forEach(view => view.classList.remove('active'));
        document.querySelectorAll('.nav-tab').forEach(btn => btn.classList.remove('active'));
        document.getElementById('playlistLiked').classList.remove('active');
        
        const header = document.getElementById('mainHeader');
        if (tabName === 'Home') {
          document.getElementById('tabHome').classList.add('active');
          header.style.display = 'flex';
        } else if (tabName === 'Search') {
          document.getElementById('tabSearch').classList.add('active');
          header.style.display = 'none';
        } else if (tabName === 'Library') {
          document.getElementById('tabLibrary').classList.add('active');
          header.style.display = 'none';
        }
        
        element.classList.add('active');
        document.getElementById('mainScrollArea').scrollTo({ top: 0, behavior: 'instant' });
      } catch (e) {
        window.__splLastError = String((e && e.message) || e);
        try { showToast('Error: ' + ((e && e.message) || e)); } catch (x) {}
      }
    }
    window.switchTab = switchTab;

    function handleSearch(query) {
      const clearBtn = document.getElementById('searchClearBtn');
      const resultsArea = document.getElementById('searchResultsArea');
      const defaultArea = document.getElementById('defaultBrowseArea');
      
      if (query.trim().length > 0) {
        clearBtn.classList.remove('search-clear-hidden');
        resultsArea.classList.remove('search-results-hidden');
        defaultArea.classList.add('default-browse-hidden');
      } else {
        clearBtn.classList.add('search-clear-hidden');
        resultsArea.classList.add('search-results-hidden');
        defaultArea.classList.remove('default-browse-hidden');
      }
    }

    function clearSearch() {
      const input = document.getElementById('searchInput');
      input.value = '';
      handleSearch('');
      input.focus();
    }

    function openPlaylistView() {
      document.querySelectorAll('.tab-view').forEach(view => view.classList.remove('active'));
      document.getElementById('playlistLiked').classList.add('active');
      document.getElementById('mainHeader').style.display = 'none';
      document.getElementById('mainScrollArea').scrollTo({ top: 0, behavior: 'instant' });
    }

    function closePlaylistView() {
      document.getElementById('playlistLiked').classList.remove('active');
      document.getElementById('tabHome').classList.add('active');
      document.getElementById('mainHeader').style.display = 'flex';
    }

    // Open a generic playlist/album detail view with real tracks.
    // Repurposes the #playlistLiked section: sets the title, clears the
    // song list, shows a loading row, then fetches tracks from the
    // Spotify Web API. All API data is rendered via innerText / property
    // assignment only (XSS-safe, never innerHTML).
    function openPlaylistDetail(uri, name, image) {
      try {
        __splLogTap('openPlaylistDetail:' + (name || uri || ''));
        var section = document.getElementById('playlistLiked');
        if (!section) { showToast('View not available'); return; }

        // Switch views (same pattern as openPlaylistView)
        document.querySelectorAll('.tab-view').forEach(function(v) { v.classList.remove('active'); });
        section.classList.add('active');
        document.getElementById('mainHeader').style.display = 'none';
        document.getElementById('mainScrollArea').scrollTo({ top: 0, behavior: 'instant' });

        var titleEl = section.querySelector('.playlist-title-large');
        if (titleEl) titleEl.innerText = name || 'Playlist';
        var statsEl = section.querySelector('.playlist-stats');
        if (statsEl) statsEl.innerText = '';

        // Header play button plays the whole playlist/album
        var playBtn = section.querySelector('.play-btn-circle');
        if (playBtn) {
          playBtn.onclick = function() {
            if (typeof window.playFromUri === 'function') {
              try { window.playFromUri(uri); } catch (e) {}
            }
          };
        }

        // Clear the song list and show a loading row
        var list = section.querySelector('.song-list');
        if (list) {
          list.innerHTML = '';
          var loading = document.createElement('div');
          loading.className = 'song-row';
          var linfo = document.createElement('div');
          linfo.className = 'song-info';
          var lname = document.createElement('div');
          lname.className = 'song-name';
          lname.innerText = 'Loading...';
          linfo.appendChild(lname);
          loading.appendChild(linfo);
          list.appendChild(loading);
        }

        // Auth guard
        if (!window.spotAuthToken) {
          if (list) list.innerHTML = '';
          showToast("Couldn't load tracks");
          return;
        }

        var url = null;
        if (uri && uri.indexOf('collection') !== -1) {
          url = 'https://api.spotify.com/v1/me/tracks?limit=50';
        } else {
          var m = /spotify:(playlist|album):([^:/?#]+)/.exec(uri || '');
          if (!m) {
            if (list) list.innerHTML = '';
            showToast("Couldn't load tracks");
            return;
          }
          url = m[1] === 'album'
            ? 'https://api.spotify.com/v1/albums/' + m[2] + '/tracks?limit=50'
            : 'https://api.spotify.com/v1/playlists/' + m[2] + '/tracks?limit=50&fields=items(track(uri,name,artists(name),album(images)))';
        }

        fetch(url, { headers: { 'Authorization': window.spotAuthToken } })
          .then(function(r) { return r.json(); })
          .then(function(data) {
            try {
              var tracks = [];
              (data.items || []).forEach(function(it) {
                var t = it.track || it;
                if (t && t.uri) tracks.push(t);
              });
              if (statsEl) statsEl.innerText = tracks.length + (tracks.length === 1 ? ' song' : ' songs');
              renderPlaylistTracks(list, tracks, uri);
            } catch (e) {
              window.__splLastError = String((e && e.message) || e);
            }
          })
          .catch(function(e) {
            window.__splLastError = String((e && e.message) || e);
            if (list) list.innerHTML = '';
            showToast("Couldn't load tracks");
          });
      } catch (e) {
        window.__splLastError = String((e && e.message) || e);
        try { showToast('Error: ' + ((e && e.message) || e)); } catch (x) {}
      }
    }
    window.openPlaylistDetail = openPlaylistDetail;

    // Render track rows into a .song-list using the existing .song-row
    // markup pattern. Row tap plays the track with the playlist as context.
    function renderPlaylistTracks(list, tracks, contextUri) {
      if (!list) return;
      list.innerHTML = '';
      if (!tracks || !tracks.length) {
        var empty = document.createElement('div');
        empty.className = 'song-row';
        var einfo = document.createElement('div');
        einfo.className = 'song-info';
        var ename = document.createElement('div');
        ename.className = 'song-name';
        ename.innerText = 'No tracks found';
        einfo.appendChild(ename);
        empty.appendChild(einfo);
        list.appendChild(empty);
        return;
      }
      tracks.forEach(function(t) {
        var row = document.createElement('div');
        row.className = 'song-row';
        var art = document.createElement('img');
        art.className = 'song-art';
        var imgs = (t.album && t.album.images) || [];
        var imgUrl = imgs.length ? imgs[imgs.length - 1].url : '';
        if (imgUrl) art.src = imgUrl;
        art.alt = t.name || '';
        row.appendChild(art);
        var info = document.createElement('div');
        info.className = 'song-info';
        var nameEl = document.createElement('div');
        nameEl.className = 'song-name';
        nameEl.innerText = t.name || 'Unknown';
        info.appendChild(nameEl);
        var sub = document.createElement('div');
        sub.className = 'song-subtitle';
        var subSpan = document.createElement('span');
        var artists = (t.artists || []).map(function(a) { return a && a.name; }).filter(Boolean).join(', ');
        subSpan.innerText = artists;
        sub.appendChild(subSpan);
        info.appendChild(sub);
        row.appendChild(info);
        row.addEventListener('click', function() {
          if (typeof window.playFromUri === 'function') {
            try { window.playFromUri(t.uri, contextUri); } catch (e) {}
          }
        });
        list.appendChild(row);
      });
    }
    window.renderPlaylistTracks = renderPlaylistTracks;

    function openFullPlayer() {
      document.getElementById('fullPlayer').classList.add('open');
    }

    function closeFullPlayer() {
      document.getElementById('fullPlayer').classList.remove('open');
    }

    function openLyricsView() {
      document.getElementById('lyricsOverlay').classList.add('open');
    }

    function closeLyricsView() {
      document.getElementById('lyricsOverlay').classList.remove('open');
    }

    function togglePlayState() {
      isPlaying = !isPlaying;
      if (__b.toggle) { try { __b.toggle(); } catch (e) {} }
      const pauseSvg = '<path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>';
      const playSvg = '<path d="M8 5v14l11-7z"/>';
      
      const newPath = isPlaying ? pauseSvg : playSvg;
      document.getElementById('miniPlayIcon').innerHTML = newPath;
      document.getElementById('fpPlayIcon').innerHTML = newPath;
      document.getElementById('headerPlayIcon').innerHTML = newPath;
    }

    // Play track with dynamic album accent background
    function playSong(title, artist, thumb, accentColor, uri) {
      // If a Spotify URI is provided, play it for real via the bridge seams.
      if (uri && typeof window.playFromUri === 'function') {
        try { window.playFromUri(uri); } catch (e) {}
      }
      activeTrack.title = title;
      activeTrack.artist = artist;
      activeTrack.thumb = thumb;
      activeTrack.colorToken = accentColor || 'sp-accent-congaso';

      document.getElementById('miniTitle').innerText = title;
      document.getElementById('miniArtist').innerText = artist;
      document.getElementById('miniThumb').src = thumb;

      document.getElementById('fpTitle').innerText = title;
      document.getElementById('fpArtist').innerText = artist;
      document.getElementById('fpArtwork').src = thumb;

      // Dynamically adapt full player gradient background to album art
      var accent = resolveAccent(activeTrack.colorToken);
      document.getElementById('fullPlayer').style.background = 
        `linear-gradient(180deg, ${accent} 0%, var(--sp-black) 60%, var(--sp-black) 100%)`;

      isPlaying = true;
      const pauseSvg = '<path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>';
      document.getElementById('miniPlayIcon').innerHTML = pauseSvg;
      document.getElementById('fpPlayIcon').innerHTML = pauseSvg;
      document.getElementById('headerPlayIcon').innerHTML = pauseSvg;
      openFullPlayer();
    }

    // Play a real Spotify playlist/album/artist by URI. Used by the
    // dynamically-populated home grid (see CustomUI.kt loadRealHomeContent).
    function playPlaylist(uri, name, image) {
      if (uri && typeof window.playFromUri === 'function') {
        try { window.playFromUri(uri); } catch (e) {}
      }
      // Optimistic UI update; the live sync loop corrects it when the
      // real track metadata arrives.
      var title = name || 'Playing...';
      activeTrack.title = title;
      activeTrack.artist = '';
      activeTrack.thumb = image || activeTrack.thumb;
      activeTrack.colorToken = 'sp-accent-congaso';
      try {
        document.getElementById('miniTitle').innerText = title;
        document.getElementById('miniArtist').innerText = '';
        if (image) document.getElementById('miniThumb').src = image;
        document.getElementById('fpTitle').innerText = title;
        document.getElementById('fpArtist').innerText = '';
        if (image) document.getElementById('fpArtwork').src = image;
      } catch (e) {}
      isPlaying = true;
      try {
        const pauseSvg = '<path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>';
        document.getElementById('miniPlayIcon').innerHTML = pauseSvg;
        document.getElementById('fpPlayIcon').innerHTML = pauseSvg;
        document.getElementById('headerPlayIcon').innerHTML = pauseSvg;
      } catch (e) {}
    }

    function skipTrack(dir) {
      showToast(dir > 0 ? 'Playing next track' : 'Playing previous track');
    }

    function toggleLike(e) {
      if (e) e.stopPropagation();
      isLiked = !isLiked;
      const color = isLiked ? 'var(--sp-green)' : 'var(--sp-text-secondary)';
      document.querySelector('#miniLikeBtn svg').setAttribute('fill', color);
      document.querySelector('#fpHeartIcon').setAttribute('fill', color);
      document.getElementById('menuLikeText').innerText = isLiked ? 'Remove from Liked Songs' : 'Add to Liked Songs';
    }

    function updateScrubberTime(val) {
      const mins = Math.floor(val / 60);
      const secs = String(val % 60).padStart(2, '0');
      document.getElementById('fpCurrentTime').innerText = `${mins}:${secs}`;
      document.getElementById('miniProgressFill').style.width = `${(val / 89) * 100}%`;
      const scrub = document.getElementById('fpScrubber');
      if (scrub) {
        const pct = (val / 89) * 100;
        scrub.style.setProperty('--fp-progress', pct + '%');
        if (scrub.value !== String(val)) scrub.value = val;
      }
    }

    // ---- bridge-driven initial sync (ported addition) ----
    // The authored file hardcoded the player DOM to match its demo track.
    // Sync the DOM from the bridge-seeded state on load so window.__bridge
    // actually drives the UI; falls back to authored values when no bridge.
    (function syncInitialPlayer() {
      try {
        document.getElementById('miniTitle').innerText = activeTrack.title;
        document.getElementById('miniArtist').innerText = activeTrack.artist;
        document.getElementById('miniThumb').src = activeTrack.thumb;
        document.getElementById('fpTitle').innerText = activeTrack.title;
        document.getElementById('fpArtist').innerText = activeTrack.artist;
        document.getElementById('fpArtwork').src = activeTrack.thumb;
        document.getElementById('fullPlayer').style.background =
          `linear-gradient(180deg, ${resolveAccent(activeTrack.colorToken)} 0%, var(--sp-black) 60%, var(--sp-black) 100%)`;
        const pauseSvg = '<path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>';
        const playSvg = '<path d="M8 5v14l11-7z"/>';
        const icon = isPlaying ? pauseSvg : playSvg;
        document.getElementById('miniPlayIcon').innerHTML = icon;
        document.getElementById('fpPlayIcon').innerHTML = icon;
        document.getElementById('headerPlayIcon').innerHTML = icon;
        const likeColor = isLiked ? 'var(--sp-green)' : 'var(--sp-text-secondary)';
        document.querySelector('#miniLikeBtn svg').setAttribute('fill', likeColor);
        document.querySelector('#fpHeartIcon').setAttribute('fill', likeColor);
        const menuLike = document.getElementById('menuLikeText');
        if (menuLike) menuLike.innerText = isLiked ? 'Remove from Liked Songs' : 'Add to Liked Songs';
        const scrub = document.getElementById('fpScrubber');
        if (scrub) updateScrubberTime(scrub.value || 83);
      } catch (e) { /* non-fatal: leave authored DOM as-is */ }
    })();

    // ---- long-press diagnostic on the header avatar ----
    // Hold the avatar 800ms to see the last 10 logged taps, whether
    // switchTab exists, and the last caught error. For device debugging.
    (function bindAvatarDiagnostic() {
      function showDiag() {
        try {
          var taps = (window.__splTapLog || []).slice(-10).map(function(x) {
            var d = new Date(x.t);
            var hh = String(d.getHours()).padStart(2, '0');
            var mm = String(d.getMinutes()).padStart(2, '0');
            var ss = String(d.getSeconds()).padStart(2, '0');
            return hh + ':' + mm + ':' + ss + ' ' + x.what;
          }).join('\n');
          alert('TAPS (last 10):\n' + (taps || '(none)') +
                '\n\ntypeof switchTab: ' + (typeof window.switchTab) +
                '\nlast error: ' + (window.__splLastError || '(none)'));
        } catch (e) {}
      }
      function attach() {
        var av = document.querySelector('#mainHeader .user-avatar');
        if (!av || av.__splDiagBound) return false;
        av.__splDiagBound = true;
        var timer = null;
        function start() {
          cancel();
          timer = setTimeout(function() { timer = null; showDiag(); }, 800);
        }
        function cancel() {
          if (timer) { clearTimeout(timer); timer = null; }
        }
        av.addEventListener('touchstart', start, { passive: true });
        av.addEventListener('touchend', cancel);
        av.addEventListener('touchcancel', cancel);
        av.addEventListener('mousedown', start);
        av.addEventListener('mouseup', cancel);
        av.addEventListener('mouseleave', cancel);
        return true;
      }
      if (!attach()) {
        var iv = setInterval(function() { if (attach()) clearInterval(iv); }, 500);
        setTimeout(function() { clearInterval(iv); }, 10000);
      }
    })();
