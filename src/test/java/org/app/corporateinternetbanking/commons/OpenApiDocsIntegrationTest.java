package org.app.corporateinternetbanking.commons;

import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.app.corporateinternetbanking.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

class OpenApiDocsIntegrationTest extends IntegrationTestBase {

    @Test
    void docsArePublicAndDeclareJwtBearerAuth() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme")
                        .value("bearer"))
                .andExpect(jsonPath("$.security[0].bearerAuth").exists())
                // Public endpoints opt out, so Swagger UI calls them without a token
                .andExpect(jsonPath("$.paths['/auth/login'].post.security").value(empty()))
                .andExpect(
                        jsonPath("$.paths['/webhook/paystack'].post.security").value(empty()))
                .andExpect(jsonPath("$.paths['/dashboard/stats'].get.security").doesNotExist());

        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
