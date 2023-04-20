/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLVideoElement.h>
#include <LibWeb/HTML/VideoTrack.h>
#include <LibWeb/Layout/VideoBox.h>

namespace Web::HTML {

HTMLVideoElement::HTMLVideoElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLMediaElement(document, move(qualified_name))
{
}

HTMLVideoElement::~HTMLVideoElement() = default;

JS::ThrowCompletionOr<void> HTMLVideoElement::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLVideoElementPrototype>(realm, "HTMLVideoElement"));

    return {};
}

void HTMLVideoElement::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_video_track);
}

JS::GCPtr<Layout::Node> HTMLVideoElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return heap().allocate_without_realm<Layout::VideoBox>(document(), *this, move(style));
}

Layout::VideoBox* HTMLVideoElement::layout_node()
{
    return static_cast<Layout::VideoBox*>(Node::layout_node());
}

Layout::VideoBox const* HTMLVideoElement::layout_node() const
{
    return static_cast<Layout::VideoBox const*>(Node::layout_node());
}

// https://html.spec.whatwg.org/multipage/media.html#dom-video-videowidth
u32 HTMLVideoElement::video_width() const
{
    // The videoWidth IDL attribute must return the intrinsic width of the video in CSS pixels. The videoHeight IDL
    // attribute must return the intrinsic height of the video in CSS pixels. If the element's readyState attribute
    // is HAVE_NOTHING, then the attributes must return 0.
    if (ready_state() == ReadyState::HaveNothing)
        return 0;
    return m_video_width;
}

// https://html.spec.whatwg.org/multipage/media.html#dom-video-videoheight
u32 HTMLVideoElement::video_height() const
{
    // The videoWidth IDL attribute must return the intrinsic width of the video in CSS pixels. The videoHeight IDL
    // attribute must return the intrinsic height of the video in CSS pixels. If the element's readyState attribute
    // is HAVE_NOTHING, then the attributes must return 0.
    if (ready_state() == ReadyState::HaveNothing)
        return 0;
    return m_video_height;
}

void HTMLVideoElement::set_video_track(JS::GCPtr<HTML::VideoTrack> video_track)
{
    set_needs_style_update(true);
    document().set_needs_layout();

    if (m_video_track)
        m_video_track->pause_video({});

    m_video_track = video_track;
}

void HTMLVideoElement::set_current_frame(Badge<VideoTrack>, RefPtr<Gfx::Bitmap> frame, double position)
{
    m_current_frame = { move(frame), position };
    layout_node()->set_needs_display();
}

void HTMLVideoElement::on_playing()
{
    if (m_video_track)
        m_video_track->play_video({});
}

void HTMLVideoElement::on_paused()
{
    if (m_video_track)
        m_video_track->pause_video({});
}

void HTMLVideoElement::on_seek(double position, MediaSeekMode seek_mode)
{
    if (m_video_track)
        m_video_track->seek(Time::from_milliseconds(position * 1000.0), seek_mode);
}

}
