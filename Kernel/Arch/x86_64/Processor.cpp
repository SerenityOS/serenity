/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BuiltinWrappers.h>
#include <AK/Format.h>
#include <AK/StackUnwinder.h>
#include <AK/StdLibExtras.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>

#include <Kernel/Arch/x86_64/Interrupts/APIC.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Library/StdLib.h>
#include <Kernel/Sections.h>
#include <Kernel/Security/Random.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/Thread.h>

#include <Kernel/Arch/Interrupts.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/SafeMem.h>
#include <Kernel/Arch/TrapFrame.h>
#include <Kernel/Arch/x86_64/CPUID.h>
#include <Kernel/Arch/x86_64/MSR.h>
#include <Kernel/Arch/x86_64/ProcessorInfo.h>
#include <Kernel/Library/ScopedCritical.h>

#include <Kernel/Arch/PageDirectory.h>
#include <Kernel/Memory/ScopedAddressSpaceSwitcher.h>

namespace Kernel {

READONLY_AFTER_INIT static ProcessorContainer s_processors {};
READONLY_AFTER_INIT static bool volatile s_smp_enabled;

static Atomic<ProcessorMessage*> s_message_pool;
Atomic<u32> Processor::s_idle_cpu_mask { 0 };

extern "C" void enter_thread_context(Thread* from_thread, Thread* to_thread) __attribute__((used));
extern "C" FlatPtr do_init_context(Thread* thread, u32 flags) __attribute__((used));
extern "C" void syscall_entry();

template<typename T>
bool ProcessorBase<T>::is_smp_enabled()
{
    return s_smp_enabled;
}

UNMAP_AFTER_INIT static void sse_init()
{
    write_cr0((read_cr0() & 0xfffffffbu) | 0x2);
    write_cr4(read_cr4() | 0x600);
}

UNMAP_AFTER_INIT void Processor::cpu_detect()
{
    // NOTE: This is called during Processor::early_initialize, we cannot
    //       safely log at this point because we don't have kmalloc
    //       initialized yet!
    m_features = CPUFeature::Type(0u);

    CPUID processor_info(0x1);

    auto handle_edx_bit_11_feature = [&] {
        u32 stepping = processor_info.eax() & 0xf;
        u32 model = (processor_info.eax() >> 4) & 0xf;
        u32 family = (processor_info.eax() >> 8) & 0xf;
        // FIXME: I have no clue what these mean or where it's from (the Intel manual I've seen just says EDX[11] is SEP).
        //        If you do, please convert them to constants or add comments!
        if (!(family == 6 && model < 3 && stepping < 3))
            m_features |= CPUFeature::SEP;
        if ((family == 6 && model >= 3) || (family == 0xf && model >= 0xe))
            m_features |= CPUFeature::CONSTANT_TSC;
    };

    if (processor_info.ecx() & (1 << 0))
        m_features |= CPUFeature::SSE3;
    if (processor_info.ecx() & (1 << 1))
        m_features |= CPUFeature::PCLMULQDQ;
    if (processor_info.ecx() & (1 << 2))
        m_features |= CPUFeature::DTES64;
    if (processor_info.ecx() & (1 << 3))
        m_features |= CPUFeature::MONITOR;
    if (processor_info.ecx() & (1 << 4))
        m_features |= CPUFeature::DS_CPL;
    if (processor_info.ecx() & (1 << 5))
        m_features |= CPUFeature::VMX;
    if (processor_info.ecx() & (1 << 6))
        m_features |= CPUFeature::SMX;
    if (processor_info.ecx() & (1 << 7))
        m_features |= CPUFeature::EST;
    if (processor_info.ecx() & (1 << 8))
        m_features |= CPUFeature::TM2;
    if (processor_info.ecx() & (1 << 9))
        m_features |= CPUFeature::SSSE3;
    if (processor_info.ecx() & (1 << 10))
        m_features |= CPUFeature::CNXT_ID;
    if (processor_info.ecx() & (1 << 11))
        m_features |= CPUFeature::SDBG;
    if (processor_info.ecx() & (1 << 12))
        m_features |= CPUFeature::FMA;
    if (processor_info.ecx() & (1 << 13))
        m_features |= CPUFeature::CX16;
    if (processor_info.ecx() & (1 << 14))
        m_features |= CPUFeature::XTPR;
    if (processor_info.ecx() & (1 << 15))
        m_features |= CPUFeature::PDCM;
    if (processor_info.ecx() & (1 << 17))
        m_features |= CPUFeature::PCID;
    if (processor_info.ecx() & (1 << 18))
        m_features |= CPUFeature::DCA;
    if (processor_info.ecx() & (1 << 19))
        m_features |= CPUFeature::SSE4_1;
    if (processor_info.ecx() & (1 << 20))
        m_features |= CPUFeature::SSE4_2;
    if (processor_info.ecx() & (1 << 21))
        m_features |= CPUFeature::X2APIC;
    if (processor_info.ecx() & (1 << 22))
        m_features |= CPUFeature::MOVBE;
    if (processor_info.ecx() & (1 << 23))
        m_features |= CPUFeature::POPCNT;
    if (processor_info.ecx() & (1 << 24))
        m_features |= CPUFeature::TSC_DEADLINE;
    if (processor_info.ecx() & (1 << 25))
        m_features |= CPUFeature::AES;
    if (processor_info.ecx() & (1 << 26))
        m_features |= CPUFeature::XSAVE;
    if (processor_info.ecx() & (1 << 27))
        m_features |= CPUFeature::OSXSAVE;
    if (processor_info.ecx() & (1 << 28))
        m_features |= CPUFeature::AVX;
    if (processor_info.ecx() & (1 << 29))
        m_features |= CPUFeature::F16C;
    if (processor_info.ecx() & (1 << 30))
        m_features |= CPUFeature::RDRAND;
    if (processor_info.ecx() & (1 << 31))
        m_features |= CPUFeature::HYPERVISOR;

    if (processor_info.edx() & (1 << 0))
        m_features |= CPUFeature::FPU;
    if (processor_info.edx() & (1 << 1))
        m_features |= CPUFeature::VME;
    if (processor_info.edx() & (1 << 2))
        m_features |= CPUFeature::DE;
    if (processor_info.edx() & (1 << 3))
        m_features |= CPUFeature::PSE;
    if (processor_info.edx() & (1 << 4))
        m_features |= CPUFeature::TSC;
    if (processor_info.edx() & (1 << 5))
        m_features |= CPUFeature::MSR;
    if (processor_info.edx() & (1 << 6))
        m_features |= CPUFeature::PAE;
    if (processor_info.edx() & (1 << 7))
        m_features |= CPUFeature::MCE;
    if (processor_info.edx() & (1 << 8))
        m_features |= CPUFeature::CX8;
    if (processor_info.edx() & (1 << 9))
        m_features |= CPUFeature::APIC;
    if (processor_info.edx() & (1 << 11))
        handle_edx_bit_11_feature();
    if (processor_info.edx() & (1 << 12))
        m_features |= CPUFeature::MTRR;
    if (processor_info.edx() & (1 << 13))
        m_features |= CPUFeature::PGE;
    if (processor_info.edx() & (1 << 14))
        m_features |= CPUFeature::MCA;
    if (processor_info.edx() & (1 << 15))
        m_features |= CPUFeature::CMOV;
    if (processor_info.edx() & (1 << 16))
        m_features |= CPUFeature::PAT;
    if (processor_info.edx() & (1 << 17))
        m_features |= CPUFeature::PSE36;
    if (processor_info.edx() & (1 << 18))
        m_features |= CPUFeature::PSN;
    if (processor_info.edx() & (1 << 19))
        m_features |= CPUFeature::CLFLUSH;
    if (processor_info.edx() & (1 << 21))
        m_features |= CPUFeature::DS;
    if (processor_info.edx() & (1 << 22))
        m_features |= CPUFeature::ACPI;
    if (processor_info.edx() & (1 << 23))
        m_features |= CPUFeature::MMX;
    if (processor_info.edx() & (1 << 24))
        m_features |= CPUFeature::FXSR;
    if (processor_info.edx() & (1 << 25))
        m_features |= CPUFeature::SSE;
    if (processor_info.edx() & (1 << 26))
        m_features |= CPUFeature::SSE2;
    if (processor_info.edx() & (1 << 27))
        m_features |= CPUFeature::SS;
    if (processor_info.edx() & (1 << 28))
        m_features |= CPUFeature::HTT;
    if (processor_info.edx() & (1 << 29))
        m_features |= CPUFeature::TM;
    if (processor_info.edx() & (1 << 30))
        m_features |= CPUFeature::IA64;
    if (processor_info.edx() & (1 << 31))
        m_features |= CPUFeature::PBE;

    CPUID extended_features(0x7);

    if (extended_features.ebx() & (1 << 0))
        m_features |= CPUFeature::FSGSBASE;
    if (extended_features.ebx() & (1 << 1))
        m_features |= CPUFeature::TSC_ADJUST;
    if (extended_features.ebx() & (1 << 2))
        m_features |= CPUFeature::SGX;
    if (extended_features.ebx() & (1 << 3))
        m_features |= CPUFeature::BMI1;
    if (extended_features.ebx() & (1 << 4))
        m_features |= CPUFeature::HLE;
    if (extended_features.ebx() & (1 << 5))
        m_features |= CPUFeature::AVX2;
    if (extended_features.ebx() & (1 << 6))
        m_features |= CPUFeature::FDP_EXCPTN_ONLY;
    if (extended_features.ebx() & (1 << 7))
        m_features |= CPUFeature::SMEP;
    if (extended_features.ebx() & (1 << 8))
        m_features |= CPUFeature::BMI2;
    if (extended_features.ebx() & (1 << 9))
        m_features |= CPUFeature::ERMS;
    if (extended_features.ebx() & (1 << 10))
        m_features |= CPUFeature::INVPCID;
    if (extended_features.ebx() & (1 << 11))
        m_features |= CPUFeature::RTM;
    if (extended_features.ebx() & (1 << 12))
        m_features |= CPUFeature::PQM;
    if (extended_features.ebx() & (1 << 13))
        m_features |= CPUFeature::ZERO_FCS_FDS;
    if (extended_features.ebx() & (1 << 14))
        m_features |= CPUFeature::MPX;
    if (extended_features.ebx() & (1 << 15))
        m_features |= CPUFeature::PQE;
    if (extended_features.ebx() & (1 << 16))
        m_features |= CPUFeature::AVX512_F;
    if (extended_features.ebx() & (1 << 17))
        m_features |= CPUFeature::AVX512_DQ;
    if (extended_features.ebx() & (1 << 18))
        m_features |= CPUFeature::RDSEED;
    if (extended_features.ebx() & (1 << 19))
        m_features |= CPUFeature::ADX;
    if (extended_features.ebx() & (1 << 20))
        m_features |= CPUFeature::SMAP;
    if (extended_features.ebx() & (1 << 21))
        m_features |= CPUFeature::AVX512_IFMA;
    if (extended_features.ebx() & (1 << 22))
        m_features |= CPUFeature::PCOMMIT;
    if (extended_features.ebx() & (1 << 23))
        m_features |= CPUFeature::CLFLUSHOPT;
    if (extended_features.ebx() & (1 << 24))
        m_features |= CPUFeature::CLWB;
    if (extended_features.ebx() & (1 << 25))
        m_features |= CPUFeature::INTEL_PT;
    if (extended_features.ebx() & (1 << 26))
        m_features |= CPUFeature::AVX512_PF;
    if (extended_features.ebx() & (1 << 27))
        m_features |= CPUFeature::AVX512_ER;
    if (extended_features.ebx() & (1 << 28))
        m_features |= CPUFeature::AVX512_CD;
    if (extended_features.ebx() & (1 << 29))
        m_features |= CPUFeature::SHA;
    if (extended_features.ebx() & (1 << 30))
        m_features |= CPUFeature::AVX512_BW;
    if (extended_features.ebx() & (1 << 31))
        m_features |= CPUFeature::AVX512_VL;

    if (extended_features.ecx() & (1 << 0))
        m_features |= CPUFeature::PREFETCHWT1;
    if (extended_features.ecx() & (1 << 1))
        m_features |= CPUFeature::AVX512_VBMI;
    if (extended_features.ecx() & (1 << 2))
        m_features |= CPUFeature::UMIP;
    if (extended_features.ecx() & (1 << 3))
        m_features |= CPUFeature::PKU;
    if (extended_features.ecx() & (1 << 4))
        m_features |= CPUFeature::OSPKE;
    if (extended_features.ecx() & (1 << 5))
        m_features |= CPUFeature::WAITPKG;
    if (extended_features.ecx() & (1 << 6))
        m_features |= CPUFeature::AVX512_VBMI2;
    if (extended_features.ecx() & (1 << 7))
        m_features |= CPUFeature::CET_SS;
    if (extended_features.ecx() & (1 << 8))
        m_features |= CPUFeature::GFNI;
    if (extended_features.ecx() & (1 << 9))
        m_features |= CPUFeature::VAES;
    if (extended_features.ecx() & (1 << 10))
        m_features |= CPUFeature::VPCLMULQDQ;
    if (extended_features.ecx() & (1 << 11))
        m_features |= CPUFeature::AVX512_VNNI;
    if (extended_features.ecx() & (1 << 12))
        m_features |= CPUFeature::AVX512_BITALG;
    if (extended_features.ecx() & (1 << 13))
        m_features |= CPUFeature::TME_EN;
    if (extended_features.ecx() & (1 << 14))
        m_features |= CPUFeature::AVX512_VPOPCNTDQ;
    if (extended_features.ecx() & (1 << 16))
        m_features |= CPUFeature::INTEL_5_LEVEL_PAGING;
    if (extended_features.ecx() & (1 << 22))
        m_features |= CPUFeature::RDPID;
    if (extended_features.ecx() & (1 << 23))
        m_features |= CPUFeature::KL;
    if (extended_features.ecx() & (1 << 25))
        m_features |= CPUFeature::CLDEMOTE;
    if (extended_features.ecx() & (1 << 27))
        m_features |= CPUFeature::MOVDIRI;
    if (extended_features.ecx() & (1 << 28))
        m_features |= CPUFeature::MOVDIR64B;
    if (extended_features.ecx() & (1 << 29))
        m_features |= CPUFeature::ENQCMD;
    if (extended_features.ecx() & (1 << 30))
        m_features |= CPUFeature::SGX_LC;
    if (extended_features.ecx() & (1 << 31))
        m_features |= CPUFeature::PKS;

    if (extended_features.edx() & (1 << 2))
        m_features |= CPUFeature::AVX512_4VNNIW;
    if (extended_features.edx() & (1 << 3))
        m_features |= CPUFeature::AVX512_4FMAPS;
    if (extended_features.edx() & (1 << 4))
        m_features |= CPUFeature::FSRM;
    if (extended_features.edx() & (1 << 8))
        m_features |= CPUFeature::AVX512_VP2INTERSECT;
    if (extended_features.edx() & (1 << 9))
        m_features |= CPUFeature::SRBDS_CTRL;
    if (extended_features.edx() & (1 << 10))
        m_features |= CPUFeature::MD_CLEAR;
    if (extended_features.edx() & (1 << 11))
        m_features |= CPUFeature::RTM_ALWAYS_ABORT;
    if (extended_features.edx() & (1 << 13))
        m_features |= CPUFeature::TSX_FORCE_ABORT;
    if (extended_features.edx() & (1 << 14))
        m_features |= CPUFeature::SERIALIZE;
    if (extended_features.edx() & (1 << 15))
        m_features |= CPUFeature::HYBRID;
    if (extended_features.edx() & (1 << 16))
        m_features |= CPUFeature::TSXLDTRK;
    if (extended_features.edx() & (1 << 18))
        m_features |= CPUFeature::PCONFIG;
    if (extended_features.edx() & (1 << 19))
        m_features |= CPUFeature::LBR;
    if (extended_features.edx() & (1 << 20))
        m_features |= CPUFeature::CET_IBT;
    if (extended_features.edx() & (1 << 22))
        m_features |= CPUFeature::AMX_BF16;
    if (extended_features.edx() & (1 << 23))
        m_features |= CPUFeature::AVX512_FP16;
    if (extended_features.edx() & (1 << 24))
        m_features |= CPUFeature::AMX_TILE;
    if (extended_features.edx() & (1 << 25))
        m_features |= CPUFeature::AMX_INT8;
    if (extended_features.edx() & (1 << 26))
        m_features |= CPUFeature::SPEC_CTRL;
    if (extended_features.edx() & (1 << 27))
        m_features |= CPUFeature::STIBP;
    if (extended_features.edx() & (1 << 28))
        m_features |= CPUFeature::L1D_FLUSH;
    if (extended_features.edx() & (1 << 29))
        m_features |= CPUFeature::IA32_ARCH_CAPABILITIES;
    if (extended_features.edx() & (1 << 30))
        m_features |= CPUFeature::IA32_CORE_CAPABILITIES;
    if (extended_features.edx() & (1 << 31))
        m_features |= CPUFeature::SSBD;

    u32 max_extended_leaf = CPUID(0x80000000).eax();

    if (max_extended_leaf >= 0x80000001) {
        CPUID extended_processor_info(0x80000001);

        if (extended_processor_info.ecx() & (1 << 0))
            m_features |= CPUFeature::LAHF_LM;
        if (extended_processor_info.ecx() & (1 << 1))
            m_features |= CPUFeature::CMP_LEGACY;
        if (extended_processor_info.ecx() & (1 << 2))
            m_features |= CPUFeature::SVM;
        if (extended_processor_info.ecx() & (1 << 3))
            m_features |= CPUFeature::EXTAPIC;
        if (extended_processor_info.ecx() & (1 << 4))
            m_features |= CPUFeature::CR8_LEGACY;
        if (extended_processor_info.ecx() & (1 << 5))
            m_features |= CPUFeature::ABM;
        if (extended_processor_info.ecx() & (1 << 6))
            m_features |= CPUFeature::SSE4A;
        if (extended_processor_info.ecx() & (1 << 7))
            m_features |= CPUFeature::MISALIGNSSE;
        if (extended_processor_info.ecx() & (1 << 8))
            m_features |= CPUFeature::_3DNOWPREFETCH;
        if (extended_processor_info.ecx() & (1 << 9))
            m_features |= CPUFeature::OSVW;
        if (extended_processor_info.ecx() & (1 << 10))
            m_features |= CPUFeature::IBS;
        if (extended_processor_info.ecx() & (1 << 11))
            m_features |= CPUFeature::XOP;
        if (extended_processor_info.ecx() & (1 << 12))
            m_features |= CPUFeature::SKINIT;
        if (extended_processor_info.ecx() & (1 << 13))
            m_features |= CPUFeature::WDT;
        if (extended_processor_info.ecx() & (1 << 15))
            m_features |= CPUFeature::LWP;
        if (extended_processor_info.ecx() & (1 << 16))
            m_features |= CPUFeature::FMA4;
        if (extended_processor_info.ecx() & (1 << 17))
            m_features |= CPUFeature::TCE;
        if (extended_processor_info.ecx() & (1 << 19))
            m_features |= CPUFeature::NODEID_MSR;
        if (extended_processor_info.ecx() & (1 << 21))
            m_features |= CPUFeature::TBM;
        if (extended_processor_info.ecx() & (1 << 22))
            m_features |= CPUFeature::TOPOEXT;
        if (extended_processor_info.ecx() & (1 << 23))
            m_features |= CPUFeature::PERFCTR_CORE;
        if (extended_processor_info.ecx() & (1 << 24))
            m_features |= CPUFeature::PERFCTR_NB;
        if (extended_processor_info.ecx() & (1 << 26))
            m_features |= CPUFeature::DBX;
        if (extended_processor_info.ecx() & (1 << 27))
            m_features |= CPUFeature::PERFTSC;
        if (extended_processor_info.ecx() & (1 << 28))
            m_features |= CPUFeature::PCX_L2I;

        if (extended_processor_info.edx() & (1 << 11))
            m_features |= CPUFeature::SYSCALL; // Only available in 64 bit mode
        if (extended_processor_info.edx() & (1 << 19))
            m_features |= CPUFeature::MP;
        if (extended_processor_info.edx() & (1 << 20))
            m_features |= CPUFeature::NX;
        if (extended_processor_info.edx() & (1 << 22))
            m_features |= CPUFeature::MMXEXT;
        if (extended_processor_info.edx() & (1 << 23))
            m_features |= CPUFeature::RDTSCP;
        if (extended_processor_info.edx() & (1 << 25))
            m_features |= CPUFeature::FXSR_OPT;
        if (extended_processor_info.edx() & (1 << 26))
            m_features |= CPUFeature::PDPE1GB;
        if (extended_processor_info.edx() & (1 << 27))
            m_features |= CPUFeature::RDTSCP;
        if (extended_processor_info.edx() & (1 << 29))
            m_features |= CPUFeature::LM;
        if (extended_processor_info.edx() & (1 << 30))
            m_features |= CPUFeature::_3DNOWEXT;
        if (extended_processor_info.edx() & (1 << 31))
            m_features |= CPUFeature::_3DNOW;
    }

    if (max_extended_leaf >= 0x80000007) {
        CPUID cpuid(0x80000007);
        if (cpuid.edx() & (1 << 8)) {
            m_features |= CPUFeature::CONSTANT_TSC;
            m_features |= CPUFeature::NONSTOP_TSC;
        }
    }

    if (max_extended_leaf >= 0x80000008) {
        // CPUID.80000008H:EAX[7:0] reports the physical-address width supported by the processor.
        CPUID cpuid(0x80000008);
        m_physical_address_bit_width = cpuid.eax() & 0xff;
        // CPUID.80000008H:EAX[15:8] reports the linear-address width supported by the processor.
        m_virtual_address_bit_width = (cpuid.eax() >> 8) & 0xff;
    } else {
        // For processors that do not support CPUID function 80000008H, the width is generally 36 if CPUID.01H:EDX.PAE [bit 6] = 1 and 32 otherwise.
        m_physical_address_bit_width = has_feature(CPUFeature::PAE) ? 36 : 32;
        // Processors that do not support CPUID function 80000008H, support a linear-address width of 32.
        m_virtual_address_bit_width = 32;
        // Workaround QEMU hypervisor.framework bug
        // https://gitlab.com/qemu-project/qemu/-/issues/664
        //
        // We detect this as follows:
        //    * We're in a hypervisor
        //    * hypervisor_leaf_range is null under Hypervisor.framework
        //    * m_physical_address_bit_width is 36 bits
        if (has_feature(CPUFeature::HYPERVISOR)) {
            CPUID hypervisor_leaf_range(0x40000000);
            if (!hypervisor_leaf_range.ebx() && m_physical_address_bit_width == 36) {
                m_has_qemu_hvf_quirk.set();
                m_virtual_address_bit_width = 48;
            }
        }
    }
}

UNMAP_AFTER_INIT void Processor::cpu_setup()
{
    // NOTE: This is called during Processor::early_initialize, we cannot
    //       safely log at this point because we don't have kmalloc
    //       initialized yet!
    cpu_detect();

    if (has_feature(CPUFeature::SSE)) {
        // enter_thread_context() assumes that if a x86 CPU supports SSE then it also supports FXSR.
        // SSE support without FXSR is an extremely unlikely scenario, so let's be pragmatic about it.
        VERIFY(has_feature(CPUFeature::FXSR));
        sse_init();
    }

    write_cr0(read_cr0() | 0x00010000);

    if (has_feature(CPUFeature::PGE)) {
        // Turn on CR4.PGE so the CPU will respect the G bit in page tables.
        write_cr4(read_cr4() | 0x80);
    }

    if (has_feature(CPUFeature::NX)) {
        // Turn on IA32_EFER.NXE
        MSR ia32_efer(MSR_IA32_EFER);
        ia32_efer.set(ia32_efer.get() | 0x800);
    }

    if (has_feature(CPUFeature::PAT)) {
        MSR ia32_pat(MSR_IA32_PAT);
        // Set PA4 to Write Comine. This allows us to
        // use this mode by only setting the bit in the PTE
        // and leaving all other bits in the upper levels unset,
        // which maps to setting bit 3 of the index, resulting
        // in the index value 0 or 4.
        u64 pat = ia32_pat.get() & ~(0x7ull << 32);
        pat |= 0x1ull << 32; // set WC mode for PA4
        ia32_pat.set(pat);
    }

    if (has_feature(CPUFeature::SMEP)) {
        // Turn on CR4.SMEP
        write_cr4(read_cr4() | 0x100000);
    }

    if (has_feature(CPUFeature::SMAP)) {
        // Turn on CR4.SMAP
        write_cr4(read_cr4() | 0x200000);
    }

    if (has_feature(CPUFeature::UMIP)) {
        write_cr4(read_cr4() | 0x800);
    }

    if (has_feature(CPUFeature::XSAVE)) {
        // Turn on CR4.OSXSAVE
        write_cr4(read_cr4() | 0x40000);

        // According to the Intel manual: "After reset, all bits (except bit 0) in XCR0 are cleared to zero; XCR0[0] is set to 1."
        // Sadly we can't trust this, for example VirtualBox starts with bits 0-4 set, so let's do it ourselves.
        write_xcr0(0x1);

        if (has_feature(CPUFeature::AVX)) {
            // Turn on SSE, AVX and x87 flags
            write_xcr0(read_xcr0() | SIMD::StateComponent::AVX | SIMD::StateComponent::SSE | SIMD::StateComponent::X87);
        }
    }

    // x86_64 processors must support the syscall feature.
    VERIFY(has_feature(CPUFeature::SYSCALL));
    MSR efer_msr(MSR_EFER);
    efer_msr.set(efer_msr.get() | 1u);

    // Write code and stack selectors to the STAR MSR. The first value stored in bits 63:48 controls the sysret CS (value + 0x10) and SS (value + 0x8),
    // and the value stored in bits 47:32 controls the syscall CS (value) and SS (value + 0x8).
    u64 star = 0;
    star |= 0x13ul << 48u;
    star |= 0x08ul << 32u;
    MSR star_msr(MSR_STAR);
    star_msr.set(star);

    // Write the syscall entry point to the LSTAR MSR.
    MSR lstar_msr(MSR_LSTAR);
    lstar_msr.set(reinterpret_cast<u64>(&syscall_entry));

    // Write the SFMASK MSR. This MSR controls which bits of rflags are masked when a syscall instruction is executed -
    // if a bit is set in sfmask, the corresponding bit in rflags is cleared. The value set here clears most of rflags,
    // but keeps the reserved and virtualization bits intact. The userspace rflags value is saved in r11 by syscall.
    constexpr u64 rflags_mask = 0x257fd5u;
    MSR sfmask_msr(MSR_SFMASK);
    sfmask_msr.set(rflags_mask);

    if (has_feature(CPUFeature::FSGSBASE)) {
        // Turn off CR4.FSGSBASE to ensure the current Processor base kernel address is not leaked via
        // the RDGSBASE instruction until we implement proper GS swapping at the userspace/kernel boundaries
        write_cr4(read_cr4() & ~0x10000);
    }

    // Query OS-enabled CPUID features again, and set the flags if needed.
    CPUID processor_info(0x1);
    if (processor_info.ecx() & (1 << 27))
        m_features |= CPUFeature::OSXSAVE;
    CPUID extended_features(0x7);
    if (extended_features.ecx() & (1 << 4))
        m_features |= CPUFeature::OSPKE;
}

template<typename T>
UNMAP_AFTER_INIT void ProcessorBase<T>::early_initialize(u32 cpu)
{
    m_self = static_cast<Processor*>(this);
    auto self = static_cast<Processor*>(this);

    m_cpu = cpu;
    m_in_irq = 0;
    m_in_critical = 0;

    m_invoke_scheduler_async = false;
    m_in_scheduler = true;

    self->m_message_queue = nullptr;
    m_idle_thread = nullptr;
    m_current_thread = nullptr;
    self->m_info = nullptr;

    m_halt_requested = false;
    if (cpu == 0) {
        s_smp_enabled = false;
        g_total_processors.store(1u, AK::MemoryOrder::memory_order_release);
    } else {
        g_total_processors.fetch_add(1u, AK::MemoryOrder::memory_order_acq_rel);
    }

    m_deferred_call_pool.init();

    self->cpu_setup();
    self->gdt_init();

    VERIFY(is_initialized());
    VERIFY(&current() == this); // sanity check
}

template<typename T>
UNMAP_AFTER_INIT void ProcessorBase<T>::initialize(u32 cpu)
{
    VERIFY(m_self == this);
    VERIFY(&current() == this); // sanity check

    auto self = static_cast<Processor*>(this);

    self->m_info = new ProcessorInfo(*self);

    dmesgln("CPU[{}]: Supported features: {}", current_id(), self->m_info->features_string());
    if (!has_feature(CPUFeature::RDRAND))
        dmesgln("CPU[{}]: No RDRAND support detected, randomness will be poor", current_id());
    dmesgln("CPU[{}]: Physical address bit width: {}", current_id(), m_physical_address_bit_width);
    dmesgln("CPU[{}]: Virtual address bit width: {}", current_id(), m_virtual_address_bit_width);
    if (self->m_has_qemu_hvf_quirk.was_set())
        dmesgln("CPU[{}]: Applied correction for QEMU Hypervisor.framework quirk", current_id());

    if (cpu == 0)
        initialize_interrupts();
    else
        flush_idt();

    if (cpu == 0) {
        VERIFY((FlatPtr(&s_clean_fpu_state) & 0xF) == 0);
        asm volatile("fninit");
        // Initialize AVX state
        if (has_feature(CPUFeature::XSAVE | CPUFeature::AVX)) {
            asm volatile("xsave %0\n"
                         : "=m"(s_clean_fpu_state)
                         : "a"(static_cast<u32>(SIMD::StateComponent::AVX | SIMD::StateComponent::SSE | SIMD::StateComponent::X87)), "d"(0u));
        } else if (has_feature(CPUFeature::FXSR)) {
            asm volatile("fxsave %0"
                         : "=m"(s_clean_fpu_state));
        } else {
            asm volatile("fnsave %0"
                         : "=m"(s_clean_fpu_state));
        }

        if (has_feature(CPUFeature::HYPERVISOR))
            self->detect_hypervisor();
    }

    {
        // We need to prevent races between APs starting up at the same time
        VERIFY(cpu < s_processors.size());
        s_processors[cpu] = static_cast<Processor*>(this);
    }
}

UNMAP_AFTER_INIT void Processor::detect_hypervisor()
{
    CPUID hypervisor_leaf_range(0x40000000);
    auto hypervisor_vendor_id_string = m_info->hypervisor_vendor_id_string();
    dmesgln("CPU[{}]: CPUID hypervisor signature '{}', max leaf {:#x}", current_id(), hypervisor_vendor_id_string, hypervisor_leaf_range.eax());

    if (hypervisor_vendor_id_string == "Microsoft Hv"sv)
        detect_hypervisor_hyperv(hypervisor_leaf_range);
}

UNMAP_AFTER_INIT void Processor::detect_hypervisor_hyperv(CPUID const& hypervisor_leaf_range)
{
    if (hypervisor_leaf_range.eax() < 0x40000001)
        return;

    CPUID hypervisor_interface(0x40000001);

    // Get signature of hypervisor interface.
    alignas(sizeof(u32)) char interface_signature_buffer[5];
    *reinterpret_cast<u32*>(interface_signature_buffer) = hypervisor_interface.eax();
    interface_signature_buffer[4] = '\0';
    StringView hyperv_interface_signature { interface_signature_buffer, strlen(interface_signature_buffer) };

    dmesgln("CPU[{}]: Hyper-V interface signature '{}' ({:#x})", current_id(), hyperv_interface_signature, hypervisor_interface.eax());

    if (hypervisor_leaf_range.eax() < 0x40000001)
        return;

    CPUID hypervisor_sysid(0x40000002);
    dmesgln("CPU[{}]: Hyper-V system identity {}.{}, build number {}", current_id(), hypervisor_sysid.ebx() >> 16, hypervisor_sysid.ebx() & 0xFFFF, hypervisor_sysid.eax());

    if (hypervisor_leaf_range.eax() < 0x40000005 || hyperv_interface_signature != "Hv#1"sv)
        return;

    dmesgln("CPU[{}]: Hyper-V hypervisor detected", current_id());

    // TODO: Actually do something with Hyper-V.
}

void Processor::write_raw_gdt_entry(u16 selector, u32 low, u32 high)
{
    u16 i = (selector & 0xfffc) >> 3;
    u32 prev_gdt_length = m_gdt_length;

    if (i >= m_gdt_length) {
        m_gdt_length = i + 1;
        VERIFY(m_gdt_length <= sizeof(m_gdt) / sizeof(m_gdt[0]));
        m_gdtr.limit = (m_gdt_length + 1) * 8 - 1;
    }
    m_gdt[i].low = low;
    m_gdt[i].high = high;

    // clear selectors we may have skipped
    for (auto j = prev_gdt_length; j < i; ++j) {
        m_gdt[j].low = 0;
        m_gdt[j].high = 0;
    }
}

void Processor::write_gdt_entry(u16 selector, Descriptor& descriptor)
{
    write_raw_gdt_entry(selector, descriptor.low, descriptor.high);
}

Descriptor& Processor::get_gdt_entry(u16 selector)
{
    u16 i = (selector & 0xfffc) >> 3;
    return *(Descriptor*)(&m_gdt[i]);
}

void Processor::flush_gdt()
{
    m_gdtr.address = m_gdt;
    m_gdtr.limit = (m_gdt_length * 8) - 1;
    asm volatile("lgdt %0" ::"m"(m_gdtr)
                 : "memory");
}

DescriptorTablePointer const& Processor::get_gdtr()
{
    return m_gdtr;
}

ProcessorContainer& Processor::processors()
{
    return s_processors;
}

template<typename T>
Processor& ProcessorBase<T>::by_id(u32 id)
{
    return *s_processors[id];
}

template<typename T>
void ProcessorBase<T>::exit_trap(TrapFrame& trap)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(&Processor::current() == this);

