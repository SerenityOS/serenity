LIBCRYPTO_OBJS = \
	   	Cipher/AES.o \
		Hash/MD5.o \
		Hash/SHA2.o \
		PK/RSA.o \
		BigInt/UnsignedBigInteger.o

OBJS = $(LIBCRYPTO_OBJS)

LIBRARY = libcrypto.a

install:
	for dir in . Cipher Cipher/Mode Hash PK PK/Code; do \
	    mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibCrypto/$$dir; \
	    cp $$dir/*.h $(SERENITY_BASE_DIR)/Root/usr/include/LibCrypto/$$dir/; \
	done
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common

include ../../Makefile.subdir
