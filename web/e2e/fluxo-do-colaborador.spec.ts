import { expect, test } from "@playwright/test";

test("colaborador entra, faz uma lição e vê a classificação", async ({ page }) => {
  await page.goto("/entrar");
  await page.getByLabel("Empresa").selectOption({ label: "Unimed Exemplo" });
  await page.getByLabel("Setor").selectOption({ label: "Tecnologia da Informação" });
  await page.getByRole("button", { name: "Bruno Teste" }).click();
  await page.getByLabel(/PIN/).fill("123");
  await page.getByRole("button", { name: "Sim, sou eu" }).click();

  await page
    .getByRole("link", { name: /Começar a lição/ })
    .first()
    .click();

  for (let passo = 0; passo < 10; passo += 1) {
    const resultado = page.getByRole("heading", { name: /Lição (perfeita|concluída)!/ });
    if (await resultado.isVisible()) {
      break;
    }
    const opcao = page.getByRole("radio").first();
    if (await opcao.isVisible()) {
      await opcao.click();
    } else {
      await page.getByRole("group", { name: "Banco de palavras" }).getByRole("button").first().click();
    }
    await page.getByRole("button", { name: "Verificar" }).click();
    await page.getByRole("button", { name: "Continuar" }).click();
  }

  await expect(page.getByRole("heading", { name: /Lição (perfeita|concluída)!/ })).toBeVisible();
  await page.getByRole("link", { name: "Voltar ao caminho" }).click();
  await page.getByRole("link", { name: "Classificação" }).click();
  await expect(page.getByText("Bruno Teste")).toBeVisible();
});
