#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutInline.h>

LayoutInline::LayoutInline(const Node& node, StyleProperties&& style_properties)
    : LayoutNode(&node, move(style_properties))
{
}

LayoutInline::~LayoutInline()
{
}

void LayoutInline::layout()
{
    Point origin;

    if (previous_sibling() != nullptr) {
        auto& previous_sibling_rect = previous_sibling()->rect();
        auto& previous_sibling_style = previous_sibling()->style();
        origin = previous_sibling_rect.location();
        // FIXME: Implement proper inline positioning when
        // there are nodes with different heights. And don't
        // hardcode font size like we do here.
        origin.move_by(previous_sibling_rect.width(), previous_sibling_rect.height());
        origin.move_by(previous_sibling_style.full_margin().right, -11);
    } else {
        origin = parent()->rect().location();
    }

    rect().set_location(origin);

    for_each_child([&](auto& child) {
        child.layout();
        rect().set_right(child.rect().right() + child.style().full_margin().right);
        rect().set_bottom(child.rect().bottom() + child.style().full_margin().bottom);
    });
}

void LayoutInline::split_into_lines(LayoutBlock& container)
{
    for_each_child([&](auto& child) {
        if (child.is_inline()) {
            static_cast<LayoutInline&>(child).split_into_lines(container);
        } else {
            // FIXME: Support block children of inlines.
        }
    });
}
