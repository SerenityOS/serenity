/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf8View.h>
#include <LibWeb/DOM/Range.h>
#include <LibWeb/Layout/LayoutState.h>
#include <LibWeb/Layout/Viewport.h>
#include <ctype.h>

namespace Web::Layout {

bool LineBoxFragment::ends_in_whitespace() const
{
    auto text = this->text();
    if (text.is_empty())
        return false;
    return isspace(text[text.length() - 1]);
}

bool LineBoxFragment::is_justifiable_whitespace() const
{
    return text() == " ";
}

StringView LineBoxFragment::text() const
{
    if (!is<TextNode>(layout_node()))
        return {};
    return verify_cast<TextNode>(layout_node()).text_for_rendering().bytes_as_string_view().substring_view(m_start, m_length);
}

CSSPixelRect const LineBoxFragment::absolute_rect() const
{
    CSSPixelRect rect { {}, size() };
    rect.set_location(m_layout_node->containing_block()->paintable_box()->absolute_position());
    rect.translate_by(offset());
    return rect;
}

bool LineBoxFragment::is_atomic_inline() const
{
    return layout_node().is_replaced_box() || (layout_node().display().is_inline_outside() && !layout_node().display().is_flow_inside());
}

}
