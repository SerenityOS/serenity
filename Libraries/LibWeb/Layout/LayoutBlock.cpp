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

    if (!is_inline())
        compute_position();

    layout_children(layout_mode);

    compute_height();

    if (layout_mode == LayoutMode::Default)
        layout_absolutely_positioned_descendants();
}

void LayoutBlock::layout_absolutely_positioned_descendants()
{
    for (auto& box : m_absolutely_positioned_descendants) {
        box->layout(LayoutMode::Default);
        auto& box_model = box->box_model();
        auto& style = box->style();
        auto zero_value = Length(0, Length::Type::Px);

        auto specified_width = style.length_or_fallback(CSS::PropertyID::Width, Length(), width());

        box_model.margin().top = style.length_or_fallback(CSS::PropertyID::MarginTop, {}, height());
        box_model.margin().right = style.length_or_fallback(CSS::PropertyID::MarginRight, {}, width());
        box_model.margin().bottom = style.length_or_fallback(CSS::PropertyID::MarginBottom, {}, height());
        box_model.margin().left = style.length_or_fallback(CSS::PropertyID::MarginLeft, {}, width());

        box_model.offset().top = style.length_or_fallback(CSS::PropertyID::Top, {}, height());
        box_model.offset().right = style.length_or_fallback(CSS::PropertyID::Right, {}, width());
        box_model.offset().bottom = style.length_or_fallback(CSS::PropertyID::Bottom, {}, height());
        box_model.offset().left = style.length_or_fallback(CSS::PropertyID::Left, {}, width());

        if (box_model.offset().left.is_auto() && specified_width.is_auto() && box_model.offset().right.is_auto()) {
            if (box_model.margin().left.is_auto())
                box_model.margin().left = zero_value;
            if (box_model.margin().right.is_auto())
                box_model.margin().right = zero_value;
        }

        Gfx::FloatPoint used_offset;

        float x_offset = box_model.offset().left.to_px(*box)
            + box_model.border_box(*box).left
            - box_model.offset().right.to_px(*box)
            - box_model.border_box(*box).right;

        float y_offset = box_model.offset().top.to_px(*box)
            + box_model.border_box(*box).top
            - box_model.offset().bottom.to_px(*box)
            - box_model.border_box(*box).bottom;

        if (!box_model.offset().left.is_auto()) {
            used_offset.set_x(x_offset + box_model.margin().left.to_px(*box));
        } else if (!box_model.offset().right.is_auto()) {
            used_offset.set_x(width() + x_offset - box->width() - box_model.margin().right.to_px(*box));
        }

        if (!box_model.offset().top.is_auto()) {
            used_offset.set_y(y_offset + box_model.margin().top.to_px(*box));
        } else if (!box_model.offset().bottom.is_auto()) {
            used_offset.set_y(height() + y_offset - box->height() - box_model.margin().bottom.to_px(*box));
        }

        box->set_offset(used_offset);
    }
}

void LayoutBlock::add_absolutely_positioned_descendant(LayoutBox& box)
{
    m_absolutely_positioned_descendants.set(box);
}

void LayoutBlock::layout_children(LayoutMode layout_mode)
{
    if (children_are_inline())
        layout_inline_children(layout_mode);
    else
        layout_block_children(layout_mode);
}

void LayoutBlock::layout_block_children(LayoutMode layout_mode)
{
    ASSERT(!children_are_inline());
    float content_height = 0;
    for_each_child([&](auto& child) {
        // FIXME: What should we do here? Something like a <table> might have a bunch of useless text children..
        if (child.is_inline())
            return;
        auto& child_block = static_cast<LayoutBlock&>(child);
        child_block.layout(layout_mode);

        if (!child_block.is_absolutely_positioned())
            content_height = max(content_height, child_block.effective_offset().y() + child_block.height() + child_block.box_model().margin_box(*this).bottom);
    });
    if (layout_mode != LayoutMode::Default) {
        float max_width = 0;
        for_each_child([&](auto& child) {
            if (child.is_box() && !child.is_absolutely_positioned())
                max_width = max(max_width, to<LayoutBox>(child).width());
        });
        set_width(max_width);
    }
    set_height(content_height);
}

