#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GFilePicker.h>
#include <LibGUI/GMessageBox.h>
#include <LibGUI/GStatusBar.h>
#include <LibGUI/GToolBar.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GFontDatabase.h>
#include <LibCore/CFile.h>
#include <AK/StringBuilder.h>
#include <unistd.h>
#include <stdio.h>
#include <signal.h>
#include <fcntl.h>

void open_sesame(GWindow& window, GTextEditor& editor, const String& path)
{
    CFile file(path);

    if (!file.open(CIODevice::ReadOnly)) {
        GMessageBox::show(String::format("Opening \"%s\" failed: %s", path.characters(), strerror(errno)), "Error", GMessageBox::Type::Error, &window);
    }

    window.set_title(String::format("Text Editor: %s", path.characters()));
    editor.set_text(String::copy(file.read_all()));
}

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* window = new GWindow;
    window->set_title("Text Editor");
    auto* widget = new GWidget;
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->layout()->set_spacing(0);

    auto* toolbar = new GToolBar(widget);
    auto* text_editor = new GTextEditor(GTextEditor::MultiLine, widget);
    text_editor->set_ruler_visible(true);
    text_editor->set_automatic_indentation_enabled(true);
    auto* statusbar = new GStatusBar(widget);

    text_editor->on_cursor_change = [statusbar, text_editor] {
        StringBuilder builder;
        builder.appendf("Line: %d, Column: %d", text_editor->cursor().line(), text_editor->cursor().column());
        statusbar->set_text(builder.to_string());
    };

    String path = "/tmp/TextEditor.save.txt";
    if (argc >= 2) {
        path = argv[1];
        open_sesame(*window, *text_editor, path);
    }

    auto new_action = GAction::create("New document", { Mod_Ctrl, Key_N }, GraphicsBitmap::load_from_file("/res/icons/16x16/new.png"), [] (const GAction&) {
        dbgprintf("FIXME: Implement File/New\n");
    });

    auto open_action = GAction::create("Open document", { Mod_Ctrl, Key_O }, GraphicsBitmap::load_from_file("/res/icons/16x16/open.png"), [window, text_editor, &path](const GAction&) {
        GFilePicker picker;
        if (picker.exec() == GDialog::ExecOK) {
            path = picker.selected_file().string();
            open_sesame(*window, *text_editor, path);
        }
    });

    auto save_action = GAction::create("Save document", { Mod_Ctrl, Key_S }, GraphicsBitmap::load_from_file("/res/icons/16x16/save.png"), [&] (const GAction&) {
        dbgprintf("Writing document to '%s'\n", path.characters());
        text_editor->write_to_file(path);
    });

    auto menubar = make<GMenuBar>();
    auto app_menu = make<GMenu>("Text Editor");
    app_menu->add_action(GAction::create("Quit", { Mod_Alt, Key_F4 }, [] (const GAction&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto file_menu = make<GMenu>("File");
    file_menu->add_action(new_action.copy_ref());
    file_menu->add_action(open_action.copy_ref());
    file_menu->add_action(save_action.copy_ref());
    menubar->add_menu(move(file_menu));

    auto edit_menu = make<GMenu>("Edit");
    edit_menu->add_action(text_editor->undo_action());
    edit_menu->add_action(text_editor->redo_action());
    edit_menu->add_separator();
    edit_menu->add_action(text_editor->cut_action());
    edit_menu->add_action(text_editor->copy_action());
    edit_menu->add_action(text_editor->paste_action());
    edit_menu->add_action(text_editor->delete_action());
    menubar->add_menu(move(edit_menu));

    auto font_menu = make<GMenu>("Font");
    GFontDatabase::the().for_each_fixed_width_font([&] (const String& font_name) {
        font_menu->add_action(GAction::create(font_name, [text_editor] (const GAction& action) {
            text_editor->set_font(GFontDatabase::the().get_by_name(action.text()));
            text_editor->update();
        }));
    });
    menubar->add_menu(move(font_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [] (const GAction&) {
        dbgprintf("FIXME: Implement Help/About\n");
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    toolbar->add_action(move(new_action));
    toolbar->add_action(move(open_action));
    toolbar->add_action(move(save_action));

    toolbar->add_separator();

    toolbar->add_action(text_editor->cut_action());
    toolbar->add_action(text_editor->copy_action());
    toolbar->add_action(text_editor->paste_action());
    toolbar->add_action(text_editor->delete_action());

    toolbar->add_separator();

    toolbar->add_action(text_editor->undo_action());
    toolbar->add_action(text_editor->redo_action());

    window->set_rect(20, 200, 640, 400);
    window->set_main_widget(widget);
    window->set_should_exit_event_loop_on_close(true);
    text_editor->set_focus(true);
    window->show();

    window->set_icon_path("/res/icons/TextEditor16.png");

    return app.exec();
}
