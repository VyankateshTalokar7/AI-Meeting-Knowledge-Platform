const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

async function request(path, options = {}) {
  const isFormData = options.body instanceof FormData
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
      ...options.headers,
    },
  })

  const body = await response.json().catch(() => null)
  if (!response.ok) {
    const error = new Error(body?.message || 'The request could not be completed.')
    error.status = response.status
    error.fieldErrors = body?.fieldErrors || {}
    throw error
  }
  return body
}

export function requestErrorMessage(error) {
  const fieldMessages = Object.values(error.fieldErrors || {})
  return fieldMessages.length > 0 ? fieldMessages.join(' ') : error.message
}

export function registerUser(payload) {
  return request('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function loginUser(payload) {
  return request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function getCurrentUser(token) {
  return authenticatedRequest('/api/users/me', token)
}

function authenticatedRequest(path, token, options = {}) {
  return request(path, {
    ...options,
    headers: {
      ...options.headers,
      Authorization: `Bearer ${token}`,
    },
  })
}

export function createMeeting(token, payload) {
  return authenticatedRequest('/api/meetings', token, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function getMeetings(token) {
  return authenticatedRequest('/api/meetings', token)
}

export function getMeeting(token, id) {
  return authenticatedRequest(`/api/meetings/${id}`, token)
}

export function deleteMeeting(token, id) {
  return authenticatedRequest(`/api/meetings/${id}`, token, { method: 'DELETE' })
}

export function getMeetingAudio(token, meetingId) {
  return authenticatedRequest(`/api/meetings/${meetingId}/audio`, token)
}

export function deleteMeetingAudio(token, meetingId) {
  return authenticatedRequest(`/api/meetings/${meetingId}/audio`, token, { method: 'DELETE' })
}

export function uploadMeetingAudio(token, meetingId, file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('POST', `${API_BASE_URL}/api/meetings/${meetingId}/audio`)
    xhr.setRequestHeader('Authorization', `Bearer ${token}`)
    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) {
        onProgress(Math.round((event.loaded / event.total) * 100))
      }
    }
    xhr.onload = () => {
      let body = null
      try {
        body = xhr.responseText ? JSON.parse(xhr.responseText) : null
      } catch {
        body = null
      }
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve(body)
        return
      }
      const error = new Error(body?.message || 'The request could not be completed.')
      error.status = xhr.status
      error.fieldErrors = body?.fieldErrors || {}
      reject(error)
    }
    xhr.onerror = () => reject(new Error('Unable to reach the server.'))
    xhr.send(formData)
  })
}
