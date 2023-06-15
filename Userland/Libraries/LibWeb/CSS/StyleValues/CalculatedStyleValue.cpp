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

static double resolve_value_radians(CalculatedStyleValue::CalculationResult::Value value)
{
    return value.visit(
        [](Number const& number) { return number.value(); },
        [](Angle const& angle) { return angle.to_radians(); },
        [](auto const&) { VERIFY_NOT_REACHED(); return 0.0; });
};

static double resolve_value(CalculatedStyleValue::CalculationResult::Value value, Optional<Length::ResolutionContext const&> context)
{
    return value.visit(
        [](Number const& number) { return number.value(); },
        [](Angle const& angle) { return angle.to_degrees(); },
        [](Frequency const& frequency) { return frequency.to_hertz(); },
        [&context](Length const& length) { return length.to_px(*context).to_double(); },
        [](Percentage const& percentage) { return percentage.value(); },
        [](Time const& time) { return time.to_seconds(); });
};

static CalculatedStyleValue::CalculationResult to_resolved_type(CalculatedStyleValue::ResolvedType type, double value)
{
    switch (type) {
    case CalculatedStyleValue::ResolvedType::Integer:
        return { Number(Number::Type::Integer, value) };
    case CalculatedStyleValue::ResolvedType::Number:
        return { Number(Number::Type::Number, value) };
    case CalculatedStyleValue::ResolvedType::Angle:
        return { Angle::make_degrees(value) };
    case CalculatedStyleValue::ResolvedType::Frequency:
        return { Frequency::make_hertz(value) };
    case CalculatedStyleValue::ResolvedType::Length:
        return { Length::make_px(value) };
    case CalculatedStyleValue::ResolvedType::Percentage:
        return { Percentage(value) };
    case CalculatedStyleValue::ResolvedType::Time:
        return { Time::make_seconds(value) };
    }

    VERIFY_NOT_REACHED();
};

CalculationNode::CalculationNode(Type type)
    : m_type(type)
{
}

CalculationNode::~CalculationNode() = default;

ErrorOr<NonnullOwnPtr<NumericCalculationNode>> NumericCalculationNode::create(NumericValue value)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) NumericCalculationNode(move(value)));
}

NumericCalculationNode::NumericCalculationNode(NumericValue value)
    : CalculationNode(Type::Numeric)
    , m_value(move(value))
{
}

NumericCalculationNode::~NumericCalculationNode() = default;

ErrorOr<String> NumericCalculationNode::to_string() const
{
    return m_value.visit([](auto& value) { return value.to_string(); });
}

Optional<CalculatedStyleValue::ResolvedType> NumericCalculationNode::resolved_type() const
{
    return m_value.visit(
        [](Number const&) { return CalculatedStyleValue::ResolvedType::Number; },
        [](Angle const&) { return CalculatedStyleValue::ResolvedType::Angle; },
        [](Frequency const&) { return CalculatedStyleValue::ResolvedType::Frequency; },
        [](Length const&) { return CalculatedStyleValue::ResolvedType::Length; },
        [](Percentage const&) { return CalculatedStyleValue::ResolvedType::Percentage; },
        [](Time const&) { return CalculatedStyleValue::ResolvedType::Time; });
}

bool NumericCalculationNode::contains_percentage() const
{
    return m_value.has<Percentage>();
}

CalculatedStyleValue::CalculationResult NumericCalculationNode::resolve(Optional<Length::ResolutionContext const&>, CalculatedStyleValue::PercentageBasis const&) const
{
    return m_value;
}

ErrorOr<void> NumericCalculationNode::dump(StringBuilder& builder, int indent) const
{
    return builder.try_appendff("{: >{}}NUMERIC({})\n", "", indent, TRY(m_value.visit([](auto& it) { return it.to_string(); })));
}

ErrorOr<NonnullOwnPtr<SumCalculationNode>> SumCalculationNode::create(Vector<NonnullOwnPtr<CalculationNode>> values)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) SumCalculationNode(move(values)));
}

SumCalculationNode::SumCalculationNode(Vector<NonnullOwnPtr<CalculationNode>> values)
    : CalculationNode(Type::Sum)
    , m_values(move(values))
{
    VERIFY(!m_values.is_empty());
}

SumCalculationNode::~SumCalculationNode() = default;

ErrorOr<String> SumCalculationNode::to_string() const
{
    bool first = true;
    StringBuilder builder;
    for (auto& value : m_values) {
        if (!first)
            TRY(builder.try_append(" + "sv));
        TRY(builder.try_append(TRY(value->to_string())));
        first = false;
    }
    return builder.to_string();
}

Optional<CalculatedStyleValue::ResolvedType> SumCalculationNode::resolved_type() const
{
    // FIXME: Implement https://www.w3.org/TR/css-values-4/#determine-the-type-of-a-calculation
    //        For now, this is just ad-hoc, based on the old implementation.

    Optional<CalculatedStyleValue::ResolvedType> type;
    for (auto const& value : m_values) {
        auto maybe_value_type = value->resolved_type();
        if (!maybe_value_type.has_value())
            return {};
        auto value_type = maybe_value_type.value();

        if (!type.has_value()) {
            type = value_type;
            continue;
        }

        // At + or -, check that both sides have the same type, or that one side is a <number> and the other is an <integer>.
        // If both sides are the same type, resolve to that type.
        if (value_type == type)
            continue;

        // If one side is a <number> and the other is an <integer>, resolve to <number>.
        if (is_number(*type) && is_number(value_type)) {
            type = CalculatedStyleValue::ResolvedType::Number;
            continue;
        }

        // FIXME: calc() handles <percentage> by allowing them to pretend to be whatever <dimension> type is allowed at this location.
        //        Since we can't easily check what that type is, we just allow <percentage> to combine with any other <dimension> type.
        if (type == CalculatedStyleValue::ResolvedType::Percentage && is_dimension(value_type)) {
            type = value_type;
            continue;
        }
        if (is_dimension(*type) && value_type == CalculatedStyleValue::ResolvedType::Percentage)
            continue;

        return {};
    }
    return type;
}

bool SumCalculationNode::contains_percentage() const
{
    for (auto const& value : m_values) {
        if (value->contains_percentage())
            return true;
    }
    return false;
}

CalculatedStyleValue::CalculationResult SumCalculationNode::resolve(Optional<Length::ResolutionContext const&> context, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    Optional<CalculatedStyleValue::CalculationResult> total;

    for (auto& additional_product : m_values) {
        auto additional_value = additional_product->resolve(context, percentage_basis);
        if (!total.has_value()) {
            total = additional_value;
            continue;
        }
        total->add(additional_value, context, percentage_basis);
    }

    return total.value();
}

