/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/ByteBuffer.h>
#include <LibGfx/PNGLoader.h>
#include <LibGfx/Palette.h>
#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/InProcessWebView.h>
#include <LibWeb/Loader/LoadRequest.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Page/Frame.h>

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
    switch (id()) {
    case CSS::ValueID::Invalid:
        return "(invalid)";
    case CSS::ValueID::VendorSpecificLink:
        return "-libweb-link";
    case CSS::ValueID::VendorSpecificCenter:
        return "-libweb-center";
    case CSS::ValueID::VendorSpecificPaletteDesktopBackground:
        return "-libweb-palette-desktop-background";
    case CSS::ValueID::VendorSpecificPaletteActiveWindowBorder1:
        return "-libweb-palette-active-window-border1";
    case CSS::ValueID::VendorSpecificPaletteActiveWindowBorder2:
        return "-libweb-palette-active-window-border2";
    case CSS::ValueID::VendorSpecificPaletteActiveWindowTitle:
        return "-libweb-palette-active-window-title";
    case CSS::ValueID::VendorSpecificPaletteInactiveWindowBorder1:
        return "-libweb-palette-inactive-window-border1";
    case CSS::ValueID::VendorSpecificPaletteInactiveWindowBorder2:
        return "-libweb-palette-inactive-window-border2";
    case CSS::ValueID::VendorSpecificPaletteInactiveWindowTitle:
        return "-libweb-palette-inactive-window-title";
    case CSS::ValueID::VendorSpecificPaletteMovingWindowBorder1:
        return "-libweb-palette-moving-window-border1";
    case CSS::ValueID::VendorSpecificPaletteMovingWindowBorder2:
        return "-libweb-palette-moving-window-border2";
    case CSS::ValueID::VendorSpecificPaletteMovingWindowTitle:
        return "-libweb-palette-moving-window-title";
    case CSS::ValueID::VendorSpecificPaletteHighlightWindowBorder1:
        return "-libweb-palette-highlight-window-border1";
    case CSS::ValueID::VendorSpecificPaletteHighlightWindowBorder2:
        return "-libweb-palette-highlight-window-border2";
    case CSS::ValueID::VendorSpecificPaletteHighlightWindowTitle:
        return "-libweb-palette-highlight-window-title";
    case CSS::ValueID::VendorSpecificPaletteMenuStripe:
        return "-libweb-palette-menu-stripe";
    case CSS::ValueID::VendorSpecificPaletteMenuBase:
        return "-libweb-palette-menu-base";
    case CSS::ValueID::VendorSpecificPaletteMenuBaseText:
        return "-libweb-palette-menu-base-text";
    case CSS::ValueID::VendorSpecificPaletteMenuSelection:
        return "-libweb-palette-menu-selection";
    case CSS::ValueID::VendorSpecificPaletteMenuSelectionText:
        return "-libweb-palette-menu-selection-text";
    case CSS::ValueID::VendorSpecificPaletteWindow:
        return "-libweb-palette-window";
    case CSS::ValueID::VendorSpecificPaletteWindowText:
        return "-libweb-palette-window-text";
    case CSS::ValueID::VendorSpecificPaletteButton:
        return "-libweb-palette-button";
    case CSS::ValueID::VendorSpecificPaletteButtonText:
        return "-libweb-palette-button-text";
    case CSS::ValueID::VendorSpecificPaletteBase:
        return "-libweb-palette-base";
    case CSS::ValueID::VendorSpecificPaletteBaseText:
        return "-libweb-palette-base-text";
    case CSS::ValueID::VendorSpecificPaletteThreedHighlight:
        return "-libweb-palette-threed-highlight";
    case CSS::ValueID::VendorSpecificPaletteThreedShadow1:
        return "-libweb-palette-threed-shadow1";
    case CSS::ValueID::VendorSpecificPaletteThreedShadow2:
        return "-libweb-palette-threed-shadow2";
    case CSS::ValueID::VendorSpecificPaletteHoverHighlight:
        return "-libweb-palette-hover-highlight";
    case CSS::ValueID::VendorSpecificPaletteSelection:
        return "-libweb-palette-selection";
    case CSS::ValueID::VendorSpecificPaletteSelectionText:
        return "-libweb-palette-selection-text";
    case CSS::ValueID::VendorSpecificPaletteInactiveSelection:
        return "-libweb-palette-inactive-selection";
    case CSS::ValueID::VendorSpecificPaletteInactiveSelectionText:
        return "-libweb-palette-inactive-selection-text";
    case CSS::ValueID::VendorSpecificPaletteRubberBandFill:
        return "-libweb-palette-rubber-band-fill";
    case CSS::ValueID::VendorSpecificPaletteRubberBandBorder:
        return "-libweb-palette-rubber-band-border";
    case CSS::ValueID::VendorSpecificPaletteLink:
        return "-libweb-palette-link";
    case CSS::ValueID::VendorSpecificPaletteActiveLink:
        return "-libweb-palette-active-link";
    case CSS::ValueID::VendorSpecificPaletteVisitedLink:
        return "-libweb-palette-visited-link";
    case CSS::ValueID::VendorSpecificPaletteRuler:
        return "-libweb-palette-ruler";
    case CSS::ValueID::VendorSpecificPaletteRulerBorder:
        return "-libweb-palette-ruler-border";
    case CSS::ValueID::VendorSpecificPaletteRulerActiveText:
        return "-libweb-palette-ruler-active-text";
    case CSS::ValueID::VendorSpecificPaletteRulerInactiveText:
        return "-libweb-palette-ruler-inactive-text";
    case CSS::ValueID::VendorSpecificPaletteTextCursor:
        return "-libweb-palette-text-cursor";
    case CSS::ValueID::VendorSpecificPaletteFocusOutline:
        return "-libweb-palette-focus-outline";
    case CSS::ValueID::VendorSpecificPaletteSyntaxComment:
        return "-libweb-palette-syntax-comment";
    case CSS::ValueID::VendorSpecificPaletteSyntaxNumber:
        return "-libweb-palette-syntax-number";
    case CSS::ValueID::VendorSpecificPaletteSyntaxString:
        return "-libweb-palette-syntax-string";
    case CSS::ValueID::VendorSpecificPaletteSyntaxType:
        return "-libweb-palette-syntax-type";
    case CSS::ValueID::VendorSpecificPaletteSyntaxPunctuation:
        return "-libweb-palette-syntax-punctuation";
    case CSS::ValueID::VendorSpecificPaletteSyntaxOperator:
        return "-libweb-palette-syntax-operator";
    case CSS::ValueID::VendorSpecificPaletteSyntaxKeyword:
        return "-libweb-palette-syntax-keyword";
    case CSS::ValueID::VendorSpecificPaletteSyntaxControlKeyword:
        return "-libweb-palette-syntax-control-keyword";
    case CSS::ValueID::VendorSpecificPaletteSyntaxIdentifier:
        return "-libweb-palette-syntax-identifier";
    case CSS::ValueID::VendorSpecificPaletteSyntaxPreprocessorStatement:
        return "-libweb-palette-syntax-preprocessor-statement";
    case CSS::ValueID::VendorSpecificPaletteSyntaxPreprocessorValue:
        return "-libweb-palette-syntax-preprocessor-value";
    default:
        ASSERT_NOT_REACHED();
    }
}

