OBJS = \
    Decoder.o

LIBRARY = libtextcodec.a

install:
	for dir in .; do \
	    mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibTextCodec/$$dir; \
	    cp $$dir/*.h $(SERENITY_BASE_DIR)/Root/usr/include/LibTextCodec/$$dir/; \
	done
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common

include ../../Makefile.subdir
