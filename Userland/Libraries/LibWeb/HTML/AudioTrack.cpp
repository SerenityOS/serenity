/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IDAllocator.h>
#include <AK/Time.h>
#include <LibAudio/Loader.h>
#include <LibAudio/Resampler.h>
#include <LibAudio/Sample.h>
#include <LibJS/Runtime/Realm.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/AudioTrackPrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/AudioTrack.h>
#include <LibWeb/HTML/AudioTrackList.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/HTMLMediaElement.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Platform/AudioCodecPlugin.h>
#include <LibWeb/Platform/Timer.h>

namespace Web::HTML {

static IDAllocator s_audio_track_id_allocator;

// Number of milliseconds of audio data contained in each audio buffer
static constexpr u32 BUFFER_SIZE_MS = 50;

AudioTrack::AudioTrack(JS::Realm& realm, JS::NonnullGCPtr<HTMLMediaElement> media_element, NonnullRefPtr<Audio::Loader> loader)
    : PlatformObject(realm)
    , m_media_element(media_element)
    , m_audio_plugin(Platform::AudioCodecPlugin::create().release_value_but_fixme_should_propagate_errors())
    , m_loader(move(loader))
    , m_sample_timer(Platform::Timer::create_repeating(BUFFER_SIZE_MS, [this]() {
        play_next_samples();
    }))
{
    m_audio_plugin->device_sample_rate();
}

AudioTrack::~AudioTrack()
{
    auto id = m_id.to_number<int>();
    VERIFY(id.has_value());

    s_audio_track_id_allocator.deallocate(id.value());
}

JS::ThrowCompletionOr<void> AudioTrack::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::AudioTrackPrototype>(realm, "AudioTrack"));

    auto id = s_audio_track_id_allocator.allocate();
    m_id = TRY_OR_THROW_OOM(realm.vm(), String::number(id));

    return {};
}

void AudioTrack::play(Badge<HTMLAudioElement>)
{
    m_audio_plugin->resume_playback();
    m_sample_timer->start();
}

void AudioTrack::pause(Badge<HTMLAudioElement>)
{
    m_audio_plugin->pause_playback();
    m_sample_timer->stop();
}

Duration AudioTrack::position() const
{
    auto samples_played = static_cast<double>(m_loader->loaded_samples());
    auto sample_rate = static_cast<double>(m_loader->sample_rate());

    auto source_to_device_ratio = sample_rate / static_cast<double>(m_audio_plugin->device_sample_rate());
    samples_played *= source_to_device_ratio;

    return Duration::from_milliseconds(static_cast<i64>(samples_played / sample_rate * 1000.0));
}

Duration AudioTrack::duration() const
{
    auto duration = static_cast<double>(m_loader->total_samples()) / static_cast<double>(m_loader->sample_rate());
    return Duration::from_milliseconds(static_cast<i64>(duration * 1000.0));
}

void AudioTrack::seek(double position, MediaSeekMode seek_mode)
{
    // FIXME: Implement seeking mode.
    (void)seek_mode;

    auto duration = static_cast<double>(this->duration().to_milliseconds()) / 1000.0;
    position = position / duration * static_cast<double>(m_loader->total_samples());

    m_loader->seek(position).release_value_but_fixme_should_propagate_errors();
}

void AudioTrack::update_volume()
{
    m_audio_plugin->set_volume(m_media_element->effective_media_volume());
}

void AudioTrack::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_media_element);
    visitor.visit(m_audio_track_list);
}

// https://html.spec.whatwg.org/multipage/media.html#dom-audiotrack-enabled
void AudioTrack::set_enabled(bool enabled)
{
    // On setting, it must enable the track if the new value is true, and disable it otherwise. (If the track is no
    // longer in an AudioTrackList object, then the track being enabled or disabled has no effect beyond changing the
    // value of the attribute on the AudioTrack object.)
    if (m_enabled == enabled)
        return;

    if (m_audio_track_list) {
        // Whenever an audio track in an AudioTrackList that was disabled is enabled, and whenever one that was enabled
        // is disabled, the user agent must queue a media element task given the media element to fire an event named
        // change at the AudioTrackList object.
        m_media_element->queue_a_media_element_task([this]() {
            m_audio_track_list->dispatch_event(DOM::Event::create(realm(), HTML::EventNames::change).release_value_but_fixme_should_propagate_errors());
        });
    }

    m_enabled = enabled;
}

Optional<FixedArray<Audio::Sample>> AudioTrack::get_next_samples()
{
    bool all_samples_loaded = m_loader->loaded_samples() >= m_loader->total_samples();
    bool audio_server_done = m_audio_plugin->remaining_samples() == 0;

    if (all_samples_loaded && audio_server_done)
        return {};

    auto samples_to_load_per_buffer = static_cast<size_t>(BUFFER_SIZE_MS / 1000.0f * static_cast<float>(m_loader->sample_rate()));

    auto buffer_or_error = m_loader->get_more_samples(samples_to_load_per_buffer);
    if (buffer_or_error.is_error()) {
        dbgln("Error while loading samples: {}", buffer_or_error.error().description);
        return {};
    }

    return buffer_or_error.release_value();
}

void AudioTrack::play_next_samples()
{
    if (auto* layout_node = m_media_element->layout_node())
        layout_node->set_needs_display();

    auto samples = get_next_samples();
    if (!samples.has_value()) {
        m_audio_plugin->playback_ended();
        (void)m_loader->reset();

        auto playback_position = static_cast<double>(duration().to_milliseconds()) / 1000.0;
        m_media_element->set_current_playback_position(playback_position);

        return;
    }

    Audio::ResampleHelper<Audio::Sample> resampler(m_loader->sample_rate(), m_audio_plugin->device_sample_rate());
    auto resampled = FixedArray<Audio::Sample>::create(resampler.resample(samples.release_value()).span()).release_value_but_fixme_should_propagate_errors();

    m_audio_plugin->enqueue_samples(move(resampled));

    auto playback_position = static_cast<double>(position().to_milliseconds()) / 1000.0;
    m_media_element->set_current_playback_position(playback_position);
}

}
