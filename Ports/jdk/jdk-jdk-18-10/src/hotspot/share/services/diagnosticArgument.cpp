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

#include "precompiled.hpp"
#include "jvm.h"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/thread.hpp"
#include "services/diagnosticArgument.hpp"

StringArrayArgument::StringArrayArgument() {
  _array = new (ResourceObj::C_HEAP, mtServiceability) GrowableArray<char *>(32, mtServiceability);
  assert(_array != NULL, "Sanity check");
}

StringArrayArgument::~StringArrayArgument() {
  for (int i=0; i<_array->length(); i++) {
    FREE_C_HEAP_ARRAY(char, _array->at(i));
  }
  delete _array;
}

void StringArrayArgument::add(const char* str, size_t len) {
  if (str != NULL) {
    char* ptr = NEW_C_HEAP_ARRAY(char, len+1, mtInternal);
    strncpy(ptr, str, len);
    ptr[len] = 0;
    _array->append(ptr);
  }
}

void GenDCmdArgument::read_value(const char* str, size_t len, TRAPS) {
  /* NOTE:Some argument types doesn't require a value,
   * for instance boolean arguments: "enableFeatureX". is
   * equivalent to "enableFeatureX=true". In these cases,
   * str will be null. This is perfectly valid.
   * All argument types must perform null checks on str.
   */

  if (is_set() && !allow_multiple()) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
            "Duplicates in diagnostic command arguments\n");
  }
  parse_value(str, len, CHECK);
  set_is_set(true);
}

void GenDCmdArgument::to_string(jlong l, char* buf, size_t len) const {
  jio_snprintf(buf, len, INT64_FORMAT, l);
}

void GenDCmdArgument::to_string(bool b, char* buf, size_t len) const {
  jio_snprintf(buf, len, b ? "true" : "false");
}

void GenDCmdArgument::to_string(NanoTimeArgument n, char* buf, size_t len) const {
  jio_snprintf(buf, len, INT64_FORMAT, n._nanotime);
}

void GenDCmdArgument::to_string(MemorySizeArgument m, char* buf, size_t len) const {
  jio_snprintf(buf, len, INT64_FORMAT, m._size);
}

void GenDCmdArgument::to_string(char* c, char* buf, size_t len) const {
  jio_snprintf(buf, len, "%s", (c != NULL) ? c : "");
}

void GenDCmdArgument::to_string(StringArrayArgument* f, char* buf, size_t len) const {
  int length = f->array()->length();
  size_t written = 0;
  buf[0] = 0;
  for (int i = 0; i < length; i++) {
    char* next_str = f->array()->at(i);
    size_t next_size = strlen(next_str);
    //Check if there's room left to write next element
    if (written + next_size > len) {
      return;
    }
    //Actually write element
    strcat(buf, next_str);
    written += next_size;
    //Check if there's room left for the comma
    if (i < length-1 && len - written > 0) {
      strcat(buf, ",");
    }
  }
}

template <> void DCmdArgument<jlong>::parse_value(const char* str,
                                                  size_t len, TRAPS) {
  int scanned = -1;
  if (str == NULL
      || sscanf(str, JLONG_FORMAT "%n", &_value, &scanned) != 1
      || (size_t)scanned != len)
  {
    ResourceMark rm;

    char* buf = NEW_RESOURCE_ARRAY(char, len + 1);
    strncpy(buf, str, len);
    buf[len] = '\0';
    Exceptions::fthrow(THREAD_AND_LOCATION, vmSymbols::java_lang_IllegalArgumentException(),
      "Integer parsing error in command argument '%s'. Could not parse: %s.\n", _name, buf);
  }
}

template <> void DCmdArgument<jlong>::init_value(TRAPS) {
  if (has_default()) {
    this->parse_value(_default_string, strlen(_default_string), THREAD);
    if (HAS_PENDING_EXCEPTION) {
      fatal("Default string must be parseable");
    }
  } else {
    set_value(0);
  }
}

template <> void DCmdArgument<jlong>::destroy_value() { }

template <> void DCmdArgument<bool>::parse_value(const char* str,
                                                 size_t len, TRAPS) {
  // len is the length of the current token starting at str
  if (len == 0) {
    set_value(true);
  } else {
    if (len == strlen("true") && strncasecmp(str, "true", len) == 0) {
       set_value(true);
    } else if (len == strlen("false") && strncasecmp(str, "false", len) == 0) {
       set_value(false);
    } else {
      ResourceMark rm;

      char* buf = NEW_RESOURCE_ARRAY(char, len + 1);

PRAGMA_DIAG_PUSH
PRAGMA_STRINGOP_TRUNCATION_IGNORED
      // This code can incorrectly cause a "stringop-truncation" warning with gcc
      strncpy(buf, str, len);
PRAGMA_DIAG_POP

      buf[len] = '\0';
      Exceptions::fthrow(THREAD_AND_LOCATION, vmSymbols::java_lang_IllegalArgumentException(),
        "Boolean parsing error in command argument '%s'. Could not parse: %s.\n", _name, buf);
    }
  }
}

template <> void DCmdArgument<bool>::init_value(TRAPS) {
  if (has_default()) {
    this->parse_value(_default_string, strlen(_default_string), THREAD);
    if (HAS_PENDING_EXCEPTION) {
      fatal("Default string must be parsable");
    }
  } else {
    set_value(false);
  }
}

template <> void DCmdArgument<bool>::destroy_value() { }

template <> void DCmdArgument<char*>::parse_value(const char* str,
                                                  size_t len, TRAPS) {
  if (str == NULL) {
    _value = NULL;
  } else {
    _value = NEW_C_HEAP_ARRAY(char, len + 1, mtInternal);
    int n = os::snprintf(_value, len + 1, "%.*s", (int)len, str);
    assert((size_t)n <= len, "Unexpected number of characters in string");
  }
}

