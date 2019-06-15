#include <LibHTML/Dump.h>
#include <LibHTML/Element.h>
#include <LibHTML/Parser.h>

int main()
{
    String html = "<html><head><title>my page</title></head><body><h1>Hi there</h1><p>Hello World!</p></body></html>";
    auto doc = parse(html);
    dump_tree(doc);
}
