function(add_simple_fuzzer name)
  add_executable(${name} "${name}.cpp")

  if (ENABLE_FUZZERS_OSSFUZZ)
      target_link_libraries(${name}
          PUBLIC ${ARGN} LibCore)
  elseif (ENABLE_FUZZERS_LIBFUZZER)
    target_compile_options(${name}
      PRIVATE $<$<CXX_COMPILER_ID:Clang>:-g -O1 -fsanitize=fuzzer>
      )
    target_link_libraries(${name}
      PUBLIC ${ARGN} LibCore
      PRIVATE $<$<CXX_COMPILER_ID:Clang>:-fsanitize=fuzzer>
      )
  else()
    target_sources(${name} PRIVATE "EntryShim.cpp")
    target_link_libraries(${name} PUBLIC ${ARGN} LibCore)
  endif()
endfunction()

include(fuzzers.cmake)

foreach(target IN LISTS FUZZER_TARGETS)
  add_simple_fuzzer("Fuzz${target}" "${FUZZER_DEPENDENCIES_${target}}")
endforeach()

if (ENABLE_FUZZERS_LIBFUZZER)
set(CMAKE_EXE_LINKER_FLAGS "${ORIGINAL_CMAKE_EXE_LINKER_FLAGS} -fsanitize=address")
set(CMAKE_SHARED_LINKER_FLAGS "${ORIGINAL_CMAKE_SHARED_LINKER_FLAGS} -fsanitize=address")
set(CMAKE_MODULE_LINKER_FLAGS "${ORIGINAL_CMAKE_MODULE_LINKER_FLAGS} -fsanitize=address")
add_executable(FuzzilliJs FuzzilliJs.cpp)
target_compile_options(FuzzilliJs
    PRIVATE $<$<CXX_COMPILER_ID:Clang>:-g -O1 -fsanitize-coverage=trace-pc-guard>
    )
target_link_libraries(FuzzilliJs
    PUBLIC LibCore LibJS
    PRIVATE $<$<CXX_COMPILER_ID:Clang>:-fsanitize-coverage=trace-pc-guard>
    )
endif()
