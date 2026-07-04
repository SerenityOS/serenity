/*
 * Copyright (c) 2025-2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/MemoryFences.h>
#include <Kernel/Net/Cadence/GEM.h>
#include <Kernel/Net/Cadence/GEMBufferDescriptors.h>
#include <Kernel/Net/Cadence/GEMRegisters.h>

namespace Kernel {

// FIXME: This likely isn't always correct. Alternatively, we could automatically detect the correct PHY address
//        by reading some register(s) (like the PHY Identifier in register 2 and 3) for each PHY address
//        and checking if the contents are valid.
//        Or we could just hardcode the correct PHY address for each specific model.
static constexpr size_t PHY_ID = 1;

CadenceGEMNetworkAdapter::~CadenceGEMNetworkAdapter()
{
    if (m_mdio_handling_process) {
        m_mdio_handling_process->die();
        // Block until all threads exited to prevent UAF.
        ErrorOr<siginfo_t> result = siginfo_t {};
        (void)Thread::current()->block<Thread::WaitBlocker>({}, WEXITED, m_mdio_handling_process.release_nonnull(), result);
    }
}

ErrorOr<void> CadenceGEMNetworkAdapter::initialize(Badge<NetworkingManagement>)
{
    VERIFY_NOT_REACHED();
}

i32 CadenceGEMNetworkAdapter::link_speed()
{
    return 1000; // FIXME
}

bool CadenceGEMNetworkAdapter::link_full_duplex()
{
    return true; // FIXME
}

void CadenceGEMNetworkAdapter::send_raw(ReadonlyBytes data)
{
    MutexLocker locker { m_transmit_mutex };

    auto* descriptor = tx_descriptor_entry(m_tx_enqueue_index);

    ErrorOr<void> maybe_error {};
    do {
        maybe_error = m_wait_queue.wait_until([&descriptor]() {
            // Inform the compiler that the contents of DMA regions might have changed,
            // even if it didn't see any writes to them.
            optimizer_fence();
            return descriptor->is_software_owned;
        });
    } while (maybe_error.is_error() && maybe_error.error().code() == EINTR);
    VERIFY(!maybe_error.is_error());

    // Ensure that the software owned check is done before writing the descriptor and buffer data.
    full_memory_fence();

    auto& tx_buffer = m_tx_buffers[m_tx_enqueue_index];
    m_tx_enqueue_index = (m_tx_enqueue_index + 1) % TX_DESCRIPTOR_COUNT;

    VERIFY(data.size() <= TRANSMIT_BUFFER_SIZE);
    memcpy(tx_buffer->vaddr().as_ptr(), data.data(), data.size());

    auto buffer_paddr = tx_buffer->physical_page(0)->paddr().get();

    // Make sure that all unused fields are in a known state (zeroed).
    TxBufferDescriptorEntry new_descriptor = {};
    new_descriptor.is_software_owned = 1;
    new_descriptor.is_last_descriptor = descriptor->is_last_descriptor;
    new_descriptor.buffer_address_63_32 = buffer_paddr >> 32;
    new_descriptor.buffer_address_31_0 = buffer_paddr & 0xffff'ffff;
    new_descriptor.buffer_length = data.size();
    new_descriptor.last_buffer = 1; // We currently don't use scatter-gather transfers, and therefore only need one descriptor.

    *descriptor = new_descriptor;

    // Ensure that the buffer and buffer descriptor writes are visible before giving away ownership.
    store_memory_fence();

    descriptor->is_software_owned = 0;

    // Ensure that us giving away ownership is visible before requesting transmission start.
    store_memory_fence();

    m_registers->network_control |= Registers::NetworkControl::StartTransmission;
}

ErrorOr<void> CadenceGEMNetworkAdapter::initialize()
{
    // "Initialize the Controller"

    // "1. Clear the network control register. Write 0x0 to the gem.network_control register."
    m_registers->network_control = static_cast<Registers::NetworkControl>(0);

    // "2. Clear the statistics registers. Write a 1 to the gem.network_control[clear_all_stats_regs]."
    m_registers->network_control |= Registers::NetworkControl::ClearStatisticsRegisters;

    // "3. Clear the status registers. Write a 1 to the status registers. gem.receive_status = 0x0F and gem.transmit_status = 0xFF."
    m_registers->receive_status = static_cast<Registers::ReceiveStatus>(0x0f);
    m_registers->transmit_status = static_cast<Registers::TransmitStatus>(0xff);

    // "4. Disable all interrupts. Write 0x7FF_FEFF to the gem.int_disable register."
    m_registers->interrupt_disable = static_cast<Registers::Interrupt>(0x7ff'feff);

    // "5. Clear the buffer queues. Write 0x0 to the gem.receive_q{,1}_ptr and gem.transmit_q{,1}_ptr registers."
    m_registers->receive_buffer_queue_base_address = 0;
    m_registers->receive_buffer_queue_1_base_address = 0;
    m_registers->transmit_buffer_queue_base_address = 0;
    m_registers->transmit_buffer_queue_1_base_address = 0;

    // This roughly follows the steps described in the "Configure the Controller" section.

    auto mdc_divisor_field = determine_mdc_clock_divisor(mdio_clock_input_frequency());
    if (!mdc_divisor_field.has_value()) {
        dmesgln("Failed to find a suitable MDC clock divisor");
        return ENOTSUP;
    }

    m_registers->network_configuration &= ~static_cast<Registers::NetworkConfiguration>(Registers::NETWORK_CONFIGURATION_MDC_DIVISION_MASK);
    m_registers->network_configuration |= static_cast<Registers::NetworkConfiguration>(*mdc_divisor_field << Registers::NETWORK_CONFIGURATION_MDC_DIVISION_OFFSET);

    m_registers->network_configuration |= Registers::NetworkConfiguration::FullDuplex
        | Registers::NetworkConfiguration::GigabitModeEnable
        | Registers::NetworkConfiguration::DisableCopyOfPauseFrames;

    m_registers->network_configuration &= ~(Registers::NetworkConfiguration::NoBroadcast
        | Registers::NetworkConfiguration::CopyAllFrames
        | Registers::NetworkConfiguration::JumboFrames
        | Registers::NetworkConfiguration::Receive1536ByteFrames);

    m_registers->specific_address_1_bottom = (static_cast<u32>(mac_address()[0]) << 0uz)
        | (static_cast<u32>(mac_address()[1]) << 8uz)
        | (static_cast<u32>(mac_address()[2]) << 16uz)
        | (static_cast<u32>(mac_address()[3]) << 24uz);

    m_registers->specific_address_1_top = (static_cast<u32>(mac_address()[4]) << 0uz)
        | (static_cast<u32>(mac_address()[5]) << 8uz);

    VERIFY(RECEIVE_BUFFER_SIZE >= mtu() + sizeof(EthernetFrameHeader) + 4); // + 4 for the CRC
    static_assert((RECEIVE_BUFFER_SIZE % 64) == 0);
    auto receive_buffer_size_field = RECEIVE_BUFFER_SIZE / 64;

    m_registers->dma_configuration &= ~static_cast<Registers::DMAConfiguration>(Registers::DMA_CONFIGURATION_RECEIVE_BUFFER_SIZE_MASK);
    m_registers->dma_configuration |= static_cast<Registers::DMAConfiguration>(receive_buffer_size_field << Registers::DMA_CONFIGURATION_RECEIVE_BUFFER_SIZE_OFFSET);
    m_registers->dma_configuration |= Registers::DMAConfiguration::AddressBusWidth64;

    m_registers->network_control |= Registers::NetworkControl::EnableManagementPort;

    m_mdio_handling_process = TRY(spawn_mdio_handling_task(PHY_ID));

    TRY(initialize_rx_descriptors());
    TRY(initialize_tx_descriptors());

    register_and_enable_interrupt();

    m_registers->interrupt_enable |= Registers::Interrupt::ReceiveComplete
        | Registers::Interrupt::TransmitComplete;

    m_registers->network_control |= Registers::NetworkControl::EnableTransmit
        | Registers::NetworkControl::EnableReceive;

    return {};
}

ErrorOr<void> CadenceGEMNetworkAdapter::initialize_rx_descriptors()
{
    m_rx_descriptor_list_region = TRY(MM.allocate_dma_buffer_pages(MUST(Memory::page_round_up(RX_DESCRIPTOR_COUNT * sizeof(RxBufferDescriptorEntry))), "Cadence GEM Rx Descriptor List"sv, Memory::Region::Access::ReadWrite));

    for (size_t i = 0; i < RX_DESCRIPTOR_COUNT; i++) {
        auto* entry = rx_descriptor_entry(i);

        auto buffer = TRY(MM.allocate_dma_buffer_pages(MUST(Memory::page_round_up(RECEIVE_BUFFER_SIZE)), "Cadence GEM Rx Buffer"sv, Memory::Region::Access::ReadOnly));
        auto buffer_paddr = buffer->physical_page(0)->paddr().get();
        m_rx_buffers[i] = move(buffer);

        entry->is_software_owned = 0;
        entry->buffer_address_31_2 = buffer_paddr >> 2;
        entry->buffer_address_63_32 = buffer_paddr >> 32;
    }

    rx_descriptor_entry(RX_DESCRIPTOR_COUNT - 1)->is_last_descriptor = 1;

    auto rx_descriptor_list_paddr = m_rx_descriptor_list_region->physical_page(0)->paddr().get();

    // Ensure that the initialization of the descriptors and the zeroing of the buffers is visible
    // before giving their addresses to the controller.
    store_memory_fence();

    m_registers->receive_buffer_queue_base_address = rx_descriptor_list_paddr & 0xffff'ffff;
    m_registers->receive_buffer_queue_base_address_high = rx_descriptor_list_paddr >> 32;

    return {};
}

ErrorOr<void> CadenceGEMNetworkAdapter::initialize_tx_descriptors()
{
    m_tx_descriptor_list_region = TRY(MM.allocate_dma_buffer_pages(MUST(Memory::page_round_up(TX_DESCRIPTOR_COUNT * sizeof(TxBufferDescriptorEntry))), "Cadence GEM Tx Descriptor List"sv, Memory::Region::Access::ReadWrite));

    for (size_t i = 0; i < TX_DESCRIPTOR_COUNT; i++) {
        auto* entry = tx_descriptor_entry(i);

        auto buffer = TRY(MM.allocate_dma_buffer_pages(MUST(Memory::page_round_up(TRANSMIT_BUFFER_SIZE)), "Cadence GEM Tx Buffer"sv, Memory::Region::Access::Write));
        auto buffer_paddr = buffer->physical_page(0)->paddr().get();
        m_tx_buffers[i] = move(buffer);

        entry->is_software_owned = 1;
        entry->buffer_address_31_0 = buffer_paddr & 0xffff'ffff;
        entry->buffer_address_63_32 = buffer_paddr >> 32;
    }

    tx_descriptor_entry(TX_DESCRIPTOR_COUNT - 1)->is_last_descriptor = 1;

    auto tx_descriptor_list_paddr = m_tx_descriptor_list_region->physical_page(0)->paddr().get();

    // Ensure that the initialization of the descriptors and the zeroing of the buffers is visible
    // before giving their addresses to the controller.
    store_memory_fence();

    m_registers->transmit_buffer_queue_base_address = tx_descriptor_list_paddr & 0xffff'ffff;
    m_registers->transmit_buffer_queue_base_address_high = tx_descriptor_list_paddr >> 32;

    return {};
}

bool CadenceGEMNetworkAdapter::handle_interrupt()
{
    auto interrupt_status = m_registers->interrupt_status;

    // Acknowledge the interrupt(s).
    m_registers->interrupt_status = interrupt_status;

    if (has_flag(interrupt_status, Registers::Interrupt::ReceiveComplete)) {
        auto receive_status = m_registers->receive_status;

        // Acknowledge the receive status.
        m_registers->receive_status = receive_status;

        for (;;) {
            // Inform the compiler that the contents of DMA regions might have changed,
            // even if it didn't see any writes to them.
            optimizer_fence();

            auto* descriptor = rx_descriptor_entry(m_rx_dequeue_index);
            if (!descriptor->is_software_owned)
                break;

            // Ensure the software owned check is done before reading the descriptor and buffer data.
            load_memory_fence();

            auto length = descriptor->received_frame_length;
            if (length > RECEIVE_BUFFER_SIZE) {
                dbgln("CadenceGEM: Controller set a bogus received frame length: {} (max is {})", length, RECEIVE_BUFFER_SIZE);
            } else {
                ReadonlyBytes buffer { m_rx_buffers[m_rx_dequeue_index]->vaddr().as_ptr(), length };
                did_receive(buffer);

                // did_receive() copies the packet, so it's safe for us to give back ownership to the controller.
            }

            // Ensure that the descriptor and buffer are fully read before giving back ownership.
            full_memory_fence();

            descriptor->is_software_owned = 0;

            m_rx_dequeue_index = (m_rx_dequeue_index + 1) % RX_DESCRIPTOR_COUNT;
        }
    }

    if (has_flag(interrupt_status, Registers::Interrupt::TransmitComplete)) {
        auto transmit_status = m_registers->transmit_status;

        // Acknowledge the transmit status.
        m_registers->transmit_status = transmit_status;

        // Some transmit descriptors might now be free; notify any waiters about this.
        m_wait_queue.notify_all();
    }

    return true;
}

void CadenceGEMNetworkAdapter::on_phy_link_status_change(MDIO::LinkStatus link_status)
{
    m_link_up = link_status == MDIO::LinkStatus::Up;
}

u16 CadenceGEMNetworkAdapter::read_phy_register(u8 phy_id, MDIO::Clause22::RegisterAddress address)
{
    // The address and PHY ID are 5 bits.
    VERIFY((phy_id & ~0x1f) == 0);
    VERIFY((to_underlying(address) & ~0x1f) == 0);

    // "Example: PHY Read/Write Operation"

    // "1. Check to see that no MDIO operation is in progress. Read until gem.net_status[man_done] = 1."
    while (!has_flag(m_registers->network_status, Registers::NetworkStatus::ManagementLogicIdle))
        Processor::wait_check();

    // "2. Write data to the PHY management register (gem.phy_management). This initiates the data shift operation over MDIO."
    m_registers->phy_maintenance = 0b10 << Registers::PHY_MAINTENANCE_WRITE_10_OFFSET
        | to_underlying(address) << Registers::PHY_MAINTENANCE_REGISTER_ADDRESS_OFFSET
        | phy_id << Registers::PHY_MAINTENANCE_PHY_ADDRESS_OFFSET
        | to_underlying(Registers::PhyMaintenanceOperation::Clause22Read) << Registers::PHY_MAINTENANCE_OPERATION_OFFSET
        | 1 << Registers::PHY_MAINTENANCE_CLAUSE_22_FRAME_OFFSET;

    // "3. Wait for completion of operation. Read until gem.net_status[man_done] = 1."
    while (!has_flag(m_registers->network_status, Registers::NetworkStatus::ManagementLogicIdle))
        Processor::wait_check();

    // "4. Read data bits for a read operation. The PHY register data is available in gem.phy_management[phy_write_read_data]."
    return (m_registers->phy_maintenance >> Registers::PHY_MAINTENANCE_WRITE_OR_READ_DATA_OFFSET)
        & Registers::PHY_MAINTENANCE_WRITE_OR_READ_DATA_MASK;
}

void CadenceGEMNetworkAdapter::write_phy_register(u8 phy_id, MDIO::Clause22::RegisterAddress address, u16 value)
{
    // The address and PHY ID are 5 bits.
    VERIFY((phy_id & ~0x1f) == 0);
    VERIFY((to_underlying(address) & ~0x1f) == 0);

    // "Example: PHY Read/Write Operation"

    // "1. Check to see that no MDIO operation is in progress. Read until gem.net_status[man_done] = 1."
    while (!has_flag(m_registers->network_status, Registers::NetworkStatus::ManagementLogicIdle))
        Processor::wait_check();

    // "2. Write data to the PHY management register (gem.phy_management). This initiates the data shift operation over MDIO."
    m_registers->phy_maintenance = value << Registers::PHY_MAINTENANCE_WRITE_OR_READ_DATA_OFFSET
        | 0b10 << Registers::PHY_MAINTENANCE_WRITE_10_OFFSET
        | to_underlying(address) << Registers::PHY_MAINTENANCE_REGISTER_ADDRESS_OFFSET
        | phy_id << Registers::PHY_MAINTENANCE_PHY_ADDRESS_OFFSET
        | to_underlying(Registers::PhyMaintenanceOperation::Clause22Write) << Registers::PHY_MAINTENANCE_OPERATION_OFFSET
        | 1 << Registers::PHY_MAINTENANCE_CLAUSE_22_FRAME_OFFSET;

    // "3. Wait for completion of operation. Read until gem.net_status[man_done] = 1."
    while (!has_flag(m_registers->network_status, Registers::NetworkStatus::ManagementLogicIdle))
        Processor::wait_check();
}

CadenceGEMNetworkAdapter::RxBufferDescriptorEntry* CadenceGEMNetworkAdapter::rx_descriptor_entry(size_t index)
{
    size_t offset = index * sizeof(RxBufferDescriptorEntry);
    return reinterpret_cast<RxBufferDescriptorEntry*>(m_rx_descriptor_list_region->vaddr().offset(offset).as_ptr());
}

CadenceGEMNetworkAdapter::TxBufferDescriptorEntry* CadenceGEMNetworkAdapter::tx_descriptor_entry(size_t index)
{
    size_t offset = index * sizeof(TxBufferDescriptorEntry);
    return reinterpret_cast<TxBufferDescriptorEntry*>(m_tx_descriptor_list_region->vaddr().offset(offset).as_ptr());
}

Optional<u8> CadenceGEMNetworkAdapter::determine_mdc_clock_divisor(u32 input_frequency)
{
    // IEEE 802.3, 22.2.2.13 MDC (management data clock)
    // "[T]he minimum period for MDC shall be 400 ns."
    // 1 / 400 ns = 2.5 MHz, so the frequency should be below that value.
    u32 max_mdc_frequency = 2'500'000;

    struct Divisor {
        u8 divisor;
        u8 register_field_value;
    };

    static constexpr auto DIVISORS = to_array<Divisor>({
        { 8, 0b000 },
        { 16, 0b001 },
        { 32, 0b010 },
        { 48, 0b011 },
        { 64, 0b100 },
        { 96, 0b101 },
        { 128, 0b110 },
        { 224, 0b111 },
    });

    for (auto [divisor, register_field_value] : DIVISORS) {
        if (input_frequency / divisor <= max_mdc_frequency) {
            return register_field_value;
        }
    }

    return {};
}

CadenceGEMNetworkAdapter::CadenceGEMNetworkAdapter(StringView interface_name, MACAddress mac_address, Memory::TypedMapping<Registers volatile> registers)
    : NetworkAdapter(interface_name)
    , m_registers(move(registers))
{
    set_mac_address(mac_address);
}

}
