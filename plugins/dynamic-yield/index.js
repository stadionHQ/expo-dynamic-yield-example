const {
  createRunOncePlugin,
  withAndroidManifest,
} = require("expo/config-plugins");

const fs = require("fs");
const path = require("path");

const withDynamicYieldAndroid = (config) => {
  // Add internet permission to AndroidManifest.xml
  config = withAndroidManifest(config, async (newConfig) => {
    // Check if the internet permission already exists
    const internetPermissionExists = newConfig.modResults.manifest[
      "uses-permission"
    ]?.some(
      (permission) =>
        permission["$"]["android:name"] === "android.permission.INTERNET"
    );

    if (!internetPermissionExists) {
      // Add internet permission
      newConfig.modResults.manifest["uses-permission"] = [
        ...(newConfig.modResults.manifest["uses-permission"] || []),
        {
          $: {
            "android:name": "android.permission.INTERNET",
          },
        },
      ];
    }
    return newConfig;
  });
  return config;
};

const packageJson = fs.readFileSync(
  path.join(process.cwd(), "package.json"),
  "utf8"
);
const packageJsonObject = JSON.parse(packageJson);
const version = packageJsonObject.version;

module.exports = createRunOncePlugin(
  (config) => {
    return withDynamicYieldAndroid(config);
  },
  "dynamic-yield",
  version
);
