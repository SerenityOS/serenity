/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <LibGfx/Palette.h>
#include <LibWeb/CSS/Serialize.h>
#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Loader/LoadRequest.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Page/Page.h>

namespace Web::CSS {

StyleValue::StyleValue(Type type)
    : m_type(type)
{
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

ContentStyleValue const& StyleValue::as_content() const
{
    VERIFY(is_content());
    return static_cast<ContentStyleValue const&>(*this);
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

BackgroundStyleValue::BackgroundStyleValue(
    NonnullRefPtr<StyleValue> color,
    NonnullRefPtr<StyleValue> image,
    NonnullRefPtr<StyleValue> position,
    NonnullRefPtr<StyleValue> size,
    NonnullRefPtr<StyleValue> repeat,
    NonnullRefPtr<StyleValue> attachment,
    NonnullRefPtr<StyleValue> origin,
    NonnullRefPtr<StyleValue> clip)
    : StyleValue(Type::Background)
    , m_color(color)
    , m_image(image)
    , m_position(position)
    , m_size(size)
    , m_repeat(repeat)
    , m_attachment(attachment)
    , m_origin(origin)
    , m_clip(clip)
{
    auto layer_count = [](auto style_value) -> size_t {
        if (style_value->is_value_list())
            return style_value->as_value_list().size();
        else
            return 1;
    };

    m_layer_count = max(layer_count(m_image), layer_count(m_position));
    m_layer_count = max(m_layer_count, layer_count(m_size));
    m_layer_count = max(m_layer_count, layer_count(m_repeat));
    m_layer_count = max(m_layer_count, layer_count(m_attachment));
    m_layer_count = max(m_layer_count, layer_count(m_origin));
    m_layer_count = max(m_layer_count, layer_count(m_clip));

    VERIFY(!m_color->is_value_list());
}

String BackgroundStyleValue::to_string() const
{
    if (m_layer_count == 1) {
        return String::formatted("{} {} {} {} {} {} {} {}", m_color->to_string(), m_image->to_string(), m_position->to_string(), m_size->to_string(), m_repeat->to_string(), m_attachment->to_string(), m_origin->to_string(), m_clip->to_string());
    }

    auto get_layer_value_string = [](NonnullRefPtr<StyleValue> const& style_value, size_t index) {
        if (style_value->is_value_list())
            return style_value->as_value_list().value_at(index, true)->to_string();
        return style_value->to_string();
    };

    StringBuilder builder;
    for (size_t i = 0; i < m_layer_count; i++) {
        if (i)
            builder.append(", ");
        if (i == m_layer_count - 1)
            builder.appendff("{} ", m_color->to_string());
        builder.appendff("{} {} {} {} {} {} {}", get_layer_value_string(m_image, i), get_layer_value_string(m_position, i), get_layer_value_string(m_size, i), get_layer_value_string(m_repeat, i), get_layer_value_string(m_attachment, i), get_layer_value_string(m_origin, i), get_layer_value_string(m_clip, i));
    }

    return builder.to_string();
}

bool BackgroundStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    auto const& typed_other = other.as_background();
    return m_color->equals(typed_other.m_color)
        && m_image->equals(typed_other.m_image)
        && m_position->equals(typed_other.m_position)
        && m_size->equals(typed_other.m_size)
        && m_repeat->equals(typed_other.m_repeat)
        && m_attachment->equals(typed_other.m_attachment)
        && m_origin->equals(typed_other.m_origin)
        && m_clip->equals(typed_other.m_clip);
}

String BackgroundRepeatStyleValue::to_string() const
{
    return String::formatted("{} {}", CSS::to_string(m_repeat_x), CSS::to_string(m_repeat_y));
}

bool BackgroundRepeatStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    auto const& typed_other = other.as_background_repeat();
    return m_repeat_x == typed_other.m_repeat_x && m_repeat_y == typed_other.m_repeat_y;
}

String BackgroundSizeStyleValue::to_string() const
{
    return String::formatted("{} {}", m_size_x.to_string(), m_size_y.to_string());
}

bool BackgroundSizeStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    auto const& typed_other = other.as_background_size();
    return m_size_x == typed_other.m_size_x && m_size_y == typed_other.m_size_y;
}

String BorderStyleValue::to_string() const
{
    return String::formatted("{} {} {}", m_border_width->to_string(), m_border_style->to_string(), m_border_color->to_string());
}

bool BorderStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    auto const& typed_other = other.as_border();
    return m_border_width->equals(typed_other.m_border_width)
        && m_border_style->equals(typed_other.m_border_style)
        && m_border_color->equals(typed_other.m_border_color);
}

String BorderRadiusStyleValue::to_string() const
{
    if (m_horizontal_radius == m_vertical_radius)
        return m_horizontal_radius.to_string();
    return String::formatted("{} / {}", m_horizontal_radius.to_string(), m_vertical_radius.to_string());
}

