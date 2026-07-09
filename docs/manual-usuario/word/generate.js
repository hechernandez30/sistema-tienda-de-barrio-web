/*
 * Genera un documento Word (.docx) a partir de un archivo Markdown.
 * Cada imagen del Markdown se convierte en un "recuadro" (marco punteado)
 * donde se puede pegar una captura.
 *
 * Uso (manual de usuario, por defecto):
 *   npm install
 *   npm run build
 *
 * Uso genérico (otro manual):
 *   node generate.js <input.md> <output.docx> "<texto pie de página>" "<título>"
 */

const fs = require('fs');
const path = require('path');
const {
  Document,
  Packer,
  Paragraph,
  TextRun,
  HeadingLevel,
  AlignmentType,
  Table,
  TableRow,
  TableCell,
  WidthType,
  BorderStyle,
  ShadingType,
  VerticalAlign,
  HeightRule,
  Footer,
  PageNumber,
} = require('docx');

// Rutas por defecto (manual de usuario). Se pueden sobreescribir por argumentos:
//   node generate.js <input.md> <output.docx> "<pie de página>" "<título>"
const DEFAULT_MD = path.join(__dirname, '..', 'manual-usuario.md');
const DEFAULT_OUT = path.join(__dirname, '..', 'Manual-de-Usuario-Tienda-de-Barrio.docx');

const MD_PATH = process.argv[2] ? path.resolve(process.argv[2]) : DEFAULT_MD;
const OUT_PATH = process.argv[3] ? path.resolve(process.argv[3]) : DEFAULT_OUT;
const FOOTER_LABEL = process.argv[4] || 'Manual de Usuario · Sistema Tienda de Barrio · Página ';
const DOC_TITLE = process.argv[5] || 'Manual de Usuario - Sistema Tienda de Barrio';

const GRAY = '6B7280';
const LIGHT_GRAY = 'AAAAAA';
const BOX_FILL = 'F5F6F8';
const HEADER_FILL = 'D9E1F2';
const RULE_COLOR = 'CBD5E1';

// ---------------------------------------------------------------------------
// Parseo de formato en línea: **negrita**, `código`, [texto](enlace)
// ---------------------------------------------------------------------------
function parseInline(text, base = {}) {
  const runs = [];
  let buffer = '';
  let i = 0;

  const flush = () => {
    if (buffer) {
      runs.push(new TextRun({ text: buffer, ...base }));
      buffer = '';
    }
  };

  while (i < text.length) {
    if (text.startsWith('**', i)) {
      const end = text.indexOf('**', i + 2);
      if (end !== -1) {
        flush();
        runs.push(new TextRun({ text: text.slice(i + 2, end), bold: true, ...base }));
        i = end + 2;
        continue;
      }
    }
    if (text[i] === '`') {
      const end = text.indexOf('`', i + 1);
      if (end !== -1) {
        flush();
        runs.push(
          new TextRun({ text: text.slice(i + 1, end), font: 'Consolas', ...base }),
        );
        i = end + 1;
        continue;
      }
    }
    if (text[i] === '[') {
      const close = text.indexOf(']', i + 1);
      if (close !== -1 && text[close + 1] === '(') {
        const paren = text.indexOf(')', close + 2);
        if (paren !== -1) {
          flush();
          runs.push(new TextRun({ text: text.slice(i + 1, close), ...base }));
          i = paren + 1;
          continue;
        }
      }
    }
    buffer += text[i];
    i++;
  }
  flush();
  if (runs.length === 0) runs.push(new TextRun({ text: '', ...base }));
  return runs;
}

// ---------------------------------------------------------------------------
// Detectores de bloque
// ---------------------------------------------------------------------------
const reHeading = /^(#{1,6})\s+(.*)$/;
const reImage = /^!\[([^\]]*)\]\(([^)]+)\)\s*$/;
const reUnordered = /^\s*-\s+(.*)$/;
const reOrdered = /^\s*(\d+)\.\s+(.*)$/;
const reQuote = /^>\s?(.*)$/;

function isBlank(line) {
  return line.trim() === '';
}
function isRule(line) {
  return line.trim() === '---';
}
function isTable(line) {
  return line.trim().startsWith('|');
}
function isSpecial(line) {
  return (
    isBlank(line) ||
    isRule(line) ||
    isTable(line) ||
    reHeading.test(line) ||
    reImage.test(line) ||
    reQuote.test(line) ||
    reUnordered.test(line) ||
    reOrdered.test(line)
  );
}

