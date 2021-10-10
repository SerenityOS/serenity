/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Selection/Selection.h>

namespace Web::Selection {

NonnullRefPtr<Selection> Selection::create()
{
    return adopt_ref(*new Selection);
}

DOM::Node* Selection::anchor_node()
{
    TODO();
}

unsigned Selection::anchor_offset()
{
    TODO();
}

DOM::Node* Selection::focus_node()
{
    TODO();
}

unsigned Selection::focus_offset() const
{
    TODO();
}

bool Selection::is_collapsed() const
{
    TODO();
}

unsigned Selection::range_count() const
{
    TODO();
}

String Selection::type() const
{
    TODO();
}

NonnullRefPtr<DOM::Range> Selection::get_range_at(unsigned index)
{
    (void)index;
    TODO();
}

void Selection::add_range(DOM::Range&)
{
    TODO();
}

void Selection::remove_range(DOM::Range&)
{
    TODO();
}

void Selection::remove_all_ranges()
{
    TODO();
}

void Selection::empty()
{
    TODO();
}

void Selection::collapse(DOM::Node*, unsigned offset)
{
    (void)offset;
    TODO();
}

void Selection::set_position(DOM::Node*, unsigned offset)
{
    (void)offset;
    TODO();
}

void Selection::collapse_to_start()
{
    TODO();
}

void Selection::collapse_to_end()
{
    TODO();
}

void Selection::extend(DOM::Node&, unsigned offset)
{
    (void)offset;
    TODO();
}

void Selection::set_base_and_extent(DOM::Node& anchor_node, unsigned anchor_offset, DOM::Node& focus_node, unsigned focus_offset)
{
    (void)anchor_node;
    (void)anchor_offset;
    (void)focus_node;
    (void)focus_offset;
    TODO();
}

void Selection::select_all_children(DOM::Node&)
{
    TODO();
}

void Selection::delete_from_document()
{
    TODO();
}

bool Selection::contains_node(DOM::Node&, bool allow_partial_containment) const
{
    (void)allow_partial_containment;
    TODO();
}

String Selection::to_string() const
{
    TODO();
}

}
