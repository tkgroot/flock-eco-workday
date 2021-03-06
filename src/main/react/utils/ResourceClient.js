const internalize = res => {
  if (res.ok) {
    if (res.status === 204) {
      return null
    }
    return res.json()
  }
  return res.text(text => {
    throw new Error(text)
  })
}

export function ResourceClient(path) {
  const get = id => {
    const opts = {
      method: "GET",
    }
    return fetch(`${path}/${id}`, opts).then(internalize)
  }

  const post = item => {
    const opts = {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(item),
    }
    return fetch(path, opts).then(internalize)
  }

  const put = (id, item) => {
    const opts = {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(item),
    }
    return fetch(`${path}/${id}`, opts).then(internalize)
  }

  const del = id => {
    const opts = {
      method: "DELETE",
    }
    return fetch(`${path}/${id}`, opts).then(internalize)
  }

  return {get, post, put, delete: del}
}
