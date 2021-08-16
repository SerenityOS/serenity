/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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

/* hsdis.c -- dump a range of addresses as native instructions
   This implements the plugin protocol required by the
   HotSpot PrintAssembly option.
*/

#include <config.h> /* required by bfd.h */
#include <errno.h>
#include <inttypes.h>
#include <string.h>

#include <libiberty.h>
#include <bfd.h>
#include <bfdver.h>
#include <dis-asm.h>

#include "hsdis.h"

#ifndef bool
#define bool int
#define true 1
#define false 0
#endif /*bool*/

/* short names for stuff in hsdis.h */
typedef decode_instructions_event_callback_ftype  event_callback_t;
typedef decode_instructions_printf_callback_ftype printf_callback_t;

/* disassemble_info.application_data object */
struct hsdis_app_data {
  /* virtual address of data */
  uintptr_t start_va, end_va;
  /* the instructions to be decoded */
  unsigned char* buffer;
  uintptr_t length;
  event_callback_t  event_callback;  void* event_stream;
  printf_callback_t printf_callback; void* printf_stream;
  bool losing;
  bool do_newline;

  /* the architecture being disassembled */
  const char* arch_name;
  const bfd_arch_info_type* arch_info;

  /* the disassembler we are going to use: */
  disassembler_ftype      dfn;
  struct disassemble_info dinfo; /* the actual struct! */

  char mach_option[64];
  char insn_options[256];
};

static void* decode(struct hsdis_app_data* app_data, const char* options);

#define DECL_APP_DATA(dinfo) \
  struct hsdis_app_data* app_data = (struct hsdis_app_data*) (dinfo)->application_data

#define DECL_EVENT_CALLBACK(app_data) \
  event_callback_t  event_callback = (app_data)->event_callback; \
  void*             event_stream   = (app_data)->event_stream

#define DECL_PRINTF_CALLBACK(app_data) \
  printf_callback_t  printf_callback = (app_data)->printf_callback; \
  void*              printf_stream   = (app_data)->printf_stream


static void print_help(struct hsdis_app_data* app_data,
                       const char* msg, const char* arg);
static void setup_app_data(struct hsdis_app_data* app_data,
                           const char* options);
static const char* format_insn_close(const char* close,
                                     disassemble_info* dinfo,
                                     char* buf, size_t bufsize);

void*
#ifdef DLL_ENTRY
  DLL_ENTRY
#endif
decode_instructions_virtual(uintptr_t start_va, uintptr_t end_va,
                            unsigned char* buffer, uintptr_t length,
                            event_callback_t  event_callback_arg,  void* event_stream_arg,
                            printf_callback_t printf_callback_arg, void* printf_stream_arg,
                            const char* options, int newline) {
  struct hsdis_app_data app_data;
  memset(&app_data, 0, sizeof(app_data));
  app_data.start_va    = start_va;
  app_data.end_va      = end_va;
  app_data.buffer = buffer;
  app_data.length = length;
  app_data.event_callback  = event_callback_arg;
  app_data.event_stream    = event_stream_arg;
  app_data.printf_callback = printf_callback_arg;
  app_data.printf_stream   = printf_stream_arg;
  app_data.do_newline = newline == 0 ? false : true;

  return decode(&app_data, options);
}

/* This is the compatability interface for older version of hotspot */
void*
#ifdef DLL_ENTRY
  DLL_ENTRY
#endif
decode_instructions(void* start_pv, void* end_pv,
                    event_callback_t  event_callback_arg,  void* event_stream_arg,
                    printf_callback_t printf_callback_arg, void* printf_stream_arg,
                    const char* options) {
  return decode_instructions_virtual((uintptr_t)start_pv,
                                     (uintptr_t)end_pv,
                                     (unsigned char*)start_pv,
                                     (uintptr_t)end_pv - (uintptr_t)start_pv,
                                     event_callback_arg,
                                     event_stream_arg,
                                     printf_callback_arg,
                                     printf_stream_arg,
                                     options, false);
}

