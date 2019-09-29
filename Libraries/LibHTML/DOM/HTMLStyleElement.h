#pragma once

#include <LibHTML/DOM/HTMLElement.h>

class StyleSheet;

class HTMLStyleElement : public HTMLElement {
public:
    HTMLStyleElement(Document&, const String& tag_name);
    virtual ~HTMLStyleElement() override;

    virtual void inserted_into(Node&) override;
    virtual void removed_from(Node&) override;

private:
    RefPtr<StyleSheet> m_stylesheet;
};
