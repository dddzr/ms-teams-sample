/**
 * 로딩 오버레이 모듈
 * 
 * 사용법:
 * 1. HTML에 로딩 오버레이 요소 추가:
 *    <div id="loadingOverlay" class="loading-overlay">
 *        <div class="loading-spinner"></div>
 *        <div class="loading-message" id="loadingMessage">처리 중...</div>
 *    </div>
 * 
 * 2. CSS 파일 로드 (loading.css 또는 인라인 스타일)
 * 
 * 3. JavaScript에서 사용:
 *    showLoading('로딩 중...');
 *    hideLoading();
 */

/**
 * 로딩 오버레이 표시
 * @param {string} message - 표시할 메시지 (기본값: '처리 중...')
 */
function showLoading(message = '처리 중...') {
    const overlay = document.getElementById('loadingOverlay');
    const messageEl = document.getElementById('loadingMessage');
    
    if (!overlay) {
        console.warn('로딩 오버레이 요소를 찾을 수 없습니다. id="loadingOverlay" 요소가 필요합니다.');
        return;
    }
    
    if (messageEl) {
        messageEl.textContent = message;
    }
    
    overlay.classList.add('show');
}

/**
 * 로딩 오버레이 숨김
 */
function hideLoading() {
    const overlay = document.getElementById('loadingOverlay');
    
    if (!overlay) {
        console.warn('로딩 오버레이 요소를 찾을 수 없습니다.');
        return;
    }
    
    overlay.classList.remove('show');
}

/**
 * 로딩 오버레이 초기화
 * 페이지에 로딩 오버레이 요소가 없으면 자동으로 생성
 */
function initLoadingOverlay() {
    let overlay = document.getElementById('loadingOverlay');
    
    if (!overlay) {
        // 로딩 오버레이 요소 생성
        overlay = document.createElement('div');
        overlay.id = 'loadingOverlay';
        overlay.className = 'loading-overlay';
        
        const spinner = document.createElement('div');
        spinner.className = 'loading-spinner';
        
        const message = document.createElement('div');
        message.id = 'loadingMessage';
        message.className = 'loading-message';
        message.textContent = '처리 중...';
        
        overlay.appendChild(spinner);
        overlay.appendChild(message);
        document.body.appendChild(overlay);
    }
}

// DOMContentLoaded 시 자동 초기화
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initLoadingOverlay);
} else {
    initLoadingOverlay();
}

