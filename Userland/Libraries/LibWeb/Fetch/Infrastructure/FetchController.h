/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibJS/SafeFunction.h>
#include <LibWeb/Fetch/Infrastructure/FetchTimingInfo.h>

namespace Web::Fetch::Infrastructure {

// https://fetch.spec.whatwg.org/#fetch-controller
class FetchController : public JS::Cell {
    JS_CELL(FetchController, JS::Cell);

public:
    enum class State {
        Ongoing,
        Terminated,
        Aborted,
    };

    [[nodiscard]] static JS::NonnullGCPtr<FetchController> create(JS::VM&);

    void set_full_timing_info(JS::NonnullGCPtr<FetchTimingInfo> full_timing_info) { m_full_timing_info = full_timing_info; }
    void set_report_timing_steps(JS::SafeFunction<void(JS::Object const&)> report_timing_steps) { m_report_timing_steps = move(report_timing_steps); }
    void set_next_manual_redirect_steps(JS::SafeFunction<void()> next_manual_redirect_steps) { m_next_manual_redirect_steps = move(next_manual_redirect_steps); }

    [[nodiscard]] State state() const { return m_state; }

    void report_timing(JS::Object const&) const;
    void process_next_manual_redirect() const;
    [[nodiscard]] JS::NonnullGCPtr<FetchTimingInfo> extract_full_timing_info() const;
    void abort(JS::Realm&, Optional<JS::Value>);
    void terminate();

private:
    FetchController();

    virtual void visit_edges(JS::Cell::Visitor&) override;

    // https://fetch.spec.whatwg.org/#fetch-controller-state
    // state (default "ongoing")
    //    "ongoing", "terminated", or "aborted"
    State m_state { State::Ongoing };

    // https://fetch.spec.whatwg.org/#fetch-controller-full-timing-info
    // full timing info (default null)
    //    Null or a fetch timing info.
    JS::GCPtr<FetchTimingInfo> m_full_timing_info;

    // https://fetch.spec.whatwg.org/#fetch-controller-report-timing-steps
    // report timing steps (default null)
    //    Null or an algorithm accepting a global object.
    Optional<JS::SafeFunction<void(JS::Object const&)>> m_report_timing_steps;

    // https://fetch.spec.whatwg.org/#fetch-controller-report-timing-steps
    // FIXME: serialized abort reason (default null)
    //     Null or a Record (result of StructuredSerialize).

    // https://fetch.spec.whatwg.org/#fetch-controller-next-manual-redirect-steps
    // next manual redirect steps (default null)
    //     Null or an algorithm accepting nothing.
    Optional<JS::SafeFunction<void()>> m_next_manual_redirect_steps;
};

}
