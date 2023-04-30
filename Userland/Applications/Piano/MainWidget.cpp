/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include "KeysWidget.h"
#include "PlayerWidget.h"
#include "RollWidget.h"
#include "SamplerWidget.h"
#include "TrackControlsWidget.h"
#include "TrackManager.h"
#include "WaveWidget.h"
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Slider.h>
#include <LibGUI/TabWidget.h>

ErrorOr<NonnullRefPtr<MainWidget>> MainWidget::try_create(TrackManager& manager, AudioPlayerLoop& loop)
{
    auto widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) MainWidget(manager, loop)));
    TRY(widget->initialize());
    return widget;
}

MainWidget::MainWidget(TrackManager& track_manager, AudioPlayerLoop& loop)
    : m_track_manager(track_manager)
    , m_audio_loop(loop)
{
}

ErrorOr<void> MainWidget::initialize()
{
    TRY(try_set_layout<GUI::VerticalBoxLayout>(2, 2));
    set_fill_with_background_color(true);

    m_wave_widget = TRY(try_add<WaveWidget>(m_track_manager));
    m_wave_widget->set_fixed_height(100);

    m_tab_widget = TRY(try_add<GUI::TabWidget>());
    m_roll_widget = TRY(m_tab_widget->try_add_tab<RollWidget>(TRY("Piano Roll"_string), m_track_manager));

    m_roll_widget->set_fixed_height(300);

    (void)TRY(m_tab_widget->try_add_tab<SamplerWidget>("Sampler"_short_string, m_track_manager));
    m_player_widget = TRY(try_add<PlayerWidget>(m_track_manager, m_audio_loop));

    m_keys_and_knobs_container = TRY(try_add<GUI::Widget>());
    TRY(m_keys_and_knobs_container->try_set_layout<GUI::HorizontalBoxLayout>(GUI::Margins {}, 2));
    m_keys_and_knobs_container->set_fixed_height(130);
    m_keys_and_knobs_container->set_fill_with_background_color(true);

    m_keys_widget = TRY(m_keys_and_knobs_container->try_add<KeysWidget>(m_track_manager.keyboard()));

    m_octave_container = TRY(m_keys_and_knobs_container->try_add<GUI::Widget>());
    m_octave_container->set_preferred_width(GUI::SpecialDimension::Fit);
    TRY(m_octave_container->try_set_layout<GUI::VerticalBoxLayout>());
    auto octave_label = TRY(m_octave_container->try_add<GUI::Label>("Octave"_short_string));
    octave_label->set_preferred_width(GUI::SpecialDimension::Fit);
    m_octave_value = TRY(m_octave_container->try_add<GUI::Label>(TRY(String::number(m_track_manager.keyboard()->virtual_keyboard_octave()))));
    m_octave_value->set_preferred_width(GUI::SpecialDimension::Fit);

    // FIXME: Implement vertical flipping in GUI::Slider, not here.
    m_octave_knob = TRY(m_octave_container->try_add<GUI::VerticalSlider>());
    m_octave_knob->set_preferred_width(GUI::SpecialDimension::Fit);
    m_octave_knob->set_tooltip("Z: octave down, X: octave up");
    m_octave_knob->set_range(octave_min - 1, octave_max - 1);
    m_octave_knob->set_value((octave_max - 1) - (m_track_manager.keyboard()->virtual_keyboard_octave() - 1));
    m_octave_knob->set_step(1);
    m_octave_knob->on_change = [this](int value) {
        int new_octave = octave_max - value;
        set_octave_via_slider(new_octave);
        VERIFY(new_octave == m_track_manager.keyboard()->virtual_keyboard_octave());
        m_octave_value->set_text(String::number(new_octave).release_value_but_fixme_should_propagate_errors());
    };

    m_knobs_widget = TRY(m_keys_and_knobs_container->try_add<TrackControlsWidget>(m_track_manager, *this));

    m_roll_widget->set_keys_widget(m_keys_widget);

    return {};
}

ErrorOr<void> MainWidget::add_track_actions(GUI::Menu& menu)
{
    TRY(menu.try_add_action(GUI::Action::create("&Add Track", { Mod_Ctrl, Key_T }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/plus.png"sv)), [&](auto&) {
        m_player_widget->add_track();
    })));

    TRY(menu.try_add_action(GUI::Action::create("&Next Track", { Mod_Ctrl, Key_N }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-last.png"sv)), [&](auto&) {
        turn_off_pressed_keys();
        m_player_widget->next_track();
        turn_on_pressed_keys();
    })));

    return {};
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
    if (!event.alt() && !event.ctrl() && !event.shift()) {
        // This is to stop held-down keys from creating multiple events.
        if (m_keys_pressed[event.key()])
            return;
        m_keys_pressed[event.key()] = true;

        bool event_was_accepted = false;
        if (note_key_action(event.key(), DSP::Keyboard::Switch::On))
            event_was_accepted = true;
        if (special_key_action(event.key()))
            event_was_accepted = true;
        if (!event_was_accepted)
            event.ignore();
    } else {
        event.ignore();
    }

    m_keys_widget->update();
}

void MainWidget::keyup_event(GUI::KeyEvent& event)
{
    m_keys_pressed[event.key()] = false;

    note_key_action(event.key(), DSP::Keyboard::Switch::Off);
    m_keys_widget->update();
}

bool MainWidget::note_key_action(int key_code, DSP::Keyboard::Switch switch_note)
{
    auto key = m_keys_widget->key_code_to_key(key_code);
    if (key == -1)
        return false;
    m_track_manager.keyboard()->set_keyboard_note_in_active_octave(key, switch_note);
    return true;
}

bool MainWidget::special_key_action(int key_code)
{
    switch (key_code) {
    case Key_Z:
        change_octave_via_keys(DSP::Keyboard::Direction::Down);
        return true;
    case Key_X:
        change_octave_via_keys(DSP::Keyboard::Direction::Up);
        return true;
    case Key_Space:
        m_player_widget->toggle_paused();
        return true;
    }

    return false;
}

void MainWidget::turn_off_pressed_keys()
{
    if (m_keys_widget->mouse_note() != -1)
        m_track_manager.keyboard()->set_keyboard_note_in_active_octave(m_keys_widget->mouse_note(), DSP::Keyboard::Switch::Off);
    for (int i = 0; i < key_code_count; ++i) {
        if (m_keys_pressed[i])
            note_key_action(i, DSP::Keyboard::Switch::Off);
    }
}

void MainWidget::turn_on_pressed_keys()
{
    if (m_keys_widget->mouse_note() != -1)
        m_track_manager.keyboard()->set_keyboard_note_in_active_octave(m_keys_widget->mouse_note(), DSP::Keyboard::Switch::On);
    for (int i = 0; i < key_code_count; ++i) {
        if (m_keys_pressed[i])
            note_key_action(i, DSP::Keyboard::Switch::On);
    }
}

void MainWidget::set_octave_via_slider(int octave)
{
    turn_off_pressed_keys();
    MUST(m_track_manager.keyboard()->set_virtual_keyboard_octave(octave));
    turn_on_pressed_keys();

    m_keys_widget->update();
}

void MainWidget::change_octave_via_keys(DSP::Keyboard::Direction direction)
{
    turn_off_pressed_keys();
    m_track_manager.keyboard()->change_virtual_keyboard_octave(direction);
    turn_on_pressed_keys();

    m_octave_knob->set_value(octave_max - m_track_manager.keyboard()->virtual_keyboard_octave());
    m_keys_widget->update();
}
