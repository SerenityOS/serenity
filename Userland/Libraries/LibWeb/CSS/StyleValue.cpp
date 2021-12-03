/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <LibGfx/Palette.h>
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

StyleValue::~StyleValue()
{
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

BoxShadowStyleValue const& StyleValue::as_box_shadow() const
{
    VERIFY(is_box_shadow());
    return static_cast<BoxShadowStyleValue const&>(*this);
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

PositionStyleValue const& StyleValue::as_position() const
{
    VERIFY(is_position());
    return static_cast<PositionStyleValue const&>(*this);
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

String IdentifierStyleValue::to_string() const
{
    return CSS::string_from_value_id(m_id);
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

    VERIFY(document.page());
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

// https://www.w3.org/TR/css-color-4/#serializing-sRGB-values
String ColorStyleValue::to_string() const
{
    if (m_color.alpha() == 1)
        return String::formatted("rgb({}, {}, {})", m_color.red(), m_color.green(), m_color.blue());
    return String::formatted("rgba({}, {}, {}, {})", m_color.red(), m_color.green(), m_color.blue(), (float)(m_color.alpha()) / 255.0f);
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

String UnresolvedStyleValue::to_string() const
{
    StringBuilder builder;
    for (auto& value : m_values)
        builder.append(value.to_string());
    return builder.to_string();
}

}
