import type { ContentBody } from "@/lib/api/content";

export function ContentBodyView({ body }: { body: ContentBody | null | undefined }) {
  const blocks = body?.blocks ?? [];
  if (blocks.length === 0) {
    return null;
  }
  return (
    <div className="content-body">
      {blocks.map((block, index) => {
        if (block.type === "heading") {
          const Tag = block.level === 3 ? "h3" : block.level === 1 ? "h1" : "h2";
          return <Tag key={index}>{block.text}</Tag>;
        }
        if (block.type === "list") {
          const ListTag = block.ordered ? "ol" : "ul";
          return (
            <ListTag key={index}>
              {(block.items ?? []).map((item, itemIndex) => (
                <li key={itemIndex}>{item}</li>
              ))}
            </ListTag>
          );
        }
        if (block.type === "code") {
          return (
            <pre key={index}>
              <code>{block.text}</code>
            </pre>
          );
        }
        if (block.type === "quote") {
          return <blockquote key={index}>{block.text}</blockquote>;
        }
        if (block.type === "link" && block.href) {
          return (
            <p key={index}>
              <a href={block.href} rel="noopener noreferrer">
                {block.text || block.href}
              </a>
            </p>
          );
        }
        if (block.type === "image" && block.href) {
          return (
            <p key={index}>
              {/* Contributor image URLs are sanitized to http(s) only on the API. */}
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={block.href} alt={block.text ?? ""} />
            </p>
          );
        }
        return <p key={index}>{block.text}</p>;
      })}
    </div>
  );
}
