/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibGUI/GFrame.h>

namespace GUI {
class Label;
class Slider;
}

class AudioEngine;
class MainWidget;

class KnobsWidget final : public GUI::Frame {
    C_OBJECT(KnobsWidget)
public:
    virtual ~KnobsWidget() override;

    void update_knobs();

private:
    KnobsWidget(GUI::Widget* parent, AudioEngine&, MainWidget&);

    AudioEngine& m_audio_engine;
    MainWidget& m_main_widget;

    RefPtr<GUI::Widget> m_labels_container;
    RefPtr<GUI::Label> m_octave_label;
    RefPtr<GUI::Label> m_wave_label;
    RefPtr<GUI::Label> m_attack_label;
    RefPtr<GUI::Label> m_decay_label;
    RefPtr<GUI::Label> m_sustain_label;
    RefPtr<GUI::Label> m_delay_label;

    RefPtr<GUI::Widget> m_values_container;
    RefPtr<GUI::Label> m_octave_value;
    RefPtr<GUI::Label> m_wave_value;
    RefPtr<GUI::Label> m_attack_value;
    RefPtr<GUI::Label> m_decay_value;
    RefPtr<GUI::Label> m_sustain_value;
    RefPtr<GUI::Label> m_delay_value;

    RefPtr<GUI::Widget> m_knobs_container;
    RefPtr<GUI::Slider> m_octave_knob;
    RefPtr<GUI::Slider> m_wave_knob;
    RefPtr<GUI::Slider> m_attack_knob;
    RefPtr<GUI::Slider> m_decay_knob;
    RefPtr<GUI::Slider> m_sustain_knob;
    RefPtr<GUI::Slider> m_delay_knob;

    bool m_change_octave { true };
};
