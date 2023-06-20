/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AudioCodecPluginLadybird.h"
#include <AK/Endian.h>
#include <AK/MemoryStream.h>
#include <LibAudio/Loader.h>
#include <LibAudio/Sample.h>
#include <LibCore/SharedCircularQueue.h>
#include <QAudioFormat>
#include <QAudioSink>
#include <QByteArray>
#include <QMediaDevices>
#include <QThread>

namespace Ladybird {

static constexpr u32 UPDATE_RATE_MS = 50;

struct AudioTask {
    enum class Type {
        Stop,
        Play,
        Pause,
        Seek,
        Volume,
    };

    Type type;
    Optional<double> data {};
};

using AudioTaskQueue = Core::SharedSingleProducerCircularQueue<AudioTask>;

class AudioThread final : public QThread { // We have to use QThread, otherwise internal Qt media QTimer objects do not work.
    Q_OBJECT

public:
    static ErrorOr<NonnullOwnPtr<AudioThread>> create(NonnullRefPtr<Audio::Loader> loader)
    {
        auto task_queue = TRY(AudioTaskQueue::create());
        return adopt_nonnull_own_or_enomem(new (nothrow) AudioThread(move(loader), move(task_queue)));
    }

    ErrorOr<void> stop()
    {
        TRY(queue_task({ AudioTask::Type::Stop }));
        wait();

        return {};
    }

    Duration duration() const
    {
        return m_duration;
    }

    ErrorOr<void> queue_task(AudioTask task)
    {
        return m_task_queue.blocking_enqueue(move(task), []() {
            usleep(UPDATE_RATE_MS * 1000);
        });
    }

Q_SIGNALS:
    void playback_position_updated(Duration);

private:
    AudioThread(NonnullRefPtr<Audio::Loader> loader, AudioTaskQueue task_queue)
        : m_loader(move(loader))
        , m_task_queue(move(task_queue))
    {
        auto duration = static_cast<double>(m_loader->total_samples()) / static_cast<double>(m_loader->sample_rate());
        m_duration = Duration::from_milliseconds(static_cast<i64>(duration * 1000.0));

        m_samples_to_load_per_buffer = static_cast<size_t>(UPDATE_RATE_MS / 1000.0 * static_cast<double>(m_loader->sample_rate()));
    }

    enum class Paused {
        Yes,
        No,
    };

    void run() override
    {
        auto devices = make<QMediaDevices>();
        auto const& device_info = devices->defaultAudioOutput();

        auto format = device_info.preferredFormat();
        format.setSampleFormat(QAudioFormat::Int16);
        format.setChannelCount(2);

        auto audio_output = make<QAudioSink>(device_info, format);
        auto* io_device = audio_output->start();

        auto paused = Paused::Yes;

        while (true) {
            if (auto result = m_task_queue.dequeue(); result.is_error()) {
                VERIFY(result.error() == AudioTaskQueue::QueueStatus::Empty);
            } else {
                auto task = result.release_value();

                switch (task.type) {
                case AudioTask::Type::Stop:
                    return;

                case AudioTask::Type::Play:
                    audio_output->resume();
                    paused = Paused::No;
                    break;

                case AudioTask::Type::Pause:
                    audio_output->suspend();
                    paused = Paused::Yes;
                    break;

                case AudioTask::Type::Seek: {
                    VERIFY(task.data.has_value());
                    auto position = *task.data;

                    auto duration = static_cast<double>(this->duration().to_milliseconds()) / 1000.0;
                    position = position / duration * static_cast<double>(m_loader->total_samples());

                    m_loader->seek(static_cast<int>(position)).release_value_but_fixme_should_propagate_errors();

                    if (paused == Paused::Yes) {
                        m_position = Web::Platform::AudioCodecPlugin::current_loader_position(m_loader, audio_output->format().sampleRate());
                        Q_EMIT playback_position_updated(m_position);
                    }

                    break;
                }

                case AudioTask::Type::Volume:
                    VERIFY(task.data.has_value());
                    audio_output->setVolume(*task.data);
                    break;
                }
            }

            if (paused == Paused::No) {
                if (auto result = play_next_samples(*audio_output, *io_device); result.is_error()) {
                    // FIXME: Propagate the error to the HTMLMediaElement.
                } else {
                    Q_EMIT playback_position_updated(m_position);
                    paused = result.value();
                }
            }

            usleep(UPDATE_RATE_MS * 1000);
        }
    }

