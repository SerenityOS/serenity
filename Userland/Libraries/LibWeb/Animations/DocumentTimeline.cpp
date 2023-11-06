/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Realm.h>
#include <LibWeb/Animations/DocumentTimeline.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Animations {

JS::NonnullGCPtr<DocumentTimeline> DocumentTimeline::create(JS::Realm& realm, DOM::Document& document, HighResolutionTime::DOMHighResTimeStamp origin_time)
{
    return realm.heap().allocate<DocumentTimeline>(realm, realm, document, origin_time);
}

// https://www.w3.org/TR/web-animations-1/#dom-documenttimeline-documenttimeline
WebIDL::ExceptionOr<JS::NonnullGCPtr<DocumentTimeline>> DocumentTimeline::construct_impl(JS::Realm& realm, DocumentTimelineOptions options)
{
    // Creates a new DocumentTimeline. The Document with which the timeline is associated is the Document associated
    // with the Window that is the current global object.
    auto& window = verify_cast<HTML::Window>(realm.global_object());
    return create(realm, window.associated_document(), options.origin_time);
}

// https://www.w3.org/TR/web-animations-1/#ref-for-timeline-time-to-origin-relative-time
Optional<double> DocumentTimeline::convert_a_timeline_time_to_an_original_relative_time(Optional<double> timeline_time)
{
    // To convert a timeline time, timeline time, to an origin-relative time for a document timeline, timeline, return
    // the sum of the timeline time and timeline’s origin time. If timeline is inactive, return an unresolved time
    // value.
    if (is_inactive() || !timeline_time.has_value())
        return {};
    return timeline_time.value() + m_origin_time;
}

// https://www.w3.org/TR/web-animations-1/#origin-time
WebIDL::ExceptionOr<void> DocumentTimeline::set_current_time(Optional<double> current_time)
{
    // A document timeline is a type of timeline that is associated with a document and whose current time is calculated
    // as a fixed offset from the now timestamp provided each time the update animations and send events procedure is
    // run. This fixed offset is referred to as the document timeline’s origin time.
    if (!current_time.has_value())
        TRY(Base::set_current_time({}));
    else
        TRY(Base::set_current_time(current_time.value() - m_origin_time));

    // After a document timeline becomes active, it is monotonically increasing.
    if (!is_inactive())
        VERIFY(is_monotonically_increasing());

    return {};
}

// https://www.w3.org/TR/web-animations-1/#document-timelines
bool DocumentTimeline::is_inactive() const
{
    // A document timeline that is associated with a Document which is not an active document is also considered to be
    // inactive.
    return Base::is_inactive() || !associated_document()->is_active();
}

DocumentTimeline::DocumentTimeline(JS::Realm& realm, DOM::Document& document, HighResolutionTime::DOMHighResTimeStamp origin_time)
    : AnimationTimeline(realm)
    , m_origin_time(origin_time)
{
    set_associated_document(document);
}

void DocumentTimeline::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::DocumentTimelinePrototype>(realm, "DocumentTimeline"));
}

}
