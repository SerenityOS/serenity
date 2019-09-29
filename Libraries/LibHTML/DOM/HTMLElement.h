#pragma once

#include <LibHTML/DOM/Element.h>

class HTMLElement : public Element {
public:
    HTMLElement(Document&, const String& tag_name);
    virtual ~HTMLElement() override;
};
