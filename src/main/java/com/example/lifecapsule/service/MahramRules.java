package com.example.lifecapsule.service;

import com.example.lifecapsule.entity.Person;
import com.example.lifecapsule.entity.Relationship;
import com.example.lifecapsule.entity.enumirated.Gender;
import com.example.lifecapsule.entity.enumirated.RelationshipType;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Who counts as a mahram, worked out from the family's PARENT and PARTNER links only -
 * adoption does not create mahramiyat, and milk kinship is not recorded. Every partner link
 * is treated as a (current or past) marriage until divorces are tracked.
 */
final class MahramRules {
    /** Girls from this age have their details shown to mahrams only, unless set otherwise. */
    static final int RESTRICTED_FROM_AGE = 9;

    private final Map<Long, Set<Long>> parents = new HashMap<>();
    private final Map<Long, Set<Long>> children = new HashMap<>();
    private final Map<Long, Set<Long>> partners = new HashMap<>();

    MahramRules(Collection<Relationship> relationships) {
        for (Relationship relationship : relationships) {
            Long from = relationship.getFromPerson().getId();
            Long to = relationship.getToPerson().getId();
            if (relationship.getType() == RelationshipType.PARENT) {
                link(parents, to, from);
                link(children, from, to);
            } else if (relationship.getType() == RelationshipType.PARTNER) {
                link(partners, from, to);
                link(partners, to, from);
            }
        }
    }

    /** Whether this woman's details are for mahrams only: her own choice, or by default from age 9 (or unknown age). */
    static boolean isRestricted(Person person, LocalDate today) {
        if (person.getGender() != Gender.FEMALE) return false;
        if (person.getMahramOnly() != null) return person.getMahramOnly();
        return person.getBirthDate() == null || !person.getBirthDate().plusYears(RESTRICTED_FROM_AGE).isAfter(today);
    }

    /**
     * Everyone a man may see unveiled, i.e. the women he can never marry:
     * mother and grandmothers, daughters and granddaughters, sisters (also half-sisters), aunts at any
     * height, nieces at any depth, his wives, their mothers and grandmothers (mother-in-law), their
     * daughters (stepdaughters), the wives of his sons and grandsons (daughter-in-law) and the wives of
     * his father and grandfathers (stepmother). The result may contain men too; callers filter by gender.
     */
    Set<Long> mahramsOfMan(Long manId) {
        Set<Long> ancestors = walk(manId, parents);
        Set<Long> descendants = walk(manId, children);
        Set<Long> siblings = siblingsOf(manId);
        Set<Long> wives = partners.getOrDefault(manId, Set.of());

        Set<Long> result = new HashSet<>();
        result.addAll(ancestors);
        result.addAll(descendants);
        result.addAll(siblings);
        ancestors.forEach(ancestor -> result.addAll(siblingsOf(ancestor)));
        siblings.forEach(sibling -> result.addAll(walk(sibling, children)));
        result.addAll(wives);
        wives.forEach(wife -> {
            result.addAll(walk(wife, parents));
            result.addAll(walk(wife, children));
        });
        descendants.forEach(descendant -> result.addAll(partners.getOrDefault(descendant, Set.of())));
        ancestors.forEach(ancestor -> result.addAll(partners.getOrDefault(ancestor, Set.of())));
        result.remove(manId);
        return result;
    }

    private Set<Long> siblingsOf(Long personId) {
        Set<Long> siblings = new HashSet<>();
        parents.getOrDefault(personId, Set.of()).forEach(parent -> siblings.addAll(children.getOrDefault(parent, Set.of())));
        siblings.remove(personId);
        return siblings;
    }

    /** Everyone reachable by repeatedly following the given direction (ancestors or descendants). */
    private static Set<Long> walk(Long start, Map<Long, Set<Long>> direction) {
        Set<Long> seen = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>(direction.getOrDefault(start, Set.of()));
        while (!queue.isEmpty()) {
            Long next = queue.poll();
            if (!next.equals(start) && seen.add(next)) queue.addAll(direction.getOrDefault(next, Set.of()));
        }
        return seen;
    }

    private static void link(Map<Long, Set<Long>> map, Long key, Long value) {
        map.computeIfAbsent(key, ignored -> new HashSet<>()).add(value);
    }
}
