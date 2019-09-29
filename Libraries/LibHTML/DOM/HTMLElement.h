#pragma once

#include <LibHTML/DOM/Element.h>

class HTMLElement : public Element {
public:
    HTMLElement(Document&, const String& tag_name);
    virtual ~HTMLElement() override;

    String title() const { return attribute("title"); }

private:
    virtual bool is_html_element() const final { return true; }
};
