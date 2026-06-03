"""
HRD-Net 세션 + 요청템플릿 하베스터 (mitmproxy 애드온)

HRD 데스크탑 행정프로그램(XPLATFORM)을 이 프록시 경유로 띄워두면, www.hrd.go.kr 으로
가는 요청에서 두 가지를 전광판 서버(wooriportal)로 자동 전송한다:
  1) 세션 쿠키(JSESSIONID/WMONID)
  2) 요청 본문 템플릿(인증서 gds_userInfo 포함) — selectAtendList / selectDailAtndceDetail
     → 이 본문(인증서)이 실인증 수단이라, 최근 사용자의 자격으로 서버가 자동 동작.
       (특정 개인 인증서 만료에 묶이지 않음)

실행:
    pip install mitmproxy
    mitmdump -s hrd_harvest.py --listen-port 8899

환경변수:
    DASHBOARD_URL   기본 http://woori10-0.iptime.org:4402/coolapi/hrd/session
    HARVEST_TOKEN   서버 hrd.harvest.token 과 동일하게(설정 시)
    SOURCE          연동자 이름(웹 표시). 기본 = Windows 사용자명/호스트명
"""
import os
import json
import time
import socket
import threading
import urllib.request
import urllib.error

DASHBOARD_URL = os.environ.get("DASHBOARD_URL", "http://woori10-0.iptime.org:4402/coolapi/hrd/session")
HARVEST_TOKEN = os.environ.get("HARVEST_TOKEN", "")
SOURCE = os.environ.get("SOURCE") or os.environ.get("USERNAME") or socket.gethostname()
HRD_HOST = "www.hrd.go.kr"
HEARTBEAT_SEC = int(os.environ.get("HEARTBEAT_SEC", "30"))

# 서버 루트 도출 → 템플릿 업로드 URL
SERVER_ROOT = DASHBOARD_URL.split("/coolapi")[0]
TEMPLATE_URL = SERVER_ROOT + "/coolapi/hrd/harvester/template"

# 요청 경로 → 템플릿 엔드포인트 키
TEMPLATE_ENDPOINTS = {
    "selectAtendList.do": "list",
    "selectDailAtndceDetail.do": "detail",
}

_last_sent = {"jsessionId": None, "wmonid": None, "at": 0.0}
_last_tpl = {}  # endpoint -> hash
_lock = threading.Lock()


def _parse_cookies(cookie_header: str) -> dict:
    out = {}
    for part in cookie_header.split(";"):
        if "=" in part:
            k, v = part.strip().split("=", 1)
            out[k.strip()] = v.strip()
    return out


def _post(url, data, content_type):
    req = urllib.request.Request(url, data=data, method="POST")
    req.add_header("Content-Type", content_type)
    if HARVEST_TOKEN:
        req.add_header("X-Harvest-Token", HARVEST_TOKEN)
    with urllib.request.urlopen(req, timeout=5) as resp:
        resp.read()


def _push_session(jsession, wmonid):
    body = json.dumps({"jsessionId": jsession, "wmonid": wmonid, "source": SOURCE}).encode("utf-8")
    try:
        _post(DASHBOARD_URL, body, "application/json")
        print(f"[hrd_harvest] 세션 연동됨 (source={SOURCE})")
    except urllib.error.HTTPError as e:
        if e.code == 409:
            print("[hrd_harvest] 다른 사용자가 연동 중 — 웹에서 'HRD 연동'으로 전환하세요")
        else:
            print(f"[hrd_harvest] 세션 push 실패: HTTP {e.code}")
    except Exception as e:
        print(f"[hrd_harvest] 세션 push 실패: {e}")


def _push_template(endpoint, body_bytes):
    try:
        _post(TEMPLATE_URL + "?endpoint=" + endpoint, body_bytes, "application/octet-stream")
        print(f"[hrd_harvest] 템플릿 전송: {endpoint} ({len(body_bytes)} bytes)")
    except Exception as e:
        print(f"[hrd_harvest] 템플릿 push 실패({endpoint}): {e}")


def request(flow):
    if HRD_HOST not in flow.request.pretty_host:
        return

    # 1) 세션 쿠키
    cookie_header = flow.request.headers.get("Cookie", "")
    cookies = _parse_cookies(cookie_header) if cookie_header else {}
    jsession = cookies.get("JSESSIONID")
    wmonid = cookies.get("WMONID")
    if jsession:
        with _lock:
            unchanged = _last_sent["jsessionId"] == jsession and _last_sent["wmonid"] == wmonid
            fresh = (time.time() - _last_sent["at"]) < HEARTBEAT_SEC
            send = not (unchanged and fresh)
            if send:
                _last_sent.update({"jsessionId": jsession, "wmonid": wmonid, "at": time.time()})
        if send:
            threading.Thread(target=_push_session, args=(jsession, wmonid), daemon=True).start()

    # 2) 요청 본문 템플릿(인증서 포함) — 대상 엔드포인트만
    path = flow.request.path or ""
    for needle, endpoint in TEMPLATE_ENDPOINTS.items():
        if needle in path:
            body = flow.request.raw_content or b""
            if len(body) >= 2 and body[0] == 0xFF and body[1] == 0xAD:
                h = hash(body)
                with _lock:
                    changed = _last_tpl.get(endpoint) != h
                    if changed:
                        _last_tpl[endpoint] = h
                if changed:
                    threading.Thread(target=_push_template, args=(endpoint, body), daemon=True).start()
            break
