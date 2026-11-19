import { act, renderHook } from "@testing-library/react";
import { alternarSom, tocar, useSomLigado } from "./sons";

const iniciar = vi.fn();
const subirTom = vi.fn();
const oscilador = {
  type: "",
  frequency: { setValueAtTime: vi.fn(), exponentialRampToValueAtTime: subirTom },
  connect: vi.fn(),
  start: iniciar,
  stop: vi.fn(),
};
const volume = {
  gain: { setValueAtTime: vi.fn(), exponentialRampToValueAtTime: vi.fn() },
  connect: vi.fn(),
};
oscilador.connect.mockReturnValue(volume);

function prepararAudio() {
  iniciar.mockClear();
  subirTom.mockClear();
  vi.stubGlobal(
    "AudioContext",
    class {
      currentTime = 0;
      state = "running";
      destination = {};
      createOscillator = () => oscilador;
      createGain = () => volume;
      resume = vi.fn();
    },
  );
}

function ajustarSom(ligar: boolean) {
  const { result } = renderHook(() => useSomLigado());
  if (result.current !== ligar) {
    act(() => alternarSom());
  }
}

describe("sons", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    ajustarSom(false);
  });

  it("começa desligado, alterna e guarda a escolha no aparelho", () => {
    const { result } = renderHook(() => useSomLigado());
    const inicial = result.current;

    act(() => alternarSom());
    expect(result.current).toBe(!inicial);
    expect(localStorage.getItem("uniquiz:som-ligado")).toBe(inicial ? "nao" : "sim");

    act(() => alternarSom());
    expect(result.current).toBe(inicial);
  });

  it("toca as notas quando o som está ligado", () => {
    prepararAudio();
    ajustarSom(true);

    tocar("conclusao");

    expect(iniciar).toHaveBeenCalledTimes(4);
  });

  it("o clique é uma nota só e o erro desce de tom", () => {
    prepararAudio();
    ajustarSom(true);

    tocar("clique");
    expect(iniciar).toHaveBeenCalledTimes(1);

    tocar("erro");
    expect(iniciar).toHaveBeenCalledTimes(3);
    expect(subirTom).toHaveBeenCalledTimes(2);
  });

  it("vibra um instante no acerto e no erro, mas não no clique", () => {
    prepararAudio();
    const vibrar = vi.fn();
    vi.stubGlobal("navigator", { vibrate: vibrar });
    ajustarSom(true);

    tocar("clique");
    expect(vibrar).not.toHaveBeenCalled();
    tocar("acerto");
    tocar("erro");
    expect(vibrar).toHaveBeenCalledTimes(2);
  });

  it("não faz nada com o som desligado", () => {
    prepararAudio();
    ajustarSom(false);

    tocar("acerto");

    expect(iniciar).not.toHaveBeenCalled();
  });
});
