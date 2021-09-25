/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/PerformanceWrapper.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/HighResolutionTime/Performance.h>

namespace Web::HighResolutionTime {

Performance::Performance(DOM::Window& window)
    : DOM::EventTarget(static_cast<Bindings::ScriptExecutionContext&>(window.associated_document()))
    , m_window(window)
    , m_timing(make<NavigationTiming::PerformanceTiming>(window))
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

JS::Object* Performance::create_wrapper(JS::GlobalObject& global_object)
{
    return Bindings::wrap(global_object, *this);
}

}
