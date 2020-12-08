#include <LibCore/ArgsParser.h>
#include <LibGUI/Application.h>
#include <LibWeb/Dump.h>
#include <LibWeb/InProcessWebView.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
#include <stdlib.h>

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);
    auto window = GUI::Window::construct();
    window->set_title("DumpLayoutTree");
    window->resize(800, 600);
    window->show();
    auto& web_view = window->set_main_widget<Web::InProcessWebView>();
    web_view.load(URL::create_with_file_protocol(argv[1]));
    web_view.on_load_finish = [&](auto&) {
        auto* document = web_view.document();
        if (!document) {
            warnln("No document.");
            _exit(1);
        }
        auto* layout_root = document->layout_node();
        if (!layout_root) {
            warnln("No layout tree.");
            _exit(1);
        }
        StringBuilder builder;
        Web::dump_tree(builder, *layout_root);
        write(STDOUT_FILENO, builder.string_view().characters_without_null_termination(), builder.length());
        _exit(0);
    };
    return app->exec();
}
