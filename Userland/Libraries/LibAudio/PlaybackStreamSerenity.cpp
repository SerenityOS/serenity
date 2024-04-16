/*
 * Copyright (c) 2023, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PlaybackStreamSerenity.h"

#include <LibCore/ThreadedPromise.h>

namespace Audio {

ErrorOr<NonnullRefPtr<PlaybackStream>> PlaybackStreamSerenity::create(OutputState initial_state, u32 sample_rate, u8 channels, [[maybe_unused]] u32 target_latency_ms, AudioDataRequestCallback&& data_request_callback)
{
    // ConnectionToServer can only handle stereo audio currently. If it is able to accept mono audio
    // later, this can be removed.
    VERIFY(channels == 2);

    VERIFY(data_request_callback);
    auto connection = TRY(ConnectionToServer::try_create());
    if (auto result = connection->try_set_self_sample_rate(sample_rate); result.is_error())
        return Error::from_string_literal("Failed to set sample rate");

    auto polling_timer = Core::Timer::create();
    auto implementation = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) PlaybackStreamSerenity(connection, move(polling_timer), move(data_request_callback))));
    if (initial_state == OutputState::Playing)
        connection->async_start_playback();
    return implementation;
}

PlaybackStreamSerenity::PlaybackStreamSerenity(NonnullRefPtr<ConnectionToServer> stream, NonnullRefPtr<Core::Timer> polling_timer, AudioDataRequestCallback&& data_request_callback)
    : m_connection(move(stream))
    , m_polling_timer(move(polling_timer))
    , m_data_request_callback(move(data_request_callback))
{
    // Ensure that our audio buffers are filled when they are more than 3/4 empty.
    // FIXME: Add an event to ConnectionToServer track the sample rate and update this interval, or
    //        implement the data request into ConnectionToServer so each client doesn't need to poll
    //        on a timer with an arbitrary interval.
    m_polling_timer->set_interval(static_cast<int>((AUDIO_BUFFERS_COUNT * 3 / 4) * AUDIO_BUFFER_SIZE * 1000 / m_connection->get_self_sample_rate()));
    m_polling_timer->on_timeout = [this]() {
        fill_buffers();
    };
    m_polling_timer->start();
}

void PlaybackStreamSerenity::fill_buffers()
{
    while (m_connection->can_enqueue()) {
        Array<Sample, AUDIO_BUFFER_SIZE> buffer;
        buffer.fill({ 0.0f, 0.0f });
        auto written_data = m_data_request_callback(Bytes { reinterpret_cast<u8*>(buffer.data()), sizeof(buffer) }, PcmSampleFormat::Float32, AUDIO_BUFFER_SIZE);
        // FIXME: The buffer we are enqueuing here is a fixed size, meaning that the server will not be
        //        aware of exactly how many samples we have written here. We should allow the server to
        //        consume sized buffers to allow us to obtain sample-accurate timing information even
        //        when we run out of samples on a sample count that is not a multiple of AUDIO_BUFFER_SIZE.
        m_number_of_samples_enqueued += written_data.size() / sizeof(Sample);
        MUST(m_connection->realtime_enqueue(buffer));
    }
}

void PlaybackStreamSerenity::set_underrun_callback(Function<void()> callback)
{
    // FIXME: Implement underrun callback in AudioServer
    (void)callback;
}

NonnullRefPtr<Core::ThreadedPromise<Duration>> PlaybackStreamSerenity::resume()
{
    auto promise = Core::ThreadedPromise<Duration>::create();
    // FIXME: We need to get the time played at the correct time from the server. If a message to
    //        start playback is sent while there is any other message being processed, this may end
    //        up being inaccurate.
    auto time = MUST(total_time_played());
    fill_buffers();
    m_connection->async_start_playback();
    m_polling_timer->start();
    promise->resolve(move(time));
    return promise;
}

NonnullRefPtr<Core::ThreadedPromise<void>> PlaybackStreamSerenity::drain_buffer_and_suspend()
{
    // FIXME: Play back all samples on the server before pausing. This can be achieved by stopping
    //        enqueuing samples and receiving a message that a buffer underrun has occurred.
    auto promise = Core::ThreadedPromise<void>::create();
    m_connection->async_pause_playback();
    m_polling_timer->stop();
    promise->resolve();
    return promise;
}

NonnullRefPtr<Core::ThreadedPromise<void>> PlaybackStreamSerenity::discard_buffer_and_suspend()
{
    auto promise = Core::ThreadedPromise<void>::create();
    m_connection->async_clear_buffer();
    m_connection->async_pause_playback();
    m_polling_timer->stop();
    promise->resolve();
    return promise;
}

ErrorOr<Duration> PlaybackStreamSerenity::total_time_played()
{
    return Duration::from_milliseconds(m_number_of_samples_enqueued * 1000 / m_connection->get_self_sample_rate());
}

NonnullRefPtr<Core::ThreadedPromise<void>> PlaybackStreamSerenity::set_volume(double volume)
{
    auto promise = Core::ThreadedPromise<void>::create();
    m_connection->async_set_self_volume(volume);
    promise->resolve();
    return promise;
}

}
