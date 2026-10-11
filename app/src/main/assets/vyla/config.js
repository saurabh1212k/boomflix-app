export const SOURCES = [
    // ── 5 User-Requested Core Servers ──
    {
        key: 'movy',
        label: 'Argos',
        sourceFile: 'movy',
        genre: 'general',
        proxyParam: 'mv',
        timeout: 25000,
        jitter: 500,
        retries: 2,
        multiUrl: true,
        skipProxy: true,
        cdnHeaders: [{
            pattern: /wecollege\.net|movy\.bz|paleoak\.top|zenoak\.top|stormhive\.top/i,
            headers: {
                Referer: 'https://www.movy.bz/',
                Origin: 'https://www.movy.bz',
            },
        }]
    },
    {
        key: 'frame',
        label: 'Olympus',
        sourceFile: 'frame',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'vidzee',
        label: 'Helios',
        sourceFile: 'vidzee',
        genre: 'general',
        proxyParam: 'vz',
        timeout: 20000,
        sourcesTimeout: 10000,
        jitter: 400,
        retries: 3,
        verifyHeaders: {
            Accept: '*/*',
            'Accept-Language': 'en-US,en;q=0.9',
            Referer: 'https://player.vidzee.wtf',
            Origin: 'https://player.vidzee.wtf',
        }
    },
    {
        key: 'peestream',
        label: 'Orion',
        sourceFile: 'peestream',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'rivestream',
        label: 'Atlantia',
        sourceFile: 'rivestream',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },

    // ── 15 Best Working, No-Lag Content Extractors ──
    {
        key: 'vidapi',
        label: 'Chronos',
        sourceFile: 'vidapi',
        genre: 'general',
        proxyParam: 'va',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: '111477',
        label: 'Titan',
        sourceFile: '111477',
        genre: 'general',
        proxyParam: 'a11',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true,
        skipProxy: true,
        cdnHeaders: [{
            pattern: /111477\.xyz/i,
            headers: {
                Referer: 'https://st.111477.xyz/',
                Origin: 'https://st.111477.xyz',
            },
        }]
    },
    {
        key: 'vidlink',
        label: 'Apex',
        sourceFile: 'vidlink',
        genre: 'general',
        proxyParam: 'vl',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true,
        skipProxy: true
    },
    {
        key: 'xdownloader',
        label: 'Nova',
        sourceFile: 'xdownloader',
        genre: 'general',
        proxyParam: 'xd',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true,
        skipProxy: true,
        cdnHeaders: [{
            pattern: /films365\.org/i,
            headers: {
                Referer: 'https://www.films365.org/',
                Origin: 'https://www.films365.org',
            },
        }]
    },
    {
        key: 'purstream',
        label: 'Nebula',
        sourceFile: 'purstream',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'flaxmovies',
        label: 'Phoenix',
        sourceFile: 'flaxmovies',
        genre: 'general',
        proxyParam: 'fx',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true,
        skipProxy: true,
        cdnHeaders: [{
            pattern: /flix2watch\.pro|flaxmovies\.xyz/i,
            headers: {
                Referer: 'https://flaxmovies.xyz/',
                Origin: 'https://flaxmovies.xyz',
            },
        }]
    },
    {
        key: 'lmscript',
        label: 'Genesis',
        sourceFile: 'lmscript',
        genre: 'general',
        proxyParam: 'lmsc',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'vidvault',
        label: 'Valkyrie',
        sourceFile: 'vidvault',
        genre: 'general',
        proxyParam: 'vv',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'movienight',
        label: 'Aether',
        sourceFile: 'movienight',
        genre: 'general',
        proxyParam: 'mn',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true,
        skipProxy: true,
        cdnHeaders: [{
            pattern: /movienig\.ht/i,
            headers: {
                Referer: 'https://movienig.ht/',
                Origin: 'https://movienig.ht',
            },
        }]
    },
    {
        key: 'cinesu',
        label: 'Zenith',
        sourceFile: 'cinesu',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true,
        cdnHeaders: [{
            pattern: /cine\.su/i,
            headers: {
                Referer: 'https://cine.su/',
                Origin: 'https://cine.su',
            },
        }]
    },
    {
        key: 'pstream',
        label: 'Hyperion',
        sourceFile: 'pstream',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'vidfast',
        label: 'Polaris',
        sourceFile: 'vidfast',
        genre: 'general',
        proxyParam: 'vf',
        timeout: 25000,
        jitter: 300,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'vidcore',
        label: 'Vortex',
        sourceFile: 'vidcore',
        genre: 'general',
        proxyParam: 'vc',
        timeout: 25000,
        jitter: 300,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'animetsu',
        label: 'Sakura',
        sourceFile: 'animetsu',
        genre: 'anime',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'hianime',
        label: 'Shinobi',
        sourceFile: 'hianime',
        genre: 'anime',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    }
];

export const ANIME_SOURCES = SOURCES.filter(s => s.genre === 'anime');
export const GENERAL_SOURCES = SOURCES.filter(s => s.genre !== 'anime');

export function getSourcesForGenre(isAnime) {
    if (isAnime) {
        return [...ANIME_SOURCES, ...GENERAL_SOURCES];
    }
    return [...GENERAL_SOURCES, ...ANIME_SOURCES];
}

export const HEALTH_PROBE_ID = '155';
export const SOURCE_MAP = Object.fromEntries(SOURCES.map(s => [s.key, s]));
export default SOURCES;