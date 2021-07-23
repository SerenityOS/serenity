/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobi@tobyase.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/Variant.h>
#include <LibWeb/CSS/Length.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/Page/BrowsingContext.h>

namespace Web::CSS {

float Length::relative_length_to_px(const Layout::Node& layout_node) const
{
    switch (m_type) {
    case Type::Ex:
        return m_value * layout_node.font().x_height();
    case Type::Em:
        return m_value * layout_node.font_size();
    case Type::Rem:
        return m_value * layout_node.document().document_element()->layout_node()->font_size();
    case Type::Vw:
        return layout_node.document().browsing_context()->viewport_rect().width() * (m_value / 100);
    case Type::Vh:
        return layout_node.document().browsing_context()->viewport_rect().height() * (m_value / 100);
    case Type::Vmin: {
        auto viewport = layout_node.document().browsing_context()->viewport_rect();

        return min(viewport.width(), viewport.height()) * (m_value / 100);
    }
    case Type::Vmax: {
        auto viewport = layout_node.document().browsing_context()->viewport_rect();

        return max(viewport.width(), viewport.height()) * (m_value / 100);
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

static float resolve_calc_value(CalculatedStyleValue::CalcValue const&, const Layout::Node& layout_node, float reference_for_percent);
static float resolve_calc_number_value(CalculatedStyleValue::CalcNumberValue const&);
static float resolve_calc_product(NonnullOwnPtr<CalculatedStyleValue::CalcProduct> const&, const Layout::Node& layout_node, float reference_for_percent);
static float resolve_calc_sum(NonnullOwnPtr<CalculatedStyleValue::CalcSum> const&, const Layout::Node& layout_node, float reference_for_percent);
static float resolve_calc_number_sum(NonnullOwnPtr<CalculatedStyleValue::CalcNumberSum> const&);
static float resolve_calc_number_product(NonnullOwnPtr<CalculatedStyleValue::CalcNumberProduct> const&);

float Length::resolve_calculated_value(const Layout::Node& layout_node, float reference_for_percent) const
{
    if (!m_calculated_style)
        return 0.0f;

    auto& expression = m_calculated_style->expression();

    auto length = resolve_calc_sum(expression, layout_node, reference_for_percent);
    return length;
};

static float resolve_calc_value(CalculatedStyleValue::CalcValue const& calc_value, const Layout::Node& layout_node, float reference_for_percent)
{
    return calc_value.visit(
        [](float value) { return value; },
        [&](Length length) {
            return length.resolved_or_zero(layout_node, reference_for_percent).to_px(layout_node);
        },
        [&](NonnullOwnPtr<CalculatedStyleValue::CalcSum>& calc_sum) {
            return resolve_calc_sum(calc_sum, layout_node, reference_for_percent);
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
        [](NonnullOwnPtr<CalculatedStyleValue::CalcNumberSum>& calc_number_sum) {
            return resolve_calc_number_sum(calc_number_sum);
        });
}

static float resolve_calc_product(NonnullOwnPtr<CalculatedStyleValue::CalcProduct> const& calc_product, const Layout::Node& layout_node, float reference_for_percent)
{
    auto value = resolve_calc_value(calc_product->first_calc_value, layout_node, reference_for_percent);

    for (auto& additional_value : calc_product->zero_or_more_additional_calc_values) {
        additional_value.value.visit(
            [&](CalculatedStyleValue::CalcValue const& calc_value) {
                if (additional_value.op != CalculatedStyleValue::CalcProductPartWithOperator::Multiply)
                    VERIFY_NOT_REACHED();
                auto resolved_value = resolve_calc_value(calc_value, layout_node, reference_for_percent);
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

static float resolve_calc_sum(NonnullOwnPtr<CalculatedStyleValue::CalcSum> const& calc_sum, const Layout::Node& layout_node, float reference_for_percent)
{
    auto value = resolve_calc_product(calc_sum->first_calc_product, layout_node, reference_for_percent);

    for (auto& additional_product : calc_sum->zero_or_more_additional_calc_products) {
        auto additional_value = resolve_calc_product(additional_product.calc_product, layout_node, reference_for_percent);
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
    case Type::Rem:
        return "rem";
    case Type::Auto:
        return "auto";
    case Type::Percentage:
        return "%";
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
