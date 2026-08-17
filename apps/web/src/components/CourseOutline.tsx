import Link from "next/link";
import { contentHref, type ContentChild } from "@/lib/api/content";

type Props = {
  items: ContentChild[];
};

export function CourseOutline({ items }: Props) {
  if (!items.length) {
    return null;
  }
  return (
    <ol className="outline">
      {items.map((item) => (
        <li key={item.id}>
          <Link href={contentHref(item.kind, item.slug)}>{item.title}</Link>
          <span className="muted">
            {" "}
            · {kindLabel(item.kind)}
            {item.required ? "" : " · opsional"}
          </span>
          {item.children?.length ? <CourseOutline items={item.children} /> : null}
        </li>
      ))}
    </ol>
  );
}

function kindLabel(kind: string): string {
  switch (kind) {
    case "MODULE":
      return "Modul";
    case "LESSON":
      return "Pelajaran";
    case "MATERIAL":
      return "Materi";
    case "QUIZ":
      return "Kuis";
    case "COURSE":
      return "Kursus";
    case "LEARNING_PATH":
      return "Jalur belajar";
    default:
      return kind;
  }
}
