#include "ThreadCatalogModel.h"
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GTableView.h>
#include <LibGUI/GWindow.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* window = new GWindow;
    window->set_title("ChanViewer");
    window->set_rect(100, 100, 800, 500);
    window->set_icon(load_png("/res/icons/16x16/app-chanviewer.png"));

    auto* widget = new GWidget;
    window->set_main_widget(widget);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));

    auto* catalog_view = new GTableView(widget);
    catalog_view->set_model(ThreadCatalogModel::create());

    window->show();

    return app.exec();
}
