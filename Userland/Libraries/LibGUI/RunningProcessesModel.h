/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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
    virtual ~RunningProcessesModel() override;

    enum Column {
        Icon,
        PID,
        UID,
        Name,
        __Count,
    };

    virtual int row_count(const GUI::ModelIndex&) const override;
    virtual int column_count(const GUI::ModelIndex&) const override;
    virtual String column_name(int column_index) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;

    void update();

private:
    RunningProcessesModel();

    struct Process {
        pid_t pid;
        uid_t uid;
        RefPtr<Gfx::Bitmap> icon;
        String name;
    };
    Vector<Process> m_processes;
};

}
