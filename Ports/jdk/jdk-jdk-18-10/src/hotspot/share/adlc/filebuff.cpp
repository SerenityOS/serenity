/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

// FILEBUFF.CPP - Routines for handling a parser file buffer
#include "adlc.hpp"

//------------------------------FileBuff---------------------------------------
// Create a new parsing buffer
FileBuff::FileBuff( BufferedFile *fptr, ArchDesc& archDesc) : _fp(fptr), _AD(archDesc) {
  _err = fseek(_fp->_fp, 0, SEEK_END);  // Seek to end of file
  if (_err) {
    file_error(SEMERR, 0, "File seek error reading input file");
    exit(1);                    // Exit on seek error
  }
  _filepos = ftell(_fp->_fp);   // Find offset of end of file
  _bufferSize = _filepos + 5;   // Filepos points to last char, so add padding
  _err = fseek(_fp->_fp, 0, SEEK_SET);  // Reset to beginning of file
  if (_err) {
    file_error(SEMERR, 0, "File seek error reading input file\n");
    exit(1);                    // Exit on seek error
  }
  _filepos = ftell(_fp->_fp);      // Reset current file position
  _linenum = 0;

  _bigbuf = new char[_bufferSize]; // Create buffer to hold text for parser
  if( !_bigbuf ) {
    file_error(SEMERR, 0, "Buffer allocation failed\n");
    exit(1);                    // Exit on allocation failure
  }
  *_bigbuf = '\n';               // Lead with a sentinel newline
  _buf = _bigbuf+1;                     // Skip sentinel
  _bufmax = _buf;               // Buffer is empty
  _bufeol = _bigbuf;              // _bufeol points at sentinel
  _filepos = -1;                 // filepos is in sync with _bufeol
  _bufoff = _offset = 0L;       // Offset at file start

  _bufmax += fread(_buf, 1, _bufferSize-2, _fp->_fp); // Fill buffer & set end value
  if (_bufmax == _buf) {
    file_error(SEMERR, 0, "File read error, no input read\n");
    exit(1);                     // Exit on read error
  }
  *_bufmax = '\n';               // End with a sentinel new-line
  *(_bufmax+1) = '\0';           // Then end with a sentinel NULL
}

//------------------------------~FileBuff--------------------------------------
// Nuke the FileBuff
FileBuff::~FileBuff() {
  delete[] _bigbuf;
}

//------------------------------get_line----------------------------------------
char *FileBuff::get_line(void) {
  char *retval;

  // Check for end of file & return NULL
  if (_bufeol >= _bufmax) return NULL;

  _linenum++;
  retval = ++_bufeol;      // return character following end of previous line
  if (*retval == '\0') return NULL; // Check for EOF sentinel
  // Search for newline character which must end each line
  for(_filepos++; *_bufeol != '\n'; _bufeol++)
    _filepos++;                    // keep filepos in sync with _bufeol
  // _bufeol & filepos point at end of current line, so return pointer to start
  return retval;
}

//------------------------------file_error-------------------------------------
void FileBuff::file_error(int flag, int linenum, const char *fmt, ...)
{
  va_list args;

  va_start(args, fmt);
  switch (flag) {
  case 0: _AD._warnings += _AD.emit_msg(0, flag, linenum, fmt, args);
    break;
  case 1: _AD._syntax_errs += _AD.emit_msg(0, flag, linenum, fmt, args);
    break;
  case 2: _AD._semantic_errs += _AD.emit_msg(0, flag, linenum, fmt, args);
    break;
  default: assert(0, ""); break;
  }
  va_end(args);
  _AD._no_output = 1;
}
