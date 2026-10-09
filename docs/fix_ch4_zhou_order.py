# -*- coding: utf-8 -*-
"""
按周思汉第四章结构修正：
  图4.1 架构（框内对齐、不出框）
  图4.2 系统功能图（树形+竖排叶子，仿范本）
  图4.3 用户端流程图
  图4.4 管理端流程图
  图4.5 E-R 图
编号顺序：4.1→4.2→4.3→4.4→4.5，写入一份 Word。
"""
from pathlib import Path
import shutil
import matplotlib.pyplot as plt
from matplotlib.patches import Rectangle, FancyBboxPatch, Polygon
from docx import Document
from docx.shared import Pt, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn

ROOT = Path(r"f:\plcmin")
FIG = ROOT / "docs" / "thesis_figs"
FIG.mkdir(parents=True, exist_ok=True)
FINAL = ROOT / "docs" / "冷库设备运维管理系统-论文第1-4章.docx"
SRC_CANDIDATES = [
    ROOT / "docs" / "冷库设备运维管理系统-论文第1-4章-已更新.docx",
    ROOT / "docs" / "冷库设备运维管理系统-论文第1-4章-用例图已改格式.docx",
    FINAL,
]

plt.rcParams["font.sans-serif"] = ["SimSun", "Microsoft YaHei", "SimHei"]
plt.rcParams["axes.unicode_minus"] = False


def save(fig, name):
    p = FIG / name
    # 不用 tight 裁切，避免外框被吃掉导致“出框”错觉；内容已严格落在 xlim/ylim 内
    fig.savefig(p, dpi=200, bbox_inches=None, facecolor="white", pad_inches=0.15)
    plt.close(fig)
    return p


def set_run_font(run, name_cn="宋体", name_en="Times New Roman", size=12, bold=False):
    run.bold = bold
    run.font.size = Pt(size)
    run.font.name = name_en
    rPr = run._element.get_or_add_rPr()
    rFonts = rPr.get_or_add_rFonts()
    rFonts.set(qn("w:eastAsia"), name_cn)


def add_para(doc, text, *, size=12, bold=False, align="left", first_indent=True, space_after=6, cn="宋体"):
    p = doc.add_paragraph()
    if align == "center":
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    else:
        p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    pf = p.paragraph_format
    pf.space_after = Pt(space_after)
    pf.line_spacing = 1.5
    if first_indent and align == "left":
        pf.first_line_indent = Cm(0.74)
    run = p.add_run(text)
    set_run_font(run, name_cn=cn, size=size, bold=bold)
    return p


def add_h(doc, text, level=1):
    # 用普通段落模拟标题，避免样式依赖
    size = {1: 16, 2: 14, 3: 12}.get(level, 12)
    cn = "黑体"
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.LEFT
    p.paragraph_format.space_before = Pt(12 if level <= 2 else 8)
    p.paragraph_format.space_after = Pt(6)
    p.paragraph_format.first_line_indent = Cm(0)
    run = p.add_run(text)
    set_run_font(run, name_cn=cn, size=size, bold=True)
    return p


