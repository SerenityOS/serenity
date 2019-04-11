#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GButton.h>
#include "VBForm.h"
#include "VBWidget.h"
#include <unistd.h>
#include <stdio.h>
#include <signal.h>
#include <fcntl.h>

static GWindow* make_toolbox_window();

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* form1 = new VBForm("Form1");

    auto menubar = make<GMenuBar>();
    auto app_menu = make<GMenu>("Visual Builder");
    app_menu->add_action(GAction::create("Quit", { Mod_Alt, Key_F4 }, [] (const GAction&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto file_menu = make<GMenu>("File");
    menubar->add_menu(move(file_menu));

    auto edit_menu = make<GMenu>("Edit");
    menubar->add_menu(move(edit_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [] (const GAction&) {
        dbgprintf("FIXME: Implement Help/About\n");
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    auto* window = new GWindow;
    window->set_title(form1->name());
    window->set_rect(120, 200, 640, 400);
    window->set_main_widget(form1);
    window->set_should_exit_event_loop_on_close(true);
    window->show();

    auto* toolbox = make_toolbox_window();
    toolbox->show();

    return app.exec();
}

GWindow* make_toolbox_window()
{
    auto* window = new GWindow;
    window->set_title("Widgets");
    window->set_rect(20, 200, 80, 300);

    auto* widget = new GWidget;
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    window->set_main_widget(widget);

    auto* button_button = new GButton(widget);
    button_button->set_tooltip("GButton");
    button_button->set_icon(GraphicsBitmap::load_from_file("/res/icons/vbwidgets/button.png"));
    button_button->on_click = [] (GButton&) {
        if (auto* form = VBForm::current())
            form->insert_widget(WidgetType::GButton);
    };
    auto* spinbox_button = new GButton(widget);
    spinbox_button->set_tooltip("GSpinBox");
    spinbox_button->set_icon(GraphicsBitmap::load_from_file("/res/icons/vbwidgets/spinbox.png"));
    spinbox_button->on_click = [] (GButton&) {
        if (auto* form = VBForm::current())
            form->insert_widget(WidgetType::GSpinBox);
    };
    auto* editor_button = new GButton(widget);
    editor_button->set_tooltip("GTextEditor");
    editor_button->set_icon(GraphicsBitmap::load_from_file("/res/icons/vbwidgets/textbox.png"));
    editor_button->on_click = [] (GButton&) {
        if (auto* form = VBForm::current())
            form->insert_widget(WidgetType::GTextEditor);
    };
    auto* progress_bar_button = new GButton(widget);
    progress_bar_button->set_tooltip("GProgressBar");
    progress_bar_button->set_icon(GraphicsBitmap::load_from_file("/res/icons/vbwidgets/progressbar.png"));
    progress_bar_button->on_click = [] (GButton&) {
        if (auto* form = VBForm::current())
            form->insert_widget(WidgetType::GProgressBar);
    };
    return window;
}
