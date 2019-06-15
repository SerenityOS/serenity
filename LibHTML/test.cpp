#include <LibCore/CFile.h>
#include <LibHTML/Dump.h>
#include <LibHTML/Element.h>
#include <LibHTML/Parser.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    CFile f(argc == 1 ? "/home/anon/small.html" : argv[1]);
    if (!f.open(CIODevice::ReadOnly)) {
        fprintf(stderr, "Error: %s\n", f.error_string());
        return 1;
    }
    String html = String::copy(f.read_all());
    auto doc = parse(html);
    dump_tree(doc);

    doc->build_layout_tree();
    ASSERT(doc->layout_node());
    dump_tree(*doc->layout_node());
    return 0;
}
