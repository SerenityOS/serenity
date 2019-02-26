#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

#define WNOHANG 1
pid_t wait(int* wstatus);

__END_DECLS
