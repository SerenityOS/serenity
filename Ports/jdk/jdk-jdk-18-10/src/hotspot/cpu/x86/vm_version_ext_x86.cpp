/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

#include "precompiled.hpp"
#include "jvm.h"
#include "utilities/macros.hpp"
#include "asm/macroAssembler.hpp"
#include "asm/macroAssembler.inline.hpp"
#include "code/codeBlob.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/java.hpp"
#include "runtime/stubCodeGenerator.hpp"
#include "vm_version_ext_x86.hpp"

typedef enum {
   CPU_FAMILY_8086_8088  = 0,
   CPU_FAMILY_INTEL_286  = 2,
   CPU_FAMILY_INTEL_386  = 3,
   CPU_FAMILY_INTEL_486  = 4,
   CPU_FAMILY_PENTIUM    = 5,
   CPU_FAMILY_PENTIUMPRO = 6,    // Same family several models
   CPU_FAMILY_PENTIUM_4  = 0xF
} FamilyFlag;

typedef enum {
  RDTSCP_FLAG  = 0x08000000, // bit 27
  INTEL64_FLAG = 0x20000000  // bit 29
} _featureExtendedEdxFlag;

#define CPUID_STANDARD_FN   0x0
#define CPUID_STANDARD_FN_1 0x1
#define CPUID_STANDARD_FN_4 0x4
#define CPUID_STANDARD_FN_B 0xb

#define CPUID_EXTENDED_FN   0x80000000
#define CPUID_EXTENDED_FN_1 0x80000001
#define CPUID_EXTENDED_FN_2 0x80000002
#define CPUID_EXTENDED_FN_3 0x80000003
#define CPUID_EXTENDED_FN_4 0x80000004
#define CPUID_EXTENDED_FN_7 0x80000007
#define CPUID_EXTENDED_FN_8 0x80000008

typedef enum {
   FPU_FLAG     = 0x00000001,
   VME_FLAG     = 0x00000002,
   DE_FLAG      = 0x00000004,
   PSE_FLAG     = 0x00000008,
   TSC_FLAG     = 0x00000010,
   MSR_FLAG     = 0x00000020,
   PAE_FLAG     = 0x00000040,
   MCE_FLAG     = 0x00000080,
   CX8_FLAG     = 0x00000100,
   APIC_FLAG    = 0x00000200,
   SEP_FLAG     = 0x00000800,
   MTRR_FLAG    = 0x00001000,
   PGE_FLAG     = 0x00002000,
   MCA_FLAG     = 0x00004000,
   CMOV_FLAG    = 0x00008000,
   PAT_FLAG     = 0x00010000,
   PSE36_FLAG   = 0x00020000,
   PSNUM_FLAG   = 0x00040000,
   CLFLUSH_FLAG = 0x00080000,
   DTS_FLAG     = 0x00200000,
   ACPI_FLAG    = 0x00400000,
   MMX_FLAG     = 0x00800000,
   FXSR_FLAG    = 0x01000000,
   SSE_FLAG     = 0x02000000,
   SSE2_FLAG    = 0x04000000,
   SS_FLAG      = 0x08000000,
   HTT_FLAG     = 0x10000000,
   TM_FLAG      = 0x20000000
} FeatureEdxFlag;

static BufferBlob* cpuid_brand_string_stub_blob;
static const int   cpuid_brand_string_stub_size = 550;

extern "C" {
  typedef void (*getCPUIDBrandString_stub_t)(void*);
}

static getCPUIDBrandString_stub_t getCPUIDBrandString_stub = NULL;

class VM_Version_Ext_StubGenerator: public StubCodeGenerator {
 public:

  VM_Version_Ext_StubGenerator(CodeBuffer *c) : StubCodeGenerator(c) {}

