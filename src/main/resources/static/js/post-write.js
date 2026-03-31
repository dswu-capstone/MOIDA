// AI 태그 생성 (AI 서버에서 실제로 추출)
async function generateTags() {
    const content = document.getElementById('post_content').value;
    const title = document.getElementById('post_title').value;
    const category = document.querySelector('input[name="category"]:checked').value;
    const boardType = document.querySelector('input[name="post_type"]:checked').value;

    if (content.trim() === "" || title.trim() === "") {
        alert("제목과 상세 내용을 먼저 작성해주세요!");
        return;
    }

    const resultDiv = document.getElementById('ai_result');
    resultDiv.innerHTML = '<span class="ai-desc">⏳ AI가 태그를 생성하고 있습니다...</span>';
    resultDiv.classList.add('active');

    try {
        const res = await fetch('http://localhost:8080/api/boards/tags', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                title: title,
                body: content,
                category: category,
                boardType: boardType
            })
        });

        if (!res.ok) throw new Error('태그 생성 실패');
        const data = await res.json();

        if (data.tags && data.tags.length > 0) {
            resultDiv.innerHTML = data.tags.map(tag =>
                `<span class="ai-tag">#${tag}</span>`
            ).join('') + '<span class="ai-desc">(AI가 추출한 태그입니다)</span>';
        } else {
            resultDiv.innerHTML = '<span class="ai-desc">태그를 생성하지 못했습니다. 내용을 더 구체적으로 작성해주세요.</span>';
        }
    } catch (err) {
        console.error('AI 태그 생성 오류:', err);
        resultDiv.innerHTML = '<span class="ai-desc">AI 서버에 연결할 수 없습니다.</span>';
    }
}

// 게시글 등록
async function submitPost() {
    const token = localStorage.getItem('accessToken');

    if (!token) {
        alert('로그인이 필요합니다.');
        location.href = 'index.html';
        return;
    }

    const boardType = document.querySelector('input[name="post_type"]:checked').value;
    const category = document.querySelector('input[name="category"]:checked').value;
    const title = document.getElementById('post_title').value.trim();
    const body = document.getElementById('post_content').value.trim();

    if (!title || !body) {
        alert('제목과 내용을 모두 입력해주세요.');
        return;
    }

    const tagElements = document.querySelectorAll('#ai_result .ai-tag');
    const tags = Array.from(tagElements).map(el => el.textContent.replace('#', ''));

    if (tags.length === 0) {
        const proceed = confirm('AI 태그가 없습니다. 태그 없이 등록하시겠습니까?');
        if (!proceed) return;
    }

    // 로딩 표시
    let loadingHtml = '<div class="loading-overlay" id="loadingOverlay">';
    loadingHtml += '<div class="loading-modal">';
    loadingHtml += '<div class="loading-spinner"></div>';
    loadingHtml += '<p>게시글을 등록하고 있습니다...</p>';
    loadingHtml += '</div></div>';
    document.body.insertAdjacentHTML('beforeend', loadingHtml);

    // 등록 버튼 비활성화 (중복 클릭 방지)
    const submitBtn = document.querySelector('.btn-submit');
    submitBtn.disabled = true;

    const openChatLink = document.getElementById('open_chat').value.trim();

    try {
        const res = await fetch('http://localhost:8080/api/boards', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                boardType: boardType,
                title: title,
                body: body,
                category: category,
                tags: tags,
                openChatLink: openChatLink
            })
        });

        if (!res.ok) throw new Error('등록 실패');

        alert('게시글이 등록되었습니다!');
        location.href = 'post.html';

    } catch (err) {
        console.error('게시글 등록 오류:', err);
        document.getElementById('loadingOverlay').remove();
        submitBtn.disabled = false;
        alert('게시글 등록에 실패했습니다.');
    }
}