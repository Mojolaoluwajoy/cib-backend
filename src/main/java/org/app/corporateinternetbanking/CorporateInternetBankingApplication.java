package org.app.corporateinternetbanking;

import org.app.corporateinternetbanking.user.superAdminConfiguration.SuperAdminCommand;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class CorporateInternetBankingApplication {

    public static void main(String[] args) {
        if (args.length > 0 && SuperAdminCommand.NAME.equals(args[0])) {
            System.exit(SuperAdminCommand.run(args));
        }
        SpringApplication.run(CorporateInternetBankingApplication.class, args);
    }
}
