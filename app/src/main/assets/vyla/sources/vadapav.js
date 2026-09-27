import { getUA } from '../utils/helpers.js';

const ADDON_BASE = 'https://stremio.vadapav.mov';

async function resolveImdbId(sdk, id, isTv) {
    const apiKey = sdk?.tmdbApiKey || process.env.TMDB_API_KEY;
    if (!apiKey) return null;
    const type = isTv ? 'tv' : 'movie';
    try {
        const res = await fetch(`https://api.themoviedb.org/3/${type}/${id}?api_key=${apiKey}&append_to_response=external_ids`, {
            signal: AbortSignal.timeout(6000),
        });
        if (!res.ok) return null;
        const data = await res.json();
        return data.imdb_id || data.external_ids?.imdb_id || null;
    } catch {
        return null;
    }
}

export async function getStream({ id, s, e, sdk }) {
    const isTv = Boolean(s && e);
    const ua = getUA();

    const imdbId = await resolveImdbId(sdk, id, isTv);
    const targetIds = [];
    if (imdbId && imdbId.startsWith('tt')) {
        targetIds.push(imdbId);
    }
    targetIds.push(`tmdb:${id}`);

    const seenUrls = new Set();
    const allUrls = [];

    const defaultHeaders = {
        'User-Agent': ua,
        Accept: 'application/json, text/plain, */*',
    };

    for (const targetId of targetIds) {
        const streamPath = isTv
            ? `${ADDON_BASE}/stream/series/${targetId}:${s}:${e}.json`
            : `${ADDON_BASE}/stream/movie/${targetId}.json`;

        try {
            const res = await fetch(streamPath, {
                headers: defaultHeaders,
                signal: AbortSignal.timeout(12000),
            });

            if (!res.ok) continue;
            const data = await res.json();

            if (Array.isArray(data?.streams) && data.streams.length > 0) {
                for (const item of data.streams) {
                    if (!item || typeof item !== 'object') continue;
                    const url = String(item.url || '').trim();
                    if (!url || !url.startsWith('http') || seenUrls.has(url)) continue;

                    seenUrls.add(url);

                    const rawTitle = item.title ? String(item.title).trim() : '';
                    const rawName = item.name ? String(item.name).trim() : 'vadapav.mov';
                    const label = rawTitle || rawName;

                    allUrls.push({
                        url,
                        label,
                        headers: defaultHeaders,
                    });
                }

                if (allUrls.length > 0) break;
            }
        } catch { }
    }

    if (allUrls.length === 0) return null;

    return {
        url: allUrls[0].url,
        allUrls,
        headers: allUrls[0].headers,
    };
}