ErrorOr<void> SumCalculationNode::for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const& callback)
{
    for (auto& item : m_values) {
        TRY(item->for_each_child_node(callback));
        TRY(callback(item));
    }

    return {};
}

ErrorOr<void> SumCalculationNode::dump(StringBuilder& builder, int indent) const
{
    TRY(builder.try_appendff("{: >{}}SUM:\n", "", indent));
    for (auto const& item : m_values)
        TRY(item->dump(builder, indent + 2));
    return {};
}

ErrorOr<NonnullOwnPtr<ProductCalculationNode>> ProductCalculationNode::create(Vector<NonnullOwnPtr<CalculationNode>> values)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) ProductCalculationNode(move(values)));
}

ProductCalculationNode::ProductCalculationNode(Vector<NonnullOwnPtr<CalculationNode>> values)
    : CalculationNode(Type::Product)
    , m_values(move(values))
{
    VERIFY(!m_values.is_empty());
}

ProductCalculationNode::~ProductCalculationNode() = default;

ErrorOr<String> ProductCalculationNode::to_string() const
{
    bool first = true;
    StringBuilder builder;
    for (auto& value : m_values) {
        if (!first)
            TRY(builder.try_append(" * "sv));
        TRY(builder.try_append(TRY(value->to_string())));
        first = false;
    }
    return builder.to_string();
}

Optional<CalculatedStyleValue::ResolvedType> ProductCalculationNode::resolved_type() const
{
    // FIXME: Implement https://www.w3.org/TR/css-values-4/#determine-the-type-of-a-calculation
    //        For now, this is just ad-hoc, based on the old implementation.

    Optional<CalculatedStyleValue::ResolvedType> type;
    for (auto const& value : m_values) {
        auto maybe_value_type = value->resolved_type();
        if (!maybe_value_type.has_value())
            return {};
        auto value_type = maybe_value_type.value();

        if (!type.has_value()) {
            type = value_type;
            continue;
        }

        // At *, check that at least one side is <number>.
        if (!(is_number(*type) || is_number(value_type)))
            return {};
        // If both sides are <integer>, resolve to <integer>.
        if (type == CalculatedStyleValue::ResolvedType::Integer && value_type == CalculatedStyleValue::ResolvedType::Integer) {
            type = CalculatedStyleValue::ResolvedType::Integer;
        } else {
            // Otherwise, resolve to the type of the other side.
            if (is_number(*type))
                type = value_type;
        }
    }
    return type;
}

bool ProductCalculationNode::contains_percentage() const
{
    for (auto const& value : m_values) {
        if (value->contains_percentage())
            return true;
    }
    return false;
}

CalculatedStyleValue::CalculationResult ProductCalculationNode::resolve(Optional<Length::ResolutionContext const&> context, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    Optional<CalculatedStyleValue::CalculationResult> total;

    for (auto& additional_product : m_values) {
        auto additional_value = additional_product->resolve(context, percentage_basis);
        if (!total.has_value()) {
            total = additional_value;
            continue;
        }
        total->multiply_by(additional_value, context);
    }

    return total.value();
}

ErrorOr<void> ProductCalculationNode::for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const& callback)
{
    for (auto& item : m_values) {
        TRY(item->for_each_child_node(callback));
        TRY(callback(item));
    }

    return {};
}

ErrorOr<void> ProductCalculationNode::dump(StringBuilder& builder, int indent) const
{
    TRY(builder.try_appendff("{: >{}}PRODUCT:\n", "", indent));
    for (auto const& item : m_values)
        TRY(item->dump(builder, indent + 2));
    return {};
}

ErrorOr<NonnullOwnPtr<NegateCalculationNode>> NegateCalculationNode::create(NonnullOwnPtr<Web::CSS::CalculationNode> value)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) NegateCalculationNode(move(value)));
}

NegateCalculationNode::NegateCalculationNode(NonnullOwnPtr<CalculationNode> value)
    : CalculationNode(Type::Negate)
    , m_value(move(value))
{
}

NegateCalculationNode::~NegateCalculationNode() = default;

ErrorOr<String> NegateCalculationNode::to_string() const
{
    return String::formatted("(0 - {})", TRY(m_value->to_string()));
}

Optional<CalculatedStyleValue::ResolvedType> NegateCalculationNode::resolved_type() const
{
    return m_value->resolved_type();
}

bool NegateCalculationNode::contains_percentage() const
{
    return m_value->contains_percentage();
}

CalculatedStyleValue::CalculationResult NegateCalculationNode::resolve(Optional<Length::ResolutionContext const&> context, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    auto child_value = m_value->resolve(context, percentage_basis);
    child_value.negate();
    return child_value;
}

ErrorOr<void> NegateCalculationNode::for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const& callback)
{
    TRY(m_value->for_each_child_node(callback));
    TRY(callback(m_value));
    return {};
}

ErrorOr<void> NegateCalculationNode::dump(StringBuilder& builder, int indent) const
{
    TRY(builder.try_appendff("{: >{}}NEGATE:\n", "", indent));
    TRY(m_value->dump(builder, indent + 2));
    return {};
}

ErrorOr<NonnullOwnPtr<InvertCalculationNode>> InvertCalculationNode::create(NonnullOwnPtr<Web::CSS::CalculationNode> value)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) InvertCalculationNode(move(value)));
}

InvertCalculationNode::InvertCalculationNode(NonnullOwnPtr<CalculationNode> value)
    : CalculationNode(Type::Invert)
    , m_value(move(value))
{
}

InvertCalculationNode::~InvertCalculationNode() = default;

ErrorOr<String> InvertCalculationNode::to_string() const
{
    return String::formatted("(1 / {})", TRY(m_value->to_string()));
}

Optional<CalculatedStyleValue::ResolvedType> InvertCalculationNode::resolved_type() const
{
    auto type = m_value->resolved_type();
    if (type == CalculatedStyleValue::ResolvedType::Integer)
        return CalculatedStyleValue::ResolvedType::Number;
    return type;
}

bool InvertCalculationNode::contains_percentage() const
{
    return m_value->contains_percentage();
}

CalculatedStyleValue::CalculationResult InvertCalculationNode::resolve(Optional<Length::ResolutionContext const&> context, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    auto child_value = m_value->resolve(context, percentage_basis);
    child_value.invert();
    return child_value;
}

ErrorOr<void> InvertCalculationNode::for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const& callback)
{
    TRY(m_value->for_each_child_node(callback));
    TRY(callback(m_value));
    return {};
}

ErrorOr<void> InvertCalculationNode::dump(StringBuilder& builder, int indent) const
{
    TRY(builder.try_appendff("{: >{}}INVERT:\n", "", indent));
    TRY(m_value->dump(builder, indent + 2));
    return {};
}

