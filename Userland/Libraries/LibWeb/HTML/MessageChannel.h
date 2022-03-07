/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/web-messaging.html#message-channels
class MessageChannel final
    : public RefCounted<MessageChannel>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::MessageChannelWrapper;

    using RefCounted::ref;
    using RefCounted::unref;

    static NonnullRefPtr<MessageChannel> create_with_global_object(Bindings::WindowObject&)
    {
        return adopt_ref(*new MessageChannel());
    }

    virtual ~MessageChannel() override;

    MessagePort* port1() { return m_port1; }
    MessagePort const* port1() const { return m_port1; }

    MessagePort* port2() { return m_port2; }
    MessagePort const* port2() const { return m_port2; }

private:
    MessageChannel();

    RefPtr<MessagePort> m_port1;
    RefPtr<MessagePort> m_port2;
};

}
