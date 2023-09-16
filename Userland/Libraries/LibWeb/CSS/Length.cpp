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
#include <LibWeb/Layout/Node.h>

namespace Web::CSS {

Length::FontMetrics::FontMetrics(CSSPixels font_size, Gfx::FontPixelMetrics const& pixel_metrics, CSSPixels line_height)
    : font_size(font_size)
    , x_height(pixel_metrics.x_height)
    // FIXME: This is only approximately the cap height. The spec suggests measuring the "O" glyph:
    //        https://www.w3.org/TR/css-values-4/#cap
    , cap_height(pixel_metrics.ascent)
    , zero_advance(pixel_metrics.advance_of_ascii_zero + pixel_metrics.glyph_spacing)
    , line_height(line_height)
{
}

Length::Length(double value, Type type)
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
    return Length(value.to_double(), Type::Px);
}

Length Length::percentage_of(Percentage const& percentage) const
{
    if (is_auto()) {
        dbgln("Attempting to get percentage of an auto length, this seems wrong? But for now we just return the original length.");
        return *this;
    }

    return Length { percentage.as_fraction() * raw_value(), m_type };
}

CSSPixels Length::font_relative_length_to_px(Length::FontMetrics const& font_metrics, Length::FontMetrics const& root_font_metrics) const
{
    switch (m_type) {
    case Type::Em:
        return CSSPixels::nearest_value_for(m_value * font_metrics.font_size.to_double());
    case Type::Rem:
        return CSSPixels::nearest_value_for(m_value * root_font_metrics.font_size.to_double());
    case Type::Ex:
        return CSSPixels::nearest_value_for(m_value * font_metrics.x_height.to_double());
    case Type::Rex:
        return CSSPixels::nearest_value_for(m_value * root_font_metrics.x_height.to_double());
    case Type::Cap:
        return CSSPixels::nearest_value_for(m_value * font_metrics.cap_height.to_double());
    case Type::Rcap:
        return CSSPixels::nearest_value_for(m_value * root_font_metrics.cap_height.to_double());
    case Type::Ch:
        return CSSPixels::nearest_value_for(m_value * font_metrics.zero_advance.to_double());
    case Type::Rch:
        return CSSPixels::nearest_value_for(m_value * root_font_metrics.zero_advance.to_double());
    case Type::Ic:
        // FIXME: Use the "advance measure of the “水” (CJK water ideograph, U+6C34) glyph"
        return CSSPixels::nearest_value_for(m_value * font_metrics.font_size.to_double());
    case Type::Ric:
        // FIXME: Use the "advance measure of the “水” (CJK water ideograph, U+6C34) glyph"
        return CSSPixels::nearest_value_for(m_value * root_font_metrics.font_size.to_double());
    case Type::Lh:
        return CSSPixels::nearest_value_for(m_value * font_metrics.line_height.to_double());
    case Type::Rlh:
        return CSSPixels::nearest_value_for(m_value * root_font_metrics.line_height.to_double());
    default:
        VERIFY_NOT_REACHED();
    }
}

CSSPixels Length::viewport_relative_length_to_px(CSSPixelRect const& viewport_rect) const
{
    switch (m_type) {
    case Type::Vw:
    case Type::Svw:
    case Type::Lvw:
    case Type::Dvw:
        return viewport_rect.width() * (CSSPixels::nearest_value_for(m_value) / 100);
    case Type::Vh:
    case Type::Svh:
    case Type::Lvh:
    case Type::Dvh:
        return viewport_rect.height() * (CSSPixels::nearest_value_for(m_value) / 100);
    case Type::Vi:
    case Type::Svi:
    case Type::Lvi:
    case Type::Dvi:
        // FIXME: Select the width or height based on which is the inline axis.
        return viewport_rect.width() * (CSSPixels::nearest_value_for(m_value) / 100);
    case Type::Vb:
    case Type::Svb:
    case Type::Lvb:
    case Type::Dvb:
        // FIXME: Select the width or height based on which is the block axis.
        return viewport_rect.height() * (CSSPixels::nearest_value_for(m_value) / 100);
    case Type::Vmin:
    case Type::Svmin:
    case Type::Lvmin:
    case Type::Dvmin:
        return min(viewport_rect.width(), viewport_rect.height()) * (CSSPixels::nearest_value_for(m_value) / 100);
    case Type::Vmax:
    case Type::Svmax:
    case Type::Lvmax:
    case Type::Dvmax:
        return max(viewport_rect.width(), viewport_rect.height()) * (CSSPixels::nearest_value_for(m_value) / 100);
    default:
        VERIFY_NOT_REACHED();
    }
}

