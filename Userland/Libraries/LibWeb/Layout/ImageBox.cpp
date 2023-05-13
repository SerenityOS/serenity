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
    auto bitmap = m_image_provider.current_image_bitmap();

    if (!bitmap) {
        set_intrinsic_width(0);
        set_intrinsic_height(0);
    } else {
        auto width = bitmap->width();
        auto height = bitmap->height();
        set_intrinsic_width(width);
        set_intrinsic_height(height);
        set_intrinsic_aspect_ratio(static_cast<float>(width) / static_cast<float>(height));
    }

    if (renders_as_alt_text()) {
        auto& image_element = verify_cast<HTML::HTMLImageElement>(dom_node());
        auto& font = Platform::FontPlugin::the().default_font();
        auto alt = image_element.alt();

        CSSPixels alt_text_width = 0;
        if (!m_cached_alt_text_width.has_value())
            m_cached_alt_text_width = font.width(alt);
        alt_text_width = m_cached_alt_text_width.value();

        set_intrinsic_width(alt_text_width + 16);
        set_intrinsic_height(font.pixel_size() + 16);
    }

    if (!has_intrinsic_width() && !has_intrinsic_height()) {
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