bool BorderRadiusStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    auto const& typed_other = other.as_border_radius();
    return m_is_elliptical == typed_other.m_is_elliptical
        && m_horizontal_radius == typed_other.m_horizontal_radius
        && m_vertical_radius == typed_other.m_vertical_radius;
}

String BorderRadiusShorthandStyleValue::to_string() const
{
    return String::formatted("{} {} {} {} / {} {} {} {}", m_top_left->horizontal_radius().to_string(), m_top_right->horizontal_radius().to_string(), m_bottom_right->horizontal_radius().to_string(), m_bottom_left->horizontal_radius().to_string(), m_top_left->vertical_radius().to_string(), m_top_right->vertical_radius().to_string(), m_bottom_right->vertical_radius().to_string(), m_bottom_left->vertical_radius().to_string());
}

bool BorderRadiusShorthandStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    auto const& typed_other = other.as_border_radius_shorthand();
    return m_top_left->equals(typed_other.m_top_left)
        && m_top_right->equals(typed_other.m_top_right)
        && m_bottom_right->equals(typed_other.m_bottom_right)
        && m_bottom_left->equals(typed_other.m_bottom_left);
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
            if (op == SumOperation::Add)
                new_value.add(*this, layout_node, percentage_basis);
            else
                new_value.subtract(*this, layout_node, percentage_basis);

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

String CalculatedStyleValue::to_string() const
{
    return String::formatted("calc({})", m_expression->to_string());
}

bool CalculatedStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    // This is a case where comparing the strings actually makes sense.
    return to_string() == other.to_string();
}

String CalculatedStyleValue::CalcNumberValue::to_string() const
{
    return value.visit(
        [](Number const& number) { return String::number(number.value()); },
        [](NonnullOwnPtr<CalcNumberSum> const& sum) { return String::formatted("({})", sum->to_string()); });
}

String CalculatedStyleValue::CalcValue::to_string() const
{
    return value.visit(
        [](Number const& number) { return String::number(number.value()); },
        [](NonnullOwnPtr<CalcSum> const& sum) { return String::formatted("({})", sum->to_string()); },
        [](auto const& v) { return v.to_string(); });
}

String CalculatedStyleValue::CalcSum::to_string() const
{
    StringBuilder builder;
    builder.append(first_calc_product->to_string());
    for (auto const& item : zero_or_more_additional_calc_products)
        builder.append(item.to_string());
    return builder.to_string();
}

String CalculatedStyleValue::CalcNumberSum::to_string() const
{
    StringBuilder builder;
    builder.append(first_calc_number_product->to_string());
    for (auto const& item : zero_or_more_additional_calc_number_products)
        builder.append(item.to_string());
    return builder.to_string();
}

String CalculatedStyleValue::CalcProduct::to_string() const
{
    StringBuilder builder;
    builder.append(first_calc_value.to_string());
    for (auto const& item : zero_or_more_additional_calc_values)
        builder.append(item.to_string());
    return builder.to_string();
}

String CalculatedStyleValue::CalcSumPartWithOperator::to_string() const
{
    return String::formatted(" {} {}", op == SumOperation::Add ? "+"sv : "-"sv, value->to_string());
}

String CalculatedStyleValue::CalcProductPartWithOperator::to_string() const
{
    auto value_string = value.visit(
        [](CalcValue const& v) { return v.to_string(); },
        [](CalcNumberValue const& v) { return v.to_string(); });
    return String::formatted(" {} {}", op == ProductOperation::Multiply ? "*"sv : "/"sv, value_string);
}

String CalculatedStyleValue::CalcNumberProduct::to_string() const
{
    StringBuilder builder;
    builder.append(first_calc_number_value.to_string());
    for (auto const& item : zero_or_more_additional_calc_number_values)
        builder.append(item.to_string());
    return builder.to_string();
}

String CalculatedStyleValue::CalcNumberProductPartWithOperator::to_string() const
{
    return String::formatted(" {} {}", op == ProductOperation::Multiply ? "*"sv : "/"sv, value.to_string());
}

String CalculatedStyleValue::CalcNumberSumPartWithOperator::to_string() const
{
    return String::formatted(" {} {}", op == SumOperation::Add ? "+"sv : "-"sv, value->to_string());
}

Optional<Angle> CalculatedStyleValue::resolve_angle() const
{
    auto result = m_expression->resolve(nullptr, {});

    if (result.value().has<Angle>())
        return result.value().get<Angle>();
    return {};
}

