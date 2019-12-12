#include "Profile.h"
#include <LibGUI/GApplication.h>
#include <LibGUI/GTreeView.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: %s <profile-file>\n", argv[0]);
        return 0;
    }

    auto profile = Profile::load_from_file(argv[1]);
    if (!profile) {
        fprintf(stderr, "Unable to load profile '%s'\n", argv[1]);
        return 1;
    }

    GApplication app(argc, argv);

    auto window = GWindow::construct();
    window->set_title("ProfileViewer");
    window->set_rect(100, 100, 800, 600);

    auto tree_view = GTreeView::construct(nullptr);
    tree_view->set_model(profile->model());

    window->set_main_widget(tree_view);

    window->show();
    return app.exec();
}
