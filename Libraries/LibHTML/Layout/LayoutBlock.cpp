#include <LibGUI/GPainter.h>
#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutInline.h>
#include <LibHTML/Layout/LayoutReplaced.h>
#include <LibHTML/Layout/LayoutText.h>
#include <math.h>

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

void LayoutBlock::layout()
{
    compute_width();
    compute_position();

    if (children_are_inline())
        layout_inline_children();
    else
        layout_block_children();

    compute_height();
}

void LayoutBlock::layout_block_children()
{
    ASSERT(!children_are_inline());
    float content_height = 0;
    for_each_child([&](auto& child) {
        // FIXME: What should we do here? Something like a <table> might have a bunch of useless text children..
        if (child.is_inline())
            return;
        auto& child_block = static_cast<LayoutBlock&>(child);
        child_block.layout();
        content_height = child_block.rect().bottom() + child_block.box_model().full_margin().bottom - rect().top();
    });
    rect().set_height(content_height);
}

void LayoutBlock::layout_inline_children()
{
    ASSERT(children_are_inline());
    m_line_boxes.clear();
    for_each_child([&](auto& child) {
        ASSERT(child.is_inline());
        child.split_into_lines(*this);
    });

    for (auto& line_box : m_line_boxes) {
        line_box.trim_trailing_whitespace();
    }

    float min_line_height = style().line_height();
    float line_spacing = min_line_height - style().font().glyph_height();
    float content_height = 0;

    // FIXME: This should be done by the CSS parser!
    CSS::ValueID text_align = CSS::ValueID::Left;
    auto text_align_string = style().string_or_fallback(CSS::PropertyID::TextAlign, "left");
    if (text_align_string == "center")
        text_align = CSS::ValueID::Center;
    else if (text_align_string == "left")
        text_align = CSS::ValueID::Left;
    else if (text_align_string == "right")
        text_align = CSS::ValueID::Right;
    else if (text_align_string == "justify")
        text_align = CSS::ValueID::Justify;

    for (auto& line_box : m_line_boxes) {
        float max_height = min_line_height;
        for (auto& fragment : line_box.fragments()) {
            max_height = max(max_height, fragment.rect().height());
        }

        float x_offset = x();
        float excess_horizontal_space = (float)width() - line_box.width();

        switch (text_align) {
        case CSS::ValueID::Center:
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
                    excess_horizontal_space_including_whitespace += fragment.rect().width();
                }
            }
        }

        float justified_space_width = whitespace_count ? (excess_horizontal_space_including_whitespace / (float)whitespace_count) : 0;

        for (int i = 0; i < line_box.fragments().size(); ++i) {
            auto& fragment = line_box.fragments()[i];
            // Vertically align everyone's bottom to the line.
            // FIXME: Support other kinds of vertical alignment.
            fragment.rect().set_x(roundf(x_offset + fragment.rect().x()));
            fragment.rect().set_y(y() + content_height + (max_height - fragment.rect().height()) - (line_spacing / 2));

            if (text_align == CSS::ValueID::Justify) {
                if (fragment.is_justifiable_whitespace()) {
                    if (fragment.rect().width() != justified_space_width) {
                        float diff = justified_space_width - fragment.rect().width();
                        fragment.rect().set_width(justified_space_width);
                        // Shift subsequent sibling fragments to the right to adjust for change in width.
                        for (int j = i + 1; j < line_box.fragments().size(); ++j) {
                            line_box.fragments()[j].rect().move_by(diff, 0);
                        }
                    }
                }
            }

            if (is<LayoutReplaced>(fragment.layout_node()))
                const_cast<LayoutReplaced&>(to<LayoutReplaced>(fragment.layout_node())).set_rect(fragment.rect());

            float final_line_box_width = 0;
            for (auto& fragment : line_box.fragments())
                final_line_box_width += fragment.rect().width();
            line_box.m_width = final_line_box_width;
        }

        content_height += max_height;
    }

    rect().set_height(content_height);
}

