#pragma once

#include <sys/cdefs.h>

#define no_argument        0
#define required_argument  1
#define optional_argument  2

struct option {
    const char *name;
    int has_arg;
    int* flag;
    int val;
};

__BEGIN_DECLS

int	getopt_long(int, char* const*, const char*, const struct option*, int*);
int	getopt_long_only(int, char* const*, const char*, const struct option*, int*);

#ifndef _GETOPT_DECLARED
#define	_GETOPT_DECLARED
int	 getopt(int, char * const [], const char *);
extern char *optarg;
extern int optind;
extern int opterr;
extern int optopt;
#endif

#ifndef _OPTRESET_DECLARED
#define	_OPTRESET_DECLARED
extern int optreset;
#endif

__END_DECLS

