// NOTE: This file is only for syntax highlighting, documentation, etc.

interface LibwebTester {
    /**
     * Changes the page to the specified URL. Everything afterwards will refer to the new page.
     * @param url Page to load.
     */
    changePage(url: string): void;
}

interface Window {
    /**
     * Special test object used to ease test development for LibWeb.
     */
    readonly libweb_tester: LibwebTester;
}
