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

Length::Length() = default;
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

Length Length::percentage_of(Percentage const& percentage) const
{
    if (is_undefined_or_auto()) {
        dbgln("Attempting to get percentage of an undefined or auto length, this seems wrong? But for now we just return the original length.");
        return *this;
    }

    return Length { percentage.as_fraction() * raw_value(), m_type };
}

Length Length::resolved(Length const& fallback_for_undefined, Layout::Node const& layout_node) const
{
    if (is_undefined())
        return fallback_for_undefined;
    if (is_calculated())
        return Length(resolve_calculated_value(layout_node), Type::Px);
    if (is_relative())
        return make_px(to_px(layout_node));
    return *this;
}

Length Length::resolved_or_auto(Layout::Node const& layout_node) const
{
    return resolved(make_auto(), layout_node);
}

Length Length::resolved_or_zero(Layout::Node const& layout_node) const
{
    return resolved(make_px(0), layout_node);
}

void Length::set_calculated_style(CalculatedStyleValue* value)
{
    m_calculated_style = value;
}

float Length::relative_length_to_px(Gfx::IntRect const& viewport_rect, Gfx::FontMetrics const& font_metrics, float root_font_size) const
{
    switch (m_type) {
    case Type::Ex:
        return m_value * font_metrics.x_height;
    case Type::Em:
        return m_value * font_metrics.size;
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
    if (!layout_node.document().browsing_context())
        return 0;
    auto viewport_rect = layout_node.document().browsing_context()->viewport_rect();
    auto* root_element = layout_node.document().document_element();
    if (!root_element || !root_element->layout_node())
        return 0;
    return to_px(viewport_rect, layout_node.font().metrics('M'), root_element->layout_node()->font().presentation_size());
}

static float resolve_calc_value(CalculatedStyleValue::CalcValue const& calc_value, Layout::Node const& layout_node);
static float resolve_calc_number_value(CalculatedStyleValue::CalcNumberValue const&);
static float resolve_calc_product(NonnullOwnPtr<CalculatedStyleValue::CalcProduct> const& calc_product, Layout::Node const& layout_node);
static float resolve_calc_sum(NonnullOwnPtr<CalculatedStyleValue::CalcSum> const& calc_sum, Layout::Node const& layout_node);
static float resolve_calc_number_sum(NonnullOwnPtr<CalculatedStyleValue::CalcNumberSum> const&);
static float resolve_calc_number_product(NonnullOwnPtr<CalculatedStyleValue::CalcNumberProduct> const&);

float Length::resolve_calculated_value(Layout::Node const& layout_node) const
{
    if (!m_calculated_style)
        return 0.0f;

    auto& expression = m_calculated_style->expression();

    auto length = resolve_calc_sum(expression, layout_node);
    return length;
};

static float resolve_calc_value(CalculatedStyleValue::CalcValue const& calc_value, Layout::Node const& layout_node)
{
    return calc_value.visit(
        [](float value) { return value; },
        [&](Length const& length) {
            return length.resolved_or_zero(layout_node).to_px(layout_node);
        },
        [&](NonnullOwnPtr<CalculatedStyleValue::CalcSum> const& calc_sum) {
            return resolve_calc_sum(calc_sum, layout_node);
        },
        [](auto&) {
            VERIFY_NOT_REACHED();
            return 0.0f;
        });
}

static float resolve_calc_number_product(NonnullOwnPtr<CalculatedStyleValue::CalcNumberProduct> const& calc_number_product)
{
    auto value = resolve_calc_number_value(calc_number_product->first_calc_number_value);

    for (auto& additional_number_value : calc_number_product->zero_or_more_additional_calc_number_values) {
        auto additional_value = resolve_calc_number_value(additional_number_value.value);
        if (additional_number_value.op == CalculatedStyleValue::CalcNumberProductPartWithOperator::Multiply)
            value *= additional_value;
        else if (additional_number_value.op == CalculatedStyleValue::CalcNumberProductPartWithOperator::Divide)
            value /= additional_value;
        else
            VERIFY_NOT_REACHED();
    }

    return value;
}

static float resolve_calc_number_sum(NonnullOwnPtr<CalculatedStyleValue::CalcNumberSum> const& calc_number_sum)
{
    auto value = resolve_calc_number_product(calc_number_sum->first_calc_number_product);

    for (auto& additional_product : calc_number_sum->zero_or_more_additional_calc_number_products) {
        auto additional_value = resolve_calc_number_product(additional_product.calc_number_product);
        if (additional_product.op == CSS::CalculatedStyleValue::CalcNumberSumPartWithOperator::Add)
            value += additional_value;
        else if (additional_product.op == CSS::CalculatedStyleValue::CalcNumberSumPartWithOperator::Subtract)
            value -= additional_value;
        else
            VERIFY_NOT_REACHED();
    }

    return value;
}

static float resolve_calc_number_value(CalculatedStyleValue::CalcNumberValue const& number_value)
{
    return number_value.visit(
        [](float number) { return number; },
        [](NonnullOwnPtr<CalculatedStyleValue::CalcNumberSum> const& calc_number_sum) {
            return resolve_calc_number_sum(calc_number_sum);
        });
}

static float resolve_calc_product(NonnullOwnPtr<CalculatedStyleValue::CalcProduct> const& calc_product, Layout::Node const& layout_node)
{
    auto value = resolve_calc_value(calc_product->first_calc_value, layout_node);

    for (auto& additional_value : calc_product->zero_or_more_additional_calc_values) {
        additional_value.value.visit(
            [&](CalculatedStyleValue::CalcValue const& calc_value) {
                if (additional_value.op != CalculatedStyleValue::CalcProductPartWithOperator::Multiply)
                    VERIFY_NOT_REACHED();
                auto resolved_value = resolve_calc_value(calc_value, layout_node);
                value *= resolved_value;
            },
            [&](CalculatedStyleValue::CalcNumberValue const& calc_number_value) {
                if (additional_value.op != CalculatedStyleValue::CalcProductPartWithOperator::Divide)
                    VERIFY_NOT_REACHED();
                auto resolved_calc_number_value = resolve_calc_number_value(calc_number_value);
                value /= resolved_calc_number_value;
            });
    }

    return value;
}

static float resolve_calc_sum(NonnullOwnPtr<CalculatedStyleValue::CalcSum> const& calc_sum, Layout::Node const& layout_node)
{
    auto value = resolve_calc_product(calc_sum->first_calc_product, layout_node);

    for (auto& additional_product : calc_sum->zero_or_more_additional_calc_products) {
        auto additional_value = resolve_calc_product(additional_product.calc_product, layout_node);
        if (additional_product.op == CalculatedStyleValue::CalcSumPartWithOperator::Operation::Add)
            value += additional_value;
        else if (additional_product.op == CalculatedStyleValue::CalcSumPartWithOperator::Operation::Subtract)
            value -= additional_value;
        else
            VERIFY_NOT_REACHED();
    }

    return value;
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
    case Type::Undefined:
        return "undefined";
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

}
