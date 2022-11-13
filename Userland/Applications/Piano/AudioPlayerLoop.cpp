/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2021, JJ Roberts-White <computerfido@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AudioPlayerLoop.h"
#include "Music.h"
#include "TrackManager.h"
#include <AK/Assertions.h>
#include <AK/FixedArray.h>
#include <AK/Forward.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/NumericLimits.h>
#include <AK/StdLibExtras.h>
#include <LibAudio/ConnectionToServer.h>
#include <LibAudio/Queue.h>
#include <LibAudio/Resampler.h>
#include <LibAudio/Sample.h>
#include <LibIPC/Connection.h>
#include <LibThreading/Thread.h>
#include <sched.h>
#include <time.h>

struct AudioLoopResponsivenessTimer final : public IPC::ResponsivenessTimer {
    AudioLoopResponsivenessTimer(IPC::ConnectionBase& parent)
        : IPC::ResponsivenessTimer(parent)
    {
        update_time();
    }

    void update_time()
    {
        clock_gettime(CLOCK_REALTIME, &last_check_time);
        if (!is_running)
            return;

        if (ns_duration_between(start_time, last_check_time) >= IPC::RESPONSIVENESS_TIMEOUT_MS * 1'000'000LL) {
            timer_expired();
            stop();
        }
    }

    static long ns_duration_between(timespec start, timespec end)
    {
        auto s_duration = static_cast<i64>(end.tv_sec - start.tv_sec);
        auto ns_duration = end.tv_nsec - start.tv_nsec;
        return s_duration * 1'000'000'000 + ns_duration;
    }

    virtual void start() override
    {
        start_time = last_check_time;
        is_running = true;
    }
    virtual void stop() override { is_running = false; }

    // FIXME: Replace raw POSIX time APIs with something nicer, cf. Java's Instant
    timespec last_check_time {};
    timespec start_time {};
    bool is_running { false };
};

struct AudioLoopDeferredInvoker final : public IPC::DeferredInvoker {
    static constexpr size_t INLINE_FUNCTIONS = 4;

    virtual ~AudioLoopDeferredInvoker() = default;

    virtual void schedule(Function<void()> function) override
    {
        deferred_functions.append(move(function));
    }

    void run_functions()
    {
        if (deferred_functions.size() > INLINE_FUNCTIONS)
            dbgln("Warning: Audio loop has more than {} deferred functions, audio might glitch!", INLINE_FUNCTIONS);
        if (!deferred_functions.is_empty())
            dbgln("running {} functions", deferred_functions.size());
        while (!deferred_functions.is_empty()) {
            auto function = deferred_functions.take_last();
            function();
        }
    }

    Vector<Function<void()>, INLINE_FUNCTIONS> deferred_functions;
};

AudioPlayerLoop::AudioPlayerLoop(TrackManager& track_manager, Atomic<bool>& need_to_write_wav, Atomic<int>& wav_percent_written, Threading::MutexProtected<Audio::WavWriter>& wav_writer)
    : m_track_manager(track_manager)
    , m_buffer(FixedArray<DSP::Sample>::must_create_but_fixme_should_propagate_errors(sample_count))
    , m_pipeline_thread(Threading::Thread::construct([this]() {
        return this->pipeline_thread_main();
    },
          "Audio pipeline"sv))
    , m_need_to_write_wav(need_to_write_wav)
    , m_wav_percent_written(wav_percent_written)
    , m_wav_writer(wav_writer)
{
    m_audio_client = Audio::ConnectionToServer::try_create().release_value_but_fixme_should_propagate_errors();

    auto target_sample_rate = m_audio_client->get_sample_rate();
    if (target_sample_rate == 0)
        target_sample_rate = Music::sample_rate;
    m_resampler = Audio::ResampleHelper<DSP::Sample>(Music::sample_rate, target_sample_rate);

    MUST(m_pipeline_thread->set_priority(sched_get_priority_max(0)));
    m_pipeline_thread->start();
}

AudioPlayerLoop::~AudioPlayerLoop()
{
    // Tell the pipeline to exit and wait for the last audio cycle to finish.
    m_exit_requested.store(true);
    auto result = m_pipeline_thread->join();
    // FIXME: Get rid of the EINVAL/ESRCH check once we allow to join dead threads.
    VERIFY(!result.is_error() || result.error() == EINVAL || result.error() == ESRCH);

    m_audio_client->shutdown();
}

