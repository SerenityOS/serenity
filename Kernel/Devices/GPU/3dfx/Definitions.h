/*
 * Copyright (c) 2023, Edwin Rijkee <edwin@virtualparadise.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Types.h>

namespace Kernel::VoodooGraphics {

enum class VGAPort : u16 {
    AttributeController = 0x3c0,
    MiscOutputWrite = 0x3c2,
    SequencerIndex = 0x3c4,
    SequencerData = 0x3c5,
    MiscOutputRead = 0x3cc,
    GraphicsControllerIndex = 0x3ce,
    GraphicsControllerData = 0x3cf,
    CrtcIndex = 0x3d4,
    CrtcData = 0x3d5,
    InputStatus1 = 0x3da,
};

enum CRTCHorizontalBlankingEndFlags : u8 {
    CompatibilityRead = 1 << 7
};

enum CRTCVerticalSyncEndFlags : u8 {
    EnableVertInt = 1 << 5,
    CRTCRegsWriteProt = 1 << 7
};

enum CRTCModeControlFlags : u8 {
    ByteWordMode = 1 << 6,
    TimingEnable = 1 << 7
};

enum GraphicsControllerMiscellaneousFlags : u8 {
    MemoryMapEGAVGAExtended = 1 << 2,
};

enum AttributeControllerModeFlags : u8 {
    GraphicsMode = 1 << 0,
    PixelWidth = 1 << 6,
};

enum SequencerResetFlags : u8 {
    AsynchronousReset = 1 << 0,
    SynchronousReset = 1 << 1,
};

enum SequencerClockingModeFlags : u8 {
    DotClock8 = 1 << 0,
};

enum MiscellaneousOutputFlags : u8 {
    CRTCAddressColor = 1 << 0,
    ClockSelectPLL = 0b1100,
    VerticalSyncPositive = 1 << 7,
    HorizontalSyncPositive = 1 << 6,
};

enum DacModeFlags : u32 {
    DacMode2x = 1 << 0,
};

enum VgaInit0Flags : u32 {
    FIFODepth8Bit = 1 << 2,
    EnableVgaExtensions = 1 << 6,
    WakeUpSelect3C3 = 1 << 8,
    EnableAltReadback = 1 << 10,
    ExtendedShiftOut = 1 << 12,
};

enum VidProcCfgFlags : u32 {
    VideoProcessorEnable = 1 << 0,
    DesktopSurfaceEnable = 1 << 7,
    DesktopCLUTBypass = 1 << 10,
    DesktopPixelFormat32Bit = 0b11 << 18,
    TwoXMode = 1 << 26,
};

struct PLLSettings {
    static i32 const reference_frequency_in_khz = 14318;
    i32 m = 0;
    i32 n = 0;
    i32 k = 0;

    int frequency_in_khz() const
    {
        return (reference_frequency_in_khz * (n + 2) / (m + 2)) >> k;
    }

    u32 register_value() const
    {
        return (n << 8) | (m << 2) | k;
    }
};

// CRT Controller Registers
struct CRRegisters {
    u8 horizontal_total;              // CR0
    u8 horizontal_display_enable_end; // CR1
    u8 horizontal_blanking_start;     // CR2
    u8 horizontal_blanking_end;       // CR3
    u8 horizontal_sync_start;         // CR4
    u8 horizontal_sync_end;           // CR5
    u8 vertical_total;                // CR6
    u8 overflow;                      // CR7
    u8 reserved_0;                    // CR8
    u8 maximum_scan_line;             // CR9
    u8 reserved_1[6];
    u8 vertical_sync_start;         // CR10
    u8 vertical_sync_end;           // CR11
    u8 vertical_display_enable_end; // CR12
    u8 reserved_2[2];
    u8 vertical_blanking_start; // CR15
    u8 vertical_blanking_end;   // CR16
    u8 mode_control;            // CR17
    u8 reserved_3[2];
    u8 horizontal_extensions; // CR1A
    u8 vertical_extensions;   // CR1B
};

// Graphics Controller Registers
struct GRRegisters {
    u8 reserved_0[6];
    u8 graphics_controller_miscellaneous; // GR6
    u8 reserved_1[2];
};

// Attribute Controller Registers
struct ARRegisters {
    u8 reserved_0[15];
    u8 attribute_controller_mode; // AR10
    u8 reserved_1[5];
};

// Sequencer Registers
struct SRRegisters {
    u8 sequencer_reset;         // SR0
    u8 sequencer_clocking_mode; // SR1
    u8 reserved[3];
};

struct ModeRegisters {
    u32 vid_screen_size = 0;
    u32 vid_desktop_overlay_stride = 0;
    u8 misc_out_reg = 0;
    u32 vga_init0 = 0;
    u32 vid_proc_cfg = 0;
    u32 dac_mode = 0;
    u32 pll_ctrl0 = 0;

    union {
        Array<u8, 0x1c> cr_data = { 0 };
        CRRegisters cr;
    };

    union {
        Array<u8, 0x09> gr_data = { 0 };
        GRRegisters gr;
    };

    union {
        Array<u8, 0x15> ar_data = { 0 };
        ARRegisters ar;
    };

    union {
        Array<u8, 0x05> sr_data = { 0 };
        SRRegisters sr;
    };
};

static_assert(sizeof(ModeRegisters::cr_data) == sizeof(ModeRegisters::cr));
static_assert(sizeof(ModeRegisters::gr_data) == sizeof(ModeRegisters::gr));
static_assert(sizeof(ModeRegisters::ar_data) == sizeof(ModeRegisters::ar));

struct [[gnu::packed]] RegisterMap {
    u32 status;
    u32 reserved_0[9];
    u32 vga_init0;
    u32 reserved_1[5];
    u32 pll_ctrl0;
    u32 reserved_2[2];
    u32 dac_mode;
    u32 reserved_3[3];

    u32 vid_proc_cfg;
    u32 reserved_4[14];
    u32 vid_screen_size;
    u32 reserved_5[18];
    u32 vid_desktop_start_addr;
    u32 vid_desktop_overlay_stride;
};

static_assert(__builtin_offsetof(RegisterMap, status) == 0);
static_assert(__builtin_offsetof(RegisterMap, vga_init0) == 0x28);
static_assert(__builtin_offsetof(RegisterMap, pll_ctrl0) == 0x40);
static_assert(__builtin_offsetof(RegisterMap, dac_mode) == 0x4c);
static_assert(__builtin_offsetof(RegisterMap, vid_proc_cfg) == 0x5c);
static_assert(__builtin_offsetof(RegisterMap, vid_screen_size) == 0x98);
static_assert(__builtin_offsetof(RegisterMap, vid_desktop_start_addr) == 0xe4);
static_assert(__builtin_offsetof(RegisterMap, vid_desktop_overlay_stride) == 0xe8);
}
