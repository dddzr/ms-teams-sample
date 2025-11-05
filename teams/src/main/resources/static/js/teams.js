// Team 선택 시 ID 자동 입력
function selectTeam(teamId) {
    document.getElementById('teamIdForChannels').value = teamId;
    document.getElementById('teamIdForMessages').value = teamId;
    document.getElementById('channelTeamId').value = teamId;
}

// Teams 목록 조회
async function loadTeams() {
    const container = document.getElementById('teamsList');
    container.innerHTML = '<div class="loading">로딩 중...</div>';
    
    try {
        const response = await fetch('/api/teams');
        if (!response.ok) throw new Error('API 호출 실패');
        
        const teams = await response.json();
        if (teams.length === 0) {
            container.innerHTML = '<div class="error">Teams가 없습니다.</div>';
            return;
        }
        
        let html = '<div class="result-box">';
        teams.forEach(team => {
            html += `
                <div class="team-item" onclick="selectTeam('${team.id}')">
                    <h4>${team.displayName || 'N/A'}</h4>
                    <p><strong>ID:</strong> ${team.id}</p>
                    <p><strong>설명:</strong> ${team.description || 'N/A'}</p>
                    ${team.webUrl ? `<p><a href="${team.webUrl}" target="_blank">Teams 열기</a></p>` : ''}
                </div>
            `;
        });
        html += '</div>';
        container.innerHTML = html;
    } catch (error) {
        container.innerHTML = `<div class="error">오류: ${error.message}</div>`;
    }
}

// 채널 선택 시 ID 자동 입력
function selectChannel(channelId) {
    document.getElementById('channelIdForMessages').value = channelId;
}

// 채널 목록 조회
async function loadChannels() {
    const teamId = document.getElementById('teamIdForChannels').value.trim();
    const container = document.getElementById('channelsList');
    
    if (!teamId) {
        container.innerHTML = '<div class="error">Team ID를 입력해주세요</div>';
        return;
    }
    
    container.innerHTML = '<div class="loading">로딩 중...</div>';
    
    try {
        const response = await fetch(`/api/teams/${teamId}/channels`);
        if (!response.ok) throw new Error('API 호출 실패');
        
        const channels = await response.json();
        if (channels.length === 0) {
            container.innerHTML = '<div class="error">채널이 없습니다.</div>';
            return;
        }
        
        let html = '<div class="result-box">';
        channels.forEach(channel => {
            html += `
                <div class="channel-item" onclick="selectChannel('${channel.id}')">
                    <h4>${channel.displayName || 'N/A'}</h4>
                    <p><strong>ID:</strong> ${channel.id}</p>
                    <p><strong>설명:</strong> ${channel.description || 'N/A'}</p>
                    ${channel.webUrl ? `<p><a href="${channel.webUrl}" target="_blank">채널 열기</a></p>` : ''}
                </div>
            `;
        });
        html += '</div>';
        container.innerHTML = html;
    } catch (error) {
        container.innerHTML = `<div class="error">오류: ${error.message}</div>`;
    }
}

// 채널 생성
async function createChannel() {
    const teamId = document.getElementById('channelTeamId').value.trim();
    const name = document.getElementById('channelName').value.trim();
    const description = document.getElementById('channelDescription').value.trim();
    const container = document.getElementById('channelCreateResult');
    
    if (!teamId || !name) {
        container.innerHTML = '<div class="error">Team ID와 채널 이름을 입력해주세요</div>';
        return;
    }
    
    container.innerHTML = '<div class="loading">생성 중...</div>';
    
    try {
        const response = await fetch(`/api/teams/${teamId}/channels`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                displayName: name,
                description: description || ''
            })
        });
        if (!response.ok) throw new Error('API 호출 실패');
        
        const result = await response.json();
        container.innerHTML = '<div class="result-box"><pre>' + 
            JSON.stringify(result, null, 2) + '</pre></div>';
    } catch (error) {
        container.innerHTML = `<div class="error">오류: ${error.message}</div>`;
    }
}

// 채널 메시지 조회
async function loadMessages() {
    const teamId = document.getElementById('teamIdForMessages').value.trim();
    const channelId = document.getElementById('channelIdForMessages').value.trim();
    const container = document.getElementById('messagesList');
    
    if (!teamId || !channelId) {
        container.innerHTML = '<div class="error">Team ID와 Channel ID를 입력해주세요</div>';
        return;
    }
    
    container.innerHTML = '<div class="loading">로딩 중...</div>';
    
    try {
        const response = await fetch(`/api/teams/${teamId}/channels/${channelId}/messages`);
        if (!response.ok) throw new Error('API 호출 실패');
        
        const messages = await response.json();
        if (messages.length === 0) {
            container.innerHTML = '<div class="error">메시지가 없습니다.</div>';
            return;
        }
        
        let html = '<div class="result-box">';
        messages.forEach(message => {
            html += `
                <div class="message-item">
                    <h4>${message.subject || '제목 없음'}</h4>
                    <p><strong>발신자:</strong> ${message.from || 'N/A'}</p>
                    <p><strong>시간:</strong> ${message.createdDateTime || 'N/A'}</p>
                    <p><strong>내용:</strong> ${message.bodyPreview || message.body || 'N/A'}</p>
                </div>
            `;
        });
        html += '</div>';
        container.innerHTML = html;
    } catch (error) {
        container.innerHTML = `<div class="error">오류: ${error.message}</div>`;
    }
}

