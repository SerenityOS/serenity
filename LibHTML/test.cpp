#include <LibCore/CFile.h>
#include <LibHTML/Dump.h>
#include <LibHTML/Frame.h>
#include <LibHTML/Parser/CSSParser.h>
#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Parser/HTMLParser.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    CFile f(argc == 1 ? "/home/anon/small.html" : argv[1]);
    if (!f.open(CIODevice::ReadOnly)) {
        fprintf(stderr, "Error: %s\n", f.error_string());
        return 1;
    }

    extern const char default_stylesheet_source[];
    String css = default_stylesheet_source;

    auto sheet = parse_css(css);
    dump_sheet(sheet);

    String html = String::copy(f.read_all());
    auto doc = parse_html(html);
    dump_tree(doc);

    StyleResolver resolver(*doc);
    resolver.add_sheet(*sheet);

    auto doc_style = resolver.resolve_document_style(*doc);

    Function<void(const ParentNode&)> resolve_style = [&](const ParentNode& node) {
        node.for_each_child([&](const Node& child) {
            if (!child.is_element())
                return;
            auto style = resolver.resolve_element_style(static_cast<const Element&>(node));
            printf("Resolved LayoutStyle{%p} for Element{%p}\n", style.ptr(), &node);
            resolve_style(static_cast<const Element&>(child));
        });
    };
    resolve_style(*doc);

    doc->build_layout_tree();
    ASSERT(doc->layout_node());

    printf("\033[33;1mLayout tree before layout:\033[0m\n");
    dump_tree(*doc->layout_node());

    auto frame = make<Frame>();
    frame->set_document(doc);
    frame->layout();

    printf("\033[33;1mLayout tree after layout:\033[0m\n");
    dump_tree(*doc->layout_node());
    return 0;
}
