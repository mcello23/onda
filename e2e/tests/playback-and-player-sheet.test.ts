import { test, expect } from '@mobilewright/test';
import { launchFreshOnda } from './helpers.js';

test.describe('Playback Mini-Player & Now Playing Sheet', () => {
  test('starts station, opens Now Playing sheet, and opens Sleep Timer & Equalizer', async ({ app, device, screen }) => {
    await launchFreshOnda(app, device, screen);

    // Tap first Spanish curated station card ("Rock FM") in Top Stations carousel
    const rockFm = screen.getByText('Rock FM', { exact: true }).first();
    await expect(rockFm).toBeVisible();
    await rockFm.tap();

    // Floating mini-player appears
    const miniPlayer = screen.getByTestId('mini_player');
    await expect(miniPlayer).toBeVisible({ timeout: 20_000 });

    // Open full-screen Now Playing modal sheet
    await miniPlayer.tap();
    await expect(screen.getByTestId('now_playing_sheet')).toBeVisible();
    await expect(screen.getByText(/AO VIVO/)).toBeVisible();

    // Toggle favorite from Now Playing quick action bar
    await screen.getByText('Favoritar', { exact: true }).tap();
    await expect(screen.getByText('Favorito', { exact: true })).toBeVisible();

    // Open Sleep Timer dialog and select 15 minutes
    await screen.getByText('Timer', { exact: true }).tap();
    await expect(screen.getByText('Temporizador para dormir')).toBeVisible();
    await screen.getByText('15 minutos', { exact: true }).tap();
    await expect(screen.getByText('Temporizador para dormir')).toBeHidden();

    // Open Equalizer sheet from Now Playing
    await screen.getByText('EQ', { exact: true }).tap();
    await expect(screen.getByText('Áudio & Equalizador')).toBeVisible();
    await screen.goBack();

    // Stop playback via main player toggle button and verify state switches to Play
    await expect(screen.getByLabel('Parar', { exact: true })).toBeVisible({ timeout: 15_000 });
    await screen.getByTestId('player_toggle_btn').tap();
    await expect(screen.getByLabel('Tocar', { exact: true })).toBeVisible({ timeout: 15_000 });
  });
});
