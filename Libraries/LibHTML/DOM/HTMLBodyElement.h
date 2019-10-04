#pragma once

#include <LibHTML/DOM/HTMLElement.h>

class HTMLBodyElement : public HTMLElement {
public:
    HTMLBodyElement(Document&, const String& tag_name);
    virtual ~HTMLBodyElement() override;

    virtual void apply_presentational_hints(StyleProperties&) const override;
};
