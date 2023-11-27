/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/NavigationTiming/PerformanceTiming.h>

namespace Web::NavigationTiming {

JS_DEFINE_ALLOCATOR(PerformanceTiming);

PerformanceTiming::PerformanceTiming(HTML::Window& window)
    : PlatformObject(window.realm())
    , m_window(window)
{
}

PerformanceTiming::~PerformanceTiming() = default;

void PerformanceTiming::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::PerformanceTimingPrototype>(realm, "PerformanceTiming"_fly_string));
}

void PerformanceTiming::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_window);
}

}
