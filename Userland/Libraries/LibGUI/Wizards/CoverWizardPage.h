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
    C_OBJECT_ABSTRACT(CoverWizardPage);

    static ErrorOr<NonnullRefPtr<CoverWizardPage>> create(StringView title, StringView subtitle);

    ImageWidget& banner_image_widget() { return *m_banner_image_widget; }

    void set_header_text(String);
    void set_body_text(String);

protected:
    virtual ErrorOr<void> build(String title, String subtitle);

private:
    CoverWizardPage() = default;

    RefPtr<ImageWidget> m_banner_image_widget;
    RefPtr<Widget> m_content_widget;

    RefPtr<Label> m_header_label;
    RefPtr<Label> m_body_label;
};

}
