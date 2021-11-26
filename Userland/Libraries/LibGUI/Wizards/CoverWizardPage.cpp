/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/BoxLayout.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/Wizards/CoverWizardPage.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/SystemTheme.h>

namespace GUI {

CoverWizardPage::CoverWizardPage()
    : AbstractWizardPage()
{
    set_fill_with_background_color(true);
    set_background_role(Gfx::ColorRole::Base);
    set_layout<HorizontalBoxLayout>();
    m_banner_image_widget = add<ImageWidget>();
    m_banner_image_widget->set_fixed_size(160, 315);
    m_banner_image_widget->load_from_file("/res/graphics/wizard-banner-simple.png");

    m_content_widget = add<Widget>();
    m_content_widget->set_layout<VerticalBoxLayout>();
    m_content_widget->layout()->set_margins(20);

    m_header_label = m_content_widget->add<Label>();
    m_header_label->set_font(Gfx::FontDatabase::the().get("Pebbleton", 14, 700));
    m_header_label->set_text_alignment(Gfx::TextAlignment::TopLeft);
    m_header_label->set_fixed_height(48);

    m_body_label = m_content_widget->add<Label>();
    m_body_label->set_text_alignment(Gfx::TextAlignment::TopLeft);
}

void CoverWizardPage::set_header_text(const String& text)
{
    m_header_label->set_text(text);
}

void CoverWizardPage::set_body_text(const String& text)
{
    m_body_label->set_text(text);
}

}
