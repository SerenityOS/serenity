#include <AK/URL.h>
#include <LibCore/CFile.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/HTMLLinkElement.h>
#include <LibHTML/Parser/CSSParser.h>
#include <LibHTML/ResourceLoader.h>

HTMLLinkElement::HTMLLinkElement(Document& document, const String& tag_name)
    : HTMLElement(document, tag_name)
{
}

HTMLLinkElement::~HTMLLinkElement()
{
}

void HTMLLinkElement::inserted_into(Node&)
{
    if (rel() == "stylesheet") {
        URL url = document().complete_url(href());
        ResourceLoader::the().load(url, [&](auto data) {
            if (data.is_null()) {
                dbg() << "HTMLLinkElement: Failed to load stylesheet: " << href();
                return;
            }
            auto sheet = parse_css(data);
            if (!sheet) {
                dbg() << "HTMLLinkElement: Failed to parse stylesheet: " << href();
                return;
            }
            document().add_sheet(*sheet);
            document().invalidate_layout();
        });
    }
}
