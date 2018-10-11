#pragma once

#include "Object.h"

class AbstractScreen : public Object {
public:
    virtual ~AbstractScreen();

    unsigned width() const { return m_width; }
    unsigned height() const { return m_height; }

    static AbstractScreen& the();

protected:
    AbstractScreen(unsigned width, unsigned height);

private:
    unsigned m_width { 0 };
    unsigned m_height { 0 };
};

