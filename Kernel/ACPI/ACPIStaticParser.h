#pragma once

#include <ACPI/ACPIParser.h>
#include <AK/OwnPtr.h>

class ACPIStaticParser : ACPIParser {
public:
    static void initialize(ACPI_RAW::RSDPDescriptor20& rsdp);
    static void initialize_without_rsdp();
    static bool is_initialized();

    virtual ACPI_RAW::SDTHeader* find_table(const char* sig) override;
    virtual void do_acpi_reboot() override;
    virtual void do_acpi_shutdown() override;
    virtual bool is_operable() override { return m_operable; }

protected:
    ACPIStaticParser();
    explicit ACPIStaticParser(ACPI_RAW::RSDPDescriptor20&);

private:
    void locate_static_data();
    void locate_all_aml_tables();
    void locate_main_system_description_table();
    void initialize_main_system_description_table();
    size_t get_table_size(ACPI_RAW::SDTHeader&);
    u8 get_table_revision(ACPI_RAW::SDTHeader&);
    void init_fadt();
    ACPI_RAW::RSDPDescriptor20* search_rsdp();

    // Early pointers that are needed really for initializtion only...
    ACPI_RAW::RSDPDescriptor20* m_rsdp;
    ACPI_RAW::SDTHeader* m_main_system_description_table;

    OwnPtr<ACPI::MainSystemDescriptionTable> m_main_sdt;
    OwnPtr<ACPI::FixedACPIData> m_fadt;

    Vector<ACPI_RAW::SDTHeader*> m_aml_tables_ptrs;
    bool m_xsdt_supported;
};