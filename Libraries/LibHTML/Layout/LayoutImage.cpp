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

#include <LibGfx/Font.h>
#include <LibGfx/StylePainter.h>
#include <LibGUI/Painter.h>
#include <LibHTML/Layout/LayoutImage.h>

LayoutImage::LayoutImage(const HTMLImageElement& element, NonnullRefPtr<StyleProperties> style)
    : LayoutReplaced(element, move(style))
{
}

LayoutImage::~LayoutImage()
{
}

void LayoutImage::layout()
{
    if (node().preferred_width() && node().preferred_height()) {
        rect().set_width(node().preferred_width());
        rect().set_height(node().preferred_height());
    } else if (renders_as_alt_text()) {
        auto& font = Gfx::Font::default_font();
        auto alt = node().alt();
        if (alt.is_empty())
            alt = node().src();
        rect().set_width(font.width(alt) + 16);
        rect().set_height(font.glyph_height() + 16);
    } else {
        rect().set_width(16);
        rect().set_height(16);
    }

    LayoutReplaced::layout();
}

void LayoutImage::render(RenderingContext& context)
{
    if (!is_visible())
        return;

    // FIXME: This should be done at a different level. Also rect() does not include padding etc!
    if (!context.viewport_rect().intersects(enclosing_int_rect(rect())))
        return;

    if (renders_as_alt_text()) {
        context.painter().set_font(Gfx::Font::default_font());
        Gfx::StylePainter::paint_frame(context.painter(), enclosing_int_rect(rect()), context.palette(), Gfx::FrameShape::Container, Gfx::FrameShadow::Sunken, 2);
        auto alt = node().alt();
        if (alt.is_empty())
            alt = node().src();
        context.painter().draw_text(enclosing_int_rect(rect()), alt, Gfx::TextAlignment::Center, style().color_or_fallback(CSS::PropertyID::Color, document(), Color::Black), Gfx::TextElision::Right);
    } else if (node().bitmap())
        context.painter().draw_scaled_bitmap(enclosing_int_rect(rect()), *node().bitmap(), node().bitmap()->rect());
    LayoutReplaced::render(context);
}

bool LayoutImage::renders_as_alt_text() const
{
    return !node().image_decoder();
}
