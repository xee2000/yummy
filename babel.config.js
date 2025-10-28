module.exports = {
  presets: ['module:@react-native/babel-preset'], // Ensure this is correct
  plugins: [
    [
      'module:react-native-dotenv',
      {
        moduleName: '@env',
        path: '.env', // Ensure your .env file is in the root directory
        blocklist: null,
        allowlist: null,
        safe: false,
        allowUndefined: true,
      },
    ],
  ],
};