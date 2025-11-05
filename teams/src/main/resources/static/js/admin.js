// 관리자 권한 확인
async function checkAdminStatus() {
    const container = document.getElementById('adminStatus');
    container.innerHTML = '<div class="loading">확인 중...</div>';
    
    try {
        const response = await fetch('/api/admin/check');
        if (!response.ok) throw new Error('API 호출 실패');
        
        const data = await response.json();
        const statusHtml = `
            <div class="result-box">
                <h3>관리자 권한 상태</h3>
                <p><strong>관리자 여부:</strong> ${data.isAdmin ? '✅ 예' : '❌ 아니오'}</p>
                ${data.scopes ? `<p><strong>Scope:</strong> ${data.scopes}</p>` : ''}
                ${data.roles ? `<p><strong>Roles:</strong> ${JSON.stringify(data.roles)}</p>` : ''}
            </div>
        `;
        container.innerHTML = statusHtml;
    } catch (error) {
        container.innerHTML = `<div class="error">오류: ${error.message}</div>`;
    }
}

// 토큰 정보 보기
async function showTokenInfo() {
    const container = document.getElementById('tokenInfo');
    container.innerHTML = '<div class="loading">로딩 중...</div>';
    
    try {
        const response = await fetch('/api/admin/token-info');
        if (!response.ok) throw new Error('API 호출 실패');
        
        const data = await response.json();
        container.innerHTML = '<div class="result-box"><pre>' + 
            JSON.stringify(data, null, 2) + '</pre></div>';
    } catch (error) {
        container.innerHTML = `<div class="error">오류: ${error.message}</div>`;
    }
}

// 사용자 검색
async function searchUsers() {
    const searchTerm = document.getElementById('userSearch').value.trim();
    const container = document.getElementById('usersList');
    
    if (!searchTerm) {
        container.innerHTML = '<div class="error">검색어를 입력해주세요</div>';
        return;
    }
    
    container.innerHTML = '<div class="loading">검색 중...</div>';
    
    try {
        const response = await fetch(`/api/admin/users?search=${encodeURIComponent(searchTerm)}`);
        if (!response.ok) {
            if (response.status === 403) {
                throw new Error('관리자 권한이 필요합니다');
            }
            throw new Error('API 호출 실패');
        }
        
        const users = await response.json();
        if (users.length === 0) {
            container.innerHTML = '<div class="error">검색 결과가 없습니다</div>';
            return;
        }
        
        let html = '<div class="result-box">';
        users.forEach(user => {
            html += `
                <div class="user-item">
                    <h4>${user.displayName || 'N/A'}</h4>
                    <p><strong>이메일:</strong> ${user.mail || user.userPrincipalName || 'N/A'}</p>
                    <p><strong>ID:</strong> ${user.id || 'N/A'}</p>
                    ${user.jobTitle ? `<p><strong>직책:</strong> ${user.jobTitle}</p>` : ''}
                </div>
            `;
        });
        html += '</div>';
        container.innerHTML = html;
    } catch (error) {
        container.innerHTML = `<div class="error">오류: ${error.message}</div>`;
    }
}

