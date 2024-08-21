/*
 * Copyright (c) 2020-2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/FileSystem/SysFS/Component.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Firmware/Directory.h>
#include <Kernel/Firmware/ACPI/Definitions.h>
#include <Kernel/Firmware/ACPI/Initialize.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Memory/VirtualAddress.h>

namespace Kernel::ACPI {

class ACPISysFSDirectory : public SysFSDirectory {
public:
    virtual StringView name() const override { return "acpi"sv; }
    static NonnullLockRefPtr<ACPISysFSDirectory> must_create(SysFSFirmwareDirectory& firmware_directory);

private:
    void find_tables_and_register_them_as_components();
    explicit ACPISysFSDirectory(SysFSFirmwareDirectory& firmware_directory);
};

class ACPISysFSComponent : public SysFSComponent {
public:
    static NonnullLockRefPtr<ACPISysFSComponent> create(StringView name, PhysicalAddress, size_t table_size);
    virtual StringView name() const override { return m_table_name->view(); }
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const override;

    virtual size_t size() const override final { return m_length; }

protected:
    ErrorOr<NonnullOwnPtr<KBuffer>> try_to_generate_buffer() const;
    ACPISysFSComponent(NonnullOwnPtr<KString> table_name, PhysicalAddress, size_t table_size);

    PhysicalAddress m_paddr;
    size_t m_length { 0 };
    NonnullOwnPtr<KString> m_table_name;
};

class Parser final : public IRQHandler {
public:
    static Parser* the();

    static void must_initialize(PhysicalAddress rsdp, PhysicalAddress fadt, u8 irq_number);

    virtual StringView purpose() const override { return "ACPI Parser"sv; }
    virtual bool handle_irq() override;

    Optional<PhysicalAddress> find_table(StringView signature);

    void try_acpi_reboot();
    bool can_reboot();
    void try_acpi_shutdown();
    bool can_shutdown() { return false; }

    void enable_aml_parsing();

    PhysicalAddress rsdp() const { return m_rsdp; }
    PhysicalAddress main_system_description_table() const { return m_main_system_description_table; }
    bool is_xsdt_supported() const { return m_xsdt_supported; }

    void enumerate_static_tables(Function<void(StringView, PhysicalAddress, size_t)>);

    virtual bool have_8042() const
    {
        return m_x86_specific_flags.keyboard_8042;
    }

    FADTFlags::HardwareFeatures const& hardware_features() const { return m_hardware_flags; }
    FADTFlags::x86_Specific_Flags const& x86_specific_flags() const { return m_x86_specific_flags; }

    ~Parser() = default;

private:
    Parser(PhysicalAddress rsdp, PhysicalAddress fadt, u8 irq_number);

    void locate_static_data();
    void locate_main_system_description_table();
    void initialize_main_system_description_table();
    size_t get_table_size(PhysicalAddress);
    u8 get_table_revision(PhysicalAddress);
    void process_fadt_data();
    void process_dsdt();

    bool validate_reset_register(Memory::TypedMapping<Structures::FADT> const&);
    void access_generic_address(Structures::GenericAddressStructure const&, u32 value);

    PhysicalAddress m_rsdp;
    PhysicalAddress m_main_system_description_table;

    Vector<PhysicalAddress> m_sdt_pointers;
    PhysicalAddress m_fadt;

    bool m_xsdt_supported { false };
    bool m_can_process_bytecode { false };
    FADTFlags::HardwareFeatures m_hardware_flags;
    FADTFlags::x86_Specific_Flags m_x86_specific_flags;
};

}
