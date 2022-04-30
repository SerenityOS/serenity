/*
 * Copyright (c) 2021-2022, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/RefCounted.h>
#include <AK/URL.h>
#include <AK/Weakable.h>
#include <LibCore/Object.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/Forward.h>

#define ENUMERATE_WEBSOCKET_EVENT_HANDLERS(E) \
    E(onerror, HTML::EventNames::error)       \
    E(onclose, HTML::EventNames::close)       \
    E(onopen, HTML::EventNames::open)         \
    E(onmessage, HTML::EventNames::message)

namespace Web::WebSockets {

class WebSocketClientSocket;
class WebSocketClientManager;

class WebSocket final
    : public RefCounted<WebSocket>
    , public Weakable<WebSocket>
    , public DOM::EventTarget
    , public Bindings::Wrappable {
public:
    enum class ReadyState : u16 {
        Connecting = 0,
        Open = 1,
        Closing = 2,
        Closed = 3,
    };

    using WrapperType = Bindings::WebSocketWrapper;

    static NonnullRefPtr<WebSocket> create(HTML::Window& window, AK::URL& url)
    {
        return adopt_ref(*new WebSocket(window, url));
    }

    static DOM::ExceptionOr<NonnullRefPtr<WebSocket>> create_with_global_object(Bindings::WindowObject& window, String const& url);

    virtual ~WebSocket() override;

    using RefCounted::ref;
    using RefCounted::unref;

    String url() const { return m_url.to_string(); }

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)                  \
    void set_##attribute_name(Optional<Bindings::CallbackType>); \
    Bindings::CallbackType* attribute_name();
    ENUMERATE_WEBSOCKET_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

    ReadyState ready_state() const;
    String extensions() const;
    String protocol() const;

    String const& binary_type() { return m_binary_type; };
    void set_binary_type(String const& type) { m_binary_type = type; };

    DOM::ExceptionOr<void> close(Optional<u16> code, Optional<String> reason);
    DOM::ExceptionOr<void> send(String const& data);

private:
    virtual void ref_event_target() override { ref(); }
    virtual void unref_event_target() override { unref(); }
    virtual JS::Object* create_wrapper(JS::GlobalObject&) override;

    void on_open();
    void on_message(ByteBuffer message, bool is_text);
    void on_error();
    void on_close(u16 code, String reason, bool was_clean);

    explicit WebSocket(HTML::Window&, AK::URL&);

    NonnullRefPtr<HTML::Window> m_window;

    AK::URL m_url;
    String m_binary_type { "blob" };
    RefPtr<WebSocketClientSocket> m_websocket;
};

class WebSocketClientSocket : public RefCounted<WebSocketClientSocket> {
public:
    virtual ~WebSocketClientSocket();

    struct CertificateAndKey {
        String certificate;
        String key;
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

    virtual void send(ByteBuffer binary_or_text_message, bool is_text) = 0;
    virtual void send(StringView text_message) = 0;
    virtual void close(u16 code = 1005, String reason = {}) = 0;

    Function<void()> on_open;
    Function<void(Message)> on_message;
    Function<void(Error)> on_error;
    Function<void(u16 code, String reason, bool was_clean)> on_close;
    Function<CertificateAndKey()> on_certificate_requested;

protected:
    explicit WebSocketClientSocket();
};

class WebSocketClientManager : public Core::Object {
    C_OBJECT_ABSTRACT(WebSocketClientManager)
public:
    static void initialize(RefPtr<WebSocketClientManager>);
    static WebSocketClientManager& the();

    virtual RefPtr<WebSocketClientSocket> connect(AK::URL const&, String const& origin) = 0;

protected:
    explicit WebSocketClientManager();
};

}
