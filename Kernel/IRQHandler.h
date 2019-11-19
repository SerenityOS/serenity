#pragma once

#include <AK/Types.h>

class IRQHandler {
public:
    virtual ~IRQHandler();
    virtual void handle_irq() { m_interrupted = true; }

    u8 irq_number() const { return m_irq_number; }

    void enable_irq();
    void disable_irq();

    bool wait_for_irq();

    bool was_interrupted() const { return m_interrupted; }

protected:
    explicit IRQHandler(u8 irq);
    void set_interrupted(bool interrupted) { m_interrupted = interrupted; }

private:
    u8 m_irq_number { 0 };
    volatile bool m_interrupted { false };
};