static void* decode(struct hsdis_app_data* app_data, const char* options) {
  setup_app_data(app_data, options);
  char buf[128];

  {
    /* now reload everything from app_data: */
    DECL_EVENT_CALLBACK(app_data);
    DECL_PRINTF_CALLBACK(app_data);
    uintptr_t start = app_data->start_va;
    uintptr_t end   = app_data->end_va;
    uintptr_t p     = start;

    (*event_callback)(event_stream, "insns", (void*)start);

    (*event_callback)(event_stream, "mach name='%s'",
                      (void*) app_data->arch_info->printable_name);
    if (app_data->dinfo.bytes_per_line != 0) {
      (*event_callback)(event_stream, "format bytes-per-line='%p'/",
                        (void*)(intptr_t) app_data->dinfo.bytes_per_line);
    }

    while (p < end && !app_data->losing) {
      (*event_callback)(event_stream, "insn", (void*) p);

      /* reset certain state, so we can read it with confidence */
      app_data->dinfo.insn_info_valid    = 0;
      app_data->dinfo.branch_delay_insns = 0;
      app_data->dinfo.data_size          = 0;
      app_data->dinfo.insn_type          = 0;

      int size = (*app_data->dfn)((bfd_vma) p, &app_data->dinfo);

      if (size > 0)  p += size;
      else           app_data->losing = true;

      if (!app_data->losing) {
        const char* insn_close = format_insn_close("/insn", &app_data->dinfo,
                                                   buf, sizeof(buf));
        (*event_callback)(event_stream, insn_close, (void*) p);

        if (app_data->do_newline) {
          /* follow each complete insn by a nice newline */
          (*printf_callback)(printf_stream, "\n");
        }
      }
    }

    if (app_data->losing) (*event_callback)(event_stream, "/insns", (void*) p);
    return (void*) p;
  }
}

/* take the address of the function, for luck, and also test the typedef: */
const decode_func_vtype decode_func_virtual_address = &decode_instructions_virtual;
const decode_func_stype decode_func_address = &decode_instructions;

static const char* format_insn_close(const char* close,
                                     disassemble_info* dinfo,
                                     char* buf, size_t bufsize) {
  if (!dinfo->insn_info_valid)
    return close;
  enum dis_insn_type itype = dinfo->insn_type;
  int dsize = dinfo->data_size, delays = dinfo->branch_delay_insns;
  if ((itype == dis_nonbranch && (dsize | delays) == 0)
      || (strlen(close) + 3*20 > bufsize))
    return close;

  const char* type = "unknown";
  switch (itype) {
  case dis_nonbranch:   type = NULL;         break;
  case dis_branch:      type = "branch";     break;
  case dis_condbranch:  type = "condbranch"; break;
  case dis_jsr:         type = "jsr";        break;
  case dis_condjsr:     type = "condjsr";    break;
  case dis_dref:        type = "dref";       break;
  case dis_dref2:       type = "dref2";      break;
  case dis_noninsn:     type = "noninsn";    break;
  }

  strcpy(buf, close);
  char* p = buf;
  if (type)    sprintf(p += strlen(p), " type='%s'", type);
  if (dsize)   sprintf(p += strlen(p), " dsize='%d'", dsize);
  if (delays)  sprintf(p += strlen(p), " delay='%d'", delays);
  return buf;
}

/* handler functions */

static int
hsdis_read_memory_func(bfd_vma memaddr,
                       bfd_byte* myaddr,
                       unsigned int length,
                       struct disassemble_info* dinfo) {
  DECL_APP_DATA(dinfo);
  /* convert the virtual address memaddr into an address within memory buffer */
  uintptr_t offset = ((uintptr_t) memaddr) - app_data->start_va;
  if (offset + length > app_data->length) {
    /* read is out of bounds */
    return EIO;
  } else {
    memcpy(myaddr, (bfd_byte*) (app_data->buffer + offset), length);
    return 0;
  }
}

