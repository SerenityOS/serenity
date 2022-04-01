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

class CoverWizardPage : public AbstractWizardPage {
    C_OBJECT(CoverWizardPage);

    ImageWidget& banner_image_widget() { return *m_banner_image_widget; }

    void set_header_text(String const& text);
    void set_body_text(String const& text);

private:
    explicit CoverWizardPage();

    RefPtr<ImageWidget> m_banner_image_widget;
    RefPtr<Widget> m_content_widget;

    RefPtr<Label> m_header_label;
    RefPtr<Label> m_body_label;
};

}
