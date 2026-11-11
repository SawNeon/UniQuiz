import { act, renderHook } from "@testing-library/react";
import { alternarSom, tocar, useSomLigado } from "./sons";

describe("sons", () => {
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
    const iniciar = vi.fn();
    const oscilador = {
      type: "",
      frequency: { value: 0 },
      connect: vi.fn().mockReturnThis(),
      start: iniciar,
      stop: vi.fn(),
    };
    const volume = {
      gain: { setValueAtTime: vi.fn(), exponentialRampToValueAtTime: vi.fn() },
      connect: vi.fn(),
    };
    oscilador.connect.mockReturnValue(volume);
    vi.stubGlobal(
      "AudioContext",
      class {
        currentTime = 0;
        destination = {};
        createOscillator = () => oscilador;
        createGain = () => volume;
      },
    );
    const { result } = renderHook(() => useSomLigado());
    if (!result.current) {
      act(() => alternarSom());
    }

    tocar("conclusao");

    expect(iniciar).toHaveBeenCalledTimes(3);
    act(() => alternarSom());
  });
});
