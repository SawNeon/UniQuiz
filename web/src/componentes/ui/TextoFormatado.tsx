import Markdown from "react-markdown";

const ELEMENTOS_PERMITIDOS = ["p", "strong", "em", "ul", "ol", "li", "h1", "h2", "h3"];

export function TextoFormatado({ conteudo }: { conteudo: string }) {
  return (
    <div className="flex flex-col gap-4 font-slab text-lg leading-relaxed [&_h1]:font-sans [&_h1]:text-3xl [&_h1]:font-extrabold [&_h2]:font-sans [&_h2]:text-2xl [&_h2]:font-extrabold [&_h3]:font-sans [&_h3]:text-xl [&_h3]:font-extrabold [&_ol]:list-decimal [&_ol]:pl-6 [&_strong]:font-bold [&_ul]:list-disc [&_ul]:pl-6">
      <Markdown skipHtml allowedElements={ELEMENTOS_PERMITIDOS} unwrapDisallowed>
        {conteudo}
      </Markdown>
    </div>
  );
}
