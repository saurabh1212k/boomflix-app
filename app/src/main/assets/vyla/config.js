export const SOURCES = [
    // ── 1) ANIME EXTRACTORS ──
    {
        key: 'animetsu',
        label: 'Animetsu',
        sourceFile: 'animetsu',
        genre: 'anime',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'hianime',
        label: 'HiAnime',
        sourceFile: 'hianime',
        genre: 'anime',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'zunime',
        label: 'Zunime',
        sourceFile: 'zunime',
        genre: 'anime',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: '123anime',
        label: '123Anime',
        sourceFile: '123anime',
        genre: 'anime',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'anihq',
        label: 'AniHQ',
        sourceFile: 'anihq',
        genre: 'anime',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'anineko',
        label: 'AniNeko',
        sourceFile: 'anineko',
        genre: 'anime',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'kisskh',
        label: 'KissKH',
        sourceFile: 'kisskh',
        genre: 'anime',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },

    // ── 2) ALL OTHER EXTRACTORS (MOVIES & TV) ──
    {
        key: 'frame',
        label: 'Frame',
        sourceFile: 'frame',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'pstream',
        label: 'Pstream',
        sourceFile: 'pstream',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'insertunit',
        label: 'InsertUnit',
        sourceFile: 'insertunit',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'turbovid',
        label: 'TurboVid',
        sourceFile: 'turbovid',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'voe',
        label: 'VOE',
        sourceFile: 'voe',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'warezcdn',
        label: 'WarezCDN',
        sourceFile: 'warezcdn',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'soapertv',
        label: 'SoaperTV',
        sourceFile: 'soapertv',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'vixsrc',
        label: 'VixSrc',
        sourceFile: 'vixsrc',
        genre: 'general',
        proxyParam: 'vx',
        timeout: 35000,
        jitter: 0,
        retries: 2,
        multiUrl: false,
        skipProxy: true,
        verifyHeaders: {
            Accept: 'application/json, text/javascript, /; q=0.01',
            'Accept-Language': 'en-US,en;q=0.9',
            Referer: 'https://vixsrc.to/',
            Origin: 'https://vixsrc.to',
        },
    },
    {
        key: 'vidapi',
        label: 'VidAPI',
        sourceFile: 'vidapi',
        genre: 'general',
        proxyParam: 'va',
        timeout: 25000,
        jitter: 500,
        retries: 1,
        multiUrl: true
    },
    {
        key: 'vidnest',
        label: 'VidNest',
        sourceFile: 'vidnest',
        genre: 'general',
        proxyParam: 'vn',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'cinejoy',
        label: 'Cinejoy',
        sourceFile: 'cinejoy',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'fsharetv',
        label: 'Fsharetv',
        sourceFile: 'fsharetv',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'fsonic',
        label: 'Fsonic',
        sourceFile: 'fsonic',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'fsonline',
        label: 'Fsonline',
        sourceFile: 'fsonline',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'lmscript',
        label: 'Lmscript',
        sourceFile: 'lmscript',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'lookmovie',
        label: 'Lookmovie',
        sourceFile: 'lookmovie',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'meowtv',
        label: 'Meowtv',
        sourceFile: 'meowtv',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'peestream',
        label: 'Peestream',
        sourceFile: 'peestream',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'pengu',
        label: 'Pengu',
        sourceFile: 'pengu',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'purstream',
        label: 'Purstream',
        sourceFile: 'purstream',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'rivestream',
        label: 'Rivestream',
        sourceFile: 'rivestream',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'vidcore',
        label: 'Vidcore',
        sourceFile: 'vidcore',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'vidfast',
        label: 'Vidfast',
        sourceFile: 'vidfast',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'vidlink',
        label: 'Vidlink',
        sourceFile: 'vidlink',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'vidrock',
        label: 'Vidrock',
        sourceFile: 'vidrock',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'vidvault',
        label: 'Vidvault',
        sourceFile: 'vidvault',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'vidzee',
        label: 'VidZee',
        sourceFile: 'vidzee',
        genre: 'general',
        proxyParam: 'vz',
        timeout: 20000,
        sourcesTimeout: 10000,
        jitter: 400,
        retries: 3,
        verifyHeaders: {
            Accept: '/',
            'Accept-Language': 'en-US,en;q=0.9',
            Referer: 'https://player.vidzee.wtf',
            Origin: 'https://player.vidzee.wtf',
        }
    },
    {
        key: 'vidup',
        label: 'Vidup',
        sourceFile: 'vidup',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'cinesu',
        label: 'Cinesu',
        sourceFile: 'cinesu',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'cinesrc',
        label: 'Cinesrc',
        sourceFile: 'cinesrc',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'bcine',
        label: 'Bcine',
        sourceFile: 'bcine',
        genre: 'general',
        timeout: 20000,
        jitter: 200,
        retries: 2,
        multiUrl: true
    },
    {
        key: '111477',
        label: '111477',
        sourceFile: '111477',
        genre: 'general',
        proxyParam: 'a11',
        timeout: 25000,
        jitter: 500,
        retries: 2,
        multiUrl: true
    },
    {
        key: '4khdhub',
        label: '4KHDHub',
        sourceFile: '4khdhub',
        genre: 'general',
        proxyParam: 'khd',
        timeout: 35000,
        jitter: 500,
        retries: 1,
        multiUrl: true
    },
    {
        key: 'downloadeverything',
        label: 'DownloadEverything',
        sourceFile: 'downloadeverything',
        genre: 'general',
        proxyParam: 'de',
        timeout: 35000,
        jitter: 500,
        retries: 1,
        multiUrl: true
    },
    {
        key: 'flaxmovies',
        label: 'FlaxMovies',
        sourceFile: 'flaxmovies',
        genre: 'general',
        proxyParam: 'fx',
        timeout: 20000,
        jitter: 500,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'hexa',
        label: 'Hexa',
        sourceFile: 'hexa',
        genre: 'general',
        proxyParam: 'hx',
        timeout: 20000,
        jitter: 500,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'movienight',
        label: 'MovieNight',
        sourceFile: 'movienight',
        genre: 'general',
        proxyParam: 'mn',
        timeout: 20000,
        jitter: 500,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'movy',
        label: 'Movy',
        sourceFile: 'movy',
        genre: 'general',
        proxyParam: 'mv',
        timeout: 25000,
        jitter: 500,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'streamaggregator',
        label: 'StreamAggregator',
        sourceFile: 'streamaggregator',
        genre: 'general',
        proxyParam: 'sa',
        timeout: 25000,
        jitter: 500,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'vadapav',
        label: 'Vadapav',
        sourceFile: 'vadapav',
        genre: 'general',
        proxyParam: 'vp',
        timeout: 25000,
        jitter: 500,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'vidgod',
        label: 'VidGod',
        sourceFile: 'vidgod',
        genre: 'general',
        proxyParam: 'vg',
        timeout: 25000,
        jitter: 500,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'vuflix',
        label: 'Vuflix',
        sourceFile: 'vuflix',
        genre: 'general',
        proxyParam: 'vfx',
        timeout: 25000,
        jitter: 500,
        retries: 2,
        multiUrl: true
    },
    {
        key: 'xdownloader',
        label: 'XDownloader',
        sourceFile: 'xdownloader',
        genre: 'general',
        proxyParam: 'xd',
        timeout: 25000,
        jitter: 500,
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