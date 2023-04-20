/*
 * Copyright (c) 2021-2022, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/URL.h>
#include <LibCore/Object.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

#define ENUMERATE_WEBSOCKET_EVENT_HANDLERS(E) \
    E(onerror, HTML::EventNames::error)       \
    E(onclose, HTML::EventNames::close)       \
    E(onopen, HTML::EventNames::open)         \
    E(onmessage, HTML::EventNames::message)

namespace Web::WebSockets {

class WebSocketClientSocket;
class WebSocketClientManager;

class WebSocket final : public DOM::EventTarget {
    WEB_PLATFORM_OBJECT(WebSocket, DOM::EventTarget);

public:
    enum class ReadyState : u16 {
        Connecting = 0,
        Open = 1,
        Closing = 2,
        Closed = 3,
    };

    static WebIDL::ExceptionOr<JS::NonnullGCPtr<WebSocket>> construct_impl(JS::Realm&, DeprecatedString const& url, Optional<Variant<DeprecatedString, Vector<DeprecatedString>>> const& protocols);

    virtual ~WebSocket() override;

    DeprecatedString url() const { return m_url.to_deprecated_string(); }

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)       \
    void set_##attribute_name(WebIDL::CallbackType*); \
    WebIDL::CallbackType* attribute_name();
    ENUMERATE_WEBSOCKET_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

    ReadyState ready_state() const;
    DeprecatedString extensions() const;
    DeprecatedString protocol() const;

    DeprecatedString const& binary_type() { return m_binary_type; };
    void set_binary_type(DeprecatedString const& type) { m_binary_type = type; };

    WebIDL::ExceptionOr<void> close(Optional<u16> code, Optional<DeprecatedString> reason);
    WebIDL::ExceptionOr<void> send(Variant<JS::Handle<JS::Object>, JS::Handle<FileAPI::Blob>, DeprecatedString> const& data);

private:
    void on_open();
    void on_message(ByteBuffer message, bool is_text);
    void on_error();
    void on_close(u16 code, DeprecatedString reason, bool was_clean);

    WebSocket(HTML::Window&, AK::URL&, Vector<DeprecatedString> const& protocols);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    JS::NonnullGCPtr<HTML::Window> m_window;

    AK::URL m_url;
    DeprecatedString m_binary_type { "blob" };
    RefPtr<WebSocketClientSocket> m_websocket;
};

class WebSocketClientSocket : public RefCounted<WebSocketClientSocket> {
public:
    virtual ~WebSocketClientSocket();

    struct CertificateAndKey {
        DeprecatedString certificate;
        DeprecatedString key;
    };

    struct Message {
        ByteBuffer data;
        bool is_text { false };
    };

    enum class Error {
        CouldNotEstablishConnection,
        ConnectionUpgradeFailed,
        ServerClosedSocket,
    };

    virtual Web::WebSockets::WebSocket::ReadyState ready_state() = 0;
    virtual DeprecatedString subprotocol_in_use() = 0;

    virtual void send(ByteBuffer binary_or_text_message, bool is_text) = 0;
    virtual void send(StringView text_message) = 0;
    virtual void close(u16 code = 1005, DeprecatedString reason = {}) = 0;

    Function<void()> on_open;
    Function<void(Message)> on_message;
    Function<void(Error)> on_error;
    Function<void(u16 code, DeprecatedString reason, bool was_clean)> on_close;
    Function<CertificateAndKey()> on_certificate_requested;

protected:
    explicit WebSocketClientSocket();
};

class WebSocketClientManager : public Core::Object {
    C_OBJECT_ABSTRACT(WebSocketClientManager)
public:
    static void initialize(RefPtr<WebSocketClientManager>);
    static WebSocketClientManager& the();

    virtual RefPtr<WebSocketClientSocket> connect(AK::URL const&, DeprecatedString const& origin, Vector<DeprecatedString> const& protocols) = 0;

protected:
    explicit WebSocketClientManager();
};

}
