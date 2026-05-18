// ── simileland 스킨 효과 ───────────────────────────────────────────────────────
// 의존: 전역 변수 skin (layout.html에서 th:inline으로 선언)

(function () {
    if (typeof skin === 'undefined' || skin !== 'simileland') return;

    const colors = [
        '#d42b1e', '#e8c020', '#2a60c0', '#1e9e8a',
        '#e8648a', '#c8860a', '#7b3a9b', '#1e5c30', '#d85010',
    ];

    function shuffle(arr) {
        const a = [...arr];
        for (let i = a.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [a[i], a[j]] = [a[j], a[i]];
        }
        return a;
    }

    // ── 상위 메뉴 글자 컬러 블록 효과 ────────────────────────────────────────
    document.querySelectorAll('#sidebar .sidebar-menu:not(.submenu) > li > a').forEach(a => {
        const icon    = a.querySelector('i');
        const iconHtml = icon ? icon.outerHTML : '';
        const text    = a.innerText.trim();
        const shuffled = shuffle(colors);

        let html = iconHtml ? iconHtml + ' ' : '';
        [...text].forEach((ch, i) => {
            if (ch === ' ') { html += ' '; return; }
            const bg      = shuffled[i % shuffled.length];
            const rot     = (Math.random() * 8 - 4).toFixed(1);
            const isLight = bg === '#e8c020';
            html += `<span style="display:inline-block;background:${bg};color:${isLight ? '#111' : '#fff'};`
                + `padding:1px 3px;border:1.5px solid #111;border-radius:1px;`
                + `transform:rotate(${rot}deg);line-height:1.2;margin:0 0.5px;">${ch}</span>`;
        });
        a.innerHTML = html;
    });

    // ── 로고 이미지 치환 ──────────────────────────────────────────────────────
    const logo = document.querySelector('.logo');
    if (!logo) return;

    const fallbackHtml = `
        <span style="display:block;background:#d42b1e;color:#fff;font-family:'Black Han Sans',sans-serif;
            font-size:11px;font-weight:900;padding:2px 6px;border:2px solid #111;
            box-shadow:2px 2px 0 #111;letter-spacing:0.1em;transform:rotate(-2.5deg);line-height:1.4;">SIMILE</span>
        <span style="display:block;background:#e8c020;color:#111;font-family:'Black Han Sans',sans-serif;
            font-size:11px;font-weight:900;padding:2px 6px;border:2px solid #111;
            box-shadow:2px 2px 0 #111;letter-spacing:0.2em;transform:rotate(2deg);line-height:1.4;">LAND</span>
    `;

    const img   = document.createElement('img');
    img.src     = '/bg/simileland.webp';
    img.alt     = 'SIMILE LAND';
    img.style.cssText = 'height:150%;width:100%;display:block;flex-shrink:0;';

    img.onload = function () {
        logo.style.cssText += 'display:flex!important;align-items:center!important;'
            + 'justify-content:flex-start!important;padding:0!important;overflow:visible!important;text-indent:0!important;';
    };
    img.onerror = function () {
        logo.style.cssText += 'text-indent:0!important;display:flex!important;flex-direction:column!important;'
            + 'align-items:center!important;justify-content:center!important;gap:3px!important;';
        logo.innerHTML = fallbackHtml;
    };

    logo.innerHTML = '';
    logo.appendChild(img);
})();