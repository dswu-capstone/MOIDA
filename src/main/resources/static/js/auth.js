// 로그인
async function handleLogin() {
  const memberId = document.getElementById('username').value.trim();
  const pw       = document.getElementById('password').value.trim();
  const errorMsg = document.getElementById('errorMsg');

  errorMsg.textContent = '';

  if (!memberId || !pw) {
    errorMsg.textContent = '아이디와 비밀번호를 입력해주세요.';
    return;
  }

  try {
    const data = await api.post('/api/login', { memberId, pw });

    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);

    window.location.href = '/post.html';

  } catch (e) {
    errorMsg.textContent = e.message || '아이디 또는 비밀번호가 올바르지 않습니다.';
  }
}

// 회원가입
async function handleSignup() {
  const memberId = document.getElementById('username').value.trim();
  const pw       = document.getElementById('password').value.trim();
  const nickname = document.getElementById('nickname').value.trim();
  const major    = document.getElementById('major')?.value.trim() || '';
  const goal     = document.getElementById('goal')?.value.trim() || '';
  const errorMsg = document.getElementById('errorMsg');

  errorMsg.textContent = '';

  const interestCategory = [...document.querySelectorAll('input[name="category"]:checked')]
    .map(el => el.value);

  if (!memberId) {
    errorMsg.textContent = '아이디를 입력해주세요.'; return;
  }
  if (!pw) {
    errorMsg.textContent = '비밀번호를 입력해주세요.'; return;
  }
  if (!nickname) {
    errorMsg.textContent = '닉네임을 입력해주세요.'; return;
  }
  if (!major) {
    errorMsg.textContent = '전공을 입력해주세요.'; return;
  }
  if (interestCategory.length === 0) {
    errorMsg.textContent = '관심 카테고리를 하나 이상 선택해주세요.'; return;
  }
  if (!goal) {
    errorMsg.textContent = '목표를 입력해주세요.'; return;
  }

  try {
    const data = await api.post('/api/signup', {
      memberId,
      pw,
      nickname,
      major,
      interestCategory,
      goal,
    });

    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);

    window.location.href = '/post.html';

  } catch (e) {
    errorMsg.textContent = e.message || '회원가입에 실패했습니다.';
  }
}

// 로그아웃
function handleLogout() {
  localStorage.clear();
  window.location.href = '/index.html';
}