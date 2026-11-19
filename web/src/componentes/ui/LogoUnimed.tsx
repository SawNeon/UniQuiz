import logo from "../../assets/logo-unimed-varginha.png";

export function LogoUnimed({ altura = 40, className = "" }: { altura?: number; className?: string }) {
  return (
    <img
      src={logo}
      alt="Unimed Varginha"
      height={altura}
      style={{ height: altura, width: "auto" }}
      className={["select-none", className].join(" ")}
      draggable={false}
    />
  );
}
