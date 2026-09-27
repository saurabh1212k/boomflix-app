import { getUA } from '../utils/helpers.js';

const SLAVE_URL = 'https://slave.downloadeverythingfromeverywhere.com/';
const REFERER = 'https://downloadeverythingfromeverywhere.com/';
const ORIGIN = 'https://downloadeverythingfromeverywhere.com';
const USER_AGENT = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36';

function extractJsonObjects(text) {
    const objects = [];
    let depth = 0;
    let start = -1;
    let inString = false;
    let escape = false;

    for (let i = 0; i < text.length; i++) {
        const char = text[i];
        if (escape) {
            escape = false;
            continue;
        }
        if (char === '\\') {
            escape = true;
            continue;
        }
        if (char === '"') {
            inString = !inString;
            continue;
        }
        if (!inString) {
            if (char === '{') {
                if (depth === 0) start = i;
                depth++;
            } else if (char === '}') {
                depth--;
                if (depth === 0 && start !== -1) {
                    const jsonStr = text.substring(start, i + 1);
                    try {
                        objects.push(JSON.parse(jsonStr));
                    } catch { }
                    start = -1;
                }
            }
        }
    }
    return objects;
}

async function resolveDetails(sdk, id, isTv) {
    const apiKey = sdk?.tmdbApiKey || process.env.TMDB_API_KEY;
    if (!apiKey) return null;
    const type = isTv ? 'tv' : 'movie';
    try {
        const res = await fetch(`https://api.themoviedb.org/3/${type}/${id}?api_key=${apiKey}&append_to_response=external_ids`, {
            signal: AbortSignal.timeout(6000),
        });
        if (!res.ok) return null;
        const data = await res.json();
        const title = isTv ? data.name : data.title;
        const releaseDate = isTv ? data.first_air_date : data.release_date;
        const year = releaseDate ? parseInt(releaseDate.split('-')[0], 10) : null;
        const imdbId = data.imdb_id || data.external_ids?.imdb_id || null;
        return { title, year, imdbId };
    } catch {
        return null;
    }
}

