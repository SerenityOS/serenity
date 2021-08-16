/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include "libTestDowncall.h"
#ifdef __clang__
#pragma clang optimize off
#elif defined __GNUC__
#pragma GCC optimize ("O0")
#elif defined _MSC_BUILD
#pragma optimize( "", off )
#endif

EXPORT void f0_V__(void) { }
EXPORT void f0_V_I_(int p0) { }
EXPORT void f0_V_F_(float p0) { }
EXPORT void f0_V_D_(double p0) { }
EXPORT void f0_V_P_(void* p0) { }
EXPORT void f0_V_S_I(struct S_I p0) { }
EXPORT void f0_V_S_F(struct S_F p0) { }
EXPORT void f0_V_S_D(struct S_D p0) { }
EXPORT void f0_V_S_P(struct S_P p0) { }
EXPORT void f0_V_S_II(struct S_II p0) { }
EXPORT void f0_V_S_IF(struct S_IF p0) { }
EXPORT void f0_V_S_ID(struct S_ID p0) { }
EXPORT void f0_V_S_IP(struct S_IP p0) { }
EXPORT void f0_V_S_FI(struct S_FI p0) { }
EXPORT void f0_V_S_FF(struct S_FF p0) { }
EXPORT void f0_V_S_FD(struct S_FD p0) { }
EXPORT void f0_V_S_FP(struct S_FP p0) { }
EXPORT void f0_V_S_DI(struct S_DI p0) { }
EXPORT void f0_V_S_DF(struct S_DF p0) { }
EXPORT void f0_V_S_DD(struct S_DD p0) { }
EXPORT void f0_V_S_DP(struct S_DP p0) { }
EXPORT void f0_V_S_PI(struct S_PI p0) { }
EXPORT void f0_V_S_PF(struct S_PF p0) { }
EXPORT void f0_V_S_PD(struct S_PD p0) { }
EXPORT void f0_V_S_PP(struct S_PP p0) { }
EXPORT void f0_V_S_III(struct S_III p0) { }
EXPORT void f0_V_S_IIF(struct S_IIF p0) { }
EXPORT void f0_V_S_IID(struct S_IID p0) { }
EXPORT void f0_V_S_IIP(struct S_IIP p0) { }
EXPORT void f0_V_S_IFI(struct S_IFI p0) { }
EXPORT void f0_V_S_IFF(struct S_IFF p0) { }
EXPORT void f0_V_S_IFD(struct S_IFD p0) { }
EXPORT void f0_V_S_IFP(struct S_IFP p0) { }
EXPORT void f0_V_S_IDI(struct S_IDI p0) { }
EXPORT void f0_V_S_IDF(struct S_IDF p0) { }
EXPORT void f0_V_S_IDD(struct S_IDD p0) { }
EXPORT void f0_V_S_IDP(struct S_IDP p0) { }
EXPORT void f0_V_S_IPI(struct S_IPI p0) { }
EXPORT void f0_V_S_IPF(struct S_IPF p0) { }
EXPORT void f0_V_S_IPD(struct S_IPD p0) { }
EXPORT void f0_V_S_IPP(struct S_IPP p0) { }
EXPORT void f0_V_S_FII(struct S_FII p0) { }
EXPORT void f0_V_S_FIF(struct S_FIF p0) { }
EXPORT void f0_V_S_FID(struct S_FID p0) { }
EXPORT void f0_V_S_FIP(struct S_FIP p0) { }
EXPORT void f0_V_S_FFI(struct S_FFI p0) { }
EXPORT void f0_V_S_FFF(struct S_FFF p0) { }
EXPORT void f0_V_S_FFD(struct S_FFD p0) { }
EXPORT void f0_V_S_FFP(struct S_FFP p0) { }
EXPORT void f0_V_S_FDI(struct S_FDI p0) { }
EXPORT void f0_V_S_FDF(struct S_FDF p0) { }
EXPORT void f0_V_S_FDD(struct S_FDD p0) { }
EXPORT void f0_V_S_FDP(struct S_FDP p0) { }
EXPORT void f0_V_S_FPI(struct S_FPI p0) { }
EXPORT void f0_V_S_FPF(struct S_FPF p0) { }
EXPORT void f0_V_S_FPD(struct S_FPD p0) { }
EXPORT void f0_V_S_FPP(struct S_FPP p0) { }
EXPORT void f0_V_S_DII(struct S_DII p0) { }
EXPORT void f0_V_S_DIF(struct S_DIF p0) { }
EXPORT void f0_V_S_DID(struct S_DID p0) { }
EXPORT void f0_V_S_DIP(struct S_DIP p0) { }
EXPORT void f0_V_S_DFI(struct S_DFI p0) { }
EXPORT void f0_V_S_DFF(struct S_DFF p0) { }
EXPORT void f0_V_S_DFD(struct S_DFD p0) { }
EXPORT void f0_V_S_DFP(struct S_DFP p0) { }
EXPORT void f0_V_S_DDI(struct S_DDI p0) { }
EXPORT void f0_V_S_DDF(struct S_DDF p0) { }
EXPORT void f0_V_S_DDD(struct S_DDD p0) { }
EXPORT void f0_V_S_DDP(struct S_DDP p0) { }
EXPORT void f0_V_S_DPI(struct S_DPI p0) { }
EXPORT void f0_V_S_DPF(struct S_DPF p0) { }
EXPORT void f0_V_S_DPD(struct S_DPD p0) { }
EXPORT void f0_V_S_DPP(struct S_DPP p0) { }
EXPORT void f0_V_S_PII(struct S_PII p0) { }
EXPORT void f0_V_S_PIF(struct S_PIF p0) { }
EXPORT void f0_V_S_PID(struct S_PID p0) { }
EXPORT void f0_V_S_PIP(struct S_PIP p0) { }
EXPORT void f0_V_S_PFI(struct S_PFI p0) { }
EXPORT void f0_V_S_PFF(struct S_PFF p0) { }
EXPORT void f0_V_S_PFD(struct S_PFD p0) { }
EXPORT void f0_V_S_PFP(struct S_PFP p0) { }
EXPORT void f0_V_S_PDI(struct S_PDI p0) { }
EXPORT void f0_V_S_PDF(struct S_PDF p0) { }
EXPORT void f0_V_S_PDD(struct S_PDD p0) { }
EXPORT void f0_V_S_PDP(struct S_PDP p0) { }
EXPORT void f0_V_S_PPI(struct S_PPI p0) { }
EXPORT void f0_V_S_PPF(struct S_PPF p0) { }
EXPORT void f0_V_S_PPD(struct S_PPD p0) { }
EXPORT void f0_V_S_PPP(struct S_PPP p0) { }
EXPORT void f0_V_II_(int p0, int p1) { }
EXPORT void f0_V_IF_(int p0, float p1) { }
EXPORT void f0_V_ID_(int p0, double p1) { }
EXPORT void f0_V_IP_(int p0, void* p1) { }
EXPORT void f0_V_IS_I(int p0, struct S_I p1) { }
EXPORT void f0_V_IS_F(int p0, struct S_F p1) { }
EXPORT void f0_V_IS_D(int p0, struct S_D p1) { }
EXPORT void f0_V_IS_P(int p0, struct S_P p1) { }
EXPORT void f0_V_IS_II(int p0, struct S_II p1) { }
EXPORT void f0_V_IS_IF(int p0, struct S_IF p1) { }
EXPORT void f0_V_IS_ID(int p0, struct S_ID p1) { }
EXPORT void f0_V_IS_IP(int p0, struct S_IP p1) { }
EXPORT void f0_V_IS_FI(int p0, struct S_FI p1) { }
EXPORT void f0_V_IS_FF(int p0, struct S_FF p1) { }
EXPORT void f0_V_IS_FD(int p0, struct S_FD p1) { }
EXPORT void f0_V_IS_FP(int p0, struct S_FP p1) { }
EXPORT void f0_V_IS_DI(int p0, struct S_DI p1) { }
EXPORT void f0_V_IS_DF(int p0, struct S_DF p1) { }
EXPORT void f0_V_IS_DD(int p0, struct S_DD p1) { }
EXPORT void f0_V_IS_DP(int p0, struct S_DP p1) { }
EXPORT void f0_V_IS_PI(int p0, struct S_PI p1) { }
EXPORT void f0_V_IS_PF(int p0, struct S_PF p1) { }
EXPORT void f0_V_IS_PD(int p0, struct S_PD p1) { }
EXPORT void f0_V_IS_PP(int p0, struct S_PP p1) { }
EXPORT void f0_V_IS_III(int p0, struct S_III p1) { }
EXPORT void f0_V_IS_IIF(int p0, struct S_IIF p1) { }
EXPORT void f0_V_IS_IID(int p0, struct S_IID p1) { }
EXPORT void f0_V_IS_IIP(int p0, struct S_IIP p1) { }
EXPORT void f0_V_IS_IFI(int p0, struct S_IFI p1) { }
EXPORT void f0_V_IS_IFF(int p0, struct S_IFF p1) { }
EXPORT void f0_V_IS_IFD(int p0, struct S_IFD p1) { }
EXPORT void f0_V_IS_IFP(int p0, struct S_IFP p1) { }
EXPORT void f0_V_IS_IDI(int p0, struct S_IDI p1) { }
EXPORT void f0_V_IS_IDF(int p0, struct S_IDF p1) { }
EXPORT void f0_V_IS_IDD(int p0, struct S_IDD p1) { }
EXPORT void f0_V_IS_IDP(int p0, struct S_IDP p1) { }
EXPORT void f0_V_IS_IPI(int p0, struct S_IPI p1) { }
EXPORT void f0_V_IS_IPF(int p0, struct S_IPF p1) { }
EXPORT void f0_V_IS_IPD(int p0, struct S_IPD p1) { }
EXPORT void f0_V_IS_IPP(int p0, struct S_IPP p1) { }
EXPORT void f0_V_IS_FII(int p0, struct S_FII p1) { }
EXPORT void f0_V_IS_FIF(int p0, struct S_FIF p1) { }
EXPORT void f0_V_IS_FID(int p0, struct S_FID p1) { }
EXPORT void f0_V_IS_FIP(int p0, struct S_FIP p1) { }
EXPORT void f0_V_IS_FFI(int p0, struct S_FFI p1) { }
EXPORT void f0_V_IS_FFF(int p0, struct S_FFF p1) { }
EXPORT void f0_V_IS_FFD(int p0, struct S_FFD p1) { }
EXPORT void f0_V_IS_FFP(int p0, struct S_FFP p1) { }
EXPORT void f0_V_IS_FDI(int p0, struct S_FDI p1) { }
EXPORT void f0_V_IS_FDF(int p0, struct S_FDF p1) { }
EXPORT void f0_V_IS_FDD(int p0, struct S_FDD p1) { }
EXPORT void f0_V_IS_FDP(int p0, struct S_FDP p1) { }
EXPORT void f0_V_IS_FPI(int p0, struct S_FPI p1) { }
EXPORT void f0_V_IS_FPF(int p0, struct S_FPF p1) { }
EXPORT void f0_V_IS_FPD(int p0, struct S_FPD p1) { }
EXPORT void f0_V_IS_FPP(int p0, struct S_FPP p1) { }
EXPORT void f0_V_IS_DII(int p0, struct S_DII p1) { }
EXPORT void f0_V_IS_DIF(int p0, struct S_DIF p1) { }
EXPORT void f0_V_IS_DID(int p0, struct S_DID p1) { }
EXPORT void f0_V_IS_DIP(int p0, struct S_DIP p1) { }
EXPORT void f0_V_IS_DFI(int p0, struct S_DFI p1) { }
EXPORT void f0_V_IS_DFF(int p0, struct S_DFF p1) { }
EXPORT void f0_V_IS_DFD(int p0, struct S_DFD p1) { }
EXPORT void f0_V_IS_DFP(int p0, struct S_DFP p1) { }
EXPORT void f0_V_IS_DDI(int p0, struct S_DDI p1) { }
EXPORT void f0_V_IS_DDF(int p0, struct S_DDF p1) { }
EXPORT void f0_V_IS_DDD(int p0, struct S_DDD p1) { }
EXPORT void f0_V_IS_DDP(int p0, struct S_DDP p1) { }
EXPORT void f0_V_IS_DPI(int p0, struct S_DPI p1) { }
EXPORT void f0_V_IS_DPF(int p0, struct S_DPF p1) { }
EXPORT void f0_V_IS_DPD(int p0, struct S_DPD p1) { }
EXPORT void f0_V_IS_DPP(int p0, struct S_DPP p1) { }
EXPORT void f0_V_IS_PII(int p0, struct S_PII p1) { }
EXPORT void f0_V_IS_PIF(int p0, struct S_PIF p1) { }
EXPORT void f0_V_IS_PID(int p0, struct S_PID p1) { }
EXPORT void f0_V_IS_PIP(int p0, struct S_PIP p1) { }
EXPORT void f0_V_IS_PFI(int p0, struct S_PFI p1) { }
EXPORT void f0_V_IS_PFF(int p0, struct S_PFF p1) { }
EXPORT void f0_V_IS_PFD(int p0, struct S_PFD p1) { }
EXPORT void f0_V_IS_PFP(int p0, struct S_PFP p1) { }
EXPORT void f0_V_IS_PDI(int p0, struct S_PDI p1) { }
EXPORT void f0_V_IS_PDF(int p0, struct S_PDF p1) { }
EXPORT void f0_V_IS_PDD(int p0, struct S_PDD p1) { }
EXPORT void f0_V_IS_PDP(int p0, struct S_PDP p1) { }
EXPORT void f0_V_IS_PPI(int p0, struct S_PPI p1) { }
EXPORT void f0_V_IS_PPF(int p0, struct S_PPF p1) { }
EXPORT void f0_V_IS_PPD(int p0, struct S_PPD p1) { }
EXPORT void f0_V_IS_PPP(int p0, struct S_PPP p1) { }
EXPORT void f0_V_FI_(float p0, int p1) { }
EXPORT void f0_V_FF_(float p0, float p1) { }
EXPORT void f0_V_FD_(float p0, double p1) { }
EXPORT void f0_V_FP_(float p0, void* p1) { }
EXPORT void f0_V_FS_I(float p0, struct S_I p1) { }
EXPORT void f0_V_FS_F(float p0, struct S_F p1) { }
EXPORT void f0_V_FS_D(float p0, struct S_D p1) { }
EXPORT void f0_V_FS_P(float p0, struct S_P p1) { }
EXPORT void f0_V_FS_II(float p0, struct S_II p1) { }
EXPORT void f0_V_FS_IF(float p0, struct S_IF p1) { }
EXPORT void f0_V_FS_ID(float p0, struct S_ID p1) { }
EXPORT void f0_V_FS_IP(float p0, struct S_IP p1) { }
EXPORT void f0_V_FS_FI(float p0, struct S_FI p1) { }
EXPORT void f0_V_FS_FF(float p0, struct S_FF p1) { }
EXPORT void f0_V_FS_FD(float p0, struct S_FD p1) { }
EXPORT void f0_V_FS_FP(float p0, struct S_FP p1) { }
EXPORT void f0_V_FS_DI(float p0, struct S_DI p1) { }
EXPORT void f0_V_FS_DF(float p0, struct S_DF p1) { }
EXPORT void f0_V_FS_DD(float p0, struct S_DD p1) { }
EXPORT void f0_V_FS_DP(float p0, struct S_DP p1) { }
EXPORT void f0_V_FS_PI(float p0, struct S_PI p1) { }
EXPORT void f0_V_FS_PF(float p0, struct S_PF p1) { }
EXPORT void f0_V_FS_PD(float p0, struct S_PD p1) { }
EXPORT void f0_V_FS_PP(float p0, struct S_PP p1) { }
EXPORT void f0_V_FS_III(float p0, struct S_III p1) { }
EXPORT void f0_V_FS_IIF(float p0, struct S_IIF p1) { }
EXPORT void f0_V_FS_IID(float p0, struct S_IID p1) { }
EXPORT void f0_V_FS_IIP(float p0, struct S_IIP p1) { }
EXPORT void f0_V_FS_IFI(float p0, struct S_IFI p1) { }
EXPORT void f0_V_FS_IFF(float p0, struct S_IFF p1) { }
EXPORT void f0_V_FS_IFD(float p0, struct S_IFD p1) { }
EXPORT void f0_V_FS_IFP(float p0, struct S_IFP p1) { }
EXPORT void f0_V_FS_IDI(float p0, struct S_IDI p1) { }
EXPORT void f0_V_FS_IDF(float p0, struct S_IDF p1) { }
EXPORT void f0_V_FS_IDD(float p0, struct S_IDD p1) { }
EXPORT void f0_V_FS_IDP(float p0, struct S_IDP p1) { }
EXPORT void f0_V_FS_IPI(float p0, struct S_IPI p1) { }
EXPORT void f0_V_FS_IPF(float p0, struct S_IPF p1) { }
EXPORT void f0_V_FS_IPD(float p0, struct S_IPD p1) { }
EXPORT void f0_V_FS_IPP(float p0, struct S_IPP p1) { }
EXPORT void f0_V_FS_FII(float p0, struct S_FII p1) { }
EXPORT void f0_V_FS_FIF(float p0, struct S_FIF p1) { }
EXPORT void f0_V_FS_FID(float p0, struct S_FID p1) { }
EXPORT void f0_V_FS_FIP(float p0, struct S_FIP p1) { }
EXPORT void f0_V_FS_FFI(float p0, struct S_FFI p1) { }
EXPORT void f0_V_FS_FFF(float p0, struct S_FFF p1) { }
EXPORT void f0_V_FS_FFD(float p0, struct S_FFD p1) { }
EXPORT void f0_V_FS_FFP(float p0, struct S_FFP p1) { }
EXPORT void f0_V_FS_FDI(float p0, struct S_FDI p1) { }
EXPORT void f0_V_FS_FDF(float p0, struct S_FDF p1) { }
EXPORT void f0_V_FS_FDD(float p0, struct S_FDD p1) { }
EXPORT void f0_V_FS_FDP(float p0, struct S_FDP p1) { }
EXPORT void f0_V_FS_FPI(float p0, struct S_FPI p1) { }
EXPORT void f0_V_FS_FPF(float p0, struct S_FPF p1) { }
EXPORT void f0_V_FS_FPD(float p0, struct S_FPD p1) { }
EXPORT void f0_V_FS_FPP(float p0, struct S_FPP p1) { }
EXPORT void f0_V_FS_DII(float p0, struct S_DII p1) { }
EXPORT void f0_V_FS_DIF(float p0, struct S_DIF p1) { }
EXPORT void f0_V_FS_DID(float p0, struct S_DID p1) { }
EXPORT void f0_V_FS_DIP(float p0, struct S_DIP p1) { }
EXPORT void f0_V_FS_DFI(float p0, struct S_DFI p1) { }
EXPORT void f0_V_FS_DFF(float p0, struct S_DFF p1) { }
EXPORT void f0_V_FS_DFD(float p0, struct S_DFD p1) { }
EXPORT void f0_V_FS_DFP(float p0, struct S_DFP p1) { }
EXPORT void f0_V_FS_DDI(float p0, struct S_DDI p1) { }
EXPORT void f0_V_FS_DDF(float p0, struct S_DDF p1) { }
EXPORT void f0_V_FS_DDD(float p0, struct S_DDD p1) { }
EXPORT void f0_V_FS_DDP(float p0, struct S_DDP p1) { }
EXPORT void f0_V_FS_DPI(float p0, struct S_DPI p1) { }
EXPORT void f0_V_FS_DPF(float p0, struct S_DPF p1) { }
EXPORT void f0_V_FS_DPD(float p0, struct S_DPD p1) { }
EXPORT void f0_V_FS_DPP(float p0, struct S_DPP p1) { }
EXPORT void f0_V_FS_PII(float p0, struct S_PII p1) { }
EXPORT void f0_V_FS_PIF(float p0, struct S_PIF p1) { }
EXPORT void f0_V_FS_PID(float p0, struct S_PID p1) { }
EXPORT void f0_V_FS_PIP(float p0, struct S_PIP p1) { }
EXPORT void f0_V_FS_PFI(float p0, struct S_PFI p1) { }
EXPORT void f0_V_FS_PFF(float p0, struct S_PFF p1) { }
EXPORT void f0_V_FS_PFD(float p0, struct S_PFD p1) { }
EXPORT void f0_V_FS_PFP(float p0, struct S_PFP p1) { }
EXPORT void f0_V_FS_PDI(float p0, struct S_PDI p1) { }
EXPORT void f0_V_FS_PDF(float p0, struct S_PDF p1) { }
EXPORT void f0_V_FS_PDD(float p0, struct S_PDD p1) { }
EXPORT void f0_V_FS_PDP(float p0, struct S_PDP p1) { }
EXPORT void f0_V_FS_PPI(float p0, struct S_PPI p1) { }
EXPORT void f0_V_FS_PPF(float p0, struct S_PPF p1) { }
EXPORT void f0_V_FS_PPD(float p0, struct S_PPD p1) { }
EXPORT void f0_V_FS_PPP(float p0, struct S_PPP p1) { }
EXPORT void f0_V_DI_(double p0, int p1) { }
EXPORT void f0_V_DF_(double p0, float p1) { }
EXPORT void f0_V_DD_(double p0, double p1) { }
EXPORT void f0_V_DP_(double p0, void* p1) { }
EXPORT void f0_V_DS_I(double p0, struct S_I p1) { }
EXPORT void f0_V_DS_F(double p0, struct S_F p1) { }
EXPORT void f0_V_DS_D(double p0, struct S_D p1) { }
EXPORT void f0_V_DS_P(double p0, struct S_P p1) { }
EXPORT void f0_V_DS_II(double p0, struct S_II p1) { }
EXPORT void f0_V_DS_IF(double p0, struct S_IF p1) { }
EXPORT void f0_V_DS_ID(double p0, struct S_ID p1) { }
EXPORT void f0_V_DS_IP(double p0, struct S_IP p1) { }
EXPORT void f0_V_DS_FI(double p0, struct S_FI p1) { }
EXPORT void f0_V_DS_FF(double p0, struct S_FF p1) { }
EXPORT void f0_V_DS_FD(double p0, struct S_FD p1) { }
EXPORT void f0_V_DS_FP(double p0, struct S_FP p1) { }
EXPORT void f0_V_DS_DI(double p0, struct S_DI p1) { }
EXPORT void f0_V_DS_DF(double p0, struct S_DF p1) { }
EXPORT void f0_V_DS_DD(double p0, struct S_DD p1) { }
EXPORT void f0_V_DS_DP(double p0, struct S_DP p1) { }
EXPORT void f0_V_DS_PI(double p0, struct S_PI p1) { }
EXPORT void f0_V_DS_PF(double p0, struct S_PF p1) { }
EXPORT void f0_V_DS_PD(double p0, struct S_PD p1) { }
EXPORT void f0_V_DS_PP(double p0, struct S_PP p1) { }
EXPORT void f0_V_DS_III(double p0, struct S_III p1) { }
EXPORT void f0_V_DS_IIF(double p0, struct S_IIF p1) { }
EXPORT void f0_V_DS_IID(double p0, struct S_IID p1) { }
EXPORT void f0_V_DS_IIP(double p0, struct S_IIP p1) { }
EXPORT void f0_V_DS_IFI(double p0, struct S_IFI p1) { }
EXPORT void f0_V_DS_IFF(double p0, struct S_IFF p1) { }
EXPORT void f0_V_DS_IFD(double p0, struct S_IFD p1) { }
EXPORT void f0_V_DS_IFP(double p0, struct S_IFP p1) { }
EXPORT void f0_V_DS_IDI(double p0, struct S_IDI p1) { }
EXPORT void f0_V_DS_IDF(double p0, struct S_IDF p1) { }
EXPORT void f0_V_DS_IDD(double p0, struct S_IDD p1) { }
EXPORT void f0_V_DS_IDP(double p0, struct S_IDP p1) { }
EXPORT void f0_V_DS_IPI(double p0, struct S_IPI p1) { }
EXPORT void f0_V_DS_IPF(double p0, struct S_IPF p1) { }
EXPORT void f0_V_DS_IPD(double p0, struct S_IPD p1) { }
EXPORT void f0_V_DS_IPP(double p0, struct S_IPP p1) { }
EXPORT void f0_V_DS_FII(double p0, struct S_FII p1) { }
EXPORT void f0_V_DS_FIF(double p0, struct S_FIF p1) { }
EXPORT void f0_V_DS_FID(double p0, struct S_FID p1) { }
EXPORT void f0_V_DS_FIP(double p0, struct S_FIP p1) { }
EXPORT void f0_V_DS_FFI(double p0, struct S_FFI p1) { }
EXPORT void f0_V_DS_FFF(double p0, struct S_FFF p1) { }
EXPORT void f0_V_DS_FFD(double p0, struct S_FFD p1) { }
EXPORT void f0_V_DS_FFP(double p0, struct S_FFP p1) { }
EXPORT void f0_V_DS_FDI(double p0, struct S_FDI p1) { }
EXPORT void f0_V_DS_FDF(double p0, struct S_FDF p1) { }
EXPORT void f0_V_DS_FDD(double p0, struct S_FDD p1) { }
EXPORT void f0_V_DS_FDP(double p0, struct S_FDP p1) { }
EXPORT void f0_V_DS_FPI(double p0, struct S_FPI p1) { }
EXPORT void f0_V_DS_FPF(double p0, struct S_FPF p1) { }
EXPORT void f0_V_DS_FPD(double p0, struct S_FPD p1) { }
EXPORT void f0_V_DS_FPP(double p0, struct S_FPP p1) { }
EXPORT void f0_V_DS_DII(double p0, struct S_DII p1) { }
EXPORT void f0_V_DS_DIF(double p0, struct S_DIF p1) { }
EXPORT void f0_V_DS_DID(double p0, struct S_DID p1) { }
EXPORT void f0_V_DS_DIP(double p0, struct S_DIP p1) { }
EXPORT void f0_V_DS_DFI(double p0, struct S_DFI p1) { }
EXPORT void f0_V_DS_DFF(double p0, struct S_DFF p1) { }
EXPORT void f0_V_DS_DFD(double p0, struct S_DFD p1) { }
EXPORT void f0_V_DS_DFP(double p0, struct S_DFP p1) { }
EXPORT void f0_V_DS_DDI(double p0, struct S_DDI p1) { }
EXPORT void f0_V_DS_DDF(double p0, struct S_DDF p1) { }
EXPORT void f0_V_DS_DDD(double p0, struct S_DDD p1) { }
EXPORT void f0_V_DS_DDP(double p0, struct S_DDP p1) { }
EXPORT void f0_V_DS_DPI(double p0, struct S_DPI p1) { }
EXPORT void f0_V_DS_DPF(double p0, struct S_DPF p1) { }
EXPORT void f0_V_DS_DPD(double p0, struct S_DPD p1) { }
EXPORT void f0_V_DS_DPP(double p0, struct S_DPP p1) { }
EXPORT void f0_V_DS_PII(double p0, struct S_PII p1) { }
EXPORT void f0_V_DS_PIF(double p0, struct S_PIF p1) { }
EXPORT void f0_V_DS_PID(double p0, struct S_PID p1) { }
EXPORT void f0_V_DS_PIP(double p0, struct S_PIP p1) { }
EXPORT void f0_V_DS_PFI(double p0, struct S_PFI p1) { }
EXPORT void f0_V_DS_PFF(double p0, struct S_PFF p1) { }
EXPORT void f0_V_DS_PFD(double p0, struct S_PFD p1) { }
EXPORT void f0_V_DS_PFP(double p0, struct S_PFP p1) { }
EXPORT void f0_V_DS_PDI(double p0, struct S_PDI p1) { }
EXPORT void f0_V_DS_PDF(double p0, struct S_PDF p1) { }
EXPORT void f0_V_DS_PDD(double p0, struct S_PDD p1) { }
EXPORT void f0_V_DS_PDP(double p0, struct S_PDP p1) { }
EXPORT void f0_V_DS_PPI(double p0, struct S_PPI p1) { }
EXPORT void f0_V_DS_PPF(double p0, struct S_PPF p1) { }
EXPORT void f0_V_DS_PPD(double p0, struct S_PPD p1) { }
EXPORT void f0_V_DS_PPP(double p0, struct S_PPP p1) { }
EXPORT void f0_V_PI_(void* p0, int p1) { }
EXPORT void f0_V_PF_(void* p0, float p1) { }
EXPORT void f0_V_PD_(void* p0, double p1) { }
EXPORT void f0_V_PP_(void* p0, void* p1) { }
EXPORT void f0_V_PS_I(void* p0, struct S_I p1) { }
EXPORT void f0_V_PS_F(void* p0, struct S_F p1) { }
EXPORT void f0_V_PS_D(void* p0, struct S_D p1) { }
EXPORT void f0_V_PS_P(void* p0, struct S_P p1) { }
EXPORT void f0_V_PS_II(void* p0, struct S_II p1) { }
EXPORT void f0_V_PS_IF(void* p0, struct S_IF p1) { }
EXPORT void f0_V_PS_ID(void* p0, struct S_ID p1) { }
EXPORT void f0_V_PS_IP(void* p0, struct S_IP p1) { }
EXPORT void f0_V_PS_FI(void* p0, struct S_FI p1) { }
EXPORT void f0_V_PS_FF(void* p0, struct S_FF p1) { }
EXPORT void f0_V_PS_FD(void* p0, struct S_FD p1) { }
EXPORT void f0_V_PS_FP(void* p0, struct S_FP p1) { }
EXPORT void f0_V_PS_DI(void* p0, struct S_DI p1) { }
EXPORT void f0_V_PS_DF(void* p0, struct S_DF p1) { }
EXPORT void f0_V_PS_DD(void* p0, struct S_DD p1) { }
EXPORT void f0_V_PS_DP(void* p0, struct S_DP p1) { }
EXPORT void f0_V_PS_PI(void* p0, struct S_PI p1) { }
EXPORT void f0_V_PS_PF(void* p0, struct S_PF p1) { }
EXPORT void f0_V_PS_PD(void* p0, struct S_PD p1) { }
EXPORT void f0_V_PS_PP(void* p0, struct S_PP p1) { }
EXPORT void f0_V_PS_III(void* p0, struct S_III p1) { }
EXPORT void f0_V_PS_IIF(void* p0, struct S_IIF p1) { }
EXPORT void f0_V_PS_IID(void* p0, struct S_IID p1) { }
EXPORT void f0_V_PS_IIP(void* p0, struct S_IIP p1) { }
EXPORT void f0_V_PS_IFI(void* p0, struct S_IFI p1) { }
EXPORT void f0_V_PS_IFF(void* p0, struct S_IFF p1) { }
EXPORT void f0_V_PS_IFD(void* p0, struct S_IFD p1) { }
EXPORT void f0_V_PS_IFP(void* p0, struct S_IFP p1) { }
EXPORT void f0_V_PS_IDI(void* p0, struct S_IDI p1) { }
EXPORT void f0_V_PS_IDF(void* p0, struct S_IDF p1) { }
EXPORT void f0_V_PS_IDD(void* p0, struct S_IDD p1) { }
EXPORT void f0_V_PS_IDP(void* p0, struct S_IDP p1) { }
EXPORT void f0_V_PS_IPI(void* p0, struct S_IPI p1) { }
EXPORT void f0_V_PS_IPF(void* p0, struct S_IPF p1) { }
EXPORT void f0_V_PS_IPD(void* p0, struct S_IPD p1) { }
EXPORT void f0_V_PS_IPP(void* p0, struct S_IPP p1) { }
EXPORT void f0_V_PS_FII(void* p0, struct S_FII p1) { }
EXPORT void f0_V_PS_FIF(void* p0, struct S_FIF p1) { }
EXPORT void f0_V_PS_FID(void* p0, struct S_FID p1) { }
EXPORT void f0_V_PS_FIP(void* p0, struct S_FIP p1) { }
EXPORT void f0_V_PS_FFI(void* p0, struct S_FFI p1) { }
EXPORT void f0_V_PS_FFF(void* p0, struct S_FFF p1) { }
EXPORT void f0_V_PS_FFD(void* p0, struct S_FFD p1) { }
EXPORT void f0_V_PS_FFP(void* p0, struct S_FFP p1) { }
EXPORT void f0_V_PS_FDI(void* p0, struct S_FDI p1) { }
EXPORT void f0_V_PS_FDF(void* p0, struct S_FDF p1) { }
EXPORT void f0_V_PS_FDD(void* p0, struct S_FDD p1) { }
EXPORT void f0_V_PS_FDP(void* p0, struct S_FDP p1) { }
EXPORT void f0_V_PS_FPI(void* p0, struct S_FPI p1) { }
EXPORT void f0_V_PS_FPF(void* p0, struct S_FPF p1) { }
EXPORT void f0_V_PS_FPD(void* p0, struct S_FPD p1) { }
EXPORT void f0_V_PS_FPP(void* p0, struct S_FPP p1) { }
EXPORT void f0_V_PS_DII(void* p0, struct S_DII p1) { }
EXPORT void f0_V_PS_DIF(void* p0, struct S_DIF p1) { }
EXPORT void f0_V_PS_DID(void* p0, struct S_DID p1) { }
EXPORT void f0_V_PS_DIP(void* p0, struct S_DIP p1) { }
EXPORT void f0_V_PS_DFI(void* p0, struct S_DFI p1) { }
EXPORT void f0_V_PS_DFF(void* p0, struct S_DFF p1) { }
EXPORT void f0_V_PS_DFD(void* p0, struct S_DFD p1) { }
EXPORT void f0_V_PS_DFP(void* p0, struct S_DFP p1) { }
EXPORT void f0_V_PS_DDI(void* p0, struct S_DDI p1) { }
EXPORT void f0_V_PS_DDF(void* p0, struct S_DDF p1) { }
EXPORT void f0_V_PS_DDD(void* p0, struct S_DDD p1) { }
EXPORT void f0_V_PS_DDP(void* p0, struct S_DDP p1) { }
EXPORT void f0_V_PS_DPI(void* p0, struct S_DPI p1) { }
EXPORT void f0_V_PS_DPF(void* p0, struct S_DPF p1) { }
EXPORT void f0_V_PS_DPD(void* p0, struct S_DPD p1) { }
EXPORT void f0_V_PS_DPP(void* p0, struct S_DPP p1) { }
EXPORT void f0_V_PS_PII(void* p0, struct S_PII p1) { }
EXPORT void f0_V_PS_PIF(void* p0, struct S_PIF p1) { }
EXPORT void f0_V_PS_PID(void* p0, struct S_PID p1) { }
EXPORT void f0_V_PS_PIP(void* p0, struct S_PIP p1) { }
EXPORT void f0_V_PS_PFI(void* p0, struct S_PFI p1) { }
EXPORT void f0_V_PS_PFF(void* p0, struct S_PFF p1) { }
EXPORT void f0_V_PS_PFD(void* p0, struct S_PFD p1) { }
EXPORT void f0_V_PS_PFP(void* p0, struct S_PFP p1) { }
EXPORT void f0_V_PS_PDI(void* p0, struct S_PDI p1) { }
EXPORT void f0_V_PS_PDF(void* p0, struct S_PDF p1) { }
EXPORT void f0_V_PS_PDD(void* p0, struct S_PDD p1) { }
EXPORT void f0_V_PS_PDP(void* p0, struct S_PDP p1) { }
EXPORT void f0_V_PS_PPI(void* p0, struct S_PPI p1) { }
EXPORT void f0_V_PS_PPF(void* p0, struct S_PPF p1) { }
EXPORT void f0_V_PS_PPD(void* p0, struct S_PPD p1) { }
EXPORT void f0_V_PS_PPP(void* p0, struct S_PPP p1) { }
EXPORT void f0_V_SI_I(struct S_I p0, int p1) { }
EXPORT void f0_V_SI_F(struct S_F p0, int p1) { }
EXPORT void f0_V_SI_D(struct S_D p0, int p1) { }
EXPORT void f0_V_SI_P(struct S_P p0, int p1) { }
EXPORT void f0_V_SI_II(struct S_II p0, int p1) { }
EXPORT void f0_V_SI_IF(struct S_IF p0, int p1) { }
EXPORT void f0_V_SI_ID(struct S_ID p0, int p1) { }
EXPORT void f0_V_SI_IP(struct S_IP p0, int p1) { }
EXPORT void f0_V_SI_FI(struct S_FI p0, int p1) { }
EXPORT void f0_V_SI_FF(struct S_FF p0, int p1) { }
EXPORT void f0_V_SI_FD(struct S_FD p0, int p1) { }
EXPORT void f0_V_SI_FP(struct S_FP p0, int p1) { }
EXPORT void f0_V_SI_DI(struct S_DI p0, int p1) { }
EXPORT void f0_V_SI_DF(struct S_DF p0, int p1) { }
EXPORT void f0_V_SI_DD(struct S_DD p0, int p1) { }
EXPORT void f0_V_SI_DP(struct S_DP p0, int p1) { }
EXPORT void f0_V_SI_PI(struct S_PI p0, int p1) { }
EXPORT void f0_V_SI_PF(struct S_PF p0, int p1) { }
EXPORT void f0_V_SI_PD(struct S_PD p0, int p1) { }
EXPORT void f0_V_SI_PP(struct S_PP p0, int p1) { }
EXPORT void f0_V_SI_III(struct S_III p0, int p1) { }
EXPORT void f0_V_SI_IIF(struct S_IIF p0, int p1) { }
EXPORT void f0_V_SI_IID(struct S_IID p0, int p1) { }
EXPORT void f0_V_SI_IIP(struct S_IIP p0, int p1) { }
EXPORT void f0_V_SI_IFI(struct S_IFI p0, int p1) { }
EXPORT void f0_V_SI_IFF(struct S_IFF p0, int p1) { }
EXPORT void f0_V_SI_IFD(struct S_IFD p0, int p1) { }
EXPORT void f0_V_SI_IFP(struct S_IFP p0, int p1) { }
EXPORT void f0_V_SI_IDI(struct S_IDI p0, int p1) { }
EXPORT void f0_V_SI_IDF(struct S_IDF p0, int p1) { }
EXPORT void f0_V_SI_IDD(struct S_IDD p0, int p1) { }
EXPORT void f0_V_SI_IDP(struct S_IDP p0, int p1) { }
EXPORT void f0_V_SI_IPI(struct S_IPI p0, int p1) { }
EXPORT void f0_V_SI_IPF(struct S_IPF p0, int p1) { }
EXPORT void f0_V_SI_IPD(struct S_IPD p0, int p1) { }
EXPORT void f0_V_SI_IPP(struct S_IPP p0, int p1) { }
EXPORT void f0_V_SI_FII(struct S_FII p0, int p1) { }
EXPORT void f0_V_SI_FIF(struct S_FIF p0, int p1) { }
EXPORT void f0_V_SI_FID(struct S_FID p0, int p1) { }
EXPORT void f0_V_SI_FIP(struct S_FIP p0, int p1) { }
EXPORT void f0_V_SI_FFI(struct S_FFI p0, int p1) { }
EXPORT void f0_V_SI_FFF(struct S_FFF p0, int p1) { }
EXPORT void f0_V_SI_FFD(struct S_FFD p0, int p1) { }
EXPORT void f0_V_SI_FFP(struct S_FFP p0, int p1) { }
EXPORT void f0_V_SI_FDI(struct S_FDI p0, int p1) { }
EXPORT void f0_V_SI_FDF(struct S_FDF p0, int p1) { }
EXPORT void f0_V_SI_FDD(struct S_FDD p0, int p1) { }
EXPORT void f0_V_SI_FDP(struct S_FDP p0, int p1) { }
EXPORT void f0_V_SI_FPI(struct S_FPI p0, int p1) { }
EXPORT void f0_V_SI_FPF(struct S_FPF p0, int p1) { }
EXPORT void f0_V_SI_FPD(struct S_FPD p0, int p1) { }
EXPORT void f0_V_SI_FPP(struct S_FPP p0, int p1) { }
EXPORT void f0_V_SI_DII(struct S_DII p0, int p1) { }
EXPORT void f0_V_SI_DIF(struct S_DIF p0, int p1) { }
EXPORT void f0_V_SI_DID(struct S_DID p0, int p1) { }
EXPORT void f0_V_SI_DIP(struct S_DIP p0, int p1) { }
EXPORT void f0_V_SI_DFI(struct S_DFI p0, int p1) { }
EXPORT void f0_V_SI_DFF(struct S_DFF p0, int p1) { }
EXPORT void f0_V_SI_DFD(struct S_DFD p0, int p1) { }
EXPORT void f0_V_SI_DFP(struct S_DFP p0, int p1) { }
EXPORT void f0_V_SI_DDI(struct S_DDI p0, int p1) { }
EXPORT void f0_V_SI_DDF(struct S_DDF p0, int p1) { }
EXPORT void f0_V_SI_DDD(struct S_DDD p0, int p1) { }
EXPORT void f0_V_SI_DDP(struct S_DDP p0, int p1) { }
EXPORT void f0_V_SI_DPI(struct S_DPI p0, int p1) { }
EXPORT void f0_V_SI_DPF(struct S_DPF p0, int p1) { }
EXPORT void f0_V_SI_DPD(struct S_DPD p0, int p1) { }
EXPORT void f0_V_SI_DPP(struct S_DPP p0, int p1) { }
EXPORT void f0_V_SI_PII(struct S_PII p0, int p1) { }
EXPORT void f0_V_SI_PIF(struct S_PIF p0, int p1) { }
EXPORT void f0_V_SI_PID(struct S_PID p0, int p1) { }
EXPORT void f0_V_SI_PIP(struct S_PIP p0, int p1) { }
EXPORT void f0_V_SI_PFI(struct S_PFI p0, int p1) { }
EXPORT void f0_V_SI_PFF(struct S_PFF p0, int p1) { }
EXPORT void f0_V_SI_PFD(struct S_PFD p0, int p1) { }
EXPORT void f0_V_SI_PFP(struct S_PFP p0, int p1) { }
EXPORT void f0_V_SI_PDI(struct S_PDI p0, int p1) { }
EXPORT void f0_V_SI_PDF(struct S_PDF p0, int p1) { }
EXPORT void f0_V_SI_PDD(struct S_PDD p0, int p1) { }
EXPORT void f0_V_SI_PDP(struct S_PDP p0, int p1) { }
EXPORT void f0_V_SI_PPI(struct S_PPI p0, int p1) { }
EXPORT void f0_V_SI_PPF(struct S_PPF p0, int p1) { }
EXPORT void f0_V_SI_PPD(struct S_PPD p0, int p1) { }
EXPORT void f0_V_SI_PPP(struct S_PPP p0, int p1) { }
EXPORT void f0_V_SF_I(struct S_I p0, float p1) { }
EXPORT void f0_V_SF_F(struct S_F p0, float p1) { }
EXPORT void f0_V_SF_D(struct S_D p0, float p1) { }
EXPORT void f0_V_SF_P(struct S_P p0, float p1) { }
EXPORT void f0_V_SF_II(struct S_II p0, float p1) { }
EXPORT void f0_V_SF_IF(struct S_IF p0, float p1) { }
EXPORT void f0_V_SF_ID(struct S_ID p0, float p1) { }
EXPORT void f0_V_SF_IP(struct S_IP p0, float p1) { }
EXPORT void f0_V_SF_FI(struct S_FI p0, float p1) { }
EXPORT void f0_V_SF_FF(struct S_FF p0, float p1) { }
EXPORT void f0_V_SF_FD(struct S_FD p0, float p1) { }
EXPORT void f0_V_SF_FP(struct S_FP p0, float p1) { }
EXPORT void f0_V_SF_DI(struct S_DI p0, float p1) { }
EXPORT void f0_V_SF_DF(struct S_DF p0, float p1) { }
EXPORT void f0_V_SF_DD(struct S_DD p0, float p1) { }
EXPORT void f0_V_SF_DP(struct S_DP p0, float p1) { }
EXPORT void f0_V_SF_PI(struct S_PI p0, float p1) { }
EXPORT void f0_V_SF_PF(struct S_PF p0, float p1) { }
EXPORT void f0_V_SF_PD(struct S_PD p0, float p1) { }
EXPORT void f0_V_SF_PP(struct S_PP p0, float p1) { }
EXPORT void f0_V_SF_III(struct S_III p0, float p1) { }
EXPORT void f0_V_SF_IIF(struct S_IIF p0, float p1) { }
EXPORT void f0_V_SF_IID(struct S_IID p0, float p1) { }
EXPORT void f0_V_SF_IIP(struct S_IIP p0, float p1) { }
EXPORT void f0_V_SF_IFI(struct S_IFI p0, float p1) { }
EXPORT void f0_V_SF_IFF(struct S_IFF p0, float p1) { }
EXPORT void f0_V_SF_IFD(struct S_IFD p0, float p1) { }
EXPORT void f0_V_SF_IFP(struct S_IFP p0, float p1) { }
EXPORT void f0_V_SF_IDI(struct S_IDI p0, float p1) { }
EXPORT void f0_V_SF_IDF(struct S_IDF p0, float p1) { }
EXPORT void f0_V_SF_IDD(struct S_IDD p0, float p1) { }
EXPORT void f0_V_SF_IDP(struct S_IDP p0, float p1) { }
EXPORT void f0_V_SF_IPI(struct S_IPI p0, float p1) { }
EXPORT void f0_V_SF_IPF(struct S_IPF p0, float p1) { }
EXPORT void f0_V_SF_IPD(struct S_IPD p0, float p1) { }
EXPORT void f0_V_SF_IPP(struct S_IPP p0, float p1) { }
EXPORT void f0_V_SF_FII(struct S_FII p0, float p1) { }
EXPORT void f0_V_SF_FIF(struct S_FIF p0, float p1) { }
EXPORT void f0_V_SF_FID(struct S_FID p0, float p1) { }
EXPORT void f0_V_SF_FIP(struct S_FIP p0, float p1) { }
EXPORT void f0_V_SF_FFI(struct S_FFI p0, float p1) { }
EXPORT void f0_V_SF_FFF(struct S_FFF p0, float p1) { }
EXPORT void f0_V_SF_FFD(struct S_FFD p0, float p1) { }
EXPORT void f0_V_SF_FFP(struct S_FFP p0, float p1) { }
EXPORT void f0_V_SF_FDI(struct S_FDI p0, float p1) { }
EXPORT void f0_V_SF_FDF(struct S_FDF p0, float p1) { }
EXPORT void f0_V_SF_FDD(struct S_FDD p0, float p1) { }
EXPORT void f0_V_SF_FDP(struct S_FDP p0, float p1) { }
EXPORT void f0_V_SF_FPI(struct S_FPI p0, float p1) { }
EXPORT void f0_V_SF_FPF(struct S_FPF p0, float p1) { }
EXPORT void f0_V_SF_FPD(struct S_FPD p0, float p1) { }
EXPORT void f0_V_SF_FPP(struct S_FPP p0, float p1) { }
EXPORT void f0_V_SF_DII(struct S_DII p0, float p1) { }
EXPORT void f0_V_SF_DIF(struct S_DIF p0, float p1) { }
EXPORT void f0_V_SF_DID(struct S_DID p0, float p1) { }
EXPORT void f0_V_SF_DIP(struct S_DIP p0, float p1) { }
EXPORT void f0_V_SF_DFI(struct S_DFI p0, float p1) { }
EXPORT void f0_V_SF_DFF(struct S_DFF p0, float p1) { }
EXPORT void f0_V_SF_DFD(struct S_DFD p0, float p1) { }
EXPORT void f0_V_SF_DFP(struct S_DFP p0, float p1) { }
EXPORT void f0_V_SF_DDI(struct S_DDI p0, float p1) { }
EXPORT void f0_V_SF_DDF(struct S_DDF p0, float p1) { }
EXPORT void f0_V_SF_DDD(struct S_DDD p0, float p1) { }
EXPORT void f0_V_SF_DDP(struct S_DDP p0, float p1) { }
EXPORT void f0_V_SF_DPI(struct S_DPI p0, float p1) { }
EXPORT void f0_V_SF_DPF(struct S_DPF p0, float p1) { }
EXPORT void f0_V_SF_DPD(struct S_DPD p0, float p1) { }
EXPORT void f0_V_SF_DPP(struct S_DPP p0, float p1) { }
EXPORT void f0_V_SF_PII(struct S_PII p0, float p1) { }
EXPORT void f0_V_SF_PIF(struct S_PIF p0, float p1) { }
EXPORT void f0_V_SF_PID(struct S_PID p0, float p1) { }
EXPORT void f0_V_SF_PIP(struct S_PIP p0, float p1) { }
EXPORT void f0_V_SF_PFI(struct S_PFI p0, float p1) { }
EXPORT void f0_V_SF_PFF(struct S_PFF p0, float p1) { }
EXPORT void f0_V_SF_PFD(struct S_PFD p0, float p1) { }
EXPORT void f1_V_SF_PFP(struct S_PFP p0, float p1) { }
EXPORT void f1_V_SF_PDI(struct S_PDI p0, float p1) { }
EXPORT void f1_V_SF_PDF(struct S_PDF p0, float p1) { }
EXPORT void f1_V_SF_PDD(struct S_PDD p0, float p1) { }
EXPORT void f1_V_SF_PDP(struct S_PDP p0, float p1) { }
EXPORT void f1_V_SF_PPI(struct S_PPI p0, float p1) { }
EXPORT void f1_V_SF_PPF(struct S_PPF p0, float p1) { }
EXPORT void f1_V_SF_PPD(struct S_PPD p0, float p1) { }
EXPORT void f1_V_SF_PPP(struct S_PPP p0, float p1) { }
EXPORT void f1_V_SD_I(struct S_I p0, double p1) { }
EXPORT void f1_V_SD_F(struct S_F p0, double p1) { }
EXPORT void f1_V_SD_D(struct S_D p0, double p1) { }
EXPORT void f1_V_SD_P(struct S_P p0, double p1) { }
EXPORT void f1_V_SD_II(struct S_II p0, double p1) { }
EXPORT void f1_V_SD_IF(struct S_IF p0, double p1) { }
EXPORT void f1_V_SD_ID(struct S_ID p0, double p1) { }
EXPORT void f1_V_SD_IP(struct S_IP p0, double p1) { }
EXPORT void f1_V_SD_FI(struct S_FI p0, double p1) { }
EXPORT void f1_V_SD_FF(struct S_FF p0, double p1) { }
EXPORT void f1_V_SD_FD(struct S_FD p0, double p1) { }
EXPORT void f1_V_SD_FP(struct S_FP p0, double p1) { }
EXPORT void f1_V_SD_DI(struct S_DI p0, double p1) { }
EXPORT void f1_V_SD_DF(struct S_DF p0, double p1) { }
EXPORT void f1_V_SD_DD(struct S_DD p0, double p1) { }
EXPORT void f1_V_SD_DP(struct S_DP p0, double p1) { }
EXPORT void f1_V_SD_PI(struct S_PI p0, double p1) { }
EXPORT void f1_V_SD_PF(struct S_PF p0, double p1) { }
EXPORT void f1_V_SD_PD(struct S_PD p0, double p1) { }
EXPORT void f1_V_SD_PP(struct S_PP p0, double p1) { }
EXPORT void f1_V_SD_III(struct S_III p0, double p1) { }
EXPORT void f1_V_SD_IIF(struct S_IIF p0, double p1) { }
EXPORT void f1_V_SD_IID(struct S_IID p0, double p1) { }
EXPORT void f1_V_SD_IIP(struct S_IIP p0, double p1) { }
EXPORT void f1_V_SD_IFI(struct S_IFI p0, double p1) { }
EXPORT void f1_V_SD_IFF(struct S_IFF p0, double p1) { }
EXPORT void f1_V_SD_IFD(struct S_IFD p0, double p1) { }
EXPORT void f1_V_SD_IFP(struct S_IFP p0, double p1) { }
EXPORT void f1_V_SD_IDI(struct S_IDI p0, double p1) { }
EXPORT void f1_V_SD_IDF(struct S_IDF p0, double p1) { }
EXPORT void f1_V_SD_IDD(struct S_IDD p0, double p1) { }
EXPORT void f1_V_SD_IDP(struct S_IDP p0, double p1) { }
EXPORT void f1_V_SD_IPI(struct S_IPI p0, double p1) { }
EXPORT void f1_V_SD_IPF(struct S_IPF p0, double p1) { }
EXPORT void f1_V_SD_IPD(struct S_IPD p0, double p1) { }
EXPORT void f1_V_SD_IPP(struct S_IPP p0, double p1) { }
EXPORT void f1_V_SD_FII(struct S_FII p0, double p1) { }
EXPORT void f1_V_SD_FIF(struct S_FIF p0, double p1) { }
EXPORT void f1_V_SD_FID(struct S_FID p0, double p1) { }
EXPORT void f1_V_SD_FIP(struct S_FIP p0, double p1) { }
EXPORT void f1_V_SD_FFI(struct S_FFI p0, double p1) { }
EXPORT void f1_V_SD_FFF(struct S_FFF p0, double p1) { }
EXPORT void f1_V_SD_FFD(struct S_FFD p0, double p1) { }
EXPORT void f1_V_SD_FFP(struct S_FFP p0, double p1) { }
EXPORT void f1_V_SD_FDI(struct S_FDI p0, double p1) { }
EXPORT void f1_V_SD_FDF(struct S_FDF p0, double p1) { }
EXPORT void f1_V_SD_FDD(struct S_FDD p0, double p1) { }
EXPORT void f1_V_SD_FDP(struct S_FDP p0, double p1) { }
EXPORT void f1_V_SD_FPI(struct S_FPI p0, double p1) { }
EXPORT void f1_V_SD_FPF(struct S_FPF p0, double p1) { }
EXPORT void f1_V_SD_FPD(struct S_FPD p0, double p1) { }
EXPORT void f1_V_SD_FPP(struct S_FPP p0, double p1) { }
EXPORT void f1_V_SD_DII(struct S_DII p0, double p1) { }
EXPORT void f1_V_SD_DIF(struct S_DIF p0, double p1) { }
EXPORT void f1_V_SD_DID(struct S_DID p0, double p1) { }
EXPORT void f1_V_SD_DIP(struct S_DIP p0, double p1) { }
EXPORT void f1_V_SD_DFI(struct S_DFI p0, double p1) { }
EXPORT void f1_V_SD_DFF(struct S_DFF p0, double p1) { }
EXPORT void f1_V_SD_DFD(struct S_DFD p0, double p1) { }
EXPORT void f1_V_SD_DFP(struct S_DFP p0, double p1) { }
EXPORT void f1_V_SD_DDI(struct S_DDI p0, double p1) { }
EXPORT void f1_V_SD_DDF(struct S_DDF p0, double p1) { }
EXPORT void f1_V_SD_DDD(struct S_DDD p0, double p1) { }
EXPORT void f1_V_SD_DDP(struct S_DDP p0, double p1) { }
EXPORT void f1_V_SD_DPI(struct S_DPI p0, double p1) { }
EXPORT void f1_V_SD_DPF(struct S_DPF p0, double p1) { }
EXPORT void f1_V_SD_DPD(struct S_DPD p0, double p1) { }
EXPORT void f1_V_SD_DPP(struct S_DPP p0, double p1) { }
EXPORT void f1_V_SD_PII(struct S_PII p0, double p1) { }
EXPORT void f1_V_SD_PIF(struct S_PIF p0, double p1) { }
EXPORT void f1_V_SD_PID(struct S_PID p0, double p1) { }
EXPORT void f1_V_SD_PIP(struct S_PIP p0, double p1) { }
EXPORT void f1_V_SD_PFI(struct S_PFI p0, double p1) { }
EXPORT void f1_V_SD_PFF(struct S_PFF p0, double p1) { }
EXPORT void f1_V_SD_PFD(struct S_PFD p0, double p1) { }
EXPORT void f1_V_SD_PFP(struct S_PFP p0, double p1) { }
EXPORT void f1_V_SD_PDI(struct S_PDI p0, double p1) { }
EXPORT void f1_V_SD_PDF(struct S_PDF p0, double p1) { }
EXPORT void f1_V_SD_PDD(struct S_PDD p0, double p1) { }
EXPORT void f1_V_SD_PDP(struct S_PDP p0, double p1) { }
EXPORT void f1_V_SD_PPI(struct S_PPI p0, double p1) { }
EXPORT void f1_V_SD_PPF(struct S_PPF p0, double p1) { }
EXPORT void f1_V_SD_PPD(struct S_PPD p0, double p1) { }
EXPORT void f1_V_SD_PPP(struct S_PPP p0, double p1) { }
EXPORT void f1_V_SP_I(struct S_I p0, void* p1) { }
EXPORT void f1_V_SP_F(struct S_F p0, void* p1) { }
EXPORT void f1_V_SP_D(struct S_D p0, void* p1) { }
EXPORT void f1_V_SP_P(struct S_P p0, void* p1) { }
EXPORT void f1_V_SP_II(struct S_II p0, void* p1) { }
EXPORT void f1_V_SP_IF(struct S_IF p0, void* p1) { }
EXPORT void f1_V_SP_ID(struct S_ID p0, void* p1) { }
EXPORT void f1_V_SP_IP(struct S_IP p0, void* p1) { }
EXPORT void f1_V_SP_FI(struct S_FI p0, void* p1) { }
EXPORT void f1_V_SP_FF(struct S_FF p0, void* p1) { }
EXPORT void f1_V_SP_FD(struct S_FD p0, void* p1) { }
EXPORT void f1_V_SP_FP(struct S_FP p0, void* p1) { }
EXPORT void f1_V_SP_DI(struct S_DI p0, void* p1) { }
EXPORT void f1_V_SP_DF(struct S_DF p0, void* p1) { }
EXPORT void f1_V_SP_DD(struct S_DD p0, void* p1) { }
EXPORT void f1_V_SP_DP(struct S_DP p0, void* p1) { }
EXPORT void f1_V_SP_PI(struct S_PI p0, void* p1) { }
EXPORT void f1_V_SP_PF(struct S_PF p0, void* p1) { }
EXPORT void f1_V_SP_PD(struct S_PD p0, void* p1) { }
EXPORT void f1_V_SP_PP(struct S_PP p0, void* p1) { }
EXPORT void f1_V_SP_III(struct S_III p0, void* p1) { }
EXPORT void f1_V_SP_IIF(struct S_IIF p0, void* p1) { }
EXPORT void f1_V_SP_IID(struct S_IID p0, void* p1) { }
EXPORT void f1_V_SP_IIP(struct S_IIP p0, void* p1) { }
EXPORT void f1_V_SP_IFI(struct S_IFI p0, void* p1) { }
EXPORT void f1_V_SP_IFF(struct S_IFF p0, void* p1) { }
EXPORT void f1_V_SP_IFD(struct S_IFD p0, void* p1) { }
EXPORT void f1_V_SP_IFP(struct S_IFP p0, void* p1) { }
EXPORT void f1_V_SP_IDI(struct S_IDI p0, void* p1) { }
EXPORT void f1_V_SP_IDF(struct S_IDF p0, void* p1) { }
EXPORT void f1_V_SP_IDD(struct S_IDD p0, void* p1) { }
EXPORT void f1_V_SP_IDP(struct S_IDP p0, void* p1) { }
EXPORT void f1_V_SP_IPI(struct S_IPI p0, void* p1) { }
EXPORT void f1_V_SP_IPF(struct S_IPF p0, void* p1) { }
EXPORT void f1_V_SP_IPD(struct S_IPD p0, void* p1) { }
EXPORT void f1_V_SP_IPP(struct S_IPP p0, void* p1) { }
EXPORT void f1_V_SP_FII(struct S_FII p0, void* p1) { }
EXPORT void f1_V_SP_FIF(struct S_FIF p0, void* p1) { }
EXPORT void f1_V_SP_FID(struct S_FID p0, void* p1) { }
EXPORT void f1_V_SP_FIP(struct S_FIP p0, void* p1) { }
EXPORT void f1_V_SP_FFI(struct S_FFI p0, void* p1) { }
EXPORT void f1_V_SP_FFF(struct S_FFF p0, void* p1) { }
EXPORT void f1_V_SP_FFD(struct S_FFD p0, void* p1) { }
EXPORT void f1_V_SP_FFP(struct S_FFP p0, void* p1) { }
EXPORT void f1_V_SP_FDI(struct S_FDI p0, void* p1) { }
EXPORT void f1_V_SP_FDF(struct S_FDF p0, void* p1) { }
EXPORT void f1_V_SP_FDD(struct S_FDD p0, void* p1) { }
EXPORT void f1_V_SP_FDP(struct S_FDP p0, void* p1) { }
EXPORT void f1_V_SP_FPI(struct S_FPI p0, void* p1) { }
EXPORT void f1_V_SP_FPF(struct S_FPF p0, void* p1) { }
EXPORT void f1_V_SP_FPD(struct S_FPD p0, void* p1) { }
EXPORT void f1_V_SP_FPP(struct S_FPP p0, void* p1) { }
EXPORT void f1_V_SP_DII(struct S_DII p0, void* p1) { }
EXPORT void f1_V_SP_DIF(struct S_DIF p0, void* p1) { }
EXPORT void f1_V_SP_DID(struct S_DID p0, void* p1) { }
EXPORT void f1_V_SP_DIP(struct S_DIP p0, void* p1) { }
EXPORT void f1_V_SP_DFI(struct S_DFI p0, void* p1) { }
EXPORT void f1_V_SP_DFF(struct S_DFF p0, void* p1) { }
EXPORT void f1_V_SP_DFD(struct S_DFD p0, void* p1) { }
EXPORT void f1_V_SP_DFP(struct S_DFP p0, void* p1) { }
EXPORT void f1_V_SP_DDI(struct S_DDI p0, void* p1) { }
EXPORT void f1_V_SP_DDF(struct S_DDF p0, void* p1) { }
EXPORT void f1_V_SP_DDD(struct S_DDD p0, void* p1) { }
EXPORT void f1_V_SP_DDP(struct S_DDP p0, void* p1) { }
EXPORT void f1_V_SP_DPI(struct S_DPI p0, void* p1) { }
EXPORT void f1_V_SP_DPF(struct S_DPF p0, void* p1) { }
EXPORT void f1_V_SP_DPD(struct S_DPD p0, void* p1) { }
EXPORT void f1_V_SP_DPP(struct S_DPP p0, void* p1) { }
EXPORT void f1_V_SP_PII(struct S_PII p0, void* p1) { }
EXPORT void f1_V_SP_PIF(struct S_PIF p0, void* p1) { }
EXPORT void f1_V_SP_PID(struct S_PID p0, void* p1) { }
EXPORT void f1_V_SP_PIP(struct S_PIP p0, void* p1) { }
EXPORT void f1_V_SP_PFI(struct S_PFI p0, void* p1) { }
EXPORT void f1_V_SP_PFF(struct S_PFF p0, void* p1) { }
EXPORT void f1_V_SP_PFD(struct S_PFD p0, void* p1) { }
EXPORT void f1_V_SP_PFP(struct S_PFP p0, void* p1) { }
EXPORT void f1_V_SP_PDI(struct S_PDI p0, void* p1) { }
EXPORT void f1_V_SP_PDF(struct S_PDF p0, void* p1) { }
EXPORT void f1_V_SP_PDD(struct S_PDD p0, void* p1) { }
EXPORT void f1_V_SP_PDP(struct S_PDP p0, void* p1) { }
EXPORT void f1_V_SP_PPI(struct S_PPI p0, void* p1) { }
EXPORT void f1_V_SP_PPF(struct S_PPF p0, void* p1) { }
EXPORT void f1_V_SP_PPD(struct S_PPD p0, void* p1) { }
EXPORT void f1_V_SP_PPP(struct S_PPP p0, void* p1) { }
EXPORT void f1_V_SS_I(struct S_I p0, struct S_I p1) { }
EXPORT void f1_V_SS_F(struct S_F p0, struct S_F p1) { }
EXPORT void f1_V_SS_D(struct S_D p0, struct S_D p1) { }
EXPORT void f1_V_SS_P(struct S_P p0, struct S_P p1) { }
EXPORT void f1_V_SS_II(struct S_II p0, struct S_II p1) { }
EXPORT void f1_V_SS_IF(struct S_IF p0, struct S_IF p1) { }
EXPORT void f1_V_SS_ID(struct S_ID p0, struct S_ID p1) { }
EXPORT void f1_V_SS_IP(struct S_IP p0, struct S_IP p1) { }
EXPORT void f1_V_SS_FI(struct S_FI p0, struct S_FI p1) { }
EXPORT void f1_V_SS_FF(struct S_FF p0, struct S_FF p1) { }
EXPORT void f1_V_SS_FD(struct S_FD p0, struct S_FD p1) { }
EXPORT void f1_V_SS_FP(struct S_FP p0, struct S_FP p1) { }
EXPORT void f1_V_SS_DI(struct S_DI p0, struct S_DI p1) { }
EXPORT void f1_V_SS_DF(struct S_DF p0, struct S_DF p1) { }
EXPORT void f1_V_SS_DD(struct S_DD p0, struct S_DD p1) { }
EXPORT void f1_V_SS_DP(struct S_DP p0, struct S_DP p1) { }
EXPORT void f1_V_SS_PI(struct S_PI p0, struct S_PI p1) { }
EXPORT void f1_V_SS_PF(struct S_PF p0, struct S_PF p1) { }
EXPORT void f1_V_SS_PD(struct S_PD p0, struct S_PD p1) { }
EXPORT void f1_V_SS_PP(struct S_PP p0, struct S_PP p1) { }
EXPORT void f1_V_SS_III(struct S_III p0, struct S_III p1) { }
EXPORT void f1_V_SS_IIF(struct S_IIF p0, struct S_IIF p1) { }
EXPORT void f1_V_SS_IID(struct S_IID p0, struct S_IID p1) { }
EXPORT void f1_V_SS_IIP(struct S_IIP p0, struct S_IIP p1) { }
EXPORT void f1_V_SS_IFI(struct S_IFI p0, struct S_IFI p1) { }
EXPORT void f1_V_SS_IFF(struct S_IFF p0, struct S_IFF p1) { }
EXPORT void f1_V_SS_IFD(struct S_IFD p0, struct S_IFD p1) { }
EXPORT void f1_V_SS_IFP(struct S_IFP p0, struct S_IFP p1) { }
EXPORT void f1_V_SS_IDI(struct S_IDI p0, struct S_IDI p1) { }
EXPORT void f1_V_SS_IDF(struct S_IDF p0, struct S_IDF p1) { }
EXPORT void f1_V_SS_IDD(struct S_IDD p0, struct S_IDD p1) { }
EXPORT void f1_V_SS_IDP(struct S_IDP p0, struct S_IDP p1) { }
EXPORT void f1_V_SS_IPI(struct S_IPI p0, struct S_IPI p1) { }
EXPORT void f1_V_SS_IPF(struct S_IPF p0, struct S_IPF p1) { }
EXPORT void f1_V_SS_IPD(struct S_IPD p0, struct S_IPD p1) { }
EXPORT void f1_V_SS_IPP(struct S_IPP p0, struct S_IPP p1) { }
EXPORT void f1_V_SS_FII(struct S_FII p0, struct S_FII p1) { }
EXPORT void f1_V_SS_FIF(struct S_FIF p0, struct S_FIF p1) { }
EXPORT void f1_V_SS_FID(struct S_FID p0, struct S_FID p1) { }
EXPORT void f1_V_SS_FIP(struct S_FIP p0, struct S_FIP p1) { }
EXPORT void f1_V_SS_FFI(struct S_FFI p0, struct S_FFI p1) { }
EXPORT void f1_V_SS_FFF(struct S_FFF p0, struct S_FFF p1) { }
EXPORT void f1_V_SS_FFD(struct S_FFD p0, struct S_FFD p1) { }
EXPORT void f1_V_SS_FFP(struct S_FFP p0, struct S_FFP p1) { }
EXPORT void f1_V_SS_FDI(struct S_FDI p0, struct S_FDI p1) { }
EXPORT void f1_V_SS_FDF(struct S_FDF p0, struct S_FDF p1) { }
EXPORT void f1_V_SS_FDD(struct S_FDD p0, struct S_FDD p1) { }
EXPORT void f1_V_SS_FDP(struct S_FDP p0, struct S_FDP p1) { }
EXPORT void f1_V_SS_FPI(struct S_FPI p0, struct S_FPI p1) { }
EXPORT void f1_V_SS_FPF(struct S_FPF p0, struct S_FPF p1) { }
EXPORT void f1_V_SS_FPD(struct S_FPD p0, struct S_FPD p1) { }
EXPORT void f1_V_SS_FPP(struct S_FPP p0, struct S_FPP p1) { }
EXPORT void f1_V_SS_DII(struct S_DII p0, struct S_DII p1) { }
EXPORT void f1_V_SS_DIF(struct S_DIF p0, struct S_DIF p1) { }
EXPORT void f1_V_SS_DID(struct S_DID p0, struct S_DID p1) { }
EXPORT void f1_V_SS_DIP(struct S_DIP p0, struct S_DIP p1) { }
EXPORT void f1_V_SS_DFI(struct S_DFI p0, struct S_DFI p1) { }
EXPORT void f1_V_SS_DFF(struct S_DFF p0, struct S_DFF p1) { }
EXPORT void f1_V_SS_DFD(struct S_DFD p0, struct S_DFD p1) { }
EXPORT void f1_V_SS_DFP(struct S_DFP p0, struct S_DFP p1) { }
EXPORT void f1_V_SS_DDI(struct S_DDI p0, struct S_DDI p1) { }
EXPORT void f1_V_SS_DDF(struct S_DDF p0, struct S_DDF p1) { }
EXPORT void f1_V_SS_DDD(struct S_DDD p0, struct S_DDD p1) { }
EXPORT void f1_V_SS_DDP(struct S_DDP p0, struct S_DDP p1) { }
EXPORT void f1_V_SS_DPI(struct S_DPI p0, struct S_DPI p1) { }
EXPORT void f1_V_SS_DPF(struct S_DPF p0, struct S_DPF p1) { }
EXPORT void f1_V_SS_DPD(struct S_DPD p0, struct S_DPD p1) { }
EXPORT void f1_V_SS_DPP(struct S_DPP p0, struct S_DPP p1) { }
EXPORT void f1_V_SS_PII(struct S_PII p0, struct S_PII p1) { }
EXPORT void f1_V_SS_PIF(struct S_PIF p0, struct S_PIF p1) { }
EXPORT void f1_V_SS_PID(struct S_PID p0, struct S_PID p1) { }
EXPORT void f1_V_SS_PIP(struct S_PIP p0, struct S_PIP p1) { }
EXPORT void f1_V_SS_PFI(struct S_PFI p0, struct S_PFI p1) { }
EXPORT void f1_V_SS_PFF(struct S_PFF p0, struct S_PFF p1) { }
EXPORT void f1_V_SS_PFD(struct S_PFD p0, struct S_PFD p1) { }
EXPORT void f1_V_SS_PFP(struct S_PFP p0, struct S_PFP p1) { }
EXPORT void f1_V_SS_PDI(struct S_PDI p0, struct S_PDI p1) { }
EXPORT void f1_V_SS_PDF(struct S_PDF p0, struct S_PDF p1) { }
EXPORT void f1_V_SS_PDD(struct S_PDD p0, struct S_PDD p1) { }
EXPORT void f1_V_SS_PDP(struct S_PDP p0, struct S_PDP p1) { }
EXPORT void f1_V_SS_PPI(struct S_PPI p0, struct S_PPI p1) { }
EXPORT void f1_V_SS_PPF(struct S_PPF p0, struct S_PPF p1) { }
EXPORT void f1_V_SS_PPD(struct S_PPD p0, struct S_PPD p1) { }
EXPORT void f1_V_SS_PPP(struct S_PPP p0, struct S_PPP p1) { }
EXPORT void f1_V_III_(int p0, int p1, int p2) { }
EXPORT void f1_V_IIF_(int p0, int p1, float p2) { }
EXPORT void f1_V_IID_(int p0, int p1, double p2) { }
EXPORT void f1_V_IIP_(int p0, int p1, void* p2) { }
EXPORT void f1_V_IIS_I(int p0, int p1, struct S_I p2) { }
EXPORT void f1_V_IIS_F(int p0, int p1, struct S_F p2) { }
EXPORT void f1_V_IIS_D(int p0, int p1, struct S_D p2) { }
EXPORT void f1_V_IIS_P(int p0, int p1, struct S_P p2) { }
EXPORT void f1_V_IIS_II(int p0, int p1, struct S_II p2) { }
EXPORT void f1_V_IIS_IF(int p0, int p1, struct S_IF p2) { }
EXPORT void f1_V_IIS_ID(int p0, int p1, struct S_ID p2) { }
EXPORT void f1_V_IIS_IP(int p0, int p1, struct S_IP p2) { }
EXPORT void f1_V_IIS_FI(int p0, int p1, struct S_FI p2) { }
EXPORT void f1_V_IIS_FF(int p0, int p1, struct S_FF p2) { }
EXPORT void f1_V_IIS_FD(int p0, int p1, struct S_FD p2) { }
EXPORT void f1_V_IIS_FP(int p0, int p1, struct S_FP p2) { }
EXPORT void f1_V_IIS_DI(int p0, int p1, struct S_DI p2) { }
EXPORT void f1_V_IIS_DF(int p0, int p1, struct S_DF p2) { }
EXPORT void f1_V_IIS_DD(int p0, int p1, struct S_DD p2) { }
EXPORT void f1_V_IIS_DP(int p0, int p1, struct S_DP p2) { }
EXPORT void f1_V_IIS_PI(int p0, int p1, struct S_PI p2) { }
EXPORT void f1_V_IIS_PF(int p0, int p1, struct S_PF p2) { }
EXPORT void f1_V_IIS_PD(int p0, int p1, struct S_PD p2) { }
EXPORT void f1_V_IIS_PP(int p0, int p1, struct S_PP p2) { }
EXPORT void f1_V_IIS_III(int p0, int p1, struct S_III p2) { }
EXPORT void f1_V_IIS_IIF(int p0, int p1, struct S_IIF p2) { }
EXPORT void f1_V_IIS_IID(int p0, int p1, struct S_IID p2) { }
EXPORT void f1_V_IIS_IIP(int p0, int p1, struct S_IIP p2) { }
EXPORT void f1_V_IIS_IFI(int p0, int p1, struct S_IFI p2) { }
EXPORT void f1_V_IIS_IFF(int p0, int p1, struct S_IFF p2) { }
EXPORT void f1_V_IIS_IFD(int p0, int p1, struct S_IFD p2) { }
EXPORT void f1_V_IIS_IFP(int p0, int p1, struct S_IFP p2) { }
EXPORT void f1_V_IIS_IDI(int p0, int p1, struct S_IDI p2) { }
EXPORT void f1_V_IIS_IDF(int p0, int p1, struct S_IDF p2) { }
EXPORT void f1_V_IIS_IDD(int p0, int p1, struct S_IDD p2) { }
EXPORT void f1_V_IIS_IDP(int p0, int p1, struct S_IDP p2) { }
EXPORT void f1_V_IIS_IPI(int p0, int p1, struct S_IPI p2) { }
EXPORT void f1_V_IIS_IPF(int p0, int p1, struct S_IPF p2) { }
EXPORT void f1_V_IIS_IPD(int p0, int p1, struct S_IPD p2) { }
EXPORT void f1_V_IIS_IPP(int p0, int p1, struct S_IPP p2) { }
EXPORT void f1_V_IIS_FII(int p0, int p1, struct S_FII p2) { }
EXPORT void f1_V_IIS_FIF(int p0, int p1, struct S_FIF p2) { }
EXPORT void f1_V_IIS_FID(int p0, int p1, struct S_FID p2) { }
EXPORT void f1_V_IIS_FIP(int p0, int p1, struct S_FIP p2) { }
EXPORT void f1_V_IIS_FFI(int p0, int p1, struct S_FFI p2) { }
EXPORT void f1_V_IIS_FFF(int p0, int p1, struct S_FFF p2) { }
EXPORT void f1_V_IIS_FFD(int p0, int p1, struct S_FFD p2) { }
EXPORT void f1_V_IIS_FFP(int p0, int p1, struct S_FFP p2) { }
EXPORT void f1_V_IIS_FDI(int p0, int p1, struct S_FDI p2) { }
EXPORT void f1_V_IIS_FDF(int p0, int p1, struct S_FDF p2) { }
EXPORT void f1_V_IIS_FDD(int p0, int p1, struct S_FDD p2) { }
EXPORT void f1_V_IIS_FDP(int p0, int p1, struct S_FDP p2) { }
EXPORT void f1_V_IIS_FPI(int p0, int p1, struct S_FPI p2) { }
EXPORT void f1_V_IIS_FPF(int p0, int p1, struct S_FPF p2) { }
EXPORT void f1_V_IIS_FPD(int p0, int p1, struct S_FPD p2) { }
EXPORT void f1_V_IIS_FPP(int p0, int p1, struct S_FPP p2) { }
EXPORT void f1_V_IIS_DII(int p0, int p1, struct S_DII p2) { }
EXPORT void f1_V_IIS_DIF(int p0, int p1, struct S_DIF p2) { }
EXPORT void f1_V_IIS_DID(int p0, int p1, struct S_DID p2) { }
EXPORT void f1_V_IIS_DIP(int p0, int p1, struct S_DIP p2) { }
EXPORT void f1_V_IIS_DFI(int p0, int p1, struct S_DFI p2) { }
EXPORT void f1_V_IIS_DFF(int p0, int p1, struct S_DFF p2) { }
EXPORT void f1_V_IIS_DFD(int p0, int p1, struct S_DFD p2) { }
EXPORT void f1_V_IIS_DFP(int p0, int p1, struct S_DFP p2) { }
EXPORT void f1_V_IIS_DDI(int p0, int p1, struct S_DDI p2) { }
EXPORT void f1_V_IIS_DDF(int p0, int p1, struct S_DDF p2) { }
EXPORT void f1_V_IIS_DDD(int p0, int p1, struct S_DDD p2) { }
EXPORT void f1_V_IIS_DDP(int p0, int p1, struct S_DDP p2) { }
EXPORT void f1_V_IIS_DPI(int p0, int p1, struct S_DPI p2) { }
EXPORT void f1_V_IIS_DPF(int p0, int p1, struct S_DPF p2) { }
EXPORT void f1_V_IIS_DPD(int p0, int p1, struct S_DPD p2) { }
EXPORT void f1_V_IIS_DPP(int p0, int p1, struct S_DPP p2) { }
EXPORT void f1_V_IIS_PII(int p0, int p1, struct S_PII p2) { }
EXPORT void f1_V_IIS_PIF(int p0, int p1, struct S_PIF p2) { }
EXPORT void f1_V_IIS_PID(int p0, int p1, struct S_PID p2) { }
EXPORT void f1_V_IIS_PIP(int p0, int p1, struct S_PIP p2) { }
EXPORT void f1_V_IIS_PFI(int p0, int p1, struct S_PFI p2) { }
EXPORT void f1_V_IIS_PFF(int p0, int p1, struct S_PFF p2) { }
EXPORT void f1_V_IIS_PFD(int p0, int p1, struct S_PFD p2) { }
EXPORT void f1_V_IIS_PFP(int p0, int p1, struct S_PFP p2) { }
EXPORT void f1_V_IIS_PDI(int p0, int p1, struct S_PDI p2) { }
EXPORT void f1_V_IIS_PDF(int p0, int p1, struct S_PDF p2) { }
EXPORT void f1_V_IIS_PDD(int p0, int p1, struct S_PDD p2) { }
EXPORT void f1_V_IIS_PDP(int p0, int p1, struct S_PDP p2) { }
EXPORT void f1_V_IIS_PPI(int p0, int p1, struct S_PPI p2) { }
EXPORT void f1_V_IIS_PPF(int p0, int p1, struct S_PPF p2) { }
EXPORT void f1_V_IIS_PPD(int p0, int p1, struct S_PPD p2) { }
EXPORT void f1_V_IIS_PPP(int p0, int p1, struct S_PPP p2) { }
EXPORT void f1_V_IFI_(int p0, float p1, int p2) { }
EXPORT void f1_V_IFF_(int p0, float p1, float p2) { }
EXPORT void f1_V_IFD_(int p0, float p1, double p2) { }
EXPORT void f1_V_IFP_(int p0, float p1, void* p2) { }
EXPORT void f1_V_IFS_I(int p0, float p1, struct S_I p2) { }
EXPORT void f1_V_IFS_F(int p0, float p1, struct S_F p2) { }
EXPORT void f1_V_IFS_D(int p0, float p1, struct S_D p2) { }
EXPORT void f1_V_IFS_P(int p0, float p1, struct S_P p2) { }
EXPORT void f1_V_IFS_II(int p0, float p1, struct S_II p2) { }
EXPORT void f1_V_IFS_IF(int p0, float p1, struct S_IF p2) { }
EXPORT void f1_V_IFS_ID(int p0, float p1, struct S_ID p2) { }
EXPORT void f1_V_IFS_IP(int p0, float p1, struct S_IP p2) { }
EXPORT void f1_V_IFS_FI(int p0, float p1, struct S_FI p2) { }
EXPORT void f1_V_IFS_FF(int p0, float p1, struct S_FF p2) { }
EXPORT void f1_V_IFS_FD(int p0, float p1, struct S_FD p2) { }
EXPORT void f1_V_IFS_FP(int p0, float p1, struct S_FP p2) { }
EXPORT void f1_V_IFS_DI(int p0, float p1, struct S_DI p2) { }
EXPORT void f1_V_IFS_DF(int p0, float p1, struct S_DF p2) { }
EXPORT void f1_V_IFS_DD(int p0, float p1, struct S_DD p2) { }
EXPORT void f1_V_IFS_DP(int p0, float p1, struct S_DP p2) { }
EXPORT void f1_V_IFS_PI(int p0, float p1, struct S_PI p2) { }
EXPORT void f1_V_IFS_PF(int p0, float p1, struct S_PF p2) { }
EXPORT void f1_V_IFS_PD(int p0, float p1, struct S_PD p2) { }
EXPORT void f1_V_IFS_PP(int p0, float p1, struct S_PP p2) { }
EXPORT void f1_V_IFS_III(int p0, float p1, struct S_III p2) { }
EXPORT void f1_V_IFS_IIF(int p0, float p1, struct S_IIF p2) { }
EXPORT void f1_V_IFS_IID(int p0, float p1, struct S_IID p2) { }
EXPORT void f1_V_IFS_IIP(int p0, float p1, struct S_IIP p2) { }
EXPORT void f1_V_IFS_IFI(int p0, float p1, struct S_IFI p2) { }
EXPORT void f1_V_IFS_IFF(int p0, float p1, struct S_IFF p2) { }
EXPORT void f1_V_IFS_IFD(int p0, float p1, struct S_IFD p2) { }
EXPORT void f1_V_IFS_IFP(int p0, float p1, struct S_IFP p2) { }
EXPORT void f1_V_IFS_IDI(int p0, float p1, struct S_IDI p2) { }
EXPORT void f1_V_IFS_IDF(int p0, float p1, struct S_IDF p2) { }
EXPORT void f1_V_IFS_IDD(int p0, float p1, struct S_IDD p2) { }
EXPORT void f1_V_IFS_IDP(int p0, float p1, struct S_IDP p2) { }
EXPORT void f1_V_IFS_IPI(int p0, float p1, struct S_IPI p2) { }
EXPORT void f1_V_IFS_IPF(int p0, float p1, struct S_IPF p2) { }
EXPORT void f1_V_IFS_IPD(int p0, float p1, struct S_IPD p2) { }
EXPORT void f1_V_IFS_IPP(int p0, float p1, struct S_IPP p2) { }
EXPORT void f1_V_IFS_FII(int p0, float p1, struct S_FII p2) { }
EXPORT void f1_V_IFS_FIF(int p0, float p1, struct S_FIF p2) { }
EXPORT void f1_V_IFS_FID(int p0, float p1, struct S_FID p2) { }
EXPORT void f1_V_IFS_FIP(int p0, float p1, struct S_FIP p2) { }
EXPORT void f1_V_IFS_FFI(int p0, float p1, struct S_FFI p2) { }
EXPORT void f1_V_IFS_FFF(int p0, float p1, struct S_FFF p2) { }
EXPORT void f1_V_IFS_FFD(int p0, float p1, struct S_FFD p2) { }
EXPORT void f1_V_IFS_FFP(int p0, float p1, struct S_FFP p2) { }
EXPORT void f1_V_IFS_FDI(int p0, float p1, struct S_FDI p2) { }
EXPORT void f1_V_IFS_FDF(int p0, float p1, struct S_FDF p2) { }
EXPORT void f1_V_IFS_FDD(int p0, float p1, struct S_FDD p2) { }
EXPORT void f1_V_IFS_FDP(int p0, float p1, struct S_FDP p2) { }
EXPORT void f1_V_IFS_FPI(int p0, float p1, struct S_FPI p2) { }
EXPORT void f1_V_IFS_FPF(int p0, float p1, struct S_FPF p2) { }
EXPORT void f1_V_IFS_FPD(int p0, float p1, struct S_FPD p2) { }
EXPORT void f1_V_IFS_FPP(int p0, float p1, struct S_FPP p2) { }
EXPORT void f1_V_IFS_DII(int p0, float p1, struct S_DII p2) { }
EXPORT void f1_V_IFS_DIF(int p0, float p1, struct S_DIF p2) { }
EXPORT void f1_V_IFS_DID(int p0, float p1, struct S_DID p2) { }
EXPORT void f1_V_IFS_DIP(int p0, float p1, struct S_DIP p2) { }
EXPORT void f1_V_IFS_DFI(int p0, float p1, struct S_DFI p2) { }
EXPORT void f1_V_IFS_DFF(int p0, float p1, struct S_DFF p2) { }
EXPORT void f1_V_IFS_DFD(int p0, float p1, struct S_DFD p2) { }
EXPORT void f1_V_IFS_DFP(int p0, float p1, struct S_DFP p2) { }
EXPORT void f1_V_IFS_DDI(int p0, float p1, struct S_DDI p2) { }
EXPORT void f1_V_IFS_DDF(int p0, float p1, struct S_DDF p2) { }
EXPORT void f1_V_IFS_DDD(int p0, float p1, struct S_DDD p2) { }
EXPORT void f1_V_IFS_DDP(int p0, float p1, struct S_DDP p2) { }
EXPORT void f1_V_IFS_DPI(int p0, float p1, struct S_DPI p2) { }
EXPORT void f1_V_IFS_DPF(int p0, float p1, struct S_DPF p2) { }
EXPORT void f1_V_IFS_DPD(int p0, float p1, struct S_DPD p2) { }
EXPORT void f1_V_IFS_DPP(int p0, float p1, struct S_DPP p2) { }
EXPORT void f1_V_IFS_PII(int p0, float p1, struct S_PII p2) { }
EXPORT void f1_V_IFS_PIF(int p0, float p1, struct S_PIF p2) { }
EXPORT void f1_V_IFS_PID(int p0, float p1, struct S_PID p2) { }
EXPORT void f1_V_IFS_PIP(int p0, float p1, struct S_PIP p2) { }
EXPORT void f1_V_IFS_PFI(int p0, float p1, struct S_PFI p2) { }
EXPORT void f1_V_IFS_PFF(int p0, float p1, struct S_PFF p2) { }
EXPORT void f1_V_IFS_PFD(int p0, float p1, struct S_PFD p2) { }
EXPORT void f1_V_IFS_PFP(int p0, float p1, struct S_PFP p2) { }
EXPORT void f1_V_IFS_PDI(int p0, float p1, struct S_PDI p2) { }
EXPORT void f1_V_IFS_PDF(int p0, float p1, struct S_PDF p2) { }
EXPORT void f1_V_IFS_PDD(int p0, float p1, struct S_PDD p2) { }
EXPORT void f1_V_IFS_PDP(int p0, float p1, struct S_PDP p2) { }
EXPORT void f1_V_IFS_PPI(int p0, float p1, struct S_PPI p2) { }
EXPORT void f1_V_IFS_PPF(int p0, float p1, struct S_PPF p2) { }
EXPORT void f1_V_IFS_PPD(int p0, float p1, struct S_PPD p2) { }
EXPORT void f1_V_IFS_PPP(int p0, float p1, struct S_PPP p2) { }
EXPORT void f1_V_IDI_(int p0, double p1, int p2) { }
EXPORT void f1_V_IDF_(int p0, double p1, float p2) { }
EXPORT void f1_V_IDD_(int p0, double p1, double p2) { }
EXPORT void f1_V_IDP_(int p0, double p1, void* p2) { }
EXPORT void f1_V_IDS_I(int p0, double p1, struct S_I p2) { }
EXPORT void f1_V_IDS_F(int p0, double p1, struct S_F p2) { }
EXPORT void f1_V_IDS_D(int p0, double p1, struct S_D p2) { }
EXPORT void f1_V_IDS_P(int p0, double p1, struct S_P p2) { }
EXPORT void f1_V_IDS_II(int p0, double p1, struct S_II p2) { }
EXPORT void f1_V_IDS_IF(int p0, double p1, struct S_IF p2) { }
EXPORT void f1_V_IDS_ID(int p0, double p1, struct S_ID p2) { }
EXPORT void f1_V_IDS_IP(int p0, double p1, struct S_IP p2) { }
EXPORT void f1_V_IDS_FI(int p0, double p1, struct S_FI p2) { }
EXPORT void f1_V_IDS_FF(int p0, double p1, struct S_FF p2) { }
EXPORT void f1_V_IDS_FD(int p0, double p1, struct S_FD p2) { }
EXPORT void f1_V_IDS_FP(int p0, double p1, struct S_FP p2) { }
EXPORT void f1_V_IDS_DI(int p0, double p1, struct S_DI p2) { }
EXPORT void f1_V_IDS_DF(int p0, double p1, struct S_DF p2) { }
EXPORT void f1_V_IDS_DD(int p0, double p1, struct S_DD p2) { }
EXPORT void f1_V_IDS_DP(int p0, double p1, struct S_DP p2) { }
EXPORT void f1_V_IDS_PI(int p0, double p1, struct S_PI p2) { }
EXPORT void f1_V_IDS_PF(int p0, double p1, struct S_PF p2) { }
EXPORT void f1_V_IDS_PD(int p0, double p1, struct S_PD p2) { }
EXPORT void f1_V_IDS_PP(int p0, double p1, struct S_PP p2) { }
EXPORT void f1_V_IDS_III(int p0, double p1, struct S_III p2) { }
EXPORT void f1_V_IDS_IIF(int p0, double p1, struct S_IIF p2) { }
EXPORT void f1_V_IDS_IID(int p0, double p1, struct S_IID p2) { }
EXPORT void f1_V_IDS_IIP(int p0, double p1, struct S_IIP p2) { }
EXPORT void f1_V_IDS_IFI(int p0, double p1, struct S_IFI p2) { }
EXPORT void f1_V_IDS_IFF(int p0, double p1, struct S_IFF p2) { }
EXPORT void f1_V_IDS_IFD(int p0, double p1, struct S_IFD p2) { }
EXPORT void f1_V_IDS_IFP(int p0, double p1, struct S_IFP p2) { }
EXPORT void f1_V_IDS_IDI(int p0, double p1, struct S_IDI p2) { }
EXPORT void f1_V_IDS_IDF(int p0, double p1, struct S_IDF p2) { }
EXPORT void f1_V_IDS_IDD(int p0, double p1, struct S_IDD p2) { }
EXPORT void f1_V_IDS_IDP(int p0, double p1, struct S_IDP p2) { }
EXPORT void f1_V_IDS_IPI(int p0, double p1, struct S_IPI p2) { }
EXPORT void f1_V_IDS_IPF(int p0, double p1, struct S_IPF p2) { }
EXPORT void f1_V_IDS_IPD(int p0, double p1, struct S_IPD p2) { }
EXPORT void f1_V_IDS_IPP(int p0, double p1, struct S_IPP p2) { }
EXPORT void f1_V_IDS_FII(int p0, double p1, struct S_FII p2) { }
EXPORT void f1_V_IDS_FIF(int p0, double p1, struct S_FIF p2) { }
EXPORT void f1_V_IDS_FID(int p0, double p1, struct S_FID p2) { }
EXPORT void f1_V_IDS_FIP(int p0, double p1, struct S_FIP p2) { }
EXPORT void f1_V_IDS_FFI(int p0, double p1, struct S_FFI p2) { }
EXPORT void f1_V_IDS_FFF(int p0, double p1, struct S_FFF p2) { }
EXPORT void f1_V_IDS_FFD(int p0, double p1, struct S_FFD p2) { }
EXPORT void f1_V_IDS_FFP(int p0, double p1, struct S_FFP p2) { }
EXPORT void f1_V_IDS_FDI(int p0, double p1, struct S_FDI p2) { }
EXPORT void f1_V_IDS_FDF(int p0, double p1, struct S_FDF p2) { }
EXPORT void f1_V_IDS_FDD(int p0, double p1, struct S_FDD p2) { }
EXPORT void f1_V_IDS_FDP(int p0, double p1, struct S_FDP p2) { }
EXPORT void f1_V_IDS_FPI(int p0, double p1, struct S_FPI p2) { }
EXPORT void f1_V_IDS_FPF(int p0, double p1, struct S_FPF p2) { }
EXPORT void f1_V_IDS_FPD(int p0, double p1, struct S_FPD p2) { }
EXPORT void f1_V_IDS_FPP(int p0, double p1, struct S_FPP p2) { }
EXPORT void f1_V_IDS_DII(int p0, double p1, struct S_DII p2) { }
EXPORT void f1_V_IDS_DIF(int p0, double p1, struct S_DIF p2) { }
EXPORT void f1_V_IDS_DID(int p0, double p1, struct S_DID p2) { }
EXPORT void f1_V_IDS_DIP(int p0, double p1, struct S_DIP p2) { }
EXPORT void f1_V_IDS_DFI(int p0, double p1, struct S_DFI p2) { }
EXPORT void f1_V_IDS_DFF(int p0, double p1, struct S_DFF p2) { }
EXPORT void f1_V_IDS_DFD(int p0, double p1, struct S_DFD p2) { }
EXPORT void f1_V_IDS_DFP(int p0, double p1, struct S_DFP p2) { }
EXPORT void f1_V_IDS_DDI(int p0, double p1, struct S_DDI p2) { }
EXPORT void f1_V_IDS_DDF(int p0, double p1, struct S_DDF p2) { }
EXPORT void f1_V_IDS_DDD(int p0, double p1, struct S_DDD p2) { }
EXPORT void f1_V_IDS_DDP(int p0, double p1, struct S_DDP p2) { }
EXPORT void f1_V_IDS_DPI(int p0, double p1, struct S_DPI p2) { }
EXPORT void f1_V_IDS_DPF(int p0, double p1, struct S_DPF p2) { }
EXPORT void f1_V_IDS_DPD(int p0, double p1, struct S_DPD p2) { }
EXPORT void f1_V_IDS_DPP(int p0, double p1, struct S_DPP p2) { }
EXPORT void f1_V_IDS_PII(int p0, double p1, struct S_PII p2) { }
EXPORT void f1_V_IDS_PIF(int p0, double p1, struct S_PIF p2) { }
EXPORT void f1_V_IDS_PID(int p0, double p1, struct S_PID p2) { }
EXPORT void f1_V_IDS_PIP(int p0, double p1, struct S_PIP p2) { }
EXPORT void f1_V_IDS_PFI(int p0, double p1, struct S_PFI p2) { }
EXPORT void f1_V_IDS_PFF(int p0, double p1, struct S_PFF p2) { }
EXPORT void f1_V_IDS_PFD(int p0, double p1, struct S_PFD p2) { }
EXPORT void f1_V_IDS_PFP(int p0, double p1, struct S_PFP p2) { }
EXPORT void f1_V_IDS_PDI(int p0, double p1, struct S_PDI p2) { }
EXPORT void f1_V_IDS_PDF(int p0, double p1, struct S_PDF p2) { }
EXPORT void f1_V_IDS_PDD(int p0, double p1, struct S_PDD p2) { }
EXPORT void f1_V_IDS_PDP(int p0, double p1, struct S_PDP p2) { }
EXPORT void f1_V_IDS_PPI(int p0, double p1, struct S_PPI p2) { }
EXPORT void f1_V_IDS_PPF(int p0, double p1, struct S_PPF p2) { }
EXPORT void f1_V_IDS_PPD(int p0, double p1, struct S_PPD p2) { }
EXPORT void f1_V_IDS_PPP(int p0, double p1, struct S_PPP p2) { }
EXPORT void f1_V_IPI_(int p0, void* p1, int p2) { }
EXPORT void f1_V_IPF_(int p0, void* p1, float p2) { }
EXPORT void f1_V_IPD_(int p0, void* p1, double p2) { }
EXPORT void f1_V_IPP_(int p0, void* p1, void* p2) { }
EXPORT void f1_V_IPS_I(int p0, void* p1, struct S_I p2) { }
EXPORT void f1_V_IPS_F(int p0, void* p1, struct S_F p2) { }
EXPORT void f1_V_IPS_D(int p0, void* p1, struct S_D p2) { }
EXPORT void f1_V_IPS_P(int p0, void* p1, struct S_P p2) { }
EXPORT void f1_V_IPS_II(int p0, void* p1, struct S_II p2) { }
EXPORT void f1_V_IPS_IF(int p0, void* p1, struct S_IF p2) { }
EXPORT void f1_V_IPS_ID(int p0, void* p1, struct S_ID p2) { }
EXPORT void f1_V_IPS_IP(int p0, void* p1, struct S_IP p2) { }
EXPORT void f1_V_IPS_FI(int p0, void* p1, struct S_FI p2) { }
EXPORT void f1_V_IPS_FF(int p0, void* p1, struct S_FF p2) { }
EXPORT void f1_V_IPS_FD(int p0, void* p1, struct S_FD p2) { }
EXPORT void f1_V_IPS_FP(int p0, void* p1, struct S_FP p2) { }
EXPORT void f1_V_IPS_DI(int p0, void* p1, struct S_DI p2) { }
EXPORT void f1_V_IPS_DF(int p0, void* p1, struct S_DF p2) { }
EXPORT void f1_V_IPS_DD(int p0, void* p1, struct S_DD p2) { }
EXPORT void f1_V_IPS_DP(int p0, void* p1, struct S_DP p2) { }
EXPORT void f1_V_IPS_PI(int p0, void* p1, struct S_PI p2) { }
EXPORT void f1_V_IPS_PF(int p0, void* p1, struct S_PF p2) { }
EXPORT void f1_V_IPS_PD(int p0, void* p1, struct S_PD p2) { }
EXPORT void f1_V_IPS_PP(int p0, void* p1, struct S_PP p2) { }
EXPORT void f1_V_IPS_III(int p0, void* p1, struct S_III p2) { }
EXPORT void f1_V_IPS_IIF(int p0, void* p1, struct S_IIF p2) { }
EXPORT void f1_V_IPS_IID(int p0, void* p1, struct S_IID p2) { }
EXPORT void f1_V_IPS_IIP(int p0, void* p1, struct S_IIP p2) { }
EXPORT void f1_V_IPS_IFI(int p0, void* p1, struct S_IFI p2) { }
EXPORT void f1_V_IPS_IFF(int p0, void* p1, struct S_IFF p2) { }
EXPORT void f1_V_IPS_IFD(int p0, void* p1, struct S_IFD p2) { }
EXPORT void f1_V_IPS_IFP(int p0, void* p1, struct S_IFP p2) { }
EXPORT void f1_V_IPS_IDI(int p0, void* p1, struct S_IDI p2) { }
EXPORT void f1_V_IPS_IDF(int p0, void* p1, struct S_IDF p2) { }
EXPORT void f1_V_IPS_IDD(int p0, void* p1, struct S_IDD p2) { }
EXPORT void f1_V_IPS_IDP(int p0, void* p1, struct S_IDP p2) { }
EXPORT void f1_V_IPS_IPI(int p0, void* p1, struct S_IPI p2) { }
EXPORT void f1_V_IPS_IPF(int p0, void* p1, struct S_IPF p2) { }
EXPORT void f1_V_IPS_IPD(int p0, void* p1, struct S_IPD p2) { }
EXPORT void f1_V_IPS_IPP(int p0, void* p1, struct S_IPP p2) { }
EXPORT void f1_V_IPS_FII(int p0, void* p1, struct S_FII p2) { }
EXPORT void f1_V_IPS_FIF(int p0, void* p1, struct S_FIF p2) { }
EXPORT void f1_V_IPS_FID(int p0, void* p1, struct S_FID p2) { }
EXPORT void f1_V_IPS_FIP(int p0, void* p1, struct S_FIP p2) { }
EXPORT void f1_V_IPS_FFI(int p0, void* p1, struct S_FFI p2) { }
EXPORT void f1_V_IPS_FFF(int p0, void* p1, struct S_FFF p2) { }
EXPORT void f1_V_IPS_FFD(int p0, void* p1, struct S_FFD p2) { }
EXPORT void f1_V_IPS_FFP(int p0, void* p1, struct S_FFP p2) { }
EXPORT void f1_V_IPS_FDI(int p0, void* p1, struct S_FDI p2) { }
EXPORT void f1_V_IPS_FDF(int p0, void* p1, struct S_FDF p2) { }
EXPORT void f1_V_IPS_FDD(int p0, void* p1, struct S_FDD p2) { }
EXPORT void f1_V_IPS_FDP(int p0, void* p1, struct S_FDP p2) { }
EXPORT void f1_V_IPS_FPI(int p0, void* p1, struct S_FPI p2) { }
EXPORT void f1_V_IPS_FPF(int p0, void* p1, struct S_FPF p2) { }
EXPORT void f1_V_IPS_FPD(int p0, void* p1, struct S_FPD p2) { }
EXPORT void f1_V_IPS_FPP(int p0, void* p1, struct S_FPP p2) { }
EXPORT void f1_V_IPS_DII(int p0, void* p1, struct S_DII p2) { }
EXPORT void f1_V_IPS_DIF(int p0, void* p1, struct S_DIF p2) { }
EXPORT void f1_V_IPS_DID(int p0, void* p1, struct S_DID p2) { }
EXPORT void f1_V_IPS_DIP(int p0, void* p1, struct S_DIP p2) { }
EXPORT void f1_V_IPS_DFI(int p0, void* p1, struct S_DFI p2) { }
EXPORT void f1_V_IPS_DFF(int p0, void* p1, struct S_DFF p2) { }
EXPORT void f1_V_IPS_DFD(int p0, void* p1, struct S_DFD p2) { }
EXPORT void f1_V_IPS_DFP(int p0, void* p1, struct S_DFP p2) { }
EXPORT void f1_V_IPS_DDI(int p0, void* p1, struct S_DDI p2) { }
EXPORT void f1_V_IPS_DDF(int p0, void* p1, struct S_DDF p2) { }
EXPORT void f1_V_IPS_DDD(int p0, void* p1, struct S_DDD p2) { }
EXPORT void f1_V_IPS_DDP(int p0, void* p1, struct S_DDP p2) { }
EXPORT void f1_V_IPS_DPI(int p0, void* p1, struct S_DPI p2) { }
EXPORT void f1_V_IPS_DPF(int p0, void* p1, struct S_DPF p2) { }
EXPORT void f1_V_IPS_DPD(int p0, void* p1, struct S_DPD p2) { }
EXPORT void f1_V_IPS_DPP(int p0, void* p1, struct S_DPP p2) { }
EXPORT void f1_V_IPS_PII(int p0, void* p1, struct S_PII p2) { }
EXPORT void f1_V_IPS_PIF(int p0, void* p1, struct S_PIF p2) { }
EXPORT void f1_V_IPS_PID(int p0, void* p1, struct S_PID p2) { }
EXPORT void f2_V_IPS_PIP(int p0, void* p1, struct S_PIP p2) { }
EXPORT void f2_V_IPS_PFI(int p0, void* p1, struct S_PFI p2) { }
EXPORT void f2_V_IPS_PFF(int p0, void* p1, struct S_PFF p2) { }
EXPORT void f2_V_IPS_PFD(int p0, void* p1, struct S_PFD p2) { }
EXPORT void f2_V_IPS_PFP(int p0, void* p1, struct S_PFP p2) { }
EXPORT void f2_V_IPS_PDI(int p0, void* p1, struct S_PDI p2) { }
EXPORT void f2_V_IPS_PDF(int p0, void* p1, struct S_PDF p2) { }
EXPORT void f2_V_IPS_PDD(int p0, void* p1, struct S_PDD p2) { }
EXPORT void f2_V_IPS_PDP(int p0, void* p1, struct S_PDP p2) { }
EXPORT void f2_V_IPS_PPI(int p0, void* p1, struct S_PPI p2) { }
EXPORT void f2_V_IPS_PPF(int p0, void* p1, struct S_PPF p2) { }
EXPORT void f2_V_IPS_PPD(int p0, void* p1, struct S_PPD p2) { }
EXPORT void f2_V_IPS_PPP(int p0, void* p1, struct S_PPP p2) { }
EXPORT void f2_V_ISI_I(int p0, struct S_I p1, int p2) { }
EXPORT void f2_V_ISI_F(int p0, struct S_F p1, int p2) { }
EXPORT void f2_V_ISI_D(int p0, struct S_D p1, int p2) { }
EXPORT void f2_V_ISI_P(int p0, struct S_P p1, int p2) { }
EXPORT void f2_V_ISI_II(int p0, struct S_II p1, int p2) { }
EXPORT void f2_V_ISI_IF(int p0, struct S_IF p1, int p2) { }
EXPORT void f2_V_ISI_ID(int p0, struct S_ID p1, int p2) { }
EXPORT void f2_V_ISI_IP(int p0, struct S_IP p1, int p2) { }
EXPORT void f2_V_ISI_FI(int p0, struct S_FI p1, int p2) { }
EXPORT void f2_V_ISI_FF(int p0, struct S_FF p1, int p2) { }
EXPORT void f2_V_ISI_FD(int p0, struct S_FD p1, int p2) { }
EXPORT void f2_V_ISI_FP(int p0, struct S_FP p1, int p2) { }
EXPORT void f2_V_ISI_DI(int p0, struct S_DI p1, int p2) { }
EXPORT void f2_V_ISI_DF(int p0, struct S_DF p1, int p2) { }
EXPORT void f2_V_ISI_DD(int p0, struct S_DD p1, int p2) { }
EXPORT void f2_V_ISI_DP(int p0, struct S_DP p1, int p2) { }
EXPORT void f2_V_ISI_PI(int p0, struct S_PI p1, int p2) { }
EXPORT void f2_V_ISI_PF(int p0, struct S_PF p1, int p2) { }
EXPORT void f2_V_ISI_PD(int p0, struct S_PD p1, int p2) { }
EXPORT void f2_V_ISI_PP(int p0, struct S_PP p1, int p2) { }
EXPORT void f2_V_ISI_III(int p0, struct S_III p1, int p2) { }
EXPORT void f2_V_ISI_IIF(int p0, struct S_IIF p1, int p2) { }
EXPORT void f2_V_ISI_IID(int p0, struct S_IID p1, int p2) { }
EXPORT void f2_V_ISI_IIP(int p0, struct S_IIP p1, int p2) { }
EXPORT void f2_V_ISI_IFI(int p0, struct S_IFI p1, int p2) { }
EXPORT void f2_V_ISI_IFF(int p0, struct S_IFF p1, int p2) { }
EXPORT void f2_V_ISI_IFD(int p0, struct S_IFD p1, int p2) { }
EXPORT void f2_V_ISI_IFP(int p0, struct S_IFP p1, int p2) { }
EXPORT void f2_V_ISI_IDI(int p0, struct S_IDI p1, int p2) { }
EXPORT void f2_V_ISI_IDF(int p0, struct S_IDF p1, int p2) { }
EXPORT void f2_V_ISI_IDD(int p0, struct S_IDD p1, int p2) { }
EXPORT void f2_V_ISI_IDP(int p0, struct S_IDP p1, int p2) { }
EXPORT void f2_V_ISI_IPI(int p0, struct S_IPI p1, int p2) { }
EXPORT void f2_V_ISI_IPF(int p0, struct S_IPF p1, int p2) { }
EXPORT void f2_V_ISI_IPD(int p0, struct S_IPD p1, int p2) { }
EXPORT void f2_V_ISI_IPP(int p0, struct S_IPP p1, int p2) { }
EXPORT void f2_V_ISI_FII(int p0, struct S_FII p1, int p2) { }
EXPORT void f2_V_ISI_FIF(int p0, struct S_FIF p1, int p2) { }
EXPORT void f2_V_ISI_FID(int p0, struct S_FID p1, int p2) { }
EXPORT void f2_V_ISI_FIP(int p0, struct S_FIP p1, int p2) { }
EXPORT void f2_V_ISI_FFI(int p0, struct S_FFI p1, int p2) { }
EXPORT void f2_V_ISI_FFF(int p0, struct S_FFF p1, int p2) { }
EXPORT void f2_V_ISI_FFD(int p0, struct S_FFD p1, int p2) { }
EXPORT void f2_V_ISI_FFP(int p0, struct S_FFP p1, int p2) { }
EXPORT void f2_V_ISI_FDI(int p0, struct S_FDI p1, int p2) { }
EXPORT void f2_V_ISI_FDF(int p0, struct S_FDF p1, int p2) { }
EXPORT void f2_V_ISI_FDD(int p0, struct S_FDD p1, int p2) { }
EXPORT void f2_V_ISI_FDP(int p0, struct S_FDP p1, int p2) { }
EXPORT void f2_V_ISI_FPI(int p0, struct S_FPI p1, int p2) { }
EXPORT void f2_V_ISI_FPF(int p0, struct S_FPF p1, int p2) { }
EXPORT void f2_V_ISI_FPD(int p0, struct S_FPD p1, int p2) { }
EXPORT void f2_V_ISI_FPP(int p0, struct S_FPP p1, int p2) { }
EXPORT void f2_V_ISI_DII(int p0, struct S_DII p1, int p2) { }
EXPORT void f2_V_ISI_DIF(int p0, struct S_DIF p1, int p2) { }
EXPORT void f2_V_ISI_DID(int p0, struct S_DID p1, int p2) { }
EXPORT void f2_V_ISI_DIP(int p0, struct S_DIP p1, int p2) { }
EXPORT void f2_V_ISI_DFI(int p0, struct S_DFI p1, int p2) { }
EXPORT void f2_V_ISI_DFF(int p0, struct S_DFF p1, int p2) { }
EXPORT void f2_V_ISI_DFD(int p0, struct S_DFD p1, int p2) { }
EXPORT void f2_V_ISI_DFP(int p0, struct S_DFP p1, int p2) { }
EXPORT void f2_V_ISI_DDI(int p0, struct S_DDI p1, int p2) { }
EXPORT void f2_V_ISI_DDF(int p0, struct S_DDF p1, int p2) { }
EXPORT void f2_V_ISI_DDD(int p0, struct S_DDD p1, int p2) { }
EXPORT void f2_V_ISI_DDP(int p0, struct S_DDP p1, int p2) { }
EXPORT void f2_V_ISI_DPI(int p0, struct S_DPI p1, int p2) { }
EXPORT void f2_V_ISI_DPF(int p0, struct S_DPF p1, int p2) { }
EXPORT void f2_V_ISI_DPD(int p0, struct S_DPD p1, int p2) { }
EXPORT void f2_V_ISI_DPP(int p0, struct S_DPP p1, int p2) { }
EXPORT void f2_V_ISI_PII(int p0, struct S_PII p1, int p2) { }
EXPORT void f2_V_ISI_PIF(int p0, struct S_PIF p1, int p2) { }
EXPORT void f2_V_ISI_PID(int p0, struct S_PID p1, int p2) { }
EXPORT void f2_V_ISI_PIP(int p0, struct S_PIP p1, int p2) { }
EXPORT void f2_V_ISI_PFI(int p0, struct S_PFI p1, int p2) { }
EXPORT void f2_V_ISI_PFF(int p0, struct S_PFF p1, int p2) { }
EXPORT void f2_V_ISI_PFD(int p0, struct S_PFD p1, int p2) { }
EXPORT void f2_V_ISI_PFP(int p0, struct S_PFP p1, int p2) { }
EXPORT void f2_V_ISI_PDI(int p0, struct S_PDI p1, int p2) { }
EXPORT void f2_V_ISI_PDF(int p0, struct S_PDF p1, int p2) { }
EXPORT void f2_V_ISI_PDD(int p0, struct S_PDD p1, int p2) { }
EXPORT void f2_V_ISI_PDP(int p0, struct S_PDP p1, int p2) { }
EXPORT void f2_V_ISI_PPI(int p0, struct S_PPI p1, int p2) { }
EXPORT void f2_V_ISI_PPF(int p0, struct S_PPF p1, int p2) { }
EXPORT void f2_V_ISI_PPD(int p0, struct S_PPD p1, int p2) { }
EXPORT void f2_V_ISI_PPP(int p0, struct S_PPP p1, int p2) { }
EXPORT void f2_V_ISF_I(int p0, struct S_I p1, float p2) { }
EXPORT void f2_V_ISF_F(int p0, struct S_F p1, float p2) { }
EXPORT void f2_V_ISF_D(int p0, struct S_D p1, float p2) { }
EXPORT void f2_V_ISF_P(int p0, struct S_P p1, float p2) { }
EXPORT void f2_V_ISF_II(int p0, struct S_II p1, float p2) { }
EXPORT void f2_V_ISF_IF(int p0, struct S_IF p1, float p2) { }
EXPORT void f2_V_ISF_ID(int p0, struct S_ID p1, float p2) { }
EXPORT void f2_V_ISF_IP(int p0, struct S_IP p1, float p2) { }
EXPORT void f2_V_ISF_FI(int p0, struct S_FI p1, float p2) { }
EXPORT void f2_V_ISF_FF(int p0, struct S_FF p1, float p2) { }
EXPORT void f2_V_ISF_FD(int p0, struct S_FD p1, float p2) { }
EXPORT void f2_V_ISF_FP(int p0, struct S_FP p1, float p2) { }
EXPORT void f2_V_ISF_DI(int p0, struct S_DI p1, float p2) { }
EXPORT void f2_V_ISF_DF(int p0, struct S_DF p1, float p2) { }
EXPORT void f2_V_ISF_DD(int p0, struct S_DD p1, float p2) { }
EXPORT void f2_V_ISF_DP(int p0, struct S_DP p1, float p2) { }
EXPORT void f2_V_ISF_PI(int p0, struct S_PI p1, float p2) { }
EXPORT void f2_V_ISF_PF(int p0, struct S_PF p1, float p2) { }
EXPORT void f2_V_ISF_PD(int p0, struct S_PD p1, float p2) { }
EXPORT void f2_V_ISF_PP(int p0, struct S_PP p1, float p2) { }
EXPORT void f2_V_ISF_III(int p0, struct S_III p1, float p2) { }
EXPORT void f2_V_ISF_IIF(int p0, struct S_IIF p1, float p2) { }
EXPORT void f2_V_ISF_IID(int p0, struct S_IID p1, float p2) { }
EXPORT void f2_V_ISF_IIP(int p0, struct S_IIP p1, float p2) { }
EXPORT void f2_V_ISF_IFI(int p0, struct S_IFI p1, float p2) { }
EXPORT void f2_V_ISF_IFF(int p0, struct S_IFF p1, float p2) { }
EXPORT void f2_V_ISF_IFD(int p0, struct S_IFD p1, float p2) { }
EXPORT void f2_V_ISF_IFP(int p0, struct S_IFP p1, float p2) { }
EXPORT void f2_V_ISF_IDI(int p0, struct S_IDI p1, float p2) { }
EXPORT void f2_V_ISF_IDF(int p0, struct S_IDF p1, float p2) { }
EXPORT void f2_V_ISF_IDD(int p0, struct S_IDD p1, float p2) { }
EXPORT void f2_V_ISF_IDP(int p0, struct S_IDP p1, float p2) { }
EXPORT void f2_V_ISF_IPI(int p0, struct S_IPI p1, float p2) { }
EXPORT void f2_V_ISF_IPF(int p0, struct S_IPF p1, float p2) { }
EXPORT void f2_V_ISF_IPD(int p0, struct S_IPD p1, float p2) { }
EXPORT void f2_V_ISF_IPP(int p0, struct S_IPP p1, float p2) { }
EXPORT void f2_V_ISF_FII(int p0, struct S_FII p1, float p2) { }
EXPORT void f2_V_ISF_FIF(int p0, struct S_FIF p1, float p2) { }
EXPORT void f2_V_ISF_FID(int p0, struct S_FID p1, float p2) { }
EXPORT void f2_V_ISF_FIP(int p0, struct S_FIP p1, float p2) { }
EXPORT void f2_V_ISF_FFI(int p0, struct S_FFI p1, float p2) { }
EXPORT void f2_V_ISF_FFF(int p0, struct S_FFF p1, float p2) { }
EXPORT void f2_V_ISF_FFD(int p0, struct S_FFD p1, float p2) { }
EXPORT void f2_V_ISF_FFP(int p0, struct S_FFP p1, float p2) { }
EXPORT void f2_V_ISF_FDI(int p0, struct S_FDI p1, float p2) { }
EXPORT void f2_V_ISF_FDF(int p0, struct S_FDF p1, float p2) { }
EXPORT void f2_V_ISF_FDD(int p0, struct S_FDD p1, float p2) { }
EXPORT void f2_V_ISF_FDP(int p0, struct S_FDP p1, float p2) { }
EXPORT void f2_V_ISF_FPI(int p0, struct S_FPI p1, float p2) { }
EXPORT void f2_V_ISF_FPF(int p0, struct S_FPF p1, float p2) { }
EXPORT void f2_V_ISF_FPD(int p0, struct S_FPD p1, float p2) { }
EXPORT void f2_V_ISF_FPP(int p0, struct S_FPP p1, float p2) { }
EXPORT void f2_V_ISF_DII(int p0, struct S_DII p1, float p2) { }
EXPORT void f2_V_ISF_DIF(int p0, struct S_DIF p1, float p2) { }
EXPORT void f2_V_ISF_DID(int p0, struct S_DID p1, float p2) { }
EXPORT void f2_V_ISF_DIP(int p0, struct S_DIP p1, float p2) { }
EXPORT void f2_V_ISF_DFI(int p0, struct S_DFI p1, float p2) { }
EXPORT void f2_V_ISF_DFF(int p0, struct S_DFF p1, float p2) { }
EXPORT void f2_V_ISF_DFD(int p0, struct S_DFD p1, float p2) { }
EXPORT void f2_V_ISF_DFP(int p0, struct S_DFP p1, float p2) { }
EXPORT void f2_V_ISF_DDI(int p0, struct S_DDI p1, float p2) { }
EXPORT void f2_V_ISF_DDF(int p0, struct S_DDF p1, float p2) { }
EXPORT void f2_V_ISF_DDD(int p0, struct S_DDD p1, float p2) { }
EXPORT void f2_V_ISF_DDP(int p0, struct S_DDP p1, float p2) { }
EXPORT void f2_V_ISF_DPI(int p0, struct S_DPI p1, float p2) { }
EXPORT void f2_V_ISF_DPF(int p0, struct S_DPF p1, float p2) { }
EXPORT void f2_V_ISF_DPD(int p0, struct S_DPD p1, float p2) { }
EXPORT void f2_V_ISF_DPP(int p0, struct S_DPP p1, float p2) { }
EXPORT void f2_V_ISF_PII(int p0, struct S_PII p1, float p2) { }
EXPORT void f2_V_ISF_PIF(int p0, struct S_PIF p1, float p2) { }
EXPORT void f2_V_ISF_PID(int p0, struct S_PID p1, float p2) { }
EXPORT void f2_V_ISF_PIP(int p0, struct S_PIP p1, float p2) { }
EXPORT void f2_V_ISF_PFI(int p0, struct S_PFI p1, float p2) { }
EXPORT void f2_V_ISF_PFF(int p0, struct S_PFF p1, float p2) { }
EXPORT void f2_V_ISF_PFD(int p0, struct S_PFD p1, float p2) { }
EXPORT void f2_V_ISF_PFP(int p0, struct S_PFP p1, float p2) { }
EXPORT void f2_V_ISF_PDI(int p0, struct S_PDI p1, float p2) { }
EXPORT void f2_V_ISF_PDF(int p0, struct S_PDF p1, float p2) { }
EXPORT void f2_V_ISF_PDD(int p0, struct S_PDD p1, float p2) { }
EXPORT void f2_V_ISF_PDP(int p0, struct S_PDP p1, float p2) { }
EXPORT void f2_V_ISF_PPI(int p0, struct S_PPI p1, float p2) { }
EXPORT void f2_V_ISF_PPF(int p0, struct S_PPF p1, float p2) { }
EXPORT void f2_V_ISF_PPD(int p0, struct S_PPD p1, float p2) { }
EXPORT void f2_V_ISF_PPP(int p0, struct S_PPP p1, float p2) { }
EXPORT void f2_V_ISD_I(int p0, struct S_I p1, double p2) { }
EXPORT void f2_V_ISD_F(int p0, struct S_F p1, double p2) { }
EXPORT void f2_V_ISD_D(int p0, struct S_D p1, double p2) { }
EXPORT void f2_V_ISD_P(int p0, struct S_P p1, double p2) { }
EXPORT void f2_V_ISD_II(int p0, struct S_II p1, double p2) { }
EXPORT void f2_V_ISD_IF(int p0, struct S_IF p1, double p2) { }
EXPORT void f2_V_ISD_ID(int p0, struct S_ID p1, double p2) { }
EXPORT void f2_V_ISD_IP(int p0, struct S_IP p1, double p2) { }
EXPORT void f2_V_ISD_FI(int p0, struct S_FI p1, double p2) { }
EXPORT void f2_V_ISD_FF(int p0, struct S_FF p1, double p2) { }
EXPORT void f2_V_ISD_FD(int p0, struct S_FD p1, double p2) { }
EXPORT void f2_V_ISD_FP(int p0, struct S_FP p1, double p2) { }
EXPORT void f2_V_ISD_DI(int p0, struct S_DI p1, double p2) { }
EXPORT void f2_V_ISD_DF(int p0, struct S_DF p1, double p2) { }
EXPORT void f2_V_ISD_DD(int p0, struct S_DD p1, double p2) { }
EXPORT void f2_V_ISD_DP(int p0, struct S_DP p1, double p2) { }
EXPORT void f2_V_ISD_PI(int p0, struct S_PI p1, double p2) { }
EXPORT void f2_V_ISD_PF(int p0, struct S_PF p1, double p2) { }
EXPORT void f2_V_ISD_PD(int p0, struct S_PD p1, double p2) { }
EXPORT void f2_V_ISD_PP(int p0, struct S_PP p1, double p2) { }
EXPORT void f2_V_ISD_III(int p0, struct S_III p1, double p2) { }
EXPORT void f2_V_ISD_IIF(int p0, struct S_IIF p1, double p2) { }
EXPORT void f2_V_ISD_IID(int p0, struct S_IID p1, double p2) { }
EXPORT void f2_V_ISD_IIP(int p0, struct S_IIP p1, double p2) { }
EXPORT void f2_V_ISD_IFI(int p0, struct S_IFI p1, double p2) { }
EXPORT void f2_V_ISD_IFF(int p0, struct S_IFF p1, double p2) { }
EXPORT void f2_V_ISD_IFD(int p0, struct S_IFD p1, double p2) { }
EXPORT void f2_V_ISD_IFP(int p0, struct S_IFP p1, double p2) { }
EXPORT void f2_V_ISD_IDI(int p0, struct S_IDI p1, double p2) { }
EXPORT void f2_V_ISD_IDF(int p0, struct S_IDF p1, double p2) { }
EXPORT void f2_V_ISD_IDD(int p0, struct S_IDD p1, double p2) { }
EXPORT void f2_V_ISD_IDP(int p0, struct S_IDP p1, double p2) { }
EXPORT void f2_V_ISD_IPI(int p0, struct S_IPI p1, double p2) { }
EXPORT void f2_V_ISD_IPF(int p0, struct S_IPF p1, double p2) { }
EXPORT void f2_V_ISD_IPD(int p0, struct S_IPD p1, double p2) { }
EXPORT void f2_V_ISD_IPP(int p0, struct S_IPP p1, double p2) { }
EXPORT void f2_V_ISD_FII(int p0, struct S_FII p1, double p2) { }
EXPORT void f2_V_ISD_FIF(int p0, struct S_FIF p1, double p2) { }
EXPORT void f2_V_ISD_FID(int p0, struct S_FID p1, double p2) { }
EXPORT void f2_V_ISD_FIP(int p0, struct S_FIP p1, double p2) { }
EXPORT void f2_V_ISD_FFI(int p0, struct S_FFI p1, double p2) { }
EXPORT void f2_V_ISD_FFF(int p0, struct S_FFF p1, double p2) { }
EXPORT void f2_V_ISD_FFD(int p0, struct S_FFD p1, double p2) { }
EXPORT void f2_V_ISD_FFP(int p0, struct S_FFP p1, double p2) { }
EXPORT void f2_V_ISD_FDI(int p0, struct S_FDI p1, double p2) { }
EXPORT void f2_V_ISD_FDF(int p0, struct S_FDF p1, double p2) { }
EXPORT void f2_V_ISD_FDD(int p0, struct S_FDD p1, double p2) { }
EXPORT void f2_V_ISD_FDP(int p0, struct S_FDP p1, double p2) { }
EXPORT void f2_V_ISD_FPI(int p0, struct S_FPI p1, double p2) { }
EXPORT void f2_V_ISD_FPF(int p0, struct S_FPF p1, double p2) { }
EXPORT void f2_V_ISD_FPD(int p0, struct S_FPD p1, double p2) { }
EXPORT void f2_V_ISD_FPP(int p0, struct S_FPP p1, double p2) { }
EXPORT void f2_V_ISD_DII(int p0, struct S_DII p1, double p2) { }
EXPORT void f2_V_ISD_DIF(int p0, struct S_DIF p1, double p2) { }
EXPORT void f2_V_ISD_DID(int p0, struct S_DID p1, double p2) { }
EXPORT void f2_V_ISD_DIP(int p0, struct S_DIP p1, double p2) { }
EXPORT void f2_V_ISD_DFI(int p0, struct S_DFI p1, double p2) { }
EXPORT void f2_V_ISD_DFF(int p0, struct S_DFF p1, double p2) { }
EXPORT void f2_V_ISD_DFD(int p0, struct S_DFD p1, double p2) { }
EXPORT void f2_V_ISD_DFP(int p0, struct S_DFP p1, double p2) { }
EXPORT void f2_V_ISD_DDI(int p0, struct S_DDI p1, double p2) { }
EXPORT void f2_V_ISD_DDF(int p0, struct S_DDF p1, double p2) { }
EXPORT void f2_V_ISD_DDD(int p0, struct S_DDD p1, double p2) { }
EXPORT void f2_V_ISD_DDP(int p0, struct S_DDP p1, double p2) { }
EXPORT void f2_V_ISD_DPI(int p0, struct S_DPI p1, double p2) { }
EXPORT void f2_V_ISD_DPF(int p0, struct S_DPF p1, double p2) { }
EXPORT void f2_V_ISD_DPD(int p0, struct S_DPD p1, double p2) { }
EXPORT void f2_V_ISD_DPP(int p0, struct S_DPP p1, double p2) { }
EXPORT void f2_V_ISD_PII(int p0, struct S_PII p1, double p2) { }
EXPORT void f2_V_ISD_PIF(int p0, struct S_PIF p1, double p2) { }
EXPORT void f2_V_ISD_PID(int p0, struct S_PID p1, double p2) { }
EXPORT void f2_V_ISD_PIP(int p0, struct S_PIP p1, double p2) { }
EXPORT void f2_V_ISD_PFI(int p0, struct S_PFI p1, double p2) { }
EXPORT void f2_V_ISD_PFF(int p0, struct S_PFF p1, double p2) { }
EXPORT void f2_V_ISD_PFD(int p0, struct S_PFD p1, double p2) { }
EXPORT void f2_V_ISD_PFP(int p0, struct S_PFP p1, double p2) { }
EXPORT void f2_V_ISD_PDI(int p0, struct S_PDI p1, double p2) { }
EXPORT void f2_V_ISD_PDF(int p0, struct S_PDF p1, double p2) { }
EXPORT void f2_V_ISD_PDD(int p0, struct S_PDD p1, double p2) { }
EXPORT void f2_V_ISD_PDP(int p0, struct S_PDP p1, double p2) { }
EXPORT void f2_V_ISD_PPI(int p0, struct S_PPI p1, double p2) { }
EXPORT void f2_V_ISD_PPF(int p0, struct S_PPF p1, double p2) { }
EXPORT void f2_V_ISD_PPD(int p0, struct S_PPD p1, double p2) { }
EXPORT void f2_V_ISD_PPP(int p0, struct S_PPP p1, double p2) { }
EXPORT void f2_V_ISP_I(int p0, struct S_I p1, void* p2) { }
EXPORT void f2_V_ISP_F(int p0, struct S_F p1, void* p2) { }
EXPORT void f2_V_ISP_D(int p0, struct S_D p1, void* p2) { }
EXPORT void f2_V_ISP_P(int p0, struct S_P p1, void* p2) { }
EXPORT void f2_V_ISP_II(int p0, struct S_II p1, void* p2) { }
EXPORT void f2_V_ISP_IF(int p0, struct S_IF p1, void* p2) { }
EXPORT void f2_V_ISP_ID(int p0, struct S_ID p1, void* p2) { }
EXPORT void f2_V_ISP_IP(int p0, struct S_IP p1, void* p2) { }
EXPORT void f2_V_ISP_FI(int p0, struct S_FI p1, void* p2) { }
EXPORT void f2_V_ISP_FF(int p0, struct S_FF p1, void* p2) { }
EXPORT void f2_V_ISP_FD(int p0, struct S_FD p1, void* p2) { }
EXPORT void f2_V_ISP_FP(int p0, struct S_FP p1, void* p2) { }
EXPORT void f2_V_ISP_DI(int p0, struct S_DI p1, void* p2) { }
EXPORT void f2_V_ISP_DF(int p0, struct S_DF p1, void* p2) { }
EXPORT void f2_V_ISP_DD(int p0, struct S_DD p1, void* p2) { }
EXPORT void f2_V_ISP_DP(int p0, struct S_DP p1, void* p2) { }
EXPORT void f2_V_ISP_PI(int p0, struct S_PI p1, void* p2) { }
EXPORT void f2_V_ISP_PF(int p0, struct S_PF p1, void* p2) { }
EXPORT void f2_V_ISP_PD(int p0, struct S_PD p1, void* p2) { }
EXPORT void f2_V_ISP_PP(int p0, struct S_PP p1, void* p2) { }
EXPORT void f2_V_ISP_III(int p0, struct S_III p1, void* p2) { }
EXPORT void f2_V_ISP_IIF(int p0, struct S_IIF p1, void* p2) { }
EXPORT void f2_V_ISP_IID(int p0, struct S_IID p1, void* p2) { }
EXPORT void f2_V_ISP_IIP(int p0, struct S_IIP p1, void* p2) { }
EXPORT void f2_V_ISP_IFI(int p0, struct S_IFI p1, void* p2) { }
EXPORT void f2_V_ISP_IFF(int p0, struct S_IFF p1, void* p2) { }
EXPORT void f2_V_ISP_IFD(int p0, struct S_IFD p1, void* p2) { }
EXPORT void f2_V_ISP_IFP(int p0, struct S_IFP p1, void* p2) { }
EXPORT void f2_V_ISP_IDI(int p0, struct S_IDI p1, void* p2) { }
EXPORT void f2_V_ISP_IDF(int p0, struct S_IDF p1, void* p2) { }
EXPORT void f2_V_ISP_IDD(int p0, struct S_IDD p1, void* p2) { }
EXPORT void f2_V_ISP_IDP(int p0, struct S_IDP p1, void* p2) { }
EXPORT void f2_V_ISP_IPI(int p0, struct S_IPI p1, void* p2) { }
EXPORT void f2_V_ISP_IPF(int p0, struct S_IPF p1, void* p2) { }
EXPORT void f2_V_ISP_IPD(int p0, struct S_IPD p1, void* p2) { }
EXPORT void f2_V_ISP_IPP(int p0, struct S_IPP p1, void* p2) { }
EXPORT void f2_V_ISP_FII(int p0, struct S_FII p1, void* p2) { }
EXPORT void f2_V_ISP_FIF(int p0, struct S_FIF p1, void* p2) { }
EXPORT void f2_V_ISP_FID(int p0, struct S_FID p1, void* p2) { }
EXPORT void f2_V_ISP_FIP(int p0, struct S_FIP p1, void* p2) { }
EXPORT void f2_V_ISP_FFI(int p0, struct S_FFI p1, void* p2) { }
EXPORT void f2_V_ISP_FFF(int p0, struct S_FFF p1, void* p2) { }
EXPORT void f2_V_ISP_FFD(int p0, struct S_FFD p1, void* p2) { }
EXPORT void f2_V_ISP_FFP(int p0, struct S_FFP p1, void* p2) { }
EXPORT void f2_V_ISP_FDI(int p0, struct S_FDI p1, void* p2) { }
EXPORT void f2_V_ISP_FDF(int p0, struct S_FDF p1, void* p2) { }
EXPORT void f2_V_ISP_FDD(int p0, struct S_FDD p1, void* p2) { }
EXPORT void f2_V_ISP_FDP(int p0, struct S_FDP p1, void* p2) { }
EXPORT void f2_V_ISP_FPI(int p0, struct S_FPI p1, void* p2) { }
EXPORT void f2_V_ISP_FPF(int p0, struct S_FPF p1, void* p2) { }
EXPORT void f2_V_ISP_FPD(int p0, struct S_FPD p1, void* p2) { }
EXPORT void f2_V_ISP_FPP(int p0, struct S_FPP p1, void* p2) { }
EXPORT void f2_V_ISP_DII(int p0, struct S_DII p1, void* p2) { }
EXPORT void f2_V_ISP_DIF(int p0, struct S_DIF p1, void* p2) { }
EXPORT void f2_V_ISP_DID(int p0, struct S_DID p1, void* p2) { }
EXPORT void f2_V_ISP_DIP(int p0, struct S_DIP p1, void* p2) { }
EXPORT void f2_V_ISP_DFI(int p0, struct S_DFI p1, void* p2) { }
EXPORT void f2_V_ISP_DFF(int p0, struct S_DFF p1, void* p2) { }
EXPORT void f2_V_ISP_DFD(int p0, struct S_DFD p1, void* p2) { }
EXPORT void f2_V_ISP_DFP(int p0, struct S_DFP p1, void* p2) { }
EXPORT void f2_V_ISP_DDI(int p0, struct S_DDI p1, void* p2) { }
EXPORT void f2_V_ISP_DDF(int p0, struct S_DDF p1, void* p2) { }
EXPORT void f2_V_ISP_DDD(int p0, struct S_DDD p1, void* p2) { }
EXPORT void f2_V_ISP_DDP(int p0, struct S_DDP p1, void* p2) { }
EXPORT void f2_V_ISP_DPI(int p0, struct S_DPI p1, void* p2) { }
EXPORT void f2_V_ISP_DPF(int p0, struct S_DPF p1, void* p2) { }
EXPORT void f2_V_ISP_DPD(int p0, struct S_DPD p1, void* p2) { }
EXPORT void f2_V_ISP_DPP(int p0, struct S_DPP p1, void* p2) { }
EXPORT void f2_V_ISP_PII(int p0, struct S_PII p1, void* p2) { }
EXPORT void f2_V_ISP_PIF(int p0, struct S_PIF p1, void* p2) { }
EXPORT void f2_V_ISP_PID(int p0, struct S_PID p1, void* p2) { }
EXPORT void f2_V_ISP_PIP(int p0, struct S_PIP p1, void* p2) { }
EXPORT void f2_V_ISP_PFI(int p0, struct S_PFI p1, void* p2) { }
EXPORT void f2_V_ISP_PFF(int p0, struct S_PFF p1, void* p2) { }
EXPORT void f2_V_ISP_PFD(int p0, struct S_PFD p1, void* p2) { }
EXPORT void f2_V_ISP_PFP(int p0, struct S_PFP p1, void* p2) { }
EXPORT void f2_V_ISP_PDI(int p0, struct S_PDI p1, void* p2) { }
EXPORT void f2_V_ISP_PDF(int p0, struct S_PDF p1, void* p2) { }
EXPORT void f2_V_ISP_PDD(int p0, struct S_PDD p1, void* p2) { }
EXPORT void f2_V_ISP_PDP(int p0, struct S_PDP p1, void* p2) { }
EXPORT void f2_V_ISP_PPI(int p0, struct S_PPI p1, void* p2) { }
EXPORT void f2_V_ISP_PPF(int p0, struct S_PPF p1, void* p2) { }
EXPORT void f2_V_ISP_PPD(int p0, struct S_PPD p1, void* p2) { }
EXPORT void f2_V_ISP_PPP(int p0, struct S_PPP p1, void* p2) { }
EXPORT void f2_V_ISS_I(int p0, struct S_I p1, struct S_I p2) { }
EXPORT void f2_V_ISS_F(int p0, struct S_F p1, struct S_F p2) { }
EXPORT void f2_V_ISS_D(int p0, struct S_D p1, struct S_D p2) { }
EXPORT void f2_V_ISS_P(int p0, struct S_P p1, struct S_P p2) { }
EXPORT void f2_V_ISS_II(int p0, struct S_II p1, struct S_II p2) { }
EXPORT void f2_V_ISS_IF(int p0, struct S_IF p1, struct S_IF p2) { }
EXPORT void f2_V_ISS_ID(int p0, struct S_ID p1, struct S_ID p2) { }
EXPORT void f2_V_ISS_IP(int p0, struct S_IP p1, struct S_IP p2) { }
EXPORT void f2_V_ISS_FI(int p0, struct S_FI p1, struct S_FI p2) { }
EXPORT void f2_V_ISS_FF(int p0, struct S_FF p1, struct S_FF p2) { }
EXPORT void f2_V_ISS_FD(int p0, struct S_FD p1, struct S_FD p2) { }
EXPORT void f2_V_ISS_FP(int p0, struct S_FP p1, struct S_FP p2) { }
EXPORT void f2_V_ISS_DI(int p0, struct S_DI p1, struct S_DI p2) { }
EXPORT void f2_V_ISS_DF(int p0, struct S_DF p1, struct S_DF p2) { }
EXPORT void f2_V_ISS_DD(int p0, struct S_DD p1, struct S_DD p2) { }
EXPORT void f2_V_ISS_DP(int p0, struct S_DP p1, struct S_DP p2) { }
EXPORT void f2_V_ISS_PI(int p0, struct S_PI p1, struct S_PI p2) { }
EXPORT void f2_V_ISS_PF(int p0, struct S_PF p1, struct S_PF p2) { }
EXPORT void f2_V_ISS_PD(int p0, struct S_PD p1, struct S_PD p2) { }
EXPORT void f2_V_ISS_PP(int p0, struct S_PP p1, struct S_PP p2) { }
EXPORT void f2_V_ISS_III(int p0, struct S_III p1, struct S_III p2) { }
EXPORT void f2_V_ISS_IIF(int p0, struct S_IIF p1, struct S_IIF p2) { }
EXPORT void f2_V_ISS_IID(int p0, struct S_IID p1, struct S_IID p2) { }
EXPORT void f2_V_ISS_IIP(int p0, struct S_IIP p1, struct S_IIP p2) { }
EXPORT void f2_V_ISS_IFI(int p0, struct S_IFI p1, struct S_IFI p2) { }
EXPORT void f2_V_ISS_IFF(int p0, struct S_IFF p1, struct S_IFF p2) { }
EXPORT void f2_V_ISS_IFD(int p0, struct S_IFD p1, struct S_IFD p2) { }
EXPORT void f2_V_ISS_IFP(int p0, struct S_IFP p1, struct S_IFP p2) { }
EXPORT void f2_V_ISS_IDI(int p0, struct S_IDI p1, struct S_IDI p2) { }
EXPORT void f2_V_ISS_IDF(int p0, struct S_IDF p1, struct S_IDF p2) { }
EXPORT void f2_V_ISS_IDD(int p0, struct S_IDD p1, struct S_IDD p2) { }
EXPORT void f2_V_ISS_IDP(int p0, struct S_IDP p1, struct S_IDP p2) { }
EXPORT void f2_V_ISS_IPI(int p0, struct S_IPI p1, struct S_IPI p2) { }
EXPORT void f2_V_ISS_IPF(int p0, struct S_IPF p1, struct S_IPF p2) { }
EXPORT void f2_V_ISS_IPD(int p0, struct S_IPD p1, struct S_IPD p2) { }
EXPORT void f2_V_ISS_IPP(int p0, struct S_IPP p1, struct S_IPP p2) { }
EXPORT void f2_V_ISS_FII(int p0, struct S_FII p1, struct S_FII p2) { }
EXPORT void f2_V_ISS_FIF(int p0, struct S_FIF p1, struct S_FIF p2) { }
EXPORT void f2_V_ISS_FID(int p0, struct S_FID p1, struct S_FID p2) { }
EXPORT void f2_V_ISS_FIP(int p0, struct S_FIP p1, struct S_FIP p2) { }
EXPORT void f2_V_ISS_FFI(int p0, struct S_FFI p1, struct S_FFI p2) { }
EXPORT void f2_V_ISS_FFF(int p0, struct S_FFF p1, struct S_FFF p2) { }
EXPORT void f2_V_ISS_FFD(int p0, struct S_FFD p1, struct S_FFD p2) { }
EXPORT void f2_V_ISS_FFP(int p0, struct S_FFP p1, struct S_FFP p2) { }
EXPORT void f2_V_ISS_FDI(int p0, struct S_FDI p1, struct S_FDI p2) { }
EXPORT void f2_V_ISS_FDF(int p0, struct S_FDF p1, struct S_FDF p2) { }
EXPORT void f2_V_ISS_FDD(int p0, struct S_FDD p1, struct S_FDD p2) { }
EXPORT void f2_V_ISS_FDP(int p0, struct S_FDP p1, struct S_FDP p2) { }
EXPORT void f2_V_ISS_FPI(int p0, struct S_FPI p1, struct S_FPI p2) { }
EXPORT void f2_V_ISS_FPF(int p0, struct S_FPF p1, struct S_FPF p2) { }
EXPORT void f2_V_ISS_FPD(int p0, struct S_FPD p1, struct S_FPD p2) { }
EXPORT void f2_V_ISS_FPP(int p0, struct S_FPP p1, struct S_FPP p2) { }
EXPORT void f2_V_ISS_DII(int p0, struct S_DII p1, struct S_DII p2) { }
EXPORT void f2_V_ISS_DIF(int p0, struct S_DIF p1, struct S_DIF p2) { }
EXPORT void f2_V_ISS_DID(int p0, struct S_DID p1, struct S_DID p2) { }
EXPORT void f2_V_ISS_DIP(int p0, struct S_DIP p1, struct S_DIP p2) { }
EXPORT void f2_V_ISS_DFI(int p0, struct S_DFI p1, struct S_DFI p2) { }
EXPORT void f2_V_ISS_DFF(int p0, struct S_DFF p1, struct S_DFF p2) { }
EXPORT void f2_V_ISS_DFD(int p0, struct S_DFD p1, struct S_DFD p2) { }
EXPORT void f2_V_ISS_DFP(int p0, struct S_DFP p1, struct S_DFP p2) { }
EXPORT void f2_V_ISS_DDI(int p0, struct S_DDI p1, struct S_DDI p2) { }
EXPORT void f2_V_ISS_DDF(int p0, struct S_DDF p1, struct S_DDF p2) { }
EXPORT void f2_V_ISS_DDD(int p0, struct S_DDD p1, struct S_DDD p2) { }
EXPORT void f2_V_ISS_DDP(int p0, struct S_DDP p1, struct S_DDP p2) { }
EXPORT void f2_V_ISS_DPI(int p0, struct S_DPI p1, struct S_DPI p2) { }
EXPORT void f2_V_ISS_DPF(int p0, struct S_DPF p1, struct S_DPF p2) { }
EXPORT void f2_V_ISS_DPD(int p0, struct S_DPD p1, struct S_DPD p2) { }
EXPORT void f2_V_ISS_DPP(int p0, struct S_DPP p1, struct S_DPP p2) { }
EXPORT void f2_V_ISS_PII(int p0, struct S_PII p1, struct S_PII p2) { }
EXPORT void f2_V_ISS_PIF(int p0, struct S_PIF p1, struct S_PIF p2) { }
EXPORT void f2_V_ISS_PID(int p0, struct S_PID p1, struct S_PID p2) { }
EXPORT void f2_V_ISS_PIP(int p0, struct S_PIP p1, struct S_PIP p2) { }
EXPORT void f2_V_ISS_PFI(int p0, struct S_PFI p1, struct S_PFI p2) { }
EXPORT void f2_V_ISS_PFF(int p0, struct S_PFF p1, struct S_PFF p2) { }
EXPORT void f2_V_ISS_PFD(int p0, struct S_PFD p1, struct S_PFD p2) { }
EXPORT void f2_V_ISS_PFP(int p0, struct S_PFP p1, struct S_PFP p2) { }
EXPORT void f2_V_ISS_PDI(int p0, struct S_PDI p1, struct S_PDI p2) { }
EXPORT void f2_V_ISS_PDF(int p0, struct S_PDF p1, struct S_PDF p2) { }
EXPORT void f2_V_ISS_PDD(int p0, struct S_PDD p1, struct S_PDD p2) { }
EXPORT void f2_V_ISS_PDP(int p0, struct S_PDP p1, struct S_PDP p2) { }
EXPORT void f2_V_ISS_PPI(int p0, struct S_PPI p1, struct S_PPI p2) { }
EXPORT void f2_V_ISS_PPF(int p0, struct S_PPF p1, struct S_PPF p2) { }
EXPORT void f2_V_ISS_PPD(int p0, struct S_PPD p1, struct S_PPD p2) { }
EXPORT void f2_V_ISS_PPP(int p0, struct S_PPP p1, struct S_PPP p2) { }
EXPORT void f2_V_FII_(float p0, int p1, int p2) { }
EXPORT void f2_V_FIF_(float p0, int p1, float p2) { }
EXPORT void f2_V_FID_(float p0, int p1, double p2) { }
EXPORT void f2_V_FIP_(float p0, int p1, void* p2) { }
EXPORT void f2_V_FIS_I(float p0, int p1, struct S_I p2) { }
EXPORT void f2_V_FIS_F(float p0, int p1, struct S_F p2) { }
EXPORT void f2_V_FIS_D(float p0, int p1, struct S_D p2) { }
EXPORT void f2_V_FIS_P(float p0, int p1, struct S_P p2) { }
EXPORT void f2_V_FIS_II(float p0, int p1, struct S_II p2) { }
EXPORT void f2_V_FIS_IF(float p0, int p1, struct S_IF p2) { }
EXPORT void f2_V_FIS_ID(float p0, int p1, struct S_ID p2) { }
EXPORT void f2_V_FIS_IP(float p0, int p1, struct S_IP p2) { }
EXPORT void f2_V_FIS_FI(float p0, int p1, struct S_FI p2) { }
EXPORT void f2_V_FIS_FF(float p0, int p1, struct S_FF p2) { }
EXPORT void f2_V_FIS_FD(float p0, int p1, struct S_FD p2) { }
EXPORT void f2_V_FIS_FP(float p0, int p1, struct S_FP p2) { }
EXPORT void f2_V_FIS_DI(float p0, int p1, struct S_DI p2) { }
EXPORT void f2_V_FIS_DF(float p0, int p1, struct S_DF p2) { }
EXPORT void f2_V_FIS_DD(float p0, int p1, struct S_DD p2) { }
EXPORT void f2_V_FIS_DP(float p0, int p1, struct S_DP p2) { }
EXPORT void f2_V_FIS_PI(float p0, int p1, struct S_PI p2) { }
EXPORT void f2_V_FIS_PF(float p0, int p1, struct S_PF p2) { }
EXPORT void f2_V_FIS_PD(float p0, int p1, struct S_PD p2) { }
EXPORT void f2_V_FIS_PP(float p0, int p1, struct S_PP p2) { }
EXPORT void f2_V_FIS_III(float p0, int p1, struct S_III p2) { }
EXPORT void f2_V_FIS_IIF(float p0, int p1, struct S_IIF p2) { }
EXPORT void f2_V_FIS_IID(float p0, int p1, struct S_IID p2) { }
EXPORT void f2_V_FIS_IIP(float p0, int p1, struct S_IIP p2) { }
EXPORT void f2_V_FIS_IFI(float p0, int p1, struct S_IFI p2) { }
EXPORT void f2_V_FIS_IFF(float p0, int p1, struct S_IFF p2) { }
EXPORT void f2_V_FIS_IFD(float p0, int p1, struct S_IFD p2) { }
EXPORT void f2_V_FIS_IFP(float p0, int p1, struct S_IFP p2) { }
EXPORT void f2_V_FIS_IDI(float p0, int p1, struct S_IDI p2) { }
EXPORT void f2_V_FIS_IDF(float p0, int p1, struct S_IDF p2) { }
EXPORT void f2_V_FIS_IDD(float p0, int p1, struct S_IDD p2) { }
EXPORT void f2_V_FIS_IDP(float p0, int p1, struct S_IDP p2) { }
EXPORT void f2_V_FIS_IPI(float p0, int p1, struct S_IPI p2) { }
EXPORT void f2_V_FIS_IPF(float p0, int p1, struct S_IPF p2) { }
EXPORT void f2_V_FIS_IPD(float p0, int p1, struct S_IPD p2) { }
EXPORT void f2_V_FIS_IPP(float p0, int p1, struct S_IPP p2) { }
EXPORT void f2_V_FIS_FII(float p0, int p1, struct S_FII p2) { }
EXPORT void f2_V_FIS_FIF(float p0, int p1, struct S_FIF p2) { }
EXPORT void f2_V_FIS_FID(float p0, int p1, struct S_FID p2) { }
EXPORT void f2_V_FIS_FIP(float p0, int p1, struct S_FIP p2) { }
EXPORT void f2_V_FIS_FFI(float p0, int p1, struct S_FFI p2) { }
EXPORT void f2_V_FIS_FFF(float p0, int p1, struct S_FFF p2) { }
EXPORT void f2_V_FIS_FFD(float p0, int p1, struct S_FFD p2) { }
EXPORT void f2_V_FIS_FFP(float p0, int p1, struct S_FFP p2) { }
EXPORT void f2_V_FIS_FDI(float p0, int p1, struct S_FDI p2) { }
EXPORT void f2_V_FIS_FDF(float p0, int p1, struct S_FDF p2) { }
EXPORT void f2_V_FIS_FDD(float p0, int p1, struct S_FDD p2) { }
EXPORT void f2_V_FIS_FDP(float p0, int p1, struct S_FDP p2) { }
EXPORT void f2_V_FIS_FPI(float p0, int p1, struct S_FPI p2) { }
EXPORT void f2_V_FIS_FPF(float p0, int p1, struct S_FPF p2) { }
EXPORT void f2_V_FIS_FPD(float p0, int p1, struct S_FPD p2) { }
EXPORT void f2_V_FIS_FPP(float p0, int p1, struct S_FPP p2) { }
EXPORT void f2_V_FIS_DII(float p0, int p1, struct S_DII p2) { }
EXPORT void f2_V_FIS_DIF(float p0, int p1, struct S_DIF p2) { }
EXPORT void f2_V_FIS_DID(float p0, int p1, struct S_DID p2) { }
EXPORT void f2_V_FIS_DIP(float p0, int p1, struct S_DIP p2) { }
EXPORT void f2_V_FIS_DFI(float p0, int p1, struct S_DFI p2) { }
EXPORT void f2_V_FIS_DFF(float p0, int p1, struct S_DFF p2) { }
EXPORT void f2_V_FIS_DFD(float p0, int p1, struct S_DFD p2) { }
EXPORT void f2_V_FIS_DFP(float p0, int p1, struct S_DFP p2) { }
EXPORT void f2_V_FIS_DDI(float p0, int p1, struct S_DDI p2) { }
EXPORT void f2_V_FIS_DDF(float p0, int p1, struct S_DDF p2) { }
EXPORT void f2_V_FIS_DDD(float p0, int p1, struct S_DDD p2) { }
EXPORT void f2_V_FIS_DDP(float p0, int p1, struct S_DDP p2) { }
EXPORT void f2_V_FIS_DPI(float p0, int p1, struct S_DPI p2) { }
EXPORT void f2_V_FIS_DPF(float p0, int p1, struct S_DPF p2) { }
EXPORT void f2_V_FIS_DPD(float p0, int p1, struct S_DPD p2) { }
EXPORT void f2_V_FIS_DPP(float p0, int p1, struct S_DPP p2) { }
EXPORT void f2_V_FIS_PII(float p0, int p1, struct S_PII p2) { }
EXPORT void f2_V_FIS_PIF(float p0, int p1, struct S_PIF p2) { }
EXPORT void f2_V_FIS_PID(float p0, int p1, struct S_PID p2) { }
EXPORT void f2_V_FIS_PIP(float p0, int p1, struct S_PIP p2) { }
EXPORT void f2_V_FIS_PFI(float p0, int p1, struct S_PFI p2) { }
EXPORT void f2_V_FIS_PFF(float p0, int p1, struct S_PFF p2) { }
EXPORT void f2_V_FIS_PFD(float p0, int p1, struct S_PFD p2) { }
EXPORT void f2_V_FIS_PFP(float p0, int p1, struct S_PFP p2) { }
EXPORT void f2_V_FIS_PDI(float p0, int p1, struct S_PDI p2) { }
EXPORT void f2_V_FIS_PDF(float p0, int p1, struct S_PDF p2) { }
EXPORT void f2_V_FIS_PDD(float p0, int p1, struct S_PDD p2) { }
EXPORT void f2_V_FIS_PDP(float p0, int p1, struct S_PDP p2) { }
EXPORT void f2_V_FIS_PPI(float p0, int p1, struct S_PPI p2) { }
EXPORT void f2_V_FIS_PPF(float p0, int p1, struct S_PPF p2) { }
EXPORT void f2_V_FIS_PPD(float p0, int p1, struct S_PPD p2) { }
EXPORT void f2_V_FIS_PPP(float p0, int p1, struct S_PPP p2) { }
EXPORT void f2_V_FFI_(float p0, float p1, int p2) { }
EXPORT void f2_V_FFF_(float p0, float p1, float p2) { }
EXPORT void f2_V_FFD_(float p0, float p1, double p2) { }
EXPORT void f2_V_FFP_(float p0, float p1, void* p2) { }
EXPORT void f2_V_FFS_I(float p0, float p1, struct S_I p2) { }
EXPORT void f2_V_FFS_F(float p0, float p1, struct S_F p2) { }
EXPORT void f2_V_FFS_D(float p0, float p1, struct S_D p2) { }
EXPORT void f2_V_FFS_P(float p0, float p1, struct S_P p2) { }
EXPORT void f2_V_FFS_II(float p0, float p1, struct S_II p2) { }
EXPORT void f2_V_FFS_IF(float p0, float p1, struct S_IF p2) { }
EXPORT void f2_V_FFS_ID(float p0, float p1, struct S_ID p2) { }
EXPORT void f2_V_FFS_IP(float p0, float p1, struct S_IP p2) { }
EXPORT void f2_V_FFS_FI(float p0, float p1, struct S_FI p2) { }
EXPORT void f2_V_FFS_FF(float p0, float p1, struct S_FF p2) { }
EXPORT void f2_V_FFS_FD(float p0, float p1, struct S_FD p2) { }
EXPORT void f2_V_FFS_FP(float p0, float p1, struct S_FP p2) { }
EXPORT void f2_V_FFS_DI(float p0, float p1, struct S_DI p2) { }
EXPORT void f2_V_FFS_DF(float p0, float p1, struct S_DF p2) { }
EXPORT void f2_V_FFS_DD(float p0, float p1, struct S_DD p2) { }
EXPORT void f2_V_FFS_DP(float p0, float p1, struct S_DP p2) { }
EXPORT void f2_V_FFS_PI(float p0, float p1, struct S_PI p2) { }
EXPORT void f2_V_FFS_PF(float p0, float p1, struct S_PF p2) { }
EXPORT void f2_V_FFS_PD(float p0, float p1, struct S_PD p2) { }
EXPORT void f2_V_FFS_PP(float p0, float p1, struct S_PP p2) { }
EXPORT void f2_V_FFS_III(float p0, float p1, struct S_III p2) { }
EXPORT void f2_V_FFS_IIF(float p0, float p1, struct S_IIF p2) { }
EXPORT void f2_V_FFS_IID(float p0, float p1, struct S_IID p2) { }
EXPORT void f2_V_FFS_IIP(float p0, float p1, struct S_IIP p2) { }
EXPORT void f2_V_FFS_IFI(float p0, float p1, struct S_IFI p2) { }
EXPORT void f2_V_FFS_IFF(float p0, float p1, struct S_IFF p2) { }
EXPORT void f2_V_FFS_IFD(float p0, float p1, struct S_IFD p2) { }
EXPORT void f2_V_FFS_IFP(float p0, float p1, struct S_IFP p2) { }
EXPORT void f2_V_FFS_IDI(float p0, float p1, struct S_IDI p2) { }
EXPORT void f2_V_FFS_IDF(float p0, float p1, struct S_IDF p2) { }
EXPORT void f2_V_FFS_IDD(float p0, float p1, struct S_IDD p2) { }
EXPORT void f2_V_FFS_IDP(float p0, float p1, struct S_IDP p2) { }
EXPORT void f2_V_FFS_IPI(float p0, float p1, struct S_IPI p2) { }
EXPORT void f2_V_FFS_IPF(float p0, float p1, struct S_IPF p2) { }
EXPORT void f2_V_FFS_IPD(float p0, float p1, struct S_IPD p2) { }
EXPORT void f2_V_FFS_IPP(float p0, float p1, struct S_IPP p2) { }
EXPORT void f2_V_FFS_FII(float p0, float p1, struct S_FII p2) { }
EXPORT void f2_V_FFS_FIF(float p0, float p1, struct S_FIF p2) { }
EXPORT void f2_V_FFS_FID(float p0, float p1, struct S_FID p2) { }
EXPORT void f2_V_FFS_FIP(float p0, float p1, struct S_FIP p2) { }
EXPORT void f2_V_FFS_FFI(float p0, float p1, struct S_FFI p2) { }
EXPORT void f2_V_FFS_FFF(float p0, float p1, struct S_FFF p2) { }
EXPORT void f2_V_FFS_FFD(float p0, float p1, struct S_FFD p2) { }
EXPORT void f2_V_FFS_FFP(float p0, float p1, struct S_FFP p2) { }
EXPORT void f2_V_FFS_FDI(float p0, float p1, struct S_FDI p2) { }
EXPORT void f2_V_FFS_FDF(float p0, float p1, struct S_FDF p2) { }
EXPORT void f2_V_FFS_FDD(float p0, float p1, struct S_FDD p2) { }
EXPORT void f2_V_FFS_FDP(float p0, float p1, struct S_FDP p2) { }
EXPORT void f2_V_FFS_FPI(float p0, float p1, struct S_FPI p2) { }
EXPORT void f2_V_FFS_FPF(float p0, float p1, struct S_FPF p2) { }
EXPORT void f2_V_FFS_FPD(float p0, float p1, struct S_FPD p2) { }
EXPORT void f2_V_FFS_FPP(float p0, float p1, struct S_FPP p2) { }
EXPORT void f2_V_FFS_DII(float p0, float p1, struct S_DII p2) { }
EXPORT void f2_V_FFS_DIF(float p0, float p1, struct S_DIF p2) { }
EXPORT void f2_V_FFS_DID(float p0, float p1, struct S_DID p2) { }
EXPORT void f2_V_FFS_DIP(float p0, float p1, struct S_DIP p2) { }
EXPORT void f2_V_FFS_DFI(float p0, float p1, struct S_DFI p2) { }
EXPORT void f2_V_FFS_DFF(float p0, float p1, struct S_DFF p2) { }
EXPORT void f2_V_FFS_DFD(float p0, float p1, struct S_DFD p2) { }
EXPORT void f2_V_FFS_DFP(float p0, float p1, struct S_DFP p2) { }
EXPORT void f2_V_FFS_DDI(float p0, float p1, struct S_DDI p2) { }
EXPORT void f2_V_FFS_DDF(float p0, float p1, struct S_DDF p2) { }
EXPORT void f2_V_FFS_DDD(float p0, float p1, struct S_DDD p2) { }
EXPORT void f2_V_FFS_DDP(float p0, float p1, struct S_DDP p2) { }
EXPORT void f2_V_FFS_DPI(float p0, float p1, struct S_DPI p2) { }
EXPORT void f2_V_FFS_DPF(float p0, float p1, struct S_DPF p2) { }
EXPORT void f2_V_FFS_DPD(float p0, float p1, struct S_DPD p2) { }
EXPORT void f2_V_FFS_DPP(float p0, float p1, struct S_DPP p2) { }
EXPORT void f2_V_FFS_PII(float p0, float p1, struct S_PII p2) { }
EXPORT void f2_V_FFS_PIF(float p0, float p1, struct S_PIF p2) { }
EXPORT void f2_V_FFS_PID(float p0, float p1, struct S_PID p2) { }
EXPORT void f2_V_FFS_PIP(float p0, float p1, struct S_PIP p2) { }
EXPORT void f2_V_FFS_PFI(float p0, float p1, struct S_PFI p2) { }
EXPORT void f2_V_FFS_PFF(float p0, float p1, struct S_PFF p2) { }
EXPORT void f2_V_FFS_PFD(float p0, float p1, struct S_PFD p2) { }
EXPORT void f3_V_FFS_PFP(float p0, float p1, struct S_PFP p2) { }
EXPORT void f3_V_FFS_PDI(float p0, float p1, struct S_PDI p2) { }
EXPORT void f3_V_FFS_PDF(float p0, float p1, struct S_PDF p2) { }
EXPORT void f3_V_FFS_PDD(float p0, float p1, struct S_PDD p2) { }
EXPORT void f3_V_FFS_PDP(float p0, float p1, struct S_PDP p2) { }
EXPORT void f3_V_FFS_PPI(float p0, float p1, struct S_PPI p2) { }
EXPORT void f3_V_FFS_PPF(float p0, float p1, struct S_PPF p2) { }
EXPORT void f3_V_FFS_PPD(float p0, float p1, struct S_PPD p2) { }
EXPORT void f3_V_FFS_PPP(float p0, float p1, struct S_PPP p2) { }
EXPORT void f3_V_FDI_(float p0, double p1, int p2) { }
EXPORT void f3_V_FDF_(float p0, double p1, float p2) { }
EXPORT void f3_V_FDD_(float p0, double p1, double p2) { }
EXPORT void f3_V_FDP_(float p0, double p1, void* p2) { }
EXPORT void f3_V_FDS_I(float p0, double p1, struct S_I p2) { }
EXPORT void f3_V_FDS_F(float p0, double p1, struct S_F p2) { }
EXPORT void f3_V_FDS_D(float p0, double p1, struct S_D p2) { }
EXPORT void f3_V_FDS_P(float p0, double p1, struct S_P p2) { }
EXPORT void f3_V_FDS_II(float p0, double p1, struct S_II p2) { }
EXPORT void f3_V_FDS_IF(float p0, double p1, struct S_IF p2) { }
EXPORT void f3_V_FDS_ID(float p0, double p1, struct S_ID p2) { }
EXPORT void f3_V_FDS_IP(float p0, double p1, struct S_IP p2) { }
EXPORT void f3_V_FDS_FI(float p0, double p1, struct S_FI p2) { }
EXPORT void f3_V_FDS_FF(float p0, double p1, struct S_FF p2) { }
EXPORT void f3_V_FDS_FD(float p0, double p1, struct S_FD p2) { }
EXPORT void f3_V_FDS_FP(float p0, double p1, struct S_FP p2) { }
EXPORT void f3_V_FDS_DI(float p0, double p1, struct S_DI p2) { }
EXPORT void f3_V_FDS_DF(float p0, double p1, struct S_DF p2) { }
EXPORT void f3_V_FDS_DD(float p0, double p1, struct S_DD p2) { }
EXPORT void f3_V_FDS_DP(float p0, double p1, struct S_DP p2) { }
EXPORT void f3_V_FDS_PI(float p0, double p1, struct S_PI p2) { }
EXPORT void f3_V_FDS_PF(float p0, double p1, struct S_PF p2) { }
EXPORT void f3_V_FDS_PD(float p0, double p1, struct S_PD p2) { }
EXPORT void f3_V_FDS_PP(float p0, double p1, struct S_PP p2) { }
EXPORT void f3_V_FDS_III(float p0, double p1, struct S_III p2) { }
EXPORT void f3_V_FDS_IIF(float p0, double p1, struct S_IIF p2) { }
EXPORT void f3_V_FDS_IID(float p0, double p1, struct S_IID p2) { }
EXPORT void f3_V_FDS_IIP(float p0, double p1, struct S_IIP p2) { }
EXPORT void f3_V_FDS_IFI(float p0, double p1, struct S_IFI p2) { }
EXPORT void f3_V_FDS_IFF(float p0, double p1, struct S_IFF p2) { }
EXPORT void f3_V_FDS_IFD(float p0, double p1, struct S_IFD p2) { }
EXPORT void f3_V_FDS_IFP(float p0, double p1, struct S_IFP p2) { }
EXPORT void f3_V_FDS_IDI(float p0, double p1, struct S_IDI p2) { }
EXPORT void f3_V_FDS_IDF(float p0, double p1, struct S_IDF p2) { }
EXPORT void f3_V_FDS_IDD(float p0, double p1, struct S_IDD p2) { }
EXPORT void f3_V_FDS_IDP(float p0, double p1, struct S_IDP p2) { }
EXPORT void f3_V_FDS_IPI(float p0, double p1, struct S_IPI p2) { }
EXPORT void f3_V_FDS_IPF(float p0, double p1, struct S_IPF p2) { }
EXPORT void f3_V_FDS_IPD(float p0, double p1, struct S_IPD p2) { }
EXPORT void f3_V_FDS_IPP(float p0, double p1, struct S_IPP p2) { }
EXPORT void f3_V_FDS_FII(float p0, double p1, struct S_FII p2) { }
EXPORT void f3_V_FDS_FIF(float p0, double p1, struct S_FIF p2) { }
EXPORT void f3_V_FDS_FID(float p0, double p1, struct S_FID p2) { }
EXPORT void f3_V_FDS_FIP(float p0, double p1, struct S_FIP p2) { }
EXPORT void f3_V_FDS_FFI(float p0, double p1, struct S_FFI p2) { }
EXPORT void f3_V_FDS_FFF(float p0, double p1, struct S_FFF p2) { }
EXPORT void f3_V_FDS_FFD(float p0, double p1, struct S_FFD p2) { }
EXPORT void f3_V_FDS_FFP(float p0, double p1, struct S_FFP p2) { }
EXPORT void f3_V_FDS_FDI(float p0, double p1, struct S_FDI p2) { }
EXPORT void f3_V_FDS_FDF(float p0, double p1, struct S_FDF p2) { }
EXPORT void f3_V_FDS_FDD(float p0, double p1, struct S_FDD p2) { }
EXPORT void f3_V_FDS_FDP(float p0, double p1, struct S_FDP p2) { }
EXPORT void f3_V_FDS_FPI(float p0, double p1, struct S_FPI p2) { }
EXPORT void f3_V_FDS_FPF(float p0, double p1, struct S_FPF p2) { }
EXPORT void f3_V_FDS_FPD(float p0, double p1, struct S_FPD p2) { }
EXPORT void f3_V_FDS_FPP(float p0, double p1, struct S_FPP p2) { }
EXPORT void f3_V_FDS_DII(float p0, double p1, struct S_DII p2) { }
EXPORT void f3_V_FDS_DIF(float p0, double p1, struct S_DIF p2) { }
EXPORT void f3_V_FDS_DID(float p0, double p1, struct S_DID p2) { }
EXPORT void f3_V_FDS_DIP(float p0, double p1, struct S_DIP p2) { }
EXPORT void f3_V_FDS_DFI(float p0, double p1, struct S_DFI p2) { }
EXPORT void f3_V_FDS_DFF(float p0, double p1, struct S_DFF p2) { }
EXPORT void f3_V_FDS_DFD(float p0, double p1, struct S_DFD p2) { }
EXPORT void f3_V_FDS_DFP(float p0, double p1, struct S_DFP p2) { }
EXPORT void f3_V_FDS_DDI(float p0, double p1, struct S_DDI p2) { }
EXPORT void f3_V_FDS_DDF(float p0, double p1, struct S_DDF p2) { }
EXPORT void f3_V_FDS_DDD(float p0, double p1, struct S_DDD p2) { }
EXPORT void f3_V_FDS_DDP(float p0, double p1, struct S_DDP p2) { }
EXPORT void f3_V_FDS_DPI(float p0, double p1, struct S_DPI p2) { }
EXPORT void f3_V_FDS_DPF(float p0, double p1, struct S_DPF p2) { }
EXPORT void f3_V_FDS_DPD(float p0, double p1, struct S_DPD p2) { }
EXPORT void f3_V_FDS_DPP(float p0, double p1, struct S_DPP p2) { }
EXPORT void f3_V_FDS_PII(float p0, double p1, struct S_PII p2) { }
EXPORT void f3_V_FDS_PIF(float p0, double p1, struct S_PIF p2) { }
EXPORT void f3_V_FDS_PID(float p0, double p1, struct S_PID p2) { }
EXPORT void f3_V_FDS_PIP(float p0, double p1, struct S_PIP p2) { }
EXPORT void f3_V_FDS_PFI(float p0, double p1, struct S_PFI p2) { }
EXPORT void f3_V_FDS_PFF(float p0, double p1, struct S_PFF p2) { }
EXPORT void f3_V_FDS_PFD(float p0, double p1, struct S_PFD p2) { }
EXPORT void f3_V_FDS_PFP(float p0, double p1, struct S_PFP p2) { }
EXPORT void f3_V_FDS_PDI(float p0, double p1, struct S_PDI p2) { }
EXPORT void f3_V_FDS_PDF(float p0, double p1, struct S_PDF p2) { }
EXPORT void f3_V_FDS_PDD(float p0, double p1, struct S_PDD p2) { }
EXPORT void f3_V_FDS_PDP(float p0, double p1, struct S_PDP p2) { }
EXPORT void f3_V_FDS_PPI(float p0, double p1, struct S_PPI p2) { }
EXPORT void f3_V_FDS_PPF(float p0, double p1, struct S_PPF p2) { }
EXPORT void f3_V_FDS_PPD(float p0, double p1, struct S_PPD p2) { }
EXPORT void f3_V_FDS_PPP(float p0, double p1, struct S_PPP p2) { }
EXPORT void f3_V_FPI_(float p0, void* p1, int p2) { }
EXPORT void f3_V_FPF_(float p0, void* p1, float p2) { }
EXPORT void f3_V_FPD_(float p0, void* p1, double p2) { }
EXPORT void f3_V_FPP_(float p0, void* p1, void* p2) { }
EXPORT void f3_V_FPS_I(float p0, void* p1, struct S_I p2) { }
EXPORT void f3_V_FPS_F(float p0, void* p1, struct S_F p2) { }
EXPORT void f3_V_FPS_D(float p0, void* p1, struct S_D p2) { }
EXPORT void f3_V_FPS_P(float p0, void* p1, struct S_P p2) { }
EXPORT void f3_V_FPS_II(float p0, void* p1, struct S_II p2) { }
EXPORT void f3_V_FPS_IF(float p0, void* p1, struct S_IF p2) { }
EXPORT void f3_V_FPS_ID(float p0, void* p1, struct S_ID p2) { }
EXPORT void f3_V_FPS_IP(float p0, void* p1, struct S_IP p2) { }
EXPORT void f3_V_FPS_FI(float p0, void* p1, struct S_FI p2) { }
EXPORT void f3_V_FPS_FF(float p0, void* p1, struct S_FF p2) { }
EXPORT void f3_V_FPS_FD(float p0, void* p1, struct S_FD p2) { }
EXPORT void f3_V_FPS_FP(float p0, void* p1, struct S_FP p2) { }
EXPORT void f3_V_FPS_DI(float p0, void* p1, struct S_DI p2) { }
EXPORT void f3_V_FPS_DF(float p0, void* p1, struct S_DF p2) { }
EXPORT void f3_V_FPS_DD(float p0, void* p1, struct S_DD p2) { }
EXPORT void f3_V_FPS_DP(float p0, void* p1, struct S_DP p2) { }
EXPORT void f3_V_FPS_PI(float p0, void* p1, struct S_PI p2) { }
EXPORT void f3_V_FPS_PF(float p0, void* p1, struct S_PF p2) { }
EXPORT void f3_V_FPS_PD(float p0, void* p1, struct S_PD p2) { }
EXPORT void f3_V_FPS_PP(float p0, void* p1, struct S_PP p2) { }
EXPORT void f3_V_FPS_III(float p0, void* p1, struct S_III p2) { }
EXPORT void f3_V_FPS_IIF(float p0, void* p1, struct S_IIF p2) { }
EXPORT void f3_V_FPS_IID(float p0, void* p1, struct S_IID p2) { }
EXPORT void f3_V_FPS_IIP(float p0, void* p1, struct S_IIP p2) { }
EXPORT void f3_V_FPS_IFI(float p0, void* p1, struct S_IFI p2) { }
EXPORT void f3_V_FPS_IFF(float p0, void* p1, struct S_IFF p2) { }
EXPORT void f3_V_FPS_IFD(float p0, void* p1, struct S_IFD p2) { }
EXPORT void f3_V_FPS_IFP(float p0, void* p1, struct S_IFP p2) { }
EXPORT void f3_V_FPS_IDI(float p0, void* p1, struct S_IDI p2) { }
EXPORT void f3_V_FPS_IDF(float p0, void* p1, struct S_IDF p2) { }
EXPORT void f3_V_FPS_IDD(float p0, void* p1, struct S_IDD p2) { }
EXPORT void f3_V_FPS_IDP(float p0, void* p1, struct S_IDP p2) { }
EXPORT void f3_V_FPS_IPI(float p0, void* p1, struct S_IPI p2) { }
EXPORT void f3_V_FPS_IPF(float p0, void* p1, struct S_IPF p2) { }
EXPORT void f3_V_FPS_IPD(float p0, void* p1, struct S_IPD p2) { }
EXPORT void f3_V_FPS_IPP(float p0, void* p1, struct S_IPP p2) { }
EXPORT void f3_V_FPS_FII(float p0, void* p1, struct S_FII p2) { }
EXPORT void f3_V_FPS_FIF(float p0, void* p1, struct S_FIF p2) { }
EXPORT void f3_V_FPS_FID(float p0, void* p1, struct S_FID p2) { }
EXPORT void f3_V_FPS_FIP(float p0, void* p1, struct S_FIP p2) { }
EXPORT void f3_V_FPS_FFI(float p0, void* p1, struct S_FFI p2) { }
EXPORT void f3_V_FPS_FFF(float p0, void* p1, struct S_FFF p2) { }
EXPORT void f3_V_FPS_FFD(float p0, void* p1, struct S_FFD p2) { }
EXPORT void f3_V_FPS_FFP(float p0, void* p1, struct S_FFP p2) { }
EXPORT void f3_V_FPS_FDI(float p0, void* p1, struct S_FDI p2) { }
EXPORT void f3_V_FPS_FDF(float p0, void* p1, struct S_FDF p2) { }
EXPORT void f3_V_FPS_FDD(float p0, void* p1, struct S_FDD p2) { }
EXPORT void f3_V_FPS_FDP(float p0, void* p1, struct S_FDP p2) { }
EXPORT void f3_V_FPS_FPI(float p0, void* p1, struct S_FPI p2) { }
EXPORT void f3_V_FPS_FPF(float p0, void* p1, struct S_FPF p2) { }
EXPORT void f3_V_FPS_FPD(float p0, void* p1, struct S_FPD p2) { }
EXPORT void f3_V_FPS_FPP(float p0, void* p1, struct S_FPP p2) { }
EXPORT void f3_V_FPS_DII(float p0, void* p1, struct S_DII p2) { }
EXPORT void f3_V_FPS_DIF(float p0, void* p1, struct S_DIF p2) { }
EXPORT void f3_V_FPS_DID(float p0, void* p1, struct S_DID p2) { }
EXPORT void f3_V_FPS_DIP(float p0, void* p1, struct S_DIP p2) { }
EXPORT void f3_V_FPS_DFI(float p0, void* p1, struct S_DFI p2) { }
EXPORT void f3_V_FPS_DFF(float p0, void* p1, struct S_DFF p2) { }
EXPORT void f3_V_FPS_DFD(float p0, void* p1, struct S_DFD p2) { }
EXPORT void f3_V_FPS_DFP(float p0, void* p1, struct S_DFP p2) { }
EXPORT void f3_V_FPS_DDI(float p0, void* p1, struct S_DDI p2) { }
EXPORT void f3_V_FPS_DDF(float p0, void* p1, struct S_DDF p2) { }
EXPORT void f3_V_FPS_DDD(float p0, void* p1, struct S_DDD p2) { }
EXPORT void f3_V_FPS_DDP(float p0, void* p1, struct S_DDP p2) { }
EXPORT void f3_V_FPS_DPI(float p0, void* p1, struct S_DPI p2) { }
EXPORT void f3_V_FPS_DPF(float p0, void* p1, struct S_DPF p2) { }
EXPORT void f3_V_FPS_DPD(float p0, void* p1, struct S_DPD p2) { }
EXPORT void f3_V_FPS_DPP(float p0, void* p1, struct S_DPP p2) { }
EXPORT void f3_V_FPS_PII(float p0, void* p1, struct S_PII p2) { }
EXPORT void f3_V_FPS_PIF(float p0, void* p1, struct S_PIF p2) { }
EXPORT void f3_V_FPS_PID(float p0, void* p1, struct S_PID p2) { }
EXPORT void f3_V_FPS_PIP(float p0, void* p1, struct S_PIP p2) { }
EXPORT void f3_V_FPS_PFI(float p0, void* p1, struct S_PFI p2) { }
EXPORT void f3_V_FPS_PFF(float p0, void* p1, struct S_PFF p2) { }
EXPORT void f3_V_FPS_PFD(float p0, void* p1, struct S_PFD p2) { }
EXPORT void f3_V_FPS_PFP(float p0, void* p1, struct S_PFP p2) { }
EXPORT void f3_V_FPS_PDI(float p0, void* p1, struct S_PDI p2) { }
EXPORT void f3_V_FPS_PDF(float p0, void* p1, struct S_PDF p2) { }
EXPORT void f3_V_FPS_PDD(float p0, void* p1, struct S_PDD p2) { }
EXPORT void f3_V_FPS_PDP(float p0, void* p1, struct S_PDP p2) { }
EXPORT void f3_V_FPS_PPI(float p0, void* p1, struct S_PPI p2) { }
EXPORT void f3_V_FPS_PPF(float p0, void* p1, struct S_PPF p2) { }
EXPORT void f3_V_FPS_PPD(float p0, void* p1, struct S_PPD p2) { }
EXPORT void f3_V_FPS_PPP(float p0, void* p1, struct S_PPP p2) { }
EXPORT void f3_V_FSI_I(float p0, struct S_I p1, int p2) { }
EXPORT void f3_V_FSI_F(float p0, struct S_F p1, int p2) { }
EXPORT void f3_V_FSI_D(float p0, struct S_D p1, int p2) { }
EXPORT void f3_V_FSI_P(float p0, struct S_P p1, int p2) { }
EXPORT void f3_V_FSI_II(float p0, struct S_II p1, int p2) { }
EXPORT void f3_V_FSI_IF(float p0, struct S_IF p1, int p2) { }
EXPORT void f3_V_FSI_ID(float p0, struct S_ID p1, int p2) { }
EXPORT void f3_V_FSI_IP(float p0, struct S_IP p1, int p2) { }
EXPORT void f3_V_FSI_FI(float p0, struct S_FI p1, int p2) { }
EXPORT void f3_V_FSI_FF(float p0, struct S_FF p1, int p2) { }
EXPORT void f3_V_FSI_FD(float p0, struct S_FD p1, int p2) { }
EXPORT void f3_V_FSI_FP(float p0, struct S_FP p1, int p2) { }
EXPORT void f3_V_FSI_DI(float p0, struct S_DI p1, int p2) { }
EXPORT void f3_V_FSI_DF(float p0, struct S_DF p1, int p2) { }
EXPORT void f3_V_FSI_DD(float p0, struct S_DD p1, int p2) { }
EXPORT void f3_V_FSI_DP(float p0, struct S_DP p1, int p2) { }
EXPORT void f3_V_FSI_PI(float p0, struct S_PI p1, int p2) { }
EXPORT void f3_V_FSI_PF(float p0, struct S_PF p1, int p2) { }
EXPORT void f3_V_FSI_PD(float p0, struct S_PD p1, int p2) { }
EXPORT void f3_V_FSI_PP(float p0, struct S_PP p1, int p2) { }
EXPORT void f3_V_FSI_III(float p0, struct S_III p1, int p2) { }
EXPORT void f3_V_FSI_IIF(float p0, struct S_IIF p1, int p2) { }
EXPORT void f3_V_FSI_IID(float p0, struct S_IID p1, int p2) { }
EXPORT void f3_V_FSI_IIP(float p0, struct S_IIP p1, int p2) { }
EXPORT void f3_V_FSI_IFI(float p0, struct S_IFI p1, int p2) { }
EXPORT void f3_V_FSI_IFF(float p0, struct S_IFF p1, int p2) { }
EXPORT void f3_V_FSI_IFD(float p0, struct S_IFD p1, int p2) { }
EXPORT void f3_V_FSI_IFP(float p0, struct S_IFP p1, int p2) { }
EXPORT void f3_V_FSI_IDI(float p0, struct S_IDI p1, int p2) { }
EXPORT void f3_V_FSI_IDF(float p0, struct S_IDF p1, int p2) { }
EXPORT void f3_V_FSI_IDD(float p0, struct S_IDD p1, int p2) { }
EXPORT void f3_V_FSI_IDP(float p0, struct S_IDP p1, int p2) { }
EXPORT void f3_V_FSI_IPI(float p0, struct S_IPI p1, int p2) { }
EXPORT void f3_V_FSI_IPF(float p0, struct S_IPF p1, int p2) { }
EXPORT void f3_V_FSI_IPD(float p0, struct S_IPD p1, int p2) { }
EXPORT void f3_V_FSI_IPP(float p0, struct S_IPP p1, int p2) { }
EXPORT void f3_V_FSI_FII(float p0, struct S_FII p1, int p2) { }
EXPORT void f3_V_FSI_FIF(float p0, struct S_FIF p1, int p2) { }
EXPORT void f3_V_FSI_FID(float p0, struct S_FID p1, int p2) { }
EXPORT void f3_V_FSI_FIP(float p0, struct S_FIP p1, int p2) { }
EXPORT void f3_V_FSI_FFI(float p0, struct S_FFI p1, int p2) { }
EXPORT void f3_V_FSI_FFF(float p0, struct S_FFF p1, int p2) { }
EXPORT void f3_V_FSI_FFD(float p0, struct S_FFD p1, int p2) { }
EXPORT void f3_V_FSI_FFP(float p0, struct S_FFP p1, int p2) { }
EXPORT void f3_V_FSI_FDI(float p0, struct S_FDI p1, int p2) { }
EXPORT void f3_V_FSI_FDF(float p0, struct S_FDF p1, int p2) { }
EXPORT void f3_V_FSI_FDD(float p0, struct S_FDD p1, int p2) { }
EXPORT void f3_V_FSI_FDP(float p0, struct S_FDP p1, int p2) { }
EXPORT void f3_V_FSI_FPI(float p0, struct S_FPI p1, int p2) { }
EXPORT void f3_V_FSI_FPF(float p0, struct S_FPF p1, int p2) { }
EXPORT void f3_V_FSI_FPD(float p0, struct S_FPD p1, int p2) { }
EXPORT void f3_V_FSI_FPP(float p0, struct S_FPP p1, int p2) { }
EXPORT void f3_V_FSI_DII(float p0, struct S_DII p1, int p2) { }
EXPORT void f3_V_FSI_DIF(float p0, struct S_DIF p1, int p2) { }
EXPORT void f3_V_FSI_DID(float p0, struct S_DID p1, int p2) { }
EXPORT void f3_V_FSI_DIP(float p0, struct S_DIP p1, int p2) { }
EXPORT void f3_V_FSI_DFI(float p0, struct S_DFI p1, int p2) { }
EXPORT void f3_V_FSI_DFF(float p0, struct S_DFF p1, int p2) { }
EXPORT void f3_V_FSI_DFD(float p0, struct S_DFD p1, int p2) { }
EXPORT void f3_V_FSI_DFP(float p0, struct S_DFP p1, int p2) { }
EXPORT void f3_V_FSI_DDI(float p0, struct S_DDI p1, int p2) { }
EXPORT void f3_V_FSI_DDF(float p0, struct S_DDF p1, int p2) { }
EXPORT void f3_V_FSI_DDD(float p0, struct S_DDD p1, int p2) { }
EXPORT void f3_V_FSI_DDP(float p0, struct S_DDP p1, int p2) { }
EXPORT void f3_V_FSI_DPI(float p0, struct S_DPI p1, int p2) { }
EXPORT void f3_V_FSI_DPF(float p0, struct S_DPF p1, int p2) { }
EXPORT void f3_V_FSI_DPD(float p0, struct S_DPD p1, int p2) { }
EXPORT void f3_V_FSI_DPP(float p0, struct S_DPP p1, int p2) { }
EXPORT void f3_V_FSI_PII(float p0, struct S_PII p1, int p2) { }
EXPORT void f3_V_FSI_PIF(float p0, struct S_PIF p1, int p2) { }
EXPORT void f3_V_FSI_PID(float p0, struct S_PID p1, int p2) { }
EXPORT void f3_V_FSI_PIP(float p0, struct S_PIP p1, int p2) { }
EXPORT void f3_V_FSI_PFI(float p0, struct S_PFI p1, int p2) { }
EXPORT void f3_V_FSI_PFF(float p0, struct S_PFF p1, int p2) { }
EXPORT void f3_V_FSI_PFD(float p0, struct S_PFD p1, int p2) { }
EXPORT void f3_V_FSI_PFP(float p0, struct S_PFP p1, int p2) { }
EXPORT void f3_V_FSI_PDI(float p0, struct S_PDI p1, int p2) { }
EXPORT void f3_V_FSI_PDF(float p0, struct S_PDF p1, int p2) { }
EXPORT void f3_V_FSI_PDD(float p0, struct S_PDD p1, int p2) { }
EXPORT void f3_V_FSI_PDP(float p0, struct S_PDP p1, int p2) { }
EXPORT void f3_V_FSI_PPI(float p0, struct S_PPI p1, int p2) { }
EXPORT void f3_V_FSI_PPF(float p0, struct S_PPF p1, int p2) { }
EXPORT void f3_V_FSI_PPD(float p0, struct S_PPD p1, int p2) { }
EXPORT void f3_V_FSI_PPP(float p0, struct S_PPP p1, int p2) { }
EXPORT void f3_V_FSF_I(float p0, struct S_I p1, float p2) { }
EXPORT void f3_V_FSF_F(float p0, struct S_F p1, float p2) { }
EXPORT void f3_V_FSF_D(float p0, struct S_D p1, float p2) { }
EXPORT void f3_V_FSF_P(float p0, struct S_P p1, float p2) { }
EXPORT void f3_V_FSF_II(float p0, struct S_II p1, float p2) { }
EXPORT void f3_V_FSF_IF(float p0, struct S_IF p1, float p2) { }
EXPORT void f3_V_FSF_ID(float p0, struct S_ID p1, float p2) { }
EXPORT void f3_V_FSF_IP(float p0, struct S_IP p1, float p2) { }
EXPORT void f3_V_FSF_FI(float p0, struct S_FI p1, float p2) { }
EXPORT void f3_V_FSF_FF(float p0, struct S_FF p1, float p2) { }
EXPORT void f3_V_FSF_FD(float p0, struct S_FD p1, float p2) { }
EXPORT void f3_V_FSF_FP(float p0, struct S_FP p1, float p2) { }
EXPORT void f3_V_FSF_DI(float p0, struct S_DI p1, float p2) { }
EXPORT void f3_V_FSF_DF(float p0, struct S_DF p1, float p2) { }
EXPORT void f3_V_FSF_DD(float p0, struct S_DD p1, float p2) { }
EXPORT void f3_V_FSF_DP(float p0, struct S_DP p1, float p2) { }
EXPORT void f3_V_FSF_PI(float p0, struct S_PI p1, float p2) { }
EXPORT void f3_V_FSF_PF(float p0, struct S_PF p1, float p2) { }
EXPORT void f3_V_FSF_PD(float p0, struct S_PD p1, float p2) { }
EXPORT void f3_V_FSF_PP(float p0, struct S_PP p1, float p2) { }
EXPORT void f3_V_FSF_III(float p0, struct S_III p1, float p2) { }
EXPORT void f3_V_FSF_IIF(float p0, struct S_IIF p1, float p2) { }
EXPORT void f3_V_FSF_IID(float p0, struct S_IID p1, float p2) { }
EXPORT void f3_V_FSF_IIP(float p0, struct S_IIP p1, float p2) { }
EXPORT void f3_V_FSF_IFI(float p0, struct S_IFI p1, float p2) { }
EXPORT void f3_V_FSF_IFF(float p0, struct S_IFF p1, float p2) { }
EXPORT void f3_V_FSF_IFD(float p0, struct S_IFD p1, float p2) { }
EXPORT void f3_V_FSF_IFP(float p0, struct S_IFP p1, float p2) { }
EXPORT void f3_V_FSF_IDI(float p0, struct S_IDI p1, float p2) { }
EXPORT void f3_V_FSF_IDF(float p0, struct S_IDF p1, float p2) { }
EXPORT void f3_V_FSF_IDD(float p0, struct S_IDD p1, float p2) { }
EXPORT void f3_V_FSF_IDP(float p0, struct S_IDP p1, float p2) { }
EXPORT void f3_V_FSF_IPI(float p0, struct S_IPI p1, float p2) { }
EXPORT void f3_V_FSF_IPF(float p0, struct S_IPF p1, float p2) { }
EXPORT void f3_V_FSF_IPD(float p0, struct S_IPD p1, float p2) { }
EXPORT void f3_V_FSF_IPP(float p0, struct S_IPP p1, float p2) { }
EXPORT void f3_V_FSF_FII(float p0, struct S_FII p1, float p2) { }
EXPORT void f3_V_FSF_FIF(float p0, struct S_FIF p1, float p2) { }
EXPORT void f3_V_FSF_FID(float p0, struct S_FID p1, float p2) { }
EXPORT void f3_V_FSF_FIP(float p0, struct S_FIP p1, float p2) { }
EXPORT void f3_V_FSF_FFI(float p0, struct S_FFI p1, float p2) { }
EXPORT void f3_V_FSF_FFF(float p0, struct S_FFF p1, float p2) { }
EXPORT void f3_V_FSF_FFD(float p0, struct S_FFD p1, float p2) { }
EXPORT void f3_V_FSF_FFP(float p0, struct S_FFP p1, float p2) { }
EXPORT void f3_V_FSF_FDI(float p0, struct S_FDI p1, float p2) { }
EXPORT void f3_V_FSF_FDF(float p0, struct S_FDF p1, float p2) { }
EXPORT void f3_V_FSF_FDD(float p0, struct S_FDD p1, float p2) { }
EXPORT void f3_V_FSF_FDP(float p0, struct S_FDP p1, float p2) { }
EXPORT void f3_V_FSF_FPI(float p0, struct S_FPI p1, float p2) { }
EXPORT void f3_V_FSF_FPF(float p0, struct S_FPF p1, float p2) { }
EXPORT void f3_V_FSF_FPD(float p0, struct S_FPD p1, float p2) { }
EXPORT void f3_V_FSF_FPP(float p0, struct S_FPP p1, float p2) { }
EXPORT void f3_V_FSF_DII(float p0, struct S_DII p1, float p2) { }
EXPORT void f3_V_FSF_DIF(float p0, struct S_DIF p1, float p2) { }
EXPORT void f3_V_FSF_DID(float p0, struct S_DID p1, float p2) { }
EXPORT void f3_V_FSF_DIP(float p0, struct S_DIP p1, float p2) { }
EXPORT void f3_V_FSF_DFI(float p0, struct S_DFI p1, float p2) { }
EXPORT void f3_V_FSF_DFF(float p0, struct S_DFF p1, float p2) { }
EXPORT void f3_V_FSF_DFD(float p0, struct S_DFD p1, float p2) { }
EXPORT void f3_V_FSF_DFP(float p0, struct S_DFP p1, float p2) { }
EXPORT void f3_V_FSF_DDI(float p0, struct S_DDI p1, float p2) { }
EXPORT void f3_V_FSF_DDF(float p0, struct S_DDF p1, float p2) { }
EXPORT void f3_V_FSF_DDD(float p0, struct S_DDD p1, float p2) { }
EXPORT void f3_V_FSF_DDP(float p0, struct S_DDP p1, float p2) { }
EXPORT void f3_V_FSF_DPI(float p0, struct S_DPI p1, float p2) { }
EXPORT void f3_V_FSF_DPF(float p0, struct S_DPF p1, float p2) { }
EXPORT void f3_V_FSF_DPD(float p0, struct S_DPD p1, float p2) { }
EXPORT void f3_V_FSF_DPP(float p0, struct S_DPP p1, float p2) { }
EXPORT void f3_V_FSF_PII(float p0, struct S_PII p1, float p2) { }
EXPORT void f3_V_FSF_PIF(float p0, struct S_PIF p1, float p2) { }
EXPORT void f3_V_FSF_PID(float p0, struct S_PID p1, float p2) { }
EXPORT void f3_V_FSF_PIP(float p0, struct S_PIP p1, float p2) { }
EXPORT void f3_V_FSF_PFI(float p0, struct S_PFI p1, float p2) { }
EXPORT void f3_V_FSF_PFF(float p0, struct S_PFF p1, float p2) { }
EXPORT void f3_V_FSF_PFD(float p0, struct S_PFD p1, float p2) { }
EXPORT void f3_V_FSF_PFP(float p0, struct S_PFP p1, float p2) { }
EXPORT void f3_V_FSF_PDI(float p0, struct S_PDI p1, float p2) { }
EXPORT void f3_V_FSF_PDF(float p0, struct S_PDF p1, float p2) { }
EXPORT void f3_V_FSF_PDD(float p0, struct S_PDD p1, float p2) { }
EXPORT void f3_V_FSF_PDP(float p0, struct S_PDP p1, float p2) { }
EXPORT void f3_V_FSF_PPI(float p0, struct S_PPI p1, float p2) { }
EXPORT void f3_V_FSF_PPF(float p0, struct S_PPF p1, float p2) { }
EXPORT void f3_V_FSF_PPD(float p0, struct S_PPD p1, float p2) { }
EXPORT void f3_V_FSF_PPP(float p0, struct S_PPP p1, float p2) { }
EXPORT void f3_V_FSD_I(float p0, struct S_I p1, double p2) { }
EXPORT void f3_V_FSD_F(float p0, struct S_F p1, double p2) { }
EXPORT void f3_V_FSD_D(float p0, struct S_D p1, double p2) { }
EXPORT void f3_V_FSD_P(float p0, struct S_P p1, double p2) { }
EXPORT void f3_V_FSD_II(float p0, struct S_II p1, double p2) { }
EXPORT void f3_V_FSD_IF(float p0, struct S_IF p1, double p2) { }
EXPORT void f3_V_FSD_ID(float p0, struct S_ID p1, double p2) { }
EXPORT void f3_V_FSD_IP(float p0, struct S_IP p1, double p2) { }
EXPORT void f3_V_FSD_FI(float p0, struct S_FI p1, double p2) { }
EXPORT void f3_V_FSD_FF(float p0, struct S_FF p1, double p2) { }
EXPORT void f3_V_FSD_FD(float p0, struct S_FD p1, double p2) { }
EXPORT void f3_V_FSD_FP(float p0, struct S_FP p1, double p2) { }
EXPORT void f3_V_FSD_DI(float p0, struct S_DI p1, double p2) { }
EXPORT void f3_V_FSD_DF(float p0, struct S_DF p1, double p2) { }
EXPORT void f3_V_FSD_DD(float p0, struct S_DD p1, double p2) { }
EXPORT void f3_V_FSD_DP(float p0, struct S_DP p1, double p2) { }
EXPORT void f3_V_FSD_PI(float p0, struct S_PI p1, double p2) { }
EXPORT void f3_V_FSD_PF(float p0, struct S_PF p1, double p2) { }
EXPORT void f3_V_FSD_PD(float p0, struct S_PD p1, double p2) { }
EXPORT void f3_V_FSD_PP(float p0, struct S_PP p1, double p2) { }
EXPORT void f3_V_FSD_III(float p0, struct S_III p1, double p2) { }
EXPORT void f3_V_FSD_IIF(float p0, struct S_IIF p1, double p2) { }
EXPORT void f3_V_FSD_IID(float p0, struct S_IID p1, double p2) { }
EXPORT void f3_V_FSD_IIP(float p0, struct S_IIP p1, double p2) { }
EXPORT void f3_V_FSD_IFI(float p0, struct S_IFI p1, double p2) { }
EXPORT void f3_V_FSD_IFF(float p0, struct S_IFF p1, double p2) { }
EXPORT void f3_V_FSD_IFD(float p0, struct S_IFD p1, double p2) { }
EXPORT void f3_V_FSD_IFP(float p0, struct S_IFP p1, double p2) { }
EXPORT void f3_V_FSD_IDI(float p0, struct S_IDI p1, double p2) { }
EXPORT void f3_V_FSD_IDF(float p0, struct S_IDF p1, double p2) { }
EXPORT void f3_V_FSD_IDD(float p0, struct S_IDD p1, double p2) { }
EXPORT void f3_V_FSD_IDP(float p0, struct S_IDP p1, double p2) { }
EXPORT void f3_V_FSD_IPI(float p0, struct S_IPI p1, double p2) { }
EXPORT void f3_V_FSD_IPF(float p0, struct S_IPF p1, double p2) { }
EXPORT void f3_V_FSD_IPD(float p0, struct S_IPD p1, double p2) { }
EXPORT void f3_V_FSD_IPP(float p0, struct S_IPP p1, double p2) { }
EXPORT void f3_V_FSD_FII(float p0, struct S_FII p1, double p2) { }
EXPORT void f3_V_FSD_FIF(float p0, struct S_FIF p1, double p2) { }
EXPORT void f3_V_FSD_FID(float p0, struct S_FID p1, double p2) { }
EXPORT void f3_V_FSD_FIP(float p0, struct S_FIP p1, double p2) { }
EXPORT void f3_V_FSD_FFI(float p0, struct S_FFI p1, double p2) { }
EXPORT void f3_V_FSD_FFF(float p0, struct S_FFF p1, double p2) { }
EXPORT void f3_V_FSD_FFD(float p0, struct S_FFD p1, double p2) { }
EXPORT void f3_V_FSD_FFP(float p0, struct S_FFP p1, double p2) { }
EXPORT void f3_V_FSD_FDI(float p0, struct S_FDI p1, double p2) { }
EXPORT void f3_V_FSD_FDF(float p0, struct S_FDF p1, double p2) { }
EXPORT void f3_V_FSD_FDD(float p0, struct S_FDD p1, double p2) { }
EXPORT void f3_V_FSD_FDP(float p0, struct S_FDP p1, double p2) { }
EXPORT void f3_V_FSD_FPI(float p0, struct S_FPI p1, double p2) { }
EXPORT void f3_V_FSD_FPF(float p0, struct S_FPF p1, double p2) { }
EXPORT void f3_V_FSD_FPD(float p0, struct S_FPD p1, double p2) { }
EXPORT void f3_V_FSD_FPP(float p0, struct S_FPP p1, double p2) { }
EXPORT void f3_V_FSD_DII(float p0, struct S_DII p1, double p2) { }
EXPORT void f3_V_FSD_DIF(float p0, struct S_DIF p1, double p2) { }
EXPORT void f3_V_FSD_DID(float p0, struct S_DID p1, double p2) { }
EXPORT void f3_V_FSD_DIP(float p0, struct S_DIP p1, double p2) { }
EXPORT void f3_V_FSD_DFI(float p0, struct S_DFI p1, double p2) { }
EXPORT void f3_V_FSD_DFF(float p0, struct S_DFF p1, double p2) { }
EXPORT void f3_V_FSD_DFD(float p0, struct S_DFD p1, double p2) { }
EXPORT void f3_V_FSD_DFP(float p0, struct S_DFP p1, double p2) { }
EXPORT void f3_V_FSD_DDI(float p0, struct S_DDI p1, double p2) { }
EXPORT void f3_V_FSD_DDF(float p0, struct S_DDF p1, double p2) { }
EXPORT void f3_V_FSD_DDD(float p0, struct S_DDD p1, double p2) { }
EXPORT void f3_V_FSD_DDP(float p0, struct S_DDP p1, double p2) { }
EXPORT void f3_V_FSD_DPI(float p0, struct S_DPI p1, double p2) { }
EXPORT void f3_V_FSD_DPF(float p0, struct S_DPF p1, double p2) { }
EXPORT void f3_V_FSD_DPD(float p0, struct S_DPD p1, double p2) { }
EXPORT void f3_V_FSD_DPP(float p0, struct S_DPP p1, double p2) { }
EXPORT void f3_V_FSD_PII(float p0, struct S_PII p1, double p2) { }
EXPORT void f3_V_FSD_PIF(float p0, struct S_PIF p1, double p2) { }
EXPORT void f3_V_FSD_PID(float p0, struct S_PID p1, double p2) { }
EXPORT void f3_V_FSD_PIP(float p0, struct S_PIP p1, double p2) { }
EXPORT void f3_V_FSD_PFI(float p0, struct S_PFI p1, double p2) { }
EXPORT void f3_V_FSD_PFF(float p0, struct S_PFF p1, double p2) { }
EXPORT void f3_V_FSD_PFD(float p0, struct S_PFD p1, double p2) { }
EXPORT void f3_V_FSD_PFP(float p0, struct S_PFP p1, double p2) { }
EXPORT void f3_V_FSD_PDI(float p0, struct S_PDI p1, double p2) { }
EXPORT void f3_V_FSD_PDF(float p0, struct S_PDF p1, double p2) { }
EXPORT void f3_V_FSD_PDD(float p0, struct S_PDD p1, double p2) { }
EXPORT void f3_V_FSD_PDP(float p0, struct S_PDP p1, double p2) { }
EXPORT void f3_V_FSD_PPI(float p0, struct S_PPI p1, double p2) { }
EXPORT void f3_V_FSD_PPF(float p0, struct S_PPF p1, double p2) { }
EXPORT void f3_V_FSD_PPD(float p0, struct S_PPD p1, double p2) { }
EXPORT void f3_V_FSD_PPP(float p0, struct S_PPP p1, double p2) { }
EXPORT void f3_V_FSP_I(float p0, struct S_I p1, void* p2) { }
EXPORT void f3_V_FSP_F(float p0, struct S_F p1, void* p2) { }
EXPORT void f3_V_FSP_D(float p0, struct S_D p1, void* p2) { }
EXPORT void f3_V_FSP_P(float p0, struct S_P p1, void* p2) { }
EXPORT void f3_V_FSP_II(float p0, struct S_II p1, void* p2) { }
EXPORT void f3_V_FSP_IF(float p0, struct S_IF p1, void* p2) { }
EXPORT void f3_V_FSP_ID(float p0, struct S_ID p1, void* p2) { }
EXPORT void f3_V_FSP_IP(float p0, struct S_IP p1, void* p2) { }
EXPORT void f3_V_FSP_FI(float p0, struct S_FI p1, void* p2) { }
EXPORT void f3_V_FSP_FF(float p0, struct S_FF p1, void* p2) { }
EXPORT void f3_V_FSP_FD(float p0, struct S_FD p1, void* p2) { }
EXPORT void f3_V_FSP_FP(float p0, struct S_FP p1, void* p2) { }
EXPORT void f3_V_FSP_DI(float p0, struct S_DI p1, void* p2) { }
EXPORT void f3_V_FSP_DF(float p0, struct S_DF p1, void* p2) { }
EXPORT void f3_V_FSP_DD(float p0, struct S_DD p1, void* p2) { }
EXPORT void f3_V_FSP_DP(float p0, struct S_DP p1, void* p2) { }
EXPORT void f3_V_FSP_PI(float p0, struct S_PI p1, void* p2) { }
EXPORT void f3_V_FSP_PF(float p0, struct S_PF p1, void* p2) { }
EXPORT void f3_V_FSP_PD(float p0, struct S_PD p1, void* p2) { }
EXPORT void f3_V_FSP_PP(float p0, struct S_PP p1, void* p2) { }
EXPORT void f3_V_FSP_III(float p0, struct S_III p1, void* p2) { }
EXPORT void f3_V_FSP_IIF(float p0, struct S_IIF p1, void* p2) { }
EXPORT void f3_V_FSP_IID(float p0, struct S_IID p1, void* p2) { }
EXPORT void f3_V_FSP_IIP(float p0, struct S_IIP p1, void* p2) { }
EXPORT void f3_V_FSP_IFI(float p0, struct S_IFI p1, void* p2) { }
EXPORT void f3_V_FSP_IFF(float p0, struct S_IFF p1, void* p2) { }
EXPORT void f3_V_FSP_IFD(float p0, struct S_IFD p1, void* p2) { }
EXPORT void f3_V_FSP_IFP(float p0, struct S_IFP p1, void* p2) { }
EXPORT void f3_V_FSP_IDI(float p0, struct S_IDI p1, void* p2) { }
EXPORT void f3_V_FSP_IDF(float p0, struct S_IDF p1, void* p2) { }
EXPORT void f3_V_FSP_IDD(float p0, struct S_IDD p1, void* p2) { }
EXPORT void f3_V_FSP_IDP(float p0, struct S_IDP p1, void* p2) { }
EXPORT void f3_V_FSP_IPI(float p0, struct S_IPI p1, void* p2) { }
EXPORT void f3_V_FSP_IPF(float p0, struct S_IPF p1, void* p2) { }
EXPORT void f3_V_FSP_IPD(float p0, struct S_IPD p1, void* p2) { }
EXPORT void f3_V_FSP_IPP(float p0, struct S_IPP p1, void* p2) { }
EXPORT void f3_V_FSP_FII(float p0, struct S_FII p1, void* p2) { }
EXPORT void f3_V_FSP_FIF(float p0, struct S_FIF p1, void* p2) { }
EXPORT void f3_V_FSP_FID(float p0, struct S_FID p1, void* p2) { }
EXPORT void f3_V_FSP_FIP(float p0, struct S_FIP p1, void* p2) { }
EXPORT void f3_V_FSP_FFI(float p0, struct S_FFI p1, void* p2) { }
EXPORT void f3_V_FSP_FFF(float p0, struct S_FFF p1, void* p2) { }
EXPORT void f3_V_FSP_FFD(float p0, struct S_FFD p1, void* p2) { }
EXPORT void f3_V_FSP_FFP(float p0, struct S_FFP p1, void* p2) { }
EXPORT void f3_V_FSP_FDI(float p0, struct S_FDI p1, void* p2) { }
EXPORT void f3_V_FSP_FDF(float p0, struct S_FDF p1, void* p2) { }
EXPORT void f3_V_FSP_FDD(float p0, struct S_FDD p1, void* p2) { }
EXPORT void f3_V_FSP_FDP(float p0, struct S_FDP p1, void* p2) { }
EXPORT void f3_V_FSP_FPI(float p0, struct S_FPI p1, void* p2) { }
EXPORT void f3_V_FSP_FPF(float p0, struct S_FPF p1, void* p2) { }
EXPORT void f3_V_FSP_FPD(float p0, struct S_FPD p1, void* p2) { }
EXPORT void f3_V_FSP_FPP(float p0, struct S_FPP p1, void* p2) { }
EXPORT void f3_V_FSP_DII(float p0, struct S_DII p1, void* p2) { }
EXPORT void f3_V_FSP_DIF(float p0, struct S_DIF p1, void* p2) { }
EXPORT void f3_V_FSP_DID(float p0, struct S_DID p1, void* p2) { }
EXPORT void f3_V_FSP_DIP(float p0, struct S_DIP p1, void* p2) { }
EXPORT void f3_V_FSP_DFI(float p0, struct S_DFI p1, void* p2) { }
EXPORT void f3_V_FSP_DFF(float p0, struct S_DFF p1, void* p2) { }
EXPORT void f3_V_FSP_DFD(float p0, struct S_DFD p1, void* p2) { }
EXPORT void f3_V_FSP_DFP(float p0, struct S_DFP p1, void* p2) { }
EXPORT void f3_V_FSP_DDI(float p0, struct S_DDI p1, void* p2) { }
EXPORT void f3_V_FSP_DDF(float p0, struct S_DDF p1, void* p2) { }
EXPORT void f3_V_FSP_DDD(float p0, struct S_DDD p1, void* p2) { }
EXPORT void f3_V_FSP_DDP(float p0, struct S_DDP p1, void* p2) { }
EXPORT void f3_V_FSP_DPI(float p0, struct S_DPI p1, void* p2) { }
EXPORT void f3_V_FSP_DPF(float p0, struct S_DPF p1, void* p2) { }
EXPORT void f3_V_FSP_DPD(float p0, struct S_DPD p1, void* p2) { }
EXPORT void f3_V_FSP_DPP(float p0, struct S_DPP p1, void* p2) { }
EXPORT void f3_V_FSP_PII(float p0, struct S_PII p1, void* p2) { }
EXPORT void f3_V_FSP_PIF(float p0, struct S_PIF p1, void* p2) { }
EXPORT void f3_V_FSP_PID(float p0, struct S_PID p1, void* p2) { }
EXPORT void f3_V_FSP_PIP(float p0, struct S_PIP p1, void* p2) { }
EXPORT void f3_V_FSP_PFI(float p0, struct S_PFI p1, void* p2) { }
EXPORT void f3_V_FSP_PFF(float p0, struct S_PFF p1, void* p2) { }
EXPORT void f3_V_FSP_PFD(float p0, struct S_PFD p1, void* p2) { }
EXPORT void f3_V_FSP_PFP(float p0, struct S_PFP p1, void* p2) { }
EXPORT void f3_V_FSP_PDI(float p0, struct S_PDI p1, void* p2) { }
EXPORT void f3_V_FSP_PDF(float p0, struct S_PDF p1, void* p2) { }
EXPORT void f3_V_FSP_PDD(float p0, struct S_PDD p1, void* p2) { }
EXPORT void f3_V_FSP_PDP(float p0, struct S_PDP p1, void* p2) { }
EXPORT void f3_V_FSP_PPI(float p0, struct S_PPI p1, void* p2) { }
EXPORT void f3_V_FSP_PPF(float p0, struct S_PPF p1, void* p2) { }
EXPORT void f3_V_FSP_PPD(float p0, struct S_PPD p1, void* p2) { }
EXPORT void f3_V_FSP_PPP(float p0, struct S_PPP p1, void* p2) { }
EXPORT void f3_V_FSS_I(float p0, struct S_I p1, struct S_I p2) { }
EXPORT void f3_V_FSS_F(float p0, struct S_F p1, struct S_F p2) { }
EXPORT void f3_V_FSS_D(float p0, struct S_D p1, struct S_D p2) { }
EXPORT void f3_V_FSS_P(float p0, struct S_P p1, struct S_P p2) { }
EXPORT void f3_V_FSS_II(float p0, struct S_II p1, struct S_II p2) { }
EXPORT void f3_V_FSS_IF(float p0, struct S_IF p1, struct S_IF p2) { }
EXPORT void f3_V_FSS_ID(float p0, struct S_ID p1, struct S_ID p2) { }
EXPORT void f3_V_FSS_IP(float p0, struct S_IP p1, struct S_IP p2) { }
EXPORT void f3_V_FSS_FI(float p0, struct S_FI p1, struct S_FI p2) { }
EXPORT void f3_V_FSS_FF(float p0, struct S_FF p1, struct S_FF p2) { }
EXPORT void f3_V_FSS_FD(float p0, struct S_FD p1, struct S_FD p2) { }
EXPORT void f3_V_FSS_FP(float p0, struct S_FP p1, struct S_FP p2) { }
EXPORT void f3_V_FSS_DI(float p0, struct S_DI p1, struct S_DI p2) { }
EXPORT void f3_V_FSS_DF(float p0, struct S_DF p1, struct S_DF p2) { }
EXPORT void f3_V_FSS_DD(float p0, struct S_DD p1, struct S_DD p2) { }
EXPORT void f3_V_FSS_DP(float p0, struct S_DP p1, struct S_DP p2) { }
EXPORT void f3_V_FSS_PI(float p0, struct S_PI p1, struct S_PI p2) { }
EXPORT void f3_V_FSS_PF(float p0, struct S_PF p1, struct S_PF p2) { }
EXPORT void f3_V_FSS_PD(float p0, struct S_PD p1, struct S_PD p2) { }
EXPORT void f3_V_FSS_PP(float p0, struct S_PP p1, struct S_PP p2) { }
EXPORT void f3_V_FSS_III(float p0, struct S_III p1, struct S_III p2) { }
EXPORT void f3_V_FSS_IIF(float p0, struct S_IIF p1, struct S_IIF p2) { }
EXPORT void f3_V_FSS_IID(float p0, struct S_IID p1, struct S_IID p2) { }
EXPORT void f3_V_FSS_IIP(float p0, struct S_IIP p1, struct S_IIP p2) { }
EXPORT void f3_V_FSS_IFI(float p0, struct S_IFI p1, struct S_IFI p2) { }
EXPORT void f3_V_FSS_IFF(float p0, struct S_IFF p1, struct S_IFF p2) { }
EXPORT void f3_V_FSS_IFD(float p0, struct S_IFD p1, struct S_IFD p2) { }
EXPORT void f3_V_FSS_IFP(float p0, struct S_IFP p1, struct S_IFP p2) { }
EXPORT void f3_V_FSS_IDI(float p0, struct S_IDI p1, struct S_IDI p2) { }
EXPORT void f3_V_FSS_IDF(float p0, struct S_IDF p1, struct S_IDF p2) { }
EXPORT void f3_V_FSS_IDD(float p0, struct S_IDD p1, struct S_IDD p2) { }
EXPORT void f3_V_FSS_IDP(float p0, struct S_IDP p1, struct S_IDP p2) { }
EXPORT void f3_V_FSS_IPI(float p0, struct S_IPI p1, struct S_IPI p2) { }
EXPORT void f3_V_FSS_IPF(float p0, struct S_IPF p1, struct S_IPF p2) { }
EXPORT void f3_V_FSS_IPD(float p0, struct S_IPD p1, struct S_IPD p2) { }
EXPORT void f3_V_FSS_IPP(float p0, struct S_IPP p1, struct S_IPP p2) { }
EXPORT void f3_V_FSS_FII(float p0, struct S_FII p1, struct S_FII p2) { }
EXPORT void f3_V_FSS_FIF(float p0, struct S_FIF p1, struct S_FIF p2) { }
EXPORT void f3_V_FSS_FID(float p0, struct S_FID p1, struct S_FID p2) { }
EXPORT void f3_V_FSS_FIP(float p0, struct S_FIP p1, struct S_FIP p2) { }
EXPORT void f3_V_FSS_FFI(float p0, struct S_FFI p1, struct S_FFI p2) { }
EXPORT void f3_V_FSS_FFF(float p0, struct S_FFF p1, struct S_FFF p2) { }
EXPORT void f3_V_FSS_FFD(float p0, struct S_FFD p1, struct S_FFD p2) { }
EXPORT void f3_V_FSS_FFP(float p0, struct S_FFP p1, struct S_FFP p2) { }
EXPORT void f3_V_FSS_FDI(float p0, struct S_FDI p1, struct S_FDI p2) { }
EXPORT void f3_V_FSS_FDF(float p0, struct S_FDF p1, struct S_FDF p2) { }
EXPORT void f3_V_FSS_FDD(float p0, struct S_FDD p1, struct S_FDD p2) { }
EXPORT void f3_V_FSS_FDP(float p0, struct S_FDP p1, struct S_FDP p2) { }
EXPORT void f3_V_FSS_FPI(float p0, struct S_FPI p1, struct S_FPI p2) { }
EXPORT void f3_V_FSS_FPF(float p0, struct S_FPF p1, struct S_FPF p2) { }
EXPORT void f3_V_FSS_FPD(float p0, struct S_FPD p1, struct S_FPD p2) { }
EXPORT void f3_V_FSS_FPP(float p0, struct S_FPP p1, struct S_FPP p2) { }
EXPORT void f3_V_FSS_DII(float p0, struct S_DII p1, struct S_DII p2) { }
EXPORT void f3_V_FSS_DIF(float p0, struct S_DIF p1, struct S_DIF p2) { }
EXPORT void f3_V_FSS_DID(float p0, struct S_DID p1, struct S_DID p2) { }
EXPORT void f3_V_FSS_DIP(float p0, struct S_DIP p1, struct S_DIP p2) { }
EXPORT void f3_V_FSS_DFI(float p0, struct S_DFI p1, struct S_DFI p2) { }
EXPORT void f3_V_FSS_DFF(float p0, struct S_DFF p1, struct S_DFF p2) { }
EXPORT void f3_V_FSS_DFD(float p0, struct S_DFD p1, struct S_DFD p2) { }
EXPORT void f3_V_FSS_DFP(float p0, struct S_DFP p1, struct S_DFP p2) { }
EXPORT void f3_V_FSS_DDI(float p0, struct S_DDI p1, struct S_DDI p2) { }
EXPORT void f3_V_FSS_DDF(float p0, struct S_DDF p1, struct S_DDF p2) { }
EXPORT void f3_V_FSS_DDD(float p0, struct S_DDD p1, struct S_DDD p2) { }
EXPORT void f3_V_FSS_DDP(float p0, struct S_DDP p1, struct S_DDP p2) { }
EXPORT void f3_V_FSS_DPI(float p0, struct S_DPI p1, struct S_DPI p2) { }
EXPORT void f3_V_FSS_DPF(float p0, struct S_DPF p1, struct S_DPF p2) { }
EXPORT void f3_V_FSS_DPD(float p0, struct S_DPD p1, struct S_DPD p2) { }
EXPORT void f3_V_FSS_DPP(float p0, struct S_DPP p1, struct S_DPP p2) { }
EXPORT void f3_V_FSS_PII(float p0, struct S_PII p1, struct S_PII p2) { }
EXPORT void f3_V_FSS_PIF(float p0, struct S_PIF p1, struct S_PIF p2) { }
EXPORT void f3_V_FSS_PID(float p0, struct S_PID p1, struct S_PID p2) { }
EXPORT void f3_V_FSS_PIP(float p0, struct S_PIP p1, struct S_PIP p2) { }
EXPORT void f3_V_FSS_PFI(float p0, struct S_PFI p1, struct S_PFI p2) { }
EXPORT void f3_V_FSS_PFF(float p0, struct S_PFF p1, struct S_PFF p2) { }
EXPORT void f3_V_FSS_PFD(float p0, struct S_PFD p1, struct S_PFD p2) { }
EXPORT void f3_V_FSS_PFP(float p0, struct S_PFP p1, struct S_PFP p2) { }
EXPORT void f3_V_FSS_PDI(float p0, struct S_PDI p1, struct S_PDI p2) { }
EXPORT void f3_V_FSS_PDF(float p0, struct S_PDF p1, struct S_PDF p2) { }
EXPORT void f3_V_FSS_PDD(float p0, struct S_PDD p1, struct S_PDD p2) { }
EXPORT void f4_V_FSS_PDP(float p0, struct S_PDP p1, struct S_PDP p2) { }
EXPORT void f4_V_FSS_PPI(float p0, struct S_PPI p1, struct S_PPI p2) { }
EXPORT void f4_V_FSS_PPF(float p0, struct S_PPF p1, struct S_PPF p2) { }
EXPORT void f4_V_FSS_PPD(float p0, struct S_PPD p1, struct S_PPD p2) { }
EXPORT void f4_V_FSS_PPP(float p0, struct S_PPP p1, struct S_PPP p2) { }
EXPORT void f4_V_DII_(double p0, int p1, int p2) { }
EXPORT void f4_V_DIF_(double p0, int p1, float p2) { }
EXPORT void f4_V_DID_(double p0, int p1, double p2) { }
EXPORT void f4_V_DIP_(double p0, int p1, void* p2) { }
EXPORT void f4_V_DIS_I(double p0, int p1, struct S_I p2) { }
EXPORT void f4_V_DIS_F(double p0, int p1, struct S_F p2) { }
EXPORT void f4_V_DIS_D(double p0, int p1, struct S_D p2) { }
EXPORT void f4_V_DIS_P(double p0, int p1, struct S_P p2) { }
EXPORT void f4_V_DIS_II(double p0, int p1, struct S_II p2) { }
EXPORT void f4_V_DIS_IF(double p0, int p1, struct S_IF p2) { }
EXPORT void f4_V_DIS_ID(double p0, int p1, struct S_ID p2) { }
EXPORT void f4_V_DIS_IP(double p0, int p1, struct S_IP p2) { }
EXPORT void f4_V_DIS_FI(double p0, int p1, struct S_FI p2) { }
EXPORT void f4_V_DIS_FF(double p0, int p1, struct S_FF p2) { }
EXPORT void f4_V_DIS_FD(double p0, int p1, struct S_FD p2) { }
EXPORT void f4_V_DIS_FP(double p0, int p1, struct S_FP p2) { }
EXPORT void f4_V_DIS_DI(double p0, int p1, struct S_DI p2) { }
EXPORT void f4_V_DIS_DF(double p0, int p1, struct S_DF p2) { }
EXPORT void f4_V_DIS_DD(double p0, int p1, struct S_DD p2) { }
EXPORT void f4_V_DIS_DP(double p0, int p1, struct S_DP p2) { }
EXPORT void f4_V_DIS_PI(double p0, int p1, struct S_PI p2) { }
EXPORT void f4_V_DIS_PF(double p0, int p1, struct S_PF p2) { }
EXPORT void f4_V_DIS_PD(double p0, int p1, struct S_PD p2) { }
EXPORT void f4_V_DIS_PP(double p0, int p1, struct S_PP p2) { }
EXPORT void f4_V_DIS_III(double p0, int p1, struct S_III p2) { }
EXPORT void f4_V_DIS_IIF(double p0, int p1, struct S_IIF p2) { }
EXPORT void f4_V_DIS_IID(double p0, int p1, struct S_IID p2) { }
EXPORT void f4_V_DIS_IIP(double p0, int p1, struct S_IIP p2) { }
EXPORT void f4_V_DIS_IFI(double p0, int p1, struct S_IFI p2) { }
EXPORT void f4_V_DIS_IFF(double p0, int p1, struct S_IFF p2) { }
EXPORT void f4_V_DIS_IFD(double p0, int p1, struct S_IFD p2) { }
EXPORT void f4_V_DIS_IFP(double p0, int p1, struct S_IFP p2) { }
EXPORT void f4_V_DIS_IDI(double p0, int p1, struct S_IDI p2) { }
EXPORT void f4_V_DIS_IDF(double p0, int p1, struct S_IDF p2) { }
EXPORT void f4_V_DIS_IDD(double p0, int p1, struct S_IDD p2) { }
EXPORT void f4_V_DIS_IDP(double p0, int p1, struct S_IDP p2) { }
EXPORT void f4_V_DIS_IPI(double p0, int p1, struct S_IPI p2) { }
EXPORT void f4_V_DIS_IPF(double p0, int p1, struct S_IPF p2) { }
EXPORT void f4_V_DIS_IPD(double p0, int p1, struct S_IPD p2) { }
EXPORT void f4_V_DIS_IPP(double p0, int p1, struct S_IPP p2) { }
EXPORT void f4_V_DIS_FII(double p0, int p1, struct S_FII p2) { }
EXPORT void f4_V_DIS_FIF(double p0, int p1, struct S_FIF p2) { }
EXPORT void f4_V_DIS_FID(double p0, int p1, struct S_FID p2) { }
EXPORT void f4_V_DIS_FIP(double p0, int p1, struct S_FIP p2) { }
EXPORT void f4_V_DIS_FFI(double p0, int p1, struct S_FFI p2) { }
EXPORT void f4_V_DIS_FFF(double p0, int p1, struct S_FFF p2) { }
EXPORT void f4_V_DIS_FFD(double p0, int p1, struct S_FFD p2) { }
EXPORT void f4_V_DIS_FFP(double p0, int p1, struct S_FFP p2) { }
EXPORT void f4_V_DIS_FDI(double p0, int p1, struct S_FDI p2) { }
EXPORT void f4_V_DIS_FDF(double p0, int p1, struct S_FDF p2) { }
EXPORT void f4_V_DIS_FDD(double p0, int p1, struct S_FDD p2) { }
EXPORT void f4_V_DIS_FDP(double p0, int p1, struct S_FDP p2) { }
EXPORT void f4_V_DIS_FPI(double p0, int p1, struct S_FPI p2) { }
EXPORT void f4_V_DIS_FPF(double p0, int p1, struct S_FPF p2) { }
EXPORT void f4_V_DIS_FPD(double p0, int p1, struct S_FPD p2) { }
EXPORT void f4_V_DIS_FPP(double p0, int p1, struct S_FPP p2) { }
EXPORT void f4_V_DIS_DII(double p0, int p1, struct S_DII p2) { }
EXPORT void f4_V_DIS_DIF(double p0, int p1, struct S_DIF p2) { }
EXPORT void f4_V_DIS_DID(double p0, int p1, struct S_DID p2) { }
EXPORT void f4_V_DIS_DIP(double p0, int p1, struct S_DIP p2) { }
EXPORT void f4_V_DIS_DFI(double p0, int p1, struct S_DFI p2) { }
EXPORT void f4_V_DIS_DFF(double p0, int p1, struct S_DFF p2) { }
EXPORT void f4_V_DIS_DFD(double p0, int p1, struct S_DFD p2) { }
EXPORT void f4_V_DIS_DFP(double p0, int p1, struct S_DFP p2) { }
EXPORT void f4_V_DIS_DDI(double p0, int p1, struct S_DDI p2) { }
EXPORT void f4_V_DIS_DDF(double p0, int p1, struct S_DDF p2) { }
EXPORT void f4_V_DIS_DDD(double p0, int p1, struct S_DDD p2) { }
EXPORT void f4_V_DIS_DDP(double p0, int p1, struct S_DDP p2) { }
EXPORT void f4_V_DIS_DPI(double p0, int p1, struct S_DPI p2) { }
EXPORT void f4_V_DIS_DPF(double p0, int p1, struct S_DPF p2) { }
EXPORT void f4_V_DIS_DPD(double p0, int p1, struct S_DPD p2) { }
EXPORT void f4_V_DIS_DPP(double p0, int p1, struct S_DPP p2) { }
EXPORT void f4_V_DIS_PII(double p0, int p1, struct S_PII p2) { }
EXPORT void f4_V_DIS_PIF(double p0, int p1, struct S_PIF p2) { }
EXPORT void f4_V_DIS_PID(double p0, int p1, struct S_PID p2) { }
EXPORT void f4_V_DIS_PIP(double p0, int p1, struct S_PIP p2) { }
EXPORT void f4_V_DIS_PFI(double p0, int p1, struct S_PFI p2) { }
EXPORT void f4_V_DIS_PFF(double p0, int p1, struct S_PFF p2) { }
EXPORT void f4_V_DIS_PFD(double p0, int p1, struct S_PFD p2) { }
EXPORT void f4_V_DIS_PFP(double p0, int p1, struct S_PFP p2) { }
EXPORT void f4_V_DIS_PDI(double p0, int p1, struct S_PDI p2) { }
EXPORT void f4_V_DIS_PDF(double p0, int p1, struct S_PDF p2) { }
EXPORT void f4_V_DIS_PDD(double p0, int p1, struct S_PDD p2) { }
EXPORT void f4_V_DIS_PDP(double p0, int p1, struct S_PDP p2) { }
EXPORT void f4_V_DIS_PPI(double p0, int p1, struct S_PPI p2) { }
EXPORT void f4_V_DIS_PPF(double p0, int p1, struct S_PPF p2) { }
EXPORT void f4_V_DIS_PPD(double p0, int p1, struct S_PPD p2) { }
EXPORT void f4_V_DIS_PPP(double p0, int p1, struct S_PPP p2) { }
EXPORT void f4_V_DFI_(double p0, float p1, int p2) { }
EXPORT void f4_V_DFF_(double p0, float p1, float p2) { }
EXPORT void f4_V_DFD_(double p0, float p1, double p2) { }
EXPORT void f4_V_DFP_(double p0, float p1, void* p2) { }
EXPORT void f4_V_DFS_I(double p0, float p1, struct S_I p2) { }
EXPORT void f4_V_DFS_F(double p0, float p1, struct S_F p2) { }
EXPORT void f4_V_DFS_D(double p0, float p1, struct S_D p2) { }
EXPORT void f4_V_DFS_P(double p0, float p1, struct S_P p2) { }
EXPORT void f4_V_DFS_II(double p0, float p1, struct S_II p2) { }
EXPORT void f4_V_DFS_IF(double p0, float p1, struct S_IF p2) { }
EXPORT void f4_V_DFS_ID(double p0, float p1, struct S_ID p2) { }
EXPORT void f4_V_DFS_IP(double p0, float p1, struct S_IP p2) { }
EXPORT void f4_V_DFS_FI(double p0, float p1, struct S_FI p2) { }
EXPORT void f4_V_DFS_FF(double p0, float p1, struct S_FF p2) { }
EXPORT void f4_V_DFS_FD(double p0, float p1, struct S_FD p2) { }
EXPORT void f4_V_DFS_FP(double p0, float p1, struct S_FP p2) { }
EXPORT void f4_V_DFS_DI(double p0, float p1, struct S_DI p2) { }
EXPORT void f4_V_DFS_DF(double p0, float p1, struct S_DF p2) { }
EXPORT void f4_V_DFS_DD(double p0, float p1, struct S_DD p2) { }
EXPORT void f4_V_DFS_DP(double p0, float p1, struct S_DP p2) { }
EXPORT void f4_V_DFS_PI(double p0, float p1, struct S_PI p2) { }
EXPORT void f4_V_DFS_PF(double p0, float p1, struct S_PF p2) { }
EXPORT void f4_V_DFS_PD(double p0, float p1, struct S_PD p2) { }
EXPORT void f4_V_DFS_PP(double p0, float p1, struct S_PP p2) { }
EXPORT void f4_V_DFS_III(double p0, float p1, struct S_III p2) { }
EXPORT void f4_V_DFS_IIF(double p0, float p1, struct S_IIF p2) { }
EXPORT void f4_V_DFS_IID(double p0, float p1, struct S_IID p2) { }
EXPORT void f4_V_DFS_IIP(double p0, float p1, struct S_IIP p2) { }
EXPORT void f4_V_DFS_IFI(double p0, float p1, struct S_IFI p2) { }
EXPORT void f4_V_DFS_IFF(double p0, float p1, struct S_IFF p2) { }
EXPORT void f4_V_DFS_IFD(double p0, float p1, struct S_IFD p2) { }
EXPORT void f4_V_DFS_IFP(double p0, float p1, struct S_IFP p2) { }
EXPORT void f4_V_DFS_IDI(double p0, float p1, struct S_IDI p2) { }
EXPORT void f4_V_DFS_IDF(double p0, float p1, struct S_IDF p2) { }
EXPORT void f4_V_DFS_IDD(double p0, float p1, struct S_IDD p2) { }
EXPORT void f4_V_DFS_IDP(double p0, float p1, struct S_IDP p2) { }
EXPORT void f4_V_DFS_IPI(double p0, float p1, struct S_IPI p2) { }
EXPORT void f4_V_DFS_IPF(double p0, float p1, struct S_IPF p2) { }
EXPORT void f4_V_DFS_IPD(double p0, float p1, struct S_IPD p2) { }
EXPORT void f4_V_DFS_IPP(double p0, float p1, struct S_IPP p2) { }
EXPORT void f4_V_DFS_FII(double p0, float p1, struct S_FII p2) { }
EXPORT void f4_V_DFS_FIF(double p0, float p1, struct S_FIF p2) { }
EXPORT void f4_V_DFS_FID(double p0, float p1, struct S_FID p2) { }
EXPORT void f4_V_DFS_FIP(double p0, float p1, struct S_FIP p2) { }
EXPORT void f4_V_DFS_FFI(double p0, float p1, struct S_FFI p2) { }
EXPORT void f4_V_DFS_FFF(double p0, float p1, struct S_FFF p2) { }
EXPORT void f4_V_DFS_FFD(double p0, float p1, struct S_FFD p2) { }
EXPORT void f4_V_DFS_FFP(double p0, float p1, struct S_FFP p2) { }
EXPORT void f4_V_DFS_FDI(double p0, float p1, struct S_FDI p2) { }
EXPORT void f4_V_DFS_FDF(double p0, float p1, struct S_FDF p2) { }
EXPORT void f4_V_DFS_FDD(double p0, float p1, struct S_FDD p2) { }
EXPORT void f4_V_DFS_FDP(double p0, float p1, struct S_FDP p2) { }
EXPORT void f4_V_DFS_FPI(double p0, float p1, struct S_FPI p2) { }
EXPORT void f4_V_DFS_FPF(double p0, float p1, struct S_FPF p2) { }
EXPORT void f4_V_DFS_FPD(double p0, float p1, struct S_FPD p2) { }
EXPORT void f4_V_DFS_FPP(double p0, float p1, struct S_FPP p2) { }
EXPORT void f4_V_DFS_DII(double p0, float p1, struct S_DII p2) { }
EXPORT void f4_V_DFS_DIF(double p0, float p1, struct S_DIF p2) { }
EXPORT void f4_V_DFS_DID(double p0, float p1, struct S_DID p2) { }
EXPORT void f4_V_DFS_DIP(double p0, float p1, struct S_DIP p2) { }
EXPORT void f4_V_DFS_DFI(double p0, float p1, struct S_DFI p2) { }
EXPORT void f4_V_DFS_DFF(double p0, float p1, struct S_DFF p2) { }
EXPORT void f4_V_DFS_DFD(double p0, float p1, struct S_DFD p2) { }
EXPORT void f4_V_DFS_DFP(double p0, float p1, struct S_DFP p2) { }
EXPORT void f4_V_DFS_DDI(double p0, float p1, struct S_DDI p2) { }
EXPORT void f4_V_DFS_DDF(double p0, float p1, struct S_DDF p2) { }
EXPORT void f4_V_DFS_DDD(double p0, float p1, struct S_DDD p2) { }
EXPORT void f4_V_DFS_DDP(double p0, float p1, struct S_DDP p2) { }
EXPORT void f4_V_DFS_DPI(double p0, float p1, struct S_DPI p2) { }
EXPORT void f4_V_DFS_DPF(double p0, float p1, struct S_DPF p2) { }
EXPORT void f4_V_DFS_DPD(double p0, float p1, struct S_DPD p2) { }
EXPORT void f4_V_DFS_DPP(double p0, float p1, struct S_DPP p2) { }
EXPORT void f4_V_DFS_PII(double p0, float p1, struct S_PII p2) { }
EXPORT void f4_V_DFS_PIF(double p0, float p1, struct S_PIF p2) { }
EXPORT void f4_V_DFS_PID(double p0, float p1, struct S_PID p2) { }
EXPORT void f4_V_DFS_PIP(double p0, float p1, struct S_PIP p2) { }
EXPORT void f4_V_DFS_PFI(double p0, float p1, struct S_PFI p2) { }
EXPORT void f4_V_DFS_PFF(double p0, float p1, struct S_PFF p2) { }
EXPORT void f4_V_DFS_PFD(double p0, float p1, struct S_PFD p2) { }
EXPORT void f4_V_DFS_PFP(double p0, float p1, struct S_PFP p2) { }
EXPORT void f4_V_DFS_PDI(double p0, float p1, struct S_PDI p2) { }
EXPORT void f4_V_DFS_PDF(double p0, float p1, struct S_PDF p2) { }
EXPORT void f4_V_DFS_PDD(double p0, float p1, struct S_PDD p2) { }
EXPORT void f4_V_DFS_PDP(double p0, float p1, struct S_PDP p2) { }
EXPORT void f4_V_DFS_PPI(double p0, float p1, struct S_PPI p2) { }
EXPORT void f4_V_DFS_PPF(double p0, float p1, struct S_PPF p2) { }
EXPORT void f4_V_DFS_PPD(double p0, float p1, struct S_PPD p2) { }
EXPORT void f4_V_DFS_PPP(double p0, float p1, struct S_PPP p2) { }
EXPORT void f4_V_DDI_(double p0, double p1, int p2) { }
EXPORT void f4_V_DDF_(double p0, double p1, float p2) { }
EXPORT void f4_V_DDD_(double p0, double p1, double p2) { }
EXPORT void f4_V_DDP_(double p0, double p1, void* p2) { }
EXPORT void f4_V_DDS_I(double p0, double p1, struct S_I p2) { }
EXPORT void f4_V_DDS_F(double p0, double p1, struct S_F p2) { }
EXPORT void f4_V_DDS_D(double p0, double p1, struct S_D p2) { }
EXPORT void f4_V_DDS_P(double p0, double p1, struct S_P p2) { }
EXPORT void f4_V_DDS_II(double p0, double p1, struct S_II p2) { }
EXPORT void f4_V_DDS_IF(double p0, double p1, struct S_IF p2) { }
EXPORT void f4_V_DDS_ID(double p0, double p1, struct S_ID p2) { }
EXPORT void f4_V_DDS_IP(double p0, double p1, struct S_IP p2) { }
EXPORT void f4_V_DDS_FI(double p0, double p1, struct S_FI p2) { }
EXPORT void f4_V_DDS_FF(double p0, double p1, struct S_FF p2) { }
EXPORT void f4_V_DDS_FD(double p0, double p1, struct S_FD p2) { }
EXPORT void f4_V_DDS_FP(double p0, double p1, struct S_FP p2) { }
EXPORT void f4_V_DDS_DI(double p0, double p1, struct S_DI p2) { }
EXPORT void f4_V_DDS_DF(double p0, double p1, struct S_DF p2) { }
EXPORT void f4_V_DDS_DD(double p0, double p1, struct S_DD p2) { }
EXPORT void f4_V_DDS_DP(double p0, double p1, struct S_DP p2) { }
EXPORT void f4_V_DDS_PI(double p0, double p1, struct S_PI p2) { }
EXPORT void f4_V_DDS_PF(double p0, double p1, struct S_PF p2) { }
EXPORT void f4_V_DDS_PD(double p0, double p1, struct S_PD p2) { }
EXPORT void f4_V_DDS_PP(double p0, double p1, struct S_PP p2) { }
EXPORT void f4_V_DDS_III(double p0, double p1, struct S_III p2) { }
EXPORT void f4_V_DDS_IIF(double p0, double p1, struct S_IIF p2) { }
EXPORT void f4_V_DDS_IID(double p0, double p1, struct S_IID p2) { }
EXPORT void f4_V_DDS_IIP(double p0, double p1, struct S_IIP p2) { }
EXPORT void f4_V_DDS_IFI(double p0, double p1, struct S_IFI p2) { }
EXPORT void f4_V_DDS_IFF(double p0, double p1, struct S_IFF p2) { }
EXPORT void f4_V_DDS_IFD(double p0, double p1, struct S_IFD p2) { }
EXPORT void f4_V_DDS_IFP(double p0, double p1, struct S_IFP p2) { }
EXPORT void f4_V_DDS_IDI(double p0, double p1, struct S_IDI p2) { }
EXPORT void f4_V_DDS_IDF(double p0, double p1, struct S_IDF p2) { }
EXPORT void f4_V_DDS_IDD(double p0, double p1, struct S_IDD p2) { }
EXPORT void f4_V_DDS_IDP(double p0, double p1, struct S_IDP p2) { }
EXPORT void f4_V_DDS_IPI(double p0, double p1, struct S_IPI p2) { }
EXPORT void f4_V_DDS_IPF(double p0, double p1, struct S_IPF p2) { }
EXPORT void f4_V_DDS_IPD(double p0, double p1, struct S_IPD p2) { }
EXPORT void f4_V_DDS_IPP(double p0, double p1, struct S_IPP p2) { }
EXPORT void f4_V_DDS_FII(double p0, double p1, struct S_FII p2) { }
EXPORT void f4_V_DDS_FIF(double p0, double p1, struct S_FIF p2) { }
EXPORT void f4_V_DDS_FID(double p0, double p1, struct S_FID p2) { }
EXPORT void f4_V_DDS_FIP(double p0, double p1, struct S_FIP p2) { }
EXPORT void f4_V_DDS_FFI(double p0, double p1, struct S_FFI p2) { }
EXPORT void f4_V_DDS_FFF(double p0, double p1, struct S_FFF p2) { }
EXPORT void f4_V_DDS_FFD(double p0, double p1, struct S_FFD p2) { }
EXPORT void f4_V_DDS_FFP(double p0, double p1, struct S_FFP p2) { }
EXPORT void f4_V_DDS_FDI(double p0, double p1, struct S_FDI p2) { }
EXPORT void f4_V_DDS_FDF(double p0, double p1, struct S_FDF p2) { }
EXPORT void f4_V_DDS_FDD(double p0, double p1, struct S_FDD p2) { }
EXPORT void f4_V_DDS_FDP(double p0, double p1, struct S_FDP p2) { }
EXPORT void f4_V_DDS_FPI(double p0, double p1, struct S_FPI p2) { }
EXPORT void f4_V_DDS_FPF(double p0, double p1, struct S_FPF p2) { }
EXPORT void f4_V_DDS_FPD(double p0, double p1, struct S_FPD p2) { }
EXPORT void f4_V_DDS_FPP(double p0, double p1, struct S_FPP p2) { }
EXPORT void f4_V_DDS_DII(double p0, double p1, struct S_DII p2) { }
EXPORT void f4_V_DDS_DIF(double p0, double p1, struct S_DIF p2) { }
EXPORT void f4_V_DDS_DID(double p0, double p1, struct S_DID p2) { }
EXPORT void f4_V_DDS_DIP(double p0, double p1, struct S_DIP p2) { }
EXPORT void f4_V_DDS_DFI(double p0, double p1, struct S_DFI p2) { }
EXPORT void f4_V_DDS_DFF(double p0, double p1, struct S_DFF p2) { }
EXPORT void f4_V_DDS_DFD(double p0, double p1, struct S_DFD p2) { }
EXPORT void f4_V_DDS_DFP(double p0, double p1, struct S_DFP p2) { }
EXPORT void f4_V_DDS_DDI(double p0, double p1, struct S_DDI p2) { }
EXPORT void f4_V_DDS_DDF(double p0, double p1, struct S_DDF p2) { }
EXPORT void f4_V_DDS_DDD(double p0, double p1, struct S_DDD p2) { }
EXPORT void f4_V_DDS_DDP(double p0, double p1, struct S_DDP p2) { }
EXPORT void f4_V_DDS_DPI(double p0, double p1, struct S_DPI p2) { }
EXPORT void f4_V_DDS_DPF(double p0, double p1, struct S_DPF p2) { }
EXPORT void f4_V_DDS_DPD(double p0, double p1, struct S_DPD p2) { }
EXPORT void f4_V_DDS_DPP(double p0, double p1, struct S_DPP p2) { }
EXPORT void f4_V_DDS_PII(double p0, double p1, struct S_PII p2) { }
EXPORT void f4_V_DDS_PIF(double p0, double p1, struct S_PIF p2) { }
EXPORT void f4_V_DDS_PID(double p0, double p1, struct S_PID p2) { }
EXPORT void f4_V_DDS_PIP(double p0, double p1, struct S_PIP p2) { }
EXPORT void f4_V_DDS_PFI(double p0, double p1, struct S_PFI p2) { }
EXPORT void f4_V_DDS_PFF(double p0, double p1, struct S_PFF p2) { }
EXPORT void f4_V_DDS_PFD(double p0, double p1, struct S_PFD p2) { }
EXPORT void f4_V_DDS_PFP(double p0, double p1, struct S_PFP p2) { }
EXPORT void f4_V_DDS_PDI(double p0, double p1, struct S_PDI p2) { }
EXPORT void f4_V_DDS_PDF(double p0, double p1, struct S_PDF p2) { }
EXPORT void f4_V_DDS_PDD(double p0, double p1, struct S_PDD p2) { }
EXPORT void f4_V_DDS_PDP(double p0, double p1, struct S_PDP p2) { }
EXPORT void f4_V_DDS_PPI(double p0, double p1, struct S_PPI p2) { }
EXPORT void f4_V_DDS_PPF(double p0, double p1, struct S_PPF p2) { }
EXPORT void f4_V_DDS_PPD(double p0, double p1, struct S_PPD p2) { }
EXPORT void f4_V_DDS_PPP(double p0, double p1, struct S_PPP p2) { }
EXPORT void f4_V_DPI_(double p0, void* p1, int p2) { }
EXPORT void f4_V_DPF_(double p0, void* p1, float p2) { }
EXPORT void f4_V_DPD_(double p0, void* p1, double p2) { }
EXPORT void f4_V_DPP_(double p0, void* p1, void* p2) { }
EXPORT void f4_V_DPS_I(double p0, void* p1, struct S_I p2) { }
EXPORT void f4_V_DPS_F(double p0, void* p1, struct S_F p2) { }
EXPORT void f4_V_DPS_D(double p0, void* p1, struct S_D p2) { }
EXPORT void f4_V_DPS_P(double p0, void* p1, struct S_P p2) { }
EXPORT void f4_V_DPS_II(double p0, void* p1, struct S_II p2) { }
EXPORT void f4_V_DPS_IF(double p0, void* p1, struct S_IF p2) { }
EXPORT void f4_V_DPS_ID(double p0, void* p1, struct S_ID p2) { }
EXPORT void f4_V_DPS_IP(double p0, void* p1, struct S_IP p2) { }
EXPORT void f4_V_DPS_FI(double p0, void* p1, struct S_FI p2) { }
EXPORT void f4_V_DPS_FF(double p0, void* p1, struct S_FF p2) { }
EXPORT void f4_V_DPS_FD(double p0, void* p1, struct S_FD p2) { }
EXPORT void f4_V_DPS_FP(double p0, void* p1, struct S_FP p2) { }
EXPORT void f4_V_DPS_DI(double p0, void* p1, struct S_DI p2) { }
EXPORT void f4_V_DPS_DF(double p0, void* p1, struct S_DF p2) { }
EXPORT void f4_V_DPS_DD(double p0, void* p1, struct S_DD p2) { }
EXPORT void f4_V_DPS_DP(double p0, void* p1, struct S_DP p2) { }
EXPORT void f4_V_DPS_PI(double p0, void* p1, struct S_PI p2) { }
EXPORT void f4_V_DPS_PF(double p0, void* p1, struct S_PF p2) { }
EXPORT void f4_V_DPS_PD(double p0, void* p1, struct S_PD p2) { }
EXPORT void f4_V_DPS_PP(double p0, void* p1, struct S_PP p2) { }
EXPORT void f4_V_DPS_III(double p0, void* p1, struct S_III p2) { }
EXPORT void f4_V_DPS_IIF(double p0, void* p1, struct S_IIF p2) { }
EXPORT void f4_V_DPS_IID(double p0, void* p1, struct S_IID p2) { }
EXPORT void f4_V_DPS_IIP(double p0, void* p1, struct S_IIP p2) { }
EXPORT void f4_V_DPS_IFI(double p0, void* p1, struct S_IFI p2) { }
EXPORT void f4_V_DPS_IFF(double p0, void* p1, struct S_IFF p2) { }
EXPORT void f4_V_DPS_IFD(double p0, void* p1, struct S_IFD p2) { }
EXPORT void f4_V_DPS_IFP(double p0, void* p1, struct S_IFP p2) { }
EXPORT void f4_V_DPS_IDI(double p0, void* p1, struct S_IDI p2) { }
EXPORT void f4_V_DPS_IDF(double p0, void* p1, struct S_IDF p2) { }
EXPORT void f4_V_DPS_IDD(double p0, void* p1, struct S_IDD p2) { }
EXPORT void f4_V_DPS_IDP(double p0, void* p1, struct S_IDP p2) { }
EXPORT void f4_V_DPS_IPI(double p0, void* p1, struct S_IPI p2) { }
EXPORT void f4_V_DPS_IPF(double p0, void* p1, struct S_IPF p2) { }
EXPORT void f4_V_DPS_IPD(double p0, void* p1, struct S_IPD p2) { }
EXPORT void f4_V_DPS_IPP(double p0, void* p1, struct S_IPP p2) { }
EXPORT void f4_V_DPS_FII(double p0, void* p1, struct S_FII p2) { }
EXPORT void f4_V_DPS_FIF(double p0, void* p1, struct S_FIF p2) { }
EXPORT void f4_V_DPS_FID(double p0, void* p1, struct S_FID p2) { }
EXPORT void f4_V_DPS_FIP(double p0, void* p1, struct S_FIP p2) { }
EXPORT void f4_V_DPS_FFI(double p0, void* p1, struct S_FFI p2) { }
EXPORT void f4_V_DPS_FFF(double p0, void* p1, struct S_FFF p2) { }
EXPORT void f4_V_DPS_FFD(double p0, void* p1, struct S_FFD p2) { }
EXPORT void f4_V_DPS_FFP(double p0, void* p1, struct S_FFP p2) { }
EXPORT void f4_V_DPS_FDI(double p0, void* p1, struct S_FDI p2) { }
EXPORT void f4_V_DPS_FDF(double p0, void* p1, struct S_FDF p2) { }
EXPORT void f4_V_DPS_FDD(double p0, void* p1, struct S_FDD p2) { }
EXPORT void f4_V_DPS_FDP(double p0, void* p1, struct S_FDP p2) { }
EXPORT void f4_V_DPS_FPI(double p0, void* p1, struct S_FPI p2) { }
EXPORT void f4_V_DPS_FPF(double p0, void* p1, struct S_FPF p2) { }
EXPORT void f4_V_DPS_FPD(double p0, void* p1, struct S_FPD p2) { }
EXPORT void f4_V_DPS_FPP(double p0, void* p1, struct S_FPP p2) { }
EXPORT void f4_V_DPS_DII(double p0, void* p1, struct S_DII p2) { }
EXPORT void f4_V_DPS_DIF(double p0, void* p1, struct S_DIF p2) { }
EXPORT void f4_V_DPS_DID(double p0, void* p1, struct S_DID p2) { }
EXPORT void f4_V_DPS_DIP(double p0, void* p1, struct S_DIP p2) { }
EXPORT void f4_V_DPS_DFI(double p0, void* p1, struct S_DFI p2) { }
EXPORT void f4_V_DPS_DFF(double p0, void* p1, struct S_DFF p2) { }
EXPORT void f4_V_DPS_DFD(double p0, void* p1, struct S_DFD p2) { }
EXPORT void f4_V_DPS_DFP(double p0, void* p1, struct S_DFP p2) { }
EXPORT void f4_V_DPS_DDI(double p0, void* p1, struct S_DDI p2) { }
EXPORT void f4_V_DPS_DDF(double p0, void* p1, struct S_DDF p2) { }
EXPORT void f4_V_DPS_DDD(double p0, void* p1, struct S_DDD p2) { }
EXPORT void f4_V_DPS_DDP(double p0, void* p1, struct S_DDP p2) { }
EXPORT void f4_V_DPS_DPI(double p0, void* p1, struct S_DPI p2) { }
EXPORT void f4_V_DPS_DPF(double p0, void* p1, struct S_DPF p2) { }
EXPORT void f4_V_DPS_DPD(double p0, void* p1, struct S_DPD p2) { }
EXPORT void f4_V_DPS_DPP(double p0, void* p1, struct S_DPP p2) { }
EXPORT void f4_V_DPS_PII(double p0, void* p1, struct S_PII p2) { }
EXPORT void f4_V_DPS_PIF(double p0, void* p1, struct S_PIF p2) { }
EXPORT void f4_V_DPS_PID(double p0, void* p1, struct S_PID p2) { }
EXPORT void f4_V_DPS_PIP(double p0, void* p1, struct S_PIP p2) { }
EXPORT void f4_V_DPS_PFI(double p0, void* p1, struct S_PFI p2) { }
EXPORT void f4_V_DPS_PFF(double p0, void* p1, struct S_PFF p2) { }
EXPORT void f4_V_DPS_PFD(double p0, void* p1, struct S_PFD p2) { }
EXPORT void f4_V_DPS_PFP(double p0, void* p1, struct S_PFP p2) { }
EXPORT void f4_V_DPS_PDI(double p0, void* p1, struct S_PDI p2) { }
EXPORT void f4_V_DPS_PDF(double p0, void* p1, struct S_PDF p2) { }
EXPORT void f4_V_DPS_PDD(double p0, void* p1, struct S_PDD p2) { }
EXPORT void f4_V_DPS_PDP(double p0, void* p1, struct S_PDP p2) { }
EXPORT void f4_V_DPS_PPI(double p0, void* p1, struct S_PPI p2) { }
EXPORT void f4_V_DPS_PPF(double p0, void* p1, struct S_PPF p2) { }
EXPORT void f4_V_DPS_PPD(double p0, void* p1, struct S_PPD p2) { }
EXPORT void f4_V_DPS_PPP(double p0, void* p1, struct S_PPP p2) { }
EXPORT void f4_V_DSI_I(double p0, struct S_I p1, int p2) { }
EXPORT void f4_V_DSI_F(double p0, struct S_F p1, int p2) { }
EXPORT void f4_V_DSI_D(double p0, struct S_D p1, int p2) { }
EXPORT void f4_V_DSI_P(double p0, struct S_P p1, int p2) { }
EXPORT void f4_V_DSI_II(double p0, struct S_II p1, int p2) { }
EXPORT void f4_V_DSI_IF(double p0, struct S_IF p1, int p2) { }
EXPORT void f4_V_DSI_ID(double p0, struct S_ID p1, int p2) { }
EXPORT void f4_V_DSI_IP(double p0, struct S_IP p1, int p2) { }
EXPORT void f4_V_DSI_FI(double p0, struct S_FI p1, int p2) { }
EXPORT void f4_V_DSI_FF(double p0, struct S_FF p1, int p2) { }
EXPORT void f4_V_DSI_FD(double p0, struct S_FD p1, int p2) { }
EXPORT void f4_V_DSI_FP(double p0, struct S_FP p1, int p2) { }
EXPORT void f4_V_DSI_DI(double p0, struct S_DI p1, int p2) { }
EXPORT void f4_V_DSI_DF(double p0, struct S_DF p1, int p2) { }
EXPORT void f4_V_DSI_DD(double p0, struct S_DD p1, int p2) { }
EXPORT void f4_V_DSI_DP(double p0, struct S_DP p1, int p2) { }
EXPORT void f4_V_DSI_PI(double p0, struct S_PI p1, int p2) { }
EXPORT void f4_V_DSI_PF(double p0, struct S_PF p1, int p2) { }
EXPORT void f4_V_DSI_PD(double p0, struct S_PD p1, int p2) { }
EXPORT void f4_V_DSI_PP(double p0, struct S_PP p1, int p2) { }
EXPORT void f4_V_DSI_III(double p0, struct S_III p1, int p2) { }
EXPORT void f4_V_DSI_IIF(double p0, struct S_IIF p1, int p2) { }
EXPORT void f4_V_DSI_IID(double p0, struct S_IID p1, int p2) { }
EXPORT void f4_V_DSI_IIP(double p0, struct S_IIP p1, int p2) { }
EXPORT void f4_V_DSI_IFI(double p0, struct S_IFI p1, int p2) { }
EXPORT void f4_V_DSI_IFF(double p0, struct S_IFF p1, int p2) { }
EXPORT void f4_V_DSI_IFD(double p0, struct S_IFD p1, int p2) { }
EXPORT void f4_V_DSI_IFP(double p0, struct S_IFP p1, int p2) { }
EXPORT void f4_V_DSI_IDI(double p0, struct S_IDI p1, int p2) { }
EXPORT void f4_V_DSI_IDF(double p0, struct S_IDF p1, int p2) { }
EXPORT void f4_V_DSI_IDD(double p0, struct S_IDD p1, int p2) { }
EXPORT void f4_V_DSI_IDP(double p0, struct S_IDP p1, int p2) { }
EXPORT void f4_V_DSI_IPI(double p0, struct S_IPI p1, int p2) { }
EXPORT void f4_V_DSI_IPF(double p0, struct S_IPF p1, int p2) { }
EXPORT void f4_V_DSI_IPD(double p0, struct S_IPD p1, int p2) { }
EXPORT void f4_V_DSI_IPP(double p0, struct S_IPP p1, int p2) { }
EXPORT void f4_V_DSI_FII(double p0, struct S_FII p1, int p2) { }
EXPORT void f4_V_DSI_FIF(double p0, struct S_FIF p1, int p2) { }
EXPORT void f4_V_DSI_FID(double p0, struct S_FID p1, int p2) { }
EXPORT void f4_V_DSI_FIP(double p0, struct S_FIP p1, int p2) { }
EXPORT void f4_V_DSI_FFI(double p0, struct S_FFI p1, int p2) { }
EXPORT void f4_V_DSI_FFF(double p0, struct S_FFF p1, int p2) { }
EXPORT void f4_V_DSI_FFD(double p0, struct S_FFD p1, int p2) { }
EXPORT void f4_V_DSI_FFP(double p0, struct S_FFP p1, int p2) { }
EXPORT void f4_V_DSI_FDI(double p0, struct S_FDI p1, int p2) { }
EXPORT void f4_V_DSI_FDF(double p0, struct S_FDF p1, int p2) { }
EXPORT void f4_V_DSI_FDD(double p0, struct S_FDD p1, int p2) { }
EXPORT void f4_V_DSI_FDP(double p0, struct S_FDP p1, int p2) { }
EXPORT void f4_V_DSI_FPI(double p0, struct S_FPI p1, int p2) { }
EXPORT void f4_V_DSI_FPF(double p0, struct S_FPF p1, int p2) { }
EXPORT void f4_V_DSI_FPD(double p0, struct S_FPD p1, int p2) { }
EXPORT void f4_V_DSI_FPP(double p0, struct S_FPP p1, int p2) { }
EXPORT void f4_V_DSI_DII(double p0, struct S_DII p1, int p2) { }
EXPORT void f4_V_DSI_DIF(double p0, struct S_DIF p1, int p2) { }
EXPORT void f4_V_DSI_DID(double p0, struct S_DID p1, int p2) { }
EXPORT void f4_V_DSI_DIP(double p0, struct S_DIP p1, int p2) { }
EXPORT void f4_V_DSI_DFI(double p0, struct S_DFI p1, int p2) { }
EXPORT void f4_V_DSI_DFF(double p0, struct S_DFF p1, int p2) { }
EXPORT void f4_V_DSI_DFD(double p0, struct S_DFD p1, int p2) { }
EXPORT void f4_V_DSI_DFP(double p0, struct S_DFP p1, int p2) { }
EXPORT void f4_V_DSI_DDI(double p0, struct S_DDI p1, int p2) { }
EXPORT void f4_V_DSI_DDF(double p0, struct S_DDF p1, int p2) { }
EXPORT void f4_V_DSI_DDD(double p0, struct S_DDD p1, int p2) { }
EXPORT void f4_V_DSI_DDP(double p0, struct S_DDP p1, int p2) { }
EXPORT void f4_V_DSI_DPI(double p0, struct S_DPI p1, int p2) { }
EXPORT void f4_V_DSI_DPF(double p0, struct S_DPF p1, int p2) { }
EXPORT void f4_V_DSI_DPD(double p0, struct S_DPD p1, int p2) { }
EXPORT void f4_V_DSI_DPP(double p0, struct S_DPP p1, int p2) { }
EXPORT void f4_V_DSI_PII(double p0, struct S_PII p1, int p2) { }
EXPORT void f4_V_DSI_PIF(double p0, struct S_PIF p1, int p2) { }
EXPORT void f4_V_DSI_PID(double p0, struct S_PID p1, int p2) { }
EXPORT void f4_V_DSI_PIP(double p0, struct S_PIP p1, int p2) { }
EXPORT void f4_V_DSI_PFI(double p0, struct S_PFI p1, int p2) { }
EXPORT void f4_V_DSI_PFF(double p0, struct S_PFF p1, int p2) { }
EXPORT void f4_V_DSI_PFD(double p0, struct S_PFD p1, int p2) { }
EXPORT void f4_V_DSI_PFP(double p0, struct S_PFP p1, int p2) { }
EXPORT void f4_V_DSI_PDI(double p0, struct S_PDI p1, int p2) { }
EXPORT void f4_V_DSI_PDF(double p0, struct S_PDF p1, int p2) { }
EXPORT void f4_V_DSI_PDD(double p0, struct S_PDD p1, int p2) { }
EXPORT void f4_V_DSI_PDP(double p0, struct S_PDP p1, int p2) { }
EXPORT void f4_V_DSI_PPI(double p0, struct S_PPI p1, int p2) { }
EXPORT void f4_V_DSI_PPF(double p0, struct S_PPF p1, int p2) { }
EXPORT void f4_V_DSI_PPD(double p0, struct S_PPD p1, int p2) { }
EXPORT void f4_V_DSI_PPP(double p0, struct S_PPP p1, int p2) { }
EXPORT void f4_V_DSF_I(double p0, struct S_I p1, float p2) { }
EXPORT void f4_V_DSF_F(double p0, struct S_F p1, float p2) { }
EXPORT void f4_V_DSF_D(double p0, struct S_D p1, float p2) { }
EXPORT void f4_V_DSF_P(double p0, struct S_P p1, float p2) { }
EXPORT void f4_V_DSF_II(double p0, struct S_II p1, float p2) { }
EXPORT void f4_V_DSF_IF(double p0, struct S_IF p1, float p2) { }
EXPORT void f4_V_DSF_ID(double p0, struct S_ID p1, float p2) { }
EXPORT void f4_V_DSF_IP(double p0, struct S_IP p1, float p2) { }
EXPORT void f4_V_DSF_FI(double p0, struct S_FI p1, float p2) { }
EXPORT void f4_V_DSF_FF(double p0, struct S_FF p1, float p2) { }
EXPORT void f4_V_DSF_FD(double p0, struct S_FD p1, float p2) { }
EXPORT void f4_V_DSF_FP(double p0, struct S_FP p1, float p2) { }
EXPORT void f4_V_DSF_DI(double p0, struct S_DI p1, float p2) { }
EXPORT void f4_V_DSF_DF(double p0, struct S_DF p1, float p2) { }
EXPORT void f4_V_DSF_DD(double p0, struct S_DD p1, float p2) { }
EXPORT void f4_V_DSF_DP(double p0, struct S_DP p1, float p2) { }
EXPORT void f4_V_DSF_PI(double p0, struct S_PI p1, float p2) { }
EXPORT void f4_V_DSF_PF(double p0, struct S_PF p1, float p2) { }
EXPORT void f4_V_DSF_PD(double p0, struct S_PD p1, float p2) { }
EXPORT void f4_V_DSF_PP(double p0, struct S_PP p1, float p2) { }
EXPORT void f4_V_DSF_III(double p0, struct S_III p1, float p2) { }
EXPORT void f4_V_DSF_IIF(double p0, struct S_IIF p1, float p2) { }
EXPORT void f4_V_DSF_IID(double p0, struct S_IID p1, float p2) { }
EXPORT void f4_V_DSF_IIP(double p0, struct S_IIP p1, float p2) { }
EXPORT void f4_V_DSF_IFI(double p0, struct S_IFI p1, float p2) { }
EXPORT void f4_V_DSF_IFF(double p0, struct S_IFF p1, float p2) { }
EXPORT void f4_V_DSF_IFD(double p0, struct S_IFD p1, float p2) { }
EXPORT void f4_V_DSF_IFP(double p0, struct S_IFP p1, float p2) { }
EXPORT void f4_V_DSF_IDI(double p0, struct S_IDI p1, float p2) { }
EXPORT void f4_V_DSF_IDF(double p0, struct S_IDF p1, float p2) { }
EXPORT void f4_V_DSF_IDD(double p0, struct S_IDD p1, float p2) { }
EXPORT void f4_V_DSF_IDP(double p0, struct S_IDP p1, float p2) { }
EXPORT void f4_V_DSF_IPI(double p0, struct S_IPI p1, float p2) { }
EXPORT void f4_V_DSF_IPF(double p0, struct S_IPF p1, float p2) { }
EXPORT void f4_V_DSF_IPD(double p0, struct S_IPD p1, float p2) { }
EXPORT void f4_V_DSF_IPP(double p0, struct S_IPP p1, float p2) { }
EXPORT void f4_V_DSF_FII(double p0, struct S_FII p1, float p2) { }
EXPORT void f4_V_DSF_FIF(double p0, struct S_FIF p1, float p2) { }
EXPORT void f4_V_DSF_FID(double p0, struct S_FID p1, float p2) { }
EXPORT void f4_V_DSF_FIP(double p0, struct S_FIP p1, float p2) { }
EXPORT void f4_V_DSF_FFI(double p0, struct S_FFI p1, float p2) { }
EXPORT void f4_V_DSF_FFF(double p0, struct S_FFF p1, float p2) { }
EXPORT void f4_V_DSF_FFD(double p0, struct S_FFD p1, float p2) { }
EXPORT void f4_V_DSF_FFP(double p0, struct S_FFP p1, float p2) { }
EXPORT void f4_V_DSF_FDI(double p0, struct S_FDI p1, float p2) { }
EXPORT void f4_V_DSF_FDF(double p0, struct S_FDF p1, float p2) { }
EXPORT void f4_V_DSF_FDD(double p0, struct S_FDD p1, float p2) { }
EXPORT void f4_V_DSF_FDP(double p0, struct S_FDP p1, float p2) { }
EXPORT void f4_V_DSF_FPI(double p0, struct S_FPI p1, float p2) { }
EXPORT void f4_V_DSF_FPF(double p0, struct S_FPF p1, float p2) { }
EXPORT void f4_V_DSF_FPD(double p0, struct S_FPD p1, float p2) { }
EXPORT void f4_V_DSF_FPP(double p0, struct S_FPP p1, float p2) { }
EXPORT void f4_V_DSF_DII(double p0, struct S_DII p1, float p2) { }
EXPORT void f4_V_DSF_DIF(double p0, struct S_DIF p1, float p2) { }
EXPORT void f4_V_DSF_DID(double p0, struct S_DID p1, float p2) { }
EXPORT void f4_V_DSF_DIP(double p0, struct S_DIP p1, float p2) { }
EXPORT void f4_V_DSF_DFI(double p0, struct S_DFI p1, float p2) { }
EXPORT void f4_V_DSF_DFF(double p0, struct S_DFF p1, float p2) { }
EXPORT void f4_V_DSF_DFD(double p0, struct S_DFD p1, float p2) { }
EXPORT void f4_V_DSF_DFP(double p0, struct S_DFP p1, float p2) { }
EXPORT void f4_V_DSF_DDI(double p0, struct S_DDI p1, float p2) { }
EXPORT void f4_V_DSF_DDF(double p0, struct S_DDF p1, float p2) { }
EXPORT void f4_V_DSF_DDD(double p0, struct S_DDD p1, float p2) { }
EXPORT void f4_V_DSF_DDP(double p0, struct S_DDP p1, float p2) { }
EXPORT void f4_V_DSF_DPI(double p0, struct S_DPI p1, float p2) { }
EXPORT void f4_V_DSF_DPF(double p0, struct S_DPF p1, float p2) { }
EXPORT void f4_V_DSF_DPD(double p0, struct S_DPD p1, float p2) { }
EXPORT void f4_V_DSF_DPP(double p0, struct S_DPP p1, float p2) { }
EXPORT void f4_V_DSF_PII(double p0, struct S_PII p1, float p2) { }
EXPORT void f4_V_DSF_PIF(double p0, struct S_PIF p1, float p2) { }
EXPORT void f4_V_DSF_PID(double p0, struct S_PID p1, float p2) { }
EXPORT void f4_V_DSF_PIP(double p0, struct S_PIP p1, float p2) { }
EXPORT void f4_V_DSF_PFI(double p0, struct S_PFI p1, float p2) { }
EXPORT void f4_V_DSF_PFF(double p0, struct S_PFF p1, float p2) { }
EXPORT void f4_V_DSF_PFD(double p0, struct S_PFD p1, float p2) { }
EXPORT void f4_V_DSF_PFP(double p0, struct S_PFP p1, float p2) { }
EXPORT void f4_V_DSF_PDI(double p0, struct S_PDI p1, float p2) { }
EXPORT void f4_V_DSF_PDF(double p0, struct S_PDF p1, float p2) { }
EXPORT void f4_V_DSF_PDD(double p0, struct S_PDD p1, float p2) { }
EXPORT void f4_V_DSF_PDP(double p0, struct S_PDP p1, float p2) { }
EXPORT void f4_V_DSF_PPI(double p0, struct S_PPI p1, float p2) { }
EXPORT void f4_V_DSF_PPF(double p0, struct S_PPF p1, float p2) { }
EXPORT void f4_V_DSF_PPD(double p0, struct S_PPD p1, float p2) { }
EXPORT void f4_V_DSF_PPP(double p0, struct S_PPP p1, float p2) { }
EXPORT void f4_V_DSD_I(double p0, struct S_I p1, double p2) { }
EXPORT void f4_V_DSD_F(double p0, struct S_F p1, double p2) { }
EXPORT void f4_V_DSD_D(double p0, struct S_D p1, double p2) { }
EXPORT void f4_V_DSD_P(double p0, struct S_P p1, double p2) { }
EXPORT void f4_V_DSD_II(double p0, struct S_II p1, double p2) { }
EXPORT void f4_V_DSD_IF(double p0, struct S_IF p1, double p2) { }
EXPORT void f4_V_DSD_ID(double p0, struct S_ID p1, double p2) { }
EXPORT void f4_V_DSD_IP(double p0, struct S_IP p1, double p2) { }
EXPORT void f4_V_DSD_FI(double p0, struct S_FI p1, double p2) { }
EXPORT void f4_V_DSD_FF(double p0, struct S_FF p1, double p2) { }
EXPORT void f4_V_DSD_FD(double p0, struct S_FD p1, double p2) { }
EXPORT void f4_V_DSD_FP(double p0, struct S_FP p1, double p2) { }
EXPORT void f4_V_DSD_DI(double p0, struct S_DI p1, double p2) { }
EXPORT void f4_V_DSD_DF(double p0, struct S_DF p1, double p2) { }
EXPORT void f4_V_DSD_DD(double p0, struct S_DD p1, double p2) { }
EXPORT void f4_V_DSD_DP(double p0, struct S_DP p1, double p2) { }
EXPORT void f4_V_DSD_PI(double p0, struct S_PI p1, double p2) { }
EXPORT void f4_V_DSD_PF(double p0, struct S_PF p1, double p2) { }
EXPORT void f4_V_DSD_PD(double p0, struct S_PD p1, double p2) { }
EXPORT void f4_V_DSD_PP(double p0, struct S_PP p1, double p2) { }
EXPORT void f4_V_DSD_III(double p0, struct S_III p1, double p2) { }
EXPORT void f4_V_DSD_IIF(double p0, struct S_IIF p1, double p2) { }
EXPORT void f4_V_DSD_IID(double p0, struct S_IID p1, double p2) { }
EXPORT void f4_V_DSD_IIP(double p0, struct S_IIP p1, double p2) { }
EXPORT void f4_V_DSD_IFI(double p0, struct S_IFI p1, double p2) { }
EXPORT void f4_V_DSD_IFF(double p0, struct S_IFF p1, double p2) { }
EXPORT void f4_V_DSD_IFD(double p0, struct S_IFD p1, double p2) { }
EXPORT void f4_V_DSD_IFP(double p0, struct S_IFP p1, double p2) { }
EXPORT void f4_V_DSD_IDI(double p0, struct S_IDI p1, double p2) { }
EXPORT void f4_V_DSD_IDF(double p0, struct S_IDF p1, double p2) { }
EXPORT void f4_V_DSD_IDD(double p0, struct S_IDD p1, double p2) { }
EXPORT void f4_V_DSD_IDP(double p0, struct S_IDP p1, double p2) { }
EXPORT void f4_V_DSD_IPI(double p0, struct S_IPI p1, double p2) { }
EXPORT void f4_V_DSD_IPF(double p0, struct S_IPF p1, double p2) { }
EXPORT void f4_V_DSD_IPD(double p0, struct S_IPD p1, double p2) { }
EXPORT void f4_V_DSD_IPP(double p0, struct S_IPP p1, double p2) { }
EXPORT void f4_V_DSD_FII(double p0, struct S_FII p1, double p2) { }
EXPORT void f4_V_DSD_FIF(double p0, struct S_FIF p1, double p2) { }
EXPORT void f4_V_DSD_FID(double p0, struct S_FID p1, double p2) { }
EXPORT void f4_V_DSD_FIP(double p0, struct S_FIP p1, double p2) { }
EXPORT void f4_V_DSD_FFI(double p0, struct S_FFI p1, double p2) { }
EXPORT void f4_V_DSD_FFF(double p0, struct S_FFF p1, double p2) { }
EXPORT void f4_V_DSD_FFD(double p0, struct S_FFD p1, double p2) { }
EXPORT void f4_V_DSD_FFP(double p0, struct S_FFP p1, double p2) { }
EXPORT void f4_V_DSD_FDI(double p0, struct S_FDI p1, double p2) { }
EXPORT void f4_V_DSD_FDF(double p0, struct S_FDF p1, double p2) { }
EXPORT void f4_V_DSD_FDD(double p0, struct S_FDD p1, double p2) { }
EXPORT void f4_V_DSD_FDP(double p0, struct S_FDP p1, double p2) { }
EXPORT void f4_V_DSD_FPI(double p0, struct S_FPI p1, double p2) { }
EXPORT void f4_V_DSD_FPF(double p0, struct S_FPF p1, double p2) { }
EXPORT void f4_V_DSD_FPD(double p0, struct S_FPD p1, double p2) { }
EXPORT void f4_V_DSD_FPP(double p0, struct S_FPP p1, double p2) { }
EXPORT void f4_V_DSD_DII(double p0, struct S_DII p1, double p2) { }
EXPORT void f4_V_DSD_DIF(double p0, struct S_DIF p1, double p2) { }
EXPORT void f4_V_DSD_DID(double p0, struct S_DID p1, double p2) { }
EXPORT void f4_V_DSD_DIP(double p0, struct S_DIP p1, double p2) { }
EXPORT void f4_V_DSD_DFI(double p0, struct S_DFI p1, double p2) { }
EXPORT void f4_V_DSD_DFF(double p0, struct S_DFF p1, double p2) { }
EXPORT void f4_V_DSD_DFD(double p0, struct S_DFD p1, double p2) { }
EXPORT void f4_V_DSD_DFP(double p0, struct S_DFP p1, double p2) { }
EXPORT void f4_V_DSD_DDI(double p0, struct S_DDI p1, double p2) { }
EXPORT void f4_V_DSD_DDF(double p0, struct S_DDF p1, double p2) { }
EXPORT void f4_V_DSD_DDD(double p0, struct S_DDD p1, double p2) { }
EXPORT void f4_V_DSD_DDP(double p0, struct S_DDP p1, double p2) { }
EXPORT void f4_V_DSD_DPI(double p0, struct S_DPI p1, double p2) { }
EXPORT void f4_V_DSD_DPF(double p0, struct S_DPF p1, double p2) { }
EXPORT void f4_V_DSD_DPD(double p0, struct S_DPD p1, double p2) { }
EXPORT void f4_V_DSD_DPP(double p0, struct S_DPP p1, double p2) { }
EXPORT void f4_V_DSD_PII(double p0, struct S_PII p1, double p2) { }
EXPORT void f4_V_DSD_PIF(double p0, struct S_PIF p1, double p2) { }
EXPORT void f4_V_DSD_PID(double p0, struct S_PID p1, double p2) { }
EXPORT void f4_V_DSD_PIP(double p0, struct S_PIP p1, double p2) { }
EXPORT void f4_V_DSD_PFI(double p0, struct S_PFI p1, double p2) { }
EXPORT void f4_V_DSD_PFF(double p0, struct S_PFF p1, double p2) { }
EXPORT void f4_V_DSD_PFD(double p0, struct S_PFD p1, double p2) { }
EXPORT void f5_V_DSD_PFP(double p0, struct S_PFP p1, double p2) { }
EXPORT void f5_V_DSD_PDI(double p0, struct S_PDI p1, double p2) { }
EXPORT void f5_V_DSD_PDF(double p0, struct S_PDF p1, double p2) { }
EXPORT void f5_V_DSD_PDD(double p0, struct S_PDD p1, double p2) { }
EXPORT void f5_V_DSD_PDP(double p0, struct S_PDP p1, double p2) { }
EXPORT void f5_V_DSD_PPI(double p0, struct S_PPI p1, double p2) { }
EXPORT void f5_V_DSD_PPF(double p0, struct S_PPF p1, double p2) { }
EXPORT void f5_V_DSD_PPD(double p0, struct S_PPD p1, double p2) { }
EXPORT void f5_V_DSD_PPP(double p0, struct S_PPP p1, double p2) { }
EXPORT void f5_V_DSP_I(double p0, struct S_I p1, void* p2) { }
EXPORT void f5_V_DSP_F(double p0, struct S_F p1, void* p2) { }
EXPORT void f5_V_DSP_D(double p0, struct S_D p1, void* p2) { }
EXPORT void f5_V_DSP_P(double p0, struct S_P p1, void* p2) { }
EXPORT void f5_V_DSP_II(double p0, struct S_II p1, void* p2) { }
EXPORT void f5_V_DSP_IF(double p0, struct S_IF p1, void* p2) { }
EXPORT void f5_V_DSP_ID(double p0, struct S_ID p1, void* p2) { }
EXPORT void f5_V_DSP_IP(double p0, struct S_IP p1, void* p2) { }
EXPORT void f5_V_DSP_FI(double p0, struct S_FI p1, void* p2) { }
EXPORT void f5_V_DSP_FF(double p0, struct S_FF p1, void* p2) { }
EXPORT void f5_V_DSP_FD(double p0, struct S_FD p1, void* p2) { }
EXPORT void f5_V_DSP_FP(double p0, struct S_FP p1, void* p2) { }
EXPORT void f5_V_DSP_DI(double p0, struct S_DI p1, void* p2) { }
EXPORT void f5_V_DSP_DF(double p0, struct S_DF p1, void* p2) { }
EXPORT void f5_V_DSP_DD(double p0, struct S_DD p1, void* p2) { }
EXPORT void f5_V_DSP_DP(double p0, struct S_DP p1, void* p2) { }
EXPORT void f5_V_DSP_PI(double p0, struct S_PI p1, void* p2) { }
EXPORT void f5_V_DSP_PF(double p0, struct S_PF p1, void* p2) { }
EXPORT void f5_V_DSP_PD(double p0, struct S_PD p1, void* p2) { }
EXPORT void f5_V_DSP_PP(double p0, struct S_PP p1, void* p2) { }
EXPORT void f5_V_DSP_III(double p0, struct S_III p1, void* p2) { }
EXPORT void f5_V_DSP_IIF(double p0, struct S_IIF p1, void* p2) { }
EXPORT void f5_V_DSP_IID(double p0, struct S_IID p1, void* p2) { }
EXPORT void f5_V_DSP_IIP(double p0, struct S_IIP p1, void* p2) { }
EXPORT void f5_V_DSP_IFI(double p0, struct S_IFI p1, void* p2) { }
EXPORT void f5_V_DSP_IFF(double p0, struct S_IFF p1, void* p2) { }
EXPORT void f5_V_DSP_IFD(double p0, struct S_IFD p1, void* p2) { }
EXPORT void f5_V_DSP_IFP(double p0, struct S_IFP p1, void* p2) { }
EXPORT void f5_V_DSP_IDI(double p0, struct S_IDI p1, void* p2) { }
EXPORT void f5_V_DSP_IDF(double p0, struct S_IDF p1, void* p2) { }
EXPORT void f5_V_DSP_IDD(double p0, struct S_IDD p1, void* p2) { }
EXPORT void f5_V_DSP_IDP(double p0, struct S_IDP p1, void* p2) { }
EXPORT void f5_V_DSP_IPI(double p0, struct S_IPI p1, void* p2) { }
EXPORT void f5_V_DSP_IPF(double p0, struct S_IPF p1, void* p2) { }
EXPORT void f5_V_DSP_IPD(double p0, struct S_IPD p1, void* p2) { }
EXPORT void f5_V_DSP_IPP(double p0, struct S_IPP p1, void* p2) { }
EXPORT void f5_V_DSP_FII(double p0, struct S_FII p1, void* p2) { }
EXPORT void f5_V_DSP_FIF(double p0, struct S_FIF p1, void* p2) { }
EXPORT void f5_V_DSP_FID(double p0, struct S_FID p1, void* p2) { }
EXPORT void f5_V_DSP_FIP(double p0, struct S_FIP p1, void* p2) { }
EXPORT void f5_V_DSP_FFI(double p0, struct S_FFI p1, void* p2) { }
EXPORT void f5_V_DSP_FFF(double p0, struct S_FFF p1, void* p2) { }
EXPORT void f5_V_DSP_FFD(double p0, struct S_FFD p1, void* p2) { }
EXPORT void f5_V_DSP_FFP(double p0, struct S_FFP p1, void* p2) { }
EXPORT void f5_V_DSP_FDI(double p0, struct S_FDI p1, void* p2) { }
EXPORT void f5_V_DSP_FDF(double p0, struct S_FDF p1, void* p2) { }
EXPORT void f5_V_DSP_FDD(double p0, struct S_FDD p1, void* p2) { }
EXPORT void f5_V_DSP_FDP(double p0, struct S_FDP p1, void* p2) { }
EXPORT void f5_V_DSP_FPI(double p0, struct S_FPI p1, void* p2) { }
EXPORT void f5_V_DSP_FPF(double p0, struct S_FPF p1, void* p2) { }
EXPORT void f5_V_DSP_FPD(double p0, struct S_FPD p1, void* p2) { }
EXPORT void f5_V_DSP_FPP(double p0, struct S_FPP p1, void* p2) { }
EXPORT void f5_V_DSP_DII(double p0, struct S_DII p1, void* p2) { }
EXPORT void f5_V_DSP_DIF(double p0, struct S_DIF p1, void* p2) { }
EXPORT void f5_V_DSP_DID(double p0, struct S_DID p1, void* p2) { }
EXPORT void f5_V_DSP_DIP(double p0, struct S_DIP p1, void* p2) { }
EXPORT void f5_V_DSP_DFI(double p0, struct S_DFI p1, void* p2) { }
EXPORT void f5_V_DSP_DFF(double p0, struct S_DFF p1, void* p2) { }
EXPORT void f5_V_DSP_DFD(double p0, struct S_DFD p1, void* p2) { }
EXPORT void f5_V_DSP_DFP(double p0, struct S_DFP p1, void* p2) { }
EXPORT void f5_V_DSP_DDI(double p0, struct S_DDI p1, void* p2) { }
EXPORT void f5_V_DSP_DDF(double p0, struct S_DDF p1, void* p2) { }
EXPORT void f5_V_DSP_DDD(double p0, struct S_DDD p1, void* p2) { }
EXPORT void f5_V_DSP_DDP(double p0, struct S_DDP p1, void* p2) { }
EXPORT void f5_V_DSP_DPI(double p0, struct S_DPI p1, void* p2) { }
EXPORT void f5_V_DSP_DPF(double p0, struct S_DPF p1, void* p2) { }
EXPORT void f5_V_DSP_DPD(double p0, struct S_DPD p1, void* p2) { }
EXPORT void f5_V_DSP_DPP(double p0, struct S_DPP p1, void* p2) { }
EXPORT void f5_V_DSP_PII(double p0, struct S_PII p1, void* p2) { }
EXPORT void f5_V_DSP_PIF(double p0, struct S_PIF p1, void* p2) { }
EXPORT void f5_V_DSP_PID(double p0, struct S_PID p1, void* p2) { }
EXPORT void f5_V_DSP_PIP(double p0, struct S_PIP p1, void* p2) { }
EXPORT void f5_V_DSP_PFI(double p0, struct S_PFI p1, void* p2) { }
EXPORT void f5_V_DSP_PFF(double p0, struct S_PFF p1, void* p2) { }
EXPORT void f5_V_DSP_PFD(double p0, struct S_PFD p1, void* p2) { }
EXPORT void f5_V_DSP_PFP(double p0, struct S_PFP p1, void* p2) { }
EXPORT void f5_V_DSP_PDI(double p0, struct S_PDI p1, void* p2) { }
EXPORT void f5_V_DSP_PDF(double p0, struct S_PDF p1, void* p2) { }
EXPORT void f5_V_DSP_PDD(double p0, struct S_PDD p1, void* p2) { }
EXPORT void f5_V_DSP_PDP(double p0, struct S_PDP p1, void* p2) { }
EXPORT void f5_V_DSP_PPI(double p0, struct S_PPI p1, void* p2) { }
EXPORT void f5_V_DSP_PPF(double p0, struct S_PPF p1, void* p2) { }
EXPORT void f5_V_DSP_PPD(double p0, struct S_PPD p1, void* p2) { }
EXPORT void f5_V_DSP_PPP(double p0, struct S_PPP p1, void* p2) { }
EXPORT void f5_V_DSS_I(double p0, struct S_I p1, struct S_I p2) { }
EXPORT void f5_V_DSS_F(double p0, struct S_F p1, struct S_F p2) { }
EXPORT void f5_V_DSS_D(double p0, struct S_D p1, struct S_D p2) { }
EXPORT void f5_V_DSS_P(double p0, struct S_P p1, struct S_P p2) { }
EXPORT void f5_V_DSS_II(double p0, struct S_II p1, struct S_II p2) { }
EXPORT void f5_V_DSS_IF(double p0, struct S_IF p1, struct S_IF p2) { }
EXPORT void f5_V_DSS_ID(double p0, struct S_ID p1, struct S_ID p2) { }
EXPORT void f5_V_DSS_IP(double p0, struct S_IP p1, struct S_IP p2) { }
EXPORT void f5_V_DSS_FI(double p0, struct S_FI p1, struct S_FI p2) { }
EXPORT void f5_V_DSS_FF(double p0, struct S_FF p1, struct S_FF p2) { }
EXPORT void f5_V_DSS_FD(double p0, struct S_FD p1, struct S_FD p2) { }
EXPORT void f5_V_DSS_FP(double p0, struct S_FP p1, struct S_FP p2) { }
EXPORT void f5_V_DSS_DI(double p0, struct S_DI p1, struct S_DI p2) { }
EXPORT void f5_V_DSS_DF(double p0, struct S_DF p1, struct S_DF p2) { }
EXPORT void f5_V_DSS_DD(double p0, struct S_DD p1, struct S_DD p2) { }
EXPORT void f5_V_DSS_DP(double p0, struct S_DP p1, struct S_DP p2) { }
EXPORT void f5_V_DSS_PI(double p0, struct S_PI p1, struct S_PI p2) { }
EXPORT void f5_V_DSS_PF(double p0, struct S_PF p1, struct S_PF p2) { }
EXPORT void f5_V_DSS_PD(double p0, struct S_PD p1, struct S_PD p2) { }
EXPORT void f5_V_DSS_PP(double p0, struct S_PP p1, struct S_PP p2) { }
EXPORT void f5_V_DSS_III(double p0, struct S_III p1, struct S_III p2) { }
EXPORT void f5_V_DSS_IIF(double p0, struct S_IIF p1, struct S_IIF p2) { }
EXPORT void f5_V_DSS_IID(double p0, struct S_IID p1, struct S_IID p2) { }
EXPORT void f5_V_DSS_IIP(double p0, struct S_IIP p1, struct S_IIP p2) { }
EXPORT void f5_V_DSS_IFI(double p0, struct S_IFI p1, struct S_IFI p2) { }
EXPORT void f5_V_DSS_IFF(double p0, struct S_IFF p1, struct S_IFF p2) { }
EXPORT void f5_V_DSS_IFD(double p0, struct S_IFD p1, struct S_IFD p2) { }
EXPORT void f5_V_DSS_IFP(double p0, struct S_IFP p1, struct S_IFP p2) { }
EXPORT void f5_V_DSS_IDI(double p0, struct S_IDI p1, struct S_IDI p2) { }
EXPORT void f5_V_DSS_IDF(double p0, struct S_IDF p1, struct S_IDF p2) { }
EXPORT void f5_V_DSS_IDD(double p0, struct S_IDD p1, struct S_IDD p2) { }
EXPORT void f5_V_DSS_IDP(double p0, struct S_IDP p1, struct S_IDP p2) { }
EXPORT void f5_V_DSS_IPI(double p0, struct S_IPI p1, struct S_IPI p2) { }
EXPORT void f5_V_DSS_IPF(double p0, struct S_IPF p1, struct S_IPF p2) { }
EXPORT void f5_V_DSS_IPD(double p0, struct S_IPD p1, struct S_IPD p2) { }
EXPORT void f5_V_DSS_IPP(double p0, struct S_IPP p1, struct S_IPP p2) { }
EXPORT void f5_V_DSS_FII(double p0, struct S_FII p1, struct S_FII p2) { }
EXPORT void f5_V_DSS_FIF(double p0, struct S_FIF p1, struct S_FIF p2) { }
EXPORT void f5_V_DSS_FID(double p0, struct S_FID p1, struct S_FID p2) { }
EXPORT void f5_V_DSS_FIP(double p0, struct S_FIP p1, struct S_FIP p2) { }
EXPORT void f5_V_DSS_FFI(double p0, struct S_FFI p1, struct S_FFI p2) { }
EXPORT void f5_V_DSS_FFF(double p0, struct S_FFF p1, struct S_FFF p2) { }
EXPORT void f5_V_DSS_FFD(double p0, struct S_FFD p1, struct S_FFD p2) { }
EXPORT void f5_V_DSS_FFP(double p0, struct S_FFP p1, struct S_FFP p2) { }
EXPORT void f5_V_DSS_FDI(double p0, struct S_FDI p1, struct S_FDI p2) { }
EXPORT void f5_V_DSS_FDF(double p0, struct S_FDF p1, struct S_FDF p2) { }
EXPORT void f5_V_DSS_FDD(double p0, struct S_FDD p1, struct S_FDD p2) { }
EXPORT void f5_V_DSS_FDP(double p0, struct S_FDP p1, struct S_FDP p2) { }
EXPORT void f5_V_DSS_FPI(double p0, struct S_FPI p1, struct S_FPI p2) { }
EXPORT void f5_V_DSS_FPF(double p0, struct S_FPF p1, struct S_FPF p2) { }
EXPORT void f5_V_DSS_FPD(double p0, struct S_FPD p1, struct S_FPD p2) { }
EXPORT void f5_V_DSS_FPP(double p0, struct S_FPP p1, struct S_FPP p2) { }
EXPORT void f5_V_DSS_DII(double p0, struct S_DII p1, struct S_DII p2) { }
EXPORT void f5_V_DSS_DIF(double p0, struct S_DIF p1, struct S_DIF p2) { }
EXPORT void f5_V_DSS_DID(double p0, struct S_DID p1, struct S_DID p2) { }
EXPORT void f5_V_DSS_DIP(double p0, struct S_DIP p1, struct S_DIP p2) { }
EXPORT void f5_V_DSS_DFI(double p0, struct S_DFI p1, struct S_DFI p2) { }
EXPORT void f5_V_DSS_DFF(double p0, struct S_DFF p1, struct S_DFF p2) { }
EXPORT void f5_V_DSS_DFD(double p0, struct S_DFD p1, struct S_DFD p2) { }
EXPORT void f5_V_DSS_DFP(double p0, struct S_DFP p1, struct S_DFP p2) { }
EXPORT void f5_V_DSS_DDI(double p0, struct S_DDI p1, struct S_DDI p2) { }
EXPORT void f5_V_DSS_DDF(double p0, struct S_DDF p1, struct S_DDF p2) { }
EXPORT void f5_V_DSS_DDD(double p0, struct S_DDD p1, struct S_DDD p2) { }
EXPORT void f5_V_DSS_DDP(double p0, struct S_DDP p1, struct S_DDP p2) { }
EXPORT void f5_V_DSS_DPI(double p0, struct S_DPI p1, struct S_DPI p2) { }
EXPORT void f5_V_DSS_DPF(double p0, struct S_DPF p1, struct S_DPF p2) { }
EXPORT void f5_V_DSS_DPD(double p0, struct S_DPD p1, struct S_DPD p2) { }
EXPORT void f5_V_DSS_DPP(double p0, struct S_DPP p1, struct S_DPP p2) { }
EXPORT void f5_V_DSS_PII(double p0, struct S_PII p1, struct S_PII p2) { }
EXPORT void f5_V_DSS_PIF(double p0, struct S_PIF p1, struct S_PIF p2) { }
EXPORT void f5_V_DSS_PID(double p0, struct S_PID p1, struct S_PID p2) { }
EXPORT void f5_V_DSS_PIP(double p0, struct S_PIP p1, struct S_PIP p2) { }
EXPORT void f5_V_DSS_PFI(double p0, struct S_PFI p1, struct S_PFI p2) { }
EXPORT void f5_V_DSS_PFF(double p0, struct S_PFF p1, struct S_PFF p2) { }
EXPORT void f5_V_DSS_PFD(double p0, struct S_PFD p1, struct S_PFD p2) { }
EXPORT void f5_V_DSS_PFP(double p0, struct S_PFP p1, struct S_PFP p2) { }
EXPORT void f5_V_DSS_PDI(double p0, struct S_PDI p1, struct S_PDI p2) { }
EXPORT void f5_V_DSS_PDF(double p0, struct S_PDF p1, struct S_PDF p2) { }
EXPORT void f5_V_DSS_PDD(double p0, struct S_PDD p1, struct S_PDD p2) { }
EXPORT void f5_V_DSS_PDP(double p0, struct S_PDP p1, struct S_PDP p2) { }
EXPORT void f5_V_DSS_PPI(double p0, struct S_PPI p1, struct S_PPI p2) { }
EXPORT void f5_V_DSS_PPF(double p0, struct S_PPF p1, struct S_PPF p2) { }
EXPORT void f5_V_DSS_PPD(double p0, struct S_PPD p1, struct S_PPD p2) { }
EXPORT void f5_V_DSS_PPP(double p0, struct S_PPP p1, struct S_PPP p2) { }
EXPORT void f5_V_PII_(void* p0, int p1, int p2) { }
EXPORT void f5_V_PIF_(void* p0, int p1, float p2) { }
EXPORT void f5_V_PID_(void* p0, int p1, double p2) { }
EXPORT void f5_V_PIP_(void* p0, int p1, void* p2) { }
EXPORT void f5_V_PIS_I(void* p0, int p1, struct S_I p2) { }
EXPORT void f5_V_PIS_F(void* p0, int p1, struct S_F p2) { }
EXPORT void f5_V_PIS_D(void* p0, int p1, struct S_D p2) { }
EXPORT void f5_V_PIS_P(void* p0, int p1, struct S_P p2) { }
EXPORT void f5_V_PIS_II(void* p0, int p1, struct S_II p2) { }
EXPORT void f5_V_PIS_IF(void* p0, int p1, struct S_IF p2) { }
EXPORT void f5_V_PIS_ID(void* p0, int p1, struct S_ID p2) { }
EXPORT void f5_V_PIS_IP(void* p0, int p1, struct S_IP p2) { }
EXPORT void f5_V_PIS_FI(void* p0, int p1, struct S_FI p2) { }
EXPORT void f5_V_PIS_FF(void* p0, int p1, struct S_FF p2) { }
EXPORT void f5_V_PIS_FD(void* p0, int p1, struct S_FD p2) { }
EXPORT void f5_V_PIS_FP(void* p0, int p1, struct S_FP p2) { }
EXPORT void f5_V_PIS_DI(void* p0, int p1, struct S_DI p2) { }
EXPORT void f5_V_PIS_DF(void* p0, int p1, struct S_DF p2) { }
EXPORT void f5_V_PIS_DD(void* p0, int p1, struct S_DD p2) { }
EXPORT void f5_V_PIS_DP(void* p0, int p1, struct S_DP p2) { }
EXPORT void f5_V_PIS_PI(void* p0, int p1, struct S_PI p2) { }
EXPORT void f5_V_PIS_PF(void* p0, int p1, struct S_PF p2) { }
EXPORT void f5_V_PIS_PD(void* p0, int p1, struct S_PD p2) { }
EXPORT void f5_V_PIS_PP(void* p0, int p1, struct S_PP p2) { }
EXPORT void f5_V_PIS_III(void* p0, int p1, struct S_III p2) { }
EXPORT void f5_V_PIS_IIF(void* p0, int p1, struct S_IIF p2) { }
EXPORT void f5_V_PIS_IID(void* p0, int p1, struct S_IID p2) { }
EXPORT void f5_V_PIS_IIP(void* p0, int p1, struct S_IIP p2) { }
EXPORT void f5_V_PIS_IFI(void* p0, int p1, struct S_IFI p2) { }
EXPORT void f5_V_PIS_IFF(void* p0, int p1, struct S_IFF p2) { }
EXPORT void f5_V_PIS_IFD(void* p0, int p1, struct S_IFD p2) { }
EXPORT void f5_V_PIS_IFP(void* p0, int p1, struct S_IFP p2) { }
EXPORT void f5_V_PIS_IDI(void* p0, int p1, struct S_IDI p2) { }
EXPORT void f5_V_PIS_IDF(void* p0, int p1, struct S_IDF p2) { }
EXPORT void f5_V_PIS_IDD(void* p0, int p1, struct S_IDD p2) { }
EXPORT void f5_V_PIS_IDP(void* p0, int p1, struct S_IDP p2) { }
EXPORT void f5_V_PIS_IPI(void* p0, int p1, struct S_IPI p2) { }
EXPORT void f5_V_PIS_IPF(void* p0, int p1, struct S_IPF p2) { }
EXPORT void f5_V_PIS_IPD(void* p0, int p1, struct S_IPD p2) { }
EXPORT void f5_V_PIS_IPP(void* p0, int p1, struct S_IPP p2) { }
EXPORT void f5_V_PIS_FII(void* p0, int p1, struct S_FII p2) { }
EXPORT void f5_V_PIS_FIF(void* p0, int p1, struct S_FIF p2) { }
EXPORT void f5_V_PIS_FID(void* p0, int p1, struct S_FID p2) { }
EXPORT void f5_V_PIS_FIP(void* p0, int p1, struct S_FIP p2) { }
EXPORT void f5_V_PIS_FFI(void* p0, int p1, struct S_FFI p2) { }
EXPORT void f5_V_PIS_FFF(void* p0, int p1, struct S_FFF p2) { }
EXPORT void f5_V_PIS_FFD(void* p0, int p1, struct S_FFD p2) { }
EXPORT void f5_V_PIS_FFP(void* p0, int p1, struct S_FFP p2) { }
EXPORT void f5_V_PIS_FDI(void* p0, int p1, struct S_FDI p2) { }
EXPORT void f5_V_PIS_FDF(void* p0, int p1, struct S_FDF p2) { }
EXPORT void f5_V_PIS_FDD(void* p0, int p1, struct S_FDD p2) { }
EXPORT void f5_V_PIS_FDP(void* p0, int p1, struct S_FDP p2) { }
EXPORT void f5_V_PIS_FPI(void* p0, int p1, struct S_FPI p2) { }
EXPORT void f5_V_PIS_FPF(void* p0, int p1, struct S_FPF p2) { }
EXPORT void f5_V_PIS_FPD(void* p0, int p1, struct S_FPD p2) { }
EXPORT void f5_V_PIS_FPP(void* p0, int p1, struct S_FPP p2) { }
EXPORT void f5_V_PIS_DII(void* p0, int p1, struct S_DII p2) { }
EXPORT void f5_V_PIS_DIF(void* p0, int p1, struct S_DIF p2) { }
EXPORT void f5_V_PIS_DID(void* p0, int p1, struct S_DID p2) { }
EXPORT void f5_V_PIS_DIP(void* p0, int p1, struct S_DIP p2) { }
EXPORT void f5_V_PIS_DFI(void* p0, int p1, struct S_DFI p2) { }
EXPORT void f5_V_PIS_DFF(void* p0, int p1, struct S_DFF p2) { }
EXPORT void f5_V_PIS_DFD(void* p0, int p1, struct S_DFD p2) { }
EXPORT void f5_V_PIS_DFP(void* p0, int p1, struct S_DFP p2) { }
EXPORT void f5_V_PIS_DDI(void* p0, int p1, struct S_DDI p2) { }
EXPORT void f5_V_PIS_DDF(void* p0, int p1, struct S_DDF p2) { }
EXPORT void f5_V_PIS_DDD(void* p0, int p1, struct S_DDD p2) { }
EXPORT void f5_V_PIS_DDP(void* p0, int p1, struct S_DDP p2) { }
EXPORT void f5_V_PIS_DPI(void* p0, int p1, struct S_DPI p2) { }
EXPORT void f5_V_PIS_DPF(void* p0, int p1, struct S_DPF p2) { }
EXPORT void f5_V_PIS_DPD(void* p0, int p1, struct S_DPD p2) { }
EXPORT void f5_V_PIS_DPP(void* p0, int p1, struct S_DPP p2) { }
EXPORT void f5_V_PIS_PII(void* p0, int p1, struct S_PII p2) { }
EXPORT void f5_V_PIS_PIF(void* p0, int p1, struct S_PIF p2) { }
EXPORT void f5_V_PIS_PID(void* p0, int p1, struct S_PID p2) { }
EXPORT void f5_V_PIS_PIP(void* p0, int p1, struct S_PIP p2) { }
EXPORT void f5_V_PIS_PFI(void* p0, int p1, struct S_PFI p2) { }
EXPORT void f5_V_PIS_PFF(void* p0, int p1, struct S_PFF p2) { }
EXPORT void f5_V_PIS_PFD(void* p0, int p1, struct S_PFD p2) { }
EXPORT void f5_V_PIS_PFP(void* p0, int p1, struct S_PFP p2) { }
EXPORT void f5_V_PIS_PDI(void* p0, int p1, struct S_PDI p2) { }
EXPORT void f5_V_PIS_PDF(void* p0, int p1, struct S_PDF p2) { }
EXPORT void f5_V_PIS_PDD(void* p0, int p1, struct S_PDD p2) { }
EXPORT void f5_V_PIS_PDP(void* p0, int p1, struct S_PDP p2) { }
EXPORT void f5_V_PIS_PPI(void* p0, int p1, struct S_PPI p2) { }
EXPORT void f5_V_PIS_PPF(void* p0, int p1, struct S_PPF p2) { }
EXPORT void f5_V_PIS_PPD(void* p0, int p1, struct S_PPD p2) { }
EXPORT void f5_V_PIS_PPP(void* p0, int p1, struct S_PPP p2) { }
EXPORT void f5_V_PFI_(void* p0, float p1, int p2) { }
EXPORT void f5_V_PFF_(void* p0, float p1, float p2) { }
EXPORT void f5_V_PFD_(void* p0, float p1, double p2) { }
EXPORT void f5_V_PFP_(void* p0, float p1, void* p2) { }
EXPORT void f5_V_PFS_I(void* p0, float p1, struct S_I p2) { }
EXPORT void f5_V_PFS_F(void* p0, float p1, struct S_F p2) { }
EXPORT void f5_V_PFS_D(void* p0, float p1, struct S_D p2) { }
EXPORT void f5_V_PFS_P(void* p0, float p1, struct S_P p2) { }
EXPORT void f5_V_PFS_II(void* p0, float p1, struct S_II p2) { }
EXPORT void f5_V_PFS_IF(void* p0, float p1, struct S_IF p2) { }
EXPORT void f5_V_PFS_ID(void* p0, float p1, struct S_ID p2) { }
EXPORT void f5_V_PFS_IP(void* p0, float p1, struct S_IP p2) { }
EXPORT void f5_V_PFS_FI(void* p0, float p1, struct S_FI p2) { }
EXPORT void f5_V_PFS_FF(void* p0, float p1, struct S_FF p2) { }
EXPORT void f5_V_PFS_FD(void* p0, float p1, struct S_FD p2) { }
EXPORT void f5_V_PFS_FP(void* p0, float p1, struct S_FP p2) { }
EXPORT void f5_V_PFS_DI(void* p0, float p1, struct S_DI p2) { }
EXPORT void f5_V_PFS_DF(void* p0, float p1, struct S_DF p2) { }
EXPORT void f5_V_PFS_DD(void* p0, float p1, struct S_DD p2) { }
EXPORT void f5_V_PFS_DP(void* p0, float p1, struct S_DP p2) { }
EXPORT void f5_V_PFS_PI(void* p0, float p1, struct S_PI p2) { }
EXPORT void f5_V_PFS_PF(void* p0, float p1, struct S_PF p2) { }
EXPORT void f5_V_PFS_PD(void* p0, float p1, struct S_PD p2) { }
EXPORT void f5_V_PFS_PP(void* p0, float p1, struct S_PP p2) { }
EXPORT void f5_V_PFS_III(void* p0, float p1, struct S_III p2) { }
EXPORT void f5_V_PFS_IIF(void* p0, float p1, struct S_IIF p2) { }
EXPORT void f5_V_PFS_IID(void* p0, float p1, struct S_IID p2) { }
EXPORT void f5_V_PFS_IIP(void* p0, float p1, struct S_IIP p2) { }
EXPORT void f5_V_PFS_IFI(void* p0, float p1, struct S_IFI p2) { }
EXPORT void f5_V_PFS_IFF(void* p0, float p1, struct S_IFF p2) { }
EXPORT void f5_V_PFS_IFD(void* p0, float p1, struct S_IFD p2) { }
EXPORT void f5_V_PFS_IFP(void* p0, float p1, struct S_IFP p2) { }
EXPORT void f5_V_PFS_IDI(void* p0, float p1, struct S_IDI p2) { }
EXPORT void f5_V_PFS_IDF(void* p0, float p1, struct S_IDF p2) { }
EXPORT void f5_V_PFS_IDD(void* p0, float p1, struct S_IDD p2) { }
EXPORT void f5_V_PFS_IDP(void* p0, float p1, struct S_IDP p2) { }
EXPORT void f5_V_PFS_IPI(void* p0, float p1, struct S_IPI p2) { }
EXPORT void f5_V_PFS_IPF(void* p0, float p1, struct S_IPF p2) { }
EXPORT void f5_V_PFS_IPD(void* p0, float p1, struct S_IPD p2) { }
EXPORT void f5_V_PFS_IPP(void* p0, float p1, struct S_IPP p2) { }
EXPORT void f5_V_PFS_FII(void* p0, float p1, struct S_FII p2) { }
EXPORT void f5_V_PFS_FIF(void* p0, float p1, struct S_FIF p2) { }
EXPORT void f5_V_PFS_FID(void* p0, float p1, struct S_FID p2) { }
EXPORT void f5_V_PFS_FIP(void* p0, float p1, struct S_FIP p2) { }
EXPORT void f5_V_PFS_FFI(void* p0, float p1, struct S_FFI p2) { }
EXPORT void f5_V_PFS_FFF(void* p0, float p1, struct S_FFF p2) { }
EXPORT void f5_V_PFS_FFD(void* p0, float p1, struct S_FFD p2) { }
EXPORT void f5_V_PFS_FFP(void* p0, float p1, struct S_FFP p2) { }
EXPORT void f5_V_PFS_FDI(void* p0, float p1, struct S_FDI p2) { }
EXPORT void f5_V_PFS_FDF(void* p0, float p1, struct S_FDF p2) { }
EXPORT void f5_V_PFS_FDD(void* p0, float p1, struct S_FDD p2) { }
EXPORT void f5_V_PFS_FDP(void* p0, float p1, struct S_FDP p2) { }
EXPORT void f5_V_PFS_FPI(void* p0, float p1, struct S_FPI p2) { }
EXPORT void f5_V_PFS_FPF(void* p0, float p1, struct S_FPF p2) { }
EXPORT void f5_V_PFS_FPD(void* p0, float p1, struct S_FPD p2) { }
EXPORT void f5_V_PFS_FPP(void* p0, float p1, struct S_FPP p2) { }
EXPORT void f5_V_PFS_DII(void* p0, float p1, struct S_DII p2) { }
EXPORT void f5_V_PFS_DIF(void* p0, float p1, struct S_DIF p2) { }
EXPORT void f5_V_PFS_DID(void* p0, float p1, struct S_DID p2) { }
EXPORT void f5_V_PFS_DIP(void* p0, float p1, struct S_DIP p2) { }
EXPORT void f5_V_PFS_DFI(void* p0, float p1, struct S_DFI p2) { }
EXPORT void f5_V_PFS_DFF(void* p0, float p1, struct S_DFF p2) { }
EXPORT void f5_V_PFS_DFD(void* p0, float p1, struct S_DFD p2) { }
EXPORT void f5_V_PFS_DFP(void* p0, float p1, struct S_DFP p2) { }
EXPORT void f5_V_PFS_DDI(void* p0, float p1, struct S_DDI p2) { }
EXPORT void f5_V_PFS_DDF(void* p0, float p1, struct S_DDF p2) { }
EXPORT void f5_V_PFS_DDD(void* p0, float p1, struct S_DDD p2) { }
EXPORT void f5_V_PFS_DDP(void* p0, float p1, struct S_DDP p2) { }
EXPORT void f5_V_PFS_DPI(void* p0, float p1, struct S_DPI p2) { }
EXPORT void f5_V_PFS_DPF(void* p0, float p1, struct S_DPF p2) { }
EXPORT void f5_V_PFS_DPD(void* p0, float p1, struct S_DPD p2) { }
EXPORT void f5_V_PFS_DPP(void* p0, float p1, struct S_DPP p2) { }
EXPORT void f5_V_PFS_PII(void* p0, float p1, struct S_PII p2) { }
EXPORT void f5_V_PFS_PIF(void* p0, float p1, struct S_PIF p2) { }
EXPORT void f5_V_PFS_PID(void* p0, float p1, struct S_PID p2) { }
EXPORT void f5_V_PFS_PIP(void* p0, float p1, struct S_PIP p2) { }
EXPORT void f5_V_PFS_PFI(void* p0, float p1, struct S_PFI p2) { }
EXPORT void f5_V_PFS_PFF(void* p0, float p1, struct S_PFF p2) { }
EXPORT void f5_V_PFS_PFD(void* p0, float p1, struct S_PFD p2) { }
EXPORT void f5_V_PFS_PFP(void* p0, float p1, struct S_PFP p2) { }
EXPORT void f5_V_PFS_PDI(void* p0, float p1, struct S_PDI p2) { }
EXPORT void f5_V_PFS_PDF(void* p0, float p1, struct S_PDF p2) { }
EXPORT void f5_V_PFS_PDD(void* p0, float p1, struct S_PDD p2) { }
EXPORT void f5_V_PFS_PDP(void* p0, float p1, struct S_PDP p2) { }
EXPORT void f5_V_PFS_PPI(void* p0, float p1, struct S_PPI p2) { }
EXPORT void f5_V_PFS_PPF(void* p0, float p1, struct S_PPF p2) { }
EXPORT void f5_V_PFS_PPD(void* p0, float p1, struct S_PPD p2) { }
EXPORT void f5_V_PFS_PPP(void* p0, float p1, struct S_PPP p2) { }
EXPORT void f5_V_PDI_(void* p0, double p1, int p2) { }
EXPORT void f5_V_PDF_(void* p0, double p1, float p2) { }
EXPORT void f5_V_PDD_(void* p0, double p1, double p2) { }
EXPORT void f5_V_PDP_(void* p0, double p1, void* p2) { }
EXPORT void f5_V_PDS_I(void* p0, double p1, struct S_I p2) { }
EXPORT void f5_V_PDS_F(void* p0, double p1, struct S_F p2) { }
EXPORT void f5_V_PDS_D(void* p0, double p1, struct S_D p2) { }
EXPORT void f5_V_PDS_P(void* p0, double p1, struct S_P p2) { }
EXPORT void f5_V_PDS_II(void* p0, double p1, struct S_II p2) { }
EXPORT void f5_V_PDS_IF(void* p0, double p1, struct S_IF p2) { }
EXPORT void f5_V_PDS_ID(void* p0, double p1, struct S_ID p2) { }
EXPORT void f5_V_PDS_IP(void* p0, double p1, struct S_IP p2) { }
EXPORT void f5_V_PDS_FI(void* p0, double p1, struct S_FI p2) { }
EXPORT void f5_V_PDS_FF(void* p0, double p1, struct S_FF p2) { }
EXPORT void f5_V_PDS_FD(void* p0, double p1, struct S_FD p2) { }
EXPORT void f5_V_PDS_FP(void* p0, double p1, struct S_FP p2) { }
EXPORT void f5_V_PDS_DI(void* p0, double p1, struct S_DI p2) { }
EXPORT void f5_V_PDS_DF(void* p0, double p1, struct S_DF p2) { }
EXPORT void f5_V_PDS_DD(void* p0, double p1, struct S_DD p2) { }
EXPORT void f5_V_PDS_DP(void* p0, double p1, struct S_DP p2) { }
EXPORT void f5_V_PDS_PI(void* p0, double p1, struct S_PI p2) { }
EXPORT void f5_V_PDS_PF(void* p0, double p1, struct S_PF p2) { }
EXPORT void f5_V_PDS_PD(void* p0, double p1, struct S_PD p2) { }
EXPORT void f5_V_PDS_PP(void* p0, double p1, struct S_PP p2) { }
EXPORT void f5_V_PDS_III(void* p0, double p1, struct S_III p2) { }
EXPORT void f5_V_PDS_IIF(void* p0, double p1, struct S_IIF p2) { }
EXPORT void f5_V_PDS_IID(void* p0, double p1, struct S_IID p2) { }
EXPORT void f5_V_PDS_IIP(void* p0, double p1, struct S_IIP p2) { }
EXPORT void f5_V_PDS_IFI(void* p0, double p1, struct S_IFI p2) { }
EXPORT void f5_V_PDS_IFF(void* p0, double p1, struct S_IFF p2) { }
EXPORT void f5_V_PDS_IFD(void* p0, double p1, struct S_IFD p2) { }
EXPORT void f5_V_PDS_IFP(void* p0, double p1, struct S_IFP p2) { }
EXPORT void f5_V_PDS_IDI(void* p0, double p1, struct S_IDI p2) { }
EXPORT void f5_V_PDS_IDF(void* p0, double p1, struct S_IDF p2) { }
EXPORT void f5_V_PDS_IDD(void* p0, double p1, struct S_IDD p2) { }
EXPORT void f5_V_PDS_IDP(void* p0, double p1, struct S_IDP p2) { }
EXPORT void f5_V_PDS_IPI(void* p0, double p1, struct S_IPI p2) { }
EXPORT void f5_V_PDS_IPF(void* p0, double p1, struct S_IPF p2) { }
EXPORT void f5_V_PDS_IPD(void* p0, double p1, struct S_IPD p2) { }
EXPORT void f5_V_PDS_IPP(void* p0, double p1, struct S_IPP p2) { }
EXPORT void f5_V_PDS_FII(void* p0, double p1, struct S_FII p2) { }
EXPORT void f5_V_PDS_FIF(void* p0, double p1, struct S_FIF p2) { }
EXPORT void f5_V_PDS_FID(void* p0, double p1, struct S_FID p2) { }
EXPORT void f5_V_PDS_FIP(void* p0, double p1, struct S_FIP p2) { }
EXPORT void f5_V_PDS_FFI(void* p0, double p1, struct S_FFI p2) { }
EXPORT void f5_V_PDS_FFF(void* p0, double p1, struct S_FFF p2) { }
EXPORT void f5_V_PDS_FFD(void* p0, double p1, struct S_FFD p2) { }
EXPORT void f5_V_PDS_FFP(void* p0, double p1, struct S_FFP p2) { }
EXPORT void f5_V_PDS_FDI(void* p0, double p1, struct S_FDI p2) { }
EXPORT void f5_V_PDS_FDF(void* p0, double p1, struct S_FDF p2) { }
EXPORT void f5_V_PDS_FDD(void* p0, double p1, struct S_FDD p2) { }
EXPORT void f5_V_PDS_FDP(void* p0, double p1, struct S_FDP p2) { }
EXPORT void f5_V_PDS_FPI(void* p0, double p1, struct S_FPI p2) { }
EXPORT void f5_V_PDS_FPF(void* p0, double p1, struct S_FPF p2) { }
EXPORT void f5_V_PDS_FPD(void* p0, double p1, struct S_FPD p2) { }
EXPORT void f5_V_PDS_FPP(void* p0, double p1, struct S_FPP p2) { }
EXPORT void f5_V_PDS_DII(void* p0, double p1, struct S_DII p2) { }
EXPORT void f5_V_PDS_DIF(void* p0, double p1, struct S_DIF p2) { }
EXPORT void f5_V_PDS_DID(void* p0, double p1, struct S_DID p2) { }
EXPORT void f5_V_PDS_DIP(void* p0, double p1, struct S_DIP p2) { }
EXPORT void f5_V_PDS_DFI(void* p0, double p1, struct S_DFI p2) { }
EXPORT void f5_V_PDS_DFF(void* p0, double p1, struct S_DFF p2) { }
EXPORT void f5_V_PDS_DFD(void* p0, double p1, struct S_DFD p2) { }
EXPORT void f5_V_PDS_DFP(void* p0, double p1, struct S_DFP p2) { }
EXPORT void f5_V_PDS_DDI(void* p0, double p1, struct S_DDI p2) { }
EXPORT void f5_V_PDS_DDF(void* p0, double p1, struct S_DDF p2) { }
EXPORT void f5_V_PDS_DDD(void* p0, double p1, struct S_DDD p2) { }
EXPORT void f5_V_PDS_DDP(void* p0, double p1, struct S_DDP p2) { }
EXPORT void f5_V_PDS_DPI(void* p0, double p1, struct S_DPI p2) { }
EXPORT void f5_V_PDS_DPF(void* p0, double p1, struct S_DPF p2) { }
EXPORT void f5_V_PDS_DPD(void* p0, double p1, struct S_DPD p2) { }
EXPORT void f5_V_PDS_DPP(void* p0, double p1, struct S_DPP p2) { }
EXPORT void f5_V_PDS_PII(void* p0, double p1, struct S_PII p2) { }
EXPORT void f5_V_PDS_PIF(void* p0, double p1, struct S_PIF p2) { }
EXPORT void f5_V_PDS_PID(void* p0, double p1, struct S_PID p2) { }
EXPORT void f5_V_PDS_PIP(void* p0, double p1, struct S_PIP p2) { }
EXPORT void f5_V_PDS_PFI(void* p0, double p1, struct S_PFI p2) { }
EXPORT void f5_V_PDS_PFF(void* p0, double p1, struct S_PFF p2) { }
EXPORT void f5_V_PDS_PFD(void* p0, double p1, struct S_PFD p2) { }
EXPORT void f5_V_PDS_PFP(void* p0, double p1, struct S_PFP p2) { }
EXPORT void f5_V_PDS_PDI(void* p0, double p1, struct S_PDI p2) { }
EXPORT void f5_V_PDS_PDF(void* p0, double p1, struct S_PDF p2) { }
EXPORT void f5_V_PDS_PDD(void* p0, double p1, struct S_PDD p2) { }
EXPORT void f5_V_PDS_PDP(void* p0, double p1, struct S_PDP p2) { }
EXPORT void f5_V_PDS_PPI(void* p0, double p1, struct S_PPI p2) { }
EXPORT void f5_V_PDS_PPF(void* p0, double p1, struct S_PPF p2) { }
EXPORT void f5_V_PDS_PPD(void* p0, double p1, struct S_PPD p2) { }
EXPORT void f5_V_PDS_PPP(void* p0, double p1, struct S_PPP p2) { }
EXPORT void f5_V_PPI_(void* p0, void* p1, int p2) { }
EXPORT void f5_V_PPF_(void* p0, void* p1, float p2) { }
EXPORT void f5_V_PPD_(void* p0, void* p1, double p2) { }
EXPORT void f5_V_PPP_(void* p0, void* p1, void* p2) { }
EXPORT void f5_V_PPS_I(void* p0, void* p1, struct S_I p2) { }
EXPORT void f5_V_PPS_F(void* p0, void* p1, struct S_F p2) { }
EXPORT void f5_V_PPS_D(void* p0, void* p1, struct S_D p2) { }
EXPORT void f5_V_PPS_P(void* p0, void* p1, struct S_P p2) { }
EXPORT void f5_V_PPS_II(void* p0, void* p1, struct S_II p2) { }
EXPORT void f5_V_PPS_IF(void* p0, void* p1, struct S_IF p2) { }
EXPORT void f5_V_PPS_ID(void* p0, void* p1, struct S_ID p2) { }
EXPORT void f5_V_PPS_IP(void* p0, void* p1, struct S_IP p2) { }
EXPORT void f5_V_PPS_FI(void* p0, void* p1, struct S_FI p2) { }
EXPORT void f5_V_PPS_FF(void* p0, void* p1, struct S_FF p2) { }
EXPORT void f5_V_PPS_FD(void* p0, void* p1, struct S_FD p2) { }
EXPORT void f5_V_PPS_FP(void* p0, void* p1, struct S_FP p2) { }
EXPORT void f5_V_PPS_DI(void* p0, void* p1, struct S_DI p2) { }
EXPORT void f5_V_PPS_DF(void* p0, void* p1, struct S_DF p2) { }
EXPORT void f5_V_PPS_DD(void* p0, void* p1, struct S_DD p2) { }
EXPORT void f5_V_PPS_DP(void* p0, void* p1, struct S_DP p2) { }
EXPORT void f5_V_PPS_PI(void* p0, void* p1, struct S_PI p2) { }
EXPORT void f5_V_PPS_PF(void* p0, void* p1, struct S_PF p2) { }
EXPORT void f5_V_PPS_PD(void* p0, void* p1, struct S_PD p2) { }
EXPORT void f5_V_PPS_PP(void* p0, void* p1, struct S_PP p2) { }
EXPORT void f5_V_PPS_III(void* p0, void* p1, struct S_III p2) { }
EXPORT void f5_V_PPS_IIF(void* p0, void* p1, struct S_IIF p2) { }
EXPORT void f5_V_PPS_IID(void* p0, void* p1, struct S_IID p2) { }
EXPORT void f5_V_PPS_IIP(void* p0, void* p1, struct S_IIP p2) { }
EXPORT void f5_V_PPS_IFI(void* p0, void* p1, struct S_IFI p2) { }
EXPORT void f5_V_PPS_IFF(void* p0, void* p1, struct S_IFF p2) { }
EXPORT void f5_V_PPS_IFD(void* p0, void* p1, struct S_IFD p2) { }
EXPORT void f5_V_PPS_IFP(void* p0, void* p1, struct S_IFP p2) { }
EXPORT void f5_V_PPS_IDI(void* p0, void* p1, struct S_IDI p2) { }
EXPORT void f5_V_PPS_IDF(void* p0, void* p1, struct S_IDF p2) { }
EXPORT void f5_V_PPS_IDD(void* p0, void* p1, struct S_IDD p2) { }
EXPORT void f5_V_PPS_IDP(void* p0, void* p1, struct S_IDP p2) { }
EXPORT void f5_V_PPS_IPI(void* p0, void* p1, struct S_IPI p2) { }
EXPORT void f5_V_PPS_IPF(void* p0, void* p1, struct S_IPF p2) { }
EXPORT void f5_V_PPS_IPD(void* p0, void* p1, struct S_IPD p2) { }
EXPORT void f5_V_PPS_IPP(void* p0, void* p1, struct S_IPP p2) { }
EXPORT void f5_V_PPS_FII(void* p0, void* p1, struct S_FII p2) { }
EXPORT void f5_V_PPS_FIF(void* p0, void* p1, struct S_FIF p2) { }
EXPORT void f5_V_PPS_FID(void* p0, void* p1, struct S_FID p2) { }
EXPORT void f5_V_PPS_FIP(void* p0, void* p1, struct S_FIP p2) { }
EXPORT void f5_V_PPS_FFI(void* p0, void* p1, struct S_FFI p2) { }
EXPORT void f5_V_PPS_FFF(void* p0, void* p1, struct S_FFF p2) { }
EXPORT void f5_V_PPS_FFD(void* p0, void* p1, struct S_FFD p2) { }
EXPORT void f5_V_PPS_FFP(void* p0, void* p1, struct S_FFP p2) { }
EXPORT void f5_V_PPS_FDI(void* p0, void* p1, struct S_FDI p2) { }
EXPORT void f5_V_PPS_FDF(void* p0, void* p1, struct S_FDF p2) { }
EXPORT void f5_V_PPS_FDD(void* p0, void* p1, struct S_FDD p2) { }
EXPORT void f5_V_PPS_FDP(void* p0, void* p1, struct S_FDP p2) { }
EXPORT void f5_V_PPS_FPI(void* p0, void* p1, struct S_FPI p2) { }
EXPORT void f5_V_PPS_FPF(void* p0, void* p1, struct S_FPF p2) { }
EXPORT void f5_V_PPS_FPD(void* p0, void* p1, struct S_FPD p2) { }
EXPORT void f5_V_PPS_FPP(void* p0, void* p1, struct S_FPP p2) { }
EXPORT void f5_V_PPS_DII(void* p0, void* p1, struct S_DII p2) { }
EXPORT void f5_V_PPS_DIF(void* p0, void* p1, struct S_DIF p2) { }
EXPORT void f5_V_PPS_DID(void* p0, void* p1, struct S_DID p2) { }
EXPORT void f5_V_PPS_DIP(void* p0, void* p1, struct S_DIP p2) { }
EXPORT void f5_V_PPS_DFI(void* p0, void* p1, struct S_DFI p2) { }
EXPORT void f5_V_PPS_DFF(void* p0, void* p1, struct S_DFF p2) { }
EXPORT void f5_V_PPS_DFD(void* p0, void* p1, struct S_DFD p2) { }
EXPORT void f5_V_PPS_DFP(void* p0, void* p1, struct S_DFP p2) { }
EXPORT void f5_V_PPS_DDI(void* p0, void* p1, struct S_DDI p2) { }
EXPORT void f5_V_PPS_DDF(void* p0, void* p1, struct S_DDF p2) { }
EXPORT void f5_V_PPS_DDD(void* p0, void* p1, struct S_DDD p2) { }
EXPORT void f5_V_PPS_DDP(void* p0, void* p1, struct S_DDP p2) { }
EXPORT void f5_V_PPS_DPI(void* p0, void* p1, struct S_DPI p2) { }
EXPORT void f5_V_PPS_DPF(void* p0, void* p1, struct S_DPF p2) { }
EXPORT void f5_V_PPS_DPD(void* p0, void* p1, struct S_DPD p2) { }
EXPORT void f5_V_PPS_DPP(void* p0, void* p1, struct S_DPP p2) { }
EXPORT void f5_V_PPS_PII(void* p0, void* p1, struct S_PII p2) { }
EXPORT void f5_V_PPS_PIF(void* p0, void* p1, struct S_PIF p2) { }
EXPORT void f5_V_PPS_PID(void* p0, void* p1, struct S_PID p2) { }
EXPORT void f5_V_PPS_PIP(void* p0, void* p1, struct S_PIP p2) { }
EXPORT void f5_V_PPS_PFI(void* p0, void* p1, struct S_PFI p2) { }
EXPORT void f5_V_PPS_PFF(void* p0, void* p1, struct S_PFF p2) { }
EXPORT void f5_V_PPS_PFD(void* p0, void* p1, struct S_PFD p2) { }
EXPORT void f5_V_PPS_PFP(void* p0, void* p1, struct S_PFP p2) { }
EXPORT void f5_V_PPS_PDI(void* p0, void* p1, struct S_PDI p2) { }
EXPORT void f5_V_PPS_PDF(void* p0, void* p1, struct S_PDF p2) { }
EXPORT void f5_V_PPS_PDD(void* p0, void* p1, struct S_PDD p2) { }
EXPORT void f5_V_PPS_PDP(void* p0, void* p1, struct S_PDP p2) { }
EXPORT void f5_V_PPS_PPI(void* p0, void* p1, struct S_PPI p2) { }
EXPORT void f5_V_PPS_PPF(void* p0, void* p1, struct S_PPF p2) { }
EXPORT void f5_V_PPS_PPD(void* p0, void* p1, struct S_PPD p2) { }
EXPORT void f5_V_PPS_PPP(void* p0, void* p1, struct S_PPP p2) { }
EXPORT void f5_V_PSI_I(void* p0, struct S_I p1, int p2) { }
EXPORT void f5_V_PSI_F(void* p0, struct S_F p1, int p2) { }
EXPORT void f5_V_PSI_D(void* p0, struct S_D p1, int p2) { }
EXPORT void f5_V_PSI_P(void* p0, struct S_P p1, int p2) { }
EXPORT void f5_V_PSI_II(void* p0, struct S_II p1, int p2) { }
EXPORT void f5_V_PSI_IF(void* p0, struct S_IF p1, int p2) { }
EXPORT void f5_V_PSI_ID(void* p0, struct S_ID p1, int p2) { }
EXPORT void f5_V_PSI_IP(void* p0, struct S_IP p1, int p2) { }
EXPORT void f5_V_PSI_FI(void* p0, struct S_FI p1, int p2) { }
EXPORT void f5_V_PSI_FF(void* p0, struct S_FF p1, int p2) { }
EXPORT void f5_V_PSI_FD(void* p0, struct S_FD p1, int p2) { }
EXPORT void f5_V_PSI_FP(void* p0, struct S_FP p1, int p2) { }
EXPORT void f5_V_PSI_DI(void* p0, struct S_DI p1, int p2) { }
EXPORT void f5_V_PSI_DF(void* p0, struct S_DF p1, int p2) { }
EXPORT void f5_V_PSI_DD(void* p0, struct S_DD p1, int p2) { }
EXPORT void f5_V_PSI_DP(void* p0, struct S_DP p1, int p2) { }
EXPORT void f5_V_PSI_PI(void* p0, struct S_PI p1, int p2) { }
EXPORT void f5_V_PSI_PF(void* p0, struct S_PF p1, int p2) { }
EXPORT void f5_V_PSI_PD(void* p0, struct S_PD p1, int p2) { }
EXPORT void f5_V_PSI_PP(void* p0, struct S_PP p1, int p2) { }
EXPORT void f5_V_PSI_III(void* p0, struct S_III p1, int p2) { }
EXPORT void f5_V_PSI_IIF(void* p0, struct S_IIF p1, int p2) { }
EXPORT void f5_V_PSI_IID(void* p0, struct S_IID p1, int p2) { }
EXPORT void f5_V_PSI_IIP(void* p0, struct S_IIP p1, int p2) { }
EXPORT void f5_V_PSI_IFI(void* p0, struct S_IFI p1, int p2) { }
EXPORT void f5_V_PSI_IFF(void* p0, struct S_IFF p1, int p2) { }
EXPORT void f5_V_PSI_IFD(void* p0, struct S_IFD p1, int p2) { }
EXPORT void f5_V_PSI_IFP(void* p0, struct S_IFP p1, int p2) { }
EXPORT void f5_V_PSI_IDI(void* p0, struct S_IDI p1, int p2) { }
EXPORT void f5_V_PSI_IDF(void* p0, struct S_IDF p1, int p2) { }
EXPORT void f5_V_PSI_IDD(void* p0, struct S_IDD p1, int p2) { }
EXPORT void f5_V_PSI_IDP(void* p0, struct S_IDP p1, int p2) { }
EXPORT void f5_V_PSI_IPI(void* p0, struct S_IPI p1, int p2) { }
EXPORT void f5_V_PSI_IPF(void* p0, struct S_IPF p1, int p2) { }
EXPORT void f5_V_PSI_IPD(void* p0, struct S_IPD p1, int p2) { }
EXPORT void f5_V_PSI_IPP(void* p0, struct S_IPP p1, int p2) { }
EXPORT void f5_V_PSI_FII(void* p0, struct S_FII p1, int p2) { }
EXPORT void f5_V_PSI_FIF(void* p0, struct S_FIF p1, int p2) { }
EXPORT void f5_V_PSI_FID(void* p0, struct S_FID p1, int p2) { }
EXPORT void f5_V_PSI_FIP(void* p0, struct S_FIP p1, int p2) { }
EXPORT void f5_V_PSI_FFI(void* p0, struct S_FFI p1, int p2) { }
EXPORT void f5_V_PSI_FFF(void* p0, struct S_FFF p1, int p2) { }
EXPORT void f5_V_PSI_FFD(void* p0, struct S_FFD p1, int p2) { }
EXPORT void f5_V_PSI_FFP(void* p0, struct S_FFP p1, int p2) { }
EXPORT void f5_V_PSI_FDI(void* p0, struct S_FDI p1, int p2) { }
EXPORT void f5_V_PSI_FDF(void* p0, struct S_FDF p1, int p2) { }
EXPORT void f5_V_PSI_FDD(void* p0, struct S_FDD p1, int p2) { }
EXPORT void f5_V_PSI_FDP(void* p0, struct S_FDP p1, int p2) { }
EXPORT void f5_V_PSI_FPI(void* p0, struct S_FPI p1, int p2) { }
EXPORT void f5_V_PSI_FPF(void* p0, struct S_FPF p1, int p2) { }
EXPORT void f5_V_PSI_FPD(void* p0, struct S_FPD p1, int p2) { }
EXPORT void f5_V_PSI_FPP(void* p0, struct S_FPP p1, int p2) { }
EXPORT void f5_V_PSI_DII(void* p0, struct S_DII p1, int p2) { }
EXPORT void f5_V_PSI_DIF(void* p0, struct S_DIF p1, int p2) { }
EXPORT void f5_V_PSI_DID(void* p0, struct S_DID p1, int p2) { }
EXPORT void f5_V_PSI_DIP(void* p0, struct S_DIP p1, int p2) { }
EXPORT void f5_V_PSI_DFI(void* p0, struct S_DFI p1, int p2) { }
EXPORT void f5_V_PSI_DFF(void* p0, struct S_DFF p1, int p2) { }
EXPORT void f5_V_PSI_DFD(void* p0, struct S_DFD p1, int p2) { }
EXPORT void f5_V_PSI_DFP(void* p0, struct S_DFP p1, int p2) { }
EXPORT void f5_V_PSI_DDI(void* p0, struct S_DDI p1, int p2) { }
EXPORT void f5_V_PSI_DDF(void* p0, struct S_DDF p1, int p2) { }
EXPORT void f5_V_PSI_DDD(void* p0, struct S_DDD p1, int p2) { }
EXPORT void f5_V_PSI_DDP(void* p0, struct S_DDP p1, int p2) { }
EXPORT void f5_V_PSI_DPI(void* p0, struct S_DPI p1, int p2) { }
EXPORT void f5_V_PSI_DPF(void* p0, struct S_DPF p1, int p2) { }
EXPORT void f5_V_PSI_DPD(void* p0, struct S_DPD p1, int p2) { }
EXPORT void f5_V_PSI_DPP(void* p0, struct S_DPP p1, int p2) { }
EXPORT void f5_V_PSI_PII(void* p0, struct S_PII p1, int p2) { }
EXPORT void f5_V_PSI_PIF(void* p0, struct S_PIF p1, int p2) { }
EXPORT void f5_V_PSI_PID(void* p0, struct S_PID p1, int p2) { }
EXPORT void f6_V_PSI_PIP(void* p0, struct S_PIP p1, int p2) { }
EXPORT void f6_V_PSI_PFI(void* p0, struct S_PFI p1, int p2) { }
EXPORT void f6_V_PSI_PFF(void* p0, struct S_PFF p1, int p2) { }
EXPORT void f6_V_PSI_PFD(void* p0, struct S_PFD p1, int p2) { }
EXPORT void f6_V_PSI_PFP(void* p0, struct S_PFP p1, int p2) { }
EXPORT void f6_V_PSI_PDI(void* p0, struct S_PDI p1, int p2) { }
EXPORT void f6_V_PSI_PDF(void* p0, struct S_PDF p1, int p2) { }
EXPORT void f6_V_PSI_PDD(void* p0, struct S_PDD p1, int p2) { }
EXPORT void f6_V_PSI_PDP(void* p0, struct S_PDP p1, int p2) { }
EXPORT void f6_V_PSI_PPI(void* p0, struct S_PPI p1, int p2) { }
EXPORT void f6_V_PSI_PPF(void* p0, struct S_PPF p1, int p2) { }
EXPORT void f6_V_PSI_PPD(void* p0, struct S_PPD p1, int p2) { }
EXPORT void f6_V_PSI_PPP(void* p0, struct S_PPP p1, int p2) { }
EXPORT void f6_V_PSF_I(void* p0, struct S_I p1, float p2) { }
EXPORT void f6_V_PSF_F(void* p0, struct S_F p1, float p2) { }
EXPORT void f6_V_PSF_D(void* p0, struct S_D p1, float p2) { }
EXPORT void f6_V_PSF_P(void* p0, struct S_P p1, float p2) { }
EXPORT void f6_V_PSF_II(void* p0, struct S_II p1, float p2) { }
EXPORT void f6_V_PSF_IF(void* p0, struct S_IF p1, float p2) { }
EXPORT void f6_V_PSF_ID(void* p0, struct S_ID p1, float p2) { }
EXPORT void f6_V_PSF_IP(void* p0, struct S_IP p1, float p2) { }
EXPORT void f6_V_PSF_FI(void* p0, struct S_FI p1, float p2) { }
EXPORT void f6_V_PSF_FF(void* p0, struct S_FF p1, float p2) { }
EXPORT void f6_V_PSF_FD(void* p0, struct S_FD p1, float p2) { }
EXPORT void f6_V_PSF_FP(void* p0, struct S_FP p1, float p2) { }
EXPORT void f6_V_PSF_DI(void* p0, struct S_DI p1, float p2) { }
EXPORT void f6_V_PSF_DF(void* p0, struct S_DF p1, float p2) { }
EXPORT void f6_V_PSF_DD(void* p0, struct S_DD p1, float p2) { }
EXPORT void f6_V_PSF_DP(void* p0, struct S_DP p1, float p2) { }
EXPORT void f6_V_PSF_PI(void* p0, struct S_PI p1, float p2) { }
EXPORT void f6_V_PSF_PF(void* p0, struct S_PF p1, float p2) { }
EXPORT void f6_V_PSF_PD(void* p0, struct S_PD p1, float p2) { }
EXPORT void f6_V_PSF_PP(void* p0, struct S_PP p1, float p2) { }
EXPORT void f6_V_PSF_III(void* p0, struct S_III p1, float p2) { }
EXPORT void f6_V_PSF_IIF(void* p0, struct S_IIF p1, float p2) { }
EXPORT void f6_V_PSF_IID(void* p0, struct S_IID p1, float p2) { }
EXPORT void f6_V_PSF_IIP(void* p0, struct S_IIP p1, float p2) { }
EXPORT void f6_V_PSF_IFI(void* p0, struct S_IFI p1, float p2) { }
EXPORT void f6_V_PSF_IFF(void* p0, struct S_IFF p1, float p2) { }
EXPORT void f6_V_PSF_IFD(void* p0, struct S_IFD p1, float p2) { }
EXPORT void f6_V_PSF_IFP(void* p0, struct S_IFP p1, float p2) { }
EXPORT void f6_V_PSF_IDI(void* p0, struct S_IDI p1, float p2) { }
EXPORT void f6_V_PSF_IDF(void* p0, struct S_IDF p1, float p2) { }
EXPORT void f6_V_PSF_IDD(void* p0, struct S_IDD p1, float p2) { }
EXPORT void f6_V_PSF_IDP(void* p0, struct S_IDP p1, float p2) { }
EXPORT void f6_V_PSF_IPI(void* p0, struct S_IPI p1, float p2) { }
EXPORT void f6_V_PSF_IPF(void* p0, struct S_IPF p1, float p2) { }
EXPORT void f6_V_PSF_IPD(void* p0, struct S_IPD p1, float p2) { }
EXPORT void f6_V_PSF_IPP(void* p0, struct S_IPP p1, float p2) { }
EXPORT void f6_V_PSF_FII(void* p0, struct S_FII p1, float p2) { }
EXPORT void f6_V_PSF_FIF(void* p0, struct S_FIF p1, float p2) { }
EXPORT void f6_V_PSF_FID(void* p0, struct S_FID p1, float p2) { }
EXPORT void f6_V_PSF_FIP(void* p0, struct S_FIP p1, float p2) { }
EXPORT void f6_V_PSF_FFI(void* p0, struct S_FFI p1, float p2) { }
EXPORT void f6_V_PSF_FFF(void* p0, struct S_FFF p1, float p2) { }
EXPORT void f6_V_PSF_FFD(void* p0, struct S_FFD p1, float p2) { }
EXPORT void f6_V_PSF_FFP(void* p0, struct S_FFP p1, float p2) { }
EXPORT void f6_V_PSF_FDI(void* p0, struct S_FDI p1, float p2) { }
EXPORT void f6_V_PSF_FDF(void* p0, struct S_FDF p1, float p2) { }
EXPORT void f6_V_PSF_FDD(void* p0, struct S_FDD p1, float p2) { }
EXPORT void f6_V_PSF_FDP(void* p0, struct S_FDP p1, float p2) { }
EXPORT void f6_V_PSF_FPI(void* p0, struct S_FPI p1, float p2) { }
EXPORT void f6_V_PSF_FPF(void* p0, struct S_FPF p1, float p2) { }
EXPORT void f6_V_PSF_FPD(void* p0, struct S_FPD p1, float p2) { }
EXPORT void f6_V_PSF_FPP(void* p0, struct S_FPP p1, float p2) { }
EXPORT void f6_V_PSF_DII(void* p0, struct S_DII p1, float p2) { }
EXPORT void f6_V_PSF_DIF(void* p0, struct S_DIF p1, float p2) { }
EXPORT void f6_V_PSF_DID(void* p0, struct S_DID p1, float p2) { }
EXPORT void f6_V_PSF_DIP(void* p0, struct S_DIP p1, float p2) { }
EXPORT void f6_V_PSF_DFI(void* p0, struct S_DFI p1, float p2) { }
EXPORT void f6_V_PSF_DFF(void* p0, struct S_DFF p1, float p2) { }
EXPORT void f6_V_PSF_DFD(void* p0, struct S_DFD p1, float p2) { }
EXPORT void f6_V_PSF_DFP(void* p0, struct S_DFP p1, float p2) { }
EXPORT void f6_V_PSF_DDI(void* p0, struct S_DDI p1, float p2) { }
EXPORT void f6_V_PSF_DDF(void* p0, struct S_DDF p1, float p2) { }
EXPORT void f6_V_PSF_DDD(void* p0, struct S_DDD p1, float p2) { }
EXPORT void f6_V_PSF_DDP(void* p0, struct S_DDP p1, float p2) { }
EXPORT void f6_V_PSF_DPI(void* p0, struct S_DPI p1, float p2) { }
EXPORT void f6_V_PSF_DPF(void* p0, struct S_DPF p1, float p2) { }
EXPORT void f6_V_PSF_DPD(void* p0, struct S_DPD p1, float p2) { }
EXPORT void f6_V_PSF_DPP(void* p0, struct S_DPP p1, float p2) { }
EXPORT void f6_V_PSF_PII(void* p0, struct S_PII p1, float p2) { }
EXPORT void f6_V_PSF_PIF(void* p0, struct S_PIF p1, float p2) { }
EXPORT void f6_V_PSF_PID(void* p0, struct S_PID p1, float p2) { }
EXPORT void f6_V_PSF_PIP(void* p0, struct S_PIP p1, float p2) { }
EXPORT void f6_V_PSF_PFI(void* p0, struct S_PFI p1, float p2) { }
EXPORT void f6_V_PSF_PFF(void* p0, struct S_PFF p1, float p2) { }
EXPORT void f6_V_PSF_PFD(void* p0, struct S_PFD p1, float p2) { }
EXPORT void f6_V_PSF_PFP(void* p0, struct S_PFP p1, float p2) { }
EXPORT void f6_V_PSF_PDI(void* p0, struct S_PDI p1, float p2) { }
EXPORT void f6_V_PSF_PDF(void* p0, struct S_PDF p1, float p2) { }
EXPORT void f6_V_PSF_PDD(void* p0, struct S_PDD p1, float p2) { }
EXPORT void f6_V_PSF_PDP(void* p0, struct S_PDP p1, float p2) { }
EXPORT void f6_V_PSF_PPI(void* p0, struct S_PPI p1, float p2) { }
EXPORT void f6_V_PSF_PPF(void* p0, struct S_PPF p1, float p2) { }
EXPORT void f6_V_PSF_PPD(void* p0, struct S_PPD p1, float p2) { }
EXPORT void f6_V_PSF_PPP(void* p0, struct S_PPP p1, float p2) { }
EXPORT void f6_V_PSD_I(void* p0, struct S_I p1, double p2) { }
EXPORT void f6_V_PSD_F(void* p0, struct S_F p1, double p2) { }
EXPORT void f6_V_PSD_D(void* p0, struct S_D p1, double p2) { }
EXPORT void f6_V_PSD_P(void* p0, struct S_P p1, double p2) { }
EXPORT void f6_V_PSD_II(void* p0, struct S_II p1, double p2) { }
EXPORT void f6_V_PSD_IF(void* p0, struct S_IF p1, double p2) { }
EXPORT void f6_V_PSD_ID(void* p0, struct S_ID p1, double p2) { }
EXPORT void f6_V_PSD_IP(void* p0, struct S_IP p1, double p2) { }
EXPORT void f6_V_PSD_FI(void* p0, struct S_FI p1, double p2) { }
EXPORT void f6_V_PSD_FF(void* p0, struct S_FF p1, double p2) { }
EXPORT void f6_V_PSD_FD(void* p0, struct S_FD p1, double p2) { }
EXPORT void f6_V_PSD_FP(void* p0, struct S_FP p1, double p2) { }
EXPORT void f6_V_PSD_DI(void* p0, struct S_DI p1, double p2) { }
EXPORT void f6_V_PSD_DF(void* p0, struct S_DF p1, double p2) { }
EXPORT void f6_V_PSD_DD(void* p0, struct S_DD p1, double p2) { }
EXPORT void f6_V_PSD_DP(void* p0, struct S_DP p1, double p2) { }
EXPORT void f6_V_PSD_PI(void* p0, struct S_PI p1, double p2) { }
EXPORT void f6_V_PSD_PF(void* p0, struct S_PF p1, double p2) { }
EXPORT void f6_V_PSD_PD(void* p0, struct S_PD p1, double p2) { }
EXPORT void f6_V_PSD_PP(void* p0, struct S_PP p1, double p2) { }
EXPORT void f6_V_PSD_III(void* p0, struct S_III p1, double p2) { }
EXPORT void f6_V_PSD_IIF(void* p0, struct S_IIF p1, double p2) { }
EXPORT void f6_V_PSD_IID(void* p0, struct S_IID p1, double p2) { }
EXPORT void f6_V_PSD_IIP(void* p0, struct S_IIP p1, double p2) { }
EXPORT void f6_V_PSD_IFI(void* p0, struct S_IFI p1, double p2) { }
EXPORT void f6_V_PSD_IFF(void* p0, struct S_IFF p1, double p2) { }
EXPORT void f6_V_PSD_IFD(void* p0, struct S_IFD p1, double p2) { }
EXPORT void f6_V_PSD_IFP(void* p0, struct S_IFP p1, double p2) { }
EXPORT void f6_V_PSD_IDI(void* p0, struct S_IDI p1, double p2) { }
EXPORT void f6_V_PSD_IDF(void* p0, struct S_IDF p1, double p2) { }
EXPORT void f6_V_PSD_IDD(void* p0, struct S_IDD p1, double p2) { }
EXPORT void f6_V_PSD_IDP(void* p0, struct S_IDP p1, double p2) { }
EXPORT void f6_V_PSD_IPI(void* p0, struct S_IPI p1, double p2) { }
EXPORT void f6_V_PSD_IPF(void* p0, struct S_IPF p1, double p2) { }
EXPORT void f6_V_PSD_IPD(void* p0, struct S_IPD p1, double p2) { }
EXPORT void f6_V_PSD_IPP(void* p0, struct S_IPP p1, double p2) { }
EXPORT void f6_V_PSD_FII(void* p0, struct S_FII p1, double p2) { }
EXPORT void f6_V_PSD_FIF(void* p0, struct S_FIF p1, double p2) { }
EXPORT void f6_V_PSD_FID(void* p0, struct S_FID p1, double p2) { }
EXPORT void f6_V_PSD_FIP(void* p0, struct S_FIP p1, double p2) { }
EXPORT void f6_V_PSD_FFI(void* p0, struct S_FFI p1, double p2) { }
EXPORT void f6_V_PSD_FFF(void* p0, struct S_FFF p1, double p2) { }
EXPORT void f6_V_PSD_FFD(void* p0, struct S_FFD p1, double p2) { }
EXPORT void f6_V_PSD_FFP(void* p0, struct S_FFP p1, double p2) { }
EXPORT void f6_V_PSD_FDI(void* p0, struct S_FDI p1, double p2) { }
EXPORT void f6_V_PSD_FDF(void* p0, struct S_FDF p1, double p2) { }
EXPORT void f6_V_PSD_FDD(void* p0, struct S_FDD p1, double p2) { }
EXPORT void f6_V_PSD_FDP(void* p0, struct S_FDP p1, double p2) { }
EXPORT void f6_V_PSD_FPI(void* p0, struct S_FPI p1, double p2) { }
EXPORT void f6_V_PSD_FPF(void* p0, struct S_FPF p1, double p2) { }
EXPORT void f6_V_PSD_FPD(void* p0, struct S_FPD p1, double p2) { }
EXPORT void f6_V_PSD_FPP(void* p0, struct S_FPP p1, double p2) { }
EXPORT void f6_V_PSD_DII(void* p0, struct S_DII p1, double p2) { }
EXPORT void f6_V_PSD_DIF(void* p0, struct S_DIF p1, double p2) { }
EXPORT void f6_V_PSD_DID(void* p0, struct S_DID p1, double p2) { }
EXPORT void f6_V_PSD_DIP(void* p0, struct S_DIP p1, double p2) { }
EXPORT void f6_V_PSD_DFI(void* p0, struct S_DFI p1, double p2) { }
EXPORT void f6_V_PSD_DFF(void* p0, struct S_DFF p1, double p2) { }
EXPORT void f6_V_PSD_DFD(void* p0, struct S_DFD p1, double p2) { }
EXPORT void f6_V_PSD_DFP(void* p0, struct S_DFP p1, double p2) { }
EXPORT void f6_V_PSD_DDI(void* p0, struct S_DDI p1, double p2) { }
EXPORT void f6_V_PSD_DDF(void* p0, struct S_DDF p1, double p2) { }
EXPORT void f6_V_PSD_DDD(void* p0, struct S_DDD p1, double p2) { }
EXPORT void f6_V_PSD_DDP(void* p0, struct S_DDP p1, double p2) { }
EXPORT void f6_V_PSD_DPI(void* p0, struct S_DPI p1, double p2) { }
EXPORT void f6_V_PSD_DPF(void* p0, struct S_DPF p1, double p2) { }
EXPORT void f6_V_PSD_DPD(void* p0, struct S_DPD p1, double p2) { }
EXPORT void f6_V_PSD_DPP(void* p0, struct S_DPP p1, double p2) { }
EXPORT void f6_V_PSD_PII(void* p0, struct S_PII p1, double p2) { }
EXPORT void f6_V_PSD_PIF(void* p0, struct S_PIF p1, double p2) { }
EXPORT void f6_V_PSD_PID(void* p0, struct S_PID p1, double p2) { }
EXPORT void f6_V_PSD_PIP(void* p0, struct S_PIP p1, double p2) { }
EXPORT void f6_V_PSD_PFI(void* p0, struct S_PFI p1, double p2) { }
EXPORT void f6_V_PSD_PFF(void* p0, struct S_PFF p1, double p2) { }
EXPORT void f6_V_PSD_PFD(void* p0, struct S_PFD p1, double p2) { }
EXPORT void f6_V_PSD_PFP(void* p0, struct S_PFP p1, double p2) { }
EXPORT void f6_V_PSD_PDI(void* p0, struct S_PDI p1, double p2) { }
EXPORT void f6_V_PSD_PDF(void* p0, struct S_PDF p1, double p2) { }
EXPORT void f6_V_PSD_PDD(void* p0, struct S_PDD p1, double p2) { }
EXPORT void f6_V_PSD_PDP(void* p0, struct S_PDP p1, double p2) { }
EXPORT void f6_V_PSD_PPI(void* p0, struct S_PPI p1, double p2) { }
EXPORT void f6_V_PSD_PPF(void* p0, struct S_PPF p1, double p2) { }
EXPORT void f6_V_PSD_PPD(void* p0, struct S_PPD p1, double p2) { }
EXPORT void f6_V_PSD_PPP(void* p0, struct S_PPP p1, double p2) { }
EXPORT void f6_V_PSP_I(void* p0, struct S_I p1, void* p2) { }
EXPORT void f6_V_PSP_F(void* p0, struct S_F p1, void* p2) { }
EXPORT void f6_V_PSP_D(void* p0, struct S_D p1, void* p2) { }
EXPORT void f6_V_PSP_P(void* p0, struct S_P p1, void* p2) { }
EXPORT void f6_V_PSP_II(void* p0, struct S_II p1, void* p2) { }
EXPORT void f6_V_PSP_IF(void* p0, struct S_IF p1, void* p2) { }
EXPORT void f6_V_PSP_ID(void* p0, struct S_ID p1, void* p2) { }
EXPORT void f6_V_PSP_IP(void* p0, struct S_IP p1, void* p2) { }
EXPORT void f6_V_PSP_FI(void* p0, struct S_FI p1, void* p2) { }
EXPORT void f6_V_PSP_FF(void* p0, struct S_FF p1, void* p2) { }
EXPORT void f6_V_PSP_FD(void* p0, struct S_FD p1, void* p2) { }
EXPORT void f6_V_PSP_FP(void* p0, struct S_FP p1, void* p2) { }
EXPORT void f6_V_PSP_DI(void* p0, struct S_DI p1, void* p2) { }
EXPORT void f6_V_PSP_DF(void* p0, struct S_DF p1, void* p2) { }
EXPORT void f6_V_PSP_DD(void* p0, struct S_DD p1, void* p2) { }
EXPORT void f6_V_PSP_DP(void* p0, struct S_DP p1, void* p2) { }
EXPORT void f6_V_PSP_PI(void* p0, struct S_PI p1, void* p2) { }
EXPORT void f6_V_PSP_PF(void* p0, struct S_PF p1, void* p2) { }
EXPORT void f6_V_PSP_PD(void* p0, struct S_PD p1, void* p2) { }
EXPORT void f6_V_PSP_PP(void* p0, struct S_PP p1, void* p2) { }
EXPORT void f6_V_PSP_III(void* p0, struct S_III p1, void* p2) { }
EXPORT void f6_V_PSP_IIF(void* p0, struct S_IIF p1, void* p2) { }
EXPORT void f6_V_PSP_IID(void* p0, struct S_IID p1, void* p2) { }
EXPORT void f6_V_PSP_IIP(void* p0, struct S_IIP p1, void* p2) { }
EXPORT void f6_V_PSP_IFI(void* p0, struct S_IFI p1, void* p2) { }
EXPORT void f6_V_PSP_IFF(void* p0, struct S_IFF p1, void* p2) { }
EXPORT void f6_V_PSP_IFD(void* p0, struct S_IFD p1, void* p2) { }
EXPORT void f6_V_PSP_IFP(void* p0, struct S_IFP p1, void* p2) { }
EXPORT void f6_V_PSP_IDI(void* p0, struct S_IDI p1, void* p2) { }
EXPORT void f6_V_PSP_IDF(void* p0, struct S_IDF p1, void* p2) { }
EXPORT void f6_V_PSP_IDD(void* p0, struct S_IDD p1, void* p2) { }
EXPORT void f6_V_PSP_IDP(void* p0, struct S_IDP p1, void* p2) { }
EXPORT void f6_V_PSP_IPI(void* p0, struct S_IPI p1, void* p2) { }
EXPORT void f6_V_PSP_IPF(void* p0, struct S_IPF p1, void* p2) { }
EXPORT void f6_V_PSP_IPD(void* p0, struct S_IPD p1, void* p2) { }
EXPORT void f6_V_PSP_IPP(void* p0, struct S_IPP p1, void* p2) { }
EXPORT void f6_V_PSP_FII(void* p0, struct S_FII p1, void* p2) { }
EXPORT void f6_V_PSP_FIF(void* p0, struct S_FIF p1, void* p2) { }
EXPORT void f6_V_PSP_FID(void* p0, struct S_FID p1, void* p2) { }
EXPORT void f6_V_PSP_FIP(void* p0, struct S_FIP p1, void* p2) { }
EXPORT void f6_V_PSP_FFI(void* p0, struct S_FFI p1, void* p2) { }
EXPORT void f6_V_PSP_FFF(void* p0, struct S_FFF p1, void* p2) { }
EXPORT void f6_V_PSP_FFD(void* p0, struct S_FFD p1, void* p2) { }
EXPORT void f6_V_PSP_FFP(void* p0, struct S_FFP p1, void* p2) { }
EXPORT void f6_V_PSP_FDI(void* p0, struct S_FDI p1, void* p2) { }
EXPORT void f6_V_PSP_FDF(void* p0, struct S_FDF p1, void* p2) { }
EXPORT void f6_V_PSP_FDD(void* p0, struct S_FDD p1, void* p2) { }
EXPORT void f6_V_PSP_FDP(void* p0, struct S_FDP p1, void* p2) { }
EXPORT void f6_V_PSP_FPI(void* p0, struct S_FPI p1, void* p2) { }
EXPORT void f6_V_PSP_FPF(void* p0, struct S_FPF p1, void* p2) { }
EXPORT void f6_V_PSP_FPD(void* p0, struct S_FPD p1, void* p2) { }
EXPORT void f6_V_PSP_FPP(void* p0, struct S_FPP p1, void* p2) { }
EXPORT void f6_V_PSP_DII(void* p0, struct S_DII p1, void* p2) { }
EXPORT void f6_V_PSP_DIF(void* p0, struct S_DIF p1, void* p2) { }
EXPORT void f6_V_PSP_DID(void* p0, struct S_DID p1, void* p2) { }
EXPORT void f6_V_PSP_DIP(void* p0, struct S_DIP p1, void* p2) { }
EXPORT void f6_V_PSP_DFI(void* p0, struct S_DFI p1, void* p2) { }
EXPORT void f6_V_PSP_DFF(void* p0, struct S_DFF p1, void* p2) { }
EXPORT void f6_V_PSP_DFD(void* p0, struct S_DFD p1, void* p2) { }
EXPORT void f6_V_PSP_DFP(void* p0, struct S_DFP p1, void* p2) { }
EXPORT void f6_V_PSP_DDI(void* p0, struct S_DDI p1, void* p2) { }
EXPORT void f6_V_PSP_DDF(void* p0, struct S_DDF p1, void* p2) { }
EXPORT void f6_V_PSP_DDD(void* p0, struct S_DDD p1, void* p2) { }
EXPORT void f6_V_PSP_DDP(void* p0, struct S_DDP p1, void* p2) { }
EXPORT void f6_V_PSP_DPI(void* p0, struct S_DPI p1, void* p2) { }
EXPORT void f6_V_PSP_DPF(void* p0, struct S_DPF p1, void* p2) { }
EXPORT void f6_V_PSP_DPD(void* p0, struct S_DPD p1, void* p2) { }
EXPORT void f6_V_PSP_DPP(void* p0, struct S_DPP p1, void* p2) { }
EXPORT void f6_V_PSP_PII(void* p0, struct S_PII p1, void* p2) { }
EXPORT void f6_V_PSP_PIF(void* p0, struct S_PIF p1, void* p2) { }
EXPORT void f6_V_PSP_PID(void* p0, struct S_PID p1, void* p2) { }
EXPORT void f6_V_PSP_PIP(void* p0, struct S_PIP p1, void* p2) { }
EXPORT void f6_V_PSP_PFI(void* p0, struct S_PFI p1, void* p2) { }
EXPORT void f6_V_PSP_PFF(void* p0, struct S_PFF p1, void* p2) { }
EXPORT void f6_V_PSP_PFD(void* p0, struct S_PFD p1, void* p2) { }
EXPORT void f6_V_PSP_PFP(void* p0, struct S_PFP p1, void* p2) { }
EXPORT void f6_V_PSP_PDI(void* p0, struct S_PDI p1, void* p2) { }
EXPORT void f6_V_PSP_PDF(void* p0, struct S_PDF p1, void* p2) { }
EXPORT void f6_V_PSP_PDD(void* p0, struct S_PDD p1, void* p2) { }
EXPORT void f6_V_PSP_PDP(void* p0, struct S_PDP p1, void* p2) { }
EXPORT void f6_V_PSP_PPI(void* p0, struct S_PPI p1, void* p2) { }
EXPORT void f6_V_PSP_PPF(void* p0, struct S_PPF p1, void* p2) { }
EXPORT void f6_V_PSP_PPD(void* p0, struct S_PPD p1, void* p2) { }
EXPORT void f6_V_PSP_PPP(void* p0, struct S_PPP p1, void* p2) { }
EXPORT void f6_V_PSS_I(void* p0, struct S_I p1, struct S_I p2) { }
EXPORT void f6_V_PSS_F(void* p0, struct S_F p1, struct S_F p2) { }
EXPORT void f6_V_PSS_D(void* p0, struct S_D p1, struct S_D p2) { }
EXPORT void f6_V_PSS_P(void* p0, struct S_P p1, struct S_P p2) { }
EXPORT void f6_V_PSS_II(void* p0, struct S_II p1, struct S_II p2) { }
EXPORT void f6_V_PSS_IF(void* p0, struct S_IF p1, struct S_IF p2) { }
EXPORT void f6_V_PSS_ID(void* p0, struct S_ID p1, struct S_ID p2) { }
EXPORT void f6_V_PSS_IP(void* p0, struct S_IP p1, struct S_IP p2) { }
EXPORT void f6_V_PSS_FI(void* p0, struct S_FI p1, struct S_FI p2) { }
EXPORT void f6_V_PSS_FF(void* p0, struct S_FF p1, struct S_FF p2) { }
EXPORT void f6_V_PSS_FD(void* p0, struct S_FD p1, struct S_FD p2) { }
EXPORT void f6_V_PSS_FP(void* p0, struct S_FP p1, struct S_FP p2) { }
EXPORT void f6_V_PSS_DI(void* p0, struct S_DI p1, struct S_DI p2) { }
EXPORT void f6_V_PSS_DF(void* p0, struct S_DF p1, struct S_DF p2) { }
EXPORT void f6_V_PSS_DD(void* p0, struct S_DD p1, struct S_DD p2) { }
EXPORT void f6_V_PSS_DP(void* p0, struct S_DP p1, struct S_DP p2) { }
EXPORT void f6_V_PSS_PI(void* p0, struct S_PI p1, struct S_PI p2) { }
EXPORT void f6_V_PSS_PF(void* p0, struct S_PF p1, struct S_PF p2) { }
EXPORT void f6_V_PSS_PD(void* p0, struct S_PD p1, struct S_PD p2) { }
EXPORT void f6_V_PSS_PP(void* p0, struct S_PP p1, struct S_PP p2) { }
EXPORT void f6_V_PSS_III(void* p0, struct S_III p1, struct S_III p2) { }
EXPORT void f6_V_PSS_IIF(void* p0, struct S_IIF p1, struct S_IIF p2) { }
EXPORT void f6_V_PSS_IID(void* p0, struct S_IID p1, struct S_IID p2) { }
EXPORT void f6_V_PSS_IIP(void* p0, struct S_IIP p1, struct S_IIP p2) { }
EXPORT void f6_V_PSS_IFI(void* p0, struct S_IFI p1, struct S_IFI p2) { }
EXPORT void f6_V_PSS_IFF(void* p0, struct S_IFF p1, struct S_IFF p2) { }
EXPORT void f6_V_PSS_IFD(void* p0, struct S_IFD p1, struct S_IFD p2) { }
EXPORT void f6_V_PSS_IFP(void* p0, struct S_IFP p1, struct S_IFP p2) { }
EXPORT void f6_V_PSS_IDI(void* p0, struct S_IDI p1, struct S_IDI p2) { }
EXPORT void f6_V_PSS_IDF(void* p0, struct S_IDF p1, struct S_IDF p2) { }
EXPORT void f6_V_PSS_IDD(void* p0, struct S_IDD p1, struct S_IDD p2) { }
EXPORT void f6_V_PSS_IDP(void* p0, struct S_IDP p1, struct S_IDP p2) { }
EXPORT void f6_V_PSS_IPI(void* p0, struct S_IPI p1, struct S_IPI p2) { }
EXPORT void f6_V_PSS_IPF(void* p0, struct S_IPF p1, struct S_IPF p2) { }
EXPORT void f6_V_PSS_IPD(void* p0, struct S_IPD p1, struct S_IPD p2) { }
EXPORT void f6_V_PSS_IPP(void* p0, struct S_IPP p1, struct S_IPP p2) { }
EXPORT void f6_V_PSS_FII(void* p0, struct S_FII p1, struct S_FII p2) { }
EXPORT void f6_V_PSS_FIF(void* p0, struct S_FIF p1, struct S_FIF p2) { }
EXPORT void f6_V_PSS_FID(void* p0, struct S_FID p1, struct S_FID p2) { }
EXPORT void f6_V_PSS_FIP(void* p0, struct S_FIP p1, struct S_FIP p2) { }
EXPORT void f6_V_PSS_FFI(void* p0, struct S_FFI p1, struct S_FFI p2) { }
EXPORT void f6_V_PSS_FFF(void* p0, struct S_FFF p1, struct S_FFF p2) { }
EXPORT void f6_V_PSS_FFD(void* p0, struct S_FFD p1, struct S_FFD p2) { }
EXPORT void f6_V_PSS_FFP(void* p0, struct S_FFP p1, struct S_FFP p2) { }
EXPORT void f6_V_PSS_FDI(void* p0, struct S_FDI p1, struct S_FDI p2) { }
EXPORT void f6_V_PSS_FDF(void* p0, struct S_FDF p1, struct S_FDF p2) { }
EXPORT void f6_V_PSS_FDD(void* p0, struct S_FDD p1, struct S_FDD p2) { }
EXPORT void f6_V_PSS_FDP(void* p0, struct S_FDP p1, struct S_FDP p2) { }
EXPORT void f6_V_PSS_FPI(void* p0, struct S_FPI p1, struct S_FPI p2) { }
EXPORT void f6_V_PSS_FPF(void* p0, struct S_FPF p1, struct S_FPF p2) { }
EXPORT void f6_V_PSS_FPD(void* p0, struct S_FPD p1, struct S_FPD p2) { }
EXPORT void f6_V_PSS_FPP(void* p0, struct S_FPP p1, struct S_FPP p2) { }
EXPORT void f6_V_PSS_DII(void* p0, struct S_DII p1, struct S_DII p2) { }
EXPORT void f6_V_PSS_DIF(void* p0, struct S_DIF p1, struct S_DIF p2) { }
EXPORT void f6_V_PSS_DID(void* p0, struct S_DID p1, struct S_DID p2) { }
EXPORT void f6_V_PSS_DIP(void* p0, struct S_DIP p1, struct S_DIP p2) { }
EXPORT void f6_V_PSS_DFI(void* p0, struct S_DFI p1, struct S_DFI p2) { }
EXPORT void f6_V_PSS_DFF(void* p0, struct S_DFF p1, struct S_DFF p2) { }
EXPORT void f6_V_PSS_DFD(void* p0, struct S_DFD p1, struct S_DFD p2) { }
EXPORT void f6_V_PSS_DFP(void* p0, struct S_DFP p1, struct S_DFP p2) { }
EXPORT void f6_V_PSS_DDI(void* p0, struct S_DDI p1, struct S_DDI p2) { }
EXPORT void f6_V_PSS_DDF(void* p0, struct S_DDF p1, struct S_DDF p2) { }
EXPORT void f6_V_PSS_DDD(void* p0, struct S_DDD p1, struct S_DDD p2) { }
EXPORT void f6_V_PSS_DDP(void* p0, struct S_DDP p1, struct S_DDP p2) { }
EXPORT void f6_V_PSS_DPI(void* p0, struct S_DPI p1, struct S_DPI p2) { }
EXPORT void f6_V_PSS_DPF(void* p0, struct S_DPF p1, struct S_DPF p2) { }
EXPORT void f6_V_PSS_DPD(void* p0, struct S_DPD p1, struct S_DPD p2) { }
EXPORT void f6_V_PSS_DPP(void* p0, struct S_DPP p1, struct S_DPP p2) { }
EXPORT void f6_V_PSS_PII(void* p0, struct S_PII p1, struct S_PII p2) { }
EXPORT void f6_V_PSS_PIF(void* p0, struct S_PIF p1, struct S_PIF p2) { }
EXPORT void f6_V_PSS_PID(void* p0, struct S_PID p1, struct S_PID p2) { }
EXPORT void f6_V_PSS_PIP(void* p0, struct S_PIP p1, struct S_PIP p2) { }
EXPORT void f6_V_PSS_PFI(void* p0, struct S_PFI p1, struct S_PFI p2) { }
EXPORT void f6_V_PSS_PFF(void* p0, struct S_PFF p1, struct S_PFF p2) { }
EXPORT void f6_V_PSS_PFD(void* p0, struct S_PFD p1, struct S_PFD p2) { }
EXPORT void f6_V_PSS_PFP(void* p0, struct S_PFP p1, struct S_PFP p2) { }
EXPORT void f6_V_PSS_PDI(void* p0, struct S_PDI p1, struct S_PDI p2) { }
EXPORT void f6_V_PSS_PDF(void* p0, struct S_PDF p1, struct S_PDF p2) { }
EXPORT void f6_V_PSS_PDD(void* p0, struct S_PDD p1, struct S_PDD p2) { }
EXPORT void f6_V_PSS_PDP(void* p0, struct S_PDP p1, struct S_PDP p2) { }
EXPORT void f6_V_PSS_PPI(void* p0, struct S_PPI p1, struct S_PPI p2) { }
EXPORT void f6_V_PSS_PPF(void* p0, struct S_PPF p1, struct S_PPF p2) { }
EXPORT void f6_V_PSS_PPD(void* p0, struct S_PPD p1, struct S_PPD p2) { }
EXPORT void f6_V_PSS_PPP(void* p0, struct S_PPP p1, struct S_PPP p2) { }
EXPORT void f6_V_SII_I(struct S_I p0, int p1, int p2) { }
EXPORT void f6_V_SII_F(struct S_F p0, int p1, int p2) { }
EXPORT void f6_V_SII_D(struct S_D p0, int p1, int p2) { }
EXPORT void f6_V_SII_P(struct S_P p0, int p1, int p2) { }
EXPORT void f6_V_SII_II(struct S_II p0, int p1, int p2) { }
EXPORT void f6_V_SII_IF(struct S_IF p0, int p1, int p2) { }
EXPORT void f6_V_SII_ID(struct S_ID p0, int p1, int p2) { }
EXPORT void f6_V_SII_IP(struct S_IP p0, int p1, int p2) { }
EXPORT void f6_V_SII_FI(struct S_FI p0, int p1, int p2) { }
EXPORT void f6_V_SII_FF(struct S_FF p0, int p1, int p2) { }
EXPORT void f6_V_SII_FD(struct S_FD p0, int p1, int p2) { }
EXPORT void f6_V_SII_FP(struct S_FP p0, int p1, int p2) { }
EXPORT void f6_V_SII_DI(struct S_DI p0, int p1, int p2) { }
EXPORT void f6_V_SII_DF(struct S_DF p0, int p1, int p2) { }
EXPORT void f6_V_SII_DD(struct S_DD p0, int p1, int p2) { }
EXPORT void f6_V_SII_DP(struct S_DP p0, int p1, int p2) { }
EXPORT void f6_V_SII_PI(struct S_PI p0, int p1, int p2) { }
EXPORT void f6_V_SII_PF(struct S_PF p0, int p1, int p2) { }
EXPORT void f6_V_SII_PD(struct S_PD p0, int p1, int p2) { }
EXPORT void f6_V_SII_PP(struct S_PP p0, int p1, int p2) { }
EXPORT void f6_V_SII_III(struct S_III p0, int p1, int p2) { }
EXPORT void f6_V_SII_IIF(struct S_IIF p0, int p1, int p2) { }
EXPORT void f6_V_SII_IID(struct S_IID p0, int p1, int p2) { }
EXPORT void f6_V_SII_IIP(struct S_IIP p0, int p1, int p2) { }
EXPORT void f6_V_SII_IFI(struct S_IFI p0, int p1, int p2) { }
EXPORT void f6_V_SII_IFF(struct S_IFF p0, int p1, int p2) { }
EXPORT void f6_V_SII_IFD(struct S_IFD p0, int p1, int p2) { }
EXPORT void f6_V_SII_IFP(struct S_IFP p0, int p1, int p2) { }
EXPORT void f6_V_SII_IDI(struct S_IDI p0, int p1, int p2) { }
EXPORT void f6_V_SII_IDF(struct S_IDF p0, int p1, int p2) { }
EXPORT void f6_V_SII_IDD(struct S_IDD p0, int p1, int p2) { }
EXPORT void f6_V_SII_IDP(struct S_IDP p0, int p1, int p2) { }
EXPORT void f6_V_SII_IPI(struct S_IPI p0, int p1, int p2) { }
EXPORT void f6_V_SII_IPF(struct S_IPF p0, int p1, int p2) { }
EXPORT void f6_V_SII_IPD(struct S_IPD p0, int p1, int p2) { }
EXPORT void f6_V_SII_IPP(struct S_IPP p0, int p1, int p2) { }
EXPORT void f6_V_SII_FII(struct S_FII p0, int p1, int p2) { }
EXPORT void f6_V_SII_FIF(struct S_FIF p0, int p1, int p2) { }
EXPORT void f6_V_SII_FID(struct S_FID p0, int p1, int p2) { }
EXPORT void f6_V_SII_FIP(struct S_FIP p0, int p1, int p2) { }
EXPORT void f6_V_SII_FFI(struct S_FFI p0, int p1, int p2) { }
EXPORT void f6_V_SII_FFF(struct S_FFF p0, int p1, int p2) { }
EXPORT void f6_V_SII_FFD(struct S_FFD p0, int p1, int p2) { }
EXPORT void f6_V_SII_FFP(struct S_FFP p0, int p1, int p2) { }
EXPORT void f6_V_SII_FDI(struct S_FDI p0, int p1, int p2) { }
EXPORT void f6_V_SII_FDF(struct S_FDF p0, int p1, int p2) { }
EXPORT void f6_V_SII_FDD(struct S_FDD p0, int p1, int p2) { }
EXPORT void f6_V_SII_FDP(struct S_FDP p0, int p1, int p2) { }
EXPORT void f6_V_SII_FPI(struct S_FPI p0, int p1, int p2) { }
EXPORT void f6_V_SII_FPF(struct S_FPF p0, int p1, int p2) { }
EXPORT void f6_V_SII_FPD(struct S_FPD p0, int p1, int p2) { }
EXPORT void f6_V_SII_FPP(struct S_FPP p0, int p1, int p2) { }
EXPORT void f6_V_SII_DII(struct S_DII p0, int p1, int p2) { }
EXPORT void f6_V_SII_DIF(struct S_DIF p0, int p1, int p2) { }
EXPORT void f6_V_SII_DID(struct S_DID p0, int p1, int p2) { }
EXPORT void f6_V_SII_DIP(struct S_DIP p0, int p1, int p2) { }
EXPORT void f6_V_SII_DFI(struct S_DFI p0, int p1, int p2) { }
EXPORT void f6_V_SII_DFF(struct S_DFF p0, int p1, int p2) { }
EXPORT void f6_V_SII_DFD(struct S_DFD p0, int p1, int p2) { }
EXPORT void f6_V_SII_DFP(struct S_DFP p0, int p1, int p2) { }
EXPORT void f6_V_SII_DDI(struct S_DDI p0, int p1, int p2) { }
EXPORT void f6_V_SII_DDF(struct S_DDF p0, int p1, int p2) { }
EXPORT void f6_V_SII_DDD(struct S_DDD p0, int p1, int p2) { }
EXPORT void f6_V_SII_DDP(struct S_DDP p0, int p1, int p2) { }
EXPORT void f6_V_SII_DPI(struct S_DPI p0, int p1, int p2) { }
EXPORT void f6_V_SII_DPF(struct S_DPF p0, int p1, int p2) { }
EXPORT void f6_V_SII_DPD(struct S_DPD p0, int p1, int p2) { }
EXPORT void f6_V_SII_DPP(struct S_DPP p0, int p1, int p2) { }
EXPORT void f6_V_SII_PII(struct S_PII p0, int p1, int p2) { }
EXPORT void f6_V_SII_PIF(struct S_PIF p0, int p1, int p2) { }
EXPORT void f6_V_SII_PID(struct S_PID p0, int p1, int p2) { }
EXPORT void f6_V_SII_PIP(struct S_PIP p0, int p1, int p2) { }
EXPORT void f6_V_SII_PFI(struct S_PFI p0, int p1, int p2) { }
EXPORT void f6_V_SII_PFF(struct S_PFF p0, int p1, int p2) { }
EXPORT void f6_V_SII_PFD(struct S_PFD p0, int p1, int p2) { }
EXPORT void f6_V_SII_PFP(struct S_PFP p0, int p1, int p2) { }
EXPORT void f6_V_SII_PDI(struct S_PDI p0, int p1, int p2) { }
EXPORT void f6_V_SII_PDF(struct S_PDF p0, int p1, int p2) { }
EXPORT void f6_V_SII_PDD(struct S_PDD p0, int p1, int p2) { }
EXPORT void f6_V_SII_PDP(struct S_PDP p0, int p1, int p2) { }
EXPORT void f6_V_SII_PPI(struct S_PPI p0, int p1, int p2) { }
EXPORT void f6_V_SII_PPF(struct S_PPF p0, int p1, int p2) { }
EXPORT void f6_V_SII_PPD(struct S_PPD p0, int p1, int p2) { }
EXPORT void f6_V_SII_PPP(struct S_PPP p0, int p1, int p2) { }
EXPORT void f6_V_SIF_I(struct S_I p0, int p1, float p2) { }
EXPORT void f6_V_SIF_F(struct S_F p0, int p1, float p2) { }
EXPORT void f6_V_SIF_D(struct S_D p0, int p1, float p2) { }
EXPORT void f6_V_SIF_P(struct S_P p0, int p1, float p2) { }
EXPORT void f6_V_SIF_II(struct S_II p0, int p1, float p2) { }
EXPORT void f6_V_SIF_IF(struct S_IF p0, int p1, float p2) { }
EXPORT void f6_V_SIF_ID(struct S_ID p0, int p1, float p2) { }
EXPORT void f6_V_SIF_IP(struct S_IP p0, int p1, float p2) { }
EXPORT void f6_V_SIF_FI(struct S_FI p0, int p1, float p2) { }
EXPORT void f6_V_SIF_FF(struct S_FF p0, int p1, float p2) { }
EXPORT void f6_V_SIF_FD(struct S_FD p0, int p1, float p2) { }
EXPORT void f6_V_SIF_FP(struct S_FP p0, int p1, float p2) { }
EXPORT void f6_V_SIF_DI(struct S_DI p0, int p1, float p2) { }
EXPORT void f6_V_SIF_DF(struct S_DF p0, int p1, float p2) { }
EXPORT void f6_V_SIF_DD(struct S_DD p0, int p1, float p2) { }
EXPORT void f6_V_SIF_DP(struct S_DP p0, int p1, float p2) { }
EXPORT void f6_V_SIF_PI(struct S_PI p0, int p1, float p2) { }
EXPORT void f6_V_SIF_PF(struct S_PF p0, int p1, float p2) { }
EXPORT void f6_V_SIF_PD(struct S_PD p0, int p1, float p2) { }
EXPORT void f6_V_SIF_PP(struct S_PP p0, int p1, float p2) { }
EXPORT void f6_V_SIF_III(struct S_III p0, int p1, float p2) { }
EXPORT void f6_V_SIF_IIF(struct S_IIF p0, int p1, float p2) { }
EXPORT void f6_V_SIF_IID(struct S_IID p0, int p1, float p2) { }
EXPORT void f6_V_SIF_IIP(struct S_IIP p0, int p1, float p2) { }
EXPORT void f6_V_SIF_IFI(struct S_IFI p0, int p1, float p2) { }
EXPORT void f6_V_SIF_IFF(struct S_IFF p0, int p1, float p2) { }
EXPORT void f6_V_SIF_IFD(struct S_IFD p0, int p1, float p2) { }
EXPORT void f6_V_SIF_IFP(struct S_IFP p0, int p1, float p2) { }
EXPORT void f6_V_SIF_IDI(struct S_IDI p0, int p1, float p2) { }
EXPORT void f6_V_SIF_IDF(struct S_IDF p0, int p1, float p2) { }
EXPORT void f6_V_SIF_IDD(struct S_IDD p0, int p1, float p2) { }
EXPORT void f6_V_SIF_IDP(struct S_IDP p0, int p1, float p2) { }
EXPORT void f6_V_SIF_IPI(struct S_IPI p0, int p1, float p2) { }
EXPORT void f6_V_SIF_IPF(struct S_IPF p0, int p1, float p2) { }
EXPORT void f6_V_SIF_IPD(struct S_IPD p0, int p1, float p2) { }
EXPORT void f6_V_SIF_IPP(struct S_IPP p0, int p1, float p2) { }
EXPORT void f6_V_SIF_FII(struct S_FII p0, int p1, float p2) { }
EXPORT void f6_V_SIF_FIF(struct S_FIF p0, int p1, float p2) { }
EXPORT void f6_V_SIF_FID(struct S_FID p0, int p1, float p2) { }
EXPORT void f6_V_SIF_FIP(struct S_FIP p0, int p1, float p2) { }
EXPORT void f6_V_SIF_FFI(struct S_FFI p0, int p1, float p2) { }
EXPORT void f6_V_SIF_FFF(struct S_FFF p0, int p1, float p2) { }
EXPORT void f6_V_SIF_FFD(struct S_FFD p0, int p1, float p2) { }
EXPORT void f6_V_SIF_FFP(struct S_FFP p0, int p1, float p2) { }
EXPORT void f6_V_SIF_FDI(struct S_FDI p0, int p1, float p2) { }
EXPORT void f6_V_SIF_FDF(struct S_FDF p0, int p1, float p2) { }
EXPORT void f6_V_SIF_FDD(struct S_FDD p0, int p1, float p2) { }
EXPORT void f6_V_SIF_FDP(struct S_FDP p0, int p1, float p2) { }
EXPORT void f6_V_SIF_FPI(struct S_FPI p0, int p1, float p2) { }
EXPORT void f6_V_SIF_FPF(struct S_FPF p0, int p1, float p2) { }
EXPORT void f6_V_SIF_FPD(struct S_FPD p0, int p1, float p2) { }
EXPORT void f6_V_SIF_FPP(struct S_FPP p0, int p1, float p2) { }
EXPORT void f6_V_SIF_DII(struct S_DII p0, int p1, float p2) { }
EXPORT void f6_V_SIF_DIF(struct S_DIF p0, int p1, float p2) { }
EXPORT void f6_V_SIF_DID(struct S_DID p0, int p1, float p2) { }
EXPORT void f6_V_SIF_DIP(struct S_DIP p0, int p1, float p2) { }
EXPORT void f6_V_SIF_DFI(struct S_DFI p0, int p1, float p2) { }
EXPORT void f6_V_SIF_DFF(struct S_DFF p0, int p1, float p2) { }
EXPORT void f6_V_SIF_DFD(struct S_DFD p0, int p1, float p2) { }
EXPORT void f6_V_SIF_DFP(struct S_DFP p0, int p1, float p2) { }
EXPORT void f6_V_SIF_DDI(struct S_DDI p0, int p1, float p2) { }
EXPORT void f6_V_SIF_DDF(struct S_DDF p0, int p1, float p2) { }
EXPORT void f6_V_SIF_DDD(struct S_DDD p0, int p1, float p2) { }
EXPORT void f6_V_SIF_DDP(struct S_DDP p0, int p1, float p2) { }
EXPORT void f6_V_SIF_DPI(struct S_DPI p0, int p1, float p2) { }
EXPORT void f6_V_SIF_DPF(struct S_DPF p0, int p1, float p2) { }
EXPORT void f6_V_SIF_DPD(struct S_DPD p0, int p1, float p2) { }
EXPORT void f6_V_SIF_DPP(struct S_DPP p0, int p1, float p2) { }
EXPORT void f6_V_SIF_PII(struct S_PII p0, int p1, float p2) { }
EXPORT void f6_V_SIF_PIF(struct S_PIF p0, int p1, float p2) { }
EXPORT void f6_V_SIF_PID(struct S_PID p0, int p1, float p2) { }
EXPORT void f6_V_SIF_PIP(struct S_PIP p0, int p1, float p2) { }
EXPORT void f6_V_SIF_PFI(struct S_PFI p0, int p1, float p2) { }
EXPORT void f6_V_SIF_PFF(struct S_PFF p0, int p1, float p2) { }
EXPORT void f6_V_SIF_PFD(struct S_PFD p0, int p1, float p2) { }
EXPORT void f6_V_SIF_PFP(struct S_PFP p0, int p1, float p2) { }
EXPORT void f6_V_SIF_PDI(struct S_PDI p0, int p1, float p2) { }
EXPORT void f6_V_SIF_PDF(struct S_PDF p0, int p1, float p2) { }
EXPORT void f6_V_SIF_PDD(struct S_PDD p0, int p1, float p2) { }
EXPORT void f6_V_SIF_PDP(struct S_PDP p0, int p1, float p2) { }
EXPORT void f6_V_SIF_PPI(struct S_PPI p0, int p1, float p2) { }
EXPORT void f6_V_SIF_PPF(struct S_PPF p0, int p1, float p2) { }
EXPORT void f6_V_SIF_PPD(struct S_PPD p0, int p1, float p2) { }
EXPORT void f6_V_SIF_PPP(struct S_PPP p0, int p1, float p2) { }
EXPORT void f6_V_SID_I(struct S_I p0, int p1, double p2) { }
EXPORT void f6_V_SID_F(struct S_F p0, int p1, double p2) { }
EXPORT void f6_V_SID_D(struct S_D p0, int p1, double p2) { }
EXPORT void f6_V_SID_P(struct S_P p0, int p1, double p2) { }
EXPORT void f6_V_SID_II(struct S_II p0, int p1, double p2) { }
EXPORT void f6_V_SID_IF(struct S_IF p0, int p1, double p2) { }
EXPORT void f6_V_SID_ID(struct S_ID p0, int p1, double p2) { }
EXPORT void f6_V_SID_IP(struct S_IP p0, int p1, double p2) { }
EXPORT void f6_V_SID_FI(struct S_FI p0, int p1, double p2) { }
EXPORT void f6_V_SID_FF(struct S_FF p0, int p1, double p2) { }
EXPORT void f6_V_SID_FD(struct S_FD p0, int p1, double p2) { }
EXPORT void f6_V_SID_FP(struct S_FP p0, int p1, double p2) { }
EXPORT void f6_V_SID_DI(struct S_DI p0, int p1, double p2) { }
EXPORT void f6_V_SID_DF(struct S_DF p0, int p1, double p2) { }
EXPORT void f6_V_SID_DD(struct S_DD p0, int p1, double p2) { }
EXPORT void f6_V_SID_DP(struct S_DP p0, int p1, double p2) { }
EXPORT void f6_V_SID_PI(struct S_PI p0, int p1, double p2) { }
EXPORT void f6_V_SID_PF(struct S_PF p0, int p1, double p2) { }
EXPORT void f6_V_SID_PD(struct S_PD p0, int p1, double p2) { }
EXPORT void f6_V_SID_PP(struct S_PP p0, int p1, double p2) { }
EXPORT void f6_V_SID_III(struct S_III p0, int p1, double p2) { }
EXPORT void f6_V_SID_IIF(struct S_IIF p0, int p1, double p2) { }
EXPORT void f6_V_SID_IID(struct S_IID p0, int p1, double p2) { }
EXPORT void f6_V_SID_IIP(struct S_IIP p0, int p1, double p2) { }
EXPORT void f6_V_SID_IFI(struct S_IFI p0, int p1, double p2) { }
EXPORT void f6_V_SID_IFF(struct S_IFF p0, int p1, double p2) { }
EXPORT void f6_V_SID_IFD(struct S_IFD p0, int p1, double p2) { }
EXPORT void f6_V_SID_IFP(struct S_IFP p0, int p1, double p2) { }
EXPORT void f6_V_SID_IDI(struct S_IDI p0, int p1, double p2) { }
EXPORT void f6_V_SID_IDF(struct S_IDF p0, int p1, double p2) { }
EXPORT void f6_V_SID_IDD(struct S_IDD p0, int p1, double p2) { }
EXPORT void f6_V_SID_IDP(struct S_IDP p0, int p1, double p2) { }
EXPORT void f6_V_SID_IPI(struct S_IPI p0, int p1, double p2) { }
EXPORT void f6_V_SID_IPF(struct S_IPF p0, int p1, double p2) { }
EXPORT void f6_V_SID_IPD(struct S_IPD p0, int p1, double p2) { }
EXPORT void f6_V_SID_IPP(struct S_IPP p0, int p1, double p2) { }
EXPORT void f6_V_SID_FII(struct S_FII p0, int p1, double p2) { }
EXPORT void f6_V_SID_FIF(struct S_FIF p0, int p1, double p2) { }
EXPORT void f6_V_SID_FID(struct S_FID p0, int p1, double p2) { }
EXPORT void f6_V_SID_FIP(struct S_FIP p0, int p1, double p2) { }
EXPORT void f6_V_SID_FFI(struct S_FFI p0, int p1, double p2) { }
EXPORT void f6_V_SID_FFF(struct S_FFF p0, int p1, double p2) { }
EXPORT void f6_V_SID_FFD(struct S_FFD p0, int p1, double p2) { }
EXPORT void f6_V_SID_FFP(struct S_FFP p0, int p1, double p2) { }
EXPORT void f6_V_SID_FDI(struct S_FDI p0, int p1, double p2) { }
EXPORT void f6_V_SID_FDF(struct S_FDF p0, int p1, double p2) { }
EXPORT void f6_V_SID_FDD(struct S_FDD p0, int p1, double p2) { }
EXPORT void f6_V_SID_FDP(struct S_FDP p0, int p1, double p2) { }
EXPORT void f6_V_SID_FPI(struct S_FPI p0, int p1, double p2) { }
EXPORT void f6_V_SID_FPF(struct S_FPF p0, int p1, double p2) { }
EXPORT void f6_V_SID_FPD(struct S_FPD p0, int p1, double p2) { }
EXPORT void f6_V_SID_FPP(struct S_FPP p0, int p1, double p2) { }
EXPORT void f6_V_SID_DII(struct S_DII p0, int p1, double p2) { }
EXPORT void f6_V_SID_DIF(struct S_DIF p0, int p1, double p2) { }
EXPORT void f6_V_SID_DID(struct S_DID p0, int p1, double p2) { }
EXPORT void f6_V_SID_DIP(struct S_DIP p0, int p1, double p2) { }
EXPORT void f6_V_SID_DFI(struct S_DFI p0, int p1, double p2) { }
EXPORT void f6_V_SID_DFF(struct S_DFF p0, int p1, double p2) { }
EXPORT void f6_V_SID_DFD(struct S_DFD p0, int p1, double p2) { }
EXPORT void f6_V_SID_DFP(struct S_DFP p0, int p1, double p2) { }
EXPORT void f6_V_SID_DDI(struct S_DDI p0, int p1, double p2) { }
EXPORT void f6_V_SID_DDF(struct S_DDF p0, int p1, double p2) { }
EXPORT void f6_V_SID_DDD(struct S_DDD p0, int p1, double p2) { }
EXPORT void f6_V_SID_DDP(struct S_DDP p0, int p1, double p2) { }
EXPORT void f6_V_SID_DPI(struct S_DPI p0, int p1, double p2) { }
EXPORT void f6_V_SID_DPF(struct S_DPF p0, int p1, double p2) { }
EXPORT void f6_V_SID_DPD(struct S_DPD p0, int p1, double p2) { }
EXPORT void f6_V_SID_DPP(struct S_DPP p0, int p1, double p2) { }
EXPORT void f6_V_SID_PII(struct S_PII p0, int p1, double p2) { }
EXPORT void f6_V_SID_PIF(struct S_PIF p0, int p1, double p2) { }
EXPORT void f6_V_SID_PID(struct S_PID p0, int p1, double p2) { }
EXPORT void f6_V_SID_PIP(struct S_PIP p0, int p1, double p2) { }
EXPORT void f6_V_SID_PFI(struct S_PFI p0, int p1, double p2) { }
EXPORT void f6_V_SID_PFF(struct S_PFF p0, int p1, double p2) { }
EXPORT void f6_V_SID_PFD(struct S_PFD p0, int p1, double p2) { }
EXPORT void f6_V_SID_PFP(struct S_PFP p0, int p1, double p2) { }
EXPORT void f6_V_SID_PDI(struct S_PDI p0, int p1, double p2) { }
EXPORT void f6_V_SID_PDF(struct S_PDF p0, int p1, double p2) { }
EXPORT void f6_V_SID_PDD(struct S_PDD p0, int p1, double p2) { }
EXPORT void f6_V_SID_PDP(struct S_PDP p0, int p1, double p2) { }
EXPORT void f6_V_SID_PPI(struct S_PPI p0, int p1, double p2) { }
EXPORT void f6_V_SID_PPF(struct S_PPF p0, int p1, double p2) { }
EXPORT void f6_V_SID_PPD(struct S_PPD p0, int p1, double p2) { }
EXPORT void f7_V_SID_PPP(struct S_PPP p0, int p1, double p2) { }
EXPORT void f7_V_SIP_I(struct S_I p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_F(struct S_F p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_D(struct S_D p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_P(struct S_P p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_II(struct S_II p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_IF(struct S_IF p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_ID(struct S_ID p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_IP(struct S_IP p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_FI(struct S_FI p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_FF(struct S_FF p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_FD(struct S_FD p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_FP(struct S_FP p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_DI(struct S_DI p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_DF(struct S_DF p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_DD(struct S_DD p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_DP(struct S_DP p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_PI(struct S_PI p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_PF(struct S_PF p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_PD(struct S_PD p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_PP(struct S_PP p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_III(struct S_III p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_IIF(struct S_IIF p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_IID(struct S_IID p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_IIP(struct S_IIP p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_IFI(struct S_IFI p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_IFF(struct S_IFF p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_IFD(struct S_IFD p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_IFP(struct S_IFP p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_IDI(struct S_IDI p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_IDF(struct S_IDF p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_IDD(struct S_IDD p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_IDP(struct S_IDP p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_IPI(struct S_IPI p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_IPF(struct S_IPF p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_IPD(struct S_IPD p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_IPP(struct S_IPP p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_FII(struct S_FII p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_FIF(struct S_FIF p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_FID(struct S_FID p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_FIP(struct S_FIP p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_FFI(struct S_FFI p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_FFF(struct S_FFF p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_FFD(struct S_FFD p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_FFP(struct S_FFP p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_FDI(struct S_FDI p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_FDF(struct S_FDF p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_FDD(struct S_FDD p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_FDP(struct S_FDP p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_FPI(struct S_FPI p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_FPF(struct S_FPF p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_FPD(struct S_FPD p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_FPP(struct S_FPP p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_DII(struct S_DII p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_DIF(struct S_DIF p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_DID(struct S_DID p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_DIP(struct S_DIP p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_DFI(struct S_DFI p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_DFF(struct S_DFF p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_DFD(struct S_DFD p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_DFP(struct S_DFP p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_DDI(struct S_DDI p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_DDF(struct S_DDF p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_DDD(struct S_DDD p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_DDP(struct S_DDP p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_DPI(struct S_DPI p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_DPF(struct S_DPF p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_DPD(struct S_DPD p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_DPP(struct S_DPP p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_PII(struct S_PII p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_PIF(struct S_PIF p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_PID(struct S_PID p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_PIP(struct S_PIP p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_PFI(struct S_PFI p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_PFF(struct S_PFF p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_PFD(struct S_PFD p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_PFP(struct S_PFP p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_PDI(struct S_PDI p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_PDF(struct S_PDF p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_PDD(struct S_PDD p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_PDP(struct S_PDP p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_PPI(struct S_PPI p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_PPF(struct S_PPF p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_PPD(struct S_PPD p0, int p1, void* p2) { }
EXPORT void f7_V_SIP_PPP(struct S_PPP p0, int p1, void* p2) { }
EXPORT void f7_V_SIS_I(struct S_I p0, int p1, struct S_I p2) { }
EXPORT void f7_V_SIS_F(struct S_F p0, int p1, struct S_F p2) { }
EXPORT void f7_V_SIS_D(struct S_D p0, int p1, struct S_D p2) { }
EXPORT void f7_V_SIS_P(struct S_P p0, int p1, struct S_P p2) { }
EXPORT void f7_V_SIS_II(struct S_II p0, int p1, struct S_II p2) { }
EXPORT void f7_V_SIS_IF(struct S_IF p0, int p1, struct S_IF p2) { }
EXPORT void f7_V_SIS_ID(struct S_ID p0, int p1, struct S_ID p2) { }
EXPORT void f7_V_SIS_IP(struct S_IP p0, int p1, struct S_IP p2) { }
EXPORT void f7_V_SIS_FI(struct S_FI p0, int p1, struct S_FI p2) { }
EXPORT void f7_V_SIS_FF(struct S_FF p0, int p1, struct S_FF p2) { }
EXPORT void f7_V_SIS_FD(struct S_FD p0, int p1, struct S_FD p2) { }
EXPORT void f7_V_SIS_FP(struct S_FP p0, int p1, struct S_FP p2) { }
EXPORT void f7_V_SIS_DI(struct S_DI p0, int p1, struct S_DI p2) { }
EXPORT void f7_V_SIS_DF(struct S_DF p0, int p1, struct S_DF p2) { }
EXPORT void f7_V_SIS_DD(struct S_DD p0, int p1, struct S_DD p2) { }
EXPORT void f7_V_SIS_DP(struct S_DP p0, int p1, struct S_DP p2) { }
EXPORT void f7_V_SIS_PI(struct S_PI p0, int p1, struct S_PI p2) { }
EXPORT void f7_V_SIS_PF(struct S_PF p0, int p1, struct S_PF p2) { }
EXPORT void f7_V_SIS_PD(struct S_PD p0, int p1, struct S_PD p2) { }
EXPORT void f7_V_SIS_PP(struct S_PP p0, int p1, struct S_PP p2) { }
EXPORT void f7_V_SIS_III(struct S_III p0, int p1, struct S_III p2) { }
EXPORT void f7_V_SIS_IIF(struct S_IIF p0, int p1, struct S_IIF p2) { }
EXPORT void f7_V_SIS_IID(struct S_IID p0, int p1, struct S_IID p2) { }
EXPORT void f7_V_SIS_IIP(struct S_IIP p0, int p1, struct S_IIP p2) { }
EXPORT void f7_V_SIS_IFI(struct S_IFI p0, int p1, struct S_IFI p2) { }
EXPORT void f7_V_SIS_IFF(struct S_IFF p0, int p1, struct S_IFF p2) { }
EXPORT void f7_V_SIS_IFD(struct S_IFD p0, int p1, struct S_IFD p2) { }
EXPORT void f7_V_SIS_IFP(struct S_IFP p0, int p1, struct S_IFP p2) { }
EXPORT void f7_V_SIS_IDI(struct S_IDI p0, int p1, struct S_IDI p2) { }
EXPORT void f7_V_SIS_IDF(struct S_IDF p0, int p1, struct S_IDF p2) { }
EXPORT void f7_V_SIS_IDD(struct S_IDD p0, int p1, struct S_IDD p2) { }
EXPORT void f7_V_SIS_IDP(struct S_IDP p0, int p1, struct S_IDP p2) { }
EXPORT void f7_V_SIS_IPI(struct S_IPI p0, int p1, struct S_IPI p2) { }
EXPORT void f7_V_SIS_IPF(struct S_IPF p0, int p1, struct S_IPF p2) { }
EXPORT void f7_V_SIS_IPD(struct S_IPD p0, int p1, struct S_IPD p2) { }
EXPORT void f7_V_SIS_IPP(struct S_IPP p0, int p1, struct S_IPP p2) { }
EXPORT void f7_V_SIS_FII(struct S_FII p0, int p1, struct S_FII p2) { }
EXPORT void f7_V_SIS_FIF(struct S_FIF p0, int p1, struct S_FIF p2) { }
EXPORT void f7_V_SIS_FID(struct S_FID p0, int p1, struct S_FID p2) { }
EXPORT void f7_V_SIS_FIP(struct S_FIP p0, int p1, struct S_FIP p2) { }
EXPORT void f7_V_SIS_FFI(struct S_FFI p0, int p1, struct S_FFI p2) { }
EXPORT void f7_V_SIS_FFF(struct S_FFF p0, int p1, struct S_FFF p2) { }
EXPORT void f7_V_SIS_FFD(struct S_FFD p0, int p1, struct S_FFD p2) { }
EXPORT void f7_V_SIS_FFP(struct S_FFP p0, int p1, struct S_FFP p2) { }
EXPORT void f7_V_SIS_FDI(struct S_FDI p0, int p1, struct S_FDI p2) { }
EXPORT void f7_V_SIS_FDF(struct S_FDF p0, int p1, struct S_FDF p2) { }
EXPORT void f7_V_SIS_FDD(struct S_FDD p0, int p1, struct S_FDD p2) { }
EXPORT void f7_V_SIS_FDP(struct S_FDP p0, int p1, struct S_FDP p2) { }
EXPORT void f7_V_SIS_FPI(struct S_FPI p0, int p1, struct S_FPI p2) { }
EXPORT void f7_V_SIS_FPF(struct S_FPF p0, int p1, struct S_FPF p2) { }
EXPORT void f7_V_SIS_FPD(struct S_FPD p0, int p1, struct S_FPD p2) { }
EXPORT void f7_V_SIS_FPP(struct S_FPP p0, int p1, struct S_FPP p2) { }
EXPORT void f7_V_SIS_DII(struct S_DII p0, int p1, struct S_DII p2) { }
EXPORT void f7_V_SIS_DIF(struct S_DIF p0, int p1, struct S_DIF p2) { }
EXPORT void f7_V_SIS_DID(struct S_DID p0, int p1, struct S_DID p2) { }
EXPORT void f7_V_SIS_DIP(struct S_DIP p0, int p1, struct S_DIP p2) { }
EXPORT void f7_V_SIS_DFI(struct S_DFI p0, int p1, struct S_DFI p2) { }
EXPORT void f7_V_SIS_DFF(struct S_DFF p0, int p1, struct S_DFF p2) { }
EXPORT void f7_V_SIS_DFD(struct S_DFD p0, int p1, struct S_DFD p2) { }
EXPORT void f7_V_SIS_DFP(struct S_DFP p0, int p1, struct S_DFP p2) { }
EXPORT void f7_V_SIS_DDI(struct S_DDI p0, int p1, struct S_DDI p2) { }
EXPORT void f7_V_SIS_DDF(struct S_DDF p0, int p1, struct S_DDF p2) { }
EXPORT void f7_V_SIS_DDD(struct S_DDD p0, int p1, struct S_DDD p2) { }
EXPORT void f7_V_SIS_DDP(struct S_DDP p0, int p1, struct S_DDP p2) { }
EXPORT void f7_V_SIS_DPI(struct S_DPI p0, int p1, struct S_DPI p2) { }
EXPORT void f7_V_SIS_DPF(struct S_DPF p0, int p1, struct S_DPF p2) { }
EXPORT void f7_V_SIS_DPD(struct S_DPD p0, int p1, struct S_DPD p2) { }
EXPORT void f7_V_SIS_DPP(struct S_DPP p0, int p1, struct S_DPP p2) { }
EXPORT void f7_V_SIS_PII(struct S_PII p0, int p1, struct S_PII p2) { }
EXPORT void f7_V_SIS_PIF(struct S_PIF p0, int p1, struct S_PIF p2) { }
EXPORT void f7_V_SIS_PID(struct S_PID p0, int p1, struct S_PID p2) { }
EXPORT void f7_V_SIS_PIP(struct S_PIP p0, int p1, struct S_PIP p2) { }
EXPORT void f7_V_SIS_PFI(struct S_PFI p0, int p1, struct S_PFI p2) { }
EXPORT void f7_V_SIS_PFF(struct S_PFF p0, int p1, struct S_PFF p2) { }
EXPORT void f7_V_SIS_PFD(struct S_PFD p0, int p1, struct S_PFD p2) { }
EXPORT void f7_V_SIS_PFP(struct S_PFP p0, int p1, struct S_PFP p2) { }
EXPORT void f7_V_SIS_PDI(struct S_PDI p0, int p1, struct S_PDI p2) { }
EXPORT void f7_V_SIS_PDF(struct S_PDF p0, int p1, struct S_PDF p2) { }
EXPORT void f7_V_SIS_PDD(struct S_PDD p0, int p1, struct S_PDD p2) { }
EXPORT void f7_V_SIS_PDP(struct S_PDP p0, int p1, struct S_PDP p2) { }
EXPORT void f7_V_SIS_PPI(struct S_PPI p0, int p1, struct S_PPI p2) { }
EXPORT void f7_V_SIS_PPF(struct S_PPF p0, int p1, struct S_PPF p2) { }
EXPORT void f7_V_SIS_PPD(struct S_PPD p0, int p1, struct S_PPD p2) { }
EXPORT void f7_V_SIS_PPP(struct S_PPP p0, int p1, struct S_PPP p2) { }
EXPORT void f7_V_SFI_I(struct S_I p0, float p1, int p2) { }
EXPORT void f7_V_SFI_F(struct S_F p0, float p1, int p2) { }
EXPORT void f7_V_SFI_D(struct S_D p0, float p1, int p2) { }
EXPORT void f7_V_SFI_P(struct S_P p0, float p1, int p2) { }
EXPORT void f7_V_SFI_II(struct S_II p0, float p1, int p2) { }
EXPORT void f7_V_SFI_IF(struct S_IF p0, float p1, int p2) { }
EXPORT void f7_V_SFI_ID(struct S_ID p0, float p1, int p2) { }
EXPORT void f7_V_SFI_IP(struct S_IP p0, float p1, int p2) { }
EXPORT void f7_V_SFI_FI(struct S_FI p0, float p1, int p2) { }
EXPORT void f7_V_SFI_FF(struct S_FF p0, float p1, int p2) { }
EXPORT void f7_V_SFI_FD(struct S_FD p0, float p1, int p2) { }
EXPORT void f7_V_SFI_FP(struct S_FP p0, float p1, int p2) { }
EXPORT void f7_V_SFI_DI(struct S_DI p0, float p1, int p2) { }
EXPORT void f7_V_SFI_DF(struct S_DF p0, float p1, int p2) { }
EXPORT void f7_V_SFI_DD(struct S_DD p0, float p1, int p2) { }
EXPORT void f7_V_SFI_DP(struct S_DP p0, float p1, int p2) { }
EXPORT void f7_V_SFI_PI(struct S_PI p0, float p1, int p2) { }
EXPORT void f7_V_SFI_PF(struct S_PF p0, float p1, int p2) { }
EXPORT void f7_V_SFI_PD(struct S_PD p0, float p1, int p2) { }
EXPORT void f7_V_SFI_PP(struct S_PP p0, float p1, int p2) { }
EXPORT void f7_V_SFI_III(struct S_III p0, float p1, int p2) { }
EXPORT void f7_V_SFI_IIF(struct S_IIF p0, float p1, int p2) { }
EXPORT void f7_V_SFI_IID(struct S_IID p0, float p1, int p2) { }
EXPORT void f7_V_SFI_IIP(struct S_IIP p0, float p1, int p2) { }
EXPORT void f7_V_SFI_IFI(struct S_IFI p0, float p1, int p2) { }
EXPORT void f7_V_SFI_IFF(struct S_IFF p0, float p1, int p2) { }
EXPORT void f7_V_SFI_IFD(struct S_IFD p0, float p1, int p2) { }
EXPORT void f7_V_SFI_IFP(struct S_IFP p0, float p1, int p2) { }
EXPORT void f7_V_SFI_IDI(struct S_IDI p0, float p1, int p2) { }
EXPORT void f7_V_SFI_IDF(struct S_IDF p0, float p1, int p2) { }
EXPORT void f7_V_SFI_IDD(struct S_IDD p0, float p1, int p2) { }
EXPORT void f7_V_SFI_IDP(struct S_IDP p0, float p1, int p2) { }
EXPORT void f7_V_SFI_IPI(struct S_IPI p0, float p1, int p2) { }
EXPORT void f7_V_SFI_IPF(struct S_IPF p0, float p1, int p2) { }
EXPORT void f7_V_SFI_IPD(struct S_IPD p0, float p1, int p2) { }
EXPORT void f7_V_SFI_IPP(struct S_IPP p0, float p1, int p2) { }
EXPORT void f7_V_SFI_FII(struct S_FII p0, float p1, int p2) { }
EXPORT void f7_V_SFI_FIF(struct S_FIF p0, float p1, int p2) { }
EXPORT void f7_V_SFI_FID(struct S_FID p0, float p1, int p2) { }
EXPORT void f7_V_SFI_FIP(struct S_FIP p0, float p1, int p2) { }
EXPORT void f7_V_SFI_FFI(struct S_FFI p0, float p1, int p2) { }
EXPORT void f7_V_SFI_FFF(struct S_FFF p0, float p1, int p2) { }
EXPORT void f7_V_SFI_FFD(struct S_FFD p0, float p1, int p2) { }
EXPORT void f7_V_SFI_FFP(struct S_FFP p0, float p1, int p2) { }
EXPORT void f7_V_SFI_FDI(struct S_FDI p0, float p1, int p2) { }
EXPORT void f7_V_SFI_FDF(struct S_FDF p0, float p1, int p2) { }
EXPORT void f7_V_SFI_FDD(struct S_FDD p0, float p1, int p2) { }
EXPORT void f7_V_SFI_FDP(struct S_FDP p0, float p1, int p2) { }
EXPORT void f7_V_SFI_FPI(struct S_FPI p0, float p1, int p2) { }
EXPORT void f7_V_SFI_FPF(struct S_FPF p0, float p1, int p2) { }
EXPORT void f7_V_SFI_FPD(struct S_FPD p0, float p1, int p2) { }
EXPORT void f7_V_SFI_FPP(struct S_FPP p0, float p1, int p2) { }
EXPORT void f7_V_SFI_DII(struct S_DII p0, float p1, int p2) { }
EXPORT void f7_V_SFI_DIF(struct S_DIF p0, float p1, int p2) { }
EXPORT void f7_V_SFI_DID(struct S_DID p0, float p1, int p2) { }
EXPORT void f7_V_SFI_DIP(struct S_DIP p0, float p1, int p2) { }
EXPORT void f7_V_SFI_DFI(struct S_DFI p0, float p1, int p2) { }
EXPORT void f7_V_SFI_DFF(struct S_DFF p0, float p1, int p2) { }
EXPORT void f7_V_SFI_DFD(struct S_DFD p0, float p1, int p2) { }
EXPORT void f7_V_SFI_DFP(struct S_DFP p0, float p1, int p2) { }
EXPORT void f7_V_SFI_DDI(struct S_DDI p0, float p1, int p2) { }
EXPORT void f7_V_SFI_DDF(struct S_DDF p0, float p1, int p2) { }
EXPORT void f7_V_SFI_DDD(struct S_DDD p0, float p1, int p2) { }
EXPORT void f7_V_SFI_DDP(struct S_DDP p0, float p1, int p2) { }
EXPORT void f7_V_SFI_DPI(struct S_DPI p0, float p1, int p2) { }
EXPORT void f7_V_SFI_DPF(struct S_DPF p0, float p1, int p2) { }
EXPORT void f7_V_SFI_DPD(struct S_DPD p0, float p1, int p2) { }
EXPORT void f7_V_SFI_DPP(struct S_DPP p0, float p1, int p2) { }
EXPORT void f7_V_SFI_PII(struct S_PII p0, float p1, int p2) { }
EXPORT void f7_V_SFI_PIF(struct S_PIF p0, float p1, int p2) { }
EXPORT void f7_V_SFI_PID(struct S_PID p0, float p1, int p2) { }
EXPORT void f7_V_SFI_PIP(struct S_PIP p0, float p1, int p2) { }
EXPORT void f7_V_SFI_PFI(struct S_PFI p0, float p1, int p2) { }
EXPORT void f7_V_SFI_PFF(struct S_PFF p0, float p1, int p2) { }
EXPORT void f7_V_SFI_PFD(struct S_PFD p0, float p1, int p2) { }
EXPORT void f7_V_SFI_PFP(struct S_PFP p0, float p1, int p2) { }
EXPORT void f7_V_SFI_PDI(struct S_PDI p0, float p1, int p2) { }
EXPORT void f7_V_SFI_PDF(struct S_PDF p0, float p1, int p2) { }
EXPORT void f7_V_SFI_PDD(struct S_PDD p0, float p1, int p2) { }
EXPORT void f7_V_SFI_PDP(struct S_PDP p0, float p1, int p2) { }
EXPORT void f7_V_SFI_PPI(struct S_PPI p0, float p1, int p2) { }
EXPORT void f7_V_SFI_PPF(struct S_PPF p0, float p1, int p2) { }
EXPORT void f7_V_SFI_PPD(struct S_PPD p0, float p1, int p2) { }
EXPORT void f7_V_SFI_PPP(struct S_PPP p0, float p1, int p2) { }
EXPORT void f7_V_SFF_I(struct S_I p0, float p1, float p2) { }
EXPORT void f7_V_SFF_F(struct S_F p0, float p1, float p2) { }
EXPORT void f7_V_SFF_D(struct S_D p0, float p1, float p2) { }
EXPORT void f7_V_SFF_P(struct S_P p0, float p1, float p2) { }
EXPORT void f7_V_SFF_II(struct S_II p0, float p1, float p2) { }
EXPORT void f7_V_SFF_IF(struct S_IF p0, float p1, float p2) { }
EXPORT void f7_V_SFF_ID(struct S_ID p0, float p1, float p2) { }
EXPORT void f7_V_SFF_IP(struct S_IP p0, float p1, float p2) { }
EXPORT void f7_V_SFF_FI(struct S_FI p0, float p1, float p2) { }
EXPORT void f7_V_SFF_FF(struct S_FF p0, float p1, float p2) { }
EXPORT void f7_V_SFF_FD(struct S_FD p0, float p1, float p2) { }
EXPORT void f7_V_SFF_FP(struct S_FP p0, float p1, float p2) { }
EXPORT void f7_V_SFF_DI(struct S_DI p0, float p1, float p2) { }
EXPORT void f7_V_SFF_DF(struct S_DF p0, float p1, float p2) { }
EXPORT void f7_V_SFF_DD(struct S_DD p0, float p1, float p2) { }
EXPORT void f7_V_SFF_DP(struct S_DP p0, float p1, float p2) { }
EXPORT void f7_V_SFF_PI(struct S_PI p0, float p1, float p2) { }
EXPORT void f7_V_SFF_PF(struct S_PF p0, float p1, float p2) { }
EXPORT void f7_V_SFF_PD(struct S_PD p0, float p1, float p2) { }
EXPORT void f7_V_SFF_PP(struct S_PP p0, float p1, float p2) { }
EXPORT void f7_V_SFF_III(struct S_III p0, float p1, float p2) { }
EXPORT void f7_V_SFF_IIF(struct S_IIF p0, float p1, float p2) { }
EXPORT void f7_V_SFF_IID(struct S_IID p0, float p1, float p2) { }
EXPORT void f7_V_SFF_IIP(struct S_IIP p0, float p1, float p2) { }
EXPORT void f7_V_SFF_IFI(struct S_IFI p0, float p1, float p2) { }
EXPORT void f7_V_SFF_IFF(struct S_IFF p0, float p1, float p2) { }
EXPORT void f7_V_SFF_IFD(struct S_IFD p0, float p1, float p2) { }
EXPORT void f7_V_SFF_IFP(struct S_IFP p0, float p1, float p2) { }
EXPORT void f7_V_SFF_IDI(struct S_IDI p0, float p1, float p2) { }
EXPORT void f7_V_SFF_IDF(struct S_IDF p0, float p1, float p2) { }
EXPORT void f7_V_SFF_IDD(struct S_IDD p0, float p1, float p2) { }
EXPORT void f7_V_SFF_IDP(struct S_IDP p0, float p1, float p2) { }
EXPORT void f7_V_SFF_IPI(struct S_IPI p0, float p1, float p2) { }
EXPORT void f7_V_SFF_IPF(struct S_IPF p0, float p1, float p2) { }
EXPORT void f7_V_SFF_IPD(struct S_IPD p0, float p1, float p2) { }
EXPORT void f7_V_SFF_IPP(struct S_IPP p0, float p1, float p2) { }
EXPORT void f7_V_SFF_FII(struct S_FII p0, float p1, float p2) { }
EXPORT void f7_V_SFF_FIF(struct S_FIF p0, float p1, float p2) { }
EXPORT void f7_V_SFF_FID(struct S_FID p0, float p1, float p2) { }
EXPORT void f7_V_SFF_FIP(struct S_FIP p0, float p1, float p2) { }
EXPORT void f7_V_SFF_FFI(struct S_FFI p0, float p1, float p2) { }
EXPORT void f7_V_SFF_FFF(struct S_FFF p0, float p1, float p2) { }
EXPORT void f7_V_SFF_FFD(struct S_FFD p0, float p1, float p2) { }
EXPORT void f7_V_SFF_FFP(struct S_FFP p0, float p1, float p2) { }
EXPORT void f7_V_SFF_FDI(struct S_FDI p0, float p1, float p2) { }
EXPORT void f7_V_SFF_FDF(struct S_FDF p0, float p1, float p2) { }
EXPORT void f7_V_SFF_FDD(struct S_FDD p0, float p1, float p2) { }
EXPORT void f7_V_SFF_FDP(struct S_FDP p0, float p1, float p2) { }
EXPORT void f7_V_SFF_FPI(struct S_FPI p0, float p1, float p2) { }
EXPORT void f7_V_SFF_FPF(struct S_FPF p0, float p1, float p2) { }
EXPORT void f7_V_SFF_FPD(struct S_FPD p0, float p1, float p2) { }
EXPORT void f7_V_SFF_FPP(struct S_FPP p0, float p1, float p2) { }
EXPORT void f7_V_SFF_DII(struct S_DII p0, float p1, float p2) { }
EXPORT void f7_V_SFF_DIF(struct S_DIF p0, float p1, float p2) { }
EXPORT void f7_V_SFF_DID(struct S_DID p0, float p1, float p2) { }
EXPORT void f7_V_SFF_DIP(struct S_DIP p0, float p1, float p2) { }
EXPORT void f7_V_SFF_DFI(struct S_DFI p0, float p1, float p2) { }
EXPORT void f7_V_SFF_DFF(struct S_DFF p0, float p1, float p2) { }
EXPORT void f7_V_SFF_DFD(struct S_DFD p0, float p1, float p2) { }
EXPORT void f7_V_SFF_DFP(struct S_DFP p0, float p1, float p2) { }
EXPORT void f7_V_SFF_DDI(struct S_DDI p0, float p1, float p2) { }
EXPORT void f7_V_SFF_DDF(struct S_DDF p0, float p1, float p2) { }
EXPORT void f7_V_SFF_DDD(struct S_DDD p0, float p1, float p2) { }
EXPORT void f7_V_SFF_DDP(struct S_DDP p0, float p1, float p2) { }
EXPORT void f7_V_SFF_DPI(struct S_DPI p0, float p1, float p2) { }
EXPORT void f7_V_SFF_DPF(struct S_DPF p0, float p1, float p2) { }
EXPORT void f7_V_SFF_DPD(struct S_DPD p0, float p1, float p2) { }
EXPORT void f7_V_SFF_DPP(struct S_DPP p0, float p1, float p2) { }
EXPORT void f7_V_SFF_PII(struct S_PII p0, float p1, float p2) { }
EXPORT void f7_V_SFF_PIF(struct S_PIF p0, float p1, float p2) { }
EXPORT void f7_V_SFF_PID(struct S_PID p0, float p1, float p2) { }
EXPORT void f7_V_SFF_PIP(struct S_PIP p0, float p1, float p2) { }
EXPORT void f7_V_SFF_PFI(struct S_PFI p0, float p1, float p2) { }
EXPORT void f7_V_SFF_PFF(struct S_PFF p0, float p1, float p2) { }
EXPORT void f7_V_SFF_PFD(struct S_PFD p0, float p1, float p2) { }
EXPORT void f7_V_SFF_PFP(struct S_PFP p0, float p1, float p2) { }
EXPORT void f7_V_SFF_PDI(struct S_PDI p0, float p1, float p2) { }
EXPORT void f7_V_SFF_PDF(struct S_PDF p0, float p1, float p2) { }
EXPORT void f7_V_SFF_PDD(struct S_PDD p0, float p1, float p2) { }
EXPORT void f7_V_SFF_PDP(struct S_PDP p0, float p1, float p2) { }
EXPORT void f7_V_SFF_PPI(struct S_PPI p0, float p1, float p2) { }
EXPORT void f7_V_SFF_PPF(struct S_PPF p0, float p1, float p2) { }
EXPORT void f7_V_SFF_PPD(struct S_PPD p0, float p1, float p2) { }
EXPORT void f7_V_SFF_PPP(struct S_PPP p0, float p1, float p2) { }
EXPORT void f7_V_SFD_I(struct S_I p0, float p1, double p2) { }
EXPORT void f7_V_SFD_F(struct S_F p0, float p1, double p2) { }
EXPORT void f7_V_SFD_D(struct S_D p0, float p1, double p2) { }
EXPORT void f7_V_SFD_P(struct S_P p0, float p1, double p2) { }
EXPORT void f7_V_SFD_II(struct S_II p0, float p1, double p2) { }
EXPORT void f7_V_SFD_IF(struct S_IF p0, float p1, double p2) { }
EXPORT void f7_V_SFD_ID(struct S_ID p0, float p1, double p2) { }
EXPORT void f7_V_SFD_IP(struct S_IP p0, float p1, double p2) { }
EXPORT void f7_V_SFD_FI(struct S_FI p0, float p1, double p2) { }
EXPORT void f7_V_SFD_FF(struct S_FF p0, float p1, double p2) { }
EXPORT void f7_V_SFD_FD(struct S_FD p0, float p1, double p2) { }
EXPORT void f7_V_SFD_FP(struct S_FP p0, float p1, double p2) { }
EXPORT void f7_V_SFD_DI(struct S_DI p0, float p1, double p2) { }
EXPORT void f7_V_SFD_DF(struct S_DF p0, float p1, double p2) { }
EXPORT void f7_V_SFD_DD(struct S_DD p0, float p1, double p2) { }
EXPORT void f7_V_SFD_DP(struct S_DP p0, float p1, double p2) { }
EXPORT void f7_V_SFD_PI(struct S_PI p0, float p1, double p2) { }
EXPORT void f7_V_SFD_PF(struct S_PF p0, float p1, double p2) { }
EXPORT void f7_V_SFD_PD(struct S_PD p0, float p1, double p2) { }
EXPORT void f7_V_SFD_PP(struct S_PP p0, float p1, double p2) { }
EXPORT void f7_V_SFD_III(struct S_III p0, float p1, double p2) { }
EXPORT void f7_V_SFD_IIF(struct S_IIF p0, float p1, double p2) { }
EXPORT void f7_V_SFD_IID(struct S_IID p0, float p1, double p2) { }
EXPORT void f7_V_SFD_IIP(struct S_IIP p0, float p1, double p2) { }
EXPORT void f7_V_SFD_IFI(struct S_IFI p0, float p1, double p2) { }
EXPORT void f7_V_SFD_IFF(struct S_IFF p0, float p1, double p2) { }
EXPORT void f7_V_SFD_IFD(struct S_IFD p0, float p1, double p2) { }
EXPORT void f7_V_SFD_IFP(struct S_IFP p0, float p1, double p2) { }
EXPORT void f7_V_SFD_IDI(struct S_IDI p0, float p1, double p2) { }
EXPORT void f7_V_SFD_IDF(struct S_IDF p0, float p1, double p2) { }
EXPORT void f7_V_SFD_IDD(struct S_IDD p0, float p1, double p2) { }
EXPORT void f7_V_SFD_IDP(struct S_IDP p0, float p1, double p2) { }
EXPORT void f7_V_SFD_IPI(struct S_IPI p0, float p1, double p2) { }
EXPORT void f7_V_SFD_IPF(struct S_IPF p0, float p1, double p2) { }
EXPORT void f7_V_SFD_IPD(struct S_IPD p0, float p1, double p2) { }
EXPORT void f7_V_SFD_IPP(struct S_IPP p0, float p1, double p2) { }
EXPORT void f7_V_SFD_FII(struct S_FII p0, float p1, double p2) { }
EXPORT void f7_V_SFD_FIF(struct S_FIF p0, float p1, double p2) { }
EXPORT void f7_V_SFD_FID(struct S_FID p0, float p1, double p2) { }
EXPORT void f7_V_SFD_FIP(struct S_FIP p0, float p1, double p2) { }
EXPORT void f7_V_SFD_FFI(struct S_FFI p0, float p1, double p2) { }
EXPORT void f7_V_SFD_FFF(struct S_FFF p0, float p1, double p2) { }
EXPORT void f7_V_SFD_FFD(struct S_FFD p0, float p1, double p2) { }
EXPORT void f7_V_SFD_FFP(struct S_FFP p0, float p1, double p2) { }
EXPORT void f7_V_SFD_FDI(struct S_FDI p0, float p1, double p2) { }
EXPORT void f7_V_SFD_FDF(struct S_FDF p0, float p1, double p2) { }
EXPORT void f7_V_SFD_FDD(struct S_FDD p0, float p1, double p2) { }
EXPORT void f7_V_SFD_FDP(struct S_FDP p0, float p1, double p2) { }
EXPORT void f7_V_SFD_FPI(struct S_FPI p0, float p1, double p2) { }
EXPORT void f7_V_SFD_FPF(struct S_FPF p0, float p1, double p2) { }
EXPORT void f7_V_SFD_FPD(struct S_FPD p0, float p1, double p2) { }
EXPORT void f7_V_SFD_FPP(struct S_FPP p0, float p1, double p2) { }
EXPORT void f7_V_SFD_DII(struct S_DII p0, float p1, double p2) { }
EXPORT void f7_V_SFD_DIF(struct S_DIF p0, float p1, double p2) { }
EXPORT void f7_V_SFD_DID(struct S_DID p0, float p1, double p2) { }
EXPORT void f7_V_SFD_DIP(struct S_DIP p0, float p1, double p2) { }
EXPORT void f7_V_SFD_DFI(struct S_DFI p0, float p1, double p2) { }
EXPORT void f7_V_SFD_DFF(struct S_DFF p0, float p1, double p2) { }
EXPORT void f7_V_SFD_DFD(struct S_DFD p0, float p1, double p2) { }
EXPORT void f7_V_SFD_DFP(struct S_DFP p0, float p1, double p2) { }
EXPORT void f7_V_SFD_DDI(struct S_DDI p0, float p1, double p2) { }
EXPORT void f7_V_SFD_DDF(struct S_DDF p0, float p1, double p2) { }
EXPORT void f7_V_SFD_DDD(struct S_DDD p0, float p1, double p2) { }
EXPORT void f7_V_SFD_DDP(struct S_DDP p0, float p1, double p2) { }
EXPORT void f7_V_SFD_DPI(struct S_DPI p0, float p1, double p2) { }
EXPORT void f7_V_SFD_DPF(struct S_DPF p0, float p1, double p2) { }
EXPORT void f7_V_SFD_DPD(struct S_DPD p0, float p1, double p2) { }
EXPORT void f7_V_SFD_DPP(struct S_DPP p0, float p1, double p2) { }
EXPORT void f7_V_SFD_PII(struct S_PII p0, float p1, double p2) { }
EXPORT void f7_V_SFD_PIF(struct S_PIF p0, float p1, double p2) { }
EXPORT void f7_V_SFD_PID(struct S_PID p0, float p1, double p2) { }
EXPORT void f7_V_SFD_PIP(struct S_PIP p0, float p1, double p2) { }
EXPORT void f7_V_SFD_PFI(struct S_PFI p0, float p1, double p2) { }
EXPORT void f7_V_SFD_PFF(struct S_PFF p0, float p1, double p2) { }
EXPORT void f7_V_SFD_PFD(struct S_PFD p0, float p1, double p2) { }
EXPORT void f7_V_SFD_PFP(struct S_PFP p0, float p1, double p2) { }
EXPORT void f7_V_SFD_PDI(struct S_PDI p0, float p1, double p2) { }
EXPORT void f7_V_SFD_PDF(struct S_PDF p0, float p1, double p2) { }
EXPORT void f7_V_SFD_PDD(struct S_PDD p0, float p1, double p2) { }
EXPORT void f7_V_SFD_PDP(struct S_PDP p0, float p1, double p2) { }
EXPORT void f7_V_SFD_PPI(struct S_PPI p0, float p1, double p2) { }
EXPORT void f7_V_SFD_PPF(struct S_PPF p0, float p1, double p2) { }
EXPORT void f7_V_SFD_PPD(struct S_PPD p0, float p1, double p2) { }
EXPORT void f7_V_SFD_PPP(struct S_PPP p0, float p1, double p2) { }
EXPORT void f7_V_SFP_I(struct S_I p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_F(struct S_F p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_D(struct S_D p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_P(struct S_P p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_II(struct S_II p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_IF(struct S_IF p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_ID(struct S_ID p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_IP(struct S_IP p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_FI(struct S_FI p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_FF(struct S_FF p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_FD(struct S_FD p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_FP(struct S_FP p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_DI(struct S_DI p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_DF(struct S_DF p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_DD(struct S_DD p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_DP(struct S_DP p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_PI(struct S_PI p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_PF(struct S_PF p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_PD(struct S_PD p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_PP(struct S_PP p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_III(struct S_III p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_IIF(struct S_IIF p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_IID(struct S_IID p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_IIP(struct S_IIP p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_IFI(struct S_IFI p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_IFF(struct S_IFF p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_IFD(struct S_IFD p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_IFP(struct S_IFP p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_IDI(struct S_IDI p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_IDF(struct S_IDF p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_IDD(struct S_IDD p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_IDP(struct S_IDP p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_IPI(struct S_IPI p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_IPF(struct S_IPF p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_IPD(struct S_IPD p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_IPP(struct S_IPP p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_FII(struct S_FII p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_FIF(struct S_FIF p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_FID(struct S_FID p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_FIP(struct S_FIP p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_FFI(struct S_FFI p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_FFF(struct S_FFF p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_FFD(struct S_FFD p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_FFP(struct S_FFP p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_FDI(struct S_FDI p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_FDF(struct S_FDF p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_FDD(struct S_FDD p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_FDP(struct S_FDP p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_FPI(struct S_FPI p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_FPF(struct S_FPF p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_FPD(struct S_FPD p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_FPP(struct S_FPP p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_DII(struct S_DII p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_DIF(struct S_DIF p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_DID(struct S_DID p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_DIP(struct S_DIP p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_DFI(struct S_DFI p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_DFF(struct S_DFF p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_DFD(struct S_DFD p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_DFP(struct S_DFP p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_DDI(struct S_DDI p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_DDF(struct S_DDF p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_DDD(struct S_DDD p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_DDP(struct S_DDP p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_DPI(struct S_DPI p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_DPF(struct S_DPF p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_DPD(struct S_DPD p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_DPP(struct S_DPP p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_PII(struct S_PII p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_PIF(struct S_PIF p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_PID(struct S_PID p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_PIP(struct S_PIP p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_PFI(struct S_PFI p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_PFF(struct S_PFF p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_PFD(struct S_PFD p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_PFP(struct S_PFP p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_PDI(struct S_PDI p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_PDF(struct S_PDF p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_PDD(struct S_PDD p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_PDP(struct S_PDP p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_PPI(struct S_PPI p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_PPF(struct S_PPF p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_PPD(struct S_PPD p0, float p1, void* p2) { }
EXPORT void f7_V_SFP_PPP(struct S_PPP p0, float p1, void* p2) { }
EXPORT void f7_V_SFS_I(struct S_I p0, float p1, struct S_I p2) { }
EXPORT void f7_V_SFS_F(struct S_F p0, float p1, struct S_F p2) { }
EXPORT void f7_V_SFS_D(struct S_D p0, float p1, struct S_D p2) { }
EXPORT void f7_V_SFS_P(struct S_P p0, float p1, struct S_P p2) { }
EXPORT void f7_V_SFS_II(struct S_II p0, float p1, struct S_II p2) { }
EXPORT void f7_V_SFS_IF(struct S_IF p0, float p1, struct S_IF p2) { }
EXPORT void f7_V_SFS_ID(struct S_ID p0, float p1, struct S_ID p2) { }
EXPORT void f7_V_SFS_IP(struct S_IP p0, float p1, struct S_IP p2) { }
EXPORT void f7_V_SFS_FI(struct S_FI p0, float p1, struct S_FI p2) { }
EXPORT void f7_V_SFS_FF(struct S_FF p0, float p1, struct S_FF p2) { }
EXPORT void f7_V_SFS_FD(struct S_FD p0, float p1, struct S_FD p2) { }
EXPORT void f7_V_SFS_FP(struct S_FP p0, float p1, struct S_FP p2) { }
EXPORT void f7_V_SFS_DI(struct S_DI p0, float p1, struct S_DI p2) { }
EXPORT void f7_V_SFS_DF(struct S_DF p0, float p1, struct S_DF p2) { }
EXPORT void f7_V_SFS_DD(struct S_DD p0, float p1, struct S_DD p2) { }
EXPORT void f7_V_SFS_DP(struct S_DP p0, float p1, struct S_DP p2) { }
EXPORT void f7_V_SFS_PI(struct S_PI p0, float p1, struct S_PI p2) { }
EXPORT void f7_V_SFS_PF(struct S_PF p0, float p1, struct S_PF p2) { }
EXPORT void f7_V_SFS_PD(struct S_PD p0, float p1, struct S_PD p2) { }
EXPORT void f7_V_SFS_PP(struct S_PP p0, float p1, struct S_PP p2) { }
EXPORT void f7_V_SFS_III(struct S_III p0, float p1, struct S_III p2) { }
EXPORT void f7_V_SFS_IIF(struct S_IIF p0, float p1, struct S_IIF p2) { }
EXPORT void f7_V_SFS_IID(struct S_IID p0, float p1, struct S_IID p2) { }
EXPORT void f7_V_SFS_IIP(struct S_IIP p0, float p1, struct S_IIP p2) { }
EXPORT void f7_V_SFS_IFI(struct S_IFI p0, float p1, struct S_IFI p2) { }
EXPORT void f7_V_SFS_IFF(struct S_IFF p0, float p1, struct S_IFF p2) { }
EXPORT void f7_V_SFS_IFD(struct S_IFD p0, float p1, struct S_IFD p2) { }
EXPORT void f7_V_SFS_IFP(struct S_IFP p0, float p1, struct S_IFP p2) { }
EXPORT void f7_V_SFS_IDI(struct S_IDI p0, float p1, struct S_IDI p2) { }
EXPORT void f7_V_SFS_IDF(struct S_IDF p0, float p1, struct S_IDF p2) { }
EXPORT void f7_V_SFS_IDD(struct S_IDD p0, float p1, struct S_IDD p2) { }
EXPORT void f7_V_SFS_IDP(struct S_IDP p0, float p1, struct S_IDP p2) { }
EXPORT void f7_V_SFS_IPI(struct S_IPI p0, float p1, struct S_IPI p2) { }
EXPORT void f7_V_SFS_IPF(struct S_IPF p0, float p1, struct S_IPF p2) { }
EXPORT void f7_V_SFS_IPD(struct S_IPD p0, float p1, struct S_IPD p2) { }
EXPORT void f7_V_SFS_IPP(struct S_IPP p0, float p1, struct S_IPP p2) { }
EXPORT void f7_V_SFS_FII(struct S_FII p0, float p1, struct S_FII p2) { }
EXPORT void f7_V_SFS_FIF(struct S_FIF p0, float p1, struct S_FIF p2) { }
EXPORT void f7_V_SFS_FID(struct S_FID p0, float p1, struct S_FID p2) { }
EXPORT void f7_V_SFS_FIP(struct S_FIP p0, float p1, struct S_FIP p2) { }
EXPORT void f7_V_SFS_FFI(struct S_FFI p0, float p1, struct S_FFI p2) { }
EXPORT void f7_V_SFS_FFF(struct S_FFF p0, float p1, struct S_FFF p2) { }
EXPORT void f7_V_SFS_FFD(struct S_FFD p0, float p1, struct S_FFD p2) { }
EXPORT void f7_V_SFS_FFP(struct S_FFP p0, float p1, struct S_FFP p2) { }
EXPORT void f7_V_SFS_FDI(struct S_FDI p0, float p1, struct S_FDI p2) { }
EXPORT void f7_V_SFS_FDF(struct S_FDF p0, float p1, struct S_FDF p2) { }
EXPORT void f7_V_SFS_FDD(struct S_FDD p0, float p1, struct S_FDD p2) { }
EXPORT void f7_V_SFS_FDP(struct S_FDP p0, float p1, struct S_FDP p2) { }
EXPORT void f7_V_SFS_FPI(struct S_FPI p0, float p1, struct S_FPI p2) { }
EXPORT void f7_V_SFS_FPF(struct S_FPF p0, float p1, struct S_FPF p2) { }
EXPORT void f7_V_SFS_FPD(struct S_FPD p0, float p1, struct S_FPD p2) { }
EXPORT void f7_V_SFS_FPP(struct S_FPP p0, float p1, struct S_FPP p2) { }
EXPORT void f7_V_SFS_DII(struct S_DII p0, float p1, struct S_DII p2) { }
EXPORT void f7_V_SFS_DIF(struct S_DIF p0, float p1, struct S_DIF p2) { }
EXPORT void f7_V_SFS_DID(struct S_DID p0, float p1, struct S_DID p2) { }
EXPORT void f7_V_SFS_DIP(struct S_DIP p0, float p1, struct S_DIP p2) { }
EXPORT void f7_V_SFS_DFI(struct S_DFI p0, float p1, struct S_DFI p2) { }
EXPORT void f7_V_SFS_DFF(struct S_DFF p0, float p1, struct S_DFF p2) { }
EXPORT void f7_V_SFS_DFD(struct S_DFD p0, float p1, struct S_DFD p2) { }
EXPORT void f7_V_SFS_DFP(struct S_DFP p0, float p1, struct S_DFP p2) { }
EXPORT void f7_V_SFS_DDI(struct S_DDI p0, float p1, struct S_DDI p2) { }
EXPORT void f7_V_SFS_DDF(struct S_DDF p0, float p1, struct S_DDF p2) { }
EXPORT void f7_V_SFS_DDD(struct S_DDD p0, float p1, struct S_DDD p2) { }
EXPORT void f7_V_SFS_DDP(struct S_DDP p0, float p1, struct S_DDP p2) { }
EXPORT void f7_V_SFS_DPI(struct S_DPI p0, float p1, struct S_DPI p2) { }
EXPORT void f7_V_SFS_DPF(struct S_DPF p0, float p1, struct S_DPF p2) { }
EXPORT void f7_V_SFS_DPD(struct S_DPD p0, float p1, struct S_DPD p2) { }
EXPORT void f7_V_SFS_DPP(struct S_DPP p0, float p1, struct S_DPP p2) { }
EXPORT void f7_V_SFS_PII(struct S_PII p0, float p1, struct S_PII p2) { }
EXPORT void f7_V_SFS_PIF(struct S_PIF p0, float p1, struct S_PIF p2) { }
EXPORT void f7_V_SFS_PID(struct S_PID p0, float p1, struct S_PID p2) { }
EXPORT void f7_V_SFS_PIP(struct S_PIP p0, float p1, struct S_PIP p2) { }
EXPORT void f7_V_SFS_PFI(struct S_PFI p0, float p1, struct S_PFI p2) { }
EXPORT void f7_V_SFS_PFF(struct S_PFF p0, float p1, struct S_PFF p2) { }
EXPORT void f7_V_SFS_PFD(struct S_PFD p0, float p1, struct S_PFD p2) { }
EXPORT void f7_V_SFS_PFP(struct S_PFP p0, float p1, struct S_PFP p2) { }
EXPORT void f7_V_SFS_PDI(struct S_PDI p0, float p1, struct S_PDI p2) { }
EXPORT void f7_V_SFS_PDF(struct S_PDF p0, float p1, struct S_PDF p2) { }
EXPORT void f7_V_SFS_PDD(struct S_PDD p0, float p1, struct S_PDD p2) { }
EXPORT void f7_V_SFS_PDP(struct S_PDP p0, float p1, struct S_PDP p2) { }
EXPORT void f7_V_SFS_PPI(struct S_PPI p0, float p1, struct S_PPI p2) { }
EXPORT void f7_V_SFS_PPF(struct S_PPF p0, float p1, struct S_PPF p2) { }
EXPORT void f7_V_SFS_PPD(struct S_PPD p0, float p1, struct S_PPD p2) { }
EXPORT void f7_V_SFS_PPP(struct S_PPP p0, float p1, struct S_PPP p2) { }
EXPORT void f7_V_SDI_I(struct S_I p0, double p1, int p2) { }
EXPORT void f7_V_SDI_F(struct S_F p0, double p1, int p2) { }
EXPORT void f7_V_SDI_D(struct S_D p0, double p1, int p2) { }
EXPORT void f7_V_SDI_P(struct S_P p0, double p1, int p2) { }
EXPORT void f7_V_SDI_II(struct S_II p0, double p1, int p2) { }
EXPORT void f7_V_SDI_IF(struct S_IF p0, double p1, int p2) { }
EXPORT void f7_V_SDI_ID(struct S_ID p0, double p1, int p2) { }
EXPORT void f7_V_SDI_IP(struct S_IP p0, double p1, int p2) { }
EXPORT void f7_V_SDI_FI(struct S_FI p0, double p1, int p2) { }
EXPORT void f7_V_SDI_FF(struct S_FF p0, double p1, int p2) { }
EXPORT void f7_V_SDI_FD(struct S_FD p0, double p1, int p2) { }
EXPORT void f8_V_SDI_FP(struct S_FP p0, double p1, int p2) { }
EXPORT void f8_V_SDI_DI(struct S_DI p0, double p1, int p2) { }
EXPORT void f8_V_SDI_DF(struct S_DF p0, double p1, int p2) { }
EXPORT void f8_V_SDI_DD(struct S_DD p0, double p1, int p2) { }
EXPORT void f8_V_SDI_DP(struct S_DP p0, double p1, int p2) { }
EXPORT void f8_V_SDI_PI(struct S_PI p0, double p1, int p2) { }
EXPORT void f8_V_SDI_PF(struct S_PF p0, double p1, int p2) { }
EXPORT void f8_V_SDI_PD(struct S_PD p0, double p1, int p2) { }
EXPORT void f8_V_SDI_PP(struct S_PP p0, double p1, int p2) { }
EXPORT void f8_V_SDI_III(struct S_III p0, double p1, int p2) { }
EXPORT void f8_V_SDI_IIF(struct S_IIF p0, double p1, int p2) { }
EXPORT void f8_V_SDI_IID(struct S_IID p0, double p1, int p2) { }
EXPORT void f8_V_SDI_IIP(struct S_IIP p0, double p1, int p2) { }
EXPORT void f8_V_SDI_IFI(struct S_IFI p0, double p1, int p2) { }
EXPORT void f8_V_SDI_IFF(struct S_IFF p0, double p1, int p2) { }
EXPORT void f8_V_SDI_IFD(struct S_IFD p0, double p1, int p2) { }
EXPORT void f8_V_SDI_IFP(struct S_IFP p0, double p1, int p2) { }
EXPORT void f8_V_SDI_IDI(struct S_IDI p0, double p1, int p2) { }
EXPORT void f8_V_SDI_IDF(struct S_IDF p0, double p1, int p2) { }
EXPORT void f8_V_SDI_IDD(struct S_IDD p0, double p1, int p2) { }
EXPORT void f8_V_SDI_IDP(struct S_IDP p0, double p1, int p2) { }
EXPORT void f8_V_SDI_IPI(struct S_IPI p0, double p1, int p2) { }
EXPORT void f8_V_SDI_IPF(struct S_IPF p0, double p1, int p2) { }
EXPORT void f8_V_SDI_IPD(struct S_IPD p0, double p1, int p2) { }
EXPORT void f8_V_SDI_IPP(struct S_IPP p0, double p1, int p2) { }
EXPORT void f8_V_SDI_FII(struct S_FII p0, double p1, int p2) { }
EXPORT void f8_V_SDI_FIF(struct S_FIF p0, double p1, int p2) { }
EXPORT void f8_V_SDI_FID(struct S_FID p0, double p1, int p2) { }
EXPORT void f8_V_SDI_FIP(struct S_FIP p0, double p1, int p2) { }
EXPORT void f8_V_SDI_FFI(struct S_FFI p0, double p1, int p2) { }
EXPORT void f8_V_SDI_FFF(struct S_FFF p0, double p1, int p2) { }
EXPORT void f8_V_SDI_FFD(struct S_FFD p0, double p1, int p2) { }
EXPORT void f8_V_SDI_FFP(struct S_FFP p0, double p1, int p2) { }
EXPORT void f8_V_SDI_FDI(struct S_FDI p0, double p1, int p2) { }
EXPORT void f8_V_SDI_FDF(struct S_FDF p0, double p1, int p2) { }
EXPORT void f8_V_SDI_FDD(struct S_FDD p0, double p1, int p2) { }
EXPORT void f8_V_SDI_FDP(struct S_FDP p0, double p1, int p2) { }
EXPORT void f8_V_SDI_FPI(struct S_FPI p0, double p1, int p2) { }
EXPORT void f8_V_SDI_FPF(struct S_FPF p0, double p1, int p2) { }
EXPORT void f8_V_SDI_FPD(struct S_FPD p0, double p1, int p2) { }
EXPORT void f8_V_SDI_FPP(struct S_FPP p0, double p1, int p2) { }
EXPORT void f8_V_SDI_DII(struct S_DII p0, double p1, int p2) { }
EXPORT void f8_V_SDI_DIF(struct S_DIF p0, double p1, int p2) { }
EXPORT void f8_V_SDI_DID(struct S_DID p0, double p1, int p2) { }
EXPORT void f8_V_SDI_DIP(struct S_DIP p0, double p1, int p2) { }
EXPORT void f8_V_SDI_DFI(struct S_DFI p0, double p1, int p2) { }
EXPORT void f8_V_SDI_DFF(struct S_DFF p0, double p1, int p2) { }
EXPORT void f8_V_SDI_DFD(struct S_DFD p0, double p1, int p2) { }
EXPORT void f8_V_SDI_DFP(struct S_DFP p0, double p1, int p2) { }
EXPORT void f8_V_SDI_DDI(struct S_DDI p0, double p1, int p2) { }
EXPORT void f8_V_SDI_DDF(struct S_DDF p0, double p1, int p2) { }
EXPORT void f8_V_SDI_DDD(struct S_DDD p0, double p1, int p2) { }
EXPORT void f8_V_SDI_DDP(struct S_DDP p0, double p1, int p2) { }
EXPORT void f8_V_SDI_DPI(struct S_DPI p0, double p1, int p2) { }
EXPORT void f8_V_SDI_DPF(struct S_DPF p0, double p1, int p2) { }
EXPORT void f8_V_SDI_DPD(struct S_DPD p0, double p1, int p2) { }
EXPORT void f8_V_SDI_DPP(struct S_DPP p0, double p1, int p2) { }
EXPORT void f8_V_SDI_PII(struct S_PII p0, double p1, int p2) { }
EXPORT void f8_V_SDI_PIF(struct S_PIF p0, double p1, int p2) { }
EXPORT void f8_V_SDI_PID(struct S_PID p0, double p1, int p2) { }
EXPORT void f8_V_SDI_PIP(struct S_PIP p0, double p1, int p2) { }
EXPORT void f8_V_SDI_PFI(struct S_PFI p0, double p1, int p2) { }
EXPORT void f8_V_SDI_PFF(struct S_PFF p0, double p1, int p2) { }
EXPORT void f8_V_SDI_PFD(struct S_PFD p0, double p1, int p2) { }
EXPORT void f8_V_SDI_PFP(struct S_PFP p0, double p1, int p2) { }
EXPORT void f8_V_SDI_PDI(struct S_PDI p0, double p1, int p2) { }
EXPORT void f8_V_SDI_PDF(struct S_PDF p0, double p1, int p2) { }
EXPORT void f8_V_SDI_PDD(struct S_PDD p0, double p1, int p2) { }
EXPORT void f8_V_SDI_PDP(struct S_PDP p0, double p1, int p2) { }
EXPORT void f8_V_SDI_PPI(struct S_PPI p0, double p1, int p2) { }
EXPORT void f8_V_SDI_PPF(struct S_PPF p0, double p1, int p2) { }
EXPORT void f8_V_SDI_PPD(struct S_PPD p0, double p1, int p2) { }
EXPORT void f8_V_SDI_PPP(struct S_PPP p0, double p1, int p2) { }
EXPORT void f8_V_SDF_I(struct S_I p0, double p1, float p2) { }
EXPORT void f8_V_SDF_F(struct S_F p0, double p1, float p2) { }
EXPORT void f8_V_SDF_D(struct S_D p0, double p1, float p2) { }
EXPORT void f8_V_SDF_P(struct S_P p0, double p1, float p2) { }
EXPORT void f8_V_SDF_II(struct S_II p0, double p1, float p2) { }
EXPORT void f8_V_SDF_IF(struct S_IF p0, double p1, float p2) { }
EXPORT void f8_V_SDF_ID(struct S_ID p0, double p1, float p2) { }
EXPORT void f8_V_SDF_IP(struct S_IP p0, double p1, float p2) { }
EXPORT void f8_V_SDF_FI(struct S_FI p0, double p1, float p2) { }
EXPORT void f8_V_SDF_FF(struct S_FF p0, double p1, float p2) { }
EXPORT void f8_V_SDF_FD(struct S_FD p0, double p1, float p2) { }
EXPORT void f8_V_SDF_FP(struct S_FP p0, double p1, float p2) { }
EXPORT void f8_V_SDF_DI(struct S_DI p0, double p1, float p2) { }
EXPORT void f8_V_SDF_DF(struct S_DF p0, double p1, float p2) { }
EXPORT void f8_V_SDF_DD(struct S_DD p0, double p1, float p2) { }
EXPORT void f8_V_SDF_DP(struct S_DP p0, double p1, float p2) { }
EXPORT void f8_V_SDF_PI(struct S_PI p0, double p1, float p2) { }
EXPORT void f8_V_SDF_PF(struct S_PF p0, double p1, float p2) { }
EXPORT void f8_V_SDF_PD(struct S_PD p0, double p1, float p2) { }
EXPORT void f8_V_SDF_PP(struct S_PP p0, double p1, float p2) { }
EXPORT void f8_V_SDF_III(struct S_III p0, double p1, float p2) { }
EXPORT void f8_V_SDF_IIF(struct S_IIF p0, double p1, float p2) { }
EXPORT void f8_V_SDF_IID(struct S_IID p0, double p1, float p2) { }
EXPORT void f8_V_SDF_IIP(struct S_IIP p0, double p1, float p2) { }
EXPORT void f8_V_SDF_IFI(struct S_IFI p0, double p1, float p2) { }
EXPORT void f8_V_SDF_IFF(struct S_IFF p0, double p1, float p2) { }
EXPORT void f8_V_SDF_IFD(struct S_IFD p0, double p1, float p2) { }
EXPORT void f8_V_SDF_IFP(struct S_IFP p0, double p1, float p2) { }
EXPORT void f8_V_SDF_IDI(struct S_IDI p0, double p1, float p2) { }
EXPORT void f8_V_SDF_IDF(struct S_IDF p0, double p1, float p2) { }
EXPORT void f8_V_SDF_IDD(struct S_IDD p0, double p1, float p2) { }
EXPORT void f8_V_SDF_IDP(struct S_IDP p0, double p1, float p2) { }
EXPORT void f8_V_SDF_IPI(struct S_IPI p0, double p1, float p2) { }
EXPORT void f8_V_SDF_IPF(struct S_IPF p0, double p1, float p2) { }
EXPORT void f8_V_SDF_IPD(struct S_IPD p0, double p1, float p2) { }
EXPORT void f8_V_SDF_IPP(struct S_IPP p0, double p1, float p2) { }
EXPORT void f8_V_SDF_FII(struct S_FII p0, double p1, float p2) { }
EXPORT void f8_V_SDF_FIF(struct S_FIF p0, double p1, float p2) { }
EXPORT void f8_V_SDF_FID(struct S_FID p0, double p1, float p2) { }
EXPORT void f8_V_SDF_FIP(struct S_FIP p0, double p1, float p2) { }
EXPORT void f8_V_SDF_FFI(struct S_FFI p0, double p1, float p2) { }
EXPORT void f8_V_SDF_FFF(struct S_FFF p0, double p1, float p2) { }
EXPORT void f8_V_SDF_FFD(struct S_FFD p0, double p1, float p2) { }
EXPORT void f8_V_SDF_FFP(struct S_FFP p0, double p1, float p2) { }
EXPORT void f8_V_SDF_FDI(struct S_FDI p0, double p1, float p2) { }
EXPORT void f8_V_SDF_FDF(struct S_FDF p0, double p1, float p2) { }
EXPORT void f8_V_SDF_FDD(struct S_FDD p0, double p1, float p2) { }
EXPORT void f8_V_SDF_FDP(struct S_FDP p0, double p1, float p2) { }
EXPORT void f8_V_SDF_FPI(struct S_FPI p0, double p1, float p2) { }
EXPORT void f8_V_SDF_FPF(struct S_FPF p0, double p1, float p2) { }
EXPORT void f8_V_SDF_FPD(struct S_FPD p0, double p1, float p2) { }
EXPORT void f8_V_SDF_FPP(struct S_FPP p0, double p1, float p2) { }
EXPORT void f8_V_SDF_DII(struct S_DII p0, double p1, float p2) { }
EXPORT void f8_V_SDF_DIF(struct S_DIF p0, double p1, float p2) { }
EXPORT void f8_V_SDF_DID(struct S_DID p0, double p1, float p2) { }
EXPORT void f8_V_SDF_DIP(struct S_DIP p0, double p1, float p2) { }
EXPORT void f8_V_SDF_DFI(struct S_DFI p0, double p1, float p2) { }
EXPORT void f8_V_SDF_DFF(struct S_DFF p0, double p1, float p2) { }
EXPORT void f8_V_SDF_DFD(struct S_DFD p0, double p1, float p2) { }
EXPORT void f8_V_SDF_DFP(struct S_DFP p0, double p1, float p2) { }
EXPORT void f8_V_SDF_DDI(struct S_DDI p0, double p1, float p2) { }
EXPORT void f8_V_SDF_DDF(struct S_DDF p0, double p1, float p2) { }
EXPORT void f8_V_SDF_DDD(struct S_DDD p0, double p1, float p2) { }
EXPORT void f8_V_SDF_DDP(struct S_DDP p0, double p1, float p2) { }
EXPORT void f8_V_SDF_DPI(struct S_DPI p0, double p1, float p2) { }
EXPORT void f8_V_SDF_DPF(struct S_DPF p0, double p1, float p2) { }
EXPORT void f8_V_SDF_DPD(struct S_DPD p0, double p1, float p2) { }
EXPORT void f8_V_SDF_DPP(struct S_DPP p0, double p1, float p2) { }
EXPORT void f8_V_SDF_PII(struct S_PII p0, double p1, float p2) { }
EXPORT void f8_V_SDF_PIF(struct S_PIF p0, double p1, float p2) { }
EXPORT void f8_V_SDF_PID(struct S_PID p0, double p1, float p2) { }
EXPORT void f8_V_SDF_PIP(struct S_PIP p0, double p1, float p2) { }
EXPORT void f8_V_SDF_PFI(struct S_PFI p0, double p1, float p2) { }
EXPORT void f8_V_SDF_PFF(struct S_PFF p0, double p1, float p2) { }
EXPORT void f8_V_SDF_PFD(struct S_PFD p0, double p1, float p2) { }
EXPORT void f8_V_SDF_PFP(struct S_PFP p0, double p1, float p2) { }
EXPORT void f8_V_SDF_PDI(struct S_PDI p0, double p1, float p2) { }
EXPORT void f8_V_SDF_PDF(struct S_PDF p0, double p1, float p2) { }
EXPORT void f8_V_SDF_PDD(struct S_PDD p0, double p1, float p2) { }
EXPORT void f8_V_SDF_PDP(struct S_PDP p0, double p1, float p2) { }
EXPORT void f8_V_SDF_PPI(struct S_PPI p0, double p1, float p2) { }
EXPORT void f8_V_SDF_PPF(struct S_PPF p0, double p1, float p2) { }
EXPORT void f8_V_SDF_PPD(struct S_PPD p0, double p1, float p2) { }
EXPORT void f8_V_SDF_PPP(struct S_PPP p0, double p1, float p2) { }
EXPORT void f8_V_SDD_I(struct S_I p0, double p1, double p2) { }
EXPORT void f8_V_SDD_F(struct S_F p0, double p1, double p2) { }
EXPORT void f8_V_SDD_D(struct S_D p0, double p1, double p2) { }
EXPORT void f8_V_SDD_P(struct S_P p0, double p1, double p2) { }
EXPORT void f8_V_SDD_II(struct S_II p0, double p1, double p2) { }
EXPORT void f8_V_SDD_IF(struct S_IF p0, double p1, double p2) { }
EXPORT void f8_V_SDD_ID(struct S_ID p0, double p1, double p2) { }
EXPORT void f8_V_SDD_IP(struct S_IP p0, double p1, double p2) { }
EXPORT void f8_V_SDD_FI(struct S_FI p0, double p1, double p2) { }
EXPORT void f8_V_SDD_FF(struct S_FF p0, double p1, double p2) { }
EXPORT void f8_V_SDD_FD(struct S_FD p0, double p1, double p2) { }
EXPORT void f8_V_SDD_FP(struct S_FP p0, double p1, double p2) { }
EXPORT void f8_V_SDD_DI(struct S_DI p0, double p1, double p2) { }
EXPORT void f8_V_SDD_DF(struct S_DF p0, double p1, double p2) { }
EXPORT void f8_V_SDD_DD(struct S_DD p0, double p1, double p2) { }
EXPORT void f8_V_SDD_DP(struct S_DP p0, double p1, double p2) { }
EXPORT void f8_V_SDD_PI(struct S_PI p0, double p1, double p2) { }
EXPORT void f8_V_SDD_PF(struct S_PF p0, double p1, double p2) { }
EXPORT void f8_V_SDD_PD(struct S_PD p0, double p1, double p2) { }
EXPORT void f8_V_SDD_PP(struct S_PP p0, double p1, double p2) { }
EXPORT void f8_V_SDD_III(struct S_III p0, double p1, double p2) { }
EXPORT void f8_V_SDD_IIF(struct S_IIF p0, double p1, double p2) { }
EXPORT void f8_V_SDD_IID(struct S_IID p0, double p1, double p2) { }
EXPORT void f8_V_SDD_IIP(struct S_IIP p0, double p1, double p2) { }
EXPORT void f8_V_SDD_IFI(struct S_IFI p0, double p1, double p2) { }
EXPORT void f8_V_SDD_IFF(struct S_IFF p0, double p1, double p2) { }
EXPORT void f8_V_SDD_IFD(struct S_IFD p0, double p1, double p2) { }
EXPORT void f8_V_SDD_IFP(struct S_IFP p0, double p1, double p2) { }
EXPORT void f8_V_SDD_IDI(struct S_IDI p0, double p1, double p2) { }
EXPORT void f8_V_SDD_IDF(struct S_IDF p0, double p1, double p2) { }
EXPORT void f8_V_SDD_IDD(struct S_IDD p0, double p1, double p2) { }
EXPORT void f8_V_SDD_IDP(struct S_IDP p0, double p1, double p2) { }
EXPORT void f8_V_SDD_IPI(struct S_IPI p0, double p1, double p2) { }
EXPORT void f8_V_SDD_IPF(struct S_IPF p0, double p1, double p2) { }
EXPORT void f8_V_SDD_IPD(struct S_IPD p0, double p1, double p2) { }
EXPORT void f8_V_SDD_IPP(struct S_IPP p0, double p1, double p2) { }
EXPORT void f8_V_SDD_FII(struct S_FII p0, double p1, double p2) { }
EXPORT void f8_V_SDD_FIF(struct S_FIF p0, double p1, double p2) { }
EXPORT void f8_V_SDD_FID(struct S_FID p0, double p1, double p2) { }
EXPORT void f8_V_SDD_FIP(struct S_FIP p0, double p1, double p2) { }
EXPORT void f8_V_SDD_FFI(struct S_FFI p0, double p1, double p2) { }
EXPORT void f8_V_SDD_FFF(struct S_FFF p0, double p1, double p2) { }
EXPORT void f8_V_SDD_FFD(struct S_FFD p0, double p1, double p2) { }
EXPORT void f8_V_SDD_FFP(struct S_FFP p0, double p1, double p2) { }
EXPORT void f8_V_SDD_FDI(struct S_FDI p0, double p1, double p2) { }
EXPORT void f8_V_SDD_FDF(struct S_FDF p0, double p1, double p2) { }
EXPORT void f8_V_SDD_FDD(struct S_FDD p0, double p1, double p2) { }
EXPORT void f8_V_SDD_FDP(struct S_FDP p0, double p1, double p2) { }
EXPORT void f8_V_SDD_FPI(struct S_FPI p0, double p1, double p2) { }
EXPORT void f8_V_SDD_FPF(struct S_FPF p0, double p1, double p2) { }
EXPORT void f8_V_SDD_FPD(struct S_FPD p0, double p1, double p2) { }
EXPORT void f8_V_SDD_FPP(struct S_FPP p0, double p1, double p2) { }
EXPORT void f8_V_SDD_DII(struct S_DII p0, double p1, double p2) { }
EXPORT void f8_V_SDD_DIF(struct S_DIF p0, double p1, double p2) { }
EXPORT void f8_V_SDD_DID(struct S_DID p0, double p1, double p2) { }
EXPORT void f8_V_SDD_DIP(struct S_DIP p0, double p1, double p2) { }
EXPORT void f8_V_SDD_DFI(struct S_DFI p0, double p1, double p2) { }
EXPORT void f8_V_SDD_DFF(struct S_DFF p0, double p1, double p2) { }
EXPORT void f8_V_SDD_DFD(struct S_DFD p0, double p1, double p2) { }
EXPORT void f8_V_SDD_DFP(struct S_DFP p0, double p1, double p2) { }
EXPORT void f8_V_SDD_DDI(struct S_DDI p0, double p1, double p2) { }
EXPORT void f8_V_SDD_DDF(struct S_DDF p0, double p1, double p2) { }
EXPORT void f8_V_SDD_DDD(struct S_DDD p0, double p1, double p2) { }
EXPORT void f8_V_SDD_DDP(struct S_DDP p0, double p1, double p2) { }
EXPORT void f8_V_SDD_DPI(struct S_DPI p0, double p1, double p2) { }
EXPORT void f8_V_SDD_DPF(struct S_DPF p0, double p1, double p2) { }
EXPORT void f8_V_SDD_DPD(struct S_DPD p0, double p1, double p2) { }
EXPORT void f8_V_SDD_DPP(struct S_DPP p0, double p1, double p2) { }
EXPORT void f8_V_SDD_PII(struct S_PII p0, double p1, double p2) { }
EXPORT void f8_V_SDD_PIF(struct S_PIF p0, double p1, double p2) { }
EXPORT void f8_V_SDD_PID(struct S_PID p0, double p1, double p2) { }
EXPORT void f8_V_SDD_PIP(struct S_PIP p0, double p1, double p2) { }
EXPORT void f8_V_SDD_PFI(struct S_PFI p0, double p1, double p2) { }
EXPORT void f8_V_SDD_PFF(struct S_PFF p0, double p1, double p2) { }
EXPORT void f8_V_SDD_PFD(struct S_PFD p0, double p1, double p2) { }
EXPORT void f8_V_SDD_PFP(struct S_PFP p0, double p1, double p2) { }
EXPORT void f8_V_SDD_PDI(struct S_PDI p0, double p1, double p2) { }
EXPORT void f8_V_SDD_PDF(struct S_PDF p0, double p1, double p2) { }
EXPORT void f8_V_SDD_PDD(struct S_PDD p0, double p1, double p2) { }
EXPORT void f8_V_SDD_PDP(struct S_PDP p0, double p1, double p2) { }
EXPORT void f8_V_SDD_PPI(struct S_PPI p0, double p1, double p2) { }
EXPORT void f8_V_SDD_PPF(struct S_PPF p0, double p1, double p2) { }
EXPORT void f8_V_SDD_PPD(struct S_PPD p0, double p1, double p2) { }
EXPORT void f8_V_SDD_PPP(struct S_PPP p0, double p1, double p2) { }
EXPORT void f8_V_SDP_I(struct S_I p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_F(struct S_F p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_D(struct S_D p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_P(struct S_P p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_II(struct S_II p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_IF(struct S_IF p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_ID(struct S_ID p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_IP(struct S_IP p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_FI(struct S_FI p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_FF(struct S_FF p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_FD(struct S_FD p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_FP(struct S_FP p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_DI(struct S_DI p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_DF(struct S_DF p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_DD(struct S_DD p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_DP(struct S_DP p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_PI(struct S_PI p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_PF(struct S_PF p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_PD(struct S_PD p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_PP(struct S_PP p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_III(struct S_III p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_IIF(struct S_IIF p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_IID(struct S_IID p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_IIP(struct S_IIP p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_IFI(struct S_IFI p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_IFF(struct S_IFF p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_IFD(struct S_IFD p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_IFP(struct S_IFP p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_IDI(struct S_IDI p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_IDF(struct S_IDF p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_IDD(struct S_IDD p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_IDP(struct S_IDP p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_IPI(struct S_IPI p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_IPF(struct S_IPF p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_IPD(struct S_IPD p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_IPP(struct S_IPP p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_FII(struct S_FII p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_FIF(struct S_FIF p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_FID(struct S_FID p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_FIP(struct S_FIP p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_FFI(struct S_FFI p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_FFF(struct S_FFF p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_FFD(struct S_FFD p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_FFP(struct S_FFP p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_FDI(struct S_FDI p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_FDF(struct S_FDF p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_FDD(struct S_FDD p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_FDP(struct S_FDP p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_FPI(struct S_FPI p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_FPF(struct S_FPF p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_FPD(struct S_FPD p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_FPP(struct S_FPP p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_DII(struct S_DII p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_DIF(struct S_DIF p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_DID(struct S_DID p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_DIP(struct S_DIP p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_DFI(struct S_DFI p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_DFF(struct S_DFF p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_DFD(struct S_DFD p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_DFP(struct S_DFP p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_DDI(struct S_DDI p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_DDF(struct S_DDF p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_DDD(struct S_DDD p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_DDP(struct S_DDP p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_DPI(struct S_DPI p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_DPF(struct S_DPF p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_DPD(struct S_DPD p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_DPP(struct S_DPP p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_PII(struct S_PII p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_PIF(struct S_PIF p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_PID(struct S_PID p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_PIP(struct S_PIP p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_PFI(struct S_PFI p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_PFF(struct S_PFF p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_PFD(struct S_PFD p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_PFP(struct S_PFP p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_PDI(struct S_PDI p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_PDF(struct S_PDF p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_PDD(struct S_PDD p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_PDP(struct S_PDP p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_PPI(struct S_PPI p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_PPF(struct S_PPF p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_PPD(struct S_PPD p0, double p1, void* p2) { }
EXPORT void f8_V_SDP_PPP(struct S_PPP p0, double p1, void* p2) { }
EXPORT void f8_V_SDS_I(struct S_I p0, double p1, struct S_I p2) { }
EXPORT void f8_V_SDS_F(struct S_F p0, double p1, struct S_F p2) { }
EXPORT void f8_V_SDS_D(struct S_D p0, double p1, struct S_D p2) { }
EXPORT void f8_V_SDS_P(struct S_P p0, double p1, struct S_P p2) { }
EXPORT void f8_V_SDS_II(struct S_II p0, double p1, struct S_II p2) { }
EXPORT void f8_V_SDS_IF(struct S_IF p0, double p1, struct S_IF p2) { }
EXPORT void f8_V_SDS_ID(struct S_ID p0, double p1, struct S_ID p2) { }
EXPORT void f8_V_SDS_IP(struct S_IP p0, double p1, struct S_IP p2) { }
EXPORT void f8_V_SDS_FI(struct S_FI p0, double p1, struct S_FI p2) { }
EXPORT void f8_V_SDS_FF(struct S_FF p0, double p1, struct S_FF p2) { }
EXPORT void f8_V_SDS_FD(struct S_FD p0, double p1, struct S_FD p2) { }
EXPORT void f8_V_SDS_FP(struct S_FP p0, double p1, struct S_FP p2) { }
EXPORT void f8_V_SDS_DI(struct S_DI p0, double p1, struct S_DI p2) { }
EXPORT void f8_V_SDS_DF(struct S_DF p0, double p1, struct S_DF p2) { }
EXPORT void f8_V_SDS_DD(struct S_DD p0, double p1, struct S_DD p2) { }
EXPORT void f8_V_SDS_DP(struct S_DP p0, double p1, struct S_DP p2) { }
EXPORT void f8_V_SDS_PI(struct S_PI p0, double p1, struct S_PI p2) { }
EXPORT void f8_V_SDS_PF(struct S_PF p0, double p1, struct S_PF p2) { }
EXPORT void f8_V_SDS_PD(struct S_PD p0, double p1, struct S_PD p2) { }
EXPORT void f8_V_SDS_PP(struct S_PP p0, double p1, struct S_PP p2) { }
EXPORT void f8_V_SDS_III(struct S_III p0, double p1, struct S_III p2) { }
EXPORT void f8_V_SDS_IIF(struct S_IIF p0, double p1, struct S_IIF p2) { }
EXPORT void f8_V_SDS_IID(struct S_IID p0, double p1, struct S_IID p2) { }
EXPORT void f8_V_SDS_IIP(struct S_IIP p0, double p1, struct S_IIP p2) { }
EXPORT void f8_V_SDS_IFI(struct S_IFI p0, double p1, struct S_IFI p2) { }
EXPORT void f8_V_SDS_IFF(struct S_IFF p0, double p1, struct S_IFF p2) { }
EXPORT void f8_V_SDS_IFD(struct S_IFD p0, double p1, struct S_IFD p2) { }
EXPORT void f8_V_SDS_IFP(struct S_IFP p0, double p1, struct S_IFP p2) { }
EXPORT void f8_V_SDS_IDI(struct S_IDI p0, double p1, struct S_IDI p2) { }
EXPORT void f8_V_SDS_IDF(struct S_IDF p0, double p1, struct S_IDF p2) { }
EXPORT void f8_V_SDS_IDD(struct S_IDD p0, double p1, struct S_IDD p2) { }
EXPORT void f8_V_SDS_IDP(struct S_IDP p0, double p1, struct S_IDP p2) { }
EXPORT void f8_V_SDS_IPI(struct S_IPI p0, double p1, struct S_IPI p2) { }
EXPORT void f8_V_SDS_IPF(struct S_IPF p0, double p1, struct S_IPF p2) { }
EXPORT void f8_V_SDS_IPD(struct S_IPD p0, double p1, struct S_IPD p2) { }
EXPORT void f8_V_SDS_IPP(struct S_IPP p0, double p1, struct S_IPP p2) { }
EXPORT void f8_V_SDS_FII(struct S_FII p0, double p1, struct S_FII p2) { }
EXPORT void f8_V_SDS_FIF(struct S_FIF p0, double p1, struct S_FIF p2) { }
EXPORT void f8_V_SDS_FID(struct S_FID p0, double p1, struct S_FID p2) { }
EXPORT void f8_V_SDS_FIP(struct S_FIP p0, double p1, struct S_FIP p2) { }
EXPORT void f8_V_SDS_FFI(struct S_FFI p0, double p1, struct S_FFI p2) { }
EXPORT void f8_V_SDS_FFF(struct S_FFF p0, double p1, struct S_FFF p2) { }
EXPORT void f8_V_SDS_FFD(struct S_FFD p0, double p1, struct S_FFD p2) { }
EXPORT void f8_V_SDS_FFP(struct S_FFP p0, double p1, struct S_FFP p2) { }
EXPORT void f8_V_SDS_FDI(struct S_FDI p0, double p1, struct S_FDI p2) { }
EXPORT void f8_V_SDS_FDF(struct S_FDF p0, double p1, struct S_FDF p2) { }
EXPORT void f8_V_SDS_FDD(struct S_FDD p0, double p1, struct S_FDD p2) { }
EXPORT void f8_V_SDS_FDP(struct S_FDP p0, double p1, struct S_FDP p2) { }
EXPORT void f8_V_SDS_FPI(struct S_FPI p0, double p1, struct S_FPI p2) { }
EXPORT void f8_V_SDS_FPF(struct S_FPF p0, double p1, struct S_FPF p2) { }
EXPORT void f8_V_SDS_FPD(struct S_FPD p0, double p1, struct S_FPD p2) { }
EXPORT void f8_V_SDS_FPP(struct S_FPP p0, double p1, struct S_FPP p2) { }
EXPORT void f8_V_SDS_DII(struct S_DII p0, double p1, struct S_DII p2) { }
EXPORT void f8_V_SDS_DIF(struct S_DIF p0, double p1, struct S_DIF p2) { }
EXPORT void f8_V_SDS_DID(struct S_DID p0, double p1, struct S_DID p2) { }
EXPORT void f8_V_SDS_DIP(struct S_DIP p0, double p1, struct S_DIP p2) { }
EXPORT void f8_V_SDS_DFI(struct S_DFI p0, double p1, struct S_DFI p2) { }
EXPORT void f8_V_SDS_DFF(struct S_DFF p0, double p1, struct S_DFF p2) { }
EXPORT void f8_V_SDS_DFD(struct S_DFD p0, double p1, struct S_DFD p2) { }
EXPORT void f8_V_SDS_DFP(struct S_DFP p0, double p1, struct S_DFP p2) { }
EXPORT void f8_V_SDS_DDI(struct S_DDI p0, double p1, struct S_DDI p2) { }
EXPORT void f8_V_SDS_DDF(struct S_DDF p0, double p1, struct S_DDF p2) { }
EXPORT void f8_V_SDS_DDD(struct S_DDD p0, double p1, struct S_DDD p2) { }
EXPORT void f8_V_SDS_DDP(struct S_DDP p0, double p1, struct S_DDP p2) { }
EXPORT void f8_V_SDS_DPI(struct S_DPI p0, double p1, struct S_DPI p2) { }
EXPORT void f8_V_SDS_DPF(struct S_DPF p0, double p1, struct S_DPF p2) { }
EXPORT void f8_V_SDS_DPD(struct S_DPD p0, double p1, struct S_DPD p2) { }
EXPORT void f8_V_SDS_DPP(struct S_DPP p0, double p1, struct S_DPP p2) { }
EXPORT void f8_V_SDS_PII(struct S_PII p0, double p1, struct S_PII p2) { }
EXPORT void f8_V_SDS_PIF(struct S_PIF p0, double p1, struct S_PIF p2) { }
EXPORT void f8_V_SDS_PID(struct S_PID p0, double p1, struct S_PID p2) { }
EXPORT void f8_V_SDS_PIP(struct S_PIP p0, double p1, struct S_PIP p2) { }
EXPORT void f8_V_SDS_PFI(struct S_PFI p0, double p1, struct S_PFI p2) { }
EXPORT void f8_V_SDS_PFF(struct S_PFF p0, double p1, struct S_PFF p2) { }
EXPORT void f8_V_SDS_PFD(struct S_PFD p0, double p1, struct S_PFD p2) { }
EXPORT void f8_V_SDS_PFP(struct S_PFP p0, double p1, struct S_PFP p2) { }
EXPORT void f8_V_SDS_PDI(struct S_PDI p0, double p1, struct S_PDI p2) { }
EXPORT void f8_V_SDS_PDF(struct S_PDF p0, double p1, struct S_PDF p2) { }
EXPORT void f8_V_SDS_PDD(struct S_PDD p0, double p1, struct S_PDD p2) { }
EXPORT void f8_V_SDS_PDP(struct S_PDP p0, double p1, struct S_PDP p2) { }
EXPORT void f8_V_SDS_PPI(struct S_PPI p0, double p1, struct S_PPI p2) { }
EXPORT void f8_V_SDS_PPF(struct S_PPF p0, double p1, struct S_PPF p2) { }
EXPORT void f8_V_SDS_PPD(struct S_PPD p0, double p1, struct S_PPD p2) { }
EXPORT void f8_V_SDS_PPP(struct S_PPP p0, double p1, struct S_PPP p2) { }
EXPORT void f8_V_SPI_I(struct S_I p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_F(struct S_F p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_D(struct S_D p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_P(struct S_P p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_II(struct S_II p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_IF(struct S_IF p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_ID(struct S_ID p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_IP(struct S_IP p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_FI(struct S_FI p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_FF(struct S_FF p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_FD(struct S_FD p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_FP(struct S_FP p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_DI(struct S_DI p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_DF(struct S_DF p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_DD(struct S_DD p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_DP(struct S_DP p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_PI(struct S_PI p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_PF(struct S_PF p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_PD(struct S_PD p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_PP(struct S_PP p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_III(struct S_III p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_IIF(struct S_IIF p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_IID(struct S_IID p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_IIP(struct S_IIP p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_IFI(struct S_IFI p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_IFF(struct S_IFF p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_IFD(struct S_IFD p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_IFP(struct S_IFP p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_IDI(struct S_IDI p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_IDF(struct S_IDF p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_IDD(struct S_IDD p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_IDP(struct S_IDP p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_IPI(struct S_IPI p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_IPF(struct S_IPF p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_IPD(struct S_IPD p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_IPP(struct S_IPP p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_FII(struct S_FII p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_FIF(struct S_FIF p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_FID(struct S_FID p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_FIP(struct S_FIP p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_FFI(struct S_FFI p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_FFF(struct S_FFF p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_FFD(struct S_FFD p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_FFP(struct S_FFP p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_FDI(struct S_FDI p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_FDF(struct S_FDF p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_FDD(struct S_FDD p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_FDP(struct S_FDP p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_FPI(struct S_FPI p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_FPF(struct S_FPF p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_FPD(struct S_FPD p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_FPP(struct S_FPP p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_DII(struct S_DII p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_DIF(struct S_DIF p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_DID(struct S_DID p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_DIP(struct S_DIP p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_DFI(struct S_DFI p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_DFF(struct S_DFF p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_DFD(struct S_DFD p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_DFP(struct S_DFP p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_DDI(struct S_DDI p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_DDF(struct S_DDF p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_DDD(struct S_DDD p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_DDP(struct S_DDP p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_DPI(struct S_DPI p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_DPF(struct S_DPF p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_DPD(struct S_DPD p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_DPP(struct S_DPP p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_PII(struct S_PII p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_PIF(struct S_PIF p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_PID(struct S_PID p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_PIP(struct S_PIP p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_PFI(struct S_PFI p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_PFF(struct S_PFF p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_PFD(struct S_PFD p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_PFP(struct S_PFP p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_PDI(struct S_PDI p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_PDF(struct S_PDF p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_PDD(struct S_PDD p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_PDP(struct S_PDP p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_PPI(struct S_PPI p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_PPF(struct S_PPF p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_PPD(struct S_PPD p0, void* p1, int p2) { }
EXPORT void f8_V_SPI_PPP(struct S_PPP p0, void* p1, int p2) { }
EXPORT void f8_V_SPF_I(struct S_I p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_F(struct S_F p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_D(struct S_D p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_P(struct S_P p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_II(struct S_II p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_IF(struct S_IF p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_ID(struct S_ID p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_IP(struct S_IP p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_FI(struct S_FI p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_FF(struct S_FF p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_FD(struct S_FD p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_FP(struct S_FP p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_DI(struct S_DI p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_DF(struct S_DF p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_DD(struct S_DD p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_DP(struct S_DP p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_PI(struct S_PI p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_PF(struct S_PF p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_PD(struct S_PD p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_PP(struct S_PP p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_III(struct S_III p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_IIF(struct S_IIF p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_IID(struct S_IID p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_IIP(struct S_IIP p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_IFI(struct S_IFI p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_IFF(struct S_IFF p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_IFD(struct S_IFD p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_IFP(struct S_IFP p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_IDI(struct S_IDI p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_IDF(struct S_IDF p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_IDD(struct S_IDD p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_IDP(struct S_IDP p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_IPI(struct S_IPI p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_IPF(struct S_IPF p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_IPD(struct S_IPD p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_IPP(struct S_IPP p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_FII(struct S_FII p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_FIF(struct S_FIF p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_FID(struct S_FID p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_FIP(struct S_FIP p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_FFI(struct S_FFI p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_FFF(struct S_FFF p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_FFD(struct S_FFD p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_FFP(struct S_FFP p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_FDI(struct S_FDI p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_FDF(struct S_FDF p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_FDD(struct S_FDD p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_FDP(struct S_FDP p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_FPI(struct S_FPI p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_FPF(struct S_FPF p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_FPD(struct S_FPD p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_FPP(struct S_FPP p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_DII(struct S_DII p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_DIF(struct S_DIF p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_DID(struct S_DID p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_DIP(struct S_DIP p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_DFI(struct S_DFI p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_DFF(struct S_DFF p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_DFD(struct S_DFD p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_DFP(struct S_DFP p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_DDI(struct S_DDI p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_DDF(struct S_DDF p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_DDD(struct S_DDD p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_DDP(struct S_DDP p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_DPI(struct S_DPI p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_DPF(struct S_DPF p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_DPD(struct S_DPD p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_DPP(struct S_DPP p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_PII(struct S_PII p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_PIF(struct S_PIF p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_PID(struct S_PID p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_PIP(struct S_PIP p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_PFI(struct S_PFI p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_PFF(struct S_PFF p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_PFD(struct S_PFD p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_PFP(struct S_PFP p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_PDI(struct S_PDI p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_PDF(struct S_PDF p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_PDD(struct S_PDD p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_PDP(struct S_PDP p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_PPI(struct S_PPI p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_PPF(struct S_PPF p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_PPD(struct S_PPD p0, void* p1, float p2) { }
EXPORT void f8_V_SPF_PPP(struct S_PPP p0, void* p1, float p2) { }
EXPORT void f8_V_SPD_I(struct S_I p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_F(struct S_F p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_D(struct S_D p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_P(struct S_P p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_II(struct S_II p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_IF(struct S_IF p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_ID(struct S_ID p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_IP(struct S_IP p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_FI(struct S_FI p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_FF(struct S_FF p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_FD(struct S_FD p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_FP(struct S_FP p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_DI(struct S_DI p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_DF(struct S_DF p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_DD(struct S_DD p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_DP(struct S_DP p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_PI(struct S_PI p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_PF(struct S_PF p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_PD(struct S_PD p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_PP(struct S_PP p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_III(struct S_III p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_IIF(struct S_IIF p0, void* p1, double p2) { }
EXPORT void f8_V_SPD_IID(struct S_IID p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_IIP(struct S_IIP p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_IFI(struct S_IFI p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_IFF(struct S_IFF p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_IFD(struct S_IFD p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_IFP(struct S_IFP p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_IDI(struct S_IDI p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_IDF(struct S_IDF p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_IDD(struct S_IDD p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_IDP(struct S_IDP p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_IPI(struct S_IPI p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_IPF(struct S_IPF p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_IPD(struct S_IPD p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_IPP(struct S_IPP p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_FII(struct S_FII p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_FIF(struct S_FIF p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_FID(struct S_FID p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_FIP(struct S_FIP p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_FFI(struct S_FFI p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_FFF(struct S_FFF p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_FFD(struct S_FFD p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_FFP(struct S_FFP p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_FDI(struct S_FDI p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_FDF(struct S_FDF p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_FDD(struct S_FDD p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_FDP(struct S_FDP p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_FPI(struct S_FPI p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_FPF(struct S_FPF p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_FPD(struct S_FPD p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_FPP(struct S_FPP p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_DII(struct S_DII p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_DIF(struct S_DIF p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_DID(struct S_DID p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_DIP(struct S_DIP p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_DFI(struct S_DFI p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_DFF(struct S_DFF p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_DFD(struct S_DFD p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_DFP(struct S_DFP p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_DDI(struct S_DDI p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_DDF(struct S_DDF p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_DDD(struct S_DDD p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_DDP(struct S_DDP p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_DPI(struct S_DPI p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_DPF(struct S_DPF p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_DPD(struct S_DPD p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_DPP(struct S_DPP p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_PII(struct S_PII p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_PIF(struct S_PIF p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_PID(struct S_PID p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_PIP(struct S_PIP p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_PFI(struct S_PFI p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_PFF(struct S_PFF p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_PFD(struct S_PFD p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_PFP(struct S_PFP p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_PDI(struct S_PDI p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_PDF(struct S_PDF p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_PDD(struct S_PDD p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_PDP(struct S_PDP p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_PPI(struct S_PPI p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_PPF(struct S_PPF p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_PPD(struct S_PPD p0, void* p1, double p2) { }
EXPORT void f9_V_SPD_PPP(struct S_PPP p0, void* p1, double p2) { }
EXPORT void f9_V_SPP_I(struct S_I p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_F(struct S_F p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_D(struct S_D p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_P(struct S_P p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_II(struct S_II p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_IF(struct S_IF p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_ID(struct S_ID p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_IP(struct S_IP p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_FI(struct S_FI p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_FF(struct S_FF p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_FD(struct S_FD p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_FP(struct S_FP p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_DI(struct S_DI p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_DF(struct S_DF p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_DD(struct S_DD p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_DP(struct S_DP p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_PI(struct S_PI p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_PF(struct S_PF p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_PD(struct S_PD p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_PP(struct S_PP p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_III(struct S_III p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_IIF(struct S_IIF p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_IID(struct S_IID p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_IIP(struct S_IIP p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_IFI(struct S_IFI p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_IFF(struct S_IFF p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_IFD(struct S_IFD p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_IFP(struct S_IFP p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_IDI(struct S_IDI p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_IDF(struct S_IDF p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_IDD(struct S_IDD p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_IDP(struct S_IDP p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_IPI(struct S_IPI p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_IPF(struct S_IPF p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_IPD(struct S_IPD p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_IPP(struct S_IPP p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_FII(struct S_FII p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_FIF(struct S_FIF p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_FID(struct S_FID p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_FIP(struct S_FIP p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_FFI(struct S_FFI p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_FFF(struct S_FFF p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_FFD(struct S_FFD p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_FFP(struct S_FFP p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_FDI(struct S_FDI p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_FDF(struct S_FDF p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_FDD(struct S_FDD p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_FDP(struct S_FDP p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_FPI(struct S_FPI p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_FPF(struct S_FPF p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_FPD(struct S_FPD p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_FPP(struct S_FPP p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_DII(struct S_DII p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_DIF(struct S_DIF p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_DID(struct S_DID p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_DIP(struct S_DIP p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_DFI(struct S_DFI p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_DFF(struct S_DFF p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_DFD(struct S_DFD p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_DFP(struct S_DFP p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_DDI(struct S_DDI p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_DDF(struct S_DDF p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_DDD(struct S_DDD p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_DDP(struct S_DDP p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_DPI(struct S_DPI p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_DPF(struct S_DPF p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_DPD(struct S_DPD p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_DPP(struct S_DPP p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_PII(struct S_PII p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_PIF(struct S_PIF p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_PID(struct S_PID p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_PIP(struct S_PIP p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_PFI(struct S_PFI p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_PFF(struct S_PFF p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_PFD(struct S_PFD p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_PFP(struct S_PFP p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_PDI(struct S_PDI p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_PDF(struct S_PDF p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_PDD(struct S_PDD p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_PDP(struct S_PDP p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_PPI(struct S_PPI p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_PPF(struct S_PPF p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_PPD(struct S_PPD p0, void* p1, void* p2) { }
EXPORT void f9_V_SPP_PPP(struct S_PPP p0, void* p1, void* p2) { }
EXPORT void f9_V_SPS_I(struct S_I p0, void* p1, struct S_I p2) { }
EXPORT void f9_V_SPS_F(struct S_F p0, void* p1, struct S_F p2) { }
EXPORT void f9_V_SPS_D(struct S_D p0, void* p1, struct S_D p2) { }
EXPORT void f9_V_SPS_P(struct S_P p0, void* p1, struct S_P p2) { }
EXPORT void f9_V_SPS_II(struct S_II p0, void* p1, struct S_II p2) { }
EXPORT void f9_V_SPS_IF(struct S_IF p0, void* p1, struct S_IF p2) { }
EXPORT void f9_V_SPS_ID(struct S_ID p0, void* p1, struct S_ID p2) { }
EXPORT void f9_V_SPS_IP(struct S_IP p0, void* p1, struct S_IP p2) { }
EXPORT void f9_V_SPS_FI(struct S_FI p0, void* p1, struct S_FI p2) { }
EXPORT void f9_V_SPS_FF(struct S_FF p0, void* p1, struct S_FF p2) { }
EXPORT void f9_V_SPS_FD(struct S_FD p0, void* p1, struct S_FD p2) { }
EXPORT void f9_V_SPS_FP(struct S_FP p0, void* p1, struct S_FP p2) { }
EXPORT void f9_V_SPS_DI(struct S_DI p0, void* p1, struct S_DI p2) { }
EXPORT void f9_V_SPS_DF(struct S_DF p0, void* p1, struct S_DF p2) { }
EXPORT void f9_V_SPS_DD(struct S_DD p0, void* p1, struct S_DD p2) { }
EXPORT void f9_V_SPS_DP(struct S_DP p0, void* p1, struct S_DP p2) { }
EXPORT void f9_V_SPS_PI(struct S_PI p0, void* p1, struct S_PI p2) { }
EXPORT void f9_V_SPS_PF(struct S_PF p0, void* p1, struct S_PF p2) { }
EXPORT void f9_V_SPS_PD(struct S_PD p0, void* p1, struct S_PD p2) { }
EXPORT void f9_V_SPS_PP(struct S_PP p0, void* p1, struct S_PP p2) { }
EXPORT void f9_V_SPS_III(struct S_III p0, void* p1, struct S_III p2) { }
EXPORT void f9_V_SPS_IIF(struct S_IIF p0, void* p1, struct S_IIF p2) { }
EXPORT void f9_V_SPS_IID(struct S_IID p0, void* p1, struct S_IID p2) { }
EXPORT void f9_V_SPS_IIP(struct S_IIP p0, void* p1, struct S_IIP p2) { }
EXPORT void f9_V_SPS_IFI(struct S_IFI p0, void* p1, struct S_IFI p2) { }
EXPORT void f9_V_SPS_IFF(struct S_IFF p0, void* p1, struct S_IFF p2) { }
EXPORT void f9_V_SPS_IFD(struct S_IFD p0, void* p1, struct S_IFD p2) { }
EXPORT void f9_V_SPS_IFP(struct S_IFP p0, void* p1, struct S_IFP p2) { }
EXPORT void f9_V_SPS_IDI(struct S_IDI p0, void* p1, struct S_IDI p2) { }
EXPORT void f9_V_SPS_IDF(struct S_IDF p0, void* p1, struct S_IDF p2) { }
EXPORT void f9_V_SPS_IDD(struct S_IDD p0, void* p1, struct S_IDD p2) { }
EXPORT void f9_V_SPS_IDP(struct S_IDP p0, void* p1, struct S_IDP p2) { }
EXPORT void f9_V_SPS_IPI(struct S_IPI p0, void* p1, struct S_IPI p2) { }
EXPORT void f9_V_SPS_IPF(struct S_IPF p0, void* p1, struct S_IPF p2) { }
EXPORT void f9_V_SPS_IPD(struct S_IPD p0, void* p1, struct S_IPD p2) { }
EXPORT void f9_V_SPS_IPP(struct S_IPP p0, void* p1, struct S_IPP p2) { }
EXPORT void f9_V_SPS_FII(struct S_FII p0, void* p1, struct S_FII p2) { }
EXPORT void f9_V_SPS_FIF(struct S_FIF p0, void* p1, struct S_FIF p2) { }
EXPORT void f9_V_SPS_FID(struct S_FID p0, void* p1, struct S_FID p2) { }
EXPORT void f9_V_SPS_FIP(struct S_FIP p0, void* p1, struct S_FIP p2) { }
EXPORT void f9_V_SPS_FFI(struct S_FFI p0, void* p1, struct S_FFI p2) { }
EXPORT void f9_V_SPS_FFF(struct S_FFF p0, void* p1, struct S_FFF p2) { }
EXPORT void f9_V_SPS_FFD(struct S_FFD p0, void* p1, struct S_FFD p2) { }
EXPORT void f9_V_SPS_FFP(struct S_FFP p0, void* p1, struct S_FFP p2) { }
EXPORT void f9_V_SPS_FDI(struct S_FDI p0, void* p1, struct S_FDI p2) { }
EXPORT void f9_V_SPS_FDF(struct S_FDF p0, void* p1, struct S_FDF p2) { }
EXPORT void f9_V_SPS_FDD(struct S_FDD p0, void* p1, struct S_FDD p2) { }
EXPORT void f9_V_SPS_FDP(struct S_FDP p0, void* p1, struct S_FDP p2) { }
EXPORT void f9_V_SPS_FPI(struct S_FPI p0, void* p1, struct S_FPI p2) { }
EXPORT void f9_V_SPS_FPF(struct S_FPF p0, void* p1, struct S_FPF p2) { }
EXPORT void f9_V_SPS_FPD(struct S_FPD p0, void* p1, struct S_FPD p2) { }
EXPORT void f9_V_SPS_FPP(struct S_FPP p0, void* p1, struct S_FPP p2) { }
EXPORT void f9_V_SPS_DII(struct S_DII p0, void* p1, struct S_DII p2) { }
EXPORT void f9_V_SPS_DIF(struct S_DIF p0, void* p1, struct S_DIF p2) { }
EXPORT void f9_V_SPS_DID(struct S_DID p0, void* p1, struct S_DID p2) { }
EXPORT void f9_V_SPS_DIP(struct S_DIP p0, void* p1, struct S_DIP p2) { }
EXPORT void f9_V_SPS_DFI(struct S_DFI p0, void* p1, struct S_DFI p2) { }
EXPORT void f9_V_SPS_DFF(struct S_DFF p0, void* p1, struct S_DFF p2) { }
EXPORT void f9_V_SPS_DFD(struct S_DFD p0, void* p1, struct S_DFD p2) { }
EXPORT void f9_V_SPS_DFP(struct S_DFP p0, void* p1, struct S_DFP p2) { }
EXPORT void f9_V_SPS_DDI(struct S_DDI p0, void* p1, struct S_DDI p2) { }
EXPORT void f9_V_SPS_DDF(struct S_DDF p0, void* p1, struct S_DDF p2) { }
EXPORT void f9_V_SPS_DDD(struct S_DDD p0, void* p1, struct S_DDD p2) { }
EXPORT void f9_V_SPS_DDP(struct S_DDP p0, void* p1, struct S_DDP p2) { }
EXPORT void f9_V_SPS_DPI(struct S_DPI p0, void* p1, struct S_DPI p2) { }
EXPORT void f9_V_SPS_DPF(struct S_DPF p0, void* p1, struct S_DPF p2) { }
EXPORT void f9_V_SPS_DPD(struct S_DPD p0, void* p1, struct S_DPD p2) { }
EXPORT void f9_V_SPS_DPP(struct S_DPP p0, void* p1, struct S_DPP p2) { }
EXPORT void f9_V_SPS_PII(struct S_PII p0, void* p1, struct S_PII p2) { }
EXPORT void f9_V_SPS_PIF(struct S_PIF p0, void* p1, struct S_PIF p2) { }
EXPORT void f9_V_SPS_PID(struct S_PID p0, void* p1, struct S_PID p2) { }
EXPORT void f9_V_SPS_PIP(struct S_PIP p0, void* p1, struct S_PIP p2) { }
EXPORT void f9_V_SPS_PFI(struct S_PFI p0, void* p1, struct S_PFI p2) { }
EXPORT void f9_V_SPS_PFF(struct S_PFF p0, void* p1, struct S_PFF p2) { }
EXPORT void f9_V_SPS_PFD(struct S_PFD p0, void* p1, struct S_PFD p2) { }
EXPORT void f9_V_SPS_PFP(struct S_PFP p0, void* p1, struct S_PFP p2) { }
EXPORT void f9_V_SPS_PDI(struct S_PDI p0, void* p1, struct S_PDI p2) { }
EXPORT void f9_V_SPS_PDF(struct S_PDF p0, void* p1, struct S_PDF p2) { }
EXPORT void f9_V_SPS_PDD(struct S_PDD p0, void* p1, struct S_PDD p2) { }
EXPORT void f9_V_SPS_PDP(struct S_PDP p0, void* p1, struct S_PDP p2) { }
EXPORT void f9_V_SPS_PPI(struct S_PPI p0, void* p1, struct S_PPI p2) { }
EXPORT void f9_V_SPS_PPF(struct S_PPF p0, void* p1, struct S_PPF p2) { }
EXPORT void f9_V_SPS_PPD(struct S_PPD p0, void* p1, struct S_PPD p2) { }
EXPORT void f9_V_SPS_PPP(struct S_PPP p0, void* p1, struct S_PPP p2) { }
EXPORT void f9_V_SSI_I(struct S_I p0, struct S_I p1, int p2) { }
EXPORT void f9_V_SSI_F(struct S_F p0, struct S_F p1, int p2) { }
EXPORT void f9_V_SSI_D(struct S_D p0, struct S_D p1, int p2) { }
EXPORT void f9_V_SSI_P(struct S_P p0, struct S_P p1, int p2) { }
EXPORT void f9_V_SSI_II(struct S_II p0, struct S_II p1, int p2) { }
EXPORT void f9_V_SSI_IF(struct S_IF p0, struct S_IF p1, int p2) { }
EXPORT void f9_V_SSI_ID(struct S_ID p0, struct S_ID p1, int p2) { }
EXPORT void f9_V_SSI_IP(struct S_IP p0, struct S_IP p1, int p2) { }
EXPORT void f9_V_SSI_FI(struct S_FI p0, struct S_FI p1, int p2) { }
EXPORT void f9_V_SSI_FF(struct S_FF p0, struct S_FF p1, int p2) { }
EXPORT void f9_V_SSI_FD(struct S_FD p0, struct S_FD p1, int p2) { }
EXPORT void f9_V_SSI_FP(struct S_FP p0, struct S_FP p1, int p2) { }
EXPORT void f9_V_SSI_DI(struct S_DI p0, struct S_DI p1, int p2) { }
EXPORT void f9_V_SSI_DF(struct S_DF p0, struct S_DF p1, int p2) { }
EXPORT void f9_V_SSI_DD(struct S_DD p0, struct S_DD p1, int p2) { }
EXPORT void f9_V_SSI_DP(struct S_DP p0, struct S_DP p1, int p2) { }
EXPORT void f9_V_SSI_PI(struct S_PI p0, struct S_PI p1, int p2) { }
EXPORT void f9_V_SSI_PF(struct S_PF p0, struct S_PF p1, int p2) { }
EXPORT void f9_V_SSI_PD(struct S_PD p0, struct S_PD p1, int p2) { }
EXPORT void f9_V_SSI_PP(struct S_PP p0, struct S_PP p1, int p2) { }
EXPORT void f9_V_SSI_III(struct S_III p0, struct S_III p1, int p2) { }
EXPORT void f9_V_SSI_IIF(struct S_IIF p0, struct S_IIF p1, int p2) { }
EXPORT void f9_V_SSI_IID(struct S_IID p0, struct S_IID p1, int p2) { }
EXPORT void f9_V_SSI_IIP(struct S_IIP p0, struct S_IIP p1, int p2) { }
EXPORT void f9_V_SSI_IFI(struct S_IFI p0, struct S_IFI p1, int p2) { }
EXPORT void f9_V_SSI_IFF(struct S_IFF p0, struct S_IFF p1, int p2) { }
EXPORT void f9_V_SSI_IFD(struct S_IFD p0, struct S_IFD p1, int p2) { }
EXPORT void f9_V_SSI_IFP(struct S_IFP p0, struct S_IFP p1, int p2) { }
EXPORT void f9_V_SSI_IDI(struct S_IDI p0, struct S_IDI p1, int p2) { }
EXPORT void f9_V_SSI_IDF(struct S_IDF p0, struct S_IDF p1, int p2) { }
EXPORT void f9_V_SSI_IDD(struct S_IDD p0, struct S_IDD p1, int p2) { }
EXPORT void f9_V_SSI_IDP(struct S_IDP p0, struct S_IDP p1, int p2) { }
EXPORT void f9_V_SSI_IPI(struct S_IPI p0, struct S_IPI p1, int p2) { }
EXPORT void f9_V_SSI_IPF(struct S_IPF p0, struct S_IPF p1, int p2) { }
EXPORT void f9_V_SSI_IPD(struct S_IPD p0, struct S_IPD p1, int p2) { }
EXPORT void f9_V_SSI_IPP(struct S_IPP p0, struct S_IPP p1, int p2) { }
EXPORT void f9_V_SSI_FII(struct S_FII p0, struct S_FII p1, int p2) { }
EXPORT void f9_V_SSI_FIF(struct S_FIF p0, struct S_FIF p1, int p2) { }
EXPORT void f9_V_SSI_FID(struct S_FID p0, struct S_FID p1, int p2) { }
EXPORT void f9_V_SSI_FIP(struct S_FIP p0, struct S_FIP p1, int p2) { }
EXPORT void f9_V_SSI_FFI(struct S_FFI p0, struct S_FFI p1, int p2) { }
EXPORT void f9_V_SSI_FFF(struct S_FFF p0, struct S_FFF p1, int p2) { }
EXPORT void f9_V_SSI_FFD(struct S_FFD p0, struct S_FFD p1, int p2) { }
EXPORT void f9_V_SSI_FFP(struct S_FFP p0, struct S_FFP p1, int p2) { }
EXPORT void f9_V_SSI_FDI(struct S_FDI p0, struct S_FDI p1, int p2) { }
EXPORT void f9_V_SSI_FDF(struct S_FDF p0, struct S_FDF p1, int p2) { }
EXPORT void f9_V_SSI_FDD(struct S_FDD p0, struct S_FDD p1, int p2) { }
EXPORT void f9_V_SSI_FDP(struct S_FDP p0, struct S_FDP p1, int p2) { }
EXPORT void f9_V_SSI_FPI(struct S_FPI p0, struct S_FPI p1, int p2) { }
EXPORT void f9_V_SSI_FPF(struct S_FPF p0, struct S_FPF p1, int p2) { }
EXPORT void f9_V_SSI_FPD(struct S_FPD p0, struct S_FPD p1, int p2) { }
EXPORT void f9_V_SSI_FPP(struct S_FPP p0, struct S_FPP p1, int p2) { }
EXPORT void f9_V_SSI_DII(struct S_DII p0, struct S_DII p1, int p2) { }
EXPORT void f9_V_SSI_DIF(struct S_DIF p0, struct S_DIF p1, int p2) { }
EXPORT void f9_V_SSI_DID(struct S_DID p0, struct S_DID p1, int p2) { }
EXPORT void f9_V_SSI_DIP(struct S_DIP p0, struct S_DIP p1, int p2) { }
EXPORT void f9_V_SSI_DFI(struct S_DFI p0, struct S_DFI p1, int p2) { }
EXPORT void f9_V_SSI_DFF(struct S_DFF p0, struct S_DFF p1, int p2) { }
EXPORT void f9_V_SSI_DFD(struct S_DFD p0, struct S_DFD p1, int p2) { }
EXPORT void f9_V_SSI_DFP(struct S_DFP p0, struct S_DFP p1, int p2) { }
EXPORT void f9_V_SSI_DDI(struct S_DDI p0, struct S_DDI p1, int p2) { }
EXPORT void f9_V_SSI_DDF(struct S_DDF p0, struct S_DDF p1, int p2) { }
EXPORT void f9_V_SSI_DDD(struct S_DDD p0, struct S_DDD p1, int p2) { }
EXPORT void f9_V_SSI_DDP(struct S_DDP p0, struct S_DDP p1, int p2) { }
EXPORT void f9_V_SSI_DPI(struct S_DPI p0, struct S_DPI p1, int p2) { }
EXPORT void f9_V_SSI_DPF(struct S_DPF p0, struct S_DPF p1, int p2) { }
EXPORT void f9_V_SSI_DPD(struct S_DPD p0, struct S_DPD p1, int p2) { }
EXPORT void f9_V_SSI_DPP(struct S_DPP p0, struct S_DPP p1, int p2) { }
EXPORT void f9_V_SSI_PII(struct S_PII p0, struct S_PII p1, int p2) { }
EXPORT void f9_V_SSI_PIF(struct S_PIF p0, struct S_PIF p1, int p2) { }
EXPORT void f9_V_SSI_PID(struct S_PID p0, struct S_PID p1, int p2) { }
EXPORT void f9_V_SSI_PIP(struct S_PIP p0, struct S_PIP p1, int p2) { }
EXPORT void f9_V_SSI_PFI(struct S_PFI p0, struct S_PFI p1, int p2) { }
EXPORT void f9_V_SSI_PFF(struct S_PFF p0, struct S_PFF p1, int p2) { }
EXPORT void f9_V_SSI_PFD(struct S_PFD p0, struct S_PFD p1, int p2) { }
EXPORT void f9_V_SSI_PFP(struct S_PFP p0, struct S_PFP p1, int p2) { }
EXPORT void f9_V_SSI_PDI(struct S_PDI p0, struct S_PDI p1, int p2) { }
EXPORT void f9_V_SSI_PDF(struct S_PDF p0, struct S_PDF p1, int p2) { }
EXPORT void f9_V_SSI_PDD(struct S_PDD p0, struct S_PDD p1, int p2) { }
EXPORT void f9_V_SSI_PDP(struct S_PDP p0, struct S_PDP p1, int p2) { }
EXPORT void f9_V_SSI_PPI(struct S_PPI p0, struct S_PPI p1, int p2) { }
EXPORT void f9_V_SSI_PPF(struct S_PPF p0, struct S_PPF p1, int p2) { }
EXPORT void f9_V_SSI_PPD(struct S_PPD p0, struct S_PPD p1, int p2) { }
EXPORT void f9_V_SSI_PPP(struct S_PPP p0, struct S_PPP p1, int p2) { }
EXPORT void f9_V_SSF_I(struct S_I p0, struct S_I p1, float p2) { }
EXPORT void f9_V_SSF_F(struct S_F p0, struct S_F p1, float p2) { }
EXPORT void f9_V_SSF_D(struct S_D p0, struct S_D p1, float p2) { }
EXPORT void f9_V_SSF_P(struct S_P p0, struct S_P p1, float p2) { }
EXPORT void f9_V_SSF_II(struct S_II p0, struct S_II p1, float p2) { }
EXPORT void f9_V_SSF_IF(struct S_IF p0, struct S_IF p1, float p2) { }
EXPORT void f9_V_SSF_ID(struct S_ID p0, struct S_ID p1, float p2) { }
EXPORT void f9_V_SSF_IP(struct S_IP p0, struct S_IP p1, float p2) { }
EXPORT void f9_V_SSF_FI(struct S_FI p0, struct S_FI p1, float p2) { }
EXPORT void f9_V_SSF_FF(struct S_FF p0, struct S_FF p1, float p2) { }
EXPORT void f9_V_SSF_FD(struct S_FD p0, struct S_FD p1, float p2) { }
EXPORT void f9_V_SSF_FP(struct S_FP p0, struct S_FP p1, float p2) { }
EXPORT void f9_V_SSF_DI(struct S_DI p0, struct S_DI p1, float p2) { }
EXPORT void f9_V_SSF_DF(struct S_DF p0, struct S_DF p1, float p2) { }
EXPORT void f9_V_SSF_DD(struct S_DD p0, struct S_DD p1, float p2) { }
EXPORT void f9_V_SSF_DP(struct S_DP p0, struct S_DP p1, float p2) { }
EXPORT void f9_V_SSF_PI(struct S_PI p0, struct S_PI p1, float p2) { }
EXPORT void f9_V_SSF_PF(struct S_PF p0, struct S_PF p1, float p2) { }
EXPORT void f9_V_SSF_PD(struct S_PD p0, struct S_PD p1, float p2) { }
EXPORT void f9_V_SSF_PP(struct S_PP p0, struct S_PP p1, float p2) { }
EXPORT void f9_V_SSF_III(struct S_III p0, struct S_III p1, float p2) { }
EXPORT void f9_V_SSF_IIF(struct S_IIF p0, struct S_IIF p1, float p2) { }
EXPORT void f9_V_SSF_IID(struct S_IID p0, struct S_IID p1, float p2) { }
EXPORT void f9_V_SSF_IIP(struct S_IIP p0, struct S_IIP p1, float p2) { }
EXPORT void f9_V_SSF_IFI(struct S_IFI p0, struct S_IFI p1, float p2) { }
EXPORT void f9_V_SSF_IFF(struct S_IFF p0, struct S_IFF p1, float p2) { }
EXPORT void f9_V_SSF_IFD(struct S_IFD p0, struct S_IFD p1, float p2) { }
EXPORT void f9_V_SSF_IFP(struct S_IFP p0, struct S_IFP p1, float p2) { }
EXPORT void f9_V_SSF_IDI(struct S_IDI p0, struct S_IDI p1, float p2) { }
EXPORT void f9_V_SSF_IDF(struct S_IDF p0, struct S_IDF p1, float p2) { }
EXPORT void f9_V_SSF_IDD(struct S_IDD p0, struct S_IDD p1, float p2) { }
EXPORT void f9_V_SSF_IDP(struct S_IDP p0, struct S_IDP p1, float p2) { }
EXPORT void f9_V_SSF_IPI(struct S_IPI p0, struct S_IPI p1, float p2) { }
EXPORT void f9_V_SSF_IPF(struct S_IPF p0, struct S_IPF p1, float p2) { }
EXPORT void f9_V_SSF_IPD(struct S_IPD p0, struct S_IPD p1, float p2) { }
EXPORT void f9_V_SSF_IPP(struct S_IPP p0, struct S_IPP p1, float p2) { }
EXPORT void f9_V_SSF_FII(struct S_FII p0, struct S_FII p1, float p2) { }
EXPORT void f9_V_SSF_FIF(struct S_FIF p0, struct S_FIF p1, float p2) { }
EXPORT void f9_V_SSF_FID(struct S_FID p0, struct S_FID p1, float p2) { }
EXPORT void f9_V_SSF_FIP(struct S_FIP p0, struct S_FIP p1, float p2) { }
EXPORT void f9_V_SSF_FFI(struct S_FFI p0, struct S_FFI p1, float p2) { }
EXPORT void f9_V_SSF_FFF(struct S_FFF p0, struct S_FFF p1, float p2) { }
EXPORT void f9_V_SSF_FFD(struct S_FFD p0, struct S_FFD p1, float p2) { }
EXPORT void f9_V_SSF_FFP(struct S_FFP p0, struct S_FFP p1, float p2) { }
EXPORT void f9_V_SSF_FDI(struct S_FDI p0, struct S_FDI p1, float p2) { }
EXPORT void f9_V_SSF_FDF(struct S_FDF p0, struct S_FDF p1, float p2) { }
EXPORT void f9_V_SSF_FDD(struct S_FDD p0, struct S_FDD p1, float p2) { }
EXPORT void f9_V_SSF_FDP(struct S_FDP p0, struct S_FDP p1, float p2) { }
EXPORT void f9_V_SSF_FPI(struct S_FPI p0, struct S_FPI p1, float p2) { }
EXPORT void f9_V_SSF_FPF(struct S_FPF p0, struct S_FPF p1, float p2) { }
EXPORT void f9_V_SSF_FPD(struct S_FPD p0, struct S_FPD p1, float p2) { }
EXPORT void f9_V_SSF_FPP(struct S_FPP p0, struct S_FPP p1, float p2) { }
EXPORT void f9_V_SSF_DII(struct S_DII p0, struct S_DII p1, float p2) { }
EXPORT void f9_V_SSF_DIF(struct S_DIF p0, struct S_DIF p1, float p2) { }
EXPORT void f9_V_SSF_DID(struct S_DID p0, struct S_DID p1, float p2) { }
EXPORT void f9_V_SSF_DIP(struct S_DIP p0, struct S_DIP p1, float p2) { }
EXPORT void f9_V_SSF_DFI(struct S_DFI p0, struct S_DFI p1, float p2) { }
EXPORT void f9_V_SSF_DFF(struct S_DFF p0, struct S_DFF p1, float p2) { }
EXPORT void f9_V_SSF_DFD(struct S_DFD p0, struct S_DFD p1, float p2) { }
EXPORT void f9_V_SSF_DFP(struct S_DFP p0, struct S_DFP p1, float p2) { }
EXPORT void f9_V_SSF_DDI(struct S_DDI p0, struct S_DDI p1, float p2) { }
EXPORT void f9_V_SSF_DDF(struct S_DDF p0, struct S_DDF p1, float p2) { }
EXPORT void f9_V_SSF_DDD(struct S_DDD p0, struct S_DDD p1, float p2) { }
EXPORT void f9_V_SSF_DDP(struct S_DDP p0, struct S_DDP p1, float p2) { }
EXPORT void f9_V_SSF_DPI(struct S_DPI p0, struct S_DPI p1, float p2) { }
EXPORT void f9_V_SSF_DPF(struct S_DPF p0, struct S_DPF p1, float p2) { }
EXPORT void f9_V_SSF_DPD(struct S_DPD p0, struct S_DPD p1, float p2) { }
EXPORT void f9_V_SSF_DPP(struct S_DPP p0, struct S_DPP p1, float p2) { }
EXPORT void f9_V_SSF_PII(struct S_PII p0, struct S_PII p1, float p2) { }
EXPORT void f9_V_SSF_PIF(struct S_PIF p0, struct S_PIF p1, float p2) { }
EXPORT void f9_V_SSF_PID(struct S_PID p0, struct S_PID p1, float p2) { }
EXPORT void f9_V_SSF_PIP(struct S_PIP p0, struct S_PIP p1, float p2) { }
EXPORT void f9_V_SSF_PFI(struct S_PFI p0, struct S_PFI p1, float p2) { }
EXPORT void f9_V_SSF_PFF(struct S_PFF p0, struct S_PFF p1, float p2) { }
EXPORT void f9_V_SSF_PFD(struct S_PFD p0, struct S_PFD p1, float p2) { }
EXPORT void f9_V_SSF_PFP(struct S_PFP p0, struct S_PFP p1, float p2) { }
EXPORT void f9_V_SSF_PDI(struct S_PDI p0, struct S_PDI p1, float p2) { }
EXPORT void f9_V_SSF_PDF(struct S_PDF p0, struct S_PDF p1, float p2) { }
EXPORT void f9_V_SSF_PDD(struct S_PDD p0, struct S_PDD p1, float p2) { }
EXPORT void f9_V_SSF_PDP(struct S_PDP p0, struct S_PDP p1, float p2) { }
EXPORT void f9_V_SSF_PPI(struct S_PPI p0, struct S_PPI p1, float p2) { }
EXPORT void f9_V_SSF_PPF(struct S_PPF p0, struct S_PPF p1, float p2) { }
EXPORT void f9_V_SSF_PPD(struct S_PPD p0, struct S_PPD p1, float p2) { }
EXPORT void f9_V_SSF_PPP(struct S_PPP p0, struct S_PPP p1, float p2) { }
EXPORT void f9_V_SSD_I(struct S_I p0, struct S_I p1, double p2) { }
EXPORT void f9_V_SSD_F(struct S_F p0, struct S_F p1, double p2) { }
EXPORT void f9_V_SSD_D(struct S_D p0, struct S_D p1, double p2) { }
EXPORT void f9_V_SSD_P(struct S_P p0, struct S_P p1, double p2) { }
EXPORT void f9_V_SSD_II(struct S_II p0, struct S_II p1, double p2) { }
EXPORT void f9_V_SSD_IF(struct S_IF p0, struct S_IF p1, double p2) { }
EXPORT void f9_V_SSD_ID(struct S_ID p0, struct S_ID p1, double p2) { }
EXPORT void f9_V_SSD_IP(struct S_IP p0, struct S_IP p1, double p2) { }
EXPORT void f9_V_SSD_FI(struct S_FI p0, struct S_FI p1, double p2) { }
EXPORT void f9_V_SSD_FF(struct S_FF p0, struct S_FF p1, double p2) { }
EXPORT void f9_V_SSD_FD(struct S_FD p0, struct S_FD p1, double p2) { }
EXPORT void f9_V_SSD_FP(struct S_FP p0, struct S_FP p1, double p2) { }
EXPORT void f9_V_SSD_DI(struct S_DI p0, struct S_DI p1, double p2) { }
EXPORT void f9_V_SSD_DF(struct S_DF p0, struct S_DF p1, double p2) { }
EXPORT void f9_V_SSD_DD(struct S_DD p0, struct S_DD p1, double p2) { }
EXPORT void f9_V_SSD_DP(struct S_DP p0, struct S_DP p1, double p2) { }
EXPORT void f9_V_SSD_PI(struct S_PI p0, struct S_PI p1, double p2) { }
EXPORT void f9_V_SSD_PF(struct S_PF p0, struct S_PF p1, double p2) { }
EXPORT void f9_V_SSD_PD(struct S_PD p0, struct S_PD p1, double p2) { }
EXPORT void f9_V_SSD_PP(struct S_PP p0, struct S_PP p1, double p2) { }
EXPORT void f9_V_SSD_III(struct S_III p0, struct S_III p1, double p2) { }
EXPORT void f9_V_SSD_IIF(struct S_IIF p0, struct S_IIF p1, double p2) { }
EXPORT void f9_V_SSD_IID(struct S_IID p0, struct S_IID p1, double p2) { }
EXPORT void f9_V_SSD_IIP(struct S_IIP p0, struct S_IIP p1, double p2) { }
EXPORT void f9_V_SSD_IFI(struct S_IFI p0, struct S_IFI p1, double p2) { }
EXPORT void f9_V_SSD_IFF(struct S_IFF p0, struct S_IFF p1, double p2) { }
EXPORT void f9_V_SSD_IFD(struct S_IFD p0, struct S_IFD p1, double p2) { }
EXPORT void f9_V_SSD_IFP(struct S_IFP p0, struct S_IFP p1, double p2) { }
EXPORT void f9_V_SSD_IDI(struct S_IDI p0, struct S_IDI p1, double p2) { }
EXPORT void f9_V_SSD_IDF(struct S_IDF p0, struct S_IDF p1, double p2) { }
EXPORT void f9_V_SSD_IDD(struct S_IDD p0, struct S_IDD p1, double p2) { }
EXPORT void f9_V_SSD_IDP(struct S_IDP p0, struct S_IDP p1, double p2) { }
EXPORT void f9_V_SSD_IPI(struct S_IPI p0, struct S_IPI p1, double p2) { }
EXPORT void f9_V_SSD_IPF(struct S_IPF p0, struct S_IPF p1, double p2) { }
EXPORT void f9_V_SSD_IPD(struct S_IPD p0, struct S_IPD p1, double p2) { }
EXPORT void f9_V_SSD_IPP(struct S_IPP p0, struct S_IPP p1, double p2) { }
EXPORT void f9_V_SSD_FII(struct S_FII p0, struct S_FII p1, double p2) { }
EXPORT void f9_V_SSD_FIF(struct S_FIF p0, struct S_FIF p1, double p2) { }
EXPORT void f9_V_SSD_FID(struct S_FID p0, struct S_FID p1, double p2) { }
EXPORT void f9_V_SSD_FIP(struct S_FIP p0, struct S_FIP p1, double p2) { }
EXPORT void f9_V_SSD_FFI(struct S_FFI p0, struct S_FFI p1, double p2) { }
EXPORT void f9_V_SSD_FFF(struct S_FFF p0, struct S_FFF p1, double p2) { }
EXPORT void f9_V_SSD_FFD(struct S_FFD p0, struct S_FFD p1, double p2) { }
EXPORT void f9_V_SSD_FFP(struct S_FFP p0, struct S_FFP p1, double p2) { }
EXPORT void f9_V_SSD_FDI(struct S_FDI p0, struct S_FDI p1, double p2) { }
EXPORT void f9_V_SSD_FDF(struct S_FDF p0, struct S_FDF p1, double p2) { }
EXPORT void f9_V_SSD_FDD(struct S_FDD p0, struct S_FDD p1, double p2) { }
EXPORT void f9_V_SSD_FDP(struct S_FDP p0, struct S_FDP p1, double p2) { }
EXPORT void f9_V_SSD_FPI(struct S_FPI p0, struct S_FPI p1, double p2) { }
EXPORT void f9_V_SSD_FPF(struct S_FPF p0, struct S_FPF p1, double p2) { }
EXPORT void f9_V_SSD_FPD(struct S_FPD p0, struct S_FPD p1, double p2) { }
EXPORT void f9_V_SSD_FPP(struct S_FPP p0, struct S_FPP p1, double p2) { }
EXPORT void f9_V_SSD_DII(struct S_DII p0, struct S_DII p1, double p2) { }
EXPORT void f9_V_SSD_DIF(struct S_DIF p0, struct S_DIF p1, double p2) { }
EXPORT void f9_V_SSD_DID(struct S_DID p0, struct S_DID p1, double p2) { }
EXPORT void f9_V_SSD_DIP(struct S_DIP p0, struct S_DIP p1, double p2) { }
EXPORT void f9_V_SSD_DFI(struct S_DFI p0, struct S_DFI p1, double p2) { }
EXPORT void f9_V_SSD_DFF(struct S_DFF p0, struct S_DFF p1, double p2) { }
EXPORT void f9_V_SSD_DFD(struct S_DFD p0, struct S_DFD p1, double p2) { }
EXPORT void f9_V_SSD_DFP(struct S_DFP p0, struct S_DFP p1, double p2) { }
EXPORT void f9_V_SSD_DDI(struct S_DDI p0, struct S_DDI p1, double p2) { }
EXPORT void f9_V_SSD_DDF(struct S_DDF p0, struct S_DDF p1, double p2) { }
EXPORT void f9_V_SSD_DDD(struct S_DDD p0, struct S_DDD p1, double p2) { }
EXPORT void f9_V_SSD_DDP(struct S_DDP p0, struct S_DDP p1, double p2) { }
EXPORT void f9_V_SSD_DPI(struct S_DPI p0, struct S_DPI p1, double p2) { }
EXPORT void f9_V_SSD_DPF(struct S_DPF p0, struct S_DPF p1, double p2) { }
EXPORT void f9_V_SSD_DPD(struct S_DPD p0, struct S_DPD p1, double p2) { }
EXPORT void f9_V_SSD_DPP(struct S_DPP p0, struct S_DPP p1, double p2) { }
EXPORT void f9_V_SSD_PII(struct S_PII p0, struct S_PII p1, double p2) { }
EXPORT void f9_V_SSD_PIF(struct S_PIF p0, struct S_PIF p1, double p2) { }
EXPORT void f9_V_SSD_PID(struct S_PID p0, struct S_PID p1, double p2) { }
EXPORT void f9_V_SSD_PIP(struct S_PIP p0, struct S_PIP p1, double p2) { }
EXPORT void f9_V_SSD_PFI(struct S_PFI p0, struct S_PFI p1, double p2) { }
EXPORT void f9_V_SSD_PFF(struct S_PFF p0, struct S_PFF p1, double p2) { }
EXPORT void f9_V_SSD_PFD(struct S_PFD p0, struct S_PFD p1, double p2) { }
EXPORT void f9_V_SSD_PFP(struct S_PFP p0, struct S_PFP p1, double p2) { }
EXPORT void f9_V_SSD_PDI(struct S_PDI p0, struct S_PDI p1, double p2) { }
EXPORT void f9_V_SSD_PDF(struct S_PDF p0, struct S_PDF p1, double p2) { }
EXPORT void f9_V_SSD_PDD(struct S_PDD p0, struct S_PDD p1, double p2) { }
EXPORT void f9_V_SSD_PDP(struct S_PDP p0, struct S_PDP p1, double p2) { }
EXPORT void f9_V_SSD_PPI(struct S_PPI p0, struct S_PPI p1, double p2) { }
EXPORT void f9_V_SSD_PPF(struct S_PPF p0, struct S_PPF p1, double p2) { }
EXPORT void f9_V_SSD_PPD(struct S_PPD p0, struct S_PPD p1, double p2) { }
EXPORT void f9_V_SSD_PPP(struct S_PPP p0, struct S_PPP p1, double p2) { }
EXPORT void f9_V_SSP_I(struct S_I p0, struct S_I p1, void* p2) { }
EXPORT void f9_V_SSP_F(struct S_F p0, struct S_F p1, void* p2) { }
EXPORT void f9_V_SSP_D(struct S_D p0, struct S_D p1, void* p2) { }
EXPORT void f9_V_SSP_P(struct S_P p0, struct S_P p1, void* p2) { }
EXPORT void f9_V_SSP_II(struct S_II p0, struct S_II p1, void* p2) { }
EXPORT void f9_V_SSP_IF(struct S_IF p0, struct S_IF p1, void* p2) { }
EXPORT void f9_V_SSP_ID(struct S_ID p0, struct S_ID p1, void* p2) { }
EXPORT void f9_V_SSP_IP(struct S_IP p0, struct S_IP p1, void* p2) { }
EXPORT void f9_V_SSP_FI(struct S_FI p0, struct S_FI p1, void* p2) { }
EXPORT void f9_V_SSP_FF(struct S_FF p0, struct S_FF p1, void* p2) { }
EXPORT void f9_V_SSP_FD(struct S_FD p0, struct S_FD p1, void* p2) { }
EXPORT void f9_V_SSP_FP(struct S_FP p0, struct S_FP p1, void* p2) { }
EXPORT void f9_V_SSP_DI(struct S_DI p0, struct S_DI p1, void* p2) { }
EXPORT void f9_V_SSP_DF(struct S_DF p0, struct S_DF p1, void* p2) { }
EXPORT void f9_V_SSP_DD(struct S_DD p0, struct S_DD p1, void* p2) { }
EXPORT void f9_V_SSP_DP(struct S_DP p0, struct S_DP p1, void* p2) { }
EXPORT void f9_V_SSP_PI(struct S_PI p0, struct S_PI p1, void* p2) { }
EXPORT void f9_V_SSP_PF(struct S_PF p0, struct S_PF p1, void* p2) { }
EXPORT void f9_V_SSP_PD(struct S_PD p0, struct S_PD p1, void* p2) { }
EXPORT void f9_V_SSP_PP(struct S_PP p0, struct S_PP p1, void* p2) { }
EXPORT void f9_V_SSP_III(struct S_III p0, struct S_III p1, void* p2) { }
EXPORT void f9_V_SSP_IIF(struct S_IIF p0, struct S_IIF p1, void* p2) { }
EXPORT void f9_V_SSP_IID(struct S_IID p0, struct S_IID p1, void* p2) { }
EXPORT void f9_V_SSP_IIP(struct S_IIP p0, struct S_IIP p1, void* p2) { }
EXPORT void f9_V_SSP_IFI(struct S_IFI p0, struct S_IFI p1, void* p2) { }
EXPORT void f9_V_SSP_IFF(struct S_IFF p0, struct S_IFF p1, void* p2) { }
EXPORT void f9_V_SSP_IFD(struct S_IFD p0, struct S_IFD p1, void* p2) { }
EXPORT void f9_V_SSP_IFP(struct S_IFP p0, struct S_IFP p1, void* p2) { }
EXPORT void f9_V_SSP_IDI(struct S_IDI p0, struct S_IDI p1, void* p2) { }
EXPORT void f9_V_SSP_IDF(struct S_IDF p0, struct S_IDF p1, void* p2) { }
EXPORT void f9_V_SSP_IDD(struct S_IDD p0, struct S_IDD p1, void* p2) { }
EXPORT void f9_V_SSP_IDP(struct S_IDP p0, struct S_IDP p1, void* p2) { }
EXPORT void f9_V_SSP_IPI(struct S_IPI p0, struct S_IPI p1, void* p2) { }
EXPORT void f9_V_SSP_IPF(struct S_IPF p0, struct S_IPF p1, void* p2) { }
EXPORT void f9_V_SSP_IPD(struct S_IPD p0, struct S_IPD p1, void* p2) { }
EXPORT void f9_V_SSP_IPP(struct S_IPP p0, struct S_IPP p1, void* p2) { }
EXPORT void f9_V_SSP_FII(struct S_FII p0, struct S_FII p1, void* p2) { }
EXPORT void f9_V_SSP_FIF(struct S_FIF p0, struct S_FIF p1, void* p2) { }
EXPORT void f9_V_SSP_FID(struct S_FID p0, struct S_FID p1, void* p2) { }
EXPORT void f9_V_SSP_FIP(struct S_FIP p0, struct S_FIP p1, void* p2) { }
EXPORT void f9_V_SSP_FFI(struct S_FFI p0, struct S_FFI p1, void* p2) { }
EXPORT void f9_V_SSP_FFF(struct S_FFF p0, struct S_FFF p1, void* p2) { }
EXPORT void f9_V_SSP_FFD(struct S_FFD p0, struct S_FFD p1, void* p2) { }
EXPORT void f9_V_SSP_FFP(struct S_FFP p0, struct S_FFP p1, void* p2) { }
EXPORT void f9_V_SSP_FDI(struct S_FDI p0, struct S_FDI p1, void* p2) { }
EXPORT void f9_V_SSP_FDF(struct S_FDF p0, struct S_FDF p1, void* p2) { }
EXPORT void f9_V_SSP_FDD(struct S_FDD p0, struct S_FDD p1, void* p2) { }
EXPORT void f9_V_SSP_FDP(struct S_FDP p0, struct S_FDP p1, void* p2) { }
EXPORT void f9_V_SSP_FPI(struct S_FPI p0, struct S_FPI p1, void* p2) { }
EXPORT void f9_V_SSP_FPF(struct S_FPF p0, struct S_FPF p1, void* p2) { }
EXPORT void f9_V_SSP_FPD(struct S_FPD p0, struct S_FPD p1, void* p2) { }
EXPORT void f9_V_SSP_FPP(struct S_FPP p0, struct S_FPP p1, void* p2) { }
EXPORT void f9_V_SSP_DII(struct S_DII p0, struct S_DII p1, void* p2) { }
EXPORT void f9_V_SSP_DIF(struct S_DIF p0, struct S_DIF p1, void* p2) { }
EXPORT void f9_V_SSP_DID(struct S_DID p0, struct S_DID p1, void* p2) { }
EXPORT void f9_V_SSP_DIP(struct S_DIP p0, struct S_DIP p1, void* p2) { }
EXPORT void f9_V_SSP_DFI(struct S_DFI p0, struct S_DFI p1, void* p2) { }
EXPORT void f9_V_SSP_DFF(struct S_DFF p0, struct S_DFF p1, void* p2) { }
EXPORT void f9_V_SSP_DFD(struct S_DFD p0, struct S_DFD p1, void* p2) { }
EXPORT void f9_V_SSP_DFP(struct S_DFP p0, struct S_DFP p1, void* p2) { }
EXPORT void f9_V_SSP_DDI(struct S_DDI p0, struct S_DDI p1, void* p2) { }
EXPORT void f9_V_SSP_DDF(struct S_DDF p0, struct S_DDF p1, void* p2) { }
EXPORT void f9_V_SSP_DDD(struct S_DDD p0, struct S_DDD p1, void* p2) { }
EXPORT void f9_V_SSP_DDP(struct S_DDP p0, struct S_DDP p1, void* p2) { }
EXPORT void f9_V_SSP_DPI(struct S_DPI p0, struct S_DPI p1, void* p2) { }
EXPORT void f9_V_SSP_DPF(struct S_DPF p0, struct S_DPF p1, void* p2) { }
EXPORT void f9_V_SSP_DPD(struct S_DPD p0, struct S_DPD p1, void* p2) { }
EXPORT void f9_V_SSP_DPP(struct S_DPP p0, struct S_DPP p1, void* p2) { }
EXPORT void f9_V_SSP_PII(struct S_PII p0, struct S_PII p1, void* p2) { }
EXPORT void f9_V_SSP_PIF(struct S_PIF p0, struct S_PIF p1, void* p2) { }
EXPORT void f9_V_SSP_PID(struct S_PID p0, struct S_PID p1, void* p2) { }
EXPORT void f9_V_SSP_PIP(struct S_PIP p0, struct S_PIP p1, void* p2) { }
EXPORT void f9_V_SSP_PFI(struct S_PFI p0, struct S_PFI p1, void* p2) { }
EXPORT void f9_V_SSP_PFF(struct S_PFF p0, struct S_PFF p1, void* p2) { }
EXPORT void f9_V_SSP_PFD(struct S_PFD p0, struct S_PFD p1, void* p2) { }
EXPORT void f9_V_SSP_PFP(struct S_PFP p0, struct S_PFP p1, void* p2) { }
EXPORT void f9_V_SSP_PDI(struct S_PDI p0, struct S_PDI p1, void* p2) { }
EXPORT void f9_V_SSP_PDF(struct S_PDF p0, struct S_PDF p1, void* p2) { }
EXPORT void f9_V_SSP_PDD(struct S_PDD p0, struct S_PDD p1, void* p2) { }
EXPORT void f9_V_SSP_PDP(struct S_PDP p0, struct S_PDP p1, void* p2) { }
EXPORT void f9_V_SSP_PPI(struct S_PPI p0, struct S_PPI p1, void* p2) { }
EXPORT void f9_V_SSP_PPF(struct S_PPF p0, struct S_PPF p1, void* p2) { }
EXPORT void f9_V_SSP_PPD(struct S_PPD p0, struct S_PPD p1, void* p2) { }
EXPORT void f9_V_SSP_PPP(struct S_PPP p0, struct S_PPP p1, void* p2) { }
EXPORT void f9_V_SSS_I(struct S_I p0, struct S_I p1, struct S_I p2) { }
EXPORT void f9_V_SSS_F(struct S_F p0, struct S_F p1, struct S_F p2) { }
EXPORT void f9_V_SSS_D(struct S_D p0, struct S_D p1, struct S_D p2) { }
EXPORT void f9_V_SSS_P(struct S_P p0, struct S_P p1, struct S_P p2) { }
EXPORT void f9_V_SSS_II(struct S_II p0, struct S_II p1, struct S_II p2) { }
EXPORT void f9_V_SSS_IF(struct S_IF p0, struct S_IF p1, struct S_IF p2) { }
EXPORT void f9_V_SSS_ID(struct S_ID p0, struct S_ID p1, struct S_ID p2) { }
EXPORT void f9_V_SSS_IP(struct S_IP p0, struct S_IP p1, struct S_IP p2) { }
EXPORT void f9_V_SSS_FI(struct S_FI p0, struct S_FI p1, struct S_FI p2) { }
EXPORT void f9_V_SSS_FF(struct S_FF p0, struct S_FF p1, struct S_FF p2) { }
EXPORT void f9_V_SSS_FD(struct S_FD p0, struct S_FD p1, struct S_FD p2) { }
EXPORT void f9_V_SSS_FP(struct S_FP p0, struct S_FP p1, struct S_FP p2) { }
EXPORT void f9_V_SSS_DI(struct S_DI p0, struct S_DI p1, struct S_DI p2) { }
EXPORT void f9_V_SSS_DF(struct S_DF p0, struct S_DF p1, struct S_DF p2) { }
EXPORT void f9_V_SSS_DD(struct S_DD p0, struct S_DD p1, struct S_DD p2) { }
EXPORT void f9_V_SSS_DP(struct S_DP p0, struct S_DP p1, struct S_DP p2) { }
EXPORT void f9_V_SSS_PI(struct S_PI p0, struct S_PI p1, struct S_PI p2) { }
EXPORT void f9_V_SSS_PF(struct S_PF p0, struct S_PF p1, struct S_PF p2) { }
EXPORT void f9_V_SSS_PD(struct S_PD p0, struct S_PD p1, struct S_PD p2) { }
EXPORT void f9_V_SSS_PP(struct S_PP p0, struct S_PP p1, struct S_PP p2) { }
EXPORT void f9_V_SSS_III(struct S_III p0, struct S_III p1, struct S_III p2) { }
EXPORT void f9_V_SSS_IIF(struct S_IIF p0, struct S_IIF p1, struct S_IIF p2) { }
EXPORT void f9_V_SSS_IID(struct S_IID p0, struct S_IID p1, struct S_IID p2) { }
EXPORT void f9_V_SSS_IIP(struct S_IIP p0, struct S_IIP p1, struct S_IIP p2) { }
EXPORT void f9_V_SSS_IFI(struct S_IFI p0, struct S_IFI p1, struct S_IFI p2) { }
EXPORT void f9_V_SSS_IFF(struct S_IFF p0, struct S_IFF p1, struct S_IFF p2) { }
EXPORT void f9_V_SSS_IFD(struct S_IFD p0, struct S_IFD p1, struct S_IFD p2) { }
EXPORT void f9_V_SSS_IFP(struct S_IFP p0, struct S_IFP p1, struct S_IFP p2) { }
EXPORT void f9_V_SSS_IDI(struct S_IDI p0, struct S_IDI p1, struct S_IDI p2) { }
EXPORT void f9_V_SSS_IDF(struct S_IDF p0, struct S_IDF p1, struct S_IDF p2) { }
EXPORT void f9_V_SSS_IDD(struct S_IDD p0, struct S_IDD p1, struct S_IDD p2) { }
EXPORT void f9_V_SSS_IDP(struct S_IDP p0, struct S_IDP p1, struct S_IDP p2) { }
EXPORT void f9_V_SSS_IPI(struct S_IPI p0, struct S_IPI p1, struct S_IPI p2) { }
EXPORT void f9_V_SSS_IPF(struct S_IPF p0, struct S_IPF p1, struct S_IPF p2) { }
EXPORT void f9_V_SSS_IPD(struct S_IPD p0, struct S_IPD p1, struct S_IPD p2) { }
EXPORT void f10_V_SSS_IPP(struct S_IPP p0, struct S_IPP p1, struct S_IPP p2) { }
EXPORT void f10_V_SSS_FII(struct S_FII p0, struct S_FII p1, struct S_FII p2) { }
EXPORT void f10_V_SSS_FIF(struct S_FIF p0, struct S_FIF p1, struct S_FIF p2) { }
EXPORT void f10_V_SSS_FID(struct S_FID p0, struct S_FID p1, struct S_FID p2) { }
EXPORT void f10_V_SSS_FIP(struct S_FIP p0, struct S_FIP p1, struct S_FIP p2) { }
EXPORT void f10_V_SSS_FFI(struct S_FFI p0, struct S_FFI p1, struct S_FFI p2) { }
EXPORT void f10_V_SSS_FFF(struct S_FFF p0, struct S_FFF p1, struct S_FFF p2) { }
EXPORT void f10_V_SSS_FFD(struct S_FFD p0, struct S_FFD p1, struct S_FFD p2) { }
EXPORT void f10_V_SSS_FFP(struct S_FFP p0, struct S_FFP p1, struct S_FFP p2) { }
EXPORT void f10_V_SSS_FDI(struct S_FDI p0, struct S_FDI p1, struct S_FDI p2) { }
EXPORT void f10_V_SSS_FDF(struct S_FDF p0, struct S_FDF p1, struct S_FDF p2) { }
EXPORT void f10_V_SSS_FDD(struct S_FDD p0, struct S_FDD p1, struct S_FDD p2) { }
EXPORT void f10_V_SSS_FDP(struct S_FDP p0, struct S_FDP p1, struct S_FDP p2) { }
EXPORT void f10_V_SSS_FPI(struct S_FPI p0, struct S_FPI p1, struct S_FPI p2) { }
EXPORT void f10_V_SSS_FPF(struct S_FPF p0, struct S_FPF p1, struct S_FPF p2) { }
EXPORT void f10_V_SSS_FPD(struct S_FPD p0, struct S_FPD p1, struct S_FPD p2) { }
EXPORT void f10_V_SSS_FPP(struct S_FPP p0, struct S_FPP p1, struct S_FPP p2) { }
EXPORT void f10_V_SSS_DII(struct S_DII p0, struct S_DII p1, struct S_DII p2) { }
EXPORT void f10_V_SSS_DIF(struct S_DIF p0, struct S_DIF p1, struct S_DIF p2) { }
EXPORT void f10_V_SSS_DID(struct S_DID p0, struct S_DID p1, struct S_DID p2) { }
EXPORT void f10_V_SSS_DIP(struct S_DIP p0, struct S_DIP p1, struct S_DIP p2) { }
EXPORT void f10_V_SSS_DFI(struct S_DFI p0, struct S_DFI p1, struct S_DFI p2) { }
EXPORT void f10_V_SSS_DFF(struct S_DFF p0, struct S_DFF p1, struct S_DFF p2) { }
EXPORT void f10_V_SSS_DFD(struct S_DFD p0, struct S_DFD p1, struct S_DFD p2) { }
EXPORT void f10_V_SSS_DFP(struct S_DFP p0, struct S_DFP p1, struct S_DFP p2) { }
EXPORT void f10_V_SSS_DDI(struct S_DDI p0, struct S_DDI p1, struct S_DDI p2) { }
EXPORT void f10_V_SSS_DDF(struct S_DDF p0, struct S_DDF p1, struct S_DDF p2) { }
EXPORT void f10_V_SSS_DDD(struct S_DDD p0, struct S_DDD p1, struct S_DDD p2) { }
EXPORT void f10_V_SSS_DDP(struct S_DDP p0, struct S_DDP p1, struct S_DDP p2) { }
EXPORT void f10_V_SSS_DPI(struct S_DPI p0, struct S_DPI p1, struct S_DPI p2) { }
EXPORT void f10_V_SSS_DPF(struct S_DPF p0, struct S_DPF p1, struct S_DPF p2) { }
EXPORT void f10_V_SSS_DPD(struct S_DPD p0, struct S_DPD p1, struct S_DPD p2) { }
EXPORT void f10_V_SSS_DPP(struct S_DPP p0, struct S_DPP p1, struct S_DPP p2) { }
EXPORT void f10_V_SSS_PII(struct S_PII p0, struct S_PII p1, struct S_PII p2) { }
EXPORT void f10_V_SSS_PIF(struct S_PIF p0, struct S_PIF p1, struct S_PIF p2) { }
EXPORT void f10_V_SSS_PID(struct S_PID p0, struct S_PID p1, struct S_PID p2) { }
EXPORT void f10_V_SSS_PIP(struct S_PIP p0, struct S_PIP p1, struct S_PIP p2) { }
EXPORT void f10_V_SSS_PFI(struct S_PFI p0, struct S_PFI p1, struct S_PFI p2) { }
EXPORT void f10_V_SSS_PFF(struct S_PFF p0, struct S_PFF p1, struct S_PFF p2) { }
EXPORT void f10_V_SSS_PFD(struct S_PFD p0, struct S_PFD p1, struct S_PFD p2) { }
EXPORT void f10_V_SSS_PFP(struct S_PFP p0, struct S_PFP p1, struct S_PFP p2) { }
EXPORT void f10_V_SSS_PDI(struct S_PDI p0, struct S_PDI p1, struct S_PDI p2) { }
EXPORT void f10_V_SSS_PDF(struct S_PDF p0, struct S_PDF p1, struct S_PDF p2) { }
EXPORT void f10_V_SSS_PDD(struct S_PDD p0, struct S_PDD p1, struct S_PDD p2) { }
EXPORT void f10_V_SSS_PDP(struct S_PDP p0, struct S_PDP p1, struct S_PDP p2) { }
EXPORT void f10_V_SSS_PPI(struct S_PPI p0, struct S_PPI p1, struct S_PPI p2) { }
EXPORT void f10_V_SSS_PPF(struct S_PPF p0, struct S_PPF p1, struct S_PPF p2) { }
EXPORT void f10_V_SSS_PPD(struct S_PPD p0, struct S_PPD p1, struct S_PPD p2) { }
EXPORT void f10_V_SSS_PPP(struct S_PPP p0, struct S_PPP p1, struct S_PPP p2) { }
EXPORT int f10_I_I_(int p0) { return p0; }
EXPORT float f10_F_F_(float p0) { return p0; }
EXPORT double f10_D_D_(double p0) { return p0; }
EXPORT void* f10_P_P_(void* p0) { return p0; }
EXPORT struct S_I f10_S_S_I(struct S_I p0) { return p0; }
EXPORT struct S_F f10_S_S_F(struct S_F p0) { return p0; }
EXPORT struct S_D f10_S_S_D(struct S_D p0) { return p0; }
EXPORT struct S_P f10_S_S_P(struct S_P p0) { return p0; }
EXPORT struct S_II f10_S_S_II(struct S_II p0) { return p0; }
EXPORT struct S_IF f10_S_S_IF(struct S_IF p0) { return p0; }
EXPORT struct S_ID f10_S_S_ID(struct S_ID p0) { return p0; }
EXPORT struct S_IP f10_S_S_IP(struct S_IP p0) { return p0; }
EXPORT struct S_FI f10_S_S_FI(struct S_FI p0) { return p0; }
EXPORT struct S_FF f10_S_S_FF(struct S_FF p0) { return p0; }
EXPORT struct S_FD f10_S_S_FD(struct S_FD p0) { return p0; }
EXPORT struct S_FP f10_S_S_FP(struct S_FP p0) { return p0; }
EXPORT struct S_DI f10_S_S_DI(struct S_DI p0) { return p0; }
EXPORT struct S_DF f10_S_S_DF(struct S_DF p0) { return p0; }
EXPORT struct S_DD f10_S_S_DD(struct S_DD p0) { return p0; }
EXPORT struct S_DP f10_S_S_DP(struct S_DP p0) { return p0; }
EXPORT struct S_PI f10_S_S_PI(struct S_PI p0) { return p0; }
EXPORT struct S_PF f10_S_S_PF(struct S_PF p0) { return p0; }
EXPORT struct S_PD f10_S_S_PD(struct S_PD p0) { return p0; }
EXPORT struct S_PP f10_S_S_PP(struct S_PP p0) { return p0; }
EXPORT struct S_III f10_S_S_III(struct S_III p0) { return p0; }
EXPORT struct S_IIF f10_S_S_IIF(struct S_IIF p0) { return p0; }
EXPORT struct S_IID f10_S_S_IID(struct S_IID p0) { return p0; }
EXPORT struct S_IIP f10_S_S_IIP(struct S_IIP p0) { return p0; }
EXPORT struct S_IFI f10_S_S_IFI(struct S_IFI p0) { return p0; }
EXPORT struct S_IFF f10_S_S_IFF(struct S_IFF p0) { return p0; }
EXPORT struct S_IFD f10_S_S_IFD(struct S_IFD p0) { return p0; }
EXPORT struct S_IFP f10_S_S_IFP(struct S_IFP p0) { return p0; }
EXPORT struct S_IDI f10_S_S_IDI(struct S_IDI p0) { return p0; }
EXPORT struct S_IDF f10_S_S_IDF(struct S_IDF p0) { return p0; }
EXPORT struct S_IDD f10_S_S_IDD(struct S_IDD p0) { return p0; }
EXPORT struct S_IDP f10_S_S_IDP(struct S_IDP p0) { return p0; }
EXPORT struct S_IPI f10_S_S_IPI(struct S_IPI p0) { return p0; }
EXPORT struct S_IPF f10_S_S_IPF(struct S_IPF p0) { return p0; }
EXPORT struct S_IPD f10_S_S_IPD(struct S_IPD p0) { return p0; }
EXPORT struct S_IPP f10_S_S_IPP(struct S_IPP p0) { return p0; }
EXPORT struct S_FII f10_S_S_FII(struct S_FII p0) { return p0; }
EXPORT struct S_FIF f10_S_S_FIF(struct S_FIF p0) { return p0; }
EXPORT struct S_FID f10_S_S_FID(struct S_FID p0) { return p0; }
EXPORT struct S_FIP f10_S_S_FIP(struct S_FIP p0) { return p0; }
EXPORT struct S_FFI f10_S_S_FFI(struct S_FFI p0) { return p0; }
EXPORT struct S_FFF f10_S_S_FFF(struct S_FFF p0) { return p0; }
EXPORT struct S_FFD f10_S_S_FFD(struct S_FFD p0) { return p0; }
EXPORT struct S_FFP f10_S_S_FFP(struct S_FFP p0) { return p0; }
EXPORT struct S_FDI f10_S_S_FDI(struct S_FDI p0) { return p0; }
EXPORT struct S_FDF f10_S_S_FDF(struct S_FDF p0) { return p0; }
EXPORT struct S_FDD f10_S_S_FDD(struct S_FDD p0) { return p0; }
EXPORT struct S_FDP f10_S_S_FDP(struct S_FDP p0) { return p0; }
EXPORT struct S_FPI f10_S_S_FPI(struct S_FPI p0) { return p0; }
EXPORT struct S_FPF f10_S_S_FPF(struct S_FPF p0) { return p0; }
EXPORT struct S_FPD f10_S_S_FPD(struct S_FPD p0) { return p0; }
EXPORT struct S_FPP f10_S_S_FPP(struct S_FPP p0) { return p0; }
EXPORT struct S_DII f10_S_S_DII(struct S_DII p0) { return p0; }
EXPORT struct S_DIF f10_S_S_DIF(struct S_DIF p0) { return p0; }
EXPORT struct S_DID f10_S_S_DID(struct S_DID p0) { return p0; }
EXPORT struct S_DIP f10_S_S_DIP(struct S_DIP p0) { return p0; }
EXPORT struct S_DFI f10_S_S_DFI(struct S_DFI p0) { return p0; }
EXPORT struct S_DFF f10_S_S_DFF(struct S_DFF p0) { return p0; }
EXPORT struct S_DFD f10_S_S_DFD(struct S_DFD p0) { return p0; }
EXPORT struct S_DFP f10_S_S_DFP(struct S_DFP p0) { return p0; }
EXPORT struct S_DDI f10_S_S_DDI(struct S_DDI p0) { return p0; }
EXPORT struct S_DDF f10_S_S_DDF(struct S_DDF p0) { return p0; }
EXPORT struct S_DDD f10_S_S_DDD(struct S_DDD p0) { return p0; }
EXPORT struct S_DDP f10_S_S_DDP(struct S_DDP p0) { return p0; }
EXPORT struct S_DPI f10_S_S_DPI(struct S_DPI p0) { return p0; }
EXPORT struct S_DPF f10_S_S_DPF(struct S_DPF p0) { return p0; }
EXPORT struct S_DPD f10_S_S_DPD(struct S_DPD p0) { return p0; }
EXPORT struct S_DPP f10_S_S_DPP(struct S_DPP p0) { return p0; }
EXPORT struct S_PII f10_S_S_PII(struct S_PII p0) { return p0; }
EXPORT struct S_PIF f10_S_S_PIF(struct S_PIF p0) { return p0; }
EXPORT struct S_PID f10_S_S_PID(struct S_PID p0) { return p0; }
EXPORT struct S_PIP f10_S_S_PIP(struct S_PIP p0) { return p0; }
EXPORT struct S_PFI f10_S_S_PFI(struct S_PFI p0) { return p0; }
EXPORT struct S_PFF f10_S_S_PFF(struct S_PFF p0) { return p0; }
EXPORT struct S_PFD f10_S_S_PFD(struct S_PFD p0) { return p0; }
EXPORT struct S_PFP f10_S_S_PFP(struct S_PFP p0) { return p0; }
EXPORT struct S_PDI f10_S_S_PDI(struct S_PDI p0) { return p0; }
EXPORT struct S_PDF f10_S_S_PDF(struct S_PDF p0) { return p0; }
EXPORT struct S_PDD f10_S_S_PDD(struct S_PDD p0) { return p0; }
EXPORT struct S_PDP f10_S_S_PDP(struct S_PDP p0) { return p0; }
EXPORT struct S_PPI f10_S_S_PPI(struct S_PPI p0) { return p0; }
EXPORT struct S_PPF f10_S_S_PPF(struct S_PPF p0) { return p0; }
EXPORT struct S_PPD f10_S_S_PPD(struct S_PPD p0) { return p0; }
EXPORT struct S_PPP f10_S_S_PPP(struct S_PPP p0) { return p0; }
EXPORT int f10_I_II_(int p0, int p1) { return p0; }
EXPORT int f10_I_IF_(int p0, float p1) { return p0; }
EXPORT int f10_I_ID_(int p0, double p1) { return p0; }
EXPORT int f10_I_IP_(int p0, void* p1) { return p0; }
EXPORT int f10_I_IS_I(int p0, struct S_I p1) { return p0; }
EXPORT int f10_I_IS_F(int p0, struct S_F p1) { return p0; }
EXPORT int f10_I_IS_D(int p0, struct S_D p1) { return p0; }
EXPORT int f10_I_IS_P(int p0, struct S_P p1) { return p0; }
EXPORT int f10_I_IS_II(int p0, struct S_II p1) { return p0; }
EXPORT int f10_I_IS_IF(int p0, struct S_IF p1) { return p0; }
EXPORT int f10_I_IS_ID(int p0, struct S_ID p1) { return p0; }
EXPORT int f10_I_IS_IP(int p0, struct S_IP p1) { return p0; }
EXPORT int f10_I_IS_FI(int p0, struct S_FI p1) { return p0; }
EXPORT int f10_I_IS_FF(int p0, struct S_FF p1) { return p0; }
EXPORT int f10_I_IS_FD(int p0, struct S_FD p1) { return p0; }
EXPORT int f10_I_IS_FP(int p0, struct S_FP p1) { return p0; }
EXPORT int f10_I_IS_DI(int p0, struct S_DI p1) { return p0; }
EXPORT int f10_I_IS_DF(int p0, struct S_DF p1) { return p0; }
EXPORT int f10_I_IS_DD(int p0, struct S_DD p1) { return p0; }
EXPORT int f10_I_IS_DP(int p0, struct S_DP p1) { return p0; }
EXPORT int f10_I_IS_PI(int p0, struct S_PI p1) { return p0; }
EXPORT int f10_I_IS_PF(int p0, struct S_PF p1) { return p0; }
EXPORT int f10_I_IS_PD(int p0, struct S_PD p1) { return p0; }
EXPORT int f10_I_IS_PP(int p0, struct S_PP p1) { return p0; }
EXPORT int f10_I_IS_III(int p0, struct S_III p1) { return p0; }
EXPORT int f10_I_IS_IIF(int p0, struct S_IIF p1) { return p0; }
EXPORT int f10_I_IS_IID(int p0, struct S_IID p1) { return p0; }
EXPORT int f10_I_IS_IIP(int p0, struct S_IIP p1) { return p0; }
EXPORT int f10_I_IS_IFI(int p0, struct S_IFI p1) { return p0; }
EXPORT int f10_I_IS_IFF(int p0, struct S_IFF p1) { return p0; }
EXPORT int f10_I_IS_IFD(int p0, struct S_IFD p1) { return p0; }
EXPORT int f10_I_IS_IFP(int p0, struct S_IFP p1) { return p0; }
EXPORT int f10_I_IS_IDI(int p0, struct S_IDI p1) { return p0; }
EXPORT int f10_I_IS_IDF(int p0, struct S_IDF p1) { return p0; }
EXPORT int f10_I_IS_IDD(int p0, struct S_IDD p1) { return p0; }
EXPORT int f10_I_IS_IDP(int p0, struct S_IDP p1) { return p0; }
EXPORT int f10_I_IS_IPI(int p0, struct S_IPI p1) { return p0; }
EXPORT int f10_I_IS_IPF(int p0, struct S_IPF p1) { return p0; }
EXPORT int f10_I_IS_IPD(int p0, struct S_IPD p1) { return p0; }
EXPORT int f10_I_IS_IPP(int p0, struct S_IPP p1) { return p0; }
EXPORT int f10_I_IS_FII(int p0, struct S_FII p1) { return p0; }
EXPORT int f10_I_IS_FIF(int p0, struct S_FIF p1) { return p0; }
EXPORT int f10_I_IS_FID(int p0, struct S_FID p1) { return p0; }
EXPORT int f10_I_IS_FIP(int p0, struct S_FIP p1) { return p0; }
EXPORT int f10_I_IS_FFI(int p0, struct S_FFI p1) { return p0; }
EXPORT int f10_I_IS_FFF(int p0, struct S_FFF p1) { return p0; }
EXPORT int f10_I_IS_FFD(int p0, struct S_FFD p1) { return p0; }
EXPORT int f10_I_IS_FFP(int p0, struct S_FFP p1) { return p0; }
EXPORT int f10_I_IS_FDI(int p0, struct S_FDI p1) { return p0; }
EXPORT int f10_I_IS_FDF(int p0, struct S_FDF p1) { return p0; }
EXPORT int f10_I_IS_FDD(int p0, struct S_FDD p1) { return p0; }
EXPORT int f10_I_IS_FDP(int p0, struct S_FDP p1) { return p0; }
EXPORT int f10_I_IS_FPI(int p0, struct S_FPI p1) { return p0; }
EXPORT int f10_I_IS_FPF(int p0, struct S_FPF p1) { return p0; }
EXPORT int f10_I_IS_FPD(int p0, struct S_FPD p1) { return p0; }
EXPORT int f10_I_IS_FPP(int p0, struct S_FPP p1) { return p0; }
EXPORT int f10_I_IS_DII(int p0, struct S_DII p1) { return p0; }
EXPORT int f10_I_IS_DIF(int p0, struct S_DIF p1) { return p0; }
EXPORT int f10_I_IS_DID(int p0, struct S_DID p1) { return p0; }
EXPORT int f10_I_IS_DIP(int p0, struct S_DIP p1) { return p0; }
EXPORT int f10_I_IS_DFI(int p0, struct S_DFI p1) { return p0; }
EXPORT int f10_I_IS_DFF(int p0, struct S_DFF p1) { return p0; }
EXPORT int f10_I_IS_DFD(int p0, struct S_DFD p1) { return p0; }
EXPORT int f10_I_IS_DFP(int p0, struct S_DFP p1) { return p0; }
EXPORT int f10_I_IS_DDI(int p0, struct S_DDI p1) { return p0; }
EXPORT int f10_I_IS_DDF(int p0, struct S_DDF p1) { return p0; }
EXPORT int f10_I_IS_DDD(int p0, struct S_DDD p1) { return p0; }
EXPORT int f10_I_IS_DDP(int p0, struct S_DDP p1) { return p0; }
EXPORT int f10_I_IS_DPI(int p0, struct S_DPI p1) { return p0; }
EXPORT int f10_I_IS_DPF(int p0, struct S_DPF p1) { return p0; }
EXPORT int f10_I_IS_DPD(int p0, struct S_DPD p1) { return p0; }
EXPORT int f10_I_IS_DPP(int p0, struct S_DPP p1) { return p0; }
EXPORT int f10_I_IS_PII(int p0, struct S_PII p1) { return p0; }
EXPORT int f10_I_IS_PIF(int p0, struct S_PIF p1) { return p0; }
EXPORT int f10_I_IS_PID(int p0, struct S_PID p1) { return p0; }
EXPORT int f10_I_IS_PIP(int p0, struct S_PIP p1) { return p0; }
EXPORT int f10_I_IS_PFI(int p0, struct S_PFI p1) { return p0; }
EXPORT int f10_I_IS_PFF(int p0, struct S_PFF p1) { return p0; }
EXPORT int f10_I_IS_PFD(int p0, struct S_PFD p1) { return p0; }
EXPORT int f10_I_IS_PFP(int p0, struct S_PFP p1) { return p0; }
EXPORT int f10_I_IS_PDI(int p0, struct S_PDI p1) { return p0; }
EXPORT int f10_I_IS_PDF(int p0, struct S_PDF p1) { return p0; }
EXPORT int f10_I_IS_PDD(int p0, struct S_PDD p1) { return p0; }
EXPORT int f10_I_IS_PDP(int p0, struct S_PDP p1) { return p0; }
EXPORT int f10_I_IS_PPI(int p0, struct S_PPI p1) { return p0; }
EXPORT int f10_I_IS_PPF(int p0, struct S_PPF p1) { return p0; }
EXPORT int f10_I_IS_PPD(int p0, struct S_PPD p1) { return p0; }
EXPORT int f10_I_IS_PPP(int p0, struct S_PPP p1) { return p0; }
EXPORT float f10_F_FI_(float p0, int p1) { return p0; }
EXPORT float f10_F_FF_(float p0, float p1) { return p0; }
EXPORT float f10_F_FD_(float p0, double p1) { return p0; }
EXPORT float f10_F_FP_(float p0, void* p1) { return p0; }
EXPORT float f10_F_FS_I(float p0, struct S_I p1) { return p0; }
EXPORT float f10_F_FS_F(float p0, struct S_F p1) { return p0; }
EXPORT float f10_F_FS_D(float p0, struct S_D p1) { return p0; }
EXPORT float f10_F_FS_P(float p0, struct S_P p1) { return p0; }
EXPORT float f10_F_FS_II(float p0, struct S_II p1) { return p0; }
EXPORT float f10_F_FS_IF(float p0, struct S_IF p1) { return p0; }
EXPORT float f10_F_FS_ID(float p0, struct S_ID p1) { return p0; }
EXPORT float f10_F_FS_IP(float p0, struct S_IP p1) { return p0; }
EXPORT float f10_F_FS_FI(float p0, struct S_FI p1) { return p0; }
EXPORT float f10_F_FS_FF(float p0, struct S_FF p1) { return p0; }
EXPORT float f10_F_FS_FD(float p0, struct S_FD p1) { return p0; }
EXPORT float f10_F_FS_FP(float p0, struct S_FP p1) { return p0; }
EXPORT float f10_F_FS_DI(float p0, struct S_DI p1) { return p0; }
EXPORT float f10_F_FS_DF(float p0, struct S_DF p1) { return p0; }
EXPORT float f10_F_FS_DD(float p0, struct S_DD p1) { return p0; }
EXPORT float f10_F_FS_DP(float p0, struct S_DP p1) { return p0; }
EXPORT float f10_F_FS_PI(float p0, struct S_PI p1) { return p0; }
EXPORT float f10_F_FS_PF(float p0, struct S_PF p1) { return p0; }
EXPORT float f10_F_FS_PD(float p0, struct S_PD p1) { return p0; }
EXPORT float f10_F_FS_PP(float p0, struct S_PP p1) { return p0; }
EXPORT float f10_F_FS_III(float p0, struct S_III p1) { return p0; }
EXPORT float f10_F_FS_IIF(float p0, struct S_IIF p1) { return p0; }
EXPORT float f10_F_FS_IID(float p0, struct S_IID p1) { return p0; }
EXPORT float f10_F_FS_IIP(float p0, struct S_IIP p1) { return p0; }
EXPORT float f10_F_FS_IFI(float p0, struct S_IFI p1) { return p0; }
EXPORT float f10_F_FS_IFF(float p0, struct S_IFF p1) { return p0; }
EXPORT float f10_F_FS_IFD(float p0, struct S_IFD p1) { return p0; }
EXPORT float f10_F_FS_IFP(float p0, struct S_IFP p1) { return p0; }
EXPORT float f10_F_FS_IDI(float p0, struct S_IDI p1) { return p0; }
EXPORT float f10_F_FS_IDF(float p0, struct S_IDF p1) { return p0; }
EXPORT float f10_F_FS_IDD(float p0, struct S_IDD p1) { return p0; }
EXPORT float f10_F_FS_IDP(float p0, struct S_IDP p1) { return p0; }
EXPORT float f10_F_FS_IPI(float p0, struct S_IPI p1) { return p0; }
EXPORT float f10_F_FS_IPF(float p0, struct S_IPF p1) { return p0; }
EXPORT float f10_F_FS_IPD(float p0, struct S_IPD p1) { return p0; }
EXPORT float f10_F_FS_IPP(float p0, struct S_IPP p1) { return p0; }
EXPORT float f10_F_FS_FII(float p0, struct S_FII p1) { return p0; }
EXPORT float f10_F_FS_FIF(float p0, struct S_FIF p1) { return p0; }
EXPORT float f10_F_FS_FID(float p0, struct S_FID p1) { return p0; }
EXPORT float f10_F_FS_FIP(float p0, struct S_FIP p1) { return p0; }
EXPORT float f10_F_FS_FFI(float p0, struct S_FFI p1) { return p0; }
EXPORT float f10_F_FS_FFF(float p0, struct S_FFF p1) { return p0; }
EXPORT float f10_F_FS_FFD(float p0, struct S_FFD p1) { return p0; }
EXPORT float f10_F_FS_FFP(float p0, struct S_FFP p1) { return p0; }
EXPORT float f10_F_FS_FDI(float p0, struct S_FDI p1) { return p0; }
EXPORT float f10_F_FS_FDF(float p0, struct S_FDF p1) { return p0; }
EXPORT float f10_F_FS_FDD(float p0, struct S_FDD p1) { return p0; }
EXPORT float f10_F_FS_FDP(float p0, struct S_FDP p1) { return p0; }
EXPORT float f10_F_FS_FPI(float p0, struct S_FPI p1) { return p0; }
EXPORT float f10_F_FS_FPF(float p0, struct S_FPF p1) { return p0; }
EXPORT float f10_F_FS_FPD(float p0, struct S_FPD p1) { return p0; }
EXPORT float f10_F_FS_FPP(float p0, struct S_FPP p1) { return p0; }
EXPORT float f10_F_FS_DII(float p0, struct S_DII p1) { return p0; }
EXPORT float f10_F_FS_DIF(float p0, struct S_DIF p1) { return p0; }
EXPORT float f10_F_FS_DID(float p0, struct S_DID p1) { return p0; }
EXPORT float f10_F_FS_DIP(float p0, struct S_DIP p1) { return p0; }
EXPORT float f10_F_FS_DFI(float p0, struct S_DFI p1) { return p0; }
EXPORT float f10_F_FS_DFF(float p0, struct S_DFF p1) { return p0; }
EXPORT float f10_F_FS_DFD(float p0, struct S_DFD p1) { return p0; }
EXPORT float f10_F_FS_DFP(float p0, struct S_DFP p1) { return p0; }
EXPORT float f10_F_FS_DDI(float p0, struct S_DDI p1) { return p0; }
EXPORT float f10_F_FS_DDF(float p0, struct S_DDF p1) { return p0; }
EXPORT float f10_F_FS_DDD(float p0, struct S_DDD p1) { return p0; }
EXPORT float f10_F_FS_DDP(float p0, struct S_DDP p1) { return p0; }
EXPORT float f10_F_FS_DPI(float p0, struct S_DPI p1) { return p0; }
EXPORT float f10_F_FS_DPF(float p0, struct S_DPF p1) { return p0; }
EXPORT float f10_F_FS_DPD(float p0, struct S_DPD p1) { return p0; }
EXPORT float f10_F_FS_DPP(float p0, struct S_DPP p1) { return p0; }
EXPORT float f10_F_FS_PII(float p0, struct S_PII p1) { return p0; }
EXPORT float f10_F_FS_PIF(float p0, struct S_PIF p1) { return p0; }
EXPORT float f10_F_FS_PID(float p0, struct S_PID p1) { return p0; }
EXPORT float f10_F_FS_PIP(float p0, struct S_PIP p1) { return p0; }
EXPORT float f10_F_FS_PFI(float p0, struct S_PFI p1) { return p0; }
EXPORT float f10_F_FS_PFF(float p0, struct S_PFF p1) { return p0; }
EXPORT float f10_F_FS_PFD(float p0, struct S_PFD p1) { return p0; }
EXPORT float f10_F_FS_PFP(float p0, struct S_PFP p1) { return p0; }
EXPORT float f10_F_FS_PDI(float p0, struct S_PDI p1) { return p0; }
EXPORT float f10_F_FS_PDF(float p0, struct S_PDF p1) { return p0; }
EXPORT float f10_F_FS_PDD(float p0, struct S_PDD p1) { return p0; }
EXPORT float f10_F_FS_PDP(float p0, struct S_PDP p1) { return p0; }
EXPORT float f10_F_FS_PPI(float p0, struct S_PPI p1) { return p0; }
EXPORT float f10_F_FS_PPF(float p0, struct S_PPF p1) { return p0; }
EXPORT float f10_F_FS_PPD(float p0, struct S_PPD p1) { return p0; }
EXPORT float f10_F_FS_PPP(float p0, struct S_PPP p1) { return p0; }
EXPORT double f10_D_DI_(double p0, int p1) { return p0; }
EXPORT double f10_D_DF_(double p0, float p1) { return p0; }
EXPORT double f10_D_DD_(double p0, double p1) { return p0; }
EXPORT double f10_D_DP_(double p0, void* p1) { return p0; }
EXPORT double f10_D_DS_I(double p0, struct S_I p1) { return p0; }
EXPORT double f10_D_DS_F(double p0, struct S_F p1) { return p0; }
EXPORT double f10_D_DS_D(double p0, struct S_D p1) { return p0; }
EXPORT double f10_D_DS_P(double p0, struct S_P p1) { return p0; }
EXPORT double f10_D_DS_II(double p0, struct S_II p1) { return p0; }
EXPORT double f10_D_DS_IF(double p0, struct S_IF p1) { return p0; }
EXPORT double f10_D_DS_ID(double p0, struct S_ID p1) { return p0; }
EXPORT double f10_D_DS_IP(double p0, struct S_IP p1) { return p0; }
EXPORT double f10_D_DS_FI(double p0, struct S_FI p1) { return p0; }
EXPORT double f10_D_DS_FF(double p0, struct S_FF p1) { return p0; }
EXPORT double f10_D_DS_FD(double p0, struct S_FD p1) { return p0; }
EXPORT double f10_D_DS_FP(double p0, struct S_FP p1) { return p0; }
EXPORT double f10_D_DS_DI(double p0, struct S_DI p1) { return p0; }
EXPORT double f10_D_DS_DF(double p0, struct S_DF p1) { return p0; }
EXPORT double f10_D_DS_DD(double p0, struct S_DD p1) { return p0; }
EXPORT double f10_D_DS_DP(double p0, struct S_DP p1) { return p0; }
EXPORT double f10_D_DS_PI(double p0, struct S_PI p1) { return p0; }
EXPORT double f10_D_DS_PF(double p0, struct S_PF p1) { return p0; }
EXPORT double f10_D_DS_PD(double p0, struct S_PD p1) { return p0; }
EXPORT double f10_D_DS_PP(double p0, struct S_PP p1) { return p0; }
EXPORT double f10_D_DS_III(double p0, struct S_III p1) { return p0; }
EXPORT double f10_D_DS_IIF(double p0, struct S_IIF p1) { return p0; }
EXPORT double f10_D_DS_IID(double p0, struct S_IID p1) { return p0; }
EXPORT double f10_D_DS_IIP(double p0, struct S_IIP p1) { return p0; }
EXPORT double f10_D_DS_IFI(double p0, struct S_IFI p1) { return p0; }
EXPORT double f10_D_DS_IFF(double p0, struct S_IFF p1) { return p0; }
EXPORT double f10_D_DS_IFD(double p0, struct S_IFD p1) { return p0; }
EXPORT double f10_D_DS_IFP(double p0, struct S_IFP p1) { return p0; }
EXPORT double f10_D_DS_IDI(double p0, struct S_IDI p1) { return p0; }
EXPORT double f10_D_DS_IDF(double p0, struct S_IDF p1) { return p0; }
EXPORT double f10_D_DS_IDD(double p0, struct S_IDD p1) { return p0; }
EXPORT double f10_D_DS_IDP(double p0, struct S_IDP p1) { return p0; }
EXPORT double f10_D_DS_IPI(double p0, struct S_IPI p1) { return p0; }
EXPORT double f10_D_DS_IPF(double p0, struct S_IPF p1) { return p0; }
EXPORT double f10_D_DS_IPD(double p0, struct S_IPD p1) { return p0; }
EXPORT double f10_D_DS_IPP(double p0, struct S_IPP p1) { return p0; }
EXPORT double f10_D_DS_FII(double p0, struct S_FII p1) { return p0; }
EXPORT double f10_D_DS_FIF(double p0, struct S_FIF p1) { return p0; }
EXPORT double f10_D_DS_FID(double p0, struct S_FID p1) { return p0; }
EXPORT double f10_D_DS_FIP(double p0, struct S_FIP p1) { return p0; }
EXPORT double f10_D_DS_FFI(double p0, struct S_FFI p1) { return p0; }
EXPORT double f10_D_DS_FFF(double p0, struct S_FFF p1) { return p0; }
EXPORT double f10_D_DS_FFD(double p0, struct S_FFD p1) { return p0; }
EXPORT double f10_D_DS_FFP(double p0, struct S_FFP p1) { return p0; }
EXPORT double f10_D_DS_FDI(double p0, struct S_FDI p1) { return p0; }
EXPORT double f10_D_DS_FDF(double p0, struct S_FDF p1) { return p0; }
EXPORT double f10_D_DS_FDD(double p0, struct S_FDD p1) { return p0; }
EXPORT double f10_D_DS_FDP(double p0, struct S_FDP p1) { return p0; }
EXPORT double f10_D_DS_FPI(double p0, struct S_FPI p1) { return p0; }
EXPORT double f10_D_DS_FPF(double p0, struct S_FPF p1) { return p0; }
EXPORT double f10_D_DS_FPD(double p0, struct S_FPD p1) { return p0; }
EXPORT double f10_D_DS_FPP(double p0, struct S_FPP p1) { return p0; }
EXPORT double f10_D_DS_DII(double p0, struct S_DII p1) { return p0; }
EXPORT double f10_D_DS_DIF(double p0, struct S_DIF p1) { return p0; }
EXPORT double f10_D_DS_DID(double p0, struct S_DID p1) { return p0; }
EXPORT double f10_D_DS_DIP(double p0, struct S_DIP p1) { return p0; }
EXPORT double f10_D_DS_DFI(double p0, struct S_DFI p1) { return p0; }
EXPORT double f10_D_DS_DFF(double p0, struct S_DFF p1) { return p0; }
EXPORT double f10_D_DS_DFD(double p0, struct S_DFD p1) { return p0; }
EXPORT double f10_D_DS_DFP(double p0, struct S_DFP p1) { return p0; }
EXPORT double f10_D_DS_DDI(double p0, struct S_DDI p1) { return p0; }
EXPORT double f10_D_DS_DDF(double p0, struct S_DDF p1) { return p0; }
EXPORT double f10_D_DS_DDD(double p0, struct S_DDD p1) { return p0; }
EXPORT double f10_D_DS_DDP(double p0, struct S_DDP p1) { return p0; }
EXPORT double f10_D_DS_DPI(double p0, struct S_DPI p1) { return p0; }
EXPORT double f10_D_DS_DPF(double p0, struct S_DPF p1) { return p0; }
EXPORT double f10_D_DS_DPD(double p0, struct S_DPD p1) { return p0; }
EXPORT double f10_D_DS_DPP(double p0, struct S_DPP p1) { return p0; }
EXPORT double f10_D_DS_PII(double p0, struct S_PII p1) { return p0; }
EXPORT double f10_D_DS_PIF(double p0, struct S_PIF p1) { return p0; }
EXPORT double f10_D_DS_PID(double p0, struct S_PID p1) { return p0; }
EXPORT double f10_D_DS_PIP(double p0, struct S_PIP p1) { return p0; }
EXPORT double f10_D_DS_PFI(double p0, struct S_PFI p1) { return p0; }
EXPORT double f10_D_DS_PFF(double p0, struct S_PFF p1) { return p0; }
EXPORT double f10_D_DS_PFD(double p0, struct S_PFD p1) { return p0; }
EXPORT double f10_D_DS_PFP(double p0, struct S_PFP p1) { return p0; }
EXPORT double f10_D_DS_PDI(double p0, struct S_PDI p1) { return p0; }
EXPORT double f10_D_DS_PDF(double p0, struct S_PDF p1) { return p0; }
EXPORT double f10_D_DS_PDD(double p0, struct S_PDD p1) { return p0; }
EXPORT double f10_D_DS_PDP(double p0, struct S_PDP p1) { return p0; }
EXPORT double f10_D_DS_PPI(double p0, struct S_PPI p1) { return p0; }
EXPORT double f10_D_DS_PPF(double p0, struct S_PPF p1) { return p0; }
EXPORT double f10_D_DS_PPD(double p0, struct S_PPD p1) { return p0; }
EXPORT double f10_D_DS_PPP(double p0, struct S_PPP p1) { return p0; }
EXPORT void* f10_P_PI_(void* p0, int p1) { return p0; }
EXPORT void* f10_P_PF_(void* p0, float p1) { return p0; }
EXPORT void* f10_P_PD_(void* p0, double p1) { return p0; }
EXPORT void* f10_P_PP_(void* p0, void* p1) { return p0; }
EXPORT void* f10_P_PS_I(void* p0, struct S_I p1) { return p0; }
EXPORT void* f10_P_PS_F(void* p0, struct S_F p1) { return p0; }
EXPORT void* f10_P_PS_D(void* p0, struct S_D p1) { return p0; }
EXPORT void* f10_P_PS_P(void* p0, struct S_P p1) { return p0; }
EXPORT void* f10_P_PS_II(void* p0, struct S_II p1) { return p0; }
EXPORT void* f10_P_PS_IF(void* p0, struct S_IF p1) { return p0; }
EXPORT void* f10_P_PS_ID(void* p0, struct S_ID p1) { return p0; }
EXPORT void* f10_P_PS_IP(void* p0, struct S_IP p1) { return p0; }
EXPORT void* f10_P_PS_FI(void* p0, struct S_FI p1) { return p0; }
EXPORT void* f10_P_PS_FF(void* p0, struct S_FF p1) { return p0; }
EXPORT void* f10_P_PS_FD(void* p0, struct S_FD p1) { return p0; }
EXPORT void* f10_P_PS_FP(void* p0, struct S_FP p1) { return p0; }
EXPORT void* f10_P_PS_DI(void* p0, struct S_DI p1) { return p0; }
EXPORT void* f10_P_PS_DF(void* p0, struct S_DF p1) { return p0; }
EXPORT void* f10_P_PS_DD(void* p0, struct S_DD p1) { return p0; }
EXPORT void* f10_P_PS_DP(void* p0, struct S_DP p1) { return p0; }
EXPORT void* f10_P_PS_PI(void* p0, struct S_PI p1) { return p0; }
EXPORT void* f10_P_PS_PF(void* p0, struct S_PF p1) { return p0; }
EXPORT void* f10_P_PS_PD(void* p0, struct S_PD p1) { return p0; }
EXPORT void* f10_P_PS_PP(void* p0, struct S_PP p1) { return p0; }
EXPORT void* f10_P_PS_III(void* p0, struct S_III p1) { return p0; }
EXPORT void* f10_P_PS_IIF(void* p0, struct S_IIF p1) { return p0; }
EXPORT void* f10_P_PS_IID(void* p0, struct S_IID p1) { return p0; }
EXPORT void* f10_P_PS_IIP(void* p0, struct S_IIP p1) { return p0; }
EXPORT void* f10_P_PS_IFI(void* p0, struct S_IFI p1) { return p0; }
EXPORT void* f10_P_PS_IFF(void* p0, struct S_IFF p1) { return p0; }
EXPORT void* f10_P_PS_IFD(void* p0, struct S_IFD p1) { return p0; }
EXPORT void* f10_P_PS_IFP(void* p0, struct S_IFP p1) { return p0; }
EXPORT void* f10_P_PS_IDI(void* p0, struct S_IDI p1) { return p0; }
EXPORT void* f10_P_PS_IDF(void* p0, struct S_IDF p1) { return p0; }
EXPORT void* f10_P_PS_IDD(void* p0, struct S_IDD p1) { return p0; }
EXPORT void* f10_P_PS_IDP(void* p0, struct S_IDP p1) { return p0; }
EXPORT void* f10_P_PS_IPI(void* p0, struct S_IPI p1) { return p0; }
EXPORT void* f10_P_PS_IPF(void* p0, struct S_IPF p1) { return p0; }
EXPORT void* f10_P_PS_IPD(void* p0, struct S_IPD p1) { return p0; }
EXPORT void* f10_P_PS_IPP(void* p0, struct S_IPP p1) { return p0; }
EXPORT void* f10_P_PS_FII(void* p0, struct S_FII p1) { return p0; }
EXPORT void* f10_P_PS_FIF(void* p0, struct S_FIF p1) { return p0; }
EXPORT void* f10_P_PS_FID(void* p0, struct S_FID p1) { return p0; }
EXPORT void* f10_P_PS_FIP(void* p0, struct S_FIP p1) { return p0; }
EXPORT void* f10_P_PS_FFI(void* p0, struct S_FFI p1) { return p0; }
EXPORT void* f10_P_PS_FFF(void* p0, struct S_FFF p1) { return p0; }
EXPORT void* f10_P_PS_FFD(void* p0, struct S_FFD p1) { return p0; }
EXPORT void* f10_P_PS_FFP(void* p0, struct S_FFP p1) { return p0; }
EXPORT void* f10_P_PS_FDI(void* p0, struct S_FDI p1) { return p0; }
EXPORT void* f10_P_PS_FDF(void* p0, struct S_FDF p1) { return p0; }
EXPORT void* f10_P_PS_FDD(void* p0, struct S_FDD p1) { return p0; }
EXPORT void* f10_P_PS_FDP(void* p0, struct S_FDP p1) { return p0; }
EXPORT void* f10_P_PS_FPI(void* p0, struct S_FPI p1) { return p0; }
EXPORT void* f10_P_PS_FPF(void* p0, struct S_FPF p1) { return p0; }
EXPORT void* f10_P_PS_FPD(void* p0, struct S_FPD p1) { return p0; }
EXPORT void* f10_P_PS_FPP(void* p0, struct S_FPP p1) { return p0; }
EXPORT void* f10_P_PS_DII(void* p0, struct S_DII p1) { return p0; }
EXPORT void* f10_P_PS_DIF(void* p0, struct S_DIF p1) { return p0; }
EXPORT void* f10_P_PS_DID(void* p0, struct S_DID p1) { return p0; }
EXPORT void* f10_P_PS_DIP(void* p0, struct S_DIP p1) { return p0; }
EXPORT void* f10_P_PS_DFI(void* p0, struct S_DFI p1) { return p0; }
EXPORT void* f10_P_PS_DFF(void* p0, struct S_DFF p1) { return p0; }
EXPORT void* f10_P_PS_DFD(void* p0, struct S_DFD p1) { return p0; }
EXPORT void* f10_P_PS_DFP(void* p0, struct S_DFP p1) { return p0; }
EXPORT void* f10_P_PS_DDI(void* p0, struct S_DDI p1) { return p0; }
EXPORT void* f10_P_PS_DDF(void* p0, struct S_DDF p1) { return p0; }
EXPORT void* f10_P_PS_DDD(void* p0, struct S_DDD p1) { return p0; }
EXPORT void* f10_P_PS_DDP(void* p0, struct S_DDP p1) { return p0; }
EXPORT void* f10_P_PS_DPI(void* p0, struct S_DPI p1) { return p0; }
EXPORT void* f10_P_PS_DPF(void* p0, struct S_DPF p1) { return p0; }
EXPORT void* f10_P_PS_DPD(void* p0, struct S_DPD p1) { return p0; }
EXPORT void* f10_P_PS_DPP(void* p0, struct S_DPP p1) { return p0; }
EXPORT void* f10_P_PS_PII(void* p0, struct S_PII p1) { return p0; }
EXPORT void* f10_P_PS_PIF(void* p0, struct S_PIF p1) { return p0; }
EXPORT void* f10_P_PS_PID(void* p0, struct S_PID p1) { return p0; }
EXPORT void* f10_P_PS_PIP(void* p0, struct S_PIP p1) { return p0; }
EXPORT void* f10_P_PS_PFI(void* p0, struct S_PFI p1) { return p0; }
EXPORT void* f10_P_PS_PFF(void* p0, struct S_PFF p1) { return p0; }
EXPORT void* f10_P_PS_PFD(void* p0, struct S_PFD p1) { return p0; }
EXPORT void* f10_P_PS_PFP(void* p0, struct S_PFP p1) { return p0; }
EXPORT void* f10_P_PS_PDI(void* p0, struct S_PDI p1) { return p0; }
EXPORT void* f10_P_PS_PDF(void* p0, struct S_PDF p1) { return p0; }
EXPORT void* f10_P_PS_PDD(void* p0, struct S_PDD p1) { return p0; }
EXPORT void* f10_P_PS_PDP(void* p0, struct S_PDP p1) { return p0; }
EXPORT void* f10_P_PS_PPI(void* p0, struct S_PPI p1) { return p0; }
EXPORT void* f10_P_PS_PPF(void* p0, struct S_PPF p1) { return p0; }
EXPORT void* f10_P_PS_PPD(void* p0, struct S_PPD p1) { return p0; }
EXPORT void* f10_P_PS_PPP(void* p0, struct S_PPP p1) { return p0; }
EXPORT struct S_I f10_S_SI_I(struct S_I p0, int p1) { return p0; }
EXPORT struct S_F f10_S_SI_F(struct S_F p0, int p1) { return p0; }
EXPORT struct S_D f10_S_SI_D(struct S_D p0, int p1) { return p0; }
EXPORT struct S_P f10_S_SI_P(struct S_P p0, int p1) { return p0; }
EXPORT struct S_II f10_S_SI_II(struct S_II p0, int p1) { return p0; }
EXPORT struct S_IF f10_S_SI_IF(struct S_IF p0, int p1) { return p0; }
EXPORT struct S_ID f10_S_SI_ID(struct S_ID p0, int p1) { return p0; }
EXPORT struct S_IP f10_S_SI_IP(struct S_IP p0, int p1) { return p0; }
EXPORT struct S_FI f10_S_SI_FI(struct S_FI p0, int p1) { return p0; }
EXPORT struct S_FF f10_S_SI_FF(struct S_FF p0, int p1) { return p0; }
EXPORT struct S_FD f10_S_SI_FD(struct S_FD p0, int p1) { return p0; }
EXPORT struct S_FP f10_S_SI_FP(struct S_FP p0, int p1) { return p0; }
EXPORT struct S_DI f10_S_SI_DI(struct S_DI p0, int p1) { return p0; }
EXPORT struct S_DF f10_S_SI_DF(struct S_DF p0, int p1) { return p0; }
EXPORT struct S_DD f10_S_SI_DD(struct S_DD p0, int p1) { return p0; }
EXPORT struct S_DP f10_S_SI_DP(struct S_DP p0, int p1) { return p0; }
EXPORT struct S_PI f10_S_SI_PI(struct S_PI p0, int p1) { return p0; }
EXPORT struct S_PF f10_S_SI_PF(struct S_PF p0, int p1) { return p0; }
EXPORT struct S_PD f10_S_SI_PD(struct S_PD p0, int p1) { return p0; }
EXPORT struct S_PP f10_S_SI_PP(struct S_PP p0, int p1) { return p0; }
EXPORT struct S_III f10_S_SI_III(struct S_III p0, int p1) { return p0; }
EXPORT struct S_IIF f10_S_SI_IIF(struct S_IIF p0, int p1) { return p0; }
EXPORT struct S_IID f10_S_SI_IID(struct S_IID p0, int p1) { return p0; }
EXPORT struct S_IIP f10_S_SI_IIP(struct S_IIP p0, int p1) { return p0; }
EXPORT struct S_IFI f10_S_SI_IFI(struct S_IFI p0, int p1) { return p0; }
EXPORT struct S_IFF f10_S_SI_IFF(struct S_IFF p0, int p1) { return p0; }
EXPORT struct S_IFD f10_S_SI_IFD(struct S_IFD p0, int p1) { return p0; }
EXPORT struct S_IFP f10_S_SI_IFP(struct S_IFP p0, int p1) { return p0; }
EXPORT struct S_IDI f10_S_SI_IDI(struct S_IDI p0, int p1) { return p0; }
EXPORT struct S_IDF f10_S_SI_IDF(struct S_IDF p0, int p1) { return p0; }
EXPORT struct S_IDD f10_S_SI_IDD(struct S_IDD p0, int p1) { return p0; }
EXPORT struct S_IDP f10_S_SI_IDP(struct S_IDP p0, int p1) { return p0; }
EXPORT struct S_IPI f10_S_SI_IPI(struct S_IPI p0, int p1) { return p0; }
EXPORT struct S_IPF f10_S_SI_IPF(struct S_IPF p0, int p1) { return p0; }
EXPORT struct S_IPD f10_S_SI_IPD(struct S_IPD p0, int p1) { return p0; }
EXPORT struct S_IPP f10_S_SI_IPP(struct S_IPP p0, int p1) { return p0; }
EXPORT struct S_FII f10_S_SI_FII(struct S_FII p0, int p1) { return p0; }
EXPORT struct S_FIF f10_S_SI_FIF(struct S_FIF p0, int p1) { return p0; }
EXPORT struct S_FID f10_S_SI_FID(struct S_FID p0, int p1) { return p0; }
EXPORT struct S_FIP f10_S_SI_FIP(struct S_FIP p0, int p1) { return p0; }
EXPORT struct S_FFI f10_S_SI_FFI(struct S_FFI p0, int p1) { return p0; }
EXPORT struct S_FFF f10_S_SI_FFF(struct S_FFF p0, int p1) { return p0; }
EXPORT struct S_FFD f10_S_SI_FFD(struct S_FFD p0, int p1) { return p0; }
EXPORT struct S_FFP f10_S_SI_FFP(struct S_FFP p0, int p1) { return p0; }
EXPORT struct S_FDI f10_S_SI_FDI(struct S_FDI p0, int p1) { return p0; }
EXPORT struct S_FDF f10_S_SI_FDF(struct S_FDF p0, int p1) { return p0; }
EXPORT struct S_FDD f10_S_SI_FDD(struct S_FDD p0, int p1) { return p0; }
EXPORT struct S_FDP f10_S_SI_FDP(struct S_FDP p0, int p1) { return p0; }
EXPORT struct S_FPI f10_S_SI_FPI(struct S_FPI p0, int p1) { return p0; }
EXPORT struct S_FPF f10_S_SI_FPF(struct S_FPF p0, int p1) { return p0; }
EXPORT struct S_FPD f10_S_SI_FPD(struct S_FPD p0, int p1) { return p0; }
EXPORT struct S_FPP f10_S_SI_FPP(struct S_FPP p0, int p1) { return p0; }
EXPORT struct S_DII f10_S_SI_DII(struct S_DII p0, int p1) { return p0; }
EXPORT struct S_DIF f10_S_SI_DIF(struct S_DIF p0, int p1) { return p0; }
EXPORT struct S_DID f10_S_SI_DID(struct S_DID p0, int p1) { return p0; }
EXPORT struct S_DIP f10_S_SI_DIP(struct S_DIP p0, int p1) { return p0; }
EXPORT struct S_DFI f10_S_SI_DFI(struct S_DFI p0, int p1) { return p0; }
EXPORT struct S_DFF f10_S_SI_DFF(struct S_DFF p0, int p1) { return p0; }
EXPORT struct S_DFD f10_S_SI_DFD(struct S_DFD p0, int p1) { return p0; }
EXPORT struct S_DFP f10_S_SI_DFP(struct S_DFP p0, int p1) { return p0; }
EXPORT struct S_DDI f10_S_SI_DDI(struct S_DDI p0, int p1) { return p0; }
EXPORT struct S_DDF f10_S_SI_DDF(struct S_DDF p0, int p1) { return p0; }
EXPORT struct S_DDD f10_S_SI_DDD(struct S_DDD p0, int p1) { return p0; }
EXPORT struct S_DDP f10_S_SI_DDP(struct S_DDP p0, int p1) { return p0; }
EXPORT struct S_DPI f10_S_SI_DPI(struct S_DPI p0, int p1) { return p0; }
EXPORT struct S_DPF f10_S_SI_DPF(struct S_DPF p0, int p1) { return p0; }
EXPORT struct S_DPD f10_S_SI_DPD(struct S_DPD p0, int p1) { return p0; }
EXPORT struct S_DPP f10_S_SI_DPP(struct S_DPP p0, int p1) { return p0; }
EXPORT struct S_PII f10_S_SI_PII(struct S_PII p0, int p1) { return p0; }
EXPORT struct S_PIF f10_S_SI_PIF(struct S_PIF p0, int p1) { return p0; }
EXPORT struct S_PID f10_S_SI_PID(struct S_PID p0, int p1) { return p0; }
EXPORT struct S_PIP f10_S_SI_PIP(struct S_PIP p0, int p1) { return p0; }
EXPORT struct S_PFI f10_S_SI_PFI(struct S_PFI p0, int p1) { return p0; }
EXPORT struct S_PFF f10_S_SI_PFF(struct S_PFF p0, int p1) { return p0; }
EXPORT struct S_PFD f10_S_SI_PFD(struct S_PFD p0, int p1) { return p0; }
EXPORT struct S_PFP f10_S_SI_PFP(struct S_PFP p0, int p1) { return p0; }
EXPORT struct S_PDI f10_S_SI_PDI(struct S_PDI p0, int p1) { return p0; }
EXPORT struct S_PDF f10_S_SI_PDF(struct S_PDF p0, int p1) { return p0; }
EXPORT struct S_PDD f10_S_SI_PDD(struct S_PDD p0, int p1) { return p0; }
EXPORT struct S_PDP f10_S_SI_PDP(struct S_PDP p0, int p1) { return p0; }
EXPORT struct S_PPI f10_S_SI_PPI(struct S_PPI p0, int p1) { return p0; }
EXPORT struct S_PPF f10_S_SI_PPF(struct S_PPF p0, int p1) { return p0; }
EXPORT struct S_PPD f10_S_SI_PPD(struct S_PPD p0, int p1) { return p0; }
EXPORT struct S_PPP f10_S_SI_PPP(struct S_PPP p0, int p1) { return p0; }
EXPORT struct S_I f10_S_SF_I(struct S_I p0, float p1) { return p0; }
EXPORT struct S_F f10_S_SF_F(struct S_F p0, float p1) { return p0; }
EXPORT struct S_D f10_S_SF_D(struct S_D p0, float p1) { return p0; }
EXPORT struct S_P f10_S_SF_P(struct S_P p0, float p1) { return p0; }
EXPORT struct S_II f10_S_SF_II(struct S_II p0, float p1) { return p0; }
EXPORT struct S_IF f10_S_SF_IF(struct S_IF p0, float p1) { return p0; }
EXPORT struct S_ID f10_S_SF_ID(struct S_ID p0, float p1) { return p0; }
EXPORT struct S_IP f10_S_SF_IP(struct S_IP p0, float p1) { return p0; }
EXPORT struct S_FI f10_S_SF_FI(struct S_FI p0, float p1) { return p0; }
EXPORT struct S_FF f10_S_SF_FF(struct S_FF p0, float p1) { return p0; }
EXPORT struct S_FD f10_S_SF_FD(struct S_FD p0, float p1) { return p0; }
EXPORT struct S_FP f10_S_SF_FP(struct S_FP p0, float p1) { return p0; }
EXPORT struct S_DI f10_S_SF_DI(struct S_DI p0, float p1) { return p0; }
EXPORT struct S_DF f10_S_SF_DF(struct S_DF p0, float p1) { return p0; }
EXPORT struct S_DD f10_S_SF_DD(struct S_DD p0, float p1) { return p0; }
EXPORT struct S_DP f10_S_SF_DP(struct S_DP p0, float p1) { return p0; }
EXPORT struct S_PI f10_S_SF_PI(struct S_PI p0, float p1) { return p0; }
EXPORT struct S_PF f10_S_SF_PF(struct S_PF p0, float p1) { return p0; }
EXPORT struct S_PD f10_S_SF_PD(struct S_PD p0, float p1) { return p0; }
EXPORT struct S_PP f10_S_SF_PP(struct S_PP p0, float p1) { return p0; }
EXPORT struct S_III f10_S_SF_III(struct S_III p0, float p1) { return p0; }
EXPORT struct S_IIF f10_S_SF_IIF(struct S_IIF p0, float p1) { return p0; }
EXPORT struct S_IID f10_S_SF_IID(struct S_IID p0, float p1) { return p0; }
EXPORT struct S_IIP f10_S_SF_IIP(struct S_IIP p0, float p1) { return p0; }
EXPORT struct S_IFI f10_S_SF_IFI(struct S_IFI p0, float p1) { return p0; }
EXPORT struct S_IFF f10_S_SF_IFF(struct S_IFF p0, float p1) { return p0; }
EXPORT struct S_IFD f10_S_SF_IFD(struct S_IFD p0, float p1) { return p0; }
EXPORT struct S_IFP f11_S_SF_IFP(struct S_IFP p0, float p1) { return p0; }
EXPORT struct S_IDI f11_S_SF_IDI(struct S_IDI p0, float p1) { return p0; }
EXPORT struct S_IDF f11_S_SF_IDF(struct S_IDF p0, float p1) { return p0; }
EXPORT struct S_IDD f11_S_SF_IDD(struct S_IDD p0, float p1) { return p0; }
EXPORT struct S_IDP f11_S_SF_IDP(struct S_IDP p0, float p1) { return p0; }
EXPORT struct S_IPI f11_S_SF_IPI(struct S_IPI p0, float p1) { return p0; }
EXPORT struct S_IPF f11_S_SF_IPF(struct S_IPF p0, float p1) { return p0; }
EXPORT struct S_IPD f11_S_SF_IPD(struct S_IPD p0, float p1) { return p0; }
EXPORT struct S_IPP f11_S_SF_IPP(struct S_IPP p0, float p1) { return p0; }
EXPORT struct S_FII f11_S_SF_FII(struct S_FII p0, float p1) { return p0; }
EXPORT struct S_FIF f11_S_SF_FIF(struct S_FIF p0, float p1) { return p0; }
EXPORT struct S_FID f11_S_SF_FID(struct S_FID p0, float p1) { return p0; }
EXPORT struct S_FIP f11_S_SF_FIP(struct S_FIP p0, float p1) { return p0; }
EXPORT struct S_FFI f11_S_SF_FFI(struct S_FFI p0, float p1) { return p0; }
EXPORT struct S_FFF f11_S_SF_FFF(struct S_FFF p0, float p1) { return p0; }
EXPORT struct S_FFD f11_S_SF_FFD(struct S_FFD p0, float p1) { return p0; }
EXPORT struct S_FFP f11_S_SF_FFP(struct S_FFP p0, float p1) { return p0; }
EXPORT struct S_FDI f11_S_SF_FDI(struct S_FDI p0, float p1) { return p0; }
EXPORT struct S_FDF f11_S_SF_FDF(struct S_FDF p0, float p1) { return p0; }
EXPORT struct S_FDD f11_S_SF_FDD(struct S_FDD p0, float p1) { return p0; }
EXPORT struct S_FDP f11_S_SF_FDP(struct S_FDP p0, float p1) { return p0; }
EXPORT struct S_FPI f11_S_SF_FPI(struct S_FPI p0, float p1) { return p0; }
EXPORT struct S_FPF f11_S_SF_FPF(struct S_FPF p0, float p1) { return p0; }
EXPORT struct S_FPD f11_S_SF_FPD(struct S_FPD p0, float p1) { return p0; }
EXPORT struct S_FPP f11_S_SF_FPP(struct S_FPP p0, float p1) { return p0; }
EXPORT struct S_DII f11_S_SF_DII(struct S_DII p0, float p1) { return p0; }
EXPORT struct S_DIF f11_S_SF_DIF(struct S_DIF p0, float p1) { return p0; }
EXPORT struct S_DID f11_S_SF_DID(struct S_DID p0, float p1) { return p0; }
EXPORT struct S_DIP f11_S_SF_DIP(struct S_DIP p0, float p1) { return p0; }
EXPORT struct S_DFI f11_S_SF_DFI(struct S_DFI p0, float p1) { return p0; }
EXPORT struct S_DFF f11_S_SF_DFF(struct S_DFF p0, float p1) { return p0; }
EXPORT struct S_DFD f11_S_SF_DFD(struct S_DFD p0, float p1) { return p0; }
EXPORT struct S_DFP f11_S_SF_DFP(struct S_DFP p0, float p1) { return p0; }
EXPORT struct S_DDI f11_S_SF_DDI(struct S_DDI p0, float p1) { return p0; }
EXPORT struct S_DDF f11_S_SF_DDF(struct S_DDF p0, float p1) { return p0; }
EXPORT struct S_DDD f11_S_SF_DDD(struct S_DDD p0, float p1) { return p0; }
EXPORT struct S_DDP f11_S_SF_DDP(struct S_DDP p0, float p1) { return p0; }
EXPORT struct S_DPI f11_S_SF_DPI(struct S_DPI p0, float p1) { return p0; }
EXPORT struct S_DPF f11_S_SF_DPF(struct S_DPF p0, float p1) { return p0; }
EXPORT struct S_DPD f11_S_SF_DPD(struct S_DPD p0, float p1) { return p0; }
EXPORT struct S_DPP f11_S_SF_DPP(struct S_DPP p0, float p1) { return p0; }
EXPORT struct S_PII f11_S_SF_PII(struct S_PII p0, float p1) { return p0; }
EXPORT struct S_PIF f11_S_SF_PIF(struct S_PIF p0, float p1) { return p0; }
EXPORT struct S_PID f11_S_SF_PID(struct S_PID p0, float p1) { return p0; }
EXPORT struct S_PIP f11_S_SF_PIP(struct S_PIP p0, float p1) { return p0; }
EXPORT struct S_PFI f11_S_SF_PFI(struct S_PFI p0, float p1) { return p0; }
EXPORT struct S_PFF f11_S_SF_PFF(struct S_PFF p0, float p1) { return p0; }
EXPORT struct S_PFD f11_S_SF_PFD(struct S_PFD p0, float p1) { return p0; }
EXPORT struct S_PFP f11_S_SF_PFP(struct S_PFP p0, float p1) { return p0; }
EXPORT struct S_PDI f11_S_SF_PDI(struct S_PDI p0, float p1) { return p0; }
EXPORT struct S_PDF f11_S_SF_PDF(struct S_PDF p0, float p1) { return p0; }
EXPORT struct S_PDD f11_S_SF_PDD(struct S_PDD p0, float p1) { return p0; }
EXPORT struct S_PDP f11_S_SF_PDP(struct S_PDP p0, float p1) { return p0; }
EXPORT struct S_PPI f11_S_SF_PPI(struct S_PPI p0, float p1) { return p0; }
EXPORT struct S_PPF f11_S_SF_PPF(struct S_PPF p0, float p1) { return p0; }
EXPORT struct S_PPD f11_S_SF_PPD(struct S_PPD p0, float p1) { return p0; }
EXPORT struct S_PPP f11_S_SF_PPP(struct S_PPP p0, float p1) { return p0; }
EXPORT struct S_I f11_S_SD_I(struct S_I p0, double p1) { return p0; }
EXPORT struct S_F f11_S_SD_F(struct S_F p0, double p1) { return p0; }
EXPORT struct S_D f11_S_SD_D(struct S_D p0, double p1) { return p0; }
EXPORT struct S_P f11_S_SD_P(struct S_P p0, double p1) { return p0; }
EXPORT struct S_II f11_S_SD_II(struct S_II p0, double p1) { return p0; }
EXPORT struct S_IF f11_S_SD_IF(struct S_IF p0, double p1) { return p0; }
EXPORT struct S_ID f11_S_SD_ID(struct S_ID p0, double p1) { return p0; }
EXPORT struct S_IP f11_S_SD_IP(struct S_IP p0, double p1) { return p0; }
EXPORT struct S_FI f11_S_SD_FI(struct S_FI p0, double p1) { return p0; }
EXPORT struct S_FF f11_S_SD_FF(struct S_FF p0, double p1) { return p0; }
EXPORT struct S_FD f11_S_SD_FD(struct S_FD p0, double p1) { return p0; }
EXPORT struct S_FP f11_S_SD_FP(struct S_FP p0, double p1) { return p0; }
EXPORT struct S_DI f11_S_SD_DI(struct S_DI p0, double p1) { return p0; }
EXPORT struct S_DF f11_S_SD_DF(struct S_DF p0, double p1) { return p0; }
EXPORT struct S_DD f11_S_SD_DD(struct S_DD p0, double p1) { return p0; }
EXPORT struct S_DP f11_S_SD_DP(struct S_DP p0, double p1) { return p0; }
EXPORT struct S_PI f11_S_SD_PI(struct S_PI p0, double p1) { return p0; }
EXPORT struct S_PF f11_S_SD_PF(struct S_PF p0, double p1) { return p0; }
EXPORT struct S_PD f11_S_SD_PD(struct S_PD p0, double p1) { return p0; }
EXPORT struct S_PP f11_S_SD_PP(struct S_PP p0, double p1) { return p0; }
EXPORT struct S_III f11_S_SD_III(struct S_III p0, double p1) { return p0; }
EXPORT struct S_IIF f11_S_SD_IIF(struct S_IIF p0, double p1) { return p0; }
EXPORT struct S_IID f11_S_SD_IID(struct S_IID p0, double p1) { return p0; }
EXPORT struct S_IIP f11_S_SD_IIP(struct S_IIP p0, double p1) { return p0; }
EXPORT struct S_IFI f11_S_SD_IFI(struct S_IFI p0, double p1) { return p0; }
EXPORT struct S_IFF f11_S_SD_IFF(struct S_IFF p0, double p1) { return p0; }
EXPORT struct S_IFD f11_S_SD_IFD(struct S_IFD p0, double p1) { return p0; }
EXPORT struct S_IFP f11_S_SD_IFP(struct S_IFP p0, double p1) { return p0; }
EXPORT struct S_IDI f11_S_SD_IDI(struct S_IDI p0, double p1) { return p0; }
EXPORT struct S_IDF f11_S_SD_IDF(struct S_IDF p0, double p1) { return p0; }
EXPORT struct S_IDD f11_S_SD_IDD(struct S_IDD p0, double p1) { return p0; }
EXPORT struct S_IDP f11_S_SD_IDP(struct S_IDP p0, double p1) { return p0; }
EXPORT struct S_IPI f11_S_SD_IPI(struct S_IPI p0, double p1) { return p0; }
EXPORT struct S_IPF f11_S_SD_IPF(struct S_IPF p0, double p1) { return p0; }
EXPORT struct S_IPD f11_S_SD_IPD(struct S_IPD p0, double p1) { return p0; }
EXPORT struct S_IPP f11_S_SD_IPP(struct S_IPP p0, double p1) { return p0; }
EXPORT struct S_FII f11_S_SD_FII(struct S_FII p0, double p1) { return p0; }
EXPORT struct S_FIF f11_S_SD_FIF(struct S_FIF p0, double p1) { return p0; }
EXPORT struct S_FID f11_S_SD_FID(struct S_FID p0, double p1) { return p0; }
EXPORT struct S_FIP f11_S_SD_FIP(struct S_FIP p0, double p1) { return p0; }
EXPORT struct S_FFI f11_S_SD_FFI(struct S_FFI p0, double p1) { return p0; }
EXPORT struct S_FFF f11_S_SD_FFF(struct S_FFF p0, double p1) { return p0; }
EXPORT struct S_FFD f11_S_SD_FFD(struct S_FFD p0, double p1) { return p0; }
EXPORT struct S_FFP f11_S_SD_FFP(struct S_FFP p0, double p1) { return p0; }
EXPORT struct S_FDI f11_S_SD_FDI(struct S_FDI p0, double p1) { return p0; }
EXPORT struct S_FDF f11_S_SD_FDF(struct S_FDF p0, double p1) { return p0; }
EXPORT struct S_FDD f11_S_SD_FDD(struct S_FDD p0, double p1) { return p0; }
EXPORT struct S_FDP f11_S_SD_FDP(struct S_FDP p0, double p1) { return p0; }
EXPORT struct S_FPI f11_S_SD_FPI(struct S_FPI p0, double p1) { return p0; }
EXPORT struct S_FPF f11_S_SD_FPF(struct S_FPF p0, double p1) { return p0; }
EXPORT struct S_FPD f11_S_SD_FPD(struct S_FPD p0, double p1) { return p0; }
EXPORT struct S_FPP f11_S_SD_FPP(struct S_FPP p0, double p1) { return p0; }
EXPORT struct S_DII f11_S_SD_DII(struct S_DII p0, double p1) { return p0; }
EXPORT struct S_DIF f11_S_SD_DIF(struct S_DIF p0, double p1) { return p0; }
EXPORT struct S_DID f11_S_SD_DID(struct S_DID p0, double p1) { return p0; }
EXPORT struct S_DIP f11_S_SD_DIP(struct S_DIP p0, double p1) { return p0; }
EXPORT struct S_DFI f11_S_SD_DFI(struct S_DFI p0, double p1) { return p0; }
EXPORT struct S_DFF f11_S_SD_DFF(struct S_DFF p0, double p1) { return p0; }
EXPORT struct S_DFD f11_S_SD_DFD(struct S_DFD p0, double p1) { return p0; }
EXPORT struct S_DFP f11_S_SD_DFP(struct S_DFP p0, double p1) { return p0; }
EXPORT struct S_DDI f11_S_SD_DDI(struct S_DDI p0, double p1) { return p0; }
EXPORT struct S_DDF f11_S_SD_DDF(struct S_DDF p0, double p1) { return p0; }
EXPORT struct S_DDD f11_S_SD_DDD(struct S_DDD p0, double p1) { return p0; }
EXPORT struct S_DDP f11_S_SD_DDP(struct S_DDP p0, double p1) { return p0; }
EXPORT struct S_DPI f11_S_SD_DPI(struct S_DPI p0, double p1) { return p0; }
EXPORT struct S_DPF f11_S_SD_DPF(struct S_DPF p0, double p1) { return p0; }
EXPORT struct S_DPD f11_S_SD_DPD(struct S_DPD p0, double p1) { return p0; }
EXPORT struct S_DPP f11_S_SD_DPP(struct S_DPP p0, double p1) { return p0; }
EXPORT struct S_PII f11_S_SD_PII(struct S_PII p0, double p1) { return p0; }
EXPORT struct S_PIF f11_S_SD_PIF(struct S_PIF p0, double p1) { return p0; }
EXPORT struct S_PID f11_S_SD_PID(struct S_PID p0, double p1) { return p0; }
EXPORT struct S_PIP f11_S_SD_PIP(struct S_PIP p0, double p1) { return p0; }
EXPORT struct S_PFI f11_S_SD_PFI(struct S_PFI p0, double p1) { return p0; }
EXPORT struct S_PFF f11_S_SD_PFF(struct S_PFF p0, double p1) { return p0; }
EXPORT struct S_PFD f11_S_SD_PFD(struct S_PFD p0, double p1) { return p0; }
EXPORT struct S_PFP f11_S_SD_PFP(struct S_PFP p0, double p1) { return p0; }
EXPORT struct S_PDI f11_S_SD_PDI(struct S_PDI p0, double p1) { return p0; }
EXPORT struct S_PDF f11_S_SD_PDF(struct S_PDF p0, double p1) { return p0; }
EXPORT struct S_PDD f11_S_SD_PDD(struct S_PDD p0, double p1) { return p0; }
EXPORT struct S_PDP f11_S_SD_PDP(struct S_PDP p0, double p1) { return p0; }
EXPORT struct S_PPI f11_S_SD_PPI(struct S_PPI p0, double p1) { return p0; }
EXPORT struct S_PPF f11_S_SD_PPF(struct S_PPF p0, double p1) { return p0; }
EXPORT struct S_PPD f11_S_SD_PPD(struct S_PPD p0, double p1) { return p0; }
EXPORT struct S_PPP f11_S_SD_PPP(struct S_PPP p0, double p1) { return p0; }
EXPORT struct S_I f11_S_SP_I(struct S_I p0, void* p1) { return p0; }
EXPORT struct S_F f11_S_SP_F(struct S_F p0, void* p1) { return p0; }
EXPORT struct S_D f11_S_SP_D(struct S_D p0, void* p1) { return p0; }
EXPORT struct S_P f11_S_SP_P(struct S_P p0, void* p1) { return p0; }
EXPORT struct S_II f11_S_SP_II(struct S_II p0, void* p1) { return p0; }
EXPORT struct S_IF f11_S_SP_IF(struct S_IF p0, void* p1) { return p0; }
EXPORT struct S_ID f11_S_SP_ID(struct S_ID p0, void* p1) { return p0; }
EXPORT struct S_IP f11_S_SP_IP(struct S_IP p0, void* p1) { return p0; }
EXPORT struct S_FI f11_S_SP_FI(struct S_FI p0, void* p1) { return p0; }
EXPORT struct S_FF f11_S_SP_FF(struct S_FF p0, void* p1) { return p0; }
EXPORT struct S_FD f11_S_SP_FD(struct S_FD p0, void* p1) { return p0; }
EXPORT struct S_FP f11_S_SP_FP(struct S_FP p0, void* p1) { return p0; }
EXPORT struct S_DI f11_S_SP_DI(struct S_DI p0, void* p1) { return p0; }
EXPORT struct S_DF f11_S_SP_DF(struct S_DF p0, void* p1) { return p0; }
EXPORT struct S_DD f11_S_SP_DD(struct S_DD p0, void* p1) { return p0; }
EXPORT struct S_DP f11_S_SP_DP(struct S_DP p0, void* p1) { return p0; }
EXPORT struct S_PI f11_S_SP_PI(struct S_PI p0, void* p1) { return p0; }
EXPORT struct S_PF f11_S_SP_PF(struct S_PF p0, void* p1) { return p0; }
EXPORT struct S_PD f11_S_SP_PD(struct S_PD p0, void* p1) { return p0; }
EXPORT struct S_PP f11_S_SP_PP(struct S_PP p0, void* p1) { return p0; }
EXPORT struct S_III f11_S_SP_III(struct S_III p0, void* p1) { return p0; }
EXPORT struct S_IIF f11_S_SP_IIF(struct S_IIF p0, void* p1) { return p0; }
EXPORT struct S_IID f11_S_SP_IID(struct S_IID p0, void* p1) { return p0; }
EXPORT struct S_IIP f11_S_SP_IIP(struct S_IIP p0, void* p1) { return p0; }
EXPORT struct S_IFI f11_S_SP_IFI(struct S_IFI p0, void* p1) { return p0; }
EXPORT struct S_IFF f11_S_SP_IFF(struct S_IFF p0, void* p1) { return p0; }
EXPORT struct S_IFD f11_S_SP_IFD(struct S_IFD p0, void* p1) { return p0; }
EXPORT struct S_IFP f11_S_SP_IFP(struct S_IFP p0, void* p1) { return p0; }
EXPORT struct S_IDI f11_S_SP_IDI(struct S_IDI p0, void* p1) { return p0; }
EXPORT struct S_IDF f11_S_SP_IDF(struct S_IDF p0, void* p1) { return p0; }
EXPORT struct S_IDD f11_S_SP_IDD(struct S_IDD p0, void* p1) { return p0; }
EXPORT struct S_IDP f11_S_SP_IDP(struct S_IDP p0, void* p1) { return p0; }
EXPORT struct S_IPI f11_S_SP_IPI(struct S_IPI p0, void* p1) { return p0; }
EXPORT struct S_IPF f11_S_SP_IPF(struct S_IPF p0, void* p1) { return p0; }
EXPORT struct S_IPD f11_S_SP_IPD(struct S_IPD p0, void* p1) { return p0; }
EXPORT struct S_IPP f11_S_SP_IPP(struct S_IPP p0, void* p1) { return p0; }
EXPORT struct S_FII f11_S_SP_FII(struct S_FII p0, void* p1) { return p0; }
EXPORT struct S_FIF f11_S_SP_FIF(struct S_FIF p0, void* p1) { return p0; }
EXPORT struct S_FID f11_S_SP_FID(struct S_FID p0, void* p1) { return p0; }
EXPORT struct S_FIP f11_S_SP_FIP(struct S_FIP p0, void* p1) { return p0; }
EXPORT struct S_FFI f11_S_SP_FFI(struct S_FFI p0, void* p1) { return p0; }
EXPORT struct S_FFF f11_S_SP_FFF(struct S_FFF p0, void* p1) { return p0; }
EXPORT struct S_FFD f11_S_SP_FFD(struct S_FFD p0, void* p1) { return p0; }
EXPORT struct S_FFP f11_S_SP_FFP(struct S_FFP p0, void* p1) { return p0; }
EXPORT struct S_FDI f11_S_SP_FDI(struct S_FDI p0, void* p1) { return p0; }
EXPORT struct S_FDF f11_S_SP_FDF(struct S_FDF p0, void* p1) { return p0; }
EXPORT struct S_FDD f11_S_SP_FDD(struct S_FDD p0, void* p1) { return p0; }
EXPORT struct S_FDP f11_S_SP_FDP(struct S_FDP p0, void* p1) { return p0; }
EXPORT struct S_FPI f11_S_SP_FPI(struct S_FPI p0, void* p1) { return p0; }
EXPORT struct S_FPF f11_S_SP_FPF(struct S_FPF p0, void* p1) { return p0; }
EXPORT struct S_FPD f11_S_SP_FPD(struct S_FPD p0, void* p1) { return p0; }
EXPORT struct S_FPP f11_S_SP_FPP(struct S_FPP p0, void* p1) { return p0; }
EXPORT struct S_DII f11_S_SP_DII(struct S_DII p0, void* p1) { return p0; }
EXPORT struct S_DIF f11_S_SP_DIF(struct S_DIF p0, void* p1) { return p0; }
EXPORT struct S_DID f11_S_SP_DID(struct S_DID p0, void* p1) { return p0; }
EXPORT struct S_DIP f11_S_SP_DIP(struct S_DIP p0, void* p1) { return p0; }
EXPORT struct S_DFI f11_S_SP_DFI(struct S_DFI p0, void* p1) { return p0; }
EXPORT struct S_DFF f11_S_SP_DFF(struct S_DFF p0, void* p1) { return p0; }
EXPORT struct S_DFD f11_S_SP_DFD(struct S_DFD p0, void* p1) { return p0; }
EXPORT struct S_DFP f11_S_SP_DFP(struct S_DFP p0, void* p1) { return p0; }
EXPORT struct S_DDI f11_S_SP_DDI(struct S_DDI p0, void* p1) { return p0; }
EXPORT struct S_DDF f11_S_SP_DDF(struct S_DDF p0, void* p1) { return p0; }
EXPORT struct S_DDD f11_S_SP_DDD(struct S_DDD p0, void* p1) { return p0; }
EXPORT struct S_DDP f11_S_SP_DDP(struct S_DDP p0, void* p1) { return p0; }
EXPORT struct S_DPI f11_S_SP_DPI(struct S_DPI p0, void* p1) { return p0; }
EXPORT struct S_DPF f11_S_SP_DPF(struct S_DPF p0, void* p1) { return p0; }
EXPORT struct S_DPD f11_S_SP_DPD(struct S_DPD p0, void* p1) { return p0; }
EXPORT struct S_DPP f11_S_SP_DPP(struct S_DPP p0, void* p1) { return p0; }
EXPORT struct S_PII f11_S_SP_PII(struct S_PII p0, void* p1) { return p0; }
EXPORT struct S_PIF f11_S_SP_PIF(struct S_PIF p0, void* p1) { return p0; }
EXPORT struct S_PID f11_S_SP_PID(struct S_PID p0, void* p1) { return p0; }
EXPORT struct S_PIP f11_S_SP_PIP(struct S_PIP p0, void* p1) { return p0; }
EXPORT struct S_PFI f11_S_SP_PFI(struct S_PFI p0, void* p1) { return p0; }
EXPORT struct S_PFF f11_S_SP_PFF(struct S_PFF p0, void* p1) { return p0; }
EXPORT struct S_PFD f11_S_SP_PFD(struct S_PFD p0, void* p1) { return p0; }
EXPORT struct S_PFP f11_S_SP_PFP(struct S_PFP p0, void* p1) { return p0; }
EXPORT struct S_PDI f11_S_SP_PDI(struct S_PDI p0, void* p1) { return p0; }
EXPORT struct S_PDF f11_S_SP_PDF(struct S_PDF p0, void* p1) { return p0; }
EXPORT struct S_PDD f11_S_SP_PDD(struct S_PDD p0, void* p1) { return p0; }
EXPORT struct S_PDP f11_S_SP_PDP(struct S_PDP p0, void* p1) { return p0; }
EXPORT struct S_PPI f11_S_SP_PPI(struct S_PPI p0, void* p1) { return p0; }
EXPORT struct S_PPF f11_S_SP_PPF(struct S_PPF p0, void* p1) { return p0; }
EXPORT struct S_PPD f11_S_SP_PPD(struct S_PPD p0, void* p1) { return p0; }
EXPORT struct S_PPP f11_S_SP_PPP(struct S_PPP p0, void* p1) { return p0; }
EXPORT struct S_I f11_S_SS_I(struct S_I p0, struct S_I p1) { return p0; }
EXPORT struct S_F f11_S_SS_F(struct S_F p0, struct S_F p1) { return p0; }
EXPORT struct S_D f11_S_SS_D(struct S_D p0, struct S_D p1) { return p0; }
EXPORT struct S_P f11_S_SS_P(struct S_P p0, struct S_P p1) { return p0; }
EXPORT struct S_II f11_S_SS_II(struct S_II p0, struct S_II p1) { return p0; }
EXPORT struct S_IF f11_S_SS_IF(struct S_IF p0, struct S_IF p1) { return p0; }
EXPORT struct S_ID f11_S_SS_ID(struct S_ID p0, struct S_ID p1) { return p0; }
EXPORT struct S_IP f11_S_SS_IP(struct S_IP p0, struct S_IP p1) { return p0; }
EXPORT struct S_FI f11_S_SS_FI(struct S_FI p0, struct S_FI p1) { return p0; }
EXPORT struct S_FF f11_S_SS_FF(struct S_FF p0, struct S_FF p1) { return p0; }
EXPORT struct S_FD f11_S_SS_FD(struct S_FD p0, struct S_FD p1) { return p0; }
EXPORT struct S_FP f11_S_SS_FP(struct S_FP p0, struct S_FP p1) { return p0; }
EXPORT struct S_DI f11_S_SS_DI(struct S_DI p0, struct S_DI p1) { return p0; }
EXPORT struct S_DF f11_S_SS_DF(struct S_DF p0, struct S_DF p1) { return p0; }
EXPORT struct S_DD f11_S_SS_DD(struct S_DD p0, struct S_DD p1) { return p0; }
EXPORT struct S_DP f11_S_SS_DP(struct S_DP p0, struct S_DP p1) { return p0; }
EXPORT struct S_PI f11_S_SS_PI(struct S_PI p0, struct S_PI p1) { return p0; }
EXPORT struct S_PF f11_S_SS_PF(struct S_PF p0, struct S_PF p1) { return p0; }
EXPORT struct S_PD f11_S_SS_PD(struct S_PD p0, struct S_PD p1) { return p0; }
EXPORT struct S_PP f11_S_SS_PP(struct S_PP p0, struct S_PP p1) { return p0; }
EXPORT struct S_III f11_S_SS_III(struct S_III p0, struct S_III p1) { return p0; }
EXPORT struct S_IIF f11_S_SS_IIF(struct S_IIF p0, struct S_IIF p1) { return p0; }
EXPORT struct S_IID f11_S_SS_IID(struct S_IID p0, struct S_IID p1) { return p0; }
EXPORT struct S_IIP f11_S_SS_IIP(struct S_IIP p0, struct S_IIP p1) { return p0; }
EXPORT struct S_IFI f11_S_SS_IFI(struct S_IFI p0, struct S_IFI p1) { return p0; }
EXPORT struct S_IFF f11_S_SS_IFF(struct S_IFF p0, struct S_IFF p1) { return p0; }
EXPORT struct S_IFD f11_S_SS_IFD(struct S_IFD p0, struct S_IFD p1) { return p0; }
EXPORT struct S_IFP f11_S_SS_IFP(struct S_IFP p0, struct S_IFP p1) { return p0; }
EXPORT struct S_IDI f11_S_SS_IDI(struct S_IDI p0, struct S_IDI p1) { return p0; }
EXPORT struct S_IDF f11_S_SS_IDF(struct S_IDF p0, struct S_IDF p1) { return p0; }
EXPORT struct S_IDD f11_S_SS_IDD(struct S_IDD p0, struct S_IDD p1) { return p0; }
EXPORT struct S_IDP f11_S_SS_IDP(struct S_IDP p0, struct S_IDP p1) { return p0; }
EXPORT struct S_IPI f11_S_SS_IPI(struct S_IPI p0, struct S_IPI p1) { return p0; }
EXPORT struct S_IPF f11_S_SS_IPF(struct S_IPF p0, struct S_IPF p1) { return p0; }
EXPORT struct S_IPD f11_S_SS_IPD(struct S_IPD p0, struct S_IPD p1) { return p0; }
EXPORT struct S_IPP f11_S_SS_IPP(struct S_IPP p0, struct S_IPP p1) { return p0; }
EXPORT struct S_FII f11_S_SS_FII(struct S_FII p0, struct S_FII p1) { return p0; }
EXPORT struct S_FIF f11_S_SS_FIF(struct S_FIF p0, struct S_FIF p1) { return p0; }
EXPORT struct S_FID f11_S_SS_FID(struct S_FID p0, struct S_FID p1) { return p0; }
EXPORT struct S_FIP f11_S_SS_FIP(struct S_FIP p0, struct S_FIP p1) { return p0; }
EXPORT struct S_FFI f11_S_SS_FFI(struct S_FFI p0, struct S_FFI p1) { return p0; }
EXPORT struct S_FFF f11_S_SS_FFF(struct S_FFF p0, struct S_FFF p1) { return p0; }
EXPORT struct S_FFD f11_S_SS_FFD(struct S_FFD p0, struct S_FFD p1) { return p0; }
EXPORT struct S_FFP f11_S_SS_FFP(struct S_FFP p0, struct S_FFP p1) { return p0; }
EXPORT struct S_FDI f11_S_SS_FDI(struct S_FDI p0, struct S_FDI p1) { return p0; }
EXPORT struct S_FDF f11_S_SS_FDF(struct S_FDF p0, struct S_FDF p1) { return p0; }
EXPORT struct S_FDD f11_S_SS_FDD(struct S_FDD p0, struct S_FDD p1) { return p0; }
EXPORT struct S_FDP f11_S_SS_FDP(struct S_FDP p0, struct S_FDP p1) { return p0; }
EXPORT struct S_FPI f11_S_SS_FPI(struct S_FPI p0, struct S_FPI p1) { return p0; }
EXPORT struct S_FPF f11_S_SS_FPF(struct S_FPF p0, struct S_FPF p1) { return p0; }
EXPORT struct S_FPD f11_S_SS_FPD(struct S_FPD p0, struct S_FPD p1) { return p0; }
EXPORT struct S_FPP f11_S_SS_FPP(struct S_FPP p0, struct S_FPP p1) { return p0; }
EXPORT struct S_DII f11_S_SS_DII(struct S_DII p0, struct S_DII p1) { return p0; }
EXPORT struct S_DIF f11_S_SS_DIF(struct S_DIF p0, struct S_DIF p1) { return p0; }
EXPORT struct S_DID f11_S_SS_DID(struct S_DID p0, struct S_DID p1) { return p0; }
EXPORT struct S_DIP f11_S_SS_DIP(struct S_DIP p0, struct S_DIP p1) { return p0; }
EXPORT struct S_DFI f11_S_SS_DFI(struct S_DFI p0, struct S_DFI p1) { return p0; }
EXPORT struct S_DFF f11_S_SS_DFF(struct S_DFF p0, struct S_DFF p1) { return p0; }
EXPORT struct S_DFD f11_S_SS_DFD(struct S_DFD p0, struct S_DFD p1) { return p0; }
EXPORT struct S_DFP f11_S_SS_DFP(struct S_DFP p0, struct S_DFP p1) { return p0; }
EXPORT struct S_DDI f11_S_SS_DDI(struct S_DDI p0, struct S_DDI p1) { return p0; }
EXPORT struct S_DDF f11_S_SS_DDF(struct S_DDF p0, struct S_DDF p1) { return p0; }
EXPORT struct S_DDD f11_S_SS_DDD(struct S_DDD p0, struct S_DDD p1) { return p0; }
EXPORT struct S_DDP f11_S_SS_DDP(struct S_DDP p0, struct S_DDP p1) { return p0; }
EXPORT struct S_DPI f11_S_SS_DPI(struct S_DPI p0, struct S_DPI p1) { return p0; }
EXPORT struct S_DPF f11_S_SS_DPF(struct S_DPF p0, struct S_DPF p1) { return p0; }
EXPORT struct S_DPD f11_S_SS_DPD(struct S_DPD p0, struct S_DPD p1) { return p0; }
EXPORT struct S_DPP f11_S_SS_DPP(struct S_DPP p0, struct S_DPP p1) { return p0; }
EXPORT struct S_PII f11_S_SS_PII(struct S_PII p0, struct S_PII p1) { return p0; }
EXPORT struct S_PIF f11_S_SS_PIF(struct S_PIF p0, struct S_PIF p1) { return p0; }
EXPORT struct S_PID f11_S_SS_PID(struct S_PID p0, struct S_PID p1) { return p0; }
EXPORT struct S_PIP f11_S_SS_PIP(struct S_PIP p0, struct S_PIP p1) { return p0; }
EXPORT struct S_PFI f11_S_SS_PFI(struct S_PFI p0, struct S_PFI p1) { return p0; }
EXPORT struct S_PFF f11_S_SS_PFF(struct S_PFF p0, struct S_PFF p1) { return p0; }
EXPORT struct S_PFD f11_S_SS_PFD(struct S_PFD p0, struct S_PFD p1) { return p0; }
EXPORT struct S_PFP f11_S_SS_PFP(struct S_PFP p0, struct S_PFP p1) { return p0; }
EXPORT struct S_PDI f11_S_SS_PDI(struct S_PDI p0, struct S_PDI p1) { return p0; }
EXPORT struct S_PDF f11_S_SS_PDF(struct S_PDF p0, struct S_PDF p1) { return p0; }
EXPORT struct S_PDD f11_S_SS_PDD(struct S_PDD p0, struct S_PDD p1) { return p0; }
EXPORT struct S_PDP f11_S_SS_PDP(struct S_PDP p0, struct S_PDP p1) { return p0; }
EXPORT struct S_PPI f11_S_SS_PPI(struct S_PPI p0, struct S_PPI p1) { return p0; }
EXPORT struct S_PPF f11_S_SS_PPF(struct S_PPF p0, struct S_PPF p1) { return p0; }
EXPORT struct S_PPD f11_S_SS_PPD(struct S_PPD p0, struct S_PPD p1) { return p0; }
EXPORT struct S_PPP f11_S_SS_PPP(struct S_PPP p0, struct S_PPP p1) { return p0; }
EXPORT int f11_I_III_(int p0, int p1, int p2) { return p0; }
EXPORT int f11_I_IIF_(int p0, int p1, float p2) { return p0; }
EXPORT int f11_I_IID_(int p0, int p1, double p2) { return p0; }
EXPORT int f11_I_IIP_(int p0, int p1, void* p2) { return p0; }
EXPORT int f11_I_IIS_I(int p0, int p1, struct S_I p2) { return p0; }
EXPORT int f11_I_IIS_F(int p0, int p1, struct S_F p2) { return p0; }
EXPORT int f11_I_IIS_D(int p0, int p1, struct S_D p2) { return p0; }
EXPORT int f11_I_IIS_P(int p0, int p1, struct S_P p2) { return p0; }
EXPORT int f11_I_IIS_II(int p0, int p1, struct S_II p2) { return p0; }
EXPORT int f11_I_IIS_IF(int p0, int p1, struct S_IF p2) { return p0; }
EXPORT int f11_I_IIS_ID(int p0, int p1, struct S_ID p2) { return p0; }
EXPORT int f11_I_IIS_IP(int p0, int p1, struct S_IP p2) { return p0; }
EXPORT int f11_I_IIS_FI(int p0, int p1, struct S_FI p2) { return p0; }
EXPORT int f11_I_IIS_FF(int p0, int p1, struct S_FF p2) { return p0; }
EXPORT int f11_I_IIS_FD(int p0, int p1, struct S_FD p2) { return p0; }
EXPORT int f11_I_IIS_FP(int p0, int p1, struct S_FP p2) { return p0; }
EXPORT int f11_I_IIS_DI(int p0, int p1, struct S_DI p2) { return p0; }
EXPORT int f11_I_IIS_DF(int p0, int p1, struct S_DF p2) { return p0; }
EXPORT int f11_I_IIS_DD(int p0, int p1, struct S_DD p2) { return p0; }
EXPORT int f11_I_IIS_DP(int p0, int p1, struct S_DP p2) { return p0; }
EXPORT int f11_I_IIS_PI(int p0, int p1, struct S_PI p2) { return p0; }
EXPORT int f11_I_IIS_PF(int p0, int p1, struct S_PF p2) { return p0; }
EXPORT int f11_I_IIS_PD(int p0, int p1, struct S_PD p2) { return p0; }
EXPORT int f11_I_IIS_PP(int p0, int p1, struct S_PP p2) { return p0; }
EXPORT int f11_I_IIS_III(int p0, int p1, struct S_III p2) { return p0; }
EXPORT int f11_I_IIS_IIF(int p0, int p1, struct S_IIF p2) { return p0; }
EXPORT int f11_I_IIS_IID(int p0, int p1, struct S_IID p2) { return p0; }
EXPORT int f11_I_IIS_IIP(int p0, int p1, struct S_IIP p2) { return p0; }
EXPORT int f11_I_IIS_IFI(int p0, int p1, struct S_IFI p2) { return p0; }
EXPORT int f11_I_IIS_IFF(int p0, int p1, struct S_IFF p2) { return p0; }
EXPORT int f11_I_IIS_IFD(int p0, int p1, struct S_IFD p2) { return p0; }
EXPORT int f11_I_IIS_IFP(int p0, int p1, struct S_IFP p2) { return p0; }
EXPORT int f11_I_IIS_IDI(int p0, int p1, struct S_IDI p2) { return p0; }
EXPORT int f11_I_IIS_IDF(int p0, int p1, struct S_IDF p2) { return p0; }
EXPORT int f11_I_IIS_IDD(int p0, int p1, struct S_IDD p2) { return p0; }
EXPORT int f11_I_IIS_IDP(int p0, int p1, struct S_IDP p2) { return p0; }
EXPORT int f11_I_IIS_IPI(int p0, int p1, struct S_IPI p2) { return p0; }
EXPORT int f11_I_IIS_IPF(int p0, int p1, struct S_IPF p2) { return p0; }
EXPORT int f11_I_IIS_IPD(int p0, int p1, struct S_IPD p2) { return p0; }
EXPORT int f11_I_IIS_IPP(int p0, int p1, struct S_IPP p2) { return p0; }
EXPORT int f11_I_IIS_FII(int p0, int p1, struct S_FII p2) { return p0; }
EXPORT int f11_I_IIS_FIF(int p0, int p1, struct S_FIF p2) { return p0; }
EXPORT int f11_I_IIS_FID(int p0, int p1, struct S_FID p2) { return p0; }
EXPORT int f11_I_IIS_FIP(int p0, int p1, struct S_FIP p2) { return p0; }
EXPORT int f11_I_IIS_FFI(int p0, int p1, struct S_FFI p2) { return p0; }
EXPORT int f11_I_IIS_FFF(int p0, int p1, struct S_FFF p2) { return p0; }
EXPORT int f11_I_IIS_FFD(int p0, int p1, struct S_FFD p2) { return p0; }
EXPORT int f11_I_IIS_FFP(int p0, int p1, struct S_FFP p2) { return p0; }
EXPORT int f11_I_IIS_FDI(int p0, int p1, struct S_FDI p2) { return p0; }
EXPORT int f11_I_IIS_FDF(int p0, int p1, struct S_FDF p2) { return p0; }
EXPORT int f11_I_IIS_FDD(int p0, int p1, struct S_FDD p2) { return p0; }
EXPORT int f11_I_IIS_FDP(int p0, int p1, struct S_FDP p2) { return p0; }
EXPORT int f11_I_IIS_FPI(int p0, int p1, struct S_FPI p2) { return p0; }
EXPORT int f11_I_IIS_FPF(int p0, int p1, struct S_FPF p2) { return p0; }
EXPORT int f11_I_IIS_FPD(int p0, int p1, struct S_FPD p2) { return p0; }
EXPORT int f11_I_IIS_FPP(int p0, int p1, struct S_FPP p2) { return p0; }
EXPORT int f11_I_IIS_DII(int p0, int p1, struct S_DII p2) { return p0; }
EXPORT int f11_I_IIS_DIF(int p0, int p1, struct S_DIF p2) { return p0; }
EXPORT int f11_I_IIS_DID(int p0, int p1, struct S_DID p2) { return p0; }
EXPORT int f11_I_IIS_DIP(int p0, int p1, struct S_DIP p2) { return p0; }
EXPORT int f11_I_IIS_DFI(int p0, int p1, struct S_DFI p2) { return p0; }
EXPORT int f11_I_IIS_DFF(int p0, int p1, struct S_DFF p2) { return p0; }
EXPORT int f11_I_IIS_DFD(int p0, int p1, struct S_DFD p2) { return p0; }
EXPORT int f11_I_IIS_DFP(int p0, int p1, struct S_DFP p2) { return p0; }
EXPORT int f11_I_IIS_DDI(int p0, int p1, struct S_DDI p2) { return p0; }
EXPORT int f11_I_IIS_DDF(int p0, int p1, struct S_DDF p2) { return p0; }
EXPORT int f11_I_IIS_DDD(int p0, int p1, struct S_DDD p2) { return p0; }
EXPORT int f11_I_IIS_DDP(int p0, int p1, struct S_DDP p2) { return p0; }
EXPORT int f11_I_IIS_DPI(int p0, int p1, struct S_DPI p2) { return p0; }
EXPORT int f11_I_IIS_DPF(int p0, int p1, struct S_DPF p2) { return p0; }
EXPORT int f11_I_IIS_DPD(int p0, int p1, struct S_DPD p2) { return p0; }
EXPORT int f11_I_IIS_DPP(int p0, int p1, struct S_DPP p2) { return p0; }
EXPORT int f11_I_IIS_PII(int p0, int p1, struct S_PII p2) { return p0; }
EXPORT int f11_I_IIS_PIF(int p0, int p1, struct S_PIF p2) { return p0; }
EXPORT int f11_I_IIS_PID(int p0, int p1, struct S_PID p2) { return p0; }
EXPORT int f11_I_IIS_PIP(int p0, int p1, struct S_PIP p2) { return p0; }
EXPORT int f11_I_IIS_PFI(int p0, int p1, struct S_PFI p2) { return p0; }
EXPORT int f11_I_IIS_PFF(int p0, int p1, struct S_PFF p2) { return p0; }
EXPORT int f11_I_IIS_PFD(int p0, int p1, struct S_PFD p2) { return p0; }
EXPORT int f11_I_IIS_PFP(int p0, int p1, struct S_PFP p2) { return p0; }
EXPORT int f11_I_IIS_PDI(int p0, int p1, struct S_PDI p2) { return p0; }
EXPORT int f11_I_IIS_PDF(int p0, int p1, struct S_PDF p2) { return p0; }
EXPORT int f11_I_IIS_PDD(int p0, int p1, struct S_PDD p2) { return p0; }
EXPORT int f11_I_IIS_PDP(int p0, int p1, struct S_PDP p2) { return p0; }
EXPORT int f11_I_IIS_PPI(int p0, int p1, struct S_PPI p2) { return p0; }
EXPORT int f11_I_IIS_PPF(int p0, int p1, struct S_PPF p2) { return p0; }
EXPORT int f11_I_IIS_PPD(int p0, int p1, struct S_PPD p2) { return p0; }
EXPORT int f11_I_IIS_PPP(int p0, int p1, struct S_PPP p2) { return p0; }
EXPORT int f11_I_IFI_(int p0, float p1, int p2) { return p0; }
EXPORT int f11_I_IFF_(int p0, float p1, float p2) { return p0; }
EXPORT int f11_I_IFD_(int p0, float p1, double p2) { return p0; }
EXPORT int f11_I_IFP_(int p0, float p1, void* p2) { return p0; }
EXPORT int f11_I_IFS_I(int p0, float p1, struct S_I p2) { return p0; }
EXPORT int f11_I_IFS_F(int p0, float p1, struct S_F p2) { return p0; }
EXPORT int f11_I_IFS_D(int p0, float p1, struct S_D p2) { return p0; }
EXPORT int f11_I_IFS_P(int p0, float p1, struct S_P p2) { return p0; }
EXPORT int f11_I_IFS_II(int p0, float p1, struct S_II p2) { return p0; }
EXPORT int f11_I_IFS_IF(int p0, float p1, struct S_IF p2) { return p0; }
EXPORT int f11_I_IFS_ID(int p0, float p1, struct S_ID p2) { return p0; }
EXPORT int f11_I_IFS_IP(int p0, float p1, struct S_IP p2) { return p0; }
EXPORT int f11_I_IFS_FI(int p0, float p1, struct S_FI p2) { return p0; }
EXPORT int f11_I_IFS_FF(int p0, float p1, struct S_FF p2) { return p0; }
EXPORT int f11_I_IFS_FD(int p0, float p1, struct S_FD p2) { return p0; }
EXPORT int f11_I_IFS_FP(int p0, float p1, struct S_FP p2) { return p0; }
EXPORT int f11_I_IFS_DI(int p0, float p1, struct S_DI p2) { return p0; }
EXPORT int f11_I_IFS_DF(int p0, float p1, struct S_DF p2) { return p0; }
EXPORT int f11_I_IFS_DD(int p0, float p1, struct S_DD p2) { return p0; }
EXPORT int f11_I_IFS_DP(int p0, float p1, struct S_DP p2) { return p0; }
EXPORT int f11_I_IFS_PI(int p0, float p1, struct S_PI p2) { return p0; }
EXPORT int f11_I_IFS_PF(int p0, float p1, struct S_PF p2) { return p0; }
EXPORT int f11_I_IFS_PD(int p0, float p1, struct S_PD p2) { return p0; }
EXPORT int f11_I_IFS_PP(int p0, float p1, struct S_PP p2) { return p0; }
EXPORT int f11_I_IFS_III(int p0, float p1, struct S_III p2) { return p0; }
EXPORT int f11_I_IFS_IIF(int p0, float p1, struct S_IIF p2) { return p0; }
EXPORT int f11_I_IFS_IID(int p0, float p1, struct S_IID p2) { return p0; }
EXPORT int f11_I_IFS_IIP(int p0, float p1, struct S_IIP p2) { return p0; }
EXPORT int f11_I_IFS_IFI(int p0, float p1, struct S_IFI p2) { return p0; }
EXPORT int f11_I_IFS_IFF(int p0, float p1, struct S_IFF p2) { return p0; }
EXPORT int f11_I_IFS_IFD(int p0, float p1, struct S_IFD p2) { return p0; }
EXPORT int f11_I_IFS_IFP(int p0, float p1, struct S_IFP p2) { return p0; }
EXPORT int f11_I_IFS_IDI(int p0, float p1, struct S_IDI p2) { return p0; }
EXPORT int f11_I_IFS_IDF(int p0, float p1, struct S_IDF p2) { return p0; }
EXPORT int f11_I_IFS_IDD(int p0, float p1, struct S_IDD p2) { return p0; }
EXPORT int f11_I_IFS_IDP(int p0, float p1, struct S_IDP p2) { return p0; }
EXPORT int f11_I_IFS_IPI(int p0, float p1, struct S_IPI p2) { return p0; }
EXPORT int f11_I_IFS_IPF(int p0, float p1, struct S_IPF p2) { return p0; }
EXPORT int f11_I_IFS_IPD(int p0, float p1, struct S_IPD p2) { return p0; }
EXPORT int f11_I_IFS_IPP(int p0, float p1, struct S_IPP p2) { return p0; }
EXPORT int f11_I_IFS_FII(int p0, float p1, struct S_FII p2) { return p0; }
EXPORT int f11_I_IFS_FIF(int p0, float p1, struct S_FIF p2) { return p0; }
EXPORT int f11_I_IFS_FID(int p0, float p1, struct S_FID p2) { return p0; }
EXPORT int f11_I_IFS_FIP(int p0, float p1, struct S_FIP p2) { return p0; }
EXPORT int f11_I_IFS_FFI(int p0, float p1, struct S_FFI p2) { return p0; }
EXPORT int f11_I_IFS_FFF(int p0, float p1, struct S_FFF p2) { return p0; }
EXPORT int f11_I_IFS_FFD(int p0, float p1, struct S_FFD p2) { return p0; }
EXPORT int f11_I_IFS_FFP(int p0, float p1, struct S_FFP p2) { return p0; }
EXPORT int f11_I_IFS_FDI(int p0, float p1, struct S_FDI p2) { return p0; }
EXPORT int f11_I_IFS_FDF(int p0, float p1, struct S_FDF p2) { return p0; }
EXPORT int f11_I_IFS_FDD(int p0, float p1, struct S_FDD p2) { return p0; }
EXPORT int f11_I_IFS_FDP(int p0, float p1, struct S_FDP p2) { return p0; }
EXPORT int f11_I_IFS_FPI(int p0, float p1, struct S_FPI p2) { return p0; }
EXPORT int f11_I_IFS_FPF(int p0, float p1, struct S_FPF p2) { return p0; }
EXPORT int f11_I_IFS_FPD(int p0, float p1, struct S_FPD p2) { return p0; }
EXPORT int f11_I_IFS_FPP(int p0, float p1, struct S_FPP p2) { return p0; }
EXPORT int f11_I_IFS_DII(int p0, float p1, struct S_DII p2) { return p0; }
EXPORT int f11_I_IFS_DIF(int p0, float p1, struct S_DIF p2) { return p0; }
EXPORT int f11_I_IFS_DID(int p0, float p1, struct S_DID p2) { return p0; }
EXPORT int f11_I_IFS_DIP(int p0, float p1, struct S_DIP p2) { return p0; }
EXPORT int f11_I_IFS_DFI(int p0, float p1, struct S_DFI p2) { return p0; }
EXPORT int f11_I_IFS_DFF(int p0, float p1, struct S_DFF p2) { return p0; }
EXPORT int f11_I_IFS_DFD(int p0, float p1, struct S_DFD p2) { return p0; }
EXPORT int f11_I_IFS_DFP(int p0, float p1, struct S_DFP p2) { return p0; }
EXPORT int f11_I_IFS_DDI(int p0, float p1, struct S_DDI p2) { return p0; }
EXPORT int f11_I_IFS_DDF(int p0, float p1, struct S_DDF p2) { return p0; }
EXPORT int f11_I_IFS_DDD(int p0, float p1, struct S_DDD p2) { return p0; }
EXPORT int f11_I_IFS_DDP(int p0, float p1, struct S_DDP p2) { return p0; }
EXPORT int f11_I_IFS_DPI(int p0, float p1, struct S_DPI p2) { return p0; }
EXPORT int f11_I_IFS_DPF(int p0, float p1, struct S_DPF p2) { return p0; }
EXPORT int f11_I_IFS_DPD(int p0, float p1, struct S_DPD p2) { return p0; }
EXPORT int f11_I_IFS_DPP(int p0, float p1, struct S_DPP p2) { return p0; }
EXPORT int f11_I_IFS_PII(int p0, float p1, struct S_PII p2) { return p0; }
EXPORT int f11_I_IFS_PIF(int p0, float p1, struct S_PIF p2) { return p0; }
EXPORT int f11_I_IFS_PID(int p0, float p1, struct S_PID p2) { return p0; }
EXPORT int f11_I_IFS_PIP(int p0, float p1, struct S_PIP p2) { return p0; }
EXPORT int f11_I_IFS_PFI(int p0, float p1, struct S_PFI p2) { return p0; }
EXPORT int f11_I_IFS_PFF(int p0, float p1, struct S_PFF p2) { return p0; }
EXPORT int f11_I_IFS_PFD(int p0, float p1, struct S_PFD p2) { return p0; }
EXPORT int f11_I_IFS_PFP(int p0, float p1, struct S_PFP p2) { return p0; }
EXPORT int f11_I_IFS_PDI(int p0, float p1, struct S_PDI p2) { return p0; }
EXPORT int f11_I_IFS_PDF(int p0, float p1, struct S_PDF p2) { return p0; }
EXPORT int f11_I_IFS_PDD(int p0, float p1, struct S_PDD p2) { return p0; }
EXPORT int f11_I_IFS_PDP(int p0, float p1, struct S_PDP p2) { return p0; }
EXPORT int f11_I_IFS_PPI(int p0, float p1, struct S_PPI p2) { return p0; }
EXPORT int f11_I_IFS_PPF(int p0, float p1, struct S_PPF p2) { return p0; }
EXPORT int f11_I_IFS_PPD(int p0, float p1, struct S_PPD p2) { return p0; }
EXPORT int f11_I_IFS_PPP(int p0, float p1, struct S_PPP p2) { return p0; }
EXPORT int f11_I_IDI_(int p0, double p1, int p2) { return p0; }
EXPORT int f11_I_IDF_(int p0, double p1, float p2) { return p0; }
EXPORT int f11_I_IDD_(int p0, double p1, double p2) { return p0; }
EXPORT int f11_I_IDP_(int p0, double p1, void* p2) { return p0; }
EXPORT int f11_I_IDS_I(int p0, double p1, struct S_I p2) { return p0; }
EXPORT int f11_I_IDS_F(int p0, double p1, struct S_F p2) { return p0; }
EXPORT int f11_I_IDS_D(int p0, double p1, struct S_D p2) { return p0; }
EXPORT int f11_I_IDS_P(int p0, double p1, struct S_P p2) { return p0; }
EXPORT int f11_I_IDS_II(int p0, double p1, struct S_II p2) { return p0; }
EXPORT int f11_I_IDS_IF(int p0, double p1, struct S_IF p2) { return p0; }
EXPORT int f11_I_IDS_ID(int p0, double p1, struct S_ID p2) { return p0; }
EXPORT int f11_I_IDS_IP(int p0, double p1, struct S_IP p2) { return p0; }
EXPORT int f11_I_IDS_FI(int p0, double p1, struct S_FI p2) { return p0; }
EXPORT int f11_I_IDS_FF(int p0, double p1, struct S_FF p2) { return p0; }
EXPORT int f11_I_IDS_FD(int p0, double p1, struct S_FD p2) { return p0; }
EXPORT int f11_I_IDS_FP(int p0, double p1, struct S_FP p2) { return p0; }
EXPORT int f11_I_IDS_DI(int p0, double p1, struct S_DI p2) { return p0; }
EXPORT int f11_I_IDS_DF(int p0, double p1, struct S_DF p2) { return p0; }
EXPORT int f11_I_IDS_DD(int p0, double p1, struct S_DD p2) { return p0; }
EXPORT int f11_I_IDS_DP(int p0, double p1, struct S_DP p2) { return p0; }
EXPORT int f11_I_IDS_PI(int p0, double p1, struct S_PI p2) { return p0; }
EXPORT int f11_I_IDS_PF(int p0, double p1, struct S_PF p2) { return p0; }
EXPORT int f11_I_IDS_PD(int p0, double p1, struct S_PD p2) { return p0; }
EXPORT int f11_I_IDS_PP(int p0, double p1, struct S_PP p2) { return p0; }
EXPORT int f11_I_IDS_III(int p0, double p1, struct S_III p2) { return p0; }
EXPORT int f11_I_IDS_IIF(int p0, double p1, struct S_IIF p2) { return p0; }
EXPORT int f11_I_IDS_IID(int p0, double p1, struct S_IID p2) { return p0; }
EXPORT int f11_I_IDS_IIP(int p0, double p1, struct S_IIP p2) { return p0; }
EXPORT int f11_I_IDS_IFI(int p0, double p1, struct S_IFI p2) { return p0; }
EXPORT int f11_I_IDS_IFF(int p0, double p1, struct S_IFF p2) { return p0; }
EXPORT int f11_I_IDS_IFD(int p0, double p1, struct S_IFD p2) { return p0; }
EXPORT int f11_I_IDS_IFP(int p0, double p1, struct S_IFP p2) { return p0; }
EXPORT int f11_I_IDS_IDI(int p0, double p1, struct S_IDI p2) { return p0; }
EXPORT int f11_I_IDS_IDF(int p0, double p1, struct S_IDF p2) { return p0; }
EXPORT int f11_I_IDS_IDD(int p0, double p1, struct S_IDD p2) { return p0; }
EXPORT int f11_I_IDS_IDP(int p0, double p1, struct S_IDP p2) { return p0; }
EXPORT int f11_I_IDS_IPI(int p0, double p1, struct S_IPI p2) { return p0; }
EXPORT int f11_I_IDS_IPF(int p0, double p1, struct S_IPF p2) { return p0; }
EXPORT int f11_I_IDS_IPD(int p0, double p1, struct S_IPD p2) { return p0; }
EXPORT int f11_I_IDS_IPP(int p0, double p1, struct S_IPP p2) { return p0; }
EXPORT int f11_I_IDS_FII(int p0, double p1, struct S_FII p2) { return p0; }
EXPORT int f11_I_IDS_FIF(int p0, double p1, struct S_FIF p2) { return p0; }
EXPORT int f11_I_IDS_FID(int p0, double p1, struct S_FID p2) { return p0; }
EXPORT int f11_I_IDS_FIP(int p0, double p1, struct S_FIP p2) { return p0; }
EXPORT int f11_I_IDS_FFI(int p0, double p1, struct S_FFI p2) { return p0; }
EXPORT int f11_I_IDS_FFF(int p0, double p1, struct S_FFF p2) { return p0; }
EXPORT int f11_I_IDS_FFD(int p0, double p1, struct S_FFD p2) { return p0; }
EXPORT int f11_I_IDS_FFP(int p0, double p1, struct S_FFP p2) { return p0; }
EXPORT int f11_I_IDS_FDI(int p0, double p1, struct S_FDI p2) { return p0; }
EXPORT int f11_I_IDS_FDF(int p0, double p1, struct S_FDF p2) { return p0; }
EXPORT int f11_I_IDS_FDD(int p0, double p1, struct S_FDD p2) { return p0; }
EXPORT int f11_I_IDS_FDP(int p0, double p1, struct S_FDP p2) { return p0; }
EXPORT int f11_I_IDS_FPI(int p0, double p1, struct S_FPI p2) { return p0; }
EXPORT int f11_I_IDS_FPF(int p0, double p1, struct S_FPF p2) { return p0; }
EXPORT int f11_I_IDS_FPD(int p0, double p1, struct S_FPD p2) { return p0; }
EXPORT int f11_I_IDS_FPP(int p0, double p1, struct S_FPP p2) { return p0; }
EXPORT int f11_I_IDS_DII(int p0, double p1, struct S_DII p2) { return p0; }
EXPORT int f11_I_IDS_DIF(int p0, double p1, struct S_DIF p2) { return p0; }
EXPORT int f11_I_IDS_DID(int p0, double p1, struct S_DID p2) { return p0; }
EXPORT int f11_I_IDS_DIP(int p0, double p1, struct S_DIP p2) { return p0; }
EXPORT int f11_I_IDS_DFI(int p0, double p1, struct S_DFI p2) { return p0; }
EXPORT int f11_I_IDS_DFF(int p0, double p1, struct S_DFF p2) { return p0; }
EXPORT int f11_I_IDS_DFD(int p0, double p1, struct S_DFD p2) { return p0; }
EXPORT int f11_I_IDS_DFP(int p0, double p1, struct S_DFP p2) { return p0; }
EXPORT int f11_I_IDS_DDI(int p0, double p1, struct S_DDI p2) { return p0; }
EXPORT int f11_I_IDS_DDF(int p0, double p1, struct S_DDF p2) { return p0; }
EXPORT int f11_I_IDS_DDD(int p0, double p1, struct S_DDD p2) { return p0; }
EXPORT int f11_I_IDS_DDP(int p0, double p1, struct S_DDP p2) { return p0; }
EXPORT int f11_I_IDS_DPI(int p0, double p1, struct S_DPI p2) { return p0; }
EXPORT int f11_I_IDS_DPF(int p0, double p1, struct S_DPF p2) { return p0; }
EXPORT int f11_I_IDS_DPD(int p0, double p1, struct S_DPD p2) { return p0; }
EXPORT int f11_I_IDS_DPP(int p0, double p1, struct S_DPP p2) { return p0; }
EXPORT int f11_I_IDS_PII(int p0, double p1, struct S_PII p2) { return p0; }
EXPORT int f11_I_IDS_PIF(int p0, double p1, struct S_PIF p2) { return p0; }
EXPORT int f11_I_IDS_PID(int p0, double p1, struct S_PID p2) { return p0; }
EXPORT int f11_I_IDS_PIP(int p0, double p1, struct S_PIP p2) { return p0; }
EXPORT int f11_I_IDS_PFI(int p0, double p1, struct S_PFI p2) { return p0; }
EXPORT int f11_I_IDS_PFF(int p0, double p1, struct S_PFF p2) { return p0; }
EXPORT int f11_I_IDS_PFD(int p0, double p1, struct S_PFD p2) { return p0; }
EXPORT int f11_I_IDS_PFP(int p0, double p1, struct S_PFP p2) { return p0; }
EXPORT int f11_I_IDS_PDI(int p0, double p1, struct S_PDI p2) { return p0; }
EXPORT int f11_I_IDS_PDF(int p0, double p1, struct S_PDF p2) { return p0; }
EXPORT int f11_I_IDS_PDD(int p0, double p1, struct S_PDD p2) { return p0; }
EXPORT int f11_I_IDS_PDP(int p0, double p1, struct S_PDP p2) { return p0; }
EXPORT int f11_I_IDS_PPI(int p0, double p1, struct S_PPI p2) { return p0; }
EXPORT int f11_I_IDS_PPF(int p0, double p1, struct S_PPF p2) { return p0; }
EXPORT int f11_I_IDS_PPD(int p0, double p1, struct S_PPD p2) { return p0; }
EXPORT int f11_I_IDS_PPP(int p0, double p1, struct S_PPP p2) { return p0; }
EXPORT int f11_I_IPI_(int p0, void* p1, int p2) { return p0; }
EXPORT int f11_I_IPF_(int p0, void* p1, float p2) { return p0; }
EXPORT int f11_I_IPD_(int p0, void* p1, double p2) { return p0; }
EXPORT int f11_I_IPP_(int p0, void* p1, void* p2) { return p0; }
EXPORT int f11_I_IPS_I(int p0, void* p1, struct S_I p2) { return p0; }
EXPORT int f11_I_IPS_F(int p0, void* p1, struct S_F p2) { return p0; }
EXPORT int f11_I_IPS_D(int p0, void* p1, struct S_D p2) { return p0; }
EXPORT int f11_I_IPS_P(int p0, void* p1, struct S_P p2) { return p0; }
EXPORT int f11_I_IPS_II(int p0, void* p1, struct S_II p2) { return p0; }
EXPORT int f11_I_IPS_IF(int p0, void* p1, struct S_IF p2) { return p0; }
EXPORT int f11_I_IPS_ID(int p0, void* p1, struct S_ID p2) { return p0; }
EXPORT int f11_I_IPS_IP(int p0, void* p1, struct S_IP p2) { return p0; }
EXPORT int f11_I_IPS_FI(int p0, void* p1, struct S_FI p2) { return p0; }
EXPORT int f11_I_IPS_FF(int p0, void* p1, struct S_FF p2) { return p0; }
EXPORT int f11_I_IPS_FD(int p0, void* p1, struct S_FD p2) { return p0; }
EXPORT int f11_I_IPS_FP(int p0, void* p1, struct S_FP p2) { return p0; }
EXPORT int f11_I_IPS_DI(int p0, void* p1, struct S_DI p2) { return p0; }
EXPORT int f11_I_IPS_DF(int p0, void* p1, struct S_DF p2) { return p0; }
EXPORT int f11_I_IPS_DD(int p0, void* p1, struct S_DD p2) { return p0; }
EXPORT int f11_I_IPS_DP(int p0, void* p1, struct S_DP p2) { return p0; }
EXPORT int f11_I_IPS_PI(int p0, void* p1, struct S_PI p2) { return p0; }
EXPORT int f11_I_IPS_PF(int p0, void* p1, struct S_PF p2) { return p0; }
EXPORT int f11_I_IPS_PD(int p0, void* p1, struct S_PD p2) { return p0; }
EXPORT int f11_I_IPS_PP(int p0, void* p1, struct S_PP p2) { return p0; }
EXPORT int f11_I_IPS_III(int p0, void* p1, struct S_III p2) { return p0; }
EXPORT int f11_I_IPS_IIF(int p0, void* p1, struct S_IIF p2) { return p0; }
EXPORT int f11_I_IPS_IID(int p0, void* p1, struct S_IID p2) { return p0; }
EXPORT int f12_I_IPS_IIP(int p0, void* p1, struct S_IIP p2) { return p0; }
EXPORT int f12_I_IPS_IFI(int p0, void* p1, struct S_IFI p2) { return p0; }
EXPORT int f12_I_IPS_IFF(int p0, void* p1, struct S_IFF p2) { return p0; }
EXPORT int f12_I_IPS_IFD(int p0, void* p1, struct S_IFD p2) { return p0; }
EXPORT int f12_I_IPS_IFP(int p0, void* p1, struct S_IFP p2) { return p0; }
EXPORT int f12_I_IPS_IDI(int p0, void* p1, struct S_IDI p2) { return p0; }
EXPORT int f12_I_IPS_IDF(int p0, void* p1, struct S_IDF p2) { return p0; }
EXPORT int f12_I_IPS_IDD(int p0, void* p1, struct S_IDD p2) { return p0; }
EXPORT int f12_I_IPS_IDP(int p0, void* p1, struct S_IDP p2) { return p0; }
EXPORT int f12_I_IPS_IPI(int p0, void* p1, struct S_IPI p2) { return p0; }
EXPORT int f12_I_IPS_IPF(int p0, void* p1, struct S_IPF p2) { return p0; }
EXPORT int f12_I_IPS_IPD(int p0, void* p1, struct S_IPD p2) { return p0; }
EXPORT int f12_I_IPS_IPP(int p0, void* p1, struct S_IPP p2) { return p0; }
EXPORT int f12_I_IPS_FII(int p0, void* p1, struct S_FII p2) { return p0; }
EXPORT int f12_I_IPS_FIF(int p0, void* p1, struct S_FIF p2) { return p0; }
EXPORT int f12_I_IPS_FID(int p0, void* p1, struct S_FID p2) { return p0; }
EXPORT int f12_I_IPS_FIP(int p0, void* p1, struct S_FIP p2) { return p0; }
EXPORT int f12_I_IPS_FFI(int p0, void* p1, struct S_FFI p2) { return p0; }
EXPORT int f12_I_IPS_FFF(int p0, void* p1, struct S_FFF p2) { return p0; }
EXPORT int f12_I_IPS_FFD(int p0, void* p1, struct S_FFD p2) { return p0; }
EXPORT int f12_I_IPS_FFP(int p0, void* p1, struct S_FFP p2) { return p0; }
EXPORT int f12_I_IPS_FDI(int p0, void* p1, struct S_FDI p2) { return p0; }
EXPORT int f12_I_IPS_FDF(int p0, void* p1, struct S_FDF p2) { return p0; }
EXPORT int f12_I_IPS_FDD(int p0, void* p1, struct S_FDD p2) { return p0; }
EXPORT int f12_I_IPS_FDP(int p0, void* p1, struct S_FDP p2) { return p0; }
EXPORT int f12_I_IPS_FPI(int p0, void* p1, struct S_FPI p2) { return p0; }
EXPORT int f12_I_IPS_FPF(int p0, void* p1, struct S_FPF p2) { return p0; }
EXPORT int f12_I_IPS_FPD(int p0, void* p1, struct S_FPD p2) { return p0; }
EXPORT int f12_I_IPS_FPP(int p0, void* p1, struct S_FPP p2) { return p0; }
EXPORT int f12_I_IPS_DII(int p0, void* p1, struct S_DII p2) { return p0; }
EXPORT int f12_I_IPS_DIF(int p0, void* p1, struct S_DIF p2) { return p0; }
EXPORT int f12_I_IPS_DID(int p0, void* p1, struct S_DID p2) { return p0; }
EXPORT int f12_I_IPS_DIP(int p0, void* p1, struct S_DIP p2) { return p0; }
EXPORT int f12_I_IPS_DFI(int p0, void* p1, struct S_DFI p2) { return p0; }
EXPORT int f12_I_IPS_DFF(int p0, void* p1, struct S_DFF p2) { return p0; }
EXPORT int f12_I_IPS_DFD(int p0, void* p1, struct S_DFD p2) { return p0; }
EXPORT int f12_I_IPS_DFP(int p0, void* p1, struct S_DFP p2) { return p0; }
EXPORT int f12_I_IPS_DDI(int p0, void* p1, struct S_DDI p2) { return p0; }
EXPORT int f12_I_IPS_DDF(int p0, void* p1, struct S_DDF p2) { return p0; }
EXPORT int f12_I_IPS_DDD(int p0, void* p1, struct S_DDD p2) { return p0; }
EXPORT int f12_I_IPS_DDP(int p0, void* p1, struct S_DDP p2) { return p0; }
EXPORT int f12_I_IPS_DPI(int p0, void* p1, struct S_DPI p2) { return p0; }
EXPORT int f12_I_IPS_DPF(int p0, void* p1, struct S_DPF p2) { return p0; }
EXPORT int f12_I_IPS_DPD(int p0, void* p1, struct S_DPD p2) { return p0; }
EXPORT int f12_I_IPS_DPP(int p0, void* p1, struct S_DPP p2) { return p0; }
EXPORT int f12_I_IPS_PII(int p0, void* p1, struct S_PII p2) { return p0; }
EXPORT int f12_I_IPS_PIF(int p0, void* p1, struct S_PIF p2) { return p0; }
EXPORT int f12_I_IPS_PID(int p0, void* p1, struct S_PID p2) { return p0; }
EXPORT int f12_I_IPS_PIP(int p0, void* p1, struct S_PIP p2) { return p0; }
EXPORT int f12_I_IPS_PFI(int p0, void* p1, struct S_PFI p2) { return p0; }
EXPORT int f12_I_IPS_PFF(int p0, void* p1, struct S_PFF p2) { return p0; }
EXPORT int f12_I_IPS_PFD(int p0, void* p1, struct S_PFD p2) { return p0; }
EXPORT int f12_I_IPS_PFP(int p0, void* p1, struct S_PFP p2) { return p0; }
EXPORT int f12_I_IPS_PDI(int p0, void* p1, struct S_PDI p2) { return p0; }
EXPORT int f12_I_IPS_PDF(int p0, void* p1, struct S_PDF p2) { return p0; }
EXPORT int f12_I_IPS_PDD(int p0, void* p1, struct S_PDD p2) { return p0; }
EXPORT int f12_I_IPS_PDP(int p0, void* p1, struct S_PDP p2) { return p0; }
EXPORT int f12_I_IPS_PPI(int p0, void* p1, struct S_PPI p2) { return p0; }
EXPORT int f12_I_IPS_PPF(int p0, void* p1, struct S_PPF p2) { return p0; }
EXPORT int f12_I_IPS_PPD(int p0, void* p1, struct S_PPD p2) { return p0; }
EXPORT int f12_I_IPS_PPP(int p0, void* p1, struct S_PPP p2) { return p0; }
EXPORT int f12_I_ISI_I(int p0, struct S_I p1, int p2) { return p0; }
EXPORT int f12_I_ISI_F(int p0, struct S_F p1, int p2) { return p0; }
EXPORT int f12_I_ISI_D(int p0, struct S_D p1, int p2) { return p0; }
EXPORT int f12_I_ISI_P(int p0, struct S_P p1, int p2) { return p0; }
EXPORT int f12_I_ISI_II(int p0, struct S_II p1, int p2) { return p0; }
EXPORT int f12_I_ISI_IF(int p0, struct S_IF p1, int p2) { return p0; }
EXPORT int f12_I_ISI_ID(int p0, struct S_ID p1, int p2) { return p0; }
EXPORT int f12_I_ISI_IP(int p0, struct S_IP p1, int p2) { return p0; }
EXPORT int f12_I_ISI_FI(int p0, struct S_FI p1, int p2) { return p0; }
EXPORT int f12_I_ISI_FF(int p0, struct S_FF p1, int p2) { return p0; }
EXPORT int f12_I_ISI_FD(int p0, struct S_FD p1, int p2) { return p0; }
EXPORT int f12_I_ISI_FP(int p0, struct S_FP p1, int p2) { return p0; }
EXPORT int f12_I_ISI_DI(int p0, struct S_DI p1, int p2) { return p0; }
EXPORT int f12_I_ISI_DF(int p0, struct S_DF p1, int p2) { return p0; }
EXPORT int f12_I_ISI_DD(int p0, struct S_DD p1, int p2) { return p0; }
EXPORT int f12_I_ISI_DP(int p0, struct S_DP p1, int p2) { return p0; }
EXPORT int f12_I_ISI_PI(int p0, struct S_PI p1, int p2) { return p0; }
EXPORT int f12_I_ISI_PF(int p0, struct S_PF p1, int p2) { return p0; }
EXPORT int f12_I_ISI_PD(int p0, struct S_PD p1, int p2) { return p0; }
EXPORT int f12_I_ISI_PP(int p0, struct S_PP p1, int p2) { return p0; }
EXPORT int f12_I_ISI_III(int p0, struct S_III p1, int p2) { return p0; }
EXPORT int f12_I_ISI_IIF(int p0, struct S_IIF p1, int p2) { return p0; }
EXPORT int f12_I_ISI_IID(int p0, struct S_IID p1, int p2) { return p0; }
EXPORT int f12_I_ISI_IIP(int p0, struct S_IIP p1, int p2) { return p0; }
EXPORT int f12_I_ISI_IFI(int p0, struct S_IFI p1, int p2) { return p0; }
EXPORT int f12_I_ISI_IFF(int p0, struct S_IFF p1, int p2) { return p0; }
EXPORT int f12_I_ISI_IFD(int p0, struct S_IFD p1, int p2) { return p0; }
EXPORT int f12_I_ISI_IFP(int p0, struct S_IFP p1, int p2) { return p0; }
EXPORT int f12_I_ISI_IDI(int p0, struct S_IDI p1, int p2) { return p0; }
EXPORT int f12_I_ISI_IDF(int p0, struct S_IDF p1, int p2) { return p0; }
EXPORT int f12_I_ISI_IDD(int p0, struct S_IDD p1, int p2) { return p0; }
EXPORT int f12_I_ISI_IDP(int p0, struct S_IDP p1, int p2) { return p0; }
EXPORT int f12_I_ISI_IPI(int p0, struct S_IPI p1, int p2) { return p0; }
EXPORT int f12_I_ISI_IPF(int p0, struct S_IPF p1, int p2) { return p0; }
EXPORT int f12_I_ISI_IPD(int p0, struct S_IPD p1, int p2) { return p0; }
EXPORT int f12_I_ISI_IPP(int p0, struct S_IPP p1, int p2) { return p0; }
EXPORT int f12_I_ISI_FII(int p0, struct S_FII p1, int p2) { return p0; }
EXPORT int f12_I_ISI_FIF(int p0, struct S_FIF p1, int p2) { return p0; }
EXPORT int f12_I_ISI_FID(int p0, struct S_FID p1, int p2) { return p0; }
EXPORT int f12_I_ISI_FIP(int p0, struct S_FIP p1, int p2) { return p0; }
EXPORT int f12_I_ISI_FFI(int p0, struct S_FFI p1, int p2) { return p0; }
EXPORT int f12_I_ISI_FFF(int p0, struct S_FFF p1, int p2) { return p0; }
EXPORT int f12_I_ISI_FFD(int p0, struct S_FFD p1, int p2) { return p0; }
EXPORT int f12_I_ISI_FFP(int p0, struct S_FFP p1, int p2) { return p0; }
EXPORT int f12_I_ISI_FDI(int p0, struct S_FDI p1, int p2) { return p0; }
EXPORT int f12_I_ISI_FDF(int p0, struct S_FDF p1, int p2) { return p0; }
EXPORT int f12_I_ISI_FDD(int p0, struct S_FDD p1, int p2) { return p0; }
EXPORT int f12_I_ISI_FDP(int p0, struct S_FDP p1, int p2) { return p0; }
EXPORT int f12_I_ISI_FPI(int p0, struct S_FPI p1, int p2) { return p0; }
EXPORT int f12_I_ISI_FPF(int p0, struct S_FPF p1, int p2) { return p0; }
EXPORT int f12_I_ISI_FPD(int p0, struct S_FPD p1, int p2) { return p0; }
EXPORT int f12_I_ISI_FPP(int p0, struct S_FPP p1, int p2) { return p0; }
EXPORT int f12_I_ISI_DII(int p0, struct S_DII p1, int p2) { return p0; }
EXPORT int f12_I_ISI_DIF(int p0, struct S_DIF p1, int p2) { return p0; }
EXPORT int f12_I_ISI_DID(int p0, struct S_DID p1, int p2) { return p0; }
EXPORT int f12_I_ISI_DIP(int p0, struct S_DIP p1, int p2) { return p0; }
EXPORT int f12_I_ISI_DFI(int p0, struct S_DFI p1, int p2) { return p0; }
EXPORT int f12_I_ISI_DFF(int p0, struct S_DFF p1, int p2) { return p0; }
EXPORT int f12_I_ISI_DFD(int p0, struct S_DFD p1, int p2) { return p0; }
EXPORT int f12_I_ISI_DFP(int p0, struct S_DFP p1, int p2) { return p0; }
EXPORT int f12_I_ISI_DDI(int p0, struct S_DDI p1, int p2) { return p0; }
EXPORT int f12_I_ISI_DDF(int p0, struct S_DDF p1, int p2) { return p0; }
EXPORT int f12_I_ISI_DDD(int p0, struct S_DDD p1, int p2) { return p0; }
EXPORT int f12_I_ISI_DDP(int p0, struct S_DDP p1, int p2) { return p0; }
EXPORT int f12_I_ISI_DPI(int p0, struct S_DPI p1, int p2) { return p0; }
EXPORT int f12_I_ISI_DPF(int p0, struct S_DPF p1, int p2) { return p0; }
EXPORT int f12_I_ISI_DPD(int p0, struct S_DPD p1, int p2) { return p0; }
EXPORT int f12_I_ISI_DPP(int p0, struct S_DPP p1, int p2) { return p0; }
EXPORT int f12_I_ISI_PII(int p0, struct S_PII p1, int p2) { return p0; }
EXPORT int f12_I_ISI_PIF(int p0, struct S_PIF p1, int p2) { return p0; }
EXPORT int f12_I_ISI_PID(int p0, struct S_PID p1, int p2) { return p0; }
EXPORT int f12_I_ISI_PIP(int p0, struct S_PIP p1, int p2) { return p0; }
EXPORT int f12_I_ISI_PFI(int p0, struct S_PFI p1, int p2) { return p0; }
EXPORT int f12_I_ISI_PFF(int p0, struct S_PFF p1, int p2) { return p0; }
EXPORT int f12_I_ISI_PFD(int p0, struct S_PFD p1, int p2) { return p0; }
EXPORT int f12_I_ISI_PFP(int p0, struct S_PFP p1, int p2) { return p0; }
EXPORT int f12_I_ISI_PDI(int p0, struct S_PDI p1, int p2) { return p0; }
EXPORT int f12_I_ISI_PDF(int p0, struct S_PDF p1, int p2) { return p0; }
EXPORT int f12_I_ISI_PDD(int p0, struct S_PDD p1, int p2) { return p0; }
EXPORT int f12_I_ISI_PDP(int p0, struct S_PDP p1, int p2) { return p0; }
EXPORT int f12_I_ISI_PPI(int p0, struct S_PPI p1, int p2) { return p0; }
EXPORT int f12_I_ISI_PPF(int p0, struct S_PPF p1, int p2) { return p0; }
EXPORT int f12_I_ISI_PPD(int p0, struct S_PPD p1, int p2) { return p0; }
EXPORT int f12_I_ISI_PPP(int p0, struct S_PPP p1, int p2) { return p0; }
EXPORT int f12_I_ISF_I(int p0, struct S_I p1, float p2) { return p0; }
EXPORT int f12_I_ISF_F(int p0, struct S_F p1, float p2) { return p0; }
EXPORT int f12_I_ISF_D(int p0, struct S_D p1, float p2) { return p0; }
EXPORT int f12_I_ISF_P(int p0, struct S_P p1, float p2) { return p0; }
EXPORT int f12_I_ISF_II(int p0, struct S_II p1, float p2) { return p0; }
EXPORT int f12_I_ISF_IF(int p0, struct S_IF p1, float p2) { return p0; }
EXPORT int f12_I_ISF_ID(int p0, struct S_ID p1, float p2) { return p0; }
EXPORT int f12_I_ISF_IP(int p0, struct S_IP p1, float p2) { return p0; }
EXPORT int f12_I_ISF_FI(int p0, struct S_FI p1, float p2) { return p0; }
EXPORT int f12_I_ISF_FF(int p0, struct S_FF p1, float p2) { return p0; }
EXPORT int f12_I_ISF_FD(int p0, struct S_FD p1, float p2) { return p0; }
EXPORT int f12_I_ISF_FP(int p0, struct S_FP p1, float p2) { return p0; }
EXPORT int f12_I_ISF_DI(int p0, struct S_DI p1, float p2) { return p0; }
EXPORT int f12_I_ISF_DF(int p0, struct S_DF p1, float p2) { return p0; }
EXPORT int f12_I_ISF_DD(int p0, struct S_DD p1, float p2) { return p0; }
EXPORT int f12_I_ISF_DP(int p0, struct S_DP p1, float p2) { return p0; }
EXPORT int f12_I_ISF_PI(int p0, struct S_PI p1, float p2) { return p0; }
EXPORT int f12_I_ISF_PF(int p0, struct S_PF p1, float p2) { return p0; }
EXPORT int f12_I_ISF_PD(int p0, struct S_PD p1, float p2) { return p0; }
EXPORT int f12_I_ISF_PP(int p0, struct S_PP p1, float p2) { return p0; }
EXPORT int f12_I_ISF_III(int p0, struct S_III p1, float p2) { return p0; }
EXPORT int f12_I_ISF_IIF(int p0, struct S_IIF p1, float p2) { return p0; }
EXPORT int f12_I_ISF_IID(int p0, struct S_IID p1, float p2) { return p0; }
EXPORT int f12_I_ISF_IIP(int p0, struct S_IIP p1, float p2) { return p0; }
EXPORT int f12_I_ISF_IFI(int p0, struct S_IFI p1, float p2) { return p0; }
EXPORT int f12_I_ISF_IFF(int p0, struct S_IFF p1, float p2) { return p0; }
EXPORT int f12_I_ISF_IFD(int p0, struct S_IFD p1, float p2) { return p0; }
EXPORT int f12_I_ISF_IFP(int p0, struct S_IFP p1, float p2) { return p0; }
EXPORT int f12_I_ISF_IDI(int p0, struct S_IDI p1, float p2) { return p0; }
EXPORT int f12_I_ISF_IDF(int p0, struct S_IDF p1, float p2) { return p0; }
EXPORT int f12_I_ISF_IDD(int p0, struct S_IDD p1, float p2) { return p0; }
EXPORT int f12_I_ISF_IDP(int p0, struct S_IDP p1, float p2) { return p0; }
EXPORT int f12_I_ISF_IPI(int p0, struct S_IPI p1, float p2) { return p0; }
EXPORT int f12_I_ISF_IPF(int p0, struct S_IPF p1, float p2) { return p0; }
EXPORT int f12_I_ISF_IPD(int p0, struct S_IPD p1, float p2) { return p0; }
EXPORT int f12_I_ISF_IPP(int p0, struct S_IPP p1, float p2) { return p0; }
EXPORT int f12_I_ISF_FII(int p0, struct S_FII p1, float p2) { return p0; }
EXPORT int f12_I_ISF_FIF(int p0, struct S_FIF p1, float p2) { return p0; }
EXPORT int f12_I_ISF_FID(int p0, struct S_FID p1, float p2) { return p0; }
EXPORT int f12_I_ISF_FIP(int p0, struct S_FIP p1, float p2) { return p0; }
EXPORT int f12_I_ISF_FFI(int p0, struct S_FFI p1, float p2) { return p0; }
EXPORT int f12_I_ISF_FFF(int p0, struct S_FFF p1, float p2) { return p0; }
EXPORT int f12_I_ISF_FFD(int p0, struct S_FFD p1, float p2) { return p0; }
EXPORT int f12_I_ISF_FFP(int p0, struct S_FFP p1, float p2) { return p0; }
EXPORT int f12_I_ISF_FDI(int p0, struct S_FDI p1, float p2) { return p0; }
EXPORT int f12_I_ISF_FDF(int p0, struct S_FDF p1, float p2) { return p0; }
EXPORT int f12_I_ISF_FDD(int p0, struct S_FDD p1, float p2) { return p0; }
EXPORT int f12_I_ISF_FDP(int p0, struct S_FDP p1, float p2) { return p0; }
EXPORT int f12_I_ISF_FPI(int p0, struct S_FPI p1, float p2) { return p0; }
EXPORT int f12_I_ISF_FPF(int p0, struct S_FPF p1, float p2) { return p0; }
EXPORT int f12_I_ISF_FPD(int p0, struct S_FPD p1, float p2) { return p0; }
EXPORT int f12_I_ISF_FPP(int p0, struct S_FPP p1, float p2) { return p0; }
EXPORT int f12_I_ISF_DII(int p0, struct S_DII p1, float p2) { return p0; }
EXPORT int f12_I_ISF_DIF(int p0, struct S_DIF p1, float p2) { return p0; }
EXPORT int f12_I_ISF_DID(int p0, struct S_DID p1, float p2) { return p0; }
EXPORT int f12_I_ISF_DIP(int p0, struct S_DIP p1, float p2) { return p0; }
EXPORT int f12_I_ISF_DFI(int p0, struct S_DFI p1, float p2) { return p0; }
EXPORT int f12_I_ISF_DFF(int p0, struct S_DFF p1, float p2) { return p0; }
EXPORT int f12_I_ISF_DFD(int p0, struct S_DFD p1, float p2) { return p0; }
EXPORT int f12_I_ISF_DFP(int p0, struct S_DFP p1, float p2) { return p0; }
EXPORT int f12_I_ISF_DDI(int p0, struct S_DDI p1, float p2) { return p0; }
EXPORT int f12_I_ISF_DDF(int p0, struct S_DDF p1, float p2) { return p0; }
EXPORT int f12_I_ISF_DDD(int p0, struct S_DDD p1, float p2) { return p0; }
EXPORT int f12_I_ISF_DDP(int p0, struct S_DDP p1, float p2) { return p0; }
EXPORT int f12_I_ISF_DPI(int p0, struct S_DPI p1, float p2) { return p0; }
EXPORT int f12_I_ISF_DPF(int p0, struct S_DPF p1, float p2) { return p0; }
EXPORT int f12_I_ISF_DPD(int p0, struct S_DPD p1, float p2) { return p0; }
EXPORT int f12_I_ISF_DPP(int p0, struct S_DPP p1, float p2) { return p0; }
EXPORT int f12_I_ISF_PII(int p0, struct S_PII p1, float p2) { return p0; }
EXPORT int f12_I_ISF_PIF(int p0, struct S_PIF p1, float p2) { return p0; }
EXPORT int f12_I_ISF_PID(int p0, struct S_PID p1, float p2) { return p0; }
EXPORT int f12_I_ISF_PIP(int p0, struct S_PIP p1, float p2) { return p0; }
EXPORT int f12_I_ISF_PFI(int p0, struct S_PFI p1, float p2) { return p0; }
EXPORT int f12_I_ISF_PFF(int p0, struct S_PFF p1, float p2) { return p0; }
EXPORT int f12_I_ISF_PFD(int p0, struct S_PFD p1, float p2) { return p0; }
EXPORT int f12_I_ISF_PFP(int p0, struct S_PFP p1, float p2) { return p0; }
EXPORT int f12_I_ISF_PDI(int p0, struct S_PDI p1, float p2) { return p0; }
EXPORT int f12_I_ISF_PDF(int p0, struct S_PDF p1, float p2) { return p0; }
EXPORT int f12_I_ISF_PDD(int p0, struct S_PDD p1, float p2) { return p0; }
EXPORT int f12_I_ISF_PDP(int p0, struct S_PDP p1, float p2) { return p0; }
EXPORT int f12_I_ISF_PPI(int p0, struct S_PPI p1, float p2) { return p0; }
EXPORT int f12_I_ISF_PPF(int p0, struct S_PPF p1, float p2) { return p0; }
EXPORT int f12_I_ISF_PPD(int p0, struct S_PPD p1, float p2) { return p0; }
EXPORT int f12_I_ISF_PPP(int p0, struct S_PPP p1, float p2) { return p0; }
EXPORT int f12_I_ISD_I(int p0, struct S_I p1, double p2) { return p0; }
EXPORT int f12_I_ISD_F(int p0, struct S_F p1, double p2) { return p0; }
EXPORT int f12_I_ISD_D(int p0, struct S_D p1, double p2) { return p0; }
EXPORT int f12_I_ISD_P(int p0, struct S_P p1, double p2) { return p0; }
EXPORT int f12_I_ISD_II(int p0, struct S_II p1, double p2) { return p0; }
EXPORT int f12_I_ISD_IF(int p0, struct S_IF p1, double p2) { return p0; }
EXPORT int f12_I_ISD_ID(int p0, struct S_ID p1, double p2) { return p0; }
EXPORT int f12_I_ISD_IP(int p0, struct S_IP p1, double p2) { return p0; }
EXPORT int f12_I_ISD_FI(int p0, struct S_FI p1, double p2) { return p0; }
EXPORT int f12_I_ISD_FF(int p0, struct S_FF p1, double p2) { return p0; }
EXPORT int f12_I_ISD_FD(int p0, struct S_FD p1, double p2) { return p0; }
EXPORT int f12_I_ISD_FP(int p0, struct S_FP p1, double p2) { return p0; }
EXPORT int f12_I_ISD_DI(int p0, struct S_DI p1, double p2) { return p0; }
EXPORT int f12_I_ISD_DF(int p0, struct S_DF p1, double p2) { return p0; }
EXPORT int f12_I_ISD_DD(int p0, struct S_DD p1, double p2) { return p0; }
EXPORT int f12_I_ISD_DP(int p0, struct S_DP p1, double p2) { return p0; }
EXPORT int f12_I_ISD_PI(int p0, struct S_PI p1, double p2) { return p0; }
EXPORT int f12_I_ISD_PF(int p0, struct S_PF p1, double p2) { return p0; }
EXPORT int f12_I_ISD_PD(int p0, struct S_PD p1, double p2) { return p0; }
EXPORT int f12_I_ISD_PP(int p0, struct S_PP p1, double p2) { return p0; }
EXPORT int f12_I_ISD_III(int p0, struct S_III p1, double p2) { return p0; }
EXPORT int f12_I_ISD_IIF(int p0, struct S_IIF p1, double p2) { return p0; }
EXPORT int f12_I_ISD_IID(int p0, struct S_IID p1, double p2) { return p0; }
EXPORT int f12_I_ISD_IIP(int p0, struct S_IIP p1, double p2) { return p0; }
EXPORT int f12_I_ISD_IFI(int p0, struct S_IFI p1, double p2) { return p0; }
EXPORT int f12_I_ISD_IFF(int p0, struct S_IFF p1, double p2) { return p0; }
EXPORT int f12_I_ISD_IFD(int p0, struct S_IFD p1, double p2) { return p0; }
EXPORT int f12_I_ISD_IFP(int p0, struct S_IFP p1, double p2) { return p0; }
EXPORT int f12_I_ISD_IDI(int p0, struct S_IDI p1, double p2) { return p0; }
EXPORT int f12_I_ISD_IDF(int p0, struct S_IDF p1, double p2) { return p0; }
EXPORT int f12_I_ISD_IDD(int p0, struct S_IDD p1, double p2) { return p0; }
EXPORT int f12_I_ISD_IDP(int p0, struct S_IDP p1, double p2) { return p0; }
EXPORT int f12_I_ISD_IPI(int p0, struct S_IPI p1, double p2) { return p0; }
EXPORT int f12_I_ISD_IPF(int p0, struct S_IPF p1, double p2) { return p0; }
EXPORT int f12_I_ISD_IPD(int p0, struct S_IPD p1, double p2) { return p0; }
EXPORT int f12_I_ISD_IPP(int p0, struct S_IPP p1, double p2) { return p0; }
EXPORT int f12_I_ISD_FII(int p0, struct S_FII p1, double p2) { return p0; }
EXPORT int f12_I_ISD_FIF(int p0, struct S_FIF p1, double p2) { return p0; }
EXPORT int f12_I_ISD_FID(int p0, struct S_FID p1, double p2) { return p0; }
EXPORT int f12_I_ISD_FIP(int p0, struct S_FIP p1, double p2) { return p0; }
EXPORT int f12_I_ISD_FFI(int p0, struct S_FFI p1, double p2) { return p0; }
EXPORT int f12_I_ISD_FFF(int p0, struct S_FFF p1, double p2) { return p0; }
EXPORT int f12_I_ISD_FFD(int p0, struct S_FFD p1, double p2) { return p0; }
EXPORT int f12_I_ISD_FFP(int p0, struct S_FFP p1, double p2) { return p0; }
EXPORT int f12_I_ISD_FDI(int p0, struct S_FDI p1, double p2) { return p0; }
EXPORT int f12_I_ISD_FDF(int p0, struct S_FDF p1, double p2) { return p0; }
EXPORT int f12_I_ISD_FDD(int p0, struct S_FDD p1, double p2) { return p0; }
EXPORT int f12_I_ISD_FDP(int p0, struct S_FDP p1, double p2) { return p0; }
EXPORT int f12_I_ISD_FPI(int p0, struct S_FPI p1, double p2) { return p0; }
EXPORT int f12_I_ISD_FPF(int p0, struct S_FPF p1, double p2) { return p0; }
EXPORT int f12_I_ISD_FPD(int p0, struct S_FPD p1, double p2) { return p0; }
EXPORT int f12_I_ISD_FPP(int p0, struct S_FPP p1, double p2) { return p0; }
EXPORT int f12_I_ISD_DII(int p0, struct S_DII p1, double p2) { return p0; }
EXPORT int f12_I_ISD_DIF(int p0, struct S_DIF p1, double p2) { return p0; }
EXPORT int f12_I_ISD_DID(int p0, struct S_DID p1, double p2) { return p0; }
EXPORT int f12_I_ISD_DIP(int p0, struct S_DIP p1, double p2) { return p0; }
EXPORT int f12_I_ISD_DFI(int p0, struct S_DFI p1, double p2) { return p0; }
EXPORT int f12_I_ISD_DFF(int p0, struct S_DFF p1, double p2) { return p0; }
EXPORT int f12_I_ISD_DFD(int p0, struct S_DFD p1, double p2) { return p0; }
EXPORT int f12_I_ISD_DFP(int p0, struct S_DFP p1, double p2) { return p0; }
EXPORT int f12_I_ISD_DDI(int p0, struct S_DDI p1, double p2) { return p0; }
EXPORT int f12_I_ISD_DDF(int p0, struct S_DDF p1, double p2) { return p0; }
EXPORT int f12_I_ISD_DDD(int p0, struct S_DDD p1, double p2) { return p0; }
EXPORT int f12_I_ISD_DDP(int p0, struct S_DDP p1, double p2) { return p0; }
EXPORT int f12_I_ISD_DPI(int p0, struct S_DPI p1, double p2) { return p0; }
EXPORT int f12_I_ISD_DPF(int p0, struct S_DPF p1, double p2) { return p0; }
EXPORT int f12_I_ISD_DPD(int p0, struct S_DPD p1, double p2) { return p0; }
EXPORT int f12_I_ISD_DPP(int p0, struct S_DPP p1, double p2) { return p0; }
EXPORT int f12_I_ISD_PII(int p0, struct S_PII p1, double p2) { return p0; }
EXPORT int f12_I_ISD_PIF(int p0, struct S_PIF p1, double p2) { return p0; }
EXPORT int f12_I_ISD_PID(int p0, struct S_PID p1, double p2) { return p0; }
EXPORT int f12_I_ISD_PIP(int p0, struct S_PIP p1, double p2) { return p0; }
EXPORT int f12_I_ISD_PFI(int p0, struct S_PFI p1, double p2) { return p0; }
EXPORT int f12_I_ISD_PFF(int p0, struct S_PFF p1, double p2) { return p0; }
EXPORT int f12_I_ISD_PFD(int p0, struct S_PFD p1, double p2) { return p0; }
EXPORT int f12_I_ISD_PFP(int p0, struct S_PFP p1, double p2) { return p0; }
EXPORT int f12_I_ISD_PDI(int p0, struct S_PDI p1, double p2) { return p0; }
EXPORT int f12_I_ISD_PDF(int p0, struct S_PDF p1, double p2) { return p0; }
EXPORT int f12_I_ISD_PDD(int p0, struct S_PDD p1, double p2) { return p0; }
EXPORT int f12_I_ISD_PDP(int p0, struct S_PDP p1, double p2) { return p0; }
EXPORT int f12_I_ISD_PPI(int p0, struct S_PPI p1, double p2) { return p0; }
EXPORT int f12_I_ISD_PPF(int p0, struct S_PPF p1, double p2) { return p0; }
EXPORT int f12_I_ISD_PPD(int p0, struct S_PPD p1, double p2) { return p0; }
EXPORT int f12_I_ISD_PPP(int p0, struct S_PPP p1, double p2) { return p0; }
EXPORT int f12_I_ISP_I(int p0, struct S_I p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_F(int p0, struct S_F p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_D(int p0, struct S_D p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_P(int p0, struct S_P p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_II(int p0, struct S_II p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_IF(int p0, struct S_IF p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_ID(int p0, struct S_ID p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_IP(int p0, struct S_IP p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_FI(int p0, struct S_FI p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_FF(int p0, struct S_FF p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_FD(int p0, struct S_FD p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_FP(int p0, struct S_FP p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_DI(int p0, struct S_DI p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_DF(int p0, struct S_DF p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_DD(int p0, struct S_DD p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_DP(int p0, struct S_DP p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_PI(int p0, struct S_PI p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_PF(int p0, struct S_PF p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_PD(int p0, struct S_PD p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_PP(int p0, struct S_PP p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_III(int p0, struct S_III p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_IIF(int p0, struct S_IIF p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_IID(int p0, struct S_IID p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_IIP(int p0, struct S_IIP p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_IFI(int p0, struct S_IFI p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_IFF(int p0, struct S_IFF p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_IFD(int p0, struct S_IFD p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_IFP(int p0, struct S_IFP p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_IDI(int p0, struct S_IDI p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_IDF(int p0, struct S_IDF p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_IDD(int p0, struct S_IDD p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_IDP(int p0, struct S_IDP p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_IPI(int p0, struct S_IPI p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_IPF(int p0, struct S_IPF p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_IPD(int p0, struct S_IPD p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_IPP(int p0, struct S_IPP p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_FII(int p0, struct S_FII p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_FIF(int p0, struct S_FIF p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_FID(int p0, struct S_FID p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_FIP(int p0, struct S_FIP p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_FFI(int p0, struct S_FFI p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_FFF(int p0, struct S_FFF p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_FFD(int p0, struct S_FFD p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_FFP(int p0, struct S_FFP p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_FDI(int p0, struct S_FDI p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_FDF(int p0, struct S_FDF p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_FDD(int p0, struct S_FDD p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_FDP(int p0, struct S_FDP p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_FPI(int p0, struct S_FPI p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_FPF(int p0, struct S_FPF p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_FPD(int p0, struct S_FPD p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_FPP(int p0, struct S_FPP p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_DII(int p0, struct S_DII p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_DIF(int p0, struct S_DIF p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_DID(int p0, struct S_DID p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_DIP(int p0, struct S_DIP p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_DFI(int p0, struct S_DFI p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_DFF(int p0, struct S_DFF p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_DFD(int p0, struct S_DFD p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_DFP(int p0, struct S_DFP p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_DDI(int p0, struct S_DDI p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_DDF(int p0, struct S_DDF p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_DDD(int p0, struct S_DDD p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_DDP(int p0, struct S_DDP p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_DPI(int p0, struct S_DPI p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_DPF(int p0, struct S_DPF p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_DPD(int p0, struct S_DPD p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_DPP(int p0, struct S_DPP p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_PII(int p0, struct S_PII p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_PIF(int p0, struct S_PIF p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_PID(int p0, struct S_PID p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_PIP(int p0, struct S_PIP p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_PFI(int p0, struct S_PFI p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_PFF(int p0, struct S_PFF p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_PFD(int p0, struct S_PFD p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_PFP(int p0, struct S_PFP p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_PDI(int p0, struct S_PDI p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_PDF(int p0, struct S_PDF p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_PDD(int p0, struct S_PDD p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_PDP(int p0, struct S_PDP p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_PPI(int p0, struct S_PPI p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_PPF(int p0, struct S_PPF p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_PPD(int p0, struct S_PPD p1, void* p2) { return p0; }
EXPORT int f12_I_ISP_PPP(int p0, struct S_PPP p1, void* p2) { return p0; }
EXPORT int f12_I_ISS_I(int p0, struct S_I p1, struct S_I p2) { return p0; }
EXPORT int f12_I_ISS_F(int p0, struct S_F p1, struct S_F p2) { return p0; }
EXPORT int f12_I_ISS_D(int p0, struct S_D p1, struct S_D p2) { return p0; }
EXPORT int f12_I_ISS_P(int p0, struct S_P p1, struct S_P p2) { return p0; }
EXPORT int f12_I_ISS_II(int p0, struct S_II p1, struct S_II p2) { return p0; }
EXPORT int f12_I_ISS_IF(int p0, struct S_IF p1, struct S_IF p2) { return p0; }
EXPORT int f12_I_ISS_ID(int p0, struct S_ID p1, struct S_ID p2) { return p0; }
EXPORT int f12_I_ISS_IP(int p0, struct S_IP p1, struct S_IP p2) { return p0; }
EXPORT int f12_I_ISS_FI(int p0, struct S_FI p1, struct S_FI p2) { return p0; }
EXPORT int f12_I_ISS_FF(int p0, struct S_FF p1, struct S_FF p2) { return p0; }
EXPORT int f12_I_ISS_FD(int p0, struct S_FD p1, struct S_FD p2) { return p0; }
EXPORT int f12_I_ISS_FP(int p0, struct S_FP p1, struct S_FP p2) { return p0; }
EXPORT int f12_I_ISS_DI(int p0, struct S_DI p1, struct S_DI p2) { return p0; }
EXPORT int f12_I_ISS_DF(int p0, struct S_DF p1, struct S_DF p2) { return p0; }
EXPORT int f12_I_ISS_DD(int p0, struct S_DD p1, struct S_DD p2) { return p0; }
EXPORT int f12_I_ISS_DP(int p0, struct S_DP p1, struct S_DP p2) { return p0; }
EXPORT int f12_I_ISS_PI(int p0, struct S_PI p1, struct S_PI p2) { return p0; }
EXPORT int f12_I_ISS_PF(int p0, struct S_PF p1, struct S_PF p2) { return p0; }
EXPORT int f12_I_ISS_PD(int p0, struct S_PD p1, struct S_PD p2) { return p0; }
EXPORT int f12_I_ISS_PP(int p0, struct S_PP p1, struct S_PP p2) { return p0; }
EXPORT int f12_I_ISS_III(int p0, struct S_III p1, struct S_III p2) { return p0; }
EXPORT int f12_I_ISS_IIF(int p0, struct S_IIF p1, struct S_IIF p2) { return p0; }
EXPORT int f12_I_ISS_IID(int p0, struct S_IID p1, struct S_IID p2) { return p0; }
EXPORT int f12_I_ISS_IIP(int p0, struct S_IIP p1, struct S_IIP p2) { return p0; }
EXPORT int f12_I_ISS_IFI(int p0, struct S_IFI p1, struct S_IFI p2) { return p0; }
EXPORT int f12_I_ISS_IFF(int p0, struct S_IFF p1, struct S_IFF p2) { return p0; }
EXPORT int f12_I_ISS_IFD(int p0, struct S_IFD p1, struct S_IFD p2) { return p0; }
EXPORT int f12_I_ISS_IFP(int p0, struct S_IFP p1, struct S_IFP p2) { return p0; }
EXPORT int f12_I_ISS_IDI(int p0, struct S_IDI p1, struct S_IDI p2) { return p0; }
EXPORT int f12_I_ISS_IDF(int p0, struct S_IDF p1, struct S_IDF p2) { return p0; }
EXPORT int f12_I_ISS_IDD(int p0, struct S_IDD p1, struct S_IDD p2) { return p0; }
EXPORT int f12_I_ISS_IDP(int p0, struct S_IDP p1, struct S_IDP p2) { return p0; }
EXPORT int f12_I_ISS_IPI(int p0, struct S_IPI p1, struct S_IPI p2) { return p0; }
EXPORT int f12_I_ISS_IPF(int p0, struct S_IPF p1, struct S_IPF p2) { return p0; }
EXPORT int f12_I_ISS_IPD(int p0, struct S_IPD p1, struct S_IPD p2) { return p0; }
EXPORT int f12_I_ISS_IPP(int p0, struct S_IPP p1, struct S_IPP p2) { return p0; }
EXPORT int f12_I_ISS_FII(int p0, struct S_FII p1, struct S_FII p2) { return p0; }
EXPORT int f12_I_ISS_FIF(int p0, struct S_FIF p1, struct S_FIF p2) { return p0; }
EXPORT int f12_I_ISS_FID(int p0, struct S_FID p1, struct S_FID p2) { return p0; }
EXPORT int f12_I_ISS_FIP(int p0, struct S_FIP p1, struct S_FIP p2) { return p0; }
EXPORT int f12_I_ISS_FFI(int p0, struct S_FFI p1, struct S_FFI p2) { return p0; }
EXPORT int f12_I_ISS_FFF(int p0, struct S_FFF p1, struct S_FFF p2) { return p0; }
EXPORT int f12_I_ISS_FFD(int p0, struct S_FFD p1, struct S_FFD p2) { return p0; }
EXPORT int f12_I_ISS_FFP(int p0, struct S_FFP p1, struct S_FFP p2) { return p0; }
EXPORT int f12_I_ISS_FDI(int p0, struct S_FDI p1, struct S_FDI p2) { return p0; }
EXPORT int f12_I_ISS_FDF(int p0, struct S_FDF p1, struct S_FDF p2) { return p0; }
EXPORT int f12_I_ISS_FDD(int p0, struct S_FDD p1, struct S_FDD p2) { return p0; }
EXPORT int f12_I_ISS_FDP(int p0, struct S_FDP p1, struct S_FDP p2) { return p0; }
EXPORT int f12_I_ISS_FPI(int p0, struct S_FPI p1, struct S_FPI p2) { return p0; }
EXPORT int f12_I_ISS_FPF(int p0, struct S_FPF p1, struct S_FPF p2) { return p0; }
EXPORT int f12_I_ISS_FPD(int p0, struct S_FPD p1, struct S_FPD p2) { return p0; }
EXPORT int f12_I_ISS_FPP(int p0, struct S_FPP p1, struct S_FPP p2) { return p0; }
EXPORT int f12_I_ISS_DII(int p0, struct S_DII p1, struct S_DII p2) { return p0; }
EXPORT int f12_I_ISS_DIF(int p0, struct S_DIF p1, struct S_DIF p2) { return p0; }
EXPORT int f12_I_ISS_DID(int p0, struct S_DID p1, struct S_DID p2) { return p0; }
EXPORT int f12_I_ISS_DIP(int p0, struct S_DIP p1, struct S_DIP p2) { return p0; }
EXPORT int f12_I_ISS_DFI(int p0, struct S_DFI p1, struct S_DFI p2) { return p0; }
EXPORT int f12_I_ISS_DFF(int p0, struct S_DFF p1, struct S_DFF p2) { return p0; }
EXPORT int f12_I_ISS_DFD(int p0, struct S_DFD p1, struct S_DFD p2) { return p0; }
EXPORT int f12_I_ISS_DFP(int p0, struct S_DFP p1, struct S_DFP p2) { return p0; }
EXPORT int f12_I_ISS_DDI(int p0, struct S_DDI p1, struct S_DDI p2) { return p0; }
EXPORT int f12_I_ISS_DDF(int p0, struct S_DDF p1, struct S_DDF p2) { return p0; }
EXPORT int f12_I_ISS_DDD(int p0, struct S_DDD p1, struct S_DDD p2) { return p0; }
EXPORT int f12_I_ISS_DDP(int p0, struct S_DDP p1, struct S_DDP p2) { return p0; }
EXPORT int f12_I_ISS_DPI(int p0, struct S_DPI p1, struct S_DPI p2) { return p0; }
EXPORT int f12_I_ISS_DPF(int p0, struct S_DPF p1, struct S_DPF p2) { return p0; }
EXPORT int f12_I_ISS_DPD(int p0, struct S_DPD p1, struct S_DPD p2) { return p0; }
EXPORT int f12_I_ISS_DPP(int p0, struct S_DPP p1, struct S_DPP p2) { return p0; }
EXPORT int f12_I_ISS_PII(int p0, struct S_PII p1, struct S_PII p2) { return p0; }
EXPORT int f12_I_ISS_PIF(int p0, struct S_PIF p1, struct S_PIF p2) { return p0; }
EXPORT int f12_I_ISS_PID(int p0, struct S_PID p1, struct S_PID p2) { return p0; }
EXPORT int f12_I_ISS_PIP(int p0, struct S_PIP p1, struct S_PIP p2) { return p0; }
EXPORT int f12_I_ISS_PFI(int p0, struct S_PFI p1, struct S_PFI p2) { return p0; }
EXPORT int f12_I_ISS_PFF(int p0, struct S_PFF p1, struct S_PFF p2) { return p0; }
EXPORT int f12_I_ISS_PFD(int p0, struct S_PFD p1, struct S_PFD p2) { return p0; }
EXPORT int f12_I_ISS_PFP(int p0, struct S_PFP p1, struct S_PFP p2) { return p0; }
EXPORT int f12_I_ISS_PDI(int p0, struct S_PDI p1, struct S_PDI p2) { return p0; }
EXPORT int f12_I_ISS_PDF(int p0, struct S_PDF p1, struct S_PDF p2) { return p0; }
EXPORT int f12_I_ISS_PDD(int p0, struct S_PDD p1, struct S_PDD p2) { return p0; }
EXPORT int f12_I_ISS_PDP(int p0, struct S_PDP p1, struct S_PDP p2) { return p0; }
EXPORT int f12_I_ISS_PPI(int p0, struct S_PPI p1, struct S_PPI p2) { return p0; }
EXPORT int f12_I_ISS_PPF(int p0, struct S_PPF p1, struct S_PPF p2) { return p0; }
EXPORT int f12_I_ISS_PPD(int p0, struct S_PPD p1, struct S_PPD p2) { return p0; }
EXPORT int f12_I_ISS_PPP(int p0, struct S_PPP p1, struct S_PPP p2) { return p0; }
EXPORT float f12_F_FII_(float p0, int p1, int p2) { return p0; }
EXPORT float f12_F_FIF_(float p0, int p1, float p2) { return p0; }
EXPORT float f12_F_FID_(float p0, int p1, double p2) { return p0; }
EXPORT float f12_F_FIP_(float p0, int p1, void* p2) { return p0; }
EXPORT float f12_F_FIS_I(float p0, int p1, struct S_I p2) { return p0; }
EXPORT float f12_F_FIS_F(float p0, int p1, struct S_F p2) { return p0; }
EXPORT float f12_F_FIS_D(float p0, int p1, struct S_D p2) { return p0; }
EXPORT float f12_F_FIS_P(float p0, int p1, struct S_P p2) { return p0; }
EXPORT float f12_F_FIS_II(float p0, int p1, struct S_II p2) { return p0; }
EXPORT float f12_F_FIS_IF(float p0, int p1, struct S_IF p2) { return p0; }
EXPORT float f12_F_FIS_ID(float p0, int p1, struct S_ID p2) { return p0; }
EXPORT float f12_F_FIS_IP(float p0, int p1, struct S_IP p2) { return p0; }
EXPORT float f12_F_FIS_FI(float p0, int p1, struct S_FI p2) { return p0; }
EXPORT float f12_F_FIS_FF(float p0, int p1, struct S_FF p2) { return p0; }
EXPORT float f12_F_FIS_FD(float p0, int p1, struct S_FD p2) { return p0; }
EXPORT float f12_F_FIS_FP(float p0, int p1, struct S_FP p2) { return p0; }
EXPORT float f12_F_FIS_DI(float p0, int p1, struct S_DI p2) { return p0; }
EXPORT float f12_F_FIS_DF(float p0, int p1, struct S_DF p2) { return p0; }
EXPORT float f12_F_FIS_DD(float p0, int p1, struct S_DD p2) { return p0; }
EXPORT float f12_F_FIS_DP(float p0, int p1, struct S_DP p2) { return p0; }
EXPORT float f12_F_FIS_PI(float p0, int p1, struct S_PI p2) { return p0; }
EXPORT float f12_F_FIS_PF(float p0, int p1, struct S_PF p2) { return p0; }
EXPORT float f12_F_FIS_PD(float p0, int p1, struct S_PD p2) { return p0; }
EXPORT float f12_F_FIS_PP(float p0, int p1, struct S_PP p2) { return p0; }
EXPORT float f12_F_FIS_III(float p0, int p1, struct S_III p2) { return p0; }
EXPORT float f12_F_FIS_IIF(float p0, int p1, struct S_IIF p2) { return p0; }
EXPORT float f12_F_FIS_IID(float p0, int p1, struct S_IID p2) { return p0; }
EXPORT float f12_F_FIS_IIP(float p0, int p1, struct S_IIP p2) { return p0; }
EXPORT float f12_F_FIS_IFI(float p0, int p1, struct S_IFI p2) { return p0; }
EXPORT float f12_F_FIS_IFF(float p0, int p1, struct S_IFF p2) { return p0; }
EXPORT float f12_F_FIS_IFD(float p0, int p1, struct S_IFD p2) { return p0; }
EXPORT float f12_F_FIS_IFP(float p0, int p1, struct S_IFP p2) { return p0; }
EXPORT float f12_F_FIS_IDI(float p0, int p1, struct S_IDI p2) { return p0; }
EXPORT float f12_F_FIS_IDF(float p0, int p1, struct S_IDF p2) { return p0; }
EXPORT float f12_F_FIS_IDD(float p0, int p1, struct S_IDD p2) { return p0; }
EXPORT float f12_F_FIS_IDP(float p0, int p1, struct S_IDP p2) { return p0; }
EXPORT float f12_F_FIS_IPI(float p0, int p1, struct S_IPI p2) { return p0; }
EXPORT float f12_F_FIS_IPF(float p0, int p1, struct S_IPF p2) { return p0; }
EXPORT float f12_F_FIS_IPD(float p0, int p1, struct S_IPD p2) { return p0; }
EXPORT float f12_F_FIS_IPP(float p0, int p1, struct S_IPP p2) { return p0; }
EXPORT float f12_F_FIS_FII(float p0, int p1, struct S_FII p2) { return p0; }
EXPORT float f12_F_FIS_FIF(float p0, int p1, struct S_FIF p2) { return p0; }
EXPORT float f12_F_FIS_FID(float p0, int p1, struct S_FID p2) { return p0; }
EXPORT float f12_F_FIS_FIP(float p0, int p1, struct S_FIP p2) { return p0; }
EXPORT float f12_F_FIS_FFI(float p0, int p1, struct S_FFI p2) { return p0; }
EXPORT float f12_F_FIS_FFF(float p0, int p1, struct S_FFF p2) { return p0; }
EXPORT float f12_F_FIS_FFD(float p0, int p1, struct S_FFD p2) { return p0; }
EXPORT float f12_F_FIS_FFP(float p0, int p1, struct S_FFP p2) { return p0; }
EXPORT float f12_F_FIS_FDI(float p0, int p1, struct S_FDI p2) { return p0; }
EXPORT float f12_F_FIS_FDF(float p0, int p1, struct S_FDF p2) { return p0; }
EXPORT float f12_F_FIS_FDD(float p0, int p1, struct S_FDD p2) { return p0; }
EXPORT float f12_F_FIS_FDP(float p0, int p1, struct S_FDP p2) { return p0; }
EXPORT float f12_F_FIS_FPI(float p0, int p1, struct S_FPI p2) { return p0; }
EXPORT float f12_F_FIS_FPF(float p0, int p1, struct S_FPF p2) { return p0; }
EXPORT float f12_F_FIS_FPD(float p0, int p1, struct S_FPD p2) { return p0; }
EXPORT float f12_F_FIS_FPP(float p0, int p1, struct S_FPP p2) { return p0; }
EXPORT float f12_F_FIS_DII(float p0, int p1, struct S_DII p2) { return p0; }
EXPORT float f12_F_FIS_DIF(float p0, int p1, struct S_DIF p2) { return p0; }
EXPORT float f12_F_FIS_DID(float p0, int p1, struct S_DID p2) { return p0; }
EXPORT float f12_F_FIS_DIP(float p0, int p1, struct S_DIP p2) { return p0; }
EXPORT float f12_F_FIS_DFI(float p0, int p1, struct S_DFI p2) { return p0; }
EXPORT float f12_F_FIS_DFF(float p0, int p1, struct S_DFF p2) { return p0; }
EXPORT float f12_F_FIS_DFD(float p0, int p1, struct S_DFD p2) { return p0; }
EXPORT float f12_F_FIS_DFP(float p0, int p1, struct S_DFP p2) { return p0; }
EXPORT float f12_F_FIS_DDI(float p0, int p1, struct S_DDI p2) { return p0; }
EXPORT float f12_F_FIS_DDF(float p0, int p1, struct S_DDF p2) { return p0; }
EXPORT float f12_F_FIS_DDD(float p0, int p1, struct S_DDD p2) { return p0; }
EXPORT float f12_F_FIS_DDP(float p0, int p1, struct S_DDP p2) { return p0; }
EXPORT float f12_F_FIS_DPI(float p0, int p1, struct S_DPI p2) { return p0; }
EXPORT float f12_F_FIS_DPF(float p0, int p1, struct S_DPF p2) { return p0; }
EXPORT float f12_F_FIS_DPD(float p0, int p1, struct S_DPD p2) { return p0; }
EXPORT float f12_F_FIS_DPP(float p0, int p1, struct S_DPP p2) { return p0; }
EXPORT float f12_F_FIS_PII(float p0, int p1, struct S_PII p2) { return p0; }
EXPORT float f12_F_FIS_PIF(float p0, int p1, struct S_PIF p2) { return p0; }
EXPORT float f12_F_FIS_PID(float p0, int p1, struct S_PID p2) { return p0; }
EXPORT float f12_F_FIS_PIP(float p0, int p1, struct S_PIP p2) { return p0; }
EXPORT float f12_F_FIS_PFI(float p0, int p1, struct S_PFI p2) { return p0; }
EXPORT float f12_F_FIS_PFF(float p0, int p1, struct S_PFF p2) { return p0; }
EXPORT float f12_F_FIS_PFD(float p0, int p1, struct S_PFD p2) { return p0; }
EXPORT float f12_F_FIS_PFP(float p0, int p1, struct S_PFP p2) { return p0; }
EXPORT float f12_F_FIS_PDI(float p0, int p1, struct S_PDI p2) { return p0; }
EXPORT float f12_F_FIS_PDF(float p0, int p1, struct S_PDF p2) { return p0; }
EXPORT float f12_F_FIS_PDD(float p0, int p1, struct S_PDD p2) { return p0; }
EXPORT float f12_F_FIS_PDP(float p0, int p1, struct S_PDP p2) { return p0; }
EXPORT float f12_F_FIS_PPI(float p0, int p1, struct S_PPI p2) { return p0; }
EXPORT float f12_F_FIS_PPF(float p0, int p1, struct S_PPF p2) { return p0; }
EXPORT float f12_F_FIS_PPD(float p0, int p1, struct S_PPD p2) { return p0; }
EXPORT float f12_F_FIS_PPP(float p0, int p1, struct S_PPP p2) { return p0; }
EXPORT float f12_F_FFI_(float p0, float p1, int p2) { return p0; }
EXPORT float f12_F_FFF_(float p0, float p1, float p2) { return p0; }
EXPORT float f12_F_FFD_(float p0, float p1, double p2) { return p0; }
EXPORT float f12_F_FFP_(float p0, float p1, void* p2) { return p0; }
EXPORT float f12_F_FFS_I(float p0, float p1, struct S_I p2) { return p0; }
EXPORT float f12_F_FFS_F(float p0, float p1, struct S_F p2) { return p0; }
EXPORT float f12_F_FFS_D(float p0, float p1, struct S_D p2) { return p0; }
EXPORT float f12_F_FFS_P(float p0, float p1, struct S_P p2) { return p0; }
EXPORT float f12_F_FFS_II(float p0, float p1, struct S_II p2) { return p0; }
EXPORT float f12_F_FFS_IF(float p0, float p1, struct S_IF p2) { return p0; }
EXPORT float f12_F_FFS_ID(float p0, float p1, struct S_ID p2) { return p0; }
EXPORT float f12_F_FFS_IP(float p0, float p1, struct S_IP p2) { return p0; }
EXPORT float f12_F_FFS_FI(float p0, float p1, struct S_FI p2) { return p0; }
EXPORT float f12_F_FFS_FF(float p0, float p1, struct S_FF p2) { return p0; }
EXPORT float f12_F_FFS_FD(float p0, float p1, struct S_FD p2) { return p0; }
EXPORT float f12_F_FFS_FP(float p0, float p1, struct S_FP p2) { return p0; }
EXPORT float f12_F_FFS_DI(float p0, float p1, struct S_DI p2) { return p0; }
EXPORT float f12_F_FFS_DF(float p0, float p1, struct S_DF p2) { return p0; }
EXPORT float f12_F_FFS_DD(float p0, float p1, struct S_DD p2) { return p0; }
EXPORT float f12_F_FFS_DP(float p0, float p1, struct S_DP p2) { return p0; }
EXPORT float f12_F_FFS_PI(float p0, float p1, struct S_PI p2) { return p0; }
EXPORT float f12_F_FFS_PF(float p0, float p1, struct S_PF p2) { return p0; }
EXPORT float f12_F_FFS_PD(float p0, float p1, struct S_PD p2) { return p0; }
EXPORT float f12_F_FFS_PP(float p0, float p1, struct S_PP p2) { return p0; }
EXPORT float f12_F_FFS_III(float p0, float p1, struct S_III p2) { return p0; }
EXPORT float f12_F_FFS_IIF(float p0, float p1, struct S_IIF p2) { return p0; }
EXPORT float f12_F_FFS_IID(float p0, float p1, struct S_IID p2) { return p0; }
EXPORT float f12_F_FFS_IIP(float p0, float p1, struct S_IIP p2) { return p0; }
EXPORT float f12_F_FFS_IFI(float p0, float p1, struct S_IFI p2) { return p0; }
EXPORT float f12_F_FFS_IFF(float p0, float p1, struct S_IFF p2) { return p0; }
EXPORT float f12_F_FFS_IFD(float p0, float p1, struct S_IFD p2) { return p0; }
EXPORT float f13_F_FFS_IFP(float p0, float p1, struct S_IFP p2) { return p0; }
EXPORT float f13_F_FFS_IDI(float p0, float p1, struct S_IDI p2) { return p0; }
EXPORT float f13_F_FFS_IDF(float p0, float p1, struct S_IDF p2) { return p0; }
EXPORT float f13_F_FFS_IDD(float p0, float p1, struct S_IDD p2) { return p0; }
EXPORT float f13_F_FFS_IDP(float p0, float p1, struct S_IDP p2) { return p0; }
EXPORT float f13_F_FFS_IPI(float p0, float p1, struct S_IPI p2) { return p0; }
EXPORT float f13_F_FFS_IPF(float p0, float p1, struct S_IPF p2) { return p0; }
EXPORT float f13_F_FFS_IPD(float p0, float p1, struct S_IPD p2) { return p0; }
EXPORT float f13_F_FFS_IPP(float p0, float p1, struct S_IPP p2) { return p0; }
EXPORT float f13_F_FFS_FII(float p0, float p1, struct S_FII p2) { return p0; }
EXPORT float f13_F_FFS_FIF(float p0, float p1, struct S_FIF p2) { return p0; }
EXPORT float f13_F_FFS_FID(float p0, float p1, struct S_FID p2) { return p0; }
EXPORT float f13_F_FFS_FIP(float p0, float p1, struct S_FIP p2) { return p0; }
EXPORT float f13_F_FFS_FFI(float p0, float p1, struct S_FFI p2) { return p0; }
EXPORT float f13_F_FFS_FFF(float p0, float p1, struct S_FFF p2) { return p0; }
EXPORT float f13_F_FFS_FFD(float p0, float p1, struct S_FFD p2) { return p0; }
EXPORT float f13_F_FFS_FFP(float p0, float p1, struct S_FFP p2) { return p0; }
EXPORT float f13_F_FFS_FDI(float p0, float p1, struct S_FDI p2) { return p0; }
EXPORT float f13_F_FFS_FDF(float p0, float p1, struct S_FDF p2) { return p0; }
EXPORT float f13_F_FFS_FDD(float p0, float p1, struct S_FDD p2) { return p0; }
EXPORT float f13_F_FFS_FDP(float p0, float p1, struct S_FDP p2) { return p0; }
EXPORT float f13_F_FFS_FPI(float p0, float p1, struct S_FPI p2) { return p0; }
EXPORT float f13_F_FFS_FPF(float p0, float p1, struct S_FPF p2) { return p0; }
EXPORT float f13_F_FFS_FPD(float p0, float p1, struct S_FPD p2) { return p0; }
EXPORT float f13_F_FFS_FPP(float p0, float p1, struct S_FPP p2) { return p0; }
EXPORT float f13_F_FFS_DII(float p0, float p1, struct S_DII p2) { return p0; }
EXPORT float f13_F_FFS_DIF(float p0, float p1, struct S_DIF p2) { return p0; }
EXPORT float f13_F_FFS_DID(float p0, float p1, struct S_DID p2) { return p0; }
EXPORT float f13_F_FFS_DIP(float p0, float p1, struct S_DIP p2) { return p0; }
EXPORT float f13_F_FFS_DFI(float p0, float p1, struct S_DFI p2) { return p0; }
EXPORT float f13_F_FFS_DFF(float p0, float p1, struct S_DFF p2) { return p0; }
EXPORT float f13_F_FFS_DFD(float p0, float p1, struct S_DFD p2) { return p0; }
EXPORT float f13_F_FFS_DFP(float p0, float p1, struct S_DFP p2) { return p0; }
EXPORT float f13_F_FFS_DDI(float p0, float p1, struct S_DDI p2) { return p0; }
EXPORT float f13_F_FFS_DDF(float p0, float p1, struct S_DDF p2) { return p0; }
EXPORT float f13_F_FFS_DDD(float p0, float p1, struct S_DDD p2) { return p0; }
EXPORT float f13_F_FFS_DDP(float p0, float p1, struct S_DDP p2) { return p0; }
EXPORT float f13_F_FFS_DPI(float p0, float p1, struct S_DPI p2) { return p0; }
EXPORT float f13_F_FFS_DPF(float p0, float p1, struct S_DPF p2) { return p0; }
EXPORT float f13_F_FFS_DPD(float p0, float p1, struct S_DPD p2) { return p0; }
EXPORT float f13_F_FFS_DPP(float p0, float p1, struct S_DPP p2) { return p0; }
EXPORT float f13_F_FFS_PII(float p0, float p1, struct S_PII p2) { return p0; }
EXPORT float f13_F_FFS_PIF(float p0, float p1, struct S_PIF p2) { return p0; }
EXPORT float f13_F_FFS_PID(float p0, float p1, struct S_PID p2) { return p0; }
EXPORT float f13_F_FFS_PIP(float p0, float p1, struct S_PIP p2) { return p0; }
EXPORT float f13_F_FFS_PFI(float p0, float p1, struct S_PFI p2) { return p0; }
EXPORT float f13_F_FFS_PFF(float p0, float p1, struct S_PFF p2) { return p0; }
EXPORT float f13_F_FFS_PFD(float p0, float p1, struct S_PFD p2) { return p0; }
EXPORT float f13_F_FFS_PFP(float p0, float p1, struct S_PFP p2) { return p0; }
EXPORT float f13_F_FFS_PDI(float p0, float p1, struct S_PDI p2) { return p0; }
EXPORT float f13_F_FFS_PDF(float p0, float p1, struct S_PDF p2) { return p0; }
EXPORT float f13_F_FFS_PDD(float p0, float p1, struct S_PDD p2) { return p0; }
EXPORT float f13_F_FFS_PDP(float p0, float p1, struct S_PDP p2) { return p0; }
EXPORT float f13_F_FFS_PPI(float p0, float p1, struct S_PPI p2) { return p0; }
EXPORT float f13_F_FFS_PPF(float p0, float p1, struct S_PPF p2) { return p0; }
EXPORT float f13_F_FFS_PPD(float p0, float p1, struct S_PPD p2) { return p0; }
EXPORT float f13_F_FFS_PPP(float p0, float p1, struct S_PPP p2) { return p0; }
EXPORT float f13_F_FDI_(float p0, double p1, int p2) { return p0; }
EXPORT float f13_F_FDF_(float p0, double p1, float p2) { return p0; }
EXPORT float f13_F_FDD_(float p0, double p1, double p2) { return p0; }
EXPORT float f13_F_FDP_(float p0, double p1, void* p2) { return p0; }
EXPORT float f13_F_FDS_I(float p0, double p1, struct S_I p2) { return p0; }
EXPORT float f13_F_FDS_F(float p0, double p1, struct S_F p2) { return p0; }
EXPORT float f13_F_FDS_D(float p0, double p1, struct S_D p2) { return p0; }
EXPORT float f13_F_FDS_P(float p0, double p1, struct S_P p2) { return p0; }
EXPORT float f13_F_FDS_II(float p0, double p1, struct S_II p2) { return p0; }
EXPORT float f13_F_FDS_IF(float p0, double p1, struct S_IF p2) { return p0; }
EXPORT float f13_F_FDS_ID(float p0, double p1, struct S_ID p2) { return p0; }
EXPORT float f13_F_FDS_IP(float p0, double p1, struct S_IP p2) { return p0; }
EXPORT float f13_F_FDS_FI(float p0, double p1, struct S_FI p2) { return p0; }
EXPORT float f13_F_FDS_FF(float p0, double p1, struct S_FF p2) { return p0; }
EXPORT float f13_F_FDS_FD(float p0, double p1, struct S_FD p2) { return p0; }
EXPORT float f13_F_FDS_FP(float p0, double p1, struct S_FP p2) { return p0; }
EXPORT float f13_F_FDS_DI(float p0, double p1, struct S_DI p2) { return p0; }
EXPORT float f13_F_FDS_DF(float p0, double p1, struct S_DF p2) { return p0; }
EXPORT float f13_F_FDS_DD(float p0, double p1, struct S_DD p2) { return p0; }
EXPORT float f13_F_FDS_DP(float p0, double p1, struct S_DP p2) { return p0; }
EXPORT float f13_F_FDS_PI(float p0, double p1, struct S_PI p2) { return p0; }
EXPORT float f13_F_FDS_PF(float p0, double p1, struct S_PF p2) { return p0; }
EXPORT float f13_F_FDS_PD(float p0, double p1, struct S_PD p2) { return p0; }
EXPORT float f13_F_FDS_PP(float p0, double p1, struct S_PP p2) { return p0; }
EXPORT float f13_F_FDS_III(float p0, double p1, struct S_III p2) { return p0; }
EXPORT float f13_F_FDS_IIF(float p0, double p1, struct S_IIF p2) { return p0; }
EXPORT float f13_F_FDS_IID(float p0, double p1, struct S_IID p2) { return p0; }
EXPORT float f13_F_FDS_IIP(float p0, double p1, struct S_IIP p2) { return p0; }
EXPORT float f13_F_FDS_IFI(float p0, double p1, struct S_IFI p2) { return p0; }
EXPORT float f13_F_FDS_IFF(float p0, double p1, struct S_IFF p2) { return p0; }
EXPORT float f13_F_FDS_IFD(float p0, double p1, struct S_IFD p2) { return p0; }
EXPORT float f13_F_FDS_IFP(float p0, double p1, struct S_IFP p2) { return p0; }
EXPORT float f13_F_FDS_IDI(float p0, double p1, struct S_IDI p2) { return p0; }
EXPORT float f13_F_FDS_IDF(float p0, double p1, struct S_IDF p2) { return p0; }
EXPORT float f13_F_FDS_IDD(float p0, double p1, struct S_IDD p2) { return p0; }
EXPORT float f13_F_FDS_IDP(float p0, double p1, struct S_IDP p2) { return p0; }
EXPORT float f13_F_FDS_IPI(float p0, double p1, struct S_IPI p2) { return p0; }
EXPORT float f13_F_FDS_IPF(float p0, double p1, struct S_IPF p2) { return p0; }
EXPORT float f13_F_FDS_IPD(float p0, double p1, struct S_IPD p2) { return p0; }
EXPORT float f13_F_FDS_IPP(float p0, double p1, struct S_IPP p2) { return p0; }
EXPORT float f13_F_FDS_FII(float p0, double p1, struct S_FII p2) { return p0; }
EXPORT float f13_F_FDS_FIF(float p0, double p1, struct S_FIF p2) { return p0; }
EXPORT float f13_F_FDS_FID(float p0, double p1, struct S_FID p2) { return p0; }
EXPORT float f13_F_FDS_FIP(float p0, double p1, struct S_FIP p2) { return p0; }
EXPORT float f13_F_FDS_FFI(float p0, double p1, struct S_FFI p2) { return p0; }
EXPORT float f13_F_FDS_FFF(float p0, double p1, struct S_FFF p2) { return p0; }
EXPORT float f13_F_FDS_FFD(float p0, double p1, struct S_FFD p2) { return p0; }
EXPORT float f13_F_FDS_FFP(float p0, double p1, struct S_FFP p2) { return p0; }
EXPORT float f13_F_FDS_FDI(float p0, double p1, struct S_FDI p2) { return p0; }
EXPORT float f13_F_FDS_FDF(float p0, double p1, struct S_FDF p2) { return p0; }
EXPORT float f13_F_FDS_FDD(float p0, double p1, struct S_FDD p2) { return p0; }
EXPORT float f13_F_FDS_FDP(float p0, double p1, struct S_FDP p2) { return p0; }
EXPORT float f13_F_FDS_FPI(float p0, double p1, struct S_FPI p2) { return p0; }
EXPORT float f13_F_FDS_FPF(float p0, double p1, struct S_FPF p2) { return p0; }
EXPORT float f13_F_FDS_FPD(float p0, double p1, struct S_FPD p2) { return p0; }
EXPORT float f13_F_FDS_FPP(float p0, double p1, struct S_FPP p2) { return p0; }
EXPORT float f13_F_FDS_DII(float p0, double p1, struct S_DII p2) { return p0; }
EXPORT float f13_F_FDS_DIF(float p0, double p1, struct S_DIF p2) { return p0; }
EXPORT float f13_F_FDS_DID(float p0, double p1, struct S_DID p2) { return p0; }
EXPORT float f13_F_FDS_DIP(float p0, double p1, struct S_DIP p2) { return p0; }
EXPORT float f13_F_FDS_DFI(float p0, double p1, struct S_DFI p2) { return p0; }
EXPORT float f13_F_FDS_DFF(float p0, double p1, struct S_DFF p2) { return p0; }
EXPORT float f13_F_FDS_DFD(float p0, double p1, struct S_DFD p2) { return p0; }
EXPORT float f13_F_FDS_DFP(float p0, double p1, struct S_DFP p2) { return p0; }
EXPORT float f13_F_FDS_DDI(float p0, double p1, struct S_DDI p2) { return p0; }
EXPORT float f13_F_FDS_DDF(float p0, double p1, struct S_DDF p2) { return p0; }
EXPORT float f13_F_FDS_DDD(float p0, double p1, struct S_DDD p2) { return p0; }
EXPORT float f13_F_FDS_DDP(float p0, double p1, struct S_DDP p2) { return p0; }
EXPORT float f13_F_FDS_DPI(float p0, double p1, struct S_DPI p2) { return p0; }
EXPORT float f13_F_FDS_DPF(float p0, double p1, struct S_DPF p2) { return p0; }
EXPORT float f13_F_FDS_DPD(float p0, double p1, struct S_DPD p2) { return p0; }
EXPORT float f13_F_FDS_DPP(float p0, double p1, struct S_DPP p2) { return p0; }
EXPORT float f13_F_FDS_PII(float p0, double p1, struct S_PII p2) { return p0; }
EXPORT float f13_F_FDS_PIF(float p0, double p1, struct S_PIF p2) { return p0; }
EXPORT float f13_F_FDS_PID(float p0, double p1, struct S_PID p2) { return p0; }
EXPORT float f13_F_FDS_PIP(float p0, double p1, struct S_PIP p2) { return p0; }
EXPORT float f13_F_FDS_PFI(float p0, double p1, struct S_PFI p2) { return p0; }
EXPORT float f13_F_FDS_PFF(float p0, double p1, struct S_PFF p2) { return p0; }
EXPORT float f13_F_FDS_PFD(float p0, double p1, struct S_PFD p2) { return p0; }
EXPORT float f13_F_FDS_PFP(float p0, double p1, struct S_PFP p2) { return p0; }
EXPORT float f13_F_FDS_PDI(float p0, double p1, struct S_PDI p2) { return p0; }
EXPORT float f13_F_FDS_PDF(float p0, double p1, struct S_PDF p2) { return p0; }
EXPORT float f13_F_FDS_PDD(float p0, double p1, struct S_PDD p2) { return p0; }
EXPORT float f13_F_FDS_PDP(float p0, double p1, struct S_PDP p2) { return p0; }
EXPORT float f13_F_FDS_PPI(float p0, double p1, struct S_PPI p2) { return p0; }
EXPORT float f13_F_FDS_PPF(float p0, double p1, struct S_PPF p2) { return p0; }
EXPORT float f13_F_FDS_PPD(float p0, double p1, struct S_PPD p2) { return p0; }
EXPORT float f13_F_FDS_PPP(float p0, double p1, struct S_PPP p2) { return p0; }
EXPORT float f13_F_FPI_(float p0, void* p1, int p2) { return p0; }
EXPORT float f13_F_FPF_(float p0, void* p1, float p2) { return p0; }
EXPORT float f13_F_FPD_(float p0, void* p1, double p2) { return p0; }
EXPORT float f13_F_FPP_(float p0, void* p1, void* p2) { return p0; }
EXPORT float f13_F_FPS_I(float p0, void* p1, struct S_I p2) { return p0; }
EXPORT float f13_F_FPS_F(float p0, void* p1, struct S_F p2) { return p0; }
EXPORT float f13_F_FPS_D(float p0, void* p1, struct S_D p2) { return p0; }
EXPORT float f13_F_FPS_P(float p0, void* p1, struct S_P p2) { return p0; }
EXPORT float f13_F_FPS_II(float p0, void* p1, struct S_II p2) { return p0; }
EXPORT float f13_F_FPS_IF(float p0, void* p1, struct S_IF p2) { return p0; }
EXPORT float f13_F_FPS_ID(float p0, void* p1, struct S_ID p2) { return p0; }
EXPORT float f13_F_FPS_IP(float p0, void* p1, struct S_IP p2) { return p0; }
EXPORT float f13_F_FPS_FI(float p0, void* p1, struct S_FI p2) { return p0; }
EXPORT float f13_F_FPS_FF(float p0, void* p1, struct S_FF p2) { return p0; }
EXPORT float f13_F_FPS_FD(float p0, void* p1, struct S_FD p2) { return p0; }
EXPORT float f13_F_FPS_FP(float p0, void* p1, struct S_FP p2) { return p0; }
EXPORT float f13_F_FPS_DI(float p0, void* p1, struct S_DI p2) { return p0; }
EXPORT float f13_F_FPS_DF(float p0, void* p1, struct S_DF p2) { return p0; }
EXPORT float f13_F_FPS_DD(float p0, void* p1, struct S_DD p2) { return p0; }
EXPORT float f13_F_FPS_DP(float p0, void* p1, struct S_DP p2) { return p0; }
EXPORT float f13_F_FPS_PI(float p0, void* p1, struct S_PI p2) { return p0; }
EXPORT float f13_F_FPS_PF(float p0, void* p1, struct S_PF p2) { return p0; }
EXPORT float f13_F_FPS_PD(float p0, void* p1, struct S_PD p2) { return p0; }
EXPORT float f13_F_FPS_PP(float p0, void* p1, struct S_PP p2) { return p0; }
EXPORT float f13_F_FPS_III(float p0, void* p1, struct S_III p2) { return p0; }
EXPORT float f13_F_FPS_IIF(float p0, void* p1, struct S_IIF p2) { return p0; }
EXPORT float f13_F_FPS_IID(float p0, void* p1, struct S_IID p2) { return p0; }
EXPORT float f13_F_FPS_IIP(float p0, void* p1, struct S_IIP p2) { return p0; }
EXPORT float f13_F_FPS_IFI(float p0, void* p1, struct S_IFI p2) { return p0; }
EXPORT float f13_F_FPS_IFF(float p0, void* p1, struct S_IFF p2) { return p0; }
EXPORT float f13_F_FPS_IFD(float p0, void* p1, struct S_IFD p2) { return p0; }
EXPORT float f13_F_FPS_IFP(float p0, void* p1, struct S_IFP p2) { return p0; }
EXPORT float f13_F_FPS_IDI(float p0, void* p1, struct S_IDI p2) { return p0; }
EXPORT float f13_F_FPS_IDF(float p0, void* p1, struct S_IDF p2) { return p0; }
EXPORT float f13_F_FPS_IDD(float p0, void* p1, struct S_IDD p2) { return p0; }
EXPORT float f13_F_FPS_IDP(float p0, void* p1, struct S_IDP p2) { return p0; }
EXPORT float f13_F_FPS_IPI(float p0, void* p1, struct S_IPI p2) { return p0; }
EXPORT float f13_F_FPS_IPF(float p0, void* p1, struct S_IPF p2) { return p0; }
EXPORT float f13_F_FPS_IPD(float p0, void* p1, struct S_IPD p2) { return p0; }
EXPORT float f13_F_FPS_IPP(float p0, void* p1, struct S_IPP p2) { return p0; }
EXPORT float f13_F_FPS_FII(float p0, void* p1, struct S_FII p2) { return p0; }
EXPORT float f13_F_FPS_FIF(float p0, void* p1, struct S_FIF p2) { return p0; }
EXPORT float f13_F_FPS_FID(float p0, void* p1, struct S_FID p2) { return p0; }
EXPORT float f13_F_FPS_FIP(float p0, void* p1, struct S_FIP p2) { return p0; }
EXPORT float f13_F_FPS_FFI(float p0, void* p1, struct S_FFI p2) { return p0; }
EXPORT float f13_F_FPS_FFF(float p0, void* p1, struct S_FFF p2) { return p0; }
EXPORT float f13_F_FPS_FFD(float p0, void* p1, struct S_FFD p2) { return p0; }
EXPORT float f13_F_FPS_FFP(float p0, void* p1, struct S_FFP p2) { return p0; }
EXPORT float f13_F_FPS_FDI(float p0, void* p1, struct S_FDI p2) { return p0; }
EXPORT float f13_F_FPS_FDF(float p0, void* p1, struct S_FDF p2) { return p0; }
EXPORT float f13_F_FPS_FDD(float p0, void* p1, struct S_FDD p2) { return p0; }
EXPORT float f13_F_FPS_FDP(float p0, void* p1, struct S_FDP p2) { return p0; }
EXPORT float f13_F_FPS_FPI(float p0, void* p1, struct S_FPI p2) { return p0; }
EXPORT float f13_F_FPS_FPF(float p0, void* p1, struct S_FPF p2) { return p0; }
EXPORT float f13_F_FPS_FPD(float p0, void* p1, struct S_FPD p2) { return p0; }
EXPORT float f13_F_FPS_FPP(float p0, void* p1, struct S_FPP p2) { return p0; }
EXPORT float f13_F_FPS_DII(float p0, void* p1, struct S_DII p2) { return p0; }
EXPORT float f13_F_FPS_DIF(float p0, void* p1, struct S_DIF p2) { return p0; }
EXPORT float f13_F_FPS_DID(float p0, void* p1, struct S_DID p2) { return p0; }
EXPORT float f13_F_FPS_DIP(float p0, void* p1, struct S_DIP p2) { return p0; }
EXPORT float f13_F_FPS_DFI(float p0, void* p1, struct S_DFI p2) { return p0; }
EXPORT float f13_F_FPS_DFF(float p0, void* p1, struct S_DFF p2) { return p0; }
EXPORT float f13_F_FPS_DFD(float p0, void* p1, struct S_DFD p2) { return p0; }
EXPORT float f13_F_FPS_DFP(float p0, void* p1, struct S_DFP p2) { return p0; }
EXPORT float f13_F_FPS_DDI(float p0, void* p1, struct S_DDI p2) { return p0; }
EXPORT float f13_F_FPS_DDF(float p0, void* p1, struct S_DDF p2) { return p0; }
EXPORT float f13_F_FPS_DDD(float p0, void* p1, struct S_DDD p2) { return p0; }
EXPORT float f13_F_FPS_DDP(float p0, void* p1, struct S_DDP p2) { return p0; }
EXPORT float f13_F_FPS_DPI(float p0, void* p1, struct S_DPI p2) { return p0; }
EXPORT float f13_F_FPS_DPF(float p0, void* p1, struct S_DPF p2) { return p0; }
EXPORT float f13_F_FPS_DPD(float p0, void* p1, struct S_DPD p2) { return p0; }
EXPORT float f13_F_FPS_DPP(float p0, void* p1, struct S_DPP p2) { return p0; }
EXPORT float f13_F_FPS_PII(float p0, void* p1, struct S_PII p2) { return p0; }
EXPORT float f13_F_FPS_PIF(float p0, void* p1, struct S_PIF p2) { return p0; }
EXPORT float f13_F_FPS_PID(float p0, void* p1, struct S_PID p2) { return p0; }
EXPORT float f13_F_FPS_PIP(float p0, void* p1, struct S_PIP p2) { return p0; }
EXPORT float f13_F_FPS_PFI(float p0, void* p1, struct S_PFI p2) { return p0; }
EXPORT float f13_F_FPS_PFF(float p0, void* p1, struct S_PFF p2) { return p0; }
EXPORT float f13_F_FPS_PFD(float p0, void* p1, struct S_PFD p2) { return p0; }
EXPORT float f13_F_FPS_PFP(float p0, void* p1, struct S_PFP p2) { return p0; }
EXPORT float f13_F_FPS_PDI(float p0, void* p1, struct S_PDI p2) { return p0; }
EXPORT float f13_F_FPS_PDF(float p0, void* p1, struct S_PDF p2) { return p0; }
EXPORT float f13_F_FPS_PDD(float p0, void* p1, struct S_PDD p2) { return p0; }
EXPORT float f13_F_FPS_PDP(float p0, void* p1, struct S_PDP p2) { return p0; }
EXPORT float f13_F_FPS_PPI(float p0, void* p1, struct S_PPI p2) { return p0; }
EXPORT float f13_F_FPS_PPF(float p0, void* p1, struct S_PPF p2) { return p0; }
EXPORT float f13_F_FPS_PPD(float p0, void* p1, struct S_PPD p2) { return p0; }
EXPORT float f13_F_FPS_PPP(float p0, void* p1, struct S_PPP p2) { return p0; }
EXPORT float f13_F_FSI_I(float p0, struct S_I p1, int p2) { return p0; }
EXPORT float f13_F_FSI_F(float p0, struct S_F p1, int p2) { return p0; }
EXPORT float f13_F_FSI_D(float p0, struct S_D p1, int p2) { return p0; }
EXPORT float f13_F_FSI_P(float p0, struct S_P p1, int p2) { return p0; }
EXPORT float f13_F_FSI_II(float p0, struct S_II p1, int p2) { return p0; }
EXPORT float f13_F_FSI_IF(float p0, struct S_IF p1, int p2) { return p0; }
EXPORT float f13_F_FSI_ID(float p0, struct S_ID p1, int p2) { return p0; }
EXPORT float f13_F_FSI_IP(float p0, struct S_IP p1, int p2) { return p0; }
EXPORT float f13_F_FSI_FI(float p0, struct S_FI p1, int p2) { return p0; }
EXPORT float f13_F_FSI_FF(float p0, struct S_FF p1, int p2) { return p0; }
EXPORT float f13_F_FSI_FD(float p0, struct S_FD p1, int p2) { return p0; }
EXPORT float f13_F_FSI_FP(float p0, struct S_FP p1, int p2) { return p0; }
EXPORT float f13_F_FSI_DI(float p0, struct S_DI p1, int p2) { return p0; }
EXPORT float f13_F_FSI_DF(float p0, struct S_DF p1, int p2) { return p0; }
EXPORT float f13_F_FSI_DD(float p0, struct S_DD p1, int p2) { return p0; }
EXPORT float f13_F_FSI_DP(float p0, struct S_DP p1, int p2) { return p0; }
EXPORT float f13_F_FSI_PI(float p0, struct S_PI p1, int p2) { return p0; }
EXPORT float f13_F_FSI_PF(float p0, struct S_PF p1, int p2) { return p0; }
EXPORT float f13_F_FSI_PD(float p0, struct S_PD p1, int p2) { return p0; }
EXPORT float f13_F_FSI_PP(float p0, struct S_PP p1, int p2) { return p0; }
EXPORT float f13_F_FSI_III(float p0, struct S_III p1, int p2) { return p0; }
EXPORT float f13_F_FSI_IIF(float p0, struct S_IIF p1, int p2) { return p0; }
EXPORT float f13_F_FSI_IID(float p0, struct S_IID p1, int p2) { return p0; }
EXPORT float f13_F_FSI_IIP(float p0, struct S_IIP p1, int p2) { return p0; }
EXPORT float f13_F_FSI_IFI(float p0, struct S_IFI p1, int p2) { return p0; }
EXPORT float f13_F_FSI_IFF(float p0, struct S_IFF p1, int p2) { return p0; }
EXPORT float f13_F_FSI_IFD(float p0, struct S_IFD p1, int p2) { return p0; }
EXPORT float f13_F_FSI_IFP(float p0, struct S_IFP p1, int p2) { return p0; }
EXPORT float f13_F_FSI_IDI(float p0, struct S_IDI p1, int p2) { return p0; }
EXPORT float f13_F_FSI_IDF(float p0, struct S_IDF p1, int p2) { return p0; }
EXPORT float f13_F_FSI_IDD(float p0, struct S_IDD p1, int p2) { return p0; }
EXPORT float f13_F_FSI_IDP(float p0, struct S_IDP p1, int p2) { return p0; }
EXPORT float f13_F_FSI_IPI(float p0, struct S_IPI p1, int p2) { return p0; }
EXPORT float f13_F_FSI_IPF(float p0, struct S_IPF p1, int p2) { return p0; }
EXPORT float f13_F_FSI_IPD(float p0, struct S_IPD p1, int p2) { return p0; }
EXPORT float f13_F_FSI_IPP(float p0, struct S_IPP p1, int p2) { return p0; }
EXPORT float f13_F_FSI_FII(float p0, struct S_FII p1, int p2) { return p0; }
EXPORT float f13_F_FSI_FIF(float p0, struct S_FIF p1, int p2) { return p0; }
EXPORT float f13_F_FSI_FID(float p0, struct S_FID p1, int p2) { return p0; }
EXPORT float f13_F_FSI_FIP(float p0, struct S_FIP p1, int p2) { return p0; }
EXPORT float f13_F_FSI_FFI(float p0, struct S_FFI p1, int p2) { return p0; }
EXPORT float f13_F_FSI_FFF(float p0, struct S_FFF p1, int p2) { return p0; }
EXPORT float f13_F_FSI_FFD(float p0, struct S_FFD p1, int p2) { return p0; }
EXPORT float f13_F_FSI_FFP(float p0, struct S_FFP p1, int p2) { return p0; }
EXPORT float f13_F_FSI_FDI(float p0, struct S_FDI p1, int p2) { return p0; }
EXPORT float f13_F_FSI_FDF(float p0, struct S_FDF p1, int p2) { return p0; }
EXPORT float f13_F_FSI_FDD(float p0, struct S_FDD p1, int p2) { return p0; }
EXPORT float f13_F_FSI_FDP(float p0, struct S_FDP p1, int p2) { return p0; }
EXPORT float f13_F_FSI_FPI(float p0, struct S_FPI p1, int p2) { return p0; }
EXPORT float f13_F_FSI_FPF(float p0, struct S_FPF p1, int p2) { return p0; }
EXPORT float f13_F_FSI_FPD(float p0, struct S_FPD p1, int p2) { return p0; }
EXPORT float f13_F_FSI_FPP(float p0, struct S_FPP p1, int p2) { return p0; }
EXPORT float f13_F_FSI_DII(float p0, struct S_DII p1, int p2) { return p0; }
EXPORT float f13_F_FSI_DIF(float p0, struct S_DIF p1, int p2) { return p0; }
EXPORT float f13_F_FSI_DID(float p0, struct S_DID p1, int p2) { return p0; }
EXPORT float f13_F_FSI_DIP(float p0, struct S_DIP p1, int p2) { return p0; }
EXPORT float f13_F_FSI_DFI(float p0, struct S_DFI p1, int p2) { return p0; }
EXPORT float f13_F_FSI_DFF(float p0, struct S_DFF p1, int p2) { return p0; }
EXPORT float f13_F_FSI_DFD(float p0, struct S_DFD p1, int p2) { return p0; }
EXPORT float f13_F_FSI_DFP(float p0, struct S_DFP p1, int p2) { return p0; }
EXPORT float f13_F_FSI_DDI(float p0, struct S_DDI p1, int p2) { return p0; }
EXPORT float f13_F_FSI_DDF(float p0, struct S_DDF p1, int p2) { return p0; }
EXPORT float f13_F_FSI_DDD(float p0, struct S_DDD p1, int p2) { return p0; }
EXPORT float f13_F_FSI_DDP(float p0, struct S_DDP p1, int p2) { return p0; }
EXPORT float f13_F_FSI_DPI(float p0, struct S_DPI p1, int p2) { return p0; }
EXPORT float f13_F_FSI_DPF(float p0, struct S_DPF p1, int p2) { return p0; }
EXPORT float f13_F_FSI_DPD(float p0, struct S_DPD p1, int p2) { return p0; }
EXPORT float f13_F_FSI_DPP(float p0, struct S_DPP p1, int p2) { return p0; }
EXPORT float f13_F_FSI_PII(float p0, struct S_PII p1, int p2) { return p0; }
EXPORT float f13_F_FSI_PIF(float p0, struct S_PIF p1, int p2) { return p0; }
EXPORT float f13_F_FSI_PID(float p0, struct S_PID p1, int p2) { return p0; }
EXPORT float f13_F_FSI_PIP(float p0, struct S_PIP p1, int p2) { return p0; }
EXPORT float f13_F_FSI_PFI(float p0, struct S_PFI p1, int p2) { return p0; }
EXPORT float f13_F_FSI_PFF(float p0, struct S_PFF p1, int p2) { return p0; }
EXPORT float f13_F_FSI_PFD(float p0, struct S_PFD p1, int p2) { return p0; }
EXPORT float f13_F_FSI_PFP(float p0, struct S_PFP p1, int p2) { return p0; }
EXPORT float f13_F_FSI_PDI(float p0, struct S_PDI p1, int p2) { return p0; }
EXPORT float f13_F_FSI_PDF(float p0, struct S_PDF p1, int p2) { return p0; }
EXPORT float f13_F_FSI_PDD(float p0, struct S_PDD p1, int p2) { return p0; }
EXPORT float f13_F_FSI_PDP(float p0, struct S_PDP p1, int p2) { return p0; }
EXPORT float f13_F_FSI_PPI(float p0, struct S_PPI p1, int p2) { return p0; }
EXPORT float f13_F_FSI_PPF(float p0, struct S_PPF p1, int p2) { return p0; }
EXPORT float f13_F_FSI_PPD(float p0, struct S_PPD p1, int p2) { return p0; }
EXPORT float f13_F_FSI_PPP(float p0, struct S_PPP p1, int p2) { return p0; }
EXPORT float f13_F_FSF_I(float p0, struct S_I p1, float p2) { return p0; }
EXPORT float f13_F_FSF_F(float p0, struct S_F p1, float p2) { return p0; }
EXPORT float f13_F_FSF_D(float p0, struct S_D p1, float p2) { return p0; }
EXPORT float f13_F_FSF_P(float p0, struct S_P p1, float p2) { return p0; }
EXPORT float f13_F_FSF_II(float p0, struct S_II p1, float p2) { return p0; }
EXPORT float f13_F_FSF_IF(float p0, struct S_IF p1, float p2) { return p0; }
EXPORT float f13_F_FSF_ID(float p0, struct S_ID p1, float p2) { return p0; }
EXPORT float f13_F_FSF_IP(float p0, struct S_IP p1, float p2) { return p0; }
EXPORT float f13_F_FSF_FI(float p0, struct S_FI p1, float p2) { return p0; }
EXPORT float f13_F_FSF_FF(float p0, struct S_FF p1, float p2) { return p0; }
EXPORT float f13_F_FSF_FD(float p0, struct S_FD p1, float p2) { return p0; }
EXPORT float f13_F_FSF_FP(float p0, struct S_FP p1, float p2) { return p0; }
EXPORT float f13_F_FSF_DI(float p0, struct S_DI p1, float p2) { return p0; }
EXPORT float f13_F_FSF_DF(float p0, struct S_DF p1, float p2) { return p0; }
EXPORT float f13_F_FSF_DD(float p0, struct S_DD p1, float p2) { return p0; }
EXPORT float f13_F_FSF_DP(float p0, struct S_DP p1, float p2) { return p0; }
EXPORT float f13_F_FSF_PI(float p0, struct S_PI p1, float p2) { return p0; }
EXPORT float f13_F_FSF_PF(float p0, struct S_PF p1, float p2) { return p0; }
EXPORT float f13_F_FSF_PD(float p0, struct S_PD p1, float p2) { return p0; }
EXPORT float f13_F_FSF_PP(float p0, struct S_PP p1, float p2) { return p0; }
EXPORT float f13_F_FSF_III(float p0, struct S_III p1, float p2) { return p0; }
EXPORT float f13_F_FSF_IIF(float p0, struct S_IIF p1, float p2) { return p0; }
EXPORT float f13_F_FSF_IID(float p0, struct S_IID p1, float p2) { return p0; }
EXPORT float f13_F_FSF_IIP(float p0, struct S_IIP p1, float p2) { return p0; }
EXPORT float f13_F_FSF_IFI(float p0, struct S_IFI p1, float p2) { return p0; }
EXPORT float f13_F_FSF_IFF(float p0, struct S_IFF p1, float p2) { return p0; }
EXPORT float f13_F_FSF_IFD(float p0, struct S_IFD p1, float p2) { return p0; }
EXPORT float f13_F_FSF_IFP(float p0, struct S_IFP p1, float p2) { return p0; }
EXPORT float f13_F_FSF_IDI(float p0, struct S_IDI p1, float p2) { return p0; }
EXPORT float f13_F_FSF_IDF(float p0, struct S_IDF p1, float p2) { return p0; }
EXPORT float f13_F_FSF_IDD(float p0, struct S_IDD p1, float p2) { return p0; }
EXPORT float f13_F_FSF_IDP(float p0, struct S_IDP p1, float p2) { return p0; }
EXPORT float f13_F_FSF_IPI(float p0, struct S_IPI p1, float p2) { return p0; }
EXPORT float f13_F_FSF_IPF(float p0, struct S_IPF p1, float p2) { return p0; }
EXPORT float f13_F_FSF_IPD(float p0, struct S_IPD p1, float p2) { return p0; }
EXPORT float f13_F_FSF_IPP(float p0, struct S_IPP p1, float p2) { return p0; }
EXPORT float f13_F_FSF_FII(float p0, struct S_FII p1, float p2) { return p0; }
EXPORT float f13_F_FSF_FIF(float p0, struct S_FIF p1, float p2) { return p0; }
EXPORT float f13_F_FSF_FID(float p0, struct S_FID p1, float p2) { return p0; }
EXPORT float f13_F_FSF_FIP(float p0, struct S_FIP p1, float p2) { return p0; }
EXPORT float f13_F_FSF_FFI(float p0, struct S_FFI p1, float p2) { return p0; }
EXPORT float f13_F_FSF_FFF(float p0, struct S_FFF p1, float p2) { return p0; }
EXPORT float f13_F_FSF_FFD(float p0, struct S_FFD p1, float p2) { return p0; }
EXPORT float f13_F_FSF_FFP(float p0, struct S_FFP p1, float p2) { return p0; }
EXPORT float f13_F_FSF_FDI(float p0, struct S_FDI p1, float p2) { return p0; }
EXPORT float f13_F_FSF_FDF(float p0, struct S_FDF p1, float p2) { return p0; }
EXPORT float f13_F_FSF_FDD(float p0, struct S_FDD p1, float p2) { return p0; }
EXPORT float f13_F_FSF_FDP(float p0, struct S_FDP p1, float p2) { return p0; }
EXPORT float f13_F_FSF_FPI(float p0, struct S_FPI p1, float p2) { return p0; }
EXPORT float f13_F_FSF_FPF(float p0, struct S_FPF p1, float p2) { return p0; }
EXPORT float f13_F_FSF_FPD(float p0, struct S_FPD p1, float p2) { return p0; }
EXPORT float f13_F_FSF_FPP(float p0, struct S_FPP p1, float p2) { return p0; }
EXPORT float f13_F_FSF_DII(float p0, struct S_DII p1, float p2) { return p0; }
EXPORT float f13_F_FSF_DIF(float p0, struct S_DIF p1, float p2) { return p0; }
EXPORT float f13_F_FSF_DID(float p0, struct S_DID p1, float p2) { return p0; }
EXPORT float f13_F_FSF_DIP(float p0, struct S_DIP p1, float p2) { return p0; }
EXPORT float f13_F_FSF_DFI(float p0, struct S_DFI p1, float p2) { return p0; }
EXPORT float f13_F_FSF_DFF(float p0, struct S_DFF p1, float p2) { return p0; }
EXPORT float f13_F_FSF_DFD(float p0, struct S_DFD p1, float p2) { return p0; }
EXPORT float f13_F_FSF_DFP(float p0, struct S_DFP p1, float p2) { return p0; }
EXPORT float f13_F_FSF_DDI(float p0, struct S_DDI p1, float p2) { return p0; }
EXPORT float f13_F_FSF_DDF(float p0, struct S_DDF p1, float p2) { return p0; }
EXPORT float f13_F_FSF_DDD(float p0, struct S_DDD p1, float p2) { return p0; }
EXPORT float f13_F_FSF_DDP(float p0, struct S_DDP p1, float p2) { return p0; }
EXPORT float f13_F_FSF_DPI(float p0, struct S_DPI p1, float p2) { return p0; }
EXPORT float f13_F_FSF_DPF(float p0, struct S_DPF p1, float p2) { return p0; }
EXPORT float f13_F_FSF_DPD(float p0, struct S_DPD p1, float p2) { return p0; }
EXPORT float f13_F_FSF_DPP(float p0, struct S_DPP p1, float p2) { return p0; }
EXPORT float f13_F_FSF_PII(float p0, struct S_PII p1, float p2) { return p0; }
EXPORT float f13_F_FSF_PIF(float p0, struct S_PIF p1, float p2) { return p0; }
EXPORT float f13_F_FSF_PID(float p0, struct S_PID p1, float p2) { return p0; }
EXPORT float f13_F_FSF_PIP(float p0, struct S_PIP p1, float p2) { return p0; }
EXPORT float f13_F_FSF_PFI(float p0, struct S_PFI p1, float p2) { return p0; }
EXPORT float f13_F_FSF_PFF(float p0, struct S_PFF p1, float p2) { return p0; }
EXPORT float f13_F_FSF_PFD(float p0, struct S_PFD p1, float p2) { return p0; }
EXPORT float f13_F_FSF_PFP(float p0, struct S_PFP p1, float p2) { return p0; }
EXPORT float f13_F_FSF_PDI(float p0, struct S_PDI p1, float p2) { return p0; }
EXPORT float f13_F_FSF_PDF(float p0, struct S_PDF p1, float p2) { return p0; }
EXPORT float f13_F_FSF_PDD(float p0, struct S_PDD p1, float p2) { return p0; }
EXPORT float f13_F_FSF_PDP(float p0, struct S_PDP p1, float p2) { return p0; }
EXPORT float f13_F_FSF_PPI(float p0, struct S_PPI p1, float p2) { return p0; }
EXPORT float f13_F_FSF_PPF(float p0, struct S_PPF p1, float p2) { return p0; }
EXPORT float f13_F_FSF_PPD(float p0, struct S_PPD p1, float p2) { return p0; }
EXPORT float f13_F_FSF_PPP(float p0, struct S_PPP p1, float p2) { return p0; }
EXPORT float f13_F_FSD_I(float p0, struct S_I p1, double p2) { return p0; }
EXPORT float f13_F_FSD_F(float p0, struct S_F p1, double p2) { return p0; }
EXPORT float f13_F_FSD_D(float p0, struct S_D p1, double p2) { return p0; }
EXPORT float f13_F_FSD_P(float p0, struct S_P p1, double p2) { return p0; }
EXPORT float f13_F_FSD_II(float p0, struct S_II p1, double p2) { return p0; }
EXPORT float f13_F_FSD_IF(float p0, struct S_IF p1, double p2) { return p0; }
EXPORT float f13_F_FSD_ID(float p0, struct S_ID p1, double p2) { return p0; }
EXPORT float f13_F_FSD_IP(float p0, struct S_IP p1, double p2) { return p0; }
EXPORT float f13_F_FSD_FI(float p0, struct S_FI p1, double p2) { return p0; }
EXPORT float f13_F_FSD_FF(float p0, struct S_FF p1, double p2) { return p0; }
EXPORT float f13_F_FSD_FD(float p0, struct S_FD p1, double p2) { return p0; }
EXPORT float f13_F_FSD_FP(float p0, struct S_FP p1, double p2) { return p0; }
EXPORT float f13_F_FSD_DI(float p0, struct S_DI p1, double p2) { return p0; }
EXPORT float f13_F_FSD_DF(float p0, struct S_DF p1, double p2) { return p0; }
EXPORT float f13_F_FSD_DD(float p0, struct S_DD p1, double p2) { return p0; }
EXPORT float f13_F_FSD_DP(float p0, struct S_DP p1, double p2) { return p0; }
EXPORT float f13_F_FSD_PI(float p0, struct S_PI p1, double p2) { return p0; }
EXPORT float f13_F_FSD_PF(float p0, struct S_PF p1, double p2) { return p0; }
EXPORT float f13_F_FSD_PD(float p0, struct S_PD p1, double p2) { return p0; }
EXPORT float f13_F_FSD_PP(float p0, struct S_PP p1, double p2) { return p0; }
EXPORT float f13_F_FSD_III(float p0, struct S_III p1, double p2) { return p0; }
EXPORT float f13_F_FSD_IIF(float p0, struct S_IIF p1, double p2) { return p0; }
EXPORT float f13_F_FSD_IID(float p0, struct S_IID p1, double p2) { return p0; }
EXPORT float f13_F_FSD_IIP(float p0, struct S_IIP p1, double p2) { return p0; }
EXPORT float f13_F_FSD_IFI(float p0, struct S_IFI p1, double p2) { return p0; }
EXPORT float f13_F_FSD_IFF(float p0, struct S_IFF p1, double p2) { return p0; }
EXPORT float f13_F_FSD_IFD(float p0, struct S_IFD p1, double p2) { return p0; }
EXPORT float f13_F_FSD_IFP(float p0, struct S_IFP p1, double p2) { return p0; }
EXPORT float f13_F_FSD_IDI(float p0, struct S_IDI p1, double p2) { return p0; }
EXPORT float f13_F_FSD_IDF(float p0, struct S_IDF p1, double p2) { return p0; }
EXPORT float f13_F_FSD_IDD(float p0, struct S_IDD p1, double p2) { return p0; }
EXPORT float f13_F_FSD_IDP(float p0, struct S_IDP p1, double p2) { return p0; }
EXPORT float f13_F_FSD_IPI(float p0, struct S_IPI p1, double p2) { return p0; }
EXPORT float f13_F_FSD_IPF(float p0, struct S_IPF p1, double p2) { return p0; }
EXPORT float f13_F_FSD_IPD(float p0, struct S_IPD p1, double p2) { return p0; }
EXPORT float f13_F_FSD_IPP(float p0, struct S_IPP p1, double p2) { return p0; }
EXPORT float f13_F_FSD_FII(float p0, struct S_FII p1, double p2) { return p0; }
EXPORT float f13_F_FSD_FIF(float p0, struct S_FIF p1, double p2) { return p0; }
EXPORT float f13_F_FSD_FID(float p0, struct S_FID p1, double p2) { return p0; }
EXPORT float f13_F_FSD_FIP(float p0, struct S_FIP p1, double p2) { return p0; }
EXPORT float f13_F_FSD_FFI(float p0, struct S_FFI p1, double p2) { return p0; }
EXPORT float f13_F_FSD_FFF(float p0, struct S_FFF p1, double p2) { return p0; }
EXPORT float f13_F_FSD_FFD(float p0, struct S_FFD p1, double p2) { return p0; }
EXPORT float f13_F_FSD_FFP(float p0, struct S_FFP p1, double p2) { return p0; }
EXPORT float f13_F_FSD_FDI(float p0, struct S_FDI p1, double p2) { return p0; }
EXPORT float f13_F_FSD_FDF(float p0, struct S_FDF p1, double p2) { return p0; }
EXPORT float f13_F_FSD_FDD(float p0, struct S_FDD p1, double p2) { return p0; }
EXPORT float f13_F_FSD_FDP(float p0, struct S_FDP p1, double p2) { return p0; }
EXPORT float f13_F_FSD_FPI(float p0, struct S_FPI p1, double p2) { return p0; }
EXPORT float f13_F_FSD_FPF(float p0, struct S_FPF p1, double p2) { return p0; }
EXPORT float f13_F_FSD_FPD(float p0, struct S_FPD p1, double p2) { return p0; }
EXPORT float f13_F_FSD_FPP(float p0, struct S_FPP p1, double p2) { return p0; }
EXPORT float f13_F_FSD_DII(float p0, struct S_DII p1, double p2) { return p0; }
EXPORT float f13_F_FSD_DIF(float p0, struct S_DIF p1, double p2) { return p0; }
EXPORT float f13_F_FSD_DID(float p0, struct S_DID p1, double p2) { return p0; }
EXPORT float f13_F_FSD_DIP(float p0, struct S_DIP p1, double p2) { return p0; }
EXPORT float f13_F_FSD_DFI(float p0, struct S_DFI p1, double p2) { return p0; }
EXPORT float f13_F_FSD_DFF(float p0, struct S_DFF p1, double p2) { return p0; }
EXPORT float f13_F_FSD_DFD(float p0, struct S_DFD p1, double p2) { return p0; }
EXPORT float f13_F_FSD_DFP(float p0, struct S_DFP p1, double p2) { return p0; }
EXPORT float f13_F_FSD_DDI(float p0, struct S_DDI p1, double p2) { return p0; }
EXPORT float f13_F_FSD_DDF(float p0, struct S_DDF p1, double p2) { return p0; }
EXPORT float f13_F_FSD_DDD(float p0, struct S_DDD p1, double p2) { return p0; }
EXPORT float f13_F_FSD_DDP(float p0, struct S_DDP p1, double p2) { return p0; }
EXPORT float f13_F_FSD_DPI(float p0, struct S_DPI p1, double p2) { return p0; }
EXPORT float f13_F_FSD_DPF(float p0, struct S_DPF p1, double p2) { return p0; }
EXPORT float f13_F_FSD_DPD(float p0, struct S_DPD p1, double p2) { return p0; }
EXPORT float f13_F_FSD_DPP(float p0, struct S_DPP p1, double p2) { return p0; }
EXPORT float f13_F_FSD_PII(float p0, struct S_PII p1, double p2) { return p0; }
EXPORT float f13_F_FSD_PIF(float p0, struct S_PIF p1, double p2) { return p0; }
EXPORT float f13_F_FSD_PID(float p0, struct S_PID p1, double p2) { return p0; }
EXPORT float f13_F_FSD_PIP(float p0, struct S_PIP p1, double p2) { return p0; }
EXPORT float f13_F_FSD_PFI(float p0, struct S_PFI p1, double p2) { return p0; }
EXPORT float f13_F_FSD_PFF(float p0, struct S_PFF p1, double p2) { return p0; }
EXPORT float f13_F_FSD_PFD(float p0, struct S_PFD p1, double p2) { return p0; }
EXPORT float f13_F_FSD_PFP(float p0, struct S_PFP p1, double p2) { return p0; }
EXPORT float f13_F_FSD_PDI(float p0, struct S_PDI p1, double p2) { return p0; }
EXPORT float f13_F_FSD_PDF(float p0, struct S_PDF p1, double p2) { return p0; }
EXPORT float f13_F_FSD_PDD(float p0, struct S_PDD p1, double p2) { return p0; }
EXPORT float f13_F_FSD_PDP(float p0, struct S_PDP p1, double p2) { return p0; }
EXPORT float f13_F_FSD_PPI(float p0, struct S_PPI p1, double p2) { return p0; }
EXPORT float f13_F_FSD_PPF(float p0, struct S_PPF p1, double p2) { return p0; }
EXPORT float f13_F_FSD_PPD(float p0, struct S_PPD p1, double p2) { return p0; }
EXPORT float f13_F_FSD_PPP(float p0, struct S_PPP p1, double p2) { return p0; }
EXPORT float f13_F_FSP_I(float p0, struct S_I p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_F(float p0, struct S_F p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_D(float p0, struct S_D p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_P(float p0, struct S_P p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_II(float p0, struct S_II p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_IF(float p0, struct S_IF p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_ID(float p0, struct S_ID p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_IP(float p0, struct S_IP p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_FI(float p0, struct S_FI p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_FF(float p0, struct S_FF p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_FD(float p0, struct S_FD p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_FP(float p0, struct S_FP p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_DI(float p0, struct S_DI p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_DF(float p0, struct S_DF p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_DD(float p0, struct S_DD p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_DP(float p0, struct S_DP p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_PI(float p0, struct S_PI p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_PF(float p0, struct S_PF p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_PD(float p0, struct S_PD p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_PP(float p0, struct S_PP p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_III(float p0, struct S_III p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_IIF(float p0, struct S_IIF p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_IID(float p0, struct S_IID p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_IIP(float p0, struct S_IIP p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_IFI(float p0, struct S_IFI p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_IFF(float p0, struct S_IFF p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_IFD(float p0, struct S_IFD p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_IFP(float p0, struct S_IFP p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_IDI(float p0, struct S_IDI p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_IDF(float p0, struct S_IDF p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_IDD(float p0, struct S_IDD p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_IDP(float p0, struct S_IDP p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_IPI(float p0, struct S_IPI p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_IPF(float p0, struct S_IPF p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_IPD(float p0, struct S_IPD p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_IPP(float p0, struct S_IPP p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_FII(float p0, struct S_FII p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_FIF(float p0, struct S_FIF p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_FID(float p0, struct S_FID p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_FIP(float p0, struct S_FIP p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_FFI(float p0, struct S_FFI p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_FFF(float p0, struct S_FFF p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_FFD(float p0, struct S_FFD p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_FFP(float p0, struct S_FFP p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_FDI(float p0, struct S_FDI p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_FDF(float p0, struct S_FDF p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_FDD(float p0, struct S_FDD p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_FDP(float p0, struct S_FDP p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_FPI(float p0, struct S_FPI p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_FPF(float p0, struct S_FPF p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_FPD(float p0, struct S_FPD p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_FPP(float p0, struct S_FPP p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_DII(float p0, struct S_DII p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_DIF(float p0, struct S_DIF p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_DID(float p0, struct S_DID p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_DIP(float p0, struct S_DIP p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_DFI(float p0, struct S_DFI p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_DFF(float p0, struct S_DFF p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_DFD(float p0, struct S_DFD p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_DFP(float p0, struct S_DFP p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_DDI(float p0, struct S_DDI p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_DDF(float p0, struct S_DDF p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_DDD(float p0, struct S_DDD p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_DDP(float p0, struct S_DDP p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_DPI(float p0, struct S_DPI p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_DPF(float p0, struct S_DPF p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_DPD(float p0, struct S_DPD p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_DPP(float p0, struct S_DPP p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_PII(float p0, struct S_PII p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_PIF(float p0, struct S_PIF p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_PID(float p0, struct S_PID p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_PIP(float p0, struct S_PIP p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_PFI(float p0, struct S_PFI p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_PFF(float p0, struct S_PFF p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_PFD(float p0, struct S_PFD p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_PFP(float p0, struct S_PFP p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_PDI(float p0, struct S_PDI p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_PDF(float p0, struct S_PDF p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_PDD(float p0, struct S_PDD p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_PDP(float p0, struct S_PDP p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_PPI(float p0, struct S_PPI p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_PPF(float p0, struct S_PPF p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_PPD(float p0, struct S_PPD p1, void* p2) { return p0; }
EXPORT float f13_F_FSP_PPP(float p0, struct S_PPP p1, void* p2) { return p0; }
EXPORT float f13_F_FSS_I(float p0, struct S_I p1, struct S_I p2) { return p0; }
EXPORT float f13_F_FSS_F(float p0, struct S_F p1, struct S_F p2) { return p0; }
EXPORT float f13_F_FSS_D(float p0, struct S_D p1, struct S_D p2) { return p0; }
EXPORT float f13_F_FSS_P(float p0, struct S_P p1, struct S_P p2) { return p0; }
EXPORT float f13_F_FSS_II(float p0, struct S_II p1, struct S_II p2) { return p0; }
EXPORT float f13_F_FSS_IF(float p0, struct S_IF p1, struct S_IF p2) { return p0; }
EXPORT float f13_F_FSS_ID(float p0, struct S_ID p1, struct S_ID p2) { return p0; }
EXPORT float f13_F_FSS_IP(float p0, struct S_IP p1, struct S_IP p2) { return p0; }
EXPORT float f13_F_FSS_FI(float p0, struct S_FI p1, struct S_FI p2) { return p0; }
EXPORT float f13_F_FSS_FF(float p0, struct S_FF p1, struct S_FF p2) { return p0; }
EXPORT float f13_F_FSS_FD(float p0, struct S_FD p1, struct S_FD p2) { return p0; }
EXPORT float f13_F_FSS_FP(float p0, struct S_FP p1, struct S_FP p2) { return p0; }
EXPORT float f13_F_FSS_DI(float p0, struct S_DI p1, struct S_DI p2) { return p0; }
EXPORT float f13_F_FSS_DF(float p0, struct S_DF p1, struct S_DF p2) { return p0; }
EXPORT float f13_F_FSS_DD(float p0, struct S_DD p1, struct S_DD p2) { return p0; }
EXPORT float f13_F_FSS_DP(float p0, struct S_DP p1, struct S_DP p2) { return p0; }
EXPORT float f13_F_FSS_PI(float p0, struct S_PI p1, struct S_PI p2) { return p0; }
EXPORT float f13_F_FSS_PF(float p0, struct S_PF p1, struct S_PF p2) { return p0; }
EXPORT float f13_F_FSS_PD(float p0, struct S_PD p1, struct S_PD p2) { return p0; }
EXPORT float f13_F_FSS_PP(float p0, struct S_PP p1, struct S_PP p2) { return p0; }
EXPORT float f13_F_FSS_III(float p0, struct S_III p1, struct S_III p2) { return p0; }
EXPORT float f13_F_FSS_IIF(float p0, struct S_IIF p1, struct S_IIF p2) { return p0; }
EXPORT float f13_F_FSS_IID(float p0, struct S_IID p1, struct S_IID p2) { return p0; }
EXPORT float f13_F_FSS_IIP(float p0, struct S_IIP p1, struct S_IIP p2) { return p0; }
EXPORT float f13_F_FSS_IFI(float p0, struct S_IFI p1, struct S_IFI p2) { return p0; }
EXPORT float f13_F_FSS_IFF(float p0, struct S_IFF p1, struct S_IFF p2) { return p0; }
EXPORT float f13_F_FSS_IFD(float p0, struct S_IFD p1, struct S_IFD p2) { return p0; }
EXPORT float f13_F_FSS_IFP(float p0, struct S_IFP p1, struct S_IFP p2) { return p0; }
EXPORT float f13_F_FSS_IDI(float p0, struct S_IDI p1, struct S_IDI p2) { return p0; }
EXPORT float f13_F_FSS_IDF(float p0, struct S_IDF p1, struct S_IDF p2) { return p0; }
EXPORT float f13_F_FSS_IDD(float p0, struct S_IDD p1, struct S_IDD p2) { return p0; }
EXPORT float f14_F_FSS_IDP(float p0, struct S_IDP p1, struct S_IDP p2) { return p0; }
EXPORT float f14_F_FSS_IPI(float p0, struct S_IPI p1, struct S_IPI p2) { return p0; }
EXPORT float f14_F_FSS_IPF(float p0, struct S_IPF p1, struct S_IPF p2) { return p0; }
EXPORT float f14_F_FSS_IPD(float p0, struct S_IPD p1, struct S_IPD p2) { return p0; }
EXPORT float f14_F_FSS_IPP(float p0, struct S_IPP p1, struct S_IPP p2) { return p0; }
EXPORT float f14_F_FSS_FII(float p0, struct S_FII p1, struct S_FII p2) { return p0; }
EXPORT float f14_F_FSS_FIF(float p0, struct S_FIF p1, struct S_FIF p2) { return p0; }
EXPORT float f14_F_FSS_FID(float p0, struct S_FID p1, struct S_FID p2) { return p0; }
EXPORT float f14_F_FSS_FIP(float p0, struct S_FIP p1, struct S_FIP p2) { return p0; }
EXPORT float f14_F_FSS_FFI(float p0, struct S_FFI p1, struct S_FFI p2) { return p0; }
EXPORT float f14_F_FSS_FFF(float p0, struct S_FFF p1, struct S_FFF p2) { return p0; }
EXPORT float f14_F_FSS_FFD(float p0, struct S_FFD p1, struct S_FFD p2) { return p0; }
EXPORT float f14_F_FSS_FFP(float p0, struct S_FFP p1, struct S_FFP p2) { return p0; }
EXPORT float f14_F_FSS_FDI(float p0, struct S_FDI p1, struct S_FDI p2) { return p0; }
EXPORT float f14_F_FSS_FDF(float p0, struct S_FDF p1, struct S_FDF p2) { return p0; }
EXPORT float f14_F_FSS_FDD(float p0, struct S_FDD p1, struct S_FDD p2) { return p0; }
EXPORT float f14_F_FSS_FDP(float p0, struct S_FDP p1, struct S_FDP p2) { return p0; }
EXPORT float f14_F_FSS_FPI(float p0, struct S_FPI p1, struct S_FPI p2) { return p0; }
EXPORT float f14_F_FSS_FPF(float p0, struct S_FPF p1, struct S_FPF p2) { return p0; }
EXPORT float f14_F_FSS_FPD(float p0, struct S_FPD p1, struct S_FPD p2) { return p0; }
EXPORT float f14_F_FSS_FPP(float p0, struct S_FPP p1, struct S_FPP p2) { return p0; }
EXPORT float f14_F_FSS_DII(float p0, struct S_DII p1, struct S_DII p2) { return p0; }
EXPORT float f14_F_FSS_DIF(float p0, struct S_DIF p1, struct S_DIF p2) { return p0; }
EXPORT float f14_F_FSS_DID(float p0, struct S_DID p1, struct S_DID p2) { return p0; }
EXPORT float f14_F_FSS_DIP(float p0, struct S_DIP p1, struct S_DIP p2) { return p0; }
EXPORT float f14_F_FSS_DFI(float p0, struct S_DFI p1, struct S_DFI p2) { return p0; }
EXPORT float f14_F_FSS_DFF(float p0, struct S_DFF p1, struct S_DFF p2) { return p0; }
EXPORT float f14_F_FSS_DFD(float p0, struct S_DFD p1, struct S_DFD p2) { return p0; }
EXPORT float f14_F_FSS_DFP(float p0, struct S_DFP p1, struct S_DFP p2) { return p0; }
EXPORT float f14_F_FSS_DDI(float p0, struct S_DDI p1, struct S_DDI p2) { return p0; }
EXPORT float f14_F_FSS_DDF(float p0, struct S_DDF p1, struct S_DDF p2) { return p0; }
EXPORT float f14_F_FSS_DDD(float p0, struct S_DDD p1, struct S_DDD p2) { return p0; }
EXPORT float f14_F_FSS_DDP(float p0, struct S_DDP p1, struct S_DDP p2) { return p0; }
EXPORT float f14_F_FSS_DPI(float p0, struct S_DPI p1, struct S_DPI p2) { return p0; }
EXPORT float f14_F_FSS_DPF(float p0, struct S_DPF p1, struct S_DPF p2) { return p0; }
EXPORT float f14_F_FSS_DPD(float p0, struct S_DPD p1, struct S_DPD p2) { return p0; }
EXPORT float f14_F_FSS_DPP(float p0, struct S_DPP p1, struct S_DPP p2) { return p0; }
EXPORT float f14_F_FSS_PII(float p0, struct S_PII p1, struct S_PII p2) { return p0; }
EXPORT float f14_F_FSS_PIF(float p0, struct S_PIF p1, struct S_PIF p2) { return p0; }
EXPORT float f14_F_FSS_PID(float p0, struct S_PID p1, struct S_PID p2) { return p0; }
EXPORT float f14_F_FSS_PIP(float p0, struct S_PIP p1, struct S_PIP p2) { return p0; }
EXPORT float f14_F_FSS_PFI(float p0, struct S_PFI p1, struct S_PFI p2) { return p0; }
EXPORT float f14_F_FSS_PFF(float p0, struct S_PFF p1, struct S_PFF p2) { return p0; }
EXPORT float f14_F_FSS_PFD(float p0, struct S_PFD p1, struct S_PFD p2) { return p0; }
EXPORT float f14_F_FSS_PFP(float p0, struct S_PFP p1, struct S_PFP p2) { return p0; }
EXPORT float f14_F_FSS_PDI(float p0, struct S_PDI p1, struct S_PDI p2) { return p0; }
EXPORT float f14_F_FSS_PDF(float p0, struct S_PDF p1, struct S_PDF p2) { return p0; }
EXPORT float f14_F_FSS_PDD(float p0, struct S_PDD p1, struct S_PDD p2) { return p0; }
EXPORT float f14_F_FSS_PDP(float p0, struct S_PDP p1, struct S_PDP p2) { return p0; }
EXPORT float f14_F_FSS_PPI(float p0, struct S_PPI p1, struct S_PPI p2) { return p0; }
EXPORT float f14_F_FSS_PPF(float p0, struct S_PPF p1, struct S_PPF p2) { return p0; }
EXPORT float f14_F_FSS_PPD(float p0, struct S_PPD p1, struct S_PPD p2) { return p0; }
EXPORT float f14_F_FSS_PPP(float p0, struct S_PPP p1, struct S_PPP p2) { return p0; }
EXPORT double f14_D_DII_(double p0, int p1, int p2) { return p0; }
EXPORT double f14_D_DIF_(double p0, int p1, float p2) { return p0; }
EXPORT double f14_D_DID_(double p0, int p1, double p2) { return p0; }
EXPORT double f14_D_DIP_(double p0, int p1, void* p2) { return p0; }
EXPORT double f14_D_DIS_I(double p0, int p1, struct S_I p2) { return p0; }
EXPORT double f14_D_DIS_F(double p0, int p1, struct S_F p2) { return p0; }
EXPORT double f14_D_DIS_D(double p0, int p1, struct S_D p2) { return p0; }
EXPORT double f14_D_DIS_P(double p0, int p1, struct S_P p2) { return p0; }
EXPORT double f14_D_DIS_II(double p0, int p1, struct S_II p2) { return p0; }
EXPORT double f14_D_DIS_IF(double p0, int p1, struct S_IF p2) { return p0; }
EXPORT double f14_D_DIS_ID(double p0, int p1, struct S_ID p2) { return p0; }
EXPORT double f14_D_DIS_IP(double p0, int p1, struct S_IP p2) { return p0; }
EXPORT double f14_D_DIS_FI(double p0, int p1, struct S_FI p2) { return p0; }
EXPORT double f14_D_DIS_FF(double p0, int p1, struct S_FF p2) { return p0; }
EXPORT double f14_D_DIS_FD(double p0, int p1, struct S_FD p2) { return p0; }
EXPORT double f14_D_DIS_FP(double p0, int p1, struct S_FP p2) { return p0; }
EXPORT double f14_D_DIS_DI(double p0, int p1, struct S_DI p2) { return p0; }
EXPORT double f14_D_DIS_DF(double p0, int p1, struct S_DF p2) { return p0; }
EXPORT double f14_D_DIS_DD(double p0, int p1, struct S_DD p2) { return p0; }
EXPORT double f14_D_DIS_DP(double p0, int p1, struct S_DP p2) { return p0; }
EXPORT double f14_D_DIS_PI(double p0, int p1, struct S_PI p2) { return p0; }
EXPORT double f14_D_DIS_PF(double p0, int p1, struct S_PF p2) { return p0; }
EXPORT double f14_D_DIS_PD(double p0, int p1, struct S_PD p2) { return p0; }
EXPORT double f14_D_DIS_PP(double p0, int p1, struct S_PP p2) { return p0; }
EXPORT double f14_D_DIS_III(double p0, int p1, struct S_III p2) { return p0; }
EXPORT double f14_D_DIS_IIF(double p0, int p1, struct S_IIF p2) { return p0; }
EXPORT double f14_D_DIS_IID(double p0, int p1, struct S_IID p2) { return p0; }
EXPORT double f14_D_DIS_IIP(double p0, int p1, struct S_IIP p2) { return p0; }
EXPORT double f14_D_DIS_IFI(double p0, int p1, struct S_IFI p2) { return p0; }
EXPORT double f14_D_DIS_IFF(double p0, int p1, struct S_IFF p2) { return p0; }
EXPORT double f14_D_DIS_IFD(double p0, int p1, struct S_IFD p2) { return p0; }
EXPORT double f14_D_DIS_IFP(double p0, int p1, struct S_IFP p2) { return p0; }
EXPORT double f14_D_DIS_IDI(double p0, int p1, struct S_IDI p2) { return p0; }
EXPORT double f14_D_DIS_IDF(double p0, int p1, struct S_IDF p2) { return p0; }
EXPORT double f14_D_DIS_IDD(double p0, int p1, struct S_IDD p2) { return p0; }
EXPORT double f14_D_DIS_IDP(double p0, int p1, struct S_IDP p2) { return p0; }
EXPORT double f14_D_DIS_IPI(double p0, int p1, struct S_IPI p2) { return p0; }
EXPORT double f14_D_DIS_IPF(double p0, int p1, struct S_IPF p2) { return p0; }
EXPORT double f14_D_DIS_IPD(double p0, int p1, struct S_IPD p2) { return p0; }
EXPORT double f14_D_DIS_IPP(double p0, int p1, struct S_IPP p2) { return p0; }
EXPORT double f14_D_DIS_FII(double p0, int p1, struct S_FII p2) { return p0; }
EXPORT double f14_D_DIS_FIF(double p0, int p1, struct S_FIF p2) { return p0; }
EXPORT double f14_D_DIS_FID(double p0, int p1, struct S_FID p2) { return p0; }
EXPORT double f14_D_DIS_FIP(double p0, int p1, struct S_FIP p2) { return p0; }
EXPORT double f14_D_DIS_FFI(double p0, int p1, struct S_FFI p2) { return p0; }
EXPORT double f14_D_DIS_FFF(double p0, int p1, struct S_FFF p2) { return p0; }
EXPORT double f14_D_DIS_FFD(double p0, int p1, struct S_FFD p2) { return p0; }
EXPORT double f14_D_DIS_FFP(double p0, int p1, struct S_FFP p2) { return p0; }
EXPORT double f14_D_DIS_FDI(double p0, int p1, struct S_FDI p2) { return p0; }
EXPORT double f14_D_DIS_FDF(double p0, int p1, struct S_FDF p2) { return p0; }
EXPORT double f14_D_DIS_FDD(double p0, int p1, struct S_FDD p2) { return p0; }
EXPORT double f14_D_DIS_FDP(double p0, int p1, struct S_FDP p2) { return p0; }
EXPORT double f14_D_DIS_FPI(double p0, int p1, struct S_FPI p2) { return p0; }
EXPORT double f14_D_DIS_FPF(double p0, int p1, struct S_FPF p2) { return p0; }
EXPORT double f14_D_DIS_FPD(double p0, int p1, struct S_FPD p2) { return p0; }
EXPORT double f14_D_DIS_FPP(double p0, int p1, struct S_FPP p2) { return p0; }
EXPORT double f14_D_DIS_DII(double p0, int p1, struct S_DII p2) { return p0; }
EXPORT double f14_D_DIS_DIF(double p0, int p1, struct S_DIF p2) { return p0; }
EXPORT double f14_D_DIS_DID(double p0, int p1, struct S_DID p2) { return p0; }
EXPORT double f14_D_DIS_DIP(double p0, int p1, struct S_DIP p2) { return p0; }
EXPORT double f14_D_DIS_DFI(double p0, int p1, struct S_DFI p2) { return p0; }
EXPORT double f14_D_DIS_DFF(double p0, int p1, struct S_DFF p2) { return p0; }
EXPORT double f14_D_DIS_DFD(double p0, int p1, struct S_DFD p2) { return p0; }
EXPORT double f14_D_DIS_DFP(double p0, int p1, struct S_DFP p2) { return p0; }
EXPORT double f14_D_DIS_DDI(double p0, int p1, struct S_DDI p2) { return p0; }
EXPORT double f14_D_DIS_DDF(double p0, int p1, struct S_DDF p2) { return p0; }
EXPORT double f14_D_DIS_DDD(double p0, int p1, struct S_DDD p2) { return p0; }
EXPORT double f14_D_DIS_DDP(double p0, int p1, struct S_DDP p2) { return p0; }
EXPORT double f14_D_DIS_DPI(double p0, int p1, struct S_DPI p2) { return p0; }
EXPORT double f14_D_DIS_DPF(double p0, int p1, struct S_DPF p2) { return p0; }
EXPORT double f14_D_DIS_DPD(double p0, int p1, struct S_DPD p2) { return p0; }
EXPORT double f14_D_DIS_DPP(double p0, int p1, struct S_DPP p2) { return p0; }
EXPORT double f14_D_DIS_PII(double p0, int p1, struct S_PII p2) { return p0; }
EXPORT double f14_D_DIS_PIF(double p0, int p1, struct S_PIF p2) { return p0; }
EXPORT double f14_D_DIS_PID(double p0, int p1, struct S_PID p2) { return p0; }
EXPORT double f14_D_DIS_PIP(double p0, int p1, struct S_PIP p2) { return p0; }
EXPORT double f14_D_DIS_PFI(double p0, int p1, struct S_PFI p2) { return p0; }
EXPORT double f14_D_DIS_PFF(double p0, int p1, struct S_PFF p2) { return p0; }
EXPORT double f14_D_DIS_PFD(double p0, int p1, struct S_PFD p2) { return p0; }
EXPORT double f14_D_DIS_PFP(double p0, int p1, struct S_PFP p2) { return p0; }
EXPORT double f14_D_DIS_PDI(double p0, int p1, struct S_PDI p2) { return p0; }
EXPORT double f14_D_DIS_PDF(double p0, int p1, struct S_PDF p2) { return p0; }
EXPORT double f14_D_DIS_PDD(double p0, int p1, struct S_PDD p2) { return p0; }
EXPORT double f14_D_DIS_PDP(double p0, int p1, struct S_PDP p2) { return p0; }
EXPORT double f14_D_DIS_PPI(double p0, int p1, struct S_PPI p2) { return p0; }
EXPORT double f14_D_DIS_PPF(double p0, int p1, struct S_PPF p2) { return p0; }
EXPORT double f14_D_DIS_PPD(double p0, int p1, struct S_PPD p2) { return p0; }
EXPORT double f14_D_DIS_PPP(double p0, int p1, struct S_PPP p2) { return p0; }
EXPORT double f14_D_DFI_(double p0, float p1, int p2) { return p0; }
EXPORT double f14_D_DFF_(double p0, float p1, float p2) { return p0; }
EXPORT double f14_D_DFD_(double p0, float p1, double p2) { return p0; }
EXPORT double f14_D_DFP_(double p0, float p1, void* p2) { return p0; }
EXPORT double f14_D_DFS_I(double p0, float p1, struct S_I p2) { return p0; }
EXPORT double f14_D_DFS_F(double p0, float p1, struct S_F p2) { return p0; }
EXPORT double f14_D_DFS_D(double p0, float p1, struct S_D p2) { return p0; }
EXPORT double f14_D_DFS_P(double p0, float p1, struct S_P p2) { return p0; }
EXPORT double f14_D_DFS_II(double p0, float p1, struct S_II p2) { return p0; }
EXPORT double f14_D_DFS_IF(double p0, float p1, struct S_IF p2) { return p0; }
EXPORT double f14_D_DFS_ID(double p0, float p1, struct S_ID p2) { return p0; }
EXPORT double f14_D_DFS_IP(double p0, float p1, struct S_IP p2) { return p0; }
EXPORT double f14_D_DFS_FI(double p0, float p1, struct S_FI p2) { return p0; }
EXPORT double f14_D_DFS_FF(double p0, float p1, struct S_FF p2) { return p0; }
EXPORT double f14_D_DFS_FD(double p0, float p1, struct S_FD p2) { return p0; }
EXPORT double f14_D_DFS_FP(double p0, float p1, struct S_FP p2) { return p0; }
EXPORT double f14_D_DFS_DI(double p0, float p1, struct S_DI p2) { return p0; }
EXPORT double f14_D_DFS_DF(double p0, float p1, struct S_DF p2) { return p0; }
EXPORT double f14_D_DFS_DD(double p0, float p1, struct S_DD p2) { return p0; }
EXPORT double f14_D_DFS_DP(double p0, float p1, struct S_DP p2) { return p0; }
EXPORT double f14_D_DFS_PI(double p0, float p1, struct S_PI p2) { return p0; }
EXPORT double f14_D_DFS_PF(double p0, float p1, struct S_PF p2) { return p0; }
EXPORT double f14_D_DFS_PD(double p0, float p1, struct S_PD p2) { return p0; }
EXPORT double f14_D_DFS_PP(double p0, float p1, struct S_PP p2) { return p0; }
EXPORT double f14_D_DFS_III(double p0, float p1, struct S_III p2) { return p0; }
EXPORT double f14_D_DFS_IIF(double p0, float p1, struct S_IIF p2) { return p0; }
EXPORT double f14_D_DFS_IID(double p0, float p1, struct S_IID p2) { return p0; }
EXPORT double f14_D_DFS_IIP(double p0, float p1, struct S_IIP p2) { return p0; }
EXPORT double f14_D_DFS_IFI(double p0, float p1, struct S_IFI p2) { return p0; }
EXPORT double f14_D_DFS_IFF(double p0, float p1, struct S_IFF p2) { return p0; }
EXPORT double f14_D_DFS_IFD(double p0, float p1, struct S_IFD p2) { return p0; }
EXPORT double f14_D_DFS_IFP(double p0, float p1, struct S_IFP p2) { return p0; }
EXPORT double f14_D_DFS_IDI(double p0, float p1, struct S_IDI p2) { return p0; }
EXPORT double f14_D_DFS_IDF(double p0, float p1, struct S_IDF p2) { return p0; }
EXPORT double f14_D_DFS_IDD(double p0, float p1, struct S_IDD p2) { return p0; }
EXPORT double f14_D_DFS_IDP(double p0, float p1, struct S_IDP p2) { return p0; }
EXPORT double f14_D_DFS_IPI(double p0, float p1, struct S_IPI p2) { return p0; }
EXPORT double f14_D_DFS_IPF(double p0, float p1, struct S_IPF p2) { return p0; }
EXPORT double f14_D_DFS_IPD(double p0, float p1, struct S_IPD p2) { return p0; }
EXPORT double f14_D_DFS_IPP(double p0, float p1, struct S_IPP p2) { return p0; }
EXPORT double f14_D_DFS_FII(double p0, float p1, struct S_FII p2) { return p0; }
EXPORT double f14_D_DFS_FIF(double p0, float p1, struct S_FIF p2) { return p0; }
EXPORT double f14_D_DFS_FID(double p0, float p1, struct S_FID p2) { return p0; }
EXPORT double f14_D_DFS_FIP(double p0, float p1, struct S_FIP p2) { return p0; }
EXPORT double f14_D_DFS_FFI(double p0, float p1, struct S_FFI p2) { return p0; }
EXPORT double f14_D_DFS_FFF(double p0, float p1, struct S_FFF p2) { return p0; }
EXPORT double f14_D_DFS_FFD(double p0, float p1, struct S_FFD p2) { return p0; }
EXPORT double f14_D_DFS_FFP(double p0, float p1, struct S_FFP p2) { return p0; }
EXPORT double f14_D_DFS_FDI(double p0, float p1, struct S_FDI p2) { return p0; }
EXPORT double f14_D_DFS_FDF(double p0, float p1, struct S_FDF p2) { return p0; }
EXPORT double f14_D_DFS_FDD(double p0, float p1, struct S_FDD p2) { return p0; }
EXPORT double f14_D_DFS_FDP(double p0, float p1, struct S_FDP p2) { return p0; }
EXPORT double f14_D_DFS_FPI(double p0, float p1, struct S_FPI p2) { return p0; }
EXPORT double f14_D_DFS_FPF(double p0, float p1, struct S_FPF p2) { return p0; }
EXPORT double f14_D_DFS_FPD(double p0, float p1, struct S_FPD p2) { return p0; }
EXPORT double f14_D_DFS_FPP(double p0, float p1, struct S_FPP p2) { return p0; }
EXPORT double f14_D_DFS_DII(double p0, float p1, struct S_DII p2) { return p0; }
EXPORT double f14_D_DFS_DIF(double p0, float p1, struct S_DIF p2) { return p0; }
EXPORT double f14_D_DFS_DID(double p0, float p1, struct S_DID p2) { return p0; }
EXPORT double f14_D_DFS_DIP(double p0, float p1, struct S_DIP p2) { return p0; }
EXPORT double f14_D_DFS_DFI(double p0, float p1, struct S_DFI p2) { return p0; }
EXPORT double f14_D_DFS_DFF(double p0, float p1, struct S_DFF p2) { return p0; }
EXPORT double f14_D_DFS_DFD(double p0, float p1, struct S_DFD p2) { return p0; }
EXPORT double f14_D_DFS_DFP(double p0, float p1, struct S_DFP p2) { return p0; }
EXPORT double f14_D_DFS_DDI(double p0, float p1, struct S_DDI p2) { return p0; }
EXPORT double f14_D_DFS_DDF(double p0, float p1, struct S_DDF p2) { return p0; }
EXPORT double f14_D_DFS_DDD(double p0, float p1, struct S_DDD p2) { return p0; }
EXPORT double f14_D_DFS_DDP(double p0, float p1, struct S_DDP p2) { return p0; }
EXPORT double f14_D_DFS_DPI(double p0, float p1, struct S_DPI p2) { return p0; }
EXPORT double f14_D_DFS_DPF(double p0, float p1, struct S_DPF p2) { return p0; }
EXPORT double f14_D_DFS_DPD(double p0, float p1, struct S_DPD p2) { return p0; }
EXPORT double f14_D_DFS_DPP(double p0, float p1, struct S_DPP p2) { return p0; }
EXPORT double f14_D_DFS_PII(double p0, float p1, struct S_PII p2) { return p0; }
EXPORT double f14_D_DFS_PIF(double p0, float p1, struct S_PIF p2) { return p0; }
EXPORT double f14_D_DFS_PID(double p0, float p1, struct S_PID p2) { return p0; }
EXPORT double f14_D_DFS_PIP(double p0, float p1, struct S_PIP p2) { return p0; }
EXPORT double f14_D_DFS_PFI(double p0, float p1, struct S_PFI p2) { return p0; }
EXPORT double f14_D_DFS_PFF(double p0, float p1, struct S_PFF p2) { return p0; }
EXPORT double f14_D_DFS_PFD(double p0, float p1, struct S_PFD p2) { return p0; }
EXPORT double f14_D_DFS_PFP(double p0, float p1, struct S_PFP p2) { return p0; }
EXPORT double f14_D_DFS_PDI(double p0, float p1, struct S_PDI p2) { return p0; }
EXPORT double f14_D_DFS_PDF(double p0, float p1, struct S_PDF p2) { return p0; }
EXPORT double f14_D_DFS_PDD(double p0, float p1, struct S_PDD p2) { return p0; }
EXPORT double f14_D_DFS_PDP(double p0, float p1, struct S_PDP p2) { return p0; }
EXPORT double f14_D_DFS_PPI(double p0, float p1, struct S_PPI p2) { return p0; }
EXPORT double f14_D_DFS_PPF(double p0, float p1, struct S_PPF p2) { return p0; }
EXPORT double f14_D_DFS_PPD(double p0, float p1, struct S_PPD p2) { return p0; }
EXPORT double f14_D_DFS_PPP(double p0, float p1, struct S_PPP p2) { return p0; }
EXPORT double f14_D_DDI_(double p0, double p1, int p2) { return p0; }
EXPORT double f14_D_DDF_(double p0, double p1, float p2) { return p0; }
EXPORT double f14_D_DDD_(double p0, double p1, double p2) { return p0; }
EXPORT double f14_D_DDP_(double p0, double p1, void* p2) { return p0; }
EXPORT double f14_D_DDS_I(double p0, double p1, struct S_I p2) { return p0; }
EXPORT double f14_D_DDS_F(double p0, double p1, struct S_F p2) { return p0; }
EXPORT double f14_D_DDS_D(double p0, double p1, struct S_D p2) { return p0; }
EXPORT double f14_D_DDS_P(double p0, double p1, struct S_P p2) { return p0; }
EXPORT double f14_D_DDS_II(double p0, double p1, struct S_II p2) { return p0; }
EXPORT double f14_D_DDS_IF(double p0, double p1, struct S_IF p2) { return p0; }
EXPORT double f14_D_DDS_ID(double p0, double p1, struct S_ID p2) { return p0; }
EXPORT double f14_D_DDS_IP(double p0, double p1, struct S_IP p2) { return p0; }
EXPORT double f14_D_DDS_FI(double p0, double p1, struct S_FI p2) { return p0; }
EXPORT double f14_D_DDS_FF(double p0, double p1, struct S_FF p2) { return p0; }
EXPORT double f14_D_DDS_FD(double p0, double p1, struct S_FD p2) { return p0; }
EXPORT double f14_D_DDS_FP(double p0, double p1, struct S_FP p2) { return p0; }
EXPORT double f14_D_DDS_DI(double p0, double p1, struct S_DI p2) { return p0; }
EXPORT double f14_D_DDS_DF(double p0, double p1, struct S_DF p2) { return p0; }
EXPORT double f14_D_DDS_DD(double p0, double p1, struct S_DD p2) { return p0; }
EXPORT double f14_D_DDS_DP(double p0, double p1, struct S_DP p2) { return p0; }
EXPORT double f14_D_DDS_PI(double p0, double p1, struct S_PI p2) { return p0; }
EXPORT double f14_D_DDS_PF(double p0, double p1, struct S_PF p2) { return p0; }
EXPORT double f14_D_DDS_PD(double p0, double p1, struct S_PD p2) { return p0; }
EXPORT double f14_D_DDS_PP(double p0, double p1, struct S_PP p2) { return p0; }
EXPORT double f14_D_DDS_III(double p0, double p1, struct S_III p2) { return p0; }
EXPORT double f14_D_DDS_IIF(double p0, double p1, struct S_IIF p2) { return p0; }
EXPORT double f14_D_DDS_IID(double p0, double p1, struct S_IID p2) { return p0; }
EXPORT double f14_D_DDS_IIP(double p0, double p1, struct S_IIP p2) { return p0; }
EXPORT double f14_D_DDS_IFI(double p0, double p1, struct S_IFI p2) { return p0; }
EXPORT double f14_D_DDS_IFF(double p0, double p1, struct S_IFF p2) { return p0; }
EXPORT double f14_D_DDS_IFD(double p0, double p1, struct S_IFD p2) { return p0; }
EXPORT double f14_D_DDS_IFP(double p0, double p1, struct S_IFP p2) { return p0; }
EXPORT double f14_D_DDS_IDI(double p0, double p1, struct S_IDI p2) { return p0; }
EXPORT double f14_D_DDS_IDF(double p0, double p1, struct S_IDF p2) { return p0; }
EXPORT double f14_D_DDS_IDD(double p0, double p1, struct S_IDD p2) { return p0; }
EXPORT double f14_D_DDS_IDP(double p0, double p1, struct S_IDP p2) { return p0; }
EXPORT double f14_D_DDS_IPI(double p0, double p1, struct S_IPI p2) { return p0; }
EXPORT double f14_D_DDS_IPF(double p0, double p1, struct S_IPF p2) { return p0; }
EXPORT double f14_D_DDS_IPD(double p0, double p1, struct S_IPD p2) { return p0; }
EXPORT double f14_D_DDS_IPP(double p0, double p1, struct S_IPP p2) { return p0; }
EXPORT double f14_D_DDS_FII(double p0, double p1, struct S_FII p2) { return p0; }
EXPORT double f14_D_DDS_FIF(double p0, double p1, struct S_FIF p2) { return p0; }
EXPORT double f14_D_DDS_FID(double p0, double p1, struct S_FID p2) { return p0; }
EXPORT double f14_D_DDS_FIP(double p0, double p1, struct S_FIP p2) { return p0; }
EXPORT double f14_D_DDS_FFI(double p0, double p1, struct S_FFI p2) { return p0; }
EXPORT double f14_D_DDS_FFF(double p0, double p1, struct S_FFF p2) { return p0; }
EXPORT double f14_D_DDS_FFD(double p0, double p1, struct S_FFD p2) { return p0; }
EXPORT double f14_D_DDS_FFP(double p0, double p1, struct S_FFP p2) { return p0; }
EXPORT double f14_D_DDS_FDI(double p0, double p1, struct S_FDI p2) { return p0; }
EXPORT double f14_D_DDS_FDF(double p0, double p1, struct S_FDF p2) { return p0; }
EXPORT double f14_D_DDS_FDD(double p0, double p1, struct S_FDD p2) { return p0; }
EXPORT double f14_D_DDS_FDP(double p0, double p1, struct S_FDP p2) { return p0; }
EXPORT double f14_D_DDS_FPI(double p0, double p1, struct S_FPI p2) { return p0; }
EXPORT double f14_D_DDS_FPF(double p0, double p1, struct S_FPF p2) { return p0; }
EXPORT double f14_D_DDS_FPD(double p0, double p1, struct S_FPD p2) { return p0; }
EXPORT double f14_D_DDS_FPP(double p0, double p1, struct S_FPP p2) { return p0; }
EXPORT double f14_D_DDS_DII(double p0, double p1, struct S_DII p2) { return p0; }
EXPORT double f14_D_DDS_DIF(double p0, double p1, struct S_DIF p2) { return p0; }
EXPORT double f14_D_DDS_DID(double p0, double p1, struct S_DID p2) { return p0; }
EXPORT double f14_D_DDS_DIP(double p0, double p1, struct S_DIP p2) { return p0; }
EXPORT double f14_D_DDS_DFI(double p0, double p1, struct S_DFI p2) { return p0; }
EXPORT double f14_D_DDS_DFF(double p0, double p1, struct S_DFF p2) { return p0; }
EXPORT double f14_D_DDS_DFD(double p0, double p1, struct S_DFD p2) { return p0; }
EXPORT double f14_D_DDS_DFP(double p0, double p1, struct S_DFP p2) { return p0; }
EXPORT double f14_D_DDS_DDI(double p0, double p1, struct S_DDI p2) { return p0; }
EXPORT double f14_D_DDS_DDF(double p0, double p1, struct S_DDF p2) { return p0; }
EXPORT double f14_D_DDS_DDD(double p0, double p1, struct S_DDD p2) { return p0; }
EXPORT double f14_D_DDS_DDP(double p0, double p1, struct S_DDP p2) { return p0; }
EXPORT double f14_D_DDS_DPI(double p0, double p1, struct S_DPI p2) { return p0; }
EXPORT double f14_D_DDS_DPF(double p0, double p1, struct S_DPF p2) { return p0; }
EXPORT double f14_D_DDS_DPD(double p0, double p1, struct S_DPD p2) { return p0; }
EXPORT double f14_D_DDS_DPP(double p0, double p1, struct S_DPP p2) { return p0; }
EXPORT double f14_D_DDS_PII(double p0, double p1, struct S_PII p2) { return p0; }
EXPORT double f14_D_DDS_PIF(double p0, double p1, struct S_PIF p2) { return p0; }
EXPORT double f14_D_DDS_PID(double p0, double p1, struct S_PID p2) { return p0; }
EXPORT double f14_D_DDS_PIP(double p0, double p1, struct S_PIP p2) { return p0; }
EXPORT double f14_D_DDS_PFI(double p0, double p1, struct S_PFI p2) { return p0; }
EXPORT double f14_D_DDS_PFF(double p0, double p1, struct S_PFF p2) { return p0; }
EXPORT double f14_D_DDS_PFD(double p0, double p1, struct S_PFD p2) { return p0; }
EXPORT double f14_D_DDS_PFP(double p0, double p1, struct S_PFP p2) { return p0; }
EXPORT double f14_D_DDS_PDI(double p0, double p1, struct S_PDI p2) { return p0; }
EXPORT double f14_D_DDS_PDF(double p0, double p1, struct S_PDF p2) { return p0; }
EXPORT double f14_D_DDS_PDD(double p0, double p1, struct S_PDD p2) { return p0; }
EXPORT double f14_D_DDS_PDP(double p0, double p1, struct S_PDP p2) { return p0; }
EXPORT double f14_D_DDS_PPI(double p0, double p1, struct S_PPI p2) { return p0; }
EXPORT double f14_D_DDS_PPF(double p0, double p1, struct S_PPF p2) { return p0; }
EXPORT double f14_D_DDS_PPD(double p0, double p1, struct S_PPD p2) { return p0; }
EXPORT double f14_D_DDS_PPP(double p0, double p1, struct S_PPP p2) { return p0; }
EXPORT double f14_D_DPI_(double p0, void* p1, int p2) { return p0; }
EXPORT double f14_D_DPF_(double p0, void* p1, float p2) { return p0; }
EXPORT double f14_D_DPD_(double p0, void* p1, double p2) { return p0; }
EXPORT double f14_D_DPP_(double p0, void* p1, void* p2) { return p0; }
EXPORT double f14_D_DPS_I(double p0, void* p1, struct S_I p2) { return p0; }
EXPORT double f14_D_DPS_F(double p0, void* p1, struct S_F p2) { return p0; }
EXPORT double f14_D_DPS_D(double p0, void* p1, struct S_D p2) { return p0; }
EXPORT double f14_D_DPS_P(double p0, void* p1, struct S_P p2) { return p0; }
EXPORT double f14_D_DPS_II(double p0, void* p1, struct S_II p2) { return p0; }
EXPORT double f14_D_DPS_IF(double p0, void* p1, struct S_IF p2) { return p0; }
EXPORT double f14_D_DPS_ID(double p0, void* p1, struct S_ID p2) { return p0; }
EXPORT double f14_D_DPS_IP(double p0, void* p1, struct S_IP p2) { return p0; }
EXPORT double f14_D_DPS_FI(double p0, void* p1, struct S_FI p2) { return p0; }
EXPORT double f14_D_DPS_FF(double p0, void* p1, struct S_FF p2) { return p0; }
EXPORT double f14_D_DPS_FD(double p0, void* p1, struct S_FD p2) { return p0; }
EXPORT double f14_D_DPS_FP(double p0, void* p1, struct S_FP p2) { return p0; }
EXPORT double f14_D_DPS_DI(double p0, void* p1, struct S_DI p2) { return p0; }
EXPORT double f14_D_DPS_DF(double p0, void* p1, struct S_DF p2) { return p0; }
EXPORT double f14_D_DPS_DD(double p0, void* p1, struct S_DD p2) { return p0; }
EXPORT double f14_D_DPS_DP(double p0, void* p1, struct S_DP p2) { return p0; }
EXPORT double f14_D_DPS_PI(double p0, void* p1, struct S_PI p2) { return p0; }
EXPORT double f14_D_DPS_PF(double p0, void* p1, struct S_PF p2) { return p0; }
EXPORT double f14_D_DPS_PD(double p0, void* p1, struct S_PD p2) { return p0; }
EXPORT double f14_D_DPS_PP(double p0, void* p1, struct S_PP p2) { return p0; }
EXPORT double f14_D_DPS_III(double p0, void* p1, struct S_III p2) { return p0; }
EXPORT double f14_D_DPS_IIF(double p0, void* p1, struct S_IIF p2) { return p0; }
EXPORT double f14_D_DPS_IID(double p0, void* p1, struct S_IID p2) { return p0; }
EXPORT double f14_D_DPS_IIP(double p0, void* p1, struct S_IIP p2) { return p0; }
EXPORT double f14_D_DPS_IFI(double p0, void* p1, struct S_IFI p2) { return p0; }
EXPORT double f14_D_DPS_IFF(double p0, void* p1, struct S_IFF p2) { return p0; }
EXPORT double f14_D_DPS_IFD(double p0, void* p1, struct S_IFD p2) { return p0; }
EXPORT double f14_D_DPS_IFP(double p0, void* p1, struct S_IFP p2) { return p0; }
EXPORT double f14_D_DPS_IDI(double p0, void* p1, struct S_IDI p2) { return p0; }
EXPORT double f14_D_DPS_IDF(double p0, void* p1, struct S_IDF p2) { return p0; }
EXPORT double f14_D_DPS_IDD(double p0, void* p1, struct S_IDD p2) { return p0; }
EXPORT double f14_D_DPS_IDP(double p0, void* p1, struct S_IDP p2) { return p0; }
EXPORT double f14_D_DPS_IPI(double p0, void* p1, struct S_IPI p2) { return p0; }
EXPORT double f14_D_DPS_IPF(double p0, void* p1, struct S_IPF p2) { return p0; }
EXPORT double f14_D_DPS_IPD(double p0, void* p1, struct S_IPD p2) { return p0; }
EXPORT double f14_D_DPS_IPP(double p0, void* p1, struct S_IPP p2) { return p0; }
EXPORT double f14_D_DPS_FII(double p0, void* p1, struct S_FII p2) { return p0; }
EXPORT double f14_D_DPS_FIF(double p0, void* p1, struct S_FIF p2) { return p0; }
EXPORT double f14_D_DPS_FID(double p0, void* p1, struct S_FID p2) { return p0; }
EXPORT double f14_D_DPS_FIP(double p0, void* p1, struct S_FIP p2) { return p0; }
EXPORT double f14_D_DPS_FFI(double p0, void* p1, struct S_FFI p2) { return p0; }
EXPORT double f14_D_DPS_FFF(double p0, void* p1, struct S_FFF p2) { return p0; }
EXPORT double f14_D_DPS_FFD(double p0, void* p1, struct S_FFD p2) { return p0; }
EXPORT double f14_D_DPS_FFP(double p0, void* p1, struct S_FFP p2) { return p0; }
EXPORT double f14_D_DPS_FDI(double p0, void* p1, struct S_FDI p2) { return p0; }
EXPORT double f14_D_DPS_FDF(double p0, void* p1, struct S_FDF p2) { return p0; }
EXPORT double f14_D_DPS_FDD(double p0, void* p1, struct S_FDD p2) { return p0; }
EXPORT double f14_D_DPS_FDP(double p0, void* p1, struct S_FDP p2) { return p0; }
EXPORT double f14_D_DPS_FPI(double p0, void* p1, struct S_FPI p2) { return p0; }
EXPORT double f14_D_DPS_FPF(double p0, void* p1, struct S_FPF p2) { return p0; }
EXPORT double f14_D_DPS_FPD(double p0, void* p1, struct S_FPD p2) { return p0; }
EXPORT double f14_D_DPS_FPP(double p0, void* p1, struct S_FPP p2) { return p0; }
EXPORT double f14_D_DPS_DII(double p0, void* p1, struct S_DII p2) { return p0; }
EXPORT double f14_D_DPS_DIF(double p0, void* p1, struct S_DIF p2) { return p0; }
EXPORT double f14_D_DPS_DID(double p0, void* p1, struct S_DID p2) { return p0; }
EXPORT double f14_D_DPS_DIP(double p0, void* p1, struct S_DIP p2) { return p0; }
EXPORT double f14_D_DPS_DFI(double p0, void* p1, struct S_DFI p2) { return p0; }
EXPORT double f14_D_DPS_DFF(double p0, void* p1, struct S_DFF p2) { return p0; }
EXPORT double f14_D_DPS_DFD(double p0, void* p1, struct S_DFD p2) { return p0; }
EXPORT double f14_D_DPS_DFP(double p0, void* p1, struct S_DFP p2) { return p0; }
EXPORT double f14_D_DPS_DDI(double p0, void* p1, struct S_DDI p2) { return p0; }
EXPORT double f14_D_DPS_DDF(double p0, void* p1, struct S_DDF p2) { return p0; }
EXPORT double f14_D_DPS_DDD(double p0, void* p1, struct S_DDD p2) { return p0; }
EXPORT double f14_D_DPS_DDP(double p0, void* p1, struct S_DDP p2) { return p0; }
EXPORT double f14_D_DPS_DPI(double p0, void* p1, struct S_DPI p2) { return p0; }
EXPORT double f14_D_DPS_DPF(double p0, void* p1, struct S_DPF p2) { return p0; }
EXPORT double f14_D_DPS_DPD(double p0, void* p1, struct S_DPD p2) { return p0; }
EXPORT double f14_D_DPS_DPP(double p0, void* p1, struct S_DPP p2) { return p0; }
EXPORT double f14_D_DPS_PII(double p0, void* p1, struct S_PII p2) { return p0; }
EXPORT double f14_D_DPS_PIF(double p0, void* p1, struct S_PIF p2) { return p0; }
EXPORT double f14_D_DPS_PID(double p0, void* p1, struct S_PID p2) { return p0; }
EXPORT double f14_D_DPS_PIP(double p0, void* p1, struct S_PIP p2) { return p0; }
EXPORT double f14_D_DPS_PFI(double p0, void* p1, struct S_PFI p2) { return p0; }
EXPORT double f14_D_DPS_PFF(double p0, void* p1, struct S_PFF p2) { return p0; }
EXPORT double f14_D_DPS_PFD(double p0, void* p1, struct S_PFD p2) { return p0; }
EXPORT double f14_D_DPS_PFP(double p0, void* p1, struct S_PFP p2) { return p0; }
EXPORT double f14_D_DPS_PDI(double p0, void* p1, struct S_PDI p2) { return p0; }
EXPORT double f14_D_DPS_PDF(double p0, void* p1, struct S_PDF p2) { return p0; }
EXPORT double f14_D_DPS_PDD(double p0, void* p1, struct S_PDD p2) { return p0; }
EXPORT double f14_D_DPS_PDP(double p0, void* p1, struct S_PDP p2) { return p0; }
EXPORT double f14_D_DPS_PPI(double p0, void* p1, struct S_PPI p2) { return p0; }
EXPORT double f14_D_DPS_PPF(double p0, void* p1, struct S_PPF p2) { return p0; }
EXPORT double f14_D_DPS_PPD(double p0, void* p1, struct S_PPD p2) { return p0; }
EXPORT double f14_D_DPS_PPP(double p0, void* p1, struct S_PPP p2) { return p0; }
EXPORT double f14_D_DSI_I(double p0, struct S_I p1, int p2) { return p0; }
EXPORT double f14_D_DSI_F(double p0, struct S_F p1, int p2) { return p0; }
EXPORT double f14_D_DSI_D(double p0, struct S_D p1, int p2) { return p0; }
EXPORT double f14_D_DSI_P(double p0, struct S_P p1, int p2) { return p0; }
EXPORT double f14_D_DSI_II(double p0, struct S_II p1, int p2) { return p0; }
EXPORT double f14_D_DSI_IF(double p0, struct S_IF p1, int p2) { return p0; }
EXPORT double f14_D_DSI_ID(double p0, struct S_ID p1, int p2) { return p0; }
EXPORT double f14_D_DSI_IP(double p0, struct S_IP p1, int p2) { return p0; }
EXPORT double f14_D_DSI_FI(double p0, struct S_FI p1, int p2) { return p0; }
EXPORT double f14_D_DSI_FF(double p0, struct S_FF p1, int p2) { return p0; }
EXPORT double f14_D_DSI_FD(double p0, struct S_FD p1, int p2) { return p0; }
EXPORT double f14_D_DSI_FP(double p0, struct S_FP p1, int p2) { return p0; }
EXPORT double f14_D_DSI_DI(double p0, struct S_DI p1, int p2) { return p0; }
EXPORT double f14_D_DSI_DF(double p0, struct S_DF p1, int p2) { return p0; }
EXPORT double f14_D_DSI_DD(double p0, struct S_DD p1, int p2) { return p0; }
EXPORT double f14_D_DSI_DP(double p0, struct S_DP p1, int p2) { return p0; }
EXPORT double f14_D_DSI_PI(double p0, struct S_PI p1, int p2) { return p0; }
EXPORT double f14_D_DSI_PF(double p0, struct S_PF p1, int p2) { return p0; }
EXPORT double f14_D_DSI_PD(double p0, struct S_PD p1, int p2) { return p0; }
EXPORT double f14_D_DSI_PP(double p0, struct S_PP p1, int p2) { return p0; }
EXPORT double f14_D_DSI_III(double p0, struct S_III p1, int p2) { return p0; }
EXPORT double f14_D_DSI_IIF(double p0, struct S_IIF p1, int p2) { return p0; }
EXPORT double f14_D_DSI_IID(double p0, struct S_IID p1, int p2) { return p0; }
EXPORT double f14_D_DSI_IIP(double p0, struct S_IIP p1, int p2) { return p0; }
EXPORT double f14_D_DSI_IFI(double p0, struct S_IFI p1, int p2) { return p0; }
EXPORT double f14_D_DSI_IFF(double p0, struct S_IFF p1, int p2) { return p0; }
EXPORT double f14_D_DSI_IFD(double p0, struct S_IFD p1, int p2) { return p0; }
EXPORT double f14_D_DSI_IFP(double p0, struct S_IFP p1, int p2) { return p0; }
EXPORT double f14_D_DSI_IDI(double p0, struct S_IDI p1, int p2) { return p0; }
EXPORT double f14_D_DSI_IDF(double p0, struct S_IDF p1, int p2) { return p0; }
EXPORT double f14_D_DSI_IDD(double p0, struct S_IDD p1, int p2) { return p0; }
EXPORT double f14_D_DSI_IDP(double p0, struct S_IDP p1, int p2) { return p0; }
EXPORT double f14_D_DSI_IPI(double p0, struct S_IPI p1, int p2) { return p0; }
EXPORT double f14_D_DSI_IPF(double p0, struct S_IPF p1, int p2) { return p0; }
EXPORT double f14_D_DSI_IPD(double p0, struct S_IPD p1, int p2) { return p0; }
EXPORT double f14_D_DSI_IPP(double p0, struct S_IPP p1, int p2) { return p0; }
EXPORT double f14_D_DSI_FII(double p0, struct S_FII p1, int p2) { return p0; }
EXPORT double f14_D_DSI_FIF(double p0, struct S_FIF p1, int p2) { return p0; }
EXPORT double f14_D_DSI_FID(double p0, struct S_FID p1, int p2) { return p0; }
EXPORT double f14_D_DSI_FIP(double p0, struct S_FIP p1, int p2) { return p0; }
EXPORT double f14_D_DSI_FFI(double p0, struct S_FFI p1, int p2) { return p0; }
EXPORT double f14_D_DSI_FFF(double p0, struct S_FFF p1, int p2) { return p0; }
EXPORT double f14_D_DSI_FFD(double p0, struct S_FFD p1, int p2) { return p0; }
EXPORT double f14_D_DSI_FFP(double p0, struct S_FFP p1, int p2) { return p0; }
EXPORT double f14_D_DSI_FDI(double p0, struct S_FDI p1, int p2) { return p0; }
EXPORT double f14_D_DSI_FDF(double p0, struct S_FDF p1, int p2) { return p0; }
EXPORT double f14_D_DSI_FDD(double p0, struct S_FDD p1, int p2) { return p0; }
EXPORT double f14_D_DSI_FDP(double p0, struct S_FDP p1, int p2) { return p0; }
EXPORT double f14_D_DSI_FPI(double p0, struct S_FPI p1, int p2) { return p0; }
EXPORT double f14_D_DSI_FPF(double p0, struct S_FPF p1, int p2) { return p0; }
EXPORT double f14_D_DSI_FPD(double p0, struct S_FPD p1, int p2) { return p0; }
EXPORT double f14_D_DSI_FPP(double p0, struct S_FPP p1, int p2) { return p0; }
EXPORT double f14_D_DSI_DII(double p0, struct S_DII p1, int p2) { return p0; }
EXPORT double f14_D_DSI_DIF(double p0, struct S_DIF p1, int p2) { return p0; }
EXPORT double f14_D_DSI_DID(double p0, struct S_DID p1, int p2) { return p0; }
EXPORT double f14_D_DSI_DIP(double p0, struct S_DIP p1, int p2) { return p0; }
EXPORT double f14_D_DSI_DFI(double p0, struct S_DFI p1, int p2) { return p0; }
EXPORT double f14_D_DSI_DFF(double p0, struct S_DFF p1, int p2) { return p0; }
EXPORT double f14_D_DSI_DFD(double p0, struct S_DFD p1, int p2) { return p0; }
EXPORT double f14_D_DSI_DFP(double p0, struct S_DFP p1, int p2) { return p0; }
EXPORT double f14_D_DSI_DDI(double p0, struct S_DDI p1, int p2) { return p0; }
EXPORT double f14_D_DSI_DDF(double p0, struct S_DDF p1, int p2) { return p0; }
EXPORT double f14_D_DSI_DDD(double p0, struct S_DDD p1, int p2) { return p0; }
EXPORT double f14_D_DSI_DDP(double p0, struct S_DDP p1, int p2) { return p0; }
EXPORT double f14_D_DSI_DPI(double p0, struct S_DPI p1, int p2) { return p0; }
EXPORT double f14_D_DSI_DPF(double p0, struct S_DPF p1, int p2) { return p0; }
EXPORT double f14_D_DSI_DPD(double p0, struct S_DPD p1, int p2) { return p0; }
EXPORT double f14_D_DSI_DPP(double p0, struct S_DPP p1, int p2) { return p0; }
EXPORT double f14_D_DSI_PII(double p0, struct S_PII p1, int p2) { return p0; }
EXPORT double f14_D_DSI_PIF(double p0, struct S_PIF p1, int p2) { return p0; }
EXPORT double f14_D_DSI_PID(double p0, struct S_PID p1, int p2) { return p0; }
EXPORT double f14_D_DSI_PIP(double p0, struct S_PIP p1, int p2) { return p0; }
EXPORT double f14_D_DSI_PFI(double p0, struct S_PFI p1, int p2) { return p0; }
EXPORT double f14_D_DSI_PFF(double p0, struct S_PFF p1, int p2) { return p0; }
EXPORT double f14_D_DSI_PFD(double p0, struct S_PFD p1, int p2) { return p0; }
EXPORT double f14_D_DSI_PFP(double p0, struct S_PFP p1, int p2) { return p0; }
EXPORT double f14_D_DSI_PDI(double p0, struct S_PDI p1, int p2) { return p0; }
EXPORT double f14_D_DSI_PDF(double p0, struct S_PDF p1, int p2) { return p0; }
EXPORT double f14_D_DSI_PDD(double p0, struct S_PDD p1, int p2) { return p0; }
EXPORT double f14_D_DSI_PDP(double p0, struct S_PDP p1, int p2) { return p0; }
EXPORT double f14_D_DSI_PPI(double p0, struct S_PPI p1, int p2) { return p0; }
EXPORT double f14_D_DSI_PPF(double p0, struct S_PPF p1, int p2) { return p0; }
EXPORT double f14_D_DSI_PPD(double p0, struct S_PPD p1, int p2) { return p0; }
EXPORT double f14_D_DSI_PPP(double p0, struct S_PPP p1, int p2) { return p0; }
EXPORT double f14_D_DSF_I(double p0, struct S_I p1, float p2) { return p0; }
EXPORT double f14_D_DSF_F(double p0, struct S_F p1, float p2) { return p0; }
EXPORT double f14_D_DSF_D(double p0, struct S_D p1, float p2) { return p0; }
EXPORT double f14_D_DSF_P(double p0, struct S_P p1, float p2) { return p0; }
EXPORT double f14_D_DSF_II(double p0, struct S_II p1, float p2) { return p0; }
EXPORT double f14_D_DSF_IF(double p0, struct S_IF p1, float p2) { return p0; }
EXPORT double f14_D_DSF_ID(double p0, struct S_ID p1, float p2) { return p0; }
EXPORT double f14_D_DSF_IP(double p0, struct S_IP p1, float p2) { return p0; }
EXPORT double f14_D_DSF_FI(double p0, struct S_FI p1, float p2) { return p0; }
EXPORT double f14_D_DSF_FF(double p0, struct S_FF p1, float p2) { return p0; }
EXPORT double f14_D_DSF_FD(double p0, struct S_FD p1, float p2) { return p0; }
EXPORT double f14_D_DSF_FP(double p0, struct S_FP p1, float p2) { return p0; }
EXPORT double f14_D_DSF_DI(double p0, struct S_DI p1, float p2) { return p0; }
EXPORT double f14_D_DSF_DF(double p0, struct S_DF p1, float p2) { return p0; }
EXPORT double f14_D_DSF_DD(double p0, struct S_DD p1, float p2) { return p0; }
EXPORT double f14_D_DSF_DP(double p0, struct S_DP p1, float p2) { return p0; }
EXPORT double f14_D_DSF_PI(double p0, struct S_PI p1, float p2) { return p0; }
EXPORT double f14_D_DSF_PF(double p0, struct S_PF p1, float p2) { return p0; }
EXPORT double f14_D_DSF_PD(double p0, struct S_PD p1, float p2) { return p0; }
EXPORT double f14_D_DSF_PP(double p0, struct S_PP p1, float p2) { return p0; }
EXPORT double f14_D_DSF_III(double p0, struct S_III p1, float p2) { return p0; }
EXPORT double f14_D_DSF_IIF(double p0, struct S_IIF p1, float p2) { return p0; }
EXPORT double f14_D_DSF_IID(double p0, struct S_IID p1, float p2) { return p0; }
EXPORT double f14_D_DSF_IIP(double p0, struct S_IIP p1, float p2) { return p0; }
EXPORT double f14_D_DSF_IFI(double p0, struct S_IFI p1, float p2) { return p0; }
EXPORT double f14_D_DSF_IFF(double p0, struct S_IFF p1, float p2) { return p0; }
EXPORT double f14_D_DSF_IFD(double p0, struct S_IFD p1, float p2) { return p0; }
EXPORT double f14_D_DSF_IFP(double p0, struct S_IFP p1, float p2) { return p0; }
EXPORT double f14_D_DSF_IDI(double p0, struct S_IDI p1, float p2) { return p0; }
EXPORT double f14_D_DSF_IDF(double p0, struct S_IDF p1, float p2) { return p0; }
EXPORT double f14_D_DSF_IDD(double p0, struct S_IDD p1, float p2) { return p0; }
EXPORT double f14_D_DSF_IDP(double p0, struct S_IDP p1, float p2) { return p0; }
EXPORT double f14_D_DSF_IPI(double p0, struct S_IPI p1, float p2) { return p0; }
EXPORT double f14_D_DSF_IPF(double p0, struct S_IPF p1, float p2) { return p0; }
EXPORT double f14_D_DSF_IPD(double p0, struct S_IPD p1, float p2) { return p0; }
EXPORT double f14_D_DSF_IPP(double p0, struct S_IPP p1, float p2) { return p0; }
EXPORT double f14_D_DSF_FII(double p0, struct S_FII p1, float p2) { return p0; }
EXPORT double f14_D_DSF_FIF(double p0, struct S_FIF p1, float p2) { return p0; }
EXPORT double f14_D_DSF_FID(double p0, struct S_FID p1, float p2) { return p0; }
EXPORT double f14_D_DSF_FIP(double p0, struct S_FIP p1, float p2) { return p0; }
EXPORT double f14_D_DSF_FFI(double p0, struct S_FFI p1, float p2) { return p0; }
EXPORT double f14_D_DSF_FFF(double p0, struct S_FFF p1, float p2) { return p0; }
EXPORT double f14_D_DSF_FFD(double p0, struct S_FFD p1, float p2) { return p0; }
EXPORT double f14_D_DSF_FFP(double p0, struct S_FFP p1, float p2) { return p0; }
EXPORT double f14_D_DSF_FDI(double p0, struct S_FDI p1, float p2) { return p0; }
EXPORT double f14_D_DSF_FDF(double p0, struct S_FDF p1, float p2) { return p0; }
EXPORT double f14_D_DSF_FDD(double p0, struct S_FDD p1, float p2) { return p0; }
EXPORT double f14_D_DSF_FDP(double p0, struct S_FDP p1, float p2) { return p0; }
EXPORT double f14_D_DSF_FPI(double p0, struct S_FPI p1, float p2) { return p0; }
EXPORT double f14_D_DSF_FPF(double p0, struct S_FPF p1, float p2) { return p0; }
EXPORT double f14_D_DSF_FPD(double p0, struct S_FPD p1, float p2) { return p0; }
EXPORT double f14_D_DSF_FPP(double p0, struct S_FPP p1, float p2) { return p0; }
EXPORT double f14_D_DSF_DII(double p0, struct S_DII p1, float p2) { return p0; }
EXPORT double f14_D_DSF_DIF(double p0, struct S_DIF p1, float p2) { return p0; }
EXPORT double f14_D_DSF_DID(double p0, struct S_DID p1, float p2) { return p0; }
EXPORT double f14_D_DSF_DIP(double p0, struct S_DIP p1, float p2) { return p0; }
EXPORT double f14_D_DSF_DFI(double p0, struct S_DFI p1, float p2) { return p0; }
EXPORT double f14_D_DSF_DFF(double p0, struct S_DFF p1, float p2) { return p0; }
EXPORT double f14_D_DSF_DFD(double p0, struct S_DFD p1, float p2) { return p0; }
EXPORT double f14_D_DSF_DFP(double p0, struct S_DFP p1, float p2) { return p0; }
EXPORT double f14_D_DSF_DDI(double p0, struct S_DDI p1, float p2) { return p0; }
EXPORT double f14_D_DSF_DDF(double p0, struct S_DDF p1, float p2) { return p0; }
EXPORT double f14_D_DSF_DDD(double p0, struct S_DDD p1, float p2) { return p0; }
EXPORT double f14_D_DSF_DDP(double p0, struct S_DDP p1, float p2) { return p0; }
EXPORT double f14_D_DSF_DPI(double p0, struct S_DPI p1, float p2) { return p0; }
EXPORT double f14_D_DSF_DPF(double p0, struct S_DPF p1, float p2) { return p0; }
EXPORT double f14_D_DSF_DPD(double p0, struct S_DPD p1, float p2) { return p0; }
EXPORT double f14_D_DSF_DPP(double p0, struct S_DPP p1, float p2) { return p0; }
EXPORT double f14_D_DSF_PII(double p0, struct S_PII p1, float p2) { return p0; }
EXPORT double f14_D_DSF_PIF(double p0, struct S_PIF p1, float p2) { return p0; }
EXPORT double f14_D_DSF_PID(double p0, struct S_PID p1, float p2) { return p0; }
EXPORT double f14_D_DSF_PIP(double p0, struct S_PIP p1, float p2) { return p0; }
EXPORT double f14_D_DSF_PFI(double p0, struct S_PFI p1, float p2) { return p0; }
EXPORT double f14_D_DSF_PFF(double p0, struct S_PFF p1, float p2) { return p0; }
EXPORT double f14_D_DSF_PFD(double p0, struct S_PFD p1, float p2) { return p0; }
EXPORT double f14_D_DSF_PFP(double p0, struct S_PFP p1, float p2) { return p0; }
EXPORT double f14_D_DSF_PDI(double p0, struct S_PDI p1, float p2) { return p0; }
EXPORT double f14_D_DSF_PDF(double p0, struct S_PDF p1, float p2) { return p0; }
EXPORT double f14_D_DSF_PDD(double p0, struct S_PDD p1, float p2) { return p0; }
EXPORT double f14_D_DSF_PDP(double p0, struct S_PDP p1, float p2) { return p0; }
EXPORT double f14_D_DSF_PPI(double p0, struct S_PPI p1, float p2) { return p0; }
EXPORT double f14_D_DSF_PPF(double p0, struct S_PPF p1, float p2) { return p0; }
EXPORT double f14_D_DSF_PPD(double p0, struct S_PPD p1, float p2) { return p0; }
EXPORT double f14_D_DSF_PPP(double p0, struct S_PPP p1, float p2) { return p0; }
EXPORT double f14_D_DSD_I(double p0, struct S_I p1, double p2) { return p0; }
EXPORT double f14_D_DSD_F(double p0, struct S_F p1, double p2) { return p0; }
EXPORT double f14_D_DSD_D(double p0, struct S_D p1, double p2) { return p0; }
EXPORT double f14_D_DSD_P(double p0, struct S_P p1, double p2) { return p0; }
EXPORT double f14_D_DSD_II(double p0, struct S_II p1, double p2) { return p0; }
EXPORT double f14_D_DSD_IF(double p0, struct S_IF p1, double p2) { return p0; }
EXPORT double f14_D_DSD_ID(double p0, struct S_ID p1, double p2) { return p0; }
EXPORT double f14_D_DSD_IP(double p0, struct S_IP p1, double p2) { return p0; }
EXPORT double f14_D_DSD_FI(double p0, struct S_FI p1, double p2) { return p0; }
EXPORT double f14_D_DSD_FF(double p0, struct S_FF p1, double p2) { return p0; }
EXPORT double f14_D_DSD_FD(double p0, struct S_FD p1, double p2) { return p0; }
EXPORT double f14_D_DSD_FP(double p0, struct S_FP p1, double p2) { return p0; }
EXPORT double f14_D_DSD_DI(double p0, struct S_DI p1, double p2) { return p0; }
EXPORT double f14_D_DSD_DF(double p0, struct S_DF p1, double p2) { return p0; }
EXPORT double f14_D_DSD_DD(double p0, struct S_DD p1, double p2) { return p0; }
EXPORT double f14_D_DSD_DP(double p0, struct S_DP p1, double p2) { return p0; }
EXPORT double f14_D_DSD_PI(double p0, struct S_PI p1, double p2) { return p0; }
EXPORT double f14_D_DSD_PF(double p0, struct S_PF p1, double p2) { return p0; }
EXPORT double f14_D_DSD_PD(double p0, struct S_PD p1, double p2) { return p0; }
EXPORT double f14_D_DSD_PP(double p0, struct S_PP p1, double p2) { return p0; }
EXPORT double f14_D_DSD_III(double p0, struct S_III p1, double p2) { return p0; }
EXPORT double f14_D_DSD_IIF(double p0, struct S_IIF p1, double p2) { return p0; }
EXPORT double f14_D_DSD_IID(double p0, struct S_IID p1, double p2) { return p0; }
EXPORT double f14_D_DSD_IIP(double p0, struct S_IIP p1, double p2) { return p0; }
EXPORT double f14_D_DSD_IFI(double p0, struct S_IFI p1, double p2) { return p0; }
EXPORT double f14_D_DSD_IFF(double p0, struct S_IFF p1, double p2) { return p0; }
EXPORT double f14_D_DSD_IFD(double p0, struct S_IFD p1, double p2) { return p0; }
EXPORT double f15_D_DSD_IFP(double p0, struct S_IFP p1, double p2) { return p0; }
EXPORT double f15_D_DSD_IDI(double p0, struct S_IDI p1, double p2) { return p0; }
EXPORT double f15_D_DSD_IDF(double p0, struct S_IDF p1, double p2) { return p0; }
EXPORT double f15_D_DSD_IDD(double p0, struct S_IDD p1, double p2) { return p0; }
EXPORT double f15_D_DSD_IDP(double p0, struct S_IDP p1, double p2) { return p0; }
EXPORT double f15_D_DSD_IPI(double p0, struct S_IPI p1, double p2) { return p0; }
EXPORT double f15_D_DSD_IPF(double p0, struct S_IPF p1, double p2) { return p0; }
EXPORT double f15_D_DSD_IPD(double p0, struct S_IPD p1, double p2) { return p0; }
EXPORT double f15_D_DSD_IPP(double p0, struct S_IPP p1, double p2) { return p0; }
EXPORT double f15_D_DSD_FII(double p0, struct S_FII p1, double p2) { return p0; }
EXPORT double f15_D_DSD_FIF(double p0, struct S_FIF p1, double p2) { return p0; }
EXPORT double f15_D_DSD_FID(double p0, struct S_FID p1, double p2) { return p0; }
EXPORT double f15_D_DSD_FIP(double p0, struct S_FIP p1, double p2) { return p0; }
EXPORT double f15_D_DSD_FFI(double p0, struct S_FFI p1, double p2) { return p0; }
EXPORT double f15_D_DSD_FFF(double p0, struct S_FFF p1, double p2) { return p0; }
EXPORT double f15_D_DSD_FFD(double p0, struct S_FFD p1, double p2) { return p0; }
EXPORT double f15_D_DSD_FFP(double p0, struct S_FFP p1, double p2) { return p0; }
EXPORT double f15_D_DSD_FDI(double p0, struct S_FDI p1, double p2) { return p0; }
EXPORT double f15_D_DSD_FDF(double p0, struct S_FDF p1, double p2) { return p0; }
EXPORT double f15_D_DSD_FDD(double p0, struct S_FDD p1, double p2) { return p0; }
EXPORT double f15_D_DSD_FDP(double p0, struct S_FDP p1, double p2) { return p0; }
EXPORT double f15_D_DSD_FPI(double p0, struct S_FPI p1, double p2) { return p0; }
EXPORT double f15_D_DSD_FPF(double p0, struct S_FPF p1, double p2) { return p0; }
EXPORT double f15_D_DSD_FPD(double p0, struct S_FPD p1, double p2) { return p0; }
EXPORT double f15_D_DSD_FPP(double p0, struct S_FPP p1, double p2) { return p0; }
EXPORT double f15_D_DSD_DII(double p0, struct S_DII p1, double p2) { return p0; }
EXPORT double f15_D_DSD_DIF(double p0, struct S_DIF p1, double p2) { return p0; }
EXPORT double f15_D_DSD_DID(double p0, struct S_DID p1, double p2) { return p0; }
EXPORT double f15_D_DSD_DIP(double p0, struct S_DIP p1, double p2) { return p0; }
EXPORT double f15_D_DSD_DFI(double p0, struct S_DFI p1, double p2) { return p0; }
EXPORT double f15_D_DSD_DFF(double p0, struct S_DFF p1, double p2) { return p0; }
EXPORT double f15_D_DSD_DFD(double p0, struct S_DFD p1, double p2) { return p0; }
EXPORT double f15_D_DSD_DFP(double p0, struct S_DFP p1, double p2) { return p0; }
EXPORT double f15_D_DSD_DDI(double p0, struct S_DDI p1, double p2) { return p0; }
EXPORT double f15_D_DSD_DDF(double p0, struct S_DDF p1, double p2) { return p0; }
EXPORT double f15_D_DSD_DDD(double p0, struct S_DDD p1, double p2) { return p0; }
EXPORT double f15_D_DSD_DDP(double p0, struct S_DDP p1, double p2) { return p0; }
EXPORT double f15_D_DSD_DPI(double p0, struct S_DPI p1, double p2) { return p0; }
EXPORT double f15_D_DSD_DPF(double p0, struct S_DPF p1, double p2) { return p0; }
EXPORT double f15_D_DSD_DPD(double p0, struct S_DPD p1, double p2) { return p0; }
EXPORT double f15_D_DSD_DPP(double p0, struct S_DPP p1, double p2) { return p0; }
EXPORT double f15_D_DSD_PII(double p0, struct S_PII p1, double p2) { return p0; }
EXPORT double f15_D_DSD_PIF(double p0, struct S_PIF p1, double p2) { return p0; }
EXPORT double f15_D_DSD_PID(double p0, struct S_PID p1, double p2) { return p0; }
EXPORT double f15_D_DSD_PIP(double p0, struct S_PIP p1, double p2) { return p0; }
EXPORT double f15_D_DSD_PFI(double p0, struct S_PFI p1, double p2) { return p0; }
EXPORT double f15_D_DSD_PFF(double p0, struct S_PFF p1, double p2) { return p0; }
EXPORT double f15_D_DSD_PFD(double p0, struct S_PFD p1, double p2) { return p0; }
EXPORT double f15_D_DSD_PFP(double p0, struct S_PFP p1, double p2) { return p0; }
EXPORT double f15_D_DSD_PDI(double p0, struct S_PDI p1, double p2) { return p0; }
EXPORT double f15_D_DSD_PDF(double p0, struct S_PDF p1, double p2) { return p0; }
EXPORT double f15_D_DSD_PDD(double p0, struct S_PDD p1, double p2) { return p0; }
EXPORT double f15_D_DSD_PDP(double p0, struct S_PDP p1, double p2) { return p0; }
EXPORT double f15_D_DSD_PPI(double p0, struct S_PPI p1, double p2) { return p0; }
EXPORT double f15_D_DSD_PPF(double p0, struct S_PPF p1, double p2) { return p0; }
EXPORT double f15_D_DSD_PPD(double p0, struct S_PPD p1, double p2) { return p0; }
EXPORT double f15_D_DSD_PPP(double p0, struct S_PPP p1, double p2) { return p0; }
EXPORT double f15_D_DSP_I(double p0, struct S_I p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_F(double p0, struct S_F p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_D(double p0, struct S_D p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_P(double p0, struct S_P p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_II(double p0, struct S_II p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_IF(double p0, struct S_IF p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_ID(double p0, struct S_ID p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_IP(double p0, struct S_IP p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_FI(double p0, struct S_FI p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_FF(double p0, struct S_FF p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_FD(double p0, struct S_FD p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_FP(double p0, struct S_FP p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_DI(double p0, struct S_DI p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_DF(double p0, struct S_DF p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_DD(double p0, struct S_DD p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_DP(double p0, struct S_DP p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_PI(double p0, struct S_PI p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_PF(double p0, struct S_PF p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_PD(double p0, struct S_PD p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_PP(double p0, struct S_PP p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_III(double p0, struct S_III p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_IIF(double p0, struct S_IIF p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_IID(double p0, struct S_IID p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_IIP(double p0, struct S_IIP p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_IFI(double p0, struct S_IFI p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_IFF(double p0, struct S_IFF p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_IFD(double p0, struct S_IFD p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_IFP(double p0, struct S_IFP p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_IDI(double p0, struct S_IDI p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_IDF(double p0, struct S_IDF p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_IDD(double p0, struct S_IDD p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_IDP(double p0, struct S_IDP p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_IPI(double p0, struct S_IPI p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_IPF(double p0, struct S_IPF p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_IPD(double p0, struct S_IPD p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_IPP(double p0, struct S_IPP p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_FII(double p0, struct S_FII p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_FIF(double p0, struct S_FIF p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_FID(double p0, struct S_FID p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_FIP(double p0, struct S_FIP p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_FFI(double p0, struct S_FFI p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_FFF(double p0, struct S_FFF p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_FFD(double p0, struct S_FFD p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_FFP(double p0, struct S_FFP p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_FDI(double p0, struct S_FDI p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_FDF(double p0, struct S_FDF p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_FDD(double p0, struct S_FDD p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_FDP(double p0, struct S_FDP p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_FPI(double p0, struct S_FPI p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_FPF(double p0, struct S_FPF p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_FPD(double p0, struct S_FPD p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_FPP(double p0, struct S_FPP p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_DII(double p0, struct S_DII p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_DIF(double p0, struct S_DIF p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_DID(double p0, struct S_DID p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_DIP(double p0, struct S_DIP p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_DFI(double p0, struct S_DFI p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_DFF(double p0, struct S_DFF p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_DFD(double p0, struct S_DFD p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_DFP(double p0, struct S_DFP p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_DDI(double p0, struct S_DDI p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_DDF(double p0, struct S_DDF p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_DDD(double p0, struct S_DDD p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_DDP(double p0, struct S_DDP p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_DPI(double p0, struct S_DPI p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_DPF(double p0, struct S_DPF p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_DPD(double p0, struct S_DPD p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_DPP(double p0, struct S_DPP p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_PII(double p0, struct S_PII p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_PIF(double p0, struct S_PIF p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_PID(double p0, struct S_PID p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_PIP(double p0, struct S_PIP p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_PFI(double p0, struct S_PFI p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_PFF(double p0, struct S_PFF p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_PFD(double p0, struct S_PFD p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_PFP(double p0, struct S_PFP p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_PDI(double p0, struct S_PDI p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_PDF(double p0, struct S_PDF p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_PDD(double p0, struct S_PDD p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_PDP(double p0, struct S_PDP p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_PPI(double p0, struct S_PPI p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_PPF(double p0, struct S_PPF p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_PPD(double p0, struct S_PPD p1, void* p2) { return p0; }
EXPORT double f15_D_DSP_PPP(double p0, struct S_PPP p1, void* p2) { return p0; }
EXPORT double f15_D_DSS_I(double p0, struct S_I p1, struct S_I p2) { return p0; }
EXPORT double f15_D_DSS_F(double p0, struct S_F p1, struct S_F p2) { return p0; }
EXPORT double f15_D_DSS_D(double p0, struct S_D p1, struct S_D p2) { return p0; }
EXPORT double f15_D_DSS_P(double p0, struct S_P p1, struct S_P p2) { return p0; }
EXPORT double f15_D_DSS_II(double p0, struct S_II p1, struct S_II p2) { return p0; }
EXPORT double f15_D_DSS_IF(double p0, struct S_IF p1, struct S_IF p2) { return p0; }
EXPORT double f15_D_DSS_ID(double p0, struct S_ID p1, struct S_ID p2) { return p0; }
EXPORT double f15_D_DSS_IP(double p0, struct S_IP p1, struct S_IP p2) { return p0; }
EXPORT double f15_D_DSS_FI(double p0, struct S_FI p1, struct S_FI p2) { return p0; }
EXPORT double f15_D_DSS_FF(double p0, struct S_FF p1, struct S_FF p2) { return p0; }
EXPORT double f15_D_DSS_FD(double p0, struct S_FD p1, struct S_FD p2) { return p0; }
EXPORT double f15_D_DSS_FP(double p0, struct S_FP p1, struct S_FP p2) { return p0; }
EXPORT double f15_D_DSS_DI(double p0, struct S_DI p1, struct S_DI p2) { return p0; }
EXPORT double f15_D_DSS_DF(double p0, struct S_DF p1, struct S_DF p2) { return p0; }
EXPORT double f15_D_DSS_DD(double p0, struct S_DD p1, struct S_DD p2) { return p0; }
EXPORT double f15_D_DSS_DP(double p0, struct S_DP p1, struct S_DP p2) { return p0; }
EXPORT double f15_D_DSS_PI(double p0, struct S_PI p1, struct S_PI p2) { return p0; }
EXPORT double f15_D_DSS_PF(double p0, struct S_PF p1, struct S_PF p2) { return p0; }
EXPORT double f15_D_DSS_PD(double p0, struct S_PD p1, struct S_PD p2) { return p0; }
EXPORT double f15_D_DSS_PP(double p0, struct S_PP p1, struct S_PP p2) { return p0; }
EXPORT double f15_D_DSS_III(double p0, struct S_III p1, struct S_III p2) { return p0; }
EXPORT double f15_D_DSS_IIF(double p0, struct S_IIF p1, struct S_IIF p2) { return p0; }
EXPORT double f15_D_DSS_IID(double p0, struct S_IID p1, struct S_IID p2) { return p0; }
EXPORT double f15_D_DSS_IIP(double p0, struct S_IIP p1, struct S_IIP p2) { return p0; }
EXPORT double f15_D_DSS_IFI(double p0, struct S_IFI p1, struct S_IFI p2) { return p0; }
EXPORT double f15_D_DSS_IFF(double p0, struct S_IFF p1, struct S_IFF p2) { return p0; }
EXPORT double f15_D_DSS_IFD(double p0, struct S_IFD p1, struct S_IFD p2) { return p0; }
EXPORT double f15_D_DSS_IFP(double p0, struct S_IFP p1, struct S_IFP p2) { return p0; }
EXPORT double f15_D_DSS_IDI(double p0, struct S_IDI p1, struct S_IDI p2) { return p0; }
EXPORT double f15_D_DSS_IDF(double p0, struct S_IDF p1, struct S_IDF p2) { return p0; }
EXPORT double f15_D_DSS_IDD(double p0, struct S_IDD p1, struct S_IDD p2) { return p0; }
EXPORT double f15_D_DSS_IDP(double p0, struct S_IDP p1, struct S_IDP p2) { return p0; }
EXPORT double f15_D_DSS_IPI(double p0, struct S_IPI p1, struct S_IPI p2) { return p0; }
EXPORT double f15_D_DSS_IPF(double p0, struct S_IPF p1, struct S_IPF p2) { return p0; }
EXPORT double f15_D_DSS_IPD(double p0, struct S_IPD p1, struct S_IPD p2) { return p0; }
EXPORT double f15_D_DSS_IPP(double p0, struct S_IPP p1, struct S_IPP p2) { return p0; }
EXPORT double f15_D_DSS_FII(double p0, struct S_FII p1, struct S_FII p2) { return p0; }
EXPORT double f15_D_DSS_FIF(double p0, struct S_FIF p1, struct S_FIF p2) { return p0; }
EXPORT double f15_D_DSS_FID(double p0, struct S_FID p1, struct S_FID p2) { return p0; }
EXPORT double f15_D_DSS_FIP(double p0, struct S_FIP p1, struct S_FIP p2) { return p0; }
EXPORT double f15_D_DSS_FFI(double p0, struct S_FFI p1, struct S_FFI p2) { return p0; }
EXPORT double f15_D_DSS_FFF(double p0, struct S_FFF p1, struct S_FFF p2) { return p0; }
EXPORT double f15_D_DSS_FFD(double p0, struct S_FFD p1, struct S_FFD p2) { return p0; }
EXPORT double f15_D_DSS_FFP(double p0, struct S_FFP p1, struct S_FFP p2) { return p0; }
EXPORT double f15_D_DSS_FDI(double p0, struct S_FDI p1, struct S_FDI p2) { return p0; }
EXPORT double f15_D_DSS_FDF(double p0, struct S_FDF p1, struct S_FDF p2) { return p0; }
EXPORT double f15_D_DSS_FDD(double p0, struct S_FDD p1, struct S_FDD p2) { return p0; }
EXPORT double f15_D_DSS_FDP(double p0, struct S_FDP p1, struct S_FDP p2) { return p0; }
EXPORT double f15_D_DSS_FPI(double p0, struct S_FPI p1, struct S_FPI p2) { return p0; }
EXPORT double f15_D_DSS_FPF(double p0, struct S_FPF p1, struct S_FPF p2) { return p0; }
EXPORT double f15_D_DSS_FPD(double p0, struct S_FPD p1, struct S_FPD p2) { return p0; }
EXPORT double f15_D_DSS_FPP(double p0, struct S_FPP p1, struct S_FPP p2) { return p0; }
EXPORT double f15_D_DSS_DII(double p0, struct S_DII p1, struct S_DII p2) { return p0; }
EXPORT double f15_D_DSS_DIF(double p0, struct S_DIF p1, struct S_DIF p2) { return p0; }
EXPORT double f15_D_DSS_DID(double p0, struct S_DID p1, struct S_DID p2) { return p0; }
EXPORT double f15_D_DSS_DIP(double p0, struct S_DIP p1, struct S_DIP p2) { return p0; }
EXPORT double f15_D_DSS_DFI(double p0, struct S_DFI p1, struct S_DFI p2) { return p0; }
EXPORT double f15_D_DSS_DFF(double p0, struct S_DFF p1, struct S_DFF p2) { return p0; }
EXPORT double f15_D_DSS_DFD(double p0, struct S_DFD p1, struct S_DFD p2) { return p0; }
EXPORT double f15_D_DSS_DFP(double p0, struct S_DFP p1, struct S_DFP p2) { return p0; }
EXPORT double f15_D_DSS_DDI(double p0, struct S_DDI p1, struct S_DDI p2) { return p0; }
EXPORT double f15_D_DSS_DDF(double p0, struct S_DDF p1, struct S_DDF p2) { return p0; }
EXPORT double f15_D_DSS_DDD(double p0, struct S_DDD p1, struct S_DDD p2) { return p0; }
EXPORT double f15_D_DSS_DDP(double p0, struct S_DDP p1, struct S_DDP p2) { return p0; }
EXPORT double f15_D_DSS_DPI(double p0, struct S_DPI p1, struct S_DPI p2) { return p0; }
EXPORT double f15_D_DSS_DPF(double p0, struct S_DPF p1, struct S_DPF p2) { return p0; }
EXPORT double f15_D_DSS_DPD(double p0, struct S_DPD p1, struct S_DPD p2) { return p0; }
EXPORT double f15_D_DSS_DPP(double p0, struct S_DPP p1, struct S_DPP p2) { return p0; }
EXPORT double f15_D_DSS_PII(double p0, struct S_PII p1, struct S_PII p2) { return p0; }
EXPORT double f15_D_DSS_PIF(double p0, struct S_PIF p1, struct S_PIF p2) { return p0; }
EXPORT double f15_D_DSS_PID(double p0, struct S_PID p1, struct S_PID p2) { return p0; }
EXPORT double f15_D_DSS_PIP(double p0, struct S_PIP p1, struct S_PIP p2) { return p0; }
EXPORT double f15_D_DSS_PFI(double p0, struct S_PFI p1, struct S_PFI p2) { return p0; }
EXPORT double f15_D_DSS_PFF(double p0, struct S_PFF p1, struct S_PFF p2) { return p0; }
EXPORT double f15_D_DSS_PFD(double p0, struct S_PFD p1, struct S_PFD p2) { return p0; }
EXPORT double f15_D_DSS_PFP(double p0, struct S_PFP p1, struct S_PFP p2) { return p0; }
EXPORT double f15_D_DSS_PDI(double p0, struct S_PDI p1, struct S_PDI p2) { return p0; }
EXPORT double f15_D_DSS_PDF(double p0, struct S_PDF p1, struct S_PDF p2) { return p0; }
EXPORT double f15_D_DSS_PDD(double p0, struct S_PDD p1, struct S_PDD p2) { return p0; }
EXPORT double f15_D_DSS_PDP(double p0, struct S_PDP p1, struct S_PDP p2) { return p0; }
EXPORT double f15_D_DSS_PPI(double p0, struct S_PPI p1, struct S_PPI p2) { return p0; }
EXPORT double f15_D_DSS_PPF(double p0, struct S_PPF p1, struct S_PPF p2) { return p0; }
EXPORT double f15_D_DSS_PPD(double p0, struct S_PPD p1, struct S_PPD p2) { return p0; }
EXPORT double f15_D_DSS_PPP(double p0, struct S_PPP p1, struct S_PPP p2) { return p0; }
EXPORT void* f15_P_PII_(void* p0, int p1, int p2) { return p0; }
EXPORT void* f15_P_PIF_(void* p0, int p1, float p2) { return p0; }
EXPORT void* f15_P_PID_(void* p0, int p1, double p2) { return p0; }
EXPORT void* f15_P_PIP_(void* p0, int p1, void* p2) { return p0; }
EXPORT void* f15_P_PIS_I(void* p0, int p1, struct S_I p2) { return p0; }
EXPORT void* f15_P_PIS_F(void* p0, int p1, struct S_F p2) { return p0; }
EXPORT void* f15_P_PIS_D(void* p0, int p1, struct S_D p2) { return p0; }
EXPORT void* f15_P_PIS_P(void* p0, int p1, struct S_P p2) { return p0; }
EXPORT void* f15_P_PIS_II(void* p0, int p1, struct S_II p2) { return p0; }
EXPORT void* f15_P_PIS_IF(void* p0, int p1, struct S_IF p2) { return p0; }
EXPORT void* f15_P_PIS_ID(void* p0, int p1, struct S_ID p2) { return p0; }
EXPORT void* f15_P_PIS_IP(void* p0, int p1, struct S_IP p2) { return p0; }
EXPORT void* f15_P_PIS_FI(void* p0, int p1, struct S_FI p2) { return p0; }
EXPORT void* f15_P_PIS_FF(void* p0, int p1, struct S_FF p2) { return p0; }
EXPORT void* f15_P_PIS_FD(void* p0, int p1, struct S_FD p2) { return p0; }
EXPORT void* f15_P_PIS_FP(void* p0, int p1, struct S_FP p2) { return p0; }
EXPORT void* f15_P_PIS_DI(void* p0, int p1, struct S_DI p2) { return p0; }
EXPORT void* f15_P_PIS_DF(void* p0, int p1, struct S_DF p2) { return p0; }
EXPORT void* f15_P_PIS_DD(void* p0, int p1, struct S_DD p2) { return p0; }
EXPORT void* f15_P_PIS_DP(void* p0, int p1, struct S_DP p2) { return p0; }
EXPORT void* f15_P_PIS_PI(void* p0, int p1, struct S_PI p2) { return p0; }
EXPORT void* f15_P_PIS_PF(void* p0, int p1, struct S_PF p2) { return p0; }
EXPORT void* f15_P_PIS_PD(void* p0, int p1, struct S_PD p2) { return p0; }
EXPORT void* f15_P_PIS_PP(void* p0, int p1, struct S_PP p2) { return p0; }
EXPORT void* f15_P_PIS_III(void* p0, int p1, struct S_III p2) { return p0; }
EXPORT void* f15_P_PIS_IIF(void* p0, int p1, struct S_IIF p2) { return p0; }
EXPORT void* f15_P_PIS_IID(void* p0, int p1, struct S_IID p2) { return p0; }
EXPORT void* f15_P_PIS_IIP(void* p0, int p1, struct S_IIP p2) { return p0; }
EXPORT void* f15_P_PIS_IFI(void* p0, int p1, struct S_IFI p2) { return p0; }
EXPORT void* f15_P_PIS_IFF(void* p0, int p1, struct S_IFF p2) { return p0; }
EXPORT void* f15_P_PIS_IFD(void* p0, int p1, struct S_IFD p2) { return p0; }
EXPORT void* f15_P_PIS_IFP(void* p0, int p1, struct S_IFP p2) { return p0; }
EXPORT void* f15_P_PIS_IDI(void* p0, int p1, struct S_IDI p2) { return p0; }
EXPORT void* f15_P_PIS_IDF(void* p0, int p1, struct S_IDF p2) { return p0; }
EXPORT void* f15_P_PIS_IDD(void* p0, int p1, struct S_IDD p2) { return p0; }
EXPORT void* f15_P_PIS_IDP(void* p0, int p1, struct S_IDP p2) { return p0; }
EXPORT void* f15_P_PIS_IPI(void* p0, int p1, struct S_IPI p2) { return p0; }
EXPORT void* f15_P_PIS_IPF(void* p0, int p1, struct S_IPF p2) { return p0; }
EXPORT void* f15_P_PIS_IPD(void* p0, int p1, struct S_IPD p2) { return p0; }
EXPORT void* f15_P_PIS_IPP(void* p0, int p1, struct S_IPP p2) { return p0; }
EXPORT void* f15_P_PIS_FII(void* p0, int p1, struct S_FII p2) { return p0; }
EXPORT void* f15_P_PIS_FIF(void* p0, int p1, struct S_FIF p2) { return p0; }
EXPORT void* f15_P_PIS_FID(void* p0, int p1, struct S_FID p2) { return p0; }
EXPORT void* f15_P_PIS_FIP(void* p0, int p1, struct S_FIP p2) { return p0; }
EXPORT void* f15_P_PIS_FFI(void* p0, int p1, struct S_FFI p2) { return p0; }
EXPORT void* f15_P_PIS_FFF(void* p0, int p1, struct S_FFF p2) { return p0; }
EXPORT void* f15_P_PIS_FFD(void* p0, int p1, struct S_FFD p2) { return p0; }
EXPORT void* f15_P_PIS_FFP(void* p0, int p1, struct S_FFP p2) { return p0; }
EXPORT void* f15_P_PIS_FDI(void* p0, int p1, struct S_FDI p2) { return p0; }
EXPORT void* f15_P_PIS_FDF(void* p0, int p1, struct S_FDF p2) { return p0; }
EXPORT void* f15_P_PIS_FDD(void* p0, int p1, struct S_FDD p2) { return p0; }
EXPORT void* f15_P_PIS_FDP(void* p0, int p1, struct S_FDP p2) { return p0; }
EXPORT void* f15_P_PIS_FPI(void* p0, int p1, struct S_FPI p2) { return p0; }
EXPORT void* f15_P_PIS_FPF(void* p0, int p1, struct S_FPF p2) { return p0; }
EXPORT void* f15_P_PIS_FPD(void* p0, int p1, struct S_FPD p2) { return p0; }
EXPORT void* f15_P_PIS_FPP(void* p0, int p1, struct S_FPP p2) { return p0; }
EXPORT void* f15_P_PIS_DII(void* p0, int p1, struct S_DII p2) { return p0; }
EXPORT void* f15_P_PIS_DIF(void* p0, int p1, struct S_DIF p2) { return p0; }
EXPORT void* f15_P_PIS_DID(void* p0, int p1, struct S_DID p2) { return p0; }
EXPORT void* f15_P_PIS_DIP(void* p0, int p1, struct S_DIP p2) { return p0; }
EXPORT void* f15_P_PIS_DFI(void* p0, int p1, struct S_DFI p2) { return p0; }
EXPORT void* f15_P_PIS_DFF(void* p0, int p1, struct S_DFF p2) { return p0; }
EXPORT void* f15_P_PIS_DFD(void* p0, int p1, struct S_DFD p2) { return p0; }
EXPORT void* f15_P_PIS_DFP(void* p0, int p1, struct S_DFP p2) { return p0; }
EXPORT void* f15_P_PIS_DDI(void* p0, int p1, struct S_DDI p2) { return p0; }
EXPORT void* f15_P_PIS_DDF(void* p0, int p1, struct S_DDF p2) { return p0; }
EXPORT void* f15_P_PIS_DDD(void* p0, int p1, struct S_DDD p2) { return p0; }
EXPORT void* f15_P_PIS_DDP(void* p0, int p1, struct S_DDP p2) { return p0; }
EXPORT void* f15_P_PIS_DPI(void* p0, int p1, struct S_DPI p2) { return p0; }
EXPORT void* f15_P_PIS_DPF(void* p0, int p1, struct S_DPF p2) { return p0; }
EXPORT void* f15_P_PIS_DPD(void* p0, int p1, struct S_DPD p2) { return p0; }
EXPORT void* f15_P_PIS_DPP(void* p0, int p1, struct S_DPP p2) { return p0; }
EXPORT void* f15_P_PIS_PII(void* p0, int p1, struct S_PII p2) { return p0; }
EXPORT void* f15_P_PIS_PIF(void* p0, int p1, struct S_PIF p2) { return p0; }
EXPORT void* f15_P_PIS_PID(void* p0, int p1, struct S_PID p2) { return p0; }
EXPORT void* f15_P_PIS_PIP(void* p0, int p1, struct S_PIP p2) { return p0; }
EXPORT void* f15_P_PIS_PFI(void* p0, int p1, struct S_PFI p2) { return p0; }
EXPORT void* f15_P_PIS_PFF(void* p0, int p1, struct S_PFF p2) { return p0; }
EXPORT void* f15_P_PIS_PFD(void* p0, int p1, struct S_PFD p2) { return p0; }
EXPORT void* f15_P_PIS_PFP(void* p0, int p1, struct S_PFP p2) { return p0; }
EXPORT void* f15_P_PIS_PDI(void* p0, int p1, struct S_PDI p2) { return p0; }
EXPORT void* f15_P_PIS_PDF(void* p0, int p1, struct S_PDF p2) { return p0; }
EXPORT void* f15_P_PIS_PDD(void* p0, int p1, struct S_PDD p2) { return p0; }
EXPORT void* f15_P_PIS_PDP(void* p0, int p1, struct S_PDP p2) { return p0; }
EXPORT void* f15_P_PIS_PPI(void* p0, int p1, struct S_PPI p2) { return p0; }
EXPORT void* f15_P_PIS_PPF(void* p0, int p1, struct S_PPF p2) { return p0; }
EXPORT void* f15_P_PIS_PPD(void* p0, int p1, struct S_PPD p2) { return p0; }
EXPORT void* f15_P_PIS_PPP(void* p0, int p1, struct S_PPP p2) { return p0; }
EXPORT void* f15_P_PFI_(void* p0, float p1, int p2) { return p0; }
EXPORT void* f15_P_PFF_(void* p0, float p1, float p2) { return p0; }
EXPORT void* f15_P_PFD_(void* p0, float p1, double p2) { return p0; }
EXPORT void* f15_P_PFP_(void* p0, float p1, void* p2) { return p0; }
EXPORT void* f15_P_PFS_I(void* p0, float p1, struct S_I p2) { return p0; }
EXPORT void* f15_P_PFS_F(void* p0, float p1, struct S_F p2) { return p0; }
EXPORT void* f15_P_PFS_D(void* p0, float p1, struct S_D p2) { return p0; }
EXPORT void* f15_P_PFS_P(void* p0, float p1, struct S_P p2) { return p0; }
EXPORT void* f15_P_PFS_II(void* p0, float p1, struct S_II p2) { return p0; }
EXPORT void* f15_P_PFS_IF(void* p0, float p1, struct S_IF p2) { return p0; }
EXPORT void* f15_P_PFS_ID(void* p0, float p1, struct S_ID p2) { return p0; }
EXPORT void* f15_P_PFS_IP(void* p0, float p1, struct S_IP p2) { return p0; }
EXPORT void* f15_P_PFS_FI(void* p0, float p1, struct S_FI p2) { return p0; }
EXPORT void* f15_P_PFS_FF(void* p0, float p1, struct S_FF p2) { return p0; }
EXPORT void* f15_P_PFS_FD(void* p0, float p1, struct S_FD p2) { return p0; }
EXPORT void* f15_P_PFS_FP(void* p0, float p1, struct S_FP p2) { return p0; }
EXPORT void* f15_P_PFS_DI(void* p0, float p1, struct S_DI p2) { return p0; }
EXPORT void* f15_P_PFS_DF(void* p0, float p1, struct S_DF p2) { return p0; }
EXPORT void* f15_P_PFS_DD(void* p0, float p1, struct S_DD p2) { return p0; }
EXPORT void* f15_P_PFS_DP(void* p0, float p1, struct S_DP p2) { return p0; }
EXPORT void* f15_P_PFS_PI(void* p0, float p1, struct S_PI p2) { return p0; }
EXPORT void* f15_P_PFS_PF(void* p0, float p1, struct S_PF p2) { return p0; }
EXPORT void* f15_P_PFS_PD(void* p0, float p1, struct S_PD p2) { return p0; }
EXPORT void* f15_P_PFS_PP(void* p0, float p1, struct S_PP p2) { return p0; }
EXPORT void* f15_P_PFS_III(void* p0, float p1, struct S_III p2) { return p0; }
EXPORT void* f15_P_PFS_IIF(void* p0, float p1, struct S_IIF p2) { return p0; }
EXPORT void* f15_P_PFS_IID(void* p0, float p1, struct S_IID p2) { return p0; }
EXPORT void* f15_P_PFS_IIP(void* p0, float p1, struct S_IIP p2) { return p0; }
EXPORT void* f15_P_PFS_IFI(void* p0, float p1, struct S_IFI p2) { return p0; }
EXPORT void* f15_P_PFS_IFF(void* p0, float p1, struct S_IFF p2) { return p0; }
EXPORT void* f15_P_PFS_IFD(void* p0, float p1, struct S_IFD p2) { return p0; }
EXPORT void* f15_P_PFS_IFP(void* p0, float p1, struct S_IFP p2) { return p0; }
EXPORT void* f15_P_PFS_IDI(void* p0, float p1, struct S_IDI p2) { return p0; }
EXPORT void* f15_P_PFS_IDF(void* p0, float p1, struct S_IDF p2) { return p0; }
EXPORT void* f15_P_PFS_IDD(void* p0, float p1, struct S_IDD p2) { return p0; }
EXPORT void* f15_P_PFS_IDP(void* p0, float p1, struct S_IDP p2) { return p0; }
EXPORT void* f15_P_PFS_IPI(void* p0, float p1, struct S_IPI p2) { return p0; }
EXPORT void* f15_P_PFS_IPF(void* p0, float p1, struct S_IPF p2) { return p0; }
EXPORT void* f15_P_PFS_IPD(void* p0, float p1, struct S_IPD p2) { return p0; }
EXPORT void* f15_P_PFS_IPP(void* p0, float p1, struct S_IPP p2) { return p0; }
EXPORT void* f15_P_PFS_FII(void* p0, float p1, struct S_FII p2) { return p0; }
EXPORT void* f15_P_PFS_FIF(void* p0, float p1, struct S_FIF p2) { return p0; }
EXPORT void* f15_P_PFS_FID(void* p0, float p1, struct S_FID p2) { return p0; }
EXPORT void* f15_P_PFS_FIP(void* p0, float p1, struct S_FIP p2) { return p0; }
EXPORT void* f15_P_PFS_FFI(void* p0, float p1, struct S_FFI p2) { return p0; }
EXPORT void* f15_P_PFS_FFF(void* p0, float p1, struct S_FFF p2) { return p0; }
EXPORT void* f15_P_PFS_FFD(void* p0, float p1, struct S_FFD p2) { return p0; }
EXPORT void* f15_P_PFS_FFP(void* p0, float p1, struct S_FFP p2) { return p0; }
EXPORT void* f15_P_PFS_FDI(void* p0, float p1, struct S_FDI p2) { return p0; }
EXPORT void* f15_P_PFS_FDF(void* p0, float p1, struct S_FDF p2) { return p0; }
EXPORT void* f15_P_PFS_FDD(void* p0, float p1, struct S_FDD p2) { return p0; }
EXPORT void* f15_P_PFS_FDP(void* p0, float p1, struct S_FDP p2) { return p0; }
EXPORT void* f15_P_PFS_FPI(void* p0, float p1, struct S_FPI p2) { return p0; }
EXPORT void* f15_P_PFS_FPF(void* p0, float p1, struct S_FPF p2) { return p0; }
EXPORT void* f15_P_PFS_FPD(void* p0, float p1, struct S_FPD p2) { return p0; }
EXPORT void* f15_P_PFS_FPP(void* p0, float p1, struct S_FPP p2) { return p0; }
EXPORT void* f15_P_PFS_DII(void* p0, float p1, struct S_DII p2) { return p0; }
EXPORT void* f15_P_PFS_DIF(void* p0, float p1, struct S_DIF p2) { return p0; }
EXPORT void* f15_P_PFS_DID(void* p0, float p1, struct S_DID p2) { return p0; }
EXPORT void* f15_P_PFS_DIP(void* p0, float p1, struct S_DIP p2) { return p0; }
EXPORT void* f15_P_PFS_DFI(void* p0, float p1, struct S_DFI p2) { return p0; }
EXPORT void* f15_P_PFS_DFF(void* p0, float p1, struct S_DFF p2) { return p0; }
EXPORT void* f15_P_PFS_DFD(void* p0, float p1, struct S_DFD p2) { return p0; }
EXPORT void* f15_P_PFS_DFP(void* p0, float p1, struct S_DFP p2) { return p0; }
EXPORT void* f15_P_PFS_DDI(void* p0, float p1, struct S_DDI p2) { return p0; }
EXPORT void* f15_P_PFS_DDF(void* p0, float p1, struct S_DDF p2) { return p0; }
EXPORT void* f15_P_PFS_DDD(void* p0, float p1, struct S_DDD p2) { return p0; }
EXPORT void* f15_P_PFS_DDP(void* p0, float p1, struct S_DDP p2) { return p0; }
EXPORT void* f15_P_PFS_DPI(void* p0, float p1, struct S_DPI p2) { return p0; }
EXPORT void* f15_P_PFS_DPF(void* p0, float p1, struct S_DPF p2) { return p0; }
EXPORT void* f15_P_PFS_DPD(void* p0, float p1, struct S_DPD p2) { return p0; }
EXPORT void* f15_P_PFS_DPP(void* p0, float p1, struct S_DPP p2) { return p0; }
EXPORT void* f15_P_PFS_PII(void* p0, float p1, struct S_PII p2) { return p0; }
EXPORT void* f15_P_PFS_PIF(void* p0, float p1, struct S_PIF p2) { return p0; }
EXPORT void* f15_P_PFS_PID(void* p0, float p1, struct S_PID p2) { return p0; }
EXPORT void* f15_P_PFS_PIP(void* p0, float p1, struct S_PIP p2) { return p0; }
EXPORT void* f15_P_PFS_PFI(void* p0, float p1, struct S_PFI p2) { return p0; }
EXPORT void* f15_P_PFS_PFF(void* p0, float p1, struct S_PFF p2) { return p0; }
EXPORT void* f15_P_PFS_PFD(void* p0, float p1, struct S_PFD p2) { return p0; }
EXPORT void* f15_P_PFS_PFP(void* p0, float p1, struct S_PFP p2) { return p0; }
EXPORT void* f15_P_PFS_PDI(void* p0, float p1, struct S_PDI p2) { return p0; }
EXPORT void* f15_P_PFS_PDF(void* p0, float p1, struct S_PDF p2) { return p0; }
EXPORT void* f15_P_PFS_PDD(void* p0, float p1, struct S_PDD p2) { return p0; }
EXPORT void* f15_P_PFS_PDP(void* p0, float p1, struct S_PDP p2) { return p0; }
EXPORT void* f15_P_PFS_PPI(void* p0, float p1, struct S_PPI p2) { return p0; }
EXPORT void* f15_P_PFS_PPF(void* p0, float p1, struct S_PPF p2) { return p0; }
EXPORT void* f15_P_PFS_PPD(void* p0, float p1, struct S_PPD p2) { return p0; }
EXPORT void* f15_P_PFS_PPP(void* p0, float p1, struct S_PPP p2) { return p0; }
EXPORT void* f15_P_PDI_(void* p0, double p1, int p2) { return p0; }
EXPORT void* f15_P_PDF_(void* p0, double p1, float p2) { return p0; }
EXPORT void* f15_P_PDD_(void* p0, double p1, double p2) { return p0; }
EXPORT void* f15_P_PDP_(void* p0, double p1, void* p2) { return p0; }
EXPORT void* f15_P_PDS_I(void* p0, double p1, struct S_I p2) { return p0; }
EXPORT void* f15_P_PDS_F(void* p0, double p1, struct S_F p2) { return p0; }
EXPORT void* f15_P_PDS_D(void* p0, double p1, struct S_D p2) { return p0; }
EXPORT void* f15_P_PDS_P(void* p0, double p1, struct S_P p2) { return p0; }
EXPORT void* f15_P_PDS_II(void* p0, double p1, struct S_II p2) { return p0; }
EXPORT void* f15_P_PDS_IF(void* p0, double p1, struct S_IF p2) { return p0; }
EXPORT void* f15_P_PDS_ID(void* p0, double p1, struct S_ID p2) { return p0; }
EXPORT void* f15_P_PDS_IP(void* p0, double p1, struct S_IP p2) { return p0; }
EXPORT void* f15_P_PDS_FI(void* p0, double p1, struct S_FI p2) { return p0; }
EXPORT void* f15_P_PDS_FF(void* p0, double p1, struct S_FF p2) { return p0; }
EXPORT void* f15_P_PDS_FD(void* p0, double p1, struct S_FD p2) { return p0; }
EXPORT void* f15_P_PDS_FP(void* p0, double p1, struct S_FP p2) { return p0; }
EXPORT void* f15_P_PDS_DI(void* p0, double p1, struct S_DI p2) { return p0; }
EXPORT void* f15_P_PDS_DF(void* p0, double p1, struct S_DF p2) { return p0; }
EXPORT void* f15_P_PDS_DD(void* p0, double p1, struct S_DD p2) { return p0; }
EXPORT void* f15_P_PDS_DP(void* p0, double p1, struct S_DP p2) { return p0; }
EXPORT void* f15_P_PDS_PI(void* p0, double p1, struct S_PI p2) { return p0; }
EXPORT void* f15_P_PDS_PF(void* p0, double p1, struct S_PF p2) { return p0; }
EXPORT void* f15_P_PDS_PD(void* p0, double p1, struct S_PD p2) { return p0; }
EXPORT void* f15_P_PDS_PP(void* p0, double p1, struct S_PP p2) { return p0; }
EXPORT void* f15_P_PDS_III(void* p0, double p1, struct S_III p2) { return p0; }
EXPORT void* f15_P_PDS_IIF(void* p0, double p1, struct S_IIF p2) { return p0; }
EXPORT void* f15_P_PDS_IID(void* p0, double p1, struct S_IID p2) { return p0; }
EXPORT void* f15_P_PDS_IIP(void* p0, double p1, struct S_IIP p2) { return p0; }
EXPORT void* f15_P_PDS_IFI(void* p0, double p1, struct S_IFI p2) { return p0; }
EXPORT void* f15_P_PDS_IFF(void* p0, double p1, struct S_IFF p2) { return p0; }
EXPORT void* f15_P_PDS_IFD(void* p0, double p1, struct S_IFD p2) { return p0; }
EXPORT void* f15_P_PDS_IFP(void* p0, double p1, struct S_IFP p2) { return p0; }
EXPORT void* f15_P_PDS_IDI(void* p0, double p1, struct S_IDI p2) { return p0; }
EXPORT void* f15_P_PDS_IDF(void* p0, double p1, struct S_IDF p2) { return p0; }
EXPORT void* f15_P_PDS_IDD(void* p0, double p1, struct S_IDD p2) { return p0; }
EXPORT void* f15_P_PDS_IDP(void* p0, double p1, struct S_IDP p2) { return p0; }
EXPORT void* f15_P_PDS_IPI(void* p0, double p1, struct S_IPI p2) { return p0; }
EXPORT void* f15_P_PDS_IPF(void* p0, double p1, struct S_IPF p2) { return p0; }
EXPORT void* f15_P_PDS_IPD(void* p0, double p1, struct S_IPD p2) { return p0; }
EXPORT void* f15_P_PDS_IPP(void* p0, double p1, struct S_IPP p2) { return p0; }
EXPORT void* f15_P_PDS_FII(void* p0, double p1, struct S_FII p2) { return p0; }
EXPORT void* f15_P_PDS_FIF(void* p0, double p1, struct S_FIF p2) { return p0; }
EXPORT void* f15_P_PDS_FID(void* p0, double p1, struct S_FID p2) { return p0; }
EXPORT void* f15_P_PDS_FIP(void* p0, double p1, struct S_FIP p2) { return p0; }
EXPORT void* f15_P_PDS_FFI(void* p0, double p1, struct S_FFI p2) { return p0; }
EXPORT void* f15_P_PDS_FFF(void* p0, double p1, struct S_FFF p2) { return p0; }
EXPORT void* f15_P_PDS_FFD(void* p0, double p1, struct S_FFD p2) { return p0; }
EXPORT void* f15_P_PDS_FFP(void* p0, double p1, struct S_FFP p2) { return p0; }
EXPORT void* f15_P_PDS_FDI(void* p0, double p1, struct S_FDI p2) { return p0; }
EXPORT void* f15_P_PDS_FDF(void* p0, double p1, struct S_FDF p2) { return p0; }
EXPORT void* f15_P_PDS_FDD(void* p0, double p1, struct S_FDD p2) { return p0; }
EXPORT void* f15_P_PDS_FDP(void* p0, double p1, struct S_FDP p2) { return p0; }
EXPORT void* f15_P_PDS_FPI(void* p0, double p1, struct S_FPI p2) { return p0; }
EXPORT void* f15_P_PDS_FPF(void* p0, double p1, struct S_FPF p2) { return p0; }
EXPORT void* f15_P_PDS_FPD(void* p0, double p1, struct S_FPD p2) { return p0; }
EXPORT void* f15_P_PDS_FPP(void* p0, double p1, struct S_FPP p2) { return p0; }
EXPORT void* f15_P_PDS_DII(void* p0, double p1, struct S_DII p2) { return p0; }
EXPORT void* f15_P_PDS_DIF(void* p0, double p1, struct S_DIF p2) { return p0; }
EXPORT void* f15_P_PDS_DID(void* p0, double p1, struct S_DID p2) { return p0; }
EXPORT void* f15_P_PDS_DIP(void* p0, double p1, struct S_DIP p2) { return p0; }
EXPORT void* f15_P_PDS_DFI(void* p0, double p1, struct S_DFI p2) { return p0; }
EXPORT void* f15_P_PDS_DFF(void* p0, double p1, struct S_DFF p2) { return p0; }
EXPORT void* f15_P_PDS_DFD(void* p0, double p1, struct S_DFD p2) { return p0; }
EXPORT void* f15_P_PDS_DFP(void* p0, double p1, struct S_DFP p2) { return p0; }
EXPORT void* f15_P_PDS_DDI(void* p0, double p1, struct S_DDI p2) { return p0; }
EXPORT void* f15_P_PDS_DDF(void* p0, double p1, struct S_DDF p2) { return p0; }
EXPORT void* f15_P_PDS_DDD(void* p0, double p1, struct S_DDD p2) { return p0; }
EXPORT void* f15_P_PDS_DDP(void* p0, double p1, struct S_DDP p2) { return p0; }
EXPORT void* f15_P_PDS_DPI(void* p0, double p1, struct S_DPI p2) { return p0; }
EXPORT void* f15_P_PDS_DPF(void* p0, double p1, struct S_DPF p2) { return p0; }
EXPORT void* f15_P_PDS_DPD(void* p0, double p1, struct S_DPD p2) { return p0; }
EXPORT void* f15_P_PDS_DPP(void* p0, double p1, struct S_DPP p2) { return p0; }
EXPORT void* f15_P_PDS_PII(void* p0, double p1, struct S_PII p2) { return p0; }
EXPORT void* f15_P_PDS_PIF(void* p0, double p1, struct S_PIF p2) { return p0; }
EXPORT void* f15_P_PDS_PID(void* p0, double p1, struct S_PID p2) { return p0; }
EXPORT void* f15_P_PDS_PIP(void* p0, double p1, struct S_PIP p2) { return p0; }
EXPORT void* f15_P_PDS_PFI(void* p0, double p1, struct S_PFI p2) { return p0; }
EXPORT void* f15_P_PDS_PFF(void* p0, double p1, struct S_PFF p2) { return p0; }
EXPORT void* f15_P_PDS_PFD(void* p0, double p1, struct S_PFD p2) { return p0; }
EXPORT void* f15_P_PDS_PFP(void* p0, double p1, struct S_PFP p2) { return p0; }
EXPORT void* f15_P_PDS_PDI(void* p0, double p1, struct S_PDI p2) { return p0; }
EXPORT void* f15_P_PDS_PDF(void* p0, double p1, struct S_PDF p2) { return p0; }
EXPORT void* f15_P_PDS_PDD(void* p0, double p1, struct S_PDD p2) { return p0; }
EXPORT void* f15_P_PDS_PDP(void* p0, double p1, struct S_PDP p2) { return p0; }
EXPORT void* f15_P_PDS_PPI(void* p0, double p1, struct S_PPI p2) { return p0; }
EXPORT void* f15_P_PDS_PPF(void* p0, double p1, struct S_PPF p2) { return p0; }
EXPORT void* f15_P_PDS_PPD(void* p0, double p1, struct S_PPD p2) { return p0; }
EXPORT void* f15_P_PDS_PPP(void* p0, double p1, struct S_PPP p2) { return p0; }
EXPORT void* f15_P_PPI_(void* p0, void* p1, int p2) { return p0; }
EXPORT void* f15_P_PPF_(void* p0, void* p1, float p2) { return p0; }
EXPORT void* f15_P_PPD_(void* p0, void* p1, double p2) { return p0; }
EXPORT void* f15_P_PPP_(void* p0, void* p1, void* p2) { return p0; }
EXPORT void* f15_P_PPS_I(void* p0, void* p1, struct S_I p2) { return p0; }
EXPORT void* f15_P_PPS_F(void* p0, void* p1, struct S_F p2) { return p0; }
EXPORT void* f15_P_PPS_D(void* p0, void* p1, struct S_D p2) { return p0; }
EXPORT void* f15_P_PPS_P(void* p0, void* p1, struct S_P p2) { return p0; }
EXPORT void* f15_P_PPS_II(void* p0, void* p1, struct S_II p2) { return p0; }
EXPORT void* f15_P_PPS_IF(void* p0, void* p1, struct S_IF p2) { return p0; }
EXPORT void* f15_P_PPS_ID(void* p0, void* p1, struct S_ID p2) { return p0; }
EXPORT void* f15_P_PPS_IP(void* p0, void* p1, struct S_IP p2) { return p0; }
EXPORT void* f15_P_PPS_FI(void* p0, void* p1, struct S_FI p2) { return p0; }
EXPORT void* f15_P_PPS_FF(void* p0, void* p1, struct S_FF p2) { return p0; }
EXPORT void* f15_P_PPS_FD(void* p0, void* p1, struct S_FD p2) { return p0; }
EXPORT void* f15_P_PPS_FP(void* p0, void* p1, struct S_FP p2) { return p0; }
EXPORT void* f15_P_PPS_DI(void* p0, void* p1, struct S_DI p2) { return p0; }
EXPORT void* f15_P_PPS_DF(void* p0, void* p1, struct S_DF p2) { return p0; }
EXPORT void* f15_P_PPS_DD(void* p0, void* p1, struct S_DD p2) { return p0; }
EXPORT void* f15_P_PPS_DP(void* p0, void* p1, struct S_DP p2) { return p0; }
EXPORT void* f15_P_PPS_PI(void* p0, void* p1, struct S_PI p2) { return p0; }
EXPORT void* f15_P_PPS_PF(void* p0, void* p1, struct S_PF p2) { return p0; }
EXPORT void* f15_P_PPS_PD(void* p0, void* p1, struct S_PD p2) { return p0; }
EXPORT void* f15_P_PPS_PP(void* p0, void* p1, struct S_PP p2) { return p0; }
EXPORT void* f15_P_PPS_III(void* p0, void* p1, struct S_III p2) { return p0; }
EXPORT void* f15_P_PPS_IIF(void* p0, void* p1, struct S_IIF p2) { return p0; }
EXPORT void* f15_P_PPS_IID(void* p0, void* p1, struct S_IID p2) { return p0; }
EXPORT void* f15_P_PPS_IIP(void* p0, void* p1, struct S_IIP p2) { return p0; }
EXPORT void* f15_P_PPS_IFI(void* p0, void* p1, struct S_IFI p2) { return p0; }
EXPORT void* f15_P_PPS_IFF(void* p0, void* p1, struct S_IFF p2) { return p0; }
EXPORT void* f15_P_PPS_IFD(void* p0, void* p1, struct S_IFD p2) { return p0; }
EXPORT void* f15_P_PPS_IFP(void* p0, void* p1, struct S_IFP p2) { return p0; }
EXPORT void* f15_P_PPS_IDI(void* p0, void* p1, struct S_IDI p2) { return p0; }
EXPORT void* f15_P_PPS_IDF(void* p0, void* p1, struct S_IDF p2) { return p0; }
EXPORT void* f15_P_PPS_IDD(void* p0, void* p1, struct S_IDD p2) { return p0; }
EXPORT void* f15_P_PPS_IDP(void* p0, void* p1, struct S_IDP p2) { return p0; }
EXPORT void* f15_P_PPS_IPI(void* p0, void* p1, struct S_IPI p2) { return p0; }
EXPORT void* f15_P_PPS_IPF(void* p0, void* p1, struct S_IPF p2) { return p0; }
EXPORT void* f15_P_PPS_IPD(void* p0, void* p1, struct S_IPD p2) { return p0; }
EXPORT void* f15_P_PPS_IPP(void* p0, void* p1, struct S_IPP p2) { return p0; }
EXPORT void* f15_P_PPS_FII(void* p0, void* p1, struct S_FII p2) { return p0; }
EXPORT void* f15_P_PPS_FIF(void* p0, void* p1, struct S_FIF p2) { return p0; }
EXPORT void* f15_P_PPS_FID(void* p0, void* p1, struct S_FID p2) { return p0; }
EXPORT void* f15_P_PPS_FIP(void* p0, void* p1, struct S_FIP p2) { return p0; }
EXPORT void* f15_P_PPS_FFI(void* p0, void* p1, struct S_FFI p2) { return p0; }
EXPORT void* f15_P_PPS_FFF(void* p0, void* p1, struct S_FFF p2) { return p0; }
EXPORT void* f15_P_PPS_FFD(void* p0, void* p1, struct S_FFD p2) { return p0; }
EXPORT void* f15_P_PPS_FFP(void* p0, void* p1, struct S_FFP p2) { return p0; }
EXPORT void* f15_P_PPS_FDI(void* p0, void* p1, struct S_FDI p2) { return p0; }
EXPORT void* f15_P_PPS_FDF(void* p0, void* p1, struct S_FDF p2) { return p0; }
EXPORT void* f15_P_PPS_FDD(void* p0, void* p1, struct S_FDD p2) { return p0; }
EXPORT void* f15_P_PPS_FDP(void* p0, void* p1, struct S_FDP p2) { return p0; }
EXPORT void* f15_P_PPS_FPI(void* p0, void* p1, struct S_FPI p2) { return p0; }
EXPORT void* f15_P_PPS_FPF(void* p0, void* p1, struct S_FPF p2) { return p0; }
EXPORT void* f15_P_PPS_FPD(void* p0, void* p1, struct S_FPD p2) { return p0; }
EXPORT void* f15_P_PPS_FPP(void* p0, void* p1, struct S_FPP p2) { return p0; }
EXPORT void* f15_P_PPS_DII(void* p0, void* p1, struct S_DII p2) { return p0; }
EXPORT void* f15_P_PPS_DIF(void* p0, void* p1, struct S_DIF p2) { return p0; }
EXPORT void* f15_P_PPS_DID(void* p0, void* p1, struct S_DID p2) { return p0; }
EXPORT void* f15_P_PPS_DIP(void* p0, void* p1, struct S_DIP p2) { return p0; }
EXPORT void* f15_P_PPS_DFI(void* p0, void* p1, struct S_DFI p2) { return p0; }
EXPORT void* f15_P_PPS_DFF(void* p0, void* p1, struct S_DFF p2) { return p0; }
EXPORT void* f15_P_PPS_DFD(void* p0, void* p1, struct S_DFD p2) { return p0; }
EXPORT void* f15_P_PPS_DFP(void* p0, void* p1, struct S_DFP p2) { return p0; }
EXPORT void* f15_P_PPS_DDI(void* p0, void* p1, struct S_DDI p2) { return p0; }
EXPORT void* f15_P_PPS_DDF(void* p0, void* p1, struct S_DDF p2) { return p0; }
EXPORT void* f15_P_PPS_DDD(void* p0, void* p1, struct S_DDD p2) { return p0; }
EXPORT void* f15_P_PPS_DDP(void* p0, void* p1, struct S_DDP p2) { return p0; }
EXPORT void* f15_P_PPS_DPI(void* p0, void* p1, struct S_DPI p2) { return p0; }
EXPORT void* f15_P_PPS_DPF(void* p0, void* p1, struct S_DPF p2) { return p0; }
EXPORT void* f15_P_PPS_DPD(void* p0, void* p1, struct S_DPD p2) { return p0; }
EXPORT void* f15_P_PPS_DPP(void* p0, void* p1, struct S_DPP p2) { return p0; }
EXPORT void* f15_P_PPS_PII(void* p0, void* p1, struct S_PII p2) { return p0; }
EXPORT void* f15_P_PPS_PIF(void* p0, void* p1, struct S_PIF p2) { return p0; }
EXPORT void* f15_P_PPS_PID(void* p0, void* p1, struct S_PID p2) { return p0; }
EXPORT void* f15_P_PPS_PIP(void* p0, void* p1, struct S_PIP p2) { return p0; }
EXPORT void* f15_P_PPS_PFI(void* p0, void* p1, struct S_PFI p2) { return p0; }
EXPORT void* f15_P_PPS_PFF(void* p0, void* p1, struct S_PFF p2) { return p0; }
EXPORT void* f15_P_PPS_PFD(void* p0, void* p1, struct S_PFD p2) { return p0; }
EXPORT void* f15_P_PPS_PFP(void* p0, void* p1, struct S_PFP p2) { return p0; }
EXPORT void* f15_P_PPS_PDI(void* p0, void* p1, struct S_PDI p2) { return p0; }
EXPORT void* f15_P_PPS_PDF(void* p0, void* p1, struct S_PDF p2) { return p0; }
EXPORT void* f15_P_PPS_PDD(void* p0, void* p1, struct S_PDD p2) { return p0; }
EXPORT void* f15_P_PPS_PDP(void* p0, void* p1, struct S_PDP p2) { return p0; }
EXPORT void* f15_P_PPS_PPI(void* p0, void* p1, struct S_PPI p2) { return p0; }
EXPORT void* f15_P_PPS_PPF(void* p0, void* p1, struct S_PPF p2) { return p0; }
EXPORT void* f15_P_PPS_PPD(void* p0, void* p1, struct S_PPD p2) { return p0; }
EXPORT void* f15_P_PPS_PPP(void* p0, void* p1, struct S_PPP p2) { return p0; }
EXPORT void* f15_P_PSI_I(void* p0, struct S_I p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_F(void* p0, struct S_F p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_D(void* p0, struct S_D p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_P(void* p0, struct S_P p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_II(void* p0, struct S_II p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_IF(void* p0, struct S_IF p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_ID(void* p0, struct S_ID p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_IP(void* p0, struct S_IP p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_FI(void* p0, struct S_FI p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_FF(void* p0, struct S_FF p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_FD(void* p0, struct S_FD p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_FP(void* p0, struct S_FP p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_DI(void* p0, struct S_DI p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_DF(void* p0, struct S_DF p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_DD(void* p0, struct S_DD p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_DP(void* p0, struct S_DP p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_PI(void* p0, struct S_PI p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_PF(void* p0, struct S_PF p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_PD(void* p0, struct S_PD p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_PP(void* p0, struct S_PP p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_III(void* p0, struct S_III p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_IIF(void* p0, struct S_IIF p1, int p2) { return p0; }
EXPORT void* f15_P_PSI_IID(void* p0, struct S_IID p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_IIP(void* p0, struct S_IIP p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_IFI(void* p0, struct S_IFI p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_IFF(void* p0, struct S_IFF p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_IFD(void* p0, struct S_IFD p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_IFP(void* p0, struct S_IFP p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_IDI(void* p0, struct S_IDI p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_IDF(void* p0, struct S_IDF p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_IDD(void* p0, struct S_IDD p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_IDP(void* p0, struct S_IDP p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_IPI(void* p0, struct S_IPI p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_IPF(void* p0, struct S_IPF p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_IPD(void* p0, struct S_IPD p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_IPP(void* p0, struct S_IPP p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_FII(void* p0, struct S_FII p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_FIF(void* p0, struct S_FIF p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_FID(void* p0, struct S_FID p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_FIP(void* p0, struct S_FIP p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_FFI(void* p0, struct S_FFI p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_FFF(void* p0, struct S_FFF p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_FFD(void* p0, struct S_FFD p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_FFP(void* p0, struct S_FFP p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_FDI(void* p0, struct S_FDI p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_FDF(void* p0, struct S_FDF p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_FDD(void* p0, struct S_FDD p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_FDP(void* p0, struct S_FDP p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_FPI(void* p0, struct S_FPI p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_FPF(void* p0, struct S_FPF p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_FPD(void* p0, struct S_FPD p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_FPP(void* p0, struct S_FPP p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_DII(void* p0, struct S_DII p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_DIF(void* p0, struct S_DIF p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_DID(void* p0, struct S_DID p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_DIP(void* p0, struct S_DIP p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_DFI(void* p0, struct S_DFI p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_DFF(void* p0, struct S_DFF p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_DFD(void* p0, struct S_DFD p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_DFP(void* p0, struct S_DFP p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_DDI(void* p0, struct S_DDI p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_DDF(void* p0, struct S_DDF p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_DDD(void* p0, struct S_DDD p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_DDP(void* p0, struct S_DDP p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_DPI(void* p0, struct S_DPI p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_DPF(void* p0, struct S_DPF p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_DPD(void* p0, struct S_DPD p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_DPP(void* p0, struct S_DPP p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_PII(void* p0, struct S_PII p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_PIF(void* p0, struct S_PIF p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_PID(void* p0, struct S_PID p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_PIP(void* p0, struct S_PIP p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_PFI(void* p0, struct S_PFI p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_PFF(void* p0, struct S_PFF p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_PFD(void* p0, struct S_PFD p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_PFP(void* p0, struct S_PFP p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_PDI(void* p0, struct S_PDI p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_PDF(void* p0, struct S_PDF p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_PDD(void* p0, struct S_PDD p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_PDP(void* p0, struct S_PDP p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_PPI(void* p0, struct S_PPI p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_PPF(void* p0, struct S_PPF p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_PPD(void* p0, struct S_PPD p1, int p2) { return p0; }
EXPORT void* f16_P_PSI_PPP(void* p0, struct S_PPP p1, int p2) { return p0; }
EXPORT void* f16_P_PSF_I(void* p0, struct S_I p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_F(void* p0, struct S_F p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_D(void* p0, struct S_D p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_P(void* p0, struct S_P p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_II(void* p0, struct S_II p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_IF(void* p0, struct S_IF p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_ID(void* p0, struct S_ID p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_IP(void* p0, struct S_IP p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_FI(void* p0, struct S_FI p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_FF(void* p0, struct S_FF p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_FD(void* p0, struct S_FD p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_FP(void* p0, struct S_FP p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_DI(void* p0, struct S_DI p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_DF(void* p0, struct S_DF p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_DD(void* p0, struct S_DD p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_DP(void* p0, struct S_DP p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_PI(void* p0, struct S_PI p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_PF(void* p0, struct S_PF p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_PD(void* p0, struct S_PD p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_PP(void* p0, struct S_PP p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_III(void* p0, struct S_III p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_IIF(void* p0, struct S_IIF p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_IID(void* p0, struct S_IID p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_IIP(void* p0, struct S_IIP p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_IFI(void* p0, struct S_IFI p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_IFF(void* p0, struct S_IFF p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_IFD(void* p0, struct S_IFD p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_IFP(void* p0, struct S_IFP p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_IDI(void* p0, struct S_IDI p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_IDF(void* p0, struct S_IDF p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_IDD(void* p0, struct S_IDD p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_IDP(void* p0, struct S_IDP p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_IPI(void* p0, struct S_IPI p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_IPF(void* p0, struct S_IPF p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_IPD(void* p0, struct S_IPD p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_IPP(void* p0, struct S_IPP p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_FII(void* p0, struct S_FII p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_FIF(void* p0, struct S_FIF p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_FID(void* p0, struct S_FID p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_FIP(void* p0, struct S_FIP p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_FFI(void* p0, struct S_FFI p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_FFF(void* p0, struct S_FFF p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_FFD(void* p0, struct S_FFD p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_FFP(void* p0, struct S_FFP p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_FDI(void* p0, struct S_FDI p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_FDF(void* p0, struct S_FDF p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_FDD(void* p0, struct S_FDD p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_FDP(void* p0, struct S_FDP p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_FPI(void* p0, struct S_FPI p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_FPF(void* p0, struct S_FPF p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_FPD(void* p0, struct S_FPD p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_FPP(void* p0, struct S_FPP p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_DII(void* p0, struct S_DII p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_DIF(void* p0, struct S_DIF p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_DID(void* p0, struct S_DID p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_DIP(void* p0, struct S_DIP p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_DFI(void* p0, struct S_DFI p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_DFF(void* p0, struct S_DFF p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_DFD(void* p0, struct S_DFD p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_DFP(void* p0, struct S_DFP p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_DDI(void* p0, struct S_DDI p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_DDF(void* p0, struct S_DDF p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_DDD(void* p0, struct S_DDD p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_DDP(void* p0, struct S_DDP p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_DPI(void* p0, struct S_DPI p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_DPF(void* p0, struct S_DPF p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_DPD(void* p0, struct S_DPD p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_DPP(void* p0, struct S_DPP p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_PII(void* p0, struct S_PII p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_PIF(void* p0, struct S_PIF p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_PID(void* p0, struct S_PID p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_PIP(void* p0, struct S_PIP p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_PFI(void* p0, struct S_PFI p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_PFF(void* p0, struct S_PFF p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_PFD(void* p0, struct S_PFD p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_PFP(void* p0, struct S_PFP p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_PDI(void* p0, struct S_PDI p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_PDF(void* p0, struct S_PDF p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_PDD(void* p0, struct S_PDD p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_PDP(void* p0, struct S_PDP p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_PPI(void* p0, struct S_PPI p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_PPF(void* p0, struct S_PPF p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_PPD(void* p0, struct S_PPD p1, float p2) { return p0; }
EXPORT void* f16_P_PSF_PPP(void* p0, struct S_PPP p1, float p2) { return p0; }
EXPORT void* f16_P_PSD_I(void* p0, struct S_I p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_F(void* p0, struct S_F p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_D(void* p0, struct S_D p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_P(void* p0, struct S_P p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_II(void* p0, struct S_II p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_IF(void* p0, struct S_IF p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_ID(void* p0, struct S_ID p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_IP(void* p0, struct S_IP p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_FI(void* p0, struct S_FI p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_FF(void* p0, struct S_FF p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_FD(void* p0, struct S_FD p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_FP(void* p0, struct S_FP p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_DI(void* p0, struct S_DI p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_DF(void* p0, struct S_DF p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_DD(void* p0, struct S_DD p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_DP(void* p0, struct S_DP p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_PI(void* p0, struct S_PI p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_PF(void* p0, struct S_PF p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_PD(void* p0, struct S_PD p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_PP(void* p0, struct S_PP p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_III(void* p0, struct S_III p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_IIF(void* p0, struct S_IIF p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_IID(void* p0, struct S_IID p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_IIP(void* p0, struct S_IIP p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_IFI(void* p0, struct S_IFI p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_IFF(void* p0, struct S_IFF p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_IFD(void* p0, struct S_IFD p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_IFP(void* p0, struct S_IFP p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_IDI(void* p0, struct S_IDI p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_IDF(void* p0, struct S_IDF p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_IDD(void* p0, struct S_IDD p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_IDP(void* p0, struct S_IDP p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_IPI(void* p0, struct S_IPI p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_IPF(void* p0, struct S_IPF p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_IPD(void* p0, struct S_IPD p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_IPP(void* p0, struct S_IPP p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_FII(void* p0, struct S_FII p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_FIF(void* p0, struct S_FIF p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_FID(void* p0, struct S_FID p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_FIP(void* p0, struct S_FIP p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_FFI(void* p0, struct S_FFI p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_FFF(void* p0, struct S_FFF p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_FFD(void* p0, struct S_FFD p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_FFP(void* p0, struct S_FFP p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_FDI(void* p0, struct S_FDI p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_FDF(void* p0, struct S_FDF p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_FDD(void* p0, struct S_FDD p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_FDP(void* p0, struct S_FDP p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_FPI(void* p0, struct S_FPI p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_FPF(void* p0, struct S_FPF p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_FPD(void* p0, struct S_FPD p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_FPP(void* p0, struct S_FPP p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_DII(void* p0, struct S_DII p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_DIF(void* p0, struct S_DIF p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_DID(void* p0, struct S_DID p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_DIP(void* p0, struct S_DIP p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_DFI(void* p0, struct S_DFI p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_DFF(void* p0, struct S_DFF p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_DFD(void* p0, struct S_DFD p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_DFP(void* p0, struct S_DFP p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_DDI(void* p0, struct S_DDI p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_DDF(void* p0, struct S_DDF p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_DDD(void* p0, struct S_DDD p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_DDP(void* p0, struct S_DDP p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_DPI(void* p0, struct S_DPI p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_DPF(void* p0, struct S_DPF p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_DPD(void* p0, struct S_DPD p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_DPP(void* p0, struct S_DPP p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_PII(void* p0, struct S_PII p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_PIF(void* p0, struct S_PIF p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_PID(void* p0, struct S_PID p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_PIP(void* p0, struct S_PIP p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_PFI(void* p0, struct S_PFI p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_PFF(void* p0, struct S_PFF p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_PFD(void* p0, struct S_PFD p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_PFP(void* p0, struct S_PFP p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_PDI(void* p0, struct S_PDI p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_PDF(void* p0, struct S_PDF p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_PDD(void* p0, struct S_PDD p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_PDP(void* p0, struct S_PDP p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_PPI(void* p0, struct S_PPI p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_PPF(void* p0, struct S_PPF p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_PPD(void* p0, struct S_PPD p1, double p2) { return p0; }
EXPORT void* f16_P_PSD_PPP(void* p0, struct S_PPP p1, double p2) { return p0; }
EXPORT void* f16_P_PSP_I(void* p0, struct S_I p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_F(void* p0, struct S_F p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_D(void* p0, struct S_D p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_P(void* p0, struct S_P p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_II(void* p0, struct S_II p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_IF(void* p0, struct S_IF p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_ID(void* p0, struct S_ID p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_IP(void* p0, struct S_IP p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_FI(void* p0, struct S_FI p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_FF(void* p0, struct S_FF p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_FD(void* p0, struct S_FD p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_FP(void* p0, struct S_FP p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_DI(void* p0, struct S_DI p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_DF(void* p0, struct S_DF p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_DD(void* p0, struct S_DD p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_DP(void* p0, struct S_DP p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_PI(void* p0, struct S_PI p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_PF(void* p0, struct S_PF p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_PD(void* p0, struct S_PD p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_PP(void* p0, struct S_PP p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_III(void* p0, struct S_III p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_IIF(void* p0, struct S_IIF p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_IID(void* p0, struct S_IID p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_IIP(void* p0, struct S_IIP p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_IFI(void* p0, struct S_IFI p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_IFF(void* p0, struct S_IFF p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_IFD(void* p0, struct S_IFD p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_IFP(void* p0, struct S_IFP p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_IDI(void* p0, struct S_IDI p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_IDF(void* p0, struct S_IDF p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_IDD(void* p0, struct S_IDD p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_IDP(void* p0, struct S_IDP p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_IPI(void* p0, struct S_IPI p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_IPF(void* p0, struct S_IPF p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_IPD(void* p0, struct S_IPD p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_IPP(void* p0, struct S_IPP p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_FII(void* p0, struct S_FII p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_FIF(void* p0, struct S_FIF p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_FID(void* p0, struct S_FID p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_FIP(void* p0, struct S_FIP p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_FFI(void* p0, struct S_FFI p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_FFF(void* p0, struct S_FFF p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_FFD(void* p0, struct S_FFD p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_FFP(void* p0, struct S_FFP p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_FDI(void* p0, struct S_FDI p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_FDF(void* p0, struct S_FDF p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_FDD(void* p0, struct S_FDD p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_FDP(void* p0, struct S_FDP p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_FPI(void* p0, struct S_FPI p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_FPF(void* p0, struct S_FPF p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_FPD(void* p0, struct S_FPD p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_FPP(void* p0, struct S_FPP p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_DII(void* p0, struct S_DII p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_DIF(void* p0, struct S_DIF p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_DID(void* p0, struct S_DID p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_DIP(void* p0, struct S_DIP p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_DFI(void* p0, struct S_DFI p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_DFF(void* p0, struct S_DFF p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_DFD(void* p0, struct S_DFD p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_DFP(void* p0, struct S_DFP p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_DDI(void* p0, struct S_DDI p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_DDF(void* p0, struct S_DDF p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_DDD(void* p0, struct S_DDD p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_DDP(void* p0, struct S_DDP p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_DPI(void* p0, struct S_DPI p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_DPF(void* p0, struct S_DPF p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_DPD(void* p0, struct S_DPD p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_DPP(void* p0, struct S_DPP p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_PII(void* p0, struct S_PII p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_PIF(void* p0, struct S_PIF p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_PID(void* p0, struct S_PID p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_PIP(void* p0, struct S_PIP p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_PFI(void* p0, struct S_PFI p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_PFF(void* p0, struct S_PFF p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_PFD(void* p0, struct S_PFD p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_PFP(void* p0, struct S_PFP p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_PDI(void* p0, struct S_PDI p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_PDF(void* p0, struct S_PDF p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_PDD(void* p0, struct S_PDD p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_PDP(void* p0, struct S_PDP p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_PPI(void* p0, struct S_PPI p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_PPF(void* p0, struct S_PPF p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_PPD(void* p0, struct S_PPD p1, void* p2) { return p0; }
EXPORT void* f16_P_PSP_PPP(void* p0, struct S_PPP p1, void* p2) { return p0; }
EXPORT void* f16_P_PSS_I(void* p0, struct S_I p1, struct S_I p2) { return p0; }
EXPORT void* f16_P_PSS_F(void* p0, struct S_F p1, struct S_F p2) { return p0; }
EXPORT void* f16_P_PSS_D(void* p0, struct S_D p1, struct S_D p2) { return p0; }
EXPORT void* f16_P_PSS_P(void* p0, struct S_P p1, struct S_P p2) { return p0; }
EXPORT void* f16_P_PSS_II(void* p0, struct S_II p1, struct S_II p2) { return p0; }
EXPORT void* f16_P_PSS_IF(void* p0, struct S_IF p1, struct S_IF p2) { return p0; }
EXPORT void* f16_P_PSS_ID(void* p0, struct S_ID p1, struct S_ID p2) { return p0; }
EXPORT void* f16_P_PSS_IP(void* p0, struct S_IP p1, struct S_IP p2) { return p0; }
EXPORT void* f16_P_PSS_FI(void* p0, struct S_FI p1, struct S_FI p2) { return p0; }
EXPORT void* f16_P_PSS_FF(void* p0, struct S_FF p1, struct S_FF p2) { return p0; }
EXPORT void* f16_P_PSS_FD(void* p0, struct S_FD p1, struct S_FD p2) { return p0; }
EXPORT void* f16_P_PSS_FP(void* p0, struct S_FP p1, struct S_FP p2) { return p0; }
EXPORT void* f16_P_PSS_DI(void* p0, struct S_DI p1, struct S_DI p2) { return p0; }
EXPORT void* f16_P_PSS_DF(void* p0, struct S_DF p1, struct S_DF p2) { return p0; }
EXPORT void* f16_P_PSS_DD(void* p0, struct S_DD p1, struct S_DD p2) { return p0; }
EXPORT void* f16_P_PSS_DP(void* p0, struct S_DP p1, struct S_DP p2) { return p0; }
EXPORT void* f16_P_PSS_PI(void* p0, struct S_PI p1, struct S_PI p2) { return p0; }
EXPORT void* f16_P_PSS_PF(void* p0, struct S_PF p1, struct S_PF p2) { return p0; }
EXPORT void* f16_P_PSS_PD(void* p0, struct S_PD p1, struct S_PD p2) { return p0; }
EXPORT void* f16_P_PSS_PP(void* p0, struct S_PP p1, struct S_PP p2) { return p0; }
EXPORT void* f16_P_PSS_III(void* p0, struct S_III p1, struct S_III p2) { return p0; }
EXPORT void* f16_P_PSS_IIF(void* p0, struct S_IIF p1, struct S_IIF p2) { return p0; }
EXPORT void* f16_P_PSS_IID(void* p0, struct S_IID p1, struct S_IID p2) { return p0; }
EXPORT void* f16_P_PSS_IIP(void* p0, struct S_IIP p1, struct S_IIP p2) { return p0; }
EXPORT void* f16_P_PSS_IFI(void* p0, struct S_IFI p1, struct S_IFI p2) { return p0; }
EXPORT void* f16_P_PSS_IFF(void* p0, struct S_IFF p1, struct S_IFF p2) { return p0; }
EXPORT void* f16_P_PSS_IFD(void* p0, struct S_IFD p1, struct S_IFD p2) { return p0; }
EXPORT void* f16_P_PSS_IFP(void* p0, struct S_IFP p1, struct S_IFP p2) { return p0; }
EXPORT void* f16_P_PSS_IDI(void* p0, struct S_IDI p1, struct S_IDI p2) { return p0; }
EXPORT void* f16_P_PSS_IDF(void* p0, struct S_IDF p1, struct S_IDF p2) { return p0; }
EXPORT void* f16_P_PSS_IDD(void* p0, struct S_IDD p1, struct S_IDD p2) { return p0; }
EXPORT void* f16_P_PSS_IDP(void* p0, struct S_IDP p1, struct S_IDP p2) { return p0; }
EXPORT void* f16_P_PSS_IPI(void* p0, struct S_IPI p1, struct S_IPI p2) { return p0; }
EXPORT void* f16_P_PSS_IPF(void* p0, struct S_IPF p1, struct S_IPF p2) { return p0; }
EXPORT void* f16_P_PSS_IPD(void* p0, struct S_IPD p1, struct S_IPD p2) { return p0; }
EXPORT void* f16_P_PSS_IPP(void* p0, struct S_IPP p1, struct S_IPP p2) { return p0; }
EXPORT void* f16_P_PSS_FII(void* p0, struct S_FII p1, struct S_FII p2) { return p0; }
EXPORT void* f16_P_PSS_FIF(void* p0, struct S_FIF p1, struct S_FIF p2) { return p0; }
EXPORT void* f16_P_PSS_FID(void* p0, struct S_FID p1, struct S_FID p2) { return p0; }
EXPORT void* f16_P_PSS_FIP(void* p0, struct S_FIP p1, struct S_FIP p2) { return p0; }
EXPORT void* f16_P_PSS_FFI(void* p0, struct S_FFI p1, struct S_FFI p2) { return p0; }
EXPORT void* f16_P_PSS_FFF(void* p0, struct S_FFF p1, struct S_FFF p2) { return p0; }
EXPORT void* f16_P_PSS_FFD(void* p0, struct S_FFD p1, struct S_FFD p2) { return p0; }
EXPORT void* f16_P_PSS_FFP(void* p0, struct S_FFP p1, struct S_FFP p2) { return p0; }
EXPORT void* f16_P_PSS_FDI(void* p0, struct S_FDI p1, struct S_FDI p2) { return p0; }
EXPORT void* f16_P_PSS_FDF(void* p0, struct S_FDF p1, struct S_FDF p2) { return p0; }
EXPORT void* f16_P_PSS_FDD(void* p0, struct S_FDD p1, struct S_FDD p2) { return p0; }
EXPORT void* f16_P_PSS_FDP(void* p0, struct S_FDP p1, struct S_FDP p2) { return p0; }
EXPORT void* f16_P_PSS_FPI(void* p0, struct S_FPI p1, struct S_FPI p2) { return p0; }
EXPORT void* f16_P_PSS_FPF(void* p0, struct S_FPF p1, struct S_FPF p2) { return p0; }
EXPORT void* f16_P_PSS_FPD(void* p0, struct S_FPD p1, struct S_FPD p2) { return p0; }
EXPORT void* f16_P_PSS_FPP(void* p0, struct S_FPP p1, struct S_FPP p2) { return p0; }
EXPORT void* f16_P_PSS_DII(void* p0, struct S_DII p1, struct S_DII p2) { return p0; }
EXPORT void* f16_P_PSS_DIF(void* p0, struct S_DIF p1, struct S_DIF p2) { return p0; }
EXPORT void* f16_P_PSS_DID(void* p0, struct S_DID p1, struct S_DID p2) { return p0; }
EXPORT void* f16_P_PSS_DIP(void* p0, struct S_DIP p1, struct S_DIP p2) { return p0; }
EXPORT void* f16_P_PSS_DFI(void* p0, struct S_DFI p1, struct S_DFI p2) { return p0; }
EXPORT void* f16_P_PSS_DFF(void* p0, struct S_DFF p1, struct S_DFF p2) { return p0; }
EXPORT void* f16_P_PSS_DFD(void* p0, struct S_DFD p1, struct S_DFD p2) { return p0; }
EXPORT void* f16_P_PSS_DFP(void* p0, struct S_DFP p1, struct S_DFP p2) { return p0; }
EXPORT void* f16_P_PSS_DDI(void* p0, struct S_DDI p1, struct S_DDI p2) { return p0; }
EXPORT void* f16_P_PSS_DDF(void* p0, struct S_DDF p1, struct S_DDF p2) { return p0; }
EXPORT void* f16_P_PSS_DDD(void* p0, struct S_DDD p1, struct S_DDD p2) { return p0; }
EXPORT void* f16_P_PSS_DDP(void* p0, struct S_DDP p1, struct S_DDP p2) { return p0; }
EXPORT void* f16_P_PSS_DPI(void* p0, struct S_DPI p1, struct S_DPI p2) { return p0; }
EXPORT void* f16_P_PSS_DPF(void* p0, struct S_DPF p1, struct S_DPF p2) { return p0; }
EXPORT void* f16_P_PSS_DPD(void* p0, struct S_DPD p1, struct S_DPD p2) { return p0; }
EXPORT void* f16_P_PSS_DPP(void* p0, struct S_DPP p1, struct S_DPP p2) { return p0; }
EXPORT void* f16_P_PSS_PII(void* p0, struct S_PII p1, struct S_PII p2) { return p0; }
EXPORT void* f16_P_PSS_PIF(void* p0, struct S_PIF p1, struct S_PIF p2) { return p0; }
EXPORT void* f16_P_PSS_PID(void* p0, struct S_PID p1, struct S_PID p2) { return p0; }
EXPORT void* f16_P_PSS_PIP(void* p0, struct S_PIP p1, struct S_PIP p2) { return p0; }
EXPORT void* f16_P_PSS_PFI(void* p0, struct S_PFI p1, struct S_PFI p2) { return p0; }
EXPORT void* f16_P_PSS_PFF(void* p0, struct S_PFF p1, struct S_PFF p2) { return p0; }
EXPORT void* f16_P_PSS_PFD(void* p0, struct S_PFD p1, struct S_PFD p2) { return p0; }
EXPORT void* f16_P_PSS_PFP(void* p0, struct S_PFP p1, struct S_PFP p2) { return p0; }
EXPORT void* f16_P_PSS_PDI(void* p0, struct S_PDI p1, struct S_PDI p2) { return p0; }
EXPORT void* f16_P_PSS_PDF(void* p0, struct S_PDF p1, struct S_PDF p2) { return p0; }
EXPORT void* f16_P_PSS_PDD(void* p0, struct S_PDD p1, struct S_PDD p2) { return p0; }
EXPORT void* f16_P_PSS_PDP(void* p0, struct S_PDP p1, struct S_PDP p2) { return p0; }
EXPORT void* f16_P_PSS_PPI(void* p0, struct S_PPI p1, struct S_PPI p2) { return p0; }
EXPORT void* f16_P_PSS_PPF(void* p0, struct S_PPF p1, struct S_PPF p2) { return p0; }
EXPORT void* f16_P_PSS_PPD(void* p0, struct S_PPD p1, struct S_PPD p2) { return p0; }
EXPORT void* f16_P_PSS_PPP(void* p0, struct S_PPP p1, struct S_PPP p2) { return p0; }
EXPORT struct S_I f16_S_SII_I(struct S_I p0, int p1, int p2) { return p0; }
EXPORT struct S_F f16_S_SII_F(struct S_F p0, int p1, int p2) { return p0; }
EXPORT struct S_D f16_S_SII_D(struct S_D p0, int p1, int p2) { return p0; }
EXPORT struct S_P f16_S_SII_P(struct S_P p0, int p1, int p2) { return p0; }
EXPORT struct S_II f16_S_SII_II(struct S_II p0, int p1, int p2) { return p0; }
EXPORT struct S_IF f16_S_SII_IF(struct S_IF p0, int p1, int p2) { return p0; }
EXPORT struct S_ID f16_S_SII_ID(struct S_ID p0, int p1, int p2) { return p0; }
EXPORT struct S_IP f16_S_SII_IP(struct S_IP p0, int p1, int p2) { return p0; }
EXPORT struct S_FI f16_S_SII_FI(struct S_FI p0, int p1, int p2) { return p0; }
EXPORT struct S_FF f16_S_SII_FF(struct S_FF p0, int p1, int p2) { return p0; }
EXPORT struct S_FD f16_S_SII_FD(struct S_FD p0, int p1, int p2) { return p0; }
EXPORT struct S_FP f16_S_SII_FP(struct S_FP p0, int p1, int p2) { return p0; }
EXPORT struct S_DI f16_S_SII_DI(struct S_DI p0, int p1, int p2) { return p0; }
EXPORT struct S_DF f16_S_SII_DF(struct S_DF p0, int p1, int p2) { return p0; }
EXPORT struct S_DD f16_S_SII_DD(struct S_DD p0, int p1, int p2) { return p0; }
EXPORT struct S_DP f16_S_SII_DP(struct S_DP p0, int p1, int p2) { return p0; }
EXPORT struct S_PI f16_S_SII_PI(struct S_PI p0, int p1, int p2) { return p0; }
EXPORT struct S_PF f16_S_SII_PF(struct S_PF p0, int p1, int p2) { return p0; }
EXPORT struct S_PD f16_S_SII_PD(struct S_PD p0, int p1, int p2) { return p0; }
EXPORT struct S_PP f16_S_SII_PP(struct S_PP p0, int p1, int p2) { return p0; }
EXPORT struct S_III f16_S_SII_III(struct S_III p0, int p1, int p2) { return p0; }
EXPORT struct S_IIF f16_S_SII_IIF(struct S_IIF p0, int p1, int p2) { return p0; }
EXPORT struct S_IID f16_S_SII_IID(struct S_IID p0, int p1, int p2) { return p0; }
EXPORT struct S_IIP f16_S_SII_IIP(struct S_IIP p0, int p1, int p2) { return p0; }
EXPORT struct S_IFI f16_S_SII_IFI(struct S_IFI p0, int p1, int p2) { return p0; }
EXPORT struct S_IFF f16_S_SII_IFF(struct S_IFF p0, int p1, int p2) { return p0; }
EXPORT struct S_IFD f16_S_SII_IFD(struct S_IFD p0, int p1, int p2) { return p0; }
EXPORT struct S_IFP f16_S_SII_IFP(struct S_IFP p0, int p1, int p2) { return p0; }
EXPORT struct S_IDI f16_S_SII_IDI(struct S_IDI p0, int p1, int p2) { return p0; }
EXPORT struct S_IDF f16_S_SII_IDF(struct S_IDF p0, int p1, int p2) { return p0; }
EXPORT struct S_IDD f16_S_SII_IDD(struct S_IDD p0, int p1, int p2) { return p0; }
EXPORT struct S_IDP f16_S_SII_IDP(struct S_IDP p0, int p1, int p2) { return p0; }
EXPORT struct S_IPI f16_S_SII_IPI(struct S_IPI p0, int p1, int p2) { return p0; }
EXPORT struct S_IPF f16_S_SII_IPF(struct S_IPF p0, int p1, int p2) { return p0; }
EXPORT struct S_IPD f16_S_SII_IPD(struct S_IPD p0, int p1, int p2) { return p0; }
EXPORT struct S_IPP f16_S_SII_IPP(struct S_IPP p0, int p1, int p2) { return p0; }
EXPORT struct S_FII f16_S_SII_FII(struct S_FII p0, int p1, int p2) { return p0; }
EXPORT struct S_FIF f16_S_SII_FIF(struct S_FIF p0, int p1, int p2) { return p0; }
EXPORT struct S_FID f16_S_SII_FID(struct S_FID p0, int p1, int p2) { return p0; }
EXPORT struct S_FIP f16_S_SII_FIP(struct S_FIP p0, int p1, int p2) { return p0; }
EXPORT struct S_FFI f16_S_SII_FFI(struct S_FFI p0, int p1, int p2) { return p0; }
EXPORT struct S_FFF f16_S_SII_FFF(struct S_FFF p0, int p1, int p2) { return p0; }
EXPORT struct S_FFD f16_S_SII_FFD(struct S_FFD p0, int p1, int p2) { return p0; }
EXPORT struct S_FFP f16_S_SII_FFP(struct S_FFP p0, int p1, int p2) { return p0; }
EXPORT struct S_FDI f16_S_SII_FDI(struct S_FDI p0, int p1, int p2) { return p0; }
EXPORT struct S_FDF f16_S_SII_FDF(struct S_FDF p0, int p1, int p2) { return p0; }
EXPORT struct S_FDD f16_S_SII_FDD(struct S_FDD p0, int p1, int p2) { return p0; }
EXPORT struct S_FDP f16_S_SII_FDP(struct S_FDP p0, int p1, int p2) { return p0; }
EXPORT struct S_FPI f16_S_SII_FPI(struct S_FPI p0, int p1, int p2) { return p0; }
EXPORT struct S_FPF f16_S_SII_FPF(struct S_FPF p0, int p1, int p2) { return p0; }
EXPORT struct S_FPD f16_S_SII_FPD(struct S_FPD p0, int p1, int p2) { return p0; }
EXPORT struct S_FPP f16_S_SII_FPP(struct S_FPP p0, int p1, int p2) { return p0; }
EXPORT struct S_DII f16_S_SII_DII(struct S_DII p0, int p1, int p2) { return p0; }
EXPORT struct S_DIF f16_S_SII_DIF(struct S_DIF p0, int p1, int p2) { return p0; }
EXPORT struct S_DID f16_S_SII_DID(struct S_DID p0, int p1, int p2) { return p0; }
EXPORT struct S_DIP f16_S_SII_DIP(struct S_DIP p0, int p1, int p2) { return p0; }
EXPORT struct S_DFI f16_S_SII_DFI(struct S_DFI p0, int p1, int p2) { return p0; }
EXPORT struct S_DFF f16_S_SII_DFF(struct S_DFF p0, int p1, int p2) { return p0; }
EXPORT struct S_DFD f16_S_SII_DFD(struct S_DFD p0, int p1, int p2) { return p0; }
EXPORT struct S_DFP f16_S_SII_DFP(struct S_DFP p0, int p1, int p2) { return p0; }
EXPORT struct S_DDI f16_S_SII_DDI(struct S_DDI p0, int p1, int p2) { return p0; }
EXPORT struct S_DDF f16_S_SII_DDF(struct S_DDF p0, int p1, int p2) { return p0; }
EXPORT struct S_DDD f16_S_SII_DDD(struct S_DDD p0, int p1, int p2) { return p0; }
EXPORT struct S_DDP f16_S_SII_DDP(struct S_DDP p0, int p1, int p2) { return p0; }
EXPORT struct S_DPI f16_S_SII_DPI(struct S_DPI p0, int p1, int p2) { return p0; }
EXPORT struct S_DPF f16_S_SII_DPF(struct S_DPF p0, int p1, int p2) { return p0; }
EXPORT struct S_DPD f16_S_SII_DPD(struct S_DPD p0, int p1, int p2) { return p0; }
EXPORT struct S_DPP f16_S_SII_DPP(struct S_DPP p0, int p1, int p2) { return p0; }
EXPORT struct S_PII f16_S_SII_PII(struct S_PII p0, int p1, int p2) { return p0; }
EXPORT struct S_PIF f16_S_SII_PIF(struct S_PIF p0, int p1, int p2) { return p0; }
EXPORT struct S_PID f16_S_SII_PID(struct S_PID p0, int p1, int p2) { return p0; }
EXPORT struct S_PIP f16_S_SII_PIP(struct S_PIP p0, int p1, int p2) { return p0; }
EXPORT struct S_PFI f16_S_SII_PFI(struct S_PFI p0, int p1, int p2) { return p0; }
EXPORT struct S_PFF f16_S_SII_PFF(struct S_PFF p0, int p1, int p2) { return p0; }
EXPORT struct S_PFD f16_S_SII_PFD(struct S_PFD p0, int p1, int p2) { return p0; }
EXPORT struct S_PFP f16_S_SII_PFP(struct S_PFP p0, int p1, int p2) { return p0; }
EXPORT struct S_PDI f16_S_SII_PDI(struct S_PDI p0, int p1, int p2) { return p0; }
EXPORT struct S_PDF f16_S_SII_PDF(struct S_PDF p0, int p1, int p2) { return p0; }
EXPORT struct S_PDD f16_S_SII_PDD(struct S_PDD p0, int p1, int p2) { return p0; }
EXPORT struct S_PDP f16_S_SII_PDP(struct S_PDP p0, int p1, int p2) { return p0; }
EXPORT struct S_PPI f16_S_SII_PPI(struct S_PPI p0, int p1, int p2) { return p0; }
EXPORT struct S_PPF f16_S_SII_PPF(struct S_PPF p0, int p1, int p2) { return p0; }
EXPORT struct S_PPD f16_S_SII_PPD(struct S_PPD p0, int p1, int p2) { return p0; }
EXPORT struct S_PPP f16_S_SII_PPP(struct S_PPP p0, int p1, int p2) { return p0; }
EXPORT struct S_I f16_S_SIF_I(struct S_I p0, int p1, float p2) { return p0; }
EXPORT struct S_F f16_S_SIF_F(struct S_F p0, int p1, float p2) { return p0; }
EXPORT struct S_D f16_S_SIF_D(struct S_D p0, int p1, float p2) { return p0; }
EXPORT struct S_P f16_S_SIF_P(struct S_P p0, int p1, float p2) { return p0; }
EXPORT struct S_II f16_S_SIF_II(struct S_II p0, int p1, float p2) { return p0; }
EXPORT struct S_IF f16_S_SIF_IF(struct S_IF p0, int p1, float p2) { return p0; }
EXPORT struct S_ID f16_S_SIF_ID(struct S_ID p0, int p1, float p2) { return p0; }
EXPORT struct S_IP f16_S_SIF_IP(struct S_IP p0, int p1, float p2) { return p0; }
EXPORT struct S_FI f16_S_SIF_FI(struct S_FI p0, int p1, float p2) { return p0; }
EXPORT struct S_FF f16_S_SIF_FF(struct S_FF p0, int p1, float p2) { return p0; }
EXPORT struct S_FD f16_S_SIF_FD(struct S_FD p0, int p1, float p2) { return p0; }
EXPORT struct S_FP f16_S_SIF_FP(struct S_FP p0, int p1, float p2) { return p0; }
EXPORT struct S_DI f16_S_SIF_DI(struct S_DI p0, int p1, float p2) { return p0; }
EXPORT struct S_DF f16_S_SIF_DF(struct S_DF p0, int p1, float p2) { return p0; }
EXPORT struct S_DD f16_S_SIF_DD(struct S_DD p0, int p1, float p2) { return p0; }
EXPORT struct S_DP f16_S_SIF_DP(struct S_DP p0, int p1, float p2) { return p0; }
EXPORT struct S_PI f16_S_SIF_PI(struct S_PI p0, int p1, float p2) { return p0; }
EXPORT struct S_PF f16_S_SIF_PF(struct S_PF p0, int p1, float p2) { return p0; }
EXPORT struct S_PD f16_S_SIF_PD(struct S_PD p0, int p1, float p2) { return p0; }
EXPORT struct S_PP f16_S_SIF_PP(struct S_PP p0, int p1, float p2) { return p0; }
EXPORT struct S_III f16_S_SIF_III(struct S_III p0, int p1, float p2) { return p0; }
EXPORT struct S_IIF f16_S_SIF_IIF(struct S_IIF p0, int p1, float p2) { return p0; }
EXPORT struct S_IID f16_S_SIF_IID(struct S_IID p0, int p1, float p2) { return p0; }
EXPORT struct S_IIP f16_S_SIF_IIP(struct S_IIP p0, int p1, float p2) { return p0; }
EXPORT struct S_IFI f16_S_SIF_IFI(struct S_IFI p0, int p1, float p2) { return p0; }
EXPORT struct S_IFF f16_S_SIF_IFF(struct S_IFF p0, int p1, float p2) { return p0; }
EXPORT struct S_IFD f16_S_SIF_IFD(struct S_IFD p0, int p1, float p2) { return p0; }
EXPORT struct S_IFP f16_S_SIF_IFP(struct S_IFP p0, int p1, float p2) { return p0; }
EXPORT struct S_IDI f16_S_SIF_IDI(struct S_IDI p0, int p1, float p2) { return p0; }
EXPORT struct S_IDF f16_S_SIF_IDF(struct S_IDF p0, int p1, float p2) { return p0; }
EXPORT struct S_IDD f16_S_SIF_IDD(struct S_IDD p0, int p1, float p2) { return p0; }
EXPORT struct S_IDP f16_S_SIF_IDP(struct S_IDP p0, int p1, float p2) { return p0; }
EXPORT struct S_IPI f16_S_SIF_IPI(struct S_IPI p0, int p1, float p2) { return p0; }
EXPORT struct S_IPF f16_S_SIF_IPF(struct S_IPF p0, int p1, float p2) { return p0; }
EXPORT struct S_IPD f16_S_SIF_IPD(struct S_IPD p0, int p1, float p2) { return p0; }
EXPORT struct S_IPP f16_S_SIF_IPP(struct S_IPP p0, int p1, float p2) { return p0; }
EXPORT struct S_FII f16_S_SIF_FII(struct S_FII p0, int p1, float p2) { return p0; }
EXPORT struct S_FIF f16_S_SIF_FIF(struct S_FIF p0, int p1, float p2) { return p0; }
EXPORT struct S_FID f16_S_SIF_FID(struct S_FID p0, int p1, float p2) { return p0; }
EXPORT struct S_FIP f16_S_SIF_FIP(struct S_FIP p0, int p1, float p2) { return p0; }
EXPORT struct S_FFI f16_S_SIF_FFI(struct S_FFI p0, int p1, float p2) { return p0; }
EXPORT struct S_FFF f16_S_SIF_FFF(struct S_FFF p0, int p1, float p2) { return p0; }
EXPORT struct S_FFD f16_S_SIF_FFD(struct S_FFD p0, int p1, float p2) { return p0; }
EXPORT struct S_FFP f16_S_SIF_FFP(struct S_FFP p0, int p1, float p2) { return p0; }
EXPORT struct S_FDI f16_S_SIF_FDI(struct S_FDI p0, int p1, float p2) { return p0; }
EXPORT struct S_FDF f16_S_SIF_FDF(struct S_FDF p0, int p1, float p2) { return p0; }
EXPORT struct S_FDD f16_S_SIF_FDD(struct S_FDD p0, int p1, float p2) { return p0; }
EXPORT struct S_FDP f16_S_SIF_FDP(struct S_FDP p0, int p1, float p2) { return p0; }
EXPORT struct S_FPI f16_S_SIF_FPI(struct S_FPI p0, int p1, float p2) { return p0; }
EXPORT struct S_FPF f16_S_SIF_FPF(struct S_FPF p0, int p1, float p2) { return p0; }
EXPORT struct S_FPD f16_S_SIF_FPD(struct S_FPD p0, int p1, float p2) { return p0; }
EXPORT struct S_FPP f16_S_SIF_FPP(struct S_FPP p0, int p1, float p2) { return p0; }
EXPORT struct S_DII f16_S_SIF_DII(struct S_DII p0, int p1, float p2) { return p0; }
EXPORT struct S_DIF f16_S_SIF_DIF(struct S_DIF p0, int p1, float p2) { return p0; }
EXPORT struct S_DID f16_S_SIF_DID(struct S_DID p0, int p1, float p2) { return p0; }
EXPORT struct S_DIP f16_S_SIF_DIP(struct S_DIP p0, int p1, float p2) { return p0; }
EXPORT struct S_DFI f16_S_SIF_DFI(struct S_DFI p0, int p1, float p2) { return p0; }
EXPORT struct S_DFF f16_S_SIF_DFF(struct S_DFF p0, int p1, float p2) { return p0; }
EXPORT struct S_DFD f16_S_SIF_DFD(struct S_DFD p0, int p1, float p2) { return p0; }
EXPORT struct S_DFP f16_S_SIF_DFP(struct S_DFP p0, int p1, float p2) { return p0; }
EXPORT struct S_DDI f16_S_SIF_DDI(struct S_DDI p0, int p1, float p2) { return p0; }
EXPORT struct S_DDF f16_S_SIF_DDF(struct S_DDF p0, int p1, float p2) { return p0; }
EXPORT struct S_DDD f16_S_SIF_DDD(struct S_DDD p0, int p1, float p2) { return p0; }
EXPORT struct S_DDP f16_S_SIF_DDP(struct S_DDP p0, int p1, float p2) { return p0; }
EXPORT struct S_DPI f16_S_SIF_DPI(struct S_DPI p0, int p1, float p2) { return p0; }
EXPORT struct S_DPF f16_S_SIF_DPF(struct S_DPF p0, int p1, float p2) { return p0; }
EXPORT struct S_DPD f16_S_SIF_DPD(struct S_DPD p0, int p1, float p2) { return p0; }
EXPORT struct S_DPP f16_S_SIF_DPP(struct S_DPP p0, int p1, float p2) { return p0; }
EXPORT struct S_PII f16_S_SIF_PII(struct S_PII p0, int p1, float p2) { return p0; }
EXPORT struct S_PIF f16_S_SIF_PIF(struct S_PIF p0, int p1, float p2) { return p0; }
EXPORT struct S_PID f16_S_SIF_PID(struct S_PID p0, int p1, float p2) { return p0; }
EXPORT struct S_PIP f16_S_SIF_PIP(struct S_PIP p0, int p1, float p2) { return p0; }
EXPORT struct S_PFI f16_S_SIF_PFI(struct S_PFI p0, int p1, float p2) { return p0; }
EXPORT struct S_PFF f16_S_SIF_PFF(struct S_PFF p0, int p1, float p2) { return p0; }
EXPORT struct S_PFD f16_S_SIF_PFD(struct S_PFD p0, int p1, float p2) { return p0; }
EXPORT struct S_PFP f16_S_SIF_PFP(struct S_PFP p0, int p1, float p2) { return p0; }
EXPORT struct S_PDI f16_S_SIF_PDI(struct S_PDI p0, int p1, float p2) { return p0; }
EXPORT struct S_PDF f16_S_SIF_PDF(struct S_PDF p0, int p1, float p2) { return p0; }
EXPORT struct S_PDD f16_S_SIF_PDD(struct S_PDD p0, int p1, float p2) { return p0; }
EXPORT struct S_PDP f16_S_SIF_PDP(struct S_PDP p0, int p1, float p2) { return p0; }
EXPORT struct S_PPI f16_S_SIF_PPI(struct S_PPI p0, int p1, float p2) { return p0; }
EXPORT struct S_PPF f16_S_SIF_PPF(struct S_PPF p0, int p1, float p2) { return p0; }
EXPORT struct S_PPD f16_S_SIF_PPD(struct S_PPD p0, int p1, float p2) { return p0; }
EXPORT struct S_PPP f16_S_SIF_PPP(struct S_PPP p0, int p1, float p2) { return p0; }
EXPORT struct S_I f16_S_SID_I(struct S_I p0, int p1, double p2) { return p0; }
EXPORT struct S_F f16_S_SID_F(struct S_F p0, int p1, double p2) { return p0; }
EXPORT struct S_D f16_S_SID_D(struct S_D p0, int p1, double p2) { return p0; }
EXPORT struct S_P f16_S_SID_P(struct S_P p0, int p1, double p2) { return p0; }
EXPORT struct S_II f16_S_SID_II(struct S_II p0, int p1, double p2) { return p0; }
EXPORT struct S_IF f16_S_SID_IF(struct S_IF p0, int p1, double p2) { return p0; }
EXPORT struct S_ID f16_S_SID_ID(struct S_ID p0, int p1, double p2) { return p0; }
EXPORT struct S_IP f16_S_SID_IP(struct S_IP p0, int p1, double p2) { return p0; }
EXPORT struct S_FI f16_S_SID_FI(struct S_FI p0, int p1, double p2) { return p0; }
EXPORT struct S_FF f16_S_SID_FF(struct S_FF p0, int p1, double p2) { return p0; }
EXPORT struct S_FD f16_S_SID_FD(struct S_FD p0, int p1, double p2) { return p0; }
EXPORT struct S_FP f16_S_SID_FP(struct S_FP p0, int p1, double p2) { return p0; }
EXPORT struct S_DI f16_S_SID_DI(struct S_DI p0, int p1, double p2) { return p0; }
EXPORT struct S_DF f16_S_SID_DF(struct S_DF p0, int p1, double p2) { return p0; }
EXPORT struct S_DD f16_S_SID_DD(struct S_DD p0, int p1, double p2) { return p0; }
EXPORT struct S_DP f16_S_SID_DP(struct S_DP p0, int p1, double p2) { return p0; }
EXPORT struct S_PI f16_S_SID_PI(struct S_PI p0, int p1, double p2) { return p0; }
EXPORT struct S_PF f16_S_SID_PF(struct S_PF p0, int p1, double p2) { return p0; }
EXPORT struct S_PD f16_S_SID_PD(struct S_PD p0, int p1, double p2) { return p0; }
EXPORT struct S_PP f16_S_SID_PP(struct S_PP p0, int p1, double p2) { return p0; }
EXPORT struct S_III f16_S_SID_III(struct S_III p0, int p1, double p2) { return p0; }
EXPORT struct S_IIF f16_S_SID_IIF(struct S_IIF p0, int p1, double p2) { return p0; }
EXPORT struct S_IID f16_S_SID_IID(struct S_IID p0, int p1, double p2) { return p0; }
EXPORT struct S_IIP f16_S_SID_IIP(struct S_IIP p0, int p1, double p2) { return p0; }
EXPORT struct S_IFI f16_S_SID_IFI(struct S_IFI p0, int p1, double p2) { return p0; }
EXPORT struct S_IFF f16_S_SID_IFF(struct S_IFF p0, int p1, double p2) { return p0; }
EXPORT struct S_IFD f16_S_SID_IFD(struct S_IFD p0, int p1, double p2) { return p0; }
EXPORT struct S_IFP f16_S_SID_IFP(struct S_IFP p0, int p1, double p2) { return p0; }
EXPORT struct S_IDI f16_S_SID_IDI(struct S_IDI p0, int p1, double p2) { return p0; }
EXPORT struct S_IDF f16_S_SID_IDF(struct S_IDF p0, int p1, double p2) { return p0; }
EXPORT struct S_IDD f16_S_SID_IDD(struct S_IDD p0, int p1, double p2) { return p0; }
EXPORT struct S_IDP f16_S_SID_IDP(struct S_IDP p0, int p1, double p2) { return p0; }
EXPORT struct S_IPI f16_S_SID_IPI(struct S_IPI p0, int p1, double p2) { return p0; }
EXPORT struct S_IPF f16_S_SID_IPF(struct S_IPF p0, int p1, double p2) { return p0; }
EXPORT struct S_IPD f16_S_SID_IPD(struct S_IPD p0, int p1, double p2) { return p0; }
EXPORT struct S_IPP f17_S_SID_IPP(struct S_IPP p0, int p1, double p2) { return p0; }
EXPORT struct S_FII f17_S_SID_FII(struct S_FII p0, int p1, double p2) { return p0; }
EXPORT struct S_FIF f17_S_SID_FIF(struct S_FIF p0, int p1, double p2) { return p0; }
EXPORT struct S_FID f17_S_SID_FID(struct S_FID p0, int p1, double p2) { return p0; }
EXPORT struct S_FIP f17_S_SID_FIP(struct S_FIP p0, int p1, double p2) { return p0; }
EXPORT struct S_FFI f17_S_SID_FFI(struct S_FFI p0, int p1, double p2) { return p0; }
EXPORT struct S_FFF f17_S_SID_FFF(struct S_FFF p0, int p1, double p2) { return p0; }
EXPORT struct S_FFD f17_S_SID_FFD(struct S_FFD p0, int p1, double p2) { return p0; }
EXPORT struct S_FFP f17_S_SID_FFP(struct S_FFP p0, int p1, double p2) { return p0; }
EXPORT struct S_FDI f17_S_SID_FDI(struct S_FDI p0, int p1, double p2) { return p0; }
EXPORT struct S_FDF f17_S_SID_FDF(struct S_FDF p0, int p1, double p2) { return p0; }
EXPORT struct S_FDD f17_S_SID_FDD(struct S_FDD p0, int p1, double p2) { return p0; }
EXPORT struct S_FDP f17_S_SID_FDP(struct S_FDP p0, int p1, double p2) { return p0; }
EXPORT struct S_FPI f17_S_SID_FPI(struct S_FPI p0, int p1, double p2) { return p0; }
EXPORT struct S_FPF f17_S_SID_FPF(struct S_FPF p0, int p1, double p2) { return p0; }
EXPORT struct S_FPD f17_S_SID_FPD(struct S_FPD p0, int p1, double p2) { return p0; }
EXPORT struct S_FPP f17_S_SID_FPP(struct S_FPP p0, int p1, double p2) { return p0; }
EXPORT struct S_DII f17_S_SID_DII(struct S_DII p0, int p1, double p2) { return p0; }
EXPORT struct S_DIF f17_S_SID_DIF(struct S_DIF p0, int p1, double p2) { return p0; }
EXPORT struct S_DID f17_S_SID_DID(struct S_DID p0, int p1, double p2) { return p0; }
EXPORT struct S_DIP f17_S_SID_DIP(struct S_DIP p0, int p1, double p2) { return p0; }
EXPORT struct S_DFI f17_S_SID_DFI(struct S_DFI p0, int p1, double p2) { return p0; }
EXPORT struct S_DFF f17_S_SID_DFF(struct S_DFF p0, int p1, double p2) { return p0; }
EXPORT struct S_DFD f17_S_SID_DFD(struct S_DFD p0, int p1, double p2) { return p0; }
EXPORT struct S_DFP f17_S_SID_DFP(struct S_DFP p0, int p1, double p2) { return p0; }
EXPORT struct S_DDI f17_S_SID_DDI(struct S_DDI p0, int p1, double p2) { return p0; }
EXPORT struct S_DDF f17_S_SID_DDF(struct S_DDF p0, int p1, double p2) { return p0; }
EXPORT struct S_DDD f17_S_SID_DDD(struct S_DDD p0, int p1, double p2) { return p0; }
EXPORT struct S_DDP f17_S_SID_DDP(struct S_DDP p0, int p1, double p2) { return p0; }
EXPORT struct S_DPI f17_S_SID_DPI(struct S_DPI p0, int p1, double p2) { return p0; }
EXPORT struct S_DPF f17_S_SID_DPF(struct S_DPF p0, int p1, double p2) { return p0; }
EXPORT struct S_DPD f17_S_SID_DPD(struct S_DPD p0, int p1, double p2) { return p0; }
EXPORT struct S_DPP f17_S_SID_DPP(struct S_DPP p0, int p1, double p2) { return p0; }
EXPORT struct S_PII f17_S_SID_PII(struct S_PII p0, int p1, double p2) { return p0; }
EXPORT struct S_PIF f17_S_SID_PIF(struct S_PIF p0, int p1, double p2) { return p0; }
EXPORT struct S_PID f17_S_SID_PID(struct S_PID p0, int p1, double p2) { return p0; }
EXPORT struct S_PIP f17_S_SID_PIP(struct S_PIP p0, int p1, double p2) { return p0; }
EXPORT struct S_PFI f17_S_SID_PFI(struct S_PFI p0, int p1, double p2) { return p0; }
EXPORT struct S_PFF f17_S_SID_PFF(struct S_PFF p0, int p1, double p2) { return p0; }
EXPORT struct S_PFD f17_S_SID_PFD(struct S_PFD p0, int p1, double p2) { return p0; }
EXPORT struct S_PFP f17_S_SID_PFP(struct S_PFP p0, int p1, double p2) { return p0; }
EXPORT struct S_PDI f17_S_SID_PDI(struct S_PDI p0, int p1, double p2) { return p0; }
EXPORT struct S_PDF f17_S_SID_PDF(struct S_PDF p0, int p1, double p2) { return p0; }
EXPORT struct S_PDD f17_S_SID_PDD(struct S_PDD p0, int p1, double p2) { return p0; }
EXPORT struct S_PDP f17_S_SID_PDP(struct S_PDP p0, int p1, double p2) { return p0; }
EXPORT struct S_PPI f17_S_SID_PPI(struct S_PPI p0, int p1, double p2) { return p0; }
EXPORT struct S_PPF f17_S_SID_PPF(struct S_PPF p0, int p1, double p2) { return p0; }
EXPORT struct S_PPD f17_S_SID_PPD(struct S_PPD p0, int p1, double p2) { return p0; }
EXPORT struct S_PPP f17_S_SID_PPP(struct S_PPP p0, int p1, double p2) { return p0; }
EXPORT struct S_I f17_S_SIP_I(struct S_I p0, int p1, void* p2) { return p0; }
EXPORT struct S_F f17_S_SIP_F(struct S_F p0, int p1, void* p2) { return p0; }
EXPORT struct S_D f17_S_SIP_D(struct S_D p0, int p1, void* p2) { return p0; }
EXPORT struct S_P f17_S_SIP_P(struct S_P p0, int p1, void* p2) { return p0; }
EXPORT struct S_II f17_S_SIP_II(struct S_II p0, int p1, void* p2) { return p0; }
EXPORT struct S_IF f17_S_SIP_IF(struct S_IF p0, int p1, void* p2) { return p0; }
EXPORT struct S_ID f17_S_SIP_ID(struct S_ID p0, int p1, void* p2) { return p0; }
EXPORT struct S_IP f17_S_SIP_IP(struct S_IP p0, int p1, void* p2) { return p0; }
EXPORT struct S_FI f17_S_SIP_FI(struct S_FI p0, int p1, void* p2) { return p0; }
EXPORT struct S_FF f17_S_SIP_FF(struct S_FF p0, int p1, void* p2) { return p0; }
EXPORT struct S_FD f17_S_SIP_FD(struct S_FD p0, int p1, void* p2) { return p0; }
EXPORT struct S_FP f17_S_SIP_FP(struct S_FP p0, int p1, void* p2) { return p0; }
EXPORT struct S_DI f17_S_SIP_DI(struct S_DI p0, int p1, void* p2) { return p0; }
EXPORT struct S_DF f17_S_SIP_DF(struct S_DF p0, int p1, void* p2) { return p0; }
EXPORT struct S_DD f17_S_SIP_DD(struct S_DD p0, int p1, void* p2) { return p0; }
EXPORT struct S_DP f17_S_SIP_DP(struct S_DP p0, int p1, void* p2) { return p0; }
EXPORT struct S_PI f17_S_SIP_PI(struct S_PI p0, int p1, void* p2) { return p0; }
EXPORT struct S_PF f17_S_SIP_PF(struct S_PF p0, int p1, void* p2) { return p0; }
EXPORT struct S_PD f17_S_SIP_PD(struct S_PD p0, int p1, void* p2) { return p0; }
EXPORT struct S_PP f17_S_SIP_PP(struct S_PP p0, int p1, void* p2) { return p0; }
EXPORT struct S_III f17_S_SIP_III(struct S_III p0, int p1, void* p2) { return p0; }
EXPORT struct S_IIF f17_S_SIP_IIF(struct S_IIF p0, int p1, void* p2) { return p0; }
EXPORT struct S_IID f17_S_SIP_IID(struct S_IID p0, int p1, void* p2) { return p0; }
EXPORT struct S_IIP f17_S_SIP_IIP(struct S_IIP p0, int p1, void* p2) { return p0; }
EXPORT struct S_IFI f17_S_SIP_IFI(struct S_IFI p0, int p1, void* p2) { return p0; }
EXPORT struct S_IFF f17_S_SIP_IFF(struct S_IFF p0, int p1, void* p2) { return p0; }
EXPORT struct S_IFD f17_S_SIP_IFD(struct S_IFD p0, int p1, void* p2) { return p0; }
EXPORT struct S_IFP f17_S_SIP_IFP(struct S_IFP p0, int p1, void* p2) { return p0; }
EXPORT struct S_IDI f17_S_SIP_IDI(struct S_IDI p0, int p1, void* p2) { return p0; }
EXPORT struct S_IDF f17_S_SIP_IDF(struct S_IDF p0, int p1, void* p2) { return p0; }
EXPORT struct S_IDD f17_S_SIP_IDD(struct S_IDD p0, int p1, void* p2) { return p0; }
EXPORT struct S_IDP f17_S_SIP_IDP(struct S_IDP p0, int p1, void* p2) { return p0; }
EXPORT struct S_IPI f17_S_SIP_IPI(struct S_IPI p0, int p1, void* p2) { return p0; }
EXPORT struct S_IPF f17_S_SIP_IPF(struct S_IPF p0, int p1, void* p2) { return p0; }
EXPORT struct S_IPD f17_S_SIP_IPD(struct S_IPD p0, int p1, void* p2) { return p0; }
EXPORT struct S_IPP f17_S_SIP_IPP(struct S_IPP p0, int p1, void* p2) { return p0; }
EXPORT struct S_FII f17_S_SIP_FII(struct S_FII p0, int p1, void* p2) { return p0; }
EXPORT struct S_FIF f17_S_SIP_FIF(struct S_FIF p0, int p1, void* p2) { return p0; }
EXPORT struct S_FID f17_S_SIP_FID(struct S_FID p0, int p1, void* p2) { return p0; }
EXPORT struct S_FIP f17_S_SIP_FIP(struct S_FIP p0, int p1, void* p2) { return p0; }
EXPORT struct S_FFI f17_S_SIP_FFI(struct S_FFI p0, int p1, void* p2) { return p0; }
EXPORT struct S_FFF f17_S_SIP_FFF(struct S_FFF p0, int p1, void* p2) { return p0; }
EXPORT struct S_FFD f17_S_SIP_FFD(struct S_FFD p0, int p1, void* p2) { return p0; }
EXPORT struct S_FFP f17_S_SIP_FFP(struct S_FFP p0, int p1, void* p2) { return p0; }
EXPORT struct S_FDI f17_S_SIP_FDI(struct S_FDI p0, int p1, void* p2) { return p0; }
EXPORT struct S_FDF f17_S_SIP_FDF(struct S_FDF p0, int p1, void* p2) { return p0; }
EXPORT struct S_FDD f17_S_SIP_FDD(struct S_FDD p0, int p1, void* p2) { return p0; }
EXPORT struct S_FDP f17_S_SIP_FDP(struct S_FDP p0, int p1, void* p2) { return p0; }
EXPORT struct S_FPI f17_S_SIP_FPI(struct S_FPI p0, int p1, void* p2) { return p0; }
EXPORT struct S_FPF f17_S_SIP_FPF(struct S_FPF p0, int p1, void* p2) { return p0; }
EXPORT struct S_FPD f17_S_SIP_FPD(struct S_FPD p0, int p1, void* p2) { return p0; }
EXPORT struct S_FPP f17_S_SIP_FPP(struct S_FPP p0, int p1, void* p2) { return p0; }
EXPORT struct S_DII f17_S_SIP_DII(struct S_DII p0, int p1, void* p2) { return p0; }
EXPORT struct S_DIF f17_S_SIP_DIF(struct S_DIF p0, int p1, void* p2) { return p0; }
EXPORT struct S_DID f17_S_SIP_DID(struct S_DID p0, int p1, void* p2) { return p0; }
EXPORT struct S_DIP f17_S_SIP_DIP(struct S_DIP p0, int p1, void* p2) { return p0; }
EXPORT struct S_DFI f17_S_SIP_DFI(struct S_DFI p0, int p1, void* p2) { return p0; }
EXPORT struct S_DFF f17_S_SIP_DFF(struct S_DFF p0, int p1, void* p2) { return p0; }
EXPORT struct S_DFD f17_S_SIP_DFD(struct S_DFD p0, int p1, void* p2) { return p0; }
EXPORT struct S_DFP f17_S_SIP_DFP(struct S_DFP p0, int p1, void* p2) { return p0; }
EXPORT struct S_DDI f17_S_SIP_DDI(struct S_DDI p0, int p1, void* p2) { return p0; }
EXPORT struct S_DDF f17_S_SIP_DDF(struct S_DDF p0, int p1, void* p2) { return p0; }
EXPORT struct S_DDD f17_S_SIP_DDD(struct S_DDD p0, int p1, void* p2) { return p0; }
EXPORT struct S_DDP f17_S_SIP_DDP(struct S_DDP p0, int p1, void* p2) { return p0; }
EXPORT struct S_DPI f17_S_SIP_DPI(struct S_DPI p0, int p1, void* p2) { return p0; }
EXPORT struct S_DPF f17_S_SIP_DPF(struct S_DPF p0, int p1, void* p2) { return p0; }
EXPORT struct S_DPD f17_S_SIP_DPD(struct S_DPD p0, int p1, void* p2) { return p0; }
EXPORT struct S_DPP f17_S_SIP_DPP(struct S_DPP p0, int p1, void* p2) { return p0; }
EXPORT struct S_PII f17_S_SIP_PII(struct S_PII p0, int p1, void* p2) { return p0; }
EXPORT struct S_PIF f17_S_SIP_PIF(struct S_PIF p0, int p1, void* p2) { return p0; }
EXPORT struct S_PID f17_S_SIP_PID(struct S_PID p0, int p1, void* p2) { return p0; }
EXPORT struct S_PIP f17_S_SIP_PIP(struct S_PIP p0, int p1, void* p2) { return p0; }
EXPORT struct S_PFI f17_S_SIP_PFI(struct S_PFI p0, int p1, void* p2) { return p0; }
EXPORT struct S_PFF f17_S_SIP_PFF(struct S_PFF p0, int p1, void* p2) { return p0; }
EXPORT struct S_PFD f17_S_SIP_PFD(struct S_PFD p0, int p1, void* p2) { return p0; }
EXPORT struct S_PFP f17_S_SIP_PFP(struct S_PFP p0, int p1, void* p2) { return p0; }
EXPORT struct S_PDI f17_S_SIP_PDI(struct S_PDI p0, int p1, void* p2) { return p0; }
EXPORT struct S_PDF f17_S_SIP_PDF(struct S_PDF p0, int p1, void* p2) { return p0; }
EXPORT struct S_PDD f17_S_SIP_PDD(struct S_PDD p0, int p1, void* p2) { return p0; }
EXPORT struct S_PDP f17_S_SIP_PDP(struct S_PDP p0, int p1, void* p2) { return p0; }
EXPORT struct S_PPI f17_S_SIP_PPI(struct S_PPI p0, int p1, void* p2) { return p0; }
EXPORT struct S_PPF f17_S_SIP_PPF(struct S_PPF p0, int p1, void* p2) { return p0; }
EXPORT struct S_PPD f17_S_SIP_PPD(struct S_PPD p0, int p1, void* p2) { return p0; }
EXPORT struct S_PPP f17_S_SIP_PPP(struct S_PPP p0, int p1, void* p2) { return p0; }
EXPORT struct S_I f17_S_SIS_I(struct S_I p0, int p1, struct S_I p2) { return p0; }
EXPORT struct S_F f17_S_SIS_F(struct S_F p0, int p1, struct S_F p2) { return p0; }
EXPORT struct S_D f17_S_SIS_D(struct S_D p0, int p1, struct S_D p2) { return p0; }
EXPORT struct S_P f17_S_SIS_P(struct S_P p0, int p1, struct S_P p2) { return p0; }
EXPORT struct S_II f17_S_SIS_II(struct S_II p0, int p1, struct S_II p2) { return p0; }
EXPORT struct S_IF f17_S_SIS_IF(struct S_IF p0, int p1, struct S_IF p2) { return p0; }
EXPORT struct S_ID f17_S_SIS_ID(struct S_ID p0, int p1, struct S_ID p2) { return p0; }
EXPORT struct S_IP f17_S_SIS_IP(struct S_IP p0, int p1, struct S_IP p2) { return p0; }
EXPORT struct S_FI f17_S_SIS_FI(struct S_FI p0, int p1, struct S_FI p2) { return p0; }
EXPORT struct S_FF f17_S_SIS_FF(struct S_FF p0, int p1, struct S_FF p2) { return p0; }
EXPORT struct S_FD f17_S_SIS_FD(struct S_FD p0, int p1, struct S_FD p2) { return p0; }
EXPORT struct S_FP f17_S_SIS_FP(struct S_FP p0, int p1, struct S_FP p2) { return p0; }
EXPORT struct S_DI f17_S_SIS_DI(struct S_DI p0, int p1, struct S_DI p2) { return p0; }
EXPORT struct S_DF f17_S_SIS_DF(struct S_DF p0, int p1, struct S_DF p2) { return p0; }
EXPORT struct S_DD f17_S_SIS_DD(struct S_DD p0, int p1, struct S_DD p2) { return p0; }
EXPORT struct S_DP f17_S_SIS_DP(struct S_DP p0, int p1, struct S_DP p2) { return p0; }
EXPORT struct S_PI f17_S_SIS_PI(struct S_PI p0, int p1, struct S_PI p2) { return p0; }
EXPORT struct S_PF f17_S_SIS_PF(struct S_PF p0, int p1, struct S_PF p2) { return p0; }
EXPORT struct S_PD f17_S_SIS_PD(struct S_PD p0, int p1, struct S_PD p2) { return p0; }
EXPORT struct S_PP f17_S_SIS_PP(struct S_PP p0, int p1, struct S_PP p2) { return p0; }
EXPORT struct S_III f17_S_SIS_III(struct S_III p0, int p1, struct S_III p2) { return p0; }
EXPORT struct S_IIF f17_S_SIS_IIF(struct S_IIF p0, int p1, struct S_IIF p2) { return p0; }
EXPORT struct S_IID f17_S_SIS_IID(struct S_IID p0, int p1, struct S_IID p2) { return p0; }
EXPORT struct S_IIP f17_S_SIS_IIP(struct S_IIP p0, int p1, struct S_IIP p2) { return p0; }
EXPORT struct S_IFI f17_S_SIS_IFI(struct S_IFI p0, int p1, struct S_IFI p2) { return p0; }
EXPORT struct S_IFF f17_S_SIS_IFF(struct S_IFF p0, int p1, struct S_IFF p2) { return p0; }
EXPORT struct S_IFD f17_S_SIS_IFD(struct S_IFD p0, int p1, struct S_IFD p2) { return p0; }
EXPORT struct S_IFP f17_S_SIS_IFP(struct S_IFP p0, int p1, struct S_IFP p2) { return p0; }
EXPORT struct S_IDI f17_S_SIS_IDI(struct S_IDI p0, int p1, struct S_IDI p2) { return p0; }
EXPORT struct S_IDF f17_S_SIS_IDF(struct S_IDF p0, int p1, struct S_IDF p2) { return p0; }
EXPORT struct S_IDD f17_S_SIS_IDD(struct S_IDD p0, int p1, struct S_IDD p2) { return p0; }
EXPORT struct S_IDP f17_S_SIS_IDP(struct S_IDP p0, int p1, struct S_IDP p2) { return p0; }
EXPORT struct S_IPI f17_S_SIS_IPI(struct S_IPI p0, int p1, struct S_IPI p2) { return p0; }
EXPORT struct S_IPF f17_S_SIS_IPF(struct S_IPF p0, int p1, struct S_IPF p2) { return p0; }
EXPORT struct S_IPD f17_S_SIS_IPD(struct S_IPD p0, int p1, struct S_IPD p2) { return p0; }
EXPORT struct S_IPP f17_S_SIS_IPP(struct S_IPP p0, int p1, struct S_IPP p2) { return p0; }
EXPORT struct S_FII f17_S_SIS_FII(struct S_FII p0, int p1, struct S_FII p2) { return p0; }
EXPORT struct S_FIF f17_S_SIS_FIF(struct S_FIF p0, int p1, struct S_FIF p2) { return p0; }
EXPORT struct S_FID f17_S_SIS_FID(struct S_FID p0, int p1, struct S_FID p2) { return p0; }
EXPORT struct S_FIP f17_S_SIS_FIP(struct S_FIP p0, int p1, struct S_FIP p2) { return p0; }
EXPORT struct S_FFI f17_S_SIS_FFI(struct S_FFI p0, int p1, struct S_FFI p2) { return p0; }
EXPORT struct S_FFF f17_S_SIS_FFF(struct S_FFF p0, int p1, struct S_FFF p2) { return p0; }
EXPORT struct S_FFD f17_S_SIS_FFD(struct S_FFD p0, int p1, struct S_FFD p2) { return p0; }
EXPORT struct S_FFP f17_S_SIS_FFP(struct S_FFP p0, int p1, struct S_FFP p2) { return p0; }
EXPORT struct S_FDI f17_S_SIS_FDI(struct S_FDI p0, int p1, struct S_FDI p2) { return p0; }
EXPORT struct S_FDF f17_S_SIS_FDF(struct S_FDF p0, int p1, struct S_FDF p2) { return p0; }
EXPORT struct S_FDD f17_S_SIS_FDD(struct S_FDD p0, int p1, struct S_FDD p2) { return p0; }
EXPORT struct S_FDP f17_S_SIS_FDP(struct S_FDP p0, int p1, struct S_FDP p2) { return p0; }
EXPORT struct S_FPI f17_S_SIS_FPI(struct S_FPI p0, int p1, struct S_FPI p2) { return p0; }
EXPORT struct S_FPF f17_S_SIS_FPF(struct S_FPF p0, int p1, struct S_FPF p2) { return p0; }
EXPORT struct S_FPD f17_S_SIS_FPD(struct S_FPD p0, int p1, struct S_FPD p2) { return p0; }
EXPORT struct S_FPP f17_S_SIS_FPP(struct S_FPP p0, int p1, struct S_FPP p2) { return p0; }
EXPORT struct S_DII f17_S_SIS_DII(struct S_DII p0, int p1, struct S_DII p2) { return p0; }
EXPORT struct S_DIF f17_S_SIS_DIF(struct S_DIF p0, int p1, struct S_DIF p2) { return p0; }
EXPORT struct S_DID f17_S_SIS_DID(struct S_DID p0, int p1, struct S_DID p2) { return p0; }
EXPORT struct S_DIP f17_S_SIS_DIP(struct S_DIP p0, int p1, struct S_DIP p2) { return p0; }
EXPORT struct S_DFI f17_S_SIS_DFI(struct S_DFI p0, int p1, struct S_DFI p2) { return p0; }
EXPORT struct S_DFF f17_S_SIS_DFF(struct S_DFF p0, int p1, struct S_DFF p2) { return p0; }
EXPORT struct S_DFD f17_S_SIS_DFD(struct S_DFD p0, int p1, struct S_DFD p2) { return p0; }
EXPORT struct S_DFP f17_S_SIS_DFP(struct S_DFP p0, int p1, struct S_DFP p2) { return p0; }
EXPORT struct S_DDI f17_S_SIS_DDI(struct S_DDI p0, int p1, struct S_DDI p2) { return p0; }
EXPORT struct S_DDF f17_S_SIS_DDF(struct S_DDF p0, int p1, struct S_DDF p2) { return p0; }
EXPORT struct S_DDD f17_S_SIS_DDD(struct S_DDD p0, int p1, struct S_DDD p2) { return p0; }
EXPORT struct S_DDP f17_S_SIS_DDP(struct S_DDP p0, int p1, struct S_DDP p2) { return p0; }
EXPORT struct S_DPI f17_S_SIS_DPI(struct S_DPI p0, int p1, struct S_DPI p2) { return p0; }
EXPORT struct S_DPF f17_S_SIS_DPF(struct S_DPF p0, int p1, struct S_DPF p2) { return p0; }
EXPORT struct S_DPD f17_S_SIS_DPD(struct S_DPD p0, int p1, struct S_DPD p2) { return p0; }
EXPORT struct S_DPP f17_S_SIS_DPP(struct S_DPP p0, int p1, struct S_DPP p2) { return p0; }
EXPORT struct S_PII f17_S_SIS_PII(struct S_PII p0, int p1, struct S_PII p2) { return p0; }
EXPORT struct S_PIF f17_S_SIS_PIF(struct S_PIF p0, int p1, struct S_PIF p2) { return p0; }
EXPORT struct S_PID f17_S_SIS_PID(struct S_PID p0, int p1, struct S_PID p2) { return p0; }
EXPORT struct S_PIP f17_S_SIS_PIP(struct S_PIP p0, int p1, struct S_PIP p2) { return p0; }
EXPORT struct S_PFI f17_S_SIS_PFI(struct S_PFI p0, int p1, struct S_PFI p2) { return p0; }
EXPORT struct S_PFF f17_S_SIS_PFF(struct S_PFF p0, int p1, struct S_PFF p2) { return p0; }
EXPORT struct S_PFD f17_S_SIS_PFD(struct S_PFD p0, int p1, struct S_PFD p2) { return p0; }
EXPORT struct S_PFP f17_S_SIS_PFP(struct S_PFP p0, int p1, struct S_PFP p2) { return p0; }
EXPORT struct S_PDI f17_S_SIS_PDI(struct S_PDI p0, int p1, struct S_PDI p2) { return p0; }
EXPORT struct S_PDF f17_S_SIS_PDF(struct S_PDF p0, int p1, struct S_PDF p2) { return p0; }
EXPORT struct S_PDD f17_S_SIS_PDD(struct S_PDD p0, int p1, struct S_PDD p2) { return p0; }
EXPORT struct S_PDP f17_S_SIS_PDP(struct S_PDP p0, int p1, struct S_PDP p2) { return p0; }
EXPORT struct S_PPI f17_S_SIS_PPI(struct S_PPI p0, int p1, struct S_PPI p2) { return p0; }
EXPORT struct S_PPF f17_S_SIS_PPF(struct S_PPF p0, int p1, struct S_PPF p2) { return p0; }
EXPORT struct S_PPD f17_S_SIS_PPD(struct S_PPD p0, int p1, struct S_PPD p2) { return p0; }
EXPORT struct S_PPP f17_S_SIS_PPP(struct S_PPP p0, int p1, struct S_PPP p2) { return p0; }
EXPORT struct S_I f17_S_SFI_I(struct S_I p0, float p1, int p2) { return p0; }
EXPORT struct S_F f17_S_SFI_F(struct S_F p0, float p1, int p2) { return p0; }
EXPORT struct S_D f17_S_SFI_D(struct S_D p0, float p1, int p2) { return p0; }
EXPORT struct S_P f17_S_SFI_P(struct S_P p0, float p1, int p2) { return p0; }
EXPORT struct S_II f17_S_SFI_II(struct S_II p0, float p1, int p2) { return p0; }
EXPORT struct S_IF f17_S_SFI_IF(struct S_IF p0, float p1, int p2) { return p0; }
EXPORT struct S_ID f17_S_SFI_ID(struct S_ID p0, float p1, int p2) { return p0; }
EXPORT struct S_IP f17_S_SFI_IP(struct S_IP p0, float p1, int p2) { return p0; }
EXPORT struct S_FI f17_S_SFI_FI(struct S_FI p0, float p1, int p2) { return p0; }
EXPORT struct S_FF f17_S_SFI_FF(struct S_FF p0, float p1, int p2) { return p0; }
EXPORT struct S_FD f17_S_SFI_FD(struct S_FD p0, float p1, int p2) { return p0; }
EXPORT struct S_FP f17_S_SFI_FP(struct S_FP p0, float p1, int p2) { return p0; }
EXPORT struct S_DI f17_S_SFI_DI(struct S_DI p0, float p1, int p2) { return p0; }
EXPORT struct S_DF f17_S_SFI_DF(struct S_DF p0, float p1, int p2) { return p0; }
EXPORT struct S_DD f17_S_SFI_DD(struct S_DD p0, float p1, int p2) { return p0; }
EXPORT struct S_DP f17_S_SFI_DP(struct S_DP p0, float p1, int p2) { return p0; }
EXPORT struct S_PI f17_S_SFI_PI(struct S_PI p0, float p1, int p2) { return p0; }
EXPORT struct S_PF f17_S_SFI_PF(struct S_PF p0, float p1, int p2) { return p0; }
EXPORT struct S_PD f17_S_SFI_PD(struct S_PD p0, float p1, int p2) { return p0; }
EXPORT struct S_PP f17_S_SFI_PP(struct S_PP p0, float p1, int p2) { return p0; }
EXPORT struct S_III f17_S_SFI_III(struct S_III p0, float p1, int p2) { return p0; }
EXPORT struct S_IIF f17_S_SFI_IIF(struct S_IIF p0, float p1, int p2) { return p0; }
EXPORT struct S_IID f17_S_SFI_IID(struct S_IID p0, float p1, int p2) { return p0; }
EXPORT struct S_IIP f17_S_SFI_IIP(struct S_IIP p0, float p1, int p2) { return p0; }
EXPORT struct S_IFI f17_S_SFI_IFI(struct S_IFI p0, float p1, int p2) { return p0; }
EXPORT struct S_IFF f17_S_SFI_IFF(struct S_IFF p0, float p1, int p2) { return p0; }
EXPORT struct S_IFD f17_S_SFI_IFD(struct S_IFD p0, float p1, int p2) { return p0; }
EXPORT struct S_IFP f17_S_SFI_IFP(struct S_IFP p0, float p1, int p2) { return p0; }
EXPORT struct S_IDI f17_S_SFI_IDI(struct S_IDI p0, float p1, int p2) { return p0; }
EXPORT struct S_IDF f17_S_SFI_IDF(struct S_IDF p0, float p1, int p2) { return p0; }
EXPORT struct S_IDD f17_S_SFI_IDD(struct S_IDD p0, float p1, int p2) { return p0; }
EXPORT struct S_IDP f17_S_SFI_IDP(struct S_IDP p0, float p1, int p2) { return p0; }
EXPORT struct S_IPI f17_S_SFI_IPI(struct S_IPI p0, float p1, int p2) { return p0; }
EXPORT struct S_IPF f17_S_SFI_IPF(struct S_IPF p0, float p1, int p2) { return p0; }
EXPORT struct S_IPD f17_S_SFI_IPD(struct S_IPD p0, float p1, int p2) { return p0; }
EXPORT struct S_IPP f17_S_SFI_IPP(struct S_IPP p0, float p1, int p2) { return p0; }
EXPORT struct S_FII f17_S_SFI_FII(struct S_FII p0, float p1, int p2) { return p0; }
EXPORT struct S_FIF f17_S_SFI_FIF(struct S_FIF p0, float p1, int p2) { return p0; }
EXPORT struct S_FID f17_S_SFI_FID(struct S_FID p0, float p1, int p2) { return p0; }
EXPORT struct S_FIP f17_S_SFI_FIP(struct S_FIP p0, float p1, int p2) { return p0; }
EXPORT struct S_FFI f17_S_SFI_FFI(struct S_FFI p0, float p1, int p2) { return p0; }
EXPORT struct S_FFF f17_S_SFI_FFF(struct S_FFF p0, float p1, int p2) { return p0; }
EXPORT struct S_FFD f17_S_SFI_FFD(struct S_FFD p0, float p1, int p2) { return p0; }
EXPORT struct S_FFP f17_S_SFI_FFP(struct S_FFP p0, float p1, int p2) { return p0; }
EXPORT struct S_FDI f17_S_SFI_FDI(struct S_FDI p0, float p1, int p2) { return p0; }
EXPORT struct S_FDF f17_S_SFI_FDF(struct S_FDF p0, float p1, int p2) { return p0; }
EXPORT struct S_FDD f17_S_SFI_FDD(struct S_FDD p0, float p1, int p2) { return p0; }
EXPORT struct S_FDP f17_S_SFI_FDP(struct S_FDP p0, float p1, int p2) { return p0; }
EXPORT struct S_FPI f17_S_SFI_FPI(struct S_FPI p0, float p1, int p2) { return p0; }
EXPORT struct S_FPF f17_S_SFI_FPF(struct S_FPF p0, float p1, int p2) { return p0; }
EXPORT struct S_FPD f17_S_SFI_FPD(struct S_FPD p0, float p1, int p2) { return p0; }
EXPORT struct S_FPP f17_S_SFI_FPP(struct S_FPP p0, float p1, int p2) { return p0; }
EXPORT struct S_DII f17_S_SFI_DII(struct S_DII p0, float p1, int p2) { return p0; }
EXPORT struct S_DIF f17_S_SFI_DIF(struct S_DIF p0, float p1, int p2) { return p0; }
EXPORT struct S_DID f17_S_SFI_DID(struct S_DID p0, float p1, int p2) { return p0; }
EXPORT struct S_DIP f17_S_SFI_DIP(struct S_DIP p0, float p1, int p2) { return p0; }
EXPORT struct S_DFI f17_S_SFI_DFI(struct S_DFI p0, float p1, int p2) { return p0; }
EXPORT struct S_DFF f17_S_SFI_DFF(struct S_DFF p0, float p1, int p2) { return p0; }
EXPORT struct S_DFD f17_S_SFI_DFD(struct S_DFD p0, float p1, int p2) { return p0; }
EXPORT struct S_DFP f17_S_SFI_DFP(struct S_DFP p0, float p1, int p2) { return p0; }
EXPORT struct S_DDI f17_S_SFI_DDI(struct S_DDI p0, float p1, int p2) { return p0; }
EXPORT struct S_DDF f17_S_SFI_DDF(struct S_DDF p0, float p1, int p2) { return p0; }
EXPORT struct S_DDD f17_S_SFI_DDD(struct S_DDD p0, float p1, int p2) { return p0; }
EXPORT struct S_DDP f17_S_SFI_DDP(struct S_DDP p0, float p1, int p2) { return p0; }
EXPORT struct S_DPI f17_S_SFI_DPI(struct S_DPI p0, float p1, int p2) { return p0; }
EXPORT struct S_DPF f17_S_SFI_DPF(struct S_DPF p0, float p1, int p2) { return p0; }
EXPORT struct S_DPD f17_S_SFI_DPD(struct S_DPD p0, float p1, int p2) { return p0; }
EXPORT struct S_DPP f17_S_SFI_DPP(struct S_DPP p0, float p1, int p2) { return p0; }
EXPORT struct S_PII f17_S_SFI_PII(struct S_PII p0, float p1, int p2) { return p0; }
EXPORT struct S_PIF f17_S_SFI_PIF(struct S_PIF p0, float p1, int p2) { return p0; }
EXPORT struct S_PID f17_S_SFI_PID(struct S_PID p0, float p1, int p2) { return p0; }
EXPORT struct S_PIP f17_S_SFI_PIP(struct S_PIP p0, float p1, int p2) { return p0; }
EXPORT struct S_PFI f17_S_SFI_PFI(struct S_PFI p0, float p1, int p2) { return p0; }
EXPORT struct S_PFF f17_S_SFI_PFF(struct S_PFF p0, float p1, int p2) { return p0; }
EXPORT struct S_PFD f17_S_SFI_PFD(struct S_PFD p0, float p1, int p2) { return p0; }
EXPORT struct S_PFP f17_S_SFI_PFP(struct S_PFP p0, float p1, int p2) { return p0; }
EXPORT struct S_PDI f17_S_SFI_PDI(struct S_PDI p0, float p1, int p2) { return p0; }
EXPORT struct S_PDF f17_S_SFI_PDF(struct S_PDF p0, float p1, int p2) { return p0; }
EXPORT struct S_PDD f17_S_SFI_PDD(struct S_PDD p0, float p1, int p2) { return p0; }
EXPORT struct S_PDP f17_S_SFI_PDP(struct S_PDP p0, float p1, int p2) { return p0; }
EXPORT struct S_PPI f17_S_SFI_PPI(struct S_PPI p0, float p1, int p2) { return p0; }
EXPORT struct S_PPF f17_S_SFI_PPF(struct S_PPF p0, float p1, int p2) { return p0; }
EXPORT struct S_PPD f17_S_SFI_PPD(struct S_PPD p0, float p1, int p2) { return p0; }
EXPORT struct S_PPP f17_S_SFI_PPP(struct S_PPP p0, float p1, int p2) { return p0; }
EXPORT struct S_I f17_S_SFF_I(struct S_I p0, float p1, float p2) { return p0; }
EXPORT struct S_F f17_S_SFF_F(struct S_F p0, float p1, float p2) { return p0; }
EXPORT struct S_D f17_S_SFF_D(struct S_D p0, float p1, float p2) { return p0; }
EXPORT struct S_P f17_S_SFF_P(struct S_P p0, float p1, float p2) { return p0; }
EXPORT struct S_II f17_S_SFF_II(struct S_II p0, float p1, float p2) { return p0; }
EXPORT struct S_IF f17_S_SFF_IF(struct S_IF p0, float p1, float p2) { return p0; }
EXPORT struct S_ID f17_S_SFF_ID(struct S_ID p0, float p1, float p2) { return p0; }
EXPORT struct S_IP f17_S_SFF_IP(struct S_IP p0, float p1, float p2) { return p0; }
EXPORT struct S_FI f17_S_SFF_FI(struct S_FI p0, float p1, float p2) { return p0; }
EXPORT struct S_FF f17_S_SFF_FF(struct S_FF p0, float p1, float p2) { return p0; }
EXPORT struct S_FD f17_S_SFF_FD(struct S_FD p0, float p1, float p2) { return p0; }
EXPORT struct S_FP f17_S_SFF_FP(struct S_FP p0, float p1, float p2) { return p0; }
EXPORT struct S_DI f17_S_SFF_DI(struct S_DI p0, float p1, float p2) { return p0; }
EXPORT struct S_DF f17_S_SFF_DF(struct S_DF p0, float p1, float p2) { return p0; }
EXPORT struct S_DD f17_S_SFF_DD(struct S_DD p0, float p1, float p2) { return p0; }
EXPORT struct S_DP f17_S_SFF_DP(struct S_DP p0, float p1, float p2) { return p0; }
EXPORT struct S_PI f17_S_SFF_PI(struct S_PI p0, float p1, float p2) { return p0; }
EXPORT struct S_PF f17_S_SFF_PF(struct S_PF p0, float p1, float p2) { return p0; }
EXPORT struct S_PD f17_S_SFF_PD(struct S_PD p0, float p1, float p2) { return p0; }
EXPORT struct S_PP f17_S_SFF_PP(struct S_PP p0, float p1, float p2) { return p0; }
EXPORT struct S_III f17_S_SFF_III(struct S_III p0, float p1, float p2) { return p0; }
EXPORT struct S_IIF f17_S_SFF_IIF(struct S_IIF p0, float p1, float p2) { return p0; }
EXPORT struct S_IID f17_S_SFF_IID(struct S_IID p0, float p1, float p2) { return p0; }
EXPORT struct S_IIP f17_S_SFF_IIP(struct S_IIP p0, float p1, float p2) { return p0; }
EXPORT struct S_IFI f17_S_SFF_IFI(struct S_IFI p0, float p1, float p2) { return p0; }
EXPORT struct S_IFF f17_S_SFF_IFF(struct S_IFF p0, float p1, float p2) { return p0; }
EXPORT struct S_IFD f17_S_SFF_IFD(struct S_IFD p0, float p1, float p2) { return p0; }
EXPORT struct S_IFP f17_S_SFF_IFP(struct S_IFP p0, float p1, float p2) { return p0; }
EXPORT struct S_IDI f17_S_SFF_IDI(struct S_IDI p0, float p1, float p2) { return p0; }
EXPORT struct S_IDF f17_S_SFF_IDF(struct S_IDF p0, float p1, float p2) { return p0; }
EXPORT struct S_IDD f17_S_SFF_IDD(struct S_IDD p0, float p1, float p2) { return p0; }
EXPORT struct S_IDP f17_S_SFF_IDP(struct S_IDP p0, float p1, float p2) { return p0; }
EXPORT struct S_IPI f17_S_SFF_IPI(struct S_IPI p0, float p1, float p2) { return p0; }
EXPORT struct S_IPF f17_S_SFF_IPF(struct S_IPF p0, float p1, float p2) { return p0; }
EXPORT struct S_IPD f17_S_SFF_IPD(struct S_IPD p0, float p1, float p2) { return p0; }
EXPORT struct S_IPP f17_S_SFF_IPP(struct S_IPP p0, float p1, float p2) { return p0; }
EXPORT struct S_FII f17_S_SFF_FII(struct S_FII p0, float p1, float p2) { return p0; }
EXPORT struct S_FIF f17_S_SFF_FIF(struct S_FIF p0, float p1, float p2) { return p0; }
EXPORT struct S_FID f17_S_SFF_FID(struct S_FID p0, float p1, float p2) { return p0; }
EXPORT struct S_FIP f17_S_SFF_FIP(struct S_FIP p0, float p1, float p2) { return p0; }
EXPORT struct S_FFI f17_S_SFF_FFI(struct S_FFI p0, float p1, float p2) { return p0; }
EXPORT struct S_FFF f17_S_SFF_FFF(struct S_FFF p0, float p1, float p2) { return p0; }
EXPORT struct S_FFD f17_S_SFF_FFD(struct S_FFD p0, float p1, float p2) { return p0; }
EXPORT struct S_FFP f17_S_SFF_FFP(struct S_FFP p0, float p1, float p2) { return p0; }
EXPORT struct S_FDI f17_S_SFF_FDI(struct S_FDI p0, float p1, float p2) { return p0; }
EXPORT struct S_FDF f17_S_SFF_FDF(struct S_FDF p0, float p1, float p2) { return p0; }
EXPORT struct S_FDD f17_S_SFF_FDD(struct S_FDD p0, float p1, float p2) { return p0; }
EXPORT struct S_FDP f17_S_SFF_FDP(struct S_FDP p0, float p1, float p2) { return p0; }
EXPORT struct S_FPI f17_S_SFF_FPI(struct S_FPI p0, float p1, float p2) { return p0; }
EXPORT struct S_FPF f17_S_SFF_FPF(struct S_FPF p0, float p1, float p2) { return p0; }
EXPORT struct S_FPD f17_S_SFF_FPD(struct S_FPD p0, float p1, float p2) { return p0; }
EXPORT struct S_FPP f17_S_SFF_FPP(struct S_FPP p0, float p1, float p2) { return p0; }
EXPORT struct S_DII f17_S_SFF_DII(struct S_DII p0, float p1, float p2) { return p0; }
EXPORT struct S_DIF f17_S_SFF_DIF(struct S_DIF p0, float p1, float p2) { return p0; }
EXPORT struct S_DID f17_S_SFF_DID(struct S_DID p0, float p1, float p2) { return p0; }
EXPORT struct S_DIP f17_S_SFF_DIP(struct S_DIP p0, float p1, float p2) { return p0; }
EXPORT struct S_DFI f17_S_SFF_DFI(struct S_DFI p0, float p1, float p2) { return p0; }
EXPORT struct S_DFF f17_S_SFF_DFF(struct S_DFF p0, float p1, float p2) { return p0; }
EXPORT struct S_DFD f17_S_SFF_DFD(struct S_DFD p0, float p1, float p2) { return p0; }
EXPORT struct S_DFP f17_S_SFF_DFP(struct S_DFP p0, float p1, float p2) { return p0; }
EXPORT struct S_DDI f17_S_SFF_DDI(struct S_DDI p0, float p1, float p2) { return p0; }
EXPORT struct S_DDF f17_S_SFF_DDF(struct S_DDF p0, float p1, float p2) { return p0; }
EXPORT struct S_DDD f17_S_SFF_DDD(struct S_DDD p0, float p1, float p2) { return p0; }
EXPORT struct S_DDP f17_S_SFF_DDP(struct S_DDP p0, float p1, float p2) { return p0; }
EXPORT struct S_DPI f17_S_SFF_DPI(struct S_DPI p0, float p1, float p2) { return p0; }
EXPORT struct S_DPF f17_S_SFF_DPF(struct S_DPF p0, float p1, float p2) { return p0; }
EXPORT struct S_DPD f17_S_SFF_DPD(struct S_DPD p0, float p1, float p2) { return p0; }
EXPORT struct S_DPP f17_S_SFF_DPP(struct S_DPP p0, float p1, float p2) { return p0; }
EXPORT struct S_PII f17_S_SFF_PII(struct S_PII p0, float p1, float p2) { return p0; }
EXPORT struct S_PIF f17_S_SFF_PIF(struct S_PIF p0, float p1, float p2) { return p0; }
EXPORT struct S_PID f17_S_SFF_PID(struct S_PID p0, float p1, float p2) { return p0; }
EXPORT struct S_PIP f17_S_SFF_PIP(struct S_PIP p0, float p1, float p2) { return p0; }
EXPORT struct S_PFI f17_S_SFF_PFI(struct S_PFI p0, float p1, float p2) { return p0; }
EXPORT struct S_PFF f17_S_SFF_PFF(struct S_PFF p0, float p1, float p2) { return p0; }
EXPORT struct S_PFD f17_S_SFF_PFD(struct S_PFD p0, float p1, float p2) { return p0; }
EXPORT struct S_PFP f17_S_SFF_PFP(struct S_PFP p0, float p1, float p2) { return p0; }
EXPORT struct S_PDI f17_S_SFF_PDI(struct S_PDI p0, float p1, float p2) { return p0; }
EXPORT struct S_PDF f17_S_SFF_PDF(struct S_PDF p0, float p1, float p2) { return p0; }
EXPORT struct S_PDD f17_S_SFF_PDD(struct S_PDD p0, float p1, float p2) { return p0; }
EXPORT struct S_PDP f17_S_SFF_PDP(struct S_PDP p0, float p1, float p2) { return p0; }
EXPORT struct S_PPI f17_S_SFF_PPI(struct S_PPI p0, float p1, float p2) { return p0; }
EXPORT struct S_PPF f17_S_SFF_PPF(struct S_PPF p0, float p1, float p2) { return p0; }
EXPORT struct S_PPD f17_S_SFF_PPD(struct S_PPD p0, float p1, float p2) { return p0; }
EXPORT struct S_PPP f17_S_SFF_PPP(struct S_PPP p0, float p1, float p2) { return p0; }
EXPORT struct S_I f17_S_SFD_I(struct S_I p0, float p1, double p2) { return p0; }
EXPORT struct S_F f17_S_SFD_F(struct S_F p0, float p1, double p2) { return p0; }
EXPORT struct S_D f17_S_SFD_D(struct S_D p0, float p1, double p2) { return p0; }
EXPORT struct S_P f17_S_SFD_P(struct S_P p0, float p1, double p2) { return p0; }
EXPORT struct S_II f17_S_SFD_II(struct S_II p0, float p1, double p2) { return p0; }
EXPORT struct S_IF f17_S_SFD_IF(struct S_IF p0, float p1, double p2) { return p0; }
EXPORT struct S_ID f17_S_SFD_ID(struct S_ID p0, float p1, double p2) { return p0; }
EXPORT struct S_IP f17_S_SFD_IP(struct S_IP p0, float p1, double p2) { return p0; }
EXPORT struct S_FI f17_S_SFD_FI(struct S_FI p0, float p1, double p2) { return p0; }
EXPORT struct S_FF f17_S_SFD_FF(struct S_FF p0, float p1, double p2) { return p0; }
EXPORT struct S_FD f17_S_SFD_FD(struct S_FD p0, float p1, double p2) { return p0; }
EXPORT struct S_FP f17_S_SFD_FP(struct S_FP p0, float p1, double p2) { return p0; }
EXPORT struct S_DI f17_S_SFD_DI(struct S_DI p0, float p1, double p2) { return p0; }
EXPORT struct S_DF f17_S_SFD_DF(struct S_DF p0, float p1, double p2) { return p0; }
EXPORT struct S_DD f17_S_SFD_DD(struct S_DD p0, float p1, double p2) { return p0; }
EXPORT struct S_DP f17_S_SFD_DP(struct S_DP p0, float p1, double p2) { return p0; }
EXPORT struct S_PI f17_S_SFD_PI(struct S_PI p0, float p1, double p2) { return p0; }
EXPORT struct S_PF f17_S_SFD_PF(struct S_PF p0, float p1, double p2) { return p0; }
EXPORT struct S_PD f17_S_SFD_PD(struct S_PD p0, float p1, double p2) { return p0; }
EXPORT struct S_PP f17_S_SFD_PP(struct S_PP p0, float p1, double p2) { return p0; }
EXPORT struct S_III f17_S_SFD_III(struct S_III p0, float p1, double p2) { return p0; }
EXPORT struct S_IIF f17_S_SFD_IIF(struct S_IIF p0, float p1, double p2) { return p0; }
EXPORT struct S_IID f17_S_SFD_IID(struct S_IID p0, float p1, double p2) { return p0; }
EXPORT struct S_IIP f17_S_SFD_IIP(struct S_IIP p0, float p1, double p2) { return p0; }
EXPORT struct S_IFI f17_S_SFD_IFI(struct S_IFI p0, float p1, double p2) { return p0; }
EXPORT struct S_IFF f17_S_SFD_IFF(struct S_IFF p0, float p1, double p2) { return p0; }
EXPORT struct S_IFD f17_S_SFD_IFD(struct S_IFD p0, float p1, double p2) { return p0; }
EXPORT struct S_IFP f17_S_SFD_IFP(struct S_IFP p0, float p1, double p2) { return p0; }
EXPORT struct S_IDI f17_S_SFD_IDI(struct S_IDI p0, float p1, double p2) { return p0; }
EXPORT struct S_IDF f17_S_SFD_IDF(struct S_IDF p0, float p1, double p2) { return p0; }
EXPORT struct S_IDD f17_S_SFD_IDD(struct S_IDD p0, float p1, double p2) { return p0; }
EXPORT struct S_IDP f17_S_SFD_IDP(struct S_IDP p0, float p1, double p2) { return p0; }
EXPORT struct S_IPI f17_S_SFD_IPI(struct S_IPI p0, float p1, double p2) { return p0; }
EXPORT struct S_IPF f17_S_SFD_IPF(struct S_IPF p0, float p1, double p2) { return p0; }
EXPORT struct S_IPD f17_S_SFD_IPD(struct S_IPD p0, float p1, double p2) { return p0; }
EXPORT struct S_IPP f17_S_SFD_IPP(struct S_IPP p0, float p1, double p2) { return p0; }
EXPORT struct S_FII f17_S_SFD_FII(struct S_FII p0, float p1, double p2) { return p0; }
EXPORT struct S_FIF f17_S_SFD_FIF(struct S_FIF p0, float p1, double p2) { return p0; }
EXPORT struct S_FID f17_S_SFD_FID(struct S_FID p0, float p1, double p2) { return p0; }
EXPORT struct S_FIP f17_S_SFD_FIP(struct S_FIP p0, float p1, double p2) { return p0; }
EXPORT struct S_FFI f17_S_SFD_FFI(struct S_FFI p0, float p1, double p2) { return p0; }
EXPORT struct S_FFF f17_S_SFD_FFF(struct S_FFF p0, float p1, double p2) { return p0; }
EXPORT struct S_FFD f17_S_SFD_FFD(struct S_FFD p0, float p1, double p2) { return p0; }
EXPORT struct S_FFP f17_S_SFD_FFP(struct S_FFP p0, float p1, double p2) { return p0; }
EXPORT struct S_FDI f17_S_SFD_FDI(struct S_FDI p0, float p1, double p2) { return p0; }
EXPORT struct S_FDF f17_S_SFD_FDF(struct S_FDF p0, float p1, double p2) { return p0; }
EXPORT struct S_FDD f17_S_SFD_FDD(struct S_FDD p0, float p1, double p2) { return p0; }
EXPORT struct S_FDP f17_S_SFD_FDP(struct S_FDP p0, float p1, double p2) { return p0; }
EXPORT struct S_FPI f17_S_SFD_FPI(struct S_FPI p0, float p1, double p2) { return p0; }
EXPORT struct S_FPF f17_S_SFD_FPF(struct S_FPF p0, float p1, double p2) { return p0; }
EXPORT struct S_FPD f17_S_SFD_FPD(struct S_FPD p0, float p1, double p2) { return p0; }
EXPORT struct S_FPP f17_S_SFD_FPP(struct S_FPP p0, float p1, double p2) { return p0; }
EXPORT struct S_DII f17_S_SFD_DII(struct S_DII p0, float p1, double p2) { return p0; }
EXPORT struct S_DIF f17_S_SFD_DIF(struct S_DIF p0, float p1, double p2) { return p0; }
EXPORT struct S_DID f17_S_SFD_DID(struct S_DID p0, float p1, double p2) { return p0; }
EXPORT struct S_DIP f17_S_SFD_DIP(struct S_DIP p0, float p1, double p2) { return p0; }
EXPORT struct S_DFI f17_S_SFD_DFI(struct S_DFI p0, float p1, double p2) { return p0; }
EXPORT struct S_DFF f17_S_SFD_DFF(struct S_DFF p0, float p1, double p2) { return p0; }
EXPORT struct S_DFD f17_S_SFD_DFD(struct S_DFD p0, float p1, double p2) { return p0; }
EXPORT struct S_DFP f17_S_SFD_DFP(struct S_DFP p0, float p1, double p2) { return p0; }
EXPORT struct S_DDI f17_S_SFD_DDI(struct S_DDI p0, float p1, double p2) { return p0; }
EXPORT struct S_DDF f17_S_SFD_DDF(struct S_DDF p0, float p1, double p2) { return p0; }
EXPORT struct S_DDD f17_S_SFD_DDD(struct S_DDD p0, float p1, double p2) { return p0; }
EXPORT struct S_DDP f17_S_SFD_DDP(struct S_DDP p0, float p1, double p2) { return p0; }
EXPORT struct S_DPI f17_S_SFD_DPI(struct S_DPI p0, float p1, double p2) { return p0; }
EXPORT struct S_DPF f17_S_SFD_DPF(struct S_DPF p0, float p1, double p2) { return p0; }
EXPORT struct S_DPD f17_S_SFD_DPD(struct S_DPD p0, float p1, double p2) { return p0; }
EXPORT struct S_DPP f17_S_SFD_DPP(struct S_DPP p0, float p1, double p2) { return p0; }
EXPORT struct S_PII f17_S_SFD_PII(struct S_PII p0, float p1, double p2) { return p0; }
EXPORT struct S_PIF f17_S_SFD_PIF(struct S_PIF p0, float p1, double p2) { return p0; }
EXPORT struct S_PID f17_S_SFD_PID(struct S_PID p0, float p1, double p2) { return p0; }
EXPORT struct S_PIP f17_S_SFD_PIP(struct S_PIP p0, float p1, double p2) { return p0; }
EXPORT struct S_PFI f17_S_SFD_PFI(struct S_PFI p0, float p1, double p2) { return p0; }
EXPORT struct S_PFF f17_S_SFD_PFF(struct S_PFF p0, float p1, double p2) { return p0; }
EXPORT struct S_PFD f17_S_SFD_PFD(struct S_PFD p0, float p1, double p2) { return p0; }
EXPORT struct S_PFP f17_S_SFD_PFP(struct S_PFP p0, float p1, double p2) { return p0; }
EXPORT struct S_PDI f17_S_SFD_PDI(struct S_PDI p0, float p1, double p2) { return p0; }
EXPORT struct S_PDF f17_S_SFD_PDF(struct S_PDF p0, float p1, double p2) { return p0; }
EXPORT struct S_PDD f17_S_SFD_PDD(struct S_PDD p0, float p1, double p2) { return p0; }
EXPORT struct S_PDP f17_S_SFD_PDP(struct S_PDP p0, float p1, double p2) { return p0; }
EXPORT struct S_PPI f17_S_SFD_PPI(struct S_PPI p0, float p1, double p2) { return p0; }
EXPORT struct S_PPF f17_S_SFD_PPF(struct S_PPF p0, float p1, double p2) { return p0; }
EXPORT struct S_PPD f17_S_SFD_PPD(struct S_PPD p0, float p1, double p2) { return p0; }
EXPORT struct S_PPP f17_S_SFD_PPP(struct S_PPP p0, float p1, double p2) { return p0; }
EXPORT struct S_I f17_S_SFP_I(struct S_I p0, float p1, void* p2) { return p0; }
EXPORT struct S_F f17_S_SFP_F(struct S_F p0, float p1, void* p2) { return p0; }
EXPORT struct S_D f17_S_SFP_D(struct S_D p0, float p1, void* p2) { return p0; }
EXPORT struct S_P f17_S_SFP_P(struct S_P p0, float p1, void* p2) { return p0; }
EXPORT struct S_II f17_S_SFP_II(struct S_II p0, float p1, void* p2) { return p0; }
EXPORT struct S_IF f17_S_SFP_IF(struct S_IF p0, float p1, void* p2) { return p0; }
EXPORT struct S_ID f17_S_SFP_ID(struct S_ID p0, float p1, void* p2) { return p0; }
EXPORT struct S_IP f17_S_SFP_IP(struct S_IP p0, float p1, void* p2) { return p0; }
EXPORT struct S_FI f17_S_SFP_FI(struct S_FI p0, float p1, void* p2) { return p0; }
EXPORT struct S_FF f17_S_SFP_FF(struct S_FF p0, float p1, void* p2) { return p0; }
EXPORT struct S_FD f17_S_SFP_FD(struct S_FD p0, float p1, void* p2) { return p0; }
EXPORT struct S_FP f17_S_SFP_FP(struct S_FP p0, float p1, void* p2) { return p0; }
EXPORT struct S_DI f17_S_SFP_DI(struct S_DI p0, float p1, void* p2) { return p0; }
EXPORT struct S_DF f17_S_SFP_DF(struct S_DF p0, float p1, void* p2) { return p0; }
EXPORT struct S_DD f17_S_SFP_DD(struct S_DD p0, float p1, void* p2) { return p0; }
EXPORT struct S_DP f17_S_SFP_DP(struct S_DP p0, float p1, void* p2) { return p0; }
EXPORT struct S_PI f17_S_SFP_PI(struct S_PI p0, float p1, void* p2) { return p0; }
EXPORT struct S_PF f17_S_SFP_PF(struct S_PF p0, float p1, void* p2) { return p0; }
EXPORT struct S_PD f17_S_SFP_PD(struct S_PD p0, float p1, void* p2) { return p0; }
EXPORT struct S_PP f17_S_SFP_PP(struct S_PP p0, float p1, void* p2) { return p0; }
EXPORT struct S_III f17_S_SFP_III(struct S_III p0, float p1, void* p2) { return p0; }
EXPORT struct S_IIF f17_S_SFP_IIF(struct S_IIF p0, float p1, void* p2) { return p0; }
EXPORT struct S_IID f17_S_SFP_IID(struct S_IID p0, float p1, void* p2) { return p0; }
EXPORT struct S_IIP f17_S_SFP_IIP(struct S_IIP p0, float p1, void* p2) { return p0; }
EXPORT struct S_IFI f17_S_SFP_IFI(struct S_IFI p0, float p1, void* p2) { return p0; }
EXPORT struct S_IFF f17_S_SFP_IFF(struct S_IFF p0, float p1, void* p2) { return p0; }
EXPORT struct S_IFD f17_S_SFP_IFD(struct S_IFD p0, float p1, void* p2) { return p0; }
EXPORT struct S_IFP f17_S_SFP_IFP(struct S_IFP p0, float p1, void* p2) { return p0; }
EXPORT struct S_IDI f17_S_SFP_IDI(struct S_IDI p0, float p1, void* p2) { return p0; }
EXPORT struct S_IDF f17_S_SFP_IDF(struct S_IDF p0, float p1, void* p2) { return p0; }
EXPORT struct S_IDD f17_S_SFP_IDD(struct S_IDD p0, float p1, void* p2) { return p0; }
EXPORT struct S_IDP f17_S_SFP_IDP(struct S_IDP p0, float p1, void* p2) { return p0; }
EXPORT struct S_IPI f17_S_SFP_IPI(struct S_IPI p0, float p1, void* p2) { return p0; }
EXPORT struct S_IPF f17_S_SFP_IPF(struct S_IPF p0, float p1, void* p2) { return p0; }
EXPORT struct S_IPD f17_S_SFP_IPD(struct S_IPD p0, float p1, void* p2) { return p0; }
EXPORT struct S_IPP f17_S_SFP_IPP(struct S_IPP p0, float p1, void* p2) { return p0; }
EXPORT struct S_FII f17_S_SFP_FII(struct S_FII p0, float p1, void* p2) { return p0; }
EXPORT struct S_FIF f17_S_SFP_FIF(struct S_FIF p0, float p1, void* p2) { return p0; }
EXPORT struct S_FID f17_S_SFP_FID(struct S_FID p0, float p1, void* p2) { return p0; }
EXPORT struct S_FIP f17_S_SFP_FIP(struct S_FIP p0, float p1, void* p2) { return p0; }
EXPORT struct S_FFI f17_S_SFP_FFI(struct S_FFI p0, float p1, void* p2) { return p0; }
EXPORT struct S_FFF f17_S_SFP_FFF(struct S_FFF p0, float p1, void* p2) { return p0; }
EXPORT struct S_FFD f17_S_SFP_FFD(struct S_FFD p0, float p1, void* p2) { return p0; }
EXPORT struct S_FFP f17_S_SFP_FFP(struct S_FFP p0, float p1, void* p2) { return p0; }
EXPORT struct S_FDI f17_S_SFP_FDI(struct S_FDI p0, float p1, void* p2) { return p0; }
EXPORT struct S_FDF f17_S_SFP_FDF(struct S_FDF p0, float p1, void* p2) { return p0; }
EXPORT struct S_FDD f17_S_SFP_FDD(struct S_FDD p0, float p1, void* p2) { return p0; }
EXPORT struct S_FDP f17_S_SFP_FDP(struct S_FDP p0, float p1, void* p2) { return p0; }
EXPORT struct S_FPI f17_S_SFP_FPI(struct S_FPI p0, float p1, void* p2) { return p0; }
EXPORT struct S_FPF f17_S_SFP_FPF(struct S_FPF p0, float p1, void* p2) { return p0; }
EXPORT struct S_FPD f17_S_SFP_FPD(struct S_FPD p0, float p1, void* p2) { return p0; }
EXPORT struct S_FPP f17_S_SFP_FPP(struct S_FPP p0, float p1, void* p2) { return p0; }
EXPORT struct S_DII f17_S_SFP_DII(struct S_DII p0, float p1, void* p2) { return p0; }
EXPORT struct S_DIF f17_S_SFP_DIF(struct S_DIF p0, float p1, void* p2) { return p0; }
EXPORT struct S_DID f17_S_SFP_DID(struct S_DID p0, float p1, void* p2) { return p0; }
EXPORT struct S_DIP f17_S_SFP_DIP(struct S_DIP p0, float p1, void* p2) { return p0; }
EXPORT struct S_DFI f17_S_SFP_DFI(struct S_DFI p0, float p1, void* p2) { return p0; }
EXPORT struct S_DFF f17_S_SFP_DFF(struct S_DFF p0, float p1, void* p2) { return p0; }
EXPORT struct S_DFD f17_S_SFP_DFD(struct S_DFD p0, float p1, void* p2) { return p0; }
EXPORT struct S_DFP f17_S_SFP_DFP(struct S_DFP p0, float p1, void* p2) { return p0; }
EXPORT struct S_DDI f17_S_SFP_DDI(struct S_DDI p0, float p1, void* p2) { return p0; }
EXPORT struct S_DDF f17_S_SFP_DDF(struct S_DDF p0, float p1, void* p2) { return p0; }
EXPORT struct S_DDD f17_S_SFP_DDD(struct S_DDD p0, float p1, void* p2) { return p0; }
EXPORT struct S_DDP f17_S_SFP_DDP(struct S_DDP p0, float p1, void* p2) { return p0; }
EXPORT struct S_DPI f17_S_SFP_DPI(struct S_DPI p0, float p1, void* p2) { return p0; }
EXPORT struct S_DPF f17_S_SFP_DPF(struct S_DPF p0, float p1, void* p2) { return p0; }
EXPORT struct S_DPD f17_S_SFP_DPD(struct S_DPD p0, float p1, void* p2) { return p0; }
EXPORT struct S_DPP f17_S_SFP_DPP(struct S_DPP p0, float p1, void* p2) { return p0; }
EXPORT struct S_PII f17_S_SFP_PII(struct S_PII p0, float p1, void* p2) { return p0; }
EXPORT struct S_PIF f17_S_SFP_PIF(struct S_PIF p0, float p1, void* p2) { return p0; }
EXPORT struct S_PID f17_S_SFP_PID(struct S_PID p0, float p1, void* p2) { return p0; }
EXPORT struct S_PIP f17_S_SFP_PIP(struct S_PIP p0, float p1, void* p2) { return p0; }
EXPORT struct S_PFI f17_S_SFP_PFI(struct S_PFI p0, float p1, void* p2) { return p0; }
EXPORT struct S_PFF f17_S_SFP_PFF(struct S_PFF p0, float p1, void* p2) { return p0; }
EXPORT struct S_PFD f17_S_SFP_PFD(struct S_PFD p0, float p1, void* p2) { return p0; }
EXPORT struct S_PFP f17_S_SFP_PFP(struct S_PFP p0, float p1, void* p2) { return p0; }
EXPORT struct S_PDI f17_S_SFP_PDI(struct S_PDI p0, float p1, void* p2) { return p0; }
EXPORT struct S_PDF f17_S_SFP_PDF(struct S_PDF p0, float p1, void* p2) { return p0; }
EXPORT struct S_PDD f17_S_SFP_PDD(struct S_PDD p0, float p1, void* p2) { return p0; }
EXPORT struct S_PDP f17_S_SFP_PDP(struct S_PDP p0, float p1, void* p2) { return p0; }
EXPORT struct S_PPI f17_S_SFP_PPI(struct S_PPI p0, float p1, void* p2) { return p0; }
EXPORT struct S_PPF f17_S_SFP_PPF(struct S_PPF p0, float p1, void* p2) { return p0; }
EXPORT struct S_PPD f17_S_SFP_PPD(struct S_PPD p0, float p1, void* p2) { return p0; }
EXPORT struct S_PPP f17_S_SFP_PPP(struct S_PPP p0, float p1, void* p2) { return p0; }
EXPORT struct S_I f17_S_SFS_I(struct S_I p0, float p1, struct S_I p2) { return p0; }
EXPORT struct S_F f17_S_SFS_F(struct S_F p0, float p1, struct S_F p2) { return p0; }
EXPORT struct S_D f17_S_SFS_D(struct S_D p0, float p1, struct S_D p2) { return p0; }
EXPORT struct S_P f17_S_SFS_P(struct S_P p0, float p1, struct S_P p2) { return p0; }
EXPORT struct S_II f17_S_SFS_II(struct S_II p0, float p1, struct S_II p2) { return p0; }
EXPORT struct S_IF f17_S_SFS_IF(struct S_IF p0, float p1, struct S_IF p2) { return p0; }
EXPORT struct S_ID f17_S_SFS_ID(struct S_ID p0, float p1, struct S_ID p2) { return p0; }
EXPORT struct S_IP f17_S_SFS_IP(struct S_IP p0, float p1, struct S_IP p2) { return p0; }
EXPORT struct S_FI f17_S_SFS_FI(struct S_FI p0, float p1, struct S_FI p2) { return p0; }
EXPORT struct S_FF f17_S_SFS_FF(struct S_FF p0, float p1, struct S_FF p2) { return p0; }
EXPORT struct S_FD f17_S_SFS_FD(struct S_FD p0, float p1, struct S_FD p2) { return p0; }
EXPORT struct S_FP f17_S_SFS_FP(struct S_FP p0, float p1, struct S_FP p2) { return p0; }
EXPORT struct S_DI f17_S_SFS_DI(struct S_DI p0, float p1, struct S_DI p2) { return p0; }
EXPORT struct S_DF f17_S_SFS_DF(struct S_DF p0, float p1, struct S_DF p2) { return p0; }
EXPORT struct S_DD f17_S_SFS_DD(struct S_DD p0, float p1, struct S_DD p2) { return p0; }
EXPORT struct S_DP f17_S_SFS_DP(struct S_DP p0, float p1, struct S_DP p2) { return p0; }
EXPORT struct S_PI f17_S_SFS_PI(struct S_PI p0, float p1, struct S_PI p2) { return p0; }
EXPORT struct S_PF f17_S_SFS_PF(struct S_PF p0, float p1, struct S_PF p2) { return p0; }
EXPORT struct S_PD f17_S_SFS_PD(struct S_PD p0, float p1, struct S_PD p2) { return p0; }
EXPORT struct S_PP f17_S_SFS_PP(struct S_PP p0, float p1, struct S_PP p2) { return p0; }
EXPORT struct S_III f17_S_SFS_III(struct S_III p0, float p1, struct S_III p2) { return p0; }
EXPORT struct S_IIF f17_S_SFS_IIF(struct S_IIF p0, float p1, struct S_IIF p2) { return p0; }
EXPORT struct S_IID f17_S_SFS_IID(struct S_IID p0, float p1, struct S_IID p2) { return p0; }
EXPORT struct S_IIP f17_S_SFS_IIP(struct S_IIP p0, float p1, struct S_IIP p2) { return p0; }
EXPORT struct S_IFI f17_S_SFS_IFI(struct S_IFI p0, float p1, struct S_IFI p2) { return p0; }
EXPORT struct S_IFF f17_S_SFS_IFF(struct S_IFF p0, float p1, struct S_IFF p2) { return p0; }
EXPORT struct S_IFD f17_S_SFS_IFD(struct S_IFD p0, float p1, struct S_IFD p2) { return p0; }
EXPORT struct S_IFP f17_S_SFS_IFP(struct S_IFP p0, float p1, struct S_IFP p2) { return p0; }
EXPORT struct S_IDI f17_S_SFS_IDI(struct S_IDI p0, float p1, struct S_IDI p2) { return p0; }
EXPORT struct S_IDF f17_S_SFS_IDF(struct S_IDF p0, float p1, struct S_IDF p2) { return p0; }
EXPORT struct S_IDD f17_S_SFS_IDD(struct S_IDD p0, float p1, struct S_IDD p2) { return p0; }
EXPORT struct S_IDP f17_S_SFS_IDP(struct S_IDP p0, float p1, struct S_IDP p2) { return p0; }
EXPORT struct S_IPI f17_S_SFS_IPI(struct S_IPI p0, float p1, struct S_IPI p2) { return p0; }
EXPORT struct S_IPF f17_S_SFS_IPF(struct S_IPF p0, float p1, struct S_IPF p2) { return p0; }
EXPORT struct S_IPD f17_S_SFS_IPD(struct S_IPD p0, float p1, struct S_IPD p2) { return p0; }
EXPORT struct S_IPP f17_S_SFS_IPP(struct S_IPP p0, float p1, struct S_IPP p2) { return p0; }
EXPORT struct S_FII f17_S_SFS_FII(struct S_FII p0, float p1, struct S_FII p2) { return p0; }
EXPORT struct S_FIF f17_S_SFS_FIF(struct S_FIF p0, float p1, struct S_FIF p2) { return p0; }
EXPORT struct S_FID f17_S_SFS_FID(struct S_FID p0, float p1, struct S_FID p2) { return p0; }
EXPORT struct S_FIP f17_S_SFS_FIP(struct S_FIP p0, float p1, struct S_FIP p2) { return p0; }
EXPORT struct S_FFI f17_S_SFS_FFI(struct S_FFI p0, float p1, struct S_FFI p2) { return p0; }
EXPORT struct S_FFF f17_S_SFS_FFF(struct S_FFF p0, float p1, struct S_FFF p2) { return p0; }
EXPORT struct S_FFD f17_S_SFS_FFD(struct S_FFD p0, float p1, struct S_FFD p2) { return p0; }
EXPORT struct S_FFP f17_S_SFS_FFP(struct S_FFP p0, float p1, struct S_FFP p2) { return p0; }
EXPORT struct S_FDI f17_S_SFS_FDI(struct S_FDI p0, float p1, struct S_FDI p2) { return p0; }
EXPORT struct S_FDF f17_S_SFS_FDF(struct S_FDF p0, float p1, struct S_FDF p2) { return p0; }
EXPORT struct S_FDD f17_S_SFS_FDD(struct S_FDD p0, float p1, struct S_FDD p2) { return p0; }
EXPORT struct S_FDP f18_S_SFS_FDP(struct S_FDP p0, float p1, struct S_FDP p2) { return p0; }
EXPORT struct S_FPI f18_S_SFS_FPI(struct S_FPI p0, float p1, struct S_FPI p2) { return p0; }
EXPORT struct S_FPF f18_S_SFS_FPF(struct S_FPF p0, float p1, struct S_FPF p2) { return p0; }
EXPORT struct S_FPD f18_S_SFS_FPD(struct S_FPD p0, float p1, struct S_FPD p2) { return p0; }
EXPORT struct S_FPP f18_S_SFS_FPP(struct S_FPP p0, float p1, struct S_FPP p2) { return p0; }
EXPORT struct S_DII f18_S_SFS_DII(struct S_DII p0, float p1, struct S_DII p2) { return p0; }
EXPORT struct S_DIF f18_S_SFS_DIF(struct S_DIF p0, float p1, struct S_DIF p2) { return p0; }
EXPORT struct S_DID f18_S_SFS_DID(struct S_DID p0, float p1, struct S_DID p2) { return p0; }
EXPORT struct S_DIP f18_S_SFS_DIP(struct S_DIP p0, float p1, struct S_DIP p2) { return p0; }
EXPORT struct S_DFI f18_S_SFS_DFI(struct S_DFI p0, float p1, struct S_DFI p2) { return p0; }
EXPORT struct S_DFF f18_S_SFS_DFF(struct S_DFF p0, float p1, struct S_DFF p2) { return p0; }
EXPORT struct S_DFD f18_S_SFS_DFD(struct S_DFD p0, float p1, struct S_DFD p2) { return p0; }
EXPORT struct S_DFP f18_S_SFS_DFP(struct S_DFP p0, float p1, struct S_DFP p2) { return p0; }
EXPORT struct S_DDI f18_S_SFS_DDI(struct S_DDI p0, float p1, struct S_DDI p2) { return p0; }
EXPORT struct S_DDF f18_S_SFS_DDF(struct S_DDF p0, float p1, struct S_DDF p2) { return p0; }
EXPORT struct S_DDD f18_S_SFS_DDD(struct S_DDD p0, float p1, struct S_DDD p2) { return p0; }
EXPORT struct S_DDP f18_S_SFS_DDP(struct S_DDP p0, float p1, struct S_DDP p2) { return p0; }
EXPORT struct S_DPI f18_S_SFS_DPI(struct S_DPI p0, float p1, struct S_DPI p2) { return p0; }
EXPORT struct S_DPF f18_S_SFS_DPF(struct S_DPF p0, float p1, struct S_DPF p2) { return p0; }
EXPORT struct S_DPD f18_S_SFS_DPD(struct S_DPD p0, float p1, struct S_DPD p2) { return p0; }
EXPORT struct S_DPP f18_S_SFS_DPP(struct S_DPP p0, float p1, struct S_DPP p2) { return p0; }
EXPORT struct S_PII f18_S_SFS_PII(struct S_PII p0, float p1, struct S_PII p2) { return p0; }
EXPORT struct S_PIF f18_S_SFS_PIF(struct S_PIF p0, float p1, struct S_PIF p2) { return p0; }
EXPORT struct S_PID f18_S_SFS_PID(struct S_PID p0, float p1, struct S_PID p2) { return p0; }
EXPORT struct S_PIP f18_S_SFS_PIP(struct S_PIP p0, float p1, struct S_PIP p2) { return p0; }
EXPORT struct S_PFI f18_S_SFS_PFI(struct S_PFI p0, float p1, struct S_PFI p2) { return p0; }
EXPORT struct S_PFF f18_S_SFS_PFF(struct S_PFF p0, float p1, struct S_PFF p2) { return p0; }
EXPORT struct S_PFD f18_S_SFS_PFD(struct S_PFD p0, float p1, struct S_PFD p2) { return p0; }
EXPORT struct S_PFP f18_S_SFS_PFP(struct S_PFP p0, float p1, struct S_PFP p2) { return p0; }
EXPORT struct S_PDI f18_S_SFS_PDI(struct S_PDI p0, float p1, struct S_PDI p2) { return p0; }
EXPORT struct S_PDF f18_S_SFS_PDF(struct S_PDF p0, float p1, struct S_PDF p2) { return p0; }
EXPORT struct S_PDD f18_S_SFS_PDD(struct S_PDD p0, float p1, struct S_PDD p2) { return p0; }
EXPORT struct S_PDP f18_S_SFS_PDP(struct S_PDP p0, float p1, struct S_PDP p2) { return p0; }
EXPORT struct S_PPI f18_S_SFS_PPI(struct S_PPI p0, float p1, struct S_PPI p2) { return p0; }
EXPORT struct S_PPF f18_S_SFS_PPF(struct S_PPF p0, float p1, struct S_PPF p2) { return p0; }
EXPORT struct S_PPD f18_S_SFS_PPD(struct S_PPD p0, float p1, struct S_PPD p2) { return p0; }
EXPORT struct S_PPP f18_S_SFS_PPP(struct S_PPP p0, float p1, struct S_PPP p2) { return p0; }
EXPORT struct S_I f18_S_SDI_I(struct S_I p0, double p1, int p2) { return p0; }
EXPORT struct S_F f18_S_SDI_F(struct S_F p0, double p1, int p2) { return p0; }
EXPORT struct S_D f18_S_SDI_D(struct S_D p0, double p1, int p2) { return p0; }
EXPORT struct S_P f18_S_SDI_P(struct S_P p0, double p1, int p2) { return p0; }
EXPORT struct S_II f18_S_SDI_II(struct S_II p0, double p1, int p2) { return p0; }
EXPORT struct S_IF f18_S_SDI_IF(struct S_IF p0, double p1, int p2) { return p0; }
EXPORT struct S_ID f18_S_SDI_ID(struct S_ID p0, double p1, int p2) { return p0; }
EXPORT struct S_IP f18_S_SDI_IP(struct S_IP p0, double p1, int p2) { return p0; }
EXPORT struct S_FI f18_S_SDI_FI(struct S_FI p0, double p1, int p2) { return p0; }
EXPORT struct S_FF f18_S_SDI_FF(struct S_FF p0, double p1, int p2) { return p0; }
EXPORT struct S_FD f18_S_SDI_FD(struct S_FD p0, double p1, int p2) { return p0; }
EXPORT struct S_FP f18_S_SDI_FP(struct S_FP p0, double p1, int p2) { return p0; }
EXPORT struct S_DI f18_S_SDI_DI(struct S_DI p0, double p1, int p2) { return p0; }
EXPORT struct S_DF f18_S_SDI_DF(struct S_DF p0, double p1, int p2) { return p0; }
EXPORT struct S_DD f18_S_SDI_DD(struct S_DD p0, double p1, int p2) { return p0; }
EXPORT struct S_DP f18_S_SDI_DP(struct S_DP p0, double p1, int p2) { return p0; }
EXPORT struct S_PI f18_S_SDI_PI(struct S_PI p0, double p1, int p2) { return p0; }
EXPORT struct S_PF f18_S_SDI_PF(struct S_PF p0, double p1, int p2) { return p0; }
EXPORT struct S_PD f18_S_SDI_PD(struct S_PD p0, double p1, int p2) { return p0; }
EXPORT struct S_PP f18_S_SDI_PP(struct S_PP p0, double p1, int p2) { return p0; }
EXPORT struct S_III f18_S_SDI_III(struct S_III p0, double p1, int p2) { return p0; }
EXPORT struct S_IIF f18_S_SDI_IIF(struct S_IIF p0, double p1, int p2) { return p0; }
EXPORT struct S_IID f18_S_SDI_IID(struct S_IID p0, double p1, int p2) { return p0; }
EXPORT struct S_IIP f18_S_SDI_IIP(struct S_IIP p0, double p1, int p2) { return p0; }
EXPORT struct S_IFI f18_S_SDI_IFI(struct S_IFI p0, double p1, int p2) { return p0; }
EXPORT struct S_IFF f18_S_SDI_IFF(struct S_IFF p0, double p1, int p2) { return p0; }
EXPORT struct S_IFD f18_S_SDI_IFD(struct S_IFD p0, double p1, int p2) { return p0; }
EXPORT struct S_IFP f18_S_SDI_IFP(struct S_IFP p0, double p1, int p2) { return p0; }
EXPORT struct S_IDI f18_S_SDI_IDI(struct S_IDI p0, double p1, int p2) { return p0; }
EXPORT struct S_IDF f18_S_SDI_IDF(struct S_IDF p0, double p1, int p2) { return p0; }
EXPORT struct S_IDD f18_S_SDI_IDD(struct S_IDD p0, double p1, int p2) { return p0; }
EXPORT struct S_IDP f18_S_SDI_IDP(struct S_IDP p0, double p1, int p2) { return p0; }
EXPORT struct S_IPI f18_S_SDI_IPI(struct S_IPI p0, double p1, int p2) { return p0; }
EXPORT struct S_IPF f18_S_SDI_IPF(struct S_IPF p0, double p1, int p2) { return p0; }
EXPORT struct S_IPD f18_S_SDI_IPD(struct S_IPD p0, double p1, int p2) { return p0; }
EXPORT struct S_IPP f18_S_SDI_IPP(struct S_IPP p0, double p1, int p2) { return p0; }
EXPORT struct S_FII f18_S_SDI_FII(struct S_FII p0, double p1, int p2) { return p0; }
EXPORT struct S_FIF f18_S_SDI_FIF(struct S_FIF p0, double p1, int p2) { return p0; }
EXPORT struct S_FID f18_S_SDI_FID(struct S_FID p0, double p1, int p2) { return p0; }
EXPORT struct S_FIP f18_S_SDI_FIP(struct S_FIP p0, double p1, int p2) { return p0; }
EXPORT struct S_FFI f18_S_SDI_FFI(struct S_FFI p0, double p1, int p2) { return p0; }
EXPORT struct S_FFF f18_S_SDI_FFF(struct S_FFF p0, double p1, int p2) { return p0; }
EXPORT struct S_FFD f18_S_SDI_FFD(struct S_FFD p0, double p1, int p2) { return p0; }
EXPORT struct S_FFP f18_S_SDI_FFP(struct S_FFP p0, double p1, int p2) { return p0; }
EXPORT struct S_FDI f18_S_SDI_FDI(struct S_FDI p0, double p1, int p2) { return p0; }
EXPORT struct S_FDF f18_S_SDI_FDF(struct S_FDF p0, double p1, int p2) { return p0; }
EXPORT struct S_FDD f18_S_SDI_FDD(struct S_FDD p0, double p1, int p2) { return p0; }
EXPORT struct S_FDP f18_S_SDI_FDP(struct S_FDP p0, double p1, int p2) { return p0; }
EXPORT struct S_FPI f18_S_SDI_FPI(struct S_FPI p0, double p1, int p2) { return p0; }
EXPORT struct S_FPF f18_S_SDI_FPF(struct S_FPF p0, double p1, int p2) { return p0; }
EXPORT struct S_FPD f18_S_SDI_FPD(struct S_FPD p0, double p1, int p2) { return p0; }
EXPORT struct S_FPP f18_S_SDI_FPP(struct S_FPP p0, double p1, int p2) { return p0; }
EXPORT struct S_DII f18_S_SDI_DII(struct S_DII p0, double p1, int p2) { return p0; }
EXPORT struct S_DIF f18_S_SDI_DIF(struct S_DIF p0, double p1, int p2) { return p0; }
EXPORT struct S_DID f18_S_SDI_DID(struct S_DID p0, double p1, int p2) { return p0; }
EXPORT struct S_DIP f18_S_SDI_DIP(struct S_DIP p0, double p1, int p2) { return p0; }
EXPORT struct S_DFI f18_S_SDI_DFI(struct S_DFI p0, double p1, int p2) { return p0; }
EXPORT struct S_DFF f18_S_SDI_DFF(struct S_DFF p0, double p1, int p2) { return p0; }
EXPORT struct S_DFD f18_S_SDI_DFD(struct S_DFD p0, double p1, int p2) { return p0; }
EXPORT struct S_DFP f18_S_SDI_DFP(struct S_DFP p0, double p1, int p2) { return p0; }
EXPORT struct S_DDI f18_S_SDI_DDI(struct S_DDI p0, double p1, int p2) { return p0; }
EXPORT struct S_DDF f18_S_SDI_DDF(struct S_DDF p0, double p1, int p2) { return p0; }
EXPORT struct S_DDD f18_S_SDI_DDD(struct S_DDD p0, double p1, int p2) { return p0; }
EXPORT struct S_DDP f18_S_SDI_DDP(struct S_DDP p0, double p1, int p2) { return p0; }
EXPORT struct S_DPI f18_S_SDI_DPI(struct S_DPI p0, double p1, int p2) { return p0; }
EXPORT struct S_DPF f18_S_SDI_DPF(struct S_DPF p0, double p1, int p2) { return p0; }
EXPORT struct S_DPD f18_S_SDI_DPD(struct S_DPD p0, double p1, int p2) { return p0; }
EXPORT struct S_DPP f18_S_SDI_DPP(struct S_DPP p0, double p1, int p2) { return p0; }
EXPORT struct S_PII f18_S_SDI_PII(struct S_PII p0, double p1, int p2) { return p0; }
EXPORT struct S_PIF f18_S_SDI_PIF(struct S_PIF p0, double p1, int p2) { return p0; }
EXPORT struct S_PID f18_S_SDI_PID(struct S_PID p0, double p1, int p2) { return p0; }
EXPORT struct S_PIP f18_S_SDI_PIP(struct S_PIP p0, double p1, int p2) { return p0; }
EXPORT struct S_PFI f18_S_SDI_PFI(struct S_PFI p0, double p1, int p2) { return p0; }
EXPORT struct S_PFF f18_S_SDI_PFF(struct S_PFF p0, double p1, int p2) { return p0; }
EXPORT struct S_PFD f18_S_SDI_PFD(struct S_PFD p0, double p1, int p2) { return p0; }
EXPORT struct S_PFP f18_S_SDI_PFP(struct S_PFP p0, double p1, int p2) { return p0; }
EXPORT struct S_PDI f18_S_SDI_PDI(struct S_PDI p0, double p1, int p2) { return p0; }
EXPORT struct S_PDF f18_S_SDI_PDF(struct S_PDF p0, double p1, int p2) { return p0; }
EXPORT struct S_PDD f18_S_SDI_PDD(struct S_PDD p0, double p1, int p2) { return p0; }
EXPORT struct S_PDP f18_S_SDI_PDP(struct S_PDP p0, double p1, int p2) { return p0; }
EXPORT struct S_PPI f18_S_SDI_PPI(struct S_PPI p0, double p1, int p2) { return p0; }
EXPORT struct S_PPF f18_S_SDI_PPF(struct S_PPF p0, double p1, int p2) { return p0; }
EXPORT struct S_PPD f18_S_SDI_PPD(struct S_PPD p0, double p1, int p2) { return p0; }
EXPORT struct S_PPP f18_S_SDI_PPP(struct S_PPP p0, double p1, int p2) { return p0; }
EXPORT struct S_I f18_S_SDF_I(struct S_I p0, double p1, float p2) { return p0; }
EXPORT struct S_F f18_S_SDF_F(struct S_F p0, double p1, float p2) { return p0; }
EXPORT struct S_D f18_S_SDF_D(struct S_D p0, double p1, float p2) { return p0; }
EXPORT struct S_P f18_S_SDF_P(struct S_P p0, double p1, float p2) { return p0; }
EXPORT struct S_II f18_S_SDF_II(struct S_II p0, double p1, float p2) { return p0; }
EXPORT struct S_IF f18_S_SDF_IF(struct S_IF p0, double p1, float p2) { return p0; }
EXPORT struct S_ID f18_S_SDF_ID(struct S_ID p0, double p1, float p2) { return p0; }
EXPORT struct S_IP f18_S_SDF_IP(struct S_IP p0, double p1, float p2) { return p0; }
EXPORT struct S_FI f18_S_SDF_FI(struct S_FI p0, double p1, float p2) { return p0; }
EXPORT struct S_FF f18_S_SDF_FF(struct S_FF p0, double p1, float p2) { return p0; }
EXPORT struct S_FD f18_S_SDF_FD(struct S_FD p0, double p1, float p2) { return p0; }
EXPORT struct S_FP f18_S_SDF_FP(struct S_FP p0, double p1, float p2) { return p0; }
EXPORT struct S_DI f18_S_SDF_DI(struct S_DI p0, double p1, float p2) { return p0; }
EXPORT struct S_DF f18_S_SDF_DF(struct S_DF p0, double p1, float p2) { return p0; }
EXPORT struct S_DD f18_S_SDF_DD(struct S_DD p0, double p1, float p2) { return p0; }
EXPORT struct S_DP f18_S_SDF_DP(struct S_DP p0, double p1, float p2) { return p0; }
EXPORT struct S_PI f18_S_SDF_PI(struct S_PI p0, double p1, float p2) { return p0; }
EXPORT struct S_PF f18_S_SDF_PF(struct S_PF p0, double p1, float p2) { return p0; }
EXPORT struct S_PD f18_S_SDF_PD(struct S_PD p0, double p1, float p2) { return p0; }
EXPORT struct S_PP f18_S_SDF_PP(struct S_PP p0, double p1, float p2) { return p0; }
EXPORT struct S_III f18_S_SDF_III(struct S_III p0, double p1, float p2) { return p0; }
EXPORT struct S_IIF f18_S_SDF_IIF(struct S_IIF p0, double p1, float p2) { return p0; }
EXPORT struct S_IID f18_S_SDF_IID(struct S_IID p0, double p1, float p2) { return p0; }
EXPORT struct S_IIP f18_S_SDF_IIP(struct S_IIP p0, double p1, float p2) { return p0; }
EXPORT struct S_IFI f18_S_SDF_IFI(struct S_IFI p0, double p1, float p2) { return p0; }
EXPORT struct S_IFF f18_S_SDF_IFF(struct S_IFF p0, double p1, float p2) { return p0; }
EXPORT struct S_IFD f18_S_SDF_IFD(struct S_IFD p0, double p1, float p2) { return p0; }
EXPORT struct S_IFP f18_S_SDF_IFP(struct S_IFP p0, double p1, float p2) { return p0; }
EXPORT struct S_IDI f18_S_SDF_IDI(struct S_IDI p0, double p1, float p2) { return p0; }
EXPORT struct S_IDF f18_S_SDF_IDF(struct S_IDF p0, double p1, float p2) { return p0; }
EXPORT struct S_IDD f18_S_SDF_IDD(struct S_IDD p0, double p1, float p2) { return p0; }
EXPORT struct S_IDP f18_S_SDF_IDP(struct S_IDP p0, double p1, float p2) { return p0; }
EXPORT struct S_IPI f18_S_SDF_IPI(struct S_IPI p0, double p1, float p2) { return p0; }
EXPORT struct S_IPF f18_S_SDF_IPF(struct S_IPF p0, double p1, float p2) { return p0; }
EXPORT struct S_IPD f18_S_SDF_IPD(struct S_IPD p0, double p1, float p2) { return p0; }
EXPORT struct S_IPP f18_S_SDF_IPP(struct S_IPP p0, double p1, float p2) { return p0; }
EXPORT struct S_FII f18_S_SDF_FII(struct S_FII p0, double p1, float p2) { return p0; }
EXPORT struct S_FIF f18_S_SDF_FIF(struct S_FIF p0, double p1, float p2) { return p0; }
EXPORT struct S_FID f18_S_SDF_FID(struct S_FID p0, double p1, float p2) { return p0; }
EXPORT struct S_FIP f18_S_SDF_FIP(struct S_FIP p0, double p1, float p2) { return p0; }
EXPORT struct S_FFI f18_S_SDF_FFI(struct S_FFI p0, double p1, float p2) { return p0; }
EXPORT struct S_FFF f18_S_SDF_FFF(struct S_FFF p0, double p1, float p2) { return p0; }
EXPORT struct S_FFD f18_S_SDF_FFD(struct S_FFD p0, double p1, float p2) { return p0; }
EXPORT struct S_FFP f18_S_SDF_FFP(struct S_FFP p0, double p1, float p2) { return p0; }
EXPORT struct S_FDI f18_S_SDF_FDI(struct S_FDI p0, double p1, float p2) { return p0; }
EXPORT struct S_FDF f18_S_SDF_FDF(struct S_FDF p0, double p1, float p2) { return p0; }
EXPORT struct S_FDD f18_S_SDF_FDD(struct S_FDD p0, double p1, float p2) { return p0; }
EXPORT struct S_FDP f18_S_SDF_FDP(struct S_FDP p0, double p1, float p2) { return p0; }
EXPORT struct S_FPI f18_S_SDF_FPI(struct S_FPI p0, double p1, float p2) { return p0; }
EXPORT struct S_FPF f18_S_SDF_FPF(struct S_FPF p0, double p1, float p2) { return p0; }
EXPORT struct S_FPD f18_S_SDF_FPD(struct S_FPD p0, double p1, float p2) { return p0; }
EXPORT struct S_FPP f18_S_SDF_FPP(struct S_FPP p0, double p1, float p2) { return p0; }
EXPORT struct S_DII f18_S_SDF_DII(struct S_DII p0, double p1, float p2) { return p0; }
EXPORT struct S_DIF f18_S_SDF_DIF(struct S_DIF p0, double p1, float p2) { return p0; }
EXPORT struct S_DID f18_S_SDF_DID(struct S_DID p0, double p1, float p2) { return p0; }
EXPORT struct S_DIP f18_S_SDF_DIP(struct S_DIP p0, double p1, float p2) { return p0; }
EXPORT struct S_DFI f18_S_SDF_DFI(struct S_DFI p0, double p1, float p2) { return p0; }
EXPORT struct S_DFF f18_S_SDF_DFF(struct S_DFF p0, double p1, float p2) { return p0; }
EXPORT struct S_DFD f18_S_SDF_DFD(struct S_DFD p0, double p1, float p2) { return p0; }
EXPORT struct S_DFP f18_S_SDF_DFP(struct S_DFP p0, double p1, float p2) { return p0; }
EXPORT struct S_DDI f18_S_SDF_DDI(struct S_DDI p0, double p1, float p2) { return p0; }
EXPORT struct S_DDF f18_S_SDF_DDF(struct S_DDF p0, double p1, float p2) { return p0; }
EXPORT struct S_DDD f18_S_SDF_DDD(struct S_DDD p0, double p1, float p2) { return p0; }
EXPORT struct S_DDP f18_S_SDF_DDP(struct S_DDP p0, double p1, float p2) { return p0; }
EXPORT struct S_DPI f18_S_SDF_DPI(struct S_DPI p0, double p1, float p2) { return p0; }
EXPORT struct S_DPF f18_S_SDF_DPF(struct S_DPF p0, double p1, float p2) { return p0; }
EXPORT struct S_DPD f18_S_SDF_DPD(struct S_DPD p0, double p1, float p2) { return p0; }
EXPORT struct S_DPP f18_S_SDF_DPP(struct S_DPP p0, double p1, float p2) { return p0; }
EXPORT struct S_PII f18_S_SDF_PII(struct S_PII p0, double p1, float p2) { return p0; }
EXPORT struct S_PIF f18_S_SDF_PIF(struct S_PIF p0, double p1, float p2) { return p0; }
EXPORT struct S_PID f18_S_SDF_PID(struct S_PID p0, double p1, float p2) { return p0; }
EXPORT struct S_PIP f18_S_SDF_PIP(struct S_PIP p0, double p1, float p2) { return p0; }
EXPORT struct S_PFI f18_S_SDF_PFI(struct S_PFI p0, double p1, float p2) { return p0; }
EXPORT struct S_PFF f18_S_SDF_PFF(struct S_PFF p0, double p1, float p2) { return p0; }
EXPORT struct S_PFD f18_S_SDF_PFD(struct S_PFD p0, double p1, float p2) { return p0; }
EXPORT struct S_PFP f18_S_SDF_PFP(struct S_PFP p0, double p1, float p2) { return p0; }
EXPORT struct S_PDI f18_S_SDF_PDI(struct S_PDI p0, double p1, float p2) { return p0; }
EXPORT struct S_PDF f18_S_SDF_PDF(struct S_PDF p0, double p1, float p2) { return p0; }
EXPORT struct S_PDD f18_S_SDF_PDD(struct S_PDD p0, double p1, float p2) { return p0; }
EXPORT struct S_PDP f18_S_SDF_PDP(struct S_PDP p0, double p1, float p2) { return p0; }
EXPORT struct S_PPI f18_S_SDF_PPI(struct S_PPI p0, double p1, float p2) { return p0; }
EXPORT struct S_PPF f18_S_SDF_PPF(struct S_PPF p0, double p1, float p2) { return p0; }
EXPORT struct S_PPD f18_S_SDF_PPD(struct S_PPD p0, double p1, float p2) { return p0; }
EXPORT struct S_PPP f18_S_SDF_PPP(struct S_PPP p0, double p1, float p2) { return p0; }
EXPORT struct S_I f18_S_SDD_I(struct S_I p0, double p1, double p2) { return p0; }
EXPORT struct S_F f18_S_SDD_F(struct S_F p0, double p1, double p2) { return p0; }
EXPORT struct S_D f18_S_SDD_D(struct S_D p0, double p1, double p2) { return p0; }
EXPORT struct S_P f18_S_SDD_P(struct S_P p0, double p1, double p2) { return p0; }
EXPORT struct S_II f18_S_SDD_II(struct S_II p0, double p1, double p2) { return p0; }
EXPORT struct S_IF f18_S_SDD_IF(struct S_IF p0, double p1, double p2) { return p0; }
EXPORT struct S_ID f18_S_SDD_ID(struct S_ID p0, double p1, double p2) { return p0; }
EXPORT struct S_IP f18_S_SDD_IP(struct S_IP p0, double p1, double p2) { return p0; }
EXPORT struct S_FI f18_S_SDD_FI(struct S_FI p0, double p1, double p2) { return p0; }
EXPORT struct S_FF f18_S_SDD_FF(struct S_FF p0, double p1, double p2) { return p0; }
EXPORT struct S_FD f18_S_SDD_FD(struct S_FD p0, double p1, double p2) { return p0; }
EXPORT struct S_FP f18_S_SDD_FP(struct S_FP p0, double p1, double p2) { return p0; }
EXPORT struct S_DI f18_S_SDD_DI(struct S_DI p0, double p1, double p2) { return p0; }
EXPORT struct S_DF f18_S_SDD_DF(struct S_DF p0, double p1, double p2) { return p0; }
EXPORT struct S_DD f18_S_SDD_DD(struct S_DD p0, double p1, double p2) { return p0; }
EXPORT struct S_DP f18_S_SDD_DP(struct S_DP p0, double p1, double p2) { return p0; }
EXPORT struct S_PI f18_S_SDD_PI(struct S_PI p0, double p1, double p2) { return p0; }
EXPORT struct S_PF f18_S_SDD_PF(struct S_PF p0, double p1, double p2) { return p0; }
EXPORT struct S_PD f18_S_SDD_PD(struct S_PD p0, double p1, double p2) { return p0; }
EXPORT struct S_PP f18_S_SDD_PP(struct S_PP p0, double p1, double p2) { return p0; }
EXPORT struct S_III f18_S_SDD_III(struct S_III p0, double p1, double p2) { return p0; }
EXPORT struct S_IIF f18_S_SDD_IIF(struct S_IIF p0, double p1, double p2) { return p0; }
EXPORT struct S_IID f18_S_SDD_IID(struct S_IID p0, double p1, double p2) { return p0; }
EXPORT struct S_IIP f18_S_SDD_IIP(struct S_IIP p0, double p1, double p2) { return p0; }
EXPORT struct S_IFI f18_S_SDD_IFI(struct S_IFI p0, double p1, double p2) { return p0; }
EXPORT struct S_IFF f18_S_SDD_IFF(struct S_IFF p0, double p1, double p2) { return p0; }
EXPORT struct S_IFD f18_S_SDD_IFD(struct S_IFD p0, double p1, double p2) { return p0; }
EXPORT struct S_IFP f18_S_SDD_IFP(struct S_IFP p0, double p1, double p2) { return p0; }
EXPORT struct S_IDI f18_S_SDD_IDI(struct S_IDI p0, double p1, double p2) { return p0; }
EXPORT struct S_IDF f18_S_SDD_IDF(struct S_IDF p0, double p1, double p2) { return p0; }
EXPORT struct S_IDD f18_S_SDD_IDD(struct S_IDD p0, double p1, double p2) { return p0; }
EXPORT struct S_IDP f18_S_SDD_IDP(struct S_IDP p0, double p1, double p2) { return p0; }
EXPORT struct S_IPI f18_S_SDD_IPI(struct S_IPI p0, double p1, double p2) { return p0; }
EXPORT struct S_IPF f18_S_SDD_IPF(struct S_IPF p0, double p1, double p2) { return p0; }
EXPORT struct S_IPD f18_S_SDD_IPD(struct S_IPD p0, double p1, double p2) { return p0; }
EXPORT struct S_IPP f18_S_SDD_IPP(struct S_IPP p0, double p1, double p2) { return p0; }
EXPORT struct S_FII f18_S_SDD_FII(struct S_FII p0, double p1, double p2) { return p0; }
EXPORT struct S_FIF f18_S_SDD_FIF(struct S_FIF p0, double p1, double p2) { return p0; }
EXPORT struct S_FID f18_S_SDD_FID(struct S_FID p0, double p1, double p2) { return p0; }
EXPORT struct S_FIP f18_S_SDD_FIP(struct S_FIP p0, double p1, double p2) { return p0; }
EXPORT struct S_FFI f18_S_SDD_FFI(struct S_FFI p0, double p1, double p2) { return p0; }
EXPORT struct S_FFF f18_S_SDD_FFF(struct S_FFF p0, double p1, double p2) { return p0; }
EXPORT struct S_FFD f18_S_SDD_FFD(struct S_FFD p0, double p1, double p2) { return p0; }
EXPORT struct S_FFP f18_S_SDD_FFP(struct S_FFP p0, double p1, double p2) { return p0; }
EXPORT struct S_FDI f18_S_SDD_FDI(struct S_FDI p0, double p1, double p2) { return p0; }
EXPORT struct S_FDF f18_S_SDD_FDF(struct S_FDF p0, double p1, double p2) { return p0; }
EXPORT struct S_FDD f18_S_SDD_FDD(struct S_FDD p0, double p1, double p2) { return p0; }
EXPORT struct S_FDP f18_S_SDD_FDP(struct S_FDP p0, double p1, double p2) { return p0; }
EXPORT struct S_FPI f18_S_SDD_FPI(struct S_FPI p0, double p1, double p2) { return p0; }
EXPORT struct S_FPF f18_S_SDD_FPF(struct S_FPF p0, double p1, double p2) { return p0; }
EXPORT struct S_FPD f18_S_SDD_FPD(struct S_FPD p0, double p1, double p2) { return p0; }
EXPORT struct S_FPP f18_S_SDD_FPP(struct S_FPP p0, double p1, double p2) { return p0; }
EXPORT struct S_DII f18_S_SDD_DII(struct S_DII p0, double p1, double p2) { return p0; }
EXPORT struct S_DIF f18_S_SDD_DIF(struct S_DIF p0, double p1, double p2) { return p0; }
EXPORT struct S_DID f18_S_SDD_DID(struct S_DID p0, double p1, double p2) { return p0; }
EXPORT struct S_DIP f18_S_SDD_DIP(struct S_DIP p0, double p1, double p2) { return p0; }
EXPORT struct S_DFI f18_S_SDD_DFI(struct S_DFI p0, double p1, double p2) { return p0; }
EXPORT struct S_DFF f18_S_SDD_DFF(struct S_DFF p0, double p1, double p2) { return p0; }
EXPORT struct S_DFD f18_S_SDD_DFD(struct S_DFD p0, double p1, double p2) { return p0; }
EXPORT struct S_DFP f18_S_SDD_DFP(struct S_DFP p0, double p1, double p2) { return p0; }
EXPORT struct S_DDI f18_S_SDD_DDI(struct S_DDI p0, double p1, double p2) { return p0; }
EXPORT struct S_DDF f18_S_SDD_DDF(struct S_DDF p0, double p1, double p2) { return p0; }
EXPORT struct S_DDD f18_S_SDD_DDD(struct S_DDD p0, double p1, double p2) { return p0; }
EXPORT struct S_DDP f18_S_SDD_DDP(struct S_DDP p0, double p1, double p2) { return p0; }
EXPORT struct S_DPI f18_S_SDD_DPI(struct S_DPI p0, double p1, double p2) { return p0; }
EXPORT struct S_DPF f18_S_SDD_DPF(struct S_DPF p0, double p1, double p2) { return p0; }
EXPORT struct S_DPD f18_S_SDD_DPD(struct S_DPD p0, double p1, double p2) { return p0; }
EXPORT struct S_DPP f18_S_SDD_DPP(struct S_DPP p0, double p1, double p2) { return p0; }
EXPORT struct S_PII f18_S_SDD_PII(struct S_PII p0, double p1, double p2) { return p0; }
EXPORT struct S_PIF f18_S_SDD_PIF(struct S_PIF p0, double p1, double p2) { return p0; }
EXPORT struct S_PID f18_S_SDD_PID(struct S_PID p0, double p1, double p2) { return p0; }
EXPORT struct S_PIP f18_S_SDD_PIP(struct S_PIP p0, double p1, double p2) { return p0; }
EXPORT struct S_PFI f18_S_SDD_PFI(struct S_PFI p0, double p1, double p2) { return p0; }
EXPORT struct S_PFF f18_S_SDD_PFF(struct S_PFF p0, double p1, double p2) { return p0; }
EXPORT struct S_PFD f18_S_SDD_PFD(struct S_PFD p0, double p1, double p2) { return p0; }
EXPORT struct S_PFP f18_S_SDD_PFP(struct S_PFP p0, double p1, double p2) { return p0; }
EXPORT struct S_PDI f18_S_SDD_PDI(struct S_PDI p0, double p1, double p2) { return p0; }
EXPORT struct S_PDF f18_S_SDD_PDF(struct S_PDF p0, double p1, double p2) { return p0; }
EXPORT struct S_PDD f18_S_SDD_PDD(struct S_PDD p0, double p1, double p2) { return p0; }
EXPORT struct S_PDP f18_S_SDD_PDP(struct S_PDP p0, double p1, double p2) { return p0; }
EXPORT struct S_PPI f18_S_SDD_PPI(struct S_PPI p0, double p1, double p2) { return p0; }
EXPORT struct S_PPF f18_S_SDD_PPF(struct S_PPF p0, double p1, double p2) { return p0; }
EXPORT struct S_PPD f18_S_SDD_PPD(struct S_PPD p0, double p1, double p2) { return p0; }
EXPORT struct S_PPP f18_S_SDD_PPP(struct S_PPP p0, double p1, double p2) { return p0; }
EXPORT struct S_I f18_S_SDP_I(struct S_I p0, double p1, void* p2) { return p0; }
EXPORT struct S_F f18_S_SDP_F(struct S_F p0, double p1, void* p2) { return p0; }
EXPORT struct S_D f18_S_SDP_D(struct S_D p0, double p1, void* p2) { return p0; }
EXPORT struct S_P f18_S_SDP_P(struct S_P p0, double p1, void* p2) { return p0; }
EXPORT struct S_II f18_S_SDP_II(struct S_II p0, double p1, void* p2) { return p0; }
EXPORT struct S_IF f18_S_SDP_IF(struct S_IF p0, double p1, void* p2) { return p0; }
EXPORT struct S_ID f18_S_SDP_ID(struct S_ID p0, double p1, void* p2) { return p0; }
EXPORT struct S_IP f18_S_SDP_IP(struct S_IP p0, double p1, void* p2) { return p0; }
EXPORT struct S_FI f18_S_SDP_FI(struct S_FI p0, double p1, void* p2) { return p0; }
EXPORT struct S_FF f18_S_SDP_FF(struct S_FF p0, double p1, void* p2) { return p0; }
EXPORT struct S_FD f18_S_SDP_FD(struct S_FD p0, double p1, void* p2) { return p0; }
EXPORT struct S_FP f18_S_SDP_FP(struct S_FP p0, double p1, void* p2) { return p0; }
EXPORT struct S_DI f18_S_SDP_DI(struct S_DI p0, double p1, void* p2) { return p0; }
EXPORT struct S_DF f18_S_SDP_DF(struct S_DF p0, double p1, void* p2) { return p0; }
EXPORT struct S_DD f18_S_SDP_DD(struct S_DD p0, double p1, void* p2) { return p0; }
EXPORT struct S_DP f18_S_SDP_DP(struct S_DP p0, double p1, void* p2) { return p0; }
EXPORT struct S_PI f18_S_SDP_PI(struct S_PI p0, double p1, void* p2) { return p0; }
EXPORT struct S_PF f18_S_SDP_PF(struct S_PF p0, double p1, void* p2) { return p0; }
EXPORT struct S_PD f18_S_SDP_PD(struct S_PD p0, double p1, void* p2) { return p0; }
EXPORT struct S_PP f18_S_SDP_PP(struct S_PP p0, double p1, void* p2) { return p0; }
EXPORT struct S_III f18_S_SDP_III(struct S_III p0, double p1, void* p2) { return p0; }
EXPORT struct S_IIF f18_S_SDP_IIF(struct S_IIF p0, double p1, void* p2) { return p0; }
EXPORT struct S_IID f18_S_SDP_IID(struct S_IID p0, double p1, void* p2) { return p0; }
EXPORT struct S_IIP f18_S_SDP_IIP(struct S_IIP p0, double p1, void* p2) { return p0; }
EXPORT struct S_IFI f18_S_SDP_IFI(struct S_IFI p0, double p1, void* p2) { return p0; }
EXPORT struct S_IFF f18_S_SDP_IFF(struct S_IFF p0, double p1, void* p2) { return p0; }
EXPORT struct S_IFD f18_S_SDP_IFD(struct S_IFD p0, double p1, void* p2) { return p0; }
EXPORT struct S_IFP f18_S_SDP_IFP(struct S_IFP p0, double p1, void* p2) { return p0; }
EXPORT struct S_IDI f18_S_SDP_IDI(struct S_IDI p0, double p1, void* p2) { return p0; }
EXPORT struct S_IDF f18_S_SDP_IDF(struct S_IDF p0, double p1, void* p2) { return p0; }
EXPORT struct S_IDD f18_S_SDP_IDD(struct S_IDD p0, double p1, void* p2) { return p0; }
EXPORT struct S_IDP f18_S_SDP_IDP(struct S_IDP p0, double p1, void* p2) { return p0; }
EXPORT struct S_IPI f18_S_SDP_IPI(struct S_IPI p0, double p1, void* p2) { return p0; }
EXPORT struct S_IPF f18_S_SDP_IPF(struct S_IPF p0, double p1, void* p2) { return p0; }
EXPORT struct S_IPD f18_S_SDP_IPD(struct S_IPD p0, double p1, void* p2) { return p0; }
EXPORT struct S_IPP f18_S_SDP_IPP(struct S_IPP p0, double p1, void* p2) { return p0; }
EXPORT struct S_FII f18_S_SDP_FII(struct S_FII p0, double p1, void* p2) { return p0; }
EXPORT struct S_FIF f18_S_SDP_FIF(struct S_FIF p0, double p1, void* p2) { return p0; }
EXPORT struct S_FID f18_S_SDP_FID(struct S_FID p0, double p1, void* p2) { return p0; }
EXPORT struct S_FIP f18_S_SDP_FIP(struct S_FIP p0, double p1, void* p2) { return p0; }
EXPORT struct S_FFI f18_S_SDP_FFI(struct S_FFI p0, double p1, void* p2) { return p0; }
EXPORT struct S_FFF f18_S_SDP_FFF(struct S_FFF p0, double p1, void* p2) { return p0; }
EXPORT struct S_FFD f18_S_SDP_FFD(struct S_FFD p0, double p1, void* p2) { return p0; }
EXPORT struct S_FFP f18_S_SDP_FFP(struct S_FFP p0, double p1, void* p2) { return p0; }
EXPORT struct S_FDI f18_S_SDP_FDI(struct S_FDI p0, double p1, void* p2) { return p0; }
EXPORT struct S_FDF f18_S_SDP_FDF(struct S_FDF p0, double p1, void* p2) { return p0; }
EXPORT struct S_FDD f18_S_SDP_FDD(struct S_FDD p0, double p1, void* p2) { return p0; }
EXPORT struct S_FDP f18_S_SDP_FDP(struct S_FDP p0, double p1, void* p2) { return p0; }
EXPORT struct S_FPI f18_S_SDP_FPI(struct S_FPI p0, double p1, void* p2) { return p0; }
EXPORT struct S_FPF f18_S_SDP_FPF(struct S_FPF p0, double p1, void* p2) { return p0; }
EXPORT struct S_FPD f18_S_SDP_FPD(struct S_FPD p0, double p1, void* p2) { return p0; }
EXPORT struct S_FPP f18_S_SDP_FPP(struct S_FPP p0, double p1, void* p2) { return p0; }
EXPORT struct S_DII f18_S_SDP_DII(struct S_DII p0, double p1, void* p2) { return p0; }
EXPORT struct S_DIF f18_S_SDP_DIF(struct S_DIF p0, double p1, void* p2) { return p0; }
EXPORT struct S_DID f18_S_SDP_DID(struct S_DID p0, double p1, void* p2) { return p0; }
EXPORT struct S_DIP f18_S_SDP_DIP(struct S_DIP p0, double p1, void* p2) { return p0; }
EXPORT struct S_DFI f18_S_SDP_DFI(struct S_DFI p0, double p1, void* p2) { return p0; }
EXPORT struct S_DFF f18_S_SDP_DFF(struct S_DFF p0, double p1, void* p2) { return p0; }
EXPORT struct S_DFD f18_S_SDP_DFD(struct S_DFD p0, double p1, void* p2) { return p0; }
EXPORT struct S_DFP f18_S_SDP_DFP(struct S_DFP p0, double p1, void* p2) { return p0; }
EXPORT struct S_DDI f18_S_SDP_DDI(struct S_DDI p0, double p1, void* p2) { return p0; }
EXPORT struct S_DDF f18_S_SDP_DDF(struct S_DDF p0, double p1, void* p2) { return p0; }
EXPORT struct S_DDD f18_S_SDP_DDD(struct S_DDD p0, double p1, void* p2) { return p0; }
EXPORT struct S_DDP f18_S_SDP_DDP(struct S_DDP p0, double p1, void* p2) { return p0; }
EXPORT struct S_DPI f18_S_SDP_DPI(struct S_DPI p0, double p1, void* p2) { return p0; }
EXPORT struct S_DPF f18_S_SDP_DPF(struct S_DPF p0, double p1, void* p2) { return p0; }
EXPORT struct S_DPD f18_S_SDP_DPD(struct S_DPD p0, double p1, void* p2) { return p0; }
EXPORT struct S_DPP f18_S_SDP_DPP(struct S_DPP p0, double p1, void* p2) { return p0; }
EXPORT struct S_PII f18_S_SDP_PII(struct S_PII p0, double p1, void* p2) { return p0; }
EXPORT struct S_PIF f18_S_SDP_PIF(struct S_PIF p0, double p1, void* p2) { return p0; }
EXPORT struct S_PID f18_S_SDP_PID(struct S_PID p0, double p1, void* p2) { return p0; }
EXPORT struct S_PIP f18_S_SDP_PIP(struct S_PIP p0, double p1, void* p2) { return p0; }
EXPORT struct S_PFI f18_S_SDP_PFI(struct S_PFI p0, double p1, void* p2) { return p0; }
EXPORT struct S_PFF f18_S_SDP_PFF(struct S_PFF p0, double p1, void* p2) { return p0; }
EXPORT struct S_PFD f18_S_SDP_PFD(struct S_PFD p0, double p1, void* p2) { return p0; }
EXPORT struct S_PFP f18_S_SDP_PFP(struct S_PFP p0, double p1, void* p2) { return p0; }
EXPORT struct S_PDI f18_S_SDP_PDI(struct S_PDI p0, double p1, void* p2) { return p0; }
EXPORT struct S_PDF f18_S_SDP_PDF(struct S_PDF p0, double p1, void* p2) { return p0; }
EXPORT struct S_PDD f18_S_SDP_PDD(struct S_PDD p0, double p1, void* p2) { return p0; }
EXPORT struct S_PDP f18_S_SDP_PDP(struct S_PDP p0, double p1, void* p2) { return p0; }
EXPORT struct S_PPI f18_S_SDP_PPI(struct S_PPI p0, double p1, void* p2) { return p0; }
EXPORT struct S_PPF f18_S_SDP_PPF(struct S_PPF p0, double p1, void* p2) { return p0; }
EXPORT struct S_PPD f18_S_SDP_PPD(struct S_PPD p0, double p1, void* p2) { return p0; }
EXPORT struct S_PPP f18_S_SDP_PPP(struct S_PPP p0, double p1, void* p2) { return p0; }
EXPORT struct S_I f18_S_SDS_I(struct S_I p0, double p1, struct S_I p2) { return p0; }
EXPORT struct S_F f18_S_SDS_F(struct S_F p0, double p1, struct S_F p2) { return p0; }
EXPORT struct S_D f18_S_SDS_D(struct S_D p0, double p1, struct S_D p2) { return p0; }
EXPORT struct S_P f18_S_SDS_P(struct S_P p0, double p1, struct S_P p2) { return p0; }
EXPORT struct S_II f18_S_SDS_II(struct S_II p0, double p1, struct S_II p2) { return p0; }
EXPORT struct S_IF f18_S_SDS_IF(struct S_IF p0, double p1, struct S_IF p2) { return p0; }
EXPORT struct S_ID f18_S_SDS_ID(struct S_ID p0, double p1, struct S_ID p2) { return p0; }
EXPORT struct S_IP f18_S_SDS_IP(struct S_IP p0, double p1, struct S_IP p2) { return p0; }
EXPORT struct S_FI f18_S_SDS_FI(struct S_FI p0, double p1, struct S_FI p2) { return p0; }
EXPORT struct S_FF f18_S_SDS_FF(struct S_FF p0, double p1, struct S_FF p2) { return p0; }
EXPORT struct S_FD f18_S_SDS_FD(struct S_FD p0, double p1, struct S_FD p2) { return p0; }
EXPORT struct S_FP f18_S_SDS_FP(struct S_FP p0, double p1, struct S_FP p2) { return p0; }
EXPORT struct S_DI f18_S_SDS_DI(struct S_DI p0, double p1, struct S_DI p2) { return p0; }
EXPORT struct S_DF f18_S_SDS_DF(struct S_DF p0, double p1, struct S_DF p2) { return p0; }
EXPORT struct S_DD f18_S_SDS_DD(struct S_DD p0, double p1, struct S_DD p2) { return p0; }
EXPORT struct S_DP f18_S_SDS_DP(struct S_DP p0, double p1, struct S_DP p2) { return p0; }
EXPORT struct S_PI f18_S_SDS_PI(struct S_PI p0, double p1, struct S_PI p2) { return p0; }
EXPORT struct S_PF f18_S_SDS_PF(struct S_PF p0, double p1, struct S_PF p2) { return p0; }
EXPORT struct S_PD f18_S_SDS_PD(struct S_PD p0, double p1, struct S_PD p2) { return p0; }
EXPORT struct S_PP f18_S_SDS_PP(struct S_PP p0, double p1, struct S_PP p2) { return p0; }
EXPORT struct S_III f18_S_SDS_III(struct S_III p0, double p1, struct S_III p2) { return p0; }
EXPORT struct S_IIF f18_S_SDS_IIF(struct S_IIF p0, double p1, struct S_IIF p2) { return p0; }
EXPORT struct S_IID f18_S_SDS_IID(struct S_IID p0, double p1, struct S_IID p2) { return p0; }
EXPORT struct S_IIP f18_S_SDS_IIP(struct S_IIP p0, double p1, struct S_IIP p2) { return p0; }
EXPORT struct S_IFI f18_S_SDS_IFI(struct S_IFI p0, double p1, struct S_IFI p2) { return p0; }
EXPORT struct S_IFF f18_S_SDS_IFF(struct S_IFF p0, double p1, struct S_IFF p2) { return p0; }
EXPORT struct S_IFD f18_S_SDS_IFD(struct S_IFD p0, double p1, struct S_IFD p2) { return p0; }
EXPORT struct S_IFP f18_S_SDS_IFP(struct S_IFP p0, double p1, struct S_IFP p2) { return p0; }
EXPORT struct S_IDI f18_S_SDS_IDI(struct S_IDI p0, double p1, struct S_IDI p2) { return p0; }
EXPORT struct S_IDF f18_S_SDS_IDF(struct S_IDF p0, double p1, struct S_IDF p2) { return p0; }
EXPORT struct S_IDD f18_S_SDS_IDD(struct S_IDD p0, double p1, struct S_IDD p2) { return p0; }
EXPORT struct S_IDP f18_S_SDS_IDP(struct S_IDP p0, double p1, struct S_IDP p2) { return p0; }
EXPORT struct S_IPI f18_S_SDS_IPI(struct S_IPI p0, double p1, struct S_IPI p2) { return p0; }
EXPORT struct S_IPF f18_S_SDS_IPF(struct S_IPF p0, double p1, struct S_IPF p2) { return p0; }
EXPORT struct S_IPD f18_S_SDS_IPD(struct S_IPD p0, double p1, struct S_IPD p2) { return p0; }
EXPORT struct S_IPP f18_S_SDS_IPP(struct S_IPP p0, double p1, struct S_IPP p2) { return p0; }
EXPORT struct S_FII f18_S_SDS_FII(struct S_FII p0, double p1, struct S_FII p2) { return p0; }
EXPORT struct S_FIF f18_S_SDS_FIF(struct S_FIF p0, double p1, struct S_FIF p2) { return p0; }
EXPORT struct S_FID f18_S_SDS_FID(struct S_FID p0, double p1, struct S_FID p2) { return p0; }
EXPORT struct S_FIP f18_S_SDS_FIP(struct S_FIP p0, double p1, struct S_FIP p2) { return p0; }
EXPORT struct S_FFI f18_S_SDS_FFI(struct S_FFI p0, double p1, struct S_FFI p2) { return p0; }
EXPORT struct S_FFF f18_S_SDS_FFF(struct S_FFF p0, double p1, struct S_FFF p2) { return p0; }
EXPORT struct S_FFD f18_S_SDS_FFD(struct S_FFD p0, double p1, struct S_FFD p2) { return p0; }
EXPORT struct S_FFP f18_S_SDS_FFP(struct S_FFP p0, double p1, struct S_FFP p2) { return p0; }
EXPORT struct S_FDI f18_S_SDS_FDI(struct S_FDI p0, double p1, struct S_FDI p2) { return p0; }
EXPORT struct S_FDF f18_S_SDS_FDF(struct S_FDF p0, double p1, struct S_FDF p2) { return p0; }
EXPORT struct S_FDD f18_S_SDS_FDD(struct S_FDD p0, double p1, struct S_FDD p2) { return p0; }
EXPORT struct S_FDP f18_S_SDS_FDP(struct S_FDP p0, double p1, struct S_FDP p2) { return p0; }
EXPORT struct S_FPI f18_S_SDS_FPI(struct S_FPI p0, double p1, struct S_FPI p2) { return p0; }
EXPORT struct S_FPF f18_S_SDS_FPF(struct S_FPF p0, double p1, struct S_FPF p2) { return p0; }
EXPORT struct S_FPD f18_S_SDS_FPD(struct S_FPD p0, double p1, struct S_FPD p2) { return p0; }
EXPORT struct S_FPP f18_S_SDS_FPP(struct S_FPP p0, double p1, struct S_FPP p2) { return p0; }
EXPORT struct S_DII f18_S_SDS_DII(struct S_DII p0, double p1, struct S_DII p2) { return p0; }
EXPORT struct S_DIF f18_S_SDS_DIF(struct S_DIF p0, double p1, struct S_DIF p2) { return p0; }
EXPORT struct S_DID f18_S_SDS_DID(struct S_DID p0, double p1, struct S_DID p2) { return p0; }
EXPORT struct S_DIP f18_S_SDS_DIP(struct S_DIP p0, double p1, struct S_DIP p2) { return p0; }
EXPORT struct S_DFI f18_S_SDS_DFI(struct S_DFI p0, double p1, struct S_DFI p2) { return p0; }
EXPORT struct S_DFF f18_S_SDS_DFF(struct S_DFF p0, double p1, struct S_DFF p2) { return p0; }
EXPORT struct S_DFD f18_S_SDS_DFD(struct S_DFD p0, double p1, struct S_DFD p2) { return p0; }
EXPORT struct S_DFP f18_S_SDS_DFP(struct S_DFP p0, double p1, struct S_DFP p2) { return p0; }
EXPORT struct S_DDI f18_S_SDS_DDI(struct S_DDI p0, double p1, struct S_DDI p2) { return p0; }
EXPORT struct S_DDF f18_S_SDS_DDF(struct S_DDF p0, double p1, struct S_DDF p2) { return p0; }
EXPORT struct S_DDD f18_S_SDS_DDD(struct S_DDD p0, double p1, struct S_DDD p2) { return p0; }
EXPORT struct S_DDP f18_S_SDS_DDP(struct S_DDP p0, double p1, struct S_DDP p2) { return p0; }
EXPORT struct S_DPI f18_S_SDS_DPI(struct S_DPI p0, double p1, struct S_DPI p2) { return p0; }
EXPORT struct S_DPF f18_S_SDS_DPF(struct S_DPF p0, double p1, struct S_DPF p2) { return p0; }
EXPORT struct S_DPD f18_S_SDS_DPD(struct S_DPD p0, double p1, struct S_DPD p2) { return p0; }
EXPORT struct S_DPP f18_S_SDS_DPP(struct S_DPP p0, double p1, struct S_DPP p2) { return p0; }
EXPORT struct S_PII f18_S_SDS_PII(struct S_PII p0, double p1, struct S_PII p2) { return p0; }
EXPORT struct S_PIF f18_S_SDS_PIF(struct S_PIF p0, double p1, struct S_PIF p2) { return p0; }
EXPORT struct S_PID f18_S_SDS_PID(struct S_PID p0, double p1, struct S_PID p2) { return p0; }
EXPORT struct S_PIP f18_S_SDS_PIP(struct S_PIP p0, double p1, struct S_PIP p2) { return p0; }
EXPORT struct S_PFI f18_S_SDS_PFI(struct S_PFI p0, double p1, struct S_PFI p2) { return p0; }
EXPORT struct S_PFF f18_S_SDS_PFF(struct S_PFF p0, double p1, struct S_PFF p2) { return p0; }
EXPORT struct S_PFD f18_S_SDS_PFD(struct S_PFD p0, double p1, struct S_PFD p2) { return p0; }
EXPORT struct S_PFP f18_S_SDS_PFP(struct S_PFP p0, double p1, struct S_PFP p2) { return p0; }
EXPORT struct S_PDI f18_S_SDS_PDI(struct S_PDI p0, double p1, struct S_PDI p2) { return p0; }
EXPORT struct S_PDF f18_S_SDS_PDF(struct S_PDF p0, double p1, struct S_PDF p2) { return p0; }
EXPORT struct S_PDD f18_S_SDS_PDD(struct S_PDD p0, double p1, struct S_PDD p2) { return p0; }
EXPORT struct S_PDP f18_S_SDS_PDP(struct S_PDP p0, double p1, struct S_PDP p2) { return p0; }
EXPORT struct S_PPI f18_S_SDS_PPI(struct S_PPI p0, double p1, struct S_PPI p2) { return p0; }
EXPORT struct S_PPF f18_S_SDS_PPF(struct S_PPF p0, double p1, struct S_PPF p2) { return p0; }
EXPORT struct S_PPD f18_S_SDS_PPD(struct S_PPD p0, double p1, struct S_PPD p2) { return p0; }
EXPORT struct S_PPP f18_S_SDS_PPP(struct S_PPP p0, double p1, struct S_PPP p2) { return p0; }
EXPORT struct S_I f18_S_SPI_I(struct S_I p0, void* p1, int p2) { return p0; }
EXPORT struct S_F f18_S_SPI_F(struct S_F p0, void* p1, int p2) { return p0; }
EXPORT struct S_D f18_S_SPI_D(struct S_D p0, void* p1, int p2) { return p0; }
EXPORT struct S_P f18_S_SPI_P(struct S_P p0, void* p1, int p2) { return p0; }
EXPORT struct S_II f18_S_SPI_II(struct S_II p0, void* p1, int p2) { return p0; }
EXPORT struct S_IF f18_S_SPI_IF(struct S_IF p0, void* p1, int p2) { return p0; }
EXPORT struct S_ID f18_S_SPI_ID(struct S_ID p0, void* p1, int p2) { return p0; }
EXPORT struct S_IP f18_S_SPI_IP(struct S_IP p0, void* p1, int p2) { return p0; }
EXPORT struct S_FI f18_S_SPI_FI(struct S_FI p0, void* p1, int p2) { return p0; }
EXPORT struct S_FF f18_S_SPI_FF(struct S_FF p0, void* p1, int p2) { return p0; }
EXPORT struct S_FD f18_S_SPI_FD(struct S_FD p0, void* p1, int p2) { return p0; }
EXPORT struct S_FP f18_S_SPI_FP(struct S_FP p0, void* p1, int p2) { return p0; }
EXPORT struct S_DI f18_S_SPI_DI(struct S_DI p0, void* p1, int p2) { return p0; }
EXPORT struct S_DF f18_S_SPI_DF(struct S_DF p0, void* p1, int p2) { return p0; }
EXPORT struct S_DD f18_S_SPI_DD(struct S_DD p0, void* p1, int p2) { return p0; }
EXPORT struct S_DP f18_S_SPI_DP(struct S_DP p0, void* p1, int p2) { return p0; }
EXPORT struct S_PI f18_S_SPI_PI(struct S_PI p0, void* p1, int p2) { return p0; }
EXPORT struct S_PF f18_S_SPI_PF(struct S_PF p0, void* p1, int p2) { return p0; }
EXPORT struct S_PD f18_S_SPI_PD(struct S_PD p0, void* p1, int p2) { return p0; }
EXPORT struct S_PP f18_S_SPI_PP(struct S_PP p0, void* p1, int p2) { return p0; }
EXPORT struct S_III f18_S_SPI_III(struct S_III p0, void* p1, int p2) { return p0; }
EXPORT struct S_IIF f18_S_SPI_IIF(struct S_IIF p0, void* p1, int p2) { return p0; }
EXPORT struct S_IID f18_S_SPI_IID(struct S_IID p0, void* p1, int p2) { return p0; }
EXPORT struct S_IIP f18_S_SPI_IIP(struct S_IIP p0, void* p1, int p2) { return p0; }
EXPORT struct S_IFI f18_S_SPI_IFI(struct S_IFI p0, void* p1, int p2) { return p0; }
EXPORT struct S_IFF f18_S_SPI_IFF(struct S_IFF p0, void* p1, int p2) { return p0; }
EXPORT struct S_IFD f18_S_SPI_IFD(struct S_IFD p0, void* p1, int p2) { return p0; }
EXPORT struct S_IFP f18_S_SPI_IFP(struct S_IFP p0, void* p1, int p2) { return p0; }
EXPORT struct S_IDI f18_S_SPI_IDI(struct S_IDI p0, void* p1, int p2) { return p0; }
EXPORT struct S_IDF f18_S_SPI_IDF(struct S_IDF p0, void* p1, int p2) { return p0; }
EXPORT struct S_IDD f18_S_SPI_IDD(struct S_IDD p0, void* p1, int p2) { return p0; }
EXPORT struct S_IDP f18_S_SPI_IDP(struct S_IDP p0, void* p1, int p2) { return p0; }
EXPORT struct S_IPI f18_S_SPI_IPI(struct S_IPI p0, void* p1, int p2) { return p0; }
EXPORT struct S_IPF f18_S_SPI_IPF(struct S_IPF p0, void* p1, int p2) { return p0; }
EXPORT struct S_IPD f18_S_SPI_IPD(struct S_IPD p0, void* p1, int p2) { return p0; }
EXPORT struct S_IPP f18_S_SPI_IPP(struct S_IPP p0, void* p1, int p2) { return p0; }
EXPORT struct S_FII f18_S_SPI_FII(struct S_FII p0, void* p1, int p2) { return p0; }
EXPORT struct S_FIF f18_S_SPI_FIF(struct S_FIF p0, void* p1, int p2) { return p0; }
EXPORT struct S_FID f18_S_SPI_FID(struct S_FID p0, void* p1, int p2) { return p0; }
EXPORT struct S_FIP f18_S_SPI_FIP(struct S_FIP p0, void* p1, int p2) { return p0; }
EXPORT struct S_FFI f18_S_SPI_FFI(struct S_FFI p0, void* p1, int p2) { return p0; }
EXPORT struct S_FFF f18_S_SPI_FFF(struct S_FFF p0, void* p1, int p2) { return p0; }
EXPORT struct S_FFD f18_S_SPI_FFD(struct S_FFD p0, void* p1, int p2) { return p0; }
EXPORT struct S_FFP f18_S_SPI_FFP(struct S_FFP p0, void* p1, int p2) { return p0; }
EXPORT struct S_FDI f18_S_SPI_FDI(struct S_FDI p0, void* p1, int p2) { return p0; }
EXPORT struct S_FDF f18_S_SPI_FDF(struct S_FDF p0, void* p1, int p2) { return p0; }
EXPORT struct S_FDD f18_S_SPI_FDD(struct S_FDD p0, void* p1, int p2) { return p0; }
EXPORT struct S_FDP f18_S_SPI_FDP(struct S_FDP p0, void* p1, int p2) { return p0; }
EXPORT struct S_FPI f18_S_SPI_FPI(struct S_FPI p0, void* p1, int p2) { return p0; }
EXPORT struct S_FPF f18_S_SPI_FPF(struct S_FPF p0, void* p1, int p2) { return p0; }
EXPORT struct S_FPD f18_S_SPI_FPD(struct S_FPD p0, void* p1, int p2) { return p0; }
EXPORT struct S_FPP f18_S_SPI_FPP(struct S_FPP p0, void* p1, int p2) { return p0; }
EXPORT struct S_DII f18_S_SPI_DII(struct S_DII p0, void* p1, int p2) { return p0; }
EXPORT struct S_DIF f18_S_SPI_DIF(struct S_DIF p0, void* p1, int p2) { return p0; }
EXPORT struct S_DID f18_S_SPI_DID(struct S_DID p0, void* p1, int p2) { return p0; }
EXPORT struct S_DIP f18_S_SPI_DIP(struct S_DIP p0, void* p1, int p2) { return p0; }
EXPORT struct S_DFI f18_S_SPI_DFI(struct S_DFI p0, void* p1, int p2) { return p0; }
EXPORT struct S_DFF f18_S_SPI_DFF(struct S_DFF p0, void* p1, int p2) { return p0; }
EXPORT struct S_DFD f18_S_SPI_DFD(struct S_DFD p0, void* p1, int p2) { return p0; }
EXPORT struct S_DFP f18_S_SPI_DFP(struct S_DFP p0, void* p1, int p2) { return p0; }
EXPORT struct S_DDI f18_S_SPI_DDI(struct S_DDI p0, void* p1, int p2) { return p0; }
EXPORT struct S_DDF f18_S_SPI_DDF(struct S_DDF p0, void* p1, int p2) { return p0; }
EXPORT struct S_DDD f18_S_SPI_DDD(struct S_DDD p0, void* p1, int p2) { return p0; }
EXPORT struct S_DDP f18_S_SPI_DDP(struct S_DDP p0, void* p1, int p2) { return p0; }
EXPORT struct S_DPI f18_S_SPI_DPI(struct S_DPI p0, void* p1, int p2) { return p0; }
EXPORT struct S_DPF f18_S_SPI_DPF(struct S_DPF p0, void* p1, int p2) { return p0; }
EXPORT struct S_DPD f18_S_SPI_DPD(struct S_DPD p0, void* p1, int p2) { return p0; }
EXPORT struct S_DPP f18_S_SPI_DPP(struct S_DPP p0, void* p1, int p2) { return p0; }
EXPORT struct S_PII f18_S_SPI_PII(struct S_PII p0, void* p1, int p2) { return p0; }
EXPORT struct S_PIF f18_S_SPI_PIF(struct S_PIF p0, void* p1, int p2) { return p0; }
EXPORT struct S_PID f18_S_SPI_PID(struct S_PID p0, void* p1, int p2) { return p0; }
EXPORT struct S_PIP f18_S_SPI_PIP(struct S_PIP p0, void* p1, int p2) { return p0; }
EXPORT struct S_PFI f18_S_SPI_PFI(struct S_PFI p0, void* p1, int p2) { return p0; }
EXPORT struct S_PFF f18_S_SPI_PFF(struct S_PFF p0, void* p1, int p2) { return p0; }
EXPORT struct S_PFD f18_S_SPI_PFD(struct S_PFD p0, void* p1, int p2) { return p0; }
EXPORT struct S_PFP f18_S_SPI_PFP(struct S_PFP p0, void* p1, int p2) { return p0; }
EXPORT struct S_PDI f18_S_SPI_PDI(struct S_PDI p0, void* p1, int p2) { return p0; }
EXPORT struct S_PDF f18_S_SPI_PDF(struct S_PDF p0, void* p1, int p2) { return p0; }
EXPORT struct S_PDD f18_S_SPI_PDD(struct S_PDD p0, void* p1, int p2) { return p0; }
EXPORT struct S_PDP f18_S_SPI_PDP(struct S_PDP p0, void* p1, int p2) { return p0; }
EXPORT struct S_PPI f18_S_SPI_PPI(struct S_PPI p0, void* p1, int p2) { return p0; }
EXPORT struct S_PPF f18_S_SPI_PPF(struct S_PPF p0, void* p1, int p2) { return p0; }
EXPORT struct S_PPD f18_S_SPI_PPD(struct S_PPD p0, void* p1, int p2) { return p0; }
EXPORT struct S_PPP f18_S_SPI_PPP(struct S_PPP p0, void* p1, int p2) { return p0; }
EXPORT struct S_I f18_S_SPF_I(struct S_I p0, void* p1, float p2) { return p0; }
EXPORT struct S_F f18_S_SPF_F(struct S_F p0, void* p1, float p2) { return p0; }
EXPORT struct S_D f18_S_SPF_D(struct S_D p0, void* p1, float p2) { return p0; }
EXPORT struct S_P f18_S_SPF_P(struct S_P p0, void* p1, float p2) { return p0; }
EXPORT struct S_II f18_S_SPF_II(struct S_II p0, void* p1, float p2) { return p0; }
EXPORT struct S_IF f18_S_SPF_IF(struct S_IF p0, void* p1, float p2) { return p0; }
EXPORT struct S_ID f18_S_SPF_ID(struct S_ID p0, void* p1, float p2) { return p0; }
EXPORT struct S_IP f18_S_SPF_IP(struct S_IP p0, void* p1, float p2) { return p0; }
EXPORT struct S_FI f18_S_SPF_FI(struct S_FI p0, void* p1, float p2) { return p0; }
EXPORT struct S_FF f18_S_SPF_FF(struct S_FF p0, void* p1, float p2) { return p0; }
EXPORT struct S_FD f18_S_SPF_FD(struct S_FD p0, void* p1, float p2) { return p0; }
EXPORT struct S_FP f18_S_SPF_FP(struct S_FP p0, void* p1, float p2) { return p0; }
EXPORT struct S_DI f18_S_SPF_DI(struct S_DI p0, void* p1, float p2) { return p0; }
EXPORT struct S_DF f18_S_SPF_DF(struct S_DF p0, void* p1, float p2) { return p0; }
EXPORT struct S_DD f18_S_SPF_DD(struct S_DD p0, void* p1, float p2) { return p0; }
EXPORT struct S_DP f18_S_SPF_DP(struct S_DP p0, void* p1, float p2) { return p0; }
EXPORT struct S_PI f18_S_SPF_PI(struct S_PI p0, void* p1, float p2) { return p0; }
EXPORT struct S_PF f18_S_SPF_PF(struct S_PF p0, void* p1, float p2) { return p0; }
EXPORT struct S_PD f18_S_SPF_PD(struct S_PD p0, void* p1, float p2) { return p0; }
EXPORT struct S_PP f18_S_SPF_PP(struct S_PP p0, void* p1, float p2) { return p0; }
EXPORT struct S_III f18_S_SPF_III(struct S_III p0, void* p1, float p2) { return p0; }
EXPORT struct S_IIF f18_S_SPF_IIF(struct S_IIF p0, void* p1, float p2) { return p0; }
EXPORT struct S_IID f18_S_SPF_IID(struct S_IID p0, void* p1, float p2) { return p0; }
EXPORT struct S_IIP f18_S_SPF_IIP(struct S_IIP p0, void* p1, float p2) { return p0; }
EXPORT struct S_IFI f18_S_SPF_IFI(struct S_IFI p0, void* p1, float p2) { return p0; }
EXPORT struct S_IFF f18_S_SPF_IFF(struct S_IFF p0, void* p1, float p2) { return p0; }
EXPORT struct S_IFD f18_S_SPF_IFD(struct S_IFD p0, void* p1, float p2) { return p0; }
EXPORT struct S_IFP f18_S_SPF_IFP(struct S_IFP p0, void* p1, float p2) { return p0; }
EXPORT struct S_IDI f18_S_SPF_IDI(struct S_IDI p0, void* p1, float p2) { return p0; }
EXPORT struct S_IDF f18_S_SPF_IDF(struct S_IDF p0, void* p1, float p2) { return p0; }
EXPORT struct S_IDD f18_S_SPF_IDD(struct S_IDD p0, void* p1, float p2) { return p0; }
EXPORT struct S_IDP f18_S_SPF_IDP(struct S_IDP p0, void* p1, float p2) { return p0; }
EXPORT struct S_IPI f18_S_SPF_IPI(struct S_IPI p0, void* p1, float p2) { return p0; }
EXPORT struct S_IPF f18_S_SPF_IPF(struct S_IPF p0, void* p1, float p2) { return p0; }
EXPORT struct S_IPD f18_S_SPF_IPD(struct S_IPD p0, void* p1, float p2) { return p0; }
EXPORT struct S_IPP f18_S_SPF_IPP(struct S_IPP p0, void* p1, float p2) { return p0; }
EXPORT struct S_FII f18_S_SPF_FII(struct S_FII p0, void* p1, float p2) { return p0; }
EXPORT struct S_FIF f18_S_SPF_FIF(struct S_FIF p0, void* p1, float p2) { return p0; }
EXPORT struct S_FID f18_S_SPF_FID(struct S_FID p0, void* p1, float p2) { return p0; }
EXPORT struct S_FIP f18_S_SPF_FIP(struct S_FIP p0, void* p1, float p2) { return p0; }
EXPORT struct S_FFI f18_S_SPF_FFI(struct S_FFI p0, void* p1, float p2) { return p0; }
EXPORT struct S_FFF f18_S_SPF_FFF(struct S_FFF p0, void* p1, float p2) { return p0; }
EXPORT struct S_FFD f18_S_SPF_FFD(struct S_FFD p0, void* p1, float p2) { return p0; }
EXPORT struct S_FFP f18_S_SPF_FFP(struct S_FFP p0, void* p1, float p2) { return p0; }
EXPORT struct S_FDI f18_S_SPF_FDI(struct S_FDI p0, void* p1, float p2) { return p0; }
EXPORT struct S_FDF f18_S_SPF_FDF(struct S_FDF p0, void* p1, float p2) { return p0; }
EXPORT struct S_FDD f18_S_SPF_FDD(struct S_FDD p0, void* p1, float p2) { return p0; }
EXPORT struct S_FDP f18_S_SPF_FDP(struct S_FDP p0, void* p1, float p2) { return p0; }
EXPORT struct S_FPI f18_S_SPF_FPI(struct S_FPI p0, void* p1, float p2) { return p0; }
EXPORT struct S_FPF f18_S_SPF_FPF(struct S_FPF p0, void* p1, float p2) { return p0; }
EXPORT struct S_FPD f18_S_SPF_FPD(struct S_FPD p0, void* p1, float p2) { return p0; }
EXPORT struct S_FPP f18_S_SPF_FPP(struct S_FPP p0, void* p1, float p2) { return p0; }
EXPORT struct S_DII f18_S_SPF_DII(struct S_DII p0, void* p1, float p2) { return p0; }
EXPORT struct S_DIF f18_S_SPF_DIF(struct S_DIF p0, void* p1, float p2) { return p0; }
EXPORT struct S_DID f18_S_SPF_DID(struct S_DID p0, void* p1, float p2) { return p0; }
EXPORT struct S_DIP f18_S_SPF_DIP(struct S_DIP p0, void* p1, float p2) { return p0; }
EXPORT struct S_DFI f18_S_SPF_DFI(struct S_DFI p0, void* p1, float p2) { return p0; }
EXPORT struct S_DFF f18_S_SPF_DFF(struct S_DFF p0, void* p1, float p2) { return p0; }
EXPORT struct S_DFD f18_S_SPF_DFD(struct S_DFD p0, void* p1, float p2) { return p0; }
EXPORT struct S_DFP f19_S_SPF_DFP(struct S_DFP p0, void* p1, float p2) { return p0; }
EXPORT struct S_DDI f19_S_SPF_DDI(struct S_DDI p0, void* p1, float p2) { return p0; }
EXPORT struct S_DDF f19_S_SPF_DDF(struct S_DDF p0, void* p1, float p2) { return p0; }
EXPORT struct S_DDD f19_S_SPF_DDD(struct S_DDD p0, void* p1, float p2) { return p0; }
EXPORT struct S_DDP f19_S_SPF_DDP(struct S_DDP p0, void* p1, float p2) { return p0; }
EXPORT struct S_DPI f19_S_SPF_DPI(struct S_DPI p0, void* p1, float p2) { return p0; }
EXPORT struct S_DPF f19_S_SPF_DPF(struct S_DPF p0, void* p1, float p2) { return p0; }
EXPORT struct S_DPD f19_S_SPF_DPD(struct S_DPD p0, void* p1, float p2) { return p0; }
EXPORT struct S_DPP f19_S_SPF_DPP(struct S_DPP p0, void* p1, float p2) { return p0; }
EXPORT struct S_PII f19_S_SPF_PII(struct S_PII p0, void* p1, float p2) { return p0; }
EXPORT struct S_PIF f19_S_SPF_PIF(struct S_PIF p0, void* p1, float p2) { return p0; }
EXPORT struct S_PID f19_S_SPF_PID(struct S_PID p0, void* p1, float p2) { return p0; }
EXPORT struct S_PIP f19_S_SPF_PIP(struct S_PIP p0, void* p1, float p2) { return p0; }
EXPORT struct S_PFI f19_S_SPF_PFI(struct S_PFI p0, void* p1, float p2) { return p0; }
EXPORT struct S_PFF f19_S_SPF_PFF(struct S_PFF p0, void* p1, float p2) { return p0; }
EXPORT struct S_PFD f19_S_SPF_PFD(struct S_PFD p0, void* p1, float p2) { return p0; }
EXPORT struct S_PFP f19_S_SPF_PFP(struct S_PFP p0, void* p1, float p2) { return p0; }
EXPORT struct S_PDI f19_S_SPF_PDI(struct S_PDI p0, void* p1, float p2) { return p0; }
EXPORT struct S_PDF f19_S_SPF_PDF(struct S_PDF p0, void* p1, float p2) { return p0; }
EXPORT struct S_PDD f19_S_SPF_PDD(struct S_PDD p0, void* p1, float p2) { return p0; }
EXPORT struct S_PDP f19_S_SPF_PDP(struct S_PDP p0, void* p1, float p2) { return p0; }
EXPORT struct S_PPI f19_S_SPF_PPI(struct S_PPI p0, void* p1, float p2) { return p0; }
EXPORT struct S_PPF f19_S_SPF_PPF(struct S_PPF p0, void* p1, float p2) { return p0; }
EXPORT struct S_PPD f19_S_SPF_PPD(struct S_PPD p0, void* p1, float p2) { return p0; }
EXPORT struct S_PPP f19_S_SPF_PPP(struct S_PPP p0, void* p1, float p2) { return p0; }
EXPORT struct S_I f19_S_SPD_I(struct S_I p0, void* p1, double p2) { return p0; }
EXPORT struct S_F f19_S_SPD_F(struct S_F p0, void* p1, double p2) { return p0; }
EXPORT struct S_D f19_S_SPD_D(struct S_D p0, void* p1, double p2) { return p0; }
EXPORT struct S_P f19_S_SPD_P(struct S_P p0, void* p1, double p2) { return p0; }
EXPORT struct S_II f19_S_SPD_II(struct S_II p0, void* p1, double p2) { return p0; }
EXPORT struct S_IF f19_S_SPD_IF(struct S_IF p0, void* p1, double p2) { return p0; }
EXPORT struct S_ID f19_S_SPD_ID(struct S_ID p0, void* p1, double p2) { return p0; }
EXPORT struct S_IP f19_S_SPD_IP(struct S_IP p0, void* p1, double p2) { return p0; }
EXPORT struct S_FI f19_S_SPD_FI(struct S_FI p0, void* p1, double p2) { return p0; }
EXPORT struct S_FF f19_S_SPD_FF(struct S_FF p0, void* p1, double p2) { return p0; }
EXPORT struct S_FD f19_S_SPD_FD(struct S_FD p0, void* p1, double p2) { return p0; }
EXPORT struct S_FP f19_S_SPD_FP(struct S_FP p0, void* p1, double p2) { return p0; }
EXPORT struct S_DI f19_S_SPD_DI(struct S_DI p0, void* p1, double p2) { return p0; }
EXPORT struct S_DF f19_S_SPD_DF(struct S_DF p0, void* p1, double p2) { return p0; }
EXPORT struct S_DD f19_S_SPD_DD(struct S_DD p0, void* p1, double p2) { return p0; }
EXPORT struct S_DP f19_S_SPD_DP(struct S_DP p0, void* p1, double p2) { return p0; }
EXPORT struct S_PI f19_S_SPD_PI(struct S_PI p0, void* p1, double p2) { return p0; }
EXPORT struct S_PF f19_S_SPD_PF(struct S_PF p0, void* p1, double p2) { return p0; }
EXPORT struct S_PD f19_S_SPD_PD(struct S_PD p0, void* p1, double p2) { return p0; }
EXPORT struct S_PP f19_S_SPD_PP(struct S_PP p0, void* p1, double p2) { return p0; }
EXPORT struct S_III f19_S_SPD_III(struct S_III p0, void* p1, double p2) { return p0; }
EXPORT struct S_IIF f19_S_SPD_IIF(struct S_IIF p0, void* p1, double p2) { return p0; }
EXPORT struct S_IID f19_S_SPD_IID(struct S_IID p0, void* p1, double p2) { return p0; }
EXPORT struct S_IIP f19_S_SPD_IIP(struct S_IIP p0, void* p1, double p2) { return p0; }
EXPORT struct S_IFI f19_S_SPD_IFI(struct S_IFI p0, void* p1, double p2) { return p0; }
EXPORT struct S_IFF f19_S_SPD_IFF(struct S_IFF p0, void* p1, double p2) { return p0; }
EXPORT struct S_IFD f19_S_SPD_IFD(struct S_IFD p0, void* p1, double p2) { return p0; }
EXPORT struct S_IFP f19_S_SPD_IFP(struct S_IFP p0, void* p1, double p2) { return p0; }
EXPORT struct S_IDI f19_S_SPD_IDI(struct S_IDI p0, void* p1, double p2) { return p0; }
EXPORT struct S_IDF f19_S_SPD_IDF(struct S_IDF p0, void* p1, double p2) { return p0; }
EXPORT struct S_IDD f19_S_SPD_IDD(struct S_IDD p0, void* p1, double p2) { return p0; }
EXPORT struct S_IDP f19_S_SPD_IDP(struct S_IDP p0, void* p1, double p2) { return p0; }
EXPORT struct S_IPI f19_S_SPD_IPI(struct S_IPI p0, void* p1, double p2) { return p0; }
EXPORT struct S_IPF f19_S_SPD_IPF(struct S_IPF p0, void* p1, double p2) { return p0; }
EXPORT struct S_IPD f19_S_SPD_IPD(struct S_IPD p0, void* p1, double p2) { return p0; }
EXPORT struct S_IPP f19_S_SPD_IPP(struct S_IPP p0, void* p1, double p2) { return p0; }
EXPORT struct S_FII f19_S_SPD_FII(struct S_FII p0, void* p1, double p2) { return p0; }
EXPORT struct S_FIF f19_S_SPD_FIF(struct S_FIF p0, void* p1, double p2) { return p0; }
EXPORT struct S_FID f19_S_SPD_FID(struct S_FID p0, void* p1, double p2) { return p0; }
EXPORT struct S_FIP f19_S_SPD_FIP(struct S_FIP p0, void* p1, double p2) { return p0; }
EXPORT struct S_FFI f19_S_SPD_FFI(struct S_FFI p0, void* p1, double p2) { return p0; }
EXPORT struct S_FFF f19_S_SPD_FFF(struct S_FFF p0, void* p1, double p2) { return p0; }
EXPORT struct S_FFD f19_S_SPD_FFD(struct S_FFD p0, void* p1, double p2) { return p0; }
EXPORT struct S_FFP f19_S_SPD_FFP(struct S_FFP p0, void* p1, double p2) { return p0; }
EXPORT struct S_FDI f19_S_SPD_FDI(struct S_FDI p0, void* p1, double p2) { return p0; }
EXPORT struct S_FDF f19_S_SPD_FDF(struct S_FDF p0, void* p1, double p2) { return p0; }
EXPORT struct S_FDD f19_S_SPD_FDD(struct S_FDD p0, void* p1, double p2) { return p0; }
EXPORT struct S_FDP f19_S_SPD_FDP(struct S_FDP p0, void* p1, double p2) { return p0; }
EXPORT struct S_FPI f19_S_SPD_FPI(struct S_FPI p0, void* p1, double p2) { return p0; }
EXPORT struct S_FPF f19_S_SPD_FPF(struct S_FPF p0, void* p1, double p2) { return p0; }
EXPORT struct S_FPD f19_S_SPD_FPD(struct S_FPD p0, void* p1, double p2) { return p0; }
EXPORT struct S_FPP f19_S_SPD_FPP(struct S_FPP p0, void* p1, double p2) { return p0; }
EXPORT struct S_DII f19_S_SPD_DII(struct S_DII p0, void* p1, double p2) { return p0; }
EXPORT struct S_DIF f19_S_SPD_DIF(struct S_DIF p0, void* p1, double p2) { return p0; }
EXPORT struct S_DID f19_S_SPD_DID(struct S_DID p0, void* p1, double p2) { return p0; }
EXPORT struct S_DIP f19_S_SPD_DIP(struct S_DIP p0, void* p1, double p2) { return p0; }
EXPORT struct S_DFI f19_S_SPD_DFI(struct S_DFI p0, void* p1, double p2) { return p0; }
EXPORT struct S_DFF f19_S_SPD_DFF(struct S_DFF p0, void* p1, double p2) { return p0; }
EXPORT struct S_DFD f19_S_SPD_DFD(struct S_DFD p0, void* p1, double p2) { return p0; }
EXPORT struct S_DFP f19_S_SPD_DFP(struct S_DFP p0, void* p1, double p2) { return p0; }
EXPORT struct S_DDI f19_S_SPD_DDI(struct S_DDI p0, void* p1, double p2) { return p0; }
EXPORT struct S_DDF f19_S_SPD_DDF(struct S_DDF p0, void* p1, double p2) { return p0; }
EXPORT struct S_DDD f19_S_SPD_DDD(struct S_DDD p0, void* p1, double p2) { return p0; }
EXPORT struct S_DDP f19_S_SPD_DDP(struct S_DDP p0, void* p1, double p2) { return p0; }
EXPORT struct S_DPI f19_S_SPD_DPI(struct S_DPI p0, void* p1, double p2) { return p0; }
EXPORT struct S_DPF f19_S_SPD_DPF(struct S_DPF p0, void* p1, double p2) { return p0; }
EXPORT struct S_DPD f19_S_SPD_DPD(struct S_DPD p0, void* p1, double p2) { return p0; }
EXPORT struct S_DPP f19_S_SPD_DPP(struct S_DPP p0, void* p1, double p2) { return p0; }
EXPORT struct S_PII f19_S_SPD_PII(struct S_PII p0, void* p1, double p2) { return p0; }
EXPORT struct S_PIF f19_S_SPD_PIF(struct S_PIF p0, void* p1, double p2) { return p0; }
EXPORT struct S_PID f19_S_SPD_PID(struct S_PID p0, void* p1, double p2) { return p0; }
EXPORT struct S_PIP f19_S_SPD_PIP(struct S_PIP p0, void* p1, double p2) { return p0; }
EXPORT struct S_PFI f19_S_SPD_PFI(struct S_PFI p0, void* p1, double p2) { return p0; }
EXPORT struct S_PFF f19_S_SPD_PFF(struct S_PFF p0, void* p1, double p2) { return p0; }
EXPORT struct S_PFD f19_S_SPD_PFD(struct S_PFD p0, void* p1, double p2) { return p0; }
EXPORT struct S_PFP f19_S_SPD_PFP(struct S_PFP p0, void* p1, double p2) { return p0; }
EXPORT struct S_PDI f19_S_SPD_PDI(struct S_PDI p0, void* p1, double p2) { return p0; }
EXPORT struct S_PDF f19_S_SPD_PDF(struct S_PDF p0, void* p1, double p2) { return p0; }
EXPORT struct S_PDD f19_S_SPD_PDD(struct S_PDD p0, void* p1, double p2) { return p0; }
EXPORT struct S_PDP f19_S_SPD_PDP(struct S_PDP p0, void* p1, double p2) { return p0; }
EXPORT struct S_PPI f19_S_SPD_PPI(struct S_PPI p0, void* p1, double p2) { return p0; }
EXPORT struct S_PPF f19_S_SPD_PPF(struct S_PPF p0, void* p1, double p2) { return p0; }
EXPORT struct S_PPD f19_S_SPD_PPD(struct S_PPD p0, void* p1, double p2) { return p0; }
EXPORT struct S_PPP f19_S_SPD_PPP(struct S_PPP p0, void* p1, double p2) { return p0; }
EXPORT struct S_I f19_S_SPP_I(struct S_I p0, void* p1, void* p2) { return p0; }
EXPORT struct S_F f19_S_SPP_F(struct S_F p0, void* p1, void* p2) { return p0; }
EXPORT struct S_D f19_S_SPP_D(struct S_D p0, void* p1, void* p2) { return p0; }
EXPORT struct S_P f19_S_SPP_P(struct S_P p0, void* p1, void* p2) { return p0; }
EXPORT struct S_II f19_S_SPP_II(struct S_II p0, void* p1, void* p2) { return p0; }
EXPORT struct S_IF f19_S_SPP_IF(struct S_IF p0, void* p1, void* p2) { return p0; }
EXPORT struct S_ID f19_S_SPP_ID(struct S_ID p0, void* p1, void* p2) { return p0; }
EXPORT struct S_IP f19_S_SPP_IP(struct S_IP p0, void* p1, void* p2) { return p0; }
EXPORT struct S_FI f19_S_SPP_FI(struct S_FI p0, void* p1, void* p2) { return p0; }
EXPORT struct S_FF f19_S_SPP_FF(struct S_FF p0, void* p1, void* p2) { return p0; }
EXPORT struct S_FD f19_S_SPP_FD(struct S_FD p0, void* p1, void* p2) { return p0; }
EXPORT struct S_FP f19_S_SPP_FP(struct S_FP p0, void* p1, void* p2) { return p0; }
EXPORT struct S_DI f19_S_SPP_DI(struct S_DI p0, void* p1, void* p2) { return p0; }
EXPORT struct S_DF f19_S_SPP_DF(struct S_DF p0, void* p1, void* p2) { return p0; }
EXPORT struct S_DD f19_S_SPP_DD(struct S_DD p0, void* p1, void* p2) { return p0; }
EXPORT struct S_DP f19_S_SPP_DP(struct S_DP p0, void* p1, void* p2) { return p0; }
EXPORT struct S_PI f19_S_SPP_PI(struct S_PI p0, void* p1, void* p2) { return p0; }
EXPORT struct S_PF f19_S_SPP_PF(struct S_PF p0, void* p1, void* p2) { return p0; }
EXPORT struct S_PD f19_S_SPP_PD(struct S_PD p0, void* p1, void* p2) { return p0; }
EXPORT struct S_PP f19_S_SPP_PP(struct S_PP p0, void* p1, void* p2) { return p0; }
EXPORT struct S_III f19_S_SPP_III(struct S_III p0, void* p1, void* p2) { return p0; }
EXPORT struct S_IIF f19_S_SPP_IIF(struct S_IIF p0, void* p1, void* p2) { return p0; }
EXPORT struct S_IID f19_S_SPP_IID(struct S_IID p0, void* p1, void* p2) { return p0; }
EXPORT struct S_IIP f19_S_SPP_IIP(struct S_IIP p0, void* p1, void* p2) { return p0; }
EXPORT struct S_IFI f19_S_SPP_IFI(struct S_IFI p0, void* p1, void* p2) { return p0; }
EXPORT struct S_IFF f19_S_SPP_IFF(struct S_IFF p0, void* p1, void* p2) { return p0; }
EXPORT struct S_IFD f19_S_SPP_IFD(struct S_IFD p0, void* p1, void* p2) { return p0; }
EXPORT struct S_IFP f19_S_SPP_IFP(struct S_IFP p0, void* p1, void* p2) { return p0; }
EXPORT struct S_IDI f19_S_SPP_IDI(struct S_IDI p0, void* p1, void* p2) { return p0; }
EXPORT struct S_IDF f19_S_SPP_IDF(struct S_IDF p0, void* p1, void* p2) { return p0; }
EXPORT struct S_IDD f19_S_SPP_IDD(struct S_IDD p0, void* p1, void* p2) { return p0; }
EXPORT struct S_IDP f19_S_SPP_IDP(struct S_IDP p0, void* p1, void* p2) { return p0; }
EXPORT struct S_IPI f19_S_SPP_IPI(struct S_IPI p0, void* p1, void* p2) { return p0; }
EXPORT struct S_IPF f19_S_SPP_IPF(struct S_IPF p0, void* p1, void* p2) { return p0; }
EXPORT struct S_IPD f19_S_SPP_IPD(struct S_IPD p0, void* p1, void* p2) { return p0; }
EXPORT struct S_IPP f19_S_SPP_IPP(struct S_IPP p0, void* p1, void* p2) { return p0; }
EXPORT struct S_FII f19_S_SPP_FII(struct S_FII p0, void* p1, void* p2) { return p0; }
EXPORT struct S_FIF f19_S_SPP_FIF(struct S_FIF p0, void* p1, void* p2) { return p0; }
EXPORT struct S_FID f19_S_SPP_FID(struct S_FID p0, void* p1, void* p2) { return p0; }
EXPORT struct S_FIP f19_S_SPP_FIP(struct S_FIP p0, void* p1, void* p2) { return p0; }
EXPORT struct S_FFI f19_S_SPP_FFI(struct S_FFI p0, void* p1, void* p2) { return p0; }
EXPORT struct S_FFF f19_S_SPP_FFF(struct S_FFF p0, void* p1, void* p2) { return p0; }
EXPORT struct S_FFD f19_S_SPP_FFD(struct S_FFD p0, void* p1, void* p2) { return p0; }
EXPORT struct S_FFP f19_S_SPP_FFP(struct S_FFP p0, void* p1, void* p2) { return p0; }
EXPORT struct S_FDI f19_S_SPP_FDI(struct S_FDI p0, void* p1, void* p2) { return p0; }
EXPORT struct S_FDF f19_S_SPP_FDF(struct S_FDF p0, void* p1, void* p2) { return p0; }
EXPORT struct S_FDD f19_S_SPP_FDD(struct S_FDD p0, void* p1, void* p2) { return p0; }
EXPORT struct S_FDP f19_S_SPP_FDP(struct S_FDP p0, void* p1, void* p2) { return p0; }
EXPORT struct S_FPI f19_S_SPP_FPI(struct S_FPI p0, void* p1, void* p2) { return p0; }
EXPORT struct S_FPF f19_S_SPP_FPF(struct S_FPF p0, void* p1, void* p2) { return p0; }
EXPORT struct S_FPD f19_S_SPP_FPD(struct S_FPD p0, void* p1, void* p2) { return p0; }
EXPORT struct S_FPP f19_S_SPP_FPP(struct S_FPP p0, void* p1, void* p2) { return p0; }
EXPORT struct S_DII f19_S_SPP_DII(struct S_DII p0, void* p1, void* p2) { return p0; }
EXPORT struct S_DIF f19_S_SPP_DIF(struct S_DIF p0, void* p1, void* p2) { return p0; }
EXPORT struct S_DID f19_S_SPP_DID(struct S_DID p0, void* p1, void* p2) { return p0; }
EXPORT struct S_DIP f19_S_SPP_DIP(struct S_DIP p0, void* p1, void* p2) { return p0; }
EXPORT struct S_DFI f19_S_SPP_DFI(struct S_DFI p0, void* p1, void* p2) { return p0; }
EXPORT struct S_DFF f19_S_SPP_DFF(struct S_DFF p0, void* p1, void* p2) { return p0; }
EXPORT struct S_DFD f19_S_SPP_DFD(struct S_DFD p0, void* p1, void* p2) { return p0; }
EXPORT struct S_DFP f19_S_SPP_DFP(struct S_DFP p0, void* p1, void* p2) { return p0; }
EXPORT struct S_DDI f19_S_SPP_DDI(struct S_DDI p0, void* p1, void* p2) { return p0; }
EXPORT struct S_DDF f19_S_SPP_DDF(struct S_DDF p0, void* p1, void* p2) { return p0; }
EXPORT struct S_DDD f19_S_SPP_DDD(struct S_DDD p0, void* p1, void* p2) { return p0; }
EXPORT struct S_DDP f19_S_SPP_DDP(struct S_DDP p0, void* p1, void* p2) { return p0; }
EXPORT struct S_DPI f19_S_SPP_DPI(struct S_DPI p0, void* p1, void* p2) { return p0; }
EXPORT struct S_DPF f19_S_SPP_DPF(struct S_DPF p0, void* p1, void* p2) { return p0; }
EXPORT struct S_DPD f19_S_SPP_DPD(struct S_DPD p0, void* p1, void* p2) { return p0; }
EXPORT struct S_DPP f19_S_SPP_DPP(struct S_DPP p0, void* p1, void* p2) { return p0; }
EXPORT struct S_PII f19_S_SPP_PII(struct S_PII p0, void* p1, void* p2) { return p0; }
EXPORT struct S_PIF f19_S_SPP_PIF(struct S_PIF p0, void* p1, void* p2) { return p0; }
EXPORT struct S_PID f19_S_SPP_PID(struct S_PID p0, void* p1, void* p2) { return p0; }
EXPORT struct S_PIP f19_S_SPP_PIP(struct S_PIP p0, void* p1, void* p2) { return p0; }
EXPORT struct S_PFI f19_S_SPP_PFI(struct S_PFI p0, void* p1, void* p2) { return p0; }
EXPORT struct S_PFF f19_S_SPP_PFF(struct S_PFF p0, void* p1, void* p2) { return p0; }
EXPORT struct S_PFD f19_S_SPP_PFD(struct S_PFD p0, void* p1, void* p2) { return p0; }
EXPORT struct S_PFP f19_S_SPP_PFP(struct S_PFP p0, void* p1, void* p2) { return p0; }
EXPORT struct S_PDI f19_S_SPP_PDI(struct S_PDI p0, void* p1, void* p2) { return p0; }
EXPORT struct S_PDF f19_S_SPP_PDF(struct S_PDF p0, void* p1, void* p2) { return p0; }
EXPORT struct S_PDD f19_S_SPP_PDD(struct S_PDD p0, void* p1, void* p2) { return p0; }
EXPORT struct S_PDP f19_S_SPP_PDP(struct S_PDP p0, void* p1, void* p2) { return p0; }
EXPORT struct S_PPI f19_S_SPP_PPI(struct S_PPI p0, void* p1, void* p2) { return p0; }
EXPORT struct S_PPF f19_S_SPP_PPF(struct S_PPF p0, void* p1, void* p2) { return p0; }
EXPORT struct S_PPD f19_S_SPP_PPD(struct S_PPD p0, void* p1, void* p2) { return p0; }
EXPORT struct S_PPP f19_S_SPP_PPP(struct S_PPP p0, void* p1, void* p2) { return p0; }
EXPORT struct S_I f19_S_SPS_I(struct S_I p0, void* p1, struct S_I p2) { return p0; }
EXPORT struct S_F f19_S_SPS_F(struct S_F p0, void* p1, struct S_F p2) { return p0; }
EXPORT struct S_D f19_S_SPS_D(struct S_D p0, void* p1, struct S_D p2) { return p0; }
EXPORT struct S_P f19_S_SPS_P(struct S_P p0, void* p1, struct S_P p2) { return p0; }
EXPORT struct S_II f19_S_SPS_II(struct S_II p0, void* p1, struct S_II p2) { return p0; }
EXPORT struct S_IF f19_S_SPS_IF(struct S_IF p0, void* p1, struct S_IF p2) { return p0; }
EXPORT struct S_ID f19_S_SPS_ID(struct S_ID p0, void* p1, struct S_ID p2) { return p0; }
EXPORT struct S_IP f19_S_SPS_IP(struct S_IP p0, void* p1, struct S_IP p2) { return p0; }
EXPORT struct S_FI f19_S_SPS_FI(struct S_FI p0, void* p1, struct S_FI p2) { return p0; }
EXPORT struct S_FF f19_S_SPS_FF(struct S_FF p0, void* p1, struct S_FF p2) { return p0; }
EXPORT struct S_FD f19_S_SPS_FD(struct S_FD p0, void* p1, struct S_FD p2) { return p0; }
EXPORT struct S_FP f19_S_SPS_FP(struct S_FP p0, void* p1, struct S_FP p2) { return p0; }
EXPORT struct S_DI f19_S_SPS_DI(struct S_DI p0, void* p1, struct S_DI p2) { return p0; }
EXPORT struct S_DF f19_S_SPS_DF(struct S_DF p0, void* p1, struct S_DF p2) { return p0; }
EXPORT struct S_DD f19_S_SPS_DD(struct S_DD p0, void* p1, struct S_DD p2) { return p0; }
EXPORT struct S_DP f19_S_SPS_DP(struct S_DP p0, void* p1, struct S_DP p2) { return p0; }
EXPORT struct S_PI f19_S_SPS_PI(struct S_PI p0, void* p1, struct S_PI p2) { return p0; }
EXPORT struct S_PF f19_S_SPS_PF(struct S_PF p0, void* p1, struct S_PF p2) { return p0; }
EXPORT struct S_PD f19_S_SPS_PD(struct S_PD p0, void* p1, struct S_PD p2) { return p0; }
EXPORT struct S_PP f19_S_SPS_PP(struct S_PP p0, void* p1, struct S_PP p2) { return p0; }
EXPORT struct S_III f19_S_SPS_III(struct S_III p0, void* p1, struct S_III p2) { return p0; }
EXPORT struct S_IIF f19_S_SPS_IIF(struct S_IIF p0, void* p1, struct S_IIF p2) { return p0; }
EXPORT struct S_IID f19_S_SPS_IID(struct S_IID p0, void* p1, struct S_IID p2) { return p0; }
EXPORT struct S_IIP f19_S_SPS_IIP(struct S_IIP p0, void* p1, struct S_IIP p2) { return p0; }
EXPORT struct S_IFI f19_S_SPS_IFI(struct S_IFI p0, void* p1, struct S_IFI p2) { return p0; }
EXPORT struct S_IFF f19_S_SPS_IFF(struct S_IFF p0, void* p1, struct S_IFF p2) { return p0; }
EXPORT struct S_IFD f19_S_SPS_IFD(struct S_IFD p0, void* p1, struct S_IFD p2) { return p0; }
EXPORT struct S_IFP f19_S_SPS_IFP(struct S_IFP p0, void* p1, struct S_IFP p2) { return p0; }
EXPORT struct S_IDI f19_S_SPS_IDI(struct S_IDI p0, void* p1, struct S_IDI p2) { return p0; }
EXPORT struct S_IDF f19_S_SPS_IDF(struct S_IDF p0, void* p1, struct S_IDF p2) { return p0; }
EXPORT struct S_IDD f19_S_SPS_IDD(struct S_IDD p0, void* p1, struct S_IDD p2) { return p0; }
EXPORT struct S_IDP f19_S_SPS_IDP(struct S_IDP p0, void* p1, struct S_IDP p2) { return p0; }
EXPORT struct S_IPI f19_S_SPS_IPI(struct S_IPI p0, void* p1, struct S_IPI p2) { return p0; }
EXPORT struct S_IPF f19_S_SPS_IPF(struct S_IPF p0, void* p1, struct S_IPF p2) { return p0; }
EXPORT struct S_IPD f19_S_SPS_IPD(struct S_IPD p0, void* p1, struct S_IPD p2) { return p0; }
EXPORT struct S_IPP f19_S_SPS_IPP(struct S_IPP p0, void* p1, struct S_IPP p2) { return p0; }
EXPORT struct S_FII f19_S_SPS_FII(struct S_FII p0, void* p1, struct S_FII p2) { return p0; }
EXPORT struct S_FIF f19_S_SPS_FIF(struct S_FIF p0, void* p1, struct S_FIF p2) { return p0; }
EXPORT struct S_FID f19_S_SPS_FID(struct S_FID p0, void* p1, struct S_FID p2) { return p0; }
EXPORT struct S_FIP f19_S_SPS_FIP(struct S_FIP p0, void* p1, struct S_FIP p2) { return p0; }
EXPORT struct S_FFI f19_S_SPS_FFI(struct S_FFI p0, void* p1, struct S_FFI p2) { return p0; }
EXPORT struct S_FFF f19_S_SPS_FFF(struct S_FFF p0, void* p1, struct S_FFF p2) { return p0; }
EXPORT struct S_FFD f19_S_SPS_FFD(struct S_FFD p0, void* p1, struct S_FFD p2) { return p0; }
EXPORT struct S_FFP f19_S_SPS_FFP(struct S_FFP p0, void* p1, struct S_FFP p2) { return p0; }
EXPORT struct S_FDI f19_S_SPS_FDI(struct S_FDI p0, void* p1, struct S_FDI p2) { return p0; }
EXPORT struct S_FDF f19_S_SPS_FDF(struct S_FDF p0, void* p1, struct S_FDF p2) { return p0; }
EXPORT struct S_FDD f19_S_SPS_FDD(struct S_FDD p0, void* p1, struct S_FDD p2) { return p0; }
EXPORT struct S_FDP f19_S_SPS_FDP(struct S_FDP p0, void* p1, struct S_FDP p2) { return p0; }
EXPORT struct S_FPI f19_S_SPS_FPI(struct S_FPI p0, void* p1, struct S_FPI p2) { return p0; }
EXPORT struct S_FPF f19_S_SPS_FPF(struct S_FPF p0, void* p1, struct S_FPF p2) { return p0; }
EXPORT struct S_FPD f19_S_SPS_FPD(struct S_FPD p0, void* p1, struct S_FPD p2) { return p0; }
EXPORT struct S_FPP f19_S_SPS_FPP(struct S_FPP p0, void* p1, struct S_FPP p2) { return p0; }
EXPORT struct S_DII f19_S_SPS_DII(struct S_DII p0, void* p1, struct S_DII p2) { return p0; }
EXPORT struct S_DIF f19_S_SPS_DIF(struct S_DIF p0, void* p1, struct S_DIF p2) { return p0; }
EXPORT struct S_DID f19_S_SPS_DID(struct S_DID p0, void* p1, struct S_DID p2) { return p0; }
EXPORT struct S_DIP f19_S_SPS_DIP(struct S_DIP p0, void* p1, struct S_DIP p2) { return p0; }
EXPORT struct S_DFI f19_S_SPS_DFI(struct S_DFI p0, void* p1, struct S_DFI p2) { return p0; }
EXPORT struct S_DFF f19_S_SPS_DFF(struct S_DFF p0, void* p1, struct S_DFF p2) { return p0; }
EXPORT struct S_DFD f19_S_SPS_DFD(struct S_DFD p0, void* p1, struct S_DFD p2) { return p0; }
EXPORT struct S_DFP f19_S_SPS_DFP(struct S_DFP p0, void* p1, struct S_DFP p2) { return p0; }
EXPORT struct S_DDI f19_S_SPS_DDI(struct S_DDI p0, void* p1, struct S_DDI p2) { return p0; }
EXPORT struct S_DDF f19_S_SPS_DDF(struct S_DDF p0, void* p1, struct S_DDF p2) { return p0; }
EXPORT struct S_DDD f19_S_SPS_DDD(struct S_DDD p0, void* p1, struct S_DDD p2) { return p0; }
EXPORT struct S_DDP f19_S_SPS_DDP(struct S_DDP p0, void* p1, struct S_DDP p2) { return p0; }
EXPORT struct S_DPI f19_S_SPS_DPI(struct S_DPI p0, void* p1, struct S_DPI p2) { return p0; }
EXPORT struct S_DPF f19_S_SPS_DPF(struct S_DPF p0, void* p1, struct S_DPF p2) { return p0; }
EXPORT struct S_DPD f19_S_SPS_DPD(struct S_DPD p0, void* p1, struct S_DPD p2) { return p0; }
EXPORT struct S_DPP f19_S_SPS_DPP(struct S_DPP p0, void* p1, struct S_DPP p2) { return p0; }
EXPORT struct S_PII f19_S_SPS_PII(struct S_PII p0, void* p1, struct S_PII p2) { return p0; }
EXPORT struct S_PIF f19_S_SPS_PIF(struct S_PIF p0, void* p1, struct S_PIF p2) { return p0; }
EXPORT struct S_PID f19_S_SPS_PID(struct S_PID p0, void* p1, struct S_PID p2) { return p0; }
EXPORT struct S_PIP f19_S_SPS_PIP(struct S_PIP p0, void* p1, struct S_PIP p2) { return p0; }
EXPORT struct S_PFI f19_S_SPS_PFI(struct S_PFI p0, void* p1, struct S_PFI p2) { return p0; }
EXPORT struct S_PFF f19_S_SPS_PFF(struct S_PFF p0, void* p1, struct S_PFF p2) { return p0; }
EXPORT struct S_PFD f19_S_SPS_PFD(struct S_PFD p0, void* p1, struct S_PFD p2) { return p0; }
EXPORT struct S_PFP f19_S_SPS_PFP(struct S_PFP p0, void* p1, struct S_PFP p2) { return p0; }
EXPORT struct S_PDI f19_S_SPS_PDI(struct S_PDI p0, void* p1, struct S_PDI p2) { return p0; }
EXPORT struct S_PDF f19_S_SPS_PDF(struct S_PDF p0, void* p1, struct S_PDF p2) { return p0; }
EXPORT struct S_PDD f19_S_SPS_PDD(struct S_PDD p0, void* p1, struct S_PDD p2) { return p0; }
EXPORT struct S_PDP f19_S_SPS_PDP(struct S_PDP p0, void* p1, struct S_PDP p2) { return p0; }
EXPORT struct S_PPI f19_S_SPS_PPI(struct S_PPI p0, void* p1, struct S_PPI p2) { return p0; }
EXPORT struct S_PPF f19_S_SPS_PPF(struct S_PPF p0, void* p1, struct S_PPF p2) { return p0; }
EXPORT struct S_PPD f19_S_SPS_PPD(struct S_PPD p0, void* p1, struct S_PPD p2) { return p0; }
EXPORT struct S_PPP f19_S_SPS_PPP(struct S_PPP p0, void* p1, struct S_PPP p2) { return p0; }
EXPORT struct S_I f19_S_SSI_I(struct S_I p0, struct S_I p1, int p2) { return p0; }
EXPORT struct S_F f19_S_SSI_F(struct S_F p0, struct S_F p1, int p2) { return p0; }
EXPORT struct S_D f19_S_SSI_D(struct S_D p0, struct S_D p1, int p2) { return p0; }
EXPORT struct S_P f19_S_SSI_P(struct S_P p0, struct S_P p1, int p2) { return p0; }
EXPORT struct S_II f19_S_SSI_II(struct S_II p0, struct S_II p1, int p2) { return p0; }
EXPORT struct S_IF f19_S_SSI_IF(struct S_IF p0, struct S_IF p1, int p2) { return p0; }
EXPORT struct S_ID f19_S_SSI_ID(struct S_ID p0, struct S_ID p1, int p2) { return p0; }
EXPORT struct S_IP f19_S_SSI_IP(struct S_IP p0, struct S_IP p1, int p2) { return p0; }
EXPORT struct S_FI f19_S_SSI_FI(struct S_FI p0, struct S_FI p1, int p2) { return p0; }
EXPORT struct S_FF f19_S_SSI_FF(struct S_FF p0, struct S_FF p1, int p2) { return p0; }
EXPORT struct S_FD f19_S_SSI_FD(struct S_FD p0, struct S_FD p1, int p2) { return p0; }
EXPORT struct S_FP f19_S_SSI_FP(struct S_FP p0, struct S_FP p1, int p2) { return p0; }
EXPORT struct S_DI f19_S_SSI_DI(struct S_DI p0, struct S_DI p1, int p2) { return p0; }
EXPORT struct S_DF f19_S_SSI_DF(struct S_DF p0, struct S_DF p1, int p2) { return p0; }
EXPORT struct S_DD f19_S_SSI_DD(struct S_DD p0, struct S_DD p1, int p2) { return p0; }
EXPORT struct S_DP f19_S_SSI_DP(struct S_DP p0, struct S_DP p1, int p2) { return p0; }
EXPORT struct S_PI f19_S_SSI_PI(struct S_PI p0, struct S_PI p1, int p2) { return p0; }
EXPORT struct S_PF f19_S_SSI_PF(struct S_PF p0, struct S_PF p1, int p2) { return p0; }
EXPORT struct S_PD f19_S_SSI_PD(struct S_PD p0, struct S_PD p1, int p2) { return p0; }
EXPORT struct S_PP f19_S_SSI_PP(struct S_PP p0, struct S_PP p1, int p2) { return p0; }
EXPORT struct S_III f19_S_SSI_III(struct S_III p0, struct S_III p1, int p2) { return p0; }
EXPORT struct S_IIF f19_S_SSI_IIF(struct S_IIF p0, struct S_IIF p1, int p2) { return p0; }
EXPORT struct S_IID f19_S_SSI_IID(struct S_IID p0, struct S_IID p1, int p2) { return p0; }
EXPORT struct S_IIP f19_S_SSI_IIP(struct S_IIP p0, struct S_IIP p1, int p2) { return p0; }
EXPORT struct S_IFI f19_S_SSI_IFI(struct S_IFI p0, struct S_IFI p1, int p2) { return p0; }
EXPORT struct S_IFF f19_S_SSI_IFF(struct S_IFF p0, struct S_IFF p1, int p2) { return p0; }
EXPORT struct S_IFD f19_S_SSI_IFD(struct S_IFD p0, struct S_IFD p1, int p2) { return p0; }
EXPORT struct S_IFP f19_S_SSI_IFP(struct S_IFP p0, struct S_IFP p1, int p2) { return p0; }
EXPORT struct S_IDI f19_S_SSI_IDI(struct S_IDI p0, struct S_IDI p1, int p2) { return p0; }
EXPORT struct S_IDF f19_S_SSI_IDF(struct S_IDF p0, struct S_IDF p1, int p2) { return p0; }
EXPORT struct S_IDD f19_S_SSI_IDD(struct S_IDD p0, struct S_IDD p1, int p2) { return p0; }
EXPORT struct S_IDP f19_S_SSI_IDP(struct S_IDP p0, struct S_IDP p1, int p2) { return p0; }
EXPORT struct S_IPI f19_S_SSI_IPI(struct S_IPI p0, struct S_IPI p1, int p2) { return p0; }
EXPORT struct S_IPF f19_S_SSI_IPF(struct S_IPF p0, struct S_IPF p1, int p2) { return p0; }
EXPORT struct S_IPD f19_S_SSI_IPD(struct S_IPD p0, struct S_IPD p1, int p2) { return p0; }
EXPORT struct S_IPP f19_S_SSI_IPP(struct S_IPP p0, struct S_IPP p1, int p2) { return p0; }
EXPORT struct S_FII f19_S_SSI_FII(struct S_FII p0, struct S_FII p1, int p2) { return p0; }
EXPORT struct S_FIF f19_S_SSI_FIF(struct S_FIF p0, struct S_FIF p1, int p2) { return p0; }
EXPORT struct S_FID f19_S_SSI_FID(struct S_FID p0, struct S_FID p1, int p2) { return p0; }
EXPORT struct S_FIP f19_S_SSI_FIP(struct S_FIP p0, struct S_FIP p1, int p2) { return p0; }
EXPORT struct S_FFI f19_S_SSI_FFI(struct S_FFI p0, struct S_FFI p1, int p2) { return p0; }
EXPORT struct S_FFF f19_S_SSI_FFF(struct S_FFF p0, struct S_FFF p1, int p2) { return p0; }
EXPORT struct S_FFD f19_S_SSI_FFD(struct S_FFD p0, struct S_FFD p1, int p2) { return p0; }
EXPORT struct S_FFP f19_S_SSI_FFP(struct S_FFP p0, struct S_FFP p1, int p2) { return p0; }
EXPORT struct S_FDI f19_S_SSI_FDI(struct S_FDI p0, struct S_FDI p1, int p2) { return p0; }
EXPORT struct S_FDF f19_S_SSI_FDF(struct S_FDF p0, struct S_FDF p1, int p2) { return p0; }
EXPORT struct S_FDD f19_S_SSI_FDD(struct S_FDD p0, struct S_FDD p1, int p2) { return p0; }
EXPORT struct S_FDP f19_S_SSI_FDP(struct S_FDP p0, struct S_FDP p1, int p2) { return p0; }
EXPORT struct S_FPI f19_S_SSI_FPI(struct S_FPI p0, struct S_FPI p1, int p2) { return p0; }
EXPORT struct S_FPF f19_S_SSI_FPF(struct S_FPF p0, struct S_FPF p1, int p2) { return p0; }
EXPORT struct S_FPD f19_S_SSI_FPD(struct S_FPD p0, struct S_FPD p1, int p2) { return p0; }
EXPORT struct S_FPP f19_S_SSI_FPP(struct S_FPP p0, struct S_FPP p1, int p2) { return p0; }
EXPORT struct S_DII f19_S_SSI_DII(struct S_DII p0, struct S_DII p1, int p2) { return p0; }
EXPORT struct S_DIF f19_S_SSI_DIF(struct S_DIF p0, struct S_DIF p1, int p2) { return p0; }
EXPORT struct S_DID f19_S_SSI_DID(struct S_DID p0, struct S_DID p1, int p2) { return p0; }
EXPORT struct S_DIP f19_S_SSI_DIP(struct S_DIP p0, struct S_DIP p1, int p2) { return p0; }
EXPORT struct S_DFI f19_S_SSI_DFI(struct S_DFI p0, struct S_DFI p1, int p2) { return p0; }
EXPORT struct S_DFF f19_S_SSI_DFF(struct S_DFF p0, struct S_DFF p1, int p2) { return p0; }
EXPORT struct S_DFD f19_S_SSI_DFD(struct S_DFD p0, struct S_DFD p1, int p2) { return p0; }
EXPORT struct S_DFP f19_S_SSI_DFP(struct S_DFP p0, struct S_DFP p1, int p2) { return p0; }
EXPORT struct S_DDI f19_S_SSI_DDI(struct S_DDI p0, struct S_DDI p1, int p2) { return p0; }
EXPORT struct S_DDF f19_S_SSI_DDF(struct S_DDF p0, struct S_DDF p1, int p2) { return p0; }
EXPORT struct S_DDD f19_S_SSI_DDD(struct S_DDD p0, struct S_DDD p1, int p2) { return p0; }
EXPORT struct S_DDP f19_S_SSI_DDP(struct S_DDP p0, struct S_DDP p1, int p2) { return p0; }
EXPORT struct S_DPI f19_S_SSI_DPI(struct S_DPI p0, struct S_DPI p1, int p2) { return p0; }
EXPORT struct S_DPF f19_S_SSI_DPF(struct S_DPF p0, struct S_DPF p1, int p2) { return p0; }
EXPORT struct S_DPD f19_S_SSI_DPD(struct S_DPD p0, struct S_DPD p1, int p2) { return p0; }
EXPORT struct S_DPP f19_S_SSI_DPP(struct S_DPP p0, struct S_DPP p1, int p2) { return p0; }
EXPORT struct S_PII f19_S_SSI_PII(struct S_PII p0, struct S_PII p1, int p2) { return p0; }
EXPORT struct S_PIF f19_S_SSI_PIF(struct S_PIF p0, struct S_PIF p1, int p2) { return p0; }
EXPORT struct S_PID f19_S_SSI_PID(struct S_PID p0, struct S_PID p1, int p2) { return p0; }
EXPORT struct S_PIP f19_S_SSI_PIP(struct S_PIP p0, struct S_PIP p1, int p2) { return p0; }
EXPORT struct S_PFI f19_S_SSI_PFI(struct S_PFI p0, struct S_PFI p1, int p2) { return p0; }
EXPORT struct S_PFF f19_S_SSI_PFF(struct S_PFF p0, struct S_PFF p1, int p2) { return p0; }
EXPORT struct S_PFD f19_S_SSI_PFD(struct S_PFD p0, struct S_PFD p1, int p2) { return p0; }
EXPORT struct S_PFP f19_S_SSI_PFP(struct S_PFP p0, struct S_PFP p1, int p2) { return p0; }
EXPORT struct S_PDI f19_S_SSI_PDI(struct S_PDI p0, struct S_PDI p1, int p2) { return p0; }
EXPORT struct S_PDF f19_S_SSI_PDF(struct S_PDF p0, struct S_PDF p1, int p2) { return p0; }
EXPORT struct S_PDD f19_S_SSI_PDD(struct S_PDD p0, struct S_PDD p1, int p2) { return p0; }
EXPORT struct S_PDP f19_S_SSI_PDP(struct S_PDP p0, struct S_PDP p1, int p2) { return p0; }
EXPORT struct S_PPI f19_S_SSI_PPI(struct S_PPI p0, struct S_PPI p1, int p2) { return p0; }
EXPORT struct S_PPF f19_S_SSI_PPF(struct S_PPF p0, struct S_PPF p1, int p2) { return p0; }
EXPORT struct S_PPD f19_S_SSI_PPD(struct S_PPD p0, struct S_PPD p1, int p2) { return p0; }
EXPORT struct S_PPP f19_S_SSI_PPP(struct S_PPP p0, struct S_PPP p1, int p2) { return p0; }
EXPORT struct S_I f19_S_SSF_I(struct S_I p0, struct S_I p1, float p2) { return p0; }
EXPORT struct S_F f19_S_SSF_F(struct S_F p0, struct S_F p1, float p2) { return p0; }
EXPORT struct S_D f19_S_SSF_D(struct S_D p0, struct S_D p1, float p2) { return p0; }
EXPORT struct S_P f19_S_SSF_P(struct S_P p0, struct S_P p1, float p2) { return p0; }
EXPORT struct S_II f19_S_SSF_II(struct S_II p0, struct S_II p1, float p2) { return p0; }
EXPORT struct S_IF f19_S_SSF_IF(struct S_IF p0, struct S_IF p1, float p2) { return p0; }
EXPORT struct S_ID f19_S_SSF_ID(struct S_ID p0, struct S_ID p1, float p2) { return p0; }
EXPORT struct S_IP f19_S_SSF_IP(struct S_IP p0, struct S_IP p1, float p2) { return p0; }
EXPORT struct S_FI f19_S_SSF_FI(struct S_FI p0, struct S_FI p1, float p2) { return p0; }
EXPORT struct S_FF f19_S_SSF_FF(struct S_FF p0, struct S_FF p1, float p2) { return p0; }
EXPORT struct S_FD f19_S_SSF_FD(struct S_FD p0, struct S_FD p1, float p2) { return p0; }
EXPORT struct S_FP f19_S_SSF_FP(struct S_FP p0, struct S_FP p1, float p2) { return p0; }
EXPORT struct S_DI f19_S_SSF_DI(struct S_DI p0, struct S_DI p1, float p2) { return p0; }
EXPORT struct S_DF f19_S_SSF_DF(struct S_DF p0, struct S_DF p1, float p2) { return p0; }
EXPORT struct S_DD f19_S_SSF_DD(struct S_DD p0, struct S_DD p1, float p2) { return p0; }
EXPORT struct S_DP f19_S_SSF_DP(struct S_DP p0, struct S_DP p1, float p2) { return p0; }
EXPORT struct S_PI f19_S_SSF_PI(struct S_PI p0, struct S_PI p1, float p2) { return p0; }
EXPORT struct S_PF f19_S_SSF_PF(struct S_PF p0, struct S_PF p1, float p2) { return p0; }
EXPORT struct S_PD f19_S_SSF_PD(struct S_PD p0, struct S_PD p1, float p2) { return p0; }
EXPORT struct S_PP f19_S_SSF_PP(struct S_PP p0, struct S_PP p1, float p2) { return p0; }
EXPORT struct S_III f19_S_SSF_III(struct S_III p0, struct S_III p1, float p2) { return p0; }
EXPORT struct S_IIF f19_S_SSF_IIF(struct S_IIF p0, struct S_IIF p1, float p2) { return p0; }
EXPORT struct S_IID f19_S_SSF_IID(struct S_IID p0, struct S_IID p1, float p2) { return p0; }
EXPORT struct S_IIP f19_S_SSF_IIP(struct S_IIP p0, struct S_IIP p1, float p2) { return p0; }
EXPORT struct S_IFI f19_S_SSF_IFI(struct S_IFI p0, struct S_IFI p1, float p2) { return p0; }
EXPORT struct S_IFF f19_S_SSF_IFF(struct S_IFF p0, struct S_IFF p1, float p2) { return p0; }
EXPORT struct S_IFD f19_S_SSF_IFD(struct S_IFD p0, struct S_IFD p1, float p2) { return p0; }
EXPORT struct S_IFP f19_S_SSF_IFP(struct S_IFP p0, struct S_IFP p1, float p2) { return p0; }
EXPORT struct S_IDI f19_S_SSF_IDI(struct S_IDI p0, struct S_IDI p1, float p2) { return p0; }
EXPORT struct S_IDF f19_S_SSF_IDF(struct S_IDF p0, struct S_IDF p1, float p2) { return p0; }
EXPORT struct S_IDD f19_S_SSF_IDD(struct S_IDD p0, struct S_IDD p1, float p2) { return p0; }
EXPORT struct S_IDP f19_S_SSF_IDP(struct S_IDP p0, struct S_IDP p1, float p2) { return p0; }
EXPORT struct S_IPI f19_S_SSF_IPI(struct S_IPI p0, struct S_IPI p1, float p2) { return p0; }
EXPORT struct S_IPF f19_S_SSF_IPF(struct S_IPF p0, struct S_IPF p1, float p2) { return p0; }
EXPORT struct S_IPD f19_S_SSF_IPD(struct S_IPD p0, struct S_IPD p1, float p2) { return p0; }
EXPORT struct S_IPP f19_S_SSF_IPP(struct S_IPP p0, struct S_IPP p1, float p2) { return p0; }
EXPORT struct S_FII f19_S_SSF_FII(struct S_FII p0, struct S_FII p1, float p2) { return p0; }
EXPORT struct S_FIF f19_S_SSF_FIF(struct S_FIF p0, struct S_FIF p1, float p2) { return p0; }
EXPORT struct S_FID f19_S_SSF_FID(struct S_FID p0, struct S_FID p1, float p2) { return p0; }
EXPORT struct S_FIP f19_S_SSF_FIP(struct S_FIP p0, struct S_FIP p1, float p2) { return p0; }
EXPORT struct S_FFI f19_S_SSF_FFI(struct S_FFI p0, struct S_FFI p1, float p2) { return p0; }
EXPORT struct S_FFF f19_S_SSF_FFF(struct S_FFF p0, struct S_FFF p1, float p2) { return p0; }
EXPORT struct S_FFD f19_S_SSF_FFD(struct S_FFD p0, struct S_FFD p1, float p2) { return p0; }
EXPORT struct S_FFP f19_S_SSF_FFP(struct S_FFP p0, struct S_FFP p1, float p2) { return p0; }
EXPORT struct S_FDI f19_S_SSF_FDI(struct S_FDI p0, struct S_FDI p1, float p2) { return p0; }
EXPORT struct S_FDF f19_S_SSF_FDF(struct S_FDF p0, struct S_FDF p1, float p2) { return p0; }
EXPORT struct S_FDD f19_S_SSF_FDD(struct S_FDD p0, struct S_FDD p1, float p2) { return p0; }
EXPORT struct S_FDP f19_S_SSF_FDP(struct S_FDP p0, struct S_FDP p1, float p2) { return p0; }
EXPORT struct S_FPI f19_S_SSF_FPI(struct S_FPI p0, struct S_FPI p1, float p2) { return p0; }
EXPORT struct S_FPF f19_S_SSF_FPF(struct S_FPF p0, struct S_FPF p1, float p2) { return p0; }
EXPORT struct S_FPD f19_S_SSF_FPD(struct S_FPD p0, struct S_FPD p1, float p2) { return p0; }
EXPORT struct S_FPP f19_S_SSF_FPP(struct S_FPP p0, struct S_FPP p1, float p2) { return p0; }
EXPORT struct S_DII f19_S_SSF_DII(struct S_DII p0, struct S_DII p1, float p2) { return p0; }
EXPORT struct S_DIF f19_S_SSF_DIF(struct S_DIF p0, struct S_DIF p1, float p2) { return p0; }
EXPORT struct S_DID f19_S_SSF_DID(struct S_DID p0, struct S_DID p1, float p2) { return p0; }
EXPORT struct S_DIP f19_S_SSF_DIP(struct S_DIP p0, struct S_DIP p1, float p2) { return p0; }
EXPORT struct S_DFI f19_S_SSF_DFI(struct S_DFI p0, struct S_DFI p1, float p2) { return p0; }
EXPORT struct S_DFF f19_S_SSF_DFF(struct S_DFF p0, struct S_DFF p1, float p2) { return p0; }
EXPORT struct S_DFD f19_S_SSF_DFD(struct S_DFD p0, struct S_DFD p1, float p2) { return p0; }
EXPORT struct S_DFP f19_S_SSF_DFP(struct S_DFP p0, struct S_DFP p1, float p2) { return p0; }
EXPORT struct S_DDI f19_S_SSF_DDI(struct S_DDI p0, struct S_DDI p1, float p2) { return p0; }
EXPORT struct S_DDF f19_S_SSF_DDF(struct S_DDF p0, struct S_DDF p1, float p2) { return p0; }
EXPORT struct S_DDD f19_S_SSF_DDD(struct S_DDD p0, struct S_DDD p1, float p2) { return p0; }
EXPORT struct S_DDP f19_S_SSF_DDP(struct S_DDP p0, struct S_DDP p1, float p2) { return p0; }
EXPORT struct S_DPI f19_S_SSF_DPI(struct S_DPI p0, struct S_DPI p1, float p2) { return p0; }
EXPORT struct S_DPF f19_S_SSF_DPF(struct S_DPF p0, struct S_DPF p1, float p2) { return p0; }
EXPORT struct S_DPD f19_S_SSF_DPD(struct S_DPD p0, struct S_DPD p1, float p2) { return p0; }
EXPORT struct S_DPP f19_S_SSF_DPP(struct S_DPP p0, struct S_DPP p1, float p2) { return p0; }
EXPORT struct S_PII f19_S_SSF_PII(struct S_PII p0, struct S_PII p1, float p2) { return p0; }
EXPORT struct S_PIF f19_S_SSF_PIF(struct S_PIF p0, struct S_PIF p1, float p2) { return p0; }
EXPORT struct S_PID f19_S_SSF_PID(struct S_PID p0, struct S_PID p1, float p2) { return p0; }
EXPORT struct S_PIP f19_S_SSF_PIP(struct S_PIP p0, struct S_PIP p1, float p2) { return p0; }
EXPORT struct S_PFI f19_S_SSF_PFI(struct S_PFI p0, struct S_PFI p1, float p2) { return p0; }
EXPORT struct S_PFF f19_S_SSF_PFF(struct S_PFF p0, struct S_PFF p1, float p2) { return p0; }
EXPORT struct S_PFD f19_S_SSF_PFD(struct S_PFD p0, struct S_PFD p1, float p2) { return p0; }
EXPORT struct S_PFP f19_S_SSF_PFP(struct S_PFP p0, struct S_PFP p1, float p2) { return p0; }
EXPORT struct S_PDI f19_S_SSF_PDI(struct S_PDI p0, struct S_PDI p1, float p2) { return p0; }
EXPORT struct S_PDF f19_S_SSF_PDF(struct S_PDF p0, struct S_PDF p1, float p2) { return p0; }
EXPORT struct S_PDD f19_S_SSF_PDD(struct S_PDD p0, struct S_PDD p1, float p2) { return p0; }
EXPORT struct S_PDP f19_S_SSF_PDP(struct S_PDP p0, struct S_PDP p1, float p2) { return p0; }
EXPORT struct S_PPI f19_S_SSF_PPI(struct S_PPI p0, struct S_PPI p1, float p2) { return p0; }
EXPORT struct S_PPF f19_S_SSF_PPF(struct S_PPF p0, struct S_PPF p1, float p2) { return p0; }
EXPORT struct S_PPD f19_S_SSF_PPD(struct S_PPD p0, struct S_PPD p1, float p2) { return p0; }
EXPORT struct S_PPP f19_S_SSF_PPP(struct S_PPP p0, struct S_PPP p1, float p2) { return p0; }
EXPORT struct S_I f19_S_SSD_I(struct S_I p0, struct S_I p1, double p2) { return p0; }
EXPORT struct S_F f19_S_SSD_F(struct S_F p0, struct S_F p1, double p2) { return p0; }
EXPORT struct S_D f19_S_SSD_D(struct S_D p0, struct S_D p1, double p2) { return p0; }
EXPORT struct S_P f19_S_SSD_P(struct S_P p0, struct S_P p1, double p2) { return p0; }
EXPORT struct S_II f19_S_SSD_II(struct S_II p0, struct S_II p1, double p2) { return p0; }
EXPORT struct S_IF f19_S_SSD_IF(struct S_IF p0, struct S_IF p1, double p2) { return p0; }
EXPORT struct S_ID f19_S_SSD_ID(struct S_ID p0, struct S_ID p1, double p2) { return p0; }
EXPORT struct S_IP f19_S_SSD_IP(struct S_IP p0, struct S_IP p1, double p2) { return p0; }
EXPORT struct S_FI f19_S_SSD_FI(struct S_FI p0, struct S_FI p1, double p2) { return p0; }
EXPORT struct S_FF f19_S_SSD_FF(struct S_FF p0, struct S_FF p1, double p2) { return p0; }
EXPORT struct S_FD f19_S_SSD_FD(struct S_FD p0, struct S_FD p1, double p2) { return p0; }
EXPORT struct S_FP f19_S_SSD_FP(struct S_FP p0, struct S_FP p1, double p2) { return p0; }
EXPORT struct S_DI f19_S_SSD_DI(struct S_DI p0, struct S_DI p1, double p2) { return p0; }
EXPORT struct S_DF f19_S_SSD_DF(struct S_DF p0, struct S_DF p1, double p2) { return p0; }
EXPORT struct S_DD f19_S_SSD_DD(struct S_DD p0, struct S_DD p1, double p2) { return p0; }
EXPORT struct S_DP f19_S_SSD_DP(struct S_DP p0, struct S_DP p1, double p2) { return p0; }
EXPORT struct S_PI f19_S_SSD_PI(struct S_PI p0, struct S_PI p1, double p2) { return p0; }
EXPORT struct S_PF f19_S_SSD_PF(struct S_PF p0, struct S_PF p1, double p2) { return p0; }
EXPORT struct S_PD f19_S_SSD_PD(struct S_PD p0, struct S_PD p1, double p2) { return p0; }
EXPORT struct S_PP f19_S_SSD_PP(struct S_PP p0, struct S_PP p1, double p2) { return p0; }
EXPORT struct S_III f19_S_SSD_III(struct S_III p0, struct S_III p1, double p2) { return p0; }
EXPORT struct S_IIF f19_S_SSD_IIF(struct S_IIF p0, struct S_IIF p1, double p2) { return p0; }
EXPORT struct S_IID f19_S_SSD_IID(struct S_IID p0, struct S_IID p1, double p2) { return p0; }
EXPORT struct S_IIP f19_S_SSD_IIP(struct S_IIP p0, struct S_IIP p1, double p2) { return p0; }
EXPORT struct S_IFI f19_S_SSD_IFI(struct S_IFI p0, struct S_IFI p1, double p2) { return p0; }
EXPORT struct S_IFF f19_S_SSD_IFF(struct S_IFF p0, struct S_IFF p1, double p2) { return p0; }
EXPORT struct S_IFD f19_S_SSD_IFD(struct S_IFD p0, struct S_IFD p1, double p2) { return p0; }
EXPORT struct S_IFP f19_S_SSD_IFP(struct S_IFP p0, struct S_IFP p1, double p2) { return p0; }
EXPORT struct S_IDI f19_S_SSD_IDI(struct S_IDI p0, struct S_IDI p1, double p2) { return p0; }
EXPORT struct S_IDF f19_S_SSD_IDF(struct S_IDF p0, struct S_IDF p1, double p2) { return p0; }
EXPORT struct S_IDD f19_S_SSD_IDD(struct S_IDD p0, struct S_IDD p1, double p2) { return p0; }
EXPORT struct S_IDP f19_S_SSD_IDP(struct S_IDP p0, struct S_IDP p1, double p2) { return p0; }
EXPORT struct S_IPI f19_S_SSD_IPI(struct S_IPI p0, struct S_IPI p1, double p2) { return p0; }
EXPORT struct S_IPF f19_S_SSD_IPF(struct S_IPF p0, struct S_IPF p1, double p2) { return p0; }
EXPORT struct S_IPD f19_S_SSD_IPD(struct S_IPD p0, struct S_IPD p1, double p2) { return p0; }
EXPORT struct S_IPP f19_S_SSD_IPP(struct S_IPP p0, struct S_IPP p1, double p2) { return p0; }
EXPORT struct S_FII f19_S_SSD_FII(struct S_FII p0, struct S_FII p1, double p2) { return p0; }
EXPORT struct S_FIF f19_S_SSD_FIF(struct S_FIF p0, struct S_FIF p1, double p2) { return p0; }
EXPORT struct S_FID f19_S_SSD_FID(struct S_FID p0, struct S_FID p1, double p2) { return p0; }
EXPORT struct S_FIP f19_S_SSD_FIP(struct S_FIP p0, struct S_FIP p1, double p2) { return p0; }
EXPORT struct S_FFI f19_S_SSD_FFI(struct S_FFI p0, struct S_FFI p1, double p2) { return p0; }
EXPORT struct S_FFF f19_S_SSD_FFF(struct S_FFF p0, struct S_FFF p1, double p2) { return p0; }
EXPORT struct S_FFD f19_S_SSD_FFD(struct S_FFD p0, struct S_FFD p1, double p2) { return p0; }
EXPORT struct S_FFP f19_S_SSD_FFP(struct S_FFP p0, struct S_FFP p1, double p2) { return p0; }
EXPORT struct S_FDI f19_S_SSD_FDI(struct S_FDI p0, struct S_FDI p1, double p2) { return p0; }
EXPORT struct S_FDF f19_S_SSD_FDF(struct S_FDF p0, struct S_FDF p1, double p2) { return p0; }
EXPORT struct S_FDD f19_S_SSD_FDD(struct S_FDD p0, struct S_FDD p1, double p2) { return p0; }
EXPORT struct S_FDP f19_S_SSD_FDP(struct S_FDP p0, struct S_FDP p1, double p2) { return p0; }
EXPORT struct S_FPI f19_S_SSD_FPI(struct S_FPI p0, struct S_FPI p1, double p2) { return p0; }
EXPORT struct S_FPF f19_S_SSD_FPF(struct S_FPF p0, struct S_FPF p1, double p2) { return p0; }
EXPORT struct S_FPD f19_S_SSD_FPD(struct S_FPD p0, struct S_FPD p1, double p2) { return p0; }
EXPORT struct S_FPP f19_S_SSD_FPP(struct S_FPP p0, struct S_FPP p1, double p2) { return p0; }
EXPORT struct S_DII f19_S_SSD_DII(struct S_DII p0, struct S_DII p1, double p2) { return p0; }
EXPORT struct S_DIF f19_S_SSD_DIF(struct S_DIF p0, struct S_DIF p1, double p2) { return p0; }
EXPORT struct S_DID f19_S_SSD_DID(struct S_DID p0, struct S_DID p1, double p2) { return p0; }
EXPORT struct S_DIP f19_S_SSD_DIP(struct S_DIP p0, struct S_DIP p1, double p2) { return p0; }
EXPORT struct S_DFI f19_S_SSD_DFI(struct S_DFI p0, struct S_DFI p1, double p2) { return p0; }
EXPORT struct S_DFF f19_S_SSD_DFF(struct S_DFF p0, struct S_DFF p1, double p2) { return p0; }
EXPORT struct S_DFD f19_S_SSD_DFD(struct S_DFD p0, struct S_DFD p1, double p2) { return p0; }
EXPORT struct S_DFP f19_S_SSD_DFP(struct S_DFP p0, struct S_DFP p1, double p2) { return p0; }
EXPORT struct S_DDI f19_S_SSD_DDI(struct S_DDI p0, struct S_DDI p1, double p2) { return p0; }
EXPORT struct S_DDF f19_S_SSD_DDF(struct S_DDF p0, struct S_DDF p1, double p2) { return p0; }
EXPORT struct S_DDD f19_S_SSD_DDD(struct S_DDD p0, struct S_DDD p1, double p2) { return p0; }
EXPORT struct S_DDP f19_S_SSD_DDP(struct S_DDP p0, struct S_DDP p1, double p2) { return p0; }
EXPORT struct S_DPI f19_S_SSD_DPI(struct S_DPI p0, struct S_DPI p1, double p2) { return p0; }
EXPORT struct S_DPF f19_S_SSD_DPF(struct S_DPF p0, struct S_DPF p1, double p2) { return p0; }
EXPORT struct S_DPD f19_S_SSD_DPD(struct S_DPD p0, struct S_DPD p1, double p2) { return p0; }
EXPORT struct S_DPP f19_S_SSD_DPP(struct S_DPP p0, struct S_DPP p1, double p2) { return p0; }
EXPORT struct S_PII f19_S_SSD_PII(struct S_PII p0, struct S_PII p1, double p2) { return p0; }
EXPORT struct S_PIF f19_S_SSD_PIF(struct S_PIF p0, struct S_PIF p1, double p2) { return p0; }
EXPORT struct S_PID f19_S_SSD_PID(struct S_PID p0, struct S_PID p1, double p2) { return p0; }
EXPORT struct S_PIP f19_S_SSD_PIP(struct S_PIP p0, struct S_PIP p1, double p2) { return p0; }
EXPORT struct S_PFI f19_S_SSD_PFI(struct S_PFI p0, struct S_PFI p1, double p2) { return p0; }
EXPORT struct S_PFF f19_S_SSD_PFF(struct S_PFF p0, struct S_PFF p1, double p2) { return p0; }
EXPORT struct S_PFD f19_S_SSD_PFD(struct S_PFD p0, struct S_PFD p1, double p2) { return p0; }
EXPORT struct S_PFP f19_S_SSD_PFP(struct S_PFP p0, struct S_PFP p1, double p2) { return p0; }
EXPORT struct S_PDI f19_S_SSD_PDI(struct S_PDI p0, struct S_PDI p1, double p2) { return p0; }
EXPORT struct S_PDF f19_S_SSD_PDF(struct S_PDF p0, struct S_PDF p1, double p2) { return p0; }
EXPORT struct S_PDD f19_S_SSD_PDD(struct S_PDD p0, struct S_PDD p1, double p2) { return p0; }
EXPORT struct S_PDP f19_S_SSD_PDP(struct S_PDP p0, struct S_PDP p1, double p2) { return p0; }
EXPORT struct S_PPI f19_S_SSD_PPI(struct S_PPI p0, struct S_PPI p1, double p2) { return p0; }
EXPORT struct S_PPF f19_S_SSD_PPF(struct S_PPF p0, struct S_PPF p1, double p2) { return p0; }
EXPORT struct S_PPD f19_S_SSD_PPD(struct S_PPD p0, struct S_PPD p1, double p2) { return p0; }
EXPORT struct S_PPP f19_S_SSD_PPP(struct S_PPP p0, struct S_PPP p1, double p2) { return p0; }
EXPORT struct S_I f19_S_SSP_I(struct S_I p0, struct S_I p1, void* p2) { return p0; }
EXPORT struct S_F f19_S_SSP_F(struct S_F p0, struct S_F p1, void* p2) { return p0; }
EXPORT struct S_D f19_S_SSP_D(struct S_D p0, struct S_D p1, void* p2) { return p0; }
EXPORT struct S_P f19_S_SSP_P(struct S_P p0, struct S_P p1, void* p2) { return p0; }
EXPORT struct S_II f19_S_SSP_II(struct S_II p0, struct S_II p1, void* p2) { return p0; }
EXPORT struct S_IF f19_S_SSP_IF(struct S_IF p0, struct S_IF p1, void* p2) { return p0; }
EXPORT struct S_ID f19_S_SSP_ID(struct S_ID p0, struct S_ID p1, void* p2) { return p0; }
EXPORT struct S_IP f19_S_SSP_IP(struct S_IP p0, struct S_IP p1, void* p2) { return p0; }
EXPORT struct S_FI f19_S_SSP_FI(struct S_FI p0, struct S_FI p1, void* p2) { return p0; }
EXPORT struct S_FF f19_S_SSP_FF(struct S_FF p0, struct S_FF p1, void* p2) { return p0; }
EXPORT struct S_FD f19_S_SSP_FD(struct S_FD p0, struct S_FD p1, void* p2) { return p0; }
EXPORT struct S_FP f19_S_SSP_FP(struct S_FP p0, struct S_FP p1, void* p2) { return p0; }
EXPORT struct S_DI f19_S_SSP_DI(struct S_DI p0, struct S_DI p1, void* p2) { return p0; }
EXPORT struct S_DF f19_S_SSP_DF(struct S_DF p0, struct S_DF p1, void* p2) { return p0; }
EXPORT struct S_DD f19_S_SSP_DD(struct S_DD p0, struct S_DD p1, void* p2) { return p0; }
EXPORT struct S_DP f19_S_SSP_DP(struct S_DP p0, struct S_DP p1, void* p2) { return p0; }
EXPORT struct S_PI f19_S_SSP_PI(struct S_PI p0, struct S_PI p1, void* p2) { return p0; }
EXPORT struct S_PF f19_S_SSP_PF(struct S_PF p0, struct S_PF p1, void* p2) { return p0; }
EXPORT struct S_PD f19_S_SSP_PD(struct S_PD p0, struct S_PD p1, void* p2) { return p0; }
EXPORT struct S_PP f19_S_SSP_PP(struct S_PP p0, struct S_PP p1, void* p2) { return p0; }
EXPORT struct S_III f19_S_SSP_III(struct S_III p0, struct S_III p1, void* p2) { return p0; }
EXPORT struct S_IIF f19_S_SSP_IIF(struct S_IIF p0, struct S_IIF p1, void* p2) { return p0; }
EXPORT struct S_IID f19_S_SSP_IID(struct S_IID p0, struct S_IID p1, void* p2) { return p0; }
EXPORT struct S_IIP f19_S_SSP_IIP(struct S_IIP p0, struct S_IIP p1, void* p2) { return p0; }
EXPORT struct S_IFI f19_S_SSP_IFI(struct S_IFI p0, struct S_IFI p1, void* p2) { return p0; }
EXPORT struct S_IFF f19_S_SSP_IFF(struct S_IFF p0, struct S_IFF p1, void* p2) { return p0; }
EXPORT struct S_IFD f19_S_SSP_IFD(struct S_IFD p0, struct S_IFD p1, void* p2) { return p0; }
EXPORT struct S_IFP f19_S_SSP_IFP(struct S_IFP p0, struct S_IFP p1, void* p2) { return p0; }
EXPORT struct S_IDI f19_S_SSP_IDI(struct S_IDI p0, struct S_IDI p1, void* p2) { return p0; }
EXPORT struct S_IDF f19_S_SSP_IDF(struct S_IDF p0, struct S_IDF p1, void* p2) { return p0; }
EXPORT struct S_IDD f19_S_SSP_IDD(struct S_IDD p0, struct S_IDD p1, void* p2) { return p0; }
EXPORT struct S_IDP f19_S_SSP_IDP(struct S_IDP p0, struct S_IDP p1, void* p2) { return p0; }
EXPORT struct S_IPI f19_S_SSP_IPI(struct S_IPI p0, struct S_IPI p1, void* p2) { return p0; }
EXPORT struct S_IPF f19_S_SSP_IPF(struct S_IPF p0, struct S_IPF p1, void* p2) { return p0; }
EXPORT struct S_IPD f19_S_SSP_IPD(struct S_IPD p0, struct S_IPD p1, void* p2) { return p0; }
EXPORT struct S_IPP f19_S_SSP_IPP(struct S_IPP p0, struct S_IPP p1, void* p2) { return p0; }
EXPORT struct S_FII f19_S_SSP_FII(struct S_FII p0, struct S_FII p1, void* p2) { return p0; }
EXPORT struct S_FIF f19_S_SSP_FIF(struct S_FIF p0, struct S_FIF p1, void* p2) { return p0; }
EXPORT struct S_FID f19_S_SSP_FID(struct S_FID p0, struct S_FID p1, void* p2) { return p0; }
EXPORT struct S_FIP f19_S_SSP_FIP(struct S_FIP p0, struct S_FIP p1, void* p2) { return p0; }
EXPORT struct S_FFI f19_S_SSP_FFI(struct S_FFI p0, struct S_FFI p1, void* p2) { return p0; }
EXPORT struct S_FFF f19_S_SSP_FFF(struct S_FFF p0, struct S_FFF p1, void* p2) { return p0; }
EXPORT struct S_FFD f19_S_SSP_FFD(struct S_FFD p0, struct S_FFD p1, void* p2) { return p0; }
EXPORT struct S_FFP f19_S_SSP_FFP(struct S_FFP p0, struct S_FFP p1, void* p2) { return p0; }
EXPORT struct S_FDI f19_S_SSP_FDI(struct S_FDI p0, struct S_FDI p1, void* p2) { return p0; }
EXPORT struct S_FDF f19_S_SSP_FDF(struct S_FDF p0, struct S_FDF p1, void* p2) { return p0; }
EXPORT struct S_FDD f19_S_SSP_FDD(struct S_FDD p0, struct S_FDD p1, void* p2) { return p0; }
EXPORT struct S_FDP f19_S_SSP_FDP(struct S_FDP p0, struct S_FDP p1, void* p2) { return p0; }
EXPORT struct S_FPI f19_S_SSP_FPI(struct S_FPI p0, struct S_FPI p1, void* p2) { return p0; }
EXPORT struct S_FPF f19_S_SSP_FPF(struct S_FPF p0, struct S_FPF p1, void* p2) { return p0; }
EXPORT struct S_FPD f19_S_SSP_FPD(struct S_FPD p0, struct S_FPD p1, void* p2) { return p0; }
EXPORT struct S_FPP f19_S_SSP_FPP(struct S_FPP p0, struct S_FPP p1, void* p2) { return p0; }
EXPORT struct S_DII f19_S_SSP_DII(struct S_DII p0, struct S_DII p1, void* p2) { return p0; }
EXPORT struct S_DIF f19_S_SSP_DIF(struct S_DIF p0, struct S_DIF p1, void* p2) { return p0; }
EXPORT struct S_DID f19_S_SSP_DID(struct S_DID p0, struct S_DID p1, void* p2) { return p0; }
EXPORT struct S_DIP f19_S_SSP_DIP(struct S_DIP p0, struct S_DIP p1, void* p2) { return p0; }
EXPORT struct S_DFI f19_S_SSP_DFI(struct S_DFI p0, struct S_DFI p1, void* p2) { return p0; }
EXPORT struct S_DFF f19_S_SSP_DFF(struct S_DFF p0, struct S_DFF p1, void* p2) { return p0; }
EXPORT struct S_DFD f19_S_SSP_DFD(struct S_DFD p0, struct S_DFD p1, void* p2) { return p0; }
EXPORT struct S_DFP f19_S_SSP_DFP(struct S_DFP p0, struct S_DFP p1, void* p2) { return p0; }
EXPORT struct S_DDI f19_S_SSP_DDI(struct S_DDI p0, struct S_DDI p1, void* p2) { return p0; }
EXPORT struct S_DDF f19_S_SSP_DDF(struct S_DDF p0, struct S_DDF p1, void* p2) { return p0; }
EXPORT struct S_DDD f19_S_SSP_DDD(struct S_DDD p0, struct S_DDD p1, void* p2) { return p0; }
EXPORT struct S_DDP f19_S_SSP_DDP(struct S_DDP p0, struct S_DDP p1, void* p2) { return p0; }
EXPORT struct S_DPI f19_S_SSP_DPI(struct S_DPI p0, struct S_DPI p1, void* p2) { return p0; }
EXPORT struct S_DPF f19_S_SSP_DPF(struct S_DPF p0, struct S_DPF p1, void* p2) { return p0; }
EXPORT struct S_DPD f19_S_SSP_DPD(struct S_DPD p0, struct S_DPD p1, void* p2) { return p0; }
EXPORT struct S_DPP f19_S_SSP_DPP(struct S_DPP p0, struct S_DPP p1, void* p2) { return p0; }
EXPORT struct S_PII f19_S_SSP_PII(struct S_PII p0, struct S_PII p1, void* p2) { return p0; }
EXPORT struct S_PIF f19_S_SSP_PIF(struct S_PIF p0, struct S_PIF p1, void* p2) { return p0; }
EXPORT struct S_PID f19_S_SSP_PID(struct S_PID p0, struct S_PID p1, void* p2) { return p0; }
EXPORT struct S_PIP f20_S_SSP_PIP(struct S_PIP p0, struct S_PIP p1, void* p2) { return p0; }
EXPORT struct S_PFI f20_S_SSP_PFI(struct S_PFI p0, struct S_PFI p1, void* p2) { return p0; }
EXPORT struct S_PFF f20_S_SSP_PFF(struct S_PFF p0, struct S_PFF p1, void* p2) { return p0; }
EXPORT struct S_PFD f20_S_SSP_PFD(struct S_PFD p0, struct S_PFD p1, void* p2) { return p0; }
EXPORT struct S_PFP f20_S_SSP_PFP(struct S_PFP p0, struct S_PFP p1, void* p2) { return p0; }
EXPORT struct S_PDI f20_S_SSP_PDI(struct S_PDI p0, struct S_PDI p1, void* p2) { return p0; }
EXPORT struct S_PDF f20_S_SSP_PDF(struct S_PDF p0, struct S_PDF p1, void* p2) { return p0; }
EXPORT struct S_PDD f20_S_SSP_PDD(struct S_PDD p0, struct S_PDD p1, void* p2) { return p0; }
EXPORT struct S_PDP f20_S_SSP_PDP(struct S_PDP p0, struct S_PDP p1, void* p2) { return p0; }
EXPORT struct S_PPI f20_S_SSP_PPI(struct S_PPI p0, struct S_PPI p1, void* p2) { return p0; }
EXPORT struct S_PPF f20_S_SSP_PPF(struct S_PPF p0, struct S_PPF p1, void* p2) { return p0; }
EXPORT struct S_PPD f20_S_SSP_PPD(struct S_PPD p0, struct S_PPD p1, void* p2) { return p0; }
EXPORT struct S_PPP f20_S_SSP_PPP(struct S_PPP p0, struct S_PPP p1, void* p2) { return p0; }
EXPORT struct S_I f20_S_SSS_I(struct S_I p0, struct S_I p1, struct S_I p2) { return p0; }
EXPORT struct S_F f20_S_SSS_F(struct S_F p0, struct S_F p1, struct S_F p2) { return p0; }
EXPORT struct S_D f20_S_SSS_D(struct S_D p0, struct S_D p1, struct S_D p2) { return p0; }
EXPORT struct S_P f20_S_SSS_P(struct S_P p0, struct S_P p1, struct S_P p2) { return p0; }
EXPORT struct S_II f20_S_SSS_II(struct S_II p0, struct S_II p1, struct S_II p2) { return p0; }
EXPORT struct S_IF f20_S_SSS_IF(struct S_IF p0, struct S_IF p1, struct S_IF p2) { return p0; }
EXPORT struct S_ID f20_S_SSS_ID(struct S_ID p0, struct S_ID p1, struct S_ID p2) { return p0; }
EXPORT struct S_IP f20_S_SSS_IP(struct S_IP p0, struct S_IP p1, struct S_IP p2) { return p0; }
EXPORT struct S_FI f20_S_SSS_FI(struct S_FI p0, struct S_FI p1, struct S_FI p2) { return p0; }
EXPORT struct S_FF f20_S_SSS_FF(struct S_FF p0, struct S_FF p1, struct S_FF p2) { return p0; }
EXPORT struct S_FD f20_S_SSS_FD(struct S_FD p0, struct S_FD p1, struct S_FD p2) { return p0; }
EXPORT struct S_FP f20_S_SSS_FP(struct S_FP p0, struct S_FP p1, struct S_FP p2) { return p0; }
EXPORT struct S_DI f20_S_SSS_DI(struct S_DI p0, struct S_DI p1, struct S_DI p2) { return p0; }
EXPORT struct S_DF f20_S_SSS_DF(struct S_DF p0, struct S_DF p1, struct S_DF p2) { return p0; }
EXPORT struct S_DD f20_S_SSS_DD(struct S_DD p0, struct S_DD p1, struct S_DD p2) { return p0; }
EXPORT struct S_DP f20_S_SSS_DP(struct S_DP p0, struct S_DP p1, struct S_DP p2) { return p0; }
EXPORT struct S_PI f20_S_SSS_PI(struct S_PI p0, struct S_PI p1, struct S_PI p2) { return p0; }
EXPORT struct S_PF f20_S_SSS_PF(struct S_PF p0, struct S_PF p1, struct S_PF p2) { return p0; }
EXPORT struct S_PD f20_S_SSS_PD(struct S_PD p0, struct S_PD p1, struct S_PD p2) { return p0; }
EXPORT struct S_PP f20_S_SSS_PP(struct S_PP p0, struct S_PP p1, struct S_PP p2) { return p0; }
EXPORT struct S_III f20_S_SSS_III(struct S_III p0, struct S_III p1, struct S_III p2) { return p0; }
EXPORT struct S_IIF f20_S_SSS_IIF(struct S_IIF p0, struct S_IIF p1, struct S_IIF p2) { return p0; }
EXPORT struct S_IID f20_S_SSS_IID(struct S_IID p0, struct S_IID p1, struct S_IID p2) { return p0; }
EXPORT struct S_IIP f20_S_SSS_IIP(struct S_IIP p0, struct S_IIP p1, struct S_IIP p2) { return p0; }
EXPORT struct S_IFI f20_S_SSS_IFI(struct S_IFI p0, struct S_IFI p1, struct S_IFI p2) { return p0; }
EXPORT struct S_IFF f20_S_SSS_IFF(struct S_IFF p0, struct S_IFF p1, struct S_IFF p2) { return p0; }
EXPORT struct S_IFD f20_S_SSS_IFD(struct S_IFD p0, struct S_IFD p1, struct S_IFD p2) { return p0; }
EXPORT struct S_IFP f20_S_SSS_IFP(struct S_IFP p0, struct S_IFP p1, struct S_IFP p2) { return p0; }
EXPORT struct S_IDI f20_S_SSS_IDI(struct S_IDI p0, struct S_IDI p1, struct S_IDI p2) { return p0; }
EXPORT struct S_IDF f20_S_SSS_IDF(struct S_IDF p0, struct S_IDF p1, struct S_IDF p2) { return p0; }
EXPORT struct S_IDD f20_S_SSS_IDD(struct S_IDD p0, struct S_IDD p1, struct S_IDD p2) { return p0; }
EXPORT struct S_IDP f20_S_SSS_IDP(struct S_IDP p0, struct S_IDP p1, struct S_IDP p2) { return p0; }
EXPORT struct S_IPI f20_S_SSS_IPI(struct S_IPI p0, struct S_IPI p1, struct S_IPI p2) { return p0; }
EXPORT struct S_IPF f20_S_SSS_IPF(struct S_IPF p0, struct S_IPF p1, struct S_IPF p2) { return p0; }
EXPORT struct S_IPD f20_S_SSS_IPD(struct S_IPD p0, struct S_IPD p1, struct S_IPD p2) { return p0; }
EXPORT struct S_IPP f20_S_SSS_IPP(struct S_IPP p0, struct S_IPP p1, struct S_IPP p2) { return p0; }
EXPORT struct S_FII f20_S_SSS_FII(struct S_FII p0, struct S_FII p1, struct S_FII p2) { return p0; }
EXPORT struct S_FIF f20_S_SSS_FIF(struct S_FIF p0, struct S_FIF p1, struct S_FIF p2) { return p0; }
EXPORT struct S_FID f20_S_SSS_FID(struct S_FID p0, struct S_FID p1, struct S_FID p2) { return p0; }
EXPORT struct S_FIP f20_S_SSS_FIP(struct S_FIP p0, struct S_FIP p1, struct S_FIP p2) { return p0; }
EXPORT struct S_FFI f20_S_SSS_FFI(struct S_FFI p0, struct S_FFI p1, struct S_FFI p2) { return p0; }
EXPORT struct S_FFF f20_S_SSS_FFF(struct S_FFF p0, struct S_FFF p1, struct S_FFF p2) { return p0; }
EXPORT struct S_FFD f20_S_SSS_FFD(struct S_FFD p0, struct S_FFD p1, struct S_FFD p2) { return p0; }
EXPORT struct S_FFP f20_S_SSS_FFP(struct S_FFP p0, struct S_FFP p1, struct S_FFP p2) { return p0; }
EXPORT struct S_FDI f20_S_SSS_FDI(struct S_FDI p0, struct S_FDI p1, struct S_FDI p2) { return p0; }
EXPORT struct S_FDF f20_S_SSS_FDF(struct S_FDF p0, struct S_FDF p1, struct S_FDF p2) { return p0; }
EXPORT struct S_FDD f20_S_SSS_FDD(struct S_FDD p0, struct S_FDD p1, struct S_FDD p2) { return p0; }
EXPORT struct S_FDP f20_S_SSS_FDP(struct S_FDP p0, struct S_FDP p1, struct S_FDP p2) { return p0; }
EXPORT struct S_FPI f20_S_SSS_FPI(struct S_FPI p0, struct S_FPI p1, struct S_FPI p2) { return p0; }
EXPORT struct S_FPF f20_S_SSS_FPF(struct S_FPF p0, struct S_FPF p1, struct S_FPF p2) { return p0; }
EXPORT struct S_FPD f20_S_SSS_FPD(struct S_FPD p0, struct S_FPD p1, struct S_FPD p2) { return p0; }
EXPORT struct S_FPP f20_S_SSS_FPP(struct S_FPP p0, struct S_FPP p1, struct S_FPP p2) { return p0; }
EXPORT struct S_DII f20_S_SSS_DII(struct S_DII p0, struct S_DII p1, struct S_DII p2) { return p0; }
EXPORT struct S_DIF f20_S_SSS_DIF(struct S_DIF p0, struct S_DIF p1, struct S_DIF p2) { return p0; }
EXPORT struct S_DID f20_S_SSS_DID(struct S_DID p0, struct S_DID p1, struct S_DID p2) { return p0; }
EXPORT struct S_DIP f20_S_SSS_DIP(struct S_DIP p0, struct S_DIP p1, struct S_DIP p2) { return p0; }
EXPORT struct S_DFI f20_S_SSS_DFI(struct S_DFI p0, struct S_DFI p1, struct S_DFI p2) { return p0; }
EXPORT struct S_DFF f20_S_SSS_DFF(struct S_DFF p0, struct S_DFF p1, struct S_DFF p2) { return p0; }
EXPORT struct S_DFD f20_S_SSS_DFD(struct S_DFD p0, struct S_DFD p1, struct S_DFD p2) { return p0; }
EXPORT struct S_DFP f20_S_SSS_DFP(struct S_DFP p0, struct S_DFP p1, struct S_DFP p2) { return p0; }
EXPORT struct S_DDI f20_S_SSS_DDI(struct S_DDI p0, struct S_DDI p1, struct S_DDI p2) { return p0; }
EXPORT struct S_DDF f20_S_SSS_DDF(struct S_DDF p0, struct S_DDF p1, struct S_DDF p2) { return p0; }
EXPORT struct S_DDD f20_S_SSS_DDD(struct S_DDD p0, struct S_DDD p1, struct S_DDD p2) { return p0; }
EXPORT struct S_DDP f20_S_SSS_DDP(struct S_DDP p0, struct S_DDP p1, struct S_DDP p2) { return p0; }
EXPORT struct S_DPI f20_S_SSS_DPI(struct S_DPI p0, struct S_DPI p1, struct S_DPI p2) { return p0; }
EXPORT struct S_DPF f20_S_SSS_DPF(struct S_DPF p0, struct S_DPF p1, struct S_DPF p2) { return p0; }
EXPORT struct S_DPD f20_S_SSS_DPD(struct S_DPD p0, struct S_DPD p1, struct S_DPD p2) { return p0; }
EXPORT struct S_DPP f20_S_SSS_DPP(struct S_DPP p0, struct S_DPP p1, struct S_DPP p2) { return p0; }
EXPORT struct S_PII f20_S_SSS_PII(struct S_PII p0, struct S_PII p1, struct S_PII p2) { return p0; }
EXPORT struct S_PIF f20_S_SSS_PIF(struct S_PIF p0, struct S_PIF p1, struct S_PIF p2) { return p0; }
EXPORT struct S_PID f20_S_SSS_PID(struct S_PID p0, struct S_PID p1, struct S_PID p2) { return p0; }
EXPORT struct S_PIP f20_S_SSS_PIP(struct S_PIP p0, struct S_PIP p1, struct S_PIP p2) { return p0; }
EXPORT struct S_PFI f20_S_SSS_PFI(struct S_PFI p0, struct S_PFI p1, struct S_PFI p2) { return p0; }
EXPORT struct S_PFF f20_S_SSS_PFF(struct S_PFF p0, struct S_PFF p1, struct S_PFF p2) { return p0; }
EXPORT struct S_PFD f20_S_SSS_PFD(struct S_PFD p0, struct S_PFD p1, struct S_PFD p2) { return p0; }
EXPORT struct S_PFP f20_S_SSS_PFP(struct S_PFP p0, struct S_PFP p1, struct S_PFP p2) { return p0; }
EXPORT struct S_PDI f20_S_SSS_PDI(struct S_PDI p0, struct S_PDI p1, struct S_PDI p2) { return p0; }
EXPORT struct S_PDF f20_S_SSS_PDF(struct S_PDF p0, struct S_PDF p1, struct S_PDF p2) { return p0; }
EXPORT struct S_PDD f20_S_SSS_PDD(struct S_PDD p0, struct S_PDD p1, struct S_PDD p2) { return p0; }
EXPORT struct S_PDP f20_S_SSS_PDP(struct S_PDP p0, struct S_PDP p1, struct S_PDP p2) { return p0; }
EXPORT struct S_PPI f20_S_SSS_PPI(struct S_PPI p0, struct S_PPI p1, struct S_PPI p2) { return p0; }
EXPORT struct S_PPF f20_S_SSS_PPF(struct S_PPF p0, struct S_PPF p1, struct S_PPF p2) { return p0; }
EXPORT struct S_PPD f20_S_SSS_PPD(struct S_PPD p0, struct S_PPD p1, struct S_PPD p2) { return p0; }
EXPORT struct S_PPP f20_S_SSS_PPP(struct S_PPP p0, struct S_PPP p1, struct S_PPP p2) { return p0; }
