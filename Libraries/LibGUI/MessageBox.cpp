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

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGfx/Font.h>
#include <stdio.h>

namespace GUI {

int MessageBox::show(const StringView& text, const StringView& title, Type type, InputType input_type, Core::Object* parent)
{
    auto box = MessageBox::construct(text, title, type, input_type);
    if (parent)
        parent->add_child(box);
    return box->exec();
}

MessageBox::MessageBox(const StringView& text, const StringView& title, Type type, InputType input_type, Core::Object* parent)
    : Dialog(parent)
    , m_text(text)
    , m_type(type)
    , m_input_type(input_type)
{
    set_title(title);
    build();
}

MessageBox::~MessageBox()
{
}

RefPtr<Gfx::Bitmap> MessageBox::icon() const
{
    switch (m_type) {
    case Type::Information:
        return Gfx::Bitmap::load_from_file("/res/icons/32x32/msgbox-information.png");
    case Type::Warning:
        return Gfx::Bitmap::load_from_file("/res/icons/32x32/msgbox-warning.png");
    case Type::Error:
        return Gfx::Bitmap::load_from_file("/res/icons/32x32/msgbox-error.png");
    default:
        return nullptr;
    }
}

bool MessageBox::should_include_ok_button() const
{
    return m_input_type == InputType::OK || m_input_type == InputType::OKCancel;
}

bool MessageBox::should_include_cancel_button() const
{
    return m_input_type == InputType::OKCancel || m_input_type == InputType::YesNoCancel;
}

bool MessageBox::should_include_yes_button() const
{
    return m_input_type == InputType::YesNo || m_input_type == InputType::YesNoCancel;
}

bool MessageBox::should_include_no_button() const
{
    return should_include_yes_button();
}

void MessageBox::build()
{
    auto& widget = set_main_widget<Widget>();

    int text_width = widget.font().width(m_text);
    int icon_width = 0;

    widget.set_layout<VerticalBoxLayout>();
    widget.set_fill_with_background_color(true);

    widget.layout()->set_margins({ 0, 15, 0, 15 });
    widget.layout()->set_spacing(15);

    RefPtr<Widget> message_container = widget;
    if (m_type != Type::None) {
        message_container = widget.add<Widget>();
        message_container->set_layout<HorizontalBoxLayout>();
        message_container->layout()->set_margins({ 8, 0, 8, 0 });
        message_container->layout()->set_spacing(8);

        auto& icon_label = message_container->add<Label>();
        icon_label.set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
        icon_label.set_preferred_size(32, 32);
        icon_label.set_icon(icon());
        icon_width = icon_label.icon()->width();
    }

    auto& label = message_container->add<Label>(m_text);
    label.set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    label.set_preferred_size(text_width, 16);

    auto& button_container = widget.add<Widget>();
    button_container.set_layout<HorizontalBoxLayout>();
    button_container.layout()->set_spacing(5);
    button_container.layout()->set_margins({ 15, 0, 15, 0 });

    auto add_button = [&](String label, Dialog::ExecResult result) {
        auto& button = button_container.add<Button>();
        button.set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
        button.set_preferred_size(0, 20);
        button.set_text(label);
        button.on_click = [this, label, result] {
            dbg() << "GUI::MessageBox: '" << label << "' button clicked";
            done(result);
        };
    };

    if (should_include_ok_button())
        add_button("OK", Dialog::ExecOK);
    if (should_include_yes_button())
        add_button("Yes", Dialog::ExecYes);
    if (should_include_no_button())
        add_button("No", Dialog::ExecNo);
    if (should_include_cancel_button())
        add_button("Cancel", Dialog::ExecCancel);

    set_rect(x(), y(), text_width + icon_width + 80, 100);
    set_resizable(false);
}

}
