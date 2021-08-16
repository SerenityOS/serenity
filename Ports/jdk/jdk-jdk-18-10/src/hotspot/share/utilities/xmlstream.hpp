/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_XMLSTREAM_HPP
#define SHARE_UTILITIES_XMLSTREAM_HPP

#include "runtime/handles.hpp"
#include "utilities/ostream.hpp"

class xmlStream;
class defaultStream;

// Sub-stream for writing quoted text, as opposed to markup.
// Characters written to this stream are subject to quoting,
// as '<' => "&lt;", etc.
class xmlTextStream : public outputStream {
  friend class xmlStream;
  friend class defaultStream; // tty
 private:

  xmlStream* _outer_xmlStream;

  xmlTextStream() { _outer_xmlStream = NULL; }

 public:
   virtual void flush(); // _outer.flush();
   virtual void write(const char* str, size_t len); // _outer->write_text()
};


// Output stream for writing XML-structured logs.
// To write markup, use special calls elem, head/tail, etc.
// Use the xmlStream::text() stream to write unmarked text.
// Text written that way will be quoted as necessary using '&lt;', etc.
// Characters written directly to an xmlStream via print_cr, etc.,
// are directly written to the encapsulated stream, xmlStream::out().
// This can be used to produce markup directly, character by character.
// (Such writes are not checked for markup syntax errors.)

class xmlStream : public outputStream {
  friend class defaultStream; // tty
 public:
  enum MarkupState { BODY,       // after end_head() call, in text
                     HEAD,       // after begin_head() call, in attrs
                     ELEM };     // after begin_elem() call, in attrs

 protected:
  outputStream* _out;            // file stream by which it goes
  julong        _last_flush;     // last position of flush
  MarkupState   _markup_state;   // where in the elem/head/tail dance
  outputStream* _text;           // text stream
  xmlTextStream _text_init;

  // for subclasses
  xmlStream() {}
  void initialize(outputStream* out);

  // protect this from public use:
  outputStream* out()                            { return _out; }

  // helpers for writing XML elements
  void          va_tag(bool push, const char* format, va_list ap) ATTRIBUTE_PRINTF(3, 0);
  virtual void see_tag(const char* tag, bool push) NOT_DEBUG({});
  virtual void pop_tag(const char* tag) NOT_DEBUG({});

#ifdef ASSERT
  // in debug mode, we verify matching of opening and closing tags
  int   _element_depth;              // number of unfinished elements
  char* _element_close_stack_high;   // upper limit of down-growing stack
  char* _element_close_stack_low;    // upper limit of down-growing stack
  char* _element_close_stack_ptr;    // pointer of down-growing stack
#endif

 public:
  // creation
  xmlStream(outputStream* out) { initialize(out); }
  DEBUG_ONLY(virtual ~xmlStream();)

  bool is_open() { return _out != NULL; }

  // text output
  bool inside_attrs() { return _markup_state != BODY; }

  // flushing
  virtual void flush();  // flushes out, sets _last_flush = count()
  virtual void write(const char* s, size_t len);
  void    write_text(const char* s, size_t len);  // used by xmlTextStream
  int unflushed_count() { return (int)(out()->count() - _last_flush); }

  // writing complete XML elements
  void          elem(const char* format, ...) ATTRIBUTE_PRINTF(2, 3);
  void    begin_elem(const char* format, ...) ATTRIBUTE_PRINTF(2, 3);
  void      end_elem(const char* format, ...) ATTRIBUTE_PRINTF(2, 3);
  void      end_elem();
  void          head(const char* format, ...) ATTRIBUTE_PRINTF(2, 3);
  void    begin_head(const char* format, ...) ATTRIBUTE_PRINTF(2, 3);
  void      end_head(const char* format, ...) ATTRIBUTE_PRINTF(2, 3);
  void      end_head();
  void          done(const char* format, ...) ATTRIBUTE_PRINTF(2, 3);  // xxx_done event, plus tail
  void          done_raw(const char * kind);
  void          tail(const char* kind);

  // va_list versions
  void       va_elem(const char* format, va_list ap) ATTRIBUTE_PRINTF(2, 0);
  void va_begin_elem(const char* format, va_list ap) ATTRIBUTE_PRINTF(2, 0);
  void       va_head(const char* format, va_list ap) ATTRIBUTE_PRINTF(2, 0);
  void va_begin_head(const char* format, va_list ap) ATTRIBUTE_PRINTF(2, 0);
  void       va_done(const char* format, va_list ap) ATTRIBUTE_PRINTF(2, 0);

  // write text (with quoting of special XML characters <>&'" etc.)
  outputStream* text() { return _text; }
  void          text(const char* format, ...) ATTRIBUTE_PRINTF(2, 3);
  void       va_text(const char* format, va_list ap) ATTRIBUTE_PRINTF(2, 0) {
    text()->vprint(format, ap);
  }

  // commonly used XML attributes
  void          stamp();                 // stamp='1.234'
  void          method(Method* m);       // method='k n s' ...
  void          klass(Klass* k);         // klass='name'
  void          name(const Symbol* s);   // name='name'
  void          object(const char* attr, Metadata* val);
  void          object(const char* attr, Handle val);

  // print the text alone (sans ''):
  void          method_text(Method* m);
  void          klass_text(Klass* k);         // klass='name'
  void          name_text(const Symbol* s);   // name='name'
  void          object_text(Metadata* x);
  void          object_text(Handle x);

  /*  Example uses:

      // Empty element, simple case.
      elem("X Y='Z'");          <X Y='Z'/> \n

      // Empty element, general case.
      begin_elem("X Y='Z'");    <X Y='Z'
      ...attrs...               ...attrs...
      end_elem();               />

      // Compound element, simple case.
      head("X Y='Z'");          <X Y='Z'> \n
      ...body...                ...body...
      tail("X");                </X> \n

      // Compound element, general case.
      begin_head("X Y='Z'");    <X Y='Z'
      ...attrs...               ...attrs...
      end_head();               > \n
      ...body...                ...body...
      tail("X");                </X> \n

      // Printf-style formatting:
      elem("X Y='%s'", "Z");    <X Y='Z'/> \n

   */

};

// Standard log file, null if no logging is happening.
extern xmlStream* xtty;

// Note:  If ::xtty != NULL, ::tty == ::xtty->text().

#endif // SHARE_UTILITIES_XMLSTREAM_HPP
