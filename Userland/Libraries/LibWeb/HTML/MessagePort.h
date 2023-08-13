/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

#define ENUMERATE_MESSAGE_PORT_EVENT_HANDLERS(E) \
    E(onmessage, HTML::EventNames::message)      \
    E(onmessageerror, HTML::EventNames::messageerror)

// https://html.spec.whatwg.org/multipage/web-messaging.html#structuredserializeoptions
struct StructuredSerializeOptions {
    Vector<JS::Handle<JS::Object>> transfer;
};

// https://html.spec.whatwg.org/multipage/web-messaging.html#message-ports
class MessagePort final : public DOM::EventTarget {
    WEB_PLATFORM_OBJECT(MessagePort, DOM::EventTarget);

public:
    [[nodiscard]] static JS::NonnullGCPtr<MessagePort> create(JS::Realm&);

    virtual ~MessagePort() override;

    // https://html.spec.whatwg.org/multipage/web-messaging.html#entangle
    void entangle_with(MessagePort&);

    // https://html.spec.whatwg.org/multipage/web-messaging.html#dom-messageport-postmessage
    void post_message(JS::Value);

    void start();

    void close();

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)       \
    void set_##attribute_name(WebIDL::CallbackType*); \
    WebIDL::CallbackType* attribute_name();
    ENUMERATE_MESSAGE_PORT_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE

private:
    explicit MessagePort(JS::Realm&);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    bool is_entangled() const { return m_remote_port; }
    void disentangle();

    // The HTML spec implies(!) that this is MessagePort.[[RemotePort]]
    JS::GCPtr<MessagePort> m_remote_port;

    // https://html.spec.whatwg.org/multipage/web-messaging.html#has-been-shipped
    bool m_has_been_shipped { false };

    // This is TransferableObject.[[Detached]]
    bool m_detached { false };
};

}
