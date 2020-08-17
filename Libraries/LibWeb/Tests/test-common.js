// NOTE: The tester loads in LibJS's test-common to prevent duplication.

// NOTE: "window.libweb_tester" is set to a special tester object.
//       See libweb_tester.d.ts for definitions.

let __PageToLoad__;

// This tells the tester which page to load.
// This will only be checked when we look at which page the test wants to use.
// Subsequent calls to loadPage in before/after initial load will be ignored.
let loadPage;

let __BeforeInitialPageLoad__ = () => {};

// This function will be called just before loading the initial page.
// This is useful for injecting event listeners.
// Defaults to an empty function.
let beforeInitialPageLoad;

let __AfterInitialPageLoad__ = () => {};

// This function will be called just after loading the initial page.
// This is where the main bulk of the tests should be.
// Defaults to an empty function.
let afterInitialPageLoad;

(() => {
    loadPage = (page) => __PageToLoad__ = page;
    beforeInitialPageLoad = (callback) => __BeforeInitialPageLoad__ = callback;
    afterInitialPageLoad = (callback) => __AfterInitialPageLoad__ = callback;
})();
