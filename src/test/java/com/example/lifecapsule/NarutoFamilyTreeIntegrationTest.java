package com.example.lifecapsule;

import com.example.lifecapsule.entity.enumirated.FamilyVisibility;
import com.example.lifecapsule.entity.enumirated.Gender;
import com.example.lifecapsule.entity.enumirated.RelationshipType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Naruto oilasining shajarasini to'liq end-to-end tarzda yaratadi:
 *   signup -> login -> family -> persons -> relationships -> verification.
 * PostgreSQL ulanishi talab qilinadi (application.yml dagi default).
 */
@SpringBootTest
@AutoConfigureMockMvc
class NarutoFamilyTreeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void seedsNarutoFamilyTree() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "hokage_" + suffix;
        String email = username + "@konoha.test";
        String password = "RamenLover123!";

        // 1. Signup
        ObjectNode signup = objectMapper.createObjectNode()
                .put("username", username)
                .put("email", email)
                .put("firstName", "Naruto")
                .put("lastName", "Uzumaki")
                .put("password", password);
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signup.toString()))
                .andExpect(status().isOk());

        // 2. Login -> JWT
        ObjectNode login = objectMapper.createObjectNode()
                .put("username", username)
                .put("password", password);
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login.toString()))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsByteArray())
                .get("token").asText();
        assertNotNull(token);
        String bearer = "Bearer " + token;

        // 3. Family (Uzumaki klani)
        ObjectNode familyBody = objectMapper.createObjectNode()
                .put("name", "Uzumaki Clan " + suffix)
                .put("description", "Naruto Uzumaki oilasining shajarasi — Konohagakure")
                .put("visibility", FamilyVisibility.INVITE_ONLY.name());
        MvcResult familyResult = mockMvc.perform(post("/families")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(familyBody.toString()))
                .andExpect(status().isOk())
                .andReturn();
        long familyId = objectMapper.readTree(familyResult.getResponse().getContentAsByteArray())
                .get("id").asLong();

        // 4. Shajara a'zolari
        Map<String, Long> p = new LinkedHashMap<>();
        p.put("jiraiya", createPerson(bearer, familyId,
                "Jiraiya", null, Gender.MALE,
                LocalDate.of(1915, 11, 11), LocalDate.of(1985, 4, 20),
                "Mount Myoboku", "Sannin / Yozuvchi",
                "Legendary Sannin, Minatoning ustozi va Narutoning cho'qintirgan otasi"));

        p.put("minato", createPerson(bearer, familyId,
                "Minato", "Namikaze", Gender.MALE,
                LocalDate.of(1950, 1, 25), LocalDate.of(1985, 10, 10),
                "Konohagakure", "4-chi Hokage",
                "Yellow Flash — Hiraishin texnikasining sohibi"));

        p.put("kushina", createPerson(bearer, familyId,
                "Kushina", "Uzumaki", Gender.FEMALE,
                LocalDate.of(1950, 7, 10), LocalDate.of(1985, 10, 10),
                "Uzushiogakure", "Kyuubi jinchuriki",
                "Red Hot-Blooded Habanero — Uzumaki klanidan"));

        p.put("hiashi", createPerson(bearer, familyId,
                "Hiashi", "Hyuga", Gender.MALE,
                LocalDate.of(1940, 2, 8), null,
                "Konohagakure", "Hyuga klani boshlig'i",
                "Hinata va Hanabining otasi"));

        p.put("naruto", createPerson(bearer, familyId,
                "Naruto", "Uzumaki", Gender.MALE,
                LocalDate.of(1985, 10, 10), null,
                "Konohagakure", "7-chi Hokage",
                "Minato va Kushinaning o'g'li, Kyuubi jinchuriki"));

        p.put("hinata", createPersonWithMaiden(bearer, familyId,
                "Hinata", "Uzumaki", "Hyuga", Gender.FEMALE,
                LocalDate.of(1985, 12, 27), null,
                "Konohagakure", "Kunoichi",
                "Hyuga klanidan — Narutoning rafiqasi"));

        p.put("boruto", createPerson(bearer, familyId,
                "Boruto", "Uzumaki", Gender.MALE,
                LocalDate.of(2010, 3, 27), null,
                "Konohagakure", "Genin",
                "Naruto va Hinataning o'g'li"));

        p.put("himawari", createPerson(bearer, familyId,
                "Himawari", "Uzumaki", Gender.FEMALE,
                LocalDate.of(2012, 7, 15), null,
                "Konohagakure", "Akademiya o'quvchisi",
                "Naruto va Hinataning qizi"));

        // 5. Qarindoshlik aloqalari (PARENT / PARTNER / ADOPTIVE_PARENT)
        //    Jiraiya -> Minato (ustoz / cho'qintirgan ota)
        createRelationship(bearer, familyId, p.get("jiraiya"), p.get("minato"),
                RelationshipType.ADOPTIVE_PARENT, "Ustoz / cho'qintirgan ota");
        //    Minato <-> Kushina
        createRelationship(bearer, familyId, p.get("minato"), p.get("kushina"),
                RelationshipType.PARTNER, "Er-xotin");
        //    Minato -> Naruto, Kushina -> Naruto
        createRelationship(bearer, familyId, p.get("minato"), p.get("naruto"),
                RelationshipType.PARENT, "Ota");
        createRelationship(bearer, familyId, p.get("kushina"), p.get("naruto"),
                RelationshipType.PARENT, "Ona");
        //    Jiraiya -> Naruto (cho'qintirgan ota)
        createRelationship(bearer, familyId, p.get("jiraiya"), p.get("naruto"),
                RelationshipType.ADOPTIVE_PARENT, "Cho'qintirgan ota");
        //    Hiashi -> Hinata
        createRelationship(bearer, familyId, p.get("hiashi"), p.get("hinata"),
                RelationshipType.PARENT, "Ota");
        //    Naruto <-> Hinata
        createRelationship(bearer, familyId, p.get("naruto"), p.get("hinata"),
                RelationshipType.PARTNER, "Er-xotin");
        //    Naruto -> Boruto, Hinata -> Boruto
        createRelationship(bearer, familyId, p.get("naruto"), p.get("boruto"),
                RelationshipType.PARENT, "Ota");
        createRelationship(bearer, familyId, p.get("hinata"), p.get("boruto"),
                RelationshipType.PARENT, "Ona");
        //    Naruto -> Himawari, Hinata -> Himawari
        createRelationship(bearer, familyId, p.get("naruto"), p.get("himawari"),
                RelationshipType.PARENT, "Ota");
        createRelationship(bearer, familyId, p.get("hinata"), p.get("himawari"),
                RelationshipType.PARENT, "Ona");

        // 6. Verifikatsiya — personlar va aloqalar soni
        MvcResult personsPage = mockMvc.perform(get("/families/{id}/persons", familyId)
                        .header("Authorization", bearer)
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode personsJson = objectMapper.readTree(personsPage.getResponse().getContentAsByteArray());
        long personCount = personsJson.has("totalElements")
                ? personsJson.get("totalElements").asLong()
                : personsJson.get("content").size();
        assertTrue(personCount >= 8, "Shajarada kamida 8 ta a'zo bo'lishi kerak, bor: " + personCount);

        MvcResult relPage = mockMvc.perform(get("/families/{id}/relationships", familyId)
                        .header("Authorization", bearer)
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode relJson = objectMapper.readTree(relPage.getResponse().getContentAsByteArray());
        long relCount = relJson.has("totalElements")
                ? relJson.get("totalElements").asLong()
                : relJson.get("content").size();
        assertEquals(11, relCount, "11 ta qarindoshlik aloqasi kutilgan edi");

        System.out.println("\n==== Naruto shajarasi muvaffaqiyatli yaratildi ====");
        System.out.println("username = " + username + " / password = " + password);
        System.out.println("familyId = " + familyId);
        p.forEach((k, v) -> System.out.println("  " + k + " -> personId=" + v));
    }

    private long createPerson(String bearer, long familyId,
                              String firstName, String lastName, Gender gender,
                              LocalDate birthDate, LocalDate deathDate,
                              String birthPlace, String occupation, String biography) throws Exception {
        return createPersonWithMaiden(bearer, familyId, firstName, lastName, null,
                gender, birthDate, deathDate, birthPlace, occupation, biography);
    }

    private long createPersonWithMaiden(String bearer, long familyId,
                                        String firstName, String lastName, String maidenName,
                                        Gender gender, LocalDate birthDate, LocalDate deathDate,
                                        String birthPlace, String occupation, String biography) throws Exception {
        ObjectNode body = objectMapper.createObjectNode()
                .put("firstName", firstName);
        if (lastName != null) body.put("lastName", lastName);
        if (maidenName != null) body.put("maidenName", maidenName);
        if (gender != null) body.put("gender", gender.name());
        if (birthDate != null) body.put("birthDate", birthDate.toString());
        if (deathDate != null) body.put("deathDate", deathDate.toString());
        if (birthPlace != null) body.put("birthPlace", birthPlace);
        if (occupation != null) body.put("occupation", occupation);
        if (biography != null) body.put("biography", biography);

        MvcResult result = mockMvc.perform(post("/families/{id}/persons", familyId)
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .get("id").asLong();
    }

    private void createRelationship(String bearer, long familyId,
                                    long fromId, long toId,
                                    RelationshipType type, String note) throws Exception {
        ObjectNode body = objectMapper.createObjectNode()
                .put("fromPersonId", fromId)
                .put("toPersonId", toId)
                .put("type", type.name())
                .put("note", note);
        mockMvc.perform(post("/families/{id}/relationships", familyId)
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isOk());
    }
}
