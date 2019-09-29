#pragma once

#include <LibHTML/DOM/HTMLElement.h>

class HTMLHeadingElement : public HTMLElement {
public:
    HTMLHeadingElement(Document&, const String& tag_name);
    virtual ~HTMLHeadingElement() override;
};
