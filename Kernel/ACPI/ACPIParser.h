#pragma once

#include <AK/Types.h>
#include <Kernel/ACPI/Definitions.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/VM/PhysicalAddress.h>
#include <Kernel/VM/Region.h>
#include <Kernel/VM/VirtualAddress.h>

class ACPIParser {
public:
    static ACPIParser& the();

    static bool is_initialized();
    static void initialize_limited();
    virtual ACPI_RAW::SDTHeader* find_table(const char* sig);

    virtual void do_acpi_reboot();
    virtual void do_acpi_shutdown();

    virtual void enable_aml_interpretation();
    virtual void enable_aml_interpretation(File&);
    virtual void enable_aml_interpretation(u8*, u32);
    virtual void disable_aml_interpretation();
    virtual bool is_operable();

protected:
    explicit ACPIParser(bool usable);
    bool m_operable;

    virtual void mmap(VirtualAddress, PhysicalAddress, u32);
    virtual void mmap_region(Region&, PhysicalAddress);
};