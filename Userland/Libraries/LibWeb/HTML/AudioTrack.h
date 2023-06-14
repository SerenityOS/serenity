/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Time.h>
#include <LibAudio/Forward.h>
#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::HTML {

class AudioTrack final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(AudioTrack, Bindings::PlatformObject);

public:
    virtual ~AudioTrack() override;

    void set_audio_track_list(Badge<AudioTrackList>, JS::GCPtr<AudioTrackList> audio_track_list) { m_audio_track_list = audio_track_list; }

    void play(Badge<HTMLAudioElement>);
    void pause(Badge<HTMLAudioElement>);

    Duration position() const;
    Duration duration() const;
    void seek(double, MediaSeekMode);

    void update_volume();

    String const& id() const { return m_id; }
    String const& kind() const { return m_kind; }
    String const& label() const { return m_label; }
    String const& language() const { return m_language; }

    bool enabled() const { return m_enabled; }
    void set_enabled(bool enabled);

private:
    AudioTrack(JS::Realm&, JS::NonnullGCPtr<HTMLMediaElement>, NonnullRefPtr<Audio::Loader>);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    Optional<FixedArray<Audio::Sample>> get_next_samples();
    void play_next_samples();

    // https://html.spec.whatwg.org/multipage/media.html#dom-audiotrack-id
    String m_id;

    // https://html.spec.whatwg.org/multipage/media.html#dom-audiotrack-kind
    String m_kind;

    // https://html.spec.whatwg.org/multipage/media.html#dom-audiotrack-label
    String m_label;

    // https://html.spec.whatwg.org/multipage/media.html#dom-audiotrack-language
    String m_language;

    // https://html.spec.whatwg.org/multipage/media.html#dom-audiotrack-enabled
    bool m_enabled { false };

    JS::NonnullGCPtr<HTMLMediaElement> m_media_element;
    JS::GCPtr<AudioTrackList> m_audio_track_list;

    NonnullOwnPtr<Platform::AudioCodecPlugin> m_audio_plugin;
    NonnullRefPtr<Audio::Loader> m_loader;
    NonnullRefPtr<Platform::Timer> m_sample_timer;
};

}
