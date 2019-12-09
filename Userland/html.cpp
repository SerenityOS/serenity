#include <LibCore/CFile.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
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
#include <stdio.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto f = CFile::construct();
    bool success;
    if (argc < 2) {
        success = f->open(STDIN_FILENO, CIODevice::OpenMode::ReadOnly, CFile::ShouldCloseFileDescription::No);
    } else {
        f->set_filename(argv[1]);
        success = f->open(CIODevice::OpenMode::ReadOnly);
    }
    if (!success) {
        fprintf(stderr, "Error: %s\n", f->error_string());
        return 1;
    }

    String html = String::copy(f->read_all());
    auto document = parse_html_document(html);

    auto window = GWindow::construct();
    auto widget = HtmlView::construct();
    widget->set_document(document);
    if (!widget->document()->title().is_null())
        window->set_title(String::format("%s - HTML", widget->document()->title().characters()));
    else
        window->set_title("HTML");
    window->set_main_widget(widget);
    window->show();

    auto menubar = make<GMenuBar>();

    auto app_menu = GMenu::construct("HTML");
    app_menu->add_action(GCommonActions::make_quit_action([&](auto&) {
        app.quit();
    }));
    menubar->add_menu(move(app_menu));

    auto help_menu = GMenu::construct("Help");
    help_menu->add_action(GAction::create("About", [&](const GAction&) {
        GAboutDialog::show("HTML", GraphicsBitmap::load_from_file("/res/icons/32x32/filetype-html.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    window->set_icon(GraphicsBitmap::load_from_file("/res/icons/16x16/filetype-html.png"));

    return app.exec();
}
