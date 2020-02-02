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

#include "MainWidget.h"
#include "AudioEngine.h"
#include "KeysWidget.h"
#include "KnobsWidget.h"
#include "RollWidget.h"
#include "WaveWidget.h"
#include <LibGUI/GBoxLayout.h>

MainWidget::MainWidget(AudioEngine& audio_engine)
    : m_audio_engine(audio_engine)
{
    set_layout(make<GVBoxLayout>());
    layout()->set_spacing(2);
    layout()->set_margins({ 2, 2, 2, 2 });
    set_fill_with_background_color(true);

    m_wave_widget = WaveWidget::construct(this, audio_engine);
    m_wave_widget->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_wave_widget->set_preferred_size(0, 100);

    m_roll_widget = RollWidget::construct(this, audio_engine);
    m_roll_widget->set_size_policy(SizePolicy::Fill, SizePolicy::Fill);
    m_roll_widget->set_preferred_size(0, 300);

    m_keys_and_knobs_container = GWidget::construct(this);
    m_keys_and_knobs_container->set_layout(make<GHBoxLayout>());
    m_keys_and_knobs_container->layout()->set_spacing(2);
    m_keys_and_knobs_container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_keys_and_knobs_container->set_preferred_size(0, 100);
    m_keys_and_knobs_container->set_fill_with_background_color(true);

    m_keys_widget = KeysWidget::construct(m_keys_and_knobs_container, audio_engine);

    m_knobs_widget = KnobsWidget::construct(m_keys_and_knobs_container, audio_engine, *this);
    m_knobs_widget->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    m_knobs_widget->set_preferred_size(200, 0);
}

MainWidget::~MainWidget()
{
}

// FIXME: There are some unnecessary calls to update() throughout this program,
// which are an easy target for optimization.

void MainWidget::custom_event(CCustomEvent&)
{
    m_wave_widget->update();

    if (m_audio_engine.time() == 0)
        m_roll_widget->update_roll();
}

void MainWidget::keydown_event(GKeyEvent& event)
{
    // This is to stop held-down keys from creating multiple events.
    if (m_keys_pressed[event.key()])
        return;
    else
        m_keys_pressed[event.key()] = true;

    note_key_action(event.key(), On);
    special_key_action(event.key());
    m_keys_widget->update();
}

void MainWidget::keyup_event(GKeyEvent& event)
{
    m_keys_pressed[event.key()] = false;

    note_key_action(event.key(), Off);
    m_keys_widget->update();
}

void MainWidget::note_key_action(int key_code, Switch switch_note)
{
    int key = m_keys_widget->key_code_to_key(key_code);
    m_keys_widget->set_key(key, switch_note);
}

void MainWidget::special_key_action(int key_code)
{
    switch (key_code) {
    case Key_Z:
        set_octave_and_ensure_note_change(Down);
        break;
    case Key_X:
        set_octave_and_ensure_note_change(Up);
        break;
    case Key_C:
        m_audio_engine.set_wave(Up);
        m_knobs_widget->update_knobs();
        break;
    }
}

void MainWidget::set_octave_and_ensure_note_change(Direction direction)
{
    m_keys_widget->set_key(m_keys_widget->mouse_note(), Off);
    for (int i = 0; i < key_code_count; ++i) {
        if (m_keys_pressed[i])
            note_key_action(i, Off);
    }

    m_audio_engine.set_octave(direction);

    m_keys_widget->set_key(m_keys_widget->mouse_note(), On);
    for (int i = 0; i < key_code_count; ++i) {
        if (m_keys_pressed[i])
            note_key_action(i, On);
    }

    m_knobs_widget->update_knobs();
    m_keys_widget->update();
}
