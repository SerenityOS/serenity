#include <LibCore/CFile.h>
#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/CSS/StyledNode.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Dump.h>
#include <LibHTML/Frame.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutInline.h>
#include <LibHTML/Parser/CSSParser.h>
#include <LibHTML/Parser/HTMLParser.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    auto f = CFile::construct(argc == 1 ? "/home/anon/small.html" : argv[1]);
    if (!f->open(CIODevice::ReadOnly)) {
        fprintf(stderr, "Error: %s\n", f->error_string());
        return 1;
    }

    extern const char default_stylesheet_source[];
    String css = default_stylesheet_source;

    auto sheet = parse_css(css);
    dump_sheet(sheet);

    String html = String::copy(f->read_all());
    auto document = parse_html(html);
    dump_tree(document);
    document->add_sheet(*sheet);

    auto frame = make<Frame>();
    frame->set_document(document);
    frame->layout();
    return 0;
}