Color IdentifierStyleValue::to_color(const DOM::Document& document) const
{
    if (id() == CSS::ValueID::VendorSpecificLink)
        return document.link_color();

    auto palette = document.frame()->page().palette();
    switch (id()) {
    case CSS::ValueID::VendorSpecificPaletteDesktopBackground:
        return palette.color(ColorRole::DesktopBackground);
    case CSS::ValueID::VendorSpecificPaletteActiveWindowBorder1:
        return palette.color(ColorRole::ActiveWindowBorder1);
    case CSS::ValueID::VendorSpecificPaletteActiveWindowBorder2:
        return palette.color(ColorRole::ActiveWindowBorder2);
    case CSS::ValueID::VendorSpecificPaletteActiveWindowTitle:
        return palette.color(ColorRole::ActiveWindowTitle);
    case CSS::ValueID::VendorSpecificPaletteInactiveWindowBorder1:
        return palette.color(ColorRole::InactiveWindowBorder1);
    case CSS::ValueID::VendorSpecificPaletteInactiveWindowBorder2:
        return palette.color(ColorRole::InactiveWindowBorder2);
    case CSS::ValueID::VendorSpecificPaletteInactiveWindowTitle:
        return palette.color(ColorRole::InactiveWindowTitle);
    case CSS::ValueID::VendorSpecificPaletteMovingWindowBorder1:
        return palette.color(ColorRole::MovingWindowBorder1);
    case CSS::ValueID::VendorSpecificPaletteMovingWindowBorder2:
        return palette.color(ColorRole::MovingWindowBorder2);
    case CSS::ValueID::VendorSpecificPaletteMovingWindowTitle:
        return palette.color(ColorRole::MovingWindowTitle);
    case CSS::ValueID::VendorSpecificPaletteHighlightWindowBorder1:
        return palette.color(ColorRole::HighlightWindowBorder1);
    case CSS::ValueID::VendorSpecificPaletteHighlightWindowBorder2:
        return palette.color(ColorRole::HighlightWindowBorder2);
    case CSS::ValueID::VendorSpecificPaletteHighlightWindowTitle:
        return palette.color(ColorRole::HighlightWindowTitle);
    case CSS::ValueID::VendorSpecificPaletteMenuStripe:
        return palette.color(ColorRole::MenuStripe);
    case CSS::ValueID::VendorSpecificPaletteMenuBase:
        return palette.color(ColorRole::MenuBase);
    case CSS::ValueID::VendorSpecificPaletteMenuBaseText:
        return palette.color(ColorRole::MenuBaseText);
    case CSS::ValueID::VendorSpecificPaletteMenuSelection:
        return palette.color(ColorRole::MenuSelection);
    case CSS::ValueID::VendorSpecificPaletteMenuSelectionText:
        return palette.color(ColorRole::MenuSelectionText);
    case CSS::ValueID::VendorSpecificPaletteWindow:
        return palette.color(ColorRole::Window);
    case CSS::ValueID::VendorSpecificPaletteWindowText:
        return palette.color(ColorRole::WindowText);
    case CSS::ValueID::VendorSpecificPaletteButton:
        return palette.color(ColorRole::Button);
    case CSS::ValueID::VendorSpecificPaletteButtonText:
        return palette.color(ColorRole::ButtonText);
    case CSS::ValueID::VendorSpecificPaletteBase:
        return palette.color(ColorRole::Base);
    case CSS::ValueID::VendorSpecificPaletteBaseText:
        return palette.color(ColorRole::BaseText);
    case CSS::ValueID::VendorSpecificPaletteThreedHighlight:
        return palette.color(ColorRole::ThreedHighlight);
    case CSS::ValueID::VendorSpecificPaletteThreedShadow1:
        return palette.color(ColorRole::ThreedShadow1);
    case CSS::ValueID::VendorSpecificPaletteThreedShadow2:
        return palette.color(ColorRole::ThreedShadow2);
    case CSS::ValueID::VendorSpecificPaletteHoverHighlight:
        return palette.color(ColorRole::HoverHighlight);
    case CSS::ValueID::VendorSpecificPaletteSelection:
        return palette.color(ColorRole::Selection);
    case CSS::ValueID::VendorSpecificPaletteSelectionText:
        return palette.color(ColorRole::SelectionText);
    case CSS::ValueID::VendorSpecificPaletteInactiveSelection:
        return palette.color(ColorRole::InactiveSelection);
    case CSS::ValueID::VendorSpecificPaletteInactiveSelectionText:
        return palette.color(ColorRole::InactiveSelectionText);
    case CSS::ValueID::VendorSpecificPaletteRubberBandFill:
        return palette.color(ColorRole::RubberBandFill);
    case CSS::ValueID::VendorSpecificPaletteRubberBandBorder:
        return palette.color(ColorRole::RubberBandBorder);
    case CSS::ValueID::VendorSpecificPaletteLink:
        return palette.color(ColorRole::Link);
    case CSS::ValueID::VendorSpecificPaletteActiveLink:
        return palette.color(ColorRole::ActiveLink);
    case CSS::ValueID::VendorSpecificPaletteVisitedLink:
        return palette.color(ColorRole::VisitedLink);
    case CSS::ValueID::VendorSpecificPaletteRuler:
        return palette.color(ColorRole::Ruler);
    case CSS::ValueID::VendorSpecificPaletteRulerBorder:
        return palette.color(ColorRole::RulerBorder);
    case CSS::ValueID::VendorSpecificPaletteRulerActiveText:
        return palette.color(ColorRole::RulerActiveText);
    case CSS::ValueID::VendorSpecificPaletteRulerInactiveText:
        return palette.color(ColorRole::RulerInactiveText);
    case CSS::ValueID::VendorSpecificPaletteTextCursor:
        return palette.color(ColorRole::TextCursor);
    case CSS::ValueID::VendorSpecificPaletteFocusOutline:
        return palette.color(ColorRole::FocusOutline);
    case CSS::ValueID::VendorSpecificPaletteSyntaxComment:
        return palette.color(ColorRole::SyntaxComment);
    case CSS::ValueID::VendorSpecificPaletteSyntaxNumber:
        return palette.color(ColorRole::SyntaxNumber);
    case CSS::ValueID::VendorSpecificPaletteSyntaxString:
        return palette.color(ColorRole::SyntaxString);
    case CSS::ValueID::VendorSpecificPaletteSyntaxType:
        return palette.color(ColorRole::SyntaxType);
    case CSS::ValueID::VendorSpecificPaletteSyntaxPunctuation:
        return palette.color(ColorRole::SyntaxPunctuation);
    case CSS::ValueID::VendorSpecificPaletteSyntaxOperator:
        return palette.color(ColorRole::SyntaxOperator);
    case CSS::ValueID::VendorSpecificPaletteSyntaxKeyword:
        return palette.color(ColorRole::SyntaxKeyword);
    case CSS::ValueID::VendorSpecificPaletteSyntaxControlKeyword:
        return palette.color(ColorRole::SyntaxControlKeyword);
    case CSS::ValueID::VendorSpecificPaletteSyntaxIdentifier:
        return palette.color(ColorRole::SyntaxIdentifier);
    case CSS::ValueID::VendorSpecificPaletteSyntaxPreprocessorStatement:
        return palette.color(ColorRole::SyntaxPreprocessorStatement);
    case CSS::ValueID::VendorSpecificPaletteSyntaxPreprocessorValue:
        return palette.color(ColorRole::SyntaxPreprocessorValue);
    default:
        return {};
    }
}

ImageStyleValue::ImageStyleValue(const URL& url, DOM::Document& document)
    : StyleValue(Type::Image)
    , m_url(url)
    , m_document(document.make_weak_ptr())
{
    LoadRequest request;
    request.set_url(url);
    set_resource(ResourceLoader::the().load_resource(Resource::Type::Image, request));
}

void ImageStyleValue::resource_did_load()
{
    if (!m_document)
        return;
    m_bitmap = resource()->bitmap();
    // FIXME: Do less than a full repaint if possible?
    if (m_document->frame())
        m_document->frame()->set_needs_display({});
}

}
