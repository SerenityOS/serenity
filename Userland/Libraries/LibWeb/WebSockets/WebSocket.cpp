/*
 * Copyright (c) 2021-2022, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibWeb/Bindings/WebSocketWrapper.h>
#include <LibWeb/DOM/DOMException.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/DOM/IDLEventListener.h>
#include <LibWeb/HTML/CloseEvent.h>
#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/MessageEvent.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/WebSockets/WebSocket.h>

namespace Web::WebSockets {

static RefPtr<WebSocketClientManager> s_websocket_client_manager;

void WebSocketClientManager::initialize(RefPtr<WebSocketClientManager> websocket_client_manager)
{
    s_websocket_client_manager = websocket_client_manager;
}

WebSocketClientManager& WebSocketClientManager::the()
{
    if (!s_websocket_client_manager) [[unlikely]] {
        dbgln("Web::WebSockets::WebSocketClientManager was not initialized!");
        VERIFY_NOT_REACHED();
    }
    return *s_websocket_client_manager;
}

WebSocketClientSocket::WebSocketClientSocket() = default;

WebSocketClientSocket::~WebSocketClientSocket() = default;

WebSocketClientManager::WebSocketClientManager() = default;

// https://websockets.spec.whatwg.org/#dom-websocket-websocket
DOM::ExceptionOr<NonnullRefPtr<WebSocket>> WebSocket::create_with_global_object(Bindings::WindowObject& window, String const& url)
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

WebSocket::WebSocket(HTML::Window& window, AK::URL& url)
    : EventTarget()
    , m_window(window)
{
    // FIXME: Integrate properly with FETCH as per https://fetch.spec.whatwg.org/#websocket-opening-handshake
    auto origin_string = m_window->associated_document().origin().serialize();
    m_websocket = WebSocketClientManager::the().connect(url, origin_string);
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

WebSocket::~WebSocket() = default;

// https://websockets.spec.whatwg.org/#dom-websocket-readystate
WebSocket::ReadyState WebSocket::ready_state() const
{
    if (!m_websocket)
        return WebSocket::ReadyState::Closed;
    return const_cast<WebSocketClientSocket&>(*m_websocket).ready_state();
}

// https://websockets.spec.whatwg.org/#dom-websocket-extensions
String WebSocket::extensions() const
{
    if (!m_websocket)
        return String::empty();
    // https://websockets.spec.whatwg.org/#feedback-from-the-protocol
    // FIXME: Change the extensions attribute's value to the extensions in use, if it is not the null value.
    return String::empty();
}

// https://websockets.spec.whatwg.org/#dom-websocket-protocol
String WebSocket::protocol() const
{
    if (!m_websocket)
        return String::empty();
    // https://websockets.spec.whatwg.org/#feedback-from-the-protocol
    // FIXME: Change the protocol attribute's value to the subprotocol in use, if it is not the null value.
    return String::empty();
}

// https://websockets.spec.whatwg.org/#dom-websocket-close
DOM::ExceptionOr<void> WebSocket::close(Optional<u16> code, Optional<String> reason)
{
    // 1. If code is present, but is neither an integer equal to 1000 nor an integer in the range 3000 to 4999, inclusive, throw an "InvalidAccessError" DOMException.
    if (code.has_value() && *code != 1000 && (*code < 3000 || *code > 4099))
        return DOM::InvalidAccessError::create("The close error code is invalid");
    // 2. If reason is present, then run these substeps:
    if (reason.has_value()) {
        // 1. Let reasonBytes be the result of encoding reason.
        // 2. If reasonBytes is longer than 123 bytes, then throw a "SyntaxError" DOMException.
        if (reason->bytes().size() > 123)
            return DOM::SyntaxError::create("The close reason is longer than 123 bytes");
    }
    // 3. Run the first matching steps from the following list:
    auto state = ready_state();
    // -> If this's ready state is CLOSING (2) or CLOSED (3)
    if (state == WebSocket::ReadyState::Closing || state == WebSocket::ReadyState::Closed)
        return {};
    // -> If the WebSocket connection is not yet established [WSP]
    // -> If the WebSocket closing handshake has not yet been started [WSP]
    // -> Otherwise
    // NOTE: All of these are handled by the WebSocket Protocol when calling close()
    // FIXME: LibProtocol does not yet support sending empty Close messages, so we use default values for now
    m_websocket->close(code.value_or(1000), reason.value_or(String::empty()));
    return {};
}

// https://websockets.spec.whatwg.org/#dom-websocket-send
DOM::ExceptionOr<void> WebSocket::send(String const& data)
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

// https://websockets.spec.whatwg.org/#feedback-from-the-protocol
void WebSocket::on_open()
{
    // 1. Change the readyState attribute's value to OPEN (1).
    // 2. Change the extensions attribute's value to the extensions in use, if it is not the null value. [WSP]
    // 3. Change the protocol attribute's value to the subprotocol in use, if it is not the null value. [WSP]
    dispatch_event(*DOM::Event::create(*m_window->wrapper(), HTML::EventNames::open));
}

// https://websockets.spec.whatwg.org/#feedback-from-the-protocol
void WebSocket::on_error()
{
    dispatch_event(*DOM::Event::create(*m_window->wrapper(), HTML::EventNames::error));
}

// https://websockets.spec.whatwg.org/#feedback-from-the-protocol
void WebSocket::on_close(u16 code, String reason, bool was_clean)
{
    // 1. Change the readyState attribute's value to CLOSED. This is handled by the Protocol's WebSocket
    // 2. If [needed], fire an event named error at the WebSocket object. This is handled by the Protocol's WebSocket
    HTML::CloseEventInit event_init {};
    event_init.was_clean = was_clean;
    event_init.code = code;
    event_init.reason = move(reason);
    dispatch_event(*HTML::CloseEvent::create(*m_window->wrapper(), HTML::EventNames::close, event_init));
}

// https://websockets.spec.whatwg.org/#feedback-from-the-protocol
void WebSocket::on_message(ByteBuffer message, bool is_text)
{
    if (m_websocket->ready_state() != WebSocket::ReadyState::Open)
        return;
    if (is_text) {
        auto text_message = String(ReadonlyBytes(message));
        HTML::MessageEventInit event_init;
        event_init.data = JS::js_string(wrapper()->vm(), text_message);
        event_init.origin = url();
        dispatch_event(*HTML::MessageEvent::create(*m_window->wrapper(), HTML::EventNames::message, event_init));
        return;
    }

    if (m_binary_type == "blob") {
        // type indicates that the data is Binary and binaryType is "blob"
        TODO();
    } else if (m_binary_type == "arraybuffer") {
        // type indicates that the data is Binary and binaryType is "arraybuffer"
        auto& vm = wrapper()->vm();
        auto& realm = *vm.current_realm();
        HTML::MessageEventInit event_init;
        event_init.data = JS::ArrayBuffer::create(realm, message);
        event_init.origin = url();
        dispatch_event(*HTML::MessageEvent::create(*m_window->wrapper(), HTML::EventNames::message, event_init));
        return;
    }

    dbgln("Unsupported WebSocket message type {}", m_binary_type);
    TODO();
}

JS::Object* WebSocket::create_wrapper(JS::Realm& realm)
{
    return wrap(realm, *this);
}

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)                         \
    void WebSocket::set_##attribute_name(Bindings::CallbackType* value) \
    {                                                                   \
        set_event_handler_attribute(event_name, value);                 \
    }                                                                   \
    Bindings::CallbackType* WebSocket::attribute_name()                 \
    {                                                                   \
        return event_handler_attribute(event_name);                     \
    }
ENUMERATE_WEBSOCKET_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

}
