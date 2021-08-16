/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2019 SAP SE. All rights reserved.
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


// Implementation of LoadedLibraries and friends

// Ultimately this just uses loadquery()
// See:
// http://publib.boulder.ibm.com/infocenter/pseries/v5r3/index.jsp
//      ?topic=/com.ibm.aix.basetechref/doc/basetrf1/loadquery.htm

#include "loadlib_aix.hpp"
#include "misc_aix.hpp"
#include "porting_aix.hpp"
#include "utilities/debug.hpp"
#include "utilities/ostream.hpp"

// For loadquery()
#include <sys/ldr.h>

// Use raw malloc instead of os::malloc - this code gets used for error reporting.

// A class to "intern" eternal strings.
// TODO: similar coding exists in AIX version of dladdr and potentially elsewhere: consolidate!
class StringList {

  char** _list;
  int _cap;
  int _num;

  // Enlarge list. If oom, leave old list intact and return false.
  bool enlarge() {
    int cap2 = _cap + 64;
    char** l2 = (char**) ::realloc(_list, sizeof(char*) * cap2);
    if (!l2) {
      return false;
    }
    _list = l2;
    _cap = cap2;
    return true;
  }

  // Append string to end of list.
  // Returns NULL if oom.
  char* append(const char* s) {
    if (_cap == _num) {
      if (!enlarge()) {
        return NULL;
      }
    }
    assert0(_cap > _num);
    char* s2 = ::strdup(s);
    if (!s2) {
      return NULL;
    }
    _list[_num] = s2;
    trcVerbose("StringDir: added %s at pos %d", s2, _num);
    _num ++;
    return s2;
  }

public:

  StringList()
    : _list(NULL)
    , _cap(0)
    , _num(0)
  {}

  // String is copied into the list; pointer to copy is returned.
  // Returns NULL if oom.
  char* add (const char* s) {
    for (int i = 0; i < _num; i++) {
      if (strcmp(_list[i], s) == 0) {
        return _list[i];
      }
    }
    return append(s);
  }

};

static StringList g_stringlist;

//////////////////////

// Entries are kept in a linked list ordered by text address. Entries are not
// eternal - this list is rebuilt on every reload.
// Note that we do not hand out those entries, but copies of them.

struct entry_t {
  entry_t* next;
  loaded_module_t info;
};

static void print_entry(const entry_t* e, outputStream* os) {
  const loaded_module_t* const lm = &(e->info);
  os->print(" %c text: " INTPTR_FORMAT " - " INTPTR_FORMAT
            ", data: " INTPTR_FORMAT " - " INTPTR_FORMAT " "
            "%s",
      (lm->is_in_vm ? '*' : ' '),
      lm->text, (uintptr_t)lm->text + lm->text_len,
      lm->data, (uintptr_t)lm->data + lm->data_len,
      lm->path);
  if (lm->member) {
    os->print("(%s)", lm->member);
  }
}

static entry_t* g_first = NULL;

static entry_t* find_entry_for_text_address(const void* p) {
  for (entry_t* e = g_first; e; e = e->next) {
    if ((uintptr_t)p >= (uintptr_t)e->info.text &&
        (uintptr_t)p < ((uintptr_t)e->info.text + e->info.text_len)) {
      return e;
    }
  }
  return NULL;
}

static entry_t* find_entry_for_data_address(const void* p) {
  for (entry_t* e = g_first; e; e = e->next) {
    if ((uintptr_t)p >= (uintptr_t)e->info.data &&
        (uintptr_t)p < ((uintptr_t)e->info.data + e->info.data_len)) {
      return e;
    }
  }
  return NULL;
}

// Adds a new entry to the list (ordered by text address ascending).
static void add_entry_to_list(entry_t* e, entry_t** start) {
  entry_t* last = NULL;
  entry_t* e2 = *start;
  while (e2 && e2->info.text < e->info.text) {
    last = e2;
    e2 = e2->next;
  }
  if (last) {
    last->next = e;
  } else {
    *start = e;
  }
  e->next = e2;
}

static void free_entry_list(entry_t** start) {
  entry_t* e = *start;
  while (e) {
    entry_t* const e2 = e->next;
    ::free(e);
    e = e2;
  }
  *start = NULL;
}