void LayoutBlock::layout_inline_children(LayoutMode layout_mode)
{
    ASSERT(children_are_inline());
    m_line_boxes.clear();
    for_each_child([&](auto& child) {
        ASSERT(child.is_inline());
        if (child.is_box() && child.is_absolutely_positioned()) {
            const_cast<LayoutBlock*>(child.containing_block())->add_absolutely_positioned_descendant((LayoutBox&)child);
            return;
        }
        child.split_into_lines(*this, layout_mode);
    });

    for (auto& line_box : m_line_boxes) {
        line_box.trim_trailing_whitespace();
    }

    auto text_align = style().text_align();
    float min_line_height = style().line_height(*this);
    float line_spacing = min_line_height - style().font().glyph_height();
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
        case CSS::ValueID::Center:
        case CSS::ValueID::VendorSpecificCenter:
            x_offset += excess_horizontal_space / 2;
            break;
        case CSS::ValueID::Right:
            x_offset += excess_horizontal_space;
            break;
        case CSS::ValueID::Left:
        case CSS::ValueID::Justify:
        default:
            break;
        }

        float excess_horizontal_space_including_whitespace = excess_horizontal_space;
        int whitespace_count = 0;
        if (text_align == CSS::ValueID::Justify) {
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

            if (text_align == CSS::ValueID::Justify) {
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

void LayoutBlock::compute_width()
{
    auto& style = this->style();

    auto auto_value = Length();
    auto zero_value = Length(0, Length::Type::Px);

    Length margin_left;
    Length margin_right;
    Length border_left;
    Length border_right;
    Length padding_left;
    Length padding_right;

    auto& containing_block = *this->containing_block();

    auto try_compute_width = [&](const auto& a_width) {
        Length width = a_width;
#ifdef HTML_DEBUG
        dbg() << " Left: " << margin_left << "+" << border_left << "+" << padding_left;
        dbg() << "Right: " << margin_right << "+" << border_right << "+" << padding_right;
#endif
        margin_left = style.length_or_fallback(CSS::PropertyID::MarginLeft, zero_value, containing_block.width());
        margin_right = style.length_or_fallback(CSS::PropertyID::MarginRight, zero_value, containing_block.width());
        border_left = style.length_or_fallback(CSS::PropertyID::BorderLeftWidth, zero_value);
        border_right = style.length_or_fallback(CSS::PropertyID::BorderRightWidth, zero_value);
        padding_left = style.length_or_fallback(CSS::PropertyID::PaddingLeft, zero_value, containing_block.width());
        padding_right = style.length_or_fallback(CSS::PropertyID::PaddingRight, zero_value, containing_block.width());

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

                // Find the available width: in this case, this is the width of the containing
                // block minus the used values of 'margin-left', 'border-left-width', 'padding-left',
                // 'padding-right', 'border-right-width', 'margin-right', and the widths of any relevant scroll bars.

                float available_width = containing_block.width()
                    - margin_left.to_px(*this) - border_left.to_px(*this) - padding_left.to_px(*this)
                    - padding_right.to_px(*this) - border_right.to_px(*this) - margin_right.to_px(*this);

                // Calculate the preferred width by formatting the content without breaking lines
                // other than where explicit line breaks occur.
                layout_children(LayoutMode::OnlyRequiredLineBreaks);
                float preferred_width = greatest_child_width();

                // Also calculate the preferred minimum width, e.g., by trying all possible line breaks.
                // CSS 2.2 does not define the exact algorithm.

                layout_children(LayoutMode::AllPossibleLineBreaks);
                float preferred_minimum_width = greatest_child_width();

                // Then the shrink-to-fit width is: min(max(preferred minimum width, available width), preferred width).
                width = Length(min(max(preferred_minimum_width, available_width), preferred_width), Length::Type::Px);
            }
        }

        return width;
    };

    auto specified_width = style.length_or_fallback(CSS::PropertyID::Width, auto_value, containing_block.width());

    // 1. The tentative used width is calculated (without 'min-width' and 'max-width')
    auto used_width = try_compute_width(specified_width);

    // 2. The tentative used width is greater than 'max-width', the rules above are applied again,
    //    but this time using the computed value of 'max-width' as the computed value for 'width'.
    auto specified_max_width = style.length_or_fallback(CSS::PropertyID::MaxWidth, auto_value, containing_block.width());
    if (!specified_max_width.is_auto()) {
        if (used_width.to_px(*this) > specified_max_width.to_px(*this)) {
            used_width = try_compute_width(specified_max_width);
        }
    }

    // 3. If the resulting width is smaller than 'min-width', the rules above are applied again,
    //    but this time using the value of 'min-width' as the computed value for 'width'.
    auto specified_min_width = style.length_or_fallback(CSS::PropertyID::MinWidth, auto_value, containing_block.width());
    if (!specified_min_width.is_auto()) {
        if (used_width.to_px(*this) < specified_min_width.to_px(*this)) {
            used_width = try_compute_width(specified_min_width);
        }
    }

    set_width(used_width.to_px(*this));
    box_model().margin().left = margin_left;
    box_model().margin().right = margin_right;
    box_model().border().left = border_left;
    box_model().border().right = border_right;
    box_model().padding().left = padding_left;
    box_model().padding().right = padding_right;
}

