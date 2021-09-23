/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullRefPtr.h>
#include <LibWeb/CSS/ComputedCSSStyleDeclaration.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>

namespace Web::CSS {

ComputedCSSStyleDeclaration::ComputedCSSStyleDeclaration(DOM::Element& element)
    : m_element(element)
{
}

ComputedCSSStyleDeclaration::~ComputedCSSStyleDeclaration()
{
}

size_t ComputedCSSStyleDeclaration::length() const
{
    return 0;
}

String ComputedCSSStyleDeclaration::item(size_t index) const
{
    (void)index;
    return {};
}

static CSS::ValueID to_css_value_id(CSS::Display value)
{
    switch (value) {
    case CSS::Display::None:
        return CSS::ValueID::None;
    case CSS::Display::Block:
        return CSS::ValueID::Block;
    case CSS::Display::Inline:
        return CSS::ValueID::Inline;
    case CSS::Display::InlineBlock:
        return CSS::ValueID::InlineBlock;
    case CSS::Display::ListItem:
        return CSS::ValueID::ListItem;
    case CSS::Display::Table:
        return CSS::ValueID::Table;
    case CSS::Display::TableRow:
        return CSS::ValueID::TableRow;
    case CSS::Display::TableCell:
        return CSS::ValueID::TableCell;
    case CSS::Display::TableHeaderGroup:
        return CSS::ValueID::TableHeaderGroup;
    case CSS::Display::TableRowGroup:
        return CSS::ValueID::TableRowGroup;
    case CSS::Display::TableFooterGroup:
        return CSS::ValueID::TableFooterGroup;
    case CSS::Display::TableColumn:
        return CSS::ValueID::TableColumn;
    case CSS::Display::TableColumnGroup:
        return CSS::ValueID::TableColumnGroup;
    case CSS::Display::TableCaption:
        return CSS::ValueID::TableCaption;
    case CSS::Display::Flex:
        return CSS::ValueID::Flex;
    }
    VERIFY_NOT_REACHED();
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

static CSS::ValueID to_css_value_id(CSS::Repeat value)
{
    switch (value) {
    case Repeat::NoRepeat:
        return CSS::ValueID::NoRepeat;
    case Repeat::Repeat:
        return CSS::ValueID::Repeat;
    case Repeat::Round:
        return CSS::ValueID::Round;
    case Repeat::Space:
        return CSS::ValueID::Space;
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

static NonnullRefPtr<StyleValue> value_or_default(Optional<StyleProperty> property, NonnullRefPtr<StyleValue> default_style)
{
    if (property.has_value())
        return property.value().value;
    return default_style;
}

RefPtr<StyleValue> ComputedCSSStyleDeclaration::style_value_for_property(Layout::NodeWithStyle const& layout_node, PropertyID property_id) const
{
    switch (property_id) {
    case CSS::PropertyID::Float:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().float_()));
    case CSS::PropertyID::Clear:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().clear()));
    case CSS::PropertyID::Cursor:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().cursor()));
    case CSS::PropertyID::Display:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().display()));
    case CSS::PropertyID::ZIndex: {
        auto maybe_z_index = layout_node.computed_values().z_index();
        if (!maybe_z_index.has_value())
            return {};
        return NumericStyleValue::create(maybe_z_index.release_value());
    }
    case CSS::PropertyID::TextAlign:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().text_align()));
    case CSS::PropertyID::TextDecorationLine:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().text_decoration_line()));
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
        case FlexBasis::Length:
            return LengthStyleValue::create(layout_node.computed_values().flex_basis().length);
        case FlexBasis::Auto:
            return IdentifierStyleValue::create(CSS::ValueID::Auto);
        default:
            VERIFY_NOT_REACHED();
        }
        break;
    case CSS::PropertyID::FlexGrow: {
        auto maybe_grow_factor = layout_node.computed_values().flex_grow_factor();
        if (!maybe_grow_factor.has_value())
            return {};
        return NumericStyleValue::create(maybe_grow_factor.release_value());
    }
    case CSS::PropertyID::FlexShrink: {
        auto maybe_shrink_factor = layout_node.computed_values().flex_shrink_factor();
        if (!maybe_shrink_factor.has_value())
            return {};
        return NumericStyleValue::create(maybe_shrink_factor.release_value());
    }
    case CSS::PropertyID::Opacity: {
        auto maybe_opacity = layout_node.computed_values().opacity();
        if (!maybe_opacity.has_value())
            return {};
        return NumericStyleValue::create(maybe_opacity.release_value());
    }
    case CSS::PropertyID::JustifyContent:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().justify_content()));
    case CSS::PropertyID::BoxShadow: {
        auto maybe_box_shadow = layout_node.computed_values().box_shadow();
        if (!maybe_box_shadow.has_value())
            return {};
        auto box_shadow_data = maybe_box_shadow.release_value();
        return BoxShadowStyleValue::create(box_shadow_data.offset_x, box_shadow_data.offset_y, box_shadow_data.blur_radius, box_shadow_data.color);
    }
    case CSS::PropertyID::Width:
        return LengthStyleValue::create(layout_node.computed_values().width());
    case CSS::PropertyID::MinWidth:
        return LengthStyleValue::create(layout_node.computed_values().min_width());
    case CSS::PropertyID::MaxWidth:
        return LengthStyleValue::create(layout_node.computed_values().max_width());
    case CSS::PropertyID::Height:
        return LengthStyleValue::create(layout_node.computed_values().height());
    case CSS::PropertyID::MinHeight:
        return LengthStyleValue::create(layout_node.computed_values().min_height());
    case CSS::PropertyID::MaxHeight:
        return LengthStyleValue::create(layout_node.computed_values().max_height());
    case CSS::PropertyID::MarginTop:
        return LengthStyleValue::create(layout_node.computed_values().margin().top);
    case CSS::PropertyID::MarginRight:
        return LengthStyleValue::create(layout_node.computed_values().margin().right);
    case CSS::PropertyID::MarginBottom:
        return LengthStyleValue::create(layout_node.computed_values().margin().bottom);
    case CSS::PropertyID::MarginLeft:
        return LengthStyleValue::create(layout_node.computed_values().margin().left);
    case CSS::PropertyID::PaddingTop:
        return LengthStyleValue::create(layout_node.computed_values().padding().top);
    case CSS::PropertyID::PaddingRight:
        return LengthStyleValue::create(layout_node.computed_values().padding().right);
    case CSS::PropertyID::PaddingBottom:
        return LengthStyleValue::create(layout_node.computed_values().padding().bottom);
    case CSS::PropertyID::PaddingLeft:
        return LengthStyleValue::create(layout_node.computed_values().padding().left);
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
    case CSS::PropertyID::OverflowX:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().overflow_x()));
    case CSS::PropertyID::OverflowY:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().overflow_y()));
    case CSS::PropertyID::Color:
        return ColorStyleValue::create(layout_node.computed_values().color());
    case PropertyID::BackgroundColor:
        return ColorStyleValue::create(layout_node.computed_values().background_color());
    case CSS::PropertyID::BackgroundRepeatX:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().background_repeat_x()));
    case CSS::PropertyID::BackgroundRepeatY:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().background_repeat_y()));
    case CSS::PropertyID::BackgroundRepeat: {
        auto maybe_background_repeat_x = property(CSS::PropertyID::BackgroundRepeatX);
        auto maybe_background_repeat_y = property(CSS::PropertyID::BackgroundRepeatY);
        return BackgroundRepeatStyleValue::create(value_or_default(maybe_background_repeat_x, IdentifierStyleValue::create(CSS::ValueID::RepeatX)), value_or_default(maybe_background_repeat_y, IdentifierStyleValue::create(CSS::ValueID::RepeatY)));
    }
    case CSS::PropertyID::Background: {
        auto maybe_background_color = property(CSS::PropertyID::BackgroundColor);
        auto maybe_background_image = property(CSS::PropertyID::BackgroundImage);
        auto maybe_background_repeat_x = property(CSS::PropertyID::BackgroundRepeatX);
        auto maybe_background_repeat_y = property(CSS::PropertyID::BackgroundRepeatY);

        return BackgroundStyleValue::create(value_or_default(maybe_background_color, InitialStyleValue::the()), value_or_default(maybe_background_image, IdentifierStyleValue::create(CSS::ValueID::None)), value_or_default(maybe_background_repeat_x, IdentifierStyleValue::create(CSS::ValueID::RepeatX)), value_or_default(maybe_background_repeat_y, IdentifierStyleValue::create(CSS::ValueID::RepeatX)));
    }
    case CSS::PropertyID::ListStyleType:
        return IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().list_style_type()));
    case CSS::PropertyID::Invalid:
        return IdentifierStyleValue::create(CSS::ValueID::Invalid);
    case CSS::PropertyID::Custom:
        dbgln("Computed style for custom properties was requested (?)");
        return {};
    default:
        dbgln("FIXME: Computed style for the '{}' property was requested", string_from_property_id(property_id));
        return {};
    }
    }
}

Optional<StyleProperty> ComputedCSSStyleDeclaration::property(PropertyID property_id) const
{
    const_cast<DOM::Document&>(m_element->document()).ensure_layout();

    if (!m_element->layout_node()) {
        auto style = m_element->document().style_resolver().resolve_style(const_cast<DOM::Element&>(*m_element));
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

bool ComputedCSSStyleDeclaration::set_property(PropertyID, StringView)
{
    return false;
}
}
