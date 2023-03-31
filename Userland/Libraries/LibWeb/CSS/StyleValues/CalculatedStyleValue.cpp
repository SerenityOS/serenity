/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CalculatedStyleValue.h"
#include <LibWeb/CSS/Percentage.h>

namespace Web::CSS {

void CalculatedStyleValue::CalculationResult::add(CalculationResult const& other, Layout::Node const* layout_node, PercentageBasis const& percentage_basis)
{
    add_or_subtract_internal(SumOperation::Add, other, layout_node, percentage_basis);
}

void CalculatedStyleValue::CalculationResult::subtract(CalculationResult const& other, Layout::Node const* layout_node, PercentageBasis const& percentage_basis)
{
    add_or_subtract_internal(SumOperation::Subtract, other, layout_node, percentage_basis);
}

void CalculatedStyleValue::CalculationResult::add_or_subtract_internal(SumOperation op, CalculationResult const& other, Layout::Node const* layout_node, PercentageBasis const& percentage_basis)
{
    // We know from validation when resolving the type, that "both sides have the same type, or that one side is a <number> and the other is an <integer>".
    // Though, having the same type may mean that one side is a <dimension> and the other a <percentage>.
    // Note: This is almost identical to ::add()

    m_value.visit(
        [&](Number const& number) {
            auto other_number = other.m_value.get<Number>();
            if (op == SumOperation::Add) {
                m_value = number + other_number;
            } else {
                m_value = number - other_number;
            }
        },
        [&](Angle const& angle) {
            auto this_degrees = angle.to_degrees();
            if (other.m_value.has<Angle>()) {
                auto other_degrees = other.m_value.get<Angle>().to_degrees();
                if (op == SumOperation::Add)
                    m_value = Angle::make_degrees(this_degrees + other_degrees);
                else
                    m_value = Angle::make_degrees(this_degrees - other_degrees);
            } else {
                VERIFY(percentage_basis.has<Angle>());

                auto other_degrees = percentage_basis.get<Angle>().percentage_of(other.m_value.get<Percentage>()).to_degrees();
                if (op == SumOperation::Add)
                    m_value = Angle::make_degrees(this_degrees + other_degrees);
                else
                    m_value = Angle::make_degrees(this_degrees - other_degrees);
            }
        },
        [&](Frequency const& frequency) {
            auto this_hertz = frequency.to_hertz();
            if (other.m_value.has<Frequency>()) {
                auto other_hertz = other.m_value.get<Frequency>().to_hertz();
                if (op == SumOperation::Add)
                    m_value = Frequency::make_hertz(this_hertz + other_hertz);
                else
                    m_value = Frequency::make_hertz(this_hertz - other_hertz);
            } else {
                VERIFY(percentage_basis.has<Frequency>());

                auto other_hertz = percentage_basis.get<Frequency>().percentage_of(other.m_value.get<Percentage>()).to_hertz();
                if (op == SumOperation::Add)
                    m_value = Frequency::make_hertz(this_hertz + other_hertz);
                else
                    m_value = Frequency::make_hertz(this_hertz - other_hertz);
            }
        },
        [&](Length const& length) {
            auto this_px = length.to_px(*layout_node);
            if (other.m_value.has<Length>()) {
                auto other_px = other.m_value.get<Length>().to_px(*layout_node);
                if (op == SumOperation::Add)
                    m_value = Length::make_px(this_px + other_px);
                else
                    m_value = Length::make_px(this_px - other_px);
            } else {
                VERIFY(percentage_basis.has<Length>());

                auto other_px = percentage_basis.get<Length>().percentage_of(other.m_value.get<Percentage>()).to_px(*layout_node);
                if (op == SumOperation::Add)
                    m_value = Length::make_px(this_px + other_px);
                else
                    m_value = Length::make_px(this_px - other_px);
            }
        },
        [&](Time const& time) {
            auto this_seconds = time.to_seconds();
            if (other.m_value.has<Time>()) {
                auto other_seconds = other.m_value.get<Time>().to_seconds();
                if (op == SumOperation::Add)
                    m_value = Time::make_seconds(this_seconds + other_seconds);
                else
                    m_value = Time::make_seconds(this_seconds - other_seconds);
            } else {
                VERIFY(percentage_basis.has<Time>());

                auto other_seconds = percentage_basis.get<Time>().percentage_of(other.m_value.get<Percentage>()).to_seconds();
                if (op == SumOperation::Add)
                    m_value = Time::make_seconds(this_seconds + other_seconds);
                else
                    m_value = Time::make_seconds(this_seconds - other_seconds);
            }
        },
        [&](Percentage const& percentage) {
            if (other.m_value.has<Percentage>()) {
                if (op == SumOperation::Add)
                    m_value = Percentage { percentage.value() + other.m_value.get<Percentage>().value() };
                else
                    m_value = Percentage { percentage.value() - other.m_value.get<Percentage>().value() };
                return;
            }

            // Other side isn't a percentage, so the easiest way to handle it without duplicating all the logic, is just to swap `this` and `other`.
            CalculationResult new_value = other;
            if (op == SumOperation::Add) {
                new_value.add(*this, layout_node, percentage_basis);
            } else {
                // Turn 'this - other' into '-other + this', as 'A + B == B + A', but 'A - B != B - A'
                new_value.multiply_by({ Number { Number::Type::Integer, -1.0f } }, layout_node);
                new_value.add(*this, layout_node, percentage_basis);
            }

            *this = new_value;
        });
}

void CalculatedStyleValue::CalculationResult::multiply_by(CalculationResult const& other, Layout::Node const* layout_node)
{
    // We know from validation when resolving the type, that at least one side must be a <number> or <integer>.
    // Both of these are represented as a float.
    VERIFY(m_value.has<Number>() || other.m_value.has<Number>());
    bool other_is_number = other.m_value.has<Number>();

    m_value.visit(
        [&](Number const& number) {
            if (other_is_number) {
                m_value = number * other.m_value.get<Number>();
            } else {
                // Avoid duplicating all the logic by swapping `this` and `other`.
                CalculationResult new_value = other;
                new_value.multiply_by(*this, layout_node);
                *this = new_value;
            }
        },
        [&](Angle const& angle) {
            m_value = Angle::make_degrees(angle.to_degrees() * other.m_value.get<Number>().value());
        },
        [&](Frequency const& frequency) {
            m_value = Frequency::make_hertz(frequency.to_hertz() * other.m_value.get<Number>().value());
        },
        [&](Length const& length) {
            VERIFY(layout_node);
            m_value = Length::make_px(length.to_px(*layout_node) * other.m_value.get<Number>().value());
        },
        [&](Time const& time) {
            m_value = Time::make_seconds(time.to_seconds() * other.m_value.get<Number>().value());
        },
        [&](Percentage const& percentage) {
            m_value = Percentage { percentage.value() * other.m_value.get<Number>().value() };
        });
}

void CalculatedStyleValue::CalculationResult::divide_by(CalculationResult const& other, Layout::Node const* layout_node)
{
    // We know from validation when resolving the type, that `other` must be a <number> or <integer>.
    // Both of these are represented as a Number.
    auto denominator = other.m_value.get<Number>().value();
    // FIXME: Dividing by 0 is invalid, and should be caught during parsing.
    VERIFY(denominator != 0.0f);

    m_value.visit(
        [&](Number const& number) {
            m_value = Number {
                Number::Type::Number,
                number.value() / denominator
            };
        },
        [&](Angle const& angle) {
            m_value = Angle::make_degrees(angle.to_degrees() / denominator);
        },
        [&](Frequency const& frequency) {
            m_value = Frequency::make_hertz(frequency.to_hertz() / denominator);
        },
        [&](Length const& length) {
            VERIFY(layout_node);
            m_value = Length::make_px(length.to_px(*layout_node) / denominator);
        },
        [&](Time const& time) {
            m_value = Time::make_seconds(time.to_seconds() / denominator);
        },
        [&](Percentage const& percentage) {
            m_value = Percentage { percentage.value() / denominator };
        });
}

ErrorOr<String> CalculatedStyleValue::to_string() const
{
    return String::formatted("calc({})", TRY(m_expression->to_string()));
}

bool CalculatedStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    // This is a case where comparing the strings actually makes sense.
    return to_string().release_value_but_fixme_should_propagate_errors() == other.to_string().release_value_but_fixme_should_propagate_errors();
}