    auto* self = static_cast<Processor*>(this);

    // Temporarily enter a critical section. This is to prevent critical
    // sections entered and left within e.g. smp_process_pending_messages
    // to trigger a context switch while we're executing this function
    // See the comment at the end of the function why we don't use
    // ScopedCritical here.
    m_in_critical = m_in_critical + 1;

    VERIFY(m_in_irq >= trap.prev_irq_level);
    m_in_irq = trap.prev_irq_level;

    if (s_smp_enabled)
        self->smp_process_pending_messages();

    // Process the deferred call queue. Among other things, this ensures
    // that any pending thread unblocks happen before we enter the scheduler.
    m_deferred_call_pool.execute_pending();

    auto* current_thread = Processor::current_thread();
    if (current_thread) {
        auto& current_trap = current_thread->current_trap();
        current_trap = trap.next_trap;
        ExecutionMode new_previous_mode;
        if (current_trap) {
            VERIFY(current_trap->regs);
            // If we have another higher level trap then we probably returned
            // from an interrupt or irq handler.
            new_previous_mode = current_trap->regs->previous_mode();
        } else {
            // If we don't have a higher level trap then we're back in user mode.
            // Which means that the previous mode prior to being back in user mode was kernel mode
            new_previous_mode = ExecutionMode::Kernel;
        }

        if (current_thread->set_previous_mode(new_previous_mode))
            current_thread->update_time_scheduled(TimeManagement::scheduler_current_time(), true, false);
    }

