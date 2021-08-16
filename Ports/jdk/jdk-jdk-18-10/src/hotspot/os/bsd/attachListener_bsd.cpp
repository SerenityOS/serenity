/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "logging/log.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/os.inline.hpp"
#include "services/attachListener.hpp"
#include "services/dtraceAttacher.hpp"

#include <unistd.h>
#include <signal.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/stat.h>

#ifndef UNIX_PATH_MAX
#define UNIX_PATH_MAX   sizeof(((struct sockaddr_un *)0)->sun_path)
#endif

// The attach mechanism on Bsd uses a UNIX domain socket. An attach listener
// thread is created at startup or is created on-demand via a signal from
// the client tool. The attach listener creates a socket and binds it to a file
// in the filesystem. The attach listener then acts as a simple (single-
// threaded) server - it waits for a client to connect, reads the request,
// executes it, and returns the response to the client via the socket
// connection.
//
// As the socket is a UNIX domain socket it means that only clients on the
// local machine can connect. In addition there are two other aspects to
// the security:
// 1. The well known file that the socket is bound to has permission 400
// 2. When a client connect, the SO_PEERCRED socket option is used to
//    obtain the credentials of client. We check that the effective uid
//    of the client matches this process.

// forward reference
class BsdAttachOperation;

class BsdAttachListener: AllStatic {
 private:
  // the path to which we bind the UNIX domain socket
  static char _path[UNIX_PATH_MAX];
  static bool _has_path;

  // the file descriptor for the listening socket
  static volatile int _listener;

  static bool _atexit_registered;

  // reads a request from the given connected socket
  static BsdAttachOperation* read_request(int s);

 public:
  enum {
    ATTACH_PROTOCOL_VER = 1                     // protocol version
  };
  enum {
    ATTACH_ERROR_BADVERSION     = 101           // error codes
  };

  static void set_path(char* path) {
    if (path == NULL) {
      _path[0] = '\0';
      _has_path = false;
    } else {
      strncpy(_path, path, UNIX_PATH_MAX);
      _path[UNIX_PATH_MAX-1] = '\0';
      _has_path = true;
    }
  }

  static void set_listener(int s)               { _listener = s; }

  // initialize the listener, returns 0 if okay
  static int init();

  static char* path()                   { return _path; }
  static bool has_path()                { return _has_path; }
  static int listener()                 { return _listener; }

  // write the given buffer to a socket
  static int write_fully(int s, char* buf, int len);

  static BsdAttachOperation* dequeue();
};

class BsdAttachOperation: public AttachOperation {
 private:
  // the connection to the client
  int _socket;

 public:
  void complete(jint res, bufferedStream* st);

  void set_socket(int s)                                { _socket = s; }
  int socket() const                                    { return _socket; }

  BsdAttachOperation(char* name) : AttachOperation(name) {
    set_socket(-1);
  }
};

// statics
char BsdAttachListener::_path[UNIX_PATH_MAX];
bool BsdAttachListener::_has_path;
volatile int BsdAttachListener::_listener = -1;
bool BsdAttachListener::_atexit_registered = false;

// Supporting class to help split a buffer into individual components
class ArgumentIterator : public StackObj {
 private:
  char* _pos;
  char* _end;
 public:
  ArgumentIterator(char* arg_buffer, size_t arg_size) {
    _pos = arg_buffer;
    _end = _pos + arg_size - 1;
  }
  char* next() {
    if (*_pos == '\0') {
      // advance the iterator if possible (null arguments)
      if (_pos < _end) {
        _pos += 1;
      }
      return NULL;
    }
    char* res = _pos;
    char* next_pos = strchr(_pos, '\0');
    if (next_pos < _end)  {
      next_pos++;
    }
    _pos = next_pos;
    return res;
  }
};


