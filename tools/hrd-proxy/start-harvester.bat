@echo off
REM HRD 세션 하베스터 — HRD 행정프로그램 쿠키를 전광판 서버로 자동 전송
REM 사전 1회: pip install mitmproxy / http://mitm.it 에서 CA 신뢰 / Windows 프록시 127.0.0.1:8899

set DASHBOARD_URL=http://localhost:4402/coolapi/hrd/session
set HARVEST_TOKEN=
set HEARTBEAT_SEC=60

cd /d %~dp0
echo [hrd-harvester] mitmdump 시작 (포트 8899) — 대시보드: %DASHBOARD_URL%
mitmdump -s hrd_harvest.py --listen-port 8899