    VERIFY_INTERRUPTS_DISABLED();

    // Leave the critical section without actually enabling interrupts.
    // We don't want context switches to happen until we're explicitly
    // triggering a switch in check_invoke_scheduler.
    m_in_critical = m_in_critical - 1;
    if (!m_in_irq && !m_in_critical)
        check_invoke_scheduler();
}

template<typename T>
void ProcessorBase<T>::flush_tlb_local(VirtualAddress vaddr, size_t page_count)
{
    auto ptr = vaddr.as_ptr();
    while (page_count > 0) {
        asm volatile("invlpg %0"
                     :
                     : "m"(*ptr)
                     : "memory");
        ptr += PAGE_SIZE;
        page_count--;
    }
}

template<typename T>
void ProcessorBase<T>::flush_entire_tlb_local()
{
    write_cr3(read_cr3());
}

template<typename T>
void ProcessorBase<T>::flush_tlb(Memory::PageDirectory const* page_directory, VirtualAddress vaddr, size_t page_count)
{
    if (s_smp_enabled && (!Memory::is_user_address(vaddr) || Process::current().thread_count() > 1))
        Processor::smp_broadcast_flush_tlb(page_directory, vaddr, page_count);
    else
        flush_tlb_local(vaddr, page_count);
}

template<typename T>
void ProcessorBase<T>::flush_instruction_cache(VirtualAddress, size_t)
{
    // The instruction and data cache are coherent on x86, so we don't need to do anything here.
}

