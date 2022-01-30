/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/Result.h>
#include <AK/String.h>
#include <LibRx/BehaviorSubject.h>
#include <LibGUI/Dialog.h>

enum OptionId {
    OPTION_INVALID = -1,
    OPTION_ASCII_STRING,
    OPTION_HEX_VALUE
};

class FindDialogViewModel final : public Core::Object {
    C_OBJECT(FindDialogViewModel);

public:
    NonnullRefPtr<Rx::BehaviorSubject<String>> text() { return m_text; }
    NonnullRefPtr<Rx::BehaviorSubject<OptionId>> selected_option() { return m_selected_option; }
    NonnullRefPtr<Rx::BehaviorSubject<bool>> can_execute_find() { return m_can_execute_find; }

private:
    FindDialogViewModel();

    NonnullRefPtr<Rx::BehaviorSubject<String>> m_text;
    NonnullRefPtr<Rx::BehaviorSubject<OptionId>> m_selected_option;
    NonnullRefPtr<Rx::BehaviorSubject<bool>> m_can_execute_find;
};

class FindDialog : public GUI::Dialog {
    C_OBJECT(FindDialog);

public:
    static int show(GUI::Window* parent_window, String& out_tex, ByteBuffer& out_buffer, bool& find_all);

private:
    Result<ByteBuffer, String> process_input(String text_value, OptionId opt);

    bool find_all() const { return m_find_all; }

    FindDialog(NonnullRefPtr<FindDialogViewModel> vm);
    virtual ~FindDialog() override;

    RefPtr<GUI::TextEditor> m_text_editor;
    RefPtr<GUI::Button> m_find_button;
    RefPtr<GUI::Button> m_find_all_button;
    RefPtr<GUI::Button> m_cancel_button;

    bool m_find_all { false };

    NonnullRefPtr<FindDialogViewModel> m_vm;
};
