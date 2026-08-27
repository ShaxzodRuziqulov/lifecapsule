package com.example.lifecapsule.config;

import com.example.lifecapsule.entity.Family;
import com.example.lifecapsule.entity.FamilyAccess;
import com.example.lifecapsule.entity.Person;
import com.example.lifecapsule.entity.Relationship;
import com.example.lifecapsule.entity.Users;
import com.example.lifecapsule.entity.enumirated.AccessStatus;
import com.example.lifecapsule.entity.enumirated.FamilyAccessRole;
import com.example.lifecapsule.entity.enumirated.FamilyVisibility;
import com.example.lifecapsule.entity.enumirated.Gender;
import com.example.lifecapsule.entity.enumirated.RelationshipType;
import com.example.lifecapsule.entity.enumirated.Role;
import com.example.lifecapsule.entity.enumirated.Status;
import com.example.lifecapsule.repository.FamilyAccessRepository;
import com.example.lifecapsule.repository.FamilyRepository;
import com.example.lifecapsule.repository.PersonRepository;
import com.example.lifecapsule.repository.RelationshipRepository;
import com.example.lifecapsule.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {
    public static final String DEMO_EMAIL = "demo@lifecapsule.uz";
    public static final String DEMO_EDITOR_EMAIL = "editor@lifecapsule.uz";
    public static final String DEMO_VIEWER_EMAIL = "viewer@lifecapsule.uz";
    public static final String DEMO_OUTSIDER_EMAIL = "outsider@lifecapsule.uz";
    public static final String DEMO_PASSWORD = "Demo123!";
    private static final String DEMO_FAMILY_NAME = "Roziqulovlar sulolasi";

    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final FamilyAccessRepository familyAccessRepository;
    private final PersonRepository personRepository;
    private final RelationshipRepository relationshipRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public DemoDataInitializer(
            UserRepository userRepository,
            FamilyRepository familyRepository,
            FamilyAccessRepository familyAccessRepository,
            PersonRepository personRepository,
            RelationshipRepository relationshipRepository,
            BCryptPasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
        this.familyAccessRepository = familyAccessRepository;
        this.personRepository = personRepository;
        this.relationshipRepository = relationshipRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Users owner = getOrCreateDemoUser(DEMO_EMAIL, "lifecapsule_demo", "Shaxzod", "Roziqulov", null);
        Users editor = getOrCreateDemoUser(DEMO_EDITOR_EMAIL, "lifecapsule_editor", "Malika", "Roziqulova", null);
        Users viewer = getOrCreateDemoUser(DEMO_VIEWER_EMAIL, "lifecapsule_viewer", "Kamol", "Roziqulov", null);
        getOrCreateDemoUser(DEMO_OUTSIDER_EMAIL, "lifecapsule_outsider", "Aziz", "Mehmon", null);

        Family family = getOrCreateFamily(
                owner,
                DEMO_FAMILY_NAME,
                "1930-yillardan hozirgacha davom etgan katta oilaviy shajara."
        );
        saveAccess(owner, family, FamilyAccessRole.OWNER);
        saveAccess(editor, family, FamilyAccessRole.EDITOR);
        saveAccess(viewer, family, FamilyAccessRole.VIEWER);
        seedLargeDynasty(family, owner, editor, viewer);
    }

    private void seedLargeDynasty(Family family, Users owner, Users editor, Users viewer) {
        List<Person> people = new ArrayList<>(personRepository.findAllByFamilyIdOrderByFirstNameAsc(family.getId()));

        Person rustam = findOrCreatePerson(people, family, new PersonSeed(
                "Rustam", "Roziqulov", Gender.MALE, "1930-04-12", null,
                "Samarqand", "Ustoz", "Sulolaning bosh avlod vakili, oila tarixini birlashtirgan inson.", null
        ));
        Person munavvar = findOrCreatePerson(people, family, new PersonSeed(
                "Munavvar", "Roziqulova", Gender.FEMALE, "1934-08-20", null,
                "Samarqand", "Shifokor", "Katta oila tarbiyasi va mehr-oqibatini saqlagan ona.", null
        ));
        saveRelationshipIfMissing(family, rustam, munavvar, RelationshipType.PARTNER, "Bosh avlod nikohi");

        for (BranchSeed branch : branches(owner, editor, viewer)) {
            Person child = findOrCreatePerson(people, family, branch.child());
            Person spouse = findOrCreatePerson(people, family, branch.spouse());
            saveRelationshipIfMissing(family, rustam, child, RelationshipType.PARENT, "Otasi");
            saveRelationshipIfMissing(family, munavvar, child, RelationshipType.PARENT, "Onasi");
            saveRelationshipIfMissing(family, child, spouse, RelationshipType.PARTNER, "Nikoh orqali bog'langan oila");

            for (PersonSeed grandchildSeed : branch.children()) {
                Person grandchild = findOrCreatePerson(people, family, grandchildSeed);
                saveRelationshipIfMissing(family, child, grandchild, RelationshipType.PARENT, "Ota-ona farzand bog'lanishi");
                saveRelationshipIfMissing(family, spouse, grandchild, RelationshipType.PARENT, "Ota-ona farzand bog'lanishi");
            }
        }

        seedCurrentGeneration(people, family);
    }

    private List<BranchSeed> branches(Users owner, Users editor, Users viewer) {
        return List.of(
                branch(child("Bahrom", Gender.MALE, "1951-02-17", "Quruvchi"), spouse("Nargiza", "Saidova", Gender.FEMALE, "1954-06-03"), List.of(
                        grand("Dilshod", "Roziqulov", Gender.MALE, "1976-03-11", "Muhandis"),
                        grand("Gavhar", "Roziqulova", Gender.FEMALE, "1979-09-24", "O'qituvchi"),
                        grand("Javlon", "Roziqulov", Gender.MALE, "1982-12-02", "Tadbirkor"),
                        grand("Shoira", "Roziqulova", Gender.FEMALE, "1986-05-18", "Shifokor")
                )),
                branch(child("Dilorom", Gender.FEMALE, "1953-07-09", "Kutubxonachi"), spouse("Akmal", "Tursunov", Gender.MALE, "1950-01-22"), List.of(
                        grand("Zilola", "Tursunova", Gender.FEMALE, "1977-04-14", "Dizayner"),
                        grand("Bekzod", "Tursunov", Gender.MALE, "1980-08-30", "Bank xodimi"),
                        grand("Madina", "Tursunova", Gender.FEMALE, "1983-11-19", "Hamshira"),
                        grand("Azamat", "Tursunov", Gender.MALE, "1987-02-06", "Dasturchi"),
                        grand("Ruxshona", "Tursunova", Gender.FEMALE, "1991-10-12", "Talaba")
                )),
                branch(child("Anvar", Gender.MALE, "1955-05-26", "Muhandis"), spouse("Dilbar", "Karimova", Gender.FEMALE, "1958-10-07"), List.of(
                        grand("Shaxzod", "Roziqulov", Gender.MALE, "1981-04-14", "Loyiha rahbari", owner),
                        grand("Malika", "Roziqulova", Gender.FEMALE, "1984-08-09", "Dizayner", editor),
                        grand("Kamol", "Roziqulov", Gender.MALE, "1987-01-21", "Tadbirkor", viewer),
                        grand("Sevara", "Roziqulova", Gender.FEMALE, "1990-12-05", "Shifokor"),
                        grand("Jasur", "Roziqulov", Gender.MALE, "1993-07-17", "Dasturchi"),
                        grand("Laylo", "Roziqulova", Gender.FEMALE, "1996-03-29", "O'qituvchi"),
                        grand("Sardor", "Roziqulov", Gender.MALE, "1999-09-01", "Talaba")
                )),
                branch(child("Saida", Gender.FEMALE, "1957-12-03", "Tikuvchi"), spouse("Odil", "Yusupov", Gender.MALE, "1954-03-16"), List.of(
                        grand("Umida", "Yusupova", Gender.FEMALE, "1981-06-08", "Moliyachi"),
                        grand("Farrux", "Yusupov", Gender.MALE, "1985-01-27", "Usta"),
                        grand("Mohira", "Yusupova", Gender.FEMALE, "1989-04-22", "O'qituvchi")
                )),
                branch(child("Komil", Gender.MALE, "1959-09-15", "Agronom"), spouse("Shirin", "Aliyeva", Gender.FEMALE, "1962-02-10"), List.of(
                        grand("Otabek", "Roziqulov", Gender.MALE, "1983-05-15", "Arxitektor"),
                        grand("Nilufar", "Roziqulova", Gender.FEMALE, "1986-11-13", "Farmatsevt"),
                        grand("Islom", "Roziqulov", Gender.MALE, "1989-07-07", "Haydovchi"),
                        grand("Durdona", "Roziqulova", Gender.FEMALE, "1992-02-28", "Buxgalter"),
                        grand("Abror", "Roziqulov", Gender.MALE, "1995-06-21", "Dasturchi"),
                        grand("Madinabonu", "Roziqulova", Gender.FEMALE, "1998-10-03", "Talaba")
                )),
                branch(child("Lola", Gender.FEMALE, "1961-04-28", "O'qituvchi"), spouse("Farhod", "Ergashev", Gender.MALE, "1958-08-18"), List.of(
                        grand("Sanjar", "Ergashev", Gender.MALE, "1986-09-02", "Tadbirkor"),
                        grand("Gulbahor", "Ergasheva", Gender.FEMALE, "1990-05-25", "Shifokor")
                )),
                branch(child("Ravshan", Gender.MALE, "1963-01-06", "Elektrik"), spouse("Gulnoza", "Hakimova", Gender.FEMALE, "1965-12-11"), List.of(
                        grand("Sherzod", "Roziqulov", Gender.MALE, "1988-03-03", "Muhandis"),
                        grand("Maftuna", "Roziqulova", Gender.FEMALE, "1991-07-16", "Dizayner"),
                        grand("Iroda", "Roziqulova", Gender.FEMALE, "1994-04-09", "O'qituvchi"),
                        grand("Temur", "Roziqulov", Gender.MALE, "1997-11-30", "Dasturchi")
                )),
                branch(child("Matluba", Gender.FEMALE, "1965-06-20", "Hamshira"), spouse("Zafar", "Qodirov", Gender.MALE, "1962-09-27"), List.of(
                        grand("Jahongir", "Qodirov", Gender.MALE, "1989-12-18", "Bank xodimi"),
                        grand("Nodira", "Qodirova", Gender.FEMALE, "1992-08-04", "O'qituvchi"),
                        grand("Asadbek", "Qodirov", Gender.MALE, "1995-01-26", "Dasturchi"),
                        grand("Fotima", "Qodirova", Gender.FEMALE, "1998-06-12", "Talaba"),
                        grand("Sabrina", "Qodirova", Gender.FEMALE, "2002-03-08", "Talaba")
                )),
                branch(child("Shuhrat", Gender.MALE, "1967-11-02", "Shifokor"), spouse("Barno", "Usmonova", Gender.FEMALE, "1969-05-05"), List.of(
                        grand("Aziz", "Roziqulov", Gender.MALE, "1990-02-14", "Advokat"),
                        grand("Rayhona", "Roziqulova", Gender.FEMALE, "1994-09-19", "Dizayner"),
                        grand("Murod", "Roziqulov", Gender.MALE, "1998-12-23", "Talaba")
                )),
                branch(child("Nasiba", Gender.FEMALE, "1969-03-13", "Buxgalter"), spouse("Jamshid", "Mirzayev", Gender.MALE, "1966-07-24"), List.of(
                        grand("Munisa", "Mirzayeva", Gender.FEMALE, "1992-05-01", "Shifokor"),
                        grand("Bobur", "Mirzayev", Gender.MALE, "1995-10-20", "Muhandis"),
                        grand("Shahnoza", "Mirzayeva", Gender.FEMALE, "1999-02-11", "Talaba"),
                        grand("Ibrohim", "Mirzayev", Gender.MALE, "2003-08-29", "O'quvchi")
                )),
                branch(child("Olim", Gender.MALE, "1971-08-01", "Tadbirkor"), spouse("Sevara", "Abduvaliyeva", Gender.FEMALE, "1974-04-12"), List.of(
                        grand("Alisher", "Roziqulov", Gender.MALE, "1997-03-21", "Dasturchi"),
                        grand("Zarina", "Roziqulova", Gender.FEMALE, "2001-11-02", "Talaba")
                )),
                branch(child("Feruza", Gender.FEMALE, "1973-02-25", "O'qituvchi"), spouse("Sardor", "Kenjayev", Gender.MALE, "1970-06-14"), List.of(
                        grand("Diyor", "Kenjayev", Gender.MALE, "1998-04-04", "Muhandis"),
                        grand("Shahlo", "Kenjayeva", Gender.FEMALE, "2000-09-16", "Talaba"),
                        grand("Laziz", "Kenjayev", Gender.MALE, "2003-01-30", "O'quvchi"),
                        grand("Mubina", "Kenjayeva", Gender.FEMALE, "2006-07-27", "O'quvchi"),
                        grand("Yusuf", "Kenjayev", Gender.MALE, "2009-12-09", "O'quvchi")
                )),
                branch(child("Ulugbek", Gender.MALE, "1975-10-19", "Arxitektor"), spouse("Maftuna", "Sobirova", Gender.FEMALE, "1978-02-03"), List.of(
                        grand("Sarvar", "Roziqulov", Gender.MALE, "2000-06-06", "Talaba"),
                        grand("Madina", "Roziqulova", Gender.FEMALE, "2004-03-18", "Talaba"),
                        grand("Oydin", "Roziqulova", Gender.FEMALE, "2008-10-25", "O'quvchi")
                )),
                branch(child("Mavluda", Gender.FEMALE, "1977-05-08", "Shifokor"), spouse("Bekzod", "Rahimov", Gender.MALE, "1980-08-23"), List.of(
                        grand("Soliha", "Rahimova", Gender.FEMALE, "2004-05-12", "Talaba"),
                        grand("Abdulaziz", "Rahimov", Gender.MALE, "2008-02-17", "O'quvchi")
                )),
                branch(child("Komron", Gender.MALE, "1979-12-27", "Dasturchi"), spouse("Mohira", "Ismoilova", Gender.FEMALE, "1982-01-30"), List.of(
                        grand("Javohir", "Roziqulov", Gender.MALE, "2005-09-05", "Talaba"),
                        grand("Muslima", "Roziqulova", Gender.FEMALE, "2009-04-03", "O'quvchi"),
                        grand("Imron", "Roziqulov", Gender.MALE, "2012-07-15", "O'quvchi"),
                        grand("Maryam", "Roziqulova", Gender.FEMALE, "2016-11-20", "Bog'cha tarbiyalanuvchisi")
                ))
        );
    }

    private void seedCurrentGeneration(List<Person> people, Family family) {
        seedMarriageWithChildren(people, family,
                findPerson(people, "Shaxzod", "Roziqulov"),
                spouseSeed("Aziza", "Karimova", Gender.FEMALE, "1984-10-02", "Dizayner"),
                List.of(
                        childSeed("Anora", "Roziqulova", Gender.FEMALE, "2010-03-12", "O'quvchi"),
                        childSeed("Behruz", "Roziqulov", Gender.MALE, "2014-09-18", "O'quvchi"),
                        childSeed("Muhammadali", "Roziqulov", Gender.MALE, "2020-05-06", "Bog'cha tarbiyalanuvchisi")
                ));
        seedMarriageWithChildren(people, family,
                findPerson(people, "Malika", "Roziqulova"),
                spouseSeed("Otabek", "Aliyev", Gender.MALE, "1982-05-15", "Arxitektor"),
                List.of(
                        childSeed("Layli", "Aliyeva", Gender.FEMALE, "2012-11-30", "O'quvchi"),
                        childSeed("Jasur", "Aliyev", Gender.MALE, "2017-08-05", "O'quvchi")
                ));
        seedMarriageWithChildren(people, family,
                findPerson(people, "Kamol", "Roziqulov"),
                spouseSeed("Dildora", "Nazarova", Gender.FEMALE, "1989-06-26", "Shifokor"),
                List.of(
                        childSeed("Yasmina", "Roziqulova", Gender.FEMALE, "2015-01-08", "O'quvchi"),
                        childSeed("Amir", "Roziqulov", Gender.MALE, "2019-04-22", "Bog'cha tarbiyalanuvchisi")
                ));
        seedMarriageWithChildren(people, family,
                findPerson(people, "Jasur", "Roziqulov"),
                spouseSeed("Sitora", "Umarova", Gender.FEMALE, "1995-12-12", "Dasturchi"),
                List.of(childSeed("Madinaxon", "Roziqulova", Gender.FEMALE, "2023-02-18", "Chaqaloq")));
        seedMarriageWithChildren(people, family,
                findPerson(people, "Alisher", "Roziqulov"),
                spouseSeed("Nozima", "Sultonova", Gender.FEMALE, "1999-07-14", "Dizayner"),
                List.of(childSeed("Said", "Roziqulov", Gender.MALE, "2024-01-10", "Chaqaloq")));
    }

    private void seedMarriageWithChildren(
            List<Person> people,
            Family family,
            Person person,
            PersonSeed spouseSeed,
            List<PersonSeed> childSeeds
    ) {
        if (person == null) {
            return;
        }

        Person spouse = findOrCreatePerson(people, family, spouseSeed);
        saveRelationshipIfMissing(family, person, spouse, RelationshipType.PARTNER, "Nikoh orqali bog'langan oila");

        for (PersonSeed childSeed : childSeeds) {
            Person child = findOrCreatePerson(people, family, childSeed);
            saveRelationshipIfMissing(family, person, child, RelationshipType.PARENT, "Ota-ona farzand bog'lanishi");
            saveRelationshipIfMissing(family, spouse, child, RelationshipType.PARENT, "Ota-ona farzand bog'lanishi");
        }
    }

    private BranchSeed branch(PersonSeed child, PersonSeed spouse, List<PersonSeed> children) {
        return new BranchSeed(child, spouse, children);
    }

    private PersonSeed child(String firstName, Gender gender, String birthDate, String occupation) {
        String lastName = gender == Gender.FEMALE ? "Roziqulova" : "Roziqulov";
        return new PersonSeed(firstName, lastName, gender, birthDate, null, "Samarqand", occupation,
                "Rustam va Munavvar Roziqulovlar oilasining farzandi.", null);
    }

    private PersonSeed spouse(String firstName, String lastName, Gender gender, String birthDate) {
        return spouseSeed(firstName, lastName, gender, birthDate, gender == Gender.FEMALE ? "Uy bekasi" : "Mutaxassis");
    }

    private PersonSeed spouseSeed(String firstName, String lastName, Gender gender, String birthDate, String occupation) {
        return new PersonSeed(firstName, lastName, gender, birthDate, null, "Toshkent", occupation,
                "Nikoh orqali Roziqulovlar sulolasiga bog'langan.", null);
    }

    private PersonSeed grand(String firstName, String lastName, Gender gender, String birthDate, String occupation) {
        return grand(firstName, lastName, gender, birthDate, occupation, null);
    }

    private PersonSeed grand(
            String firstName,
            String lastName,
            Gender gender,
            String birthDate,
            String occupation,
            Users linkedUser
    ) {
        return new PersonSeed(firstName, lastName, gender, birthDate, null, "Toshkent", occupation,
                "Sulolaning keyingi avlod vakili.", linkedUser);
    }

    private PersonSeed childSeed(String firstName, String lastName, Gender gender, String birthDate, String occupation) {
        return new PersonSeed(firstName, lastName, gender, birthDate, null, "Toshkent", occupation,
                "Hozirgi avlod vakili.", null);
    }

    private Users getOrCreateDemoUser(
            String email,
            String username,
            String firstName,
            String lastName,
            String middleName
    ) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> createDemoUser(email, username, firstName, lastName, middleName));
    }

    private Users createDemoUser(
            String email,
            String username,
            String firstName,
            String lastName,
            String middleName
    ) {
        Users user = new Users();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setMiddleName(middleName);
        user.setEmail(email);
        user.setUserName(username);
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRole(Role.USER);
        user.setStatus(Status.ACTIVE);
        return userRepository.save(user);
    }

    private Family getOrCreateFamily(Users owner, String name, String description) {
        return familyRepository.findAllByCreatedByIdOrderByCreatedAtDesc(owner.getId())
                .stream()
                .filter(family -> name.equals(family.getName()))
                .findFirst()
                .orElseGet(() -> createFamily(owner, name, description));
    }

    private Family createFamily(Users owner, String name, String description) {
        Family family = new Family();
        family.setName(name);
        family.setDescription(description);
        family.setCreatedBy(owner);
        family.setVisibility(FamilyVisibility.INVITE_ONLY);
        return familyRepository.save(family);
    }

    private void saveAccess(Users user, Family family, FamilyAccessRole role) {
        FamilyAccess access = familyAccessRepository.findByFamilyIdAndUserId(family.getId(), user.getId())
                .orElseGet(FamilyAccess::new);
        access.setFamily(family);
        access.setUser(user);
        access.setAccessRole(role);
        access.setStatus(AccessStatus.ACTIVE);
        familyAccessRepository.save(access);
    }

    private Person findOrCreatePerson(List<Person> people, Family family, PersonSeed seed) {
        Person existingPerson = findPerson(people, seed.firstName(), seed.lastName());
        if (existingPerson != null) {
            if (seed.linkedUser() != null
                    && (existingPerson.getLinkedUser() == null
                    || !existingPerson.getLinkedUser().getId().equals(seed.linkedUser().getId()))) {
                existingPerson.setLinkedUser(seed.linkedUser());
                personRepository.save(existingPerson);
            }
            return existingPerson;
        }

        Person savedPerson = personRepository.save(person(family, seed));
        people.add(savedPerson);
        return savedPerson;
    }

    private Person findPerson(List<Person> people, String firstName, String lastName) {
        return people.stream()
                .filter(person -> firstName.equals(person.getFirstName()))
                .filter(person -> lastName.equals(person.getLastName()))
                .findFirst()
                .orElse(null);
    }

    private void saveRelationshipIfMissing(
            Family family,
            Person from,
            Person to,
            RelationshipType type,
            String note
    ) {
        if (from == null || to == null) {
            return;
        }

        boolean exists = type == RelationshipType.PARTNER
                ? relationshipRepository.existsByFamilyIdAndFromPersonIdAndToPersonIdAndTypeOrFamilyIdAndFromPersonIdAndToPersonIdAndType(
                family.getId(), from.getId(), to.getId(), type,
                family.getId(), to.getId(), from.getId(), type
        )
                : relationshipRepository.existsByFamilyIdAndFromPersonIdAndToPersonIdAndType(
                family.getId(), from.getId(), to.getId(), type
        );

        if (!exists) {
            relationshipRepository.save(relationship(family, from, to, type, note));
        }
    }

    private Person person(Family family, PersonSeed seed) {
        Person person = new Person();
        person.setFamily(family);
        person.setFirstName(seed.firstName());
        person.setLastName(seed.lastName());
        person.setGender(seed.gender());
        person.setBirthDate(LocalDate.parse(seed.birthDate()));
        person.setDeathDate(seed.deathDate() == null ? null : LocalDate.parse(seed.deathDate()));
        person.setBirthPlace(seed.birthPlace());
        person.setOccupation(seed.occupation());
        person.setBiography(seed.biography());
        person.setLinkedUser(seed.linkedUser());
        return person;
    }

    private Relationship relationship(
            Family family,
            Person from,
            Person to,
            RelationshipType type,
            String note
    ) {
        Relationship relationship = new Relationship();
        relationship.setFamily(family);
        relationship.setFromPerson(from);
        relationship.setToPerson(to);
        relationship.setType(type);
        relationship.setNote(note);
        return relationship;
    }

    private record PersonSeed(
            String firstName,
            String lastName,
            Gender gender,
            String birthDate,
            String deathDate,
            String birthPlace,
            String occupation,
            String biography,
            Users linkedUser
    ) {
    }

    private record BranchSeed(PersonSeed child, PersonSeed spouse, List<PersonSeed> children) {
    }
}
