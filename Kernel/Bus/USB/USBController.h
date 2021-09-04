/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <Kernel/API/KResult.h>
#include <Kernel/Bus/USB/USBDevice.h>
#include <Kernel/Bus/USB/USBTransfer.h>

namespace Kernel::USB {

class USBController : public RefCounted<USBController> {
public:
    virtual ~USBController() = default;

    virtual KResult initialize() = 0;

    virtual KResult reset() = 0;
    virtual KResult stop() = 0;
    virtual KResult start() = 0;

    virtual KResultOr<size_t> submit_control_transfer(Transfer&) = 0;

    u8 allocate_address();

private:
    u8 m_next_device_index { 1 };

    IntrusiveListNode<USBController, NonnullRefPtr<USBController>> m_controller_list_node;

public:
    using List = IntrusiveList<USBController, NonnullRefPtr<USBController>, &USBController::m_controller_list_node>;
};

}
