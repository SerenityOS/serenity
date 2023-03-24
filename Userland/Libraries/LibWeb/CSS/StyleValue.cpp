/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <LibGfx/Palette.h>
#include <LibWeb/CSS/Serialize.h>
#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/CSS/StyleValues/AbstractImageStyleValue.h>
#include <LibWeb/CSS/StyleValues/AngleStyleValue.h>
#include <LibWeb/CSS/StyleValues/BackgroundRepeatStyleValue.h>
#include <LibWeb/CSS/StyleValues/BackgroundSizeStyleValue.h>
#include <LibWeb/CSS/StyleValues/BackgroundStyleValue.h>
#include <LibWeb/CSS/StyleValues/BorderRadiusShorthandStyleValue.h>
#include <LibWeb/CSS/StyleValues/BorderRadiusStyleValue.h>
#include <LibWeb/CSS/StyleValues/BorderStyleValue.h>
#include <LibWeb/CSS/StyleValues/ColorStyleValue.h>
#include <LibWeb/CSS/StyleValues/ConicGradientStyleValue.h>
#include <LibWeb/CSS/StyleValues/ContentStyleValue.h>
#include <LibWeb/CSS/StyleValues/FilterValueListStyleValue.h>
#include <LibWeb/CSS/StyleValues/FlexFlowStyleValue.h>
#include <LibWeb/CSS/StyleValues/FlexStyleValue.h>
#include <LibWeb/CSS/StyleValues/FontStyleValue.h>
#include <LibWeb/CSS/StyleValues/FrequencyStyleValue.h>
#include <LibWeb/CSS/StyleValues/GridAreaShorthandStyleValue.h>
#include <LibWeb/CSS/StyleValues/GridTemplateAreaStyleValue.h>
#include <LibWeb/CSS/StyleValues/GridTrackPlacementShorthandStyleValue.h>
#include <LibWeb/CSS/StyleValues/GridTrackPlacementStyleValue.h>
#include <LibWeb/CSS/StyleValues/GridTrackSizeStyleValue.h>
#include <LibWeb/CSS/StyleValues/IdentifierStyleValue.h>
#include <LibWeb/CSS/StyleValues/ImageStyleValue.h>
#include <LibWeb/CSS/StyleValues/InheritStyleValue.h>
#include <LibWeb/CSS/StyleValues/InitialStyleValue.h>
#include <LibWeb/CSS/StyleValues/LengthStyleValue.h>
#include <LibWeb/CSS/StyleValues/LinearGradientStyleValue.h>
#include <LibWeb/CSS/StyleValues/ListStyleStyleValue.h>
#include <LibWeb/CSS/StyleValues/NumericStyleValue.h>
#include <LibWeb/CSS/StyleValues/OverflowStyleValue.h>
#include <LibWeb/CSS/StyleValues/PercentageStyleValue.h>
#include <LibWeb/CSS/StyleValues/PositionStyleValue.h>
#include <LibWeb/CSS/StyleValues/RadialGradientStyleValue.h>
#include <LibWeb/CSS/StyleValues/ResolutionStyleValue.h>
#include <LibWeb/CSS/StyleValues/ShadowStyleValue.h>
#include <LibWeb/CSS/StyleValues/StringStyleValue.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Loader/LoadRequest.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Painting/GradientPainting.h>
#include <LibWeb/Platform/Timer.h>

