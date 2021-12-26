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

#include <LibCore/CTimer.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GCheckBox.h>
#include <LibGUI/GGroupBox.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GProgressBar.h>
#include <LibGUI/GRadioButton.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GSlider.h>
#include <LibGUI/GSpinBox.h>
#include <LibGUI/GTextBox.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto window = GWindow::construct();
    window->set_rect(100, 100, 320, 620);
    window->set_title("Widget Gallery");

    auto main_widget = GWidget::construct();
    window->set_main_widget(main_widget);
    main_widget->set_fill_with_background_color(true);
    main_widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    main_widget->layout()->set_margins({ 4, 4, 4, 4 });

    auto checkbox1 = GCheckBox::construct("GCheckBox 1", main_widget);
    (void)checkbox1;
    auto checkbox2 = GCheckBox::construct("GCheckBox 2", main_widget);
    checkbox2->set_enabled(false);

    auto radio1 = GRadioButton::construct("GRadioButton 1", main_widget);
    (void)radio1;
    auto radio2 = GRadioButton::construct("GRadioButton 2", main_widget);
    radio2->set_enabled(false);

    auto button1 = GButton::construct("GButton 1", main_widget);
    (void)button1;
    auto button2 = GButton::construct("GButton 2", main_widget);
    button2->set_enabled(false);

    auto progress1 = GProgressBar::construct(main_widget);
    auto timer = CTimer::construct(100, [&] {
        progress1->set_value(progress1->value() + 1);
        if (progress1->value() == progress1->max())
            progress1->set_value(progress1->min());
    });

    auto label1 = GLabel::construct("GLabel 1", main_widget);
    (void)label1;
    auto label2 = GLabel::construct("GLabel 2", main_widget);
    label2->set_enabled(false);

    auto textbox1 = GTextBox::construct(main_widget);
    textbox1->set_text("GTextBox 1");
    auto textbox2 = GTextBox::construct(main_widget);
    textbox2->set_text("GTextBox 2");
    textbox2->set_enabled(false);

    auto spinbox1 = GSpinBox::construct(main_widget);
    (void)spinbox1;
    auto spinbox2 = GSpinBox::construct(main_widget);
    spinbox2->set_enabled(false);

    auto vertical_slider_container = GWidget::construct(main_widget.ptr());
    vertical_slider_container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    vertical_slider_container->set_preferred_size(0, 100);
    vertical_slider_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    auto vslider1 = GSlider::construct(Orientation::Vertical, vertical_slider_container);
    (void)vslider1;
    auto vslider2 = GSlider::construct(Orientation::Vertical, vertical_slider_container);
    vslider2->set_enabled(false);
    auto vslider3 = GSlider::construct(Orientation::Vertical, vertical_slider_container);
    vslider3->set_max(5);
    vslider3->set_knob_size_mode(GSlider::KnobSizeMode::Proportional);

    auto slider1 = GSlider::construct(Orientation::Horizontal, main_widget);
    (void)slider1;
    auto slider2 = GSlider::construct(Orientation::Horizontal, main_widget);
    slider2->set_enabled(false);
    auto slider3 = GSlider::construct(Orientation::Horizontal, main_widget);
    slider3->set_max(5);
    slider3->set_knob_size_mode(GSlider::KnobSizeMode::Proportional);

    auto scrollbar1 = GScrollBar::construct(Orientation::Horizontal, main_widget);
    scrollbar1->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    scrollbar1->set_preferred_size(0, 16);
    scrollbar1->set_min(0);
    scrollbar1->set_max(100);
    scrollbar1->set_value(50);
    auto scrollbar2 = GScrollBar::construct(Orientation::Horizontal, main_widget);
    scrollbar2->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    scrollbar2->set_preferred_size(0, 16);
    scrollbar2->set_enabled(false);

    window->show();

    return app.exec();
}
