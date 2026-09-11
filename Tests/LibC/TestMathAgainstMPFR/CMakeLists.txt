if (BUILD_LAGOM_TOOLS)
    serenity_component(
        TestMathAgainstMPFR
        TARGETS TestMathAgainstMPFR
    )

    find_library(MPFR_LIBRARY
            NAMES mpfr
            REQUIRED
    )

    set(SOURCES main.cpp reference_functions.cpp)

    serenity_bin(TestMathAgainstMPFR)
    target_include_directories(TestMathAgainstMPFR PRIVATE ${MPFR_INCLUDE_DIR})
    target_link_libraries(TestMathAgainstMPFR PRIVATE LibMain LibCore LibFileSystem ${MPFR_LIBRARY})

    add_library(m-for-math-test SHARED
        ../../../Userland/Libraries/LibC/math.cpp
    )

    add_dependencies(TestMathAgainstMPFR m-for-math-test)
endif ()
