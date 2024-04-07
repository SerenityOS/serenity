/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Path.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Layout/FormattingContext.h>
#include <LibWeb/Layout/SVGSVGBox.h>
#include <LibWeb/Layout/SVGTextBox.h>
#include <LibWeb/Layout/SVGTextPathBox.h>

namespace Web::Layout {

class SVGFormattingContext : public FormattingContext {
public:
    explicit SVGFormattingContext(LayoutState&, Box const&, FormattingContext* parent, Gfx::AffineTransform parent_viewbox_transform = {});
    ~SVGFormattingContext();

    virtual void run(Box const&, LayoutMode, AvailableSpace const&) override;
    virtual CSSPixels automatic_content_width() const override;
    virtual CSSPixels automatic_content_height() const override;

private:
    void layout_svg_element(Box const&);
    void layout_nested_viewport(Box const&);
    void layout_container_element(SVGBox const&);
    void layout_graphics_element(SVGGraphicsBox const&);
    void layout_path_like_element(SVGGraphicsBox const&);
    void layout_mask_or_clip(SVGBox const&);

    Gfx::Path compute_path_for_text(SVGTextBox const&);
    Gfx::Path compute_path_for_text_path(SVGTextPathBox const&);

    Gfx::AffineTransform m_parent_viewbox_transform {};

    Optional<AvailableSpace> m_available_space {};
    Gfx::AffineTransform m_current_viewbox_transform {};
    CSSPixelSize m_viewport_size {};
    CSSPixelPoint m_svg_offset {};
};

}
