#pragma once

#include <LibHTML/DOM/HTMLElement.h>

class HTMLFontElement : public HTMLElement {
public:
    HTMLFontElement(Document&, const String& tag_name);
    virtual ~HTMLFontElement() override;

    virtual void apply_presentational_hints(StyleProperties&) const override;
};
