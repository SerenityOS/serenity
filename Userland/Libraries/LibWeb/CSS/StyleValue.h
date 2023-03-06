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
#include <AK/Function.h>
#include <AK/GenericShorthands.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/URL.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Color.h>
#include <LibGfx/Painter.h>
#include <LibWeb/CSS/Angle.h>
#include <LibWeb/CSS/Display.h>
#include <LibWeb/CSS/Enums.h>
#include <LibWeb/CSS/Frequency.h>
#include <LibWeb/CSS/GridTrackPlacement.h>
#include <LibWeb/CSS/GridTrackSize.h>
#include <LibWeb/CSS/Length.h>
#include <LibWeb/CSS/Number.h>
#include <LibWeb/CSS/Parser/ComponentValue.h>
#include <LibWeb/CSS/Percentage.h>
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/CSS/Resolution.h>
#include <LibWeb/CSS/Time.h>
#include <LibWeb/CSS/TransformFunctions.h>
#include <LibWeb/CSS/ValueID.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Loader/ImageResource.h>
#include <LibWeb/Painting/GradientPainting.h>

namespace Web::CSS {

enum class BackgroundSize {
    Contain,
    Cover,
    LengthPercentage,
};

enum class ShadowPlacement {
    Outer,
    Inner,
};

enum class FlexBasis {
    Content,
    LengthPercentage,
    Auto,
};

// Note: The sides must be before the corners in this enum (as this order is used in parsing).
enum class SideOrCorner {
    Top,
    Bottom,
    Left,
    Right,
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight
};

template<typename TPosition>
struct ColorStopListElement {
    using PositionType = TPosition;
    struct ColorHint {
        TPosition value;
        inline bool operator==(ColorHint const&) const = default;
    };

    Optional<ColorHint> transition_hint;
    struct ColorStop {
        Color color;
        Optional<TPosition> position;
        Optional<TPosition> second_position = {};
        inline bool operator==(ColorStop const&) const = default;
    } color_stop;

    inline bool operator==(ColorStopListElement const&) const = default;
};

using LinearColorStopListElement = ColorStopListElement<LengthPercentage>;
using AngularColorStopListElement = ColorStopListElement<AnglePercentage>;

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

namespace Filter {

struct Blur {
    Optional<Length> radius {};
    float resolved_radius(Layout::Node const&) const;
    bool operator==(Blur const&) const = default;
};

struct DropShadow {
    Length offset_x;
    Length offset_y;
    Optional<Length> radius {};
    Optional<Color> color {};
    struct Resolved {
        float offset_x;
        float offset_y;
        float radius;
        Color color;
    };
    Resolved resolved(Layout::Node const&) const;
    bool operator==(DropShadow const&) const = default;
};

struct HueRotate {
    struct Zero {
        bool operator==(Zero const&) const = default;
    };
    using AngleOrZero = Variant<Angle, Zero>;
    Optional<AngleOrZero> angle {};
    float angle_degrees() const;
    bool operator==(HueRotate const&) const = default;
};

struct Color {
    enum class Operation {
        Brightness,
        Contrast,
        Grayscale,
        Invert,
        Opacity,
        Saturate,
        Sepia
    } operation;
    Optional<NumberPercentage> amount {};
    float resolved_amount() const;
    bool operator==(Color const&) const = default;
};

};

using FilterFunction = Variant<Filter::Blur, Filter::DropShadow, Filter::HueRotate, Filter::Color>;

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

    virtual ValueComparingNonnullRefPtr<StyleValue const> absolutized(CSSPixelRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, CSSPixels font_size, CSSPixels root_font_size) const;

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

class AngleStyleValue : public StyleValueWithDefaultOperators<AngleStyleValue> {
public:
    static ValueComparingNonnullRefPtr<AngleStyleValue> create(Angle angle)
    {
        return adopt_ref(*new AngleStyleValue(move(angle)));
    }
    virtual ~AngleStyleValue() override { }

    Angle const& angle() const { return m_angle; }

    virtual ErrorOr<String> to_string() const override { return m_angle.to_string(); }

    bool properties_equal(AngleStyleValue const& other) const { return m_angle == other.m_angle; }

private:
    explicit AngleStyleValue(Angle angle)
        : StyleValueWithDefaultOperators(Type::Angle)
        , m_angle(move(angle))
    {
    }

    Angle m_angle;
};

class BackgroundStyleValue final : public StyleValueWithDefaultOperators<BackgroundStyleValue> {
public:
    static ValueComparingNonnullRefPtr<BackgroundStyleValue> create(
        ValueComparingNonnullRefPtr<StyleValue const> color,
        ValueComparingNonnullRefPtr<StyleValue const> image,
        ValueComparingNonnullRefPtr<StyleValue const> position,
        ValueComparingNonnullRefPtr<StyleValue const> size,
        ValueComparingNonnullRefPtr<StyleValue const> repeat,
        ValueComparingNonnullRefPtr<StyleValue const> attachment,
        ValueComparingNonnullRefPtr<StyleValue const> origin,
        ValueComparingNonnullRefPtr<StyleValue const> clip)
    {
        return adopt_ref(*new BackgroundStyleValue(move(color), move(image), move(position), move(size), move(repeat), move(attachment), move(origin), move(clip)));
    }
    virtual ~BackgroundStyleValue() override = default;

    size_t layer_count() const { return m_properties.layer_count; }

