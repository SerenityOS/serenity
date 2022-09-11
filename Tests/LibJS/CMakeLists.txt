serenity_testjs_test(test-js.cpp test-js)

install(TARGETS test-js RUNTIME DESTINATION bin OPTIONAL)
link_with_locale_data(test-js)

serenity_test(test-invalid-unicode-js.cpp LibJS LIBS LibJS)
link_with_locale_data(test-invalid-unicode-js)

serenity_test(test-bytecode-js.cpp LibJS LIBS LibJS)
link_with_locale_data(test-bytecode-js)

serenity_test(test-value-js.cpp LibJS LIBS LibJS)
link_with_locale_data(test-value-js)

serenity_component(
    test262-runner
    TARGETS test262-runner
)
add_executable(test262-runner test262-runner.cpp)
target_link_libraries(test262-runner LibJS LibCore)
link_with_locale_data(test262-runner)
install(TARGETS test262-runner RUNTIME DESTINATION bin OPTIONAL)

serenity_component(
        test-test262
        TARGETS test-test262
)
add_executable(test-test262 test-test262.cpp)
target_link_libraries(test-test262 LibMain LibCore)
install(TARGETS test-test262 RUNTIME DESTINATION bin OPTIONAL)
