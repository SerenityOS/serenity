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

#ifndef SHARE_PRIMS_JVMTIEVENTCONTROLLER_HPP
#define SHARE_PRIMS_JVMTIEVENTCONTROLLER_HPP

#include "jvmtifiles/jvmti.h"
#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"

// forward declaration
class JvmtiEventControllerPrivate;
class JvmtiEventController;
class JvmtiEnvThreadState;
class JvmtiFramePop;
class JvmtiEnvBase;


// Extension event support
//
// jvmtiExtEvent is the extensions equivalent of jvmtiEvent
// jvmtiExtCallbacks is the extensions equivalent of jvmtiEventCallbacks

// Extension events start JVMTI_MIN_EVENT_TYPE_VAL-1 and work towards 0.
typedef enum {
  EXT_EVENT_CLASS_UNLOAD = JVMTI_MIN_EVENT_TYPE_VAL-1,
  EXT_MIN_EVENT_TYPE_VAL = EXT_EVENT_CLASS_UNLOAD,
  EXT_MAX_EVENT_TYPE_VAL = EXT_EVENT_CLASS_UNLOAD
} jvmtiExtEvent;

typedef struct {
  jvmtiExtensionEvent ClassUnload;
} jvmtiExtEventCallbacks;


// The complete range of events is EXT_MIN_EVENT_TYPE_VAL to
// JVMTI_MAX_EVENT_TYPE_VAL (inclusive and contiguous).
const int TOTAL_MIN_EVENT_TYPE_VAL = EXT_MIN_EVENT_TYPE_VAL;
const int TOTAL_MAX_EVENT_TYPE_VAL = JVMTI_MAX_EVENT_TYPE_VAL;


///////////////////////////////////////////////////////////////
//
// JvmtiEventEnabled
//
// Utility class
//
// A boolean array indexed by event_type, used as an internal
// data structure to track what JVMTI event types are enabled.
// Used for user set enabling and disabling (globally and on a
// per thread basis), and for computed merges across environments,
// threads and the VM as a whole.
//
// for inlines see jvmtiEventController_inline.hpp
//

class JvmtiEventEnabled {
private:
  friend class JvmtiEventControllerPrivate;
  jlong _enabled_bits;
#ifndef PRODUCT
  enum {
    JEE_INIT_GUARD = 0xEAD0
  } _init_guard;
#endif
  static jlong bit_for(jvmtiEvent event_type);
  jlong get_bits();
  void set_bits(jlong bits);
public:
  JvmtiEventEnabled();
  void clear();
  bool is_enabled(jvmtiEvent event_type);
  void set_enabled(jvmtiEvent event_type, bool enabled);
};


///////////////////////////////////////////////////////////////
//
// JvmtiEnvThreadEventEnable
//
// JvmtiEventController data specific to a particular environment and thread.
//
// for inlines see jvmtiEventController_inline.hpp
//

class JvmtiEnvThreadEventEnable {
private:
  friend class JvmtiEventControllerPrivate;
  JvmtiEventEnabled _event_user_enabled;
  JvmtiEventEnabled _event_enabled;

public:
  JvmtiEnvThreadEventEnable();
  ~JvmtiEnvThreadEventEnable();
  bool is_enabled(jvmtiEvent event_type);
  void set_user_enabled(jvmtiEvent event_type, bool enabled);
};


///////////////////////////////////////////////////////////////
//
// JvmtiThreadEventEnable
//
// JvmtiEventController data specific to a particular thread.
//
// for inlines see jvmtiEventController_inline.hpp
//

class JvmtiThreadEventEnable {
private:
  friend class JvmtiEventControllerPrivate;
  JvmtiEventEnabled _event_enabled;

public:
  JvmtiThreadEventEnable();
  ~JvmtiThreadEventEnable();
  bool is_enabled(jvmtiEvent event_type);
};


///////////////////////////////////////////////////////////////
//
// JvmtiEnvEventEnable
//
// JvmtiEventController data specific to a particular environment.
//
// for inlines see jvmtiEventController_inline.hpp
//

class JvmtiEnvEventEnable {
private:
  friend class JvmtiEventControllerPrivate;

  // user set global event enablement indexed by jvmtiEvent
  JvmtiEventEnabled _event_user_enabled;

  // this flag indicates the presence (true) or absence (false) of event callbacks
  // it is indexed by jvmtiEvent
  JvmtiEventEnabled _event_callback_enabled;

  // indexed by jvmtiEvent true if enabled globally or on any thread.
  // True only if there is a callback for it.
  JvmtiEventEnabled _event_enabled;

public:
  JvmtiEnvEventEnable();
  ~JvmtiEnvEventEnable();
  bool is_enabled(jvmtiEvent event_type);
  void set_user_enabled(jvmtiEvent event_type, bool enabled);
};


///////////////////////////////////////////////////////////////
//
// JvmtiEventController
//
// The class is the access point for all actions that change
// which events are active, this include:
//      enabling and disabling events
//      changing the callbacks/eventhook (they may be null)
//      setting and clearing field watchpoints
//      setting frame pops
//      encountering frame pops
//
// for inlines see jvmtiEventController_inline.hpp
//

class JvmtiEventController : AllStatic {
private:
  friend class JvmtiEventControllerPrivate;

  // for all environments, global array indexed by jvmtiEvent
  static JvmtiEventEnabled _universal_global_event_enabled;

public:
  static bool is_enabled(jvmtiEvent event_type);

  // events that can ONLY be enabled/disabled globally (can't toggle on individual threads).
  static bool is_global_event(jvmtiEvent event_type);

  // is the event_type valid?
  // to do: check against valid event array
  static bool is_valid_event_type(jvmtiEvent event_type) {
    return ((int)event_type >= TOTAL_MIN_EVENT_TYPE_VAL)
        && ((int)event_type <= TOTAL_MAX_EVENT_TYPE_VAL);
  }

  // Use (thread == NULL) to enable/disable an event globally.
  // Use (thread != NULL) to enable/disable an event for a particular thread.
  // thread is ignored for events that can only be specified globally
  static void set_user_enabled(JvmtiEnvBase *env, JavaThread *thread,
                               jvmtiEvent event_type, bool enabled);

  // Setting callbacks changes computed enablement and must be done
  // at a safepoint otherwise a NULL callback could be attempted
  static void set_event_callbacks(JvmtiEnvBase *env,
                                  const jvmtiEventCallbacks* callbacks,
                                  jint size_of_callbacks);

  // Sets the callback function for a single extension event and enables
  // (or disables it).
  static void set_extension_event_callback(JvmtiEnvBase* env,
                                           jint extension_event_index,
                                           jvmtiExtensionEvent callback);

  static void set_frame_pop(JvmtiEnvThreadState *env_thread, JvmtiFramePop fpop);
  static void clear_frame_pop(JvmtiEnvThreadState *env_thread, JvmtiFramePop fpop);

  static void change_field_watch(jvmtiEvent event_type, bool added);

  static void thread_started(JavaThread *thread);
  static void thread_ended(JavaThread *thread);

  static void env_initialize(JvmtiEnvBase *env);
  static void env_dispose(JvmtiEnvBase *env);

  static void vm_start();
  static void vm_init();
  static void vm_death();
};

#endif // SHARE_PRIMS_JVMTIEVENTCONTROLLER_HPP