  address generate_getCPUIDBrandString(void) {
    // Flags to test CPU type.
    const uint32_t HS_EFL_AC           = 0x40000;
    const uint32_t HS_EFL_ID           = 0x200000;
    // Values for when we don't have a CPUID instruction.
    const int      CPU_FAMILY_SHIFT = 8;
    const uint32_t CPU_FAMILY_386   = (3 << CPU_FAMILY_SHIFT);
    const uint32_t CPU_FAMILY_486   = (4 << CPU_FAMILY_SHIFT);

    Label detect_486, cpu486, detect_586, done, ext_cpuid;

    StubCodeMark mark(this, "VM_Version_Ext", "getCPUIDNameInfo_stub");
#   define __ _masm->

    address start = __ pc();

    //
    // void getCPUIDBrandString(VM_Version::CpuidInfo* cpuid_info);
    //
    // LP64: rcx and rdx are first and second argument registers on windows

    __ push(rbp);
#ifdef _LP64
    __ mov(rbp, c_rarg0); // cpuid_info address
#else
    __ movptr(rbp, Address(rsp, 8)); // cpuid_info address
#endif
    __ push(rbx);
    __ push(rsi);
    __ pushf();          // preserve rbx, and flags
    __ pop(rax);
    __ push(rax);
    __ mov(rcx, rax);
    //
    // if we are unable to change the AC flag, we have a 386
    //
    __ xorl(rax, HS_EFL_AC);
    __ push(rax);
    __ popf();
    __ pushf();
    __ pop(rax);
    __ cmpptr(rax, rcx);
    __ jccb(Assembler::notEqual, detect_486);

    __ movl(rax, CPU_FAMILY_386);
    __ jmp(done);

    //
    // If we are unable to change the ID flag, we have a 486 which does
    // not support the "cpuid" instruction.
    //
    __ bind(detect_486);
    __ mov(rax, rcx);
    __ xorl(rax, HS_EFL_ID);
    __ push(rax);
    __ popf();
    __ pushf();
    __ pop(rax);
    __ cmpptr(rcx, rax);
    __ jccb(Assembler::notEqual, detect_586);

    __ bind(cpu486);
    __ movl(rax, CPU_FAMILY_486);
    __ jmp(done);

    //
    // At this point, we have a chip which supports the "cpuid" instruction
    //
    __ bind(detect_586);
    __ xorl(rax, rax);
    __ cpuid();
    __ orl(rax, rax);
    __ jcc(Assembler::equal, cpu486);   // if cpuid doesn't support an input
                                        // value of at least 1, we give up and
                                        // assume a 486

    //
    // Extended cpuid(0x80000000) for processor brand string detection
    //
    __ bind(ext_cpuid);
    __ movl(rax, CPUID_EXTENDED_FN);
    __ cpuid();
    __ cmpl(rax, CPUID_EXTENDED_FN_4);
    __ jcc(Assembler::below, done);

    //
    // Extended cpuid(0x80000002)  // first 16 bytes in brand string
    //
    __ movl(rax, CPUID_EXTENDED_FN_2);
    __ cpuid();
    __ lea(rsi, Address(rbp, in_bytes(VM_Version_Ext::proc_name_0_offset())));
    __ movl(Address(rsi, 0), rax);
    __ lea(rsi, Address(rbp, in_bytes(VM_Version_Ext::proc_name_1_offset())));
    __ movl(Address(rsi, 0), rbx);
    __ lea(rsi, Address(rbp, in_bytes(VM_Version_Ext::proc_name_2_offset())));
    __ movl(Address(rsi, 0), rcx);
    __ lea(rsi, Address(rbp, in_bytes(VM_Version_Ext::proc_name_3_offset())));
    __ movl(Address(rsi,0), rdx);

    //
    // Extended cpuid(0x80000003) // next 16 bytes in brand string
    //
    __ movl(rax, CPUID_EXTENDED_FN_3);
    __ cpuid();
    __ lea(rsi, Address(rbp, in_bytes(VM_Version_Ext::proc_name_4_offset())));
    __ movl(Address(rsi, 0), rax);
    __ lea(rsi, Address(rbp, in_bytes(VM_Version_Ext::proc_name_5_offset())));
    __ movl(Address(rsi, 0), rbx);
    __ lea(rsi, Address(rbp, in_bytes(VM_Version_Ext::proc_name_6_offset())));
    __ movl(Address(rsi, 0), rcx);
    __ lea(rsi, Address(rbp, in_bytes(VM_Version_Ext::proc_name_7_offset())));
    __ movl(Address(rsi,0), rdx);

    //
    // Extended cpuid(0x80000004) // last 16 bytes in brand string
    //
    __ movl(rax, CPUID_EXTENDED_FN_4);
    __ cpuid();
    __ lea(rsi, Address(rbp, in_bytes(VM_Version_Ext::proc_name_8_offset())));
    __ movl(Address(rsi, 0), rax);
    __ lea(rsi, Address(rbp, in_bytes(VM_Version_Ext::proc_name_9_offset())));
    __ movl(Address(rsi, 0), rbx);
    __ lea(rsi, Address(rbp, in_bytes(VM_Version_Ext::proc_name_10_offset())));
    __ movl(Address(rsi, 0), rcx);
    __ lea(rsi, Address(rbp, in_bytes(VM_Version_Ext::proc_name_11_offset())));
    __ movl(Address(rsi,0), rdx);

    //
    // return
    //
    __ bind(done);
    __ popf();
    __ pop(rsi);
    __ pop(rbx);
    __ pop(rbp);
    __ ret(0);

#   undef __

    return start;
  };
};


