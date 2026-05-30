"""
局域网数据包捕获程序设计 — 课题18
基于 Python + Scapy + Tkinter 的局域网数据包捕获与分析工具

功能：
  1. 基于 scapy + Npcap 捕获以太网帧级数据包
  2. 解析 Ethernet / ARP / IP / TCP / UDP / ICMP 协议头部
  3. 图形界面展示数据包列表、协议详情树、十六进制原始数据
  4. 支持保存为 PCAP 格式（可用 Wireshark 打开）
  5. 支持从 PCAP 文件加载数据包
  6. 支持按协议类型过滤、列排序、示例数据生成

运行环境：
  - Windows 10/11, Python 3.8+
  - pip install scapy
  - 安装 Npcap（https://npcap.com/），实时捕获需管理员权限运行
"""

import struct
import threading
import time
import os
import sys
import random
from datetime import datetime
import tkinter as tk
from tkinter import ttk, messagebox, filedialog, scrolledtext

# ─── scapy 导入 ────────────────────────────────────────
try:
    from scapy.all import (sniff, wrpcap, rdpcap,
                           Ether, ARP, IP, TCP, UDP, ICMP, Raw, Padding)
    from scapy.packet import Packet as ScapyPacket
    SCAPY_AVAILABLE = True
except ImportError:
    SCAPY_AVAILABLE = False

# ─── 协议常量 ───────────────────────────────────────────

PROTO_NAMES = {1: "ICMP", 2: "IGMP", 6: "TCP", 17: "UDP", 89: "OSPF"}

TCP_FLAG_MASK = {
    0x01: "FIN", 0x02: "SYN", 0x04: "RST",
    0x08: "PSH", 0x10: "ACK", 0x20: "URG",
    0x40: "ECE", 0x80: "CWR"
}

PORT_SERVICES = {
    20: "FTP-DATA", 21: "FTP", 22: "SSH", 23: "Telnet",
    25: "SMTP", 53: "DNS", 67: "DHCP-Srv", 68: "DHCP-Cli",
    80: "HTTP", 110: "POP3", 123: "NTP", 143: "IMAP",
    161: "SNMP", 443: "HTTPS", 445: "SMB", 3306: "MySQL",
    3389: "RDP", 8080: "HTTP-Proxy"
}

ARP_OP_NAMES = {1: "请求(Who has?)", 2: "应答(Is at)"}


# ─── 数据包解析模块 ─────────────────────────────────────

