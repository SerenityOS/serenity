#include "VBPropertiesWindow.h"
#include <LibGUI/GWidget.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GTableView.h>
#include <LibGUI/GTextBox.h>

VBPropertiesWindow::VBPropertiesWindow()
{
    set_title("Properties");
    set_rect(780, 200, 200, 280);

    auto* widget = new GWidget;
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    set_main_widget(widget);

    m_text_box = new GTextBox(widget);
    m_text_box->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_text_box->set_preferred_size({ 0, 21 });

    m_table_view = new GTableView(widget);
}

VBPropertiesWindow::~VBPropertiesWindow()
{
}