// atexit hook to stop listener and unlink the file that it is
// bound too.
extern "C" {
  static void listener_cleanup() {
    int s = BsdAttachListener::listener();
    if (s != -1) {
      BsdAttachListener::set_listener(-1);
      ::shutdown(s, SHUT_RDWR);
      ::close(s);
    }
    if (BsdAttachListener::has_path()) {
      ::unlink(BsdAttachListener::path());
      BsdAttachListener::set_path(NULL);
    }
  }
}

// Initialization - create a listener socket and bind it to a file

int BsdAttachListener::init() {
  char path[UNIX_PATH_MAX];          // socket file
  char initial_path[UNIX_PATH_MAX];  // socket file during setup
  int listener;                      // listener socket (file descriptor)

  // register function to cleanup
  if (!_atexit_registered) {
    _atexit_registered = true;
    ::atexit(listener_cleanup);
  }

  int n = snprintf(path, UNIX_PATH_MAX, "%s/.java_pid%d",
                   os::get_temp_directory(), os::current_process_id());
  if (n < (int)UNIX_PATH_MAX) {
    n = snprintf(initial_path, UNIX_PATH_MAX, "%s.tmp", path);
  }
  if (n >= (int)UNIX_PATH_MAX) {
    return -1;
  }

  // create the listener socket
  listener = ::socket(PF_UNIX, SOCK_STREAM, 0);
  if (listener == -1) {
    return -1;
  }

  // bind socket
  struct sockaddr_un addr;
  memset((void *)&addr, 0, sizeof(addr));
  addr.sun_family = AF_UNIX;
  strcpy(addr.sun_path, initial_path);
  ::unlink(initial_path);
  int res = ::bind(listener, (struct sockaddr*)&addr, sizeof(addr));
  if (res == -1) {
    ::close(listener);
    return -1;
  }

  // put in listen mode, set permissions, and rename into place
  res = ::listen(listener, 5);
  if (res == 0) {
    RESTARTABLE(::chmod(initial_path, S_IREAD|S_IWRITE), res);
    if (res == 0) {
      // make sure the file is owned by the effective user and effective group
      // e.g. default behavior on mac is that new files inherit the group of
      // the directory that they are created in
      RESTARTABLE(::chown(initial_path, geteuid(), getegid()), res);
      if (res == 0) {
        res = ::rename(initial_path, path);
      }
    }
  }
  if (res == -1) {
    ::close(listener);
    ::unlink(initial_path);
    return -1;
  }
  set_path(path);
  set_listener(listener);

  return 0;
}

// Given a socket that is connected to a peer we read the request and
// create an AttachOperation. As the socket is blocking there is potential
// for a denial-of-service if the peer does not response. However this happens
// after the peer credentials have been checked and in the worst case it just
// means that the attach listener thread is blocked.
//
BsdAttachOperation* BsdAttachListener::read_request(int s) {
  char ver_str[8];
  sprintf(ver_str, "%d", ATTACH_PROTOCOL_VER);

  // The request is a sequence of strings so we first figure out the
  // expected count and the maximum possible length of the request.
  // The request is:
  //   <ver>0<cmd>0<arg>0<arg>0<arg>0
  // where <ver> is the protocol version (1), <cmd> is the command
  // name ("load", "datadump", ...), and <arg> is an argument
  int expected_str_count = 2 + AttachOperation::arg_count_max;
  const int max_len = (sizeof(ver_str) + 1) + (AttachOperation::name_length_max + 1) +
    AttachOperation::arg_count_max*(AttachOperation::arg_length_max + 1);

  char buf[max_len];
  int str_count = 0;

  // Read until all (expected) strings have been read, the buffer is
  // full, or EOF.

  int off = 0;
  int left = max_len;

  do {
    int n;
    RESTARTABLE(read(s, buf+off, left), n);
    assert(n <= left, "buffer was too small, impossible!");
    buf[max_len - 1] = '\0';
    if (n == -1) {
      return NULL;      // reset by peer or other error
    }
    if (n == 0) {
      break;
    }
    for (int i=0; i<n; i++) {
      if (buf[off+i] == 0) {
        // EOS found
        str_count++;

        // The first string is <ver> so check it now to
        // check for protocol mis-match
        if (str_count == 1) {
          if ((strlen(buf) != strlen(ver_str)) ||
              (atoi(buf) != ATTACH_PROTOCOL_VER)) {
            char msg[32];
            sprintf(msg, "%d\n", ATTACH_ERROR_BADVERSION);
            write_fully(s, msg, strlen(msg));
            return NULL;
          }
        }
      }
    }
    off += n;
    left -= n;
  } while (left > 0 && str_count < expected_str_count);

  if (str_count != expected_str_count) {
    return NULL;        // incomplete request
  }

  // parse request

  ArgumentIterator args(buf, (max_len)-left);

  // version already checked
  char* v = args.next();

  char* name = args.next();
  if (name == NULL || strlen(name) > AttachOperation::name_length_max) {
    return NULL;
  }

  BsdAttachOperation* op = new BsdAttachOperation(name);

  for (int i=0; i<AttachOperation::arg_count_max; i++) {
    char* arg = args.next();
    if (arg == NULL) {
      op->set_arg(i, NULL);
    } else {
      if (strlen(arg) > AttachOperation::arg_length_max) {
        delete op;
        return NULL;
      }
      op->set_arg(i, arg);
    }
  }

  op->set_socket(s);
  return op;
}


