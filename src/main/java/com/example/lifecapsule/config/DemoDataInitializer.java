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
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {
    public static final String DEMO_EMAIL = "demo@lifecapsule.uz";
    public static final String DEMO_EDITOR_EMAIL = "editor@lifecapsule.uz";
    public static final String DEMO_VIEWER_EMAIL = "viewer@lifecapsule.uz";
    public static final String DEMO_PASSWORD = "Demo123!";
    private static final String DEMO_FAMILY_NAME = "Roziqulovlar oilasi";
    private static final String DEMO_SECOND_FAMILY_NAME = "Karimovlar oilasi";
    private static final String DEMO_THIRD_FAMILY_NAME = "Aliyevlar oilasi";

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
        Users owner = getOrCreateDemoUser(
                DEMO_EMAIL,
                "lifecapsule_demo",
                "Shaxzod",
                "Roziqulov",
                "Anvar ogli"
        );
        Users editor = getOrCreateDemoUser(
                DEMO_EDITOR_EMAIL,
                "lifecapsule_editor",
                "Malika",
                "Roziqulova",
                null
        );
        Users viewer = getOrCreateDemoUser(
                DEMO_VIEWER_EMAIL,
                "lifecapsule_viewer",
                "Kamol",
                "Roziqulov",
                null
        );
        Family roziqulovFamily = getOrCreateFamily(
                owner,
                DEMO_FAMILY_NAME,
                "Uch avlod tarixi, shajara va oilaviy xotiralar."
        );
        saveAccess(owner, roziqulovFamily, FamilyAccessRole.OWNER);
        saveAccess(editor, roziqulovFamily, FamilyAccessRole.EDITOR);
        saveAccess(viewer, roziqulovFamily, FamilyAccessRole.VIEWER);
        seedRoziqulovFamily(roziqulovFamily);

        Family karimovFamily = getOrCreateFamily(
                owner,
                DEMO_SECOND_FAMILY_NAME,
                "Farg'ona vodiysidan boshlangan oila tarixi va avlodlar bog'lanishi."
        );
        saveAccess(owner, karimovFamily, FamilyAccessRole.OWNER);
        saveAccess(viewer, karimovFamily, FamilyAccessRole.VIEWER);
        seedKarimovFamily(karimovFamily);

        Family aliyevFamily = getOrCreateFamily(
                owner,
                DEMO_THIRD_FAMILY_NAME,
                "Buxoro va Toshkentga bog'langan oilaviy shajara namunasi."
        );
        saveAccess(owner, aliyevFamily, FamilyAccessRole.OWNER);
        saveAccess(viewer, aliyevFamily, FamilyAccessRole.VIEWER);
        seedAliyevFamily(aliyevFamily);
    }

    private void seedRoziqulovFamily(Family family) {
        if (hasPeople(family)) {
            return;
        }

        Person bobur = person(family, "Bobur", "Roziqulov", Gender.MALE, "1948-03-12", null,
                "Samarqand", "Ustoz", "Oiladagi katta avlod vakili.");
        Person saodat = person(family, "Saodat", "Roziqulova", Gender.FEMALE, "1952-09-04", null,
                "Samarqand", "Shifokor", "Mehribon ona va buvi.");
        Person anvar = person(family, "Anvar", "Roziqulov", Gender.MALE, "1975-06-18", null,
                "Toshkent", "Muhandis", "Texnika va sayohatga qiziqadi.");
        Person dilbar = person(family, "Dilbar", "Roziqulova", Gender.FEMALE, "1978-11-25", null,
                "Buxoro", "Oqituvchi", "Oila tarixini yigishni yaxshi koradi.");
        Person shaxzod = person(family, "Shaxzod", "Roziqulov", Gender.MALE, "2000-04-14", null,
                "Toshkent", "Dasturchi", "LifeCapsule oilaviy arxivini boshqaradi.");
        Person malika = person(family, "Malika", "Roziqulova", Gender.FEMALE, "2003-08-09", null,
                "Toshkent", "Dizayner", "Rasmlar va oilaviy xotiralarni saqlaydi.");
        Person kamol = person(family, "Kamol", "Roziqulov", Gender.MALE, "2007-01-21", null,
                "Toshkent", "Talaba", "Sport va musiqa bilan qiziqadi.");

        personRepository.saveAll(List.of(bobur, saodat, anvar, dilbar, shaxzod, malika, kamol));

        relationshipRepository.saveAll(List.of(
                relationship(family, bobur, saodat, RelationshipType.PARTNER, "Turmush ortoqlar"),
                relationship(family, bobur, anvar, RelationshipType.PARENT, "Ota va ogil"),
                relationship(family, saodat, anvar, RelationshipType.PARENT, "Ona va ogil"),
                relationship(family, anvar, dilbar, RelationshipType.PARTNER, "Turmush ortoqlar"),
                relationship(family, anvar, shaxzod, RelationshipType.PARENT, "Ota va ogil"),
                relationship(family, dilbar, shaxzod, RelationshipType.PARENT, "Ona va ogil"),
                relationship(family, anvar, malika, RelationshipType.PARENT, "Ota va qiz"),
                relationship(family, dilbar, malika, RelationshipType.PARENT, "Ona va qiz"),
                relationship(family, anvar, kamol, RelationshipType.PARENT, "Ota va ogil"),
                relationship(family, dilbar, kamol, RelationshipType.PARENT, "Ona va ogil")
        ));
    }

    private void seedKarimovFamily(Family family) {
        if (hasPeople(family)) {
            return;
        }

        Person rahim = person(family, "Rahim", "Karimov", Gender.MALE, "1942-05-03", null,
                "Qo'qon", "Dehqon", "Yer va bog'dorchilik ishlarini yaxshi bilgan.");
        Person gulnora = person(family, "Gulnora", "Karimova", Gender.FEMALE, "1947-12-19", null,
                "Farg'ona", "Tikuvchi", "Oilaviy an'analarni saqlagan mehribon buvi.");
        Person jamshid = person(family, "Jamshid", "Karimov", Gender.MALE, "1972-02-08", null,
                "Farg'ona", "Tadbirkor", "Oila ishlarini birlashtirib yuradi.");
        Person madina = person(family, "Madina", "Karimova", Gender.FEMALE, "1976-07-14", null,
                "Andijon", "Hamshira", "Sog'liq va g'amxo'rlik ishlariga mas'ul.");
        Person aziza = person(family, "Aziza", "Karimova", Gender.FEMALE, "1999-10-02", null,
                "Toshkent", "Talaba", "Oila tarixini raqamlashtirishga qiziqadi.");
        Person sardor = person(family, "Sardor", "Karimov", Gender.MALE, "2005-04-28", null,
                "Farg'ona", "O'quvchi", "Futbol va texnologiyaga qiziqadi.");

        personRepository.saveAll(List.of(rahim, gulnora, jamshid, madina, aziza, sardor));

        relationshipRepository.saveAll(List.of(
                relationship(family, rahim, gulnora, RelationshipType.PARTNER, "Turmush ortoqlar"),
                relationship(family, rahim, jamshid, RelationshipType.PARENT, "Ota va ogil"),
                relationship(family, gulnora, jamshid, RelationshipType.PARENT, "Ona va ogil"),
                relationship(family, jamshid, madina, RelationshipType.PARTNER, "Turmush ortoqlar"),
                relationship(family, jamshid, aziza, RelationshipType.PARENT, "Ota va qiz"),
                relationship(family, madina, aziza, RelationshipType.PARENT, "Ona va qiz"),
                relationship(family, jamshid, sardor, RelationshipType.PARENT, "Ota va ogil"),
                relationship(family, madina, sardor, RelationshipType.PARENT, "Ona va ogil")
        ));
    }

    private void seedAliyevFamily(Family family) {
        if (hasPeople(family)) {
            return;
        }

        Person nasriddin = person(family, "Nasriddin", "Aliyev", Gender.MALE, "1950-01-26", null,
                "Buxoro", "Usta", "Hunarmandchilik va oilaviy kasbni davom ettirgan.");
        Person zarina = person(family, "Zarina", "Aliyeva", Gender.FEMALE, "1954-06-11", null,
                "Buxoro", "Kutubxonachi", "Kitob va hikoyalarni yaxshi ko'radi.");
        Person farhod = person(family, "Farhod", "Aliyev", Gender.MALE, "1980-09-17", null,
                "Buxoro", "Arxitektor", "Tarixiy binolar va shaharsozlikka qiziqadi.");
        Person nargiza = person(family, "Nargiza", "Aliyeva", Gender.FEMALE, "1984-03-22", null,
                "Toshkent", "Moliyachi", "Oilaviy hujjatlarni tartibga soladi.");
        Person jasur = person(family, "Jasur", "Aliyev", Gender.MALE, "2010-08-05", null,
                "Toshkent", "O'quvchi", "Rasm chizish va robototexnikaga qiziqadi.");
        Person laylo = person(family, "Laylo", "Aliyeva", Gender.FEMALE, "2013-11-30", null,
                "Toshkent", "O'quvchi", "Musiqa va raqsni yaxshi ko'radi.");

        personRepository.saveAll(List.of(nasriddin, zarina, farhod, nargiza, jasur, laylo));

        relationshipRepository.saveAll(List.of(
                relationship(family, nasriddin, zarina, RelationshipType.PARTNER, "Turmush ortoqlar"),
                relationship(family, nasriddin, farhod, RelationshipType.PARENT, "Ota va ogil"),
                relationship(family, zarina, farhod, RelationshipType.PARENT, "Ona va ogil"),
                relationship(family, farhod, nargiza, RelationshipType.PARTNER, "Turmush ortoqlar"),
                relationship(family, farhod, jasur, RelationshipType.PARENT, "Ota va ogil"),
                relationship(family, nargiza, jasur, RelationshipType.PARENT, "Ona va ogil"),
                relationship(family, farhod, laylo, RelationshipType.PARENT, "Ota va qiz"),
                relationship(family, nargiza, laylo, RelationshipType.PARENT, "Ona va qiz")
        ));
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

    private boolean hasPeople(Family family) {
        return !personRepository.findAllByFamilyIdOrderByFirstNameAsc(family.getId()).isEmpty();
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

    private Person person(
            Family family,
            String firstName,
            String lastName,
            Gender gender,
            String birthDate,
            String deathDate,
            String birthPlace,
            String occupation,
            String biography
    ) {
        Person person = new Person();
        person.setFamily(family);
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setGender(gender);
        person.setBirthDate(LocalDate.parse(birthDate));
        person.setDeathDate(deathDate == null ? null : LocalDate.parse(deathDate));
        person.setBirthPlace(birthPlace);
        person.setOccupation(occupation);
        person.setBiography(biography);
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
}
