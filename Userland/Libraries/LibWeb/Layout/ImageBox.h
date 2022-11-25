/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/Layout/ReplacedBox.h>

namespace Web::Layout {

class ImageBox final
    : public ReplacedBox
    , public HTML::BrowsingContext::ViewportClient {
    JS_CELL(ImageBox, ReplacedBox);

public:
    ImageBox(DOM::Document&, DOM::Element&, NonnullRefPtr<CSS::StyleProperties>, ImageLoader const&);
    virtual ~ImageBox() override;

    virtual void prepare_for_replaced_layout() override;

    const DOM::Element& dom_node() const { return static_cast<const DOM::Element&>(ReplacedBox::dom_node()); }

    bool renders_as_alt_text() const;

    virtual RefPtr<Painting::Paintable> create_paintable() const override;

    auto const& image_loader() const { return m_image_loader; }

    void dom_node_did_update_alt_text(Badge<HTML::HTMLImageElement>);

private:
    // ^BrowsingContext::ViewportClient
    virtual void browsing_context_did_set_viewport_rect(CSSPixelRect const&) final;

    // ^JS::Cell
    virtual void finalize() override;

    int preferred_width() const;
    int preferred_height() const;

    ImageLoader const& m_image_loader;

    Optional<float> m_cached_alt_text_width;
};

}