ErrorOr<NonnullOwnPtr<MinCalculationNode>> MinCalculationNode::create(Vector<NonnullOwnPtr<Web::CSS::CalculationNode>> values)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) MinCalculationNode(move(values)));
}

MinCalculationNode::MinCalculationNode(Vector<NonnullOwnPtr<CalculationNode>> values)
    : CalculationNode(Type::Min)
    , m_values(move(values))
{
}

MinCalculationNode::~MinCalculationNode() = default;

ErrorOr<String> MinCalculationNode::to_string() const
{
    StringBuilder builder;
    TRY(builder.try_append("min("sv));
    for (size_t i = 0; i < m_values.size(); ++i) {
        if (i != 0)
            TRY(builder.try_append(", "sv));
        TRY(builder.try_append(TRY(m_values[i]->to_string())));
    }
    TRY(builder.try_append(")"sv));
    return builder.to_string();
}

Optional<CalculatedStyleValue::ResolvedType> MinCalculationNode::resolved_type() const
{
    // NOTE: We check during parsing that all values have the same type.
    return m_values[0]->resolved_type();
}

bool MinCalculationNode::contains_percentage() const
{
    for (auto const& value : m_values) {
        if (value->contains_percentage())
            return true;
    }

    return false;
}

CalculatedStyleValue::CalculationResult MinCalculationNode::resolve(Optional<Length::ResolutionContext const&> context, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    CalculatedStyleValue::CalculationResult smallest_node = m_values.first()->resolve(context, percentage_basis);
    auto smallest_value = resolve_value(smallest_node.value(), context);

    for (size_t i = 1; i < m_values.size(); i++) {
        auto child_resolved = m_values[i]->resolve(context, percentage_basis);
        auto child_value = resolve_value(child_resolved.value(), context);

        if (child_value < smallest_value) {
            smallest_value = child_value;
            smallest_node = child_resolved;
        }
    }

    return smallest_node;
}

ErrorOr<void> MinCalculationNode::for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const& callback)
{
    for (auto& value : m_values) {
        TRY(value->for_each_child_node(callback));
        TRY(callback(value));
    }

    return {};
}

ErrorOr<void> MinCalculationNode::dump(StringBuilder& builder, int indent) const
{
    TRY(builder.try_appendff("{: >{}}MIN:\n", "", indent));
    for (auto const& value : m_values)
        TRY(value->dump(builder, indent + 2));
    return {};
}

ErrorOr<NonnullOwnPtr<MaxCalculationNode>> MaxCalculationNode::create(Vector<NonnullOwnPtr<Web::CSS::CalculationNode>> values)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) MaxCalculationNode(move(values)));
}

MaxCalculationNode::MaxCalculationNode(Vector<NonnullOwnPtr<CalculationNode>> values)
    : CalculationNode(Type::Max)
    , m_values(move(values))
{
}

MaxCalculationNode::~MaxCalculationNode() = default;

ErrorOr<String> MaxCalculationNode::to_string() const
{
    StringBuilder builder;
    TRY(builder.try_append("max("sv));
    for (size_t i = 0; i < m_values.size(); ++i) {
        if (i != 0)
            TRY(builder.try_append(", "sv));
        TRY(builder.try_append(TRY(m_values[i]->to_string())));
    }
    TRY(builder.try_append(")"sv));
    return builder.to_string();
}

Optional<CalculatedStyleValue::ResolvedType> MaxCalculationNode::resolved_type() const
{
    // NOTE: We check during parsing that all values have the same type.
    return m_values[0]->resolved_type();
}

bool MaxCalculationNode::contains_percentage() const
{
    for (auto const& value : m_values) {
        if (value->contains_percentage())
            return true;
    }

    return false;
}

CalculatedStyleValue::CalculationResult MaxCalculationNode::resolve(Optional<Length::ResolutionContext const&> context, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    CalculatedStyleValue::CalculationResult largest_node = m_values.first()->resolve(context, percentage_basis);
    auto largest_value = resolve_value(largest_node.value(), context);

    for (size_t i = 1; i < m_values.size(); i++) {
        auto child_resolved = m_values[i]->resolve(context, percentage_basis);
        auto child_value = resolve_value(child_resolved.value(), context);

        if (child_value > largest_value) {
            largest_value = child_value;
            largest_node = child_resolved;
        }
    }

    return largest_node;
}

ErrorOr<void> MaxCalculationNode::for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const& callback)
{
    for (auto& value : m_values) {
        TRY(value->for_each_child_node(callback));
        TRY(callback(value));
    }

    return {};
}

ErrorOr<void> MaxCalculationNode::dump(StringBuilder& builder, int indent) const
{
    TRY(builder.try_appendff("{: >{}}MAX:\n", "", indent));
    for (auto const& value : m_values)
        TRY(value->dump(builder, indent + 2));
    return {};
}

ErrorOr<NonnullOwnPtr<ClampCalculationNode>> ClampCalculationNode::create(NonnullOwnPtr<CalculationNode> min, NonnullOwnPtr<CalculationNode> center, NonnullOwnPtr<CalculationNode> max)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) ClampCalculationNode(move(min), move(center), move(max)));
}

ClampCalculationNode::ClampCalculationNode(NonnullOwnPtr<CalculationNode> min, NonnullOwnPtr<CalculationNode> center, NonnullOwnPtr<CalculationNode> max)
    : CalculationNode(Type::Clamp)
    , m_min_value(move(min))
    , m_center_value(move(center))
    , m_max_value(move(max))
{
}

ClampCalculationNode::~ClampCalculationNode() = default;

ErrorOr<String> ClampCalculationNode::to_string() const
{
    StringBuilder builder;
    TRY(builder.try_append("clamp("sv));
    TRY(builder.try_append(TRY(m_min_value->to_string())));
    TRY(builder.try_append(", "sv));
    TRY(builder.try_append(TRY(m_center_value->to_string())));
    TRY(builder.try_append(", "sv));
    TRY(builder.try_append(TRY(m_max_value->to_string())));
    TRY(builder.try_append(")"sv));
    return builder.to_string();
}

Optional<CalculatedStyleValue::ResolvedType> ClampCalculationNode::resolved_type() const
{
    // NOTE: We check during parsing that all values have the same type.
    return m_min_value->resolved_type();
}

bool ClampCalculationNode::contains_percentage() const
{
    return m_min_value->contains_percentage() || m_center_value->contains_percentage() || m_max_value->contains_percentage();
}

