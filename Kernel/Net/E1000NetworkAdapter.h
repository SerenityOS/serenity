#pragma once

#include <AK/OwnPtr.h>
#include <Kernel/IRQHandler.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/PCI/Access.h>

class E1000NetworkAdapter final : public NetworkAdapter
    , public IRQHandler {
public:
    static OwnPtr<E1000NetworkAdapter> autodetect();

    E1000NetworkAdapter(PCI::Address, u8 irq);
    virtual ~E1000NetworkAdapter() override;

    virtual void send_raw(const u8*, int) override;
    virtual bool link_up() override;

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
    u32 read_eeprom(u8 address);
    void read_mac_address();

    void write_command(u16 address, u32);
    u32 read_command(u16 address);

    void initialize_rx_descriptors();
    void initialize_tx_descriptors();

    void out8(u16 address, u8);
    void out16(u16 address, u16);
    void out32(u16 address, u32);
    u8 in8(u16 address);
    u16 in16(u16 address);
    u32 in32(u16 address);

    void receive();

    PCI::Address m_pci_address;
    u16 m_io_base { 0 };
    PhysicalAddress m_mmio_base;
    u8 m_interrupt_line { 0 };
    bool m_has_eeprom { false };
    bool m_use_mmio { false };

    static const int number_of_rx_descriptors = 32;
    static const int number_of_tx_descriptors = 8;

    e1000_rx_desc* m_rx_descriptors;
    e1000_tx_desc* m_tx_descriptors;

    WaitQueue m_wait_queue;
};
