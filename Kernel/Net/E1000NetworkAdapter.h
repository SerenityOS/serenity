#pragma once

#include <AK/OwnPtr.h>
#include <Kernel/IRQHandler.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/PCI.h>
#include <Kernel/VM/MemoryManager.h>

class E1000NetworkAdapter final : public NetworkAdapter
    , public IRQHandler {
public:
    static E1000NetworkAdapter* the();

    static OwnPtr<E1000NetworkAdapter> autodetect();

    E1000NetworkAdapter(PCI::Address, byte irq);
    virtual ~E1000NetworkAdapter() override;

    virtual void send_raw(const byte*, int) override;

private:
    virtual void handle_irq() override;
    virtual const char* class_name() const override { return "E1000NetworkAdapter"; }

    struct [[gnu::packed]] e1000_rx_desc
    {
        volatile uint64_t addr { 0 };
        volatile uint16_t length { 0 };
        volatile uint16_t checksum { 0 };
        volatile uint8_t status { 0 };
        volatile uint8_t errors { 0 };
        volatile uint16_t special { 0 };
    };

    struct [[gnu::packed]] e1000_tx_desc
    {
        volatile uint64_t addr { 0 };
        volatile uint16_t length { 0 };
        volatile uint8_t cso { 0 };
        volatile uint8_t cmd { 0 };
        volatile uint8_t status { 0 };
        volatile uint8_t css { 0 };
        volatile uint16_t special { 0 };
    };

    void detect_eeprom();
    dword read_eeprom(byte address);
    void read_mac_address();

    void write_command(word address, dword);
    dword read_command(word address);

    void initialize_rx_descriptors();
    void initialize_tx_descriptors();

    void out8(word address, byte);
    void out16(word address, word);
    void out32(word address, dword);
    byte in8(word address);
    word in16(word address);
    dword in32(word address);

    void receive();

    PCI::Address m_pci_address;
    word m_io_base { 0 };
    PhysicalAddress m_mmio_base;
    byte m_interrupt_line { 0 };
    bool m_has_eeprom { false };
    bool m_use_mmio { false };

    static const int number_of_rx_descriptors = 32;
    static const int number_of_tx_descriptors = 8;

    e1000_rx_desc* m_rx_descriptors;
    e1000_tx_desc* m_tx_descriptors;
};
