export const TAMANHO_DO_PIN = 3;

export function pinDigitado(texto: string): string {
  return texto.replace(/\D/g, "").slice(0, TAMANHO_DO_PIN);
}

export function pinCompleto(pin: string): boolean {
  return pin.length === TAMANHO_DO_PIN;
}