void Processor::smp_return_to_pool(ProcessorMessage& msg)
{
    ProcessorMessage* next = nullptr;
    for (;;) {
        msg.next = next;
        if (s_message_pool.compare_exchange_strong(next, &msg, AK::MemoryOrder::memory_order_acq_rel))
            break;
        Processor::pause();
    }
}

ProcessorMessage& Processor::smp_get_from_pool()
{
    ProcessorMessage* msg;

    // The assumption is that messages are never removed from the pool!
    for (;;) {
        msg = s_message_pool.load(AK::MemoryOrder::memory_order_consume);
        if (!msg) {
            if (!Processor::current().smp_process_pending_messages()) {
                Processor::pause();
            }
            continue;
        }
        // If another processor were to use this message in the meanwhile,
        // "msg" is still valid (because it never gets freed). We'd detect
        // this because the expected value "msg" and pool would
        // no longer match, and the compare_exchange will fail. But accessing
        // "msg->next" is always safe here.
        if (s_message_pool.compare_exchange_strong(msg, msg->next, AK::MemoryOrder::memory_order_acq_rel)) {
            // We successfully "popped" this available message
            break;
        }
    }

    VERIFY(msg != nullptr);
    return *msg;
}

template<typename T>
u32 ProcessorBase<T>::smp_wake_n_idle_processors(u32 wake_count)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(wake_count > 0);
    if (!s_smp_enabled)
        return 0;

    // Wake at most N - 1 processors
    if (wake_count >= Processor::count()) {
        wake_count = Processor::count() - 1;
        VERIFY(wake_count > 0);
    }

    u32 current_id = Processor::current_id();

    u32 did_wake_count = 0;
    auto& apic = APIC::the();
    while (did_wake_count < wake_count) {
        // Try to get a set of idle CPUs and flip them to busy
        u32 idle_mask = Processor::s_idle_cpu_mask.load(AK::MemoryOrder::memory_order_relaxed) & ~(1u << current_id);
        u32 idle_count = popcount(idle_mask);
        if (idle_count == 0)
            break; // No (more) idle processor available

        u32 found_mask = 0;
        for (u32 i = 0; i < idle_count; i++) {
            u32 cpu = bit_scan_forward(idle_mask) - 1;
            idle_mask &= ~(1u << cpu);
            found_mask |= 1u << cpu;
        }

        idle_mask = Processor::s_idle_cpu_mask.fetch_and(~found_mask, AK::MemoryOrder::memory_order_acq_rel) & found_mask;
        if (idle_mask == 0)
            continue; // All of them were flipped to busy, try again
        idle_count = popcount(idle_mask);
        for (u32 i = 0; i < idle_count; i++) {
            u32 cpu = bit_scan_forward(idle_mask) - 1;
            idle_mask &= ~(1u << cpu);

            // Send an IPI to that CPU to wake it up. There is a possibility
            // someone else woke it up as well, or that it woke up due to
            // a timer interrupt. But we tried hard to avoid this...
            apic.send_ipi(cpu);
            did_wake_count++;
        }
    }
    return did_wake_count;
}