CalculatedStyleValue::CalculationResult ClampCalculationNode::resolve(Optional<Length::ResolutionContext const&> context, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    auto min_node = m_min_value->resolve(context, percentage_basis);
    auto center_node = m_center_value->resolve(context, percentage_basis);
    auto max_node = m_max_value->resolve(context, percentage_basis);

    auto min_value = resolve_value(min_node.value(), context);
    auto center_value = resolve_value(center_node.value(), context);
    auto max_value = resolve_value(max_node.value(), context);

    // NOTE: The value should be returned as "max(MIN, min(VAL, MAX))"
    auto chosen_value = max(min_value, min(center_value, max_value));
    if (chosen_value == min_value)
        return min_node;
    if (chosen_value == center_value)
        return center_node;
    if (chosen_value == max_value)
        return max_node;

    VERIFY_NOT_REACHED();
}

ErrorOr<void> ClampCalculationNode::for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const& callback)
{
    TRY(m_min_value->for_each_child_node(callback));
    TRY(m_center_value->for_each_child_node(callback));
    TRY(m_max_value->for_each_child_node(callback));
    TRY(callback(m_min_value));
    TRY(callback(m_center_value));
    TRY(callback(m_max_value));

    return {};
}

ErrorOr<void> ClampCalculationNode::dump(StringBuilder& builder, int indent) const
{
    TRY(builder.try_appendff("{: >{}}CLAMP:\n", "", indent));
    TRY(m_min_value->dump(builder, indent + 2));
    TRY(m_center_value->dump(builder, indent + 2));
    TRY(m_max_value->dump(builder, indent + 2));
    return {};
}

ErrorOr<NonnullOwnPtr<AbsCalculationNode>> AbsCalculationNode::create(NonnullOwnPtr<CalculationNode> value)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) AbsCalculationNode(move(value)));
}

AbsCalculationNode::AbsCalculationNode(NonnullOwnPtr<CalculationNode> value)
    : CalculationNode(Type::Abs)
    , m_value(move(value))
{
}

AbsCalculationNode::~AbsCalculationNode() = default;

ErrorOr<String> AbsCalculationNode::to_string() const
{
    StringBuilder builder;
    builder.append("abs("sv);
    builder.append(TRY(m_value->to_string()));
    builder.append(")"sv);
    return builder.to_string();
}

Optional<CalculatedStyleValue::ResolvedType> AbsCalculationNode::resolved_type() const
{
    return m_value->resolved_type();
}

bool AbsCalculationNode::contains_percentage() const
{
    return m_value->contains_percentage();
}

CalculatedStyleValue::CalculationResult AbsCalculationNode::resolve(Optional<Length::ResolutionContext const&> context, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    auto resolved_type = m_value->resolved_type().value();
    auto node_a = m_value->resolve(context, percentage_basis);
    auto node_a_value = resolve_value(node_a.value(), context);

    if (node_a_value < 0)
        return to_resolved_type(resolved_type, -node_a_value);

    return node_a;
}

ErrorOr<void> AbsCalculationNode::for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const& callback)
{
    TRY(m_value->for_each_child_node(callback));
    TRY(callback(m_value));
    return {};
}

ErrorOr<void> AbsCalculationNode::dump(StringBuilder& builder, int indent) const
{
    TRY(builder.try_appendff("{: >{}}ABS: {}\n", "", indent, TRY(to_string())));
    return {};
}

ErrorOr<NonnullOwnPtr<SignCalculationNode>> SignCalculationNode::create(NonnullOwnPtr<CalculationNode> value)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) SignCalculationNode(move(value)));
}

SignCalculationNode::SignCalculationNode(NonnullOwnPtr<CalculationNode> value)
    : CalculationNode(Type::Sign)
    , m_value(move(value))
{
}

SignCalculationNode::~SignCalculationNode() = default;

ErrorOr<String> SignCalculationNode::to_string() const
{
    StringBuilder builder;
    builder.append("sign("sv);
    builder.append(TRY(m_value->to_string()));
    builder.append(")"sv);
    return builder.to_string();
}

Optional<CalculatedStyleValue::ResolvedType> SignCalculationNode::resolved_type() const
{
    return CalculatedStyleValue::ResolvedType::Integer;
}

bool SignCalculationNode::contains_percentage() const
{
    return m_value->contains_percentage();
}

CalculatedStyleValue::CalculationResult SignCalculationNode::resolve(Optional<Length::ResolutionContext const&> context, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    auto node_a = m_value->resolve(context, percentage_basis);
    auto node_a_value = resolve_value(node_a.value(), context);

    if (node_a_value < 0)
        return { Number(Number::Type::Integer, -1) };

    if (node_a_value > 0)
        return { Number(Number::Type::Integer, 1) };

    return { Number(Number::Type::Integer, 0) };
}

ErrorOr<void> SignCalculationNode::for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const& callback)
{
    TRY(m_value->for_each_child_node(callback));
    TRY(callback(m_value));
    return {};
}

ErrorOr<void> SignCalculationNode::dump(StringBuilder& builder, int indent) const
{
    TRY(builder.try_appendff("{: >{}}SIGN: {}\n", "", indent, TRY(to_string())));
    return {};
}

ErrorOr<NonnullOwnPtr<ConstantCalculationNode>> ConstantCalculationNode::create(ConstantType constant)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) ConstantCalculationNode(constant));
}

ConstantCalculationNode::ConstantCalculationNode(ConstantType constant)
    : CalculationNode(Type::Constant)
    , m_constant(constant)
{
}

ConstantCalculationNode::~ConstantCalculationNode() = default;

ErrorOr<String> ConstantCalculationNode::to_string() const
{
    switch (m_constant) {
    case CalculationNode::ConstantType::E:
        return "e"_short_string;
    case CalculationNode::ConstantType::PI:
        return "pi"_short_string;
    case CalculationNode::ConstantType::Infinity:
        return "infinity"_string;
    case CalculationNode::ConstantType::MinusInfinity:
        return "-infinity"_string;
    case CalculationNode::ConstantType::NaN:
        return "NaN"_string;
    }

    VERIFY_NOT_REACHED();
}
Optional<CalculatedStyleValue::ResolvedType> ConstantCalculationNode::resolved_type() const
{
    return CalculatedStyleValue::ResolvedType::Number;
}

CalculatedStyleValue::CalculationResult ConstantCalculationNode::resolve([[maybe_unused]] Optional<Length::ResolutionContext const&> context, [[maybe_unused]] CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    switch (m_constant) {
    case CalculationNode::ConstantType::E:
        return { Number(Number::Type::Number, M_E) };
    case CalculationNode::ConstantType::PI:
        return { Number(Number::Type::Number, M_PI) };
    // FIXME: We need to keep track of Infinity and NaN across all nodes, since they require special handling.
    case CalculationNode::ConstantType::Infinity:
        return { Number(Number::Type::Number, NumericLimits<float>::max()) };
    case CalculationNode::ConstantType::MinusInfinity:
        return { Number(Number::Type::Number, NumericLimits<float>::lowest()) };
    case CalculationNode::ConstantType::NaN:
        return { Number(Number::Type::Number, NAN) };
    }

    VERIFY_NOT_REACHED();
}

