const fs = require("fs");
const Mastodon = require("mastodon");
const { ACCESS_TOKEN } = process.env;
const tootLength = 500;
// Mastodon always considers links to be 23 chars, see https://docs.joinmastodon.org/user/posting/#links
const mastodonLinkLength = 23;

const mastodon = new Mastodon({
    access_token: ACCESS_TOKEN,
    timeout_ms: 60 * 1000,
    api_url: "https://serenityos.social/api/v1/",
});

(async () => {
    const githubEvent = JSON.parse(fs.readFileSync(0).toString());
    const toots = [];
    for (const commit of githubEvent["commits"]) {
        const authorLine = `Author: ${commit["author"]["name"]}`;
        const maxMessageLength = tootLength - authorLine.length - mastodonLinkLength - 2; // -2 for newlines
        const commitMessage =
            commit["message"].length > maxMessageLength
                ? commit["message"].substring(0, maxMessageLength - 2) + "…" // Ellipsis counts as 2 characters
                : commit["message"];

        toots.push(`${commitMessage}\n${authorLine}\n${commit["url"]}`);
    }
    for (const toot of toots) {
        try {
            await mastodon.post("statuses", { status: toot, visibility: "unlisted" });
        } catch (e) {
            console.error("Failed to post a toot!", e.message);
        }
    }
})();
