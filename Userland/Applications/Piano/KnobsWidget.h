/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ProcessorParameterWidget/Dropdown.h"
#include "ProcessorParameterWidget/Slider.h"
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
    void cycle_waveform();

private:
    KnobsWidget(TrackManager&, MainWidget&);

    TrackManager& m_track_manager;
    MainWidget& m_main_widget;

    RefPtr<GUI::Widget> m_labels_container;
    RefPtr<GUI::Label> m_volume_label;
    RefPtr<GUI::Label> m_octave_label;
    NonnullRefPtrVector<GUI::Label> m_synth_labels;
    NonnullRefPtrVector<GUI::Label> m_delay_labels;

    RefPtr<GUI::Widget> m_values_container;
    RefPtr<GUI::Label> m_volume_value;
    RefPtr<GUI::Label> m_octave_value;
    NonnullRefPtrVector<GUI::Label> m_synth_values;
    NonnullRefPtrVector<GUI::Label> m_delay_values;

    RefPtr<GUI::Widget> m_knobs_container;
    RefPtr<GUI::Slider> m_volume_knob;
    RefPtr<GUI::Slider> m_octave_knob;
    RefPtr<ProcessorParameterDropdown<LibDSP::Synthesizers::Waveform>> m_synth_waveform;
    NonnullRefPtrVector<GUI::Widget> m_synth_knobs;
    NonnullRefPtrVector<ProcessorParameterSlider> m_delay_knobs;

    bool m_change_underlying { true };
};
