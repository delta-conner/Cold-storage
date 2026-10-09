# -*- coding: utf-8 -*-
"""按范本重绘黑白 UML 用例图，并重建第1-4章 Word。"""
from pathlib import Path
import matplotlib.pyplot as plt
from matplotlib.patches import Ellipse, FancyBboxPatch, Rectangle
from docx import Document
from docx.shared import Pt, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn

ROOT = Path(r"f:\plcmin")
FIG = ROOT / "docs" / "thesis_figs"
FIG.mkdir(parents=True, exist_ok=True)
OUT = ROOT / "docs" / "冷库设备运维管理系统-论文第1-4章.docx"
OUT3 = ROOT / "docs" / "冷库设备运维管理系统-论文前三章.docx"

plt.rcParams["font.sans-serif"] = ["SimSun", "Microsoft YaHei", "SimHei"]
plt.rcParams["axes.unicode_minus"] = False


def savefig(fig, name):
    path = FIG / name
    fig.savefig(path, dpi=180, bbox_inches="tight", facecolor="white", edgecolor="none")
    plt.close(fig)
    return path


def draw_actor(ax, x, y, label):
    """经典火柴人"""
    ax.add_patch(plt.Circle((x, y), 0.11, fill=False, ec="black", lw=1.3))
    ax.plot([x, x], [y - 0.11, y - 0.42], color="black", lw=1.3)
    ax.plot([x - 0.16, x + 0.16], [y - 0.22, y - 0.22], color="black", lw=1.3)
    ax.plot([x, x - 0.14], [y - 0.42, y - 0.62], color="black", lw=1.3)
    ax.plot([x, x + 0.14], [y - 0.42, y - 0.62], color="black", lw=1.3)
    ax.text(x, y - 0.78, label, ha="center", va="top", fontsize=11, fontname="SimSun")


def draw_uc(ax, x, y, w, h, text):
    e = Ellipse((x, y), w, h, facecolor="white", edgecolor="black", lw=1.2, zorder=3)
    ax.add_patch(e)
    ax.text(x, y, text, ha="center", va="center", fontsize=10, fontname="SimSun", zorder=4)


def draw_boundary(ax, x0, y0, x1, y1, title):
    ax.add_patch(Rectangle((x0, y0), x1 - x0, y1 - y0, fill=False, ec="black", lw=1.4, zorder=1))
    ax.text((x0 + x1) / 2, y1 - 0.28, title, ha="center", va="top",
            fontsize=12, fontname="SimSun", fontweight="bold")


def assoc(ax, x1, y1, x2, y2):
    ax.plot([x1, x2], [y1, y2], color="black", lw=1.0, zorder=2)


def include_arrow(ax, x1, y1, x2, y2):
    ax.annotate("", xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle="->", color="black", lw=1.0, ls="--"), zorder=2)
    mx, my = (x1 + x2) / 2, (y1 + y2) / 2
    ax.text(mx, my + 0.12, "<<include>>", ha="center", va="bottom", fontsize=8, fontname="SimSun")


def classic_actor_usecases(fname, system_title, actor, cases, login_include=True):
    """
    cases: list of use case names (不含登录；登录单独画在右侧并 include)
    若 login_include=False，则把 cases 全部画成直接关联（含登录/退出）
    """
    n = len(cases)
    height = max(5.2, 1.2 + n * 0.72 + 0.8)
    fig, ax = plt.subplots(figsize=(9.2, height * 0.85))
    ax.set_xlim(0, 9.2)
    ax.set_ylim(0, height)
    ax.axis("off")

    y_top = height - 0.35
    y_bot = 0.35
    draw_boundary(ax, 2.3, y_bot, 8.9, y_top, system_title)

    actor_y = (y_top + y_bot) / 2 + 0.15
    draw_actor(ax, 1.0, actor_y, actor)

    if login_include:
        # 用例纵向排列在左侧区域，登录在右侧
        span = y_top - y_bot - 1.0
        ys = [y_top - 0.85 - i * (span / max(n - 1, 1)) for i in range(n)] if n > 1 else [(y_top + y_bot) / 2]
        login_y = (y_top + y_bot) / 2
        draw_uc(ax, 7.3, login_y, 2.4, 0.7, "登录")
        for y, name in zip(ys, cases):
            draw_uc(ax, 4.5, y, 2.6, 0.65, name)
            assoc(ax, 1.2, actor_y - 0.15, 3.15, y)
            include_arrow(ax, 5.75, y, 6.05, login_y)
    else:
        span = y_top - y_bot - 1.0
        ys = [y_top - 0.85 - i * (span / max(n - 1, 1)) for i in range(n)] if n > 1 else [(y_top + y_bot) / 2]
        for y, name in zip(ys, cases):
            draw_uc(ax, 5.6, y, 2.8, 0.62, name)
            assoc(ax, 1.2, actor_y - 0.15, 4.15, y)

    return savefig(fig, fname)


