/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "KnobsWidget.h"
#include "MainWidget.h"
#include "TrackManager.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Slider.h>

constexpr int max_attack = 1000;
constexpr int max_decay = 1000;
constexpr int max_sustain = 1000;
constexpr int max_release = 1000;
constexpr int max_delay = 8;

KnobsWidget::KnobsWidget(TrackManager& track_manager, MainWidget& main_widget)
    : m_track_manager(track_manager)
    , m_main_widget(main_widget)
{
    set_layout<GUI::VerticalBoxLayout>();
    set_fill_with_background_color(true);

    m_labels_container = add<GUI::Widget>();
    m_labels_container->set_layout<GUI::HorizontalBoxLayout>();
    m_labels_container->set_fixed_height(20);

    m_octave_label = m_labels_container->add<GUI::Label>("Octave");
    m_wave_label = m_labels_container->add<GUI::Label>("Wave");
    m_attack_label = m_labels_container->add<GUI::Label>("Attack");
    m_decay_label = m_labels_container->add<GUI::Label>("Decay");
    m_sustain_label = m_labels_container->add<GUI::Label>("Sustain");
    m_release_label = m_labels_container->add<GUI::Label>("Release");
    m_delay_label = m_labels_container->add<GUI::Label>("Delay");

    m_values_container = add<GUI::Widget>();
    m_values_container->set_layout<GUI::HorizontalBoxLayout>();
    m_values_container->set_fixed_height(10);

    m_octave_value = m_values_container->add<GUI::Label>(String::number(m_track_manager.octave()));
    m_wave_value = m_values_container->add<GUI::Label>(wave_strings[m_track_manager.current_track().wave()]);
    m_attack_value = m_values_container->add<GUI::Label>(String::number(m_track_manager.current_track().attack()));
    m_decay_value = m_values_container->add<GUI::Label>(String::number(m_track_manager.current_track().decay()));
    m_sustain_value = m_values_container->add<GUI::Label>(String::number(m_track_manager.current_track().sustain()));
    m_release_value = m_values_container->add<GUI::Label>(String::number(m_track_manager.current_track().release()));
    m_delay_value = m_values_container->add<GUI::Label>(String::number(m_track_manager.current_track().delay()));

    m_knobs_container = add<GUI::Widget>();
    m_knobs_container->set_layout<GUI::HorizontalBoxLayout>();

    // FIXME: Implement vertical flipping in GUI::Slider, not here.

    m_octave_knob = m_knobs_container->add<GUI::VerticalSlider>();
    m_octave_knob->set_tooltip("Z: octave down, X: octave up");
    m_octave_knob->set_range(octave_min - 1, octave_max - 1);
    m_octave_knob->set_value((octave_max - 1) - (m_track_manager.octave() - 1));
    m_octave_knob->on_change = [this](int value) {
        int new_octave = octave_max - value;
        if (m_change_underlying)
            m_main_widget.set_octave_and_ensure_note_change(new_octave);
        VERIFY(new_octave == m_track_manager.octave());
        m_octave_value->set_text(String::number(new_octave));
    };

    m_wave_knob = m_knobs_container->add<GUI::VerticalSlider>();
    m_wave_knob->set_tooltip("C: cycle through waveforms");
    m_wave_knob->set_range(0, last_wave);
    m_wave_knob->set_value(last_wave - m_track_manager.current_track().wave());
    m_wave_knob->on_change = [this](int value) {
        int new_wave = last_wave - value;
        if (m_change_underlying)
            m_track_manager.current_track().set_wave(new_wave);
        VERIFY(new_wave == m_track_manager.current_track().wave());
        m_wave_value->set_text(wave_strings[new_wave]);
    };

    m_attack_knob = m_knobs_container->add<GUI::VerticalSlider>();
    m_attack_knob->set_range(0, max_attack);
    m_attack_knob->set_value(max_attack - m_track_manager.current_track().attack());
    m_attack_knob->set_step(100);
    m_attack_knob->on_change = [this](int value) {
        int new_attack = max_attack - value;
        if (m_change_underlying)
            m_track_manager.current_track().set_attack(new_attack);
        VERIFY(new_attack == m_track_manager.current_track().attack());
        m_attack_value->set_text(String::number(new_attack));
    };

    m_decay_knob = m_knobs_container->add<GUI::VerticalSlider>();
    m_decay_knob->set_range(0, max_decay);
    m_decay_knob->set_value(max_decay - m_track_manager.current_track().decay());
    m_decay_knob->set_step(100);
    m_decay_knob->on_change = [this](int value) {
        int new_decay = max_decay - value;
        if (m_change_underlying)
            m_track_manager.current_track().set_decay(new_decay);
        VERIFY(new_decay == m_track_manager.current_track().decay());
        m_decay_value->set_text(String::number(new_decay));
    };

    m_sustain_knob = m_knobs_container->add<GUI::VerticalSlider>();
    m_sustain_knob->set_range(0, max_sustain);
    m_sustain_knob->set_value(max_sustain - m_track_manager.current_track().sustain());
    m_sustain_knob->set_step(100);
    m_sustain_knob->on_change = [this](int value) {
        int new_sustain = max_sustain - value;
        if (m_change_underlying)
            m_track_manager.current_track().set_sustain(new_sustain);
        VERIFY(new_sustain == m_track_manager.current_track().sustain());
        m_sustain_value->set_text(String::number(new_sustain));
    };

    m_release_knob = m_knobs_container->add<GUI::VerticalSlider>();
    m_release_knob->set_range(0, max_release);
    m_release_knob->set_value(max_release - m_track_manager.current_track().release());
    m_release_knob->set_step(100);
    m_release_knob->on_change = [this](int value) {
        int new_release = max_release - value;
        if (m_change_underlying)
            m_track_manager.current_track().set_release(new_release);
        VERIFY(new_release == m_track_manager.current_track().release());
        m_release_value->set_text(String::number(new_release));
    };

    m_delay_knob = m_knobs_container->add<GUI::VerticalSlider>();
    m_delay_knob->set_range(0, max_delay);
    m_delay_knob->set_value(max_delay - m_track_manager.current_track().delay());
    m_delay_knob->on_change = [this](int value) {
        int new_delay = max_delay - value;
        if (m_change_underlying)
            m_track_manager.current_track().set_delay(new_delay);
        VERIFY(new_delay == m_track_manager.current_track().delay());
        m_delay_value->set_text(String::number(new_delay));
    };
}

KnobsWidget::~KnobsWidget()
{
}

void KnobsWidget::update_knobs()
{
    m_wave_knob->set_value(last_wave - m_track_manager.current_track().wave());

    // FIXME: This is needed because when the slider is changed normally, we
    // need to change the underlying value, but if the keyboard was used, we
    // need to change the slider without changing the underlying value.
    m_change_underlying = false;

    m_octave_knob->set_value(octave_max - m_track_manager.octave());
    m_wave_knob->set_value(last_wave - m_track_manager.current_track().wave());
    m_attack_knob->set_value(max_attack - m_track_manager.current_track().attack());
    m_decay_knob->set_value(max_decay - m_track_manager.current_track().decay());
    m_sustain_knob->set_value(max_sustain - m_track_manager.current_track().sustain());
    m_release_knob->set_value(max_release - m_track_manager.current_track().release());
    m_delay_knob->set_value(max_delay - m_track_manager.current_track().delay());

    m_change_underlying = true;
}