ErrorOr<void> ConstantCalculationNode::for_each_child_node([[maybe_unused]] Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const& callback)
{
    return {};
}

ErrorOr<void> ConstantCalculationNode::dump(StringBuilder& builder, int indent) const
{
    TRY(builder.try_appendff("{: >{}}CONSTANT: {}\n", "", indent, TRY(to_string())));
    return {};
}

ErrorOr<NonnullOwnPtr<SinCalculationNode>> SinCalculationNode::create(NonnullOwnPtr<CalculationNode> value)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) SinCalculationNode(move(value)));
}

SinCalculationNode::SinCalculationNode(NonnullOwnPtr<CalculationNode> value)
    : CalculationNode(Type::Sin)
    , m_value(move(value))
{
}

SinCalculationNode::~SinCalculationNode() = default;

ErrorOr<String> SinCalculationNode::to_string() const
{
    StringBuilder builder;
    builder.append("sin("sv);
    builder.append(TRY(m_value->to_string()));
    builder.append(")"sv);
    return builder.to_string();
}

Optional<CalculatedStyleValue::ResolvedType> SinCalculationNode::resolved_type() const
{
    return CalculatedStyleValue::ResolvedType::Number;
}

bool SinCalculationNode::contains_percentage() const
{
    return m_value->contains_percentage();
}

CalculatedStyleValue::CalculationResult SinCalculationNode::resolve(Optional<Length::ResolutionContext const&> context, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    auto node_a = m_value->resolve(context, percentage_basis);
    auto node_a_value = resolve_value_radians(node_a.value());
    auto result = sin(node_a_value);

    return { Number(Number::Type::Number, result) };
}

ErrorOr<void> SinCalculationNode::for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const& callback)
{
    TRY(m_value->for_each_child_node(callback));
    TRY(callback(m_value));
    return {};
}

ErrorOr<void> SinCalculationNode::dump(StringBuilder& builder, int indent) const
{
    TRY(builder.try_appendff("{: >{}}SIN: {}\n", "", indent, TRY(to_string())));
    return {};
}

ErrorOr<NonnullOwnPtr<CosCalculationNode>> CosCalculationNode::create(NonnullOwnPtr<CalculationNode> value)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) CosCalculationNode(move(value)));
}

CosCalculationNode::CosCalculationNode(NonnullOwnPtr<CalculationNode> value)
    : CalculationNode(Type::Cos)
    , m_value(move(value))
{
}

CosCalculationNode::~CosCalculationNode() = default;

ErrorOr<String> CosCalculationNode::to_string() const
{
    StringBuilder builder;
    builder.append("cos("sv);
    builder.append(TRY(m_value->to_string()));
    builder.append(")"sv);
    return builder.to_string();
}

Optional<CalculatedStyleValue::ResolvedType> CosCalculationNode::resolved_type() const
{
    return CalculatedStyleValue::ResolvedType::Number;
}

bool CosCalculationNode::contains_percentage() const
{
    return m_value->contains_percentage();
}

CalculatedStyleValue::CalculationResult CosCalculationNode::resolve(Optional<Length::ResolutionContext const&> context, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    auto node_a = m_value->resolve(context, percentage_basis);
    auto node_a_value = resolve_value_radians(node_a.value());
    auto result = cos(node_a_value);

    return { Number(Number::Type::Number, result) };
}

ErrorOr<void> CosCalculationNode::for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const& callback)
{
    TRY(m_value->for_each_child_node(callback));
    TRY(callback(m_value));
    return {};
}

ErrorOr<void> CosCalculationNode::dump(StringBuilder& builder, int indent) const
{
    TRY(builder.try_appendff("{: >{}}COS: {}\n", "", indent, TRY(to_string())));
    return {};
}

ErrorOr<NonnullOwnPtr<TanCalculationNode>> TanCalculationNode::create(NonnullOwnPtr<CalculationNode> value)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) TanCalculationNode(move(value)));
}

TanCalculationNode::TanCalculationNode(NonnullOwnPtr<CalculationNode> value)
    : CalculationNode(Type::Tan)
    , m_value(move(value))
{
}

TanCalculationNode::~TanCalculationNode() = default;

ErrorOr<String> TanCalculationNode::to_string() const
{
    StringBuilder builder;
    builder.append("tan("sv);
    builder.append(TRY(m_value->to_string()));
    builder.append(")"sv);
    return builder.to_string();
}

Optional<CalculatedStyleValue::ResolvedType> TanCalculationNode::resolved_type() const
{
    return CalculatedStyleValue::ResolvedType::Number;
}

bool TanCalculationNode::contains_percentage() const
{
    return m_value->contains_percentage();
}

CalculatedStyleValue::CalculationResult TanCalculationNode::resolve(Optional<Length::ResolutionContext const&> context, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    auto node_a = m_value->resolve(context, percentage_basis);
    auto node_a_value = resolve_value_radians(node_a.value());
    auto result = tan(node_a_value);

    return { Number(Number::Type::Number, result) };
}

ErrorOr<void> TanCalculationNode::for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const& callback)
{
    TRY(m_value->for_each_child_node(callback));
    TRY(callback(m_value));
    return {};
}

ErrorOr<void> TanCalculationNode::dump(StringBuilder& builder, int indent) const
{
    TRY(builder.try_appendff("{: >{}}TAN: {}\n", "", indent, TRY(to_string())));
    return {};
}

ErrorOr<NonnullOwnPtr<AsinCalculationNode>> AsinCalculationNode::create(NonnullOwnPtr<CalculationNode> value)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) AsinCalculationNode(move(value)));
}

AsinCalculationNode::AsinCalculationNode(NonnullOwnPtr<CalculationNode> value)
    : CalculationNode(Type::Asin)
    , m_value(move(value))
{
}

AsinCalculationNode::~AsinCalculationNode() = default;

ErrorOr<String> AsinCalculationNode::to_string() const
{
    StringBuilder builder;
    builder.append("asin("sv);
    builder.append(TRY(m_value->to_string()));
    builder.append(")"sv);
    return builder.to_string();
}

Optional<CalculatedStyleValue::ResolvedType> AsinCalculationNode::resolved_type() const
{
    return CalculatedStyleValue::ResolvedType::Angle;
}

bool AsinCalculationNode::contains_percentage() const
{
    return m_value->contains_percentage();
}

CalculatedStyleValue::CalculationResult AsinCalculationNode::resolve(Optional<Length::ResolutionContext const&> context, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    auto node_a = m_value->resolve(context, percentage_basis);
    auto node_a_value = resolve_value(node_a.value(), context);
    auto result = asin(node_a_value);

    return { Angle(result, Angle::Type::Rad) };
}