async function resolveHubCloud(hubUrl, ua) {
    try {
        const res1 = await fetch(hubUrl, {
            headers: {
                'User-Agent': ua,
                Referer: REFERER,
            },
            signal: AbortSignal.timeout(8000),
        });

        const html = await res1.text();
        const hubPhpMatch = html.match(/https?:\/\/[^\s"<>]+\/hubcloud\.php\?[^\s"<>]+/i);
        if (!hubPhpMatch) return null;

        const phpUrl = hubPhpMatch[0];
        const phpRes = await fetch(phpUrl, {
            headers: {
                'User-Agent': ua,
                Referer: hubUrl,
            },
            signal: AbortSignal.timeout(8000),
        });

        const phpBody = await phpRes.text();

        const r2Match = phpBody.match(/https?:\/\/[a-zA-Z0-9.\-_]+\.r2\.cloudflarestorage\.com\/[^\s"<>]+/i);
        if (r2Match) {
            return r2Match[0].replaceAll('&amp;', '&');
        }

        const pixelMatch = phpBody.match(/https?:\/\/pixel\.hubcloud\.[a-z]+\/\?id=[^\s"<>]+/i);
        if (pixelMatch) {
            return pixelMatch[0];
        }
    } catch { }
    return null;
}

async function resolveClicknUpload(clicknUrl, ua) {
    try {
        const res1 = await fetch(clicknUrl, {
            headers: {
                'User-Agent': ua,
                Referer: REFERER,
            },
            signal: AbortSignal.timeout(8000),
        });

        const body1 = await res1.text();
        const formMatch1 = body1.match(/<form[^>]+method=["']POST["'][^>]*>([\s\S]*?)<\/form>/i);
        if (!formMatch1) return null;

        const inputMatches1 = [...formMatch1[1].matchAll(/<input[^>]+name=["']([^"']+)["'][^>]+value=["']([^"']*)["']/gi)];
        const params1 = new URLSearchParams();
        for (const m of inputMatches1) {
            params1.set(m[1], m[2]);
        }
        params1.set('method_free', 'Slow Download');

        const rawCookies = res1.headers.getSetCookie?.() || [res1.headers.get('set-cookie')].filter(Boolean);
        const cookieHeader = rawCookies.join('; ');

        const res2 = await fetch(clicknUrl, {
            method: 'POST',
            headers: {
                'User-Agent': ua,
                Referer: clicknUrl,
                'Content-Type': 'application/x-www-form-urlencoded',
                ...(cookieHeader ? { Cookie: cookieHeader } : {}),
            },
            body: params1.toString(),
            signal: AbortSignal.timeout(8000),
        });

        const body2 = await res2.text();
        const formMatch2 = body2.match(/<form[^>]+method=["']POST["'][^>]*>([\s\S]*?)<\/form>/i);
        if (!formMatch2) return null;

        const inputMatches2 = [...formMatch2[1].matchAll(/<input[^>]+name=["']([^"']+)["'][^>]+value=["']([^"']*)["']/gi)];
        const params2 = new URLSearchParams();
        for (const m of inputMatches2) {
            params2.set(m[1], m[2]);
        }
        params2.set('down_script', '1');

        await new Promise(r => setTimeout(r, 4500));

        const res3 = await fetch(clicknUrl, {
            method: 'POST',
            headers: {
                'User-Agent': ua,
                Referer: clicknUrl,
                'Content-Type': 'application/x-www-form-urlencoded',
                ...(cookieHeader ? { Cookie: cookieHeader } : {}),
            },
            body: params2.toString(),
            signal: AbortSignal.timeout(8000),
        });

        const body3 = await res3.text();
        const directMatch = body3.match(/https?:\/\/[a-zA-Z0-9.\-_:]+\/d\/[a-zA-Z0-9_\-\/]+/i) ||
            body3.match(/window\.open\(["'](https?:\/\/[^"']+)["']\)/i);

        if (directMatch) {
            const found = directMatch[1] || directMatch[0];
            if (found.includes('clicknupload.') && !found.includes('/d/')) {
                return null;
            }
            return found;
        }
    } catch { }
    return null;
}

async function resolveItem(item, fallbackTitle, ua) {
    const rawUrl = String(item.url || '').trim();
    if (!rawUrl) return null;

    if (
        rawUrl.includes('111477.xyz') ||
        rawUrl.includes('vadapav.mov') ||
        rawUrl.includes('driveseed.org') ||
        rawUrl.includes('new3.gdflix.io') ||
        rawUrl.includes('rapidrar.cr') ||
        rawUrl.includes('megaup.net') ||
        rawUrl.includes('telegram.dog') ||
        rawUrl.includes('t.me')
    ) {
        return null;
    }

    let directStreamUrl = null;
    let provider = item.site || 'DownloadEverything';
    let streamHeaders = { 'User-Agent': ua };

    try {
        if (rawUrl.includes('hakunaymatata.com')) {
            directStreamUrl = rawUrl;
            provider = 'Moviebox';
            streamHeaders = { 'User-Agent': 'Lavf/60.16.100' };
        } else if (rawUrl.includes('pixeldrain.dev') || rawUrl.includes('pixeldrain.com')) {
            const m = rawUrl.match(/pixeldrain\.(?:dev|com)\/(?:u|l)\/([a-zA-Z0-9_-]+)/);
            if (m) {
                directStreamUrl = `https://pixeldrain.com/api/file/${m[1]}`;
                provider = 'Pixeldrain';
            }
        } else if (rawUrl.includes('hubcloud.') || rawUrl.includes('vcloud.zip')) {
            directStreamUrl = await resolveHubCloud(rawUrl, ua);
            if (directStreamUrl) {
                provider = 'HubCloud';
            }
        } else if (rawUrl.includes('clicknupload.')) {
            directStreamUrl = await resolveClicknUpload(rawUrl, ua);
            if (directStreamUrl) {
                provider = 'ClicknUpload';
            }
        } else if (
            /\.(?:mp4|mkv)(?:\?|$)/i.test(rawUrl) &&
            !rawUrl.includes('111477.xyz') &&
            !rawUrl.includes('vadapav.mov') &&
            !rawUrl.includes('.cyou/res/')
        ) {
            try {
                const check = await fetch(rawUrl, {
                    method: 'HEAD',
                    headers: { 'User-Agent': ua },
                    signal: AbortSignal.timeout(2000),
                });
                if (check.status === 200 || check.status === 206 || (check.status >= 300 && check.status < 400)) {
                    directStreamUrl = rawUrl;
                    provider = item.site || 'DirectStream';
                }
            } catch { }
        }
    } catch { }

    if (!directStreamUrl) return null;

    const tags = Array.isArray(item.tags) ? item.tags.map(String) : [];
    const qualityMatch = tags.find(t => /2160p|4k|1080p|720p|480p/i.test(t)) || '1080p';
    const rawTitle = item.name || item.release || fallbackTitle;

    return {
        url: directStreamUrl,
        label: `[${provider}] ${rawTitle} (${qualityMatch})`,
        headers: streamHeaders,
    };
}

export async function getStream({ id, s, e, sdk }) {
    const isTv = Boolean(s && e);
    const mediaType = isTv ? 'series' : 'movie';
    const ua = USER_AGENT;

    const details = await resolveDetails(sdk, id, isTv);
    const title = details?.title || String(id);
    const tmdbId = parseInt(id, 10);

    const payload = {
        mode: mediaType,
        title,
        ...(details?.year ? { year: String(details.year) } : {}),
        ...(tmdbId ? { tmdb_id: tmdbId } : {}),
        ...(details?.imdbId ? { imdb_id: details.imdbId } : {}),
        ...(isTv && s != null ? { season: Number(s) } : {}),
        ...(isTv && e != null ? { episode: Number(e) } : {}),
    };

    const res = await fetch(SLAVE_URL, {
        method: 'POST',
        headers: {
            'User-Agent': ua,
            Origin: ORIGIN,
            Referer: REFERER,
            'Content-Type': 'application/json',
            Accept: 'application/x-ndjson',
            'x-defe-manual': '1',
            'sec-ch-ua': '"Microsoft Edge";v="153", "Not_A Brand";v="8", "Chromium";v="153"',
            'sec-ch-ua-mobile': '?0',
            'sec-ch-ua-platform': '"Windows"',
            'sec-fetch-dest': 'empty',
            'sec-fetch-mode': 'cors',
            'sec-fetch-site': 'same-site',
        },
        body: JSON.stringify(payload),
        signal: AbortSignal.timeout(16000),
    });

    if (!res.ok) return null;

    let fullText = '';
    if (res.body?.getReader) {
        const reader = res.body.getReader();
        const decoder = new TextDecoder();

        try {
            while (true) {
                const { done, value } = await reader.read();
                if (done) break;
                fullText += decoder.decode(value, { stream: true });
            }
        } catch { }
    } else {
        fullText = await res.text();
    }

    const objects = extractJsonObjects(fullText);
    const candidateItems = [];

    for (const parsed of objects) {
        if (parsed?.t === 'hit' && Array.isArray(parsed.links)) {
            const site = parsed.site || 'DownloadEverything';
            for (const l of parsed.links) {
                if (l && typeof l === 'object') {
                    candidateItems.push({ site, ...l });
                }
            }
        }
    }

    const resolved = await Promise.all(candidateItems.map(item => resolveItem(item, title, ua)));
    const allUrls = resolved.filter(Boolean);

    if (allUrls.length === 0) return null;

    return {
        url: allUrls[0].url,
        allUrls,
        headers: allUrls[0].headers,
    };
}