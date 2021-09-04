/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Wizards/AbstractWizardPage.h>

namespace GUI {

class WizardPage : public AbstractWizardPage {
    C_OBJECT(WizardPage);

    Widget& body_widget() { return *m_body_widget; };

    void set_page_title(String const& text);
    void set_page_subtitle(String const& text);

private:
    explicit WizardPage(String const& title_text, String const& subtitle_text);

    RefPtr<Widget> m_body_widget;

    RefPtr<Label> m_title_label;
    RefPtr<Label> m_subtitle_label;
};

}
