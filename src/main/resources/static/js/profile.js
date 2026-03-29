// ─────────────────────────────────────────
// profile.js  ·  프로필 조회 / 등록·수정
// ─────────────────────────────────────────

// ── 페이지 로드 시 프로필 조회 ─────────────
document.addEventListener('DOMContentLoaded', async () => {
  await loadProfile();

  // 관심 분야 최대 2개 제한
  document.querySelectorAll('input[name="interest"]').forEach(cb => {
    cb.addEventListener('change', () => {
      const checked = document.querySelectorAll('input[name="interest"]:checked');
      if (checked.length > 2) cb.checked = false;
    });
  });
});

// ── 프로필 조회 (GET /api/profile) ──────────
async function loadProfile() {
  try {
    const data = await api.get('/api/profile');

    // 회원가입 때 입력한 값 → 표시
    document.getElementById('nicknameDisplay').textContent = data.nickname || '';
    document.getElementById('major').value = data.major || '';
    document.getElementById('goal').value  = data.goal  || '';

    // 관심 카테고리 체크
    if (data.interestCategory) {
      data.interestCategory.forEach(val => {
        const cb = document.querySelector(`input[name="interest"][value="${val}"]`);
        if (cb) cb.checked = true;
      });
    }

    // 이하는 null이면 그냥 빈 상태로 두면 됨
    if (data.availableDays) {
      data.availableDays.forEach(val => {
        const cb = document.querySelector(`input[name="day"][value="${val}"]`);
        if (cb) cb.checked = true;
      });
    }

    if (data.availableTime) {
      data.availableTime.forEach(val => {
        const cb = document.querySelector(`input[name="time"][value="${val}"]`);
        if (cb) cb.checked = true;
      });
    }

    if (data.introduce) {
      document.getElementById('introduce').value = data.introduce;
    }

    if (data.level) {
      const rb = document.querySelector(`input[name="level"][value="${data.level}"]`);
      if (rb) rb.checked = true;
    }

    if (data.locationType) {
      const rb = document.querySelector(`input[name="locationType"][value="${data.locationType}"]`);
      if (rb) rb.checked = true;
    }

    if (data.region) {
      const cb = document.querySelector(`input[name="region"][value="${data.region}"]`);
      if (cb) cb.checked = true;
    }

  } catch (e) {
    console.error('프로필 조회 실패:', e.message);
  }
}

// ── 프로필 저장 (POST /api/profile) ─────────
async function saveProfile() {
  const availableDays = [...document.querySelectorAll('input[name="day"]:checked')]
    .map(el => el.value);

  const availableTime = [...document.querySelectorAll('input[name="time"]:checked')]
    .map(el => el.value);

  const levelEl       = document.querySelector('input[name="level"]:checked');
  const locationEl    = document.querySelector('input[name="locationType"]:checked');
  const regionEl      = document.querySelector('input[name="region"]:checked');

  const body = {
    availableDays,
    availableTime,
    introduce:    document.getElementById('introduce').value.trim(),
    level:        levelEl     ? levelEl.value     : null,
    locationType: locationEl  ? locationEl.value  : null,
    region:       regionEl    ? regionEl.value     : null,
  };

  try {
    await api.post('/api/profile', body);
    alert('프로필이 저장되었습니다!');
  } catch (e) {
    alert('저장에 실패했습니다: ' + e.message);
  }
}

// ── 지역 펼치기/접기 ─────────────────────────
function toggleRegion() {
  const wrap = document.getElementById('regionWrap');
  const btn  = document.getElementById('regionToggle');
  const isCollapsed = wrap.classList.contains('collapsed');
  wrap.classList.toggle('collapsed');
  btn.textContent = isCollapsed ? '▲ 접기' : '▼ 전체 보기';
}