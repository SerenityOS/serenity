serenity_testjs_test(test-wasm.cpp test-wasm LIBS LibWasm LibJS)
install(TARGETS test-wasm RUNTIME DESTINATION bin OPTIONAL)
