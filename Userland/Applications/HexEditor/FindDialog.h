/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "FindWidget.h"
#include <AK/Result.h>
#include <AK/String.h>
#include <LibGUI/Dialog.h>

enum class OptionId {
    Invalid = -1,
    AsciiString,
    HexValue
};

class FindDialog : public GUI::Dialog {
    C_OBJECT_ABSTRACT(FindDialog);

public:
    static ExecResult show(GUI::Window* parent_window, String& out_text, ByteBuffer& out_buffer, bool& find_all);
    static ErrorOr<NonnullRefPtr<FindDialog>> try_create();

private:
    static ErrorOr<ByteBuffer> process_input(StringView text_value, OptionId opt);

    String text_value() const { return m_text_value; }
    OptionId selected_option() const { return m_selected_option; }
    bool find_all() const { return m_find_all; }

    FindDialog(NonnullRefPtr<HexEditor::FindWidget> find_widget);
    virtual ~FindDialog() override = default;

    RefPtr<GUI::TextEditor> m_text_editor;
    RefPtr<GUI::Button> m_find_button;
    RefPtr<GUI::Button> m_find_all_button;
    RefPtr<GUI::Button> m_cancel_button;

    bool m_find_all { false };
    String m_text_value;
    OptionId m_selected_option { OptionId::Invalid };
};
