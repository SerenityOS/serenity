/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "jvmti.h"
#include "jni_tools.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define PASSED 0
#define STATUS_FAILED 2

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static jvmtiCapabilities jvmti_caps;
static jint dummy_user_data = 0;
static jboolean user_data_error_flag = JNI_FALSE;

#define HEAP_ROOT_REF_KIND_BASE 100
#define MISSED_REF_KIND_BASE 300

typedef enum {
  rthread,
  rclass,
  rother,
  rmark
} refKind;

struct _refLink;

typedef struct _myTag {
  refKind kind;
  const struct _myTag* class_tag;
  jlong size;
  jlong sequence;
  jboolean visited;
  const char* name;
  struct _refLink *ref;
} MyTag;

typedef struct _refLink {
  MyTag* tag;
  int reference_kind;
  struct _refLink *next;
} refLink;

static MyTag *fakeRoot = NULL;
static MyTag *missed = NULL;

static void breakpoint() {
  printf("Continuing from BREAKPOINT\n");
}

static MyTag *newTag(refKind kind,
                     const MyTag* class_tag,
                     jlong size,
                     const char* name) {
  static jlong seq_num = 0;
  MyTag* new_tag = NULL;

  new_tag = (MyTag*) malloc(sizeof(MyTag));
  if (NULL == new_tag) {
    printf("Error (newTag malloc): failed\n");
    result = STATUS_FAILED;
  }
  new_tag->kind = kind;
  new_tag->class_tag = class_tag;
  new_tag->size = size;
  new_tag->sequence = ++seq_num;
  new_tag->visited = JNI_FALSE;
  new_tag->name = name;
  new_tag->ref = NULL;
  return new_tag;
}

static void setTag(JNIEnv *env,
                   jobject obj,
                   refKind kind,
                   const char* name) {
  MyTag *new_tag = NULL;
  MyTag *class_tag = NULL;
  jvmtiError err;
  jlong size = 0;
  jclass obj_class = NULL;
  jlong haba = 0;

  err = jvmti->GetObjectSize(obj, &size);
  if (err != JVMTI_ERROR_NONE) {
    printf("Error (ObjectSize): %s (%d)\n", TranslateError(err), err);
    result = STATUS_FAILED;
  }

  obj_class = env->GetObjectClass(obj);

  err = jvmti->GetTag(obj_class, &haba);
  class_tag = (MyTag*)(intptr_t)haba;
  if (err != JVMTI_ERROR_NONE) {
    printf("Error (GetTag): %s (%d)\n", TranslateError(err), err);
    result = STATUS_FAILED;
  }
  if (class_tag != NULL && class_tag->kind != rclass) {
    printf("Error class tag which is not a class\n");
    result = STATUS_FAILED;
  }

  new_tag = newTag(kind, class_tag, size, name);

  err = jvmti->SetTag(obj, (intptr_t)new_tag);
  if (err != JVMTI_ERROR_NONE) {
    printf("Error (SetTag): %s (%d)\n", TranslateError(err), err);
    result = STATUS_FAILED;
  }
}

static void addRef(MyTag *from, int reference_kind, MyTag *to) {
  refLink *new_ref;

  new_ref = (refLink*) malloc(sizeof(refLink));
  if (NULL == new_ref) {
    printf("Error (addRef malloc): failed\n");
    result = STATUS_FAILED;
  }
  new_ref->tag = to;
  new_ref->reference_kind = reference_kind;
  new_ref->next = from->ref;
  from->ref = new_ref;
}

