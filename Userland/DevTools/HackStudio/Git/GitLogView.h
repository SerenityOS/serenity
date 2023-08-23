/*
 * Copyright (c) 2023, Abhishek Raturi <raturiabhi1000@gmail.com>
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "GitFilesView.h"
#include "GitRepo.h"
#include <AK/Function.h>
#include <LibGUI/Forward.h>
#include <LibGUI/TableView.h>

namespace HackStudio {

using GitLogActionCallback = Function<void(void)>;
class GitLogView : public GUI::ListView {
    C_OBJECT(GitLogView)
public:
    virtual ~GitLogView() override = default;

protected:
    GitLogView(GitLogActionCallback);

private:
    virtual void paint_list_item(GUI::Painter& painter, int row_index, int painted_item_index) override;
    GitFileActionCallback m_action_callback;
};

}
