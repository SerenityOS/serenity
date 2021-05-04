/*
 * Copyright (c) 2021, Gunnar Beutner <gunnar@beutner.name>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/ComboBox.h>
#include <LibGUI/Frame.h>

namespace Profiler {

class Profile;

class ProcessPickerWidget final : public GUI::Frame {
    C_OBJECT(ProcessPickerWidget)
public:
    virtual ~ProcessPickerWidget() override;

    struct Process {
        pid_t pid;
        String name;
    };

private:
    explicit ProcessPickerWidget(Profile&);

    Profile& m_profile;
    Vector<String> m_processes;

    RefPtr<GUI::ComboBox> m_process_combo;
};

}
