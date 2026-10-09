# -*- coding: utf-8 -*-
"""在前三章 Word 基础上追加第4章系统设计，并更新封面/目录。"""
from pathlib import Path
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch, Rectangle, Circle, Ellipse
from docx import Document
from docx.shared import Pt, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn

ROOT = Path(r"f:\plcmin")
FIG = ROOT / "docs" / "thesis_figs"
FIG.mkdir(parents=True, exist_ok=True)
SRC = ROOT / "docs" / "冷库设备运维管理系统-论文前三章.docx"
OUT = ROOT / "docs" / "冷库设备运维管理系统-论文第1-4章.docx"

plt.rcParams["font.sans-serif"] = ["Microsoft YaHei", "SimHei", "SimSun"]
plt.rcParams["axes.unicode_minus"] = False


def savefig(fig, name):
    path = FIG / name
    fig.savefig(path, dpi=160, bbox_inches="tight", facecolor="white")
    plt.close(fig)
    return path


def box(ax, x, y, w, h, text, fc="#E6F4FF", ec="#1677FF", fs=9, bold=False):
    p = FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.02,rounding_size=0.04",
                       facecolor=fc, edgecolor=ec, lw=1.2)
    ax.add_patch(p)
    ax.text(x + w / 2, y + h / 2, text, ha="center", va="center", fontsize=fs,
            fontweight="bold" if bold else "normal", wrap=True)


def arrow(ax, x1, y1, x2, y2):
    ax.annotate("", xy=(x2, y2), xytext=(x1, y1),
                arrowprops=dict(arrowstyle="->", color="#333", lw=1.2))


def fig_architecture():
    fig, ax = plt.subplots(figsize=(11, 7.2))
    ax.set_xlim(0, 11)
    ax.set_ylim(0, 7.2)
    ax.axis("off")
    # layers top to bottom
    box(ax, 0.5, 6.1, 10, 0.8, "表现层（Vue2 + ElementUI + ECharts + Axios + Vue Router）\n登录 / 冷库设备 / 工单维保 / 备件 / 统计 / 消息中心",
        fc="#FFF7E6", ec="#D48806", fs=9, bold=True)
    arrow(ax, 5.5, 6.1, 5.5, 5.7)
    box(ax, 0.5, 4.7, 10, 0.9, "接口与网关层（Spring MVC Controller + JWT 拦截器 + CORS）\nREST /api/**    WebSocket /ws/messages    静态 /uploads",
        fc="#E6F4FF", ec="#1677FF", fs=9, bold=True)
    arrow(ax, 5.5, 4.7, 5.5, 4.3)
    box(ax, 0.5, 3.1, 10, 1.1,
        "业务服务层（Service）\n认证鉴权 | 冷库/冷藏间 | 设备生命周期 | 工单状态机 | 故障/案例\n维保计划任务 | 备件库存 | 健康分析 | 统计 | 消息推送 | Excel导入导出",
        fc="#F6FFED", ec="#389E0D", fs=9, bold=True)
    arrow(ax, 5.5, 3.1, 5.5, 2.7)
    box(ax, 0.5, 1.7, 4.7, 0.9, "数据访问层\nMyBatis-Plus Mapper", fc="#F9F0FF", ec="#722ED1", fs=9, bold=True)
    box(ax, 5.8, 1.7, 4.7, 0.9, "基础设施\n本地文件上传 / POI / WS Handler", fc="#F9F0FF", ec="#722ED1", fs=9, bold=True)
    arrow(ax, 2.8, 1.7, 2.8, 1.3)
    arrow(ax, 8.1, 1.7, 8.1, 1.3)
    box(ax, 0.5, 0.25, 4.7, 0.9, "MySQL\n业务结构化数据", fc="#FFF1F0", ec="#CF1322", fs=9, bold=True)
    box(ax, 5.8, 0.25, 4.7, 0.9, "Redis（可降级本地）\nToken黑名单/会话标记", fc="#FFF1F0", ec="#CF1322", fs=9, bold=True)
    ax.set_title("图4.1 系统软件层次架构图", fontsize=12, pad=8)
    return savefig(fig, "fig4_1_arch.png")


