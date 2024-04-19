/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Definitions.h>

namespace Kernel::USB::EHCI {

// https://www.intel.com/content/www/us/en/products/docs/io/universal-serial-bus/ehci-specification-for-usb.html

// 2.1.3 USBBASE - Register Space Base Address Register
// Address Offset: 10−13h   => BAR0
constexpr auto SpaceBaseAddressRegister = PCI::HeaderType0BaseRegister::BAR0;

union BaseRegister {
    enum class MappingSupport64Bit : u32 {
        No = 0b00,
        Yes = 0b10
    };
    struct {

        u32 : 1;
        MappingSupport64Bit mapping_support : 2;
        u32 : 5;
        u32 base_address_hi : 24;
    };
    u32 raw;
};
static_assert(AssertSize<BaseRegister, 32 / 8>());

// 2.1.4 SBRN - Serial Bus Release Number Register
// Address Offset: 60h
// Attribute: RO
// Size: 8 bits
// Note: Assuming the layout based on the default value of 0x20 representing USB 2.0
struct SBRN {
    u8 minor : 4;
    u8 major : 4;
};
static_assert(AssertSize<SBRN, 8 / 8>());

// 2.1.7 USBLEGSUP - USB Legacy Support Extended Capability
// Offset: EECP + 00h
// Attribute RO, R/W
// Size: 32 bits
struct LegacySupport {
    u8 capability : 8;
    u8 next_ehci_extended_capabilites_pointer : 8;
    // These should be u8's as we want individual accesses to these bits,
    // if we decide to cooperative ownership of the Controller with the BIOS
    u8 HC_BIOS_owned_semaphore : 1;
    u8 : 7;
    u8 HC_OS_owned_semaphore : 1;
    u8 : 7;
};
static_assert(AssertSize<LegacySupport, 32 / 8>());

// 2.1.8 USBLEGCTLSTS - USB Legacy Support Control/Status
// Offset: EECP + 04h
// Default Value 00000000h
// Size: 32 bits
struct LegacySupportControl {
    u32 smi_enable : 1;
    u32 smi_on_usb_error_enable : 1;
    u32 smi_on_port_change_enable : 1;
    u32 smi_on_frame_list_rollover_enable : 1;
    u32 smi_on_sys_error_enable : 1;
    u32 smi_on_async_advance_enable : 1;
    u32 : 7;
    u32 smi_on_os_ownership_enable : 1;
    u32 smi_on_pci_command_enable : 1;
    u32 smi_on_bar_enable : 1;

    u32 smi_on_usb_complete : 1;
    u32 smi_on_usb_error : 1;
    u32 smi_on_port_change_detected : 1;
    u32 smi_on_frame_list_rollover : 1;
    u32 smi_on_host_system_error : 1;
    u32 smi_on_async_advance : 1;
    u32 : 7;
    u32 smi_on_os_ownership_change : 1;
    u32 smi_on_pci_command : 1;
    u32 smi_on_bar : 1;
};
static_assert(AssertSize<LegacySupportControl, 32 / 8>());

// 2.2 Host Controller Capability Registers
struct CapabilityRegisters {
    // 2.2.1 CAPLENGTH - Capability Registers Length
    u8 capability_length; // Offset to beginning to Operational Register
    u8 : 8;

    // 2.2.2 HCIVERSION - Host Controller Interface Version Number
    struct InterfaceVersion {
        u8 minor;
        u8 major;
    } interface_version;
    static_assert(AssertSize<InterfaceVersion, 16 / 8>());

    // 2.2.3 HCSPARAMS - Structural Parameters
    struct StructuralParameters {
        u32 n_ports : 4;
        u32 port_power_control : 1; // N_PPC
        u32 : 2;
        u32 port_routing_rules : 1;
        u32 n_ports_per_companion_controller : 4; // N_PCC
        u32 n_companion_controllers : 4;          // N_CC
        u32 port_indicators : 1;                  // P_INDICATOR
        u32 : 3;
        u32 debug_port_number : 4;
        u32 : 8;
    } structural_parameters;
    static_assert(AssertSize<StructuralParameters, 32 / 8>());

    // 2.2.4 HCCPARAMS - Capability Parameters
    struct CapabilityParameters {
        u32 addressing_capability_64bit : 1;
        u32 programmable_frame_list_flag : 1;
        u32 asynchronous_schedule_park_capability : 1;
        u32 : 1;
        u32 isochronous_scheduling_threshold : 4;
        u32 ehci_extended_capabilities_pointer : 8; // EECP
        u32 : 16;
    } capability_parameters;
    static_assert(AssertSize<CapabilityParameters, 32 / 8>());

    // 2.2.5 HCSP-PORTROUTE - Companion Port Route Description
    // Note: Technically only 60 bits
    //       Technically a u4[n_ports]
    u32 companion_port_route_description[2];
};
// Table 2-5. Enhanced Host Controller Capability Registers
static_assert(__builtin_offsetof(CapabilityRegisters, capability_length) == 0x00);
static_assert(__builtin_offsetof(CapabilityRegisters, interface_version) == 0x02);
static_assert(__builtin_offsetof(CapabilityRegisters, structural_parameters) == 0x04);
static_assert(__builtin_offsetof(CapabilityRegisters, capability_parameters) == 0x08);
static_assert(__builtin_offsetof(CapabilityRegisters, companion_port_route_description) == 0x0C);

// 2.3 Host Controller Operational Registers
struct OperationalRegisters {
    // 2.3.1 USBCMD - USB Command Register
    // Default Value: 00080000h (00080B00h if Asynchronous Schedule Park Capability is a one)
    union CommandRegister {
        struct {
            u32 run_stop : 1;        // RS
            u32 reset : 1;           // HCRESET
            u32 frame_list_size : 2; // 1024  / N Elements | N < 0b11
            u32 periodic_schedule_enable : 1;
            u32 asynchronous_schedule_enable : 1;
            u32 interrupt_on_async_advance_doorbell : 1;
            u32 light_host_controller_reset : 1;
            u32 asynchronous_schedule_park_mode_count : 2;
            u32 : 1;
            u32 asynchronous_schedule_park_mode_enable : 1;
            u32 : 4;
            u32 interrupt_threshold_control : 8;
            u32 : 8;
        };
        u32 raw;
    } command;
    static_assert(AssertSize<CommandRegister, 32 / 8>());