namespace Web::CSS {

StyleValue::StyleValue(Type type)
    : m_type(type)
{
}

AbstractImageStyleValue const& StyleValue::as_abstract_image() const
{
    VERIFY(is_abstract_image());
    return static_cast<AbstractImageStyleValue const&>(*this);
}

AngleStyleValue const& StyleValue::as_angle() const
{
    VERIFY(is_angle());
    return static_cast<AngleStyleValue const&>(*this);
}

BackgroundStyleValue const& StyleValue::as_background() const
{
    VERIFY(is_background());
    return static_cast<BackgroundStyleValue const&>(*this);
}

BackgroundRepeatStyleValue const& StyleValue::as_background_repeat() const
{
    VERIFY(is_background_repeat());
    return static_cast<BackgroundRepeatStyleValue const&>(*this);
}

BackgroundSizeStyleValue const& StyleValue::as_background_size() const
{
    VERIFY(is_background_size());
    return static_cast<BackgroundSizeStyleValue const&>(*this);
}

BorderStyleValue const& StyleValue::as_border() const
{
    VERIFY(is_border());
    return static_cast<BorderStyleValue const&>(*this);
}

BorderRadiusStyleValue const& StyleValue::as_border_radius() const
{
    VERIFY(is_border_radius());
    return static_cast<BorderRadiusStyleValue const&>(*this);
}

BorderRadiusShorthandStyleValue const& StyleValue::as_border_radius_shorthand() const
{
    VERIFY(is_border_radius_shorthand());
    return static_cast<BorderRadiusShorthandStyleValue const&>(*this);
}

ShadowStyleValue const& StyleValue::as_shadow() const
{
    VERIFY(is_shadow());
    return static_cast<ShadowStyleValue const&>(*this);
}

CalculatedStyleValue const& StyleValue::as_calculated() const
{
    VERIFY(is_calculated());
    return static_cast<CalculatedStyleValue const&>(*this);
}

ColorStyleValue const& StyleValue::as_color() const
{
    VERIFY(is_color());
    return static_cast<ColorStyleValue const&>(*this);
}

ConicGradientStyleValue const& StyleValue::as_conic_gradient() const
{
    VERIFY(is_conic_gradient());
    return static_cast<ConicGradientStyleValue const&>(*this);
}

ContentStyleValue const& StyleValue::as_content() const
{
    VERIFY(is_content());
    return static_cast<ContentStyleValue const&>(*this);
}

FilterValueListStyleValue const& StyleValue::as_filter_value_list() const
{
    VERIFY(is_filter_value_list());
    return static_cast<FilterValueListStyleValue const&>(*this);
}

FlexStyleValue const& StyleValue::as_flex() const
{
    VERIFY(is_flex());
    return static_cast<FlexStyleValue const&>(*this);
}

FlexFlowStyleValue const& StyleValue::as_flex_flow() const
{
    VERIFY(is_flex_flow());
    return static_cast<FlexFlowStyleValue const&>(*this);
}

FontStyleValue const& StyleValue::as_font() const
{
    VERIFY(is_font());
    return static_cast<FontStyleValue const&>(*this);
}

FrequencyStyleValue const& StyleValue::as_frequency() const
{
    VERIFY(is_frequency());
    return static_cast<FrequencyStyleValue const&>(*this);
}

GridTrackPlacementShorthandStyleValue const& StyleValue::as_grid_track_placement_shorthand() const
{
    VERIFY(is_grid_track_placement_shorthand());
    return static_cast<GridTrackPlacementShorthandStyleValue const&>(*this);
}

GridAreaShorthandStyleValue const& StyleValue::as_grid_area_shorthand() const
{
    VERIFY(is_grid_area_shorthand());
    return static_cast<GridAreaShorthandStyleValue const&>(*this);
}

GridTemplateAreaStyleValue const& StyleValue::as_grid_template_area() const
{
    VERIFY(is_grid_template_area());
    return static_cast<GridTemplateAreaStyleValue const&>(*this);
}

GridTrackPlacementStyleValue const& StyleValue::as_grid_track_placement() const
{
    VERIFY(is_grid_track_placement());
    return static_cast<GridTrackPlacementStyleValue const&>(*this);
}

IdentifierStyleValue const& StyleValue::as_identifier() const
{
    VERIFY(is_identifier());
    return static_cast<IdentifierStyleValue const&>(*this);
}

ImageStyleValue const& StyleValue::as_image() const
{
    VERIFY(is_image());
    return static_cast<ImageStyleValue const&>(*this);
}

InheritStyleValue const& StyleValue::as_inherit() const
{
    VERIFY(is_inherit());
    return static_cast<InheritStyleValue const&>(*this);
}

InitialStyleValue const& StyleValue::as_initial() const
{
    VERIFY(is_initial());
    return static_cast<InitialStyleValue const&>(*this);
}

LengthStyleValue const& StyleValue::as_length() const
{
    VERIFY(is_length());
    return static_cast<LengthStyleValue const&>(*this);
}

GridTrackSizeStyleValue const& StyleValue::as_grid_track_size_list() const
{
    VERIFY(is_grid_track_size_list());
    return static_cast<GridTrackSizeStyleValue const&>(*this);
}

LinearGradientStyleValue const& StyleValue::as_linear_gradient() const
{
    VERIFY(is_linear_gradient());
    return static_cast<LinearGradientStyleValue const&>(*this);
}

ListStyleStyleValue const& StyleValue::as_list_style() const
{
    VERIFY(is_list_style());
    return static_cast<ListStyleStyleValue const&>(*this);
}

NumericStyleValue const& StyleValue::as_numeric() const
{
    VERIFY(is_numeric());
    return static_cast<NumericStyleValue const&>(*this);
}

OverflowStyleValue const& StyleValue::as_overflow() const
{
    VERIFY(is_overflow());
    return static_cast<OverflowStyleValue const&>(*this);
}

PercentageStyleValue const& StyleValue::as_percentage() const
{
    VERIFY(is_percentage());
    return static_cast<PercentageStyleValue const&>(*this);
}

PositionStyleValue const& StyleValue::as_position() const
{
    VERIFY(is_position());
    return static_cast<PositionStyleValue const&>(*this);
}

RadialGradientStyleValue const& StyleValue::as_radial_gradient() const
{
    VERIFY(is_radial_gradient());
    return static_cast<RadialGradientStyleValue const&>(*this);
}

RectStyleValue const& StyleValue::as_rect() const
{
    VERIFY(is_rect());
    return static_cast<RectStyleValue const&>(*this);
}

ResolutionStyleValue const& StyleValue::as_resolution() const
{
    VERIFY(is_resolution());
    return static_cast<ResolutionStyleValue const&>(*this);
}

StringStyleValue const& StyleValue::as_string() const
{
    VERIFY(is_string());
    return static_cast<StringStyleValue const&>(*this);
}

TextDecorationStyleValue const& StyleValue::as_text_decoration() const
{
    VERIFY(is_text_decoration());
    return static_cast<TextDecorationStyleValue const&>(*this);
}

TimeStyleValue const& StyleValue::as_time() const
{
    VERIFY(is_time());
    return static_cast<TimeStyleValue const&>(*this);
}

TransformationStyleValue const& StyleValue::as_transformation() const
{
    VERIFY(is_transformation());
    return static_cast<TransformationStyleValue const&>(*this);
}

UnresolvedStyleValue const& StyleValue::as_unresolved() const
{
    VERIFY(is_unresolved());
    return static_cast<UnresolvedStyleValue const&>(*this);
}

UnsetStyleValue const& StyleValue::as_unset() const
{
    VERIFY(is_unset());
    return static_cast<UnsetStyleValue const&>(*this);
}

StyleValueList const& StyleValue::as_value_list() const
{
    VERIFY(is_value_list());
    return static_cast<StyleValueList const&>(*this);
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

// https://www.w3.org/TR/CSS2/visufx.html#value-def-shape
Gfx::FloatRect EdgeRect::resolved(Layout::Node const& layout_node, Gfx::FloatRect border_box) const
{
    // In CSS 2.1, the only valid <shape> value is: rect(<top>, <right>, <bottom>, <left>) where
    // <top> and <bottom> specify offsets from the top border edge of the box, and <right>, and
    // <left> specify offsets from the left border edge of the box.

    // The value 'auto' means that a given edge of the clipping region will be the same as the edge
    // of the element's generated border box (i.e., 'auto' means the same as '0' for <top> and
    // <left>, the same as the used value of the height plus the sum of vertical padding and border
    // widths for <bottom>, and the same as the used value of the width plus the sum of the
    // horizontal padding and border widths for <right>, such that four 'auto' values result in the
    // clipping region being the same as the element's border box).
    auto left = border_box.left() + (left_edge.is_auto() ? 0 : left_edge.to_px(layout_node)).value();
    auto top = border_box.top() + (top_edge.is_auto() ? 0 : top_edge.to_px(layout_node)).value();
    auto right = border_box.left() + (right_edge.is_auto() ? border_box.width() : right_edge.to_px(layout_node)).value();
    auto bottom = border_box.top() + (bottom_edge.is_auto() ? border_box.height() : bottom_edge.to_px(layout_node)).value();
    return Gfx::FloatRect {
        left,
        top,
        right - left,
        bottom - top
    };
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

CSSPixelPoint PositionValue::resolved(Layout::Node const& node, CSSPixelRect const& rect) const
{
    // Note: A preset + a none default x/y_relative_to is impossible in the syntax (and makes little sense)
    CSSPixels x = horizontal_position.visit(
        [&](HorizontalPreset preset) -> CSSPixels {
            return rect.width() * [&] {
                switch (preset) {
                case HorizontalPreset::Left:
                    return 0.0f;
                case HorizontalPreset::Center:
                    return 0.5f;
                case HorizontalPreset::Right:
                    return 1.0f;
                default:
                    VERIFY_NOT_REACHED();
                }
            }();
        },
        [&](LengthPercentage length_percentage) -> CSSPixels {
            return length_percentage.resolved(node, Length::make_px(rect.width())).to_px(node);
        });
    CSSPixels y = vertical_position.visit(
        [&](VerticalPreset preset) -> CSSPixels {
            return rect.height() * [&] {
                switch (preset) {
                case VerticalPreset::Top:
                    return 0.0f;
                case VerticalPreset::Center:
                    return 0.5f;
                case VerticalPreset::Bottom:
                    return 1.0f;
                default:
                    VERIFY_NOT_REACHED();
                }
            }();
        },
        [&](LengthPercentage length_percentage) -> CSSPixels {
            return length_percentage.resolved(node, Length::make_px(rect.height())).to_px(node);
        });
    if (x_relative_to == HorizontalEdge::Right)
        x = rect.width() - x;
    if (y_relative_to == VerticalEdge::Bottom)
        y = rect.height() - y;
    return CSSPixelPoint { rect.x() + x, rect.y() + y };
}

ErrorOr<void> PositionValue::serialize(StringBuilder& builder) const
{
    // Note: This means our serialization with simplify any with explicit edges that are just `top left`.
    bool has_relative_edges = x_relative_to == HorizontalEdge::Right || y_relative_to == VerticalEdge::Bottom;
    if (has_relative_edges)
        TRY(builder.try_append(x_relative_to == HorizontalEdge::Left ? "left "sv : "right "sv));
    TRY(horizontal_position.visit(
        [&](HorizontalPreset preset) -> ErrorOr<void> {
            return builder.try_append([&] {
                switch (preset) {
                case HorizontalPreset::Left:
                    return "left"sv;
                case HorizontalPreset::Center:
                    return "center"sv;
                case HorizontalPreset::Right:
                    return "right"sv;
                default:
                    VERIFY_NOT_REACHED();
                }
            }());
        },
        [&](LengthPercentage length_percentage) -> ErrorOr<void> {
            return builder.try_appendff(TRY(length_percentage.to_string()));
        }));
    TRY(builder.try_append(' '));
    if (has_relative_edges)
        TRY(builder.try_append(y_relative_to == VerticalEdge::Top ? "top "sv : "bottom "sv));
    TRY(vertical_position.visit(
        [&](VerticalPreset preset) -> ErrorOr<void> {
            return builder.try_append([&] {
                switch (preset) {
                case VerticalPreset::Top:
                    return "top"sv;
                case VerticalPreset::Center:
                    return "center"sv;
                case VerticalPreset::Bottom:
                    return "bottom"sv;
                default:
                    VERIFY_NOT_REACHED();
                }
            }());
        },
        [&](LengthPercentage length_percentage) -> ErrorOr<void> {
            return builder.try_append(TRY(length_percentage.to_string()));
        }));
    return {};
}

ErrorOr<String> RectStyleValue::to_string() const
{
    return String::formatted("rect({} {} {} {})", m_rect.top_edge, m_rect.right_edge, m_rect.bottom_edge, m_rect.left_edge);
}

ErrorOr<String> TextDecorationStyleValue::to_string() const
{
    return String::formatted("{} {} {} {}", TRY(m_properties.line->to_string()), TRY(m_properties.thickness->to_string()), TRY(m_properties.style->to_string()), TRY(m_properties.color->to_string()));
}

ErrorOr<String> TransformationStyleValue::to_string() const
{
    StringBuilder builder;
    TRY(builder.try_append(CSS::to_string(m_properties.transform_function)));
    TRY(builder.try_append('('));
    for (size_t i = 0; i < m_properties.values.size(); ++i) {
        TRY(builder.try_append(TRY(m_properties.values[i]->to_string())));
        if (i != m_properties.values.size() - 1)
            TRY(builder.try_append(", "sv));
    }
    TRY(builder.try_append(')'));

    return builder.to_string();
}

bool TransformationStyleValue::Properties::operator==(Properties const& other) const
{
    return transform_function == other.transform_function && values.span() == other.values.span();
}

ErrorOr<String> UnresolvedStyleValue::to_string() const
{
    StringBuilder builder;
    for (auto& value : m_values)
        TRY(builder.try_append(TRY(value.to_string())));
    return builder.to_string();
}

bool UnresolvedStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    // This is a case where comparing the strings actually makes sense.
    return to_string().release_value_but_fixme_should_propagate_errors() == other.to_string().release_value_but_fixme_should_propagate_errors();
}

bool StyleValueList::Properties::operator==(Properties const& other) const
{
    return separator == other.separator && values.span() == other.values.span();
}

ErrorOr<String> StyleValueList::to_string() const
{
    auto separator = ""sv;
    switch (m_properties.separator) {
    case Separator::Space:
        separator = " "sv;
        break;
    case Separator::Comma:
        separator = ", "sv;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    StringBuilder builder;
    for (size_t i = 0; i < m_properties.values.size(); ++i) {
        TRY(builder.try_append(TRY(m_properties.values[i]->to_string())));
        if (i != m_properties.values.size() - 1)
            TRY(builder.try_append(separator));
    }
    return builder.to_string();
}


ValueComparingNonnullRefPtr<RectStyleValue> RectStyleValue::create(EdgeRect rect)
{
    return adopt_ref(*new RectStyleValue(rect));
}

Optional<CSS::Length> absolutized_length(CSS::Length const& length, CSSPixelRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, CSSPixels font_size, CSSPixels root_font_size, CSSPixels line_height, CSSPixels root_line_height)
{
    if (length.is_px())
        return {};
    if (length.is_absolute() || length.is_relative()) {
        auto px = length.to_px(viewport_rect, font_metrics, font_size, root_font_size, line_height, root_line_height);
        return CSS::Length::make_px(px);
    }
    return {};
}

ValueComparingNonnullRefPtr<StyleValue const> StyleValue::absolutized(CSSPixelRect const&, Gfx::FontPixelMetrics const&, CSSPixels, CSSPixels, CSSPixels, CSSPixels) const
{
    return *this;
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

bool calculated_style_value_contains_percentage(CalculatedStyleValue const& value)
{
    return value.contains_percentage();
}

}