def add_cap(doc, text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.space_after = Pt(10)
    set_run_font(p.add_run(text), size=10.5)


def add_img(doc, path, w=14.0):
    if not Path(path).exists():
        return
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.add_run().add_picture(str(path), width=Cm(w))


def add_table(doc, title, headers, rows):
    add_para(doc, title, bold=True, align="center", first_indent=False, size=10.5)
    table = doc.add_table(rows=1 + len(rows), cols=len(headers))
    table.style = "Table Grid"
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    for j, h in enumerate(headers):
        table.rows[0].cells[j].text = h
    for i, row in enumerate(rows):
        for j, val in enumerate(row):
            table.rows[i + 1].cells[j].text = str(val)
    for row in table.rows:
        for cell in row.cells:
            for p in cell.paragraphs:
                for r in p.runs:
                    set_run_font(r, size=9)
    add_para(doc, "", first_indent=False, space_after=4)


# ===================== 图4.1 架构（严格不出框） =====================
def fig_arch():
    W, H = 12.0, 8.4
    fig, ax = plt.subplots(figsize=(W, H))
    ax.set_xlim(0, W)
    ax.set_ylim(0, H)
    ax.axis("off")

    def rbox(x, y, w, h, text, fs=9.5, fc="#FFFFFF", lw=1.15):
        ax.add_patch(Rectangle((x, y), w, h, fill=True, facecolor=fc, edgecolor="black", lw=lw, clip_on=True))
        ax.text(x + w / 2, y + h / 2, text, ha="center", va="center", fontsize=fs, clip_on=True)

    def vlabel(x, y, w, h, text, fs=12):
        rbox(x, y, w, h, "", fs=fs, fc="#FAFAFA", lw=1.35)
        ax.text(x + w / 2, y + h / 2, text, ha="center", va="center", fontsize=fs, rotation=90)

    # 外边距：整图四周留白，内容绝不贴边
    m = 0.28
    # ===== 前端 =====
    fe_x, fe_y, fe_w, fe_h = m, 5.05, W - 2 * m, 3.05
    ax.add_patch(Rectangle((fe_x, fe_y), fe_w, fe_h, fill=False, ec="black", lw=1.7))
    vlabel(fe_x + 0.08, fe_y + 0.1, 0.5, fe_h - 0.2, "前端", 13)
    rbox(fe_x + 0.68, fe_y + 0.1, 0.62, fe_h - 0.2, "视图层", 10.5, "#F5F5F5")

    web_x = fe_x + 1.45
    web_y = fe_y + 0.18
    web_w = fe_x + fe_w - 0.12 - web_x
    web_h = fe_h - 0.36
    ax.add_patch(Rectangle((web_x, web_y), web_w, web_h, fill=False, ec="black", lw=1.15))
    ax.text(web_x + web_w / 2, web_y + web_h - 0.22, "Web 管理端（浏览器）", ha="center", va="center", fontsize=11)

    # 三角色页：均分，左右各留 0.12
    pages = [
        "管理员页面\n用户/冷库设备/工单派单\n维保备件/统计/日志",
        "运维人员页面\n工作台/工单处理\n故障案例/维保任务",
        "甲方用户页面\n设备浏览/报修验收\n评价/消息/个人中心",
    ]
    gap = 0.12
    pad = 0.14
    pw = (web_w - 2 * pad - 2 * gap) / 3
    py = web_y + 1.05
    ph = 1.35
    for i, t in enumerate(pages):
        rbox(web_x + pad + i * (pw + gap), py, pw, ph, t, 8.5)

    techs = ["Vue2", "ElementUI", "ECharts", "Axios", "Vue Router", "Vite"]
    tg, tpad = 0.1, 0.14
    tw = (web_w - 2 * tpad - tg * (len(techs) - 1)) / len(techs)
    ty = web_y + 0.22
    for i, t in enumerate(techs):
        rbox(web_x + tpad + i * (tw + tg), ty, tw, 0.55, t, 9)

    # ===== 后端 =====
    be_x, be_y, be_w, be_h = m, m, W - 2 * m, 4.5
    ax.add_patch(Rectangle((be_x, be_y), be_w, be_h, fill=False, ec="black", lw=1.7))
    vlabel(be_x + 0.08, be_y + 0.1, 0.5, be_h - 0.2, "后端", 13)

    layers = [
        (be_y + 3.35, "控制层", ["JWT拦截器", "Controller", "统一Result", "CORS", "WebSocket"]),
        (be_y + 2.3, "业务逻辑层", ["认证/冷库/设备", "工单状态机", "故障维保", "备件库存", "统计消息Excel"]),
        (be_y + 1.25, "数据访问层", ["MyBatis-Plus Mapper", "实体 Entity", "分页插件"]),
        (be_y + 0.2, "数据层", ["MySQL 业务库", "Redis 缓存(可降级)", "本地文件 uploads"]),
    ]
    label_w = 1.25
    label_x = be_x + 0.68
    # 内容区右边界：外框内侧再缩 0.12
    content_left = label_x + label_w + 0.12
    content_right = be_x + be_w - 0.12
    avail = content_right - content_left
    for y, name, items in layers:
        rbox(label_x, y, label_w, 0.85, name, 10.5, "#F5F5F5")
        n = len(items)
        ig = 0.1
        iw = (avail - ig * (n - 1)) / n
        for i, it in enumerate(items):
            rbox(content_left + i * (iw + ig), y + 0.12, iw, 0.6, it, 8.8)

    return save(fig, "fig4_1_arch.png")


# ===================== 图4.2 系统功能图（树形竖排） =====================
def fig_func_tree():
    """仿范本：根→用户端/管理端→角色→竖排功能叶子；组间留空，竖框互不黏连。"""
    W, H = 15.2, 8.4
    fig, ax = plt.subplots(figsize=(W, H))
    ax.set_xlim(0, W)
    ax.set_ylim(0, H)
    ax.axis("off")

    def hbox(cx, cy, w, h, text, fs=10):
        x, y = cx - w / 2, cy - h / 2
        ax.add_patch(Rectangle((x, y), w, h, fill=True, facecolor="white", edgecolor="black", lw=0.9))
        ax.text(cx, cy, text, ha="center", va="center", fontsize=fs)
        return cx, cy

    def vbox(cx, y_top, w, h, text, fs=8.5):
        x, y = cx - w / 2, y_top - h
        ax.add_patch(Rectangle((x, y), w, h, fill=True, facecolor="white", edgecolor="black", lw=0.85))
        ax.text(cx, y + h / 2, "\n".join(list(text)), ha="center", va="center", fontsize=fs, linespacing=1.08)

    def wire(x1, y1, x2, y2):
        ax.plot([x1, x2], [y1, y2], "k-", lw=0.85)

    root = "基于Spring Boot+Vue的冷库设备综合运维管理系统"
    rcx, rcy = hbox(W / 2, 7.75, 10.0, 0.52, root, 11)
    root_bottom = rcy - 0.26

    # 角色与叶子：先算叶子坐标，再反推角色中心（组间强制留白）
    roles = [
        ("甲方用户", ["设备浏览", "提交报修", "验收确认", "服务评价", "消息中心", "个人中心"]),
        ("运维人员", ["接单处理", "过程备件", "提交完工", "维保任务", "故障案例", "备件领用", "个人中心"]),
        ("管理员", ["用户管理", "冷库设备", "工单派单", "维保计划", "备件供应商", "数据统计", "操作日志", "个人中心"]),
    ]
    leaf_w, leaf_h = 0.40, 3.45
    leaf_gap = 0.20          # 组内竖框间距
    group_gap = 0.75         # 角色组之间空隙（防黏连）
    leaf_top = 4.5
    # 先算总宽，再左右居中
    spans = []
    for _, leaves in roles:
        n = len(leaves)
        spans.append(n * leaf_w + (n - 1) * leaf_gap)
    total_w = sum(spans) + group_gap * (len(roles) - 1)
    margin_l = max(0.4, (W - total_w) / 2)


    placed = []  # (role_name, role_cx, leaf_xs)
    x = margin_l
    for name, leaves in roles:
        n = len(leaves)
        span = n * leaf_w + (n - 1) * leaf_gap
        leaf_xs = [x + leaf_w / 2 + i * (leaf_w + leaf_gap) for i in range(n)]
        role_cx = (leaf_xs[0] + leaf_xs[-1]) / 2
        placed.append((name, role_cx, leaf_xs, leaves))
        x = x + span + group_gap

    # 第二层：用户端跨前两组中心，管理端在管理员中心
    y2 = 6.7
    user_cx = (placed[0][1] + placed[1][1]) / 2
    admin_cx = placed[2][1]
    hbox(user_cx, y2, 2.2, 0.46, "用户端", 11)
    hbox(admin_cx, y2, 2.2, 0.46, "管理端", 11)
    bus_y = (root_bottom + y2 + 0.23) / 2
    wire(rcx, root_bottom, rcx, bus_y)
    wire(user_cx, bus_y, admin_cx, bus_y)
    wire(user_cx, bus_y, user_cx, y2 + 0.23)
    wire(admin_cx, bus_y, admin_cx, y2 + 0.23)

    # 第三层角色
    y3 = 5.55
    mid23 = (y2 - 0.23 + y3 + 0.23) / 2
    wire(user_cx, y2 - 0.23, user_cx, mid23)
    wire(placed[0][1], mid23, placed[1][1], mid23)
    wire(placed[0][1], mid23, placed[0][1], y3 + 0.23)
    wire(placed[1][1], mid23, placed[1][1], y3 + 0.23)
    wire(admin_cx, y2 - 0.23, admin_cx, y3 + 0.23)

    for name, role_cx, leaf_xs, leaves in placed:
        hbox(role_cx, y3, 1.85, 0.46, name, 10.5)
        mid = (y3 - 0.23 + leaf_top) / 2
        wire(role_cx, y3 - 0.23, role_cx, mid)
        wire(leaf_xs[0], mid, leaf_xs[-1], mid)
        for lx, t in zip(leaf_xs, leaves):
            wire(lx, mid, lx, leaf_top)
            vbox(lx, leaf_top, leaf_w, leaf_h, t, 8.2)

    return save(fig, "fig4_2_func.png")


# ===================== 流程图公共件 =====================
def _flow_rect(ax, cx, cy, w, h, text, fs=9):
    ax.add_patch(Rectangle((cx - w / 2, cy - h / 2), w, h, fill=True, facecolor="white",
                            edgecolor="black", lw=1.1, zorder=5))
    ax.text(cx, cy, text, ha="center", va="center", fontsize=fs, zorder=6)


def _flow_oval(ax, cx, cy, w, h, text, fs=9):
    ax.add_patch(FancyBboxPatch((cx - w / 2, cy - h / 2), w, h, boxstyle="round,pad=0.02,rounding_size=0.35",
                                facecolor="white", edgecolor="black", lw=1.1, zorder=5))
    ax.text(cx, cy, text, ha="center", va="center", fontsize=fs, zorder=6)


def _flow_diamond(ax, cx, cy, w, h, text, fs=8.5):
    pts = [(cx, cy + h / 2), (cx + w / 2, cy), (cx, cy - h / 2), (cx - w / 2, cy)]
    ax.add_patch(Polygon(pts, closed=True, facecolor="white", edgecolor="black", lw=1.1, zorder=5))
    ax.text(cx, cy, text, ha="center", va="center", fontsize=fs, zorder=6)


def _arr(ax, x1, y1, x2, y2):
    ax.annotate("", xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle="->", color="black", lw=1.0))