// VM_Version_Ext statics
const size_t VM_Version_Ext::VENDOR_LENGTH = 13;
const size_t VM_Version_Ext::CPU_EBS_MAX_LENGTH = (3 * 4 * 4 + 1);
const size_t VM_Version_Ext::CPU_TYPE_DESC_BUF_SIZE = 256;
const size_t VM_Version_Ext::CPU_DETAILED_DESC_BUF_SIZE = 4096;
char* VM_Version_Ext::_cpu_brand_string = NULL;
int64_t VM_Version_Ext::_max_qualified_cpu_frequency = 0;

int VM_Version_Ext::_no_of_threads = 0;
int VM_Version_Ext::_no_of_cores = 0;
int VM_Version_Ext::_no_of_packages = 0;

void VM_Version_Ext::initialize(void) {
  ResourceMark rm;

  cpuid_brand_string_stub_blob = BufferBlob::create("getCPUIDBrandString_stub", cpuid_brand_string_stub_size);
  if (cpuid_brand_string_stub_blob == NULL) {
    vm_exit_during_initialization("Unable to allocate getCPUIDBrandString_stub");
  }
  CodeBuffer c(cpuid_brand_string_stub_blob);
  VM_Version_Ext_StubGenerator g(&c);
  getCPUIDBrandString_stub = CAST_TO_FN_PTR(getCPUIDBrandString_stub_t,
                                   g.generate_getCPUIDBrandString());
}

const char* VM_Version_Ext::cpu_model_description(void) {
  uint32_t cpu_family = extended_cpu_family();
  uint32_t cpu_model = extended_cpu_model();
  const char* model = NULL;

  if (cpu_family == CPU_FAMILY_PENTIUMPRO) {
    for (uint32_t i = 0; i <= cpu_model; i++) {
      model = _model_id_pentium_pro[i];
      if (model == NULL) {
        break;
      }
    }
  }
  return model;
}

const char* VM_Version_Ext::cpu_brand_string(void) {
  if (_cpu_brand_string == NULL) {
    _cpu_brand_string = NEW_C_HEAP_ARRAY_RETURN_NULL(char, CPU_EBS_MAX_LENGTH, mtInternal);
    if (NULL == _cpu_brand_string) {
      return NULL;
    }
    int ret_val = cpu_extended_brand_string(_cpu_brand_string, CPU_EBS_MAX_LENGTH);
    if (ret_val != OS_OK) {
      FREE_C_HEAP_ARRAY(char, _cpu_brand_string);
      _cpu_brand_string = NULL;
    }
  }
  return _cpu_brand_string;
}

const char* VM_Version_Ext::cpu_brand(void) {
  const char*  brand  = NULL;

  if ((_cpuid_info.std_cpuid1_ebx.value & 0xFF) > 0) {
    int brand_num = _cpuid_info.std_cpuid1_ebx.value & 0xFF;
    brand = _brand_id[0];
    for (int i = 0; brand != NULL && i <= brand_num; i += 1) {
      brand = _brand_id[i];
    }
  }
  return brand;
}

bool VM_Version_Ext::cpu_is_em64t(void) {
  return ((_cpuid_info.ext_cpuid1_edx.value & INTEL64_FLAG) == INTEL64_FLAG);
}

bool VM_Version_Ext::is_netburst(void) {
  return (is_intel() && (extended_cpu_family() == CPU_FAMILY_PENTIUM_4));
}

