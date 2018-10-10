#pragma once

#include "Object.h"

class Widget;

class AbstractScreen : public Object {
public:
    virtual ~AbstractScreen();

    unsigned width() const { return m_width; }
    unsigned height() const { return m_height; }

    Widget* rootWidget() { return m_rootWidget; }
    void setRootWidget(Widget*);

    static AbstractScreen& the();

protected:
    AbstractScreen(unsigned width, unsigned height);

private:
    virtual void event(Event&) override;

    unsigned m_width { 0 };
    unsigned m_height { 0 };

    Widget* m_rootWidget { nullptr };
};

