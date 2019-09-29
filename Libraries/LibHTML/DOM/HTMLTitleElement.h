#pragma once

#include <LibHTML/DOM/HTMLElement.h>

class HTMLTitleElement : public HTMLElement {
public:
    HTMLTitleElement(Document&, const String& tag_name);
    virtual ~HTMLTitleElement() override;
};
