/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>
#include <LibCore/ElapsedTimer.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/NavigationTiming/PerformanceTiming.h>

namespace Web::HighResolutionTime {

class Performance final : public DOM::EventTarget {
    WEB_PLATFORM_OBJECT(Performance, DOM::EventTarget);

public:
    virtual ~Performance() override;

    double now() const { return m_timer.elapsed(); }
    double time_origin() const;

    JS::GCPtr<NavigationTiming::PerformanceTiming> timing() { return *m_timing; }

private:
    explicit Performance(HTML::Window&);

    virtual void visit_edges(Cell::Visitor&) override;

    JS::NonnullGCPtr<HTML::Window> m_window;

    Core::ElapsedTimer m_timer;

    OwnPtr<NavigationTiming::PerformanceTiming> m_timing;
};

}

WRAPPER_HACK(Performance, Web::HighResolutionTime)
