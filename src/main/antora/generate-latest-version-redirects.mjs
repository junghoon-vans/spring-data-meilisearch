import fs from 'node:fs/promises';
import path from 'node:path';

const siteDir = process.argv[2] ?? 'target/site';
const siteRoot = path.resolve(siteDir);
const manifestPath = path.join(siteRoot, 'site-manifest.json');
const manifest = JSON.parse(await fs.readFile(manifestPath, 'utf8'));

for (const component of Object.values(manifest.components ?? {})) {
  const latestVersion = component.versions?.[component.latest];

  if (!latestVersion || latestVersion.prerelease) {
    continue;
  }

  const versionSegment = getVersionSegment(component.latest);

  if (!versionSegment) {
    continue;
  }

  for (const page of latestVersion.pages ?? []) {
    if (!page.url?.startsWith('/')) {
      continue;
    }

    const aliasUrl = `/${versionSegment}${page.url}`;
    const aliasPath = resolveSitePath(aliasUrl);
    const targetPath = resolveSitePath(page.url);

    if (!aliasPath || !targetPath) {
      continue;
    }

    const relativeTarget = path.relative(path.dirname(aliasPath), targetPath).replaceAll(path.sep, '/');

    await fs.mkdir(path.dirname(aliasPath), { recursive: true });
    await fs.writeFile(aliasPath, redirectPage(relativeTarget), 'utf8');
  }
}

function getVersionSegment(version) {
  const match = version.match(/^(\d+\.\d+)/);

  return match?.[1];
}

function resolveSitePath(urlPath) {
  const resolvedPath = path.resolve(siteRoot, urlPath.slice(1));
  const relativePath = path.relative(siteRoot, resolvedPath);

  if (relativePath.startsWith('..') || path.isAbsolute(relativePath)) {
    return undefined;
  }

  return resolvedPath;
}

function redirectPage(target) {
  const escapedTarget = escapeHtml(target);

  return `<!DOCTYPE html>
<meta charset="utf-8">
<script>location=${scriptString(target)}</script>
<meta http-equiv="refresh" content="0; url=${escapedTarget}">
<meta name="robots" content="noindex">
<title>Redirect Notice</title>
<h1>Redirect Notice</h1>
<p>The page you requested has been relocated to <a href="${escapedTarget}">${escapedTarget}</a>.</p>
`;
}

function escapeHtml(value) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('"', '&quot;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;');
}

function scriptString(value) {
  return JSON.stringify(value).replace(/[<>&\u2028\u2029]/g, (character) => {
    switch (character) {
      case '<':
        return '\\u003C';
      case '>':
        return '\\u003E';
      case '&':
        return '\\u0026';
      case '\u2028':
        return '\\u2028';
      case '\u2029':
        return '\\u2029';
      default:
        return character;
    }
  });
}
