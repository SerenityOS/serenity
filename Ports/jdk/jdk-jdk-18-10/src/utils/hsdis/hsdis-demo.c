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

/* hsdis-demo.c -- dump a range of addresses as native instructions
   This demonstrates the protocol required by the HotSpot PrintAssembly option.
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <inttypes.h>

#include "hsdis.h"


void greet(const char*);
void disassemble(uintptr_t, uintptr_t);
void end_of_file();

const char* options = NULL;
int         raw     = 0;
int         xml     = 0;

int main(int ac, char** av) {
  int greeted = 0;
  int i;
  for (i = 1; i < ac; i++) {
    const char* arg = av[i];
    if (arg[0] == '-') {
      if (!strcmp(arg, "-xml"))
        xml ^= 1;
      else if (!strcmp(arg, "-raw"))
        raw ^= 1;
      else if (!strncmp(arg, "-options=", 9))
        options = arg+9;
      else
        { printf("Usage: %s [-xml] [name...]\n", av[0]); exit(2); }
      continue;
    }
    greet(arg);
    greeted = 1;
  }
  if (!greeted)
    greet("world");
  printf("...And now for something completely different:\n");
  void *start = (void*) &main;
  void *end = (void*) &end_of_file;
#if defined(__ia64) || (defined(__powerpc__) && !defined(ABI_ELFv2))
  /* On IA64 and PPC function pointers are pointers to function descriptors */
  start = *((void**)start);
  end = *((void**)end);
#endif
  disassemble(start, (end > start) ? end : start + 64);
  printf("Cheers!\n");
}

void greet(const char* whom) {
  printf("Hello, %s!\n", whom);
}

void end_of_file() { }

/* don't disassemble after this point... */

#include "dlfcn.h"

#define DECODE_INSTRUCTIONS_VIRTUAL_NAME "decode_instructions_virtual"
#define DECODE_INSTRUCTIONS_NAME "decode_instructions"
#define HSDIS_NAME               "hsdis"
static void* decode_instructions_pv = 0;
static void* decode_instructions_sv = 0;
static const char* hsdis_path[] = {
  HSDIS_NAME"-"LIBARCH LIB_EXT,
  "./" HSDIS_NAME"-"LIBARCH LIB_EXT,
#ifdef TARGET_DIR
  TARGET_DIR"/"HSDIS_NAME"-"LIBARCH LIB_EXT,
#endif
  NULL
};

static const char* load_decode_instructions() {
  void* dllib = NULL;
  const char* *next_in_path = hsdis_path;
  while (1) {
    decode_instructions_pv = dlsym(dllib, DECODE_INSTRUCTIONS_VIRTUAL_NAME);
    decode_instructions_sv = dlsym(dllib, DECODE_INSTRUCTIONS_NAME);
    if (decode_instructions_pv != NULL || decode_instructions_sv != NULL)
      return NULL;
    if (dllib != NULL)
      return "plugin does not defined "DECODE_INSTRUCTIONS_VIRTUAL_NAME" and "DECODE_INSTRUCTIONS_NAME;
    for (dllib = NULL; dllib == NULL; ) {
      const char* next_lib = (*next_in_path++);
      if (next_lib == NULL)
        return "cannot find plugin "HSDIS_NAME LIB_EXT;
      dllib = dlopen(next_lib, RTLD_LAZY);
    }
  }
}


static const char* lookup(void* addr) {
#if defined(__ia64) || defined(__powerpc__)
  /* On IA64 and PPC function pointers are pointers to function descriptors */
#define CHECK_NAME(fn) \
  if (addr == *((void**) &fn))  return #fn;
#else
#define CHECK_NAME(fn) \
  if (addr == (void*) &fn)  return #fn;
#endif

  CHECK_NAME(main);
  CHECK_NAME(greet);
  return NULL;
}

/* does the event match the tag, followed by a null, space, or slash? */
#define MATCH(event, tag) \
  (!strncmp(event, tag, sizeof(tag)-1) && \
   (!event[sizeof(tag)-1] || strchr(" /", event[sizeof(tag)-1])))


