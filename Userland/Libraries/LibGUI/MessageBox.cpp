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
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>

namespace GUI {

ErrorOr<NonnullRefPtr<MessageBox>> MessageBox::create(Window* parent_window, StringView text, StringView title, Type type, InputType input_type)
{
    auto box = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) MessageBox(parent_window, type, input_type)));
    TRY(box->build());
    box->set_title(TRY(String::from_utf8(title)).to_byte_string());
    box->set_text(TRY(String::from_utf8(text)));
    auto size = box->main_widget()->effective_min_size();
    box->resize(TRY(size.width().shrink_value()), TRY(size.height().shrink_value()));

    return box;
}

Dialog::ExecResult MessageBox::show(Window* parent_window, StringView text, StringView title, Type type, InputType input_type)
{
    return MUST(try_show(parent_window, text, title, type, input_type));
}

ErrorOr<Dialog::ExecResult> MessageBox::try_show(Window* parent_window, StringView text, StringView title, Type type, InputType input_type)
{
    auto box = TRY(MessageBox::create(parent_window, text, title, type, input_type));
    if (parent_window)
        box->set_icon(parent_window->icon());
    return box->exec();
}

ErrorOr<Dialog::ExecResult> MessageBox::try_show(Badge<FileSystemAccessServer::ConnectionFromClient>, i32 window_server_client_id, i32 parent_window_id, StringView text, StringView title)
{
    auto box = TRY(MessageBox::create(nullptr, text, title, MessageBox::Type::Warning, MessageBox::InputType::YesNo));
    auto parent_rect = ConnectionToWindowServer::the().get_window_rect_from_client(window_server_client_id, parent_window_id);
    box->center_within(parent_rect);
    box->constrain_to_desktop();
    box->set_screen_position(ScreenPosition::DoNotPosition);
    box->Dialog::show();
    ConnectionToWindowServer::the().set_window_parent_from_client(window_server_client_id, parent_window_id, box->window_id());
    return box->exec();
}

Dialog::ExecResult MessageBox::show_error(Window* parent_window, StringView text)
{
    return MUST(try_show_error(parent_window, text));
}

ErrorOr<Dialog::ExecResult> MessageBox::try_show_error(Window* parent_window, StringView text)
{
    return TRY(try_show(parent_window, text, "Error"sv, GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK));
}

Dialog::ExecResult MessageBox::ask_about_unsaved_changes(Window* parent_window, StringView path, Optional<MonotonicTime> last_unmodified_timestamp)
{
    return MUST(try_ask_about_unsaved_changes(parent_window, path, move(last_unmodified_timestamp)));
}

ErrorOr<Dialog::ExecResult> MessageBox::try_ask_about_unsaved_changes(Window* parent_window, StringView path, Optional<MonotonicTime> last_unmodified_timestamp)
{
    StringBuilder builder;
    TRY(builder.try_append("Save changes to "sv));
    if (path.is_empty())
        TRY(builder.try_append("untitled document"sv));
    else
        TRY(builder.try_appendff("\"{}\"", LexicalPath::basename(path)));
    TRY(builder.try_append(" before closing?"sv));

    if (!path.is_empty() && last_unmodified_timestamp.has_value()) {
        auto age = (MonotonicTime::now() - *last_unmodified_timestamp).to_seconds();
        auto readable_time = human_readable_time(age);
        TRY(builder.try_appendff("\nLast saved {} ago.", readable_time));
    }

    auto box = TRY(MessageBox::create(parent_window, builder.string_view(), "Unsaved Changes"sv, Type::Warning, InputType::YesNoCancel));
    if (parent_window)
        box->set_icon(parent_window->icon());

    if (path.is_empty())
        box->m_yes_button->set_text("Save As..."_string);
    else
        box->m_yes_button->set_text("Save"_string);
    box->m_no_button->set_text("Discard"_string);
    box->m_cancel_button->set_text("Cancel"_string);

    return box->exec();
}

void MessageBox::set_text(String text)
{
    m_text_label->set_text(move(text));
}

MessageBox::MessageBox(Window* parent_window, Type type, InputType input_type)
    : Dialog(parent_window)
    , m_type(type)
    , m_input_type(input_type)
{
    set_resizable(false);
    set_auto_shrink(true);
}

ErrorOr<RefPtr<Gfx::Bitmap>> MessageBox::icon() const
{
    switch (m_type) {
    case Type::Information:
        return TRY(Gfx::Bitmap::load_from_file("/res/icons/32x32/msgbox-information.png"sv));
    case Type::Warning:
        return TRY(Gfx::Bitmap::load_from_file("/res/icons/32x32/msgbox-warning.png"sv));
    case Type::Error:
        return TRY(Gfx::Bitmap::load_from_file("/res/icons/32x32/msgbox-error.png"sv));
    case Type::Question:
        return TRY(Gfx::Bitmap::load_from_file("/res/icons/32x32/msgbox-question.png"sv));
    default:
        return nullptr;
    }
}

bool MessageBox::should_include_ok_button() const
{
    return m_input_type == InputType::OK || m_input_type == InputType::OKCancel || m_input_type == InputType::OKReveal;
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

bool MessageBox::should_include_reveal_button() const
{
    return m_input_type == InputType::OKReveal;
}

ErrorOr<void> MessageBox::build()
{
    auto main_widget = set_main_widget<Widget>();
    main_widget->set_fill_with_background_color(true);
    main_widget->set_layout<VerticalBoxLayout>(8, 6);

    auto& message_container = main_widget->add<Widget>();
    auto message_margins = Margins { 8, m_type != Type::None ? 8 : 0 };
    message_container.set_layout<HorizontalBoxLayout>(message_margins, 8);

    if (auto icon = TRY(this->icon()); icon && m_type != Type::None) {
        auto& image_widget = message_container.add<ImageWidget>();
        image_widget.set_bitmap(icon);
    }

    m_text_label = message_container.add<Label>();
    m_text_label->set_text_wrapping(Gfx::TextWrapping::DontWrap);
    m_text_label->set_autosize(true);
    if (m_type != Type::None)
        m_text_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);

    auto& button_container = main_widget->add<Widget>();
    button_container.set_layout<HorizontalBoxLayout>(Margins {}, 8);

    auto add_button = [&](String text, ExecResult result) -> NonnullRefPtr<Button> {
        auto& button = button_container.add<DialogButton>();
        button.set_text(move(text));
        button.on_click = [this, result](auto) { done(result); };
        return button;
    };

    button_container.add_spacer();
    if (should_include_ok_button())
        m_ok_button = add_button("OK"_string, ExecResult::OK);
    if (should_include_yes_button())
        m_yes_button = add_button("Yes"_string, ExecResult::Yes);
    if (should_include_no_button())
        m_no_button = add_button("No"_string, ExecResult::No);
    if (should_include_cancel_button())
        m_cancel_button = add_button("Cancel"_string, ExecResult::Cancel);
    if (should_include_reveal_button())
        m_reveal_button = add_button("Open folder"_string, ExecResult::Reveal);
    button_container.add_spacer();

    return {};
}

}
