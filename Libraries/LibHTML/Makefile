include ../../Makefile.common

LIBRARY = libhtml.a

all: $(LIBRARY)

include Makefile.shared

$(LIBRARY): $(LIBHTML_OBJS)
	@echo "LIB $@"; $(AR) rcs $@ $(LIBHTML_OBJS)
