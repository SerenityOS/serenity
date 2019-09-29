#pragma once

#include <LibHTML/DOM/HTMLElement.h>

class HTMLHtmlElement : public HTMLElement {
public:
    HTMLHtmlElement(Document&, const String& tag_name);
    virtual ~HTMLHtmlElement() override;
};
