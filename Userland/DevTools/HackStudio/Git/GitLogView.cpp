/*
 * Copyright (c) 2023, Abhishek Raturi <raturiabhi1000@gmail.com>
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GitLogView.h"
#include <LibGUI/BoxLayout.h>

namespace HackStudio {

void GitLogView::paint_list_item(GUI::Painter& painter, int row_index, int painted_item_index)
{
    ListView::paint_list_item(painter, row_index, painted_item_index);
}

GitLogView::GitLogView(GitLogActionCallback)
{
    set_alternating_row_colors(true);
}
}
