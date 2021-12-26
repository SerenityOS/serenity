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

#include "TextEditorWidget.h"
#include <AK/Optional.h>
#include <AK/StringBuilder.h>
#include <AK/URL.h>
#include <LibCore/File.h>
#include <LibCore/MimeData.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CppSyntaxHighlighter.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/FontDatabase.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/StatusBar.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/ToolBar.h>
#include <LibGfx/Font.h>

TextEditorWidget::TextEditorWidget()
{
    set_layout(make<GUI::VerticalBoxLayout>());
    layout()->set_spacing(0);

    auto toolbar = add<GUI::ToolBar>();
    m_editor = add<GUI::TextEditor>();
    m_editor->set_ruler_visible(true);
    m_editor->set_automatic_indentation_enabled(true);
    m_editor->set_line_wrapping_enabled(true);

    m_editor->on_change = [this] {
        // Do not mark as diry on the first change (When document is first opened.)
        if (m_document_opening) {
            m_document_opening = false;
            return;
        }

        bool was_dirty = m_document_dirty;
        m_document_dirty = true;
        if (!was_dirty)
            update_title();
    };

    m_find_replace_widget = add<GUI::Widget>();
    m_find_replace_widget->set_fill_with_background_color(true);
    m_find_replace_widget->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_find_replace_widget->set_preferred_size(0, 48);
    m_find_replace_widget->set_layout(make<GUI::VerticalBoxLayout>());
    m_find_replace_widget->layout()->set_margins({ 2, 2, 2, 4 });
    m_find_replace_widget->set_visible(false);

    m_find_widget = m_find_replace_widget->add<GUI::Widget>();
    m_find_widget->set_fill_with_background_color(true);
    m_find_widget->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_find_widget->set_preferred_size(0, 22);
    m_find_widget->set_layout(make<GUI::HorizontalBoxLayout>());
    m_find_widget->set_visible(false);

    m_replace_widget = m_find_replace_widget->add<GUI::Widget>();
    m_replace_widget->set_fill_with_background_color(true);
    m_replace_widget->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_replace_widget->set_preferred_size(0, 22);
    m_replace_widget->set_layout(make<GUI::HorizontalBoxLayout>());
    m_replace_widget->set_visible(false);

    m_find_textbox = m_find_widget->add<GUI::TextBox>();
    m_replace_textbox = m_replace_widget->add<GUI::TextBox>();

    m_find_next_action = GUI::Action::create("Find next", { Mod_Ctrl, Key_G }, [&](auto&) {
        auto needle = m_find_textbox->text();
        if (needle.is_empty()) {
            dbg() << "find_next(\"\")";
            return;
        }
        auto found_range = m_editor->document().find_next(needle, m_editor->normalized_selection().end());
        dbg() << "find_next(\"" << needle << "\") returned " << found_range;
        if (found_range.is_valid()) {
            m_editor->set_selection(found_range);
        } else {
            GUI::MessageBox::show(
                String::format("Not found: \"%s\"", needle.characters()),
                "Not found",
                GUI::MessageBox::Type::Information,
                GUI::MessageBox::InputType::OK, window());
        }
    });

    m_find_previous_action = GUI::Action::create("Find previous", { Mod_Ctrl | Mod_Shift, Key_G }, [&](auto&) {
        auto needle = m_find_textbox->text();
        if (needle.is_empty()) {
            dbg() << "find_prev(\"\")";
            return;
        }

        auto selection_start = m_editor->normalized_selection().start();
        if (!selection_start.is_valid())
            selection_start = m_editor->normalized_selection().end();

        auto found_range = m_editor->document().find_previous(needle, selection_start);

        dbg() << "find_prev(\"" << needle << "\") returned " << found_range;
        if (found_range.is_valid()) {
            m_editor->set_selection(found_range);
        } else {
            GUI::MessageBox::show(
                String::format("Not found: \"%s\"", needle.characters()),
                "Not found",
                GUI::MessageBox::Type::Information,
                GUI::MessageBox::InputType::OK, window());
        }
    });

    m_replace_next_action = GUI::Action::create("Replace next", { Mod_Ctrl, Key_F1 }, [&](auto&) {
        auto needle = m_find_textbox->text();
        auto substitute = m_replace_textbox->text();

        if (needle.is_empty())
            return;

        auto selection_start = m_editor->normalized_selection().start();
        if (!selection_start.is_valid())
            selection_start = m_editor->normalized_selection().start();

        auto found_range = m_editor->document().find_next(needle, selection_start);

        if (found_range.is_valid()) {
            m_editor->set_selection(found_range);
            m_editor->insert_at_cursor_or_replace_selection(substitute);
        } else {
            GUI::MessageBox::show(
                String::format("Not found: \"%s\"", needle.characters()),
                "Not found",
                GUI::MessageBox::Type::Information,
                GUI::MessageBox::InputType::OK, window());
        }
    });

    m_replace_previous_action = GUI::Action::create("Replace previous", { Mod_Ctrl | Mod_Shift, Key_F1 }, [&](auto&) {
        auto needle = m_find_textbox->text();
        auto substitute = m_replace_textbox->text();
        if (needle.is_empty())
            return;

        auto selection_start = m_editor->normalized_selection().start();
        if (!selection_start.is_valid())
            selection_start = m_editor->normalized_selection().start();

        auto found_range = m_editor->document().find_previous(needle, selection_start);

        if (found_range.is_valid()) {
            m_editor->set_selection(found_range);
            m_editor->insert_at_cursor_or_replace_selection(substitute);
        } else {
            GUI::MessageBox::show(
                String::format("Not found: \"%s\"", needle.characters()),
                "Not found",
                GUI::MessageBox::Type::Information,
                GUI::MessageBox::InputType::OK, window());
        }
    });

    m_replace_all_action = GUI::Action::create("Replace all", { Mod_Ctrl, Key_F2 }, [&](auto&) {
        auto needle = m_find_textbox->text();
        auto substitute = m_replace_textbox->text();
        if (needle.is_empty())
            return;

        auto found_range = m_editor->document().find_next(needle);
        while (found_range.is_valid()) {
            m_editor->set_selection(found_range);
            m_editor->insert_at_cursor_or_replace_selection(substitute);
            found_range = m_editor->document().find_next(needle);
        }
    });

    m_find_previous_button = m_find_widget->add<GUI::Button>("Find previous");
    m_find_previous_button->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    m_find_previous_button->set_preferred_size(150, 0);
    m_find_previous_button->set_action(*m_find_previous_action);

    m_find_next_button = m_find_widget->add<GUI::Button>("Find next");
    m_find_next_button->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    m_find_next_button->set_preferred_size(150, 0);
    m_find_next_button->set_action(*m_find_next_action);

    m_find_textbox->on_return_pressed = [this] {
        m_find_next_button->click();
    };

    m_find_textbox->on_escape_pressed = [this] {
        m_find_replace_widget->set_visible(false);
        m_editor->set_focus(true);
    };

    m_replace_previous_button = m_replace_widget->add<GUI::Button>("Replace previous");
    m_replace_previous_button->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    m_replace_previous_button->set_preferred_size(100, 0);
    m_replace_previous_button->set_action(*m_replace_previous_action);

    m_replace_next_button = m_replace_widget->add<GUI::Button>("Replace next");
    m_replace_next_button->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    m_replace_next_button->set_preferred_size(100, 0);
    m_replace_next_button->set_action(*m_replace_next_action);

    m_replace_all_button = m_replace_widget->add<GUI::Button>("Replace all");
    m_replace_all_button->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    m_replace_all_button->set_preferred_size(100, 0);
    m_replace_all_button->set_action(*m_replace_all_action);

    m_replace_textbox->on_return_pressed = [this] {
        m_replace_next_button->click();
    };

    m_replace_textbox->on_escape_pressed = [this] {
        m_find_replace_widget->set_visible(false);
        m_editor->set_focus(true);
    };

    m_find_replace_action = GUI::Action::create("Find/Replace...", { Mod_Ctrl, Key_F }, Gfx::Bitmap::load_from_file("/res/icons/16x16/find.png"), [this](auto&) {
        m_find_replace_widget->set_visible(true);
        m_find_widget->set_visible(true);
        m_replace_widget->set_visible(true);
        m_find_textbox->set_focus(true);

        if (m_editor->has_selection()) {
            auto selected_text = m_editor->document().text_in_range(m_editor->normalized_selection());
            m_find_textbox->set_text(selected_text);
        }
        m_find_textbox->select_all();
    });

    m_editor->add_custom_context_menu_action(*m_find_replace_action);
    m_editor->add_custom_context_menu_action(*m_find_next_action);
    m_editor->add_custom_context_menu_action(*m_find_previous_action);

    m_statusbar = add<GUI::StatusBar>();

    m_editor->on_cursor_change = [this] {
        StringBuilder builder;
        builder.appendf("Line: %d, Column: %d", m_editor->cursor().line() + 1, m_editor->cursor().column());
        m_statusbar->set_text(builder.to_string());
    };

    m_new_action = GUI::Action::create("New", { Mod_Ctrl, Key_N }, Gfx::Bitmap::load_from_file("/res/icons/16x16/new.png"), [this](const GUI::Action&) {
        if (m_document_dirty) {
            auto save_document_first_result = GUI::MessageBox::show("Save Document First?", "Warning", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::YesNoCancel);
            if (save_document_first_result == GUI::Dialog::ExecResult::ExecYes)
                m_save_action->activate();
            if (save_document_first_result == GUI::Dialog::ExecResult::ExecCancel)
                return;
        }

        m_document_dirty = false;
        m_editor->set_text(StringView());
        set_path(FileSystemPath());
        update_title();
    });

    m_open_action = GUI::CommonActions::make_open_action([this](auto&) {
        Optional<String> open_path = GUI::FilePicker::get_open_filepath();

        if (!open_path.has_value())
            return;

        if (m_document_dirty) {
            auto save_document_first_result = GUI::MessageBox::show("Save Document First?", "Warning", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::YesNoCancel, window());
            if (save_document_first_result == GUI::Dialog::ExecResult::ExecYes)
                m_save_action->activate();
            if (save_document_first_result == GUI::Dialog::ExecResult::ExecCancel)
                return;
        }

        open_sesame(open_path.value());
    });

    m_save_as_action = GUI::Action::create("Save as...", { Mod_Ctrl | Mod_Shift, Key_S }, Gfx::Bitmap::load_from_file("/res/icons/16x16/save.png"), [this](const GUI::Action&) {
        Optional<String> save_path = GUI::FilePicker::get_save_filepath(m_name.is_null() ? "Untitled" : m_name, m_extension.is_null() ? "txt" : m_extension);
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

    m_line_wrapping_setting_action = GUI::Action::create("Line wrapping", [&](GUI::Action& action) {
        action.set_checked(!action.is_checked());
        m_editor->set_line_wrapping_enabled(action.is_checked());
    });
    m_line_wrapping_setting_action->set_checkable(true);
    m_line_wrapping_setting_action->set_checked(m_editor->is_line_wrapping_enabled());

    auto menubar = make<GUI::MenuBar>();
    auto app_menu = GUI::Menu::construct("Text Editor");
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

    auto edit_menu = GUI::Menu::construct("Edit");
    edit_menu->add_action(m_editor->undo_action());
    edit_menu->add_action(m_editor->redo_action());
    edit_menu->add_separator();
    edit_menu->add_action(m_editor->cut_action());
    edit_menu->add_action(m_editor->copy_action());
    edit_menu->add_action(m_editor->paste_action());
    edit_menu->add_action(m_editor->delete_action());
    edit_menu->add_separator();
    edit_menu->add_action(*m_find_replace_action);
    edit_menu->add_action(*m_find_next_action);
    edit_menu->add_action(*m_find_previous_action);
    edit_menu->add_action(*m_replace_next_action);
    edit_menu->add_action(*m_replace_previous_action);
    edit_menu->add_action(*m_replace_all_action);
    menubar->add_menu(move(edit_menu));

    auto font_menu = GUI::Menu::construct("Font");
    GFontDatabase::the().for_each_fixed_width_font([&](const StringView& font_name) {
        font_menu->add_action(GUI::Action::create(font_name, [this](const GUI::Action& action) {
            m_editor->set_font(GFontDatabase::the().get_by_name(action.text()));
            m_editor->update();
        }));
    });

    auto view_menu = GUI::Menu::construct("View");
    view_menu->add_action(*m_line_wrapping_setting_action);
    view_menu->add_separator();
    view_menu->add_submenu(move(font_menu));
    menubar->add_menu(move(view_menu));

    auto help_menu = GUI::Menu::construct("Help");
    help_menu->add_action(GUI::Action::create("About", [&](const GUI::Action&) {
        GUI::AboutDialog::show("Text Editor", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-texteditor.png"), window());
    }));
    menubar->add_menu(move(help_menu));

    GUI::Application::the().set_menubar(move(menubar));

    toolbar->add_action(*m_new_action);
    toolbar->add_action(*m_open_action);
    toolbar->add_action(*m_save_action);

    toolbar->add_separator();

    toolbar->add_action(m_editor->cut_action());
    toolbar->add_action(m_editor->copy_action());
    toolbar->add_action(m_editor->paste_action());
    toolbar->add_action(m_editor->delete_action());

    toolbar->add_separator();

    toolbar->add_action(m_editor->undo_action());
    toolbar->add_action(m_editor->redo_action());
}

TextEditorWidget::~TextEditorWidget()
{
}

void TextEditorWidget::set_path(const FileSystemPath& file)
{
    m_path = file.string();
    m_name = file.title();
    m_extension = file.extension();

    if (m_extension == "cpp" || m_extension == "h")
        m_editor->set_syntax_highlighter(make<GUI::CppSyntaxHighlighter>());

    update_title();
}

void TextEditorWidget::update_title()
{
    StringBuilder builder;
    builder.append("Text Editor: ");
    builder.append(m_path);
    if (m_document_dirty)
        builder.append(" (*)");
    window()->set_title(builder.to_string());
}

void TextEditorWidget::open_sesame(const String& path)
{
    auto file = Core::File::construct(path);
    if (!file->open(Core::IODevice::ReadOnly)) {
        GUI::MessageBox::show(String::format("Opening \"%s\" failed: %s", path.characters(), strerror(errno)), "Error", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, window());
        return;
    }

    m_editor->set_text(file->read_all());
    m_document_dirty = false;
    m_document_opening = true;

    set_path(FileSystemPath(path));

    m_editor->set_focus(true);
}

bool TextEditorWidget::request_close()
{
    if (!m_document_dirty)
        return true;
    auto result = GUI::MessageBox::show("The document has been modified. Would you like to save?", "Unsaved changes", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::YesNoCancel, window());

    if (result == GUI::MessageBox::ExecYes)
        m_save_action->activate();

    if (result == GUI::MessageBox::ExecNo)
        return true;

    return false;
}

void TextEditorWidget::drop_event(GUI::DropEvent& event)
{
    event.accept();
    window()->move_to_front();

    if (event.mime_data().has_urls()) {
        auto urls = event.mime_data().urls();
        if (urls.is_empty())
            return;
        if (urls.size() > 1) {
            GUI::MessageBox::show("TextEditor can only open one file at a time!", "One at a time please!", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, window());
            return;
        }
        open_sesame(urls.first().path());
    }
}
