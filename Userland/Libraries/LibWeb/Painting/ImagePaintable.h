/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Painting/PaintableBox.h>

namespace Web::Painting {

class ImagePaintable final
    : public PaintableBox
    , public HTML::BrowsingContext::ViewportClient {
    JS_CELL(ImagePaintable, PaintableBox);

public:
    static JS::NonnullGCPtr<ImagePaintable> create(Layout::ImageBox const&);

    virtual void paint(PaintContext&, PaintPhase) const override;

    Layout::ImageBox const& layout_box() const;

private:
    // ^JS::Cell
    virtual void finalize() override;

    // ^BrowsingContext::ViewportClient
    virtual void browsing_context_did_set_viewport_rect(CSSPixelRect const&) final;

    ImagePaintable(Layout::ImageBox const&);
};

}
