'use strict'

const fs = require('fs')
const path = require('path')

const target = path.join(
  __dirname,
  '..',
  'node_modules',
  '@springio',
  'antora-extensions',
  'lib',
  'cache-scandir',
  'cache-scandir.js'
)

if (!fs.existsSync(target)) {
  console.log(`[postinstall] skip: ${target} not found yet`)
  process.exit(0)
}

const original = fs.readFileSync(target, 'utf8')
const before = '    fs.mkdirSync(dest)'
const after = '    fs.mkdirSync(dest, { recursive: true })'

if (original.includes(after)) {
  console.log('[postinstall] antora cache-scandir patch already applied')
  process.exit(0)
}

if (!original.includes(before)) {
  console.error('[postinstall] unexpected cache-scandir.js contents; patch not applied')
  process.exit(1)
}

fs.writeFileSync(target, original.replace(before, after))
console.log('[postinstall] patched @springio/antora-extensions cache-scandir mkdirSync to be recursive')