template<typename T>
UNMAP_AFTER_INIT void ProcessorBase<T>::smp_enable()
{
    size_t msg_pool_size = Processor::count() * 100u;
    size_t msg_entries_cnt = Processor::count();

    auto msgs = new ProcessorMessage[msg_pool_size];
    auto msg_entries = new ProcessorMessageEntry[msg_pool_size * msg_entries_cnt];
    size_t msg_entry_i = 0;
    for (size_t i = 0; i < msg_pool_size; i++, msg_entry_i += msg_entries_cnt) {
        auto& msg = msgs[i];
        msg.next = i < msg_pool_size - 1 ? &msgs[i + 1] : nullptr;
        msg.per_proc_entries = &msg_entries[msg_entry_i];
        for (size_t k = 0; k < msg_entries_cnt; k++)
            msg_entries[msg_entry_i + k].msg = &msg;
    }

    s_message_pool.store(&msgs[0], AK::MemoryOrder::memory_order_release);

    // Start sending IPI messages
    s_smp_enabled = true;
}

void Processor::smp_cleanup_message(ProcessorMessage& msg)
{
    switch (msg.type) {
    case ProcessorMessage::Callback:
        msg.callback_value().~Function();
        break;
    default:
        break;
    }
}

bool Processor::smp_process_pending_messages()
{
    VERIFY(s_smp_enabled);

    bool did_process = false;
    enter_critical();

    if (auto pending_msgs = m_message_queue.exchange(nullptr, AK::MemoryOrder::memory_order_acq_rel)) {
        // We pulled the stack of pending messages in LIFO order, so we need to reverse the list first
        auto reverse_list =
            [](ProcessorMessageEntry* list) -> ProcessorMessageEntry* {
            ProcessorMessageEntry* rev_list = nullptr;
            while (list) {
                auto next = list->next;
                list->next = rev_list;
                rev_list = list;
                list = next;
            }
            return rev_list;
        };

        pending_msgs = reverse_list(pending_msgs);

        // now process in the right order
        ProcessorMessageEntry* next_msg;
        for (auto cur_msg = pending_msgs; cur_msg; cur_msg = next_msg) {
            next_msg = cur_msg->next;
            auto msg = cur_msg->msg;

            dbgln_if(SMP_DEBUG, "SMP[{}]: Processing message {}", current_id(), VirtualAddress(msg));

            switch (msg->type) {
            case ProcessorMessage::Callback:
                msg->invoke_callback();
                break;
            case ProcessorMessage::FlushTlb:
                if (Memory::is_user_address(VirtualAddress(msg->flush_tlb.ptr))) {
                    // We assume that we don't cross into kernel land!
                    VERIFY(Memory::is_user_range(VirtualAddress(msg->flush_tlb.ptr), msg->flush_tlb.page_count * PAGE_SIZE));
                    if (read_cr3() != msg->flush_tlb.page_directory->cr3()) {
                        // This processor isn't using this page directory right now, we can ignore this request
                        dbgln_if(SMP_DEBUG, "SMP[{}]: No need to flush {} pages at {}", current_id(), msg->flush_tlb.page_count, VirtualAddress(msg->flush_tlb.ptr));
                        break;
                    }
                }
                flush_tlb_local(VirtualAddress(msg->flush_tlb.ptr), msg->flush_tlb.page_count);
                break;
            }

            bool is_async = msg->async; // Need to cache this value *before* dropping the ref count!
            auto prev_refs = msg->refs.fetch_sub(1u, AK::MemoryOrder::memory_order_acq_rel);
            VERIFY(prev_refs != 0);
            if (prev_refs == 1) {
                // All processors handled this. If this is an async message,
                // we need to clean it up and return it to the pool
                if (is_async) {
                    smp_cleanup_message(*msg);
                    smp_return_to_pool(*msg);
                }
            }

            if (m_halt_requested.load(AK::MemoryOrder::memory_order_relaxed))
                halt_this();
        }
        did_process = true;
    } else if (m_halt_requested.load(AK::MemoryOrder::memory_order_relaxed)) {
        halt_this();
    }

    leave_critical();
    return did_process;
}

