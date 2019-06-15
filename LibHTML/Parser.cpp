#include <LibHTML/Element.h>
#include <LibHTML/Parser.h>
#include <LibHTML/Text.h>

static Retained<Element> create_element(const String& tag_name)
{
    return adopt(*new Element(tag_name));
}

Retained<Document> parse(const String& html)
{
    auto doc = adopt(*new Document);

    auto head = create_element("head");
    auto title = create_element("title");
    auto title_text = adopt(*new Text("Page Title"));
    title->append_child(title_text);
    head->append_child(title);

    doc->append_child(head);

    auto body = create_element("body");
    auto h1 = create_element("h1");
    auto h1_text = adopt(*new Text("Hello World!"));

    h1->append_child(h1_text);
    body->append_child(h1);
    doc->append_child(body);

    return doc;
}