def fig_user_flow():
    """图4.3 用户端流程图：登录后区分甲方 / 运维。"""
    W, H = 11.5, 9.0
    fig, ax = plt.subplots(figsize=(W, H))
    ax.set_xlim(0, W)
    ax.set_ylim(0, H)
    ax.axis("off")

    _flow_oval(ax, 5.75, 8.55, 1.4, 0.45, "开始")
    _arr(ax, 5.75, 8.32, 5.75, 7.95)
    _flow_rect(ax, 5.75, 7.7, 2.2, 0.45, "登录系统")
    _arr(ax, 5.75, 7.47, 5.75, 7.15)
    _flow_diamond(ax, 5.75, 6.7, 2.6, 0.85, "是否为\n甲方用户")

    # 是 → 甲方
    _arr(ax, 4.45, 6.7, 2.6, 6.7)
    ax.text(3.5, 6.9, "是", fontsize=9)
    _flow_rect(ax, 2.0, 6.7, 1.5, 0.45, "甲方用户")
    _arr(ax, 2.0, 6.47, 2.0, 5.95)
    _flow_rect(ax, 2.0, 5.7, 1.8, 0.45, "设备浏览")
    _arr(ax, 2.0, 5.47, 2.0, 5.05)
    _flow_rect(ax, 2.0, 4.8, 1.8, 0.45, "提交报修")
    _arr(ax, 2.0, 4.57, 2.0, 4.15)
    _flow_rect(ax, 2.0, 3.9, 1.8, 0.45, "验收/评价")
    _arr(ax, 2.0, 3.67, 2.0, 3.2)
    _flow_rect(ax, 2.0, 2.95, 1.8, 0.45, "消息/个人中心")

    # 否 → 运维
    _arr(ax, 7.05, 6.7, 9.0, 6.7)
    ax.text(8.0, 6.9, "否", fontsize=9)
    _flow_rect(ax, 9.5, 6.7, 1.5, 0.45, "运维人员")
    _arr(ax, 9.5, 6.47, 9.5, 5.95)
    _flow_rect(ax, 9.5, 5.7, 1.8, 0.45, "运维工作台")
    _arr(ax, 9.5, 5.47, 9.5, 5.05)
    _flow_rect(ax, 9.5, 4.8, 1.8, 0.45, "接单/处理工单")
    _arr(ax, 9.5, 4.57, 9.5, 4.15)
    _flow_rect(ax, 9.5, 3.9, 1.8, 0.45, "维保/故障案例")
    _arr(ax, 9.5, 3.67, 9.5, 3.2)
    _flow_rect(ax, 9.5, 2.95, 1.8, 0.45, "消息/个人中心")

    # 汇合到结束
    ax.plot([2.0, 2.0], [2.72, 1.7], "k-", lw=1.0)
    ax.plot([9.5, 9.5], [2.72, 1.7], "k-", lw=1.0)
    ax.plot([2.0, 9.5], [1.7, 1.7], "k-", lw=1.0)
    _arr(ax, 5.75, 1.7, 5.75, 1.25)
    _flow_oval(ax, 5.75, 0.95, 1.4, 0.45, "结束")

    return save(fig, "fig4_3_user_flow.png")


