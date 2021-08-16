#
# Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
# Copyright (c) 2021, Azul Systems, Inc. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# The Universal Permissive License (UPL), Version 1.0
#
# Subject to the condition set forth below, permission is hereby granted to
# any person obtaining a copy of this software, associated documentation
# and/or data (collectively the "Software"), free of charge and under any
# and all copyright rights in the Software, and any and all patent rights
# owned or freely licensable by each licensor hereunder covering either (i)
# the unmodified Software as contributed to or provided by such licensor,
# or (ii) the Larger Works (as defined below), to deal in both
#
# (a) the Software, and
#
# (b) any piece of software and/or hardware listed in the lrgrwrks.txt file
# if one is included with the Software (each a "Larger Work" to which the
# Software is contributed by such licensors),
#
# without restriction, including without limitation the rights to copy,
# create derivative works of, display, perform, and distribute the Software
# and make, use, sell, offer for sale, import, export, have made, and have
# sold the Software and the Larger Work(s), and to sublicense the foregoing
# rights on either these or other terms.
#
# This license is subject to the following condition:
#
# The above copyright notice and either this complete permission notice or
# at a minimum a reference to the UPL must be included in all copies or
# substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
# OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
# NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
# DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
# OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
# USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#
#

# Single gnu makefile for linux and windows (windows requires cygwin and mingw)

# Default arch; it is changed below as needed.
ARCH		= i386
OS		= $(shell uname)
AR		= ar

## OS = Linux ##
ifeq		($(OS),Linux)
ifneq           ($(MINGW),)
LIB_EXT		= .dll
CPPFLAGS += -I$(TARGET_DIR)/include
LDFLAGS += -L$(TARGET_DIR)/lib
OS=windows
ifneq           ($(findstring x86_64-,$(MINGW)),)
ARCH=amd64
else
ARCH=i386
endif
CC 		= $(MINGW)-gcc
CONFIGURE_ARGS= --host=$(MINGW) --target=$(MINGW)
else   #linux
CPU             = $(shell uname -m)
ARCH1=$(CPU:x86_64=amd64)
ARCH=$(ARCH1:i686=i386)
ifdef LP64
CFLAGS/amd64	+= -m64
CFLAGS/ppc64	+= -m64
CFLAGS/ppc64le  += -m64 -DABI_ELFv2
else
ARCH=$(ARCH1:amd64=i386)
ifneq ($(findstring arm,$(ARCH)),)
ARCH=arm
endif
CFLAGS/i386	+= -m32
endif
CFLAGS		+= $(CFLAGS/$(ARCH))
CFLAGS		+= -fPIC
OS		= linux
LIB_EXT		= .so
CC 		= gcc
endif
CFLAGS		+= -O
DLDFLAGS	+= -shared
LDFLAGS         += -ldl
OUTFLAGS	+= -o $@
else
## OS = AIX ##
ifeq		($(OS),AIX)
OS              = aix
ARCH            = ppc64
CC              = xlc_r
CFLAGS          += -DAIX -g -qpic=large -q64
CFLAGS/ppc64    += -q64
AR              = ar -X64
DLDFLAGS        += -qmkshrobj -lz
OUTFLAGS        += -o $@
LIB_EXT		= .so
else
## OS = Darwin ##
ifeq ($(OS),Darwin)
CPU             = $(shell uname -m)
ARCH1=$(CPU:x86_64=amd64)
ARCH2=$(ARCH1:arm64=aarch64)
ARCH=$(ARCH2:i686=i386)
CONFIGURE_ARGS/aarch64= --enable-targets=aarch64-darwin
CONFIGURE_ARGS = $(CONFIGURE_ARGS/$(ARCH))
ifdef LP64
CFLAGS/amd64    += -m64
else
ARCH=$(ARCH2:amd64=i386)
CFLAGS/i386     += -m32
endif # LP64
ifeq ($(CPU), arm64)
CFLAGS/aarch64  += -m64
endif # arm64
CFLAGS          += $(CFLAGS/$(ARCH))
CFLAGS          += -fPIC
OS              = macosx
LIB_EXT         = .dylib
CC              = gcc
CFLAGS          += -O
# CFLAGS        += -DZ_PREFIX
DLDFLAGS        += -shared
DLDFLAGS        += -lz
LDFLAGS         += -ldl
OUTFLAGS        += -o $@
else
## OS = Windows ##
OS		= windows
CC		= gcc
CFLAGS		+=  /nologo /MD /W3 /WX /O2 /Fo$(@:.dll=.obj) /Gi-
CFLAGS		+= LIBARCH=\"$(LIBARCH)\"
DLDFLAGS	+= /dll /subsystem:windows /incremental:no \
			/export:decode_instruction
