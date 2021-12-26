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

.PHONY: test
test:
	$(QUIET) $(MAKE) -C AK/Tests clean all clean