ErrorOr<String> CalculatedStyleValue::CalcNumberValue::to_string() const
{
    return value.visit(
        [](Number const& number) -> ErrorOr<String> { return String::number(number.value()); },
        [](NonnullOwnPtr<CalcNumberSum> const& sum) -> ErrorOr<String> { return String::formatted("({})", TRY(sum->to_string())); });
}

ErrorOr<String> CalculatedStyleValue::CalcValue::to_string() const
{
    return value.visit(
        [](Number const& number) -> ErrorOr<String> { return String::number(number.value()); },
        [](NonnullOwnPtr<CalcSum> const& sum) -> ErrorOr<String> { return String::formatted("({})", TRY(sum->to_string())); },
        [](auto const& v) -> ErrorOr<String> { return v.to_string(); });
}

ErrorOr<String> CalculatedStyleValue::CalcSum::to_string() const
{
    StringBuilder builder;
    TRY(builder.try_append(TRY(first_calc_product->to_string())));
    for (auto const& item : zero_or_more_additional_calc_products)
        TRY(builder.try_append(TRY(item->to_string())));
    return builder.to_string();
}

ErrorOr<String> CalculatedStyleValue::CalcNumberSum::to_string() const
{
    StringBuilder builder;
    TRY(builder.try_append(TRY(first_calc_number_product->to_string())));
    for (auto const& item : zero_or_more_additional_calc_number_products)
        TRY(builder.try_append(TRY(item->to_string())));
    return builder.to_string();
}