Length::ResolutionContext Length::ResolutionContext::for_layout_node(Layout::Node const& node)
{
    auto const* root_element = node.document().document_element();
    VERIFY(root_element);
    VERIFY(root_element->layout_node());
    return Length::ResolutionContext {
        .viewport_rect = node.navigable()->viewport_rect(),
        .font_metrics = { node.computed_values().font_size(), node.font().pixel_metrics(), node.line_height() },
        .root_font_metrics = { root_element->layout_node()->computed_values().font_size(), root_element->layout_node()->font().pixel_metrics(), root_element->layout_node()->line_height() },
    };
}

CSSPixels Length::to_px(ResolutionContext const& context) const
{
    return to_px(context.viewport_rect, context.font_metrics, context.root_font_metrics);
}

CSSPixels Length::to_px(Layout::Node const& layout_node) const
{
    if (is_auto()) {
        // FIXME: We really, really shouldn't end up here, but we do, and so frequently that
        //        adding a dbgln() here outputs a couple hundred lines loading `welcome.html`.
        return 0;
    }

    if (is_absolute())
        return absolute_length_to_px();

    if (!layout_node.document().browsing_context())
        return 0;

    if (is_font_relative()) {
        auto* root_element = layout_node.document().document_element();
        if (!root_element || !root_element->layout_node())
            return 0;

        FontMetrics font_metrics {
            layout_node.computed_values().font_size(),
            layout_node.font().pixel_metrics(),
            layout_node.line_height()
        };
        FontMetrics root_font_metrics {
            root_element->layout_node()->computed_values().font_size(),
            root_element->layout_node()->font().pixel_metrics(),
            root_element->layout_node()->line_height()
        };

        return font_relative_length_to_px(font_metrics, root_font_metrics);
    }

    VERIFY(is_viewport_relative());
    auto const& viewport_rect = layout_node.document().viewport_rect();
    return viewport_relative_length_to_px(viewport_rect);
}

String Length::to_string() const
{
    if (is_auto())
        return "auto"_string;
    return MUST(String::formatted("{}{}", m_value, unit_name()));
}

char const* Length::unit_name() const
{
    switch (m_type) {
    case Type::Em:
        return "em";
    case Type::Rem:
        return "rem";
    case Type::Ex:
        return "ex";
    case Type::Rex:
        return "rex";
    case Type::Cap:
        return "cap";
    case Type::Rcap:
        return "rcap";
    case Type::Ch:
        return "ch";
    case Type::Rch:
        return "rch";
    case Type::Ic:
        return "ic";
    case Type::Ric:
        return "ric";
    case Type::Lh:
        return "lh";
    case Type::Rlh:
        return "rlh";
    case Type::Vw:
        return "vw";
    case Type::Svw:
        return "svw";
    case Type::Lvw:
        return "lvw";
    case Type::Dvw:
        return "dvw";
    case Type::Vh:
        return "vh";
    case Type::Svh:
        return "svh";
    case Type::Lvh:
        return "lvh";
    case Type::Dvh:
        return "dvh";
    case Type::Vi:
        return "vi";
    case Type::Svi:
        return "svi";
    case Type::Lvi:
        return "lvi";
    case Type::Dvi:
        return "dvi";
    case Type::Vb:
        return "vb";
    case Type::Svb:
        return "svb";
    case Type::Lvb:
        return "lvb";
    case Type::Dvb:
        return "dvb";
    case Type::Vmin:
        return "vmin";
    case Type::Svmin:
        return "svmin";
    case Type::Lvmin:
        return "lvmin";
    case Type::Dvmin:
        return "dvmin";
    case Type::Vmax:
        return "vmax";
    case Type::Svmax:
        return "svmax";
    case Type::Lvmax:
        return "lvmax";
    case Type::Dvmax:
        return "dvmax";
    case Type::Cm:
        return "cm";
    case Type::Mm:
        return "mm";
    case Type::Q:
        return "Q";
    case Type::In:
        return "in";
    case Type::Pt:
        return "pt";
    case Type::Pc:
        return "pc";
    case Type::Px:
        return "px";
    case Type::Auto:
        return "auto";
    }
    VERIFY_NOT_REACHED();
}

