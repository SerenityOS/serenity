/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "prims/jvmtiExport.hpp"
#include "prims/jvmtiExtensions.hpp"

// the list of extension functions
GrowableArray<jvmtiExtensionFunctionInfo*>* JvmtiExtensions::_ext_functions;

// the list of extension events
GrowableArray<jvmtiExtensionEventInfo*>* JvmtiExtensions::_ext_events;


// extension function
static jvmtiError JNICALL IsClassUnloadingEnabled(const jvmtiEnv* env, ...) {
  jboolean* enabled = NULL;
  va_list ap;

  va_start(ap, env);
  enabled = va_arg(ap, jboolean *);
  va_end(ap);

  if (enabled == NULL) {
    return JVMTI_ERROR_NULL_POINTER;
  }
  *enabled = (jboolean)ClassUnloading;
  return JVMTI_ERROR_NONE;
}

// register extension functions and events. In this implementation we
// have a single extension function (to prove the API) that tests if class
// unloading is enabled or disabled. We also have a single extension event
// EXT_EVENT_CLASS_UNLOAD which is used to provide the JVMDI_EVENT_CLASS_UNLOAD
// event. The function and the event are registered here.
//
void JvmtiExtensions::register_extensions() {
  _ext_functions = new (ResourceObj::C_HEAP, mtServiceability) GrowableArray<jvmtiExtensionFunctionInfo*>(1, mtServiceability);
  _ext_events = new (ResourceObj::C_HEAP, mtServiceability) GrowableArray<jvmtiExtensionEventInfo*>(1, mtServiceability);

  // register our extension function
  static jvmtiParamInfo func_params[] = {
    { (char*)"IsClassUnloadingEnabled", JVMTI_KIND_OUT,  JVMTI_TYPE_JBOOLEAN, JNI_FALSE }
  };
  static jvmtiExtensionFunctionInfo ext_func = {
    (jvmtiExtensionFunction)IsClassUnloadingEnabled,
    (char*)"com.sun.hotspot.functions.IsClassUnloadingEnabled",
    (char*)"Tell if class unloading is enabled (-noclassgc)",
    sizeof(func_params)/sizeof(func_params[0]),
    func_params,
    0,              // no non-universal errors
    NULL
  };
  _ext_functions->append(&ext_func);

  // register our extension event

  static jvmtiParamInfo event_params[] = {
    { (char*)"JNI Environment", JVMTI_KIND_IN_PTR, JVMTI_TYPE_JNIENV, JNI_FALSE },
    { (char*)"Class", JVMTI_KIND_IN_PTR, JVMTI_TYPE_CCHAR, JNI_FALSE }
  };
  static jvmtiExtensionEventInfo ext_event = {
    EXT_EVENT_CLASS_UNLOAD,
    (char*)"com.sun.hotspot.events.ClassUnload",
    (char*)"CLASS_UNLOAD event",
    sizeof(event_params)/sizeof(event_params[0]),
    event_params
  };
  _ext_events->append(&ext_event);
}


// return the list of extension functions

jvmtiError JvmtiExtensions::get_functions(JvmtiEnv* env,
                                          jint* extension_count_ptr,
                                          jvmtiExtensionFunctionInfo** extensions)
{
  guarantee(_ext_functions != NULL, "registration not done");

  ResourceTracker rt(env);

  jvmtiExtensionFunctionInfo* ext_funcs;
  jvmtiError err = rt.allocate(_ext_functions->length() *
                               sizeof(jvmtiExtensionFunctionInfo),
                               (unsigned char**)&ext_funcs);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }

  for (int i=0; i<_ext_functions->length(); i++ ) {
    ext_funcs[i].func = _ext_functions->at(i)->func;

    char *id = _ext_functions->at(i)->id;
    err = rt.allocate(strlen(id)+1, (unsigned char**)&(ext_funcs[i].id));
    if (err != JVMTI_ERROR_NONE) {
      return err;
    }
    strcpy(ext_funcs[i].id, id);

    char *desc = _ext_functions->at(i)->short_description;
    err = rt.allocate(strlen(desc)+1,
                      (unsigned char**)&(ext_funcs[i].short_description));
    if (err != JVMTI_ERROR_NONE) {
      return err;
    }
    strcpy(ext_funcs[i].short_description, desc);

    // params

    jint param_count = _ext_functions->at(i)->param_count;

    ext_funcs[i].param_count = param_count;
    if (param_count == 0) {
      ext_funcs[i].params = NULL;
    } else {
      err = rt.allocate(param_count*sizeof(jvmtiParamInfo),
                        (unsigned char**)&(ext_funcs[i].params));
      if (err != JVMTI_ERROR_NONE) {
        return err;
      }
      jvmtiParamInfo* src_params = _ext_functions->at(i)->params;
      jvmtiParamInfo* dst_params = ext_funcs[i].params;

      for (int j=0; j<param_count; j++) {
        err = rt.allocate(strlen(src_params[j].name)+1,
                          (unsigned char**)&(dst_params[j].name));
        if (err != JVMTI_ERROR_NONE) {
          return err;
        }
        strcpy(dst_params[j].name, src_params[j].name);

        dst_params[j].kind = src_params[j].kind;
        dst_params[j].base_type = src_params[j].base_type;
        dst_params[j].null_ok = src_params[j].null_ok;
      }
    }

    // errors

    jint error_count = _ext_functions->at(i)->error_count;
    ext_funcs[i].error_count = error_count;
    if (error_count == 0) {
      ext_funcs[i].errors = NULL;
    } else {
      err = rt.allocate(error_count*sizeof(jvmtiError),
                        (unsigned char**)&(ext_funcs[i].errors));
      if (err != JVMTI_ERROR_NONE) {
        return err;
      }
      memcpy(ext_funcs[i].errors, _ext_functions->at(i)->errors,
             error_count*sizeof(jvmtiError));
    }
  }

  *extension_count_ptr = _ext_functions->length();
  *extensions = ext_funcs;
  return JVMTI_ERROR_NONE;
}


