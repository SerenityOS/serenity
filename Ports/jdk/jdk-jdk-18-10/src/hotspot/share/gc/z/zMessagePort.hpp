/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZMESSAGEPORT_HPP
#define SHARE_GC_Z_ZMESSAGEPORT_HPP

#include "gc/z/zFuture.hpp"
#include "gc/z/zList.hpp"
#include "runtime/mutex.hpp"

template <typename T> class ZMessageRequest;

template <typename T>
class ZMessagePort {
private:
  typedef ZMessageRequest<T> Request;

  mutable Monitor _monitor;
  bool            _has_message;
  T               _message;
  uint64_t        _seqnum;
  ZList<Request>  _queue;

public:
  ZMessagePort();

  bool is_busy() const;

  void send_sync(const T& message);
  void send_async(const T& message);

  T receive();
  void ack();
};

class ZRendezvousPort {
private:
  ZMessagePort<bool> _port;

public:
  void signal();
  void wait();
  void ack();
};

#endif // SHARE_GC_Z_ZMESSAGEPORT_HPP
