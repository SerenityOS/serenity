/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 * Copyright (c) 2021, Leandro A. F. Pereira <leandro@tia.mat.br>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Player.h"

Player::Player(Audio::ClientConnection& audio_client_connection)
    : m_audio_client_connection(audio_client_connection)
    , m_playback_manager(audio_client_connection)
{
    m_playback_manager.on_update = [&]() {
        auto samples_played = m_audio_client_connection.get_played_samples();
        auto sample_rate = m_playback_manager.loader()->sample_rate();
        float source_to_dest_ratio = static_cast<float>(sample_rate) / m_playback_manager.device_sample_rate();
        samples_played *= source_to_dest_ratio;
        samples_played += m_playback_manager.last_seek();

        auto played_seconds = samples_played / sample_rate;
        time_elapsed(played_seconds);
        sound_buffer_played(m_playback_manager.current_buffer(), m_playback_manager.device_sample_rate(), samples_played);
    };
    m_playback_manager.on_finished_playing = [&]() {
        set_play_state(PlayState::Stopped);

        switch (loop_mode()) {
        case LoopMode::File:
            play_file_path(loaded_filename());
            return;
        case LoopMode::Playlist:
            play_file_path(m_playlist.next());
            return;
        case LoopMode::None:
            return;
        }
    };
}

void Player::play_file_path(String const& path)
{
    if (path.is_null())
        return;

    if (!Core::File::exists(path)) {
        audio_load_error(path, "File does not exist");
        return;
    }

    if (path.ends_with(".m3u", AK::CaseSensitivity::CaseInsensitive) || path.ends_with(".m3u8", AK::CaseSensitivity::CaseInsensitive)) {
        playlist_loaded(path, m_playlist.load(path));
        return;
    }

    auto maybe_loader = Audio::Loader::create(path);
    if (maybe_loader.is_error()) {
        audio_load_error(path, maybe_loader.error().description);
        return;
    }
    auto loader = maybe_loader.value();

    m_loaded_filename = path;

    file_name_changed(path);
    total_samples_changed(loader->total_samples());
    m_playback_manager.set_loader(move(loader));

    play();
}

void Player::set_play_state(PlayState state)
{
    if (m_play_state != state) {
        m_play_state = state;
        play_state_changed(state);
    }
}

void Player::set_loop_mode(LoopMode mode)
{
    if (m_loop_mode != mode) {
        m_loop_mode = mode;
        m_playlist.set_looping(mode == LoopMode::Playlist);
        loop_mode_changed(mode);
    }
}

void Player::set_volume(double volume)
{
    m_volume = clamp(volume, 0, 1.5);
    m_audio_client_connection.set_self_volume(m_volume);
    volume_changed(m_volume);
}

void Player::set_shuffle_mode(ShuffleMode mode)
{
    if (m_shuffle_mode != mode) {
        m_shuffle_mode = mode;
        m_playlist.set_shuffling(mode == ShuffleMode::Shuffling);
        shuffle_mode_changed(mode);
    }
}

void Player::play()
{
    m_playback_manager.play();
    set_play_state(PlayState::Playing);
}

void Player::pause()
{
    m_playback_manager.pause();
    set_play_state(PlayState::Paused);
}

void Player::toggle_pause()
{
    bool paused = m_playback_manager.toggle_pause();
    set_play_state(paused ? PlayState::Paused : PlayState::Playing);
}

void Player::stop()
{
    m_playback_manager.stop();
    set_play_state(PlayState::Stopped);
}

void Player::seek(int sample)
{
    m_playback_manager.seek(sample);
}
