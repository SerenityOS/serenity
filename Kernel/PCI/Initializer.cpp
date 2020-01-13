#include <Kernel/ACPI/ACPIParser.h>
#include <Kernel/IO.h>
#include <Kernel/KParams.h>
#include <Kernel/PCI/IOAccess.h>
#include <Kernel/PCI/Initializer.h>
#include <Kernel/PCI/MMIOAccess.h>

static PCI::Initializer* s_pci_initializer;

PCI::Initializer& PCI::Initializer::the()
{
    if (s_pci_initializer == nullptr) {
        s_pci_initializer = new PCI::Initializer();
    }
    return *s_pci_initializer;
}
void PCI::Initializer::initialize_pci_mmio_access(ACPI_RAW::MCFG& mcfg)
{
    PCI::MMIOAccess::initialize(mcfg);
}
void PCI::Initializer::initialize_pci_io_access()
{
    PCI::IOAccess::initialize();
}
void PCI::Initializer::test_and_initialize(bool disable_pci_mmio)
{
    if (disable_pci_mmio) {
        if (test_pci_io()) {
            initialize_pci_io_access();
        } else {
            kprintf("No PCI Bus Access Method Detected, Halt!\n");
            ASSERT_NOT_REACHED(); // NO PCI Access ?!
        }
        return;
    }
    if (test_acpi()) {
        if (test_pci_mmio()) {
            initialize_pci_mmio_access_after_test();
        } else {
            if (test_pci_io()) {
                initialize_pci_io_access();
            } else {
                kprintf("No PCI Bus Access Method Detected, Halt!\n");
                ASSERT_NOT_REACHED(); // NO PCI Access ?!
            }
        }
    } else {
        if (test_pci_io()) {
            initialize_pci_io_access();
        } else {
            kprintf("No PCI Bus Access Method Detected, Halt!\n");
            ASSERT_NOT_REACHED(); // NO PCI Access ?!
        }
    }
}
PCI::Initializer::Initializer()
{
}
bool PCI::Initializer::test_acpi()
{
    if ((KParams::the().has("noacpi")) || !ACPIParser::the().is_operable())
        return false;
    else
        return true;
}

bool PCI::Initializer::test_pci_io()
{
    kprintf("Testing PCI via manual probing... ");
    u32 tmp = 0x80000000;
    IO::out32(PCI_ADDRESS_PORT, tmp);
    tmp = IO::in32(PCI_ADDRESS_PORT);
    if (tmp == 0x80000000) {
        kprintf("PCI IO Supported!\n");
        return true;
    }

    kprintf("PCI IO Not Supported!\n");
    return false;
}

bool PCI::Initializer::test_pci_mmio()
{
    if (ACPIParser::the().find_table("MCFG") != nullptr)
        return true;
    else
        return false;
}

void PCI::Initializer::initialize_pci_mmio_access_after_test()
{
    initialize_pci_mmio_access(*(ACPI_RAW::MCFG*)(ACPIParser::the().find_table("MCFG")));
}

void PCI::Initializer::dismiss()
{
    if (s_pci_initializer == nullptr)
        return;
    kprintf("PCI Subsystem Initializer dismissed.\n");
    delete s_pci_initializer;
    s_pci_initializer = nullptr;
}

PCI::Initializer::~Initializer()
{
}