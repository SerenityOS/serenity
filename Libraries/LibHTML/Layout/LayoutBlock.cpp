#include <LibGUI/GPainter.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutBlock.h>

LayoutBlock::LayoutBlock(const Node* node, StyleProperties&& style_properties)
    : LayoutNode(node, move(style_properties))
{
}

LayoutBlock::~LayoutBlock()
{
}

LayoutNode& LayoutBlock::inline_wrapper()
{
    if (!last_child() || !last_child()->is_block() || last_child()->node() != nullptr) {
        append_child(adopt(*new LayoutBlock(nullptr, {})));
    }
    return *last_child();
}

void LayoutBlock::layout()
{
    compute_width();
    compute_position();

    int content_height = 0;
    for_each_child([&](auto& child) {
        child.layout();
        content_height = child.rect().bottom() + child.style().full_margin().bottom - rect().top();
    });
    rect().set_height(content_height);

    compute_height();
}

void LayoutBlock::compute_width()
{
    auto& style_properties = this->style_properties();

    auto auto_value = Length();
    auto zero_value = Length(0, Length::Type::Absolute);
    auto width = style_properties.length_or_fallback("width", auto_value);
    auto margin_left = style_properties.length_or_fallback("margin-left", zero_value);
    auto margin_right = style_properties.length_or_fallback("margin-right", zero_value);
    auto border_left = style_properties.length_or_fallback("border-left", zero_value);
    auto border_right = style_properties.length_or_fallback("border-right", zero_value);
    auto padding_left = style_properties.length_or_fallback("padding-left", zero_value);
    auto padding_right = style_properties.length_or_fallback("padding-right", zero_value);

#ifdef HTML_DEBUG
    dbg() << " Left: " << margin_left << "+" << border_left << "+" << padding_left;
    dbg() << "Right: " << margin_right << "+" << border_right << "+" << padding_right;
#endif

    int total_px = 0;
    for (auto& value : { margin_left, border_left, padding_left, width, padding_right, border_right, margin_right }) {
        total_px += value.to_px();
    }

#ifdef HTML_DEBUG
    dbg() << "Total: " << total_px;
#endif

    // 10.3.3 Block-level, non-replaced elements in normal flow
    // If 'width' is not 'auto' and 'border-left-width' + 'padding-left' + 'width' + 'padding-right' + 'border-right-width' (plus any of 'margin-left' or 'margin-right' that are not 'auto') is larger than the width of the containing block, then any 'auto' values for 'margin-left' or 'margin-right' are, for the following rules, treated as zero.
    if (width.is_auto() && total_px > containing_block()->rect().width()) {
        if (margin_left.is_auto())
            margin_left = zero_value;
        if (margin_right.is_auto())
            margin_right = zero_value;
    }

    // 10.3.3 cont'd.
    auto underflow_px = containing_block()->rect().width() - total_px;

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

    rect().set_width(width.to_px());
    style().margin().left = margin_left;
    style().margin().right = margin_right;
    style().border().left = border_left;
    style().border().right = border_right;
    style().padding().left = padding_left;
    style().padding().right = padding_right;
}

void LayoutBlock::compute_position()
{
    auto& style_properties = this->style_properties();

    auto auto_value = Length();
    auto zero_value = Length(0, Length::Type::Absolute);

    auto width = style_properties.length_or_fallback("width", auto_value);

    style().margin().top = style_properties.length_or_fallback("margin-top", zero_value);
    style().margin().bottom = style_properties.length_or_fallback("margin-bottom", zero_value);
    style().border().top = style_properties.length_or_fallback("border-top", zero_value);
    style().border().bottom = style_properties.length_or_fallback("border-bottom", zero_value);
    style().padding().top = style_properties.length_or_fallback("padding-top", zero_value);
    style().padding().bottom = style_properties.length_or_fallback("padding-bottom", zero_value);
    rect().set_x(containing_block()->rect().x() + style().margin().left.to_px() + style().border().left.to_px() + style().padding().left.to_px());

    int top_border = -1;
    if (previous_sibling() != nullptr) {
        auto& previous_sibling_rect = previous_sibling()->rect();
        auto& previous_sibling_style = previous_sibling()->style();
        top_border = previous_sibling_rect.y() + previous_sibling_rect.height();
        top_border += previous_sibling_style.full_margin().bottom;
    } else {
        top_border = containing_block()->rect().y();
    }
    rect().set_y(top_border + style().full_margin().top);
}

void LayoutBlock::compute_height()
{
    auto& style_properties = this->style_properties();

    auto height_property = style_properties.property("height");
    if (!height_property.has_value())
        return;
    auto height_length = height_property.value()->to_length();
    if (height_length.is_absolute())
        rect().set_height(height_length.to_px());
}

void LayoutBlock::render(RenderingContext& context)
{
    LayoutNode::render(context);

    // FIXME: position this properly
    if (style_properties().string_or_fallback("display", "block") == "list-item") {
        Rect bullet_rect {
            rect().x() - 8,
            rect().y() + 4,
            3,
            3
        };
        context.painter().fill_rect(bullet_rect, Color::Black);
    }
}
