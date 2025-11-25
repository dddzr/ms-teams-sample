/**
 * Microsoft Teams 초기화 모듈
 * 
 * Teams SDK 초기화 및 환경 감지를 담당합니다.
 * notification.js에 의존하지 않도록 콘솔 로그만 사용
 * callback 통해 상위 컴포넌트에 알림, 상위 컴포넌트에서 notification 함수 호출로 알림 처리.
 * 
 * 사용법:
 *   // 자동 초기화 (기본) - JS 로드 시 자동 실행
 *   <script src="/js/common/teams-init.js"></script>
 *   
 *   // SSO 자동 로그인 시도 (초기화 후 별도 호출)
 *   if (isTeamsReady()) {
 *       tryTeamsSSO((result) => { ... });
 *   }
 */

// 내부 변수 (외부에서는 isTeams(), isTeamsReady() 함수로 접근)
let isTeamsContext = false;
let teamsInitialized = false;

/**
 * Teams 환경 초기화
 * @returns {Promise<boolean>} Teams 환경 여부
 */
function initTeams() {
    // 이미 초기화 완료되었으면 즉시 반환
    if (teamsInitialized) {
        return Promise.resolve(isTeamsContext);
    }
    
    // SDK가 로드되지 않았으면 일반 브라우저로 처리
    // SDK는 index.html에서 직접 로드됨
    if (typeof microsoftTeams === 'undefined') {
        isTeamsContext = false;
        teamsInitialized = true;
        console.log('Teams 환경이 아닙니다. 일반 브라우저에서 실행 중입니다.');
        return Promise.resolve(false);
    }
    
    return new Promise((resolve) => {
        // Teams SDK 초기화 시작
        if (typeof showLoading === 'function') {
            showLoading('Teams 초기화 중...');
        }
        
        microsoftTeams.app.initialize()
            .then(() => {
                isTeamsContext = true;
                teamsInitialized = true;
                
                if (typeof hideLoading === 'function') {
                    hideLoading();
                }
                
                resolve(true);
            })
            .catch((error) => {
                isTeamsContext = false;
                teamsInitialized = true;
                
                if (typeof hideLoading === 'function') {
                    hideLoading();
                }
                
                console.error('Teams 초기화 실패: ' + (error.message || '알 수 없는 오류'));
                
                resolve(false);
            });
    });
}

/**
 * Teams SSO 자동 로그인 시도
 * @param {Function} callback - SSO 성공 시 콜백 함수
 */
