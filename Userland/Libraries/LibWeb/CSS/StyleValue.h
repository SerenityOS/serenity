/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/GenericShorthands.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/NonnullRefPtrVector.h>
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

struct GradientColorStop {
    Color color;
    Optional<LengthPercentage> length;
};

struct GradientColorHint {
    LengthPercentage value;
};

struct ColorStopListElement {
    Optional<GradientColorHint> transition_hint;
    GradientColorStop color_stop;
};

struct EdgeRect {
    Length top_edge;
    Length right_edge;
    Length bottom_edge;
    Length left_edge;
    Gfx::FloatRect resolved(Layout::Node const&, Gfx::FloatRect) const;
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
        Content,
        Flex,
        FlexFlow,
        Font,
        Frequency,
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

    bool is_abstract_image() const { return AK::first_is_one_of(type(), Type::Image, Type::LinearGradient); }
    bool is_angle() const { return type() == Type::Angle; }
    bool is_background() const { return type() == Type::Background; }
    bool is_background_repeat() const { return type() == Type::BackgroundRepeat; }
    bool is_background_size() const { return type() == Type::BackgroundSize; }
    bool is_border() const { return type() == Type::Border; }
    bool is_border_radius() const { return type() == Type::BorderRadius; }
    bool is_border_radius_shorthand() const { return type() == Type::BorderRadiusShorthand; }
    bool is_calculated() const { return type() == Type::Calculated; }
    bool is_color() const { return type() == Type::Color; }
    bool is_content() const { return type() == Type::Content; }
    bool is_flex() const { return type() == Type::Flex; }
    bool is_flex_flow() const { return type() == Type::FlexFlow; }
    bool is_font() const { return type() == Type::Font; }
    bool is_frequency() const { return type() == Type::Frequency; }
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
    ContentStyleValue const& as_content() const;
    FlexFlowStyleValue const& as_flex_flow() const;
    FlexStyleValue const& as_flex() const;
    FontStyleValue const& as_font() const;
    FrequencyStyleValue const& as_frequency() const;
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
    ContentStyleValue& as_content() { return const_cast<ContentStyleValue&>(const_cast<StyleValue const&>(*this).as_content()); }
    FlexFlowStyleValue& as_flex_flow() { return const_cast<FlexFlowStyleValue&>(const_cast<StyleValue const&>(*this).as_flex_flow()); }
    FlexStyleValue& as_flex() { return const_cast<FlexStyleValue&>(const_cast<StyleValue const&>(*this).as_flex()); }
    FontStyleValue& as_font() { return const_cast<FontStyleValue&>(const_cast<StyleValue const&>(*this).as_font()); }
    FrequencyStyleValue& as_frequency() { return const_cast<FrequencyStyleValue&>(const_cast<StyleValue const&>(*this).as_frequency()); }
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

    virtual NonnullRefPtr<StyleValue> absolutized(Gfx::IntRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, float font_size, float root_font_size) const;

    virtual Color to_color(Layout::NodeWithStyle const&) const { return {}; }
    virtual EdgeRect to_rect() const { VERIFY_NOT_REACHED(); }
    virtual CSS::ValueID to_identifier() const { return ValueID::Invalid; }
    virtual Length to_length() const { VERIFY_NOT_REACHED(); }
    virtual float to_number() const { return 0; }
    virtual float to_integer() const { return 0; }
    virtual String to_string() const = 0;

    bool operator==(StyleValue const& other) const { return equals(other); }
    bool operator!=(StyleValue const& other) const { return !(*this == other); }

    virtual bool equals(StyleValue const& other) const = 0;

protected:
    explicit StyleValue(Type);

private:
    Type m_type { Type::Invalid };
};

class AngleStyleValue : public StyleValue {
public:
    static NonnullRefPtr<AngleStyleValue> create(Angle angle)
    {
        return adopt_ref(*new AngleStyleValue(move(angle)));
    }
    virtual ~AngleStyleValue() override { }

    Angle const& angle() const { return m_angle; }

    virtual String to_string() const override { return m_angle.to_string(); }

    virtual bool equals(StyleValue const& other) const override
    {
        if (type() != other.type())
            return false;
        return m_angle == static_cast<AngleStyleValue const&>(other).m_angle;
    }

private:
    explicit AngleStyleValue(Angle angle)
        : StyleValue(Type::Angle)
        , m_angle(move(angle))
    {
    }

    Angle m_angle;
};

class BackgroundStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<BackgroundStyleValue> create(
        NonnullRefPtr<StyleValue> color,
        NonnullRefPtr<StyleValue> image,
        NonnullRefPtr<StyleValue> position,
        NonnullRefPtr<StyleValue> size,
        NonnullRefPtr<StyleValue> repeat,
        NonnullRefPtr<StyleValue> attachment,
        NonnullRefPtr<StyleValue> origin,
        NonnullRefPtr<StyleValue> clip)
    {
        return adopt_ref(*new BackgroundStyleValue(color, image, position, size, repeat, attachment, origin, clip));
    }
    virtual ~BackgroundStyleValue() override = default;

    size_t layer_count() const { return m_layer_count; }

    NonnullRefPtr<StyleValue> attachment() const { return m_attachment; }
    NonnullRefPtr<StyleValue> clip() const { return m_clip; }
    NonnullRefPtr<StyleValue> color() const { return m_color; }
    NonnullRefPtr<StyleValue> image() const { return m_image; }
    NonnullRefPtr<StyleValue> origin() const { return m_origin; }
    NonnullRefPtr<StyleValue> position() const { return m_position; }
    NonnullRefPtr<StyleValue> repeat() const { return m_repeat; }
    NonnullRefPtr<StyleValue> size() const { return m_size; }

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    BackgroundStyleValue(
        NonnullRefPtr<StyleValue> color,
        NonnullRefPtr<StyleValue> image,
        NonnullRefPtr<StyleValue> position,
        NonnullRefPtr<StyleValue> size,
        NonnullRefPtr<StyleValue> repeat,
        NonnullRefPtr<StyleValue> attachment,
        NonnullRefPtr<StyleValue> origin,
        NonnullRefPtr<StyleValue> clip);

    NonnullRefPtr<StyleValue> m_color;
    NonnullRefPtr<StyleValue> m_image;
    NonnullRefPtr<StyleValue> m_position;
    NonnullRefPtr<StyleValue> m_size;
    NonnullRefPtr<StyleValue> m_repeat;
    NonnullRefPtr<StyleValue> m_attachment;
    NonnullRefPtr<StyleValue> m_origin;
    NonnullRefPtr<StyleValue> m_clip;

    size_t m_layer_count;
};

class BackgroundRepeatStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<BackgroundRepeatStyleValue> create(Repeat repeat_x, Repeat repeat_y)
    {
        return adopt_ref(*new BackgroundRepeatStyleValue(repeat_x, repeat_y));
    }
    virtual ~BackgroundRepeatStyleValue() override = default;

    Repeat repeat_x() const { return m_repeat_x; }
    Repeat repeat_y() const { return m_repeat_y; }

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    BackgroundRepeatStyleValue(Repeat repeat_x, Repeat repeat_y)
        : StyleValue(Type::BackgroundRepeat)
        , m_repeat_x(repeat_x)
        , m_repeat_y(repeat_y)
    {
    }

    Repeat m_repeat_x;
    Repeat m_repeat_y;
};

// NOTE: This is not used for identifier sizes, like `cover` and `contain`.
class BackgroundSizeStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<BackgroundSizeStyleValue> create(LengthPercentage size_x, LengthPercentage size_y)
    {
        return adopt_ref(*new BackgroundSizeStyleValue(size_x, size_y));
    }
    virtual ~BackgroundSizeStyleValue() override = default;

    LengthPercentage size_x() const { return m_size_x; }
    LengthPercentage size_y() const { return m_size_y; }

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    BackgroundSizeStyleValue(LengthPercentage size_x, LengthPercentage size_y)
        : StyleValue(Type::BackgroundSize)
        , m_size_x(size_x)
        , m_size_y(size_y)
    {
    }

    LengthPercentage m_size_x;
    LengthPercentage m_size_y;
};

class BorderStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<BorderStyleValue> create(
        NonnullRefPtr<StyleValue> border_width,
        NonnullRefPtr<StyleValue> border_style,
        NonnullRefPtr<StyleValue> border_color)
    {
        return adopt_ref(*new BorderStyleValue(border_width, border_style, border_color));
    }
    virtual ~BorderStyleValue() override = default;

    NonnullRefPtr<StyleValue> border_width() const { return m_border_width; }
    NonnullRefPtr<StyleValue> border_style() const { return m_border_style; }
    NonnullRefPtr<StyleValue> border_color() const { return m_border_color; }

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    BorderStyleValue(
        NonnullRefPtr<StyleValue> border_width,
        NonnullRefPtr<StyleValue> border_style,
        NonnullRefPtr<StyleValue> border_color)
        : StyleValue(Type::Border)
        , m_border_width(border_width)
        , m_border_style(border_style)
        , m_border_color(border_color)
    {
    }

    NonnullRefPtr<StyleValue> m_border_width;
    NonnullRefPtr<StyleValue> m_border_style;
    NonnullRefPtr<StyleValue> m_border_color;
};

class BorderRadiusStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<BorderRadiusStyleValue> create(LengthPercentage const& horizontal_radius, LengthPercentage const& vertical_radius)
    {
        return adopt_ref(*new BorderRadiusStyleValue(horizontal_radius, vertical_radius));
    }
    virtual ~BorderRadiusStyleValue() override = default;

    LengthPercentage const& horizontal_radius() const { return m_horizontal_radius; }
    LengthPercentage const& vertical_radius() const { return m_vertical_radius; }
    bool is_elliptical() const { return m_is_elliptical; }

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    BorderRadiusStyleValue(LengthPercentage const& horizontal_radius, LengthPercentage const& vertical_radius)
        : StyleValue(Type::BorderRadius)
        , m_horizontal_radius(horizontal_radius)
        , m_vertical_radius(vertical_radius)
    {
        m_is_elliptical = (m_horizontal_radius != m_vertical_radius);
    }

    virtual NonnullRefPtr<StyleValue> absolutized(Gfx::IntRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, float font_size, float root_font_size) const override;

    bool m_is_elliptical;
    LengthPercentage m_horizontal_radius;
    LengthPercentage m_vertical_radius;
};

class BorderRadiusShorthandStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<BorderRadiusShorthandStyleValue> create(NonnullRefPtr<BorderRadiusStyleValue> top_left, NonnullRefPtr<BorderRadiusStyleValue> top_right, NonnullRefPtr<BorderRadiusStyleValue> bottom_right, NonnullRefPtr<BorderRadiusStyleValue> bottom_left)
    {
        return adopt_ref(*new BorderRadiusShorthandStyleValue(top_left, top_right, bottom_right, bottom_left));
    }
    virtual ~BorderRadiusShorthandStyleValue() override = default;

    NonnullRefPtr<BorderRadiusStyleValue> top_left() const { return m_top_left; }
    NonnullRefPtr<BorderRadiusStyleValue> top_right() const { return m_top_right; }
    NonnullRefPtr<BorderRadiusStyleValue> bottom_right() const { return m_bottom_right; }
    NonnullRefPtr<BorderRadiusStyleValue> bottom_left() const { return m_bottom_left; }

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    BorderRadiusShorthandStyleValue(NonnullRefPtr<BorderRadiusStyleValue> top_left, NonnullRefPtr<BorderRadiusStyleValue> top_right, NonnullRefPtr<BorderRadiusStyleValue> bottom_right, NonnullRefPtr<BorderRadiusStyleValue> bottom_left)
        : StyleValue(Type::BorderRadiusShorthand)
        , m_top_left(top_left)
        , m_top_right(top_right)
        , m_bottom_right(bottom_right)
        , m_bottom_left(bottom_left)
    {
    }

    NonnullRefPtr<BorderRadiusStyleValue> m_top_left;
    NonnullRefPtr<BorderRadiusStyleValue> m_top_right;
    NonnullRefPtr<BorderRadiusStyleValue> m_bottom_right;
    NonnullRefPtr<BorderRadiusStyleValue> m_bottom_left;
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
        String to_string() const;
        Optional<ResolvedType> resolved_type() const;
        CalculationResult resolve(Layout::Node const*, PercentageBasis const& percentage_basis) const;
    };

    struct CalcValue {
        Variant<Number, Angle, Frequency, Length, Percentage, Time, NonnullOwnPtr<CalcSum>> value;
        String to_string() const;
        Optional<ResolvedType> resolved_type() const;
        CalculationResult resolve(Layout::Node const*, PercentageBasis const& percentage_basis) const;
    };

    // This represents that: https://www.w3.org/TR/css-values-3/#calc-syntax
    struct CalcSum {
        CalcSum(NonnullOwnPtr<CalcProduct> first_calc_product, NonnullOwnPtrVector<CalcSumPartWithOperator> additional)
            : first_calc_product(move(first_calc_product))
            , zero_or_more_additional_calc_products(move(additional)) {};

        NonnullOwnPtr<CalcProduct> first_calc_product;
        NonnullOwnPtrVector<CalcSumPartWithOperator> zero_or_more_additional_calc_products;

        String to_string() const;
        Optional<ResolvedType> resolved_type() const;
        CalculationResult resolve(Layout::Node const*, PercentageBasis const& percentage_basis) const;
    };

    struct CalcNumberSum {
        CalcNumberSum(NonnullOwnPtr<CalcNumberProduct> first_calc_number_product, NonnullOwnPtrVector<CalcNumberSumPartWithOperator> additional)
            : first_calc_number_product(move(first_calc_number_product))
            , zero_or_more_additional_calc_number_products(move(additional)) {};

        NonnullOwnPtr<CalcNumberProduct> first_calc_number_product;
        NonnullOwnPtrVector<CalcNumberSumPartWithOperator> zero_or_more_additional_calc_number_products;

        String to_string() const;
        Optional<ResolvedType> resolved_type() const;
        CalculationResult resolve(Layout::Node const*, PercentageBasis const& percentage_basis) const;
    };

    struct CalcProduct {
        CalcValue first_calc_value;
        NonnullOwnPtrVector<CalcProductPartWithOperator> zero_or_more_additional_calc_values;

        String to_string() const;
        Optional<ResolvedType> resolved_type() const;
        CalculationResult resolve(Layout::Node const*, PercentageBasis const& percentage_basis) const;
    };

    struct CalcSumPartWithOperator {
        CalcSumPartWithOperator(SumOperation op, NonnullOwnPtr<CalcProduct> calc_product)
            : op(op)
            , value(move(calc_product)) {};

        SumOperation op;
        NonnullOwnPtr<CalcProduct> value;

        String to_string() const;
        Optional<ResolvedType> resolved_type() const;
        CalculationResult resolve(Layout::Node const*, PercentageBasis const& percentage_basis) const;
    };

    struct CalcProductPartWithOperator {
        ProductOperation op;
        Variant<CalcValue, CalcNumberValue> value;

        String to_string() const;
        Optional<ResolvedType> resolved_type() const;
        CalculationResult resolve(Layout::Node const*, PercentageBasis const& percentage_basis) const;
    };

    struct CalcNumberProduct {
        CalcNumberValue first_calc_number_value;
        NonnullOwnPtrVector<CalcNumberProductPartWithOperator> zero_or_more_additional_calc_number_values;

        String to_string() const;
        Optional<ResolvedType> resolved_type() const;
        CalculationResult resolve(Layout::Node const*, PercentageBasis const& percentage_basis) const;
    };

    struct CalcNumberProductPartWithOperator {
        ProductOperation op;
        CalcNumberValue value;

        String to_string() const;
        Optional<ResolvedType> resolved_type() const;
        CalculationResult resolve(Layout::Node const*, PercentageBasis const& percentage_basis) const;
    };

    struct CalcNumberSumPartWithOperator {
        CalcNumberSumPartWithOperator(SumOperation op, NonnullOwnPtr<CalcNumberProduct> calc_number_product)
            : op(op)
            , value(move(calc_number_product)) {};

        SumOperation op;
        NonnullOwnPtr<CalcNumberProduct> value;

        String to_string() const;
        Optional<ResolvedType> resolved_type() const;
        CalculationResult resolve(Layout::Node const*, PercentageBasis const& percentage_basis) const;
    };

    static NonnullRefPtr<CalculatedStyleValue> create(NonnullOwnPtr<CalcSum> calc_sum, ResolvedType resolved_type)
    {
        return adopt_ref(*new CalculatedStyleValue(move(calc_sum), resolved_type));
    }

    String to_string() const override;
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

