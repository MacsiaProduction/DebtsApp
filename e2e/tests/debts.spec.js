const { test, expect } = require('@playwright/test');

async function registerAndLogin(page, username, password) {
  await page.goto('/login');
  await page.getByRole('tab', { name: 'Регистрация' }).click();
  await page.locator('#regUsername').fill(username);
  await page.locator('#regPassword').fill(password);
  await page.getByRole('button', { name: 'Зарегистрироваться' }).click();
  await expect(page.getByText('Регистрация прошла успешно')).toBeVisible();
  await page.getByRole('tab', { name: 'Вход по паролю' }).click();
  await page.getByRole('button', { name: 'Войти' }).click();
  await expect(page).toHaveURL('/transactions');
}

async function createTransaction(page, toName, sum, comment) {
  await page.goto('/new');
  await page.locator('#toName').fill(toName);
  await page.locator('#sum').fill(sum);
  await page.locator('#comment').fill(comment);
  await page.getByRole('button', { name: 'Создать' }).click();
  await expect(page.getByText('Транзакция добавлена')).toBeVisible();
  await page.waitForURL('/transactions');
}

test.describe('Debts', () => {
  test('new user has empty debts list', async ({ page }) => {
    const username = `debt_empty_${Date.now()}`;
    await registerAndLogin(page, username, 'pass');
    await page.goto('/debts');
    await expect(page.getByText('Нет долгов для отображения')).toBeVisible();
  });

  test('debt appears after transaction is created', async ({ browser }) => {
    const ts = Date.now();
    const alice = `debt_alice_${ts}`;
    const bob = `debt_bob_${ts}`;

    const ctx1 = await browser.newContext();
    const ctx2 = await browser.newContext();
    const page1 = await ctx1.newPage();
    const page2 = await ctx2.newPage();

    try {
      await registerAndLogin(page1, alice, 'pass1');
      await registerAndLogin(page2, bob, 'pass2');

      await createTransaction(page1, bob, '150', 'debt-test');

      await page1.goto('/debts');
      await expect(page1.getByRole('cell', { name: bob })).toBeVisible();
      await expect(page1.getByRole('cell', { name: '150' })).toBeVisible();
    } finally {
      await ctx1.close();
      await ctx2.close();
    }
  });

  test('both sides see the same debt after transaction creation', async ({ browser }) => {
    const ts = Date.now() + 1;
    const alice = `debt_alice2_${ts}`;
    const bob = `debt_bob2_${ts}`;

    const ctx1 = await browser.newContext();
    const ctx2 = await browser.newContext();
    const page1 = await ctx1.newPage();
    const page2 = await ctx2.newPage();

    try {
      await registerAndLogin(page1, alice, 'pass1');
      await registerAndLogin(page2, bob, 'pass2');

      await createTransaction(page1, bob, '75', 'shared-debt');

      await page1.goto('/debts');
      await expect(page1.getByRole('cell', { name: bob })).toBeVisible();
      await expect(page1.getByRole('cell', { name: '75', exact: true })).toBeVisible();

      await page2.goto('/debts');
      await expect(page2.getByRole('cell', { name: alice })).toBeVisible();
      await expect(page2.getByRole('cell', { name: '75', exact: true })).toBeVisible();
    } finally {
      await ctx1.close();
      await ctx2.close();
    }
  });
});
