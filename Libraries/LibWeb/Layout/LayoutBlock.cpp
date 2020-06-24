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
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/Dump.h>
#include <LibWeb/Layout/LayoutBlock.h>
#include <LibWeb/Layout/LayoutDocument.h>
#include <LibWeb/Layout/LayoutInline.h>
#include <LibWeb/Layout/LayoutReplaced.h>
#include <LibWeb/Layout/LayoutText.h>
#include <LibWeb/Layout/LayoutWidget.h>
#include <math.h>

namespace Web {

LayoutBlock::LayoutBlock(const Node* node, NonnullRefPtr<StyleProperties> style)
    : LayoutBox(node, move(style))
{
}

LayoutBlock::~LayoutBlock()
{
}

LayoutNode& LayoutBlock::inline_wrapper()
{
    if (!last_child() || !last_child()->is_block() || last_child()->node() != nullptr) {
        append_child(adopt(*new LayoutBlock(nullptr, style_for_anonymous_block())));
        last_child()->set_children_are_inline(true);
    }
    return *last_child();
}

void LayoutBlock::layout(LayoutMode layout_mode)
{
    compute_width();
    layout_inside(layout_mode);
    compute_height();

    layout_absolutely_positioned_descendants();
}

void LayoutBlock::layout_absolutely_positioned_descendant(LayoutBox& box)
{
    box.layout(LayoutMode::Default);
    auto& box_model = box.box_model();
    auto& specified_style = box.specified_style();
    auto zero_value = Length::make_px(0);

    auto specified_width = box.style().width().resolved_or_auto(box, width());

    box_model.margin.top = specified_style.length_or_fallback(CSS::PropertyID::MarginTop, Length::make_auto(), height());
    box_model.margin.right = specified_style.length_or_fallback(CSS::PropertyID::MarginRight, Length::make_auto(), width());
    box_model.margin.bottom = specified_style.length_or_fallback(CSS::PropertyID::MarginBottom, Length::make_auto(), height());
    box_model.margin.left = specified_style.length_or_fallback(CSS::PropertyID::MarginLeft, Length::make_auto(), width());

    box_model.offset.top = specified_style.length_or_fallback(CSS::PropertyID::Top, Length::make_auto(), height());
    box_model.offset.right = specified_style.length_or_fallback(CSS::PropertyID::Right, Length::make_auto(), width());
    box_model.offset.bottom = specified_style.length_or_fallback(CSS::PropertyID::Bottom, Length::make_auto(), height());
    box_model.offset.left = specified_style.length_or_fallback(CSS::PropertyID::Left, Length::make_auto(), width());

    if (box_model.offset.left.is_auto() && specified_width.is_auto() && box_model.offset.right.is_auto()) {
        if (box_model.margin.left.is_auto())
            box_model.margin.left = zero_value;
        if (box_model.margin.right.is_auto())
            box_model.margin.right = zero_value;
    }

    Gfx::FloatPoint used_offset;

    float x_offset = box_model.offset.left.to_px(box)
        + box_model.border_box(box).left
        - box_model.offset.right.to_px(box)
        - box_model.border_box(box).right;

    float y_offset = box_model.offset.top.to_px(box)
        + box_model.border_box(box).top
        - box_model.offset.bottom.to_px(box)
        - box_model.border_box(box).bottom;

    bool has_left_side_constraints = !box_model.offset.left.is_auto() || !box_model.margin.left.is_auto();
    bool has_right_side_constraints = !box_model.offset.right.is_auto() || !box_model.margin.right.is_auto();

    if (has_left_side_constraints && has_right_side_constraints) {
        // If both 'left' and 'right' are set, we will have stretched the width to accomodate both.
        x_offset += box_model.offset.right.to_px(box);
    }

    if (has_left_side_constraints) {
        used_offset.set_x(x_offset + box_model.margin.left.to_px(box));
    } else if (has_right_side_constraints) {
        used_offset.set_x(width() + x_offset - box.width() - box_model.margin.right.to_px(box));
    }

    if (!box_model.offset.top.is_auto()) {
        used_offset.set_y(y_offset + box_model.margin.top.to_px(box));
    } else if (!box_model.offset.bottom.is_auto()) {
        used_offset.set_y(height() + y_offset - box.height() - box_model.margin.bottom.to_px(box));
    }

    box.set_offset(used_offset);
}

void LayoutBlock::layout_inside(LayoutMode layout_mode)
{
    if (children_are_inline())
        layout_inline_children(layout_mode);
    else
        layout_contained_boxes(layout_mode);
}

void LayoutBlock::layout_absolutely_positioned_descendants()
{
    for_each_in_subtree_of_type<LayoutBox>([&](auto& box) {
        if (box.is_absolutely_positioned() && box.containing_block() == this) {
            layout_absolutely_positioned_descendant(box);
        }
        return IterationDecision::Continue;
    });
}

void LayoutBlock::layout_contained_boxes(LayoutMode layout_mode)
{
    float content_height = 0;
    float content_width = 0;
    for_each_in_subtree_of_type<LayoutBox>([&](auto& box) {
        if (box.is_absolutely_positioned() || box.containing_block() != this)
            return IterationDecision::Continue;
        box.layout(layout_mode);
        if (box.is_replaced())
            place_block_level_replaced_element_in_normal_flow(to<LayoutReplaced>(box));
        else if (box.is_block())
            place_block_level_non_replaced_element_in_normal_flow(to<LayoutBlock>(box));
        else
            dbg() << "FIXME: LayoutBlock::layout_contained_boxes doesn't know how to place a " << box.class_name();
        content_height = max(content_height, box.effective_offset().y() + box.height() + box.box_model().margin_box(*this).bottom);
        content_width = max(content_width, to<LayoutBox>(box).width());
        return IterationDecision::Continue;
    });

    if (layout_mode != LayoutMode::Default) {
        if (style().width().is_undefined() || style().width().is_auto())
            set_width(content_width);
    }

    set_height(content_height);
}

void LayoutBlock::layout_inline_children(LayoutMode layout_mode)
{
    ASSERT(children_are_inline());
    m_line_boxes.clear();
    for_each_child([&](auto& child) {
        ASSERT(child.is_inline());
        if (child.is_absolutely_positioned())
            return;
        child.split_into_lines(*this, layout_mode);
    });

    for (auto& line_box : m_line_boxes) {
        line_box.trim_trailing_whitespace();
    }

    auto text_align = style().text_align();
    float min_line_height = specified_style().line_height(*this);
    float line_spacing = min_line_height - specified_style().font().glyph_height();
    float content_height = 0;
    float max_linebox_width = 0;

    for (auto& line_box : m_line_boxes) {
        float max_height = min_line_height;
        for (auto& fragment : line_box.fragments()) {
            max_height = max(max_height, fragment.height());
        }

        float x_offset = 0;
        float excess_horizontal_space = (float)width() - line_box.width();

        switch (text_align) {
        case CSS::TextAlign::Center:
        case CSS::TextAlign::VendorSpecificCenter:
            x_offset += excess_horizontal_space / 2;
            break;
        case CSS::TextAlign::Right:
            x_offset += excess_horizontal_space;
            break;
        case CSS::TextAlign::Left:
        case CSS::TextAlign::Justify:
        default:
            break;
        }

        float excess_horizontal_space_including_whitespace = excess_horizontal_space;
        int whitespace_count = 0;
        if (text_align == CSS::TextAlign::Justify) {
            for (auto& fragment : line_box.fragments()) {
                if (fragment.is_justifiable_whitespace()) {
                    ++whitespace_count;
                    excess_horizontal_space_including_whitespace += fragment.width();
                }
            }
        }

        float justified_space_width = whitespace_count ? (excess_horizontal_space_including_whitespace / (float)whitespace_count) : 0;

        for (size_t i = 0; i < line_box.fragments().size(); ++i) {
            auto& fragment = line_box.fragments()[i];

            // Vertically align everyone's bottom to the line.
            // FIXME: Support other kinds of vertical alignment.
            fragment.set_offset({ roundf(x_offset + fragment.offset().x()), content_height + (max_height - fragment.height()) - (line_spacing / 2) });

            if (text_align == CSS::TextAlign::Justify) {
                if (fragment.is_justifiable_whitespace()) {
                    if (fragment.width() != justified_space_width) {
                        float diff = justified_space_width - fragment.width();
                        fragment.set_width(justified_space_width);
                        // Shift subsequent sibling fragments to the right to adjust for change in width.
                        for (size_t j = i + 1; j < line_box.fragments().size(); ++j) {
                            auto offset = line_box.fragments()[j].offset();
                            offset.move_by(diff, 0);
                            line_box.fragments()[j].set_offset(offset);
                        }
                    }
                }
            }

            if (fragment.layout_node().is_inline_block()) {
                auto& inline_block = const_cast<LayoutBlock&>(to<LayoutBlock>(fragment.layout_node()));
                inline_block.set_size(fragment.size());
                inline_block.layout(layout_mode);
            }

            float final_line_box_width = 0;
            for (auto& fragment : line_box.fragments())
                final_line_box_width += fragment.width();
            line_box.m_width = final_line_box_width;

            max_linebox_width = max(max_linebox_width, final_line_box_width);
        }

        content_height += max_height;
    }

    if (layout_mode != LayoutMode::Default) {
        set_width(max_linebox_width);
    }

    set_height(content_height);
}

void LayoutBlock::compute_width_for_absolutely_positioned_block()
{
    auto& specified_style = this->specified_style();
    auto& containing_block = *this->containing_block();
    auto zero_value = Length::make_px(0);

    Length margin_left = Length::make_auto();
    Length margin_right = Length::make_auto();
    Length border_left = Length::make_auto();
    Length border_right = Length::make_auto();
    Length padding_left = Length::make_auto();
    Length padding_right = Length::make_auto();

    auto try_compute_width = [&](const auto& a_width) {
        margin_left = specified_style.length_or_fallback(CSS::PropertyID::MarginLeft, zero_value, containing_block.width());
        margin_right = specified_style.length_or_fallback(CSS::PropertyID::MarginRight, zero_value, containing_block.width());
        border_left = specified_style.length_or_fallback(CSS::PropertyID::BorderLeftWidth, zero_value);
        border_right = specified_style.length_or_fallback(CSS::PropertyID::BorderRightWidth, zero_value);
        padding_left = specified_style.length_or_fallback(CSS::PropertyID::PaddingLeft, zero_value, containing_block.width());
        padding_right = specified_style.length_or_fallback(CSS::PropertyID::PaddingRight, zero_value, containing_block.width());

        auto left = specified_style.length_or_fallback(CSS::PropertyID::Left, Length::make_auto(), containing_block.width());
        auto right = specified_style.length_or_fallback(CSS::PropertyID::Right, Length::make_auto(), containing_block.width());
        auto width = a_width;

        auto solve_for_left = [&] {
            return Length(containing_block.width() - margin_left.to_px(*this) - border_left.to_px(*this) - padding_left.to_px(*this) - width.to_px(*this) - padding_right.to_px(*this) - border_right.to_px(*this) - margin_right.to_px(*this) - right.to_px(*this), Length::Type::Px);
        };

        auto solve_for_width = [&] {
            return Length(containing_block.width() - left.to_px(*this) - margin_left.to_px(*this) - border_left.to_px(*this) - padding_left.to_px(*this) - padding_right.to_px(*this) - border_right.to_px(*this) - margin_right.to_px(*this) - right.to_px(*this), Length::Type::Px);
        };

        auto solve_for_right = [&] {
            return Length(containing_block.width() - left.to_px(*this) - margin_left.to_px(*this) - border_left.to_px(*this) - padding_left.to_px(*this) - width.to_px(*this) - padding_right.to_px(*this) - border_right.to_px(*this) - margin_right.to_px(*this), Length::Type::Px);
        };

        // If all three of 'left', 'width', and 'right' are 'auto':
        if (left.is_auto() && width.is_auto() && right.is_auto()) {
            // First set any 'auto' values for 'margin-left' and 'margin-right' to 0.
            if (margin_left.is_auto())
                margin_left = Length::make_px(0);
            if (margin_right.is_auto())
                margin_right = Length::make_px(0);
            // Then, if the 'direction' property of the element establishing the static-position containing block
            // is 'ltr' set 'left' to the static position and apply rule number three below;
            // otherwise, set 'right' to the static position and apply rule number one below.
            // FIXME: This is very hackish.
            left = Length::make_px(0);
            goto Rule3;
        }

        if (!left.is_auto() && !width.is_auto() && !right.is_auto()) {
            // FIXME: This should be solved in a more complicated way.
            return width;
        }

        if (margin_left.is_auto())
            margin_left = Length::make_px(0);
        if (margin_right.is_auto())
            margin_right = Length::make_px(0);

        // 1. 'left' and 'width' are 'auto' and 'right' is not 'auto',
        //    then the width is shrink-to-fit. Then solve for 'left'
        if (left.is_auto() && width.is_auto() && !right.is_auto()) {
            auto result = calculate_shrink_to_fit_width();
            solve_for_left();
            auto available_width = solve_for_width();
            width = Length(min(max(result.preferred_minimum_width, available_width.to_px(*this)), result.preferred_width), Length::Type::Px);
        }

        // 2. 'left' and 'right' are 'auto' and 'width' is not 'auto',
        //    then if the 'direction' property of the element establishing
        //    the static-position containing block is 'ltr' set 'left'
        //    to the static position, otherwise set 'right' to the static position.
        //    Then solve for 'left' (if 'direction is 'rtl') or 'right' (if 'direction' is 'ltr').
        else if (left.is_auto() && right.is_auto() && !width.is_auto()) {
            // FIXME: Check direction
            // FIXME: Use the static-position containing block
            left = zero_value;
            right = solve_for_right();
        }

        // 3. 'width' and 'right' are 'auto' and 'left' is not 'auto',
        //    then the width is shrink-to-fit. Then solve for 'right'
        else if (width.is_auto() && right.is_auto() && !left.is_auto()) {
        Rule3:
            auto result = calculate_shrink_to_fit_width();
            right = solve_for_right();
            auto available_width = solve_for_width();
            width = Length(min(max(result.preferred_minimum_width, available_width.to_px(*this)), result.preferred_width), Length::Type::Px);
        }

        // 4. 'left' is 'auto', 'width' and 'right' are not 'auto', then solve for 'left'
        else if (left.is_auto() && !width.is_auto() && !right.is_auto()) {
            left = solve_for_left();
        }

        // 5. 'width' is 'auto', 'left' and 'right' are not 'auto', then solve for 'width'
        else if (width.is_auto() && !left.is_auto() && !right.is_auto()) {
            width = solve_for_width();
        }

        // 6. 'right' is 'auto', 'left' and 'width' are not 'auto', then solve for 'right'
        else if (right.is_auto() && !left.is_auto() && !width.is_auto()) {
            right = solve_for_right();
        }

        return width;
    };

    auto specified_width = style().width().resolved_or_auto(*this, containing_block.width());

    // 1. The tentative used width is calculated (without 'min-width' and 'max-width')
    auto used_width = try_compute_width(specified_width);

    // 2. The tentative used width is greater than 'max-width', the rules above are applied again,
    //    but this time using the computed value of 'max-width' as the computed value for 'width'.
    auto specified_max_width = style().max_width().resolved_or_auto(*this, containing_block.width());
    if (!specified_max_width.is_auto()) {
        if (used_width.to_px(*this) > specified_max_width.to_px(*this)) {
            used_width = try_compute_width(specified_max_width);
        }
    }

    // 3. If the resulting width is smaller than 'min-width', the rules above are applied again,
    //    but this time using the value of 'min-width' as the computed value for 'width'.
    auto specified_min_width = style().min_width().resolved_or_auto(*this, containing_block.width());
    if (!specified_min_width.is_auto()) {
        if (used_width.to_px(*this) < specified_min_width.to_px(*this)) {
            used_width = try_compute_width(specified_min_width);
        }
    }

    set_width(used_width.to_px(*this));

    box_model().margin.left = margin_left;
    box_model().margin.right = margin_right;
    box_model().border.left = border_left;
    box_model().border.right = border_right;
    box_model().padding.left = padding_left;
    box_model().padding.right = padding_right;
}

void LayoutBlock::compute_width()
{
    if (is_absolutely_positioned())
        return compute_width_for_absolutely_positioned_block();

    auto& specified_style = this->specified_style();
    auto zero_value = Length::make_px(0);

    Length margin_left = Length::make_auto();
    Length margin_right = Length::make_auto();
    Length border_left = Length::make_auto();
    Length border_right = Length::make_auto();
    Length padding_left = Length::make_auto();
    Length padding_right = Length::make_auto();

    auto& containing_block = *this->containing_block();

    auto try_compute_width = [&](const auto& a_width) {
        Length width = a_width;
#ifdef HTML_DEBUG
        dbg() << " Left: " << margin_left << "+" << border_left << "+" << padding_left;
        dbg() << "Right: " << margin_right << "+" << border_right << "+" << padding_right;
#endif
        margin_left = specified_style.length_or_fallback(CSS::PropertyID::MarginLeft, zero_value, containing_block.width());
        margin_right = specified_style.length_or_fallback(CSS::PropertyID::MarginRight, zero_value, containing_block.width());
        border_left = specified_style.length_or_fallback(CSS::PropertyID::BorderLeftWidth, zero_value);
        border_right = specified_style.length_or_fallback(CSS::PropertyID::BorderRightWidth, zero_value);
        padding_left = specified_style.length_or_fallback(CSS::PropertyID::PaddingLeft, zero_value, containing_block.width());
        padding_right = specified_style.length_or_fallback(CSS::PropertyID::PaddingRight, zero_value, containing_block.width());

        float total_px = 0;
        for (auto& value : { margin_left, border_left, padding_left, width, padding_right, border_right, margin_right }) {
            total_px += value.to_px(*this);
        }

#ifdef HTML_DEBUG
        dbg() << "Total: " << total_px;
#endif

        if (!is_replaced() && !is_inline()) {
            // 10.3.3 Block-level, non-replaced elements in normal flow
            // If 'width' is not 'auto' and 'border-left-width' + 'padding-left' + 'width' + 'padding-right' + 'border-right-width' (plus any of 'margin-left' or 'margin-right' that are not 'auto') is larger than the width of the containing block, then any 'auto' values for 'margin-left' or 'margin-right' are, for the following rules, treated as zero.
            if (width.is_auto() && total_px > containing_block.width()) {
                if (margin_left.is_auto())
                    margin_left = zero_value;
                if (margin_right.is_auto())
                    margin_right = zero_value;
            }

            // 10.3.3 cont'd.
            auto underflow_px = containing_block.width() - total_px;

            if (width.is_auto()) {
                if (margin_left.is_auto())
                    margin_left = zero_value;
                if (margin_right.is_auto())
                    margin_right = zero_value;
                if (underflow_px >= 0) {
                    width = Length(underflow_px, Length::Type::Px);
                } else {
                    width = zero_value;
                    margin_right = Length(margin_right.to_px(*this) + underflow_px, Length::Type::Px);
                }
            } else {
                if (!margin_left.is_auto() && !margin_right.is_auto()) {
                    margin_right = Length(margin_right.to_px(*this) + underflow_px, Length::Type::Px);
                } else if (!margin_left.is_auto() && margin_right.is_auto()) {
                    margin_right = Length(underflow_px, Length::Type::Px);
                } else if (margin_left.is_auto() && !margin_right.is_auto()) {
                    margin_left = Length(underflow_px, Length::Type::Px);
                } else { // margin_left.is_auto() && margin_right.is_auto()
                    auto half_of_the_underflow = Length(underflow_px / 2, Length::Type::Px);
                    margin_left = half_of_the_underflow;
                    margin_right = half_of_the_underflow;
                }
            }
        } else if (!is_replaced() && is_inline_block()) {

            // 10.3.9 'Inline-block', non-replaced elements in normal flow

            // A computed value of 'auto' for 'margin-left' or 'margin-right' becomes a used value of '0'.
            if (margin_left.is_auto())
                margin_left = zero_value;
            if (margin_right.is_auto())
                margin_right = zero_value;

            // If 'width' is 'auto', the used value is the shrink-to-fit width as for floating elements.
            if (width.is_auto()) {

                // Find the available width: in this case, this is the width of the containing
                // block minus the used values of 'margin-left', 'border-left-width', 'padding-left',
                // 'padding-right', 'border-right-width', 'margin-right', and the widths of any relevant scroll bars.
                float available_width = containing_block.width()
                    - margin_left.to_px(*this) - border_left.to_px(*this) - padding_left.to_px(*this)
                    - padding_right.to_px(*this) - border_right.to_px(*this) - margin_right.to_px(*this);

                auto result = calculate_shrink_to_fit_width();

                // Then the shrink-to-fit width is: min(max(preferred minimum width, available width), preferred width).
                width = Length(min(max(result.preferred_minimum_width, available_width), result.preferred_width), Length::Type::Px);
            }
        }

        return width;
    };

    auto specified_width = style().width().resolved_or_auto(*this, containing_block.width());

    // 1. The tentative used width is calculated (without 'min-width' and 'max-width')
    auto used_width = try_compute_width(specified_width);

    // 2. The tentative used width is greater than 'max-width', the rules above are applied again,
    //    but this time using the computed value of 'max-width' as the computed value for 'width'.
    auto specified_max_width = style().max_width().resolved_or_auto(*this, containing_block.width());
    if (!specified_max_width.is_auto()) {
        if (used_width.to_px(*this) > specified_max_width.to_px(*this)) {
            used_width = try_compute_width(specified_max_width);
        }
    }

    // 3. If the resulting width is smaller than 'min-width', the rules above are applied again,
    //    but this time using the value of 'min-width' as the computed value for 'width'.
    auto specified_min_width = style().min_width().resolved_or_auto(*this, containing_block.width());
    if (!specified_min_width.is_auto()) {
        if (used_width.to_px(*this) < specified_min_width.to_px(*this)) {
            used_width = try_compute_width(specified_min_width);
        }
    }

    set_width(used_width.to_px(*this));
    box_model().margin.left = margin_left;
    box_model().margin.right = margin_right;
    box_model().border.left = border_left;
    box_model().border.right = border_right;
    box_model().padding.left = padding_left;
    box_model().padding.right = padding_right;
}

void LayoutBlock::place_block_level_replaced_element_in_normal_flow(LayoutReplaced& box)
{
    ASSERT(!is_absolutely_positioned());
    auto& style = box.specified_style();
    auto zero_value = Length::make_px(0);
    auto& containing_block = *this;
    auto& replaced_element_box_model = box.box_model();

    replaced_element_box_model.margin.top = style.length_or_fallback(CSS::PropertyID::MarginTop, zero_value, containing_block.width());
    replaced_element_box_model.margin.bottom = style.length_or_fallback(CSS::PropertyID::MarginBottom, zero_value, containing_block.width());
    replaced_element_box_model.border.top = style.length_or_fallback(CSS::PropertyID::BorderTopWidth, zero_value);
    replaced_element_box_model.border.bottom = style.length_or_fallback(CSS::PropertyID::BorderBottomWidth, zero_value);
    replaced_element_box_model.padding.top = style.length_or_fallback(CSS::PropertyID::PaddingTop, zero_value, containing_block.width());
    replaced_element_box_model.padding.bottom = style.length_or_fallback(CSS::PropertyID::PaddingBottom, zero_value, containing_block.width());

    float x = replaced_element_box_model.margin.left.to_px(*this)
        + replaced_element_box_model.border.left.to_px(*this)
        + replaced_element_box_model.padding.left.to_px(*this)
        + replaced_element_box_model.offset.left.to_px(*this);

    float y = replaced_element_box_model.margin_box(*this).top + box_model().offset.top.to_px(*this);

    box.set_offset(x, y);
}

LayoutBlock::ShrinkToFitResult LayoutBlock::calculate_shrink_to_fit_width()
{
    auto greatest_child_width = [&] {
        float max_width = 0;
        if (children_are_inline()) {
            for (auto& box : line_boxes()) {
                max_width = max(max_width, box.width());
            }
        } else {
            for_each_child([&](auto& child) {
                if (child.is_box())
                    max_width = max(max_width, to<LayoutBox>(child).width());
            });
        }
        return max_width;
    };

    // Calculate the preferred width by formatting the content without breaking lines
    // other than where explicit line breaks occur.
    layout_inside(LayoutMode::OnlyRequiredLineBreaks);
    float preferred_width = greatest_child_width();

    // Also calculate the preferred minimum width, e.g., by trying all possible line breaks.
    // CSS 2.2 does not define the exact algorithm.

    layout_inside(LayoutMode::AllPossibleLineBreaks);
    float preferred_minimum_width = greatest_child_width();

    return { preferred_width, preferred_minimum_width };
}

void LayoutBlock::place_block_level_non_replaced_element_in_normal_flow(LayoutBlock& block)
{
    auto& specified_style = block.specified_style();
    auto zero_value = Length::make_px(0);
    auto& containing_block = *this;
    auto& box = block.box_model();

    box.margin.top = specified_style.length_or_fallback(CSS::PropertyID::MarginTop, zero_value, containing_block.width());
    box.margin.bottom = specified_style.length_or_fallback(CSS::PropertyID::MarginBottom, zero_value, containing_block.width());
    box.border.top = specified_style.length_or_fallback(CSS::PropertyID::BorderTopWidth, zero_value);
    box.border.bottom = specified_style.length_or_fallback(CSS::PropertyID::BorderBottomWidth, zero_value);
    box.padding.top = specified_style.length_or_fallback(CSS::PropertyID::PaddingTop, zero_value, containing_block.width());
    box.padding.bottom = specified_style.length_or_fallback(CSS::PropertyID::PaddingBottom, zero_value, containing_block.width());

    float x = box.margin.left.to_px(*this)
        + box.border.left.to_px(*this)
        + box.padding.left.to_px(*this)
        + box.offset.left.to_px(*this);

    if (this->style().text_align() == CSS::TextAlign::VendorSpecificCenter) {
        x = (containing_block.width() / 2) - block.width() / 2;
    }

    float y = box.margin_box(*this).top
        + box.offset.top.to_px(*this);

    auto* relevant_sibling = block.previous_sibling();
    while (relevant_sibling != nullptr) {
        if (relevant_sibling->style().position() != CSS::Position::Absolute)
            break;
        relevant_sibling = relevant_sibling->previous_sibling();
    }

    if (relevant_sibling) {
        auto& sibling_box = relevant_sibling->box_model();
        y += relevant_sibling->effective_offset().y() + relevant_sibling->height();

        // Collapse top margin with bottom margin of previous sibling if necessary
        float previous_sibling_margin_bottom = sibling_box.margin.bottom.to_px(*relevant_sibling);
        float my_margin_top = box.margin.top.to_px(*this);

        if (my_margin_top < 0 || previous_sibling_margin_bottom < 0) {
            // Negative margins present.
            float largest_negative_margin = -min(my_margin_top, previous_sibling_margin_bottom);
            float largest_positive_margin = (my_margin_top < 0 && previous_sibling_margin_bottom < 0) ? 0 : max(my_margin_top, previous_sibling_margin_bottom);
            float final_margin = largest_positive_margin - largest_negative_margin;
            y += final_margin - my_margin_top;
        } else if (previous_sibling_margin_bottom > my_margin_top) {
            // Sibling's margin is larger than mine, adjust so we use sibling's.
            y += previous_sibling_margin_bottom - my_margin_top;
        }
    }

    block.set_offset(x, y);
}

void LayoutBlock::compute_height()
{
    auto& style = this->specified_style();

    auto specified_height = style.length_or_fallback(CSS::PropertyID::Height, Length::make_auto(), containing_block()->height());
    auto specified_max_height = style.length_or_fallback(CSS::PropertyID::MaxHeight, Length::make_auto(), containing_block()->height());

    auto& containing_block = *this->containing_block();

    box_model().margin.top = style.length_or_fallback(CSS::PropertyID::MarginTop, Length::make_px(0), containing_block.width());
    box_model().margin.bottom = style.length_or_fallback(CSS::PropertyID::MarginBottom, Length::make_px(0), containing_block.width());
    box_model().border.top = style.length_or_fallback(CSS::PropertyID::BorderTopWidth, Length::make_px(0));
    box_model().border.bottom = style.length_or_fallback(CSS::PropertyID::BorderBottomWidth, Length::make_px(0));
    box_model().padding.top = style.length_or_fallback(CSS::PropertyID::PaddingTop, Length::make_px(0), containing_block.width());
    box_model().padding.bottom = style.length_or_fallback(CSS::PropertyID::PaddingBottom, Length::make_px(0), containing_block.width());

    if (!specified_height.is_auto()) {
        float used_height = specified_height.to_px(*this);
        if (!specified_max_height.is_auto())
            used_height = min(used_height, specified_max_height.to_px(*this));
        set_height(used_height);
    }
}

void LayoutBlock::paint(PaintContext& context, PaintPhase phase)
{
    if (!is_visible())
        return;

    LayoutBox::paint(context, phase);

    // FIXME: Inline backgrounds etc.
    if (phase == PaintPhase::Foreground) {
        if (children_are_inline()) {
            for (auto& line_box : m_line_boxes) {
                for (auto& fragment : line_box.fragments()) {
                    if (context.should_show_line_box_borders())
                        context.painter().draw_rect(enclosing_int_rect(fragment.absolute_rect()), Color::Green);
                    fragment.render(context);
                }
            }
        }
    }
}

HitTestResult LayoutBlock::hit_test(const Gfx::IntPoint& position) const
{
    if (!children_are_inline())
        return LayoutBox::hit_test(position);

    HitTestResult result;
    for (auto& line_box : m_line_boxes) {
        for (auto& fragment : line_box.fragments()) {
            if (enclosing_int_rect(fragment.absolute_rect()).contains(position)) {
                if (fragment.layout_node().is_block())
                    return to<LayoutBlock>(fragment.layout_node()).hit_test(position);
                return { fragment.layout_node(), fragment.text_index_at(position.x()) };
            }
        }
    }

    // FIXME: This should be smarter about the text position if we're hitting a block
    //        that has text inside it, but `position` is to the right of the text box.
    return { absolute_rect().contains(position.x(), position.y()) ? this : nullptr };
}

NonnullRefPtr<StyleProperties> LayoutBlock::style_for_anonymous_block() const
{
    auto new_style = StyleProperties::create();

    specified_style().for_each_property([&](auto property_id, auto& value) {
        if (StyleResolver::is_inherited_property(property_id))
            new_style->set_property(property_id, value);
    });

    return new_style;
}

LineBox& LayoutBlock::ensure_last_line_box()
{
    if (m_line_boxes.is_empty())
        m_line_boxes.append(LineBox());
    return m_line_boxes.last();
}

LineBox& LayoutBlock::add_line_box()
{
    m_line_boxes.append(LineBox());
    return m_line_boxes.last();
}

void LayoutBlock::split_into_lines(LayoutBlock& container, LayoutMode layout_mode)
{
    layout(layout_mode);

    auto* line_box = &container.ensure_last_line_box();
    if (layout_mode != LayoutMode::OnlyRequiredLineBreaks && line_box->width() > 0 && line_box->width() + width() > container.width()) {
        line_box = &container.add_line_box();
    }
    line_box->add_fragment(*this, 0, 0, width(), height());
}

}
