#include "VBForm.h"
#include "VBPropertiesWindow.h"
#include "VBWidget.h"
#include "VBWidgetPropertyModel.h"
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GTableView.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <fcntl.h>
#include <signal.h>
#include <stdio.h>
#include <unistd.h>

static ObjectPtr<GWindow> make_toolbox_window();

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* propbox = new VBPropertiesWindow;

    auto* form1 = new VBForm("Form1");
    form1->on_widget_selected = [propbox](VBWidget* widget) {
        propbox->table_view().set_model(widget ? &widget->property_model() : nullptr);
    };

    auto menubar = make<GMenuBar>();
    auto app_menu = make<GMenu>("Visual Builder");
    app_menu->add_action(GCommonActions::make_quit_action([](auto&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto file_menu = make<GMenu>("File");
    file_menu->add_action(GAction::create("Dump Form", [&](auto&) {
        form1->dump();
    }));
    file_menu->add_action(GAction::create("Save Form...", { Mod_Ctrl, Key_S }, [form1](auto&) {
        form1->write_to_file("/tmp/form.frm");
    }));
    menubar->add_menu(move(file_menu));

    auto window = GWindow::construct();
    window->set_title(form1->name());
    window->set_rect(120, 200, 640, 400);
    window->set_main_widget(form1);

    window->show();

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [&](const GAction&) {
        GAboutDialog::show("Visual Builder", load_png("/res/icons/32x32/app-visual-builder.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    auto toolbox = make_toolbox_window();
    toolbox->show();

    propbox->show();

    if (argc == 2) {
        form1->load_from_file(argv[1]);
    }

    return app.exec();
}

ObjectPtr<GWindow> make_toolbox_window()
{
    auto window = GWindow::construct();
    window->set_title("Widgets");
    window->set_rect(20, 200, 80, 300);

    auto widget = GWidget::construct();
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->layout()->set_spacing(0);
    window->set_main_widget(widget);

    auto* label_button = new GButton(widget);
    label_button->set_button_style(ButtonStyle::CoolBar);
    label_button->set_tooltip("GLabel");
    label_button->set_icon(GraphicsBitmap::load_from_file("/res/icons/vbwidgets/label.png"));
    label_button->on_click = [](GButton&) {
        if (auto* form = VBForm::current())
            form->insert_widget(VBWidgetType::GLabel);
    };

    auto* button_button = new GButton(widget);
    button_button->set_button_style(ButtonStyle::CoolBar);
    button_button->set_tooltip("GButton");
    button_button->set_icon(GraphicsBitmap::load_from_file("/res/icons/vbwidgets/button.png"));
    button_button->on_click = [](GButton&) {
        if (auto* form = VBForm::current())
            form->insert_widget(VBWidgetType::GButton);
    };
    auto* spinbox_button = new GButton(widget);
    spinbox_button->set_button_style(ButtonStyle::CoolBar);
    spinbox_button->set_tooltip("GSpinBox");
    spinbox_button->set_icon(GraphicsBitmap::load_from_file("/res/icons/vbwidgets/spinbox.png"));
    spinbox_button->on_click = [](GButton&) {
        if (auto* form = VBForm::current())
            form->insert_widget(VBWidgetType::GSpinBox);
    };
    auto* editor_button = new GButton(widget);
    editor_button->set_button_style(ButtonStyle::CoolBar);
    editor_button->set_tooltip("GTextEditor");
    editor_button->set_icon(GraphicsBitmap::load_from_file("/res/icons/vbwidgets/textbox.png"));
    editor_button->on_click = [](GButton&) {
        if (auto* form = VBForm::current())
            form->insert_widget(VBWidgetType::GTextEditor);
    };
    auto* progress_bar_button = new GButton(widget);
    progress_bar_button->set_button_style(ButtonStyle::CoolBar);
    progress_bar_button->set_tooltip("GProgressBar");
    progress_bar_button->set_icon(GraphicsBitmap::load_from_file("/res/icons/vbwidgets/progressbar.png"));
    progress_bar_button->on_click = [](GButton&) {
        if (auto* form = VBForm::current())
            form->insert_widget(VBWidgetType::GProgressBar);
    };
    auto* slider_button = new GButton(widget);
    slider_button->set_button_style(ButtonStyle::CoolBar);
    slider_button->set_tooltip("GSlider");
    slider_button->set_icon(GraphicsBitmap::load_from_file("/res/icons/vbwidgets/slider.png"));
    slider_button->on_click = [](GButton&) {
        if (auto* form = VBForm::current())
            form->insert_widget(VBWidgetType::GSlider);
    };
    auto* checkbox_button = new GButton(widget);
    checkbox_button->set_button_style(ButtonStyle::CoolBar);
    checkbox_button->set_tooltip("GCheckBox");
    checkbox_button->set_icon(GraphicsBitmap::load_from_file("/res/icons/vbwidgets/checkbox.png"));
    checkbox_button->on_click = [](GButton&) {
        if (auto* form = VBForm::current())
            form->insert_widget(VBWidgetType::GCheckBox);
    };
    auto* radiobutton_button = new GButton(widget);
    radiobutton_button->set_button_style(ButtonStyle::CoolBar);
    radiobutton_button->set_tooltip("GRadioButton");
    radiobutton_button->set_icon(GraphicsBitmap::load_from_file("/res/icons/filled-radio-circle.png"));
    radiobutton_button->on_click = [](GButton&) {
        if (auto* form = VBForm::current())
            form->insert_widget(VBWidgetType::GRadioButton);
    };
    auto* scrollbar_button = new GButton(widget);
    scrollbar_button->set_button_style(ButtonStyle::CoolBar);
    scrollbar_button->set_tooltip("GScrollBar");
    scrollbar_button->set_icon(GraphicsBitmap::load_from_file("/res/icons/vbwidgets/scrollbar.png"));
    scrollbar_button->on_click = [](GButton&) {
        if (auto* form = VBForm::current())
            form->insert_widget(VBWidgetType::GScrollBar);
    };
    auto* groupbox_button = new GButton(widget);
    groupbox_button->set_button_style(ButtonStyle::CoolBar);
    groupbox_button->set_tooltip("GGroupBox");
    groupbox_button->set_icon(GraphicsBitmap::load_from_file("/res/icons/vbwidgets/groupbox.png"));
    groupbox_button->on_click = [](GButton&) {
        if (auto* form = VBForm::current())
            form->insert_widget(VBWidgetType::GGroupBox);
    };
    return window;
}
