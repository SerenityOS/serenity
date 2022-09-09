/*
 * Copyright (c) 2022, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SoftVPU.h"
#include "SoftCPU.h"
#include <AK/SIMDMath.h>

namespace UserspaceEmulator {

void SoftVPU::PREFETCHTNTA(X86::Instruction const&) { TODO(); }
void SoftVPU::PREFETCHT0(X86::Instruction const&) { TODO(); }
void SoftVPU::PREFETCHT1(X86::Instruction const&) { TODO(); }
void SoftVPU::PREFETCHT2(X86::Instruction const&) { TODO(); }

void SoftVPU::LDMXCSR(X86::Instruction const& insn)
{
    // FIXME: Shadows
    m_mxcsr.mxcsr = insn.modrm().read32(m_cpu, insn).value();

    // #GP - General Protection Fault
    VERIFY((m_mxcsr.mxcsr & 0xFFFF'0000) == 0);

    // Just let the host's SSE (or if not available x87) handle the rounding for us
    // We do not want to accidentally raise an FP-Exception on the host, so we
    // mask all exceptions
#ifdef __SSE__
    AK::MXCSR temp = m_mxcsr;
    temp.invalid_operation_mask = 1;
    temp.denormal_operation_mask = 1;
    temp.divide_by_zero_mask = 1;
    temp.overflow_mask = 1;
    temp.underflow_mask = 1;
    temp.precision_mask = 1;
    AK::set_mxcsr(temp);
#else
    // FIXME: This will mess with x87-land, because it uses the same trick, and
    //        Does not know of us doing this
    AK::X87ControlWord cw { 0x037F };
    cw.rounding_control = m_mxcsr.rounding_control;
    AK::set_cw_x87(cw);
#endif
}
void SoftVPU::STMXCSR(X86::Instruction const& insn)
{
    // FIXME: Shadows
    insn.modrm().write32(m_cpu, insn, ValueWithShadow<u32>::create_initialized(m_mxcsr.mxcsr));
}

void SoftVPU::MOVUPS_xmm1_xmm2m128(X86::Instruction const& insn)
{
    u8 xmm1 = insn.modrm().reg();
    if (insn.modrm().is_register()) {
        m_xmm[xmm1] = m_xmm[insn.modrm().rm()];
    } else {
        // FIXME: Shadows
        m_xmm[xmm1].ps = bit_cast<f32x4>(insn.modrm().read128(m_cpu, insn).value());
    }
}
void SoftVPU::MOVSS_xmm1_xmm2m32(X86::Instruction const& insn)
{
    u8 xmm1 = insn.modrm().reg();
    if (insn.modrm().is_register()) {
        m_xmm[xmm1].ps[0] = m_xmm[insn.modrm().rm()].ps[0];
    } else {
        // FIXME: Shadows
        m_xmm[xmm1].ps[0] = bit_cast<float>(insn.modrm().read32(m_cpu, insn).value());
    }
}
void SoftVPU::MOVUPS_xmm1m128_xmm2(X86::Instruction const& insn)
{
    u8 xmm2 = insn.modrm().reg();
    if (insn.modrm().is_register()) {
        m_xmm[insn.modrm().rm()] = m_xmm[xmm2];
    } else {
        // FIXME: Shadows
        u128 temp = bit_cast<u128>(m_xmm[xmm2]);
        insn.modrm().write128(m_cpu, insn, ValueWithShadow<u128>::create_initialized(temp));
    }
}
void SoftVPU::MOVSS_xmm1m32_xmm2(X86::Instruction const& insn)
{
    u8 xmm2 = insn.modrm().reg();
    if (insn.modrm().is_register()) {
        m_xmm[insn.modrm().rm()].ps[0] = m_xmm[xmm2].ps[0];
    } else {
        // FIXME: Shadows
        u32 temp = bit_cast<u32>(m_xmm[xmm2].ps[0]);
        insn.modrm().write32(m_cpu, insn, ValueWithShadow<u32>::create_initialized(temp));
    }
}
void SoftVPU::MOVLPS_xmm1_xmm2m64(X86::Instruction const& insn)
{
    u8 xmm1 = insn.modrm().reg();
    if (insn.modrm().is_register()) {
        // Note: MOVHLPS
        m_xmm[xmm1].puqw[0] = m_xmm[insn.modrm().rm()].puqw[1];
    } else {
        // FIXME: Shadows
        // Note: Technically we are transferring two packed floats not a quad word
        m_xmm[xmm1].puqw[0] = insn.modrm().read64(m_cpu, insn).value();
    }
}
void SoftVPU::MOVLPS_m64_xmm2(X86::Instruction const& insn)
{
    u8 xmm2 = insn.modrm().reg();
    // FIXME: This might not hold true for SSE2 or later
    VERIFY(!insn.modrm().is_register());
    // Note: Technically we are transferring two packed floats not a quad word
    insn.modrm().write64(m_cpu, insn, ValueWithShadow<u64>::create_initialized(m_xmm[xmm2].puqw[0]));
}

void SoftVPU::UNPCKLPS_xmm1_xmm2m128(X86::Instruction const& insn)
{
    f32x4& xmm1 = m_xmm[insn.modrm().reg()].ps;
    f32x4 xmm2m128;

    if (insn.modrm().is_register()) {
        xmm2m128 = m_xmm[insn.modrm().rm()].ps;
    } else {
        // FIXME: Shadows
        xmm2m128 = bit_cast<f32x4>(insn.modrm().read128(m_cpu, insn).value());
    }
    f32x4 dest;
    if (insn.modrm().is_register()) {
        xmm2m128 = m_xmm[insn.modrm().rm()].ps;
    } else {
        // FIXME: Shadows
        xmm2m128 = bit_cast<f32x4>(insn.modrm().read128(m_cpu, insn).value());
    }

    dest[0] = xmm1[0];
    dest[1] = xmm2m128[0];
    dest[2] = xmm1[1];
    dest[3] = xmm2m128[1];

    m_xmm[insn.modrm().reg()].ps = dest;
}
void SoftVPU::UNPCKHPS_xmm1_xmm2m128(X86::Instruction const& insn)
{
    f32x4 xmm1 = m_xmm[insn.modrm().reg()].ps;
    f32x4 xmm2m128;

    if (insn.modrm().is_register()) {
        xmm2m128 = m_xmm[insn.modrm().rm()].ps;
    } else {
        // FIXME: Shadows
        xmm2m128 = bit_cast<f32x4>(insn.modrm().read128(m_cpu, insn).value());
    }
    f32x4 dest;
    dest[0] = xmm1[2];
    dest[1] = xmm2m128[2];
    dest[2] = xmm1[3];
    dest[3] = xmm2m128[3];

    m_xmm[insn.modrm().reg()].ps = dest;
}

void SoftVPU::MOVHPS_xmm1_xmm2m64(X86::Instruction const& insn)
{
    u8 xmm1 = insn.modrm().reg();
    if (insn.modrm().is_register()) {
        // Note: MOVLHPS
        m_xmm[xmm1].puqw[1] = m_xmm[insn.modrm().rm()].puqw[0];
    } else {
        // FIXME: Shadows
        // Note: Technically we are transferring two packed floats not a quad word
        m_xmm[xmm1].puqw[1] = insn.modrm().read64(m_cpu, insn).value();
    }
}
void SoftVPU::MOVHPS_m64_xmm2(X86::Instruction const& insn)
{
    u8 xmm1 = insn.modrm().reg();
    VERIFY(!insn.modrm().is_register());
    // Note: Technically we are transferring two packed floats not a quad word
    insn.modrm().write64(m_cpu, insn, ValueWithShadow<u64>::create_initialized(m_xmm[xmm1].puqw[1]));
}
void SoftVPU::MOVAPS_xmm1_xmm2m128(X86::Instruction const& insn)
{
    u8 xmm1 = insn.modrm().reg();
    if (insn.modrm().is_register()) {
        m_xmm[xmm1] = m_xmm[insn.modrm().rm()];
    } else {
        // FIXME: Alignment-check 16
        auto temp = insn.modrm().read128(m_cpu, insn);
        m_xmm[xmm1].ps = bit_cast<f32x4>(temp.value());
    }
}
void SoftVPU::MOVAPS_xmm1m128_xmm2(X86::Instruction const& insn)
{
    u8 xmm2 = insn.modrm().reg();
    if (insn.modrm().is_register()) {
        m_xmm[insn.modrm().rm()] = m_xmm[xmm2];
    } else {
        // FIXME: Alignment-check 16
        u128 temp = bit_cast<u128>(m_xmm[xmm2]);
        insn.modrm().write128(m_cpu, insn, ValueWithShadow<u128>::create_initialized(temp));
    }
}

void SoftVPU::CVTPI2PS_xmm1_mm2m64(X86::Instruction const& insn)
{
    // FIXME: Raise Precision
    // FIXME: Honor Rounding control
    u8 xmm1 = insn.modrm().reg();
    if (insn.modrm().is_register()) {
        i32x2 mm = m_cpu.mmx_get(insn.modrm().rm()).v32;
        m_xmm[xmm1].ps[0] = mm[0];
        m_xmm[xmm1].ps[1] = mm[1];
    } else {
        // FIXME: Shadows
        i32x2 m64 = bit_cast<i32x2>(insn.modrm().read64(m_cpu, insn).value());
        m_xmm[xmm1].ps[0] = m64[0];
        m_xmm[xmm1].ps[1] = m64[1];
    }
}
void SoftVPU::CVTSI2SS_xmm1_rm32(X86::Instruction const& insn)
{
    // FIXME: Raise Precision
    // FIXME: Shadows
    // FIXME: Honor Rounding Control
    m_xmm[insn.modrm().reg()].ps[0] = (i32)insn.modrm().read32(m_cpu, insn).value();
}

void SoftVPU::MOVNTPS_xmm1m128_xmm2(X86::Instruction const&) { TODO(); }

void SoftVPU::CVTTPS2PI_mm1_xmm2m64(X86::Instruction const&) { TODO(); }
void SoftVPU::CVTTSS2SI_r32_xmm2m32(X86::Instruction const& insn)
{
    // FIXME: Raise Invalid, Precision
    float value;
    if (insn.modrm().is_register())
        value = m_xmm[insn.modrm().rm()].ps[0];
    else
        value = bit_cast<float>(insn.modrm().read32(m_cpu, insn).value());

    m_cpu.gpr32(insn.reg32()) = ValueWithShadow<u32>::create_initialized((u32)(i32)truncf(value));
}
void SoftVPU::CVTPS2PI_xmm1_mm2m64(X86::Instruction const&) { TODO(); }
void SoftVPU::CVTSS2SI_r32_xmm2m32(X86::Instruction const& insn)
{
    // FIXME: Raise Invalid, Precision
    insn.modrm().write32(m_cpu, insn,
        ValueWithShadow<u32>::create_initialized(static_cast<i32>(m_xmm[insn.modrm().reg()].ps[0])));
}

void SoftVPU::UCOMISS_xmm1_xmm2m32(X86::Instruction const& insn)
{
    float xmm1 = m_xmm[insn.modrm().reg()].ps[0];
    float xmm2m32;
    if (insn.modrm().is_register())
        xmm2m32 = m_xmm[insn.modrm().rm()].ps[0];
    else
        xmm2m32 = bit_cast<float>(insn.modrm().read32(m_cpu, insn).value());
    // FIXME: Raise Invalid on SNaN
    if (isnan(xmm1) || isnan(xmm2m32)) {
        m_cpu.set_zf(true);
        m_cpu.set_pf(true);
        m_cpu.set_cf(true);
    } else {
        m_cpu.set_zf(xmm1 == xmm2m32);
        m_cpu.set_pf(false);
        m_cpu.set_cf(xmm1 < xmm2m32);
    }
    m_cpu.set_of(false);
    m_cpu.set_af(false);
    m_cpu.set_sf(false);
}
void SoftVPU::COMISS_xmm1_xmm2m32(X86::Instruction const& insn)
{
    // FIXME: Raise on QNaN
    UCOMISS_xmm1_xmm2m32(insn);
}

void SoftVPU::MOVMSKPS_reg_xmm(X86::Instruction const& insn)
{
    VERIFY(insn.modrm().is_register());
    u8 mask = 0;
    f32x4 xmm = m_xmm[insn.modrm().rm()].ps;
    mask |= signbit(xmm[0]) << 0;
    mask |= signbit(xmm[1]) << 1;
    mask |= signbit(xmm[2]) << 2;
    mask |= signbit(xmm[3]) << 3;

    m_cpu.gpr32(insn.reg32()) = ValueWithShadow<u32>::create_initialized(mask);
}

void SoftVPU::SQRTPS_xmm1_xmm2m128(X86::Instruction const& insn)
{
    // FIXME: Raise Invalid, Precision, Denormal
    f32x4 xmm2m128;

    if (insn.modrm().is_register()) {
        xmm2m128 = m_xmm[insn.modrm().rm()].ps;
    } else {
        // FIXME: Shadows
        xmm2m128 = bit_cast<f32x4>(insn.modrm().read128(m_cpu, insn).value());
    }

    m_xmm[insn.modrm().reg()].ps = sqrt(xmm2m128);
}
void SoftVPU::SQRTSS_xmm1_xmm2m32(X86::Instruction const& insn)
{
    // FIXME: Raise Invalid, Precision, Denormal
    float xmm2m32;

    if (insn.modrm().is_register()) {
        xmm2m32 = m_xmm[insn.modrm().rm()].ps[0];
    } else {
        // FIXME: Shadows
        xmm2m32 = bit_cast<float>(insn.modrm().read32(m_cpu, insn).value());
    }

    m_xmm[insn.modrm().reg()].ps[0] = AK::sqrt(xmm2m32);
}
void SoftVPU::RSQRTPS_xmm1_xmm2m128(X86::Instruction const& insn)
{
    f32x4 xmm2m128;
    if (insn.modrm().is_register()) {
        xmm2m128 = m_xmm[insn.modrm().rm()].ps;
    } else {
        // FIXME: Shadows
        xmm2m128 = bit_cast<f32x4>(insn.modrm().read128(m_cpu, insn).value());
    }

    m_xmm[insn.modrm().reg()].ps = rsqrt(xmm2m128);
}
void SoftVPU::RSQRTSS_xmm1_xmm2m32(X86::Instruction const& insn)
{
    float xmm2m32;
    if (insn.modrm().is_register()) {
        xmm2m32 = m_xmm[insn.modrm().rm()].ps[0];
    } else {
        // FIXME: Shadows
        xmm2m32 = bit_cast<float>(insn.modrm().read32(m_cpu, insn).value());
    }

    m_xmm[insn.modrm().reg()].ps[0] = AK::rsqrt(xmm2m32);
}

void SoftVPU::RCPPS_xmm1_xmm2m128(X86::Instruction const& insn)
{
    f32x4 xmm2m128;
    if (insn.modrm().is_register()) {
        xmm2m128 = m_xmm[insn.modrm().rm()].ps;
    } else {
        // FIXME: Shadows
        xmm2m128 = bit_cast<f32x4>(insn.modrm().read128(m_cpu, insn).value());
    }

    m_xmm[insn.modrm().reg()].ps = 1.f / xmm2m128;
}
void SoftVPU::RCPSS_xmm1_xmm2m32(X86::Instruction const& insn)
{
    float xmm2m32;
    if (insn.modrm().is_register()) {
        xmm2m32 = m_xmm[insn.modrm().rm()].ps[0];
    } else {
        // FIXME: Shadows
        xmm2m32 = bit_cast<float>(insn.modrm().read32(m_cpu, insn).value());
    }

    m_xmm[insn.modrm().reg()].ps[0] = 1.f / xmm2m32;
}

void SoftVPU::ANDPS_xmm1_xmm2m128(X86::Instruction const& insn)
{
    u32x4 xmm2m128;
    if (insn.modrm().is_register()) {
        xmm2m128 = m_xmm[insn.modrm().rm()].pudw;
    } else {
        // FIXME: Shadows
        xmm2m128 = bit_cast<u32x4>(insn.modrm().read128(m_cpu, insn).value());
    }

    m_xmm[insn.modrm().reg()].pudw &= xmm2m128;
}
void SoftVPU::ANDNPS_xmm1_xmm2m128(X86::Instruction const& insn)
{
    u32x4 xmm2m128;
    if (insn.modrm().is_register()) {
        xmm2m128 = m_xmm[insn.modrm().rm()].pudw;
    } else {
        // FIXME: Shadows
        xmm2m128 = bit_cast<u32x4>(insn.modrm().read128(m_cpu, insn).value());
    }

    u32x4& xmm1 = m_xmm[insn.modrm().reg()].pudw;
    xmm1 = ~xmm1 & xmm2m128;
}
void SoftVPU::ORPS_xmm1_xmm2m128(X86::Instruction const& insn)
{
    u32x4 xmm2m128;
    if (insn.modrm().is_register()) {
        xmm2m128 = m_xmm[insn.modrm().rm()].pudw;
    } else {
        // FIXME: Shadows
        xmm2m128 = bit_cast<u32x4>(insn.modrm().read128(m_cpu, insn).value());
    }

    m_xmm[insn.modrm().reg()].pudw |= xmm2m128;
}
void SoftVPU::XORPS_xmm1_xmm2m128(X86::Instruction const& insn)
{
    u32x4 xmm2m128;
    if (insn.modrm().is_register()) {
        xmm2m128 = m_xmm[insn.modrm().rm()].pudw;
    } else {
        // FIXME: Shadows
        xmm2m128 = bit_cast<u32x4>(insn.modrm().read128(m_cpu, insn).value());
    }

    m_xmm[insn.modrm().reg()].pudw ^= xmm2m128;
}

void SoftVPU::ADDPS_xmm1_xmm2m128(X86::Instruction const& insn)
{
    // Raise Overflow, Underflow, Invalid, Precision, Denormal
    f32x4 xmm2m128;
    if (insn.modrm().is_register()) {
        xmm2m128 = m_xmm[insn.modrm().rm()].ps;
    } else {
        // FIXME: Shadows
        xmm2m128 = bit_cast<f32x4>(insn.modrm().read128(m_cpu, insn).value());
    }

    m_xmm[insn.modrm().reg()].ps += xmm2m128;
}
void SoftVPU::ADDSS_xmm1_xmm2m32(X86::Instruction const& insn)
{
    // Raise Overflow, Underflow, Invalid, Precision, Denormal
    float xmm2m32;
    if (insn.modrm().is_register()) {
        xmm2m32 = m_xmm[insn.modrm().rm()].ps[0];
    } else {
        // FIXME: Shadows
        xmm2m32 = bit_cast<float>(insn.modrm().read32(m_cpu, insn).value());
    }

    m_xmm[insn.modrm().reg()].ps[0] += xmm2m32;
}

void SoftVPU::MULPS_xmm1_xmm2m128(X86::Instruction const& insn)
{
    // Raise Overflow, Underflow, Invalid, Precision, Denormal
    f32x4 xmm2m128;
    if (insn.modrm().is_register()) {
        xmm2m128 = m_xmm[insn.modrm().rm()].ps;
    } else {
        // FIXME: Shadows
        xmm2m128 = bit_cast<f32x4>(insn.modrm().read128(m_cpu, insn).value());
    }

    m_xmm[insn.modrm().reg()].ps *= xmm2m128;
}
void SoftVPU::MULSS_xmm1_xmm2m32(X86::Instruction const& insn)
{
    // Raise Overflow, Underflow, Invalid, Precision, Denormal
    float xmm1 = m_xmm[insn.modrm().reg()].ps[0];
    float xmm2m32;

    if (insn.modrm().is_register()) {
        xmm2m32 = m_xmm[insn.modrm().rm()].ps[0];
    } else {
        // FIXME: Shadows
        xmm2m32 = bit_cast<float>(insn.modrm().read32(m_cpu, insn).value());
    }
    xmm1 *= xmm2m32;

    m_xmm[insn.modrm().reg()].ps[0] *= xmm1;
}

void SoftVPU::SUBPS_xmm1_xmm2m128(X86::Instruction const& insn)
{
    // Raise Overflow, Underflow, Invalid, Precision, Denormal
    f32x4 xmm2m128;

    if (insn.modrm().is_register()) {
        xmm2m128 = m_xmm[insn.modrm().rm()].ps;
    } else {
        // FIXME: Shadows
        xmm2m128 = bit_cast<f32x4>(insn.modrm().read128(m_cpu, insn).value());
    }

    m_xmm[insn.modrm().reg()].ps -= xmm2m128;
}
void SoftVPU::SUBSS_xmm1_xmm2m32(X86::Instruction const& insn)
{
    // Raise Overflow, Underflow, Invalid, Precision, Denormal
    float xmm2m32;

    if (insn.modrm().is_register()) {
        xmm2m32 = m_xmm[insn.modrm().rm()].ps[0];
    } else {
        // FIXME: Shadows
        xmm2m32 = bit_cast<float>(insn.modrm().read32(m_cpu, insn).value());
    }

    m_xmm[insn.modrm().reg()].ps[0] -= xmm2m32;
}

void SoftVPU::MINPS_xmm1_xmm2m128(X86::Instruction const& insn)
{
    // FIXME: Raise Invalid (including QNaN Source Operand), Denormal
    f32x4 xmm1 = m_xmm[insn.modrm().reg()].ps;
    f32x4 xmm2m128;

    if (insn.modrm().is_register()) {
        xmm2m128 = m_xmm[insn.modrm().rm()].ps;
    } else {
        // FIXME: Shadows
        xmm2m128 = bit_cast<f32x4>(insn.modrm().read128(m_cpu, insn).value());
    }

    for (auto i = 0; i < 4; ++i) {
        // When only one is NaN or both are 0.0s (of either sign), or
        // FIXME: xmm2m32 is SNaN
        // xmm2m32 is returned unchanged
        if (isnan(xmm1[i]) || isnan(xmm2m128[i]) || xmm1[i] == xmm2m128[i])
            xmm1[i] = xmm2m128[i];
        else
            xmm1[i] = min(xmm1[i], xmm2m128[i]);
    }

    m_xmm[insn.modrm().reg()].ps = xmm1;
}
void SoftVPU::MINSS_xmm1_xmm2m32(X86::Instruction const& insn)
{
    // FIXME: Raise Invalid (Including QNaN Source Operand), Denormal
    float xmm1 = m_xmm[insn.modrm().reg()].ps[0];
    float xmm2m32;

    if (insn.modrm().is_register()) {
        xmm2m32 = m_xmm[insn.modrm().rm()].ps[0];
    } else {
        // FIXME: Shadows
        xmm2m32 = bit_cast<float>(insn.modrm().read32(m_cpu, insn).value());
    }
    // When only one is NaN or both are 0.0s (of either sign), or
    // FIXME: xmm2m32 is SNaN
    // xmm2m32 is returned unchanged
    if (isnan(xmm1) || isnan(xmm2m32) || xmm1 == xmm2m32)
        xmm1 = xmm2m32;
    else
        xmm1 = min(xmm1, xmm2m32);

    m_xmm[insn.modrm().reg()].ps[0] = xmm1;
}

void SoftVPU::DIVPS_xmm1_xmm2m128(X86::Instruction const& insn)
{
    // Raise Overflow, Underflow, Invalid, Divide-by-Zero, Precision, Denormal
    f32x4 xmm2m128;
    if (insn.modrm().is_register()) {
        xmm2m128 = m_xmm[insn.modrm().rm()].ps;
    } else {
        // FIXME: Shadows
        xmm2m128 = bit_cast<f32x4>(insn.modrm().read128(m_cpu, insn).value());
    }

    m_xmm[insn.modrm().reg()].ps /= xmm2m128;
}
void SoftVPU::DIVSS_xmm1_xmm2m32(X86::Instruction const& insn)
{
    // Raise Overflow, Underflow, Invalid, Divide-by-Zero, Precision, Denormal
    float xmm2m32;
    if (insn.modrm().is_register()) {
        xmm2m32 = m_xmm[insn.modrm().rm()].ps[0];
    } else {
        // FIXME: Shadows
        xmm2m32 = bit_cast<float>(insn.modrm().read32(m_cpu, insn).value());
    }

    m_xmm[insn.modrm().reg()].ps[0] /= xmm2m32;
}

void SoftVPU::MAXPS_xmm1_xmm2m128(X86::Instruction const& insn)
{
    // FIXME: Raise Invalid (including QNaN Source Operand), Denormal
    f32x4 xmm1 = m_xmm[insn.modrm().reg()].ps;
    f32x4 xmm2m128;

    if (insn.modrm().is_register()) {
        xmm2m128 = m_xmm[insn.modrm().rm()].ps;
    } else {
        // FIXME: Shadows
        xmm2m128 = bit_cast<f32x4>(insn.modrm().read128(m_cpu, insn).value());
    }

    for (auto i = 0; i < 4; ++i) {
        // When only one is NaN or both are 0.0s (of either sign), or
        // FIXME: xmm2m32 is SNaN
        // xmm2m32 is returned unchanged
        if (isnan(xmm1[i]) || isnan(xmm2m128[i]) || xmm1[i] == xmm2m128[i])
            xmm1[i] = xmm2m128[i];
        else
            xmm1[i] = max(xmm1[i], xmm2m128[i]);
    }

    m_xmm[insn.modrm().reg()].ps = xmm1;
}
void SoftVPU::MAXSS_xmm1_xmm2m32(X86::Instruction const& insn)
{
    // FIXME: Raise Invalid (Including QNaN Source Operand), Denormal
    float xmm1 = m_xmm[insn.modrm().reg()].ps[0];
    float xmm2m32;

    if (insn.modrm().is_register()) {
        xmm2m32 = m_xmm[insn.modrm().rm()].ps[0];
    } else {
        // FIXME: Shadows
        xmm2m32 = bit_cast<float>(insn.modrm().read32(m_cpu, insn).value());
    }
    // When only one is NaN or both are 0.0s (of either sign), or
    // FIXME: xmm2m32 is SNaN
    // xmm2m32 is returned unchanged
    if (isnan(xmm1) || isnan(xmm2m32) || xmm1 == xmm2m32)
        xmm1 = xmm2m32;
    else
        xmm1 = max(xmm1, xmm2m32);

    m_xmm[insn.modrm().reg()].ps[0] = xmm1;
}

void SoftVPU::PSHUFW_mm1_mm2m64_imm8(X86::Instruction const& insn)
{
    MMX src;
    if (insn.modrm().is_register()) {
        src = m_cpu.mmx_get(insn.modrm().rm());
    } else {
        // FIXME: Shadows
        src = bit_cast<MMX>(insn.modrm().read64(m_cpu, insn).value());
    }

    u8 order = insn.imm8();
    MMX dest;

    dest.v16u[0] = src.v16u[(order >> 0) & 0b11];
    dest.v16u[1] = src.v16u[(order >> 2) & 0b11];
    dest.v16u[2] = src.v16u[(order >> 4) & 0b11];
    dest.v16u[3] = src.v16u[(order >> 6) & 0b11];

    m_cpu.mmx_set(insn.modrm().reg(), dest);
}

void SoftVPU::CMPPS_xmm1_xmm2m128_imm8(X86::Instruction const& insn)
{
    // FIXME: Raise Denormal, Invalid Operation (QNaN dependent on imm8)
    XMM& xmm1 = m_xmm[insn.modrm().reg()];
    f32x4 xmm2m128;

    if (insn.modrm().is_register()) {
        xmm2m128 = m_xmm[insn.modrm().rm()].ps;
    } else {
        // FIXME: Shadows
        xmm2m128 = bit_cast<f32x4>(insn.modrm().read128(m_cpu, insn).value());
    }
    using enum ComparePredicate;
    switch ((ComparePredicate)insn.imm8()) {
    case EQ:
        xmm1.ps = xmm1.ps == xmm2m128;
        break;
    case LT:
        xmm1.ps = xmm1.ps < xmm2m128;
        break;
    case LE:
        xmm1.ps = xmm1.ps <= xmm2m128;
        break;
    case UNORD:
        for (auto i = 0; i < 4; ++i)
            xmm1.pudw[i] = 0xFFFF'FFFF * (isnan(xmm1.ps[i]) || isnan(xmm2m128[i]));
        break;
    case NEQ:
        xmm1.ps = xmm1.ps != xmm2m128;
        break;
    case NLT:
        xmm1.ps = xmm1.ps >= xmm2m128;
        break;
    case NLE:
        xmm1.ps = xmm1.ps > xmm2m128;
        break;
    case ORD:
        for (auto i = 0; i < 4; ++i)
            xmm1.pudw[i] = 0xFFFF'FFFF * (!isnan(xmm1.ps[i]) && !isnan(xmm2m128[i]));
        break;
    }
}
void SoftVPU::CMPSS_xmm1_xmm2m32_imm8(X86::Instruction const& insn)
{
    // FIXME: Raise Denormal, Invalid Operation (QNaN dependent on imm8)
    float xmm1 = m_xmm[insn.modrm().reg()].ps[0];
    float xmm2m128;
    bool res;

    if (insn.modrm().is_register()) {
        xmm2m128 = m_xmm[insn.modrm().rm()].ps[0];
    } else {
        // FIXME: Shadows
        xmm2m128 = bit_cast<float>(insn.modrm().read32(m_cpu, insn).value());
    }
    using enum ComparePredicate;
    switch ((ComparePredicate)insn.imm8()) {
    case EQ:
        res = xmm1 == xmm2m128;
        break;
    case LT:
        res = xmm1 < xmm2m128;
        break;
    case LE:
        res = xmm1 <= xmm2m128;
        break;
    case UNORD:
        res = isnan(xmm1) || isnan(xmm2m128);
        break;
    case NEQ:
        res = xmm1 != xmm2m128;
        break;
    case NLT:
        res = xmm1 >= xmm2m128;
        break;
    case NLE:
        res = xmm1 > xmm2m128;
        break;
    case ORD:
        res = !isnan(xmm1) && !isnan(xmm2m128);
        break;
    }

    m_xmm[insn.modrm().reg()].pudw[0] = 0xFFFF'FFFF * res;
}

void SoftVPU::PINSRW_mm1_r32m16_imm8(X86::Instruction const&) { TODO(); }
void SoftVPU::PINSRW_xmm1_r32m16_imm8(X86::Instruction const&) { TODO(); }
void SoftVPU::PEXTRW_reg_mm1_imm8(X86::Instruction const&) { TODO(); }
void SoftVPU::PEXTRW_reg_xmm1_imm8(X86::Instruction const&) { TODO(); }

void SoftVPU::SHUFPS_xmm1_xmm2m128_imm8(X86::Instruction const& insn)
{
    f32x4 src;
    if (insn.modrm().is_register()) {
        src = m_xmm[insn.modrm().rm()].ps;
    } else {
        // FIXME: Shadows
        src = bit_cast<f32x4>(insn.modrm().read128(m_cpu, insn).value());
    }

    u8 order = insn.imm8();
    f32x4 dest;
    dest[0] = src[(order >> 0) & 0b11];
    dest[1] = src[(order >> 2) & 0b11];
    dest[2] = src[(order >> 4) & 0b11];
    dest[3] = src[(order >> 6) & 0b11];

    m_xmm[insn.modrm().reg()].ps = dest;
}

void SoftVPU::PMOVMSKB_reg_mm1(X86::Instruction const&) { TODO(); }
void SoftVPU::PMOVMSKB_reg_xmm1(X86::Instruction const& insn)
{
    VERIFY(insn.modrm().is_register());
    XMM src = m_xmm[insn.modrm().rm()];

    u32 dest = 0;
    for (int i = 0; i < 16; ++i)
        dest |= (src.pub[i] >> 7) << i;

    m_cpu.gpr32(insn.reg32()) = ValueWithShadow<u32>::create_initialized(dest);
}

void SoftVPU::PMINUB_mm1_mm2m64(X86::Instruction const&) { TODO(); }
void SoftVPU::PMINUB_xmm1_xmm2m128(X86::Instruction const&) { TODO(); }

void SoftVPU::PMAXUB_mm1_mm2m64(X86::Instruction const&) { TODO(); }
void SoftVPU::PMAXUB_xmm1_xmm2m128(X86::Instruction const&) { TODO(); }

void SoftVPU::PAVGB_mm1_mm2m64(X86::Instruction const&) { TODO(); }
void SoftVPU::PAVGB_xmm1_xmm2m128(X86::Instruction const&) { TODO(); }

void SoftVPU::PAVGW_mm1_mm2m64(X86::Instruction const&) { TODO(); }
void SoftVPU::PAVGW_xmm1_xmm2m128(X86::Instruction const&) { TODO(); }

void SoftVPU::PMULHUW_mm1_mm2m64(X86::Instruction const&) { TODO(); }
void SoftVPU::PMULHUW_xmm1_xmm2m64(X86::Instruction const&) { TODO(); }

void SoftVPU::MOVNTQ_m64_mm1(X86::Instruction const&) { TODO(); }

void SoftVPU::PMINSB_mm1_mm2m64(X86::Instruction const&) { TODO(); }
void SoftVPU::PMINSB_xmm1_xmm2m128(X86::Instruction const&) { TODO(); }

void SoftVPU::PMAXSB_mm1_mm2m64(X86::Instruction const&) { TODO(); }
void SoftVPU::PMAXSB_xmm1_xmm2m128(X86::Instruction const&) { TODO(); }

void SoftVPU::PSADBB_mm1_mm2m64(X86::Instruction const&) { TODO(); }
void SoftVPU::PSADBB_xmm1_xmm2m128(X86::Instruction const&) { TODO(); }

void SoftVPU::MASKMOVQ_mm1_mm2m64(X86::Instruction const&) { TODO(); }
}
