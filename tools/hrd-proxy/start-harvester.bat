@echo off
REM HRD 세션 하베스터 — HRD 행정프로그램 쿠키를 전광판 서버로 자동 전송
REM 사전 1회: pip install mitmproxy / http://mitm.it 에서 CA 신뢰 / Windows 프록시 127.0.0.1:8899

set DASHBOARD_URL=http://woori10-0.iptime.org:4402/coolapi/hrd/session
REM 연동자 이름(누가 연동했는지 웹에 표시). 비우면 Windows 사용자명 사용
set SOURCE=%USERNAME%
REM 서버 hrd.harvest.token 설정 시 동일 값 입력(네트워크 노출 보호)
set HARVEST_TOKEN=
set HEARTBEAT_SEC=30

cd /d %~dp0
echo [hrd-harvester] mitmdump 시작 (포트 8899)
echo   서버: %DASHBOARD_URL%
echo   연동자: %SOURCE%
mitmdump -s hrd_harvest.py --listen-port 8899
