#include <LibCore/CFile.h>
#include <LibGUI/GApplication.h>
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

    extern const char default_stylesheet_source[];
    String css = default_stylesheet_source;
    auto sheet = parse_css(css);

    String html = String::copy(f->read_all());
    auto document = parse_html(html);
    document->normalize();
    document->add_sheet(*sheet);

    auto window = GWindow::construct();
    auto widget = HtmlView::construct();
    widget->set_document(document);
    if (!widget->document()->title().is_null())
        window->set_title(String::format("%s - HTML", widget->document()->title().characters()));
    else
        window->set_title("HTML");
    window->set_main_widget(widget);
    window->show();

    return app.exec();
}