class ColorStyleValue : public StyleValue {
public:
    static NonnullRefPtr<ColorStyleValue> create(Color color);
    virtual ~ColorStyleValue() override = default;

    Color color() const { return m_color; }
    virtual String to_string() const override;
    virtual bool has_color() const override { return true; }
    virtual Color to_color(Layout::NodeWithStyle const&) const override { return m_color; }

    virtual bool equals(StyleValue const& other) const override;

private:
    explicit ColorStyleValue(Color color)
        : StyleValue(Type::Color)
        , m_color(color)
    {
    }

    Color m_color;
};

class ContentStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<ContentStyleValue> create(NonnullRefPtr<StyleValueList> content, RefPtr<StyleValueList> alt_text)
    {
        return adopt_ref(*new ContentStyleValue(move(content), move(alt_text)));
    }

    StyleValueList const& content() const { return *m_content; }
    bool has_alt_text() const { return !m_alt_text.is_null(); }
    StyleValueList const* alt_text() const { return m_alt_text; }

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    ContentStyleValue(NonnullRefPtr<StyleValueList> content, RefPtr<StyleValueList> alt_text)
        : StyleValue(Type::Content)
        , m_content(move(content))
        , m_alt_text(move(alt_text))
    {
    }

    NonnullRefPtr<StyleValueList> m_content;
    RefPtr<StyleValueList> m_alt_text;
};

class FlexStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<FlexStyleValue> create(
        NonnullRefPtr<StyleValue> grow,
        NonnullRefPtr<StyleValue> shrink,
        NonnullRefPtr<StyleValue> basis)
    {
        return adopt_ref(*new FlexStyleValue(grow, shrink, basis));
    }
    virtual ~FlexStyleValue() override = default;

    NonnullRefPtr<StyleValue> grow() const { return m_grow; }
    NonnullRefPtr<StyleValue> shrink() const { return m_shrink; }
    NonnullRefPtr<StyleValue> basis() const { return m_basis; }

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    FlexStyleValue(
        NonnullRefPtr<StyleValue> grow,
        NonnullRefPtr<StyleValue> shrink,
        NonnullRefPtr<StyleValue> basis)
        : StyleValue(Type::Flex)
        , m_grow(grow)
        , m_shrink(shrink)
        , m_basis(basis)
    {
    }

    NonnullRefPtr<StyleValue> m_grow;
    NonnullRefPtr<StyleValue> m_shrink;
    NonnullRefPtr<StyleValue> m_basis;
};

class FlexFlowStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<FlexFlowStyleValue> create(NonnullRefPtr<StyleValue> flex_direction, NonnullRefPtr<StyleValue> flex_wrap)
    {
        return adopt_ref(*new FlexFlowStyleValue(flex_direction, flex_wrap));
    }
    virtual ~FlexFlowStyleValue() override = default;

    NonnullRefPtr<StyleValue> flex_direction() const { return m_flex_direction; }
    NonnullRefPtr<StyleValue> flex_wrap() const { return m_flex_wrap; }

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    FlexFlowStyleValue(NonnullRefPtr<StyleValue> flex_direction, NonnullRefPtr<StyleValue> flex_wrap)
        : StyleValue(Type::FlexFlow)
        , m_flex_direction(flex_direction)
        , m_flex_wrap(flex_wrap)
    {
    }

    NonnullRefPtr<StyleValue> m_flex_direction;
    NonnullRefPtr<StyleValue> m_flex_wrap;
};

class FontStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<FontStyleValue> create(NonnullRefPtr<StyleValue> font_style, NonnullRefPtr<StyleValue> font_weight, NonnullRefPtr<StyleValue> font_size, NonnullRefPtr<StyleValue> line_height, NonnullRefPtr<StyleValue> font_families) { return adopt_ref(*new FontStyleValue(font_style, font_weight, font_size, line_height, font_families)); }
    virtual ~FontStyleValue() override = default;

    NonnullRefPtr<StyleValue> font_style() const { return m_font_style; }
    NonnullRefPtr<StyleValue> font_weight() const { return m_font_weight; }
    NonnullRefPtr<StyleValue> font_size() const { return m_font_size; }
    NonnullRefPtr<StyleValue> line_height() const { return m_line_height; }
    NonnullRefPtr<StyleValue> font_families() const { return m_font_families; }

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    FontStyleValue(NonnullRefPtr<StyleValue> font_style, NonnullRefPtr<StyleValue> font_weight, NonnullRefPtr<StyleValue> font_size, NonnullRefPtr<StyleValue> line_height, NonnullRefPtr<StyleValue> font_families)
        : StyleValue(Type::Font)
        , m_font_style(font_style)
        , m_font_weight(font_weight)
        , m_font_size(font_size)
        , m_line_height(line_height)
        , m_font_families(font_families)
    {
    }

    NonnullRefPtr<StyleValue> m_font_style;
    NonnullRefPtr<StyleValue> m_font_weight;
    NonnullRefPtr<StyleValue> m_font_size;
    NonnullRefPtr<StyleValue> m_line_height;
    NonnullRefPtr<StyleValue> m_font_families;
    // FIXME: Implement font-stretch and font-variant.
};

class FrequencyStyleValue : public StyleValue {
public:
    static NonnullRefPtr<FrequencyStyleValue> create(Frequency frequency)
    {
        return adopt_ref(*new FrequencyStyleValue(move(frequency)));
    }
    virtual ~FrequencyStyleValue() override { }

    Frequency const& frequency() const { return m_frequency; }

    virtual String to_string() const override { return m_frequency.to_string(); }
    virtual bool equals(StyleValue const& other) const override;

private:
    explicit FrequencyStyleValue(Frequency frequency)
        : StyleValue(Type::Frequency)
        , m_frequency(move(frequency))
    {
    }

    Frequency m_frequency;
};

class IdentifierStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<IdentifierStyleValue> create(CSS::ValueID id)
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
    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    explicit IdentifierStyleValue(CSS::ValueID id)
        : StyleValue(Type::Identifier)
        , m_id(id)
    {
    }

    CSS::ValueID m_id { CSS::ValueID::Invalid };
};

class AbstractImageStyleValue : public StyleValue {
public:
    using StyleValue::StyleValue;

    virtual Optional<int> natural_width() const { return {}; }
    virtual Optional<int> natural_height() const { return {}; }

    virtual void load_any_resources(DOM::Document&) {};
    virtual void resolve_for_size(Layout::Node const&, Gfx::FloatSize const&) const {};

    virtual bool is_paintable() const = 0;
    virtual void paint(PaintContext& context, Gfx::IntRect const& dest_rect, CSS::ImageRendering image_rendering) const = 0;
};

class ImageStyleValue final
    : public AbstractImageStyleValue
    , public ImageResourceClient {
public:
    static NonnullRefPtr<ImageStyleValue> create(AK::URL const& url) { return adopt_ref(*new ImageStyleValue(url)); }
    virtual ~ImageStyleValue() override = default;

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

    virtual void load_any_resources(DOM::Document&) override;

    Gfx::Bitmap const* bitmap() const { return m_bitmap; }

    Optional<int> natural_width() const override;
    Optional<int> natural_height() const override;

    bool is_paintable() const override { return !m_bitmap.is_null(); }
    void paint(PaintContext& context, Gfx::IntRect const& dest_rect, CSS::ImageRendering image_rendering) const override;

private:
    ImageStyleValue(AK::URL const&);

    // ^ResourceClient
    virtual void resource_did_load() override;

    AK::URL m_url;
    WeakPtr<DOM::Document> m_document;
    RefPtr<Gfx::Bitmap> m_bitmap;
};

class LinearGradientStyleValue final : public AbstractImageStyleValue {
public:
    using GradientDirection = Variant<Angle, SideOrCorner>;

    enum class GradientType {
        Standard,
        WebKit
    };

    enum class Repeating {
        Yes,
        No
    };

    static NonnullRefPtr<LinearGradientStyleValue> create(GradientDirection direction, Vector<ColorStopListElement> color_stop_list, GradientType type, Repeating repeating)
    {
        VERIFY(color_stop_list.size() >= 2);
        return adopt_ref(*new LinearGradientStyleValue(direction, move(color_stop_list), type, repeating));
    }

    virtual String to_string() const override;
    virtual ~LinearGradientStyleValue() override = default;
    virtual bool equals(StyleValue const& other) const override;

    Vector<ColorStopListElement> const& color_stop_list() const
    {
        return m_color_stop_list;
    }

    bool is_repeating() const { return m_repeating == Repeating::Yes; }

