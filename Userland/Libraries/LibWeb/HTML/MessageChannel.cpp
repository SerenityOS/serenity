/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/MessageChannel.h>
#include <LibWeb/HTML/MessagePort.h>

namespace Web::HTML {

MessageChannel::MessageChannel(Bindings::WindowObject& global_object)
{
    auto& context = global_object.impl().associated_document();

    // 1. Set this's port 1 to a new MessagePort in this's relevant Realm.
    m_port1 = MessagePort::create(context);

    // 2. Set this's port 2 to a new MessagePort in this's relevant Realm.
    m_port2 = MessagePort::create(context);

    // 3. Entangle this's port 1 and this's port 2.
    m_port1->entangle_with({}, *m_port2);
}

MessageChannel::~MessageChannel()
{
}

}
