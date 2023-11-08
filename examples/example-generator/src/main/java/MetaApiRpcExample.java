import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonProcessingException;

import cloud.metaapi.sdk.clients.meta_api.TradeException;
import cloud.metaapi.sdk.clients.meta_api.models.MetatraderAccountDto.DeploymentState;
import cloud.metaapi.sdk.clients.meta_api.models.MetatraderTradeResponse;
import cloud.metaapi.sdk.clients.meta_api.models.PendingTradeOptions;
import cloud.metaapi.sdk.clients.models.IsoTime;
import cloud.metaapi.sdk.meta_api.MetaApi;
import cloud.metaapi.sdk.meta_api.MetaApiConnection;
import cloud.metaapi.sdk.meta_api.MetatraderAccount;
import cloud.metaapi.sdk.util.JsonMapper;
import io.vertx.core.Vertx;

/**
 * Note: for information on how to use this example code please read https://metaapi.cloud/docs/client/usingCodeExamples
 */
public class MetaApiRpcExample {

  private static final String token = getEnvOrDefault("TOKEN", "<put in your token here>");
  private static final String accountId = getEnvOrDefault("ACCOUNT_ID", "<put in your account id here>");
  private static final Vertx vertx = Vertx.vertx();
  
  public static void main(String[] args) {
    try {
      MetaApi api = new MetaApi(token,vertx);

      MetatraderAccount account = api.getMetatraderAccountApi().getAccount(accountId)
              .toCompletionStage().toCompletableFuture().get();
      DeploymentState initialState = account.getState();
      List<DeploymentState> deployedStates = new ArrayList<>();
      deployedStates.add(DeploymentState.DEPLOYING);
      deployedStates.add(DeploymentState.DEPLOYED);
      
      if (!deployedStates.contains(initialState)) {
        // wait until account is deployed and connected to broker
        System.out.println("Deploying account");
        account.deploy().toCompletionStage().toCompletableFuture().get();
      }
      
      System.out.println("Waiting for API server to connect to broker (may take couple of minutes)");
      account.waitConnected().toCompletionStage().toCompletableFuture().get();
      
      // connect to MetaApi API
      MetaApiConnection connection = account.connect().toCompletionStage().toCompletableFuture().get();
      
      System.out.println("Waiting for SDK to synchronize to terminal state "
        + "(may take some time depending on your history size)");
      connection.waitSynchronized().get();
      
      // invoke RPC API (replace ticket numbers with actual ticket numbers which exist in your MT account)
      System.out.println("Testing MetaAPI RPC API");
      System.out.println("account information: " + asJson(connection.getAccountInformation().get()));
      System.out.println("positions: " + asJson(connection.getPositions().get()));
      System.out.println("open orders:" + asJson(connection.getOrders().get()));
      System.out.println("history orders by ticket: " + asJson(connection.getHistoryOrdersByTicket("1234567").get()));
      System.out.println("history orders by position: " + asJson(connection.getHistoryOrdersByPosition("1234567").get()));
      System.out.println("history orders (~last 3 months): " + asJson(connection.getHistoryOrdersByTimeRange(
        new IsoTime(Date.from(Instant.now().plusSeconds(-90 * 24 * 60 * 60))), 
        new IsoTime(Date.from(Instant.now())),
        0, 1000).get()));
      System.out.println("history deals by ticket: " + asJson(connection.getDealsByTicket("1234567").get()));
      System.out.println("history deals by position: " + asJson(connection.getDealsByPosition("1234567").get()));
      System.out.println("history deals (~last 3 months): " + asJson(connection.getDealsByTimeRange(
        new IsoTime(Date.from(Instant.now().plusSeconds(-90 * 24 * 60 * 60))), 
        new IsoTime(Date.from(Instant.now())), 
        0, 1000).get()));
      
      // trade
      System.out.println("Submitting pending order");
      try {
        MetatraderTradeResponse result = connection
          .createLimitBuyOrder("GBPUSD", 0.07, 1.0, 0.9, 2.0, new PendingTradeOptions() {{
            comment = "comm"; clientId = "TE_GBPUSD_7hyINWqAlE"; 
          }}).get();
        System.out.println("Trade successful, result code is " + result.stringCode);
      } catch (ExecutionException err) {
        System.out.println("Trade failed with result code " + ((TradeException) err.getCause()).stringCode);
      }
      
      if (!deployedStates.contains(initialState)) {
        // finally, undeploy account
        System.out.println("Undeploying account so that it does not consume any unwanted resources");
        account.undeploy().toCompletionStage().toCompletableFuture().get();
      }
      
    } catch (Exception err) {
      System.err.println(err.getMessage());
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