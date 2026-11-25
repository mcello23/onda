import { test, expect } from '@mobilewright/test';
import { launchFreshOnda, relaunchOnda } from './helpers.js';

test.describe('Favorites & Custom Radio', () => {
  test('favorites a curated station and persists across app relaunch', async ({ app, device, screen }) => {
    await launchFreshOnda(app, device, screen);
    await expect(screen.getByText('Sem favoritos ainda')).toBeVisible();

    // Scroll to the curated World Rock list on Home and favorite the first visible station
    const rockHeader = screen.getByText(/Melhores de Rock do Mundo/);
    await rockHeader.scrollIntoViewIfNeeded();
    await expect(rockHeader).toBeVisible();

    const favButton = screen.getByLabel('Favorite ', { exact: false }).first();
    await favButton.scrollIntoViewIfNeeded();
    await favButton.tap();

    // Scroll back up to Favorites and verify empty-state placeholder is gone + strip visible
    const favHeader = screen.getByText('Seus Favoritos');
    await favHeader.scrollIntoViewIfNeeded({ direction: 'down' });
    await expect(screen.getByText('Sem favoritos ainda')).toBeHidden();
    await expect(screen.getByTestId('home_favorites_strip')).toBeVisible();

    // Collapse favorites strip on Home and verify state persists across relaunch
    await screen.getByTestId('home_favorites_toggle').tap();
    await expect(screen.getByTestId('home_favorites_strip')).toBeHidden();

    // Open dedicated Favorites tab via "See all" button
    await screen.getByTestId('home_favorites_see_all').tap();
    await expect(screen.getByTestId('favorites_search_input')).toBeVisible();

    // Relaunch app without clearing data and verify favorite & collapsed state persisted
    await relaunchOnda(device, screen);
    await expect(screen.getByText('Seus Favoritos')).toBeVisible();
    await expect(screen.getByTestId('home_favorites_strip')).toBeHidden();

    // Expand again and confirm favorite is still present
    await screen.getByTestId('home_favorites_toggle').tap();
    await expect(screen.getByTestId('home_favorites_strip')).toBeVisible();
  });

  test('adds a custom radio station via URL dialog and validates input', async ({ app, device, screen }) => {
    await launchFreshOnda(app, device, screen);

    await screen.getByTestId('home_add_custom').tap();
    await expect(screen.getByText('Adicionar Rádio Manual')).toBeVisible();

    const confirmBtn = screen.getByTestId('custom_radio_confirm');
    await expect(confirmBtn).toBeDisabled();

    await screen.getByTestId('custom_radio_name').fill('Onda Test FM');
    // Still disabled until URL starts with http:// or https://
    await expect(confirmBtn).toBeDisabled();

    await screen.getByTestId('custom_radio_url').fill('https://example.com/live/onda.m3u8');
    await expect(confirmBtn).toBeEnabled();

    await confirmBtn.tap();

    // Custom station is added to favorites and starts playing in mini-player
    await expect(screen.getByText('Adicionar Rádio Manual')).toBeHidden();
    await expect(screen.getByText('Onda Test FM').first()).toBeVisible();
    await expect(screen.getByTestId('mini_player')).toBeVisible();
  });
});
