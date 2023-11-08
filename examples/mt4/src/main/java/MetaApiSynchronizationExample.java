import cloud.metaapi.sdk.clients.meta_api.TradeException;
import cloud.metaapi.sdk.clients.meta_api.models.MetatraderTradeResponse;
import cloud.metaapi.sdk.clients.meta_api.models.NewMetatraderAccountDto;
import cloud.metaapi.sdk.clients.meta_api.models.NewProvisioningProfileDto;
import cloud.metaapi.sdk.clients.meta_api.models.PendingTradeOptions;
import cloud.metaapi.sdk.meta_api.*;
import cloud.metaapi.sdk.util.JsonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.vertx.core.Vertx;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Note: for information on how to use this example code please read https://metaapi.cloud/docs/client/usingCodeExamples
 */
public class MetaApiSynchronizationExample {

    private static final String token = getEnvOrDefault("TOKEN", "<put in your token here>");
    private static final String login = getEnvOrDefault("LOGIN", "<put in your MT login here>");
    private static final String password = getEnvOrDefault("PASSWORD", "<put in your MT password here>");
    private static final String serverName = getEnvOrDefault("SERVER", "<put in your MT server name here>");
    private static final String brokerSrvFile = getEnvOrDefault("PATH_TO_BROKER_SRV", "/path/to/your/broker.srv");

    private static final Vertx vertx=Vertx.vertx();

    public static void main(String[] args) {
        try {
            MetaApi api = new MetaApi(token,vertx);
            List<ProvisioningProfile> profiles = api.getProvisioningProfileApi().getProvisioningProfiles()
                    .toCompletionStage().toCompletableFuture().get();

            // create test MetaTrader account profile
            Optional<ProvisioningProfile> profile = profiles.stream()
                    .filter(p -> p.getName().equals(serverName))
                    .findFirst();
            if (!profile.isPresent()) {
                System.out.println("Creating account profile");
                NewProvisioningProfileDto newDto = new NewProvisioningProfileDto() {{
                    name = serverName;
                    version = 4;
                    brokerTimezone = "EET";
                    brokerDSTSwitchTimezone = "EET";
                }};
                profile = Optional.of(api.getProvisioningProfileApi().createProvisioningProfile(newDto)
                        .toCompletionStage().toCompletableFuture().get());
                profile.get().uploadFile(brokerSrvFile).toCompletionStage().toCompletableFuture().get();
            }
            if (profile.get().getStatus().equals("new")) {
                System.out.println("Uploading broker.srv");
                profile.get().uploadFile(brokerSrvFile).toCompletionStage().toCompletableFuture().get();
            } else {
                System.out.println("Account profile already created");
            }

            // Add test MetaTrader account
            List<MetatraderAccount> accounts = api.getMetatraderAccountApi().getAccounts()
                    .toCompletionStage().toCompletableFuture().get();
            Optional<MetatraderAccount> account = accounts.stream()
                    .filter(a -> a.getLogin().equals(login) && a.getType().startsWith("cloud"))
                    .findFirst();
            if (!account.isPresent()) {
                System.out.println("Adding MT4 account to MetaApi");
                String mtLogin = login;
                String mtPassword = password;
                ProvisioningProfile provisioningProfile = profile.get();
                account = Optional.of(api.getMetatraderAccountApi().createAccount(new NewMetatraderAccountDto() {{
                    name = "Test account";
                    type = "cloud";
                    login = mtLogin;
                    password = mtPassword;
                    server = serverName;
                    provisioningProfileId = provisioningProfile.getId();
                    application = "MetaApi";
                    magic = 1000;
                }}).toCompletionStage().toCompletableFuture().get());
            } else {
                System.out.println("MT4 account already added to MetaApi");
            }

            // wait until account is deployed and connected to broker
            System.out.println("Deploying account");
            account.get().deploy().toCompletionStage().toCompletableFuture().get();
            System.out.println("Waiting for API server to connect to broker (may take couple of minutes)");
            account.get().waitConnected().toCompletionStage().toCompletableFuture().get();

            // connect to MetaApi API
            MetaApiConnection connection = account.get().connect().toCompletionStage().toCompletableFuture().get();

            System.out.println("Waiting for SDK to synchronize to terminal state "
                    + "(may take some time depending on your history size)");
            connection.waitSynchronized().toCompletionStage().toCompletableFuture().get();

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
                        }}).toCompletionStage().toCompletableFuture().get();
                System.out.println("Trade successful, result code is " + result.stringCode);
            } catch (ExecutionException err) {
                System.out.println("Trade failed with result code " + ((TradeException) err.getCause()).stringCode);
            }

            // finally, undeploy account
            System.out.println("Undeploying MT4 account so that it does not consume any unwanted resources");
            account.get().undeploy().toCompletionStage().toCompletableFuture().get();
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