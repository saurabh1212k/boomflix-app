import { getUA } from '../utils/helpers.js';

const INITIAL_BASE_URL = 'https://4khdhub.one';
let cachedBaseUrl = null;
let cachedBaseUrlExpiry = 0;

function rot13(str) {
    return str.replace(/[a-zA-Z]/g, (c) => {
        const code = c.charCodeAt(0);
        if (code >= 65 && code <= 90) {
            return String.fromCharCode(((code - 65 + 13) % 26) + 65);
        }
        return String.fromCharCode(((code - 97 + 13) % 26) + 97);
    });
}

function levenshtein(a, b) {
    if (a === b) return 0;
    if (!a.length) return b.length;
    if (!b.length) return a.length;
    const la = a.length;
    const lb = b.length;
    const d = Array.from({ length: la + 1 }, () => new Array(lb + 1).fill(0));
    for (let i = 0; i <= la; i++) d[i][0] = i;
    for (let j = 0; j <= lb; j++) d[0][j] = j;
    for (let i = 1; i <= la; i++) {
        for (let j = 1; j <= lb; j++) {
            const cost = a[i - 1] === b[j - 1] ? 0 : 1;
            d[i][j] = Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost);
        }
    }
    return d[la][lb];
}

async function getBaseUrl(ua) {
    if (cachedBaseUrl && Date.now() < cachedBaseUrlExpiry) {
        return cachedBaseUrl;
    }

    let currentUrl = INITIAL_BASE_URL;
    for (let i = 0; i < 10; i++) {
        try {
            const res = await fetch(currentUrl, {
                method: 'HEAD',
                headers: { 'User-Agent': ua },
                redirect: 'manual',
                signal: AbortSignal.timeout(10000),
            });

            if (res.status >= 300 && res.status < 400) {
                const location = res.headers.get('location');
                if (location) {
                    currentUrl = new URL(location, currentUrl).toString();
                    continue;
                }
            }
            break;
        } catch {
            break;
        }
    }

    const parsed = new URL(currentUrl);
    cachedBaseUrl = `${parsed.protocol}//${parsed.host}`;
    cachedBaseUrlExpiry = Date.now() + 3600000;
    return cachedBaseUrl;
}

async function resolveMetadata(sdk, id, isTv) {
    const apiKey = sdk?.tmdbApiKey || process.env.TMDB_API_KEY;
    if (!apiKey) return null;
    const type = isTv ? 'tv' : 'movie';
    try {
        const res = await fetch(`https://api.themoviedb.org/3/${type}/${id}?api_key=${apiKey}`, {
            signal: AbortSignal.timeout(8000),
        });
        if (!res.ok) return null;
        const data = await res.json();
        const title = isTv ? data.name : data.title;
        const releaseDate = isTv ? data.first_air_date : data.release_date;
        const year = releaseDate ? parseInt(releaseDate.split('-')[0], 10) : null;
        return { title, year };
    } catch {
        return null;
    }
}

function sanitizeStreamUrl(rawUrl) {
    try {
        const parsed = new URL(rawUrl);
        parsed.pathname = parsed.pathname.replaceAll('::', '%3A%3A');
        return parsed.toString();
    } catch {
        return rawUrl.replaceAll('::', '%3A%3A');
    }
}

async function validateStreamUrl(url, ua) {
    try {
        const res = await fetch(url, {
            headers: {
                'User-Agent': ua,
                Range: 'bytes=0-1024',
            },
            signal: AbortSignal.timeout(4000),
        });

        const contentType = (res.headers.get('content-type') || '').toLowerCase();
        const isSuccess = (res.status >= 200 && res.status < 300) || res.status === 206;
        const isHtmlOrJsonError = contentType.includes('text/html') || contentType.includes('application/json');

        if (!isSuccess || isHtmlOrJsonError) return false;
        return true;
    } catch {
        return true;
    }
}

