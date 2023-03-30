/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullOwnPtr.h>
#include <AK/Variant.h>
#include <LibGfx/Font/Font.h>
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
Length::~Length() = default;

Length Length::make_auto()
{
    return Length(0, Type::Auto);
}

Length Length::make_px(CSSPixels value)
{
    return Length(value.value(), Type::Px);
}

Length Length::percentage_of(Percentage const& percentage) const
{
    if (is_auto()) {
        dbgln("Attempting to get percentage of an auto length, this seems wrong? But for now we just return the original length.");
        return *this;
    }

    return Length { percentage.as_fraction() * raw_value(), m_type };
}

Length Length::resolved(Layout::Node const& layout_node) const
{
    if (is_relative())
        return make_px(to_px(layout_node));
    if (!isfinite(m_value))
        return make_auto();
    return *this;
}

CSSPixels Length::relative_length_to_px(CSSPixelRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, CSSPixels font_size, CSSPixels root_font_size, CSSPixels line_height, CSSPixels root_line_height) const
{
    switch (m_type) {
    case Type::Ex:
        return m_value * font_metrics.x_height;
    case Type::Em:
        return m_value * font_size;
    case Type::Ch:
        // FIXME: Use layout_node.font().pixel_size() when writing-mode is not horizontal-tb (it has to be implemented first)
        return m_value * (font_metrics.advance_of_ascii_zero + font_metrics.glyph_spacing);
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
    case Type::Lh:
        return m_value * line_height;
    case Type::Rlh:
        return m_value * root_line_height;
    default:
        VERIFY_NOT_REACHED();
    }
}

CSSPixels Length::to_px(Layout::Node const& layout_node) const
{
    if (is_absolute())
        return absolute_length_to_px();

    if (!layout_node.document().browsing_context())
        return 0;
    auto const& viewport_rect = layout_node.document().browsing_context()->viewport_rect();
    auto* root_element = layout_node.document().document_element();
    if (!root_element || !root_element->layout_node())
        return 0;
    return to_px(viewport_rect, layout_node.font().pixel_metrics(), layout_node.computed_values().font_size(), root_element->layout_node()->computed_values().font_size(), layout_node.line_height(), root_element->layout_node()->line_height());
}

ErrorOr<String> Length::to_string() const
{
    if (is_auto())
        return "auto"_string;
    return String::formatted("{}{}", m_value, unit_name());
}

char const* Length::unit_name() const
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
    case Type::Lh:
        return "lh";
    case Type::Rlh:
        return "rlh";
    }
    VERIFY_NOT_REACHED();
}

Optional<Length::Type> Length::unit_from_name(StringView name)
{
    if (name.equals_ignoring_ascii_case("px"sv)) {
        return Length::Type::Px;
    } else if (name.equals_ignoring_ascii_case("pt"sv)) {
        return Length::Type::Pt;
    } else if (name.equals_ignoring_ascii_case("pc"sv)) {
        return Length::Type::Pc;
    } else if (name.equals_ignoring_ascii_case("mm"sv)) {
        return Length::Type::Mm;
    } else if (name.equals_ignoring_ascii_case("rem"sv)) {
        return Length::Type::Rem;
    } else if (name.equals_ignoring_ascii_case("em"sv)) {
        return Length::Type::Em;
    } else if (name.equals_ignoring_ascii_case("ex"sv)) {
        return Length::Type::Ex;
    } else if (name.equals_ignoring_ascii_case("ch"sv)) {
        return Length::Type::Ch;
    } else if (name.equals_ignoring_ascii_case("vw"sv)) {
        return Length::Type::Vw;
    } else if (name.equals_ignoring_ascii_case("vh"sv)) {
        return Length::Type::Vh;
    } else if (name.equals_ignoring_ascii_case("vmax"sv)) {
        return Length::Type::Vmax;
    } else if (name.equals_ignoring_ascii_case("vmin"sv)) {
        return Length::Type::Vmin;
    } else if (name.equals_ignoring_ascii_case("cm"sv)) {
        return Length::Type::Cm;
    } else if (name.equals_ignoring_ascii_case("in"sv)) {
        return Length::Type::In;
    } else if (name.equals_ignoring_ascii_case("Q"sv)) {
        return Length::Type::Q;
    } else if (name.equals_ignoring_ascii_case("lh"sv)) {
        return Length::Type::Lh;
    } else if (name.equals_ignoring_ascii_case("rlh"sv)) {
        return Length::Type::Rlh;
    }

    return {};
}

}
