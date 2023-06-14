/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AudioCodecPluginLadybird.h"
#include <AK/Endian.h>
#include <AK/MemoryStream.h>
#include <LibAudio/Sample.h>
#include <QAudioFormat>
#include <QAudioSink>
#include <QBuffer>
#include <QMediaDevices>

namespace Ladybird {

ErrorOr<NonnullOwnPtr<AudioCodecPluginLadybird>> AudioCodecPluginLadybird::create()
{
    auto devices = TRY(adopt_nonnull_own_or_enomem(new (nothrow) QMediaDevices()));
    auto const& device_info = devices->defaultAudioOutput();

    auto format = device_info.preferredFormat();
    format.setSampleFormat(QAudioFormat::Int16);
    format.setChannelCount(2);

    if (!device_info.isFormatSupported(format))
        return Error::from_string_literal("Audio device format not supported");

    auto audio_output = TRY(adopt_nonnull_own_or_enomem(new (nothrow) QAudioSink(device_info, format)));

    return adopt_nonnull_own_or_enomem(new (nothrow) AudioCodecPluginLadybird(move(devices), move(audio_output)));
}

AudioCodecPluginLadybird::AudioCodecPluginLadybird(NonnullOwnPtr<QMediaDevices> devices, NonnullOwnPtr<QAudioSink> audio_output)
    : m_devices(move(devices))
    , m_audio_output(move(audio_output))
    , m_io_device(m_audio_output->start())
{
}

AudioCodecPluginLadybird::~AudioCodecPluginLadybird() = default;

size_t AudioCodecPluginLadybird::device_sample_rate()
{
    return m_audio_output->format().sampleRate();
}

void AudioCodecPluginLadybird::enqueue_samples(FixedArray<Audio::Sample> samples)
{
    QByteArray buffer;
    buffer.resize(samples.size() * 2 * sizeof(u16));

    FixedMemoryStream stream { Bytes { buffer.data(), static_cast<size_t>(buffer.size()) } };

    for (auto& sample : samples) {
        LittleEndian<i16> pcm;

        pcm = static_cast<i16>(sample.left * NumericLimits<i16>::max());
        MUST(stream.write_value(pcm));

        pcm = static_cast<i16>(sample.right * NumericLimits<i16>::max());
        MUST(stream.write_value(pcm));
    }

    m_io_device->write(buffer.data(), buffer.size());
}

size_t AudioCodecPluginLadybird::remaining_samples() const
{
    return 0;
}

void AudioCodecPluginLadybird::resume_playback()
{
    m_audio_output->resume();
}

void AudioCodecPluginLadybird::pause_playback()
{
    m_audio_output->suspend();
}

void AudioCodecPluginLadybird::playback_ended()
{
    m_audio_output->suspend();
}

void AudioCodecPluginLadybird::set_volume(double volume)
{
    m_audio_output->setVolume(volume);
}

}
