/*
 * Copyright (c) 2021, the SerenityOS developers.
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

#include "FindDialog.h"
#include <AK/Hex.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>

struct Option {
    String title;
    OptionId opt;
    bool enabled;
    bool default_action;
};

static const Vector<Option> options = {
    { "ACII String", OPTION_ASCII_STRING, true, true },
    { "Hex value", OPTION_HEX_VALUE, true, false },
};

int FindDialog::show(GUI::Window* parent_window, String& out_text, ByteBuffer& out_buffer)
{
    auto dialog = FindDialog::construct();

    if (parent_window)
        dialog->set_icon(parent_window->icon());

    if (!out_text.is_empty() && !out_text.is_null())
        dialog->m_text_editor->set_text(out_text);

    auto result = dialog->exec();

    if (result != GUI::Dialog::ExecOK)
        return result;

    auto processed = dialog->process_input(dialog->text_value(), dialog->selected_option());

    out_text = dialog->text_value();

    if (processed.is_error()) {
        GUI::MessageBox::show_error(parent_window, processed.error());
        result = GUI::Dialog::ExecAborted;
    } else {
        out_buffer = move(processed.value());
    }

    dbgln("Find: value={} option={}", dialog->text_value().characters(), (int)dialog->selected_option());
    return result;
}

Result<ByteBuffer, String> FindDialog::process_input(String text_value, OptionId opt)
{
    dbgln("process_input opt={}", (int)opt);
    switch (opt) {
    case OPTION_ASCII_STRING: {
        if (text_value.is_empty())
            return String("Input is empty");

        return text_value.to_byte_buffer();
    }

    case OPTION_HEX_VALUE: {
        text_value.replace(" ", "", true);
        auto decoded = decode_hex(text_value.substring_view(0, text_value.length()));
        if (!decoded.has_value())
            return String("Input contains invalid hex values.");

        return decoded.value();
    }

    default:
        ASSERT_NOT_REACHED();
    }
}

FindDialog::FindDialog()
    : Dialog(nullptr)
{
    resize(280, 180 + ((static_cast<int>(options.size()) - 3) * 16));
    center_on_screen();
    set_resizable(false);
    set_title("Find");

    auto& main = set_main_widget<GUI::Widget>();
    main.set_layout<GUI::VerticalBoxLayout>();
    main.layout()->set_margins({ 8, 8, 8, 8 });
    main.layout()->set_spacing(8);
    main.set_fill_with_background_color(true);

    auto& find_prompt_container = main.add<GUI::Widget>();
    find_prompt_container.set_layout<GUI::HorizontalBoxLayout>();

    find_prompt_container.add<GUI::Label>("Value to find");

    m_text_editor = find_prompt_container.add<GUI::TextBox>();
    m_text_editor->set_fixed_height(19);

    for (size_t i = 0; i < options.size(); i++) {
        auto action = options[i];
        auto& radio = main.add<GUI::RadioButton>();
        radio.set_enabled(action.enabled);
        radio.set_text(action.title);

        radio.on_checked = [this, i](auto) {
            m_selected_option = options[i].opt;
        };

        if (action.default_action) {
            radio.set_checked(true);
            m_selected_option = options[i].opt;
        }
    }

    auto& button_box = main.add<GUI::Widget>();
    button_box.set_layout<GUI::HorizontalBoxLayout>();
    button_box.layout()->set_spacing(8);

    auto& ok_button = button_box.add<GUI::Button>();
    ok_button.on_click = [this](auto) {
        m_text_value = m_text_editor->text();
        done(ExecResult::ExecOK);
    };
    ok_button.set_text("OK");

    auto& cancel_button = button_box.add<GUI::Button>();
    cancel_button.on_click = [this](auto) {
        done(ExecResult::ExecCancel);
    };
    cancel_button.set_text("Cancel");
}

FindDialog::~FindDialog()
{
}
