#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

extern char PC;
extern char* UP;
extern char* BC;

int tgetent(char* bp, const char* name);
int tgetflag(char* id);
int tgetnum(char* id);
char* tgetstr(char* id, char** area);
char* tgoto(const char* cap, int col, int row);
int tputs(const char* str, int affcnt, int (*putc)(int));

__END_DECLS

