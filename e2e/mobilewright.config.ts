import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { defineConfig } from 'mobilewright';

const here = dirname(fileURLToPath(import.meta.url));
const apkPath = process.env['ONDA_APK_PATH']
  ? resolve(process.env['ONDA_APK_PATH'])
  : resolve(here, '../app/build/outputs/apk/debug/app-debug.apk');

export default defineConfig({
  platform: 'android',
  bundleId: 'io.github.mcello23.onda',
  installApps: process.env['ONDA_SKIP_INSTALL'] === '1' ? undefined : apkPath,
  testDir: './tests',
  testMatch: '**/*.test.ts',
  timeout: 120_000,
  retries: process.env['CI'] ? 1 : 0,
  workers: 1,
  autoAppLaunch: true,
  viewTree: 'on-failure',
  use: {
    animations: 'off',
    screenshot: 'only-on-failure',
    actionTimeout: 10_000,
    appLaunchTimeout: 30_000,
    installTimeout: 90_000,
  },
  expect: {
    timeout: 15_000,
  },
  reporter: [['list'], ['html', { open: 'never' }]],
  ...(process.env['MOBILEWRIGHT_DEVICE_ID'] ? { deviceId: process.env['MOBILEWRIGHT_DEVICE_ID'] } : {}),
});
