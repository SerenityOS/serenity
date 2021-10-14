/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/Types.h>
#include <Kernel/Arch/RegisterState.h>

namespace Kernel {

enum class HandlerType : u8 {
    IRQHandler = 1,
    SharedIRQHandler = 2,
    UnhandledInterruptHandler = 3,
    SpuriousInterruptHandler = 4
};

class GenericInterruptHandler {
public:
    static GenericInterruptHandler& from(u8 interrupt_number);
    virtual ~GenericInterruptHandler()
    {
        VERIFY(!m_registered);
    }
    // Note: this method returns boolean value, to indicate if the handler handled
    // the interrupt or not. This is useful for shared handlers mostly.
    virtual bool handle_interrupt(const RegisterState& regs) = 0;

    void will_be_destroyed();
    bool is_registered() const { return m_registered; }
    void register_interrupt_handler();
    void unregister_interrupt_handler();

    u8 interrupt_number() const { return m_interrupt_number; }

    size_t get_invoking_count() const { return m_invoking_count; }

    virtual size_t sharing_devices_count() const = 0;
    virtual bool is_shared_handler() const = 0;
    virtual bool is_sharing_with_others() const = 0;

    virtual HandlerType type() const = 0;
    virtual StringView purpose() const = 0;
    virtual StringView controller() const = 0;

    virtual bool eoi() = 0;
    ALWAYS_INLINE void increment_invoking_counter()
    {
        m_invoking_count++;
    }

protected:
    void change_interrupt_number(u8 number);
    GenericInterruptHandler(u8 interrupt_number, bool disable_remap = false);

    void disable_remap() { m_disable_remap = true; }

private:
    Atomic<u32, AK::MemoryOrder::memory_order_relaxed> m_invoking_count { 0 };
    u8 m_interrupt_number { 0 };
    bool m_disable_remap { false };
    bool m_registered { false };

    IntrusiveListNode<GenericInterruptHandler> m_list_node;

public:
    using List = IntrusiveList<&GenericInterruptHandler::m_list_node>;
};
}
