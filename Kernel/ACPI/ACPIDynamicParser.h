#pragma once

#include <AK/RefPtr.h>
#include <Kernel/ACPI/ACPIStaticParser.h>
#include <Kernel/Devices/DiskDevice.h>
#include <Kernel/IRQHandler.h>
#include <Kernel/Lock.h>
#include <Kernel/VM/PhysicalAddress.h>
#include <Kernel/VM/PhysicalPage.h>

class ACPIDynamicParser final : public IRQHandler
    , ACPIStaticParser {
public:
    static void initialize(ACPI_RAW::RSDPDescriptor20& rsdp);
    static void initialize_without_rsdp();

    virtual void enable_aml_interpretation() override;
    virtual void enable_aml_interpretation(File& dsdt_file) override;
    virtual void enable_aml_interpretation(u8* physical_dsdt, u32 dsdt_payload_legnth) override;
    virtual void disable_aml_interpretation() override;
    virtual void do_acpi_shutdown() override;

protected:
    ACPIDynamicParser();
    explicit ACPIDynamicParser(ACPI_RAW::RSDPDescriptor20&);

private:
    void build_namespace();
    // ^IRQHandler
    virtual void handle_irq() override;

    OwnPtr<Region> m_acpi_namespace;
};