static const char event_cookie[] = "event_cookie"; /* demo placeholder */
static void* simple_handle_event(void* cookie, const char* event, void* arg) {
  if (MATCH(event, "/insn")) {
    // follow each complete insn by a nice newline
    printf("\n");
  }
  return NULL;
}

static void* handle_event(void* cookie, const char* event, void* arg) {
#define NS_DEMO "demo:"
  if (cookie != event_cookie)
    printf("*** bad event cookie %p != %p\n", cookie, event_cookie);

  if (xml) {
    /* We could almost do a printf(event, arg),
       but for the sake of a better demo,
       we dress the result up as valid XML.
    */
    const char* fmt = strchr(event, ' ');
    int evlen = (fmt ? fmt - event : strlen(event));
    if (!fmt) {
      if (event[0] != '/') {
        printf("<"NS_DEMO"%.*s>", evlen, event);
      } else {
        printf("</"NS_DEMO"%.*s>", evlen-1, event+1);
      }
    } else {
      if (event[0] != '/') {
        printf("<"NS_DEMO"%.*s", evlen, event);
        printf(fmt, arg);
        printf(">");
      } else {
        printf("<"NS_DEMO"%.*s_done", evlen-1, event+1);
        printf(fmt, arg);
        printf("/></"NS_DEMO"%.*s>", evlen-1, event+1);
      }
    }
  }

  if (MATCH(event, "insn")) {
    const char* name = lookup(arg);
    if (name)  printf("%s:\n", name);

    /* basic action for <insn>: */
    printf(" %p\t", arg);

  } else if (MATCH(event, "/insn")) {
    // follow each complete insn by a nice newline
    printf("\n");
  } else if (MATCH(event, "mach")) {
    printf("Decoding for CPU '%s'\n", (char*) arg);

  } else if (MATCH(event, "addr")) {
    /* basic action for <addr/>: */
    const char* name = lookup(arg);
    if (name) {
      printf("&%s (%p)", name, arg);
      /* return non-null to notify hsdis not to print the addr */
      return arg;
    }
  }

  /* null return is always safe; can mean "I ignored it" */
  return NULL;
}

#define fprintf_callback \
  (decode_instructions_printf_callback_ftype)&fprintf

void disassemble(uintptr_t from, uintptr_t to) {
  const char* err = load_decode_instructions();
  if (err != NULL) {
    printf("%s: %s\n", err, dlerror());
    exit(1);
  }
  decode_func_vtype decode_instructions_v
    = (decode_func_vtype) decode_instructions_pv;
  decode_func_stype decode_instructions_s
    = (decode_func_stype) decode_instructions_sv;
  void* res;
  if (decode_instructions_pv != NULL) {
    printf("\nDecoding from %p to %p...with %s\n", from, to, DECODE_INSTRUCTIONS_VIRTUAL_NAME);
    if (raw) {
      res = (*decode_instructions_v)(from, to,
                                     (unsigned char*)from, to - from,
                                     simple_handle_event, stdout,
                                     NULL, stdout,
                                     options, 0);
    } else {
      res = (*decode_instructions_v)(from, to,
                                    (unsigned char*)from, to - from,
                                     handle_event, (void*) event_cookie,
                                     fprintf_callback, stdout,
                                     options, 0);
    }
    if (res != (void*)to)
      printf("*** Result was %p!\n", res);
  }
  void* sres;
  if (decode_instructions_sv != NULL) {
    printf("\nDecoding from %p to %p...with old decode_instructions\n", from, to, DECODE_INSTRUCTIONS_NAME);
    if (raw) {
      sres = (*decode_instructions_s)(from, to,
                                      simple_handle_event, stdout,
                                      NULL, stdout,
                                      options);
    } else {
      sres = (*decode_instructions_s)(from, to,
                                      handle_event, (void*) event_cookie,
                                      fprintf_callback, stdout,
                                      options);
    }
    if (sres != (void *)to)
      printf("*** Result of decode_instructions %p!\n", sres);
  }
}
