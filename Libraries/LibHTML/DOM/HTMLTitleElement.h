#pragma once

#include <LibHTML/DOM/HTMLElement.h>

class HTMLTitleElement : public HTMLElement {
public:
    HTMLTitleElement(Document&, const String& tag_name);
    virtual ~HTMLTitleElement() override;
};

template<>
inline bool is<HTMLTitleElement>(const Node& node)
{
    return is<Element>(node) && to<Element>(node).tag_name().to_lowercase() == "title";
}
