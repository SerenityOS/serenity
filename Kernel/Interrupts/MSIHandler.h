/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/PCI/Definitions.h>

namespace Kernel {

class MSIHandler final : public GenericInterruptHandler {
public:
    virtual ~MSIHandler();

    virtual void handle_interrupt(RegisterState&) override { }

    void enable_irq();
    void disable_irq();

    virtual bool eoi() override;

    virtual size_t sharing_devices_count() const override { return 0; }
    virtual bool is_shared_handler() const override { return false; }
    virtual bool is_sharing_with_others() const override { return m_shared_with_others; }

protected:
    void change_irq_number(u8 irq);
    explicit MSIHandler(PCI::Address);

private:
    bool m_shared_with_others;
    bool m_enabled;
};
}
