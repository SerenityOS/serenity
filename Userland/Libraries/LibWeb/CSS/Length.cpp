/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Length.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/Page/BrowsingContext.h>

namespace Web::CSS {

float Length::relative_length_to_px(const Layout::Node& layout_node) const
{
    switch (m_type) {
    case Type::Ex:
        return m_value * layout_node.font().x_height();
    case Type::Em:
        return m_value * layout_node.font_size();
    case Type::Rem:
        return m_value * layout_node.document().document_element()->layout_node()->font_size();
    case Type::Vw:
        return layout_node.document().browsing_context()->viewport_rect().width() * (m_value / 100);
    case Type::Vh:
        return layout_node.document().browsing_context()->viewport_rect().height() * (m_value / 100);
    case Type::Vmin: {
        auto viewport = layout_node.document().browsing_context()->viewport_rect();

        return min(viewport.width(), viewport.height()) * (m_value / 100);
    }
    case Type::Vmax: {
        auto viewport = layout_node.document().browsing_context()->viewport_rect();

        return max(viewport.width(), viewport.height()) * (m_value / 100);
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

const char* Length::unit_name() const
{
    switch (m_type) {
    case Type::Cm:
        return "cm";
    case Type::In:
        return "in";
    case Type::Px:
        return "px";
    case Type::Pt:
        return "pt";
    case Type::Mm:
        return "mm";
    case Type::Q:
        return "Q";
    case Type::Pc:
        return "pc";
    case Type::Ex:
        return "ex";
    case Type::Em:
        return "em";
    case Type::Rem:
        return "rem";
    case Type::Auto:
        return "auto";
    case Type::Percentage:
        return "%";
    case Type::Undefined:
        return "undefined";
    case Type::Vh:
        return "vh";
    case Type::Vw:
        return "vw";
    case Type::Vmax:
        return "vmax";
    case Type::Vmin:
        return "vmin";
    }
    VERIFY_NOT_REACHED();
}

}
