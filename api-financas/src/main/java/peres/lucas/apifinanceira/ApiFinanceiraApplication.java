package peres.lucas.apifinanceira;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ApiFinanceiraApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiFinanceiraApplication.class, args);
    }

}
