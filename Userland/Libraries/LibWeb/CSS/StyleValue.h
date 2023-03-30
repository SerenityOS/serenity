/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/DeprecatedString.h>
#include <AK/GenericShorthands.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/URL.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <LibGfx/Painter.h>
#include <LibWeb/CSS/Angle.h>
#include <LibWeb/CSS/CalculatedOr.h>
#include <LibWeb/CSS/Enums.h>
#include <LibWeb/CSS/Frequency.h>
#include <LibWeb/CSS/Length.h>
#include <LibWeb/CSS/Number.h>
#include <LibWeb/CSS/Percentage.h>
#include <LibWeb/CSS/Resolution.h>
#include <LibWeb/CSS/Time.h>
#include <LibWeb/CSS/ValueID.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

// FIXME: Named PositionValue to avoid conflicts with enums, but this represents a <position>
struct PositionValue {
    enum class HorizontalPreset {
        Left,
        Center,
        Right
    };

    enum class VerticalPreset {
        Top,
        Center,
        Bottom
    };

    enum class HorizontalEdge {
        Left,
        Right
    };

    enum class VerticalEdge {
        Top,
        Bottom
    };

    static PositionValue center()
    {
        return PositionValue { HorizontalPreset::Center, VerticalPreset::Center };
    }

    Variant<HorizontalPreset, LengthPercentage> horizontal_position { HorizontalPreset::Left };
    Variant<VerticalPreset, LengthPercentage> vertical_position { VerticalPreset::Top };
    HorizontalEdge x_relative_to { HorizontalEdge::Left };
    VerticalEdge y_relative_to { VerticalEdge::Top };

    CSSPixelPoint resolved(Layout::Node const& node, CSSPixelRect const& rect) const;
    ErrorOr<void> serialize(StringBuilder&) const;
    bool operator==(PositionValue const&) const = default;
};

struct EdgeRect {
    Length top_edge;
    Length right_edge;
    Length bottom_edge;
    Length left_edge;
    Gfx::FloatRect resolved(Layout::Node const&, Gfx::FloatRect) const;
    bool operator==(EdgeRect const&) const = default;
};

// FIXME: Find a better place for this helper.
inline Gfx::Painter::ScalingMode to_gfx_scaling_mode(CSS::ImageRendering css_value)
{
    switch (css_value) {
    case CSS::ImageRendering::Auto:
    case CSS::ImageRendering::HighQuality:
    case CSS::ImageRendering::Smooth:
        return Gfx::Painter::ScalingMode::BilinearBlend;
    case CSS::ImageRendering::CrispEdges:
        return Gfx::Painter::ScalingMode::NearestNeighbor;
    case CSS::ImageRendering::Pixelated:
        return Gfx::Painter::ScalingMode::SmoothPixels;
    }
    VERIFY_NOT_REACHED();
}

template<typename T>
struct ValueComparingNonnullRefPtr : public NonnullRefPtr<T> {
    using NonnullRefPtr<T>::NonnullRefPtr;

    ValueComparingNonnullRefPtr(NonnullRefPtr<T> const& other)
        : NonnullRefPtr<T>(other)
    {
    }

    ValueComparingNonnullRefPtr(NonnullRefPtr<T>&& other)
        : NonnullRefPtr<T>(move(other))
    {
    }

    bool operator==(ValueComparingNonnullRefPtr const& other) const
    {
        return this->ptr() == other.ptr() || this->ptr()->equals(*other);
    }

private:
    using NonnullRefPtr<T>::operator==;
};

template<typename T>
struct ValueComparingRefPtr : public RefPtr<T> {
    using RefPtr<T>::RefPtr;

    ValueComparingRefPtr(RefPtr<T> const& other)
        : RefPtr<T>(other)
    {
    }

    ValueComparingRefPtr(RefPtr<T>&& other)
        : RefPtr<T>(move(other))
    {
    }

    template<typename U>
    bool operator==(ValueComparingNonnullRefPtr<U> const& other) const
    {
        return this->ptr() == other.ptr() || (this->ptr() && this->ptr()->equals(*other));
    }

