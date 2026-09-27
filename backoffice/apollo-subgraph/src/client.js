const BACKOFFICE_URL_GRAP = process.env.BACKOFFICE_URL;

function normalizeValue(value) {
  if (Array.isArray(value)) {
    return value.map(normalizeValue);
  }

  if (value && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value).map(([key, innerValue]) => [key, normalizeValue(innerValue)])
    );
  }

  if (typeof value === 'bigint') {
    return value.toString();
  }

  return value;
}

export async function callBackoffice(query, variables = {}, token = '') {
  if (!BACKOFFICE_URL_GRAP) {
    throw new Error('Falta PROJECT_ASSISTANCE_GRAPHQL_URL en el .env del subgraph');
  }

  const headers = {
    'Content-Type': 'application/json'
  };

  if (token) {
    headers.Authorization = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
  }

  const response = await fetch(BACKOFFICE_URL_GRAP, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      query,
      variables: normalizeValue(variables)
    })
  });

  const text = await response.text();
  let payload;
  try {
    payload = JSON.parse(text);
  } catch {
    throw new Error(`Respuesta no-JSON de backoffice (HTTP ${response.status}): ${text.slice(0, 200)}`);
  }

  if (!response.ok) {
    throw new Error(payload?.errors?.map((error) => error.message).join(' | ') || `Error HTTP ${response.status} llamando a backoffice`);
  }

  if (payload.errors?.length) {
    throw new Error(payload.errors.map((error) => error.message).join(' | '));
  }

  return payload.data;
}