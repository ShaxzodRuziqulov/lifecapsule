package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.Person;
import com.example.lifecapsule.entity.Relationship;
import com.example.lifecapsule.entity.enumirated.Gender;
import com.example.lifecapsule.entity.enumirated.RelationshipType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MahramRulesTest {
    private final Map<Long, Person> people = new HashMap<>();
    private final List<Relationship> links = new ArrayList<>();

    // Ids: the man is 1. Every other person is named by how she relates to him.
    private static final long MAN = 1, FATHER = 2, MOTHER = 3, GRANDFATHER = 4, GRANDMOTHER = 5,
            SISTER = 6, HALF_SISTER = 7, STEPMOTHER = 8, AUNT = 9, NIECE = 10, GRAND_NIECE = 11,
            WIFE = 12, MOTHER_IN_LAW = 13, STEPDAUGHTER = 14, DAUGHTER = 15, SON = 16, DAUGHTER_IN_LAW = 17,
            GRANDDAUGHTER = 18, COUSIN = 19, SISTER_IN_LAW = 20, ADOPTED_DAUGHTER = 21, STRANGER = 22,
            WIFE_EX_HUSBAND = 23, BROTHER = 24, BROTHERS_WIFE = 25, UNCLE = 26;

    @Test
    void findsEveryMahramAndNobodyElse() {
        parent(GRANDFATHER, FATHER); parent(GRANDMOTHER, FATHER);
        parent(GRANDFATHER, AUNT); parent(GRANDFATHER, UNCLE);
        parent(UNCLE, COUSIN);
        partner(FATHER, MOTHER); partner(FATHER, STEPMOTHER);
        parent(FATHER, MAN); parent(MOTHER, MAN);
        parent(FATHER, SISTER); parent(MOTHER, SISTER);
        parent(FATHER, HALF_SISTER); parent(STEPMOTHER, HALF_SISTER);
        parent(FATHER, BROTHER); parent(MOTHER, BROTHER); partner(BROTHER, BROTHERS_WIFE);
        parent(SISTER, NIECE); parent(NIECE, GRAND_NIECE);
        partner(MAN, WIFE); parent(MOTHER_IN_LAW, WIFE);
        partner(WIFE_EX_HUSBAND, WIFE); parent(WIFE_EX_HUSBAND, STEPDAUGHTER); parent(WIFE, STEPDAUGHTER);
        parent(MOTHER_IN_LAW, SISTER_IN_LAW);
        parent(MAN, DAUGHTER); parent(WIFE, DAUGHTER);
        parent(MAN, SON); partner(SON, DAUGHTER_IN_LAW); parent(SON, GRANDDAUGHTER);
        adoptive(MAN, ADOPTED_DAUGHTER);
        person(STRANGER);

        assertThat(new MahramRules(links).mahramsOfMan(MAN)).contains(
                FATHER, MOTHER, GRANDFATHER, GRANDMOTHER, SISTER, HALF_SISTER, STEPMOTHER, AUNT, UNCLE,
                NIECE, GRAND_NIECE, WIFE, MOTHER_IN_LAW, STEPDAUGHTER, DAUGHTER, SON, DAUGHTER_IN_LAW,
                GRANDDAUGHTER, BROTHER
        ).doesNotContain(MAN, COUSIN, SISTER_IN_LAW, ADOPTED_DAUGHTER, STRANGER, WIFE_EX_HUSBAND, BROTHERS_WIFE);
    }

    @Test
    void restrictsWomenFromNineOrWithUnknownAgeUnlessSetOtherwise() {
        LocalDate today = LocalDate.of(2026, 11, 2);
        assertThat(MahramRules.isRestricted(woman(LocalDate.of(2017, 11, 2), null), today)).isTrue();
        assertThat(MahramRules.isRestricted(woman(LocalDate.of(2017, 11, 3), null), today)).isFalse();
        assertThat(MahramRules.isRestricted(woman(null, null), today)).isTrue();
        assertThat(MahramRules.isRestricted(woman(LocalDate.of(1980, 1, 1), false), today)).isFalse();
        assertThat(MahramRules.isRestricted(woman(LocalDate.of(2020, 1, 1), true), today)).isTrue();
        Person man = woman(LocalDate.of(1980, 1, 1), true);
        man.setGender(Gender.MALE);
        assertThat(MahramRules.isRestricted(man, today)).isFalse();
    }

    private Person woman(LocalDate birthDate, Boolean mahramOnly) {
        Person person = new Person();
        person.setGender(Gender.FEMALE);
        person.setBirthDate(birthDate);
        person.setMahramOnly(mahramOnly);
        return person;
    }

    private Person person(long id) {
        return people.computeIfAbsent(id, key -> {
            Person person = new Person();
            person.setId(key);
            return person;
        });
    }

    private void parent(long parent, long child) { link(parent, child, RelationshipType.PARENT); }
    private void adoptive(long parent, long child) { link(parent, child, RelationshipType.ADOPTIVE_PARENT); }
    private void partner(long a, long b) { link(a, b, RelationshipType.PARTNER); }

    private void link(long from, long to, RelationshipType type) {
        Relationship relationship = new Relationship();
        relationship.setFromPerson(person(from));
        relationship.setToPerson(person(to));
        relationship.setType(type);
        links.add(relationship);
    }
}
