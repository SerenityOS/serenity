#include "History.h"
#include <LibCore/CFile.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GStatusBar.h>
#include <LibGUI/GTextBox.h>
#include <LibGUI/GToolBar.h>
#include <LibGUI/GTreeView.h>
#include <LibGUI/GWindow.h>
#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/DOMTreeModel.h>
#include <LibHTML/Dump.h>
#include <LibHTML/HtmlView.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutDocument.h>
#include <LibHTML/Layout/LayoutInline.h>
#include <LibHTML/Layout/LayoutNode.h>
#include <LibHTML/Parser/CSSParser.h>
#include <LibHTML/Parser/HTMLParser.h>
#include <LibHTML/ResourceLoader.h>
#include <stdio.h>

static const char* home_url = "file:///home/anon/www/welcome.html";

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto window = GWindow::construct();
    window->set_rect(100, 100, 640, 480);

    auto widget = GWidget::construct();
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->layout()->set_spacing(0);

    auto toolbar = GToolBar::construct(widget);
    auto html_widget = HtmlView::construct(widget);

    History<URL> history;

    RefPtr<GAction> go_back_action;
    RefPtr<GAction> go_forward_action;

    auto update_actions = [&]() {
        go_back_action->set_enabled(history.can_go_back());
        go_forward_action->set_enabled(history.can_go_forward());
    };

    bool should_push_loads_to_history = true;

    go_back_action = GCommonActions::make_go_back_action([&](auto&) {
        history.go_back();
        update_actions();
        TemporaryChange<bool> change(should_push_loads_to_history, false);
        html_widget->load(history.current());
    });

    go_forward_action = GCommonActions::make_go_forward_action([&](auto&) {
        history.go_forward();
        update_actions();
        TemporaryChange<bool> change(should_push_loads_to_history, false);
        html_widget->load(history.current());
    });

    toolbar->add_action(*go_back_action);
    toolbar->add_action(*go_forward_action);

    toolbar->add_action(GCommonActions::make_go_home_action([&](auto&) {
        html_widget->load(home_url);
    }));

    toolbar->add_action(GCommonActions::make_reload_action([&](auto&) {
        html_widget->reload();
    }));

    auto location_box = GTextBox::construct(toolbar);

    location_box->on_return_pressed = [&] {
        html_widget->load(location_box->text());
    };

    html_widget->on_load_start = [&](auto& url) {
        location_box->set_text(url.to_string());
        if (should_push_loads_to_history)
            history.push(url);
        update_actions();
    };

    html_widget->on_link_click = [&](auto& url) {
        if (url.starts_with("#")) {
            html_widget->scroll_to_anchor(url.substring_view(1, url.length() - 1));
        } else {
            html_widget->load(html_widget->document()->complete_url(url));
        }
    };

    html_widget->on_title_change = [&](auto& title) {
        window->set_title(String::format("%s - Browser", title.characters()));
    };

    auto focus_location_box_action = GAction::create("Focus location box", { Mod_Ctrl, Key_L }, [&](auto&) {
        location_box->select_all();
        location_box->set_focus(true);
    });

    auto statusbar = GStatusBar::construct(widget);

    html_widget->on_link_hover = [&](auto& href) {
        statusbar->set_text(href);
    };

    ResourceLoader::the().on_load_counter_change = [&] {
        if (ResourceLoader::the().pending_loads() == 0) {
            statusbar->set_text("");
            return;
        }
        statusbar->set_text(String::format("Loading (%d pending resources...)", ResourceLoader::the().pending_loads()));
    };

    auto menubar = make<GMenuBar>();

    auto app_menu = make<GMenu>("Browser");
    app_menu->add_action(GCommonActions::make_quit_action([&](auto&) {
        app.quit();
    }));
    menubar->add_menu(move(app_menu));

    RefPtr<GWindow> dom_inspector_window;
    RefPtr<GTreeView> dom_tree_view;

    auto inspect_menu = make<GMenu>("Inspect");
    inspect_menu->add_action(GAction::create("Inspect DOM tree", [&](auto&) {
        if (!dom_inspector_window) {
            dom_inspector_window = GWindow::construct();
            dom_inspector_window->set_rect(100, 100, 300, 500);
            dom_inspector_window->set_title("DOM inspector");
            dom_tree_view = GTreeView::construct(nullptr);
            dom_inspector_window->set_main_widget(dom_tree_view);
        }
        if (html_widget->document())
            dom_tree_view->set_model(DOMTreeModel::create(*html_widget->document()));
        else
            dom_tree_view->set_model(nullptr);
        dom_inspector_window->show();
        dom_inspector_window->move_to_front();
    }));
    menubar->add_menu(move(inspect_menu));

    auto debug_menu = make<GMenu>("Debug");
    debug_menu->add_action(GAction::create("Dump DOM tree", [&](auto&) {
        dump_tree(*html_widget->document());
    }));
    debug_menu->add_action(GAction::create("Dump Layout tree", [&](auto&) {
        dump_tree(*html_widget->document()->layout_node());
    }));
    debug_menu->add_separator();
    auto line_box_borders_action = GAction::create("Line box borders", [&](auto& action) {
        action.set_checked(!action.is_checked());
        html_widget->set_should_show_line_box_borders(action.is_checked());
        html_widget->update();
    });
    line_box_borders_action->set_checkable(true);
    line_box_borders_action->set_checked(false);
    debug_menu->add_action(line_box_borders_action);
    menubar->add_menu(move(debug_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [&](const GAction&) {
        GAboutDialog::show("Browser", GraphicsBitmap::load_from_file("/res/icons/32x32/filetype-html.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    window->set_icon(GraphicsBitmap::load_from_file("/res/icons/16x16/filetype-html.png"));

    window->set_title("Browser");
    window->set_main_widget(widget);
    window->show();

    URL url_to_load = home_url;

    if (app.args().size() >= 1) {
        url_to_load = URL();
        url_to_load.set_protocol("file");
        url_to_load.set_path(app.args()[0]);
    }

    html_widget->load(url_to_load);

    return app.exec();
}