ErrorOr<String> CalculatedStyleValue::CalcProduct::to_string() const
{
    StringBuilder builder;
    TRY(builder.try_append(TRY(first_calc_value.to_string())));
    for (auto const& item : zero_or_more_additional_calc_values)
        TRY(builder.try_append(TRY(item->to_string())));
    return builder.to_string();
}

ErrorOr<String> CalculatedStyleValue::CalcSumPartWithOperator::to_string() const
{
    return String::formatted(" {} {}", op == SumOperation::Add ? "+"sv : "-"sv, TRY(value->to_string()));
}

ErrorOr<String> CalculatedStyleValue::CalcProductPartWithOperator::to_string() const
{
    auto value_string = TRY(value.visit(
        [](CalcValue const& v) { return v.to_string(); },
        [](CalcNumberValue const& v) { return v.to_string(); }));
    return String::formatted(" {} {}", op == ProductOperation::Multiply ? "*"sv : "/"sv, value_string);
}

ErrorOr<String> CalculatedStyleValue::CalcNumberProduct::to_string() const
{
    StringBuilder builder;
    TRY(builder.try_append(TRY(first_calc_number_value.to_string())));
    for (auto const& item : zero_or_more_additional_calc_number_values)
        TRY(builder.try_append(TRY(item->to_string())));
    return builder.to_string();
}

ErrorOr<String> CalculatedStyleValue::CalcNumberProductPartWithOperator::to_string() const
{
    return String::formatted(" {} {}", op == ProductOperation::Multiply ? "*"sv : "/"sv, TRY(value.to_string()));
}

ErrorOr<String> CalculatedStyleValue::CalcNumberSumPartWithOperator::to_string() const
{
    return String::formatted(" {} {}", op == SumOperation::Add ? "+"sv : "-"sv, TRY(value->to_string()));
}

Optional<Angle> CalculatedStyleValue::resolve_angle() const
{
    auto result = m_expression->resolve(nullptr, {});

    if (result.value().has<Angle>())
        return result.value().get<Angle>();
    return {};
}

Optional<Angle> CalculatedStyleValue::resolve_angle_percentage(Angle const& percentage_basis) const
{
    auto result = m_expression->resolve(nullptr, percentage_basis);

    return result.value().visit(
        [&](Angle const& angle) -> Optional<Angle> {
            return angle;
        },
        [&](Percentage const& percentage) -> Optional<Angle> {
            return percentage_basis.percentage_of(percentage);
        },
        [&](auto const&) -> Optional<Angle> {
            return {};
        });
}

Optional<Frequency> CalculatedStyleValue::resolve_frequency() const
{
    auto result = m_expression->resolve(nullptr, {});

    if (result.value().has<Frequency>())
        return result.value().get<Frequency>();
    return {};
}

