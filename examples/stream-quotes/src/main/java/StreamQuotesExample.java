import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;

import cloud.metaapi.sdk.clients.meta_api.SynchronizationListener;
import cloud.metaapi.sdk.clients.meta_api.models.MarketDataSubscription;
import cloud.metaapi.sdk.clients.meta_api.models.MarketDataUnsubscription;
import cloud.metaapi.sdk.clients.meta_api.models.MetatraderAccountDto.ConnectionStatus;
import cloud.metaapi.sdk.clients.meta_api.models.MetatraderAccountDto.DeploymentState;
import cloud.metaapi.sdk.clients.meta_api.models.MetatraderBook;
import cloud.metaapi.sdk.clients.meta_api.models.MetatraderCandle;
import cloud.metaapi.sdk.clients.meta_api.models.MetatraderSymbolPrice;
import cloud.metaapi.sdk.clients.meta_api.models.MetatraderTick;
import cloud.metaapi.sdk.clients.models.IsoTime;
import cloud.metaapi.sdk.meta_api.MetaApi;
import cloud.metaapi.sdk.meta_api.MetaApiConnection;
import cloud.metaapi.sdk.meta_api.MetatraderAccount;
import cloud.metaapi.sdk.util.JsonMapper;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

/**
 * Note: for information on how to use this example code please read
 * https://metaapi.cloud/docs/client/usingCodeExamples
 */
public class StreamQuotesExample {

  private static String token = getEnvOrDefault("TOKEN", "<put in your token here>");
  private static String accountId = getEnvOrDefault("ACCOUNT_ID", "<put in your account id here>");
  private static String symbol = getEnvOrDefault("SYMBOL", "EURUSD");
  private static String domain = getEnvOrDefault("OMAIN", "agiliumtrade.agiliumtrade.ai");
  
  private static class QuoteListener extends SynchronizationListener {
    @Override
    public Future<Void> onSymbolPriceUpdated(String instanceIndex, MetatraderSymbolPrice price) {
      if (price.symbol.equals(symbol)) {
        try {
          System.out.println(symbol + " price updated " + asJson(price));
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
      }
      return Future.succeededFuture();
    }
    @Override
    public Future<Void> onCandlesUpdated(String instanceIndex, List<MetatraderCandle> candles,
      Double equity, Double margin, Double freeMargin, Double marginLevel, Double accountCurrencyExchangeRate) {
      for (MetatraderCandle candle : candles) {
        if (candle.symbol.equals(symbol)) {
          try {
            System.out.println(symbol + " candle updated " + asJson(candle));
          } catch (JsonProcessingException e) {
            e.printStackTrace();
          }
        }
      }
      return Future.succeededFuture();
    }
    @Override
    public Future<Void> onTicksUpdated(String instanceIndex, List<MetatraderTick> ticks,
      Double equity, Double margin, Double freeMargin, Double marginLevel, Double accountCurrencyExchangeRate) {
      for (MetatraderTick tick : ticks) {
        if (tick.symbol.equals(symbol)) {
          try {
            System.out.println(symbol + " tick updated " + asJson(tick));
          } catch (JsonProcessingException e) {
            e.printStackTrace();
          }
        }
      }
      return Future.succeededFuture();
    }
    @Override
    public Future<Void> onBooksUpdated(String instanceIndex, List<MetatraderBook> books,
      Double equity, Double margin, Double freeMargin, Double marginLevel, Double accountCurrencyExchangeRate) {
      for (MetatraderBook book : books) {
        if (book.symbol.equals(symbol)) {
          try {
            System.out.println(symbol + " order book updated " + asJson(book));
          } catch (JsonProcessingException e) {
            e.printStackTrace();
          }
        }
      }
      return Future.succeededFuture();
    }
    @Override
    public Future<Void> onSubscriptionDowngraded(String instanceIndex, String symbol,
      List<MarketDataSubscription> updates, List<MarketDataUnsubscription> unsubscriptions) {
      System.out.println("Market data subscriptions for " + symbol
        + " were downgraded by the server due to rate limits");
      return Future.succeededFuture();
    }
  }
  
  public static void main(String[] args) {
    try {
      MetaApi.Options apiOpts = new MetaApi.Options();
      apiOpts.domain = domain;
      MetaApi api = new MetaApi(token, Vertx.vertx());
      MetatraderAccount account = api.getMetatraderAccountApi().getAccount(accountId).toCompletionStage().toCompletableFuture().get();
      
      // wait until account is deployed and connected to broker
      System.out.println("Deploying account");
      if (account.getState() != DeploymentState.DEPLOYED) {
        account.deploy().toCompletionStage().toCompletableFuture().get();
      } else {
        System.out.println("Account already deployed");
      }
      System.out.println("Waiting for API server to connect to broker (may take couple of minutes)");
      if (account.getConnectionStatus() != ConnectionStatus.CONNECTED) {
        account.waitConnected().toCompletionStage().toCompletableFuture().get();
      }
      
      // connect to MetaApi API
      MetaApiConnection connection = account.connect().toCompletionStage().toCompletableFuture().get();
      
      SynchronizationListener quoteListener = new QuoteListener();
      connection.addSynchronizationListener(quoteListener);
      
      // wait until terminal state synchronized to the local state
      System.out.println("Waiting for SDK to synchronize to terminal state (may take some "
        + "time depending on your history size)");
      connection.waitSynchronized().toCompletionStage().toCompletableFuture().get();
      
      // Add symbol to MarketWatch if not yet added and subscribe to market data
      // Please note that currently only G1 and MT4 G2 instances support extended subscription management
      // Other instances will only stream quotes in response
      // Market depth streaming is available in MT5 only
      // ticks streaming is not available for MT4 G1
      connection.subscribeToMarketData(symbol, Arrays.asList(
        new MarketDataSubscription() {{ type = "quotes"; intervalInMilliseconds = 5000; }},
        new MarketDataSubscription() {{ type = "candles"; timeframe = "1m"; intervalInMilliseconds = 10000; }},
        new MarketDataSubscription() {{ type = "ticks"; }},
        new MarketDataSubscription() {{ type = "marketDepth"; intervalInMilliseconds = 5000; }}
      )).toCompletionStage().toCompletableFuture().join();
      System.out.println("Price after subscribe: " + asJson(connection.getTerminalState().getPrice(symbol)));
      
      System.out.println("[" + new IsoTime().toString() + "] Synchronized successfully, streaming "
        + symbol + " market data now...");
      
      while (true) {
        Thread.sleep(1000);
      }
      
    } catch (Exception err) {
      System.err.println(err);
    }
  }
  
  private static String getEnvOrDefault(String name, String defaultValue) {
    String result = System.getenv(name);
    return (result != null ? result : defaultValue);
  }
  
  private static String asJson(Object object) throws JsonProcessingException {
    return JsonMapper.getInstance().writeValueAsString(object);
  }
}