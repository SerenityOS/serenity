/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/ListView.h>
#include <LibGUI/Painter.h>

class ThumbnailsListView : public GUI::ListView {
    C_OBJECT(ThumbnailsListView)
public:
    virtual ~ThumbnailsListView() override = default;
    void select_list_item(int row_index);

protected:
    virtual void paint_list_item(GUI::Painter& painter, int row_index, int painted_item_index) override;
};