ErrorOr<void> AsinCalculationNode::for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const& callback)
{
    TRY(m_value->for_each_child_node(callback));
    TRY(callback(m_value));
    return {};
}

ErrorOr<void> AsinCalculationNode::dump(StringBuilder& builder, int indent) const
{
    TRY(builder.try_appendff("{: >{}}ASIN: {}\n", "", indent, TRY(to_string())));
    return {};
}

ErrorOr<NonnullOwnPtr<AcosCalculationNode>> AcosCalculationNode::create(NonnullOwnPtr<CalculationNode> value)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) AcosCalculationNode(move(value)));
}

AcosCalculationNode::AcosCalculationNode(NonnullOwnPtr<CalculationNode> value)
    : CalculationNode(Type::Acos)
    , m_value(move(value))
{
}

AcosCalculationNode::~AcosCalculationNode() = default;

ErrorOr<String> AcosCalculationNode::to_string() const
{
    StringBuilder builder;
    builder.append("acos("sv);
    builder.append(TRY(m_value->to_string()));
    builder.append(")"sv);
    return builder.to_string();
}

Optional<CalculatedStyleValue::ResolvedType> AcosCalculationNode::resolved_type() const
{
    return CalculatedStyleValue::ResolvedType::Angle;
}

bool AcosCalculationNode::contains_percentage() const
{
    return m_value->contains_percentage();
}

CalculatedStyleValue::CalculationResult AcosCalculationNode::resolve(Optional<Length::ResolutionContext const&> context, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    auto node_a = m_value->resolve(context, percentage_basis);
    auto node_a_value = resolve_value(node_a.value(), context);
    auto result = acos(node_a_value);

    return { Angle(result, Angle::Type::Rad) };
}

ErrorOr<void> AcosCalculationNode::for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const& callback)
{
    TRY(m_value->for_each_child_node(callback));
    TRY(callback(m_value));
    return {};
}

ErrorOr<void> AcosCalculationNode::dump(StringBuilder& builder, int indent) const
{
    TRY(builder.try_appendff("{: >{}}ACOS: {}\n", "", indent, TRY(to_string())));
    return {};
}

ErrorOr<NonnullOwnPtr<AtanCalculationNode>> AtanCalculationNode::create(NonnullOwnPtr<CalculationNode> value)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) AtanCalculationNode(move(value)));
}

AtanCalculationNode::AtanCalculationNode(NonnullOwnPtr<CalculationNode> value)
    : CalculationNode(Type::Atan)
    , m_value(move(value))
{
}

AtanCalculationNode::~AtanCalculationNode() = default;

ErrorOr<String> AtanCalculationNode::to_string() const
{
    StringBuilder builder;
    builder.append("atan("sv);
    builder.append(TRY(m_value->to_string()));
    builder.append(")"sv);
    return builder.to_string();
}

Optional<CalculatedStyleValue::ResolvedType> AtanCalculationNode::resolved_type() const
{
    return CalculatedStyleValue::ResolvedType::Angle;
}

bool AtanCalculationNode::contains_percentage() const
{
    return m_value->contains_percentage();
}

CalculatedStyleValue::CalculationResult AtanCalculationNode::resolve(Optional<Length::ResolutionContext const&> context, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    auto node_a = m_value->resolve(context, percentage_basis);
    auto node_a_value = resolve_value(node_a.value(), context);
    auto result = atan(node_a_value);

    return { Angle(result, Angle::Type::Rad) };
}

ErrorOr<void> AtanCalculationNode::for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const& callback)
{
    TRY(m_value->for_each_child_node(callback));
    TRY(callback(m_value));
    return {};
}

ErrorOr<void> AtanCalculationNode::dump(StringBuilder& builder, int indent) const
{
    TRY(builder.try_appendff("{: >{}}ATAN: {}\n", "", indent, TRY(to_string())));
    return {};
}

ErrorOr<NonnullOwnPtr<Atan2CalculationNode>> Atan2CalculationNode::create(NonnullOwnPtr<CalculationNode> y, NonnullOwnPtr<CalculationNode> x)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) Atan2CalculationNode(move(y), move(x)));
}

Atan2CalculationNode::Atan2CalculationNode(NonnullOwnPtr<CalculationNode> y, NonnullOwnPtr<CalculationNode> x)
    : CalculationNode(Type::Atan2)
    , m_y(move(y))
    , m_x(move(x))
{
}

Atan2CalculationNode::~Atan2CalculationNode() = default;

ErrorOr<String> Atan2CalculationNode::to_string() const
{
    StringBuilder builder;
    builder.append("atan2("sv);
    builder.append(TRY(m_y->to_string()));
    builder.append(", "sv);
    builder.append(TRY(m_x->to_string()));
    builder.append(")"sv);
    return builder.to_string();
}

Optional<CalculatedStyleValue::ResolvedType> Atan2CalculationNode::resolved_type() const
{
    return CalculatedStyleValue::ResolvedType::Angle;
}

bool Atan2CalculationNode::contains_percentage() const
{
    return m_y->contains_percentage() || m_x->contains_percentage();
}

CalculatedStyleValue::CalculationResult Atan2CalculationNode::resolve(Optional<Length::ResolutionContext const&> context, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    auto node_a = m_y->resolve(context, percentage_basis);
    auto node_a_value = resolve_value(node_a.value(), context);

    auto node_b = m_x->resolve(context, percentage_basis);
    auto node_b_value = resolve_value(node_b.value(), context);

    auto result = atan2(node_a_value, node_b_value);

    return { Angle(result, Angle::Type::Rad) };
}

ErrorOr<void> Atan2CalculationNode::for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const& callback)
{
    TRY(m_y->for_each_child_node(callback));
    TRY(m_x->for_each_child_node(callback));
    TRY(callback(m_y));
    TRY(callback(m_x));
    return {};
}

ErrorOr<void> Atan2CalculationNode::dump(StringBuilder& builder, int indent) const
{
    TRY(builder.try_appendff("{: >{}}ATAN2: {}\n", "", indent, TRY(to_string())));
    return {};
}

ErrorOr<NonnullOwnPtr<PowCalculationNode>> PowCalculationNode::create(NonnullOwnPtr<CalculationNode> x, NonnullOwnPtr<CalculationNode> y)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) PowCalculationNode(move(x), move(y)));
}

PowCalculationNode::PowCalculationNode(NonnullOwnPtr<CalculationNode> x, NonnullOwnPtr<CalculationNode> y)
    : CalculationNode(Type::Pow)
    , m_x(move(x))
    , m_y(move(y))
{
}

PowCalculationNode::~PowCalculationNode() = default;

