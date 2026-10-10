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

    // ---- resilient tap binding ----
    // Some Android WebViews / host pages suppress click synthesis (e.g. a
    // third-party touchstart preventDefault kills the click). Bind touchend
    // AND click with dedup so a tap registers via whichever path survives.
    function bindTap(el, fn) {
      if (!el || el.__splTapBound) return;
      el.__splTapBound = true;
      var lastTouch = 0;
      el.addEventListener('touchend', function(e) {
        lastTouch = Date.now();
        try { fn.call(el, e); } catch (err) {
          window.__splLastError = String((err && err.message) || err);
        }
      }, { passive: true });
      el.addEventListener('click', function(e) {
        if (Date.now() - lastTouch < 700) return; // touchend already handled it
        try { fn.call(el, e); } catch (err) {
          window.__splLastError = String((err && err.message) || err);
        }
      });
    }
    window.bindTap = bindTap;

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

        // Clear the song list and show skeleton rows (D5: exact .song-row geometry)
        var list = section.querySelector('.song-list');
        if (window.__splDetailIO) { try { window.__splDetailIO.disconnect(); } catch (e) {} window.__splDetailIO = null; }
        if (window.__splDetailAbort) { try { window.__splDetailAbort.abort(); } catch (e) {} window.__splDetailAbort = null; }
        if (window.__splUx) { try { window.__splUx.prefetch.cancelAll(); } catch (e) {} }
        if (list) {
          list.innerHTML = '';
          for (var ski = 0; ski < 8; ski++) list.appendChild(skelSongRow());
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

        // C6: infinite scroll via IntersectionObserver (no scroll handlers).
        // The sentinel sits after the list; rootMargin prefetches before the end.
        function setupDetailPager(nextUrl) {
          if (!nextUrl || !window.IntersectionObserver || !list) return;
          var sentinel = document.createElement('div');
          sentinel.style.cssText = 'height:1px;';
          list.appendChild(sentinel);
          var loading = false;
          var io = new IntersectionObserver(function(entries) {
            if (!entries[0].isIntersecting || loading) return;
            loading = true;
            var u = nextUrl; nextUrl = null;
            fetch(u, { headers: { 'Authorization': window.spotAuthToken } })
              .then(function(r) { if (!r.ok) throw new Error('http ' + r.status); return r.json(); })
              .then(function(data) {
                var more = [];
                (data.items || []).forEach(function(it) { var t = it.track || it; if (t && t.uri) more.push(t); });
                nextUrl = (data && data.next) || null;
                loading = false;
                if (!more.length) { io.disconnect(); if (sentinel.parentNode) sentinel.parentNode.removeChild(sentinel); return; }
                if (window.__splPlaylistVirt) window.__splPlaylistVirt.appendItems(more);
                else {
                  more.forEach(function(t) {
                    var row = document.createElement('div');
                    bindTrackRow(row, t);
                    window.bindTap(row, function() {
                      if (typeof window.playFromUri === 'function') { try { window.playFromUri(t.uri, uri); } catch (e) {} }
                    });
                    list.insertBefore(row, sentinel);
                  });
                }
                if (!nextUrl) { io.disconnect(); if (sentinel.parentNode) sentinel.parentNode.removeChild(sentinel); }
              })
              .catch(function() { loading = false; });
          }, { root: document.getElementById('mainScrollArea'), rootMargin: '400px' });
          io.observe(sentinel);
          window.__splDetailIO = io;
        }
        function tracksFrom(data) {
          var tracks = [];
          (data.items || []).forEach(function(it) {
            var t = it.track || it;
            if (t && t.uri) tracks.push(t);
          });
          return tracks;
        }
        function renderDetail(tracks, nextUrl) {
          try {
            if (statsEl) statsEl.innerText = tracks.length + (tracks.length === 1 ? ' song' : ' songs');
            renderPlaylistTracks(list, tracks, uri);
            setupDetailPager(nextUrl);
          } catch (e) {
            window.__splLastError = String((e && e.message) || e);
          }
        }
        function failDetail(e) {
          window.__splLastError = String((e && e.message) || e);
          if (list) list.innerHTML = '';
          showToast("Couldn't load tracks");
        }
        var detailAbort = null;
        try { detailAbort = new AbortController(); window.__splDetailAbort = detailAbort; } catch (e) {}
        // D3: serve from the dwell-prefetch LRU on a hit (0 ms network).
        if (window.__splUx && window.__splUx.prefetch.cache.has(uri)) {
          var rec = window.__splUx.prefetch.cache.get(uri);
          renderDetail(rec.tracks, rec.next);
        } else {
          var fetchOpts = { headers: { 'Authorization': window.spotAuthToken } };
          if (detailAbort) fetchOpts.signal = detailAbort.signal;
          fetch(url, fetchOpts)
            .then(function(r) { if (!r.ok) throw new Error('http ' + r.status); return r.json(); })
            .then(function(data) {
              var rec2 = { tracks: tracksFrom(data), next: (data && data.next) || null, ts: Date.now() };
              try { if (window.__splUx) window.__splUx.prefetch.cache.set(uri, rec2); } catch (e) {}
              renderDetail(rec2.tracks, rec2.next);
            })
            .catch(function(e) {
              if (e && e.name === 'AbortError') return;
              failDetail(e);
            });
        }
      } catch (e) {
        window.__splLastError = String((e && e.message) || e);
        try { showToast('Error: ' + ((e && e.message) || e)); } catch (x) {}
      }
    }
    window.openPlaylistDetail = openPlaylistDetail;

    // Render track rows into a .song-list using the existing .song-row
    // markup pattern. Row tap plays the track with the playlist as context.
    // D5: skeleton row with the exact .song-row box geometry (same classes;
    // the .spl-skel shimmer is a ::after overlay that never changes layout).
    function skelSongRow() {
      var row = document.createElement('div');
      row.className = 'song-row';
      row.style.pointerEvents = 'none';
      var a = document.createElement('div');
      a.className = 'song-art spl-skel';
      row.appendChild(a);
      var info = document.createElement('div');
      info.className = 'song-info';
      var nm = document.createElement('div');
      nm.className = 'song-name spl-skel';
      nm.innerText = '\u00a0\u00a0\u00a0\u00a0\u00a0\u00a0';
      info.appendChild(nm);
      var sb = document.createElement('div');
      sb.className = 'song-subtitle spl-skel';
      sb.innerText = '\u00a0\u00a0\u00a0';
      info.appendChild(sb);
      row.appendChild(info);
      return row;
    }

    // Binds one recycled row node to a track (C7: must fully rebind — the
    // virtualizer reuses these nodes while scrolling).
    function bindTrackRow(row, t) {
      row.className = 'song-row';
      row.innerHTML = '';
      var art = document.createElement('img');
      art.className = 'song-art';
      var imgs = (t.album && t.album.images) || [];
      var imgUrl = imgs.length ? imgs[imgs.length - 1].url : '';
      if (imgUrl) {
        if (window.__splUx) window.__splUx.artCache.loadInto(art, imgUrl);
        else { art.setAttribute('loading', 'lazy'); art.setAttribute('decoding', 'async'); art.src = imgUrl; }
      }
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
    }
    // Tap wiring that survives node recycling: bindTap runs once per pooled
    // node (its __splTapBound guard ignores re-binds); the per-item action is
    // swapped via __splTapFn on every recycle. Click behavior is unchanged.
    function wireTrackTap(el, t, contextUri) {
      if (!el.__splTapWired) {
        el.__splTapWired = true;
        window.bindTap(el, function() { var f = el.__splTapFn; if (f) { try { f(); } catch (e) {} } });
      }
      el.__splTapFn = function() {
        if (typeof window.playFromUri === 'function') {
          try { window.playFromUri(t.uri, contextUri); } catch (e) {}
        }
      };
    }

    // Render track rows into a .song-list using the existing .song-row
    // markup pattern. Row tap plays the track with the playlist as context.
    // C7: lists longer than 40 rows are virtualized (viewport + overscan only).
    function renderPlaylistTracks(list, tracks, contextUri) {
      if (!list) return;
      if (window.__splPlaylistVirt) { try { window.__splPlaylistVirt.destroy(); } catch (e) {} window.__splPlaylistVirt = null; }
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
      if (window.__splUx && tracks.length > 40) {
        var scroller = document.getElementById('mainScrollArea') || list;
        window.__splPlaylistVirt = window.__splUx.virtualize(list, scroller, tracks.slice(), function(el, t) {
          bindTrackRow(el, t);
          wireTrackTap(el, t, contextUri);
        }, { rowH: 0 });
        return;
      }
      tracks.forEach(function(t) {
        var row = document.createElement('div');
        bindTrackRow(row, t);
        window.bindTap(row, function() {
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

    // ---- rebind nav tabs through bindTap ----
    // Replaces inline onclick with touchend+click listeners so tab switches
    // survive WebViews that suppress click synthesis.
    (function rebindNavTabs() {
      try {
        var tabs = document.querySelectorAll('.nav-tab');
        tabs.forEach(function(btn) {
          var m = (btn.getAttribute('onclick') || '').match(/switchTab\('(\w+)'/);
          if (!m) return;
          var tabName = m[1];
          btn.onclick = null;
          btn.removeAttribute('onclick');
          window.bindTap(btn, function() { window.switchTab(tabName, btn); });
        });
      } catch (e) {}
    })();
