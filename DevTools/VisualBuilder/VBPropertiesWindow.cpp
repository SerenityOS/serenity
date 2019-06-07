#include "VBPropertiesWindow.h"
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GTableView.h>
#include <LibGUI/GTextBox.h>
#include <LibGUI/GWidget.h>

VBPropertiesWindow::VBPropertiesWindow()
{
    set_title("Properties");
    set_rect(780, 200, 240, 280);

    auto* widget = new GWidget;
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->layout()->set_margins({ 2, 2, 2, 2 });
    set_main_widget(widget);

    m_table_view = new GTableView(widget);
    m_table_view->set_headers_visible(false);
    m_table_view->set_editable(true);
}

VBPropertiesWindow::~VBPropertiesWindow()
{
}
