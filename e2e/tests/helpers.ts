import { expect } from '@mobilewright/test';
import type { App, Device, Screen } from 'mobilewright';

export const ONDA_BUNDLE_ID = 'io.github.mcello23.onda';

/**
 * Resets Onda's SharedPreferences & caches, relaunches cleanly, and waits until
 * the Home screen header is visible.
 */
export async function launchFreshOnda(app: App, device: Device, screen: Screen): Promise<void> {
  await app.clearData();
  await device.launchApp(ONDA_BUNDLE_ID);
  await expect(screen.getByText('Onda', { exact: true })).toBeVisible({ timeout: 25_000 });
}

/**
 * Terminates and relaunches Onda without clearing SharedPreferences, useful
 * for verifying persistence of favorites and settings across sessions.
 */
export async function relaunchOnda(device: Device, screen: Screen): Promise<void> {
  await device.terminateApp(ONDA_BUNDLE_ID);
  await device.launchApp(ONDA_BUNDLE_ID);
  await expect(screen.getByText('Onda', { exact: true })).toBeVisible({ timeout: 25_000 });
}