def fig_function_tree():
    fig, ax = plt.subplots(figsize=(13.5, 7.5))
    ax.set_xlim(0, 13.5)
    ax.set_ylim(0, 7.5)
    ax.axis("off")
    box(ax, 4.5, 6.6, 4.5, 0.6, "冷库设备综合运维管理系统", fc="#1a6b7c", ec="#1a6b7c", fs=10, bold=True)
    # paint white text manually
    ax.texts[-1].set_color("white")

    modules = [
        (0.3, "认证权限\n个人中心\n操作日志"),
        (2.5, "冷库管理\n冷藏间\n库区办事台"),
        (4.7, "设备台账\n生命周期\n健康/档案"),
        (6.9, "运维工单\n七状态闭环"),
        (9.1, "故障记录\n案例库"),
        (11.1, "维保计划\n维保任务"),
    ]
    for x, t in modules:
        arrow(ax, 6.75, 6.6, x + 1.0, 5.55)
        box(ax, x, 4.85, 2.0, 0.7, t, fc="#E8F4FC", ec="#1a6b7c", fs=8)

    lower = [
        (1.2, "备件库存\n出入库流水"),
        (4.0, "供应商管理"),
        (6.5, "数据统计中心"),
        (9.0, "消息中心\nWebSocket"),
        (11.2, "Excel导入导出\n文件上传"),
    ]
    for x, t in lower:
        arrow(ax, 6.75, 6.6, x + 1.0, 3.9)
        box(ax, x, 3.1, 2.1, 0.7, t, fc="#F6FFED", ec="#389E0D", fs=8)

    # role notes
    box(ax, 0.8, 1.2, 3.5, 1.3, "甲方：报修/验收/评价\n只读设备与消息", fc="#FFF7E6", ec="#D48806", fs=8)
    box(ax, 5.0, 1.2, 3.5, 1.3, "运维：处理指派工单\n维保任务/故障案例", fc="#E6F4FF", ec="#1677FF", fs=8)
    box(ax, 9.2, 1.2, 3.5, 1.3, "管理员：派单管控\n台账/统计/用户权限", fc="#FFF1F0", ec="#CF1322", fs=8)
    ax.set_title("图4.2 系统功能结构图", fontsize=12, pad=8)
    return savefig(fig, "fig4_2_func.png")


def fig_er():
    fig, ax = plt.subplots(figsize=(12.5, 8.2))
    ax.set_xlim(0, 12.5)
    ax.set_ylim(0, 8.2)
    ax.axis("off")

    def ent(x, y, w, h, title, fields):
        box(ax, x, y + h - 0.35, w, 0.35, title, fc="#1a6b7c", ec="#1a6b7c", fs=8, bold=True)
        ax.texts[-1].set_color("white")
        body = FancyBboxPatch((x, y), w, h - 0.35, boxstyle="square,pad=0",
                              facecolor="#FAFAFA", edgecolor="#555", lw=1)
        ax.add_patch(body)
        ax.text(x + 0.08, y + h - 0.55, "\n".join(fields), ha="left", va="top", fontsize=7)

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

    # relations
    def rel(x1, y1, x2, y2, label=""):
        ax.annotate("", xy=(x2, y2), xytext=(x1, y1),
                    arrowprops=dict(arrowstyle="<->", color="#666", lw=1.0))
        if label:
            ax.text((x1 + x2) / 2, (y1 + y2) / 2 + 0.08, label, fontsize=7, color="#444", ha="center")

    rel(5.3, 7.0, 5.8, 7.0, "1:N")
    rel(8.2, 7.0, 8.6, 7.0, "1:N")
    rel(9.9, 6.2, 9.5, 5.7, "1:N工单")
    rel(1.6, 6.2, 1.5, 5.7, "报修/派单")
    rel(8.6, 4.5, 9.1, 4.5, "计划任务")
    rel(2.8, 1.8, 3.2, 1.8, "1:N流水")
    rel(5.7, 1.8, 6.1, 1.8, "供应")
    rel(1.5, 6.2, 1.4, 2.8)
    ax.set_title("图4.3 数据库概念结构（核心实体关系）图", fontsize=12, pad=6)
    return savefig(fig, "fig4_3_er.png")


