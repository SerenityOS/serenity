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

class Performance final
    : public DOM::EventTarget
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::PerformanceWrapper;
    using AllowOwnPtr = TrueType;

    explicit Performance(DOM::Window&);
    ~Performance();

    double now() const { return m_timer.elapsed(); }
    double time_origin() const;

    RefPtr<NavigationTiming::PerformanceTiming> timing() { return *m_timing; }

    virtual void ref_event_target() override;
    virtual void unref_event_target() override;

    virtual JS::Object* create_wrapper(JS::GlobalObject&) override;

private:
    DOM::Window& m_window;
    Core::ElapsedTimer m_timer;

    OwnPtr<NavigationTiming::PerformanceTiming> m_timing;
};

}
