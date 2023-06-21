/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Notifier.h>

namespace Core {

Notifier::Notifier(int fd, Type type, Object* parent)
    : Object(parent)
    , m_fd(fd)
    , m_type(type)
{
    set_enabled(true);
}

Notifier::~Notifier()
{
    set_enabled(false);
}

void Notifier::set_enabled(bool enabled)
{
    if (m_fd < 0)
        return;
    if (enabled)
        Core::EventLoop::register_notifier({}, *this);
    else
        Core::EventLoop::unregister_notifier({}, *this);
}

void Notifier::close()
{
    if (m_fd < 0)
        return;
    set_enabled(false);
    m_fd = -1;
}

void Notifier::event(Core::Event& event)
{
    if (event.type() == Core::Event::NotifierActivation) {
        if (on_activation)
            on_activation();
        return;
    }
    Object::event(event);
}

}
