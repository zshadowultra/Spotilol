'use strict';
// Local test runner: extracts the injected JS from the Kotlin sources,
// assembles each driver with its dependencies, and runs it under node.
// Usage: node run-local.js [driver]   (default: all)
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const ROOT = path.resolve(__dirname, '../../..'); // app/
const INJ = path.join(ROOT, 'src/main/java/com/project/lol/webview/injections');

function readKt(name) {
  return fs.readFileSync(path.join(INJ, name), 'utf8');
}
function contentJs(kt) {
  const m = kt.match(/const val CONTENT = """\n([\s\S]*)\n    """/);
  if (!m) throw new Error('CONTENT not found');
  return m[1];
}
// brace-aware extraction starting at a marker
function extractFn(src, marker) {
  const i = src.indexOf(marker);
  if (i < 0) throw new Error('marker not found: ' + marker);
  const b = src.indexOf('{', i);
  let depth = 0, inS = null, esc = false, inC = null; // inC: 'line' | 'block'
  for (let j = b; j < src.length; j++) {
    const c = src[j];
    if (inC === 'line') { if (c === '\n') inC = null; continue; }
    if (inC === 'block') { if (c === '*' && src[j + 1] === '/') { inC = null; j++; } continue; }
    if (inS) {
      if (esc) esc = false;
      else if (c === '\\') esc = true;
      else if (c === inS) inS = null;
      continue;
    }
    if (c === '/' && src[j + 1] === '/') { inC = 'line'; j++; continue; }
    if (c === '/' && src[j + 1] === '*') { inC = 'block'; j++; continue; }
    if (c === '"' || c === "'" || c === '`') { inS = c; continue; }
    if (c === '{') depth++;
    if (c === '}') { depth--; if (depth === 0) return src.slice(i, j + 1); }
  }
  throw new Error('unbalanced braces for ' + marker);
}

const pc = contentJs(readKt('PlaybackControls.kt'));
const splux = pc.slice(pc.indexOf('/*__SPLUX_START__*/'), pc.indexOf('/*__SPLUX_END__*/') + '/*__SPLUX_END__*/'.length);

const sp = contentJs(readKt('SpotilolPlayer.kt'));
const paintFn = extractFn(sp, 'window.splPaintPlayIcon=function(playing)');

const cui = contentJs(readKt('CustomUI.kt'));
const cuiFns = ['skelRecentCard', 'realRecentCard', 'skelReleaseCard', 'realReleaseCard',
  'skelSongRow', 'libSongRow', 'splArtInto', 'splDwell']
  .map(n => extractFn(cui, 'function ' + n + '(')).join('\n');

const appjs = fs.readFileSync(path.join(ROOT, 'src/main/assets/custom-ui/app.js'), 'utf8');
const appSkel = extractFn(appjs, 'function skelSongRow(').replace('function skelSongRow(', 'function appSkelSongRow(');
const appDetail = ['tryRestoreDetailSnap', 'bindDetailTap']
  .map(n => extractFn(appjs, 'function ' + n + '(')).join('\n');

// Round 2 (timing-audit): PerfMarks.kt CONTENT for the perf.js driver.
const perfMarks = contentJs(readKt('PerfMarks.kt'));

const cssM = cui.match(/sk\.textContent=((?:'[^']*'\s*\+\s*)*'[^']*');/);
if (!cssM) throw new Error('skeleton CSS not found');
const skelCss = eval(cssM[1]);

const prelude = fs.readFileSync(path.join(__dirname, 'prelude.js'), 'utf8');
const driversDir = path.join(__dirname, 'drivers');
const only = process.argv[2];
const drivers = fs.readdirSync(driversDir).filter(f => f.endsWith('.js') && (!only || f === only || f === only + '.js'));
if (!drivers.length) throw new Error('no drivers matched');

let failed = 0;
for (const d of drivers) {
  let body = fs.readFileSync(path.join(driversDir, d), 'utf8');
  // Replace only standalone-line placeholders (never inside comments).
  const sub = (token, val) => {
    const re = new RegExp('^' + token + '$', 'm');
    if (!re.test(body)) throw new Error('placeholder missing: ' + token);
    body = body.replace(re, () => val);
  };
  if (/^__SPLUX__$/m.test(body)) sub('__SPLUX__', splux);
  if (/^__PAINT__$/m.test(body)) sub('__PAINT__', paintFn);
  if (/^__CUSTOMUI_FNS__$/m.test(body)) sub('__CUSTOMUI_FNS__', cuiFns);
  if (/^__APPSKELETON__$/m.test(body)) sub('__APPSKELETON__', appSkel);
  if (/^__APPDETAIL__$/m.test(body)) sub('__APPDETAIL__', appDetail);
  if (/^__PERF__$/m.test(body)) sub('__PERF__', perfMarks);
  // __SKELCSS__ is used inline as an expression; single unique occurrence.
  body = body.split('__SKELCSS__').join(JSON.stringify(skelCss));
  const tmp = path.join('/tmp', 'uxdrv-' + d);
  fs.writeFileSync(tmp, prelude + '\n' + body);
  try {
    const out = execFileSync('node', ['--check', tmp], { stdio: 'pipe' });
  } catch (e) {
    console.error('FAIL ' + d + ': syntax error\n' + e.stderr.toString());
    failed++;
    continue;
  }
  try {
    const out = execFileSync('node', [tmp], { timeout: 15000, stdio: 'pipe' });
    process.stdout.write('[' + d + '] ' + out.toString().trim() + '\n');
  } catch (e) {
    console.error('FAIL ' + d + ':\n' + (e.stdout || '').toString() + (e.stderr || '').toString());
    failed++;
  }
}
if (failed) { console.error(failed + ' driver(s) FAILED'); process.exit(1); }
console.log('ALL DRIVERS PASS');