Optional<Frequency> CalculatedStyleValue::resolve_frequency_percentage(Frequency const& percentage_basis) const
{
    auto result = m_expression->resolve(nullptr, percentage_basis);

    return result.value().visit(
        [&](Frequency const& frequency) -> Optional<Frequency> {
            return frequency;
        },
        [&](Percentage const& percentage) -> Optional<Frequency> {
            return percentage_basis.percentage_of(percentage);
        },
        [&](auto const&) -> Optional<Frequency> {
            return {};
        });
}

Optional<Length> CalculatedStyleValue::resolve_length(Layout::Node const& layout_node) const
{
    auto result = m_expression->resolve(&layout_node, {});

    if (result.value().has<Length>())
        return result.value().get<Length>();
    return {};
}

Optional<Length> CalculatedStyleValue::resolve_length_percentage(Layout::Node const& layout_node, Length const& percentage_basis) const
{
    auto result = m_expression->resolve(&layout_node, percentage_basis);

    return result.value().visit(
        [&](Length const& length) -> Optional<Length> {
            return length;
        },
        [&](Percentage const& percentage) -> Optional<Length> {
            return percentage_basis.percentage_of(percentage);
        },
        [&](auto const&) -> Optional<Length> {
            return {};
        });
}

Optional<Percentage> CalculatedStyleValue::resolve_percentage() const
{
    auto result = m_expression->resolve(nullptr, {});
    if (result.value().has<Percentage>())
        return result.value().get<Percentage>();
    return {};
}

Optional<Time> CalculatedStyleValue::resolve_time() const
{
    auto result = m_expression->resolve(nullptr, {});

    if (result.value().has<Time>())
        return result.value().get<Time>();
    return {};
}

Optional<Time> CalculatedStyleValue::resolve_time_percentage(Time const& percentage_basis) const
{
    auto result = m_expression->resolve(nullptr, percentage_basis);

    return result.value().visit(
        [&](Time const& time) -> Optional<Time> {
            return time;
        },
        [&](auto const&) -> Optional<Time> {
            return {};
        });
}

Optional<float> CalculatedStyleValue::resolve_number()
{
    auto result = m_expression->resolve(nullptr, {});
    if (result.value().has<Number>())
        return result.value().get<Number>().value();
    return {};
}

Optional<i64> CalculatedStyleValue::resolve_integer()
{
    auto result = m_expression->resolve(nullptr, {});
    if (result.value().has<Number>())
        return result.value().get<Number>().integer_value();
    return {};
}

static bool is_number(CalculatedStyleValue::ResolvedType type)
{
    return type == CalculatedStyleValue::ResolvedType::Number || type == CalculatedStyleValue::ResolvedType::Integer;
}

static bool is_dimension(CalculatedStyleValue::ResolvedType type)
{
    return type != CalculatedStyleValue::ResolvedType::Number
        && type != CalculatedStyleValue::ResolvedType::Integer
        && type != CalculatedStyleValue::ResolvedType::Percentage;
}

template<typename SumWithOperator>
static Optional<CalculatedStyleValue::ResolvedType> resolve_sum_type(CalculatedStyleValue::ResolvedType first_type, Vector<NonnullOwnPtr<SumWithOperator>> const& zero_or_more_additional_products)
{
    auto type = first_type;

    for (auto const& product : zero_or_more_additional_products) {
        auto maybe_product_type = product->resolved_type();
        if (!maybe_product_type.has_value())
            return {};
        auto product_type = maybe_product_type.value();

        // At + or -, check that both sides have the same type, or that one side is a <number> and the other is an <integer>.
        // If both sides are the same type, resolve to that type.
        if (product_type == type)
            continue;

        // If one side is a <number> and the other is an <integer>, resolve to <number>.
        if (is_number(type) && is_number(product_type)) {
            type = CalculatedStyleValue::ResolvedType::Number;
            continue;
        }

        // FIXME: calc() handles <percentage> by allowing them to pretend to be whatever <dimension> type is allowed at this location.
        //        Since we can't easily check what that type is, we just allow <percentage> to combine with any other <dimension> type.
        if (type == CalculatedStyleValue::ResolvedType::Percentage && is_dimension(product_type)) {
            type = product_type;
            continue;
        }
        if (is_dimension(type) && product_type == CalculatedStyleValue::ResolvedType::Percentage)
            continue;

        return {};
    }
    return type;
}

