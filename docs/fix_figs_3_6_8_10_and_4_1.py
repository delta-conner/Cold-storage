# -*- coding: utf-8 -*-
"""只修图3.6/3.8/3.10遮挡 + 重画图4.1架构（范本分层框格式），并只更新一份 Word。"""
from pathlib import Path
import math
import matplotlib.pyplot as plt
from matplotlib.patches import Ellipse, Rectangle, FancyBboxPatch
from docx import Document
from docx.shared import Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH

ROOT = Path(r"f:\plcmin")
FIG = ROOT / "docs" / "thesis_figs"
FINAL = ROOT / "docs" / "冷库设备运维管理系统-论文第1-4章.docx"

plt.rcParams["font.sans-serif"] = ["SimSun", "Microsoft YaHei", "SimHei"]
plt.rcParams["axes.unicode_minus"] = False


def save(fig, name):
    p = FIG / name
    fig.savefig(p, dpi=200, bbox_inches="tight", facecolor="white", pad_inches=0.25)
    plt.close(fig)
    return p


# 线宽对齐周思汉图3.2细线
LW_A, LW_O, LW_F, LW_L, LW_I = 0.5, 0.55, 0.6, 0.45, 0.45


def actor(ax, x, y, label):
    ax.add_patch(plt.Circle((x, y), 0.12, fill=False, ec="black", lw=LW_A, zorder=5))
    ax.plot([x, x], [y - 0.12, y - 0.48], "k-", lw=LW_A, zorder=5)
    ax.plot([x - 0.18, x + 0.18], [y - 0.26, y - 0.26], "k-", lw=LW_A, zorder=5)
    ax.plot([x, x - 0.14], [y - 0.48, y - 0.72], "k-", lw=LW_A, zorder=5)
    ax.plot([x, x + 0.14], [y - 0.48, y - 0.72], "k-", lw=LW_A, zorder=5)
    ax.text(x, y - 0.92, label, ha="center", va="top", fontsize=12)


def oval(ax, x, y, text, w=2.4, h=0.82):
    ax.add_patch(Ellipse((x, y), w, h, facecolor="white", edgecolor="black", lw=LW_O, zorder=4))
    ax.text(x, y, text, ha="center", va="center", fontsize=11, zorder=6)
    return x, y, w, h


def frame(ax, x0, y0, x1, y1, title):
    ax.add_patch(Rectangle((x0, y0), x1 - x0, y1 - y0, fill=False, ec="black", lw=LW_F, zorder=1))
    # 标题单独抬高，下面留足空白，避免被 include 标签顶到
    ax.text((x0 + x1) / 2, y1 - 0.28, title, ha="center", va="top", fontsize=13, zorder=2)


def fan(ax, ax_xy, targets):
    ax0, ay0 = ax_xy
    for tx, ty, tw, th in targets:
        ax.plot([ax0 + 0.22, tx - tw / 2.0], [ay0 - 0.12, ty], "k-", lw=LW_L, zorder=2)


def include(ax, a, b, label_xy=None, side=0.55):
    x1, y1, w1, h1 = a
    x2, y2, w2, h2 = b
    if abs(x2 - x1) >= abs(y2 - y1) * 0.55:
        sx, sy = x1 + w1 / 2.0 + 0.02, y1
        ex, ey = x2 - w2 / 2.0 - 0.02, y2
    else:
        sx, sy = x1, y1 - h1 / 2.0 - 0.02
        ex, ey = x2, y2 + h2 / 2.0 + 0.02
    ax.annotate(
        "",
        xy=(ex, ey),
        xytext=(sx, sy),
        arrowprops=dict(arrowstyle="->", color="black", lw=LW_I, linestyle=(0, (5, 3))),
        zorder=3,
    )
    if label_xy is None:
        mx, my = (sx + ex) / 2, (sy + ey) / 2
        dx, dy = ex - sx, ey - sy
        L = math.hypot(dx, dy) or 1
        nx, ny = -dy / L * side, dx / L * side
        lx, ly = mx + nx, my + ny
    else:
        lx, ly = label_xy
    ax.text(
        lx,
        ly,
        "<<include>>",
        ha="center",
        va="center",
        fontsize=9,
        zorder=8,
        bbox=dict(boxstyle="square,pad=0.22", facecolor="white", edgecolor="white", linewidth=5),
    )