class PacketParser:
    """从 scapy 捕获的原始数据包中提取各层协议信息，返回结构化字典"""

    @staticmethod
    def parse_scapy_packet(pkt):
        """输入 scapy Packet 对象，输出可显示的解析字典"""
        info = {}
        raw_bytes = bytes(pkt)
        info["raw"] = raw_bytes
        info["length"] = len(raw_bytes)
        info["summary"] = pkt.summary()

        # ── 以太网帧头 ──
        if Ether in pkt:
            eth = pkt[Ether]
            info["eth_src"] = eth.src
            info["eth_dst"] = eth.dst
            info["eth_type"] = eth.type
            info["eth_type_name"] = PacketParser._ethertype_name(eth.type)

        # ── ARP ──
        if ARP in pkt:
            arp = pkt[ARP]
            info["arp_hw_type"] = arp.hwtype
            info["arp_proto_type"] = arp.ptype
            info["arp_hw_size"] = arp.hwlen
            info["arp_proto_size"] = arp.plen
            info["arp_op"] = arp.op
            info["arp_op_name"] = ARP_OP_NAMES.get(arp.op, f"未知({arp.op})")
            info["arp_src_mac"] = arp.hwsrc
            info["arp_src_ip"] = arp.psrc
            info["arp_dst_mac"] = arp.hwdst
            info["arp_dst_ip"] = arp.pdst
            info["proto_name"] = "ARP"

        # ── IP 头部 ──
        if IP in pkt:
            ip = pkt[IP]
            info["ip_version"] = ip.version
            info["ip_ihl"] = (ip.ihl or 5) * 4
            info["ip_tos"] = ip.tos
            info["ip_len"] = ip.len
            info["ip_id"] = ip.id
            info["ip_flags"] = ip.flags.value
            info["ip_frag"] = ip.frag
            info["ip_ttl"] = ip.ttl
            info["ip_proto"] = ip.proto
            info["ip_chksum"] = ip.chksum
            info["ip_src"] = ip.src
            info["ip_dst"] = ip.dst
            info["proto_name"] = PROTO_NAMES.get(ip.proto, f"IP({ip.proto})")

            # ── TCP ──
            if TCP in pkt:
                tcp = pkt[TCP]
                info["tcp_sport"] = tcp.sport
                info["tcp_dport"] = tcp.dport
                info["tcp_seq"] = tcp.seq
                info["tcp_ack"] = tcp.ack
                info["tcp_dataofs"] = (tcp.dataofs or 5) * 4
                info["tcp_flags"] = tcp.flags.value
                info["tcp_window"] = tcp.window
                info["tcp_chksum"] = tcp.chksum
                info["tcp_urgptr"] = tcp.urgptr

                flag_strs = []
                for mask, name in TCP_FLAG_MASK.items():
                    if tcp.flags.value & mask:
                        flag_strs.append(name)
                info["tcp_flags_str"] = " ".join(flag_strs) if flag_strs else "NONE"

                for port_key, svc_key in [("tcp_sport", "tcp_sservice"),
                                           ("tcp_dport", "tcp_dservice")]:
                    port = info[port_key]
                    info[svc_key] = PORT_SERVICES.get(port, "")

                info["proto_name"] = "TCP"
                # 提取负载
                if Raw in tcp:
                    info["payload"] = bytes(tcp[Raw])

            # ── UDP ──
            elif UDP in pkt:
                udp = pkt[UDP]
                info["udp_sport"] = udp.sport
                info["udp_dport"] = udp.dport
                info["udp_len"] = udp.len
                info["udp_chksum"] = udp.chksum

                for port_key, svc_key in [("udp_sport", "udp_sservice"),
                                           ("udp_dport", "udp_dservice")]:
                    port = info[port_key]
                    info[svc_key] = PORT_SERVICES.get(port, "")

                info["proto_name"] = "UDP"
                if Raw in udp:
                    info["payload"] = bytes(udp[Raw])

            # ── ICMP ──
            elif ICMP in pkt:
                icmp = pkt[ICMP]
                info["icmp_type"] = icmp.type
                info["icmp_code"] = icmp.code
                info["icmp_chksum"] = icmp.chksum
                info["icmp_type_str"] = PacketParser._icmp_type_name(icmp.type)
                info["proto_name"] = "ICMP"
                if Raw in icmp:
                    info["payload"] = bytes(icmp[Raw])

        if "proto_name" not in info:
            info["proto_name"] = info.get("eth_type_name", "未知")

        return info

    @staticmethod
    def _ethertype_name(etype):
        mapping = {
            0x0800: "IPv4", 0x0806: "ARP", 0x86DD: "IPv6",
            0x8100: "802.1Q(VLAN)", 0x8864: "PPPoE-Discovery",
            0x8863: "PPPoE-Session"
        }
        return mapping.get(etype, f"0x{etype:04X}")

    @staticmethod
    def _icmp_type_name(itype):
        mapping = {
            0: "回显应答(Echo Reply)", 3: "目标不可达(Unreachable)",
            5: "重定向(Redirect)", 8: "回显请求(Echo Request)",
            11: "超时(Time Exceeded)"
        }
        return mapping.get(itype, f"类型{itype}")

    @staticmethod
    def get_summary(info):
        """生成数据包列表中的一行摘要文本"""
        proto = info.get("proto_name", "?")
        length = info.get("length", 0)

        if proto == "ARP":
            op = info.get("arp_op_name", "")
            src_ip = info.get("arp_src_ip", "?")
            dst_ip = info.get("arp_dst_ip", "?")
            return f"ARP {op} — 谁有 {dst_ip}？告诉 {src_ip}"

        src = info.get("ip_src", info.get("eth_src", "?"))
        dst = info.get("ip_dst", info.get("eth_dst", "?"))

        if proto == "TCP":
            sport = info.get("tcp_sport", "?")
            dport = info.get("tcp_dport", "?")
            flags = info.get("tcp_flags_str", "")
            svc = info.get("tcp_dservice", "") or info.get("tcp_sservice", "")
            svc_str = f" [{svc}]" if svc else ""
            return f"{src}:{sport} → {dst}:{dport}  [{flags}]  Len={length}{svc_str}"

        elif proto == "UDP":
            sport = info.get("udp_sport", "?")
            dport = info.get("udp_dport", "?")
            svc = info.get("udp_dservice", "") or info.get("udp_sservice", "")
            svc_str = f" [{svc}]" if svc else ""
            return f"{src}:{sport} → {dst}:{dport}  Len={length}{svc_str}"

        elif proto == "ICMP":
            itype = info.get("icmp_type_str", "")
            return f"{src} → {dst}  ICMP {itype}  Len={length}"

        return f"{src} → {dst}  {proto}  Len={length}"

    @staticmethod
    def generate_sample_packets(count=25):
        """生成模拟数据包用于无管理员权限时的功能演示。返回 scapy 包列表。"""
        packets = []
        src_macs = ["aa:bb:cc:11:22:33", "aa:bb:cc:44:55:66"]
        dst_mac = "ff:ff:ff:ff:ff:ff"       # ARP 广播
        unicast_dst = "00:11:22:33:44:55"
        local_ips = ["192.168.1.100", "192.168.1.101", "192.168.1.1"]
        remote_ips = ["142.250.80.46", "8.8.8.8", "223.5.5.5", "183.232.231.174",
                      "13.107.42.14", "114.114.114.114"]

        seq_base = random.randint(1000000, 9999999)

        scenarios = [
            # ARP 包
            lambda: (Ether(src=random.choice(src_macs), dst=dst_mac, type=0x0806)
                     / ARP(op=1, hwsrc=random.choice(src_macs), psrc="192.168.1.100",
                           hwdst="00:00:00:00:00:00", pdst="192.168.1.1")),
            lambda: (Ether(src="00:11:22:33:44:55", dst=random.choice(src_macs), type=0x0806)
                     / ARP(op=2, hwsrc="00:11:22:33:44:55", psrc="192.168.1.1",
                           hwdst=random.choice(src_macs), pdst="192.168.1.100")),
            # TCP SYN (三次握手)
            lambda: (Ether(src=random.choice(src_macs), dst=unicast_dst, type=0x0800)
                     / IP(src=random.choice(local_ips), dst=random.choice(remote_ips), ttl=64)
                     / TCP(sport=random.randint(49152, 65535), dport=80,
                           flags="S", seq=seq_base, window=65535)),
            lambda: (Ether(src=unicast_dst, dst=random.choice(src_macs), type=0x0800)
                     / IP(src=random.choice(remote_ips), dst=random.choice(local_ips), ttl=128)
                     / TCP(sport=80, dport=random.randint(49152, 65535),
                           flags="SA", seq=random.randint(1000000, 9999999),
                           ack=seq_base + 1, window=65535)),
            lambda: (Ether(src=random.choice(src_macs), dst=unicast_dst, type=0x0800)
                     / IP(src=random.choice(local_ips), dst=random.choice(remote_ips), ttl=64)
                     / TCP(sport=random.randint(49152, 65535), dport=80,
                           flags="A", seq=seq_base + 1, ack=1, window=65535)),
            # TCP SSH/Telnet
            lambda: (Ether(src=random.choice(src_macs), dst=unicast_dst, type=0x0800)
                     / IP(src=random.choice(local_ips), dst=random.choice(remote_ips), ttl=64)
                     / TCP(sport=random.randint(49152, 65535), dport=22, flags="S",
                           seq=random.randint(1000000, 9999999), window=65535)),
            lambda: (Ether(src=random.choice(src_macs), dst=unicast_dst, type=0x0800)
                     / IP(src=random.choice(local_ips), dst=random.choice(remote_ips), ttl=64)
                     / TCP(sport=random.randint(49152, 65535), dport=443, flags="S",
                           seq=random.randint(1000000, 9999999), window=65535)),
            # UDP DNS
            lambda: (Ether(src=random.choice(src_macs), dst=unicast_dst, type=0x0800)
                     / IP(src=random.choice(local_ips), dst="8.8.8.8", ttl=64)
                     / UDP(sport=random.randint(49152, 65535), dport=53)
                     / Raw(b'\x12\x34\x01\x00\x00\x01\x00\x00\x00\x00\x00\x00'
                           b'\x03www\x06google\x03com\x00\x00\x01\x00\x01')),
            lambda: (Ether(src=unicast_dst, dst=random.choice(src_macs), type=0x0800)
                     / IP(src="8.8.8.8", dst=random.choice(local_ips), ttl=128)
                     / UDP(sport=53, dport=random.randint(49152, 65535))
                     / Raw(b'\x12\x34\x81\x80\x00\x01\x00\x01\x00\x00\x00\x00'
                           b'\x03www\x06google\x03com\x00\x00\x01\x00\x01'
                           b'\xc0\x0c\x00\x01\x00\x01\x00\x00\x00\x3c\x00\x04\x8e\xfa\x50\x2e')),
            # UDP DHCP
            lambda: (Ether(src=random.choice(src_macs), dst=unicast_dst, type=0x0800)
                     / IP(src=random.choice(local_ips), dst="192.168.1.1", ttl=64)
                     / UDP(sport=68, dport=67)),
            # ICMP
            lambda: (Ether(src=random.choice(src_macs), dst=unicast_dst, type=0x0800)
                     / IP(src=random.choice(local_ips), dst=random.choice(remote_ips), ttl=64)
                     / ICMP(type=8, code=0)),
            lambda: (Ether(src=unicast_dst, dst=random.choice(src_macs), type=0x0800)
                     / IP(src=random.choice(remote_ips), dst=random.choice(local_ips), ttl=128)
                     / ICMP(type=0, code=0)),
            # 更多 TCP 到不同端口
            lambda: (Ether(src=random.choice(src_macs), dst=unicast_dst, type=0x0800)
                     / IP(src=random.choice(local_ips), dst=random.choice(remote_ips), ttl=64)
                     / TCP(sport=random.randint(49152, 65535), dport=25, flags="S",
                           seq=random.randint(1000000, 9999999), window=65535)),
            lambda: (Ether(src=random.choice(src_macs), dst=unicast_dst, type=0x0800)
                     / IP(src=random.choice(local_ips), dst=random.choice(remote_ips), ttl=64)
                     / TCP(sport=random.randint(49152, 65535), dport=3306, flags="S",
                           seq=random.randint(1000000, 9999999), window=65535)),
            # UDP NTP, SNMP
            lambda: (Ether(src=random.choice(src_macs), dst=unicast_dst, type=0x0800)
                     / IP(src=random.choice(local_ips), dst=random.choice(remote_ips), ttl=64)
                     / UDP(sport=random.randint(49152, 65535), dport=123)),
            lambda: (Ether(src=random.choice(src_macs), dst=unicast_dst, type=0x0800)
                     / IP(src=random.choice(local_ips), dst=random.choice(remote_ips), ttl=64)
                     / UDP(sport=random.randint(49152, 65535), dport=161)),
            # ICMP 目标不可达
            lambda: (Ether(src=unicast_dst, dst=random.choice(src_macs), type=0x0800)
                     / IP(src="192.168.1.1", dst=random.choice(local_ips), ttl=64)
                     / ICMP(type=3, code=3)),
            # TCP RST
            lambda: (Ether(src=unicast_dst, dst=random.choice(src_macs), type=0x0800)
                     / IP(src=random.choice(remote_ips), dst=random.choice(local_ips), ttl=128)
                     / TCP(sport=80, dport=random.randint(49152, 65535),
                           flags="RA", seq=random.randint(1000000, 9999999), window=0)),
        ]

        for i in range(min(count, len(scenarios))):
            try:
                pkt = scenarios[i]()
                pkt.time = time.time()
                packets.append(pkt)
            except Exception:
                continue

        return packets