bool Processor::smp_enqueue_message(ProcessorMessage& msg)
{
    // Note that it's quite possible that the other processor may pop
    // the queue at any given time. We rely on the fact that the messages
    // are pooled and never get freed!
    auto& msg_entry = msg.per_proc_entries[id()];
    VERIFY(msg_entry.msg == &msg);
    ProcessorMessageEntry* next = nullptr;
    for (;;) {
        msg_entry.next = next;
        if (m_message_queue.compare_exchange_strong(next, &msg_entry, AK::MemoryOrder::memory_order_acq_rel))
            break;
        Processor::pause();
    }

    // If the enqueued message was the only message in the queue when posted,
    // we return true. This is used by callers when deciding whether to generate an IPI.
    return next == nullptr;
}

void Processor::smp_broadcast_message(ProcessorMessage& msg)
{
    auto& current_processor = Processor::current();

    dbgln_if(SMP_DEBUG, "SMP[{}]: Broadcast message {} to cpus: {} processor: {}", current_processor.id(), VirtualAddress(&msg), count(), VirtualAddress(&current_processor));

    msg.refs.store(count() - 1, AK::MemoryOrder::memory_order_release);
    VERIFY(msg.refs > 0);
    bool need_broadcast = false;
    for_each(
        [&](Processor& proc) {
            if (&proc != &current_processor) {
                if (proc.smp_enqueue_message(msg))
                    need_broadcast = true;
            }
        });

    // Now trigger an IPI on all other APs (unless all targets already had messages queued)
    if (need_broadcast)
        APIC::the().broadcast_ipi();
}

void Processor::smp_broadcast_wait_sync(ProcessorMessage& msg)
{
    auto& cur_proc = Processor::current();
    VERIFY(!msg.async);
    // If synchronous then we must cleanup and return the message back
    // to the pool. Otherwise, the last processor to complete it will return it
    while (msg.refs.load(AK::MemoryOrder::memory_order_consume) != 0) {
        Processor::pause();

        // We need to process any messages that may have been sent to
        // us while we're waiting. This also checks if another processor
        // may have requested us to halt.
        cur_proc.smp_process_pending_messages();
    }

    smp_cleanup_message(msg);
    smp_return_to_pool(msg);
}

void Processor::smp_unicast_message(u32 cpu, ProcessorMessage& msg, bool async)
{
    auto& current_processor = Processor::current();
    VERIFY(cpu != current_processor.id());
    auto& target_processor = processors()[cpu];
    msg.async = async;

    dbgln_if(SMP_DEBUG, "SMP[{}]: Send message {} to cpu #{} processor: {}", current_processor.id(), VirtualAddress(&msg), cpu, VirtualAddress(&target_processor));

    msg.refs.store(1u, AK::MemoryOrder::memory_order_release);
    if (target_processor->smp_enqueue_message(msg)) {
        APIC::the().send_ipi(cpu);
    }

    if (!async) {
        // If synchronous then we must cleanup and return the message back
        // to the pool. Otherwise, the last processor to complete it will return it
        while (msg.refs.load(AK::MemoryOrder::memory_order_consume) != 0) {
            Processor::pause();

            // We need to process any messages that may have been sent to
            // us while we're waiting. This also checks if another processor
            // may have requested us to halt.
            current_processor.smp_process_pending_messages();
        }

        smp_cleanup_message(msg);
        smp_return_to_pool(msg);
    }
}

void Processor::smp_unicast(u32 cpu, Function<void()> callback, bool async)
{
    auto& msg = smp_get_from_pool();
    msg.type = ProcessorMessage::Callback;
    new (msg.callback_storage) ProcessorMessage::CallbackFunction(move(callback));
    smp_unicast_message(cpu, msg, async);
}

void Processor::smp_broadcast_flush_tlb(Memory::PageDirectory const* page_directory, VirtualAddress vaddr, size_t page_count)
{
    auto& msg = smp_get_from_pool();
    msg.async = false;
    msg.type = ProcessorMessage::FlushTlb;
    msg.flush_tlb.page_directory = page_directory;
    msg.flush_tlb.ptr = vaddr.as_ptr();
    msg.flush_tlb.page_count = page_count;
    smp_broadcast_message(msg);
    // While the other processors handle this request, we'll flush ours
    flush_tlb_local(vaddr, page_count);
    // Now wait until everybody is done as well
    smp_broadcast_wait_sync(msg);
}

void Processor::smp_broadcast_halt()
{
    // We don't want to use a message, because this could have been triggered
    // by being out of memory and we might not be able to get a message
    for_each(
        [&](Processor& proc) {
            proc.m_halt_requested.store(true, AK::MemoryOrder::memory_order_release);
        });

    // Now trigger an IPI on all other APs
    APIC::the().broadcast_ipi();
}

template<typename T>
void ProcessorBase<T>::halt()
{
    if (s_smp_enabled)
        Processor::smp_broadcast_halt();

    halt_this();
}

UNMAP_AFTER_INIT void Processor::gdt_init()
{
    m_gdt_length = 0;
    m_gdtr.address = nullptr;
    m_gdtr.limit = 0;

    write_raw_gdt_entry(0x0000, 0x00000000, 0x00000000);
    write_raw_gdt_entry(GDT_SELECTOR_CODE0, 0x0000ffff, 0x00af9a00); // code0
    write_raw_gdt_entry(GDT_SELECTOR_DATA0, 0x0000ffff, 0x00af9200); // data0
    write_raw_gdt_entry(GDT_SELECTOR_DATA3, 0x0000ffff, 0x008ff200); // data3
    write_raw_gdt_entry(GDT_SELECTOR_CODE3, 0x0000ffff, 0x00affa00); // code3

    Descriptor tss_descriptor {};
    tss_descriptor.set_base(VirtualAddress { (size_t)&m_tss & 0xffffffff });
    tss_descriptor.set_limit(sizeof(TSS) - 1);
    tss_descriptor.dpl = 0;
    tss_descriptor.segment_present = 1;
    tss_descriptor.granularity = 0;
    tss_descriptor.operation_size64 = 0;
    tss_descriptor.operation_size32 = 1;
    tss_descriptor.descriptor_type = 0;
    tss_descriptor.type = Descriptor::SystemType::AvailableTSS;
    write_gdt_entry(GDT_SELECTOR_TSS, tss_descriptor); // tss

    Descriptor tss_descriptor_part2 {};
    tss_descriptor_part2.low = (size_t)&m_tss >> 32;
    write_gdt_entry(GDT_SELECTOR_TSS_PART2, tss_descriptor_part2);

    flush_gdt();
    load_task_register(GDT_SELECTOR_TSS);

    MSR gs_base(MSR_GS_BASE);
    gs_base.set((u64)this);
}

