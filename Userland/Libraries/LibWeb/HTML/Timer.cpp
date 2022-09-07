/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/Timer.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Platform/Timer.h>

namespace Web::HTML {

JS::NonnullGCPtr<Timer> Timer::create(Window& window, i32 milliseconds, Function<void()> callback, i32 id)
{
    return *window.heap().allocate_without_realm<Timer>(window, milliseconds, move(callback), id);
}

Timer::Timer(Window& window, i32 milliseconds, Function<void()> callback, i32 id)
    : m_window(window)
    , m_callback(move(callback))
    , m_id(id)
{
    m_timer = Platform::Timer::create_single_shot(milliseconds, [this] {
        m_callback();
    });
}

void Timer::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_window.ptr());
}

Timer::~Timer()
{
}

void Timer::start()
{
    m_timer->start();
}

}
