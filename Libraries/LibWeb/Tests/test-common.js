// NOTE: The tester loads in LibJS's test-common to prevent duplication.

let __PageToLoad__;

// This tells the tester which page to load.
// This will only be checked when we look at which page the test wants to use.
// Subsequent calls to loadPage in before/after load will be ignored.
let loadPage;

let __BeforePageLoad__ = () => {};

// This function will be run just before loading the page.
// This is useful for injecting event listeners.
// Defaults to an empty function.
let beforePageLoad;

let __AfterPageLoad__ = () => {};

// This function will be run just after loading the page.
// This is where the main bulk of the tests should be.
// Defaults to an empty function.
let afterPageLoad;

(() => {
    loadPage = (page) => __PageToLoad__ = page;
    beforePageLoad = (callback) => __BeforePageLoad__ = callback;
    afterPageLoad = (callback) => __AfterPageLoad__ = callback;
})();