template <> void DCmdArgument<char*>::init_value(TRAPS) {
  if (has_default() && _default_string != NULL) {
    this->parse_value(_default_string, strlen(_default_string), THREAD);
    if (HAS_PENDING_EXCEPTION) {
     fatal("Default string must be parsable");
    }
  } else {
    set_value(NULL);
  }
}

template <> void DCmdArgument<char*>::destroy_value() {
  FREE_C_HEAP_ARRAY(char, _value);
  set_value(NULL);
}

template <> void DCmdArgument<NanoTimeArgument>::parse_value(const char* str,
                                                 size_t len, TRAPS) {
  if (str == NULL) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "Integer parsing error nanotime value: syntax error, value is null\n");
  }

  int argc = sscanf(str, JLONG_FORMAT, &_value._time);
  if (argc != 1) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "Integer parsing error nanotime value: syntax error\n");
  }
  size_t idx = 0;
  while(idx < len && isdigit(str[idx])) {
    idx++;
  }
  if (idx == len) {
    // only accept missing unit if the value is 0
    if (_value._time != 0) {
      THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
                "Integer parsing error nanotime value: unit required\n");
    } else {
      _value._nanotime = 0;
      strcpy(_value._unit, "ns");
      return;
    }
  } else if(len - idx > 2) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "Integer parsing error nanotime value: illegal unit\n");
  } else {
    strncpy(_value._unit, &str[idx], len - idx);
    /*Write an extra null termination. This is safe because _value._unit
     * is declared as char[3], and length is checked to be not larger than
     * two above. Also, this is necessary, since length might be 1, and the
     * default value already in the string is ns, which is two chars.
     */
    _value._unit[len-idx] = '\0';
  }

  if (strcmp(_value._unit, "ns") == 0) {
    _value._nanotime = _value._time;
  } else if (strcmp(_value._unit, "us") == 0) {
    _value._nanotime = _value._time * 1000;
  } else if (strcmp(_value._unit, "ms") == 0) {
    _value._nanotime = _value._time * 1000 * 1000;
  } else if (strcmp(_value._unit, "s") == 0) {
    _value._nanotime = _value._time * 1000 * 1000 * 1000;
  } else if (strcmp(_value._unit, "m") == 0) {
    _value._nanotime = _value._time * 60 * 1000 * 1000 * 1000;
  } else if (strcmp(_value._unit, "h") == 0) {
    _value._nanotime = _value._time * 60 * 60 * 1000 * 1000 * 1000;
  } else if (strcmp(_value._unit, "d") == 0) {
    _value._nanotime = _value._time * 24 * 60 * 60 * 1000 * 1000 * 1000;
  } else {
     THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
               "Integer parsing error nanotime value: illegal unit\n");
  }
}

template <> void DCmdArgument<NanoTimeArgument>::init_value(TRAPS) {
  if (has_default()) {
    this->parse_value(_default_string, strlen(_default_string), THREAD);
    if (HAS_PENDING_EXCEPTION) {
      fatal("Default string must be parsable");
    }
  } else {
    _value._time = 0;
    _value._nanotime = 0;
    strcpy(_value._unit, "ns");
  }
}

template <> void DCmdArgument<NanoTimeArgument>::destroy_value() { }

// WARNING StringArrayArgument can only be used as an option, it cannot be
// used as an argument with the DCmdParser

template <> void DCmdArgument<StringArrayArgument*>::parse_value(const char* str,
                                                  size_t len, TRAPS) {
  _value->add(str,len);
}

template <> void DCmdArgument<StringArrayArgument*>::init_value(TRAPS) {
  _value = new StringArrayArgument();
  _allow_multiple = true;
  if (has_default()) {
    fatal("StringArrayArgument cannot have default value");
  }
}

template <> void DCmdArgument<StringArrayArgument*>::destroy_value() {
  if (_value != NULL) {
    delete _value;
    set_value(NULL);
  }
}

template <> void DCmdArgument<MemorySizeArgument>::parse_value(const char* str,
                                                  size_t len, TRAPS) {
  if (str == NULL) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
               "Parsing error memory size value: syntax error, value is null\n");
  }
  if (*str == '-') {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
               "Parsing error memory size value: negative values not allowed\n");
  }
  int res = sscanf(str, UINT64_FORMAT "%c", &_value._val, &_value._multiplier);
  if (res == 2) {
     switch (_value._multiplier) {
      case 'k': case 'K':
         _value._size = _value._val * 1024;
         break;
      case 'm': case 'M':
         _value._size = _value._val * 1024 * 1024;
         break;
      case 'g': case 'G':
         _value._size = _value._val * 1024 * 1024 * 1024;
         break;
       default:
         _value._size = _value._val;
         _value._multiplier = ' ';
         //default case should be to break with no error, since user
         //can write size in bytes, or might have a delimiter and next arg
         break;
     }
   } else if (res == 1) {
     _value._size = _value._val;
   } else {
     THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
               "Parsing error memory size value: invalid value\n");
   }
}

template <> void DCmdArgument<MemorySizeArgument>::init_value(TRAPS) {
  if (has_default()) {
    this->parse_value(_default_string, strlen(_default_string), THREAD);
    if (HAS_PENDING_EXCEPTION) {
      fatal("Default string must be parsable");
    }
  } else {
    _value._size = 0;
    _value._val = 0;
    _value._multiplier = ' ';
  }
}

template <> void DCmdArgument<MemorySizeArgument>::destroy_value() { }
