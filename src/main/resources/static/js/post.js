const API_BASE = 'http://localhost:8080';
const LIMIT = 6;
const searchInput = document.getElementById('searchInput');
const searchBtn = document.getElementById('searchBtn');

let currentType = 'total';
let currentCategory = '';
let currentSearch = '';
let currentPage = 1;
let totalCount = 0;
let cursors = [null];

// API 호출
async function fetchPosts(cursor = null) {
    const params = new URLSearchParams();
    if (currentSearch) params.append('search', currentSearch);
    if (currentType !== 'total') params.append('type', currentType);
    if (currentCategory) params.append('tags', currentCategory);
    if (cursor) params.append('cursor', cursor);
    params.append('limit', LIMIT);

    try {
        const res = await fetch(`${API_BASE}/api/boards?${params.toString()}`);
        if (!res.ok) throw new Error('조회 실패');
        return await res.json();
    } catch (err) {
        console.error('게시글 조회 오류:', err);
        return { totalCount: 0, posts: [] };
    }
}

// 카드 렌더링
function renderCards(posts) {
    const grid = document.getElementById('postGrid');
    const empty = document.getElementById('emptyState');

    if (!posts || posts.length === 0) {
        grid.innerHTML = '';
        empty.style.display = 'flex';
        return;
    }

    empty.style.display = 'none';
    grid.innerHTML = posts.map(post => `
        <article class="post-card" onclick="location.href='post-detail.html?id=${post.id}'">
            <div class="card-badge ${post.boardType === 'study' ? 'badge-study' : 'badge-event'}">
                ${post.boardType === 'study' ? '스터디' : '이벤트'}
            </div>
            <h3 class="card-title">${post.title}</h3>
            <div class="card-tags">
                ${(post.tags || []).map(tag => `<span>#${tag}</span>`).join('')}
            </div>
        </article>
    `).join('');
}
// 돋보기 버튼 클릭 이벤트 추가
searchBtn.addEventListener('click', () => {
    // 현재 입력된 검색어 가져오기
    const keyword = searchInput.value.trim();

    // TODO: 여기에 기존에 만들어두신 '검색(필터링) 함수'를 호출하세요!
    // 예시: loadPosts(keyword, currentType, currentCategory);
    // (현재 post.js에 있는 목록 갱신 함수 이름을 넣어주시면 됩니다)
});
// 페이지네이션
function updatePagination() {
    const totalPages = Math.max(1, Math.ceil(totalCount / LIMIT));
    document.getElementById('pageInfo').textContent = `${currentPage} / ${totalPages}`;
    document.getElementById('prevBtn').disabled = currentPage <= 1;
    document.getElementById('nextBtn').disabled = currentPage >= totalPages;
}

async function loadPage(page) {
    currentPage = page;
    const cursor = cursors[page - 1] || null;
    const data = await fetchPosts(cursor);

    totalCount = data.totalCount;
    const posts = data.posts || [];

    if (posts.length > 0) {
        cursors[page] = posts[posts.length - 1].id;
    }

    renderCards(posts);
    updatePagination();
}

async function loadFresh() {
    cursors = [null];
    currentPage = 1;
    await loadPage(1);
}

// 탭 클릭
document.querySelectorAll('.tab').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('.tab').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        currentType = btn.dataset.type;
        loadFresh();
    });
});

// 카테고리 클릭
document.querySelectorAll('.category-tags .tag').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('.category-tags .tag').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        currentCategory = btn.dataset.cat;
        loadFresh();
    });
});

// 검색
let searchTimer;
document.querySelector('.header-search input').addEventListener('input', (e) => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => {
        currentSearch = e.target.value.trim();
        loadFresh();
    }, 400);
});


// AI 추천 버튼
document.querySelector('.tab-ai').addEventListener('click', async () => {
    const token = localStorage.getItem('accessToken');

    // 사용자 정보 (로그인 안 했으면 기본값)
    let userInput = {
        major: "",
        interestCategory: [],
        goal: "",
        availableDays: [],
        availableTime: [],
        introduce: "",
        level: "",
        locationType: "",
        region: ""
    };

    // TODO: 로그인 상태면 실제 사용자 프로필 가져오기

    try {
        const res = await fetch(`${API_BASE}/api/boards/recommend`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(userInput)
        });

        if (!res.ok) throw new Error('추천 실패');
        const data = await res.json();

        if (data.data && data.data.length > 0) {
            // 추천 결과를 모달이나 카드로 표시
            let html = '<div class="recommend-overlay" onclick="this.remove()">';
            html += '<div class="recommend-modal" onclick="event.stopPropagation()">';
            html += '<h2 class="recommend-title">✨ AI 추천 게시글</h2>';

            data.data.forEach(item => {
                html += `
                    <div class="recommend-card">
                        <h3>${item.title}</h3>
                        <div class="recommend-keywords">
                            ${item.keywords.map(k => `<span>#${k}</span>`).join('')}
                        </div>
                        <p class="recommend-reason">${item.reason}</p>
                    </div>
                `;
            });

            html += '<button class="recommend-close" onclick="this.parentElement.parentElement.remove()">닫기</button>';
            html += '</div></div>';

            document.body.insertAdjacentHTML('beforeend', html);
        } else {
            alert('추천할 게시글이 없습니다.');
        }
    } catch (err) {
        console.error('AI 추천 오류:', err);
        alert('AI 추천 서비스에 연결할 수 없습니다.');
    }
});

// 페이지 이동
document.getElementById('prevBtn').addEventListener('click', () => {
    if (currentPage > 1) loadPage(currentPage - 1);
});
document.getElementById('nextBtn').addEventListener('click', () => {
    const totalPages = Math.ceil(totalCount / LIMIT);
    if (currentPage < totalPages) loadPage(currentPage + 1);
});

// 초기 로드
loadFresh();