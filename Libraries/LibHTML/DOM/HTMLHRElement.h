#pragma once

#include <LibHTML/DOM/HTMLElement.h>

class HTMLHRElement : public HTMLElement {
public:
    HTMLHRElement(Document&, const String& tag_name);
    virtual ~HTMLHRElement() override;
};
