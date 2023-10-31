/*
 * Copyright (c) 2022, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

// FIXME: Add equivalent datastructures for aarch64
VALIDATE_IS_X86();

namespace AK {

enum class RoundingMode : u16 {
    NEAREST = 0b00,
    DOWN = 0b01,
    UP = 0b10,
    TRUNC = 0b11
};

union X87ControlWord {
    u16 cw;
    struct {
        u16 mask_invalid : 1;              // IM
        u16 mask_denorm : 1;               // DM
        u16 mask_zero_div : 1;             // ZM
        u16 mask_overflow : 1;             // OM
        u16 mask_underflow : 1;            // UM
        u16 mask_precision : 1;            // PM
        u16 : 2;                           // unused
        u16 precision : 2;                 // PC
        RoundingMode rounding_control : 2; // RC
        u16 infinity_control : 1;          // X
        u16 : 3;                           // unused
    };
};
static_assert(sizeof(X87ControlWord) == sizeof(u16));

union MXCSR {
    u32 mxcsr;
    struct {
        u32 invalid_operation_flag : 1;    // IE
        u32 denormal_operation_flag : 1;   // DE
        u32 divide_by_zero_flag : 1;       // ZE
        u32 overflow_flag : 1;             // OE
        u32 underflow_flag : 1;            // UE
        u32 precision_flag : 1;            // PE
        u32 denormals_are_zero : 1;        // DAZ
        u32 invalid_operation_mask : 1;    // IM
        u32 denormal_operation_mask : 1;   // DM
        u32 divide_by_zero_mask : 1;       // ZM
        u32 overflow_mask : 1;             // OM
        u32 underflow_mask : 1;            // UM
        u32 precision_mask : 1;            // PM
        RoundingMode rounding_control : 2; // RC
        u32 flush_to_zero : 1;             // FTZ
        u32 __reserved : 16;
    };
};
static_assert(sizeof(MXCSR) == sizeof(u32));

ALWAYS_INLINE X87ControlWord get_cw_x87()
{
    X87ControlWord control_word;
    asm("fnstcw %0"
        : "=m"(control_word));
    return control_word;
}
ALWAYS_INLINE void set_cw_x87(X87ControlWord control_word)
{
    asm("fldcw %0" ::"m"(control_word));
}

ALWAYS_INLINE MXCSR get_mxcsr()
{
    MXCSR mxcsr;
    asm("stmxcsr %0"
        : "=m"(mxcsr));
    return mxcsr;
}
ALWAYS_INLINE void set_mxcsr(MXCSR mxcsr)
{
    asm("ldmxcsr %0" ::"m"(mxcsr));
}

class X87RoundingModeScope {
public:
    X87RoundingModeScope(RoundingMode rounding_mode)
    {
        m_cw = get_cw_x87();
        auto cw = m_cw;
        cw.rounding_control = rounding_mode;
        set_cw_x87(cw);
    }
    ~X87RoundingModeScope()
    {
        set_cw_x87(m_cw);
    }

private:
    X87ControlWord m_cw;
};

class SSERoundingModeScope {
public:
    SSERoundingModeScope(RoundingMode rounding_mode)
    {
        m_mxcsr = get_mxcsr();
        auto mxcsr = m_mxcsr;
        mxcsr.rounding_control = rounding_mode;
        set_mxcsr(mxcsr);
    }
    ~SSERoundingModeScope()
    {
        set_mxcsr(m_mxcsr);
    }

private:
    MXCSR m_mxcsr;
};

}
