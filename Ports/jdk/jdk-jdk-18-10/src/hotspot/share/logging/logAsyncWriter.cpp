/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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
#include "logging/logAsyncWriter.hpp"
#include "logging/logConfiguration.hpp"
#include "logging/logFileOutput.hpp"
#include "logging/logHandle.hpp"
#include "runtime/atomic.hpp"
#include "runtime/os.inline.hpp"

class AsyncLogWriter::AsyncLogLocker : public StackObj {
 public:
  AsyncLogLocker() {
    assert(_instance != nullptr, "AsyncLogWriter::_lock is unavailable");
    _instance->_lock.lock();
  }

  ~AsyncLogLocker() {
    _instance->_lock.unlock();
  }
};

void AsyncLogWriter::enqueue_locked(const AsyncLogMessage& msg) {
  if (_buffer.size() >= _buffer_max_size) {
    bool p_created;
    uint32_t* counter = _stats.put_if_absent(msg.output(), 0, &p_created);
    *counter = *counter + 1;
    // drop the enqueueing message.
    os::free(msg.message());
    return;
  }

  _buffer.push_back(msg);
  _data_available = true;
  _lock.notify();
}

void AsyncLogWriter::enqueue(LogFileOutput& output, const LogDecorations& decorations, const char* msg) {
  AsyncLogMessage m(&output, decorations, os::strdup(msg));

  { // critical area
    AsyncLogLocker locker;
    enqueue_locked(m);
  }
}

// LogMessageBuffer consists of a multiple-part/multiple-line messsage.
// The lock here guarantees its integrity.
void AsyncLogWriter::enqueue(LogFileOutput& output, LogMessageBuffer::Iterator msg_iterator) {
  AsyncLogLocker locker;

  for (; !msg_iterator.is_at_end(); msg_iterator++) {
    AsyncLogMessage m(&output, msg_iterator.decorations(), os::strdup(msg_iterator.message()));
    enqueue_locked(m);
  }
}

AsyncLogWriter::AsyncLogWriter()
  : _flush_sem(0), _lock(), _data_available(false),
    _initialized(false),
    _stats() {
  if (os::create_thread(this, os::asynclog_thread)) {
    _initialized = true;
  } else {
    log_warning(logging, thread)("AsyncLogging failed to create thread. Falling back to synchronous logging.");
  }

  log_info(logging)("The maximum entries of AsyncLogBuffer: " SIZE_FORMAT ", estimated memory use: " SIZE_FORMAT " bytes",
                    _buffer_max_size, AsyncLogBufferSize);
}

class AsyncLogMapIterator {
  AsyncLogBuffer& _logs;

 public:
  AsyncLogMapIterator(AsyncLogBuffer& logs) :_logs(logs) {}
  bool do_entry(LogFileOutput* output, uint32_t& counter) {
    using none = LogTagSetMapping<LogTag::__NO_TAG>;

    if (counter > 0) {
      LogDecorations decorations(LogLevel::Warning, none::tagset(), LogDecorators::All);
      stringStream ss;
      ss.print(UINT32_FORMAT_W(6) " messages dropped due to async logging", counter);
      AsyncLogMessage msg(output, decorations, ss.as_string(true /*c_heap*/));
      _logs.push_back(msg);
      counter = 0;
    }

    return true;
  }
};

void AsyncLogWriter::write() {
  // Use kind of copy-and-swap idiom here.
  // Empty 'logs' swaps the content with _buffer.
  // Along with logs destruction, all processed messages are deleted.
  //
  // The operation 'pop_all()' is done in O(1). All I/O jobs are then performed without
  // lock protection. This guarantees I/O jobs don't block logsites.
  AsyncLogBuffer logs;

  { // critical region
    AsyncLogLocker locker;

    _buffer.pop_all(&logs);
    // append meta-messages of dropped counters
    AsyncLogMapIterator dropped_counters_iter(logs);
    _stats.iterate(&dropped_counters_iter);
    _data_available = false;
  }

  LinkedListIterator<AsyncLogMessage> it(logs.head());

  int req = 0;
  while (!it.is_empty()) {
    AsyncLogMessage* e = it.next();
    char* msg = e->message();

    if (msg != nullptr) {
      e->output()->write_blocking(e->decorations(), msg);
      os::free(msg);
    } else if (e->output() == nullptr) {
      // This is a flush token. Record that we found it and then
      // signal the flushing thread after the loop.
      req++;
    }
  }

  if (req > 0) {
    assert(req == 1, "AsyncLogWriter::flush() is NOT MT-safe!");
    _flush_sem.signal(req);
  }
}

void AsyncLogWriter::run() {
  while (true) {
    {
      AsyncLogLocker locker;

      while (!_data_available) {
        _lock.wait(0/* no timeout */);
      }
    }

    write();
  }
}

AsyncLogWriter* AsyncLogWriter::_instance = nullptr;

void AsyncLogWriter::initialize() {
  if (!LogConfiguration::is_async_mode()) return;

  assert(_instance == nullptr, "initialize() should only be invoked once.");

  AsyncLogWriter* self = new AsyncLogWriter();
  if (self->_initialized) {
    Atomic::release_store_fence(&AsyncLogWriter::_instance, self);
    // All readers of _instance after the fence see non-NULL.
    // We use LogOutputList's RCU counters to ensure all synchronous logsites have completed.
    // After that, we start AsyncLog Thread and it exclusively takes over all logging I/O.
    for (LogTagSet* ts = LogTagSet::first(); ts != NULL; ts = ts->next()) {
      ts->wait_until_no_readers();
    }
    os::start_thread(self);
    log_debug(logging, thread)("Async logging thread started.");
  }
}

AsyncLogWriter* AsyncLogWriter::instance() {
  return _instance;
}

// Inserts a flush token into the async output buffer and waits until the AsyncLog thread
// signals that it has seen it and completed all dequeued message processing.
// This method is not MT-safe in itself, but is guarded by another lock in the usual
// usecase - see the comments in the header file for more details.
void AsyncLogWriter::flush() {
  if (_instance != nullptr) {
    {
      using none = LogTagSetMapping<LogTag::__NO_TAG>;
      AsyncLogLocker locker;
      LogDecorations d(LogLevel::Off, none::tagset(), LogDecorators::None);
      AsyncLogMessage token(nullptr, d, nullptr);

      // Push directly in-case we are at logical max capacity, as this must not get dropped.
      _instance->_buffer.push_back(token);
      _instance->_data_available = true;
      _instance->_lock.notify();
    }

    _instance->_flush_sem.wait();
  }
}
