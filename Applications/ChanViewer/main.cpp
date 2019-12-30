#include "BoardListModel.h"
#include "ThreadCatalogModel.h"
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GComboBox.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GStatusBar.h>
#include <LibGUI/GTableView.h>
#include <LibGUI/GWindow.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto window = GWindow::construct();
    window->set_title("ChanViewer");
    window->set_rect(100, 100, 800, 500);
    window->set_icon(load_png("/res/icons/16x16/app-chanviewer.png"));

    auto widget = GWidget::construct();
    window->set_main_widget(widget);
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));

    auto board_combo = GComboBox::construct(widget);
    board_combo->set_only_allow_values_from_model(true);
    board_combo->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    board_combo->set_preferred_size(0, 20);
    board_combo->set_model(BoardListModel::create());

    auto catalog_view = GTableView::construct(widget);
    catalog_view->set_model(ThreadCatalogModel::create());
    auto& catalog_model = *static_cast<ThreadCatalogModel*>(catalog_view->model());

    auto statusbar = GStatusBar::construct(widget);

    board_combo->on_change = [&] (auto&, const GModelIndex& index) {
        auto selected_board = board_combo->model()->data(index, GModel::Role::Custom);
        ASSERT(selected_board.is_string());
        catalog_model.set_board(selected_board.to_string());
    };

    catalog_model.on_load_started = [&] {
        statusbar->set_text(String::format("Loading /%s/...", catalog_model.board().characters()));
    };

    catalog_model.on_load_finished = [&](bool success) {
        statusbar->set_text(success ? "Load finished" : "Load failed");
        if (success) {
            window->set_title(String::format("/%s/ - ChanViewer", catalog_model.board().characters()));
        }
    };

    window->show();

    auto menubar = make<GMenuBar>();

    auto app_menu = GMenu::construct("ChanViewer");
    app_menu->add_action(GCommonActions::make_quit_action([](auto&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto help_menu = GMenu::construct("Help");
    help_menu->add_action(GAction::create("About", [&](const GAction&) {
        GAboutDialog::show("ChanViewer", load_png("/res/icons/32x32/app-chanviewer.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    return app.exec();
}
