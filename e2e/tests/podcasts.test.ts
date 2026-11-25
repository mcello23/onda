import { test, expect } from '@mobilewright/test';
import { launchFreshOnda } from './helpers.js';

test.describe('Podcasts Tab', () => {
  test('switches across Discover, Following, and Offline sub-tabs', async ({ app, device, screen }) => {
    await launchFreshOnda(app, device, screen);

    await screen.getByTestId('nav_podcasts').tap();
    await expect(screen.getByText('Descobrir')).toBeVisible();
    await expect(screen.getByText('Em Destaque')).toBeVisible();

    // Empty Following state
    await screen.getByText('Seguindo (0)').tap();
    await expect(screen.getByText(/Você ainda não segue nenhum podcast/)).toBeVisible();

    // Empty Offline state
    await screen.getByText('Baixados (0)').tap();
    await expect(screen.getByText('Nenhum episódio baixado para ouvir offline.')).toBeVisible();

    // Return to Discover
    await screen.getByText('Descobrir').tap();
    await expect(screen.getByText('Em Destaque')).toBeVisible();
  });
});