Optional<CalculatedStyleValue::ResolvedType> CalculatedStyleValue::CalcSum::resolved_type() const
{
    auto maybe_type = first_calc_product->resolved_type();
    if (!maybe_type.has_value())
        return {};
    auto type = maybe_type.value();
    return resolve_sum_type(type, zero_or_more_additional_calc_products);
}

Optional<CalculatedStyleValue::ResolvedType> CalculatedStyleValue::CalcNumberSum::resolved_type() const
{
    auto maybe_type = first_calc_number_product->resolved_type();
    if (!maybe_type.has_value())
        return {};
    auto type = maybe_type.value();
    return resolve_sum_type(type, zero_or_more_additional_calc_number_products);
}

template<typename ProductWithOperator>
static Optional<CalculatedStyleValue::ResolvedType> resolve_product_type(CalculatedStyleValue::ResolvedType first_type, Vector<NonnullOwnPtr<ProductWithOperator>> const& zero_or_more_additional_values)
{
    auto type = first_type;

    for (auto const& value : zero_or_more_additional_values) {
        auto maybe_value_type = value->resolved_type();
        if (!maybe_value_type.has_value())
            return {};
        auto value_type = maybe_value_type.value();

        if (value->op == CalculatedStyleValue::ProductOperation::Multiply) {
            // At *, check that at least one side is <number>.
            if (!(is_number(type) || is_number(value_type)))
                return {};
            // If both sides are <integer>, resolve to <integer>.
            if (type == CalculatedStyleValue::ResolvedType::Integer && value_type == CalculatedStyleValue::ResolvedType::Integer) {
                type = CalculatedStyleValue::ResolvedType::Integer;
            } else {
                // Otherwise, resolve to the type of the other side.
                if (is_number(type))
                    type = value_type;
            }

            continue;
        } else {
            VERIFY(value->op == CalculatedStyleValue::ProductOperation::Divide);
            // At /, check that the right side is <number>.
            if (!is_number(value_type))
                return {};
            // If the left side is <integer>, resolve to <number>.
            if (type == CalculatedStyleValue::ResolvedType::Integer) {
                type = CalculatedStyleValue::ResolvedType::Number;
            } else {
                // Otherwise, resolve to the type of the left side.
            }

            // FIXME: Division by zero makes the whole calc() expression invalid.
        }
    }
    return type;
}

Optional<CalculatedStyleValue::ResolvedType> CalculatedStyleValue::CalcProduct::resolved_type() const
{
    auto maybe_type = first_calc_value.resolved_type();
    if (!maybe_type.has_value())
        return {};
    auto type = maybe_type.value();
    return resolve_product_type(type, zero_or_more_additional_calc_values);
}

Optional<CalculatedStyleValue::ResolvedType> CalculatedStyleValue::CalcSumPartWithOperator::resolved_type() const
{
    return value->resolved_type();
}

Optional<CalculatedStyleValue::ResolvedType> CalculatedStyleValue::CalcNumberProduct::resolved_type() const
{
    auto maybe_type = first_calc_number_value.resolved_type();
    if (!maybe_type.has_value())
        return {};
    auto type = maybe_type.value();
    return resolve_product_type(type, zero_or_more_additional_calc_number_values);
}

Optional<CalculatedStyleValue::ResolvedType> CalculatedStyleValue::CalcNumberProductPartWithOperator::resolved_type() const
{
    return value.resolved_type();
}

Optional<CalculatedStyleValue::ResolvedType> CalculatedStyleValue::CalcNumberSumPartWithOperator::resolved_type() const
{
    return value->resolved_type();
}

Optional<CalculatedStyleValue::ResolvedType> CalculatedStyleValue::CalcProductPartWithOperator::resolved_type() const
{
    return value.visit(
        [](CalcValue const& calc_value) {
            return calc_value.resolved_type();
        },
        [](CalcNumberValue const& calc_number_value) {
            return calc_number_value.resolved_type();
        });
}

