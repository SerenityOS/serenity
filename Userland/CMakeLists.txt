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

serenity_add_subdirectory(Applications)
serenity_add_subdirectory(Demos)
serenity_add_subdirectory(DevTools)
serenity_add_subdirectory(DynamicLoader)
serenity_add_subdirectory(Games)
serenity_add_subdirectory(Libraries)
serenity_add_subdirectory(BuggieBox)
serenity_add_subdirectory(Applets)
serenity_add_subdirectory(Services)
serenity_add_subdirectory(Shell)
serenity_add_subdirectory(Utilities)
