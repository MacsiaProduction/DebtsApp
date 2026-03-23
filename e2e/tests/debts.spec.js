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

      // Alice pays for Bob (Alice is creditor, Bob owes Alice)
      await page1.goto('/new');
      await page1.locator('#chatId').fill('1');
      await page1.locator('#toName').fill(bob);
      await page1.locator('#sum').fill('150');
      await page1.locator('#comment').fill('debt-test');
      await page1.getByRole('button', { name: 'Создать' }).click();
      await page1.waitForURL('/transactions');

      // Check debts page
      await page1.goto('/debts');
      await expect(page1.getByRole('cell', { name: bob })).toBeVisible();
      await expect(page1.getByRole('cell', { name: '150' })).toBeVisible();
    } finally {
      await ctx1.close();
      await ctx2.close();
    }
  });

  test('query debt between two users in a chat', async ({ browser }) => {
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

      await page1.goto('/new');
      await page1.locator('#chatId').fill('42');
      await page1.locator('#toName').fill(bob);
      await page1.locator('#sum').fill('75');
      await page1.locator('#comment').fill('between-debt');
      await page1.getByRole('button', { name: 'Создать' }).click();
      await page1.waitForURL('/transactions');

      await page1.goto('/debts');
      await page1.locator('#betweenChatId').fill('42');
      await page1.locator('#betweenFromName').fill(alice);
      await page1.locator('#betweenToName').fill(bob);
      await page1.getByRole('button', { name: 'Показать долг' }).click();

      await expect(page1.getByText('75')).toBeVisible();
    } finally {
      await ctx1.close();
      await ctx2.close();
    }
  });
});