extern "C" void context_first_init(Thread* from_thread, Thread* to_thread, [[maybe_unused]] TrapFrame* trap)
{
    do_context_first_init(from_thread, to_thread);
}

extern "C" void enter_thread_context(Thread* from_thread, Thread* to_thread)
{
    VERIFY(from_thread == to_thread || from_thread->state() != Thread::State::Running);
    VERIFY(to_thread->state() == Thread::State::Running);

    bool has_fxsr = Processor::current().has_feature(CPUFeature::FXSR);
    bool has_xsave_avx_support = Processor::current().has_feature(CPUFeature::XSAVE) && Processor::current().has_feature(CPUFeature::AVX);
    Processor::set_current_thread(*to_thread);

    auto& from_regs = from_thread->regs();
    auto& to_regs = to_thread->regs();

    // NOTE: IOPL should never be non-zero in any situation, so let's panic immediately
    //       instead of carrying on with elevated I/O privileges.
    VERIFY(get_iopl_from_eflags(to_regs.flags()) == 0);

    if (has_xsave_avx_support) {
        // The specific state components saved correspond to the bits set in the requested-feature bitmap (RFBM), which is the logical-AND of EDX:EAX and XCR0.
        // https://www.moritz.systems/blog/how-debuggers-work-getting-and-setting-x86-registers-part-2/
        asm volatile("xsave %0\n"
                     : "=m"(from_thread->fpu_state())
                     : "a"(static_cast<u32>(SIMD::StateComponent::AVX | SIMD::StateComponent::SSE | SIMD::StateComponent::X87)), "d"(0u));
    } else if (has_fxsr) {
        asm volatile("fxsave %0"
                     : "=m"(from_thread->fpu_state()));
    } else {
        asm volatile("fnsave %0"
                     : "=m"(from_thread->fpu_state()));
    }

    if (from_thread->process().is_traced())
        read_debug_registers_into(from_thread->debug_register_state());

    if (to_thread->process().is_traced()) {
        write_debug_registers_from(to_thread->debug_register_state());
    } else {
        clear_debug_registers();
    }

    auto& processor = Processor::current();
    Processor::set_fs_base(to_thread->arch_specific_data().fs_base);

    if (from_regs.cr3 != to_regs.cr3)
        write_cr3(to_regs.cr3);

    to_thread->set_cpu(processor.id());

    auto in_critical = to_thread->saved_critical();
    VERIFY(in_critical > 0);
    Processor::restore_critical(in_critical);

    if (has_xsave_avx_support)
        asm volatile("xrstor %0" ::"m"(to_thread->fpu_state()), "a"(static_cast<u32>(SIMD::StateComponent::AVX | SIMD::StateComponent::SSE | SIMD::StateComponent::X87)), "d"(0u));
    else if (has_fxsr)
        asm volatile("fxrstor %0" ::"m"(to_thread->fpu_state()));
    else
        asm volatile("frstor %0" ::"m"(to_thread->fpu_state()));
}

extern "C" NO_SANITIZE_COVERAGE FlatPtr do_init_context(Thread* thread, u32 flags)
{
    VERIFY_INTERRUPTS_DISABLED();
    thread->regs().set_flags(flags);
    return Processor::current().init_context(*thread, true);
}

// FIXME: Share this code with other architectures.
template<typename T>
void ProcessorBase<T>::assume_context(Thread& thread, InterruptsState new_interrupts_state)
{
    dbgln_if(CONTEXT_SWITCH_DEBUG, "Assume context for thread {} {}", VirtualAddress(&thread), thread);

    VERIFY_INTERRUPTS_DISABLED();
    Scheduler::prepare_after_exec();
    // in_critical() should be 2 here. The critical section in Process::exec
    // and then the scheduler lock
    VERIFY(Processor::in_critical() == 2);

    u32 flags = 2 | (new_interrupts_state == InterruptsState::Enabled ? 0x200 : 0);
    do_assume_context(&thread, flags);

    VERIFY_NOT_REACHED();
}

template<typename T>
u32 ProcessorBase<T>::clear_critical()
{
    InterruptDisabler disabler;
    auto prev_critical = in_critical();
    write_gs_ptr(__builtin_offsetof(Processor, m_in_critical), 0);
    auto& proc = current();
    if (proc.m_in_irq == 0)
        proc.check_invoke_scheduler();
    return prev_critical;
}

NAKED void thread_context_first_enter(void)
{
    // enter_thread_context returns to here first time a thread is executing
    asm(
        // switch_context will have pushed from_thread and to_thread to our news
        // stack prior to thread_context_first_enter() being called, and the
        // pointer to TrapFrame was the top of the stack before that
        "    popq %rdi \n" // from_thread (argument 0)
        "    popq %rsi \n" // to_thread (argument 1)
        "    popq %rdx \n" // pointer to TrapFrame (argument 2)
        "    cld \n"
        "    call context_first_init \n"
        "    jmp common_trap_exit \n");
}

NAKED NO_SANITIZE_COVERAGE void do_assume_context(Thread*, u32)
{
    // clang-format off
    // FIXME: I hope (Thread* thread, u32 flags) aren't compiled away
    asm(
        "    movq %rdi, %r12 \n" // save thread ptr
        "    movq %rsi, %r13 \n" // save flags
        // We're going to call Processor::init_context, so just make sure
        // we have enough stack space so we don't stomp over it
        "    subq $(" __STRINGIFY(16 + REGISTER_STATE_SIZE + TRAP_FRAME_SIZE + 8) "), %rsp \n"
        "    cld \n"
        "    call do_init_context \n"
        "    movq %rax, %rsp \n"                          // move stack pointer to what Processor::init_context set up for us
        "    movq %r12, %rdi \n"                          // to_thread
        "    movq %r12, %rsi \n"                          // from_thread
        "    pushq %r12 \n"                               // to_thread (for thread_context_first_enter)
        "    pushq %r12 \n"                               // from_thread (for thread_context_first_enter)
        "    leaq thread_context_first_enter(%rip), %r12 \n" // should be same as regs.rip
        "    pushq %r12 \n"
        "    jmp enter_thread_context \n");
    // clang-format on
}

template<typename T>
StringView ProcessorBase<T>::platform_string()
{
    return "x86_64"sv;
}