ErrorOr<String> PowCalculationNode::to_string() const
{
    StringBuilder builder;
    builder.append("pow("sv);
    builder.append(TRY(m_x->to_string()));
    builder.append(", "sv);
    builder.append(TRY(m_y->to_string()));
    builder.append(")"sv);
    return builder.to_string();
}

Optional<CalculatedStyleValue::ResolvedType> PowCalculationNode::resolved_type() const
{
    return CalculatedStyleValue::ResolvedType::Number;
}

CalculatedStyleValue::CalculationResult PowCalculationNode::resolve(Optional<Length::ResolutionContext const&> context, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    auto node_a = m_x->resolve(context, percentage_basis);
    auto node_a_value = resolve_value(node_a.value(), context);

    auto node_b = m_y->resolve(context, percentage_basis);
    auto node_b_value = resolve_value(node_b.value(), context);

    auto result = pow(node_a_value, node_b_value);

    return { Number(Number::Type::Number, result) };
}

ErrorOr<void> PowCalculationNode::for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const& callback)
{
    TRY(m_x->for_each_child_node(callback));
    TRY(m_y->for_each_child_node(callback));
    TRY(callback(m_x));
    TRY(callback(m_y));
    return {};
}

ErrorOr<void> PowCalculationNode::dump(StringBuilder& builder, int indent) const
{
    TRY(builder.try_appendff("{: >{}}POW: {}\n", "", indent, TRY(to_string())));
    return {};
}

ErrorOr<NonnullOwnPtr<SqrtCalculationNode>> SqrtCalculationNode::create(NonnullOwnPtr<CalculationNode> value)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) SqrtCalculationNode(move(value)));
}

SqrtCalculationNode::SqrtCalculationNode(NonnullOwnPtr<CalculationNode> value)
    : CalculationNode(Type::Sqrt)
    , m_value(move(value))
{
}

SqrtCalculationNode::~SqrtCalculationNode() = default;

ErrorOr<String> SqrtCalculationNode::to_string() const
{
    StringBuilder builder;
    builder.append("sqrt("sv);
    builder.append(TRY(m_value->to_string()));
    builder.append(")"sv);
    return builder.to_string();
}

Optional<CalculatedStyleValue::ResolvedType> SqrtCalculationNode::resolved_type() const
{
    return CalculatedStyleValue::ResolvedType::Number;
}

CalculatedStyleValue::CalculationResult SqrtCalculationNode::resolve(Optional<Length::ResolutionContext const&> context, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    auto node_a = m_value->resolve(context, percentage_basis);
    auto node_a_value = resolve_value(node_a.value(), context);
    auto result = sqrt(node_a_value);

    return { Number(Number::Type::Number, result) };
}

ErrorOr<void> SqrtCalculationNode::for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const& callback)
{
    TRY(m_value->for_each_child_node(callback));
    TRY(callback(m_value));
    return {};
}

ErrorOr<void> SqrtCalculationNode::dump(StringBuilder& builder, int indent) const
{
    TRY(builder.try_appendff("{: >{}}SQRT: {}\n", "", indent, TRY(to_string())));
    return {};
}

ErrorOr<NonnullOwnPtr<HypotCalculationNode>> HypotCalculationNode::create(Vector<NonnullOwnPtr<Web::CSS::CalculationNode>> values)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) HypotCalculationNode(move(values)));
}

HypotCalculationNode::HypotCalculationNode(Vector<NonnullOwnPtr<CalculationNode>> values)
    : CalculationNode(Type::Hypot)
    , m_values(move(values))
{
}

HypotCalculationNode::~HypotCalculationNode() = default;

ErrorOr<String> HypotCalculationNode::to_string() const
{
    StringBuilder builder;
    TRY(builder.try_append("hypot("sv));
    for (size_t i = 0; i < m_values.size(); ++i) {
        if (i != 0)
            TRY(builder.try_append(", "sv));
        TRY(builder.try_append(TRY(m_values[i]->to_string())));
    }
    TRY(builder.try_append(")"sv));
    return builder.to_string();
}

Optional<CalculatedStyleValue::ResolvedType> HypotCalculationNode::resolved_type() const
{
    // NOTE: We check during parsing that all values have the same type.
    return m_values[0]->resolved_type();
}

bool HypotCalculationNode::contains_percentage() const
{
    for (auto const& value : m_values) {
        if (value->contains_percentage())
            return true;
    }

    return false;
}

CalculatedStyleValue::CalculationResult HypotCalculationNode::resolve(Optional<Length::ResolutionContext const&> context, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    double square_sum = 0.0;

    for (auto const& value : m_values) {
        auto child_resolved = value->resolve(context, percentage_basis);
        auto child_value = resolve_value(child_resolved.value(), context);

        square_sum += child_value * child_value;
    }

    auto result = sqrt(square_sum);

    return to_resolved_type(resolved_type().value(), result);
}

ErrorOr<void> HypotCalculationNode::for_each_child_node(Function<ErrorOr<void>(NonnullOwnPtr<CalculationNode>&)> const& callback)
{
    for (auto& value : m_values) {
        TRY(value->for_each_child_node(callback));
        TRY(callback(value));
    }

    return {};
}

ErrorOr<void> HypotCalculationNode::dump(StringBuilder& builder, int indent) const
{
    TRY(builder.try_appendff("{: >{}}HYPOT:\n", "", indent));
    for (auto const& value : m_values)
        TRY(value->dump(builder, indent + 2));
    return {};
}

void CalculatedStyleValue::CalculationResult::add(CalculationResult const& other, Optional<Length::ResolutionContext const&> context, PercentageBasis const& percentage_basis)
{
    add_or_subtract_internal(SumOperation::Add, other, context, percentage_basis);
}

void CalculatedStyleValue::CalculationResult::subtract(CalculationResult const& other, Optional<Length::ResolutionContext const&> context, PercentageBasis const& percentage_basis)
{
    add_or_subtract_internal(SumOperation::Subtract, other, context, percentage_basis);
}

void CalculatedStyleValue::CalculationResult::add_or_subtract_internal(SumOperation op, CalculationResult const& other, Optional<Length::ResolutionContext const&> context, PercentageBasis const& percentage_basis)
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
            auto this_px = length.to_px(*context);
            if (other.m_value.has<Length>()) {
                auto other_px = other.m_value.get<Length>().to_px(*context);
                if (op == SumOperation::Add)
                    m_value = Length::make_px(this_px + other_px);
                else
                    m_value = Length::make_px(this_px - other_px);
            } else {
                VERIFY(percentage_basis.has<Length>());

                auto other_px = percentage_basis.get<Length>().percentage_of(other.m_value.get<Percentage>()).to_px(*context);
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
                new_value.add(*this, context, percentage_basis);
            } else {
                // Turn 'this - other' into '-other + this', as 'A + B == B + A', but 'A - B != B - A'
                new_value.multiply_by({ Number { Number::Type::Integer, -1.0f } }, context);
                new_value.add(*this, context, percentage_basis);
            }

            *this = new_value;
        });
}