static void
hsdis_print_address_func(bfd_vma vma, struct disassemble_info* dinfo) {
  /* the actual value to print: */
  void* addr_value = (void*) (uintptr_t) vma;
  DECL_APP_DATA(dinfo);
  DECL_EVENT_CALLBACK(app_data);

  /* issue the event: */
  void* result =
    (*event_callback)(event_stream, "addr/", addr_value);
  if (result == NULL) {
    /* event declined */
    generic_print_address(vma, dinfo);
  }
}


/* configuration */

static void set_optional_callbacks(struct hsdis_app_data* app_data);
static void parse_caller_options(struct hsdis_app_data* app_data,
                                 const char* caller_options);
static const char* native_arch_name();
static enum bfd_endian native_endian();
static const bfd_arch_info_type* find_arch_info(const char* arch_nane);
static bfd* get_native_bfd(const bfd_arch_info_type* arch_info,
                           /* to avoid malloc: */
                           bfd* empty_bfd, bfd_target* empty_xvec);
static void init_disassemble_info_from_bfd(struct disassemble_info* dinfo,
                                           void *stream,
                                           fprintf_ftype fprintf_func,
                                           bfd* bfd,
                                           char* disassembler_options);
static void parse_fake_insn(disassembler_ftype dfn,
                            struct disassemble_info* dinfo);

static void setup_app_data(struct hsdis_app_data* app_data,
                           const char* caller_options) {
  /* Make reasonable defaults for null callbacks.
     A non-null stream for a null callback is assumed to be a FILE* for output.
     Events are rendered as XML.
  */
  set_optional_callbacks(app_data);

  /* Look into caller_options for anything interesting. */
  if (caller_options != NULL)
    parse_caller_options(app_data, caller_options);

  /* Discover which architecture we are going to disassemble. */
  app_data->arch_name = &app_data->mach_option[0];
  if (app_data->arch_name[0] == '\0')
    app_data->arch_name = native_arch_name();
  app_data->arch_info = find_arch_info(app_data->arch_name);

  /* Make a fake bfd to hold the arch. and byteorder info. */
  struct {
    bfd_target empty_xvec;
    bfd        empty_bfd;
  } buf;
  bfd* native_bfd = get_native_bfd(app_data->arch_info,
                                   /* to avoid malloc: */
                                   &buf.empty_bfd, &buf.empty_xvec);
  init_disassemble_info_from_bfd(&app_data->dinfo,
                                 app_data->printf_stream,
                                 app_data->printf_callback,
                                 native_bfd,
                                 /* On PowerPC we get warnings, if we pass empty options */
                                 (caller_options == NULL) ? NULL : app_data->insn_options);

  /* Finish linking together the various callback blocks. */
  app_data->dinfo.application_data = (void*) app_data;
  app_data->dfn = disassembler(bfd_get_arch(native_bfd),
                               bfd_big_endian(native_bfd),
                               bfd_get_mach(native_bfd),
                               native_bfd);
  app_data->dinfo.print_address_func = hsdis_print_address_func;
  app_data->dinfo.read_memory_func = hsdis_read_memory_func;

  if (app_data->dfn == NULL) {
    const char* bad = app_data->arch_name;
    static bool complained;
    if (bad == &app_data->mach_option[0])
      print_help(app_data, "bad mach=%s", bad);
    else if (!complained)
      print_help(app_data, "bad native mach=%s; please port hsdis to this platform", bad);
    complained = true;
    /* must bail out */
    app_data->losing = true;
    return;
  }

  parse_fake_insn(app_data->dfn, &app_data->dinfo);
}


/* ignore all events, return a null */
static void* null_event_callback(void* ignore_stream, const char* ignore_event, void* arg) {
  return NULL;
}

/* print all events as XML markup */
static void* xml_event_callback(void* stream, const char* event, void* arg) {
  FILE* fp = (FILE*) stream;
#define NS_PFX "dis:"
  if (event[0] != '/') {
    /* issue the tag, with or without a formatted argument */
    fprintf(fp, "<"NS_PFX);
    fprintf(fp, event, arg);
    fprintf(fp, ">");
  } else {
    ++event;                    /* skip slash */
    const char* argp = strchr(event, ' ');
    if (argp == NULL) {
      /* no arguments; just issue the closing tag */
      fprintf(fp, "</"NS_PFX"%s>", event);
    } else {
      /* split out the closing attributes as <dis:foo_done attr='val'/> */
      int event_prefix = (argp - event);
      fprintf(fp, "<"NS_PFX"%.*s_done", event_prefix, event);
      fprintf(fp, argp, arg);
      fprintf(fp, "/></"NS_PFX"%.*s>", event_prefix, event);
    }
  }
  return NULL;
}

