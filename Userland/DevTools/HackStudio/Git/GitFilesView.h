/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/ListView.h>
#include <LibGfx/Bitmap.h>

namespace HackStudio {

// A "GitFileAction" is either the staging or the unstaging of a file.
using GitFileActionCallback = Function<void(ByteString const& file)>;

class GitFilesView : public GUI::ListView {
    C_OBJECT(GitFilesView)
public:
    virtual ~GitFilesView() override = default;

protected:
    GitFilesView(GitFileActionCallback, NonnullRefPtr<Gfx::Bitmap> action_icon);

private:
    virtual void paint_list_item(GUI::Painter& painter, int row_index, int painted_item_index) override;

    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual Gfx::IntRect action_icon_rect(size_t painted_item_index);

    GitFileActionCallback m_action_callback;
    NonnullRefPtr<Gfx::Bitmap> m_action_icon;
};

}
