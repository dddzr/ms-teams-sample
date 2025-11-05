// 미팅 목록 조회
async function loadMeetings() {
    const container = document.getElementById('meetingsList');
    container.innerHTML = '<div class="loading">로딩 중...</div>';
    
    try {
        const response = await fetch('/api/me/onlineMeetings');
        if (!response.ok) throw new Error('API 호출 실패');
        
        const meetings = await response.json();
        if (meetings.length === 0) {
            container.innerHTML = '<div class="error">예정된 미팅이 없습니다.</div>';
            return;
        }
        
        let html = '<div class="result-box">';
        meetings.forEach(meeting => {
            const startDate = meeting.startDateTime ? 
                new Date(meeting.startDateTime).toLocaleString('ko-KR') : 'N/A';
            const endDate = meeting.endDateTime ? 
                new Date(meeting.endDateTime).toLocaleString('ko-KR') : 'N/A';
            const joinUrl = meeting.joinWebUrl || meeting.joinUrl || '';
            html += `
                <div class="meeting-item">
                    <h4>${meeting.subject || '(제목 없음)'}</h4>
                    <p><strong>시작:</strong> ${startDate}</p>
                    <p><strong>종료:</strong> ${endDate}</p>
                    ${joinUrl ? `<p><a href="${joinUrl}" target="_blank">미팅 참가</a></p>` : ''}
                </div>
            `;
        });
        html += '</div>';
        container.innerHTML = html;
    } catch (error) {
        container.innerHTML = `<div class="error">오류: ${error.message}</div>`;
    }
}

// 미팅 생성
async function createMeeting() {
    const subject = document.getElementById('meetingSubject').value.trim();
    const start = document.getElementById('meetingStart').value;
    const end = document.getElementById('meetingEnd').value;
    const container = document.getElementById('meetingCreateResult');
    
    if (!subject || !start || !end) {
        container.innerHTML = '<div class="error">모든 필드를 입력해주세요</div>';
        return;
    }
    
    container.innerHTML = '<div class="loading">생성 중...</div>';
    
    try {
        const response = await fetch('/api/me/onlineMeetings', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                subject,
                startDateTime: new Date(start).toISOString(),
                endDateTime: new Date(end).toISOString()
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