# ─── 数据包捕获引擎 ─────────────────────────────────────

class PacketSniffer:
    """基于 scapy.sniff() 的数据包嗅探器，运行在后台线程中"""

    def __init__(self, callback):
        self.callback = callback          # 收到数据包时的回调
        self.running = False
        self.thread = None
        self.packet_count = 0
        self._sniffer = None             # AsyncSniffer 实例引用

    def start(self):
        if self.running:
            return True, "已在运行中"
        if not SCAPY_AVAILABLE:
            return False, "scapy 库未安装，请执行: pip install scapy"

        try:
            from scapy.all import AsyncSniffer
            self._sniffer = AsyncSniffer(
                prn=self._on_packet,
                store=False,
                quiet=True
            )
            self._sniffer.start()
            self.running = True
            return True, "捕获已启动（scapy sniff）"
        except PermissionError:
            return False, ("需要管理员权限！请以管理员身份运行程序。\n"
                           "如无管理员权限，可使用「生成示例数据」按钮演示功能。")
        except OSError as e:
            if "No matching devices" in str(e) or "Npcap" in str(e):
                return False, ("未找到可用的网络适配器。请确认已安装 Npcap。\n"
                               "下载地址: https://npcap.com/\n"
                               "安装时请勾选「Install Npcap in WinPcap API-compatible Mode」")
            return False, f"启动捕获失败: {e}"
        except Exception as e:
            return False, f"启动捕获失败: {e}"

    def _on_packet(self, pkt):
        """scapy 每收到一个包时调用（在捕获线程中）"""
        self.packet_count += 1
        timestamp = datetime.now().strftime("%H:%M:%S.%f")[:-3]
        try:
            info = PacketParser.parse_scapy_packet(pkt)
        except Exception:
            info = {"raw": bytes(pkt), "length": len(bytes(pkt)),
                    "proto_name": "?", "summary": "解析失败"}
        info["id"] = self.packet_count
        info["timestamp"] = timestamp
        info["scapy_pkt"] = pkt       # 保留原始 scapy 包引用（用于 PCAP 保存）
        self.callback(info)

    def stop(self):
        self.running = False
        if self._sniffer:
            try:
                self._sniffer.stop()
            except Exception:
                pass
            self._sniffer = None


# ─── 图形界面模块 ───────────────────────────────────────

