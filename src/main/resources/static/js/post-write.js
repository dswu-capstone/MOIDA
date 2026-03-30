function generateTags() {
    const resultDiv = document.getElementById('ai_result');
    const content = document.getElementById('post_content').value;

    if (content.trim() === "") {
        alert("상세 내용을 먼저 작성해주세요!");
        return;
    }

    setTimeout(() => {
        resultDiv.classList.add('active');
    }, 500);
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

    // AI 태그 수집 (생성된 태그가 있으면 가져오기)
    const tagElements = document.querySelectorAll('#ai_result .ai-tag');
    const tags = Array.from(tagElements).map(el => el.textContent.replace('#', ''));

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
                tags: tags
            })
        });

        if (!res.ok) throw new Error('등록 실패');

        alert('게시글이 등록되었습니다!');
        location.href = 'post.html';

    } catch (err) {
        console.error('게시글 등록 오류:', err);
        alert('게시글 등록에 실패했습니다.');
    }
}