async function resolveRedirectUrl(redirectUrl, ua) {
    try {
        const res = await fetch(redirectUrl, {
            headers: { 'User-Agent': ua },
            signal: AbortSignal.timeout(15000),
        });
        if (!res.ok) return null;
        const html = await res.text();

        let nextUrl = '';
        const downloadMatch = html.match(/<a id="download" href="(.*?)"/);
        if (downloadMatch) {
            nextUrl = downloadMatch[1];
        } else {
            const varMatch = html.match(/var url = '(.*?)';/);
            if (varMatch) {
                nextUrl = varMatch[1];
            }
        }

        if (!nextUrl) {
            const match = html.match(/'o','(.*?)'/);
            if (!match) return null;
            const rawData = match[1];
            const step1 = Buffer.from(rawData, 'base64').toString('utf8');
            const step2 = Buffer.from(step1, 'base64').toString('utf8');
            const step3 = rot13(step2);
            const step4 = Buffer.from(step3, 'base64').toString('utf8');
            const data = JSON.parse(step4);
            return Buffer.from(data.o, 'base64').toString('utf8');
        }

        const interRes = await fetch(nextUrl, {
            headers: { 'User-Agent': ua },
            signal: AbortSignal.timeout(15000),
        });
        if (!interRes.ok) return null;
        const interHtml = await interRes.text();

        const finalMatch = interHtml.match(/<a href="([^"]+)"[^>]*class="[^"]*btn-success/);
        if (finalMatch) {
            return finalMatch[1];
        }

        return null;
    } catch {
        return null;
    }
}

async function extractSourceResults(itemHtml, ua) {
    const sizeMatch = itemHtml.match(/([\d.]+ ?[GM]B)/);
    const sizeStr = sizeMatch ? sizeMatch[1] : null;

    const heightMatch = itemHtml.match(/\d{3,}p/);
    const heightStr = heightMatch ? heightMatch[0] : null;

    const fileTitleMatch = itemHtml.match(/class="(?:file-title|episode-file-title)"[^>]*>([^<]+)/);
    const fileTitle = fileTitleMatch ? fileTitleMatch[1].trim() : 'Unknown';

    const displayParts = [fileTitle];
    if (heightStr) displayParts.push(heightStr);
    if (sizeStr) displayParts.push(sizeStr);

    let hubCloudUrl = null;
    let hubDriveUrl = null;

    const linkRegex = /<a\s+[^>]*href="([^"]+)"[^>]*>([\s\S]*?)<\/a>/gi;
    let match;
    while ((match = linkRegex.exec(itemHtml)) !== null) {
        const href = match[1];
        const text = match[2];
        if (text.includes('HubCloud') && !hubCloudUrl) {
            hubCloudUrl = href;
        } else if (text.includes('HubDrive') && !hubDriveUrl) {
            hubDriveUrl = href;
        }
    }

    const redirectUrl = hubCloudUrl || hubDriveUrl;
    if (!redirectUrl) return null;

    const resolvedUrl = await resolveRedirectUrl(redirectUrl, ua);
    if (!resolvedUrl) return null;

    const safeUrl = sanitizeStreamUrl(resolvedUrl);
    const isValid = await validateStreamUrl(safeUrl, ua);
    if (!isValid) return null;

    return {
        url: safeUrl,
        label: displayParts.join(' · '),
        headers: {
            'User-Agent': ua,
        },
    };
}

