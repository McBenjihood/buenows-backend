(function () {
  const currentScript =
    document.currentScript || document.querySelector('script[src*="widget.js"]');
  const rootId = currentScript?.dataset.root || "bws-chatbot";
  const widgetRegistry = window.BuenoWebsiteChatbots = window.BuenoWebsiteChatbots || {};
  widgetRegistry[rootId]?.destroy?.();
  const eventCleanups = [];
  const apiBase = getApiBase(currentScript);
  const widgetLanguage = normalizeLanguage(
    currentScript?.dataset.language || currentScript?.dataset.lang || ""
  ) || normalizeLanguage(document.documentElement.lang || "");
  const uiLanguage = widgetLanguage || "en";
  const UI_TEXT = {
    de: {
      subtitle: "Wie kann ich helfen?",
      welcomeMessage: "Hallo! Wie kann ich dir helfen?",
      placeholder: "Schreib deine Frage...",
      privacyNotice:
        "Bitte senden Sie keine Passwörter, Zahlungsdaten, Ausweise oder privaten Dokumente. Gespräche können bis zu 7 Tage gespeichert werden. Der Chatbot kann Fehler machen.",
      launcherLabel: "Chat öffnen",
      handoffLabel: "Kontakt aufnehmen",
      closeLabel: "Chat schliessen",
      inputLabel: "Nachricht",
      sendLabel: "Senden",
      typingLabel: "Schreibt...",
      configError: "Config konnte nicht geladen werden.",
      sessionError: "Session konnte nicht erstellt werden.",
      missingSession: "Session-ID fehlt.",
      chatError: "Chatbot Fehler.",
      unavailable: "Der Chatbot ist gerade nicht erreichbar.",
      fallbackReply: "Entschuldigung, ich konnte gerade keine Antwort erstellen.",
      retryPrefix: "Bitte versuchen Sie es in {seconds} erneut.",
      sessionEndedMessage:
        "Diese Chat-Session ist abgeschlossen. Für eine neue Anfrage starten Sie bitte einen neuen Chat.",
      newSessionLabel: "Neuen Chat starten",
      newSessionError: "Neue Session konnte nicht erstellt werden."
    },
    en: {
      subtitle: "How can I help?",
      welcomeMessage: "Hello! How can I help you?",
      placeholder: "Write your message...",
      privacyNotice:
        "Please do not send passwords, payment data, ID documents or private documents. Conversations may be stored for up to 7 days. The chatbot can make mistakes.",
      launcherLabel: "Open chat",
      handoffLabel: "Contact us",
      closeLabel: "Close chat",
      inputLabel: "Message",
      sendLabel: "Send",
      typingLabel: "Typing...",
      configError: "Config could not be loaded.",
      sessionError: "Session could not be created.",
      missingSession: "Session ID is missing.",
      chatError: "Chatbot error.",
      unavailable: "The chatbot is currently unavailable.",
      fallbackReply: "Sorry, I could not create a reply right now.",
      retryPrefix: "Please try again in {seconds}.",
      sessionEndedMessage:
        "This chat session is complete. Please start a new chat for another request.",
      newSessionLabel: "Start new chat",
      newSessionError: "A new session could not be created."
    }
  };
  const uiText = UI_TEXT[uiLanguage];
  const storageKey = `bws_chatbot_session_${hashString(`${apiBase}:${widgetLanguage || "default"}`)}`;

  let root = document.getElementById(rootId);

  if (!root) {
    root = document.createElement("div");
    root.id = rootId;
    document.body.appendChild(root);
  }

  const fallbackConfig = {
    botName: "Chat Assistant",
    companyName: "Website",
    subtitle: uiText.subtitle,
    welcomeMessage: uiText.welcomeMessage,
    placeholder: uiText.placeholder,
    privacyNotice: uiText.privacyNotice,
    maxMessageLength: 800,
    theme: {
      primaryColor: "#42b883",
      accentColor: "#42b883",
      launcherLabel: uiText.launcherLabel
    },
    handoff: {
      label: uiText.handoffLabel,
      url: ""
    },
    contact: {
      email: "",
      phone: "",
      website: ""
    }
  };

  let isOpen = false;
  let isLoading = false;
  let isConversationEnded = false;
  let config = fallbackConfig;
  let elements = null;

  widgetRegistry[rootId] = {
    destroy
  };

  loadPublicConfig()
    .then((loadedConfig) => {
      config = mergeConfig(fallbackConfig, loadedConfig);
    })
    .catch(() => {
      config = fallbackConfig;
    })
    .finally(init);

  function getApiBase(script) {
    const explicitBase = script?.dataset.apiBase;

    if (explicitBase) {
      return explicitBase.replace(/\/$/, "");
    }

    if (script?.src) {
      return new URL(script.src, window.location.href).origin;
    }

    return window.location.origin;
  }

  function hashString(value) {
    let hash = 0;

    for (let index = 0; index < value.length; index += 1) {
      hash = (hash << 5) - hash + value.charCodeAt(index);
      hash |= 0;
    }

    return Math.abs(hash).toString(36);
  }

  function normalizeLanguage(value) {
    const normalized = String(value || "").trim().toLowerCase();

    return normalized === "de" || normalized === "en" ? normalized : "";
  }

  async function loadPublicConfig() {
    const configUrl = widgetLanguage
      ? `${apiBase}/api/chatbot/config?language=${encodeURIComponent(widgetLanguage)}`
      : `${apiBase}/api/chatbot/config`;
    const response = await fetch(configUrl, {
      headers: {
        Accept: "application/json"
      }
    });

    if (!response.ok) {
      throw new Error(uiText.configError);
    }

    return response.json();
  }

  function mergeConfig(baseConfig, loadedConfig) {
    return {
      ...baseConfig,
      ...loadedConfig,
      theme: {
        ...baseConfig.theme,
        ...(loadedConfig.theme || {})
      },
      handoff: {
        ...baseConfig.handoff,
        ...(loadedConfig.handoff || {})
      },
      contact: {
        ...baseConfig.contact,
        ...(loadedConfig.contact || {})
      }
    };
  }

  function init() {
    root.classList.add("bws-chatbot-root");
    root.style.setProperty("--bws-primary", config.theme.primaryColor);
    root.style.setProperty("--bws-accent", config.theme.accentColor);
    root.innerHTML = "";

    elements = createWidget(config);
    bindEvents();
  }

  function createWidget(widgetConfig) {
    const launcher = document.createElement("button");
    launcher.className = "bws-chat-launcher";
    launcher.type = "button";
    launcher.setAttribute("aria-label", widgetConfig.theme.launcherLabel);
    launcher.setAttribute("aria-expanded", "false");
    launcher.innerHTML = `
      <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M7.5 9.5h9M7.5 13h5.5M21 11.5c0 4.142-4.03 7.5-9 7.5a10.7 10.7 0 0 1-3.59-.61L3 20l1.62-4.05C3.6 14.7 3 13.18 3 11.5 3 7.358 7.03 4 12 4s9 3.358 9 7.5Z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    `;

    const panel = document.createElement("section");
    panel.className = "bws-chat-panel";
    panel.setAttribute("aria-label", `${widgetConfig.companyName} Chatbot`);
    panel.setAttribute("aria-hidden", "true");

    const header = document.createElement("header");
    header.className = "bws-chat-header";

    const identity = document.createElement("div");
    identity.className = "bws-chat-identity";

    const statusDot = document.createElement("span");
    statusDot.className = "bws-status-dot";
    statusDot.setAttribute("aria-hidden", "true");

    const headingGroup = document.createElement("div");

    const title = document.createElement("h2");
    title.className = "bws-chat-title";
    title.textContent = widgetConfig.botName;

    const headerNotice = document.createElement("p");
    headerNotice.className = "bws-chat-header-notice";
    headerNotice.textContent = widgetConfig.privacyNotice;

    headingGroup.append(title, headerNotice);
    identity.append(statusDot, headingGroup);

    const closeButton = document.createElement("button");
    closeButton.className = "bws-close-btn";
    closeButton.type = "button";
    closeButton.setAttribute("aria-label", uiText.closeLabel);
    closeButton.textContent = "×";

    header.append(identity, closeButton);

    const messages = document.createElement("div");
    messages.className = "bws-chat-messages";
    messages.setAttribute("aria-live", "polite");

    const footer = document.createElement("div");
    footer.className = "bws-chat-footer";

    if (widgetConfig.handoff.url) {
      const contactLink = document.createElement("a");
      contactLink.className = "bws-contact-link";
      contactLink.href = widgetConfig.handoff.url;
      contactLink.textContent = widgetConfig.handoff.label;
      contactLink.rel = "noopener noreferrer";

      if (shouldOpenInNewTab(widgetConfig.handoff.url)) {
        contactLink.target = "_blank";
      }

      footer.appendChild(contactLink);
    }

    const form = document.createElement("form");
    form.className = "bws-chat-form";

    const label = document.createElement("label");
    label.className = "bws-sr-only";
    label.setAttribute("for", `${rootId}-input`);
    label.textContent = uiText.inputLabel;

    const input = document.createElement("input");
    input.id = `${rootId}-input`;
    input.className = "bws-chat-input";
    input.type = "text";
    input.maxLength = widgetConfig.maxMessageLength || 800;
    input.placeholder = widgetConfig.placeholder;
    input.autocomplete = "off";

    const sendButton = document.createElement("button");
    sendButton.className = "bws-send-btn";
    sendButton.type = "submit";
    sendButton.textContent = uiText.sendLabel;

    form.append(label, input, sendButton);
    footer.appendChild(form);
    panel.append(header, messages, footer);
    root.append(launcher, panel);

    return {
      launcher,
      panel,
      closeButton,
      messages,
      form,
      input,
      sendButton
    };
  }

  function bindEvents() {
    listen(elements.launcher, "click", () => toggleChat());
    listen(elements.closeButton, "click", () => toggleChat(false));

    listen(document, "keydown", (event) => {
      if (event.key === "Escape" && isOpen) {
        toggleChat(false);
      }
    });

    listen(elements.form, "submit", async (event) => {
      event.preventDefault();

      const text = elements.input.value.trim();

      if (!text || isLoading) {
        return;
      }

      elements.input.value = "";
      addMessage(text, "user");

      const typing = addTyping();
      setLoading(true);

      try {
        const result = await sendMessage(text);
        typing.remove();
        addMessage(result.reply, "bot");
        if (result.sessionEnded) {
          showSessionEndedNotice();
        }
      } catch (error) {
        typing.remove();
        addMessage(getDisplayErrorMessage(error), "bot");
      } finally {
        setLoading(false);
        elements.input.focus();
      }
    });
  }

  function getSessionId() {
    try {
      return window.localStorage.getItem(storageKey);
    } catch {
      return "";
    }
  }

  function setSessionId(sessionId) {
    try {
      window.localStorage.setItem(storageKey, sessionId);
    } catch {
      // Chat still works without persistent browser storage.
    }
  }

  function clearSessionId() {
    try {
      window.localStorage.removeItem(storageKey);
    } catch {
      // Chat still works without persistent browser storage.
    }
  }

  function scrollToBottom() {
    elements.messages.scrollTop = elements.messages.scrollHeight;
  }

  function addMessage(text, sender) {
    const bubble = document.createElement("div");
    bubble.className = `bws-message ${sender}`;
    appendTextWithLinks(bubble, text);
    elements.messages.appendChild(bubble);
    scrollToBottom();
    return bubble;
  }

  function appendTextWithLinks(container, text) {
    const value = String(text || "");
    const urlPattern = /(https?:\/\/[^\s<>()]+|www\.[^\s<>()]+)/gi;
    let lastIndex = 0;
    let match = urlPattern.exec(value);

    while (match) {
      const rawUrl = match[0];
      const matchStart = match.index;
      const matchEnd = matchStart + rawUrl.length;
      const trimmedUrl = rawUrl.replace(/[.,;:!?]+$/g, "");
      const trailingText = rawUrl.slice(trimmedUrl.length);

      if (matchStart > lastIndex) {
        container.appendChild(document.createTextNode(value.slice(lastIndex, matchStart)));
      }

      const link = document.createElement("a");
      link.href = trimmedUrl.startsWith("http") ? trimmedUrl : `https://${trimmedUrl}`;
      link.textContent = trimmedUrl;
      link.rel = "noopener noreferrer";
      if (shouldOpenInNewTab(link.href)) {
        link.target = "_blank";
      }
      container.appendChild(link);

      if (trailingText) {
        container.appendChild(document.createTextNode(trailingText));
      }

      lastIndex = matchEnd;
      match = urlPattern.exec(value);
    }

    if (lastIndex < value.length) {
      container.appendChild(document.createTextNode(value.slice(lastIndex)));
    }
  }

  function addTyping() {
    const typing = document.createElement("div");
    typing.className = "bws-typing";
    typing.textContent = uiText.typingLabel;
    elements.messages.appendChild(typing);
    scrollToBottom();
    return typing;
  }

  function setLoading(value) {
    isLoading = value;
    elements.input.disabled = value || isConversationEnded;
    elements.sendButton.disabled = value || isConversationEnded;
  }

  function toggleChat(nextState) {
    isOpen = typeof nextState === "boolean" ? nextState : !isOpen;

    elements.panel.classList.toggle("is-open", isOpen);
    elements.panel.setAttribute("aria-hidden", String(!isOpen));
    elements.launcher.setAttribute("aria-expanded", String(isOpen));

    if (isOpen) {
      elements.input.focus();

      if (!elements.messages.dataset.started) {
        addMessage(config.welcomeMessage, "bot");
        elements.messages.dataset.started = "true";
      }
    }
  }

  async function ensureSession() {
    const existingSessionId = getSessionId();

    if (existingSessionId) {
      return existingSessionId;
    }

    return createSession();
  }

  async function createSession() {
    const response = await fetch(`${apiBase}/api/chatbot/session`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(widgetLanguage ? { language: widgetLanguage } : {})
    });

    const data = await parseJsonResponse(response);

    if (!response.ok) {
      throw new Error(formatApiError(data, uiText.sessionError));
    }

    if (!data.sessionId) {
      throw new Error(uiText.missingSession);
    }

    setSessionId(data.sessionId);
    return data.sessionId;
  }

  async function sendMessage(text) {
    const sessionId = await ensureSession();

    const response = await fetch(`${apiBase}/api/chatbot/chat`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        message: text,
        sessionId,
        ...(widgetLanguage ? { language: widgetLanguage } : {})
      })
    });

    const data = await parseJsonResponse(response);

    if (!response.ok) {
      throw new Error(formatApiError(data, uiText.chatError));
    }

    if (data.sessionEnded) {
      clearSessionId();
    } else if (data.sessionId) {
      setSessionId(data.sessionId);
    }

    return {
      reply: data.reply || uiText.fallbackReply,
      sessionEnded: Boolean(data.sessionEnded)
    };
  }

  function showSessionEndedNotice() {
    isConversationEnded = true;
    setLoading(false);

    const notice = document.createElement("div");
    notice.className = "bws-session-ended";

    const message = document.createElement("p");
    message.textContent = uiText.sessionEndedMessage;

    const button = document.createElement("button");
    button.className = "bws-new-session-btn";
    button.type = "button";
    button.textContent = uiText.newSessionLabel;
    button.addEventListener("click", startNewSession);

    notice.append(message, button);
    elements.messages.appendChild(notice);
    scrollToBottom();
  }

  async function startNewSession() {
    if (isLoading) {
      return;
    }

    clearSessionId();
    isConversationEnded = false;
    elements.messages.innerHTML = "";
    elements.messages.dataset.started = "";
    setLoading(true);

    try {
      await createSession();
      addMessage(config.welcomeMessage, "bot");
      elements.messages.dataset.started = "true";
    } catch (error) {
      addMessage(getDisplayErrorMessage(error) || uiText.newSessionError, "bot");
    } finally {
      setLoading(false);
      elements.input.focus();
    }
  }

  async function parseJsonResponse(response) {
    try {
      return await response.json();
    } catch {
      return {};
    }
  }

  function formatApiError(data, fallbackMessage) {
    const message = data?.error || fallbackMessage;
    const retryAfter = Number(data?.retryAfter);

    if (!Number.isFinite(retryAfter) || retryAfter <= 0) {
      return message;
    }

    return `${message} ${uiText.retryPrefix.replace("{seconds}", formatRetryAfter(retryAfter))}`;
  }

  function getDisplayErrorMessage(error) {
    const message = String(error?.message || "");

    if (!message || /failed to fetch|networkerror|load failed/i.test(message)) {
      return uiText.unavailable;
    }

    return message;
  }

  function formatRetryAfter(seconds) {
    const roundedSeconds = Math.max(1, Math.ceil(seconds));

    if (roundedSeconds < 60) {
      return uiLanguage === "de"
        ? `${roundedSeconds} Sekunden`
        : `${roundedSeconds} seconds`;
    }

    const minutes = Math.ceil(roundedSeconds / 60);
    return uiLanguage === "de"
      ? `${minutes} Minuten`
      : `${minutes} minutes`;
  }

  function listen(target, eventName, handler) {
    target.addEventListener(eventName, handler);
    eventCleanups.push(() => target.removeEventListener(eventName, handler));
  }

  function destroy() {
    for (const cleanup of eventCleanups.splice(0)) {
      cleanup();
    }

    if (root) {
      root.innerHTML = "";
    }

    if (widgetRegistry[rootId]?.destroy === destroy) {
      delete widgetRegistry[rootId];
    }
  }

  function shouldOpenInNewTab(url) {
    const value = String(url || "");

    if (!value || value.startsWith("mailto:") || value.startsWith("tel:")) {
      return false;
    }

    try {
      return new URL(value, window.location.href).origin !== window.location.origin;
    } catch {
      return false;
    }
  }
})();
