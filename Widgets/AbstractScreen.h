#pragma once

#include "Object.h"
#include "Rect.h"
#include "Size.h"

class AbstractScreen : public Object {
public:
    virtual ~AbstractScreen();

    int width() const { return m_width; }
    int height() const { return m_height; }

    static AbstractScreen& the();

    Size size() const { return { width(), height() }; }
    Rect rect() const { return { 0, 0, width(), height() }; }

protected:
    AbstractScreen(unsigned width, unsigned height);

private:
    int m_width { 0 };
    int m_height { 0 };
};

