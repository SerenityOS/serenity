#include <AK/URL.h>
#include <LibCore/CFile.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/HTMLLinkElement.h>
#include <LibHTML/Parser/CSSParser.h>

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
        if (url.protocol() != "file") {
            ASSERT_NOT_REACHED();
        }
        auto file = CFile::construct(url.path());
        if (!file->open(CIODevice::ReadOnly)) {
            dbg() << "Failed to open " << url.to_string();
            ASSERT_NOT_REACHED();
            return;
        }
        auto data = file->read_all();
        auto sheet = parse_css(String::copy(data));

        if (!sheet) {
            dbg() << "Failed to parse " << url.to_string();
            ASSERT_NOT_REACHED();
            return;
        }

        document().add_sheet(*sheet);
        document().invalidate_layout();
    }
}