bool VM_Version_Ext::supports_tscinv_ext(void) {
  if (!supports_tscinv_bit()) {
    return false;
  }

  if (is_intel()) {
    return true;
  }

  if (is_amd()) {
    return !is_amd_Barcelona();
  }

  if (is_hygon()) {
    return true;
  }

  return false;
}

void VM_Version_Ext::resolve_cpu_information_details(void) {

  // in future we want to base this information on proper cpu
  // and cache topology enumeration such as:
  // Intel 64 Architecture Processor Topology Enumeration
  // which supports system cpu and cache topology enumeration
  // either using 2xAPICIDs or initial APICIDs

  // currently only rough cpu information estimates
  // which will not necessarily reflect the exact configuration of the system

  // this is the number of logical hardware threads
  // visible to the operating system
  _no_of_threads = os::processor_count();

  // find out number of threads per cpu package
  int threads_per_package = threads_per_core() * cores_per_cpu();

  // use amount of threads visible to the process in order to guess number of sockets
  _no_of_packages = _no_of_threads / threads_per_package;

  // process might only see a subset of the total number of threads
  // from a single processor package. Virtualization/resource management for example.
  // If so then just write a hard 1 as num of pkgs.
  if (0 == _no_of_packages) {
    _no_of_packages = 1;
  }

  // estimate the number of cores
  _no_of_cores = cores_per_cpu() * _no_of_packages;
}

int VM_Version_Ext::number_of_threads(void) {
  if (_no_of_threads == 0) {
   resolve_cpu_information_details();
  }
  return _no_of_threads;
}

int VM_Version_Ext::number_of_cores(void) {
  if (_no_of_cores == 0) {
    resolve_cpu_information_details();
  }
  return _no_of_cores;
}

int VM_Version_Ext::number_of_sockets(void) {
  if (_no_of_packages == 0) {
    resolve_cpu_information_details();
  }
  return _no_of_packages;
}

const char* VM_Version_Ext::cpu_family_description(void) {
  int cpu_family_id = extended_cpu_family();
  if (is_amd()) {
    if (cpu_family_id < ExtendedFamilyIdLength_AMD) {
      return _family_id_amd[cpu_family_id];
    }
  }
  if (is_intel()) {
    if (cpu_family_id == CPU_FAMILY_PENTIUMPRO) {
      return cpu_model_description();
    }
    if (cpu_family_id < ExtendedFamilyIdLength_INTEL) {
      return _family_id_intel[cpu_family_id];
    }
  }
  if (is_hygon()) {
    return "Dhyana";
  }
  return "Unknown x86";
}

int VM_Version_Ext::cpu_type_description(char* const buf, size_t buf_len) {
  assert(buf != NULL, "buffer is NULL!");
  assert(buf_len >= CPU_TYPE_DESC_BUF_SIZE, "buffer len should at least be == CPU_TYPE_DESC_BUF_SIZE!");

  const char* cpu_type = NULL;
  const char* x64 = NULL;

  if (is_intel()) {
    cpu_type = "Intel";
    x64 = cpu_is_em64t() ? " Intel64" : "";
  } else if (is_amd()) {
    cpu_type = "AMD";
    x64 = cpu_is_em64t() ? " AMD64" : "";
  } else if (is_hygon()) {
    cpu_type = "Hygon";
    x64 = cpu_is_em64t() ? " AMD64" : "";
  } else {
    cpu_type = "Unknown x86";
    x64 = cpu_is_em64t() ? " x86_64" : "";
  }

  jio_snprintf(buf, buf_len, "%s %s%s SSE SSE2%s%s%s%s%s%s%s%s",
    cpu_type,
    cpu_family_description(),
    supports_ht() ? " (HT)" : "",
    supports_sse3() ? " SSE3" : "",
    supports_ssse3() ? " SSSE3" : "",
    supports_sse4_1() ? " SSE4.1" : "",
    supports_sse4_2() ? " SSE4.2" : "",
    supports_sse4a() ? " SSE4A" : "",
    is_netburst() ? " Netburst" : "",
    is_intel_family_core() ? " Core" : "",
    x64);

  return OS_OK;
}

