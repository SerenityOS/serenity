/*
 * Copyright (c) 2021, JJ Roberts-White <computerfido@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Toolbar.h>

class AudioPlayerLoop;
class TrackManager;

class PlayerWidget final : public GUI::Toolbar {
    C_OBJECT(PlayerWidget)
public:
    virtual ~PlayerWidget() override;

private:
    explicit PlayerWidget(TrackManager&, AudioPlayerLoop&);

    TrackManager& m_track_manager;
    AudioPlayerLoop& m_audio_loop;

    RefPtr<Gfx::Bitmap> m_play_icon;
    RefPtr<Gfx::Bitmap> m_pause_icon;
    RefPtr<Gfx::Bitmap> m_back_icon;
    RefPtr<Gfx::Bitmap> m_next_icon;

    RefPtr<GUI::Button> m_play_button;
    RefPtr<GUI::Button> m_back_button;
    RefPtr<GUI::Button> m_next_button;
};
