import { USER_AGENT, fetchText, fetchJson } from '../utils/helpers.js';

const BASE_URL = 'https://soaper.tv';

export async function getStream({ id, s, e, sdk }) {
    const tmdbApiKey = sdk?.tmdbApiKey;
    if (!tmdbApiKey) return null;

    const isTv = s != null && e != null;
    const allUrls = [];

    try {
        const tmdbData = await fetchJson(`https://api.themoviedb.org/3/${isTv ? 'tv' : 'movie'}/${id}?api_key=${tmdbApiKey}`, { signal: AbortSignal.timeout(4000) });
        const title = tmdbData?.title || tmdbData?.name;
        if (!title) return null;

        const searchHtml = await fetchText(`${BASE_URL}/search.html?keyword=${encodeURIComponent(title)}`, {
            headers: {
                'User-Agent': USER_AGENT,
                'Referer': `${BASE_URL}/`
            },
            signal: AbortSignal.timeout(6000)
        });

        const linkMatch = searchHtml.match(/href="(\/(?:movie|show)_([^.]+)\.html)"/i);
        if (linkMatch) {
            const pageUrl = `${BASE_URL}${linkMatch[1]}`;
            const pageHtml = await fetchText(pageUrl, {
                headers: {
                    'User-Agent': USER_AGENT,
                    'Referer': `${BASE_URL}/`
                },
                signal: AbortSignal.timeout(6000)
            });

            const streamMatch = pageHtml.match(/val:\s*['"]([^'"]+\.m3u8[^'"]*)['"]/i) || pageHtml.match(/source:\s*['"]([^'"]+\.m3u8[^'"]*)['"]/i);
            if (streamMatch) {
                allUrls.push({
                    url: streamMatch[1],
                    server: 'SoaperTV',
                    quality: '1080p',
                    type: 'hls',
                    headers: {
                        'Referer': `${BASE_URL}/`,
                        'User-Agent': USER_AGENT
                    }
                });
            }
        }
    } catch { }

    return allUrls.length ? { allUrls } : null;
}
