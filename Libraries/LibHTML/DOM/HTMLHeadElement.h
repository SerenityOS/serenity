#pragma once

#include <LibHTML/DOM/HTMLElement.h>

class HTMLHeadElement : public HTMLElement {
public:
    HTMLHeadElement(Document&, const String& tag_name);
    virtual ~HTMLHeadElement() override;
};
