find_library(MPFR_LIBRARY
    NAMES mpfr
    REQUIRED
)

find_path(MPFR_INCLUDE_DIR
    NAMES mpfr.h
    REQUIRED
)

set(SOURCES
    main.cpp
    reference_functions.cpp
)

lagom_tool(CompareAKMathAgainstMPFR
    LIBS LibMain LibCoreMinimal LibFileSystem ${MPFR_LIBRARY}
)
target_include_directories(CompareAKMathAgainstMPFR PRIVATE ${MPFR_INCLUDE_DIR})