    auto attachment() const { return m_properties.attachment; }
    auto clip() const { return m_properties.clip; }
    auto color() const { return m_properties.color; }
    auto image() const { return m_properties.image; }
    auto origin() const { return m_properties.origin; }
    auto position() const { return m_properties.position; }
    auto repeat() const { return m_properties.repeat; }
    auto size() const { return m_properties.size; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(BackgroundStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    BackgroundStyleValue(
        ValueComparingNonnullRefPtr<StyleValue const> color,
        ValueComparingNonnullRefPtr<StyleValue const> image,
        ValueComparingNonnullRefPtr<StyleValue const> position,
        ValueComparingNonnullRefPtr<StyleValue const> size,
        ValueComparingNonnullRefPtr<StyleValue const> repeat,
        ValueComparingNonnullRefPtr<StyleValue const> attachment,
        ValueComparingNonnullRefPtr<StyleValue const> origin,
        ValueComparingNonnullRefPtr<StyleValue const> clip);

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValue const> color;
        ValueComparingNonnullRefPtr<StyleValue const> image;
        ValueComparingNonnullRefPtr<StyleValue const> position;
        ValueComparingNonnullRefPtr<StyleValue const> size;
        ValueComparingNonnullRefPtr<StyleValue const> repeat;
        ValueComparingNonnullRefPtr<StyleValue const> attachment;
        ValueComparingNonnullRefPtr<StyleValue const> origin;
        ValueComparingNonnullRefPtr<StyleValue const> clip;
        size_t layer_count;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

class BackgroundRepeatStyleValue final : public StyleValueWithDefaultOperators<BackgroundRepeatStyleValue> {
public:
    static ValueComparingNonnullRefPtr<BackgroundRepeatStyleValue> create(Repeat repeat_x, Repeat repeat_y)
    {
        return adopt_ref(*new BackgroundRepeatStyleValue(repeat_x, repeat_y));
    }
    virtual ~BackgroundRepeatStyleValue() override = default;

    Repeat repeat_x() const { return m_properties.repeat_x; }
    Repeat repeat_y() const { return m_properties.repeat_y; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(BackgroundRepeatStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    BackgroundRepeatStyleValue(Repeat repeat_x, Repeat repeat_y)
        : StyleValueWithDefaultOperators(Type::BackgroundRepeat)
        , m_properties { .repeat_x = repeat_x, .repeat_y = repeat_y }
    {
    }

    struct Properties {
        Repeat repeat_x;
        Repeat repeat_y;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

// NOTE: This is not used for identifier sizes, like `cover` and `contain`.
class BackgroundSizeStyleValue final : public StyleValueWithDefaultOperators<BackgroundSizeStyleValue> {
public:
    static ValueComparingNonnullRefPtr<BackgroundSizeStyleValue> create(LengthPercentage size_x, LengthPercentage size_y)
    {
        return adopt_ref(*new BackgroundSizeStyleValue(size_x, size_y));
    }
    virtual ~BackgroundSizeStyleValue() override = default;

    LengthPercentage size_x() const { return m_properties.size_x; }
    LengthPercentage size_y() const { return m_properties.size_y; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(BackgroundSizeStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    BackgroundSizeStyleValue(LengthPercentage size_x, LengthPercentage size_y)
        : StyleValueWithDefaultOperators(Type::BackgroundSize)
        , m_properties { .size_x = size_x, .size_y = size_y }
    {
    }

    struct Properties {
        LengthPercentage size_x;
        LengthPercentage size_y;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

class BorderStyleValue final : public StyleValueWithDefaultOperators<BorderStyleValue> {
public:
    static ValueComparingNonnullRefPtr<BorderStyleValue> create(
        ValueComparingNonnullRefPtr<StyleValue> border_width,
        ValueComparingNonnullRefPtr<StyleValue> border_style,
        ValueComparingNonnullRefPtr<StyleValue> border_color)
    {
        return adopt_ref(*new BorderStyleValue(move(border_width), move(border_style), move(border_color)));
    }
    virtual ~BorderStyleValue() override = default;

    ValueComparingNonnullRefPtr<StyleValue> border_width() const { return m_properties.border_width; }
    ValueComparingNonnullRefPtr<StyleValue> border_style() const { return m_properties.border_style; }
    ValueComparingNonnullRefPtr<StyleValue> border_color() const { return m_properties.border_color; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(BorderStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    BorderStyleValue(
        ValueComparingNonnullRefPtr<StyleValue> border_width,
        ValueComparingNonnullRefPtr<StyleValue> border_style,
        ValueComparingNonnullRefPtr<StyleValue> border_color)
        : StyleValueWithDefaultOperators(Type::Border)
        , m_properties { .border_width = move(border_width), .border_style = move(border_style), .border_color = move(border_color) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValue> border_width;
        ValueComparingNonnullRefPtr<StyleValue> border_style;
        ValueComparingNonnullRefPtr<StyleValue> border_color;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

class BorderRadiusStyleValue final : public StyleValueWithDefaultOperators<BorderRadiusStyleValue> {
public:
    static ValueComparingNonnullRefPtr<BorderRadiusStyleValue> create(LengthPercentage const& horizontal_radius, LengthPercentage const& vertical_radius)
    {
        return adopt_ref(*new BorderRadiusStyleValue(horizontal_radius, vertical_radius));
    }
    virtual ~BorderRadiusStyleValue() override = default;

    LengthPercentage const& horizontal_radius() const { return m_properties.horizontal_radius; }
    LengthPercentage const& vertical_radius() const { return m_properties.vertical_radius; }
    bool is_elliptical() const { return m_properties.is_elliptical; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(BorderRadiusStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    BorderRadiusStyleValue(LengthPercentage const& horizontal_radius, LengthPercentage const& vertical_radius)
        : StyleValueWithDefaultOperators(Type::BorderRadius)
        , m_properties { .is_elliptical = horizontal_radius != vertical_radius, .horizontal_radius = horizontal_radius, .vertical_radius = vertical_radius }
    {
    }

    virtual ValueComparingNonnullRefPtr<StyleValue const> absolutized(CSSPixelRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, CSSPixels font_size, CSSPixels root_font_size) const override;

    struct Properties {
        bool is_elliptical;
        LengthPercentage horizontal_radius;
        LengthPercentage vertical_radius;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

class BorderRadiusShorthandStyleValue final : public StyleValueWithDefaultOperators<BorderRadiusShorthandStyleValue> {
public:
    static ValueComparingNonnullRefPtr<BorderRadiusShorthandStyleValue> create(
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> top_left,
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> top_right,
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> bottom_right,
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> bottom_left)
    {
        return adopt_ref(*new BorderRadiusShorthandStyleValue(move(top_left), move(top_right), move(bottom_right), move(bottom_left)));
    }
    virtual ~BorderRadiusShorthandStyleValue() override = default;

    auto top_left() const { return m_properties.top_left; }
    auto top_right() const { return m_properties.top_right; }
    auto bottom_right() const { return m_properties.bottom_right; }
    auto bottom_left() const { return m_properties.bottom_left; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(BorderRadiusShorthandStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    BorderRadiusShorthandStyleValue(
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> top_left,
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> top_right,
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> bottom_right,
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> bottom_left)
        : StyleValueWithDefaultOperators(Type::BorderRadiusShorthand)
        , m_properties { .top_left = move(top_left), .top_right = move(top_right), .bottom_right = move(bottom_right), .bottom_left = move(bottom_left) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> top_left;
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> top_right;
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> bottom_right;
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> bottom_left;
        bool operator==(Properties const&) const = default;
    } m_properties;
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

class ColorStyleValue : public StyleValueWithDefaultOperators<ColorStyleValue> {
public:
    static ValueComparingNonnullRefPtr<ColorStyleValue> create(Color color);
    virtual ~ColorStyleValue() override = default;

    Color color() const { return m_color; }
    virtual ErrorOr<String> to_string() const override;
    virtual bool has_color() const override { return true; }
    virtual Color to_color(Layout::NodeWithStyle const&) const override { return m_color; }

    bool properties_equal(ColorStyleValue const& other) const { return m_color == other.m_color; };

private:
    explicit ColorStyleValue(Color color)
        : StyleValueWithDefaultOperators(Type::Color)
        , m_color(color)
    {
    }

    Color m_color;
};

class ContentStyleValue final : public StyleValueWithDefaultOperators<ContentStyleValue> {
public:
    static ValueComparingNonnullRefPtr<ContentStyleValue> create(ValueComparingNonnullRefPtr<StyleValueList> content, ValueComparingRefPtr<StyleValueList> alt_text)
    {
        return adopt_ref(*new ContentStyleValue(move(content), move(alt_text)));
    }

    StyleValueList const& content() const { return *m_properties.content; }
    bool has_alt_text() const { return !m_properties.alt_text.is_null(); }
    StyleValueList const* alt_text() const { return m_properties.alt_text; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(ContentStyleValue const& other) const { return m_properties == other.m_properties; };

private:
    ContentStyleValue(ValueComparingNonnullRefPtr<StyleValueList> content, ValueComparingRefPtr<StyleValueList> alt_text)
        : StyleValueWithDefaultOperators(Type::Content)
        , m_properties { .content = move(content), .alt_text = move(alt_text) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValueList> content;
        ValueComparingRefPtr<StyleValueList> alt_text;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

class FilterValueListStyleValue final : public StyleValueWithDefaultOperators<FilterValueListStyleValue> {
public:
    static ValueComparingNonnullRefPtr<FilterValueListStyleValue> create(
        Vector<FilterFunction> filter_value_list)
    {
        VERIFY(filter_value_list.size() >= 1);
        return adopt_ref(*new FilterValueListStyleValue(move(filter_value_list)));
    }

    Vector<FilterFunction> const& filter_value_list() const { return m_filter_value_list; }

    virtual ErrorOr<String> to_string() const override;

    virtual ~FilterValueListStyleValue() override = default;

    bool properties_equal(FilterValueListStyleValue const& other) const { return m_filter_value_list == other.m_filter_value_list; };

private:
    FilterValueListStyleValue(Vector<FilterFunction> filter_value_list)
        : StyleValueWithDefaultOperators(Type::FilterValueList)
        , m_filter_value_list(move(filter_value_list))
    {
    }

    // FIXME: No support for SVG filters yet
    Vector<FilterFunction> m_filter_value_list;
};

class FlexStyleValue final : public StyleValueWithDefaultOperators<FlexStyleValue> {
public:
    static ValueComparingNonnullRefPtr<FlexStyleValue> create(
        ValueComparingNonnullRefPtr<StyleValue> grow,
        ValueComparingNonnullRefPtr<StyleValue> shrink,
        ValueComparingNonnullRefPtr<StyleValue> basis)
    {
        return adopt_ref(*new FlexStyleValue(move(grow), move(shrink), move(basis)));
    }
    virtual ~FlexStyleValue() override = default;

    ValueComparingNonnullRefPtr<StyleValue> grow() const { return m_properties.grow; }
    ValueComparingNonnullRefPtr<StyleValue> shrink() const { return m_properties.shrink; }
    ValueComparingNonnullRefPtr<StyleValue> basis() const { return m_properties.basis; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(FlexStyleValue const& other) const { return m_properties == other.m_properties; };

private:
    FlexStyleValue(
        ValueComparingNonnullRefPtr<StyleValue> grow,
        ValueComparingNonnullRefPtr<StyleValue> shrink,
        ValueComparingNonnullRefPtr<StyleValue> basis)
        : StyleValueWithDefaultOperators(Type::Flex)
        , m_properties { .grow = move(grow), .shrink = move(shrink), .basis = move(basis) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValue> grow;
        ValueComparingNonnullRefPtr<StyleValue> shrink;
        ValueComparingNonnullRefPtr<StyleValue> basis;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

class FlexFlowStyleValue final : public StyleValueWithDefaultOperators<FlexFlowStyleValue> {
public:
    static ValueComparingNonnullRefPtr<FlexFlowStyleValue> create(ValueComparingNonnullRefPtr<StyleValue> flex_direction, ValueComparingNonnullRefPtr<StyleValue> flex_wrap)
    {
        return adopt_ref(*new FlexFlowStyleValue(move(flex_direction), move(flex_wrap)));
    }
    virtual ~FlexFlowStyleValue() override = default;

    ValueComparingNonnullRefPtr<StyleValue> flex_direction() const { return m_properties.flex_direction; }
    ValueComparingNonnullRefPtr<StyleValue> flex_wrap() const { return m_properties.flex_wrap; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(FlexFlowStyleValue const& other) const { return m_properties == other.m_properties; };

private:
    FlexFlowStyleValue(ValueComparingNonnullRefPtr<StyleValue> flex_direction, ValueComparingNonnullRefPtr<StyleValue> flex_wrap)
        : StyleValueWithDefaultOperators(Type::FlexFlow)
        , m_properties { .flex_direction = move(flex_direction), .flex_wrap = move(flex_wrap) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValue> flex_direction;
        ValueComparingNonnullRefPtr<StyleValue> flex_wrap;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

class FontStyleValue final : public StyleValueWithDefaultOperators<FontStyleValue> {
public:
    static ValueComparingNonnullRefPtr<FontStyleValue> create(ValueComparingNonnullRefPtr<StyleValue> font_stretch, ValueComparingNonnullRefPtr<StyleValue> font_style, ValueComparingNonnullRefPtr<StyleValue> font_weight, ValueComparingNonnullRefPtr<StyleValue> font_size, ValueComparingNonnullRefPtr<StyleValue> line_height, ValueComparingNonnullRefPtr<StyleValue> font_families)
    {
        return adopt_ref(*new FontStyleValue(move(font_stretch), move(font_style), move(font_weight), move(font_size), move(line_height), move(font_families)));
    }
    virtual ~FontStyleValue() override = default;

    ValueComparingNonnullRefPtr<StyleValue> font_stretch() const { return m_properties.font_stretch; }
    ValueComparingNonnullRefPtr<StyleValue> font_style() const { return m_properties.font_style; }
    ValueComparingNonnullRefPtr<StyleValue> font_weight() const { return m_properties.font_weight; }
    ValueComparingNonnullRefPtr<StyleValue> font_size() const { return m_properties.font_size; }
    ValueComparingNonnullRefPtr<StyleValue> line_height() const { return m_properties.line_height; }
    ValueComparingNonnullRefPtr<StyleValue> font_families() const { return m_properties.font_families; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(FontStyleValue const& other) const { return m_properties == other.m_properties; };

private:
    FontStyleValue(ValueComparingNonnullRefPtr<StyleValue> font_stretch, ValueComparingNonnullRefPtr<StyleValue> font_style, ValueComparingNonnullRefPtr<StyleValue> font_weight, ValueComparingNonnullRefPtr<StyleValue> font_size, ValueComparingNonnullRefPtr<StyleValue> line_height, ValueComparingNonnullRefPtr<StyleValue> font_families)
        : StyleValueWithDefaultOperators(Type::Font)
        , m_properties { .font_stretch = move(font_stretch), .font_style = move(font_style), .font_weight = move(font_weight), .font_size = move(font_size), .line_height = move(line_height), .font_families = move(font_families) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValue> font_stretch;
        ValueComparingNonnullRefPtr<StyleValue> font_style;
        ValueComparingNonnullRefPtr<StyleValue> font_weight;
        ValueComparingNonnullRefPtr<StyleValue> font_size;
        ValueComparingNonnullRefPtr<StyleValue> line_height;
        ValueComparingNonnullRefPtr<StyleValue> font_families;
        // FIXME: Implement font-variant.
        bool operator==(Properties const&) const = default;
    } m_properties;
};

class FrequencyStyleValue : public StyleValueWithDefaultOperators<FrequencyStyleValue> {
public:
    static ValueComparingNonnullRefPtr<FrequencyStyleValue> create(Frequency frequency)
    {
        return adopt_ref(*new FrequencyStyleValue(move(frequency)));
    }
    virtual ~FrequencyStyleValue() override { }

    Frequency const& frequency() const { return m_frequency; }

    virtual ErrorOr<String> to_string() const override { return m_frequency.to_string(); }

    bool properties_equal(FrequencyStyleValue const& other) const { return m_frequency == other.m_frequency; };

private:
    explicit FrequencyStyleValue(Frequency frequency)
        : StyleValueWithDefaultOperators(Type::Frequency)
        , m_frequency(move(frequency))
    {
    }

    Frequency m_frequency;
};

class GridTemplateAreaStyleValue final : public StyleValueWithDefaultOperators<GridTemplateAreaStyleValue> {
public:
    static ValueComparingNonnullRefPtr<GridTemplateAreaStyleValue> create(Vector<Vector<String>> grid_template_area);
    virtual ~GridTemplateAreaStyleValue() override = default;

    Vector<Vector<String>> const& grid_template_area() const { return m_grid_template_area; }
    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(GridTemplateAreaStyleValue const& other) const { return m_grid_template_area == other.m_grid_template_area; };

private:
    explicit GridTemplateAreaStyleValue(Vector<Vector<String>> grid_template_area)
        : StyleValueWithDefaultOperators(Type::GridTemplateArea)
        , m_grid_template_area(grid_template_area)
    {
    }

    Vector<Vector<String>> m_grid_template_area;
};

class GridTrackPlacementStyleValue final : public StyleValueWithDefaultOperators<GridTrackPlacementStyleValue> {
public:
    static ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue> create(CSS::GridTrackPlacement grid_track_placement);
    virtual ~GridTrackPlacementStyleValue() override = default;

    CSS::GridTrackPlacement const& grid_track_placement() const { return m_grid_track_placement; }
    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(GridTrackPlacementStyleValue const& other) const { return m_grid_track_placement == other.m_grid_track_placement; };

private:
    explicit GridTrackPlacementStyleValue(CSS::GridTrackPlacement grid_track_placement)
        : StyleValueWithDefaultOperators(Type::GridTrackPlacement)
        , m_grid_track_placement(grid_track_placement)
    {
    }

    CSS::GridTrackPlacement m_grid_track_placement;
};

class GridTrackPlacementShorthandStyleValue final : public StyleValueWithDefaultOperators<GridTrackPlacementShorthandStyleValue> {
public:
    static ValueComparingNonnullRefPtr<GridTrackPlacementShorthandStyleValue> create(ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> start, ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> end)
    {
        return adopt_ref(*new GridTrackPlacementShorthandStyleValue(move(start), move(end)));
    }
    static ValueComparingNonnullRefPtr<GridTrackPlacementShorthandStyleValue> create(GridTrackPlacement start)
    {
        return adopt_ref(*new GridTrackPlacementShorthandStyleValue(GridTrackPlacementStyleValue::create(start), GridTrackPlacementStyleValue::create(GridTrackPlacement::make_auto())));
    }
    virtual ~GridTrackPlacementShorthandStyleValue() override = default;

    auto start() const { return m_properties.start; }
    auto end() const { return m_properties.end; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(GridTrackPlacementShorthandStyleValue const& other) const { return m_properties == other.m_properties; };

private:
    GridTrackPlacementShorthandStyleValue(ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> start, ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> end)
        : StyleValueWithDefaultOperators(Type::GridTrackPlacementShorthand)
        , m_properties { .start = move(start), .end = move(end) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> start;
        ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> end;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

class GridAreaShorthandStyleValue final : public StyleValueWithDefaultOperators<GridAreaShorthandStyleValue> {
public:
    static ValueComparingNonnullRefPtr<GridAreaShorthandStyleValue> create(
        ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> row_start,
        ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> column_start,
        ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> row_end,
        ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> column_end)
    {
        return adopt_ref(*new GridAreaShorthandStyleValue(row_start, column_start, row_end, column_end));
    }
    static ValueComparingNonnullRefPtr<GridAreaShorthandStyleValue> create(GridTrackPlacement row_start, GridTrackPlacement column_start, GridTrackPlacement row_end, GridTrackPlacement column_end)
    {
        return adopt_ref(*new GridAreaShorthandStyleValue(GridTrackPlacementStyleValue::create(row_start), GridTrackPlacementStyleValue::create(column_start), GridTrackPlacementStyleValue::create(row_end), GridTrackPlacementStyleValue::create(column_end)));
    }
    virtual ~GridAreaShorthandStyleValue() override = default;

    auto row_start() const { return m_properties.row_start; }
    auto column_start() const { return m_properties.column_start; }
    auto row_end() const { return m_properties.row_end; }
    auto column_end() const { return m_properties.column_end; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(GridAreaShorthandStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    GridAreaShorthandStyleValue(ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> row_start, ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> column_start, ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> row_end, ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> column_end)
        : StyleValueWithDefaultOperators(Type::GridAreaShorthand)
        , m_properties { .row_start = move(row_start), .column_start = move(column_start), .row_end = move(row_end), .column_end = move(column_end) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> row_start;
        ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> column_start;
        ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> row_end;
        ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> column_end;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

class GridTrackSizeStyleValue final : public StyleValueWithDefaultOperators<GridTrackSizeStyleValue> {
public:
    static ValueComparingNonnullRefPtr<GridTrackSizeStyleValue> create(CSS::GridTrackSizeList grid_track_size_list);
    virtual ~GridTrackSizeStyleValue() override = default;

    static ValueComparingNonnullRefPtr<GridTrackSizeStyleValue> make_auto();

    CSS::GridTrackSizeList grid_track_size_list() const { return m_grid_track_size_list; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(GridTrackSizeStyleValue const& other) const { return m_grid_track_size_list == other.m_grid_track_size_list; }

private:
    explicit GridTrackSizeStyleValue(CSS::GridTrackSizeList grid_track_size_list)
        : StyleValueWithDefaultOperators(Type::GridTrackSizeList)
        , m_grid_track_size_list(grid_track_size_list)
    {
    }

    CSS::GridTrackSizeList m_grid_track_size_list;
};

class IdentifierStyleValue final : public StyleValueWithDefaultOperators<IdentifierStyleValue> {
public:
    static ValueComparingNonnullRefPtr<IdentifierStyleValue> create(CSS::ValueID id)
    {
        return adopt_ref(*new IdentifierStyleValue(id));
    }
    virtual ~IdentifierStyleValue() override = default;

    CSS::ValueID id() const { return m_id; }

    virtual bool has_auto() const override { return m_id == ValueID::Auto; }
    virtual bool has_identifier() const override { return true; }
    virtual CSS::ValueID to_identifier() const override { return m_id; }
    virtual bool has_color() const override;
    virtual Color to_color(Layout::NodeWithStyle const& node) const override;
    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(IdentifierStyleValue const& other) const { return m_id == other.m_id; }

private:
    explicit IdentifierStyleValue(CSS::ValueID id)
        : StyleValueWithDefaultOperators(Type::Identifier)
        , m_id(id)
    {
    }

    CSS::ValueID m_id { CSS::ValueID::Invalid };
};

class AbstractImageStyleValue : public StyleValue {
public:
    using StyleValue::StyleValue;

    virtual Optional<CSSPixels> natural_width() const { return {}; }
    virtual Optional<CSSPixels> natural_height() const { return {}; }

    virtual void load_any_resources(DOM::Document&) {};
    virtual void resolve_for_size(Layout::Node const&, CSSPixelSize) const {};

    virtual bool is_paintable() const = 0;
    virtual void paint(PaintContext& context, DevicePixelRect const& dest_rect, CSS::ImageRendering image_rendering) const = 0;
};

class ImageStyleValue final
    : public AbstractImageStyleValue
    , public ImageResourceClient {
public:
    static ValueComparingNonnullRefPtr<ImageStyleValue> create(AK::URL const& url) { return adopt_ref(*new ImageStyleValue(url)); }
    virtual ~ImageStyleValue() override = default;

    virtual ErrorOr<String> to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

    virtual void load_any_resources(DOM::Document&) override;

    Optional<CSSPixels> natural_width() const override;
    Optional<CSSPixels> natural_height() const override;

    bool is_paintable() const override { return bitmap(0) != nullptr; }
    void paint(PaintContext& context, DevicePixelRect const& dest_rect, CSS::ImageRendering image_rendering) const override;

    Function<void()> on_animate;

private:
    ImageStyleValue(AK::URL const&);

    // ^ResourceClient
    virtual void resource_did_load() override;

    void animate();
    Gfx::Bitmap const* bitmap(size_t index) const;

    AK::URL m_url;
    WeakPtr<DOM::Document> m_document;

    size_t m_current_frame_index { 0 };
    size_t m_loops_completed { 0 };
    RefPtr<Platform::Timer> m_timer;
};

enum class GradientRepeating {
    Yes,
    No
};

class RadialGradientStyleValue final : public AbstractImageStyleValue {
public:
    enum class EndingShape {
        Circle,
        Ellipse
    };

    enum class Extent {
        ClosestCorner,
        ClosestSide,
        FarthestCorner,
        FarthestSide
    };

    struct CircleSize {
        Length radius;
        bool operator==(CircleSize const&) const = default;
    };

    struct EllipseSize {
        LengthPercentage radius_a;
        LengthPercentage radius_b;
        bool operator==(EllipseSize const&) const = default;
    };

    using Size = Variant<Extent, CircleSize, EllipseSize>;

    static ValueComparingNonnullRefPtr<RadialGradientStyleValue> create(EndingShape ending_shape, Size size, PositionValue position, Vector<LinearColorStopListElement> color_stop_list, GradientRepeating repeating)
    {
        VERIFY(color_stop_list.size() >= 2);
        return adopt_ref(*new RadialGradientStyleValue(ending_shape, size, position, move(color_stop_list), repeating));
    }

    virtual ErrorOr<String> to_string() const override;

    void paint(PaintContext&, DevicePixelRect const& dest_rect, CSS::ImageRendering) const override;

    virtual bool equals(StyleValue const& other) const override;

    Vector<LinearColorStopListElement> const& color_stop_list() const
    {
        return m_properties.color_stop_list;
    }

    bool is_paintable() const override { return true; }

    void resolve_for_size(Layout::Node const&, CSSPixelSize) const override;

    Gfx::FloatSize resolve_size(Layout::Node const&, Gfx::FloatPoint, Gfx::FloatRect const&) const;

    bool is_repeating() const { return m_properties.repeating == GradientRepeating::Yes; }

    virtual ~RadialGradientStyleValue() override = default;

private:
    RadialGradientStyleValue(EndingShape ending_shape, Size size, PositionValue position, Vector<LinearColorStopListElement> color_stop_list, GradientRepeating repeating)
        : AbstractImageStyleValue(Type::RadialGradient)
        , m_properties { .ending_shape = ending_shape, .size = size, .position = position, .color_stop_list = move(color_stop_list), .repeating = repeating }
    {
    }

    struct Properties {
        EndingShape ending_shape;
        Size size;
        PositionValue position;
        Vector<LinearColorStopListElement> color_stop_list;
        GradientRepeating repeating;
        bool operator==(Properties const&) const = default;
    } m_properties;

    struct ResolvedData {
        Painting::RadialGradientData data;
        Gfx::FloatSize gradient_size;
        Gfx::FloatPoint center;
    };

    mutable Optional<ResolvedData> m_resolved;
};

class ConicGradientStyleValue final : public AbstractImageStyleValue {
public:
    static ValueComparingNonnullRefPtr<ConicGradientStyleValue> create(Angle from_angle, PositionValue position, Vector<AngularColorStopListElement> color_stop_list, GradientRepeating repeating)
    {
        VERIFY(color_stop_list.size() >= 2);
        return adopt_ref(*new ConicGradientStyleValue(from_angle, position, move(color_stop_list), repeating));
    }

    virtual ErrorOr<String> to_string() const override;

    void paint(PaintContext&, DevicePixelRect const& dest_rect, CSS::ImageRendering) const override;

    virtual bool equals(StyleValue const& other) const override;

    Vector<AngularColorStopListElement> const& color_stop_list() const
    {
        return m_properties.color_stop_list;
    }

    float angle_degrees() const;

    bool is_paintable() const override { return true; }

    void resolve_for_size(Layout::Node const&, CSSPixelSize) const override;

    virtual ~ConicGradientStyleValue() override = default;

    bool is_repeating() const { return m_properties.repeating == GradientRepeating::Yes; }

private:
    ConicGradientStyleValue(Angle from_angle, PositionValue position, Vector<AngularColorStopListElement> color_stop_list, GradientRepeating repeating)
        : AbstractImageStyleValue(Type::ConicGradient)
        , m_properties { .from_angle = from_angle, .position = position, .color_stop_list = move(color_stop_list), .repeating = repeating }
    {
    }

    struct Properties {
        // FIXME: Support <color-interpolation-method>
        Angle from_angle;
        PositionValue position;
        Vector<AngularColorStopListElement> color_stop_list;
        GradientRepeating repeating;
        bool operator==(Properties const&) const = default;
    } m_properties;

    struct ResolvedData {
        Painting::ConicGradientData data;
        CSSPixelPoint position;
    };

    mutable Optional<ResolvedData> m_resolved;
};

class LinearGradientStyleValue final : public AbstractImageStyleValue {
public:
    using GradientDirection = Variant<Angle, SideOrCorner>;

    enum class GradientType {
        Standard,
        WebKit
    };

    static ValueComparingNonnullRefPtr<LinearGradientStyleValue> create(GradientDirection direction, Vector<LinearColorStopListElement> color_stop_list, GradientType type, GradientRepeating repeating)
    {
        VERIFY(color_stop_list.size() >= 2);
        return adopt_ref(*new LinearGradientStyleValue(direction, move(color_stop_list), type, repeating));
    }

    virtual ErrorOr<String> to_string() const override;
    virtual ~LinearGradientStyleValue() override = default;
    virtual bool equals(StyleValue const& other) const override;

    Vector<LinearColorStopListElement> const& color_stop_list() const
    {
        return m_properties.color_stop_list;
    }

    bool is_repeating() const { return m_properties.repeating == GradientRepeating::Yes; }

    float angle_degrees(CSSPixelSize gradient_size) const;

    void resolve_for_size(Layout::Node const&, CSSPixelSize) const override;

    bool is_paintable() const override { return true; }
    void paint(PaintContext& context, DevicePixelRect const& dest_rect, CSS::ImageRendering image_rendering) const override;

private:
    LinearGradientStyleValue(GradientDirection direction, Vector<LinearColorStopListElement> color_stop_list, GradientType type, GradientRepeating repeating)
        : AbstractImageStyleValue(Type::LinearGradient)
        , m_properties { .direction = direction, .color_stop_list = move(color_stop_list), .gradient_type = type, .repeating = repeating }
    {
    }

    struct Properties {
        GradientDirection direction;
        Vector<LinearColorStopListElement> color_stop_list;
        GradientType gradient_type;
        GradientRepeating repeating;
        bool operator==(Properties const&) const = default;
    } m_properties;

    struct ResolvedData {
        Painting::LinearGradientData data;
        CSSPixelSize size;
    };

    mutable Optional<ResolvedData> m_resolved;
};

class InheritStyleValue final : public StyleValueWithDefaultOperators<InheritStyleValue> {
public:
    static ValueComparingNonnullRefPtr<InheritStyleValue> the()
    {
        static ValueComparingNonnullRefPtr<InheritStyleValue> instance = adopt_ref(*new InheritStyleValue);
        return instance;
    }
    virtual ~InheritStyleValue() override = default;

    ErrorOr<String> to_string() const override { return "inherit"_string; }

    bool properties_equal(InheritStyleValue const&) const { return true; }

private:
    InheritStyleValue()
        : StyleValueWithDefaultOperators(Type::Inherit)
    {
    }
};

class InitialStyleValue final : public StyleValueWithDefaultOperators<InitialStyleValue> {
public:
    static ValueComparingNonnullRefPtr<InitialStyleValue> the()
    {
        static ValueComparingNonnullRefPtr<InitialStyleValue> instance = adopt_ref(*new InitialStyleValue);
        return instance;
    }
    virtual ~InitialStyleValue() override = default;

    ErrorOr<String> to_string() const override { return "initial"_string; }

    bool properties_equal(InitialStyleValue const&) const { return true; }

private:
    InitialStyleValue()
        : StyleValueWithDefaultOperators(Type::Initial)
    {
    }
};

class LengthStyleValue : public StyleValueWithDefaultOperators<LengthStyleValue> {
public:
    static ValueComparingNonnullRefPtr<LengthStyleValue> create(Length const&);
    virtual ~LengthStyleValue() override = default;

    Length const& length() const { return m_length; }

    virtual bool has_auto() const override { return m_length.is_auto(); }
    virtual bool has_length() const override { return true; }
    virtual bool has_identifier() const override { return has_auto(); }
    virtual ErrorOr<String> to_string() const override { return m_length.to_string(); }
    virtual Length to_length() const override { return m_length; }
    virtual ValueID to_identifier() const override { return has_auto() ? ValueID::Auto : ValueID::Invalid; }
    virtual ValueComparingNonnullRefPtr<StyleValue const> absolutized(CSSPixelRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, CSSPixels font_size, CSSPixels root_font_size) const override;

    bool properties_equal(LengthStyleValue const& other) const { return m_length == other.m_length; }

private:
    explicit LengthStyleValue(Length const& length)
        : StyleValueWithDefaultOperators(Type::Length)
        , m_length(length)
    {
    }

    Length m_length;
};

class ListStyleStyleValue final : public StyleValueWithDefaultOperators<ListStyleStyleValue> {
public:
    static ValueComparingNonnullRefPtr<ListStyleStyleValue> create(
        ValueComparingNonnullRefPtr<StyleValue> position,
        ValueComparingNonnullRefPtr<StyleValue> image,
        ValueComparingNonnullRefPtr<StyleValue> style_type)
    {
        return adopt_ref(*new ListStyleStyleValue(move(position), move(image), move(style_type)));
    }
    virtual ~ListStyleStyleValue() override = default;

    ValueComparingNonnullRefPtr<StyleValue> position() const { return m_properties.position; }
    ValueComparingNonnullRefPtr<StyleValue> image() const { return m_properties.image; }
    ValueComparingNonnullRefPtr<StyleValue> style_type() const { return m_properties.style_type; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(ListStyleStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    ListStyleStyleValue(
        ValueComparingNonnullRefPtr<StyleValue> position,
        ValueComparingNonnullRefPtr<StyleValue> image,
        ValueComparingNonnullRefPtr<StyleValue> style_type)
        : StyleValueWithDefaultOperators(Type::ListStyle)
        , m_properties { .position = move(position), .image = move(image), .style_type = move(style_type) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValue> position;
        ValueComparingNonnullRefPtr<StyleValue> image;
        ValueComparingNonnullRefPtr<StyleValue> style_type;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

class NumericStyleValue : public StyleValueWithDefaultOperators<NumericStyleValue> {
public:
    static ValueComparingNonnullRefPtr<NumericStyleValue> create_float(float value)
    {
        return adopt_ref(*new NumericStyleValue(value));
    }

    static ValueComparingNonnullRefPtr<NumericStyleValue> create_integer(i64 value)
    {
        return adopt_ref(*new NumericStyleValue(value));
    }

    virtual bool has_length() const override { return to_number() == 0; }
    virtual Length to_length() const override { return Length::make_px(0); }

    virtual bool has_number() const override { return true; }
    virtual float to_number() const override
    {
        return m_value.visit(
            [](float value) { return value; },
            [](i64 value) { return (float)value; });
    }

    virtual bool has_integer() const override { return m_value.has<i64>(); }
    virtual float to_integer() const override { return m_value.get<i64>(); }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(NumericStyleValue const& other) const { return m_value == other.m_value; }

private:
    explicit NumericStyleValue(Variant<float, i64> value)
        : StyleValueWithDefaultOperators(Type::Numeric)
        , m_value(move(value))
    {
    }

    Variant<float, i64> m_value { (i64)0 };
};

class OverflowStyleValue final : public StyleValueWithDefaultOperators<OverflowStyleValue> {
public:
    static ValueComparingNonnullRefPtr<OverflowStyleValue> create(ValueComparingNonnullRefPtr<StyleValue> overflow_x, ValueComparingNonnullRefPtr<StyleValue> overflow_y)
    {
        return adopt_ref(*new OverflowStyleValue(move(overflow_x), move(overflow_y)));
    }
    virtual ~OverflowStyleValue() override = default;

    ValueComparingNonnullRefPtr<StyleValue> overflow_x() const { return m_properties.overflow_x; }
    ValueComparingNonnullRefPtr<StyleValue> overflow_y() const { return m_properties.overflow_y; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(OverflowStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    OverflowStyleValue(ValueComparingNonnullRefPtr<StyleValue> overflow_x, ValueComparingNonnullRefPtr<StyleValue> overflow_y)
        : StyleValueWithDefaultOperators(Type::Overflow)
        , m_properties { .overflow_x = move(overflow_x), .overflow_y = move(overflow_y) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValue> overflow_x;
        ValueComparingNonnullRefPtr<StyleValue> overflow_y;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

class PercentageStyleValue final : public StyleValueWithDefaultOperators<PercentageStyleValue> {
public:
    static ValueComparingNonnullRefPtr<PercentageStyleValue> create(Percentage percentage)
    {
        return adopt_ref(*new PercentageStyleValue(move(percentage)));
    }
    virtual ~PercentageStyleValue() override = default;

    Percentage const& percentage() const { return m_percentage; }
    Percentage& percentage() { return m_percentage; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(PercentageStyleValue const& other) const { return m_percentage == other.m_percentage; }

private:
    PercentageStyleValue(Percentage&& percentage)
        : StyleValueWithDefaultOperators(Type::Percentage)
        , m_percentage(percentage)
    {
    }

    Percentage m_percentage;
};

class PositionStyleValue final : public StyleValueWithDefaultOperators<PositionStyleValue> {
public:
    static ValueComparingNonnullRefPtr<PositionStyleValue> create(PositionEdge edge_x, LengthPercentage const& offset_x, PositionEdge edge_y, LengthPercentage const& offset_y)
    {
        return adopt_ref(*new PositionStyleValue(edge_x, offset_x, edge_y, offset_y));
    }
    virtual ~PositionStyleValue() override = default;

    PositionEdge edge_x() const { return m_properties.edge_x; }
    LengthPercentage const& offset_x() const { return m_properties.offset_x; }
    PositionEdge edge_y() const { return m_properties.edge_y; }
    LengthPercentage const& offset_y() const { return m_properties.offset_y; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(PositionStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    PositionStyleValue(PositionEdge edge_x, LengthPercentage const& offset_x, PositionEdge edge_y, LengthPercentage const& offset_y)
        : StyleValueWithDefaultOperators(Type::Position)
        , m_properties { .edge_x = edge_x, .offset_x = offset_x, .edge_y = edge_y, .offset_y = offset_y }
    {
    }

    struct Properties {
        PositionEdge edge_x;
        LengthPercentage offset_x;
        PositionEdge edge_y;
        LengthPercentage offset_y;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

class ResolutionStyleValue : public StyleValueWithDefaultOperators<ResolutionStyleValue> {
public:
    static ValueComparingNonnullRefPtr<ResolutionStyleValue> create(Resolution resolution)
    {
        return adopt_ref(*new ResolutionStyleValue(move(resolution)));
    }
    virtual ~ResolutionStyleValue() override { }

    Resolution const& resolution() const { return m_resolution; }

    virtual ErrorOr<String> to_string() const override { return m_resolution.to_string(); }

    bool properties_equal(ResolutionStyleValue const& other) const { return m_resolution == other.m_resolution; }

private:
    explicit ResolutionStyleValue(Resolution resolution)
        : StyleValueWithDefaultOperators(Type::Resolution)
        , m_resolution(move(resolution))
    {
    }

    Resolution m_resolution;
};

class ShadowStyleValue final : public StyleValueWithDefaultOperators<ShadowStyleValue> {
public:
    static ValueComparingNonnullRefPtr<ShadowStyleValue>
    create(Color color, Length const& offset_x, Length const& offset_y, Length const& blur_radius, Length const& spread_distance, ShadowPlacement placement)
    {
        return adopt_ref(*new ShadowStyleValue(color, offset_x, offset_y, blur_radius, spread_distance, placement));
    }
    virtual ~ShadowStyleValue() override = default;

    Color color() const { return m_properties.color; }
    Length const& offset_x() const { return m_properties.offset_x; }
    Length const& offset_y() const { return m_properties.offset_y; }
    Length const& blur_radius() const { return m_properties.blur_radius; }
    Length const& spread_distance() const { return m_properties.spread_distance; }
    ShadowPlacement placement() const { return m_properties.placement; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(ShadowStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    explicit ShadowStyleValue(Color color, Length const& offset_x, Length const& offset_y, Length const& blur_radius, Length const& spread_distance, ShadowPlacement placement)
        : StyleValueWithDefaultOperators(Type::Shadow)
        , m_properties { .color = color, .offset_x = offset_x, .offset_y = offset_y, .blur_radius = blur_radius, .spread_distance = spread_distance, .placement = placement }
    {
    }

    virtual ValueComparingNonnullRefPtr<StyleValue const> absolutized(CSSPixelRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, CSSPixels font_size, CSSPixels root_font_size) const override;

    struct Properties {
        Color color;
        Length offset_x;
        Length offset_y;
        Length blur_radius;
        Length spread_distance;
        ShadowPlacement placement;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

class StringStyleValue : public StyleValueWithDefaultOperators<StringStyleValue> {
public:
    static ValueComparingNonnullRefPtr<StringStyleValue> create(String const& string)
    {
        return adopt_ref(*new StringStyleValue(string));
    }
    virtual ~StringStyleValue() override = default;

    ErrorOr<String> to_string() const override { return m_string; }

    bool properties_equal(StringStyleValue const& other) const { return m_string == other.m_string; }

private:
    explicit StringStyleValue(String const& string)
        : StyleValueWithDefaultOperators(Type::String)
        , m_string(string)
    {
    }

    String m_string;
};

class TextDecorationStyleValue final : public StyleValueWithDefaultOperators<TextDecorationStyleValue> {
public:
    static ValueComparingNonnullRefPtr<TextDecorationStyleValue> create(
        ValueComparingNonnullRefPtr<StyleValue> line,
        ValueComparingNonnullRefPtr<StyleValue> thickness,
        ValueComparingNonnullRefPtr<StyleValue> style,
        ValueComparingNonnullRefPtr<StyleValue> color)
    {
        return adopt_ref(*new TextDecorationStyleValue(move(line), move(thickness), move(style), move(color)));
    }
    virtual ~TextDecorationStyleValue() override = default;

    ValueComparingNonnullRefPtr<StyleValue> line() const { return m_properties.line; }
    ValueComparingNonnullRefPtr<StyleValue> thickness() const { return m_properties.thickness; }
    ValueComparingNonnullRefPtr<StyleValue> style() const { return m_properties.style; }
    ValueComparingNonnullRefPtr<StyleValue> color() const { return m_properties.color; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(TextDecorationStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    TextDecorationStyleValue(
        ValueComparingNonnullRefPtr<StyleValue> line,
        ValueComparingNonnullRefPtr<StyleValue> thickness,
        ValueComparingNonnullRefPtr<StyleValue> style,
        ValueComparingNonnullRefPtr<StyleValue> color)
        : StyleValueWithDefaultOperators(Type::TextDecoration)
        , m_properties { .line = move(line), .thickness = move(thickness), .style = move(style), .color = move(color) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValue> line;
        ValueComparingNonnullRefPtr<StyleValue> thickness;
        ValueComparingNonnullRefPtr<StyleValue> style;
        ValueComparingNonnullRefPtr<StyleValue> color;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

class TimeStyleValue : public StyleValueWithDefaultOperators<TimeStyleValue> {
public:
    static ValueComparingNonnullRefPtr<TimeStyleValue> create(Time time)
    {
        return adopt_ref(*new TimeStyleValue(move(time)));
    }
    virtual ~TimeStyleValue() override { }

    Time const& time() const { return m_time; }

    virtual ErrorOr<String> to_string() const override { return m_time.to_string(); }

    bool properties_equal(TimeStyleValue const& other) const { return m_time == other.m_time; }

private:
    explicit TimeStyleValue(Time time)
        : StyleValueWithDefaultOperators(Type::Time)
        , m_time(move(time))
    {
    }

    Time m_time;
};

class TransformationStyleValue final : public StyleValueWithDefaultOperators<TransformationStyleValue> {
public:
    static ValueComparingNonnullRefPtr<TransformationStyleValue> create(CSS::TransformFunction transform_function, StyleValueVector&& values)
    {
        return adopt_ref(*new TransformationStyleValue(transform_function, move(values)));
    }
    virtual ~TransformationStyleValue() override = default;

    CSS::TransformFunction transform_function() const { return m_properties.transform_function; }
    StyleValueVector values() const { return m_properties.values; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(TransformationStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    TransformationStyleValue(CSS::TransformFunction transform_function, StyleValueVector&& values)
        : StyleValueWithDefaultOperators(Type::Transformation)
        , m_properties { .transform_function = transform_function, .values = move(values) }
    {
    }

    struct Properties {
        CSS::TransformFunction transform_function;
        StyleValueVector values;
        bool operator==(Properties const& other) const;
    } m_properties;
};

class UnresolvedStyleValue final : public StyleValue {
public:
    static ValueComparingNonnullRefPtr<UnresolvedStyleValue> create(Vector<Parser::ComponentValue>&& values, bool contains_var_or_attr)
    {
        return adopt_ref(*new UnresolvedStyleValue(move(values), contains_var_or_attr));
    }
    virtual ~UnresolvedStyleValue() override = default;

    virtual ErrorOr<String> to_string() const override;

    Vector<Parser::ComponentValue> const& values() const { return m_values; }
    bool contains_var_or_attr() const { return m_contains_var_or_attr; }

    virtual bool equals(StyleValue const& other) const override;

private:
    UnresolvedStyleValue(Vector<Parser::ComponentValue>&& values, bool contains_var_or_attr)
        : StyleValue(Type::Unresolved)
        , m_values(move(values))
        , m_contains_var_or_attr(contains_var_or_attr)
    {
    }

    Vector<Parser::ComponentValue> m_values;
    bool m_contains_var_or_attr { false };
};

class UnsetStyleValue final : public StyleValueWithDefaultOperators<UnsetStyleValue> {
public:
    static ValueComparingNonnullRefPtr<UnsetStyleValue> the()
    {
        static ValueComparingNonnullRefPtr<UnsetStyleValue> instance = adopt_ref(*new UnsetStyleValue);
        return instance;
    }
    virtual ~UnsetStyleValue() override = default;

    ErrorOr<String> to_string() const override { return "unset"_string; }

    bool properties_equal(UnsetStyleValue const&) const { return true; }

private:
    UnsetStyleValue()
        : StyleValueWithDefaultOperators(Type::Unset)
    {
    }
};

class StyleValueList final : public StyleValueWithDefaultOperators<StyleValueList> {
public:
    enum class Separator {
        Space,
        Comma,
    };
    static ValueComparingNonnullRefPtr<StyleValueList> create(StyleValueVector&& values, Separator separator) { return adopt_ref(*new StyleValueList(move(values), separator)); }

    size_t size() const { return m_properties.values.size(); }
    StyleValueVector const& values() const { return m_properties.values; }
    ValueComparingNonnullRefPtr<StyleValue const> value_at(size_t i, bool allow_loop) const
    {
        if (allow_loop)
            return m_properties.values[i % size()];
        return m_properties.values[i];
    }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(StyleValueList const& other) const { return m_properties == other.m_properties; }

private:
    StyleValueList(StyleValueVector&& values, Separator separator)
        : StyleValueWithDefaultOperators(Type::ValueList)
        , m_properties { .separator = separator, .values = move(values) }
    {
    }

    struct Properties {
        Separator separator;
        StyleValueVector values;
        bool operator==(Properties const&) const;
    } m_properties;
};

class RectStyleValue : public StyleValueWithDefaultOperators<RectStyleValue> {
public:
    static ValueComparingNonnullRefPtr<RectStyleValue> create(EdgeRect rect);
    virtual ~RectStyleValue() override = default;

    EdgeRect rect() const { return m_rect; }
    virtual ErrorOr<String> to_string() const override;
    virtual bool has_rect() const override { return true; }
    virtual EdgeRect to_rect() const override { return m_rect; }

    bool properties_equal(RectStyleValue const& other) const { return m_rect == other.m_rect; }

private:
    explicit RectStyleValue(EdgeRect rect)
        : StyleValueWithDefaultOperators(Type::Rect)
        , m_rect(rect)
    {
    }

    EdgeRect m_rect;
};

}

template<>
struct AK::Formatter<Web::CSS::StyleValue> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSS::StyleValue const& style_value)
    {
        return Formatter<StringView>::format(builder, TRY(style_value.to_string()));
    }
};
