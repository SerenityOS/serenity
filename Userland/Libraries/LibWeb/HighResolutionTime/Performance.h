/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>
#include <LibCore/ElapsedTimer.h>
#include <LibWeb/DOM/EventTarget.h>

namespace Web::HighResolutionTime {

class Performance final : public DOM::EventTarget {
    WEB_PLATFORM_OBJECT(Performance, DOM::EventTarget);

public:
    virtual ~Performance() override;

    double now() const { return m_timer.elapsed(); }
    double time_origin() const;

    JS::GCPtr<NavigationTiming::PerformanceTiming> timing();

private:
    explicit Performance(HTML::Window&);

    virtual void visit_edges(Cell::Visitor&) override;

    JS::NonnullGCPtr<HTML::Window> m_window;
    JS::GCPtr<NavigationTiming::PerformanceTiming> m_timing;

    Core::ElapsedTimer m_timer;
};

}

WRAPPER_HACK(Performance, Web::HighResolutionTime)
