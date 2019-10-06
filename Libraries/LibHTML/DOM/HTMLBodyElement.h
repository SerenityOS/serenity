#pragma once

#include <LibHTML/DOM/HTMLElement.h>

class HTMLBodyElement : public HTMLElement {
public:
    HTMLBodyElement(Document&, const String& tag_name);
    virtual ~HTMLBodyElement() override;

    virtual void parse_attribute(const String&, const String&) override;
    virtual void apply_presentational_hints(StyleProperties&) const override;
};

template<>
inline bool is<HTMLBodyElement>(const Node& node)
{
    return is<Element>(node) && to<Element>(node).tag_name().to_lowercase() == "body";
}
