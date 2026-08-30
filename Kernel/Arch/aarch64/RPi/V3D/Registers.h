/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>
#include <Kernel/Library/StdLib.h>

namespace Kernel::RPi::V3D {

struct HubRegisters {
    enum class Interrupt : u32 {
        MMUCapExceeded = 1u << 3,
        MMUPageTableEntryInvalid = 1u << 4,
        MMUWriteViolation = 1u << 5,
    };

    enum class MMUCacheControl : u32 {
        Enable = 1u << 0,
        Flush = 1u << 1,
        Flushing = 1u << 2,
    };

    enum class MMUControl : u32 {
        Enable = 1u << 0,
        TLBClear = 1u << 2,
        TLBClearing = 1u << 7,
        WriteViolationInterrupt = 1u << 10,
        WriteViolationAbort = 1u << 11,
        InvalidPageTableEntryEnable = 1u << 16,
        InvalidPageTableEntryInterrupt = 1u << 18,
        InvalidPageTableEntryAbort = 1u << 19,
        CapExceededInterrupt = 1u << 25,
        CapExceededAbort = 1u << 26,
    };

    static constexpr u32 ILLEGAL_VADDR_TARGET_PADDR_VALID = 1u << 31;

    u8 _[0x50];

    Interrupt interrupt_status;
    Interrupt interrupt_set_pending;
    Interrupt interrupt_clear_pending;
    Interrupt interrupt_mask;
    Interrupt interrupt_mask_set;
    Interrupt interrupt_mask_clear;

    u8 _[0xf98];

    struct {
        MMUCacheControl mmu_cache_control;

        u8 _[0x1fc];

        MMUControl control;
        u32 page_table_base_page_index;

        u8 _[0x24];

        u32 fault_axi_id;
        u32 illegal_vaddr_target_paddr;
        u32 fault_vaddr;
    } mmu_0;

    u8 _[0x2dc8];
};
static_assert(AssertSize<HubRegisters, 0x4000>());
static_assert(offsetof(HubRegisters, interrupt_status) == 0x50);
static_assert(offsetof(HubRegisters, mmu_0.mmu_cache_control) == 0x1000);
static_assert(offsetof(HubRegisters, mmu_0.control) == 0x1200);
static_assert(offsetof(HubRegisters, mmu_0.fault_axi_id) == 0x122c);
static_assert(offsetof(HubRegisters, mmu_0.illegal_vaddr_target_paddr) == 0x1230);
static_assert(offsetof(HubRegisters, mmu_0.fault_vaddr) == 0x1234);

AK_ENUM_BITWISE_OPERATORS(HubRegisters::MMUCacheControl)
AK_ENUM_BITWISE_OPERATORS(HubRegisters::MMUControl)
AK_ENUM_BITWISE_OPERATORS(HubRegisters::Interrupt)

struct CoreRegisters {
    enum class Interrupt : u32 {
        RenderModeFrameDone = 1u << 0,
        BinningModeFlushDone = 1u << 1,
        BinnerOutOfMemory = 1u << 2,
        BinnerOverspillMemoryInUse = 1u << 3,
    };

    enum class TextureCacheControl : u32 {
        StartFlush = 1u << 0,
        FlushModeFlush = 0b00u << 1,
    };

    static constexpr size_t TILE_STATE_DATA_ARRAY_ADDRESS_VALID = 1u << 1;

    u32 identification_0;
    u32 identification_1;
    u32 identification_2;

    u8 _[0x18];

    u32 slices_cache_control;

    u8 _[0x8];

    TextureCacheControl texture_cache_control;
    u32 texture_cache_flush_start_addr;
    u32 texture_cache_flush_end_addr;

    u8 _[0x14];

    Interrupt interrupt_status;
    Interrupt interrupt_set_pending;
    Interrupt interrupt_clear_pending;
    Interrupt interrupt_mask;
    Interrupt interrupt_mask_set;
    Interrupt interrupt_mask_clear;

    u8 _[0x98];

    // Thread 0: Tile binning
    // Thread 1: Tile rendering

    struct {
        u32 thread_0_control_and_status; // CT0CS
        u32 thread_1_control_and_status; // CT1CS

        u32 thread_0_end_address; // CT0EA
        u32 thread_1_end_address; // CT1EA

        u32 thread_0_current_address; // CT0CA
        u32 thread_1_current_address; // CT1CA

        u32 thread_0_return_address; // CT0RA
        u32 thread_1_return_address; // CT1RA

        u32 thread_0_list_counter; // CT0LC
        u32 thread_1_list_counter; // CT1LC

        u32 thread_0_primitive_list_counter; // CT0PC
        u32 thread_1_primitive_list_counter; // CT1PC

        u32 pipeline_control_and_status; // PCS
        u32 binning_mode_flush_count;    // BFC
        u32 rendering_mode_flush_count;  // RFC

        u8 _[0x20];

        u32 thread_0_tile_state_data_array_address;

        u32 thread_0_control_list_start_address;
        u32 thread_1_control_list_start_address;

        u32 thread_0_control_list_end_address;
        u32 thread_1_control_list_end_address;

        u32 thread_0_tile_allocation_memory_address;
        u32 thread_0_tile_allocation_memory_size;
    } control_list_executor;

    u8 _[0x188];

    u32 current_address_of_binning_memory_pool;    // BPCA
    u32 remaining_size_of_binning_memory_pool;     // BPCS
    u32 address_of_overspill_binning_memory_block; // BPOA
    u32 size_of_overspill_binning_memory_block;    // BPOS

    u8 _[0xbf4];

    u32 fep_overrun_error_signals;                               // FDBGO
    u32 fep_interface_ready_and_stall_signals__fep_busy_signals; // FDBGB
    u32 fep_interface_ready_signals;                             // FDBGR
    u32 fep_internal_stall_input_signals;                        // FDBGS

    u8 _[0xc];

    u32 miscellaneous_error_signals; // ERRSTAT

    u8 _[0x50dc];
};
static_assert(AssertSize<CoreRegisters, 0x6000>());
static_assert(offsetof(CoreRegisters, texture_cache_flush_start_addr) == 0x34);
static_assert(offsetof(CoreRegisters, interrupt_status) == 0x50);
static_assert(offsetof(CoreRegisters, control_list_executor.rendering_mode_flush_count) == 0x138);
static_assert(offsetof(CoreRegisters, control_list_executor.thread_0_tile_state_data_array_address) == 0x15c);
static_assert(offsetof(CoreRegisters, control_list_executor.thread_0_control_list_start_address) == 0x160);
static_assert(offsetof(CoreRegisters, control_list_executor.thread_1_control_list_end_address) == 0x16c);
static_assert(offsetof(CoreRegisters, control_list_executor.thread_0_tile_allocation_memory_address) == 0x170);
static_assert(offsetof(CoreRegisters, size_of_overspill_binning_memory_block) == 0x30c);
static_assert(offsetof(CoreRegisters, fep_overrun_error_signals) == 0xf04);
static_assert(offsetof(CoreRegisters, fep_internal_stall_input_signals) == 0xf10);
static_assert(offsetof(CoreRegisters, miscellaneous_error_signals) == 0xf20);

AK_ENUM_BITWISE_OPERATORS(CoreRegisters::Interrupt)
AK_ENUM_BITWISE_OPERATORS(CoreRegisters::TextureCacheControl)

}
