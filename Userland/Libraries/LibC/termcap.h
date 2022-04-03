/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

extern char PC;
extern char* UP;
extern char* BC;

int tgetent(char* bp, char const* name);
int tgetflag(char const* id);
int tgetnum(char const* id);
char* tgetstr(char const* id, char** area);
char* tgoto(char const* cap, int col, int row);
int tputs(char const* str, int affcnt, int (*putc)(int));

__END_DECLS