Optional<CalculatedStyleValue::ResolvedType> CalculatedStyleValue::CalcValue::resolved_type() const
{
    return value.visit(
        [](Number const& number) -> Optional<CalculatedStyleValue::ResolvedType> {
            return { number.is_integer() ? ResolvedType::Integer : ResolvedType::Number };
        },
        [](Angle const&) -> Optional<CalculatedStyleValue::ResolvedType> { return { ResolvedType::Angle }; },
        [](Frequency const&) -> Optional<CalculatedStyleValue::ResolvedType> { return { ResolvedType::Frequency }; },
        [](Length const&) -> Optional<CalculatedStyleValue::ResolvedType> { return { ResolvedType::Length }; },
        [](Percentage const&) -> Optional<CalculatedStyleValue::ResolvedType> { return { ResolvedType::Percentage }; },
        [](Time const&) -> Optional<CalculatedStyleValue::ResolvedType> { return { ResolvedType::Time }; },
        [](NonnullOwnPtr<CalcSum> const& sum) { return sum->resolved_type(); });
}

Optional<CalculatedStyleValue::ResolvedType> CalculatedStyleValue::CalcNumberValue::resolved_type() const
{
    return value.visit(
        [](Number const& number) -> Optional<CalculatedStyleValue::ResolvedType> {
            return { number.is_integer() ? ResolvedType::Integer : ResolvedType::Number };
        },
        [](NonnullOwnPtr<CalcNumberSum> const& sum) { return sum->resolved_type(); });
}

CalculatedStyleValue::CalculationResult CalculatedStyleValue::CalcNumberValue::resolve(Layout::Node const* layout_node, PercentageBasis const& percentage_basis) const
{
    return value.visit(
        [&](Number const& number) -> CalculatedStyleValue::CalculationResult {
            return CalculatedStyleValue::CalculationResult { number };
        },
        [&](NonnullOwnPtr<CalcNumberSum> const& sum) -> CalculatedStyleValue::CalculationResult {
            return sum->resolve(layout_node, percentage_basis);
        });
}

CalculatedStyleValue::CalculationResult CalculatedStyleValue::CalcValue::resolve(Layout::Node const* layout_node, PercentageBasis const& percentage_basis) const
{
    return value.visit(
        [&](NonnullOwnPtr<CalcSum> const& sum) -> CalculatedStyleValue::CalculationResult {
            return sum->resolve(layout_node, percentage_basis);
        },
        [&](auto const& v) -> CalculatedStyleValue::CalculationResult {
            return CalculatedStyleValue::CalculationResult { v };
        });
}

CalculatedStyleValue::CalculationResult CalculatedStyleValue::CalcSum::resolve(Layout::Node const* layout_node, PercentageBasis const& percentage_basis) const
{
    auto value = first_calc_product->resolve(layout_node, percentage_basis);

    for (auto& additional_product : zero_or_more_additional_calc_products) {
        auto additional_value = additional_product->resolve(layout_node, percentage_basis);

        if (additional_product->op == CalculatedStyleValue::SumOperation::Add)
            value.add(additional_value, layout_node, percentage_basis);
        else if (additional_product->op == CalculatedStyleValue::SumOperation::Subtract)
            value.subtract(additional_value, layout_node, percentage_basis);
        else
            VERIFY_NOT_REACHED();
    }

    return value;
}

CalculatedStyleValue::CalculationResult CalculatedStyleValue::CalcNumberSum::resolve(Layout::Node const* layout_node, PercentageBasis const& percentage_basis) const
{
    auto value = first_calc_number_product->resolve(layout_node, percentage_basis);

    for (auto& additional_product : zero_or_more_additional_calc_number_products) {
        auto additional_value = additional_product->resolve(layout_node, percentage_basis);

        if (additional_product->op == CSS::CalculatedStyleValue::SumOperation::Add)
            value.add(additional_value, layout_node, percentage_basis);
        else if (additional_product->op == CalculatedStyleValue::SumOperation::Subtract)
            value.subtract(additional_value, layout_node, percentage_basis);
        else
            VERIFY_NOT_REACHED();
    }

    return value;
}

