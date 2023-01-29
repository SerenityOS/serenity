/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86_64/CPUID.h>

namespace Kernel {

StringView cpu_feature_to_name(CPUFeature::Type const& feature)
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
    if (feature == CPUFeature::FSGSBASE)
        return "fsgsbase"sv;
    if (feature == CPUFeature::TSC_ADJUST)
        return "tsc_adjust"sv;
    if (feature == CPUFeature::SGX)
        return "sgx"sv;
    if (feature == CPUFeature::BMI1)
        return "bmi1"sv;
    if (feature == CPUFeature::HLE)
        return "hle"sv;
    if (feature == CPUFeature::AVX2)
        return "avx2"sv;
    if (feature == CPUFeature::FDP_EXCPTN_ONLY)
        return "fdp_excptn_only"sv;
    if (feature == CPUFeature::SMEP)
        return "smep"sv;
    if (feature == CPUFeature::BMI2)
        return "bmi2"sv;
    if (feature == CPUFeature::ERMS)
        return "erms"sv;
    if (feature == CPUFeature::INVPCID)
        return "invpcid"sv;
    if (feature == CPUFeature::RTM)
        return "rtm"sv;
    if (feature == CPUFeature::PQM)
        return "pqm"sv;
    if (feature == CPUFeature::ZERO_FCS_FDS)
        return "zero_fcs_fds"sv;
    if (feature == CPUFeature::MPX)
        return "mpx"sv;
    if (feature == CPUFeature::PQE)
        return "pqe"sv;
    if (feature == CPUFeature::AVX512_F)
        return "avx512_f"sv;
    if (feature == CPUFeature::AVX512_DQ)
        return "avx512_dq"sv;
    if (feature == CPUFeature::RDSEED)
        return "rdseed"sv;
    if (feature == CPUFeature::ADX)
        return "adx"sv;
    if (feature == CPUFeature::SMAP)
        return "smap"sv;
    if (feature == CPUFeature::AVX512_IFMA)
        return "avx512_ifma"sv;
    if (feature == CPUFeature::PCOMMIT)
        return "pcommit"sv;
    if (feature == CPUFeature::CLFLUSHOPT)
        return "clflushopt"sv;
    if (feature == CPUFeature::CLWB)
        return "clwb"sv;
    if (feature == CPUFeature::INTEL_PT)
        return "intel_pt"sv;
    if (feature == CPUFeature::AVX512_PF)
        return "avx512_pf"sv;
    if (feature == CPUFeature::AVX512_ER)
        return "avx512_er"sv;
    if (feature == CPUFeature::AVX512_CD)
        return "avx512_cd"sv;
    if (feature == CPUFeature::SHA)
        return "sha"sv;
    if (feature == CPUFeature::AVX512_BW)
        return "avx512_bw"sv;
    if (feature == CPUFeature::AVX512_VL)
        return "avx512_vl"sv;
    if (feature == CPUFeature::PREFETCHWT1)
        return "prefetchwt1"sv;
    if (feature == CPUFeature::AVX512_VBMI)
        return "avx512_vbmi"sv;
    if (feature == CPUFeature::UMIP)
        return "umip"sv;
    if (feature == CPUFeature::PKU)
        return "pku"sv;
    if (feature == CPUFeature::OSPKE)
        return "ospke"sv;
    if (feature == CPUFeature::WAITPKG)
        return "waitpkg"sv;
    if (feature == CPUFeature::AVX512_VBMI2)
        return "avx512_vbmi2"sv;
    if (feature == CPUFeature::CET_SS)
        return "cet_ss"sv;
    if (feature == CPUFeature::GFNI)
        return "gfni"sv;
    if (feature == CPUFeature::VAES)
        return "vaes"sv;
    if (feature == CPUFeature::VPCLMULQDQ)
        return "vpclmulqdq"sv;
    if (feature == CPUFeature::AVX512_VNNI)
        return "avx512_vnni"sv;
    if (feature == CPUFeature::AVX512_BITALG)
        return "avx512_bitalg"sv;
    if (feature == CPUFeature::TME_EN)
        return "tme_en"sv;
    if (feature == CPUFeature::AVX512_VPOPCNTDQ)
        return "avx512_vpopcntdq"sv;
    if (feature == CPUFeature::INTEL_5_LEVEL_PAGING)
        return "intel_5_level_paging"sv;
    if (feature == CPUFeature::RDPID)
        return "rdpid"sv;
    if (feature == CPUFeature::KL)
        return "kl"sv;
    if (feature == CPUFeature::CLDEMOTE)
        return "cldemote"sv;
    if (feature == CPUFeature::MOVDIRI)
        return "movdiri"sv;
    if (feature == CPUFeature::MOVDIR64B)
        return "movdir64b"sv;
    if (feature == CPUFeature::ENQCMD)
        return "enqcmd"sv;
    if (feature == CPUFeature::SGX_LC)
        return "sgx_lc"sv;
    if (feature == CPUFeature::PKS)
        return "pks"sv;
    if (feature == CPUFeature::AVX512_4VNNIW)
        return "avx512_4vnniw"sv;
    if (feature == CPUFeature::AVX512_4FMAPS)
        return "avx512_4fmaps"sv;
    if (feature == CPUFeature::FSRM)
        return "fsrm"sv;
    if (feature == CPUFeature::AVX512_VP2INTERSECT)
        return "avx512_vp2intersect"sv;
    if (feature == CPUFeature::SRBDS_CTRL)
        return "srbds_ctrl"sv;
    if (feature == CPUFeature::MD_CLEAR)
        return "md_clear"sv;
    if (feature == CPUFeature::RTM_ALWAYS_ABORT)
        return "rtm_always_abort"sv;
    if (feature == CPUFeature::TSX_FORCE_ABORT)
        return "tsx_force_abort"sv;
    if (feature == CPUFeature::SERIALIZE)
        return "serialize"sv;
    if (feature == CPUFeature::HYBRID)
        return "hybrid"sv;
    if (feature == CPUFeature::TSXLDTRK)
        return "tsxldtrk"sv;
    if (feature == CPUFeature::PCONFIG)
        return "pconfig"sv;
    if (feature == CPUFeature::LBR)
        return "lbr"sv;
    if (feature == CPUFeature::CET_IBT)
        return "cet_ibt"sv;
    if (feature == CPUFeature::AMX_BF16)
        return "amx_bf16"sv;
    if (feature == CPUFeature::AVX512_FP16)
        return "avx512_fp16"sv;
    if (feature == CPUFeature::AMX_TILE)
        return "amx_tile"sv;
    if (feature == CPUFeature::AMX_INT8)
        return "amx_int8"sv;
    if (feature == CPUFeature::SPEC_CTRL)
        return "spec_ctrl"sv;
    if (feature == CPUFeature::STIBP)
        return "stibp"sv;
    // NOTE: This is called flush_l1d on Linux, but L1D_FLUSH in the Intel manual.
    if (feature == CPUFeature::L1D_FLUSH)
        return "l1d_flush"sv;
    if (feature == CPUFeature::IA32_ARCH_CAPABILITIES)
        return "ia32_arch_capabilities"sv;
    if (feature == CPUFeature::IA32_CORE_CAPABILITIES)
        return "ia32_code_capabilities"sv;
    if (feature == CPUFeature::SSBD)
        return "ssbd"sv;
    if (feature == CPUFeature::LAHF_LM)
        return "lahf_lm"sv;
    if (feature == CPUFeature::CMP_LEGACY)
        return "cmp_legacy"sv;
    if (feature == CPUFeature::SVM)
        return "svm"sv;
    if (feature == CPUFeature::EXTAPIC)
        return "extapic"sv;
    if (feature == CPUFeature::CR8_LEGACY)
        return "cr8_legacy"sv;
    if (feature == CPUFeature::ABM)
        return "abm"sv;
    if (feature == CPUFeature::SSE4A)
        return "sse4a"sv;
    if (feature == CPUFeature::MISALIGNSSE)
        return "misalignsse"sv;
    if (feature == CPUFeature::_3DNOWPREFETCH)
        return "3dnowprefetch"sv;
    if (feature == CPUFeature::OSVW)
        return "osvw"sv;
    if (feature == CPUFeature::IBS)
        return "ibs"sv;
    if (feature == CPUFeature::XOP)
        return "xop"sv;
    if (feature == CPUFeature::SKINIT)
        return "skinit"sv;
    if (feature == CPUFeature::WDT)
        return "wdt"sv;
    if (feature == CPUFeature::LWP)
        return "lwp"sv;
    if (feature == CPUFeature::FMA4)
        return "fma4"sv;
    if (feature == CPUFeature::TCE)
        return "tce"sv;
    if (feature == CPUFeature::NODEID_MSR)
        return "nodeid_msr"sv;
    if (feature == CPUFeature::TBM)
        return "tbm"sv;
    if (feature == CPUFeature::TOPOEXT)
        return "topoext"sv;
    if (feature == CPUFeature::PERFCTR_CORE)
        return "perfctr_core"sv;
    if (feature == CPUFeature::PERFCTR_NB)
        return "perfctr_nb"sv;
    if (feature == CPUFeature::DBX)
        return "dbx"sv;
    if (feature == CPUFeature::PERFTSC)
        return "perftsc"sv;
    // NOTE: This is called perfctr_l2 on Linux, but PCX_L2I in the AMD manual & other references.
    if (feature == CPUFeature::PCX_L2I)
        return "pcx_l2i"sv;
    if (feature == CPUFeature::SYSCALL)
        return "syscall"sv;
    if (feature == CPUFeature::MP)
        return "mp"sv;
    if (feature == CPUFeature::NX)
        return "nx"sv;
    if (feature == CPUFeature::MMXEXT)
        return "mmxext"sv;
    if (feature == CPUFeature::FXSR_OPT)
        return "fxsr_opt"sv;
    if (feature == CPUFeature::PDPE1GB)
        return "pdpe1gb"sv;
    if (feature == CPUFeature::RDTSCP)
        return "rdtscp"sv;
    if (feature == CPUFeature::LM)
        return "lm"sv;
    if (feature == CPUFeature::_3DNOWEXT)
        return "3dnowext"sv;
    if (feature == CPUFeature::_3DNOW)
        return "3dnow"sv;
    if (feature == CPUFeature::CONSTANT_TSC)
        return "constant_tsc"sv;
    if (feature == CPUFeature::NONSTOP_TSC)
        return "nonstop_tsc"sv;
    VERIFY_NOT_REACHED();
}

}
