/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibProtocol/WebSocket.h>
#include <LibProtocol/WebSocketClient.h>
#include <LibWeb/Bindings/EventWrapper.h>
#include <LibWeb/Bindings/WebSocketWrapper.h>
#include <LibWeb/DOM/DOMException.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/HTML/CloseEvent.h>
#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/MessageEvent.h>
#include <LibWeb/HTML/WebSocket.h>
#include <LibWeb/Origin.h>

namespace Web::HTML {

WebSocketClientManager& WebSocketClientManager::the()
{
    static WebSocketClientManager* s_the;
    if (!s_the)
        s_the = &WebSocketClientManager::construct().leak_ref();
    return *s_the;
}

WebSocketClientManager::WebSocketClientManager()
    : m_websocket_client(Protocol::WebSocketClient::construct())
{
}

RefPtr<Protocol::WebSocket> WebSocketClientManager::connect(const AK::URL& url)
{
    return m_websocket_client->connect(url);
}

// https://html.spec.whatwg.org/multipage/web-sockets.html#the-websocket-interface
DOM::ExceptionOr<NonnullRefPtr<WebSocket>> WebSocket::create_with_global_object(Bindings::WindowObject& window, const String& url)
{
    AK::URL url_record(url);
    if (!url_record.is_valid())
        return DOM::SyntaxError::create("Invalid URL");
    if (!url_record.protocol().is_one_of("ws", "wss"))
        return DOM::SyntaxError::create("Invalid protocol");
    if (!url_record.fragment().is_empty())
        return DOM::SyntaxError::create("Presence of URL fragment is invalid");
    // 5. If `protocols` is a string, set `protocols` to a sequence consisting of just that string
    // 6. If any of the values in `protocols` occur more than once or otherwise fail to match the requirements, throw SyntaxError
    return WebSocket::create(window.impl(), url_record);
}

WebSocket::WebSocket(DOM::Window& window, AK::URL& url)
    : EventTarget(static_cast<Bindings::ScriptExecutionContext&>(window.associated_document()))
    , m_window(window)
{
    // FIXME: Integrate properly with FETCH as per https://fetch.spec.whatwg.org/#websocket-opening-handshake
    m_websocket = WebSocketClientManager::the().connect(url);
    m_websocket->on_open = [weak_this = make_weak_ptr()] {
        if (!weak_this)
            return;
        auto& websocket = const_cast<WebSocket&>(*weak_this);
        websocket.on_open();
    };
    m_websocket->on_message = [weak_this = make_weak_ptr()](auto message) {
        if (!weak_this)
            return;
        auto& websocket = const_cast<WebSocket&>(*weak_this);
        websocket.on_message(move(message.data), message.is_text);
    };
    m_websocket->on_close = [weak_this = make_weak_ptr()](auto code, auto reason, bool was_clean) {
        if (!weak_this)
            return;
        auto& websocket = const_cast<WebSocket&>(*weak_this);
        websocket.on_close(code, reason, was_clean);
    };
    m_websocket->on_error = [weak_this = make_weak_ptr()](auto) {
        if (!weak_this)
            return;
        auto& websocket = const_cast<WebSocket&>(*weak_this);
        websocket.on_error();
    };
}

WebSocket::~WebSocket()
{
}

// https://html.spec.whatwg.org/multipage/web-sockets.html#the-websocket-interface
WebSocket::ReadyState WebSocket::ready_state() const
{
    if (!m_websocket)
        return WebSocket::ReadyState::Closed;
    auto ready_state = const_cast<Protocol::WebSocket&>(*m_websocket).ready_state();
    switch (ready_state) {
    case Protocol::WebSocket::ReadyState::Connecting:
        return WebSocket::ReadyState::Connecting;
    case Protocol::WebSocket::ReadyState::Open:
        return WebSocket::ReadyState::Open;
    case Protocol::WebSocket::ReadyState::Closing:
        return WebSocket::ReadyState::Closing;
    case Protocol::WebSocket::ReadyState::Closed:
        return WebSocket::ReadyState::Closed;
    }
    return WebSocket::ReadyState::Closed;
}

// https://html.spec.whatwg.org/multipage/web-sockets.html#the-websocket-interface
String WebSocket::extensions() const
{
    if (!m_websocket)
        return String::empty();
    // https://html.spec.whatwg.org/multipage/web-sockets.html#feedback-from-the-protocol
    // FIXME: Change the extensions attribute's value to the extensions in use, if it is not the null value.
    return String::empty();
}

// https://html.spec.whatwg.org/multipage/web-sockets.html#the-websocket-interface
String WebSocket::protocol() const
{
    if (!m_websocket)
        return String::empty();
    // https://html.spec.whatwg.org/multipage/web-sockets.html#feedback-from-the-protocol
    // FIXME: Change the protocol attribute's value to the subprotocol in use, if it is not the null value.
    return String::empty();
}

// https://html.spec.whatwg.org/multipage/web-sockets.html#the-websocket-interface
DOM::ExceptionOr<void> WebSocket::close(u16 code, const String& reason)
{
    // HACK : we should have an Optional<u16>
    if (code == 0)
        code = 1000;
    if (code != 1000 && (code < 3000 || code > 4099))
        return DOM::InvalidAccessError::create("The close error code is invalid");
    if (!reason.is_empty() && reason.bytes().size() > 123)
        return DOM::SyntaxError::create("The close reason is longer than 123 bytes");
    auto state = ready_state();
    if (state == WebSocket::ReadyState::Closing || state == WebSocket::ReadyState::Closed)
        return {};
    // Note : Both of these are handled by the WebSocket Protocol when calling close()
    // 3b. If the WebSocket connection is not yet established [WSP]
    // 3c. If the WebSocket closing handshake has not yet been started [WSP]
    m_websocket->close(code, reason);
    return {};
}

// https://html.spec.whatwg.org/multipage/web-sockets.html#the-websocket-interface
DOM::ExceptionOr<void> WebSocket::send(const String& data)
{
    auto state = ready_state();
    if (state == WebSocket::ReadyState::Connecting)
        return DOM::InvalidStateError::create("Websocket is still CONNECTING");
    if (state == WebSocket::ReadyState::Open) {
        m_websocket->send(data);
        // TODO : If the data cannot be sent, e.g. because it would need to be buffered but the buffer is full, the user agent must flag the WebSocket as full and then close the WebSocket connection.
        // TODO : Any invocation of this method with a string argument that does not throw an exception must increase the bufferedAmount attribute by the number of bytes needed to express the argument as UTF-8.
    }
    return {};
}

// https://html.spec.whatwg.org/multipage/web-sockets.html#feedback-from-the-protocol
void WebSocket::on_open()
{
    // 1. Change the readyState attribute's value to OPEN (1).
    // 2. Change the extensions attribute's value to the extensions in use, if it is not the null value. [WSP]
    // 3. Change the protocol attribute's value to the subprotocol in use, if it is not the null value. [WSP]
    dispatch_event(DOM::Event::create(EventNames::open));
}

// https://html.spec.whatwg.org/multipage/web-sockets.html#feedback-from-the-protocol
void WebSocket::on_error()
{
    dispatch_event(DOM::Event::create(EventNames::error));
}

// https://html.spec.whatwg.org/multipage/web-sockets.html#feedback-from-the-protocol
void WebSocket::on_close(u16 code, String reason, bool was_clean)
{
    // 1. Change the readyState attribute's value to CLOSED. This is handled by the Protocol's WebSocket
    // 2. If [needed], fire an event named error at the WebSocket object. This is handled by the Protocol's WebSocket
    CloseEventInit event_init {};
    event_init.was_clean = was_clean;
    event_init.code = code;
    event_init.reason = move(reason);
    dispatch_event(CloseEvent::create(EventNames::close, event_init));
}

// https://html.spec.whatwg.org/multipage/web-sockets.html#feedback-from-the-protocol
void WebSocket::on_message(ByteBuffer message, bool is_text)
{
    if (m_websocket->ready_state() != Protocol::WebSocket::ReadyState::Open)
        return;
    if (is_text) {
        auto text_message = String(ReadonlyBytes(message));
        MessageEventInit event_init {};
        event_init.data = JS::js_string(wrapper()->vm(), text_message);
        event_init.origin = url();
        dispatch_event(MessageEvent::create(EventNames::message, event_init));
        return;
    }
    // type indicates that the data is Binary and binaryType is "blob"
    // type indicates that the data is Binary and binaryType is "arraybuffer"
    TODO();
}

JS::Object* WebSocket::create_wrapper(JS::GlobalObject& global_object)
{
    return wrap(global_object, *this);
}

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)                    \
    void WebSocket::set_##attribute_name(HTML::EventHandler value) \
    {                                                              \
        set_event_handler_attribute(event_name, move(value));      \
    }                                                              \
    HTML::EventHandler WebSocket::attribute_name()                 \
    {                                                              \
        return event_handler_attribute(event_name);                \
    }
ENUMERATE_WEBSOCKET_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

}
