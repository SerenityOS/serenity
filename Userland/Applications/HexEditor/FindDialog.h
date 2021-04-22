/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Result.h>
#include <AK/Vector.h>
#include <LibGUI/Dialog.h>

enum OptionId {
    OPTION_INVALID = -1,
    OPTION_ASCII_STRING,
    OPTION_HEX_VALUE
};

class FindDialog : public GUI::Dialog {
    C_OBJECT(FindDialog);

public:
    static int show(GUI::Window* parent_window, String& out_tex, ByteBuffer& out_buffer);

private:
    Result<ByteBuffer, String> process_input(String text_value, OptionId opt);

    String text_value() const { return m_text_value; }
    OptionId selected_option() const { return m_selected_option; }

    FindDialog();
    virtual ~FindDialog() override;

    RefPtr<GUI::TextEditor> m_text_editor;

    String m_text_value;
    OptionId m_selected_option { OPTION_INVALID };
};
