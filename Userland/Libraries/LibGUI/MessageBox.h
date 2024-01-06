/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Time.h>
#include <LibGUI/Dialog.h>

namespace FileSystemAccessServer {

class ConnectionFromClient;

}

namespace GUI {

class MessageBox : public Dialog {
    C_OBJECT_ABSTRACT(MessageBox)
public:
    enum class Type {
        None,
        Information,
        Warning,
        Error,
        Question
    };

    enum class InputType {
        OK,
        OKCancel,
        OKReveal,
        YesNo,
        YesNoCancel,
    };

    virtual ~MessageBox() override = default;

    static ExecResult show(Window* parent_window, StringView text, StringView title, Type type = Type::None, InputType input_type = InputType::OK);
    static ExecResult show_error(Window* parent_window, StringView text);
    static ExecResult ask_about_unsaved_changes(Window* parent_window, StringView path, Optional<MonotonicTime> last_unmodified_timestamp = {});

    static ErrorOr<ExecResult> try_show(Badge<FileSystemAccessServer::ConnectionFromClient>, i32 window_server_client_id, i32 parent_window_id, StringView text, StringView title);
    static ErrorOr<ExecResult> try_show(Window* parent_window, StringView text, StringView title, Type type = Type::None, InputType input_type = InputType::OK);
    static ErrorOr<ExecResult> try_show_error(Window* parent_window, StringView text);
    static ErrorOr<ExecResult> try_ask_about_unsaved_changes(Window* parent_window, StringView path, Optional<MonotonicTime> last_unmodified_timestamp = {});

    static ErrorOr<NonnullRefPtr<MessageBox>> create(Window* parent_window, StringView text, StringView title, Type type = Type::None, InputType input_type = InputType::OK);

    void set_text(String);

private:
    MessageBox(Window* parent_window, Type type = Type::None, InputType input_type = InputType::OK);

    bool should_include_ok_button() const;
    bool should_include_cancel_button() const;
    bool should_include_yes_button() const;
    bool should_include_no_button() const;
    bool should_include_reveal_button() const;

    ErrorOr<void> build();
    ErrorOr<RefPtr<Gfx::Bitmap>> icon() const;

    Type m_type { Type::None };
    InputType m_input_type { InputType::OK };

    RefPtr<GUI::Button> m_ok_button;
    RefPtr<GUI::Button> m_yes_button;
    RefPtr<GUI::Button> m_no_button;
    RefPtr<GUI::Button> m_cancel_button;
    RefPtr<GUI::Button> m_reveal_button;
    RefPtr<Label> m_text_label;
};

}
