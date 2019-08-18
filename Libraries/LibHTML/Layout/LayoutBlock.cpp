#include <LibHTML/CSS/StyledNode.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutBlock.h>

LayoutBlock::LayoutBlock(const Node* node, const StyledNode* styled_node)
    : LayoutNode(node, styled_node)
{
}

LayoutBlock::~LayoutBlock()
{
}

LayoutNode& LayoutBlock::inline_wrapper()
{
    if (!last_child() || !last_child()->is_block()) {
        append_child(adopt(*new LayoutBlock(nullptr, nullptr)));
    }
    return *last_child();
}

void LayoutBlock::layout()
{
    compute_width();

    LayoutNode::layout();

    compute_height();
}

void LayoutBlock::compute_width()
{
    if (!styled_node()) {
        // I guess the size is "auto" in this case.
        return;
    }

    auto& styled_node = *this->styled_node();
    auto auto_value = Length();
    auto zero_value = Length(0, Length::Type::Absolute);

    auto length_or_fallback = [&](const StringView& property_name, const Length& fallback) {
        auto value = styled_node.property(property_name);
        if (!value.has_value())
            return fallback;
        return value.value()->to_length();
    };

    auto width = length_or_fallback("width", auto_value);
    auto margin_left = length_or_fallback("margin-left", zero_value);
    auto margin_right = length_or_fallback("margin-right", zero_value);
    auto border_left = length_or_fallback("border-left", zero_value);
    auto border_right = length_or_fallback("border-right", zero_value);
    auto padding_left = length_or_fallback("padding-left", zero_value);
    auto padding_right = length_or_fallback("padding-right", zero_value);

    dbg() << " Left: " << margin_left << "+" << border_left << "+" << padding_left;
    dbg() << "Right: " << margin_right << "+" << border_right << "+" << padding_right;

    int total_px = 0;
    for (auto& value : { margin_left, border_left, padding_left, width, padding_right, border_right, margin_right }) {
        total_px += value.to_px();
    }

    dbg() << "Total: " << total_px;

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

void LayoutBlock::compute_height()
{
}
