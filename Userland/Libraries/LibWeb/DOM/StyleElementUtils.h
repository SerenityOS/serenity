/*
 * Copyright (c) 2023, Preston Taylor <PrestonLeeTaylor@proton.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/CSSStyleSheet.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/WebIDL/ObservableArray.h>

namespace Web::DOM {

class StyleElementUtils {
public:
    void update_a_style_block(DOM::Element& style_element);

    CSS::CSSStyleSheet* sheet() { return m_associated_css_style_sheet; }
    CSS::CSSStyleSheet const* sheet() const { return m_associated_css_style_sheet; }

private:
    void remove_a_css_style_sheet(CSS::StyleSheetList&, CSS::CSSStyleSheet& sheet);
    void create_a_css_style_sheet(CSS::StyleSheetList&, String type, DOM::Element* owner_node, String media, String title, bool alternate, bool origin_clean, Optional<String> location, CSS::CSSStyleSheet* parent_style_sheet, CSS::CSSRule* owner_rule, CSS::CSSStyleSheet& sheet);
    void add_a_css_style_sheet(CSS::StyleSheetList&, CSS::CSSStyleSheet& sheet);

    // https://www.w3.org/TR/cssom/#associated-css-style-sheet
    JS::GCPtr<CSS::CSSStyleSheet> m_associated_css_style_sheet;
};

}
