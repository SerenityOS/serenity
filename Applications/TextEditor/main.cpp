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
#include <AK/StringBuilder.h>
#include <unistd.h>
#include <stdio.h>
#include <signal.h>
#include <fcntl.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* widget = new GWidget;
    widget->set_fill_with_background_color(false);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));

    auto* toolbar = new GToolBar(widget);
    auto* text_editor = new GTextEditor(widget);
    auto* statusbar = new GStatusBar(widget);

    text_editor->on_cursor_change = [statusbar] (GTextEditor& editor) {
        statusbar->set_text(String::format("Line: %d, Column: %d", editor.cursor().line(), editor.cursor().column()));
    };

    String path = "/tmp/TextEditor.save.txt";
    if (argc >= 2) {
        path = argv[1];
        StringBuilder builder;
        int fd = open(path.characters(), O_RDONLY);
        if (fd < 0) {
            perror("open");
            return 1;
        }
        for (;;) {
            char buffer[BUFSIZ];
            ssize_t nread = read(fd, buffer, sizeof(buffer));
            if (nread < 0) {
                perror("read");
                return 1;
            }
            if (nread == 0)
                break;
            builder.append(buffer, nread);
        }

        int rc = close(fd);
        if (rc < 0) {
            perror("close");
            return 1;
        }

        text_editor->set_text(builder.to_string());
    }

    auto new_action = GAction::create("New document", { Mod_Ctrl, Key_N }, GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/newdocument16.rgb", { 16, 16 }), [] (const GAction&) {
        dbgprintf("FIXME: Implement File/New");
    });

    auto open_action = GAction::create("Open document", { Mod_Ctrl, Key_O }, GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/open16.rgb", { 16, 16 }), [] (const GAction&) {
        dbgprintf("FIXME: Implement File/Open");
    });

    auto save_action = GAction::create("Save document", { Mod_Ctrl, Key_S }, GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/save16.rgb", { 16, 16 }), [&] (const GAction&) {
        dbgprintf("Writing document to '%s'\n", path.characters());
        text_editor->write_to_file(path);
    });

    auto cut_action = GAction::create("Cut", { Mod_Ctrl, Key_X }, GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/cut16.rgb", { 16, 16 }), [&] (const GAction&) {
        dbgprintf("FIXME: Implement Edit/Cut");
    });

    auto copy_action = GAction::create("Copy", { Mod_Ctrl, Key_C }, GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/copyfile16.rgb", { 16, 16 }), [&] (const GAction&) {
        dbgprintf("FIXME: Implement Edit/Copy");
    });

    auto paste_action = GAction::create("Paste", { Mod_Ctrl, Key_V }, GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/paste16.rgb", { 16, 16 }), [&] (const GAction&) {
        dbgprintf("FIXME: Implement Edit/Paste");
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
    edit_menu->add_action(cut_action.copy_ref());
    edit_menu->add_action(copy_action.copy_ref());
    edit_menu->add_action(paste_action.copy_ref());
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

    auto* window = new GWindow;
    window->set_title(String::format("TextEditor: %s", path.characters()));
    window->set_rect(20, 200, 640, 400);
    window->set_main_widget(widget);
    window->set_should_exit_app_on_close(true);
    text_editor->set_focus(true);
    window->show();

    return app.exec();
}
