/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FindDialog.h"
#include <AK/Array.h>
#include <AK/Hex.h>
#include <Applications/HexEditor/FindDialogGML.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/ObservableExtensions.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibRx/Observable.h>

struct Option {
    String title;
    OptionId opt;
    bool enabled;
    bool default_action;
};

static const Array<Option, 2> options = {
    {
        { "ASCII String", OPTION_ASCII_STRING, true, true },
        { "Hex value", OPTION_HEX_VALUE, true, false },
    }
};

int FindDialog::show(GUI::Window* parent_window, String& out_text, ByteBuffer& out_buffer, bool& find_all)
{
    auto vm = FindDialogViewModel::construct();

    auto dialog = FindDialog::construct(vm);

    if (parent_window)
        dialog->set_icon(parent_window->icon());

    if (!out_text.is_empty() && !out_text.is_null())
        vm->text()->set_value(out_text);

    auto result = dialog->exec();

    if (result != GUI::Dialog::ExecOK)
        return result;

    auto selected_option = vm->selected_option()->value();
    out_text = vm->text()->value();
    auto processed = dialog->process_input(out_text, selected_option);

    if (processed.is_error()) {
        GUI::MessageBox::show_error(parent_window, processed.error());
        result = GUI::Dialog::ExecAborted;
    } else {
        out_buffer = move(processed.value());
    }

    find_all = dialog->find_all();

    dbgln("Find: value={} option={} find_all={}", out_text.characters(), (int)selected_option, find_all);
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
        auto decoded = decode_hex(text_value.replace(" ", "", true));
        if (decoded.is_error())
            return String::formatted("Input is invalid: {}", decoded.error().string_literal());

        return decoded.value();
    }

    default:
        VERIFY_NOT_REACHED();
    }
}

FindDialog::FindDialog(NonnullRefPtr<FindDialogViewModel> vm)
    : Dialog(nullptr)
    , m_vm(vm)
{
    resize(280, 146);
    center_on_screen();
    set_resizable(false);
    set_title("Find");

    auto& main_widget = set_main_widget<GUI::Widget>();
    if (!main_widget.load_from_gml(find_dialog_gml))
        VERIFY_NOT_REACHED();

    m_text_editor = *main_widget.find_descendant_of_type_named<GUI::TextBox>("text_editor");
    m_find_button = *main_widget.find_descendant_of_type_named<GUI::Button>("find_button");
    m_find_all_button = *main_widget.find_descendant_of_type_named<GUI::Button>("find_all_button");
    m_cancel_button = *main_widget.find_descendant_of_type_named<GUI::Button>("cancel_button");

    auto& radio_container = *main_widget.find_descendant_of_type_named<GUI::Widget>("radio_container");

    for (size_t i = 0; i < options.size(); i++) {
        auto action = options[i];
        auto& radio = radio_container.add<GUI::RadioButton>();
        radio.set_enabled(action.enabled);
        radio.set_text(action.title);

        auto checked_filtered = radio.checked_observable()
                ->filter([](bool const& checked) { return checked; });

        checked_filtered
        ->transform<OptionId>([=](bool const& checked) { return checked ? options[i].opt : OPTION_INVALID; })
        ->bind_oneway(m_vm->selected_option());

        auto options_transformed = m_vm->selected_option()
        ->transform<bool>([=](OptionId const& opt) { return opt == options[i].opt; });
        
        options_transformed->subscribe([=](auto&) { dbgln("Option {}", i); });
        options_transformed->bind_oneway(radio.checked_observable());
    }

    Rx::bind<String>(m_vm->text(), m_text_editor->text_observable());

    m_vm->can_execute_find()
    ->bind_oneway(m_find_button->enabled_observable());

    m_vm->can_execute_find()
    ->bind_oneway(m_find_all_button->enabled_observable());

    m_text_editor->on_return_pressed = [this] {
        m_find_button->click();
    };

    m_find_button->on_click = [this](auto) {
        auto text = m_vm->text()->value();
        if (!text.is_empty()) {
            done(ExecResult::ExecOK);
        }
    };

    m_find_all_button->on_click = [this](auto) {
        m_find_all = true;
        m_find_button->click();
    };

    m_cancel_button->on_click = [this](auto) {
        done(ExecResult::ExecCancel);
    };
}

FindDialog::~FindDialog()
{
}

FindDialogViewModel::FindDialogViewModel()
    : m_text(Rx::BehaviorSubject<String>::construct(String::empty(), "FindDialogViewModel"))
    , m_selected_option(Rx::BehaviorSubject<OptionId>::construct(OPTION_INVALID, "FindDialogViewModel"))
    , m_can_execute_find(Rx::BehaviorSubject<bool>::construct(false, "FindDialogViewModel"))
{
    for (auto& option : options) {
        if (option.default_action)
            m_selected_option->set_value(option.opt);
    }

    text()->transform<bool>([](String const& s) { return !(s.is_empty()); })
        ->bind_oneway(m_can_execute_find);
}
