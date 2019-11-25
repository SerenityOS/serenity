#pragma once

#include <LibHTML/DOM/HTMLElement.h>

class HTMLFormElement : public HTMLElement {
public:
    HTMLFormElement(Document&, const String& tag_name);
    virtual ~HTMLFormElement() override;

    String action() const { return attribute("action"); }
    String method() const { return attribute("method"); }

    void submit();
};

template<>
inline bool is<HTMLFormElement>(const Node& node)
{
    return is<Element>(node) && to<Element>(node).tag_name().to_lowercase() == "form";
}
