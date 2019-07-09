#pragma once

#include <AK/Types.h>

namespace PIC {

void enable(u8 number);
void disable(u8 number);
void eoi(u8 number);
void initialize();
u16 get_isr();
u16 get_irr();

}

class IRQHandlerScope {
public:
    explicit IRQHandlerScope(u8 irq)
        : m_irq(irq)
    {
    }
    ~IRQHandlerScope() { PIC::eoi(m_irq); }

private:
    u8 m_irq { 0 };
};
