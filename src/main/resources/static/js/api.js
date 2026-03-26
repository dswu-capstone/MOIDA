// fetch 공통 함수

const BASE_URL = '';

async function apiCall(method, path, body = null) {
  const token = localStorage.getItem('accessToken');

  const options = {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': 'Bearer ' + token }),
    },
  };

  if (body) options.body = JSON.stringify(body);

  const res = await fetch(BASE_URL + path, options);

  // 401 → 토큰 만료, 로그인 페이지로
  if (res.status === 401) {
    localStorage.clear();
    window.location.href = '/index.html';
    return;
  }

  const text = await res.text();
  const data = text ? JSON.parse(text) : null;

  if (!res.ok) {
    throw new Error(data?.message || '요청에 실패했습니다.');
  }

  return data;
}

const api = {
  get:    (path)       => apiCall('GET',    path),
  post:   (path, body) => apiCall('POST',   path, body),
  put:    (path, body) => apiCall('PUT',    path, body),
  delete: (path)       => apiCall('DELETE', path),
};