/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Model.h>
#include <LibGUI/Variant.h>

namespace GUI {

Variant ModelIndex::data(ModelRole role) const
{
    if (!is_valid())
        return {};

    VERIFY(model());
    return model()->data(*this, role);
}

bool ModelIndex::is_parent_of(ModelIndex const& child) const
{
    auto current_index = child.parent();
    while (current_index.is_valid()) {
        if (current_index == *this)
            return true;
        current_index = current_index.parent();
    }
    return false;
}

ModelIndex ModelIndex::sibling(int row, int column) const
{
    if (!is_valid())
        return {};
    VERIFY(model());
    return model()->index(row, column, parent());
}

ModelIndex ModelIndex::sibling_at_column(int column) const
{
    if (!is_valid())
        return {};
    return sibling(row(), column);
}

}
