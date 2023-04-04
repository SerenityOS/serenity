/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IDAllocator.h>
#include <LibJS/Runtime/Realm.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/VideoTrackPrototype.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/HTMLMediaElement.h>
#include <LibWeb/HTML/VideoTrack.h>
#include <LibWeb/HTML/VideoTrackList.h>

namespace Web::HTML {

static IDAllocator s_video_track_id_allocator;

VideoTrack::VideoTrack(JS::Realm& realm, JS::NonnullGCPtr<HTMLMediaElement> media_element, NonnullOwnPtr<Video::Matroska::MatroskaDemuxer> demuxer, Video::Track track)
    : PlatformObject(realm)
    , m_media_element(media_element)
    , m_demuxer(move(demuxer))
    , m_track(track)
{
}

VideoTrack::~VideoTrack()
{
    auto id = m_id.to_number<int>();
    VERIFY(id.has_value());

    s_video_track_id_allocator.deallocate(id.value());
}

JS::ThrowCompletionOr<void> VideoTrack::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::VideoTrackPrototype>(realm, "VideoTrack"));

    auto id = s_video_track_id_allocator.allocate();
    m_id = TRY_OR_THROW_OOM(realm.vm(), String::number(id));

    return {};
}

void VideoTrack::visit_edges(Cell::Visitor& visitor)
{
    visitor.visit(m_media_element);
    visitor.visit(m_video_track_list);
}

// https://html.spec.whatwg.org/multipage/media.html#dom-videotrack-selected
void VideoTrack::set_selected(bool selected)
{
    // On setting, it must select the track if the new value is true, and unselect it otherwise.
    if (m_selected == selected)
        return;

    // If the track is in a VideoTrackList, then all the other VideoTrack objects in that list must be unselected. (If the track is
    // no longer in a VideoTrackList object, then the track being selected or unselected has no effect beyond changing the value of
    // the attribute on the VideoTrack object.)
    if (m_video_track_list) {
        for (auto video_track : m_video_track_list->video_tracks({})) {
            if (video_track.ptr() != this)
                video_track->m_selected = false;
        }

        // Whenever a track in a VideoTrackList that was previously not selected is selected, and whenever the selected track in a
        // VideoTrackList is unselected without a new track being selected in its stead, the user agent must queue a media element
        // task given the media element to fire an event named change at the VideoTrackList object. This task must be queued before
        // the task that fires the resize event, if any.
        auto previously_unselected_track_is_selected = !m_selected && selected;
        auto selected_track_was_unselected_without_another_selection = m_selected && !selected;

        if (previously_unselected_track_is_selected || selected_track_was_unselected_without_another_selection) {
            m_media_element->queue_a_media_element_task([this]() {
                m_video_track_list->dispatch_event(DOM::Event::create(realm(), HTML::EventNames::change.to_deprecated_fly_string()).release_value_but_fixme_should_propagate_errors());
            });
        }
    }

    m_selected = selected;
}

}
