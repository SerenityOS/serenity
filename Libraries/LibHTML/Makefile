include ../../Makefile.common

LIBRARY = libhtml.a

all: $(LIBRARY)

include Makefile.shared

$(LIBRARY): $(GENERATED_SOURCES) $(LIBHTML_OBJS)
	@echo "LIB $@"; $(AR) rcs $@ $(LIBHTML_OBJS)
