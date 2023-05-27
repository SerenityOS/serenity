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

class CalculationNode;

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
        void negate();
        void invert();

        Value const& value() const { return m_value; }

    private:
        void add_or_subtract_internal(SumOperation op, CalculationResult const& other, Layout::Node const*, PercentageBasis const& percentage_basis);
        Value m_value;
    };

    static ErrorOr<ValueComparingNonnullRefPtr<CalculatedStyleValue>> create(NonnullOwnPtr<CalculationNode> calculation, ResolvedType resolved_type)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) CalculatedStyleValue(move(calculation), resolved_type));
    }

    ErrorOr<String> to_string() const override;
    virtual bool equals(StyleValue const& other) const override;
    ResolvedType resolved_type() const { return m_resolved_type; }

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
    Optional<float> resolve_number() const;
    Optional<i64> resolve_integer();

    bool contains_percentage() const;

private:
    explicit CalculatedStyleValue(NonnullOwnPtr<CalculationNode> calculation, ResolvedType resolved_type)
        : StyleValue(Type::Calculated)
        , m_resolved_type(resolved_type)
        , m_calculation(move(calculation))
    {
    }

    ResolvedType m_resolved_type;
    NonnullOwnPtr<CalculationNode> m_calculation;
};

// https://www.w3.org/TR/css-values-4/#calculation-tree
class CalculationNode {
public:
    enum class Type {
        Numeric,
        // NOTE: Currently, any value with a `var()` or `attr()` function in it is always an
        //       UnresolvedStyleValue so we do not have to implement a NonMathFunction type here.

        // Operator nodes
        // https://www.w3.org/TR/css-values-4/#calculation-tree-operator-nodes

        // Calc-operator nodes, a sub-type of operator node
        // https://www.w3.org/TR/css-values-4/#calculation-tree-calc-operator-nodes
        Sum,
        Product,
        Negate,
        Invert,

        // This only exists during parsing.
        Unparsed,
    };
    using NumericValue = CalculatedStyleValue::CalculationResult::Value;

    virtual ~CalculationNode();

    Type type() const { return m_type; }

    bool is_operator_node() const
    {
        // FIXME: Check for operator node types once they exist
        return is_calc_operator_node();
    }

    bool is_calc_operator_node() const
    {
        return first_is_one_of(m_type, Type::Sum, Type::Product, Type::Negate, Type::Invert);
    }

    virtual ErrorOr<String> to_string() const = 0;
    virtual Optional<CalculatedStyleValue::ResolvedType> resolved_type() const = 0;
    virtual bool contains_percentage() const = 0;
    virtual CalculatedStyleValue::CalculationResult resolve(Layout::Node const*, CalculatedStyleValue::PercentageBasis const&) const = 0;
    virtual ErrorOr<void> for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const&) { return {}; }

    virtual ErrorOr<void> dump(StringBuilder&, int indent) const = 0;

protected:
    explicit CalculationNode(Type);

private:
    Type m_type;
};

class NumericCalculationNode final : public CalculationNode {
public:
    static ErrorOr<NonnullOwnPtr<NumericCalculationNode>> create(NumericValue);
    ~NumericCalculationNode();

    virtual ErrorOr<String> to_string() const override;
    virtual Optional<CalculatedStyleValue::ResolvedType> resolved_type() const override;
    virtual bool contains_percentage() const override;
    virtual CalculatedStyleValue::CalculationResult resolve(Layout::Node const*, CalculatedStyleValue::PercentageBasis const&) const override;

    virtual ErrorOr<void> dump(StringBuilder&, int indent) const override;

private:
    explicit NumericCalculationNode(NumericValue);
    NumericValue m_value;
};

class SumCalculationNode final : public CalculationNode {
public:
    static ErrorOr<NonnullOwnPtr<SumCalculationNode>> create(Vector<NonnullOwnPtr<CalculationNode>>);
    ~SumCalculationNode();

    virtual ErrorOr<String> to_string() const override;
    virtual Optional<CalculatedStyleValue::ResolvedType> resolved_type() const override;
    virtual bool contains_percentage() const override;
    virtual CalculatedStyleValue::CalculationResult resolve(Layout::Node const*, CalculatedStyleValue::PercentageBasis const&) const override;
    virtual ErrorOr<void> for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const&) override;

    virtual ErrorOr<void> dump(StringBuilder&, int indent) const override;

private:
    explicit SumCalculationNode(Vector<NonnullOwnPtr<CalculationNode>>);
    Vector<NonnullOwnPtr<CalculationNode>> m_values;
};

class ProductCalculationNode final : public CalculationNode {
public:
    static ErrorOr<NonnullOwnPtr<ProductCalculationNode>> create(Vector<NonnullOwnPtr<CalculationNode>>);
    ~ProductCalculationNode();

    virtual ErrorOr<String> to_string() const override;
    virtual Optional<CalculatedStyleValue::ResolvedType> resolved_type() const override;
    virtual bool contains_percentage() const override;
    virtual CalculatedStyleValue::CalculationResult resolve(Layout::Node const*, CalculatedStyleValue::PercentageBasis const&) const override;
    virtual ErrorOr<void> for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const&) override;

    virtual ErrorOr<void> dump(StringBuilder&, int indent) const override;

private:
    explicit ProductCalculationNode(Vector<NonnullOwnPtr<CalculationNode>>);
    Vector<NonnullOwnPtr<CalculationNode>> m_values;
};

class NegateCalculationNode final : public CalculationNode {
public:
    static ErrorOr<NonnullOwnPtr<NegateCalculationNode>> create(NonnullOwnPtr<CalculationNode>);
    ~NegateCalculationNode();

    virtual ErrorOr<String> to_string() const override;
    virtual Optional<CalculatedStyleValue::ResolvedType> resolved_type() const override;
    virtual bool contains_percentage() const override;
    virtual CalculatedStyleValue::CalculationResult resolve(Layout::Node const*, CalculatedStyleValue::PercentageBasis const&) const override;
    virtual ErrorOr<void> for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const&) override;

    virtual ErrorOr<void> dump(StringBuilder&, int indent) const override;

private:
    explicit NegateCalculationNode(NonnullOwnPtr<CalculationNode>);
    NonnullOwnPtr<CalculationNode> m_value;
};

class InvertCalculationNode final : public CalculationNode {
public:
    static ErrorOr<NonnullOwnPtr<InvertCalculationNode>> create(NonnullOwnPtr<CalculationNode>);
    ~InvertCalculationNode();

    virtual ErrorOr<String> to_string() const override;
    virtual Optional<CalculatedStyleValue::ResolvedType> resolved_type() const override;
    virtual bool contains_percentage() const override;
    virtual CalculatedStyleValue::CalculationResult resolve(Layout::Node const*, CalculatedStyleValue::PercentageBasis const&) const override;
    virtual ErrorOr<void> for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const&) override;

    virtual ErrorOr<void> dump(StringBuilder&, int indent) const override;

private:
    explicit InvertCalculationNode(NonnullOwnPtr<CalculationNode>);
    NonnullOwnPtr<CalculationNode> m_value;
};

}
