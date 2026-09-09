package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.*;
import com.example.lifecapsule.entity.enumirated.*;
import com.example.lifecapsule.errors.ConflictException;
import com.example.lifecapsule.errors.ForbiddenException;
import com.example.lifecapsule.errors.NotFoundException;
import com.example.lifecapsule.repository.*;
import com.example.lifecapsule.service.dto.*;
import com.example.lifecapsule.service.mapper.RelationshipMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static com.example.lifecapsule.entity.enumirated.RelationshipType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelationshipServiceTest {
    @Mock RelationshipRepository relationships;
    @Mock PersonRepository people;
    @Mock FamilyAccessRepository accesses;
    @Mock FamilyRepository families;
    @Mock RelationshipMapper mapper;
    @InjectMocks RelationshipService service;
    private final Users user = new Users();
    private final Family family = new Family();
    private final FamilyAccess access = new FamilyAccess();

    @BeforeEach void setup() {
        user.setId(1L);
        family.setId(10L);
        access.setFamily(family);
        access.setStatus(AccessStatus.ACTIVE);
        access.setAccessRole(FamilyAccessRole.EDITOR);
        when(accesses.findByFamilyIdAndUserId(10L, 1L)).thenReturn(Optional.of(access));
        lenient().when(families.findByIdForUpdate(10L)).thenReturn(Optional.of(family));
    }

    @Test void rejectsSelfRelationship() {
        assertThrows(IllegalArgumentException.class, () -> create(1, 1, PARENT));
        verify(relationships, never()).save(any());
    }

    @Test void rejectsDirectCycle() {
        graph(edge(1, 1, 2, PARENT));
        assertThrows(ConflictException.class, () -> create(2, 1, PARENT));
        verify(relationships, never()).save(any());
    }

    @Test void rejectsMixedAncestryCycle() {
        graph(edge(1, 1, 2, PARENT), edge(2, 2, 3, ADOPTIVE_PARENT));
        assertThrows(ConflictException.class, () -> create(3, 1, PARENT));
        assertThrows(ConflictException.class, () -> create(3, 1, ADOPTIVE_PARENT));
        verify(relationships, never()).save(any());
    }

    @Test void rejectsReversedAndExactPartnerDuplicates() {
        graph(edge(1, 1, 2, PARTNER));
        assertThrows(ConflictException.class, () -> create(2, 1, PARTNER));
        assertThrows(ConflictException.class, () -> create(1, 2, PARTNER));
    }

    @Test void rejectsExactParentDuplicate() {
        graph(edge(1, 1, 2, PARENT));
        assertThrows(ConflictException.class, () -> create(1, 2, PARENT));
    }

    @Test void allowsSharedDescendantsAndIgnoresPartnerEdges() {
        graph(edge(1, 1, 3, PARENT), edge(2, 2, 3, PARENT), edge(3, 3, 4, PARTNER));
        allowSave();
        create(4, 1, PARENT);
        verify(relationships).save(any());
        InOrder order = inOrder(families, relationships);
        order.verify(families).findByIdForUpdate(10L);
        order.verify(relationships).findAllByFamilyIdOrderByCreatedAtAsc(10L);
        order.verify(relationships).save(any());
    }

    @Test void updateCanReverseItsOwnEdge() {
        Relationship existing = edge(1, 1, 2, PARENT);
        graph(existing);
        when(relationships.findByIdAndFamilyId(1L, 10L)).thenReturn(Optional.of(existing));
        allowPeople();
        service.updateRelationship(user, 10L, 1L, update(2, 1, PARENT));
        verify(relationships).save(existing);
        assertEquals(2L, existing.getFromPerson().getId());
    }

    @Test void updateCannotCreateCycle() {
        Relationship existing = edge(3, 4, 5, PARENT);
        graph(edge(1, 1, 2, PARENT), edge(2, 2, 3, ADOPTIVE_PARENT), existing);
        when(relationships.findByIdAndFamilyId(3L, 10L)).thenReturn(Optional.of(existing));
        assertThrows(ConflictException.class, () -> service.updateRelationship(user, 10L, 3L, update(3, 1, PARENT)));
        verify(relationships, never()).save(any());
        assertEquals(4L, existing.getFromPerson().getId());
    }

    @Test void updateCannotDuplicateAnotherPartner() {
        Relationship existing = edge(3, 4, 5, PARTNER);
        graph(edge(1, 1, 2, PARTNER), existing);
        when(relationships.findByIdAndFamilyId(3L, 10L)).thenReturn(Optional.of(existing));
        assertThrows(ConflictException.class, () -> service.updateRelationship(user, 10L, 3L, update(2, 1, PARTNER)));
    }

    @Test void viewerCannotWriteOrLockFamily() {
        access.setAccessRole(FamilyAccessRole.VIEWER);
        assertThrows(ForbiddenException.class, () -> create(1, 2, PARENT));
        verifyNoInteractions(families, relationships);
    }

    @Test void rejectsPersonOutsideFamily() {
        graph();
        when(mapper.toEntity(any())).thenReturn(new Relationship());
        assertThrows(NotFoundException.class, () -> create(1, 2, PARENT));
        verify(relationships, never()).save(any());
    }

    private void graph(Relationship... edges) {
        when(relationships.findAllByFamilyIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(edges));
    }
    private void allowPeople() {
        when(people.findByIdAndFamilyId(anyLong(), eq(10L)))
                .thenAnswer(call -> Optional.of(person(call.getArgument(0))));
    }
    private void allowSave() {
        allowPeople();
        when(mapper.toEntity(any())).thenReturn(new Relationship());
    }
    private void create(long from, long to, RelationshipType type) {
        CreateRelationshipDto dto = new CreateRelationshipDto();
        dto.setFromPersonId(from); dto.setToPersonId(to); dto.setType(type);
        service.createRelationship(user, 10L, dto);
    }
    private UpdateRelationshipDto update(long from, long to, RelationshipType type) {
        UpdateRelationshipDto dto = new UpdateRelationshipDto();
        dto.setFromPersonId(from); dto.setToPersonId(to); dto.setType(type);
        return dto;
    }
    private Person person(long id) { Person p = new Person(); p.setId(id); return p; }
    private Relationship edge(long id, long from, long to, RelationshipType type) {
        Relationship r = new Relationship();
        r.setId(id); r.setFamily(family); r.setFromPerson(person(from)); r.setToPerson(person(to)); r.setType(type);
        return r;
    }
}
