/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/NavigationTiming/PerformanceTiming.h>

namespace Web::NavigationTiming {

JS_DEFINE_ALLOCATOR(PerformanceTiming);

PerformanceTiming::PerformanceTiming(HTML::WindowOrWorkerGlobalScopeMixin& window_or_worker)
    : PlatformObject(window_or_worker.this_impl().realm())
    , m_window_or_worker(window_or_worker)
{
}

PerformanceTiming::~PerformanceTiming() = default;

void PerformanceTiming::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(PerformanceTiming);
}

void PerformanceTiming::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    if (m_window_or_worker)
        visitor.visit(m_window_or_worker->this_impl());
}

}
