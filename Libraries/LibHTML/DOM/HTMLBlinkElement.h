#pragma once

#include <LibHTML/DOM/HTMLElement.h>

class CTimer;

class HTMLBlinkElement : public HTMLElement {
public:
    HTMLBlinkElement(Document&, const String& tag_name);
    virtual ~HTMLBlinkElement() override;

private:
    void blink();

    NonnullRefPtr<CTimer> m_timer;
};
