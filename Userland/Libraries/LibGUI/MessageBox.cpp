/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGfx/Font.h>

namespace GUI {

int MessageBox::show(Window* parent_window, StringView text, StringView title, Type type, InputType input_type)
{
    auto box = MessageBox::construct(parent_window, text, title, type, input_type);
    if (parent_window)
        box->set_icon(parent_window->icon());
    return box->exec();
}

int MessageBox::show_error(Window* parent_window, StringView text)
{
    return show(parent_window, text, "Error", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK);
}

MessageBox::MessageBox(Window* parent_window, StringView text, StringView title, Type type, InputType input_type)
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
        return Gfx::Bitmap::try_load_from_file("/res/icons/32x32/msgbox-information.png").release_value_but_fixme_should_propagate_errors();
    case Type::Warning:
        return Gfx::Bitmap::try_load_from_file("/res/icons/32x32/msgbox-warning.png").release_value_but_fixme_should_propagate_errors();
    case Type::Error:
        return Gfx::Bitmap::try_load_from_file("/res/icons/32x32/msgbox-error.png").release_value_but_fixme_should_propagate_errors();
    case Type::Question:
        return Gfx::Bitmap::try_load_from_file("/res/icons/32x32/msgbox-question.png").release_value_but_fixme_should_propagate_errors();
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
    auto number_of_lines = m_text.split('\n').size();
    int padded_text_height = widget.font().glyph_height() * 1.6;
    int total_text_height = number_of_lines * padded_text_height;
    int icon_width = 0;

    widget.set_layout<VerticalBoxLayout>();
    widget.set_fill_with_background_color(true);

    widget.layout()->set_margins(8);
    widget.layout()->set_spacing(6);

    auto& message_container = widget.add<Widget>();
    message_container.set_layout<HorizontalBoxLayout>();
    message_container.layout()->set_spacing(8);

    if (m_type != Type::None) {
        auto& icon_image = message_container.add<ImageWidget>();
        icon_image.set_bitmap(icon());
        if (icon()) {
            icon_width = icon()->width();
            if (icon_width > 0)
                message_container.layout()->set_margins({ 0, 0, 0, 8 });
        }
    }

    auto& label = message_container.add<Label>(m_text);
    label.set_fixed_height(total_text_height);
    if (m_type != Type::None)
        label.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    auto& button_container = widget.add<Widget>();
    button_container.set_layout<HorizontalBoxLayout>();
    button_container.set_fixed_height(24);
    button_container.layout()->set_spacing(8);

    constexpr int button_width = 80;
    int button_count = 0;

    auto add_button = [&](String label, Dialog::ExecResult result) {
        auto& button = button_container.add<Button>();
        button.set_fixed_width(button_width);
        button.set_text(label);
        button.on_click = [this, label, result](auto) {
            done(result);
        };
        ++button_count;
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

    int width = (button_count * button_width) + ((button_count - 1) * button_container.layout()->spacing()) + 32;
    width = max(width, text_width + icon_width + 56);

    set_rect(x(), y(), width, 80 + label.max_height());
    set_resizable(false);
}

}