def fig_class():
    fig, ax = plt.subplots(figsize=(11.5, 6.8))
    ax.set_xlim(0, 11.5)
    ax.set_ylim(0, 6.8)
    ax.axis("off")

    def clazz(x, y, w, h, name, lines, fc="#E6F4FF"):
        box(ax, x, y + h - 0.32, w, 0.32, name, fc="#102A43", ec="#102A43", fs=8, bold=True)
        ax.texts[-1].set_color("white")
        body = Rectangle((x, y), w, h - 0.32, facecolor=fc, edgecolor="#333", lw=1)
        ax.add_patch(body)
        ax.text(x + 0.08, y + h - 0.45, "\n".join(lines), ha="left", va="top", fontsize=7)

    clazz(0.4, 4.3, 3.2, 2.1, "WorkOrderService",
          ["+ submit()", "+ assign()", "+ start()", "+ process()", "+ accept()", "+ evaluate()", "+ archive()"])
    clazz(4.1, 4.3, 3.2, 2.1, "DeviceService",
          ["+ page()", "+ save()", "+ changeStatus()", "+ statusHistory()", "+ applyStatusFromBiz()"])
    clazz(7.8, 4.3, 3.2, 2.1, "MessageService",
          ["+ sendToUser()", "+ sendToAdmins()", "+ page()", "+ markRead()", "+ unreadCount()"])
    clazz(0.4, 1.5, 3.2, 2.1, "SparePartService",
          ["+ changeStock()", "+ consumeOut()", "+ page()", "+ stockSummary()"])
    clazz(4.1, 1.5, 3.2, 2.1, "MaintainTaskService",
          ["+ page()", "+ assign()", "+ complete()", "+ generateFromPlan()"])
    clazz(7.8, 1.5, 3.2, 2.1, "StatsService / ExcelService",
          ["+ adminStats()", "+ opsDashboard()", "+ exportDevices()", "+ importDevices()"])

    ax.annotate("", xy=(5.7, 5.2), xytext=(3.6, 5.2),
                arrowprops=dict(arrowstyle="->", color="#666"))
    ax.annotate("", xy=(7.8, 5.5), xytext=(7.3, 5.5),
                arrowprops=dict(arrowstyle="->", color="#666"))
    ax.annotate("", xy=(2.0, 4.3), xytext=(2.0, 3.7),
                arrowprops=dict(arrowstyle="->", color="#666"))
    ax.text(5.5, 0.6, "说明：Controller 调用 Service；Service 依赖 Mapper 与 MessageService 等协作完成闭环。",
            fontsize=8, ha="center")
    ax.set_title("图4.4 核心业务类设计图（节选）", fontsize=12, pad=6)
    return savefig(fig, "fig4_4_class.png")


def fig_sequence_assign():
    fig, ax = plt.subplots(figsize=(11.2, 7.0))
    ax.set_xlim(0, 11.2)
    ax.set_ylim(0, 7.0)
    ax.axis("off")
    actors = [("甲方Vue", 1.2), ("运维/管理Vue", 3.4), ("OrderController", 5.6),
              ("WorkOrderService", 7.8), ("Message/DB", 10.0)]
    for name, x in actors:
        box(ax, x - 0.85, 6.3, 1.7, 0.45, name, fc="#FFF7E6", ec="#D48806", fs=8)
        ax.plot([x, x], [0.4, 6.3], color="#999", ls="--", lw=1)

    def msg(x1, x2, y, text, back=False):
        style = "->" if not back else "->"
        ax.annotate("", xy=(x2, y), xytext=(x1, y),
                    arrowprops=dict(arrowstyle=style, color="#1677FF" if not back else "#389E0D", lw=1.1,
                                    connectionstyle="arc3,rad=0"))
        ax.text((x1 + x2) / 2, y + 0.08, text, ha="center", fontsize=7)

    msg(1.2, 5.6, 5.8, "1 POST /orders 报修")
    msg(5.6, 7.8, 5.4, "2 submit()")
    msg(7.8, 10.0, 5.0, "3 写工单+设备状态+消息")
    msg(10.0, 7.8, 4.6, "4 ok", True)
    msg(7.8, 5.6, 4.2, "5 Result", True)
    msg(5.6, 1.2, 3.8, "6 返回成功", True)

    msg(3.4, 5.6, 3.2, "7 POST /orders/{id}/assign")
    msg(5.6, 7.8, 2.8, "8 assign()")
    msg(7.8, 10.0, 2.4, "9 更新已派单+推送运维")
    msg(10.0, 3.4, 1.8, "10 WS/消息通知运维", True)
    msg(5.6, 3.4, 1.3, "11 派单成功", True)

    ax.set_title("图4.5 报修与派单时序图", fontsize=12, pad=6)
    return savefig(fig, "fig4_5_seq.png")


