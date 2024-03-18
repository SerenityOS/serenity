/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Drivers/Driver.h>
#include <Kernel/Devices/Serial/PCI/Serial8250Device.h>

namespace Kernel::PCI {

class PCISerial8250Driver final : public PCI::Driver {
public:
    static void init();

    PCISerial8250Driver()
        : PCI::Driver("PCISerial8250Driver"sv)
    {
    }

    virtual ErrorOr<void> probe(PCI::Device&) override;
    virtual void detach(PCI::Device&) override;
    virtual ClassID class_id() const override { return PCI::ClassID::SimpleCommunication; }
    virtual Span<HardwareIDMatch const> matches() override;

private:
    IntrusiveList<&PCISerial8250Device::m_driver_list_node> m_devices;

    // FIXME: Maybe get this value from a singleton that is related to TTY/SerialDevices?
    Atomic<u64> m_current_device_minor { 68 };
};

struct BoardDefinition {
    PCI::HardwareID device_id;
    StringView name;
    u32 port_count { 0 };
    u32 pci_bar { 0 };
    u32 first_offset { 0 };
    u32 port_size { 0 };
    SerialDevice::Baud baud_rate { SerialDevice::Baud::Baud38400 };
};

static constexpr BoardDefinition board_definitions[] = {
    { { PCI::VendorID::RedHat, 0x0002 }, "QEMU PCI 16550A"sv, 1, 0, 0, 8, SerialDevice::Baud::Baud115200 },
    { { PCI::VendorID::RedHat, 0x0003 }, "QEMU PCI Dual-port 16550A"sv, 2, 0, 0, 8, SerialDevice::Baud::Baud115200 },
    { { PCI::VendorID::RedHat, 0x0004 }, "QEMU PCI Quad-port 16550A"sv, 4, 0, 0, 8, SerialDevice::Baud::Baud115200 },
    { { PCI::VendorID::WCH, 0x2273 }, "WCH CH351"sv, 2, 0, 0, 8, SerialDevice::Baud::Baud115200 },
    { { PCI::VendorID::WCH, 0x3253 }, "WCH CH382 2S"sv, 2, 0, 0xC0, 8, SerialDevice::Baud::Baud115200 }
};

static ErrorOr<BoardDefinition const*> find_board_definition(PCI::Device& pci_device)
{
    for (auto& board_definition : board_definitions) {
        if (board_definition.device_id == pci_device.device_id().hardware_id())
            return &board_definition;
    }
    return Error::from_errno(ENOTSUP);
}

ErrorOr<void> PCISerial8250Driver::probe(PCI::Device& pci_device)
{
    auto const& board_definition = *TRY(find_board_definition(pci_device));

    auto registers_io_window = TRY(IOWindow::create_for_pci_device_bar(pci_device, static_cast<PCI::HeaderType0BaseRegister>(board_definition.pci_bar)));
    auto first_offset_registers_io_window = TRY(registers_io_window->create_from_io_window_with_offset(board_definition.first_offset));

    Vector<NonnullRefPtr<SerialDevice>> devices;
    for (size_t i = 0; i < board_definition.port_count; i++) {
        auto port_registers_io_window = TRY(first_offset_registers_io_window->create_from_io_window_with_offset(board_definition.port_size * i));
        auto serial_device = TRY(SerialDevice::create_with_io_window(move(port_registers_io_window), m_current_device_minor++));
        if (board_definition.baud_rate != SerialDevice::Baud::Baud38400) // non-default baud
            serial_device->set_baud(board_definition.baud_rate);
        devices.append(*serial_device);
    }
    auto device = TRY(PCISerial8250Device::create(move(devices)));
    m_devices.append(*device);
    dmesgln("PCISerial8250: Found {} @ {}", board_definition.name, pci_device.device_id().address());
    return {};
}

void PCISerial8250Driver::detach(PCI::Device&)
{
    TODO();
}

const static HardwareIDMatch __matches[] = {
    // QEMU PCI 16550A
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::RedHat,
            0x0002,
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    // QEMU PCI Dual-port 16550A
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::RedHat,
            0x0003,
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    // QEMU PCI Quad-port 16550A
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::RedHat,
            0x0004,
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    // WCH CH351
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::WCH,
            0x2273,
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },
    // WCH CH382 2S
    {
        .subclass_code = Optional<SubclassCode> {},
        .revision_id = Optional<RevisionID> {},
        .hardware_id = HardwareID {
            PCI::VendorID::WCH,
            0x3253,
        },
        .subsystem_id_match = Optional<HardwareIDMatch::SubsystemIDMatch> {},
        .programming_interface = Optional<ProgrammingInterface> {},
    },

};

Span<HardwareIDMatch const> PCISerial8250Driver::matches()
{
    return __matches;
}

void PCISerial8250Driver::init()
{
    auto driver = MUST(adopt_nonnull_ref_or_enomem(new PCISerial8250Driver()));
    PCI::Access::the().register_driver(driver);
}

PCI_DEVICE_DRIVER(PCISerial8250Driver);
}
