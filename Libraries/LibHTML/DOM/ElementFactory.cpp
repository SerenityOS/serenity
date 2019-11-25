#include <LibHTML/DOM/ElementFactory.h>
#include <LibHTML/DOM/HTMLAnchorElement.h>
#include <LibHTML/DOM/HTMLBRElement.h>
#include <LibHTML/DOM/HTMLBlinkElement.h>
#include <LibHTML/DOM/HTMLBodyElement.h>
#include <LibHTML/DOM/HTMLFontElement.h>
#include <LibHTML/DOM/HTMLFormElement.h>
#include <LibHTML/DOM/HTMLHRElement.h>
#include <LibHTML/DOM/HTMLHeadElement.h>
#include <LibHTML/DOM/HTMLHeadingElement.h>
#include <LibHTML/DOM/HTMLHtmlElement.h>
#include <LibHTML/DOM/HTMLImageElement.h>
#include <LibHTML/DOM/HTMLInputElement.h>
#include <LibHTML/DOM/HTMLLinkElement.h>
#include <LibHTML/DOM/HTMLStyleElement.h>
#include <LibHTML/DOM/HTMLTitleElement.h>

NonnullRefPtr<Element> create_element(Document& document, const String& tag_name)
{
    auto lowercase_tag_name = tag_name.to_lowercase();
    if (lowercase_tag_name == "a")
        return adopt(*new HTMLAnchorElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "html")
        return adopt(*new HTMLHtmlElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "head")
        return adopt(*new HTMLHeadElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "body")
        return adopt(*new HTMLBodyElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "font")
        return adopt(*new HTMLFontElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "hr")
        return adopt(*new HTMLHRElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "style")
        return adopt(*new HTMLStyleElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "title")
        return adopt(*new HTMLTitleElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "link")
        return adopt(*new HTMLLinkElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "img")
        return adopt(*new HTMLImageElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "blink")
        return adopt(*new HTMLBlinkElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "form")
        return adopt(*new HTMLFormElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "input")
        return adopt(*new HTMLInputElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "br")
        return adopt(*new HTMLBRElement(document, lowercase_tag_name));
    if (lowercase_tag_name == "h1"
        || lowercase_tag_name == "h2"
        || lowercase_tag_name == "h3"
        || lowercase_tag_name == "h4"
        || lowercase_tag_name == "h5"
        || lowercase_tag_name == "h6") {
        return adopt(*new HTMLHeadingElement(document, lowercase_tag_name));
    }
    return adopt(*new Element(document, lowercase_tag_name));
}
