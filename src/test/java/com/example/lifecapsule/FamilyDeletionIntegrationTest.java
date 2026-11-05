package com.example.lifecapsule;

import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.Role;
import com.example.lifecapsule.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Deleting a family only marks it deleted: it disappears for its members and for admins, an admin
 * still sees it in the admin list with who deleted it and when, and restoring brings everything back.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FamilyDeletionIntegrationTest {
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void deletedFamilyIsHiddenUntilAnAdminRestoresIt() throws Exception {
        String ownerName = "owner_" + UUID.randomUUID().toString().substring(0, 8);
        String owner = signupAndLogin(ownerName);
        long familyId = create(owner, "/families", objectMapper.createObjectNode().put("name", "Delete me " + ownerName).put("visibility", "INVITE_ONLY"));
        long personId = create(owner, "/families/" + familyId + "/persons", objectMapper.createObjectNode().put("firstName", "Ali").put("gender", "MALE"));
        mockMvc.perform(multipart("/families/{familyId}/persons/{personId}/media", familyId, personId)
                        .file(new MockMultipartFile("file", "photo.png", "image/png", PNG))
                        .header("Authorization", owner))
                .andExpect(status().isOk());

        String adminName = "admin_" + UUID.randomUUID().toString().substring(0, 8);
        signupAndLogin(adminName);
        Users adminUser = userRepository.findByUserName(adminName).orElseThrow();
        adminUser.setRole(Role.ADMIN);
        userRepository.save(adminUser);
        String admin = login(adminName);

        mockMvc.perform(delete("/families/{familyId}", familyId).header("Authorization", owner))
                .andExpect(status().isNoContent());

        // Gone for the owner, in the list and by id, and for the admin by id.
        assertFalse(json(get("/families"), owner).get("content").toString().contains("\"id\":" + familyId + ","));
        mockMvc.perform(get("/families/{familyId}", familyId).header("Authorization", owner)).andExpect(status().isNotFound());
        mockMvc.perform(get("/families/{familyId}/persons", familyId).header("Authorization", owner)).andExpect(status().isNotFound());
        mockMvc.perform(get("/families/{familyId}", familyId).header("Authorization", admin)).andExpect(status().isNotFound());

        // The admin list still has it, marked deleted, with who and when.
        JsonNode deleted = adminFamily(admin, familyId);
        assertEquals("DELETED", deleted.get("status").asText());
        assertEquals(ownerName, deleted.get("deletedByUsername").asText());
        assertNotNull(deleted.get("deletedAt").textValue());

        mockMvc.perform(post("/admin/families/{familyId}/restore", familyId).header("Authorization", owner))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/families/{familyId}/restore", familyId).header("Authorization", admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/admin/families/{familyId}/restore", familyId).header("Authorization", admin))
                .andExpect(status().isConflict());

        // Restored with everything that was in it.
        JsonNode restored = adminFamily(admin, familyId);
        assertEquals("ACTIVE", restored.get("status").asText());
        assertNull(restored.get("deletedAt").textValue());
        assertEquals(1, json(get("/families/" + familyId + "/persons"), owner).get("content").size());
        assertEquals(1, json(get("/families/" + familyId + "/media"), owner).size());
    }

    private JsonNode adminFamily(String admin, long familyId) throws Exception {
        for (JsonNode family : json(get("/admin/families?size=100&sortBy=createdAt&direction=desc"), admin).get("content")) {
            if (family.get("id").asLong() == familyId) return family;
        }
        throw new AssertionError("family " + familyId + " is not in the admin list");
    }

    private JsonNode json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String bearer) throws Exception {
        MvcResult result = mockMvc.perform(request.header("Authorization", bearer)).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private String signupAndLogin(String username) throws Exception {
        ObjectNode signup = objectMapper.createObjectNode()
                .put("username", username)
                .put("email", username + "@example.test")
                .put("firstName", "Test")
                .put("lastName", "User")
                .put("password", "Secret123!");
        mockMvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signup.toString()))
                .andExpect(status().isOk());
        return login(username);
    }

    private String login(String username) throws Exception {
        ObjectNode login = objectMapper.createObjectNode().put("username", username).put("password", "Secret123!");
        MvcResult result = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(login.toString()))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + objectMapper.readTree(result.getResponse().getContentAsByteArray()).get("token").asText();
    }

    private long create(String bearer, String path, ObjectNode body) throws Exception {
        MvcResult result = mockMvc.perform(post(path).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray()).get("id").asLong();
    }
}
