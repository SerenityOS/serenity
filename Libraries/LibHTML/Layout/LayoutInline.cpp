#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutInline.h>

LayoutInline::LayoutInline(const Node& node, StyleProperties&& style_properties)
    : LayoutNode(&node, move(style_properties))
{
}

LayoutInline::~LayoutInline()
{
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