static const char* reference_label(refLink *link) {
  int reference_kind = link->reference_kind;
  const char *name = "<font color=\"red\">**unknown**</font>";
  switch (reference_kind) {
    case JVMTI_REFERENCE_CLASS:
      name = "<font color=\"black\">class</font>";
      break;
    case JVMTI_REFERENCE_FIELD:
      name = "<font color=\"black\">field</font>";
      break;
    case JVMTI_REFERENCE_ARRAY_ELEMENT:
      name = "<font color=\"green\">array_element</font>";
      break;
    case JVMTI_REFERENCE_CLASS_LOADER:
      name = "<font color=\"purple\">class_loader</font>";
      break;
    case JVMTI_REFERENCE_SIGNERS:
      name = "<font color=\"purple\">signers</font>";
      break;
    case JVMTI_REFERENCE_PROTECTION_DOMAIN:
      name = "<font color=\"purple\">protection_domain</font>";
      break;
    case JVMTI_REFERENCE_INTERFACE:
      name = "<font color=\"purple\">interface</font>";
      break;
    case JVMTI_REFERENCE_STATIC_FIELD:
      name = "<font color=\"black\">static_field</font>";
      break;
    case HEAP_ROOT_REF_KIND_BASE+JVMTI_HEAP_ROOT_JNI_GLOBAL:
      name = "<font color=\"orange\">root::jni_global</font>";
      break;
    case HEAP_ROOT_REF_KIND_BASE+JVMTI_HEAP_ROOT_SYSTEM_CLASS:
      name = "<font color=\"orange\">root::system_class</font>";
      break;
    case HEAP_ROOT_REF_KIND_BASE+JVMTI_HEAP_ROOT_MONITOR:
      name = "<font color=\"orange\">root::monitor</font>";
      break;
    case HEAP_ROOT_REF_KIND_BASE+JVMTI_HEAP_ROOT_STACK_LOCAL:
      name = "<font color=\"orange\">root::local_var</font>";
      break;
    case HEAP_ROOT_REF_KIND_BASE+JVMTI_HEAP_ROOT_JNI_LOCAL:
      name = "<font color=\"orange\">root::jni_local</font>";
      break;
    case HEAP_ROOT_REF_KIND_BASE+JVMTI_HEAP_ROOT_THREAD:
      name = "<font color=\"orange\">root::thread</font>";
      break;
    case HEAP_ROOT_REF_KIND_BASE+JVMTI_HEAP_ROOT_OTHER:
      name = "<font color=\"orange\">root::other</font>";
      break;
    default:
      printf("Error: Unexpected reference kind %d\n", reference_kind);
      result = STATUS_FAILED;
      break;
  }
  return name;
}

