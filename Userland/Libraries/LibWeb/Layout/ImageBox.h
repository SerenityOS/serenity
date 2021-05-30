/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/Layout/ReplacedBox.h>
#include <LibWeb/Page/BrowsingContext.h>

namespace Web::Layout {

class ImageBox
    : public ReplacedBox
    , public BrowsingContext::ViewportClient {
public:
    ImageBox(DOM::Document&, DOM::Element&, NonnullRefPtr<CSS::StyleProperties>, const ImageLoader&);
    virtual ~ImageBox() override;

    virtual void prepare_for_replaced_layout() override;
    virtual void paint(PaintContext&, PaintPhase) override;

    const DOM::Element& dom_node() const { return static_cast<const DOM::Element&>(ReplacedBox::dom_node()); }

    bool renders_as_alt_text() const;

private:
    virtual void frame_did_set_viewport_rect(const Gfx::IntRect&) final;

    int preferred_width() const;
    int preferred_height() const;

    const ImageLoader& m_image_loader;
};

}
