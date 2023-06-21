add_compile_options(-O2)

# Escape hatch target to prevent runtime startup libraries from having coverage enabled
# at awkward points in program initialization
add_library(NoCoverage INTERFACE)

if (ENABLE_USERSPACE_COVERAGE_COLLECTION)
    if (CMAKE_CXX_COMPILER_ID MATCHES "Clang$")
        add_compile_options(-fprofile-instr-generate -fcoverage-mapping)
        add_link_options(-fprofile-instr-generate -fcoverage-mapping)

        target_compile_options(NoCoverage INTERFACE -fno-profile-generate -fno-profile-instr-use -fno-profile-instr-generate -fno-coverage-mapping)
        target_link_options(NoCoverage INTERFACE -fno-profile-generate -fno-profile-instr-use -fno-profile-instr-generate -fno-coverage-mapping)
    else()
        message(FATAL_ERROR "ENABLE_USERSPACE_COVERAGE_COLLECTION not supported yet for ${CMAKE_CXX_COMPILER_ID}")
    endif()
endif()

add_subdirectory(Applications)
add_subdirectory(Demos)
add_subdirectory(DevTools)
add_subdirectory(DynamicLoader)
add_subdirectory(Games)
add_subdirectory(Libraries)
add_subdirectory(BuggieBox)
add_subdirectory(Applets)
add_subdirectory(Services)
add_subdirectory(Shell)
add_subdirectory(Utilities)
