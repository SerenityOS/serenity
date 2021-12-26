/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
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

#include <LibCore/Timer.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ColorInput.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Image.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/ProgressBar.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/Slider.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);

    auto app_icon = GUI::Icon::default_icon("app-widget-gallery");

    auto window = GUI::Window::construct();
    window->set_rect(100, 100, 433, 487);
    window->set_title("Widget Gallery");
    window->set_icon(app_icon.bitmap_for_size(16));

    auto menubar = GUI::MenuBar::construct();

    auto& app_menu = menubar->add_menu("Widget Gallery");
    app_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) { app->quit(); }));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::Action::create("About", [&](auto&) {
        GUI::AboutDialog::show("Widget Gallery", app_icon.bitmap_for_size(32), window);
    }));

    auto& root_widget = window->set_main_widget<GUI::Widget>();
    root_widget.set_fill_with_background_color(true);
    root_widget.set_layout<GUI::VerticalBoxLayout>();
    root_widget.layout()->set_margins({ 4, 4, 4, 4 });

    auto& tab_widget = root_widget.add<GUI::TabWidget>();

    auto& tab_basic = tab_widget.add_tab<GUI::Widget>("Basic");
    tab_basic.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    tab_basic.set_layout<GUI::VerticalBoxLayout>();
    tab_basic.layout()->set_margins({ 4, 4, 4, 4 });
    tab_basic.layout()->set_spacing(4);

    auto& checkbox1 = tab_basic.add<GUI::CheckBox>("CheckBox 1");
    (void)checkbox1;
    auto& checkbox2 = tab_basic.add<GUI::CheckBox>("CheckBox 2");
    checkbox2.set_enabled(false);

    auto& radio1 = tab_basic.add<GUI::RadioButton>("RadioButton 1");
    (void)radio1;
    auto& radio2 = tab_basic.add<GUI::RadioButton>("RadioButton 2");
    radio2.set_enabled(false);

    auto& button1 = tab_basic.add<GUI::Button>("Button 1");
    (void)button1;
    auto& button2 = tab_basic.add<GUI::Button>("Button 2");
    button2.set_enabled(false);

    auto& progress1 = tab_basic.add<GUI::ProgressBar>();
    progress1.add<Core::Timer>(100, [&] {
        progress1.set_value(progress1.value() + 1);
        if (progress1.value() == progress1.max())
            progress1.set_value(progress1.min());
    });

    auto& label1 = tab_basic.add<GUI::Label>("Label 1");
    (void)label1;
    auto& label2 = tab_basic.add<GUI::Label>("Label 2");
    label2.set_enabled(false);

    auto& textbox1 = tab_basic.add<GUI::TextBox>();
    textbox1.set_text("TextBox 1");
    auto& textbox2 = tab_basic.add<GUI::TextBox>();
    textbox2.set_text("TextBox 2");
    textbox2.set_enabled(false);

    auto& spinbox1 = tab_basic.add<GUI::SpinBox>();
    (void)spinbox1;
    auto& spinbox2 = tab_basic.add<GUI::SpinBox>();
    spinbox2.set_enabled(false);

    auto& color_input_enabled = tab_basic.add<GUI::ColorInput>();
    color_input_enabled.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    color_input_enabled.set_color(Color::from_string("#573666ff").value());
    color_input_enabled.set_color_picker_title("Select color for desktop");

    auto& color_input_disabled = tab_basic.add<GUI::ColorInput>();
    color_input_disabled.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    color_input_disabled.set_color(Color::from_string("#573666ff").value());
    color_input_disabled.set_enabled(false);

    auto& tab_others = tab_widget.add_tab<GUI::Widget>("Sliders");
    tab_others.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    tab_others.set_layout<GUI::VerticalBoxLayout>();
    tab_others.layout()->set_margins({ 4, 4, 4, 4 });
    tab_others.layout()->set_spacing(4);

    auto& vertical_slider_container = tab_others.add<GUI::Widget>();
    vertical_slider_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    vertical_slider_container.set_preferred_size(0, 100);
    vertical_slider_container.set_layout<GUI::HorizontalBoxLayout>();
    auto& vslider1 = vertical_slider_container.add<GUI::VerticalSlider>();
    (void)vslider1;
    auto& vslider2 = vertical_slider_container.add<GUI::VerticalSlider>();
    vslider2.set_enabled(false);
    auto& vslider3 = vertical_slider_container.add<GUI::VerticalSlider>();
    vslider3.set_max(5);
    vslider3.set_knob_size_mode(GUI::Slider::KnobSizeMode::Proportional);

    auto& slider1 = tab_others.add<GUI::HorizontalSlider>();
    (void)slider1;
    auto& slider2 = tab_others.add<GUI::HorizontalSlider>();
    slider2.set_enabled(false);
    auto& slider3 = tab_others.add<GUI::HorizontalSlider>();
    slider3.set_max(5);
    slider3.set_knob_size_mode(GUI::Slider::KnobSizeMode::Proportional);

    auto& scrollbar1 = tab_others.add<GUI::ScrollBar>(Orientation::Horizontal);
    scrollbar1.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    scrollbar1.set_preferred_size(0, 16);
    scrollbar1.set_min(0);
    scrollbar1.set_max(100);
    scrollbar1.set_value(50);
    auto& scrollbar2 = tab_others.add<GUI::ScrollBar>(Orientation::Horizontal);
    scrollbar2.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    scrollbar2.set_preferred_size(0, 16);
    scrollbar2.set_enabled(false);

    auto& tab_msgbox = tab_widget.add_tab<GUI::Widget>("Message Box");
    tab_msgbox.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    tab_msgbox.set_layout<GUI::VerticalBoxLayout>();
    tab_msgbox.layout()->set_margins({ 4, 4, 4, 4 });
    tab_msgbox.layout()->set_spacing(4);

    GUI::MessageBox::Type msg_box_type = GUI::MessageBox::Type::Error;

    auto& icon_group_box = tab_msgbox.add<GUI::GroupBox>("Icon");
    icon_group_box.set_layout<GUI::VerticalBoxLayout>();
    icon_group_box.layout()->set_margins({ 5, 15, 5, 5 });
    icon_group_box.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    icon_group_box.set_preferred_size(0, 150);

    auto& radio_none = icon_group_box.add<GUI::RadioButton>("None");
    radio_none.on_checked = [&](bool) {
        msg_box_type = GUI::MessageBox::Type::None;
    };
    auto& radio_information = icon_group_box.add<GUI::RadioButton>("Information");
    radio_information.on_checked = [&](bool) {
        msg_box_type = GUI::MessageBox::Type::Information;
    };
    auto& question_information = icon_group_box.add<GUI::RadioButton>("Question");
    question_information.on_checked = [&](bool) {
        msg_box_type = GUI::MessageBox::Type::Question;
    };
    auto& radio_warning = icon_group_box.add<GUI::RadioButton>("Warning");
    radio_warning.on_checked = [&](bool) {
        msg_box_type = GUI::MessageBox::Type::Warning;
    };
    auto& radio_error = icon_group_box.add<GUI::RadioButton>("Error");
    radio_error.set_checked(true);
    radio_error.on_checked = [&](bool) {
        msg_box_type = GUI::MessageBox::Type::Error;
    };

    auto& button_group_box = tab_msgbox.add<GUI::GroupBox>("Buttons");
    button_group_box.set_layout<GUI::VerticalBoxLayout>();
    button_group_box.layout()->set_margins({ 5, 10, 5, 5 });
    button_group_box.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    button_group_box.set_preferred_size(0, 120);

    GUI::MessageBox::InputType msg_box_input_type = GUI::MessageBox::InputType::OKCancel;

    auto& radio_ok = button_group_box.add<GUI::RadioButton>("OK");
    radio_ok.on_checked = [&](bool) {
        msg_box_input_type = GUI::MessageBox::InputType::OK;
    };
    auto& radio_ok_cancel = button_group_box.add<GUI::RadioButton>("OK & Cancel");
    radio_ok_cancel.set_checked(true);
    radio_ok_cancel.on_checked = [&](bool) {
        msg_box_input_type = GUI::MessageBox::InputType::OKCancel;
    };
    auto& radio_yes_no = button_group_box.add<GUI::RadioButton>("Yes & No");
    radio_yes_no.on_checked = [&](bool) {
        msg_box_input_type = GUI::MessageBox::InputType::YesNo;
    };
    auto& radio_yes_no_cancel = button_group_box.add<GUI::RadioButton>("Yes & No & Cancel");
    radio_yes_no_cancel.on_checked = [&](bool) {
        msg_box_input_type = GUI::MessageBox::InputType::YesNoCancel;
    };

    auto& title_textbox = tab_msgbox.add<GUI::TextBox>();
    title_textbox.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    title_textbox.set_preferred_size(0, 20);
    title_textbox.set_text("Demo Title");

    auto& content_textbox = tab_msgbox.add<GUI::TextBox>();
    content_textbox.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    content_textbox.set_preferred_size(0, 20);
    content_textbox.set_text("Demo text for message box.");

    auto& show_buton = tab_msgbox.add<GUI::Button>("Show");
    show_buton.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    show_buton.set_preferred_size(0, 20);
    show_buton.on_click = [&](auto) {
        GUI::MessageBox::show(window, content_textbox.text(), title_textbox.text(), msg_box_type, msg_box_input_type);
    };

    tab_msgbox.layout()->add_spacer();

    auto& tab_image = tab_widget.add_tab<GUI::Widget>("Image");
    tab_image.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    tab_image.set_layout<GUI::VerticalBoxLayout>();
    tab_image.layout()->set_margins({ 4, 4, 4, 4 });
    tab_image.layout()->set_spacing(4);

    auto& banner_image = tab_image.add<GUI::Image>();
    banner_image.load_from_file("/res/brand-banner.png");

    auto& gif_animation_image = tab_image.add<GUI::Image>();
    gif_animation_image.load_from_file("/res/download-animation.gif");

    auto& tab_cursors = tab_widget.add_tab<GUI::Widget>("Cursors");
    tab_cursors.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    tab_cursors.set_layout<GUI::VerticalBoxLayout>();
    tab_cursors.layout()->set_margins({ 4, 4, 4, 4 });
    tab_cursors.layout()->set_spacing(4);

    auto& cursor_group_box = tab_cursors.add<GUI::GroupBox>("Cursor");
    cursor_group_box.set_layout<GUI::VerticalBoxLayout>();
    cursor_group_box.layout()->set_margins({ 5, 15, 5, 5 });
    cursor_group_box.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    cursor_group_box.set_preferred_size(0, 390);

    auto& radio_cursor_none = cursor_group_box.add<GUI::RadioButton>("None");
    radio_cursor_none.set_checked(true);
    radio_cursor_none.on_checked = [&](bool) {
        window->set_override_cursor(GUI::StandardCursor::None);
    };
    auto& radio_cursor_arrow = cursor_group_box.add<GUI::RadioButton>("Arrow");
    radio_cursor_arrow.on_checked = [&](bool) {
        window->set_override_cursor(GUI::StandardCursor::Arrow);
    };
    auto& radio_cursor_i_beam = cursor_group_box.add<GUI::RadioButton>("IBeam");
    radio_cursor_i_beam.on_checked = [&](bool) {
        window->set_override_cursor(GUI::StandardCursor::IBeam);
    };
    auto& radio_cursor_resize_horizontal = cursor_group_box.add<GUI::RadioButton>("ResizeHorizontal");
    radio_cursor_resize_horizontal.on_checked = [&](bool) {
        window->set_override_cursor(GUI::StandardCursor::ResizeHorizontal);
    };
    auto& radio_cursor_resize_vertical = cursor_group_box.add<GUI::RadioButton>("ResizeVertical");
    radio_cursor_resize_vertical.on_checked = [&](bool) {
        window->set_override_cursor(GUI::StandardCursor::ResizeVertical);
    };
    auto& radio_cursor_resize_diagonal_tlbr = cursor_group_box.add<GUI::RadioButton>("ResizeDiagonalTLBR");
    radio_cursor_resize_diagonal_tlbr.on_checked = [&](bool) {
        window->set_override_cursor(GUI::StandardCursor::ResizeDiagonalTLBR);
    };
    auto& radio_cursor_resize_diagonal_bltr = cursor_group_box.add<GUI::RadioButton>("ResizeDiagonalBLTR");
    radio_cursor_resize_diagonal_bltr.on_checked = [&](bool) {
        window->set_override_cursor(GUI::StandardCursor::ResizeDiagonalBLTR);
    };
    auto& radio_cursor_resize_column = cursor_group_box.add<GUI::RadioButton>("ResizeColumn");
    radio_cursor_resize_column.on_checked = [&](bool) {
        window->set_override_cursor(GUI::StandardCursor::ResizeColumn);
    };
    auto& radio_cursor_resize_row = cursor_group_box.add<GUI::RadioButton>("ResizeRow");
    radio_cursor_resize_row.on_checked = [&](bool) {
        window->set_override_cursor(GUI::StandardCursor::ResizeRow);
    };
    auto& radio_cursor_hand = cursor_group_box.add<GUI::RadioButton>("Hand");
    radio_cursor_hand.on_checked = [&](bool) {
        window->set_override_cursor(GUI::StandardCursor::Hand);
    };
    auto& radio_cursor_help = cursor_group_box.add<GUI::RadioButton>("Help");
    radio_cursor_help.on_checked = [&](bool) {
        window->set_override_cursor(GUI::StandardCursor::Help);
    };
    auto& radio_cursor_drag = cursor_group_box.add<GUI::RadioButton>("Drag");
    radio_cursor_drag.on_checked = [&](bool) {
        window->set_override_cursor(GUI::StandardCursor::Drag);
    };
    auto& radio_cursor_move = cursor_group_box.add<GUI::RadioButton>("Move");
    radio_cursor_move.on_checked = [&](bool) {
        window->set_override_cursor(GUI::StandardCursor::Move);
    };
    auto& radio_cursor_wait = cursor_group_box.add<GUI::RadioButton>("Wait");
    radio_cursor_wait.on_checked = [&](bool) {
        window->set_override_cursor(GUI::StandardCursor::Wait);
    };

    app->set_menubar(move(menubar));

    window->show();

    return app->exec();
}
