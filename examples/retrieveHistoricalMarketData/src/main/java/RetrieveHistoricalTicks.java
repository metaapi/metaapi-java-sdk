import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;

import cloud.metaapi.sdk.clients.meta_api.models.MetatraderAccountDto.ConnectionStatus;
import cloud.metaapi.sdk.clients.meta_api.models.MetatraderAccountDto.DeploymentState;
import cloud.metaapi.sdk.clients.meta_api.models.MetatraderTick;
import cloud.metaapi.sdk.clients.models.IsoTime;
import cloud.metaapi.sdk.meta_api.MetaApi;
import cloud.metaapi.sdk.meta_api.MetatraderAccount;
import cloud.metaapi.sdk.util.JsonMapper;

public class RetrieveHistoricalTicks {

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
      System.out.println("Downloading " + pages + "K ticks for " + symbol + " starting from 7 days ago");
      long startedAt = Date.from(Instant.now()).getTime();
      IsoTime startTime = new IsoTime(Instant.now().minus(7, ChronoUnit.DAYS));
      int offset = 0;
      List<MetatraderTick> ticks = new ArrayList<>();
      for (int i = 0; i < pages; i++) {
        // the API to retrieve historical market data is currently available for G1 only
        // historical ticks can be retrieved from MT5 only
        ticks = account.getHistoricalTicks(symbol, startTime, offset, 1000).join();
        System.out.println("Downloaded " + ticks.size() + " historical ticks for " + symbol);
        if (!ticks.isEmpty()) {
          startTime = ticks.get(ticks.size() - 1).time;
          offset = 0;
          while (ticks.size() - 1 - offset >= 0 &&
            ticks.get(ticks.size() - 1 - offset).time.getDate().equals(startTime.getDate())) {
            offset++;
          }
          System.out.println("First tick time is " + startTime + ", offset is " + offset);
        }
      }
      if (!ticks.isEmpty()) {
        System.out.println("Last tick is " + asJson(ticks.get(ticks.size() - 1)));
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