Optional<AnglePercentage> CalculatedStyleValue::resolve_angle_percentage(Angle const& percentage_basis) const
{
    auto result = m_expression->resolve(nullptr, percentage_basis);

    return result.value().visit(
        [&](Angle const& angle) -> Optional<AnglePercentage> {
            return angle;
        },
        [&](Percentage const& percentage) -> Optional<AnglePercentage> {
            return percentage;
        },
        [&](auto const&) -> Optional<AnglePercentage> {
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

Optional<FrequencyPercentage> CalculatedStyleValue::resolve_frequency_percentage(Frequency const& percentage_basis) const
{
    auto result = m_expression->resolve(nullptr, percentage_basis);

    return result.value().visit(
        [&](Frequency const& frequency) -> Optional<FrequencyPercentage> {
            return frequency;
        },
        [&](Percentage const& percentage) -> Optional<FrequencyPercentage> {
            return percentage;
        },
        [&](auto const&) -> Optional<FrequencyPercentage> {
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

Optional<LengthPercentage> CalculatedStyleValue::resolve_length_percentage(Layout::Node const& layout_node, Length const& percentage_basis) const
{
    auto result = m_expression->resolve(&layout_node, percentage_basis);

    return result.value().visit(
        [&](Length const& length) -> Optional<LengthPercentage> {
            return length;
        },
        [&](Percentage const& percentage) -> Optional<LengthPercentage> {
            return percentage;
        },
        [&](auto const&) -> Optional<LengthPercentage> {
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

Optional<TimePercentage> CalculatedStyleValue::resolve_time_percentage(Time const& percentage_basis) const
{
    auto result = m_expression->resolve(nullptr, percentage_basis);

    return result.value().visit(
        [&](Time const& time) -> Optional<TimePercentage> {
            return time;
        },
        [&](Percentage const& percentage) -> Optional<TimePercentage> {
            return percentage;
        },
        [&](auto const&) -> Optional<TimePercentage> {
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
static Optional<CalculatedStyleValue::ResolvedType> resolve_sum_type(CalculatedStyleValue::ResolvedType first_type, NonnullOwnPtrVector<SumWithOperator> const& zero_or_more_additional_products)
{
    auto type = first_type;

    for (auto const& product : zero_or_more_additional_products) {
        auto maybe_product_type = product.resolved_type();
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
static Optional<CalculatedStyleValue::ResolvedType> resolve_product_type(CalculatedStyleValue::ResolvedType first_type, NonnullOwnPtrVector<ProductWithOperator> const& zero_or_more_additional_values)
{
    auto type = first_type;

    for (auto const& value : zero_or_more_additional_values) {
        auto maybe_value_type = value.resolved_type();
        if (!maybe_value_type.has_value())
            return {};
        auto value_type = maybe_value_type.value();

        if (value.op == CalculatedStyleValue::ProductOperation::Multiply) {
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
            VERIFY(value.op == CalculatedStyleValue::ProductOperation::Divide);
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
        auto additional_value = additional_product.resolve(layout_node, percentage_basis);

        if (additional_product.op == CalculatedStyleValue::SumOperation::Add)
            value.add(additional_value, layout_node, percentage_basis);
        else if (additional_product.op == CalculatedStyleValue::SumOperation::Subtract)
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
        auto additional_value = additional_product.resolve(layout_node, percentage_basis);

        if (additional_product.op == CSS::CalculatedStyleValue::SumOperation::Add)
            value.add(additional_value, layout_node, percentage_basis);
        else if (additional_product.op == CalculatedStyleValue::SumOperation::Subtract)
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
        additional_value.value.visit(
            [&](CalculatedStyleValue::CalcValue const& calc_value) {
                VERIFY(additional_value.op == CalculatedStyleValue::ProductOperation::Multiply);
                auto resolved_value = calc_value.resolve(layout_node, percentage_basis);
                value.multiply_by(resolved_value, layout_node);
            },
            [&](CalculatedStyleValue::CalcNumberValue const& calc_number_value) {
                VERIFY(additional_value.op == CalculatedStyleValue::ProductOperation::Divide);
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
        auto additional_value = additional_number_value.resolve(layout_node, percentage_basis);

        if (additional_number_value.op == CalculatedStyleValue::ProductOperation::Multiply)
            value.multiply_by(additional_value, layout_node);
        else if (additional_number_value.op == CalculatedStyleValue::ProductOperation::Divide)
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

// https://www.w3.org/TR/css-color-4/#serializing-sRGB-values
String ColorStyleValue::to_string() const
{
    // The serialized form is derived from the computed value and thus, uses either the rgb() or rgba() form
    // (depending on whether the alpha is exactly 1, or not), with lowercase letters for the function name.
    // NOTE: Since we use Gfx::Color, having an "alpha of 1" means its value is 255.
    if (m_color.alpha() == 255)
        return String::formatted("rgb({}, {}, {})", m_color.red(), m_color.green(), m_color.blue());
    return String::formatted("rgba({}, {}, {}, {})", m_color.red(), m_color.green(), m_color.blue(), (float)(m_color.alpha()) / 255.0f);
}

bool ColorStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    return m_color == other.as_color().m_color;
}

String ContentStyleValue::to_string() const
{
    if (has_alt_text())
        return String::formatted("{} / {}", m_content->to_string(), m_alt_text->to_string());
    return m_content->to_string();
}

bool ContentStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    auto const& typed_other = other.as_content();
    if (!m_content->equals(typed_other.m_content))
        return false;
    if (m_alt_text.is_null() != typed_other.m_alt_text.is_null())
        return false;
    if (!m_alt_text.is_null())
        return m_alt_text->equals(*typed_other.m_alt_text);
    return true;
}

String FlexStyleValue::to_string() const
{
    return String::formatted("{} {} {}", m_grow->to_string(), m_shrink->to_string(), m_basis->to_string());
}

bool FlexStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    auto const& typed_other = other.as_flex();
    return m_grow->equals(typed_other.m_grow)
        && m_shrink->equals(typed_other.m_shrink)
        && m_basis->equals(typed_other.m_basis);
}

String FlexFlowStyleValue::to_string() const
{
    return String::formatted("{} {}", m_flex_direction->to_string(), m_flex_wrap->to_string());
}

bool FlexFlowStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    auto const& typed_other = other.as_flex_flow();
    return m_flex_direction->equals(typed_other.m_flex_direction)
        && m_flex_wrap->equals(typed_other.m_flex_wrap);
}

String FontStyleValue::to_string() const
{
    return String::formatted("{} {} {} / {} {}", m_font_style->to_string(), m_font_weight->to_string(), m_font_size->to_string(), m_line_height->to_string(), m_font_families->to_string());
}

bool FontStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    auto const& typed_other = other.as_font();
    return m_font_style->equals(typed_other.m_font_style)
        && m_font_weight->equals(typed_other.m_font_weight)
        && m_font_size->equals(typed_other.m_font_size)
        && m_line_height->equals(typed_other.m_line_height)
        && m_font_families->equals(typed_other.m_font_families);
}

bool FrequencyStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    return m_frequency == other.as_frequency().m_frequency;
}

String IdentifierStyleValue::to_string() const
{
    return CSS::string_from_value_id(m_id);
}

bool IdentifierStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    return m_id == other.as_identifier().m_id;
}

bool IdentifierStyleValue::has_color() const
{
    switch (m_id) {
    case ValueID::Currentcolor:
    case ValueID::LibwebLink:
    case ValueID::LibwebPaletteActiveLink:
    case ValueID::LibwebPaletteActiveWindowBorder1:
    case ValueID::LibwebPaletteActiveWindowBorder2:
    case ValueID::LibwebPaletteActiveWindowTitle:
    case ValueID::LibwebPaletteBase:
    case ValueID::LibwebPaletteBaseText:
    case ValueID::LibwebPaletteButton:
    case ValueID::LibwebPaletteButtonText:
    case ValueID::LibwebPaletteDesktopBackground:
    case ValueID::LibwebPaletteFocusOutline:
    case ValueID::LibwebPaletteHighlightWindowBorder1:
    case ValueID::LibwebPaletteHighlightWindowBorder2:
    case ValueID::LibwebPaletteHighlightWindowTitle:
    case ValueID::LibwebPaletteHoverHighlight:
    case ValueID::LibwebPaletteInactiveSelection:
    case ValueID::LibwebPaletteInactiveSelectionText:
    case ValueID::LibwebPaletteInactiveWindowBorder1:
    case ValueID::LibwebPaletteInactiveWindowBorder2:
    case ValueID::LibwebPaletteInactiveWindowTitle:
    case ValueID::LibwebPaletteLink:
    case ValueID::LibwebPaletteMenuBase:
    case ValueID::LibwebPaletteMenuBaseText:
    case ValueID::LibwebPaletteMenuSelection:
    case ValueID::LibwebPaletteMenuSelectionText:
    case ValueID::LibwebPaletteMenuStripe:
    case ValueID::LibwebPaletteMovingWindowBorder1:
    case ValueID::LibwebPaletteMovingWindowBorder2:
    case ValueID::LibwebPaletteMovingWindowTitle:
    case ValueID::LibwebPaletteRubberBandBorder:
    case ValueID::LibwebPaletteRubberBandFill:
    case ValueID::LibwebPaletteRuler:
    case ValueID::LibwebPaletteRulerActiveText:
    case ValueID::LibwebPaletteRulerBorder:
    case ValueID::LibwebPaletteRulerInactiveText:
    case ValueID::LibwebPaletteSelection:
    case ValueID::LibwebPaletteSelectionText:
    case ValueID::LibwebPaletteSyntaxComment:
    case ValueID::LibwebPaletteSyntaxControlKeyword:
    case ValueID::LibwebPaletteSyntaxIdentifier:
    case ValueID::LibwebPaletteSyntaxKeyword:
    case ValueID::LibwebPaletteSyntaxNumber:
    case ValueID::LibwebPaletteSyntaxOperator:
    case ValueID::LibwebPaletteSyntaxPreprocessorStatement:
    case ValueID::LibwebPaletteSyntaxPreprocessorValue:
    case ValueID::LibwebPaletteSyntaxPunctuation:
    case ValueID::LibwebPaletteSyntaxString:
    case ValueID::LibwebPaletteSyntaxType:
    case ValueID::LibwebPaletteTextCursor:
    case ValueID::LibwebPaletteThreedHighlight:
    case ValueID::LibwebPaletteThreedShadow1:
    case ValueID::LibwebPaletteThreedShadow2:
    case ValueID::LibwebPaletteVisitedLink:
    case ValueID::LibwebPaletteWindow:
    case ValueID::LibwebPaletteWindowText:
        return true;
    default:
        return false;
    }
}

Color IdentifierStyleValue::to_color(Layout::NodeWithStyle const& node) const
{
    if (id() == CSS::ValueID::Currentcolor) {
        if (!node.has_style())
            return Color::Black;
        return node.computed_values().color();
    }

    auto& document = node.document();
    if (id() == CSS::ValueID::LibwebLink)
        return document.link_color();

    if (!document.page())
        return {};

    auto palette = document.page()->palette();
    switch (id()) {
    case CSS::ValueID::LibwebPaletteDesktopBackground:
        return palette.color(ColorRole::DesktopBackground);
    case CSS::ValueID::LibwebPaletteActiveWindowBorder1:
        return palette.color(ColorRole::ActiveWindowBorder1);
    case CSS::ValueID::LibwebPaletteActiveWindowBorder2:
        return palette.color(ColorRole::ActiveWindowBorder2);
    case CSS::ValueID::LibwebPaletteActiveWindowTitle:
        return palette.color(ColorRole::ActiveWindowTitle);
    case CSS::ValueID::LibwebPaletteInactiveWindowBorder1:
        return palette.color(ColorRole::InactiveWindowBorder1);
    case CSS::ValueID::LibwebPaletteInactiveWindowBorder2:
        return palette.color(ColorRole::InactiveWindowBorder2);
    case CSS::ValueID::LibwebPaletteInactiveWindowTitle:
        return palette.color(ColorRole::InactiveWindowTitle);
    case CSS::ValueID::LibwebPaletteMovingWindowBorder1:
        return palette.color(ColorRole::MovingWindowBorder1);
    case CSS::ValueID::LibwebPaletteMovingWindowBorder2:
        return palette.color(ColorRole::MovingWindowBorder2);
    case CSS::ValueID::LibwebPaletteMovingWindowTitle:
        return palette.color(ColorRole::MovingWindowTitle);
    case CSS::ValueID::LibwebPaletteHighlightWindowBorder1:
        return palette.color(ColorRole::HighlightWindowBorder1);
    case CSS::ValueID::LibwebPaletteHighlightWindowBorder2:
        return palette.color(ColorRole::HighlightWindowBorder2);
    case CSS::ValueID::LibwebPaletteHighlightWindowTitle:
        return palette.color(ColorRole::HighlightWindowTitle);
    case CSS::ValueID::LibwebPaletteMenuStripe:
        return palette.color(ColorRole::MenuStripe);
    case CSS::ValueID::LibwebPaletteMenuBase:
        return palette.color(ColorRole::MenuBase);
    case CSS::ValueID::LibwebPaletteMenuBaseText:
        return palette.color(ColorRole::MenuBaseText);
    case CSS::ValueID::LibwebPaletteMenuSelection:
        return palette.color(ColorRole::MenuSelection);
    case CSS::ValueID::LibwebPaletteMenuSelectionText:
        return palette.color(ColorRole::MenuSelectionText);
    case CSS::ValueID::LibwebPaletteWindow:
        return palette.color(ColorRole::Window);
    case CSS::ValueID::LibwebPaletteWindowText:
        return palette.color(ColorRole::WindowText);
    case CSS::ValueID::LibwebPaletteButton:
        return palette.color(ColorRole::Button);
    case CSS::ValueID::LibwebPaletteButtonText:
        return palette.color(ColorRole::ButtonText);
    case CSS::ValueID::LibwebPaletteBase:
        return palette.color(ColorRole::Base);
    case CSS::ValueID::LibwebPaletteBaseText:
        return palette.color(ColorRole::BaseText);
    case CSS::ValueID::LibwebPaletteThreedHighlight:
        return palette.color(ColorRole::ThreedHighlight);
    case CSS::ValueID::LibwebPaletteThreedShadow1:
        return palette.color(ColorRole::ThreedShadow1);
    case CSS::ValueID::LibwebPaletteThreedShadow2:
        return palette.color(ColorRole::ThreedShadow2);
    case CSS::ValueID::LibwebPaletteHoverHighlight:
        return palette.color(ColorRole::HoverHighlight);
    case CSS::ValueID::LibwebPaletteSelection:
        return palette.color(ColorRole::Selection);
    case CSS::ValueID::LibwebPaletteSelectionText:
        return palette.color(ColorRole::SelectionText);
    case CSS::ValueID::LibwebPaletteInactiveSelection:
        return palette.color(ColorRole::InactiveSelection);
    case CSS::ValueID::LibwebPaletteInactiveSelectionText:
        return palette.color(ColorRole::InactiveSelectionText);
    case CSS::ValueID::LibwebPaletteRubberBandFill:
        return palette.color(ColorRole::RubberBandFill);
    case CSS::ValueID::LibwebPaletteRubberBandBorder:
        return palette.color(ColorRole::RubberBandBorder);
    case CSS::ValueID::LibwebPaletteLink:
        return palette.color(ColorRole::Link);
    case CSS::ValueID::LibwebPaletteActiveLink:
        return palette.color(ColorRole::ActiveLink);
    case CSS::ValueID::LibwebPaletteVisitedLink:
        return palette.color(ColorRole::VisitedLink);
    case CSS::ValueID::LibwebPaletteRuler:
        return palette.color(ColorRole::Ruler);
    case CSS::ValueID::LibwebPaletteRulerBorder:
        return palette.color(ColorRole::RulerBorder);
    case CSS::ValueID::LibwebPaletteRulerActiveText:
        return palette.color(ColorRole::RulerActiveText);
    case CSS::ValueID::LibwebPaletteRulerInactiveText:
        return palette.color(ColorRole::RulerInactiveText);
    case CSS::ValueID::LibwebPaletteTextCursor:
        return palette.color(ColorRole::TextCursor);
    case CSS::ValueID::LibwebPaletteFocusOutline:
        return palette.color(ColorRole::FocusOutline);
    case CSS::ValueID::LibwebPaletteSyntaxComment:
        return palette.color(ColorRole::SyntaxComment);
    case CSS::ValueID::LibwebPaletteSyntaxNumber:
        return palette.color(ColorRole::SyntaxNumber);
    case CSS::ValueID::LibwebPaletteSyntaxString:
        return palette.color(ColorRole::SyntaxString);
    case CSS::ValueID::LibwebPaletteSyntaxType:
        return palette.color(ColorRole::SyntaxType);
    case CSS::ValueID::LibwebPaletteSyntaxPunctuation:
        return palette.color(ColorRole::SyntaxPunctuation);
    case CSS::ValueID::LibwebPaletteSyntaxOperator:
        return palette.color(ColorRole::SyntaxOperator);
    case CSS::ValueID::LibwebPaletteSyntaxKeyword:
        return palette.color(ColorRole::SyntaxKeyword);
    case CSS::ValueID::LibwebPaletteSyntaxControlKeyword:
        return palette.color(ColorRole::SyntaxControlKeyword);
    case CSS::ValueID::LibwebPaletteSyntaxIdentifier:
        return palette.color(ColorRole::SyntaxIdentifier);
    case CSS::ValueID::LibwebPaletteSyntaxPreprocessorStatement:
        return palette.color(ColorRole::SyntaxPreprocessorStatement);
    case CSS::ValueID::LibwebPaletteSyntaxPreprocessorValue:
        return palette.color(ColorRole::SyntaxPreprocessorValue);
    default:
        return {};
    }
}

ImageStyleValue::ImageStyleValue(AK::URL const& url)
    : StyleValue(Type::Image)
    , m_url(url)
{
}

void ImageStyleValue::load_bitmap(DOM::Document& document)
{
    if (m_bitmap)
        return;

    m_document = &document;
    auto request = LoadRequest::create_for_url_on_page(m_url, document.page());
    set_resource(ResourceLoader::the().load_resource(Resource::Type::Image, request));
}

void ImageStyleValue::resource_did_load()
{
    if (!m_document)
        return;
    m_bitmap = resource()->bitmap();
    // FIXME: Do less than a full repaint if possible?
    if (m_document && m_document->browsing_context())
        m_document->browsing_context()->set_needs_display({});
}

String ImageStyleValue::to_string() const
{
    return serialize_a_url(m_url.to_string());
}

bool ImageStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    return m_url == other.as_image().m_url;
}

bool InheritStyleValue::equals(StyleValue const& other) const
{
    return type() == other.type();
}

bool InitialStyleValue::equals(StyleValue const& other) const
{
    return type() == other.type();
}

bool LengthStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    return m_length == other.as_length().m_length;
}

String ListStyleStyleValue::to_string() const
{
    return String::formatted("{} {} {}", m_position->to_string(), m_image->to_string(), m_style_type->to_string());
}

bool ListStyleStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    auto const& typed_other = other.as_list_style();
    return m_position->equals(typed_other.m_position)
        && m_image->equals(typed_other.m_image)
        && m_style_type->equals(typed_other.m_style_type);
}

String NumericStyleValue::to_string() const
{
    return m_value.visit(
        [](float value) {
            return String::formatted("{}", value);
        },
        [](i64 value) {
            return String::formatted("{}", value);
        });
}

bool NumericStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    if (has_integer() != other.has_integer())
        return false;
    if (has_integer())
        return m_value.get<i64>() == other.as_numeric().m_value.get<i64>();
    return m_value.get<float>() == other.as_numeric().m_value.get<float>();
}

String OverflowStyleValue::to_string() const
{
    return String::formatted("{} {}", m_overflow_x->to_string(), m_overflow_y->to_string());
}

bool OverflowStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    auto const& typed_other = other.as_overflow();
    return m_overflow_x->equals(typed_other.m_overflow_x)
        && m_overflow_y->equals(typed_other.m_overflow_y);
}

String PercentageStyleValue::to_string() const
{
    return m_percentage.to_string();
}

bool PercentageStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    return m_percentage == other.as_percentage().m_percentage;
}

String PositionStyleValue::to_string() const
{
    auto to_string = [](PositionEdge edge) {
        switch (edge) {
        case PositionEdge::Left:
            return "left";
        case PositionEdge::Right:
            return "right";
        case PositionEdge::Top:
            return "top";
        case PositionEdge::Bottom:
            return "bottom";
        }
        VERIFY_NOT_REACHED();
    };

    return String::formatted("{} {} {} {}", to_string(m_edge_x), m_offset_x.to_string(), to_string(m_edge_y), m_offset_y.to_string());
}

bool PositionStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    auto const& typed_other = other.as_position();
    return m_edge_x == typed_other.m_edge_x
        && m_offset_x == typed_other.m_offset_x
        && m_edge_y == typed_other.m_edge_y
        && m_offset_y == typed_other.m_offset_y;
}

bool ResolutionStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    return m_resolution == other.as_resolution().m_resolution;
}

String ShadowStyleValue::to_string() const
{
    StringBuilder builder;
    builder.appendff("{} {} {} {} {}", m_color.to_string(), m_offset_x.to_string(), m_offset_y.to_string(), m_blur_radius.to_string(), m_spread_distance.to_string());
    if (m_placement == ShadowPlacement::Inner)
        builder.append(" inset");
    return builder.to_string();
}

bool ShadowStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    auto const& typed_other = other.as_shadow();
    return m_color == typed_other.m_color
        && m_offset_x == typed_other.m_offset_x
        && m_offset_y == typed_other.m_offset_y
        && m_blur_radius == typed_other.m_blur_radius
        && m_spread_distance == typed_other.m_spread_distance
        && m_placement == typed_other.m_placement;
}

bool StringStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    return m_string == other.as_string().m_string;
}

String TextDecorationStyleValue::to_string() const
{
    return String::formatted("{} {} {} {}", m_line->to_string(), m_thickness->to_string(), m_style->to_string(), m_color->to_string());
}

bool TextDecorationStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    auto const& typed_other = other.as_text_decoration();
    return m_line->equals(typed_other.m_line)
        && m_thickness->equals(typed_other.m_thickness)
        && m_style->equals(typed_other.m_style)
        && m_color->equals(typed_other.m_color);
}

bool TimeStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    return m_time == other.as_time().m_time;
}

