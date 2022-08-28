/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/MessageChannel.h>
#include <LibWeb/HTML/MessagePort.h>

namespace Web::HTML {

MessageChannel::MessageChannel(HTML::Window& window)
{
    // 1. Set this's port 1 to a new MessagePort in this's relevant Realm.
    m_port1 = JS::make_handle(*MessagePort::create(window));

    // 2. Set this's port 2 to a new MessagePort in this's relevant Realm.
    m_port2 = JS::make_handle(*MessagePort::create(window));

    // 3. Entangle this's port 1 and this's port 2.
    m_port1->entangle_with(*m_port2);
}

MessageChannel::~MessageChannel() = default;

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
