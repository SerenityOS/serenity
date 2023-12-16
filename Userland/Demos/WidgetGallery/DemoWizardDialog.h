/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Progressbar.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>
#include <LibGUI/Wizards/CoverWizardPage.h>
#include <LibGUI/Wizards/WizardDialog.h>
#include <LibGUI/Wizards/WizardPage.h>

class DemoWizardDialog : public GUI::WizardDialog {
    C_OBJECT(DemoWizardDialog);

public:
    ByteString page_1_location() { return m_page_1_location_text_box->get_text(); }

private:
    DemoWizardDialog(GUI::Window* parent_window);

    RefPtr<GUI::CoverWizardPage> m_front_page;
    RefPtr<GUI::WizardPage> m_page_1;
    RefPtr<GUI::TextBox> m_page_1_location_text_box;

    RefPtr<GUI::WizardPage> m_page_2;
    RefPtr<GUI::Progressbar> m_page_2_progressbar;
    int m_page_2_progress_value { 0 };
    RefPtr<Core::Timer> m_page_2_timer;

    RefPtr<GUI::CoverWizardPage> m_back_page;
};
