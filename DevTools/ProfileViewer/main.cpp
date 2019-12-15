#include "Profile.h"
#include "ProfileTimelineWidget.h"
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GTreeView.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: %s <profile-file>\n", argv[0]);
        return 0;
    }

    const char* path = argv[1];

    auto profile = Profile::load_from_file(path);
    if (!profile) {
        fprintf(stderr, "Unable to load profile '%s'\n", path);
        return 1;
    }

    GApplication app(argc, argv);

    auto window = GWindow::construct();
    window->set_title("ProfileViewer");
    window->set_rect(100, 100, 800, 600);

    auto main_widget = GWidget::construct();
    window->set_main_widget(main_widget);
    main_widget->set_fill_with_background_color(true);
    main_widget->set_layout(make<GBoxLayout>(Orientation::Vertical));

    auto timeline_widget = ProfileTimelineWidget::construct(*profile, main_widget);

    auto tree_view = GTreeView::construct(main_widget);
    tree_view->set_headers_visible(true);
    tree_view->set_size_columns_to_fit_content(true);
    tree_view->set_model(profile->model());

    auto menubar = make<GMenuBar>();
    auto app_menu = GMenu::construct("ProfileViewer");
    app_menu->add_action(GCommonActions::make_quit_action([&](auto&) { app.quit(); }));

    menubar->add_menu(move(app_menu));

    app.set_menubar(move(menubar));

    window->show();
    return app.exec();
}
