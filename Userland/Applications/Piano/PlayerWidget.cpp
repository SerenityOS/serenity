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

PlayerWidget::PlayerWidget(TrackManager& manager, AudioPlayerLoop& loop)
    : m_track_manager(manager)
    , m_audio_loop(loop)
{
    set_layout<GUI::HorizontalBoxLayout>();
    set_fill_with_background_color(true);

    m_play_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/play.png").release_value_but_fixme_should_propagate_errors();
    m_pause_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/pause.png").release_value_but_fixme_should_propagate_errors();
    m_back_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-back.png").release_value_but_fixme_should_propagate_errors();    // Go back a note
    m_next_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-forward.png").release_value_but_fixme_should_propagate_errors(); // Advance a note

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
