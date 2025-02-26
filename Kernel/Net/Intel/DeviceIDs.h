/*
 * Copyright (c) 2025, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::E1000 {

namespace Series8254x {
// https://www.intel.com/content/dam/doc/manual/pci-pci-x-family-gbe-controllers-software-dev-manual.pdf
// Section 5.2
constexpr u16 _82540EM_A = 0x100E;
constexpr u16 _82545EM_A_COPPER = 0x100F;
constexpr u16 _82546EB_A1_COPPER = 0x1010;
constexpr u16 _82545EM_A_FIBER = 0x1011;
constexpr u16 _82546EB_A1_FIBER = 0x1012;
constexpr u16 _82541EI_A0_B0 = 0x1013;
constexpr u16 _82540EM_A_LOM = 0x1015;
constexpr u16 _82540EP_A_LOM = 0x1016;
constexpr u16 _82540EP_A = 0x1017;
constexpr u16 _82541EI_B0_MOBILE = 0x1018;
constexpr u16 _82547EI_A01_B0_82547GI_B0 = 0x1019;
constexpr u16 _82547EI_B0_MOBILE = 0x101A;
constexpr u16 _82546EB_A1_QUAD_COPPER = 0x101D;
constexpr u16 _82545GM_B_COPPER = 0x1026;
constexpr u16 _82545GM_B_FIBER = 0x1027;
constexpr u16 _82545GM_B_SERDES = 0x1028;
constexpr u16 _82541GI_B1_82541PI_C0 = 0x1076;
constexpr u16 _82541GI_B1_MOBILE = 0x1077;
constexpr u16 _82541ER_C0 = 0x1078;
constexpr u16 _82546GB_B0_COPPER = 0x1079;
constexpr u16 _82546GB_B0_FIBER = 0x107A;
constexpr u16 _82546GB_B0_SERDES = 0x107B;
constexpr u16 _82544EI_A4 = 0x1107;
constexpr u16 _82544GC_A4 = 0x1112;
}

namespace Series8257x {

// 63[12]xESB
// 8256[34]EB
// 8257[123]:
// FIXME: Intel link
// https://ftp.mizar.org/packages/e1000/8257x%20Developer%20Manual/Revision%201.8/OpenSDM_8257x-18.pdf

}

}
