/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "PlaybackManager.h"
#include "Player.h"
#include "VisualizationWidget.h"
#include <AK/FixedArray.h>
#include <AK/NonnullRefPtr.h>
#include <LibAudio/ConnectionToServer.h>
#include <LibGUI/HorizontalSlider.h>
#include <LibGUI/Slider.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/Widget.h>
#include <LibImageDecoderClient/Client.h>

class SoundPlayerWidget final : public GUI::Widget
    , public Player {
    C_OBJECT(SoundPlayerWidget)

public:
    void set_nonlinear_volume_slider(bool nonlinear);
    void set_playlist_visible(bool visible);
    RefPtr<Gfx::Bitmap> get_image_from_music_file();

    template<typename T, typename... Args>
    void set_visualization(Args... args)
    {
        m_visualization->remove_from_parent();
        update();
        auto new_visualization = T::construct(move(args)...);
        m_player_view->insert_child_before(new_visualization, *static_cast<Core::EventReceiver*>(m_playback_progress_slider.ptr()));
        m_visualization = new_visualization;
        if (!loaded_filename().is_empty())
            m_visualization->start_new_file(loaded_filename());
    }

    virtual void play_state_changed(PlayState) override;
    virtual void loop_mode_changed(LoopMode) override;
    virtual void shuffle_mode_changed(ShuffleMode) override;
    virtual void time_elapsed(int) override;
    virtual void file_name_changed(StringView) override;
    virtual void playlist_loaded(StringView, bool) override;
    virtual void audio_load_error(StringView path, StringView error_reason) override;
    virtual void volume_changed(double) override;
    virtual void mute_changed(bool) override;
    virtual void total_samples_changed(int) override;
    virtual void sound_buffer_played(FixedArray<Audio::Sample> const&, int sample_rate, int samples_played) override;

protected:
    void keydown_event(GUI::KeyEvent&) override;

private:
    SoundPlayerWidget(GUI::Window&, Audio::ConnectionToServer&, ImageDecoderClient::Client&);

    void sync_previous_next_actions();

    void drag_enter_event(GUI::DragEvent& event) override;
    void drop_event(GUI::DropEvent& event) override;
    GUI::Window& m_window;
    ImageDecoderClient::Client& m_image_decoder_client;

    RefPtr<GUI::HorizontalSplitter> m_splitter;
    RefPtr<GUI::Widget> m_player_view;
    RefPtr<PlaylistWidget> m_playlist_widget;
    RefPtr<VisualizationWidget> m_visualization;

    RefPtr<Gfx::Bitmap> m_play_icon;
    RefPtr<Gfx::Bitmap> m_pause_icon;
    RefPtr<Gfx::Bitmap> m_stop_icon;
    RefPtr<Gfx::Bitmap> m_back_icon;
    RefPtr<Gfx::Bitmap> m_next_icon;
    RefPtr<Gfx::Bitmap> m_volume_icon;
    RefPtr<Gfx::Bitmap> m_muted_icon;

    RefPtr<GUI::Action> m_play_action;
    RefPtr<GUI::Action> m_stop_action;
    RefPtr<GUI::Action> m_back_action;
    RefPtr<GUI::Action> m_next_action;
    RefPtr<GUI::Action> m_mute_action;

    RefPtr<GUI::HorizontalSlider> m_playback_progress_slider;
    RefPtr<GUI::Label> m_volume_label;
    RefPtr<GUI::HorizontalSlider> m_volume_slider;
    RefPtr<GUI::Label> m_timestamp_label;

    bool m_nonlinear_volume_slider;
};