Optional<Length::Type> Length::unit_from_name(StringView name)
{
    if (name.equals_ignoring_ascii_case("em"sv)) {
        return Length::Type::Em;
    } else if (name.equals_ignoring_ascii_case("rem"sv)) {
        return Length::Type::Rem;
    } else if (name.equals_ignoring_ascii_case("ex"sv)) {
        return Length::Type::Ex;
    } else if (name.equals_ignoring_ascii_case("rex"sv)) {
        return Length::Type::Rex;
    } else if (name.equals_ignoring_ascii_case("cap"sv)) {
        return Length::Type::Cap;
    } else if (name.equals_ignoring_ascii_case("rcap"sv)) {
        return Length::Type::Rcap;
    } else if (name.equals_ignoring_ascii_case("ch"sv)) {
        return Length::Type::Ch;
    } else if (name.equals_ignoring_ascii_case("rch"sv)) {
        return Length::Type::Rch;
    } else if (name.equals_ignoring_ascii_case("ic"sv)) {
        return Length::Type::Ic;
    } else if (name.equals_ignoring_ascii_case("ric"sv)) {
        return Length::Type::Ric;
    } else if (name.equals_ignoring_ascii_case("lh"sv)) {
        return Length::Type::Lh;
    } else if (name.equals_ignoring_ascii_case("rlh"sv)) {
        return Length::Type::Rlh;
    } else if (name.equals_ignoring_ascii_case("vw"sv)) {
        return Length::Type::Vw;
    } else if (name.equals_ignoring_ascii_case("svw"sv)) {
        return Length::Type::Svw;
    } else if (name.equals_ignoring_ascii_case("lvw"sv)) {
        return Length::Type::Lvw;
    } else if (name.equals_ignoring_ascii_case("dvw"sv)) {
        return Length::Type::Dvw;
    } else if (name.equals_ignoring_ascii_case("vh"sv)) {
        return Length::Type::Vh;
    } else if (name.equals_ignoring_ascii_case("svh"sv)) {
        return Length::Type::Svh;
    } else if (name.equals_ignoring_ascii_case("lvh"sv)) {
        return Length::Type::Lvh;
    } else if (name.equals_ignoring_ascii_case("dvh"sv)) {
        return Length::Type::Dvh;
    } else if (name.equals_ignoring_ascii_case("vi"sv)) {
        return Length::Type::Vi;
    } else if (name.equals_ignoring_ascii_case("svi"sv)) {
        return Length::Type::Svi;
    } else if (name.equals_ignoring_ascii_case("lvi"sv)) {
        return Length::Type::Lvi;
    } else if (name.equals_ignoring_ascii_case("dvi"sv)) {
        return Length::Type::Dvi;
    } else if (name.equals_ignoring_ascii_case("vb"sv)) {
        return Length::Type::Vb;
    } else if (name.equals_ignoring_ascii_case("svb"sv)) {
        return Length::Type::Svb;
    } else if (name.equals_ignoring_ascii_case("lvb"sv)) {
        return Length::Type::Lvb;
    } else if (name.equals_ignoring_ascii_case("dvb"sv)) {
        return Length::Type::Dvb;
    } else if (name.equals_ignoring_ascii_case("vmin"sv)) {
        return Length::Type::Vmin;
    } else if (name.equals_ignoring_ascii_case("svmin"sv)) {
        return Length::Type::Svmin;
    } else if (name.equals_ignoring_ascii_case("lvmin"sv)) {
        return Length::Type::Lvmin;
    } else if (name.equals_ignoring_ascii_case("dvmin"sv)) {
        return Length::Type::Dvmin;
    } else if (name.equals_ignoring_ascii_case("vmax"sv)) {
        return Length::Type::Vmax;
    } else if (name.equals_ignoring_ascii_case("svmax"sv)) {
        return Length::Type::Svmax;
    } else if (name.equals_ignoring_ascii_case("lvmax"sv)) {
        return Length::Type::Lvmax;
    } else if (name.equals_ignoring_ascii_case("dvmax"sv)) {
        return Length::Type::Dvmax;
    } else if (name.equals_ignoring_ascii_case("cm"sv)) {
        return Length::Type::Cm;
    } else if (name.equals_ignoring_ascii_case("mm"sv)) {
        return Length::Type::Mm;
    } else if (name.equals_ignoring_ascii_case("Q"sv)) {
        return Length::Type::Q;
    } else if (name.equals_ignoring_ascii_case("in"sv)) {
        return Length::Type::In;
    } else if (name.equals_ignoring_ascii_case("pt"sv)) {
        return Length::Type::Pt;
    } else if (name.equals_ignoring_ascii_case("pc"sv)) {
        return Length::Type::Pc;
    } else if (name.equals_ignoring_ascii_case("px"sv)) {
        return Length::Type::Px;
    }

    return {};
}

Optional<Length> Length::absolutize(CSSPixelRect const& viewport_rect, FontMetrics const& font_metrics, FontMetrics const& root_font_metrics) const
{
    if (is_px())
        return {};
    if (is_absolute() || is_relative()) {
        auto px = to_px(viewport_rect, font_metrics, root_font_metrics);
        return CSS::Length::make_px(px);
    }
    return {};
}

Length Length::absolutized(CSSPixelRect const& viewport_rect, FontMetrics const& font_metrics, FontMetrics const& root_font_metrics) const
{
    return absolutize(viewport_rect, font_metrics, root_font_metrics).value_or(*this);
}

}
