add_executable(test-js test-js.cpp)
target_link_libraries(test-js LibJS LibLine LibCore)
install(TARGETS test-js RUNTIME DESTINATION bin)