def usecase_2x2(fname, title, actor_name, left_top, left_bot, right_top, right_bot="登录"):
    """
    仿范本图3.5：仅两个扇形关联 + 三条 include。
    加大列距/行距，include 标签强制放到空白坐标，杜绝压标题/压椭圆。
    """
    fig, ax = plt.subplots(figsize=(10.0, 6.2))
    ax.set_xlim(0, 10.0)
    ax.set_ylim(0, 6.2)
    ax.axis("off")

    # 系统框：顶部标题带加高
    x0, y0, x1, y1 = 2.55, 0.45, 9.7, 5.85
    frame(ax, x0, y0, x1, y1, title)

    # 2x2 位置：上排 y=4.15（低于标题带），下排 y=2.15；列间距拉大
    lt = oval(ax, 4.55, 4.15, left_top, w=2.35, h=0.85)
    lb = oval(ax, 4.55, 2.15, left_bot, w=2.35, h=0.85)
    rt = oval(ax, 7.75, 4.15, right_top, w=2.45, h=0.85)
    rb = oval(ax, 7.75, 2.05, right_bot, w=2.2, h=0.8)

    actor(ax, 1.15, 3.2, actor_name)
    fan(ax, (1.15, 3.2), [lt, lb])

    # 标签放在明确空白：上横线上方、下斜线下方、竖线右侧
    include(ax, lt, rt, label_xy=(6.15, 4.55))   # 上横，标签在线上方（仍低于标题）
    include(ax, lb, rt, label_xy=(6.15, 2.85))   # 斜线中段空白
    include(ax, rt, rb, label_xy=(8.55, 3.1))    # 竖线右侧

    return save(fig, fname)


def fig_arch_v2():
    """按范本图4.1：前端/后端大分区 + 左侧竖向层名 + 内部方框，无交叉遮挡。"""
    fig, ax = plt.subplots(figsize=(12.2, 8.6))
    ax.set_xlim(0, 12.2)
    ax.set_ylim(0, 8.6)
    ax.axis("off")

    def rbox(x, y, w, h, text, fs=10, fc="#FFFFFF"):
        ax.add_patch(Rectangle((x, y), w, h, fill=True, facecolor=fc, edgecolor="black", lw=1.2))
        ax.text(x + w / 2, y + h / 2, text, ha="center", va="center", fontsize=fs)

    def vlabel(x, y, w, h, text, fs=12):
        ax.add_patch(Rectangle((x, y), w, h, fill=True, facecolor="#FAFAFA", edgecolor="black", lw=1.4))
        ax.text(x + w / 2, y + h / 2, text, ha="center", va="center", fontsize=fs, rotation=90)

    # ===== 前端区 =====
    ax.add_patch(Rectangle((0.35, 5.15), 11.5, 3.15, fill=False, ec="black", lw=1.8))
    vlabel(0.45, 5.25, 0.55, 2.95, "前端", 13)
    rbox(1.15, 5.25, 0.7, 2.95, "视图层", 11, "#F7F7F7")

    # Web 端大框
    ax.add_patch(Rectangle((2.05, 5.35), 9.5, 2.75, fill=False, ec="black", lw=1.2))
    ax.text(6.8, 7.9, "Web 管理端（浏览器）", ha="center", va="center", fontsize=12)

    # 三角色页面示意
    rbox(2.25, 6.35, 2.9, 1.35, "管理员页面\n用户/冷库设备/工单派单\n维保备件/统计/日志", 9)
    rbox(5.35, 6.35, 2.9, 1.35, "运维人员页面\n工作台/工单处理\n故障案例/维保任务", 9)
    rbox(8.45, 6.35, 2.9, 1.35, "甲方用户页面\n设备浏览/报修验收\n评价/消息/个人中心", 9)

    # 前端技术栈小盒
    techs = ["Vue2", "ElementUI", "ECharts", "Axios", "Vue Router", "Vite"]
    tw = 1.4
    tx0 = 2.4
    for i, t in enumerate(techs):
        rbox(tx0 + i * (tw + 0.12), 5.5, tw, 0.55, t, 9)

    # ===== 后端区 =====
    ax.add_patch(Rectangle((0.35, 0.35), 11.5, 4.55, fill=False, ec="black", lw=1.8))
    vlabel(0.45, 0.45, 0.55, 4.35, "后端", 13)

    # 四层
    layers = [
        (3.85, "控制层", ["JWT拦截器", "Controller", "统一Result", "CORS", "WebSocket"]),
        (2.85, "业务逻辑层", ["认证/冷库/设备", "工单状态机", "故障维保", "备件库存", "统计消息Excel"]),
        (1.85, "数据访问层", ["MyBatis-Plus Mapper", "实体 Entity", "分页插件"]),
        (0.55, "数据层", ["MySQL 业务库", "Redis 缓存(可降级)", "本地文件 uploads"]),
    ]
    for y, name, items in layers:
        rbox(1.15, y, 1.35, 0.85, name, 11, "#F7F7F7")
        iw = (9.9) / len(items) - 0.12
        for i, it in enumerate(items):
            rbox(2.7 + i * (iw + 0.12), y + 0.12, iw, 0.6, it, 9)

    ax.set_title("图4.1 系统架构图", fontsize=13, pad=8)
    return save(fig, "fig4_1_arch.png")


