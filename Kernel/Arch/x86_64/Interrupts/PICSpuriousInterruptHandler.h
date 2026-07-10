/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Arch/IRQController.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>

namespace Kernel {

class PIC;

class PICSpuriousInterruptHandler final : public GenericInterruptHandler {
public:
    static void initialize(PIC&, InterruptNumber interrupt_number);
    static void initialize_for_disabled_master_pic(PIC&);
    static void initialize_for_disabled_slave_pic(PIC&);
    virtual ~PICSpuriousInterruptHandler();
    virtual bool handle_interrupt() override;

    void register_handler(GenericInterruptHandler&);
    void unregister_handler(GenericInterruptHandler&);

    virtual bool eoi() override;

    virtual size_t sharing_devices_count() const override { return 1; }
    virtual bool is_shared_handler() const override { return false; }

    virtual HandlerType type() const override { return HandlerType::SpuriousInterruptHandler; }
    virtual StringView purpose() const override;
    virtual StringView controller() const override;

    void enable_interrupt_vector_for_disabled_pic();

private:
    void enable_interrupt_vector();
    void disable_interrupt_vector();
    explicit PICSpuriousInterruptHandler(PIC&, InterruptNumber interrupt_number);
    bool m_enabled { false };
    bool m_real_irq { false };
    NonnullLockRefPtr<PIC> m_pic;
    OwnPtr<GenericInterruptHandler> m_real_handler;
};
}