def fig_admin_flow():
    """
    图4.4 管理端流程图 —— 对齐周思汉图4.3：
    开始→登录→概览→上总线扇出→功能框→下总线汇合→结束。
    中轴对准中间功能框（竖线进框/出框），禁止落在两框空隙造成“穿空线”。
    """
    W, H = 12.2, 7.8
    fig, ax = plt.subplots(figsize=(W, H))
    ax.set_xlim(0, W)
    ax.set_ylim(0, H)
    ax.axis("off")

    def line(x1, y1, x2, y2):
        ax.plot([x1, x2], [y1, y2], "k-", lw=1.05, solid_capstyle="butt", zorder=1)

    # 5 个功能 + 个人信息；中轴对准第 3 个「工单派单管控」
    names = ["用户管理", "冷库/设备台账", "工单派单管控", "维保/备件", "统计/日志", "个人信息/密码"]
    n = len(names)
    box_w = 1.65
    gap = 0.22
    total = n * box_w + (n - 1) * gap
    x0 = (W - total) / 2 + box_w / 2
    xs = [x0 + i * (box_w + gap) for i in range(n)]
    cx = xs[2]  # 中轴 = 工单派单管控

    box_h = 0.55
    box_cy = 4.35
    box_top = box_cy + box_h / 2
    box_bot = box_cy - box_h / 2
    top_bus = 5.2
    bot_bus = 3.5

    _flow_oval(ax, cx, 7.35, 1.5, 0.4, "开始")
    _arr(ax, cx, 7.15, cx, 6.75)
    _flow_rect(ax, cx, 6.5, 2.5, 0.4, "登录管理端")
    _arr(ax, cx, 6.3, cx, 5.9)
    _flow_rect(ax, cx, 5.65, 2.5, 0.4, "数据概览")
    _arr(ax, cx, 5.45, cx, top_bus)

    # 上总线 + 落到各框顶（在框外收住，不穿框）
    line(xs[0], top_bus, xs[-1], top_bus)
    for x, name in zip(xs, names):
        line(x, top_bus, x, box_top + 0.03)
        _flow_rect(ax, x, box_cy, box_w, box_h, name, 8.5)

    # 下总线：框底外侧短竖线止于总线；再从总线中轴到结束
    for x in xs:
        line(x, box_bot - 0.03, x, bot_bus)
    line(xs[0], bot_bus, xs[-1], bot_bus)
    _arr(ax, cx, bot_bus, cx, 2.9)
    _flow_oval(ax, cx, 2.55, 1.5, 0.4, "结束")

    return save(fig, "fig4_4_admin_flow.png")