    float angle_degrees(Gfx::FloatSize const& gradient_size) const;

    void resolve_for_size(Layout::Node const&, Gfx::FloatSize const&) const override;

    bool is_paintable() const override { return true; }
    void paint(PaintContext& context, Gfx::IntRect const& dest_rect, CSS::ImageRendering image_rendering) const override;

private:
    LinearGradientStyleValue(GradientDirection direction, Vector<ColorStopListElement> color_stop_list, GradientType type, Repeating repeating)
        : AbstractImageStyleValue(Type::LinearGradient)
        , m_direction(direction)
        , m_color_stop_list(move(color_stop_list))
        , m_gradient_type(type)
        , m_repeating(repeating)
    {
    }

    GradientDirection m_direction;
    Vector<ColorStopListElement> m_color_stop_list;
    GradientType m_gradient_type;
    Repeating m_repeating;

    mutable Optional<Painting::LinearGradientData> m_resolved_data;
};

class InheritStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<InheritStyleValue> the()
    {
        static NonnullRefPtr<InheritStyleValue> instance = adopt_ref(*new InheritStyleValue);
        return instance;
    }
    virtual ~InheritStyleValue() override = default;

    String to_string() const override { return "inherit"; }
    virtual bool equals(StyleValue const& other) const override;

private:
    InheritStyleValue()
        : StyleValue(Type::Inherit)
    {
    }
};

class InitialStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<InitialStyleValue> the()
    {
        static NonnullRefPtr<InitialStyleValue> instance = adopt_ref(*new InitialStyleValue);
        return instance;
    }
    virtual ~InitialStyleValue() override = default;

    String to_string() const override { return "initial"; }
    virtual bool equals(StyleValue const& other) const override;

private:
    InitialStyleValue()
        : StyleValue(Type::Initial)
    {
    }
};

class LengthStyleValue : public StyleValue {
public:
    static NonnullRefPtr<LengthStyleValue> create(Length const&);
    virtual ~LengthStyleValue() override = default;

    Length const& length() const { return m_length; }

    virtual bool has_auto() const override { return m_length.is_auto(); }
    virtual bool has_length() const override { return true; }
    virtual bool has_identifier() const override { return has_auto(); }
    virtual String to_string() const override { return m_length.to_string(); }
    virtual Length to_length() const override { return m_length; }
    virtual ValueID to_identifier() const override { return has_auto() ? ValueID::Auto : ValueID::Invalid; }
    virtual NonnullRefPtr<StyleValue> absolutized(Gfx::IntRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, float font_size, float root_font_size) const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    explicit LengthStyleValue(Length const& length)
        : StyleValue(Type::Length)
        , m_length(length)
    {
    }

    Length m_length;
};

class ListStyleStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<ListStyleStyleValue> create(
        NonnullRefPtr<StyleValue> position,
        NonnullRefPtr<StyleValue> image,
        NonnullRefPtr<StyleValue> style_type)
    {
        return adopt_ref(*new ListStyleStyleValue(position, image, style_type));
    }
    virtual ~ListStyleStyleValue() override = default;

    NonnullRefPtr<StyleValue> position() const { return m_position; }
    NonnullRefPtr<StyleValue> image() const { return m_image; }
    NonnullRefPtr<StyleValue> style_type() const { return m_style_type; }

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    ListStyleStyleValue(
        NonnullRefPtr<StyleValue> position,
        NonnullRefPtr<StyleValue> image,
        NonnullRefPtr<StyleValue> style_type)
        : StyleValue(Type::ListStyle)
        , m_position(position)
        , m_image(image)
        , m_style_type(style_type)
    {
    }

    NonnullRefPtr<StyleValue> m_position;
    NonnullRefPtr<StyleValue> m_image;
    NonnullRefPtr<StyleValue> m_style_type;
};

class NumericStyleValue : public StyleValue {
public:
    static NonnullRefPtr<NumericStyleValue> create_float(float value)
    {
        return adopt_ref(*new NumericStyleValue(value));
    }

    static NonnullRefPtr<NumericStyleValue> create_integer(i64 value)
    {
        return adopt_ref(*new NumericStyleValue(value));
    }

    virtual bool has_length() const override { return to_number() == 0; }
    virtual Length to_length() const override { return Length(0, Length::Type::Px); }

    virtual bool has_number() const override { return true; }
    virtual float to_number() const override
    {
        return m_value.visit(
            [](float value) { return value; },
            [](i64 value) { return (float)value; });
    }

    virtual bool has_integer() const override { return m_value.has<i64>(); }
    virtual float to_integer() const override { return m_value.get<i64>(); }

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    explicit NumericStyleValue(Variant<float, i64> value)
        : StyleValue(Type::Numeric)
        , m_value(move(value))
    {
    }

    Variant<float, i64> m_value { (i64)0 };
};

class OverflowStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<OverflowStyleValue> create(NonnullRefPtr<StyleValue> overflow_x, NonnullRefPtr<StyleValue> overflow_y)
    {
        return adopt_ref(*new OverflowStyleValue(overflow_x, overflow_y));
    }
    virtual ~OverflowStyleValue() override = default;

    NonnullRefPtr<StyleValue> overflow_x() const { return m_overflow_x; }
    NonnullRefPtr<StyleValue> overflow_y() const { return m_overflow_y; }

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    OverflowStyleValue(NonnullRefPtr<StyleValue> overflow_x, NonnullRefPtr<StyleValue> overflow_y)
        : StyleValue(Type::Overflow)
        , m_overflow_x(overflow_x)
        , m_overflow_y(overflow_y)
    {
    }

    NonnullRefPtr<StyleValue> m_overflow_x;
    NonnullRefPtr<StyleValue> m_overflow_y;
};

class PercentageStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<PercentageStyleValue> create(Percentage percentage)
    {
        return adopt_ref(*new PercentageStyleValue(move(percentage)));
    }
    virtual ~PercentageStyleValue() override = default;

    Percentage const& percentage() const { return m_percentage; }
    Percentage& percentage() { return m_percentage; }

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    PercentageStyleValue(Percentage&& percentage)
        : StyleValue(Type::Percentage)
        , m_percentage(percentage)
    {
    }

    Percentage m_percentage;
};

class PositionStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<PositionStyleValue> create(PositionEdge edge_x, LengthPercentage const& offset_x, PositionEdge edge_y, LengthPercentage const& offset_y)
    {
        return adopt_ref(*new PositionStyleValue(edge_x, offset_x, edge_y, offset_y));
    }
    virtual ~PositionStyleValue() override = default;

    PositionEdge edge_x() const { return m_edge_x; }
    LengthPercentage const& offset_x() const { return m_offset_x; }
    PositionEdge edge_y() const { return m_edge_y; }
    LengthPercentage const& offset_y() const { return m_offset_y; }

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    PositionStyleValue(PositionEdge edge_x, LengthPercentage const& offset_x, PositionEdge edge_y, LengthPercentage const& offset_y)
        : StyleValue(Type::Position)
        , m_edge_x(edge_x)
        , m_offset_x(offset_x)
        , m_edge_y(edge_y)
        , m_offset_y(offset_y)
    {
    }

    PositionEdge m_edge_x;
    LengthPercentage m_offset_x;
    PositionEdge m_edge_y;
    LengthPercentage m_offset_y;
};

class ResolutionStyleValue : public StyleValue {
public:
    static NonnullRefPtr<ResolutionStyleValue> create(Resolution resolution)
    {
        return adopt_ref(*new ResolutionStyleValue(move(resolution)));
    }
    virtual ~ResolutionStyleValue() override { }

    Resolution const& resolution() const { return m_resolution; }

    virtual String to_string() const override { return m_resolution.to_string(); }
    virtual bool equals(StyleValue const& other) const override;

private:
    explicit ResolutionStyleValue(Resolution resolution)
        : StyleValue(Type::Resolution)
        , m_resolution(move(resolution))
    {
    }

    Resolution m_resolution;
};

class ShadowStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<ShadowStyleValue>
    create(Color const& color, Length const& offset_x, Length const& offset_y, Length const& blur_radius, Length const& spread_distance, ShadowPlacement placement)
    {
        return adopt_ref(*new ShadowStyleValue(color, offset_x, offset_y, blur_radius, spread_distance, placement));
    }
    virtual ~ShadowStyleValue() override = default;

    Color const& color() const { return m_color; }
    Length const& offset_x() const { return m_offset_x; }
    Length const& offset_y() const { return m_offset_y; }
    Length const& blur_radius() const { return m_blur_radius; }
    Length const& spread_distance() const { return m_spread_distance; }
    ShadowPlacement placement() const { return m_placement; }

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    explicit ShadowStyleValue(Color const& color, Length const& offset_x, Length const& offset_y, Length const& blur_radius, Length const& spread_distance, ShadowPlacement placement)
        : StyleValue(Type::Shadow)
        , m_color(color)
        , m_offset_x(offset_x)
        , m_offset_y(offset_y)
        , m_blur_radius(blur_radius)
        , m_spread_distance(spread_distance)
        , m_placement(placement)
    {
    }

    virtual NonnullRefPtr<StyleValue> absolutized(Gfx::IntRect const& viewport_rect, Gfx::FontPixelMetrics const& font_metrics, float font_size, float root_font_size) const override;

    Color m_color;
    Length m_offset_x;
    Length m_offset_y;
    Length m_blur_radius;
    Length m_spread_distance;
    ShadowPlacement m_placement;
};

class StringStyleValue : public StyleValue {
public:
    static NonnullRefPtr<StringStyleValue> create(String const& string)
    {
        return adopt_ref(*new StringStyleValue(string));
    }
    virtual ~StringStyleValue() override = default;

    String to_string() const override { return m_string; }
    virtual bool equals(StyleValue const& other) const override;

private:
    explicit StringStyleValue(String const& string)
        : StyleValue(Type::String)
        , m_string(string)
    {
    }

    String m_string;
};

class TextDecorationStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<TextDecorationStyleValue> create(
        NonnullRefPtr<StyleValue> line,
        NonnullRefPtr<StyleValue> thickness,
        NonnullRefPtr<StyleValue> style,
        NonnullRefPtr<StyleValue> color)
    {
        return adopt_ref(*new TextDecorationStyleValue(line, thickness, style, color));
    }
    virtual ~TextDecorationStyleValue() override = default;

    NonnullRefPtr<StyleValue> line() const { return m_line; }
    NonnullRefPtr<StyleValue> thickness() const { return m_thickness; }
    NonnullRefPtr<StyleValue> style() const { return m_style; }
    NonnullRefPtr<StyleValue> color() const { return m_color; }

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    TextDecorationStyleValue(
        NonnullRefPtr<StyleValue> line,
        NonnullRefPtr<StyleValue> thickness,
        NonnullRefPtr<StyleValue> style,
        NonnullRefPtr<StyleValue> color)
        : StyleValue(Type::TextDecoration)
        , m_line(line)
        , m_thickness(thickness)
        , m_style(style)
        , m_color(color)
    {
    }

    NonnullRefPtr<StyleValue> m_line;
    NonnullRefPtr<StyleValue> m_thickness;
    NonnullRefPtr<StyleValue> m_style;
    NonnullRefPtr<StyleValue> m_color;
};

class TimeStyleValue : public StyleValue {
public:
    static NonnullRefPtr<TimeStyleValue> create(Time time)
    {
        return adopt_ref(*new TimeStyleValue(move(time)));
    }
    virtual ~TimeStyleValue() override { }

    Time const& time() const { return m_time; }

    virtual String to_string() const override { return m_time.to_string(); }
    virtual bool equals(StyleValue const& other) const override;

private:
    explicit TimeStyleValue(Time time)
        : StyleValue(Type::Time)
        , m_time(move(time))
    {
    }

    Time m_time;
};

class TransformationStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<TransformationStyleValue> create(CSS::TransformFunction transform_function, NonnullRefPtrVector<StyleValue>&& values)
    {
        return adopt_ref(*new TransformationStyleValue(transform_function, move(values)));
    }
    virtual ~TransformationStyleValue() override = default;

    CSS::TransformFunction transform_function() const { return m_transform_function; }
    NonnullRefPtrVector<StyleValue> values() const { return m_values; }

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    TransformationStyleValue(CSS::TransformFunction transform_function, NonnullRefPtrVector<StyleValue>&& values)
        : StyleValue(Type::Transformation)
        , m_transform_function(transform_function)
        , m_values(move(values))
    {
    }

    CSS::TransformFunction m_transform_function;
    NonnullRefPtrVector<StyleValue> m_values;
};

class UnresolvedStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<UnresolvedStyleValue> create(Vector<Parser::ComponentValue>&& values, bool contains_var_or_attr)
    {
        return adopt_ref(*new UnresolvedStyleValue(move(values), contains_var_or_attr));
    }
    virtual ~UnresolvedStyleValue() override = default;

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

    Vector<Parser::ComponentValue> const& values() const { return m_values; }
    bool contains_var_or_attr() const { return m_contains_var_or_attr; }

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

class UnsetStyleValue final : public StyleValue {
public:
    static NonnullRefPtr<UnsetStyleValue> the()
    {
        static NonnullRefPtr<UnsetStyleValue> instance = adopt_ref(*new UnsetStyleValue);
        return instance;
    }
    virtual ~UnsetStyleValue() override = default;

    String to_string() const override { return "unset"; }
    virtual bool equals(StyleValue const& other) const override;

private:
    UnsetStyleValue()
        : StyleValue(Type::Unset)
    {
    }
};

class StyleValueList final : public StyleValue {
public:
    enum class Separator {
        Space,
        Comma,
    };
    static NonnullRefPtr<StyleValueList> create(NonnullRefPtrVector<StyleValue>&& values, Separator separator) { return adopt_ref(*new StyleValueList(move(values), separator)); }

    size_t size() const { return m_values.size(); }
    NonnullRefPtrVector<StyleValue> const& values() const { return m_values; }
    NonnullRefPtr<StyleValue> value_at(size_t i, bool allow_loop) const
    {
        if (allow_loop)
            return m_values[i % size()];
        return m_values[i];
    }

    virtual String to_string() const override;
    virtual bool equals(StyleValue const& other) const override;

private:
    StyleValueList(NonnullRefPtrVector<StyleValue>&& values, Separator separator)
        : StyleValue(Type::ValueList)
        , m_separator(separator)
        , m_values(move(values))
    {
    }

    Separator m_separator;
    NonnullRefPtrVector<StyleValue> m_values;
};

class RectStyleValue : public StyleValue {
public:
    static NonnullRefPtr<RectStyleValue> create(EdgeRect rect);
    virtual ~RectStyleValue() override = default;

    EdgeRect rect() const { return m_rect; }
    virtual String to_string() const override;
    virtual bool has_rect() const override { return true; }
    virtual EdgeRect to_rect() const override { return m_rect; }
    virtual bool equals(StyleValue const& other) const override;

private:
    explicit RectStyleValue(EdgeRect rect)
        : StyleValue(Type::Rect)
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
        return Formatter<StringView>::format(builder, style_value.to_string());
    }
};
