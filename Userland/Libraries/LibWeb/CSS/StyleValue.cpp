/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <LibGfx/Palette.h>
#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/InProcessWebView.h>
#include <LibWeb/Loader/LoadRequest.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Page/BrowsingContext.h>

namespace Web::CSS {

StyleValue::StyleValue(Type type)
    : m_type(type)
{
}

StyleValue::~StyleValue()
{
}

String IdentifierStyleValue::to_string() const
{
    return CSS::string_from_value_id(m_id);
}

Color IdentifierStyleValue::to_color(const DOM::Document& document) const
{
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

ImageStyleValue::ImageStyleValue(const URL& url, DOM::Document& document)
    : StyleValue(Type::Image)
    , m_url(url)
    , m_document(document)
{
    auto request = LoadRequest::create_for_url_on_page(url, document.page());
    set_resource(ResourceLoader::the().load_resource(Resource::Type::Image, request));
}

void ImageStyleValue::resource_did_load()
{
    if (!m_document)
        return;
    m_bitmap = resource()->bitmap();
    // FIXME: Do less than a full repaint if possible?
    if (m_document->browsing_context())
        m_document->browsing_context()->set_needs_display({});
}

}
