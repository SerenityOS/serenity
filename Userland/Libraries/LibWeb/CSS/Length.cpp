/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullOwnPtr.h>
#include <AK/Variant.h>
#include <LibGfx/Font.h>
#include <LibGfx/Rect.h>
#include <LibWeb/CSS/Length.h>
#include <LibWeb/CSS/Percentage.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>

namespace Web::CSS {

Length::Length(int value, Type type)
    : m_type(type)
    , m_value(value)
{
}
Length::Length(float value, Type type)
    : m_type(type)
    , m_value(value)
{
}

Length Length::make_auto()
{
    return Length(0, Type::Auto);
}

Length Length::make_px(float value)
{
    return Length(value, Type::Px);
}

Length Length::make_calculated(NonnullRefPtr<CalculatedStyleValue> calculated_style_value)
{
    Length length { 0, Type::Calculated };
    length.m_calculated_style = move(calculated_style_value);
    return length;
}

Length Length::percentage_of(Percentage const& percentage) const
{
    VERIFY(!is_calculated());

    if (is_auto()) {
        dbgln("Attempting to get percentage of an auto length, this seems wrong? But for now we just return the original length.");
        return *this;
    }

    return Length { percentage.as_fraction() * raw_value(), m_type };
}

Length Length::resolved(Layout::Node const& layout_node) const
{
    if (is_calculated())
        return m_calculated_style->resolve_length(layout_node).release_value();
    if (is_relative())
        return make_px(to_px(layout_node));
    return *this;
}

float Length::relative_length_to_px(Gfx::IntRect const& viewport_rect, Gfx::FontMetrics const& font_metrics, float font_size, float root_font_size) const
{
    switch (m_type) {
    case Type::Ex:
        return m_value * font_metrics.x_height;
    case Type::Em:
        return m_value * font_size;
    case Type::Ch:
        // FIXME: Use layout_node.font().glyph_height() when writing-mode is not horizontal-tb (it has to be implemented first)
        return m_value * (font_metrics.glyph_width + font_metrics.glyph_spacing);
    case Type::Rem:
        return m_value * root_font_size;
    case Type::Vw:
        return viewport_rect.width() * (m_value / 100);
    case Type::Vh:
        return viewport_rect.height() * (m_value / 100);
    case Type::Vmin:
        return min(viewport_rect.width(), viewport_rect.height()) * (m_value / 100);
    case Type::Vmax:
        return max(viewport_rect.width(), viewport_rect.height()) * (m_value / 100);
    default:
        VERIFY_NOT_REACHED();
    }
}

float Length::to_px(Layout::Node const& layout_node) const
{
    if (is_calculated())
        return m_calculated_style->resolve_length(layout_node)->to_px(layout_node);

    if (!layout_node.document().browsing_context())
        return 0;
    auto viewport_rect = layout_node.document().browsing_context()->viewport_rect();
    auto* root_element = layout_node.document().document_element();
    if (!root_element || !root_element->layout_node())
        return 0;
    return to_px(viewport_rect, layout_node.font().metrics('M'), layout_node.computed_values().font_size(), root_element->layout_node()->computed_values().font_size());
}

String Length::to_string() const
{
    if (is_calculated())
        return m_calculated_style->to_string();
    if (is_auto())
        return "auto";
    return String::formatted("{}{}", m_value, unit_name());
}

const char* Length::unit_name() const
{
    switch (m_type) {
    case Type::Cm:
        return "cm";
    case Type::In:
        return "in";
    case Type::Px:
        return "px";
    case Type::Pt:
        return "pt";
    case Type::Mm:
        return "mm";
    case Type::Q:
        return "Q";
    case Type::Pc:
        return "pc";
    case Type::Ex:
        return "ex";
    case Type::Em:
        return "em";
    case Type::Ch:
        return "ch";
    case Type::Rem:
        return "rem";
    case Type::Auto:
        return "auto";
    case Type::Vh:
        return "vh";
    case Type::Vw:
        return "vw";
    case Type::Vmax:
        return "vmax";
    case Type::Vmin:
        return "vmin";
    case Type::Calculated:
        return "calculated";
    }
    VERIFY_NOT_REACHED();
}

Optional<Length::Type> Length::unit_from_name(StringView name)
{
    if (name.equals_ignoring_case("px"sv)) {
        return Length::Type::Px;
    } else if (name.equals_ignoring_case("pt"sv)) {
        return Length::Type::Pt;
    } else if (name.equals_ignoring_case("pc"sv)) {
        return Length::Type::Pc;
    } else if (name.equals_ignoring_case("mm"sv)) {
        return Length::Type::Mm;
    } else if (name.equals_ignoring_case("rem"sv)) {
        return Length::Type::Rem;
    } else if (name.equals_ignoring_case("em"sv)) {
        return Length::Type::Em;
    } else if (name.equals_ignoring_case("ex"sv)) {
        return Length::Type::Ex;
    } else if (name.equals_ignoring_case("ch"sv)) {
        return Length::Type::Ch;
    } else if (name.equals_ignoring_case("vw"sv)) {
        return Length::Type::Vw;
    } else if (name.equals_ignoring_case("vh"sv)) {
        return Length::Type::Vh;
    } else if (name.equals_ignoring_case("vmax"sv)) {
        return Length::Type::Vmax;
    } else if (name.equals_ignoring_case("vmin"sv)) {
        return Length::Type::Vmin;
    } else if (name.equals_ignoring_case("cm"sv)) {
        return Length::Type::Cm;
    } else if (name.equals_ignoring_case("in"sv)) {
        return Length::Type::In;
    } else if (name.equals_ignoring_case("Q"sv)) {
        return Length::Type::Q;
    }

    return {};
}

}
