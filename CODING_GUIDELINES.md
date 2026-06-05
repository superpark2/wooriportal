# 코딩 가이드라인

이 프로젝트의 코드를 작성할 때 지켜야 할 규칙입니다.

## 1. HashMap 사용 자제 (필요 시 DTO 사용)

- `Map<String, Object>` 같은 범용 맵으로 데이터를 주고받지 않는다.
- 구조가 정해진 데이터는 **전용 DTO 클래스**로 표현한다.
- DTO를 쓰면 필드명·타입이 컴파일 시점에 보장되고, IDE 자동완성과 리팩터링이 안전해진다.

```java
// ❌ 지양
Map<String, Object> user = new HashMap<>();
user.put("name", "홍길동");
user.put("age", 30);

// ✅ 권장
public record UserDto(String name, int age) {}
UserDto user = new UserDto("홍길동", 30);
```

> HashMap이 정말 적합한 경우(동적 키, 캐시 등)에만 제한적으로 사용한다.

## 2. 중복 코드 최소화

- 같은 로직이 2번 이상 반복되면 메서드/클래스로 추출한다.
- 공통 처리(검증, 변환, 예외 처리)는 유틸 또는 공통 컴포넌트로 모은다.
- 복사-붙여넣기 대신 재사용을 우선한다.

```java
// ❌ 지양 — 같은 변환 로직이 여러 곳에 반복
// ✅ 권장 — 한 곳에 모아 호출
private String formatPhone(String raw) {
    return raw.replaceAll("[^0-9]", "");
}
```

## 3. 직관적이고 쉽고 간단한 코드

- 짧고 명확한 이름을 쓴다 (변수·메서드·클래스).
- 한 메서드는 한 가지 일만 한다.
- 과도한 추상화·불필요한 패턴보다 **읽기 쉬운 코드**를 우선한다.
- 주석보다 코드 자체가 의도를 드러내도록 작성한다.

```java
// ❌ 지양
if (u != null && u.getS() == 1 && u.getT().after(new Date())) { ... }

// ✅ 권장
if (user.isActive()) { ... }
```

## 4. Lombok은 기본적으로 Getter / Setter 사용

- 기본은 `@Getter` / `@Setter`를 사용한다.
- `@Builder`는 **정말 필요할 때 최후의 수단**으로만 쓴다.
- 빌더를 남발하지 말고, 단순한 객체 생성은 Getter/Setter나 생성자로 처리한다.

```java
// ✅ 권장 — 기본은 Getter/Setter
@Getter
@Setter
public class UserDto {
    private String name;
    private int age;
}

// ⚠️ 최후의 수단 — 꼭 필요할 때만 Builder
@Getter
@Builder
public class ComplexDto {
    private String a;
    private String b;
    private String c;
}
```

---

이 규칙은 새 코드 작성과 기존 코드 수정 모두에 적용된다.
