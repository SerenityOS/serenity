#include "IRQHandler.h"
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Arch/i386/PIC.h>
#include "Thread.h"

IRQHandler::IRQHandler(u8 irq)
    : m_irq_number(irq)
{
    register_irq_handler(m_irq_number, *this);
}

IRQHandler::~IRQHandler()
{
    unregister_irq_handler(m_irq_number, *this);
}

void IRQHandler::enable_irq()
{
    PIC::enable(m_irq_number);
}

void IRQHandler::disable_irq()
{
    PIC::disable(m_irq_number);
}

bool IRQHandler::wait_for_irq()
{
    // Wait for IRQ, uninterruptibly. That means that even if we do
    // get unterrupted, we continue waiting instead of returning to
    // the caller. However, we still want to be "killable" when we
    // actually get killed, not just sent a harmless signal.
    while (!m_interrupted) {
        auto block_result = current->block<Thread::WaitForIRQBlocker>(*this);
        if (block_result == Thread::BlockResult::Interrupted && current->should_die())
            return false;
    }
    return true;
}
