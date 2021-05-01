/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
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
