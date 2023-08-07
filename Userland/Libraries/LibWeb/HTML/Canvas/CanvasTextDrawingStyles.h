/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/CSS/StyleValues/FontStyleValue.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/Canvas/CanvasState.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/canvas.html#canvastextdrawingstyles
template<typename IncludingClass>
class CanvasTextDrawingStyles {
public:
    ~CanvasTextDrawingStyles() = default;

    DeprecatedString font() const
    {
        // When font style value is empty return default string
        if (!my_drawing_state().font_style_value) {
            return "10px sans-serif";
        }

        // On getting, the font attribute must return the serialized form of the current font of the context (with no 'line-height' component).
        auto const& font_style_value = my_drawing_state().font_style_value->as_font();
        return DeprecatedString::formatted("{} {} {} {}",
            font_style_value.font_style()->to_string().release_value_but_fixme_should_propagate_errors(),
            font_style_value.font_weight()->to_string().release_value_but_fixme_should_propagate_errors(),
            font_style_value.font_size()->to_string().release_value_but_fixme_should_propagate_errors(),
            font_style_value.font_families()->to_string().release_value_but_fixme_should_propagate_errors());
    }

    void set_font(DeprecatedString const& font)
    {
        // The font IDL attribute, on setting, must be parsed as a CSS <'font'> value (but without supporting property-independent style sheet syntax like 'inherit'),
        // and the resulting font must be assigned to the context, with the 'line-height' component forced to 'normal', with the 'font-size' component converted to CSS pixels,
        // and with system fonts being computed to explicit values.
        auto parsing_context = CSS::Parser::ParsingContext { reinterpret_cast<IncludingClass&>(*this).realm() };
        auto font_style_value_result = parse_css_value(parsing_context, font, CSS::PropertyID::Font);

        // If the new value is syntactically incorrect (including using property-independent style sheet syntax like 'inherit' or 'initial'), then it must be ignored, without assigning a new font value.
        if (font_style_value_result.is_error()) {
            return;
        }
        my_drawing_state().font_style_value = font_style_value_result.value();

        // Load font with font style value properties
        auto const& font_style_value = my_drawing_state().font_style_value->as_font();
        auto& canvas_element = reinterpret_cast<IncludingClass&>(*this).canvas_element();
        my_drawing_state().current_font = canvas_element.document().style_computer().compute_font_for_style_values(&canvas_element, {}, font_style_value.font_families(), font_style_value.font_size(), font_style_value.font_style(), font_style_value.font_weight(), font_style_value.font_stretch());
    }

    Bindings::CanvasTextAlign text_align() const { return my_drawing_state().text_align; }
    void set_text_align(Bindings::CanvasTextAlign text_align) { my_drawing_state().text_align = text_align; }

    Bindings::CanvasTextBaseline text_baseline() const { return my_drawing_state().text_baseline; }
    void set_text_baseline(Bindings::CanvasTextBaseline text_baseline) { my_drawing_state().text_baseline = text_baseline; }

protected:
    CanvasTextDrawingStyles() = default;

private:
    CanvasState::DrawingState& my_drawing_state() { return reinterpret_cast<IncludingClass&>(*this).drawing_state(); }
    CanvasState::DrawingState const& my_drawing_state() const { return reinterpret_cast<IncludingClass const&>(*this).drawing_state(); }
};

}
