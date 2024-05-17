/*
 * Copyright (c) 2024, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullOwnPtr.h>
#include <Kernel/Bus/USB/USBHub.h>
#include <Kernel/Bus/USB/USBTransfer.h>
#include <Kernel/Library/NonnullLockRefPtr.h>

namespace Kernel::USB {

class xHCIController;

class xHCIRootHub {
public:
    static ErrorOr<NonnullOwnPtr<xHCIRootHub>> try_create(NonnullLockRefPtr<xHCIController>);

    xHCIRootHub(NonnullLockRefPtr<xHCIController>);
    ~xHCIRootHub() = default;

    ErrorOr<void> setup(Badge<xHCIController>);

    u8 device_address() const { return m_hub->address(); }

    ErrorOr<size_t> handle_control_transfer(Transfer& transfer);

    void check_for_port_updates() { m_hub->check_for_port_updates(); }

    Hub const& hub() const { return *m_hub; }

private:
    NonnullLockRefPtr<xHCIController> m_controller;
    LockRefPtr<Hub> m_hub;
};

}
