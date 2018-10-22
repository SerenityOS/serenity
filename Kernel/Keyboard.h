#pragma once

#include <AK/Types.h>
#include "IRQHandler.h"

class Keyboard final : public IRQHandler {
public:
    virtual ~Keyboard() override;
    Keyboard();

private:
    virtual void handleIRQ() override;

    byte m_modifiers { 0 };
};

