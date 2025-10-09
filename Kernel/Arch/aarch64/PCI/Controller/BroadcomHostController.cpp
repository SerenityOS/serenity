/*
 * Copyright (c) 2024-2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Delay.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/DeviceTreeHelpers.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>
#include <Kernel/Firmware/DeviceTree/Driver.h>
#include <Kernel/Firmware/DeviceTree/Management.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::PCI {

// This driver requires that the host controller is already initialized.
// On the Pi 5, the firmware initializes it for us. It only asserts PERST# before starting the OS, so we just need to deassert it.
// On the Pi 4, you need to boot with EDK II. The standard Raspberry Pi firmware does not initialize the host controller.

// This host controller is not ECAM-compliant.
// The config space is accessible through a single 4K window that can be mapped to any bus/device/function.
// The b/d/f can be configured by writing an ECAM offset to Registers::config_space_window_address.

struct Registers {
    u8 unknown1[0x400c];

    // Bus base [31:0] @ [31:0]
    u32 bus_window_base_low; // 0x400c

    // Bus base [63:32] @ [31:0]
    u32 bus_window_base_high; // 0x4010

    u8 unknown2[0x4064 - (0x4010 + 4)];

    enum class Control : u32 {
        PERST_N = 1 << 2,
    };
    Control control; // 0x4064

    enum class State : u32 {
        LinkUp = 0b11 << 4,
    };
    State state; // 0x4068

    u8 unknown3[0x4070 - (0x4068 + 4)];

    // The host controller doesn't seem to support 16-bit accesses, so the base and limit are grouped into one field.
    // CPU limit [31:20] @ [31:20]
    // CPU base [31:20] @ [15:4]
    u32 cpu_window_base_low_and_cpu_window_limit_low; // 0x4070

    u8 unknown4[0x4080 - (0x4070 + 4)];

    // CPU base [39:32] @ [7:0]
    u32 cpu_window_base_high; // 0x4080

    // CPU limit [39:32] @ [7:0]
    u32 cpu_window_limit_high; // 0x4084

    u8 unknown5[0x8000 - (0x4084 + 4)];

    u8 config_space_window[0x1000];  // 0x8000
    u32 config_space_window_address; // 0x9000

    u8 unknown6[0x9310 - (0x9000 + 4)]; // 0x9000
};
static_assert(AssertSize<Registers, 0x9310>());

AK_ENUM_BITWISE_OPERATORS(Registers::Control)
AK_ENUM_BITWISE_OPERATORS(Registers::State)

class BroadcomHostController : public HostController {
public:
    enum class Model {
        BCM2711,
        BCM2712,
    };

    static ErrorOr<NonnullOwnPtr<BroadcomHostController>> create(DeviceTree::Device const& device, Model);

private:
    ErrorOr<VirtualAddress> map_config_space_for(BusNumber, DeviceNumber, FunctionNumber);

    virtual void write8_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field, u8 value) override;
    virtual void write16_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field, u16 value) override;
    virtual void write32_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field, u32 value) override;

    virtual u8 read8_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field) override;
    virtual u16 read16_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field) override;
    virtual u32 read32_field_locked(BusNumber, DeviceNumber, FunctionNumber, u32 field) override;

    explicit BroadcomHostController(Model model, PCI::Domain const&, Memory::TypedMapping<Registers volatile>);

    Model m_model { Model::BCM2711 };
    Memory::TypedMapping<Registers volatile> m_registers;
};

ErrorOr<NonnullOwnPtr<BroadcomHostController>> BroadcomHostController::create(DeviceTree::Device const& device, Model model)
{
    auto domain = TRY(determine_pci_domain_for_devicetree_node(device.node(), device.node_name()));
    auto registers_resource = TRY(device.get_resource(0));

    if (registers_resource.size < sizeof(Registers))
        return ERANGE;

    auto const* parent_node = device.node().parent();
    if (parent_node == nullptr)
        return EINVAL;

    auto registers = TRY(Memory::map_typed_writable<Registers volatile>(registers_resource.paddr));

    for (auto range : TRY(device.node().ranges())) {
        auto pci_address = TRY(range.child_bus_address().as<OpenFirmwareAddress>());
        auto cpu_base = TRY(TRY(parent_node->translate_child_bus_address_to_root_address(range.parent_bus_address())).as_flatptr());
        auto range_size = TRY(range.length().as_size_t());

        // FIXME: Configure the 64-bit window.
        if (pci_address.space_type != OpenFirmwareAddress::SpaceType::Memory32BitSpace)
            continue;

        auto bus_base = pci_address.io_or_memory_space_address;
        auto cpu_limit = cpu_base + range_size;

        // Configure the CPU -> PCIe window. This determines how CPU addresses are mapped to PCIe addresses.

        registers->bus_window_base_low = bus_base & 0xffff'ffff;
        registers->bus_window_base_high = bus_base >> 32;

        registers->cpu_window_base_low_and_cpu_window_limit_low = (cpu_limit & 0xfff0'0000) | ((cpu_base & 0xfff0'0000) >> 16);
        registers->cpu_window_base_high = cpu_base >> 32;
        registers->cpu_window_limit_high = cpu_limit >> 32;

        // FIXME: Are we guaranteed to only have one 32-bit window?
        break;
    }

    if (model == Model::BCM2712) {
        // Deassert PERST# to initialize the link.
        registers->control |= Registers::Control::PERST_N;

        microseconds_delay(100'000);
    } else if (model == Model::BCM2711) {
        // We expect that PERST# is already deasserted on the Pi 4.
    }

    // Check the link state.
    if ((registers->state & Registers::State::LinkUp) != Registers::State::LinkUp) {
        dbgln("{}: Link down", device.node_name());

        if (model == Model::BCM2712) {
            // We failed to initialize the link; assert PERST# again.
            registers->control &= ~Registers::Control::PERST_N;
        }
        return EIO;
    }

    dbgln("{}: Link up", device.node_name());

    return TRY(adopt_nonnull_own_or_enomem(new (nothrow) BroadcomHostController(model, domain, move(registers))));
}

BroadcomHostController::BroadcomHostController(Model model, PCI::Domain const& domain, Memory::TypedMapping<Registers volatile> registers)
    : HostController(domain)
    , m_model(model)
    , m_registers(move(registers))
{
}

ErrorOr<VirtualAddress> BroadcomHostController::map_config_space_for(BusNumber bus, DeviceNumber device, FunctionNumber function)
{
    if (bus == 0) {
        if (device != 0 || function != 0)
            return EINVAL;

        return m_registers.base_address();
    }

    // Accessing any other device on bus 1 causes SError interrupts on the Pi 4.
    if (m_model == Model::BCM2711 && bus == 1 && device != 0)
        return EINVAL;

    u32 const address = (bus.value() << 20) | (device.value() << 15) | (function.value() << 12);
    m_registers->config_space_window_address = address;

    return VirtualAddress { bit_cast<FlatPtr>(&m_registers->config_space_window) };
}

void BroadcomHostController::write8_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u8 value)
{
    VERIFY(m_access_lock.is_locked());

    auto vaddr_or_error = map_config_space_for(bus, device, function);
    if (vaddr_or_error.is_error())
        return;

    *reinterpret_cast<u8 volatile*>(vaddr_or_error.release_value().offset(field).as_ptr()) = value;
}

void BroadcomHostController::write16_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u16 value)
{
    VERIFY(m_access_lock.is_locked());
    VERIFY(field % sizeof(u16) == 0);

    auto vaddr_or_error = map_config_space_for(bus, device, function);
    if (vaddr_or_error.is_error())
        return;

    *reinterpret_cast<u16 volatile*>(vaddr_or_error.release_value().offset(field).as_ptr()) = value;
}

void BroadcomHostController::write32_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field, u32 value)
{
    VERIFY(m_access_lock.is_locked());
    VERIFY(field % sizeof(u32) == 0);

    auto vaddr_or_error = map_config_space_for(bus, device, function);
    if (vaddr_or_error.is_error())
        return;

    *reinterpret_cast<u32 volatile*>(vaddr_or_error.release_value().offset(field).as_ptr()) = value;
}

u8 BroadcomHostController::read8_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    VERIFY(m_access_lock.is_locked());

    auto vaddr_or_error = map_config_space_for(bus, device, function);
    if (vaddr_or_error.is_error())
        return 0xff;

    return *reinterpret_cast<u8 volatile*>(vaddr_or_error.release_value().offset(field).as_ptr());
}

u16 BroadcomHostController::read16_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    VERIFY(m_access_lock.is_locked());
    VERIFY(field % sizeof(u16) == 0);

    auto vaddr_or_error = map_config_space_for(bus, device, function);
    if (vaddr_or_error.is_error())
        return 0xffff;

    return *reinterpret_cast<u16 volatile*>(vaddr_or_error.release_value().offset(field).as_ptr());
}

u32 BroadcomHostController::read32_field_locked(BusNumber bus, DeviceNumber device, FunctionNumber function, u32 field)
{
    VERIFY(m_access_lock.is_locked());
    VERIFY(field % sizeof(u32) == 0);

    auto vaddr_or_error = map_config_space_for(bus, device, function);
    if (vaddr_or_error.is_error())
        return 0xffff'ffff;

    return *reinterpret_cast<u32 volatile*>(vaddr_or_error.release_value().offset(field).as_ptr());
}

static constinit Array const compatibles_array = {
    "brcm,bcm2711-pcie"sv,
    "brcm,bcm2712-pcie"sv,
};

DEVICETREE_DRIVER(BroadcomPCIeHostControllerDriver, compatibles_array);

// https://www.kernel.org/doc/Documentation/devicetree/bindings/pci/brcm%2Cstb-pcie.yaml
ErrorOr<void> BroadcomPCIeHostControllerDriver::probe(DeviceTree::Device const& device, StringView compatible) const
{
    if (kernel_command_line().is_pci_disabled())
        return {};

    BroadcomHostController::Model model;
    if (compatible == "brcm,bcm2711-pcie"sv)
        model = BroadcomHostController::Model::BCM2711;
    else if (compatible == "brcm,bcm2712-pcie"sv)
        model = BroadcomHostController::Model::BCM2712;
    else
        VERIFY_NOT_REACHED();

    auto host_controller = TRY(BroadcomHostController::create(device, model));

    TRY(configure_devicetree_host_controller(*host_controller, device.node(), device.node_name()));
    Access::the().add_host_controller(move(host_controller));

    return {};
}

}
