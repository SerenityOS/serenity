#pragma once

namespace PIC {

void enable(BYTE number);
void disable(BYTE number);
void eoi(BYTE number);
void initialize();

}

class IRQHandlerScope {
public:
    explicit IRQHandlerScope(BYTE irq) : m_irq(irq) { }
    ~IRQHandlerScope() { PIC::eoi(m_irq); }

private:
    BYTE m_irq { 0 };
};
