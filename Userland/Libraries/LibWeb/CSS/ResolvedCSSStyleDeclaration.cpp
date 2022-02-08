/*
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/NonnullRefPtr.h>
#include <LibWeb/CSS/ResolvedCSSStyleDeclaration.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>

namespace Web::CSS {

ResolvedCSSStyleDeclaration::ResolvedCSSStyleDeclaration(DOM::Element& element)
    : m_element(element)
{
}

ResolvedCSSStyleDeclaration::~ResolvedCSSStyleDeclaration()
{
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

static CSS::ValueID to_css_value_id(CSS::BoxSizing value)
{
    switch (value) {
    case CSS::BoxSizing::BorderBox:
        return CSS::ValueID::BorderBox;
    case CSS::BoxSizing::ContentBox:
        return CSS::ValueID::ContentBox;
    }
    VERIFY_NOT_REACHED();
}

static RefPtr<StyleValue> style_value_for_display(CSS::Display display)
{
    if (display.is_none())
        return IdentifierStyleValue::create(CSS::ValueID::None);

    if (display.it_outside_and_inside()) {
        NonnullRefPtrVector<StyleValue> values;
        switch (display.outside()) {
        case CSS::Display::Outside::Inline:
            values.append(IdentifierStyleValue::create(CSS::ValueID::Inline));
            break;
        case CSS::Display::Outside::Block:
            values.append(IdentifierStyleValue::create(CSS::ValueID::Block));
            break;
        case CSS::Display::Outside::RunIn:
            values.append(IdentifierStyleValue::create(CSS::ValueID::RunIn));
            break;
        }
        switch (display.inside()) {
        case CSS::Display::Inside::Flow:
            values.append(IdentifierStyleValue::create(CSS::ValueID::Flow));
            break;
        case CSS::Display::Inside::FlowRoot:
            values.append(IdentifierStyleValue::create(CSS::ValueID::FlowRoot));
            break;
        case CSS::Display::Inside::Table:
            values.append(IdentifierStyleValue::create(CSS::ValueID::Table));
            break;
        case CSS::Display::Inside::Flex:
            values.append(IdentifierStyleValue::create(CSS::ValueID::Flex));
            break;
        case CSS::Display::Inside::Grid:
            values.append(IdentifierStyleValue::create(CSS::ValueID::Grid));
            break;
        case CSS::Display::Inside::Ruby:
            values.append(IdentifierStyleValue::create(CSS::ValueID::Ruby));
            break;
        }

        return StyleValueList::create(move(values), StyleValueList::Separator::Space);
    }

    if (display.is_internal()) {
        switch (display.internal()) {
        case CSS::Display::Internal::TableRowGroup:
            return IdentifierStyleValue::create(CSS::ValueID::TableRowGroup);
        case CSS::Display::Internal::TableHeaderGroup:
            return IdentifierStyleValue::create(CSS::ValueID::TableHeaderGroup);
        case CSS::Display::Internal::TableFooterGroup:
            return IdentifierStyleValue::create(CSS::ValueID::TableFooterGroup);
        case CSS::Display::Internal::TableRow:
            return IdentifierStyleValue::create(CSS::ValueID::TableRow);
        case CSS::Display::Internal::TableCell:
            return IdentifierStyleValue::create(CSS::ValueID::TableCell);
        case CSS::Display::Internal::TableColumnGroup:
            return IdentifierStyleValue::create(CSS::ValueID::TableColumnGroup);
        case CSS::Display::Internal::TableColumn:
            return IdentifierStyleValue::create(CSS::ValueID::TableColumn);
        case CSS::Display::Internal::TableCaption:
            return IdentifierStyleValue::create(CSS::ValueID::TableCaption);
        case CSS::Display::Internal::RubyBase:
            return IdentifierStyleValue::create(CSS::ValueID::RubyBase);
        case CSS::Display::Internal::RubyText:
            return IdentifierStyleValue::create(CSS::ValueID::RubyText);
        case CSS::Display::Internal::RubyBaseContainer:
            return IdentifierStyleValue::create(CSS::ValueID::RubyBaseContainer);
        case CSS::Display::Internal::RubyTextContainer:
            return IdentifierStyleValue::create(CSS::ValueID::RubyTextContainer);
        }
    }

    TODO();
}

static CSS::ValueID to_css_value_id(CSS::Float value)
{
    switch (value) {
    case Float::None:
        return CSS::ValueID::None;
    case Float::Left:
        return CSS::ValueID::Left;
    case Float::Right:
        return CSS::ValueID::Right;
    }
    VERIFY_NOT_REACHED();
}

static CSS::ValueID to_css_value_id(CSS::Clear value)
{
    switch (value) {
    case Clear::None:
        return CSS::ValueID::None;
    case Clear::Left:
        return CSS::ValueID::Left;
    case Clear::Right:
        return CSS::ValueID::Right;
    case Clear::Both:
        return CSS::ValueID::Both;
    }
    VERIFY_NOT_REACHED();
}

static CSS::ValueID to_css_value_id(CSS::TextDecorationLine value)
{
    switch (value) {
    case TextDecorationLine::None:
        return CSS::ValueID::None;
    case TextDecorationLine::Underline:
        return CSS::ValueID::Underline;
    case TextDecorationLine::Overline:
        return CSS::ValueID::Overline;
    case TextDecorationLine::LineThrough:
        return CSS::ValueID::LineThrough;
    case TextDecorationLine::Blink:
        return CSS::ValueID::Blink;
    }
    VERIFY_NOT_REACHED();
}

static CSS::ValueID to_css_value_id(CSS::TextDecorationStyle value)
{
    switch (value) {
    case TextDecorationStyle::Solid:
        return CSS::ValueID::Solid;
    case TextDecorationStyle::Double:
        return CSS::ValueID::Double;
    case TextDecorationStyle::Dotted:
        return CSS::ValueID::Dotted;
    case TextDecorationStyle::Dashed:
        return CSS::ValueID::Dashed;
    case TextDecorationStyle::Wavy:
        return CSS::ValueID::Wavy;
    }
    VERIFY_NOT_REACHED();
}

static CSS::ValueID to_css_value_id(CSS::Cursor value)
{
    switch (value) {
    case Cursor::Auto:
        return CSS::ValueID::Auto;
    case Cursor::Default:
        return CSS::ValueID::Default;
    case Cursor::None:
        return CSS::ValueID::None;
    case Cursor::ContextMenu:
        return CSS::ValueID::ContextMenu;
    case Cursor::Help:
        return CSS::ValueID::Help;
    case Cursor::Pointer:
        return CSS::ValueID::Pointer;
    case Cursor::Progress:
        return CSS::ValueID::Progress;
    case Cursor::Wait:
        return CSS::ValueID::Wait;
    case Cursor::Cell:
        return CSS::ValueID::Cell;
    case Cursor::Crosshair:
        return CSS::ValueID::Crosshair;
    case Cursor::Text:
        return CSS::ValueID::Text;
    case Cursor::VerticalText:
        return CSS::ValueID::VerticalText;
    case Cursor::Alias:
        return CSS::ValueID::Alias;
    case Cursor::Copy:
        return CSS::ValueID::Copy;
    case Cursor::Move:
        return CSS::ValueID::Move;
    case Cursor::NoDrop:
        return CSS::ValueID::NoDrop;
    case Cursor::NotAllowed:
        return CSS::ValueID::NotAllowed;
    case Cursor::Grab:
        return CSS::ValueID::Grab;
    case Cursor::Grabbing:
        return CSS::ValueID::Grabbing;
    case Cursor::EResize:
        return CSS::ValueID::EResize;
    case Cursor::NResize:
        return CSS::ValueID::NResize;
    case Cursor::NeResize:
        return CSS::ValueID::NeResize;
    case Cursor::NwResize:
        return CSS::ValueID::NwResize;
    case Cursor::SResize:
        return CSS::ValueID::SResize;
    case Cursor::SeResize:
        return CSS::ValueID::SeResize;
    case Cursor::SwResize:
        return CSS::ValueID::SwResize;
    case Cursor::WResize:
        return CSS::ValueID::WResize;
    case Cursor::EwResize:
        return CSS::ValueID::EwResize;
    case Cursor::NsResize:
        return CSS::ValueID::NsResize;
    case Cursor::NeswResize:
        return CSS::ValueID::NeswResize;
    case Cursor::NwseResize:
        return CSS::ValueID::NwseResize;
    case Cursor::ColResize:
        return CSS::ValueID::ColResize;
    case Cursor::RowResize:
        return CSS::ValueID::RowResize;
    case Cursor::AllScroll:
        return CSS::ValueID::AllScroll;
    case Cursor::ZoomIn:
        return CSS::ValueID::ZoomIn;
    case Cursor::ZoomOut:
        return CSS::ValueID::ZoomOut;
    }
    VERIFY_NOT_REACHED();
}

static CSS::ValueID to_css_value_id(CSS::TextAlign value)
{
    switch (value) {
    case TextAlign::Left:
        return CSS::ValueID::Left;
    case TextAlign::Center:
        return CSS::ValueID::Center;
    case TextAlign::Right:
        return CSS::ValueID::Right;
    case TextAlign::Justify:
        return CSS::ValueID::Justify;
    case TextAlign::LibwebCenter:
        return CSS::ValueID::LibwebCenter;
    }
    VERIFY_NOT_REACHED();
}

static CSS::ValueID to_css_value_id(CSS::TextTransform value)
{
    switch (value) {
    case TextTransform::None:
        return CSS::ValueID::None;
    case TextTransform::Capitalize:
        return CSS::ValueID::Capitalize;
    case TextTransform::Uppercase:
        return CSS::ValueID::Uppercase;
    case TextTransform::Lowercase:
        return CSS::ValueID::Lowercase;
    case TextTransform::FullWidth:
        return CSS::ValueID::FullWidth;
    case TextTransform::FullSizeKana:
        return CSS::ValueID::FullSizeKana;
    }
    VERIFY_NOT_REACHED();
}

static CSS::ValueID to_css_value_id(CSS::Position value)
{
    switch (value) {
    case Position::Static:
        return CSS::ValueID::Static;
    case Position::Relative:
        return CSS::ValueID::Relative;
    case Position::Absolute:
        return CSS::ValueID::Absolute;
    case Position::Fixed:
        return CSS::ValueID::Fixed;
    case Position::Sticky:
        return CSS::ValueID::Sticky;
    }
    VERIFY_NOT_REACHED();
}

static CSS::ValueID to_css_value_id(CSS::WhiteSpace value)
{
    switch (value) {
    case WhiteSpace::Normal:
        return CSS::ValueID::Normal;
    case WhiteSpace::Pre:
        return CSS::ValueID::Pre;
    case WhiteSpace::Nowrap:
        return CSS::ValueID::Nowrap;
    case WhiteSpace::PreLine:
        return CSS::ValueID::PreLine;
    case WhiteSpace::PreWrap:
        return CSS::ValueID::PreWrap;
    }
    VERIFY_NOT_REACHED();
}

static CSS::ValueID to_css_value_id(CSS::FlexDirection value)
{
    switch (value) {
    case FlexDirection::Row:
        return CSS::ValueID::Row;
    case FlexDirection::RowReverse:
        return CSS::ValueID::RowReverse;
    case FlexDirection::Column:
        return CSS::ValueID::Column;
    case FlexDirection::ColumnReverse:
        return CSS::ValueID::ColumnReverse;
    }
    VERIFY_NOT_REACHED();
}

static CSS::ValueID to_css_value_id(CSS::FlexWrap value)
{
    switch (value) {
    case FlexWrap::Nowrap:
        return CSS::ValueID::Nowrap;
    case FlexWrap::Wrap:
        return CSS::ValueID::Wrap;
    case FlexWrap::WrapReverse:
        return CSS::ValueID::WrapReverse;
    }
    VERIFY_NOT_REACHED();
}

static CSS::ValueID to_css_value_id(CSS::JustifyContent value)
{
    switch (value) {
    case JustifyContent::FlexStart:
        return CSS::ValueID::FlexStart;
    case JustifyContent::FlexEnd:
        return CSS::ValueID::FlexEnd;
    case JustifyContent::Center:
        return CSS::ValueID::Center;
    case JustifyContent::SpaceBetween:
        return CSS::ValueID::SpaceBetween;
    case JustifyContent::SpaceAround:
        return CSS::ValueID::SpaceAround;
    }
    VERIFY_NOT_REACHED();
}

static CSS::ValueID to_css_value_id(CSS::Overflow value)
{
    switch (value) {
    case Overflow::Auto:
        return CSS::ValueID::Auto;
    case Overflow::Clip:
        return CSS::ValueID::Clip;
    case Overflow::Hidden:
        return CSS::ValueID::Hidden;
    case Overflow::Scroll:
        return CSS::ValueID::Scroll;
    case Overflow::Visible:
        return CSS::ValueID::Visible;
    }
    VERIFY_NOT_REACHED();
}

static CSS::ValueID to_css_value_id(CSS::ListStyleType value)
{
    switch (value) {
    case ListStyleType::None:
        return CSS::ValueID::None;
    case ListStyleType::Disc:
        return CSS::ValueID::Disc;
    case ListStyleType::Circle:
        return CSS::ValueID::Circle;
    case ListStyleType::Square:
        return CSS::ValueID::Square;
    case ListStyleType::Decimal:
        return CSS::ValueID::Decimal;
    case ListStyleType::DecimalLeadingZero:
        return CSS::ValueID::DecimalLeadingZero;
    case ListStyleType::LowerAlpha:
        return CSS::ValueID::LowerAlpha;
    case ListStyleType::LowerLatin:
        return CSS::ValueID::LowerLatin;
    case ListStyleType::LowerRoman:
        return CSS::ValueID::LowerRoman;
    case ListStyleType::UpperAlpha:
        return CSS::ValueID::UpperAlpha;
    case ListStyleType::UpperLatin:
        return CSS::ValueID::UpperLatin;
    case ListStyleType::UpperRoman:
        return CSS::ValueID::UpperRoman;
    }
    VERIFY_NOT_REACHED();
}

static CSS::ValueID to_css_value_id(CSS::LineStyle value)
{
    switch (value) {
    case CSS::LineStyle::None:
        return CSS::ValueID::None;
    case CSS::LineStyle::Hidden:
        return CSS::ValueID::Hidden;
    case CSS::LineStyle::Dotted:
        return CSS::ValueID::Dotted;
    case CSS::LineStyle::Dashed:
        return CSS::ValueID::Dashed;
    case CSS::LineStyle::Solid:
        return CSS::ValueID::Solid;
    case CSS::LineStyle::Double:
        return CSS::ValueID::Double;
    case CSS::LineStyle::Groove:
        return CSS::ValueID::Groove;
    case CSS::LineStyle::Ridge:
        return CSS::ValueID::Ridge;
    case CSS::LineStyle::Inset:
        return CSS::ValueID::Inset;
    case CSS::LineStyle::Outset:
        return CSS::ValueID::Outset;
    }
    VERIFY_NOT_REACHED();
}

static NonnullRefPtr<StyleValue> value_or_default(Optional<StyleProperty> property, NonnullRefPtr<StyleValue> default_style)
{
    if (property.has_value())
        return property.value().value;
    return default_style;
}

static NonnullRefPtr<StyleValue> style_value_for_length_percentage(LengthPercentage const& length_percentage)
{
    if (length_percentage.is_percentage())
        return PercentageStyleValue::create(length_percentage.percentage());
    return LengthStyleValue::create(length_percentage.length());
}

RefPtr<StyleValue> ResolvedCSSStyleDeclaration::style_value_for_property(Layout::NodeWithStyle const& layout_node, PropertyID property_id) const
{
    switch (property_id) {
    case CSS::PropertyID::Float:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().float_()));
    case CSS::PropertyID::Clear:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().clear()));
    case CSS::PropertyID::Cursor:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().cursor()));
    case CSS::PropertyID::Display:
        return style_value_for_display(layout_node.computed_values().display());
    case CSS::PropertyID::ZIndex: {
        auto maybe_z_index = layout_node.computed_values().z_index();
        if (!maybe_z_index.has_value())
            return {};
        return NumericStyleValue::create_integer(maybe_z_index.release_value());
    }
    case CSS::PropertyID::TextAlign:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().text_align()));
    case CSS::PropertyID::TextDecorationLine:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().text_decoration_line()));
    case CSS::PropertyID::TextDecorationStyle:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().text_decoration_style()));
    case CSS::PropertyID::TextTransform:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().text_transform()));
    case CSS::PropertyID::Position:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().position()));
    case CSS::PropertyID::WhiteSpace:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().white_space()));
    case CSS::PropertyID::FlexDirection:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().flex_direction()));
    case CSS::PropertyID::FlexWrap:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().flex_wrap()));
    case CSS::PropertyID::FlexBasis: {
        switch (layout_node.computed_values().flex_basis().type) {
        case FlexBasis::Content:
            return IdentifierStyleValue::create(CSS::ValueID::Content);
        case FlexBasis::LengthPercentage:
            return style_value_for_length_percentage(*layout_node.computed_values().flex_basis().length_percentage);
        case FlexBasis::Auto:
            return IdentifierStyleValue::create(CSS::ValueID::Auto);
        default:
            VERIFY_NOT_REACHED();
        }
        break;
    case CSS::PropertyID::FlexGrow:
        return NumericStyleValue::create_float(layout_node.computed_values().flex_grow());
    case CSS::PropertyID::FlexShrink:
        return NumericStyleValue::create_float(layout_node.computed_values().flex_shrink());
    case CSS::PropertyID::Opacity:
        return NumericStyleValue::create_float(layout_node.computed_values().opacity());
    case CSS::PropertyID::JustifyContent:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().justify_content()));
    case CSS::PropertyID::BoxShadow: {
        auto box_shadow_layers = layout_node.computed_values().box_shadow();
        if (box_shadow_layers.is_empty())
            return {};

        auto make_box_shadow_style_value = [](BoxShadowData const& data) {
            return BoxShadowStyleValue::create(data.color, data.offset_x, data.offset_y, data.blur_radius, data.spread_distance, data.placement);
        };

        if (box_shadow_layers.size() == 1)
            return make_box_shadow_style_value(box_shadow_layers.first());

        NonnullRefPtrVector<StyleValue> box_shadow;
        box_shadow.ensure_capacity(box_shadow_layers.size());
        for (auto const& layer : box_shadow_layers)
            box_shadow.append(make_box_shadow_style_value(layer));
        return StyleValueList::create(move(box_shadow), StyleValueList::Separator::Comma);
    }
    case CSS::PropertyID::Width:
        return style_value_for_length_percentage(layout_node.computed_values().width());
    case CSS::PropertyID::MinWidth:
        if (layout_node.computed_values().min_width().is_length() && layout_node.computed_values().min_width().length().is_undefined_or_auto())
            return IdentifierStyleValue::create(CSS::ValueID::Auto);
        return style_value_for_length_percentage(layout_node.computed_values().min_width());
    case CSS::PropertyID::MaxWidth:
        if (layout_node.computed_values().max_width().is_length() && layout_node.computed_values().max_width().length().is_undefined())
            return IdentifierStyleValue::create(CSS::ValueID::None);
        return style_value_for_length_percentage(layout_node.computed_values().max_width());
    case CSS::PropertyID::Height:
        return style_value_for_length_percentage(layout_node.computed_values().height());
    case CSS::PropertyID::MinHeight:
        if (layout_node.computed_values().min_height().is_length() && layout_node.computed_values().min_height().length().is_undefined_or_auto())
            return IdentifierStyleValue::create(CSS::ValueID::Auto);
        return style_value_for_length_percentage(layout_node.computed_values().min_height());
    case CSS::PropertyID::MaxHeight:
        if (layout_node.computed_values().max_height().is_length() && layout_node.computed_values().max_height().length().is_undefined())
            return IdentifierStyleValue::create(CSS::ValueID::None);
        return style_value_for_length_percentage(layout_node.computed_values().max_height());
    case CSS::PropertyID::Margin: {
        auto margin = layout_node.computed_values().margin();
        auto values = NonnullRefPtrVector<StyleValue> {};
        values.append(style_value_for_length_percentage(margin.top));
        values.append(style_value_for_length_percentage(margin.right));
        values.append(style_value_for_length_percentage(margin.bottom));
        values.append(style_value_for_length_percentage(margin.left));
        return StyleValueList::create(move(values), StyleValueList::Separator::Space);
    }
    case CSS::PropertyID::MarginTop:
        return style_value_for_length_percentage(layout_node.computed_values().margin().top);
    case CSS::PropertyID::MarginRight:
        return style_value_for_length_percentage(layout_node.computed_values().margin().right);
    case CSS::PropertyID::MarginBottom:
        return style_value_for_length_percentage(layout_node.computed_values().margin().bottom);
    case CSS::PropertyID::MarginLeft:
        return style_value_for_length_percentage(layout_node.computed_values().margin().left);
    case CSS::PropertyID::Padding: {
        auto padding = layout_node.computed_values().padding();
        auto values = NonnullRefPtrVector<StyleValue> {};
        values.append(style_value_for_length_percentage(padding.top));
        values.append(style_value_for_length_percentage(padding.right));
        values.append(style_value_for_length_percentage(padding.bottom));
        values.append(style_value_for_length_percentage(padding.left));
        return StyleValueList::create(move(values), StyleValueList::Separator::Space);
    }
    case CSS::PropertyID::PaddingTop:
        return style_value_for_length_percentage(layout_node.computed_values().padding().top);
    case CSS::PropertyID::PaddingRight:
        return style_value_for_length_percentage(layout_node.computed_values().padding().right);
    case CSS::PropertyID::PaddingBottom:
        return style_value_for_length_percentage(layout_node.computed_values().padding().bottom);
    case CSS::PropertyID::PaddingLeft:
        return style_value_for_length_percentage(layout_node.computed_values().padding().left);
    case CSS::PropertyID::BorderRadius: {
        auto maybe_top_left_radius = property(CSS::PropertyID::BorderTopLeftRadius);
        auto maybe_top_right_radius = property(CSS::PropertyID::BorderTopRightRadius);
        auto maybe_bottom_left_radius = property(CSS::PropertyID::BorderBottomLeftRadius);
        auto maybe_bottom_right_radius = property(CSS::PropertyID::BorderBottomRightRadius);
        RefPtr<BorderRadiusStyleValue> top_left_radius, top_right_radius, bottom_left_radius, bottom_right_radius;
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

        return CombinedBorderRadiusStyleValue::create(top_left_radius.release_nonnull(), top_right_radius.release_nonnull(), bottom_right_radius.release_nonnull(), bottom_left_radius.release_nonnull());
    }
    // FIXME: The two radius components are not yet stored, as we currently don't actually render them.
    case CSS::PropertyID::BorderBottomLeftRadius:
        return BorderRadiusStyleValue::create(layout_node.computed_values().border_bottom_left_radius(), layout_node.computed_values().border_bottom_left_radius());
    case CSS::PropertyID::BorderBottomRightRadius:
        return BorderRadiusStyleValue::create(layout_node.computed_values().border_bottom_right_radius(), layout_node.computed_values().border_bottom_right_radius());
    case CSS::PropertyID::BorderTopLeftRadius:
        return BorderRadiusStyleValue::create(layout_node.computed_values().border_top_left_radius(), layout_node.computed_values().border_top_left_radius());
    case CSS::PropertyID::BorderTopRightRadius:
        return BorderRadiusStyleValue::create(layout_node.computed_values().border_top_right_radius(), layout_node.computed_values().border_top_right_radius());
    case CSS::PropertyID::BorderTopWidth:
        return LengthStyleValue::create(Length::make_px(layout_node.computed_values().border_top().width));
    case CSS::PropertyID::BorderRightWidth:
        return LengthStyleValue::create(Length::make_px(layout_node.computed_values().border_right().width));
    case CSS::PropertyID::BorderBottomWidth:
        return LengthStyleValue::create(Length::make_px(layout_node.computed_values().border_bottom().width));
    case CSS::PropertyID::BorderLeftWidth:
        return LengthStyleValue::create(Length::make_px(layout_node.computed_values().border_left().width));
    case CSS::PropertyID::BorderTopColor:
        return ColorStyleValue::create(layout_node.computed_values().border_top().color);
    case CSS::PropertyID::BorderRightColor:
        return ColorStyleValue::create(layout_node.computed_values().border_right().color);
    case CSS::PropertyID::BorderBottomColor:
        return ColorStyleValue::create(layout_node.computed_values().border_bottom().color);
    case CSS::PropertyID::BorderLeftColor:
        return ColorStyleValue::create(layout_node.computed_values().border_left().color);
    case CSS::PropertyID::BorderTopStyle:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().border_top().line_style));
    case CSS::PropertyID::BorderRightStyle:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().border_right().line_style));
    case CSS::PropertyID::BorderBottomStyle:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().border_bottom().line_style));
    case CSS::PropertyID::BorderLeftStyle:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().border_left().line_style));
    case CSS::PropertyID::BorderTop: {
        auto border = layout_node.computed_values().border_top();
        auto width = LengthStyleValue::create(Length::make_px(border.width));
        auto style = IdentifierStyleValue::create(to_css_value_id(border.line_style));
        auto color = ColorStyleValue::create(border.color);
        return BorderStyleValue::create(width, style, color);
    }
    case CSS::PropertyID::BorderRight: {
        auto border = layout_node.computed_values().border_right();
        auto width = LengthStyleValue::create(Length::make_px(border.width));
        auto style = IdentifierStyleValue::create(to_css_value_id(border.line_style));
        auto color = ColorStyleValue::create(border.color);
        return BorderStyleValue::create(width, style, color);
    }
    case CSS::PropertyID::BorderBottom: {
        auto border = layout_node.computed_values().border_bottom();
        auto width = LengthStyleValue::create(Length::make_px(border.width));
        auto style = IdentifierStyleValue::create(to_css_value_id(border.line_style));
        auto color = ColorStyleValue::create(border.color);
        return BorderStyleValue::create(width, style, color);
    }
    case CSS::PropertyID::BorderLeft: {
        auto border = layout_node.computed_values().border_left();
        auto width = LengthStyleValue::create(Length::make_px(border.width));
        auto style = IdentifierStyleValue::create(to_css_value_id(border.line_style));
        auto color = ColorStyleValue::create(border.color);
        return BorderStyleValue::create(width, style, color);
    }
    case CSS::PropertyID::OverflowX:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().overflow_x()));
    case CSS::PropertyID::OverflowY:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().overflow_y()));
    case CSS::PropertyID::Color:
        return ColorStyleValue::create(layout_node.computed_values().color());
    case PropertyID::BackgroundColor:
        return ColorStyleValue::create(layout_node.computed_values().background_color());
    case CSS::PropertyID::Background: {
        auto maybe_background_color = property(CSS::PropertyID::BackgroundColor);
        auto maybe_background_image = property(CSS::PropertyID::BackgroundImage);
        auto maybe_background_position = property(CSS::PropertyID::BackgroundPosition);
        auto maybe_background_size = property(CSS::PropertyID::BackgroundSize);
        auto maybe_background_repeat = property(CSS::PropertyID::BackgroundRepeat);
        auto maybe_background_attachment = property(CSS::PropertyID::BackgroundAttachment);
        auto maybe_background_origin = property(CSS::PropertyID::BackgroundOrigin);
        auto maybe_background_clip = property(CSS::PropertyID::BackgroundClip);

        return BackgroundStyleValue::create(
            value_or_default(maybe_background_color, InitialStyleValue::the()),
            value_or_default(maybe_background_image, IdentifierStyleValue::create(CSS::ValueID::None)),
            value_or_default(maybe_background_position, PositionStyleValue::create(PositionEdge::Left, Length::make_px(0), PositionEdge::Top, Length::make_px(0))),
            value_or_default(maybe_background_size, IdentifierStyleValue::create(CSS::ValueID::Auto)),
            value_or_default(maybe_background_repeat, BackgroundRepeatStyleValue::create(CSS::Repeat::Repeat, CSS::Repeat::Repeat)),
            value_or_default(maybe_background_attachment, IdentifierStyleValue::create(CSS::ValueID::Scroll)),
            value_or_default(maybe_background_origin, IdentifierStyleValue::create(CSS::ValueID::PaddingBox)),
            value_or_default(maybe_background_clip, IdentifierStyleValue::create(CSS::ValueID::BorderBox)));
    }
    case CSS::PropertyID::ListStyleType:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().list_style_type()));
    case CSS::PropertyID::BoxSizing:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().box_sizing()));
    case CSS::PropertyID::Invalid:
        return IdentifierStyleValue::create(CSS::ValueID::Invalid);
    case CSS::PropertyID::Custom:
        dbgln_if(LIBWEB_CSS_DEBUG, "Computed style for custom properties was requested (?)");
        return {};
    default:
        dbgln_if(LIBWEB_CSS_DEBUG, "FIXME: Computed style for the '{}' property was requested", string_from_property_id(property_id));
        return {};
    }
    }
}

Optional<StyleProperty> ResolvedCSSStyleDeclaration::property(PropertyID property_id) const
{
    const_cast<DOM::Document&>(m_element->document()).ensure_layout();

    if (!m_element->layout_node()) {
        auto style = m_element->document().style_computer().compute_style(const_cast<DOM::Element&>(*m_element));
        if (auto maybe_property = style->property(property_id); maybe_property.has_value()) {
            return StyleProperty {
                .property_id = property_id,
                .value = maybe_property.release_value(),
            };
        }
        return {};
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

bool ResolvedCSSStyleDeclaration::set_property(PropertyID, StringView)
{
    return false;
}

String ResolvedCSSStyleDeclaration::serialized() const
{
    // https://www.w3.org/TR/cssom/#dom-cssstyledeclaration-csstext
    // If the computed flag is set, then return the empty string.

    // NOTE: ResolvedCSSStyleDeclaration is something you would only get from window.getComputedStyle(),
    //       which returns what the spec calls "resolved style". The "computed flag" is always set here.
    return String::empty();
}

}
