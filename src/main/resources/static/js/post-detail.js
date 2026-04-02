// URL에서 게시글 id 추출
const params = new URLSearchParams(window.location.search);
const postId = params.get('id');

async function loadPostDetail() {
    if (!postId) {
        alert('게시글 ID가 없습니다.');
        location.href = 'post.html';
        return;
    }

    try {
        // api.js의 get 메서드를 사용합니다. (토큰 헤더 처리를 api.js가 알아서 해줍니다!)
        const data = await api.get(`/api/boards/${postId}`);

        // 배지
        const badge = document.getElementById('detailBadge');
        badge.textContent = data.boardType === 'study' ? '스터디' : '이벤트';
        badge.className = `detail-badge ${data.boardType === 'study' ? 'badge-study' : 'badge-event'}`;

        // 제목
        document.getElementById('detailTitle').textContent = data.title;

        // 태그
        const tagsEl = document.getElementById('detailTags');
        tagsEl.innerHTML = (data.tags || []).map(tag => `<span>#${tag}</span>`).join('');

        // 작성자
        document.getElementById('detailWriter').textContent = `작성자: ${data.writer || '알 수 없음'}`;

        // 날짜
        if (data.createdAt) {
            const date = new Date(data.createdAt);
            document.getElementById('detailDate').textContent =
                `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`;
        }

        // 본문
        document.getElementById('detailBody').textContent = data.body;

        // 오픈채팅 링크 저장
        if (data.openChatLink) {
            document.getElementById('applyBtn').dataset.link = data.openChatLink;
        } else {
            document.getElementById('applyBtn').dataset.link = '';
        }

        // 페이지 타이틀
        document.title = `MOIDA - ${data.title}`;

    } catch (err) {
        console.error('게시글 상세 조회 오류:', err);
        document.getElementById('detailTitle').textContent = '게시글을 불러올 수 없습니다.';
        document.getElementById('detailBody').textContent = '다시 시도해주세요.';
    }
}

function applyPost() {
    const link = document.getElementById('applyBtn').dataset.link;
    const chatBox = document.getElementById('openChatBox');
    const chatLink = document.getElementById('openChatLink');

    if (!link) {
        alert('작성자가 오픈채팅 링크를 등록하지 않았습니다.');
        return;
    }

    chatBox.style.display = 'block';
    chatLink.href = link;
    chatLink.textContent = link;
}

loadPostDetail();