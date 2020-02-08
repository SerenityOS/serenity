/*
 * Copyright (c) 2020-2020, William McPherson <willmcpherson2@gmail.com>
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

#include <LibGUI/Frame.h>

namespace GUI {
class Label;
class Button;
}

class AudioEngine;

class WaveEditor final : public GUI::Frame {
    C_OBJECT(WaveEditor)
public:
    virtual ~WaveEditor() override;

private:
    WaveEditor(GUI::Widget* parent, AudioEngine&);

    virtual void paint_event(GUI::PaintEvent&) override;

    int sample_to_y(float percentage) const;

    AudioEngine& m_audio_engine;
};

class SamplerWidget final : public GUI::Frame {
    C_OBJECT(SamplerWidget)
public:
    virtual ~SamplerWidget() override;

private:
    SamplerWidget(GUI::Widget* parent, AudioEngine&);

    AudioEngine& m_audio_engine;

    RefPtr<GUI::Widget> m_open_button_and_recorded_sample_name_container;
    RefPtr<GUI::Button> m_open_button;
    RefPtr<GUI::Label> m_recorded_sample_name;
    RefPtr<WaveEditor> m_wave_editor;
};
