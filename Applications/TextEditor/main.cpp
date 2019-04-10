#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GStatusBar.h>
#include <LibGUI/GToolBar.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GFontDatabase.h>
#include <LibGUI/GFile.h>
#include <AK/StringBuilder.h>
#include <unistd.h>
#include <stdio.h>
#include <signal.h>
#include <fcntl.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* widget = new GWidget;
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));

    auto* toolbar = new GToolBar(widget);
    auto* text_editor = new GTextEditor(GTextEditor::MultiLine, widget);
    auto* statusbar = new GStatusBar(widget);

    text_editor->on_cursor_change = [statusbar, text_editor] {
        StringBuilder builder;
        builder.appendf("Line: %d, Column: %d", text_editor->cursor().line(), text_editor->cursor().column());
        statusbar->set_text(builder.to_string());
    };

    String path = "/tmp/TextEditor.save.txt";
    if (argc >= 2) {
        path = argv[1];
        GFile file(path);

        if (!file.open(GIODevice::ReadOnly)) {
            fprintf(stderr, "Opening %s: %s\n", path.characters(), file.error_string());
            return 1;
        }
        text_editor->set_text(String::from_byte_buffer(file.read_all()));
    }

    auto new_action = GAction::create("New document", { Mod_Ctrl, Key_N }, GraphicsBitmap::load_from_file("/res/icons/16x16/new.png"), [] (const GAction&) {
        dbgprintf("FIXME: Implement File/New\n");
    });

    auto open_action = GAction::create("Open document", { Mod_Ctrl, Key_O }, GraphicsBitmap::load_from_file("/res/icons/16x16/open.png"), [] (const GAction&) {
        dbgprintf("FIXME: Implement File/Open\n");
    });

    auto save_action = GAction::create("Save document", { Mod_Ctrl, Key_S }, GraphicsBitmap::load_from_file("/res/icons/16x16/save.png"), [&] (const GAction&) {
        dbgprintf("Writing document to '%s'\n", path.characters());
        text_editor->write_to_file(path);
    });

    auto undo_action = GAction::create("Undo", { Mod_Ctrl, Key_Z }, GraphicsBitmap::load_from_file("/res/icons/16x16/undo.png"), [&] (const GAction&) {
        // FIXME: Undo
    });

    auto redo_action = GAction::create("Redo", { Mod_Ctrl, Key_Y }, GraphicsBitmap::load_from_file("/res/icons/16x16/redo.png"), [&] (const GAction&) {
        // FIXME: Redo
    });

    auto cut_action = GAction::create("Cut", { Mod_Ctrl, Key_X }, GraphicsBitmap::load_from_file("/res/icons/cut16.png"), [&] (const GAction&) {
        text_editor->cut();
    });

    auto copy_action = GAction::create("Copy", { Mod_Ctrl, Key_C }, GraphicsBitmap::load_from_file("/res/icons/16x16/edit-copy.png"), [&] (const GAction&) {
        text_editor->copy();
    });

    auto paste_action = GAction::create("Paste", { Mod_Ctrl, Key_V }, GraphicsBitmap::load_from_file("/res/icons/paste16.png"), [&] (const GAction&) {
        text_editor->paste();
    });

    auto delete_action = GAction::create("Delete", { 0, Key_Delete }, GraphicsBitmap::load_from_file("/res/icons/16x16/delete.png"), [&] (const GAction&) {
        text_editor->do_delete();
    });

    auto menubar = make<GMenuBar>();
    auto app_menu = make<GMenu>("TextEditor");
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
    edit_menu->add_action(undo_action.copy_ref());
    edit_menu->add_action(redo_action.copy_ref());
    edit_menu->add_separator();
    edit_menu->add_action(cut_action.copy_ref());
    edit_menu->add_action(copy_action.copy_ref());
    edit_menu->add_action(paste_action.copy_ref());
    edit_menu->add_action(delete_action.copy_ref());
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

    toolbar->add_action(move(cut_action));
    toolbar->add_action(move(copy_action));
    toolbar->add_action(move(paste_action));
    toolbar->add_action(move(delete_action));

    toolbar->add_separator();

    toolbar->add_action(move(undo_action));
    toolbar->add_action(move(redo_action));

    auto* window = new GWindow;
    window->set_title(String::format("TextEditor: %s", path.characters()));
    window->set_rect(20, 200, 640, 400);
    window->set_main_widget(widget);
    window->set_should_exit_event_loop_on_close(true);
    text_editor->set_focus(true);
    window->show();

    return app.exec();
}
