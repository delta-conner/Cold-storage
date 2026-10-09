# -*- coding: utf-8 -*-
"""清理错误目录，按周思汉格式重建：Heading1-3 + 自动TOC域 + 点线目录。"""
from pathlib import Path
import re
from docx import Document
from docx.shared import Pt, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_TAB_ALIGNMENT, WD_TAB_LEADER
from docx.oxml.ns import qn
from docx.oxml import OxmlElement
from docx.text.paragraph import Paragraph

FINAL = Path(r"f:\plcmin\docs\冷库设备运维管理系统-论文第1-4章.docx")


def set_run_font(run, name_cn="宋体", name_en="Times New Roman", size=12, bold=False):
    run.bold = bold
    run.font.size = Pt(size)
    run.font.name = name_en
    rPr = run._element.get_or_add_rPr()
    rFonts = rPr.get_or_add_rFonts()
    rFonts.set(qn("w:eastAsia"), name_cn)


def configure_heading_styles(doc):
    for name, size, align, before, after in [
        ("Heading 1", 16, WD_ALIGN_PARAGRAPH.CENTER, Pt(12), Pt(10)),
        ("Heading 2", 14, WD_ALIGN_PARAGRAPH.LEFT, Pt(8), Pt(6)),
        ("Heading 3", 12, WD_ALIGN_PARAGRAPH.LEFT, Pt(6), Pt(4)),
    ]:
        try:
            st = doc.styles[name]
        except KeyError:
            continue
        st.font.bold = True
        st.font.size = Pt(size)
        st.font.name = "Times New Roman"
        try:
            st._element.get_or_add_rPr().get_or_add_rFonts().set(qn("w:eastAsia"), "黑体")
        except Exception:
            pass
        st.paragraph_format.alignment = align
        st.paragraph_format.space_before = before
        st.paragraph_format.space_after = after
        st.paragraph_format.line_spacing = 1.5
        st.paragraph_format.first_line_indent = Cm(0)


def heading_level(text: str):
    t = text.strip().lstrip("\u3000").lstrip()
    if re.match(r"^第[0-9一二三四五六七八九十]+章", t):
        return 1
    if re.match(r"^\d+\.\d+\.\d+", t):
        return 3
    if re.match(r"^\d+\.\d+", t):
        return 2
    return None


def find_toc_title(doc):
    for i, p in enumerate(doc.paragraphs):
        t = (p.text or "").strip().replace(" ", "").replace("\u3000", "")
        if t == "目录":
            return i
    raise SystemExit("未找到目　录")


def find_real_body_ch1(doc, toc_idx):
    """正文第1章：无页码制表符，且随后很快出现长正文。"""
    for i, p in enumerate(doc.paragraphs):
        if i <= toc_idx:
            continue
        raw = p.text or ""
        if "\t" in raw:
            continue
        t = raw.strip().lstrip("\u3000")
        if not t.startswith("第1章"):
            continue
        for j in range(i + 1, min(i + 12, len(doc.paragraphs))):
            tj = (doc.paragraphs[j].text or "").strip()
            if len(tj) > 80:
                return i
    raise SystemExit("未找到正文第1章")


def delete_paragraph(p):
    el = p._element
    parent = el.getparent()
    if parent is not None:
        parent.remove(el)


def clear_between(doc, after_idx, before_idx):
    """删除 (after_idx, before_idx) 开区间内段落。"""
    for i in range(before_idx - 1, after_idx, -1):
        delete_paragraph(doc.paragraphs[i])


def restyle_toc_title(p, doc):
    for r in list(p.runs):
        r._element.getparent().remove(r._element)
    for child in list(p._element):
        if child.tag.endswith("}r"):
            p._element.remove(child)
    try:
        p.style = doc.styles["Normal"]
    except Exception:
        pass
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(12)
    p.paragraph_format.space_after = Pt(12)
    p.paragraph_format.line_spacing = 1.5
    p.paragraph_format.first_line_indent = Cm(0)
    run = p.add_run("目　录")
    set_run_font(run, name_cn="黑体", size=16, bold=True)


def insert_after(paragraph):
    new_p = OxmlElement("w:p")
    paragraph._element.addnext(new_p)
    return Paragraph(new_p, paragraph._parent)


