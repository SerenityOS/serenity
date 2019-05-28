#pragma once

#include <AK/Types.h>

namespace PIC {

void enable(byte number);
void disable(byte number);
void eoi(byte number);
void initialize();
word get_isr();
word get_irr();

}

class IRQHandlerScope {
public:
    explicit IRQHandlerScope(byte irq)
        : m_irq(irq)
    {
    }
    ~IRQHandlerScope() { PIC::eoi(m_irq); }

private:
    byte m_irq { 0 };
};
