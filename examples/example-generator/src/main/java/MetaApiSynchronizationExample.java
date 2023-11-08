import cloud.metaapi.sdk.clients.meta_api.TradeException;
import cloud.metaapi.sdk.clients.meta_api.models.MetatraderAccountDto.DeploymentState;
import cloud.metaapi.sdk.clients.meta_api.models.MetatraderTradeResponse;
import cloud.metaapi.sdk.clients.meta_api.models.PendingTradeOptions;
import cloud.metaapi.sdk.meta_api.MetaApi;
import cloud.metaapi.sdk.meta_api.MetaApiConnection;
import cloud.metaapi.sdk.meta_api.MetatraderAccount;
import cloud.metaapi.sdk.meta_api.TerminalState;
import cloud.metaapi.sdk.util.JsonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.vertx.core.Vertx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Note: for information on how to use this example code please read https://metaapi.cloud/docs/client/usingCodeExamples
 */
public class MetaApiSynchronizationExample {

    private static final String token = getEnvOrDefault("TOKEN", "<put in your token here>");
    private static final String accountId = getEnvOrDefault("ACCOUNT_ID", "<put in your account id here>");

    private static final Vertx vertx = Vertx.vertx();

    public static void main(String[] args) {
        try {
            MetaApi api = new MetaApi(token, vertx);

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

            // access local copy of terminal state
            System.out.println("Testing terminal state access");
            TerminalState terminalState = connection.getTerminalState();
            System.out.println("connected: " + terminalState.isConnected());
            System.out.println("connected to broker: " + terminalState.isConnectedToBroker());
            System.out.println("account information: " + asJson(terminalState.getAccountInformation().orElse(null)));
            System.out.println("positions: " + asJson(terminalState.getPositions()));
            System.out.println("orders: " + asJson(terminalState.getOrders()));
            System.out.println("specifications: " + asJson(terminalState.getSpecifications()));
            System.out.println("EURUSD specification: " + asJson(terminalState.getSpecification("EURUSD").orElse(null)));
            connection.subscribeToMarketData("EURUSD").join();
            System.out.println("EURUSD price: " + asJson(terminalState.getPrice("EURUSD").orElse(null)));

            // trade
            System.out.println("Submitting pending order");
            try {
                MetatraderTradeResponse result = connection
                        .createLimitBuyOrder("GBPUSD", 0.07, 1.0, 0.9, 2.0, new PendingTradeOptions() {{
                            comment = "comm";
                            clientId = "TE_GBPUSD_7hyINWqAlE";
                        }}).get();
                System.out.println("Trade successful, result code is " + result.stringCode);
            } catch (ExecutionException err) {
                System.out.println("Trade failed with result code " + ((TradeException) err.getCause()).stringCode);
            }

            if (!deployedStates.contains(initialState)) {
                // undeploy account if it was undeployed
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
