/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibCore/Timer.h>
#include <LibJS/Runtime/Function.h>
#include <LibWeb/DOM/Timer.h>
#include <LibWeb/DOM/Window.h>

namespace Web::DOM {

NonnullRefPtr<Timer> Timer::create_interval(Window& window, int milliseconds, JS::Function& callback)
{
    return adopt(*new Timer(window, Type::Interval, milliseconds, callback));
}

NonnullRefPtr<Timer> Timer::create_timeout(Window& window, int milliseconds, JS::Function& callback)
{
    return adopt(*new Timer(window, Type::Timeout, milliseconds, callback));
}

Timer::Timer(Window& window, Type type, int milliseconds, JS::Function& callback)
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
}

}
