# HRD 세션 프록시 하베스터

HRD 행정프로그램이 들고 있는 로그인 세션(JSESSIONID/WMONID)을 자동으로 수확해
전광판 서버(wooriportal)에 실시간으로 밀어넣는다. HTTP Toolkit 을 상시 하베스터로 대체.

## 동작

```
HRD 행정프로그램 ──▶ mitmproxy(:8899) ──▶ www.hrd.go.kr:44381
                        │
                        └─ Cookie 헤더에서 JSESSIONID/WMONID 추출
                           └─▶ POST {dashboard}/coolapi/hrd/session
                               (변경 시 + 60초 하트비트)
```

세션값이 안 바뀌어도 60초마다 재전송(하트비트)하므로, 대시보드 서버를 재시작해도
1분 내 세션이 자동 복구된다.

## 1회 설정

1. **mitmproxy 설치**: `pip install mitmproxy`
2. **CA 신뢰**: `start-harvester.bat` 실행 후 브라우저로 <http://mitm.it> → Windows 인증서 설치
   (HTTP Toolkit 때와 동일 절차. mitmproxy 는 자체 CA 라 한 번 더 필요).
3. **Windows 프록시 지정**: 설정 → 네트워크 → 프록시 → 수동, `127.0.0.1:8899`.
   XPLATFORM(IE/WinINet 기반)은 이 시스템 프록시를 따른다.

## 실행

```
tools\hrd-proxy\start-harvester.bat
```
포트 8899 로 상주. HRD 프로그램을 켜고 출결을 한 번 조회하면 즉시 수확된다.
(`start-harvester.bat` 안에서 DASHBOARD_URL/포트 조정 가능)

## 확인

```
GET http://localhost:4402/coolapi/hrd/session/status
→ {"present":true, ...}
```
`present:true` 면 세션 확보 완료. 이후 폴러가 이 쿠키로 HRD 를 직접 호출한다.

## 부팅 시 자동 실행 (선택)

작업 스케줄러에 `start-harvester.bat` 을 "로그온 시 실행"으로 등록하면 무인 운영.
(또는 바로가기를 `shell:startup` 폴더에 넣기)

## 주의

- 세션은 HRD 앱이 켜져 있고 세션이 유지되는 동안만 유효하다. 앱을 끄거나 HRD
  서버 세션이 만료되면 호출이 로그인 리다이렉트로 떨어진다 → 앱에서 다시
  로그인/조회하면 새 쿠키가 자동 수확된다.
- 시스템 프록시를 켜면 모든 트래픽이 mitmproxy 를 경유한다. 전용 PC 가 아니면
  필요 시에만 프록시를 켜는 것을 권장.
- 운영 고도화 시 서버 자동로그인(공인인증서)으로 격상 검토.
