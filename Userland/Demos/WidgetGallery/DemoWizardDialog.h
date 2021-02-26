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

#include <LibGUI/ProgressBar.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>
#include <LibGUI/Wizards/CoverWizardPage.h>
#include <LibGUI/Wizards/WizardDialog.h>
#include <LibGUI/Wizards/WizardPage.h>

class DemoWizardDialog : public GUI::WizardDialog {
    C_OBJECT(DemoWizardDialog);

public:
    String page_1_location() { return m_page_1_location_text_box->get_text(); }

private:
    DemoWizardDialog(GUI::Window* parent_window);

    RefPtr<GUI::CoverWizardPage> m_front_page;
    RefPtr<GUI::WizardPage> m_page_1;
    RefPtr<GUI::TextBox> m_page_1_location_text_box;

    RefPtr<GUI::WizardPage> m_page_2;
    RefPtr<GUI::ProgressBar> m_page_2_progress_bar;
    int m_page_2_progress_value { 0 };
    RefPtr<Core::Timer> m_page_2_timer;

    RefPtr<GUI::CoverWizardPage> m_back_page;
};
