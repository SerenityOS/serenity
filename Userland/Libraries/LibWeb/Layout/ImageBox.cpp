/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibGUI/Painter.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/ImageDecoder.h>
#include <LibGfx/StylePainter.h>
#include <LibWeb/Layout/ImageBox.h>

namespace Web::Layout {

ImageBox::ImageBox(DOM::Document& document, DOM::Element& element, NonnullRefPtr<CSS::StyleProperties> style, const ImageLoader& image_loader)
    : ReplacedBox(document, element, move(style))
    , m_image_loader(image_loader)
{
}

ImageBox::~ImageBox()
{
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
        set_has_intrinsic_width(true);
        set_has_intrinsic_height(true);
        set_intrinsic_width(0);
        set_intrinsic_height(0);
    } else {
        if (m_image_loader.width()) {
            set_has_intrinsic_width(true);
            set_intrinsic_width(m_image_loader.width());
        }
        if (m_image_loader.height()) {
            set_has_intrinsic_height(true);
            set_intrinsic_height(m_image_loader.height());
        }

        if (m_image_loader.width() && m_image_loader.height()) {
            set_has_intrinsic_ratio(true);
            set_intrinsic_ratio((float)m_image_loader.width() / (float)m_image_loader.height());
        } else {
            set_has_intrinsic_ratio(false);
        }
    }

    if (renders_as_alt_text()) {
        auto& image_element = downcast<HTML::HTMLImageElement>(dom_node());
        auto& font = Gfx::FontDatabase::default_font();
        auto alt = image_element.alt();
        if (alt.is_empty())
            alt = image_element.src();
        set_width(font.width(alt) + 16);
        set_height(font.glyph_height() + 16);
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
            auto& image_element = downcast<HTML::HTMLImageElement>(dom_node());
            context.painter().set_font(Gfx::FontDatabase::default_font());
            Gfx::StylePainter::paint_frame(context.painter(), enclosing_int_rect(absolute_rect()), context.palette(), Gfx::FrameShape::Container, Gfx::FrameShadow::Sunken, 2);
            auto alt = image_element.alt();
            if (alt.is_empty())
                alt = image_element.src();
            context.painter().draw_text(enclosing_int_rect(absolute_rect()), alt, Gfx::TextAlignment::Center, computed_values().color(), Gfx::TextElision::Right);
        } else if (auto bitmap = m_image_loader.bitmap(m_image_loader.current_frame_index())) {
            context.painter().draw_scaled_bitmap(enclosing_int_rect(absolute_rect()), *bitmap, bitmap->rect());
        }
    }
}

bool ImageBox::renders_as_alt_text() const
{
    if (is<HTML::HTMLImageElement>(dom_node()))
        return !m_image_loader.has_image();
    return false;
}

void ImageBox::set_visible_in_viewport(Badge<Layout::InitialContainingBlockBox>, bool visible_in_viewport)
{
    m_image_loader.set_visible_in_viewport(visible_in_viewport);
}

}
