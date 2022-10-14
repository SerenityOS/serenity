/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <stddef.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

typedef void* iconv_t;

extern iconv_t iconv_open(char const* tocode, char const* fromcode);
extern size_t iconv(iconv_t, char** inbuf, size_t* inbytesleft, char** outbuf, size_t* outbytesleft);
extern int iconv_close(iconv_t);

__END_DECLS
