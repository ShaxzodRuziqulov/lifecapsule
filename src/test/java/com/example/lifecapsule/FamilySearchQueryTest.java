package com.example.lifecapsule;

import com.example.lifecapsule.entity.enumirated.AccessStatus;
import com.example.lifecapsule.repository.FamilyAccessRepository;
import com.example.lifecapsule.repository.FamilyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional(readOnly = true)
class FamilySearchQueryTest {
    @Autowired FamilyRepository families;
    @Autowired FamilyAccessRepository accesses;

    @Test
    void familyQueriesAcceptNullSearchOnPostgres() {
        var page = PageRequest.of(0, 1);
        assertNotNull(families.searchAllFamiliesPaging(null, page));
        assertNotNull(accesses.searchMyFamiliesPaging(-1L, AccessStatus.ACTIVE, null, page));
    }

    @Test
    void familyQueriesAcceptTextSearchOnPostgres() {
        var page = PageRequest.of(0, 1);
        assertNotNull(families.searchAllFamiliesPaging("query-regression-check", page));
        assertNotNull(accesses.searchMyFamiliesPaging(-1L, AccessStatus.ACTIVE, "query-regression-check", page));
    }
}
