/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "KnobsWidget.h"
#include "MainWidget.h"
#include "ProcessorParameterWidget/ParameterWidget.h"
#include "TrackManager.h"
#include <LibDSP/ProcessorParameter.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Slider.h>
#include <LibGfx/Orientation.h>

KnobsWidget::KnobsWidget(TrackManager& track_manager, MainWidget& main_widget)
    : m_track_manager(track_manager)
    , m_main_widget(main_widget)
{
    set_layout<GUI::HorizontalBoxLayout>();
    set_fill_with_background_color(true);

    m_octave_container = add<GUI::Widget>();
    m_octave_container->set_layout<GUI::VerticalBoxLayout>();
    m_octave_container->add<GUI::Label>("Octave");
    m_octave_value = m_octave_container->add<GUI::Label>(DeprecatedString::number(m_track_manager.keyboard()->virtual_keyboard_octave()));

    // FIXME: Implement vertical flipping in GUI::Slider, not here.
    m_octave_knob = m_octave_container->add<GUI::VerticalSlider>();
    m_octave_knob->set_tooltip("Z: octave down, X: octave up");
    m_octave_knob->set_range(octave_min - 1, octave_max - 1);
    m_octave_knob->set_value((octave_max - 1) - (m_track_manager.keyboard()->virtual_keyboard_octave() - 1));
    m_octave_knob->set_step(1);
    m_octave_knob->on_change = [this](int value) {
        int new_octave = octave_max - value;
        if (m_change_underlying)
            m_main_widget.set_octave_and_ensure_note_change(new_octave);
        VERIFY(new_octave == m_track_manager.keyboard()->virtual_keyboard_octave());
        m_octave_value->set_text(DeprecatedString::number(new_octave));
    };

    for (auto& parameter : m_track_manager.current_track()->track_mastering()->parameters())
        m_parameter_widgets.append(add<ProcessorParameterWidget>(parameter));

    for (auto& parameter : m_track_manager.current_track()->synth()->parameters())
        m_parameter_widgets.append(add<ProcessorParameterWidget>(parameter));

    for (auto& parameter : m_track_manager.current_track()->delay()->parameters())
        m_parameter_widgets.append(add<ProcessorParameterWidget>(parameter));
}

void KnobsWidget::update_knobs()
{
    // FIXME: This is needed because when the slider is changed normally, we
    // need to change the underlying value, but if the keyboard was used, we
    // need to change the slider without changing the underlying value.
    m_change_underlying = false;

    m_octave_knob->set_value(octave_max - m_track_manager.keyboard()->virtual_keyboard_octave());

    m_change_underlying = true;
}
