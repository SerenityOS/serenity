/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLVideoElement.h>
#include <LibWeb/Layout/VideoBox.h>
#include <LibWeb/Painting/VideoPaintable.h>

namespace Web::Layout {

VideoBox::VideoBox(DOM::Document& document, DOM::Element& element, NonnullRefPtr<CSS::StyleProperties> style)
    : ReplacedBox(document, element, move(style))
{
    browsing_context().register_viewport_client(*this);
}

void VideoBox::finalize()
{
    Base::finalize();

    // NOTE: We unregister from the browsing context in finalize() to avoid trouble
    //       in the scenario where our BrowsingContext has already been swept by GC.
    browsing_context().unregister_viewport_client(*this);
}

HTML::HTMLVideoElement& VideoBox::dom_node()
{
    return static_cast<HTML::HTMLVideoElement&>(ReplacedBox::dom_node());
}

HTML::HTMLVideoElement const& VideoBox::dom_node() const
{
    return static_cast<HTML::HTMLVideoElement const&>(ReplacedBox::dom_node());
}

int VideoBox::preferred_width() const
{
    return dom_node().attribute(HTML::AttributeNames::width).to_int().value_or(dom_node().video_width());
}

int VideoBox::preferred_height() const
{
    return dom_node().attribute(HTML::AttributeNames::height).to_int().value_or(dom_node().video_height());
}

void VideoBox::prepare_for_replaced_layout()
{
    auto width = static_cast<float>(dom_node().video_width());
    set_intrinsic_width(width);

    auto height = static_cast<float>(dom_node().video_height());
    set_intrinsic_height(height);

    if (width != 0 && height != 0)
        set_intrinsic_aspect_ratio(width / height);
    else
        set_intrinsic_aspect_ratio({});
}

void VideoBox::browsing_context_did_set_viewport_rect(CSSPixelRect const&)
{
    // FIXME: Several steps in HTMLMediaElement indicate we may optionally handle whether the media object
    //        is in view. Implement those steps.
}

JS::GCPtr<Painting::Paintable> VideoBox::create_paintable() const
{
    return Painting::VideoPaintable::create(*this);
}

}
