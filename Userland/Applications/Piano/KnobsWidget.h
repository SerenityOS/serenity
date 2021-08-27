/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ProcessorParameterSlider.h"
#include <AK/NonnullRefPtrVector.h>
#include <LibGUI/Frame.h>

class TrackManager;
class MainWidget;

class KnobsWidget final : public GUI::Frame {
    C_OBJECT(KnobsWidget)
public:
    virtual ~KnobsWidget() override;

    void update_knobs();

private:
    KnobsWidget(TrackManager&, MainWidget&);

    TrackManager& m_track_manager;
    MainWidget& m_main_widget;

    RefPtr<GUI::Widget> m_labels_container;
    RefPtr<GUI::Label> m_volume_label;
    RefPtr<GUI::Label> m_octave_label;
    RefPtr<GUI::Label> m_wave_label;
    RefPtr<GUI::Label> m_attack_label;
    RefPtr<GUI::Label> m_decay_label;
    RefPtr<GUI::Label> m_sustain_label;
    RefPtr<GUI::Label> m_release_label;
    NonnullRefPtrVector<GUI::Label> m_delay_labels;

    RefPtr<GUI::Widget> m_values_container;
    RefPtr<GUI::Label> m_volume_value;
    RefPtr<GUI::Label> m_octave_value;
    RefPtr<GUI::Label> m_wave_value;
    RefPtr<GUI::Label> m_attack_value;
    RefPtr<GUI::Label> m_decay_value;
    RefPtr<GUI::Label> m_sustain_value;
    RefPtr<GUI::Label> m_release_value;
    NonnullRefPtrVector<GUI::Label> m_delay_values;

    RefPtr<GUI::Widget> m_knobs_container;
    RefPtr<GUI::Slider> m_volume_knob;
    RefPtr<GUI::Slider> m_octave_knob;
    RefPtr<GUI::Slider> m_wave_knob;
    RefPtr<GUI::Slider> m_attack_knob;
    RefPtr<GUI::Slider> m_decay_knob;
    RefPtr<GUI::Slider> m_sustain_knob;
    RefPtr<GUI::Slider> m_release_knob;
    NonnullRefPtrVector<ProcessorParameterSlider> m_delay_knobs;

    bool m_change_underlying { true };
};
