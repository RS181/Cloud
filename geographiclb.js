addEventListener('fetch', event => {
  event.respondWith(handleRequest(event.request));
});

async function handleRequest(request) {
  const continentCode = request.cf.continent;
  let backendHost; // Usar um nome de domínio configurado na Cloudflare

  switch (continentCode) {
    case 'EU':
      backendHost = 'westeulb.up202109728.pt'; // load balancer EU
      //backendHost = 'origin.up202109728.pt';
      break;
    case 'AS':
      backendHost = 'eastaslb.up202109728.pt'; // load balancer US
      break;
    default:
      backendHost = 'origin.up202109728.pt'; // Origin VM
  }

  if (!backendHost) {
    return new Response('Erro de encaminhamento.', { status: 500 });
  }

  const url = new URL(request.url);
  const backendURL = `https://${backendHost}${url.pathname}${url.search}`;

  try {
    const backendResponse = await fetch(backendURL, {
      method: request.method,
      headers: request.headers,
      body: request.body,
    });

    // Clona os headers da resposta original
    const modifiedHeaders = new Headers(backendResponse.headers);
    modifiedHeaders.set("X-Cache-Status", backendResponse.headers.get("X-Cache-Status") || "MISS");
    modifiedHeaders.set("X-Served-By", backendResponse.headers.get("X-Served-By") || "unknown");

    // Enviar a resposta do backend para o cliente
    return new Response(backendResponse.body, {
      status: backendResponse.status,
      statusText: backendResponse.statusText,
      headers: modifiedHeaders
    });


  } catch (error) {
    console.error('Erro ao buscar do backend:', error);
    return new Response('Erro ao conectar ao servidor de origem.', { status: 500 });
  }
}

export default {
  async fetch(request, env, ctx) {
    return handleRequest(request);
  },
};
