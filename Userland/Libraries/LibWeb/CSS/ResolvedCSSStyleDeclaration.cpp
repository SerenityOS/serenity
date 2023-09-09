/*
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/NonnullRefPtr.h>
#include <LibWeb/CSS/Enums.h>
#include <LibWeb/CSS/ResolvedCSSStyleDeclaration.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/CSS/StyleValues/BackgroundRepeatStyleValue.h>
#include <LibWeb/CSS/StyleValues/BackgroundSizeStyleValue.h>
#include <LibWeb/CSS/StyleValues/BackgroundStyleValue.h>
#include <LibWeb/CSS/StyleValues/BorderRadiusShorthandStyleValue.h>
#include <LibWeb/CSS/StyleValues/BorderRadiusStyleValue.h>
#include <LibWeb/CSS/StyleValues/BorderStyleValue.h>
#include <LibWeb/CSS/StyleValues/CalculatedStyleValue.h>
#include <LibWeb/CSS/StyleValues/ColorStyleValue.h>
#include <LibWeb/CSS/StyleValues/EdgeStyleValue.h>
#include <LibWeb/CSS/StyleValues/GridAreaShorthandStyleValue.h>
#include <LibWeb/CSS/StyleValues/GridTrackPlacementShorthandStyleValue.h>
#include <LibWeb/CSS/StyleValues/GridTrackPlacementStyleValue.h>
#include <LibWeb/CSS/StyleValues/GridTrackSizeListShorthandStyleValue.h>
#include <LibWeb/CSS/StyleValues/GridTrackSizeListStyleValue.h>
#include <LibWeb/CSS/StyleValues/IdentifierStyleValue.h>
#include <LibWeb/CSS/StyleValues/InitialStyleValue.h>
#include <LibWeb/CSS/StyleValues/IntegerStyleValue.h>
#include <LibWeb/CSS/StyleValues/LengthStyleValue.h>
#include <LibWeb/CSS/StyleValues/NumberStyleValue.h>
#include <LibWeb/CSS/StyleValues/PercentageStyleValue.h>
#include <LibWeb/CSS/StyleValues/PositionStyleValue.h>
#include <LibWeb/CSS/StyleValues/RatioStyleValue.h>
#include <LibWeb/CSS/StyleValues/RectStyleValue.h>
#include <LibWeb/CSS/StyleValues/ShadowStyleValue.h>
#include <LibWeb/CSS/StyleValues/StyleValueList.h>
#include <LibWeb/CSS/StyleValues/TextDecorationStyleValue.h>
#include <LibWeb/CSS/StyleValues/TimeStyleValue.h>
#include <LibWeb/CSS/StyleValues/TransformationStyleValue.h>
#include <LibWeb/CSS/StyleValues/URLStyleValue.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/StackingContext.h>
#include <LibWeb/Painting/ViewportPaintable.h>

namespace Web::CSS {

JS::NonnullGCPtr<ResolvedCSSStyleDeclaration> ResolvedCSSStyleDeclaration::create(DOM::Element& element)
{
    return element.realm().heap().allocate<ResolvedCSSStyleDeclaration>(element.realm(), element);
}

ResolvedCSSStyleDeclaration::ResolvedCSSStyleDeclaration(DOM::Element& element)
    : CSSStyleDeclaration(element.realm())
    , m_element(element)
{
}

void ResolvedCSSStyleDeclaration::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_element.ptr());
}

size_t ResolvedCSSStyleDeclaration::length() const
{
    return 0;
}

String ResolvedCSSStyleDeclaration::item(size_t index) const
{
    (void)index;
    return {};
}

static NonnullRefPtr<StyleValue const> style_value_for_background_property(Layout::NodeWithStyle const& layout_node, Function<NonnullRefPtr<StyleValue const>(BackgroundLayerData const&)> callback, Function<NonnullRefPtr<StyleValue const>()> default_value)
{
    auto const& background_layers = layout_node.background_layers();
    if (background_layers.is_empty())
        return default_value();
    if (background_layers.size() == 1)
        return callback(background_layers.first());
    StyleValueVector values;
    values.ensure_capacity(background_layers.size());
    for (auto const& layer : background_layers)
        values.unchecked_append(callback(layer));
    return StyleValueList::create(move(values), StyleValueList::Separator::Comma);
}

static NonnullRefPtr<StyleValue const> value_or_default(Optional<StyleProperty> property, NonnullRefPtr<StyleValue> default_style)
{
    if (property.has_value())
        return property.value().value;
    return default_style;
}

static NonnullRefPtr<StyleValue const> style_value_for_length_percentage(LengthPercentage const& length_percentage)
{
    if (length_percentage.is_auto())
        return IdentifierStyleValue::create(ValueID::Auto);
    if (length_percentage.is_percentage())
        return PercentageStyleValue::create(length_percentage.percentage());
    if (length_percentage.is_length())
        return LengthStyleValue::create(length_percentage.length());
    return length_percentage.calculated();
}

static NonnullRefPtr<StyleValue const> style_value_for_size(Size const& size)
{
    if (size.is_none())
        return IdentifierStyleValue::create(ValueID::None);
    if (size.is_percentage())
        return PercentageStyleValue::create(size.percentage());
    if (size.is_length())
        return LengthStyleValue::create(size.length());
    if (size.is_auto())
        return IdentifierStyleValue::create(ValueID::Auto);
    if (size.is_calculated())
        return size.calculated();
    if (size.is_min_content())
        return IdentifierStyleValue::create(ValueID::MinContent);
    if (size.is_max_content())
        return IdentifierStyleValue::create(ValueID::MaxContent);
    // FIXME: Support fit-content(<length>)
    if (size.is_fit_content())
        return IdentifierStyleValue::create(ValueID::FitContent);
    TODO();
}

static NonnullRefPtr<StyleValue const> style_value_for_sided_shorthand(ValueComparingNonnullRefPtr<StyleValue const> top, ValueComparingNonnullRefPtr<StyleValue const> right, ValueComparingNonnullRefPtr<StyleValue const> bottom, ValueComparingNonnullRefPtr<StyleValue const> left)
{
    bool top_and_bottom_same = top == bottom;
    bool left_and_right_same = left == right;

    if (top_and_bottom_same && left_and_right_same && top == left)
        return top;

    if (top_and_bottom_same && left_and_right_same)
        return StyleValueList::create(StyleValueVector { move(top), move(right) }, StyleValueList::Separator::Space);

    if (left_and_right_same)
        return StyleValueList::create(StyleValueVector { move(top), move(right), move(bottom) }, StyleValueList::Separator::Space);

    return StyleValueList::create(StyleValueVector { move(top), move(right), move(bottom), move(left) }, StyleValueList::Separator::Space);
}

RefPtr<StyleValue const> ResolvedCSSStyleDeclaration::style_value_for_property(Layout::NodeWithStyle const& layout_node, PropertyID property_id) const
{
    // A limited number of properties have special rules for producing their "resolved value".
    // We also have to manually construct shorthands from their longhands here.
    // Everything else uses the computed value.
    // https://www.w3.org/TR/cssom-1/#resolved-values
    switch (property_id) {
    case PropertyID::Background: {
        auto maybe_background_color = property(PropertyID::BackgroundColor);
        auto maybe_background_image = property(PropertyID::BackgroundImage);
        auto maybe_background_position = property(PropertyID::BackgroundPosition);
        auto maybe_background_size = property(PropertyID::BackgroundSize);
        auto maybe_background_repeat = property(PropertyID::BackgroundRepeat);
        auto maybe_background_attachment = property(PropertyID::BackgroundAttachment);
        auto maybe_background_origin = property(PropertyID::BackgroundOrigin);
        auto maybe_background_clip = property(PropertyID::BackgroundClip);

        return BackgroundStyleValue::create(
            value_or_default(maybe_background_color, InitialStyleValue::the()),
            value_or_default(maybe_background_image, IdentifierStyleValue::create(ValueID::None)),
            value_or_default(maybe_background_position, PositionStyleValue::create(EdgeStyleValue::create(PositionEdge::Left, Length::make_px(0)), EdgeStyleValue::create(PositionEdge::Top, Length::make_px(0)))),
            value_or_default(maybe_background_size, IdentifierStyleValue::create(ValueID::Auto)),
            value_or_default(maybe_background_repeat, BackgroundRepeatStyleValue::create(Repeat::Repeat, Repeat::Repeat)),
            value_or_default(maybe_background_attachment, IdentifierStyleValue::create(ValueID::Scroll)),
            value_or_default(maybe_background_origin, IdentifierStyleValue::create(ValueID::PaddingBox)),
            value_or_default(maybe_background_clip, IdentifierStyleValue::create(ValueID::BorderBox)));
    }
    case PropertyID::BackgroundColor:
        return ColorStyleValue::create(layout_node.computed_values().background_color());
    case PropertyID::BackgroundPosition:
        return style_value_for_background_property(
            layout_node,
            [](auto& layer) -> NonnullRefPtr<StyleValue> {
                return PositionStyleValue::create(
                    EdgeStyleValue::create(layer.position_edge_x, layer.position_offset_x),
                    EdgeStyleValue::create(layer.position_edge_y, layer.position_offset_y));
            },
            []() -> NonnullRefPtr<StyleValue> {
                return PositionStyleValue::create(
                    EdgeStyleValue::create(PositionEdge::Left, Percentage(0)),
                    EdgeStyleValue::create(PositionEdge::Top, Percentage(0)));
            });
    case PropertyID::Border: {
        auto top = layout_node.computed_values().border_top();
        auto right = layout_node.computed_values().border_right();
        auto bottom = layout_node.computed_values().border_bottom();
        auto left = layout_node.computed_values().border_left();
        // `border` only has a reasonable value if all four sides are the same.
        if (top != right || top != bottom || top != left)
            return nullptr;
        auto width = LengthStyleValue::create(Length::make_px(top.width));
        auto style = IdentifierStyleValue::create(to_value_id(top.line_style));
        auto color = ColorStyleValue::create(top.color);
        return BorderStyleValue::create(width, style, color);
    }
    case PropertyID::BorderBottom: {
        auto border = layout_node.computed_values().border_bottom();
        auto width = LengthStyleValue::create(Length::make_px(border.width));
        auto style = IdentifierStyleValue::create(to_value_id(border.line_style));
        auto color = ColorStyleValue::create(border.color);
        return BorderStyleValue::create(width, style, color);
    }
    case PropertyID::BorderBottomColor:
        return ColorStyleValue::create(layout_node.computed_values().border_bottom().color);
    case PropertyID::BorderColor: {
        auto top = ColorStyleValue::create(layout_node.computed_values().border_top().color);
        auto right = ColorStyleValue::create(layout_node.computed_values().border_right().color);
        auto bottom = ColorStyleValue::create(layout_node.computed_values().border_bottom().color);
        auto left = ColorStyleValue::create(layout_node.computed_values().border_left().color);
        return style_value_for_sided_shorthand(top, right, bottom, left);
    }
    case PropertyID::BorderLeft: {
        auto border = layout_node.computed_values().border_left();
        auto width = LengthStyleValue::create(Length::make_px(border.width));
        auto style = IdentifierStyleValue::create(to_value_id(border.line_style));
        auto color = ColorStyleValue::create(border.color);
        return BorderStyleValue::create(width, style, color);
    }
    case PropertyID::BorderLeftColor:
        return ColorStyleValue::create(layout_node.computed_values().border_left().color);
    case PropertyID::BorderRadius: {
        auto maybe_top_left_radius = property(PropertyID::BorderTopLeftRadius);
        auto maybe_top_right_radius = property(PropertyID::BorderTopRightRadius);
        auto maybe_bottom_left_radius = property(PropertyID::BorderBottomLeftRadius);
        auto maybe_bottom_right_radius = property(PropertyID::BorderBottomRightRadius);
        RefPtr<BorderRadiusStyleValue const> top_left_radius, top_right_radius, bottom_left_radius, bottom_right_radius;
        if (maybe_top_left_radius.has_value()) {
            VERIFY(maybe_top_left_radius.value().value->is_border_radius());
            top_left_radius = maybe_top_left_radius.value().value->as_border_radius();
        }
        if (maybe_top_right_radius.has_value()) {
            VERIFY(maybe_top_right_radius.value().value->is_border_radius());
            top_right_radius = maybe_top_right_radius.value().value->as_border_radius();
        }
        if (maybe_bottom_left_radius.has_value()) {
            VERIFY(maybe_bottom_left_radius.value().value->is_border_radius());
            bottom_left_radius = maybe_bottom_left_radius.value().value->as_border_radius();
        }
        if (maybe_bottom_right_radius.has_value()) {
            VERIFY(maybe_bottom_right_radius.value().value->is_border_radius());
            bottom_right_radius = maybe_bottom_right_radius.value().value->as_border_radius();
        }

        return BorderRadiusShorthandStyleValue::create(top_left_radius.release_nonnull(), top_right_radius.release_nonnull(), bottom_right_radius.release_nonnull(), bottom_left_radius.release_nonnull());
    }
    case PropertyID::BorderRight: {
        auto border = layout_node.computed_values().border_right();
        auto width = LengthStyleValue::create(Length::make_px(border.width));
        auto style = IdentifierStyleValue::create(to_value_id(border.line_style));
        auto color = ColorStyleValue::create(border.color);
        return BorderStyleValue::create(width, style, color);
    }
    case PropertyID::BorderRightColor:
        return ColorStyleValue::create(layout_node.computed_values().border_right().color);
    case PropertyID::BorderStyle: {
        auto top = IdentifierStyleValue::create(to_value_id(layout_node.computed_values().border_top().line_style));
        auto right = IdentifierStyleValue::create(to_value_id(layout_node.computed_values().border_right().line_style));
        auto bottom = IdentifierStyleValue::create(to_value_id(layout_node.computed_values().border_bottom().line_style));
        auto left = IdentifierStyleValue::create(to_value_id(layout_node.computed_values().border_left().line_style));
        return style_value_for_sided_shorthand(top, right, bottom, left);
    }
    case PropertyID::BorderTop: {
        auto border = layout_node.computed_values().border_top();
        auto width = LengthStyleValue::create(Length::make_px(border.width));
        auto style = IdentifierStyleValue::create(to_value_id(border.line_style));
        auto color = ColorStyleValue::create(border.color);
        return BorderStyleValue::create(width, style, color);
    }
    case PropertyID::BorderTopColor:
        return ColorStyleValue::create(layout_node.computed_values().border_top().color);
    case PropertyID::BorderWidth: {
        auto top = LengthStyleValue::create(Length::make_px(layout_node.computed_values().border_top().width));
        auto right = LengthStyleValue::create(Length::make_px(layout_node.computed_values().border_right().width));
        auto bottom = LengthStyleValue::create(Length::make_px(layout_node.computed_values().border_bottom().width));
        auto left = LengthStyleValue::create(Length::make_px(layout_node.computed_values().border_left().width));
        return style_value_for_sided_shorthand(top, right, bottom, left);
    }
    case PropertyID::Bottom:
        return style_value_for_length_percentage(layout_node.computed_values().inset().bottom());
    case PropertyID::Color:
        return ColorStyleValue::create(layout_node.computed_values().color());
    case PropertyID::GridArea: {
        auto maybe_grid_row_start = property(PropertyID::GridRowStart);
        auto maybe_grid_column_start = property(PropertyID::GridColumnStart);
        auto maybe_grid_row_end = property(PropertyID::GridRowEnd);
        auto maybe_grid_column_end = property(PropertyID::GridColumnEnd);
        RefPtr<GridTrackPlacementStyleValue const> grid_row_start, grid_column_start, grid_row_end, grid_column_end;
        if (maybe_grid_row_start.has_value()) {
            VERIFY(maybe_grid_row_start.value().value->is_grid_track_placement());
            grid_row_start = maybe_grid_row_start.value().value->as_grid_track_placement();
        }
        if (maybe_grid_column_start.has_value()) {
            VERIFY(maybe_grid_column_start.value().value->is_grid_track_placement());
            grid_column_start = maybe_grid_column_start.value().value->as_grid_track_placement();
        }
        if (maybe_grid_row_end.has_value()) {
            VERIFY(maybe_grid_row_end.value().value->is_grid_track_placement());
            grid_row_end = maybe_grid_row_end.value().value->as_grid_track_placement();
        }
        if (maybe_grid_column_end.has_value()) {
            VERIFY(maybe_grid_column_end.value().value->is_grid_track_placement());
            grid_column_end = maybe_grid_column_end.value().value->as_grid_track_placement();
        }
        return GridAreaShorthandStyleValue::create(
            grid_row_start.release_nonnull(),
            grid_column_start.release_nonnull(),
            grid_row_end.release_nonnull(),
            grid_column_end.release_nonnull());
    }
    case PropertyID::GridColumn: {
        auto maybe_grid_column_end = property(PropertyID::GridColumnEnd);
        auto maybe_grid_column_start = property(PropertyID::GridColumnStart);
        RefPtr<GridTrackPlacementStyleValue const> grid_column_start, grid_column_end;
        if (maybe_grid_column_end.has_value()) {
            VERIFY(maybe_grid_column_end.value().value->is_grid_track_placement());
            grid_column_end = maybe_grid_column_end.value().value->as_grid_track_placement();
        }
        if (maybe_grid_column_start.has_value()) {
            VERIFY(maybe_grid_column_start.value().value->is_grid_track_placement());
            grid_column_start = maybe_grid_column_start.value().value->as_grid_track_placement();
        }
        return GridTrackPlacementShorthandStyleValue::create(grid_column_end.release_nonnull(), grid_column_start.release_nonnull());
    }
    case PropertyID::GridRow: {
        auto maybe_grid_row_end = property(PropertyID::GridRowEnd);
        auto maybe_grid_row_start = property(PropertyID::GridRowStart);
        RefPtr<GridTrackPlacementStyleValue const> grid_row_start, grid_row_end;
        if (maybe_grid_row_end.has_value()) {
            VERIFY(maybe_grid_row_end.value().value->is_grid_track_placement());
            grid_row_end = maybe_grid_row_end.value().value->as_grid_track_placement();
        }
        if (maybe_grid_row_start.has_value()) {
            VERIFY(maybe_grid_row_start.value().value->is_grid_track_placement());
            grid_row_start = maybe_grid_row_start.value().value->as_grid_track_placement();
        }
        return GridTrackPlacementShorthandStyleValue::create(grid_row_end.release_nonnull(), grid_row_start.release_nonnull());
    }
    case PropertyID::GridTemplate: {
        auto maybe_grid_template_areas = property(PropertyID::GridTemplateAreas);
        auto maybe_grid_template_rows = property(PropertyID::GridTemplateRows);
        auto maybe_grid_template_columns = property(PropertyID::GridTemplateColumns);
        RefPtr<GridTemplateAreaStyleValue const> grid_template_areas;
        RefPtr<GridTrackSizeListStyleValue const> grid_template_rows, grid_template_columns;
        if (maybe_grid_template_areas.has_value()) {
            VERIFY(maybe_grid_template_areas.value().value->is_grid_template_area());
            grid_template_areas = maybe_grid_template_areas.value().value->as_grid_template_area();
        }
        if (maybe_grid_template_rows.has_value()) {
            VERIFY(maybe_grid_template_rows.value().value->is_grid_track_size_list());
            grid_template_rows = maybe_grid_template_rows.value().value->as_grid_track_size_list();
        }
        if (maybe_grid_template_columns.has_value()) {
            VERIFY(maybe_grid_template_columns.value().value->is_grid_track_size_list());
            grid_template_columns = maybe_grid_template_columns.value().value->as_grid_track_size_list();
        }
        return GridTrackSizeListShorthandStyleValue::create(grid_template_areas.release_nonnull(), grid_template_rows.release_nonnull(), grid_template_columns.release_nonnull());
    }
    case PropertyID::Height:
        return style_value_for_size(layout_node.computed_values().height());
    case PropertyID::Left:
        return style_value_for_length_percentage(layout_node.computed_values().inset().left());
    case PropertyID::Margin: {
        auto margin = layout_node.computed_values().margin();
        auto top = style_value_for_length_percentage(margin.top());
        auto right = style_value_for_length_percentage(margin.right());
        auto bottom = style_value_for_length_percentage(margin.bottom());
        auto left = style_value_for_length_percentage(margin.left());
        return style_value_for_sided_shorthand(move(top), move(right), move(bottom), move(left));
    }
    case PropertyID::MarginBottom:
        return style_value_for_length_percentage(layout_node.computed_values().margin().bottom());
    case PropertyID::MarginLeft:
        return style_value_for_length_percentage(layout_node.computed_values().margin().left());
    case PropertyID::MarginRight:
        return style_value_for_length_percentage(layout_node.computed_values().margin().right());
    case PropertyID::MarginTop:
        return style_value_for_length_percentage(layout_node.computed_values().margin().top());
    case PropertyID::Outline: {
        return StyleValueList::create(
            { style_value_for_property(layout_node, PropertyID::OutlineColor).release_nonnull(),
                style_value_for_property(layout_node, PropertyID::OutlineStyle).release_nonnull(),
                style_value_for_property(layout_node, PropertyID::OutlineWidth).release_nonnull() },
            StyleValueList::Separator::Space);
    }
    case PropertyID::OutlineColor:
        return ColorStyleValue::create(layout_node.computed_values().outline_color());
    case PropertyID::Padding: {
        auto padding = layout_node.computed_values().padding();
        auto top = style_value_for_length_percentage(padding.top());
        auto right = style_value_for_length_percentage(padding.right());
        auto bottom = style_value_for_length_percentage(padding.bottom());
        auto left = style_value_for_length_percentage(padding.left());
        return style_value_for_sided_shorthand(move(top), move(right), move(bottom), move(left));
    }
    case PropertyID::PaddingBottom:
        return style_value_for_length_percentage(layout_node.computed_values().padding().bottom());
    case PropertyID::PaddingLeft:
        return style_value_for_length_percentage(layout_node.computed_values().padding().left());
    case PropertyID::PaddingRight:
        return style_value_for_length_percentage(layout_node.computed_values().padding().right());
    case PropertyID::PaddingTop:
        return style_value_for_length_percentage(layout_node.computed_values().padding().top());
    case PropertyID::Right:
        return style_value_for_length_percentage(layout_node.computed_values().inset().right());
    case PropertyID::TextDecoration: {
        auto line = style_value_for_property(layout_node, PropertyID::TextDecorationLine);
        auto thickness = style_value_for_property(layout_node, PropertyID::TextDecorationThickness);
        auto style = style_value_for_property(layout_node, PropertyID::TextDecorationStyle);
        auto color = style_value_for_property(layout_node, PropertyID::TextDecorationColor);
        return TextDecorationStyleValue::create(*line, *thickness, *style, *color);
    }
    case PropertyID::TextDecorationColor:
        return ColorStyleValue::create(layout_node.computed_values().text_decoration_color());
    case PropertyID::Top:
        return style_value_for_length_percentage(layout_node.computed_values().inset().top());
    case PropertyID::Transform: {
        // NOTE: The computed value for `transform` serializes as a single `matrix(...)` value, instead of
        //       the original list of transform functions. So, we produce a StyleValue for that.
        //       https://www.w3.org/TR/css-transforms-1/#serialization-of-the-computed-value
        // FIXME: Computing values should happen in the StyleComputer!
        auto transformations = layout_node.computed_values().transformations();
        if (transformations.is_empty())
            return IdentifierStyleValue::create(ValueID::None);

        // The transform matrix is held by the StackingContext, so we need to make sure we have one first.
        auto const* viewport = layout_node.document().paintable();
        VERIFY(viewport);
        const_cast<Painting::ViewportPaintable&>(*viewport).build_stacking_context_tree_if_needed();

        VERIFY(layout_node.paintable());
        auto const& paintable_box = verify_cast<Painting::PaintableBox const>(layout_node.paintable());
        VERIFY(paintable_box->stacking_context());

        // FIXME: This needs to serialize to matrix3d if the transformation matrix is a 3D matrix.
        //        https://w3c.github.io/csswg-drafts/css-transforms-2/#serialization-of-the-computed-value
        auto affine_matrix = paintable_box->stacking_context()->affine_transform_matrix();

        StyleValueVector parameters;
        parameters.ensure_capacity(6);
        parameters.unchecked_append(NumberStyleValue::create(affine_matrix.a()));
        parameters.unchecked_append(NumberStyleValue::create(affine_matrix.b()));
        parameters.unchecked_append(NumberStyleValue::create(affine_matrix.c()));
        parameters.unchecked_append(NumberStyleValue::create(affine_matrix.d()));
        parameters.unchecked_append(NumberStyleValue::create(affine_matrix.e()));
        parameters.unchecked_append(NumberStyleValue::create(affine_matrix.f()));

        NonnullRefPtr<StyleValue> matrix_function = TransformationStyleValue::create(TransformFunction::Matrix, move(parameters));
        // Elsewhere we always store the transform property's value as a StyleValueList of TransformationStyleValues,
        // so this is just for consistency.
        StyleValueVector matrix_functions { matrix_function };
        return StyleValueList::create(move(matrix_functions), StyleValueList::Separator::Space);
    }
    case PropertyID::Width:
        return style_value_for_size(layout_node.computed_values().width());
    case PropertyID::Invalid:
        return IdentifierStyleValue::create(ValueID::Invalid);
    case PropertyID::Custom:
        dbgln_if(LIBWEB_CSS_DEBUG, "Computed style for custom properties was requested (?)");
        return nullptr;
    default:
        if (!property_is_shorthand(property_id))
            return static_cast<DOM::Element const&>(*layout_node.dom_node()).computed_css_values()->property(property_id);
        dbgln_if(LIBWEB_CSS_DEBUG, "FIXME: Computed style for the '{}' shorthand property was requested", string_from_property_id(property_id));
        return nullptr;
    }
}

Optional<StyleProperty> ResolvedCSSStyleDeclaration::property(PropertyID property_id) const
{
    // https://www.w3.org/TR/cssom-1/#dom-window-getcomputedstyle
    // NOTE: This is a partial enforcement of step 5 ("If elt is connected, ...")
    if (!m_element->is_connected())
        return {};

    if (property_affects_layout(property_id)) {
        const_cast<DOM::Document&>(m_element->document()).update_layout();
    } else {
        // FIXME: If we had a way to update style for a single element, this would be a good place to use it.
        const_cast<DOM::Document&>(m_element->document()).update_style();
    }

    if (!m_element->layout_node()) {
        auto style_or_error = m_element->document().style_computer().compute_style(const_cast<DOM::Element&>(*m_element));
        if (style_or_error.is_error()) {
            dbgln("ResolvedCSSStyleDeclaration::property style computer failed");
            return {};
        }
        auto style = style_or_error.release_value();

        // FIXME: This is a stopgap until we implement shorthand -> longhand conversion.
        auto value = style->maybe_null_property(property_id);
        if (!value) {
            dbgln("FIXME: ResolvedCSSStyleDeclaration::property(property_id=0x{:x}) No value for property ID in newly computed style case.", to_underlying(property_id));
            return {};
        }
        return StyleProperty {
            .property_id = property_id,
            .value = value.release_nonnull(),
        };
    }

    auto& layout_node = *m_element->layout_node();
    auto value = style_value_for_property(layout_node, property_id);
    if (!value)
        return {};
    return StyleProperty {
        .property_id = property_id,
        .value = value.release_nonnull(),
    };
}

// https://drafts.csswg.org/cssom/#dom-cssstyledeclaration-setproperty
WebIDL::ExceptionOr<void> ResolvedCSSStyleDeclaration::set_property(PropertyID, StringView, StringView)
{
    // 1. If the computed flag is set, then throw a NoModificationAllowedError exception.
    return WebIDL::NoModificationAllowedError::create(realm(), "Cannot modify properties in result of getComputedStyle()"_fly_string);
}

// https://drafts.csswg.org/cssom/#dom-cssstyledeclaration-removeproperty
WebIDL::ExceptionOr<String> ResolvedCSSStyleDeclaration::remove_property(PropertyID)
{
    // 1. If the computed flag is set, then throw a NoModificationAllowedError exception.
    return WebIDL::NoModificationAllowedError::create(realm(), "Cannot remove properties from result of getComputedStyle()"_fly_string);
}

DeprecatedString ResolvedCSSStyleDeclaration::serialized() const
{
    // https://www.w3.org/TR/cssom/#dom-cssstyledeclaration-csstext
    // If the computed flag is set, then return the empty string.

    // NOTE: ResolvedCSSStyleDeclaration is something you would only get from window.getComputedStyle(),
    //       which returns what the spec calls "resolved style". The "computed flag" is always set here.
    return DeprecatedString::empty();
}

// https://drafts.csswg.org/cssom/#dom-cssstyledeclaration-csstext
WebIDL::ExceptionOr<void> ResolvedCSSStyleDeclaration::set_css_text(StringView)
{
    // 1. If the computed flag is set, then throw a NoModificationAllowedError exception.
    return WebIDL::NoModificationAllowedError::create(realm(), "Cannot modify properties in result of getComputedStyle()"_fly_string);
}

}
