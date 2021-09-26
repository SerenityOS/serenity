/*
 * Copyright (c) 2021, Leandro A. F. Pereira <leandro@tia.mat.br>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AmpWidget.h"
#include "BarsVisualizationWidget.h"
#include "PlaybackManager.h"
#include "Player.h"
#include "PlaylistWidget.h"
#include <AK/NonnullRefPtr.h>
#include <LibAudio/ClientConnection.h>
#include <LibGUI/Window.h>

class AmpPlayerView final : public AmpWidget
    , public Player {
    C_OBJECT(AmpPlayerView)

public:
    explicit AmpPlayerView(GUI::Window&, Audio::ClientConnection&);

    virtual void play_state_changed(PlayState) override;
    virtual void loop_mode_changed(LoopMode) override;
    virtual void time_elapsed(int) override;
    virtual void file_name_changed(StringView) override;
    virtual void volume_changed(double) override;
    virtual void total_samples_changed(int) override;
    virtual void sound_buffer_played(RefPtr<Audio::Buffer>, int sample_rate, int samples_played) override;
    virtual void playlist_loaded(StringView, bool) override;
    virtual void audio_load_error(StringView, StringView) override;
    virtual void shuffle_mode_changed(ShuffleMode) override;

    void set_playlist_visible(bool);
    bool playlist_visible() const { return m_playlist_window->is_visible(); }

    template<typename T>
    void set_visualization()
    {
        set_visualization_widget(add<T>());
    }

private:
    void drop_event(GUI::DropEvent& event) override;
    GUI::Window& m_window;

    bool m_nonlinear_volume_slider;
    size_t m_device_sample_rate { 44100 };

    RefPtr<GUI::Window> m_playlist_window;
    RefPtr<PlaylistWidget> m_playlist_widget;
};
