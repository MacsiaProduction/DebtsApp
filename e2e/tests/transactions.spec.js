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

async function loginOnly(page, username, password) {
  await page.goto('/login');
  await page.locator('#loginUsername').fill(username);
  await page.locator('#loginPassword').fill(password);
  await page.getByRole('button', { name: 'Войти' }).click();
  await expect(page).toHaveURL('/transactions');
}

test.describe('Transactions', () => {
  test('new user has empty transactions list', async ({ page }) => {
    const username = `empty_${Date.now()}`;
    await registerAndLogin(page, username, 'pass');
    await expect(page.getByText('Нет транзакций для отображения')).toBeVisible();
  });

  test('sender can create a transaction and see it in their list', async ({ browser }) => {
    const ts = Date.now();
    const alice = `alice_${ts}`;
    const bob = `bob_${ts}`;

    const ctx1 = await browser.newContext();
    const ctx2 = await browser.newContext();
    const page1 = await ctx1.newPage();
    const page2 = await ctx2.newPage();

    try {
      await registerAndLogin(page1, alice, 'pass1');
      await registerAndLogin(page2, bob, 'pass2');

      // Alice creates a transaction to Bob
      await page1.goto('/new');
      await page1.locator('#chatId').fill('1');
      await page1.locator('#toName').fill(bob);
      await page1.locator('#sum').fill('100');
      await page1.locator('#comment').fill('dinner');
      await page1.getByRole('button', { name: 'Создать' }).click();

      await expect(page1.getByText('Транзакция добавлена')).toBeVisible();
      await page1.waitForURL('/transactions');

      // Alice should see the transaction
      await expect(page1.getByRole('cell', { name: alice })).toBeVisible();
      await expect(page1.getByRole('cell', { name: bob })).toBeVisible();
      await expect(page1.getByRole('cell', { name: '100' })).toBeVisible();
      await expect(page1.getByRole('cell', { name: 'dinner' })).toBeVisible();
    } finally {
      await ctx1.close();
      await ctx2.close();
    }
  });

  test('recipient can see incoming transaction', async ({ browser }) => {
    const ts = Date.now() + 1;
    const alice = `alice2_${ts}`;
    const bob = `bob2_${ts}`;

    const ctx1 = await browser.newContext();
    const ctx2 = await browser.newContext();
    const page1 = await ctx1.newPage();
    const page2 = await ctx2.newPage();

    try {
      await registerAndLogin(page1, alice, 'pass1');
      await registerAndLogin(page2, bob, 'pass2');

      // Alice sends Bob money
      await page1.goto('/new');
      await page1.locator('#chatId').fill('1');
      await page1.locator('#toName').fill(bob);
      await page1.locator('#sum').fill('200');
      await page1.locator('#comment').fill('lunch');
      await page1.getByRole('button', { name: 'Создать' }).click();
      await page1.waitForURL('/transactions');

      // Bob should see the transaction in his list
      await page2.goto('/transactions');
      await expect(page2.getByRole('cell', { name: 'lunch' })).toBeVisible();
      await expect(page2.getByRole('cell', { name: '200' })).toBeVisible();
      await expect(page2.getByRole('cell', { name: alice })).toBeVisible();
    } finally {
      await ctx1.close();
      await ctx2.close();
    }
  });

  test('filter by "between two users" shows only their transactions', async ({ browser }) => {
    const ts = Date.now() + 2;
    const alice = `alice3_${ts}`;
    const bob = `bob3_${ts}`;
    const charlie = `charlie3_${ts}`;

    const ctx1 = await browser.newContext();
    const ctx2 = await browser.newContext();
    const ctx3 = await browser.newContext();
    const page1 = await ctx1.newPage();
    const page2 = await ctx2.newPage();
    const page3 = await ctx3.newPage();

    try {
      await registerAndLogin(page1, alice, 'pass1');
      await registerAndLogin(page2, bob, 'pass2');
      await registerAndLogin(page3, charlie, 'pass3');

      // Alice → Bob
      await page1.goto('/new');
      await page1.locator('#chatId').fill('1');
      await page1.locator('#toName').fill(bob);
      await page1.locator('#sum').fill('50');
      await page1.locator('#comment').fill('filter-test');
      await page1.getByRole('button', { name: 'Создать' }).click();
      await page1.waitForURL('/transactions');

      // Alice → Charlie (should NOT appear in filter)
      await page1.goto('/new');
      await page1.locator('#chatId').fill('1');
      await page1.locator('#toName').fill(charlie);
      await page1.locator('#sum').fill('999');
      await page1.locator('#comment').fill('should-not-appear');
      await page1.getByRole('button', { name: 'Создать' }).click();
      await page1.waitForURL('/transactions');

      // Apply "between" filter: alice → bob
      await page1.locator('#txMode').selectOption('between');
      await page1.locator('#txSender').fill(alice);
      await page1.locator('#txRecipient').fill(bob);
      await page1.getByRole('button', { name: 'Применить фильтр' }).click();

      await expect(page1.getByRole('cell', { name: 'filter-test' })).toBeVisible();
      await expect(page1.getByText('should-not-appear')).not.toBeVisible();
    } finally {
      await ctx1.close();
      await ctx2.close();
      await ctx3.close();
    }
  });

  test('multiple transactions accumulate and are all visible', async ({ browser }) => {
    const ts = Date.now() + 3;
    const alice = `alice4_${ts}`;
    const bob = `bob4_${ts}`;

    const ctx1 = await browser.newContext();
    const ctx2 = await browser.newContext();
    const page1 = await ctx1.newPage();
    const page2 = await ctx2.newPage();

    try {
      await registerAndLogin(page1, alice, 'pass1');
      await registerAndLogin(page2, bob, 'pass2');

      for (const [sum, comment] of [['10', 'coffee'], ['20', 'taxi'], ['30', 'hotel']]) {
        await page1.goto('/new');
        await page1.locator('#chatId').fill('1');
        await page1.locator('#toName').fill(bob);
        await page1.locator('#sum').fill(sum);
        await page1.locator('#comment').fill(comment);
        await page1.getByRole('button', { name: 'Создать' }).click();
        await page1.waitForURL('/transactions');
      }

      const rows = page1.getByRole('row');
      await expect(rows).toHaveCount(4); // 3 data rows + 1 header
    } finally {
      await ctx1.close();
      await ctx2.close();
    }
  });
});
