SUBDIRS += \
	AK \
	Applications \
	DevTools \
	Kernel \
	Libraries \
	MenuApplets \
	Servers \
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
	$(QUIET) $(MAKE) -C AK/Tests clean all clean
endif

