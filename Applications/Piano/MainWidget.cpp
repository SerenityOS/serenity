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
#include "KeysWidget.h"
#include "KnobsWidget.h"
#include "RollWidget.h"
#include "SamplerWidget.h"
#include "TrackManager.h"
#include "WaveWidget.h"
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Menu.h>
#include <LibGUI/TabWidget.h>

MainWidget::MainWidget(TrackManager& track_manager)
    : m_track_manager(track_manager)
{
    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_spacing(2);
    layout()->set_margins({ 2, 2, 2, 2 });
    set_fill_with_background_color(true);

    m_wave_widget = add<WaveWidget>(track_manager);
    m_wave_widget->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_wave_widget->set_preferred_size(0, 100);

    m_tab_widget = add<GUI::TabWidget>();
    m_roll_widget = m_tab_widget->add_tab<RollWidget>("Piano Roll", track_manager);

    m_roll_widget->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    m_roll_widget->set_preferred_size(0, 300);

    m_tab_widget->add_tab<SamplerWidget>("Sampler", track_manager);

    m_keys_and_knobs_container = add<GUI::Widget>();
    m_keys_and_knobs_container->set_layout<GUI::HorizontalBoxLayout>();
    m_keys_and_knobs_container->layout()->set_spacing(2);
    m_keys_and_knobs_container->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_keys_and_knobs_container->set_preferred_size(0, 100);
    m_keys_and_knobs_container->set_fill_with_background_color(true);

    m_keys_widget = m_keys_and_knobs_container->add<KeysWidget>(track_manager);

    m_knobs_widget = m_keys_and_knobs_container->add<KnobsWidget>(track_manager, *this);
    m_knobs_widget->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    m_knobs_widget->set_preferred_size(350, 0);
}

MainWidget::~MainWidget()
{
}

void MainWidget::add_actions(GUI::Menu& menu)
{
    menu.add_action(GUI::Action::create("Add track", { Mod_Ctrl, Key_T }, [&](auto&) {
        m_track_manager.add_track();
    }));

    menu.add_action(GUI::Action::create("Next track", { Mod_Ctrl, Key_N }, [&](auto&) {
        turn_off_pressed_keys();
        m_track_manager.next_track();
        turn_on_pressed_keys();

        m_knobs_widget->update_knobs();
    }));
}

// FIXME: There are some unnecessary calls to update() throughout this program,
// which are an easy target for optimization.

void MainWidget::custom_event(Core::CustomEvent&)
{
    m_wave_widget->update();
    m_roll_widget->update();
}

void MainWidget::keydown_event(GUI::KeyEvent& event)
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

void MainWidget::keyup_event(GUI::KeyEvent& event)
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
        m_track_manager.current_track().set_wave(Up);
        m_knobs_widget->update_knobs();
        break;
    }
}

void MainWidget::turn_off_pressed_keys()
{
    m_keys_widget->set_key(m_keys_widget->mouse_note(), Off);
    for (int i = 0; i < key_code_count; ++i) {
        if (m_keys_pressed[i])
            note_key_action(i, Off);
    }
}

void MainWidget::turn_on_pressed_keys()
{
    m_keys_widget->set_key(m_keys_widget->mouse_note(), On);
    for (int i = 0; i < key_code_count; ++i) {
        if (m_keys_pressed[i])
            note_key_action(i, On);
    }
}

void MainWidget::set_octave_and_ensure_note_change(int octave)
{
    turn_off_pressed_keys();
    m_track_manager.set_octave(octave);
    turn_on_pressed_keys();

    m_knobs_widget->update_knobs();
    m_keys_widget->update();
}

void MainWidget::set_octave_and_ensure_note_change(Direction direction)
{
    turn_off_pressed_keys();
    m_track_manager.set_octave(direction);
    turn_on_pressed_keys();

    m_knobs_widget->update_knobs();
    m_keys_widget->update();
}
