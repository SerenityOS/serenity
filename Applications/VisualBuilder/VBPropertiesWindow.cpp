#include "VBPropertiesWindow.h"
#include <LibGUI/GWidget.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GTableView.h>

VBPropertiesWindow::VBPropertiesWindow()
{
    set_title("Properties");
    set_rect(780, 200, 200, 280);

    auto* widget = new GWidget;
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    set_main_widget(widget);

    m_table_view = new GTableView(widget);
}

VBPropertiesWindow::~VBPropertiesWindow()
{
}