// Dequeue an operation
//
// In the Bsd implementation there is only a single operation and clients
// cannot queue commands (except at the socket level).
//
BsdAttachOperation* BsdAttachListener::dequeue() {
  for (;;) {
    int s;

    // wait for client to connect
    struct sockaddr addr;
    socklen_t len = sizeof(addr);
    RESTARTABLE(::accept(listener(), &addr, &len), s);
    if (s == -1) {
      return NULL;      // log a warning?
    }

    // get the credentials of the peer and check the effective uid/guid
    uid_t puid;
    gid_t pgid;
    if (::getpeereid(s, &puid, &pgid) != 0) {
      log_debug(attach)("Failed to get peer id");
      ::close(s);
      continue;
    }

    if (!os::Posix::matches_effective_uid_and_gid_or_root(puid, pgid)) {
      log_debug(attach)("euid/egid check failed (%d/%d vs %d/%d)", puid, pgid,
              geteuid(), getegid());
      ::close(s);
      continue;
    }

    // peer credential look okay so we read the request
    BsdAttachOperation* op = read_request(s);
    if (op == NULL) {
      ::close(s);
      continue;
    } else {
      return op;
    }
  }
}

// write the given buffer to the socket
int BsdAttachListener::write_fully(int s, char* buf, int len) {
  do {
    int n = ::write(s, buf, len);
    if (n == -1) {
      if (errno != EINTR) return -1;
    } else {
      buf += n;
      len -= n;
    }
  }
  while (len > 0);
  return 0;
}

// Complete an operation by sending the operation result and any result
// output to the client. At this time the socket is in blocking mode so
// potentially we can block if there is a lot of data and the client is
// non-responsive. For most operations this is a non-issue because the
// default send buffer is sufficient to buffer everything. In the future
// if there are operations that involves a very big reply then it the
// socket could be made non-blocking and a timeout could be used.

void BsdAttachOperation::complete(jint result, bufferedStream* st) {
  JavaThread* thread = JavaThread::current();
  ThreadBlockInVM tbivm(thread);

  // write operation result
  char msg[32];
  sprintf(msg, "%d\n", result);
  int rc = BsdAttachListener::write_fully(this->socket(), msg, strlen(msg));

  // write any result data
  if (rc == 0) {
    BsdAttachListener::write_fully(this->socket(), (char*) st->base(), st->size());
    ::shutdown(this->socket(), 2);
  }

  // done
  ::close(this->socket());

  delete this;
}


