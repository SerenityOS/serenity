#pragma once

#include <AK/Types.h>

class IRQHandler {
public:
    virtual ~IRQHandler();
    virtual void handle_irq() = 0;

    u8 irq_number() const { return m_irq_number; }

    void enable_irq();
    void disable_irq();

protected:
    explicit IRQHandler(u8 irq);

private:
    u8 m_irq_number { 0 };
};
