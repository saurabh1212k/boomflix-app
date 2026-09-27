import { USER_AGENT, fetchJson } from '../utils/helpers.js';

export async function getStream({ id, s, e }) {
    const isTv = s != null && e != null;
    const allUrls = [];

    const url = isTv
        ? `https://warezcdn.link/get/tv/${id}/${s}/${e}`
        : `https://warezcdn.link/get/movie/${id}`;

    try {
        const data = await fetchJson(url, {
            headers: {
                'User-Agent': USER_AGENT,
                'Referer': 'https://warezcdn.link/',
                'Accept': 'application/json'
            },
            signal: AbortSignal.timeout(6000)
        });

        const streamUrl = data?.url || data?.file || data?.stream;
        if (streamUrl) {
            allUrls.push({
                url: streamUrl,
                server: 'WarezCDN',
                quality: 'Auto',
                type: streamUrl.includes('.m3u8') ? 'hls' : 'mp4',
                headers: {
                    'User-Agent': USER_AGENT,
                    'Referer': 'https://warezcdn.link/'
                }
            });
        }
    } catch { }

    return allUrls.length ? { allUrls } : null;
}