// AttachListener functions

AttachOperation* AttachListener::dequeue() {
  JavaThread* thread = JavaThread::current();
  ThreadBlockInVM tbivm(thread);

  AttachOperation* op = BsdAttachListener::dequeue();

  return op;
}

// Performs initialization at vm startup
// For BSD we remove any stale .java_pid file which could cause
// an attaching process to think we are ready to receive on the
// domain socket before we are properly initialized

void AttachListener::vm_start() {
  char fn[UNIX_PATH_MAX];
  struct stat st;
  int ret;

  int n = snprintf(fn, UNIX_PATH_MAX, "%s/.java_pid%d",
           os::get_temp_directory(), os::current_process_id());
  assert(n < (int)UNIX_PATH_MAX, "java_pid file name buffer overflow");

  RESTARTABLE(::stat(fn, &st), ret);
  if (ret == 0) {
    ret = ::unlink(fn);
    if (ret == -1) {
      log_debug(attach)("Failed to remove stale attach pid file at %s", fn);
    }
  }
}

int AttachListener::pd_init() {
  JavaThread* thread = JavaThread::current();
  ThreadBlockInVM tbivm(thread);

  int ret_code = BsdAttachListener::init();

  return ret_code;
}

bool AttachListener::check_socket_file() {
  int ret;
  struct stat st;
  ret = stat(BsdAttachListener::path(), &st);
  if (ret == -1) { // need to restart attach listener.
    log_debug(attach)("Socket file %s does not exist - Restart Attach Listener",
                      BsdAttachListener::path());

    listener_cleanup();

    // wait to terminate current attach listener instance...
    {
      // avoid deadlock if AttachListener thread is blocked at safepoint
      ThreadBlockInVM tbivm(JavaThread::current());
      while (AttachListener::transit_state(AL_INITIALIZING,
                                           AL_NOT_INITIALIZED) != AL_NOT_INITIALIZED) {
        os::naked_yield();
      }
    }
    return is_init_trigger();
  }
  return false;
}

// Attach Listener is started lazily except in the case when
// +ReduseSignalUsage is used
bool AttachListener::init_at_startup() {
  if (ReduceSignalUsage) {
    return true;
  } else {
    return false;
  }
}

// If the file .attach_pid<pid> exists in the working directory
// or /tmp then this is the trigger to start the attach mechanism
bool AttachListener::is_init_trigger() {
  if (init_at_startup() || is_initialized()) {
    return false;               // initialized at startup or already initialized
  }
  char fn[PATH_MAX + 1];
  int ret;
  struct stat st;
  snprintf(fn, PATH_MAX + 1, "%s/.attach_pid%d",
           os::get_temp_directory(), os::current_process_id());
  RESTARTABLE(::stat(fn, &st), ret);
  if (ret == -1) {
    log_debug(attach)("Failed to find attach file: %s", fn);
  }
  if (ret == 0) {
    // simple check to avoid starting the attach mechanism when
    // a bogus non-root user creates the file
    if (os::Posix::matches_effective_uid_or_root(st.st_uid)) {
      init();
      log_trace(attach)("Attach triggered by %s", fn);
      return true;
    } else {
      log_debug(attach)("File %s has wrong user id %d (vs %d). Attach is not triggered", fn, st.st_uid, geteuid());
    }
  }
  return false;
}

// if VM aborts then remove listener
void AttachListener::abort() {
  listener_cleanup();
}

void AttachListener::pd_data_dump() {
  os::signal_notify(SIGQUIT);
}

AttachOperationFunctionInfo* AttachListener::pd_find_operation(const char* n) {
  return NULL;
}

jint AttachListener::pd_set_flag(AttachOperation* op, outputStream* out) {
  out->print_cr("flag '%s' cannot be changed", op->arg(0));
  return JNI_ERR;
}

void AttachListener::pd_detachall() {
  // do nothing for now
}
