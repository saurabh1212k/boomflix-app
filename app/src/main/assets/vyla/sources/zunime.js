import { getTmdbInfo, tmdbToAnilist, fetchJson, USER_AGENT } from '../utils/helpers.js';

const ZUNIME_SERVERS = ['alfa', 'bravo', 'charlie'];
const BASE_URL = 'https://backend.zunime.net';

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
        'Referer': 'https://zunime.net/',
        'Accept': 'application/json, text/plain, */*',
    };

    for (const server of ZUNIME_SERVERS) {
        try {
            const data = await fetchJson(`${BASE_URL}/api/anime/stream?server=${server}&id=${anilistId}&ep=${e || 1}`, {
                headers,
                signal: AbortSignal.timeout(6000),
            });

            const streamUrl = data?.sources?.url || data?.stream?.url || data?.url;
            if (streamUrl) {
                allUrls.push({
                    url: streamUrl,
                    server: `Zunime (${server.toUpperCase()})`,
                    quality: 'Auto',
                    type: streamUrl.includes('.m3u8') ? 'hls' : 'mp4',
                    headers,
                });
                break;
            }
        } catch { }
    }

    return allUrls.length ? { allUrls } : null;
}
