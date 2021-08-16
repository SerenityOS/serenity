/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to
 * any person obtaining a copy of this software, associated documentation
 * and/or data (collectively the "Software"), free of charge and under any
 * and all copyright rights in the Software, and any and all patent rights
 * owned or freely licensable by each licensor hereunder covering either (i)
 * the unmodified Software as contributed to or provided by such licensor,
 * or (ii) the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file
 * if one is included with the Software (each a "Larger Work" to which the
 * Software is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy,
 * create derivative works of, display, perform, and distribute the Software
 * and make, use, sell, offer for sale, import, export, have made, and have
 * sold the Software and the Larger Work(s), and to sublicense the foregoing
 * rights on either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or
 * at a minimum a reference to the UPL must be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

/* decode_instructions -- dump a range of addresses as native instructions
   This implements the protocol required by the HotSpot PrintAssembly option.

   The start_va, end_va is the virtual address the region of memory to
   disasemble and buffer contains the instructions to decode,
   Disassembling instructions in the current address space is done by
   having start_va == buffer.

   The option string, if not empty, is interpreted by the disassembler implementation.

   The printf callback is 'fprintf' or any other workalike.
   It is called as (*printf_callback)(printf_stream, "some format...", some, format, args).

   The event callback receives an event tag (a string) and an argument (a void*).
   It is called as (*event_callback)(event_stream, "tag", arg).

   Events:
     <insn pc='%p'>             begin an instruction, at a given location
     </insn pc='%d'>            end an instruction, at a given location
     <addr value='%p'/>         emit the symbolic value of an address

   A tag format is one of three basic forms: "tag", "/tag", "tag/",
   where tag is a simple identifier, signifying (as in XML) a element start,
   element end, and standalone element.  (To render as XML, add angle brackets.)
*/
#ifndef SHARED_TOOLS_HSDIS_H
#define SHARED_TOOLS_HSDIS_H

extern
#ifdef DLL_EXPORT
  DLL_EXPORT
#endif
void* decode_instructions_virtual(uintptr_t start_va, uintptr_t end_va,
                                  unsigned char* buffer, uintptr_t length,
                                  void* (*event_callback)(void*, const char*, void*),
                                  void* event_stream,
                                  int (*printf_callback)(void*, const char*, ...),
                                  void* printf_stream,
                                  const char* options,
                                  int newline /* bool value for nice new line */);

/* This is the compatability interface for older versions of hotspot */
extern
#ifdef DLL_ENTRY
  DLL_ENTRY
#endif
void* decode_instructions(void* start_pv, void* end_pv,
                    void* (*event_callback)(void*, const char*, void*),
                    void* event_stream,
                    int   (*printf_callback)(void*, const char*, ...),
                    void* printf_stream,
                    const char* options);

/* convenience typedefs */

typedef void* (*decode_instructions_event_callback_ftype)  (void*, const char*, void*);
typedef int   (*decode_instructions_printf_callback_ftype) (void*, const char*, ...);
typedef void* (*decode_func_vtype) (uintptr_t start_va, uintptr_t end_va,
                                    unsigned char* buffer, uintptr_t length,
                                    decode_instructions_event_callback_ftype event_callback,
                                    void* event_stream,
                                    decode_instructions_printf_callback_ftype printf_callback,
                                    void* printf_stream,
                                    const char* options,
                                    int newline);
typedef void* (*decode_func_stype) (void* start_pv, void* end_pv,
                                    decode_instructions_event_callback_ftype event_callback,
                                    void* event_stream,
                                    decode_instructions_printf_callback_ftype printf_callback,
                                    void* printf_stream,
                                    const char* options);
#endif /* SHARED_TOOLS_HSDIS_H */
