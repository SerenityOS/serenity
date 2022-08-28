/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/PerformancePrototype.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/HighResolutionTime/Performance.h>

namespace Web::HighResolutionTime {

Performance::Performance(HTML::Window& window)
    : DOM::EventTarget(window.realm())
    , m_window(window)
    , m_timing(make<NavigationTiming::PerformanceTiming>(window))
{
    set_prototype(&window.ensure_web_prototype<Bindings::PerformancePrototype>("Performance"));
    m_timer.start();
}

Performance::~Performance() = default;

void Performance::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_window.ptr());
}

double Performance::time_origin() const
{
    auto origin = m_timer.origin_time();
    return (origin.tv_sec * 1000.0) + (origin.tv_usec / 1000.0);
}

}
