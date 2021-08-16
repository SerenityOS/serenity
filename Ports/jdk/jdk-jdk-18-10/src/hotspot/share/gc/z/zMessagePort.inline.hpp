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

#ifndef SHARE_GC_Z_ZMESSAGEPORT_INLINE_HPP
#define SHARE_GC_Z_ZMESSAGEPORT_INLINE_HPP

#include "gc/z/zMessagePort.hpp"

#include "gc/z/zFuture.inline.hpp"
#include "gc/z/zList.inline.hpp"
#include "runtime/mutexLocker.hpp"

template <typename T>
class ZMessageRequest : public StackObj {
  friend class ZList<ZMessageRequest>;

private:
  T                          _message;
  uint64_t                   _seqnum;
  ZFuture<T>                 _result;
  ZListNode<ZMessageRequest> _node;

public:
  void initialize(T message, uint64_t seqnum) {
    _message = message;
    _seqnum = seqnum;
  }

  T message() const {
    return _message;
  }

  uint64_t seqnum() const {
    return _seqnum;
  }

  void wait() {
    const T message = _result.get();
    assert(message == _message, "Message mismatch");
  }

  void satisfy(T message) {
    _result.set(message);
  }
};

template <typename T>
inline ZMessagePort<T>::ZMessagePort() :
    _monitor(Monitor::leaf,
             "ZMessagePort",
             Monitor::_allow_vm_block_flag,
             Monitor::_safepoint_check_never),
    _has_message(false),
    _seqnum(0),
    _queue() {}

template <typename T>
inline bool ZMessagePort<T>::is_busy() const {
  MonitorLocker ml(&_monitor, Monitor::_no_safepoint_check_flag);
  return _has_message;
}

template <typename T>
inline void ZMessagePort<T>::send_sync(const T& message) {
  Request request;

  {
    // Enqueue message
    MonitorLocker ml(&_monitor, Monitor::_no_safepoint_check_flag);
    request.initialize(message, _seqnum);
    _queue.insert_last(&request);
    ml.notify();
  }

  // Wait for completion
  request.wait();

  {
    // Guard deletion of underlying semaphore. This is a workaround for a
    // bug in sem_post() in glibc < 2.21, where it's not safe to destroy
    // the semaphore immediately after returning from sem_wait(). The
    // reason is that sem_post() can touch the semaphore after a waiting
    // thread have returned from sem_wait(). To avoid this race we are
    // forcing the waiting thread to acquire/release the lock held by the
    // posting thread. https://sourceware.org/bugzilla/show_bug.cgi?id=12674
    MonitorLocker ml(&_monitor, Monitor::_no_safepoint_check_flag);
  }
}

template <typename T>
inline void ZMessagePort<T>::send_async(const T& message) {
  MonitorLocker ml(&_monitor, Monitor::_no_safepoint_check_flag);
  if (!_has_message) {
    // Post message
    _message = message;
    _has_message = true;
    ml.notify();
  }
}

template <typename T>
inline T ZMessagePort<T>::receive() {
  MonitorLocker ml(&_monitor, Monitor::_no_safepoint_check_flag);

  // Wait for message
  while (!_has_message && _queue.is_empty()) {
    ml.wait();
  }

  // Increment request sequence number
  _seqnum++;

  if (!_has_message) {
    // Message available in the queue
    _message = _queue.first()->message();
    _has_message = true;
  }

  return _message;
}

template <typename T>
inline void ZMessagePort<T>::ack() {
  MonitorLocker ml(&_monitor, Monitor::_no_safepoint_check_flag);

  if (!_has_message) {
    // Nothing to ack
    return;
  }

  // Satisfy requests (and duplicates) in queue
  ZListIterator<Request> iter(&_queue);
  for (Request* request; iter.next(&request);) {
    if (request->message() == _message && request->seqnum() < _seqnum) {
      // Dequeue and satisfy request. Note that the dequeue operation must
      // happen first, since the request will immediately be deallocated
      // once it has been satisfied.
      _queue.remove(request);
      request->satisfy(_message);
    }
  }

  if (_queue.is_empty()) {
    // Queue is empty
    _has_message = false;
  } else {
    // Post first message in queue
    _message = _queue.first()->message();
  }
}

inline void ZRendezvousPort::signal() {
  _port.send_sync(true /* ignored */);
}

inline void ZRendezvousPort::wait() {
  _port.receive();
}

inline void ZRendezvousPort::ack() {
  _port.ack();
}

#endif // SHARE_GC_Z_ZMESSAGEPORT_INLINE_HPP
