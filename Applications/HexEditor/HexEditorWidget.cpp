#include "HexEditorWidget.h"
#include <AK/Optional.h>
#include <AK/StringBuilder.h>
#include <LibCore/CFile.h>
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GFilePicker.h>
#include <LibGUI/GFontDatabase.h>
#include <LibGUI/GInputBox.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GMessageBox.h>
#include <LibGUI/GStatusBar.h>
#include <LibGUI/GTextBox.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GToolBar.h>
#include <stdio.h>

HexEditorWidget::HexEditorWidget()
{
    set_layout(make<GBoxLayout>(Orientation::Vertical));
    layout()->set_spacing(0);

    m_editor = HexEditor::construct(this);

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

    m_statusbar = GStatusBar::construct(5, this);

    m_new_action = GAction::create("New", { Mod_Ctrl, Key_N }, GraphicsBitmap::load_from_file("/res/icons/16x16/new.png"), [this](const GAction&) {
        if (m_document_dirty) {
            auto save_document_first_box = GMessageBox::construct("Save Document First?", "Warning", GMessageBox::Type::Warning, GMessageBox::InputType::OKCancel, window());
            auto save_document_first_result = save_document_first_box->exec();

            if (save_document_first_result != GDialog::ExecResult::ExecOK)
                return;
            m_save_action->activate();
        }

        auto input_box = GInputBox::construct("Enter new file size:", "New file size", this);
        if (input_box->exec() == GInputBox::ExecOK && !input_box->text_value().is_empty()) {
            auto valid = false;
            auto file_size = input_box->text_value().to_int(valid);
            if (valid && file_size > 0) {
                m_document_dirty = false;
                m_editor->set_buffer(ByteBuffer::create_zeroed(file_size));
                set_path(FileSystemPath());
                update_title();
            } else {
                GMessageBox::show("Invalid file size entered.", "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, window());
            }
        }
    });

    m_open_action = GCommonActions::make_open_action([this](auto&) {
        Optional<String> open_path = GFilePicker::get_open_filepath();

        if (!open_path.has_value())
            return;

        open_file(open_path.value());
    });

    m_save_action = GAction::create("Save", { Mod_Ctrl, Key_S }, GraphicsBitmap::load_from_file("/res/icons/16x16/save.png"), [&](const GAction&) {
        if (!m_path.is_empty()) {
            if (!m_editor->write_to_file(m_path)) {
                GMessageBox::show("Unable to save file.\n", "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, window());
            } else {
                m_document_dirty = false;
                update_title();
            }
            return;
        }

        m_save_as_action->activate();
    });

    m_save_as_action = GAction::create("Save as...", { Mod_None, Key_F12 }, GraphicsBitmap::load_from_file("/res/icons/16x16/save.png"), [this](const GAction&) {
        Optional<String> save_path = GFilePicker::get_save_filepath(m_name.is_null() ? "Untitled" : m_name, m_extension.is_null() ? "bin" : m_extension);
        if (!save_path.has_value())
            return;

        if (!m_editor->write_to_file(save_path.value())) {
            GMessageBox::show("Unable to save file.\n", "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, window());
            return;
        }

        m_document_dirty = false;
        set_path(FileSystemPath(save_path.value()));
        dbg() << "Wrote document to " << save_path.value();
    });

    auto menubar = make<GMenuBar>();
    auto app_menu = GMenu::construct("Hex Editor");
    app_menu->add_action(*m_new_action);
    app_menu->add_action(*m_open_action);
    app_menu->add_action(*m_save_action);
    app_menu->add_action(*m_save_as_action);
    app_menu->add_separator();
    app_menu->add_action(GCommonActions::make_quit_action([this](auto&) {
        if (!request_close())
            return;
        GApplication::the().quit(0);
    }));
    menubar->add_menu(move(app_menu));

    auto bytes_per_row_menu = GMenu::construct("Bytes Per Row");
    for (int i = 8; i <= 32; i += 8) {
        bytes_per_row_menu->add_action(GAction::create(String::number(i), [this, i](auto&) {
            m_editor->set_bytes_per_row(i);
            m_editor->update();
        }));
    }

    m_goto_decimal_offset_action = GAction::create("Go To Offset (Decimal)...", GraphicsBitmap::load_from_file("/res/icons/16x16/go-forward.png"), [this](const GAction&) {
        auto input_box = GInputBox::construct("Enter Decimal offset:", "Go To", this);
        if (input_box->exec() == GInputBox::ExecOK && !input_box->text_value().is_empty()) {
            auto valid = false;
            auto new_offset = input_box->text_value().to_int(valid);
            if (valid) {
                m_editor->set_position(new_offset);
            }
        }
    });

    m_goto_hex_offset_action = GAction::create("Go To Offset (Hex)...", GraphicsBitmap::load_from_file("/res/icons/16x16/go-forward.png"), [this](const GAction&) {
        auto input_box = GInputBox::construct("Enter Hex offset:", "Go To", this);
        if (input_box->exec() == GInputBox::ExecOK && !input_box->text_value().is_empty()) {
            auto new_offset = strtol(input_box->text_value().characters(), nullptr, 16);
            m_editor->set_position(new_offset);
        }
    });

    auto edit_menu = GMenu::construct("Edit");
    edit_menu->add_action(GAction::create("Fill selection...", [&](const GAction&) {
        auto input_box = GInputBox::construct("Fill byte (hex):", "Fill Selection", this);
        if (input_box->exec() == GInputBox::ExecOK && !input_box->text_value().is_empty()) {
            auto fill_byte = strtol(input_box->text_value().characters(), nullptr, 16);
            m_editor->fill_selection(fill_byte);
        }
    }));
    edit_menu->add_separator();
    edit_menu->add_action(*m_goto_decimal_offset_action);
    edit_menu->add_action(*m_goto_hex_offset_action);
    edit_menu->add_separator();
    edit_menu->add_action(GAction::create("Copy Hex", [&](const GAction&) {
        m_editor->copy_selected_hex_to_clipboard();
    }));
    edit_menu->add_action(GAction::create("Copy Text", [&](const GAction&) {
        m_editor->copy_selected_text_to_clipboard();
    }));
    edit_menu->add_separator();
    edit_menu->add_action(GAction::create("Copy As C Code", [&](const GAction&) {
        m_editor->copy_selected_hex_to_clipboard_as_c_code();
    }));
    menubar->add_menu(move(edit_menu));

    auto view_menu = GMenu::construct("View");
    view_menu->add_submenu(move(bytes_per_row_menu));
    menubar->add_menu(move(view_menu));

    auto help_menu = GMenu::construct("Help");
    help_menu->add_action(GAction::create("About", [&](const GAction&) {
        GAboutDialog::show("Hex Editor", load_png("/res/icons/32x32/app-hexeditor.png"), window());
    }));
    menubar->add_menu(move(help_menu));

    GApplication::the().set_menubar(move(menubar));

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
    auto file = CFile::construct(path);
    if (!file->open(CIODevice::ReadOnly)) {
        GMessageBox::show(String::format("Opening \"%s\" failed: %s", path.characters(), strerror(errno)), "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, window());
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
    auto result = GMessageBox::show("The file has been modified. Quit without saving?", "Quit without saving?", GMessageBox::Type::Warning, GMessageBox::InputType::OKCancel, window());
    return result == GMessageBox::ExecOK;
}
