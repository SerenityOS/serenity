/*
 * Copyright (c) 2024, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>
#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Bus/USB/xHCI/DataStructures.h>
#include <Kernel/Bus/USB/xHCI/Registers.h>
#include <Kernel/Bus/USB/xHCI/xHCIRootHub.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Tasks/DeprecatedWaitQueue.h>

namespace Kernel::USB::xHCI {

class xHCIPCIInterrupter;
class xHCIDeviceTreeInterrupter;

class xHCIController
    : public USBController {
    friend class xHCIPCIInterrupter;
    friend class xHCIDeviceTreeInterrupter;

public:
    virtual ~xHCIController() override;

    virtual ErrorOr<void> initialize() override;
    virtual ErrorOr<void> reset() override;
    virtual ErrorOr<void> stop() override;
    virtual ErrorOr<void> start() override;

    virtual void cancel_async_transfer(NonnullLockRefPtr<Transfer> transfer) override;
    virtual ErrorOr<size_t> submit_control_transfer(Transfer& transfer) override;
    virtual ErrorOr<size_t> submit_bulk_transfer(Transfer& transfer) override;
    virtual ErrorOr<void> submit_async_interrupt_transfer(NonnullLockRefPtr<Transfer> transfer, u16 ms_interval) override;

    virtual ErrorOr<void> reset_pipe(USB::Device&, USB::Pipe&) override;

    virtual ErrorOr<void> initialize_device(USB::Device&) override;

    ErrorOr<void> initialize_endpoint_if_needed(Pipe const&);

    u8 ports() const { return m_ports; }

    ErrorOr<HubStatus> get_port_status(Badge<xHCIRootHub>, u8 port);
    ErrorOr<void> set_port_feature(Badge<xHCIRootHub>, u8 port, HubFeatureSelector);
    ErrorOr<void> clear_port_feature(Badge<xHCIRootHub>, u8 port, HubFeatureSelector);

protected:
    xHCIController(Memory::TypedMapping<u8> registers_mapping);

    virtual bool using_message_signalled_interrupts() const = 0;
    virtual ErrorOr<OwnPtr<GenericInterruptHandler>> create_interrupter(u16 interrupter_id) = 0;
    virtual ErrorOr<void> write_dmesgln_prefix(StringBuilder&) const = 0;

private:
    void handle_interrupt(u16 interrupter_id);

    void take_exclusive_control_from_bios();
    ErrorOr<void> find_port_max_speeds();

    void event_handling_thread();
    void hot_plug_thread();
    void poll_thread();

    // Arbitrarily chosen to decrease allocation sizes, can be increased up to 256 if we reach this limit
    static constexpr size_t max_devices = 64;

    static constexpr size_t command_ring_size = 16;
    // Use up all the space left in the page
    static constexpr size_t event_ring_segment_size = ((PAGE_SIZE - (sizeof(EventRingSegmentTableEntry) + sizeof(TransferRequestBlock) * command_ring_size)) & ~0x3F) / sizeof(TransferRequestBlock);
    // System software is responsible for ensuring the Size of every ERST entry (Event Ring segment) is at least 16.
    static_assert(event_ring_segment_size >= 16);
    // System software shall allocate a buffer for the Event Ring Segment Table that rounds up its size to the nearest 64B boundary to allow full cache-line accesses.
    static_assert((event_ring_segment_size * sizeof(TransferRequestBlock)) % 64 == 0);
    // The command ring and event ring (ERST and the segment itself) are combined to not take 3 pages for something that fits in 1)
    struct CommandAndEventRings {
        TransferRequestBlock command_ring[command_ring_size];
        TransferRequestBlock event_ring_segment[event_ring_segment_size];
        // Software shall allocate a buffer for the Event Ring Segment Table that rounds up its size to the nearest 64B boundary to allow full cache-line accesses.
        [[gnu::aligned(64)]] EventRingSegmentTableEntry event_ring_segment_table_entry;
    };
    static_assert(sizeof(CommandAndEventRings) <= 0x1000);
    // System software shall allocate a buffer for the Event Ring Segment that rounds up its size to the nearest 64B boundary to allow full cache-line accesses
    static_assert(__builtin_offsetof(CommandAndEventRings, command_ring) % 64 == 0);
    static_assert(__builtin_offsetof(CommandAndEventRings, event_ring_segment) % 64 == 0);

    struct PendingTransfer {
        IntrusiveListNode<PendingTransfer> endpoint_list_node;
        u32 start_index { 0 };
        u32 end_index { 0 };
    };
    struct SyncPendingTransfer : public PendingTransfer {
        DeprecatedWaitQueue wait_queue;
        TransferRequestBlock::CompletionCode completion_code { TransferRequestBlock::CompletionCode::Invalid };
        u32 remainder { 0 };
    };
    struct PeriodicPendingTransfer : public PendingTransfer {
        Vector<TransferRequestBlock> transfer_request_blocks;
        NonnullLockRefPtr<Transfer> original_transfer;
    };

    struct EndpointRing {
        OwnPtr<Memory::Region> region;
        u32 enqueue_index { 0 };
        u32 free_transfer_request_blocks { endpoint_ring_size - 1 }; // One less, since we use up the last one for the link TRB
        u32 max_burst_payload { 0 };
        Pipe::Type type { Pipe::Type::Control };
        u8 producer_cycle_state { 1 };
        IntrusiveList<&PendingTransfer::endpoint_list_node> pending_transfers;
        TransferRequestBlock* ring_vaddr() const { return reinterpret_cast<TransferRequestBlock*>(region->vaddr().as_ptr()); }
        PhysicalPtr ring_paddr() const { return region->physical_page(0)->paddr().get(); }
    };
    static constexpr size_t max_endpoints = 31;
    static constexpr size_t endpoint_ring_size = PAGE_SIZE / sizeof(TransferRequestBlock);
    struct SlotState {
        RecursiveSpinlock<LockRank::None> lock;
        OwnPtr<Memory::Region> input_context_region;
        OwnPtr<Memory::Region> device_context_region;
        Array<EndpointRing, max_endpoints> endpoint_rings;
    };

    static u8 endpoint_index(u8 endpoint, Pipe::Direction direction)
    {
        if (direction == Pipe::Direction::Bidirectional) {
            VERIFY(endpoint == 0);
            direction = Pipe::Direction::In;
        }
        return endpoint * 2 + to_underlying(direction);
    }

    size_t context_entry_size() const { return m_large_contexts ? 64 : 32; }
    size_t input_context_size() const { return context_entry_size() * 33; }

    u8* input_context(u8 slot, u8 index) const
    {
        auto* base = m_slots_state[slot - 1].input_context_region->vaddr().as_ptr();
        return base + (context_entry_size() * index);
    }

    InputControlContext* input_control_context(u8 slot) const
    {
        return reinterpret_cast<InputControlContext*>(input_context(slot, 0));
    }

    SlotContext* input_slot_context(u8 slot) const
    {
        return reinterpret_cast<SlotContext*>(input_context(slot, 1));
    }

    EndpointContext* input_endpoint_context(u8 slot, u8 endpoint, Pipe::Direction direction) const
    {
        return reinterpret_cast<EndpointContext*>(input_context(slot, endpoint_index(endpoint, direction) + 1));
    }

    size_t device_context_size() const { return context_entry_size() * 32; }

    u8* device_context(u8 slot, u8 index) const
    {
        auto* base = m_slots_state[slot - 1].device_context_region->vaddr().as_ptr();
        return base + (context_entry_size() * index);
    }

    SlotContext* device_slot_context(u8 slot) const
    {
        return reinterpret_cast<SlotContext*>(device_context(slot, 0));
    }

    EndpointContext* device_endpoint_context(u8 slot, u8 endpoint, Pipe::Direction direction) const
    {
        return reinterpret_cast<EndpointContext*>(device_context(slot, endpoint_index(endpoint, direction)));
    }

    void ring_doorbell(u8 doorbell, u8 doorbell_target);
    void ring_endpoint_doorbell(u8 slot, u8 endpoint, Pipe::Direction direction)
    {
        VERIFY(slot > 0);
        ring_doorbell(slot, endpoint_index(endpoint, direction));
    }
    void ring_command_doorbell() { ring_doorbell(0, 0); }
    void enqueue_command(TransferRequestBlock&);
    void execute_command(TransferRequestBlock&);

    ErrorOr<u8> enable_slot();
    ErrorOr<void> address_device(u8 slot, u64 input_context_address);
    ErrorOr<void> evaluate_context(u8 slot, u64 input_context_address);
    ErrorOr<void> configure_endpoint(u8 slot, u64 input_context_address);

    enum class TransferStatePreserve {
        No,
        Yes,
    };
    ErrorOr<void> reset_endpoint(u8 slot, u8 endpoint, TransferStatePreserve);

    ErrorOr<void> set_tr_dequeue_pointer(u8 slot, u8 endpoint, u8 stream_context_type, u16 stream, u64 new_tr_dequeue_pointer, u8 dequeue_cycle_state);

    ErrorOr<void> enqueue_transfer(u8 slot, u8 endpoint, Pipe::Direction direction, Span<TransferRequestBlock>, PendingTransfer&);
    void handle_transfer_event(TransferRequestBlock const&);

    ErrorOr<Vector<TransferRequestBlock>> prepare_normal_transfer(Transfer& transfer);

    Memory::TypedMapping<u8> m_registers_mapping;
    CapabilityRegisters const volatile& m_capability_registers;
    OperationalRegisters volatile& m_operational_registers;
    RuntimeRegisters volatile& m_runtime_registers;
    DoorbellRegisters volatile& m_doorbell_registers;

    RefPtr<Process> m_process;
    DeprecatedWaitQueue m_event_queue;

    bool m_using_message_signalled_interrupts { false };
    bool m_large_contexts { false };
    u8 m_device_slots { 0 };
    Array<SlotState, max_devices> m_slots_state;
    u8 m_ports { 0 };
    Array<USB::Device::DeviceSpeed, 255> m_port_max_speeds {};

    Vector<NonnullOwnPtr<PeriodicPendingTransfer>> m_active_periodic_transfers;

    Spinlock<LockRank::None> m_command_lock;
    DeprecatedWaitQueue m_command_completion_queue;
    TransferRequestBlock m_command_result_transfer_request_block {};
    u32 m_command_ring_enqueue_index { 0 };
    u8 m_command_ring_producer_cycle_state { 1 };
    u32 m_event_ring_dequeue_index { 0 };
    u8 m_event_ring_consumer_cycle_state { 1 };

    OwnPtr<Memory::Region> m_device_context_base_address_array_region;
    u64* m_device_context_base_address_array { nullptr };
    OwnPtr<Memory::Region> m_scratchpad_buffers_array_region;
    Vector<NonnullRefPtr<Memory::PhysicalRAMPage>> m_scratchpad_buffers;
    OwnPtr<Memory::Region> m_command_and_event_rings_region;
    TransferRequestBlock* m_command_ring { nullptr };
    TransferRequestBlock* m_event_ring_segment { nullptr };
    PhysicalPtr m_event_ring_segment_pointer { 0 };

    OwnPtr<GenericInterruptHandler> m_interrupter;
    OwnPtr<xHCIRootHub> m_root_hub;

protected:
    template<typename... Parameters>
    void dmesgln_xhci(AK::CheckedFormatString<Parameters...>&& fmt, Parameters const&... parameters) const
    {
        StringBuilder builder;

        MUST(write_dmesgln_prefix(builder));

        AK::VariadicFormatParams<AK::AllowDebugOnlyFormatters::Yes, Parameters...> variadic_format_params { parameters... };
        MUST(AK::vformat(builder, fmt.view(), variadic_format_params));

        dmesgln("{}", builder.string_view());
    }
};

}
