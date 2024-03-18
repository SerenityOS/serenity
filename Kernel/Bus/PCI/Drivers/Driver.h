/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AtomicRefCounted.h>
#include <AK/Error.h>
#include <AK/NonnullRefPtr.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Bus/PCI/IDs.h>

namespace Kernel::PCI {

struct HardwareIDMatch {
    Optional<SubclassCode> subclass_code;
    Optional<RevisionID> revision_id;

    HardwareID hardware_id;
    struct SubsystemIDMatch {
        SubsystemID subsystem_id;
        SubsystemVendorID subsystem_vendor_id;
    };
    Optional<SubsystemIDMatch> subsystem_id_match;
    Optional<ProgrammingInterface> programming_interface;
};

using DriverInitFunction = void (*)();
#define PCI_DEVICE_DRIVER(driver_name) \
    DriverInitFunction driver_init_function_ptr_##driver_name __attribute__((section(".driver_init"), used)) = &driver_name::init

class Driver : public AtomicRefCounted<Driver> {
    friend class Access;
    friend struct ClassedDriverDeviceLists;

public:
    Driver(StringView name)
        : m_driver_name(name)
    {
    }

    virtual ~Driver() = default;

    virtual ErrorOr<void> probe(PCI::Device&) = 0;
    virtual void detach(PCI::Device&) = 0;
    virtual ClassID class_id() const = 0;
    virtual Span<HardwareIDMatch const> matches() = 0;
    StringView name() const { return m_driver_name; }

protected:
    StringView const m_driver_name;

    IntrusiveListNode<Driver, NonnullRefPtr<Driver>> m_list_node;
    IntrusiveListNode<Driver> m_classed_list_node;
};

}
