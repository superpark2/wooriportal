/**
 * CSRF 전역 처리 — Spring Security의 CookieCsrfTokenRepository(withHttpOnlyFalse)와 연동.
 *
 * 서버가 내려준 XSRF-TOKEN 쿠키 값을 읽어, 상태 변경(POST/PUT/DELETE/PATCH) 요청에
 * 자동으로 X-XSRF-TOKEN 헤더를 첨부한다. 모든 페이지의 <head>에서 가장 먼저 로드할 것.
 */
(function () {
    function getCookie(name) {
        const m = document.cookie.match('(^|;)\\s*' + name + '\\s*=\\s*([^;]+)');
        return m ? decodeURIComponent(m.pop()) : '';
    }

    const SAFE = /^(GET|HEAD|OPTIONS|TRACE)$/i;
    const origFetch = window.fetch;

    window.fetch = function (input, init) {
        init = init || {};
        const method = (init.method || (typeof input !== 'string' && input && input.method) || 'GET').toUpperCase();

        if (!SAFE.test(method)) {
            const token = getCookie('XSRF-TOKEN');
            if (token) {
                const headers = new Headers(init.headers || (typeof input !== 'string' && input && input.headers) || {});
                if (!headers.has('X-XSRF-TOKEN')) headers.set('X-XSRF-TOKEN', token);
                init.headers = headers;
            }
        }
        return origFetch.call(this, input, init);
    };

    // XMLHttpRequest 대응: jQuery $.ajax, jstree 등 fetch를 쓰지 않는 라이브러리가
    // 보내는 POST/PUT/DELETE에도 X-XSRF-TOKEN 헤더를 자동 첨부한다.
    const origOpen = XMLHttpRequest.prototype.open;
    const origSend = XMLHttpRequest.prototype.send;

    XMLHttpRequest.prototype.open = function (method) {
        this._csrfMethod = method;
        return origOpen.apply(this, arguments);
    };

    XMLHttpRequest.prototype.send = function () {
        if (!SAFE.test(this._csrfMethod || 'GET')) {
            const token = getCookie('XSRF-TOKEN');
            if (token) {
                try { this.setRequestHeader('X-XSRF-TOKEN', token); } catch (e) { /* 이미 전송됨 */ }
            }
        }
        return origSend.apply(this, arguments);
    };

    // 폼 전송(application/x-www-form-urlencoded, multipart) 대비: 숨김 필드 자동 주입
    document.addEventListener('submit', function (e) {
        const form = e.target;
        if (!form || form.method && SAFE.test(form.method)) return;
        const token = getCookie('XSRF-TOKEN');
        if (!token) return;
        if (form.querySelector('input[name="_csrf"]')) return;
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = '_csrf';
        input.value = token;
        form.appendChild(input);
    }, true);
})();