def fig_er():
    """图4.5 E-R（复用并改存为 fig4_5_er.png）。"""
    # 若旧 fig4_3_er 存在可先画新版
    W, H = 12.5, 8.2
    fig, ax = plt.subplots(figsize=(W, H))
    ax.set_xlim(0, W)
    ax.set_ylim(0, H)
    ax.axis("off")

    def ent(x, y, w, h, title, fields):
        ax.add_patch(Rectangle((x, y + h - 0.35), w, 0.35, facecolor="#222", edgecolor="black", lw=1))
        ax.text(x + w / 2, y + h - 0.175, title, ha="center", va="center", fontsize=8, color="white")
        ax.add_patch(Rectangle((x, y), w, h - 0.35, facecolor="white", edgecolor="black", lw=1))
        ax.text(x + 0.08, y + h - 0.5, "\n".join(fields), ha="left", va="top", fontsize=7)

    def rel(x1, y1, x2, y2, label=""):
        ax.annotate("", xy=(x2, y2), xytext=(x1, y1),
                    arrowprops=dict(arrowstyle="<->", color="#444", lw=1.0))
        if label:
            ax.text((x1 + x2) / 2, (y1 + y2) / 2 + 0.1, label, fontsize=7, ha="center")

    ent(0.3, 6.2, 2.3, 1.7, "sys_user", ["id PK", "username", "password", "role", "status"])
    ent(3.0, 6.2, 2.4, 1.7, "cold_storage", ["id PK", "name", "address", "status", "contact"])
    ent(5.8, 6.2, 2.4, 1.7, "cold_room", ["id PK", "storage_id FK", "name", "code", "temp"])
    ent(8.6, 6.2, 2.6, 1.7, "device", ["id PK", "room_id FK", "device_no", "status", "type"])

    ent(0.3, 3.7, 2.6, 2.0, "work_order", ["id PK", "device_id FK", "order_no", "status", "submit/assignee"])
    ent(3.3, 3.7, 2.5, 2.0, "fault_record", ["id PK", "device_id FK", "order_id FK", "fault_type", "level"])
    ent(6.2, 3.7, 2.5, 2.0, "maintain_plan", ["id PK", "plan_no", "cycle_type", "device_type", "due"])
    ent(9.1, 3.7, 2.6, 2.0, "maintain_task", ["id PK", "plan_id FK", "device_id FK", "assignee", "status"])

    ent(0.3, 0.8, 2.5, 2.0, "spare_part", ["id PK", "supplier_id FK", "part_no", "stock", "safety"])
    ent(3.2, 0.8, 2.5, 2.0, "stock_record", ["id PK", "part_id FK", "change_type", "qty", "biz"])
    ent(6.1, 0.8, 2.5, 2.0, "supplier", ["id PK", "name", "phone", "status"])
    ent(9.0, 0.8, 2.7, 2.0, "sys_message", ["id PK", "user_id FK", "msg_type", "is_read", "biz"])

    rel(5.3, 7.0, 5.8, 7.0, "1:N")
    rel(8.2, 7.0, 8.6, 7.0, "1:N")
    rel(9.9, 6.2, 9.5, 5.7, "1:N工单")
    rel(1.6, 6.2, 1.5, 5.7, "报修/派单")
    rel(8.6, 4.5, 9.1, 4.5, "计划→任务")
    rel(2.8, 1.8, 3.2, 1.8, "1:N流水")
    rel(5.7, 1.8, 6.1, 1.8, "供应")

    p = save(fig, "fig4_5_er.png")
    # 兼容旧引用名
    shutil.copy(p, FIG / "fig4_3_er.png")
    return p