int VM_Version_Ext::cpu_extended_brand_string(char* const buf, size_t buf_len) {
  assert(buf != NULL, "buffer is NULL!");
  assert(buf_len >= CPU_EBS_MAX_LENGTH, "buffer len should at least be == CPU_EBS_MAX_LENGTH!");
  assert(getCPUIDBrandString_stub != NULL, "not initialized");

  // invoke newly generated asm code to fetch CPU Brand String
  getCPUIDBrandString_stub(&_cpuid_info);

  // fetch results into buffer
  *((uint32_t*) &buf[0])  = _cpuid_info.proc_name_0;
  *((uint32_t*) &buf[4])  = _cpuid_info.proc_name_1;
  *((uint32_t*) &buf[8])  = _cpuid_info.proc_name_2;
  *((uint32_t*) &buf[12]) = _cpuid_info.proc_name_3;
  *((uint32_t*) &buf[16]) = _cpuid_info.proc_name_4;
  *((uint32_t*) &buf[20]) = _cpuid_info.proc_name_5;
  *((uint32_t*) &buf[24]) = _cpuid_info.proc_name_6;
  *((uint32_t*) &buf[28]) = _cpuid_info.proc_name_7;
  *((uint32_t*) &buf[32]) = _cpuid_info.proc_name_8;
  *((uint32_t*) &buf[36]) = _cpuid_info.proc_name_9;
  *((uint32_t*) &buf[40]) = _cpuid_info.proc_name_10;
  *((uint32_t*) &buf[44]) = _cpuid_info.proc_name_11;

  return OS_OK;
}

size_t VM_Version_Ext::cpu_write_support_string(char* const buf, size_t buf_len) {
  guarantee(buf != NULL, "buffer is NULL!");
  guarantee(buf_len > 0, "buffer len not enough!");

  unsigned int flag = 0;
  unsigned int fi = 0;
  size_t       written = 0;
  const char*  prefix = "";

#define WRITE_TO_BUF(string)                                                          \
  {                                                                                   \
    int res = jio_snprintf(&buf[written], buf_len - written, "%s%s", prefix, string); \
    if (res < 0) {                                                                    \
      return buf_len - 1;                                                             \
    }                                                                                 \
    written += res;                                                                   \
    if (prefix[0] == '\0') {                                                          \
      prefix = ", ";                                                                  \
    }                                                                                 \
  }

  for (flag = 1, fi = 0; flag <= 0x20000000 ; flag <<= 1, fi++) {
    if (flag == HTT_FLAG && (((_cpuid_info.std_cpuid1_ebx.value >> 16) & 0xff) <= 1)) {
      continue; /* no hyperthreading */
    } else if (flag == SEP_FLAG && (cpu_family() == CPU_FAMILY_PENTIUMPRO && ((_cpuid_info.std_cpuid1_eax.value & 0xff) < 0x33))) {
      continue; /* no fast system call */
    }
    if ((_cpuid_info.std_cpuid1_edx.value & flag) && strlen(_feature_edx_id[fi]) > 0) {
      WRITE_TO_BUF(_feature_edx_id[fi]);
    }
  }

  for (flag = 1, fi = 0; flag <= 0x20000000; flag <<= 1, fi++) {
    if ((_cpuid_info.std_cpuid1_ecx.value & flag) && strlen(_feature_ecx_id[fi]) > 0) {
      WRITE_TO_BUF(_feature_ecx_id[fi]);
    }
  }

  for (flag = 1, fi = 0; flag <= 0x20000000 ; flag <<= 1, fi++) {
    if ((_cpuid_info.ext_cpuid1_ecx.value & flag) && strlen(_feature_extended_ecx_id[fi]) > 0) {
      WRITE_TO_BUF(_feature_extended_ecx_id[fi]);
    }
  }

  for (flag = 1, fi = 0; flag <= 0x20000000; flag <<= 1, fi++) {
    if ((_cpuid_info.ext_cpuid1_edx.value & flag) && strlen(_feature_extended_edx_id[fi]) > 0) {
      WRITE_TO_BUF(_feature_extended_edx_id[fi]);
    }
  }

  if (supports_tscinv_bit()) {
      WRITE_TO_BUF("Invariant TSC");
  }

  return written;
}

/**
 * Write a detailed description of the cpu to a given buffer, including
 * feature set.
 */
