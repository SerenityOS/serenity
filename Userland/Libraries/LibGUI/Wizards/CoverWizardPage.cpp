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
    m_content_widget->layout()->set_margins({ 20, 20, 20, 20 });

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
