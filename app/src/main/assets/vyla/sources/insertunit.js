import { USER_AGENT, fetchJson } from '../utils/helpers.js';

export async function getStream({ id, s, e }) {
    const isTv = s != null && e != null;
    const allUrls = [];

    const endpoint = isTv
        ? `https://api.insertunit.com/stream/tv/${id}/${s}/${e}`
        : `https://api.insertunit.com/stream/movie/${id}`;

    try {
        const data = await fetchJson(endpoint, {
            headers: {
                'User-Agent': USER_AGENT,
                'Referer': 'https://insertunit.com/',
                'Origin': 'https://insertunit.com',
                'Accept': 'application/json'
            },
            signal: AbortSignal.timeout(8000)
        });

        const streamUrl = data?.url || data?.stream || data?.playlist;
        if (streamUrl) {
            allUrls.push({
                url: streamUrl,
                server: 'InsertUnit',
                quality: 'Auto',
                type: 'hls',
                headers: {
                    'Referer': 'https://insertunit.com/',
                    'User-Agent': USER_AGENT
                },
                subtitles: Array.isArray(data.subtitles) ? data.subtitles.map(sub => ({
                    url: sub.url,
                    lang: sub.language || sub.lang || 'en',
                    label: sub.label || sub.language || 'English'
                })) : []
            });
        }
    } catch { }

    return allUrls.length ? { allUrls } : null;
}