int VM_Version_Ext::cpu_detailed_description(char* const buf, size_t buf_len) {
  assert(buf != NULL, "buffer is NULL!");
  assert(buf_len >= CPU_DETAILED_DESC_BUF_SIZE, "buffer len should at least be == CPU_DETAILED_DESC_BUF_SIZE!");

  static const char* unknown = "<unknown>";
  char               vendor_id[VENDOR_LENGTH];
  const char*        family = NULL;
  const char*        model = NULL;
  const char*        brand = NULL;
  int                outputLen = 0;

  family = cpu_family_description();
  if (family == NULL) {
    family = unknown;
  }

  model = cpu_model_description();
  if (model == NULL) {
    model = unknown;
  }

  brand = cpu_brand_string();

  if (brand == NULL) {
    brand = cpu_brand();
    if (brand == NULL) {
      brand = unknown;
    }
  }

  *((uint32_t*) &vendor_id[0]) = _cpuid_info.std_vendor_name_0;
  *((uint32_t*) &vendor_id[4]) = _cpuid_info.std_vendor_name_2;
  *((uint32_t*) &vendor_id[8]) = _cpuid_info.std_vendor_name_1;
  vendor_id[VENDOR_LENGTH-1] = '\0';

  outputLen = jio_snprintf(buf, buf_len, "Brand: %s, Vendor: %s\n"
    "Family: %s (0x%x), Model: %s (0x%x), Stepping: 0x%x\n"
    "Ext. family: 0x%x, Ext. model: 0x%x, Type: 0x%x, Signature: 0x%8.8x\n"
    "Features: ebx: 0x%8.8x, ecx: 0x%8.8x, edx: 0x%8.8x\n"
    "Ext. features: eax: 0x%8.8x, ebx: 0x%8.8x, ecx: 0x%8.8x, edx: 0x%8.8x\n"
    "Supports: ",
    brand,
    vendor_id,
    family,
    extended_cpu_family(),
    model,
    extended_cpu_model(),
    cpu_stepping(),
    _cpuid_info.std_cpuid1_eax.bits.ext_family,
    _cpuid_info.std_cpuid1_eax.bits.ext_model,
    _cpuid_info.std_cpuid1_eax.bits.proc_type,
    _cpuid_info.std_cpuid1_eax.value,
    _cpuid_info.std_cpuid1_ebx.value,
    _cpuid_info.std_cpuid1_ecx.value,
    _cpuid_info.std_cpuid1_edx.value,
    _cpuid_info.ext_cpuid1_eax,
    _cpuid_info.ext_cpuid1_ebx,
    _cpuid_info.ext_cpuid1_ecx,
    _cpuid_info.ext_cpuid1_edx);

  if (outputLen < 0 || (size_t) outputLen >= buf_len - 1) {
    if (buf_len > 0) { buf[buf_len-1] = '\0'; }
    return OS_ERR;
  }

  cpu_write_support_string(&buf[outputLen], buf_len - outputLen);

  return OS_OK;
}

const char* VM_Version_Ext::cpu_name(void) {
  char cpu_type_desc[CPU_TYPE_DESC_BUF_SIZE];
  size_t cpu_desc_len = sizeof(cpu_type_desc);

  cpu_type_description(cpu_type_desc, cpu_desc_len);
  char* tmp = NEW_C_HEAP_ARRAY_RETURN_NULL(char, cpu_desc_len, mtTracing);
  if (NULL == tmp) {
    return NULL;
  }
  strncpy(tmp, cpu_type_desc, cpu_desc_len);
  return tmp;
}

const char* VM_Version_Ext::cpu_description(void) {
  char cpu_detailed_desc_buffer[CPU_DETAILED_DESC_BUF_SIZE];
  size_t cpu_detailed_desc_len = sizeof(cpu_detailed_desc_buffer);

  cpu_detailed_description(cpu_detailed_desc_buffer, cpu_detailed_desc_len);

  char* tmp = NEW_C_HEAP_ARRAY_RETURN_NULL(char, cpu_detailed_desc_len, mtTracing);

  if (NULL == tmp) {
    return NULL;
  }

  strncpy(tmp, cpu_detailed_desc_buffer, cpu_detailed_desc_len);
  return tmp;
}

/**
 *  For information about extracting the frequency from the cpu brand string, please see:
 *
 *    Intel Processor Identification and the CPUID Instruction
 *    Application Note 485
 *    May 2012
 *
 * The return value is the frequency in Hz.
 */
