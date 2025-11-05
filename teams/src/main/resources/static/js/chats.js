// 채팅 선택 시 ID 자동 입력
function selectChat(chatId) {
    document.getElementById('chatIdForMessages').value = chatId;
    document.getElementById('chatIdForSend').value = chatId;
}

// 채팅 목록 조회
async function loadChats() {
    const container = document.getElementById('chatsList');
    container.innerHTML = '<div class="loading">로딩 중...</div>';
    
    try {
        const response = await fetch('/api/chats');
        if (!response.ok) throw new Error('API 호출 실패');
        
        const chats = await response.json();
        if (chats.length === 0) {
            container.innerHTML = '<div class="error">채팅이 없습니다.</div>';
            return;
        }
        
        let html = '<div class="result-box">';
        chats.forEach(chat => {
            html += `
                <div class="chat-item" onclick="selectChat('${chat.id}')">
                    <h4>${chat.topic || '(제목 없음)'}</h4>
                    <p><strong>타입:</strong> ${chat.chatType}</p>
                    <p><strong>ID:</strong> ${chat.id}</p>
                    ${chat.webUrl ? `<p><a href="${chat.webUrl}" target="_blank">채팅 열기</a></p>` : ''}
                </div>
            `;
        });
        html += '</div>';
        container.innerHTML = html;
    } catch (error) {
        container.innerHTML = `<div class="error">오류: ${error.message}</div>`;
    }
}

// 채팅 메시지 조회
async function loadChatMessages() {
    const chatId = document.getElementById('chatIdForMessages').value.trim();
    const container = document.getElementById('chatMessagesList');
    
    if (!chatId) {
        container.innerHTML = '<div class="error">Chat ID를 입력해주세요</div>';
        return;
    }
    
    container.innerHTML = '<div class="loading">로딩 중...</div>';
    
    try {
        const response = await fetch(`/api/chats/${chatId}/messages`);
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
                    <h4>${message.from || 'Unknown'}</h4>
                    <p><strong>시간:</strong> ${message.createdDateTime || 'N/A'}</p>
                    <p><strong>내용:</strong> ${message.body || 'N/A'}</p>
                </div>
            `;
        });
        html += '</div>';
        container.innerHTML = html;
    } catch (error) {
        container.innerHTML = `<div class="error">오류: ${error.message}</div>`;
    }
}

// 채팅 멤버 조회
async function loadChatMembers() {
    const chatId = document.getElementById('chatIdForMessages').value.trim();
    const container = document.getElementById('chatMembersList');
    
    if (!chatId) {
        container.innerHTML = '<div class="error">Chat ID를 입력해주세요</div>';
        return;
    }
    
    container.innerHTML = '<div class="loading">로딩 중...</div>';
    
    try {
        const response = await fetch(`/api/chats/${chatId}/members`);
        if (!response.ok) throw new Error('API 호출 실패');
        
        const members = await response.json();
        container.innerHTML = '<div class="result-box"><pre>' + 
            JSON.stringify(members, null, 2) + '</pre></div>';
    } catch (error) {
        container.innerHTML = `<div class="error">오류: ${error.message}</div>`;
    }
}

// 채팅 메시지 전송
async function sendChatMessage() {
    const chatId = document.getElementById('chatIdForSend').value.trim();
    const body = document.getElementById('chatMessageBody').value.trim();
    const container = document.getElementById('chatSendResult');
    
    if (!chatId || !body) {
        container.innerHTML = '<div class="error">Chat ID와 메시지를 입력해주세요</div>';
        return;
    }
    
    container.innerHTML = '<div class="loading">전송 중...</div>';
    
    try {
        const response = await fetch(`/api/chats/${chatId}/messages`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ body })
        });
        if (!response.ok) throw new Error('API 호출 실패');
        
        const result = await response.json();
        container.innerHTML = '<div class="result-box"><pre>' + 
            JSON.stringify(result, null, 2) + '</pre></div>';
        document.getElementById('chatMessageBody').value = '';
    } catch (error) {
        container.innerHTML = `<div class="error">오류: ${error.message}</div>`;
    }
}

// 채팅 생성
async function createChat() {
    const chatType = document.getElementById('chatType').value;
    const userIdsInput = document.getElementById('chatUserIds').value.trim();
    const container = document.getElementById('chatCreateResult');
    
    if (!userIdsInput) {
        container.innerHTML = '<div class="error">사용자 ID를 입력해주세요</div>';
        return;
    }
    
    // 사용자 ID 파싱 (쉼표로 구분)
    const userIds = userIdsInput.split(',')
        .map(id => id.trim())
        .filter(id => id.length > 0);
    
    if (userIds.length < 2) {
        container.innerHTML = '<div class="error">최소 2명의 사용자 ID가 필요합니다</div>';
        return;
    }
    
    container.innerHTML = '<div class="loading">생성 중...</div>';
    
    try {
        const response = await fetch('/api/chats', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                chatType: chatType,
                userIds: userIds
            })
        });
        
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || 'API 호출 실패');
        }
        
        const result = await response.json();
        container.innerHTML = '<div class="result-box"><pre>' + 
            JSON.stringify(result, null, 2) + '</pre></div>';
    } catch (error) {
        container.innerHTML = `<div class="error">오류: ${error.message}</div>`;
    }
}

