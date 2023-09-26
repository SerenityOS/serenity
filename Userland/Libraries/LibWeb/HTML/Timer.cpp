/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Object.h>
#include <LibWeb/HTML/Timer.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/Platform/Timer.h>

namespace Web::HTML {

JS::NonnullGCPtr<Timer> Timer::create(JS::Object& window_or_worker_global_scope, i32 milliseconds, Function<void()> callback, i32 id)
{
    return window_or_worker_global_scope.heap().allocate_without_realm<Timer>(window_or_worker_global_scope, milliseconds, move(callback), id);
}

Timer::Timer(JS::Object& window_or_worker_global_scope, i32 milliseconds, Function<void()> callback, i32 id)
    : m_window_or_worker_global_scope(window_or_worker_global_scope)
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
    visitor.visit(m_window_or_worker_global_scope.ptr());
}

Timer::~Timer()
{
    VERIFY(!m_timer->is_active());
}

void Timer::start()
{
    m_timer->start();
}

void Timer::stop()
{
    m_timer->stop();
}

}
