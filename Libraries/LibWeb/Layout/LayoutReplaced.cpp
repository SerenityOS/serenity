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

LayoutReplaced::LayoutReplaced(DOM::Document& document, DOM::Element& element, NonnullRefPtr<CSS::StyleProperties> style)
    : LayoutBox(document, &element, move(style))
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

    auto zero_value = CSS::Length::make_px(0);
    auto& containing_block = *this->containing_block();

    auto margin_left = style().margin().left.resolved_or_zero(*this, containing_block.width());
    auto margin_right = style().margin().right.resolved_or_zero(*this, containing_block.width());

    // A computed value of 'auto' for 'margin-left' or 'margin-right' becomes a used value of '0'.
    if (margin_left.is_auto())
        margin_left = zero_value;
    if (margin_right.is_auto())
        margin_right = zero_value;

    auto specified_width = style().width().resolved_or_auto(*this, containing_block.width());
    auto specified_height = style().height().resolved_or_auto(*this, containing_block.height());

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
    else if ((specified_height.is_auto() && specified_width.is_auto() && !has_intrinsic_width() && has_intrinsic_height() && has_intrinsic_ratio()) || (computed_width.is_auto() && has_intrinsic_ratio())) {
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
    auto& containing_block = *this->containing_block();

    auto specified_width = style().width().resolved_or_auto(*this, containing_block.width());
    auto specified_height = style().height().resolved_or_auto(*this, containing_block.height());

    float used_height = specified_height.to_px(*this);

    // If 'height' and 'width' both have computed values of 'auto' and the element also has
    // an intrinsic height, then that intrinsic height is the used value of 'height'.
    if (specified_width.is_auto() && specified_height.is_auto() && has_intrinsic_height())
        used_height = intrinsic_height();
    else if (specified_height.is_auto() && has_intrinsic_ratio())
        used_height = calculate_width() / intrinsic_ratio();
    else if (specified_height.is_auto() && has_intrinsic_height())
        used_height = intrinsic_height();
    else if (specified_height.is_auto())
        used_height = 150;

    return used_height;
}

void LayoutReplaced::layout(LayoutMode)
{
    set_width(calculate_width());
    set_height(calculate_height());
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
