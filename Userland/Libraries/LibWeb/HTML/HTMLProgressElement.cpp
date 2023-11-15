/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/HTML/HTMLProgressElement.h>
#include <LibWeb/HTML/Numbers.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/Progress.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(HTMLProgressElement);

HTMLProgressElement::HTMLProgressElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLProgressElement::~HTMLProgressElement() = default;

void HTMLProgressElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLProgressElementPrototype>(realm, "HTMLProgressElement"_fly_string));
}

JS::GCPtr<Layout::Node> HTMLProgressElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    if (style->appearance().value_or(CSS::Appearance::Auto) == CSS::Appearance::None)
        return HTMLElement::create_layout_node(style);
    return heap().allocate_without_realm<Layout::Progress>(document(), *this, move(style));
}

bool HTMLProgressElement::using_system_appearance() const
{
    if (layout_node())
        return is<Layout::Progress>(*layout_node());
    return false;
}

void HTMLProgressElement::progress_position_updated()
{
    if (using_system_appearance())
        layout_node()->set_needs_display();
    else
        document().invalidate_layout();
}

// https://html.spec.whatwg.org/multipage/form-elements.html#dom-progress-value
double HTMLProgressElement::value() const
{
    auto maybe_value_string = get_attribute(HTML::AttributeNames::value);
    if (!maybe_value_string.has_value())
        return 0;
    auto maybe_value = parse_floating_point_number(maybe_value_string.value());
    if (!maybe_value.has_value())
        return 0;
    return clamp(maybe_value.value(), 0, max());
}

WebIDL::ExceptionOr<void> HTMLProgressElement::set_value(double value)
{
    if (value < 0 || value > max())
        return {};

    TRY(set_attribute(HTML::AttributeNames::value, MUST(String::number(value))));
    progress_position_updated();
    return {};
}

// https://html.spec.whatwg.org/multipage/form-elements.html#dom-progress-max
double HTMLProgressElement::max() const
{
    auto maybe_max_string = get_attribute(HTML::AttributeNames::max);
    if (!maybe_max_string.has_value())
        return 1;
    auto maybe_max = parse_floating_point_number(maybe_max_string.value());
    if (!maybe_max.has_value())
        return 1;
    return AK::max(maybe_max.value(), 0);
}

WebIDL::ExceptionOr<void> HTMLProgressElement::set_max(double value)
{
    if (value <= 0)
        return {};

    TRY(set_attribute(HTML::AttributeNames::max, MUST(String::number(value))));
    progress_position_updated();
    return {};
}

double HTMLProgressElement::position() const
{
    if (!is_determinate())
        return -1;

    return value() / max();
}

}
