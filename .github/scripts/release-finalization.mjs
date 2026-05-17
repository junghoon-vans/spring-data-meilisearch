import { readFileSync, writeFileSync } from 'node:fs';
import { pathToFileURL } from 'node:url';

const SEMVER_PATTERN = /^\d+\.\d+\.\d+$/;

const VERSION_PATTERNS = {
	project: /(<artifactId>\s*spring-data-meilisearch\s*<\/artifactId>\s*<version>)([^<]+)(<\/version>)/,
	parent: /(<parent>[\s\S]*?<artifactId>\s*spring-data-parent\s*<\/artifactId>[\s\S]*?<version>)([^<]+)(<\/version>[\s\S]*?<\/parent>)/,
	springdataCommons: /(<springdata\.commons>)([^<]+)(<\/springdata\.commons>)/,
};

export function readPomVersions(pomXml) {
	return {
		projectVersion: readRequiredVersion(pomXml, VERSION_PATTERNS.project, 'project version'),
		parentVersion: readRequiredVersion(pomXml, VERSION_PATTERNS.parent, 'spring-data-parent version'),
		springdataCommonsVersion: readRequiredVersion(pomXml, VERSION_PATTERNS.springdataCommons,
			'springdata.commons version'),
	};
}

export function detectReleaseState(pomXml) {
	const versions = readPomVersions(pomXml);
	const checks = {
		projectVersion: isReleaseVersion(versions.projectVersion),
		parentVersion: isReleaseVersion(versions.parentVersion),
		springdataCommonsVersion: isReleaseVersion(versions.springdataCommonsVersion),
	};
	const release = Object.values(checks).every(Boolean);

	return {
		release,
		versions,
		checks,
		reason: release ? `Release POM for ${versions.projectVersion}.` : buildNonReleaseReason(versions, checks),
	};
}

export function assertReleaseCommitSubject(version, subject) {
	const expected = `Release ${version}.`;

	if (subject !== expected) {
		throw new Error(`Release commit subject must be "${expected}" but was "${subject}".`);
	}
}

export function rewritePomToNextSnapshot(pomXml) {
	const state = detectReleaseState(pomXml);

	if (!state.release) {
		throw new Error(`Cannot rewrite a non-release POM. ${state.reason}`);
	}

	const nextVersions = {
		projectVersion: `${incrementPatch(state.versions.projectVersion)}-SNAPSHOT`,
		parentVersion: `${state.versions.parentVersion}-SNAPSHOT`,
		springdataCommonsVersion: `${state.versions.springdataCommonsVersion}-SNAPSHOT`,
	};

	let rewritten = replaceVersion(pomXml, VERSION_PATTERNS.project, nextVersions.projectVersion, 'project version');
	rewritten = replaceVersion(rewritten, VERSION_PATTERNS.parent, nextVersions.parentVersion, 'spring-data-parent version');
	rewritten = replaceVersion(rewritten, VERSION_PATTERNS.springdataCommons, nextVersions.springdataCommonsVersion,
		'springdata.commons version');

	return {
		pomXml: rewritten,
		nextVersions,
	};
}

export function incrementPatch(version) {
	if (!SEMVER_PATTERN.test(version)) {
		throw new Error(`Version must be semver x.y.z but was "${version}".`);
	}

	const parts = version.split('.').map((part) => Number.parseInt(part, 10));
	parts[2] += 1;

	return parts.join('.');
}

function readRequiredVersion(pomXml, pattern, name) {
	const match = pattern.exec(pomXml);

	if (!match) {
		throw new Error(`Could not find ${name} in pom.xml.`);
	}

	return match[2].trim();
}

function isReleaseVersion(version) {
	return SEMVER_PATTERN.test(version) && !version.endsWith('-SNAPSHOT');
}

function buildNonReleaseReason(versions, checks) {
	const invalid = Object.entries(checks)
		.filter(([, valid]) => !valid)
		.map(([name]) => `${name}=${versions[name]}`);

	return `Not a release POM; expected semver release versions for all release-managed entries, invalid: ${invalid.join(', ')}.`;
}

function replaceVersion(pomXml, pattern, version, name) {
	let replaced = false;
	const rewritten = pomXml.replace(pattern, (_match, before, _current, after) => {
		replaced = true;
		return `${before}${version}${after}`;
	});

	if (!replaced) {
		throw new Error(`Could not rewrite ${name} in pom.xml.`);
	}

	return rewritten;
}

function writeOutput(name, value) {
	console.log(`${name}=${value}`);
}

function inspectPom(pomPath, subject) {
	const pomXml = readFileSync(pomPath, 'utf8');
	const state = detectReleaseState(pomXml);

	writeOutput('release', String(state.release));
	writeOutput('version', state.versions.projectVersion);
	writeOutput('parent_version', state.versions.parentVersion);
	writeOutput('springdata_commons_version', state.versions.springdataCommonsVersion);
	writeOutput('reason', state.reason);

	if (!state.release) {
		return;
	}

	assertReleaseCommitSubject(state.versions.projectVersion, subject);
	writeOutput('tag', `v${state.versions.projectVersion}`);
	writeOutput('next_version', `${incrementPatch(state.versions.projectVersion)}-SNAPSHOT`);
}

function rewritePom(pomPath) {
	const pomXml = readFileSync(pomPath, 'utf8');
	const result = rewritePomToNextSnapshot(pomXml);
	writeFileSync(pomPath, result.pomXml, 'utf8');

	writeOutput('next_version', result.nextVersions.projectVersion);
	writeOutput('next_parent_version', result.nextVersions.parentVersion);
	writeOutput('next_springdata_commons_version', result.nextVersions.springdataCommonsVersion);
}

function printUsage() {
	console.error('Usage: node .github/scripts/release-finalization.mjs inspect <pom.xml> <commit-subject>');
	console.error('   or: node .github/scripts/release-finalization.mjs rewrite <pom.xml>');
}

function main(argv) {
	const [command, pomPath, ...args] = argv;

	if (!command || !pomPath) {
		printUsage();
		return 1;
	}

	if (command === 'inspect') {
		inspectPom(pomPath, args.join(' '));
		return 0;
	}

	if (command === 'rewrite') {
		rewritePom(pomPath);
		return 0;
	}

	printUsage();
	return 1;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
	try {
		process.exitCode = main(process.argv.slice(2));
	} catch (error) {
		console.error(error.message);
		process.exitCode = 1;
	}
}
