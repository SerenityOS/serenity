/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Painting/Paintable.h>

namespace Web::Painting {

Paintable::DispatchEventOfSameName Paintable::handle_mousedown(Badge<EventHandler>, Gfx::IntPoint const&, unsigned, unsigned)
{
    return DispatchEventOfSameName::Yes;
}

Paintable::DispatchEventOfSameName Paintable::handle_mouseup(Badge<EventHandler>, Gfx::IntPoint const&, unsigned, unsigned)
{
    return DispatchEventOfSameName::Yes;
}

Paintable::DispatchEventOfSameName Paintable::handle_mousemove(Badge<EventHandler>, Gfx::IntPoint const&, unsigned, unsigned)
{
    return DispatchEventOfSameName::Yes;
}

bool Paintable::handle_mousewheel(Badge<EventHandler>, Gfx::IntPoint const&, unsigned, unsigned, int wheel_delta_x, int wheel_delta_y)
{
    if (auto* containing_block = this->containing_block()) {
        if (!containing_block->is_scrollable())
            return false;
        auto new_offset = containing_block->scroll_offset();
        new_offset.translate_by(wheel_delta_x, wheel_delta_y);
        // FIXME: This const_cast is gross.
        // FIXME: Scroll offset shouldn't live in the layout tree.
        const_cast<Layout::BlockContainer*>(containing_block)->set_scroll_offset(new_offset);
        return true;
    }

    return false;
}

Optional<HitTestResult> Paintable::hit_test(Gfx::FloatPoint const&, HitTestType) const
{
    return {};
}

Paintable const* Paintable::first_child() const
{
    auto* layout_child = m_layout_node.first_child();
    for (; layout_child && !layout_child->paintable(); layout_child = layout_child->next_sibling())
        ;
    return layout_child ? layout_child->paintable() : nullptr;
}

Paintable const* Paintable::next_sibling() const
{
    auto* layout_node = m_layout_node.next_sibling();
    for (; layout_node && !layout_node->paintable(); layout_node = layout_node->next_sibling())
        ;
    return layout_node ? layout_node->paintable() : nullptr;
}

}