export async function getStream({ id, s, e, sdk }) {
    const isTv = Boolean(s && e);
    const ua = getUA();

    const meta = await resolveMetadata(sdk, id, isTv);
    if (!meta?.title) return null;

    const baseUrl = await getBaseUrl(ua);
    const searchUrl = `${baseUrl}/?s=${encodeURIComponent(meta.title)}`;

    const searchRes = await fetch(searchUrl, {
        headers: { 'User-Agent': ua },
        signal: AbortSignal.timeout(15000),
    });
    if (!searchRes.ok) return null;
    const searchHtml = await searchRes.text();

    const cardRegex = /<a\s+([^>]*\bclass="[^"]*movie-card[^"]*"[^>]*)>([\s\S]*?)<\/a>/gi;
    const formatFilter = isTv ? 'Series' : 'Movies';
    let targetPageUrl = null;

    let cardMatch;
    while ((cardMatch = cardRegex.exec(searchHtml)) !== null) {
        const attributes = cardMatch[1];
        const inner = cardMatch[2];

        const hrefMatch = attributes.match(/href="([^"]+)"/);
        if (!hrefMatch) continue;
        const href = hrefMatch[1];

        const formatMatches = [...inner.matchAll(/class="movie-card-format"[^>]*>([\s\S]*?)<\//gi)];
        const formatTexts = formatMatches.map(m => m[1].trim());
        if (!formatTexts.some(t => t.includes(formatFilter))) continue;

        if (meta.year != null) {
            const metaMatch = inner.match(/class="movie-card-meta"[^>]*>([\s\S]*?)<\//i);
            const yearMatch = metaMatch ? metaMatch[1].match(/\d{4}/) : null;
            if (yearMatch) {
                const cardYear = parseInt(yearMatch[0], 10);
                if (Math.abs(cardYear - meta.year) > 1) continue;
            }
        }

        const titleMatch = inner.match(/class="movie-card-title"[^>]*>([\s\S]*?)<\//i);
        let cardTitle = titleMatch ? titleMatch[1] : '';
        cardTitle = cardTitle.replace(/\[.*?\]/g, '').trim();

        const diff = levenshtein(cardTitle.toLowerCase(), meta.title.toLowerCase());
        const isMatch = diff < 5 || (cardTitle.toLowerCase().includes(meta.title.toLowerCase()) && diff < 16);
        if (!isMatch) continue;

        targetPageUrl = new URL(href, baseUrl).toString();
        break;
    }

    if (!targetPageUrl) return null;

    const pageRes = await fetch(targetPageUrl, {
        headers: { 'User-Agent': ua },
        signal: AbortSignal.timeout(15000),
    });
    if (!pageRes.ok) return null;
    const pageHtml = await pageRes.text();

    const matchedHtmlBlocks = [];

    if (isTv) {
        const seasonStr = String(s).padStart(2, '0');
        const episodeStr = String(e).padStart(2, '0');

        const epItemRegex = /class="[^"]*episode-item[^"]*"[^>]*>([\s\S]*?)(?=(?:class="[^"]*episode-item"|<\/main>|$))/gi;
        let epMatch;
        while ((epMatch = epItemRegex.exec(pageHtml)) !== null) {
            const epBlock = epMatch[1];
            const titleMatch = epBlock.match(/class="episode-title"[^>]*>([\s\S]*?)<\//i);
            const titleText = titleMatch ? titleMatch[1] : '';
            if (!titleText.includes(`S${seasonStr}`)) continue;

            const dlItemRegex = /class="[^"]*episode-download-item[^"]*"[^>]*>([\s\S]*?)(?=(?:class="[^"]*episode-download-item"|<\/div>\s*<\/div>|$))/gi;
            let dlMatch;
            while ((dlMatch = dlItemRegex.exec(epBlock)) !== null) {
                const dlBlock = dlMatch[1];
                if (dlBlock.includes(`Episode-${episodeStr}`)) {
                    matchedHtmlBlocks.push(dlBlock);
                }
            }
        }
    } else {
        const dlItemRegex = /class="[^"]*download-item[^"]*"[^>]*>([\s\S]*?)(?=(?:class="[^"]*download-item"|<\/main>|$))/gi;
        let dlMatch;
        while ((dlMatch = dlItemRegex.exec(pageHtml)) !== null) {
            matchedHtmlBlocks.push(dlMatch[1]);
        }
    }

    if (matchedHtmlBlocks.length === 0) return null;

    const results = await Promise.all(matchedHtmlBlocks.map((block) => extractSourceResults(block, ua)));
    const allUrls = results.filter(Boolean);

    if (allUrls.length === 0) return null;

    return {
        url: allUrls[0].url,
        allUrls,
        headers: allUrls[0].headers,
    };
}