def fig_ui_prototype():
    fig, ax = plt.subplots(figsize=(11, 6.2))
    ax.set_xlim(0, 11)
    ax.set_ylim(0, 6.2)
    ax.axis("off")
    # browser chrome
    outer = FancyBboxPatch((0.3, 0.3), 10.4, 5.6, boxstyle="round,pad=0.02,rounding_size=0.05",
                           facecolor="#F5F7FA", edgecolor="#333", lw=1.3)
    ax.add_patch(outer)
    box(ax, 0.3, 5.35, 10.4, 0.55, "冷库设备运维工单管理系统　　消息铃铛  运维-理工（运维人员） 退出",
        fc="#0b3a4a", ec="#0b3a4a", fs=8, bold=True)
    ax.texts[-1].set_color("white")
    box(ax, 0.3, 0.3, 2.2, 5.05, "", fc="#112233", ec="#112233", fs=8)
    menus = ["运维工作台", "运维工单", "冷库管理", "设备台账", "维保任务", "备件库存", "消息中心"]
    for i, m in enumerate(menus):
        y = 4.7 - i * 0.55
        c = "#1a6b7c" if m == "运维工单" else "#112233"
        box(ax, 0.4, y, 2.0, 0.45, m, fc=c, ec="#4fc3f7", fs=8)
        ax.texts[-1].set_color("#cfd8dc")
    box(ax, 2.7, 4.6, 7.7, 0.55, "筛选：状态 / 工单号 / 设备　　[查询] [报修]", fc="#FFFFFF", ec="#D9D9D9", fs=8)
    box(ax, 2.7, 1.0, 7.7, 3.4, "工单列表表格区域\n编号 | 设备 | 故障描述 | 状态 | 提交人 | 运维 | 操作\n（接单 / 处理 / 详情）",
        fc="#FFFFFF", ec="#D9D9D9", fs=9)
    box(ax, 2.7, 0.45, 7.7, 0.4, "分页 < 1 2 3 >", fc="#FFFFFF", ec="#D9D9D9", fs=8)
    ax.set_title("图4.6 运维工单界面原型图", fontsize=12, pad=6)
    return savefig(fig, "fig4_6_ui.png")


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
    elif align == "right":
        p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
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


def add_heading_cn(doc, text, level=1):
    sizes = {1: 16, 2: 14, 3: 12}
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER if level == 1 else WD_ALIGN_PARAGRAPH.LEFT
    pf = p.paragraph_format
    pf.space_before = Pt(12 if level == 1 else 8)
    pf.space_after = Pt(10 if level == 1 else 6)
    pf.line_spacing = 1.5
    run = p.add_run(text)
    set_run_font(run, name_cn="黑体", size=sizes.get(level, 12), bold=True)


