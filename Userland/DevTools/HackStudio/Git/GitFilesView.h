/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/LexicalPath.h>
#include <LibGUI/ListView.h>
#include <LibGfx/Bitmap.h>

namespace HackStudio {

// A "GitFileAction" is either the staging or the unstaging of a file.
typedef Function<void(const LexicalPath& file)> GitFileActionCallback;

class GitFilesView : public GUI::ListView {
    C_OBJECT(GitFilesView)
public:
    virtual ~GitFilesView() override;

protected:
    GitFilesView(GitFileActionCallback, NonnullRefPtr<Gfx::Bitmap> action_icon);

private:
    virtual void paint_list_item(GUI::Painter& painter, int row_index, int painted_item_index);

    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual Gfx::IntRect action_icon_rect(size_t painted_item_index);

    GitFileActionCallback m_action_callback;
    NonnullRefPtr<Gfx::Bitmap> m_action_icon;
};

}