int64_t VM_Version_Ext::max_qualified_cpu_freq_from_brand_string(void) {
  const char* const brand_string = cpu_brand_string();
  if (brand_string == NULL) {
    return 0;
  }
  const int64_t MEGA = 1000000;
  int64_t multiplier = 0;
  int64_t frequency = 0;
  uint8_t idx = 0;
  // The brand string buffer is at most 48 bytes.
  // -2 is to prevent buffer overrun when looking for y in yHz, as z is +2 from y.
  for (; idx < 48-2; ++idx) {
    // Format is either "x.xxyHz" or "xxxxyHz", where y=M, G, T and x are digits.
    // Search brand string for "yHz" where y is M, G, or T.
    if (brand_string[idx+1] == 'H' && brand_string[idx+2] == 'z') {
      if (brand_string[idx] == 'M') {
        multiplier = MEGA;
      } else if (brand_string[idx] == 'G') {
        multiplier = MEGA * 1000;
      } else if (brand_string[idx] == 'T') {
        multiplier = MEGA * MEGA;
      }
      break;
    }
  }
  if (multiplier > 0) {
    // Compute freqency (in Hz) from brand string.
    if (brand_string[idx-3] == '.') { // if format is "x.xx"
      frequency =  (brand_string[idx-4] - '0') * multiplier;
      frequency += (brand_string[idx-2] - '0') * multiplier / 10;
      frequency += (brand_string[idx-1] - '0') * multiplier / 100;
    } else { // format is "xxxx"
      frequency =  (brand_string[idx-4] - '0') * 1000;
      frequency += (brand_string[idx-3] - '0') * 100;
      frequency += (brand_string[idx-2] - '0') * 10;
      frequency += (brand_string[idx-1] - '0');
      frequency *= multiplier;
    }
  }
  return frequency;
}


int64_t VM_Version_Ext::maximum_qualified_cpu_frequency(void) {
  if (_max_qualified_cpu_frequency == 0) {
    _max_qualified_cpu_frequency = max_qualified_cpu_freq_from_brand_string();
  }
  return _max_qualified_cpu_frequency;
}

const char* const VM_Version_Ext::_family_id_intel[ExtendedFamilyIdLength_INTEL] = {
  "8086/8088",
  "",
  "286",
  "386",
  "486",
  "Pentium",
  "Pentium Pro",   //or Pentium-M/Woodcrest depeding on model
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "Pentium 4"
};

const char* const VM_Version_Ext::_family_id_amd[ExtendedFamilyIdLength_AMD] = {
  "",
  "",
  "",
  "",
  "5x86",
  "K5/K6",
  "Athlon/AthlonXP",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "Opteron/Athlon64",
  "Opteron QC/Phenom",  // Barcelona et.al.
  "",
  "",
  "",
  "",
  "",
  "",
  "Zen"
};
// Partially from Intel 64 and IA-32 Architecture Software Developer's Manual,
// September 2013, Vol 3C Table 35-1
const char* const VM_Version_Ext::_model_id_pentium_pro[] = {
  "",
  "Pentium Pro",
  "",
  "Pentium II model 3",
  "",
  "Pentium II model 5/Xeon/Celeron",
  "Celeron",
  "Pentium III/Pentium III Xeon",
  "Pentium III/Pentium III Xeon",
  "Pentium M model 9",    // Yonah
  "Pentium III, model A",
  "Pentium III, model B",
  "",
  "Pentium M model D",    // Dothan
  "",
  "Core 2",               // 0xf Woodcrest/Conroe/Merom/Kentsfield/Clovertown
  "",
  "",
  "",
  "",
  "",
  "",
  "Celeron",              // 0x16 Celeron 65nm
  "Core 2",               // 0x17 Penryn / Harpertown
  "",
  "",
  "Core i7",              // 0x1A CPU_MODEL_NEHALEM_EP
  "Atom",                 // 0x1B Z5xx series Silverthorn
  "",
  "Core 2",               // 0x1D Dunnington (6-core)
  "Nehalem",              // 0x1E CPU_MODEL_NEHALEM
  "",
  "",
  "",
  "",
  "",
  "",
  "Westmere",             // 0x25 CPU_MODEL_WESTMERE
  "",
  "",
  "",                     // 0x28
  "",
  "Sandy Bridge",         // 0x2a "2nd Generation Intel Core i7, i5, i3"
  "",
  "Westmere-EP",          // 0x2c CPU_MODEL_WESTMERE_EP
  "Sandy Bridge-EP",      // 0x2d CPU_MODEL_SANDYBRIDGE_EP
  "Nehalem-EX",           // 0x2e CPU_MODEL_NEHALEM_EX
  "Westmere-EX",          // 0x2f CPU_MODEL_WESTMERE_EX
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "Ivy Bridge",           // 0x3a
  "",
  "Haswell",              // 0x3c "4th Generation Intel Core Processor"
  "",                     // 0x3d "Next Generation Intel Core Processor"
  "Ivy Bridge-EP",        // 0x3e "Next Generation Intel Xeon Processor E7 Family"
  "",                     // 0x3f "Future Generation Intel Xeon Processor"
  "",
  "",
  "",
  "",
  "",
  "Haswell",              // 0x45 "4th Generation Intel Core Processor"
  "Haswell",              // 0x46 "4th Generation Intel Core Processor"
  NULL
};

