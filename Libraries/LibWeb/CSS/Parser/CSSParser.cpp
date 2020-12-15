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

#include <AK/HashMap.h>
#include <LibWeb/CSS/Parser/CSSParser.h>
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/CSS/StyleSheet.h>
#include <LibWeb/DOM/Document.h>
#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define PARSE_ASSERT(x)                                                   \
    if (!(x)) {                                                           \
        dbg() << "CSS PARSER ASSERTION FAILED: " << #x;                   \
        dbg() << "At character# " << index << " in CSS: _" << css << "_"; \
        ASSERT_NOT_REACHED();                                             \
    }

#define PARSE_ERROR()               \
    do {                            \
        dbg() << "CSS parse error"; \
    } while (0)

namespace Web {

namespace CSS {

ParsingContext::ParsingContext()
{
}

ParsingContext::ParsingContext(const DOM::Document& document)
    : m_document(&document)
{
}

ParsingContext::ParsingContext(const DOM::ParentNode& parent_node)
    : m_document(&parent_node.document())
{
}

bool ParsingContext::in_quirks_mode() const
{
    return m_document ? m_document->in_quirks_mode() : false;
}

}

static Optional<CSS::ValueID> value_id_for_palette_string(const StringView& string)
{
    if (string.equals_ignoring_case("desktop-background"))
        return CSS::ValueID::VendorSpecificPaletteDesktopBackground;
    if (string.equals_ignoring_case("active-window-border1"))
        return CSS::ValueID::VendorSpecificPaletteActiveWindowBorder1;
    if (string.equals_ignoring_case("active-window-border2"))
        return CSS::ValueID::VendorSpecificPaletteActiveWindowBorder2;
    if (string.equals_ignoring_case("active-window-title"))
        return CSS::ValueID::VendorSpecificPaletteActiveWindowTitle;
    if (string.equals_ignoring_case("inactive-window-border1"))
        return CSS::ValueID::VendorSpecificPaletteInactiveWindowBorder1;
    if (string.equals_ignoring_case("inactive-window-border2"))
        return CSS::ValueID::VendorSpecificPaletteInactiveWindowBorder2;
    if (string.equals_ignoring_case("inactive-window-title"))
        return CSS::ValueID::VendorSpecificPaletteInactiveWindowTitle;
    if (string.equals_ignoring_case("moving-window-border1"))
        return CSS::ValueID::VendorSpecificPaletteMovingWindowBorder1;
    if (string.equals_ignoring_case("moving-window-border2"))
        return CSS::ValueID::VendorSpecificPaletteMovingWindowBorder2;
    if (string.equals_ignoring_case("moving-window-title"))
        return CSS::ValueID::VendorSpecificPaletteMovingWindowTitle;
    if (string.equals_ignoring_case("highlight-window-border1"))
        return CSS::ValueID::VendorSpecificPaletteHighlightWindowBorder1;
    if (string.equals_ignoring_case("highlight-window-border2"))
        return CSS::ValueID::VendorSpecificPaletteHighlightWindowBorder2;
    if (string.equals_ignoring_case("highlight-window-title"))
        return CSS::ValueID::VendorSpecificPaletteHighlightWindowTitle;
    if (string.equals_ignoring_case("menu-stripe"))
        return CSS::ValueID::VendorSpecificPaletteMenuStripe;
    if (string.equals_ignoring_case("menu-base"))
        return CSS::ValueID::VendorSpecificPaletteMenuBase;
    if (string.equals_ignoring_case("menu-base-text"))
        return CSS::ValueID::VendorSpecificPaletteMenuBaseText;
    if (string.equals_ignoring_case("menu-selection"))
        return CSS::ValueID::VendorSpecificPaletteMenuSelection;
    if (string.equals_ignoring_case("menu-selection-text"))
        return CSS::ValueID::VendorSpecificPaletteMenuSelectionText;
    if (string.equals_ignoring_case("window"))
        return CSS::ValueID::VendorSpecificPaletteWindow;
    if (string.equals_ignoring_case("window-text"))
        return CSS::ValueID::VendorSpecificPaletteWindowText;
    if (string.equals_ignoring_case("button"))
        return CSS::ValueID::VendorSpecificPaletteButton;
    if (string.equals_ignoring_case("button-text"))
        return CSS::ValueID::VendorSpecificPaletteButtonText;
    if (string.equals_ignoring_case("base"))
        return CSS::ValueID::VendorSpecificPaletteBase;
    if (string.equals_ignoring_case("base-text"))
        return CSS::ValueID::VendorSpecificPaletteBaseText;
    if (string.equals_ignoring_case("threed-highlight"))
        return CSS::ValueID::VendorSpecificPaletteThreedHighlight;
    if (string.equals_ignoring_case("threed-shadow1"))
        return CSS::ValueID::VendorSpecificPaletteThreedShadow1;
    if (string.equals_ignoring_case("threed-shadow2"))
        return CSS::ValueID::VendorSpecificPaletteThreedShadow2;
    if (string.equals_ignoring_case("hover-highlight"))
        return CSS::ValueID::VendorSpecificPaletteHoverHighlight;
    if (string.equals_ignoring_case("selection"))
        return CSS::ValueID::VendorSpecificPaletteSelection;
    if (string.equals_ignoring_case("selection-text"))
        return CSS::ValueID::VendorSpecificPaletteSelectionText;
    if (string.equals_ignoring_case("inactive-selection"))
        return CSS::ValueID::VendorSpecificPaletteInactiveSelection;
    if (string.equals_ignoring_case("inactive-selection-text"))
        return CSS::ValueID::VendorSpecificPaletteInactiveSelectionText;
    if (string.equals_ignoring_case("rubber-band-fill"))
        return CSS::ValueID::VendorSpecificPaletteRubberBandFill;
    if (string.equals_ignoring_case("rubber-band-border"))
        return CSS::ValueID::VendorSpecificPaletteRubberBandBorder;
    if (string.equals_ignoring_case("link"))
        return CSS::ValueID::VendorSpecificPaletteLink;
    if (string.equals_ignoring_case("active-link"))
        return CSS::ValueID::VendorSpecificPaletteActiveLink;
    if (string.equals_ignoring_case("visited-link"))
        return CSS::ValueID::VendorSpecificPaletteVisitedLink;
    if (string.equals_ignoring_case("ruler"))
        return CSS::ValueID::VendorSpecificPaletteRuler;
    if (string.equals_ignoring_case("ruler-border"))
        return CSS::ValueID::VendorSpecificPaletteRulerBorder;
    if (string.equals_ignoring_case("ruler-active-text"))
        return CSS::ValueID::VendorSpecificPaletteRulerActiveText;
    if (string.equals_ignoring_case("ruler-inactive-text"))
        return CSS::ValueID::VendorSpecificPaletteRulerInactiveText;
    if (string.equals_ignoring_case("text-cursor"))
        return CSS::ValueID::VendorSpecificPaletteTextCursor;
    if (string.equals_ignoring_case("focus-outline"))
        return CSS::ValueID::VendorSpecificPaletteFocusOutline;
    if (string.equals_ignoring_case("syntax-comment"))
        return CSS::ValueID::VendorSpecificPaletteSyntaxComment;
    if (string.equals_ignoring_case("syntax-number"))
        return CSS::ValueID::VendorSpecificPaletteSyntaxNumber;
    if (string.equals_ignoring_case("syntax-string"))
        return CSS::ValueID::VendorSpecificPaletteSyntaxString;
    if (string.equals_ignoring_case("syntax-type"))
        return CSS::ValueID::VendorSpecificPaletteSyntaxType;
    if (string.equals_ignoring_case("syntax-punctuation"))
        return CSS::ValueID::VendorSpecificPaletteSyntaxPunctuation;
    if (string.equals_ignoring_case("syntax-operator"))
        return CSS::ValueID::VendorSpecificPaletteSyntaxOperator;
    if (string.equals_ignoring_case("syntax-keyword"))
        return CSS::ValueID::VendorSpecificPaletteSyntaxKeyword;
    if (string.equals_ignoring_case("syntax-control-keyword"))
        return CSS::ValueID::VendorSpecificPaletteSyntaxControlKeyword;
    if (string.equals_ignoring_case("syntax-identifier"))
        return CSS::ValueID::VendorSpecificPaletteSyntaxIdentifier;
    if (string.equals_ignoring_case("syntax-preprocessor-statement"))
        return CSS::ValueID::VendorSpecificPaletteSyntaxPreprocessorStatement;
    if (string.equals_ignoring_case("syntax-preprocessor-value"))
        return CSS::ValueID::VendorSpecificPaletteSyntaxPreprocessorValue;
    return {};
}

static Optional<Color> parse_css_color(const CSS::ParsingContext&, const StringView& view)
{
    if (view.equals_ignoring_case("transparent"))
        return Color::from_rgba(0x00000000);

    auto color = Color::from_string(view.to_string().to_lowercase());
    if (color.has_value())
        return color;

    return {};
}

static Optional<float> try_parse_float(const StringView& string)
{
    const char* str = string.characters_without_null_termination();
    size_t len = string.length();
    size_t weight = 1;
    int exp_val = 0;
    float value = 0.0f;
    float fraction = 0.0f;
    bool has_sign = false;
    bool is_negative = false;
    bool is_fractional = false;
    bool is_scientific = false;

    if (str[0] == '-') {
        is_negative = true;
        has_sign = true;
    }
    if (str[0] == '+') {
        has_sign = true;
    }

    for (size_t i = has_sign; i < len; i++) {

        // Looks like we're about to start working on the fractional part
        if (str[i] == '.') {
            is_fractional = true;
            continue;
        }

        if (str[i] == 'e' || str[i] == 'E') {
            if (str[i + 1] == '-' || str[i + 1] == '+')
                exp_val = atoi(str + i + 2);
            else
                exp_val = atoi(str + i + 1);

            is_scientific = true;
            continue;
        }

        if (str[i] < '0' || str[i] > '9' || exp_val != 0) {
            return {};
            continue;
        }

        if (is_fractional) {
            fraction *= 10;
            fraction += str[i] - '0';
            weight *= 10;
        } else {
            value = value * 10;
            value += str[i] - '0';
        }
    }

    fraction /= weight;
    value += fraction;

    if (is_scientific) {
        bool divide = exp_val < 0;
        if (divide)
            exp_val *= -1;

        for (int i = 0; i < exp_val; i++) {
            if (divide)
                value /= 10;
            else
                value *= 10;
        }
    }

    return is_negative ? -value : value;
}

static CSS::Length parse_length(const CSS::ParsingContext& context, const StringView& view, bool& is_bad_length)
{
    CSS::Length::Type type = CSS::Length::Type::Undefined;
    Optional<float> value;

    if (view.ends_with('%')) {
        type = CSS::Length::Type::Percentage;
        value = try_parse_float(view.substring_view(0, view.length() - 1));
    } else if (view.ends_with("px", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Px;
        value = try_parse_float(view.substring_view(0, view.length() - 2));
    } else if (view.ends_with("pt", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Pt;
        value = try_parse_float(view.substring_view(0, view.length() - 2));
    } else if (view.ends_with("pc", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Pc;
        value = try_parse_float(view.substring_view(0, view.length() - 2));
    } else if (view.ends_with("mm", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Mm;
        value = try_parse_float(view.substring_view(0, view.length() - 2));
    } else if (view.ends_with("rem", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Rem;
        value = try_parse_float(view.substring_view(0, view.length() - 3));
    } else if (view.ends_with("em", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Em;
        value = try_parse_float(view.substring_view(0, view.length() - 2));
    } else if (view.ends_with("ex", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Ex;
        value = try_parse_float(view.substring_view(0, view.length() - 2));
    } else if (view.ends_with("vw", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Vw;
        value = try_parse_float(view.substring_view(0, view.length() - 2));
    } else if (view.ends_with("vh", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Vh;
        value = try_parse_float(view.substring_view(0, view.length() - 2));
    } else if (view.ends_with("vmax", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Vmax;
        value = try_parse_float(view.substring_view(0, view.length() - 4));
    } else if (view.ends_with("vmin", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Vmin;
        value = try_parse_float(view.substring_view(0, view.length() - 4));
    } else if (view.ends_with("cm", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Cm;
        value = try_parse_float(view.substring_view(0, view.length() - 2));
    } else if (view.ends_with("in", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::In;
        value = try_parse_float(view.substring_view(0, view.length() - 2));
    } else if (view.ends_with("Q", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Q;
        value = try_parse_float(view.substring_view(0, view.length() - 1));
    } else if (view == "0") {
        type = CSS::Length::Type::Px;
        value = 0;
    } else if (context.in_quirks_mode()) {
        type = CSS::Length::Type::Px;
        value = try_parse_float(view);
    } else {
        value = try_parse_float(view);
        if (value.has_value())
            is_bad_length = true;
    }

    if (!value.has_value())
        return {};

    return CSS::Length(value.value(), type);
}

static bool takes_integer_value(CSS::PropertyID property_id)
{
    return property_id == CSS::PropertyID::ZIndex || property_id == CSS::PropertyID::FontWeight;
}

static Optional<CSS::ValueID> value_id_from_string(const String& string)
{
    // FIXME: Handle all identifiers
    // FIXME: Generate this code
    if (string.equals_ignoring_case("bold"))
        return CSS::ValueID::Bold;
    if (string.equals_ignoring_case("bolder"))
        return CSS::ValueID::Bolder;
    if (string.equals_ignoring_case("center"))
        return CSS::ValueID::Center;
    if (string.equals_ignoring_case("justify"))
        return CSS::ValueID::Justify;
    if (string.equals_ignoring_case("large"))
        return CSS::ValueID::Large;
    if (string.equals_ignoring_case("larger"))
        return CSS::ValueID::Larger;
    if (string.equals_ignoring_case("left"))
        return CSS::ValueID::Left;
    if (string.equals_ignoring_case("lighter"))
        return CSS::ValueID::Lighter;
    if (string.equals_ignoring_case("medium"))
        return CSS::ValueID::Medium;
    if (string.equals_ignoring_case("normal"))
        return CSS::ValueID::Normal;
    if (string.equals_ignoring_case("small"))
        return CSS::ValueID::Small;
    if (string.equals_ignoring_case("right"))
        return CSS::ValueID::Right;
    if (string.equals_ignoring_case("smaller"))
        return CSS::ValueID::Smaller;
    if (string.equals_ignoring_case("x-large"))
        return CSS::ValueID::XLarge;
    if (string.equals_ignoring_case("x-small"))
        return CSS::ValueID::XSmall;
    if (string.equals_ignoring_case("xx-large"))
        return CSS::ValueID::XxLarge;
    if (string.equals_ignoring_case("xx-small"))
        return CSS::ValueID::XxSmall;
    if (string.equals_ignoring_case("xxx-large"))
        return CSS::ValueID::XxxLarge;
    if (string.equals_ignoring_case("-libweb-center"))
        return CSS::ValueID::VendorSpecificCenter;
    if (string.equals_ignoring_case("-libweb-link"))
        return CSS::ValueID::VendorSpecificLink;
    if (string.equals_ignoring_case("static"))
        return CSS::ValueID::Static;
    if (string.equals_ignoring_case("relative"))
        return CSS::ValueID::Relative;
    if (string.equals_ignoring_case("absolute"))
        return CSS::ValueID::Absolute;
    if (string.equals_ignoring_case("fixed"))
        return CSS::ValueID::Fixed;
    if (string.equals_ignoring_case("sticky"))
        return CSS::ValueID::Sticky;
    if (string.equals_ignoring_case("none"))
        return CSS::ValueID::None;
    if (string.equals_ignoring_case("both"))
        return CSS::ValueID::Both;
    if (string.equals_ignoring_case("hidden"))
        return CSS::ValueID::Hidden;
    if (string.equals_ignoring_case("dotted"))
        return CSS::ValueID::Dotted;
    if (string.equals_ignoring_case("dashed"))
        return CSS::ValueID::Dashed;
    if (string.equals_ignoring_case("solid"))
        return CSS::ValueID::Solid;
    if (string.equals_ignoring_case("double"))
        return CSS::ValueID::Double;
    if (string.equals_ignoring_case("groove"))
        return CSS::ValueID::Groove;
    if (string.equals_ignoring_case("ridge"))
        return CSS::ValueID::Ridge;
    if (string.equals_ignoring_case("inset"))
        return CSS::ValueID::Inset;
    if (string.equals_ignoring_case("outset"))
        return CSS::ValueID::Outset;
    if (string.equals_ignoring_case("nowrap"))
        return CSS::ValueID::Nowrap;
    if (string.equals_ignoring_case("pre"))
        return CSS::ValueID::Pre;
    if (string.equals_ignoring_case("pre-line"))
        return CSS::ValueID::PreLine;
    if (string.equals_ignoring_case("pre-wrap"))
        return CSS::ValueID::PreWrap;
    if (string.equals_ignoring_case("block"))
        return CSS::ValueID::Block;
    if (string.equals_ignoring_case("inline"))
        return CSS::ValueID::Inline;
    if (string.equals_ignoring_case("inline-block"))
        return CSS::ValueID::InlineBlock;
    if (string.equals_ignoring_case("list-item"))
        return CSS::ValueID::ListItem;
    if (string.equals_ignoring_case("table"))
        return CSS::ValueID::Table;
    if (string.equals_ignoring_case("table-row"))
        return CSS::ValueID::TableRow;
    if (string.equals_ignoring_case("table-cell"))
        return CSS::ValueID::TableCell;
    if (string.equals_ignoring_case("table-row-group"))
        return CSS::ValueID::TableRowGroup;
    if (string.equals_ignoring_case("table-header-group"))
        return CSS::ValueID::TableHeaderGroup;
    if (string.equals_ignoring_case("table-footer-group"))
        return CSS::ValueID::TableFooterGroup;
    if (string.equals_ignoring_case("underline"))
        return CSS::ValueID::Underline;
    if (string.equals_ignoring_case("overline"))
        return CSS::ValueID::Overline;
    if (string.equals_ignoring_case("line-through"))
        return CSS::ValueID::LineThrough;
    if (string.equals_ignoring_case("blink"))
        return CSS::ValueID::Blink;
    if (string.starts_with("-libweb-palette-", CaseSensitivity::CaseInsensitive))
        return value_id_for_palette_string(string.substring_view(16, string.length() - 16));
    return {};
}

RefPtr<CSS::StyleValue> parse_css_value(const CSS::ParsingContext& context, const StringView& string, CSS::PropertyID property_id)
{
    bool is_bad_length = false;

    if (takes_integer_value(property_id)) {
        auto integer = string.to_int();
        if (integer.has_value())
            return CSS::LengthStyleValue::create(CSS::Length::make_px(integer.value()));
    }

    auto length = parse_length(context, string, is_bad_length);
    if (is_bad_length)
        return nullptr;
    if (!length.is_undefined())
        return CSS::LengthStyleValue::create(length);

    if (string.equals_ignoring_case("inherit"))
        return CSS::InheritStyleValue::create();
    if (string.equals_ignoring_case("initial"))
        return CSS::InitialStyleValue::create();
    if (string.equals_ignoring_case("auto"))
        return CSS::LengthStyleValue::create(CSS::Length::make_auto());

    auto value_id = value_id_from_string(string);
    if (value_id.has_value())
        return CSS::IdentifierStyleValue::create(value_id.value());

    auto color = parse_css_color(context, string);
    if (color.has_value())
        return CSS::ColorStyleValue::create(color.value());

    return CSS::StringStyleValue::create(string);
}

RefPtr<CSS::LengthStyleValue> parse_line_width(const CSS::ParsingContext& context, const StringView& part)
{
    auto value = parse_css_value(context, part);
    if (value && value->is_length())
        return static_ptr_cast<CSS::LengthStyleValue>(value);
    return nullptr;
}

RefPtr<CSS::ColorStyleValue> parse_color(const CSS::ParsingContext& context, const StringView& part)
{
    auto value = parse_css_value(context, part);
    if (value && value->is_color())
        return static_ptr_cast<CSS::ColorStyleValue>(value);
    return nullptr;
}

RefPtr<CSS::StringStyleValue> parse_line_style(const CSS::ParsingContext& context, const StringView& part)
{
    auto parsed_value = parse_css_value(context, part);
    if (!parsed_value || !parsed_value->is_string())
        return nullptr;
    auto value = static_ptr_cast<CSS::StringStyleValue>(parsed_value);
    if (value->to_string() == "dotted")
        return value;
    if (value->to_string() == "dashed")
        return value;
    if (value->to_string() == "solid")
        return value;
    if (value->to_string() == "double")
        return value;
    if (value->to_string() == "groove")
        return value;
    if (value->to_string() == "ridge")
        return value;
    return nullptr;
}

class CSSParser {
public:
    CSSParser(const CSS::ParsingContext& context, const StringView& input)
        : m_context(context)
        , css(input)
    {
    }

    bool next_is(const char* str) const
    {
        size_t len = strlen(str);
        for (size_t i = 0; i < len; ++i) {
            if (peek(i) != str[i])
                return false;
        }
        return true;
    }

    char peek(size_t offset = 0) const
    {
        if ((index + offset) < css.length())
            return css[index + offset];
        return 0;
    }

    bool consume_specific(char ch)
    {
        if (peek() != ch) {
            dbgln("CSSParser: Peeked '{:c}' wanted specific '{:c}'", peek(), ch);
        }
        if (!peek()) {
            PARSE_ERROR();
            return false;
        }
        if (peek() != ch) {
            PARSE_ERROR();
            ++index;
            return false;
        }
        ++index;
        return true;
    }

    char consume_one()
    {
        PARSE_ASSERT(index < css.length());
        return css[index++];
    };

    bool consume_whitespace_or_comments()
    {
        size_t original_index = index;
        bool in_comment = false;
        for (; index < css.length(); ++index) {
            char ch = peek();
            if (isspace(ch))
                continue;
            if (!in_comment && ch == '/' && peek(1) == '*') {
                in_comment = true;
                ++index;
                continue;
            }
            if (in_comment && ch == '*' && peek(1) == '/') {
                in_comment = false;
                ++index;
                continue;
            }
            if (in_comment)
                continue;
            break;
        }
        return original_index != index;
    }

    bool is_valid_selector_char(char ch) const
    {
        return isalnum(ch) || ch == '-' || ch == '_' || ch == '(' || ch == ')' || ch == '@';
    }

    bool is_combinator(char ch) const
    {
        return ch == '~' || ch == '>' || ch == '+';
    }

    Optional<CSS::Selector::SimpleSelector> parse_simple_selector()
    {
        auto index_at_start = index;

        if (consume_whitespace_or_comments())
            return {};

        if (!peek() || peek() == '{' || peek() == ',' || is_combinator(peek()))
            return {};

        CSS::Selector::SimpleSelector::Type type;

        if (peek() == '*') {
            type = CSS::Selector::SimpleSelector::Type::Universal;
            consume_one();
            return CSS::Selector::SimpleSelector {
                type,
                CSS::Selector::SimpleSelector::PseudoClass::None,
                CSS::Selector::SimpleSelector::PseudoElement::None,
                String(),
                CSS::Selector::SimpleSelector::AttributeMatchType::None,
                String(),
                String()
            };
        }

        if (peek() == '.') {
            type = CSS::Selector::SimpleSelector::Type::Class;
            consume_one();
        } else if (peek() == '#') {
            type = CSS::Selector::SimpleSelector::Type::Id;
            consume_one();
        } else if (isalpha(peek())) {
            type = CSS::Selector::SimpleSelector::Type::TagName;
        } else {
            type = CSS::Selector::SimpleSelector::Type::Universal;
        }

        if (type != CSS::Selector::SimpleSelector::Type::Universal) {
            while (is_valid_selector_char(peek()))
                buffer.append(consume_one());
            PARSE_ASSERT(!buffer.is_null());
        }

        auto value = String::copy(buffer);

        if (type == CSS::Selector::SimpleSelector::Type::TagName) {
            // Some stylesheets use uppercase tag names, so here's a hack to just lowercase them internally.
            value = value.to_lowercase();
        }

        CSS::Selector::SimpleSelector simple_selector {
            type,
            CSS::Selector::SimpleSelector::PseudoClass::None,
            CSS::Selector::SimpleSelector::PseudoElement::None,
            value,
            CSS::Selector::SimpleSelector::AttributeMatchType::None,
            String(),
            String()
        };
        buffer.clear();

        if (peek() == '[') {
            CSS::Selector::SimpleSelector::AttributeMatchType attribute_match_type = CSS::Selector::SimpleSelector::AttributeMatchType::HasAttribute;
            String attribute_name;
            String attribute_value;
            bool in_value = false;
            consume_specific('[');
            char expected_end_of_attribute_selector = ']';
            while (peek() != expected_end_of_attribute_selector) {
                char ch = consume_one();
                if (ch == '=' || (ch == '~' && peek() == '=')) {
                    if (ch == '=') {
                        attribute_match_type = CSS::Selector::SimpleSelector::AttributeMatchType::ExactValueMatch;
                    } else if (ch == '~') {
                        consume_one();
                        attribute_match_type = CSS::Selector::SimpleSelector::AttributeMatchType::Contains;
                    }
                    attribute_name = String::copy(buffer);
                    buffer.clear();
                    in_value = true;
                    consume_whitespace_or_comments();
                    if (peek() == '\'') {
                        expected_end_of_attribute_selector = '\'';
                        consume_one();
                    } else if (peek() == '"') {
                        expected_end_of_attribute_selector = '"';
                        consume_one();
                    }
                    continue;
                }
                // FIXME: This is a hack that will go away when we replace this with a big boy CSS parser.
                if (ch == '\\')
                    ch = consume_one();
                buffer.append(ch);
            }
            if (in_value)
                attribute_value = String::copy(buffer);
            else
                attribute_name = String::copy(buffer);
            buffer.clear();
            simple_selector.attribute_match_type = attribute_match_type;
            simple_selector.attribute_name = attribute_name;
            simple_selector.attribute_value = attribute_value;
            if (expected_end_of_attribute_selector != ']') {
                if (!consume_specific(expected_end_of_attribute_selector))
                    return {};
            }
            consume_whitespace_or_comments();
            if (!consume_specific(']'))
                return {};
        }

        if (peek() == ':') {
            // FIXME: Implement pseudo elements.
            [[maybe_unused]] bool is_pseudo_element = false;
            consume_one();
            if (peek() == ':') {
                is_pseudo_element = true;
                consume_one();
            }
            if (next_is("not")) {
                buffer.append(consume_one());
                buffer.append(consume_one());
                buffer.append(consume_one());
                if (!consume_specific('('))
                    return {};
                buffer.append('(');
                while (peek() != ')')
                    buffer.append(consume_one());
                if (!consume_specific(')'))
                    return {};
                buffer.append(')');
            } else {
                while (is_valid_selector_char(peek()))
                    buffer.append(consume_one());
            }

            auto pseudo_name = String::copy(buffer);
            buffer.clear();

            // Ignore for now, otherwise we produce a "false positive" selector
            // and apply styles to the element itself, not its pseudo element
            if (is_pseudo_element)
                return {};

            if (pseudo_name.equals_ignoring_case("link"))
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Link;
            else if (pseudo_name.equals_ignoring_case("visited"))
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Visited;
            else if (pseudo_name.equals_ignoring_case("hover"))
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Hover;
            else if (pseudo_name.equals_ignoring_case("focus"))
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Focus;
            else if (pseudo_name.equals_ignoring_case("first-child"))
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::FirstChild;
            else if (pseudo_name.equals_ignoring_case("last-child"))
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::LastChild;
            else if (pseudo_name.equals_ignoring_case("only-child"))
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::OnlyChild;
            else if (pseudo_name.equals_ignoring_case("empty"))
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Empty;
            else if (pseudo_name.equals_ignoring_case("root"))
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Root;
            else if (pseudo_name.equals_ignoring_case("before"))
                simple_selector.pseudo_element = CSS::Selector::SimpleSelector::PseudoElement::Before;
            else if (pseudo_name.equals_ignoring_case("after"))
                simple_selector.pseudo_element = CSS::Selector::SimpleSelector::PseudoElement::After;
        }

        if (index == index_at_start) {
            // We consumed nothing.
            return {};
        }

        return simple_selector;
    }

    Optional<CSS::Selector::ComplexSelector> parse_complex_selector()
    {
        auto relation = CSS::Selector::ComplexSelector::Relation::Descendant;

        if (peek() == '{' || peek() == ',')
            return {};

        if (is_combinator(peek())) {
            switch (peek()) {
            case '>':
                relation = CSS::Selector::ComplexSelector::Relation::ImmediateChild;
                break;
            case '+':
                relation = CSS::Selector::ComplexSelector::Relation::AdjacentSibling;
                break;
            case '~':
                relation = CSS::Selector::ComplexSelector::Relation::GeneralSibling;
                break;
            }
            consume_one();
            consume_whitespace_or_comments();
        }

        consume_whitespace_or_comments();

        Vector<CSS::Selector::SimpleSelector> simple_selectors;
        for (;;) {
            auto component = parse_simple_selector();
            if (!component.has_value())
                break;
            simple_selectors.append(component.value());
            // If this assert triggers, we're most likely up to no good.
            PARSE_ASSERT(simple_selectors.size() < 100);
        }

        if (simple_selectors.is_empty())
            return {};

        return CSS::Selector::ComplexSelector { relation, move(simple_selectors) };
    }

    void parse_selector()
    {
        Vector<CSS::Selector::ComplexSelector> complex_selectors;

        for (;;) {
            auto index_before = index;
            auto complex_selector = parse_complex_selector();
            if (complex_selector.has_value())
                complex_selectors.append(complex_selector.value());
            consume_whitespace_or_comments();
            if (!peek() || peek() == ',' || peek() == '{')
                break;
            // HACK: If we didn't move forward, just let go.
            if (index == index_before)
                break;
        }

        if (complex_selectors.is_empty())
            return;
        complex_selectors.first().relation = CSS::Selector::ComplexSelector::Relation::None;

        current_rule.selectors.append(CSS::Selector(move(complex_selectors)));
    }

    Optional<CSS::Selector> parse_individual_selector()
    {
        parse_selector();
        if (current_rule.selectors.is_empty())
            return {};
        return current_rule.selectors.last();
    }

    void parse_selector_list()
    {
        for (;;) {
            auto index_before = index;
            parse_selector();
            consume_whitespace_or_comments();
            if (peek() == ',') {
                consume_one();
                continue;
            }
            if (peek() == '{')
                break;
            // HACK: If we didn't move forward, just let go.
            if (index_before == index)
                break;
        }
    }

    bool is_valid_property_name_char(char ch) const
    {
        return ch && !isspace(ch) && ch != ':';
    }

    bool is_valid_property_value_char(char ch) const
    {
        return ch && ch != '!' && ch != ';' && ch != '}';
    }

    struct ValueAndImportant {
        String value;
        bool important { false };
    };

    ValueAndImportant consume_css_value()
    {
        buffer.clear();

        int paren_nesting_level = 0;
        bool important = false;

        for (;;) {
            char ch = peek();
            if (ch == '(') {
                ++paren_nesting_level;
                buffer.append(consume_one());
                continue;
            }
            if (ch == ')') {
                PARSE_ASSERT(paren_nesting_level > 0);
                --paren_nesting_level;
                buffer.append(consume_one());
                continue;
            }
            if (paren_nesting_level > 0) {
                buffer.append(consume_one());
                continue;
            }
            if (next_is("!important")) {
                consume_specific('!');
                consume_specific('i');
                consume_specific('m');
                consume_specific('p');
                consume_specific('o');
                consume_specific('r');
                consume_specific('t');
                consume_specific('a');
                consume_specific('n');
                consume_specific('t');
                important = true;
                continue;
            }
            if (next_is("/*")) {
                consume_whitespace_or_comments();
                continue;
            }
            if (!ch)
                break;
            if (ch == '\\') {
                consume_one();
                buffer.append(consume_one());
                continue;
            }
            if (ch == '}')
                break;
            if (ch == ';')
                break;
            buffer.append(consume_one());
        }

        // Remove trailing whitespace.
        while (!buffer.is_empty() && isspace(buffer.last()))
            buffer.take_last();

        auto string = String::copy(buffer);
        buffer.clear();

        return { string, important };
    }

    Optional<CSS::StyleProperty> parse_property()
    {
        consume_whitespace_or_comments();
        if (peek() == ';') {
            consume_one();
            return {};
        }
        if (peek() == '}')
            return {};
        buffer.clear();
        while (is_valid_property_name_char(peek()))
            buffer.append(consume_one());
        auto property_name = String::copy(buffer);
        buffer.clear();
        consume_whitespace_or_comments();
        if (!consume_specific(':'))
            return {};
        consume_whitespace_or_comments();

        auto [property_value, important] = consume_css_value();

        consume_whitespace_or_comments();

        if (peek() && peek() != '}') {
            if (!consume_specific(';'))
                return {};
        }

        auto property_id = CSS::property_id_from_string(property_name);
        if (property_id == CSS::PropertyID::Invalid) {
            dbg() << "CSSParser: Unrecognized property '" << property_name << "'";
        }
        auto value = parse_css_value(m_context, property_value, property_id);
        if (!value)
            return {};
        return CSS::StyleProperty { property_id, value.release_nonnull(), important };
    }

    void parse_declaration()
    {
        for (;;) {
            auto property = parse_property();
            if (property.has_value())
                current_rule.properties.append(property.value());
            consume_whitespace_or_comments();
            if (!peek() || peek() == '}')
                break;
        }
    }

    void parse_rule()
    {
        consume_whitespace_or_comments();
        if (!peek())
            return;

        // FIXME: We ignore @-rules for now.
        if (peek() == '@') {
            while (peek() != '{')
                consume_one();
            int level = 0;
            for (;;) {
                auto ch = consume_one();
                if (ch == '{') {
                    ++level;
                } else if (ch == '}') {
                    --level;
                    if (level == 0)
                        break;
                }
            }
            consume_whitespace_or_comments();
            return;
        }

        parse_selector_list();
        if (!consume_specific('{')) {
            PARSE_ERROR();
            return;
        }
        parse_declaration();
        if (!consume_specific('}')) {
            PARSE_ERROR();
            return;
        }
        rules.append(CSS::StyleRule::create(move(current_rule.selectors), CSS::StyleDeclaration::create(move(current_rule.properties))));
        consume_whitespace_or_comments();
    }

    RefPtr<CSS::StyleSheet> parse_sheet()
    {
        if (peek(0) == (char)0xef && peek(1) == (char)0xbb && peek(2) == (char)0xbf) {
            // HACK: Skip UTF-8 BOM.
            index += 3;
        }

        while (peek()) {
            parse_rule();
        }

        return CSS::StyleSheet::create(move(rules));
    }

    RefPtr<CSS::StyleDeclaration> parse_standalone_declaration()
    {
        consume_whitespace_or_comments();
        for (;;) {
            auto property = parse_property();
            if (property.has_value())
                current_rule.properties.append(property.value());
            consume_whitespace_or_comments();
            if (!peek())
                break;
        }
        return CSS::StyleDeclaration::create(move(current_rule.properties));
    }

private:
    CSS::ParsingContext m_context;

    NonnullRefPtrVector<CSS::StyleRule> rules;

    struct CurrentRule {
        Vector<CSS::Selector> selectors;
        Vector<CSS::StyleProperty> properties;
    };

    CurrentRule current_rule;
    Vector<char> buffer;

    size_t index = 0;

    StringView css;
};

Optional<CSS::Selector> parse_selector(const CSS::ParsingContext& context, const StringView& selector_text)
{
    CSSParser parser(context, selector_text);
    return parser.parse_individual_selector();
}

RefPtr<CSS::StyleSheet> parse_css(const CSS::ParsingContext& context, const StringView& css)
{
    if (css.is_empty())
        return CSS::StyleSheet::create({});
    CSSParser parser(context, css);
    return parser.parse_sheet();
}

RefPtr<CSS::StyleDeclaration> parse_css_declaration(const CSS::ParsingContext& context, const StringView& css)
{
    if (css.is_empty())
        return CSS::StyleDeclaration::create({});
    CSSParser parser(context, css);
    return parser.parse_standalone_declaration();
}

RefPtr<CSS::StyleValue> parse_html_length(const DOM::Document& document, const StringView& string)
{
    auto integer = string.to_int();
    if (integer.has_value())
        return CSS::LengthStyleValue::create(CSS::Length::make_px(integer.value()));
    return parse_css_value(CSS::ParsingContext(document), string);
}

}
