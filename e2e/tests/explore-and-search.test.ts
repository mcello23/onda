import { test, expect } from '@mobilewright/test';
import { launchFreshOnda } from './helpers.js';

test.describe('Explore, Country Switching & Search', () => {
  test('switches country on Home and filters curated stations in Explore', async ({ app, device, screen }) => {
    await launchFreshOnda(app, device, screen);

    // Default country is Spain (ES); switch to Brazil via chip
    await expect(screen.getByText(/Top Estações · Espanha/)).toBeVisible();
    await screen.getByText(/Brasil/).first().tap();
    await expect(screen.getByText(/Top Estações · Brasil/)).toBeVisible();

    // Switch to Explore tab and search a curated Brazilian station
    await screen.getByTestId('nav_explore').tap();
    const searchInput = screen.getByTestId('explore_search_input');
    await searchInput.fill('89 FM');

    await expect(screen.getByText(/89 FM A Rádio Rock/)).toBeVisible({ timeout: 20_000 });

    // Clear search and browse World Rock genre card
    await screen.getByTestId('explore_search_clear').tap();
    await expect(screen.getByText('Rádios por Gênero Mundial')).toBeVisible();

    await screen.getByText('Rock Mundial').tap();
    await expect(screen.getByText(/Melhores rádios · Rock Mundial/)).toBeVisible({ timeout: 20_000 });

    // Return from genre view via Back button
    await screen.getByLabel('Voltar').tap();
    await expect(screen.getByText('Rádios por Gênero Mundial')).toBeVisible();
  });

  test('opens Country Picker sheet and selects Portugal', async ({ app, device, screen }) => {
    await launchFreshOnda(app, device, screen);

    await screen.getByTestId('nav_explore').tap();
    await screen.getByText('Trocar').tap();
    await expect(screen.getByText('Escolher País')).toBeVisible();

    await screen.getByTestId('country_search_input').fill('Portugal');
    await screen.getByTestId('country_item_PT').tap();

    await expect(screen.getByText('Escolher País')).toBeHidden();
    await expect(screen.getByText(/País ativo: (Portugal|PT)/)).toBeVisible();
  });
});