/* Brand ID is for back compability
 * Newer CPUs uses the extended brand string */
const char* const VM_Version_Ext::_brand_id[] = {
  "",
  "Celeron processor",
  "Pentium III processor",
  "Intel Pentium III Xeon processor",
  "",
  "",
  "",
  "",
  "Intel Pentium 4 processor",
  NULL
};


const char* const VM_Version_Ext::_feature_edx_id[] = {
  "On-Chip FPU",
  "Virtual Mode Extensions",
  "Debugging Extensions",
  "Page Size Extensions",
  "Time Stamp Counter",
  "Model Specific Registers",
  "Physical Address Extension",
  "Machine Check Exceptions",
  "CMPXCHG8B Instruction",
  "On-Chip APIC",
  "",
  "Fast System Call",
  "Memory Type Range Registers",
  "Page Global Enable",
  "Machine Check Architecture",
  "Conditional Mov Instruction",
  "Page Attribute Table",
  "36-bit Page Size Extension",
  "Processor Serial Number",
  "CLFLUSH Instruction",
  "",
  "Debug Trace Store feature",
  "ACPI registers in MSR space",
  "Intel Architecture MMX Technology",
  "Fast Float Point Save and Restore",
  "Streaming SIMD extensions",
  "Streaming SIMD extensions 2",
  "Self-Snoop",
  "Hyper Threading",
  "Thermal Monitor",
  "",
  "Pending Break Enable"
};

const char* const VM_Version_Ext::_feature_extended_edx_id[] = {
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "SYSCALL/SYSRET",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "Execute Disable Bit",
  "",
  "",
  "",
  "",
  "",
  "",
  "RDTSCP",
  "",
  "Intel 64 Architecture",
  "",
  ""
};

const char* const VM_Version_Ext::_feature_ecx_id[] = {
  "Streaming SIMD Extensions 3",
  "PCLMULQDQ",
  "64-bit DS Area",
  "MONITOR/MWAIT instructions",
  "CPL Qualified Debug Store",
  "Virtual Machine Extensions",
  "Safer Mode Extensions",
  "Enhanced Intel SpeedStep technology",
  "Thermal Monitor 2",
  "Supplemental Streaming SIMD Extensions 3",
  "L1 Context ID",
  "",
  "Fused Multiply-Add",
  "CMPXCHG16B",
  "xTPR Update Control",
  "Perfmon and Debug Capability",
  "",
  "Process-context identifiers",
  "Direct Cache Access",
  "Streaming SIMD extensions 4.1",
  "Streaming SIMD extensions 4.2",
  "x2APIC",
  "MOVBE",
  "Popcount instruction",
  "TSC-Deadline",
  "AESNI",
  "XSAVE",
  "OSXSAVE",
  "AVX",
  "F16C",
  "RDRAND",
  ""
};

const char* const VM_Version_Ext::_feature_extended_ecx_id[] = {
  "LAHF/SAHF instruction support",
  "Core multi-processor legacy mode",
  "",
  "",
  "",
  "Advanced Bit Manipulations: LZCNT",
  "SSE4A: MOVNTSS, MOVNTSD, EXTRQ, INSERTQ",
  "Misaligned SSE mode",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  "",
  ""
};
