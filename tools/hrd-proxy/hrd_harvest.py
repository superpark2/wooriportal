"""
HRD-Net 세션 하베스터 (mitmproxy 애드온)

HRD 데스크탑 행정프로그램(XPLATFORM)을 이 프록시 경유로 띄워두면,
www.hrd.go.kr 으로 가는 모든 요청의 Cookie 헤더에서 JSESSIONID / WMONID 를
실시간으로 뽑아 전광판 서버(wooriportal)에 밀어넣는다.

실행:
    pip install mitmproxy
    mitmdump -s hrd_harvest.py --listen-port 8899
    # CA 1회 신뢰: http://mitm.it 접속 후 Windows 인증서 설치(HTTP Toolkit 때와 동일)
    # HRD 프로그램이 시스템 프록시(127.0.0.1:8899)를 타도록 설정

환경변수:
    DASHBOARD_URL   기본 http://localhost:8080/coolapi/hrd/session
    HARVEST_TOKEN   서버 hrd.harvest.token 과 동일하게(설정 시)
"""
import os
import json
import time
import threading
import urllib.request

DASHBOARD_URL = os.environ.get("DASHBOARD_URL", "http://localhost:4402/coolapi/hrd/session")
HARVEST_TOKEN = os.environ.get("HARVEST_TOKEN", "")
HRD_HOST = "www.hrd.go.kr"
# 값이 안 바뀌어도 이 주기(초)마다 재전송 — 대시보드 재시작 후 세션 복구용
HEARTBEAT_SEC = int(os.environ.get("HEARTBEAT_SEC", "60"))

_last_sent = {"jsessionId": None, "wmonid": None, "at": 0.0}
_lock = threading.Lock()


def _parse_cookies(cookie_header: str) -> dict:
    out = {}
    for part in cookie_header.split(";"):
        if "=" in part:
            k, v = part.strip().split("=", 1)
            out[k.strip()] = v.strip()
    return out


def _push(jsession: str, wmonid: str):
    body = json.dumps({"jsessionId": jsession, "wmonid": wmonid}).encode("utf-8")
    req = urllib.request.Request(DASHBOARD_URL, data=body, method="POST")
    req.add_header("Content-Type", "application/json")
    if HARVEST_TOKEN:
        req.add_header("X-Harvest-Token", HARVEST_TOKEN)
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            resp.read()
    except Exception as e:  # 서버 미기동 등은 조용히 무시(다음 요청 때 재시도)
        print(f"[hrd_harvest] push 실패: {e}")


def request(flow):
    if HRD_HOST not in flow.request.pretty_host:
        return
    cookie_header = flow.request.headers.get("Cookie", "")
    if not cookie_header:
        return
    cookies = _parse_cookies(cookie_header)
    jsession = cookies.get("JSESSIONID")
    wmonid = cookies.get("WMONID")
    if not jsession:
        return

    with _lock:
        unchanged = _last_sent["jsessionId"] == jsession and _last_sent["wmonid"] == wmonid
        fresh = (time.time() - _last_sent["at"]) < HEARTBEAT_SEC
        if unchanged and fresh:
            return  # 변경 없고 하트비트 주기 내 → 생략
        _last_sent["jsessionId"] = jsession
        _last_sent["wmonid"] = wmonid
        _last_sent["at"] = time.time()

    print(f"[hrd_harvest] 세션 전송 JSESSIONID=...{jsession[-6:]} WMONID={wmonid}")
    threading.Thread(target=_push, args=(jsession, wmonid), daemon=True).start()
