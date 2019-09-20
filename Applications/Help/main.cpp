#include "ManualModel.h"
#include <LibCore/CFile.h>
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GMessageBox.h>
#include <LibGUI/GSplitter.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GTreeView.h>
#include <LibGUI/GWindow.h>
#include <LibHTML/HtmlView.h>
#include <LibHTML/Layout/LayoutNode.h>
#include <LibHTML/Parser/CSSParser.h>
#include <LibHTML/Parser/HTMLParser.h>
#include <LibMarkdown/MDDocument.h>

int main(int argc, char* argv[])
{
    GApplication app(argc, argv);

    auto window = GWindow::construct();
    window->set_title("Help");
    window->set_rect(300, 200, 570, 500);

    auto splitter = GSplitter::construct(Orientation::Horizontal, nullptr);

    auto model = ManualModel::create();

    auto tree_view = GTreeView::construct(splitter);
    tree_view->set_model(model);
    tree_view->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    tree_view->set_preferred_size(200, 500);

    auto html_view = HtmlView::construct(splitter);

    extern const char default_stylesheet_source[];
    String css = default_stylesheet_source;
    auto sheet = parse_css(css);

    tree_view->on_selection_change = [&] {
        String path = model->page_path(tree_view->selection().first());
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
        StringView source { (char*)buffer.data(), buffer.size() };

        MDDocument md_document;
        bool success = md_document.parse(source);
        ASSERT(success);

        String html = md_document.render_to_html();
        auto html_document = parse_html(html);
        html_document->normalize();
        html_document->add_sheet(sheet);
        html_view->set_document(html_document);

        String page_and_section = model->page_and_section(tree_view->selection().first());
        window->set_title(String::format("Help: %s", page_and_section.characters()));
    };

    window->set_main_widget(splitter);
    window->show();

    window->set_icon(load_png("/res/icons/16x16/book.png"));

    return app.exec();
}
