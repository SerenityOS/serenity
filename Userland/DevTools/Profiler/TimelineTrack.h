/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Histogram.h"
#include <LibGUI/Frame.h>

namespace Profiler {

class Process;
class Profile;
class TimelineView;

class TimelineTrack final : public GUI::Frame {
    C_OBJECT(TimelineTrack);

public:
    virtual ~TimelineTrack() override;

    void set_scale(float);

private:
    virtual void event(Core::Event&) override;
    virtual void paint_event(GUI::PaintEvent&) override;

    struct HistogramInputs {
        bool operator==(HistogramInputs const&) const = default;
        u64 start { 0 };
        u64 end { 0 };
        size_t columns { 0 };
    };

    void recompute_histograms_if_needed(HistogramInputs const&);

    explicit TimelineTrack(TimelineView const&, Profile const&, Process const&);

    TimelineView const& m_view;
    Profile const& m_profile;
    Process const& m_process;

    HistogramInputs m_cached_histogram_inputs;

    Optional<Histogram<u64>> m_kernel_histogram;
    Optional<Histogram<u64>> m_user_histogram;
    decltype(m_kernel_histogram->at(0)) m_max_value { 0 };
};

}
