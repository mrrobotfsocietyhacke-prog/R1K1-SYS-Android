# R1K1.SYS Trading API contract (Android client)

All requests are sent through `ClientManager.java` to the configured custom HTTP/JSON server. No Telegram, MTProto, TON, or client-side balance mutations are used.

## Screens and endpoints

| Screen | Request | Expected response |
|---|---|---|
| Market | `GET /market/list` | JSON array or object containing `items`, `listings`, `data`, or `results` |
| Buy | `POST /market/buy` | `{ "listing_id": "..." }` |
| Standard gifts | `GET /gifts?type=standard` | gift array |
| Archived gifts | `GET /gifts?type=archive` | archived gift array |
| Send gift | `POST /gifts/send` | `{ "user_id": "...", "gift_id": "..." }` |
| My sales | `GET /market/my-listings` | listing array |
| Create listing | `POST /market/sell` | NFT: `{ "nft_id": "...", "price": 100, "currency": "Bytes", "item_type": "nft", "listing_type": "fixed" }`; gift listing uses `gift_id` and `item_type: "gift"` |
| Craft recipes | `GET /craft/recipes` | recipe array |
| Craft | `POST /craft` | `{ "recipe_id": "..." }` |
| Giveaway join | `POST /giveaways/join` | `{ "giveaway_id": "..." }` |

`currency` should be `Bytes` or `Coins`. Server must validate ownership, balances, prices, listing status, duplicate requests, and all transaction outcomes. The client only displays server responses.

## Listing / gift fields

Recommended fields: `id` or `listing_id`, `nft_id` / `gift_id`, `name` or `title`, `rarity`, `owner_username`, `price`, `currency`, `image_url` (or `image`). Auction listings may additionally return `listing_type: "auction"`, `current_bid`, `ends_at`, and preferably a server-calculated `time_remaining` string.

## Chat interactive card wire format

A bot may send a message whose text starts with `R1K1CARD:` followed by a JSON object, for example:

```json
R1K1CARD:{"type":"giveaway","giveaway_id":"gw_123","title":"R1K1 DROP","description":"Участвуй в розыгрыше","button_label":"Участвовать"}
```

The Android client renders this as a themed interactive card and submits `POST /giveaways/join`. This is a constrained native card schema, not arbitrary remote HTML/JavaScript; arbitrary HTML execution is intentionally not enabled in chat.

## Not yet wired

Auction bidding is represented in listing cards (current bid and remaining time) but bid submission is not enabled until the server defines an authenticated bid endpoint and payload. A dedicated server endpoint for withdrawing/canceling a listing is also not assumed by this client.
