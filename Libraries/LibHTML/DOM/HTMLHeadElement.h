#pragma once

#include <LibHTML/DOM/HTMLElement.h>

class HTMLHeadElement : public HTMLElement {
public:
    HTMLHeadElement(Document&, const String& tag_name);
    virtual ~HTMLHeadElement() override;
};

template<>
inline bool is<HTMLHeadElement>(const Node& node)
{
    return is<Element>(node) && to<Element>(node).tag_name().to_lowercase() == "head";
}
