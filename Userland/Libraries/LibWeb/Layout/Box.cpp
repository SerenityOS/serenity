/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/FormattingContext.h>
#include <LibWeb/Painting/PaintableBox.h>

namespace Web::Layout {

Box::Box(DOM::Document& document, DOM::Node* node, NonnullRefPtr<CSS::StyleProperties> style)
    : NodeWithStyleAndBoxModelMetrics(document, node, move(style))
{
}

Box::Box(DOM::Document& document, DOM::Node* node, CSS::ComputedValues computed_values)
    : NodeWithStyleAndBoxModelMetrics(document, node, move(computed_values))
{
}

Box::~Box()
{
}

// https://www.w3.org/TR/css-overflow-3/#overflow-control
static bool overflow_value_makes_box_a_scroll_container(CSS::Overflow overflow)
{
    switch (overflow) {
    case CSS::Overflow::Clip:
    case CSS::Overflow::Visible:
        return false;
    case CSS::Overflow::Auto:
    case CSS::Overflow::Hidden:
    case CSS::Overflow::Scroll:
        return true;
    }
    VERIFY_NOT_REACHED();
}

// https://www.w3.org/TR/css-overflow-3/#scroll-container
bool Box::is_scroll_container() const
{
    // NOTE: This isn't in the spec, but we want the viewport to behave like a scroll container.
    if (is_viewport())
        return true;

    return overflow_value_makes_box_a_scroll_container(computed_values().overflow_x())
        || overflow_value_makes_box_a_scroll_container(computed_values().overflow_y());
}

bool Box::is_scrollable() const
{
    // FIXME: Support horizontal scroll as well (overflow-x)
    return computed_values().overflow_y() == CSS::Overflow::Scroll;
}

CSSPixelPoint Box::scroll_offset() const
{
    if (is_generated_for_before_pseudo_element())
        return pseudo_element_generator()->scroll_offset(DOM::Element::ScrollOffsetFor::PseudoBefore);
    if (is_generated_for_after_pseudo_element())
        return pseudo_element_generator()->scroll_offset(DOM::Element::ScrollOffsetFor::PseudoAfter);

    if (!is<DOM::Element>(*dom_node()))
        return {};

    return static_cast<DOM::Element const*>(dom_node())->scroll_offset(DOM::Element::ScrollOffsetFor::Self);
}

void Box::set_scroll_offset(CSSPixelPoint offset)
{
    // FIXME: If there is horizontal and vertical scroll ignore only part of the new offset
    if (offset.y() < 0 || scroll_offset() == offset)
        return;

    if (is_generated_for_before_pseudo_element()) {
        pseudo_element_generator()->set_scroll_offset(DOM::Element::ScrollOffsetFor::PseudoBefore, offset);
    } else if (is_generated_for_after_pseudo_element()) {
        pseudo_element_generator()->set_scroll_offset(DOM::Element::ScrollOffsetFor::PseudoAfter, offset);
    } else if (is<DOM::Element>(*dom_node())) {
        static_cast<DOM::Element*>(dom_node())->set_scroll_offset(DOM::Element::ScrollOffsetFor::Self, offset);
    } else {
        return;
    }

    set_needs_display();
}

void Box::set_needs_display()
{
    if (paintable_box())
        browsing_context().set_needs_display(paintable_box()->absolute_rect());
}

bool Box::is_body() const
{
    return dom_node() && dom_node() == document().body();
}

JS::GCPtr<Painting::Paintable> Box::create_paintable() const
{
    return Painting::PaintableBox::create(*this);
}

Painting::PaintableBox const* Box::paintable_box() const
{
    return static_cast<Painting::PaintableBox const*>(Node::paintable());
}

Optional<float> Box::preferred_aspect_ratio() const
{
    auto computed_aspect_ratio = computed_values().aspect_ratio();
    if (computed_aspect_ratio.use_natural_aspect_ratio_if_available && natural_aspect_ratio().has_value())
        return natural_aspect_ratio();
    return computed_aspect_ratio.preferred_ratio.map([](CSS::Ratio const& ratio) { return ratio.value(); });
}

}
