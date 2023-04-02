/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/CSS/StyleValues/AbstractImageStyleValue.h>
#include <LibWeb/CSS/StyleValues/AngleStyleValue.h>
#include <LibWeb/CSS/StyleValues/BackgroundRepeatStyleValue.h>
#include <LibWeb/CSS/StyleValues/BackgroundSizeStyleValue.h>
#include <LibWeb/CSS/StyleValues/BackgroundStyleValue.h>
#include <LibWeb/CSS/StyleValues/BorderRadiusShorthandStyleValue.h>
#include <LibWeb/CSS/StyleValues/BorderRadiusStyleValue.h>
#include <LibWeb/CSS/StyleValues/BorderStyleValue.h>
#include <LibWeb/CSS/StyleValues/CalculatedStyleValue.h>
#include <LibWeb/CSS/StyleValues/ColorStyleValue.h>
#include <LibWeb/CSS/StyleValues/ConicGradientStyleValue.h>
#include <LibWeb/CSS/StyleValues/ContentStyleValue.h>
#include <LibWeb/CSS/StyleValues/EdgeStyleValue.h>
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
#include <LibWeb/CSS/StyleValues/RectStyleValue.h>
#include <LibWeb/CSS/StyleValues/ResolutionStyleValue.h>
#include <LibWeb/CSS/StyleValues/ShadowStyleValue.h>
#include <LibWeb/CSS/StyleValues/StringStyleValue.h>
#include <LibWeb/CSS/StyleValues/StyleValueList.h>
#include <LibWeb/CSS/StyleValues/TextDecorationStyleValue.h>
#include <LibWeb/CSS/StyleValues/TimeStyleValue.h>
#include <LibWeb/CSS/StyleValues/TransformationStyleValue.h>
#include <LibWeb/CSS/StyleValues/UnresolvedStyleValue.h>
#include <LibWeb/CSS/StyleValues/UnsetStyleValue.h>

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

EdgeStyleValue const& StyleValue::as_edge() const
{
    VERIFY(is_edge());
    return static_cast<EdgeStyleValue const&>(*this);
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

}
