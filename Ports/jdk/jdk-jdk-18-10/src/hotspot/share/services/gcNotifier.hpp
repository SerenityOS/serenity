/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_SERVICES_GCNOTIFIER_HPP
#define SHARE_SERVICES_GCNOTIFIER_HPP

#include "memory/allocation.hpp"
#include "services/memoryPool.hpp"
#include "services/memoryService.hpp"
#include "services/memoryManager.hpp"

class GCNotificationRequest : public CHeapObj<mtInternal> {
  friend class GCNotifier;
  GCNotificationRequest *next;
  jlong timestamp;
  GCMemoryManager *gcManager;
  const char *gcAction;
  const char *gcCause;
  GCStatInfo *gcStatInfo;
public:
  GCNotificationRequest(jlong ts, GCMemoryManager *manager, const char*action, const char *cause,GCStatInfo *info) {
    next = NULL;
    timestamp = ts;
    gcManager = manager;
    gcAction = action;
    gcCause = cause;
    gcStatInfo = info;
  }

  ~GCNotificationRequest() {
    delete gcStatInfo;
  }
};

class GCNotifier : public AllStatic {
  friend class ServiceThread;
private:
  static GCNotificationRequest *first_request;
  static GCNotificationRequest *last_request;
  static void addRequest(GCNotificationRequest *request);
  static GCNotificationRequest *getRequest();
  static void sendNotificationInternal(TRAPS);
public:
  static void pushNotification(GCMemoryManager *manager, const char *action, const char *cause);
  static bool has_event();
  static void sendNotification(TRAPS);
};

#endif // SHARE_SERVICES_GCNOTIFIER_HPP