class PacketCaptureGUI:
    """程序主界面"""

    def __init__(self):
        self.root = tk.Tk()
        self.root.title("局域网数据包捕获分析工具 — 课题18")
        self.root.geometry("1220x760")
        self.root.minsize(900, 550)

        self.packets = []           # 已捕获的解析信息列表
        self.scapy_packets = []     # 原始 scapy 包列表（用于 PCAP 保存）
        self._lock = threading.RLock()
        self.sniffer = PacketSniffer(self._on_packet)
        self.capture_filter = tk.StringVar(value="全部")
        self.status_text = tk.StringVar(value="就绪 — 点击 ▶ 开始捕获")
        self._sort_order = {}       # 列排序方向记录

        self._build_menu()
        self._build_toolbar()
        self._build_main_area()
        self._build_statusbar()

        self.root.protocol("WM_DELETE_WINDOW", self._on_close)

    # ── 菜单栏 ──────────────────────────────────────

    def _build_menu(self):
        menubar = tk.Menu(self.root)

        file_menu = tk.Menu(menubar, tearoff=0)
        file_menu.add_command(label="加载数据包 (PCAP)...",
                              command=self.load_pcap, accelerator="Ctrl+O")
        file_menu.add_command(label="保存为 PCAP...",
                              command=self.save_pcap, accelerator="Ctrl+S")
        file_menu.add_separator()
        file_menu.add_command(label="导出文本报告...",
                              command=self.export_text)
        file_menu.add_separator()
        file_menu.add_command(label="退出", command=self._on_close,
                              accelerator="Ctrl+Q")
        menubar.add_cascade(label="文件", menu=file_menu)

        capture_menu = tk.Menu(menubar, tearoff=0)
        capture_menu.add_command(label="开始捕获", command=self.start_capture,
                                 accelerator="Ctrl+R")
        capture_menu.add_command(label="停止捕获", command=self.stop_capture,
                                 accelerator="Ctrl+T")
        capture_menu.add_separator()
        capture_menu.add_command(label="生成示例数据包",
                                 command=self.generate_samples)
        capture_menu.add_command(label="清空列表", command=self.clear_packets)
        menubar.add_cascade(label="捕获", menu=capture_menu)

        view_menu = tk.Menu(menubar, tearoff=0)
        view_menu.add_command(label="展开全部详情", command=self._expand_all)
        view_menu.add_command(label="折叠全部详情", command=self._collapse_all)
        menubar.add_cascade(label="视图", menu=view_menu)

        help_menu = tk.Menu(menubar, tearoff=0)
        help_menu.add_command(label="关于", command=self._show_about)
        menubar.add_cascade(label="帮助", menu=help_menu)

        self.root.config(menu=menubar)

        # 快捷键绑定
        self.root.bind_all("<Control-r>", lambda e: self.start_capture())
        self.root.bind_all("<Control-t>", lambda e: self.stop_capture())
        self.root.bind_all("<Control-o>", lambda e: self.load_pcap())
        self.root.bind_all("<Control-s>", lambda e: self.save_pcap())
        self.root.bind_all("<Control-q>", lambda e: self._on_close())

    # ── 工具栏 ──────────────────────────────────────

    def _build_toolbar(self):
        toolbar = ttk.Frame(self.root)
        toolbar.pack(side=tk.TOP, fill=tk.X, padx=5, pady=(5, 0))

        self.btn_start = ttk.Button(toolbar, text="▶ 开始捕获",
                                    command=self.start_capture)
        self.btn_start.pack(side=tk.LEFT, padx=2)

        self.btn_stop = ttk.Button(toolbar, text="■ 停止捕获",
                                   command=self.stop_capture, state="disabled")
        self.btn_stop.pack(side=tk.LEFT, padx=2)

        ttk.Separator(toolbar, orient="vertical").pack(
            side=tk.LEFT, fill=tk.Y, padx=6)

        ttk.Button(toolbar, text="📋 示例数据",
                   command=self.generate_samples).pack(side=tk.LEFT, padx=2)
        ttk.Button(toolbar, text="📂 加载 PCAP",
                   command=self.load_pcap).pack(side=tk.LEFT, padx=2)
        ttk.Button(toolbar, text="💾 保存 PCAP",
                   command=self.save_pcap).pack(side=tk.LEFT, padx=2)
        ttk.Button(toolbar, text="🗑 清空",
                   command=self.clear_packets).pack(side=tk.LEFT, padx=2)

        ttk.Separator(toolbar, orient="vertical").pack(
            side=tk.LEFT, fill=tk.Y, padx=6, pady=2)

        ttk.Label(toolbar, text="过滤:").pack(side=tk.LEFT, padx=2)
        filter_combo = ttk.Combobox(toolbar, textvariable=self.capture_filter,
                                    width=18, state="readonly",
                                    values=["全部", "ARP", "TCP", "UDP", "ICMP",
                                            "HTTP(80)", "HTTPS(443)", "DNS(53)",
                                            "SSH(22)"])
        filter_combo.set("全部")
        filter_combo.pack(side=tk.LEFT, padx=2)
        filter_combo.bind("<<ComboboxSelected>>", lambda e: self._refresh_list())

        self.lbl_stats = ttk.Label(toolbar, text="数据包: 0")
        self.lbl_stats.pack(side=tk.RIGHT, padx=10)

    # ── 主区域 ──────────────────────────────────────

    def _build_main_area(self):
        main_pw = ttk.PanedWindow(self.root, orient=tk.VERTICAL)
        main_pw.pack(fill=tk.BOTH, expand=True, padx=5, pady=5)

        # 上半部分：包列表 + 详情面板
        top_pw = ttk.PanedWindow(main_pw, orient=tk.HORIZONTAL)
        main_pw.add(top_pw, weight=3)

        # ── 左侧：数据包列表 ──
        list_frame = ttk.LabelFrame(top_pw, text="数据包列表")
        top_pw.add(list_frame, weight=3)

        columns = ("id", "time", "src", "dst", "protocol", "length", "info")
        self.tree = ttk.Treeview(list_frame, columns=columns,
                                 show="headings", selectmode="browse")

        col_config = [
            ("id", "序号", 50, "center"),
            ("time", "时间", 95, "center"),
            ("src", "源地址", 165, "w"),
            ("dst", "目标地址", 165, "w"),
            ("protocol", "协议", 55, "center"),
            ("length", "长度", 55, "center"),
            ("info", "摘要信息", 320, "w"),
        ]
        for col, heading, width, anchor in col_config:
            self.tree.heading(col, text=heading,
                              command=lambda c=col: self._sort_by(c))
            self.tree.column(col, width=width, anchor=anchor)

        scroll_y = ttk.Scrollbar(list_frame, orient=tk.VERTICAL,
                                 command=self.tree.yview)
        self.tree.configure(yscrollcommand=scroll_y.set)
        self.tree.grid(row=0, column=0, sticky="nsew")
        scroll_y.grid(row=0, column=1, sticky="ns")
        list_frame.grid_rowconfigure(0, weight=1)
        list_frame.grid_columnconfigure(0, weight=1)

        self.tree.bind("<<TreeviewSelect>>", self._on_select_packet)

        # ── 右侧：协议详情树 ──
        detail_frame = ttk.LabelFrame(top_pw, text="数据包详情（分层协议字段）")
        top_pw.add(detail_frame, weight=2)

        self.detail_tree = ttk.Treeview(detail_frame, columns=("field", "value"),
                                        show="tree", selectmode="browse")
        self.detail_tree.heading("field", text="字段")
        self.detail_tree.heading("value", text="值")
        self.detail_tree.column("field", width=195)
        self.detail_tree.column("value", width=285)

        detail_scroll = ttk.Scrollbar(detail_frame, orient=tk.VERTICAL,
                                      command=self.detail_tree.yview)
        self.detail_tree.configure(yscrollcommand=detail_scroll.set)
        self.detail_tree.grid(row=0, column=0, sticky="nsew")
        detail_scroll.grid(row=0, column=1, sticky="ns")
        detail_frame.grid_rowconfigure(0, weight=1)
        detail_frame.grid_columnconfigure(0, weight=1)

        # ── 底部：十六进制视图 ──
        hex_frame = ttk.LabelFrame(main_pw, text="原始数据（十六进制 + ASCII）")
        main_pw.add(hex_frame, weight=1)

        self.hex_text = scrolledtext.ScrolledText(
            hex_frame, height=6, font=("Consolas", 10),
            state="disabled", wrap=tk.NONE)
        self.hex_text.pack(fill=tk.BOTH, expand=True)

        hex_scroll_x = ttk.Scrollbar(hex_frame, orient=tk.HORIZONTAL,
                                     command=self.hex_text.xview)
        hex_scroll_x.pack(side=tk.BOTTOM, fill=tk.X)
        self.hex_text.configure(xscrollcommand=hex_scroll_x.set)

    # ── 状态栏 ──────────────────────────────────────

    def _build_statusbar(self):
        statusbar = ttk.Frame(self.root, relief=tk.SUNKEN)
        statusbar.pack(side=tk.BOTTOM, fill=tk.X)

        ttk.Label(statusbar, textvariable=self.status_text).pack(
            side=tk.LEFT, padx=5, pady=2)
        self.lbl_count = ttk.Label(statusbar, text="")
        self.lbl_count.pack(side=tk.RIGHT, padx=5, pady=2)

    # ── 捕获控制 ────────────────────────────────────

    def start_capture(self):
        if self.sniffer.running:
            return
        success, msg = self.sniffer.start()
        if success:
            self.status_text.set("● 正在捕获数据包...")
            self.btn_start.config(state="disabled")
            self.btn_stop.config(state="normal")
        else:
            messagebox.showwarning("启动捕获失败", msg)
            self.status_text.set(f"启动失败: {msg}")

    def stop_capture(self):
        if not self.sniffer.running:
            return
        self.sniffer.stop()
        with self._lock:
            count = len(self.packets)
        self.status_text.set(
            f"■ 捕获已停止，共 {count} 个数据包")
        self.btn_start.config(state="normal")
        self.btn_stop.config(state="disabled")

    # ── 数据包回调 ──────────────────────────────────

    def _on_packet(self, info):
        """收到数据包时的回调（来自捕获线程，通过 after 切回主线程）"""
        truncate = False
        with self._lock:
            self.packets.append(info)
            self.scapy_packets.append(info.get("scapy_pkt"))
            # 内存保护：超过 20000 时截断
            if len(self.packets) > 20000:
                self.packets = self.packets[-10000:]
                self.scapy_packets = self.scapy_packets[-10000:]
                truncate = True
        self.root.after(0, self._add_packet_to_list, info)
        if truncate:
            self.root.after(0, self._prune_treeview_rows)

    def _add_packet_to_list(self, info):
        """在主线程中向 Treeview 中添加一行"""
        src = info.get("ip_src", info.get("arp_src_ip",
                     info.get("eth_src", "?")))
        dst = info.get("ip_dst", info.get("arp_dst_ip",
                     info.get("eth_dst", "?")))
        proto = info.get("proto_name", "?")
        length = info.get("length", 0)
        summary = PacketParser.get_summary(info)

        # 过滤检查
        filt = self.capture_filter.get()
        if not self._match_filter(proto, info, filt):
            return

        self.tree.insert("", tk.END, values=(
            info["id"], info["timestamp"], src, dst, proto, length, summary))

        # 自动滚动到最新
        children = self.tree.get_children()
        if children:
            self.tree.see(children[-1])
        self._update_stats()

    def _match_filter(self, proto, info, filt):
        """检查数据包是否符合过滤条件"""
        if filt == "全部":
            return True
        if filt == "ARP" and proto == "ARP":
            return True
        if filt == "TCP" and proto == "TCP":
            return True
        if filt == "UDP" and proto == "UDP":
            return True
        if filt == "ICMP" and proto == "ICMP":
            return True
        if filt == "HTTP(80)" and proto == "TCP":
            return (info.get("tcp_sport") == 80 or info.get("tcp_dport") == 80)
        if filt == "HTTPS(443)" and proto == "TCP":
            return (info.get("tcp_sport") == 443 or
                    info.get("tcp_dport") == 443)
        if filt == "DNS(53)" and proto == "UDP":
            return (info.get("udp_sport") == 53 or
                    info.get("udp_dport") == 53)
        if filt == "SSH(22)" and proto == "TCP":
            return (info.get("tcp_sport") == 22 or
                    info.get("tcp_dport") == 22)
        return False

    # ── 详情与十六进制显示 ────────────────────────────

    def _on_select_packet(self, event):
        selection = self.tree.selection()
        if not selection:
            return
        values = self.tree.item(selection[0], "values")
        if not values:
            return
        packet_id = int(values[0])
        with self._lock:
            for pkt in self.packets:
                if pkt.get("id") == packet_id:
                    self._show_detail(pkt)
                    return

    def _show_detail(self, info):
        """在详情面板中以分层树形式展示协议字段"""
        self.detail_tree.delete(*self.detail_tree.get_children())

        def add(parent, text, value="", expand=True):
            return self.detail_tree.insert(parent, tk.END, text=text,
                                           values=(value,), open=expand)

        def add_group(parent, title, fields, expand=True):
            grp = add(parent, title, "", expand)
            for field, val in fields:
                add(grp, field, str(val))
            return grp

        # Frame 概览
        fid = info.get("id", "?")
        frame_title = (f"Frame {fid}: {info.get('length', 0)} bytes on wire")
        frame_detail = f"捕获时间: {info.get('timestamp', '?')}"
        frame_node = add("", frame_title, frame_detail)

        # Ethernet 头部
        if "eth_src" in info:
            eth_fields = [
                ("源 MAC 地址", info.get("eth_src", "?")),
                ("目标 MAC 地址", info.get("eth_dst", "?")),
                ("帧类型 (EtherType)",
                 info.get("eth_type_name", f"0x{info.get('eth_type', 0):04X}")),
            ]
            add_group(frame_node, "Ethernet II", eth_fields)

        # ARP
        if "arp_op" in info:
            arp_fields = [
                ("硬件类型", f"{info.get('arp_hw_type', '?')} (1=以太网)"),
                ("协议类型", f"0x{info.get('arp_proto_type', 0):04X}"),
                ("硬件地址长度", f"{info.get('arp_hw_size', '?')} bytes"),
                ("协议地址长度", f"{info.get('arp_proto_size', '?')} bytes"),
                ("操作码", f"{info.get('arp_op', '?')} — "
                          f"{info.get('arp_op_name', '?')}"),
                ("发送方 MAC", info.get("arp_src_mac", "?")),
                ("发送方 IP", info.get("arp_src_ip", "?")),
                ("目标方 MAC", info.get("arp_dst_mac", "?")),
                ("目标方 IP", info.get("arp_dst_ip", "?")),
            ]
            add_group(frame_node, "Address Resolution Protocol (ARP)", arp_fields)

        # IP 头部
        if "ip_src" in info:
            flags_val = info.get("ip_flags", 0)
            df_bit = "●" if flags_val & 2 else "○"
            mf_bit = "●" if flags_val & 1 else "○"
            ip_fields = [
                ("版本", f"IPv{info.get('ip_version', 4)}"),
                ("头部长度", f"{info.get('ip_ihl', 20)} bytes"),
                ("服务类型 (ToS)", f"0x{info.get('ip_tos', 0):02X}"),
                ("总长度", f"{info.get('ip_len', 0)} bytes"),
                ("标识 (ID)", f"0x{info.get('ip_id', 0):04X}"),
                ("标志",
                 f"0x{flags_val:X}  DF={df_bit}  MF={mf_bit}"),
                ("片偏移", str(info.get("ip_frag", 0))),
                ("生存时间 (TTL)", str(info.get("ip_ttl", "?"))),
                ("协议", f"{info.get('proto_name', '?')} "
                         f"({info.get('ip_proto', '?')})"),
                ("头部校验和", f"0x{info.get('ip_chksum', 0):04X}"),
                ("源 IP 地址", info.get("ip_src", "?")),
                ("目标 IP 地址", info.get("ip_dst", "?")),
            ]
            ip_group = add_group(frame_node, "Internet Protocol (IP)", ip_fields)

            # TCP
            if "tcp_sport" in info:
                tcp_fields = [
                    ("源端口", PacketParser._fmt_port(info, "tcp_sport",
                                                       "tcp_sservice")),
                    ("目标端口", PacketParser._fmt_port(info, "tcp_dport",
                                                        "tcp_dservice")),
                    ("序列号 (Seq)", str(info.get("tcp_seq", "?"))),
                    ("确认号 (Ack)", str(info.get("tcp_ack", "?"))),
                    ("头部长度", f"{info.get('tcp_dataofs', 20)} bytes"),
                    ("标志位",
                     f"0x{info.get('tcp_flags', 0):03X} "
                     f"[{info.get('tcp_flags_str', '')}]"),
                    ("窗口大小", str(info.get("tcp_window", "?"))),
                    ("校验和", f"0x{info.get('tcp_chksum', 0):04X}"),
                    ("紧急指针", str(info.get("tcp_urgptr", "?"))),
                ]
                add_group(ip_group, "Transmission Control Protocol (TCP)",
                          tcp_fields)

            # UDP
            elif "udp_sport" in info:
                udp_fields = [
                    ("源端口", PacketParser._fmt_port(info, "udp_sport",
                                                       "udp_sservice")),
                    ("目标端口", PacketParser._fmt_port(info, "udp_dport",
                                                        "udp_dservice")),
                    ("UDP 长度", f"{info.get('udp_len', 0)} bytes"),
                    ("校验和", f"0x{info.get('udp_chksum', 0):04X}"),
                ]
                add_group(ip_group, "User Datagram Protocol (UDP)", udp_fields)

            # ICMP
            elif "icmp_type" in info:
                icmp_fields = [
                    ("类型", f"{info.get('icmp_type', '?')} — "
                             f"{info.get('icmp_type_str', '?')}"),
                    ("代码", str(info.get("icmp_code", "?"))),
                    ("校验和", f"0x{info.get('icmp_chksum', 0):04X}"),
                ]
                add_group(ip_group, "Internet Control Message Protocol (ICMP)",
                          icmp_fields)

        # 显示原始数据
        self._show_hex(info.get("raw", b""))

    @staticmethod
    def _fmt_port(info, port_key, svc_key):
        port = info.get(port_key, "?")
        svc = info.get(svc_key, "")
        return f"{port}" + (f" ({svc})" if svc else "")

    def _show_hex(self, raw):
        """在底部面板展示十六进制 + ASCII 对照"""
        self.hex_text.config(state="normal")
        self.hex_text.delete("1.0", tk.END)
        if not raw:
            self.hex_text.insert("1.0", "(无原始数据)")
            self.hex_text.config(state="disabled")
            return

        lines = []
        for offset in range(0, min(len(raw), 4096), 16):  # 最多显示 4KB
            chunk = raw[offset:offset + 16]
            hex_part = " ".join(f"{b:02X}" for b in chunk).ljust(47)
            ascii_part = "".join(
                chr(b) if 32 <= b < 127 else "." for b in chunk)
            lines.append(f"{offset:04X}  {hex_part}  {ascii_part}")
        if len(raw) > 4096:
            lines.append(f"... (共 {len(raw)} bytes，仅显示前 4096 bytes)")

        self.hex_text.insert("1.0", "\n".join(lines))
        self.hex_text.config(state="disabled")

    # ── PCAP 保存 / 加载 ─────────────────────────────

    def save_pcap(self):
        """将捕获的原始 scapy 包保存为 PCAP 格式"""
        if not SCAPY_AVAILABLE:
            messagebox.showwarning("功能不可用", "scapy 库未安装，无法保存 PCAP。\n请执行: pip install scapy")
            return
        if not self.scapy_packets:
            if self.packets:
                # 场景：数据来自 JSON 加载，没有原始 scapy 包
                messagebox.showinfo("提示",
                                    "当前数据包缺少原始 scapy 引用，无法保存为 PCAP。\n"
                                    "请先捕获新数据包或生成示例数据后再保存。")
            else:
                messagebox.showinfo("提示", "没有数据包可以保存")
            return

        file_path = filedialog.asksaveasfilename(
            title="保存为 PCAP 文件",
            defaultextension=".pcap",
            filetypes=[("PCAP 文件", "*.pcap"), ("PCAPNG 文件", "*.pcapng"),
                       ("所有文件", "*.*")]
        )
        if not file_path:
            return

        try:
            with self._lock:
                pkts_to_save = list(self.scapy_packets)
            wrpcap(file_path, pkts_to_save)
            count = len(pkts_to_save)
            self.status_text.set(
                f"已保存 {count} 个数据包到: "
                f"{os.path.basename(file_path)} (PCAP 格式)")
            messagebox.showinfo("保存成功",
                                f"成功保存 {count} 个数据包\n"
                                f"文件: {file_path}\n"
                                f"可用 Wireshark 直接打开查看。")
        except Exception as e:
            messagebox.showerror("保存失败", str(e))

    def load_pcap(self):
        """从 PCAP 文件加载数据包"""
        if not SCAPY_AVAILABLE:
            messagebox.showwarning("功能不可用", "scapy 库未安装，无法加载 PCAP。\n请执行: pip install scapy")
            return
        file_path = filedialog.askopenfilename(
            title="加载 PCAP 文件",
            filetypes=[("PCAP/PCAPNG 文件", "*.pcap *.pcapng *.cap"),
                       ("所有文件", "*.*")]
        )
        if not file_path:
            return

        try:
            scapy_pkts = rdpcap(file_path)
            if not scapy_pkts:
                messagebox.showinfo("提示", "文件中没有数据包")
                return

            with self._lock:
                start_id = len(self.packets)
            for i, pkt in enumerate(scapy_pkts):
                info = PacketParser.parse_scapy_packet(pkt)
                info["id"] = start_id + i + 1
                ts = pkt.time if hasattr(pkt, "time") else time.time()
                info["timestamp"] = datetime.fromtimestamp(float(ts)).strftime(
                    "%H:%M:%S.%f")[:-3]
                info["scapy_pkt"] = pkt
                with self._lock:
                    self.packets.append(info)
                    self.scapy_packets.append(pkt)

            self._refresh_list()
            self.status_text.set(
                f"已从 {os.path.basename(file_path)} 加载 "
                f"{len(scapy_pkts)} 个数据包 (PCAP)")
            messagebox.showinfo("加载成功",
                                f"成功加载 {len(scapy_pkts)} 个数据包\n"
                                f"来源: {file_path}")
        except Exception as e:
            messagebox.showerror("加载失败",
                                 f"无法读取文件: {e}\n请确认文件为有效的 PCAP 格式。")

    def export_text(self):
        """导出可读文本报告"""
        if not self.packets:
            messagebox.showinfo("提示", "没有数据包可以导出")
            return

        file_path = filedialog.asksaveasfilename(
            title="导出文本报告",
            defaultextension=".txt",
            filetypes=[("文本文件", "*.txt"), ("所有文件", "*.*")]
        )
        if not file_path:
            return

        try:
            with open(file_path, "w", encoding="utf-8") as f:
                f.write("=" * 70 + "\n")
                f.write("  局域网数据包捕获分析报告\n")
                f.write(f"  导出时间: {datetime.now():%Y-%m-%d %H:%M:%S}\n")
                f.write(f"  数据包总数: {len(self.packets)}\n")
                f.write("=" * 70 + "\n\n")

                for p in self.packets:
                    f.write(f"[{p.get('id', '?')}] {p.get('timestamp', '?')}\n")
                    f.write(f"  协议: {p.get('proto_name', '?')}\n")
                    f.write(f"  长度: {p.get('length', 0)} bytes\n")
                    if "eth_src" in p:
                        f.write(f"  Eth: {p['eth_src']} → {p['eth_dst']}\n")
                    if "arp_op" in p:
                        f.write(f"  ARP: {p.get('arp_op_name', '?')} — "
                                f"{p.get('arp_src_ip', '?')} ↔ "
                                f"{p.get('arp_dst_ip', '?')}\n")
                    if "ip_src" in p:
                        f.write(f"  IP: {p['ip_src']} → {p['ip_dst']} "
                                f"(TTL={p.get('ip_ttl', '?')})\n")
                    if "tcp_sport" in p:
                        f.write(f"  TCP: {p['tcp_sport']} → {p['tcp_dport']}  "
                                f"[{p.get('tcp_flags_str', '')}]\n")
                    elif "udp_sport" in p:
                        f.write(f"  UDP: {p['udp_sport']} → {p['udp_dport']}\n")
                    elif "icmp_type" in p:
                        f.write(f"  ICMP: type={p['icmp_type']} "
                                f"({p.get('icmp_type_str', '')})\n")
                    f.write("\n")

            self.status_text.set(f"已导出报告: {os.path.basename(file_path)}")
            messagebox.showinfo("导出成功", f"报告已导出到:\n{file_path}")
        except Exception as e:
            messagebox.showerror("导出失败", str(e))

    # ── 示例数据 ────────────────────────────────────

    def generate_samples(self):
        if not SCAPY_AVAILABLE:
            messagebox.showwarning("功能不可用", "scapy 库未安装，无法生成示例数据。\n请执行: pip install scapy")
            return
        if self.packets and not messagebox.askyesno(
            "确认", "当前列表已有数据，新示例将追加到末尾。是否继续？"):
            return

        try:
            samples = PacketParser.generate_sample_packets(25)
            with self._lock:
                start_id = len(self.packets)
            for i, pkt in enumerate(samples):
                info = PacketParser.parse_scapy_packet(pkt)
                info["id"] = start_id + i + 1
                info["timestamp"] = datetime.now().strftime(
                    "%H:%M:%S.%f")[:-3]
                info["scapy_pkt"] = pkt
                with self._lock:
                    self.packets.append(info)
                    self.scapy_packets.append(pkt)

            self._refresh_list()
            self.status_text.set(
                f"已生成 {len(samples)} 个示例数据包（含 ARP/TCP/UDP/ICMP）")
        except Exception as e:
            messagebox.showerror("生成失败", str(e))

    def clear_packets(self):
        if not self.packets:
            return
        if messagebox.askyesno("确认",
                               f"确定清空全部 {len(self.packets)} 个数据包？"):
            with self._lock:
                self.packets.clear()
                self.scapy_packets.clear()
            self.tree.delete(*self.tree.get_children())
            self.detail_tree.delete(*self.detail_tree.get_children())
            self.hex_text.config(state="normal")
            self.hex_text.delete("1.0", tk.END)
            self.hex_text.config(state="disabled")
            self._update_stats()
            self.status_text.set("已清空所有数据包")

    # ── 辅助方法 ────────────────────────────────────

    def _prune_treeview_rows(self):
        """截断后同步清理 Treeview 中已不在 self.packets 的旧行"""
        with self._lock:
            existing_ids = {pkt.get("id") for pkt in self.packets}
        for child in list(self.tree.get_children()):
            values = self.tree.item(child, "values")
            if values:
                try:
                    if int(values[0]) not in existing_ids:
                        self.tree.delete(child)
                except (ValueError, IndexError):
                    pass

    def _refresh_list(self):
        self.tree.delete(*self.tree.get_children())
        with self._lock:
            for info in self.packets:
                self._add_packet_to_list(info)

    def _update_stats(self):
        with self._lock:
            count = len(self.packets)
        self.lbl_stats.config(text=f"数据包: {count}")
        self.lbl_count.config(text=f"共 {count} 个数据包")

    def _sort_by(self, column):
        col_idx = {"id": 0, "time": 1, "src": 2, "dst": 3,
                   "protocol": 4, "length": 5, "info": 6}
        idx = col_idx.get(column, 0)
        reverse = self._sort_order.get(column, False)
        self._sort_order[column] = not reverse

        children = list(self.tree.get_children())
        items = [(self.tree.set(c, column), c) for c in children]
        try:
            if column in ("id", "length"):
                items.sort(key=lambda x: int(x[0]) if x[0].isdigit() else 0,
                           reverse=reverse)
            else:
                items.sort(key=lambda x: x[0], reverse=reverse)
        except Exception:
            items.sort(key=lambda x: x[0], reverse=reverse)

        for i, (_, child) in enumerate(items):
            self.tree.move(child, "", i)

    def _expand_all(self):
        for item in self.detail_tree.get_children():
            self._expand_recursive(item)

    def _expand_recursive(self, item):
        self.detail_tree.item(item, open=True)
        for child in self.detail_tree.get_children(item):
            self._expand_recursive(child)

    def _collapse_all(self):
        for item in self.detail_tree.get_children():
            self._collapse_recursive(item)

    def _collapse_recursive(self, item):
        self.detail_tree.item(item, open=False)
        for child in self.detail_tree.get_children(item):
            self._collapse_recursive(child)

    def _show_about(self):
        messagebox.showinfo("关于",
                            "局域网数据包捕获分析工具\n\n"
                            "课题18：局域网数据包捕获程序设计\n"
                            "南京信息工程大学 计算机网络课程设计\n\n"
                            "技术栈: Python + Scapy + Tkinter\n"
                            "保存格式: PCAP (兼容 Wireshark)\n\n"
                            "功能：\n"
                            "• 实时捕获局域网数据包（以太网帧级）\n"
                            "• 解析 Ethernet / ARP / IP / TCP / UDP / ICMP\n"
                            "• 分层树形展示协议字段\n"
                            "• 十六进制原始数据查看\n"
                            "• 保存 / 加载 PCAP 格式文件\n"
                            "• 协议过滤与列排序\n"
                            "• 示例数据生成（无需管理员权限）")

    def _on_close(self):
        if self.sniffer.running:
            self.sniffer.stop()
        self.root.destroy()

    def run(self):
        self.root.mainloop()


# ─── 入口 ──────────────────────────────────────────────

if __name__ == "__main__":
    if not SCAPY_AVAILABLE:
        print("=" * 55)
        print("  警告: scapy 未安装")
        print("  请执行: pip install scapy")
        print("  程序将以受限模式启动（仅示例数据 / 加载 PCAP）")
        print("=" * 55)
        print()
    else:
        # 检测 Npcap
        try:
            from scapy.all import conf
            if not conf.ifaces or len(list(conf.ifaces)) <= 1:
                print("提示: 未检测到可用的网络接口。请确认已安装 Npcap。")
                print("下载: https://npcap.com/")
        except Exception:
            pass

    # 权限提示
    import ctypes
    try:
        is_admin = ctypes.windll.shell32.IsUserAnAdmin()
    except Exception:
        is_admin = False

    if not is_admin:
        print("=" * 55)
        print("  提示: 当前未以管理员身份运行")
        print("  实时捕获需要管理员权限")
        print("  可先使用「生成示例数据」或「加载 PCAP」演示")
        print("=" * 55)
        print()

    app = PacketCaptureGUI()
    app.run()
