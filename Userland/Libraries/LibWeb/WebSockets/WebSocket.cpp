/*
 * Copyright (c) 2021-2022, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibJS/Runtime/ArrayBuffer.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/IDLEventListener.h>
#include <LibWeb/FileAPI/Blob.h>
#include <LibWeb/HTML/CloseEvent.h>
#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/MessageEvent.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/WebIDL/AbstractOperations.h>
#include <LibWeb/WebIDL/DOMException.h>
#include <LibWeb/WebIDL/ExceptionOr.h>
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
WebIDL::ExceptionOr<JS::NonnullGCPtr<WebSocket>> WebSocket::construct_impl(JS::Realm& realm, String const& url, Optional<Variant<String, Vector<String>>> const& protocols)
{
    auto& window = verify_cast<HTML::Window>(realm.global_object());
    AK::URL url_record(url);
    if (!url_record.is_valid())
        return WebIDL::SyntaxError::create(realm, "Invalid URL");
    if (!url_record.scheme().is_one_of("ws", "wss"))
        return WebIDL::SyntaxError::create(realm, "Invalid protocol");
    if (!url_record.fragment().is_empty())
        return WebIDL::SyntaxError::create(realm, "Presence of URL fragment is invalid");
    Vector<String> protocols_sequence;
    if (protocols.has_value()) {
        // 5. If `protocols` is a string, set `protocols` to a sequence consisting of just that string
        if (protocols.value().has<String>())
            protocols_sequence = { protocols.value().get<String>() };
        else
            protocols_sequence = protocols.value().get<Vector<String>>();
        // 6. If any of the values in `protocols` occur more than once or otherwise fail to match the requirements, throw SyntaxError
        auto sorted_protocols = protocols_sequence;
        quick_sort(sorted_protocols);
        for (size_t i = 0; i < sorted_protocols.size(); i++) {
            // https://datatracker.ietf.org/doc/html/rfc6455
            // The elements that comprise this value MUST be non-empty strings with characters in the range U+0021 to U+007E not including
            // separator characters as defined in [RFC2616] and MUST all be unique strings.
            auto protocol = sorted_protocols[i];
            if (i < sorted_protocols.size() - 1 && protocol == sorted_protocols[i + 1])
                return WebIDL::SyntaxError::create(realm, "Found a duplicate protocol name in the specified list");
            for (auto character : protocol.code_points()) {
                if (character < '\x21' || character > '\x7E')
                    return WebIDL::SyntaxError::create(realm, "Found invalid character in subprotocol name");
            }
        }
    }
    return MUST_OR_THROW_OOM(realm.heap().allocate<WebSocket>(realm, window, url_record, protocols_sequence));
}

WebSocket::WebSocket(HTML::Window& window, AK::URL& url, Vector<String> const& protocols)
    : EventTarget(window.realm())
    , m_window(window)
{
    // FIXME: Integrate properly with FETCH as per https://fetch.spec.whatwg.org/#websocket-opening-handshake
    auto origin_string = m_window->associated_document().origin().serialize();
    Vector<DeprecatedString> protcol_deprecated_strings;
    for (auto protocol : protocols)
        protcol_deprecated_strings.append(protocol.to_deprecated_string());
    m_websocket = WebSocketClientManager::the().connect(url, origin_string, protcol_deprecated_strings);
    m_websocket->on_open = [weak_this = make_weak_ptr<WebSocket>()] {
        if (!weak_this)
            return;
        auto& websocket = const_cast<WebSocket&>(*weak_this);
        websocket.on_open();
    };
    m_websocket->on_message = [weak_this = make_weak_ptr<WebSocket>()](auto message) {
        if (!weak_this)
            return;
        auto& websocket = const_cast<WebSocket&>(*weak_this);
        websocket.on_message(move(message.data), message.is_text);
    };
    m_websocket->on_close = [weak_this = make_weak_ptr<WebSocket>()](auto code, auto reason, bool was_clean) {
        if (!weak_this)
            return;
        auto& websocket = const_cast<WebSocket&>(*weak_this);
        websocket.on_close(code, String::from_deprecated_string(reason).release_value_but_fixme_should_propagate_errors(), was_clean);
    };
    m_websocket->on_error = [weak_this = make_weak_ptr<WebSocket>()](auto) {
        if (!weak_this)
            return;
        auto& websocket = const_cast<WebSocket&>(*weak_this);
        websocket.on_error();
    };
}

WebSocket::~WebSocket() = default;

JS::ThrowCompletionOr<void> WebSocket::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::WebSocketPrototype>(realm, "WebSocket"));

    return {};
}

void WebSocket::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_window.ptr());
}

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
        return String {};
    // https://websockets.spec.whatwg.org/#feedback-from-the-protocol
    // FIXME: Change the extensions attribute's value to the extensions in use, if it is not the null value.
    return String {};
}

// https://websockets.spec.whatwg.org/#dom-websocket-protocol
WebIDL::ExceptionOr<String> WebSocket::protocol() const
{
    if (!m_websocket)
        return String {};
    return TRY_OR_THROW_OOM(vm(), String::from_deprecated_string(m_websocket->subprotocol_in_use()));
}

// https://websockets.spec.whatwg.org/#dom-websocket-close
WebIDL::ExceptionOr<void> WebSocket::close(Optional<u16> code, Optional<String> reason)
{
    // 1. If code is present, but is neither an integer equal to 1000 nor an integer in the range 3000 to 4999, inclusive, throw an "InvalidAccessError" DOMException.
    if (code.has_value() && *code != 1000 && (*code < 3000 || *code > 4099))
        return WebIDL::InvalidAccessError::create(realm(), "The close error code is invalid");
    // 2. If reason is present, then run these substeps:
    if (reason.has_value()) {
        // 1. Let reasonBytes be the result of encoding reason.
        // 2. If reasonBytes is longer than 123 bytes, then throw a "SyntaxError" DOMException.
        if (reason->bytes().size() > 123)
            return WebIDL::SyntaxError::create(realm(), "The close reason is longer than 123 bytes");
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
    m_websocket->close(code.value_or(1000), reason.value_or(String {}).to_deprecated_string());
    return {};
}

// https://websockets.spec.whatwg.org/#dom-websocket-send
WebIDL::ExceptionOr<void> WebSocket::send(Variant<JS::Handle<JS::Object>, JS::Handle<FileAPI::Blob>, String> const& data)
{
    auto state = ready_state();
    if (state == WebSocket::ReadyState::Connecting)
        return WebIDL::InvalidStateError::create(realm(), "Websocket is still CONNECTING");
    if (state == WebSocket::ReadyState::Open) {
        TRY_OR_THROW_OOM(vm(),
            data.visit(
                [this](String const& string) -> ErrorOr<void> {
                    m_websocket->send(string);
                    return {};
                },
                [this](JS::Handle<JS::Object> const& buffer_source) -> ErrorOr<void> {
                    // FIXME: While the spec doesn't say to do this, it's not observable except from potentially throwing OOM.
                    //        Can we avoid this copy?
                    auto data_buffer = TRY(WebIDL::get_buffer_source_copy(*buffer_source.cell()));
                    m_websocket->send(data_buffer, false);
                    return {};
                },
                [this](JS::Handle<FileAPI::Blob> const& blob) -> ErrorOr<void> {
                    auto byte_buffer = TRY(ByteBuffer::copy(blob->bytes()));
                    m_websocket->send(byte_buffer, false);
                    return {};
                }));
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
    dispatch_event(DOM::Event::create(realm(), HTML::EventNames::open).release_value_but_fixme_should_propagate_errors());
}

// https://websockets.spec.whatwg.org/#feedback-from-the-protocol
void WebSocket::on_error()
{
    dispatch_event(DOM::Event::create(realm(), HTML::EventNames::error).release_value_but_fixme_should_propagate_errors());
}

// https://websockets.spec.whatwg.org/#feedback-from-the-protocol
void WebSocket::on_close(u16 code, String reason, bool was_clean)
{
    // 1. Change the readyState attribute's value to CLOSED. This is handled by the Protocol's WebSocket
    // 2. If [needed], fire an event named error at the WebSocket object. This is handled by the Protocol's WebSocket
    HTML::CloseEventInit event_init {};
    event_init.was_clean = was_clean;
    event_init.code = code;
    event_init.reason = reason;
    dispatch_event(HTML::CloseEvent::create(realm(), HTML::EventNames::close, event_init).release_value_but_fixme_should_propagate_errors());
}

// https://websockets.spec.whatwg.org/#feedback-from-the-protocol
void WebSocket::on_message(ByteBuffer message, bool is_text)
{
    if (m_websocket->ready_state() != WebSocket::ReadyState::Open)
        return;
    if (is_text) {
        auto text_message = DeprecatedString(ReadonlyBytes(message));
        HTML::MessageEventInit event_init;
        event_init.data = JS::PrimitiveString::create(vm(), text_message);
        event_init.origin = url().release_value_but_fixme_should_propagate_errors();
        dispatch_event(HTML::MessageEvent::create(realm(), HTML::EventNames::message, event_init).release_value_but_fixme_should_propagate_errors());
        return;
    }

    if (m_binary_type == "blob") {
        // type indicates that the data is Binary and binaryType is "blob"
        TODO();
    } else if (m_binary_type == "arraybuffer") {
        // type indicates that the data is Binary and binaryType is "arraybuffer"
        HTML::MessageEventInit event_init;
        event_init.data = JS::ArrayBuffer::create(realm(), message);
        event_init.origin = url().release_value_but_fixme_should_propagate_errors();
        dispatch_event(HTML::MessageEvent::create(realm(), HTML::EventNames::message, event_init).release_value_but_fixme_should_propagate_errors());
        return;
    }

    dbgln("Unsupported WebSocket message type {}", m_binary_type);
    TODO();
}

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)                       \
    void WebSocket::set_##attribute_name(WebIDL::CallbackType* value) \
    {                                                                 \
        set_event_handler_attribute(event_name, value);               \
    }                                                                 \
    WebIDL::CallbackType* WebSocket::attribute_name()                 \
    {                                                                 \
        return event_handler_attribute(event_name);                   \
    }
ENUMERATE_WEBSOCKET_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

}
