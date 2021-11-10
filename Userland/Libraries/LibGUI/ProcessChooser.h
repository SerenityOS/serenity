/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Timer.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/RunningProcessesModel.h>

namespace GUI {

class ProcessChooser final : public GUI::Dialog {
    C_OBJECT(ProcessChooser);

public:
    virtual ~ProcessChooser() override;

    pid_t pid() const { return m_pid; }

private:
    ProcessChooser(StringView window_title = "Process Chooser", StringView button_label = "Select", const Gfx::Bitmap* window_icon = nullptr, GUI::Window* parent_window = nullptr);

    void set_pid_from_index_and_close(const ModelIndex&);

    pid_t m_pid { 0 };

    String m_window_title;
    String m_button_label;
    RefPtr<Gfx::Bitmap> m_window_icon;
    RefPtr<TableView> m_table_view;
    RefPtr<RunningProcessesModel> m_process_model;

    bool m_refresh_enabled { true };
    unsigned m_refresh_interval { 1000 };
    RefPtr<Core::Timer> m_refresh_timer;
};

}