def add_toc_field(paragraph):
    run = paragraph.add_run()
    r = run._r

    def fld(typ):
        e = OxmlElement("w:fldChar")
        e.set(qn("w:fldCharType"), typ)
        return e

    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = ' TOC \\o "1-3" \\h \\z \\u '
    r.append(fld("begin"))
    r.append(instr)
    r.append(fld("separate"))
    t = OxmlElement("w:t")
    t.set(qn("xml:space"), "preserve")
    t.text = "（请右键 → 更新域 → 更新整个目录）"
    r.append(t)
    r.append(fld("end"))
    set_run_font(run, size=10.5)


def add_toc_line(prev, title, level, page):
    p = insert_after(prev)
    p.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.LEFT
    p.paragraph_format.line_spacing = 1.5
    p.paragraph_format.space_after = Pt(2)
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.first_line_indent = Cm(0)
    p.paragraph_format.tab_stops.add_tab_stop(Cm(14.5), WD_TAB_ALIGNMENT.RIGHT, WD_TAB_LEADER.DOTS)
    indent = {1: "", 2: "　　", 3: "　　　　"}[level]
    run = p.add_run(f"{indent}{title}\t{page}")
    set_run_font(run, name_cn="宋体", size=12, bold=(level == 1))
    return p


def apply_body_headings(doc, body_idx):
    for i, p in enumerate(doc.paragraphs):
        if i < body_idx:
            # 目录区保持 Normal
            if p.style and p.style.name.startswith("Heading"):
                try:
                    p.style = doc.styles["Normal"]
                except Exception:
                    pass
            continue
        raw = (p.text or "").strip()
        # 去掉误加的全角缩进
        cleaned = raw.lstrip("\u3000").strip()
        lvl = heading_level(cleaned)
        if lvl is None:
            if p.style and p.style.name.startswith("Heading"):
                try:
                    p.style = doc.styles["Normal"]
                except Exception:
                    pass
            continue
        if cleaned != raw:
            # 重写文本去缩进
            for r in list(p.runs):
                r._element.getparent().remove(r._element)
            run = p.add_run(cleaned)
            set_run_font(run, name_cn="黑体", size={1: 16, 2: 14, 3: 12}[lvl], bold=True)
        style_name = {1: "Heading 1", 2: "Heading 2", 3: "Heading 3"}[lvl]
        try:
            p.style = doc.styles[style_name]
        except KeyError:
            continue
        p.paragraph_format.first_line_indent = Cm(0)
        if lvl == 1:
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER


def collect_entries(doc, body_idx):
    entries = []
    for i, p in enumerate(doc.paragraphs):
        if i < body_idx:
            continue
        style = p.style.name if p.style else ""
        t = (p.text or "").strip().lstrip("\u3000")
        if style == "Heading 1":
            entries.append((1, t))
        elif style == "Heading 2":
            entries.append((2, t))
        elif style == "Heading 3":
            entries.append((3, t))
    return entries


def main():
    doc = Document(str(FINAL))
    configure_heading_styles(doc)

    toc_idx = find_toc_title(doc)
    body_idx = find_real_body_ch1(doc, toc_idx)
    print("before clear: toc", toc_idx, "body", body_idx, "paras", len(doc.paragraphs))

    # 删掉目录标题与正文第1章之间的全部杂项
    clear_between(doc, toc_idx, body_idx)

    toc_idx = find_toc_title(doc)
    body_idx = find_real_body_ch1(doc, toc_idx)
    print("after clear: toc", toc_idx, "body", body_idx, "paras", len(doc.paragraphs))

    apply_body_headings(doc, body_idx)
    entries = collect_entries(doc, body_idx)
    print("entries", len(entries))
    for e in entries[:8]:
        print(" ", e)
    if len(entries) < 8:
        raise SystemExit("标题过少，中止")

    toc_title = doc.paragraphs[toc_idx]
    restyle_toc_title(toc_title, doc)

    field_p = insert_after(toc_title)
    field_p.paragraph_format.space_after = Pt(6)
    add_toc_field(field_p)

    last = field_p
    page = 1
    for level, title in entries:
        last = add_toc_line(last, title, level, str(page))
        page += 2 if level == 1 else 1

    tip = insert_after(last)
    tip.paragraph_format.space_before = Pt(8)
    tr = tip.add_run("说明：自动目录域格式与周思汉范文一致。请用 Word 打开后，右键目录→更新域→更新整个目录，以刷新真实页码。")
    set_run_font(tr, size=10.5)

    try:
        doc.save(str(FINAL))
        out = FINAL
    except PermissionError:
        out = FINAL.with_name("冷库设备运维管理系统-论文第1-4章-目录已改.docx")
        doc.save(str(out))
    print("saved", out.name, "paras", len(Document(str(out)).paragraphs))


if __name__ == "__main__":
    main()