    bool operator==(ValueComparingRefPtr const& other) const
    {
        return this->ptr() == other.ptr() || (this->ptr() && other.ptr() && this->ptr()->equals(*other));
    }

private:
    using RefPtr<T>::operator==;
};

using StyleValueVector = Vector<ValueComparingNonnullRefPtr<StyleValue const>>;

Optional<CSS::Length> absolutized_length(CSS::Length const&, CSSPixelRect const& viewport_rect, Gfx::FontPixelMetrics const&, CSSPixels font_size, CSSPixels root_font_size, CSSPixels line_height, CSSPixels root_line_height);

class StyleValue : public RefCounted<StyleValue> {
public:
    virtual ~StyleValue() = default;

    enum class Type {
        Angle,
        Background,
        BackgroundRepeat,
        BackgroundSize,
        Border,
        BorderRadius,
        BorderRadiusShorthand,
        Calculated,
        Color,
        ConicGradient,
        Content,
        FilterValueList,
        Flex,
        FlexFlow,
        Font,
        Frequency,
        GridAreaShorthand,
        GridTemplateArea,
        GridTrackPlacement,
        GridTrackPlacementShorthand,
        GridTrackSizeList,
        Identifier,
        Image,
        Inherit,
        Initial,
        Invalid,
        Length,
        LinearGradient,
        ListStyle,
        Numeric,
        Overflow,
        Percentage,
        Position,
        RadialGradient,
        Rect,
        Resolution,
        Shadow,
        String,
        TextDecoration,
        Time,
        Transformation,
        Unresolved,
        Unset,
        ValueList
    };

    Type type() const { return m_type; }

    bool is_abstract_image() const { return AK::first_is_one_of(type(), Type::Image, Type::LinearGradient, Type::ConicGradient, Type::RadialGradient); }
    bool is_angle() const { return type() == Type::Angle; }
    bool is_background() const { return type() == Type::Background; }
    bool is_background_repeat() const { return type() == Type::BackgroundRepeat; }
    bool is_background_size() const { return type() == Type::BackgroundSize; }
    bool is_border() const { return type() == Type::Border; }
    bool is_border_radius() const { return type() == Type::BorderRadius; }
    bool is_border_radius_shorthand() const { return type() == Type::BorderRadiusShorthand; }
    bool is_calculated() const { return type() == Type::Calculated; }
    bool is_color() const { return type() == Type::Color; }
    bool is_conic_gradient() const { return type() == Type::ConicGradient; }
    bool is_content() const { return type() == Type::Content; }
    bool is_filter_value_list() const { return type() == Type::FilterValueList; }
    bool is_flex() const { return type() == Type::Flex; }
    bool is_flex_flow() const { return type() == Type::FlexFlow; }
    bool is_font() const { return type() == Type::Font; }
    bool is_frequency() const { return type() == Type::Frequency; }
    bool is_grid_area_shorthand() const { return type() == Type::GridAreaShorthand; }
    bool is_grid_template_area() const { return type() == Type::GridTemplateArea; }
    bool is_grid_track_placement() const { return type() == Type::GridTrackPlacement; }
    bool is_grid_track_placement_shorthand() const { return type() == Type::GridTrackPlacementShorthand; }
    bool is_grid_track_size_list() const { return type() == Type::GridTrackSizeList; }
    bool is_identifier() const { return type() == Type::Identifier; }
    bool is_image() const { return type() == Type::Image; }
    bool is_inherit() const { return type() == Type::Inherit; }
    bool is_initial() const { return type() == Type::Initial; }
    bool is_length() const { return type() == Type::Length; }
    bool is_linear_gradient() const { return type() == Type::LinearGradient; }
    bool is_list_style() const { return type() == Type::ListStyle; }
    bool is_numeric() const { return type() == Type::Numeric; }
    bool is_overflow() const { return type() == Type::Overflow; }
    bool is_percentage() const { return type() == Type::Percentage; }
    bool is_position() const { return type() == Type::Position; }
    bool is_radial_gradient() const { return type() == Type::RadialGradient; }
    bool is_rect() const { return type() == Type::Rect; }
    bool is_resolution() const { return type() == Type::Resolution; }
    bool is_shadow() const { return type() == Type::Shadow; }
    bool is_string() const { return type() == Type::String; }
    bool is_text_decoration() const { return type() == Type::TextDecoration; }
    bool is_time() const { return type() == Type::Time; }
    bool is_transformation() const { return type() == Type::Transformation; }
    bool is_unresolved() const { return type() == Type::Unresolved; }
    bool is_unset() const { return type() == Type::Unset; }
    bool is_value_list() const { return type() == Type::ValueList; }

