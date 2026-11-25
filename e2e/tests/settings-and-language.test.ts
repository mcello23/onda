import { test, expect } from '@mobilewright/test';
import { launchFreshOnda, relaunchOnda } from './helpers.js';

test.describe('Settings, Language & Equalizer', () => {
  test('switches language to English, adjusts theme/audio, and persists across relaunch', async ({ app, device, screen }) => {
    await launchFreshOnda(app, device, screen);

    await screen.getByTestId('nav_settings').tap();
    await expect(screen.getByText('Idioma do Aplicativo')).toBeVisible();

    // Switch to Dark theme
    await screen.getByText('Escuro', { exact: true }).tap();

    // Switch language to English
    const englishChip = screen.getByText(/English/);
    await englishChip.scrollIntoViewIfNeeded();
    await englishChip.tap();

    // UI immediately updates to English
    await expect(screen.getByText('App Language')).toBeVisible();
    await expect(screen.getByText('Settings', { exact: true }).first()).toBeVisible();

    // Scroll down to Streaming & Audio Quality and open Equalizer
    const eqRow = screen.getByText('Equalizer', { exact: true });
    await eqRow.scrollIntoViewIfNeeded();
    await expect(screen.getByText('Stream Quality')).toBeVisible();
    await screen.getByText('Data Saver', { exact: true }).tap();
    await screen.getByText('Fast', { exact: true }).tap();

    await eqRow.tap();
    await expect(screen.getByText('Audio & Equalizer')).toBeVisible();
    await screen.getByText('Rock', { exact: true }).tap();

    // Dismiss Equalizer sheet and verify Home tab is now in English
    await screen.goBack();
    await expect(screen.getByText('Audio & Equalizer')).toBeHidden();

    await screen.getByTestId('nav_home').tap();
    await expect(screen.getByText('Your Favorites')).toBeVisible();

    // Verify language setting persists after full restart
    await relaunchOnda(device, screen);
    await expect(screen.getByText('Your Favorites')).toBeVisible();
  });
});