// ---------------------------------------------------------------------------
// Constructores de elementos docx
// ---------------------------------------------------------------------------
function heading(level, text) {
  const map = {
    1: HeadingLevel.TITLE,
    2: HeadingLevel.HEADING_1,
    3: HeadingLevel.HEADING_2,
    4: HeadingLevel.HEADING_3,
  };
  const alignment = level === 1 ? AlignmentType.CENTER : AlignmentType.LEFT;
  return new Paragraph({
    heading: map[level] || HeadingLevel.HEADING_3,
    alignment,
    spacing: { before: level <= 2 ? 240 : 200, after: 120 },
    children: parseInline(text),
  });
}

function horizontalRule() {
  return new Paragraph({
    spacing: { before: 100, after: 100 },
    border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: RULE_COLOR, space: 1 } },
    children: [new TextRun({ text: '' })],
  });
}

function plainParagraph(text) {
  return new Paragraph({
    spacing: { after: 120 },
    children: parseInline(text),
  });
}

function quoteParagraph(text) {
  return new Paragraph({
    spacing: { before: 80, after: 120 },
    indent: { left: 360 },
    border: { left: { style: BorderStyle.SINGLE, size: 18, color: HEADER_FILL, space: 12 } },
    shading: { type: ShadingType.CLEAR, color: 'auto', fill: 'F1F5F9' },
    children: parseInline(text, { italics: true, color: GRAY }),
  });
}

function bulletParagraph(text) {
  return new Paragraph({
    spacing: { after: 60 },
    bullet: { level: 0 },
    children: parseInline(text),
  });
}

function orderedParagraph(num, text) {
  return new Paragraph({
    spacing: { after: 60 },
    indent: { left: 500, hanging: 320 },
    children: [new TextRun({ text: `${num}.  `, bold: true }), ...parseInline(text)],
  });
}

const dashedBorder = { style: BorderStyle.DASHED, size: 8, color: LIGHT_GRAY };

function imagePlaceholder(alt, src) {
  const label = alt && alt.trim() ? alt.trim() : 'Captura de pantalla';
  return new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    rows: [
      new TableRow({
        height: { value: 3000, rule: HeightRule.ATLEAST },
        children: [
          new TableCell({
            verticalAlign: VerticalAlign.CENTER,
            shading: { type: ShadingType.CLEAR, color: 'auto', fill: BOX_FILL },
            margins: { top: 200, bottom: 200, left: 200, right: 200 },
            borders: {
              top: dashedBorder,
              bottom: dashedBorder,
              left: dashedBorder,
              right: dashedBorder,
            },
            children: [
              new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { after: 60 },
                children: [
                  new TextRun({ text: 'Pegar aquí la captura', bold: true, color: GRAY, size: 24 }),
                ],
              }),
              new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { after: 40 },
                children: [new TextRun({ text: label, color: GRAY, size: 22 })],
              }),
              new Paragraph({
                alignment: AlignmentType.CENTER,
                children: [
                  new TextRun({
                    text: `(archivo sugerido: ${src})`,
                    italics: true,
                    color: LIGHT_GRAY,
                    size: 18,
                  }),
                ],
              }),
            ],
          }),
        ],
      }),
    ],
  });
}

function splitRow(line) {
  return line
    .trim()
    .replace(/^\|/, '')
    .replace(/\|$/, '')
    .split('|')
    .map((s) => s.trim());
}

function buildTable(lines) {
  const rows = lines.map(splitRow);
  const header = rows[0];
  const bodyRows = rows.slice(2); // omite la fila separadora | --- |
  const colCount = header.length;

  const makeCell = (text, isHeader) =>
    new TableCell({
      shading: isHeader
        ? { type: ShadingType.CLEAR, color: 'auto', fill: HEADER_FILL }
        : undefined,
      margins: { top: 60, bottom: 60, left: 120, right: 120 },
      children: [
        new Paragraph({ children: parseInline(text, isHeader ? { bold: true } : {}) }),
      ],
    });

  const trHeader = new TableRow({
    tableHeader: true,
    children: header.map((c) => makeCell(c, true)),
  });

  const trBody = bodyRows.map(
    (r) =>
      new TableRow({
        children: Array.from({ length: colCount }, (_, idx) => makeCell(r[idx] || '', false)),
      }),
  );

  return new Table({
    width: { size: 100, type: WidthType.PERCENTAGE },
    rows: [trHeader, ...trBody],
  });
}

