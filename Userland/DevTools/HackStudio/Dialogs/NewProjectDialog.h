/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ProjectTemplatesModel.h"
#include <DevTools/HackStudio/ProjectTemplate.h>

#include <AK/Result.h>
#include <LibGUI/Button.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Label.h>
#include <LibGUI/TextBox.h>

namespace HackStudio {

class NewProjectDialog : public GUI::Dialog {
    C_OBJECT(NewProjectDialog);

public:
    static ExecResult show(GUI::Window* parent_window);

    Optional<ByteString> created_project_path() const { return m_created_project_path; }

private:
    NewProjectDialog(GUI::Window* parent);
    virtual ~NewProjectDialog() override = default;

    void update_dialog();
    Optional<ByteString> get_available_project_name();
    Optional<ByteString> get_project_full_path();

    void do_create_project();

    RefPtr<ProjectTemplate> selected_template();

    NonnullRefPtr<ProjectTemplatesModel> m_model;
    bool m_input_valid { false };

    RefPtr<GUI::Widget> m_icon_view_container;
    RefPtr<GUI::IconView> m_icon_view;

    RefPtr<GUI::Label> m_description_label;
    RefPtr<GUI::TextBox> m_name_input;
    RefPtr<GUI::TextBox> m_create_in_input;
    RefPtr<GUI::Label> m_full_path_label;

    RefPtr<GUI::Button> m_ok_button;
    RefPtr<GUI::Button> m_cancel_button;
    RefPtr<GUI::Button> m_browse_button;

    Optional<ByteString> m_created_project_path;
};

}
