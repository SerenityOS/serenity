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

#include "Music.h"
#include <LibGUI/Widget.h>

class AudioEngine;
class WaveWidget;
class RollWidget;
class KeysWidget;
class KnobsWidget;

class MainWidget final : public GUI::Widget {
    C_OBJECT(MainWidget)
public:
    virtual ~MainWidget() override;

    void set_octave_and_ensure_note_change(Direction);

private:
    explicit MainWidget(AudioEngine&);

    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void keyup_event(GUI::KeyEvent&) override;
    virtual void custom_event(Core::CustomEvent&) override;

    void note_key_action(int key_code, Switch);
    void special_key_action(int key_code);

    AudioEngine& m_audio_engine;

    RefPtr<WaveWidget> m_wave_widget;
    RefPtr<RollWidget> m_roll_widget;
    RefPtr<GUI::Widget> m_keys_and_knobs_container;
    RefPtr<KeysWidget> m_keys_widget;
    RefPtr<KnobsWidget> m_knobs_widget;

    bool m_keys_pressed[key_code_count] { false };
};
