#pragma once

#include <AK/Types.h>

namespace PIC {

void enable(BYTE number);
void disable(BYTE number);
void eoi(BYTE number);
void initialize();
word getISR();
word get_irr();

}

class IRQHandlerScope {
public:
    explicit IRQHandlerScope(BYTE irq) : m_irq(irq) { }
    ~IRQHandlerScope() { PIC::eoi(m_irq); }

private:
    BYTE m_irq { 0 };
};
