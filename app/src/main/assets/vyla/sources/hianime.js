import { getTmdbInfo, tmdbToAnilist, fetchJson, USER_AGENT } from '../utils/helpers.js';

const BASE_URL = 'https://hianime.to';

export async function getStream({ id, s, e, sdk }) {
    const isTv = s != null && e != null;
    const mediaType = isTv ? 'tv' : 'movie';
    const info = await getTmdbInfo(sdk?.tmdbApiKey || id, id, mediaType, s);
    if (!info.isAnime) return null;

    const titles = info.titles || [];
    if (!titles.length) return null;

    const allUrls = [];
    const query = titles[0];

    try {
        const searchData = await fetchJson(`${BASE_URL}/ajax/search/suggest?keyword=${encodeURIComponent(query)}`, {
            headers: {
                'User-Agent': USER_AGENT,
                'Referer': `${BASE_URL}/`,
                'X-Requested-With': 'XMLHttpRequest'
            },
            signal: AbortSignal.timeout(6000)
        });

        const html = searchData?.html || '';
        const match = html.match(/href="\/watch\/([^"?]+)/i);
        if (match) {
            const animeId = match[1];
            // Extract episode servers
            const serversData = await fetchJson(`${BASE_URL}/ajax/v2/episode/servers?episodeId=${animeId}`, {
                headers: {
                    'User-Agent': USER_AGENT,
                    'Referer': `${BASE_URL}/watch/${animeId}`,
                    'X-Requested-With': 'XMLHttpRequest'
                },
                signal: AbortSignal.timeout(6000)
            }).catch(() => null);

            if (serversData?.html) {
                const subMatch = serversData.html.match(/(?:data-id="(\d+)"[^>]*data-type="sub"|data-type="sub"[^>]*data-id="(\d+)")/i);
                const dubMatch = serversData.html.match(/(?:data-id="(\d+)"[^>]*data-type="dub"|data-type="dub"[^>]*data-id="(\d+)")/i);
                const subId = subMatch?.[1] || subMatch?.[2];
                const dubId = dubMatch?.[1] || dubMatch?.[2];

                for (const item of [{ id: subId, label: 'Sub' }, { id: dubId, label: 'Dub' }]) {
                    if (!item.id || (item.label === 'Dub' && item.id === subId)) continue;
                    try {
                        const sourceData = await fetchJson(`${BASE_URL}/ajax/v2/episode/sources?id=${item.id}`, {
                            headers: {
                                'User-Agent': USER_AGENT,
                                'Referer': `${BASE_URL}/watch/${animeId}`,
                                'X-Requested-With': 'XMLHttpRequest'
                            },
                            signal: AbortSignal.timeout(6000)
                        });

                        const link = sourceData?.link;
                        if (link && !allUrls.some(u => u.url === link)) {
                            const isDub = item.label.toLowerCase() === 'dub';
                            allUrls.push({
                                url: link,
                                server: `HiAnime (HD ${item.label})`,
                                quality: '1080p',
                                type: link.includes('.m3u8') ? 'hls' : 'mp4',
                                audio: isDub ? 'dub' : 'sub',
                                headers: {
                                    'Referer': `${BASE_URL}/`,
                                    'User-Agent': USER_AGENT
                                }
                            });
                        }
                    } catch { }
                }

                if (allUrls.length >= 2 && allUrls[0].url !== allUrls[1].url) {
                    const primary = allUrls[0];
                    const secondary = allUrls[1];
                    const isSecondaryDub = secondary.audio === 'dub';
                    primary.audioVariants = [{
                        url: secondary.url,
                        lang: isSecondaryDub ? 'en' : 'ja',
                        label: isSecondaryDub ? 'English (Dub)' : 'Japanese (Sub)',
                        type: secondary.type
                    }];
                }
            }
        }
    } catch { }

    return allUrls.length ? { allUrls } : null;
}
