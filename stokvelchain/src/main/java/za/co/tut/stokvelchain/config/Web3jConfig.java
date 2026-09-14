package za.co.tut.stokvelchain.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

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
         */
    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(rpcUrl));
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