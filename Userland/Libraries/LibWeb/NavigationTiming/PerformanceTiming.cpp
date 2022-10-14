/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/NavigationTiming/PerformanceTiming.h>

namespace Web::NavigationTiming {

PerformanceTiming::PerformanceTiming(HTML::Window& window)
    : PlatformObject(window.realm())
    , m_window(window)
{
    set_prototype(&Bindings::cached_web_prototype(realm(), "PerformanceTiming"));
}

PerformanceTiming::~PerformanceTiming() = default;

void PerformanceTiming::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_window.ptr());
}

}