template<typename T>
FlatPtr ProcessorBase<T>::init_context(Thread& thread, bool leave_crit)
{
    VERIFY(g_scheduler_lock.is_locked());
    if (leave_crit) {
        // Leave the critical section we set up in in Process::exec,
        // but because we still have the scheduler lock we should end up with 1
        VERIFY(in_critical() == 2);
        m_in_critical = 1; // leave it without triggering anything or restoring flags
    }

    u64 kernel_stack_top = thread.kernel_stack_top();

    // Add a random offset between 0-256 (16-byte aligned)
    kernel_stack_top -= round_up_to_power_of_two(get_fast_random<u8>(), 16);

    u64 stack_top = kernel_stack_top;

    // TODO: handle NT?
    VERIFY((cpu_flags() & 0x24000) == 0); // Assume !(NT | VM)

    auto& regs = thread.regs();
    bool return_to_user = (regs.cs & 3) != 0;

    stack_top -= 1 * sizeof(u64);
    *reinterpret_cast<u64*>(kernel_stack_top - 2 * sizeof(u64)) = FlatPtr(&exit_kernel_thread);

    stack_top -= sizeof(RegisterState);

    // we want to end up 16-byte aligned, %rsp + 8 should be aligned
    stack_top -= sizeof(u64);
    *reinterpret_cast<u64*>(kernel_stack_top - sizeof(u64)) = 0;

    // set up the stack so that after returning from thread_context_first_enter()
    // we will end up either in kernel mode or user mode, depending on how the thread is set up
    // However, the first step is to always start in kernel mode with thread_context_first_enter
    RegisterState& iretframe = *reinterpret_cast<RegisterState*>(stack_top);
    iretframe.rdi = regs.rdi;
    iretframe.rsi = regs.rsi;
    iretframe.rbp = regs.rbp;
    iretframe.rsp = 0;
    iretframe.rbx = regs.rbx;
    iretframe.rdx = regs.rdx;
    iretframe.rcx = regs.rcx;
    iretframe.rax = regs.rax;
    iretframe.r8 = regs.r8;
    iretframe.r9 = regs.r9;
    iretframe.r10 = regs.r10;
    iretframe.r11 = regs.r11;
    iretframe.r12 = regs.r12;
    iretframe.r13 = regs.r13;
    iretframe.r14 = regs.r14;
    iretframe.r15 = regs.r15;
    iretframe.rflags = regs.rflags;
    iretframe.rip = regs.rip;
    iretframe.cs = regs.cs;
    if (return_to_user) {
        iretframe.userspace_rsp = regs.rsp;
        iretframe.userspace_ss = GDT_SELECTOR_DATA3 | 3;
    } else {
        iretframe.userspace_rsp = kernel_stack_top;
        iretframe.userspace_ss = 0;
    }

    // make space for a trap frame
    stack_top -= sizeof(TrapFrame);
    TrapFrame& trap = *reinterpret_cast<TrapFrame*>(stack_top);
    trap.regs = &iretframe;
    trap.prev_irq_level = 0;
    trap.next_trap = nullptr;

    stack_top -= sizeof(u64); // pointer to TrapFrame
    *reinterpret_cast<u64*>(stack_top) = stack_top + 8;

    if constexpr (CONTEXT_SWITCH_DEBUG) {
        if (return_to_user) {
            dbgln("init_context {} ({}) set up to execute at rip={}:{}, rsp={}, stack_top={}, user_top={}",
                thread,
                VirtualAddress(&thread),
                iretframe.cs, regs.rip,
                VirtualAddress(regs.rsp),
                VirtualAddress(stack_top),
                iretframe.userspace_rsp);
        } else {
            dbgln("init_context {} ({}) set up to execute at rip={}:{}, rsp={}, stack_top={}",
                thread,
                VirtualAddress(&thread),
                iretframe.cs, regs.rip,
                VirtualAddress(regs.rsp),
                VirtualAddress(stack_top));
        }
    }

    // make switch_context() always first return to thread_context_first_enter()
    // in kernel mode, so set up these values so that we end up popping iretframe
    // off the stack right after the context switch completed, at which point
    // control is transferred to what iretframe is pointing to.
    regs.rip = FlatPtr(&thread_context_first_enter);
    regs.rsp0 = kernel_stack_top;
    regs.rsp = stack_top;
    regs.cs = GDT_SELECTOR_CODE0;
    return stack_top;
}

template<typename T>
void ProcessorBase<T>::switch_context(Thread*& from_thread, Thread*& to_thread)
{
    VERIFY(!m_in_irq);
    VERIFY(m_in_critical == 1);
    auto* self = static_cast<Processor*>(this);

    dbgln_if(CONTEXT_SWITCH_DEBUG, "switch_context --> switching out of: {} {}", VirtualAddress(from_thread), *from_thread);

    // m_in_critical is restored in enter_thread_context
    from_thread->save_critical(m_in_critical);

    // clang-format off
    // Switch to new thread context, passing from_thread and to_thread
    // through to the new context using registers rdx and rax
    asm volatile(
        // NOTE: changing how much we push to the stack affects thread_context_first_enter()!
        "pushfq \n"
        "pushq %%rbx \n"
        "pushq %%rcx \n"
        "pushq %%rbp \n"
        "pushq %%rsi \n"
        "pushq %%rdi \n"
        "pushq %%r8 \n"
        "pushq %%r9 \n"
        "pushq %%r10 \n"
        "pushq %%r11 \n"
        "pushq %%r12 \n"
        "pushq %%r13 \n"
        "pushq %%r14 \n"
        "pushq %%r15 \n"
        "movq %%rsp, %[from_rsp] \n"
        "leaq 1f(%%rip), %%rbx \n"
        "movq %%rbx, %[from_rip] \n"
        "movq %[to_rsp0], %%rbx \n"
        "movl %%ebx, %[tss_rsp0l] \n"
        "shrq $32, %%rbx \n"
        "movl %%ebx, %[tss_rsp0h] \n"
        "movq %[to_rsp], %%rsp \n"
        "movq %%rbp, %[from_rbp] \n"
        "pushq %[to_thread] \n"
        "pushq %[from_thread] \n"
        "pushq %[to_rip] \n"
        "cld \n"
        "movq 16(%%rsp), %%rsi \n"
        "movq 8(%%rsp), %%rdi \n"
        "jmp enter_thread_context \n"
        "1: \n"
        "popq %%rdx \n"
        "popq %%rax \n"
        "popq %%r15 \n"
        "popq %%r14 \n"
        "popq %%r13 \n"
        "popq %%r12 \n"
        "popq %%r11 \n"
        "popq %%r10 \n"
        "popq %%r9 \n"
        "popq %%r8 \n"
        "popq %%rdi \n"
        "popq %%rsi \n"
        "popq %%rbp \n"
        "popq %%rcx \n"
        "popq %%rbx \n"
        "popfq \n"
        : [from_rsp] "=m" (from_thread->regs().rsp),
        [from_rbp] "=m" (from_thread->regs().rbp),
        [from_rip] "=m" (from_thread->regs().rip),
        [tss_rsp0l] "=m" (self->m_tss.rsp0l),
        [tss_rsp0h] "=m" (self->m_tss.rsp0h),
        "=d" (from_thread), // needed so that from_thread retains the correct value
        "=a" (to_thread) // needed so that to_thread retains the correct value
        : [to_rsp] "g" (to_thread->regs().rsp),
        [to_rsp0] "g" (to_thread->regs().rsp0),
        [to_rip] "c" (to_thread->regs().rip),
        [from_thread] "d" (from_thread),
        [to_thread] "a" (to_thread)
        : "memory", "rbx"
    );
    // clang-format on

    dbgln_if(CONTEXT_SWITCH_DEBUG, "switch_context <-- from {} {} to {} {}", VirtualAddress(from_thread), *from_thread, VirtualAddress(to_thread), *to_thread);
}

template<typename T>
UNMAP_AFTER_INIT void ProcessorBase<T>::initialize_context_switching(Thread& initial_thread)
{
    VERIFY(initial_thread.process().is_kernel_process());
    auto* self = static_cast<Processor*>(this);

    auto& regs = initial_thread.regs();
    self->m_tss.iomapbase = sizeof(self->m_tss);
    self->m_tss.rsp0l = regs.rsp0 & 0xffffffff;
    self->m_tss.rsp0h = regs.rsp0 >> 32;

    m_scheduler_initialized.set();

    // clang-format off
    asm volatile(
        "movq %[new_rsp], %%rsp \n" // switch to new stack
        "pushq %[from_to_thread] \n" // to_thread
        "pushq %[from_to_thread] \n" // from_thread
        "pushq %[new_rip] \n" // save the entry rip to the stack
        "cld \n"
        "pushq %[cpu] \n" // push argument for init_finished before register is clobbered
        "call pre_init_finished \n"
        "pop %%rdi \n" // move argument for init_finished into place
        "call init_finished \n"
        "call post_init_finished \n"
        "movq 24(%%rsp), %%rdi \n" // move pointer to TrapFrame into place
        "call enter_trap_no_irq \n"
        "retq \n"
        :: [new_rsp] "g" (regs.rsp),
        [new_rip] "a" (regs.rip),
        [from_to_thread] "b" (&initial_thread),
        [cpu] "c" ((u64)id())
    );
    // clang-format on

    VERIFY_NOT_REACHED();
}

void Processor::set_fs_base(FlatPtr fs_base)
{
    MSR fs_base_msr(MSR_FS_BASE);
    fs_base_msr.set(fs_base);
}

template<typename T>
void ProcessorBase<T>::idle_begin() const
{
    Processor::s_idle_cpu_mask.fetch_or(1u << m_cpu, AK::MemoryOrder::memory_order_relaxed);
}

template<typename T>
void ProcessorBase<T>::idle_end() const
{
    Processor::s_idle_cpu_mask.fetch_and(~(1u << m_cpu), AK::MemoryOrder::memory_order_relaxed);
}

template<typename T>
void ProcessorBase<T>::wait_for_interrupt() const
{
    asm("hlt");
}

}

#include <Kernel/Arch/ProcessorFunctions.include>
