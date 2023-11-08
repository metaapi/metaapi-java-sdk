# metaapi.cloud SDK for Java

**Note: The Java SDK was updated long time ago and lacks important bugfixes and updates, and in order to access our service using Java SDK you need to request an explicit permission to do so via online chat. We plan to update the java SDK sometime in future. Meanwhile we recommend you to use javascript or python SDK instead.**

MetaApi is a powerful, fast, cost-efficient, easy to use and standards-driven cloud forex trading API for MetaTrader 4 and MetaTrader 5 platform designed for traders, investors and forex application developers to boost forex application development process. MetaApi can be used with any broker and does not require you to be a brokerage.

CopyFactory is a simple yet powerful copy-trading API which is a part of MetaApi. See below for CopyFactory readme section.

MetaApi is a paid service, but API access to one MetaTrader account is free of charge.

The [MetaApi pricing](https://metaapi.cloud/#pricing) was developed with the intent to make your charges less or equal to what you would have to pay for hosting your own infrastructure. This is possible because over time we managed to heavily optimize our MetaTrader infrastructure. And with MetaApi you can save significantly on application development and maintenance costs and time thanks to high-quality API, open-source SDKs and convenience of a cloud service.

Official REST and websocket API documentation: [https://metaapi.cloud/docs/client/](https://metaapi.cloud/docs/client/)

Please note that this SDK provides an abstraction over REST and websocket API to simplify your application logic.

For more information about SDK APIs please check esdoc documentation in source codes located inside lib folder of this npm package.

## Working code examples
Please check [this short video](https://youtu.be/dDOUWBjdfA4) to see how you can download samples via our web application.

You can also find code examples at [examples folder of our github repo](https://github.com/agiliumtrade-ai/metaapi-java-client/tree/master/examples) or in the examples folder of the project.

We have composed a [short guide explaining how to use the example code](https://metaapi.cloud/docs/client/usingCodeExamples/)

## Installation
If you use Apache Maven, add this to `<dependencies>` in your `pom.xml`:
```xml
<dependency>
  <groupId>cloud.metaapi.sdk</groupId>
  <artifactId>metaapi-java-sdk</artifactId>
  <version>14.0.8</version>
</dependency>
```

Other options can be found on [this page](https://search.maven.org/artifact/cloud.metaapi.sdk/metaapi-java-sdk/14.0.4/jar).

## Running Java SDK examples
In order to run Java SDK examples, follow these steps:
1. Make sure that you have installed [Maven](http://maven.apache.org) and its command `mvn` is accessible.
2. Navigate to the root folder of the example project (where its `pom.xml` is located).
3. Build the project with `mvn package`.
4. Run `mvn exec:java@`_`ExampleClassName`_ where _`ExampleClassName`_ is the example to execute, e.g. `mvn exec:java@MetaApiRpcExample`.

Example parameters such as token or account id can be passed via environment variables, or set directly in the example source code. In the last case you need to rebuild the example with `mvn package`. 

## Connecting to MetaApi
Please use one of these ways: 
1. [https://app.metaapi.cloud/token](https://app.metaapi.cloud/token) web UI to obtain your API token.
2. An account access token which grants access to a single account. See section below on instructions on how to retrieve account access token.

Supply token to the MetaApi class constructor.

```java
import cloud.metaapi.sdk.metaApi.MetaApi;

String token = "...";
MetaApi api = new MetaApi(token);
```

## Retrieving account access token
Account access token grants access to a single account. You can retrieve account access token via API:
```java
String accountId = "...";
MetatraderAccount account = api.getMetatraderAccountApi().getAccount(accountId).join();
String accountAccessToken = account.getAccessToken();
System.out.println(accountAccessToken);
```

Alternatively, you can retrieve account access token via web UI on https://app.metaapi.cloud/accounts page (see [this video](https://youtu.be/PKYiDns6_xI)).

## Managing MetaTrader accounts (API servers for MT accounts)
Before you can use the API you have to add an MT account to MetaApi and start an API server for it.

However, before you can create an account, you have to create a provisioning profile.

### Managing provisioning profiles via web UI
You can manage provisioning profiles here: [https://app.metaapi.cloud/provisioning-profiles](https://app.metaapi.cloud/provisioning-profiles)

### Creating a provisioning profile via API
```java
// if you do not have created a provisioning profile for your broker,
// you should do it before creating an account
ProvisioningProfile provisioningProfile = api.getProvisioningProfileApi()
    .createProvisioningProfile(new NewProvisioningProfileDto() {{
        name = "My profile";
        version = 5;
        brokerTimezone = "EET";
        brokerDSTSwitchTimezone = "EET";
    }}).join();
// servers.dat file is required for MT5 profile and can be found inside
// config directory of your MetaTrader terminal data folder. It contains
// information about available broker servers
provisioningProfile.uploadFile("servers.dat", "/path/to/servers.dat").join();
// for MT4, you should upload an .srv file instead
provisioningProfile.uploadFile("broker.srv", "/path/to/broker.srv").join();
```

### Retrieving existing provisioning profiles via API
```java
List<ProvisioningProfile> provisioningProfiles = api.getProvisioningProfileApi()
    .getProvisioningProfiles(null, null).join();
ProvisioningProfile provisioningProfile = api.getProvisioningProfileApi().getProvisioningProfile("profileId").join();
```

### Updating a provisioning profile via API
```java
provisioningProfile.update(new ProvisioningProfileUpdateDto() {{ name = "New name" }}).join();
// for MT5, you should upload a servers.dat file
provisioningProfile.uploadFile("servers.dat", "/path/to/servers.dat").join();
// for MT4, you should upload an .srv file instead
provisioningProfile.uploadFile("broker.srv", "/path/to/broker.srv").join();
```

### Removing a provisioning profile
```java
provisioningProfile.remove().join();
```

### Managing MetaTrader accounts (API servers) via web UI
You can manage MetaTrader accounts here: [https://app.metaapi.cloud/accounts](https://app.metaapi.cloud/accounts)

### Create a MetaTrader account (API server) via API
```java
MetatraderAccount account = api.getMetatraderAccountApi().createAccount(new NewMetatraderAccountDto() {
  name = "Trading account #1";
  type = "cloud";
  login = "1234567";
  // password can be investor password for read-only access
  password = "qwerty";
  server = "ICMarketsSC-Demo";
  provisioningProfileId = provisioningProfile.getId();
  application = "MetaApi";
  magic = 123456;
  quoteStreamingIntervalInSeconds = 2.5; // set to 0 to receive quote per tick
  reliability = "regular"; // set this field to 'high' value if you want to increase uptime of your account (recommended for production environments)
});
```

### Retrieving existing accounts via API
```java
// filter and paginate accounts, see docs for full list of filter options available
List<MetatraderAccount> accounts = api.getMetatraderAccountApi().getAccounts(new AccountsFilter() {{
  limit = 10;
  offset = 0;
  query = "ICMarketsSC-MT5";
  state = List.of(DeploymentState.DEPLOYED);
}}).join();
// get accounts without filter (returns 1000 accounts max)
List<MetatraderAccount> accounts = api.getMetatraderAccountApi().getAccounts(null).join();

MetatraderAccount account = api.getMetatraderAccountApi().getAccount("accountId").join();
```

### Updating an existing account via API
```java
account.update(new MetatraderAccountUpdateDto() {
  name = "Trading account #1";
  login = "1234567";
  // password can be investor password for read-only access
  password = "qwerty";
  server = "ICMarketsSC-Demo";
  quoteStreamingIntervalInSeconds = 2.5; // set to 0 to receive quote per tick
}).join();
```

### Removing an account
```java
account.remove().join();
```

### Deploying, undeploying and redeploying an account (API server) via API
```java
account.deploy().join();
account.undeploy().join();
account.redeploy().join();
```

### Manage custom experts (EAs)
Custom expert advisors can only be used for MT4 accounts on g1 infrastructure. EAs which use DLLs are not supported.

### Creating an expert advisor via API
You can use the code below to create an EA. Please note that preset field is a base64-encoded preset file.
```java
ExpertAdvisor expert = account.createExpertAdvisor("expertId", new NewExpertAdvisorDto() {{
  period = "1h";
  symbol = "EURUSD";
  preset = "a2V5MT12YWx1ZTEKa2V5Mj12YWx1ZTIKa2V5Mz12YWx1ZTMKc3VwZXI9dHJ1ZQ";
}}).join();
expert.uploadFile("/path/to/custom-ea").join();
```

### Retrieving existing experts via API
```java
List<ExpertAdvisor> experts = account.getExpertAdvisors().join();
```

### Retrieving existing expert by id via API
```java
ExpertAdvisor expert = account.getExpertAdvisor("expertId").join();
```

### Updating existing expert via API
You can use the code below to update an EA. Please note that preset field is a base64-encoded preset file.
```java
expert.update(new NewExpertAdvisorDto() {{
  period = "4h";
  symbol = "EURUSD";
  preset = "a2V5MT12YWx1ZTEKa2V5Mj12YWx1ZTIKa2V5Mz12YWx1ZTMKc3VwZXI9dHJ1ZQ";
}}).join();
expert.uploadFile("/path/to/custom-ea").join();
```

### Removing expert via API
```java
expert.remove().join();
```

## Access MetaTrader account via RPC API
RPC API let you query the trading terminal state. You should use
RPC API if you develop trading monitoring apps like myfxbook or other
simple trading apps.

### Query account information, positions, orders and history via RPC API
```java
MetaApiConnection connection = account.connect().join();

connection.waitSynchronized().join();

// retrieve balance and equity
System.out.println(connection.getAccountInformation().join());
// retrieve open positions
System.out.println(connection.getPositions().join());
// retrieve a position by id
System.out.println(connection.getPosition("1234567").join());
// retrieve pending orders
System.out.println(connection.getOrders().join());
// retrieve a pending order by id
System.out.println(connection.getOrder("1234567").join());
// retrieve history orders by ticket
System.out.println(connection.getHistoryOrdersByTicket("1234567").join());
// retrieve history orders by position id
System.out.println(connection.getHistoryOrdersByPosition("1234567").join());
// retrieve history orders by time range
System.out.println(connection.getHistoryOrdersByTimeRange(startTime, endTime).join());
// retrieve history deals by ticket
System.out.println(connection.getDealsByTicket("1234567").join());
// retrieve history deals by position id
System.out.println(connection.getDealsByPosition("1234567").join());
// retrieve history deals by time range
System.out.println(connection.getDealsByTimeRange(startTime, endTime).join());
```

### Query contract specifications and quotes via RPC API
```java
MetaApiConnection connection = account.connect().join();

connection.waitSynchronized().join();

// first, subscribe to market data
connection.subscribeToMarketData("GBPUSD").join();

// read symbols available
System.out.println(connection.getSymbols().join());
// read constract specification
System.out.println(connection.getSymbolSpecification("GBPUSD").join());
// read current price
System.out.println(connection.getSymbolPrice("GBPUSD").join());
```

### Query historical market data via RPC API
Currently this API is supported on G1 and MT4 G2 only.

```java
// retrieve 1000 candles before the specified time
List<MetatraderCandle> candles = account.getHistoricalCandles("EURUSD", "1m", new IsoTime("2021-05-01T00:00:00.000Z"), 1000).join();
// retrieve 1000 ticks after the specified time
List<MetatraderTick> ticks = account.getHistoricalTicks("EURUSD", new IsoTime("2021-05-01T00:00:00.000Z"), 5, 1000).join();
// retrieve 1000 latest ticks
List<MetatraderTick> ticks = account.getHistoricalTicks("EURUSD", null, 0, 1000).join();
```

### Use real-time streaming API
Real-time streaming API is good for developing trading applications like trade copiers or automated trading strategies.
The API synchronizes the terminal state locally so that you can query local copy of the terminal state really fast.

#### Synchronizing and reading teminal state
```java
MetatraderAccount account = api.getMetatraderAccountApi().getAccount("accountId").join();

// access local copy of terminal state
TerminalState terminalState = connection.getTerminalState();

// wait until synchronization completed
connection.waitSynchronized().join();

System.out.println(terminalState.isConnected());
System.out.println(terminalState.isConnectedToBroker());
System.out.println(terminalState.getAccountInformation());
System.out.println(terminalState.getPositions());
System.out.println(terminalState.getOrders());
// symbol specifications
System.out.println(terminalState.getSpecifications());
System.out.println(terminalState.getSpecification("EURUSD"));
System.out.println(terminalState.getPrice("EURUSD"));

// access history storage
HistoryStorage historyStorage = connection.getHistoryStorage();

// both orderSynchronizationFinished and dealSynchronizationFinished
// should be true once history synchronization have finished
System.out.println(historyStorage.isOrderSynchronizationFinished());
System.out.println(historyStorage.isDealSynchronizationFinished());
```

#### Overriding local history storage
By default history is stored in memory only. You can override history storage to save trade history to a persistent storage like MongoDB database.
```java
import cloud.metaapi.sdk.metaApi.HistoryStorage;

class MongodbHistoryStorage extends HistoryStorage {
  // implement the abstract methods, see MemoryHistoryStorage for sample
  // implementation
}

HistoryStorage historyStorage = new MongodbHistoryStorage();

// Note: if you will not specify history storage, then in-memory storage
// will be used (instance of MemoryHistoryStorage)
MetaApiConnection connection = account.connect(historyStorage).join();

// access history storage
HistoryStorage historyStorage = connection.getHistoryStorage();

// invoke other methods provided by your history storage implementation
System.out.println(((MongodbHistoryStorage) historyStorage).yourMethod().join());
```

#### Receiving synchronization events
You can override SynchronizationListener in order to receive synchronization event notifications, such as account/position/order/history updates or symbol quote updates.
```java
import cloud.metaapi.sdk.clients.metaApi.SynchronizationListener;

// receive synchronization event notifications
// first, implement your listener
class MySynchronizationListener extends SynchronizationListener {
  // override abstract methods you want to receive notifications for
}

// now add the listener
SynchronizationListener listener = new MySynchronizationListener();
connection.addSynchronizationListener(listener);

// remove the listener when no longer needed
connection.removeSynchronizationListener(listener);
```

### Retrieve contract specifications and quotes via streaming API
```java
MetaApiConnection connection = account.connect().join();
connection.waitSynchronized().join();
// first, subscribe to market data
connection.subscribeToMarketData("GBPUSD").join();
// read constract specification
System.out.println(terminalState.getSpecification("EURUSD"));
// read current price
System.out.println(terminalState.getPrice("EURUSD"));

// unsubscribe from market data when no longer needed
connection.unsubscribeFromMarketData("GBPUSD").join();
```

### Execute trades (both RPC and streaming APIs)
```java
MetaApiConnection connection = account.connect().join();

connection.waitSynchronized().join();

// trade
TradeOptions options = new TradeOptions() {{ comment = "comment"; clientId = "TE_GBPUSD_7hyINWqAl"; }};
System.out.println(connection.createMarketBuyOrder("GBPUSD", 0.07, 0.9, 2.0, options).join());
System.out.println(connection.createMarketSellOrder("GBPUSD", 0.07, 2.0, 0.9, options).join());
System.out.println(connection.createLimitBuyOrder("GBPUSD", 0.07, 1.0, 0.9, 2.0, options).join());
System.out.println(connection.createLimitSellOrder("GBPUSD", 0.07, 1.5, 2.0, 0.9, options).join());
System.out.println(connection.createStopBuyOrder("GBPUSD", 0.07, 1.5, 0.9, 2.0, options).join());
System.out.println(connection.createStopSellOrder("GBPUSD", 0.07, 1.0, 2.0, 0.9, options).join());
System.out.println(connection.createStopLimitBuyOrder("GBPUSD", 0.07, 1.5, 1.4, 0.9, 2.0, options).join());
System.out.println(connection.createStopLimitSellOrder("GBPUSD", 0.07, 1.0, 1.1, 2.0, 0.9, options).join());
System.out.println(connection.modifyPosition("46870472", 2.0, 0.9).join());
System.out.println(connection.closePositionPartially("46870472", 0.9, null).join());
System.out.println(connection.closePosition("46870472", null).join());
System.out.println(connection.closeBy("46870472", "46870482", null).join());
System.out.println(connection.closePositionsBySymbol("EURUSD", null).join());
System.out.println(connection.modifyOrder("46870472", 1.0, 2.0, 0.9).join());
System.out.println(connection.cancelOrder("46870472").join());

// if you need to, check the extra result information in stringCode and numericCode properties of the response
MetatraderTradeResponse result = connection.createMarketBuyOrder("GBPUSD", 0.07, 0.9, 2.0, options).join();
System.out.println("Trade successful, result code is " + result.stringCode);
```

## Monitoring account connection health and uptime
You can monitor account connection health using MetaApiConnection.healthMonitor API.
```java
ConnectionHealthMonitor monitor = connection.getHealthMonitor();
// retrieve server-side app health status
System.out.println(monitor.getServerHealthStatus());
// retrieve detailed connection health status
System.out.println(monitor.getHealthStatus());
// retrieve account connection update measured over last 7 days
System.out.println(monitor.getUptime());
```

## Tracking latencies
You can track latencies uring MetaApi.latencyMonitor API. Client-side latencies include network communication delays, thus the lowest client-side latencies are achieved if you host your app in AWS Ohio region.
```java
MetaApi api = new MetaApi("token", new MetaApi.Options() {{
  enableLatencyMonitor = true;
}});
LatencyMonitor monitor = api.getLatencyMonitor();
// retrieve trade latecy stats
System.out.println(monitor.getTradeLatencies());
// retrieve update streaming latency stats
System.out.println(monitor.getUpdateLatencies());
// retrieve quote streaming latency stats
System.out.println(monitor.getPriceLatencies());
// retrieve request latency stats
System.out.println(monitor.getRequestLatencies());
```

## Managing MetaTrader demo accounts via API
Please note that not all MT4/MT5 servers allows you to create demo accounts using the method below.
### Create a MetaTrader 4 demo account
```java
MetatraderDemoAccount demoAccount = api.getMetatraderDemoAccountApi()
  .createMT4DemoAccount(provisioningProfile.getId(), new NewMT4DemoAccount() {{
  balance = 100000;
  email = "example@example.com";
  leverage = 100;
  serverName = "Exness-Trial4";
}}).join();
```

### Create a MetaTrader 5 demo account
```java
MetatraderDemoAccount demoAccount = api.getMetatraderDemoAccountApi()
  .createMT5DemoAccount(provisioningProfile.getId(), new NewMT5DemoAccount() {{
  balance = 100000;
  email = "example@example.com";
  leverage = 100;
  serverName = "ICMarketsSC-Demo";
}}).join();
```

## Rate limits & quotas
API calls you make are subject to rate limits. See [MT account management API](https://metaapi.cloud/docs/provisioning/rateLimiting/) and [MetaApi API](https://metaapi.cloud/docs/client/rateLimiting/) for details.

MetaApi applies quotas to the number of accounts and provisioning profiles, for more details see the [MT account management API quotas](https://metaapi.cloud/docs/provisioning/userQuota/)

## CopyFactory copy trading API

CopyFactory is a powerful trade copying API which makes developing forex
trade copying applications as easy as writing few lines of code.

You can find CopyFactory Java SDK documentation here: [https://github.com/agiliumtrade-ai/copyfactory-java-sdk](https://github.com/agiliumtrade-ai/copyfactory-java-sdk)

## MetaStats trading statistics API

MetaStats is a powerful trade statistics API which makes it possible to add forex trading metrics into forex applications.

You can find MetaStats Java SDK documentation here: [https://github.com/agiliumtrade-ai/metastats-java-sdk](https://github.com/agiliumtrade-ai/metastats-java-sdk)