// ---------------------------------------------------------------------------
// Parseo principal del Markdown -> elementos docx
// ---------------------------------------------------------------------------
function parseMarkdown(md) {
  const lines = md.replace(/\r\n/g, '\n').split('\n');
  const elements = [];
  let i = 0;

  while (i < lines.length) {
    const line = lines[i];

    if (isBlank(line)) {
      i++;
      continue;
    }

    if (isRule(line)) {
      elements.push(horizontalRule());
      i++;
      continue;
    }

    const mHeading = line.match(reHeading);
    if (mHeading) {
      elements.push(heading(mHeading[1].length, mHeading[2].trim()));
      i++;
      continue;
    }

    const mImage = line.match(reImage);
    if (mImage) {
      elements.push(imagePlaceholder(mImage[1], mImage[2]));
      elements.push(new Paragraph({ spacing: { after: 80 }, children: [new TextRun('')] }));
      i++;
      continue;
    }

    if (isTable(line)) {
      const block = [];
      while (i < lines.length && isTable(lines[i])) {
        block.push(lines[i]);
        i++;
      }
      elements.push(buildTable(block));
      elements.push(new Paragraph({ spacing: { after: 80 }, children: [new TextRun('')] }));
      continue;
    }

    const mQuote = line.match(reQuote);
    if (mQuote) {
      const parts = [];
      while (i < lines.length && reQuote.test(lines[i])) {
        parts.push(lines[i].match(reQuote)[1].trim());
        i++;
      }
      elements.push(quoteParagraph(parts.join(' ')));
      continue;
    }

    if (reUnordered.test(line) || reOrdered.test(line)) {
      while (i < lines.length) {
        const cur = lines[i];
        if (isBlank(cur)) {
          i++;
          break;
        }
        const mU = cur.match(reUnordered);
        const mO = cur.match(reOrdered);
        if (mO) {
          let text = mO[2];
          i++;
          // líneas de continuación (indentadas, no nuevo ítem ni bloque especial)
          while (i < lines.length && !isBlank(lines[i]) && !isSpecial(lines[i])) {
            text += ' ' + lines[i].trim();
            i++;
          }
          elements.push(orderedParagraph(mO[1], text));
        } else if (mU) {
          let text = mU[1];
          i++;
          while (i < lines.length && !isBlank(lines[i]) && !isSpecial(lines[i])) {
            text += ' ' + lines[i].trim();
            i++;
          }
          elements.push(bulletParagraph(text));
        } else {
          break;
        }
      }
      continue;
    }

    // Párrafo normal: une líneas consecutivas hasta blanco o bloque especial.
    let text = line.trim();
    i++;
    while (i < lines.length && !isBlank(lines[i]) && !isSpecial(lines[i])) {
      text += ' ' + lines[i].trim();
      i++;
    }
    elements.push(plainParagraph(text));
  }

  return elements;
}

// ---------------------------------------------------------------------------
// Generación del documento
// ---------------------------------------------------------------------------
function main() {
  const md = fs.readFileSync(MD_PATH, 'utf8');
  const children = parseMarkdown(md);

  const doc = new Document({
    creator: 'Sistema Tienda de Barrio',
    title: DOC_TITLE,
    styles: {
      default: {
        document: {
          run: { font: 'Calibri', size: 22 },
          paragraph: { spacing: { line: 276 } },
        },
      },
    },
    sections: [
      {
        properties: {
          page: { margin: { top: 1134, bottom: 1134, left: 1134, right: 1134 } },
        },
        footers: {
          default: new Footer({
            children: [
              new Paragraph({
                alignment: AlignmentType.CENTER,
                children: [
                  new TextRun({ text: FOOTER_LABEL, size: 16, color: GRAY }),
                  new TextRun({ children: [PageNumber.CURRENT], size: 16, color: GRAY }),
                ],
              }),
            ],
          }),
        },
        children,
      },
    ],
  });

  return Packer.toBuffer(doc).then((buffer) => {
    fs.writeFileSync(OUT_PATH, buffer);
    console.log('Documento generado:', OUT_PATH);
  });
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
