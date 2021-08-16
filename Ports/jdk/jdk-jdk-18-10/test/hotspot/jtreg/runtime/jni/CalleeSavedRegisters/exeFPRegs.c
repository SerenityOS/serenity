/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
#include <jni.h>
#include <stdlib.h>

#ifdef WINDOWS
#include <windows.h>
#else
#include <dlfcn.h>
#endif // WINDOWS

#ifdef WINDOWS
  HMODULE handle;
#else
  void* handle;
#endif // WINDOWS

jint(JNICALL *jni_create_java_vm)(JavaVM **, JNIEnv **, void *) = NULL;

// method to perform dlclose on an open dynamic library handle
void closeHandle() {
#ifdef WINDOWS
  if (!FreeLibrary(handle)) {
   fprintf(stderr, "Error occurred while closing handle: 0x%02X\n", GetLastError());
  }
#else
  if (dlclose(handle) != 0) {
    fprintf(stderr, "Error occurred while closing handle: %s\n", dlerror());
  }
#endif // WINDOWS
}

void fail(int code) {
  if (handle) {
    closeHandle();
  }
  exit(code);
}


// method to load the dynamic library libjvm
int loadJVM(const char* path) {
#ifdef WINDOWS
  UINT errorMode = GetErrorMode();
  SetErrorMode(errorMode | SEM_FAILCRITICALERRORS | SEM_NOOPENFILEERRORBOX);
  handle = LoadLibraryA(path);
#else
  handle = dlopen(path, RTLD_LAZY);
#endif // WINDOWS

  if (handle) {
    // find the address of function
#ifdef WINDOWS
    *(void **) (&jni_create_java_vm) = GetProcAddress(handle, "JNI_CreateJavaVM");
#else
    *(void **) (&jni_create_java_vm) = dlsym(handle, "JNI_CreateJavaVM");
#endif // WINDOWS

    if (jni_create_java_vm == NULL) {
      fprintf(stderr, "ERROR: No JNI_CreateJavaVM found: '%s'\n", path);
      return -1;
    }
  } else {
#ifdef WINDOWS
    fprintf(stderr, "ERROR: Can't load JVM library: 0x%02X\n", GetLastError());
#else
    fprintf(stderr, "ERROR: Can't load JVM library: %s\n", dlerror());
#endif // WINDOWS
    return -1;
  }
  return 0;
}

long long unsigned int d2l(double d) {
  union {
    double d;
    long long unsigned int llu;
  } dl;

  dl.d = d;
  return dl.llu;
}

#define print_reg(r) printf("%s = %f (0x%llX)\n", #r, r, d2l(r));

int main(int argc, const char** argv) {
  JavaVM* jvm;
  JNIEnv* env;
  JavaVMInitArgs vm_args;

  // values to trick constant folding
  long long unsigned int vd[32];
  int i;
  int bad_cnt = 0;

  // values occupy fp registers
  // note: suitable code shape is produced only on Windows,
  // and even then registers are corrupted not on every machine
  register double d00;
  register double d01;
  register double d02;
  register double d03;
  register double d04;
  register double d05;
  register double d06;
  register double d07;
  register double d08;
  register double d09;
  register double d10;
  register double d11;
  register double d12;
  register double d13;
  register double d14;
  register double d15;

  if (argc != 2) {
    printf("Usage: FPRegs <jvm_path>");
    fail(2);
  }
  printf("jvm_path = %s\n", argv[1]);

  if (loadJVM(argv[1]) < 0) {
    fail(3);
  }

  vm_args.version = JNI_VERSION_1_8;
  vm_args.ignoreUnrecognized = JNI_FALSE;
  vm_args.options = NULL;
  vm_args.nOptions = 0;

  for(i = 0; i < 16; i++) {
    vd[i] = d2l(100 + i);
  }

  d00 = 100.0;
  d01 = 101.0;
  d02 = 102.0;
  d03 = 103.0;
  d04 = 104.0;
  d05 = 105.0;
  d06 = 106.0;
  d07 = 107.0;
  d08 = 108.0;
  d09 = 109.0;
  d10 = 110.0;
  d11 = 111.0;
  d12 = 112.0;
  d13 = 113.0;
  d14 = 114.0;
  d15 = 115.0;

  printf("BEFORE:\n");
  print_reg(d00);
  print_reg(d01);
  print_reg(d02);
  print_reg(d03);
  print_reg(d04);
  print_reg(d05);
  print_reg(d06);
  print_reg(d07);
  print_reg(d08);
  print_reg(d09);
  print_reg(d10);
  print_reg(d11);
  print_reg(d12);
  print_reg(d13);
  print_reg(d14);
  print_reg(d15);

  if (jni_create_java_vm(&jvm, &env, &vm_args) < 0 ) {
    fprintf(stderr, "ERROR: Can't create JavaVM\n");
    fail(4);
  }

  if (d2l(d00) != vd[0]) bad_cnt++;
  if (d2l(d01) != vd[1]) bad_cnt++;
  if (d2l(d02) != vd[2]) bad_cnt++;
  if (d2l(d03) != vd[3]) bad_cnt++;
  if (d2l(d04) != vd[4]) bad_cnt++;
  if (d2l(d05) != vd[5]) bad_cnt++;
  if (d2l(d06) != vd[6]) bad_cnt++;
  if (d2l(d07) != vd[7]) bad_cnt++;
  if (d2l(d08) != vd[8]) bad_cnt++;
  if (d2l(d09) != vd[9]) bad_cnt++;
  if (d2l(d10) != vd[10]) bad_cnt++;
  if (d2l(d11) != vd[11]) bad_cnt++;
  if (d2l(d12) != vd[12]) bad_cnt++;
  if (d2l(d13) != vd[13]) bad_cnt++;
  if (d2l(d14) != vd[14]) bad_cnt++;
  if (d2l(d15) != vd[15]) bad_cnt++;

  printf("AFTER:\n");
  print_reg(d00);
  print_reg(d01);
  print_reg(d02);
  print_reg(d03);
  print_reg(d04);
  print_reg(d05);
  print_reg(d06);
  print_reg(d07);
  print_reg(d08);
  print_reg(d09);
  print_reg(d10);
  print_reg(d11);
  print_reg(d12);
  print_reg(d13);
  print_reg(d14);
  print_reg(d15);

  printf("%d registers corrupted\n", bad_cnt);
  if (bad_cnt > 0) {
      printf("TEST FAILED");
      fail(1);
  }

  printf("TEST PASSED");
  closeHandle();
  return 0;
}

