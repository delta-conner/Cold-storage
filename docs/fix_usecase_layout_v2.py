# -*- coding: utf-8 -*-
"""用例图最终排版：单列近似水平连线；分项图仿范本图3.5。"""
from pathlib import Path
import math
import matplotlib.pyplot as plt
from matplotlib.patches import Ellipse, Rectangle
from docx import Document
from docx.shared import Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH

ROOT = Path(r"f:\plcmin")
FIG = ROOT / "docs" / "thesis_figs"
OUT = ROOT / "docs" / "冷库设备运维管理系统-论文第1-4章-用例图排版优化.docx"

plt.rcParams["font.sans-serif"] = ["SimSun", "Microsoft YaHei", "SimHei"]
plt.rcParams["axes.unicode_minus"] = False


def save(fig, name):
    p = FIG / name
    fig.savefig(p, dpi=200, bbox_inches="tight", facecolor="white", pad_inches=0.2)
    plt.close(fig)
    return p


def actor(ax, x, y, label):
    ax.add_patch(plt.Circle((x, y), 0.11, fill=False, ec="black", lw=1.3, zorder=5))
    ax.plot([x, x], [y - 0.11, y - 0.45], "k-", lw=1.3, zorder=5)
    ax.plot([x - 0.16, x + 0.16], [y - 0.23, y - 0.23], "k-", lw=1.3, zorder=5)
    ax.plot([x, x - 0.13], [y - 0.45, y - 0.68], "k-", lw=1.3, zorder=5)
    ax.plot([x, x + 0.13], [y - 0.45, y - 0.68], "k-", lw=1.3, zorder=5)
    ax.text(x, y - 0.9, label, ha="center", va="top", fontsize=12)


def uc(ax, x, y, text, w=2.5, h=0.7):
    ax.add_patch(Ellipse((x, y), w, h, facecolor="white", edgecolor="black", lw=1.2, zorder=4))
    ax.text(x, y, text, ha="center", va="center", fontsize=11, zorder=6)
    return x, y, w, h


def frame(ax, x0, y0, x1, y1, title):
    """标题画在框内顶部，并预留空白带，椭圆不得侵入。"""
    ax.add_patch(Rectangle((x0, y0), x1 - x0, y1 - y0, fill=False, ec="black", lw=1.5, zorder=1))
    ax.text((x0 + x1) / 2, y1 - 0.28, title, ha="center", va="top", fontsize=13)


def solid(ax, x1, y1, x2, y2):
    ax.plot([x1, x2], [y1, y2], "k-", lw=1.0, zorder=2, solid_capstyle="round")


def include(ax, x1, y1, x2, y2, side=0.32):
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
        bbox=dict(boxstyle="square,pad=0.15", facecolor="white", edgecolor="none"),
    )


def fig_single_column(fname, title, actor_name, cases):
    """单列用例：连线近似水平，互不穿椭圆。"""
    n = len(cases)
    # 每行间距加大
    row_h = 0.95
    title_band = 0.85
    bottom_pad = 0.55
    top_pad = 0.35
    height = top_pad + title_band + n * row_h + bottom_pad
    fig, ax = plt.subplots(figsize=(8.6, max(5.0, height * 0.85)))
    ax.set_xlim(0, 8.6)
    ax.set_ylim(0, height)
    ax.axis("off")

    x0, x1 = 2.4, 8.3
    y0, y1 = 0.3, height - 0.2
    frame(ax, x0, y0, x1, y1, title)

    # 椭圆中心 x，全部在框内
    uc_x = (x0 + x1) / 2 + 0.35
    first_y = y1 - title_band - 0.15
    positions = []
    for i, name in enumerate(cases):
        y = first_y - i * row_h
        positions.append(uc(ax, uc_x, y, name, w=2.7, h=0.68))

    # 参与者放在垂直居中
    ay = (positions[0][1] + positions[-1][1]) / 2
    actor(ax, 1.05, ay + 0.15, actor_name)

    for x, y, w, h in positions:
        # 从参与者右肩附近水平连到椭圆左缘，y 对齐椭圆中心 → 线平行不粘连
        solid(ax, 1.28, y, x - w / 2 - 0.02, y)

    return save(fig, fname)


