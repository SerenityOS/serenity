set(SOURCES
    UBSanitizer.cpp
    ../LibC/ssp.cpp
    ../LibC/ssp_nonshared.cpp
)

set_source_files_properties(../LibC/ssp.cpp PROPERTIES COMPILE_FLAGS "-fno-stack-protector")
set_source_files_properties(../LibC/ssp_nonshared.cpp PROPERTIES COMPILE_FLAGS "-fno-stack-protector")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -nostdlib")
serenity_libc(LibUBSanitizer ubsan)

add_library(LibUBSanitizerStatic STATIC ${SOURCES})
target_link_libraries(LibUBSanitizerStatic PRIVATE NoCoverage)