    bool is_builtin() const { return is_inherit() || is_initial() || is_unset(); }

    AbstractImageStyleValue const& as_abstract_image() const;
    AngleStyleValue const& as_angle() const;
    BackgroundStyleValue const& as_background() const;
    BackgroundRepeatStyleValue const& as_background_repeat() const;
    BackgroundSizeStyleValue const& as_background_size() const;
    BorderRadiusStyleValue const& as_border_radius() const;
    BorderRadiusShorthandStyleValue const& as_border_radius_shorthand() const;
    BorderStyleValue const& as_border() const;
    CalculatedStyleValue const& as_calculated() const;
    ColorStyleValue const& as_color() const;
    ConicGradientStyleValue const& as_conic_gradient() const;
    ContentStyleValue const& as_content() const;
    FilterValueListStyleValue const& as_filter_value_list() const;
    FlexFlowStyleValue const& as_flex_flow() const;
    FlexStyleValue const& as_flex() const;
    FontStyleValue const& as_font() const;
    FrequencyStyleValue const& as_frequency() const;
    GridAreaShorthandStyleValue const& as_grid_area_shorthand() const;
    GridTemplateAreaStyleValue const& as_grid_template_area() const;
    GridTrackPlacementShorthandStyleValue const& as_grid_track_placement_shorthand() const;
    GridTrackPlacementStyleValue const& as_grid_track_placement() const;
    GridTrackSizeStyleValue const& as_grid_track_size_list() const;
    IdentifierStyleValue const& as_identifier() const;
    ImageStyleValue const& as_image() const;
    InheritStyleValue const& as_inherit() const;
    InitialStyleValue const& as_initial() const;
    LengthStyleValue const& as_length() const;
    LinearGradientStyleValue const& as_linear_gradient() const;
    ListStyleStyleValue const& as_list_style() const;
    NumericStyleValue const& as_numeric() const;
    OverflowStyleValue const& as_overflow() const;
    PercentageStyleValue const& as_percentage() const;
    PositionStyleValue const& as_position() const;
    RadialGradientStyleValue const& as_radial_gradient() const;
    RectStyleValue const& as_rect() const;
    ResolutionStyleValue const& as_resolution() const;
    ShadowStyleValue const& as_shadow() const;
    StringStyleValue const& as_string() const;
    TextDecorationStyleValue const& as_text_decoration() const;
    TimeStyleValue const& as_time() const;
    TransformationStyleValue const& as_transformation() const;
    UnresolvedStyleValue const& as_unresolved() const;
    UnsetStyleValue const& as_unset() const;
    StyleValueList const& as_value_list() const;

