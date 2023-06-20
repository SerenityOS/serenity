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

static constexpr u32 UPDATE_RATE_MS = 10;

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

        auto bytes_available = audio_output.bytesFree();
        auto bytes_per_sample = audio_output.format().bytesPerSample();
        auto channel_count = audio_output.format().channelCount();
        auto samples_to_load = bytes_available / bytes_per_sample / channel_count;

        auto samples = TRY(Web::Platform::AudioCodecPlugin::read_samples_from_loader(*m_loader, samples_to_load, audio_output.format().sampleRate()));
        enqueue_samples(audio_output, io_device, move(samples));

        m_position = Web::Platform::AudioCodecPlugin::current_loader_position(m_loader, audio_output.format().sampleRate());
        return Paused::No;
    }

    void enqueue_samples(QAudioSink const& audio_output, QIODevice& io_device, FixedArray<Audio::Sample> samples)
    {
        auto buffer_size = samples.size() * audio_output.format().bytesPerSample() * audio_output.format().channelCount();

        if (buffer_size > static_cast<size_t>(m_sample_buffer.size()))
            m_sample_buffer.resize(buffer_size);

        FixedMemoryStream stream { Bytes { m_sample_buffer.data(), buffer_size } };

        for (auto const& sample : samples) {
            switch (audio_output.format().sampleFormat()) {
            case QAudioFormat::UInt8:
                write_sample<u8>(stream, sample.left);
                write_sample<u8>(stream, sample.right);
                break;
            case QAudioFormat::Int16:
                write_sample<i16>(stream, sample.left);
                write_sample<i16>(stream, sample.right);
                break;
            case QAudioFormat::Int32:
                write_sample<i32>(stream, sample.left);
                write_sample<i32>(stream, sample.right);
                break;
            case QAudioFormat::Float:
                write_sample<float>(stream, sample.left);
                write_sample<float>(stream, sample.right);
                break;
            default:
                VERIFY_NOT_REACHED();
            }
        }

        io_device.write(m_sample_buffer.data(), buffer_size);
    }

    template<typename T>
    void write_sample(FixedMemoryStream& stream, float sample)
    {
        // The values that need to be written to the stream vary depending on the output channel format, and isn't
        // particularly well documented. The value derivations performed below were adapted from a Qt example:
        // https://code.qt.io/cgit/qt/qtmultimedia.git/tree/examples/multimedia/audiooutput/audiooutput.cpp?h=6.4.2#n46
        LittleEndian<T> pcm;

        if constexpr (IsSame<T, u8>)
            pcm = static_cast<u8>((sample + 1.0f) / 2 * NumericLimits<u8>::max());
        else if constexpr (IsSame<T, i16>)
            pcm = static_cast<i16>(sample * NumericLimits<i16>::max());
        else if constexpr (IsSame<T, i32>)
            pcm = static_cast<i32>(sample * NumericLimits<i32>::max());
        else if constexpr (IsSame<T, float>)
            pcm = sample;
        else
            static_assert(DependentFalse<T>);

        MUST(stream.write_value(pcm));
    }

    NonnullRefPtr<Audio::Loader> m_loader;
    AudioTaskQueue m_task_queue;

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
