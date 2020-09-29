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

#include <LibWeb/Bindings/PerformanceWrapper.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/HighResolutionTime/Performance.h>

namespace Web::HighResolutionTime {

Performance::Performance(DOM::Window& window)
    : DOM::EventTarget(static_cast<Bindings::ScriptExecutionContext&>(window.document()))
    , m_window(window)
{
    m_timer.start();
}

Performance::~Performance()
{
}

double Performance::time_origin() const
{
    auto origin = m_timer.origin_time();
    return (origin.tv_sec * 1000.0) + (origin.tv_usec / 1000.0);
}

void Performance::ref_event_target()
{
    m_window.ref();
}

void Performance::unref_event_target()
{
    m_window.unref();
}

void Performance::dispatch_event(NonnullRefPtr<DOM::Event> event)
{
    DOM::EventDispatcher::dispatch(*this, event);
}

Bindings::EventTargetWrapper* Performance::create_wrapper(JS::GlobalObject& global_object)
{
    return Bindings::wrap(global_object, *this);
}

}
