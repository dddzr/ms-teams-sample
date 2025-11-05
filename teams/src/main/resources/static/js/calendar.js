// 일정 조회
async function loadEvents() {
    const container = document.getElementById('eventsList');
    container.innerHTML = '<div class="loading">로딩 중...</div>';
    
    try {
        const response = await fetch('/api/me/events');
        if (!response.ok) throw new Error('API 호출 실패');
        
        const events = await response.json();
        if (events.length === 0) {
            container.innerHTML = '<div class="error">일정이 없습니다.</div>';
            return;
        }
        
        let html = '<div class="result-box">';
        events.forEach(event => {
            const startDate = event.start ? 
                new Date(event.start).toLocaleString('ko-KR') : 'N/A';
            html += `
                <div class="event-item">
                    <h4>${event.subject || '(제목 없음)'}</h4>
                    <p><strong>시작:</strong> ${startDate}</p>
                    ${event.location ? `<p><strong>장소:</strong> ${event.location}</p>` : ''}
                    ${event.body ? `<p><strong>내용:</strong> ${event.body.substring(0, 100)}${event.body.length > 100 ? '...' : ''}</p>` : ''}
                </div>
            `;
        });
        html += '</div>';
        container.innerHTML = html;
    } catch (error) {
        container.innerHTML = `<div class="error">오류: ${error.message}</div>`;
    }
}

// 일정 생성
async function createCalendarEvent() {
    const subject = document.getElementById('eventSubject').value.trim();
    const start = document.getElementById('eventStart').value;
    const end = document.getElementById('eventEnd').value;
    const location = document.getElementById('eventLocation').value.trim();
    const body = document.getElementById('eventBody').value.trim();
    const container = document.getElementById('eventCreateResult');
    
    if (!subject || !start || !end) {
        container.innerHTML = '<div class="error">제목, 시작, 종료 시간을 입력해주세요</div>';
        return;
    }
    
    container.innerHTML = '<div class="loading">생성 중...</div>';
    
    try {
        const response = await fetch('/api/me/events', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                subject,
                body: body || '',
                start: new Date(start).toISOString(),
                end: new Date(end).toISOString(),
                location: location || ''
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

