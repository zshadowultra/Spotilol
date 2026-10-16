(() => {
  "use strict";

  /* ---------------- helpers ---------------- */

  const $ = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
  const can = (fn) => typeof fn === "function";
  const esc = (s) =>
    String(s).replace(/[&<>"']/g, (c) => ({
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#39;",
    }[c]));

  const fmt = (ms) => {
    const s = Math.max(0, Math.round(ms / 1000));
    return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;
  };

  /* ---------------- bridge ---------------- */

  let B = {};
  let hasBridge = false;

  /* ---------------- demo data (fallback when no bridge) ---------------- */

  const DEMO_TRACKS = [
    {
      title: "Neon Cathedral",
      artist: "The Glass Arcs",
      durationMs: 254000,
      artwork: "#0d3b3f",
    },
    {
      title: "Paper Lanterns",
      artist: "Ivy Sone",
      durationMs: 197000,
      artwork: "#43203a",
    },
    {
      title: "Slow Motion Ocean",
      artist: "Halcyon Drift",
      durationMs: 312000,
      artwork: "#0a2c3f",
    },
  ];

  const RECS = [
    {
      title: "Midnight Drive",
      sub: "Neon-lit synthwave for empty highways",
      tag: "Mix",
      track: 0,
      art: "#1b1145",
    },
    {
      title: "Slow Bloom",
      sub: "Acoustic mornings, warm and unhurried",
      tag: "For you",
      track: 1,
      art: "#42203a",
    },
  ];

  const DEMO_LIBRARY = [
    {
      type: "playlist",
      name: "Midnight Drive",
      meta: "Playlist · 24 tracks",
      uri: "cadence:playlist:midnight-drive",
      art: "#1b1145",
    },
    {
      type: "playlist",
      name: "Slow Bloom",
      meta: "Playlist · 18 tracks",
      uri: "cadence:playlist:slow-bloom",
      art: "#42203a",
    },
    {
      type: "playlist",
      name: "Deep Focus",
      meta: "Playlist · 42 tracks",
      uri: "cadence:playlist:deep-focus",
      art: "#0d2b26",
    },
    {
      type: "album",
      name: "Neon Cathedral",
      meta: "Album · The Glass Arcs · 2024",
      uri: "cadence:album:neon-cathedral",
      art: "#0d3b3f",
    },
    {
      type: "album",
      name: "Paper Lanterns",
      meta: "Album · Ivy Sone · 2023",
      uri: "cadence:album:paper-lanterns",
      art: "#43203a",
    },
    {
      type: "album",
      name: "Slow Motion Ocean",
      meta: "Album · Halcyon Drift · 2022",
      uri: "cadence:album:slow-motion-ocean",
      art: "#0a2c3f",
    },
    {
      type: "artist",
      name: "The Glass Arcs",
      meta: "Artist · 1.2M followers",
      uri: "cadence:artist:the-glass-arcs",
      art: "#1e1b3a",
    },
    {
      type: "artist",
      name: "Ivy Sone",
      meta: "Artist · 864K followers",
      uri: "cadence:artist:ivy-sone",
      art: "#3a1b2c",
    },
  ];

  const LIB_FILTERS = [
    { id: "playlists", label: "Playlists" },
    { id: "albums", label: "Albums" },
    { id: "artists", label: "Artists" },
  ];

  const RECENT_SEARCHES = [
    "Neon Cathedral",
    "Ivy Sone",
    "Halcyon Drift",
    "Synthwave mix",
  ];

  const ICONS = {
    play:
      '<svg viewBox="0 0 24 24" width="25" height="25" fill="currentColor" aria-hidden="true"><path d="M8 5.3v13.4L19 12z"/></svg>',
    pause:
      '<svg viewBox="0 0 24 24" width="25" height="25" fill="currentColor" aria-hidden="true"><rect x="6.5" y="5" width="3.6" height="14" rx="1.6"/><rect x="13.9" y="5" width="3.6" height="14" rx="1.6"/></svg>',
    heart:
      '<svg viewBox="0 0 24 24" width="21" height="21" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linejoin="round" aria-hidden="true"><path d="M12 20.5S3.6 15.7 3.6 9.9C3.6 7 5.7 4.9 8.3 4.9c1.6 0 3 .8 3.7 2 .7-1.2 2.1-2 3.7-2 2.6 0 4.7 2.1 4.7 5 0 5.8-8.4 10.6-8.4 10.6z"/></svg>',
    heartOn:
      '<svg viewBox="0 0 24 24" width="21" height="21" fill="currentColor" aria-hidden="true"><path d="M12 20.5S3.6 15.7 3.6 9.9C3.6 7 5.7 4.9 8.3 4.9c1.6 0 3 .8 3.7 2 .7-1.2 2.1-2 3.7-2 2.6 0 4.7 2.1 4.7 5 0 5.8-8.4 10.6-8.4 10.6z"/></svg>',
    chevron:
      '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m9 5.5 6.5 6.5L9 18.5"/></svg>',
    search:
      '<svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true"><circle cx="11" cy="11" r="6.5"/><path d="m20.2 20.2-4.6-4.6"/></svg>',
    close:
      '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" aria-hidden="true"><path d="M6 6l12 12M18 6 6 18"/></svg>',
  };

  /* ---------------- state ---------------- */

  const state = {
    view: "home",
    demoIndex: 0,
    demoPlaying: false,
    demoPosition: 0,
    liked: false,
    libFilter: "playlists",
    query: "",
    npOpen: false,
    shuffle: false,
    repeat: false,
  };

  /* now-playing drag-to-dismiss + seek-drag scratch state */
  let npDragY = null;
  let npDragDy = 0;
  let npDragT = 0;
  let seekDrag = false;

  const els = {};

  /* ---------------- adapters (bridge → local) ---------------- */

  function track() {
    const t = (hasBridge && B.track) || DEMO_TRACKS[state.demoIndex];
    /* bridge exposes `art`; this UI reads `artwork` — normalize at the seam */
    return t && t.artwork == null && t.art != null ? { ...t, artwork: t.art } : t;
  }

  function playing() {
    return hasBridge ? !!B.playing : state.demoPlaying;
  }

  function positionMs() {
    return hasBridge ? B.positionMs || 0 : state.demoPosition;
  }

  function durationMs() {
    const d = track().durationMs;
    return typeof d === "number" && d > 0 ? d : 240000;
  }

  function artStyle(art) {
    if (!art) return "";
    const s = String(art).replace(/"/g, "%22");
    const isUrl = /^(https?:)?\/\//.test(s) || s.startsWith("/") || s.startsWith("data:");
    return isUrl
      ? `background-image: url("${s}"); background-size: cover; background-position: center;`
      : `background: ${s};`;
  }

  /* ---------------- actions ---------------- */

  function setDemoTrack(i, autoPlay) {
    const n = DEMO_TRACKS.length;
    state.demoIndex = ((i % n) + n) % n;
    state.demoPosition = 0;
    if (autoPlay) state.demoPlaying = true;
    render();
  }

  const actions = {
    toggle() {
      if (can(B.toggle)) B.toggle();
      else state.demoPlaying = !state.demoPlaying;
      render();
    },
    next() {
      if (can(B.next)) B.next();
      else setDemoTrack(state.demoIndex + 1, state.demoPlaying);
    },
    prev() {
      if (can(B.prev)) B.prev();
      else setDemoTrack(state.demoIndex - 1, state.demoPlaying);
    },
    seek(ms) {
      const target = Math.min(Math.max(0, ms), durationMs());
      if (can(B.seek)) B.seek(target);
      else state.demoPosition = target;
      renderProgress();
    },
    like() {
      if (can(B.like)) B.like();
      else state.liked = !state.liked;
      renderControls();
    },
  };

  /* ---------------- render ---------------- */

  function greeting() {
    const h = new Date().getHours();
    if (h < 5) return "Still up";
    if (h < 12) return "Good morning";
    if (h < 18) return "Good afternoon";
    return "Good evening";
  }

  function renderHome() {
    els.home.innerHTML = `
      <div class="page">
        <header class="greeting">
          <p class="greeting__eyebrow">Cadence</p>
          <h1 class="greeting__title">${greeting()}</h1>
        </header>

        <section class="recs" aria-label="Recommended">
          ${RECS.map(
            (r, i) => `
            <button class="rec" data-track="${r.track}" style="animation-delay:${i * 90}ms">
              <span class="rec__art" style="${artStyle(r.art)}">
                <span class="rec__tag">${esc(r.tag)}</span>
                <span class="eq" aria-hidden="true"><i></i><i></i><i></i></span>
              </span>
              <span class="rec__title">${esc(r.title)}</span>
              <span class="rec__sub">${esc(r.sub)}</span>
            </button>`
          ).join("")}
        </section>

        <section class="recent" aria-label="Recently played">
          <div class="recent__head">
            <h2>Recently played</h2>
            <span class="recent__count">${DEMO_TRACKS.length} tracks</span>
          </div>
          <div class="recent__row">
            ${DEMO_TRACKS.map(
              (t, i) => `
              <button class="recent__item" data-track="${i}" style="animation-delay:${140 + i * 70}ms">
                <span class="recent__art" style="${artStyle(t.artwork)}">
                  <span class="eq" aria-hidden="true"><i></i><i></i><i></i></span>
                </span>
                <span class="recent__title">${esc(t.title)}</span>
                <span class="recent__artist">${esc(t.artist)}</span>
              </button>`
            ).join("")}
          </div>
        </section>
      </div>`;
  }

  function renderHomeCurrent() {
    const demoActive = !hasBridge && playing();
    $$("#view-home [data-track], #view-search [data-track]").forEach((el) => {
      el.classList.toggle("is-current", demoActive && Number(el.dataset.track) === state.demoIndex);
    });
  }

  /* ---------------- library + search (rows) ---------------- */

  function playFromUri(uri) {
    if (!uri || !can(window.playFromUri)) return; /* demo fallback: no-op */
    window.playFromUri(uri);
  }

  function trackRow(item, i) {
    return {
      name: item.title,
      meta: item.artist,
      uri: `cadence:track:${i}`,
      track: i,
      art: item.artwork,
    };
  }

  function rowHTML(item, i) {
    const current =
      item.track != null && !hasBridge && playing() &&
      Number(item.track) === state.demoIndex;
    return `
      <button class="row${item.type === "artist" ? " row--artist" : ""}${current ? " is-current" : ""}"
        data-uri="${esc(item.uri || "")}"${item.track != null ? ` data-track="${item.track}"` : ""}
        style="animation-delay:${i * 55}ms">
        <span class="row__art" style="${artStyle(item.art)}"></span>
        <span class="row__text">
          <span class="row__name">${esc(item.name)}</span>
          <span class="row__meta">${esc(item.meta)}</span>
        </span>
        <span class="row__chev">${ICONS.chevron}</span>
      </button>`;
  }

  function renderLibrary() {
    const singular = state.libFilter.replace(/s$/, "");
    const items = DEMO_LIBRARY.filter((it) => it.type === singular);
    els.library.innerHTML = `
      <div class="page">
        <header class="view-head">
          <h1>Your Library</h1>
        </header>
        <div class="chips" role="tablist" aria-label="Library filter">
          ${LIB_FILTERS.map(
            (f) => `
            <button class="chip${state.libFilter === f.id ? " active" : ""}" role="tab"
              aria-selected="${state.libFilter === f.id}" data-filter="${f.id}">${f.label}</button>`
          ).join("")}
        </div>
        <div class="rows">
          ${items.map((it, i) => rowHTML(it, i)).join("")}
        </div>
      </div>`;
  }

  function searchMatches() {
    const rows = DEMO_TRACKS.map(trackRow);
    const q = state.query.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter(
      (r) => r.name.toLowerCase().includes(q) || r.meta.toLowerCase().includes(q)
    );
  }

  function renderSearchResults() {
    const q = state.query.trim();
    const matches = searchMatches();
    const recent = $("#search-recent", els.search);
    if (recent) recent.hidden = q !== "";
    $("#search-results-head", els.search).innerHTML = `
      <h2>${q ? "Results" : "Browse all"}</h2>
      <span>${matches.length} ${matches.length === 1 ? "item" : "items"}</span>`;
    $("#search-results", els.search).innerHTML = matches.length
      ? matches.map((m, i) => rowHTML(m, i)).join("")
      : `<p class="search-empty">No results for “${esc(state.query)}”.<br/>Try a different spelling or pick a recent search.</p>`;
    els.searchField.classList.toggle("has-query", q !== "");
  }

  function renderSearch() {
    els.search.innerHTML = `
      <div class="page">
        <header class="view-head">
          <h1>Search</h1>
        </header>
        <div class="search-field" id="search-field">
          ${ICONS.search}
          <input id="search-input" type="search" placeholder="Songs, artists, playlists"
            value="${esc(state.query)}" autocomplete="off" autocapitalize="off"
            spellcheck="false" enterkeyhint="search" aria-label="Search" />
          <button class="search-clear" id="search-clear" aria-label="Clear search">${ICONS.close}</button>
        </div>
        <div class="search-recent" id="search-recent">
          <div class="search-results-head">
            <h2>Recent searches</h2>
          </div>
          <div class="chips">
            ${RECENT_SEARCHES.map(
              (s, i) => `
              <button class="chip chip--sm" data-recent="${esc(s)}" style="animation-delay:${i * 50}ms">${esc(s)}</button>`
            ).join("")}
          </div>
        </div>
        <div class="search-results-head" id="search-results-head"></div>
        <div id="search-results"></div>
      </div>`;
    els.searchField = $("#search-field", els.search);
    renderSearchResults();
  }

  function renderMini() {
    const t = track();
    els.mpArt.setAttribute("style", artStyle(t.artwork));
    els.mpTitle.textContent = t.title || "Nothing playing";
    els.mpArtist.textContent = t.artist || "";
    renderControls();
  }

  function renderControls() {
    els.mpToggle.innerHTML = playing() ? ICONS.pause : ICONS.play;
    els.mpToggle.setAttribute("aria-label", playing() ? "Pause" : "Play");
    els.mpLike.innerHTML = state.liked ? ICONS.heartOn : ICONS.heart;
    els.mpLike.classList.toggle("is-liked", state.liked);
    els.npToggle.innerHTML = playing() ? ICONS.pause : ICONS.play;
    els.npToggle.setAttribute("aria-label", playing() ? "Pause" : "Play");
    els.npLike.innerHTML = state.liked ? ICONS.heartOn : ICONS.heart;
    els.npLike.classList.toggle("is-liked", state.liked);
    els.npShuffle.classList.toggle("is-on", state.shuffle);
    els.npShuffle.setAttribute("aria-pressed", String(state.shuffle));
    els.npRepeat.classList.toggle("is-on", state.repeat);
    els.npRepeat.setAttribute("aria-pressed", String(state.repeat));
  }

  function renderNow() {
    const t = track();
    const a = artStyle(t.artwork);
    els.npArt.setAttribute("style", a);
    els.npBg.setAttribute("style", a);
    els.npTitle.textContent = t.title || "Nothing playing";
    els.npArtist.textContent = t.artist || "";
    els.npDur.textContent = fmt(durationMs());
  }

  function paintSeek(ms) {
    const pct = Math.min(100, (ms / durationMs()) * 100);
    els.npFill.style.width = pct + "%";
    els.npPos.textContent = fmt(ms);
    els.npSeek.setAttribute("aria-valuenow", Math.round(pct));
  }

  function renderProgress() {
    const pct = Math.min(100, (positionMs() / durationMs()) * 100);
    els.mpFill.style.width = pct + "%";
    els.mpProgress.setAttribute("aria-valuenow", Math.round(pct));
    if (!seekDrag) {
      els.npFill.style.width = pct + "%";
      els.npPos.textContent = fmt(positionMs());
      els.npSeek.setAttribute("aria-valuenow", Math.round(pct));
    }
  }

  function render() {
    renderMini();
    renderNow();
    renderProgress();
    renderHomeCurrent();
  }

  /* ---------------- view switching ---------------- */

  function switchView(name) {
    state.view = name;
    $$(".view").forEach((v) => {
      const on = v.dataset.view === name;
      v.classList.toggle("active", on);
      if (on) v.scrollTop = 0;
    });
    $$(".nav-tab").forEach((t) => {
      const on = t.dataset.view === name;
      t.classList.toggle("active", on);
      if (on) t.setAttribute("aria-current", "page");
      else t.removeAttribute("aria-current");
    });
  }

  /* ---------------- now playing (full-screen sheet) ---------------- */

  function openNP() {
    if (state.npOpen) return;
    state.npOpen = true;
    renderNow();
    els.np.classList.add("open");
  }

  function closeNP() {
    if (!state.npOpen) return;
    state.npOpen = false;
    els.np.classList.remove("open");
    els.np.style.transform = "";
  }

  function endNpDrag() {
    if (npDragY == null) return;
    const flick = performance.now() - npDragT < 300 && npDragDy > 48;
    const far = npDragDy > 140;
    npDragY = null;
    npDragDy = 0;
    els.np.classList.remove("is-dragging");
    els.np.style.transform = "";
    if (far || flick) closeNP();
  }

  /* ---------------- init ---------------- */

  function init() {
    els.home = $("#view-home");
    els.library = $("#view-library");
    els.search = $("#view-search");
    els.mpArt = $("#mp-art");
    els.mpTitle = $("#mp-title");
    els.mpArtist = $("#mp-artist");
    els.mpToggle = $("#mp-toggle");
    els.mpLike = $("#mp-like");
    els.mpFill = $("#mp-fill");
    els.mpProgress = $("#mp-progress");
    els.np = $("#view-nowplaying");
    els.npArt = $("#np-art");
    els.npBg = $("#np-bg");
    els.npTitle = $("#np-title");
    els.npArtist = $("#np-artist");
    els.npToggle = $("#np-toggle");
    els.npLike = $("#np-like");
    els.npFill = $("#np-fill");
    els.npSeek = $("#np-seek");
    els.npPos = $("#np-pos");
    els.npDur = $("#np-dur");
    els.npShuffle = $("#np-shuffle");
    els.npRepeat = $("#np-repeat");

    renderHome();
    renderLibrary();
    renderSearch();
    switchView("home");
    render();

    $("#nav").addEventListener("click", (e) => {
      const tab = e.target.closest(".nav-tab");
      if (!tab) return;
      if (tab.dataset.view === "player") {
        openNP();
        return;
      }
      switchView(tab.dataset.view);
    });

    els.home.addEventListener("click", (e) => {
      const item = e.target.closest("[data-track]");
      if (!item) return;
      if (hasBridge) {
        actions.toggle();
        return;
      }
      setDemoTrack(Number(item.dataset.track), true);
    });

    /* library: filter chips + row taps → playFromUri */
    els.library.addEventListener("click", (e) => {
      const chip = e.target.closest("[data-filter]");
      if (chip && chip.dataset.filter !== state.libFilter) {
        state.libFilter = chip.dataset.filter;
        renderLibrary();
        return;
      }
      const row = e.target.closest("[data-uri]");
      if (row) playFromUri(row.dataset.uri);
    });

    /* search: input filters demo data; recents/clear/rows via delegation */
    els.search.addEventListener("input", (e) => {
      if (e.target.id !== "search-input") return;
      state.query = e.target.value;
      renderSearchResults();
    });

    els.search.addEventListener("click", (e) => {
      const recent = e.target.closest("[data-recent]");
      if (recent) {
        state.query = recent.dataset.recent;
        const input = $("#search-input", els.search);
        input.value = state.query;
        renderSearchResults();
        input.focus();
        return;
      }
      if (e.target.closest("#search-clear")) {
        state.query = "";
        const input = $("#search-input", els.search);
        input.value = "";
        renderSearchResults();
        input.focus();
        return;
      }
      const row = e.target.closest("[data-uri]");
      if (row) playFromUri(row.dataset.uri);
    });

    $("#mp-toggle").addEventListener("click", () => actions.toggle());
    $("#mp-next").addEventListener("click", () => actions.next());
    $("#mp-prev").addEventListener("click", () => actions.prev());
    $("#mp-like").addEventListener("click", () => actions.like());
    $("#mp-body").addEventListener("click", () => openNP());

    /* now playing: transport + like + shuffle/repeat (bridge if present, else demo) */
    $("#np-close").addEventListener("click", closeNP);
    $("#np-toggle").addEventListener("click", () => actions.toggle());
    $("#np-next").addEventListener("click", () => actions.next());
    $("#np-prev").addEventListener("click", () => actions.prev());
    $("#np-like").addEventListener("click", () => actions.like());
    $("#np-shuffle").addEventListener("click", () => {
      state.shuffle = !state.shuffle;
      if (can(B.shuffle)) B.shuffle(state.shuffle);
      renderControls();
    });
    $("#np-repeat").addEventListener("click", () => {
      state.repeat = !state.repeat;
      if (can(B.repeat)) B.repeat(state.repeat);
      renderControls();
    });

    /* now playing: draggable seek bar (pointer press-drag-release) */
    const seekPct = (e) => {
      const r = els.npSeek.getBoundingClientRect();
      return Math.min(1, Math.max(0, (e.clientX - r.left) / r.width));
    };
    els.npSeek.addEventListener("pointerdown", (e) => {
      seekDrag = true;
      els.npSeek.classList.add("is-drag");
      try {
        els.npSeek.setPointerCapture(e.pointerId);
      } catch {}
      paintSeek(seekPct(e) * durationMs());
      e.preventDefault();
    });
    els.npSeek.addEventListener("pointermove", (e) => {
      if (seekDrag) paintSeek(seekPct(e) * durationMs());
    });
    const endSeek = (e) => {
      if (!seekDrag) return;
      seekDrag = false;
      els.npSeek.classList.remove("is-drag");
      actions.seek(seekPct(e) * durationMs());
    };
    els.npSeek.addEventListener("pointerup", endSeek);
    els.npSeek.addEventListener("pointercancel", endSeek);

    /* now playing: drag anywhere on art/background to dismiss */
    els.np.addEventListener("pointerdown", (e) => {
      if (e.target.closest("button, .np__seek")) return;
      npDragY = e.clientY;
      npDragDy = 0;
      npDragT = performance.now();
      els.np.classList.add("is-dragging");
      try {
        els.np.setPointerCapture(e.pointerId);
      } catch {}
    });
    els.np.addEventListener("pointermove", (e) => {
      if (npDragY == null) return;
      npDragDy = Math.max(0, e.clientY - npDragY);
      if (npDragDy > 0) els.np.style.transform = `translateY(${npDragDy}px)`;
    });
    els.np.addEventListener("pointerup", endNpDrag);
    els.np.addEventListener("pointercancel", endNpDrag);

    els.mpProgress.addEventListener("click", (e) => {
      const rect = els.mpProgress.getBoundingClientRect();
      actions.seek(Math.round(((e.clientX - rect.left) / rect.width) * durationMs()));
    });

    document.addEventListener("keydown", (e) => {
      if (e.target.closest("input, textarea, [contenteditable]")) return;
      if (e.code === "Escape") {
        if (state.npOpen) closeNP();
        return;
      }
      if (e.code === "Space") {
        e.preventDefault();
        actions.toggle();
      } else if (e.code === "ArrowRight") {
        e.preventDefault();
        actions.seek(positionMs() + 5000);
      } else if (e.code === "ArrowLeft") {
        e.preventDefault();
        actions.seek(positionMs() - 5000);
      }
    });

    /* host events */
    if (can(B.on)) B.on("change", () => render());

    /* local tick: demo playback clock + progress sync */
    let lastSyncKey = "";
    setInterval(() => {
      /* host bridge on() is a stub — poll track/playing so the UI stays live */
      if (hasBridge) {
        const t = track();
        const key = `${t.title}|${t.artist}|${t.artwork || ""}|${playing()}`;
        if (key !== lastSyncKey) {
          if (lastSyncKey !== "") render();
          lastSyncKey = key;
        }
      }
      if (!hasBridge && state.demoPlaying) {
        state.demoPosition += 500;
        if (state.demoPosition >= durationMs()) {
          setDemoTrack(state.demoIndex + 1, true);
          return;
        }
        renderHomeCurrent();
      }
      renderProgress();
    }, 500);
  }

  // Boot on element-readiness, not document-readiness.
  // CustomUI.CONTENT creates #spl-custom-ui-root ~200ms after this script
  // runs (it waits for AndBridge). init() must wait for #view-home to exist.
  function boot() {
    if (window.__cadenceBooted) return;
    window.__cadenceBooted = true;
    B = window.__bridge || {};
    hasBridge = !!window.__bridge;
    init();
  }
  const tryBoot = () => {
    if ($("#view-home")) {
      clearInterval(bootIv);
      boot();
    }
  };
  let bootIv = setInterval(tryBoot, 100);
  tryBoot();
})();