static void walk(MyTag* tag, jint depth, const char* ref_label) {
  static const char* const spaces = ". . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . ";
  static const int len = 86;
  const char *indent = spaces + (len - 2 * depth);

  const MyTag* const ctag = tag->class_tag;
  const char* const cname = ctag != NULL ? ctag->name : "";

  printf("%s", indent);

  if (tag->visited) {
    printf("<a href=\"#%" LL "d\">", tag->sequence);
  } else {
    printf("<a name=\"%" LL "d\">", tag->sequence);
  }
  if (tag->name) {
    printf("<b>%s(%s)</b>", cname, tag->name);
  } else {
    printf("%s(%" LL "d)", cname, tag->sequence);
  }
  printf("</a> -- ");
  printf("%s\n", ref_label);
  if (!tag->visited) {
    refLink *ref;
    tag->visited = JNI_TRUE;
    for (ref = tag->ref; ref; ref = ref->next) {
      walk(ref->tag, depth + 1, reference_label(ref));
    }
  }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_heapref(JavaVM *jvm, char *options, void *reserved) {
  return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT jint JNICALL Agent_OnAttach_heapref(JavaVM *jvm, char *options, void *reserved) {
  return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT jint JNI_OnLoad_heapref(JavaVM *jvm, char *options, void *reserved) {
  return JNI_VERSION_1_8;
}
#endif

jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
  jint res;
  jvmtiError err;

  if (options != NULL && strcmp(options, "printdump") == 0) {
    printdump = JNI_TRUE;
  }

  res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
  if (res != JNI_OK || jvmti == NULL) {
    printf("Wrong result of a valid call to GetEnv!\n");
    return JNI_ERR;
  }

  memset((void*)&jvmti_caps, 0, sizeof(jvmtiCapabilities));
  jvmti_caps.can_tag_objects = 1;
  err = jvmti->AddCapabilities(&jvmti_caps);
  if (err != JVMTI_ERROR_NONE) {
    printf("Error (AddCapabilities): %s (%d)\n", TranslateError(err), err);
    return JNI_ERR;
  }

  return JNI_OK;
}

jvmtiIterationControl JNICALL
heapMarkCallback(jlong class_tag, jlong size, jlong* tag_ptr, void* user_data) {
  const MyTag* const tag = newTag(rmark, (const MyTag*)(intptr_t)class_tag, size, NULL);
  *tag_ptr = (intptr_t)tag;

  if (user_data != &dummy_user_data && user_data_error_flag == JNI_FALSE) {
    user_data_error_flag = JNI_TRUE;
    printf("Error (heapMarkCallback): unexpected value of user_data\n");
    result = STATUS_FAILED;
  }
  return JVMTI_ITERATION_CONTINUE;
}

jvmtiIterationControl JNICALL
heapRootCallback(jvmtiHeapRootKind root_kind,
                 jlong class_tag, jlong size,
                 jlong* tag_ptr, void* user_data) {
  refKind kind = rother;

  if (0 == *tag_ptr) {
    /* new tag */
    MyTag* tag = newTag(kind, (MyTag*)(intptr_t)class_tag, size, NULL);
    addRef(fakeRoot, HEAP_ROOT_REF_KIND_BASE+root_kind, tag);
    *tag_ptr = (intptr_t)tag;
  } else {
    /* existing tag */
    addRef(fakeRoot, HEAP_ROOT_REF_KIND_BASE+root_kind, (MyTag*)(intptr_t)*tag_ptr);
  }

  if (user_data != &dummy_user_data && user_data_error_flag == JNI_FALSE) {
    user_data_error_flag = JNI_TRUE;
    printf("Error (heapRootCallback): unexpected value of user_data\n");
    result = STATUS_FAILED;
  }
  return JVMTI_ITERATION_CONTINUE;
}

jvmtiIterationControl JNICALL
stackReferenceCallback(jvmtiHeapRootKind root_kind,
                       jlong class_tag, jlong size,
                       jlong* tag_ptr, jlong thread_tag,
                       jint depth, jmethodID method,
                       jint slot, void* user_data) {
  refKind kind = rother;

  if (0 == *tag_ptr) {
    /* new tag */
    MyTag* tag = newTag(kind, (MyTag*)(intptr_t)class_tag, size, NULL);
    addRef(fakeRoot, HEAP_ROOT_REF_KIND_BASE+root_kind, tag);
    *tag_ptr = (intptr_t)tag;
  } else {
    /* existing tag */
    addRef(fakeRoot, HEAP_ROOT_REF_KIND_BASE+root_kind, (MyTag*)(intptr_t)*tag_ptr);
  }
  if (user_data != &dummy_user_data && user_data_error_flag == JNI_FALSE) {
    user_data_error_flag = JNI_TRUE;
    printf("Error (stackReferenceCallback): unexpected value of user_data\n");
    result = STATUS_FAILED;
  }
  return JVMTI_ITERATION_CONTINUE;
}

jvmtiIterationControl JNICALL
objectReferenceCallback(jvmtiObjectReferenceKind reference_kind,
                        jlong class_tag, jlong size,
                        jlong* tag_ptr, jlong referrer_tag,
                        jint referrer_index, void* user_data) {
  refKind kind = rother;
  MyTag* referrer = NULL;

  if (0 == referrer_tag) {
    referrer = missed;
  } else {
    referrer = (MyTag *)(intptr_t)referrer_tag;
  }

  if (0 == *tag_ptr) {
    /* new tag */
    MyTag* tag = newTag(kind, (MyTag*)(intptr_t)class_tag, size, NULL);
    addRef(referrer, reference_kind, tag);
    *tag_ptr = (intptr_t) tag;
  } else {
    /* existing tag */
    MyTag* tag = (MyTag*)(intptr_t)*tag_ptr;
    addRef(referrer, reference_kind, tag);
  }
  if (user_data != &dummy_user_data && user_data_error_flag == JNI_FALSE) {
    user_data_error_flag = JNI_TRUE;
    printf("Error (objectReferenceCallback): unexpected value of user_data\n");
    result = STATUS_FAILED;
  }
  return JVMTI_ITERATION_CONTINUE;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_heapref_check(JNIEnv *env, jclass cls) {
  jvmtiError err;
  jclass *classes;
  jint classCount = 0;
  jthread *threads;
  jint threadCount = 0;
  jint i;

  if (jvmti == NULL) {
    printf("JVMTI client was not properly loaded!\n");
    return STATUS_FAILED;
  }

  fakeRoot = newTag(rother, (const MyTag *)NULL, 0, "FAKE_ROOT");
  missed = newTag(rother, (const MyTag *)NULL, 0, "MISSED");

  if (env->PushLocalFrame(500) != 0) {
    printf("Error (PushLocalFrame): failed\n");
    result = STATUS_FAILED;
  }

  err = jvmti->GetLoadedClasses(&classCount, &classes);
  if (err != JVMTI_ERROR_NONE) {
    printf("Error (GetLoadedClasses): %s (%d)\n", TranslateError(err), err);
    result = STATUS_FAILED;
  }

  for (i = 0; i < classCount; ++i) {
    char *classSig;
    jclass k = classes[i];
    err = jvmti->GetClassSignature(k, &classSig, NULL);
    if (err != JVMTI_ERROR_NONE) {
      printf("Error (getClassSignature): %s (%d)\n", TranslateError(err), err);
      result = STATUS_FAILED;
    } else {
      char* slash = strrchr(classSig, '/');
      const size_t len = strlen(classSig);
      if (classSig[len-1] == ';') {
        classSig[len-1] = 0;
      }
      if (*classSig == 'L' && slash != NULL) {
        classSig = slash + 1;
      }
      setTag(env, k, rclass, (const char*)classSig);
    }
  }

  err = jvmti->GetAllThreads(&threadCount, &threads);
  if (err != JVMTI_ERROR_NONE) {
    printf("Error (GetAllThreads): %s (%d)\n", TranslateError(err), err);
    result = STATUS_FAILED;
  }

  for (i = 0; i < threadCount; ++i) {
    jvmtiThreadInfo info;
    jthread t = threads[i];
    err = jvmti->GetThreadInfo(t, &info);
    if (err != JVMTI_ERROR_NONE) {
      printf("Error (GetThreadInfo): %s (%d)\n", TranslateError(err), err);
      result = STATUS_FAILED;
    } else {
      setTag(env, t, rthread, (const char*)info.name);
    }
  }

  env->PopLocalFrame(NULL);

  user_data_error_flag = JNI_FALSE;
  err = jvmti->IterateOverHeap(
                                  JVMTI_HEAP_OBJECT_UNTAGGED,
                                  heapMarkCallback,
                                  &dummy_user_data);
  if (err != JVMTI_ERROR_NONE) {
    printf("Error (IterateOverHeap): %s (%d)\n", TranslateError(err), err);
    result = STATUS_FAILED;
  }

  user_data_error_flag = JNI_FALSE;
  err = jvmti->IterateOverReachableObjects(
                                              heapRootCallback,
                                              stackReferenceCallback,
                                              objectReferenceCallback,
                                              &dummy_user_data);
  if (err != JVMTI_ERROR_NONE) {
    printf("Error (IterateOverReachableObjects): %s (%d)\n", TranslateError(err), err);
    result = STATUS_FAILED;
  }

  if (printdump == JNI_TRUE) {
    printf("<html><head><title>Heap Dump</title></head><body><pre>\n");
    walk(fakeRoot, 0, "roots");
    printf("\n------------------- MISSED ------------------\n\n");
    walk(missed, 0, "missed");
    printf("</pre></body></html>\n");
  }

  return result;
}

}
