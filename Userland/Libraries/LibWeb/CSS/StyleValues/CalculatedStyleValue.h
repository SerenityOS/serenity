/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/Angle.h>
#include <LibWeb/CSS/Frequency.h>
#include <LibWeb/CSS/Length.h>
#include <LibWeb/CSS/Percentage.h>
#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/CSS/Time.h>

namespace Web::CSS {

class CalculatedStyleValue : public StyleValue {
public:
    enum class ResolvedType {
        Angle,
        Frequency,
        Integer,
        Length,
        Number,
        Percentage,
        Time,
    };

    enum class SumOperation {
        Add,
        Subtract,
    };
    enum class ProductOperation {
        Multiply,
        Divide,
    };

    using PercentageBasis = Variant<Empty, Angle, Frequency, Length, Time>;

    class CalculationResult {
    public:
        using Value = Variant<Number, Angle, Frequency, Length, Percentage, Time>;
        CalculationResult(Value value)
            : m_value(move(value))
        {
        }
        void add(CalculationResult const& other, Layout::Node const*, PercentageBasis const& percentage_basis);
        void subtract(CalculationResult const& other, Layout::Node const*, PercentageBasis const& percentage_basis);
        void multiply_by(CalculationResult const& other, Layout::Node const*);
        void divide_by(CalculationResult const& other, Layout::Node const*);

        Value const& value() const { return m_value; }

    private:
        void add_or_subtract_internal(SumOperation op, CalculationResult const& other, Layout::Node const*, PercentageBasis const& percentage_basis);
        Value m_value;
    };

    struct CalcSum;
    struct CalcSumPartWithOperator;
    struct CalcProduct;
    struct CalcProductPartWithOperator;
    struct CalcNumberSum;
    struct CalcNumberSumPartWithOperator;
    struct CalcNumberProduct;
    struct CalcNumberProductPartWithOperator;

    struct CalcNumberValue {
        Variant<Number, NonnullOwnPtr<CalcNumberSum>> value;
        ErrorOr<String> to_string() const;
        Optional<ResolvedType> resolved_type() const;
        CalculationResult resolve(Layout::Node const*, PercentageBasis const& percentage_basis) const;
    };

    struct CalcValue {
        Variant<Number, Angle, Frequency, Length, Percentage, Time, NonnullOwnPtr<CalcSum>> value;
        ErrorOr<String> to_string() const;
        Optional<ResolvedType> resolved_type() const;
        CalculationResult resolve(Layout::Node const*, PercentageBasis const& percentage_basis) const;
        bool contains_percentage() const;
    };

    // This represents that: https://www.w3.org/TR/css-values-3/#calc-syntax
    struct CalcSum {
        CalcSum(NonnullOwnPtr<CalcProduct> first_calc_product, Vector<NonnullOwnPtr<CalcSumPartWithOperator>> additional)
            : first_calc_product(move(first_calc_product))
            , zero_or_more_additional_calc_products(move(additional)) {};

        NonnullOwnPtr<CalcProduct> first_calc_product;
        Vector<NonnullOwnPtr<CalcSumPartWithOperator>> zero_or_more_additional_calc_products;

        ErrorOr<String> to_string() const;
        Optional<ResolvedType> resolved_type() const;
        CalculationResult resolve(Layout::Node const*, PercentageBasis const& percentage_basis) const;

        bool contains_percentage() const;
    };

    struct CalcNumberSum {
        CalcNumberSum(NonnullOwnPtr<CalcNumberProduct> first_calc_number_product, Vector<NonnullOwnPtr<CalcNumberSumPartWithOperator>> additional)
            : first_calc_number_product(move(first_calc_number_product))
            , zero_or_more_additional_calc_number_products(move(additional)) {};

        NonnullOwnPtr<CalcNumberProduct> first_calc_number_product;
        Vector<NonnullOwnPtr<CalcNumberSumPartWithOperator>> zero_or_more_additional_calc_number_products;

        ErrorOr<String> to_string() const;
        Optional<ResolvedType> resolved_type() const;
        CalculationResult resolve(Layout::Node const*, PercentageBasis const& percentage_basis) const;
    };

    struct CalcProduct {
        CalcValue first_calc_value;
        Vector<NonnullOwnPtr<CalcProductPartWithOperator>> zero_or_more_additional_calc_values;

        ErrorOr<String> to_string() const;
        Optional<ResolvedType> resolved_type() const;
        CalculationResult resolve(Layout::Node const*, PercentageBasis const& percentage_basis) const;
        bool contains_percentage() const;
    };

