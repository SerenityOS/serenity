// Because you can't easily load modules directly we load them via here and check
// if they passed by checking the result

function expectModulePassed(filename) {
    if (!filename.endsWith(".mjs") || !filename.startsWith("./")) {
        throw new ExpectationError(
            "Expected module name to start with './' " +
                "and end with '.mjs' but got '" +
                filename +
                "'"
        );
    }

    async function getModule() {
        return import(filename);
    }

    let moduleLoaded = false;
    let moduleResult = null;
    let thrownError = null;

    getModule()
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
});

describe("loops", () => {
    test("import and export from own file", () => {
        expectModulePassed("./loop-self.mjs");
    });

    test("import something which imports a cycle", () => {
        expectModulePassed("./loop-entry.mjs");
    });
});
