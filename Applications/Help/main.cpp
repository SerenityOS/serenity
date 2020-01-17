#include "History.h"
#include "ManualModel.h"
#include <LibCore/CFile.h>
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GMessageBox.h>
#include <LibGUI/GSplitter.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GToolBar.h>
#include <LibGUI/GTreeView.h>
#include <LibGUI/GWindow.h>
#include <LibHTML/HtmlView.h>
#include <LibHTML/Layout/LayoutNode.h>
#include <LibHTML/Parser/CSSParser.h>
#include <LibHTML/Parser/HTMLParser.h>
#include <LibMarkdown/MDDocument.h>
#include <libgen.h>
#include <stdio.h>

int main(int argc, char* argv[])
{
    if (pledge("stdio shared_buffer accept rpath unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GApplication app(argc, argv);

    if (pledge("stdio shared_buffer accept rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto window = GWindow::construct();
    window->set_title("Help");
    window->set_rect(300, 200, 570, 500);

    auto widget = GWidget::construct();
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->layout()->set_spacing(0);

    auto toolbar = GToolBar::construct(widget);

    auto splitter = GSplitter::construct(Orientation::Horizontal, widget);

    auto model = ManualModel::create();

    auto tree_view = GTreeView::construct(splitter);
    tree_view->set_model(model);
    tree_view->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    tree_view->set_preferred_size(200, 500);

    auto html_view = HtmlView::construct(splitter);

    History history;

    RefPtr<GAction> go_back_action;
    RefPtr<GAction> go_forward_action;

    auto update_actions = [&]() {
        go_back_action->set_enabled(history.can_go_back());
        go_forward_action->set_enabled(history.can_go_forward());
    };

    auto open_page = [&](const String& path) {
        if (path.is_null()) {
            html_view->set_document(nullptr);
            return;
        }

        dbg() << "Opening page at " << path;

        auto file = CFile::construct();
        file->set_filename(path);

        if (!file->open(CIODevice::OpenMode::ReadOnly)) {
            int saved_errno = errno;
            GMessageBox::show(strerror(saved_errno), "Failed to open man page", GMessageBox::Type::Error, GMessageBox::InputType::OK, window);
            return;
        }
        auto buffer = file->read_all();
        StringView source { (const char*)buffer.data(), (size_t)buffer.size() };

        MDDocument md_document;
        bool success = md_document.parse(source);
        ASSERT(success);

        String html = md_document.render_to_html();
        auto html_document = parse_html_document(html);
        html_view->set_document(html_document);

        String page_and_section = model->page_and_section(tree_view->selection().first());
        window->set_title(String::format("Help: %s", page_and_section.characters()));
    };

    tree_view->on_selection_change = [&] {
        String path = model->page_path(tree_view->selection().first());
        if (path.is_null()) {
            html_view->set_document(nullptr);
            return;
        }
        history.push(path);
        update_actions();
        open_page(path);
    };

    html_view->on_link_click = [&](const String& href) {
        char* current_path = strdup(history.current().characters());
        char* dir_path = dirname(current_path);
        char* path = realpath(String::format("%s/%s", dir_path, href.characters()).characters(), nullptr);
        free(current_path);
        history.push(path);
        update_actions();
        open_page(path);
        free(path);
    };

    go_back_action = GCommonActions::make_go_back_action([&](auto&) {
        history.go_back();
        update_actions();
        open_page(history.current());
    });

    go_forward_action = GCommonActions::make_go_forward_action([&](auto&) {
        history.go_forward();
        update_actions();
        open_page(history.current());
    });

    go_back_action->set_enabled(false);
    go_forward_action->set_enabled(false);

    toolbar->add_action(*go_back_action);
    toolbar->add_action(*go_forward_action);

    auto menubar = make<GMenuBar>();

    auto app_menu = GMenu::construct("Help");
    app_menu->add_action(GAction::create("About", [&](const GAction&) {
        GAboutDialog::show("Help", load_png("/res/icons/16x16/book.png"), window);
    }));
    app_menu->add_separator();
    app_menu->add_action(GCommonActions::make_quit_action([](auto&) {
        GApplication::the().quit(0);
    }));
    menubar->add_menu(move(app_menu));

    auto go_menu = GMenu::construct("Go");
    go_menu->add_action(*go_back_action);
    go_menu->add_action(*go_forward_action);
    menubar->add_menu(move(go_menu));

    app.set_menubar(move(menubar));

    window->set_main_widget(widget);
    window->show();

    window->set_icon(load_png("/res/icons/16x16/book.png"));

    return app.exec();
}
