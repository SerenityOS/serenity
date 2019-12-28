SUBDIRS += \
	AK \
	DevTools \
	Libraries \
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

include Makefile.subdir

all: subdirs

.PHONY: test
UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Darwin)
test: 
else
test:
	$(QUIET) flock AK/Tests $(MAKE) -C AK/Tests clean all clean
endif

