/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/NumberFormat.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGfx/Font/Font.h>

namespace GUI {

Dialog::ExecResult MessageBox::show(Window* parent_window, StringView text, StringView title, Type type, InputType input_type)
{
    auto box = MessageBox::construct(parent_window, text, title, type, input_type);
    if (parent_window)
        box->set_icon(parent_window->icon());
    return box->exec();
}

Dialog::ExecResult MessageBox::show_error(Window* parent_window, StringView text)
{
    return show(parent_window, text, "Error"sv, GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK);
}

Dialog::ExecResult MessageBox::ask_about_unsaved_changes(Window* parent_window, StringView path, Optional<Time> last_unmodified_timestamp)
{
    StringBuilder builder;
    builder.append("Save changes to "sv);
    if (path.is_empty())
        builder.append("untitled document"sv);
    else
        builder.appendff("\"{}\"", LexicalPath::basename(path));
    builder.append(" before closing?"sv);

    if (!path.is_empty() && last_unmodified_timestamp.has_value()) {
        auto age = (Time::now_monotonic() - *last_unmodified_timestamp).to_seconds();
        auto readable_time = human_readable_time(age);
        builder.appendff("\nLast saved {} ago.", readable_time);
    }

    auto box = MessageBox::construct(parent_window, builder.string_view(), "Unsaved changes"sv, Type::Warning, InputType::YesNoCancel);
    if (parent_window)
        box->set_icon(parent_window->icon());

    if (path.is_empty())
        box->m_yes_button->set_text("Save As..."_string.release_value_but_fixme_should_propagate_errors());
    else
        box->m_yes_button->set_text("Save"_short_string);
    box->m_no_button->set_text("Discard"_short_string);
    box->m_cancel_button->set_text("Cancel"_short_string);

    return box->exec();
}

void MessageBox::set_text(DeprecatedString text)
{
    m_text = move(text);
    build();
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

RefPtr<Gfx::Bitmap> MessageBox::icon() const
{
    switch (m_type) {
    case Type::Information:
        return Gfx::Bitmap::load_from_file("/res/icons/32x32/msgbox-information.png"sv).release_value_but_fixme_should_propagate_errors();
    case Type::Warning:
        return Gfx::Bitmap::load_from_file("/res/icons/32x32/msgbox-warning.png"sv).release_value_but_fixme_should_propagate_errors();
    case Type::Error:
        return Gfx::Bitmap::load_from_file("/res/icons/32x32/msgbox-error.png"sv).release_value_but_fixme_should_propagate_errors();
    case Type::Question:
        return Gfx::Bitmap::load_from_file("/res/icons/32x32/msgbox-question.png"sv).release_value_but_fixme_should_propagate_errors();
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
    auto widget = set_main_widget<Widget>().release_value_but_fixme_should_propagate_errors();

    int text_width = widget->font().width(m_text);
    auto number_of_lines = m_text.split('\n').size();
    int padded_text_height = widget->font().pixel_size_rounded_up() * 1.6;
    int total_text_height = number_of_lines * padded_text_height;
    int icon_width = 0;

    widget->set_layout<VerticalBoxLayout>(8, 6);
    widget->set_fill_with_background_color(true);

    auto& message_container = widget->add<Widget>();
    message_container.set_layout<HorizontalBoxLayout>(GUI::Margins {}, 8);

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

    auto& button_container = widget->add<Widget>();
    button_container.set_layout<HorizontalBoxLayout>(GUI::Margins {}, 8);
    button_container.set_fixed_height(24);

    constexpr int button_width = 80;
    int button_count = 0;

    auto add_button = [&](String label, ExecResult result) -> GUI::Button& {
        auto& button = button_container.add<Button>();
        button.set_fixed_width(button_width);
        button.set_text(move(label));
        button.on_click = [this, result](auto) {
            done(result);
        };
        ++button_count;
        return button;
    };

    button_container.add_spacer().release_value_but_fixme_should_propagate_errors();
    if (should_include_ok_button())
        m_ok_button = add_button("OK"_short_string, ExecResult::OK);
    if (should_include_yes_button())
        m_yes_button = add_button("Yes"_short_string, ExecResult::Yes);
    if (should_include_no_button())
        m_no_button = add_button("No"_short_string, ExecResult::No);
    if (should_include_cancel_button())
        m_cancel_button = add_button("Cancel"_short_string, ExecResult::Cancel);
    button_container.add_spacer().release_value_but_fixme_should_propagate_errors();

    int width = (button_count * button_width) + ((button_count - 1) * button_container.layout()->spacing()) + 32;
    width = max(width, text_width + icon_width + 56);

    // FIXME: Use shrink from new layout system
    set_rect(x(), y(), width, 80 + label.text_calculated_preferred_height());
    set_resizable(false);
}

}
