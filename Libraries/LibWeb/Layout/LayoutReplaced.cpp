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

#include <LibWeb/DOM/Element.h>
#include <LibWeb/Layout/LayoutBlock.h>
#include <LibWeb/Layout/LayoutReplaced.h>

namespace Web {

LayoutReplaced::LayoutReplaced(const Element& element, NonnullRefPtr<StyleProperties> style)
    : LayoutBox(&element, move(style))
{
    // FIXME: Allow non-inline replaced elements.
    set_inline(true);
}

LayoutReplaced::~LayoutReplaced()
{
}

float LayoutReplaced::calculate_width() const
{
    // 10.3.2 [Inline,] replaced elements

    auto& style = this->style();
    auto auto_value = Length();
    auto zero_value = Length(0, Length::Type::Px);
    auto& containing_block = *this->containing_block();

    auto margin_left = style.length_or_fallback(CSS::PropertyID::MarginLeft, zero_value, containing_block.width());
    auto margin_right = style.length_or_fallback(CSS::PropertyID::MarginRight, zero_value, containing_block.width());

    // A computed value of 'auto' for 'margin-left' or 'margin-right' becomes a used value of '0'.
    if (margin_left.is_auto())
        margin_left = zero_value;
    if (margin_right.is_auto())
        margin_right = zero_value;

    auto specified_width = style.length_or_fallback(CSS::PropertyID::Width, auto_value, containing_block.width());
    auto specified_height = style.length_or_fallback(CSS::PropertyID::Height, auto_value, containing_block.height());

    // FIXME: Actually compute 'width'
    auto computed_width = specified_width;

    float used_width = specified_width.to_px(*this);

    // If 'height' and 'width' both have computed values of 'auto' and the element also has an intrinsic width,
    // then that intrinsic width is the used value of 'width'.
    if (specified_height.is_auto() && specified_width.is_auto() && has_intrinsic_width()) {
        used_width = intrinsic_width();
    }

    // If 'height' and 'width' both have computed values of 'auto' and the element has no intrinsic width,
    // but does have an intrinsic height and intrinsic ratio;
    // or if 'width' has a computed value of 'auto',
    // 'height' has some other computed value, and the element does have an intrinsic ratio; then the used value of 'width' is:
    //
    //     (used height) * (intrinsic ratio)
    else if ((specified_height.is_auto() && specified_width.is_auto() && !has_intrinsic_width() && has_intrinsic_height() && has_intrinsic_ratio()) || computed_width.is_auto()) {
        used_width = calculate_height() * intrinsic_ratio();
    }

    else if (computed_width.is_auto() && has_intrinsic_width()) {
        used_width = intrinsic_width();
    }

    else if (computed_width.is_auto()) {
        used_width = 300;
    }

    return used_width;
}

float LayoutReplaced::calculate_height() const
{
    // 10.6.2 Inline replaced elements, block-level replaced elements in normal flow,
    // 'inline-block' replaced elements in normal flow and floating replaced elements
    auto& style = this->style();
    auto auto_value = Length();
    auto& containing_block = *this->containing_block();

    auto specified_width = style.length_or_fallback(CSS::PropertyID::Width, auto_value, containing_block.width());
    auto specified_height = style.length_or_fallback(CSS::PropertyID::Height, auto_value, containing_block.height());

    float used_height = specified_height.to_px(*this);

    // If 'height' and 'width' both have computed values of 'auto' and the element also has
    // an intrinsic height, then that intrinsic height is the used value of 'height'.
    if (specified_width.is_auto() && specified_height.is_auto() && has_intrinsic_height())
        used_height = intrinsic_height();
    else if (specified_height.is_auto() && has_intrinsic_ratio())
        used_height = calculate_width() * intrinsic_ratio();
    else if (specified_height.is_auto() && has_intrinsic_height())
        used_height = intrinsic_height();
    else if (specified_height.is_auto())
        used_height = 150;

    return used_height;
}

Gfx::FloatPoint LayoutReplaced::calculate_position()
{
    auto& style = this->style();
    auto zero_value = Length(0, Length::Type::Px);
    auto& containing_block = *this->containing_block();

    if (style.position() == CSS::Position::Absolute) {
        box_model().offset().top = style.length_or_fallback(CSS::PropertyID::Top, zero_value, containing_block.height());
        box_model().offset().right = style.length_or_fallback(CSS::PropertyID::Right, zero_value, containing_block.width());
        box_model().offset().bottom = style.length_or_fallback(CSS::PropertyID::Bottom, zero_value, containing_block.height());
        box_model().offset().left = style.length_or_fallback(CSS::PropertyID::Left, zero_value, containing_block.width());
    }

    box_model().margin().top = style.length_or_fallback(CSS::PropertyID::MarginTop, zero_value, containing_block.width());
    box_model().margin().bottom = style.length_or_fallback(CSS::PropertyID::MarginBottom, zero_value, containing_block.width());
    box_model().border().top = style.length_or_fallback(CSS::PropertyID::BorderTopWidth, zero_value);
    box_model().border().bottom = style.length_or_fallback(CSS::PropertyID::BorderBottomWidth, zero_value);
    box_model().padding().top = style.length_or_fallback(CSS::PropertyID::PaddingTop, zero_value, containing_block.width());
    box_model().padding().bottom = style.length_or_fallback(CSS::PropertyID::PaddingBottom, zero_value, containing_block.width());

    float position_x = box_model().margin().left.to_px(*this)
        + box_model().border().left.to_px(*this)
        + box_model().padding().left.to_px(*this)
        + box_model().offset().left.to_px(*this);

    if (style.position() != CSS::Position::Absolute || containing_block.style().position() == CSS::Position::Absolute)
        position_x += containing_block.x();

    float position_y = box_model().full_margin(*this).top + box_model().offset().top.to_px(*this);

    return { position_x, position_y };
}

void LayoutReplaced::layout(LayoutMode layout_mode)
{
    rect().set_width(calculate_width());
    rect().set_height(calculate_height());

    LayoutBox::layout(layout_mode);

    rect().set_location(calculate_position());
}

void LayoutReplaced::split_into_lines(LayoutBlock& container, LayoutMode layout_mode)
{
    layout(layout_mode);

    auto* line_box = &container.ensure_last_line_box();
    if (line_box->width() > 0 && line_box->width() + width() > container.width())
        line_box = &container.add_line_box();
    line_box->add_fragment(*this, 0, 0, width(), height());
}

}
