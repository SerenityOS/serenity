/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibGfx/Forward.h>
#include <LibWeb/DOM/DocumentLoadEventDelayer.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/HTMLMediaElement.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::HTML {

struct VideoFrame {
    RefPtr<Gfx::Bitmap> frame;
    double position { 0.0 };
};

class HTMLVideoElement final : public HTMLMediaElement {
    WEB_PLATFORM_OBJECT(HTMLVideoElement, HTMLMediaElement);

public:
    virtual ~HTMLVideoElement() override;

    Layout::VideoBox* layout_node();
    Layout::VideoBox const* layout_node() const;

    void set_video_width(u32 video_width) { m_video_width = video_width; }
    u32 video_width() const;

    void set_video_height(u32 video_height) { m_video_height = video_height; }
    u32 video_height() const;

    void set_video_track(JS::GCPtr<VideoTrack>);

    void set_current_frame(Badge<VideoTrack>, RefPtr<Gfx::Bitmap> frame, double position);
    VideoFrame const& current_frame() const { return m_current_frame; }
    RefPtr<Gfx::Bitmap> const& poster_frame() const { return m_poster_frame; }

    void set_layout_mouse_position(Badge<Painting::VideoPaintable>, Optional<CSSPixelPoint> mouse_position) { m_mouse_position = move(mouse_position); }
    Optional<CSSPixelPoint> const& layout_mouse_position(Badge<Painting::VideoPaintable>) const { return m_mouse_position; }

    struct CachedLayoutBoxes {
        Optional<CSSPixelRect> control_box_rect;
        Optional<CSSPixelRect> playback_button_rect;
        Optional<CSSPixelRect> timeline_rect;
    };
    CachedLayoutBoxes& cached_layout_boxes(Badge<Painting::VideoPaintable>) const { return m_layout_boxes; }

private:
    HTMLVideoElement(DOM::Document&, DOM::QualifiedName);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    virtual void parse_attribute(DeprecatedFlyString const& name, DeprecatedString const& value) override;
    virtual void did_remove_attribute(DeprecatedFlyString const&) override;

    virtual JS::GCPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    virtual void on_playing() override;
    virtual void on_paused() override;
    virtual void on_seek(double, MediaSeekMode) override;

    WebIDL::ExceptionOr<void> determine_element_poster_frame(Optional<StringView> const& poster);

    JS::GCPtr<HTML::VideoTrack> m_video_track;
    VideoFrame m_current_frame;
    RefPtr<Gfx::Bitmap> m_poster_frame;

    u32 m_video_width { 0 };
    u32 m_video_height { 0 };

    JS::GCPtr<Fetch::Infrastructure::FetchController> m_fetch_controller;
    Optional<DOM::DocumentLoadEventDelayer> m_load_event_delayer;

    // Cached state for layout
    Optional<CSSPixelPoint> m_mouse_position;
    mutable CachedLayoutBoxes m_layout_boxes;
};

}
