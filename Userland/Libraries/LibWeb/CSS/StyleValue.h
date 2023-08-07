/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
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
#include <LibGfx/Color.h>
#include <LibWeb/CSS/Enums.h>
#include <LibWeb/CSS/Length.h>
#include <LibWeb/CSS/ValueID.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

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
        Composite,
        ConicGradient,
        Content,
        CustomIdent,
        Display,
        Easing,
        Edge,
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
        GridTrackSizeListShorthand,
        Identifier,
        Image,
        Inherit,
        Initial,
        Integer,
        Length,
        LinearGradient,
        ListStyle,
        Number,
        Overflow,
        Percentage,
        PlaceContent,
        PlaceItems,
        PlaceSelf,
        Position,
        RadialGradient,
        Ratio,
        Rect,
        Resolution,
        Revert,
        Shadow,
        String,
        TextDecoration,
        Time,
        Transformation,
        Unresolved,
        Unset,
        Url,
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
    bool is_composite() const { return type() == Type::Composite; }
    bool is_conic_gradient() const { return type() == Type::ConicGradient; }
    bool is_content() const { return type() == Type::Content; }
    bool is_custom_ident() const { return type() == Type::CustomIdent; }
    bool is_display() const { return type() == Type::Display; }
    bool is_easing() const { return type() == Type::Easing; }
    bool is_edge() const { return type() == Type::Edge; }
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
    bool is_grid_track_size_list_shorthand() const { return type() == Type::GridTrackSizeListShorthand; }
    bool is_identifier() const { return type() == Type::Identifier; }
    bool is_image() const { return type() == Type::Image; }
    bool is_inherit() const { return type() == Type::Inherit; }
    bool is_initial() const { return type() == Type::Initial; }
    bool is_integer() const { return type() == Type::Integer; }
    bool is_length() const { return type() == Type::Length; }
    bool is_linear_gradient() const { return type() == Type::LinearGradient; }
    bool is_list_style() const { return type() == Type::ListStyle; }
    bool is_number() const { return type() == Type::Number; }
    bool is_overflow() const { return type() == Type::Overflow; }
    bool is_percentage() const { return type() == Type::Percentage; }
    bool is_place_content() const { return type() == Type::PlaceContent; }
    bool is_place_items() const { return type() == Type::PlaceItems; }
    bool is_place_self() const { return type() == Type::PlaceSelf; }
    bool is_position() const { return type() == Type::Position; }
    bool is_radial_gradient() const { return type() == Type::RadialGradient; }
    bool is_ratio() const { return type() == Type::Ratio; }
    bool is_rect() const { return type() == Type::Rect; }
    bool is_resolution() const { return type() == Type::Resolution; }
    bool is_revert() const { return type() == Type::Revert; }
    bool is_shadow() const { return type() == Type::Shadow; }
    bool is_string() const { return type() == Type::String; }
    bool is_text_decoration() const { return type() == Type::TextDecoration; }
    bool is_time() const { return type() == Type::Time; }
    bool is_transformation() const { return type() == Type::Transformation; }
    bool is_unresolved() const { return type() == Type::Unresolved; }
    bool is_unset() const { return type() == Type::Unset; }
    bool is_url() const { return type() == Type::Url; }
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
    CompositeStyleValue const& as_composite() const;
    ConicGradientStyleValue const& as_conic_gradient() const;
    ContentStyleValue const& as_content() const;
    CustomIdentStyleValue const& as_custom_ident() const;
    DisplayStyleValue const& as_display() const;
    EasingStyleValue const& as_easing() const;
    EdgeStyleValue const& as_edge() const;
    FilterValueListStyleValue const& as_filter_value_list() const;
    FlexFlowStyleValue const& as_flex_flow() const;
    FlexStyleValue const& as_flex() const;
    FontStyleValue const& as_font() const;
    FrequencyStyleValue const& as_frequency() const;
    GridAreaShorthandStyleValue const& as_grid_area_shorthand() const;
    GridTemplateAreaStyleValue const& as_grid_template_area() const;
    GridTrackPlacementShorthandStyleValue const& as_grid_track_placement_shorthand() const;
    GridTrackPlacementStyleValue const& as_grid_track_placement() const;
    GridTrackSizeListShorthandStyleValue const& as_grid_track_size_list_shorthand() const;
    GridTrackSizeListStyleValue const& as_grid_track_size_list() const;
    IdentifierStyleValue const& as_identifier() const;
    ImageStyleValue const& as_image() const;
    InheritStyleValue const& as_inherit() const;
    InitialStyleValue const& as_initial() const;
    IntegerStyleValue const& as_integer() const;
    LengthStyleValue const& as_length() const;
    LinearGradientStyleValue const& as_linear_gradient() const;
    ListStyleStyleValue const& as_list_style() const;
    NumberStyleValue const& as_number() const;
    OverflowStyleValue const& as_overflow() const;
    PercentageStyleValue const& as_percentage() const;
    PlaceContentStyleValue const& as_place_content() const;
    PlaceItemsStyleValue const& as_place_items() const;
    PlaceSelfStyleValue const& as_place_self() const;
    PositionStyleValue const& as_position() const;
    RadialGradientStyleValue const& as_radial_gradient() const;
    RatioStyleValue const& as_ratio() const;
    RectStyleValue const& as_rect() const;
    ResolutionStyleValue const& as_resolution() const;
    ShadowStyleValue const& as_shadow() const;
    StringStyleValue const& as_string() const;
    TextDecorationStyleValue const& as_text_decoration() const;
    TimeStyleValue const& as_time() const;
    TransformationStyleValue const& as_transformation() const;
    UnresolvedStyleValue const& as_unresolved() const;
    UnsetStyleValue const& as_unset() const;
    URLStyleValue const& as_url() const;
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
    CustomIdentStyleValue& as_custom_ident() { return const_cast<CustomIdentStyleValue&>(const_cast<StyleValue const&>(*this).as_custom_ident()); }
    DisplayStyleValue& as_display() { return const_cast<DisplayStyleValue&>(const_cast<StyleValue const&>(*this).as_display()); }
    EasingStyleValue& as_easing() { return const_cast<EasingStyleValue&>(const_cast<StyleValue const&>(*this).as_easing()); }
    EdgeStyleValue& as_edge() { return const_cast<EdgeStyleValue&>(const_cast<StyleValue const&>(*this).as_edge()); }
    FilterValueListStyleValue& as_filter_value_list() { return const_cast<FilterValueListStyleValue&>(const_cast<StyleValue const&>(*this).as_filter_value_list()); }
    FlexFlowStyleValue& as_flex_flow() { return const_cast<FlexFlowStyleValue&>(const_cast<StyleValue const&>(*this).as_flex_flow()); }
    FlexStyleValue& as_flex() { return const_cast<FlexStyleValue&>(const_cast<StyleValue const&>(*this).as_flex()); }
    FontStyleValue& as_font() { return const_cast<FontStyleValue&>(const_cast<StyleValue const&>(*this).as_font()); }
    FrequencyStyleValue& as_frequency() { return const_cast<FrequencyStyleValue&>(const_cast<StyleValue const&>(*this).as_frequency()); }
    GridAreaShorthandStyleValue& as_grid_area_shorthand() { return const_cast<GridAreaShorthandStyleValue&>(const_cast<StyleValue const&>(*this).as_grid_area_shorthand()); }
    GridTemplateAreaStyleValue& as_grid_template_area() { return const_cast<GridTemplateAreaStyleValue&>(const_cast<StyleValue const&>(*this).as_grid_template_area()); }
    GridTrackPlacementShorthandStyleValue& as_grid_track_placement_shorthand() { return const_cast<GridTrackPlacementShorthandStyleValue&>(const_cast<StyleValue const&>(*this).as_grid_track_placement_shorthand()); }
    GridTrackPlacementStyleValue& as_grid_track_placement() { return const_cast<GridTrackPlacementStyleValue&>(const_cast<StyleValue const&>(*this).as_grid_track_placement()); }
    GridTrackSizeListShorthandStyleValue& as_grid_track_size_list_shorthand() { return const_cast<GridTrackSizeListShorthandStyleValue&>(const_cast<StyleValue const&>(*this).as_grid_track_size_list_shorthand()); }
    GridTrackSizeListStyleValue& as_grid_track_size_list() { return const_cast<GridTrackSizeListStyleValue&>(const_cast<StyleValue const&>(*this).as_grid_track_size_list()); }
    IdentifierStyleValue& as_identifier() { return const_cast<IdentifierStyleValue&>(const_cast<StyleValue const&>(*this).as_identifier()); }
    ImageStyleValue& as_image() { return const_cast<ImageStyleValue&>(const_cast<StyleValue const&>(*this).as_image()); }
    InheritStyleValue& as_inherit() { return const_cast<InheritStyleValue&>(const_cast<StyleValue const&>(*this).as_inherit()); }
    InitialStyleValue& as_initial() { return const_cast<InitialStyleValue&>(const_cast<StyleValue const&>(*this).as_initial()); }
    IntegerStyleValue& as_integer() { return const_cast<IntegerStyleValue&>(const_cast<StyleValue const&>(*this).as_integer()); }
    LengthStyleValue& as_length() { return const_cast<LengthStyleValue&>(const_cast<StyleValue const&>(*this).as_length()); }
    LinearGradientStyleValue& as_linear_gradient() { return const_cast<LinearGradientStyleValue&>(const_cast<StyleValue const&>(*this).as_linear_gradient()); }
    ListStyleStyleValue& as_list_style() { return const_cast<ListStyleStyleValue&>(const_cast<StyleValue const&>(*this).as_list_style()); }
    NumberStyleValue& as_number() { return const_cast<NumberStyleValue&>(const_cast<StyleValue const&>(*this).as_number()); }
    OverflowStyleValue& as_overflow() { return const_cast<OverflowStyleValue&>(const_cast<StyleValue const&>(*this).as_overflow()); }
    PercentageStyleValue& as_percentage() { return const_cast<PercentageStyleValue&>(const_cast<StyleValue const&>(*this).as_percentage()); }
    PlaceContentStyleValue& as_place_content() { return const_cast<PlaceContentStyleValue&>(const_cast<StyleValue const&>(*this).as_place_content()); }
    PlaceItemsStyleValue& as_place_items() { return const_cast<PlaceItemsStyleValue&>(const_cast<StyleValue const&>(*this).as_place_items()); }
    PlaceSelfStyleValue& as_place_self() { return const_cast<PlaceSelfStyleValue&>(const_cast<StyleValue const&>(*this).as_place_self()); }
    PositionStyleValue& as_position() { return const_cast<PositionStyleValue&>(const_cast<StyleValue const&>(*this).as_position()); }
    RadialGradientStyleValue& as_radial_gradient() { return const_cast<RadialGradientStyleValue&>(const_cast<StyleValue const&>(*this).as_radial_gradient()); }
    RatioStyleValue& as_ratio() { return const_cast<RatioStyleValue&>(const_cast<StyleValue const&>(*this).as_ratio()); }
    RectStyleValue& as_rect() { return const_cast<RectStyleValue&>(const_cast<StyleValue const&>(*this).as_rect()); }
    ResolutionStyleValue& as_resolution() { return const_cast<ResolutionStyleValue&>(const_cast<StyleValue const&>(*this).as_resolution()); }
    ShadowStyleValue& as_shadow() { return const_cast<ShadowStyleValue&>(const_cast<StyleValue const&>(*this).as_shadow()); }
    StringStyleValue& as_string() { return const_cast<StringStyleValue&>(const_cast<StyleValue const&>(*this).as_string()); }
    TextDecorationStyleValue& as_text_decoration() { return const_cast<TextDecorationStyleValue&>(const_cast<StyleValue const&>(*this).as_text_decoration()); }
    TimeStyleValue& as_time() { return const_cast<TimeStyleValue&>(const_cast<StyleValue const&>(*this).as_time()); }
    TransformationStyleValue& as_transformation() { return const_cast<TransformationStyleValue&>(const_cast<StyleValue const&>(*this).as_transformation()); }
    UnresolvedStyleValue& as_unresolved() { return const_cast<UnresolvedStyleValue&>(const_cast<StyleValue const&>(*this).as_unresolved()); }
    UnsetStyleValue& as_unset() { return const_cast<UnsetStyleValue&>(const_cast<StyleValue const&>(*this).as_unset()); }
    URLStyleValue& as_url() { return const_cast<URLStyleValue&>(const_cast<StyleValue const&>(*this).as_url()); }
    StyleValueList& as_value_list() { return const_cast<StyleValueList&>(const_cast<StyleValue const&>(*this).as_value_list()); }

    bool has_auto() const;
    virtual bool has_color() const { return false; }

    virtual ErrorOr<ValueComparingNonnullRefPtr<StyleValue const>> absolutized(CSSPixelRect const& viewport_rect, Length::FontMetrics const& font_metrics, Length::FontMetrics const& root_font_metrics) const;

    virtual Color to_color(Optional<Layout::NodeWithStyle const&>) const { return {}; }
    ValueID to_identifier() const;
    virtual ErrorOr<String> to_string() const = 0;

    [[nodiscard]] int to_font_weight() const;
    [[nodiscard]] int to_font_slope() const;
    [[nodiscard]] int to_font_stretch_width() const;

    virtual bool equals(StyleValue const& other) const = 0;

    bool operator==(StyleValue const& other) const
    {
        return this->equals(other);
    }

protected:
    explicit StyleValue(Type);

private:
    Type m_type;
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

}

template<>
struct AK::Formatter<Web::CSS::StyleValue> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSS::StyleValue const& style_value)
    {
        return Formatter<StringView>::format(builder, TRY(style_value.to_string()));
    }
};
