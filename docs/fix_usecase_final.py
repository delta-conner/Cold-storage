# -*- coding: utf-8 -*-
"""用例图定稿：单列水平线；分项图椭圆严格在框内；角色图无总线。"""
from pathlib import Path
import math
import matplotlib.pyplot as plt
from matplotlib.patches import Ellipse, Rectangle
from docx import Document
from docx.shared import Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH

ROOT = Path(r"f:\plcmin")
FIG = ROOT / "docs" / "thesis_figs"
DEST = ROOT / "docs" / "冷库设备运维管理系统-论文第1-4章-用例图最终版.docx"

plt.rcParams["font.sans-serif"] = ["SimSun", "Microsoft YaHei", "SimHei"]
plt.rcParams["axes.unicode_minus"] = False


def save(fig, name):
    p = FIG / name
    fig.savefig(p, dpi=200, bbox_inches="tight", facecolor="white", pad_inches=0.25)
    plt.close(fig)
    return p


def actor(ax, x, y, label):
    ax.add_patch(plt.Circle((x, y), 0.11, fill=False, ec="black", lw=1.3, zorder=5))
    ax.plot([x, x], [y - 0.11, y - 0.45], "k-", lw=1.3, zorder=5)
    ax.plot([x - 0.16, x + 0.16], [y - 0.23, y - 0.23], "k-", lw=1.3, zorder=5)
    ax.plot([x, x - 0.13], [y - 0.45, y - 0.68], "k-", lw=1.3, zorder=5)
    ax.plot([x, x + 0.13], [y - 0.45, y - 0.68], "k-", lw=1.3, zorder=5)
    ax.text(x, y - 0.88, label, ha="center", va="top", fontsize=12)


def uc(ax, x, y, text, w=2.35, h=0.68):
    ax.add_patch(Ellipse((x, y), w, h, facecolor="white", edgecolor="black", lw=1.2, zorder=4))
    ax.text(x, y, text, ha="center", va="center", fontsize=11, zorder=6)
    return x, y, w, h


def frame(ax, x0, y0, x1, y1, title):
    ax.add_patch(Rectangle((x0, y0), x1 - x0, y1 - y0, fill=False, ec="black", lw=1.5, zorder=1))
    ax.text((x0 + x1) / 2, y1 - 0.32, title, ha="center", va="top", fontsize=13)


def solid(ax, x1, y1, x2, y2):
    ax.plot([x1, x2], [y1, y2], "k-", lw=1.0, zorder=2)


def include(ax, x1, y1, x2, y2, side=0.42):
    ax.annotate(
        "",
        xy=(x2, y2),
        xytext=(x1, y1),
        arrowprops=dict(arrowstyle="->", color="black", lw=1.0, linestyle=(0, (5, 3))),
        zorder=3,
    )
    mx, my = (x1 + x2) / 2.0, (y1 + y2) / 2.0
    dx, dy = x2 - x1, y2 - y1
    L = math.hypot(dx, dy) or 1.0
    nx, ny = -dy / L * side, dx / L * side
    ax.text(
        mx + nx,
        my + ny,
        "<<include>>",
        ha="center",
        va="center",
        fontsize=9,
        zorder=8,
        bbox=dict(boxstyle="square,pad=0.18", facecolor="white", edgecolor="white", linewidth=3),
    )


def fig_single_column(fname, title, actor_name, cases):
    n = len(cases)
    row_h = 0.92
    title_band = 0.9
    height = 0.9 + title_band + n * row_h + 0.5
    fig, ax = plt.subplots(figsize=(8.4, max(5.2, height * 0.82)))
    ax.set_xlim(0, 8.4)
    ax.set_ylim(0, height)
    ax.axis("off")

    x0, x1 = 2.5, 8.1
    y0, y1 = 0.35, height - 0.2
    frame(ax, x0, y0, x1, y1, title)

    uc_x = (x0 + x1) / 2
    w = 2.5
    first_y = y1 - title_band - 0.12
    ys = [first_y - i * row_h for i in range(n)]
    ay = (ys[0] + ys[-1]) / 2 + 0.1
    actor(ax, 1.05, ay, actor_name)
    for y in ys:
        solid(ax, 1.28, y, uc_x - w / 2 - 0.08, y)
    for y, name in zip(ys, cases):
        uc(ax, uc_x, y, name, w=w, h=0.66)
    return save(fig, fname)


