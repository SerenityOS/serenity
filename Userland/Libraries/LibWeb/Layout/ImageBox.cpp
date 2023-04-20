/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Painting/ImagePaintable.h>
#include <LibWeb/Platform/FontPlugin.h>

namespace Web::Layout {

ImageBox::ImageBox(DOM::Document& document, DOM::Element& element, NonnullRefPtr<CSS::StyleProperties> style, ImageLoader const& image_loader)
    : ReplacedBox(document, element, move(style))
    , m_image_loader(image_loader)
{
    browsing_context().register_viewport_client(*this);
}

ImageBox::~ImageBox() = default;

void ImageBox::finalize()
{
    Base::finalize();

    // NOTE: We unregister from the browsing context in finalize() to avoid trouble
    //       in the scenario where our BrowsingContext has already been swept by GC.
    browsing_context().unregister_viewport_client(*this);
}

int ImageBox::preferred_width() const
{
    return dom_node().attribute(HTML::AttributeNames::width).to_int().value_or(m_image_loader.width());
}

int ImageBox::preferred_height() const
{
    return dom_node().attribute(HTML::AttributeNames::height).to_int().value_or(m_image_loader.height());
}

void ImageBox::prepare_for_replaced_layout()
{
    if (!m_image_loader.has_loaded_or_failed()) {
        set_intrinsic_width(0);
        set_intrinsic_height(0);
    } else {
        if (m_image_loader.width()) {
            set_intrinsic_width(m_image_loader.width());
        }
        if (m_image_loader.height()) {
            set_intrinsic_height(m_image_loader.height());
        }

        if (m_image_loader.width() && m_image_loader.height()) {
            set_intrinsic_aspect_ratio((float)m_image_loader.width() / (float)m_image_loader.height());
        } else {
            set_intrinsic_aspect_ratio({});
        }
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
        return !m_image_loader.has_image();
    return false;
}

void ImageBox::browsing_context_did_set_viewport_rect(CSSPixelRect const& viewport_rect)
{
    m_image_loader.set_visible_in_viewport(paintable_box() && viewport_rect.intersects(paintable_box()->absolute_rect()));
}

JS::GCPtr<Painting::Paintable> ImageBox::create_paintable() const
{
    return Painting::ImagePaintable::create(*this);
}

}
