/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/BoxLayout.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/Wizards/CoverWizardPage.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/SystemTheme.h>

namespace GUI {

ErrorOr<NonnullRefPtr<CoverWizardPage>> CoverWizardPage::create(StringView title, StringView subtitle)
{
    auto page = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) CoverWizardPage()));
    TRY(page->build(TRY(String::from_utf8(title)), TRY(String::from_utf8(subtitle))));
    return page;
}

ErrorOr<void> CoverWizardPage::build(String title, String subtitle)
{
    set_fill_with_background_color(true);
    set_background_role(Gfx::ColorRole::Base);
    set_layout<HorizontalBoxLayout>();
    m_banner_image_widget = add<ImageWidget>();
    m_banner_image_widget->set_fixed_size(160, 315);
    m_banner_image_widget->load_from_file("/res/graphics/wizard-banner-simple.png"sv);

    m_content_widget = add<Widget>();
    m_content_widget->set_layout<VerticalBoxLayout>(20);

    m_header_label = m_content_widget->add<Label>(move(title));
    m_header_label->set_font(Gfx::FontDatabase::the().get("Pebbleton"_fly_string, 14, 700, Gfx::FontWidth::Normal, 0));
    m_header_label->set_text_alignment(Gfx::TextAlignment::TopLeft);
    m_header_label->set_fixed_height(48);

    m_body_label = m_content_widget->add<Label>(move(subtitle));
    m_body_label->set_text_alignment(Gfx::TextAlignment::TopLeft);

    return {};
}

void CoverWizardPage::set_header_text(String text)
{
    m_header_label->set_text(move(text));
}

void CoverWizardPage::set_body_text(String text)
{
    m_body_label->set_text(move(text));
}

}