def redraw_fixed():
    # 3.6/3.8/3.10 改为与范本一致的 2 关联 + include，避免三线汇聚遮挡
    usecase_2x2(
        "fig3_6_process.png",
        "处理运维工单",
        "运维人员",
        "接单",
        "提交完工",
        "填写过程与备件",
    )
    usecase_2x2(
        "fig3_8_user_mgmt.png",
        "管理用户",
        "管理员",
        "新增用户",
        "启用/禁用",
        "查询用户",
    )
    usecase_2x2(
        "fig3_10_order_admin.png",
        "工单全流程管控",
        "管理员",
        "派单",
        "转派",
        "关闭/归档",
    )
    fig_arch_v2()
    print("figs updated")


def patch_docx():
    """只替换指定图，不另存一堆文件。"""
    mapping = {
        "图3.6": "fig3_6_process.png",
        "图3.8": "fig3_8_user_mgmt.png",
        "图3.10": "fig3_10_order_admin.png",
        "图4.1": "fig4_1_arch.png",
    }
    if not FINAL.exists():
        raise SystemExit(f"missing {FINAL}")
    doc = Document(str(FINAL))
    paras = list(doc.paragraphs)
    n = 0
    for i, p in enumerate(paras):
        text = (p.text or "").replace(" ", "").replace("　", "")
        hit = None
        for key in sorted(mapping, key=len, reverse=True):
            if text.startswith(key.replace(" ", "")):
                hit = mapping[key]
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
        w = 14.2 if hit.startswith("fig4_") else 13.2
        prev.add_run().add_picture(str(FIG / hit), width=Cm(w))
        prev.alignment = WD_ALIGN_PARAGRAPH.CENTER
        n += 1
    try:
        doc.save(str(FINAL))
        print("updated", FINAL.name, "images", n)
    except PermissionError:
        alt = FINAL.with_name("冷库设备运维管理系统-论文第1-4章-已更新.docx")
        doc.save(str(alt))
        print("原文件被占用，已保存为", alt.name, "images", n)


if __name__ == "__main__":
    redraw_fixed()
    patch_docx()