    AbstractImageStyleValue& as_abstract_image() { return const_cast<AbstractImageStyleValue&>(const_cast<StyleValue const&>(*this).as_abstract_image()); }
    AngleStyleValue& as_angle() { return const_cast<AngleStyleValue&>(const_cast<StyleValue const&>(*this).as_angle()); }
    BackgroundStyleValue& as_background() { return const_cast<BackgroundStyleValue&>(const_cast<StyleValue const&>(*this).as_background()); }
    BackgroundRepeatStyleValue& as_background_repeat() { return const_cast<BackgroundRepeatStyleValue&>(const_cast<StyleValue const&>(*this).as_background_repeat()); }
    BackgroundSizeStyleValue& as_background_size() { return const_cast<BackgroundSizeStyleValue&>(const_cast<StyleValue const&>(*this).as_background_size()); }
    BorderRadiusStyleValue& as_border_radius() { return const_cast<BorderRadiusStyleValue&>(const_cast<StyleValue const&>(*this).as_border_radius()); }
    BorderRadiusShorthandStyleValue& as_border_radius_shorthand() { return const_cast<BorderRadiusShorthandStyleValue&>(const_cast<StyleValue const&>(*this).as_border_radius_shorthand()); }
    BorderStyleValue& as_border() { return const_cast<BorderStyleValue&>(const_cast<StyleValue const&>(*this).as_border()); }
    CalculatedStyleValue& as_calculated() { return const_cast<CalculatedStyleValue&>(const_cast<StyleValue const&>(*this).as_calculated()); }
    ColorStyleValue& as_color() { return const_cast<ColorStyleValue&>(const_cast<StyleValue const&>(*this).as_color()); }
    ConicGradientStyleValue& as_conic_gradient() { return const_cast<ConicGradientStyleValue&>(const_cast<StyleValue const&>(*this).as_conic_gradient()); }
    ContentStyleValue& as_content() { return const_cast<ContentStyleValue&>(const_cast<StyleValue const&>(*this).as_content()); }
    FilterValueListStyleValue& as_filter_value_list() { return const_cast<FilterValueListStyleValue&>(const_cast<StyleValue const&>(*this).as_filter_value_list()); }
    FlexFlowStyleValue& as_flex_flow() { return const_cast<FlexFlowStyleValue&>(const_cast<StyleValue const&>(*this).as_flex_flow()); }
    FlexStyleValue& as_flex() { return const_cast<FlexStyleValue&>(const_cast<StyleValue const&>(*this).as_flex()); }
    FontStyleValue& as_font() { return const_cast<FontStyleValue&>(const_cast<StyleValue const&>(*this).as_font()); }
    FrequencyStyleValue& as_frequency() { return const_cast<FrequencyStyleValue&>(const_cast<StyleValue const&>(*this).as_frequency()); }
    GridAreaShorthandStyleValue& as_grid_area_shorthand() { return const_cast<GridAreaShorthandStyleValue&>(const_cast<StyleValue const&>(*this).as_grid_area_shorthand()); }
    GridTemplateAreaStyleValue& as_grid_template_area() { return const_cast<GridTemplateAreaStyleValue&>(const_cast<StyleValue const&>(*this).as_grid_template_area()); }
    GridTrackPlacementShorthandStyleValue& as_grid_track_placement_shorthand() { return const_cast<GridTrackPlacementShorthandStyleValue&>(const_cast<StyleValue const&>(*this).as_grid_track_placement_shorthand()); }
    GridTrackPlacementStyleValue& as_grid_track_placement() { return const_cast<GridTrackPlacementStyleValue&>(const_cast<StyleValue const&>(*this).as_grid_track_placement()); }
    GridTrackSizeStyleValue& as_grid_track_size_list() { return const_cast<GridTrackSizeStyleValue&>(const_cast<StyleValue const&>(*this).as_grid_track_size_list()); }
    IdentifierStyleValue& as_identifier() { return const_cast<IdentifierStyleValue&>(const_cast<StyleValue const&>(*this).as_identifier()); }
    ImageStyleValue& as_image() { return const_cast<ImageStyleValue&>(const_cast<StyleValue const&>(*this).as_image()); }
    InheritStyleValue& as_inherit() { return const_cast<InheritStyleValue&>(const_cast<StyleValue const&>(*this).as_inherit()); }
    InitialStyleValue& as_initial() { return const_cast<InitialStyleValue&>(const_cast<StyleValue const&>(*this).as_initial()); }
    LengthStyleValue& as_length() { return const_cast<LengthStyleValue&>(const_cast<StyleValue const&>(*this).as_length()); }
    LinearGradientStyleValue& as_linear_gradient() { return const_cast<LinearGradientStyleValue&>(const_cast<StyleValue const&>(*this).as_linear_gradient()); }
    ListStyleStyleValue& as_list_style() { return const_cast<ListStyleStyleValue&>(const_cast<StyleValue const&>(*this).as_list_style()); }
    NumericStyleValue& as_numeric() { return const_cast<NumericStyleValue&>(const_cast<StyleValue const&>(*this).as_numeric()); }
    OverflowStyleValue& as_overflow() { return const_cast<OverflowStyleValue&>(const_cast<StyleValue const&>(*this).as_overflow()); }
    PercentageStyleValue& as_percentage() { return const_cast<PercentageStyleValue&>(const_cast<StyleValue const&>(*this).as_percentage()); }
    PositionStyleValue& as_position() { return const_cast<PositionStyleValue&>(const_cast<StyleValue const&>(*this).as_position()); }
    RadialGradientStyleValue& as_radial_gradient() { return const_cast<RadialGradientStyleValue&>(const_cast<StyleValue const&>(*this).as_radial_gradient()); }
    RectStyleValue& as_rect() { return const_cast<RectStyleValue&>(const_cast<StyleValue const&>(*this).as_rect()); }
    ResolutionStyleValue& as_resolution() { return const_cast<ResolutionStyleValue&>(const_cast<StyleValue const&>(*this).as_resolution()); }
    ShadowStyleValue& as_shadow() { return const_cast<ShadowStyleValue&>(const_cast<StyleValue const&>(*this).as_shadow()); }
    StringStyleValue& as_string() { return const_cast<StringStyleValue&>(const_cast<StyleValue const&>(*this).as_string()); }
    TextDecorationStyleValue& as_text_decoration() { return const_cast<TextDecorationStyleValue&>(const_cast<StyleValue const&>(*this).as_text_decoration()); }
    TimeStyleValue& as_time() { return const_cast<TimeStyleValue&>(const_cast<StyleValue const&>(*this).as_time()); }
    TransformationStyleValue& as_transformation() { return const_cast<TransformationStyleValue&>(const_cast<StyleValue const&>(*this).as_transformation()); }
    UnresolvedStyleValue& as_unresolved() { return const_cast<UnresolvedStyleValue&>(const_cast<StyleValue const&>(*this).as_unresolved()); }
    UnsetStyleValue& as_unset() { return const_cast<UnsetStyleValue&>(const_cast<StyleValue const&>(*this).as_unset()); }
    StyleValueList& as_value_list() { return const_cast<StyleValueList&>(const_cast<StyleValue const&>(*this).as_value_list()); }

