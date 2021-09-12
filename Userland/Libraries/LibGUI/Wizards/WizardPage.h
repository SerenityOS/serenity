/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Wizards/AbstractWizardPage.h>

namespace GUI {

class WizardPage : public AbstractWizardPage {
    C_OBJECT(WizardPage);

    Widget& body_widget() { return *m_body_widget; };

    void set_page_title(const String& text);
    void set_page_subtitle(const String& text);

private:
    explicit WizardPage(const String& title_text, const String& subtitle_text);

    RefPtr<Widget> m_body_widget;

    RefPtr<Label> m_title_label;
    RefPtr<Label> m_subtitle_label;
};

}
