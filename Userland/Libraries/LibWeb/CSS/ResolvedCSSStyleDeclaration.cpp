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
#include <LibWeb/CSS/StyleValues/LengthStyleValue.h>
#include <LibWeb/CSS/StyleValues/NumericStyleValue.h>
#include <LibWeb/CSS/StyleValues/PercentageStyleValue.h>
#include <LibWeb/CSS/StyleValues/PositionStyleValue.h>
#include <LibWeb/CSS/StyleValues/RectStyleValue.h>
#include <LibWeb/CSS/StyleValues/ShadowStyleValue.h>
#include <LibWeb/CSS/StyleValues/StyleValueList.h>
#include <LibWeb/CSS/StyleValues/TransformationStyleValue.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Painting/PaintableBox.h>
#include <LibWeb/Painting/StackingContext.h>

namespace Web::CSS {

WebIDL::ExceptionOr<JS::NonnullGCPtr<ResolvedCSSStyleDeclaration>> ResolvedCSSStyleDeclaration::create(DOM::Element& element)
{
    return MUST_OR_THROW_OOM(element.realm().heap().allocate<ResolvedCSSStyleDeclaration>(element.realm(), element));
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

DeprecatedString ResolvedCSSStyleDeclaration::item(size_t index) const
{
    (void)index;
    return {};
}

static ErrorOr<NonnullRefPtr<StyleValue const>> style_value_for_background_property(Layout::NodeWithStyle const& layout_node, Function<ErrorOr<NonnullRefPtr<StyleValue const>>(BackgroundLayerData const&)> callback, Function<ErrorOr<NonnullRefPtr<StyleValue const>>()> default_value)
{
    auto const& background_layers = layout_node.background_layers();
    if (background_layers.is_empty())
        return default_value();
    if (background_layers.size() == 1)
        return callback(background_layers.first());
    StyleValueVector values;
    TRY(values.try_ensure_capacity(background_layers.size()));
    for (auto const& layer : background_layers)
        values.unchecked_append(TRY(callback(layer)));
    return StyleValueList::create(move(values), StyleValueList::Separator::Comma);
}

static ErrorOr<RefPtr<StyleValue>> style_value_for_display(Display display)
{
    if (display.is_none())
        return IdentifierStyleValue::create(ValueID::None);

    if (display.is_outside_and_inside()) {
        // NOTE: Following the precedence rules of “most backwards-compatible, then shortest”,
        //       serialization of equivalent display values uses the “Short display” column.
        if (display == Display::from_short(Display::Short::Block))
            return IdentifierStyleValue::create(ValueID::Block);
        if (display == Display::from_short(Display::Short::FlowRoot))
            return IdentifierStyleValue::create(ValueID::FlowRoot);
        if (display == Display::from_short(Display::Short::Inline))
            return IdentifierStyleValue::create(ValueID::Inline);
        if (display == Display::from_short(Display::Short::InlineBlock))
            return IdentifierStyleValue::create(ValueID::InlineBlock);
        if (display == Display::from_short(Display::Short::RunIn))
            return IdentifierStyleValue::create(ValueID::RunIn);
        if (display == Display::from_short(Display::Short::ListItem))
            return IdentifierStyleValue::create(ValueID::ListItem);
        if (display == Display::from_short(Display::Short::Flex))
            return IdentifierStyleValue::create(ValueID::Flex);
        if (display == Display::from_short(Display::Short::InlineFlex))
            return IdentifierStyleValue::create(ValueID::InlineFlex);
        if (display == Display::from_short(Display::Short::Grid))
            return IdentifierStyleValue::create(ValueID::Grid);
        if (display == Display::from_short(Display::Short::InlineGrid))
            return IdentifierStyleValue::create(ValueID::InlineGrid);
        if (display == Display::from_short(Display::Short::Ruby))
            return IdentifierStyleValue::create(ValueID::Ruby);
        if (display == Display::from_short(Display::Short::Table))
            return IdentifierStyleValue::create(ValueID::Table);
        if (display == Display::from_short(Display::Short::InlineTable))
            return IdentifierStyleValue::create(ValueID::InlineTable);

        StyleValueVector values;
        switch (display.outside()) {
        case Display::Outside::Inline:
            TRY(values.try_append(TRY(IdentifierStyleValue::create(ValueID::Inline))));
            break;
        case Display::Outside::Block:
            TRY(values.try_append(TRY(IdentifierStyleValue::create(ValueID::Block))));
            break;
        case Display::Outside::RunIn:
            TRY(values.try_append(TRY(IdentifierStyleValue::create(ValueID::RunIn))));
            break;
        }
        switch (display.inside()) {
        case Display::Inside::Flow:
            TRY(values.try_append(TRY(IdentifierStyleValue::create(ValueID::Flow))));
            break;
        case Display::Inside::FlowRoot:
            TRY(values.try_append(TRY(IdentifierStyleValue::create(ValueID::FlowRoot))));
            break;
        case Display::Inside::Table:
            TRY(values.try_append(TRY(IdentifierStyleValue::create(ValueID::Table))));
            break;
        case Display::Inside::Flex:
            TRY(values.try_append(TRY(IdentifierStyleValue::create(ValueID::Flex))));
            break;
        case Display::Inside::Grid:
            TRY(values.try_append(TRY(IdentifierStyleValue::create(ValueID::Grid))));
            break;
        case Display::Inside::Ruby:
            TRY(values.try_append(TRY(IdentifierStyleValue::create(ValueID::Ruby))));
            break;
        }

        return StyleValueList::create(move(values), StyleValueList::Separator::Space);
    }

    if (display.is_internal()) {
        switch (display.internal()) {
        case Display::Internal::TableRowGroup:
            return IdentifierStyleValue::create(ValueID::TableRowGroup);
        case Display::Internal::TableHeaderGroup:
            return IdentifierStyleValue::create(ValueID::TableHeaderGroup);
        case Display::Internal::TableFooterGroup:
            return IdentifierStyleValue::create(ValueID::TableFooterGroup);
        case Display::Internal::TableRow:
            return IdentifierStyleValue::create(ValueID::TableRow);
        case Display::Internal::TableCell:
            return IdentifierStyleValue::create(ValueID::TableCell);
        case Display::Internal::TableColumnGroup:
            return IdentifierStyleValue::create(ValueID::TableColumnGroup);
        case Display::Internal::TableColumn:
            return IdentifierStyleValue::create(ValueID::TableColumn);
        case Display::Internal::TableCaption:
            return IdentifierStyleValue::create(ValueID::TableCaption);
        case Display::Internal::RubyBase:
            return IdentifierStyleValue::create(ValueID::RubyBase);
        case Display::Internal::RubyText:
            return IdentifierStyleValue::create(ValueID::RubyText);
        case Display::Internal::RubyBaseContainer:
            return IdentifierStyleValue::create(ValueID::RubyBaseContainer);
        case Display::Internal::RubyTextContainer:
            return IdentifierStyleValue::create(ValueID::RubyTextContainer);
        }
    }

    TODO();
}

static NonnullRefPtr<StyleValue const> value_or_default(Optional<StyleProperty> property, NonnullRefPtr<StyleValue> default_style)
{
    if (property.has_value())
        return property.value().value;
    return default_style;
}

static ErrorOr<NonnullRefPtr<StyleValue const>> style_value_for_length_percentage(LengthPercentage const& length_percentage)
{
    if (length_percentage.is_auto())
        return IdentifierStyleValue::create(ValueID::Auto);
    if (length_percentage.is_percentage())
        return PercentageStyleValue::create(length_percentage.percentage());
    if (length_percentage.is_length())
        return LengthStyleValue::create(length_percentage.length());
    return length_percentage.calculated();
}

static ErrorOr<NonnullRefPtr<StyleValue const>> style_value_for_size(Size const& size)
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
    TODO();
}

ErrorOr<RefPtr<StyleValue const>> ResolvedCSSStyleDeclaration::style_value_for_property(Layout::NodeWithStyle const& layout_node, PropertyID property_id) const
{
    switch (property_id) {
    case PropertyID::AccentColor: {
        auto accent_color = layout_node.computed_values().accent_color();
        if (accent_color.has_value())
            return TRY(ColorStyleValue::create(accent_color.value()));
        return TRY(IdentifierStyleValue::create(ValueID::Auto));
    }
    case PropertyID::AlignContent:
        return TRY(IdentifierStyleValue::create(to_value_id(layout_node.computed_values().align_content())));
    case PropertyID::AlignItems:
        return TRY(IdentifierStyleValue::create(to_value_id(layout_node.computed_values().align_items())));
    case PropertyID::AlignSelf:
        return TRY(IdentifierStyleValue::create(to_value_id(layout_node.computed_values().align_self())));
    case PropertyID::Appearance:
        return TRY(IdentifierStyleValue::create(to_value_id(layout_node.computed_values().appearance())));
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
            value_or_default(maybe_background_color, TRY(InitialStyleValue::the())),
            value_or_default(maybe_background_image, TRY(IdentifierStyleValue::create(ValueID::None))),
            value_or_default(maybe_background_position, TRY(PositionStyleValue::create(TRY(EdgeStyleValue::create(PositionEdge::Left, Length::make_px(0))), TRY(EdgeStyleValue::create(PositionEdge::Top, Length::make_px(0)))))),
            value_or_default(maybe_background_size, TRY(IdentifierStyleValue::create(ValueID::Auto))),
            value_or_default(maybe_background_repeat, TRY(BackgroundRepeatStyleValue::create(Repeat::Repeat, Repeat::Repeat))),
            value_or_default(maybe_background_attachment, TRY(IdentifierStyleValue::create(ValueID::Scroll))),
            value_or_default(maybe_background_origin, TRY(IdentifierStyleValue::create(ValueID::PaddingBox))),
            value_or_default(maybe_background_clip, TRY(IdentifierStyleValue::create(ValueID::BorderBox))));
    }
    case PropertyID::BackgroundAttachment:
        return style_value_for_background_property(
            layout_node,
            [](auto& layer) { return IdentifierStyleValue::create(to_value_id(layer.attachment)); },
            [] { return IdentifierStyleValue::create(ValueID::Scroll); });
    case PropertyID::BackgroundClip:
        return style_value_for_background_property(
            layout_node,
            [](auto& layer) { return IdentifierStyleValue::create(to_value_id(layer.clip)); },
            [] { return IdentifierStyleValue::create(ValueID::BorderBox); });
    case PropertyID::BackgroundColor:
        return ColorStyleValue::create(layout_node.computed_values().background_color());
    case PropertyID::BackgroundImage:
        return style_value_for_background_property(
            layout_node,
            [](auto& layer) -> ErrorOr<NonnullRefPtr<StyleValue const>> {
                if (layer.background_image)
                    return *layer.background_image;
                return IdentifierStyleValue::create(ValueID::None);
            },
            [] { return IdentifierStyleValue::create(ValueID::None); });
    case PropertyID::BackgroundOrigin:
        return style_value_for_background_property(
            layout_node,
            [](auto& layer) { return IdentifierStyleValue::create(to_value_id(layer.origin)); },
            [] { return IdentifierStyleValue::create(ValueID::PaddingBox); });
    case PropertyID::BackgroundPosition:
        return style_value_for_background_property(
            layout_node,
            [](auto& layer) -> ErrorOr<NonnullRefPtr<StyleValue>> {
                return PositionStyleValue::create(
                    TRY(EdgeStyleValue::create(layer.position_edge_x, layer.position_offset_x)),
                    TRY(EdgeStyleValue::create(layer.position_edge_y, layer.position_offset_y)));
            },
            []() -> ErrorOr<NonnullRefPtr<StyleValue>> {
                return PositionStyleValue::create(
                    TRY(EdgeStyleValue::create(PositionEdge::Left, Percentage(0))),
                    TRY(EdgeStyleValue::create(PositionEdge::Top, Percentage(0))));
            });
    case PropertyID::BackgroundPositionX:
        return style_value_for_background_property(
            layout_node,
            [](auto& layer) { return EdgeStyleValue::create(layer.position_edge_x, layer.position_offset_x); },
            [] { return EdgeStyleValue::create(PositionEdge::Left, Percentage(0)); });
    case PropertyID::BackgroundPositionY:
        return style_value_for_background_property(
            layout_node,
            [](auto& layer) { return EdgeStyleValue::create(layer.position_edge_y, layer.position_offset_y); },
            [] { return EdgeStyleValue::create(PositionEdge::Top, Percentage(0)); });
    case PropertyID::BackgroundRepeat:
        return style_value_for_background_property(
            layout_node,
            [](auto& layer) -> ErrorOr<NonnullRefPtr<StyleValue const>> {
                StyleValueVector repeat {
                    TRY(IdentifierStyleValue::create(to_value_id(layer.repeat_x))),
                    TRY(IdentifierStyleValue::create(to_value_id(layer.repeat_y))),
                };
                return StyleValueList::create(move(repeat), StyleValueList::Separator::Space);
            },
            [] { return BackgroundRepeatStyleValue::create(Repeat::Repeat, Repeat::Repeat); });
    case PropertyID::BackgroundSize:
        return style_value_for_background_property(
            layout_node,
            [](auto& layer) -> ErrorOr<NonnullRefPtr<StyleValue>> {
                switch (layer.size_type) {
                case BackgroundSize::Contain:
                    return IdentifierStyleValue::create(ValueID::Contain);
                case BackgroundSize::Cover:
                    return IdentifierStyleValue::create(ValueID::Cover);
                case BackgroundSize::LengthPercentage:
                    return BackgroundSizeStyleValue::create(layer.size_x, layer.size_y);
                }
                VERIFY_NOT_REACHED();
            },
            [] { return IdentifierStyleValue::create(ValueID::Auto); });
    case PropertyID::BorderBottom: {
        auto border = layout_node.computed_values().border_bottom();
        auto width = TRY(LengthStyleValue::create(Length::make_px(border.width)));
        auto style = TRY(IdentifierStyleValue::create(to_value_id(border.line_style)));
        auto color = TRY(ColorStyleValue::create(border.color));
        return BorderStyleValue::create(width, style, color);
    }
    case PropertyID::BorderBottomColor:
        return ColorStyleValue::create(layout_node.computed_values().border_bottom().color);
    case PropertyID::BorderBottomLeftRadius: {
        auto const& border_radius = layout_node.computed_values().border_bottom_left_radius();
        return BorderRadiusStyleValue::create(border_radius.horizontal_radius, border_radius.vertical_radius);
    }
    case PropertyID::BorderBottomRightRadius: {
        auto const& border_radius = layout_node.computed_values().border_bottom_right_radius();
        return BorderRadiusStyleValue::create(border_radius.horizontal_radius, border_radius.vertical_radius);
    }
    case PropertyID::BorderBottomStyle:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().border_bottom().line_style));
    case PropertyID::BorderBottomWidth:
        return LengthStyleValue::create(Length::make_px(layout_node.computed_values().border_bottom().width));
    case PropertyID::BorderCollapse:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().border_collapse()));
    case PropertyID::BorderLeft: {
        auto border = layout_node.computed_values().border_left();
        auto width = TRY(LengthStyleValue::create(Length::make_px(border.width)));
        auto style = TRY(IdentifierStyleValue::create(to_value_id(border.line_style)));
        auto color = TRY(ColorStyleValue::create(border.color));
        return BorderStyleValue::create(width, style, color);
    }
    case PropertyID::BorderLeftColor:
        return ColorStyleValue::create(layout_node.computed_values().border_left().color);
    case PropertyID::BorderLeftStyle:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().border_left().line_style));
    case PropertyID::BorderLeftWidth:
        return LengthStyleValue::create(Length::make_px(layout_node.computed_values().border_left().width));
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
        auto width = TRY(LengthStyleValue::create(Length::make_px(border.width)));
        auto style = TRY(IdentifierStyleValue::create(to_value_id(border.line_style)));
        auto color = TRY(ColorStyleValue::create(border.color));
        return BorderStyleValue::create(width, style, color);
    }
    case PropertyID::BorderRightColor:
        return ColorStyleValue::create(layout_node.computed_values().border_right().color);
    case PropertyID::BorderRightStyle:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().border_right().line_style));
    case PropertyID::BorderRightWidth:
        return LengthStyleValue::create(Length::make_px(layout_node.computed_values().border_right().width));
    case PropertyID::BorderTop: {
        auto border = layout_node.computed_values().border_top();
        auto width = TRY(LengthStyleValue::create(Length::make_px(border.width)));
        auto style = TRY(IdentifierStyleValue::create(to_value_id(border.line_style)));
        auto color = TRY(ColorStyleValue::create(border.color));
        return BorderStyleValue::create(width, style, color);
    }
    case PropertyID::BorderTopColor:
        return ColorStyleValue::create(layout_node.computed_values().border_top().color);
    case PropertyID::BorderTopLeftRadius: {
        auto const& border_radius = layout_node.computed_values().border_top_left_radius();
        return BorderRadiusStyleValue::create(border_radius.horizontal_radius, border_radius.vertical_radius);
    }
    case PropertyID::BorderTopRightRadius: {
        auto const& border_radius = layout_node.computed_values().border_top_right_radius();
        return BorderRadiusStyleValue::create(border_radius.horizontal_radius, border_radius.vertical_radius);
    }
    case PropertyID::BorderTopStyle:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().border_top().line_style));
    case PropertyID::BorderTopWidth:
        return LengthStyleValue::create(Length::make_px(layout_node.computed_values().border_top().width));
    case PropertyID::BoxShadow: {
        auto box_shadow_layers = layout_node.computed_values().box_shadow();
        if (box_shadow_layers.is_empty())
            return nullptr;

        auto make_box_shadow_style_value = [](ShadowData const& data) {
            return ShadowStyleValue::create(data.color, data.offset_x, data.offset_y, data.blur_radius, data.spread_distance, data.placement);
        };

        if (box_shadow_layers.size() == 1)
            return make_box_shadow_style_value(box_shadow_layers.first());

        StyleValueVector box_shadow;
        TRY(box_shadow.try_ensure_capacity(box_shadow_layers.size()));
        for (auto const& layer : box_shadow_layers)
            box_shadow.unchecked_append(TRY(make_box_shadow_style_value(layer)));
        return StyleValueList::create(move(box_shadow), StyleValueList::Separator::Comma);
    }
    case PropertyID::BoxSizing:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().box_sizing()));
    case PropertyID::Bottom:
        return style_value_for_length_percentage(layout_node.computed_values().inset().bottom());
    case PropertyID::Clear:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().clear()));
    case PropertyID::Clip:
        return RectStyleValue::create(layout_node.computed_values().clip().to_rect());
    case PropertyID::Color:
        return ColorStyleValue::create(layout_node.computed_values().color());
    case PropertyID::ColumnGap:
        return style_value_for_size(layout_node.computed_values().column_gap());
    case PropertyID::Cursor:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().cursor()));
    case PropertyID::Display:
        return style_value_for_display(layout_node.display());
    case PropertyID::FlexBasis: {
        switch (layout_node.computed_values().flex_basis().type) {
        case FlexBasis::Content:
            return IdentifierStyleValue::create(ValueID::Content);
        case FlexBasis::LengthPercentage:
            return style_value_for_length_percentage(*layout_node.computed_values().flex_basis().length_percentage);
        case FlexBasis::Auto:
            return IdentifierStyleValue::create(ValueID::Auto);
        default:
            VERIFY_NOT_REACHED();
        }
        break;
    case PropertyID::FlexDirection:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().flex_direction()));
    case PropertyID::FlexGrow:
        return NumericStyleValue::create_float(layout_node.computed_values().flex_grow());
    case PropertyID::FlexShrink:
        return NumericStyleValue::create_float(layout_node.computed_values().flex_shrink());
    case PropertyID::FlexWrap:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().flex_wrap()));
    case PropertyID::Float:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().float_()));
    case PropertyID::FontSize:
        return LengthStyleValue::create(Length::make_px(layout_node.computed_values().font_size()));
    case PropertyID::FontVariant: {
        auto font_variant = layout_node.computed_values().font_variant();
        switch (font_variant) {
        case FontVariant::Normal:
            return IdentifierStyleValue::create(ValueID::Normal);
        case FontVariant::SmallCaps:
            return IdentifierStyleValue::create(ValueID::SmallCaps);
        }
        VERIFY_NOT_REACHED();
    }
    case PropertyID::FontWeight:
        return NumericStyleValue::create_integer(layout_node.computed_values().font_weight());
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
    case PropertyID::GridColumnEnd:
        return GridTrackPlacementStyleValue::create(layout_node.computed_values().grid_column_end());
    case PropertyID::GridColumnStart:
        return GridTrackPlacementStyleValue::create(layout_node.computed_values().grid_column_start());
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
    case PropertyID::GridRowEnd:
        return GridTrackPlacementStyleValue::create(layout_node.computed_values().grid_row_end());
    case PropertyID::GridRowStart:
        return GridTrackPlacementStyleValue::create(layout_node.computed_values().grid_row_start());
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
    case PropertyID::GridTemplateColumns:
        return GridTrackSizeListStyleValue::create(layout_node.computed_values().grid_template_columns());
    case PropertyID::GridTemplateRows:
        return GridTrackSizeListStyleValue::create(layout_node.computed_values().grid_template_rows());
    case PropertyID::GridTemplateAreas:
        return GridTemplateAreaStyleValue::create(layout_node.computed_values().grid_template_areas());
    case PropertyID::Height:
        return style_value_for_size(layout_node.computed_values().height());
    case PropertyID::ImageRendering:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().image_rendering()));
    case PropertyID::JustifyContent:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().justify_content()));
    case PropertyID::Left:
        return style_value_for_length_percentage(layout_node.computed_values().inset().left());
    case PropertyID::LineHeight:
        return LengthStyleValue::create(Length::make_px(layout_node.line_height()));
    case PropertyID::ListStyleType:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().list_style_type()));
    case PropertyID::Margin: {
        auto margin = layout_node.computed_values().margin();
        auto values = StyleValueVector {};
        TRY(values.try_ensure_capacity(4));
        values.unchecked_append(TRY(style_value_for_length_percentage(margin.top())));
        values.unchecked_append(TRY(style_value_for_length_percentage(margin.right())));
        values.unchecked_append(TRY(style_value_for_length_percentage(margin.bottom())));
        values.unchecked_append(TRY(style_value_for_length_percentage(margin.left())));
        return StyleValueList::create(move(values), StyleValueList::Separator::Space);
    }
    case PropertyID::MarginBottom:
        return style_value_for_length_percentage(layout_node.computed_values().margin().bottom());
    case PropertyID::MarginLeft:
        return style_value_for_length_percentage(layout_node.computed_values().margin().left());
    case PropertyID::MarginRight:
        return style_value_for_length_percentage(layout_node.computed_values().margin().right());
    case PropertyID::MarginTop:
        return style_value_for_length_percentage(layout_node.computed_values().margin().top());
    case PropertyID::MaxHeight:
        return style_value_for_size(layout_node.computed_values().max_height());
    case PropertyID::MaxWidth:
        return style_value_for_size(layout_node.computed_values().max_width());
    case PropertyID::MinHeight:
        return style_value_for_size(layout_node.computed_values().min_height());
    case PropertyID::MinWidth:
        return style_value_for_size(layout_node.computed_values().min_width());
    case PropertyID::Opacity:
        return NumericStyleValue::create_float(layout_node.computed_values().opacity());
    case PropertyID::Order:
        return NumericStyleValue::create_integer(layout_node.computed_values().order());
    case PropertyID::OverflowX:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().overflow_x()));
    case PropertyID::OverflowY:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().overflow_y()));
    case PropertyID::Padding: {
        auto padding = layout_node.computed_values().padding();
        auto values = StyleValueVector {};
        TRY(values.try_ensure_capacity(4));
        values.unchecked_append(TRY(style_value_for_length_percentage(padding.top())));
        values.unchecked_append(TRY(style_value_for_length_percentage(padding.right())));
        values.unchecked_append(TRY(style_value_for_length_percentage(padding.bottom())));
        values.unchecked_append(TRY(style_value_for_length_percentage(padding.left())));
        return StyleValueList::create(move(values), StyleValueList::Separator::Space);
    }
    case PropertyID::PaddingBottom:
        return style_value_for_length_percentage(layout_node.computed_values().padding().bottom());
    case PropertyID::PaddingLeft:
        return style_value_for_length_percentage(layout_node.computed_values().padding().left());
    case PropertyID::PaddingRight:
        return style_value_for_length_percentage(layout_node.computed_values().padding().right());
    case PropertyID::PaddingTop:
        return style_value_for_length_percentage(layout_node.computed_values().padding().top());
    case PropertyID::Position:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().position()));
    case PropertyID::Right:
        return style_value_for_length_percentage(layout_node.computed_values().inset().right());
    case PropertyID::RowGap:
        return style_value_for_size(layout_node.computed_values().row_gap());
    case PropertyID::TextAlign:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().text_align()));
    case PropertyID::TextDecorationLine: {
        auto text_decoration_lines = layout_node.computed_values().text_decoration_line();
        if (text_decoration_lines.is_empty())
            return IdentifierStyleValue::create(ValueID::None);
        StyleValueVector style_values;
        TRY(style_values.try_ensure_capacity(text_decoration_lines.size()));
        for (auto const& line : text_decoration_lines) {
            style_values.unchecked_append(TRY(IdentifierStyleValue::create(to_value_id(line))));
        }
        return StyleValueList::create(move(style_values), StyleValueList::Separator::Space);
    }
    case PropertyID::TextDecorationStyle:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().text_decoration_style()));
    case PropertyID::TextTransform:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().text_transform()));
    case PropertyID::Top:
        return style_value_for_length_percentage(layout_node.computed_values().inset().top());
    case PropertyID::Transform: {
        // NOTE: The computed value for `transform` serializes as a single `matrix(...)` value, instead of
        //       the original list of transform functions. So, we produce a StyleValue for that.
        //       https://www.w3.org/TR/css-transforms-1/#serialization-of-the-computed-value
        auto transformations = layout_node.computed_values().transformations();
        if (transformations.is_empty())
            return IdentifierStyleValue::create(ValueID::None);

        // The transform matrix is held by the StackingContext, so we need to make sure we have one first.
        auto const* viewport = layout_node.document().layout_node();
        VERIFY(viewport);
        const_cast<Layout::Viewport&>(*viewport).build_stacking_context_tree_if_needed();

        VERIFY(layout_node.paintable());
        auto const& paintable_box = verify_cast<Painting::PaintableBox const>(layout_node.paintable());
        VERIFY(paintable_box->stacking_context());

        // FIXME: This needs to serialize to matrix3d if the transformation matrix is a 3D matrix.
        //        https://w3c.github.io/csswg-drafts/css-transforms-2/#serialization-of-the-computed-value
        auto affine_matrix = paintable_box->stacking_context()->affine_transform_matrix();

        StyleValueVector parameters;
        TRY(parameters.try_ensure_capacity(6));
        parameters.unchecked_append(TRY(NumericStyleValue::create_float(affine_matrix.a())));
        parameters.unchecked_append(TRY(NumericStyleValue::create_float(affine_matrix.b())));
        parameters.unchecked_append(TRY(NumericStyleValue::create_float(affine_matrix.c())));
        parameters.unchecked_append(TRY(NumericStyleValue::create_float(affine_matrix.d())));
        parameters.unchecked_append(TRY(NumericStyleValue::create_float(affine_matrix.e())));
        parameters.unchecked_append(TRY(NumericStyleValue::create_float(affine_matrix.f())));

        NonnullRefPtr<StyleValue> matrix_function = TRY(TransformationStyleValue::create(TransformFunction::Matrix, move(parameters)));
        // Elsewhere we always store the transform property's value as a StyleValueList of TransformationStyleValues,
        // so this is just for consistency.
        StyleValueVector matrix_functions { matrix_function };
        return StyleValueList::create(move(matrix_functions), StyleValueList::Separator::Space);
    }
    case PropertyID::VerticalAlign:
        if (auto const* length_percentage = layout_node.computed_values().vertical_align().get_pointer<LengthPercentage>()) {
            return style_value_for_length_percentage(*length_percentage);
        }
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().vertical_align().get<VerticalAlign>()));
    case PropertyID::WhiteSpace:
        return IdentifierStyleValue::create(to_value_id(layout_node.computed_values().white_space()));
    case PropertyID::Width:
        return style_value_for_size(layout_node.computed_values().width());
    case PropertyID::ZIndex: {
        auto maybe_z_index = layout_node.computed_values().z_index();
        if (!maybe_z_index.has_value())
            return nullptr;
        return NumericStyleValue::create_integer(maybe_z_index.release_value());
    }
    case PropertyID::Invalid:
        return IdentifierStyleValue::create(ValueID::Invalid);
    case PropertyID::Custom:
        dbgln_if(LIBWEB_CSS_DEBUG, "Computed style for custom properties was requested (?)");
        return nullptr;
    default:
        dbgln_if(LIBWEB_CSS_DEBUG, "FIXME: Computed style for the '{}' property was requested", string_from_property_id(property_id));
        return nullptr;
    }
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
    auto value = style_value_for_property(layout_node, property_id).release_value_but_fixme_should_propagate_errors();
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
    return WebIDL::NoModificationAllowedError::create(realm(), "Cannot modify properties in result of getComputedStyle()");
}

// https://drafts.csswg.org/cssom/#dom-cssstyledeclaration-removeproperty
WebIDL::ExceptionOr<DeprecatedString> ResolvedCSSStyleDeclaration::remove_property(PropertyID)
{
    // 1. If the computed flag is set, then throw a NoModificationAllowedError exception.
    return WebIDL::NoModificationAllowedError::create(realm(), "Cannot remove properties from result of getComputedStyle()");
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
    return WebIDL::NoModificationAllowedError::create(realm(), "Cannot modify properties in result of getComputedStyle()");
}

}
