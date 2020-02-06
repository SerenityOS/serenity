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

#include "VBForm.h"
#include "VBPropertiesWindow.h"
#include "VBWidget.h"
#include "VBWidgetPropertyModel.h"
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

static RefPtr<GUI::Window> make_toolbox_window();

int main(int argc, char** argv)
{
    GUI::Application app(argc, argv);

    auto propbox = VBPropertiesWindow::construct();

    auto form1 = VBForm::construct("Form1");
    form1->on_widget_selected = [&](VBWidget* widget) {
        propbox->table_view().set_model(widget ? &widget->property_model() : nullptr);
    };

    auto menubar = make<GUI::MenuBar>();
    auto app_menu = GUI::Menu::construct("Visual Builder");
    app_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto file_menu = GUI::Menu::construct("File");
    file_menu->add_action(GUI::Action::create("Dump Form", [&](auto&) {
        form1->dump();
    }));
    file_menu->add_action(GUI::Action::create("Save Form...", { Mod_Ctrl, Key_S }, [&](auto&) {
        form1->write_to_file("/tmp/form.frm");
    }));
    menubar->add_menu(move(file_menu));

    auto window = GUI::Window::construct();
    window->set_title(form1->name());
    window->set_rect(120, 200, 640, 400);
    window->set_main_widget(form1);

    window->show();

    auto help_menu = GUI::Menu::construct("Help");
    help_menu->add_action(GUI::Action::create("About", [&](const GUI::Action&) {
        GUI::AboutDialog::show("Visual Builder", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-visual-builder.png"), window);
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

RefPtr<GUI::Window> make_toolbox_window()
{
    auto window = GUI::Window::construct();
    window->set_title("Widgets");
    window->set_rect(20, 200, 80, 300);

    auto widget = GUI::Widget::construct();
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GUI::VBoxLayout>());
    widget->layout()->set_spacing(0);
    window->set_main_widget(widget);

    auto label_button = GUI::Button::construct(widget);
    label_button->set_button_style(Gfx::ButtonStyle::CoolBar);
    label_button->set_tooltip("GLabel");
    label_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/vbwidgets/label.png"));
    label_button->on_click = [](GUI::Button&) {
        if (auto* form = VBForm::current())
            form->insert_widget(VBWidgetType::GLabel);
    };

    auto button_button = GUI::Button::construct(widget);
    button_button->set_button_style(Gfx::ButtonStyle::CoolBar);
    button_button->set_tooltip("GButton");
    button_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/vbwidgets/button.png"));
    button_button->on_click = [](GUI::Button&) {
        if (auto* form = VBForm::current())
            form->insert_widget(VBWidgetType::GButton);
    };
    auto spinbox_button = GUI::Button::construct(widget);
    spinbox_button->set_button_style(Gfx::ButtonStyle::CoolBar);
    spinbox_button->set_tooltip("GSpinBox");
    spinbox_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/vbwidgets/spinbox.png"));
    spinbox_button->on_click = [](GUI::Button&) {
        if (auto* form = VBForm::current())
            form->insert_widget(VBWidgetType::GSpinBox);
    };
    auto editor_button = GUI::Button::construct(widget);
    editor_button->set_button_style(Gfx::ButtonStyle::CoolBar);
    editor_button->set_tooltip("GTextEditor");
    editor_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/vbwidgets/textbox.png"));
    editor_button->on_click = [](GUI::Button&) {
        if (auto* form = VBForm::current())
            form->insert_widget(VBWidgetType::GTextEditor);
    };
    auto progress_bar_button = GUI::Button::construct(widget);
    progress_bar_button->set_button_style(Gfx::ButtonStyle::CoolBar);
    progress_bar_button->set_tooltip("GProgressBar");
    progress_bar_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/vbwidgets/progressbar.png"));
    progress_bar_button->on_click = [](GUI::Button&) {
        if (auto* form = VBForm::current())
            form->insert_widget(VBWidgetType::GProgressBar);
    };
    auto slider_button = GUI::Button::construct(widget);
    slider_button->set_button_style(Gfx::ButtonStyle::CoolBar);
    slider_button->set_tooltip("GSlider");
    slider_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/vbwidgets/slider.png"));
    slider_button->on_click = [](GUI::Button&) {
        if (auto* form = VBForm::current())
            form->insert_widget(VBWidgetType::GSlider);
    };
    auto checkbox_button = GUI::Button::construct(widget);
    checkbox_button->set_button_style(Gfx::ButtonStyle::CoolBar);
    checkbox_button->set_tooltip("GCheckBox");
    checkbox_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/vbwidgets/checkbox.png"));
    checkbox_button->on_click = [](GUI::Button&) {
        if (auto* form = VBForm::current())
            form->insert_widget(VBWidgetType::GCheckBox);
    };
    auto radiobutton_button = GUI::Button::construct(widget);
    radiobutton_button->set_button_style(Gfx::ButtonStyle::CoolBar);
    radiobutton_button->set_tooltip("GRadioButton");
    radiobutton_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/filled-radio-circle.png"));
    radiobutton_button->on_click = [](GUI::Button&) {
        if (auto* form = VBForm::current())
            form->insert_widget(VBWidgetType::GRadioButton);
    };
    auto scrollbar_button = GUI::Button::construct(widget);
    scrollbar_button->set_button_style(Gfx::ButtonStyle::CoolBar);
    scrollbar_button->set_tooltip("GScrollBar");
    scrollbar_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/vbwidgets/scrollbar.png"));
    scrollbar_button->on_click = [](GUI::Button&) {
        if (auto* form = VBForm::current())
            form->insert_widget(VBWidgetType::GScrollBar);
    };
    auto groupbox_button = GUI::Button::construct(widget);
    groupbox_button->set_button_style(Gfx::ButtonStyle::CoolBar);
    groupbox_button->set_tooltip("GGroupBox");
    groupbox_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/vbwidgets/groupbox.png"));
    groupbox_button->on_click = [](GUI::Button&) {
        if (auto* form = VBForm::current())
            form->insert_widget(VBWidgetType::GGroupBox);
    };
    return window;
}
