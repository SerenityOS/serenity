/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Forward.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/HTMLMediaElement.h>

namespace Web::HTML {

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
    RefPtr<Gfx::Bitmap> const& current_frame() const { return m_current_frame; }

private:
    HTMLVideoElement(DOM::Document&, DOM::QualifiedName);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    virtual JS::GCPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    JS::GCPtr<HTML::VideoTrack> m_video_track;
    RefPtr<Platform::Timer> m_video_timer;
    RefPtr<Gfx::Bitmap> m_current_frame;

    u32 m_video_width { 0 };
    u32 m_video_height { 0 };
};

}
