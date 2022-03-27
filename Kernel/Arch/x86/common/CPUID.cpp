/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/CPUID.h>

namespace Kernel {

StringView cpu_feature_to_string_view(CPUFeature::Type const& feature)
{
    if (feature == CPUFeature::SSE3)
        return "sse3"sv;
    if (feature == CPUFeature::PCLMULQDQ)
        return "pclmulqdq"sv;
    if (feature == CPUFeature::DTES64)
        return "dtes64"sv;
    if (feature == CPUFeature::MONITOR)
        return "monitor"sv;
    if (feature == CPUFeature::DS_CPL)
        return "ds_cpl"sv;
    if (feature == CPUFeature::VMX)
        return "vmx"sv;
    if (feature == CPUFeature::SMX)
        return "smx"sv;
    if (feature == CPUFeature::EST)
        return "est"sv;
    if (feature == CPUFeature::TM2)
        return "tm2"sv;
    if (feature == CPUFeature::SSSE3)
        return "ssse3"sv;
    // NOTE: This is called cid on Linux, but CNXT_ID in the Intel manual.
    if (feature == CPUFeature::CNXT_ID)
        return "cnxt_id"sv;
    if (feature == CPUFeature::SDBG)
        return "sdbg"sv;
    if (feature == CPUFeature::FMA)
        return "fma"sv;
    if (feature == CPUFeature::CX16)
        return "cx16"sv;
    if (feature == CPUFeature::XTPR)
        return "xtpr"sv;
    if (feature == CPUFeature::PDCM)
        return "pdcm"sv;
    if (feature == CPUFeature::PCID)
        return "pcid"sv;
    if (feature == CPUFeature::DCA)
        return "dca"sv;
    if (feature == CPUFeature::SSE4_1)
        return "sse4_1"sv;
    if (feature == CPUFeature::SSE4_2)
        return "sse4_2"sv;
    if (feature == CPUFeature::X2APIC)
        return "x2apic"sv;
    if (feature == CPUFeature::MOVBE)
        return "movbe"sv;
    if (feature == CPUFeature::POPCNT)
        return "popcnt"sv;
    // NOTE: This is called tsc_deadline_timer on Linux, but TSC_DEADLINE in the Intel manual.
    if (feature == CPUFeature::TSC_DEADLINE)
        return "tsc_deadline"sv;
    if (feature == CPUFeature::AES)
        return "aes"sv;
    if (feature == CPUFeature::XSAVE)
        return "xsave"sv;
    if (feature == CPUFeature::OSXSAVE)
        return "osxsave"sv;
    if (feature == CPUFeature::AVX)
        return "avx"sv;
    if (feature == CPUFeature::F16C)
        return "f16c"sv;
    if (feature == CPUFeature::RDRAND)
        return "rdrand"sv;
    if (feature == CPUFeature::HYPERVISOR)
        return "hypervisor"sv;
    if (feature == CPUFeature::FPU)
        return "fpu"sv;
    if (feature == CPUFeature::VME)
        return "vme"sv;
    if (feature == CPUFeature::DE)
        return "de"sv;
    if (feature == CPUFeature::PSE)
        return "pse"sv;
    if (feature == CPUFeature::TSC)
        return "tsc"sv;
    if (feature == CPUFeature::MSR)
        return "msr"sv;
    if (feature == CPUFeature::PAE)
        return "pae"sv;
    if (feature == CPUFeature::MCE)
        return "mce"sv;
    if (feature == CPUFeature::CX8)
        return "cx8"sv;
    if (feature == CPUFeature::APIC)
        return "apic"sv;
    if (feature == CPUFeature::SEP)
        return "sep"sv;
    if (feature == CPUFeature::MTRR)
        return "mtrr"sv;
    if (feature == CPUFeature::PGE)
        return "pge"sv;
    if (feature == CPUFeature::MCA)
        return "mca"sv;
    if (feature == CPUFeature::CMOV)
        return "cmov"sv;
    if (feature == CPUFeature::PAT)
        return "pat"sv;
    if (feature == CPUFeature::PSE36)
        return "pse36"sv;
    if (feature == CPUFeature::PSN)
        return "psn"sv;
    if (feature == CPUFeature::CLFLUSH)
        return "clflush"sv;
    if (feature == CPUFeature::DS)
        return "ds"sv;
    if (feature == CPUFeature::ACPI)
        return "acpi"sv;
    if (feature == CPUFeature::MMX)
        return "mmx"sv;
    if (feature == CPUFeature::FXSR)
        return "fxsr"sv;
    if (feature == CPUFeature::SSE)
        return "sse"sv;
    if (feature == CPUFeature::SSE2)
        return "sse2"sv;
    if (feature == CPUFeature::SS)
        return "ss"sv;
    if (feature == CPUFeature::HTT)
        return "htt"sv;
    if (feature == CPUFeature::TM)
        return "tm"sv;
    if (feature == CPUFeature::IA64)
        return "ia64"sv;
    if (feature == CPUFeature::PBE)
        return "pbe"sv;
    if (feature == CPUFeature::SMEP)
        return "smep"sv;
    if (feature == CPUFeature::RDSEED)
        return "rdseed"sv;
    if (feature == CPUFeature::SMAP)
        return "smap"sv;
    if (feature == CPUFeature::UMIP)
        return "umip"sv;
    if (feature == CPUFeature::SYSCALL)
        return "syscall"sv;
    if (feature == CPUFeature::NX)
        return "nx"sv;
    if (feature == CPUFeature::RDTSCP)
        return "rdtscp"sv;
    if (feature == CPUFeature::LM)
        return "lm"sv;
    if (feature == CPUFeature::CONSTANT_TSC)
        return "constant_tsc"sv;
    if (feature == CPUFeature::NONSTOP_TSC)
        return "nonstop_tsc"sv;
    VERIFY_NOT_REACHED();
}

}
