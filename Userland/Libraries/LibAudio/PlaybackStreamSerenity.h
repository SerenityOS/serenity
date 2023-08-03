/*
 * Copyright (c) 2023, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibAudio/ConnectionToServer.h>
#include <LibAudio/PlaybackStream.h>

namespace Audio {

class PlaybackStreamSerenity final
    : public PlaybackStream {
public:
    static ErrorOr<NonnullRefPtr<PlaybackStream>> create(OutputState initial_state, u32 sample_rate, u8 channels, u32 target_latency_ms, AudioDataRequestCallback&& data_request_callback);

    virtual void set_underrun_callback(Function<void()>) override;

    virtual NonnullRefPtr<Core::ThreadedPromise<Duration>> resume() override;
    virtual NonnullRefPtr<Core::ThreadedPromise<void>> drain_buffer_and_suspend() override;
    virtual NonnullRefPtr<Core::ThreadedPromise<void>> discard_buffer_and_suspend() override;

    virtual ErrorOr<Duration> total_time_played() override;

    virtual NonnullRefPtr<Core::ThreadedPromise<void>> set_volume(double) override;

private:
    PlaybackStreamSerenity(NonnullRefPtr<ConnectionToServer>, NonnullRefPtr<Core::Timer> polling_timer, AudioDataRequestCallback&& data_request_callback);

    void fill_buffers();

    NonnullRefPtr<ConnectionToServer> m_connection;
    size_t m_number_of_samples_enqueued { 0 };
    NonnullRefPtr<Core::Timer> m_polling_timer;
    AudioDataRequestCallback m_data_request_callback;
    bool m_paused { false };
};

}
