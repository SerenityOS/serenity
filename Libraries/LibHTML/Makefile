include ../../Makefile.common

LIBRARY = libhtml.a

all: $(LIBRARY) tho

include Makefile.shared

tho: $(TEST_OBJS) $(LIBRARY)
	$(LD) -o $@ $(LDFLAGS) -L. $(TEST_OBJS) -lhtml -lgui -lcore -lc

$(LIBRARY): $(LIBHTML_OBJS)
	@echo "LIB $@"; $(AR) rcs $@ $(LIBHTML_OBJS)
