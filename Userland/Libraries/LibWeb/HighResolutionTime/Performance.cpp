/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/HighResolutionTime/Performance.h>
#include <LibWeb/NavigationTiming/PerformanceTiming.h>

namespace Web::HighResolutionTime {

Performance::Performance(HTML::Window& window)
    : DOM::EventTarget(window.realm())
    , m_window(window)
{
    set_prototype(&Bindings::cached_web_prototype(realm(), "Performance"));
    m_timer.start();
}

Performance::~Performance() = default;

void Performance::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_window.ptr());
    visitor.visit(m_timing.ptr());
}

JS::GCPtr<NavigationTiming::PerformanceTiming> Performance::timing()
{
    if (!m_timing)
        m_timing = heap().allocate<NavigationTiming::PerformanceTiming>(realm(), *m_window);
    return m_timing;
}

double Performance::time_origin() const
{
    return static_cast<double>(m_timer.origin_time().to_milliseconds());
}

}
