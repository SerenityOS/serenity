/*
 * Copyright (c) 2021, JJ Roberts-White <computerfido@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Toolbar.h>

class AudioPlayerLoop;
class TrackManager;
class MainWidget;

class PlayerWidget final : public GUI::Toolbar {
    C_OBJECT_ABSTRACT(PlayerWidget)
public:
    static ErrorOr<NonnullRefPtr<PlayerWidget>> try_create(TrackManager&, MainWidget&, AudioPlayerLoop&);
    virtual ~PlayerWidget() override = default;

    void add_track();
    void next_track();
    void toggle_paused();

private:
    explicit PlayerWidget(TrackManager&, MainWidget&, AudioPlayerLoop&);

    ErrorOr<void> initialize();

    TrackManager& m_track_manager;
    MainWidget& m_main_widget;
    AudioPlayerLoop& m_audio_loop;
    Vector<ByteString> m_track_number_choices;

    RefPtr<Gfx::Bitmap> m_play_icon;
    RefPtr<Gfx::Bitmap> m_pause_icon;
    RefPtr<Gfx::Bitmap> m_back_icon;
    RefPtr<Gfx::Bitmap> m_next_icon;
    RefPtr<Gfx::Bitmap> m_add_track_icon;
    RefPtr<Gfx::Bitmap> m_next_track_icon;

    RefPtr<GUI::ComboBox> m_track_dropdown;
    RefPtr<GUI::Button> m_play_button;
    RefPtr<GUI::Button> m_back_button;
    RefPtr<GUI::Button> m_next_button;
    RefPtr<GUI::Button> m_add_track_button;
    RefPtr<GUI::Button> m_next_track_button;
};
