/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
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

#include "ProjectTemplatesModel.h"
#include <DevTools/HackStudio/ProjectTemplate.h>

#include <AK/Result.h>
#include <AK/Vector.h>
#include <LibGUI/Button.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/Label.h>
#include <LibGUI/TextBox.h>

namespace HackStudio {

class NewProjectDialog : public GUI::Dialog {
    C_OBJECT(NewProjectDialog);

public:
    static int show(GUI::Window* parent_window);

    Optional<String> created_project_path() const { return m_created_project_path; }

private:
    NewProjectDialog(GUI::Window* parent);
    virtual ~NewProjectDialog() override;

    void update_dialog();
    Optional<String> get_available_project_name();
    Optional<String> get_project_full_path();

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

    Optional<String> m_created_project_path;
};

}