String TransformationStyleValue::to_string() const
{
    StringBuilder builder;
    builder.append(CSS::to_string(m_transform_function));
    builder.append('(');
    builder.join(", ", m_values);
    builder.append(')');

    return builder.to_string();
}

bool TransformationStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    auto const& typed_other = other.as_transformation();
    if (m_transform_function != typed_other.m_transform_function)
        return false;
    if (m_values.size() != typed_other.m_values.size())
        return false;
    for (size_t i = 0; i < m_values.size(); ++i) {
        if (!m_values[i].equals(typed_other.m_values[i]))
            return false;
    }
    return true;
}

String UnresolvedStyleValue::to_string() const
{
    StringBuilder builder;
    for (auto& value : m_values)
        builder.append(value.to_string());
    return builder.to_string();
}

bool UnresolvedStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    // This is a case where comparing the strings actually makes sense.
    return to_string() == other.to_string();
}

bool UnsetStyleValue::equals(StyleValue const& other) const
{
    return type() == other.type();
}

String StyleValueList::to_string() const
{
    String separator = "";
    switch (m_separator) {
    case Separator::Space:
        separator = " ";
        break;
    case Separator::Comma:
        separator = ", ";
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    return String::join(separator, m_values);
}

bool StyleValueList::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    auto const& typed_other = other.as_value_list();
    if (m_separator != typed_other.m_separator)
        return false;
    if (m_values.size() != typed_other.m_values.size())
        return false;
    for (size_t i = 0; i < m_values.size(); ++i) {
        if (!m_values[i].equals(typed_other.m_values[i]))
            return false;
    }
    return true;
}

