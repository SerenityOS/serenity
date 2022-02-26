/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Queue.h>
#include <AK/Vector.h>
#include <LibAudio/Buffer.h>
#include <LibAudio/ConnectionFromClient.h>
#include <LibAudio/Loader.h>
#include <LibCore/Timer.h>

class PlaybackManager final {
public:
    PlaybackManager(NonnullRefPtr<Audio::ConnectionFromClient>);
    ~PlaybackManager() = default;

    void play();
    void stop();
    void pause();
    void seek(const int position);
    void loop(bool);
    bool toggle_pause();
    void set_loader(NonnullRefPtr<Audio::Loader>&&);
    RefPtr<Audio::Loader> loader() const { return m_loader; }
    size_t device_sample_rate() const { return m_device_sample_rate; }

    int last_seek() const { return m_last_seek; }
    bool is_paused() const { return m_paused; }
    float total_length() const { return m_total_length; }
    RefPtr<Audio::Buffer> current_buffer() const { return m_current_buffer; }

    NonnullRefPtr<Audio::ConnectionFromClient> connection() const { return m_connection; }

    Function<void()> on_update;
    Function<void(Audio::Buffer&)> on_load_sample_buffer;
    Function<void()> on_finished_playing;

private:
    // Number of buffers we want to always keep enqueued.
    static constexpr size_t always_enqueued_buffer_count = 2;

    void next_buffer();
    void set_paused(bool);

    bool m_paused { true };
    bool m_loop = { false };
    size_t m_last_seek { 0 };
    float m_total_length { 0 };
    size_t m_device_sample_rate { 44100 };
    size_t m_device_samples_per_buffer { 0 };
    size_t m_source_buffer_size_bytes { 0 };
    RefPtr<Audio::Loader> m_loader { nullptr };
    NonnullRefPtr<Audio::ConnectionFromClient> m_connection;
    RefPtr<Audio::Buffer> m_current_buffer;
    Queue<i32, always_enqueued_buffer_count + 1> m_enqueued_buffers;
    Optional<Audio::ResampleHelper<double>> m_resampler;
    RefPtr<Core::Timer> m_timer;

    // Controls the GUI update rate. A smaller value makes the visualizations nicer.
    static constexpr u32 update_rate_ms = 50;
    // Number of milliseconds of audio data contained in each audio buffer
    static constexpr u32 buffer_size_ms = 100;
};
