import { execFileSync } from 'node:child_process';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';

const sourcePlaybookPath = path.resolve(process.argv[2] ?? 'antora-playbook.yml');
const outputPlaybookPath = path.resolve(process.argv[3] ?? 'target/antora-publish-playbook.yml');
const targetAntoraDescriptorPath = path.resolve('target/classes/antora.yml');
const localUrlMarker = '    - url: .';
const localTagsMarker = 'tags:\n        - v*.*.*';

function readHeadReleaseTags() {
  const output = execFileSync('git', ['tag', '--points-at', 'HEAD', '--list', 'v*.*.*'], {
    encoding: 'utf8'
  });

  return output
    .split(/\r?\n/u)
    .map((line) => line.trim())
    .filter((line) => line.length > 0);
}

async function readCurrentComponentVersionTag() {
  try {
    const antoraDescriptor = await readFile(targetAntoraDescriptorPath, 'utf8');
    const match = antoraDescriptor.match(/^version:\s*['"]?([^'"\s]+)['"]?\s*$/mu);

    return match ? `v${match[1]}` : null;
  } catch (error) {
    if (error && error.code === 'ENOENT') {
      return null;
    }

    throw error;
  }
}

function rewriteLocalSourceUrl(playbookContents) {
  const sourceDirectory = path.dirname(sourcePlaybookPath);

  return playbookContents.replace(localUrlMarker, `    - url: ${sourceDirectory}`);
}

function injectExclusions(playbookContents, exclusions) {
  if (exclusions.length === 0) {
    return playbookContents;
  }

  const exclusionLines = exclusions.map((tag) => `\n        - '!${tag}'`).join('');

  return playbookContents.replace(localTagsMarker, `${localTagsMarker}${exclusionLines}`);
}

async function main() {
  const [playbookContents, headReleaseTags, currentComponentVersionTag] = await Promise.all([
    readFile(sourcePlaybookPath, 'utf8'),
    Promise.resolve(readHeadReleaseTags()),
    readCurrentComponentVersionTag()
  ]);

  const exclusions = [...new Set([
    ...headReleaseTags,
    ...(currentComponentVersionTag ? [currentComponentVersionTag] : [])
  ])];

  const rewrittenPlaybook = injectExclusions(rewriteLocalSourceUrl(playbookContents), exclusions);

  await mkdir(path.dirname(outputPlaybookPath), { recursive: true });
  await writeFile(outputPlaybookPath, rewrittenPlaybook, 'utf8');
}

await main();
