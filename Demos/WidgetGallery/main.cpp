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

    auto* window = new GWindow;
    window->set_rect(100, 100, 320, 620);
    window->set_title("Widget Gallery");

    auto* main_widget = new GWidget;
    window->set_main_widget(main_widget);
    main_widget->set_fill_with_background_color(true);
    main_widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    main_widget->layout()->set_margins({ 4, 4, 4, 4 });

    auto* checkbox1 = new GCheckBox("GCheckBox 1", main_widget);
    (void)checkbox1;
    auto* checkbox2 = new GCheckBox("GCheckBox 2", main_widget);
    checkbox2->set_enabled(false);

    auto* radio1 = new GRadioButton("GRadioButton 1", main_widget);
    (void)radio1;
    auto* radio2 = new GRadioButton("GRadioButton 2", main_widget);
    radio2->set_enabled(false);

    auto* button1 = new GButton("GButton 1", main_widget);
    (void)button1;
    auto* button2 = new GButton("GButton 2", main_widget);
    button2->set_enabled(false);

    auto* progress1 = new GProgressBar(main_widget);
    new CTimer(100, [progress1] {
        progress1->set_value(progress1->value() + 1);
        if (progress1->value() == progress1->max())
            progress1->set_value(progress1->min());
    });

    auto* label1 = new GLabel("GLabel 1", main_widget);
    (void)label1;
    auto* label2 = new GLabel("GLabel 2", main_widget);
    label2->set_enabled(false);

    auto* textbox1 = new GTextBox(main_widget);
    textbox1->set_text("GTextBox 1");
    auto* textbox2 = new GTextBox(main_widget);
    textbox2->set_text("GTextBox 2");
    textbox2->set_enabled(false);

    auto* spinbox1 = new GSpinBox(main_widget);
    (void)spinbox1;
    auto* spinbox2 = new GSpinBox(main_widget);
    spinbox2->set_enabled(false);

    auto* vertical_slider_container = new GWidget(main_widget);
    vertical_slider_container->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    vertical_slider_container->set_preferred_size(0, 100);
    vertical_slider_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    auto* vslider1 = new GSlider(Orientation::Vertical, vertical_slider_container);
    (void)vslider1;
    auto* vslider2 = new GSlider(Orientation::Vertical, vertical_slider_container);
    vslider2->set_enabled(false);
    auto* vslider3 = new GSlider(Orientation::Vertical, vertical_slider_container);
    vslider3->set_max(5);
    vslider3->set_knob_size_mode(GSlider::KnobSizeMode::Proportional);

    auto* slider1 = new GSlider(Orientation::Horizontal, main_widget);
    (void)slider1;
    auto* slider2 = new GSlider(Orientation::Horizontal, main_widget);
    slider2->set_enabled(false);
    auto* slider3 = new GSlider(Orientation::Horizontal, main_widget);
    slider3->set_max(5);
    slider3->set_knob_size_mode(GSlider::KnobSizeMode::Proportional);

    auto* scrollbar1 = new GScrollBar(Orientation::Horizontal, main_widget);
    scrollbar1->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    scrollbar1->set_preferred_size(0, 16);
    scrollbar1->set_min(0);
    scrollbar1->set_max(100);
    scrollbar1->set_value(50);
    auto* scrollbar2 = new GScrollBar(Orientation::Horizontal, main_widget);
    scrollbar2->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    scrollbar2->set_preferred_size(0, 16);
    scrollbar2->set_enabled(false);

    window->show();

    return app.exec();
}
