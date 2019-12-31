#pragma once

#include <AK/OwnPtr.h>
#include <Kernel/IRQHandler.h>
#include <Kernel/Net/NetworkAdapter.h>
#include <Kernel/PCI/Access.h>

#define RTL8139_TX_BUFFER_COUNT 4

class RTL8139NetworkAdapter final : public NetworkAdapter
    , public IRQHandler {
public:
    static OwnPtr<RTL8139NetworkAdapter> autodetect();

    RTL8139NetworkAdapter(PCI::Address, u8 irq);
    virtual ~RTL8139NetworkAdapter() override;

    virtual void send_raw(const u8*, int) override;
    virtual bool link_up() override { return m_link_up; }

private:
    virtual void handle_irq() override;
    virtual const char* class_name() const override { return "RTL8139NetworkAdapter"; }

    void reset();
    void read_mac_address();

    void receive();

    void out8(u16 address, u8 data);
    void out16(u16 address, u16 data);
    void out32(u16 address, u32 data);
    u8 in8(u16 address);
    u16 in16(u16 address);
    u32 in32(u16 address);

    PCI::Address m_pci_address;
    u16 m_io_base { 0 };
    u8 m_interrupt_line { 0 };
    u32 m_rx_buffer_addr { 0 };
    u16 m_rx_buffer_offset { 0 };
    u32 m_tx_buffer_addr[RTL8139_TX_BUFFER_COUNT];
    u8 m_tx_next_buffer { 0 };
    u32 m_packet_buffer { 0 };
    bool m_link_up { false };
};