void CalculatedStyleValue::CalculationResult::multiply_by(CalculationResult const& other, Optional<Length::ResolutionContext const&> context)
{
    // We know from validation when resolving the type, that at least one side must be a <number> or <integer>.
    // Both of these are represented as a double.
    VERIFY(m_value.has<Number>() || other.m_value.has<Number>());
    bool other_is_number = other.m_value.has<Number>();

    m_value.visit(
        [&](Number const& number) {
            if (other_is_number) {
                m_value = number * other.m_value.get<Number>();
            } else {
                // Avoid duplicating all the logic by swapping `this` and `other`.
                CalculationResult new_value = other;
                new_value.multiply_by(*this, context);
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
            m_value = Length::make_px(length.to_px(*context) * static_cast<double>(other.m_value.get<Number>().value()));
        },
        [&](Time const& time) {
            m_value = Time::make_seconds(time.to_seconds() * other.m_value.get<Number>().value());
        },
        [&](Percentage const& percentage) {
            m_value = Percentage { percentage.value() * other.m_value.get<Number>().value() };
        });
}

void CalculatedStyleValue::CalculationResult::divide_by(CalculationResult const& other, Optional<Length::ResolutionContext const&> context)
{
    // We know from validation when resolving the type, that `other` must be a <number> or <integer>.
    // Both of these are represented as a Number.
    auto denominator = other.m_value.get<Number>().value();
    // FIXME: Dividing by 0 is invalid, and should be caught during parsing.
    VERIFY(denominator != 0.0);

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
            m_value = Length::make_px(length.to_px(*context) / static_cast<double>(denominator));
        },
        [&](Time const& time) {
            m_value = Time::make_seconds(time.to_seconds() / denominator);
        },
        [&](Percentage const& percentage) {
            m_value = Percentage { percentage.value() / denominator };
        });
}

void CalculatedStyleValue::CalculationResult::negate()
{
    m_value.visit(
        [&](Number const& number) {
            m_value = Number { number.type(), 0 - number.value() };
        },
        [&](Angle const& angle) {
            m_value = Angle { 0 - angle.raw_value(), angle.type() };
        },
        [&](Frequency const& frequency) {
            m_value = Frequency { 0 - frequency.raw_value(), frequency.type() };
        },
        [&](Length const& length) {
            m_value = Length { 0 - length.raw_value(), length.type() };
        },
        [&](Time const& time) {
            m_value = Time { 0 - time.raw_value(), time.type() };
        },
        [&](Percentage const& percentage) {
            m_value = Percentage { 0 - percentage.value() };
        });
}

void CalculatedStyleValue::CalculationResult::invert()
{
    // FIXME: Correctly handle division by zero.
    m_value.visit(
        [&](Number const& number) {
            m_value = Number { Number::Type::Number, 1 / number.value() };
        },
        [&](Angle const& angle) {
            m_value = Angle { 1 / angle.raw_value(), angle.type() };
        },
        [&](Frequency const& frequency) {
            m_value = Frequency { 1 / frequency.raw_value(), frequency.type() };
        },
        [&](Length const& length) {
            m_value = Length { 1 / length.raw_value(), length.type() };
        },
        [&](Time const& time) {
            m_value = Time { 1 / time.raw_value(), time.type() };
        },
        [&](Percentage const& percentage) {
            m_value = Percentage { 1 / percentage.value() };
        });
}

ErrorOr<String> CalculatedStyleValue::to_string() const
{
    // FIXME: Implement this according to https://www.w3.org/TR/css-values-4/#calc-serialize once that stabilizes.
    return String::formatted("calc({})", TRY(m_calculation->to_string()));
}

bool CalculatedStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    // This is a case where comparing the strings actually makes sense.
    return to_string().release_value_but_fixme_should_propagate_errors() == other.to_string().release_value_but_fixme_should_propagate_errors();
}

Optional<Angle> CalculatedStyleValue::resolve_angle() const
{
    auto result = m_calculation->resolve({}, {});

    if (result.value().has<Angle>())
        return result.value().get<Angle>();
    return {};
}

Optional<Angle> CalculatedStyleValue::resolve_angle_percentage(Angle const& percentage_basis) const
{
    auto result = m_calculation->resolve({}, percentage_basis);

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
    auto result = m_calculation->resolve({}, {});

    if (result.value().has<Frequency>())
        return result.value().get<Frequency>();
    return {};
}

Optional<Frequency> CalculatedStyleValue::resolve_frequency_percentage(Frequency const& percentage_basis) const
{
    auto result = m_calculation->resolve({}, percentage_basis);

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

Optional<Length> CalculatedStyleValue::resolve_length(Length::ResolutionContext const& context) const
{
    auto result = m_calculation->resolve(context, {});

    if (result.value().has<Length>())
        return result.value().get<Length>();
    return {};
}

Optional<Length> CalculatedStyleValue::resolve_length(Layout::Node const& layout_node) const
{
    return resolve_length(Length::ResolutionContext::for_layout_node(layout_node));
}

Optional<Length> CalculatedStyleValue::resolve_length_percentage(Layout::Node const& layout_node, Length const& percentage_basis) const
{
    auto result = m_calculation->resolve(Length::ResolutionContext::for_layout_node(layout_node), percentage_basis);

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
    auto result = m_calculation->resolve({}, {});
    if (result.value().has<Percentage>())
        return result.value().get<Percentage>();
    return {};
}

Optional<Time> CalculatedStyleValue::resolve_time() const
{
    auto result = m_calculation->resolve({}, {});

    if (result.value().has<Time>())
        return result.value().get<Time>();
    return {};
}

Optional<Time> CalculatedStyleValue::resolve_time_percentage(Time const& percentage_basis) const
{
    auto result = m_calculation->resolve({}, percentage_basis);

    return result.value().visit(
        [&](Time const& time) -> Optional<Time> {
            return time;
        },
        [&](auto const&) -> Optional<Time> {
            return {};
        });
}

Optional<double> CalculatedStyleValue::resolve_number() const
{
    auto result = m_calculation->resolve({}, {});
    if (result.value().has<Number>())
        return result.value().get<Number>().value();
    return {};
}

Optional<i64> CalculatedStyleValue::resolve_integer()
{
    auto result = m_calculation->resolve({}, {});
    if (result.value().has<Number>())
        return result.value().get<Number>().integer_value();
    return {};
}

bool CalculatedStyleValue::contains_percentage() const
{
    return m_calculation->contains_percentage();
}

}
