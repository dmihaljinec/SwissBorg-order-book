SwissBorg Order Books
=====================
This is an Android demonstration project that connects to Bitfinex websocket API (wss://api-pub.bitfinex.com/ws/1), subscribes to BTCUSD ticker and order book and shows this information to the user.

Data Flow
---------
This project is using Kotlin coroutines, BroadcastChannel and Flow to implement reactive stream from repository down to view model. View model stores transformed models from flow into live data which fragment is observing to modify view.

![alt text](https://github.com/dmihaljinec/SwissBorg-order-book/blob/master/SwissBorg-order-book-data-flow.png?raw=true)

[WebSocketClient][1] is responsible for making a websocket connection with Bitfinex server and to automatically reconnect if conenction is lost. Once connection is established WebSocketClient user can send events to Bitfinex server and receive messages from it. Incoming messages represent a hot stream and are pushed to broadcast channel without any modification. User can get those messages as kotlin Flow.

[ChannelConnection][2] is user of WebSocketClient that allows multiple channels to share web socket connection. Channels connected to ChannelConnection will receive messages that belong to that channel. Messages are filtered by channel name, currency pair and channel id. ChannelConnection does not modify messages it only filters them.

[Channel][3] is responsible for subscribing with Bitfinex server channels and to automatically resubscribe if required. Specialized channels like [TickerChannel][4] and [OrderBooksChannel][5] are responsible for deserializing Bitfinex messages which are json strings into respective data classes (see [Api.Ticker and Api.OrderBook][6]).

[OrderBookDataSource][7] is responsible for transforming Bitfinex Api data classes into model. For ticker this transformation is trivial, while for order books it's a bit more complex. Model requires that list of order books contains pairs of buy and sell orders. Order with highest bidding (buy price) is paired with order with lowest asking (sell price). This process is repeated for remaining buy and sell orders which creates other pairs and form a list of order books.

[OrderBookRepository][8] is using single data source so it's simply pass through.

[OrderBookInteractors][9] provides uses cases getting ticker, order books and connection state.

[OrderBookListFragmentViewModel][10] collects all flows obtained from OrderBookInteractors and transforms model data into view models that are bound to views.

[OrderBookListFragment][11] binds OrderBookListFragmentViewModel into views. For list of order books it hosts adapter which observes OrderBookListFragmentViewModel list of OrderBookViewModel's.

[MainActivity][12] hosts OrderBookListFragment.

Author
------
Damir Mihaljinec - @dmihaljinec on GitHub

License
-------
Apache License, Version 2.0. See the [LICENSE][13] file for details.

[1]: https://github.com/dmihaljinec/SwissBorg-order-book/blob/master/app/src/main/java/com/swissborg/orderbook/android/ws/WebSocketClient.kt
[2]: https://github.com/dmihaljinec/SwissBorg-order-book/blob/master/app/src/main/java/com/swissborg/orderbook/android/bitfinex/ChannelConnection.kt
[3]: https://github.com/dmihaljinec/SwissBorg-order-booki/blob/master/app/src/main/java/com/swissborg/orderbook/android/bitfinex/Channel.kt
[4]: https://github.com/dmihaljinec/SwissBorg-order-book/blob/master/app/src/main/java/com/swissborg/orderbook/android/bitfinex/TickerChannel.kt
[5]: https://github.com/dmihaljinec/SwissBorg-order-book/blob/master/app/src/main/java/com/swissborg/orderbook/android/bitfinex/OrderBooksChannel.kt
[6]: https://github.com/dmihaljinec/SwissBorg-order-book/blob/master/app/src/main/java/com/swissborg/orderbook/android/bitfinex/Api.kt
[7]: https://github.com/dmihaljinec/SwissBorg-order-book/blob/master/app/src/main/java/com/swissborg/orderbook/android/bitfinex/OrderBookDataSourceImpl.kt
[8]: https://github.com/dmihaljinec/SwissBorg-order-book/blob/master/app/src/main/java/com/swissborg/orderbook/repository/OrderBookRepository.kt
[9]: https://github.com/dmihaljinec/SwissBorg-order-book/blob/master/app/src/main/java/com/swissborg/orderbook/interactor/OrderBookInteractors.kt
[10]: https://github.com/dmihaljinec/SwissBorg-order-book/blob/master/app/src/main/java/com/swissborg/orderbook/android/ui/OrderBookListFragmentViewModel.kt
[11]: https://github.com/dmihaljinec/SwissBorg-order-book/blob/master/app/src/main/java/com/swissborg/orderbook/android/ui/OrderBookListFragment.kt
[12]: https://github.com/dmihaljinec/SwissBorg-order-book/blob/master/app/src/main/java/com/swissborg/orderbook/android/ui/MainActivity.kt
[13]: https://github.com/dmihaljinec/SwissBorg-order-book/blob/master/LICENSE