    ErrorOr<Paused> play_next_samples(QAudioSink& audio_output, QIODevice& io_device)
    {
        bool all_samples_loaded = m_loader->loaded_samples() >= m_loader->total_samples();

        if (all_samples_loaded) {
            audio_output.suspend();
            (void)m_loader->reset();

            m_position = m_duration;
            return Paused::Yes;
        }

        auto samples = TRY(Web::Platform::AudioCodecPlugin::read_samples_from_loader(*m_loader, m_samples_to_load_per_buffer, audio_output.format().sampleRate()));
        enqueue_samples(io_device, move(samples));

        m_position = Web::Platform::AudioCodecPlugin::current_loader_position(m_loader, audio_output.format().sampleRate());
        return Paused::No;
    }

    void enqueue_samples(QIODevice& io_device, FixedArray<Audio::Sample> samples)
    {
        auto buffer_size = samples.size() * 2 * sizeof(u16);
        if (buffer_size > static_cast<size_t>(m_sample_buffer.size()))
            m_sample_buffer.resize(buffer_size);

        FixedMemoryStream stream { Bytes { m_sample_buffer.data(), buffer_size } };

        for (auto& sample : samples) {
            LittleEndian<i16> pcm;

            pcm = static_cast<i16>(sample.left * NumericLimits<i16>::max());
            MUST(stream.write_value(pcm));

            pcm = static_cast<i16>(sample.right * NumericLimits<i16>::max());
            MUST(stream.write_value(pcm));
        }

        io_device.write(m_sample_buffer.data(), buffer_size);
    }

    NonnullRefPtr<Audio::Loader> m_loader;
    AudioTaskQueue m_task_queue;

    size_t m_samples_to_load_per_buffer { 0 };
    QByteArray m_sample_buffer;

    Duration m_duration;
    Duration m_position;
};

ErrorOr<NonnullOwnPtr<AudioCodecPluginLadybird>> AudioCodecPluginLadybird::create(NonnullRefPtr<Audio::Loader> loader)
{
    auto audio_thread = TRY(AudioThread::create(move(loader)));
    audio_thread->start();

    return adopt_nonnull_own_or_enomem(new (nothrow) AudioCodecPluginLadybird(move(audio_thread)));
}

AudioCodecPluginLadybird::AudioCodecPluginLadybird(NonnullOwnPtr<AudioThread> audio_thread)
    : m_audio_thread(move(audio_thread))
{
    connect(m_audio_thread, &AudioThread::playback_position_updated, this, [this](auto position) {
        if (on_playback_position_updated)
            on_playback_position_updated(position);
    });
}

AudioCodecPluginLadybird::~AudioCodecPluginLadybird()
{
    m_audio_thread->stop().release_value_but_fixme_should_propagate_errors();
}

void AudioCodecPluginLadybird::resume_playback()
{
    m_audio_thread->queue_task({ AudioTask::Type::Play }).release_value_but_fixme_should_propagate_errors();
}

void AudioCodecPluginLadybird::pause_playback()
{
    m_audio_thread->queue_task({ AudioTask::Type::Pause }).release_value_but_fixme_should_propagate_errors();
}

void AudioCodecPluginLadybird::set_volume(double volume)
{

    AudioTask task { AudioTask::Type::Volume };
    task.data = volume;

    m_audio_thread->queue_task(move(task)).release_value_but_fixme_should_propagate_errors();
}

void AudioCodecPluginLadybird::seek(double position)
{
    AudioTask task { AudioTask::Type::Seek };
    task.data = position;

    m_audio_thread->queue_task(move(task)).release_value_but_fixme_should_propagate_errors();
}

Duration AudioCodecPluginLadybird::duration()
{
    return m_audio_thread->duration();
}

}

#include "AudioCodecPluginLadybird.moc"
