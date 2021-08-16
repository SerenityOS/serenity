/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * A miniature launcher for use by CustomLauncherTest.java test. It sets
 * up the absolute minimal execution environment.
 */
#include <stdlib.h>
#include <string.h>
#include <dlfcn.h>

#include "jni.h"

typedef jint (*create_vm_func)(JavaVM **, void**, void*);

void *JNU_FindCreateJavaVM(char *vmlibpath) {
    void *libVM = dlopen(vmlibpath, RTLD_LAZY);
    if (libVM == NULL) {
        return NULL;
    }
    return dlsym(libVM, "JNI_CreateJavaVM");
}

#define CP_PROP  "-Djava.class.path="

int main(int argc, char**argv) {
     JNIEnv *env;
     JavaVM *jvm;
     jint res;
     jclass cls;
     jmethodID mid;
     jstring jstr;
     jclass stringClass;
     jobjectArray args;
     create_vm_func create_vm;
     JavaVMInitArgs vm_args;
     char* cp_prop;
     JavaVMOption options[1];

     if (argc < 4) {
        fprintf(stderr, "Usage: %s jvm-path classpath class\n", argv[0]);
        return -1;
     }
     cp_prop = (char*)malloc(strlen(CP_PROP)+strlen(argv[2]) +1);
     sprintf(cp_prop, "%s%s", CP_PROP, argv[2]);

     options[0].optionString = cp_prop;
     vm_args.version = 0x00010002;
     vm_args.options = options;
     vm_args.nOptions = 1;
     vm_args.ignoreUnrecognized = JNI_TRUE;

     create_vm = (create_vm_func)JNU_FindCreateJavaVM(argv[1]);
     if (create_vm == NULL) {
        fprintf(stderr, "can't get address of JNI_CreateJavaVM\n");
        return -1;
     }

     res = (*create_vm)(&jvm, (void**)&env, &vm_args);
     if (res < 0) {
         fprintf(stderr, "Can't create Java VM\n");
         return -1;
     }
     cls = (*env)->FindClass(env, argv[3]);
     if (cls == NULL) {
         goto destroy;
     }

     mid = (*env)->GetStaticMethodID(env, cls, "main",
                                     "([Ljava/lang/String;)V");
     if (mid == NULL) {
         goto destroy;
     }
     jstr = (*env)->NewStringUTF(env, " from C!");
     if (jstr == NULL) {
         goto destroy;
     }
     stringClass = (*env)->FindClass(env, "java/lang/String");
     args = (*env)->NewObjectArray(env, 1, stringClass, jstr);
     if (args == NULL) {
         goto destroy;
     }
     (*env)->CallStaticVoidMethod(env, cls, mid, args);

 destroy:
     if ((*env)->ExceptionOccurred(env)) {
         (*env)->ExceptionDescribe(env);
     }
     (*jvm)->DestroyJavaVM(jvm);

     return 0;
 }
