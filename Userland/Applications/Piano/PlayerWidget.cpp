/*
 * Copyright (c) 2021, JJ Roberts-White <computerfido@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PlayerWidget.h"

#include "AudioPlayerLoop.h"
#include "Music.h"
#include "TrackManager.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>

PlayerWidget::PlayerWidget(TrackManager& manager, AudioPlayerLoop& loop)
    : m_track_manager(manager)
    , m_audio_loop(loop)
{
    set_layout<GUI::HorizontalBoxLayout>();
    set_fill_with_background_color(true);
    m_track_number_choices.append("1");

    m_play_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/play.png").release_value_but_fixme_should_propagate_errors();
    m_pause_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/pause.png").release_value_but_fixme_should_propagate_errors();
    m_back_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-back.png").release_value_but_fixme_should_propagate_errors();    // Go back a note
    m_next_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-forward.png").release_value_but_fixme_should_propagate_errors(); // Advance a note
    m_add_track_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/plus.png").release_value_but_fixme_should_propagate_errors();
    m_next_track_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-last.png").release_value_but_fixme_should_propagate_errors();

    RefPtr<GUI::Label> label = add<GUI::Label>("Track");
    label->set_max_width(75);

    m_track_dropdown = add<GUI::ComboBox>();
    m_track_dropdown->set_max_width(75);
    m_track_dropdown->set_model(*GUI::ItemListModel<String>::create(m_track_number_choices));
    m_track_dropdown->set_only_allow_values_from_model(true);
    m_track_dropdown->set_model_column(0);
    m_track_dropdown->set_selected_index(0);
    m_track_dropdown->on_change = [this]([[maybe_unused]] auto name, GUI::ModelIndex model_index) {
        m_track_manager.set_current_track(model_index.row());
    };

    m_add_track_button = add<GUI::Button>();
    m_add_track_button->set_icon(*m_add_track_icon);
    m_add_track_button->set_fixed_width(30);
    m_add_track_button->set_tooltip("Add Track");
    m_add_track_button->set_focus_policy(GUI::FocusPolicy::NoFocus);
    m_add_track_button->on_click = [this](unsigned) {
        add_track();
    };

    m_next_track_button = add<GUI::Button>();
    m_next_track_button->set_icon(*m_next_track_icon);
    m_next_track_button->set_fixed_width(30);
    m_next_track_button->set_tooltip("Next Track");
    m_next_track_button->set_focus_policy(GUI::FocusPolicy::NoFocus);
    m_next_track_button->on_click = [this](unsigned) {
        next_track();
    };

    m_play_button = add<GUI::Button>();
    m_play_button->set_icon(*m_pause_icon);
    m_play_button->set_fixed_width(30);
    m_play_button->set_tooltip("Play/Pause playback");
    m_play_button->set_focus_policy(GUI::FocusPolicy::NoFocus);
    m_play_button->on_click = [this](unsigned) {
        m_audio_loop.toggle_paused();

        if (m_audio_loop.is_playing()) {
            m_play_button->set_icon(*m_pause_icon);
        } else {
            m_play_button->set_icon(*m_play_icon);
        }
    };

    m_back_button = add<GUI::Button>();
    m_back_button->set_icon(*m_back_icon);
    m_back_button->set_fixed_width(30);
    m_back_button->set_tooltip("Previous Note");
    m_back_button->set_focus_policy(GUI::FocusPolicy::NoFocus);
    m_back_button->on_click = [this](unsigned) {
        m_track_manager.time_forward(-(sample_rate / (beats_per_minute / 60) / notes_per_beat));
    };

    m_next_button = add<GUI::Button>();
    m_next_button->set_icon(*m_next_icon);
    m_next_button->set_fixed_width(30);
    m_next_button->set_tooltip("Next Note");
    m_next_button->set_focus_policy(GUI::FocusPolicy::NoFocus);
    m_next_button->on_click = [this](unsigned) {
        m_track_manager.time_forward((sample_rate / (beats_per_minute / 60) / notes_per_beat));
    };
}

PlayerWidget::~PlayerWidget()
{
}

void PlayerWidget::add_track()
{
    m_track_manager.add_track();
    auto latest_track_count = m_track_manager.track_count();
    auto latest_track_string = String::number(latest_track_count);
    m_track_number_choices.append(latest_track_string);
    m_track_dropdown->set_selected_index(latest_track_count - 1);
}

void PlayerWidget::next_track()
{
    m_track_dropdown->set_selected_index(m_track_manager.next_track_index());
}

void PlayerWidget::toggle_paused()
{
    m_play_button->click();
}
