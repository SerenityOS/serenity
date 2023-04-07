/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Variant.h>
#include <LibJS/SafeFunction.h>
#include <LibWeb/HTML/EventLoop/Task.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <math.h>

namespace Web::HTML {

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

    WebIDL::ExceptionOr<Bindings::CanPlayTypeResult> can_play_type(DeprecatedString const& type) const;

    enum class ReadyState : u16 {
        HaveNothing,
        HaveMetadata,
        HaveCurrentData,
        HaveFutureData,
        HaveEnoughData,
    };
    ReadyState ready_state() const { return m_ready_state; }

    WebIDL::ExceptionOr<void> load();
    double duration() const;
    bool paused() const { return m_paused; }
    void pause() const;

    JS::NonnullGCPtr<VideoTrackList> video_tracks() const { return *m_video_tracks; }

protected:
    HTMLMediaElement(DOM::Document&, DOM::QualifiedName);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    // Override in subclasses to handle implementation-specific behavior when the element state changes
    // to playing or paused, e.g. to start/stop play timers.
    virtual void on_playing() { }
    virtual void on_paused() { }

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
    WebIDL::ExceptionOr<void> handle_media_source_failure();
    void forget_media_resource_specific_tracks();
    void set_ready_state(ReadyState);

    void notify_about_playing();
    void set_paused(bool);
    void set_duration(double);

    // https://html.spec.whatwg.org/multipage/media.html#media-element-event-task-source
    UniqueTaskSource m_media_element_event_task_source {};

    // https://html.spec.whatwg.org/multipage/media.html#dom-media-networkstate
    NetworkState m_network_state { NetworkState::Empty };

    // https://html.spec.whatwg.org/multipage/media.html#dom-media-readystate
    ReadyState m_ready_state { ReadyState::HaveNothing };
    bool m_first_data_load_event_since_load_start { false };

    // https://html.spec.whatwg.org/multipage/media.html#dom-media-duration
    double m_duration { NAN };

    // https://html.spec.whatwg.org/multipage/media.html#dom-media-paused
    bool m_paused { true };

    // https://html.spec.whatwg.org/multipage/media.html#dom-media-videotracks
    JS::GCPtr<VideoTrackList> m_video_tracks;

    // https://html.spec.whatwg.org/multipage/media.html#media-data
    ByteBuffer m_media_data;

    JS::GCPtr<Fetch::Infrastructure::FetchController> m_fetch_controller;
};

}