// return the list of extension events

jvmtiError JvmtiExtensions::get_events(JvmtiEnv* env,
                                       jint* extension_count_ptr,
                                       jvmtiExtensionEventInfo** extensions)
{
  guarantee(_ext_events != NULL, "registration not done");

  ResourceTracker rt(env);

  jvmtiExtensionEventInfo* ext_events;
  jvmtiError err = rt.allocate(_ext_events->length() * sizeof(jvmtiExtensionEventInfo),
                               (unsigned char**)&ext_events);
  if (err != JVMTI_ERROR_NONE) {
    return err;
  }

  for (int i=0; i<_ext_events->length(); i++ ) {
    ext_events[i].extension_event_index = _ext_events->at(i)->extension_event_index;

    char *id = _ext_events->at(i)->id;
    err = rt.allocate(strlen(id)+1, (unsigned char**)&(ext_events[i].id));
    if (err != JVMTI_ERROR_NONE) {
      return err;
    }
    strcpy(ext_events[i].id, id);

    char *desc = _ext_events->at(i)->short_description;
    err = rt.allocate(strlen(desc)+1,
                      (unsigned char**)&(ext_events[i].short_description));
    if (err != JVMTI_ERROR_NONE) {
      return err;
    }
    strcpy(ext_events[i].short_description, desc);

    // params

    jint param_count = _ext_events->at(i)->param_count;

    ext_events[i].param_count = param_count;
    if (param_count == 0) {
      ext_events[i].params = NULL;
    } else {
      err = rt.allocate(param_count*sizeof(jvmtiParamInfo),
                        (unsigned char**)&(ext_events[i].params));
      if (err != JVMTI_ERROR_NONE) {
        return err;
      }
      jvmtiParamInfo* src_params = _ext_events->at(i)->params;
      jvmtiParamInfo* dst_params = ext_events[i].params;

      for (int j=0; j<param_count; j++) {
        err = rt.allocate(strlen(src_params[j].name)+1,
                          (unsigned char**)&(dst_params[j].name));
        if (err != JVMTI_ERROR_NONE) {
          return err;
        }
        strcpy(dst_params[j].name, src_params[j].name);

        dst_params[j].kind = src_params[j].kind;
        dst_params[j].base_type = src_params[j].base_type;
        dst_params[j].null_ok = src_params[j].null_ok;
      }
    }
  }

  *extension_count_ptr = _ext_events->length();
  *extensions = ext_events;
  return JVMTI_ERROR_NONE;
}

// set callback for an extension event and enable/disable it.

jvmtiError JvmtiExtensions::set_event_callback(JvmtiEnv* env,
                                               jint extension_event_index,
                                               jvmtiExtensionEvent callback)
{
  guarantee(_ext_events != NULL, "registration not done");

  jvmtiExtensionEventInfo* event = NULL;

  // if there are extension events registered then validate that the
  // extension_event_index matches one of the registered events.
  if (_ext_events != NULL) {
    for (int i=0; i<_ext_events->length(); i++ ) {
      if (_ext_events->at(i)->extension_event_index == extension_event_index) {
         event = _ext_events->at(i);
         break;
      }
    }
  }

  // invalid event index
  if (event == NULL) {
    return JVMTI_ERROR_ILLEGAL_ARGUMENT;
  }

  JvmtiEventController::set_extension_event_callback(env, extension_event_index,
                                                     callback);

  return JVMTI_ERROR_NONE;
}