// Rebuild the internal module table. If an error occurs, old table remains
// unchanged.
static bool reload_table() {

  bool rc = false;

  trcVerbose("reload module table...");

  entry_t* new_list = NULL;
  const struct ld_info* ldi = NULL;

  // Call loadquery(L_GETINFO..) to get a list of all loaded Dlls from AIX. loadquery
  // requires a large enough buffer.
  uint8_t* buffer = NULL;
  size_t buflen = 1024;
  for (;;) {
    buffer = (uint8_t*) ::realloc(buffer, buflen);
    if (loadquery(L_GETINFO, buffer, buflen) == -1) {
      if (errno == ENOMEM) {
        buflen *= 2;
      } else {
        trcVerbose("loadquery failed (%d)", errno);
        goto cleanup;
      }
    } else {
      break;
    }
  }

  trcVerbose("loadquery buffer size is " SIZE_FORMAT ".", buflen);

  // Iterate over the loadquery result. For details see sys/ldr.h on AIX.
  ldi = (struct ld_info*) buffer;

  for (;;) {

    entry_t* e = (entry_t*) ::malloc(sizeof(entry_t));
    if (!e) {
      trcVerbose("OOM.");
      goto cleanup;
    }

    memset(e, 0, sizeof(entry_t));

    e->info.text = ldi->ldinfo_textorg;
    e->info.text_len = ldi->ldinfo_textsize;
    e->info.data = ldi->ldinfo_dataorg;
    e->info.data_len = ldi->ldinfo_datasize;

    e->info.path = g_stringlist.add(ldi->ldinfo_filename);
    if (!e->info.path) {
      trcVerbose("OOM.");
      goto cleanup;
    }

    // Extract short name
    {
      const char* p = strrchr(e->info.path, '/');
      if (p) {
        p ++;
        e->info.shortname = p;
      } else {
        e->info.shortname = e->info.path;
      }
    }

    // Do we have a member name as well (see ldr.h)?
    const char* p_mbr_name =
      ldi->ldinfo_filename + strlen(ldi->ldinfo_filename) + 1;
    if (*p_mbr_name) {
      e->info.member = g_stringlist.add(p_mbr_name);
      if (!e->info.member) {
        trcVerbose("OOM.");
        goto cleanup;
      }
    } else {
      e->info.member = NULL;
    }

    if (strcmp(e->info.shortname, "libjvm.so") == 0) {
      // Note that this, theoretically, is fuzzy. We may accidentally contain
      // more than one libjvm.so. But that is improbable, so lets go with this
      // solution.
      e->info.is_in_vm = true;
    }

    trcVerbose("entry: %p " SIZE_FORMAT ", %p " SIZE_FORMAT ", %s %s %s, %d",
      e->info.text, e->info.text_len,
      e->info.data, e->info.data_len,
      e->info.path, e->info.shortname,
      (e->info.member ? e->info.member : "NULL"),
      e->info.is_in_vm
    );

    // Add to list.
    add_entry_to_list(e, &new_list);

    // Next entry...
    if (ldi->ldinfo_next) {
      ldi = (struct ld_info*)(((char*)ldi) + ldi->ldinfo_next);
    } else {
      break;
    }
  }

  // We are done. All is well. Free old list and swap to new one.
  if (g_first) {
    free_entry_list(&g_first);
  }
  g_first = new_list;
  new_list = NULL;

  rc = true;

cleanup:

  if (new_list) {
    free_entry_list(&new_list);
  }

  ::free(buffer);

  return rc;

} // end LoadedLibraries::reload()


///////////////////////////////////////////////////////////////////////////////
// Externals

static MiscUtils::CritSect g_cs;

// Rebuild the internal module table. If an error occurs, old table remains
// unchanged.
bool LoadedLibraries::reload() {
  MiscUtils::AutoCritSect lck(&g_cs);
  return reload_table();
}

void LoadedLibraries::print(outputStream* os) {
  MiscUtils::AutoCritSect lck(&g_cs);
  if (!g_first) {
    reload_table();
  }
  for (entry_t* e = g_first; e; e = e->next) {
    print_entry(e, os);
    os->cr();
  }
}

bool LoadedLibraries::find_for_text_address(const void* p,
                                            loaded_module_t* info) {
  MiscUtils::AutoCritSect lck(&g_cs);
  if (!g_first) {
    reload_table();
  }
  const entry_t* const e = find_entry_for_text_address(p);
  if (e) {
    if (info) {
      *info = e->info;
    }
    return true;
  }
  return false;
}


bool LoadedLibraries::find_for_data_address (
  const void* p,
  loaded_module_t* info // optional. can be NULL:
) {
  MiscUtils::AutoCritSect lck(&g_cs);
  if (!g_first) {
    reload_table();
  }
  const entry_t* const e = find_entry_for_data_address(p);
  if (e) {
    if (info) {
      *info = e->info;
    }
    return true;
  }
  return false;
}

