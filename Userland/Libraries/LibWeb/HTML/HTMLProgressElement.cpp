/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/HTML/HTMLProgressElement.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/Progress.h>

namespace Web::HTML {

HTMLProgressElement::HTMLProgressElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLProgressElement::~HTMLProgressElement() = default;

void HTMLProgressElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLProgressElementPrototype>(realm, "HTMLProgressElement"));
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

double HTMLProgressElement::value() const
{
    auto const& value_characters = attribute(HTML::AttributeNames::value);
    if (value_characters == nullptr)
        return 0;

    // https://html.spec.whatwg.org/multipage/common-microsyntaxes.html#rules-for-parsing-floating-point-number-values
    // 6. Skip ASCII whitespace within input given position.
    auto maybe_double = value_characters.to_double(AK::TrimWhitespace::Yes);
    if (!maybe_double.has_value())
        return 0;
    if (!isfinite(maybe_double.value()) || maybe_double.value() < 0)
        return 0;

    return min(maybe_double.value(), max());
}

WebIDL::ExceptionOr<void> HTMLProgressElement::set_value(double value)
{
    if (value < 0)
        return {};

    TRY(set_attribute(HTML::AttributeNames::value, DeprecatedString::number(value)));
    progress_position_updated();
    return {};
}

double HTMLProgressElement::max() const
{
    auto const& max_characters = attribute(HTML::AttributeNames::max);
    if (max_characters == nullptr)
        return 1;

    // https://html.spec.whatwg.org/multipage/common-microsyntaxes.html#rules-for-parsing-floating-point-number-values
    // 6. Skip ASCII whitespace within input given position.
    auto double_or_none = max_characters.to_double(AK::TrimWhitespace::Yes);
    if (!double_or_none.has_value())
        return 1;
    if (!isfinite(double_or_none.value()) || double_or_none.value() <= 0)
        return 1;

    return double_or_none.value();
}

WebIDL::ExceptionOr<void> HTMLProgressElement::set_max(double value)
{
    if (value <= 0)
        return {};

    TRY(set_attribute(HTML::AttributeNames::max, DeprecatedString::number(value)));
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
