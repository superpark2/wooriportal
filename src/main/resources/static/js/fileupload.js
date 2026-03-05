// ──────────────────────────────────────────────
// 설정
// ──────────────────────────────────────────────
const MAX_FILES = 20;

// 상태 관리: { name: string, path: string, isNew: boolean, fileObj?: File }
let files = [];

// ──────────────────────────────────────────────
// 유틸
// ──────────────────────────────────────────────
function formatSize(bytes) {
    if (bytes === undefined || bytes === null) return '';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function getExt(name) {
    const parts = name.split('.');
    return parts.length > 1 ? parts.pop().toLowerCase() : '';
}

function getFileIcon(name) {
    const ext = getExt(name);
    const imgExts = ['jpg','jpeg','png','gif','webp','svg','bmp','ico','heic'];
    if (imgExts.includes(ext)) return { type: 'img', cls: 'icon-img' };
    // ... (다른 아이콘 타입들)
    return { type: 'other', cls: 'icon-other' };
}

function iconSVG(type, cls) {
    const svgs = {
        img: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" class="${cls}"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>`,
        other:`<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" class="${cls}"><path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/><polyline points="13 2 13 9 20 9"/></svg>`,
    };
    return svgs[type] || svgs.other;
}

// ──────────────────────────────────────────────
// 초기화 및 외부 인터페이스
// ──────────────────────────────────────────────
function initFileUpload(initial) {
    files = [];

    // 기존 데이터: | 로 구분된 문자열 또는 문자열 배열을 허용
    if (typeof initial === 'string' && initial.trim() !== '') {
        initial.split('|').forEach(path => {
            const trimmed = path.trim();
            if (!trimmed) return;
            const name = trimmed.substring(trimmed.lastIndexOf('/') + 1);
            files.push({ name, path: trimmed, isNew: false });
        });
    } else if (Array.isArray(initial)) {
        // 나중에 필요 시 확장: 이미 {name, path} 형태의 배열을 받을 수도 있음
        initial.forEach(item => {
            if (!item) return;
            if (typeof item === 'string') {
                const name = item.substring(item.lastIndexOf('/') + 1);
                files.push({ name, path: item, isNew: false });
            } else if (item.path) {
                const name = item.name || item.path.substring(item.path.lastIndexOf('/') + 1);
                files.push({ name, path: item.path, isNew: false });
            }
        });
    }

    render();
}

// 새 파일 / 기존 파일 경로를 폼 전송용으로 꺼내는 함수들
function getNewFiles() {
    return files.filter(f => f.isNew && f.fileObj).map(f => f.fileObj);
}

function getExistingFilePaths() {
    return files.filter(f => !f.isNew && f.path).map(f => f.path);
}

// ──────────────────────────────────────────────
// 드롭존 이벤트
// ──────────────────────────────────────────────
const dropzone = document.getElementById('dropzone');
const fileInput = document.getElementById('fileInput');
const addMoreBtn = document.getElementById('addMoreBtn');

if (dropzone && fileInput && addMoreBtn) {
    dropzone.addEventListener('click', () => fileInput.click());
    addMoreBtn.addEventListener('click', () => fileInput.click());
    dropzone.addEventListener('dragover', e => { e.preventDefault(); if (files.length < MAX_FILES) dropzone.classList.add('drag-over'); });
    dropzone.addEventListener('dragleave', () => dropzone.classList.remove('drag-over'));
    dropzone.addEventListener('drop', e => { e.preventDefault(); dropzone.classList.remove('drag-over'); addFiles(Array.from(e.dataTransfer.files)); });
    fileInput.addEventListener('change', () => { addFiles(Array.from(fileInput.files)); fileInput.value = ''; });
}

// ──────────────────────────────────────────────
// 파일 추가 / 제거
// ──────────────────────────────────────────────
function addFiles(filesToAdd) {
    const remaining = MAX_FILES - files.length;
    if (remaining <= 0) return alert('최대 20개까지만 첨부할 수 있습니다.');

    const toAdd = filesToAdd.slice(0, remaining);
    toAdd.forEach(file => {
        files.push({
            name: file.name,
            path: '', // 새 파일은 아직 경로가 없음
            isNew: true,
            fileObj: file
        });
    });
    render();
}

function removeFile(index) {
    const f = files[index];
    const name = f && f.name ? f.name : '이 파일';
    if (!confirm(`"${name}" 파일을 첨부 목록에서 삭제하시겠습니까?`)) return;
    files.splice(index, 1);
    render();
}

function clearAll() {
    files = [];
    render();
}

// ──────────────────────────────────────────────
// 렌더링
// ──────────────────────────────────────────────
function render() {
    const list = document.getElementById('fileList');
    const badge = document.getElementById('countBadge');
    if (!list || !badge) return;

    const totalCount = files.length;
    const hasFiles = totalCount > 0;
    const atMax = totalCount >= MAX_FILES;

    if (dropzone) dropzone.classList.toggle('hidden', hasFiles);
    if (addMoreBtn) {
        addMoreBtn.classList.toggle('visible', hasFiles);
        addMoreBtn.classList.toggle('maxed', atMax);
    }

    badge.textContent = `${totalCount} / ${MAX_FILES}`;
    badge.className = 'count-badge' + (atMax ? ' at-max' : '');

    if (totalCount === 0) {
        list.innerHTML = '<div class="empty-list" id="emptyMsg">첨부된 파일이 없습니다.</div>';
    } else {
        list.innerHTML = files.map((f, i) => {
            const { type } = getFileIcon(f.name);
            const isImage = type === 'img';
            let thumbHTML;

            if (f.isNew) {
                const objectURL = URL.createObjectURL(f.fileObj);
                thumbHTML = isImage ? `<div class="file-thumb"><img src="${objectURL}" alt=""></div>` : `<div class="file-thumb">${iconSVG(type, 'icon-file')}</div>`;
            } else {
                thumbHTML = isImage ? `<div class="file-thumb"><img src="${f.path}" alt=""></div>` : `<div class="file-thumb">${iconSVG(type, 'icon-file')}</div>`;
            }

            return `
            <div class="file-item ${f.isNew ? 'new' : 'existing'}">
              ${thumbHTML}
              <div class="file-info">
                <div class="file-name">${f.name} ${f.isNew ? '<span class="new-badge">NEW</span>' : ''}</div>
                <div class="file-meta">${f.isNew && f.fileObj ? formatSize(f.fileObj.size) : ''}</div>
              </div>
              <button class="file-remove" onclick="removeFile(${i})" type="button" title="제거">
                <svg viewBox="0 0 24 24"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>`;
        }).join('');
    }
}
