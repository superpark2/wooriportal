// ── 말티즈 스킨 이미지 효과 ───────────────────────────────────────────────────
// 의존: 전역 변수 skin (layout.html에서 th:inline으로 선언)

(function () {
    if (typeof skin === 'undefined' || skin !== 'maltese') return;

    const imagePaths = Array.from({ length: 30 }, (_, i) => `/style/skin/maltese/image/${i + 1}.png`);

    function shuffle(a) {
        for (let i = a.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [a[i], a[j]] = [a[j], a[i]];
        }
        return a;
    }

    const count  = Math.floor(Math.random() * 8) + 5;
    const placed = [];

    shuffle([...imagePaths]).slice(0, count).forEach(src => {
        let tries = 0;
        while (tries++ < 100) {
            const x = Math.floor(Math.random() * (window.innerWidth  - 200));
            const y = Math.floor(Math.random() * (window.innerHeight - 200));
            if (!placed.some(r => Math.abs(r.x - x) < 300 && Math.abs(r.y - y) < 300)) {
                const img = document.createElement('img');
                Object.assign(img.style, {
                    position:      'fixed',
                    width:         '200px',
                    height:        'auto',
                    left:          x + 'px',
                    top:           y + 'px',
                    opacity:       '0.4',
                    zIndex:        '0',
                    pointerEvents: 'none',
                });
                img.src = src;
                document.body.appendChild(img);
                placed.push({ x, y });
                break;
            }
        }
    });
})();