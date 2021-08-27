/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/IO.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Memory/PhysicalPage.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/Random.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

class AsyncBlockDeviceRequest;
class StorageDevice;

// Note: We work with SCSI addresses as this is the standard addressing
// in many other Unix systems that utilize SCSI as an abstraction layer.
// For ATA devices, the logical_unit_number member is ignored,
// For ATAPI devices, the logical_unit_number member can describe a sub-index of
// the device (like a function of a PCI device).
// For both ATA and ATAPI, on IDE controllers, port represents Primary or Secondary,
// and subport represents Master or Slave. For AHCI HBAs, port is the actual port on the
// the HBA, subport is related to a device being connected via a port multiplier.
struct StorageAddress {
    u8 port;
    u8 subport;
    u8 logical_unit_number;
};

class StorageController : public RefCounted<StorageController> {
    AK_MAKE_ETERNAL

public:
    virtual ~StorageController() = default;

    virtual RefPtr<StorageDevice> search_for_device(StorageAddress) const = 0;
    virtual RefPtr<StorageDevice> device_by_index(u32) const = 0;
    virtual size_t devices_count() const = 0;
    virtual Optional<size_t> max_devices_count() const = 0;

protected:
    virtual bool reset() = 0;
    virtual bool shutdown() = 0;

    virtual void complete_current_request(AsyncDeviceRequest::RequestResult) = 0;
};
}
