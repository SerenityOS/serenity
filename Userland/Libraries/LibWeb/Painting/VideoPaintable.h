/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Forward.h>
#include <LibWeb/Painting/PaintableBox.h>

namespace Web::Painting {

class VideoPaintable final : public PaintableBox {
    JS_CELL(VideoPaintable, PaintableBox);

public:
    static JS::NonnullGCPtr<VideoPaintable> create(Layout::VideoBox const&);

    virtual void paint(PaintContext&, PaintPhase) const override;

    Layout::VideoBox& layout_box();
    Layout::VideoBox const& layout_box() const;

private:
    VideoPaintable(Layout::VideoBox const&);

    virtual bool wants_mouse_events() const override { return true; }
    virtual DispatchEventOfSameName handle_mouseup(Badge<EventHandler>, CSSPixelPoint, unsigned button, unsigned modifiers) override;
    virtual DispatchEventOfSameName handle_mousemove(Badge<EventHandler>, CSSPixelPoint, unsigned buttons, unsigned modifiers) override;

    void paint_loaded_video_controls(PaintContext&, HTML::HTMLVideoElement const&, DevicePixelRect video_rect, Optional<DevicePixelPoint> const& mouse_position) const;
    void paint_placeholder_video_controls(PaintContext&, DevicePixelRect video_rect, Optional<DevicePixelPoint> const& mouse_position) const;
};

}
