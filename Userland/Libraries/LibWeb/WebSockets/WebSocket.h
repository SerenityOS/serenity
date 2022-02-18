/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
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

namespace Protocol {
class WebSocketClient;
class WebSocket;
}

namespace Web::WebSockets {

class WebSocketClientManager : public Core::Object {
    C_OBJECT_ABSTRACT(WebSocketClientManager)
public:
    static WebSocketClientManager& the();

    RefPtr<Protocol::WebSocket> connect(const AK::URL&);

private:
    static ErrorOr<NonnullRefPtr<WebSocketClientManager>> try_create();
    WebSocketClientManager(NonnullRefPtr<Protocol::WebSocketClient>);

    RefPtr<Protocol::WebSocketClient> m_websocket_client;
};

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

    static NonnullRefPtr<WebSocket> create(DOM::Window& window, AK::URL& url)
    {
        return adopt_ref(*new WebSocket(window, url));
    }

    static DOM::ExceptionOr<NonnullRefPtr<WebSocket>> create_with_global_object(Bindings::WindowObject& window, const String& url);

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

    const String& binary_type() { return m_binary_type; };
    void set_binary_type(const String& type) { m_binary_type = type; };

    DOM::ExceptionOr<void> close(u16 code, const String& reason);
    DOM::ExceptionOr<void> send(const String& data);

private:
    virtual void ref_event_target() override { ref(); }
    virtual void unref_event_target() override { unref(); }
    virtual JS::Object* create_wrapper(JS::GlobalObject&) override;

    void on_open();
    void on_message(ByteBuffer message, bool is_text);
    void on_error();
    void on_close(u16 code, String reason, bool was_clean);

    explicit WebSocket(DOM::Window&, AK::URL&);

    NonnullRefPtr<DOM::Window> m_window;

    AK::URL m_url;
    String m_binary_type { "blob" };
    RefPtr<Protocol::WebSocket> m_websocket;
};

}