static void set_optional_callbacks(struct hsdis_app_data* app_data) {
  if (app_data->printf_callback == NULL) {
    int (*fprintf_callback)(FILE*, const char*, ...) = &fprintf;
    FILE* fprintf_stream = stdout;
    app_data->printf_callback = (printf_callback_t) fprintf_callback;
    if (app_data->printf_stream == NULL)
      app_data->printf_stream   = (void*)           fprintf_stream;
  }
  if (app_data->event_callback == NULL) {
    if (app_data->event_stream == NULL)
      app_data->event_callback = &null_event_callback;
    else
      app_data->event_callback = &xml_event_callback;
  }

}

static void parse_caller_options(struct hsdis_app_data* app_data, const char* caller_options) {
  char* iop_base = app_data->insn_options;
  char* iop_limit = iop_base + sizeof(app_data->insn_options) - 1;
  char* iop = iop_base;
  const char* p;
  for (p = caller_options; p != NULL; ) {
    const char* q = strchr(p, ',');
    size_t plen = (q == NULL) ? strlen(p) : ((q++) - p);
    if (plen == 4 && strncmp(p, "help", plen) == 0) {
      print_help(app_data, NULL, NULL);
    } else if (plen >= 5 && strncmp(p, "mach=", 5) == 0) {
      char*  mach_option = app_data->mach_option;
      size_t mach_size   = sizeof(app_data->mach_option);
      mach_size -= 1;           /*leave room for the null*/
      if (plen > mach_size)  plen = mach_size;
      strncpy(mach_option, p, plen);
      mach_option[plen] = '\0';
    } else if (plen > 6 && strncmp(p, "hsdis-", 6) == 0) {
      // do not pass these to the next level
    } else {
      /* just copy it; {i386,sparc}-dis.c might like to see it  */
      if (iop > iop_base && iop < iop_limit)  (*iop++) = ',';
      if (iop + plen > iop_limit)
        plen = iop_limit - iop;
      strncpy(iop, p, plen);
      iop += plen;
    }
    p = q;
  }
  *iop = '\0';
}

static void print_help(struct hsdis_app_data* app_data,
                       const char* msg, const char* arg) {
  DECL_PRINTF_CALLBACK(app_data);
  if (msg != NULL) {
    (*printf_callback)(printf_stream, "hsdis: ");
    (*printf_callback)(printf_stream, msg, arg);
    (*printf_callback)(printf_stream, "\n");
  }
  (*printf_callback)(printf_stream, "hsdis output options:\n");
  if (printf_callback == (printf_callback_t) &fprintf)
    disassembler_usage((FILE*) printf_stream);
  else
    disassembler_usage(stderr); /* better than nothing */
  (*printf_callback)(printf_stream, "  mach=<arch>   select disassembly mode\n");
#if defined(LIBARCH_i386) || defined(LIBARCH_amd64)
  (*printf_callback)(printf_stream, "  mach=i386     select 32-bit mode\n");
  (*printf_callback)(printf_stream, "  mach=x86-64   select 64-bit mode\n");
  (*printf_callback)(printf_stream, "  suffix        always print instruction suffix\n");
#endif
  (*printf_callback)(printf_stream, "  help          print this message\n");
}


/* low-level bfd and arch stuff that binutils doesn't do for us */

static const bfd_arch_info_type* find_arch_info(const char* arch_name) {
  const bfd_arch_info_type* arch_info = bfd_scan_arch(arch_name);
  if (arch_info == NULL) {
    extern const bfd_arch_info_type bfd_default_arch_struct;
    arch_info = &bfd_default_arch_struct;
  }
  return arch_info;
}

