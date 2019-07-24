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

    auto auto_value= LengthStyleValue::create({});
    auto& styled_node = *this->styled_node();
    auto width = styled_node.property("width").value_or(auto_value);

    auto zero_value = LengthStyleValue::create(Length(0, Length::Type::Absolute));

    auto margin_left = styled_node.property("margin-left").value_or(zero_value);
    auto margin_right = styled_node.property("margin-right").value_or(zero_value);
    auto border_left = styled_node.property("border-left").value_or(zero_value);
    auto border_right = styled_node.property("border-right").value_or(zero_value);
    auto padding_left = styled_node.property("padding-left").value_or(zero_value);
    auto padding_right = styled_node.property("padding-right").value_or(zero_value);

    dbg() << " Left: " << margin_left->to_string() << "+" << border_left->to_string() << "+" << padding_left->to_string();
    dbg() << "Right: " << margin_right->to_string() << "+" << border_right->to_string() << "+" << padding_right->to_string();
}

void LayoutBlock::compute_height()
{
}