void LayoutBlock::compute_width()
{
    auto& style = this->style();

    auto auto_value = Length();
    auto zero_value = Length(0, Length::Type::Absolute);

    Length margin_left;
    Length margin_right;
    Length border_left;
    Length border_right;
    Length padding_left;
    Length padding_right;

    auto try_compute_width = [&](const auto& a_width) {
        Length width = a_width;
#ifdef HTML_DEBUG
        dbg() << " Left: " << margin_left << "+" << border_left << "+" << padding_left;
        dbg() << "Right: " << margin_right << "+" << border_right << "+" << padding_right;
#endif
        margin_left = style.length_or_fallback(CSS::PropertyID::MarginLeft, zero_value);
        margin_right = style.length_or_fallback(CSS::PropertyID::MarginRight, zero_value);
        border_left = style.length_or_fallback(CSS::PropertyID::BorderLeftWidth, zero_value);
        border_right = style.length_or_fallback(CSS::PropertyID::BorderRightWidth, zero_value);
        padding_left = style.length_or_fallback(CSS::PropertyID::PaddingLeft, zero_value);
        padding_right = style.length_or_fallback(CSS::PropertyID::PaddingRight, zero_value);

        float total_px = 0;
        for (auto& value : { margin_left, border_left, padding_left, width, padding_right, border_right, margin_right }) {
            total_px += value.to_px();
        }

#ifdef HTML_DEBUG
        dbg() << "Total: " << total_px;
#endif

        // 10.3.3 Block-level, non-replaced elements in normal flow
        // If 'width' is not 'auto' and 'border-left-width' + 'padding-left' + 'width' + 'padding-right' + 'border-right-width' (plus any of 'margin-left' or 'margin-right' that are not 'auto') is larger than the width of the containing block, then any 'auto' values for 'margin-left' or 'margin-right' are, for the following rules, treated as zero.
        if (width.is_auto() && total_px > containing_block()->width()) {
            if (margin_left.is_auto())
                margin_left = zero_value;
            if (margin_right.is_auto())
                margin_right = zero_value;
        }

        // 10.3.3 cont'd.
        auto underflow_px = containing_block()->width() - total_px;

        if (width.is_auto()) {
            if (margin_left.is_auto())
                margin_left = zero_value;
            if (margin_right.is_auto())
                margin_right = zero_value;
            if (underflow_px >= 0) {
                width = Length(underflow_px, Length::Type::Absolute);
            } else {
                width = zero_value;
                margin_right = Length(margin_right.to_px() + underflow_px, Length::Type::Absolute);
            }
        } else {
            if (!margin_left.is_auto() && !margin_right.is_auto()) {
                margin_right = Length(margin_right.to_px() + underflow_px, Length::Type::Absolute);
            } else if (!margin_left.is_auto() && margin_right.is_auto()) {
                margin_right = Length(underflow_px, Length::Type::Absolute);
            } else if (margin_left.is_auto() && !margin_right.is_auto()) {
                margin_left = Length(underflow_px, Length::Type::Absolute);
            } else { // margin_left.is_auto() && margin_right.is_auto()
                auto half_of_the_underflow = Length(underflow_px / 2, Length::Type::Absolute);
                margin_left = half_of_the_underflow;
                margin_right = half_of_the_underflow;
            }
        }
        return width;
    };

    auto specified_width = style.length_or_fallback(CSS::PropertyID::Width, auto_value);

    // 1. The tentative used width is calculated (without 'min-width' and 'max-width')
    auto used_width = try_compute_width(specified_width);

    // 2. The tentative used width is greater than 'max-width', the rules above are applied again,
    //    but this time using the computed value of 'max-width' as the computed value for 'width'.
    auto specified_max_width = style.length_or_fallback(CSS::PropertyID::MaxWidth, auto_value);
    if (!specified_max_width.is_auto()) {
        if (used_width.to_px() > specified_max_width.to_px()) {
            used_width = try_compute_width(specified_max_width);
        }
    }

    // 3. If the resulting width is smaller than 'min-width', the rules above are applied again,
    //    but this time using the value of 'min-width' as the computed value for 'width'.
    auto specified_min_width = style.length_or_fallback(CSS::PropertyID::MinWidth, auto_value);
    if (!specified_min_width.is_auto()) {
        if (used_width.to_px() < specified_min_width.to_px()) {
            used_width = try_compute_width(specified_min_width);
        }
    }

    rect().set_width(used_width.to_px());
    box_model().margin().left = margin_left;
    box_model().margin().right = margin_right;
    box_model().border().left = border_left;
    box_model().border().right = border_right;
    box_model().padding().left = padding_left;
    box_model().padding().right = padding_right;
}

void LayoutBlock::compute_position()
{
    auto& style = this->style();

    auto auto_value = Length();
    auto zero_value = Length(0, Length::Type::Absolute);

    auto width = style.length_or_fallback(CSS::PropertyID::Width, auto_value);

    box_model().margin().top = style.length_or_fallback(CSS::PropertyID::MarginTop, zero_value);
    box_model().margin().bottom = style.length_or_fallback(CSS::PropertyID::MarginBottom, zero_value);
    box_model().border().top = style.length_or_fallback(CSS::PropertyID::BorderTopWidth, zero_value);
    box_model().border().bottom = style.length_or_fallback(CSS::PropertyID::BorderBottomWidth, zero_value);
    box_model().padding().top = style.length_or_fallback(CSS::PropertyID::PaddingTop, zero_value);
    box_model().padding().bottom = style.length_or_fallback(CSS::PropertyID::PaddingBottom, zero_value);
    rect().set_x(containing_block()->x() + box_model().margin().left.to_px() + box_model().border().left.to_px() + box_model().padding().left.to_px());

    float top_border = -1;
    if (previous_sibling() != nullptr) {
        auto& previous_sibling_rect = previous_sibling()->rect();
        auto& previous_sibling_style = previous_sibling()->box_model();
        top_border = previous_sibling_rect.y() + previous_sibling_rect.height();
        top_border += previous_sibling_style.full_margin().bottom;
    } else {
        top_border = containing_block()->y();
    }
    rect().set_y(top_border + box_model().full_margin().top);
}

void LayoutBlock::compute_height()
{
    auto& style = this->style();

    auto height_property = style.property(CSS::PropertyID::Height);
    if (!height_property.has_value())
        return;
    auto height_length = height_property.value()->to_length();
    if (height_length.is_absolute())
        rect().set_height(height_length.to_px());
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
                    context.painter().draw_rect(enclosing_int_rect(fragment.rect()), Color::Green);
                fragment.render(context);
            }
        }
    }
}

HitTestResult LayoutBlock::hit_test(const Point& position) const
{
    if (!children_are_inline())
        return LayoutBox::hit_test(position);

    HitTestResult result;
    for (auto& line_box : m_line_boxes) {
        for (auto& fragment : line_box.fragments()) {
            if (enclosing_int_rect(fragment.rect()).contains(position)) {
                return { fragment.layout_node(), fragment.text_index_at(position.x()) };
            }
        }
    }
    return {};
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