    struct CalcSumPartWithOperator {
        CalcSumPartWithOperator(SumOperation op, NonnullOwnPtr<CalcProduct> calc_product)
            : op(op)
            , value(move(calc_product)) {};

        SumOperation op;
        NonnullOwnPtr<CalcProduct> value;

        ErrorOr<String> to_string() const;
        Optional<ResolvedType> resolved_type() const;
        CalculationResult resolve(Layout::Node const*, PercentageBasis const& percentage_basis) const;
        bool contains_percentage() const;
    };

    struct CalcProductPartWithOperator {
        ProductOperation op;
        Variant<CalcValue, CalcNumberValue> value;

        ErrorOr<String> to_string() const;
        Optional<ResolvedType> resolved_type() const;
        CalculationResult resolve(Layout::Node const*, PercentageBasis const& percentage_basis) const;

        bool contains_percentage() const;
    };

    struct CalcNumberProduct {
        CalcNumberValue first_calc_number_value;
        Vector<NonnullOwnPtr<CalcNumberProductPartWithOperator>> zero_or_more_additional_calc_number_values;

        ErrorOr<String> to_string() const;
        Optional<ResolvedType> resolved_type() const;
        CalculationResult resolve(Layout::Node const*, PercentageBasis const& percentage_basis) const;
    };

    struct CalcNumberProductPartWithOperator {
        ProductOperation op;
        CalcNumberValue value;

        ErrorOr<String> to_string() const;
        Optional<ResolvedType> resolved_type() const;
        CalculationResult resolve(Layout::Node const*, PercentageBasis const& percentage_basis) const;
    };

    struct CalcNumberSumPartWithOperator {
        CalcNumberSumPartWithOperator(SumOperation op, NonnullOwnPtr<CalcNumberProduct> calc_number_product)
            : op(op)
            , value(move(calc_number_product)) {};

        SumOperation op;
        NonnullOwnPtr<CalcNumberProduct> value;

        ErrorOr<String> to_string() const;
        Optional<ResolvedType> resolved_type() const;
        CalculationResult resolve(Layout::Node const*, PercentageBasis const& percentage_basis) const;
    };

    static ValueComparingNonnullRefPtr<CalculatedStyleValue> create(NonnullOwnPtr<CalcSum> calc_sum, ResolvedType resolved_type)
    {
        return adopt_ref(*new CalculatedStyleValue(move(calc_sum), resolved_type));
    }

    ErrorOr<String> to_string() const override;
    virtual bool equals(StyleValue const& other) const override;
    ResolvedType resolved_type() const { return m_resolved_type; }
    NonnullOwnPtr<CalcSum> const& expression() const { return m_expression; }

    bool resolves_to_angle() const { return m_resolved_type == ResolvedType::Angle; }
    Optional<Angle> resolve_angle() const;
    Optional<Angle> resolve_angle_percentage(Angle const& percentage_basis) const;

    bool resolves_to_frequency() const { return m_resolved_type == ResolvedType::Frequency; }
    Optional<Frequency> resolve_frequency() const;
    Optional<Frequency> resolve_frequency_percentage(Frequency const& percentage_basis) const;

    bool resolves_to_length() const { return m_resolved_type == ResolvedType::Length; }
    Optional<Length> resolve_length(Layout::Node const& layout_node) const;
    Optional<Length> resolve_length_percentage(Layout::Node const&, Length const& percentage_basis) const;

    bool resolves_to_percentage() const { return m_resolved_type == ResolvedType::Percentage; }
    Optional<Percentage> resolve_percentage() const;

    bool resolves_to_time() const { return m_resolved_type == ResolvedType::Time; }
    Optional<Time> resolve_time() const;
    Optional<Time> resolve_time_percentage(Time const& percentage_basis) const;

    bool resolves_to_integer() const { return m_resolved_type == ResolvedType::Integer; }
    bool resolves_to_number() const { return resolves_to_integer() || m_resolved_type == ResolvedType::Number; }
    Optional<float> resolve_number();
    Optional<i64> resolve_integer();

    bool contains_percentage() const;

private:
    explicit CalculatedStyleValue(NonnullOwnPtr<CalcSum> calc_sum, ResolvedType resolved_type)
        : StyleValue(Type::Calculated)
        , m_resolved_type(resolved_type)
        , m_expression(move(calc_sum))
    {
    }

    ResolvedType m_resolved_type;
    NonnullOwnPtr<CalcSum> m_expression;
};

}
