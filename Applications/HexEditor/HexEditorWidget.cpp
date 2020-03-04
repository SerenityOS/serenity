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

#include "HexEditorWidget.h"
#include <AK/Optional.h>
#include <AK/StringBuilder.h>
#include <LibCore/File.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/FontDatabase.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/StatusBar.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/ToolBar.h>
#include <stdio.h>

HexEditorWidget::HexEditorWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_spacing(0);

    m_editor = add<HexEditor>();

    m_editor->on_status_change = [this](int position, HexEditor::EditMode edit_mode, int selection_start, int selection_end) {
        m_statusbar->set_text(0, String::format("Offset: %8X", position));
        m_statusbar->set_text(1, String::format("Edit Mode: %s", edit_mode == HexEditor::EditMode::Hex ? "Hex" : "Text"));
        m_statusbar->set_text(2, String::format("Selection Start: %d", selection_start));
        m_statusbar->set_text(3, String::format("Selection End: %d", selection_end));
        m_statusbar->set_text(4, String::format("Selected Bytes: %d", (selection_end - selection_start) + 1));
    };

    m_editor->on_change = [this] {
        bool was_dirty = m_document_dirty;
        m_document_dirty = true;
        if (!was_dirty)
            update_title();
    };

    m_statusbar = add<GUI::StatusBar>(5);

    m_new_action = GUI::Action::create("New", { Mod_Ctrl, Key_N }, Gfx::Bitmap::load_from_file("/res/icons/16x16/new.png"), [this](const GUI::Action&) {
        if (m_document_dirty) {
            auto save_document_first_box = GUI::MessageBox::construct("Save Document First?", "Warning", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::OKCancel, window());
            auto save_document_first_result = save_document_first_box->exec();

            if (save_document_first_result != GUI::Dialog::ExecResult::ExecOK)
                return;
            m_save_action->activate();
        }

        auto input_box = GUI::InputBox::construct("Enter new file size:", "New file size", window());
        if (input_box->exec() == GUI::InputBox::ExecOK && !input_box->text_value().is_empty()) {
            auto valid = false;
            auto file_size = input_box->text_value().to_int(valid);
            if (valid && file_size > 0) {
                m_document_dirty = false;
                m_editor->set_buffer(ByteBuffer::create_zeroed(file_size));
                set_path(FileSystemPath());
                update_title();
            } else {
                GUI::MessageBox::show("Invalid file size entered.", "Error", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, window());
            }
        }
    });

    m_open_action = GUI::CommonActions::make_open_action([this](auto&) {
        Optional<String> open_path = GUI::FilePicker::get_open_filepath();

        if (!open_path.has_value())
            return;

        open_file(open_path.value());
    });

    m_save_action = GUI::Action::create("Save", { Mod_Ctrl, Key_S }, Gfx::Bitmap::load_from_file("/res/icons/16x16/save.png"), [&](const GUI::Action&) {
        if (!m_path.is_empty()) {
            if (!m_editor->write_to_file(m_path)) {
                GUI::MessageBox::show("Unable to save file.\n", "Error", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, window());
            } else {
                m_document_dirty = false;
                update_title();
            }
            return;
        }

        m_save_as_action->activate();
    });

    m_save_as_action = GUI::Action::create("Save as...", { Mod_Ctrl | Mod_Shift, Key_S }, Gfx::Bitmap::load_from_file("/res/icons/16x16/save.png"), [this](const GUI::Action&) {
        Optional<String> save_path = GUI::FilePicker::get_save_filepath(m_name.is_null() ? "Untitled" : m_name, m_extension.is_null() ? "bin" : m_extension);
        if (!save_path.has_value())
            return;

        if (!m_editor->write_to_file(save_path.value())) {
            GUI::MessageBox::show("Unable to save file.\n", "Error", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, window());
            return;
        }

        m_document_dirty = false;
        set_path(FileSystemPath(save_path.value()));
        dbg() << "Wrote document to " << save_path.value();
    });

    auto menubar = make<GUI::MenuBar>();
    auto app_menu = GUI::Menu::construct("Hex Editor");
    app_menu->add_action(*m_new_action);
    app_menu->add_action(*m_open_action);
    app_menu->add_action(*m_save_action);
    app_menu->add_action(*m_save_as_action);
    app_menu->add_separator();
    app_menu->add_action(GUI::CommonActions::make_quit_action([this](auto&) {
        if (!request_close())
            return;
        GUI::Application::the().quit(0);
    }));
    menubar->add_menu(move(app_menu));

    auto bytes_per_row_menu = GUI::Menu::construct("Bytes Per Row");
    for (int i = 8; i <= 32; i += 8) {
        bytes_per_row_menu->add_action(GUI::Action::create(String::number(i), [this, i](auto&) {
            m_editor->set_bytes_per_row(i);
            m_editor->update();
        }));
    }

    m_goto_decimal_offset_action = GUI::Action::create("Go To Offset (Decimal)...", { Mod_Ctrl | Mod_Shift, Key_G }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"), [this](const GUI::Action&) {
        auto input_box = GUI::InputBox::construct("Enter Decimal offset:", "Go To", window());
        if (input_box->exec() == GUI::InputBox::ExecOK && !input_box->text_value().is_empty()) {
            auto valid = false;
            auto new_offset = input_box->text_value().to_int(valid);
            if (valid) {
                m_editor->set_position(new_offset);
            }
        }
    });

    m_goto_hex_offset_action = GUI::Action::create("Go To Offset (Hex)...", { Mod_Ctrl, Key_G }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"), [this](const GUI::Action&) {
        auto input_box = GUI::InputBox::construct("Enter Hex offset:", "Go To", window());
        if (input_box->exec() == GUI::InputBox::ExecOK && !input_box->text_value().is_empty()) {
            auto new_offset = strtol(input_box->text_value().characters(), nullptr, 16);
            m_editor->set_position(new_offset);
        }
    });

    auto edit_menu = GUI::Menu::construct("Edit");
    edit_menu->add_action(GUI::Action::create("Fill selection...", { Mod_Ctrl, Key_B }, [&](const GUI::Action&) {
        auto input_box = GUI::InputBox::construct("Fill byte (hex):", "Fill Selection", window());
        if (input_box->exec() == GUI::InputBox::ExecOK && !input_box->text_value().is_empty()) {
            auto fill_byte = strtol(input_box->text_value().characters(), nullptr, 16);
            m_editor->fill_selection(fill_byte);
        }
    }));
    edit_menu->add_separator();
    edit_menu->add_action(*m_goto_decimal_offset_action);
    edit_menu->add_action(*m_goto_hex_offset_action);
    edit_menu->add_separator();
    edit_menu->add_action(GUI::Action::create("Copy Hex", { Mod_Ctrl, Key_C }, [&](const GUI::Action&) {
        m_editor->copy_selected_hex_to_clipboard();
    }));
    edit_menu->add_action(GUI::Action::create("Copy Text", { Mod_Ctrl | Mod_Shift, Key_C }, [&](const GUI::Action&) {
        m_editor->copy_selected_text_to_clipboard();
    }));
    edit_menu->add_separator();
    edit_menu->add_action(GUI::Action::create("Copy As C Code", { Mod_Alt | Mod_Shift, Key_C }, [&](const GUI::Action&) {
        m_editor->copy_selected_hex_to_clipboard_as_c_code();
    }));
    menubar->add_menu(move(edit_menu));

    auto view_menu = GUI::Menu::construct("View");
    view_menu->add_submenu(move(bytes_per_row_menu));
    menubar->add_menu(move(view_menu));

    auto help_menu = GUI::Menu::construct("Help");
    help_menu->add_action(GUI::Action::create("About", [&](const GUI::Action&) {
        GUI::AboutDialog::show("Hex Editor", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-hexeditor.png"), window());
    }));
    menubar->add_menu(move(help_menu));

    GUI::Application::the().set_menubar(move(menubar));

    m_editor->set_focus(true);
}

