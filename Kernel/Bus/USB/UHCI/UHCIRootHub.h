/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
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

class UHCIController;

class UHCIRootHub {
public:
    static ErrorOr<NonnullOwnPtr<UHCIRootHub>> try_create(NonnullLockRefPtr<UHCIController>);

    UHCIRootHub(NonnullLockRefPtr<UHCIController>);
    ~UHCIRootHub() = default;

    ErrorOr<void> setup(Badge<UHCIController>);

    u8 device_address() const { return m_hub->address(); }

    ErrorOr<size_t> handle_control_transfer(Transfer& transfer);

    void check_for_port_updates() { m_hub->check_for_port_updates(); }

private:
    NonnullLockRefPtr<UHCIController> m_uhci_controller;
    LockRefPtr<Hub> m_hub;
};

}
