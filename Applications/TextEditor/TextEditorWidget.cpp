#include "TextEditorWidget.h"
#include <AK/Optional.h>
#include <AK/StringBuilder.h>
#include <LibCore/CFile.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GFilePicker.h>
#include <LibGUI/GFontDatabase.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GMessageBox.h>
#include <LibGUI/GStatusBar.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GToolBar.h>

TextEditorWidget::TextEditorWidget()
{
    set_layout(make<GBoxLayout>(Orientation::Vertical));
    layout()->set_spacing(0);

    auto* toolbar = new GToolBar(this);
    m_editor = new GTextEditor(GTextEditor::MultiLine, this);
    m_editor->set_ruler_visible(true);
    m_editor->set_automatic_indentation_enabled(true);
    auto* statusbar = new GStatusBar(this);

    m_editor->on_cursor_change = [statusbar, this] {
        StringBuilder builder;
        builder.appendf("Line: %d, Column: %d", m_editor->cursor().line() + 1, m_editor->cursor().column());
        statusbar->set_text(builder.to_string());
    };

    m_new_action = GAction::create("New", { Mod_Ctrl, Key_N }, GraphicsBitmap::load_from_file("/res/icons/16x16/new.png"), [](const GAction&) {
        dbgprintf("FIXME: Implement File/New\n");
    });

    m_open_action = GAction::create("Open...", { Mod_Ctrl, Key_O }, GraphicsBitmap::load_from_file("/res/icons/16x16/open.png"), [this](const GAction&) {
        Optional<String> open_path = GFilePicker::get_open_filepath();

        if (!open_path.has_value())
            return;

        open_sesame(open_path.value());
    });

    m_save_as_action = GAction::create("Save as...", { Mod_None, Key_F12 }, GraphicsBitmap::load_from_file("/res/icons/16x16/save.png"), [this](const GAction&) {
        Optional<String> save_path = GFilePicker::get_save_filepath(m_name.is_null() ? "Untitled" : m_name, m_extension.is_null() ? "txt" : m_extension);
        if (!save_path.has_value())
            return;

        if (!m_editor->write_to_file(save_path.value())) {
            GMessageBox::show("Unable to save file.\n", "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, window());
            return;
        }

        set_path(FileSystemPath(save_path.value()));
        dbg() << "Wrote document to " << save_path.value();
    });

    m_save_action = GAction::create("Save", { Mod_Ctrl, Key_S }, GraphicsBitmap::load_from_file("/res/icons/16x16/save.png"), [&](const GAction&) {
        if (!m_path.is_empty()) {
            if (!m_editor->write_to_file(m_path))
                GMessageBox::show("Unable to save file.\n", "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, window());
            return;
        }

        m_save_as_action->activate();
    });

    auto menubar = make<GMenuBar>();
    auto app_menu = make<GMenu>("Text Editor");
    app_menu->add_action(GAction::create("Quit", { Mod_Alt, Key_F4 }, [](const GAction&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto file_menu = make<GMenu>("File");
    file_menu->add_action(*m_new_action);
    file_menu->add_action(*m_open_action);
    file_menu->add_action(*m_save_action);
    file_menu->add_action(*m_save_as_action);
    menubar->add_menu(move(file_menu));

    auto edit_menu = make<GMenu>("Edit");
    edit_menu->add_action(m_editor->undo_action());
    edit_menu->add_action(m_editor->redo_action());
    edit_menu->add_separator();
    edit_menu->add_action(m_editor->cut_action());
    edit_menu->add_action(m_editor->copy_action());
    edit_menu->add_action(m_editor->paste_action());
    edit_menu->add_action(m_editor->delete_action());
    menubar->add_menu(move(edit_menu));

    auto font_menu = make<GMenu>("Font");
    GFontDatabase::the().for_each_fixed_width_font([&](const StringView& font_name) {
        font_menu->add_action(GAction::create(font_name, [this](const GAction& action) {
            m_editor->set_font(GFontDatabase::the().get_by_name(action.text()));
            m_editor->update();
        }));
    });
    menubar->add_menu(move(font_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [](const GAction&) {
        dbgprintf("FIXME: Implement Help/About\n");
    }));
    menubar->add_menu(move(help_menu));

    GApplication::the().set_menubar(move(menubar));

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

    m_editor->set_focus(true);
}

TextEditorWidget::~TextEditorWidget()
{
}

void TextEditorWidget::set_path(const FileSystemPath& file)
{
    m_path = file.string();
    m_name = file.title();
    m_extension = file.extension();
    StringBuilder builder;
    builder.append("Text Editor: ");
    builder.append(file.string());
    window()->set_title(builder.to_string());
}

void TextEditorWidget::open_sesame(const String& path)
{
    dbgprintf("Our path to file in open_sesame: %s\n", path.characters());
    CFile file(path);

    if (!file.open(CIODevice::ReadOnly)) {
        GMessageBox::show(String::format("Opening \"%s\" failed: %s", path.characters(), strerror(errno)), "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, window());
    }

    m_editor->set_text(String::copy(file.read_all()));
    set_path(FileSystemPath(path));
}
