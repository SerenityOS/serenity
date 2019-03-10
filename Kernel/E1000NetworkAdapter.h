#pragma once

#include <Kernel/NetworkAdapter.h>
#include <Kernel/PCI.h>
#include <Kernel/MemoryManager.h>
#include <Kernel/IRQHandler.h>
#include <AK/OwnPtr.h>

class E1000NetworkAdapter final : public NetworkAdapter, public IRQHandler {
public:
    static OwnPtr<E1000NetworkAdapter> autodetect();

    E1000NetworkAdapter(PCI::Address, byte irq);
    virtual ~E1000NetworkAdapter() override;

private:
    virtual void handle_irq() override;
    virtual const char* class_name() const override { return "E1000NetworkAdapter"; }

    void detect_eeprom();
    dword read_eeprom(byte address);
    void read_mac_address();

    void write_command(word address, dword);
    dword read_command(word address);

    void out8(word address, byte);
    void out16(word address, word);
    void out32(word address, dword);
    byte in8(word address);
    word in16(word address);
    dword in32(word address);

    PCI::Address m_pci_address;
    word m_io_base { 0 };
    PhysicalAddress m_mmio_base;
    byte m_interrupt_line { 0 };
    bool m_has_eeprom { false };
    bool m_use_mmio { false };
};
