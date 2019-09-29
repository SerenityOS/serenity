#pragma once

#include <LibHTML/DOM/HTMLElement.h>

class HTMLAnchorElement : public HTMLElement {
public:
    HTMLAnchorElement(Document&, const String& tag_name);
    virtual ~HTMLAnchorElement() override;

    String href() const { return attribute("href"); }
};
