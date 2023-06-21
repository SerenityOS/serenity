/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Forward.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/ReplacedBox.h>

namespace Web::Layout {

class VideoBox final
    : public ReplacedBox
    , public HTML::BrowsingContext::ViewportClient {
    JS_CELL(VideoBox, ReplacedBox);

public:
    virtual void prepare_for_replaced_layout() override;

    HTML::HTMLVideoElement& dom_node();
    HTML::HTMLVideoElement const& dom_node() const;

    virtual JS::GCPtr<Painting::Paintable> create_paintable() const override;

private:
    VideoBox(DOM::Document&, DOM::Element&, NonnullRefPtr<CSS::StyleProperties>);

    // ^BrowsingContext::ViewportClient
    virtual void browsing_context_did_set_viewport_rect(CSSPixelRect const&) final;

    // ^JS::Cell
    virtual void finalize() override;

    int preferred_width() const;
    int preferred_height() const;
};

}
