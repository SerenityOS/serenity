/*
 * Copyright (c) 2020-2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/CommandLine.h>
#include <Kernel/FileSystem/SysFSComponent.h>
#include <Kernel/Firmware/ACPI/Definitions.h>
#include <Kernel/Firmware/ACPI/Initialize.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VirtualAddress.h>

namespace Kernel::ACPI {

class ACPISysFSDirectory : public SysFSDirectory {
public:
    static void initialize();

private:
    ACPISysFSDirectory();
};

class ACPISysFSComponent : public SysFSComponent {
public:
    static NonnullRefPtr<ACPISysFSComponent> create(String name, PhysicalAddress, size_t table_size);

    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, OpenFileDescription*) const override;

protected:
    KResultOr<NonnullOwnPtr<KBuffer>> try_to_generate_buffer() const;
    ACPISysFSComponent(String name, PhysicalAddress, size_t table_size);

    PhysicalAddress m_paddr;
    size_t m_length;
};

class Parser final : public IRQHandler {
public:
    static Parser* the();

    static void must_initialize(PhysicalAddress rsdp, PhysicalAddress fadt, u8 irq_number);

    virtual StringView purpose() const override { return "ACPI Parser"; }
    virtual bool handle_irq(const RegisterState&) override;

    Optional<PhysicalAddress> find_table(const StringView& signature);

    void try_acpi_reboot();
    bool can_reboot();
    void try_acpi_shutdown();
    bool can_shutdown() { return false; }

    void enable_aml_parsing();

    PhysicalAddress rsdp() const { return m_rsdp; }
    PhysicalAddress main_system_description_table() const { return m_main_system_description_table; }
    bool is_xsdt_supported() const { return m_xsdt_supported; }

    void enumerate_static_tables(Function<void(const StringView&, PhysicalAddress, size_t)>);

    virtual bool have_8042() const
    {
        return m_x86_specific_flags.keyboard_8042;
    }

    const FADTFlags::HardwareFeatures& hardware_features() const { return m_hardware_flags; }
    const FADTFlags::x86_Specific_Flags& x86_specific_flags() const { return m_x86_specific_flags; }

    ~Parser() {};

private:
    Parser(PhysicalAddress rsdp, PhysicalAddress fadt, u8 irq_number);

    void locate_static_data();
    void locate_main_system_description_table();
    void initialize_main_system_description_table();
    size_t get_table_size(PhysicalAddress);
    u8 get_table_revision(PhysicalAddress);
    void process_fadt_data();

    bool validate_reset_register();
    void access_generic_address(const Structures::GenericAddressStructure&, u32 value);

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