NonnullRefPtr<ColorStyleValue> ColorStyleValue::create(Color color)
{
    if (color.value() == 0) {
        static auto transparent = adopt_ref(*new ColorStyleValue(color));
        return transparent;
    }

    if (color == Color::from_rgb(0x000000)) {
        static auto black = adopt_ref(*new ColorStyleValue(color));
        return black;
    }

    if (color == Color::from_rgb(0xffffff)) {
        static auto white = adopt_ref(*new ColorStyleValue(color));
        return white;
    }

    return adopt_ref(*new ColorStyleValue(color));
}

NonnullRefPtr<LengthStyleValue> LengthStyleValue::create(Length const& length)
{
    if (length.is_auto()) {
        static auto value = adopt_ref(*new LengthStyleValue(CSS::Length::make_auto()));
        return value;
    }
    if (length.is_px()) {
        if (length.raw_value() == 0) {
            static auto value = adopt_ref(*new LengthStyleValue(CSS::Length::make_px(0)));
            return value;
        }
        if (length.raw_value() == 1) {
            static auto value = adopt_ref(*new LengthStyleValue(CSS::Length::make_px(1)));
            return value;
        }
    }
    return adopt_ref(*new LengthStyleValue(length));
}

static Optional<CSS::Length> absolutized_length(CSS::Length const& length, Gfx::IntRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, float font_size, float root_font_size)
{
    if (length.is_px())
        return {};
    if (length.is_absolute() || length.is_relative()) {
        auto px = length.to_px(viewport_rect, font_metrics, font_size, root_font_size);
        return CSS::Length::make_px(px);
    }
    return {};
}

