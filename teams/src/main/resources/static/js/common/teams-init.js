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
let sdkLoading = false;
let sdkLoadPromise = null;

/**
 * Teams SDK 동적 로드
 * @returns {Promise<void>} SDK 로드 완료 Promise
 */
function loadTeamsSDK() {
    // 이미 로드되어 있으면 즉시 반환
    if (typeof microsoftTeams !== 'undefined') {
        return Promise.resolve();
    }
    
    // 이미 로딩 중이면 기존 Promise 반환
    if (sdkLoading && sdkLoadPromise) {
        return sdkLoadPromise;
    }
    
    // SDK 로드 시작
    sdkLoading = true;
    sdkLoadPromise = new Promise((resolve, reject) => {
        const script = document.createElement('script');
        script.src = 'https://res.cdn.office.net/teams-js/2.0.0/js/MicrosoftTeams.min.js';
        script.crossOrigin = 'anonymous';
        script.onload = () => {
            sdkLoading = false;
            resolve();
        };
        script.onerror = () => {
            sdkLoading = false;
            sdkLoadPromise = null;
            reject(new Error('Teams SDK 로드 실패'));
        };
        document.head.appendChild(script);
    });
    
    return sdkLoadPromise;
}

/**
 * Teams 환경 초기화
 * @returns {Promise<boolean>} Teams 환경 여부
 */
async function initTeams() {
    // 이미 초기화 완료되었으면 즉시 반환
    if (teamsInitialized) {
        return Promise.resolve(isTeamsContext);
    }
    
    try {
        // Teams SDK 동적 로드
        await loadTeamsSDK();
    } catch (error) {
        // SDK 로드 실패 (일반 브라우저일 수 있음)
        isTeamsContext = false;
        teamsInitialized = true;
        console.log('Teams SDK를 로드할 수 없습니다. 일반 브라우저에서 실행 중입니다.');
        return false;
    }
    
    // SDK가 로드되었지만 여전히 undefined인 경우 (일반 브라우저)
    if (typeof microsoftTeams === 'undefined') {
        isTeamsContext = false;
        teamsInitialized = true;
        console.log('Teams 환경이 아닙니다. 일반 브라우저에서 실행 중입니다.');
        return false;
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
        // 도메인 검증 없이 사용: api://<client-id> 형식
        let ssoToken = null;
        try {
            if (typeof showLoading === 'function') {
                showLoading('SSO 토큰 요청 중...');
            }
            
            const SSO_TIMEOUT_MS = 5000; // 3초
            // v2 SDK는 콜백 기반이므로 Promise로 래핑
            ssoToken = await new Promise((resolve, reject) => {
                let timeoutTriggered = false;
                let callbackInvoked = false;
                
                // Timeout 설정
                const timeoutId = setTimeout(() => {
                    if (!callbackInvoked) {
                        timeoutTriggered = true;
                        callbackInvoked = true;
                        // 타임아웃 발생 시 로딩 메시지 업데이트
                        if (typeof showLoading === 'function') {
                            showLoading('SSO 토큰 요청 시간 초과');
                        }
                        reject(new Error('SSO 토큰 요청 시간 초과 (5초)'));
                    }
                }, SSO_TIMEOUT_MS);
                
                // getAuthToken 호출 (콜백 기반)
                try {
                    microsoftTeams.authentication.getAuthToken({
                        // resources: ['api://nwnote.saerom.co.kr/56e05b4e-9682-4e5f-8866-5ba5d76e1cbf'],
                        // 도메인 검증 없이 사용 가능한 형식: api://auth-<tenant-id>/<client-id>
                        resources: ['api://auth-844894c4-de5c-4041-9eed-f9fa243a7d17/56e05b4e-9682-4e5f-8866-5ba5d76e1cbf'],
                        silent: true,
                        successCallback: (token) => {
                            if (!callbackInvoked && !timeoutTriggered) {
                                callbackInvoked = true;
                                clearTimeout(timeoutId);
                                resolve(token);
                            }
                        },
                        failureCallback: (error) => {
                            if (!callbackInvoked && !timeoutTriggered) {
                                callbackInvoked = true;
                                clearTimeout(timeoutId);
                                reject(error);
                            }
                        }
                    });
                } catch (sdkError) {
                    // getAuthToken 호출 자체가 실패한 경우
                    if (!callbackInvoked && !timeoutTriggered) {
                        callbackInvoked = true;
                        clearTimeout(timeoutId);
                        reject(sdkError);
                    }
                }
            });
            
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

        // 2. Graph API 토큰 요청
        let graphToken = null;
        try {
            if (typeof showLoading === 'function') {
                showLoading('Graph API 토큰 요청 중...');
            }
            
            const GRAPH_TIMEOUT_MS = 5000;
            graphToken = await new Promise((resolve, reject) => {
                // Timeout 설정
                const timeoutId = setTimeout(() => {
                    reject(new Error('Graph API 토큰 요청 시간 초과 (5초)'));
                }, GRAPH_TIMEOUT_MS);
                
                // getAuthToken 호출 (콜백 기반)
                microsoftTeams.authentication.getAuthToken({
                    resources: ['https://graph.microsoft.com/.default'],
                    silent: true,
                    successCallback: (token) => {
                        clearTimeout(timeoutId);
                        resolve(token);
                    },
                    failureCallback: (error) => {
                        clearTimeout(timeoutId);
                        reject(error);
                    }
                });
            });
        } catch (graphError) {
            if (typeof hideLoading === 'function') {
                hideLoading();
            }
            console.warn('Graph API 토큰 획득 실패(' + graphError.errorCode + '): ' + graphError.message);
        }
        
        // 3. 서버로 SSO 토큰과 Graph API 토큰 전송하여 세션에 저장 및 로그인 연동
        if (typeof showLoading === 'function') {
            showLoading('서버로 SSO 토큰과 Graph API 토큰 전송 중...');
        }
        const response = await fetch('/auth/teams/sso', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ 
                ssoToken: ssoToken,
                graphToken: graphToken
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
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        initTeams();
    });
} else {
    initTeams();
}

// 전역 함수로 노출 (다른 스크립트에서 사용 가능)
window.initTeams = initTeams;
window.tryTeamsSSO = tryTeamsSSO;
window.isTeams = isTeams;
window.isTeamsReady = isTeamsReady;

