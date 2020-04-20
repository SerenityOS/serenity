LIBTLS_OBJS = TLSv12.o

OBJS = $(LIBTLS_OBJS)

LIBRARY = libtls.a

install:
	for dir in .; do \
	    mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibTLS/$$dir; \
	    cp $$dir/*.h $(SERENITY_BASE_DIR)/Root/usr/include/LibTLS/$$dir/; \
	done
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common

include ../../Makefile.subdir
