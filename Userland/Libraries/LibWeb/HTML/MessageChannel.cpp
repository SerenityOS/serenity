/*
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/MessageChannelPrototype.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/MessageChannel.h>
#include <LibWeb/HTML/MessagePort.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

JS::NonnullGCPtr<MessageChannel> MessageChannel::create_with_global_object(HTML::Window& window)
{
    return *window.heap().allocate<MessageChannel>(window.realm(), window);
}

MessageChannel::MessageChannel(HTML::Window& window)
    : PlatformObject(window.realm())
{
    set_prototype(&window.ensure_web_prototype<Bindings::MessageChannelPrototype>("MessageChannel"));

    // 1. Set this's port 1 to a new MessagePort in this's relevant Realm.
    m_port1 = MessagePort::create(window);

    // 2. Set this's port 2 to a new MessagePort in this's relevant Realm.
    m_port2 = MessagePort::create(window);

    // 3. Entangle this's port 1 and this's port 2.
    m_port1->entangle_with(*m_port2);
}

MessageChannel::~MessageChannel() = default;

void MessageChannel::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_port1.ptr());
    visitor.visit(m_port2.ptr());
}

MessagePort* MessageChannel::port1()
{
    return m_port1;
}

MessagePort* MessageChannel::port2()
{
    return m_port2;
}

MessagePort const* MessageChannel::port1() const
{
    return m_port1;
}

MessagePort const* MessageChannel::port2() const
{
    return m_port2;
}

}