async function tryTeamsSSO(callback = null) {
    if (!isTeams()) {
        console.error('Teams 환경이 아니거나 초기화되지 않았습니다. SSO를 시도할 수 없습니다.');
        return;
    }
    
    try {
        
        // 1. Teams에 로그인된 사용자의 토큰(SSO Token) 가져오기
        // SSO를 위해 앱의 App ID URI를 사용 (manifest.json의 webApplicationInfo.resource와 일치해야 함)
        let ssoToken = null;
        try {
            if (typeof showLoading === 'function') {
                showLoading('SSO 토큰 요청 중...');
            }
            
            const SSO_TIMEOUT_MS = 5000; // 5초
            // Promise 기반으로 토큰 요청 (타임아웃 포함)
            const timeoutPromise = new Promise((_, reject) => {
                setTimeout(() => {
                    if (typeof showLoading === 'function') {
                        showLoading('SSO 토큰 요청 시간 초과');
                    }
                    reject(new Error('SSO 토큰 요청 시간 초과 (5초)'));
                }, SSO_TIMEOUT_MS);
            });
            
            // getAuthToken 호출 (Promise 기반)
            const authPromise = microsoftTeams.authentication.getAuthToken({
                // 1. (X) resources: ['api://{domain}/{client-id}'],
                // 2. (X) resources: ['api://auth-<tenant-id>/<client-id>'] //도메인 검증 없이 사용 가능한 형식
                // 3. (O) resources: ['https://{domain}:{port}/{client-id}'], //https로 해야함.
                // 4. (O) resources 파라미터 생략 - manifest.json의 webApplicationInfo.resource를 기본으로 사용
                // 참고: resources: [] (빈 배열)은 리소스를 요청하지 않는 것으로 해석되어 실패할 수 있음
                // silent: true,
            });
            
            ssoToken = await Promise.race([authPromise, timeoutPromise]);
            
        } catch (error) {
            // 에러 메시지 추출 (더 상세하게)
            let errorMsg = '알 수 없는 오류';
            let errorCode = 'N/A';
            
            if (error) {
                if (typeof error === 'string') {
                    errorMsg = error;
                } else if (error.message) {
                    errorMsg = error.message;
                } else if (error.error) {
                    errorMsg = error.error;
                } else {
                    errorMsg = JSON.stringify(error);
                }
                
                if (error.errorCode) {
                    errorCode = error.errorCode;
                } else if (error.code) {
                    errorCode = error.code;
                }
            }
            
            // 로딩 숨김
            if (typeof hideLoading === 'function') {
                hideLoading();
            }

            console.warn('SSO 토큰 요청 실패(' + errorCode + '): ' + errorMsg);
            
            // 콜백 호출 (실패 상태 전달)
            if (callback) {
                callback({ 
                    success: false, 
                    error: errorMsg,
                    errorCode: errorCode,
                    requiresUserAction: true // 사용자 개입 필요
                });
            }
            
            // return으로 함수 종료 (throw하지 않음 → iframe 정상 유지)
            return;
        }

        // 2. 서버로 SSO 토큰 전송하여 OBO 방식으로 Graph API 토큰 교환 및 로그인 연동
        // OBO (On-Behalf-Of) 방식: 클라이언트는 SSO 토큰만 전송하고, 서버에서 Graph API 토큰으로 교환
        // 이렇게 하면 Graph API 토큰이 클라이언트에 노출되지 않아 보안이 강화됩니다.
        if (typeof showLoading === 'function') {
            showLoading('SSO 로그인 중...');
        }
        const response = await fetch('/auth/teams/sso', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ 
                ssoToken: ssoToken
            })
        });
        
        if (response.ok) {
            const result = await response.json();
            
            // 로그인 상태 확인 (checkLoginStatus가 있으면 사용)
            if (typeof checkLoginStatus === 'function') {
                await checkLoginStatus();
            }
            
            // 콜백 호출
            if (callback) {
                callback({ success: true, result });
            }
            
            // 페이지 새로고침 (서버에서 성공 메시지 표시)
            setTimeout(() => {
                window.location.reload();
            }, 300);
        } else {
            let errorData = null;
            let errorMessage = '알 수 없는 오류';
            let errorDetails = '';
            
            try {
                errorData = await response.json();
                errorMessage = errorData.error || errorData.message || '알 수 없는 오류';
                
                // 상세 정보 수집
                const details = [];
                if (errorData.error) details.push(`오류: ${errorData.error}`);
                if (errorData.message) details.push(`메시지: ${errorData.message}`);
                if (response.status) details.push(`HTTP 상태 코드: ${response.status}`);
                if (errorData.errorCode) details.push(`에러 코드: ${errorData.errorCode}`);
                
                if (details.length > 0) {
                    errorDetails = '\n\n오류 상세:\n' + details.join('\n');
                }
            } catch (parseError) {
                // JSON 파싱 실패 시 텍스트로 읽기 시도
                try {
                    const text = await response.text();
                    errorMessage = text || `HTTP ${response.status} 오류`;
                    errorDetails = `\n\nHTTP 상태 코드: ${response.status}`;
                } catch (textError) {
                    errorMessage = `HTTP ${response.status} 오류`;
                    errorDetails = `\n\n응답을 읽을 수 없습니다.`;
                }
            }
            
            if (typeof hideLoading === 'function') {
                hideLoading();
            }
            
            console.error(`SSO 로그인 실패: ${errorMessage}${errorDetails}`);
            
            // 콜백 호출 (상세한 에러 정보 전달)
            if (callback) {
                callback({ 
                    success: false, 
                    error: errorMessage || (errorData ? (errorData.error || errorData.message) : '알 수 없는 오류'),
                    errorCode: errorData?.errorCode || response.status,
                    errorDetails: errorDetails
                });
            }
        }
    } catch (error) {
        // SSO 실패 처리
        if (typeof hideLoading === 'function') {
            hideLoading();
        }
        
        // 에러 메시지 표시
        let errorMessage = 'SSO 시도 중 오류가 발생했습니다.';
        let errorTitle = 'SSO 오류';
        
        // 에러 상세 정보 수집 (콘솔 대신 다이얼로그에 표시)
        const errorDetails = {
            message: error.message || '알 수 없는 오류',
            errorCode: error.errorCode || 'N/A',
            errorType: error.name || 'Error'
        };
        
        if (error.message) {
            const errorMsg = error.message.toLowerCase();
            if (errorMsg.includes('usercancel') || errorMsg.includes('user_cancel')) {
                errorMessage = '사용자가 SSO 인증을 취소했습니다.';
                errorTitle = 'SSO 취소';
            } else if (errorMsg.includes('invalidresource') || errorMsg.includes('invalid_resource')) {
                errorMessage = `SSO 리소스가 올바르지 않습니다.\n\nmanifest.json의 webApplicationInfo.resource를 확인하세요.\n\n오류 상세:\n- 메시지: ${errorDetails.message}\n- 에러 코드: ${errorDetails.errorCode}\n- 에러 타입: ${errorDetails.errorType}`;
                errorTitle = 'SSO 설정 오류';
            } else if (errorMsg.includes('internalservererror') || errorMsg.includes('internal_server_error')) {
                errorMessage = `Teams 서버 오류가 발생했습니다.\n\n잠시 후 다시 시도해주세요.\n\n오류 상세:\n- 메시지: ${errorDetails.message}\n- 에러 코드: ${errorDetails.errorCode}\n- 에러 타입: ${errorDetails.errorType}`;
                errorTitle = '서버 오류';
            } else if (errorMsg.includes('consentrequired') || errorMsg.includes('consent_required')) {
                errorMessage = `앱 사용 권한이 필요합니다.\n\n관리자에게 앱 권한 승인을 요청하세요.\n\n오류 상세:\n- 메시지: ${errorDetails.message}\n- 에러 코드: ${errorDetails.errorCode}\n- 에러 타입: ${errorDetails.errorType}`;
                errorTitle = '권한 필요';
            } else if (errorMsg.includes('invalidgrant') || errorMsg.includes('invalid_grant')) {
                errorMessage = `인증이 만료되었습니다.\n\n다시 로그인해주세요.\n\n오류 상세:\n- 메시지: ${errorDetails.message}\n- 에러 코드: ${errorDetails.errorCode}\n- 에러 타입: ${errorDetails.errorType}`;
                errorTitle = '인증 만료';
            } else {
                // 상세한 에러 메시지 표시
                errorMessage = `SSO 오류가 발생했습니다.\n\n오류 상세:\n- 메시지: ${errorDetails.message}\n- 에러 코드: ${errorDetails.errorCode}\n- 에러 타입: ${errorDetails.errorType}`;
            }
        } else if (error.errorCode) {
            errorMessage = `SSO 오류 코드: ${error.errorCode}\n\n오류 상세:\n- 에러 코드: ${errorDetails.errorCode}\n- 에러 타입: ${errorDetails.errorType}`;
        } else {
            // 에러 객체 전체 정보 표시
            errorMessage = `SSO 오류가 발생했습니다.\n\n오류 상세:\n- 메시지: ${errorDetails.message}\n- 에러 코드: ${errorDetails.errorCode}\n- 에러 타입: ${errorDetails.errorType}`;
        }
        
        console.error(`SSO 오류 (${errorTitle}): ${errorMessage}`);
        
        // 콜백 호출 (상세한 에러 정보 전달)
        if (callback) {
            let callbackError = errorMessage;
            if (!callbackError || callbackError === 'SSO 시도 중 오류가 발생했습니다.') {
                // 더 구체적인 에러 메시지 사용
                if (error && error.message) {
                    callbackError = error.message;
                } else if (error && typeof error === 'string') {
                    callbackError = error;
                } else if (error) {
                    callbackError = JSON.stringify(error);
                } else {
                    callbackError = '알 수 없는 오류가 발생했습니다.';
                }
            }
            
            callback({ 
                success: false, 
                error: callbackError,
                errorCode: error?.errorCode || errorDetails.errorCode,
                errorTitle: errorTitle
            });
        }
    }
}

/**
 * Teams 환경인지 확인
 * @returns {boolean} Teams 환경 여부
 */
function isTeams() {
    return isTeamsContext === true && teamsInitialized === true;
}

/**
 * Teams 초기화 완료 여부 확인
 * @returns {boolean} 초기화 완료 여부
 */
function isTeamsReady() {
    return teamsInitialized === true;
}

// 자동 초기화 - JS 로드 시 자동으로 initTeams 실행
// async 함수이므로 await 없이 호출해도 Promise가 반환됨
(function autoInit() {
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            initTeams().catch(err => {
                console.error('Teams 자동 초기화 실패:', err);
            });
        });
    } else {
        initTeams().catch(err => {
            console.error('Teams 자동 초기화 실패:', err);
        });
    }
})();

// 전역 함수로 노출 (다른 스크립트에서 사용 가능)
window.initTeams = initTeams;
window.tryTeamsSSO = tryTeamsSSO;
window.isTeams = isTeams;
window.isTeamsReady = isTeamsReady;

