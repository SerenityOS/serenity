const fs = require("fs");
const Twit = require("twit");
const { CONSUMER_KEY, CONSUMER_SECRET, ACCESS_TOKEN, ACCESS_TOKEN_SECRET } = process.env;

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
        tweets.push(
            `${commit["message"].substring(0, 240)}\nAuthor: ${commit["author"]["name"]}\n${
                commit["url"]
            }`
        );
    }
    for (const tweet of tweets) {
        try {
            await T.post("statuses/update", { status: tweet });
        } catch (e) {
            console.error("Failed to post a tweet!", e.message);
        }
    }
})();
