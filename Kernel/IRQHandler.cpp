#include "IRQHandler.h"
#include "i386.h"
#include "PIC.h"

IRQHandler::IRQHandler(byte irq)
    : m_irqNumber(irq)
{
    registerIRQHandler(m_irqNumber, *this);
}

IRQHandler::~IRQHandler()
{
    unregisterIRQHandler(m_irqNumber, *this);
}

void IRQHandler::enableIRQ()
{
    PIC::enable(m_irqNumber);
}

void IRQHandler::disableIRQ()
{
    PIC::disable(m_irqNumber);
}

