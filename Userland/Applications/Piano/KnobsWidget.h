/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ProcessorParameterWidget/ParameterWidget.h"
#include <AK/NonnullRefPtrVector.h>
#include <LibDSP/ProcessorParameter.h>
#include <LibDSP/Synthesizers.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>

class TrackManager;
class MainWidget;

class KnobsWidget final : public GUI::Frame {
    C_OBJECT(KnobsWidget)
public:
    virtual ~KnobsWidget() override = default;

    void update_knobs();

private:
    KnobsWidget(TrackManager&, MainWidget&);

    TrackManager& m_track_manager;
    MainWidget& m_main_widget;

    RefPtr<GUI::Widget> m_octave_container;
    RefPtr<GUI::Slider> m_octave_knob;
    RefPtr<GUI::Label> m_octave_value;

    NonnullRefPtrVector<ProcessorParameterWidget> m_parameter_widgets;

    bool m_change_underlying { true };
};
