/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm.h"
#include "ci/ciMethod.hpp"
#include "code/codeCache.hpp"
#include "compiler/compileLog.hpp"
#include "memory/allocation.inline.hpp"
#include "oops/method.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/os.hpp"

CompileLog* CompileLog::_first = NULL;

// ------------------------------------------------------------------
// CompileLog::CompileLog
CompileLog::CompileLog(const char* file_name, FILE* fp, intx thread_id)
  : _context(_context_buffer, sizeof(_context_buffer))
{
  initialize(new(ResourceObj::C_HEAP, mtCompiler) fileStream(fp, true));
  _file_end = 0;
  _thread_id = thread_id;

  _identities_limit = 0;
  _identities_capacity = 400;
  _identities = NEW_C_HEAP_ARRAY(char, _identities_capacity, mtCompiler);
  _file = NEW_C_HEAP_ARRAY(char, strlen(file_name)+1, mtCompiler);
   strcpy((char*)_file, file_name);

  // link into the global list
  { MutexLocker locker(CompileTaskAlloc_lock);
    _next = _first;
    _first = this;
  }
}

CompileLog::~CompileLog() {
  delete _out; // Close fd in fileStream::~fileStream()
  _out = NULL;
  // Remove partial file after merging in CompileLog::finish_log_on_error
  unlink(_file);
  FREE_C_HEAP_ARRAY(char, _identities);
  FREE_C_HEAP_ARRAY(char, _file);
}


// see_tag, pop_tag:  Override the default do-nothing methods on xmlStream.
// These methods provide a hook for managing the extra context markup.
void CompileLog::see_tag(const char* tag, bool push) {
  if (_context.size() > 0 && _out != NULL) {
    _out->write(_context.base(), _context.size());
    _context.reset();
  }
  xmlStream::see_tag(tag, push);
}
void CompileLog::pop_tag(const char* tag) {
  _context.reset();  // toss any context info.
  xmlStream::pop_tag(tag);
}


// ------------------------------------------------------------------
// CompileLog::identify
int CompileLog::identify(ciBaseObject* obj) {
  if (obj == NULL)  return 0;
  int id = obj->ident();
  if (id < 0)  return id;
  // If it has already been identified, just return the id.
  if (id < _identities_limit && _identities[id] != 0)  return id;
  // Lengthen the array, if necessary.
  if (id >= _identities_capacity) {
    int new_cap = _identities_capacity * 2;
    if (new_cap <= id)  new_cap = id + 100;
    _identities = REALLOC_C_HEAP_ARRAY(char, _identities, new_cap, mtCompiler);
    _identities_capacity = new_cap;
  }
  while (id >= _identities_limit) {
    _identities[_identities_limit++] = 0;
  }
  assert(id < _identities_limit, "oob");
  // Mark this id as processed.
  // (Be sure to do this before any recursive calls to identify.)
  _identities[id] = 1;  // mark

  // Now, print the object's identity once, in detail.
  if (obj->is_metadata()) {
    ciMetadata* mobj = obj->as_metadata();
    if (mobj->is_klass()) {
      ciKlass* klass = mobj->as_klass();
      begin_elem("klass id='%d'", id);
      name(klass);
      if (!klass->is_loaded()) {
        print(" unloaded='1'");
      } else {
        print(" flags='%d'", klass->modifier_flags());
      }
      end_elem();
    } else if (mobj->is_method()) {
      ciMethod* method = mobj->as_method();
      ciSignature* sig = method->signature();
      // Pre-identify items that we will need!
      identify(sig->return_type());
      for (int i = 0; i < sig->count(); i++) {
        identify(sig->type_at(i));
      }
      begin_elem("method id='%d' holder='%d'",
          id, identify(method->holder()));
      name(method->name());
      print(" return='%d'", identify(sig->return_type()));
      if (sig->count() > 0) {
        print(" arguments='");
        for (int i = 0; i < sig->count(); i++) {
          print((i == 0) ? "%d" : " %d", identify(sig->type_at(i)));
        }
        print("'");
      }
      if (!method->is_loaded()) {
        print(" unloaded='1'");
      } else {
        print(" flags='%d'", (jchar) method->flags().as_int());
        // output a few metrics
        print(" bytes='%d'", method->code_size());
        method->log_nmethod_identity(this);
        //print(" count='%d'", method->invocation_count());
        //int bec = method->backedge_count();
        //if (bec != 0)  print(" backedge_count='%d'", bec);
        print(" iicount='%d'", method->interpreter_invocation_count());
      }
      end_elem();
    } else if (mobj->is_type()) {
      BasicType type = mobj->as_type()->basic_type();
      elem("type id='%d' name='%s'", id, type2name(type));
    } else {
      // Should not happen.
      elem("unknown id='%d'", id);
      ShouldNotReachHere();
    }
  } else if (obj->is_symbol()) {
    begin_elem("symbol id='%d'", id);
    name(obj->as_symbol());
    end_elem();
  } else {
    // Should not happen.
    elem("unknown id='%d'", id);
  }
  return id;
}

void CompileLog::name(ciSymbol* name) {
  if (name == NULL)  return;
  print(" name='");
  name->print_symbol_on(text());  // handles quoting conventions
  print("'");
}

void CompileLog::name(ciKlass* k) {
  print(" name='");
  if (!k->is_loaded()) {
    text()->print("%s", k->name()->as_klass_external_name());
  } else {
    text()->print("%s", k->external_name());
  }
  print("'");
}

