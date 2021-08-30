set(
  TEST_SOURCES
  ${CMAKE_CURRENT_SOURCE_DIR}/TestLibCoreArgsParser.cpp
  ${CMAKE_CURRENT_SOURCE_DIR}/TestLibCoreFileWatcher.cpp
  ${CMAKE_CURRENT_SOURCE_DIR}/TestLibCoreIODevice.cpp
  ${CMAKE_CURRENT_SOURCE_DIR}/TestLibCoreDeferredInvoke.cpp
)

foreach(source ${TEST_SOURCES})
  serenity_test(${source} LibCore)
endforeach()

install(FILES long_lines.txt DESTINATION usr/Tests/LibCore)
