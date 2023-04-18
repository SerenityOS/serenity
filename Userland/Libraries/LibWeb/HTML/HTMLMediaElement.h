/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Time.h>
#include <AK/Variant.h>
#include <LibJS/Heap/MarkedVector.h>
#include <LibJS/SafeFunction.h>
#include <LibWeb/HTML/EventLoop/Task.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/WebIDL/DOMException.h>
#include <math.h>

namespace Web::HTML {

enum class MediaSeekMode {
    Accurate,
    ApproximateForSpeed,
};

class HTMLMediaElement : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLMediaElement, HTMLElement);

public:
    virtual ~HTMLMediaElement() override;

    void queue_a_media_element_task(JS::SafeFunction<void()> steps);

    enum class NetworkState : u16 {
        Empty,
        Idle,
        Loading,
        NoSource,
    };
    NetworkState network_state() const { return m_network_state; }

    WebIDL::ExceptionOr<JS::NonnullGCPtr<TimeRanges>> buffered() const;

    WebIDL::ExceptionOr<Bindings::CanPlayTypeResult> can_play_type(DeprecatedString const& type) const;

    enum class ReadyState : u16 {
        HaveNothing,
        HaveMetadata,
        HaveCurrentData,
        HaveFutureData,
        HaveEnoughData,
    };
    ReadyState ready_state() const { return m_ready_state; }

    bool seeking() const { return m_seeking; }

    WebIDL::ExceptionOr<void> load();

    double current_time() const;
    void set_current_time(double);
    void set_current_playback_position(double);

    double duration() const;
    bool paused() const { return m_paused; }
    bool ended() const;
    WebIDL::ExceptionOr<JS::NonnullGCPtr<JS::Promise>> play();
    WebIDL::ExceptionOr<void> pause();

    JS::NonnullGCPtr<VideoTrackList> video_tracks() const { return *m_video_tracks; }

protected:
    HTMLMediaElement(DOM::Document&, DOM::QualifiedName);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    // Override in subclasses to handle implementation-specific behavior when the element state changes
    // to playing or paused, e.g. to start/stop play timers.
    virtual void on_playing() { }
    virtual void on_paused() { }

    // Override in subclasses to handle implementation-specific seeking behavior. When seeking is complete,
    // subclasses must invoke set_current_playback_position() to unblock the user agent.
    virtual void on_seek(double, MediaSeekMode) { m_seek_in_progress = false; }

private:
    struct EntireResource { };
    using ByteRange = Variant<EntireResource>; // FIXME: This will need to include "until end" and an actual byte range.

    virtual void parse_attribute(DeprecatedFlyString const& name, DeprecatedString const& value) override;

    Task::Source media_element_event_task_source() const { return m_media_element_event_task_source.source; }

    WebIDL::ExceptionOr<void> load_element();
    WebIDL::ExceptionOr<void> select_resource();
    WebIDL::ExceptionOr<void> fetch_resource(AK::URL const&, Function<void()> failure_callback);
    static bool verify_response(JS::NonnullGCPtr<Fetch::Infrastructure::Response>, ByteRange const&);
    WebIDL::ExceptionOr<void> process_media_data(Function<void()> failure_callback);
    WebIDL::ExceptionOr<void> handle_media_source_failure(Span<JS::NonnullGCPtr<WebIDL::Promise>> promises);
    void forget_media_resource_specific_tracks();
    void set_ready_state(ReadyState);

    WebIDL::ExceptionOr<void> play_element();
    WebIDL::ExceptionOr<void> pause_element();
    void seek_element(double playback_position, MediaSeekMode = MediaSeekMode::Accurate);
    void notify_about_playing();
    void set_paused(bool);
    void set_duration(double);

    bool is_eligible_for_autoplay() const;
    bool has_ended_playback() const;
    WebIDL::ExceptionOr<void> reached_end_of_media_playback();

    WebIDL::ExceptionOr<void> dispatch_time_update_event();

    enum class TimeMarchesOnReason {
        NormalPlayback,
        Other,
    };
    void time_marches_on(TimeMarchesOnReason = TimeMarchesOnReason::NormalPlayback);

    JS::MarkedVector<JS::NonnullGCPtr<WebIDL::Promise>> take_pending_play_promises();
    void resolve_pending_play_promises(ReadonlySpan<JS::NonnullGCPtr<WebIDL::Promise>> promises);
    void reject_pending_play_promises(ReadonlySpan<JS::NonnullGCPtr<WebIDL::Promise>> promises, JS::NonnullGCPtr<WebIDL::DOMException> error);

    // https://html.spec.whatwg.org/multipage/media.html#reject-pending-play-promises
    template<typename ErrorType>
    void reject_pending_play_promises(ReadonlySpan<JS::NonnullGCPtr<WebIDL::Promise>> promises, FlyString const& message)
    {
        auto& realm = this->realm();

        auto error = ErrorType::create(realm, message.to_deprecated_fly_string());
        reject_pending_play_promises(promises, error);
    }

    // https://html.spec.whatwg.org/multipage/media.html#media-element-event-task-source
    UniqueTaskSource m_media_element_event_task_source {};

    // https://html.spec.whatwg.org/multipage/media.html#dom-media-networkstate
    NetworkState m_network_state { NetworkState::Empty };

    // https://html.spec.whatwg.org/multipage/media.html#dom-media-readystate
    ReadyState m_ready_state { ReadyState::HaveNothing };
    bool m_first_data_load_event_since_load_start { false };

    // https://html.spec.whatwg.org/multipage/media.html#dom-media-seeking
    bool m_seeking { false };

    // https://html.spec.whatwg.org/multipage/media.html#current-playback-position
    double m_current_playback_position { 0 };

    // https://html.spec.whatwg.org/multipage/media.html#official-playback-position
    double m_official_playback_position { 0 };

    // https://html.spec.whatwg.org/multipage/media.html#default-playback-start-position
    double m_default_playback_start_position { 0 };

    // https://html.spec.whatwg.org/multipage/media.html#dom-media-duration
    double m_duration { NAN };

    // https://html.spec.whatwg.org/multipage/media.html#list-of-pending-play-promises
    JS::MarkedVector<JS::NonnullGCPtr<WebIDL::Promise>> m_pending_play_promises;

    // https://html.spec.whatwg.org/multipage/media.html#dom-media-paused
    bool m_paused { true };

    // https://html.spec.whatwg.org/multipage/media.html#dom-media-videotracks
    JS::GCPtr<VideoTrackList> m_video_tracks;

    // https://html.spec.whatwg.org/multipage/media.html#media-data
    ByteBuffer m_media_data;

    // https://html.spec.whatwg.org/multipage/media.html#can-autoplay-flag
    bool m_can_autoplay { true };

    bool m_running_time_update_event_handler { false };
    Optional<Time> m_last_time_update_event_time;

    JS::GCPtr<Fetch::Infrastructure::FetchController> m_fetch_controller;

    bool m_seek_in_progress = false;
};

}