CalculatedStyleValue::CalculationResult CalculatedStyleValue::CalcProduct::resolve(Layout::Node const* layout_node, PercentageBasis const& percentage_basis) const
{
    auto value = first_calc_value.resolve(layout_node, percentage_basis);

    for (auto& additional_value : zero_or_more_additional_calc_values) {
        additional_value->value.visit(
            [&](CalculatedStyleValue::CalcValue const& calc_value) {
                VERIFY(additional_value->op == CalculatedStyleValue::ProductOperation::Multiply);
                auto resolved_value = calc_value.resolve(layout_node, percentage_basis);
                value.multiply_by(resolved_value, layout_node);
            },
            [&](CalculatedStyleValue::CalcNumberValue const& calc_number_value) {
                VERIFY(additional_value->op == CalculatedStyleValue::ProductOperation::Divide);
                auto resolved_calc_number_value = calc_number_value.resolve(layout_node, percentage_basis);
                // FIXME: Checking for division by 0 should happen during parsing.
                VERIFY(resolved_calc_number_value.value().get<Number>().value() != 0.0f);
                value.divide_by(resolved_calc_number_value, layout_node);
            });
    }

    return value;
}

CalculatedStyleValue::CalculationResult CalculatedStyleValue::CalcNumberProduct::resolve(Layout::Node const* layout_node, PercentageBasis const& percentage_basis) const
{
    auto value = first_calc_number_value.resolve(layout_node, percentage_basis);

    for (auto& additional_number_value : zero_or_more_additional_calc_number_values) {
        auto additional_value = additional_number_value->resolve(layout_node, percentage_basis);

        if (additional_number_value->op == CalculatedStyleValue::ProductOperation::Multiply)
            value.multiply_by(additional_value, layout_node);
        else if (additional_number_value->op == CalculatedStyleValue::ProductOperation::Divide)
            value.divide_by(additional_value, layout_node);
        else
            VERIFY_NOT_REACHED();
    }

    return value;
}

CalculatedStyleValue::CalculationResult CalculatedStyleValue::CalcProductPartWithOperator::resolve(Layout::Node const* layout_node, PercentageBasis const& percentage_basis) const
{
    return value.visit(
        [&](CalcValue const& calc_value) {
            return calc_value.resolve(layout_node, percentage_basis);
        },
        [&](CalcNumberValue const& calc_number_value) {
            return calc_number_value.resolve(layout_node, percentage_basis);
        });
}

CalculatedStyleValue::CalculationResult CalculatedStyleValue::CalcSumPartWithOperator::resolve(Layout::Node const* layout_node, PercentageBasis const& percentage_basis) const
{
    return value->resolve(layout_node, percentage_basis);
}

CalculatedStyleValue::CalculationResult CalculatedStyleValue::CalcNumberProductPartWithOperator::resolve(Layout::Node const* layout_node, PercentageBasis const& percentage_basis) const
{
    return value.resolve(layout_node, percentage_basis);
}

CalculatedStyleValue::CalculationResult CalculatedStyleValue::CalcNumberSumPartWithOperator::resolve(Layout::Node const* layout_node, PercentageBasis const& percentage_basis) const
{
    return value->resolve(layout_node, percentage_basis);
}

bool CalculatedStyleValue::contains_percentage() const
{
    return m_expression->contains_percentage();
}

bool CalculatedStyleValue::CalcSum::contains_percentage() const
{
    if (first_calc_product->contains_percentage())
        return true;
    for (auto& part : zero_or_more_additional_calc_products) {
        if (part->contains_percentage())
            return true;
    }
    return false;
}

bool CalculatedStyleValue::CalcSumPartWithOperator::contains_percentage() const
{
    return value->contains_percentage();
}

bool CalculatedStyleValue::CalcProduct::contains_percentage() const
{
    if (first_calc_value.contains_percentage())
        return true;
    for (auto& part : zero_or_more_additional_calc_values) {
        if (part->contains_percentage())
            return true;
    }
    return false;
}

bool CalculatedStyleValue::CalcProductPartWithOperator::contains_percentage() const
{
    return value.visit(
        [](CalcValue const& value) { return value.contains_percentage(); },
        [](CalcNumberValue const&) { return false; });
}

bool CalculatedStyleValue::CalcValue::contains_percentage() const
{
    return value.visit(
        [](Percentage const&) { return true; },
        [](NonnullOwnPtr<CalcSum> const& sum) { return sum->contains_percentage(); },
        [](auto const&) { return false; });
}

}
