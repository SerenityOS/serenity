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
#include <LibWeb/Platform/Timer.h>

namespace Web::HTML {

// FIXME: Determine a reasonable framerate somehow. For now, this is roughly 24fps.
static constexpr int s_frame_delay_ms = 42;

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

    if (m_video_timer)
        m_video_timer->stop();

    m_video_track = video_track;
}

void HTMLVideoElement::on_playing()
{
    if (!m_video_timer) {
        m_video_timer = Platform::Timer::create_repeating(s_frame_delay_ms, [this]() {
            if (auto frame = m_video_track->next_frame())
                m_current_frame = move(frame);
            else
                m_video_timer->stop();

            layout_node()->set_needs_display();
        });
    }

    m_video_timer->start();
}

void HTMLVideoElement::on_paused()
{
    if (m_video_timer)
        m_video_timer->stop();
}

}
