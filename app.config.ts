import { ConfigContext, ExpoConfig } from "expo/config";
import "ts-node/register";

export default ({ config }: ConfigContext): ExpoConfig => ({
  ...config,
  name: "ExpoDynamicYieldExample",
  slug: "ExpoDynamicYieldExample",
  version: "1.0.0",
  orientation: "portrait",
  icon: "./assets/images/icon.png",
  scheme: "expodynamicyieldexample",
  userInterfaceStyle: "automatic",
  newArchEnabled: true,
  ios: {
    supportsTablet: true,
    bundleIdentifier: "com.anthlasserre.ExpoDynamicYieldExample",
  },
  android: {
    adaptiveIcon: {
      foregroundImage: "./assets/images/adaptive-icon.png",
      backgroundColor: "#ffffff",
    },
    edgeToEdgeEnabled: true,
    package: "com.anthlasserre.ExpoDynamicYieldExample",
  },
  web: {
    bundler: "metro",
    output: "static",
    favicon: "./assets/images/favicon.png",
  },
  plugins: [
    "expo-router",
    [
      "expo-splash-screen",
      {
        image: "./assets/images/splash-icon.png",
        imageWidth: 200,
        resizeMode: "contain",
        backgroundColor: "#ffffff",
      },
    ],
    "./plugins/dynamic-yield/index.js",
    [
      "expo-build-properties",
      {
        android: {
          extraMavenRepos: [
            {
              url: "https://maven.pkg.github.com/DynamicYield/Dynamic-Yield-Mobile-SDK-Kotlin",
              credentials: {
                username: process.env.GITHUB_USERNAME,
                password: process.env.GITHUB_TOKEN,
              },
            },
          ],
        },
        ios: {
          useFrameworks: "dynamic",
        },
      },
    ],
  ],
  experiments: {
    typedRoutes: true,
  },
});
