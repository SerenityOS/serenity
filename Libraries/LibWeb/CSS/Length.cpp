/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <LibWeb/CSS/Length.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/Page/Frame.h>

namespace Web::CSS {

float Length::relative_length_to_px(const LayoutNode& layout_node) const
{
    switch (m_type) {
    case Type::Ex:
        return m_value * layout_node.specified_style().font().x_height();
    case Type::Em:
        return m_value * layout_node.font_size();
    case Type::Rem:
        return m_value * layout_node.document().document_element()->layout_node()->font_size();
    case Type::Vw:
        return layout_node.document().frame()->viewport_rect().width() * (m_value / 100);
    case Type::Vh:
        return layout_node.document().frame()->viewport_rect().height() * (m_value / 100);
    case Type::Vmin: {
        auto viewport = layout_node.document().frame()->viewport_rect();

        return min(viewport.width(), viewport.height()) * (m_value / 100);
    }
    case Type::Vmax: {
        auto viewport = layout_node.document().frame()->viewport_rect();

        return max(viewport.width(), viewport.height()) * (m_value / 100);
    }
    default:
        ASSERT_NOT_REACHED();
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
    ASSERT_NOT_REACHED();
}

}