// ------------------------------------------------------------------
// CompileLog::clear_identities
// Forget which identities have been printed.
void CompileLog::clear_identities() {
  _identities_limit = 0;
}

// ------------------------------------------------------------------
// CompileLog::finish_log_on_error
//
// Note: This function is called after fatal error, avoid unnecessary memory
// or stack allocation, use only async-safe functions. It's possible JVM is
// only partially initialized.
void CompileLog::finish_log_on_error(outputStream* file, char* buf, int buflen) {
  static bool called_exit = false;
  if (called_exit)  return;
  called_exit = true;

  CompileLog* log = _first;
  while (log != NULL) {
    log->flush();
    const char* partial_file = log->file();
    int partial_fd = open(partial_file, O_RDONLY);
    if (partial_fd != -1) {
      // print/print_cr may need to allocate large stack buffer to format
      // strings, here we use snprintf() and print_raw() instead.
      file->print_raw("<compilation_log thread='");
      jio_snprintf(buf, buflen, UINTX_FORMAT, log->thread_id());
      file->print_raw(buf);
      file->print_raw_cr("'>");

      size_t nr; // number read into buf from partial log
      // In case of unsuccessful completion, read returns -1.
      ssize_t bytes_read;
      // Copy data up to the end of the last <event> element:
      julong to_read = log->_file_end;
      while (to_read > 0) {
        if (to_read < (julong)buflen)
              nr = (size_t)to_read;
        else  nr = buflen;
        bytes_read = read(partial_fd, buf, (int)nr);
        if (bytes_read <= 0) break;
        nr = bytes_read;
        to_read -= (julong)nr;
        file->write(buf, nr);
      }

      // Copy any remaining data inside a quote:
      bool saw_slop = false;
      int end_cdata = 0;  // state machine [0..2] watching for too many "]]"
      while ((bytes_read = read(partial_fd, buf, buflen-1)) > 0) {
        nr = bytes_read;
        buf[buflen-1] = '\0';
        if (!saw_slop) {
          file->print_raw_cr("<fragment>");
          file->print_raw_cr("<![CDATA[");
          saw_slop = true;
        }
        // The rest of this loop amounts to a simple copy operation:
        // { file->write(buf, nr); }
        // However, it must sometimes output the buffer in parts,
        // in case there is a CDATA quote embedded in the fragment.
        const char* bufp;  // pointer into buf
        size_t nw; // number written in each pass of the following loop:
        for (bufp = buf; nr > 0; nr -= nw, bufp += nw) {
          // Write up to any problematic CDATA delimiter (usually all of nr).
          for (nw = 0; nw < nr; nw++) {
            // First, scan ahead into the buf, checking the state machine.
            switch (bufp[nw]) {
            case ']':
              if (end_cdata < 2)   end_cdata += 1;  // saturating counter
              continue;  // keep scanning
            case '>':
              if (end_cdata == 2)  break;  // found CDATA delimiter!
              // else fall through:
            default:
              end_cdata = 0;
              continue;  // keep scanning
            }
            // If we get here, nw is pointing at a bad '>'.
            // It is very rare for this to happen.
            // However, this code has been tested by introducing
            // CDATA sequences into the compilation log.
            break;
          }
          // Now nw is the number of characters to write, usually == nr.
          file->write(bufp, nw);
          if (nw < nr) {
            // We are about to go around the loop again.
            // But first, disrupt the ]]> by closing and reopening the quote.
            file->print_raw("]]><![CDATA[");
            end_cdata = 0;  // reset state machine
          }
        }
      }
      if (saw_slop) {
        file->print_raw_cr("]]>");
        file->print_raw_cr("</fragment>");
      }
      file->print_raw_cr("</compilation_log>");
      close(partial_fd);
    }
    CompileLog* next_log = log->_next;
    delete log; // Removes partial file
    log = next_log;
  }
  _first = NULL;
}

// ------------------------------------------------------------------
// CompileLog::finish_log
//
// Called during normal shutdown. For now, any clean-up needed in normal
// shutdown is also needed in VM abort, so is covered by finish_log_on_error().
// Just allocate a buffer and call finish_log_on_error().
void CompileLog::finish_log(outputStream* file) {
  char buf[4 * K];
  finish_log_on_error(file, buf, sizeof(buf));
}

// ------------------------------------------------------------------
// CompileLog::inline_success
//
// Print about successful method inlining.
void CompileLog::inline_success(const char* reason) {
  begin_elem("inline_success reason='");
  text("%s", reason);
  end_elem("'");
}

// ------------------------------------------------------------------
// CompileLog::inline_fail
//
// Print about failed method inlining.
void CompileLog::inline_fail(const char* reason) {
  begin_elem("inline_fail reason='");
  text("%s", reason);
  end_elem("'");
}

// ------------------------------------------------------------------
// CompileLog::set_context
//
// Set XML tag as an optional marker - it is printed only if
// there are other entries after until it is reset.
void CompileLog::set_context(const char* format, ...) {
  va_list ap;
  va_start(ap, format);
  clear_context();
  _context.print("<");
  _context.vprint(format, ap);
  _context.print_cr("/>");
  va_end(ap);
}

// ------------------------------------------------------------------
// CompileLog::code_cache_state
//
// Print code cache state.
void CompileLog::code_cache_state() {
  begin_elem("code_cache");
  CodeCache::log_state(this);
  end_elem("%s", "");
}
