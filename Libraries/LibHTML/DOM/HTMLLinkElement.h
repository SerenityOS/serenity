#pragma once

#include <LibHTML/DOM/HTMLElement.h>

class HTMLLinkElement final : public HTMLElement {
public:
    HTMLLinkElement(Document&, const String& tag_name);
    virtual ~HTMLLinkElement() override;

    virtual void inserted_into(Node&) override;

    String rel() const { return attribute("rel"); }
    String type() const { return attribute("type"); }
    String href() const { return attribute("href"); }
};

template<>
inline bool is<HTMLLinkElement>(const Node& node)
{
    return is<Element>(node) && to<Element>(node).tag_name().to_lowercase() == "link";
}
