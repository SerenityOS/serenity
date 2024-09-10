/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Error.h>
#include <Kernel/Bus/USB/USBDevice.h>
#include <Kernel/Bus/USB/USBTransfer.h>

namespace Kernel::USB {

class USBController : public AtomicRefCounted<USBController> {
public:
    virtual ~USBController() = default;

    virtual ErrorOr<void> initialize() = 0;

    virtual ErrorOr<void> reset() = 0;
    virtual ErrorOr<void> stop() = 0;
    virtual ErrorOr<void> start() = 0;

    virtual void cancel_async_transfer(NonnullLockRefPtr<Transfer> transfer) = 0;
    virtual ErrorOr<size_t> submit_control_transfer(Transfer&) = 0;
    virtual ErrorOr<size_t> submit_bulk_transfer(Transfer& transfer) = 0;
    virtual ErrorOr<void> submit_async_interrupt_transfer(NonnullLockRefPtr<Transfer> transfer, u16 ms_interval) = 0;

    virtual ErrorOr<void> reset_pipe(Device&, Pipe&);

    virtual ErrorOr<void> initialize_device(Device&) = 0;

    u32 storage_controller_id() const { return m_storage_controller_id; }

protected:
    USBController();

private:
    // Note: We are pseudo storage controller for the sake of generating LUNs
    //       And do not follow a hardware_relative_controller_id for the controller class "USB",
    //       as we also have to follow the device id and its internal LUN, leaving no room for that
    u32 m_storage_controller_id { 0 };

    IntrusiveListNode<USBController, NonnullLockRefPtr<USBController>> m_controller_list_node;

public:
    using List = IntrusiveList<&USBController::m_controller_list_node>;
};

}
