import { USER_AGENT } from '../utils/helpers.js';

export async function getStream({ id, s, e }) {
    const isTv = s != null && e != null;
    const allUrls = [];

    // VOE endpoint resolution via gateway
    const targetUrl = isTv
        ? `https://voe.sx/e/tv_${id}_${s}_${e}`
        : `https://voe.sx/e/movie_${id}`;

    try {
        let res = await fetch(targetUrl, {
            headers: { 'User-Agent': USER_AGENT },
            signal: AbortSignal.timeout(6000)
        });

        let html = await res.text();
        const redirectMatch = html.match(/window\.location\.href\s*=\s*['"](https?:\/\/[^'"]+)['"]/);
        if (redirectMatch) {
            res = await fetch(redirectMatch[1], {
                headers: { 'User-Agent': USER_AGENT },
                signal: AbortSignal.timeout(6000)
            });
            html = await res.text();
        }

        const hlsMatch = html.match(/['"](https?:\/\/[^'"]+\.m3u8[^'"]*)['"]/i);
        if (hlsMatch) {
            allUrls.push({
                url: hlsMatch[1],
                server: 'VOE',
                quality: '1080p',
                type: 'hls',
                headers: {
                    'User-Agent': USER_AGENT,
                    'Referer': 'https://voe.sx/'
                }
            });
        }
    } catch { }

    return allUrls.length ? { allUrls } : null;
}
