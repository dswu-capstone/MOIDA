
// 선호 지역 펼치기/접기
function toggleRegion() {
  const wrap = document.getElementById('regionWrap');
  const btn  = document.getElementById('regionToggle');
  const isCollapsed = wrap.classList.contains('collapsed');
  wrap.classList.toggle('collapsed');
  btn.textContent = isCollapsed ? '▲ 접기' : '▼ 전체 보기';
}

// 관심 분야 최대 2개 제한
document.querySelectorAll('input[name="interest"]').forEach(cb => {
  cb.addEventListener('change', () => {
    const checked = document.querySelectorAll('input[name="interest"]:checked');
    if (checked.length > 2) {
      cb.checked = false;
    }
  });
});