NonnullRefPtr<StyleValue> StyleValue::absolutized(Gfx::IntRect const&, Gfx::FontPixelMetrics const&, float, float) const
{
    return *this;
}

NonnullRefPtr<StyleValue> LengthStyleValue::absolutized(Gfx::IntRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, float font_size, float root_font_size) const
{
    if (auto length = absolutized_length(m_length, viewport_rect, font_metrics, font_size, root_font_size); length.has_value())
        return LengthStyleValue::create(length.release_value());
    return *this;
}

NonnullRefPtr<StyleValue> ShadowStyleValue::absolutized(Gfx::IntRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, float font_size, float root_font_size) const
{
    auto absolutized_offset_x = absolutized_length(m_offset_x, viewport_rect, font_metrics, font_size, root_font_size).value_or(m_offset_x);
    auto absolutized_offset_y = absolutized_length(m_offset_y, viewport_rect, font_metrics, font_size, root_font_size).value_or(m_offset_y);
    auto absolutized_blur_radius = absolutized_length(m_blur_radius, viewport_rect, font_metrics, font_size, root_font_size).value_or(m_blur_radius);
    auto absolutized_spread_distance = absolutized_length(m_spread_distance, viewport_rect, font_metrics, font_size, root_font_size).value_or(m_spread_distance);
    return ShadowStyleValue::create(m_color, absolutized_offset_x, absolutized_offset_y, absolutized_blur_radius, absolutized_spread_distance, m_placement);
}

NonnullRefPtr<StyleValue> BorderRadiusStyleValue::absolutized(Gfx::IntRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, float font_size, float root_font_size) const
{
    if (m_horizontal_radius.is_percentage() && m_vertical_radius.is_percentage())
        return *this;
    auto absolutized_horizontal_radius = m_horizontal_radius;
    auto absolutized_vertical_radius = m_vertical_radius;
    if (!m_horizontal_radius.is_percentage())
        absolutized_horizontal_radius = absolutized_length(m_horizontal_radius.length(), viewport_rect, font_metrics, font_size, root_font_size).value_or(m_horizontal_radius.length());
    if (!m_vertical_radius.is_percentage())
        absolutized_vertical_radius = absolutized_length(m_vertical_radius.length(), viewport_rect, font_metrics, font_size, root_font_size).value_or(m_vertical_radius.length());
    return BorderRadiusStyleValue::create(absolutized_horizontal_radius, absolutized_vertical_radius);
}

}
