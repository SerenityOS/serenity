/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * A simple way to test JVMTI ClassFileLoadHook. See ../testlibrary_tests/jvmti/SimpleClassFileLoadHookTest.java
 * for an example.
 */
#include <stdio.h>
#include <stdarg.h>
#include <stdlib.h>
#include <string.h>

#include <jvmti.h>
#include <jni.h>

static char* CLASS_NAME = NULL;
static char* FROM = NULL;
static char* TO = NULL;
static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;

/**
 * For all classes whose name equals to CLASS_NAME, we replace all occurrence of FROM to TO
 * in the classfile data. CLASS_NAME must be a binary class name.
 *
 * FROM is usually chosen as part of a UTF8 string in the class file. For example, if the
 * original class file has
 *    String getXXX() { return "theXXX";}
 * You can set FROM=XXX, TO=YYY to rewrite the class to be
 *    String getYYY() { return "theYYY";}
 *
 * Please note that the replacement is NOT limited just the UTF8 strings, but rather applies
 * to all the bytes in the classfile. So if you pick a very short FROM string like X,
 * it may override any POP2 bytecodes, which have the value 88 (ascii 'X').
 *
 * A good FROM string to use is 'cellphone', where the first 4 bytes represent the bytecode
 * sequence DADD/LSUB/IDIV/IDIV, which does not appear in valid bytecode streams.
 */
void JNICALL
ClassFileLoadHook(jvmtiEnv *jvmti_env, JNIEnv *env, jclass class_beeing_redefined,
        jobject loader, const char* name, jobject protection_domain,
        jint class_data_len, const unsigned char* class_data,
        jint *new_class_data_len, unsigned char** new_class_data) {

    if (name != NULL && (strcmp(name, CLASS_NAME) == 0)) {
      size_t n = strlen(FROM);
      unsigned char* new_data;

      if ((*jvmti)->Allocate(jvmti, class_data_len, &new_data) == JNI_OK) {
        const unsigned char* s = class_data;
        unsigned char* d = new_data;
        unsigned char* end = d + class_data_len;
        int count = 0;

        fprintf(stderr, "found class to be hooked: %s - rewriting ...\n", name);

        while (d + n < end) {
          if (memcmp(s, FROM, n) == 0) {
            memcpy(d, TO, n);
            s += n;
            d += n;
            count++;
          } else {
            *d++ = *s++;
          }
        }
        while (d < end) {
          *d++ = *s++;
        }

        *new_class_data_len = class_data_len;
        *new_class_data = new_data;

        fprintf(stderr, "Rewriting done. Replaced %d occurrence(s) of \"%s\" to \"%s\"\n", count, FROM, TO);
      }
    }
}

static int early = 0;

static jint init_options(char *options) {
  char* class_name;
  char* from;
  char* to;

  fprintf(stderr, "Agent library loaded with options = %s\n", options);
  if (options != NULL && strncmp(options, "-early,", 7) == 0) {
    early = 1;
    options += 7;
  }
  if ((class_name = options) != NULL &&
      (from = strchr(class_name, ',')) != NULL && (from[1] != 0)) {
    *from = 0;
    from++;
    if ((to = strchr(from, ',')) != NULL && (to[1] != 0)) {
      *to = 0;
      to++;
      if (strchr(to, ',') == NULL &&
          strlen(to) == strlen(from) &&
          strlen(class_name) > 0 &&
          strlen(to) > 0) {
        CLASS_NAME = strdup(class_name);
        FROM = strdup(from);
        TO = strdup(to);
        fprintf(stderr, "CLASS_NAME = %s, FROM = %s, TO = %s\n",
                CLASS_NAME, FROM, TO);
        return JNI_OK;
      }
    }
  }
  fprintf(stderr,
          "Incorrect options. You need to start the JVM with -agentlib:ClassFileLoadHook=<classname>,<from>,<to>\n"
          "where <classname> is the class you want to hook, <from> is the string in the classfile to be replaced\n"
          "with <to>.  <from> and <to> must have the same length. Example:\n"
          "    @run main/native -agentlib:ClassFileLoadHook=Foo,XXX,YYY ClassFileLoadHookTest\n");
  return JNI_ERR;
}

static jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
  int rc;
  jvmtiCapabilities caps;

  if ((rc = (*jvm)->GetEnv(jvm, (void **)&jvmti, JVMTI_VERSION_1_1)) != JNI_OK) {
    fprintf(stderr, "Unable to create jvmtiEnv, GetEnv failed, error = %d\n", rc);
    return JNI_ERR;
  }
  if ((rc = init_options(options)) != JNI_OK) {
    return JNI_ERR;
  }

  memset(&caps, 0, sizeof(caps));

  caps.can_redefine_classes = 1;
  if (early) {
    fprintf(stderr, "can_generate_all_class_hook_events/can_generate_early_vmstart/can_generate_early_class_hook_events == 1\n");
    caps.can_generate_all_class_hook_events = 1;
    caps.can_generate_early_class_hook_events = 1;
  }
  if ((rc = (*jvmti)->AddCapabilities(jvmti, &caps)) != JNI_OK) {
    fprintf(stderr, "AddCapabilities failed, error = %d\n", rc);
    return JNI_ERR;
  }

  (void) memset(&callbacks, 0, sizeof(callbacks));
  callbacks.ClassFileLoadHook = &ClassFileLoadHook;
  if ((rc = (*jvmti)->SetEventCallbacks(jvmti, &callbacks, sizeof(callbacks))) != JNI_OK) {
    fprintf(stderr, "SetEventCallbacks failed, error = %d\n", rc);
    return JNI_ERR;
  }

  if ((rc = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE,
                                               JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL)) != JNI_OK) {
    fprintf(stderr, "SetEventNotificationMode failed, error = %d\n", rc);
    return JNI_ERR;
  }

  return JNI_OK;
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
  return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT jint JNICALL Agent_OnAttach(JavaVM *jvm, char *options, void *reserved) {
  return Agent_Initialize(jvm, options, reserved);
}
