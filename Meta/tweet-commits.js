const fs = require("fs");
const Twit = require("twit");
const { CONSUMER_KEY, CONSUMER_SECRET, ACCESS_TOKEN, ACCESS_TOKEN_SECRET } = process.env;
const tweetLength = 280;
// Twitter always considers t.co links to be 23 chars, see https://help.twitter.com/en/using-twitter/how-to-tweet-a-link
const twitterLinkLength = 23;

const T = new Twit({
    consumer_key: CONSUMER_KEY,
    consumer_secret: CONSUMER_SECRET,
    access_token: ACCESS_TOKEN,
    access_token_secret: ACCESS_TOKEN_SECRET,
});

(async () => {
    const githubEvent = JSON.parse(fs.readFileSync(0).toString());
    const tweets = [];
    for (const commit of githubEvent["commits"]) {
        const authorLine = `Author: ${commit["author"]["name"]}`;
        const maxMessageLength = tweetLength - authorLine.length - twitterLinkLength - 2; // -2 for newlines
        const commitMessage =
            commit["message"].length > maxMessageLength
                ? commit["message"].substring(0, maxMessageLength - 2) + "…" // Ellipsis counts as 2 characters
                : commit["message"];

        tweets.push(`${commitMessage}\n${authorLine}\n${commit["url"]}`);
    }
    for (const tweet of tweets) {
        try {
            await T.post("statuses/update", { status: tweet });
        } catch (e) {
            console.error("Failed to post a tweet!", e.message);
        }
    }
})();
