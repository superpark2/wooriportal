# HRD 세션 프록시 하베스터

HRD 행정프로그램이 들고 있는 로그인 세션(JSESSIONID/WMONID)을 자동으로 수확해
전광판 서버(wooriportal)에 실시간으로 밀어넣는다. HTTP Toolkit 을 상시 하베스터로 대체.

## 동작

```
HRD 행정프로그램 ──▶ mitmproxy(:8899) ──▶ www.hrd.go.kr:44381
                        │
                        └─ Cookie 헤더에서 JSESSIONID/WMONID 추출
                           └─▶ POST {dashboard}/coolapi/hrd/session  (변경 시에만)
```

## 설치 / 실행

```bash
pip install mitmproxy

# 하베스터 구동 (헤드리스)
set DASHBOARD_URL=http://localhost:8080/coolapi/hrd/session
set HARVEST_TOKEN=             # 서버 hrd.harvest.token 설정 시 동일 값
mitmdump -s hrd_harvest.py --listen-port 8899
```

1. **CA 1회 신뢰**: 프록시 켠 상태에서 브라우저로 <http://mitm.it> → Windows 인증서 설치
   (HTTP Toolkit 쓸 때 했던 것과 동일한 절차).
2. **HRD 프로그램이 프록시를 타게**: Windows 시스템 프록시를 `127.0.0.1:8899` 로 설정
   하거나, XPLATFORM 실행 옵션의 프록시를 지정. 앱을 켜고 한 번 조회하면 즉시 수확됨.

## 확인

```
GET http://localhost:8080/coolapi/hrd/session/status
→ {"present":true,"harvestedAt":"...","ageSeconds":3}
```

`present:true` 면 서버가 살아있는 세션을 확보한 것. 이후 폴러가 이 쿠키로 HRD 를 직접 호출한다.

## 주의

- 세션은 HRD 앱이 켜져 있고 활동/유지되는 동안만 살아있다. 앱을 끄거나 세션이
  만료되면 `ageSeconds` 가 계속 커지고 호출이 401/리다이렉트로 떨어진다 → 앱에서
  다시 조회하면 새 쿠키가 자동 수확된다.
- 운영 전환 시에는 서버 자동로그인(공인인증서)으로 격상 검토.
