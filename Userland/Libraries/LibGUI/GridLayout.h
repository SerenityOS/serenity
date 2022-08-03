/*
 * Copyright (c) 2022, Filiph Sandstrom <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Layout.h>
#include <LibGUI/UIDimensions.h>
#include <LibGUI/Widget.h>

namespace GUI {

class GridLayout final : public Layout {
    C_OBJECT(GridLayout);

public:
    virtual void run(Widget&) override;
    virtual UISize preferred_size() const override;
    virtual UISize min_size() const override;

    int item_size() { return m_item_size; }
    int item_size() const { return m_item_size; }
    void set_item_size(int item_size) { m_item_size = item_size; }

    int columns() { return m_columns; }
    int columns() const { return m_columns; }
    void set_columns(int columns) { m_columns = columns; }

    bool auto_layout() { return m_auto_layout; }
    bool auto_layout() const { return m_auto_layout; }
    // If true the `GridLayout` will automatically handle grid items
    // the take up multiple columns and/or rows. Otherwise it'll treat
    // every child as the same size and spacers will have to be used
    // instead.
    void set_auto_layout(bool auto_layout) { m_auto_layout = auto_layout; }

private:
    explicit GridLayout();
    virtual ~GridLayout() override = default;

    int m_item_size;
    int m_columns;
    bool m_auto_layout = false;
};

}
