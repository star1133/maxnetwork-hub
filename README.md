# MaxNetwork Hub (Paper/Purpur plugin)

Backend half of the MaxNetwork login system. Runs on the login/lobby server behind a BungeeCord/Waterfall proxy running [maxnetwork-proxy](https://github.com/star1133/maxnetwork-proxy).

- Inventory PIN GUI (4 digits); Bedrock players (Geyser/Floodgate) can type the PIN in chat
- While a player is authenticating: movement, chat and all commands except `/pin` are blocked
- Wrong-PIN counter with kick
- On success the Hub tells the proxy (`AuthSuccess` on `maxnetwork:main`); the proxy creates the session and routes the player
- Server selector GUI, admin GUI (`/mnadmin`), setup wizard (`/mnsetup`), stats GUI (`/mnstats`)

## Build

Requires JDK 17+ (the plugin targets Paper API 1.21) and Maven.

```bash
mvn clean package        # runs unit tests, produces target/MaxNetwork-Hub.jar
```

Copy the jar into `plugins/` of the login server and make sure the server's name in the proxy config is listed in the proxy plugin's `messaging.trusted_servers`.

## Known limitations

- 4-digit PINs are stored as unsalted SHA-256 (legacy `hashCode` entries are upgraded on first successful login). Treat the PIN files as sensitive.
- The Hub and the proxy each keep PIN data; keep them in sync via the `SyncPin` message and do not edit one side by hand.

## License

MIT — see [LICENSE](LICENSE).
