const { test, expect } = require('@playwright/test');

async function registerUser(page, username, password) {
  await page.goto('/login');
  await page.getByRole('tab', { name: 'Регистрация' }).click();
  await page.locator('#regUsername').fill(username);
  await page.locator('#regPassword').fill(password);
  await page.getByRole('button', { name: 'Зарегистрироваться' }).click();
  await expect(page.getByText('Регистрация прошла успешно')).toBeVisible();
}

test.describe('Authentication', () => {
  test('user can register a new account', async ({ page }) => {
    const username = `user_reg_${Date.now()}`;
    await registerUser(page, username, 'password123');
  });

  test('registration auto-fills login form and user can login', async ({ page }) => {
    const username = `user_login_${Date.now()}`;
    const password = 'securepass';

    await registerUser(page, username, password);

    // After registration, login form is pre-filled
    await page.getByRole('tab', { name: 'Вход по паролю' }).click();
    await page.getByRole('button', { name: 'Войти' }).click();

    await expect(page).toHaveURL('/transactions');
    await expect(page.getByText('Нет транзакций для отображения')).toBeVisible();
  });

  test('login with username and password navigates to transactions', async ({ page }) => {
    const username = `user_pw_${Date.now()}`;
    const password = 'mypassword';

    await registerUser(page, username, password);

    await page.goto('/login');
    await page.locator('#loginUsername').fill(username);
    await page.locator('#loginPassword').fill(password);
    await page.getByRole('button', { name: 'Войти' }).click();

    await expect(page).toHaveURL('/transactions');
  });

  test('wrong password shows error', async ({ page }) => {
    await page.goto('/login');
    await page.locator('#loginUsername').fill('nonexistent_xyz_user');
    await page.locator('#loginPassword').fill('wrongpassword');
    await page.getByRole('button', { name: 'Войти' }).click();

    await expect(page.locator('.alert-danger')).toBeVisible();
    await expect(page).toHaveURL('/login');
  });

  test('duplicate registration shows error', async ({ page }) => {
    const username = `user_dup_${Date.now()}`;
    await registerUser(page, username, 'pass1');

    // Try registering again
    await page.goto('/login');
    await page.getByRole('tab', { name: 'Регистрация' }).click();
    await page.locator('#regUsername').fill(username);
    await page.locator('#regPassword').fill('pass2');
    await page.getByRole('button', { name: 'Зарегистрироваться' }).click();

    await expect(page.locator('.alert-danger')).toBeVisible();
  });

  test('unauthenticated user is redirected from protected routes', async ({ page }) => {
    await page.goto('/transactions');
    await expect(page).toHaveURL('/login');
  });

  test('navbar shows logout button when logged in', async ({ page }) => {
    const username = `user_nav_${Date.now()}`;
    await registerUser(page, username, 'pass');
    await page.getByRole('tab', { name: 'Вход по паролю' }).click();
    await page.getByRole('button', { name: 'Войти' }).click();
    await expect(page).toHaveURL('/transactions');

    await expect(page.getByRole('button', { name: 'Выйти' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Транзакции' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Долги' })).toBeVisible();
  });

  test('logout redirects to login page', async ({ page }) => {
    const username = `user_logout_${Date.now()}`;
    await registerUser(page, username, 'pass');
    await page.getByRole('tab', { name: 'Вход по паролю' }).click();
    await page.getByRole('button', { name: 'Войти' }).click();
    await expect(page).toHaveURL('/transactions');

    await page.getByRole('button', { name: 'Выйти' }).click();
    await expect(page).toHaveURL('/login');
  });
});
