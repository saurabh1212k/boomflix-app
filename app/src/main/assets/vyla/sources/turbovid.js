import { USER_AGENT, fetchJson } from '../utils/helpers.js';

export async function getStream({ id, s, e }) {
    const isTv = s != null && e != null;
    const allUrls = [];

    const url = isTv
        ? `https://turbovid.stream/api/stream?tmdb=${id}&season=${s}&episode=${e}`
        : `https://turbovid.stream/api/stream?tmdb=${id}`;

    try {
        const data = await fetchJson(url, {
            headers: {
                'User-Agent': USER_AGENT,
                'Referer': 'https://turbovid.stream/',
                'Accept': 'application/json'
            },
            signal: AbortSignal.timeout(7000)
        });

        const streamUrl = data?.source || data?.stream || data?.url;
        if (streamUrl) {
            allUrls.push({
                url: streamUrl,
                server: 'TurboVid',
                quality: 'Auto',
                type: streamUrl.includes('.m3u8') ? 'hls' : 'mp4',
                headers: {
                    'Referer': 'https://turbovid.stream/',
                    'User-Agent': USER_AGENT
                }
            });
        }
    } catch { }

    return allUrls.length ? { allUrls } : null;
}