def fig_role():
    """每位参与者只直连自己的一组用例，水平线，无竖向总线。"""
    groups = [
        ("管理员", ["用户与权限管理", "工单派单管控", "健康分析与统计", "操作日志审计"]),
        ("运维人员", ["处理运维工单", "维保任务执行", "故障/案例/备件", "备件领用"]),
        ("甲方用户", ["浏览设备台账", "提交报修工单", "确认验收评价", "消息中心"]),
    ]
    # 另加登录，三人共用，放最底部，三人各连一条水平线
    login = "登录认证"

    rows = sum(len(g[1]) for g in groups) + 1
    row_h = 0.78
    title_band = 0.95
    gap = 0.25  # 组间距
    height = 0.8 + title_band + rows * row_h + 2 * gap + 0.5
    fig, ax = plt.subplots(figsize=(9.5, height * 0.82))
    ax.set_xlim(0, 9.5)
    ax.set_ylim(0, height)
    ax.axis("off")

    x0, x1 = 3.2, 9.2
    y0, y1 = 0.35, height - 0.2
    frame(ax, x0, y0, x1, y1, "冷库设备综合运维管理系统")

    uc_x = (x0 + x1) / 2
    y = y1 - title_band - 0.1
    for actor_name, cases in groups:
        ys = []
        for name in cases:
            uc(ax, uc_x, y, name, w=2.9, h=0.6)
            ys.append(y)
            y -= row_h
        ay = (ys[0] + ys[-1]) / 2
        actor(ax, 1.15, ay + 0.1, actor_name)
        for yy in ys:
            solid(ax, 1.4, yy, uc_x - 1.5, yy)
        y -= gap

    # 登录
    uc(ax, uc_x, y, login, w=2.5, h=0.6)
    # 三位参与者都连登录：从各自位置拉到登录高度会交叉，改为在登录左侧汇合短线——仍易乱。
    # 改为：登录只画在框内，用注释说明三角色均需登录；或只从最近的甲方连过去并写说明。
    # 为符合 UML，从三个固定 lane 水平接到登录会产生竖线。这里让登录与「消息中心」同组甲方连接即可，文中说明全员需登录。
    # 补：三个角色各有一条到登录的折线，通道 x 分开且只在框外折：
    login_y = y
    for i, (actor_name, cases) in enumerate(groups):
        # 参与者 y 取该组中点（重新算）
        pass
    # 简单：三条水平线从框外不同高度接到登录左缘——参与者已画完，用记录的 ay
    # 重新扫描：按组存储 ay
    return save(fig, "fig3_1_role.png")


def fig_role_v2():
    """每位参与者只直连自己的一组用例，全部水平线；登录在分项图中体现，总览不再画总线。"""
    groups = [
        ("管理员", ["用户与权限管理", "工单派单管控", "健康分析与统计", "操作日志审计"]),
        ("运维人员", ["处理运维工单", "维保任务执行", "故障/案例/备件", "备件领用"]),
        ("甲方用户", ["浏览设备台账", "提交报修工单", "确认验收评价", "消息中心"]),
    ]
    row_h = 0.82
    title_band = 1.0
    gap = 0.4
    n = sum(len(g[1]) for g in groups)
    height = 0.7 + title_band + n * row_h + 2 * gap + 0.55
    fig, ax = plt.subplots(figsize=(9.6, height * 0.8))
    ax.set_xlim(0, 9.6)
    ax.set_ylim(0, height)
    ax.axis("off")
    x0, x1 = 3.3, 9.35
    y0, y1 = 0.4, height - 0.2
    frame(ax, x0, y0, x1, y1, "冷库设备综合运维管理系统")
    uc_x = (x0 + x1) / 2
    y = y1 - title_band - 0.08
    pending_lines = []
    actors_to_draw = []
    for actor_name, cases in groups:
        ys = []
        for name in cases:
            ys.append(y)
            y -= row_h
        ay = (ys[0] + ys[-1]) / 2.0
        actors_to_draw.append((1.2, ay + 0.05, actor_name))
        for yy in ys:
            pending_lines.append((1.45, yy, uc_x - 1.55, yy))
        # store names with ys for ellipse draw after lines
        for name, yy in zip(cases, ys):
            pending_lines.append(("UC", uc_x, yy, name))
        y -= gap

    # 先画线，再画椭圆，避免线压字
    for item in pending_lines:
        if item[0] == "UC":
            continue
        solid(ax, *item)
    for item in pending_lines:
        if item[0] != "UC":
            continue
        _, cx, cy, name = item
        uc(ax, cx, cy, name, w=2.9, h=0.64)
    for ax_, ay_, name in actors_to_draw:
        actor(ax, ax_, ay_, name)
    return save(fig, "fig3_1_role.png")


def fig_detail(fname, title, actor_name, left_two, mid):
    fig, ax = plt.subplots(figsize=(9.4, 5.5))
    ax.set_xlim(0, 9.4)
    ax.set_ylim(0, 5.5)
    ax.axis("off")
    frame(ax, 2.35, 0.4, 9.05, 5.2, title)
    actor(ax, 1.0, 2.85, actor_name)

    lx, rx = 4.55, 7.35
    y1, y2, y3 = 3.75, 2.15, 2.0
    w = 2.2
    # 关联线（止于椭圆外）
    solid(ax, 1.25, y1, lx - w / 2 - 0.1, y1)
    solid(ax, 1.25, y2, lx - w / 2 - 0.1, y2)
    # include 画在间隙
    include(ax, lx + w / 2 + 0.05, y1, rx - w / 2 - 0.05, y1, side=0.55)
    include(ax, lx + w / 2 + 0.05, y2, rx - w / 2 - 0.05, y1 - 0.2, side=-0.6)
    include(ax, rx, y1 - 0.38, rx, y3 + 0.36, side=0.75)
    # 最后画椭圆盖住端点
    uc(ax, lx, y1, left_two[0], w=w, h=0.72)
    uc(ax, lx, y2, left_two[1], w=w, h=0.72)
    uc(ax, rx, y1, mid, w=w, h=0.72)
    uc(ax, rx, y3, "登录", w=2.0, h=0.68)
    return save(fig, fname)


