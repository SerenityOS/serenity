SRCS = $(shell find ./ -name '*.cpp' -printf '%f\n')
OBJS = ${SRCS:.cpp=.o}
APPS = ${SRCS:.cpp=}

ARCH_FLAGS =
STANDARD_FLAGS = -std=c++17
WARNING_FLAGS = -Wextra -Wall -Wundef -Wcast-qual -Wwrite-strings -Wimplicit-fallthrough
FLAVOR_FLAGS = -fno-exceptions -fno-rtti -fno-sized-deallocation
OPTIMIZATION_FLAGS = -Os
INCLUDE_FLAGS = -I.. -I. -I../LibC

DEFINES = -DSERENITY -DSANITIZE_PTRS -DUSERLAND

CXXFLAGS = -MMD -MP $(WARNING_FLAGS) $(OPTIMIZATION_FLAGS) $(FLAVOR_FLAGS) $(ARCH_FLAGS) $(STANDARD_FLAGS) $(INCLUDE_FLAGS) $(DEFINES)
CXX = i686-pc-serenity-g++
LD = i686-pc-serenity-g++
AR = i686-pc-serenity-r
LDFLAGS = -L../LibC -L../LibCore -L../LibGUI

all: $(OBJS) $(APPS)

list:
	@echo $(APPS)

clean:
	@echo "CLEAN"; rm -f $(APPS) $(OBJS) *.d

$(APPS) : % : %.o $(OBJS)
	@echo "LD $<"; 
	@$(LD) -o $@ $(LDFLAGS) $< -lc

%.o: %.cpp
	@echo "CXX $<"; 
	@$(CXX) $(CXXFLAGS) -o $@ -c $<

-include $(OBJS:%.o=%.d)
