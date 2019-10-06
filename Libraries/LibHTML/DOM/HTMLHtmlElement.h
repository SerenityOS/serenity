#pragma once

#include <LibHTML/DOM/HTMLElement.h>

class HTMLHtmlElement : public HTMLElement {
public:
    HTMLHtmlElement(Document&, const String& tag_name);
    virtual ~HTMLHtmlElement() override;
};

template<>
inline bool is<HTMLHtmlElement>(const Node& node)
{
    return is<Element>(node) && to<Element>(node).tag_name().to_lowercase() == "html";
}
