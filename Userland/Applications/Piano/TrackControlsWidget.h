/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ProcessorParameterWidget/ParameterWidget.h"
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <LibDSP/Track.h>
#include <LibGUI/Forward.h>

class TrackControlsWidget final : public GUI::Frame {
    C_OBJECT_ABSTRACT(TrackControlsWidget)
public:
    virtual ~TrackControlsWidget() override = default;

    static ErrorOr<NonnullRefPtr<TrackControlsWidget>> try_create(WeakPtr<DSP::Track>);

private:
    TrackControlsWidget(WeakPtr<DSP::Track>);

    WeakPtr<DSP::Track> m_track;
    Vector<NonnullRefPtr<GUI::GroupBox>> m_processor_groups;
};
