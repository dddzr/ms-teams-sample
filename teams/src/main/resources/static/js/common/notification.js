/**
 * 알림 모듈 (Teams Dialog API / Browser Alert)
 * 
 * Teams 환경에서는 Dialog API를 사용하고,
 * 일반 브라우저에서는 alert를 사용합니다.
 * 
 * 사용법:
 *   showNotification('메시지', 'success'); // 성공 알림
 *   showNotification('메시지', 'error');   // 에러 알림
 *   showNotification('메시지', 'info');     // 정보 알림
 * 
 * 주의: 이 모듈은 teams-init.js의 isTeams(), isTeamsReady() 함수를 사용합니다.
 */

// 재시도 카운터 (무한 루프 방지)
let notificationRetryCount = 0;
const MAX_NOTIFICATION_RETRIES = 50; // 최대 5초 대기 (50 * 100ms)

/**
 * 알림 표시 (Teams Dialog API 또는 Browser Alert)
 * @param {string} message - 표시할 메시지
 * @param {string} type - 알림 타입 ('success', 'error', 'info')
 * @param {string} title - 알림 제목 (기본값: 타입에 따라 자동 설정)
 */
function showNotification(message, type = 'info', title = null) {
    // Teams 초기화 대기 중이면 잠시 후 재시도 (타임아웃 있음)
    if (typeof isTeamsReady !== 'function' || !isTeamsReady()) {
        if (notificationRetryCount < MAX_NOTIFICATION_RETRIES) {
            notificationRetryCount++;
            setTimeout(() => showNotification(message, type, title), 100);
            return;
        } else {
            console.warn('Teams 초기화 대기 시간 초과. 일반 브라우저 모드로 진행합니다.');
            notificationRetryCount = 0; // 리셋
        }
    } else {
        notificationRetryCount = 0; // 초기화 완료 시 리셋
    }
    
    // 제목 자동 설정
    if (!title) {
        switch (type) {
            case 'success':
                title = '성공';
                break;
            case 'error':
                title = '오류';
                break;
            default:
                title = '알림';
        }
    }
    
    if (typeof isTeams === 'function' && isTeams()) {
        // Teams 환경: Dialog API 사용
        // showTeamsDialog(message, type, title);
    } else {
        // 일반 브라우저: alert 사용
        showBrowserAlert(message, type, title);
    }
}

/**
 * Teams Dialog API로 알림 표시
 */
function showTeamsDialog(message, type, title) {
    try {
        const baseUrl = window.location.origin;
        const dialogUrl = `${baseUrl}/dialog/alert?type=${encodeURIComponent(type)}&title=${encodeURIComponent(title)}&message=${encodeURIComponent(message)}`;
        
        // Dialog 열기
        microsoftTeams.dialog.open({
            title: title,
            url: dialogUrl,
            size: {
                height: 300,
                width: 500
            }
        }, (result) => {
            // Dialog가 닫힌 후 콜백
            if (result && result.result) {
                console.log('Dialog closed with result:', result.result);
            } else {
                console.log('Dialog closed');
            }
        });
    } catch (error) {
        console.error('Teams Dialog 오픈 실패:', error);
        // 실패 시 일반 alert로 fallback
        showBrowserAlert(message, type, title);
    }
}

/**
 * 일반 브라우저 alert로 알림 표시
 */
function showBrowserAlert(message, type, title) {
    let icon = '';
    switch (type) {
        case 'success':
            icon = '✅';
            break;
        case 'error':
            icon = '❌';
            break;
        default:
            icon = 'ℹ️';
    }
    
    alert(`${icon} ${title}\n\n${message}`);
}

/**
 * 성공 알림 (편의 함수)
 */
function showSuccess(message, title = '성공') {
    showNotification(message, 'success', title);
}

/**
 * 에러 알림 (편의 함수)
 */
function showError(message, title = '오류') {
    showNotification(message, 'error', title);
}

/**
 * 정보 알림 (편의 함수)
 */
function showInfo(message, title = '알림') {
    showNotification(message, 'info', title);
}

