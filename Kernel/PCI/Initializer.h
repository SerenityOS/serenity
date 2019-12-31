#pragma once

#include <AK/Types.h>
#include <Kernel/ACPI/Definitions.h>
#include <Kernel/PCI/Definitions.h>

class PCI::Initializer {
public:
    static PCI::Initializer& the();
    void initialize_pci_mmio_access(ACPI_RAW::MCFG& mcfg);
    void initialize_pci_io_access();
    void test_and_initialize(bool disable_pci_mmio, bool pci_force_probing);
    static void dismiss();

private:
    ~Initializer();
    Initializer();
    bool test_acpi();
    bool test_pci_io(bool pci_force_probing);
    bool test_pci_mmio();
    void initialize_pci_mmio_access_after_test();
};