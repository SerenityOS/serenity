/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Timer.h>
#include <LibWeb/HTML/Timer.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

NonnullRefPtr<Timer> Timer::create(Window& window, i32 milliseconds, Function<void()> callback, i32 id)
{
    return adopt_ref(*new Timer(window, milliseconds, move(callback), id));
}

Timer::Timer(Window& window, i32 milliseconds, Function<void()> callback, i32 id)
    : m_window(window)
    , m_id(id)
{
    m_timer = Core::Timer::create_single_shot(milliseconds, [this, callback = move(callback)] {
        NonnullRefPtr strong_timer { *this };
        callback();
    });
}

Timer::~Timer()
{
    m_window.deallocate_timer_id({}, m_id);
}

void Timer::start()
{
    m_timer->start();
}

}
