/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "jni.h"
#include "stdio.h"
#include "stdlib.h"

int main(int argc, char** args) {
    JavaVMInitArgs vm_args;
    JNIEnv *env;
    JavaVM *vm;
    int i =0;
    jint result;

    vm_args.version = JNI_VERSION_1_2;
    vm_args.ignoreUnrecognized = JNI_FALSE;

    JavaVMOption option1[2];
    option1[0].optionString="-XX:+PrintCommandLineFlags";
    option1[1].optionString="-Xrs";

    vm_args.options=option1;
    vm_args.nOptions=2;

    // Print the VM options in use
    printf("initVM: numOptions = %d\n", vm_args.nOptions);
    for (i = 0; i < vm_args.nOptions; i++)
    {
        printf("\tvm_args.options[%d].optionString = %s\n", i, vm_args.options[i].optionString);
    }

    // Initialize VM with given options
    result = JNI_CreateJavaVM( &vm, (void **) &env, &vm_args );
    if (result != 0) {
        printf("ERROR: cannot create JAVA VM.\n");
        exit(-1);
    }

    (*vm)->DestroyJavaVM(vm);
}

