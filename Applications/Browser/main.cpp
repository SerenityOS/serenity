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
#include <LibGUI/GWindow.h>
#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Dump.h>
#include <LibHTML/HtmlView.h>
#include <LibHTML/Layout/LayoutBlock.h>
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

    toolbar->add_action(GCommonActions::make_go_back_action([&](auto&) {
        // FIXME: Implement back action
    }));

    toolbar->add_action(GCommonActions::make_go_forward_action([&](auto&) {
        // FIXME: Implement forward action
    }));

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
    };

    html_widget->on_link_click = [&](auto& url) {
        html_widget->load(html_widget->document()->complete_url(url));
    };

    html_widget->on_title_change = [&](auto& title) {
        window->set_title(String::format("%s - Browser", title.characters()));
    };

    auto focus_location_box_action = GAction::create("Focus location box", { Mod_Ctrl, Key_L }, [&](auto&) {
        location_box->select_all();
        location_box->set_focus(true);
    });

    auto statusbar = GStatusBar::construct(widget);

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
