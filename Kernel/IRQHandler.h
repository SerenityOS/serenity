#pragma once

#include <AK/Types.h>

class IRQHandler {
public:
    virtual ~IRQHandler();
    virtual void handleIRQ() = 0;

    byte irqNumber() const { return m_irqNumber; }

    void enableIRQ();
    void disableIRQ();

protected:
    explicit IRQHandler(byte irq);

private:
    byte m_irqNumber { 0 };
};

