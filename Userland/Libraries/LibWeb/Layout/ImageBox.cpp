/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/DecodedImageData.h>
#include <LibWeb/HTML/ImageRequest.h>
#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Painting/ImagePaintable.h>
#include <LibWeb/Platform/FontPlugin.h>

namespace Web::Layout {

ImageBox::ImageBox(DOM::Document& document, DOM::Element& element, NonnullRefPtr<CSS::StyleProperties> style, ImageProvider const& image_provider)
    : ReplacedBox(document, element, move(style))
    , m_image_provider(image_provider)
{
}

ImageBox::~ImageBox() = default;

void ImageBox::prepare_for_replaced_layout()
{
    set_natural_width(m_image_provider.intrinsic_width());
    set_natural_height(m_image_provider.intrinsic_height());
    set_natural_aspect_ratio(m_image_provider.intrinsic_aspect_ratio());

    if (renders_as_alt_text()) {
        auto& image_element = verify_cast<HTML::HTMLImageElement>(dom_node());
        auto& font = Platform::FontPlugin::the().default_font();
        auto alt = image_element.alt();

        CSSPixels alt_text_width = 0;
        if (!m_cached_alt_text_width.has_value())
            m_cached_alt_text_width = CSSPixels(font.width(alt));
        alt_text_width = m_cached_alt_text_width.value();

        set_natural_width(alt_text_width + 16);
        set_natural_height(CSSPixels(font.pixel_size()) + 16);
    }

    if (!has_natural_width() && !has_natural_height()) {
        // FIXME: Do something.
    }
}

void ImageBox::dom_node_did_update_alt_text(Badge<HTML::HTMLImageElement>)
{
    m_cached_alt_text_width = {};
}

bool ImageBox::renders_as_alt_text() const
{
    if (is<HTML::HTMLImageElement>(dom_node()))
        return !static_cast<HTML::HTMLImageElement const&>(dom_node()).current_request().is_available();
    return false;
}

JS::GCPtr<Painting::Paintable> ImageBox::create_paintable() const
{
    return Painting::ImagePaintable::create(*this);
}

}