def fig_role():
    fig, ax = plt.subplots(figsize=(10.2, 6.6))
    ax.set_xlim(0, 10.2)
    ax.set_ylim(0, 6.6)
    ax.axis("off")
    draw_boundary(ax, 2.4, 0.35, 9.9, 6.25, "冷库设备综合运维管理系统")
    draw_actor(ax, 1.05, 5.2, "管理员")
    draw_actor(ax, 1.05, 3.4, "运维人员")
    draw_actor(ax, 1.05, 1.6, "甲方用户")
    cases = [
        (5.8, 5.7, "用户与权限管理"),
        (5.8, 5.0, "冷库/设备台账管理"),
        (5.8, 4.3, "工单全流程管控"),
        (5.8, 3.6, "维保计划与任务"),
        (5.8, 2.9, "故障/案例/备件"),
        (5.8, 2.2, "健康分析与统计"),
        (5.8, 1.5, "消息中心"),
        (5.8, 0.85, "登录认证"),
    ]
    for x, y, t in cases:
        draw_uc(ax, x, y, 2.7, 0.55, t)
    for y in [5.7, 5.0, 4.3, 3.6, 2.9, 2.2, 1.5, 0.85]:
        assoc(ax, 1.25, 5.0, 4.4, y)
    for y in [5.0, 4.3, 3.6, 2.9, 2.2, 1.5, 0.85]:
        assoc(ax, 1.25, 3.2, 4.4, y)
    for y in [5.0, 4.3, 1.5, 0.85]:
        assoc(ax, 1.25, 1.4, 4.4, y)
    return savefig(fig, "fig3_1_role.png")


def redraw_all_usecases():
    paths = {}
    paths["3_1"] = fig_role()
    paths["3_2"] = classic_actor_usecases(
        "fig3_2_client.png", "冷库设备运维管理系统", "甲方用户",
        ["浏览设备台账", "提交报修工单", "查看本人工单", "确认验收", "服务评价", "个人中心", "消息中心"],
        login_include=True)
    paths["3_3"] = classic_actor_usecases(
        "fig3_3_device_view.png", "冷库设备运维管理系统", "甲方用户",
        ["条件查询设备", "查看设备详情", "选择报修设备"], True)
    paths["3_4"] = classic_actor_usecases(
        "fig3_4_submit.png", "冷库设备运维管理系统", "甲方用户",
        ["填写故障描述", "上传现场图片", "提交生成工单", "接收进度通知"], True)
    paths["3_5"] = classic_actor_usecases(
        "fig3_5_ops.png", "冷库设备运维管理系统", "运维人员",
        ["运维工作台", "处理指派工单", "故障记录", "案例库检索", "维保任务执行", "备件领用", "设备健康查看", "消息中心"],
        True)
    paths["3_6"] = classic_actor_usecases(
        "fig3_6_process.png", "冷库设备运维管理系统", "运维人员",
        ["接单", "填写处理过程", "登记备件出库", "上传维修图片", "提交完工"], True)
    paths["3_7"] = classic_actor_usecases(
        "fig3_7_admin.png", "冷库设备运维管理系统", "管理员",
        ["用户权限管理", "冷库冷藏间管理", "设备台账管理", "工单派单管控", "维保计划任务", "备件供应商", "健康分析统计", "操作日志/消息"],
        True)
    paths["3_8"] = classic_actor_usecases(
        "fig3_8_user_mgmt.png", "冷库设备运维管理系统", "管理员",
        ["查询用户", "新增用户", "编辑用户", "启用/禁用", "绑定冷藏间"], True)
    paths["3_9"] = classic_actor_usecases(
        "fig3_9_device_mgmt.png", "冷库设备运维管理系统", "管理员",
        ["设备增删改查", "状态变更", "查看状态历史", "Excel导入", "Excel导出"], True)
    paths["3_10"] = classic_actor_usecases(
        "fig3_10_order_admin.png", "冷库设备运维管理系统", "管理员",
        ["查看全量工单", "派单", "转派", "撤回", "关闭/重开", "归档"], True)
    return paths


# ---- minimal helpers to replace images inside existing docx ----
def replace_media_in_docx(docx_path, mapping):
    """
    mapping: { 'fig3_2_client.png': Path(...), ... }
    Match by scanning document.xml.rels Target endings, replace media binaries.
    """
    import zipfile
    import tempfile
    import shutil
    docx_path = Path(docx_path)
    if not docx_path.exists():
        return False
    tmp = Path(tempfile.mkdtemp())
    extract = tmp / "x"
    with zipfile.ZipFile(docx_path, "r") as z:
        z.extractall(extract)
    rels = (extract / "word" / "_rels" / "document.xml.rels").read_text(encoding="utf-8")
    # Also try to match by reading doc and finding image relationships - simpler: replace ALL media that we know from rId order is hard.
    # Better approach: rebuild doc from scratch using existing build - skip.
    # Match filename in rels if we embedded with known names - python-docx uses image1.png etc.
    # So we need to rebuild the word document.
    shutil.rmtree(tmp, ignore_errors=True)
    return False


