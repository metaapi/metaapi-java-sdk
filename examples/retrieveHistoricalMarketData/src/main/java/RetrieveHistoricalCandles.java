import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;

import cloud.metaapi.sdk.clients.meta_api.models.MetatraderAccountDto.ConnectionStatus;
import cloud.metaapi.sdk.clients.meta_api.models.MetatraderAccountDto.DeploymentState;
import cloud.metaapi.sdk.clients.meta_api.models.MetatraderCandle;
import cloud.metaapi.sdk.clients.models.IsoTime;
import cloud.metaapi.sdk.meta_api.MetaApi;
import cloud.metaapi.sdk.meta_api.MetatraderAccount;
import cloud.metaapi.sdk.util.JsonMapper;

public class RetrieveHistoricalCandles {

  private static String token = getEnvOrDefault("TOKEN", "<put in your token here>");
  private static String accountId = getEnvOrDefault("ACCOUNT_ID", "<put in your account id here>");
  private static String symbol = getEnvOrDefault("SYMBOL", "EURUSD");
  private static String apiDomain = getEnvOrDefault("DOMAIN", "agiliumtrade.agiliumtrade.ai");
  
  public static void main(String[] args) {
    try {
      MetaApi api = new MetaApi(token, new MetaApi.Options() {{domain = apiDomain;}});
      
      MetatraderAccount account = api.getMetatraderAccountApi().getAccount(accountId).join();

      // wait until account is deployed and connected to broker
      System.out.println("Deploying account");
      if (account.getState() != DeploymentState.DEPLOYED) {
        account.deploy().join();
      } else {
        System.out.println("Account already deployed");
      }
      System.out.println("Waiting for API server to connect to broker (may take couple of minutes)");
      if (account.getConnectionStatus() != ConnectionStatus.CONNECTED) {
        account.waitConnected().join();
      }

      // retrieve last 10K 1m candles
      int pages = 10;
      System.out.println("Downloading " + pages + "K latest candles for " + symbol);
      long startedAt = Date.from(Instant.now()).getTime();
      IsoTime startTime = null;
      List<MetatraderCandle> candles = new ArrayList<>();
      for (int i = 0; i < pages; i++) {
        // the API to retrieve historical market data is currently available for G1 and MT4 G2 only
        List<MetatraderCandle> newCandles = account.getHistoricalCandles(symbol, "1m", startTime).join();
        System.out.println("Downloaded " + newCandles.size() + " historical candles for " + symbol);
        if (newCandles.size() != 0) {
          candles = newCandles;
        }
        if (!candles.isEmpty()) {
          startTime = candles.get(0).time;
          startTime.setTime(startTime.getDate().toInstant().minusSeconds(60));
          System.out.println("First candle time is " + startTime);
        }
      }
      if (!candles.isEmpty()) {
        System.out.println("First candle is " + asJson(candles.get(0)));
      }
      System.out.println("Took " + (Date.from(Instant.now()).getTime() - startedAt) + "ms");
      
    } catch (Exception err) {
      System.err.println(err);
    }
    System.exit(0);
  }
  
  private static String getEnvOrDefault(String name, String defaultValue) {
    String result = System.getenv(name);
    return (result != null ? result : defaultValue);
  }
  
  private static String asJson(Object object) throws JsonProcessingException {
    return JsonMapper.getInstance().writeValueAsString(object);
  }
}