static const char* native_arch_name() {
  const char* res = NULL;
#ifdef LIBARCH_i386
  res = "i386";
#endif
#ifdef LIBARCH_amd64
  res = "i386:x86-64";
#endif
#if  defined(LIBARCH_ppc64) || defined(LIBARCH_ppc64le)
  res = "powerpc:common64";
#endif
#ifdef LIBARCH_arm
  res = "arm";
#endif
#ifdef LIBARCH_aarch64
  res = "aarch64";
#endif
#ifdef LIBARCH_s390x
  res = "s390:64-bit";
#endif
  if (res == NULL)
    res = "architecture not set in Makefile!";
  return res;
}

static enum bfd_endian native_endian() {
  int32_t endian_test = 'x';
  if (*(const char*) &endian_test == 'x')
    return BFD_ENDIAN_LITTLE;
  else
    return BFD_ENDIAN_BIG;
}

static bfd* get_native_bfd(const bfd_arch_info_type* arch_info,
                           bfd* empty_bfd, bfd_target* empty_xvec) {
  memset(empty_bfd,  0, sizeof(*empty_bfd));
  memset(empty_xvec, 0, sizeof(*empty_xvec));
  empty_xvec->flavour = bfd_target_unknown_flavour;
  empty_xvec->byteorder = native_endian();
  empty_bfd->xvec = empty_xvec;
  empty_bfd->arch_info = arch_info;
  return empty_bfd;
}

static int read_zero_data_only(bfd_vma ignore_p,
                               bfd_byte* myaddr, unsigned int length,
                               struct disassemble_info *ignore_info) {
  memset(myaddr, 0, length);
  return 0;
}
static int print_to_dev_null(void* ignore_stream, const char* ignore_format, ...) {
  return 0;
}

/* Prime the pump by running the selected disassembler on a null input.
   This forces the machine-specific disassembler to divulge invariant
   information like bytes_per_line.
 */
static void parse_fake_insn(disassembler_ftype dfn,
                            struct disassemble_info* dinfo) {
  typedef int (*read_memory_ftype)
    (bfd_vma memaddr, bfd_byte *myaddr, unsigned int length,
     struct disassemble_info *info);
  read_memory_ftype read_memory_func = dinfo->read_memory_func;
  fprintf_ftype     fprintf_func     = dinfo->fprintf_func;

  dinfo->read_memory_func = &read_zero_data_only;
  dinfo->fprintf_func     = &print_to_dev_null;
  (*dfn)(0, dinfo);

  /* put it back */
  dinfo->read_memory_func = read_memory_func;
  dinfo->fprintf_func     = fprintf_func;
}

static void init_disassemble_info_from_bfd(struct disassemble_info* dinfo,
                                           void *stream,
                                           fprintf_ftype fprintf_func,
                                           bfd* abfd,
                                           char* disassembler_options) {
  init_disassemble_info(dinfo, stream, fprintf_func);

  dinfo->flavour = bfd_get_flavour(abfd);
  dinfo->arch = bfd_get_arch(abfd);
  dinfo->mach = bfd_get_mach(abfd);
  dinfo->disassembler_options = disassembler_options;
#if BFD_VERSION >= 234000000
  /* bfd_octets_per_byte() has 2 args since binutils 2.34 */
  dinfo->octets_per_byte = bfd_octets_per_byte (abfd, NULL);
#else
  dinfo->octets_per_byte = bfd_octets_per_byte (abfd);
#endif
  dinfo->skip_zeroes = sizeof(void*) * 2;
  dinfo->skip_zeroes_at_end = sizeof(void*)-1;
  dinfo->disassembler_needs_relocs = FALSE;

  if (bfd_big_endian(abfd))
    dinfo->display_endian = dinfo->endian = BFD_ENDIAN_BIG;
  else if (bfd_little_endian(abfd))
    dinfo->display_endian = dinfo->endian = BFD_ENDIAN_LITTLE;
  else
    dinfo->endian = native_endian();

  disassemble_init_for_target(dinfo);
}