    // 2.3.2 USBSTS - USB Status Register
    // Default Value: 00001000h
    union StatusRegister {
        // To zero an interrupt use a selective write to raw, as otherwise other
        // interrupt bits might be cleared as well
        const struct {
            u32 interrupt : 1;                  // R/WC
            u32 error_interrupt : 1;            // R/WC
            u32 port_change_detect : 1;         // R/WC
            u32 frame_list_rollover : 1;        // R/WC
            u32 host_system_error : 1;          // R/WC
            u32 interrupt_on_async_advance : 1; // R/WC
            u32 : 6;
            u32 const hc_halted : 1;
            u32 const periodic_schedule_status : 1;
            u32 const asynchronous_schedule_status : 1;
            u32 : 16;
        };
        u32 raw;
    } status;
    static_assert(AssertSize<StatusRegister, 32 / 8>());

    // 2.3.3 USBINTR - USB Interrupt Enable Register
    struct InterruptEnable {
        u32 usb_interrupt_enable : 1;
        u32 usb_error_interrupt_enable : 1;
        u32 port_change_enable : 1;
        u32 frame_list_rollover_enable : 1;
        u32 host_system_error_enable : 1;
        u32 interrupt_on_async_advance_enable : 1;
        u32 : 26;
    } interrupt_enable;
    static_assert(AssertSize<InterruptEnable, 32 / 8>());

    // 2.3.4 FRINDEX - Frame Index Register
    // Note: We use `volatile` to ensure 32 bit writes
    // Note: Only up to 14 bits are actually used, and the last 3 bits must never be `000` or `111`
    u32 volatile frame_index;

    // 2.3.5 CTRLDSSEGMENT - Control Data Structure Segment Register
    // Note: We use `volatile` to ensure 32 bit writes
    // Note: Upper 32 bits of periodic-frame- and asynchronous-list pointers
    u32 volatile segment_selector;

    // 2.3.6 PERIODICLISTBASE - Periodic Frame List Base Address Register
    // Note: We use `volatile` to ensure 32 bit writes
    // Note: Page-aligned addresses only
    u32 volatile frame_list_base_address;
    // 2.3.7 ASYNCLISTADDR - Current Asynchronous List Address Register
    // Note: We use `volatile` to ensure 32 bit writes
    // Note: 32 byte (cache-line) aligned addresses only
    u32 volatile next_asynchronous_list_address;

    u32 _padding[9];

    // 2.3.8 CONFIGFLAG - Configure Flag Register
    u32 configured_flag;

    union PortStatusControl {
        enum class LineStatus : u32 {
            SE0 = 0b00,
            J_State = 0b10,
            K_State = 0b01,
            Undefined = 0b11
        };
        enum class PortIndicatorControl : u32 {
            Off = 0b00,
            Amber = 0b01,
            Green = 0b10,
            Undefined = 0b11
        };
        enum class PortTestControl : u32 {
            NotEnabled = 0b0000,
            J_State = 0b0001,
            K_State = 0b0010,
            SE0_NAK = 0b0011,
            Packet = 0b0100,
            Force_Enable = 0b0101,
        };

        const struct {
            u32 current_connect_status : 1;
            u32 connect_status_change : 1; // R/WC
            u32 port_enable : 1;
            u32 port_enable_change : 1; // R/WC
            u32 over_current_active : 1;
            u32 over_current_change : 1; // R/WC
            u32 force_resume : 1;
            u32 suspend : 1;
            u32 port_reset : 1;
            u32 : 1;
            LineStatus line_status : 2;
            u32 port_power : 1;
            u32 port_owner : 1;
            PortIndicatorControl port_indicator_control : 2;
            PortTestControl port_test_control : 4;
            u32 wake_on_connect_enable : 1;      // WKCNNT_E
            u32 wake_on_disconnect_enable : 1;   // WKDSCNNT_E
            u32 wake_on_over_current_enable : 1; // WKOC_E
            u32 : 9;
        };
        u32 raw;
    } port_status_control[];
    static_assert(AssertSize<PortStatusControl, 32 / 8>());
};
// Table 2-8. Host Controller Operational Registers
static_assert(__builtin_offsetof(OperationalRegisters, command) == 0x00);
static_assert(__builtin_offsetof(OperationalRegisters, status) == 0x04);
static_assert(__builtin_offsetof(OperationalRegisters, interrupt_enable) == 0x08);
static_assert(__builtin_offsetof(OperationalRegisters, frame_index) == 0x0C);
static_assert(__builtin_offsetof(OperationalRegisters, segment_selector) == 0x10);
static_assert(__builtin_offsetof(OperationalRegisters, frame_list_base_address) == 0x14);
static_assert(__builtin_offsetof(OperationalRegisters, next_asynchronous_list_address) == 0x18);
static_assert(__builtin_offsetof(OperationalRegisters, configured_flag) == 0x40);
static_assert(__builtin_offsetof(OperationalRegisters, port_status_control) == 0x44);

}