HexEditorWidget::~HexEditorWidget()
{
}

void HexEditorWidget::set_path(const FileSystemPath& file)
{
    m_path = file.string();
    m_name = file.title();
    m_extension = file.extension();
    update_title();
}

void HexEditorWidget::update_title()
{
    StringBuilder builder;
    builder.append("Hex Editor: ");
    builder.append(m_path);
    if (m_document_dirty)
        builder.append(" (*)");
    window()->set_title(builder.to_string());
}

void HexEditorWidget::open_file(const String& path)
{
    auto file = Core::File::construct(path);
    if (!file->open(Core::IODevice::ReadOnly)) {
        GUI::MessageBox::show(String::format("Opening \"%s\" failed: %s", path.characters(), strerror(errno)), "Error", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, window());
        return;
    }

    m_document_dirty = false;
    m_editor->set_buffer(file->read_all()); // FIXME: On really huge files, this is never going to work. Should really create a framework to fetch data from the file on-demand.
    set_path(FileSystemPath(path));
}

bool HexEditorWidget::request_close()
{
    if (!m_document_dirty)
        return true;
    auto result = GUI::MessageBox::show("The file has been modified. Quit without saving?", "Quit without saving?", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::OKCancel, window());
    return result == GUI::MessageBox::ExecOK;
}
