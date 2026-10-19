package za.co.tut.stokvelchain.config;


import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.time.Duration;

@Configuration
public class Web3jConfig {

    @Value("${blockchain.rpc-url}")
    private String rpcUrl;

    @Value("${blockchain.wallet-private-key}")
    private String walletPrivateKey;

    /**
     * Web3j client pointed at the configured RPC endpoint (Sepolia).
     * A single shared instance is fine for this service — Web3j is
     * thread-safe and expensive to construct repeatedly.
     * Longer HTTP timeouts allow for slow responses from Sepolia / the RPC provider.
     */
    @Bean
    public Web3j web3j() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .build();
        return Web3j.build(new HttpService(rpcUrl, httpClient));
    }

    /**
     * Credentials for the backend's service wallet — the account that
     * owns the deployed StokvelLedger contract (onlyOwner-restricted
     * recordTransaction calls sign with this).
     */
    @Bean
    public Credentials credentials() {
        return Credentials.create(walletPrivateKey);
    }
}