    virtual bool has_auto() const { return false; }
    virtual bool has_color() const { return false; }
    virtual bool has_identifier() const { return false; }
    virtual bool has_length() const { return false; }
    virtual bool has_rect() const { return false; }
    virtual bool has_number() const { return false; }
    virtual bool has_integer() const { return false; }

    virtual ValueComparingNonnullRefPtr<StyleValue const> absolutized(CSSPixelRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, CSSPixels font_size, CSSPixels root_font_size, CSSPixels line_height, CSSPixels root_line_height) const;

    virtual Color to_color(Layout::NodeWithStyle const&) const { return {}; }
    virtual EdgeRect to_rect() const { VERIFY_NOT_REACHED(); }
    virtual CSS::ValueID to_identifier() const { return ValueID::Invalid; }
    virtual Length to_length() const { VERIFY_NOT_REACHED(); }
    virtual float to_number() const { return 0; }
    virtual float to_integer() const { return 0; }
    virtual ErrorOr<String> to_string() const = 0;

    virtual bool equals(StyleValue const& other) const = 0;

    bool operator==(StyleValue const& other) const
    {
        return this->equals(other);
    }

protected:
    explicit StyleValue(Type);

private:
    Type m_type { Type::Invalid };
};

template<typename T>
struct StyleValueWithDefaultOperators : public StyleValue {
    using StyleValue::StyleValue;

    virtual bool equals(StyleValue const& other) const override
    {
        if (type() != other.type())
            return false;
        auto const& typed_other = static_cast<T const&>(other);
        return static_cast<T const&>(*this).properties_equal(typed_other);
    }
};

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

template<>
struct AK::Formatter<Web::CSS::StyleValue> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSS::StyleValue const& style_value)
    {
        return Formatter<StringView>::format(builder, TRY(style_value.to_string()));
    }
};
