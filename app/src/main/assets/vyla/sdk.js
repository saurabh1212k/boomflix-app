import SOURCES, { SOURCE_MAP, getSourcesForGenre } from "./config.js";
import { fetchDownloads } from "./downloads/downloads.js";
import { probeSource } from "./health.js";
import { fetchSubtitles, getSubPathsMovie, getSubPathsTv } from "./subtitles.js";
import { createStreamArgs } from "./utils/helpers.js";

// Static imports for WebView compatibility - Curated 20 servers
import * as movy from './sources/movy.js';
import * as frame from './sources/frame.js';
import * as vidzee from './sources/vidzee.js';
import * as peestream from './sources/peestream.js';
import * as rivestream from './sources/rivestream.js';

import * as vidapi from './sources/vidapi.js';
import * as _111477 from './sources/111477.js';
import * as vidlink from './sources/vidlink.js';
import * as xdownloader from './sources/xdownloader.js';
import * as purstream from './sources/purstream.js';
import * as flaxmovies from './sources/flaxmovies.js';
import * as lmscript from './sources/lmscript.js';
import * as vidvault from './sources/vidvault.js';
import * as movienight from './sources/movienight.js';
import * as cinesu from './sources/cinesu.js';
import * as pstream from './sources/pstream.js';
import * as vidfast from './sources/vidfast.js';
import * as vidcore from './sources/vidcore.js';
import * as animetsu from './sources/animetsu.js';
import * as hianime from './sources/hianime.js';

const SOURCE_MODULES = {
    'movy': movy,
    'frame': frame,
    'vidzee': vidzee,
    'peestream': peestream,
    'rivestream': rivestream,
    'vidapi': vidapi,
    '111477': _111477,
    'vidlink': vidlink,
    'xdownloader': xdownloader,
    'purstream': purstream,
    'flaxmovies': flaxmovies,
    'lmscript': lmscript,
    'vidvault': vidvault,
    'movienight': movienight,
    'cinesu': cinesu,
    'pstream': pstream,
    'vidfast': vidfast,
    'vidcore': vidcore,
    'animetsu': animetsu,
    'hianime': hianime
};

export default class VylaSDK {
    tmdbApiKey = null;
    penguManifest = null;
    constructor({ tmdbApiKey, penguManifest } = {}) {
        this.tmdbApiKey = tmdbApiKey || null;
        this.penguManifest = penguManifest || null;
    }
    getSources(excludeDisabled = false, isAnime = null) {
        let list = SOURCES;
        if (isAnime === true) {
            list = [...SOURCES.filter(s => s.genre === 'anime'), ...SOURCES.filter(s => s.genre !== 'anime')];
        } else if (isAnime === false) {
            list = [...SOURCES.filter(s => s.genre !== 'anime'), ...SOURCES.filter(s => s.genre === 'anime')];
        }
        if (excludeDisabled) {
            return list.filter(cfg => !cfg.disabled);
        }
        return list;
    }
    async probeAllSources() {
        const active = SOURCES.filter(cfg => !cfg.disabled);
        const results = await Promise.allSettled(
            active.map(async cfg => await probeSource(cfg, SOURCE_MODULES[cfg.key]))
        );
        const sources = Object.fromEntries(
            active.map((cfg, i) => [
                cfg.key,
                results[i].status === 'fulfilled' ? results[i].value : { ok: false, ms: null },
            ])
        );
        return sources;
    }
    async probeSource(key) {
        const cfg = SOURCES.find(cfg => cfg.key === key);
        if (!cfg) throw new Error(`Source with key "${key}" not found`);
        const mod = SOURCE_MODULES[key];
        if (!mod) throw new Error(`Source module for key "${key}" not found`);
        return await probeSource(this, cfg, mod);
    }
    async getStream(key, id, s = null, e = null, clientIP = null) {
        const cfg = SOURCES.find(cfg => cfg.key === key);
        if (!cfg) throw new Error(`Source with key "${key}" not found`);
        const mod = SOURCE_MODULES[key];
        if (!mod) throw new Error(`Source module for key "${key}" not found`);
        const streamArgs = await createStreamArgs(cfg, this, id, s, e, clientIP);
        return await mod.getStream(streamArgs);
    }
    async getSubtitles(id, s = null, e = null) {
        const paths = s != null && e != null
            ? getSubPathsTv(id, s, e)
            : getSubPathsMovie(id);
        const results = await Promise.allSettled(
            paths.map(async path => await fetchSubtitles(path))
        );
        const merged = results
            .filter(r => r.status === 'fulfilled' && r.value)
            .flatMap(r => r.value);
        return Array.from(new Map(merged.map(sub => [sub.url, sub])).values());
    }
    async getDownloads(id, s = null, e = null) {
        return await fetchDownloads(id, s, e);
    }
}