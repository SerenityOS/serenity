# Note: Update this alongside Toolchain/BuildCMake.sh
set(version_ok 0)
if (CMAKE_VERSION VERSION_GREATER_EQUAL 3.25.1)
    set(version_ok 1)
endif()
execute_process(COMMAND "${CMAKE_COMMAND}" -E echo "${version_ok}")