intptr_t AudioPlayerLoop::pipeline_thread_main()
{
    m_audio_timer = &static_cast<AudioLoopResponsivenessTimer&>(m_audio_client->set_responsiveness_timer(make<AudioLoopResponsivenessTimer>(*m_audio_client)));
    auto& deferred_invoker = static_cast<AudioLoopDeferredInvoker&>(m_audio_client->set_deferred_invoker(make<AudioLoopDeferredInvoker>()));
    m_audio_client->async_start_playback();

    while (!m_exit_requested.load()) {
        m_audio_timer->update_time();
        deferred_invoker.run_functions();

        // The track manager guards against allocations itself.
        m_track_manager.fill_buffer(m_buffer);

        auto result = send_audio_to_server();
        // Tolerate errors in the audio pipeline; we don't want this thread to crash the program. This might likely happen with OOM.
        if (result.is_error()) [[unlikely]] {
            dbgln("Error in audio pipeline: {}", result.error());
            m_track_manager.reset();
        }

        write_wav_if_needed();
    }
    m_audio_client->async_pause_playback();
    return static_cast<intptr_t>(0);
}

ErrorOr<void> AudioPlayerLoop::send_audio_to_server()
{
    TRY(m_remaining_samples.try_extend(TRY(m_resampler->try_resample(m_buffer))));

    auto sample_rate = static_cast<double>(m_resampler->target());
    auto buffer_play_time_ns = 1'000'000'000.0 / (sample_rate / static_cast<double>(Audio::AUDIO_BUFFER_SIZE));
    auto good_sleep_time = Time::from_nanoseconds(static_cast<unsigned>(buffer_play_time_ns)).to_timespec();

    size_t start_of_chunk_to_write = 0;
    while (start_of_chunk_to_write + Audio::AUDIO_BUFFER_SIZE <= m_remaining_samples.size()) {
        auto const exact_chunk = m_remaining_samples.span().slice(start_of_chunk_to_write, Audio::AUDIO_BUFFER_SIZE);
        auto exact_chunk_array = Array<Audio::Sample, Audio::AUDIO_BUFFER_SIZE> {};
        AK::TypedTransfer<Audio::Sample>::copy(exact_chunk_array.data(), exact_chunk.data(), Audio::AUDIO_BUFFER_SIZE);

        TRY(m_audio_client->blocking_realtime_enqueue(exact_chunk_array, [&]() {
            nanosleep(&good_sleep_time, nullptr);
            m_audio_timer->update_time();
        }));

        start_of_chunk_to_write += Audio::AUDIO_BUFFER_SIZE;
    }
    m_remaining_samples.remove(0, start_of_chunk_to_write);
    VERIFY(m_remaining_samples.size() < Audio::AUDIO_BUFFER_SIZE);

    return {};
}

void AudioPlayerLoop::write_wav_if_needed()
{
    bool _true = true;
    if (m_need_to_write_wav.compare_exchange_strong(_true, false)) {
        m_audio_client->async_pause_playback();
        m_wav_writer.with_locked([this](auto& wav_writer) {
            m_track_manager.reset();
            m_track_manager.set_should_loop(false);
            do {
                m_audio_timer->update_time();
                // FIXME: This progress detection is crude.
                m_wav_percent_written.store(static_cast<int>(static_cast<float>(m_track_manager.transport()->time()) / roll_length * 100.0f));
                m_track_manager.fill_buffer(m_buffer);
                wav_writer.write_samples(m_buffer.span());
            } while (m_track_manager.transport()->time());
            m_wav_percent_written.store(100);
            m_track_manager.reset();
            m_track_manager.set_should_loop(true);
            wav_writer.finalize();
        });
        m_audio_client->async_start_playback();
    }
}

void AudioPlayerLoop::toggle_paused()
{
    m_should_play_audio = !m_should_play_audio;

    if (m_should_play_audio)
        m_audio_client->async_start_playback();
    else
        m_audio_client->async_pause_playback();
}
