/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GWidget.h>

GAboutDialog::GAboutDialog(const StringView& name, const GraphicsBitmap* icon, CObject* parent)
    : GDialog(parent)
    , m_name(name)
    , m_icon(icon)
{
    resize(230, 120);
    set_title(String::format("About %s", m_name.characters()));
    set_resizable(false);

    auto widget = GWidget::construct();
    set_main_widget(widget);
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GHBoxLayout>());

    auto left_container = GWidget::construct(widget.ptr());
    left_container->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    left_container->set_preferred_size(48, 0);
    left_container->set_layout(make<GVBoxLayout>());
    auto icon_label = GLabel::construct(left_container);
    icon_label->set_icon(m_icon);
    icon_label->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    icon_label->set_preferred_size(40, 40);
    left_container->layout()->add_spacer();

    auto right_container = GWidget::construct(widget.ptr());
    right_container->set_layout(make<GVBoxLayout>());
    right_container->layout()->set_margins({ 0, 4, 4, 4 });

    auto make_label = [&](const StringView& text, bool bold = false) {
        auto label = GLabel::construct(text, right_container);
        label->set_text_alignment(TextAlignment::CenterLeft);
        label->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
        label->set_preferred_size(0, 14);
        if (bold)
            label->set_font(Font::default_bold_font());
    };
    make_label(m_name, true);
    make_label("SerenityOS");
    make_label("(C) The SerenityOS developers");

    right_container->layout()->add_spacer();

    auto button_container = GWidget::construct(right_container.ptr());
    button_container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    button_container->set_preferred_size(0, 20);
    button_container->set_layout(make<GHBoxLayout>());
    button_container->layout()->add_spacer();
    auto ok_button = GButton::construct("OK", button_container);
    ok_button->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    ok_button->set_preferred_size(80, 20);
    ok_button->on_click = [this](auto&) {
        done(GDialog::ExecOK);
    };
}

GAboutDialog::~GAboutDialog()
{
}
