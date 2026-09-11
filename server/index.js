const http = require('http');

const PORT = process.env.APP_PORT || 3000;
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || '';
const DEFAULT_MODEL = 'gemini-2.5-flash';

// System prompt giving the AI a dedicated gaming copilot persona
const DEFAULT_SYSTEM_INSTRUCTION = `You are GameVault AI Copilot, an expert, personalized gaming companion inside the GameVault app.
Your mission is to help the player decide what to play next, manage their gaming backlog, analyze their gaming habits, and guide them through game franchises/series.

CRITICAL COPILOT RULES:
1. When the user asks for recommendations from their GameVault, backlog, or franchises, USE ONLY the games provided in [USER'S GAMEVAULT STATE]. Do not hallucinate games they do not own.
2. Respect playtime, series order, and game status when making gaming decisions.
3. Be concise, direct, helpful, and gamer-friendly.
4. STRICT FORMATTING:
   - NEVER use star emojis (⭐, ★, ☆, etc.).
   - Format ratings as "Rating: X.X/5.0" or "My Rating: X/10".
   - Format Metacritic as "Metacritic: XX".
   - Use bullet points and clear section headers.
   - Do NOT output raw unstyled markdown symbols; use clean structured text.
`;

function buildGeminiPayload(body) {
  const model = body.model || DEFAULT_MODEL;
  const systemText = body.systemInstruction || DEFAULT_SYSTEM_INSTRUCTION;

  const contents = [];

  // If conversation history is provided
  if (Array.isArray(body.messages) && body.messages.length > 0) {
    for (const msg of body.messages) {
      if (!msg.text || !msg.text.trim()) continue;
      const role = (msg.role === 'model' || msg.sender === 'AI') ? 'model' : 'user';
      contents.push({
        role: role,
        parts: [{ text: msg.text }]
      });
    }
  }

  // Active or focused context to include with current prompt
  let currentPrompt = body.prompt || '';
  if (body.context && body.context.trim()) {
    currentPrompt = `${body.context.trim()}\n\nUser Question: ${currentPrompt}`;
  }

  if (currentPrompt.trim()) {
    // If last message was not already this prompt
    const lastContent = contents[contents.length - 1];
    if (!lastContent || lastContent.role !== 'user' || lastContent.parts[0]?.text !== currentPrompt) {
      contents.push({
        role: 'user',
        parts: [{ text: currentPrompt }]
      });
    }
  }

  if (contents.length === 0) {
    contents.push({
      role: 'user',
      parts: [{ text: 'Hello! What can you help me with in GameVault?' }]
    });
  }

  const payload = {
    contents: contents,
    generationConfig: {
      temperature: body.temperature !== undefined ? body.temperature : 0.7,
      topP: body.topP !== undefined ? body.topP : 0.95,
      maxOutputTokens: 2048
    }
  };

  if (systemText && systemText.trim()) {
    payload.systemInstruction = {
      parts: [{ text: systemText.trim() }]
    };
  }

  return { model, payload };
}

function parseJsonBody(req) {
  return new Promise((resolve, reject) => {
    let data = '';
    req.on('data', chunk => {
      data += chunk;
      if (data.length > 10 * 1024 * 1024) { // 10MB limit
        reject(new Error('Payload too large'));
      }
    });
    req.on('end', () => {
      try {
        resolve(data ? JSON.parse(data) : {});
      } catch (err) {
        reject(new Error('Invalid JSON payload'));
      }
    });
    req.on('error', reject);
  });
}

const server = http.createServer(async (req, res) => {
  // CORS Headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  const url = new URL(req.url, `http://${req.headers.host}`);

  // Health check endpoint
  if (req.method === 'GET' && (url.pathname === '/health' || url.pathname === '/api/health')) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      status: 'ok',
      service: 'GameVault AI Secure Server',
      hasApiKey: Boolean(GEMINI_API_KEY),
      defaultModel: DEFAULT_MODEL
    }));
    return;
  }

  // Non-streaming Chat Endpoint
  if (req.method === 'POST' && (url.pathname === '/api/chat' || url.pathname === '/api/ai/chat')) {
    if (!GEMINI_API_KEY) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Server GEMINI_API_KEY is not configured.' }));
      return;
    }

    try {
      const body = await parseJsonBody(req);
      const { model, payload } = buildGeminiPayload(body);

      const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${GEMINI_API_KEY}`;
      const geminiRes = await fetch(geminiUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (!geminiRes.ok) {
        const errText = await geminiRes.text();
        console.error('Gemini API error:', geminiRes.status, errText);
        res.writeHead(geminiRes.status, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: `Gemini API error: ${geminiRes.status}`, details: errText }));
        return;
      }

      const data = await geminiRes.json();
      const text = data?.candidates?.[0]?.content?.parts
        ?.filter(p => !p.thought)
        ?.map(p => p.text)
        ?.join('') || '';

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        text: text,
        model: model,
        candidates: data.candidates
      }));
    } catch (err) {
      console.error('Error handling /api/chat:', err);
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: err.message || 'Internal Server Error' }));
    }
    return;
  }

  // Streaming Chat Endpoint (Server-Sent Events)
  if (req.method === 'POST' && (url.pathname === '/api/chat/stream' || url.pathname === '/api/ai/chat/stream')) {
    if (!GEMINI_API_KEY) {
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Server GEMINI_API_KEY is not configured.' }));
      return;
    }

    try {
      const body = await parseJsonBody(req);
      const { model, payload } = buildGeminiPayload(body);

      const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:streamGenerateContent?alt=sse&key=${GEMINI_API_KEY}`;
      const geminiRes = await fetch(geminiUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (!geminiRes.ok) {
        const errText = await geminiRes.text();
        console.error('Gemini stream error:', geminiRes.status, errText);
        res.writeHead(geminiRes.status, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: `Gemini stream error: ${geminiRes.status}`, details: errText }));
        return;
      }

      // Stream SSE to client
      res.writeHead(200, {
        'Content-Type': 'text/event-stream; charset=utf-8',
        'Cache-Control': 'no-cache, no-transform',
        'Connection': 'keep-alive',
        'X-Accel-Buffering': 'no'
      });

      const reader = geminiRes.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          const trimmed = line.trim();
          if (!trimmed) continue;
          if (trimmed.startsWith('data: ')) {
            try {
              const chunkJson = JSON.parse(trimmed.slice(6));
              const chunkText = chunkJson?.candidates?.[0]?.content?.parts
                ?.filter(p => !p.thought)
                ?.map(p => p.text)
                ?.join('') || '';

              if (chunkText) {
                res.write(`data: ${JSON.stringify({ text: chunkText })}\n\n`);
              }
            } catch (parseErr) {
              // Ignore partial or non-json data
            }
          }
        }
      }

      res.write('data: [DONE]\n\n');
      res.end();
    } catch (err) {
      console.error('Streaming error:', err);
      if (!res.headersSent) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: err.message || 'Stream processing failed' }));
      } else {
        res.write(`data: ${JSON.stringify({ error: err.message })}\n\n`);
        res.end();
      }
    }
    return;
  }

  // Fallback for unknown paths
  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ error: 'Endpoint not found', path: url.pathname }));
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`[GameVault AI Server] Listening on 0.0.0.0:${PORT} (Gemini Key present: ${Boolean(GEMINI_API_KEY)})`);
});
