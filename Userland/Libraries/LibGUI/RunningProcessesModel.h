/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Model.h>
#include <LibGfx/Bitmap.h>

namespace GUI {

class RunningProcessesModel final : public GUI::Model {
public:
    static NonnullRefPtr<RunningProcessesModel> create();
    virtual ~RunningProcessesModel() override = default;

    enum Column {
        Icon,
        PID,
        UID,
        Name,
        __Count,
    };

    virtual int row_count(const GUI::ModelIndex&) const override;
    virtual int column_count(const GUI::ModelIndex&) const override;
    virtual ErrorOr<String> column_name(int column_index) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;

    void update();

private:
    RunningProcessesModel() = default;

    struct Process {
        pid_t pid;
        uid_t uid;
        RefPtr<Gfx::Bitmap const> icon;
        ByteString name;
    };
    Vector<Process> m_processes;
};

}
