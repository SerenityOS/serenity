include ../../Makefile.common

LIBRARY = libhtml.a

all: $(LIBRARY) tho

include Makefile.shared

tho: $(TEST_OBJS) $(LIBRARY)
	$(LD) -o $@ $(LDFLAGS) -L. $(TEST_OBJS) -lhtml -lgui -lcore -lc

$(LIBRARY): $(LIBHTML_OBJS)
	@echo "LIB $@"; $(AR) rcs $@ $(LIBHTML_OBJS)

install: $(LIBRARY)
	mkdir -p ../Root/usr/include/LibHTML
	# Copy headers
	rsync -r -a --include '*/' --include '*.h' --exclude '*' . ../Root/usr/include/LibHTML
	# Install the library
	cp $(LIBRARY) ../Root/usr/lib

