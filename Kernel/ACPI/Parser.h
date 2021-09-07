/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/ACPI/Definitions.h>
#include <Kernel/ACPI/Initialize.h>
#include <Kernel/FileSystem/SysFSComponent.h>
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

    virtual KResultOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer&, FileDescription*) const override;

protected:
    OwnPtr<KBuffer> try_to_generate_buffer() const;
    ACPISysFSComponent(String name, PhysicalAddress, size_t table_size);

    PhysicalAddress m_paddr;
    size_t m_length;
};

class Parser {
public:
    static Parser* the();

    template<typename ParserType>
    static void initialize(PhysicalAddress rsdp)
    {
        set_the(*new ParserType(rsdp));
    }

    virtual Optional<PhysicalAddress> find_table(const StringView& signature);

    virtual void try_acpi_reboot();
    virtual bool can_reboot();
    virtual void try_acpi_shutdown();
    virtual bool can_shutdown() { return false; }

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

    virtual void enable_aml_interpretation();
    virtual void enable_aml_interpretation(File&);
    virtual void enable_aml_interpretation(u8*, u32);
    virtual void disable_aml_interpretation();

protected:
    explicit Parser(PhysicalAddress rsdp);
    virtual ~Parser() = default;

private:
    static void set_the(Parser&);

    void locate_static_data();
    void locate_main_system_description_table();
    void initialize_main_system_description_table();
    size_t get_table_size(PhysicalAddress);
    u8 get_table_revision(PhysicalAddress);
    void init_fadt();
    void init_facs();

    bool validate_reset_register();
    void access_generic_address(const Structures::GenericAddressStructure&, u32 value);

    PhysicalAddress m_rsdp;
    PhysicalAddress m_main_system_description_table;

    Vector<PhysicalAddress> m_sdt_pointers;
    PhysicalAddress m_fadt;
    PhysicalAddress m_facs;

    bool m_xsdt_supported { false };
    FADTFlags::HardwareFeatures m_hardware_flags;
    FADTFlags::x86_Specific_Flags m_x86_specific_flags;
};

}
