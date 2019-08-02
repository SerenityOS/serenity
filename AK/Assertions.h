#pragma once

#ifndef AK_TEST_SUITE

#ifdef KERNEL
#    include <Kernel/Assertions.h>
#else
#    include <assert.h>
#    ifndef __serenity__
#        define ASSERT assert
#        define ASSERT_NOT_REACHED() assert(false)
#        define RELEASE_ASSERT assert
#    endif
#endif

#endif

namespace AK {

inline void not_implemented() { ASSERT(false); }

}

using AK::not_implemented;