def redraw_all_ch4_figs():
    fig_arch()
    fig_func_tree()
    fig_user_flow()
    fig_admin_flow()
    fig_er()
    print("ch4 figs ok")


# ===================== Word：截断旧第4章并重写 =====================
def _para_text(el):
    texts = el.xpath(".//*[local-name()='t']")
    return "".join((t.text or "") for t in texts)


def truncate_from_ch4(doc):
    """只截断正文第4章。目录里也会出现「第4章」，必须取最后一次出现，否则会误删第1–3章。"""
    body = doc.element.body
    children = list(body)
    hits = []
    for i, child in enumerate(children):
        t = _para_text(child).replace(" ", "").replace("　", "")
        if t.startswith("第4章") or t.startswith("第四章"):
            hits.append(i)
    if not hits:
        raise SystemExit("未找到第4章标题，无法替换")
    start = hits[-1]  # 正文标题（目录为第一次）
    for child in children[start:]:
        if child.tag.endswith("sectPr"):
            continue
        body.remove(child)
    return start


def append_ch4(doc):
    f = lambda n: FIG / n
    add_h(doc, "第4章　系统设计", 1)
    add_para(
        doc,
        "在前述需求分析基础上，本章对冷库设备综合运维管理系统进行系统设计，包括技术架构、功能结构、用户端与管理端流程设计以及数据库设计。"
        "设计内容与第3章用例保持一致。",
    )

    add_h(doc, "4.1　系统技术架构设计", 2)
    add_h(doc, "4.1.1　系统软件层次架构", 3)
    add_para(
        doc,
        "本系统整体采用前后端分离架构。前端为 Vue2 单页应用，按管理员、运维人员、甲方用户三类角色组织页面；"
        "后端基于 Spring Boot，按控制层、业务逻辑层、数据访问层与数据层分层实现，并结合 JWT 鉴权、Redis 可降级缓存与 WebSocket 消息推送。"
        "系统架构如图4.1所示。",
    )
    add_img(doc, f("fig4_1_arch.png"), 14.2)
    add_cap(doc, "图4.1　系统架构图")

    add_h(doc, "4.1.2　系统功能结构", 3)
    add_para(
        doc,
        "系统功能按“用户端—管理端”划分。用户端面向甲方用户与运维人员：甲方侧重设备浏览、报修、验收评价与消息；"
        "运维侧重接单处理、过程与备件填报、提交完工、维保任务与故障案例。管理端由管理员负责用户权限、冷库设备台账、工单派单管控、"
        "维保计划、备件供应商、数据统计与操作日志等。系统功能图如图4.2所示。",
    )
    add_img(doc, f("fig4_2_func.png"), 14.5)
    add_cap(doc, "图4.2　系统功能图")

    add_h(doc, "4.2　系统的功能设计", 2)
    add_para(doc, "系统主要分为用户端与管理端两大功能模块，下面分别进行功能设计说明。")

    add_h(doc, "4.2.1　用户端", 3)
    add_para(
        doc,
        "用户端面向甲方用户与运维人员。用户登录后，系统根据角色区分权限与菜单。"
        "甲方用户可浏览绑定冷藏间/公共设备、提交报修、对完工工单进行验收与评价，并查看消息与维护个人信息；"
        "运维人员可在工作台查看待办，对接派工单进行接单、填报处理过程与备件出库、上传维修图片并提交完工，同时执行维保任务、维护故障案例。"
        "用户端流程图如图4.3所示。",
    )
    add_img(doc, f("fig4_3_user_flow.png"), 13.5)
    add_cap(doc, "图4.3　用户端流程图")

    add_h(doc, "4.2.2　管理端", 3)
    add_para(
        doc,
        "管理端面向系统管理员。管理员登录后可查看数据概览，完成用户启停与权限维护，管理冷库/冷藏间与设备台账，"
        "对工单进行派单、转派、撤回与关闭归档，配置维保计划并跟踪任务，维护备件与供应商及库存流水，查看统计报表与操作日志，"
        "并可修改个人信息与登录密码。管理端流程图如图4.4所示。",
    )
    add_img(doc, f("fig4_4_admin_flow.png"), 13.5)
    add_cap(doc, "图4.4　管理端流程图")

    add_h(doc, "4.3　数据库设计", 2)
    add_para(
        doc,
        "在技术架构与功能设计基础上进行数据库设计。业务数据以 MySQL 结构化存储为主，Redis 用于 Token 黑名单等可降级缓存，上传文件存于本地 uploads 目录。",
    )
    add_h(doc, "4.3.1　数据库的概念设计", 3)
    add_para(
        doc,
        "概念模型围绕用户、冷库—冷藏间—设备、运维工单、故障与维保、备件与供应商、消息等实体展开。"
        "冷库包含多个冷藏间，冷藏间挂载设备；设备产生工单与故障记录，并关联维保计划任务；备件归属供应商并产生库存流水；消息按用户投递。"
        "系统 E-R 图如图4.5所示。",
    )
    add_img(doc, f("fig4_5_er.png"), 14.2)
    add_cap(doc, "图4.5　冷库设备运维管理系统E-R图")

    add_h(doc, "4.3.2　数据库的逻辑设计", 3)
    add_para(doc, "系统共 19 张业务表。以下选取核心表说明结构，其余表与之关联支撑完整业务。")
    add_table(doc, "表4.1　用户表 sys_user", ["字段", "类型", "说明"], [
        ["id", "BIGINT", "主键"], ["username", "VARCHAR", "登录名"], ["password", "VARCHAR", "BCrypt"],
        ["role", "VARCHAR", "ADMIN/OPS/CLIENT"], ["status", "TINYINT", "启停"],
    ])
    add_table(doc, "表4.2　冷库表 cold_storage", ["字段", "类型", "说明"], [
        ["id", "BIGINT", "主键"], ["name", "VARCHAR", "名称"], ["address", "VARCHAR", "地址"], ["status", "VARCHAR", "状态"],
    ])
    add_table(doc, "表4.3　冷藏间表 cold_room", ["字段", "类型", "说明"], [
        ["id", "BIGINT", "主键"], ["storage_id", "BIGINT", "冷库FK"], ["name/code", "VARCHAR", "名称/编码"],
    ])
    add_table(doc, "表4.4　设备表 device", ["字段", "类型", "说明"], [
        ["id", "BIGINT", "主键"], ["device_no", "VARCHAR", "编号"], ["room_id", "BIGINT", "冷藏间FK"],
        ["status", "VARCHAR", "生命周期状态"], ["is_public", "TINYINT", "公共设备"],
    ])
    add_table(doc, "表4.5　工单表 work_order", ["字段", "类型", "说明"], [
        ["id", "BIGINT", "主键"], ["order_no", "VARCHAR", "工单号"], ["device_id", "BIGINT", "设备FK"],
        ["status", "VARCHAR", "七状态"], ["submit_user_id/assignee_id", "BIGINT", "报修人/运维"],
    ])
    add_table(doc, "表4.6　备件表 spare_part", ["字段", "类型", "说明"], [
        ["id", "BIGINT", "主键"], ["part_no", "VARCHAR", "编号"], ["stock_qty", "INT", "库存"],
        ["safety_stock", "INT", "安全库存"], ["supplier_id", "BIGINT", "供应商FK"],
    ])
    add_table(doc, "表4.7　消息表 sys_message", ["字段", "类型", "说明"], [
        ["id", "BIGINT", "主键"], ["user_id", "BIGINT", "接收人"], ["msg_type", "VARCHAR", "类型"], ["is_read", "TINYINT", "已读"],
    ])
    add_para(
        doc,
        "其余表：user_cold_room、operation_log、device_status_log、work_order_part、fault_record、fault_case、"
        "maintain_plan、maintain_plan_item、maintain_task、maintain_task_item、supplier、stock_record，分别支撑绑定、审计、状态史、工单备件、故障案例、维保与库存流水。",
    )

    add_h(doc, "4.4　本章小结", 2)
    add_para(
        doc,
        "本章完成系统设计：给出层次架构图与系统功能图，按用户端与管理端分别给出流程图，并完成数据库概念结构与核心表逻辑设计，"
        "与第3章需求对应，为后续系统实现提供依据。",
    )


