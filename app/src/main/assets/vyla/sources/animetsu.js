import { getTmdbInfo, tmdbToAnilist, fetchJson, USER_AGENT } from '../utils/helpers.js';

const ANIMETSU_SERVERS = ['zoro', 'pahe', 'meg', 'zaza', 'bato'];
const BASE_URL = 'https://backend.animetsu.net';

export async function getStream({ id, s, e, sdk }) {
    const isTv = s != null && e != null;
    const mediaType = isTv ? 'tv' : 'movie';
    const info = await getTmdbInfo(sdk?.tmdbApiKey || id, id, mediaType, s);
    if (!info.isAnime) return null;

    const anilistId = await tmdbToAnilist(id, mediaType, s || 1, info.titles, info.year);
    if (!anilistId) return null;

    const allUrls = [];
    const headers = {
        'User-Agent': USER_AGENT,
        'Referer': 'https://animetsu.net/',
        'Origin': 'https://backend.animetsu.net',
        'Accept': 'application/json, text/plain, */*',
    };

    for (const server of ANIMETSU_SERVERS) {
        let subSource = null;
        let dubSource = null;

        // Try sub and dub in parallel
        await Promise.allSettled([
            fetchJson(`${BASE_URL}/api/anime/tiddies?server=${server}&id=${anilistId}&num=${e || 1}&subType=sub`, {
                headers,
                signal: AbortSignal.timeout(6000),
            }).then(data => {
                if (data?.sources?.[0]?.url) subSource = data.sources[0];
            }).catch(() => {}),
            fetchJson(`${BASE_URL}/api/anime/tiddies?server=${server}&id=${anilistId}&num=${e || 1}&subType=dub`, {
                headers,
                signal: AbortSignal.timeout(6000),
            }).then(data => {
                if (data?.sources?.[0]?.url) dubSource = data.sources[0];
            }).catch(() => {}),
        ]);

        if (subSource?.url || dubSource?.url) {
            const primary = subSource || dubSource;
            const isSub = Boolean(subSource?.url);
            const audioVariants = [];

            if (subSource?.url && dubSource?.url) {
                audioVariants.push({
                    url: dubSource.url,
                    lang: 'en',
                    label: 'English (Dub)',
                    type: dubSource.type === 'mp4' ? 'mp4' : 'hls',
                });
            }

            allUrls.push({
                url: primary.url,
                server: `Animetsu (${server.toUpperCase()}${isSub ? ' Sub' : ' Dub'})`,
                quality: primary.quality || 'Auto',
                type: primary.type === 'mp4' ? 'mp4' : 'hls',
                audio: isSub ? 'sub' : 'dub',
                headers,
                audioVariants: audioVariants.length > 0 ? audioVariants : undefined,
            });

            break;
        }
    }

    return allUrls.length ? { allUrls } : null;
}
