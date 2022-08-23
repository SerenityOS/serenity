/*
 * Copyright (c) 2022, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/GridFormattingContext.h>

namespace Web::Layout {

GridFormattingContext::GridFormattingContext(LayoutState& state, BlockContainer const& block_container, FormattingContext* parent)
    : BlockFormattingContext(state, block_container, parent)
{
}

GridFormattingContext::~GridFormattingContext() = default;

void GridFormattingContext::run(Box const& box, LayoutMode)
{
    box.for_each_child_of_type<Box>([&](Box& child_box) {
        (void)layout_inside(child_box, LayoutMode::Normal);
    });
}

}
