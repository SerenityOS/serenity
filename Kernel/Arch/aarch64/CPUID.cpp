/*
 * Copyright (c) 2023, Konrad <konrad@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/aarch64/CPUID.h>

namespace Kernel {

CPUFeature::Type detect_cpu_features()
{
    auto features = CPUFeature::Type(0u);

    auto instruction_set_attribute_register_0 = Aarch64::ID_AA64ISAR0_EL1::read();
    auto instruction_set_attribute_register_1 = Aarch64::ID_AA64ISAR1_EL1::read();
    auto instruction_set_attribute_register_2 = Aarch64::ID_AA64ISAR2_EL1::read();
    auto processor_feature_register_0 = Aarch64::ID_AA64PFR0_EL1::read();
    auto processor_feature_register_1 = Aarch64::ID_AA64PFR1_EL1::read();
    auto memory_model_feature_register_0 = Aarch64::ID_AA64MMFR0_EL1::read();
    auto memory_model_feature_register_1 = Aarch64::ID_AA64MMFR1_EL1::read();
    auto memory_model_feature_register_2 = Aarch64::ID_AA64MMFR2_EL1::read();
    auto memory_model_feature_register_3 = Aarch64::ID_AA64MMFR3_EL1::read();
    auto sme_feature_register_0 = Aarch64::ID_AA64SMFR0_EL1::read();
    auto sve_feature_register_0 = Aarch64::ID_AA64ZFR0_EL1::read();
    auto debug_feature_register_0 = Aarch64::ID_AA64DFR0_EL1::read();
    auto debug_feature_register_1 = Aarch64::ID_AA64DFR1_EL1::read();
    auto translation_control_register = Aarch64::TCR_EL1::read();

    // positives
    if (instruction_set_attribute_register_0.AES == 0b0001)
        features |= CPUFeature::AES;
    if (instruction_set_attribute_register_0.AES == 0b0010)
        features |= CPUFeature::PMULL;
    if (instruction_set_attribute_register_0.SHA1 == 0b0001)
        features |= CPUFeature::SHA1;
    if (instruction_set_attribute_register_0.SHA2 == 0b0001)
        features |= CPUFeature::SHA256;
    if (instruction_set_attribute_register_0.SHA2 == 0b0010)
        features |= CPUFeature::SHA512;
    if (instruction_set_attribute_register_0.CRC32 == 0b0001)
        features |= CPUFeature::CRC32;
    if (instruction_set_attribute_register_0.Atomic == 0b0010)
        features |= CPUFeature::LSE;
    if (instruction_set_attribute_register_0.Atomic == 0b0011)
        features |= CPUFeature::LSE128;
    if (instruction_set_attribute_register_0.TME == 0b0001)
        // TODO: confirm that—missing in the spec
        features |= CPUFeature::TME;
    if (instruction_set_attribute_register_0.RDM == 0b0001)
        features |= CPUFeature::RDM;
    if (instruction_set_attribute_register_0.SHA3 == 0b0001)
        features |= CPUFeature::SHA3;
    if (instruction_set_attribute_register_0.SM3 == 0b0001)
        features |= CPUFeature::SM3;
    if (instruction_set_attribute_register_0.SM4 == 0b0001)
        // TODO: confirm that—unclear spec
        features |= CPUFeature::SM4;
    if (instruction_set_attribute_register_0.DP == 0b0001)
        features |= CPUFeature::DotProd;
    if (instruction_set_attribute_register_0.FHM == 0b0001)
        features |= CPUFeature::FHM;
    if (instruction_set_attribute_register_0.TS == 0b0001)
        features |= CPUFeature::FlagM;
    if (instruction_set_attribute_register_0.TS == 0b0010)
        features |= CPUFeature::FlagM2;
    if (instruction_set_attribute_register_0.TLB == 0b0001 || instruction_set_attribute_register_0.TLB == 0b0010)
        features |= CPUFeature::TLBIOS;
    if (instruction_set_attribute_register_0.TLB == 0b0010)
        features |= CPUFeature::TLBIRANGE;
    if (instruction_set_attribute_register_0.RNDR == 0b0001)
        features |= CPUFeature::RNG;
    if (instruction_set_attribute_register_1.DPB == 0b0001)
        features |= CPUFeature::DPB;
    if (instruction_set_attribute_register_1.DPB == 0b0010)
        features |= CPUFeature::DPB2;
    if (instruction_set_attribute_register_1.API == 0b0100 && instruction_set_attribute_register_1.APA == 0b0100 && instruction_set_attribute_register_2.APA3 == 0b0100)
        features |= CPUFeature::FPAC;
    if (instruction_set_attribute_register_1.API == 0b0101 && instruction_set_attribute_register_1.APA == 0b0101 && instruction_set_attribute_register_2.APA3 == 0b0101)
        features |= CPUFeature::FPACCOMBINE;
    if (instruction_set_attribute_register_1.API == 0b0001 && instruction_set_attribute_register_1.APA == 0b0001 && instruction_set_attribute_register_2.APA3 == 0b0001)
        features |= CPUFeature::PAuth;
    if (instruction_set_attribute_register_1.API == 0b0011 && instruction_set_attribute_register_1.APA == 0b0011 && instruction_set_attribute_register_2.APA3 == 0b0011)
        features |= CPUFeature::PAuth2;
    if (instruction_set_attribute_register_1.JSCVT == 0b0001)
        features |= CPUFeature::JSCVT;
    if (instruction_set_attribute_register_1.FCMA == 0b0001)
        features |= CPUFeature::FCMA;
    if (instruction_set_attribute_register_1.LRCPC == 0b0001)
        features |= CPUFeature::LRCPC;
    if (instruction_set_attribute_register_1.LRCPC == 0b0010)
        features |= CPUFeature::LRCPC2;
    if (instruction_set_attribute_register_1.LRCPC == 0b0011)
        features |= CPUFeature::LRCPC3;
    if (instruction_set_attribute_register_1.GPA == 0b0001 && instruction_set_attribute_register_1.APA != 0b0000)
        features |= CPUFeature::PACQARMA5;
    if (instruction_set_attribute_register_1.GPI == 0b0001 && instruction_set_attribute_register_1.API != 0b0000)
        features |= CPUFeature::PACIMP;
    if (instruction_set_attribute_register_1.FRINTTS == 0b0001)
        features |= CPUFeature::FRINTTS;
    if (instruction_set_attribute_register_1.SB == 0b0001)
        features |= CPUFeature::SB;
    if (instruction_set_attribute_register_1.SPECRES == 0b0001)
        features |= CPUFeature::SPECRES;
    if (instruction_set_attribute_register_1.SPECRES == 0b0010)
        features |= CPUFeature::SPECRES2;
    if (instruction_set_attribute_register_1.BF16 == 0b0001)
        features |= CPUFeature::BF16;
    if (instruction_set_attribute_register_1.BF16 == 0b0010)
        features |= CPUFeature::EBF16;
    if (instruction_set_attribute_register_1.DGH == 0b0001)
        features |= CPUFeature::DGH;
    if (instruction_set_attribute_register_1.I8MM == 0b0001)
        features |= CPUFeature::I8MM;
    if (instruction_set_attribute_register_1.XS == 0b0001)
        features |= CPUFeature::XS;
    if (instruction_set_attribute_register_1.LS64 == 0b0001)
        features |= CPUFeature::LS64;
    if (instruction_set_attribute_register_1.LS64 == 0b0010)
        features |= CPUFeature::LS64_V;
    if (instruction_set_attribute_register_1.LS64 == 0b0011)
        features |= CPUFeature::LS64_ACCDATA;
    if (instruction_set_attribute_register_2.WFxT == 0b0010)
        features |= CPUFeature::WFxT;
    if (instruction_set_attribute_register_2.RPRES == 0b0001)
        features |= CPUFeature::RPRES;
    if (instruction_set_attribute_register_2.GPA3 == 0b0001 && instruction_set_attribute_register_2.APA3 == 0b0000)
        features |= CPUFeature::PACQARMA3;
    if (instruction_set_attribute_register_2.MOPS == 0b0001)
        features |= CPUFeature::MOPS;
    if (instruction_set_attribute_register_2.BC == 0b0001)
        features |= CPUFeature::HBC;
    if (instruction_set_attribute_register_2.PAC_frac == 0b0001)
        features |= CPUFeature::CONSTPACFIELD;
    if (instruction_set_attribute_register_2.CLRBHB == 0b0001)
        features |= CPUFeature::CLRBHB;
    if (instruction_set_attribute_register_2.SYSREG_128 == 0b0001)
        features |= CPUFeature::SYSREG128;
    if (instruction_set_attribute_register_2.SYSINSTR_128 == 0b0001)
        features |= CPUFeature::SYSINSTR128;
    if (instruction_set_attribute_register_2.PRFMSLC == 0b0001)
        features |= CPUFeature::PRFMSLC;
    if (instruction_set_attribute_register_2.RPRFM == 0b0001)
        features |= CPUFeature::RPRFM;
    if (instruction_set_attribute_register_2.CSSC == 0b0001)
        features |= CPUFeature::CSSC;
    if (processor_feature_register_0.FP == 0b0001)
        features |= CPUFeature::FP16;
    if (processor_feature_register_0.AdvSIMD != 0b0000)
        features |= CPUFeature::AdvSIMD; // TODO/FIXME: not explicit?
    if (processor_feature_register_0.AdvSIMD == 0b0001)
        features |= CPUFeature::FP16;
    // TODO: GIC
    if (processor_feature_register_0.RAS == 0b0001)
        features |= CPUFeature::RAS;
    if (processor_feature_register_0.RAS == 0b0010)
        features |= CPUFeature::DoubleFault;
    if (processor_feature_register_0.RAS == 0b0010)
        features |= CPUFeature::RASv1p1;
    if (processor_feature_register_0.RAS == 0b0001 && processor_feature_register_1.RAS_frac == 0b0001)
        features |= CPUFeature::RASv1p1;
    if (processor_feature_register_0.RAS == 0b0011)
        features |= CPUFeature::RASv2;
    if (processor_feature_register_0.SVE == 0b0001)
        features |= CPUFeature::SVE;
    if (processor_feature_register_0.SEL2 == 0b0001)
        features |= CPUFeature::SEL2;
    // TODO: MPAM
    if (processor_feature_register_0.AMU == 0b0001)
        features |= CPUFeature::AMUv1;
    if (processor_feature_register_0.AMU == 0b0010)
        features |= CPUFeature::AMUv1p1;
    if (processor_feature_register_0.DIT == 0b0001)
        features |= CPUFeature::DIT;
    if (processor_feature_register_0.RME == 0b0001)
        features |= CPUFeature::RME;
    if (processor_feature_register_0.CSV2 == 0b0001)
        features |= CPUFeature::CSV2;
    if (processor_feature_register_0.CSV2 == 0b0010)
        features |= CPUFeature::CSV2_2;
    if (processor_feature_register_0.CSV2 == 0b0011)
        features |= CPUFeature::CSV2_3;
    if (processor_feature_register_0.CSV3 == 0b0001)
        features |= CPUFeature::CSV3;
    if (processor_feature_register_1.BT == 0b0001)
        features |= CPUFeature::BTI;
    if (processor_feature_register_1.SSBS == 0b0001)
        features |= CPUFeature::SSBS;
    if (processor_feature_register_1.SSBS == 0b0010)
        features |= CPUFeature::SSBS2;
    if (processor_feature_register_1.MTE == 0b0001)
        features |= CPUFeature::MTE;
    if (processor_feature_register_1.MTE == 0b0010)
        features |= CPUFeature::MTE2;
    if (processor_feature_register_1.MTE == 0b0011)
        features |= CPUFeature::MTE3;
    if (processor_feature_register_1.MTE >= 0b0010 && processor_feature_register_1.MTEX == 0b0001) {
        features |= CPUFeature::MTE4;
        features |= CPUFeature::MTE_CANONICAL_TAGS;  // FIXME: not really explicit in the spec
        features |= CPUFeature::MTE_NO_ADDRESS_TAGS; // FIXME: not really explicit in the spec
    }
    if (processor_feature_register_1.MTE >= 0b0011 && processor_feature_register_1.MTE_frac == 0b0000)
        features |= CPUFeature::MTE_ASYM_FAULT; // FIXME: not really explicit in the spec
    if (processor_feature_register_1.SME == 0b0010)
        features |= CPUFeature::SME2;
    if (processor_feature_register_1.RNDR_trap == 0b0001)
        features |= CPUFeature::RNG_TRAP;
    if (processor_feature_register_1.CSV2_frac == 0b0001)
        features |= CPUFeature::CSV2_1p1;
    if (processor_feature_register_1.CSV2_frac == 0b0010)
        features |= CPUFeature::CSV2_1p2;
    if (processor_feature_register_1.NMI == 0b0001)
        features |= CPUFeature::NMI;
    if (processor_feature_register_1.GCS == 0b0001)
        features |= CPUFeature::GCS;
    if (processor_feature_register_1.THE == 0b0001)
        features |= CPUFeature::THE;
    if (processor_feature_register_1.DF2 == 0b0001)
        features |= CPUFeature::DoubleFault2;
    if (processor_feature_register_1.PFAR == 0b0001)
        features |= CPUFeature::PFAR;
    if (memory_model_feature_register_0.PARange == 0b0110) {
        features |= translation_control_register.DS == 0b1 ? CPUFeature::LPA2 : CPUFeature::LPA;
    }
    if (memory_model_feature_register_0.PARange == 0b0111)
        features |= CPUFeature::D128;
    if (memory_model_feature_register_0.ExS == 0b0001)
        features |= CPUFeature::ExS;
    if (memory_model_feature_register_0.FGT == 0b0001)
        features |= CPUFeature::FGT;
    if (memory_model_feature_register_0.FGT == 0b0010)
        features |= CPUFeature::FGT2;
    if (memory_model_feature_register_0.ECV == 0b0001 || memory_model_feature_register_0.ECV == 0b0010)
        features |= CPUFeature::ECV;
    if (memory_model_feature_register_1.HAFDBS == 0b0001 || memory_model_feature_register_1.HAFDBS == 0b0010)
        features |= CPUFeature::HAFDBS;
    if (memory_model_feature_register_1.VMIDBits == 0b0010)
        features |= CPUFeature::VMID16;
    if (memory_model_feature_register_1.VH == 0b0011)
        features |= CPUFeature::HAFT;
    if (memory_model_feature_register_1.HPDS == 0b0010)
        features |= CPUFeature::HPDS2;
    if (memory_model_feature_register_1.LO == 0b0001)
        features |= CPUFeature::LOR;
    if (memory_model_feature_register_1.PAN == 0b0001)
        features |= CPUFeature::PAN;
    if (memory_model_feature_register_1.PAN == 0b0010)
        features |= CPUFeature::PAN2;
    if (memory_model_feature_register_1.PAN == 0b0011)
        features |= CPUFeature::PAN3;
    if (memory_model_feature_register_1.XNX == 0b0001)
        features |= CPUFeature::XNX;
    if (memory_model_feature_register_1.TWED == 0b0001)
        features |= CPUFeature::TWED;
    if (memory_model_feature_register_1.ETS == 0b0001)
        features |= CPUFeature::ETS;
    if (memory_model_feature_register_1.HCX == 0b0001)
        features |= CPUFeature::HCX;
    if (memory_model_feature_register_1.AFP == 0b0001)
        features |= CPUFeature::AFP;
    if (memory_model_feature_register_1.nTLBPA == 0b0001)
        features |= CPUFeature::nTLBPA;
    if (memory_model_feature_register_1.TIDCP1 == 0b0001)
        features |= CPUFeature::TIDCP1;
    if (memory_model_feature_register_1.CMOW == 0b0001)
        features |= CPUFeature::CMOW;
    if (memory_model_feature_register_1.ECBHB == 0b0001)
        features |= CPUFeature::ECBHB;
    if (memory_model_feature_register_2.CnP == 0b0001)
        features |= CPUFeature::TTCNP;
    if (memory_model_feature_register_2.UAO == 0b0001)
        features |= CPUFeature::UAO;
    if (memory_model_feature_register_2.LSM == 0b0001)
        features |= CPUFeature::LSMAOC;
    if (memory_model_feature_register_2.IESB == 0b0001)
        features |= CPUFeature::IESB;
    if (memory_model_feature_register_2.VARange == 0b0001)
        features |= CPUFeature::LVA;
    if (memory_model_feature_register_2.CCIDX == 0b0001)
        features |= CPUFeature::CCIDX;
    if (memory_model_feature_register_2.NV == 0b0001)
        features |= CPUFeature::NV;
    if (memory_model_feature_register_2.NV == 0b0010)
        features |= CPUFeature::NV2;
    if (memory_model_feature_register_2.ST == 0b0001)
        features |= CPUFeature::TTST;
    if (memory_model_feature_register_2.AT == 0b0001)
        features |= CPUFeature::LSE2;
    if (memory_model_feature_register_2.IDS == 0b0001)
        features |= CPUFeature::IDST;
    if (memory_model_feature_register_2.FWB == 0b0001)
        features |= CPUFeature::S2FWB;
    if (memory_model_feature_register_2.TTL == 0b0001)
        features |= CPUFeature::TTL;
    if (memory_model_feature_register_2.BBM == 0b0000 || memory_model_feature_register_2.BBM == 0b0001 || memory_model_feature_register_2.BBM == 0b0010)
        features |= CPUFeature::BBM;
    if (memory_model_feature_register_2.EVT == 0b0001 || memory_model_feature_register_2.EVT == 0b0010)
        features |= CPUFeature::EVT;
    if (memory_model_feature_register_2.E0PD == 0b0001) {
        features |= CPUFeature::E0PD;
        features |= CPUFeature::CSV3;
    }
    if (memory_model_feature_register_3.ADERR == 0b0010 && memory_model_feature_register_3.SDERR == 0b0010)
        features |= CPUFeature::ADERR;
    if (memory_model_feature_register_3.ANERR == 0b0010 && memory_model_feature_register_3.SNERR == 0b0010)
        features |= CPUFeature::ANERR;
    if (memory_model_feature_register_3.AIE == 0b0001)
        features |= CPUFeature::AIE;
    if (memory_model_feature_register_3.MEC == 0b0001)
        features |= CPUFeature::MEC;
    if (memory_model_feature_register_3.S1PIE == 0b0001)
        features |= CPUFeature::S1PIE;
    if (memory_model_feature_register_3.S2PIE == 0b0001)
        features |= CPUFeature::S2PIE;
    if (memory_model_feature_register_3.S1POE == 0b0001)
        features |= CPUFeature::S1POE;
    if (memory_model_feature_register_3.S2POE == 0b0001)
        features |= CPUFeature::S2POE;
    if (memory_model_feature_register_3.AIE == 0b0001)
        features |= CPUFeature::AIE;
    if (memory_model_feature_register_3.MEC == 0b0001)
        features |= CPUFeature::MEC;
    if (memory_model_feature_register_3.ANERR == 0b0010 && memory_model_feature_register_3.SNERR == 0b0010)
        features |= CPUFeature::ANERR;
    if (memory_model_feature_register_3.ADERR == 0b0001 && memory_model_feature_register_3.SDERR == 0b0000 && memory_model_feature_register_3.ANERR == 0b0010 && memory_model_feature_register_3.SNERR == 0b0010 && processor_feature_register_0.RAS == 0b0011)
        features |= CPUFeature::RASv2;
    if (memory_model_feature_register_3.ADERR == 0b0010 && memory_model_feature_register_3.SDERR == 0b0010)
        features |= CPUFeature::ADERR;
    if (memory_model_feature_register_3.ADERR == 0b0010 && memory_model_feature_register_3.SDERR == 0b0010)
        features |= CPUFeature::ADERR;
    if (translation_control_register.DS == 0b1) {
        features |= CPUFeature::LVA;
    }
    if (sme_feature_register_0.F16F16 == 0b1)
        features |= CPUFeature::SME_F16F16;
    if (sme_feature_register_0.F64F64 == 0b1)
        features |= CPUFeature::SME_F64F64;
    if (sme_feature_register_0.I16I64 == 0b1111)
        features |= CPUFeature::SME_I16I64;
    if (processor_feature_register_1.SME != 0b0000) {
        if (sme_feature_register_0.SMEver == 0b0000)
            features |= CPUFeature::SME;
        if (sme_feature_register_0.SMEver == 0b0001)
            features |= CPUFeature::SME2;
        if (sme_feature_register_0.SMEver == 0b0010)
            features |= CPUFeature::SME2p1;
        if (sme_feature_register_0.FA64 == 0b1)
            features |= CPUFeature::SME_FA64; // sve_feature_register_0.I8MM/SM4/SHA3/BitPerm/AES
    }
    if (sve_feature_register_0.SVEver == 0b0001 && processor_feature_register_1.SME == 0b0001)
        features |= CPUFeature::SME; // streaming sve mode only!
    if (sve_feature_register_0.SVEver == 0b0001)
        features |= CPUFeature::SVE2; // non-streaming sve mode only!
    if (sve_feature_register_0.SVEver == 0b0010)
        features |= CPUFeature::SVE2p1; // non-streaming sve mode only!
    if (sve_feature_register_0.AES == 0b0001)
        features |= CPUFeature::SVE_AES;
    if (sve_feature_register_0.AES == 0b0010)
        features |= CPUFeature::SVE_PMULL128;
    if (sve_feature_register_0.BitPerm == 0b0001)
        features |= CPUFeature::SVE_BitPerm;
    if (sve_feature_register_0.BF16 == 0b0001)
        features |= CPUFeature::BF16;
    if (sve_feature_register_0.BF16 == 0b0010)
        features |= CPUFeature::EBF16;
    if (sve_feature_register_0.B16B16 == 0b0001 && sme_feature_register_0.B16B16 == 0b1)
        features |= CPUFeature::B16B16;
    if (sve_feature_register_0.SHA3 == 0b0001)
        features |= CPUFeature::SVE_SHA3;
    if (sve_feature_register_0.SM4 == 0b0001)
        features |= CPUFeature::SVE_SM4;
    if (sve_feature_register_0.I8MM == 0b0001)
        features |= CPUFeature::I8MM;
    if (sve_feature_register_0.F32MM == 0b0001)
        features |= CPUFeature::F32MM;
    if (sve_feature_register_0.F64MM == 0b0001)
        features |= CPUFeature::F64MM;
    if (debug_feature_register_0.DebugVer == 0b1000)
        features |= CPUFeature::Debugv8p2;
    if (debug_feature_register_0.DebugVer == 0b1001)
        features |= CPUFeature::Debugv8p4;
    if (debug_feature_register_0.DebugVer == 0b1010)
        features |= CPUFeature::Debugv8p8;
    if (debug_feature_register_0.DebugVer == 0b0111 && memory_model_feature_register_1.VH == 0b0001)
        features |= CPUFeature::VHE;
    if (debug_feature_register_0.DebugVer == 0b1101)
        features |= CPUFeature::Debugv8p9;
    if (debug_feature_register_0.PMUVer == 0b0001)
        features |= CPUFeature::PMUv3;
    if (debug_feature_register_0.PMUVer == 0b0100)
        features |= CPUFeature::PMUv3p1;
    if (debug_feature_register_0.PMUVer == 0b0101)
        features |= CPUFeature::PMUv3p4;
    if (debug_feature_register_0.PMUVer == 0b0110)
        features |= CPUFeature::PMUv3p5;
    if (debug_feature_register_0.PMUVer == 0b0111)
        features |= CPUFeature::PMUv3p7;
    if (debug_feature_register_0.PMUVer == 0b1000)
        features |= CPUFeature::PMUv3p8;
    if (debug_feature_register_0.PMUVer == 0b1001)
        features |= CPUFeature::PMUv3p9;
    if (debug_feature_register_0.PMSS == 0b0001)
        features |= CPUFeature::PMUv3_SS;
    if (debug_feature_register_0.SEBEP == 0b0001)
        features |= CPUFeature::SEBEP;
    if (debug_feature_register_0.PMSVer == 0b0001)
        features |= CPUFeature::SPE;
    if (debug_feature_register_0.PMSVer == 0b0010)
        features |= CPUFeature::SPEv1p1;
    if (debug_feature_register_0.PMSVer == 0b0011)
        features |= CPUFeature::SPEv1p2;
    if (debug_feature_register_0.PMSVer == 0b0100)
        features |= CPUFeature::SPEv1p3;
    if (debug_feature_register_0.PMSVer == 0b0101)
        features |= CPUFeature::SPEv1p4;
    if (debug_feature_register_0.PMSVer == 0b0011)
        features |= CPUFeature::SPEv1p2;
    if (debug_feature_register_0.DoubleLock == 0b0000)
        features |= CPUFeature::DoubleLock;
    if (debug_feature_register_0.TraceFilt == 0b0001)
        features |= CPUFeature::TRF;
    if (debug_feature_register_0.TraceBuffer == 0b0001)
        features |= CPUFeature::TRBE;
    if (debug_feature_register_0.MTPMU == 0b0001)
        features |= CPUFeature::MTPMU; // TODO: has additional notes
    if (debug_feature_register_0.BRBE == 0b0001)
        features |= CPUFeature::BRBE;
    if (debug_feature_register_0.BRBE == 0b0010)
        features |= CPUFeature::BRBEv1p1;
    if (debug_feature_register_0.ExtTrcBuff == 0b0001 && features.has_flag(CPUFeature::TRBE)) // FIXME: order-dependent!
        features |= CPUFeature::TRBE_EXT;
    if (debug_feature_register_0.HPMN0 == 0b0001)
        features |= CPUFeature::HPMN0;
    if (debug_feature_register_1.ABLE == 0b0001)
        features |= CPUFeature::ABLE;
    if (debug_feature_register_1.EBEP == 0b0001)
        features |= CPUFeature::EBEP;
    if (debug_feature_register_1.ITE == 0b0001)
        features |= CPUFeature::ITE;
    if (debug_feature_register_1.PMICNTR == 0b0001)
        features |= CPUFeature::PMUv3_ICNTR;
    if (debug_feature_register_1.SPMU == 0b0001)
        features |= CPUFeature::SPMU;
    if (debug_feature_register_1.ABLE == 0b0001)
        features |= CPUFeature::ABLE;
    if (debug_feature_register_1.EBEP == 0b0001)
        features |= CPUFeature::EBEP;
    if (debug_feature_register_1.ITE == 0b0001)
        features |= CPUFeature::ITE;
    if (debug_feature_register_1.PMICNTR == 0b0001)
        features |= CPUFeature::PMUv3_ICNTR;
    if (debug_feature_register_1.SPMU == 0b0001)
        features |= CPUFeature::SPMU;

    // negatives
    if (sme_feature_register_0.B16B16 == 0b0000)
        features &= ~(CPUFeature::SVE2p1 | CPUFeature::SME2p1);
    if (sme_feature_register_0.F16F16 == 0b0)
        features &= ~CPUFeature::SME2p1;
    if (sve_feature_register_0.B16B16 == 0b0000)
        features &= ~(CPUFeature::SVE2p1 | CPUFeature::SME2p1);
    if (sve_feature_register_0.B16B16 == 0b0001 && sme_feature_register_0.B16B16 == 0b1)
        features |= CPUFeature::B16B16;

    return features;
}

// https://developer.arm.com/downloads/-/exploration-tools/feature-names-for-a-profile
StringView cpu_feature_to_name(CPUFeature::Type const& feature)
{
    // 2022 Architecture Extensions
    if (feature == CPUFeature::ABLE)
        return "ABLE"sv;
    if (feature == CPUFeature::ADERR)
        return "ADERR"sv;
    if (feature == CPUFeature::ANERR)
        return "ANERR"sv;
    if (feature == CPUFeature::AIE)
        return "AIE"sv;
    if (feature == CPUFeature::B16B16)
        return "B16B16"sv;
    if (feature == CPUFeature::CLRBHB)
        return "CLRBHB"sv;
    if (feature == CPUFeature::CHK)
        return "CHK"sv;
    if (feature == CPUFeature::CSSC)
        return "CSSC"sv;
    if (feature == CPUFeature::CSV2_2)
        return "CSV2_2"sv;
    if (feature == CPUFeature::CSV2_3)
        return "CSV2_3"sv;
    if (feature == CPUFeature::D128)
        return "D128"sv;
    if (feature == CPUFeature::Debugv8p9)
        return "Debugv8p9"sv;
    if (feature == CPUFeature::DoubleFault2)
        return "DoubleFault2"sv;
    if (feature == CPUFeature::EBEP)
        return "EBEP"sv;
    if (feature == CPUFeature::ECBHB)
        return "ECBHB"sv;
    if (feature == CPUFeature::ETEv1p3)
        return "ETEv1p3"sv;
    if (feature == CPUFeature::FGT2)
        return "FGT2"sv;
    if (feature == CPUFeature::GCS)
        return "GCS"sv;
    if (feature == CPUFeature::HAFT)
        return "HAFT"sv;
    if (feature == CPUFeature::ITE)
        return "ITE"sv;
    if (feature == CPUFeature::LRCPC3)
        return "LRCPC3"sv;
    if (feature == CPUFeature::LSE128)
        return "LSE128"sv;
    if (feature == CPUFeature::LVA3)
        return "LVA3"sv;
    if (feature == CPUFeature::MEC)
        return "MEC"sv;
    if (feature == CPUFeature::MTE4)
        return "MTE4"sv;
    if (feature == CPUFeature::MTE_CANONICAL_TAGS)
        return "MTE_CANONICAL_TAGS"sv;
    if (feature == CPUFeature::MTE_TAGGED_FAR)
        return "MTE_TAGGED_FAR"sv;
    if (feature == CPUFeature::MTE_STORE_ONLY)
        return "MTE_STORE_ONLY"sv;
    if (feature == CPUFeature::MTE_NO_ADDRESS_TAGS)
        return "MTE_NO_ADDRESS_TAGS"sv;
    if (feature == CPUFeature::MTE_ASYM_FAULT)
        return "MTE_ASYM_FAULT"sv;
    if (feature == CPUFeature::MTE_ASYNC)
        return "MTE_ASYNC"sv;
    if (feature == CPUFeature::MTE_PERM)
        return "MTE_PERM"sv;
    if (feature == CPUFeature::PCSRv8p9)
        return "PCSRv8p9"sv;
    if (feature == CPUFeature::PIE)
        return "PIE"sv;
    if (feature == CPUFeature::POE)
        return "POE"sv;
    if (feature == CPUFeature::S1PIE)
        return "S1PIE"sv;
    if (feature == CPUFeature::S2PIE)
        return "S2PIE"sv;
    if (feature == CPUFeature::S1POE)
        return "S1POE"sv;
    if (feature == CPUFeature::S2POE)
        return "S2POE"sv;
    if (feature == CPUFeature::PMUv3p9)
        return "PMUv3p9"sv;
    if (feature == CPUFeature::PMUv3_EDGE)
        return "PMUv3_EDGE"sv;
    if (feature == CPUFeature::PMUv3_ICNTR)
        return "PMUv3_ICNTR"sv;
    if (feature == CPUFeature::PMUv3_SS)
        return "PMUv3_SS"sv;
    if (feature == CPUFeature::PRFMSLC)
        return "PRFMSLC"sv;
    if (feature == CPUFeature::PFAR)
        return "PFAR"sv;
    if (feature == CPUFeature::RASv2)
        return "RASv2"sv;
    if (feature == CPUFeature::RPZ)
        return "RPZ"sv;
    if (feature == CPUFeature::RPRFM)
        return "RPRFM"sv;
    if (feature == CPUFeature::SCTLR2)
        return "SCTLR2"sv;
    if (feature == CPUFeature::SEBEP)
        return "SEBEP"sv;
    if (feature == CPUFeature::SME_F16F16)
        return "SME_F16F16"sv;
    if (feature == CPUFeature::SME2)
        return "SME2"sv;
    if (feature == CPUFeature::SME2p1)
        return "SME2p1"sv;
    if (feature == CPUFeature::SPECRES2)
        return "SPECRES2"sv;
    if (feature == CPUFeature::SPMU)
        return "SPMU"sv;
    if (feature == CPUFeature::SPEv1p4)
        return "SPEv1p4"sv;
    if (feature == CPUFeature::SPE_FDS)
        return "SPE_FDS"sv;
    if (feature == CPUFeature::SVE2p1)
        return "SVE2p1"sv;
    if (feature == CPUFeature::SYSINSTR128)
        return "SYSINSTR128"sv;
    if (feature == CPUFeature::SYSREG128)
        return "SYSREG128"sv;
    if (feature == CPUFeature::TCR2)
        return "TCR2"sv;
    if (feature == CPUFeature::THE)
        return "THE"sv;
    if (feature == CPUFeature::TRBE_EXT)
        return "TRBE_EXT"sv;
    if (feature == CPUFeature::TRBE_MPAM)
        return "TRBE_MPAM"sv;

    // 2021 Architecture Extensions
    if (feature == CPUFeature::CMOW)
        return "CMOW"sv;
    if (feature == CPUFeature::CONSTPACFIELD)
        return "CONSTPACFIELD"sv;
    if (feature == CPUFeature::Debugv8p8)
        return "Debugv8p8"sv;
    if (feature == CPUFeature::HBC)
        return "HBC"sv;
    if (feature == CPUFeature::HPMN0)
        return "HPMN0"sv;
    if (feature == CPUFeature::NMI)
        return "NMI"sv;
    if (feature == CPUFeature::GICv3_NMI)
        return "GICv3_NMI"sv;
    if (feature == CPUFeature::MOPS)
        return "MOPS"sv;
    if (feature == CPUFeature::PACQARMA3)
        return "PACQARMA3"sv;
    if (feature == CPUFeature::PMUv3_TH)
        return "PMUv3_TH"sv;
    if (feature == CPUFeature::PMUv3p8)
        return "PMUv3p8"sv;
    if (feature == CPUFeature::PMUv3_EXT64)
        return "PMUv3_EXT64"sv;
    if (feature == CPUFeature::PMUv3_EXT32)
        return "PMUv3_EXT32"sv;
    if (feature == CPUFeature::RNG_TRAP)
        return "RNG_TRAP"sv;
    if (feature == CPUFeature::SPEv1p3)
        return "SPEv1p3"sv;
    if (feature == CPUFeature::TIDCP1)
        return "TIDCP1"sv;
    if (feature == CPUFeature::BRBEv1p1)
        return "BRBEv1p1"sv;

    // 2020 Architecture Extensions
    if (feature == CPUFeature::AFP)
        return "AFP"sv;
    if (feature == CPUFeature::HCX)
        return "HCX"sv;
    if (feature == CPUFeature::LPA2)
        return "LPA2"sv;
    if (feature == CPUFeature::LS64)
        return "LS64"sv;
    if (feature == CPUFeature::LS64_V)
        return "LS64_V"sv;
    if (feature == CPUFeature::LS64_ACCDATA)
        return "LS64_ACCDATA"sv;
    if (feature == CPUFeature::MTE3)
        return "MTE3"sv;
    if (feature == CPUFeature::PAN3)
        return "PAN3"sv;
    if (feature == CPUFeature::PMUv3p7)
        return "PMUv3p7"sv;
    if (feature == CPUFeature::RPRES)
        return "RPRES"sv;
    if (feature == CPUFeature::RME)
        return "RME"sv;
    if (feature == CPUFeature::SME_FA64)
        return "SME_FA64"sv;
    if (feature == CPUFeature::SME_F64F64)
        return "SME_F64F64"sv;
    if (feature == CPUFeature::SME_I16I64)
        return "SME_I16I64"sv;
    if (feature == CPUFeature::EBF16)
        return "EBF16"sv;
    if (feature == CPUFeature::SPEv1p2)
        return "SPEv1p2"sv;
    if (feature == CPUFeature::WFxT)
        return "WFxT"sv;
    if (feature == CPUFeature::XS)
        return "XS"sv;
    if (feature == CPUFeature::BRBE)
        return "BRBE"sv;

    // Features introduced prior to 2020
    if (feature == CPUFeature::AdvSIMD)
        return "AdvSIMD"sv;
    if (feature == CPUFeature::AES)
        return "AES"sv;
    if (feature == CPUFeature::PMULL)
        return "PMULL"sv;
    if (feature == CPUFeature::CP15SDISABLE2)
        return "CP15SDISABLE2"sv;
    if (feature == CPUFeature::CSV2)
        return "CSV2"sv;
    if (feature == CPUFeature::CSV2_1p1)
        return "CSV2_1p1"sv;
    if (feature == CPUFeature::CSV2_1p2)
        return "CSV2_1p2"sv;
    if (feature == CPUFeature::CSV2)
        return "CSV2"sv;
    if (feature == CPUFeature::CSV3)
        return "CSV3"sv;
    if (feature == CPUFeature::DGH)
        return "DGH"sv;
    if (feature == CPUFeature::DoubleLock)
        return "DoubleLock"sv;
    if (feature == CPUFeature::ETS)
        return "ETS"sv;
    if (feature == CPUFeature::FP)
        return "FP"sv;
    if (feature == CPUFeature::IVIPT)
        return "IVIPT"sv;
    if (feature == CPUFeature::PCSRv8)
        return "PCSRv8"sv;
    if (feature == CPUFeature::SPECRES)
        return "SPECRES"sv;
    if (feature == CPUFeature::RAS)
        return "RAS"sv;
    if (feature == CPUFeature::SB)
        return "SB"sv;
    if (feature == CPUFeature::SHA1)
        return "SHA1"sv;
    if (feature == CPUFeature::SHA256)
        return "SHA256"sv;
    if (feature == CPUFeature::SSBS)
        return "SSBS2"sv;
    if (feature == CPUFeature::SSBS2)
        return "SSBS2"sv;
    if (feature == CPUFeature::CRC32)
        return "CRC32"sv;
    if (feature == CPUFeature::nTLBPA)
        return "nTLBPA"sv;
    if (feature == CPUFeature::Debugv8p1)
        return "Debugv8p1"sv;
    if (feature == CPUFeature::HPDS)
        return "HPDS"sv;
    if (feature == CPUFeature::LOR)
        return "LOR"sv;
    if (feature == CPUFeature::LSE)
        return "LSE"sv;
    if (feature == CPUFeature::PAN)
        return "PAN"sv;
    if (feature == CPUFeature::PMUv3p1)
        return "PMUv3p1"sv;
    if (feature == CPUFeature::RDM)
        return "RDM"sv;
    if (feature == CPUFeature::HAFDBS)
        return "HAFDBS"sv;
    if (feature == CPUFeature::VHE)
        return "VHE"sv;
    if (feature == CPUFeature::VMID16)
        return "VMID16"sv;
    if (feature == CPUFeature::AA32BF16)
        return "AA32BF16"sv;
    if (feature == CPUFeature::AA32HPD)
        return "AA32HPD"sv;
    if (feature == CPUFeature::AA32I8MM)
        return "AA32I8MM"sv;
    if (feature == CPUFeature::PAN2)
        return "PAN2"sv;
    if (feature == CPUFeature::BF16)
        return "BF16"sv;
    if (feature == CPUFeature::DPB2)
        return "DPB2"sv;
    if (feature == CPUFeature::DPB)
        return "DPB"sv;
    if (feature == CPUFeature::Debugv8p2)
        return "Debugv8p2"sv;
    if (feature == CPUFeature::DotProd)
        return "DotProd"sv;
    if (feature == CPUFeature::EVT)
        return "EVT"sv;
    if (feature == CPUFeature::F32MM)
        return "F32MM"sv;
    if (feature == CPUFeature::F64MM)
        return "F64MM"sv;
    if (feature == CPUFeature::FHM)
        return "FHM"sv;
    if (feature == CPUFeature::FP16)
        return "FP16"sv;
    if (feature == CPUFeature::I8MM)
        return "I8MM"sv;
    if (feature == CPUFeature::IESB)
        return "IESB"sv;
    if (feature == CPUFeature::LPA)
        return "LPA"sv;
    if (feature == CPUFeature::LSMAOC)
        return "LSMAOC"sv;
    if (feature == CPUFeature::LVA)
        return "LVA"sv;
    if (feature == CPUFeature::MPAM)
        return "MPAM"sv;
    if (feature == CPUFeature::PCSRv8p2)
        return "PCSRv8p2"sv;
    if (feature == CPUFeature::SHA3)
        return "SHA3"sv;
    if (feature == CPUFeature::SHA512)
        return "SHA512"sv;
    if (feature == CPUFeature::SM3)
        return "SM3"sv;
    if (feature == CPUFeature::SM4)
        return "SM4"sv;
    if (feature == CPUFeature::SPE)
        return "SPE"sv;
    if (feature == CPUFeature::SVE)
        return "SVE"sv;
    if (feature == CPUFeature::TTCNP)
        return "TTCNP"sv;
    if (feature == CPUFeature::HPDS2)
        return "HPDS2"sv;
    if (feature == CPUFeature::XNX)
        return "XNX"sv;
    if (feature == CPUFeature::UAO)
        return "UAO"sv;
    if (feature == CPUFeature::VPIPT)
        return "VPIPT"sv;
    if (feature == CPUFeature::CCIDX)
        return "CCIDX"sv;
    if (feature == CPUFeature::FCMA)
        return "FCMA"sv;
    if (feature == CPUFeature::DoPD)
        return "DoPD"sv;
    if (feature == CPUFeature::EPAC)
        return "EPAC"sv;
    if (feature == CPUFeature::FPAC)
        return "FPAC"sv;
    if (feature == CPUFeature::FPACCOMBINE)
        return "FPACCOMBINE"sv;
    if (feature == CPUFeature::JSCVT)
        return "JSCVT"sv;
    if (feature == CPUFeature::LRCPC)
        return "LRCPC"sv;
    if (feature == CPUFeature::NV)
        return "NV"sv;
    if (feature == CPUFeature::PACQARMA5)
        return "PACQARMA5"sv;
    if (feature == CPUFeature::PACIMP)
        return "PACIMP"sv;
    if (feature == CPUFeature::PAuth)
        return "PAuth"sv;
    if (feature == CPUFeature::PAuth2)
        return "PAuth2"sv;
    if (feature == CPUFeature::SPEv1p1)
        return "SPEv1p1"sv;
    if (feature == CPUFeature::AMUv1)
        return "AMUv1"sv;
    if (feature == CPUFeature::CNTSC)
        return "CNTSC"sv;
    if (feature == CPUFeature::Debugv8p4)
        return "Debugv8p4"sv;
    if (feature == CPUFeature::DoubleFault)
        return "DoubleFault"sv;
    if (feature == CPUFeature::DIT)
        return "DIT"sv;
    if (feature == CPUFeature::FlagM)
        return "FlagM"sv;
    if (feature == CPUFeature::IDST)
        return "IDST"sv;
    if (feature == CPUFeature::LRCPC2)
        return "LRCPC2"sv;
    if (feature == CPUFeature::LSE2)
        return "LSE2"sv;
    if (feature == CPUFeature::NV2)
        return "NV2"sv;
    if (feature == CPUFeature::PMUv3p4)
        return "PMUv3p4"sv;
    if (feature == CPUFeature::RASv1p1)
        return "RASv1p1"sv;
    if (feature == CPUFeature::S2FWB)
        return "S2FWB"sv;
    if (feature == CPUFeature::SEL2)
        return "SEL2"sv;
    if (feature == CPUFeature::TLBIOS)
        return "TLBIOS"sv;
    if (feature == CPUFeature::TLBIRANGE)
        return "TLBIRANGE"sv;
    if (feature == CPUFeature::TRF)
        return "TRF"sv;
    if (feature == CPUFeature::TTL)
        return "TTL"sv;
    if (feature == CPUFeature::BBM)
        return "BBM"sv;
    if (feature == CPUFeature::TTST)
        return "TTST"sv;
    if (feature == CPUFeature::BTI)
        return "BTI"sv;
    if (feature == CPUFeature::FlagM2)
        return "FlagM2"sv;
    if (feature == CPUFeature::ExS)
        return "ExS"sv;
    if (feature == CPUFeature::E0PD)
        return "E0PD"sv;
    if (feature == CPUFeature::FRINTTS)
        return "FRINTTS"sv;
    if (feature == CPUFeature::GTG)
        return "GTG"sv;
    if (feature == CPUFeature::MTE)
        return "MTE"sv;
    if (feature == CPUFeature::MTE2)
        return "MTE2"sv;
    if (feature == CPUFeature::PMUv3p5)
        return "PMUv3p5"sv;
    if (feature == CPUFeature::RNG)
        return "RNG"sv;
    if (feature == CPUFeature::AMUv1p1)
        return "AMUv1p1"sv;
    if (feature == CPUFeature::ECV)
        return "ECV"sv;
    if (feature == CPUFeature::FGT)
        return "FGT"sv;
    if (feature == CPUFeature::MPAMv0p1)
        return "MPAMv0p1"sv;
    if (feature == CPUFeature::MPAMv1p1)
        return "MPAMv1p1"sv;
    if (feature == CPUFeature::MTPMU)
        return "MTPMU"sv;
    if (feature == CPUFeature::TWED)
        return "TWED"sv;
    if (feature == CPUFeature::ETMv4)
        return "ETMv4"sv;
    if (feature == CPUFeature::ETMv4p1)
        return "ETMv4p1"sv;
    if (feature == CPUFeature::ETMv4p2)
        return "ETMv4p2"sv;
    if (feature == CPUFeature::ETMv4p3)
        return "ETMv4p3"sv;
    if (feature == CPUFeature::ETMv4p4)
        return "ETMv4p4"sv;
    if (feature == CPUFeature::ETMv4p5)
        return "ETMv4p5"sv;
    if (feature == CPUFeature::ETMv4p6)
        return "ETMv4p6"sv;
    if (feature == CPUFeature::GICv3)
        return "GICv3"sv;
    if (feature == CPUFeature::GICv3p1)
        return "GICv3p1"sv;
    if (feature == CPUFeature::GICv3_LEGACY)
        return "GICv3_LEGACY"sv;
    if (feature == CPUFeature::GICv3_TDIR)
        return "GICv3_TDIR"sv;
    if (feature == CPUFeature::GICv4)
        return "GICv4"sv;
    if (feature == CPUFeature::GICv4p1)
        return "GICv4p1"sv;
    if (feature == CPUFeature::PMUv3)
        return "PMUv3"sv;
    if (feature == CPUFeature::ETE)
        return "ETE"sv;
    if (feature == CPUFeature::ETEv1p1)
        return "ETEv1p1"sv;
    if (feature == CPUFeature::SVE2)
        return "SVE2"sv;
    if (feature == CPUFeature::SVE_AES)
        return "SVE_AES"sv;
    if (feature == CPUFeature::SVE_PMULL128)
        return "SVE_PMULL128"sv;
    if (feature == CPUFeature::SVE_BitPerm)
        return "SVE_BitPerm"sv;
    if (feature == CPUFeature::SVE_SHA3)
        return "SVE_SHA3"sv;
    if (feature == CPUFeature::SVE_SM4)
        return "SVE_SM4"sv;
    if (feature == CPUFeature::TME)
        return "TME"sv;
    if (feature == CPUFeature::TRBE)
        return "TRBE"sv;
    if (feature == CPUFeature::SME)
        return "SME"sv;

    VERIFY_NOT_REACHED();
}

// https://developer.arm.com/downloads/-/exploration-tools/feature-names-for-a-profile
StringView cpu_feature_to_description(CPUFeature::Type const& feature)
{
    // 2022 Architecture Extensions
    if (feature == CPUFeature::ABLE)
        return "Address Breakpoint Linking extension"sv;
    if (feature == CPUFeature::ADERR)
        return "RASv2 Additional Error syndrome reporting, for Device memory"sv;
    if (feature == CPUFeature::ANERR)
        return "RASv2 Additional Error syndrome reporting, for Normal memory"sv;
    if (feature == CPUFeature::AIE)
        return "Memory Attribute Index Enhancement"sv;
    if (feature == CPUFeature::B16B16)
        return "Non-widening BFloat16 to BFloat16 arithmetic for SVE2.1 and SME2.1"sv;
    if (feature == CPUFeature::CLRBHB)
        return "A new instruction CLRBHB is added in HINT space"sv;
    if (feature == CPUFeature::CHK)
        return "Detect when Guarded Control Stacks are implemented"sv;
    if (feature == CPUFeature::CSSC)
        return "Common Short Sequence Compression scalar integer instructions"sv;
    if (feature == CPUFeature::CSV2_3)
        return "New identification mechanism for Branch History information"sv;
    if (feature == CPUFeature::D128)
        return "128-bit Translation Tables, 56 bit PA"sv;
    if (feature == CPUFeature::Debugv8p9)
        return "Debug 2022"sv;
    if (feature == CPUFeature::DoubleFault2)
        return "Error exception routing extensions"sv; // NOTE: removed trailing dot compared to source!
    if (feature == CPUFeature::EBEP)
        return "Exception-based event profiling"sv;
    if (feature == CPUFeature::ECBHB)
        return "Imposes restrictions on branch history speculation around exceptions"sv;
    if (feature == CPUFeature::ETEv1p3)
        return "ETE support for v9.3"sv;
    if (feature == CPUFeature::FGT2)
        return "Fine-grained traps 2"sv;
    if (feature == CPUFeature::GCS)
        return "Guarded Control Stack Extension"sv;
    if (feature == CPUFeature::HAFT)
        return "Hardware managed Access Flag for Table descriptors"sv;
    if (feature == CPUFeature::ITE)
        return "Instrumentation trace extension"sv;
    if (feature == CPUFeature::LRCPC3)
        return "Load-Acquire RCpc instructions version 3"sv;
    if (feature == CPUFeature::LSE128)
        return "128-bit Atomics"sv;
    if (feature == CPUFeature::LVA3)
        return "56-bit VA"sv;
    if (feature == CPUFeature::MEC)
        return "Memory Encryption Contexts"sv;
    if (feature == CPUFeature::MTE4)
        return "Support for Canonical tag checking, reporting of all non-address bits on a fault, Store-only Tag checking, Memory tagging with Address tagging disabled"sv;
    if (feature == CPUFeature::MTE_CANONICAL_TAGS)
        return "Support for Canonical tag checking"sv;
    if (feature == CPUFeature::MTE_TAGGED_FAR)
        return "Support for reporting of all non-address bits on a fault"sv;
    if (feature == CPUFeature::MTE_STORE_ONLY)
        return "Support for Store-only Tag checking"sv;
    if (feature == CPUFeature::MTE_NO_ADDRESS_TAGS)
        return "Support for Memory tagging with Address tagging disabled"sv;
    if (feature == CPUFeature::MTE_ASYM_FAULT)
        return "Asymmetric Tag Check Fault handling"sv;
    if (feature == CPUFeature::MTE_ASYNC)
        return "Asynchronous Tag Check Fault handling"sv;
    if (feature == CPUFeature::MTE_PERM)
        return "Allocation tag access permission"sv;
    if (feature == CPUFeature::PCSRv8p9)
        return "PCSR disable control"sv;
    if (feature == CPUFeature::PIE)
        return "Permission model enhancements"sv;
    if (feature == CPUFeature::POE)
        return "Permission model enhancements"sv;
    if (feature == CPUFeature::S1PIE)
        return "Permission model enhancements"sv;
    if (feature == CPUFeature::S2PIE)
        return "Permission model enhancements"sv;
    if (feature == CPUFeature::S1POE)
        return "Permission model enhancements"sv;
    if (feature == CPUFeature::S2POE)
        return "Permission model enhancements"sv;
    if (feature == CPUFeature::PMUv3p9)
        return "EL0 access controls for PMU event counters"sv;
    if (feature == CPUFeature::PMUv3_EDGE)
        return "PMU event edge detection"sv;
    if (feature == CPUFeature::PMUv3_ICNTR)
        return "PMU instruction counter"sv;
    if (feature == CPUFeature::PMUv3_SS)
        return "PMU snapshot"sv;
    if (feature == CPUFeature::PRFMSLC)
        return "Prefetching enhancements"sv;
    if (feature == CPUFeature::PFAR)
        // https://developer.arm.com/documentation/ddi0602/2022-12/Shared-Pseudocode/Shared-Functions?lang=en
        // Library pseudocode for shared/functions/extension/HavePFAR
        return "Physical Fault Address Extension (RASv2)"sv;
    if (feature == CPUFeature::RASv2)
        return "Reliability, Availability, and Serviceability (RAS) Extension version 2"sv;
    if (feature == CPUFeature::RPZ)
        return "RPZ (RASv2)"sv; // Note: not really known
    if (feature == CPUFeature::RPRFM)
        return "RPRFM range prefetch hint instruction"sv;
    if (feature == CPUFeature::SCTLR2)
        return "Extension to SCTLR_ELx"sv;
    if (feature == CPUFeature::SEBEP)
        return "Synchronous Exception-based event profiling"sv;
    if (feature == CPUFeature::SME_F16F16)
        return "Non-widening half-precision FP16 to FP16 arithmetic for SME2.1"sv;
    if (feature == CPUFeature::SME2)
        return "Scalable Matrix Extension version 2"sv;
    if (feature == CPUFeature::SME2p1)
        return "Scalable Matrix Extension version 2.1"sv;
    if (feature == CPUFeature::SPECRES2)
        return "Adds new Clear Other Speculative Predictions instruction"sv;
    if (feature == CPUFeature::SPMU)
        return "System PMU"sv;
    if (feature == CPUFeature::SPEv1p4)
        return "Additional SPE events"sv;
    if (feature == CPUFeature::SPE_FDS)
        return "SPE filtering by data source"sv;
    if (feature == CPUFeature::SVE2p1)
        return "Scalable Vector Extension version SVE2.1"sv;
    if (feature == CPUFeature::SYSINSTR128)
        return "128-bit System instructions"sv;
    if (feature == CPUFeature::SYSREG128)
        return "128-bit System registers"sv;
    if (feature == CPUFeature::TCR2)
        return "Extension to TCR_ELx"sv;
    if (feature == CPUFeature::THE)
        return "Translation Hardening Extension"sv;
    if (feature == CPUFeature::TRBE_EXT)
        return "Represents TRBE external mode"sv;
    if (feature == CPUFeature::TRBE_MPAM)
        return "Trace Buffer MPAM extensions"sv;

    // 2021 Architecture Extensions
    if (feature == CPUFeature::CMOW)
        return "Control for cache maintenance permission"sv;
    if (feature == CPUFeature::CONSTPACFIELD)
        return "PAC Algorithm enhancement"sv;
    if (feature == CPUFeature::Debugv8p8)
        return "Debug v8.8"sv;
    if (feature == CPUFeature::HBC)
        return "Hinted conditional branches"sv;
    if (feature == CPUFeature::HPMN0)
        return "Setting of MDCR_EL2.HPMN to zero"sv;
    if (feature == CPUFeature::NMI)
        return "Non-maskable Interrupts"sv;
    if (feature == CPUFeature::GICv3_NMI)
        return "Non-maskable Interrupts"sv;
    if (feature == CPUFeature::MOPS)
        return "Standardization of memory operations"sv;
    if (feature == CPUFeature::PACQARMA3)
        return "Pointer authentication - QARMA3 algorithm"sv;
    if (feature == CPUFeature::PMUv3_TH)
        return "Event counting threshold"sv;
    if (feature == CPUFeature::PMUv3p8)
        return "Armv8.8 PMU Extensions"sv;
    if (feature == CPUFeature::PMUv3_EXT64)
        return "Optional 64-bit external interface to the Performance Monitors"sv;
    if (feature == CPUFeature::PMUv3_EXT32)
        return "Represents the original mostly 32-bit external interface to the Performance Monitors"sv;
    if (feature == CPUFeature::RNG_TRAP)
        return "Trapping support for RNDR and RNDRRS"sv;
    if (feature == CPUFeature::SPEv1p3)
        return "Armv8.8 Statistical Profiling Extensions"sv;
    if (feature == CPUFeature::TIDCP1)
        return "EL0 use of IMPLEMENTATION DEFINED functionality"sv;
    if (feature == CPUFeature::BRBEv1p1)
        return "Branch Record Buffer Extensions version 1.1"sv;

    // 2020 Architecture Extensions
    if (feature == CPUFeature::AFP)
        return "Alternate floating-point behavior"sv;
    if (feature == CPUFeature::HCX)
        return "Support for the HCRX_EL2 register"sv;
    if (feature == CPUFeature::LPA2)
        return "Larger physical address for 4KB and 16KB translation granules"sv;
    if (feature == CPUFeature::LS64)
        return "Support for 64 byte loads/stores without return"sv;
    if (feature == CPUFeature::LS64_V)
        return "Support for 64-byte stores with return"sv;
    if (feature == CPUFeature::LS64_ACCDATA)
        return "Support for 64-byte EL0 stores with return"sv;
    if (feature == CPUFeature::MTE3)
        return "MTE Asymmetric Fault Handling"sv;
    if (feature == CPUFeature::PAN3)
        return "Support for SCTLR_ELx.EPAN"sv;
    if (feature == CPUFeature::PMUv3p7)
        return "Armv8.7 PMU Extensions"sv;
    if (feature == CPUFeature::RPRES)
        return "Increased precision of Reciprocal Estimate and Reciprocal Square Root Estimate"sv;
    if (feature == CPUFeature::RME)
        return "Realm Management Extension"sv;
    if (feature == CPUFeature::SME_FA64)
        return "Additional instructions for the SME Extension"sv;
    if (feature == CPUFeature::SME_F64F64)
        return "Additional instructions for the SME Extension"sv;
    if (feature == CPUFeature::SME_I16I64)
        return "Additional instructions for the SME Extension"sv;
    if (feature == CPUFeature::EBF16)
        return "Additional instructions for the SME Extension"sv;
    if (feature == CPUFeature::SPEv1p2)
        return "Armv8.7 SPE"sv;
    if (feature == CPUFeature::WFxT)
        return "WFE and WFI instructions with timeout"sv;
    if (feature == CPUFeature::XS)
        return "XS attribute"sv;
    if (feature == CPUFeature::BRBE)
        return "Branch Record Buffer Extensions"sv;

    // Features introduced prior to 2020
    if (feature == CPUFeature::AdvSIMD)
        return "Advanced SIMD Extension"sv;
    if (feature == CPUFeature::AES)
        return "Advanced SIMD AES instructions"sv;
    if (feature == CPUFeature::PMULL)
        return "Advanced SIMD PMULL instructions"sv; // ARMv8.0-AES is split into AES and PMULL
    if (feature == CPUFeature::CP15SDISABLE2)
        return "CP15DISABLE2"sv;
    if (feature == CPUFeature::CSV2)
        return "Cache Speculation Variant 2"sv;
    if (feature == CPUFeature::CSV2_1p1)
        return "Cache Speculation Variant 2 version 1.1"sv;
    if (feature == CPUFeature::CSV2_1p2)
        return "Cache Speculation Variant 2 version 1.2"sv;
    if (feature == CPUFeature::CSV2_2)
        return "Cache Speculation Variant 2 version 2"sv; // NOTE: name mistake in source!
    if (feature == CPUFeature::CSV3)
        return "Cache Speculation Variant 3"sv;
    if (feature == CPUFeature::DGH)
        return "Data Gathering Hint"sv;
    if (feature == CPUFeature::DoubleLock)
        return "Double Lock"sv;
    if (feature == CPUFeature::ETS)
        return "Enhanced Translation Synchronization"sv;
    if (feature == CPUFeature::FP)
        return "Floating point extension"sv;
    if (feature == CPUFeature::IVIPT)
        return "The IVIPT Extension"sv;
    if (feature == CPUFeature::PCSRv8)
        return "PC Sample-base Profiling extension (not EL3 and EL2)"sv;
    if (feature == CPUFeature::SPECRES)
        return "Speculation restriction instructions"sv;
    if (feature == CPUFeature::RAS)
        return "Reliability, Availability, and Serviceability (RAS) Extension"sv;
    if (feature == CPUFeature::SB)
        return "Speculation barrier"sv;
    if (feature == CPUFeature::SHA1)
        return "Advanced SIMD SHA1 instructions"sv;
    if (feature == CPUFeature::SHA256)
        return "Advanced SIMD SHA256 instructions"sv; // ARMv8.2-SHA is split into SHA-256, SHA-512 and SHA-3
    if (feature == CPUFeature::SSBS)
        return "Speculative Store Bypass Safe Instruction"sv; // ARMv8.0-SSBS is split into SSBS and SSBS2
    if (feature == CPUFeature::SSBS2)
        return "MRS and MSR instructions for SSBS"sv; // ARMv8.0-SSBS is split into SSBS and SSBS2
    if (feature == CPUFeature::CRC32)
        return "CRC32 instructions"sv;
    if (feature == CPUFeature::nTLBPA)
        return "No intermediate caching by output address in TLB"sv;
    if (feature == CPUFeature::Debugv8p1)
        return "Debug with VHE"sv;
    if (feature == CPUFeature::HPDS)
        return "Hierarchical permission disables in translation tables"sv;
    if (feature == CPUFeature::LOR)
        return "Limited ordering regions"sv;
    if (feature == CPUFeature::LSE)
        return "Large System Extensions"sv;
    if (feature == CPUFeature::PAN)
        return "Privileged access-never"sv;
    if (feature == CPUFeature::PMUv3p1)
        return "PMU extensions version 3.1"sv;
    if (feature == CPUFeature::RDM)
        return "Rounding double multiply accumulate"sv;
    if (feature == CPUFeature::HAFDBS)
        return "Hardware updates to access flag and dirty state in translation tables"sv;
    if (feature == CPUFeature::VHE)
        return "Virtualization Host Extensions"sv;
    if (feature == CPUFeature::VMID16)
        return "16-bit VMID"sv;
    if (feature == CPUFeature::AA32BF16)
        return "AArch32 BFloat16 instructions"sv;
    if (feature == CPUFeature::AA32HPD)
        return "AArch32 Hierarchical permission disables"sv;
    if (feature == CPUFeature::AA32I8MM)
        return "AArch32 Int8 Matrix Multiplication"sv;
    if (feature == CPUFeature::PAN2)
        return "AT S1E1R and AT S1E1W instruction variants for PAN"sv;
    if (feature == CPUFeature::BF16)
        return "AArch64 BFloat16 instructions"sv; // NOTE: typo in source!
    if (feature == CPUFeature::DPB2)
        return "DC CVADP instruction"sv;
    if (feature == CPUFeature::DPB)
        return "DC CVAP instruction"sv;
    if (feature == CPUFeature::Debugv8p2)
        return "ARMv8.2 Debug"sv;
    if (feature == CPUFeature::DotProd)
        return "Advanced SIMD Int8 dot product instructions"sv;
    if (feature == CPUFeature::EVT)
        return "Enhanced Virtualization Traps"sv;
    if (feature == CPUFeature::F32MM)
        return "SVE single-precision floating-point matrix multiply instruction"sv;
    if (feature == CPUFeature::F64MM)
        return "SVE double-precision floating-point matrix multiply instruction"sv;
    if (feature == CPUFeature::FHM)
        return "Half-precision floating-point FMLAL instructions"sv;
    if (feature == CPUFeature::FP16)
        return "Half-precision floating-point data processing"sv;
    if (feature == CPUFeature::I8MM)
        return "Int8 Matrix Multiplication"sv;
    if (feature == CPUFeature::IESB)
        return "Implicit Error synchronization event"sv;
    if (feature == CPUFeature::LPA)
        return "Large PA and IPA support"sv;
    if (feature == CPUFeature::LSMAOC)
        return "Load/Store instruction multiple atomicity and ordering controls"sv;
    if (feature == CPUFeature::LVA)
        return "Large VA support"sv;
    if (feature == CPUFeature::MPAM)
        return "Memory Partitioning and Monitoring"sv;
    if (feature == CPUFeature::PCSRv8p2)
        return "PC Sample-based profiling version 8.2"sv;
    if (feature == CPUFeature::SHA3)
        return "Advanced SIMD EOR3, RAX1, XAR, and BCAX instructions"sv; // ARMv8.2-SHA is split into SHA-256, SHA-512 and SHA-3
    if (feature == CPUFeature::SHA512)
        return "Advanced SIMD SHA512 instructions"sv; // ARMv8.2-SHA is split into SHA-256, SHA-512 and SHA-3
    if (feature == CPUFeature::SM3)
        return "Advanced SIMD SM3 instructions"sv; // Split into SM3 and SM4
    if (feature == CPUFeature::SM4)
        return "Advanced SIMD SM4 instructions"sv; // Split into SM3 and SM4
    if (feature == CPUFeature::SPE)
        return "Statistical Profiling Extension"sv;
    if (feature == CPUFeature::SVE)
        return "Scalable Vector Extension"sv;
    if (feature == CPUFeature::TTCNP)
        return "Common not private translations"sv;
    if (feature == CPUFeature::HPDS2)
        return "Heirarchical permission disables in translation tables 2"sv;
    if (feature == CPUFeature::XNX)
        return "Execute-never control distinction by Exception level at stage 2"sv;
    if (feature == CPUFeature::UAO)
        return "Unprivileged Access Override control"sv;
    if (feature == CPUFeature::VPIPT)
        return "VMID-aware PIPT instruction cache"sv;
    if (feature == CPUFeature::CCIDX)
        return "Extended cache index"sv;
    if (feature == CPUFeature::FCMA)
        return "Floating-point FCMLA and FCADD instructions"sv;
    if (feature == CPUFeature::DoPD)
        return "Debug over Powerdown"sv;
    if (feature == CPUFeature::EPAC)
        return "Enhanced Pointer authentication"sv;
    if (feature == CPUFeature::FPAC)
        return "Faulting on pointer authentication instructions"sv;
    if (feature == CPUFeature::FPACCOMBINE)
        return "Faulting on combined pointer authentication instructions"sv;
    if (feature == CPUFeature::JSCVT)
        return "JavaScript FJCVTS conversion instruction"sv;
    if (feature == CPUFeature::LRCPC)
        return "Load-acquire RCpc instructions"sv;
    if (feature == CPUFeature::NV)
        return "Nested virtualization"sv;
    if (feature == CPUFeature::PACQARMA5)
        return "Pointer authentication - QARMA5 algorithm"sv;
    if (feature == CPUFeature::PACIMP)
        return "Pointer authentication - IMPLEMENTATION DEFINED algorithm"sv;
    if (feature == CPUFeature::PAuth)
        return "Pointer authentication"sv;
    if (feature == CPUFeature::PAuth2)
        return "Enhancements to pointer authentication"sv;
    if (feature == CPUFeature::SPEv1p1)
        return "Statistical Profiling Extensions version 1.1"sv;
    if (feature == CPUFeature::AMUv1)
        return "Activity Monitors Extension"sv;
    if (feature == CPUFeature::CNTSC)
        return "Generic Counter Scaling"sv;
    if (feature == CPUFeature::Debugv8p4)
        return "Debug relaxations and extensions version 8.4"sv;
    if (feature == CPUFeature::DoubleFault)
        return "Double Fault Extension"sv;
    if (feature == CPUFeature::DIT)
        return "Data Independent Timing instructions"sv;
    if (feature == CPUFeature::FlagM)
        return "Condition flag manipulation"sv;
    if (feature == CPUFeature::IDST)
        return "ID space trap handling"sv;
    if (feature == CPUFeature::LRCPC2)
        return "Load-acquire RCpc instructions version 2"sv;
    if (feature == CPUFeature::LSE2)
        return "Large System Extensions version 2"sv;
    if (feature == CPUFeature::NV2)
        return "Enhanced support for nested virtualization"sv;
    if (feature == CPUFeature::PMUv3p4)
        return "PMU extension version 3.4"sv;
    if (feature == CPUFeature::RASv1p1)
        return "Reliability, Availability, and Serviceability (RAS) Extension version 1.1"sv;
    if (feature == CPUFeature::S2FWB)
        return "Stage 2 forced write-back"sv;
    if (feature == CPUFeature::SEL2)
        return "Secure EL2"sv;
    if (feature == CPUFeature::TLBIOS)
        return "TLB invalidate outer-shared instructions"sv; // Split into TLBIOS and TLBIRANGE
    if (feature == CPUFeature::TLBIRANGE)
        return "TLB range invalidate range instructions"sv; // Split into TLBIOS and TLBIRANGE
    if (feature == CPUFeature::TRF)
        return "Self hosted Trace Extensions"sv;
    if (feature == CPUFeature::TTL)
        return "Translation Table Level"sv;
    if (feature == CPUFeature::BBM)
        return "Translation table break before make levels"sv;
    if (feature == CPUFeature::TTST)
        return "Small translation tables"sv;
    if (feature == CPUFeature::BTI)
        return "Branch target identification"sv;
    if (feature == CPUFeature::FlagM2)
        return "Condition flag manipulation version 2"sv;
    if (feature == CPUFeature::ExS)
        return "Disabling context synchronizing exception entry and exit"sv;
    if (feature == CPUFeature::E0PD)
        return "Preventing EL0 access to halves of address maps"sv;
    if (feature == CPUFeature::FRINTTS)
        return "FRINT32Z, FRINT32X, FRINT64Z, and FRINT64X instructions"sv;
    if (feature == CPUFeature::GTG)
        return "Guest translation granule size"sv;
    if (feature == CPUFeature::MTE)
        return "Instruction-only Memory Tagging Extension"sv;
    if (feature == CPUFeature::MTE2)
        return "Full Memory Tagging Extension"sv;
    if (feature == CPUFeature::PMUv3p5)
        return "PMU Extension version 3.5"sv;
    if (feature == CPUFeature::RNG)
        return "Random number generator"sv;
    if (feature == CPUFeature::AMUv1p1)
        return "Activity Monitors Extension version 1.1"sv;
    if (feature == CPUFeature::ECV)
        return "Enhanced counter virtualization"sv;
    if (feature == CPUFeature::FGT)
        return "Fine Grain Traps"sv;
    if (feature == CPUFeature::MPAMv0p1)
        return "Memory Partitioning and Monitoring version 0.1"sv;
    if (feature == CPUFeature::MPAMv1p1)
        return "Memory Partitioning and Monitoring version 1.1"sv;
    if (feature == CPUFeature::MTPMU)
        return "Multi-threaded PMU Extensions"sv;
    if (feature == CPUFeature::TWED)
        return "Delayed trapping of WFE"sv;
    if (feature == CPUFeature::ETMv4)
        return "Embedded Trace Macrocell version4"sv;
    if (feature == CPUFeature::ETMv4p1)
        return "Embedded Trace Macrocell version 4.1"sv;
    if (feature == CPUFeature::ETMv4p2)
        return "Embedded Trace Macrocell version 4.2"sv;
    if (feature == CPUFeature::ETMv4p3)
        return "Embedded Trace Macrocell version 4.3"sv;
    if (feature == CPUFeature::ETMv4p4)
        return "Embedded Trace Macrocell version 4.3"sv;
    if (feature == CPUFeature::ETMv4p5)
        return "Embedded Trace Macrocell version 4.4"sv;
    if (feature == CPUFeature::ETMv4p6)
        return "Embedded Trace Macrocell version 4.5"sv;
    if (feature == CPUFeature::GICv3)
        return "Generic Interrupt Controller version 3"sv;
    if (feature == CPUFeature::GICv3p1)
        return "Generic Interrupt Controller version 3.1"sv;
    if (feature == CPUFeature::GICv3_LEGACY)
        return "Support for GICv2 legacy operation"sv; // Note: missing in source
    if (feature == CPUFeature::GICv3_TDIR)
        return "Trapping Non-secure EL1 writes to ICV_DIR"sv;
    if (feature == CPUFeature::GICv4)
        return "Generic Interrupt Controller version 4"sv;
    if (feature == CPUFeature::GICv4p1)
        return "Generic Interrupt Controller version 4.1"sv;
    if (feature == CPUFeature::PMUv3)
        return "PMU extension version 3"sv;
    if (feature == CPUFeature::ETE)
        return "Embedded Trace Extension"sv;
    if (feature == CPUFeature::ETEv1p1)
        return "Embedded Trace Extension, version 1.1"sv;
    if (feature == CPUFeature::SVE2)
        return "SVE version 2"sv;
    if (feature == CPUFeature::SVE_AES)
        return "SVE AES instructions"sv;
    if (feature == CPUFeature::SVE_PMULL128)
        return "SVE PMULL instructions"sv; // SVE2-AES is split into AES and PMULL support
    if (feature == CPUFeature::SVE_BitPerm)
        return "SVE Bit Permute"sv;
    if (feature == CPUFeature::SVE_SHA3)
        return "SVE SHA-3 instructions"sv;
    if (feature == CPUFeature::SVE_SM4)
        return "SVE SM4 instructions"sv;
    if (feature == CPUFeature::TME)
        return "Transactional Memory Extension"sv;
    if (feature == CPUFeature::TRBE)
        return "Trace Buffer Extension"sv;
    if (feature == CPUFeature::SME)
        return "Scalable Matrix Extension"sv;

    VERIFY_NOT_REACHED();
}

NonnullOwnPtr<KString> build_cpu_feature_names(CPUFeature::Type const& features)
{
    StringBuilder builder;
    bool first = true;
    for (auto feature = CPUFeature::Type(1u); feature != CPUFeature::__End; feature <<= 1u) {
        if (features.has_flag(feature)) {
            if (first)
                first = false;
            else
                MUST(builder.try_append(' '));
            auto name = cpu_feature_to_name(feature);
            MUST(builder.try_append(name));
        }
    }
    return KString::must_create(builder.string_view());
}

u8 detect_physical_address_bit_width()
{
    auto memory_model_feature_register_0 = Aarch64::ID_AA64MMFR0_EL1::read();

    switch (memory_model_feature_register_0.PARange) {
    case 0b0000:
        return 32; // 4GB
    case 0b0001:
        return 36; // 64GB
    case 0b0010:
        return 40; // 1TB
    case 0b0011:
        return 42; // 4TB
    case 0b0100:
        return 44; // 16TB
    case 0b0101:
        return 48; // 256TB
    case 0b0110:
        return 52; // 4PB (applies for FEAT_LPA or FEAT_LPA2)
    case 0b0111:
        return 56; // 64PB (applies for FEAT_D128)
    default:
        VERIFY_NOT_REACHED();
    }
}

u8 detect_virtual_address_bit_width()
{
    auto memory_model_feature_register_2 = Aarch64::ID_AA64MMFR2_EL1::read();

    switch (memory_model_feature_register_2.VARange) {
    case 0b0000:
        return 48; // 256TB
    case 0b0001:
        return 52; // 4PB (only for 64KB translation granule)
    case 0b0010:
        return 56; // 64PB (applies for FEAT_D128)
    default:
        VERIFY_NOT_REACHED();
    }
}

}
