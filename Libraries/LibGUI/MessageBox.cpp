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
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGfx/Font.h>
#include <stdio.h>

namespace GUI {

int MessageBox::show(Window* parent_window, const StringView& text, const StringView& title, Type type, InputType input_type)
{
    auto box = MessageBox::construct(parent_window, text, title, type, input_type);
    if (parent_window)
        box->set_icon(parent_window->icon());
    return box->exec();
}

int MessageBox::show_error(Window* parent_window, const StringView& text)
{
    return show(parent_window, text, "Error", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK);
}

MessageBox::MessageBox(Window* parent_window, const StringView& text, const StringView& title, Type type, InputType input_type)
    : Dialog(parent_window)
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
    case Type::Question:
        return Gfx::Bitmap::load_from_file("/res/icons/32x32/msgbox-question.png");
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

    widget.layout()->set_margins({ 8, 8, 8, 8 });
    widget.layout()->set_spacing(8);

    auto& message_container = widget.add<Widget>();
    message_container.set_layout<HorizontalBoxLayout>();
    message_container.layout()->set_margins({ 8, 0, 0, 0 });
    message_container.layout()->set_spacing(8);

    if (m_type != Type::None) {
        auto& icon_image = message_container.add<ImageWidget>();
        icon_image.set_bitmap(icon());
        if (icon())
            icon_width = icon()->width();
    }

    auto& label = message_container.add<Label>(m_text);
    label.set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    label.set_preferred_size(text_width, 16);
    if (m_type != Type::None)
        label.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    auto& button_container = widget.add<Widget>();
    button_container.set_layout<HorizontalBoxLayout>();
    button_container.set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    button_container.set_preferred_size(0, 24);
    button_container.layout()->set_spacing(8);

    auto add_button = [&](String label, Dialog::ExecResult result) {
        auto& button = button_container.add<Button>();
        button.set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
        button.set_preferred_size(96, 0);
        button.set_text(label);
        button.on_click = [this, label, result](auto) {
            done(result);
        };
    };

    button_container.layout()->add_spacer();
    if (should_include_ok_button())
        add_button("OK", Dialog::ExecOK);
    if (should_include_yes_button())
        add_button("Yes", Dialog::ExecYes);
    if (should_include_no_button())
        add_button("No", Dialog::ExecNo);
    if (should_include_cancel_button())
        add_button("Cancel", Dialog::ExecCancel);
    button_container.layout()->add_spacer();

    int width = button_container.child_widgets().size() * 96 + 32;
    if (width < text_width + icon_width + 56)
        width = text_width + icon_width + 56;

    set_rect(x(), y(), width, 96);
    set_resizable(false);
}

}
