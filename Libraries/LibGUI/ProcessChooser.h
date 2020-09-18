/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <LibCore/Timer.h>
#include <LibGUI/Dialog.h>

namespace GUI {

class ProcessChooser final : public GUI::Dialog {
    C_OBJECT(ProcessChooser);

public:
    virtual ~ProcessChooser() override;

    pid_t pid() const { return m_pid; }

private:
    ProcessChooser(const StringView& window_title = "Process Chooser", const StringView& button_label = "Select", const Gfx::Bitmap* window_icon = nullptr, GUI::Window* parent_window = nullptr);

    void set_pid_from_index_and_close(const ModelIndex&);

    pid_t m_pid { 0 };

    String m_window_title;
    String m_button_label;
    RefPtr<Gfx::Bitmap> m_window_icon;
    RefPtr<TableView> m_table_view;

    bool m_refresh_enabled { true };
    unsigned m_refresh_interval { 1000 };
    RefPtr<Core::Timer> m_refresh_timer;
};

}
