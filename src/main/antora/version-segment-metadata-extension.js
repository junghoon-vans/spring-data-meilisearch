'use strict';

module.exports.register = function ({ config = {} }) {
  this.once('contentClassified', ({ contentCatalog }) => {
    const rootComponentName = config.rootComponentName;

    for (const component of contentCatalog.getComponents()) {
      for (const componentVersion of component.versions) {
        componentVersion.versionSegment = getVersionSegment(component, componentVersion, rootComponentName);
      }
    }
  });
};

function getVersionSegment(component, componentVersion, rootComponentName) {
  const activeVersionSegment = componentVersion.activeVersionSegment;

  if (activeVersionSegment) {
    return activeVersionSegment;
  }
  if (component.name !== rootComponentName) {
    return component.name;
  }

  return componentVersion.version;
}
