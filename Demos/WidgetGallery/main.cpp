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

#include <LibCore/Timer.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/ProgressBar.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/Slider.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

int main(int argc, char** argv)
{
    GUI::Application app(argc, argv);

    auto window = GUI::Window::construct();
    window->set_rect(100, 100, 320, 620);
    window->set_title("Widget Gallery");

    auto main_widget = GUI::Widget::construct();
    window->set_main_widget(main_widget);
    main_widget->set_fill_with_background_color(true);
    main_widget->set_layout<GUI::VerticalBoxLayout>();
    main_widget->layout()->set_margins({ 4, 4, 4, 4 });

    auto checkbox1 = main_widget->add<GUI::CheckBox>("GCheckBox 1");
    (void)checkbox1;
    auto checkbox2 = main_widget->add<GUI::CheckBox>("GCheckBox 2");
    checkbox2->set_enabled(false);

    auto radio1 = main_widget->add<GUI::RadioButton>("GRadioButton 1");
    (void)radio1;
    auto radio2 = main_widget->add<GUI::RadioButton>("GRadioButton 2");
    radio2->set_enabled(false);

    auto button1 = main_widget->add<GUI::Button>("GButton 1");
    (void)button1;
    auto button2 = main_widget->add<GUI::Button>("GButton 2");
    button2->set_enabled(false);

    auto progress1 = main_widget->add<GUI::ProgressBar>();
    auto timer = progress1->add<Core::Timer>(100, [&] {
        progress1->set_value(progress1->value() + 1);
        if (progress1->value() == progress1->max())
            progress1->set_value(progress1->min());
    });

    auto label1 = main_widget->add<GUI::Label>("GLabel 1");
    (void)label1;
    auto label2 = main_widget->add<GUI::Label>("GLabel 2");
    label2->set_enabled(false);

    auto textbox1 = main_widget->add<GUI::TextBox>();
    textbox1->set_text("GTextBox 1");
    auto textbox2 = main_widget->add<GUI::TextBox>();
    textbox2->set_text("GTextBox 2");
    textbox2->set_enabled(false);

    auto spinbox1 = main_widget->add<GUI::SpinBox>();
    (void)spinbox1;
    auto spinbox2 = main_widget->add<GUI::SpinBox>();
    spinbox2->set_enabled(false);

    auto vertical_slider_container = main_widget->add<GUI::Widget>();
    vertical_slider_container->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    vertical_slider_container->set_preferred_size(0, 100);
    vertical_slider_container->set_layout<GUI::HorizontalBoxLayout>();
    auto vslider1 = vertical_slider_container->add<GUI::VerticalSlider>();
    (void)vslider1;
    auto vslider2 = vertical_slider_container->add<GUI::VerticalSlider>();
    vslider2->set_enabled(false);
    auto vslider3 = vertical_slider_container->add<GUI::VerticalSlider>();
    vslider3->set_max(5);
    vslider3->set_knob_size_mode(GUI::Slider::KnobSizeMode::Proportional);

    auto slider1 = main_widget->add<GUI::HorizontalSlider>();
    (void)slider1;
    auto slider2 = main_widget->add<GUI::HorizontalSlider>();
    slider2->set_enabled(false);
    auto slider3 = main_widget->add<GUI::HorizontalSlider>();
    slider3->set_max(5);
    slider3->set_knob_size_mode(GUI::Slider::KnobSizeMode::Proportional);

    auto scrollbar1 = main_widget->add<GUI::ScrollBar>(Orientation::Horizontal);
    scrollbar1->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    scrollbar1->set_preferred_size(0, 16);
    scrollbar1->set_min(0);
    scrollbar1->set_max(100);
    scrollbar1->set_value(50);
    auto scrollbar2 = main_widget->add<GUI::ScrollBar>(Orientation::Horizontal);
    scrollbar2->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    scrollbar2->set_preferred_size(0, 16);
    scrollbar2->set_enabled(false);

    window->show();

    return app.exec();
}
