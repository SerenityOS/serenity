SUBDIRS += \
	Libraries \
	AK \
	DevTools \
	Servers

SUBDIRS += \
	Applications \
	Kernel \
	MenuApplets \
	Shell \
	Userland

SUBDIRS += \
	Games \
	Demos


MAKE_PID := $(shell echo $$PPID)
JOBS := $(shell ps T | sed -n 's/.*$(MAKE_PID).*$(MAKE).* \(-j\|--jobs\) *\([0-9][0-9]*\).*/\2/p')

ifeq (,$(JOBS))
	NPROC := $(shell nproc)
	PARALLEL_BUILD := -j $(NPROC)
else
	PARALLEL_BUILD := -j $(JOBS)
endif
MAKEFLAGS := $(MAKEFLAGS) $(PARALLEL_BUILD)

include Makefile.subdir
all: subdirs

.PHONY: test
UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Darwin)
test: 
else
test:
	$(QUIET) flock AK/Tests -c "$(MAKE) $(PARALLEL_BUILD) -C AK/Tests clean all && $(MAKE) $(PARALLEL_BUILD) -C AK/Tests clean"
endif

