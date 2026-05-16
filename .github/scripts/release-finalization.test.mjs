import assert from 'node:assert/strict';
import test from 'node:test';

import {
	assertReleaseCommitSubject,
	detectReleaseState,
	incrementPatch,
	readPomVersions,
	rewritePomToNextSnapshot,
} from './release-finalization.mjs';

const releasePom = `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>io.vanslog</groupId>
    <artifactId>spring-data-meilisearch</artifactId>
    <version>0.8.1</version>
    <parent>
        <groupId>org.springframework.data.build</groupId>
        <artifactId>spring-data-parent</artifactId>
        <version>3.3.13</version>
    </parent>
    <properties>
        <springdata.commons>3.3.13</springdata.commons>
    </properties>
</project>
`;

test('reads release-managed versions from pom.xml', () => {
	assert.deepEqual(readPomVersions(releasePom), {
		projectVersion: '0.8.1',
		parentVersion: '3.3.13',
		springdataCommonsVersion: '3.3.13',
	});
});

test('detects release pom only when all release-managed versions are semver releases', () => {
	const state = detectReleaseState(releasePom);

	assert.equal(state.release, true);
	assert.equal(state.reason, 'Release POM for 0.8.1.');
});

test('does not treat snapshot pom as a release', () => {
	const snapshotPom = releasePom
		.replace('<version>0.8.1</version>', '<version>0.8.2-SNAPSHOT</version>')
		.replace('<version>3.3.13</version>', '<version>3.3.13-SNAPSHOT</version>')
		.replace('<springdata.commons>3.3.13</springdata.commons>',
			'<springdata.commons>3.3.13-SNAPSHOT</springdata.commons>');
	const state = detectReleaseState(snapshotPom);

	assert.equal(state.release, false);
	assert.match(state.reason, /projectVersion=0\.8\.2-SNAPSHOT/);
	assert.match(state.reason, /parentVersion=3\.3\.13-SNAPSHOT/);
	assert.match(state.reason, /springdataCommonsVersion=3\.3\.13-SNAPSHOT/);
});

test('does not treat non-semver pom as a release', () => {
	const nonSemverPom = releasePom.replace('<version>0.8.1</version>', '<version>0.8</version>');

	assert.equal(detectReleaseState(nonSemverPom).release, false);
});

test('requires exact release commit subject', () => {
	assert.doesNotThrow(() => assertReleaseCommitSubject('0.8.1', 'Release 0.8.1.'));
	assert.throws(() => assertReleaseCommitSubject('0.8.1', 'Release 0.8.1'),
		/Release commit subject must be "Release 0\.8\.1\."/);
});

test('increments only the project patch for the next development iteration', () => {
	assert.equal(incrementPatch('0.8.1'), '0.8.2');
});

test('rewrites release pom to the next snapshot pom', () => {
	const result = rewritePomToNextSnapshot(releasePom);

	assert.deepEqual(result.nextVersions, {
		projectVersion: '0.8.2-SNAPSHOT',
		parentVersion: '3.3.13-SNAPSHOT',
		springdataCommonsVersion: '3.3.13-SNAPSHOT',
	});
	assert.match(result.pomXml, /<version>0\.8\.2-SNAPSHOT<\/version>/);
	assert.match(result.pomXml, /<artifactId>spring-data-parent<\/artifactId>\s*<version>3\.3\.13-SNAPSHOT<\/version>/);
	assert.match(result.pomXml, /<springdata\.commons>3\.3\.13-SNAPSHOT<\/springdata\.commons>/);
});

test('refuses to rewrite non-release pom', () => {
	const snapshotPom = releasePom.replace('<version>0.8.1</version>', '<version>0.8.2-SNAPSHOT</version>');

	assert.throws(() => rewritePomToNextSnapshot(snapshotPom), /Cannot rewrite a non-release POM/);
});
