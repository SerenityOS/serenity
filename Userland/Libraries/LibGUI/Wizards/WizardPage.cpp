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

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Wizards/WizardPage.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/SystemTheme.h>

namespace GUI {

WizardPage::WizardPage(const String& title_text, const String& subtitle_text)
    : AbstractWizardPage()
{
    set_layout<VerticalBoxLayout>();
    layout()->set_spacing(0);

    auto& header_widget = add<Widget>();
    header_widget.set_fill_with_background_color(true);
    header_widget.set_background_role(Gfx::ColorRole::Base);
    header_widget.set_fixed_height(58);

    header_widget.set_layout<VerticalBoxLayout>();
    header_widget.layout()->set_margins({ 30, 15, 30, 0 });
    m_title_label = header_widget.add<Label>(title_text);
    m_title_label->set_font(Gfx::FontDatabase::the().default_bold_font());
    m_title_label->set_fixed_height(Gfx::FontDatabase::the().default_bold_font().glyph_height() + 2);
    m_title_label->set_text_alignment(Gfx::TextAlignment::TopLeft);
    m_subtitle_label = header_widget.add<Label>(subtitle_text);
    m_subtitle_label->set_text_alignment(Gfx::TextAlignment::TopLeft);
    m_title_label->set_fixed_height(Gfx::FontDatabase::the().default_font().glyph_height());
    header_widget.layout()->add_spacer();

    auto& separator = add<SeparatorWidget>(Gfx::Orientation::Horizontal);
    separator.set_fixed_height(2);

    m_body_widget = add<Widget>();
    m_body_widget->set_layout<VerticalBoxLayout>();
    m_body_widget->layout()->set_margins({ 20, 20, 20, 20 });
}

void WizardPage::set_page_title(const String& text)
{
    m_title_label->set_text(text);
}

void WizardPage::set_page_subtitle(const String& text)
{
    m_subtitle_label->set_text(text);
}

}
