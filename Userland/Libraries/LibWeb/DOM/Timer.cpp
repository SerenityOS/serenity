/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Timer.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibWeb/DOM/Timer.h>
#include <LibWeb/DOM/Window.h>

namespace Web::DOM {

NonnullRefPtr<Timer> Timer::create_interval(Window& window, int milliseconds, JS::FunctionObject& callback)
{
    return adopt_ref(*new Timer(window, Type::Interval, milliseconds, callback));
}

NonnullRefPtr<Timer> Timer::create_timeout(Window& window, int milliseconds, JS::FunctionObject& callback)
{
    return adopt_ref(*new Timer(window, Type::Timeout, milliseconds, callback));
}

Timer::Timer(Window& window, Type type, int milliseconds, JS::FunctionObject& callback)
    : m_window(window)
    , m_type(type)
    , m_callback(JS::make_handle(&callback))
{
    m_id = window.allocate_timer_id({});
    m_timer = Core::Timer::construct(milliseconds, [this] { m_window.timer_did_fire({}, *this); });
    if (m_type == Type::Timeout)
        m_timer->set_single_shot(true);
}

Timer::~Timer()
{
    m_window.deallocate_timer_id({}, m_id);
}

}
