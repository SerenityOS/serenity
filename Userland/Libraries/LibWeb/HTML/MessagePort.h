/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

#define ENUMERATE_MESSAGE_PORT_EVENT_HANDLERS(E) \
    E(onmessage, HTML::EventNames::message)      \
    E(onmessageerror, HTML::EventNames::messageerror)

// https://html.spec.whatwg.org/multipage/web-messaging.html#message-ports
class MessagePort final
    : public RefCounted<MessagePort>
    , public Weakable<MessagePort>
    , public DOM::EventTarget
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::MessagePortWrapper;

    using RefCounted::ref;
    using RefCounted::unref;

    static NonnullRefPtr<MessagePort> create(Bindings::ScriptExecutionContext& context)
    {
        return adopt_ref(*new MessagePort(context));
    }

    virtual ~MessagePort() override;

    // ^EventTarget
    virtual void ref_event_target() override { ref(); }
    virtual void unref_event_target() override { unref(); }
    virtual JS::Object* create_wrapper(JS::GlobalObject&) override;

    // https://html.spec.whatwg.org/multipage/web-messaging.html#entangle
    void entangle_with(Badge<MessageChannel>, MessagePort&);

    // https://html.spec.whatwg.org/multipage/web-messaging.html#dom-messageport-postmessage
    void post_message(JS::Value);

    void start();

    void close();

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)    \
    void set_##attribute_name(HTML::EventHandler); \
    HTML::EventHandler attribute_name();
    ENUMERATE_MESSAGE_PORT_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

private:
    explicit MessagePort(Bindings::ScriptExecutionContext&);

    bool is_entangled() const { return m_remote_port; }
    void disentangle();

    // The HTML spec implies(!) that this is MessagePort.[[RemotePort]]
    WeakPtr<MessagePort> m_remote_port;

    // https://html.spec.whatwg.org/multipage/web-messaging.html#has-been-shipped
    bool m_has_been_shipped { false };

    // This is TransferableObject.[[Detached]]
    bool m_detached { false };
};

}
