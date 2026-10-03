// Seeded from window.__bridge (mock in the VM, real seams in the app).
    // Falls back to the authored demo values when the bridge is absent.
    const __b = (typeof window !== 'undefined' && window.__bridge) || {};
    const __bt = __b.track || {};
    let isPlaying = typeof __b.playing === 'boolean' ? __b.playing : true;
    let isLiked = typeof __b.liked === 'boolean' ? __b.liked : true;
    let activeTrack = {
      title: __bt.title || 'CONGASO - Totally Slowed',
      artist: __bt.artist || 'ASTXR, Bumishen\'ka',
      thumb: __bt.art || 'art/1509198397868-475647b2a1e5.jpg',
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
    }

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
    function playSong(title, artist, thumb, accentColor) {
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
