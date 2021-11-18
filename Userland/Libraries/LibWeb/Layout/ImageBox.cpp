/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/FontDatabase.h>
#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/ImageBox.h>

namespace Web::Layout {

ImageBox::ImageBox(DOM::Document& document, DOM::Element& element, NonnullRefPtr<CSS::StyleProperties> style, const ImageLoader& image_loader)
    : ReplacedBox(document, element, move(style))
    , m_image_loader(image_loader)
{
    browsing_context().register_viewport_client(*this);
}

ImageBox::~ImageBox()
{
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
        auto& font = Gfx::FontDatabase::default_font();
        auto alt = image_element.alt();
        if (alt.is_empty())
            alt = image_element.src();
        set_intrinsic_width(font.width(alt) + 16);
        set_intrinsic_height(font.glyph_height() + 16);
    }

    if (!has_intrinsic_width() && !has_intrinsic_height()) {
        set_width(16);
        set_height(16);
    }
}

void ImageBox::paint(PaintContext& context, PaintPhase phase)
{
    if (!is_visible())
        return;

    // FIXME: This should be done at a different level. Also rect() does not include padding etc!
    if (!context.viewport_rect().intersects(enclosing_int_rect(absolute_rect())))
        return;

    ReplacedBox::paint(context, phase);

    if (phase == PaintPhase::Foreground) {
        if (renders_as_alt_text()) {
            auto& image_element = verify_cast<HTML::HTMLImageElement>(dom_node());
            context.painter().set_font(Gfx::FontDatabase::default_font());
            Gfx::StylePainter::paint_frame(context.painter(), enclosing_int_rect(absolute_rect()), context.palette(), Gfx::FrameShape::Container, Gfx::FrameShadow::Sunken, 2);
            auto alt = image_element.alt();
            if (alt.is_empty())
                alt = image_element.src();
            context.painter().draw_text(enclosing_int_rect(absolute_rect()), alt, Gfx::TextAlignment::Center, computed_values().color(), Gfx::TextElision::Right);
        } else if (auto bitmap = m_image_loader.bitmap(m_image_loader.current_frame_index())) {
            context.painter().draw_scaled_bitmap(rounded_int_rect(absolute_rect()), *bitmap, bitmap->rect(), 1.0f, Gfx::Painter::ScalingMode::BilinearBlend);
        }
    }
}

bool ImageBox::renders_as_alt_text() const
{
    if (is<HTML::HTMLImageElement>(dom_node()))
        return !m_image_loader.has_image();
    return false;
}

void ImageBox::browsing_context_did_set_viewport_rect(Gfx::IntRect const& viewport_rect)
{
    m_image_loader.set_visible_in_viewport(viewport_rect.to_type<float>().intersects(absolute_rect()));
}

}