def fig_detail3(fname, title, actor_name, left_three, mid):
    fig, ax = plt.subplots(figsize=(9.4, 6.1))
    ax.set_xlim(0, 9.4)
    ax.set_ylim(0, 6.1)
    ax.axis("off")
    frame(ax, 2.35, 0.35, 9.05, 5.8, title)
    actor(ax, 1.0, 3.15, actor_name)
    lx, rx = 4.55, 7.35
    ys = [4.55, 3.25, 1.95]
    w = 2.2
    for y in ys:
        solid(ax, 1.25, y, lx - w / 2 - 0.1, y)
    sides = [0.55, -0.55, 0.58]
    for y, s in zip(ys, sides):
        include(ax, lx + w / 2 + 0.05, y, rx - w / 2 - 0.05, ys[0] - (ys[0] - y) * 0.08, side=s)
    include(ax, rx, ys[0] - 0.38, rx, 2.35 + 0.36, side=0.75)
    for y, name in zip(ys, left_three):
        uc(ax, lx, y, name, w=w, h=0.7)
    uc(ax, rx, ys[0], mid, w=w, h=0.7)
    uc(ax, rx, 2.35, "登录", w=2.0, h=0.68)
    return save(fig, fname)


def redraw():
    fig_role_v2()
    fig_single_column(
        "fig3_2_client.png",
        "冷库设备运维管理系统",
        "甲方用户",
        ["浏览设备台账", "提交报修工单", "查看本人工单", "确认验收", "服务评价", "个人中心", "消息中心", "登录"],
    )
    fig_single_column(
        "fig3_5_ops.png",
        "冷库设备运维管理系统",
        "运维人员",
        ["运维工作台", "处理指派工单", "故障记录", "案例库检索", "维保任务执行", "备件领用", "设备健康查看", "消息中心"],
    )
    fig_single_column(
        "fig3_7_admin.png",
        "冷库设备运维管理系统",
        "管理员",
        ["用户权限管理", "冷库冷藏间管理", "设备台账管理", "工单派单管控", "维保计划任务", "备件供应商", "健康分析统计", "操作日志"],
    )
    fig_detail("fig3_3_device_view.png", "浏览设备台账", "甲方用户", ["条件查询设备", "选择报修设备"], "查看设备详情")
    fig_detail("fig3_4_submit.png", "提交报修工单", "甲方用户", ["填写故障描述", "上传现场图片"], "提交生成工单")
    fig_detail3("fig3_6_process.png", "处理运维工单", "运维人员", ["接单", "填写处理过程", "提交完工"], "登记备件出库")
    fig_detail3("fig3_8_user_mgmt.png", "管理用户", "管理员", ["新增用户", "编辑用户", "启用/禁用"], "查询用户")
    fig_detail("fig3_9_device_mgmt.png", "设备台账管理", "管理员", ["设备增删改查", "状态变更"], "Excel导入导出")
    fig_detail3("fig3_10_order_admin.png", "工单全流程管控", "管理员", ["派单", "转派", "撤回"], "关闭/归档")
    print("ok")


def update_doc():
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
    srcs = [
        DEST,
        ROOT / "docs" / "冷库设备运维管理系统-论文第1-4章-用例图排版优化.docx",
        ROOT / "docs" / "冷库设备运维管理系统-论文第1-4章-用例图已改样式.docx",
        ROOT / "docs" / "冷库设备运维管理系统-论文第1-4章.docx",
    ]
    src = next((p for p in srcs if p.exists()), None)
    doc = Document(str(src))
    paras = list(doc.paragraphs)
    n = 0
    for i, p in enumerate(paras):
        text = (p.text or "").replace(" ", "").replace("　", "")
        hit = None
        for key in sorted(caption_map, key=len, reverse=True):
            if text.startswith(key.replace(" ", "")):
                hit = caption_map[key]
                break
        if not hit:
            continue
        j = i - 1
        while j >= 0 and not paras[j]._p.xpath(".//*[local-name()='drawing']"):
            j -= 1
        if j < 0:
            continue
        prev = paras[j]
        for child in list(prev._p):
            prev._p.remove(child)
        prev.add_run().add_picture(str(FIG / hit), width=Cm(12.8))
        prev.alignment = WD_ALIGN_PARAGRAPH.CENTER
        n += 1
    out = ROOT / "docs" / "冷库设备运维管理系统-论文第1-4章-用例图定稿.docx"
    try:
        doc.save(str(out))
    except PermissionError:
        out = ROOT / "docs" / "冷库设备运维管理系统-论文第1-4章-用例图定稿2.docx"
        doc.save(str(out))
    print("saved", out.name, n)


if __name__ == "__main__":
    redraw()
    update_doc()