def set_run_font(run, name_cn="宋体", name_en="Times New Roman", size=12, bold=False):
    run.bold = bold
    run.font.size = Pt(size)
    run.font.name = name_en
    rPr = run._element.get_or_add_rPr()
    rFonts = rPr.get_or_add_rFonts()
    rFonts.set(qn("w:eastAsia"), name_cn)


def add_para(doc, text, *, size=12, bold=False, align="left", first_indent=True, space_after=6, cn="宋体"):
    p = doc.add_paragraph()
    p.alignment = {
        "center": WD_ALIGN_PARAGRAPH.CENTER,
        "right": WD_ALIGN_PARAGRAPH.RIGHT,
    }.get(align, WD_ALIGN_PARAGRAPH.JUSTIFY)
    pf = p.paragraph_format
    pf.space_after = Pt(space_after)
    pf.line_spacing = 1.5
    if first_indent and align == "left":
        pf.first_line_indent = Cm(0.74)
    run = p.add_run(text)
    set_run_font(run, name_cn=cn, size=size, bold=bold)


def add_heading_cn(doc, text, level=1):
    sizes = {1: 16, 2: 14, 3: 12}
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER if level == 1 else WD_ALIGN_PARAGRAPH.LEFT
    p.paragraph_format.space_before = Pt(12 if level == 1 else 8)
    p.paragraph_format.space_after = Pt(10 if level == 1 else 6)
    p.paragraph_format.line_spacing = 1.5
    run = p.add_run(text)
    set_run_font(run, name_cn="黑体", size=sizes.get(level, 12), bold=True)


def add_caption(doc, text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.space_after = Pt(10)
    run = p.add_run(text)
    set_run_font(run, size=10.5)


def add_image(doc, path, width_cm=13.5):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.add_run().add_picture(str(path), width=Cm(width_cm))


def swap_images_in_existing_docs(uc_files):
    """
    打开已有 docx，按“图3.x”题注顺序替换紧挨其上的图片。
    """
    from docx.oxml.ns import qn as qn2
    import copy

    def process(path, save_as=None):
        path = Path(path)
        if not path.exists():
            print("skip missing", path)
            return
        doc = Document(str(path))
        caption_map = {
            "图3.1": "fig3_1_role.png",
            "图3.2": "fig3_2_client.png",
            "图3.3": "fig3_3_device_view.png",
            "图3.4": "fig3_4_submit.png",
            "图3.5": "fig3_5_ops.png",
            "图3.6": "fig3_6_process.png",
            "图3.7": "fig3_7_admin.png",
            "图3.8": "fig3_8_user_mgmt.png",
            "图3.9": "fig3_9_device_mgmt.png",
            "图3.10": "fig3_10_order_admin.png",
        }
        paras = list(doc.paragraphs)
        replaced = 0
        for i, p in enumerate(paras):
            text = (p.text or "").replace(" ", "").replace("　", "")
            hit = None
            for key in sorted(caption_map.keys(), key=len, reverse=True):
                k = key.replace(" ", "")
                if text.startswith(k):
                    hit = caption_map[key]
                    break
            if not hit:
                continue
            img_path = FIG / hit
            if not img_path.exists():
                continue
            j = i - 1
            while j >= 0:
                if paras[j]._p.xpath(".//*[local-name()='drawing']"):
                    break
                if paras[j].text.strip():
                    j -= 1
                    continue
                j -= 1
            if j < 0:
                continue
            prev = paras[j]
            if not prev._p.xpath(".//*[local-name()='drawing']"):
                continue
            for child in list(prev._p):
                prev._p.remove(child)
            run = prev.add_run()
            run.add_picture(str(img_path), width=Cm(13.2))
            prev.alignment = WD_ALIGN_PARAGRAPH.CENTER
            replaced += 1
        out = Path(save_as) if save_as else path
        try:
            doc.save(str(out))
            print("replaced", replaced, "->", out.name)
        except PermissionError:
            alt = out.with_name(out.stem + "-用例图已改样式" + out.suffix)
            doc.save(str(alt))
            print("file locked, saved as", alt.name, "replaced", replaced)

    process(OUT)
    process(OUT3)


def main():
    redraw_all_usecases()
    print("usecase figs redrawn in classic UML style")
    swap_images_in_existing_docs({})
    print("done", OUT)


if __name__ == "__main__":
    main()
