import { execFileSync } from 'node:child_process';
import { cp, mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';

const publishPlaybookPath = path.resolve(process.argv[2] ?? 'target/antora-publish-playbook.yml');
const siteDirectory = path.resolve(process.argv[3] ?? 'target/site');
const componentName = process.argv[4] ?? 'data-meilisearch';
const workDirectory = path.resolve('target/antora-exact-version-site');
const exactPlaybookPath = path.join(workDirectory, 'antora-publish-playbook.yml');
const exactSiteDirectory = path.join(workDirectory, 'site');

async function readLatestStableVersion() {
  const manifest = JSON.parse(await readFile(path.join(siteDirectory, 'site-manifest.json'), 'utf8'));
  const component = manifest.components?.[componentName];
  const latestVersion = component?.latest;
  const latestVersionEntry = latestVersion ? component.versions?.[latestVersion] : null;

  if (!latestVersion || !latestVersionEntry || latestVersionEntry.prerelease) {
    throw new Error(`Cannot resolve latest stable version for ${componentName}`);
  }

  return latestVersion;
}

async function writeExactVersionPlaybook() {
  const publishPlaybook = await readFile(publishPlaybookPath, 'utf8');
  const exactVersionPlaybook = publishPlaybook.replace(
    'latest_version_segment_strategy: redirect:to',
    'latest_version_segment_strategy: redirect:from'
  );

  await mkdir(workDirectory, { recursive: true });
  await writeFile(exactPlaybookPath, exactVersionPlaybook, 'utf8');
}

async function copyLatestStableVersion(latestVersion) {
  const sourceDirectory = path.join(exactSiteDirectory, latestVersion);
  const targetDirectory = path.join(siteDirectory, latestVersion);

  await rm(targetDirectory, { recursive: true, force: true });
  await cp(sourceDirectory, targetDirectory, { recursive: true });
}

async function main() {
  const latestVersion = await readLatestStableVersion();

  await rm(workDirectory, { recursive: true, force: true });
  await writeExactVersionPlaybook();

  execFileSync('npx', ['--offline', 'antora', '--to-dir', exactSiteDirectory, exactPlaybookPath], {
    stdio: 'inherit'
  });

  await copyLatestStableVersion(latestVersion);
  await rm(workDirectory, { recursive: true, force: true });
}

await main();
