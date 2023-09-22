/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Wizards/WizardPage.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/SystemTheme.h>

namespace GUI {

ErrorOr<NonnullRefPtr<WizardPage>> WizardPage::create(StringView title, StringView subtitle)
{
    auto page = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) WizardPage()));
    TRY(page->build(TRY(String::from_utf8(title)), TRY(String::from_utf8(subtitle))));
    return page;
}

ErrorOr<void> WizardPage::build(String title, String subtitle)
{
    set_layout<VerticalBoxLayout>(Margins {}, 0);

    auto& header_widget = add<Widget>();
    header_widget.set_fill_with_background_color(true);
    header_widget.set_background_role(Gfx::ColorRole::Base);
    header_widget.set_fixed_height(58);

    header_widget.set_layout<VerticalBoxLayout>(Margins { 15, 30, 0 });
    m_title_label = header_widget.add<Label>(move(title));
    m_title_label->set_font(Gfx::FontDatabase::default_font().bold_variant());
    m_title_label->set_fixed_height(m_title_label->font().pixel_size_rounded_up() + 2);
    m_title_label->set_text_alignment(Gfx::TextAlignment::TopLeft);

    m_subtitle_label = header_widget.add<Label>(move(subtitle));
    m_subtitle_label->set_text_alignment(Gfx::TextAlignment::TopLeft);
    m_subtitle_label->set_fixed_height(m_subtitle_label->font().pixel_size_rounded_up());
    header_widget.add_spacer();

    auto& separator = add<SeparatorWidget>(Gfx::Orientation::Horizontal);
    separator.set_fixed_height(2);

    m_body_widget = add<Widget>();
    m_body_widget->set_layout<VerticalBoxLayout>(20);

    return {};
}

void WizardPage::set_page_title(String text)
{
    m_title_label->set_text(move(text));
}

void WizardPage::set_page_subtitle(String text)
{
    m_subtitle_label->set_text(move(text));
}

}
