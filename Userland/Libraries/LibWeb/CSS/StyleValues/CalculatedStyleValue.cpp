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

static double resolve_value(CalculatedStyleValue::CalculationResult::Value value, Layout::Node const* layout_node)
{
    return value.visit(
        [](Number const& number) { return number.value(); },
        [](Angle const& angle) { return angle.to_degrees(); },
        [](Frequency const& frequency) { return frequency.to_hertz(); },
        [layout_node](Length const& length) { return length.to_px(*layout_node).value(); },
        [](Percentage const& percentage) { return percentage.value(); },
        [](Time const& time) { return time.to_seconds(); });
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

CalculatedStyleValue::CalculationResult NumericCalculationNode::resolve(Layout::Node const*, CalculatedStyleValue::PercentageBasis const&) const
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

CalculatedStyleValue::CalculationResult SumCalculationNode::resolve(Layout::Node const* layout_node, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    Optional<CalculatedStyleValue::CalculationResult> total;

    for (auto& additional_product : m_values) {
        auto additional_value = additional_product->resolve(layout_node, percentage_basis);
        if (!total.has_value()) {
            total = additional_value;
            continue;
        }
        total->add(additional_value, layout_node, percentage_basis);
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

CalculatedStyleValue::CalculationResult ProductCalculationNode::resolve(Layout::Node const* layout_node, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    Optional<CalculatedStyleValue::CalculationResult> total;

    for (auto& additional_product : m_values) {
        auto additional_value = additional_product->resolve(layout_node, percentage_basis);
        if (!total.has_value()) {
            total = additional_value;
            continue;
        }
        total->multiply_by(additional_value, layout_node);
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

CalculatedStyleValue::CalculationResult NegateCalculationNode::resolve(Layout::Node const* layout_node, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    auto child_value = m_value->resolve(layout_node, percentage_basis);
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

CalculatedStyleValue::CalculationResult InvertCalculationNode::resolve(Layout::Node const* layout_node, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    auto child_value = m_value->resolve(layout_node, percentage_basis);
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

CalculatedStyleValue::CalculationResult MinCalculationNode::resolve(Layout::Node const* layout_node, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    CalculatedStyleValue::CalculationResult smallest_node = m_values.first()->resolve(layout_node, percentage_basis);
    auto smallest_value = resolve_value(smallest_node.value(), layout_node);

    for (size_t i = 1; i < m_values.size(); i++) {
        auto child_resolved = m_values[i]->resolve(layout_node, percentage_basis);
        auto child_value = resolve_value(child_resolved.value(), layout_node);

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

CalculatedStyleValue::CalculationResult MaxCalculationNode::resolve(Layout::Node const* layout_node, CalculatedStyleValue::PercentageBasis const& percentage_basis) const
{
    CalculatedStyleValue::CalculationResult largest_node = m_values.first()->resolve(layout_node, percentage_basis);
    auto largest_value = resolve_value(largest_node.value(), layout_node);

    for (size_t i = 1; i < m_values.size(); i++) {
        auto child_resolved = m_values[i]->resolve(layout_node, percentage_basis);
        auto child_value = resolve_value(child_resolved.value(), layout_node);

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
            m_value = Length::make_px(length.to_px(*layout_node) * static_cast<double>(other.m_value.get<Number>().value()));
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
            VERIFY(layout_node);
            m_value = Length::make_px(length.to_px(*layout_node) / static_cast<double>(denominator));
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
    auto result = m_calculation->resolve(nullptr, {});

    if (result.value().has<Angle>())
        return result.value().get<Angle>();
    return {};
}

Optional<Angle> CalculatedStyleValue::resolve_angle_percentage(Angle const& percentage_basis) const
{
    auto result = m_calculation->resolve(nullptr, percentage_basis);

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
    auto result = m_calculation->resolve(nullptr, {});

    if (result.value().has<Frequency>())
        return result.value().get<Frequency>();
    return {};
}

Optional<Frequency> CalculatedStyleValue::resolve_frequency_percentage(Frequency const& percentage_basis) const
{
    auto result = m_calculation->resolve(nullptr, percentage_basis);

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
    auto result = m_calculation->resolve(&layout_node, {});

    if (result.value().has<Length>())
        return result.value().get<Length>();
    return {};
}

Optional<Length> CalculatedStyleValue::resolve_length_percentage(Layout::Node const& layout_node, Length const& percentage_basis) const
{
    auto result = m_calculation->resolve(&layout_node, percentage_basis);

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
    auto result = m_calculation->resolve(nullptr, {});
    if (result.value().has<Percentage>())
        return result.value().get<Percentage>();
    return {};
}

Optional<Time> CalculatedStyleValue::resolve_time() const
{
    auto result = m_calculation->resolve(nullptr, {});

    if (result.value().has<Time>())
        return result.value().get<Time>();
    return {};
}

Optional<Time> CalculatedStyleValue::resolve_time_percentage(Time const& percentage_basis) const
{
    auto result = m_calculation->resolve(nullptr, percentage_basis);

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
    auto result = m_calculation->resolve(nullptr, {});
    if (result.value().has<Number>())
        return result.value().get<Number>().value();
    return {};
}

Optional<i64> CalculatedStyleValue::resolve_integer()
{
    auto result = m_calculation->resolve(nullptr, {});
    if (result.value().has<Number>())
        return result.value().get<Number>().integer_value();
    return {};
}

bool CalculatedStyleValue::contains_percentage() const
{
    return m_calculation->contains_percentage();
}

}
