/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/classLoaderDataGraph.hpp"
#include "classfile/dictionary.hpp"
#include "classfile/javaClasses.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "memory/universe.hpp"
#include "oops/klass.inline.hpp"
#include "prims/jvmtiGetLoadedClasses.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/thread.hpp"
#include "utilities/stack.inline.hpp"

// The closure for GetLoadedClasses
class LoadedClassesClosure : public KlassClosure {
private:
  Stack<jclass, mtInternal> _classStack;
  JvmtiEnv* _env;
  Thread*   _cur_thread;
  bool      _dictionary_walk;

  int extract(jclass* result_list) {
    // The size of the Stack will be 0 after extract, so get it here
    int count = (int)_classStack.size();
    int i = count;

    // Pop all jclasses, fill backwards
    while (!_classStack.is_empty()) {
      result_list[--i] = _classStack.pop();
    }

    // Return the number of elements written
    return count;
  }

  // Return current size of the Stack
  int get_count() {
    return (int)_classStack.size();
  }

public:
  LoadedClassesClosure(JvmtiEnv* env, bool dictionary_walk) :
      _env(env),
      _cur_thread(Thread::current()),
      _dictionary_walk(dictionary_walk) {
  }

  void do_klass(Klass* k) {
    // Collect all jclasses
    _classStack.push((jclass) _env->jni_reference(Handle(_cur_thread, k->java_mirror())));
    if (_dictionary_walk) {
      // Collect array classes this way when walking the dictionary (because array classes are
      // not in the dictionary).
      for (Klass* l = k->array_klass_or_null(); l != NULL; l = l->array_klass_or_null()) {
        _classStack.push((jclass) _env->jni_reference(Handle(_cur_thread, l->java_mirror())));
      }
    }
  }

  jvmtiError get_result(JvmtiEnv *env, jint* classCountPtr, jclass** classesPtr) {
    // Return results by extracting the collected contents into a list
    // allocated via JvmtiEnv
    jclass* result_list;
    jvmtiError error = env->Allocate(get_count() * sizeof(jclass),
                               (unsigned char**)&result_list);

    if (error == JVMTI_ERROR_NONE) {
      int count = extract(result_list);
      *classCountPtr = count;
      *classesPtr = result_list;
    }
    return error;
  }
};

jvmtiError
JvmtiGetLoadedClasses::getLoadedClasses(JvmtiEnv *env, jint* classCountPtr, jclass** classesPtr) {

  LoadedClassesClosure closure(env, false);
  {
    // To get a consistent list of classes we need MultiArray_lock to ensure
    // array classes aren't created.
    MutexLocker ma(MultiArray_lock);

    // Iterate through all classes in ClassLoaderDataGraph
    // and collect them using the LoadedClassesClosure
    MutexLocker mcld(ClassLoaderDataGraph_lock);
    ClassLoaderDataGraph::loaded_classes_do(&closure);
  }

  return closure.get_result(env, classCountPtr, classesPtr);
}

jvmtiError
JvmtiGetLoadedClasses::getClassLoaderClasses(JvmtiEnv *env, jobject initiatingLoader,
                                             jint* classCountPtr, jclass** classesPtr) {

  LoadedClassesClosure closure(env, true);
  {
    // To get a consistent list of classes we need MultiArray_lock to ensure
    // array classes aren't created during this walk.
    MutexLocker ma(MultiArray_lock);
    MutexLocker sd(SystemDictionary_lock);
    oop loader = JNIHandles::resolve(initiatingLoader);
    // All classes loaded from this loader as initiating loader are
    // requested, so only need to walk this loader's ClassLoaderData
    // dictionary, or the NULL ClassLoaderData dictionary for bootstrap loader.
    if (loader != NULL) {
      ClassLoaderData* data = java_lang_ClassLoader::loader_data_acquire(loader);
      // ClassLoader may not be used yet for loading.
      if (data != NULL && data->dictionary() != NULL) {
        data->dictionary()->all_entries_do(&closure);
      }
    } else {
      ClassLoaderData::the_null_class_loader_data()->dictionary()->all_entries_do(&closure);
    }
    // Get basic arrays for all loaders.
    Universe::basic_type_classes_do(&closure);
  }

  return closure.get_result(env, classCountPtr, classesPtr);
}