OUTFLAGS	+= /link /out:$@
LIB_EXT		= .dll
endif   # Darwin
endif   # AIX
endif	# Linux

LIBARCH		= $(ARCH)
ifdef		LP64
LIBARCH64/i386	= amd64
LIBARCH64	= $(LIBARCH64/$(ARCH))
ifneq		($(LIBARCH64),)
LIBARCH		= $(LIBARCH64)
endif   # LIBARCH64/$(ARCH)
endif   # LP64

JDKARCH=$(LIBARCH:i386=i586)

ifeq            ($(BINUTILS),)
# Pop all the way out of the workspace to look for binutils.
# ...You probably want to override this setting.
BINUTILSDIR	= $(shell cd build/binutils;pwd)
else
BINUTILSDIR	= $(shell cd $(BINUTILS);pwd)
endif

CPPFLAGS	+= -I$(BINUTILSDIR)/include -I$(BINUTILSDIR)/bfd -I$(TARGET_DIR)/bfd
CPPFLAGS	+= -DLIBARCH_$(LIBARCH) -DLIBARCH=\"$(LIBARCH)\" -DLIB_EXT=\"$(LIB_EXT)\"

TARGET_DIR	= build/$(OS)-$(JDKARCH)
TARGET		= $(TARGET_DIR)/hsdis-$(LIBARCH)$(LIB_EXT)

SOURCE		= hsdis.c

LIBRARIES =	$(TARGET_DIR)/bfd/libbfd.a \
		$(TARGET_DIR)/opcodes/libopcodes.a \
		$(TARGET_DIR)/libiberty/libiberty.a

ifneq ($(MINGW),)
LIBRARIES +=	$(TARGET_DIR)/zlib/libz.a
endif

DEMO_TARGET	= $(TARGET_DIR)/hsdis-demo
DEMO_SOURCE	= hsdis-demo.c

.PHONY:  all clean demo both

all:  $(TARGET)

both: all all64

%64:
	$(MAKE) LP64=1 ${@:%64=%}

demo: $(TARGET) $(DEMO_TARGET)

$(LIBRARIES): $(TARGET_DIR) $(TARGET_DIR)/Makefile
	if [ ! -f $@ ]; then cd $(TARGET_DIR); make all-opcodes; fi

$(TARGET_DIR)/Makefile:
	(cd $(TARGET_DIR); CC=$(CC) CFLAGS="$(CFLAGS)" AR="$(AR)" $(BINUTILSDIR)/configure --disable-nls $(CONFIGURE_ARGS))

$(TARGET): $(SOURCE) $(LIBS) $(LIBRARIES) $(TARGET_DIR)
	$(CC) $(OUTFLAGS) $(CPPFLAGS) $(CFLAGS) $(SOURCE) $(DLDFLAGS) $(LIBRARIES)

$(DEMO_TARGET): $(DEMO_SOURCE) $(TARGET) $(TARGET_DIR)
	$(CC) $(OUTFLAGS) -DTARGET_DIR=\"$(TARGET_DIR)\" $(CPPFLAGS) -g $(CFLAGS/$(ARCH)) $(DEMO_SOURCE) $(LDFLAGS)

$(TARGET_DIR):
	[ -d $@ ] || mkdir -p $@

clean:
	rm -rf $(TARGET_DIR)
