#include "ColorDialog.h"
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GFrame.h>
#include <LibGUI/GSpinBox.h>
#include <LibGUI/GWidget.h>

ColorDialog::ColorDialog(Color color, CObject* parent)
    : GDialog(parent)
    , m_color(color)
{
    set_title("Edit Color");
    build();
}

ColorDialog::~ColorDialog()
{
}

void ColorDialog::build()
{
    auto* horizontal_container = new GWidget;
    horizontal_container->set_fill_with_background_color(true);
    horizontal_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    horizontal_container->layout()->set_margins({ 4, 4, 4, 4 });
    set_main_widget(horizontal_container);

    auto* left_vertical_container = new GWidget(horizontal_container);
    left_vertical_container->set_layout(make<GBoxLayout>(Orientation::Vertical));

    auto* right_vertical_container = new GWidget(horizontal_container);
    right_vertical_container->set_layout(make<GBoxLayout>(Orientation::Vertical));

    enum RGBComponent {
        Red, Green, Blue
    };

    auto* preview_widget = new GFrame(right_vertical_container);
    preview_widget->set_background_color(m_color);
    preview_widget->set_fill_with_background_color(true);
    right_vertical_container->layout()->add_spacer();
    auto* cancel_button = new GButton("Cancel", right_vertical_container);
    cancel_button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    cancel_button->set_preferred_size(0, 20);
    cancel_button->on_click = [&](auto&) {
        done(GDialog::ExecCancel);
    };
    auto* ok_button = new GButton("Okay", right_vertical_container);
    ok_button->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    ok_button->set_preferred_size(0, 20);
    ok_button->on_click = [&](auto&) {
        done(GDialog::ExecOK);
    };

    auto make_spinbox = [&](RGBComponent component, int initial_value) {
         auto spinbox = GSpinBox::construct(left_vertical_container);
         spinbox->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
         spinbox->set_preferred_size(0, 20);
         spinbox->set_min(0);
         spinbox->set_max(255);
         spinbox->set_value(initial_value);

         spinbox->on_change = [=](auto value) {
             if (component == Red)
                m_color.set_red(value);
             if (component == Green)
                m_color.set_green(value);
             if (component == Blue)
                m_color.set_blue(value);

             preview_widget->set_background_color(m_color);
             preview_widget->update();
         };
         return spinbox;
    };

    make_spinbox(Red, m_color.red());
    make_spinbox(Green, m_color.green());
    make_spinbox(Blue, m_color.blue());
}
