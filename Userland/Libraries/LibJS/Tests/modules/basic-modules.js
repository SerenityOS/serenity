// Because you can't easily load modules directly we load them via here and check
// if they passed by checking the result

function validTestModule(filename) {
    if (!filename.endsWith(".mjs") || !filename.startsWith("./")) {
        throw new ExpectationError(
            `Expected module name to start with './' and end with '.mjs' but got '${filename}'`
        );
    }
}

function expectModulePassed(filename, options = undefined) {
    validTestModule(filename);

    let moduleLoaded = false;
    let moduleResult = null;
    let thrownError = null;

    import(filename, options)
        .then(result => {
            moduleLoaded = true;
            moduleResult = result;
            expect(moduleResult).toHaveProperty("passed", true);
        })
        .catch(error => {
            thrownError = error;
        });

    runQueuedPromiseJobs();

    if (thrownError) {
        throw thrownError;
    }

    expect(moduleLoaded).toBeTrue();
    return moduleResult;
}

function expectedModuleToThrowSyntaxError(filename, message) {
    validTestModule(filename);

    let moduleLoaded = false;
    let thrownError = null;

    import(filename)
        .then(() => {
            moduleLoaded = true;
        })
        .catch(error => {
            thrownError = error;
        });

    runQueuedPromiseJobs();

    if (thrownError) {
        expect(() => {
            throw thrownError;
        }).toThrowWithMessage(SyntaxError, message);
    } else {
        throw new ExpectationError(
            `Expected module: '${filename}' to fail to load with a syntax error but did not throw.`
        );
    }
}

describe("testing behavior", () => {
    // To ensure the other tests are interpreter correctly we first test the underlying
    // mechanisms so these tests don't use expectModulePassed.

    test("can load a module", () => {
        let passed = false;
        let error = null;

        import("./empty.mjs")
            .then(() => {
                passed = true;
            })
            .catch(err => {
                error = err;
            });

        runQueuedPromiseJobs();
        if (error) throw error;

        expect(passed).toBeTrue();
    });

    test("can load a module twice", () => {
        let passed = false;
        let error = null;

        import("./empty.mjs")
            .then(() => {
                passed = true;
            })
            .catch(err => {
                error = err;
            });

        runQueuedPromiseJobs();
        if (error) throw error;

        expect(passed).toBeTrue();
    });

    test("can retrieve exported value", () => {
        async function getValue(filename) {
            const imported = await import(filename);
            expect(imported).toHaveProperty("passed", true);
        }

        let passed = false;
        let error = null;

        getValue("./single-const-export.mjs")
            .then(obj => {
                passed = true;
            })
            .catch(err => {
                error = err;
            });

        runQueuedPromiseJobs();

        if (error) throw error;

        expect(passed).toBeTrue();
    });

    test("expectModulePassed works", () => {
        expectModulePassed("./single-const-export.mjs");
    });

    test("can call expectModulePassed with options", () => {
        expectModulePassed("./single-const-export.mjs", { key: "value" });
        expectModulePassed("./single-const-export.mjs", { key1: "value1", key2: "value2" });
    });
});

describe("in- and exports", () => {
    test("variable and lexical declarations", () => {
        const result = expectModulePassed("./basic-export-types.mjs");
        expect(result).not.toHaveProperty("default", null);
        expect(result).toHaveProperty("constValue", 1);
        expect(result).toHaveProperty("letValue", 2);
        expect(result).toHaveProperty("varValue", 3);

        expect(result).toHaveProperty("namedConstValue", 1 + 3);
        expect(result).toHaveProperty("namedLetValue", 2 + 3);
        expect(result).toHaveProperty("namedVarValue", 3 + 3);
    });

    test("default exports", () => {
        const result = expectModulePassed("./module-with-default.mjs");
        expect(result).toHaveProperty("defaultValue");
        expect(result.default).toBe(result.defaultValue);
    });

    test("declaration exports which can be used in the module it self", () => {
        expectModulePassed("./declarations-tests.mjs");
    });

    test("string '*' is not a full namespace import", () => {
        expectModulePassed("./string-import-names.mjs");
    });

    test("can combine string and default exports", () => {
        expectModulePassed("./string-import-namespace.mjs");
    });

    test("can re export string names", () => {
        expectModulePassed("./string-import-namespace-indirect.mjs");
    });

    test("re exporting all-but-default does not export a default value", () => {
        expectedModuleToThrowSyntaxError(
            "./indirect-export-without-default.mjs",
            "Invalid or ambiguous export entry 'default'"
        );
    });

    test("can import with (useless) assertions", () => {
        expectModulePassed("./import-with-assertions.mjs");
    });
});

describe("loops", () => {
    test("import and export from own file", () => {
        expectModulePassed("./loop-self.mjs");
    });

    test("import something which imports a cycle", () => {
        expectModulePassed("./loop-entry.mjs");
    });
});