def fig_role():
    """
    三角色：用例单列；每位参与者只连自己的用例，线水平，互不交叉穿字。
    将用例按角色分区排列：上管理员专属+共享，中运维，下甲方。
    """
    # 分区用例（可有重复共享项分到各区，更清晰；这里用不重复的共享放中间）
    admin_only = ["用户与权限管理", "健康分析与统计", "操作日志审计"]
    shared_ops_admin = ["冷库/设备台账管理", "工单全流程管控", "维保计划与任务", "故障/案例/备件"]
    shared_all = ["消息中心", "登录认证"]
    # 甲方还会用到台账/工单，在图中用共享区连接甲方

    # 更简单且清晰的做法：8个用例单列，三条水平“泳道”连接
    cases = [
        ("用户与权限管理", {"admin"}),
        ("冷库/设备台账管理", {"admin", "ops", "client"}),
        ("工单全流程管控", {"admin", "ops", "client"}),
        ("维保计划与任务", {"admin", "ops"}),
        ("故障/案例/备件", {"admin", "ops"}),
        ("健康分析与统计", {"admin", "ops"}),
        ("消息中心", {"admin", "ops", "client"}),
        ("登录认证", {"admin", "ops", "client"}),
    ]

    n = len(cases)
    row_h = 0.78
    title_band = 0.9
    height = 1.0 + title_band + n * row_h + 0.6
    fig, ax = plt.subplots(figsize=(10.5, height * 0.9))
    ax.set_xlim(0, 10.5)
    ax.set_ylim(0, height)
    ax.axis("off")

    x0, x1 = 3.5, 10.2
    y0, y1 = 0.35, height - 0.25
    frame(ax, x0, y0, x1, y1, "冷库设备综合运维管理系统")

    uc_x = (x0 + x1) / 2 + 0.2
    first_y = y1 - title_band - 0.1
    pos = []
    for i, (name, roles) in enumerate(cases):
        y = first_y - i * row_h
        pos.append((uc(ax, uc_x, y, name, w=3.0, h=0.62), roles, y))

    # 三位参与者纵向对齐各自“主连”高度
    actor(ax, 1.0, pos[0][2] - 0.1, "管理员")
    actor(ax, 1.0, pos[3][2], "运维人员")
    actor(ax, 1.0, pos[6][2], "甲方用户")

    # 水平连线：管理员从 x=1.25 连全部；运维/甲方只连自己的，线从各自身体高度出发但终点 y=用例 y（先垂直到目标高度再水平，避免交叉）
    def elbow(actor_x, actor_y, target_x, target_y, lane_x):
        # 出线到专用竖向通道，再水平进椭圆，通道错开避免粘连
        solid(ax, actor_x + 0.22, actor_y - 0.15, lane_x, actor_y - 0.15)
        solid(ax, lane_x, actor_y - 0.15, lane_x, target_y)
        solid(ax, lane_x, target_y, target_x - 1.55, target_y)

    # 三条通道
    lanes = {"admin": 2.35, "ops": 2.75, "client": 3.15}
    actor_pos = {"admin": (1.0, pos[0][2] - 0.1), "ops": (1.0, pos[3][2]), "client": (1.0, pos[6][2])}

    for (x, y, w, h), roles, yy in pos:
        for r in roles:
            ax_, ay_ = actor_pos[r]
            elbow(ax_, ay_, x, yy, lanes[r])

    return save(fig, "fig3_1_role.png")


def fig_detail(fname, title, actor_name, left_two, mid, login=True):
    """仿图3.5：2个左用例 + 右上中介 + 右下登录。"""
    fig, ax = plt.subplots(figsize=(9.2, 5.4))
    ax.set_xlim(0, 9.2)
    ax.set_ylim(0, 5.4)
    ax.axis("off")
    frame(ax, 2.2, 0.4, 8.9, 5.1, title)
    actor(ax, 0.95, 2.85, actor_name)

    a = uc(ax, 4.15, 3.7, left_two[0], w=2.45, h=0.75)
    b = uc(ax, 4.15, 2.15, left_two[1], w=2.45, h=0.75)
    m = uc(ax, 7.0, 3.7, mid, w=2.45, h=0.75)
    # 水平关联，不粘
    solid(ax, 1.2, 3.7, a[0] - a[2] / 2, 3.7)
    solid(ax, 1.2, 2.15, b[0] - b[2] / 2, 2.15)
    # include: 左 -> 中
    include(ax, a[0] + a[2] / 2 - 0.05, a[1], m[0] - m[2] / 2 + 0.05, m[1], side=0.38)
    include(ax, b[0] + b[2] / 2 - 0.05, b[1], m[0] - m[2] / 2 + 0.05, m[1] - 0.12, side=-0.4)
    if login:
        lg = uc(ax, 7.0, 2.0, "登录", w=2.1, h=0.7)
        include(ax, m[0], m[1] - m[3] / 2, lg[0], lg[1] + lg[3] / 2, side=0.45)
    return save(fig, fname)


def fig_detail3(fname, title, actor_name, left_three, mid, login=True):
    fig, ax = plt.subplots(figsize=(9.2, 6.0))
    ax.set_xlim(0, 9.2)
    ax.set_ylim(0, 6.0)
    ax.axis("off")
    frame(ax, 2.2, 0.35, 8.9, 5.7, title)
    actor(ax, 0.95, 3.1, actor_name)
    ys = [4.4, 3.15, 1.9]
    lefts = []
    for y, name in zip(ys, left_three):
        lefts.append(uc(ax, 4.15, y, name, w=2.45, h=0.72))
        solid(ax, 1.2, y, 4.15 - 1.25, y)
    m = uc(ax, 7.0, 4.4, mid, w=2.45, h=0.72)
    sides = [0.4, -0.35, 0.45]
    for L, s in zip(lefts, sides):
        include(ax, L[0] + L[2] / 2 - 0.05, L[1], m[0] - m[2] / 2 + 0.05, m[1] - (4.4 - L[1]) * 0.08, side=s)
    if login:
        lg = uc(ax, 7.0, 2.2, "登录", w=2.1, h=0.7)
        include(ax, m[0], m[1] - m[3] / 2, lg[0], lg[1] + lg[3] / 2, side=0.5)
    return save(fig, fname)


def redraw():
    fig_role()
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
    print("redraw ok")


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
    src_candidates = [
        OUT,
        ROOT / "docs" / "冷库设备运维管理系统-论文第1-4章-用例图已改样式.docx",
        ROOT / "docs" / "冷库设备运维管理系统-论文第1-4章.docx",
    ]
    src = next((p for p in src_candidates if p.exists()), None)
    if not src:
        print("no doc")
        return
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
        prev.add_run().add_picture(str(FIG / hit), width=Cm(13.0))
        prev.alignment = WD_ALIGN_PARAGRAPH.CENTER
        n += 1
    dest = ROOT / "docs" / "冷库设备运维管理系统-论文第1-4章-用例图最终版.docx"
    try:
        doc.save(str(dest))
    except PermissionError:
        dest = ROOT / "docs" / "冷库设备运维管理系统-论文第1-4章-用例图最终版2.docx"
        doc.save(str(dest))
    print("saved", dest.name, "replaced", n)


if __name__ == "__main__":
    redraw()
    update_doc()