def add_caption(doc, text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.space_after = Pt(10)
    run = p.add_run(text)
    set_run_font(run, size=10.5)


def add_image(doc, path, width_cm=14.5):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.add_run().add_picture(str(path), width=Cm(width_cm))


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


def patch_front_matter(doc):
    """更新封面副标题，并在目录末尾追加第4章条目。"""
    for p in doc.paragraphs:
        t = p.text.strip()
        if "论文前三章" in t:
            p.clear()
            run = p.add_run("（论文第1–4章：绪论 · 关键技术 · 系统分析 · 系统设计）")
            set_run_font(run, size=12)
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        if t == "　　3.4　本章小结":
            # insert after this paragraph by appending TOC lines before chapter1 - harder;
            # instead append TOC items at end of TOC section when we see 目　录 chunk later
            pass

    # Append TOC entries: find last TOC-like line and add after via new paragraphs before ch1
    # Simpler: add TOC block items right before "第1章" if missing
    has_ch4_toc = any("第4章" in (p.text or "") and "系统设计" in (p.text or "") for p in doc.paragraphs)
    if has_ch4_toc:
        return
    insert_at = None
    for i, p in enumerate(doc.paragraphs):
        if p.text.strip() == "　　3.4　本章小结":
            insert_at = p
            break
    if insert_at is not None:
        # python-docx can't easily insert after; add runs by creating elements
        from docx.oxml import OxmlElement
        new_lines = [
            "第4章　系统设计",
            "　　4.1　系统技术架构设计",
            "　　　　4.1.1　系统软件层次架构",
            "　　　　4.1.2　系统功能结构",
            "　　4.2　系统功能设计",
            "　　　　4.2.1　业务模块功能设计",
            "　　　　4.2.2　核心流程设计（时序）",
            "　　4.3　数据库设计",
            "　　　　4.3.1　数据库概念设计",
            "　　　　4.3.2　数据库逻辑设计",
            "　　4.4　本章小结",
        ]
        ref = insert_at._p
        for line in reversed(new_lines):
            np = OxmlElement("w:p")
            # clone basic spacing by using high-level API workaround: insert XML paragraph with text
            ref.addnext(np)
            # fill via temporary paragraph wrapper
        # The above empty p won't have text easily; use a simpler approach:
        # just document TOC update note in chapter start. Skip fragile TOC XML.
        # Remove empty inserted nodes
        parent = ref.getparent()
        for _ in new_lines:
            nxt = ref.getnext()
            if nxt is not None and nxt.tag == qn("w:p") and not "".join(nxt.itertext()).strip():
                parent.remove(nxt)


def append_chapter4(doc, figs):
    doc.add_page_break()
    add_heading_cn(doc, "第4章　系统设计", 1)
    add_para(doc,
             "在前述需求分析基础上，本章对冷库设备综合运维管理系统进行系统设计，包括技术架构、功能结构、核心流程与数据库设计。"
             "设计内容与第3章用例保持一致，并满足软件应用与开发类作品对功能结构图、系统架构图、时序图、界面原型及 ER/数据表等模型要素的要求。")

    add_heading_cn(doc, "4.1　系统技术架构设计", 2)
    add_heading_cn(doc, "4.1.1　系统软件层次架构", 3)
    add_para(doc,
             "系统采用前后端分离架构。前端为 Vue2 单页应用，负责页面渲染、路由权限与图表展示；后端为 Spring Boot 服务，按 Controller—Service—Mapper 分层组织，"
             "通过 JWT 拦截器完成认证鉴权，业务数据持久化于 MySQL，Redis 用于 Token 黑名单等缓存场景（不可用时本地降级），"
             "WebSocket 承担消息实时推送，本地目录保存上传图片，POI 支持 Excel 导入导出。系统软件层次架构如图4.1所示。")
    add_image(doc, figs["4_1"], 15)
    add_caption(doc, "图4.1　系统软件层次架构图")

    add_heading_cn(doc, "4.1.2　系统功能结构", 3)
    add_para(doc,
             "结合三角色需求，系统功能划分为认证权限、冷库与冷藏间、设备台账与健康、运维工单、故障与案例、维保计划与任务、"
             "备件与供应商、数据统计、消息中心及 Excel/文件支撑等模块。系统功能结构如图4.2所示。")
    add_image(doc, figs["4_2"], 15.5)
    add_caption(doc, "图4.2　系统功能结构图")

    add_heading_cn(doc, "4.2　系统功能设计", 2)
    add_heading_cn(doc, "4.2.1　业务模块功能设计", 3)
    add_para(doc,
             "（1）认证与权限模块：登录签发 JWT，路由与菜单按角色渲染；退出写入 Token 黑名单；管理员维护用户启停与角色。")
    add_para(doc,
             "（2）冷库与设备模块：冷库/冷藏间档案维护；库区详情作为单库办事台；设备台账支持生命周期状态历史、健康规则分析与 Excel 导入导出。")
    add_para(doc,
             "（3）工单模块：实现七状态状态机。甲方报修与验收评价；管理员派单/转派/撤回/关闭/归档；运维接单处理并领用备件；关键节点联动设备状态与消息。")
    add_para(doc,
             "（4）故障与维保模块：故障记录与案例库支撑经验复用；维保计划生成任务并由运维填报，与故障维修双轨并行。")
    add_para(doc,
             "（5）库存与分析模块：备件出入库与安全库存预警；统计中心输出工单、故障、维保、库存等指标供 ECharts 展示。")
    add_para(doc, "核心业务类协作关系（设计类图节选）如图4.4所示。")
    add_image(doc, figs["4_4"], 14.5)
    add_caption(doc, "图4.4　核心业务类设计图（节选）")

    add_heading_cn(doc, "4.2.2　核心流程设计（时序）", 3)
    add_para(doc,
             "以“甲方报修—管理员派单—消息通知运维”为主链路，对象间交互如图4.5所示。该时序图用于指导实现阶段接口编排与事务边界划分。"
             "（活动图已在第3章给出，本章以时序图描述对象协作，符合“活动图/时序图二选一侧重”的模型要求。）")
    add_image(doc, figs["4_5"], 14.5)
    add_caption(doc, "图4.5　报修与派单时序图")

    add_para(doc, "运维工单列表界面原型如图4.6所示，用于在实现前确认布局与操作入口。")
    add_image(doc, figs["4_6"], 14)
    add_caption(doc, "图4.6　运维工单界面原型图")

    add_heading_cn(doc, "4.3　数据库设计", 2)
    add_heading_cn(doc, "4.3.1　数据库概念设计", 3)
    add_para(doc,
             "概念模型围绕“冷库—冷藏间—设备—工单/故障/维保—备件/供应商—用户/消息”展开。"
             "一座冷库包含多个冷藏间，冷藏间下挂设备；工单与故障记录关联设备；维保计划生成面向设备的维保任务；"
             "备件归属供应商并产生库存流水；消息按用户投递。核心实体关系如图4.3所示。")
    add_image(doc, figs["4_3"], 15.2)
    add_caption(doc, "图4.3　数据库概念结构（核心实体关系）图")

    add_heading_cn(doc, "4.3.2　数据库逻辑设计", 3)
    add_para(doc,
             "逻辑设计将概念实体落实为 MySQL 表。以下给出核心业务表结构（字段类型按实现口径），其余辅助表（状态日志、工单备件、计划检查项、操作日志等）与之关联。")

    add_table(doc, "表4.1　用户表 sys_user",
              ["字段名称", "类型", "说明"],
              [
                  ["id", "BIGINT", "主键"],
                  ["username", "VARCHAR", "登录名，唯一"],
                  ["password", "VARCHAR", "BCrypt 密码"],
                  ["real_name", "VARCHAR", "姓名"],
                  ["phone", "VARCHAR", "手机号"],
                  ["role", "VARCHAR", "ADMIN/OPS/CLIENT"],
                  ["status", "TINYINT", "1启用/0禁用"],
                  ["create_time", "DATETIME", "创建时间"],
              ])

    add_table(doc, "表4.2　冷库表 cold_storage",
              ["字段名称", "类型", "说明"],
              [
                  ["id", "BIGINT", "主键"],
                  ["name", "VARCHAR", "冷库名称"],
                  ["address", "VARCHAR", "地址"],
                  ["contact_name", "VARCHAR", "联系人"],
                  ["contact_phone", "VARCHAR", "联系电话"],
                  ["status", "VARCHAR", "启用/停用"],
                  ["commission_date", "DATE", "投用日期"],
                  ["remark", "VARCHAR", "备注"],
              ])

    add_table(doc, "表4.3　冷藏间表 cold_room",
              ["字段名称", "类型", "说明"],
              [
                  ["id", "BIGINT", "主键"],
                  ["storage_id", "BIGINT", "所属冷库FK"],
                  ["name", "VARCHAR", "冷藏间名称"],
                  ["code", "VARCHAR", "编码"],
                  ["temp_min/temp_max", "DECIMAL", "温度范围"],
                  ["volume", "DECIMAL", "容积"],
                  ["status", "VARCHAR", "状态"],
              ])

    add_table(doc, "表4.4　设备表 device",
              ["字段名称", "类型", "说明"],
              [
                  ["id", "BIGINT", "主键"],
                  ["device_no", "VARCHAR", "设备编号，唯一"],
                  ["device_name", "VARCHAR", "设备名称"],
                  ["device_type", "VARCHAR", "设备类型"],
                  ["room_id", "BIGINT", "冷藏间FK"],
                  ["is_public", "TINYINT", "是否公共设备"],
                  ["status", "VARCHAR", "NORMAL/FAULT/.../SCRAPPED"],
                  ["maintain_cycle_days", "INT", "维保周期"],
                  ["fault_count", "INT", "累计故障次数"],
              ])

    add_table(doc, "表4.5　工单表 work_order",
              ["字段名称", "类型", "说明"],
              [
                  ["id", "BIGINT", "主键"],
                  ["order_no", "VARCHAR", "工单号"],
                  ["device_id", "BIGINT", "设备FK"],
                  ["fault_desc", "VARCHAR", "故障描述"],
                  ["fault_images", "VARCHAR", "报修图片"],
                  ["status", "VARCHAR", "七状态枚举"],
                  ["submit_user_id", "BIGINT", "报修人"],
                  ["assignee_id", "BIGINT", "指派运维"],
                  ["process_record", "TEXT", "处理过程"],
                  ["satisfaction", "INT", "满意度"],
                  ["assign_time/finish_time/...", "DATETIME", "关键时间戳"],
              ])

    add_table(doc, "表4.6　备件表 spare_part",
              ["字段名称", "类型", "说明"],
              [
                  ["id", "BIGINT", "主键"],
                  ["part_no", "VARCHAR", "备件编号"],
                  ["part_name", "VARCHAR", "名称"],
                  ["stock_qty", "INT", "当前库存"],
                  ["safety_stock", "INT", "安全库存"],
                  ["supplier_id", "BIGINT", "供应商FK"],
                  ["unit_price", "DECIMAL", "单价"],
              ])

    add_table(doc, "表4.7　消息表 sys_message",
              ["字段名称", "类型", "说明"],
              [
                  ["id", "BIGINT", "主键"],
                  ["user_id", "BIGINT", "接收用户FK"],
                  ["title/content", "VARCHAR", "标题/内容"],
                  ["msg_type", "VARCHAR", "ORDER_NEW/ASSIGN/STOCK_LOW等"],
                  ["biz_type/biz_id", "VARCHAR/BIGINT", "业务关联"],
                  ["is_read", "TINYINT", "是否已读"],
              ])

    add_para(doc,
             "此外，系统还包括 fault_record、fault_case、maintain_plan/item、maintain_task/item、stock_record、"
             "device_status_log、work_order_part、user_cold_room、operation_log、supplier 等表，分别支撑故障案例、维保、库存流水、状态追溯、权限绑定与审计。")

    add_heading_cn(doc, "4.4　本章小结", 2)
    add_para(doc,
             "本章完成了系统总体设计：给出层次架构图与功能结构图，明确模块边界；通过类图与时序图描述核心对象协作；给出界面原型以指导页面实现；"
             "并完成数据库概念结构与核心表逻辑设计。设计结果与第3章需求一一对应，为第5章系统实现提供直接依据。")


def main():
    figs = {
        "4_1": fig_architecture(),
        "4_2": fig_function_tree(),
        "4_3": fig_er(),
        "4_4": fig_class(),
        "4_5": fig_sequence_assign(),
        "4_6": fig_ui_prototype(),
    }
    if not SRC.exists():
        raise SystemExit(f"missing {SRC}")
    doc = Document(str(SRC))
    patch_front_matter(doc)
    append_chapter4(doc, figs)
    doc.save(str(OUT))
    # also overwrite 前三章 file name variant for convenience? keep both
    print("OK", OUT)
    for k, v in figs.items():
        print(k, v)


if __name__ == "__main__":
    main()
