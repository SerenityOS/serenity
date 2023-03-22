/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/ElapsedTimer.h>
#include <LibWeb/DOM/EventTarget.h>

namespace Web::HighResolutionTime {

class Performance final : public DOM::EventTarget {
    WEB_PLATFORM_OBJECT(Performance, DOM::EventTarget);

public:
    virtual ~Performance() override;

    double now() const { return static_cast<double>(m_timer.elapsed()); }
    double time_origin() const;

    JS::GCPtr<NavigationTiming::PerformanceTiming> timing();

    WebIDL::ExceptionOr<Vector<JS::Handle<PerformanceTimeline::PerformanceEntry>>> get_entries() const;
    WebIDL::ExceptionOr<Vector<JS::Handle<PerformanceTimeline::PerformanceEntry>>> get_entries_by_type(String const& type) const;
    WebIDL::ExceptionOr<Vector<JS::Handle<PerformanceTimeline::PerformanceEntry>>> get_entries_by_name(String const& name, Optional<String> type) const;

private:
    explicit Performance(HTML::Window&);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    JS::NonnullGCPtr<HTML::Window> m_window;
    JS::GCPtr<NavigationTiming::PerformanceTiming> m_timing;

    Core::ElapsedTimer m_timer;
};

}
