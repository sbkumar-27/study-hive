package com.studyhive.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end check (real Spring context, H2 instead of MySQL) that the full register -> create
 * community -> upload material -> access-control chain is wired correctly, matching the
 * "Definition of done" golden path in the master plan: a member can upload, and a private
 * material cannot be accessed by another user.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaterialAccessControlIntegrationTest {

    @Autowired private MockMvc mockMvc;

    private String registerAndGetToken(String username) throws Exception {
        String body = """
                {"username":"%s","email":"%s@example.com","password":"Password123!","displayName":"%s"}
                """.formatted(username, username, username);
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.token");
    }

    @Test
    void memberCanUploadAndAccess_outsiderIsForbidden() throws Exception {
        String ownerToken = registerAndGetToken("owner1");
        String outsiderToken = registerAndGetToken("outsider1");

        String communityResponse = mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Community\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long communityId = ((Number) JsonPath.read(communityResponse, "$.communityId")).longValue();

        MockMultipartFile file =
                new MockMultipartFile("file", "note.txt", "text/plain", "hello world".getBytes());
        MockMultipartFile metadata = new MockMultipartFile(
                "metadata",
                "metadata",
                MediaType.APPLICATION_JSON_VALUE,
                ("{\"title\":\"Note\",\"materialType\":\"NOTE\",\"communityId\":" + communityId + "}")
                        .getBytes());

        String materialResponse = mockMvc.perform(multipart("/api/v1/materials")
                        .file(file)
                        .file(metadata)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long materialId = ((Number) JsonPath.read(materialResponse, "$.materialId")).longValue();

        mockMvc.perform(get("/api/v1/materials/" + materialId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/materials/" + materialId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/materials/" + materialId + "/download")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/materials/1")).andExpect(status().isUnauthorized());
    }
}
