import SOURCES, { SOURCE_MAP, getSourcesForGenre } from "./config.js";
import { fetchDownloads } from "./downloads/downloads.js";
import { probeSource } from "./health.js";
import { fetchSubtitles, getSubPathsMovie, getSubPathsTv } from "./subtitles.js";
import { createStreamArgs } from "./utils/helpers.js";

// Static imports for Vite compatibility
// Anime Extractors
import * as animetsu from './sources/animetsu.js';
import * as hianime from './sources/hianime.js';
import * as zunime from './sources/zunime.js';
import * as _123anime from './sources/123anime.js';
import * as anihq from './sources/anihq.js';
import * as anineko from './sources/anineko.js';
import * as kisskh from './sources/kisskh.js';

// General Extractors
import * as insertunit from './sources/insertunit.js';
import * as turbovid from './sources/turbovid.js';
import * as voe from './sources/voe.js';
import * as warezcdn from './sources/warezcdn.js';
import * as soapertv from './sources/soapertv.js';
import * as frame from './sources/frame.js';
import * as pstream from './sources/pstream.js';
import * as vixsrc from './sources/vixsrc.js';
import * as vidapi from './sources/vidapi.js';
import * as vidnest from './sources/vidnest.js';
import * as cinejoy from './sources/cinejoy.js';
import * as fsharetv from './sources/fsharetv.js';
import * as fsonic from './sources/fsonic.js';
import * as fsonline from './sources/fsonline.js';
import * as lmscript from './sources/lmscript.js';
import * as lookmovie from './sources/lookmovie.js';
import * as meowtv from './sources/meowtv.js';
import * as peestream from './sources/peestream.js';
import * as pengu from './sources/pengu.js';
import * as purstream from './sources/purstream.js';
import * as rivestream from './sources/rivestream.js';
import * as vidcore from './sources/vidcore.js';
import * as vidfast from './sources/vidfast.js';
import * as vidlink from './sources/vidlink.js';
import * as vidrock from './sources/vidrock.js';
import * as vidup from './sources/vidup.js';
import * as vidvault from './sources/vidvault.js';
import * as vidzee from './sources/vidzee.js';
import * as cinesu from './sources/cinesu.js';
import * as cinesrc from './sources/cinesrc.js';
import * as bcine from './sources/bcine.js';
import * as _111477 from './sources/111477.js';
import * as _4khdhub from './sources/4khdhub.js';
import * as downloadeverything from './sources/downloadeverything.js';
import * as flaxmovies from './sources/flaxmovies.js';
import * as hexa from './sources/hexa.js';
import * as movienight from './sources/movienight.js';
import * as movy from './sources/movy.js';
import * as streamaggregator from './sources/streamaggregator.js';
import * as vadapav from './sources/vadapav.js';
import * as vidgod from './sources/vidgod.js';
import * as vuflix from './sources/vuflix.js';
import * as xdownloader from './sources/xdownloader.js';

const SOURCE_MODULES = {
    // Anime
    'animetsu': animetsu,
    'hianime': hianime,
    'zunime': zunime,
    '123anime': _123anime,
    'anihq': anihq,
    'anineko': anineko,
    'kisskh': kisskh,

    // General
    'insertunit': insertunit,
    'turbovid': turbovid,
    'voe': voe,
    'warezcdn': warezcdn,
    'soapertv': soapertv,
    'frame': frame,
    'pstream': pstream,
    'vixsrc': vixsrc,
    'vidapi': vidapi,
    'vidnest': vidnest,
    'cinejoy': cinejoy,
    'fsharetv': fsharetv,
    'fsonic': fsonic,
    'fsonline': fsonline,
    'lmscript': lmscript,
    'lookmovie': lookmovie,
    'meowtv': meowtv,
    'peestream': peestream,
    'pengu': pengu,
    'purstream': purstream,
    'rivestream': rivestream,
    'vidcore': vidcore,
    'vidfast': vidfast,
    'vidlink': vidlink,
    'vidrock': vidrock,
    'vidup': vidup,
    'vidvault': vidvault,
    'vidzee': vidzee,
    'cinesu': cinesu,
    'cinesrc': cinesrc,
    'bcine': bcine,
    '111477': _111477,
    '4khdhub': _4khdhub,
    'downloadeverything': downloadeverything,
    'flaxmovies': flaxmovies,
    'hexa': hexa,
    'movienight': movienight,
    'movy': movy,
    'streamaggregator': streamaggregator,
    'vadapav': vadapav,
    'vidgod': vidgod,
    'vuflix': vuflix,
    'xdownloader': xdownloader
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
    async getStream(key, id, s = null, e = null, clientIP = null, extra = {}) {
        const cfg = SOURCES.find(cfg => cfg.key === key);
        if (!cfg) throw new Error(`Source with key "${key}" not found`);
        const mod = SOURCE_MODULES[key];
        if (!mod) throw new Error(`Source module for key "${key}" not found`);
        const streamArgs = await createStreamArgs(cfg, this, id, s, e, clientIP, extra);
        return await mod.getStream(streamArgs);
    }
    async getSubtitles(id, s = null, e = null) {
        const paths = s != null && e != null
            ? await getSubPathsTv(id, s, e)
            : await getSubPathsMovie(id);
        return await fetchSubtitles(paths);
    }
    async getDownloads(id, s = null, e = null) {
        return await fetchDownloads(id, s, e);
    }
}