void LayoutBlock::compute_position()
{
    if (is_absolutely_positioned()) {
        const_cast<LayoutBlock*>(containing_block())->add_absolutely_positioned_descendant(*this);
        return;
    }

    auto& style = this->style();
    auto zero_value = Length(0, Length::Type::Px);
    auto& containing_block = *this->containing_block();

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

    if (parent()->is_block() && parent()->style().text_align() == CSS::ValueID::VendorSpecificCenter) {
        position_x += (containing_block.width() / 2) - width() / 2;
    }

    float position_y = box_model().margin_box(*this).top
        + box_model().offset().top.to_px(*this);

    LayoutBlock* relevant_sibling = previous_sibling();
    while (relevant_sibling != nullptr) {
        if (relevant_sibling->style().position() != CSS::Position::Absolute)
            break;
        relevant_sibling = relevant_sibling->previous_sibling();
    }

    if (relevant_sibling) {
        auto& previous_sibling_style = relevant_sibling->box_model();
        position_y += relevant_sibling->effective_offset().y() + relevant_sibling->height();

        // Collapse top margin with bottom margin of previous sibling if necessary
        float previous_sibling_margin_bottom = previous_sibling_style.margin().bottom.to_px(*relevant_sibling);
        float my_margin_top = box_model().margin().top.to_px(*this);

        if (my_margin_top < 0 || previous_sibling_margin_bottom < 0) {
            // Negative margins present.
            float largest_negative_margin = -min(my_margin_top, previous_sibling_margin_bottom);
            float largest_positive_margin = (my_margin_top < 0 && previous_sibling_margin_bottom < 0) ? 0 : max(my_margin_top, previous_sibling_margin_bottom);
            float final_margin = largest_positive_margin - largest_negative_margin;
            position_y += final_margin - my_margin_top;
        } else if (previous_sibling_margin_bottom > my_margin_top) {
            // Sibling's margin is larger than mine, adjust so we use sibling's.
            position_y += previous_sibling_margin_bottom - my_margin_top;
        }
    }

    set_offset({ position_x, position_y });
}

void LayoutBlock::compute_height()
{
    auto& style = this->style();

    auto specified_height = style.length_or_fallback(CSS::PropertyID::Height, Length(), containing_block()->height());
    auto specified_max_height = style.length_or_fallback(CSS::PropertyID::MaxHeight, Length(), containing_block()->height());

    if (!specified_height.is_auto()) {
        float used_height = specified_height.to_px(*this);
        if (!specified_max_height.is_auto())
            used_height = min(used_height, specified_max_height.to_px(*this));
        set_height(used_height);
    }
}

void LayoutBlock::render(RenderingContext& context)
{
    if (!is_visible())
        return;

    LayoutBox::render(context);

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

    style().for_each_property([&](auto property_id, auto& value) {
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
    ASSERT(is_inline());

    layout(layout_mode);

    auto* line_box = &container.ensure_last_line_box();
    if (line_box->width() > 0 && line_box->width() + width() > container.width())
        line_box = &container.add_line_box();
    line_box->add_fragment(*this, 0, 0, width(), height());
}

}
