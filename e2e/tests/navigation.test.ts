import { test, expect } from '@mobilewright/test';
import { launchFreshOnda } from './helpers.js';

test.describe('Bottom Navigation & Core Screens', () => {
  test('launches Home and navigates across all 5 tabs', async ({ app, device, screen }) => {
    await launchFreshOnda(app, device, screen);

    // Home tab (PT-BR default)
    await expect(screen.getByText('Seus Favoritos')).toBeVisible();
    await expect(screen.getByText('Sem favoritos ainda')).toBeVisible();
    await expect(screen.getByText(/Top Estações/)).toBeVisible();

    // Favorites tab (dedicated space)
    await screen.getByTestId('nav_favorites').tap();
    await expect(screen.getByText('Nenhum favorito salvo')).toBeVisible();
    await expect(screen.getByTestId('favorites_empty_explore')).toBeVisible();

    // Explore tab
    await screen.getByTestId('nav_explore').tap();
    await expect(screen.getByTestId('explore_search_input')).toBeVisible();
    await expect(screen.getByText('Rádios por Gênero Mundial')).toBeVisible();

    // Podcasts tab
    await screen.getByTestId('nav_podcasts').tap();
    await expect(screen.getByText('Descobrir')).toBeVisible();
    await expect(screen.getByText('Seguindo (0)')).toBeVisible();
    await expect(screen.getByText('Baixados (0)')).toBeVisible();

    // Settings tab
    await screen.getByTestId('nav_settings').tap();
    await expect(screen.getByText(/Aparência/)).toBeVisible();
    await expect(screen.getByText('Idioma do Aplicativo')).toBeVisible();

    // Back to Home tab
    await screen.getByTestId('nav_home').tap();
    await expect(screen.getByText('Seus Favoritos')).toBeVisible();
  });
});