def update_toc_ch4_lines(doc):
    """目录里若仍写旧结构，尽量替换第4章相关行。"""
    for p in doc.paragraphs:
        t = (p.text or "").strip()
        if t.startswith("　　4.2") and "系统功能设计" in t:
            # 已基本一致
            pass
        # 修正摘要式目录已在原文，若有“类图/时序”字样可忽略


def patch_docx():
    # 优先用仍含第1–3章正文的备份；跳过已损坏的 FINAL（仅剩目录+第4章）
    src = None
    for p in SRC_CANDIDATES:
        if not p.exists():
            continue
        if p.resolve() == FINAL.resolve():
            # 检查是否还保留第1章正文（段落数过少则视为损坏）
            probe = Document(str(p))
            if len(probe.paragraphs) < 100:
                print("skip broken", p.name, "paras", len(probe.paragraphs))
                continue
        src = p
        break
    if src is None:
        raise SystemExit("找不到含第1–3章的完整论文 docx")
    print("source", src.name)
    doc = Document(str(src))
    truncate_from_ch4(doc)
    append_ch4(doc)
    from datetime import datetime
    try:
        doc.save(str(FINAL))
        out = FINAL
    except PermissionError:
        out = ROOT / "docs" / f"冷库设备运维管理系统-论文第1-4章-完整恢复-{datetime.now().strftime('%H%M%S')}.docx"
        doc.save(str(out))
    print("saved", out, "paras", len(Document(str(out)).paragraphs))


def main():
    # 图已生成过则直接补文档；需要重画时再调 redraw
    if not (FIG / "fig4_5_er.png").exists() or not (FIG / "fig4_2_func.png").exists():
        redraw_all_ch4_figs()
    else:
        print("reuse existing ch4 figs")
    patch_docx()


if __name__ == "__main__":
    main()
